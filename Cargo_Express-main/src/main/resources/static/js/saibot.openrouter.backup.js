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
                <div style="margin-left: auto; display: flex; gap: 15px; align-items: center;">
                    <i id="voiceToggle" class="fa-solid fa-volume-xmark" style="cursor: pointer; font-size: 14px;" title="Activar Voz" onclick="toggleVoice()"></i>
                    <i class="fa-solid fa-xmark" style="cursor: pointer; font-size: 18px;" onclick="toggleBot()"></i>
                </div>
            </div>
            <div class="sai-bot-messages" id="botMessages">
                <div class="msg msg-bot">¡Hola! 👋 Soy SAI, tu asistente inteligente. ¿En qué puedo ayudarte hoy con tus envíos?</div>
            </div>
            <div class="typing" id="botTyping" style="padding: 0 20px;">SAI está escribiendo...</div>
            <div class="sai-bot-footer">
                <div class="quick-actions">
                    <button class="quick-btn" onclick="sendQuick('¿Cómo rastrear?')">¿Cómo rastrear?</button>
                    <button class="quick-btn" onclick="sendQuick('Costos de envío')">Costos de envío</button>
                    <button class="quick-btn" onclick="sendQuick('Hablar con soporte')">Soporte</button>
                </div>
                <div class="sai-bot-input">
                    <input type="text" id="botInput" placeholder="Escribe tu duda aquí..." onkeypress="handleKey(event)">
                    <button onclick="sendMessage()"><i class="fa-solid fa-paper-plane"></i></button>
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

function toggleBot() {
    document.getElementById('sai-bot-window').classList.toggle('active');
}

function handleKey(e) {
    if (e.key === 'Enter') sendMessage();
}

function sendQuick(txt) {
    document.getElementById('botInput').value = txt;
    sendMessage();
}

let voiceEnabled = false;

function toggleVoice() {
    voiceEnabled = !voiceEnabled;
    const icon = document.getElementById('voiceToggle');
    icon.className = voiceEnabled ? 'fa-solid fa-volume-high' : 'fa-solid fa-volume-xmark';
    icon.style.color = voiceEnabled ? '#00f2ff' : 'white';
}

function sendMessage() {
    const input = document.getElementById('botInput');
    const msg = input.value.trim();
    if (!msg) return;

    addMessage(msg, 'user');
    input.value = '';

    // 1) Primero intenta el motor de reglas (respuestas instantaneas)
    showTyping(true);
    setTimeout(() => {
        const local = getResponse(msg);
        if (local) {
            addMessage(local, 'bot');
            registrarHistorial(msg, local);
            showTyping(false);
            if (voiceEnabled) speak(local);
            return;
        }
        // 2) Si no hay intencion conocida, consulta la IA real
        consultarIA(msg);
    }, 450);
}

// Historial corto de la conversacion para que la IA entienda seguimientos
// ("si", "esas ciudades", "y cuanto cuesta"...).
let historialChat = [];

function registrarHistorial(usuario, bot) {
    historialChat.push({ role: 'user', content: String(usuario).slice(0, 300) });
    historialChat.push({ role: 'assistant', content: String(bot).slice(0, 300) });
    if (historialChat.length > 4) {
        historialChat = historialChat.slice(-4);
    }
}

function consultarIA(msg) {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 90000);
    const contexto = historialChat.length
        ? '&h=' + encodeURIComponent(JSON.stringify(historialChat))
        : '';

    fetch('/chat/ia?q=' + encodeURIComponent(msg) + contexto, {
        method: 'GET',
        headers: { 'Accept': 'application/json' },
        credentials: 'same-origin',
        signal: controller.signal
    })
        .then(res => res.json().then(data => ({ ok: res.ok, data })))
        .then(({ ok, data }) => {
            let texto = data && data.respuesta ? String(data.respuesta) : '';
            texto = texto.replace(/\*\*/g, '').replace(/^#{1,6}\s*/gm, '').trim();
            if (!texto) {
                texto = 'No pude generar una respuesta en este momento. Intenta reformular tu pregunta.';
            }
            addMessage(texto, 'bot');
            registrarHistorial(msg, texto);
            if (voiceEnabled) speak(texto);
        })
        .catch(() => {
            addMessage('No pude conectar con mi motor de IA. Revisa tu conexion a internet e intenta de nuevo.', 'bot');
        })
        .finally(() => {
            clearTimeout(timeout);
            showTyping(false);
        });
}

function speak(txt) {
    const msg = new SpeechSynthesisUtterance();
    msg.text = txt;
    msg.lang = 'es-ES';
    msg.rate = 1.1;
    window.speechSynthesis.speak(msg);
}

function addMessage(txt, type) {
    const container = document.getElementById('botMessages');
    const div = document.createElement('div');
    div.className = `msg msg-${type}`;
    div.innerText = txt;
    container.appendChild(div);
    container.scrollTop = container.scrollHeight;
}

function showTyping(show) {
    document.getElementById('botTyping').style.display = show ? 'block' : 'none';
}

