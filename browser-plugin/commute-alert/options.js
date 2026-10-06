const api = globalThis.browser ?? globalThis.chrome;
const HOSTS = ["https://www.google.com/*", "https://consent.google.com/*", "https://photon.komoot.io/*", "https://nominatim.openstreetmap.org/*"];
const DAY_ORDER = [1, 2, 3, 4, 5, 6, 0];
const DAY_NAMES = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
const NUMERIC = new Set(["bufferMin", "reminderLeadMin", "changeThresholdMin", "monitorStartMin", "checkEveryMin", "updateEveryMin"]);

const $ = (id) => document.getElementById(id);
const container = $("routes");
let routes = [];

const newRoute = (over = {}) => ({
  id: crypto.randomUUID(),
  name: "Work to home",
  origin: "", originCoords: null,
  destination: "", destinationCoords: null,
  mode: "arrive",
  arriveBy: "18:30",
  updateEveryMin: 15,
  notifyMode: "always",
  useWindow: false,
  windowStart: "17:00",
  windowEnd: "19:00",
  days: [1, 2, 3, 4, 5],
  bufferMin: 5,
  reminderLeadMin: 15,
  changeThresholdMin: 5,
  monitorStartMin: 120,
  checkEveryMin: 10,
  enabled: true,
  ...over,
});

const dirty = () => { $("saved").textContent = ""; };

async function load() {
  const d = await api.storage.local.get("routes");
  routes = d.routes?.length ? d.routes.map((r) => newRoute(r)) : [newRoute()];
  render();
}

function render() {
  container.textContent = "";
  routes.forEach((r) => container.append(card(r)));
}

// ---------- address suggestions ----------
// As you type: Photon (OpenStreetMap, built for search-as-you-type, no key).
// On Enter, or if Photon fails: one Nominatim search (OpenStreetMap; not allowed for as-you-type).
const AC_HOSTS = ["https://photon.komoot.io/*", "https://nominatim.openstreetmap.org/*"];

async function hasAcPermission() {
  try { return await api.permissions.contains({ origins: AC_HOSTS }); } catch { return true; }
}

function labelFor(p) {
  const street = [p.street, p.housenumber].filter(Boolean).join(" ");
  const ownName = p.name && p.name !== p.street ? p.name : null; // a shop, building, etc.
  const main = ownName || street || p.name || p.city || "";
  const rest = [ownName && street ? street : null, p.postcode, p.city !== main ? p.city : null, p.country]
    .filter(Boolean);
  return { main, rest: [...new Set(rest)].join(", ") };
}

async function photonSearch(q, bias, signal) {
  const params = new URLSearchParams({ q, limit: "6" });
  if (bias) { params.set("lat", bias[0]); params.set("lon", bias[1]); }
  const resp = await fetch(`https://photon.komoot.io/api/?${params}`, { signal });
  if (!resp.ok) throw new Error(`address search answered ${resp.status}`);
  const data = await resp.json();
  return (data.features || []).map((f) => {
    const { main, rest } = labelFor(f.properties || {});
    const [lon, lat] = f.geometry.coordinates;
    return { main, rest, text: [main, rest].filter(Boolean).join(", "), coords: [+lat.toFixed(6), +lon.toFixed(6)] };
  }).filter((x) => x.main);
}

async function nominatimSearch(q, signal) {
  const params = new URLSearchParams({ q, format: "jsonv2", limit: "6" });
  const resp = await fetch(`https://nominatim.openstreetmap.org/search?${params}`, { signal });
  if (!resp.ok) throw new Error(`OpenStreetMap search answered ${resp.status}`);
  const data = await resp.json();
  return data.map((d) => {
    const parts = String(d.display_name || "").split(", ");
    return {
      main: parts.slice(0, 2).join(", "),
      rest: parts.slice(2).join(", "),
      text: d.display_name,
      coords: [+(+d.lat).toFixed(6), +(+d.lon).toFixed(6)],
    };
  });
}

