package ru.yandex.direct.web.entity.inventori.service;

import java.time.LocalDate;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.repository.TestCryptaSegmentRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.inventori.InventoriClient;
import ru.yandex.direct.inventori.model.request.CampaignPredictionRequest;
import ru.yandex.direct.inventori.model.response.CampaignPredictionAvailableResponse;
import ru.yandex.direct.inventori.model.response.TrafficLightPredictionResponse;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.web.core.entity.inventori.model.CampaignStrategy;
import ru.yandex.direct.web.core.entity.inventori.model.CpmForecastRequest;
import ru.yandex.direct.web.core.entity.inventori.model.ImpressionLimit;
import ru.yandex.direct.web.core.entity.inventori.service.InventoriService;
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCanvas;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpmGeoproductHtml5Creative;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByType;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultCpmRetCondition;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRules;
import static ru.yandex.direct.core.testing.steps.TurboLandingSteps.defaultBannerTurboLanding;
import static ru.yandex.direct.dbschema.ppc.enums.TurbolandingsPreset.cpm_geoproduct_preset;
import static ru.yandex.direct.web.core.entity.inventori.service.InventoriWebService.INVALID_GROUP_TYPES;
import static ru.yandex.direct.web.core.entity.inventori.validation.InventoriDefectIds.Gen.NO_SUITABLE_ADGROUPS;
import static ru.yandex.direct.web.core.entity.inventori.validation.InventoriDefectIds.Gen.UNSUPPORTED_ERROR;


public class CampaignForecastServiseInvalidGroupTypesTest extends CampaignForecastControllerTestBase{
    @Autowired
    private UserService userService;

    @Mock
    private InventoriClient inventoriClient;

    @Autowired
    private InventoriService inventoriService;

    @Autowired
    protected DirectWebAuthenticationSource authenticationSource;

    @Autowired
    protected TestCryptaSegmentRepository testCryptaSegmentRepository;

    @Autowired
    private ClientService clientService;

    @Autowired
    protected Steps steps;

    private CampaignForecastService campaignForecastService;
    private CpmForecastRequest request;
    private Goal goalAudience;
    private Goal goalSocialDemo;

    @Before
    public void before() {
        testCryptaSegmentRepository.clean();
        super.before();

        MockitoAnnotations.initMocks(this);

        campaignForecastService = new CampaignForecastService(authenticationSource, clientService, userService,
                inventoriClient, inventoriService);

        try {
            successResponseForecast();
            successResponseTrafficLightPrediction();
        } catch (JsonProcessingException e) {
        }

        createCpmBannerCampaign();

        request = request().withCampaignId(campaignInfo.getCampaignId());
        testCryptaSegmentRepository.clean();
        steps.cryptaGoalsSteps().addAllSocialDemoGoals();
        goalSocialDemo = cryptaSegmentRepository.getById(2499000002L); // Женщины
        goalAudience = defaultGoalByType(GoalType.AUDIENCE);
    }

    @Test
    public void geoproductWithoutErrors() throws JsonProcessingException {
        Creative creative = defaultCpmGeoproductHtml5Creative(clientInfo.getClientId(), null);
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(creative, clientInfo);
        Long turbolandingId = steps.turboLandingSteps()
                .createTurboLanding(clientInfo.getClientId(), defaultBannerTurboLanding(clientInfo.getClientId()).withPreset(cpm_geoproduct_preset))
                .getId();

        addCpmBannerAdGroup();

        createFullCpmGeoproductAdGroup(creativeInfo.getCreativeId(), turbolandingId);

        checkWithoutError();
    }

    @Test
    public void geoproductWithError() throws JsonProcessingException {
        Creative creative = defaultCpmGeoproductHtml5Creative(clientInfo.getClientId(), null);
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(creative, clientInfo);
        Long turbolandingId = steps.turboLandingSteps()
                .createTurboLanding(clientInfo.getClientId(), defaultBannerTurboLanding(clientInfo.getClientId()).withPreset(cpm_geoproduct_preset))
                .getId();

        createFullCpmGeoproductAdGroup(creativeInfo.getCreativeId(), turbolandingId);

        checkWithError();
    }

    @Test
    public void geoPinWithoutErrors() throws JsonProcessingException {
        CreativeInfo creativeInfo = steps.creativeSteps()
                .addDefaultCanvasCreativeWithSize(campaignInfo.getClientInfo(), 90L, 728L);

        RetargetingCondition retargetingCondition = (RetargetingCondition) defaultCpmRetCondition()
                .withRules(defaultRules(singletonList(goalAudience), singletonList(goalSocialDemo)));

        createFullCpmGeoPinAdGroup(creativeInfo.getCreativeId(), 1L, retargetingCondition);
        addCpmBannerAdGroup();

        checkWithoutError();
    }

