// Campus Parking front end. All parking rules run in Java (ParkingServer / ParkingSystem / Lot);
// this file only draws the page and sends the user's actions to the server.

const SECTIONS = ["student", "faculty", "visitor", "service", "handicap", "general"];
const GROUPS = ["student", "faculty", "visitor", "service", "handicap"];
const LABELS = {
  student: "Student", faculty: "Faculty", visitor: "Visitor",
  service: "Service", handicap: "Handicap", general: "General"
};
const LAYOUT_SECTION = { s: "student", f: "faculty", v: "visitor", r: "service", h: "handicap", g: "general" };

// where each lot sits on the map (viewBox 1000 x 640); bigger lots get bigger shapes
const LOT_LAYOUT = [
  { x: 40,  y: 50,  w: 230, h: 150 }, // Lot 1 (100)
  { x: 545, y: 40,  w: 205, h: 135 }, // Lot 2 (80)
  { x: 775, y: 40,  w: 190, h: 127 }, // Lot 3 (70)
  { x: 40,  y: 375, w: 180, h: 115 }, // Lot 4 (60)
  { x: 250, y: 375, w: 180, h: 115 }, // Lot 5 (60)
  { x: 265, y: 510, w: 165, h: 105 }, // Lot 6 (50)
  { x: 545, y: 375, w: 150, h: 92 },  // Lot 7 (40)
  { x: 545, y: 492, w: 150, h: 92 }   // Lot 8 (40)
];

const BUILDINGS = [
  { x: 300, y: 50,  w: 150, h: 130, name: "Library" },
  { x: 40,  y: 222, w: 230, h: 60,  name: "Admin" },
  { x: 300, y: 200, w: 150, h: 82,  name: "Student Center" },
  { x: 545, y: 198, w: 205, h: 84,  name: "Science Hall" },
  { x: 775, y: 192, w: 190, h: 90,  name: "Gym" },
  { x: 40,  y: 512, w: 200, h: 100, name: "Arena" },
  { x: 725, y: 375, w: 240, h: 210, name: "Residence Halls" }
];

// Dev tab sliders: key -> settings. Each gets a big slider plus a number box.
const DEV_ROWS = {
  rowsTotal:  [{ k: "total", label: "Total", max: 500 }],
  rowsForced: [
    { k: "student", label: "Student", max: 300, chip: "student" },
    { k: "faculty", label: "Faculty", max: 300, chip: "faculty" },
    { k: "visitor", label: "Visitor", max: 300, chip: "visitor" },
    { k: "service", label: "Service", max: 300, chip: "service" },
    { k: "handicapped", label: "Handicapped", max: 100, chip: "handicap" }
  ],
  rowsMix: [
    { k: "car", label: "Car", max: 100, value: 75, icon: "car", pct: true },
    { k: "truck", label: "Truck", max: 100, value: 15, icon: "truck", pct: true },
    { k: "motorcycle", label: "Motorcycle", max: 100, value: 10, icon: "motorcycle", pct: true }
  ],
  rowsLeave: [{ k: "leave", label: "Cars to remove", max: 500 }]
};
const ADD_KEYS = ["total", "student", "faculty", "visitor", "service", "handicapped"];
const MIX = ["car", "truck", "motorcycle"];

const PRESETS = {
  rush:     { total: 400 },
  fill:     { total: 600 },
  students: { student: 300 },
  handicap: { handicapped: 40 },
  clear:    {}
};

let state = { version: -1, lots: [], log: [], stats: null, history: [], undo: null };
let openLot = -1;        // lot shown in the dialog
let pending = null;      // last park request (re-sent when the user picks a choice)
let lastForm = { type: "Car", designation: "student", handicapped: false, ev: false };

const $ = (id) => document.getElementById(id);
const svgNS = "http://www.w3.org/2000/svg";

// ---------- server calls ----------

async function api(path, params = {}) {
  const res = await fetch(path, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams(params).toString()
  });
  if (!res.ok) throw new Error("Server error " + res.status);
  return res.json();
}

async function refresh() {
  try {
    const res = await fetch("/api/state", { cache: "no-store" });
    state = await res.json();
  } catch (e) {
    $("campusSummary").textContent = "Can't reach the Java server. Start ParkingServer in IntelliJ, then reload.";
    return;
  }
  renderMap();
  renderLog();
  renderOccupancy();
  renderStats();
  renderChart();
  renderUndo();
  if (openLot >= 0) renderModalLot();
}

