package ru.yandex.direct.web.entity.inventori.service;

import java.time.LocalDate;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.ManualStrategy;
import ru.yandex.direct.validation.defect.params.DateDefectParams;
import ru.yandex.direct.validation.defect.params.NumberDefectParams;
import ru.yandex.direct.web.core.entity.inventori.model.CampaignStrategy;
import ru.yandex.direct.web.core.entity.inventori.model.CpmForecastRequest;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.entity.inventori.controller.InventoriController;
import ru.yandex.direct.web.testing.data.TestCpmForecastRequest;
import ru.yandex.direct.web.validation.model.ValidationResponse;
import ru.yandex.direct.web.validation.model.WebDefect;
import ru.yandex.direct.web.validation.model.WebValidationResult;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmBannerCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.manualStrategy;
import static ru.yandex.direct.web.entity.inventori.service.InventoriTestWebDefects.webCampaignNotFound;
import static ru.yandex.direct.web.entity.inventori.service.InventoriTestWebDefects.webCannotBeNull;
import static ru.yandex.direct.web.entity.inventori.service.InventoriTestWebDefects.webInvalidCampaignStrategy;
import static ru.yandex.direct.web.entity.inventori.service.InventoriTestWebDefects.webInvalidCampaignType;
import static ru.yandex.direct.web.entity.inventori.service.InventoriTestWebDefects.webInvalidId;
import static ru.yandex.direct.web.entity.inventori.service.InventoriTestWebDefects.webMustBeGreaterThanMin;
import static ru.yandex.direct.web.entity.inventori.service.InventoriTestWebDefects.webMustBeGreaterThatOrEqualToMin;
import static ru.yandex.direct.web.entity.inventori.service.InventoriTestWebDefects.webMustBeInCollection;
import static ru.yandex.direct.web.entity.inventori.service.InventoriTestWebDefects.webMustBeNull;
import static ru.yandex.direct.web.testing.data.TestCpmForecastRequest.defaultImpressionLimit;
import static ru.yandex.direct.web.testing.data.TestCpmForecastRequest.defaultStrategy;

@SuppressWarnings("Duplicates")
public class CpmTrafficLightPredictionRequestValidationTest extends CampaignForecastControllerTestBase {

    private static final Long NONEXISTENT_CAMPAIGN_ID = 12345678L;

    @Autowired
    private InventoriController controller;

    @Test
    public void getTrafficLightPrediction_NegativeCampaignId() throws JsonProcessingException {
        Long campaignId = -1L;
        CpmForecastRequest request = new CpmForecastRequest().withCampaignId(campaignId);

        checkValidationErrorResult(request, webInvalidId(CAMPAIGN_ID_PATH, campaignId.toString()));
    }

    @Test
    public void getTrafficLightPrediction_ZeroCampaignId() throws JsonProcessingException {
        Long campaignId = 0L;
        CpmForecastRequest request = new CpmForecastRequest().withCampaignId(campaignId);

        checkValidationErrorResult(request, webInvalidId(CAMPAIGN_ID_PATH, campaignId.toString()));
    }

    @Test
    public void getTrafficLightPrediction_NonexistentCampaignId() throws JsonProcessingException {
        Long campaignId = NONEXISTENT_CAMPAIGN_ID;
        CpmForecastRequest request = new CpmForecastRequest().withCampaignId(campaignId);

        checkValidationErrorResult(request, webCampaignNotFound(CAMPAIGN_ID_PATH, campaignId.toString()));
    }

    @Test
    public void getTrafficLightPrediction_CampaignIdAndTargetIdAreNull() throws JsonProcessingException {
        CpmForecastRequest request = new CpmForecastRequest().withStrategy(defaultStrategy());

        checkValidationErrorResult(request, webCannotBeNull(EXAMPLE_TYPE_PATH), webCannotBeNull(CAMPAIGN_ID_PATH));
    }

    @Test
    public void getTrafficLightPrediction_NewCampaignExampleTypeTwo() throws JsonProcessingException {
        Integer exampleType = 2;
        CpmForecastRequest request = TestCpmForecastRequest.defaultRequest(null);
        request.withNewCampaignExampleType(exampleType);

        checkValidationErrorResult(request, webMustBeInCollection(EXAMPLE_TYPE_PATH, String.valueOf(exampleType)));
    }

    @Test
    public void getTrafficLightPrediction_NewCampaignExampleTypeZeroAndExistingCampaign() throws JsonProcessingException {
        Long campaignId = campaignInfo.getCampaignId();
        Integer exampleType = 0;
        CpmForecastRequest request = TestCpmForecastRequest.defaultRequest(campaignId);
        request.withNewCampaignExampleType(exampleType);

        checkValidationErrorResult(request, webMustBeNull(EXAMPLE_TYPE_PATH, String.valueOf(exampleType)),
                webMustBeNull(CAMPAIGN_ID_PATH, String.valueOf(campaignId)));
    }

