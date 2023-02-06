package ru.yandex.market.adv.promo.mvc.promo.partner.controller;

import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;

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

@DbUnitDataSet(before = "PartnerPromocodeControllerTest/PartnerPromocodeControllerFunctionalTest.before.csv")
class PartnerPromocodeControllerTest extends FunctionalTest {
    @Autowired
    private DataCampClient dataCampClient;

    @Autowired
    private MbiApiClient mbiApiClient;

    @BeforeEach
    void before() {
        doNothing().when(dataCampClient).addPromo(any(DataCampPromo.PromoDescription.class));
    }

    @Test
    @DbUnitDataSet(after = "PartnerPromocodeControllerTest/partner-participated-promos-empty.after.csv")
    void createPromocode_emptyPromocodeValidationTest() {
        createPromocodeRequestFieldsCorrectnessValidation(
                "promo-with-empty-promocode.json",
                "promocode: Текст промокода не может быть пуст"
        );
    }

    @Test
    @DbUnitDataSet(after = "PartnerPromocodeControllerTest/partner-participated-promos-empty.after.csv")
    void createPromocode_negativeBudgetLimitValidationTest() {
        createPromocodeRequestFieldsCorrectnessValidation(
                "promo-with-negative-budget-limit.json",
                "budgetLimit: Бюджет по акции обязан быть положительным числом"
        );
    }

    @Test
    @DbUnitDataSet(after = "PartnerPromocodeControllerTest/partner-participated-promos-empty.after.csv")
    void createPromocode_emptyFieldValidationTest() {
        createPromocodeRequestFieldsCorrectnessValidation(
                "promo-with-empty-field.json",
                "endDate: Дата завершения акции должна быть указана"
        );
    }

    @Test
    @DbUnitDataSet(after = "PartnerPromocodeControllerTest/partner-participated-promos-empty.after.csv")
    void createPromocode_emptyValidationIdTest() {
        createPromocodeRequestFieldsCorrectnessValidation(
                "promo-with-empty-last-validation-id.json",
                "lastValidationId: Идентификатор валидации должен быть указан"
        );
    }

    @Test
    @DbUnitDataSet(after = "PartnerPromocodeControllerTest/partner-participated-promos-empty.after.csv")
    void createPromocode_incorrectDatesValidationTest() {
        createPromocodeRequestFieldsCorrectnessValidation(
                "promo-with-incorrect-dates.json",
                "Promo dates are incorrect [start=2071-03-01T00:00, end=2071-02-01T23:59:59]"
        );
    }

    @Test
    @DbUnitDataSet(after = "PartnerPromocodeControllerTest/partner-participated-promos-empty.after.csv")
    void createPromocode_incorrectPercentageDiscountValidationTest() {
        createPromocodeRequestFieldsCorrectnessValidation(
                "promo-with-incorrect-percentage-discount.json",
                "Incorrect percentage discount [discountValue=180]"
        );
    }

    @Test
    @DbUnitDataSet(after = "PartnerPromocodeControllerTest/partner-participated-promos-empty.after.csv")
    void createPromocode_budgetLessThanDiscount() {
        createPromocodeRequestFieldsCorrectnessValidation(
                "promo-with-budget-less-than-discount.json",
                "Budget limit is less than discount value [discountValue=500, budgetLimit=450]"
        );
    }

    @Test
    @DbUnitDataSet(after = "PartnerPromocodeControllerTest/partner-participated-promos-empty.after.csv")
    void createPromocode_severalIncorrectFilledFieldsTest() {
        String messageError = new StringJoiner("; ")
                .add("budgetLimit: Бюджет по акции обязан быть положительным числом")
                .add("discountValue: Значение скидки должно быть указано")
                .add("lastValidationId: Идентификатор валидации должен быть указан")
                .add("promocode: Текст промокода не может быть пуст")
                .add("startDate: Дата начала акции должна быть указана")
                .toString();
        createPromocodeRequestFieldsCorrectnessValidation(
                "promo-with-several-incorrect-filled-fields.json",
                messageError
        );
    }

    private void createPromocodeRequestFieldsCorrectnessValidation(
            String requestBodyFileName,
            String errorMessage
    ) {
        String requestBody = CommonTestUtils.getResource(this.getClass(), requestBodyFileName);
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> sendCreatePromocodeRequest(1, 1, requestBody)
        );
        String responseBody = new String(exception.getResponseBodyAsByteArray(), StandardCharsets.UTF_8);
        assertEquals(((JsonObject) JsonTestUtil.parseJson(responseBody)).get("message").getAsString(), errorMessage);
        assertEquals(exception.getStatusCode(), HttpStatus.BAD_REQUEST);
        verify(dataCampClient, times(0)).addPromo(any(DataCampPromo.PromoDescription.class));
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerPromocodeControllerTest/createPromocodeTest.before.csv",
            after = "PartnerPromocodeControllerTest/createPromocodeTest.after.csv"
    )
    void createPromocodeTest() {
        long partnerId = 1;
        long businessId = 1;
        String requestBody = CommonTestUtils.getResource(this.getClass(), "correctPromocodeRequest.json");

        doReturn(new CommitPromoOffersResponse(CommitPromoOffersResult.COMMITTED))
                .when(mbiApiClient)
                .commitPromoOffers(any());
        ResponseEntity<String> response = sendCreatePromocodeRequest(partnerId, businessId, requestBody);

        verify(dataCampClient, times(1)).addPromo(any(DataCampPromo.PromoDescription.class));
        assertEquals(response.getBody(), "{\"promoId\":\"1_AERSVYDR\"}");
    }

    @Test
    @DbUnitDataSet(after = "PartnerPromocodeControllerTest/partner-participated-promos-empty.after.csv")
    void notCompletedValidationTest() {
        doReturn(new CommitPromoOffersResponse(CommitPromoOffersResult.NOT_COMPLETED))
                .when(mbiApiClient)
                .commitPromoOffers(any());
        createPromocodeRequestFieldsCorrectnessValidation(
                "correctPromocodeRequest.json",
                "Validation is incorrect [validationId=validation_id reason=NOT_COMPLETED]"
        );
    }

    private ResponseEntity<String> sendCreatePromocodeRequest(
            long partnerId,
            long businessId,
            String body
    ) {
        return FunctionalTestHelper.post(
                baseUrl() + "/partner/promo/promocode?partnerId=" + partnerId + "&businessId=" + businessId,
                new HttpEntity<>(body, getDefaultHeaders())
        );
    }
}
