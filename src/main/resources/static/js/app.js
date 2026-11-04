// BikeStore Async v1.0 - Frontend
const API_URL = '/orders'; // mismo origen: el frontend se sirve desde Spring Boot

const BIKES = [
  { id: 1, nombre: 'Bicicleta Trek Marlin', tipo: 'Montaña', precio: 850.00, img: 'images/trek-marlin.jpg',
    desc: 'Cuadro de aluminio ligero y suspensión delantera para senderos exigentes.' },
  { id: 2, nombre: 'Bicicleta Specialized Rockhopper', tipo: 'Montaña', precio: 920.00, img: 'images/rockhopper.jpg',
    desc: 'Resistente y cómoda, ideal para iniciarse en el trail con confianza.' },
  { id: 3, nombre: 'Bicicleta Giant Contend', tipo: 'Ruta', precio: 1100.00, img: 'images/giant-contend.jpg',
    desc: 'Geometría aerodinámica para devorar kilómetros de asfalto con eficiencia.' },
  { id: 4, nombre: 'Bicicleta Cannondale Quick', tipo: 'Urbana', precio: 780.00, img: 'images/cannondale-quick.jpg',
    desc: 'Ágil y versátil para la ciudad, el trabajo y los paseos de fin de semana.' }
];

const $ = (id) => document.getElementById(id);
const fmt = (n) => new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(n);

// Imagen de respaldo (SVG) si todavía no agregaste las fotos a /images
const fallback = (name) => 'data:image/svg+xml;utf8,' + encodeURIComponent(
  `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 300"><rect width="400" height="300" fill="#e9efeb"/>
   <g fill="none" stroke="#16a34a" stroke-width="6" stroke-linecap="round"><circle cx="110" cy="190" r="48"/><circle cx="290" cy="190" r="48"/>
   <path d="M110 190l55-80h90l35 80M165 110l45 80M150 95h35M255 110l-12-22h28"/></g>
   <text x="200" y="285" font-family="sans-serif" font-size="14" text-anchor="middle" fill="#6b726e">${name}</text></svg>`);

// ---- Catálogo ----
function renderCatalog() {
  $('catalogo').innerHTML = BIKES.map(b => `
    <article class="card reveal">
      <div class="card-img"><img src="${b.img}" alt="${b.nombre}" loading="lazy" data-name="${b.nombre}"></div>
      <div class="card-body">
        <span class="tag">${b.tipo}</span>
        <h3>${b.nombre.replace('Bicicleta ', '')}</h3>
        <p>${b.desc}</p>
        <div class="card-foot">
          <span class="price">${fmt(b.precio)}</span>
          <button class="btn btn-small" data-id="${b.id}">Comprar</button>
        </div>
      </div>
    </article>`).join('');
  document.querySelectorAll('.card-img img').forEach(img => {
    img.addEventListener('error', () => { img.src = fallback(img.dataset.name); }, { once: true });
  });
}

// ---- Formulario ----
function fillSelect() {
  $('producto').innerHTML = BIKES.map(b => `<option value="${b.nombre}">${b.nombre}</option>`).join('');
  updatePrice();
}
function updatePrice() {
  const bike = BIKES.find(b => b.nombre === $('producto').value);
  $('precio').value = bike ? fmt(bike.precio) : '';
}
function showError(msg) {
  const el = $('formError');
  el.textContent = msg; el.hidden = !msg;
}

async function submitOrder(e) {
  e.preventDefault();
  showError('');
  const cliente = $('cliente').value.trim();
  const correo = $('correo').value.trim();
  const bike = BIKES.find(b => b.nombre === $('producto').value);

  if (cliente.length < 2) return showError('Ingresa tu nombre.');
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo)) return showError('Ingresa un correo válido.');

  const btn = $('submitBtn');
  btn.disabled = true; btn.textContent = 'Enviando pedido...';

  try {
    const res = await fetch(API_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ cliente, correo, producto: bike.nombre, precio: bike.precio })
    });
    if (!res.ok) throw new Error(`El servidor respondió con el código ${res.status}`);
    const data = await res.json();
    showConfirmation(data); // se muestra exactamente lo que devuelve la API
  } catch (err) {
    showError('No se pudo enviar el pedido. ' + err.message);
  } finally {
    btn.disabled = false; btn.textContent = 'Confirmar compra';
  }
}

function showConfirmation(data) {
  $('confMensaje').textContent = data.mensaje ?? 'Pedido recibido para procesamiento';
  $('confId').textContent = data.pedidoId;
  $('confStatus').textContent = data.paymentStatus;
  $('orderForm').hidden = true;
  $('confirmation').hidden = false;
}

function resetOrder() {
  $('orderForm').reset(); updatePrice();
  $('confirmation').hidden = true;
  $('orderForm').hidden = false;
}

// ---- Eventos ----
document.addEventListener('DOMContentLoaded', () => {
  renderCatalog();
  fillSelect();

  $('producto').addEventListener('change', updatePrice);
  $('orderForm').addEventListener('submit', submitOrder);
  $('newOrder').addEventListener('click', resetOrder);

  // Botón "Comprar" de cada tarjeta: selecciona la bici y baja al formulario
  $('catalogo').addEventListener('click', (e) => {
    const btn = e.target.closest('button[data-id]');
    if (!btn) return;
    const bike = BIKES.find(b => b.id === Number(btn.dataset.id));
    resetOrder();
    $('producto').value = bike.nombre; updatePrice();
    $('comprar').scrollIntoView({ behavior: 'smooth' });
    setTimeout(() => $('cliente').focus({ preventScroll: true }), 700);
  });

  // Menú móvil
  const toggle = $('navToggle'), links = $('navLinks');
  toggle.addEventListener('click', () => {
    const open = links.classList.toggle('open');
    toggle.setAttribute('aria-expanded', open);
  });
  links.addEventListener('click', () => links.classList.remove('open'));

  // Animaciones al hacer scroll
  const io = new IntersectionObserver((entries) => {
    entries.forEach(en => { if (en.isIntersecting) { en.target.classList.add('visible'); io.unobserve(en.target); } });
  }, { threshold: .12 });
  document.querySelectorAll('.reveal').forEach(el => io.observe(el));
});
