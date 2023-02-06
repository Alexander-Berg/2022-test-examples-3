package ru.yandex.market.common.report;

import org.junit.Test;
import ru.yandex.market.common.report.model.MarketSearchRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class MarketReportRequestBuilderTest {

    @Test
    public void testOnlyOneRids() {
        MarketSearchRequest request = MarketReportRequestBuilder.buildGetOffer(123L, "offer_id-123");
        assertNull(request.getRegionId());
        assertNotNull(request.getParams().get("rids"));
        assertEquals(0, request.getParams().get("rids").size());

        request.setRegionId(123L);
        assertNotNull(request.getRegionId());
        assertNotNull(request.getParams().get("rids"));
        assertEquals(0, request.getParams().get("rids").size());
    }
}
