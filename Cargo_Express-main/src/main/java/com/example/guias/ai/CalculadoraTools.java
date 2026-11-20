
package com.example.guias.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.example.guias.model.Guia;
import com.example.guias.service.PriceCalculatorService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@ConditionalOnProperty(name = "sai.rag.enabled", havingValue = "true", matchIfMissing = true)
@Component
public class CalculadoraTools {

    private final PriceCalculatorService priceCalculatorService;

    public CalculadoraTools(
            PriceCalculatorService priceCalculatorService) {
        this.priceCalculatorService = priceCalculatorService;
    }

    @Tool(
        name = "calcularCostoEnvio",
        description = "Calcula el costo aproximado de un envío de Carga Express considerando altura, ancho, largo, peso físico, valor declarado, ciudad de origen y ciudad de destino."
    )
    public String calcularCostoEnvio(

        @ToolParam(description = "Altura del paquete en centímetros")
        double altura,

        @ToolParam(description = "Ancho del paquete en centímetros")
        double ancho,

        @ToolParam(description = "Largo del paquete en centímetros")
        double largo,

        @ToolParam(description = "Peso físico en kilogramos")
        double peso,

        @ToolParam(description = "Valor declarado de la mercancía en pesos colombianos")
        double valorDeclarado,

        @ToolParam(description = "Ciudad de origen")
        String origen,

        @ToolParam(description = "Ciudad de destino")
        String destino

    ) {

        if (altura <= 0 || ancho <= 0 || largo <= 0
                || peso <= 0 || valorDeclarado < 0
                || origen == null || origen.isBlank()
                || destino == null || destino.isBlank()) {

            return "Los datos del envío no son válidos. " +
                   "Verifica las dimensiones, el peso, " +
                   "el valor declarado y las ciudades.";
        }

        Guia guia = new Guia();

        guia.setAlturaCm(altura);
        guia.setAnchoCm(ancho);
        guia.setLargoCm(largo);
        guia.setPesoKg(peso);
        guia.setValorDeclarado(valorDeclarado);
        guia.setCiudadOrigen(origen.trim());
        guia.setCiudadDestino(destino.trim());

        double pesoVolumetrico =
                (altura * ancho * largo) / 4000.0;

        double pesoCobrar =
                Math.max(peso, pesoVolumetrico);

        double precio =
                priceCalculatorService.calcularPrecio(guia);

        return String.format(
            java.util.Locale.forLanguageTag("es-CO"),
            "Cotización aproximada de tu envío:\n" +
            "Origen: %s\n" +
            "Destino: %s\n" +
            "Dimensiones: %.1f x %.1f x %.1f cm\n" +
            "Peso físico: %.2f kg\n" +
            "Peso volumétrico: %.2f kg\n" +
            "Peso cobrable: %.2f kg\n" +
            "Valor declarado: $%,.0f COP\n" +
            "Valor aproximado del envío: $%,.0f COP",
            origen, destino,
            altura, ancho, largo,
            peso, pesoVolumetrico, pesoCobrar,
            valorDeclarado, precio
        );
    }
}
