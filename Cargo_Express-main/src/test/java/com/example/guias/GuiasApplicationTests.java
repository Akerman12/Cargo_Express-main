package com.example.guias;

import com.example.guias.repository.mongo.MongoDireccionRepository;
import com.example.guias.repository.mongo.MongoGuiaRepository;
import com.example.guias.repository.mongo.MongoPagoRepository;
import com.example.guias.repository.mongo.MongoUsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import com.example.guias.security.JwtUtil;

@SpringBootTest
@ActiveProfiles("test")
class GuiasApplicationTests {

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private com.example.guias.service.UsuarioDetailsService usuarioDetailsService;

    @MockBean
    private MongoDireccionRepository mongoDireccionRepository;

    @MockBean
    private MongoGuiaRepository mongoGuiaRepository;

    @MockBean
    private MongoPagoRepository mongoPagoRepository;

    @MockBean
    private MongoUsuarioRepository mongoUsuarioRepository;

	@Test
	void contextLoads() {
	}

}
