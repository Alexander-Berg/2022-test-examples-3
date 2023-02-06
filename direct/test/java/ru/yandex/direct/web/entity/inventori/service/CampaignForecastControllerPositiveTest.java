package ru.yandex.direct.web.entity.inventori.service;

import java.util.HashSet;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang.StringUtils;
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
import ru.yandex.direct.core.entity.retargeting.model.CryptaInterestType;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.inventori.InventoriClient;
import ru.yandex.direct.inventori.model.request.BlockSize;
import ru.yandex.direct.inventori.model.request.CampaignParametersRf;
import ru.yandex.direct.inventori.model.request.CampaignPredictionRequest;
import ru.yandex.direct.inventori.model.request.GroupType;
import ru.yandex.direct.inventori.model.request.Target;
import ru.yandex.direct.inventori.model.response.CampaignPredictionAvailableResponse;
import ru.yandex.direct.inventori.model.response.CampaignPredictionLowReachResponse;
import ru.yandex.direct.web.core.entity.inventori.model.AdGroupGeo;
import ru.yandex.direct.web.core.entity.inventori.model.Brandsafety;
import ru.yandex.direct.web.core.entity.inventori.model.CampaignStrategy;
import ru.yandex.direct.web.core.entity.inventori.model.CpmForecastRequest;
import ru.yandex.direct.web.core.entity.inventori.model.CpmForecastSuccessResult;
import ru.yandex.direct.web.core.entity.inventori.model.ImpressionLimit;
import ru.yandex.direct.web.core.entity.inventori.model.TrafficTypeCorrectionsWeb;
import ru.yandex.direct.web.core.entity.inventori.model.YndxFrontpagePageType;
import ru.yandex.direct.web.core.entity.inventori.service.InventoriService;
import ru.yandex.direct.web.core.entity.inventori.service.InventoriWebService;
import ru.yandex.direct.web.core.entity.inventori.service.InventoriWebValidationService;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource;
import ru.yandex.direct.web.entity.inventori.controller.InventoriController;
import ru.yandex.direct.web.entity.inventori.model.CpmForecastResponse;
import ru.yandex.direct.web.validation.kernel.ValidationResultConversionService;
import ru.yandex.direct.web.validation.model.WebValidationResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
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
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultAdaptive;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCanvas;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.web.entity.inventori.service.InventoriTestWebDefects.webContainsKeywordAdGroups;
import static ru.yandex.direct.web.entity.inventori.service.InventoriTestWebDefects.webLowReach;
import static ru.yandex.direct.web.testing.data.TestCpmForecastRequest.defaultRequest;
import static ru.yandex.direct.web.testing.data.TestCpmForecastRequest.defaultStrategy;

public class CampaignForecastControllerPositiveTest extends CampaignForecastControllerTestBase {

    private static final int SECTORS_COUNT = 3;

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
    private Long creativeId;

    @Before
    @SuppressWarnings("Duplicates")
    public void before() {
        super.before();
        adGroupInfo = steps.adGroupSteps().createActiveCpmVideoAdGroup(campaignInfo);
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(clientInfo);
        Long campaignId = campaignInfo.getCampaignId();
        Long adGroupId = adGroupInfo.getAdGroupId();
        creativeId = creativeInfo.getCreativeId();

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

        steps.cryptaGoalsSteps().addGoals((Goal) new Goal()
                .withId(4294967312L)
                .withName("Табак")
                .withInterestType(CryptaInterestType.all)
                .withKeyword("931")
                .withKeywordValue("4294967312"));
    }

    // cpm_banner

    @Test
    public void getCampaignForecast_SuccessResponse_NoErrors() throws JsonProcessingException {
        successResponse();
        ResponseEntity<WebResponse> response =
                controller.getCampaignForecast(defaultRequest(campaignInfo.getCampaignId()), clientInfo.getLogin());

        assertThat("Должен вернуться результат без ошибок", response.getStatusCode().value(), is(SUCCESS_CODE));
    }

