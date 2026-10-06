// Commute Alert – reads travel time from the Google Maps page (no API key).
const api = globalThis.browser ?? globalThis.chrome;

const TICK = "commute-alert-tick";
const MIN = 60 * 1000;

// ---------- setup ----------
async function ensureAlarm() {
  const existing = await api.alarms.get(TICK);
  if (!existing) api.alarms.create(TICK, { periodInMinutes: 1, delayInMinutes: 0.1 });
}
api.runtime.onInstalled.addListener(async (details) => {
  await ensureAlarm();
  if (details.reason === "install") api.runtime.openOptionsPage();
});
api.runtime.onStartup.addListener(ensureAlarm);
ensureAlarm();

api.alarms.onAlarm.addListener((alarm) => {
  if (alarm.name === TICK) serial(tick).catch((e) => console.error("tick failed", e));
});

// One job at a time: never two Maps windows open, never two writers to storage.
let queue = Promise.resolve();
function serial(fn) {
  const p = queue.then(fn, fn);
  queue = p.catch(() => {});
  return p;
}

// ---------- helpers ----------
const dateKey = (d) =>
  `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;

function atTime(day, hhmm) {
  const [h, m] = hhmm.split(":").map(Number);
  const d = new Date(day);
  d.setHours(h, m, 0, 0);
  return d;
}

function nextArrival(route, now = new Date()) {
  for (let i = 0; i < 8; i++) {
    const d = new Date(now);
    d.setDate(d.getDate() + i);
    const t = atTime(d, route.arriveBy);
    if (route.days.includes(t.getDay()) && t > now) return t;
  }
  const t = atTime(now, route.arriveBy);
  t.setDate(t.getDate() + 1);
  return t;
}

const fmt = (ms) => new Date(ms).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });

// Exact coordinates when the address was picked from suggestions, otherwise the typed text.
const place = (route, f) => (route[`${f}Coords`] ? route[`${f}Coords`].join(",") : route[f]);

// Page we read from: driving directions, forced to English so the text is predictable.
function readUrl(route) {
  return `https://www.google.com/maps/dir/${encodeURIComponent(place(route, "origin"))}/` +
    `${encodeURIComponent(place(route, "destination"))}/data=!4m2!4m1!3e0?hl=en`;
}
// Page we open for the person (their own language).
function mapsUrl(route) {
  return "https://www.google.com/maps/dir/?api=1" +
    `&origin=${encodeURIComponent(place(route, "origin"))}` +
    `&destination=${encodeURIComponent(place(route, "destination"))}&travelmode=driving`;
}

function levelFromRatio(ratio) {
  if (ratio < 1.15) return "light";
  if (ratio < 1.4) return "moderate";
  return "heavy";
}

// ---------- reading Google Maps ----------
// Runs INSIDE the Google Maps tab. Must be self-contained (no outside variables).
// If Google changes its page layout, this is the function to update.
async function readTripFromPage() {
  const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
  const parseDuration = (t) => {
    const m = (t || "").match(/(\d+)\s*(?:hr|hrs|hours?|h)\b(?:\s*(\d+)\s*min)?|(\d+)\s*min/i);
    if (!m) return null;
    return m[3] ? +m[3] : +m[1] * 60 + (m[2] ? +m[2] : 0);
  };
  const deadline = Date.now() + 20000;
  while (Date.now() < deadline) {
    if (location.hostname.startsWith("consent.")) return { error: "consent" };
    const trip = document.querySelector('[id^="section-directions-trip-0"], [data-trip-index="0"]');
    const text = trip?.innerText || "";
    if (text.trim()) {
      const delayEl = trip.querySelector('[class*="delay-"]');
      const durationMin = parseDuration(delayEl?.textContent) ?? parseDuration(text);
      if (durationMin != null) {
        const cls = String(delayEl?.className || "");
        const level = /delay-heavy/.test(cls) ? "heavy"
          : /delay-medium/.test(cls) ? "moderate"
          : /delay-light|delay-none/.test(cls) ? "light" : null;
        return {
          durationMin,
          level,
          via: ((text.match(/\bvia\s+([^\n]+)/i) || [])[1] || "").trim(),
          distance: (text.match(/[\d.,]+\s*(?:km|mi)\b/i) || [""])[0],
        };
      }
    }
    if (/could not calculate|can't find a way|couldn't find/i.test(document.body?.innerText || "")) {
      return { error: "noroute" };
    }
    await sleep(600);
  }
  return { error: "timeout" };
}

