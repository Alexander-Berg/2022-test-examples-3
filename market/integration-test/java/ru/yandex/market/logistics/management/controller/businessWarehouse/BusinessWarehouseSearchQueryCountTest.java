package ru.yandex.market.logistics.management.controller.businessWarehouse;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.entity.request.businessWarehouse.BusinessWarehouseFilter;
import ru.yandex.market.logistics.management.util.BusinessWarehouseFactory;
import ru.yandex.market.logistics.test.integration.jpa.JpaQueriesCount;
import ru.yandex.market.logistics.test.integration.jpa.QueriesContentInspector;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Проверка количества запросов при поиске бизнес-складов")
class BusinessWarehouseSearchQueryCountTest extends AbstractContextualTest {
    @Autowired
    private BusinessWarehouseFactory factory;

    @BeforeEach
    void setup() {
        factory.generateBusinessWarehouses(500);
    }

    @Test
    @JpaQueriesCount(82)
    void filterValidation() throws Exception {
        QueriesContentInspector.reset();
        getWarehouses().andExpect(status().isOk());
    }

    @Nonnull
    private ResultActions getWarehouses() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .put("/externalApi/business-warehouse")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(BusinessWarehouseFilter.newBuilder().build()))
            .param("size", String.valueOf(500))
            .param("page", String.valueOf(0));

        return mockMvc.perform(request);
    }
}