    @Test
    public void getCampaignForecast_SuccessResponse_CorrectRequestId() throws JsonProcessingException {
        successResponse();
        CpmForecastResponse result =
                (CpmForecastResponse) getSuccessResponse(defaultRequest(campaignInfo.getCampaignId()));

        assertThat("Request id не должен быть пустым", StringUtils.isBlank(result.requestId()), is(false));
    }

    @Test
    public void getCampaignForecast_SuccessResponse_ThreeSectorsReturned() throws JsonProcessingException {
        successResponse();
        CpmForecastSuccessResult result = ((CpmForecastResponse) getSuccessResponse(
                defaultRequest(campaignInfo.getCampaignId()))).getResult();

        assertThat("Должно вернуться три сектора", result.getSectors(), hasSize(SECTORS_COUNT));
    }

    @Test
    public void getCampaignForecast_SuccessResponse_SectorsHaveCorrectColors() throws JsonProcessingException {
        successResponse();
        CpmForecastSuccessResult result = ((CpmForecastResponse) getSuccessResponse(
                defaultRequest(campaignInfo.getCampaignId()))).getResult();

        assumeThat("Должно вернуться три сектора", result.getSectors(), hasSize(3));

        assertThat("Первый сектор должен быть красного цвета",
                result.getSectors().get(0).getColor(), is("red"));
        assertThat("Второй сектор должен быть желтого цвета",
                result.getSectors().get(1).getColor(), is("yellow"));
        assertThat("Третий сектор должен быть зеленого цвета",
                result.getSectors().get(2).getColor(), is("green"));
    }

    @Test
    public void getCampaignForecast_SuccessResponse_SectorsHaveCorrectEndings() throws JsonProcessingException {
        successResponse();
        ResponseEntity<WebResponse> response =
                controller.getCampaignForecast(defaultRequest(campaignInfo.getCampaignId()), clientInfo.getLogin());

        assumeThat("Должен вернуться результат без ошибок", response.getStatusCode().value(), is(SUCCESS_CODE));

        CpmForecastSuccessResult result = ((CpmForecastResponse) response.getBody()).getResult();

        assumeThat("Должно вернуться три сектора", result.getSectors(), hasSize(3));

        assertThat("Первый сектор должен заканчиваться раньше второго",
                result.getSectors().get(0).getMax(), lessThanOrEqualTo(result.getSectors().get(1).getMin()));
        assertThat("Второй сектор должен заканчиваться раньше третьего",
                result.getSectors().get(1).getMax(), lessThanOrEqualTo(result.getSectors().get(2).getMin()));
    }

    @Test
    public void getCampaignForecast_SuccessResponse_ResultHasCorrectRecommendedCpm() throws JsonProcessingException {
        successResponse();
        CpmForecastSuccessResult result = ((CpmForecastResponse) getSuccessResponse(
                defaultRequest(campaignInfo.getCampaignId()))).getResult();

        assertThat("Должно вернуться правильное значение рекомендуемого cpm",
                result.getRecommendedPrice(), is(123.456789));
    }

    @Test
    public void getCampaignForecast_WithCorrectionsAndSuccessResponse_ResultHasCorrectRecommendedCpm()
            throws JsonProcessingException {
        successResponse();
        CpmForecastRequest request = defaultRequest(campaignInfo.getCampaignId());
        TrafficTypeCorrectionsWeb trafficTypeCorrectionsWeb = new TrafficTypeCorrectionsWeb(0, 100, 1200, 1300, null,
                null);
        request.withTrafficTypeCorrections(trafficTypeCorrectionsWeb);
        CpmForecastSuccessResult result = ((CpmForecastResponse) getSuccessResponse(request)).getResult();

        assertThat("Должно вернуться правильное значение рекомендуемого cpm",
                result.getRecommendedPrice(), is(123.456789));
    }