function attachAutocomplete(input, route, field, otherField) {
  const box = input.parentElement;
  const list = document.createElement("ul");
  list.setAttribute("role", "listbox");
  list.id = `ac-${route.id}-${field}`;
  list.hidden = true;
  list.addEventListener("mousedown", (ev) => ev.preventDefault()); // keep focus in the field
  const pin = document.createElement("span");
  pin.className = "pin";
  pin.textContent = "✓";
  pin.title = "Exact location saved";
  box.append(list, pin);

  // "Use my current location" button
  const locate = document.createElement("button");
  locate.type = "button";
  locate.className = "locate";
  locate.title = "Use my current location";
  locate.setAttribute("aria-label", "Use my current location");
  locate.innerHTML = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><circle cx="12" cy="12" r="7"/><circle cx="12" cy="12" r="2.5" fill="currentColor"/><path d="M12 2v3M12 19v3M2 12h3M19 12h3"/></svg>';
  box.append(locate);

  input.setAttribute("role", "combobox");
  input.setAttribute("aria-controls", list.id);
  input.setAttribute("aria-expanded", "false");
  input.value = route[field] || "";

  let items = [];
  let active = -1;
  let timer = null;
  let ctrl = null;

  const showPin = () => { pin.hidden = !route[`${field}Coords`]; };
  showPin();

  const open = () => { list.hidden = false; input.setAttribute("aria-expanded", "true"); };
  const close = () => {
    list.hidden = true;
    input.setAttribute("aria-expanded", "false");
    input.removeAttribute("aria-activedescendant");
    active = -1;
  };

  const status = (msg, action) => {
    items = [];
    active = -1;
    list.textContent = "";
    const li = document.createElement("li");
    li.className = "status";
    li.textContent = msg;
    if (action) {
      const b = document.createElement("button");
      b.type = "button";
      b.textContent = action.label;
      b.addEventListener("click", action.onClick);
      li.append(" ", b);
    }
    list.append(li);
    open();
  };

  const showItems = (found, emptyMsg) => {
    items = found;
    if (!items.length) { status(emptyMsg); return; }
    list.textContent = "";
    items.forEach((it, i) => {
      const li = document.createElement("li");
      li.id = `${list.id}-${i}`;
      li.setAttribute("role", "option");
      li.textContent = it.main;
      if (it.rest) { const s = document.createElement("small"); s.textContent = it.rest; li.append(s); }
      li.addEventListener("click", () => choose(i));
      list.append(li);
    });
    active = -1;
    open();
  };

  const highlight = (i) => {
    active = i;
    [...list.querySelectorAll('[role="option"]')].forEach((li, n) => li.setAttribute("aria-selected", String(n === i)));
    const el = list.children[i];
    if (el) { input.setAttribute("aria-activedescendant", el.id); el.scrollIntoView({ block: "nearest" }); }
  };

  const choose = (i) => {
    const it = items[i];
    if (!it) return;
    route[field] = it.text;
    route[`${field}Coords`] = it.coords;
    input.value = it.text;
    showPin();
    close();
    dirty();
  };

  const askPermission = (then) => status(
    "Address suggestions use the free OpenStreetMap search and need your permission first.",
    {
      label: "Allow",
      onClick: async () => {
        const ok = await api.permissions.request({ origins: AC_HOSTS }).catch(() => false);
        if (ok) then(); else status("Permission wasn't given, so suggestions are off. You can still type a full address.");
      },
    }
  );

  const run = async (q, wide) => {
    ctrl?.abort();
    ctrl = new AbortController();
    if (!(await hasAcPermission())) { askPermission(() => run(q, wide)); return; }
    status(wide ? "Searching OpenStreetMap…" : "Searching…");
    try {
      const found = wide
        ? await nominatimSearch(q, ctrl.signal)
        : await photonSearch(q, route[`${otherField}Coords`], ctrl.signal);
      showItems(found, wide
        ? "No places found. Try adding the town, e.g. \"Via Toledo 1, Napoli\"."
        : "No matches yet. Keep typing, or press Enter to search more widely.");
    } catch (e) {
      if (e.name === "AbortError") return;
      console.warn("Address search failed:", e);
      status(wide
        ? `Couldn't reach OpenStreetMap search (${e.message}). You can still type a full address.`
        : `Suggestions aren't available right now (${e.message}). Press Enter to search OpenStreetMap instead.`);
    }
  };

  input.addEventListener("input", () => {
    route[field] = input.value;
    route[`${field}Coords`] = null; // typed by hand: Google Maps will interpret the text
    showPin();
    dirty();
    clearTimeout(timer);
    const q = input.value.trim();
    if (q.length < 3) { close(); return; }
    timer = setTimeout(() => run(q, false), 300);
  });
  input.addEventListener("keydown", (ev) => {
    if (ev.key === "ArrowDown" && items.length) { ev.preventDefault(); highlight((active + 1) % items.length); }
    else if (ev.key === "ArrowUp" && items.length) { ev.preventDefault(); highlight((active - 1 + items.length) % items.length); }
    else if (ev.key === "Enter") {
      ev.preventDefault();
      if (active >= 0) choose(active);
      else if (input.value.trim().length >= 3) { clearTimeout(timer); run(input.value.trim(), true); }
    } else if (ev.key === "Escape") close();
  });
  input.addEventListener("blur", () => setTimeout(close, 150));

  locate.addEventListener("click", async () => {
    // Ask for the address-lookup permission first, while the click still counts (Firefox).
    const canLookup = api.permissions.request({ origins: AC_HOSTS }).catch(() => false);
    if (!navigator.geolocation) { status("This browser can't share your location. Type the address instead."); return; }
    locate.disabled = true;
    status("Finding your location…");
    try {
      const pos = await new Promise((resolve, reject) =>
        navigator.geolocation.getCurrentPosition(resolve, reject, { enableHighAccuracy: true, timeout: 15000, maximumAge: 60000 }));
      const lat = +pos.coords.latitude.toFixed(6);
      const lon = +pos.coords.longitude.toFixed(6);
      let text = `My location (${lat.toFixed(4)}, ${lon.toFixed(4)})`;
      if (await canLookup) {
        try {
          const resp = await fetch(`https://photon.komoot.io/reverse?lat=${lat}&lon=${lon}&limit=1`);
          const p = resp.ok ? (await resp.json()).features?.[0]?.properties : null;
          if (p) {
            const { main, rest } = labelFor(p);
            if (main) text = [main, rest].filter(Boolean).join(", ");
          }
        } catch { /* keep the coordinates label */ }
      }
      route[field] = text;
      route[`${field}Coords`] = [lat, lon];
      input.value = text;
      showPin();
      dirty();
      const acc = Math.round(pos.coords.accuracy || 0);
      if (acc > 300) {
        const shown = acc >= 1000 ? `${(acc / 1000).toFixed(1)} km` : `${acc} m`;
        status(`Location set, but it's only accurate to about ${shown}. Check the address, or type it instead.`);
        setTimeout(close, 8000);
      } else {
        close();
      }
    } catch (e) {
      const msg = e.code === 1
        ? "Location access is blocked for this extension. Allow it in your browser settings, or type the address."
        : e.code === 3
          ? "Finding your location took too long. Try again, or type the address."
          : "Your computer couldn't find its location. On Windows, check Settings > Privacy & security > Location is on.";
      status(msg);
    } finally {
      locate.disabled = false;
    }
  });
}

