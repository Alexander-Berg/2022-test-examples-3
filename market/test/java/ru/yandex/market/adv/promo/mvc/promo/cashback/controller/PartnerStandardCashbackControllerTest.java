package ru.yandex.market.adv.promo.mvc.promo.cashback.controller;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import Market.DataCamp.DataCampPromo;
import Market.DataCamp.SyncAPI.SyncGetPromo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.adv.promo.service.loyalty.client.LoyaltyClient;
import ru.yandex.market.adv.promo.service.loyalty.dto.Group;
import ru.yandex.market.adv.promo.service.loyalty.dto.StandardPromosResponse;
import ru.yandex.market.adv.promo.utils.CommonTestUtils;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.datacamp.stroller.model.PromoDatacampRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.adv.promo.utils.CashbackMechanicTestUtils.createStandardCashback;
import static ru.yandex.market.adv.promo.utils.CommonTestUtils.getResource;

class PartnerStandardCashbackControllerTest extends FunctionalTest {

    @Autowired
    private DataCampClient dataCampClient;

    @Autowired
    private LoyaltyClient loyaltyClient;

    @BeforeEach
    void before() {
        doReturn(SyncGetPromo.GetPromoBatchResponse.getDefaultInstance())
                .when(dataCampClient).getPromos(any(PromoDatacampRequest.class));
    }

