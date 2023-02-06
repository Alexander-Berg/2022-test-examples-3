package ru.yandex.market.loyalty.core.service.report;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.yandex.market.common.report.model.MarketSearchRequest;

import static org.junit.jupiter.api.Assertions.*;

class ReportServiceRequestBuilderTest {

    @Test
    public void shouldProduceReportRequestWithRegset() {
        MarketSearchRequest regset = ReportServiceRequestBuilder.buildGetOffers(Set.of(), 0, Map.of("regset", "2"));

        assertTrue(regset.getParams().get("regset").size() == 1);
        assertTrue(regset.getParams().get("regset").contains("2"));
    }
}
