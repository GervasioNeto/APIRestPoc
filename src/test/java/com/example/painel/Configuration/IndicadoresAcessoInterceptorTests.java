package com.example.painel.Configuration;

import com.example.painel.controllers.AtendidosPorHorarioController;
import com.example.painel.controllers.IndicadorController;
import com.example.painel.services.AtendidosPorHorarioService;
import com.example.painel.services.IndicadorService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static com.example.painel.Configuration.IndicadoresAcessoInterceptor.CABECALHO_SENHA;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// INT-10: a senha dos indicadores é conferida no back, em todas as rotas /indicadores.
class IndicadoresAcessoInterceptorTests {
    private static final String SENHA = "1234";
    private static final String INICIO = "2026-09-01T00:00:00";
    private static final String FIM = "2026-09-03T23:59:59";

    private final IndicadorService indicadorService = mock(IndicadorService.class);
    private final AtendidosPorHorarioService atendidosService = mock(AtendidosPorHorarioService.class);

    private MockMvc mvc(String senhaConfigurada) {
        return MockMvcBuilders
                .standaloneSetup(new IndicadorController(indicadorService), new AtendidosPorHorarioController(atendidosService))
                .addMappedInterceptors(new String[]{"/indicadores", "/indicadores/**"},
                        new IndicadoresAcessoInterceptor(senhaConfigurada))
                .build();
    }

    @Test void acessoComSenhaCertaResponde204() throws Exception {
        mvc(SENHA).perform(post("/indicadores/acesso").header(CABECALHO_SENHA, SENHA))
                .andExpect(status().isNoContent());
    }

    @Test void acessoComSenhaErradaOuSemSenhaResponde401() throws Exception {
        var mvc = mvc(SENHA);
        mvc.perform(post("/indicadores/acesso").header(CABECALHO_SENHA, "0000"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").value("Senha dos indicadores incorreta."));
        mvc.perform(post("/indicadores/acesso"))
                .andExpect(status().isUnauthorized());
    }

    @Test void indicadoresEAtendidosSoRespondemComASenha() throws Exception {
        when(atendidosService.listar(any(), any(), any())).thenReturn(List.of());
        var mvc = mvc(SENHA);

        mvc.perform(get("/indicadores").param("inicio", INICIO).param("fim", FIM))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/indicadores/atendidos").param("inicio", INICIO).param("fim", FIM))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(indicadorService, atendidosService);

        mvc.perform(get("/indicadores/atendidos").param("inicio", INICIO).param("fim", FIM).header(CABECALHO_SENHA, SENHA))
                .andExpect(status().isOk());
    }

    @Test void semSenhaConfiguradaNoServidorResponde503() throws Exception {
        var mvc = mvc("  ");
        mvc.perform(post("/indicadores/acesso").header(CABECALHO_SENHA, ""))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.erro").value("A senha dos indicadores não foi configurada no servidor."));
        mvc.perform(get("/indicadores").param("inicio", INICIO).param("fim", FIM).header(CABECALHO_SENHA, SENHA))
                .andExpect(status().isServiceUnavailable());
        verifyNoInteractions(indicadorService);
    }
}
