/*
 * SlideCommit - "desliza para pagar" en JS puro (sin React).
 *
 * Replica el componente original: arrastre con inercia y rebote, estados
 * pending / done / error, confirmacion por teclado (End, flechas, Home) y
 * respeto por prefers-reduced-motion.
 *
 * Uso:
 *   <div id="slidePay" class="slide-commit"
 *        data-label="Desliza para pagar"
 *        data-done-label="Pago confirmado"
 *        data-error-label="Pago no procesado"
 *        data-confirm="confirmarPago"
 *        data-done="pagoConfirmado"
 *        data-error="pagoFallido"></div>
 *
 *   window.confirmarPago = () => Promise<void>   // rechaza con el motivo
 */
(function (global) {
    'use strict';

    var PAD = 4;
    var MIN_PENDING = 300;
    var EASE_OUT = 'cubic-bezier(0.23, 1, 0.32, 1)';
    var SPEED = 50;
    var MASS = 0.9;
    var STIFFNESS = 260 + (SPEED / 100) * 640;
    var CRITICAL = 2 * Math.sqrt(STIFFNESS * MASS);
    var COMMIT_DAMPING = CRITICAL;
    var HOME_DAMPING = CRITICAL * (1 - 0.38);

    var ARROW_SVG =
        '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" ' +
        'stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' +
        '<path d="M5 12h13"/><path d="m13 6 6 6-6 6"/></svg>';

    var CHECK_SVG =
        '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" ' +
        'stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' +
        '<path d="m5 13 4.5 4.5L19 7"/></svg>';

    var SPIN_SVG =
        '<svg class="slide-commit__spinner" width="20" height="20" viewBox="0 0 24 24" aria-hidden="true">' +
        '<circle cx="12" cy="12" r="9" fill="none" stroke="currentColor" stroke-width="2.4" stroke-opacity="0.25"/>' +
        '<path d="M12 3a9 9 0 0 1 9 9" fill="none" stroke="currentColor" stroke-width="2.4" ' +
        'stroke-linecap="round"/></svg>';

    function clamp(value, min, max) {
        return Math.min(max, Math.max(min, value));
    }

    function reducedMotion() {
        return !!(global.matchMedia && global.matchMedia('(prefers-reduced-motion: reduce)').matches);
    }

    function make(tag, className, html) {
        var el = document.createElement(tag);
        if (className) el.className = className;
        if (html != null) el.innerHTML = html;
        return el;
    }

    function create(root, options) {
        if (!root || root.getAttribute('data-sc-ready') === '1') return null;

        var opts = {
            label: 'Desliza para pagar',
            doneLabel: 'Pago confirmado',
            errorLabel: 'Pago no procesado',
            holdMs: 1500,
            onConfirm: null,
            onDone: null,
            onError: null
        };
        Object.keys(options || {}).forEach(function (key) {
            if (options[key] !== undefined && options[key] !== null) opts[key] = options[key];
        });

        root.classList.add('slide-commit');
        root.setAttribute('data-sc-ready', '1');
        root.innerHTML = '';

        var track = make('div', 'slide-commit__track');

        var label = make('span', 'slide-commit__label');
        label.textContent = opts.label;
        label.setAttribute('aria-hidden', 'true');

        var errorText = make('span', 'slide-commit__text--error');
        errorText.textContent = opts.errorLabel;
        errorText.setAttribute('aria-hidden', 'true');

        var capsule = make('div', 'slide-commit__capsule');
        capsule.setAttribute('role', 'slider');
        capsule.setAttribute('tabindex', '0');
        capsule.setAttribute('aria-label', opts.label);
        capsule.setAttribute('aria-valuemin', '0');
        capsule.setAttribute('aria-valuemax', '100');
        capsule.setAttribute('aria-valuenow', '0');
        capsule.setAttribute('aria-valuetext', opts.label + ', 0%');
        capsule.setAttribute('aria-orientation', 'horizontal');

        var arrow = make('span', 'slide-commit__arrow', ARROW_SVG);
        arrow.setAttribute('aria-hidden', 'true');

        var spin = make('span', 'slide-commit__spin', SPIN_SVG);
        spin.setAttribute('aria-hidden', 'true');

        var done = make('span', 'slide-commit__done');
        done.setAttribute('aria-hidden', 'true');
        done.innerHTML = CHECK_SVG + '<span></span>';
        done.lastChild.textContent = opts.doneLabel;

        capsule.appendChild(arrow);
        capsule.appendChild(spin);
        capsule.appendChild(done);
        track.appendChild(label);
        track.appendChild(errorText);
        track.appendChild(capsule);

        var live = make('span', 'slide-commit__sr');
        live.setAttribute('aria-live', 'polite');

        root.appendChild(track);
        root.appendChild(live);

        var phase = 'idle';
        var width = 0;
        var height = 0;
        var gripSize = 48;
        var inner = 0;
        var travel = 1;
        var x = 0;
        var velocity = 0;
        var raf = 0;
        var dragging = false;
        var pointerId = null;
        var grab = null;
        var moved = false;
        var history = [];
        var timers = [];

        function schedule(fn, delay) {
            var id = global.setTimeout(function () {
                timers = timers.filter(function (item) { return item !== id; });
                fn();
            }, delay);
            timers.push(id);
            return id;
        }

        function clearTimers() {
            timers.forEach(function (id) { global.clearTimeout(id); });
            timers = [];
        }

        function setPhase(next) {
            phase = next;
            root.setAttribute('data-phase', next);
            live.textContent = next === 'pending' ? 'Procesando pago'
                : next === 'done' ? opts.doneLabel
                    : next === 'error' ? opts.errorLabel : '';
        }

        function render() {
            var visible = clamp(gripSize + x, gripSize, inner);
            capsule.style.width = visible + 'px';

            var progress = clamp(x / travel, 0, 1);
            label.style.opacity = String(clamp(1 - progress * 1.9, 0, 1));
            if (phase === 'idle') {
                arrow.style.opacity = String(clamp(1 - progress * 1.6, 0, 1));
            } else {
                arrow.style.opacity = '';
            }

            capsule.setAttribute('aria-valuenow', String(Math.round(progress * 100)));
            capsule.setAttribute('aria-valuetext', opts.label + ', ' + Math.round(progress * 100) + '%');
        }

        function measure() {
            width = root.clientWidth || 280;
            height = root.clientHeight || 56;
            gripSize = Math.max(24, Math.round(height - PAD * 2));
            inner = Math.max(gripSize, width - PAD * 2);
            travel = Math.max(1, inner - gripSize);
            root.style.setProperty('--sc-grip', gripSize + 'px');
            render();
        }

        function cancelSpring() {
            if (raf) global.cancelAnimationFrame(raf);
            raf = 0;
        }

        function springTo(target, damping, onSettled) {
            cancelSpring();
            if (reducedMotion()) {
                x = target;
                velocity = 0;
                render();
                if (onSettled) onSettled();
                return;
            }

            var last = performance.now();
            function step(now) {
                var dt = Math.min(0.033, Math.max(0.001, (now - last) / 1000));
                last = now;
                var force = -STIFFNESS * (x - target) - damping * velocity;
                velocity += (force / MASS) * dt;
                x += velocity * dt;

                if (Math.abs(x - target) < 0.4 && Math.abs(velocity) < 8) {
                    x = target;
                    velocity = 0;
                    raf = 0;
                    render();
                    if (onSettled) onSettled();
                    return;
                }
                render();
                raf = global.requestAnimationFrame(step);
            }
            raf = global.requestAnimationFrame(step);
        }

        function pointerVelocity() {
            if (history.length < 2) return 0;
            var first = history[0];
            var last = history[history.length - 1];
            return ((last[1] - first[1]) / Math.max(1, last[0] - first[0])) * 1000;
        }

        function collapse(ms) {
            capsule.style.transition = reducedMotion() ? 'none' : 'width ' + ms + 'ms ' + EASE_OUT;
            capsule.style.width = gripSize + 'px';
        }

        function expand() {
            capsule.style.transition = reducedMotion() ? 'none' : 'width 240ms ' + EASE_OUT;
            capsule.style.width = inner + 'px';
        }

        function resolve() {
            if (phase === 'error') return;
            setPhase('done');
            if (opts.onDone) {
                try { opts.onDone(); } catch (err) { console.error('SlideCommit onDone:', err); }
            }
            schedule(function () {
                if (phase !== 'done') return;
                setPhase('idle');
                capsule.style.transition = 'none';
                springTo(0, HOME_DAMPING);
            }, Math.max(400, opts.holdMs || 0));
        }

        function fail(reason) {
            setPhase('error');
            if (opts.onError) {
                try { opts.onError(reason); } catch (err) { console.error('SlideCommit onError:', err); }
            }
            velocity = 0;
            collapse(420);
            schedule(function () {
                if (phase !== 'error') return;
                x = 0;
                capsule.style.transition = 'none';
                render();
                setPhase('idle');
            }, Math.max(1200, opts.holdMs || 0));
        }

        function commit() {
            if (phase !== 'idle') return;
            clearTimers();
            dragging = false;
            pointerId = null;
            root.removeAttribute('data-held');
            cancelSpring();

            x = travel;
            velocity = 0;
            render();
            setPhase('pending');
            expand();

            var started = performance.now();
            var outcome;
            try {
                outcome = opts.onConfirm ? opts.onConfirm() : null;
            } catch (reason) {
                schedule(function () { fail(reason); }, MIN_PENDING);
                return;
            }

            var promise = outcome && typeof outcome.then === 'function' ? outcome : null;
            if (!promise) {
                schedule(resolve, MIN_PENDING);
                return;
            }

            promise.then(function () {
                var wait = Math.max(0, MIN_PENDING - (performance.now() - started));
                schedule(resolve, wait);
            }, function (reason) {
                var wait = Math.max(0, MIN_PENDING - (performance.now() - started));
                schedule(function () { fail(reason); }, wait);
            });
        }

        function localX(clientX) {
            var rect = track.getBoundingClientRect();
            if (!rect.width) return 0;
            return (clientX - rect.left) * (width / rect.width);
        }

        function onPointerDown(event) {
            if (phase !== 'idle' || event.button !== 0) return;
            cancelSpring();
            dragging = true;
            pointerId = event.pointerId;
            grab = null;
            moved = false;
            history = [];
            velocity = 0;
            root.setAttribute('data-held', '');
            capsule.style.transition = 'none';
            try { track.setPointerCapture(event.pointerId); } catch (err) { /* noop */ }
        }

        function onPointerMove(event) {
            if (!dragging || event.pointerId !== pointerId) return;
            var at = localX(event.clientX);
            if (grab === null) {
                grab = at - x;
                return;
            }
            var next = clamp(at - grab, 0, travel);
            if (Math.abs(next - x) > 0.5) moved = true;
            x = next;
            history.push([event.timeStamp, x]);
            if (history.length > 4) history.shift();
            render();
        }

        function onPointerUp(event) {
            if (!dragging || (event && event.pointerId !== pointerId)) return;
            dragging = false;
            root.removeAttribute('data-held');
            try {
                if (event) track.releasePointerCapture(event.pointerId);
            } catch (err) { /* noop */ }
            pointerId = null;

            if (x >= travel - 0.5) {
                x = travel;
                commit();
                return;
            }
            if (moved) {
                velocity = clamp(pointerVelocity(), -2400, 2400);
            } else {
                velocity = 0;
            }
            springTo(0, HOME_DAMPING);
        }

        function onKeyDown(event) {
            if (phase !== 'idle') return;
            var step = travel / 10;

            if (event.key === 'End') {
                event.preventDefault();
                x = travel;
                render();
                commit();
            } else if (event.key === 'ArrowRight' || event.key === 'ArrowUp') {
                event.preventDefault();
                cancelSpring();
                x = clamp(x + step, 0, travel);
                velocity = 0;
                render();
                if (x >= travel) commit();
            } else if (event.key === 'ArrowLeft' || event.key === 'ArrowDown') {
                event.preventDefault();
                cancelSpring();
                x = clamp(x - step, 0, travel);
                velocity = 0;
                render();
            } else if (event.key === 'Home' || event.key === 'Escape') {
                event.preventDefault();
                x = 0;
                velocity = 0;
                render();
            }
        }

        track.addEventListener('pointerdown', onPointerDown);
        global.addEventListener('pointermove', onPointerMove);
        global.addEventListener('pointerup', onPointerUp);
        global.addEventListener('pointercancel', onPointerUp);
        capsule.addEventListener('keydown', onKeyDown);

        if (global.ResizeObserver) {
            new global.ResizeObserver(measure).observe(root);
        } else {
            global.addEventListener('resize', measure);
        }

        setPhase('idle');
        measure();

        return {
            refresh: measure,
            reset: function () {
                clearTimers();
                cancelSpring();
                x = 0;
                velocity = 0;
                setPhase('idle');
                render();
            }
        };
    }

    function autoInit() {
        var root = document.getElementById('slidePay');
        if (!root) return;

        function callback(name) {
            if (!name) return null;
            return function () {
                var fn = global[name];
                return typeof fn === 'function' ? fn.apply(global, arguments) : null;
            };
        }

        create(root, {
            label: root.getAttribute('data-label') || undefined,
            doneLabel: root.getAttribute('data-done-label') || undefined,
            errorLabel: root.getAttribute('data-error-label') || undefined,
            holdMs: root.getAttribute('data-hold-ms') ? Number(root.getAttribute('data-hold-ms')) : undefined,
            onConfirm: callback(root.getAttribute('data-confirm')),
            onDone: callback(root.getAttribute('data-done')),
            onError: callback(root.getAttribute('data-error'))
        });
    }

    global.SlideCommit = { create: create, init: autoInit };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', autoInit);
    } else {
        autoInit();
    }
})(window);
