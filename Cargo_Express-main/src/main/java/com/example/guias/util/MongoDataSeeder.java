package com.example.guias.util;

import com.example.guias.model.mongo.MongoGuia;
import com.example.guias.model.mongo.MongoUsuario;
import com.example.guias.repository.mongo.MongoGuiaRepository;
import com.example.guias.repository.mongo.MongoUsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.Arrays;

@Component
public class MongoDataSeeder implements CommandLineRunner {

    private final MongoUsuarioRepository usuarioRepository;
    private final MongoGuiaRepository guiaRepository;
    private final PasswordEncoder passwordEncoder;

    public MongoDataSeeder(MongoUsuarioRepository usuarioRepository, MongoGuiaRepository guiaRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.guiaRepository = guiaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Verificar si ya existen usuarios para no duplicar datos
        if (usuarioRepository.count() == 0) {
            System.out.println("No se encontraron usuarios en MongoDB. Creando datos quemados...");
            
            // Crear usuario Admin
            MongoUsuario admin = new MongoUsuario();
            admin.setNombre("Administrador Inicial");
            admin.setUsername("admin_mongo");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRol("ROLE_ADMIN");
            admin.setEmail("admin@mongo.com");
            admin.setAprobado(true);
            
            // Crear usuario Cliente
            MongoUsuario cliente = new MongoUsuario();
            cliente.setNombre("Cliente Prueba");
            cliente.setUsername("cliente_mongo");
            cliente.setPassword(passwordEncoder.encode("cliente123"));
            cliente.setRol("ROLE_CLIENTE");
            cliente.setEmail("cliente@mongo.com");
            cliente.setAprobado(true);
            
            usuarioRepository.saveAll(Arrays.asList(admin, cliente));
            System.out.println("Usuarios quemados creados en MongoDB.");

            // Crear Guías quemadas
            if (guiaRepository.count() == 0) {
                System.out.println("No se encontraron guías en MongoDB. Creando guías quemadas...");
                
                MongoGuia guia1 = new MongoGuia();
                guia1.setRemitente("Cliente Prueba");
                guia1.setDestinatario("Destinatario Uno");
                guia1.setDireccionEnvio("Calle Falsa 123");
                guia1.setDireccionRecibo("Avenida Siempre Viva 742");
                guia1.setCedulaRemitente("11111111");
                guia1.setCedulaDestinatario("22222222");
                guia1.setCiudadOrigen("Bogotá");
                guia1.setCiudadDestino("Medellín");
                guia1.setDepartamentoOrigen("Cundinamarca");
                guia1.setDepartamentoDestino("Antioquia");
                guia1.setTipoCarga("Caja pequeña");
                guia1.setPesoKg(2.5);
                guia1.setAlturaCm(10);
                guia1.setAnchoCm(20);
                guia1.setValorDeclarado(50000);
                guia1.setPrecio(15000);
                guia1.setFechaCreacion(LocalDateTime.now());
                guia1.setEstado("PENDIENTE");
                guia1.setEstadoPago("PENDIENTE");
                
                MongoGuia guia2 = new MongoGuia();
                guia2.setRemitente("Empresa XYZ");
                guia2.setDestinatario("Cliente Prueba");
                guia2.setDireccionEnvio("Zona Industrial Bodega 5");
                guia2.setDireccionRecibo("Calle Falsa 123");
                guia2.setCedulaRemitente("900000000");
                guia2.setCedulaDestinatario("11111111");
                guia2.setCiudadOrigen("Cali");
                guia2.setCiudadDestino("Bogotá");
                guia2.setDepartamentoOrigen("Valle del Cauca");
                guia2.setDepartamentoDestino("Cundinamarca");
                guia2.setTipoCarga("Documentos");
                guia2.setPesoKg(0.5);
                guia2.setAlturaCm(2);
                guia2.setAnchoCm(30);
                guia2.setValorDeclarado(10000);
                guia2.setPrecio(8000);
                guia2.setFechaCreacion(LocalDateTime.now().minusDays(2));
                guia2.setEstado("EN_TRANSITO");
                guia2.setEstadoPago("PAGADO");

                guiaRepository.saveAll(Arrays.asList(guia1, guia2));
                System.out.println("Guías quemadas creadas en MongoDB.");
            }
        } else {
            System.out.println("La base de datos MongoDB ya contiene datos, omitiendo inicialización de datos quemados.");
        }
    }
}
