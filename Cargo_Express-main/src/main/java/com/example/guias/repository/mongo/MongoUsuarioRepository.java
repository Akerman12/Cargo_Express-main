package com.example.guias.repository.mongo;

import com.example.guias.model.mongo.MongoUsuario;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MongoUsuarioRepository extends MongoRepository<MongoUsuario, String> {
    MongoUsuario findByUsername(String username);
    MongoUsuario findByEmail(String email);
    MongoUsuario findBySqlId(Long sqlId);
}
