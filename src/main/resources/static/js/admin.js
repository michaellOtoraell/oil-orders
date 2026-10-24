const $ = id => document.getElementById(id);
let jwt = localStorage.getItem('jwt');
let timer = null;
let lastNew = -1;
let lastOrdersSig = null;
let lastAnalyticsJson = null;
let analytics = null;
let period = localStorage.getItem('period') || 'daily';
let ordersAll = [];
let ordersPage = 1;
let customersAll = [];
const PAGE_SIZE = 15;

const TITLES = { overview: 'Muhtasari', orders: 'Oda', customers: 'Wateja', prices: 'Bei na Gharama' };
const STATUS = {
    NEW: { text: 'Mpya', pill: 'demo' },
    CONFIRMED: { text: 'Imethibitishwa', pill: 'info' },
    DELIVERED: { text: 'Imepelekwa', pill: 'active' },
    CANCELLED: { text: 'Imekataliwa', pill: 'inactive' }
};
const C = { navy: '#0a2f78', blue: '#1f6fc9', sky: '#3b86d9', pale: '#9cc3ee', gold: '#F7C325', goldDark: '#C98A00', goldPale: '#ffe27a' };
const PALETTE = [C.navy, C.gold, C.sky, C.goldDark, C.pale, '#195ba6', C.goldPale, '#144a87'];

const esc = s => String(s ?? '').replace(/[&<>"']/g, c =>
    ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const num = n => Math.round(Number(n) || 0).toLocaleString('en-US');
const money = n => 'TSh ' + num(n);
const initials = name => String(name || '?').trim().split(/\s+/).slice(0, 2).map(w => w[0]).join('').toUpperCase();

function toast(msg, type = 'success') {
    const el = document.createElement('div');
    el.className = 'toast-item ' + type;
    el.innerHTML = '<i class="bi ' + (type === 'error' ? 'bi-exclamation-circle' : 'bi-check-circle') + '"></i><span>' + esc(msg) + '</span>';
    $('toasts').appendChild(el);
    setTimeout(() => el.remove(), 3500);
}

async function api(path, opts = {}) {
    const res = await fetch('/api/admin' + path, {
        ...opts,
        headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + jwt, ...(opts.headers || {}) }
    });
    if (res.status === 401 || res.status === 403) { logout(); throw new Error('auth'); }
    return res;
}

function beep() {
    try {
        const c = new (window.AudioContext || window.webkitAudioContext)();
        const o = c.createOscillator();
        o.frequency.value = 880;
        o.connect(c.destination);
        o.start();
        setTimeout(() => { o.stop(); c.close(); }, 500);
    } catch (e) { /* browser imezuia sauti */ }
}

/* ======================= login / logout ======================= */
function logout() {
    localStorage.removeItem('jwt');
    jwt = null;
    clearInterval(timer);
    $('app').hidden = true;
    $('loginBox').hidden = false;
}

$('loginForm').onsubmit = async e => {
    e.preventDefault();
    const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: $('u').value, password: $('p').value })
    });
    if (!res.ok) { $('err').hidden = false; return; }
    $('err').hidden = true;
    jwt = (await res.json()).token;
    localStorage.setItem('jwt', jwt);
    start();
};
$('logoutBtn').onclick = logout;
$('revealBtn').onclick = () => {
    const show = $('p').type === 'password';
    $('p').type = show ? 'text' : 'password';
    $('revealBtn').innerHTML = '<i class="bi ' + (show ? 'bi-eye-slash' : 'bi-eye') + '"></i>';
};