function waitForMapsLoad(tabId, timeout) {
  return new Promise((resolve) => {
    let finished = false;
    const done = () => {
      if (finished) return;
      finished = true;
      clearTimeout(timer);
      api.tabs.onUpdated.removeListener(listener);
      resolve();
    };
    const isGoogle = (url) => /^https:\/\/(www|consent)\.google\.com\//.test(url || "");
    const listener = (id, info, tab) => {
      if (id === tabId && info.status === "complete" && isGoogle(tab?.url)) done();
    };
    const timer = setTimeout(done, timeout);
    api.tabs.onUpdated.addListener(listener);
    api.tabs.get(tabId).then((t) => { if (t.status === "complete" && isGoogle(t.url)) done(); }, () => {});
  });
}

async function readGoogleMaps(route, { fromPopup = false } = {}) {
  const url = readUrl(route);
  let windowId = null;
  let tabId = null;
  try {
    const canHide = typeof api.tabs.hide === "function"; // Firefox only
    if (canHide || fromPopup) {
      // A background tab doesn't take focus, so an open popup stays open.
      const tab = await api.tabs.create({ url, active: false });
      tabId = tab.id;
      if (canHide) await api.tabs.hide(tabId).catch(() => {});
    } else {
      try {
        const win = await api.windows.create({ url, state: "minimized" });
        windowId = win.id;
        tabId = win.tabs?.[0]?.id ?? (await api.tabs.query({ windowId }))[0]?.id;
      } catch {
        const tab = await api.tabs.create({ url, active: false });
        tabId = tab.id;
      }
    }
    await waitForMapsLoad(tabId, 20000);

    let result = null;
    for (let attempt = 0; attempt < 3 && !result; attempt++) {
      try {
        const [inj] = await api.scripting.executeScript({ target: { tabId }, func: readTripFromPage });
        result = inj?.result ?? null;
      } catch {
        await new Promise((r) => setTimeout(r, 2000));
      }
    }
    if (!result) throw new Error("Couldn't read the Google Maps page. Try again in a minute.");
    if (result.error === "consent") {
      throw new Error("Google is showing its cookie choice page. Open google.com/maps once in this browser, make your choice, then try again.");
    }
    if (result.error === "noroute") throw new Error("Google Maps found no driving route. Check the two addresses.");
    if (result.error === "timeout") {
      throw new Error("Google Maps didn't show a travel time in time. If this keeps happening, Google may have changed its page.");
    }
    return result;
  } finally {
    if (windowId != null) api.windows.remove(windowId).catch(() => {});
    else if (tabId != null) api.tabs.remove(tabId).catch(() => {});
  }
}

/** Current travel time plus how it compares with the usual (fastest seen) time. */
async function measureTrip(route, opts) {
  const now = Date.now();
  const trip = await readGoogleMaps(route, opts);
  const key = `${place(route, "origin")}|${place(route, "destination")}`.trim().toLowerCase();
  const { baselines = {} } = await api.storage.local.get("baselines");
  if (!baselines[key] || trip.durationMin < baselines[key]) {
    baselines[key] = trip.durationMin;
    await api.storage.local.set({ baselines });
  }
  const typical = baselines[key];
  return {
    durationMin: trip.durationMin,
    typicalMin: typical,
    extraMin: Math.max(0, trip.durationMin - typical),
    level: trip.level ?? levelFromRatio(trip.durationMin / typical),
    via: trip.via,
    distance: trip.distance,
    arrivalEstimate: now + trip.durationMin * MIN,
    computedAt: now,
  };
}

