package com.example.painel.Configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Libera as rotas /indicadores só para quem manda a senha dos indicadores no cabeçalho
 * X-Indicadores-Senha. É uma barreira simples, pensada para a intranet: impede que qualquer
 * funcionário veja os indicadores, sem login por usuário.
 * Sem senha configurada no servidor, os indicadores respondem 503 (o resto da API segue normal).
 */
public class IndicadoresAcessoInterceptor implements HandlerInterceptor {
    public static final String CABECALHO_SENHA = "X-Indicadores-Senha";

    private static final Logger log = LoggerFactory.getLogger(IndicadoresAcessoInterceptor.class);

    private final byte[] senha;

    public IndicadoresAcessoInterceptor(String senha) {
        this.senha = senha == null || senha.isBlank() ? null : senha.getBytes(StandardCharsets.UTF_8);
        if (this.senha == null) {
            log.warn("INDICADORES_SENHA não configurada: os indicadores ficam indisponíveis (503) até ela ser definida no .env");
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        // O preflight do CORS não leva o cabeçalho da senha; quem decide é a chamada de verdade, logo depois.
        if (CorsUtils.isPreFlightRequest(request)) {
            return true;
        }
        if (senha == null) {
            return recusar(response, HttpStatus.SERVICE_UNAVAILABLE, "A senha dos indicadores não foi configurada no servidor.");
        }
        String informada = request.getHeader(CABECALHO_SENHA);
        // Comparação em tempo constante, para o tempo de resposta não dar pistas da senha.
        if (informada == null || !MessageDigest.isEqual(senha, informada.getBytes(StandardCharsets.UTF_8))) {
            return recusar(response, HttpStatus.UNAUTHORIZED, "Senha dos indicadores incorreta.");
        }
        return true;
    }

    private static boolean recusar(HttpServletResponse response, HttpStatus status, String mensagem) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"erro\":\"" + mensagem + "\"}");
        return false;
    }
}
