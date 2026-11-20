package com.example.guias.service;

import com.example.guias.model.Guia;
import com.example.guias.model.Usuario;
import com.example.guias.model.Pago;
import com.example.guias.model.Direccion;
import com.example.guias.model.mongo.MongoGuia;
import com.example.guias.model.mongo.MongoUsuario;
import com.example.guias.model.mongo.MongoPago;
import com.example.guias.model.mongo.MongoDireccion;
import com.example.guias.repository.GuiaRepository;
import com.example.guias.repository.UsuarioRepository;
import com.example.guias.repository.PagoRepository;
import com.example.guias.repository.DireccionRepository;
import com.example.guias.repository.mongo.MongoGuiaRepository;
import com.example.guias.repository.mongo.MongoUsuarioRepository;
import com.example.guias.repository.mongo.MongoPagoRepository;
import com.example.guias.repository.mongo.MongoDireccionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MongoSyncService {

    @Autowired private GuiaRepository sqlGuiaRepository;
    @Autowired private MongoGuiaRepository mongoGuiaRepository;

    @Autowired private UsuarioRepository sqlUsuarioRepository;
    @Autowired private MongoUsuarioRepository mongoUsuarioRepository;

    @Autowired private PagoRepository sqlPagoRepository;
    @Autowired private MongoPagoRepository mongoPagoRepository;

    @Autowired private DireccionRepository sqlDireccionRepository;
    @Autowired private MongoDireccionRepository mongoDireccionRepository;

    public void sincronizarGuias() {
        List<Guia> guiasSql = sqlGuiaRepository.findAll();
        for (Guia sqlGuia : guiasSql) {
            MongoGuia mongoGuia = mongoGuiaRepository.findBySqlId(sqlGuia.getId());
            if (mongoGuia == null) {
                mongoGuia = new MongoGuia();
                mongoGuia.setSqlId(sqlGuia.getId());
            }
            mongoGuia.setRemitente(sqlGuia.getRemitente());
            mongoGuia.setDestinatario(sqlGuia.getDestinatario());
            mongoGuia.setDireccionEnvio(sqlGuia.getDireccionEnvio());
            mongoGuia.setDireccionRecibo(sqlGuia.getDireccionRecibo());
            mongoGuia.setCedulaRemitente(sqlGuia.getCedulaRemitente());
            mongoGuia.setCedulaDestinatario(sqlGuia.getCedulaDestinatario());
            mongoGuia.setCiudadOrigen(sqlGuia.getCiudadOrigen());
            mongoGuia.setCiudadDestino(sqlGuia.getCiudadDestino());
            mongoGuia.setDepartamentoOrigen(sqlGuia.getDepartamentoOrigen());
            mongoGuia.setDepartamentoDestino(sqlGuia.getDepartamentoDestino());
            mongoGuia.setTipoCarga(sqlGuia.getTipoCarga());
            mongoGuia.setPesoKg(sqlGuia.getPesoKg());
            mongoGuia.setAlturaCm(sqlGuia.getAlturaCm());
            mongoGuia.setAnchoCm(sqlGuia.getAnchoCm());
            mongoGuia.setValorDeclarado(sqlGuia.getValorDeclarado());
            mongoGuia.setPrecio(sqlGuia.getPrecio());
            mongoGuia.setFechaCreacion(sqlGuia.getFechaCreacion());
            mongoGuia.setEstado(sqlGuia.getEstado());
            mongoGuia.setEstadoPago(sqlGuia.getEstadoPago());
            if (sqlGuia.getUsuario() != null) mongoGuia.setUsuarioId(sqlGuia.getUsuario().getId());
            if (sqlGuia.getRepartidorAsignado() != null) {
                mongoGuia.setRepartidorAsignadoId(sqlGuia.getRepartidorAsignado().getId());
            } else {
                mongoGuia.setRepartidorAsignadoId(null);
            }
            mongoGuia.setModificadoPor(sqlGuia.getModificadoPor());
            mongoGuia.setFirmaEntrega(sqlGuia.getFirmaEntrega());
            mongoGuia.setNotasEntrega(sqlGuia.getNotasEntrega());
            mongoGuia.setFechaEntrega(sqlGuia.getFechaEntrega());
            mongoGuia.setTiempoEstimadoDias(sqlGuia.getTiempoEstimadoDias());
            mongoGuiaRepository.save(mongoGuia);
        }
    }

    public void sincronizarUsuarios() {
        List<Usuario> usuariosSql = sqlUsuarioRepository.findAll();
        for (Usuario sqlUser : usuariosSql) {
            MongoUsuario mongoUser = mongoUsuarioRepository.findBySqlId(sqlUser.getId());
            if (mongoUser == null) {
                mongoUser = new MongoUsuario();
                mongoUser.setSqlId(sqlUser.getId());
            }
            mongoUser.setNombre(sqlUser.getNombre());
            mongoUser.setUsername(sqlUser.getUsername());
            mongoUser.setPassword(sqlUser.getPassword()); // Encriptado
            mongoUser.setRol(sqlUser.getRol().name());
            mongoUser.setAprobado(sqlUser.isAprobado());
            mongoUser.setEmail(sqlUser.getEmail());
            mongoUsuarioRepository.save(mongoUser);
        }
    }

    public void sincronizarPagos() {
        List<Pago> pagosSql = sqlPagoRepository.findAll();
        for (Pago sqlPago : pagosSql) {
            MongoPago mongoPago = mongoPagoRepository.findBySqlId(sqlPago.getId());
            if (mongoPago == null) {
                mongoPago = new MongoPago();
                mongoPago.setSqlId(sqlPago.getId());
            }
            mongoPago.setMonto(sqlPago.getMonto());
            mongoPago.setMetodo(sqlPago.getMetodo());
            mongoPago.setFecha(sqlPago.getFecha());
            mongoPago.setTitular(sqlPago.getTitular());
            if (sqlPago.getGuia() != null) {
                mongoPago.setGuiaId(String.valueOf(sqlPago.getGuia().getId()));
            }
            mongoPagoRepository.save(mongoPago);
        }
    }

    public void sincronizarDirecciones() {
        // Para direcciones, como se pueden eliminar, es más seguro limpiar y reescribir para mantener sincronía perfecta
        mongoDireccionRepository.deleteAll(); 
        List<Direccion> dirsSql = sqlDireccionRepository.findAll();
        for (Direccion sqlDir : dirsSql) {
            MongoDireccion mongoDir = new MongoDireccion();
            mongoDir.setSqlId(sqlDir.getId());
            mongoDir.setAlias(sqlDir.getAlias());
            mongoDir.setNombreContacto(sqlDir.getNombreContacto());
            mongoDir.setDireccion(sqlDir.getDireccion());
            mongoDir.setCiudad(sqlDir.getCiudad());
            mongoDir.setDepartamento(sqlDir.getDepartamento());
            mongoDir.setTelefono(sqlDir.getTelefono());
            if (sqlDir.getUsuario() != null) {
                mongoDir.setUsuarioId(String.valueOf(sqlDir.getUsuario().getId()));
            }
            mongoDireccionRepository.save(mongoDir);
        }
    }
}