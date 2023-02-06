package ru.yandex.market.logistics.nesu.controller.shipment;

import javax.annotation.Nonnull;

import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.logistics.nesu.base.AbstractGetWithdrawIntervalsTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class BackOfficeGetWithdrawIntervalsTest extends AbstractGetWithdrawIntervalsTest {

    @Nonnull
    @Override
    protected MockHttpServletRequestBuilder getWithdrawIntervalsRequest() {
        return get("/back-office/shipments/intervals/withdraw");
    }
}
