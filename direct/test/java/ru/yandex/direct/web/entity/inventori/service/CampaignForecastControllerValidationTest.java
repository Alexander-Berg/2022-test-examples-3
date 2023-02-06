package ru.yandex.direct.web.entity.inventori.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.direct.web.core.entity.inventori.model.CpmForecastRequest;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.entity.inventori.controller.InventoriController;
import ru.yandex.direct.web.entity.inventori.model.CpmForecastResponse;
import ru.yandex.direct.web.validation.model.WebDefect;
import ru.yandex.direct.web.validation.model.WebValidationResult;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.web.entity.inventori.service.InventoriTestWebDefects.webNoSuitableAdGroups;
import static ru.yandex.direct.web.testing.data.TestCpmForecastRequest.defaultRequest;

public class CampaignForecastControllerValidationTest extends CampaignForecastControllerTestBase {

    @Autowired
    private InventoriController controller;

    @Test
    public void getCampaignForecast_CampaignWithNoAdGroups() throws JsonProcessingException {
        CpmForecastRequest request = defaultRequest(campaignInfo.getCampaignId());

        checkSuccessResultWithErrors(request, webNoSuitableAdGroups(request.toString()));
    }

    @Test
    public void getCampaignForecast_AdGroupWithNoRetargetingCondition() throws JsonProcessingException {
        createUserProfileCpmAdGroup();
        CpmForecastRequest request = defaultRequest(campaignInfo.getCampaignId());

        checkSuccessResultWithErrors(request, webNoSuitableAdGroups(request.toString()));
    }

    private void checkSuccessResultWithErrors(CpmForecastRequest request, WebDefect... errors)
            throws JsonProcessingException {
        ResponseEntity<WebResponse> response = controller.getCampaignForecast(request, clientInfo.getLogin());

        CpmForecastResponse cpmForecastResponse = (CpmForecastResponse) response.getBody();
        WebValidationResult vr = cpmForecastResponse.validationResult();
        WebValidationResult expectedResult =
                new WebValidationResult().addErrors(errors);

        assertThat("Должен вернуться успешный ответ", response.getStatusCode().value(),
                is(CampaignForecastControllerValidationTest.SUCCESS_CODE));
        assertThat("Тело ответа должно быть пустым", cpmForecastResponse.getResult(), nullValue());
        Assertions.assertThat(vr).isEqualToComparingFieldByFieldRecursively(expectedResult);
    }
}
