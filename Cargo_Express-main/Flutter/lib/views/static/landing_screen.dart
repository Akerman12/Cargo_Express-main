import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

class LandingScreen extends StatelessWidget {
  const LandingScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final size = MediaQuery.of(context).size;

    return Scaffold(
      body: Container(
        width: double.infinity,
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            colors: [Color(0xFF0D47A1), Color(0xFF1976D2), Color(0xFF42A5F5)],
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
          ),
        ),
        child: SafeArea(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              // Header con logo
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 24.0, vertical: 40.0),
                child: Column(
                  children: [
                    Container(
                      padding: const EdgeInsets.all(20),
                      decoration: BoxDecoration(
                        color: Colors.white.withValues(alpha: 0.1),
                        shape: BoxShape.circle,
                      ),
                      child: const Icon(
                        Icons.local_shipping,
                        size: 80,
                        color: Colors.white,
                      ),
                    ),
                    const SizedBox(height: 24),
                    const Text(
                      'CargoExpress',
                      style: TextStyle(
                        fontSize: 42,
                        fontWeight: FontWeight.w900,
                        color: Colors.white,
                        letterSpacing: 1.5,
                      ),
                    ),
                    const SizedBox(height: 12),
                    Text(
                      'Tu logística, en buenas manos.',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontSize: 18,
                        color: Colors.white.withValues(alpha: 0.9),
                        fontWeight: FontWeight.w400,
                      ),
                    ),
                  ],
                ),
              ),

              // Botones de acción principales
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(32.0),
                decoration: const BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.only(
                    topLeft: Radius.circular(40),
                    topRight: Radius.circular(40),
                  ),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    const Text(
                      'Bienvenido',
                      style: TextStyle(
                        fontSize: 28,
                        fontWeight: FontWeight.bold,
                        color: Color(0xFF0D47A1),
                      ),
                      textAlign: TextAlign.center,
                    ),
                    const SizedBox(height: 10),
                    const Text(
                      'Ingresa para gestionar tus envíos o rastrea un paquete al instante.',
                      style: TextStyle(color: Colors.grey, fontSize: 16),
                      textAlign: TextAlign.center,
                    ),
                    const SizedBox(height: 40),

                    // Botón Iniciar Sesión
                    ElevatedButton(
                      onPressed: () => context.push('/login'),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: const Color(0xFF0D47A1),
                        foregroundColor: Colors.white,
                        padding: const EdgeInsets.symmetric(vertical: 18),
                        elevation: 4,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(16),
                        ),
                      ),
                      child: const Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(Icons.login),
                          SizedBox(width: 10),
                          Text(
                            'Iniciar Sesión',
                            style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 16),

                    // Botón Rastrear Envío (Acceso rápido)
                    OutlinedButton(
                      onPressed: () {
                        // Idealmente llevaría directo al tracking público
                        context.push('/login'); 
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(content: Text('Inicia sesión para rastrear')),
                        );
                      },
                      style: OutlinedButton.styleFrom(
                        foregroundColor: const Color(0xFF0D47A1),
                        side: const BorderSide(color: Color(0xFF0D47A1), width: 2),
                        padding: const EdgeInsets.symmetric(vertical: 18),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(16),
                        ),
                      ),
                      child: const Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(Icons.search),
                          SizedBox(width: 10),
                          Text(
                            'Rastrear Envío',
                            style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 24),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
