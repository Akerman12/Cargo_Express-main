import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

class DashboardScreen extends StatelessWidget {
  const DashboardScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Dashboard'),
        actions: [
          IconButton(
            icon: const Icon(Icons.notifications_outlined),
            onPressed: () {},
          ),
          Padding(
            padding: const EdgeInsets.only(right: 16.0),
            child: CircleAvatar(
              backgroundColor: Theme.of(context).colorScheme.primary.withValues(alpha: 0.2),
              child: const Icon(Icons.person),
            ),
          ),
        ],
      ),
      drawer: _buildDrawer(context),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Bienvenido de nuevo,',
              style: TextStyle(fontSize: 16, color: Colors.grey),
            ),
            const SizedBox(height: 4),
            const Text(
              'Usuario Demo',
              style: TextStyle(fontSize: 28, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 32),
            
            // Stats Grid
            LayoutBuilder(
              builder: (context, constraints) {
                int crossAxisCount = constraints.maxWidth > 800 ? 4 : (constraints.maxWidth > 600 ? 3 : 2);
                return GridView.count(
                  crossAxisCount: crossAxisCount,
                  shrinkWrap: true,
                  physics: const NeverScrollableScrollPhysics(),
                  crossAxisSpacing: 16,
                  mainAxisSpacing: 16,
                  childAspectRatio: 1.5,
                  children: [
                    _buildStatCard(context, 'Envíos Activos', '12', Icons.local_shipping, Colors.blue),
                    _buildStatCard(context, 'Entregados', '145', Icons.check_circle, Colors.green),
                    _buildStatCard(context, 'Pendientes', '3', Icons.access_time, Colors.orange),
                    _buildStatCard(context, 'Pagos', '\$1,250', Icons.attach_money, Colors.purple),
                  ],
                );
              },
            ),
            
            const SizedBox(height: 40),
            const Text(
              'Acciones Rápidas',
              style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 16),
            
            // Quick Actions
            Wrap(
              spacing: 16,
              runSpacing: 16,
              children: [
                _buildActionChip(context, 'Crear Guía', Icons.add_box, () => context.push('/guides/create')),
                _buildActionChip(context, 'Rastrear', Icons.search, () => context.push('/tracking')),
                _buildActionChip(context, 'Mis Guías', Icons.list_alt, () => context.push('/guides')),
                _buildActionChip(context, 'Perfil', Icons.person_outline, () => context.push('/profile')),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildStatCard(BuildContext context, String title, String value, IconData icon, Color color) {
    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: color.withValues(alpha: 0.1),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Icon(icon, color: color, size: 28),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(
                  color: color.withValues(alpha: 0.1),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: const Text(
                  '+2.5%',
                  style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold),
                ),
              ),
            ],
          ),
          const Spacer(),
          Text(
            value,
            style: const TextStyle(fontSize: 28, fontWeight: FontWeight.bold),
          ),
          Text(
            title,
            style: const TextStyle(fontSize: 14, color: Colors.grey),
          ),
        ],
      ),
    );
  }

  Widget _buildActionChip(BuildContext context, String label, IconData icon, VoidCallback onTap) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(12),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
        decoration: BoxDecoration(
          color: Theme.of(context).colorScheme.surfaceContainerHighest,
          borderRadius: BorderRadius.circular(12),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 20),
            const SizedBox(width: 8),
            Text(label, style: const TextStyle(fontWeight: FontWeight.bold)),
          ],
        ),
      ),
    );
  }

  Widget _buildDrawer(BuildContext context) {
    return Drawer(
      child: Column(
        children: [
          UserAccountsDrawerHeader(
            accountName: const Text('Usuario Demo'),
            accountEmail: const Text('usuario@cargoexpress.com'),
            currentAccountPicture: const CircleAvatar(
              backgroundColor: Colors.white,
              child: Icon(Icons.person, size: 40, color: Color(0xFF0D47A1)),
            ),
            decoration: const BoxDecoration(
              color: Color(0xFF0D47A1),
            ),
          ),
          ListTile(
            leading: const Icon(Icons.dashboard),
            title: const Text('Dashboard'),
            onTap: () => context.pop(),
          ),
          ListTile(
            leading: const Icon(Icons.local_shipping),
            title: const Text('Mis Guías'),
            onTap: () {
              context.pop();
              context.push('/guides');
            },
          ),
          ListTile(
            leading: const Icon(Icons.map),
            title: const Text('Reparto'),
            onTap: () {
              context.pop();
              context.push('/delivery/map');
            },
          ),
          const Divider(),
          ListTile(
            leading: const Icon(Icons.settings),
            title: const Text('Configuración'),
            onTap: () {},
          ),
          ListTile(
            leading: const Icon(Icons.exit_to_app, color: Colors.red),
            title: const Text('Cerrar Sesión', style: TextStyle(color: Colors.red)),
            onTap: () {
              context.pop();
              context.go('/login');
            },
          ),
        ],
      ),
    );
  }
}
