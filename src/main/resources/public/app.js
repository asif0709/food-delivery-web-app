let cart=[];
async function loadMenu(){const r=await fetch('/api/menu');const items=await r.json();const el=document.getElementById('menu');
el.innerHTML=items.map(x=>x.error?'<p>'+x.error+'</p>':'<div class="item"><b>'+x.name+'</b><p>'+x.category+' · ₹'+x.price.toFixed(2)+'</p><button onclick="add('+JSON.stringify(x).replace(/"/g,'&quot;')+')">Add</button></div>').join('');}
function add(x){cart.push(x);renderCart();}
function renderCart(){document.getElementById('cart').innerHTML=cart.length?cart.map(x=>'<p>'+x.name+' — ₹'+x.price.toFixed(2)+'</p>').join(''):'<p>Your cart is empty.</p>';}
async function placeOrder(){if(!cart.length)return alert('Add an item first.');const customer=document.getElementById('customer').value.trim();if(!customer)return alert('Enter your name.');
const total=cart.reduce((s,x)=>s+x.price,0);const items=cart.map(x=>x.name).join(', ');const r=await fetch('/api/orders',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({customerName:customer,items,total})});
document.getElementById('status').textContent=r.ok?'Order saved successfully.':'Could not save order.';}
loadMenu();renderCart();