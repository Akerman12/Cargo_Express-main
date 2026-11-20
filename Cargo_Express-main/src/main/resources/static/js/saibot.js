
document.addEventListener('DOMContentLoaded', () => {
    const container = document.createElement('div');
    container.id = 'sai-bot-container';

    container.innerHTML = `
        <div id="sai-bot-window">
            <div class="sai-bot-header">
                <img src="/imagenes/LOGO Cargo Expres.jpg" alt="SAI">
                <div>
                    <h3>SAI-Bot</h3>
                    <p>Asistente de Logística Inteligente</p>
                </div>
                <div style="margin-left:auto;display:flex;gap:15px;align-items:center;">
                    <i id="voiceToggle"
                       class="fa-solid fa-volume-xmark"
                       style="cursor:pointer;font-size:14px;"
                       title="Activar voz"
                       onclick="toggleVoice()"></i>
                    <i class="fa-solid fa-xmark"
                       style="cursor:pointer;font-size:18px;"
                       onclick="toggleBot()"></i>
                </div>
            </div>

            <div class="sai-bot-messages" id="botMessages">
                <div class="msg msg-bot">
                    ¡Hola! 👋 Soy SAI, tu asistente de Cargo Express.
                    Puedo ayudarte con servicios, tarifas, procesos
                    y consultas de envíos. ¿Qué deseas saber?
                </div>
            </div>

            <div class="typing" id="botTyping" style="padding:0 20px;">
                SAI está escribiendo...
            </div>

            <div class="sai-bot-footer">
                <div class="quick-actions">
                    <button class="quick-btn"
                            onclick="sendQuick('Quiero rastrear mi envío')">
                        ¿Cómo rastrear?
                    </button>
                    <button class="quick-btn"
                            onclick="sendQuick('¿Cuáles son los costos de envío?')">
                        Costos de envío
                    </button>
                    <button class="quick-btn"
                            onclick="sendQuick('¿Cómo puedo contactar con soporte?')">
                        Soporte
                    </button>
                </div>

                <div class="sai-bot-input">
                    <input type="text"
                           id="botInput"
                           placeholder="Escribe tu duda aquí..."
                           onkeypress="handleKey(event)">
                    <button onclick="sendMessage()">
                        <i class="fa-solid fa-paper-plane"></i>
                    </button>
                </div>
            </div>
        </div>

        <div id="sai-bot-button" onclick="toggleBot()">
            <div class="sai-bot-badge"></div>
            <i class="fa-solid fa-robot"></i>
        </div>
    `;

    document.body.appendChild(container);
});

let voiceEnabled = false;

// Estados: general, correo, codigo, guia.
let estadoChat = 'general';
let historialChat = [];
// Datos temporales de la cotización
let datosCotizacion = {};
let pasoCotizacion = 0;

const preguntasCotizacion = [
    {
        campo: 'altura',
        pregunta: '¿Cuál es la altura del paquete en centímetros?',
        tipo: 'numero',
        minimo: 0
    },
    {
        campo: 'ancho',
        pregunta: '¿Cuál es el ancho del paquete en centímetros?',
        tipo: 'numero',
        minimo: 0
    },
    {
        campo: 'largo',
        pregunta: '¿Cuál es el largo del paquete en centímetros?',
        tipo: 'numero',
        minimo: 0
    },
    {
        campo: 'peso',
        pregunta: '¿Cuánto pesa el paquete en kilogramos?',
        tipo: 'numero',
        minimo: 0
    },
    {
        campo: 'valorDeclarado',
        pregunta: '¿Cuál es el valor declarado de la mercancía en pesos colombianos? Es el valor que se tendrá en cuenta para el seguro.',
        tipo: 'numero',
        minimo: 0,
        permitirCero: true
    },
    {
        campo: 'origen',
        pregunta: '¿Desde qué ciudad se realizará el envío?',
        tipo: 'texto'
    },
    {
        campo: 'destino',
        pregunta: '¿A qué ciudad deseas enviar el paquete?',
        tipo: 'texto'
    }
];
function toggleBot() {
    document.getElementById('sai-bot-window')
        .classList.toggle('active');
}

function handleKey(event) {
    if (event.key === 'Enter') {
        sendMessage();
    }
}

function sendQuick(texto) {
    document.getElementById('botInput').value = texto;
    sendMessage();
}

function toggleVoice() {
    voiceEnabled = !voiceEnabled;

    const icon = document.getElementById('voiceToggle');

    icon.className = voiceEnabled
        ? 'fa-solid fa-volume-high'
        : 'fa-solid fa-volume-xmark';

    icon.style.color = voiceEnabled ? '#00f2ff' : 'white';
}

