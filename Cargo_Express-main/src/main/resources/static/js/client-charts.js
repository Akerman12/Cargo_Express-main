/*
 * Graficas del Portal de Clientes.
 * Usa el mismo endpoint que el panel, que ya filtra por rol: un cliente
 * solo recibe sus propios envios.
 */
(function () {
    'use strict';

    var root = document.getElementById('clienteCharts');
    if (!root) return;
    if (typeof Chart === 'undefined') {
        console.warn('Chart.js no esta disponible en el portal de clientes.');
        return;
    }

    var url = root.getAttribute('data-stats-url') || '/menu/estadisticas';
    var palette = ['#002E7D', '#0EA5E9', '#F59E0B', '#10B981', '#8B5CF6', '#EF4444', '#14B8A6', '#F97316'];

    Chart.defaults.font.family = "'Inter', system-ui, -apple-system, sans-serif";
    Chart.defaults.font.size = 10;
    Chart.defaults.color = '#64748B';

    function numero(valor) {
        return new Intl.NumberFormat('es-CO', { maximumFractionDigits: 0 }).format(Number(valor) || 0);
    }

    var tooltip = {
        backgroundColor: '#0B1220',
        titleColor: '#F8FAFC',
        bodyColor: '#E2E8F0',
        padding: 10,
        cornerRadius: 9,
        displayColors: false
    };

    function crearMensual(datos) {
        var canvas = document.getElementById('cliChartMensual');
        if (!canvas) return;
        var serie = datos.mensual || { labels: [], data: [] };

        new Chart(canvas, {
            type: 'line',
            data: {
                labels: serie.labels,
                datasets: [{
                    label: 'Envios',
                    data: serie.data,
                    borderColor: '#002E7D',
                    backgroundColor: 'rgba(0,46,125,.10)',
                    borderWidth: 2.4,
                    pointRadius: 2.5,
                    pointHoverRadius: 4,
                    pointBackgroundColor: '#FFFFFF',
                    pointBorderColor: '#002E7D',
                    pointBorderWidth: 2,
                    tension: 0.38,
                    fill: true
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                interaction: { mode: 'index', intersect: false },
                plugins: { legend: { display: false }, tooltip: tooltip },
                scales: {
                    x: {
                        grid: { display: false },
                        ticks: { maxRotation: 0, autoSkip: true, maxTicksLimit: 6, font: { size: 9 } }
                    },
                    y: {
                        beginAtZero: true,
                        grid: { color: 'rgba(100,116,139,.12)' },
                        ticks: { precision: 0, font: { size: 9 } }
                    }
                }
            }
        });
    }

    function crearTipos(datos) {
        var canvas = document.getElementById('cliChartTipos');
        if (!canvas) return;
        var serie = datos.tipos || { labels: [], data: [] };

        new Chart(canvas, {
            type: 'doughnut',
            data: {
                labels: serie.labels,
                datasets: [{
                    data: serie.data,
                    backgroundColor: serie.labels.map(function (_, i) { return palette[i % palette.length]; }),
                    borderColor: '#FFFFFF',
                    borderWidth: 2,
                    hoverOffset: 5
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                cutout: '60%',
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: { usePointStyle: true, boxWidth: 7, padding: 10, font: { size: 9 } }
                    },
                    tooltip: {
                        backgroundColor: '#0B1220',
                        titleColor: '#F8FAFC',
                        bodyColor: '#E2E8F0',
                        padding: 10,
                        cornerRadius: 9,
                        callbacks: {
                            label: function (ctx) {
                                var datosSerie = ctx.dataset.data || [];
                                var total = datosSerie.reduce(function (a, b) { return a + Number(b); }, 0);
                                var pct = total > 0 ? Math.round((Number(ctx.parsed) / total) * 100) : 0;
                                return ' ' + ctx.label + ': ' + numero(ctx.parsed) + ' (' + pct + '%)';
                            }
                        }
                    }
                }
            }
        });
    }

    fetch(url, {
        method: 'GET',
        credentials: 'same-origin',
        headers: { 'Accept': 'application/json' }
    })
        .then(function (respuesta) {
            if (!respuesta.ok) throw new Error('HTTP ' + respuesta.status);
            return respuesta.json();
        })
        .then(function (datos) {
            var base = document.getElementById('cliActividadBase');
            if (base) {
                var destino = datos.destinoTop ? ' · Destino top: ' + datos.destinoTop : '';
                base.textContent = numero(datos.total) + ' envios · ' +
                    numero(datos.pesoTotal) + ' kg movidos' + destino;
            }
            crearMensual(datos);
            crearTipos(datos);
        })
        .catch(function (error) {
            console.error('No se pudieron cargar las graficas del portal:', error);
            var base = document.getElementById('cliActividadBase');
            if (base) base.textContent = 'No se pudo cargar tu actividad en este momento.';
        });
})();
