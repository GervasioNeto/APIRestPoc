package com.example.painel.Configuration;

import com.example.painel.services.ProtocoloTempoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;

@Configuration
public class ProtocoloTempoInicializacao {
    private static final Logger log = LoggerFactory.getLogger(ProtocoloTempoInicializacao.class);

    // Ao subir, garante o protocolo padrão num banco novo. Se a tabela ainda não existir
    // (ex.: ddl-auto=none com o schema aplicado depois), só avisa: não impede a aplicação de subir.
    @Bean
    public ApplicationRunner cadastrarProtocoloPadrao(ProtocoloTempoService service) {
        return args -> {
            try {
                int gravados = service.cadastrarProtocoloPadraoSeVazio();
                if (gravados > 0) {
                    log.info("protocolo_tempo estava vazia: {} tempos do protocolo padrão cadastrados", gravados);
                }
            } catch (DataAccessException e) {
                log.warn("Protocolo padrão não cadastrado: protocolo_tempo indisponível ({})", e.getMostSpecificCause().getMessage());
            }
        };
    }
}
