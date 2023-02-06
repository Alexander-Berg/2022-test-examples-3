package ru.yandex.direct.web.entity.inventori.service;

import java.util.HashSet;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableSet;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.direct.common.TranslationService;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.currency.service.CurrencyRateService;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.ManualStrategy;
import ru.yandex.direct.inventori.InventoriClient;
import ru.yandex.direct.inventori.model.request.BlockSize;
import ru.yandex.direct.inventori.model.request.CampaignParametersRf;
import ru.yandex.direct.inventori.model.request.CampaignPredictionRequest;
import ru.yandex.direct.inventori.model.request.GroupType;
import ru.yandex.direct.inventori.model.request.Target;
import ru.yandex.direct.inventori.model.response.CampaignPredictionLowReachResponse;
import ru.yandex.direct.inventori.model.response.TrafficLightPredictionResponse;
import ru.yandex.direct.web.core.entity.inventori.model.AdGroupGeo;
import ru.yandex.direct.web.core.entity.inventori.model.CampaignStrategy;
import ru.yandex.direct.web.core.entity.inventori.model.CpmForecastRequest;
import ru.yandex.direct.web.core.entity.inventori.model.ImpressionLimit;
import ru.yandex.direct.web.core.entity.inventori.model.YndxFrontpagePageType;
import ru.yandex.direct.web.core.entity.inventori.service.InventoriService;
import ru.yandex.direct.web.core.entity.inventori.service.InventoriWebService;
import ru.yandex.direct.web.core.entity.inventori.service.InventoriWebValidationService;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource;
import ru.yandex.direct.web.entity.inventori.controller.InventoriController;
import ru.yandex.direct.web.entity.inventori.model.CpmTrafficLightPredictionResponse;
import ru.yandex.direct.web.validation.kernel.ValidationResultConversionService;
import ru.yandex.direct.web.validation.model.WebValidationResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageCampaignShowType.FRONTPAGE;
import static ru.yandex.direct.core.entity.inventori.service.InventoriServiceCore.ALLOWED_BLOCK_SIZES;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmBannerCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.manualStrategy;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultAdaptive;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCanvas;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.web.entity.inventori.service.InventoriTestWebDefects.webContainsKeywordAdGroups;
import static ru.yandex.direct.web.entity.inventori.service.InventoriTestWebDefects.webLowReach;
import static ru.yandex.direct.web.testing.data.TestCpmForecastRequest.defaultRequest;
import static ru.yandex.direct.web.testing.data.TestCpmForecastRequest.defaultStrategy;

public class CpmTrafficLightPredictionPositiveTest extends CampaignForecastControllerTestBase {

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

    @Autowired
    private TestCampaignRepository testCampaignRepository;

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

    // cpm_banner

    @Test
    public void getTrafficLightPrediction_SuccessResponseFromInventori_StatusIsOk() throws JsonProcessingException {
        successResponse();
        CpmForecastRequest request = new CpmForecastRequest().withCampaignId(campaignInfo.getCampaignId());

        ResponseEntity<WebResponse> response = controller.getCpmTrafficLightPrediction(request, clientInfo.getLogin());

        assertThat("Вернулся правильный статус", response.getStatusCode().value(), is(SUCCESS_CODE));
    }

    @Test
    public void getTrafficLightPrediction_SuccessResponseFromInventori_TrafficLightColorIsCorrect()
            throws JsonProcessingException {
        successResponse();
        CpmForecastRequest request = new CpmForecastRequest().withCampaignId(campaignInfo.getCampaignId());

        CpmTrafficLightPredictionResponse response =
                (CpmTrafficLightPredictionResponse) controller
                        .getCpmTrafficLightPrediction(request, clientInfo.getLogin()).getBody();

        assertThat("Вернулся правильный цвет светофора", response.getResult().getTrafficLightColor(), is(1));
    }