function escaparHTML(texto) {
    return String(texto).replace(/[&<>"']/g, caracter => ({
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;'
    })[caracter]);
}

function convertirMarkdown(texto) {
    let lineas = escaparHTML(texto).split('\n');
    let resultado = [];
    let enTabla = false;
    let enLista = false;

    function cerrarTabla() {
        if (enTabla) {
            resultado.push('</tbody></table>');
            enTabla = false;
        }
    }

    function cerrarLista() {
        if (enLista) {
            resultado.push('</ul>');
            enLista = false;
        }
    }

    for (let linea of lineas) {
        const limpia = linea.trim();

        // Tablas Markdown
        if (limpia.startsWith('|') && limpia.endsWith('|')) {
            cerrarLista();

            const columnas = limpia.split('|')
                .slice(1, -1)
                .map(c => c.trim());

            // Ignorar separadores de tabla
            if (columnas.every(c => /^:?-+:?$/.test(c))) {
                continue;
            }

            if (!enTabla) {
                resultado.push('<table class="sai-tabla"><tbody>');
                enTabla = true;
            }

            const celdas = columnas.map(c => {
                return `<td>${c}</td>`;
            }).join('');

            resultado.push(`<tr>${celdas}</tr>`);
            continue;
        }

        cerrarTabla();

        // Listas con viñetas
        if (/^[-*]\s+/.test(limpia)) {
            if (!enLista) {
                resultado.push('<ul>');
                enLista = true;
            }

            resultado.push(
                `<li>${limpia.replace(/^[-*]\s+/, '')}</li>`
            );
            continue;
        }

        cerrarLista();

        if (!limpia) {
            resultado.push('<br>');
            continue;
        }

        // Títulos
        if (/^###\s+/.test(limpia)) {
            resultado.push(`<h4>${limpia.replace(/^###\s+/, '')}</h4>`);
        } else if (/^##\s+/.test(limpia)) {
            resultado.push(`<h3>${limpia.replace(/^##\s+/, '')}</h3>`);
        } else if (/^#\s+/.test(limpia)) {
            resultado.push(`<h3>${limpia.replace(/^#\s+/, '')}</h3>`);
        } else {
            resultado.push(`<p>${limpia}</p>`);
        }
    }

    cerrarTabla();
    cerrarLista();

    let html = resultado.join('');

    // Negritas y cursivas
    html = html.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
    html = html.replace(/\*(.*?)\*/g, '<em>$1</em>');

    return html;
}

function addMessage(texto, tipo) {
    const container = document.getElementById('botMessages');
    const div = document.createElement('div');

    div.className = `msg msg-${tipo}`;

    if (tipo === 'bot') {
        div.innerHTML = convertirMarkdown(texto);
    } else {
        div.textContent = texto;
    }

    container.appendChild(div);
    container.scrollTop = container.scrollHeight;
}

function showTyping(mostrar) {
    document.getElementById('botTyping').style.display =
        mostrar ? 'block' : 'none';
}

function speak(texto) {
    if (!voiceEnabled || !('speechSynthesis' in window)) return;

    const mensaje = new SpeechSynthesisUtterance(texto);
    mensaje.lang = 'es-CO';
    mensaje.rate = 1.0;

    window.speechSynthesis.speak(mensaje);
}

function responder(texto) {
    addMessage(texto, 'bot');
    speak(texto);
}

function normalizar(texto) {
    return texto.toLowerCase()
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .trim();
}

function obtenerCsrf() {
    const token = document.querySelector('meta[name="_csrf"]');
    const header = document.querySelector('meta[name="_csrf_header"]');

    if (!token || !header) {
        throw new Error(
            'No se encontró la configuración de seguridad CSRF.'
        );
    }

    return {
        nombre: header.content,
        token: token.content
    };
}

async function enviarFormulario(url, datos) {
    const csrf = obtenerCsrf();
    const parametros = new URLSearchParams();

    Object.entries(datos).forEach(([clave, valor]) => {
        parametros.append(clave, valor);
    });

    const headers = {
        'Content-Type': 'application/x-www-form-urlencoded',
        'Accept': 'application/json'
    };

    headers[csrf.nombre] = csrf.token;

    const respuesta = await fetch(url, {
        method: 'POST',
        credentials: 'same-origin',
        headers,
        body: parametros.toString()
    });

    let contenido;

    try {
        contenido = await respuesta.json();
    } catch {
        throw new Error('El servidor devolvió una respuesta no válida.');
    }

    return {
        ok: respuesta.ok,
        status: respuesta.status,
        data: contenido
    };
}

