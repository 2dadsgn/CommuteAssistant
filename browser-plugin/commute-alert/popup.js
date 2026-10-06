const api = globalThis.browser ?? globalThis.chrome;
const list = document.getElementById("list");
const MIN = 60000;

document.getElementById("settings").onclick = () => api.runtime.openOptionsPage();

const fmt = (ms) => new Date(ms).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
const todayKey = () => { const d = new Date(); return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,"0")}-${String(d.getDate()).padStart(2,"0")}`; };
const DAYS = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];

const place = (r, f) => (r[`${f}Coords`] ? r[`${f}Coords`].join(",") : r[f]);
function mapsUrl(r) {
  return `https://www.google.com/maps/dir/?api=1&origin=${encodeURIComponent(place(r, "origin"))}&destination=${encodeURIComponent(place(r, "destination"))}&travelmode=driving`;
}
const short = (s) => (s || "").split(",")[0];

function pickResult(r, s) {
  if (!s) return null;
  if (r.mode === "watch") return s.result || null;
  const now = Date.now();
  return [s.date === todayKey() ? s.result : null, s.preview]
    .filter((x) => x && x.arrival > now - 30 * MIN)
    .sort((a, b) => b.computedAt - a.computedAt)[0] || null;
}

function el(tag, cls, text) {
  const e = document.createElement(tag);
  if (cls) e.className = cls;
  if (text != null) e.textContent = text;
  return e;
}

function leaveLabel(res) {
  const left = Math.round((res.departAt - Date.now()) / MIN);
  const day = new Date(res.departAt).toDateString() === new Date().toDateString()
    ? "" : `${DAYS[new Date(res.departAt).getDay()]} `;
  if (left <= 0) return ["Leave now", ""];
  if (left < 60) return [fmt(res.departAt), `leave in ${left} min`];
  return [fmt(res.departAt), `${day}leave by`.trim()];
}

async function render() {
  const { routes = [], state = {} } = await api.storage.local.get(["routes", "state"]);
  list.textContent = "";
  if (!routes.length) {
    const box = el("div", "empty", "Add a route like work to home to get started.");
    const b = el("button", "primary", "Open settings");
    b.onclick = () => api.runtime.openOptionsPage();
    box.append(el("br"), b);
    list.append(box);
    return;
  }

  for (const r of routes) {
    const s = state[r.id];
    const res = pickResult(r, s);
    const card = el("section", `trip ${res ? res.level : ""}`);
    card.append(el("h2", null, r.enabled ? r.name : `${r.name} (paused)`));
    card.append(el("div", "where", `${short(r.origin)} to ${short(r.destination)}`));

    if (res && r.mode === "watch") {
      const leave = el("div", "leave");
      leave.append(el("strong", "time", `${res.durationMin} min`), el("span", null, "right now"));
      card.append(leave);
      const status = el("div", "status");
      status.append(el("b", null, `${res.level[0].toUpperCase()}${res.level.slice(1)} traffic`),
        res.extraMin > 1 ? ` (+${res.extraMin} min vs usual)` : "");
      card.append(status);
      card.append(el("div", "meta",
        `Leave at ${fmt(res.computedAt)}, arrive about ${fmt(res.arrivalEstimate)}` +
        (res.via ? ` via ${res.via}` : "") +
        `. Updates every ${r.updateEveryMin} min` +
        (r.useWindow ? ` between ${r.windowStart} and ${r.windowEnd}` : "") + "."));
    } else if (res) {
      const [big, small] = leaveLabel(res);
      const leave = el("div", "leave");
      leave.append(el("strong", "time", big), el("span", null, small));
      card.append(leave);
      const status = el("div", "status");
      status.append(`${res.durationMin} min, `, el("b", null, `${res.level} traffic`),
        res.extraMin > 1 ? ` (+${res.extraMin} min vs usual)` : "");
      card.append(status);
      const late = Math.round((res.arrivalEstimate - res.arrival) / MIN);
      card.append(el("div", "meta",
        `Arrive about ${fmt(res.arrivalEstimate)} for ${r.arriveBy}` +
        (late > 0 ? `, ${late} min late` : "") +
        (res.via ? ` via ${res.via}` : "") +
        `. Updated ${fmt(res.computedAt)}.`));
    } else {
      card.append(el("div", "meta", r.mode === "watch"
        ? `Traffic updates every ${r.updateEveryMin} min. No check yet.`
        : `Arrive by ${r.arriveBy}. No traffic check yet.`));
    }
    if (s?.error) card.append(el("div", "err", s.error));

    const row = el("div", "row");
    const refresh = el("button", "primary", "Check traffic");
    const err = el("div", "err");
    refresh.onclick = async () => {
      refresh.disabled = true; refresh.textContent = "Reading Maps…"; err.textContent = "";
      const resp = await api.runtime.sendMessage({ type: "checkNow", routeId: r.id });
      if (resp?.ok) render();
      else { err.textContent = resp?.error || "Check failed."; refresh.disabled = false; refresh.textContent = "Check traffic"; }
    };
    const open = el("button", null, "Open in Maps");
    open.onclick = () => api.tabs.create({ url: mapsUrl(r) });
    const pause = el("button", null, r.enabled ? "Pause" : "Resume");
    pause.onclick = async () => {
      const { routes: all = [] } = await api.storage.local.get("routes");
      const target = all.find((x) => x.id === r.id);
      if (!target) return;
      target.enabled = !target.enabled;
      await api.storage.local.set({ routes: all });
      render();
    };
    row.append(refresh, open, pause);
    card.append(row, err);
    list.append(card);
  }
}

render();