// auto-sync: check every 3 seconds whether anything changed (e.g. in another tab)
async function poll() {
  if (document.hidden) return;
  try {
    const res = await fetch("/api/version", { cache: "no-store" });
    const { version } = await res.json();
    if (version !== state.version) await refresh();
  } catch (e) { /* server stopped; refresh() shows the message on the next action */ }
}

// ---------- helpers ----------

function esc(s) {
  return String(s).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}
function icon(name, cls = "ico") {
  return `<svg class="${cls}" aria-hidden="true"><use href="#i-${name}"/></svg>`;
}
function chip(section) {
  return `<span class="chip c-${section}" title="${LABELS[section]}">${icon(section)}</span>`;
}
function fullness(lot) {
  const pct = lot.size ? lot.parked / lot.size : 0;
  return pct > 0.85 ? "bad" : pct >= 0.5 ? "warn" : "ok";
}
function toast(msg) {
  const t = $("toast");
  t.textContent = msg;
  t.hidden = false;
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => (t.hidden = true), 3200);
}
function el(tag, attrs = {}, parent) {
  const node = document.createElementNS(svgNS, tag);
  for (const [k, v] of Object.entries(attrs)) node.setAttribute(k, v);
  if (parent) parent.appendChild(node);
  return node;
}
function store(key, value) {
  try { localStorage.setItem(key, JSON.stringify(value)); } catch (e) { /* storage blocked */ }
}
function load(key, fallback) {
  try { const v = localStorage.getItem(key); return v ? JSON.parse(v) : fallback; } catch (e) { return fallback; }
}

// ---------- map ----------

function drawBase(svg) {
  el("rect", { x: 0, y: 0, width: 1000, height: 640, class: "grass" }, svg);
  [[470, 20], [290, 300], [455, 300], [710, 600], [980, 310], [20, 310], [250, 625], [520, 625], [960, 620], [275, 495]]
    .forEach(([cx, cy]) => el("circle", { cx, cy, r: 9, class: "tree" }, svg));
  el("rect", { x: 0, y: 300, width: 1000, height: 50, class: "road" }, svg);
  el("rect", { x: 475, y: 0, width: 50, height: 640, class: "road" }, svg);
  el("path", { d: "M0 325H475M525 325H1000", class: "road-line" }, svg);
  el("path", { d: "M500 0V300M500 350V640", class: "road-line" }, svg);
  const t1 = el("text", { x: 60, y: 330, class: "road-label" }, svg); t1.textContent = "CAMPUS DRIVE";
  const t2 = el("text", { x: 510, y: 160, class: "road-label", transform: "rotate(90 510 160)" }, svg); t2.textContent = "UNIVERSITY AVE";
  BUILDINGS.forEach((b) => {
    el("rect", { x: b.x, y: b.y, width: b.w, height: b.h, rx: 6, class: "building" }, svg);
    const t = el("text", { x: b.x + b.w / 2, y: b.y + b.h / 2, class: "building-label" }, svg);
    t.textContent = b.name;
  });
}

function renderMap() {
  const svg = $("map");
  if (!svg.dataset.base) {
    drawBase(svg);
    svg.dataset.base = "1";
  }
  hideTooltip();
  svg.querySelectorAll(".lot").forEach((n) => n.remove());

  let parked = 0, size = 0;
  state.lots.forEach((lot, i) => {
    const p = LOT_LAYOUT[i];
    if (!p) return;
    parked += lot.parked;
    size += lot.size;
    const g = el("g", {
      class: "lot " + fullness(lot), tabindex: 0, role: "button",
      "aria-label": `Lot ${i + 1}, ${lot.parked} of ${lot.size} spaces taken. Open to park a car.`,
      "data-lot": i
    }, svg);
    el("rect", { x: p.x, y: p.y, width: p.w, height: p.h, rx: 12, class: "lot-bg" }, g);
    el("rect", { x: p.x + 8, y: p.y + 8, width: p.w - 16, height: p.h - 16, rx: 6, fill: "url(#stalls)" }, g);
    const name = el("text", { x: p.x + p.w / 2, y: p.y + p.h / 2 - 4, class: "lot-name" }, g);
    name.textContent = "Lot " + (i + 1);
    const count = el("text", { x: p.x + p.w / 2, y: p.y + p.h / 2 + 16, class: "lot-count" }, g);
    count.textContent = `${lot.parked} / ${lot.size}`;
    const bw = p.w - 40;
    el("rect", { x: p.x + 20, y: p.y + p.h - 18, width: bw, height: 6, rx: 3, class: "lot-bar-bg" }, g);
    el("rect", { x: p.x + 20, y: p.y + p.h - 18, width: Math.max(0, bw * lot.parked / lot.size), height: 6, rx: 3, class: "lot-bar" }, g);

    g.addEventListener("mousemove", (e) => showTooltip(e, i));
    g.addEventListener("mouseleave", hideTooltip);
    g.addEventListener("focus", (e) => { if (g.matches(":focus-visible")) showTooltip(e, i); });
    g.addEventListener("blur", hideTooltip);
    g.addEventListener("click", () => openModal(i));
    g.addEventListener("keydown", (e) => {
      if (e.key === "Enter" || e.key === " ") { e.preventDefault(); openModal(i); }
    });
  });
  $("campusSummary").textContent = `${parked} of ${size} spaces taken across ${state.lots.length} lots`;
}

