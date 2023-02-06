package ru.yandex.market.logistics.management.controller.health;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class DatasourceHealthTest extends AbstractContextualTest {

    @Test
    void postgresMasterOk() throws Exception {
        testDatabase("master");
    }

    @Test
    void postgresReplicaOk() throws Exception {
        testDatabase("replica");
    }

    @Test
    void mysqlYadoNotOk() throws Exception {
        String contentAsString = mockMvc.perform(get("/health/datasource/mysql"))
            .andReturn()
            .getResponse()
            .getContentAsString();
        softly
            .assertThat(contentAsString)
            .startsWith(
                "2;Failed to obtain JDBC Connection: " +
                    "DataSource returned null from getConnection(): Mock for DataSource"
            );
    }

    private void testDatabase(String path) throws Exception {
        String contentAsString = mockMvc.perform(get("/health/datasource/" + path))
            .andReturn()
            .getResponse()
            .getContentAsString();
        softly
            .assertThat(contentAsString)
            .isEqualTo("0;OK");
    }


}