    @Test
    public void getCampaignForecast_SuccessResponse_CampaignHasAdGroupWithKeywords_ErrorIsAdded()
            throws JsonProcessingException {
        successResponse();
        createKeywordCpmAdGroup();
        CpmForecastRequest request = defaultRequest(campaignInfo.getCampaignId());

        WebValidationResult vr = ((CpmForecastResponse) getSuccessResponse(request)).validationResult();
        WebValidationResult expectedResult =
                new WebValidationResult().addErrors(webContainsKeywordAdGroups(request));

        Assertions.assertThat(vr).isEqualToComparingFieldByFieldRecursively(expectedResult);
    }

    @Test
    public void getCampaignForecast_LowReachResponse_ResponseWithSuccessStatus() throws JsonProcessingException {
        responseWithErrors();
        ResponseEntity<WebResponse> response =
                controller.getCampaignForecast(defaultRequest(campaignInfo.getCampaignId()), clientInfo.getLogin());

        assertThat("Должен вернуться результат с ошибкой", response.getStatusCodeValue(), is(SUCCESS_CODE));
    }

    @Test
    public void getCampaignForecast_LowReachResponse_ForecastResultIsNull() throws JsonProcessingException {
        responseWithErrors();
        CpmForecastResponse response =
                (CpmForecastResponse) getSuccessResponse(defaultRequest(campaignInfo.getCampaignId()));

        assertThat("Результат прогноза должен быть null", response.getResult(), nullValue());
    }

    @Test
    public void getCampaignForecast_LowReachResponse_ResponseHasCorrectError() throws JsonProcessingException {
        responseWithErrors();
        CpmForecastRequest request = defaultRequest(campaignInfo.getCampaignId());

        WebValidationResult vr = ((CpmForecastResponse) getSuccessResponse(request)).validationResult();
        WebValidationResult expectedResult = new WebValidationResult().addErrors(webLowReach(request.toString(),
                REACH_LESS_THAN));

        Assertions.assertThat(vr).isEqualToComparingFieldByFieldRecursively(expectedResult);
    }

    @Test
    public void getCampaignForecast_StrategyWithNullCpm_SuccessResponse() throws JsonProcessingException {
        successResponse();
        Long campaignId = campaignInfo.getCampaignId();
        CampaignStrategy strategy = defaultStrategy().withCpm(null);
        CpmForecastRequest request = new CpmForecastRequest().withCampaignId(campaignId).withStrategy(strategy);

        ResponseEntity<WebResponse> response = controller.getCampaignForecast(request, clientInfo.getLogin());

        assertThat("Должен вернуться успешный status code", response.getStatusCode().value(), is(SUCCESS_CODE));
    }

    @Test
    public void getCampaignForecast_StrategyWithNullCpm_NoErrors() throws JsonProcessingException {
        successResponse();
        Long campaignId = campaignInfo.getCampaignId();
        CampaignStrategy strategy = defaultStrategy().withCpm(null);
        CpmForecastRequest request = new CpmForecastRequest().withCampaignId(campaignId).withStrategy(strategy);

        WebValidationResult vr = ((CpmForecastResponse) getSuccessResponse(request)).validationResult();
        WebValidationResult expectedResult = new WebValidationResult();

        Assertions.assertThat(vr).isEqualToComparingFieldByFieldRecursively(expectedResult);
    }

    @Test
    public void getCampaignForecast_NewCpmBannerCampaign_NoErrors() throws JsonProcessingException {
        successResponse();
        CampaignStrategy strategy = defaultStrategy().withCpm(null);
        CpmForecastRequest request = new CpmForecastRequest().withNewCampaignExampleType(0).withStrategy(strategy);

        WebValidationResult vr = ((CpmForecastResponse) getSuccessResponse(request)).validationResult();
        WebValidationResult expectedResult = new WebValidationResult();

        Assertions.assertThat(vr).isEqualToComparingFieldByFieldRecursively(expectedResult);
    }

