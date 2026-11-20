import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import '../../services/auth_service.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  bool _isObscure = true;

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _handleLogin() async {
    final email = _emailController.text.trim();
    final password = _passwordController.text.trim();

    if (email.isEmpty || password.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Por favor, ingresa tus credenciales')),
      );
      return;
    }

    final authService = Provider.of<AuthService>(context, listen: false);
    final success = await authService.login(email, password);

    if (success) {
      if (mounted) context.go('/dashboard');
    } else {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(authService.error ?? 'Error desconocido'),
            backgroundColor: Colors.red,
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final size = MediaQuery.of(context).size;

    return Scaffold(
      backgroundColor: Colors.white,
      body: Row(
        children: [
          // Sección de imagen (visible en pantallas grandes)
          if (size.width > 800)
            Expanded(
              child: Container(
                decoration: const BoxDecoration(
                  gradient: LinearGradient(
                    colors: [Color(0xFF0D47A1), Color(0xFF1976D2)],
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                  ),
                ),
                child: const Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(Icons.local_shipping, size: 100, color: Colors.white),
                      SizedBox(height: 20),
                      Text(
                        'CargoExpress',
                        style: TextStyle(
                          color: Colors.white,
                          fontSize: 48,
                          fontWeight: FontWeight.bold,
                          letterSpacing: 1.2,
                        ),
                      ),
                      SizedBox(height: 10),
                      Text(
                        'Tu logística, en buenas manos.',
                        style: TextStyle(
                          color: Colors.white70,
                          fontSize: 18,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),

          // Sección del formulario
          Expanded(
            child: Center(
              child: SingleChildScrollView(
                padding: const EdgeInsets.symmetric(horizontal: 40.0),
                child: ConstrainedBox(
                  constraints: const BoxConstraints(maxWidth: 400),
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      if (size.width <= 800) ...[
                        const Icon(Icons.local_shipping, size: 60, color: Color(0xFF0D47A1)),
                        const SizedBox(height: 10),
                        const Text(
                          'CargoExpress',
                          textAlign: TextAlign.center,
                          style: TextStyle(
                            fontSize: 32,
                            fontWeight: FontWeight.bold,
                            color: Color(0xFF0D47A1),
                          ),
                        ),
                        const SizedBox(height: 30),
                      ],
                      const Text(
                        'Iniciar Sesión',
                        style: TextStyle(
                          fontSize: 28,
                          fontWeight: FontWeight.bold,
                          color: Colors.black87,
                        ),
                      ),
                      const SizedBox(height: 8),
                      const Text(
                        'Ingresa tus credenciales para acceder a tu cuenta.',
                        style: TextStyle(color: Colors.grey),
                      ),
                      const SizedBox(height: 40),
                      
                      // Campo de Correo
                      TextField(
                        controller: _emailController,
                        decoration: InputDecoration(
                          labelText: 'Correo Electrónico',
                          prefixIcon: const Icon(Icons.email_outlined),
                          border: OutlineInputBorder(
                            borderRadius: BorderRadius.circular(12),
                          ),
                          filled: true,
                          fillColor: Colors.grey[50],
                        ),
                      ),
                      const SizedBox(height: 20),
                      
                      // Campo de Contraseña
                      TextField(
                        controller: _passwordController,
                        obscureText: _isObscure,
                        decoration: InputDecoration(
                          labelText: 'Contraseña',
                          prefixIcon: const Icon(Icons.lock_outline),
                          suffixIcon: IconButton(
                            icon: Icon(
                              _isObscure ? Icons.visibility_off : Icons.visibility,
                            ),
                            onPressed: () {
                              setState(() {
                                _isObscure = !_isObscure;
                              });
                            },
                          ),
                          border: OutlineInputBorder(
                            borderRadius: BorderRadius.circular(12),
                          ),
                          filled: true,
                          fillColor: Colors.grey[50],
                        ),
                      ),
                      const SizedBox(height: 10),
                      
                      // Olvidé mi contraseña
                      Align(
                        alignment: Alignment.centerRight,
                        child: TextButton(
                          onPressed: () => context.push('/forgot-password'),
                          child: const Text('¿Olvidaste tu contraseña?'),
                        ),
                      ),
                      const SizedBox(height: 30),
                      
                      // Botón de Entrar
                      Consumer<AuthService>(
                        builder: (context, auth, _) {
                          return ElevatedButton(
                            onPressed: auth.isLoading ? null : _handleLogin,
                            style: ElevatedButton.styleFrom(
                              backgroundColor: const Color(0xFF0D47A1),
                              foregroundColor: Colors.white,
                              padding: const EdgeInsets.symmetric(vertical: 16),
                              shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(12),
                              ),
                              elevation: 2,
                            ),
                            child: auth.isLoading
                                ? const SizedBox(
                                    height: 24,
                                    width: 24,
                                    child: CircularProgressIndicator(
                                      color: Colors.white,
                                      strokeWidth: 2,
                                    ),
                                  )
                                : const Text(
                                    'Ingresar',
                                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                                  ),
                          );
                        },
                      ),
                      const SizedBox(height: 20),
                      
                      // Separador
                      Row(
                        children: [
                          Expanded(child: Divider(color: Colors.grey[300])),
                          const Padding(
                            padding: EdgeInsets.symmetric(horizontal: 16),
                            child: Text('O continúa con', style: TextStyle(color: Colors.grey)),
                          ),
                          Expanded(child: Divider(color: Colors.grey[300])),
                        ],
                      ),
                      const SizedBox(height: 20),
                      
                      // Botón de Google
                      OutlinedButton.icon(
                        onPressed: () {
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(content: Text('Inicio con Google próximamente')),
                          );
                        },
                        icon: Image.network(
                          'https://upload.wikimedia.org/wikipedia/commons/c/c1/Google_%22G%22_logo.svg',
                          height: 24,
                        ),
                        label: const Text(
                          'Google',
                          style: TextStyle(color: Colors.black87, fontSize: 16, fontWeight: FontWeight.bold),
                        ),
                        style: OutlinedButton.styleFrom(
                          padding: const EdgeInsets.symmetric(vertical: 16),
                          side: BorderSide(color: Colors.grey[300]!),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(12),
                          ),
                        ),
                      ),
                      const SizedBox(height: 30),
                      
                      // Registro
                      Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          const Text('¿No tienes una cuenta?'),
                          TextButton(
                            onPressed: () => context.push('/register'),
                            child: const Text('Regístrate'),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
