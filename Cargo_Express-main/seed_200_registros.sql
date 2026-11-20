-- ============================================================================
-- Script de carga masiva compatible con TODAS las versiones de MariaDB / MySQL
-- Base de datos: sananredsb
-- Genera 30 vehículos + 180 guías = 210 registros entre los usuarios existentes
--
-- Cómo ejecutarlo en tu terminal Linux:
--   mariadb -u root sananredsb < seed_200_registros.sql
-- (Si tu distro usa socket root):
--   sudo mariadb sananredsb < seed_200_registros.sql
-- ============================================================================

SET NAMES utf8mb4;
USE sananredsb;

-- Mostrar los usuarios actuales para confirmar sus IDs y roles
SELECT id, nombre, username, email, rol, region_operacion, aprobado FROM usuario ORDER BY id ASC;

SET @u1 := (SELECT id FROM usuario WHERE username = 'jgarciad06@gmail.com' OR email = 'jgarciad06@gmail.com' LIMIT 1);
SET @u2 := (SELECT id FROM usuario WHERE username = 'juliorafaelgarciadiaz15@gmail.com' OR email = 'juliorafaelgarciadiaz15@gmail.com' LIMIT 1);
SET @u1 := COALESCE(@u1, (SELECT id FROM usuario ORDER BY id ASC LIMIT 1));
SET @u2 := COALESCE(@u2, (SELECT id FROM usuario ORDER BY id ASC LIMIT 1, 1), @u1);
SET @rep_id := (SELECT id FROM usuario WHERE rol = 'ROLE_REPARTIDOR' AND aprobado = 1 ORDER BY id ASC LIMIT 1);
SET @rep_tiene_vehiculo := IF(@rep_id IS NULL, 1, (SELECT COUNT(*) FROM vehiculo WHERE repartidor_id = @rep_id));

SELECT IF(@u1 IS NULL,
    '⚠️ NO HAY USUARIOS en la tabla usuario. Arranca primero la app Spring Boot (./mvnw spring-boot:run) para que cree los usuarios.',
    CONCAT('✅ Cargando 30 vehículos y 180 guías para u1=', @u1, ' y u2=', @u2, IF(@rep_id IS NOT NULL, CONCAT(' (repartidor=', @rep_id, ')'), ''))
) AS estado_usuarios;

-- 1) Insertar 30 vehículos (placas únicas SAI-101 a SAI-130)
INSERT IGNORE INTO vehiculo (
    placa,
    modelo,
    tipo,
    capacidad_peso_kg,
    capacidad_volumen_m3,
    estado,
    repartidor_id,
    fecha_registro
)
SELECT
    CONCAT('SAI-', LPAD(100 + seq.n, 3, '0')) AS placa,
    ELT(1 + MOD(seq.n, 6),
        'Renault Master Cargo',
        'Chevrolet NHR Reward',
        'Hyundai H350 Van',
        'Mercedes-Benz Sprinter',
        'Ford Transit L3H2',
        'Kia Bongo K2500'
    ) AS modelo,
    ELT(1 + MOD(seq.n, 4), 'FURGONETA', 'FURGONETA', 'CAMION', 'UTILITARIO') AS tipo,
    CASE MOD(seq.n, 4)
        WHEN 0 THEN 1800.0
        WHEN 1 THEN 1200.0
        WHEN 2 THEN 2500.0
        ELSE 900.0
    END AS capacidad_peso_kg,
    CASE MOD(seq.n, 4)
        WHEN 0 THEN 12.0
        WHEN 1 THEN 8.5
        WHEN 2 THEN 16.0
        ELSE 5.5
    END AS capacidad_volumen_m3,
    CASE
        WHEN seq.n = 1 AND @rep_id IS NOT NULL AND @rep_tiene_vehiculo = 0 THEN 'ASIGNADO'
        WHEN MOD(seq.n, 7) = 0 THEN 'MANTENIMIENTO'
        ELSE 'DISPONIBLE'
    END AS estado,
    CASE
        WHEN seq.n = 1 AND @rep_id IS NOT NULL AND @rep_tiene_vehiculo = 0 THEN @rep_id
        ELSE NULL
    END AS repartidor_id,
    DATE_SUB(NOW(), INTERVAL (seq.n * 2) DAY) AS fecha_registro
FROM (
    SELECT (t.d * 10 + u.d + 1) AS n
    FROM (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2) t
    CROSS JOIN (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
                UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) u
) AS seq;

