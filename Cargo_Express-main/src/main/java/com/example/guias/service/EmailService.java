package com.example.guias.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @org.springframework.beans.factory.annotation.Value("${spring.mail.username}")
    private String fromEmail;

    public void enviarCorreoHtml(String para, String asunto, String htmlContent, String logoPath) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(fromEmail);
        helper.setTo(para);
        helper.setSubject(asunto);
        helper.setText(htmlContent, true);

        if (logoPath != null) {
            try {
                org.springframework.core.io.ClassPathResource res = new org.springframework.core.io.ClassPathResource(logoPath);
                if (!res.exists()) {
                    res = new org.springframework.core.io.ClassPathResource("/" + logoPath);
                }
                helper.addInline("logo", res);
            } catch (Exception e) {
                System.out.println("Error adjuntando logo al correo: " + e.getMessage());
            }
        }

        mailSender.send(message);
    }

    public void enviarCorreo(String para, String asunto, String mensaje) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(fromEmail);
        helper.setTo(para);
        helper.setSubject(asunto);
        helper.setText(mensaje, false);

        mailSender.send(message);
    }
}
