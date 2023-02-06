package ru.yandex.market.adv.promo.mvc.promo.promo_import.controller;

import java.nio.charset.StandardCharsets;

import Market.DataCamp.DataCampPromo;
import Market.DataCamp.SyncAPI.SyncGetPromo;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.adv.promo.utils.CommonTestUtils;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.datacamp.stroller.model.GetPromoBatchRequestWithFilters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class MarketPromoImportControllerTest extends FunctionalTest {
    @Autowired
    private DataCampClient dataCampClient;

    @Test
    public void testSuccessCreation() {
        doReturn(SyncGetPromo.GetPromoBatchResponse.getDefaultInstance()).when(dataCampClient)
                .getPromos(any(GetPromoBatchRequestWithFilters.class));
        doNothing().when(dataCampClient).addPromo(any());
        String requestBody = CommonTestUtils.getResource(this.getClass(), "success-create-discount-request.json");
        ResponseEntity<String> response = sendCreatePromoRequest(requestBody);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(dataCampClient, times(1)).addPromo(any(DataCampPromo.PromoDescription.class));
    }

    @Test
    public void testPromoAlreadyExists() {
        SyncGetPromo.GetPromoBatchResponse fakePromo = SyncGetPromo.GetPromoBatchResponse.newBuilder()
                .setPromos(DataCampPromo.PromoDescriptionBatch.newBuilder()
                        .addPromo(DataCampPromo.PromoDescription.newBuilder()
                                .setPrimaryKey(
                                        DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                                .setPromoId("Fake Promo")
                                )
                        )
                )
                .build();
        doReturn(fakePromo).when(dataCampClient)
                .getPromos(any(GetPromoBatchRequestWithFilters.class));
        doNothing().when(dataCampClient).addPromo(any());
        String requestBody = CommonTestUtils.getResource(this.getClass(), "success-create-discount-request.json");
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> sendCreatePromoRequest(requestBody)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        String errorMessage = "Акция с promoId: #1 уже присутствует в Акционном Хранилище.";
        String responseBody = new String(exception.getResponseBodyAsByteArray(), StandardCharsets.UTF_8);
        assertEquals(((JsonObject) JsonTestUtil.parseJson(responseBody)).get("message").getAsString(), errorMessage);
        verify(dataCampClient, times(0)).addPromo(any(DataCampPromo.PromoDescription.class));
    }

    @Test
    public void testFailedSendToPromoStorageWhileCreation() {
        doReturn(SyncGetPromo.GetPromoBatchResponse.getDefaultInstance()).when(dataCampClient)
                .getPromos(any(GetPromoBatchRequestWithFilters.class));
        doThrow(new RuntimeException("alarma!")).when(dataCampClient).addPromo(any());
        String requestBody = CommonTestUtils.getResource(this.getClass(), "success-create-discount-request.json");
        HttpServerErrorException exception = Assertions.assertThrows(
                HttpServerErrorException.class,
                () -> sendCreatePromoRequest(requestBody)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INSUFFICIENT_STORAGE);
        String errorMessage = "Не удалось сохранить акцию в Акционное хранилище. alarma!";
        String responseBody = new String(exception.getResponseBodyAsByteArray(), StandardCharsets.UTF_8);
        assertEquals(((JsonObject) JsonTestUtil.parseJson(responseBody)).get("message").getAsString(), errorMessage);
        verify(dataCampClient, times(1)).addPromo(any(DataCampPromo.PromoDescription.class));
    }

    @Test
    public void testFailedValidationWhileCreating() {
        doReturn(SyncGetPromo.GetPromoBatchResponse.getDefaultInstance()).when(dataCampClient)
                .getPromos(any(GetPromoBatchRequestWithFilters.class));
        doNothing().when(dataCampClient).addPromo(any());
        String requestBody = CommonTestUtils.getResource(this.getClass(), "failed-cashback-validation-request.json");
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> sendCreatePromoRequest(requestBody)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        String errorMessage = "Механика {PARTNER_STANDART_CASHBACK} недоступна в Партнерском Интерфейсе.\n" +
                "Дата старта акции не может быть позже даты окончания акции [start_date={11}, end_date={10}].\n" +
                "Дата публикации должна быть не позже даты старта [publish_date={12}, start_date={11}].";
        String responseBody = new String(exception.getResponseBodyAsByteArray(), StandardCharsets.UTF_8);
        assertEquals(((JsonObject) JsonTestUtil.parseJson(responseBody)).get("message").getAsString(), errorMessage);
        verify(dataCampClient, times(0)).addPromo(any(DataCampPromo.PromoDescription.class));
    }

    @Test
    public void testSuccessUpdate() {
        DataCampPromo.PromoDescription previousDescription = createPromoDescription();
        doReturn(SyncGetPromo.GetPromoBatchResponse.newBuilder()
                .setPromos(DataCampPromo.PromoDescriptionBatch.newBuilder()
                        .addPromo(previousDescription)
                )
                .build()
        ).when(dataCampClient).getPromos(any(GetPromoBatchRequestWithFilters.class));
        doNothing().when(dataCampClient).addPromo(any());
        String requestBody = CommonTestUtils.getResource(this.getClass(), "success-create-discount-request.json");
        sendUpdatePromoRequest(requestBody);
        ArgumentCaptor<DataCampPromo.PromoDescription> captor =
                ArgumentCaptor.forClass(DataCampPromo.PromoDescription.class);
        verify(dataCampClient, times(1)).addPromo(captor.capture());
        DataCampPromo.PromoDescription promo = captor.getValue();
        assertThat(promo.getConstraints().getOffersMatchingRulesList())
                .singleElement()
                .satisfies(offersMatchingRule -> assertThat(offersMatchingRule)
                        .extracting(DataCampPromo.PromoConstraints.OffersMatchingRule::getCategoryRestriction)
                        .extracting(DataCampPromo.PromoConstraints.OffersMatchingRule.CategoryRestriction::getPromoCategoryList)
                        .satisfies(categoryList -> assertThat(categoryList)
                                .containsExactlyInAnyOrder(
                                        DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory.newBuilder()
                                                .setId(1)
                                                .setMinDiscount(10)
                                                .build(),
                                        DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory.newBuilder()
                                                .setId(2)
                                                .setMinDiscount(15)
                                                .build(),
                                        DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory.newBuilder()
                                                .setId(4)
                                                .setMinDiscount(40)
                                                .build()
                                )
                        )
                );
    }

    @Test
    public void testPromoNotExists() {
        doReturn(SyncGetPromo.GetPromoBatchResponse.getDefaultInstance()).when(dataCampClient)
                .getPromos(any(GetPromoBatchRequestWithFilters.class));
        doNothing().when(dataCampClient).addPromo(any());
        String requestBody = CommonTestUtils.getResource(this.getClass(), "success-create-discount-request.json");
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> sendUpdatePromoRequest(requestBody)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        String errorMessage = "Акция с promoId: #1 отсутствует в Акционном Хранилище.";
        String responseBody = new String(exception.getResponseBodyAsByteArray(), StandardCharsets.UTF_8);
        assertEquals(((JsonObject) JsonTestUtil.parseJson(responseBody)).get("message").getAsString(), errorMessage);
        verify(dataCampClient, times(0)).addPromo(any(DataCampPromo.PromoDescription.class));
    }

    @Test
    public void testFailedSendToPromoStorageWhileUpdating() {
        DataCampPromo.PromoDescription previousDescription = createPromoDescription();
        doReturn(SyncGetPromo.GetPromoBatchResponse.newBuilder()
                .setPromos(DataCampPromo.PromoDescriptionBatch.newBuilder()
                        .addPromo(previousDescription)
                )
                .build()
        ).when(dataCampClient).getPromos(any(GetPromoBatchRequestWithFilters.class));
        doThrow(new RuntimeException("alarma!")).when(dataCampClient).addPromo(any());
        String requestBody = CommonTestUtils.getResource(this.getClass(), "success-create-discount-request.json");
        HttpServerErrorException exception = Assertions.assertThrows(
                HttpServerErrorException.class,
                () -> sendUpdatePromoRequest(requestBody)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INSUFFICIENT_STORAGE);
        String errorMessage = "Не удалось сохранить акцию в Акционное хранилище. alarma!";
        String responseBody = new String(exception.getResponseBodyAsByteArray(), StandardCharsets.UTF_8);
        assertEquals(((JsonObject) JsonTestUtil.parseJson(responseBody)).get("message").getAsString(), errorMessage);
        verify(dataCampClient, times(1)).addPromo(any(DataCampPromo.PromoDescription.class));
    }

    @Test
    public void testFailedValidationWhileUpdating() {
        DataCampPromo.PromoDescription previousDescription = createPromoDescription();
        previousDescription = previousDescription.toBuilder()
                .setConstraints(previousDescription.getConstraints().toBuilder()
                        .setEndDate(5)
                )
                .build();
        doReturn(SyncGetPromo.GetPromoBatchResponse.newBuilder()
                .setPromos(DataCampPromo.PromoDescriptionBatch.newBuilder()
                        .addPromo(previousDescription)
                )
                .build()
        ).when(dataCampClient).getPromos(any(GetPromoBatchRequestWithFilters.class));
        doNothing().when(dataCampClient).addPromo(any());
        String requestBody = CommonTestUtils.getResource(this.getClass(), "failed-discount-validation-request.json");
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> sendUpdatePromoRequest(requestBody)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        String errorMessage = "Присутствуют повторяющиеся листовые категории: {2}.\n" +
                "Для категории {4} не указан процент скидки.\n" +
                "Дата старта акции не может быть позже даты окончания акции [start_date={11}, end_date={10}].\n" +
                "Дата публикации должна быть не позже даты старта [publish_date={12}, start_date={11}].\n" +
                "Дата окончания промо не может быть сдвинута на более позднее время после публикации промо в ПИ.";
        String responseBody = new String(exception.getResponseBodyAsByteArray(), StandardCharsets.UTF_8);
        assertEquals(((JsonObject) JsonTestUtil.parseJson(responseBody)).get("message").getAsString(), errorMessage);
        verify(dataCampClient, times(0)).addPromo(any(DataCampPromo.PromoDescription.class));
    }

    private ResponseEntity<String> sendCreatePromoRequest(
            String body
    ) {
        return FunctionalTestHelper.post(
                baseUrl() + "/promo/import/market/create",
                new HttpEntity<>(body, getDefaultHeaders())
        );
    }

    private void sendUpdatePromoRequest(
            String body
    ) {
        FunctionalTestHelper.put(baseUrl() + "/promo/import/market/update", new HttpEntity<>(body, getDefaultHeaders()));
    }

    private DataCampPromo.PromoDescription createPromoDescription() {
        return DataCampPromo.PromoDescription.newBuilder()
                .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                        .setPromoId("#1")
                )
                .setPromoGeneralInfo(DataCampPromo.PromoGeneralInfo.newBuilder()
                        .setPromoType(DataCampPromo.PromoType.DIRECT_DISCOUNT)
                )
                .setAdditionalInfo(
                        DataCampPromo.PromoAdditionalInfo.newBuilder()
                                .setPublishDatePi(2)
                                .setSendPromoPi(true)
                )
                .setConstraints(DataCampPromo.PromoConstraints.newBuilder()
                        .addOffersMatchingRules(DataCampPromo.PromoConstraints.OffersMatchingRule.newBuilder()
                                .setCategoryRestriction(DataCampPromo.PromoConstraints.OffersMatchingRule.CategoryRestriction.newBuilder()
                                        .addPromoCategory(DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory.newBuilder()
                                                .setId(1)
                                                .setMinDiscount(10)
                                        )
                                        .addPromoCategory(DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory.newBuilder()
                                                .setId(2)
                                                .setMinDiscount(15)
                                        )
                                        .addPromoCategory(DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory.newBuilder()
                                                .setId(4)
                                                .setMinDiscount(40)
                                        )
                                )
                                .setOrigionalCategoryRestriction(DataCampPromo.PromoConstraints.OffersMatchingRule.OriginalCategoryRestriction.newBuilder()
                                        .addIncludeCategegoryRestriction(DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory.newBuilder()
                                                .setId(1)
                                                .setMinDiscount(10)
                                        )
                                        .addIncludeCategegoryRestriction(DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory.newBuilder()
                                                .setId(3)
                                                .setMinDiscount(15)
                                        )
                                )
                        )
                        .setStartDate(2)
                        .setEndDate(11)
                )
                .build();
    }
}
