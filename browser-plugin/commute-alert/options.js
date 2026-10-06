const api = globalThis.browser ?? globalThis.chrome;
const HOSTS = ["https://www.google.com/*", "https://consent.google.com/*", "https://photon.komoot.io/*"];
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

// ---------- address suggestions (Photon / OpenStreetMap, no key) ----------
function labelFor(p) {
  const street = [p.street, p.housenumber].filter(Boolean).join(" ");
  const main = p.name || street || p.city || "";
  const rest = [p.name && street ? street : null, p.postcode, p.city !== main ? p.city : null, p.country]
    .filter(Boolean);
  return { main, rest: [...new Set(rest)].join(", ") };
}

function attachAutocomplete(input, route, field, otherField) {
  const box = input.parentElement;
  const list = document.createElement("ul");
  list.setAttribute("role", "listbox");
  list.id = `ac-${route.id}-${field}`;
  list.hidden = true;
  const pin = document.createElement("span");
  pin.className = "pin";
  pin.textContent = "✓";
  pin.title = "Exact location saved";
  box.append(list, pin);

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

  const close = () => {
    list.hidden = true;
    input.setAttribute("aria-expanded", "false");
    input.removeAttribute("aria-activedescendant");
    active = -1;
  };

  const highlight = (i) => {
    active = i;
    [...list.children].forEach((li, n) => li.setAttribute("aria-selected", String(n === i)));
    if (i >= 0) {
      input.setAttribute("aria-activedescendant", list.children[i].id);
      list.children[i].scrollIntoView({ block: "nearest" });
    }
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

  const search = async (q) => {
    ctrl?.abort();
    ctrl = new AbortController();
    const params = new URLSearchParams({ q, limit: "6" });
    const bias = route[`${otherField}Coords`];
    if (bias) { params.set("lat", bias[0]); params.set("lon", bias[1]); }
    try {
      const resp = await fetch(`https://photon.komoot.io/api/?${params}`, { signal: ctrl.signal });
      const data = await resp.json();
      items = (data.features || []).map((f) => {
        const { main, rest } = labelFor(f.properties || {});
        const [lon, lat] = f.geometry.coordinates;
        return { main, rest, text: [main, rest].filter(Boolean).join(", "), coords: [+lat.toFixed(6), +lon.toFixed(6)] };
      }).filter((x) => x.main);
    } catch (e) {
      if (e.name === "AbortError") return;
      items = [];
    }
    list.textContent = "";
    items.forEach((it, i) => {
      const li = document.createElement("li");
      li.id = `${list.id}-${i}`;
      li.setAttribute("role", "option");
      li.textContent = it.main;
      if (it.rest) { const s = document.createElement("small"); s.textContent = it.rest; li.append(s); }
      li.addEventListener("mousedown", (ev) => { ev.preventDefault(); choose(i); });
      list.append(li);
    });
    list.hidden = !items.length;
    input.setAttribute("aria-expanded", String(!!items.length));
    active = -1;
  };

  input.addEventListener("input", () => {
    route[field] = input.value;
    route[`${field}Coords`] = null; // typed by hand: let Google Maps interpret the text
    showPin();
    dirty();
    clearTimeout(timer);
    const q = input.value.trim();
    if (q.length < 3) { close(); return; }
    timer = setTimeout(() => search(q), 300);
  });
  input.addEventListener("keydown", (ev) => {
    if (list.hidden) return;
    if (ev.key === "ArrowDown") { ev.preventDefault(); highlight((active + 1) % items.length); }
    else if (ev.key === "ArrowUp") { ev.preventDefault(); highlight((active - 1 + items.length) % items.length); }
    else if (ev.key === "Enter" && active >= 0) { ev.preventDefault(); choose(active); }
    else if (ev.key === "Escape") close();
  });
  input.addEventListener("blur", () => setTimeout(close, 100));
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

$("add").onclick = () => { routes.push(newRoute({ name: "New route" })); render(); };
$("save").onclick = save;
load();
