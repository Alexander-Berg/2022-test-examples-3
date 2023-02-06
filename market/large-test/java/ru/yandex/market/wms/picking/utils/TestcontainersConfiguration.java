package ru.yandex.market.wms.picking.utils;

import org.junit.Rule;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;

import ru.yandex.market.wms.common.spring.IntegrationTest;

@AutoConfigureMockMvc
@ContextConfiguration(initializers = TestcontainersConfiguration.DockerMSSQLServerDataSourceInitializer.class)
public class TestcontainersConfiguration extends IntegrationTest {
    private static final DockerImageName MY_IMAGE = DockerImageName.parse(
            "mcr.microsoft.com/mssql/server:2017-CU12");

    @Rule
    private static final MSSQLServerContainer<?> MSSQL_SERVER = new MSSQLServerContainer<>(MY_IMAGE)
            .withInitScript("testcontainers/init.sql")
            .acceptLicense();

    public TestcontainersConfiguration() {
        MSSQL_SERVER.start();
        System.setProperty("spring.datasource.url", MSSQL_SERVER.getJdbcUrl());
    }

    public static class DockerMSSQLServerDataSourceInitializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                    applicationContext, "spring.datasource.url=" + MSSQL_SERVER.getJdbcUrl()
            );
        }
    }
}