    @Test
    public void getTrafficLightPrediction_SuccessResponseFromInventori_RecommendedPriceIsCorrect()
            throws JsonProcessingException {
        successResponse();
        CpmForecastRequest request = new CpmForecastRequest().withCampaignId(campaignInfo.getCampaignId());

        CpmTrafficLightPredictionResponse response =
                (CpmTrafficLightPredictionResponse) controller
                        .getCpmTrafficLightPrediction(request, clientInfo.getLogin()).getBody();

        assertThat("Вернулась правильная цена", response.getResult().getRecommendedPrice(), is(12334.0));
    }

    @Test
    public void getTrafficLightPrediction_LowReachResponseFromInventori_StatusIsOk() throws JsonProcessingException {
        responseWithErrors();
        CpmForecastRequest request = new CpmForecastRequest().withCampaignId(campaignInfo.getCampaignId());

        ResponseEntity<WebResponse> response = controller.getCpmTrafficLightPrediction(request, clientInfo.getLogin());

        assertThat("Вернулся правильный статус", response.getStatusCode().value(), is(SUCCESS_CODE));
    }

    @Test
    public void getTrafficLightPrediction_LowReachResponseFromInventori_ResponseContainsCorrectError()
            throws JsonProcessingException {
        responseWithErrors();
        CpmForecastRequest request = new CpmForecastRequest().withCampaignId(campaignInfo.getCampaignId());

        WebValidationResult webValidationResult =
                ((CpmTrafficLightPredictionResponse)
                        controller.getCpmTrafficLightPrediction(request, clientInfo.getLogin()).getBody())
                        .validationResult();
        WebValidationResult expectedResult = new WebValidationResult().addErrors(
                webLowReach(request.toString(), REACH_LESS_THAN));

        Assertions.assertThat(webValidationResult).isEqualToComparingFieldByFieldRecursively(expectedResult);
    }

    @Test
    public void getTrafficLightPrediction_LowReachResponseFromInventori_ResultIsNull()
            throws JsonProcessingException {
        responseWithErrors();
        CpmForecastRequest request = new CpmForecastRequest().withCampaignId(campaignInfo.getCampaignId());

        CpmTrafficLightPredictionResponse response =
                (CpmTrafficLightPredictionResponse)
                        controller.getCpmTrafficLightPrediction(request, clientInfo.getLogin()).getBody();

        assertThat("Результат прогноза должен быть null", response.getResult(), nullValue());
    }

    @Test
    public void getTrafficLightPrediction_CampaignWithWrongStrategyType_RequestWithStrategy_StatusIsOk()
            throws JsonProcessingException {
        successResponse();
        ManualStrategy strategy = manualStrategy();
        campaignInfo = steps.campaignSteps().createCampaign(
                new CampaignInfo()
                        .withClientInfo(clientInfo)
                        .withCampaign(activeCpmBannerCampaign(null, null).withStrategy(strategy)));
        Long campaignId = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo).getCampaignId();
        CpmForecastRequest request =
                new CpmForecastRequest().withCampaignId(campaignId).withStrategy(defaultStrategy());

