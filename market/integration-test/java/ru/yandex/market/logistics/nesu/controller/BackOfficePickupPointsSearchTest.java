package ru.yandex.market.logistics.nesu.controller;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.logistics.nesu.base.AbstractPickupPointsSearchTest;
import ru.yandex.market.logistics.nesu.dto.pickuppoints.PickupPointsFilter;

@DisplayName("Поиск пунктов выдачи заказов")
class BackOfficePickupPointsSearchTest extends AbstractPickupPointsSearchTest {
    @Nonnull
    @Override
    protected ResultActions search(PickupPointsFilter filter) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = request(HttpMethod.PUT, "/back-office/pickup-points", filter);
        return mockMvc.perform(requestBuilder);
    }
}
