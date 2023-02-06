package ru.yandex.market.logistics.nesu.api.pickuppoints;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.common.services.auth.blackbox.BlackboxService;
import ru.yandex.market.logistics.nesu.api.auth.ApiAuthHolder;
import ru.yandex.market.logistics.nesu.base.AbstractPickupPointsSearchTest;
import ru.yandex.market.logistics.nesu.dto.pickuppoints.PickupPointsFilter;

@DisplayName("Поиск пунктов выдачи заказов в Open API")
class ApiPickupPointsSearchTest extends AbstractPickupPointsSearchTest {

    @Autowired
    private BlackboxService blackboxService;

    private ApiAuthHolder authHolder;

    @BeforeEach
    void setupAuth() {
        authHolder = new ApiAuthHolder(blackboxService, objectMapper);
    }

    @Nonnull
    @Override
    protected ResultActions search(PickupPointsFilter filter) throws Exception {
        return mockMvc.perform(request(HttpMethod.PUT, "/api/pickup-points", filter)
            .headers(authHolder.authHeaders()));
    }
}
