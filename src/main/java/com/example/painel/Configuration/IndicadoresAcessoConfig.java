package com.example.painel.Configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Exige a senha dos indicadores (INDICADORES_SENHA no .env) em GET /indicadores,
 * GET /indicadores/atendidos e POST /indicadores/acesso.
 */
@Configuration
public class IndicadoresAcessoConfig implements WebMvcConfigurer {
    private final String senha;

    public IndicadoresAcessoConfig(@Value("${indicadores.senha:}") String senha) {
        this.senha = senha;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new IndicadoresAcessoInterceptor(senha))
                .addPathPatterns("/indicadores", "/indicadores/**");
    }
}