function openSpotsHtml(lot) {
  return SECTIONS.map((s) => {
    const n = lot.sections[s].open;
    return `<span class="tt-item${n === 0 ? " zero" : ""}">${chip(s)}${n}</span>`;
  }).join("");
}

function showTooltip(e, i) {
  const lot = state.lots[i];
  const tip = $("tooltip");
  tip.innerHTML = `<div class="tt-title">Lot ${i + 1}</div><div class="tt-grid">${openSpotsHtml(lot)}</div>`;
  tip.hidden = false;
  const wrap = $("mapWrap").getBoundingClientRect();
  let x, y;
  if (e.type === "mousemove") {
    x = e.clientX - wrap.left + 16;
    y = e.clientY - wrap.top + 16;
  } else {
    const r = e.currentTarget.getBoundingClientRect();
    x = r.left - wrap.left + r.width / 2;
    y = r.top - wrap.top + r.height / 2;
  }
  const tw = tip.offsetWidth, th = tip.offsetHeight;
  if (x + tw > wrap.width - 8) x = x - tw - 32;
  if (y + th > wrap.height - 8) y = y - th - 32;
  tip.style.left = Math.max(8, x) + "px";
  tip.style.top = Math.max(8, y) + "px";
}
function hideTooltip() { $("tooltip").hidden = true; }

function highlightLot(i) {
  const g = document.querySelector(`.lot[data-lot="${i}"]`);
  if (!g) return;
  g.classList.remove("highlight");
  void g.getBBox();
  g.classList.add("highlight");
  setTimeout(() => g.classList.remove("highlight"), 3800);
}

// ---------- lot dialog ----------

function applyLastForm() {
  const radio = document.querySelector(`input[name="type"][value="${lastForm.type}"]`);
  if (radio) radio.checked = true;
  $("fDesig").value = lastForm.designation;
  $("fHandicapped").checked = lastForm.handicapped;
  $("fEv").checked = lastForm.ev;
}

function openModal(i) {
  openLot = i;
  hideTooltip();
  pending = null;
  $("parkMsg").hidden = true;
  $("plateError").hidden = true;
  $("parkForm").reset();
  applyLastForm();
  renderModalLot();
  $("modal").hidden = false;
  $("fPlate").focus();
}
function closeModal() {
  $("modal").hidden = true;
  const g = document.querySelector(`.lot[data-lot="${openLot}"]`);
  openLot = -1;
  if (g) g.focus();
}

function renderStalls(lot) {
  const bySpace = {};
  lot.cars.forEach((c) => (bySpace[c.space] = c));
  $("stallGrid").innerHTML = [...lot.layout].map((letter, i) => {
    const c = bySpace[i];
    const sec = LABELS[LAYOUT_SECTION[letter]];
    const title = c ? `Space ${i}: ${sec}, ${c.plate} (${c.type})` : `Space ${i}: ${sec}, empty`;
    return `<span class="stall${c ? " taken" : ""}" data-sec="${letter}" title="${esc(title)}"></span>`;
  }).join("");
}

