package ru.yandex.direct.web.entity.inventori.service;

import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.direct.asynchttp.AsyncHttpExecuteException;
import ru.yandex.direct.common.TranslationService;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.currency.service.CurrencyRateService;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.inventori.InventoriClient;
import ru.yandex.direct.inventori.model.request.CampaignPredictionRequest;
import ru.yandex.direct.inventori.model.response.CampaignPredictionAvailableResponse;
import ru.yandex.direct.web.core.entity.inventori.service.InventoriService;
import ru.yandex.direct.web.core.entity.inventori.service.InventoriWebService;
import ru.yandex.direct.web.core.entity.inventori.service.InventoriWebValidationService;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource;
import ru.yandex.direct.web.entity.inventori.controller.InventoriController;
import ru.yandex.direct.web.validation.kernel.ValidationResultConversionService;
import ru.yandex.direct.web.validation.model.ValidationResponse;
import ru.yandex.direct.web.validation.model.WebValidationResult;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.web.testing.data.TestCpmForecastRequest.defaultRequest;

public class CampaignForecastControllerNegativeTest extends CampaignForecastControllerTestBase {

    @Autowired
    private TranslationService translationService;

    @Autowired
    private InventoriWebValidationService inventoriValidationService;

    @Autowired
    private InventoriWebService inventoriWebService;

    @Autowired
    private ValidationResultConversionService validationResultConversionService;

    @Autowired
    private DirectWebAuthenticationSource authenticationSource;

    @Autowired
    private CampaignForecastValidationService campaignForecastValidationService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private UserService userService;

    @Autowired
    private CurrencyRateService currencyRateService;

    @Autowired
    private InventoriService inventoriService;

    private InventoriClient client;
    private InventoriController controller;

    @Before
    @SuppressWarnings("Duplicates")
    public void before() {
        super.before();
        adGroupInfo = steps.adGroupSteps().createActiveCpmVideoAdGroup(campaignInfo);
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(clientInfo);
        Long campaignId = campaignInfo.getCampaignId();
        Long adGroupId = adGroupInfo.getAdGroupId();
        Long creativeId = creativeInfo.getCreativeId();
        steps.bannerSteps().createActiveCpmBanner(activeCpmBanner(campaignId, adGroupId, creativeId), adGroupInfo);

        createRetargetingCondition();

        client = mock(InventoriClient.class);

        controller = new InventoriController(
                translationService,
                inventoriWebService,
                inventoriValidationService,
                validationResultConversionService,
                authenticationSource,
                campaignForecastValidationService,
                new CampaignForecastService(authenticationSource, clientService, userService,
                        client, inventoriService), clientService);
    }

    @Test
    public void getCampaignForecast_BadResult_InternalErrorStatusCode() throws JsonProcessingException {
        badResponse();

        ResponseEntity<WebResponse> response =
                controller.getCampaignForecast(defaultRequest(campaignInfo.getCampaignId()), clientInfo.getLogin());

        assertThat("Должен вернуться правильный код ответа", response.getStatusCodeValue(), is(INTERNAL_ERROR_CODE));
    }

    @Test
    public void getCampaignForecast_BadResult_NoErrorsAndWarnings() throws JsonProcessingException {
        badResponse();

        ResponseEntity<WebResponse> response =
                controller.getCampaignForecast(defaultRequest(campaignInfo.getCampaignId()), clientInfo.getLogin());
        WebValidationResult vr = ((ValidationResponse) response.getBody()).validationResult();

        assertThat("Ответ не должен содержать ошибок", vr.getErrors(), empty());
        assertThat("Ответ не должен содержать предупреждений", vr.getWarnings(), empty());
    }

    @Test(expected = AsyncHttpExecuteException.class)
    public void getCampaignForecast_Timeout_ExceptionIsThrown() throws JsonProcessingException {
        exceptionResponse();

        controller.getCampaignForecast(defaultRequest(campaignInfo.getCampaignId()), clientInfo.getLogin());
    }

    private void badResponse() throws JsonProcessingException {
        when(client.getCampaignPrediction(anyString(), nullable(String.class), nullable(String.class),
                any(CampaignPredictionRequest.class))).thenReturn(
                new CampaignPredictionAvailableResponse(emptyList(),
                        null, null, null, null, null, null, null, null, null, null));
    }

    private void exceptionResponse() throws JsonProcessingException {
        when(client.getCampaignPrediction(anyString(), nullable(String.class), nullable(String.class),
                any(CampaignPredictionRequest.class))).thenThrow(
                new AsyncHttpExecuteException("timeout", new TimeoutException()));
    }
}
