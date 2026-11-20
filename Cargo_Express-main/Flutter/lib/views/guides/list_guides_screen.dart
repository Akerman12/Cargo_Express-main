import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

class ListGuidesScreen extends StatelessWidget {
  const ListGuidesScreen({super.key});

  @override
  Widget build(BuildContext context) {
    // Simulación de datos
    final guias = [
      {
        'id': 'GUIA-2026-1029',
        'fecha': '20 May 2026',
        'estado': 'En Camino',
        'origen': 'Ciudad de México, CDMX',
        'destino': 'Guadalajara, JAL',
        'monto': '\$450.00'
      },
      {
        'id': 'GUIA-2026-1028',
        'fecha': '18 May 2026',
        'estado': 'Entregado',
        'origen': 'Monterrey, NL',
        'destino': 'Saltillo, COAH',
        'monto': '\$220.00'
      },
      {
        'id': 'GUIA-2026-1027',
        'fecha': '15 May 2026',
        'estado': 'Pendiente',
        'origen': 'Querétaro, QRO',
        'destino': 'Puebla, PUE',
        'monto': '\$340.00'
      },
    ];

    return Scaffold(
      backgroundColor: Colors.grey[50],
      appBar: AppBar(
        title: const Text('Mis Guías', style: TextStyle(fontWeight: FontWeight.bold)),
        backgroundColor: const Color(0xFF0D47A1),
        foregroundColor: Colors.white,
        elevation: 0,
        actions: [
          IconButton(
            icon: const Icon(Icons.filter_list),
            onPressed: () {
              // Filtrar
            },
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => context.push('/guides/create'),
        backgroundColor: const Color(0xFF0D47A1),
        icon: const Icon(Icons.add, color: Colors.white),
        label: const Text('Nueva Guía', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
      ),
      body: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: guias.length,
        itemBuilder: (context, index) {
          final guia = guias[index];
          return _buildGuideCard(context, guia);
        },
      ),
    );
  }

  Widget _buildGuideCard(BuildContext context, Map<String, String> guia) {
    Color estadoColor;
    switch (guia['estado']) {
      case 'Entregado':
        estadoColor = Colors.green;
        break;
      case 'En Camino':
        estadoColor = const Color(0xFF0D47A1);
        break;
      default:
        estadoColor = Colors.orange;
    }

    return Card(
      elevation: 2,
      margin: const EdgeInsets.only(bottom: 16),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: InkWell(
        onTap: () {
          // Abrir detalles de la guía o editar
          // context.push('/guides/edit/\${guia['id']}');
        },
        borderRadius: BorderRadius.circular(16),
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Encabezado
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    guia['id']!,
                    style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: estadoColor.withValues(alpha: 0.1),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Text(
                      guia['estado']!,
                      style: TextStyle(color: estadoColor, fontWeight: FontWeight.bold, fontSize: 12),
                    ),
                  ),
                ],
              ),
              const Divider(height: 24),
              
              // Ruta
              Row(
                children: [
                  Column(
                    children: [
                      const Icon(Icons.trip_origin, size: 16, color: Colors.grey),
                      const SizedBox(height: 2),
                      Container(height: 20, width: 2, color: Colors.grey),
                      const SizedBox(height: 2),
                      const Icon(Icons.location_on, size: 16, color: Color(0xFF0D47A1)),
                    ],
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(guia['origen']!, style: const TextStyle(fontSize: 14)),
                        const SizedBox(height: 12),
                        Text(guia['destino']!, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.bold)),
                      ],
                    ),
                  ),
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: [
                      const Text('Total', style: TextStyle(color: Colors.grey, fontSize: 12)),
                      Text(
                        guia['monto']!,
                        style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: Color(0xFF0D47A1)),
                      ),
                    ],
                  ),
                ],
              ),
              const SizedBox(height: 16),
              
              // Footer
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    'Fecha: \${guia["fecha"]}',
                    style: TextStyle(color: Colors.grey[600], fontSize: 13),
                  ),
                  TextButton(
                    onPressed: () {
                      // Ver detalles / Rastreo directo
                      context.push('/tracking');
                    },
                    child: const Text('Ver Rastreo'),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
