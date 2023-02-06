package ru.yandex.market.adv.promo.mvc.promo.cashback.controller;

import Market.DataCamp.DataCampPromo;
import Market.DataCamp.SyncAPI.SyncGetPromo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.datacamp.stroller.model.PromoDatacampRequest;

import static Market.DataCamp.DataCampPromo.PromoType.PARTNER_CUSTOM_CASHBACK;
import static Market.DataCamp.DataCampPromo.PromoType.PARTNER_STANDART_CASHBACK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.adv.promo.utils.CommonTestUtils.getResource;

class PartnerCashbackStatusControllerTest extends FunctionalTest {

    @Autowired
    private DataCampClient dataCampClient;

    @BeforeEach
    void before() {
        doReturn(SyncGetPromo.GetPromoBatchResponse.getDefaultInstance())
                .when(dataCampClient).getPromos(any(PromoDatacampRequest.class));
    }

    @Test
    void getCashbackStatusTest_noCashback() {
        doReturn(SyncGetPromo.GetPromoBatchResponse.getDefaultInstance())
                .when(dataCampClient).getPromos(any(PromoDatacampRequest.class));
        ResponseEntity<String> response = sendGetCashbackStatusRequest();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        String expected = getResource(this.getClass(), "getCashbackStatusTest_noCashback.json");
        JSONAssert.assertEquals(expected, response.getBody(), false);
    }

    @Test
    void getCashbackStatusTest_onlyStandardCashback() {
        getEverCaseCashbackStatusTest(
                PARTNER_STANDART_CASHBACK,
                "getCashbackStatusTest_onlyStandardCashback.json",
                false,
                false
        );
    }

    @Test
    void getCashbackStatusTest_onlyCustomCashback() {
        getEverCaseCashbackStatusTest(
                PARTNER_CUSTOM_CASHBACK,
                "getCashbackStatusTest_customCashback.json",
                false,
                false
        );
    }

    @Test
    void getCashbackStatusTest_enabledCustomCashback() {
        getEverCaseCashbackStatusTest(
                PARTNER_CUSTOM_CASHBACK,
                "getCashbackStatusTest_enabledCustomCashback.json",
                true,
                true
        );
    }

    private void getEverCaseCashbackStatusTest(
            DataCampPromo.PromoType promoType,
            String filename,
            boolean enabled,
            boolean onlyUnfinished
    ) {
        doReturn(
                stabGetPromoBatchResponse(promoType)
        ).when(dataCampClient).getPromos(
                ArgumentMatchers.argThat(
                        (ArgumentMatcher<PromoDatacampRequest>) request ->
                                request != null && request.getPromoType() != null &&
                                        request.getPromoType().contains(promoType) &&
                                        (!enabled && request.getEnabled() == null ||
                                                enabled && request.getEnabled()) &&
                                        (!onlyUnfinished && request.getOnlyUnfinished() == null ||
                                                onlyUnfinished && request.getOnlyUnfinished())
                )
        );

        ResponseEntity<String> response = sendGetCashbackStatusRequest();
        assertEquals(HttpStatus.OK, response.getStatusCode());

        String expected = getResource(this.getClass(), filename);
        JSONAssert.assertEquals(expected, response.getBody(), false);
    }

    private SyncGetPromo.GetPromoBatchResponse stabGetPromoBatchResponse(DataCampPromo.PromoType promoType) {
        return SyncGetPromo.GetPromoBatchResponse.newBuilder()
                .setPromos(
                        DataCampPromo.PromoDescriptionBatch.newBuilder()
                                .addPromo(
                                        DataCampPromo.PromoDescription.newBuilder()
                                                .setPrimaryKey(
                                                        DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                                                .setPromoId("promoId")
                                                )
                                                .setPromoGeneralInfo(
                                                        DataCampPromo.PromoGeneralInfo.newBuilder()
                                                                .setPromoType(promoType)
                                                )
                                )
                )
                .build();
    }

    private ResponseEntity<String> sendGetCashbackStatusRequest() {
        long partnerId = 1;
        long businessId = 2;
        return FunctionalTestHelper.get(
                baseUrl() + "/partner/promo/cashback/status?partnerId=" + partnerId + "&businessId=" + businessId
        );
    }
}