function renderModalLot() {
  const lot = state.lots[openLot];
  if (!lot) return;
  $("modalTitle").textContent = "Lot " + (openLot + 1);
  $("modalSub").textContent = `${lot.parked} of ${lot.size} spaces taken`;
  $("modalOpen").innerHTML = openSpotsHtml(lot);
  renderStalls(lot);
  $("carCount").textContent = `(${lot.cars.length})`;
  const table = $("carsTable");
  if (!lot.cars.length) {
    table.innerHTML = `<tr><td class="muted">No cars parked here yet.</td></tr>`;
    return;
  }
  table.innerHTML = lot.cars.map((c) => `
    <tr>
      <td class="plate">${esc(c.plate)}</td>
      <td><span class="icons">${icon(c.type.toLowerCase())}${chip(c.designation)}${c.handicapped ? chip("handicap") : ""}${c.ev ? icon("bolt") : ""}</span></td>
      <td><span class="icons muted">in ${chip(c.section)}</span></td>
      <td class="act"><button class="btn small" data-leave="${esc(c.plate)}">Leave</button></td>
    </tr>`).join("");
}

function plateProblem(p) {
  if (p === "") return "";
  if (!/^[A-Z0-9]{6}$/.test(p)) return "Plate must be exactly 6 letters or numbers (A-Z, 0-9), or blank for a random one.";
  return "";
}

// reads the form; returns null (and shows the error) if the plate is invalid
function readForm() {
  const plate = $("fPlate").value.trim().toUpperCase();
  const problem = plateProblem(plate);
  if (problem) {
    $("plateError").textContent = problem;
    $("plateError").hidden = false;
    $("fPlate").focus();
    return null;
  }
  lastForm = {
    type: document.querySelector('input[name="type"]:checked').value,
    designation: $("fDesig").value,
    handicapped: $("fHandicapped").checked,
    ev: $("fEv").checked
  };
  store("parking-form", lastForm);
  return { ...lastForm, plate, mode: "" };
}

async function submitPark(params) {
  pending = params;
  $("parkBtn").disabled = true;
  $("bestBtn").disabled = true;
  try {
    const r = await api("/api/park", params);
    handleParkResult(r);
  } catch (e) {
    showParkMsg("err", "<p>Couldn't reach the server. Is ParkingServer still running?</p>");
  } finally {
    $("parkBtn").disabled = false;
    $("bestBtn").disabled = false;
  }
}

function showParkMsg(kind, html) {
  const box = $("parkMsg");
  box.className = "park-msg " + kind;
  box.innerHTML = html;
  box.hidden = false;
}

function lotButtons(list, what) {
  return list.map((s) =>
    `<button type="button" class="btn small" data-move="${s.lot}">Lot ${s.lot + 1} (${s.open} ${what})</button>`).join("");
}

function handleParkResult(r) {
  if (r.status === "parked") {
    $("parkMsg").hidden = true;
    toast(r.message);
    const lot = r.lot;
    closeModal();
    refresh().then(() => highlightLot(lot));
    return;
  }
  if (r.status === "error") {
    if (/plate/i.test(r.message)) {
      $("plateError").textContent = r.message;
      $("plateError").hidden = false;
      $("fPlate").focus();
    } else {
      showParkMsg("err", `<p>${esc(r.message)}</p>`);
    }
    return;
  }
  if (r.status === "full") {
    const others = r.suggestions.length
      ? `<p>These lots still have room for your car:</p><div class="choices">${lotButtons(r.suggestions, "open")}</div>`
      : `<p>Every lot on campus is full for your car right now.</p>`;
    showParkMsg("warn", `<p><strong>${esc(r.message)}</strong></p>${others}`);
    refresh();
    return;
  }
  if (r.status === "handicapFull") {
    const advice = r.canParkHere
      ? "You can try another space in this lot, or move to a lot with open handicap spaces."
      : "There are no other spaces for your car in this lot. Move to a lot with open handicap spaces.";
    let html = `<p><strong>${esc(r.message)}</strong> ${advice}</p><div class="choices">`;
    if (r.canParkHere) html += `<button type="button" class="btn small primary" data-here="1">Use another space in Lot ${openLot + 1}</button>`;
    html += lotButtons(r.suggestions, "handicap open");
    html += `</div>`;
    if (!r.canParkHere && !r.suggestions.length) html = `<p><strong>${esc(r.message)}</strong> No other spaces are open for your car anywhere on campus.</p>`;
    showParkMsg("warn", html);
    refresh();
  }
}

