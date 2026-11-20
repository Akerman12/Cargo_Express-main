import 'package:flutter/material.dart';

class TrackingScreen extends StatefulWidget {
  const TrackingScreen({super.key});

  @override
  State<TrackingScreen> createState() => _TrackingScreenState();
}

class _TrackingScreenState extends State<TrackingScreen> {
  final _searchController = TextEditingController();
  bool _isSearching = false;
  bool _hasSearched = false;

  void _performSearch() {
    if (_searchController.text.trim().isEmpty) return;
    
    setState(() {
      _isSearching = true;
      _hasSearched = false;
    });

    // Simular búsqueda en backend
    Future.delayed(const Duration(seconds: 1), () {
      if (mounted) {
        setState(() {
          _isSearching = false;
          _hasSearched = true;
        });
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Rastrear Envío', style: TextStyle(fontWeight: FontWeight.bold)),
        backgroundColor: const Color(0xFF0D47A1),
        foregroundColor: Colors.white,
        elevation: 0,
      ),
      body: Column(
        children: [
          // Sección Superior - Buscador
          Container(
            color: const Color(0xFF0D47A1),
            padding: const EdgeInsets.fromLTRB(20, 10, 20, 30),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Sigue tu paquete',
                  style: TextStyle(color: Colors.white, fontSize: 24, fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 8),
                const Text(
                  'Ingresa tu número de guía para conocer el estado actual.',
                  style: TextStyle(color: Colors.white70, fontSize: 16),
                ),
                const SizedBox(height: 20),
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: _searchController,
                        style: const TextStyle(color: Colors.black87),
                        decoration: InputDecoration(
                          hintText: 'Ej. GUIA-2026-1029',
                          filled: true,
                          fillColor: Colors.white,
                          contentPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
                          border: OutlineInputBorder(
                            borderRadius: BorderRadius.circular(12),
                            borderSide: BorderSide.none,
                          ),
                          prefixIcon: const Icon(Icons.qr_code_scanner, color: Colors.grey),
                        ),
                        onSubmitted: (_) => _performSearch(),
                      ),
                    ),
                    const SizedBox(width: 10),
                    Container(
                      decoration: BoxDecoration(
                        color: Colors.orangeAccent,
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: IconButton(
                        icon: const Icon(Icons.search, color: Colors.white),
                        onPressed: _performSearch,
                        tooltip: 'Buscar',
                      ),
                    )
                  ],
                ),
              ],
            ),
          ),

          // Resultados / Timeline
          Expanded(
            child: Container(
              color: Colors.grey[50],
              child: _isSearching
                  ? const Center(child: CircularProgressIndicator())
                  : (!_hasSearched
                      ? _buildEmptyState()
                      : _buildTrackingTimeline()),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.travel_explore, size: 80, color: Colors.grey[300]),
          const SizedBox(height: 16),
          Text(
            'Ingresa un número de guía arriba',
            style: TextStyle(color: Colors.grey[500], fontSize: 16),
          ),
        ],
      ),
    );
  }

  Widget _buildTrackingTimeline() {
    // Datos simulados (Se conectará a la BD real luego)
    final events = [
      {'title': 'Entregado', 'subtitle': 'El paquete ha sido entregado exitosamente.', 'time': 'Hoy, 10:30 AM', 'isPast': false, 'isCurrent': true, 'icon': Icons.check_circle},
      {'title': 'En Camino', 'subtitle': 'El repartidor está en ruta hacia tu destino.', 'time': 'Hoy, 08:15 AM', 'isPast': true, 'isCurrent': false, 'icon': Icons.local_shipping},
      {'title': 'En Bodega', 'subtitle': 'Paquete recibido en la bodega principal.', 'time': 'Ayer, 04:00 PM', 'isPast': true, 'isCurrent': false, 'icon': Icons.warehouse},
      {'title': 'Guía Creada', 'subtitle': 'La solicitud de envío ha sido generada.', 'time': 'Ayer, 09:00 AM', 'isPast': true, 'isCurrent': false, 'icon': Icons.receipt_long},
    ];

    return ListView(
      padding: const EdgeInsets.all(24),
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              'Guía: \${_searchController.text.toUpperCase()}',
              style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
              decoration: BoxDecoration(
                color: Colors.green.withValues(alpha: 0.1),
                borderRadius: BorderRadius.circular(20),
              ),
              child: const Text('Entregado', style: TextStyle(color: Colors.green, fontWeight: FontWeight.bold)),
            ),
          ],
        ),
        const SizedBox(height: 30),
        
        ...events.map((event) {
          int index = events.indexOf(event);
          bool isLast = index == events.length - 1;
          return _buildTimelineTile(
            title: event['title'] as String,
            subtitle: event['subtitle'] as String,
            time: event['time'] as String,
            icon: event['icon'] as IconData,
            isPast: event['isPast'] as bool,
            isCurrent: event['isCurrent'] as bool,
            isLast: isLast,
          );
        }),
      ],
    );
  }

  Widget _buildTimelineTile({
    required String title,
    required String subtitle,
    required String time,
    required IconData icon,
    required bool isPast,
    required bool isCurrent,
    required bool isLast,
  }) {
    Color iconColor = isCurrent ? Colors.green : (isPast ? const Color(0xFF0D47A1) : Colors.grey);
    Color lineColor = isPast ? const Color(0xFF0D47A1) : Colors.grey[300]!;

    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // Indicador y línea
        Column(
          children: [
            Container(
              width: 40,
              height: 40,
              decoration: BoxDecoration(
                color: iconColor.withValues(alpha: 0.1),
                shape: BoxShape.circle,
                border: Border.all(color: iconColor, width: 2),
              ),
              child: Icon(icon, color: iconColor, size: 20),
            ),
            if (!isLast)
              Container(
                width: 2,
                height: 60,
                color: lineColor,
              ),
          ],
        ),
        const SizedBox(width: 16),
        // Contenido
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(title, style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: isCurrent ? Colors.green : Colors.black87)),
              const SizedBox(height: 4),
              Text(subtitle, style: TextStyle(fontSize: 14, color: Colors.grey[600])),
              const SizedBox(height: 4),
              Text(time, style: TextStyle(fontSize: 12, color: Colors.grey[400], fontWeight: FontWeight.w500)),
              const SizedBox(height: 20),
            ],
          ),
        ),
      ],
    );
  }
}