/**
 * Latest departure that still arrives by (arrival - margin), using the current
 * travel time shown by Google Maps. Re-checked regularly, so it tracks traffic as it changes.
 */
async function planDeparture(route, arrival, opts) {
  const m = await measureTrip(route, opts);
  const arrivalMs = arrival.getTime();
  const target = arrivalMs - (route.bufferMin || 0) * MIN;
  const nowMin = Math.floor(m.computedAt / MIN) * MIN;
  const departAt = Math.max(Math.floor((target - m.durationMin * MIN) / MIN) * MIN, nowMin);
  return { ...m, arrival: arrivalMs, departAt, arrivalEstimate: departAt + m.durationMin * MIN };
}

// ---------- notifications ----------
function notify(route, title, message) {
  api.notifications.create(`ca|${route.id}|${Date.now()}`, {
    type: "basic",
    iconUrl: api.runtime.getURL("icons/icon128.png"),
    title,
    message,
  });
}

api.notifications.onClicked.addListener(async (id) => {
  if (!id.startsWith("ca|")) return;
  const routeId = id.split("|")[1];
  const { routes = [] } = await api.storage.local.get("routes");
  const route = routes.find((r) => r.id === routeId);
  if (route) api.tabs.create({ url: mapsUrl(route) });
  api.notifications.clear(id);
});

function trafficLine(res) {
  const base = `${res.durationMin} min trip, ${res.level} traffic`;
  return res.extraMin > 1 ? `${base} (+${res.extraMin} min)` : base;
}

function maybeNotify(route, s, now, fresh) {
  const res = s.result;
  if (!res) return;
  const n = (s.notified ||= {});
  const dep = res.departAt;
  const lead = (route.reminderLeadMin ?? 15) * MIN;
  const threshold = (route.changeThresholdMin ?? 5) * MIN;

  if (n.lastDepart == null) n.lastDepart = dep;

  if (now >= dep) {
    if (!n.leaveNow) {
      n.leaveNow = true;
      s.done = true;
      const late = Math.round((res.arrivalEstimate - res.arrival) / MIN);
      const msg = late > 0
        ? `Leaving now you'll arrive around ${fmt(res.arrivalEstimate)}, about ${late} min late. ${trafficLine(res)}.`
        : `Arrive around ${fmt(res.arrivalEstimate)}. ${trafficLine(res)}.`;
      notify(route, `Leave now: ${route.name}`, msg);
    }
    return;
  }

  if (fresh && Math.abs(dep - n.lastDepart) >= threshold) {
    const diff = Math.round((n.lastDepart - dep) / MIN);
    const title = diff > 0
      ? `Traffic got worse: leave ${diff} min earlier`
      : `Traffic eased: you can leave ${-diff} min later`;
    notify(route, title, `${route.name}: leave by ${fmt(dep)} (was ${fmt(n.lastDepart)}). ${trafficLine(res)}.`);
    n.lastDepart = dep;
    if (now >= dep - lead) n.reminder = true;
    return;
  }

  if (now >= dep - lead && !n.reminder) {
    n.reminder = true;
    n.lastDepart = dep;
    notify(route, `Leave by ${fmt(dep)}: ${route.name}`, `To arrive by ${route.arriveBy}. ${trafficLine(res)}.`);
  }
}

