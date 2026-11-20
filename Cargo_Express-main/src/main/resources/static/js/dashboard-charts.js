/*
 * Graficas del Dashboard Main (/menu).
 * Consume /dashboard/stats, que ya aplica el filtro por rol.
 */
(function () {
    'use strict';

    var root = document.getElementById('dashboardCharts');
    if (!root) return;

    if (typeof Chart === 'undefined') {
        root.insertAdjacentHTML('beforeend', '<div class="chart-error">No se pudo cargar el motor de graficas (Chart.js).</div>');
        return;
    }

    var palette = ['#002E7D', '#0EA5E9', '#F59E0B', '#10B981', '#8B5CF6', '#EF4444', '#14B8A6', '#F97316', '#64748B', '#EC4899'];
    var statsUrl = root.getAttribute('data-stats-url') || '/menu/estadisticas';

    Chart.defaults.font.family = "'Inter', system-ui, -apple-system, sans-serif";
    Chart.defaults.font.size = 11;
    Chart.defaults.color = '#64748B';

    function numero(valor) {
        return new Intl.NumberFormat('es-CO', { maximumFractionDigits: 0 }).format(Number(valor) || 0);
    }

    function decimal(valor) {
        return new Intl.NumberFormat('es-CO', {
            minimumFractionDigits: 1,
            maximumFractionDigits: 1
        }).format(Number(valor) || 0);
    }

    function dinero(valor) {
        return '$' + numero(valor);
    }

    function tooltipBase() {
        return {
            backgroundColor: '#0F172A',
            titleColor: '#F8FAFC',
            bodyColor: '#E2E8F0',
            padding: 10,
            cornerRadius: 8,
            displayColors: false
        };
    }

    function opcionesCartesianas(horizontal, mostrarLeyenda) {
        var opciones = {
            responsive: true,
            maintainAspectRatio: false,
            indexAxis: horizontal ? 'y' : 'x',
            interaction: { mode: 'index', intersect: false },
            plugins: {
                legend: {
                    display: !!mostrarLeyenda,
                    position: 'bottom',
                    labels: { usePointStyle: true, boxWidth: 8, padding: 14 }
                },
                tooltip: tooltipBase()
            },
            scales: {
                x: {
                    beginAtZero: true,
                    grid: { color: 'rgba(100,116,139,.12)', drawBorder: false },
                    ticks: { precision: 0 }
                },
                y: {
                    beginAtZero: true,
                    grid: { color: 'rgba(100,116,139,.12)', drawBorder: false },
                    ticks: { precision: 0 }
                }
            }
        };

        if (horizontal) {
            opciones.scales.y.grid.display = false;
        }

        return opciones;
    }

    function opcionesDona() {
        return {
            responsive: true,
            maintainAspectRatio: false,
            cutout: '62%',
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: { usePointStyle: true, boxWidth: 8, padding: 12 }
                },
                tooltip: {
                    backgroundColor: '#0F172A',
                    titleColor: '#F8FAFC',
                    bodyColor: '#E2E8F0',
                    padding: 10,
                    cornerRadius: 8,
                    callbacks: {
                        label: function (contexto) {
                            var datos = contexto.dataset.data || [];
                            var total = datos.reduce(function (a, b) { return a + Number(b); }, 0);
                            var porcentaje = total > 0 ? Math.round((Number(contexto.parsed) / total) * 100) : 0;
                            return ' ' + contexto.label + ': ' + numero(contexto.parsed) + ' (' + porcentaje + '%)';
                        }
                    }
                }
            }
        };
    }

    function crear(id, configuracion) {
        var canvas = document.getElementById(id);
        if (!canvas) return null;
        return new Chart(canvas, configuracion);
    }

    function renderizar(datos) {
        document.getElementById('kpiPesoPromedio').textContent = decimal(datos.pesoPromedio) + ' kg';
        document.getElementById('kpiPesoTotal').textContent = decimal(datos.pesoTotal) + ' kg';
        document.getElementById('kpiIngresos').textContent = dinero(datos.ingresoTotal);
        document.getElementById('kpiCiudades').textContent = numero(datos.ciudadesDistintas);

        var base = document.getElementById('chartsBase');
        if (base) {
            base.textContent = numero(datos.total) + ' envios analizados' +
                (datos.destinoTop ? ' · Destino top: ' + datos.destinoTop : '') +
                (datos.tipoTop ? ' · Carga top: ' + datos.tipoTop : '');
        }

        var mensual = datos.mensual || { labels: [], data: [] };
        crear('chartMensual', {
            type: 'line',
            data: {
                labels: mensual.labels,
                datasets: [{
                    label: 'Envios',
                    data: mensual.data,
                    borderColor: '#002E7D',
                    backgroundColor: 'rgba(0,46,125,.10)',
                    borderWidth: 2.5,
                    pointRadius: 3,
                    pointHoverRadius: 5,
                    pointBackgroundColor: '#FFFFFF',
                    pointBorderColor: '#002E7D',
                    pointBorderWidth: 2,
                    tension: 0.35,
                    fill: true
                }]
            },
            options: opcionesCartesianas(false, false)
        });

        var anual = datos.anual || { labels: [], data: [] };
        crear('chartAnual', {
            type: 'bar',
            data: {
                labels: anual.labels,
                datasets: [{
                    label: 'Envios',
                    data: anual.data,
                    backgroundColor: '#F59E0B',
                    hoverBackgroundColor: '#D97706',
                    borderRadius: 8,
                    maxBarThickness: 70
                }]
            },
            options: opcionesCartesianas(false, false)
        });

        var destinos = datos.destinos || { labels: [], data: [] };
        crear('chartDestinos', {
            type: 'bar',
            data: {
                labels: destinos.labels,
                datasets: [{
                    label: 'Envios',
                    data: destinos.data,
                    backgroundColor: destinos.labels.map(function (_, i) { return palette[i % palette.length]; }),
                    borderRadius: 6,
                    maxBarThickness: 26
                }]
            },
            options: opcionesCartesianas(true, false)
        });

        var origenes = datos.origenes || { labels: [], data: [] };
        crear('chartOrigenes', {
            type: 'bar',
            data: {
                labels: origenes.labels,
                datasets: [{
                    label: 'Envios',
                    data: origenes.data,
                    backgroundColor: 'rgba(14,165,233,.85)',
                    hoverBackgroundColor: '#0369A1',
                    borderRadius: 6,
                    maxBarThickness: 26
                }]
            },
            options: opcionesCartesianas(true, false)
        });

        var tipos = datos.tipos || { labels: [], data: [] };
        crear('chartTipos', {
            type: 'doughnut',
            data: {
                labels: tipos.labels,
                datasets: [{
                    data: tipos.data,
                    backgroundColor: tipos.labels.map(function (_, i) { return palette[i % palette.length]; }),
                    borderColor: '#FFFFFF',
                    borderWidth: 2,
                    hoverOffset: 6
                }]
            },
            options: opcionesDona()
        });

        var pesos = datos.pesos || { labels: [], data: [] };
        crear('chartPesos', {
            type: 'bar',
            data: {
                labels: pesos.labels,
                datasets: [{
                    label: 'Envios',
                    data: pesos.data,
                    backgroundColor: 'rgba(16,185,129,.85)',
                    hoverBackgroundColor: '#047857',
                    borderRadius: 8,
                    maxBarThickness: 60
                }]
            },
            options: opcionesCartesianas(false, false)
        });

        var estados = datos.estados || { labels: [], data: [] };
        crear('chartEstados', {
            type: 'doughnut',
            data: {
                labels: estados.labels,
                datasets: [{
                    data: estados.data,
                    backgroundColor: estados.labels.map(function (_, i) { return palette[(i + 2) % palette.length]; }),
                    borderColor: '#FFFFFF',
                    borderWidth: 2,
                    hoverOffset: 6
                }]
            },
            options: opcionesDona()
        });
    }

    function mostrarError(mensaje) {
        root.insertAdjacentHTML('beforeend',
            '<div class="chart-error"><i class="fa-solid fa-triangle-exclamation"></i> ' + mensaje + '</div>');
    }

    fetch(statsUrl, {
        method: 'GET',
        credentials: 'same-origin',
        headers: { 'Accept': 'application/json' }
    })
        .then(function (respuesta) {
            if (!respuesta.ok) {
                throw new Error('HTTP ' + respuesta.status);
            }
            return respuesta.json();
        })
        .then(renderizar)
        .catch(function (error) {
            console.error('Error cargando estadisticas del dashboard:', error);
            mostrarError('No se pudieron cargar las graficas. Recarga la pagina o inicia sesion de nuevo.');
        });
})();
