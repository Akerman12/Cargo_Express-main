
package com.example.guias.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.example.guias.model.Guia;
import com.example.guias.service.GuiaService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@ConditionalOnProperty(name = "sai.rag.enabled", havingValue = "true", matchIfMissing = true)
@Component
public class GuiaTools {

    private final GuiaService guiaService;

    public GuiaTools(GuiaService guiaService) {
        this.guiaService = guiaService;
    }

    @Tool(
        name = "consultarEstadoGuia",
        description = "Consulta el estado de una guía de Carga Express usando su identificador."
    )
    public String consultarEstadoGuia(
        @ToolParam(description = "Identificador numérico de la guía")
        Long id
    ) {
        return guiaService.buscarPorId(id)
            .filter(guia -> !"Eliminada".equalsIgnoreCase(guia.getEstado()))
            .map(guia -> "Guía " + guia.getId()
                + ". Estado: " + guia.getEstado()
                + ". Origen: " + guia.getCiudadOrigen()
                + ". Destino: " + guia.getCiudadDestino())
            .orElse("No se encontró una guía con el identificador " + id);
    }
}
