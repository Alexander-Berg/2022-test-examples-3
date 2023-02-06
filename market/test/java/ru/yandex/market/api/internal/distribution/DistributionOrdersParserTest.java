package ru.yandex.market.api.internal.distribution;

import java.util.List;

import org.junit.Test;

import ru.yandex.market.api.common.Result;
import ru.yandex.market.api.distribution.DistributionOrder;
import ru.yandex.market.api.domain.partnerstat.PartnerStatError;
import ru.yandex.market.api.partnerstat.distribution.DistributionOrdersParser;
import ru.yandex.market.api.util.ResourceHelpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DistributionOrdersParserTest {

    @Test
    public void testParseOrdersTotal() {
        DistributionOrdersParser parser = new DistributionOrdersParser(false);
        Result<List<DistributionOrder>, PartnerStatError> result = parser.parse(ResourceHelpers.getResource("orders_total.json"));

        assertNotNull(result.getValue());
        assertNull(result.getError());
        assertEquals(2, result.getValue().size());

        assertEquals("60", result.getValue().get(0).getCart());
        assertEquals(Long.valueOf(11111), result.getValue().get(0).getClid());
        assertEquals(Long.valueOf(22222), result.getValue().get(0).getOrderId());
        assertEquals("2019-11-02T10:30:00", result.getValue().get(0).getDateCreated());
        assertEquals("2019-12-05T10:30:00", result.getValue().get(0).getDateUpdated());
        assertEquals("6", result.getValue().get(0).getPayment());
        assertEquals("APPROVED", result.getValue().get(0).getStatus());
        assertEquals("new", result.getValue().get(0).getTariff());
        assertEquals("aaabb", result.getValue().get(0).getVid());
        assertEquals("BOOK-AF", result.getValue().get(0).getPromocode());

        assertNull(result.getValue().get(0).getItems());
    }

    @Test
    public void testParseOrdersDetailed() {
        DistributionOrdersParser parser = new DistributionOrdersParser(false);
        Result<List<DistributionOrder>, PartnerStatError> result = parser.parse(ResourceHelpers.getResource("orders_detailed.json"));

        assertNotNull(result.getValue());
        assertNull(result.getError());
        assertEquals(2, result.getValue().size());

        assertEquals("60", result.getValue().get(0).getCart());
        assertEquals(11111L, (long) result.getValue().get(0).getClid());
        assertEquals(22222L, (long) result.getValue().get(0).getOrderId());
        assertEquals("2019-11-02T10:30:00", result.getValue().get(0).getDateCreated());
        assertEquals("2019-12-05T10:30:00", result.getValue().get(0).getDateUpdated());
        assertEquals("6", result.getValue().get(0).getPayment());
        assertEquals("APPROVED", result.getValue().get(0).getStatus());
        assertEquals("new", result.getValue().get(0).getTariff());
        assertEquals("aaabb", result.getValue().get(0).getVid());
        assertEquals("BOOK-AF", result.getValue().get(0).getPromocode());

        assertNotNull(result.getValue().get(0).getItems());
        assertEquals(3, result.getValue().get(0).getItems().size());

        assertEquals("10", result.getValue().get(0).getItems().get(0).getCart());
        assertEquals(0, (int) result.getValue().get(0).getItems().get(0).getItemId());
        assertEquals("1", result.getValue().get(0).getItems().get(0).getPayment());
        assertEquals("0.1", result.getValue().get(0).getItems().get(0).getTariffRate());
        assertEquals("CEHAC", result.getValue().get(0).getItems().get(0).getTariffName());
        assertNull(result.getValue().get(0).getItems().get(2).getTariffName());
        assertEquals(3, (int) result.getValue().get(0).getItems().get(0).getItemCount());
    }

    @Test
    public void testParseOrdersError() {
        DistributionOrdersParser parser = new DistributionOrdersParser(false);
        Result<List<DistributionOrder>, PartnerStatError> result = parser.parse(ResourceHelpers.getResource("orders_error.json"));

        assertNull(result.getValue());
        assertNotNull(result.getError());

        assertEquals("INVALID", result.getError().code());
        assertEquals("clid must have at least one value", result.getError().getMessage());
        assertEquals("clid", result.getError().getField());
        assertEquals("INVALID", result.getError().getSubCode());
    }
}