    @Test
    public void getCampaignForecast_ExistingCpmBannerCampaign_RequestWithZeroRfAndNonZeroRfReset_InventoriRequestHasCorrectRfAndRfReset()
            throws JsonProcessingException {
        successResponse();

        CampaignStrategy strategy = defaultStrategy().withImpressionLimit(new ImpressionLimit(0L, 10L));
        CpmForecastRequest request = new CpmForecastRequest().withCampaignId(campaignInfo.getCampaignId()).withStrategy(strategy);
        controller.getCampaignForecast(request, clientInfo.getLogin());

        ArgumentCaptor<String> requestId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> clientLogin = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> operatorLogin = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CampaignPredictionRequest> campaignPredictionRequest =
                ArgumentCaptor.forClass(CampaignPredictionRequest.class);

        Mockito.verify(client, Mockito.times(1)).getCampaignPrediction(requestId.capture(),
                operatorLogin.capture(), clientLogin.capture(), campaignPredictionRequest.capture());

        CampaignPredictionRequest inventoriRequest = campaignPredictionRequest.getValue();

        assertThat("В запросе правильные значения rf и rfReset", inventoriRequest.getParameters().getRf(),
                is(new CampaignParametersRf(0, 0)));
    }

    @Test
    public void getCampaignForecast_NewCpmBannerCampaign_RequestWithZeroRfAndNonZeroRfReset_InventoriRequestHasCorrectRfAndRfReset()
            throws JsonProcessingException {
        successResponse();

        CampaignStrategy strategy = defaultStrategy().withImpressionLimit(new ImpressionLimit(0L, 10L));
        CpmForecastRequest request = new CpmForecastRequest().withNewCampaignExampleType(0).withStrategy(strategy);
        controller.getCampaignForecast(request, clientInfo.getLogin());

        ArgumentCaptor<String> requestId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> clientLogin = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> operatorLogin = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CampaignPredictionRequest> campaignPredictionRequest =
                ArgumentCaptor.forClass(CampaignPredictionRequest.class);

        Mockito.verify(client, Mockito.times(1)).getCampaignPrediction(requestId.capture(),
                operatorLogin.capture(), clientLogin.capture(), campaignPredictionRequest.capture());

        CampaignPredictionRequest inventoriRequest = campaignPredictionRequest.getValue();

        assertThat("В запросе правильные значения rf и rfReset", inventoriRequest.getParameters().getRf(),
                is(new CampaignParametersRf(0, 0)));
    }

    @Test
    public void getCampaignForecast_CpmBannerWithAdaptiveCreative_AllBlockSizesUsedInRequest() throws JsonProcessingException {
        successResponse();

        createCpmBannerCampaign();
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(defaultAdaptive(clientInfo.getClientId(), null), clientInfo);
        createFullCpmBannerAdGroup(creativeInfo.getCreativeId());

        CampaignStrategy strategy = defaultStrategy();
        CpmForecastRequest request = new CpmForecastRequest().withCampaignId(campaignInfo.getCampaignId())
                .withStrategy(strategy);
        controller.getCampaignForecast(request, clientInfo.getLogin());

        ArgumentCaptor<String> requestId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> clientLogin = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> operatorLogin = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CampaignPredictionRequest> campaignPredictionRequest =
                ArgumentCaptor.forClass(CampaignPredictionRequest.class);

        Mockito.verify(client, Mockito.times(1)).getCampaignPrediction(requestId.capture(),
                operatorLogin.capture(), clientLogin.capture(), campaignPredictionRequest.capture());

        CampaignPredictionRequest inventoriRequest = campaignPredictionRequest.getValue();

        List<Target> targets = inventoriRequest.getTargets();
        assumeThat("В запросе должен быть 1 таргет", targets, hasSize(1));
        Target target = targets.get(0);

        assertThat("Таргет должен содержать все поддерживаемые размеры блоков", new HashSet<>(target.getBlockSizes()),
                is(ALLOWED_BLOCK_SIZES));
    }