async function leaveCar(plate, lotIdx) {
  const where = lotIdx >= 0 ? ` from Lot ${lotIdx + 1}` : "";
  if (!confirm(`Remove ${plate}${where}?`)) return null;
  const r = await api("/api/leave", { plate });
  if (r.status === "left") toast(r.message + " Use Undo to put it back.");
  await refresh();
  return r;
}

// ---------- side panel ----------

function renderLog() {
  const ol = $("log");
  ol.innerHTML = state.log.length
    ? state.log.map((l) => `<li>${esc(l)}</li>`).join("")
    : `<li class="empty">Nothing yet. Park a car to get started.</li>`;
}

function renderUndo() {
  const btn = $("undoBtn");
  btn.disabled = !state.undo;
  btn.title = state.undo ? "Undo: " + state.undo : "Nothing to undo";
  btn.setAttribute("aria-label", btn.title);
}

// ---------- Dev tab: stats, chart, tables ----------

function renderOccupancy() {
  const head = `<tr><th>Lot</th>${SECTIONS.map((s) => `<th>${chip(s)}</th>`).join("")}<th>Total</th></tr>`;
  const rows = state.lots.map((lot, i) => `<tr><td><strong>Lot ${i + 1}</strong></td>${
    SECTIONS.map((s) => {
      const sec = lot.sections[s];
      return `<td>${sec.total - sec.open}/${sec.total}</td>`;
    }).join("")}<td><strong>${lot.parked}/${lot.size}</strong></td></tr>`).join("");
  $("occTable").innerHTML = head + rows;
}

function renderStats() {
  const st = state.stats;
  if (!st) return;
  const pct = st.capacity ? Math.round((st.parked / st.capacity) * 100) : 0;
  const away = GROUPS.reduce((a, g) => a + st.turnedAway[g], 0);
  let most = -1, mostPct = -1;
  state.lots.forEach((l, i) => {
    const p = l.parked / l.size;
    if (p > mostPct) { mostPct = p; most = i; }
  });
  const tiles = [
    ["Cars parked", st.parked, `of ${st.capacity} spaces`],
    ["Campus full", pct + "%", `${st.capacity - st.parked} spaces open`],
    ["Turned away", away, "no room anywhere"],
    ["Most full lot", most >= 0 && st.parked ? "Lot " + (most + 1) : "None", most >= 0 && st.parked ? Math.round(mostPct * 100) + "% full" : "campus is empty"],
    ["EVs parked", st.ev, st.parked ? Math.round((st.ev / st.parked) * 100) + "% of cars" : "none yet"]
  ];
  $("metrics").innerHTML = tiles.map(([label, value, sub]) =>
    `<div class="metric"><div class="m-label">${label}</div><div class="m-value">${value}</div><div class="m-sub">${sub}</div></div>`).join("");
  $("groupTable").innerHTML =
    `<tr><th>Group</th><th>Parked</th><th>Turned away</th></tr>` +
    GROUPS.map((g) => `<tr><td><span class="gname">${chip(g)}${g === "handicap" ? "Handicapped" : LABELS[g]}</span></td><td>${st.parkedBy[g]}</td><td>${st.turnedAway[g]}</td></tr>`).join("");
  $("statsNote").textContent = "Turned away counts reset with Reset all lots";
}

function renderChart() {
  const svg = $("chart");
  svg.innerHTML = "";
  const W = 700, H = 220, L = 44, R = 12, T = 12, B = 28;
  const cap = state.stats ? state.stats.capacity : 500;
  const pts = state.history || [];
  [0, 25, 50, 75, 100].forEach((p) => {
    const y = T + (H - T - B) * (1 - p / 100);
    el("line", { x1: L, x2: W - R, y1: y, y2: y, class: "chart-grid" }, svg);
    const t = el("text", { x: L - 8, y: y + 4, class: "chart-axis", "text-anchor": "end" }, svg);
    t.textContent = p + "%";
  });
  if (pts.length < 2) {
    const t = el("text", { x: (L + W - R) / 2, y: H / 2, class: "chart-empty" }, svg);
    t.textContent = "Park or remove cars to see the line";
    return;
  }
  const x = (i) => L + ((W - L - R) * i) / (pts.length - 1);
  const y = (v) => T + (H - T - B) * (1 - v / cap);
  const line = pts.map((v, i) => `${i ? "L" : "M"}${x(i).toFixed(1)} ${y(v).toFixed(1)}`).join("");
  el("path", { d: `${line}L${x(pts.length - 1).toFixed(1)} ${y(0)}L${x(0)} ${y(0)}Z`, class: "chart-area" }, svg);
  el("path", { d: line, class: "chart-line" }, svg);
  el("circle", { cx: x(pts.length - 1), cy: y(pts[pts.length - 1]), r: 4, class: "chart-dot" }, svg);
  const first = el("text", { x: L, y: H - 8, class: "chart-axis" }, svg);
  first.textContent = "start";
  const last = el("text", { x: W - R, y: H - 8, class: "chart-axis", "text-anchor": "end" }, svg);
  last.textContent = `now (${Math.round((pts[pts.length - 1] / cap) * 100)}%)`;
}

