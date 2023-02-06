package ru.yandex.direct.web.entity.inventori.service;

import java.time.LocalDate;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.direct.validation.defect.params.DateDefectParams;
import ru.yandex.direct.validation.defect.params.NumberDefectParams;
import ru.yandex.direct.web.core.entity.inventori.model.CpmForecastRequest;
import ru.yandex.direct.web.core.entity.inventori.model.TrafficTypeCorrectionsWeb;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.entity.inventori.controller.InventoriController;
import ru.yandex.direct.web.testing.data.TestCpmForecastRequest;
import ru.yandex.direct.web.validation.model.ValidationResponse;
import ru.yandex.direct.web.validation.model.WebDefect;
import ru.yandex.direct.web.validation.model.WebValidationResult;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.web.entity.inventori.service.InventoriTestWebDefects.webCampaignNotFound;
import static ru.yandex.direct.web.entity.inventori.service.InventoriTestWebDefects.webCannotBeNull;
import static ru.yandex.direct.web.entity.inventori.service.InventoriTestWebDefects.webInvalidId;
import static ru.yandex.direct.web.entity.inventori.service.InventoriTestWebDefects.webMustBeGreaterThanMin;
import static ru.yandex.direct.web.entity.inventori.service.InventoriTestWebDefects.webMustBeGreaterThatOrEqualToMin;
import static ru.yandex.direct.web.entity.inventori.service.InventoriTestWebDefects.webMustBeInCollection;
import static ru.yandex.direct.web.entity.inventori.service.InventoriTestWebDefects.webMustBeNull;
import static ru.yandex.direct.web.testing.data.TestCpmForecastRequest.defaultImpressionLimit;
import static ru.yandex.direct.web.testing.data.TestCpmForecastRequest.defaultRequest;

@SuppressWarnings("Duplicates")
public class CampaignForecastControllerRequestValidationTest extends CampaignForecastControllerTestBase {

    private static final Long NONEXISTENT_CAMPAIGN_ID = 654321234L;

    private static final String CAMPAIGN_ID_PATH = "campaign_id";
    private static final String EXAMPLE_TYPE_PATH = "new_campaign_example_type";
    private static final String STRATEGY_PATH = "strategy";
    private static final String STRATEGY_TYPE_PATH = STRATEGY_PATH + ".type";
    private static final String BUDGET_PATH = STRATEGY_PATH + ".budget";
    private static final String START_DATE_PATH = STRATEGY_PATH + ".start_date";
    private static final String END_DATE_PATH = STRATEGY_PATH + ".end_date";
    private static final String IMPRESSION_LIMIT_PATH = STRATEGY_PATH + ".impression_limit";
    private static final String DAYS_PATH = IMPRESSION_LIMIT_PATH + ".days";
    private static final String IMPRESSIONS_PATH = IMPRESSION_LIMIT_PATH + ".impressions";
    private static final String CORRECTIONS_BANNER_PATH = "traffic_type_corrections.banner";


    @Autowired
    private InventoriController controller;

    @Test
    public void getCampaignForecast_NullCampaignIdAndNullNewCampaignExampleType() throws JsonProcessingException {
        CpmForecastRequest request = TestCpmForecastRequest.defaultRequest(null);

        checkValidationErrorResult(request, webCannotBeNull(EXAMPLE_TYPE_PATH), webCannotBeNull(CAMPAIGN_ID_PATH));
    }

    @Test
    public void getCampaignForecast_ZeroCampaignId() throws JsonProcessingException {
        Long campaignId = 0L;
        CpmForecastRequest request = TestCpmForecastRequest.defaultRequest(campaignId);

        checkValidationErrorResult(request, webInvalidId(CAMPAIGN_ID_PATH, String.valueOf(campaignId)));
    }

    @Test
    public void getCampaignForecast_NewCampaignExampleTypeTwo() throws JsonProcessingException {
        Integer exampleType = 2;
        CpmForecastRequest request = TestCpmForecastRequest.defaultRequest(null);
        request.withNewCampaignExampleType(exampleType);

        checkValidationErrorResult(request, webMustBeInCollection(EXAMPLE_TYPE_PATH, String.valueOf(exampleType)));
    }

    @Test
    public void getCampaignForecast_NewCampaignExampleTypeZeroAndExistingCampaign() throws JsonProcessingException {
        Long campaignId = campaignInfo.getCampaignId();
        Integer exampleType = 0;
        CpmForecastRequest request = TestCpmForecastRequest.defaultRequest(campaignId);
        request.withNewCampaignExampleType(exampleType);

        checkValidationErrorResult(request, webMustBeNull(EXAMPLE_TYPE_PATH, String.valueOf(exampleType)),
                webMustBeNull(CAMPAIGN_ID_PATH, String.valueOf(campaignId)));
    }

    @Test
    public void getCampaignForecast_NewCampaignExampleTypeOneAndExistingCampaign() throws JsonProcessingException {
        Long campaignId = campaignInfo.getCampaignId();
        Integer exampleType = 1;
        CpmForecastRequest request = TestCpmForecastRequest.defaultRequest(campaignInfo.getCampaignId());
        request.withNewCampaignExampleType(exampleType);

        checkValidationErrorResult(request, webMustBeNull(EXAMPLE_TYPE_PATH, String.valueOf(exampleType)),
                webMustBeNull(CAMPAIGN_ID_PATH, String.valueOf(campaignId)));
    }

    @Test
    public void getCampaignForecast_NonexistentCampaign() throws JsonProcessingException {
        CpmForecastRequest request = TestCpmForecastRequest.defaultRequest(NONEXISTENT_CAMPAIGN_ID);

        checkValidationErrorResult(request, webCampaignNotFound(CAMPAIGN_ID_PATH,
                String.valueOf(NONEXISTENT_CAMPAIGN_ID)));
    }

    @Test
    public void getCampaignForecast_NullStrategy() throws JsonProcessingException {
        CpmForecastRequest request = TestCpmForecastRequest.defaultRequest(campaignInfo.getCampaignId());
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
    public void getCampaignForecast_CorrectionsWithNegativeValues() throws JsonProcessingException {
        int wrongBannerValue = -20;
        CpmForecastRequest request = defaultRequest(campaignInfo.getCampaignId());
        TrafficTypeCorrectionsWeb
                trafficTypeCorrectionsWeb = new TrafficTypeCorrectionsWeb(wrongBannerValue, 100, 1200, 1300, 300, 500);
        request.withTrafficTypeCorrections(trafficTypeCorrectionsWeb);
        checkValidationErrorResult(request, webMustBeGreaterThatOrEqualToMin(CORRECTIONS_BANNER_PATH, String.valueOf(wrongBannerValue),
                new NumberDefectParams().withMin(0)));
    }

    private void checkValidationErrorResult(CpmForecastRequest request, WebDefect... errors) throws JsonProcessingException {
        ResponseEntity<WebResponse> response = controller.getCampaignForecast(request, clientInfo.getLogin());

        WebValidationResult vr = ((ValidationResponse) response.getBody()).validationResult();
        WebValidationResult expectedResult =
                new WebValidationResult().addErrors(errors);

        assertThat(response.getStatusCode().value(), is(VALIDATION_ERROR_CODE));
        Assertions.assertThat(vr).isEqualToComparingFieldByFieldRecursively(expectedResult);
    }
}
