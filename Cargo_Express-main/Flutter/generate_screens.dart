import 'dart:io';

void main() {
  final screens = {
    'auth': [
      'login_screen',
      'register_screen',
      'forgot_password_screen',
      'reset_password_screen'
    ],
    'dashboard': ['dashboard_screen'],
    'profile': [
      'view_profile_screen',
      'edit_profile_screen',
      'my_addresses_screen'
    ],
    'guides': [
      'list_guides_screen',
      'create_guide_screen',
      'edit_guide_screen',
    ],
    'tracking': ['tracking_screen'],
    'payment': ['payment_screen'],
    'delivery': ['delivery_list_screen', 'delivery_map_screen'],
    'admin': [
      'admin_users_screen',
      'admin_edit_user_screen',
      'admin_payments_screen',
      'admin_warroom_screen'
    ],
    'static': ['about_us_screen', 'support_screen']
  };

  final basePath = 'lib/views';

  screens.forEach((folder, files) {
    final dir = Directory('$basePath/$folder');
    if (!dir.existsSync()) {
      dir.createSync(recursive: true);
    }

    for (var file in files) {
      final f = File('${dir.path}/$file.dart');
      
      // Convert snake_case to CamelCase for class name
      final className = file.split('_').map((word) => word[0].toUpperCase() + word.substring(1)).join('');
      
      final content = '''
import 'package:flutter/material.dart';

class $className extends StatelessWidget {
  const $className({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('$className'),
      ),
      body: const Center(
        child: Text('Vista de $className en construcción'),
      ),
    );
  }
}
''';
      f.writeAsStringSync(content);
      print('Created $file.dart');
    }
  });
}