// ---------- Dev tab: sliders ----------

function buildDevRows() {
  for (const [container, rows] of Object.entries(DEV_ROWS)) {
    $(container).innerHTML = rows.map((r) => {
      const lead = r.chip ? chip(r.chip) : r.icon ? icon(r.icon) : "";
      const pct = r.pct ? `<span class="pct" id="p-${r.k}"></span>` : "";
      const v = r.value || 0;
      return `<div class="slider-row">
        <label for="s-${r.k}">${lead}${r.label}${pct}</label>
        <input type="number" class="num" id="n-${r.k}" min="0" max="${r.max}" step="1" value="${v}" aria-label="${r.label} (exact number)">
        <input type="range" id="s-${r.k}" min="0" max="${r.max}" step="1" value="${v}">
      </div>`;
    }).join("");
    rows.forEach((r) => {
      const range = $("s-" + r.k), num = $("n-" + r.k);
      range.addEventListener("input", () => { num.value = range.value; updateDevOutputs(); });
      num.addEventListener("input", () => {
        if (num.value === "") return;
        const v = Math.max(0, Math.min(r.max, Math.round(+num.value) || 0));
        range.value = v;
        updateDevOutputs();
      });
      num.addEventListener("change", () => { num.value = range.value; });
    });
  }
}

function setSlider(k, v) {
  $("s-" + k).value = v;
  $("n-" + k).value = $("s-" + k).value;
}

function applyPreset(name) {
  const p = PRESETS[name];
  ADD_KEYS.forEach((k) => setSlider(k, p[k] || 0));
  if (name === "clear") { setSlider("car", 75); setSlider("truck", 15); setSlider("motorcycle", 10); }
  updateDevOutputs();
}

function updateDevOutputs() {
  // color the part of each slider track left of the thumb
  document.querySelectorAll('#view-dev input[type="range"]').forEach((s) => {
    s.style.setProperty("--fill", ((s.value - s.min) / (s.max - s.min)) * 100 + "%");
  });
  const count = ADD_KEYS.reduce((a, k) => a + +$("s-" + k).value, 0);
  const sum = MIX.reduce((a, k) => a + +$("s-" + k).value, 0);
  MIX.forEach((k) => {
    $("p-" + k).textContent = (sum ? Math.round((+$("s-" + k).value / sum) * 100) : 0) + "%";
  });
  $("mixNote").textContent = "Numbers are weights. The % next to each name is its share, so the shares always add to 100%.";
  $("devCount").textContent = count;
  $("leaveCount").textContent = $("s-leave").value;
}

async function devAdd() {
  const params = {};
  [...ADD_KEYS, ...MIX].forEach((k) => (params[k] = $("s-" + k).value));
  const total = ADD_KEYS.reduce((a, k) => a + +params[k], 0);
  if (total === 0) {
    $("devResult").textContent = "Move a slider above 0 first.";
    return;
  }
  if (MIX.every((k) => +params[k] === 0)) {
    $("devResult").textContent = "Give at least one vehicle type a share above 0%.";
    return;
  }
  $("devAdd").disabled = true;
  try {
    const r = await api("/api/dev/add", params);
    $("devResult").textContent = `Added ${r.added}: ${r.parked} parked, ${r.turnedAway} turned away.`;
    await refresh();
  } catch (e) {
    $("devResult").textContent = "Couldn't reach the server.";
  } finally {
    $("devAdd").disabled = false;
  }
}

async function devLeave() {
  const count = +$("s-leave").value;
  if (!count) { $("leaveDevResult").textContent = "Move the slider above 0 first."; return; }
  const r = await api("/api/dev/leave", { count });
  $("leaveDevResult").textContent = r.left ? `${r.left} cars left. Use Undo to bring them back.` : "There are no parked cars to remove.";
  await refresh();
}

