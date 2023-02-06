package ru.yandex.market.logistics.nesu.api.controller;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.common.services.auth.blackbox.BlackboxService;
import ru.yandex.market.logistics.nesu.api.auth.ApiAuthHolder;
import ru.yandex.market.logistics.nesu.base.AbstractGetWithdrawIntervalsTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class ApiGetWithdrawIntervalsTest extends AbstractGetWithdrawIntervalsTest {

    @Autowired
    private BlackboxService blackboxService;

    private ApiAuthHolder authHolder;

    @BeforeEach
    void setup() {
        authHolder = new ApiAuthHolder(blackboxService, objectMapper);
    }

    @Nonnull
    @Override
    protected MockHttpServletRequestBuilder getWithdrawIntervalsRequest() {
        return get("/api/shipments/intervals/withdraw")
            .headers(authHolder.authHeaders());
    }
}