    @Test
    public void geoPinWithError() throws JsonProcessingException {
        CreativeInfo creativeInfo = steps.creativeSteps()
                .addDefaultCanvasCreativeWithSize(campaignInfo.getClientInfo(), 90L, 728L);

        RetargetingCondition retargetingCondition = (RetargetingCondition) defaultCpmRetCondition()
                .withRules(defaultRules(singletonList(goalAudience), singletonList(goalSocialDemo)));

        createFullCpmGeoPinAdGroup(creativeInfo.getCreativeId(), 1L, retargetingCondition);

        final ArgumentCaptor<CampaignPredictionRequest> argument = ArgumentCaptor.forClass(CampaignPredictionRequest.class);
        var resultForecast = campaignForecastService.forecast("", request);
        verify(inventoriClient, times(0)).getCampaignPrediction(any(), any(), any(), argument.capture());

        assertTrue(resultForecast.getErrors().getErrors().contains(new Defect<>(NO_SUITABLE_ADGROUPS)));

        var resultTrafficLightPrediction = campaignForecastService.trafficLightPrediction("", request);
        verify(inventoriClient, times(0)).getTrafficLightPrediction(any(), any(), any(), argument.capture());

        assertTrue(resultTrafficLightPrediction.getErrors().getErrors().contains(new Defect<>(UNSUPPORTED_ERROR)));
    }

    @Test
    public void indoorWithoutErrors() throws JsonProcessingException {
        CreativeInfo creativeInfo = steps.creativeSteps().addDefaultCpmIndoorVideoCreative(clientInfo);

        createFullCpmIndoorAdGroup(creativeInfo.getCreativeId());
        addCpmBannerAdGroup();

        checkWithoutError();
    }

    @Test
    public void indoorWithError() throws JsonProcessingException {
        CreativeInfo creativeInfo = steps.creativeSteps().addDefaultCpmIndoorVideoCreative(clientInfo);

        createFullCpmIndoorAdGroup(creativeInfo.getCreativeId());

        checkWithError();
    }

    @Test
    public void outdoorWithoutErrors() throws JsonProcessingException {
        CreativeInfo creativeInfo = steps.creativeSteps().addDefaultCpmOutdoorVideoCreative(clientInfo);

        createFullCpmOutdoorAdGroup(creativeInfo.getCreativeId());
        addCpmBannerAdGroup();

        checkWithoutError();
    }

    @Test
    public void outdoorWithError() throws JsonProcessingException {
        CreativeInfo creativeInfo = steps.creativeSteps().addDefaultCpmOutdoorVideoCreative(clientInfo);

        createFullCpmOutdoorAdGroup(creativeInfo.getCreativeId());

        checkWithError();
    }

    private void checkWithoutError() throws JsonProcessingException {
        final ArgumentCaptor<CampaignPredictionRequest> argument = ArgumentCaptor.forClass(CampaignPredictionRequest.class);
        campaignForecastService.forecast("", request);
        verify(inventoriClient, times(1)).getCampaignPrediction(any(), any(), any(), argument.capture());

        var actual = argument.getValue();

        assertTrue(actual.getTargets().stream().noneMatch(target -> INVALID_GROUP_TYPES.contains(target.getGroupType())));

        campaignForecastService.trafficLightPrediction("", request);
        verify(inventoriClient, times(1)).getTrafficLightPrediction(any(), any(), any(), argument.capture());

        actual = argument.getValue();

        assertTrue(actual.getTargets().stream().noneMatch(target -> INVALID_GROUP_TYPES.contains(target.getGroupType())));
    }

    private void checkWithError() throws JsonProcessingException {
        final ArgumentCaptor<CampaignPredictionRequest> argument = ArgumentCaptor.forClass(CampaignPredictionRequest.class);
        var resultForecast = campaignForecastService.forecast("", request);
        verify(inventoriClient, times(0)).getCampaignPrediction(any(), any(), any(), argument.capture());

        assertTrue(resultForecast.getErrors().getErrors().contains(new Defect<>(UNSUPPORTED_ERROR)));

        var resultTrafficLightPrediction = campaignForecastService.trafficLightPrediction("", request);
        verify(inventoriClient, times(0)).getTrafficLightPrediction(any(), any(), any(), argument.capture());

        assertTrue(resultTrafficLightPrediction.getErrors().getErrors().contains(new Defect<>(UNSUPPORTED_ERROR)));
    }

    private CpmForecastRequest request() {
        return new CpmForecastRequest()
                .withStrategy(new CampaignStrategy()
                        .withBudget(100_000.00)
                        .withStartDate(LocalDate.now())
                        .withEndDate(LocalDate.now().plusMonths(1))
                        .withImpressionLimit(new ImpressionLimit(0L, 0L))
                        .withType("MAX_REACH"));
    }

    private void successResponseForecast() throws JsonProcessingException {
        Mockito.when(inventoriClient.getCampaignPrediction(anyString(), nullable(String.class), nullable(String.class),
                any(CampaignPredictionRequest.class))).thenReturn(
                new CampaignPredictionAvailableResponse(
                        emptyList(),
                        123_456_789L,
                        6_000_000L,
                        7_000_000L,
                        8_000_000L,
                        9_000_000L,
                        null, null, null, null, null));
    }

    private void successResponseTrafficLightPrediction() throws JsonProcessingException {
        when(inventoriClient.getTrafficLightPrediction(anyString(), nullable(String.class), nullable(String.class),
                any(CampaignPredictionRequest.class)))
                .thenReturn(new TrafficLightPredictionResponse(emptyList(), 1, 12334000000L));
    }

    private void addCpmBannerAdGroup() {
        Creative creative2 = defaultCanvas(clientInfo.getClientId(), null);
        CreativeInfo creativeInfo2= steps.creativeSteps().createCreative(creative2, clientInfo);
        createFullCpmBannerAdGroup(creativeInfo2.getCreativeId());
    }
}