    @Test
    public void getCampaignForecast_CpmBannerWithNoAdaptiveCreative_AllBlockSizesUsedInRequest() throws JsonProcessingException {
        successResponse();

        createCpmBannerCampaign();
        Creative creative = defaultCanvas(clientInfo.getClientId(), null);
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(creative, clientInfo);
        createFullCpmBannerAdGroup(creativeInfo.getCreativeId());

        CampaignStrategy strategy = defaultStrategy();
        CpmForecastRequest request = new CpmForecastRequest().withCampaignId(campaignInfo.getCampaignId())
                .withStrategy(strategy);
        controller.getCampaignForecast(request, clientInfo.getLogin());

        ArgumentCaptor<String> requestId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> clientLogin = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> operatorLogin = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CampaignPredictionRequest> campaignPredictionRequest =
                ArgumentCaptor.forClass(CampaignPredictionRequest.class);

        Mockito.verify(client, Mockito.times(1)).getCampaignPrediction(requestId.capture(),
                operatorLogin.capture(), clientLogin.capture(), campaignPredictionRequest.capture());

        CampaignPredictionRequest inventoriRequest = campaignPredictionRequest.getValue();

        List<Target> targets = inventoriRequest.getTargets();
        assumeThat("В запросе должен быть 1 таргет", targets, hasSize(1));
        Target target = targets.get(0);

        assertThat("Таргет должен содержать только размер креатива", target.getBlockSizes(),
                is(singletonList(new BlockSize(creative.getWidth().intValue(), creative.getHeight().intValue()))));
    }

    @Test
    public void getCampaignForecast_NewCpmBannerCampaignBrandSafety_RequestHasBrandsafetyList() throws JsonProcessingException {
        successResponse();
        CampaignStrategy strategy = defaultStrategy().withCpm(null);
        CpmForecastRequest request = new CpmForecastRequest().withNewCampaignExampleType(0)
                .withStrategy(strategy)
                .withBrandsafety(
                        new Brandsafety().setEnabled(true)
                );

        controller.getCampaignForecast(request, clientInfo.getLogin());

        ArgumentCaptor<String> requestId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> clientLogin = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> operatorLogin = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CampaignPredictionRequest> campaignPredictionRequest =
                ArgumentCaptor.forClass(CampaignPredictionRequest.class);

        Mockito.verify(client, Mockito.times(1)).getCampaignPrediction(requestId.capture(),
                operatorLogin.capture(), clientLogin.capture(), campaignPredictionRequest.capture());

        CampaignPredictionRequest inventoriRequest = campaignPredictionRequest.getValue();

        List<Target> targets = inventoriRequest.getTargets();
        assumeThat("В запросе должен быть 1 таргет", targets, hasSize(1));

        Target target = targets.get(0);
        assertThat("Таргет должен содержать блок excluded_bs_categories",
                target.getExcludedBsCategories(), hasItem("931:4294967312"));
    }

    // cpm_yndx_frontpage

    @Test
    public void getCampaignForecast_FullCpmYndxFrontpageCampaign_RequestWithNoTrafficType_TrafficTypeAndGroupTypeAreCorrect()
            throws JsonProcessingException {
        successResponse();
        createCpmYndxFrontpageCampaign(ImmutableSet.of(FRONTPAGE));
        Creative creative = defaultCanvas(clientInfo.getClientId(), null);
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(creative, clientInfo);

        createFullCpmYndxFrontpageAdGroup(creativeInfo.getCreativeId());
        createFullCpmYndxFrontpageAdGroup(creativeInfo.getCreativeId());

        CampaignStrategy strategy = defaultStrategy().withCpm(null);
        CpmForecastRequest request = new CpmForecastRequest().withCampaignId(campaignInfo.getCampaignId())
                .withStrategy(strategy);
        controller.getCampaignForecast(request, clientInfo.getLogin());

        ArgumentCaptor<String> requestId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> clientLogin = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> operatorLogin = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CampaignPredictionRequest> campaignPredictionRequest =
                ArgumentCaptor.forClass(CampaignPredictionRequest.class);

        Mockito.verify(client, Mockito.times(1)).getCampaignPrediction(requestId.capture(),
                operatorLogin.capture(), clientLogin.capture(), campaignPredictionRequest.capture());

        CampaignPredictionRequest inventoriRequest = campaignPredictionRequest.getValue();

        List<Target> targets = inventoriRequest.getTargets();
        assumeThat("В запросе должно быть 2 таргета", targets, hasSize(2));

        targets.forEach(target -> assertThat("Тип нацеливания должен быть добавлен на уровне таргета", target.getGroupType(),
                is(GroupType.MAIN_PAGE_AND_NTP)));
    }