// ---------- badge ----------
async function updateBadge(routes, state, now) {
  let best = null;
  for (const r of routes) {
    const s = state[r.id];
    if (!r.enabled || r.mode === "watch" || !s?.result || s.date !== dateKey(new Date(now))) continue;
    if (now > s.result.departAt + 10 * MIN) continue;
    if (!best || s.result.departAt < best.departAt) best = s.result;
  }
  if (!best) return api.action.setBadgeText({ text: "" });
  const left = Math.round((best.departAt - now) / MIN);
  if (left > 90) return api.action.setBadgeText({ text: "" });
  const colors = { light: "#2E9E5B", moderate: "#E08A1E", heavy: "#D6453D" };
  await api.action.setBadgeBackgroundColor({ color: left <= 0 ? "#D6453D" : colors[best.level] });
  await api.action.setBadgeText({ text: left <= 0 ? "GO" : `${left}m` });
}

// Tell the person once when checks start failing, and once when they work again.
function reportFailure(route, s) {
  if (s.failNotified) return;
  s.failNotified = true;
  notify(route, `${route.name}: couldn't get traffic`, `${s.error} Commute Alert will keep trying.`);
}
function reportRecovery(route, s) {
  if (!s.failNotified) return;
  s.failNotified = false;
  if (route.mode !== "watch") notify(route, `${route.name}: traffic checks working again`, "You'll get your leave-by alerts as usual.");
}

// ---------- traffic-update routes ----------
function inWindow(route, d) {
  if (!route.useWindow) return true;
  const cur = d.getHours() * 60 + d.getMinutes();
  const toMin = (t) => { const [h, m] = t.split(":").map(Number); return h * 60 + m; };
  const a = toMin(route.windowStart), b = toMin(route.windowEnd);
  return a <= b ? cur >= a && cur < b : cur >= a || cur < b; // supports ranges past midnight
}

async function tickWatch(route, state, nowDate) {
  if (!route.days?.includes(nowDate.getDay()) || !inWindow(route, nowDate)) return;
  const s = (state[route.id] ||= {});
  const interval = Math.max(5, route.updateEveryMin ?? 15) * MIN;
  if (s.lastCheck && nowDate.getTime() - s.lastCheck < interval - 20 * 1000) return;

  try {
    s.result = await measureTrip(route);
    s.error = null;
  } catch (e) {
    s.error = String(e.message || e);
    s.lastCheck = Date.now();
    reportFailure(route, s);
    return;
  }
  s.lastCheck = Date.now();
  reportRecovery(route, s);

  const res = s.result;
  const threshold = route.changeThresholdMin ?? 5;
  const prev = s.lastNotifiedMin;
  if (route.notifyMode === "change" && prev != null && Math.abs(res.durationMin - prev) < threshold) return;

  let title = `${route.name}: ${res.durationMin} min now`;
  if (prev != null && res.durationMin - prev >= threshold) title += ` (up ${res.durationMin - prev} min)`;
  else if (prev != null && prev - res.durationMin >= threshold) title += ` (down ${prev - res.durationMin} min)`;
  const extra = res.extraMin > 1 ? ` (+${res.extraMin} min vs usual)` : "";
  notify(route, title,
    `${res.level[0].toUpperCase()}${res.level.slice(1)} traffic${extra}. ` +
    `Leave now to arrive about ${fmt(res.arrivalEstimate)}.` + (res.via ? ` Via ${res.via}.` : ""));
  s.lastNotifiedMin = res.durationMin;
}