    @Test
    void getStandardCashbackTest() {
        long businessId = 111;
        long partnerId = 1;
        PromoDatacampRequest request = new PromoDatacampRequest.Builder(businessId)
                        .withPartnerId(partnerId)
                        .withPromoType(Set.of(DataCampPromo.PromoType.PARTNER_STANDART_CASHBACK))
                        .build();
        SyncGetPromo.GetPromoBatchResponse standardCashback = SyncGetPromo.GetPromoBatchResponse.newBuilder()
                .setPromos(
                        DataCampPromo.PromoDescriptionBatch.newBuilder()
                                .addPromo(createStandardCashback("standard_cb_promoId", (int) businessId))
                )
                .build();
        doReturn(standardCashback)
                .when(dataCampClient).getPromos(ArgumentMatchers.eq(request));

        ResponseEntity<String> response = sendGetStandardCashbackRequest(partnerId, businessId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        String expected = getResource(this.getClass(), "getStandardCashbackTest.json");
        JSONAssert.assertEquals(expected, response.getBody(), false);
    }

    @Test
    void getStandardCashback_noPromoTest() {
        long businessId = 111;
        long partnerId = 1;

        ResponseEntity<String> response = sendGetStandardCashbackRequest(partnerId, businessId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JSONAssert.assertEquals("{}", response.getBody(), false);
    }

    @Test
    void getStandardCashback_severalPromosTest() {
        long businessId = 111;
        long partnerId = 1;
        PromoDatacampRequest request = new PromoDatacampRequest.Builder(businessId)
                .withPartnerId(partnerId)
                .withPromoType(Set.of(DataCampPromo.PromoType.PARTNER_STANDART_CASHBACK))
                .build();
        SyncGetPromo.GetPromoBatchResponse standardCashback = SyncGetPromo.GetPromoBatchResponse.newBuilder()
                .setPromos(
                        DataCampPromo.PromoDescriptionBatch.newBuilder()
                                .addPromo(createStandardCashback("standard_cb_promoId_1", (int) businessId))
                                .addPromo(createStandardCashback("standard_cb_promoId_2", (int) businessId))
                )
                .build();
        doReturn(standardCashback)
                .when(dataCampClient).getPromos(ArgumentMatchers.eq(request));

        Assertions.assertThrows(
                HttpServerErrorException.class,
                () -> sendGetStandardCashbackRequest(partnerId, businessId)
        );
    }

    @Test
    void createStandardCashbackTest() {
        long businessId = 111;
        long partnerId = 1;

        doReturn(
                new StandardPromosResponse(
                        0,
                        List.of(
                                new Group("cehac", 1, 1, 8, 0, Collections.emptyList()),
                                new Group("diy", 2, 1, 15, 0, Collections.emptyList()),
                                new Group("default", 5, 1, 30, 0, Collections.emptyList())
                        )
                )
        ).when(loyaltyClient).getLoyaltyStandardPromos(eq(0));

        String requestBody = CommonTestUtils.getResource(this.getClass(), "createStandardCashbackTest_request.json");
        ResponseEntity<String> response = sendCreateStandardCashbackRequest(partnerId, businessId, requestBody);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        ArgumentCaptor<DataCampPromo.PromoDescription> captor =
                ArgumentCaptor.forClass(DataCampPromo.PromoDescription.class);
        verify(dataCampClient, times(1)).addPromo(captor.capture());
        DataCampPromo.PromoDescription createdPromoDescription = captor.getValue();

        assertNotNull(createdPromoDescription);
        assertEquals("12345_PSC_12345", createdPromoDescription.getPrimaryKey().getPromoId());
        assertEquals(
                DataCampPromo.PromoType.PARTNER_STANDART_CASHBACK,
                createdPromoDescription.getPromoGeneralInfo().getPromoType()
        );
        assertTrue(createdPromoDescription.getMechanicsData().hasPartnerStandartCashback());

        DataCampPromo.PromoMechanics.PartnerStandartCashback mechanic =
                createdPromoDescription.getMechanicsData().getPartnerStandartCashback();
        assertTrue(mechanic.hasMarketTariffsVersionId());
        assertEquals(0, mechanic.getMarketTariffsVersionId());
        assertEquals(3, mechanic.getStandartGroupCount());
        assertThat(mechanic.getStandartGroupList()).containsExactlyInAnyOrderElementsOf(
                List.of(
                        DataCampPromo.PromoMechanics.PartnerStandartCashback.StandartGroup.newBuilder()
                                .setCodeName("cehac")
                                .setValue(2)
                                .build(),
                        DataCampPromo.PromoMechanics.PartnerStandartCashback.StandartGroup.newBuilder()
                                .setCodeName("diy")
                                .setValue(5)
                                .build(),
                        DataCampPromo.PromoMechanics.PartnerStandartCashback.StandartGroup.newBuilder()
                                .setCodeName("default")
                                .setValue(30)
                                .build()
                )
        );
    }

    @Test
    void updateStandardCashbackTest() {
        long businessId = 111;
        long partnerId = 1;
        String promoId = "12345_PSC_12345";

        doReturn(
                new StandardPromosResponse(
                        0,
                        List.of(
                                new Group("cehac", 1, 1, 8, 0, Collections.emptyList()),
                                new Group("diy", 2, 1, 15, 0, Collections.emptyList()),
                                new Group("default", 5, 1, 30, 0, Collections.emptyList())
                        )
                )
        ).when(loyaltyClient).getLoyaltyStandardPromos(eq(0));

        PromoDatacampRequest request = new PromoDatacampRequest.Builder(businessId)
                .withPartnerId(partnerId)
                .withPromoType(Set.of(DataCampPromo.PromoType.PARTNER_STANDART_CASHBACK))
                .build();
        SyncGetPromo.GetPromoBatchResponse standardCashback = SyncGetPromo.GetPromoBatchResponse.newBuilder()
                .setPromos(
                        DataCampPromo.PromoDescriptionBatch.newBuilder()
                                .addPromo(createStandardCashback(promoId, (int) businessId))
                )
                .build();
        doReturn(standardCashback)
                .when(dataCampClient).getPromos(ArgumentMatchers.eq(request));

        String requestBody = CommonTestUtils.getResource(this.getClass(), "updateStandardCashbackTest_request.json");
        sendUpdateStandardCashbackRequest(partnerId, businessId, requestBody);

        ArgumentCaptor<DataCampPromo.PromoDescription> captor =
                ArgumentCaptor.forClass(DataCampPromo.PromoDescription.class);
        verify(dataCampClient, times(1)).addPromo(captor.capture());
        DataCampPromo.PromoDescription updatedPromoDescription = captor.getValue();

        assertNotNull(updatedPromoDescription);
        assertEquals(promoId, updatedPromoDescription.getPrimaryKey().getPromoId());
        assertFalse(updatedPromoDescription.hasPromoGeneralInfo());
        assertFalse(updatedPromoDescription.hasConstraints());
        assertFalse(updatedPromoDescription.hasAdditionalInfo());
        assertTrue(updatedPromoDescription.getMechanicsData().hasPartnerStandartCashback());

        DataCampPromo.PromoMechanics.PartnerStandartCashback mechanic =
                updatedPromoDescription.getMechanicsData().getPartnerStandartCashback();
        assertTrue(mechanic.hasMarketTariffsVersionId());
        assertEquals(0, mechanic.getMarketTariffsVersionId());
        assertEquals(3, mechanic.getStandartGroupCount());
        assertThat(mechanic.getStandartGroupList()).containsExactlyInAnyOrderElementsOf(
                List.of(
                        DataCampPromo.PromoMechanics.PartnerStandartCashback.StandartGroup.newBuilder()
                                .setCodeName("cehac")
                                .setValue(2)
                                .build(),
                        DataCampPromo.PromoMechanics.PartnerStandartCashback.StandartGroup.newBuilder()
                                .setCodeName("diy")
                                .setValue(5)
                                .build(),
                        DataCampPromo.PromoMechanics.PartnerStandartCashback.StandartGroup.newBuilder()
                                .setCodeName("default")
                                .setValue(30)
                                .build()
                )
        );
    }

    @Test
    void updateStandardCashback_NoPromoTest() {
        long businessId = 111;
        long partnerId = 1;

        String requestBody = CommonTestUtils.getResource(this.getClass(), "updateStandardCashbackTest_request.json");
        Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> sendUpdateStandardCashbackRequest(partnerId, businessId, requestBody)
        );
    }

    @Test
    void updateStandardCashback_AnotherPromoTest() {
        long businessId = 111;
        long partnerId = 1;
        String promoId = "12345_PSC_12345";

        PromoDatacampRequest request = new PromoDatacampRequest.Builder(businessId)
                .withPartnerId(partnerId)
                .withPromoType(Set.of(DataCampPromo.PromoType.PARTNER_STANDART_CASHBACK))
                .build();
        SyncGetPromo.GetPromoBatchResponse standardCashback = SyncGetPromo.GetPromoBatchResponse.newBuilder()
                .setPromos(
                        DataCampPromo.PromoDescriptionBatch.newBuilder()
                                .addPromo(createStandardCashback(promoId, (int) businessId))
                )
                .build();
        doReturn(standardCashback)
                .when(dataCampClient).getPromos(ArgumentMatchers.eq(request));

        String requestBody =
                CommonTestUtils.getResource(this.getClass(), "updateStandardCashback_AnotherPromoTest.json");
        Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> sendUpdateStandardCashbackRequest(partnerId, businessId, requestBody)
        );
    }

    private ResponseEntity<String> sendGetStandardCashbackRequest(long partnerId, long businessId) {
        return FunctionalTestHelper.get(
                baseUrl() + "/partner/promo/cashback/standard?partnerId=" + partnerId + "&businessId=" + businessId
        );
    }

    private ResponseEntity<String> sendCreateStandardCashbackRequest(long partnerId, long businessId, String body) {
        return FunctionalTestHelper.post(
                baseUrl() + "/partner/promo/cashback/standard?partnerId=" + partnerId + "&businessId=" + businessId,
                new HttpEntity<>(body, getDefaultHeaders())
        );
    }

    private void sendUpdateStandardCashbackRequest(long partnerId, long businessId, String body) {
        FunctionalTestHelper.put(
                baseUrl() + "/partner/promo/cashback/standard?partnerId=" + partnerId + "&businessId=" + businessId,
                new HttpEntity<>(body, getDefaultHeaders())
        );
    }
}