function detectarConsultaDeGuia(texto) {
    const mensaje = normalizar(texto);

    return /\b(rastrear|rastreo|seguimiento|estado de mi envio|donde esta mi envio|consultar mi guia|consultar guia|mi paquete|mi envio)\b/i
        .test(mensaje);
}

function registrarHistorial(usuario, asistente) {
    historialChat.push({
        role: 'user',
        content: String(usuario).slice(0, 300)
    });

    historialChat.push({
        role: 'assistant',
        content: String(asistente).slice(0, 300)
    });

    if (historialChat.length > 6) {
        historialChat = historialChat.slice(-6);
    }
}

// Respaldo: motor OpenRouter existente (GET /chat/ia).
async function consultarOpenRouter(pregunta) {
    const contexto = historialChat.length
        ? '&h=' + encodeURIComponent(JSON.stringify(historialChat.slice(-4)))
        : '';

    const respuesta = await fetch(
        '/chat/ia?q=' + encodeURIComponent(pregunta) + contexto,
        {
            method: 'GET',
            credentials: 'same-origin',
            headers: { 'Accept': 'application/json' }
        }
    );

    const datos = await respuesta.json();
    const texto = datos && datos.respuesta ? String(datos.respuesta).trim() : '';

    if (!texto) {
        throw new Error('Sin respuesta del motor de respaldo.');
    }

    return texto;
}

// Motor principal: Spring AI + RAG (Groq). Si falla, cae a OpenRouter.
async function consultarRag(pregunta) {
    try {
        const respuesta = await fetch(
            '/chat/ia/spring-ai?q=' + encodeURIComponent(pregunta),
            {
                method: 'GET',
                credentials: 'same-origin',
                headers: { 'Accept': 'application/json' }
            }
        );

        const datos = await respuesta.json();

        if (!respuesta.ok || !datos.respuesta) {
            throw new Error(datos.respuesta || 'Motor principal no disponible.');
        }

        return datos.respuesta;

    } catch (errorPrincipal) {
        return await consultarOpenRouter(pregunta);
    }
}

// Solicita el código al correo.
async function solicitarCodigo(email) {
    return await enviarFormulario(
        '/chat/ia/enviar-codigo',
        { email: email }
    );
}

// Comprueba el código recibido.
async function verificarCodigo(codigo) {
    return await enviarFormulario(
        '/chat/ia/verificar-codigo',
        { codigo: codigo }
    );
}

// Consulta una guía del cliente autorizado.
async function consultarGuia(numero) {
    return await enviarFormulario(
        '/chat/ia/consultar-guia',
        { numeroGuia: numero }
    );
}

function cancelarProceso() {
    estadoChat = 'general';
    datosCotizacion = {};
    pasoCotizacion = 0;
    responder(
        'De acuerdo. Has salido del proceso de identificación. ' +
        'Puedes continuar como visitante y consultar información ' +
        'general de Cargo Express. Para rastrear una guía tendrás ' +
        'que verificar nuevamente tu correo.'
    );
}
function iniciarCotizacion() {
    datosCotizacion = {};
    pasoCotizacion = 0;
    estadoChat = 'cotizacion';

    responder(
        '¡Claro! Vamos a calcular el valor aproximado de tu envío. 📦\n\n' +
        preguntasCotizacion[0].pregunta +
        '\n\nPuedes escribir CANCELAR si deseas salir.'
    );
}