    @Test
    public void getCampaignForecast_FullCpmYndxFrontpageCampaign_RequestWithFrontpageType_TrafficTypeAndGroupTypeAreCorrect()
            throws JsonProcessingException {
        successResponse();
        createCpmYndxFrontpageCampaign(ImmutableSet.of(FRONTPAGE));
        Creative creative = defaultCanvas(clientInfo.getClientId(), null);
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(creative, clientInfo);

        createFullCpmYndxFrontpageAdGroup(creativeInfo.getCreativeId());
        createFullCpmYndxFrontpageAdGroup(creativeInfo.getCreativeId());

        CampaignStrategy strategy = defaultStrategy().withCpm(null);
        CpmForecastRequest request = new CpmForecastRequest().withCampaignId(campaignInfo.getCampaignId())
                .withYndxFrontpagePageType(YndxFrontpagePageType.ALL)
                .withStrategy(strategy);
        controller.getCampaignForecast(request, clientInfo.getLogin());

        ArgumentCaptor<String> requestId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> clientLogin = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> operatorLogin = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CampaignPredictionRequest> campaignPredictionRequest =
                ArgumentCaptor.forClass(CampaignPredictionRequest.class);

        Mockito.verify(client, Mockito.times(1)).getCampaignPrediction(requestId.capture(),
                operatorLogin.capture(), clientLogin.capture(), campaignPredictionRequest.capture());

        CampaignPredictionRequest inventoriRequest = campaignPredictionRequest.getValue();

        List<Target> targets = inventoriRequest.getTargets();
        assumeThat("В запросе должно быть 2 таргета", targets, hasSize(2));

        targets.forEach(target -> assertThat("Тип нацеливания должен быть добавлен на уровне таргета", target.getGroupType(),
                is(GroupType.MAIN_PAGE_AND_NTP)));
    }

    @Test
    public void getCampaignForecast_NewCpmYndxFrontpageCampaign_RequestWithTrafficType_TrafficTypeIsCorrect()
            throws JsonProcessingException {
        successResponse();

        CampaignStrategy strategy = defaultStrategy().withCpm(null);
        CpmForecastRequest request = new CpmForecastRequest().withNewCampaignExampleType(1)
                .withYndxFrontpagePageType(YndxFrontpagePageType.ALL)
                .withStrategy(strategy);
        controller.getCampaignForecast(request, clientInfo.getLogin());

        ArgumentCaptor<String> requestId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> clientLogin = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> operatorLogin = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CampaignPredictionRequest> campaignPredictionRequest =
                ArgumentCaptor.forClass(CampaignPredictionRequest.class);

        Mockito.verify(client, Mockito.times(1)).getCampaignPrediction(requestId.capture(),
                operatorLogin.capture(), clientLogin.capture(), campaignPredictionRequest.capture());

        CampaignPredictionRequest inventoriRequest = campaignPredictionRequest.getValue();

        List<Target> targets = inventoriRequest.getTargets();
        assumeThat("В запросе не должно быть 2 таргета", targets, nullValue());
    }