// ---------- route card ----------
function card(r) {
  const node = $("tpl").content.firstElementChild.cloneNode(true);
  node.dataset.mode = r.mode;

  node.querySelectorAll("[data-f]").forEach((input) => {
    const f = input.dataset.f;
    if (input.type === "checkbox") input.checked = !!r[f];
    else input.value = r[f] ?? "";
    input.addEventListener("input", () => {
      r[f] = input.type === "checkbox" ? input.checked : NUMERIC.has(f) ? Number(input.value) : input.value;
      if (f === "useWindow") syncWindow();
      dirty();
    });
  });

  const winInputs = node.querySelectorAll('[data-f="windowStart"], [data-f="windowEnd"]');
  const syncWindow = () => winInputs.forEach((i) => (i.disabled = !r.useWindow));
  syncWindow();

  attachAutocomplete(node.querySelector('[data-ac="origin"]'), r, "origin", "destination");
  attachAutocomplete(node.querySelector('[data-ac="destination"]'), r, "destination", "origin");

  node.querySelectorAll('.mode input').forEach((radio) => {
    radio.name = `mode-${r.id}`;
    radio.checked = radio.value === r.mode;
    radio.addEventListener("change", () => { r.mode = radio.value; node.dataset.mode = r.mode; dirty(); });
  });

  const days = node.querySelector(".days");
  DAY_ORDER.forEach((d) => {
    const lab = document.createElement("label");
    const cb = document.createElement("input");
    cb.type = "checkbox";
    cb.checked = r.days.includes(d);
    cb.onchange = () => {
      r.days = cb.checked ? [...new Set([...r.days, d])] : r.days.filter((x) => x !== d);
      dirty();
    };
    const span = document.createElement("span");
    span.textContent = DAY_NAMES[d];
    lab.append(cb, span);
    days.append(lab);
  });

  const out = node.querySelector(".out");
  node.querySelector(".del").onclick = () => {
    routes = routes.filter((x) => x !== r);
    render();
    $("saved").textContent = "Route removed. Save changes to keep this.";
  };
  node.querySelector(".reverse").onclick = () => {
    const short = (s) => (s || "").split(",")[0];
    routes.push(newRoute({
      ...r, id: crypto.randomUUID(),
      name: `${short(r.destination) || "B"} to ${short(r.origin) || "A"}`.slice(0, 60),
      origin: r.destination, originCoords: r.destinationCoords,
      destination: r.origin, destinationCoords: r.originCoords,
      arriveBy: "09:00", windowStart: "07:00", windowEnd: "09:00",
    }));
    render();
  };
  node.querySelector(".test").onclick = async (ev) => {
    const btn = ev.currentTarget;
    out.className = "out";
    if (!(await save())) return;
    btn.disabled = true;
    out.textContent = "Reading Google Maps…";
    const resp = await api.runtime.sendMessage({ type: "checkNow", routeId: r.id });
    btn.disabled = false;
    if (!resp?.ok) {
      out.className = "out bad";
      out.textContent = resp?.error || "Check failed.";
      return;
    }
    const res = resp.result;
    const t = (ms) => new Date(ms).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
    out.textContent = r.mode === "watch"
      ? `Right now: ${res.durationMin} min, ${res.level} traffic.`
      : `Next trip: leave by ${t(res.departAt)} to arrive by ${r.arriveBy}. ${res.durationMin} min right now, ${res.level} traffic.`;
  };
  return node;
}