// ---------- main loop ----------
async function tick() {
  const { routes = [], state = {} } = await api.storage.local.get(["routes", "state"]);
  const nowDate = new Date();
  const now = nowDate.getTime();
  const today = dateKey(nowDate);

  for (const route of routes) {
    if (!route.enabled) continue;
    if (route.mode === "watch") { await tickWatch(route, state, nowDate); continue; }
    if (!route.days?.includes(nowDate.getDay())) continue;
    const arrival = atTime(nowDate, route.arriveBy).getTime();
    const windowStart = arrival - (route.monitorStartMin ?? 120) * MIN;
    if (now < windowStart || now > arrival) continue;

    let s = state[route.id];
    if (!s || s.date !== today) s = state[route.id] = { date: today, notified: {}, preview: s?.preview };
    if (s.done) continue;

    const dep = s.result?.departAt;
    const nearDeparture = dep && dep - now < 30 * MIN;
    const every = Math.max(5, route.checkEveryMin ?? 10);
    const interval = (nearDeparture ? 5 : every) * MIN;

    if (s.lastCheck && now - s.lastCheck < interval) {
      maybeNotify(route, s, now, false);
      continue;
    }
    try {
      s.result = await planDeparture(route, new Date(arrival));
      s.error = null;
      reportRecovery(route, s);
    } catch (e) {
      s.error = String(e.message || e);
      reportFailure(route, s);
    }
    s.lastCheck = Date.now();
    maybeNotify(route, s, Date.now(), true);
  }

  // Re-read so a check made from the popup meanwhile isn't overwritten.
  const latest = (await api.storage.local.get("state")).state || {};
  for (const id of Object.keys(state)) latest[id] = { ...latest[id], ...state[id], preview: latest[id]?.preview ?? state[id].preview };
  await api.storage.local.set({ state: latest });
  await updateBadge(routes, latest, Date.now());
}

// ---------- messages from popup / settings ----------
// Track whether the popup is open, so a manual check can still report back if it closed.
let popupPorts = 0;
api.runtime.onConnect.addListener((port) => {
  if (port.name !== "popup") return;
  popupPorts++;
  port.onDisconnect.addListener(() => { popupPorts--; });
});

async function checkNow(routeId) {
  const { routes = [], state = {} } = await api.storage.local.get(["routes", "state"]);
  const route = routes.find((r) => r.id === routeId);
  if (!route) throw new Error("Route not found. Save your settings and try again.");
  const s = (state[route.id] ||= {});
  const opts = { fromPopup: true };

  try {
    let result;
    if (route.mode === "watch") {
      result = await measureTrip(route, opts);
      s.result = result;
    } else {
      result = await planDeparture(route, nextArrival(route), opts);
      s.preview = result;
      if (s.date === dateKey(new Date()) && s.result && s.result.arrival === result.arrival && !s.done) {
        s.result = result;
        s.lastCheck = Date.now();
      }
    }
    s.error = null;
    await api.storage.local.set({ state });
    await updateBadge(routes, state, Date.now());

    if (popupPorts === 0) {
      const extra = result.extraMin > 1 ? ` (+${result.extraMin} min vs usual)` : "";
      notify(route, route.mode === "watch"
        ? `${route.name}: ${result.durationMin} min now`
        : `${route.name}: leave by ${fmt(result.departAt)}`,
        `${result.level[0].toUpperCase()}${result.level.slice(1)} traffic${extra}. ` +
        (route.mode === "watch"
          ? `Leave now to arrive about ${fmt(result.arrivalEstimate)}.`
          : `${result.durationMin} min trip, arrive about ${fmt(result.arrivalEstimate)}.`));
    }
    return result;
  } catch (e) {
    s.error = String(e.message || e);
    await api.storage.local.set({ state });
    if (popupPorts === 0) notify(route, `${route.name}: traffic check failed`, s.error);
    throw e;
  }
}

api.runtime.onMessage.addListener((msg, _sender, sendResponse) => {
  if (msg?.type === "checkNow") {
    serial(() => checkNow(msg.routeId)).then(
      (result) => sendResponse({ ok: true, result }),
      (e) => sendResponse({ ok: false, error: String(e.message || e) })
    );
    return true;
  }
  if (msg?.type === "testNotification") {
    notify({ id: "test", name: "Test" }, "Commute Alert notifications work",
      "This is how traffic alerts will look. Click a real alert to open the route in Google Maps.");
    sendResponse({ ok: true });
    return false;
  }
  if (msg?.type === "routesChanged") {
    serial(async () => {
      await api.storage.local.set({ state: {} });
      await api.action.setBadgeText({ text: "" });
      await tick();
    }).then(() => sendResponse({ ok: true }), () => sendResponse({ ok: false }));
    return true;
  }
  return false;
});
