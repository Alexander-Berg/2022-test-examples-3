package ru.yandex.market.fulfillment.stockstorage.domain.converter;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.fulfillment.stockstorage.domain.dto.FulfillmentPartner;
import ru.yandex.market.fulfillment.stockstorage.service.lms.LmsPartnerStatus;
import ru.yandex.market.fulfillment.stockstorage.service.lms.LmsPartnerType;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class PartnerResponseConverterTest {

    @Test
    public void convertSinglePartnerResponse() {
        PartnerResponse partnerResponse = PartnerResponse.newBuilder()
                .id(2)
                .partnerType(PartnerType.FULFILLMENT)
                .name("MARKET_ROSTOV")
                .status(PartnerStatus.ACTIVE)
                .build();

        FulfillmentPartner fulfillmentPartner = PartnerResponseConverter.convert(partnerResponse);

        assertNotNull(fulfillmentPartner);

        assertEquals(2, fulfillmentPartner.getId());
        assertEquals("MARKET_ROSTOV", fulfillmentPartner.getName());
        assertEquals(LmsPartnerType.FULFILLMENT, fulfillmentPartner.getPartnerType());
        assertEquals(LmsPartnerStatus.ACTIVE, fulfillmentPartner.getStatus());
    }

    @Test
    public void convertMultiPartnerResponse() {
        List<PartnerResponse> partnerResponses = Arrays.asList(
                PartnerResponse.newBuilder()
                        .id(2)
                        .partnerType(PartnerType.FULFILLMENT)
                        .name("MARKET_ROSTOV")
                        .status(PartnerStatus.ACTIVE)
                        .build(),
                PartnerResponse.newBuilder()
                        .id(3)
                        .partnerType(PartnerType.FULFILLMENT)
                        .name("MARKET_TOM")
                        .status(PartnerStatus.ACTIVE)
                        .build()
        );

        List<FulfillmentPartner> fulfillmentPartners = PartnerResponseConverter.convert(partnerResponses);

        assertNotNull(fulfillmentPartners);
        assertEquals(2, fulfillmentPartners.size());

        FulfillmentPartner fulfillmentPartner1 = fulfillmentPartners.get(0);
        assertEquals(2, fulfillmentPartner1.getId());
        assertEquals(LmsPartnerType.FULFILLMENT, fulfillmentPartner1.getPartnerType());
        assertEquals("MARKET_ROSTOV", fulfillmentPartner1.getName());
        assertEquals(LmsPartnerStatus.ACTIVE, fulfillmentPartner1.getStatus());

        FulfillmentPartner fulfillmentPartner2 = fulfillmentPartners.get(1);
        assertEquals(3, fulfillmentPartner2.getId());
        assertEquals(LmsPartnerType.FULFILLMENT, fulfillmentPartner2.getPartnerType());
        assertEquals("MARKET_TOM", fulfillmentPartner2.getName());
        assertEquals(LmsPartnerStatus.ACTIVE, fulfillmentPartner2.getStatus());
    }

    @Test
    public void shouldReturnWithStatusUnknownIfPartnerResponseHasStatusNull() {
        PartnerResponse partnerResponse = PartnerResponse.newBuilder()
                .id(2)
                .partnerType(PartnerType.FULFILLMENT)
                .name("MARKET_ROSTOV")
                .status(null)
                .build();

        FulfillmentPartner fulfillmentPartner = PartnerResponseConverter.convert(partnerResponse);

        assertNotNull(fulfillmentPartner);
        assertEquals(LmsPartnerStatus.UNKNOWN, fulfillmentPartner.getStatus());
    }

    @Test
    public void shouldHaveStockSyncDisabledIfNull() {
        PartnerResponse partnerResponse = PartnerResponse.newBuilder()
                .id(2)
                .partnerType(null)
                .name("MARKET_ROSTOV")
                .status(PartnerStatus.ACTIVE)
                .build();

        FulfillmentPartner fulfillmentPartner = PartnerResponseConverter.convert(partnerResponse);
        assertNotNull(fulfillmentPartner);
        assertFalse(fulfillmentPartner.isSyncStockEnabled());
    }

    @Test
    public void shouldReturnWithPartnerTypeUnknownIfPartnerResponseHasPartnerTypeNull() {
        PartnerResponse partnerResponse = PartnerResponse.newBuilder()
                .id(2)
                .partnerType(null)
                .name("MARKET_ROSTOV")
                .status(PartnerStatus.ACTIVE)
                .build();

        FulfillmentPartner fulfillmentPartner = PartnerResponseConverter.convert(partnerResponse);

        assertNotNull(fulfillmentPartner);
        assertEquals(LmsPartnerType.UNKNOWN, fulfillmentPartner.getPartnerType());
    }
}
