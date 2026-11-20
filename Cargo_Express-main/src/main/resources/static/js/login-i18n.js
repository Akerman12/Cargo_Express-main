/*
 * Traductor local ES -> EN para la pantalla de acceso (login.html).
 * No depende de internet ni de servicios externos: usa un diccionario
 * de los textos visibles de esta pagina. La preferencia se guarda en
 * localStorage para que se recuerde en la proxima visita.
 */
(function () {
    'use strict';

    var STORAGE_KEY = 'sai-login-lang';
    var ATTRS = ['placeholder', 'title', 'alt', 'aria-label'];

    // Texto en espanol (tal como aparece en el HTML) -> texto en ingles.
    var EN = {
        // <title> y cabecera
        'SAI Cargo Express | Soluciones Logísticas Inteligentes': 'SAI Cargo Express | Intelligent Logistics Solutions',
        'CONÓCENOS': 'ABOUT US',
        'RASTREAR MI GUÍA': 'TRACK MY SHIPMENT',
        'Soporte y PQRS': 'Support & Complaints',
        'SOLICITAR CUENTA': 'REQUEST AN ACCOUNT',

        // Hero
        'Expertos en Fulfilment y Distribución Nacional': 'Experts in Fulfilment and National Distribution',
        'Digitalizamos tu logística con IA para reducir errores en un 85% y garantizar entregas perfectas en toda Colombia.':
            'We digitalize your logistics with AI to reduce errors by 85% and guarantee perfect deliveries throughout Colombia.',
        'REPORTAR INCIDENCIA': 'REPORT AN ISSUE',
        'REGISTRARSE': 'SIGN UP',

        // Tarjeta de login
        'Acceso Seguro': 'Secure Login',
        'Gestiona tus guías e inventarios en tiempo real.': 'Manage your shipments and inventory in real time.',
        'Sesión finalizada de forma segura.': 'You have been signed out securely.',
        'VOLVER AL INICIO': 'BACK TO HOME',
        'Credenciales inválidas. Intente de nuevo.': 'Invalid credentials. Please try again.',
        'Email o Usuario': 'Email or Username',
        'Contraseña': 'Password',
        '¿Olvidaste tu contraseña?': 'Forgot your password?',
        'INGRESAR AL PORTAL': 'SIGN IN TO PORTAL',
        'Acceso Corporativo': 'Corporate Access',
        'Continuar con Google Workspace': 'Continue with Google Workspace',

        // Mensajes que llegan desde el servidor
        'Has cerrado sesión correctamente.': 'You have successfully signed out.',
        'Registro exitoso. Ya puedes iniciar sesión.': 'Registration successful. You can now sign in.',
        'Registro exitoso. Tu cuenta de Repartidor está pendiente de aprobación por un administrador.':
            'Registration successful. Your Courier account is pending administrator approval.',
        'Error: Credenciales inválidas o cuenta bloqueada por el administrador.':
            'Error: Invalid credentials or account blocked by the administrator.',

        // Estadisticas
        'Más eficiente': 'More efficient',
        'Guías diarias': 'Daily shipments',
        'Respuesta técnica': 'Technical response',
        'Trazabilidad': 'Traceability',

        // Seccion de servicios
        'Monitoreo en Tiempo Real Activo': 'Real-Time Monitoring Active',
        'Líderes en Logística Express': 'Leaders in Express Logistics',
        'Conectamos tus productos con el mundo entero. Seguridad, rapidez y tecnología en cada envío bajo estándares internacionales.':
            'We connect your products with the entire world. Security, speed and technology in every shipment under international standards.',
        'Trazabilidad Total': 'Full Traceability',
        'Seguimiento satelital de cada paquete con actualizaciones cada 5 segundos.':
            'Satellite tracking for every package with updates every 5 seconds.',
        'Garantía de Seguridad': 'Security Guarantee',
        'Protocolos de protección avanzada para mercancía de alto valor.':
            'Advanced protection protocols for high-value merchandise.',
        'Entrega Priority': 'Priority Delivery',
        'Optimización de rutas mediante IA para entregas en tiempo récord.':
            'AI-based route optimization for record-time deliveries.',

        // Tarjetas publicitarias
        'Envíos seguros': 'Secure shipments',
        'Envíos Seguros': 'Secure Shipments',
        'Embalamos los pedidos con garantía de': 'We pack orders with a guarantee of',
        'máxima protección': 'maximum protection',
        'durante su transporte.': 'during transport.',
        'Facilitamos la': 'We provide',
        'manipulación y optimización del espacio': 'handling and optimization of space',
        'y costes del transporte.': 'and transport costs.',
        'Envíos personalizados': 'Custom shipments',
        'Envíos Personalizados': 'Custom Shipments',
        'Asesoramos sobre los': 'We advise on the',
        'embalajes más adecuados': 'most suitable packaging',
        'para cada producto y necesidad.': 'for each product and need.',
        'Incluimos la opción de utilizar': 'We include the option to use',
        'tu propio material': 'your own material',
        'de embalaje.': 'for packaging.',
        'Envíos controlados': 'Controlled shipments',
        'Envíos Controlados': 'Controlled Shipments',
        'Etiqueta única': 'Single label',
        'para destinos nacionales e internacionales en transportes con SAI.':
            'for national and international destinations in shipments with SAI.',
        'Integración con tu proveedor': 'Integration with your provider',
        'de transporte, en caso de no ser SAI.': 'of transport, if not SAI.',

        // Tecnologia y pie de pagina
        'Tecnología de Vanguardia': 'Cutting-Edge Technology',
        'Construido sobre Spring Boot para máxima escalabilidad y seguridad OAuth2.':
            'Built on Spring Boot for maximum scalability and OAuth2 security.',
        'Implementamos algoritmos de inteligencia artificial (Weka) para predecir tiempos de entrega y optimizar rutas de distribución, eliminando el ineficaz sistema manual basado en papel.':
            'We implement artificial intelligence algorithms (Weka) to predict delivery times and optimize distribution routes, eliminating the inefficient manual paper-based system.',
        '© 2026 SAI Cargo Express. Todos los derechos reservados.': '© 2026 SAI Cargo Express. All rights reserved.',
        'Seguridad respaldada por Spring Security & Google Workspace SSO.':
            'Security powered by Spring Security & Google Workspace SSO.'
    };

    var originals = new WeakMap();
    var attrOriginals = new WeakMap();

    function normalize(value) {
        return String(value).replace(/\s+/g, ' ').trim();
    }

    function shouldSkip(node) {
        var parent = node.parentNode;
        if (!parent) return true;
        var tag = parent.nodeName;
        return tag === 'SCRIPT' || tag === 'STYLE' || tag === 'NOSCRIPT' || tag === 'TEXTAREA';
    }

    function translateTextNode(node, toEnglish) {
        if (!node.nodeValue || !node.nodeValue.trim()) return;

        if (!originals.has(node)) originals.set(node, node.nodeValue);
        var original = originals.get(node);
        var key = normalize(original);
        var translated = EN[key];
        if (!translated) return;

        if (!toEnglish) {
            node.nodeValue = original;
            return;
        }

        var leading = (original.match(/^\s*/) || [''])[0];
        var trailing = (original.match(/\s*$/) || [''])[0];
        node.nodeValue = leading + translated + trailing;
    }

    function walkTextNodes(root, toEnglish) {
        var walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, null);
        var node;
        while ((node = walker.nextNode())) {
            if (!shouldSkip(node)) translateTextNode(node, toEnglish);
        }
    }

    function translateAttributes(root, toEnglish) {
        var elements = root.querySelectorAll('[placeholder], [title], [alt], [aria-label]');
        elements.forEach(function (el) {
            ATTRS.forEach(function (attr) {
                if (!el.hasAttribute(attr)) return;
                var current = el.getAttribute(attr);
                var store = attrOriginals.get(el) || {};
                if (!(attr in store)) store[attr] = current;
                attrOriginals.set(el, store);

                var original = store[attr];
                var translated = EN[normalize(original)];
                if (!translated) return;
                el.setAttribute(attr, toEnglish ? translated : original);
            });
        });
    }

    function applyLanguage(lang) {
        var toEnglish = lang === 'en';
        var root = document.body || document.documentElement;

        walkTextNodes(root, toEnglish);
        translateAttributes(root, toEnglish);

        document.documentElement.setAttribute('lang', toEnglish ? 'en' : 'es');

        var button = document.getElementById('langToggle');
        if (button) {
            var label = button.querySelector('.lang-label');
            if (label) label.textContent = toEnglish ? 'ES' : 'EN';
            var help = toEnglish ? 'Switch to Spanish' : 'Traducir al inglés';
            button.setAttribute('title', help);
            button.setAttribute('aria-label', help);
            button.setAttribute('aria-pressed', toEnglish ? 'true' : 'false');
        }

        try {
            localStorage.setItem(STORAGE_KEY, toEnglish ? 'en' : 'es');
        } catch (e) {
            /* localStorage puede estar bloqueado; el boton sigue funcionando */
        }
    }

    function currentLanguage() {
        try {
            return localStorage.getItem(STORAGE_KEY) === 'en' ? 'en' : 'es';
        } catch (e) {
            return 'es';
        }
    }

    function init() {
        var button = document.getElementById('langToggle');
        if (!button) return;

        button.addEventListener('click', function () {
            applyLanguage(currentLanguage() === 'en' ? 'es' : 'en');
        });

        // Si el usuario ya eligio ingles antes, se aplica al cargar.
        if (currentLanguage() === 'en') applyLanguage('en');
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
