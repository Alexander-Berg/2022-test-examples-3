package ru.yandex.travel.orders.services.partners;

import java.time.Instant;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.travel.hotels.administrator.export.proto.HotelAgreement;
import ru.yandex.travel.hotels.proto.EPartnerId;
import ru.yandex.travel.orders.cache.HotelAgreementDictionary;
import ru.yandex.travel.orders.commons.proto.EVat;
import ru.yandex.travel.orders.services.finances.HotelAgreementService;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class HotelAgreementServiceTest {

    private final String HOTEL_ID = "123";
    private final EPartnerId PARTNER_ID = EPartnerId.PI_TRAVELLINE;
    private final Instant INSTANT = Instant.now();

    private HotelAgreementService hotelAgreementService;
    private HotelAgreementDictionary hotelAgreementDictionary;

    @Before
    public void init() {
        hotelAgreementDictionary = Mockito.mock(HotelAgreementDictionary.class);
        hotelAgreementService = new HotelAgreementService(hotelAgreementDictionary);
    }

    @Test
    public void testNoAgreementFromDictionary() {
        when(hotelAgreementDictionary.findAgreementByHotelIdAndTimestamp(eq(PARTNER_ID), eq(HOTEL_ID), eq(INSTANT)))
                .thenReturn(null);
        try {
            hotelAgreementService.getAgreementForTimestamp(HOTEL_ID, PARTNER_ID, INSTANT);
            Assert.fail();
        } catch (NullPointerException e) {
            //NPE should be thrown
        }
    }

    @Test
    public void testAgreementFromDictionary() {
        HotelAgreement mockAgreement = HotelAgreement.newBuilder()
                .setId(1L)
                .setHotelId(HOTEL_ID)
                .setPartnerId(PARTNER_ID)
                .setInn("inn")
                .setFinancialClientId(111L)
                .setFinancialContractId(333L)
                .setAgreementStartDate(10L)
                .setEnabled(true)
                .setOrderConfirmedRate("0.1")
                .setOrderRefundedRate("0.14")
                .setVatType(EVat.VAT_NONE)
                .setSendEmptyOrdersReport(true)
                .build();
        when(hotelAgreementDictionary.findAgreementByHotelIdAndTimestamp(eq(PARTNER_ID), eq(HOTEL_ID), eq(INSTANT)))
                .thenReturn(mockAgreement);
        HotelAgreement hotelAgreement = hotelAgreementService.getAgreementForTimestamp(HOTEL_ID, PARTNER_ID, INSTANT);
        Assert.assertEquals(hotelAgreement, mockAgreement);
    }
}