async function procesarCotizacion(mensaje) {

    const pregunta = preguntasCotizacion[pasoCotizacion];

    if (!pregunta) return;

    // Validar datos numéricos
    if (pregunta.tipo === 'numero') {

        const valor = Number(
            mensaje.replace(/\s/g, '').replace(',', '.')
        );

        if (
            !Number.isFinite(valor) ||
            (pregunta.permitirCero ? valor < 0 : valor <= 0)
        ) {
            responder(
                pregunta.permitirCero
                    ? 'Ingresa un valor numérico igual o mayor que cero.'
                    : 'Por favor, ingresa un número mayor que cero.'
            );
            return;
        }

        datosCotizacion[pregunta.campo] = valor;

    } else {

        if (mensaje.length < 2 || /^\d+$/.test(mensaje)) {
            responder('Por favor, escribe una ciudad válida.');
            return;
        }

        datosCotizacion[pregunta.campo] = mensaje.trim();
    }

    pasoCotizacion++;

    // Si faltan datos, solicitar el siguiente
    if (pasoCotizacion < preguntasCotizacion.length) {

        responder(
            '¡Perfecto! Dato registrado.\n\n' +
            preguntasCotizacion[pasoCotizacion].pregunta
        );

        return;
    }

    // Mostrar resumen antes de calcular
    estadoChat = 'confirmarCotizacion';

    const d = datosCotizacion;

    responder(
        '¡Ya tengo los datos de tu paquete! 📦\n\n' +
        '**Resumen de tu envío:**\n' +
        '- Altura: ' + d.altura + ' cm\n' +
        '- Ancho: ' + d.ancho + ' cm\n' +
        '- Largo: ' + d.largo + ' cm\n' +
        '- Peso: ' + d.peso + ' kg\n' +
        '- Valor declarado: $' + d.valorDeclarado.toLocaleString('es-CO') + ' COP\n' +
        '- Origen: ' + d.origen + '\n' +
        '- Destino: ' + d.destino + '\n\n' +
        '¿Confirmas estos datos para calcular el valor aproximado? Escribe SI para continuar o NO para cancelar.'
    );
}

// Cotizacion determinista: usa el calculador de tarifas del servidor,
// sin depender del modelo de lenguaje.
async function calcularCotizacion() {

    const d = datosCotizacion;

    showTyping(true);

    try {
        const csrf = obtenerCsrf();
        const headers = {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        };
        headers[csrf.nombre] = csrf.token;

        const respuesta = await fetch('/cotizacion/calcular', {
            method: 'POST',
            credentials: 'same-origin',
            headers,
            body: JSON.stringify({
                origen: d.origen,
                destino: d.destino,
                altura: d.altura,
                ancho: d.ancho,
                largo: d.largo,
                peso: d.peso,
                valorDeclarado: d.valorDeclarado
            })
        });

        const datos = await respuesta.json();

        if (!respuesta.ok || datos.precio === undefined) {
            throw new Error(datos.error || 'Cotización no disponible.');
        }

        const fmt = n => Number(n).toLocaleString('es-CO', { maximumFractionDigits: 2 });

        responder(
            '**Cotización aproximada de tu envío**\n' +
            '- Origen: ' + datos.origen + '\n' +
            '- Destino: ' + datos.destino + '\n' +
            '- Peso físico: ' + fmt(datos.pesoFisico) + ' kg\n' +
            '- Peso volumétrico: ' + fmt(datos.pesoVolumetrico) + ' kg\n' +
            '- Peso cobrable: ' + fmt(datos.pesoCobrable) + ' kg\n' +
            '- Valor declarado: $' + fmt(datos.valorDeclarado) + ' COP\n' +
            '- **Valor aproximado: $' + fmt(datos.precio) + ' COP**\n\n' +
            'El valor final puede variar: al crear la guía se suma un recargo por kilómetro de la ruta vial seleccionada.'
        );

    } catch (error) {

        responder(
            'No fue posible calcular tu envío en este momento. ' +
            'Por favor, inténtalo nuevamente.'
        );

    } finally {

        showTyping(false);
        estadoChat = 'general';
        datosCotizacion = {};
        pasoCotizacion = 0;
    }
}

