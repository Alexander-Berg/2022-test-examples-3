package ru.yandex.market.logistics.nesu.controller.business;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.logistics.nesu.dto.business.BusinessWarehouseRequest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@ParametersAreNonnullByDefault
@DisplayName("Создание склада бизнеса")
class CreateBusinessWarehouseTest extends AbstractCreateBusinessWarehouseTest {
    @Nonnull
    @Override
    @SneakyThrows
    ResultActions createBusinessWarehouse(String shopId, BusinessWarehouseRequest dto) {
        MockHttpServletRequestBuilder request = post("/back-office/business/warehouses")
            .param("userId", "1")
            .param("shopId", shopId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto));

        return mockMvc.perform(request);
    }
}
