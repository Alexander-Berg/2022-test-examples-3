package ru.yandex.market.logistics.nesu.controller.shipment;

import javax.annotation.Nonnull;

import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.logistics.nesu.base.AbstractGetImportIntervalsTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class BackOfficeGetImportIntervalsTest extends AbstractGetImportIntervalsTest {

    @Nonnull
    @Override
    protected MockHttpServletRequestBuilder getImportIntervalsRequest() {
        return get("/back-office/shipments/intervals/import");
    }
}
