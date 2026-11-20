import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'core/router/app_router.dart';
import 'services/auth_service.dart';

void main() {
  runApp(
    MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => AuthService()),
      ],
      child: const CargoExpressApp(),
    ),
  );
}

class CargoExpressApp extends StatelessWidget {
  const CargoExpressApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp.router(
      title: 'CargoExpress',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF0D47A1), // Azul corporativo
          brightness: Brightness.light,
        ),
        useMaterial3: true,
        appBarTheme: const AppBarTheme(
          centerTitle: true,
          elevation: 0,
        ),
      ),
      routerConfig: appRouter,
    );
  }
}
