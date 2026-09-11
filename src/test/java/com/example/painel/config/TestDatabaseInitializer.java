package com.example.painel.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Inicializador de contexto de teste.
 * Garante a existência do banco de testes PostgreSQL antes da inicialização do pool de conexões do Spring.
 */
public class TestDatabaseInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Logger log = LoggerFactory.getLogger(TestDatabaseInitializer.class);

    private static final String TARGET_TEST_DATABASE = "painel_test";
    private static final String DEFAULT_ADMIN_DATABASE = "postgres";
    private static final String DEFAULT_POSTGRES_USER = "postgres";

    private static final String DATASOURCE_URL_PROPERTY = "spring.datasource.url";
    private static final String DATASOURCE_USERNAME_PROPERTY = "spring.datasource.username";
    private static final String DATASOURCE_PASSWORD_PROPERTY = "spring.datasource.password";

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        Environment environment = applicationContext.getEnvironment();
        String testDatasourceUrl = environment.getProperty(DATASOURCE_URL_PROPERTY);

        if (testDatasourceUrl == null || !testDatasourceUrl.contains(TARGET_TEST_DATABASE)) {
            return;
        }

        String username = environment.getProperty(DATASOURCE_USERNAME_PROPERTY, DEFAULT_POSTGRES_USER);
        String password = environment.getProperty(DATASOURCE_PASSWORD_PROPERTY, "");

        String adminConnectionUrl = buildAdminConnectionUrl(testDatasourceUrl);

        ensureTestDatabaseExists(adminConnectionUrl, username, password);
    }

    /**
     * Converte a URL do banco de teste para apontar para o banco administrativo padrão "postgres".
     * Exemplo: jdbc:postgresql://localhost:5432/painel_test -> jdbc:postgresql://localhost:5432/postgres
     */
    private String buildAdminConnectionUrl(String originalUrl) {
        return originalUrl.replace("/" + TARGET_TEST_DATABASE, "/" + DEFAULT_ADMIN_DATABASE);
    }

    /**
     * Verifica no catálogo do PostgreSQL se o banco de teste existe e o cria se necessário.
     */
    private void ensureTestDatabaseExists(String adminUrl, String username, String password) {
        String checkDatabaseQuery = "SELECT 1 FROM pg_database WHERE datname = '" + TARGET_TEST_DATABASE + "'";
        String createDatabaseStatement = "CREATE DATABASE " + TARGET_TEST_DATABASE;

        try (Connection connection = DriverManager.getConnection(adminUrl, username, password);
             Statement statement = connection.createStatement()) {

            try (ResultSet resultSet = statement.executeQuery(checkDatabaseQuery)) {
                boolean databaseAlreadyExists = resultSet.next();

                if (!databaseAlreadyExists) {
                    log.info("Banco de testes '{}' não encontrado. Criando automaticamente no PostgreSQL...", TARGET_TEST_DATABASE);
                    statement.executeUpdate(createDatabaseStatement);
                    log.info("Banco de testes '{}' criado com sucesso!", TARGET_TEST_DATABASE);
                } else {
                    log.debug("Banco de testes '{}' já existe. Prosseguindo com os testes.", TARGET_TEST_DATABASE);
                }
            }

        } catch (SQLException exception) {
            log.warn("Não foi possível verificar/criar o banco '{}' automaticamente. " +
                     "Certifique-se de que o PostgreSQL local está rodando e com credenciais válidas. Motivo: {}",
                     TARGET_TEST_DATABASE, exception.getMessage());
        }
    }
}