-- 2) Insertar 180 guías repartidas entre @u1 y @u2
--    Las primeras 8 (de Región Andina y peso ligero) también quedan asignadas a @rep_id si existe repartidor
INSERT INTO guia (
    remitente,
    destinatario,
    direccion_envio,
    direccion_recibo,
    cedula_remitente,
    cedula_destinatario,
    ciudad_origen,
    ciudad_destino,
    departamento_origen,
    departamento_destino,
    lat_origen,
    lon_origen,
    lat_destino,
    lon_destino,
    distancia_km,
    tiempo_estimado_dias,
    tipo_carga,
    peso_kg,
    altura_cm,
    ancho_cm,
    largo_cm,
    valor_declarado,
    precio,
    fecha_creacion,
    fecha_entrega,
    estado,
    estado_pago,
    modificado_por,
    usuario_id,
    repartidor_id,
    repartidor_recogida_id,
    repartidor_entrega_id
)
SELECT
    ELT(1 + MOD(seq.n, 10),
        'Carlos Andres Perez', 'Maria Fernanda Gomez', 'Juan Camilo Rodriguez', 'Laura Sofia Martinez', 'Andres Felipe Lopez',
        'Valentina Ruiz Diaz', 'Santiago Herrera', 'Camila Andrea Torres', 'Diego Alejandro Castro', 'Paula Andrea Vargas'
    ) AS remitente,
    ELT(1 + MOD(seq.n + 4, 10),
        'Sebastian Morales', 'Natalia Ramirez', 'Mateo Ospina', 'Daniela Restrepo', 'Julian Esteban Rojas',
        'Isabella Mendoza', 'Felipe Navarro', 'Mariana Jimenez', 'Tomas Silva', 'Valeria Cardenas'
    ) AS destinatario,
    CONCAT('Calle ', 10 + MOD(seq.n * 3, 90), ' # ', 5 + MOD(seq.n, 45), '-', 10 + MOD(seq.n * 7, 80)) AS direccion_envio,
    CONCAT('Carrera ', 7 + MOD(seq.n * 5, 80), ' # ', 12 + MOD(seq.n, 50), '-', 15 + MOD(seq.n * 3, 75)) AS direccion_recibo,
    CAST(1010000000 + seq.n * 137 AS CHAR) AS cedula_remitente,
    CAST(1090000000 + seq.n * 251 AS CHAR) AS cedula_destinatario,

    CASE WHEN seq.n <= 8 THEN ELT(1 + MOD(seq.n, 2), 'Bogotá', 'Medellín')
         ELSE ELT(1 + MOD(seq.n, 10),
            'Bogotá', 'Medellín', 'Cali', 'Barranquilla', 'Cartagena',
            'Bucaramanga', 'Pereira', 'Santa Marta', 'Villavicencio', 'Manizales')
    END AS ciudad_origen,
    CASE WHEN seq.n <= 8 THEN ELT(1 + MOD(seq.n + 1, 2), 'Bogotá', 'Medellín')
         ELSE ELT(1 + MOD(seq.n + 3, 10),
            'Bogotá', 'Medellín', 'Cali', 'Barranquilla', 'Cartagena',
            'Bucaramanga', 'Pereira', 'Santa Marta', 'Villavicencio', 'Manizales')
    END AS ciudad_destino,
    CASE WHEN seq.n <= 8 THEN ELT(1 + MOD(seq.n, 2), 'Cundinamarca', 'Antioquia')
         ELSE ELT(1 + MOD(seq.n, 10),
            'Cundinamarca', 'Antioquia', 'Valle del Cauca', 'Atlántico', 'Bolívar',
            'Santander', 'Risaralda', 'Magdalena', 'Meta', 'Caldas')
    END AS departamento_origen,
    CASE WHEN seq.n <= 8 THEN ELT(1 + MOD(seq.n + 1, 2), 'Cundinamarca', 'Antioquia')
         ELSE ELT(1 + MOD(seq.n + 3, 10),
            'Cundinamarca', 'Antioquia', 'Valle del Cauca', 'Atlántico', 'Bolívar',
            'Santander', 'Risaralda', 'Magdalena', 'Meta', 'Caldas')
    END AS departamento_destino,

    CAST(ELT(1 + MOD(seq.n, 10), 4.6097, 6.2442, 3.4516, 10.9639, 10.3910, 7.1193, 4.8133, 11.2408, 4.1420, 5.0689) AS DOUBLE)
        + (MOD(seq.n, 7) - 3) * 0.008 AS lat_origen,
    CAST(ELT(1 + MOD(seq.n, 10), -74.0817, -75.5812, -76.5320, -74.7964, -75.4794, -73.1227, -75.6961, -74.1990, -73.6266, -75.5174) AS DOUBLE)
        + (MOD(seq.n, 5) - 2) * 0.008 AS lon_origen,
    CAST(ELT(1 + MOD(seq.n + 3, 10), 4.6097, 6.2442, 3.4516, 10.9639, 10.3910, 7.1193, 4.8133, 11.2408, 4.1420, 5.0689) AS DOUBLE)
        + (MOD(seq.n, 5) - 2) * 0.008 AS lat_destino,
    CAST(ELT(1 + MOD(seq.n + 3, 10), -74.0817, -75.5812, -76.5320, -74.7964, -75.4794, -73.1227, -75.6961, -74.1990, -73.6266, -75.5174) AS DOUBLE)
        + (MOD(seq.n, 7) - 3) * 0.008 AS lon_destino,

    ROUND(120 + MOD(seq.n * 37, 780), 1) AS distancia_km,
    ROUND(1.0 + MOD(seq.n, 5), 1) AS tiempo_estimado_dias,
    ELT(1 + MOD(seq.n, 4), 'Paquete Estándar', 'Caja Grande', 'Sobre / Documento', 'Carga Frágil') AS tipo_carga,
    CASE WHEN seq.n <= 8 THEN 3.0 ELSE ROUND(1.5 + MOD(seq.n * 3, 35) * 0.5, 1) END AS peso_kg,
    20 + MOD(seq.n * 5, 60) AS altura_cm,
    20 + MOD(seq.n * 3, 50) AS ancho_cm,
    25 + MOD(seq.n * 4, 70) AS largo_cm,
    50000 + MOD(seq.n, 20) * 25000 AS valor_declarado,
    18000 + MOD(seq.n * 4500, 120000) AS precio,
    DATE_SUB(NOW(), INTERVAL MOD(seq.n, 90) DAY) AS fecha_creacion,
    CASE WHEN seq.n > 8 AND MOD(seq.n, 7) = 0 THEN DATE_SUB(NOW(), INTERVAL MOD(seq.n, 30) DAY) ELSE NULL END AS fecha_entrega,
    CASE
        WHEN seq.n <= 4 THEN 'Pendiente'
        WHEN seq.n <= 8 THEN 'En Sucursal'
        ELSE ELT(1 + MOD(seq.n, 7), 'Entregado', 'Pendiente', 'Recogido', 'En Sucursal', 'En Tránsito', 'No Recogido', 'Novedad')
    END AS estado,
    CASE WHEN MOD(seq.n, 3) = 0 THEN 'PENDIENTE' ELSE 'PAGADO' END AS estado_pago,
    'CARGA_MASIVA_SQL' AS modificado_por,
    CASE WHEN MOD(seq.n, 2) = 0 THEN @u1 ELSE @u2 END AS usuario_id,
    CASE WHEN seq.n <= 8 AND @rep_id IS NOT NULL THEN @rep_id ELSE NULL END AS repartidor_id,
    CASE WHEN seq.n <= 4 AND @rep_id IS NOT NULL THEN @rep_id ELSE NULL END AS repartidor_recogida_id,
    CASE WHEN seq.n BETWEEN 5 AND 8 AND @rep_id IS NOT NULL THEN @rep_id ELSE NULL END AS repartidor_entrega_id
FROM (
    SELECT (h.d * 100 + t.d * 10 + u.d + 1) AS n
    FROM (SELECT 0 d UNION ALL SELECT 1) h
    CROSS JOIN (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
                UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t
    CROSS JOIN (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
                UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) u
    WHERE (h.d * 100 + t.d * 10 + u.d + 1) <= 180
) AS seq
WHERE @u1 IS NOT NULL;

-- Verificación final
SELECT 'vehiculo' AS tabla, COUNT(*) AS total FROM vehiculo
UNION ALL
SELECT 'guia' AS tabla, COUNT(*) AS total FROM guia;

SELECT u.username, COUNT(g.id) AS guias_del_usuario
FROM usuario u
LEFT JOIN guia g ON g.usuario_id = u.id
GROUP BY u.id, u.username
ORDER BY u.id ASC;
