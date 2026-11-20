package com.example.guias.repository.mongo;

import com.example.guias.model.mongo.MongoGuia;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MongoGuiaRepository extends MongoRepository<MongoGuia, String> {
    List<MongoGuia> findByUsuarioId(Long usuarioId);
    MongoGuia findBySqlId(Long sqlId);
}
