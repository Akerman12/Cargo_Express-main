package com.example.guias.service;

import com.example.guias.model.Guia;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Service
public class PdfService {

    public byte[] generarEtiqueta(Guia guia) throws DocumentException, IOException {
        // Color Corporativo (Azul SAI)
        Color glsBlue = new Color(0, 46, 125);

        // Formato de etiqueta térmica estándar (4x6 pulgadas)
        Rectangle pageSize = new Rectangle(288, 432); 
        Document document = new Document(pageSize, 20, 20, 20, 20);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        PdfWriter.getInstance(document, out);
        document.open();

        // Tipografías
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, glsBlue);
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
        Font idFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, glsBlue);

        // Encabezado con Logo y Título
        try {
            java.io.InputStream logoStream = getClass().getResourceAsStream("/static/imagenes/LOGO Cargo Expres.jpg");
            if (logoStream != null) {
                byte[] logoBytes = logoStream.readAllBytes();
                Image logo = Image.getInstance(logoBytes);
                logo.scaleToFit(80, 80);
                logo.setAlignment(Element.ALIGN_CENTER);
                document.add(logo);
            } else {
                Paragraph header = new Paragraph("SAI CARGO EXPRESS", titleFont);
                header.setAlignment(Element.ALIGN_CENTER);
                document.add(header);
            }
        } catch (Exception e) {
            Paragraph header = new Paragraph("SAI CARGO EXPRESS", titleFont);
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);
        }

        Paragraph subHeader = new Paragraph("LOGÍSTICA INTELIGENTE A TU ALCANCE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, glsBlue));
        subHeader.setAlignment(Element.ALIGN_CENTER);
        document.add(subHeader);

        document.add(new Paragraph("\n"));

        // PUBLICIDAD Y PROMOCIÓN
        Paragraph promoBox = new Paragraph("¡TU LOGÍSTICA EN BUENAS MANOS!\nEnvíos Nacionales | Fulfilment | E-commerce\nCobertura en toda Colombia", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7));
        promoBox.setAlignment(Element.ALIGN_CENTER);
        document.add(promoBox);

        Paragraph urlBox = new Paragraph("www.saicargo.com", FontFactory.getFont(FontFactory.HELVETICA, 7, glsBlue));
        urlBox.setAlignment(Element.ALIGN_CENTER);
        document.add(urlBox);

        document.add(new Paragraph("------------------------------------------------------------"));

        // ID de la Guía destacado
        Paragraph idLabel = new Paragraph("NÚMERO DE GUÍA (TRACKING)", labelFont);
        idLabel.setAlignment(Element.ALIGN_CENTER);
        document.add(idLabel);
        
        Paragraph idValue = new Paragraph("#" + guia.getId(), idFont);
        idValue.setAlignment(Element.ALIGN_CENTER);
        document.add(idValue);

        document.add(new Paragraph("------------------------------------------------------------"));

        // Datos del Remitente
        document.add(new Paragraph("REMITENTE:", labelFont));
        document.add(new Paragraph(guia.getRemitente().toUpperCase(), textFont));
        document.add(new Paragraph("Ciudad: " + guia.getCiudadOrigen() + " (" + guia.getDepartamentoOrigen() + ")", textFont));
        document.add(new Paragraph("Dir: " + guia.getDireccionEnvio(), textFont));

        document.add(new Paragraph("\n"));

        // Datos del Destinatario (Más grande para visibilidad)
        document.add(new Paragraph("DESTINATARIO:", labelFont));
        document.add(new Paragraph(guia.getDestinatario().toUpperCase(), titleFont));
        document.add(new Paragraph("CIUDAD: " + guia.getCiudadDestino().toUpperCase(), titleFont));
        document.add(new Paragraph("DEPTO: " + guia.getDepartamentoDestino().toUpperCase(), labelFont));
        document.add(new Paragraph("DIR: " + guia.getDireccionRecibo(), textFont));
        document.add(new Paragraph("CC/NIT: " + guia.getCedulaDestinatario(), textFont));

        document.add(new Paragraph("\n"));
        document.add(new Paragraph("------------------------------------------------------------"));

        // Detalles de Carga
        document.add(new Paragraph("DETALLES DE CARGA:", labelFont));
        document.add(new Paragraph("Peso: " + guia.getPesoKg() + " Kg", textFont));
        document.add(new Paragraph("Tipo: " + guia.getTipoCarga(), textFont));
        document.add(new Paragraph("Fecha: " + guia.getFechaCreacion().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), textFont));
        
        Paragraph statusPara = new Paragraph("ESTADO: " + guia.getEstado().toUpperCase(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.RED));
        statusPara.setAlignment(Element.ALIGN_RIGHT);
        document.add(statusPara);

        document.add(new Paragraph("\n\n"));
        
        // Mensaje de pie
        Paragraph footer = new Paragraph("DOCUMENTO OFICIAL DE TRANSPORTE\nSAI CARGO COLOMBIA", FontFactory.getFont(FontFactory.HELVETICA, 7));
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        document.close();
        return out.toByteArray();
    }

    public byte[] generarReciboPago(com.example.guias.model.Pago pago) throws DocumentException, IOException {
        Guia guia = pago.getGuia();
        Color glsBlue = new Color(0, 46, 125);
        Color successGreen = new Color(16, 185, 129);

        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        // Fuentes
        Font h1 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, glsBlue);
        Font h2 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, glsBlue);
        Font bodyBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
        Font body = FontFactory.getFont(FontFactory.HELVETICA, 11);
        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);

        // Header Table
        com.lowagie.text.pdf.PdfPTable headerTable = new com.lowagie.text.pdf.PdfPTable(2);
        headerTable.setWidthPercentage(100);
        
        // Logo
        try {
            java.io.InputStream logoStream = getClass().getResourceAsStream("/static/imagenes/LOGO Cargo Expres.jpg");
            if (logoStream != null) {
                byte[] logoBytes = logoStream.readAllBytes();
                Image logo = Image.getInstance(logoBytes);
                logo.scaleToFit(120, 120);
                com.lowagie.text.pdf.PdfPCell logoCell = new com.lowagie.text.pdf.PdfPCell(logo);
                logoCell.setBorder(Rectangle.NO_BORDER);
                headerTable.addCell(logoCell);
            } else {
                headerTable.addCell(new Paragraph("SAI CARGO", h1));
            }
        } catch (Exception e) {
            headerTable.addCell(new Paragraph("SAI CARGO", h1));
        }

        // Invoice Info
        com.lowagie.text.pdf.PdfPCell infoCell = new com.lowagie.text.pdf.PdfPCell();
        infoCell.setBorder(Rectangle.NO_BORDER);
        infoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        infoCell.addElement(new Paragraph("RECIBO DE PAGO", h1));
        infoCell.addElement(new Paragraph("No. Transacción: #" + pago.getId(), bodyBold));
        infoCell.addElement(new Paragraph("Fecha: " + pago.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), body));
        headerTable.addCell(infoCell);
        
        document.add(headerTable);
        document.add(new Paragraph("\n"));
        document.add(new Paragraph("----------------------------------------------------------------------------------------------------------------------------------", footerFont));
        document.add(new Paragraph("\n"));

        // Client & Service Info
        document.add(new Paragraph("INFORMACIÓN DEL PAGO", h2));
        document.add(new Paragraph("\n"));
        
        document.add(new Paragraph("Cliente/Titular: " + pago.getTitular().toUpperCase(), bodyBold));
        document.add(new Paragraph("Servicio Asociado: Guía de Transporte #" + guia.getId(), body));
        document.add(new Paragraph("Método de Pago: " + pago.getMetodo(), body));
        document.add(new Paragraph("Ruta: " + guia.getCiudadOrigen() + " -> " + guia.getCiudadDestino(), body));
        
        document.add(new Paragraph("\n\n"));

        // Table of Items
        com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        
        // rows
        String[][] items = {
            {"Subtotal Servicio de Transporte", "$" + String.format("%,.0f", guia.getPrecio() * 0.81)},
            {"Seguro de Mercancía (Protección Total)", "$" + String.format("%,.0f", guia.getValorDeclarado() * 0.015)},
            {"Impuesto al Valor Agregado (IVA 19%)", "$" + String.format("%,.0f", guia.getPrecio() * 0.19)}
        };

        for (String[] item : items) {
            table.addCell(new Paragraph(item[0], body));
            table.addCell(new Paragraph(item[1], body));
        }

        // Total Row
        com.lowagie.text.pdf.PdfPCell totalLabelCell = new com.lowagie.text.pdf.PdfPCell(new Paragraph("TOTAL PAGADO", h2));
        totalLabelCell.setBackgroundColor(new Color(241, 245, 249));
        totalLabelCell.setPadding(10f);
        table.addCell(totalLabelCell);

        com.lowagie.text.pdf.PdfPCell totalValueCell = new com.lowagie.text.pdf.PdfPCell(new Paragraph("$" + String.format("%,.0f", pago.getMonto()) + " COP", h1));
        totalValueCell.setBackgroundColor(new Color(241, 245, 249));
        totalValueCell.setPadding(10f);
        totalValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(totalValueCell);

        document.add(table);
        document.add(new Paragraph("\n\n"));

        // Status Badge
        Paragraph status = new Paragraph("ESTADO: TRANSACCIÓN EXITOSA / PAGADO", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, successGreen));
        status.setAlignment(Element.ALIGN_CENTER);
        document.add(status);

        document.add(new Paragraph("\n\n\n"));

        // Footer
        Paragraph foot = new Paragraph("Gracias por confiar en SAI Cargo Express S.A.S.\nEste documento es un comprobante electrónico de pago válido ante nuestra red logística.\nSi tienes dudas, contáctanos al WhatsApp: +57 3043915104", footerFont);
        foot.setAlignment(Element.ALIGN_CENTER);
        document.add(foot);

        document.close();
        return out.toByteArray();
    }
}
