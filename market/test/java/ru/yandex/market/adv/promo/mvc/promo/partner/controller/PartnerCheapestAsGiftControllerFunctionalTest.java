package ru.yandex.market.adv.promo.mvc.promo.partner.controller;

import java.nio.charset.StandardCharsets;

import Market.DataCamp.DataCampPromo;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.adv.promo.utils.CommonTestUtils;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.promo.CommitPromoOffersResponse;
import ru.yandex.market.core.promo.CommitPromoOffersResult;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DbUnitDataSet(before = "PartnerCheapestAsGiftControllerFunctionalTest/PartnerCheapestAsGiftControllerFunctionalTest.before.csv")
public class PartnerCheapestAsGiftControllerFunctionalTest extends FunctionalTest {
    @Autowired
    private DataCampClient dataCampClient;

    @Autowired
    private MbiApiClient mbiApiClient;

    @BeforeEach
    void before() {
        doNothing().when(dataCampClient).addPromo(any(DataCampPromo.PromoDescription.class));
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerCheapestAsGiftControllerFunctionalTest/createCheapestAsGiftTest.before.csv",
            after = "PartnerCheapestAsGiftControllerFunctionalTest/createCheapestAsGiftTest.after.csv"
    )
    void createPartnerCheapestAsGiftPromoTest() {

        String requestBody = CommonTestUtils.getResource(this.getClass(), "good-create-request.json");
        doReturn(new CommitPromoOffersResponse(CommitPromoOffersResult.COMMITTED)).when(mbiApiClient).commitPromoOffers(any());
        ResponseEntity<String> response = sendCreateCheapestAsGiftRequest(1, 1, requestBody);

        verify(dataCampClient, times(1)).addPromo(any(DataCampPromo.PromoDescription.class));
        Assertions.assertEquals(response.getStatusCode(), HttpStatus.OK);
        Assertions.assertEquals(response.getBody(), "{\"promoId\":\"1_CAG_1626275850\"}");
    }

    @Test
    @DbUnitDataSet(after = "PartnerCheapestAsGiftControllerFunctionalTest/partner-participated-promos-empty.after.csv")
    void createPartnerCheapestAsGiftPromoIncorrectBundleSizeTest() {
        checkRequestWithError(
                "incorrect-bundle-size-request.json",
                "bundleSize: Количество товаров в наборе не может быть меньше 2"
        );
    }

    @Test
    @DbUnitDataSet(after = "PartnerCheapestAsGiftControllerFunctionalTest/partner-participated-promos-empty.after.csv")
    void createPartnerCheapestAsGiftPromoIncorrectPromoIdTest() {
        checkRequestWithError(
                "incorrect-promo-id-request.json",
                "promoId: Идентификатор акции должен быть указан"
        );
    }

    private void checkRequestWithError(
            String requestBodyFileName,
            String errorMessage
    ) {
        String requestBody = CommonTestUtils.getResource(this.getClass(), requestBodyFileName);
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> sendCreateCheapestAsGiftRequest(1, 1, requestBody)
        );
        String responseBody = new String(exception.getResponseBodyAsByteArray(), StandardCharsets.UTF_8);
        assertEquals(((JsonObject) JsonTestUtil.parseJson(responseBody)).get("message").getAsString(), errorMessage);
        assertEquals(exception.getStatusCode(), HttpStatus.BAD_REQUEST);
        verify(dataCampClient, times(0)).addPromo(any(DataCampPromo.PromoDescription.class));
    }

    private ResponseEntity<String> sendCreateCheapestAsGiftRequest(
            long partnerId,
            long businessId,
            String body
    ) {
        return FunctionalTestHelper.post(baseUrl() + "/partner/promo/cheapest-as-gift?partnerId=" + partnerId + "&businessId=" + businessId, new HttpEntity<>(body, getDefaultHeaders()));
    }
}