async function devReset() {
  if (!confirm("Remove every car from every lot? This can't be undone.")) return;
  await api("/api/dev/reset");
  $("devResult").textContent = "All lots are empty.";
  await refresh();
}

// ---------- theme and tabs ----------

function setTheme(t) {
  document.documentElement.dataset.theme = t;
  const btn = $("themeBtn");
  btn.innerHTML = icon(t === "dark" ? "sun" : "moon");
  btn.setAttribute("aria-label", t === "dark" ? "Switch to light mode" : "Switch to dark mode");
  store("parking-theme", t);
}

function showView(name) {
  document.querySelectorAll(".tab").forEach((t) => {
    const on = t.dataset.view === name;
    t.classList.toggle("active", on);
    t.setAttribute("aria-selected", on);
  });
  $("view-map").hidden = name !== "map";
  $("view-dev").hidden = name !== "dev";
}

// ---------- wiring ----------

function init() {
  setTheme(load("parking-theme", "light"));
  lastForm = { ...lastForm, ...load("parking-form", {}) };
  $("themeBtn").addEventListener("click", () =>
    setTheme(document.documentElement.dataset.theme === "dark" ? "light" : "dark"));

  document.querySelectorAll(".tab").forEach((t) => t.addEventListener("click", () => showView(t.dataset.view)));

  $("undoBtn").addEventListener("click", async () => {
    const r = await api("/api/undo");
    toast(r.message);
    await refresh();
  });

  $("modalClose").addEventListener("click", closeModal);
  $("modal").addEventListener("click", (e) => { if (e.target === $("modal")) closeModal(); });
  document.addEventListener("keydown", (e) => { if (e.key === "Escape" && !$("modal").hidden) closeModal(); });

  $("fPlate").addEventListener("input", () => {
    $("fPlate").value = $("fPlate").value.toUpperCase();
    $("plateError").hidden = true;
  });

  $("parkForm").addEventListener("submit", (e) => {
    e.preventDefault();
    const f = readForm();
    if (!f) return;
    $("parkMsg").hidden = true;
    submitPark({ ...f, lot: openLot });
  });
  $("bestBtn").addEventListener("click", () => {
    const f = readForm();
    if (!f) return;
    $("parkMsg").hidden = true;
    submitPark({ ...f, lot: "best" });
  });

  // choices inside the "full" message, and Leave buttons in the car list
  $("modal").addEventListener("click", async (e) => {
    const move = e.target.closest("[data-move]");
    const here = e.target.closest("[data-here]");
    const leave = e.target.closest("[data-leave]");
    if (move && pending) {
      const lot = +move.dataset.move;
      openLot = lot;
      renderModalLot();
      submitPark({ ...pending, lot, mode: "" });
    } else if (here && pending) {
      submitPark({ ...pending, mode: "here" });
    } else if (leave) {
      await leaveCar(leave.dataset.leave, openLot);
    }
  });

  $("searchForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    const plate = $("searchPlate").value.trim().toUpperCase();
    const out = $("searchResult");
    if (!plate) { out.className = "result err"; out.textContent = "Enter a plate first."; return; }
    const r = await api("/api/search", { plate });
    out.className = "result " + (r.status === "found" ? "ok" : "err");
    out.textContent = r.message;
    if (r.status === "found") { showView("map"); highlightLot(r.lot); }
  });
  $("searchPlate").addEventListener("input", () => { $("searchResult").textContent = ""; });

  $("leaveForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    const plate = $("leavePlate").value.trim().toUpperCase();
    const out = $("leaveResult");
    if (!plate) { out.className = "result err"; out.textContent = "Enter a plate first."; return; }
    const r = await leaveCar(plate, -1);
    if (!r) return;
    out.className = "result " + (r.status === "left" ? "ok" : "err");
    out.textContent = r.message;
    if (r.status === "left") $("leavePlate").value = "";
  });
  $("leavePlate").addEventListener("input", () => { $("leaveResult").textContent = ""; });

  buildDevRows();
  document.querySelectorAll("[data-preset]").forEach((b) => b.addEventListener("click", () => applyPreset(b.dataset.preset)));
  $("devAdd").addEventListener("click", devAdd);
  $("devLeave").addEventListener("click", devLeave);
  $("devReset").addEventListener("click", devReset);
  updateDevOutputs();

  refresh();
  setInterval(poll, 3000);
}

init();