    @Test
    public void getTrafficLightPrediction_NewCampaignExampleTypeOneAndExistingCampaign() throws JsonProcessingException {
        Long campaignId = campaignInfo.getCampaignId();
        Integer exampleType = 1;
        CpmForecastRequest request = TestCpmForecastRequest.defaultRequest(campaignInfo.getCampaignId());
        request.withNewCampaignExampleType(exampleType);

        checkValidationErrorResult(request, webMustBeNull(EXAMPLE_TYPE_PATH, String.valueOf(exampleType)),
                webMustBeNull(CAMPAIGN_ID_PATH, String.valueOf(campaignId)));
    }

    @Test
    public void getTrafficLightPrediction_NullStrategyAndCampaignId() throws JsonProcessingException {
        CpmForecastRequest request = TestCpmForecastRequest.defaultRequest(null).withNewCampaignExampleType(1);
        request.withStrategy(null);

        checkValidationErrorResult(request, webCannotBeNull(STRATEGY_PATH));
    }

    @Test
    public void getCampaignForecast_StrategyWithNullType() throws JsonProcessingException {
        CpmForecastRequest request = TestCpmForecastRequest.defaultRequest(campaignInfo.getCampaignId(),
                TestCpmForecastRequest.defaultStrategy().withType(null));

        checkValidationErrorResult(request, webCannotBeNull(STRATEGY_TYPE_PATH));
    }

    @Test
    public void getCampaignForecast_StrategyWithEmptyType() throws JsonProcessingException {
        String emptyStrategyType = "";
        CpmForecastRequest request = TestCpmForecastRequest.defaultRequest(campaignInfo.getCampaignId(),
                TestCpmForecastRequest.defaultStrategy().withType(emptyStrategyType));

        checkValidationErrorResult(request, webMustBeInCollection(STRATEGY_TYPE_PATH, emptyStrategyType));
    }

    @Test
    public void getCampaignForecast_StrategyWithBadType() throws JsonProcessingException {
        String badStrategyType = "BAD_TYPE";
        CpmForecastRequest request = TestCpmForecastRequest.defaultRequest(campaignInfo.getCampaignId(),
                TestCpmForecastRequest.defaultStrategy().withType(badStrategyType));

        checkValidationErrorResult(request, webMustBeInCollection(STRATEGY_TYPE_PATH, badStrategyType));
    }

    @Test
    public void getCampaignForecast_StrategyWithNullBudget() throws JsonProcessingException {
        CpmForecastRequest request = TestCpmForecastRequest.defaultRequest(campaignInfo.getCampaignId(),
                TestCpmForecastRequest.defaultStrategy().withBudget(null));

        checkValidationErrorResult(request, webCannotBeNull(BUDGET_PATH));
    }

    @Test
    public void getCampaignForecast_StrategyWithNoBudget() throws JsonProcessingException {
        Double budget = 0.0;
        CpmForecastRequest request = TestCpmForecastRequest.defaultRequest(campaignInfo.getCampaignId(),
                TestCpmForecastRequest.defaultStrategy().withBudget(budget));

        checkValidationErrorResult(request, webMustBeGreaterThanMin(BUDGET_PATH, String.valueOf(budget),
                new NumberDefectParams().withMin(budget)));
    }

    @Test
    public void getCampaignForecast_StrategyWithNullStartDay() throws JsonProcessingException {
        CpmForecastRequest request = TestCpmForecastRequest.defaultRequest(campaignInfo.getCampaignId(),
                TestCpmForecastRequest.defaultStrategy().withStartDate(null));

        checkValidationErrorResult(request, webCannotBeNull(START_DATE_PATH));
    }

    @Test
    public void getCampaignForecast_StrategyWithNullEndDay() throws JsonProcessingException {
        CpmForecastRequest request = TestCpmForecastRequest.defaultRequest(campaignInfo.getCampaignId(),
                TestCpmForecastRequest.defaultStrategy().withEndDate(null));

        checkValidationErrorResult(request, webCannotBeNull(END_DATE_PATH));
    }

    @Test
    public void getCampaignForecast_StrategyWithNullStartAndEndDay() throws JsonProcessingException {
        CpmForecastRequest request = TestCpmForecastRequest.defaultRequest(campaignInfo.getCampaignId(),
                TestCpmForecastRequest.defaultStrategy().withStartDate(null).withEndDate(null));

        checkValidationErrorResult(request, webCannotBeNull(END_DATE_PATH), webCannotBeNull(START_DATE_PATH));
    }

    @Test
    public void getCampaignForecast_StrategyWithEndDateBeforeStart() throws JsonProcessingException {
        LocalDate endDate = LocalDate.now().minusMonths(1);
        CpmForecastRequest request = TestCpmForecastRequest.defaultRequest(campaignInfo.getCampaignId(),
                TestCpmForecastRequest.defaultStrategy().withEndDate(endDate));

        checkValidationErrorResult(request, webMustBeGreaterThatOrEqualToMin(END_DATE_PATH, endDate.toString(),
                new DateDefectParams().withMin(LocalDate.now())));
    }