        ResponseEntity<WebResponse> response = controller.getCpmTrafficLightPrediction(request, clientInfo.getLogin());
        assertThat("Вернулся правильный статус", response.getStatusCode().value(), is(SUCCESS_CODE));
    }

    @Test
    public void getTrafficLightPrediction_SuccessResponse_CampaignHasAdGroupWithKeywords_ErrorIsAdded()
            throws JsonProcessingException {
        successResponse();
        createKeywordCpmAdGroup();
        CpmForecastRequest request = defaultRequest(campaignInfo.getCampaignId());

        WebValidationResult vr =
                ((CpmTrafficLightPredictionResponse)
                        controller.getCpmTrafficLightPrediction(request, clientInfo.getLogin()).getBody())
                        .validationResult();
        WebValidationResult expectedResult =
                new WebValidationResult().addErrors(webContainsKeywordAdGroups(request));

        Assertions.assertThat(vr).isEqualToComparingFieldByFieldRecursively(expectedResult);
    }

    @Test
    public void getCpmTrafficLightPrediction_NewCpmBannerCampaign_NoErrors() throws JsonProcessingException {
        successResponse();
        CampaignStrategy strategy = defaultStrategy();
        CpmForecastRequest request = new CpmForecastRequest().withNewCampaignExampleType(0).withStrategy(strategy);

        WebValidationResult vr =
                ((CpmTrafficLightPredictionResponse)
                        controller.getCpmTrafficLightPrediction(request, clientInfo.getLogin()).getBody())
                        .validationResult();
        WebValidationResult expectedResult = new WebValidationResult();

        Assertions.assertThat(vr).isEqualToComparingFieldByFieldRecursively(expectedResult);
    }

    @Test
    public void getCpmTrafficLightPrediction_ExistingCpmBannerCampaignWithZeroRfAndNonZeroRfReset_InventoriRequestHasCorrectRfAndRfReset()
            throws JsonProcessingException {
        successResponse();
        testCampaignRepository.updateRfAndRfReset(campaignInfo.getShard(), campaignInfo.getCampaignId(), 0L, 10L);

        CpmForecastRequest request = new CpmForecastRequest().withCampaignId(campaignInfo.getCampaignId());
        controller.getCpmTrafficLightPrediction(request, clientInfo.getLogin());

        ArgumentCaptor<String> requestId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> clientLogin = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> operatorLogin = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CampaignPredictionRequest> campaignPredictionRequest =
                ArgumentCaptor.forClass(CampaignPredictionRequest.class);

        Mockito.verify(client, Mockito.times(1)).getTrafficLightPrediction(requestId.capture(),
                operatorLogin.capture(), clientLogin.capture(), campaignPredictionRequest.capture());

        CampaignPredictionRequest inventoriRequest = campaignPredictionRequest.getValue();

        assertThat("В запросе правильные значения rf и rfReset", inventoriRequest.getParameters().getRf(),
                is(new CampaignParametersRf(0, 0)));
    }

    @Test
    public void getCpmTrafficLightPrediction_ExistingCpmBannerCampaign_RequestWithZeroRfAndNonZeroRfReset_InventoriRequestHasCorrectRfAndRfReset()
            throws JsonProcessingException {
        successResponse();

        CampaignStrategy campaignStrategy = defaultStrategy().withImpressionLimit(
                new ImpressionLimit().withImpressions(0L).withDays(10L));
        CpmForecastRequest request = new CpmForecastRequest().withCampaignId(campaignInfo.getCampaignId())
                .withStrategy(campaignStrategy);
        controller.getCpmTrafficLightPrediction(request, clientInfo.getLogin());

        ArgumentCaptor<String> requestId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> clientLogin = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> operatorLogin = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CampaignPredictionRequest> campaignPredictionRequest =
                ArgumentCaptor.forClass(CampaignPredictionRequest.class);

        Mockito.verify(client, Mockito.times(1)).getTrafficLightPrediction(requestId.capture(),
                operatorLogin.capture(), clientLogin.capture(), campaignPredictionRequest.capture());

        CampaignPredictionRequest inventoriRequest = campaignPredictionRequest.getValue();

        assertThat("В запросе правильные значения rf и rfReset", inventoriRequest.getParameters().getRf(),
                is(new CampaignParametersRf(0, 0)));
    }

    @Test
    public void getCpmTrafficLightPrediction_NewCpmBannerCampaign_RequestWithZeroRfAndNonZeroRfReset_InventoriRequestHasCorrectRfAndRfReset()
            throws JsonProcessingException {
        successResponse();

        CampaignStrategy campaignStrategy = defaultStrategy().withImpressionLimit(
                new ImpressionLimit().withImpressions(0L).withDays(10L));
        CpmForecastRequest request = new CpmForecastRequest().withNewCampaignExampleType(0).withStrategy(campaignStrategy);
        controller.getCpmTrafficLightPrediction(request, clientInfo.getLogin());

        ArgumentCaptor<String> requestId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> clientLogin = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> operatorLogin = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CampaignPredictionRequest> campaignPredictionRequest =
                ArgumentCaptor.forClass(CampaignPredictionRequest.class);

        Mockito.verify(client, Mockito.times(1)).getTrafficLightPrediction(requestId.capture(),
                operatorLogin.capture(), clientLogin.capture(), campaignPredictionRequest.capture());

        CampaignPredictionRequest inventoriRequest = campaignPredictionRequest.getValue();

        assertThat("В запросе правильные значения rf и rfReset", inventoriRequest.getParameters().getRf(),
                is(new CampaignParametersRf(0, 0)));
    }

    @Test
    public void getCpmTrafficLightPrediction_CpmBannerWithAdaptiveCreative_AllBlockSizesUsedInRequest()
            throws JsonProcessingException {
        successResponse();

        createCpmBannerCampaign();
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(defaultAdaptive(clientInfo.getClientId(), null), clientInfo);
        createFullCpmBannerAdGroup(creativeInfo.getCreativeId());

        CampaignStrategy strategy = defaultStrategy();
        CpmForecastRequest request = new CpmForecastRequest().withCampaignId(campaignInfo.getCampaignId())
                .withStrategy(strategy);
        controller.getCpmTrafficLightPrediction(request, clientInfo.getLogin());

        ArgumentCaptor<String> requestId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> clientLogin = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> operatorLogin = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CampaignPredictionRequest> campaignPredictionRequest =
                ArgumentCaptor.forClass(CampaignPredictionRequest.class);

        Mockito.verify(client, Mockito.times(1)).getTrafficLightPrediction(requestId.capture(),
                operatorLogin.capture(), clientLogin.capture(), campaignPredictionRequest.capture());

        CampaignPredictionRequest inventoriRequest = campaignPredictionRequest.getValue();

        List<Target> targets = inventoriRequest.getTargets();
        assumeThat("В запросе должен быть 1 таргет", targets, hasSize(1));

        assertThat("Таргет должен содержать все поддерживаемые размеры блоков", new HashSet<>(targets.get(0).getBlockSizes()),
                is(ALLOWED_BLOCK_SIZES));
    }

    @Test
    public void getCpmTrafficLightPrediction_CpmBannerWithNoAdaptiveCreative_AllBlockSizesUsedInRequest()
            throws JsonProcessingException {
        successResponse();

        createCpmBannerCampaign();
        Creative creative = defaultCanvas(clientInfo.getClientId(), null);
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(creative, clientInfo);
        createFullCpmBannerAdGroup(creativeInfo.getCreativeId());

        CampaignStrategy strategy = defaultStrategy();
        CpmForecastRequest request = new CpmForecastRequest().withCampaignId(campaignInfo.getCampaignId())
                .withStrategy(strategy);
        controller.getCpmTrafficLightPrediction(request, clientInfo.getLogin());

        ArgumentCaptor<String> requestId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> clientLogin = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> operatorLogin = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CampaignPredictionRequest> campaignPredictionRequest =
                ArgumentCaptor.forClass(CampaignPredictionRequest.class);

        Mockito.verify(client, Mockito.times(1)).getTrafficLightPrediction(requestId.capture(),
                operatorLogin.capture(), clientLogin.capture(), campaignPredictionRequest.capture());

        CampaignPredictionRequest inventoriRequest = campaignPredictionRequest.getValue();

        List<Target> targets = inventoriRequest.getTargets();
        assumeThat("В запросе должен быть 1 таргет", targets, hasSize(1));

        assertThat("Таргет должен содержать только размер креатива", targets.get(0).getBlockSizes(),
                is(singletonList(new BlockSize(creative.getWidth().intValue(), creative.getHeight().intValue()))));
    }

    // cpm_yndx_frontpage

    @Test
    public void getCpmTrafficLightPrediction_FullCpmYndxFrontpageCampaign_RequestWithNoTrafficType_TrafficTypeAndGroupTypeAreCorrect()
            throws JsonProcessingException {
        successResponse();
        createCpmYndxFrontpageCampaign(ImmutableSet.of(FRONTPAGE));
        Creative creative = defaultCanvas(clientInfo.getClientId(), null);
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(creative, clientInfo);
        createFullCpmYndxFrontpageAdGroup(creativeInfo.getCreativeId());
        createFullCpmYndxFrontpageAdGroup(creativeInfo.getCreativeId());

        CampaignStrategy strategy = defaultStrategy();
        CpmForecastRequest request = new CpmForecastRequest().withCampaignId(campaignInfo.getCampaignId())
                .withStrategy(strategy);
        controller.getCpmTrafficLightPrediction(request, clientInfo.getLogin());

        ArgumentCaptor<String> requestId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> clientLogin = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> operatorLogin = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CampaignPredictionRequest> campaignPredictionRequest =
                ArgumentCaptor.forClass(CampaignPredictionRequest.class);

        Mockito.verify(client, Mockito.times(1)).getTrafficLightPrediction(requestId.capture(),
                operatorLogin.capture(), clientLogin.capture(), campaignPredictionRequest.capture());

        CampaignPredictionRequest inventoriRequest = campaignPredictionRequest.getValue();

        List<Target> targets = inventoriRequest.getTargets();
        assumeThat("В запросе должно быть 2 таргета", targets, hasSize(2));

        targets.forEach(target -> assertThat("Тип нацеливания должен быть добавлен на уровне таргета", target.getGroupType(),
                is(GroupType.MAIN_PAGE_AND_NTP)));
    }

    @Test
    public void getCpmTrafficLightPrediction_FullCpmYndxFrontpageCampaign_RequestWithFrontpageType_TrafficTypeAndGroupTypeAreCorrect()
            throws JsonProcessingException {
        successResponse();
        createCpmYndxFrontpageCampaign(ImmutableSet.of(FRONTPAGE));
        Creative creative = defaultCanvas(clientInfo.getClientId(), null);
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(creative, clientInfo);
        createFullCpmYndxFrontpageAdGroup(creativeInfo.getCreativeId());
        createFullCpmYndxFrontpageAdGroup(creativeInfo.getCreativeId());

        CampaignStrategy strategy = defaultStrategy();
        CpmForecastRequest request = new CpmForecastRequest().withCampaignId(campaignInfo.getCampaignId())
                .withYndxFrontpagePageType(YndxFrontpagePageType.ALL)
                .withStrategy(strategy);
        controller.getCpmTrafficLightPrediction(request, clientInfo.getLogin());

        ArgumentCaptor<String> requestId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> clientLogin = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> operatorLogin = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CampaignPredictionRequest> campaignPredictionRequest =
                ArgumentCaptor.forClass(CampaignPredictionRequest.class);

        Mockito.verify(client, Mockito.times(1)).getTrafficLightPrediction(requestId.capture(),
                operatorLogin.capture(), clientLogin.capture(), campaignPredictionRequest.capture());

        CampaignPredictionRequest inventoriRequest = campaignPredictionRequest.getValue();

        List<Target> targets = inventoriRequest.getTargets();
        assumeThat("В запросе должно быть 2 таргета", targets, hasSize(2));

        targets.forEach(target -> assertThat("Тип нацеливания должен быть добавлен на уровне таргета", target.getGroupType(),
                is(GroupType.MAIN_PAGE_AND_NTP)));
    }

    @Test
    public void getCpmTrafficLightPrediction_RequestWithAdGroupGeos_GeoUpdated() throws JsonProcessingException {
        successResponse();

        CpmForecastRequest request = new CpmForecastRequest()
                .withCampaignId(campaignInfo.getCampaignId())
                .withAdGroupGeos(singletonList(new AdGroupGeo(adGroupInfo.getAdGroupId(), "555,666,777")));
        controller.getCpmTrafficLightPrediction(request, clientInfo.getLogin());

        ArgumentCaptor<String> requestId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> clientLogin = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> operatorLogin = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CampaignPredictionRequest> campaignPredictionRequest =
                ArgumentCaptor.forClass(CampaignPredictionRequest.class);

        Mockito.verify(client, Mockito.times(1)).getTrafficLightPrediction(requestId.capture(),
                operatorLogin.capture(), clientLogin.capture(), campaignPredictionRequest.capture());

        CampaignPredictionRequest inventoriRequest = campaignPredictionRequest.getValue();

        assertThat("Гео должно обновиться", inventoriRequest.getTargets().get(0).getRegions(),
                is(ImmutableSet.of(555, 666, 777)));
    }

    @Test
    public void getCpmTrafficLightPrediction_RequestWithAdGroupGeos_AnotherAdGroupId_GeoNotUpdated()
            throws JsonProcessingException {
        successResponse();

        CpmForecastRequest request = new CpmForecastRequest()
                .withCampaignId(campaignInfo.getCampaignId())
                .withAdGroupGeos(singletonList(new AdGroupGeo(adGroupInfo.getAdGroupId() + 1, "555,666,777")));
        controller.getCpmTrafficLightPrediction(request, clientInfo.getLogin());

        ArgumentCaptor<String> requestId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> clientLogin = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> operatorLogin = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CampaignPredictionRequest> campaignPredictionRequest =
                ArgumentCaptor.forClass(CampaignPredictionRequest.class);

        Mockito.verify(client, Mockito.times(1)).getTrafficLightPrediction(requestId.capture(),
                operatorLogin.capture(), clientLogin.capture(), campaignPredictionRequest.capture());

        CampaignPredictionRequest inventoriRequest = campaignPredictionRequest.getValue();

        assertThat("Гео не должно обновиться", inventoriRequest.getTargets().get(0).getRegions(),
                is(ImmutableSet.of(225)));
    }

    private void successResponse() throws JsonProcessingException {
        when(client.getTrafficLightPrediction(anyString(), nullable(String.class), nullable(String.class),
                any(CampaignPredictionRequest.class)))
                .thenReturn(new TrafficLightPredictionResponse(emptyList(), 1, 12334000000L));
    }

    private void responseWithErrors() throws JsonProcessingException {
        when(client.getTrafficLightPrediction(anyString(), nullable(String.class), nullable(String.class),
                any(CampaignPredictionRequest.class))).thenReturn(
                new CampaignPredictionLowReachResponse(emptyList(), REACH_LESS_THAN));
    }

    private void makeRequestAndCheckTargetTags(List<String> expectedTargetTags) throws JsonProcessingException {
        CampaignStrategy strategy = defaultStrategy();
        CpmForecastRequest request = new CpmForecastRequest().withCampaignId(campaignInfo.getCampaignId())
                .withStrategy(strategy);
        controller.getCpmTrafficLightPrediction(request, clientInfo.getLogin());

        ArgumentCaptor<String> requestId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> clientLogin = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> operatorLogin = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CampaignPredictionRequest> campaignPredictionRequest =
                ArgumentCaptor.forClass(CampaignPredictionRequest.class);

        Mockito.verify(client, Mockito.times(1)).getTrafficLightPrediction(requestId.capture(),
                operatorLogin.capture(), clientLogin.capture(), campaignPredictionRequest.capture());

        CampaignPredictionRequest inventoriRequest = campaignPredictionRequest.getValue();

        List<Target> targets = inventoriRequest.getTargets();
        assumeThat("В запросе должен быть 1 таргет", targets, hasSize(1));
        Target target = targets.get(0);

        assertThat("Таргет должен иметь правильный target_tags", target.getTargetTags(),
                containsInAnyOrder(expectedTargetTags.toArray()));
    }
}