    @Test
    public void getCampaignForecast_RequestWithAdGroupGeos_GeoUpdated() throws JsonProcessingException {
        successResponse();

        CpmForecastRequest request = new CpmForecastRequest()
                .withCampaignId(campaignInfo.getCampaignId())
                .withStrategy(defaultStrategy())
                .withAdGroupGeos(singletonList(new AdGroupGeo(adGroupInfo.getAdGroupId(), "555,666,777")));
        controller.getCampaignForecast(request, clientInfo.getLogin());

        ArgumentCaptor<String> requestId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> clientLogin = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> operatorLogin = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CampaignPredictionRequest> campaignPredictionRequest =
                ArgumentCaptor.forClass(CampaignPredictionRequest.class);

        Mockito.verify(client, Mockito.times(1)).getCampaignPrediction(requestId.capture(),
                operatorLogin.capture(), clientLogin.capture(), campaignPredictionRequest.capture());

        CampaignPredictionRequest inventoriRequest = campaignPredictionRequest.getValue();

        assertThat("Гео должно обновиться", inventoriRequest.getTargets().get(0).getRegions(),
                is(ImmutableSet.of(555, 666, 777)));
    }

    @Test
    public void getCampaignForecast_RequestWithAdGroupGeos_AnotherAdGroupId_GeoNotUpdated()
            throws JsonProcessingException {
        successResponse();

        CpmForecastRequest request = new CpmForecastRequest()
                .withCampaignId(campaignInfo.getCampaignId())
                .withStrategy(defaultStrategy())
                .withAdGroupGeos(singletonList(new AdGroupGeo(adGroupInfo.getAdGroupId() + 1, "555,666,777")));
        controller.getCampaignForecast(request, clientInfo.getLogin());

        ArgumentCaptor<String> requestId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> clientLogin = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> operatorLogin = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CampaignPredictionRequest> campaignPredictionRequest =
                ArgumentCaptor.forClass(CampaignPredictionRequest.class);

        Mockito.verify(client, Mockito.times(1)).getCampaignPrediction(requestId.capture(),
                operatorLogin.capture(), clientLogin.capture(), campaignPredictionRequest.capture());

        CampaignPredictionRequest inventoriRequest = campaignPredictionRequest.getValue();

        assertThat("Гео не должно обновиться", inventoriRequest.getTargets().get(0).getRegions(),
                is(ImmutableSet.of(225)));
    }

    private WebResponse getSuccessResponse(CpmForecastRequest request) throws JsonProcessingException {
        ResponseEntity<WebResponse> response = controller.getCampaignForecast(request, clientInfo.getLogin());

        assumeThat("Должен вернуться успешный status code", response.getStatusCode().value(),
                is(SUCCESS_CODE));

        return response.getBody();
    }

    private void successResponse() throws JsonProcessingException {
        when(client.getCampaignPrediction(anyString(), nullable(String.class), nullable(String.class),
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

    private void responseWithErrors() throws JsonProcessingException {
        when(client.getCampaignPrediction(anyString(), nullable(String.class), nullable(String.class),
                any(CampaignPredictionRequest.class))).thenReturn(
                new CampaignPredictionLowReachResponse(emptyList(), REACH_LESS_THAN));
    }

    private void makeRequestAndCheckTargetTags(List<String> expectedTargetTags) throws JsonProcessingException {
        CampaignStrategy strategy = defaultStrategy();
        CpmForecastRequest request = new CpmForecastRequest().withCampaignId(campaignInfo.getCampaignId())
                .withStrategy(strategy);
        controller.getCampaignForecast(request, clientInfo.getLogin());

        ArgumentCaptor<String> requestId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> clientLogin = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> operatorLogin = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CampaignPredictionRequest> campaignPredictionRequest =
                ArgumentCaptor.forClass(CampaignPredictionRequest.class);

        Mockito.verify(client, Mockito.times(1)).getCampaignPrediction(requestId.capture(),
                operatorLogin.capture(), clientLogin.capture(), campaignPredictionRequest.capture());

        CampaignPredictionRequest inventoriRequest = campaignPredictionRequest.getValue();

        List<Target> targets = inventoriRequest.getTargets();
        assumeThat("В запросе должен быть 1 таргет", targets, hasSize(1));
        Target target = targets.get(0);

        assertThat("Таргет должен иметь правильный target_tags", target.getTargetTags(),
                containsInAnyOrder(expectedTargetTags.toArray()));
    }
}
