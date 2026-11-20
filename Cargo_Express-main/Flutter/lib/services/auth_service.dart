import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;

class AuthService extends ChangeNotifier {
  // Cuando el backend esté listo, cambiaremos esta URL a algo como:
  // final String baseUrl = 'http://10.0.2.2:8080/api/auth'; // Para el emulador Android
  final String baseUrl = 'http://localhost:8080/api/auth'; 

  String? _token;
  bool _isLoading = false;
  String? _error;

  String? get token => _token;
  bool get isLoading => _isLoading;
  String? get error => _error;
  bool get isAuthenticated => _token != null;

  Future<bool> login(String email, String password) async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      // Simulación de latencia de red
      await Future.delayed(const Duration(seconds: 2));

      // ---------------------------------------------------------
      // MODO SIMULADO (Quitar este bloque cuando el backend exista)
      if (email == 'demo@cargoexpress.com' && password == '123456') {
        _token = 'simulated_jwt_token_12345';
        _isLoading = false;
        notifyListeners();
        return true;
      } else {
        _error = 'Credenciales incorrectas';
        _isLoading = false;
        notifyListeners();
        return false;
      }
      // ---------------------------------------------------------

      /*
      // MODO REAL (Descomentar cuando el backend esté listo)
      final response = await http.post(
        Uri.parse('\$baseUrl/login'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          'email': email,
          'password': password,
        }),
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        _token = data['token']; // Asumiendo que el backend retorna { "token": "..." }
        
        // TODO: Guardar el token de forma segura (ej. con flutter_secure_storage)
        
        _isLoading = false;
        notifyListeners();
        return true;
      } else {
        _error = 'Error: \${response.statusCode} - \${response.body}';
        _isLoading = false;
        notifyListeners();
        return false;
      }
      */
    } catch (e) {
      _error = 'Error de conexión: \$e';
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }

  void logout() {
    _token = null;
    _error = null;
    notifyListeners();
  }
}