function validate() {
  for (const r of routes) {
    if (!r.origin.trim() || !r.destination.trim()) return `"${r.name}" needs both a start and a destination.`;
    if (!r.days.length) return `"${r.name}" needs at least one day selected.`;
    if (r.mode === "arrive" && !/^\d{2}:\d{2}$/.test(r.arriveBy)) return `"${r.name}" needs an arrival time.`;
    if (r.mode === "watch") {
      if (!(r.updateEveryMin >= 5)) return `"${r.name}": updates can be at most every 5 minutes.`;
      if (r.useWindow && (!r.windowStart || !r.windowEnd)) return `"${r.name}" needs both times for the time range.`;
    }
  }
  return null;
}

async function save() {
  // Firefox treats host permissions as optional in MV3: ask while we still have the click.
  const granted = await api.permissions.request({ origins: HOSTS }).catch(() => true);
  const status = $("saved");
  status.style.color = "";
  if (!granted) {
    status.style.color = "var(--stop)";
    status.textContent = "Allow access to google.com so Commute Alert can read travel times from Google Maps.";
    return false;
  }
  const problem = validate();
  if (problem) {
    status.style.color = "var(--stop)";
    status.textContent = problem;
    return false;
  }
  await api.storage.local.set({ routes });
  await api.runtime.sendMessage({ type: "routesChanged" });
  status.textContent = "Saved";
  return true;
}

$("ver").textContent = `version ${api.runtime.getManifest().version}`;
$("testNotify").onclick = async () => {
  const resp = await api.runtime.sendMessage({ type: "testNotification" });
  $("notifyOut").textContent = resp?.ok ? "Sent. It should appear in the corner of your screen." : "Couldn't send it.";
};
$("add").onclick = () => { routes.push(newRoute({ name: "New route" })); render(); };
$("save").onclick = save;
load();
