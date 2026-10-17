'use strict';
/** Adaptador web de demostración. No constituye autenticación de servidor. */
const SUBJECT = 'Programación Orientada a Objetos';
const DEMO_USERS = [
  {id:'E01',name:'Valentina Ríos',email:'valentina@uco.demo',role:'student'},
  {id:'E02',name:'Samuel Gómez',email:'samuel@uco.demo',role:'student'},
  {id:'E03',name:'Luciana Vélez',email:'luciana@uco.demo',role:'student'},
  {id:'P01',name:'Andrés Moreno',email:'andres@uco.demo',role:'professor'}
];
let USERS = [...DEMO_USERS];
const BADGES = [
  {name:'Primer despegue',icon:'↗',rule:'Registra tu primera evaluación.'},
  {name:'Órbita de excelencia',icon:'✧',rule:'Mantén un promedio general de 4.0 o más.'},
  {name:'Cerebrito UCO',icon:'✳',rule:'Alcanza un promedio general de 4.5 o más.'},
  {name:'Master Code',icon:'⌘',rule:'Completa 3 evaluaciones de POO con promedio de 4.5 o más.'}
];
const $ = (selector) => document.querySelector(selector);
const escapeHTML = (value) => String(value).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const format = n => n == null ? '—' : n.toFixed(2);
// Búsqueda tolerante a tildes y diferencias entre mayúsculas/minúsculas (Física = fisica).
const normalizeSearch = value => String(value ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase('es').trim();
let user = null, selectedRole = 'student', currentView = 'overview', toastTimer, chatPollTimer = null, unreadPollTimer = null, selectedChatId = null, chatPeople = [], unreadByContact = {};
let state = {grades:[],history:[],badges:{},globalStats:{studentCount:0,gradeCount:0,average:null}}, storageAvailable = true, apiToken = null;
async function api(path, options={}) {
  const headers = {'Content-Type':'application/json', ...(options.headers||{})};
  if(apiToken) headers['X-Orbita-Token'] = apiToken;
  const response = await fetch(path, {...options, headers});
  const payload = await response.json().catch(()=>({error:'Respuesta inválida del servidor'}));
  if(!response.ok) throw new Error(payload.error || 'No fue posible completar la operación');
  return payload;
}
async function refreshState() {
  const data = await api('/api/state');
  state = {grades:data.grades||[],history:data.history||[],badges:data.badges||{},globalStats:data.globalStats||{studentCount:0,gradeCount:0,average:null}};
  USERS = data.users || USERS;
  if (user && !USERS.some(u => u.id === user.id)) USERS = [...USERS, user];
  user = data.user || user;
}
function persist() { /* La persistencia pertenece al servidor Java. */ }
function notify(text) { clearTimeout(toastTimer);$('#toast').textContent=text;$('#toast').hidden=false;toastTimer=setTimeout(()=>$('#toast').hidden=true,5000); }
function log(text) { /* La bitácora se registra en Java, nunca en localStorage. */ }
function gradesFor(id) { return state.grades.filter(g=>g.studentId===id); }
function average(grades) { return grades.length ? grades.reduce((sum,g)=>sum+g.value,0)/grades.length : null; }
function earned(id) {
  const active=state.badges?.[id]||[];
  return BADGES.map(b=>active.includes(b.name));
}
function setRole(role) {
  selectedRole=role;
  document.querySelectorAll('[data-role]').forEach(b=>{b.classList.toggle('selected',b.dataset.role===role);b.setAttribute('aria-pressed',String(b.dataset.role===role));});
  const sample=DEMO_USERS.find(u=>u.role===role);
  $('#demo-login').innerHTML=`Usar cuenta de ${role==='student'?'estudiante':'profesor'} <span>→</span>`;
  $('#demo-credentials').textContent=`${sample.email} · Orbita2026!`;
  $('#login-error').textContent='';
}
document.querySelectorAll('[data-role]').forEach(b=>b.addEventListener('click',()=>setRole(b.dataset.role)));
$('#toggle-password').addEventListener('click',()=>{
  const visible=$('#password').type==='password';$('#password').type=visible?'text':'password';
  $('#toggle-password').textContent=visible?'Ocultar':'Ver';$('#toggle-password').setAttribute('aria-label',visible?'Ocultar contraseña':'Mostrar contraseña');
});
$('#demo-login').addEventListener('click',()=>{ $('#email').value=DEMO_USERS.find(u=>u.role===selectedRole).email;$('#password').value='Orbita2026!';$('#login-form button[type=submit]').focus(); });
$('#show-register').addEventListener('click',()=>{ $('#login-form').hidden=true;$('#register-form').hidden=false;$('#demo-box').hidden=true;$('#login-error').textContent='';$('#register-name').focus(); });
$('#show-login').addEventListener('click',()=>{ $('#register-form').hidden=true;$('#login-form').hidden=false;$('#demo-box').hidden=false;$('#register-error').textContent='';$('#email').focus(); });
function updateRegisterFields(){
  const isStudent=$('#register-role').value==='student';
  $('#student-fields').hidden=!isStudent;$('#professor-fields').hidden=isStudent;
  $('#register-career').required=isStudent;
  $('#register-career-professor').required=!isStudent;
  $('#register-subject').required=!isStudent;
  $('#register-teacher-code').required=!isStudent;
  $('#register-document-type').innerHTML=isStudent?'<option value="TI">Tarjeta de identidad (TI)</option><option value="CC">Cédula de ciudadanía (CC)</option>':'<option value="CC">Cédula de ciudadanía (CC)</option>';
}
$('#register-role').addEventListener('change',updateRegisterFields);
updateRegisterFields();
$('#register-form').addEventListener('submit',async event=>{
  event.preventDefault();
  const name=$('#register-name').value.trim(),email=$('#register-email').value.trim(),role=$('#register-role').value;
  const password=$('#register-password').value,confirm=$('#register-confirm').value;
  const documentType=$('#register-document-type').value,documentNumber=$('#register-document-number').value.trim();
  const career=role==='student'?$('#register-career').value:$('#register-career-professor').value.trim();
  const subject=role==='professor'?$('#register-subject').value:'';
  const teacherCode=role==='professor'?$('#register-teacher-code').value:'';
  $('#register-error').textContent='';
  if(password!==confirm){$('#register-error').textContent='Las contraseñas no coinciden.';return;}
  if(password.length<8){$('#register-error').textContent='La contraseña debe tener al menos 8 caracteres.';return;}
  const button=$('#register-form button[type=submit]');button.disabled=true;
  try{
    await api('/api/register',{method:'POST',body:JSON.stringify({name,email,role,password,documentType,documentNumber,career,subject,teacherCode})});
    const result=await api('/api/login',{method:'POST',body:JSON.stringify({identifier:email,password})});
    apiToken=result.token;user=result.user;await refreshState();
    $('#register-form').reset();updateRegisterFields();$('#register-form').hidden=true;$('#login-view').hidden=true;$('#app-view').hidden=false;
    $('#user-name').textContent=user.name;$('#user-role').textContent=user.role==='student'?user.career:'Profesor · '+(user.subject||'Docencia');$('#avatar').textContent=user.name.split(' ').map(n=>n[0]).join('').slice(0,2);
    $('#grades-nav-label').textContent=user.role==='student'?'Mis calificaciones':'Gestionar notas';
    $('[data-view="badges"]').hidden=user.role!=='student';$('#history-nav').hidden=user.role!=='professor';$('#professors-nav').hidden=false;$('#professors-nav-label').textContent=user.role==='student'?'Buscar profesores':'Directorio docente';$('#profile-nav').hidden=user.role!=='professor';render('overview');startUnreadPolling();notify('Cuenta creada correctamente. ¡Bienvenido a Órbita UCO!');
  }catch(error){$('#register-error').textContent=error.message||'No fue posible crear la cuenta.';}
  finally{button.disabled=false;}
});
$('#login-form').addEventListener('submit',async event=>{
  event.preventDefault(); const button=$('#login-form button[type=submit]'); button.disabled=true;
  try {
    const result=await api('/api/login',{method:'POST',body:JSON.stringify({identifier:$('#email').value.trim(),password:$('#password').value})});
    if(result.user.role!==selectedRole) throw new Error('Selecciona el rol correcto para esta cuenta.');
    apiToken=result.token; user=result.user; await refreshState();
    $('#password').value='';$('#login-error').textContent='';$('#login-view').hidden=true;$('#app-view').hidden=false;
    $('#user-name').textContent=user.name;$('#user-role').textContent=user.role==='student'?user.career:'Profesor · '+(user.subject||'Docencia');$('#avatar').textContent=user.name.split(' ').map(n=>n[0]).join('');
    $('#grades-nav-label').textContent=user.role==='student'?'Mis calificaciones':'Gestionar notas';
    $('[data-view="badges"]').hidden=user.role!=='student';$('#history-nav').hidden=user.role!=='professor';$('#professors-nav').hidden=false;$('#professors-nav-label').textContent=user.role==='student'?'Buscar profesores':'Directorio docente';$('#profile-nav').hidden=user.role!=='professor';render('overview');startUnreadPolling();
  } catch(error) { $('#login-error').textContent=error.message||'No fue posible conectar con el servidor Java.'; }
  finally { button.disabled=false; }
});
$('#logout').addEventListener('click',async()=>{try{await api('/api/logout',{method:'POST',body:'{}'});}catch{} apiToken=null;user=null;selectedChatId=null;if(chatPollTimer){clearInterval(chatPollTimer);chatPollTimer=null;}if(unreadPollTimer){clearInterval(unreadPollTimer);unreadPollTimer=null;}const badge=$('#chats-unread-badge');if(badge){badge.hidden=true;badge.textContent='';}unreadByContact={};state={grades:[],history:[],badges:{},globalStats:{studentCount:0,gradeCount:0,average:null}};$('#app-view').hidden=true;$('#login-view').hidden=false;$('#email').focus();});
document.querySelectorAll('[data-view]').forEach(button=>button.addEventListener('click',()=>render(button.dataset.view)));
function heading(title,subtitle) {return `<div class="page-title"><div><span class="eyebrow muted">TU CAMPUS, A TU RITMO</span><h1>${title}</h1><p>${subtitle}</p></div><span class="semester-tag">◷ &nbsp; Semestre 2026–2</span></div>`;}
function stats(items) {return `<div class="stats">${items.map(i=>`<article class="stat"><div class="stat-label">${i[0]} <span>${i[3]}</span></div><div class="stat-value"><strong>${i[1]}</strong><small>${i[2]}</small></div><div class="stat-foot">${i[4]}</div></article>`).join('')}</div>`;}
function gradeTable(grades) {
  if(!grades.length) return '<p class="empty">Tu historia está por empezar. Aún no hay evaluaciones registradas.</p>';
  return `<div class="table-wrap"><table><thead><tr><th>Actividad / materia</th><th>Fecha</th><th>Nota</th><th>Estado</th></tr></thead><tbody>${grades.slice().reverse().map(g=>`<tr><td><b>${escapeHTML(g.activity)}</b><small>${escapeHTML(g.subject)}</small></td><td>${escapeHTML(g.date.slice(0,10))}</td><td><span class="grade-pill ${g.value<3?'low':''}">${g.value.toFixed(1)}</span></td><td>${g.value>=3?'Aprobada':'Por reforzar'}</td></tr>`).join('')}</tbody></table></div>`;
}
function badgeFeature() {
  const unlocked=earned(user.id),count=unlocked.filter(Boolean).length,last=unlocked.lastIndexOf(true),next=unlocked.indexOf(false);
  return `<article class="card"><div class="card-head"><h3>Tu colección de logros</h3><span>✧</span></div><span class="mini-subtitle">${last>=0?'EN TU ÓRBITA':'EL PRIMER PASO'}</span><div class="badge-feature"><div class="medal">${last>=0?BADGES[last].icon:'↗'}</div><div><h3>${last>=0?BADGES[last].name:'Primer despegue'}</h3><p>${last>=0?'Tu esfuerzo toma forma.':'Cada historia comienza con un reto.'}</p></div></div><div class="progress-label"><span>Insignias activas</span><b>${count} / 4</b></div><div class="progress-track"><div style="width:${count*25}%"></div></div><p class="badge-caption">${next<0?'¡Colección completa! Sigue cuidando tu promedio.':BADGES[next].rule}</p><div class="mission"><span>✦</span><div><p>LA SIGUIENTE MISIÓN</p><b>${next<0?'Mantén tu órbita de excelencia':BADGES[next].name}</b></div></div></article>`;
}
function flightPlanFeature(grades, avg) {
  const bySubject={}; grades.forEach(g=>{const key=g.subject||'Materia';(bySubject[key] ||= []).push(g.value);});
  const weakest=Object.entries(bySubject).map(([name,values])=>({name,avg:values.reduce((a,b)=>a+b,0)/values.length,count:values.length})).sort((a,b)=>a.avg-b.avg)[0];
  let title='Activa tu primera misión', message='Cuando recibas tu primera nota, Órbita te propondrá un reto de estudio adaptado a tu progreso.', action='Explorar el campus';
  if(weakest&&weakest.avg<3.5){title=`Misión de impulso: ${weakest.name}`;message=`Tu promedio actual en esta materia es ${weakest.avg.toFixed(1)}. Dedica 20 minutos a repasar un concepto y resolver dos ejercicios antes de la próxima clase.`;action='Planear mi siguiente paso';}
  else if(avg!==null&&avg>=4.5){title='Misión de mentoría';message='Tu rendimiento va en excelente órbita. Consolida lo aprendido explicándole un concepto a un compañero o creando una ficha de estudio.';action='Ver mis insignias';}
  else if(grades.length){title='Reto de constancia: 20 minutos';message='Elige la materia que más te cuesta, estudia durante 20 minutos y anota una pregunta para llevar a clase. Los pequeños avances suman.';action='Simular mi próxima nota';}
  return `<article class="card flight-plan"><div class="card-head"><div><span class="eyebrow muted">NOVEDAD · RUTA INTELIGENTE</span><h3>Tu plan de vuelo académico</h3></div><span class="flight-icon">✦</span></div><h4>${escapeHTML(title)}</h4><p>${escapeHTML(message)}</p><div class="flight-meta"><span>◷ 20 min</span><span>✧ Misión personalizada</span></div><button class="small-button" data-action="${weakest&&weakest.avg<3.5?'grades':avg!==null&&avg>=4.5?'badges':'grades'}">${escapeHTML(action)} ↗</button></article>`;
}
function campusHomeCarousel(userName, role, avg, view='badges') {
 const slides = [
  {eyebrow:'BIENVENIDO A ÓRBITA UCO', title:`Conecta, aprende y avanza${userName ? ', '+escapeHTML(userName.split(' ')[0]) : ''}.`, copy:'Tu campus, tus metas y nuevas oportunidades en un solo lugar.', action:'Explorar mi progreso', target:view, pos:'center'},
  {eyebrow:'VIDA UNIVERSITARIA', title:'Las grandes ideas crecen en equipo.', copy:'Encuentra docentes, comparte preguntas y descubre nuevas formas de aprender.', action:'Buscar profesores', target:'professors', pos:'right'},
  {eyebrow:'TU PRÓXIMO PASO', title:'Cada pequeño avance cuenta.', copy:'Revisa tus resultados y sigue construyendo tu camino académico.', action:'Ver calificaciones', target:'grades', pos:'left'}
 ];
 return `<section class="campus-hero" aria-label="Bienvenida y novedades de Órbita UCO"><div class="campus-hero-image" aria-hidden="true"></div><div class="campus-hero-shade"></div><div class="campus-hero-copy"><span class="eyebrow" data-carousel-eyebrow>${slides[0].eyebrow}</span><h2 data-carousel-title>${slides[0].title}</h2><p data-carousel-copy>${slides[0].copy}</p><button class="campus-hero-cta" data-action="${slides[0].target}" data-carousel-action>${slides[0].action} <span>↗</span></button></div><button type="button" class="carousel-arrow carousel-prev" aria-label="Imagen anterior" data-carousel-prev>‹</button><button type="button" class="carousel-arrow carousel-next" aria-label="Imagen siguiente" data-carousel-next>›</button><div class="carousel-dots" role="group" aria-label="Elegir imagen">${slides.map((_,i)=>`<button type="button" class="carousel-dot ${i===0?'active':''}" data-carousel-dot="${i}" aria-label="Mostrar imagen ${i+1}" aria-pressed="${i===0}"></button>`).join('')}</div><span class="campus-hero-tag"><span>✦</span> Tu futuro también es una órbita</span></section>`+
 `<section class="campus-story-row" aria-label="Explora la vida universitaria"><button type="button" class="story-tile story-study" data-action="grades"><span class="story-image"></span><span class="story-copy-small"><b>Aprende a tu ritmo</b><small>Consulta tu progreso</small></span><span class="story-arrow">↗</span></button><button type="button" class="story-tile story-campus" data-action="professors"><span class="story-image"></span><span class="story-copy-small"><b>Conoce a tus docentes</b><small>Encuentra orientación</small></span><span class="story-arrow">↗</span></button><button type="button" class="story-tile story-team" data-action="chats"><span class="story-image"></span><span class="story-copy-small"><b>Crece en comunidad</b><small>Conversa y comparte</small></span><span class="story-arrow">↗</span></button></section>`;
}
function overviewStudent() {
  const grades=gradesFor(user.id),avg=average(grades),globalAvg=state.globalStats?.average ?? null;
  return heading(`Hola, ${escapeHTML(user.name.split(' ')[0])}`,'Tu espacio académico, diseñado para acompañar cada paso.')+
    campusHomeCarousel(user.name,'student',avg,'badges')+
    `<section class="home-shortcuts" aria-label="Accesos rápidos"><button class="shortcut-card" data-action="grades"><span class="shortcut-icon">▤</span><b>Mis calificaciones</b><small>Consulta tus notas</small><span class="shortcut-go">↗</span></button><button class="shortcut-card" data-action="grades"><span class="shortcut-icon">↗</span><b>Mi promedio</b><small>${format(avg)} / 5.0 actual</small><span class="shortcut-go">↗</span></button><button class="shortcut-card" data-action="professors"><span class="shortcut-icon">⌕</span><b>Buscar profesores</b><small>Por materia o área</small><span class="shortcut-go">↗</span></button><button class="shortcut-card" data-action="chats"><span class="shortcut-icon">◉</span><b>Chats</b><small>Habla con docentes</small><span class="shortcut-go">↗</span></button></section>`+
    stats([['Mi promedio',format(avg),'/ 5.0','↗',avg===null?'Sin evaluaciones':avg>=4?'En órbita de excelencia':'Sigue construyendo tu progreso'],['Promedio general',format(globalAvg),'/ 5.0','◎',`${state.globalStats?.studentCount||0} estudiantes · ${state.globalStats?.gradeCount||0} notas`],['Insignias activas',earned(user.id).filter(Boolean).length,'/ 4','✧','Pequeños pasos. Grandes logros.']])+
    `<div class="two-col"><article class="card"><div class="card-head"><div><h3>Tu actividad académica</h3><p>Las últimas señales de tu progreso</p></div><button class="small-button" data-action="grades">Ver todas ↗</button></div>${gradeTable(grades.slice(-3))}</article>${badgeFeature()}</div>${flightPlanFeature(grades,avg)}<div class="bottom-note"><span>✦ &nbsp; <strong>¿Y si tu próxima nota fuera…?</strong> Descubre cómo cambiaría tu promedio.</span><button class="text-button" style="width:auto" data-action="grades">Simular mi próximo paso ↗</button></div>`;
}

function studentGrades() {
 const grades=gradesFor(user.id);
 return heading('Cada nota cuenta.','Consulta tus evaluaciones y proyecta tu siguiente paso.')+`<article class="card"><div class="card-head"><h3>Mis calificaciones</h3><span class="semester-tag">Promedio ${format(average(grades))} / 5.0</span></div>${gradeTable(grades)}</article><article class="card simulator"><div class="card-head"><div><h3>Laboratorio de futuros</h3><p>Una hipótesis, una nueva posibilidad. No modifica tus notas.</p></div><span>✦</span></div><div class="two-col"><div><label for="future-grade">Si mi próxima nota fuera <output id="future-value">4.5</output></label><input type="range" id="future-grade" min="0" max="5" step="0.1" value="4.5"><p class="form-note">Promedio aritmético de tus ${grades.length} evaluaciones actuales más una nueva evaluación.</p></div><div><span class="mini-subtitle">TU PROMEDIO PROYECTADO</span><p class="sim-result" id="future-average"></p></div></div></article>`;
}
function studentBadges() {
 const unlocked=earned(user.id);
 return heading('El esfuerzo tiene su insignia.','Reconocimientos activos según tus resultados actuales; pueden cambiar si tu promedio baja.')+`<div class="badge-grid">${BADGES.map((b,i)=>`<article class="card badge-card ${unlocked[i]?'':'locked'}"><div class="medal">${b.icon}</div><div><span class="status-label">${unlocked[i]?'DESBLOQUEADA':'POR DESCUBRIR'}</span><h3>${b.name}</h3><p>${b.rule}</p></div></article>`).join('')}</div><div class="bottom-note">✦ &nbsp; Tu valor va más allá de una nota. Las insignias celebran avances y te invitan a seguir aprendiendo.</div>`;
}
function studentRoster() {
 return `<div class="table-wrap"><table><thead><tr><th>Estudiante</th><th>Evaluaciones</th><th>Promedio</th><th>Insignias</th></tr></thead><tbody>${USERS.filter(u=>u.role==='student').map(s=>`<tr><td><b>${escapeHTML(s.name)}</b><small>${escapeHTML(s.id)} · ${escapeHTML(s.career||'Programa académico')}</small></td><td>${gradesFor(s.id).length}</td><td><span class="grade-pill">${format(average(gradesFor(s.id)))}</span></td><td>${earned(s.id).filter(Boolean).length} / 4</td></tr>`).join('')}</tbody></table></div>`;
}
function overviewProfessor() {
 const students=USERS.filter(u=>u.role==='student');
 return heading('Enseñar también es inspirar.',`Hola, ${escapeHTML(user.name.split(' ')[0])}. Acompaña el siguiente gran paso de tus estudiantes.`)+
 campusHomeCarousel(user.name,'professor',average(state.grades),'grades')+
 `<section class="home-shortcuts" aria-label="Accesos rápidos"><button class="shortcut-card" data-action="grades"><span class="shortcut-icon">▤</span><b>Gestionar notas</b><small>Registrar evaluaciones</small><span class="shortcut-go">↗</span></button><button class="shortcut-card" data-action="professors"><span class="shortcut-icon">♧</span><b>Directorio docente</b><small>Conecta con colegas</small><span class="shortcut-go">↗</span></button><button class="shortcut-card" data-action="chats"><span class="shortcut-icon">◉</span><b>Chats</b><small>Responde mensajes</small><span class="shortcut-go">↗</span></button><button class="shortcut-card" data-action="profile"><span class="shortcut-icon">◎</span><b>Mi perfil</b><small>Actualiza tu experiencia</small><span class="shortcut-go">↗</span></button></section>`+
 stats([['Estudiantes',state.globalStats?.studentCount||students.length,'registrados','♧','Estudiantes de todas las carreras'],['Promedio general',format(state.globalStats?.average??null),'/ 5.0','◎',`${state.globalStats?.gradeCount||0} evaluaciones registradas`],['Promedio del grupo',format(average(state.grades)),'/ 5.0','↗','Media de las notas visibles']])+`<article class="card"><div class="card-head"><div><h3>Tu grupo, de un vistazo</h3><p>El progreso individual de cada estudiante</p></div><button class="small-button" data-action="grades">Asignar nota ↗</button></div>${studentRoster()}</article>`;
}

function professorGrades() {
 return heading('Dale una señal a su progreso.','Registra una evaluación y observa cómo cambian el promedio y las insignias.')+`<div class="two-col"><article class="card"><div class="card-head"><h3>Nueva calificación</h3><span>↗</span></div><form id="grade-form"><div class="form-grid"><div class="span-two"><label for="student-id">Estudiante</label><select id="student-id">${USERS.filter(u=>u.role==='student').map(s=>`<option value="${s.id}">${s.name} · ${s.id}</option>`).join('')}</select></div><div class="span-two"><label for="subject">Materia asignada</label><input id="subject" value="${escapeHTML(user.subject||SUBJECT)}" readonly></div><div><label for="activity">Actividad</label><input id="activity" maxlength="80" required placeholder="Ej. Taller de interfaces"></div><div><label for="grade">Nota (0.0 a 5.0)</label><input type="number" id="grade" min="0" max="5" step="0.1" required placeholder="4.5"></div></div><p class="form-note">Todas las evaluaciones tienen igual peso. No se admite una actividad repetida para el mismo estudiante y materia.</p><p id="grade-error" class="error" role="alert"></p><button type="submit" class="primary full">Registrar calificación <span>↗</span></button><p id="grade-success" class="form-success" role="status"></p></form></article><article class="card"><div class="card-head"><h3>Expediente del estudiante</h3></div><div id="student-preview"></div></article></div><article class="card section-gap"><div class="card-head"><h3>Panorama del grupo</h3></div>${studentRoster()}</article>`;
}
async function loadProfessors() {
 const container=$('#professor-results'); if(!container)return;
 container.innerHTML='<p class="empty">Cargando profesores…</p>';
 try { const result=await api('/api/professors'); window.orbitaProfessors=result; filterProfessors(); }
 catch(error) { container.innerHTML=`<p class="error">${escapeHTML(error.message||'No fue posible consultar los profesores.')}</p>`; }
}
function filterProfessors() {
 const container=$('#professor-results'); if(!container)return;
 const all=window.orbitaProfessors||[], q=normalizeSearch($('#professor-search')?.value||'');
 const filtered=all.filter(p=>[p.name,p.subject,p.career,p.university,p.bio].some(v=>normalizeSearch(v).includes(q)));
 container.innerHTML=filtered.length?`<div class="professor-grid">${filtered.map(p=>`<article class="card professor-card"><div class="professor-card-top"><span class="avatar professor-avatar">${escapeHTML((p.name||'?').split(/\s+/).map(n=>n[0]).join('').slice(0,2))}</span><div><h3>${escapeHTML(p.name)}</h3><p>${escapeHTML(p.subject||'Materia no especificada')}</p></div></div><div class="professor-details"><p><b>Área profesional</b><span>${escapeHTML(p.career||'No especificada')}</span></p><p><b>Edad</b><span>${p.age?escapeHTML(p.age)+' años':'No indicada'}</span></p><p><b>Universidad</b><span>${escapeHTML(p.university||'No indicada')}</span></p>${p.bio?`<p><b>Perfil</b><span>${escapeHTML(p.bio)}</span></p>`:''}</div><a class="professor-email" href="mailto:${escapeHTML(p.email)}">${escapeHTML(p.email)}</a><button class="primary contact-professor" type="button" data-contact-professor="${escapeHTML(p.id)}">Contactar por chat <span>↗</span></button></article>`).join('')}</div>`:'<p class="empty">No encontramos profesores con esa búsqueda. Prueba con un nombre, una materia o escribe la búsqueda sin tildes.</p>';
 container.querySelectorAll('[data-contact-professor]').forEach(b=>b.addEventListener('click',()=>{selectedChatId=b.dataset.contactProfessor;render('chats');}));
}
function professorsView() {
 return heading(user.role==='student'?'Encuentra a tu profesor.':'Conecta con otros docentes.','Busca por nombre, materia que dicta, área profesional o universidad. La búsqueda funciona con o sin tildes.')+`<article class="card"><label for="professor-search">Buscar en el directorio docente</label><input id="professor-search" type="search" placeholder="Ej. Estadística, Andrés, Ingeniería…" autocomplete="off"><p class="form-note">Puedes buscar por la materia que dicta o por su área profesional. No importa si escribes con o sin tilde: Física también encuentra fisica.</p></article><div id="professor-results" class="section-gap"><p class="empty">Cargando profesores…</p></div>`;
}
function professorProfileView() {
 return heading('Tu perfil académico.','Comparte información opcional para que los estudiantes conozcan tu experiencia. Solo se mostrarán los datos que decidas agregar.')+`<article class="card profile-card"><div class="card-head"><div><h3>${escapeHTML(user.name)}</h3><p>${escapeHTML(user.subject||'Materia')} · ${escapeHTML(user.career||'Área profesional')}</p></div><span class="avatar">${escapeHTML((user.name||'?').split(/\s+/).map(n=>n[0]).join('').slice(0,2))}</span></div><form id="profile-form"><div class="form-grid"><div><label for="profile-age">Edad (opcional)</label><input id="profile-age" type="number" min="18" max="100" value="${escapeHTML(user.age||'')}" placeholder="Ej. 35"></div><div><label for="profile-university">Universidad donde estudiaste (opcional)</label><input id="profile-university" maxlength="120" value="${escapeHTML(user.university||'')}" placeholder="Nombre de la universidad"></div><div class="span-two"><label for="profile-bio">Experiencia o presentación (opcional)</label><textarea id="profile-bio" rows="4" maxlength="500" placeholder="Cuéntales a tus estudiantes sobre tu experiencia, intereses o enfoque de enseñanza…">${escapeHTML(user.bio||'')}</textarea><p class="form-note">Máximo 500 caracteres. No incluyas información privada o datos de contacto personales.</p></div></div><p id="profile-error" class="error" role="alert"></p><button class="primary" type="submit">Guardar perfil <span>↗</span></button><p id="profile-success" class="form-success" role="status"></p></form></article>`;
}
async function saveProfessorProfile(event) {
 event.preventDefault(); if(!user||user.role!=='professor')return;
 const payload={age:$('#profile-age').value.trim(),university:$('#profile-university').value.trim(),bio:$('#profile-bio').value.trim()};
 $('#profile-error').textContent='';$('#profile-success').textContent='';
 try { const result=await api('/api/profile',{method:'PUT',body:JSON.stringify(payload)}); user=result.user; $('#user-name').textContent=user.name; $('#user-role').textContent='Profesor · '+(user.subject||'Docencia'); $('#profile-success').textContent='Perfil académico guardado correctamente.'; notify('Tu perfil académico se guardó en Java.'); }
 catch(error){$('#profile-error').textContent=error.message||'No fue posible guardar el perfil.';}
}
async function refreshUnread() {
 if(!user||!apiToken)return;
 try {
  const data=await api('/api/unread'); unreadByContact=data.byContact||{};
  const badge=$('#chats-unread-badge'); if(badge){badge.textContent=data.total>99?'99+':String(data.total||'');badge.hidden=!(data.total>0);}
  document.querySelectorAll('[data-chat-select]').forEach(button=>{
   const count=Number(unreadByContact[button.dataset.chatSelect]||0);let tag=button.querySelector('.chat-unread-badge');
   if(count>0){if(!tag){tag=document.createElement('span');tag.className='chat-unread-badge';button.appendChild(tag);}tag.textContent=count>99?'99+':String(count);}
   else if(tag)tag.remove();
  });
 } catch (_) { /* El chat seguirá funcionando aunque falle una consulta de notificaciones. */ }
}
function startUnreadPolling() {
 if(unreadPollTimer)clearInterval(unreadPollTimer);
 refreshUnread(); unreadPollTimer=setInterval(refreshUnread,2500);
}
function chatsView() {
 return heading('Conversaciones en órbita.','Comunícate con profesores y estudiantes desde la plataforma. Los mensajes se guardan en el servidor Java.')+
 `<div class="chat-layout"><article class="card chat-contacts"><div class="card-head"><div><h3>Personas</h3><p>${user.role==='student'?'Contacta a tus profesores':'Busca estudiantes y otros profesores'}</p></div><span>◉</span></div><input id="chat-contact-search" type="search" placeholder="Buscar por nombre o materia…"><div id="chat-contact-list" class="chat-contact-list"><p class="empty">Cargando contactos…</p></div></article><article class="card chat-panel"><div id="chat-header" class="chat-header"><span class="avatar">◉</span><div><h3>Tu espacio de conversación</h3><p>Selecciona una persona para comenzar</p></div></div><div id="chat-messages" class="chat-messages"><p class="empty">Tus mensajes aparecerán aquí.</p></div><form id="chat-form" class="chat-form"><textarea id="chat-message-input" rows="2" maxlength="2000" placeholder="Escribe un mensaje…" disabled required></textarea><button class="primary" type="submit" disabled id="chat-send-button">Enviar <span>↗</span></button><p id="chat-error" class="error" role="alert"></p></form></article></div>`;
}
async function loadChatPeople(preferredId=null) {
 try {
  chatPeople=await api('/api/people');
  const search=$('#chat-contact-search'); if(!search)return;
  const draw=()=>{
   const query=search.value.trim().toLocaleLowerCase('es');
   const people=chatPeople.filter(p=>[p.name,p.subject,p.career,p.role].some(v=>(v||'').toLocaleLowerCase('es').includes(query)));
   $('#chat-contact-list').innerHTML=people.length?people.map(p=>`<button type="button" class="chat-contact ${p.id===selectedChatId?'selected':''}" data-chat-select="${escapeHTML(p.id)}"><span class="avatar">${escapeHTML((p.name||'?').split(/\s+/).map(n=>n[0]).join('').slice(0,2))}</span><span class="chat-contact-copy"><b>${escapeHTML(p.name)}</b><small>${p.role==='professor'?'Profesor':'Estudiante'}${p.subject?' · '+escapeHTML(p.subject):''}</small></span>${Number(unreadByContact[p.id]||0)>0?`<span class="chat-unread-badge">${Number(unreadByContact[p.id])>99?'99+':Number(unreadByContact[p.id])}</span>`:''}<span class="chat-contact-arrow">›</span></button>`).join(''):'<p class="empty">No hay personas con ese nombre.</p>';
   document.querySelectorAll('[data-chat-select]').forEach(b=>b.addEventListener('click',()=>selectChat(b.dataset.chatSelect)));
  };
  search.addEventListener('input',draw); draw();
  const initial=preferredId||selectedChatId;
  if(initial&&chatPeople.some(p=>p.id===initial)) await selectChat(initial); else if(selectedChatId&&!chatPeople.some(p=>p.id===selectedChatId))selectedChatId=null;
 } catch(error) { const list=$('#chat-contact-list'); if(list)list.innerHTML=`<p class="error">${escapeHTML(error.message||'No se pudieron cargar los contactos.')}</p>`; }
}
async function selectChat(id) {
 if(!chatPeople.some(p=>p.id===id))return;
 selectedChatId=id;
 document.querySelectorAll('[data-chat-select]').forEach(b=>b.classList.toggle('selected',b.dataset.chatSelect===id));
 const person=chatPeople.find(p=>p.id===id);
 $('#chat-header').innerHTML=`<span class="avatar">${escapeHTML((person.name||'?').split(/\s+/).map(n=>n[0]).join('').slice(0,2))}</span><div><h3>${escapeHTML(person.name)}</h3><p>${person.role==='professor'?'Profesor · '+escapeHTML(person.subject||person.career||'Docencia'):'Estudiante · '+escapeHTML(person.career||'Estudiante')}</p></div>`;
 $('#chat-message-input').disabled=false;$('#chat-send-button').disabled=false;$('#chat-error').textContent='';
 await loadMessages();
}
async function loadMessages() {
 if(currentView!=='chats'||!selectedChatId||!$('#chat-messages'))return;
 try {
  const messages=await api('/api/messages?with='+encodeURIComponent(selectedChatId));
  const container=$('#chat-messages');
  const nearBottom=container.scrollHeight-container.scrollTop-container.clientHeight<90;
  container.innerHTML=messages.length?messages.map(m=>`<div class="message-row ${m.senderId===user.id?'mine':'theirs'}"><div class="message-bubble"><p>${escapeHTML(m.text).replace(/\n/g,'<br>')}</p><time>${escapeHTML(new Date(m.timestamp).toLocaleString('es-CO',{dateStyle:'short',timeStyle:'short'}))}</time></div></div>`).join(''):'<p class="empty">Esta conversación está lista. Envía el primer mensaje.</p>';
  if(nearBottom)container.scrollTop=container.scrollHeight;
  await refreshUnread();
 } catch(error) { $('#chat-error').textContent=error.message||'No se pudieron cargar los mensajes.'; }
}
async function sendChatMessage(event) {
 event.preventDefault();if(!selectedChatId)return;
 const input=$('#chat-message-input'), text=input.value.trim();if(!text)return;
 const button=$('#chat-send-button');button.disabled=true;$('#chat-error').textContent='';
 try { await api('/api/messages',{method:'POST',body:JSON.stringify({recipientId:selectedChatId,text})});input.value='';await loadMessages();await refreshUnread();input.focus(); }
 catch(error){$('#chat-error').textContent=error.message||'No se pudo enviar el mensaje.';}
 finally{button.disabled=false;}
}

const WEEKDAYS = [{id:1,name:'Lunes'},{id:2,name:'Martes'},{id:3,name:'Miércoles'},{id:4,name:'Jueves'},{id:5,name:'Viernes'},{id:6,name:'Sábado'},{id:7,name:'Domingo'}];
let scheduleEntries = [];
function calendarView() {
 const isProfessor=user.role==='professor';
 return heading('Calendario de clases.','Consulta las clases programadas de lunes a domingo. Cada docente administra únicamente sus propios horarios.')+
 `<article class="card calendar-filters"><div class="card-head"><div><h3>Encuentra una clase</h3><p>Filtra por día, docente o materia.</p></div><span class="calendar-symbol">▦</span></div><div class="calendar-filter-grid"><div><label for="calendar-day-filter">Día de la semana</label><select id="calendar-day-filter"><option value="">Todos los días</option>${WEEKDAYS.map(d=>`<option value="${d.id}">${d.name}</option>`).join('')}</select></div><div><label for="calendar-professor-filter">Docente</label><select id="calendar-professor-filter"><option value="">Todos los docentes</option></select></div><div><label for="calendar-subject-filter">Materia</label><input id="calendar-subject-filter" type="search" placeholder="Ej. Termodinámica"></div></div></article>`+
 (isProfessor?`<article class="card calendar-create"><div class="card-head"><div><h3>Programar una clase</h3><p>Agrega el día, la hora y el salón. La materia viene asignada a tu perfil y cada clase ocupa un bloque de una hora.</p></div><span class="calendar-symbol">＋</span></div><form id="schedule-form"><div class="calendar-form-grid"><div><label for="schedule-day">Día</label><select id="schedule-day" required>${WEEKDAYS.map(d=>`<option value="${d.id}">${d.name}</option>`).join('')}</select></div><div><label for="schedule-time">Hora</label><input id="schedule-time" type="time" required></div><div><label for="schedule-room">Salón</label><input id="schedule-room" maxlength="80" placeholder="Ej. Bloque B · 204" required></div><div><label for="schedule-subject">Materia asignada</label><input id="schedule-subject" value="${escapeHTML(user.subject||'Sin materia asignada')}" readonly aria-readonly="true"><small class="form-note">La materia la determina tu perfil docente.</small></div></div><p id="schedule-error" class="error" role="alert"></p><button type="submit" class="primary">Guardar horario <span>↗</span></button><p class="form-note">El horario se repite semanalmente. Cada clase ocupa una hora y el servidor evita cruces de docente y salón.</p></form></article>`:'')+
 `<section class="calendar-results"><div class="calendar-results-heading"><div><span class="eyebrow muted">AGENDA SEMANAL</span><h2>Clases programadas</h2></div><span id="calendar-count" class="semester-tag">Cargando…</span></div><div id="calendar-list" class="calendar-week-grid"><p class="empty">Cargando calendario…</p></div></section>`;
}
function renderCalendarEntries() {
 const list=$('#calendar-list'); if(!list)return;
 const day=$('#calendar-day-filter')?.value||'', professor=$('#calendar-professor-filter')?.value||'', subject=normalizeSearch($('#calendar-subject-filter')?.value||'');
 const entries=scheduleEntries.filter(e=>(!day||String(e.weekday)===day)&&(!professor||e.professorId===professor)&&(!subject||normalizeSearch(e.subject).includes(subject)));
 $('#calendar-count').textContent=`${entries.length} ${entries.length===1?'clase':'clases'}`;
 if(!entries.length){list.innerHTML='<article class="card calendar-empty"><span>▦</span><h3>No hay clases para mostrar</h3><p>Prueba otro filtro o espera a que un docente publique un horario.</p></article>';return;}
 list.innerHTML=WEEKDAYS.filter(d=>!day||String(d.id)===day).map(d=>{const items=entries.filter(e=>Number(e.weekday)===d.id).sort((a,b)=>a.time.localeCompare(b.time));if(!items.length)return '';return `<section class="calendar-day-column"><div class="calendar-day-heading"><span>${d.name.slice(0,3).toUpperCase()}</span><h3>${d.name}</h3><small>${items.length} ${items.length===1?'clase':'clases'}</small></div>${items.map(e=>`<article class="calendar-class-card"><div class="calendar-class-time">${escapeHTML(e.time)}</div><h4>${escapeHTML(e.subject)}</h4><p class="calendar-teacher">${escapeHTML(e.professorName||'Docente')}</p><p class="calendar-room">⌖ &nbsp; ${escapeHTML(e.room)}</p>${user.role==='professor'&&e.professorId===user.id?`<button class="calendar-delete" type="button" data-delete-schedule="${escapeHTML(e.id)}">Eliminar horario</button>`:''}</article>`).join('')}</section>`}).join('');
 list.querySelectorAll('[data-delete-schedule]').forEach(b=>b.addEventListener('click',async()=>{if(!confirm('¿Quieres eliminar este horario?'))return;try{await api('/api/schedule/'+encodeURIComponent(b.dataset.deleteSchedule),{method:'DELETE'});notify('Horario eliminado.');await loadCalendarEntries();}catch(err){notify(err.message||'No se pudo eliminar el horario.');}}));
}
async function loadCalendarEntries(){
 const list=$('#calendar-list');if(!list)return;
 try{scheduleEntries=await api('/api/schedule');const select=$('#calendar-professor-filter');if(select){const previous=select.value;const registered=await api('/api/professors');const teachers=[...new Map([...registered.map(p=>[p.id,{id:p.id,name:p.name}]),...scheduleEntries.map(e=>[e.professorId,{id:e.professorId,name:e.professorName}])]).values()].sort((a,b)=>a.name.localeCompare(b.name,'es'));select.innerHTML='<option value="">Todos los docentes</option>'+teachers.map(t=>`<option value="${escapeHTML(t.id)}">${escapeHTML(t.name)}</option>`).join('');select.value=previous;}renderCalendarEntries();}
 catch(err){list.innerHTML=`<p class="error">${escapeHTML(err.message||'No se pudo cargar el calendario.')}</p>`;}
}
async function saveSchedule(event){
 event.preventDefault();if(user?.role!=='professor')return;
 const payload={weekday:Number($('#schedule-day').value),time:$('#schedule-time').value,room:$('#schedule-room').value.trim()};$('#schedule-error').textContent='';
 try{await api('/api/schedule',{method:'POST',body:JSON.stringify(payload)});$('#schedule-form').reset();notify('Clase agregada al calendario.');await loadCalendarEntries();}
 catch(err){$('#schedule-error').textContent=err.message||'No se pudo guardar el horario.';}
}

function historyView() {
 return heading('Las huellas del aprendizaje.','Historial de la demo en este navegador. Conserva las últimas 100 acciones.')+`<article class="card">${state.history.map(e=>`<div class="history-item"><span>${escapeHTML(new Date(e.date).toLocaleString('es-CO'))}</span><p>${escapeHTML(e.text)}</p></div>`).join('')}</article>`;
}
function render(view) {
 if(!user)return;
 if(chatPollTimer){clearInterval(chatPollTimer);chatPollTimer=null;}
 if(view==='history'&&user.role!=='professor')return;
 if(view==='badges'&&user.role!=='student')return;
 currentView=view;const titles={overview:'Mi panorama',grades:user.role==='student'?'Mis calificaciones':'Gestionar notas',badges:'Mis insignias',history:'Bitácora',professors:user.role==='student'?'Buscar profesores':'Directorio docente',calendar:'Calendario de clases',profile:'Perfil académico',chats:'Chats'};
 $('#breadcrumb').textContent=titles[view];document.querySelectorAll('[data-view]').forEach(b=>{b.classList.toggle('active',b.dataset.view===view);if(b.dataset.view===view)b.setAttribute('aria-current','page');else b.removeAttribute('aria-current');});
 const pages=user.role==='student'?{overview:overviewStudent,grades:studentGrades,badges:studentBadges,professors:professorsView,calendar:calendarView,chats:chatsView}:{overview:overviewProfessor,grades:professorGrades,history:historyView,professors:professorsView,calendar:calendarView,profile:professorProfileView,chats:chatsView};
 $('#main-content').innerHTML=pages[view]();
 document.querySelectorAll('[data-action]').forEach(b=>b.addEventListener('click',()=>render(b.dataset.action)));
 const slides=[
  {eyebrow:'BIENVENIDO A ÓRBITA UCO',title:`Conecta, aprende y avanza, ${user.name.split(' ')[0]}.`,copy:'Tu campus, tus metas y nuevas oportunidades en un solo lugar.',action:'Explorar mi progreso',target:user.role==='student'?'badges':'grades',pos:'center'},
  {eyebrow:'VIDA UNIVERSITARIA',title:'Las grandes ideas crecen en equipo.',copy:'Encuentra docentes, comparte preguntas y descubre nuevas formas de aprender.',action:'Buscar profesores',target:'professors',pos:'right'},
  {eyebrow:'TU PRÓXIMO PASO',title:'Cada pequeño avance cuenta.',copy:'Revisa tus resultados y sigue construyendo tu camino académico.',action:'Ver calificaciones',target:'grades',pos:'left'}
 ];
 if($('#main-content .campus-hero')){
  let activeSlide=0;
  const showSlide=(index)=>{activeSlide=(index+slides.length)%slides.length;const slide=slides[activeSlide];const hero=$('#main-content .campus-hero');hero.dataset.slide=String(activeSlide);hero.querySelector('[data-carousel-eyebrow]').textContent=slide.eyebrow;hero.querySelector('[data-carousel-title]').textContent=slide.title;hero.querySelector('[data-carousel-copy]').textContent=slide.copy;const action=hero.querySelector('[data-carousel-action]');action.dataset.action=slide.target;action.innerHTML=`${slide.action} <span>↗</span>`;hero.querySelectorAll('[data-carousel-dot]').forEach((dot,i)=>{dot.classList.toggle('active',i===activeSlide);dot.setAttribute('aria-pressed',String(i===activeSlide));});};
  $('#main-content [data-carousel-prev]').addEventListener('click',()=>showSlide(activeSlide-1));$('#main-content [data-carousel-next]').addEventListener('click',()=>showSlide(activeSlide+1));$('#main-content').querySelectorAll('[data-carousel-dot]').forEach(dot=>dot.addEventListener('click',()=>showSlide(Number(dot.dataset.carouselDot))));
 }
 if($('#future-grade')){const update=()=>{const value=Number($('#future-grade').value),grades=gradesFor(user.id);$('#future-value').textContent=value.toFixed(1);$('#future-average').textContent=format((grades.reduce((sum,g)=>sum+g.value,0)+value)/(grades.length+1))+' / 5.0';};$('#future-grade').addEventListener('input',update);update();}
 if($('#grade-form')){
   const preview=()=>{const s=USERS.find(u=>u.id===$('#student-id').value);$('#student-preview').innerHTML=`<h3>${s.name}</h3><p class="form-note">Promedio ${format(average(gradesFor(s.id)))} / 5.0 · ${earned(s.id).filter(Boolean).length} insignias</p>${gradeTable(gradesFor(s.id))}`;};
   $('#student-id').addEventListener('change',preview);preview();$('#grade-form').addEventListener('submit',saveGrade);
 }
 if($('#professor-search')) { $('#professor-search').addEventListener('input',filterProfessors); loadProfessors(); }
 if($('#chat-contact-search')) { loadChatPeople(); $('#chat-form').addEventListener('submit',sendChatMessage); chatPollTimer=setInterval(()=>{if(currentView==='chats'&&selectedChatId)loadMessages();},2500); }
 if($('#profile-form')) $('#profile-form').addEventListener('submit',saveProfessorProfile);
 if($('#calendar-list')){['calendar-day-filter','calendar-professor-filter','calendar-subject-filter'].forEach(id=>{const el=$('#'+id);if(el)el.addEventListener(id==='calendar-subject-filter'?'input':'change',renderCalendarEntries);});if($('#schedule-form'))$('#schedule-form').addEventListener('submit',saveSchedule);loadCalendarEntries();}
 $('#main-content').focus({preventScroll:true});
}
async function saveGrade(event) {
 event.preventDefault();if(!user||user.role!=='professor')return;
 const studentId=$('#student-id').value,activity=$('#activity').value.trim(),raw=$('#grade').value,value=Number(raw);
 $('#grade-error').textContent=''; $('#grade-success').textContent='';
 if(!activity||activity.length>80||raw===''||!Number.isFinite(value)||value<0||value>5||!USERS.some(u=>u.id===studentId&&u.role==='student')){ $('#grade-error').textContent='Completa los campos con una nota válida de 0.0 a 5.0.';return; }
 if(gradesFor(studentId).some(g=>g.subject===(user.subject||SUBJECT)&&g.activity.toLowerCase()===activity.toLowerCase())){$('#grade-error').textContent='Esta actividad ya tiene una nota para el estudiante.';return;}
 const before=earned(studentId);
 try {
   const data=await api('/api/grades',{method:'POST',body:JSON.stringify({studentId,activity,value})});
   state={grades:data.grades||[],history:data.history||[],badges:data.badges||{}};
   const student=USERS.find(u=>u.id===studentId);
   const newBadges=earned(studentId).flatMap((yes,i)=>yes&&!before[i]?[BADGES[i].name]:[]);
   render('grades');$('#student-id').value=studentId;$('#student-id').dispatchEvent(new Event('change'));
   $('#grade-success').textContent=`Calificación registrada para ${student.name}.${newBadges.length?' Nueva insignia: '+newBadges.join(', ')+'.':''}`;
   notify('Calificación guardada en el servidor Java.');
 } catch(error) { $('#grade-error').textContent=error.message||'No fue posible guardar la calificación.'; }
}
