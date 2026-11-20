package com.example.guias.repository.mongo;

import com.example.guias.model.mongo.MongoPago;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MongoPagoRepository extends MongoRepository<MongoPago, String> {
    MongoPago findByGuiaId(String guiaId);

    MongoPago findBySqlId(Long sqlId);
}
