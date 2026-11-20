package com.example.guias.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import weka.classifiers.trees.REPTree;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import jakarta.annotation.PostConstruct;
import java.io.InputStreamReader;

@Service
public class WekaService {

    // Modelo 1: Predicción de Tiempo (Regresión REPTree)
    private REPTree modeloTiempo;
    private Instances estructuraTiempo;

    // Modelo 2: Predicción de Empaque (Clasificación J48)
    private weka.classifiers.trees.J48 modeloEmpaque;
    private Instances estructuraEmpaque;

    // Modelo 3: Predicción de CO2 (Regresión Lineal)
    private weka.classifiers.functions.LinearRegression modeloCO2;
    private Instances estructuraCO2;

    @PostConstruct
    public void inicializarModelos() {
        // Inicializar Modelo de Tiempo
        try {
            ClassPathResource recursoTiempo = new ClassPathResource("datos_envios.arff");
            estructuraTiempo = new Instances(new InputStreamReader(recursoTiempo.getInputStream()));
            estructuraTiempo.setClassIndex(estructuraTiempo.numAttributes() - 1);
            modeloTiempo = new REPTree();
            modeloTiempo.buildClassifier(estructuraTiempo);
            System.out.println("🤖 Modelo de Tiempo Weka entrenado.");
        } catch (Exception e) {
            System.err.println("Error entrenando modelo tiempo: " + e.getMessage());
        }

        // Inicializar Modelo de Empaque
        try {
            ClassPathResource recursoEmpaque = new ClassPathResource("datos_empaque.arff");
            estructuraEmpaque = new Instances(new InputStreamReader(recursoEmpaque.getInputStream()));
            estructuraEmpaque.setClassIndex(estructuraEmpaque.numAttributes() - 1);
            modeloEmpaque = new weka.classifiers.trees.J48();
            modeloEmpaque.buildClassifier(estructuraEmpaque);
            System.out.println("📦 Modelo de Empaque Weka entrenado.");
        } catch (Exception e) {
            System.err.println("Error entrenando modelo empaque: " + e.getMessage());
        }

        // Inicializar Modelo de CO2 (Regresión Lineal)
        try {
            ClassPathResource recursoCO2 = new ClassPathResource("datos_co2.arff");
            estructuraCO2 = new Instances(new InputStreamReader(recursoCO2.getInputStream()));
            estructuraCO2.setClassIndex(estructuraCO2.numAttributes() - 1);
            modeloCO2 = new weka.classifiers.functions.LinearRegression();
            modeloCO2.buildClassifier(estructuraCO2);
            System.out.println("🍃 Modelo de CO2 (Regresión Lineal) Weka entrenado.");
        } catch (Exception e) {
            System.err.println("Error entrenando modelo CO2: " + e.getMessage());
        }
    }

    public double predecirTiempo(String origen, String destino, String tipoCarga) {
        try {
            Instance inst = new DenseInstance(estructuraTiempo.numAttributes());
            inst.setDataset(estructuraTiempo);
            inst.setValue(estructuraTiempo.attribute("origen"), origen);
            inst.setValue(estructuraTiempo.attribute("destino"), destino);
            inst.setValue(estructuraTiempo.attribute("tipo_carga"), tipoCarga);
            double prediccion = modeloTiempo.classifyInstance(inst);
            return Math.round(prediccion * 10.0) / 10.0;
        } catch (Exception e) {
            return 3.0;
        }
    }

    public double predecirTiempoConHeuristica(String origen, String destino, String tipoCarga, String clima, String servicio) {
        // Obtenemos la predicción base del modelo Weka
        double base = predecirTiempo(origen, destino, tipoCarga);
        
        // Ajuste por Clima
        if (clima != null) {
            clima = clima.toLowerCase();
            if (clima.contains("lluvia") || clima.contains("lluvioso")) {
                base += 1.5;
            } else if (clima.contains("tormenta")) {
                base += 3.0;
            } else if (clima.contains("nublado")) {
                base += 0.5;
            }
        }
        
        // Ajuste por Servicio
        if (servicio != null) {
            servicio = servicio.toLowerCase();
            if (servicio.contains("premium")) {
                base -= 1.0;
            } else if (servicio.contains("económico") || servicio.contains("economico")) {
                base += 2.0;
            }
        }
        
        // Ajuste extra por Distancia real
        double distancia = com.example.guias.util.DistanciaUtil.calcularDistancia(origen, destino);
        if (distancia > 1000) {
            base += 1.0;
        }

        if (base < 1.0) base = 1.0; // Mínimo 1 día
        
        return Math.round(base * 10.0) / 10.0;
    }

    public String predecirEmpaque(double peso, double ancho, double alto) {
        try {
            Instance inst = new DenseInstance(estructuraEmpaque.numAttributes());
            inst.setDataset(estructuraEmpaque);
            inst.setValue(estructuraEmpaque.attribute("peso"), peso);
            inst.setValue(estructuraEmpaque.attribute("ancho"), ancho);
            inst.setValue(estructuraEmpaque.attribute("alto"), alto);

            double resultado = modeloEmpaque.classifyInstance(inst);
            return estructuraEmpaque.classAttribute().value((int) resultado);
        } catch (Exception e) {
            System.err.println("Error en predicción empaque: " + e.getMessage());
            return "Paquete Pequeño (Hasta 5kg)";
        }
    }

    public double predecirCO2(double distancia, double peso) {
        try {
            Instance inst = new DenseInstance(estructuraCO2.numAttributes());
            inst.setDataset(estructuraCO2);
            inst.setValue(estructuraCO2.attribute("distancia_km"), distancia);
            inst.setValue(estructuraCO2.attribute("peso_kg"), peso);

            double gramosCO2 = modeloCO2.classifyInstance(inst);
            // Retornar en Kilogramos para mayor legibilidad
            return Math.round((gramosCO2 / 1000.0) * 100.0) / 100.0;
        } catch (Exception e) {
            System.err.println("Error en predicción CO2: " + e.getMessage());
            return Math.round((distancia * 0.16) * 100.0) / 100.0; // Fallback a la constante anterior
        }
    }
}