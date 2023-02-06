package ru.yandex.market.partner.mvc.controller.v3.feed.validation;

import java.net.URISyntaxException;

import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Date: 12.11.2020
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
@DbUnitDataSet(before = "UnitedFeedValidationController/csv/result/before.csv")
public class UnitedFeedValidationResultControllerTest extends FunctionalTest {

    @CsvSource({
            "wrongCampaign,1001,42",
            "wrongValidation,1003,42",
            "wrongUploadIdShop,1003,2020",
            "wrongUploadIdSupplier,1007,2120"
    })
    @ParameterizedTest(name = "{0}")
    @DisplayName("Получение информации о процессе валидации фида. Ошибка.")
    void result_wrongParam_error(String test, String campaignId, String validationId) {
        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(buildValidationResultUrl(campaignId, validationId))
        );

        JsonTestUtil.assertResponseErrorMessage(exception, this.getClass(),
                "UnitedFeedValidationController/json/result/" + test + ".json");
    }

    @CsvSource({
            "1003,2014",
            "1005,2015",
            "1007,2016",
            "1007,2017",
            "1008,2018",
    })
    @ParameterizedTest(name = "campaignId = {0} - validationId = {1}")
    @DisplayName("Получение информации о процессе валидации фида.")
    void result_correctData_successful(String campaignId, String validationId) throws URISyntaxException {
        ResponseEntity<String> response = FunctionalTestHelper.get(buildValidationResultUrl(campaignId, validationId));
        JsonTestUtil.assertEquals(response, this.getClass(),
                "UnitedFeedValidationController/json/result/" + validationId + ".json");
    }

    private String buildValidationResultUrl(String campaignId, String validationId) throws URISyntaxException {
        return new URIBuilder(baseUrl)
                .setPathSegments("v3", campaignId, "feed", "validation", validationId)
                .build()
                .toString();
    }
}