async function procesarMensaje(texto) {
    const mensaje = texto.trim();
    const limpio = normalizar(mensaje);

    if (limpio === 'cancelar' || limpio === 'visitante') {
        cancelarProceso();
        return;
    }
// Proceso de cotización paso a paso
if (estadoChat === 'cotizacion') {
    await procesarCotizacion(mensaje);
    return;
}

// Confirmar cotización
if (estadoChat === 'confirmarCotizacion') {

    if (limpio === 'si' || limpio === 'sí') {
        await calcularCotizacion();
        return;
    }

    if (limpio === 'no') {
        estadoChat = 'general';
        datosCotizacion = {};
        pasoCotizacion = 0;

        responder(
            'De acuerdo. Hemos cancelado la cotización. ' +
            'Cuando quieras puedes iniciar una nueva.'
        );

        return;
    }

    responder('Escribe SI para confirmar o NO para cancelar.');
    return;
}
    // ESTADO 1: solicitar correo electrónico.
    if (estadoChat === 'correo') {
        const email = mensaje.trim();

        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
            responder(
                'El correo no parece válido. Revísalo e inténtalo nuevamente.'
            );
            return;
        }

        showTyping(true);

        try {
            const resultado = await solicitarCodigo(email);

            if (resultado.ok) {
                estadoChat = 'codigo';

                responder(
                    resultado.data.respuesta +
                    ' Cuando recibas el código de seis dígitos, ' +
                    'escríbelo aquí. Si deseas continuar como visitante, ' +
                    'escribe CANCELAR.'
                );
            } else {
                responder(
                    resultado.data.respuesta ||
                    'No fue posible solicitar el código.'
                );
            }

        } catch (error) {
            responder(
                'No fue posible conectar con el servicio de correo. ' +
                'Intenta nuevamente más tarde.'
            );
        } finally {
            showTyping(false);
        }

        return;
    }

    // ESTADO 2: validar código OTP.
    if (estadoChat === 'codigo') {
        if (!/^\d{6}$/.test(mensaje)) {
            responder(
                'Escribe el código de seis dígitos que recibiste en tu correo. ' +
                'Si no deseas continuar, escribe CANCELAR.'
            );
            return;
        }

        showTyping(true);

        try {
            const resultado = await verificarCodigo(mensaje);

            if (!resultado.ok) {
                responder(
                    resultado.data.respuesta ||
                    'El código no es válido. Inténtalo nuevamente.'
                );
                return;
            }

            estadoChat = 'guia';

            const nombre = resultado.data.nombre
                ? ', ' + resultado.data.nombre
                : '';

            responder(
                '¡Correo verificado correctamente' + nombre + '! ' +
                'Ahora escribe el número de tu guía para consultar su estado.'
            );

        } catch (error) {
            responder(
                'No fue posible verificar el código. Intenta nuevamente.'
            );
        } finally {
            showTyping(false);
        }

        return;
    }

    // ESTADO 3: consultar número de guía.
    if (estadoChat === 'guia') {
        if (!/^\d+$/.test(mensaje)) {
            responder(
                'Por favor, escribe únicamente el número de tu guía. ' +
                'También puedes escribir CANCELAR.'
            );
            return;
        }

        showTyping(true);

        try {
            const resultado = await consultarGuia(mensaje);

            if (!resultado.ok) {
                responder(
                    resultado.data.respuesta ||
                    'No fue posible consultar esa guía.'
                );
                return;
            }

            const guia = resultado.data.guia;

            const respuesta =
                '¡Encontré tu guía!\n\n' +
                'Número: ' + (guia.numero ?? 'No disponible') + '\n' +
                'Estado: ' + (guia.estado ?? 'No disponible') + '\n' +
                'Origen: ' + (guia.origen ?? 'No disponible') + '\n' +
                'Destino: ' + (guia.destino ?? 'No disponible') + '\n' +
                'Fecha de creación: ' +
                (guia.fechaCreacion || 'No disponible');

            responder(respuesta);

            estadoChat = 'general';

        } catch (error) {
            responder(
                'No fue posible consultar la guía. Intenta nuevamente.'
            );
        } finally {
            showTyping(false);
        }

        return;
    }
        // Detectar solicitud de cotización
        if (
            /\b(cotizar|cotizacion|calcular envio|precio de enviar|valor de enviar|quiero enviar un paquete|quiero cotizar|costos de envio|costo de envio|cuanto cuesta enviar|cuanto vale enviar)\b/i
                .test(limpio)
        ) {
            iniciarCotizacion();
            return;
        }
    // ESTADO GENERAL: detectar solicitud de rastreo.
    if (detectarConsultaDeGuia(mensaje)) {
        estadoChat = 'correo';

        responder(
            '¡Claro! Puedo ayudarte a consultar tu envío. ' +
            'Primero necesitamos verificar que eres el titular de la cuenta. ' +
            'Escribe el correo electrónico registrado en Cargo Express. ' +
            'Si prefieres continuar como visitante, escribe CANCELAR.'
        );

        return;
    }

    // Consultas públicas mediante RAG.
    showTyping(true);

    try {
        const respuesta = await consultarRag(mensaje);

        responder(respuesta);
        registrarHistorial(mensaje, respuesta);

    } catch (error) {
        responder(
            'No pude conectar con mi motor de inteligencia artificial. ' +
            'Intenta nuevamente en unos momentos.'
        );
    } finally {
        showTyping(false);
    }
}

function sendMessage() {
    const input = document.getElementById('botInput');
    const mensaje = input.value.trim();

    if (!mensaje) return;

    addMessage(mensaje, 'user');
    input.value = '';

    procesarMensaje(mensaje);
}