    @Test
    public void getCampaignForecast_StrategyWithNullImpressionLimit() throws JsonProcessingException {
        CpmForecastRequest request = TestCpmForecastRequest.defaultRequest(campaignInfo.getCampaignId(),
                TestCpmForecastRequest.defaultStrategy().withImpressionLimit(null));

        checkValidationErrorResult(request, webCannotBeNull(IMPRESSION_LIMIT_PATH));
    }

    @Test
    public void getCampaignForecast_StrategyWithNullDaysImpressionLimit() throws JsonProcessingException {
        CpmForecastRequest request = TestCpmForecastRequest.defaultRequest(campaignInfo.getCampaignId(),
                TestCpmForecastRequest.defaultStrategy().withImpressionLimit(defaultImpressionLimit().withDays(null)));

        checkValidationErrorResult(request, webCannotBeNull(DAYS_PATH));
    }

    @Test
    public void getCampaignForecast_StrategyWithNullImpressionsImpressionLimit() throws JsonProcessingException {
        CpmForecastRequest request = TestCpmForecastRequest.defaultRequest(campaignInfo.getCampaignId(),
                TestCpmForecastRequest.defaultStrategy().withImpressionLimit(defaultImpressionLimit()
                        .withImpressions(null)));

        checkValidationErrorResult(request, webCannotBeNull(IMPRESSIONS_PATH));
    }

    @Test
    public void getCampaignForecast_StrategyWithNegativeDaysImpressionLimit() throws JsonProcessingException {
        Long days = -1L;
        CpmForecastRequest request = TestCpmForecastRequest.defaultRequest(campaignInfo.getCampaignId(),
                TestCpmForecastRequest.defaultStrategy().withImpressionLimit(defaultImpressionLimit()
                        .withDays(days)));

        checkValidationErrorResult(request, webMustBeGreaterThatOrEqualToMin(DAYS_PATH, String.valueOf(days),
                new NumberDefectParams().withMin(0L)));
    }

    @Test
    public void getCampaignForecast_StrategyWithNegativeImpressionsImpressionLimit() throws JsonProcessingException {
        Long impressions = -1L;
        CpmForecastRequest request = TestCpmForecastRequest.defaultRequest(campaignInfo.getCampaignId(),
                TestCpmForecastRequest.defaultStrategy().withImpressionLimit(defaultImpressionLimit()
                        .withImpressions(impressions)));

        checkValidationErrorResult(request, webMustBeGreaterThatOrEqualToMin(IMPRESSIONS_PATH, String.valueOf(impressions),
                new NumberDefectParams().withMin(0L)));
    }

    @Test
    public void getTrafficLightPrediction_NoRightsToReadCampaign() throws JsonProcessingException {
        Long campaignId = steps.campaignSteps().createActiveCpmBannerCampaign().getCampaignId();
        CpmForecastRequest request = new CpmForecastRequest().withCampaignId(campaignId);

        checkValidationErrorResult(request, webCampaignNotFound(CAMPAIGN_ID_PATH, campaignId.toString()));
    }

    @Test
    public void getTrafficLightPrediction_CampaignWithTextType() throws JsonProcessingException {
        Long campaignId = steps.campaignSteps().createActiveTextCampaign(clientInfo).getCampaignId();
        CpmForecastRequest request = new CpmForecastRequest().withCampaignId(campaignId);

        checkValidationErrorResult(request, webInvalidCampaignType(CAMPAIGN_ID_PATH, campaignId, CampaignType.TEXT));
    }

    @Test
    public void getTrafficLightPrediction_CampaignWithManualStrategy() throws JsonProcessingException {
        ManualStrategy strategy = manualStrategy();
        campaignInfo = steps.campaignSteps().createCampaign(
                new CampaignInfo()
                        .withClientInfo(clientInfo)
                        .withCampaign(activeCpmBannerCampaign(null, null).withStrategy(strategy)));
        Long campaignId = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo).getCampaignId();
        CpmForecastRequest request = new CpmForecastRequest().withCampaignId(campaignId);

        checkValidationErrorResult(request, webInvalidCampaignStrategy(CAMPAIGN_ID_PATH, campaignId, StrategyName.DEFAULT_));
    }

    @Test
    public void getTrafficLightPrediction_StrategyWithNullCpm() throws JsonProcessingException {
        Long campaignId = campaignInfo.getCampaignId();
        CampaignStrategy strategy = defaultStrategy().withCpm(null);
        CpmForecastRequest request = new CpmForecastRequest().withCampaignId(campaignId).withStrategy(strategy);

        checkValidationErrorResult(request, webCannotBeNull(CPM_PATH));
    }

    private void checkValidationErrorResult(CpmForecastRequest request, WebDefect... errors)
            throws JsonProcessingException {
        ResponseEntity<WebResponse> response = controller.getCpmTrafficLightPrediction(request, clientInfo.getLogin());

        WebValidationResult vr = ((ValidationResponse) response.getBody()).validationResult();
        WebValidationResult expectedResult =
                new WebValidationResult().addErrors(errors);

        assertThat(response.getStatusCode().value(), is(VALIDATION_ERROR_CODE));
        Assertions.assertThat(vr).isEqualToComparingFieldByFieldRecursively(expectedResult);
    }
}