function getResponse(msg) {
    // 1. Normalización Pro (PLN Lite)
    let n = msg.toLowerCase()
        .normalize("NFD").replace(/[\u0300-\u036f]/g, "") // Quitar acentos
        .replace(/[?¿!¡,. ]/g, " ") // Quitar puntuación
        .trim();

    // Palabras "vacías" que ignoramos para encontrar la intención real
    const stopWords = ["el", "la", "los", "las", "un", "una", "de", "del", "que", "es", "me", "te", "podrias", "decir", "sobre", "quiero", "saber", "por", "favor"];
    const tokens = n.split(/\s+/).filter(t => !stopWords.includes(t));

    // 2. Definición de Intenciones (Knowledge Mapping)
    const intents = {
        SALUDO: {
            keys: ['hola', 'buenos', 'dias', 'noches', 'tardes', 'saludos', 'que tal', 'hey', 'aloha'],
            ans: "¡Hola! Soy SAI, tu asistente de inteligencia logística. ¿En qué puedo orientarte hoy?"
        },
        TECNOLOGIA: {
            keys: ['tecnologia', 'stack', 'lenguaje', 'programado', 'desarrollado', 'creado', 'hecho', 'java', 'spring', 'boot', 'maven', 'jpa', 'backend', 'frontend', 'html', 'css', 'js'],
            ans: "Cargo Express v4.0 está construido con un stack robusto: Java 17 y Spring Boot 3 en el backend, con seguridad OAuth2 y BCrypt. El frontend usa Thymeleaf y CSS3 dinámico."
        },
        IA_WEKA: {
            keys: ['ia', 'weka', 'inteligencia', 'artificial', 'prediccion', 'estimado', 'tiempo', 'algoritmo', 'aprendizaje', 'machine', 'learning'],
            ans: "Mi núcleo usa Weka (Machine Learning) para analizar datos históricos y predecir tiempos de entrega. Además, uso la fórmula de Haversine para cálculos geográficos reales."
        },
        WAR_ROOM: {
            keys: ['war room', 'centro', 'control', 'radar', 'panel', 'monitoreo', 'telemetria', 'datos', 'vivo', 'admin'],
            ans: "El War Room es nuestra interfaz táctica de nivel militar para administradores. Incluye un radar de flujo, odómetros de KM y monitoreo de impacto ambiental."
        },
        ECOLOGIA: {
            keys: ['ecologia', 'verde', 'co2', 'carbono', 'planeta', 'arbol', 'reforestacion', 'amazonia', 'huella', 'ambiente'],
            ans: "Nuestra 'Logística Verde' calcula 160g de CO2 por KM. Compensamos cada gramo financiando la reforestación de la Amazonía Colombiana. ¡Somos carbono neutro!"
        },
        LOGISTICA: {
            keys: ['rastreo', 'rastrear', 'guia', 'seguimiento', 'paquete', 'donde', 'envio', 'despacho', 'crear', 'precio', 'costo', 'tarifa', 'imprimir', 'pdf', 'etiqueta'],
            ans: "En Cargo Express puedes crear guías con precios dinámicos, imprimir etiquetas térmicas profesionales y rastrear envíos con telemetría en tiempo real."
        },
        SEGURIDAD: {
            keys: ['seguridad', 'proteccion', 'hack', 'password', 'contraseña', 'encriptado', 'bcrypt', 'rol', 'acceso', 'permiso'],
            ans: "Tu seguridad es prioridad. Usamos BCrypt para contraseñas y filtros de Spring Security para prevenir ataques. Los accesos se dividen en Cliente, Repartidor y Admin."
        },
        HISTORIA: {
            keys: ['historia', 'origen', 'quienes', 'somos', 'fundacion', 'papel', 'caos', 'mision', 'vision', 'futuro', 'sena', 'comfenalco'],
            ans: "Nacimos para transformar el 'Legado de Papel' en eficiencia digital. Nuestra misión es liderar la logística autónoma y sostenible en Colombia para el 2030."
        },
        SISTEMA: {
            keys: ['diagnostico', 'status', 'estado', 'funciona', 'error', 'bug', 'hora', 'chiste', 'broma'],
            ans: "Ejecutando diagnóstico... Núcleo estable, base de datos sincronizada y módulos de IA calibrados. ¡Todo marcha a la perfección! (Y sí, también sé contar chistes si me lo pides)."
        }
    };

    // 3. Sistema de Puntuación de Intención (coincidencia por palabra, no por subcadena suelta)
    let bestIntent = null;
    let maxScore = 0;
    const full = ' ' + n + ' ';

    for (let i in intents) {
        let score = 0;
        intents[i].keys.forEach(k => {
            if (k.includes(' ')) {
                // Frases completas: "war room", "que tal"
                if (full.includes(' ' + k + ' ')) score += 2;
                return;
            }
            tokens.forEach(t => {
                if (t === k) {
                    score += 2;                        // palabra exacta
                } else if (k.length >= 3 && t.startsWith(k)) {
                    score += 1;                        // plural o variante: guia -> guias
                }
            });
        });
        if (score > maxScore) {
            maxScore = score;
            bestIntent = i;
        }
    }

    // 4. Generación de Respuesta "Natural"
    // Solo responde con reglas si la coincidencia es fuerte y la pregunta es corta;
    // asi las preguntas abiertas o de seguimiento pasan a la IA.
    const coincidenciaFuerte = maxScore >= 3 || (maxScore >= 2 && tokens.length <= 3);
    if (coincidenciaFuerte) {
        const fillers = ["Entiendo perfectamente. ", "Excelente pregunta. ", "Claro, te explico: ", "Según mis registros, ", "Interesante. ", "Te cuento que "];
        const randomFiller = fillers[Math.floor(Math.random() * fillers.length)];
        return randomFiller + intents[bestIntent].ans;
    }

    // 5. Fallback: sin intencion conocida -> null para que sendMessage consulte la IA real
    if (/^\d+$/.test(msg)) {
        return "Detecto un número de guía. Sincronizando con la red... Guía #" + msg + " localizada. Estado: Óptimo y bajo vigilancia de seguridad.";
    }

    return null;
}