function whoAmI() {
    try {
        const p = JSON.parse(atob(jwt.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
        const name = p.sub || 'Admin';
        $('whoName').textContent = name;
        $('avatar').textContent = name.charAt(0);
    } catch (e) { /* ignore */ }
}

/* ======================= sidebar / navigation ======================= */
function go(sec) {
    document.querySelectorAll('.sec').forEach(s => s.hidden = (s.id !== 'sec-' + sec));
    document.querySelectorAll('.side-link[data-sec]').forEach(a => a.classList.toggle('active', a.dataset.sec === sec));
    $('pageTitle').textContent = TITLES[sec] || '';
    document.body.classList.remove('sidebar-open');
    try { history.replaceState(null, '', '#' + sec); } catch (e) { /* ignore */ }
    window.scrollTo(0, 0);
}
document.querySelectorAll('.side-link[data-sec]').forEach(a => a.onclick = () => go(a.dataset.sec));

$('toggleBtn').onclick = () => {
    if (window.innerWidth < 992) {
        document.body.classList.toggle('sidebar-open');
    } else {
        const c = document.body.classList.toggle('sidebar-collapsed');
        try { localStorage.setItem('sideCollapsed', c ? '1' : '0'); } catch (e) { /* ignore */ }
    }
};
$('backdrop').onclick = () => document.body.classList.remove('sidebar-open');
try { if (localStorage.getItem('sideCollapsed') === '1') document.body.classList.add('sidebar-collapsed'); } catch (e) { /* ignore */ }

$('refreshBtn').onclick = () => { refresh(); loadAnalytics(true); loadCustomers(); toast('Imesasishwa'); };

/* ======================= kuanza ======================= */
function start() {
    $('loginBox').hidden = true;
    $('app').hidden = false;
    whoAmI();
    lastNew = -1;
    lastOrdersSig = null;
    lastAnalyticsJson = null;
    const hash = location.hash.replace('#', '');
    go(TITLES[hash] ? hash : 'overview');
    buildChartGrid();
    markPeriod();
    refresh();
    loadCustomers();
    loadPrices();
    clearInterval(timer);
    timer = setInterval(refresh, 10000);
}

function refresh() {
    loadOrders().catch(() => {});
    loadReport().catch(() => {});
}

/* ======================= kipindi (siku / wiki / mwezi) ======================= */
function markPeriod() {
    document.querySelectorAll('#periodToggle button').forEach(b => b.classList.toggle('active', b.dataset.p === period));
}
document.querySelectorAll('#periodToggle button').forEach(b => b.onclick = () => {
    period = b.dataset.p;
    try { localStorage.setItem('period', period); } catch (e) { /* ignore */ }
    markPeriod();
    loadAnalytics(true);
});

/* ======================= takwimu (cards + grafu) ======================= */
function fmtLabel(iso, p) {
    const [y, m, d] = iso.split('-').map(Number);
    const dt = new Date(y, m - 1, d);
    if (p === 'monthly') return dt.toLocaleDateString('sw-TZ', { month: 'short', year: '2-digit' });
    return dt.toLocaleDateString('sw-TZ', { day: 'numeric', month: 'short' });
}

function deltaHtml(cur, prev, inverse) {
    if (!prev && !cur) return '<div class="delta flat"><i class="bi bi-dash"></i> hakuna mabadiliko</div>';
    if (!prev) {
        return '<div class="delta ' + (inverse ? 'down' : 'up') + '"><i class="bi bi-arrow-up-right"></i> mpya <small>kipindi kilichopita 0</small></div>';
    }
    const pct = (cur - prev) / Math.abs(prev) * 100;
    if (Math.abs(pct) < 0.5) return '<div class="delta flat"><i class="bi bi-dash"></i> sawa <small>na kipindi kilichopita</small></div>';
    const up = pct > 0;
    const good = inverse ? !up : up;
    return '<div class="delta ' + (good ? 'up' : 'down') + '"><i class="bi ' + (up ? 'bi-arrow-up-right' : 'bi-arrow-down-right') +
        '"></i> ' + Math.abs(pct).toFixed(0) + '% <small>kipindi kilichopita</small></div>';
}

function renderKpis(a) {
    const c = a.current, p = a.previous;
    const defs = [
        { key: 'orders', label: 'Oda', icon: 'bi-receipt' },
        { key: 'dumuOrdered', label: 'Madumu yaliyoagizwa', icon: 'bi-box-seam' },
        { key: 'dumuDelivered', label: 'Madumu yaliyopelekwa', icon: 'bi-truck', sub: c.deliveryRate.toFixed(0) + '% ya oda zimepelekwa' },
        { key: 'newCustomers', label: 'Wateja wapya', icon: 'bi-person-plus' },
        { key: 'revenue', label: 'Mauzo', icon: 'bi-cash-stack', money: true },
        { key: 'cost', label: 'Gharama (pamoja na zawadi)', icon: 'bi-wallet2', money: true, inverse: true },
        { key: 'profit', label: 'Faida', icon: 'bi-graph-up-arrow', money: true, gold: true },
        { key: 'avgDumuPerOrder', label: 'Wastani wa dumu kwa oda', icon: 'bi-calculator', dec: true }
    ];
    $('kpis').innerHTML = defs.map(d => {
        const v = c[d.key], pv = p[d.key];
        const shown = d.money ? money(v) : d.dec ? Number(v).toFixed(1) : num(v);
        return '<div class="col-6 col-lg-4 col-xl-3"><div class="panel stat">' +
            '<i class="icon bi ' + d.icon + (d.gold ? ' gold' : '') + '"></i>' +
            '<div><div class="value' + (d.money ? ' sm' : '') + '">' + shown + '</div>' +
            '<div class="label">' + esc(d.label) + '</div>' +
            deltaHtml(v, pv, d.inverse) +
            (d.sub ? '<div class="sub">' + esc(d.sub) + '</div>' : '') +
            '</div></div></div>';
    }).join('');

    const n = a.rangeLength;
    const unit = a.period === 'daily' ? 'Siku ' + n : a.period === 'weekly' ? 'Wiki ' + n : 'Miezi ' + n;
    const prevUnit = a.period === 'daily' ? 'siku ' + n : a.period === 'weekly' ? 'wiki ' + n : 'miezi ' + n;
    $('rangeText').textContent = unit + ' za mwisho, zikilinganishwa na ' + prevUnit + ' zilizopita.' +
        (a.period === 'weekly' ? ' Wiki inaanza Jumatatu.' : '');
}

const KIND_NAMES = { line: 'Mstari', area: 'Eneo', bar: 'Nguzo', hist: 'Histogramu', hbar: 'Nguzo za mlalo', pie: 'Duara', doughnut: 'Donati', polar: 'Polar' };
const fromPoints = pts => ({ labels: pts.map(p => p.name), sets: [{ label: 'Idadi', data: pts.map(p => p.value) }] });

const CHARTS = [
    { id: 'orders', title: 'Oda na madumu', col: 'col-12 col-lg-6', kinds: ['bar', 'line', 'area', 'hist'], def: 'bar',
      data: a => ({ labels: a.labels.map(l => fmtLabel(l, a.period)), sets: [
          { label: 'Oda', data: a.orders, color: C.navy },
          { label: 'Madumu yaliyoagizwa', data: a.dumuOrdered, color: C.gold },
          { label: 'Madumu yaliyopelekwa', data: a.dumuDelivered, color: C.sky } ] }) },
    { id: 'customers', title: 'Wateja wapya', col: 'col-12 col-lg-6', kinds: ['bar', 'line', 'area', 'hist'], def: 'bar',
      data: a => ({ labels: a.labels.map(l => fmtLabel(l, a.period)), sets: [{ label: 'Wateja wapya', data: a.newCustomers, color: C.blue }] }) },
    { id: 'money', title: 'Mauzo, gharama na faida', col: 'col-12', tall: true, money: true, kinds: ['line', 'area', 'bar', 'hist'], def: 'line',
      data: a => ({ labels: a.labels.map(l => fmtLabel(l, a.period)), sets: [
          { label: 'Mauzo', data: a.revenue, color: C.navy },
          { label: 'Gharama', data: a.cost, color: C.sky },
          { label: 'Faida', data: a.profit, color: C.gold } ] }) },
    { id: 'status', title: 'Oda kwa hali', col: 'col-12 col-lg-6', kinds: ['doughnut', 'pie', 'polar', 'hbar'], def: 'doughnut',
      data: a => fromPoints(a.ordersByStatus) },
    { id: 'top', title: 'Wateja wakuu (madumu yaliyopelekwa)', col: 'col-12 col-lg-6', kinds: ['hbar', 'doughnut', 'pie', 'polar'], def: 'hbar',
      data: a => fromPoints(a.topCustomers) },
    { id: 'sizes', title: 'Ukubwa wa oda (histogramu ya madumu kwa oda)', col: 'col-12 col-lg-6', kinds: ['hist', 'bar', 'pie', 'doughnut'], def: 'hist',
      data: a => fromPoints(a.orderSizes) },
    { id: 'mix', title: 'Mgawanyo wa mauzo: gharama na faida', col: 'col-12 col-lg-6', money: true, kinds: ['doughnut', 'pie', 'bar'], def: 'doughnut',
      data: a => ({ labels: ['Gharama', 'Faida'], sets: [{ label: 'TSh', data: [a.current.cost, Math.max(a.current.profit, 0)] }] }) }
];
const charts = {};

function kindOf(def) {
    let k = null;
    try { k = localStorage.getItem('chartKind_' + def.id); } catch (e) { /* ignore */ }
    return def.kinds.includes(k) ? k : def.def;
}

function buildChartGrid() {
    $('chartGrid').innerHTML = CHARTS.map(d => `
        <div class="${d.col}">
            <div class="panel h-100">
                <div class="panel-head">
                    <h2>${esc(d.title)}</h2>
                    <select class="form-select" data-chart="${d.id}" aria-label="Aina ya grafu">
                        ${d.kinds.map(k => '<option value="' + k + '"' + (k === kindOf(d) ? ' selected' : '') + '>' + KIND_NAMES[k] + '</option>').join('')}
                    </select>
                </div>
                <div class="panel-body">
                    <div class="chart-box${d.tall ? ' tall' : ''}" id="box-${d.id}">
                        <canvas id="cv-${d.id}"></canvas>
                        <div class="chart-empty">Hakuna data kwa kipindi hiki</div>
                    </div>
                </div>
            </div>
        </div>`).join('');
    document.querySelectorAll('#chartGrid select[data-chart]').forEach(sel => sel.onchange = () => {
        try { localStorage.setItem('chartKind_' + sel.dataset.chart, sel.value); } catch (e) { /* ignore */ }
        const def = CHARTS.find(c => c.id === sel.dataset.chart);
        if (analytics && def) drawChart(def, analytics);
    });
}

const alpha = (hex, a) => {
    const n = parseInt(hex.slice(1), 16);
    return 'rgba(' + (n >> 16) + ',' + ((n >> 8) & 255) + ',' + (n & 255) + ',' + a + ')';
};
const shortNum = v => v >= 1e6 ? (v / 1e6).toFixed(v % 1e6 ? 1 : 0) + 'M' : v >= 1e3 ? (v / 1e3).toFixed(v % 1e3 ? 1 : 0) + 'K' : v;

function buildConfig(def, kind, d) {
    const circle = ['pie', 'doughnut', 'polar'].includes(kind);
    const horizontal = kind === 'hbar';
    const line = kind === 'line' || kind === 'area';

    const tooltip = {
        callbacks: {
            label: ctx => {
                const p = ctx.parsed;
                const v = typeof p === 'number' ? p : (p.r !== undefined ? p.r : (horizontal ? p.x : p.y));
                const name = ctx.dataset.label && !circle ? ctx.dataset.label + ': ' : (circle ? ctx.label + ': ' : '');
                return name + (def.money ? money(v) : num(v));
            }
        }
    };

    if (circle) {
        const set = d.sets[0];
        return {
            type: kind === 'polar' ? 'polarArea' : kind,
            data: { labels: d.labels, datasets: [{ data: set.data,
                backgroundColor: d.labels.map((_, i) => PALETTE[i % PALETTE.length]), borderColor: '#fff', borderWidth: 2 }] },
            options: { responsive: true, maintainAspectRatio: false, cutout: kind === 'doughnut' ? '62%' : undefined,
                plugins: { legend: { position: 'bottom' }, tooltip } }
        };
    }

    const datasets = d.sets.map((s, i) => {
        const color = s.color || PALETTE[i % PALETTE.length];
        const base = { label: s.label, data: s.data, borderColor: color };
        if (line) {
            return { ...base, backgroundColor: kind === 'area' ? alpha(color, .18) : color, fill: kind === 'area',
                tension: .35, borderWidth: 2.5, pointRadius: 3, pointBackgroundColor: color };
        }
        const multi = d.sets.length > 1;
        const fillColors = s.color ? alpha(color, kind === 'hist' ? .75 : .92) : d.labels.map((_, k) => PALETTE[k % PALETTE.length]);
        return { ...base, backgroundColor: fillColors, borderWidth: 1, borderRadius: kind === 'hist' ? 0 : 5,
            categoryPercentage: kind === 'hist' ? 1 : (multi ? .8 : .7), barPercentage: kind === 'hist' ? 1 : .9 };
    });

    const valueAxis = { beginAtZero: true, grid: { color: 'rgba(0,0,0,.05)' },
        ticks: { precision: 0, callback: v => def.money ? shortNum(v) : v } };
    const catAxis = { grid: { display: false } };
    return {
        type: line ? 'line' : 'bar',
        data: { labels: d.labels, datasets },
        options: {
            responsive: true, maintainAspectRatio: false, indexAxis: horizontal ? 'y' : 'x',
            interaction: { mode: 'index', intersect: false },
            plugins: { legend: { display: d.sets.length > 1, position: 'bottom' }, tooltip },
            scales: horizontal ? { x: valueAxis, y: catAxis } : { x: catAxis, y: valueAxis }
        }
    };
}

function drawChart(def, a) {
    const box = $('box-' + def.id);
    if (typeof Chart === 'undefined') {
        box.classList.add('is-empty');
        box.querySelector('.chart-empty').textContent = 'Grafu hazikupakia. Angalia mtandao.';
        return;
    }
    const d = def.data(a);
    const empty = d.sets.every(s => s.data.every(v => !v));
    if (charts[def.id]) { charts[def.id].destroy(); delete charts[def.id]; }
    box.classList.toggle('is-empty', empty);
    if (empty) return;
    charts[def.id] = new Chart($('cv-' + def.id), buildConfig(def, kindOf(def), d));
}

async function loadAnalytics(force) {
    const res = await api('/analytics?period=' + encodeURIComponent(period));
    if (!res.ok) return;
    const a = await res.json();
    const json = JSON.stringify(a);
    if (!force && json === lastAnalyticsJson) return;
    lastAnalyticsJson = json;
    analytics = a;
    renderKpis(a);
    CHARTS.forEach(def => drawChart(def, a));
}

if (typeof Chart !== 'undefined') {
    Chart.defaults.font.family = '"Plus Jakarta Sans", system-ui, sans-serif';
    Chart.defaults.color = '#5d6878';
}

/* ======================= hali ya sasa (report) ======================= */
async function loadReport() {
    const res = await api('/report');
    if (!res.ok) return;
    const r = await res.json();
    $('nowPending').textContent = num(r.pendingDumu);
    $('nowExpected').textContent = money(r.expectedProfit);
}

/* ======================= pager ======================= */
function renderPager(el, page, pages, goTo) {
    if (pages <= 1) { el.innerHTML = ''; return; }
    const items = [];
    const add = (label, p, disabled, active) => items.push(
        '<li class="page-item' + (disabled ? ' disabled' : '') + (active ? ' active' : '') + '"><a class="page-link" data-p="' + p + '">' + label + '</a></li>');
    add('&laquo;', page - 1, page === 1);
    for (let i = Math.max(1, page - 2); i <= Math.min(pages, page + 2); i++) add(i, i, false, i === page);
    add('&raquo;', page + 1, page === pages);
    el.innerHTML = items.join('');
    el.querySelectorAll('a[data-p]').forEach(a => a.onclick = () => {
        const p = Number(a.dataset.p);
        if (p >= 1 && p <= pages && p !== page) goTo(p);
    });
}

/* ======================= oda ======================= */
async function loadOrders() {
    const res = await api('/orders');
    if (!res.ok) return;
    ordersAll = await res.json();

    const newCount = ordersAll.filter(o => o.status === 'NEW').length;
    if (lastNew >= 0 && newCount > lastNew) { beep(); toast('Oda mpya imeingia'); }
    lastNew = newCount;
    $('navNew').textContent = newCount;
    $('navNew').hidden = newCount === 0;
    $('nowNew').textContent = newCount;
    document.title = (newCount ? '(' + newCount + ') ' : '') + 'Dashibodi - Ottoil';

    renderOrders();

    const sig = ordersAll.map(o => o.id + o.status).join(',');
    if (sig !== lastOrdersSig) {
        lastOrdersSig = sig;
        loadAnalytics(false).catch(() => {});
    }
}

function renderOrders() {
    const q = $('ordersSearch').value.trim().toLowerCase();
    const st = $('ordersStatus').value;
    const list = ordersAll.filter(o =>
        (!st || o.status === st) &&
        (!q || [o.customerName, o.customerPhone, o.customerLocation].some(x => String(x || '').toLowerCase().includes(q))));
    const pages = Math.max(1, Math.ceil(list.length / PAGE_SIZE));
    if (ordersPage > pages) ordersPage = pages;
    const slice = list.slice((ordersPage - 1) * PAGE_SIZE, ordersPage * PAGE_SIZE);

    $('ordersBody').innerHTML = slice.map(o => {
        const s = STATUS[o.status] || { text: o.status, pill: 'inactive' };
        return `<tr>
            <td data-label="Mteja"><div class="org-cell"><span class="org-logo">${esc(initials(o.customerName))}</span>
                <div><a href="tel:${esc(o.customerPhone)}">${esc(o.customerName)}</a><small>${esc(o.customerPhone)}</small>
                ${o.rewardsAvailable > 0 ? '<span class="pill gold mt-1">Zawadi: lita ' + o.rewardsAvailable + ' bure</span>' : ''}</div></div></td>
            <td data-label="Dumu"><strong>${o.quantity}</strong> <small class="text-muted">(lita ${o.quantity * 20})</small></td>
            <td data-label="Eneo">${esc(o.customerLocation)}</td>
            <td data-label="Muda" class="small text-nowrap">${new Date(o.createdAt).toLocaleString('sw-TZ')}</td>
            <td data-label="Hali"><span class="pill ${s.pill}">${esc(s.text)}</span></td>
            <td class="cell-actions text-nowrap">${actions(o)}</td>
        </tr>`;
    }).join('') || '<tr><td colspan="6"><div class="empty"><i class="bi bi-inbox"></i><strong>Hakuna oda</strong>Oda zitaonekana hapa zikiingia.</div></td></tr>';

    $('ordersInfo').textContent = list.length ? 'Oda ' + list.length + ' kwa jumla' : '';
    renderPager($('ordersPager'), ordersPage, pages, p => { ordersPage = p; renderOrders(); });
}

function actions(o) {
    if (o.status === 'NEW') {
        return '<button class="btn btn-primary btn-sm" onclick="setStatus(' + o.id + ',\'CONFIRMED\')">Thibitisha</button> ' +
               '<button class="btn btn-light-line btn-sm" onclick="setStatus(' + o.id + ',\'CANCELLED\')">Kataa</button>';
    }
    if (o.status === 'CONFIRMED') {
        return '<button class="btn btn-accent btn-sm" onclick="setStatus(' + o.id + ',\'DELIVERED\')">Imepelekwa</button>';
    }
    return '';
}

async function setStatus(id, value) {
    const res = await api('/orders/' + id + '/status?value=' + value, { method: 'PUT' });
    if (!res.ok) { toast('Imeshindikana kubadilisha hali', 'error'); return; }
    toast('Hali ya oda imebadilishwa');
    refresh();
}

$('ordersSearch').addEventListener('input', () => { ordersPage = 1; renderOrders(); });
$('ordersStatus').addEventListener('change', () => { ordersPage = 1; renderOrders(); });

/* ======================= wateja ======================= */
async function loadCustomers() {
    const res = await api('/customers');
    if (!res.ok) return;
    customersAll = await res.json();
    $('nowCustomers').textContent = num(customersAll.length);
    renderCustomers();
}

function renderCustomers() {
    const q = $('custSearch').value.trim().toLowerCase();
    const list = customersAll.filter(c => !q || [c.name, c.phone, c.location].some(x => String(x || '').toLowerCase().includes(q)));
    $('custBody').innerHTML = list.map(c => {
        const pct = Math.round(c.progress / 6 * 100);
        return `<tr>
            <td data-label="Mteja"><div class="org-cell"><span class="org-logo">${esc(initials(c.name))}</span><div><strong>${esc(c.name)}</strong></div></div></td>
            <td data-label="Simu"><a href="tel:${esc(c.phone)}">${esc(c.phone)}</a></td>
            <td data-label="Eneo">${esc(c.location)}</td>
            <td data-label="Stempu" style="min-width:170px">
                <div class="dist-row"><div class="top"><span>${c.progress}/6</span><span>jumla ${c.totalDumu}</span></div>
                <div class="bar"><div style="width:${pct}%"></div></div></div>
                ${c.rewardsAvailable > 0 ? '<span class="pill gold mt-2">Zawadi ' + c.rewardsAvailable + '</span>' : ''}</td>
            <td class="cell-actions text-nowrap">
                ${c.rewardsAvailable > 0 ? '<button class="btn btn-accent btn-sm" onclick="redeem(' + c.id + ')">Nimempa zawadi</button> ' : ''}
                <button class="btn btn-light-line btn-sm" onclick="openCard(${c.id})"><i class="bi bi-qr-code"></i> Kadi ya QR</button></td>
        </tr>`;
    }).join('') || '<tr><td colspan="5"><div class="empty"><i class="bi bi-people"></i><strong>Hakuna wateja</strong>Sajili mteja wa kwanza hapo juu.</div></td></tr>';
}
$('custSearch').addEventListener('input', renderCustomers);

$('custForm').onsubmit = async e => {
    e.preventDefault();
    const res = await api('/customers', {
        method: 'POST',
        body: JSON.stringify({ name: $('cName').value, phone: $('cPhone').value, location: $('cLoc').value })
    });
    if (!res.ok) { toast('Imeshindikana kusajili mteja', 'error'); return; }
    e.target.reset();
    toast('Mteja amesajiliwa');
    loadCustomers();
    loadAnalytics(true);
};

async function redeem(id) {
    if (!confirm('Umempa mteja lita 1 bure?')) return;
    const res = await api('/customers/' + id + '/redeem', { method: 'POST' });
    if (!res.ok) { toast('Hakuna zawadi inayosubiri', 'error'); return; }
    toast('Zawadi imerekodiwa');
    loadCustomers();
    refresh();
    loadAnalytics(true);
}

function openCard(id) {
    window.open('/card.html?id=' + id, '_blank');
}

/* ======================= bei na gharama ======================= */
function previewPrices() {
    const buy = Number($('pBuy').value) || 0;
    const extra = Number($('pExtra').value) || 0;
    const sell = Number($('pSell').value) || 0;
    const revenue = sell * 20;
    const profit = revenue - buy - extra;
    const pct = revenue > 0 ? (profit / revenue * 100).toFixed(1) : '0.0';
    $('pPreview').innerHTML = '<div>' +
        'Mauzo kwa dumu: <b>' + money(revenue) + '</b><br>' +
        'Gharama kwa dumu: <b>' + money(buy + extra) + '</b><br>' +
        'Faida kwa dumu: <b style="color:' + (profit >= 0 ? '#067647' : '#b42318') + '">' + money(profit) + '</b> (' + pct + '%)<br>' +
        'Faida kwa lita: <b>' + money(profit / 20) + '</b></div>';
}
['pBuy', 'pExtra', 'pSell'].forEach(id => $(id).addEventListener('input', previewPrices));

async function loadPrices() {
    const res = await api('/prices');
    if (!res.ok) return;
    const list = await res.json();
    if (!list.length) return;
    const cur = list[0];
    if (!['pBuy', 'pExtra', 'pSell'].includes(document.activeElement && document.activeElement.id)) {
        $('pBuy').value = cur.buyPricePerDumu;
        $('pExtra').value = cur.extraCostPerDumu;
        $('pSell').value = cur.sellPricePerLitre;
        previewPrices();
    }
    $('priceBody').innerHTML = list.map((p, i) => `
        <tr>
            <td data-label="Tarehe" class="small text-nowrap">${new Date(p.createdAt).toLocaleDateString('sw-TZ')}${i === 0 ? ' <span class="pill gold">sasa</span>' : ''}</td>
            <td data-label="Kununua">${num(p.buyPricePerDumu)}</td>
            <td data-label="Nyingine">${num(p.extraCostPerDumu)}</td>
            <td data-label="Kuuza/L">${num(p.sellPricePerLitre)}</td>
            <td data-label="Faida/dumu"><strong>${num(p.profitPerDumu)}</strong></td>
        </tr>`).join('');
}

$('priceForm').onsubmit = async e => {
    e.preventDefault();
    const res = await api('/prices', {
        method: 'POST',
        body: JSON.stringify({
            buyPricePerDumu: Number($('pBuy').value),
            extraCostPerDumu: Number($('pExtra').value),
            sellPricePerLitre: Number($('pSell').value)
        })
    });
    const data = await res.json().catch(() => ({}));
    if (res.ok) {
        toast('Bei mpya zimehifadhiwa');
        if (document.activeElement) document.activeElement.blur();
        loadPrices();
        loadReport();
    } else {
        toast(data.message || 'Imeshindikana kuhifadhi bei', 'error');
    }
};

if (jwt) start();
