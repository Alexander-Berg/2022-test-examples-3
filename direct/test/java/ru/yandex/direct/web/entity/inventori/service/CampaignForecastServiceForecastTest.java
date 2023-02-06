package ru.yandex.direct.web.entity.inventori.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.crypta.repository.CryptaSegmentRepository;
import ru.yandex.direct.core.entity.currency.service.CurrencyRateService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.inventori.service.CampaignInfoCollector;
import ru.yandex.direct.core.entity.inventori.service.InventoriServiceCore;
import ru.yandex.direct.core.entity.pricepackage.service.PricePackageService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.security.DirectAuthentication;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.inventori.InventoriClient;
import ru.yandex.direct.inventori.model.request.CampaignPredictionRequest;
import ru.yandex.direct.inventori.model.response.CampaignPredictionAvailableResponse;
import ru.yandex.direct.inventori.model.response.CampaignPredictionLowReachResponse;
import ru.yandex.direct.inventori.model.response.CampaignPredictionResponse;
import ru.yandex.direct.inventori.model.response.error.ErrorType;
import ru.yandex.direct.inventori.model.response.error.PredictionResponseInternalError;
import ru.yandex.direct.inventori.model.response.error.PredictionResponseInvalidBudgetError;
import ru.yandex.direct.inventori.model.response.error.PredictionResponseInvalidCpmError;
import ru.yandex.direct.inventori.model.response.error.PredictionResponseInvalidDatesError;
import ru.yandex.direct.inventori.model.response.error.PredictionResponseInvalidRequestError;
import ru.yandex.direct.inventori.model.response.error.PredictionResponseInvalidRfError;
import ru.yandex.direct.inventori.model.response.error.PredictionResponseUnknownSegmentsError;
import ru.yandex.direct.inventori.model.response.error.PredictionResponseUnsupportedSegmentsError;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.validation.result.DefectId;
import ru.yandex.direct.web.core.entity.inventori.model.CampaignStrategy;
import ru.yandex.direct.web.core.entity.inventori.model.CpmForecastRequest;
import ru.yandex.direct.web.core.entity.inventori.model.CpmForecastResult;
import ru.yandex.direct.web.core.entity.inventori.model.ImpressionLimit;
import ru.yandex.direct.web.core.entity.inventori.service.InventoriService;
import ru.yandex.direct.web.core.entity.inventori.validation.InventoriDefectIds;
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.path;
import static ru.yandex.direct.web.core.entity.inventori.validation.InventoriDefectIds.Number.LOW_REACH;

@RunWith(Parameterized.class)
public class CampaignForecastServiceForecastTest {

    private static final String REQUEST_ID = UUID.randomUUID().toString().toUpperCase();

    @Parameterized.Parameter(0)
    public CampaignPredictionResponse response;

    @Parameterized.Parameter(1)
    public DefectId defectId;

    @Parameterized.Parameters(name = "timeToken: {0}, limit: {1}. Is errors expected: {2}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {new CampaignPredictionLowReachResponse(emptyList(), 5_000L), LOW_REACH},
                {internalErrorResponse(), InventoriDefectIds.Gen.INTERNAL_ERROR},
                {invalidBudgetResponse(), InventoriDefectIds.Number.INVALID_BUDGET},
                {invalidCpmResponse(), InventoriDefectIds.Number.INVALID_CPM},
                {invalidDatesResponse(), InventoriDefectIds.String.INVALID_DATES},
                {invalidRequestResponse(), InventoriDefectIds.String.INVALID_REQUEST},
                {invalidRfResponse(), InventoriDefectIds.String.INVALID_RF},
                {unknownSegmentsResponse(), InventoriDefectIds.String.UNKNOWN_SEGMENTS},
                {unsupportedSegmentsResponse(), InventoriDefectIds.String.UNSUPPORTED_SEGMENTS}
        });
    }

    private CampaignForecastService service;

    @Before
    public void setUp() throws JsonProcessingException {
        ClientId clientId = ClientId.fromLong(1L);

        DirectWebAuthenticationSource authProvider = mock(DirectWebAuthenticationSource.class);
        when(authProvider.getAuthentication()).thenReturn(
                new DirectAuthentication(new User().withLogin("operator").withRole(RbacRole.AGENCY),
                        new User().withClientId(clientId).withLogin("client")));

        ClientService clientService = mock(ClientService.class);
        when(clientService.getClient(clientId)).thenReturn(new Client().withWorkCurrency(CurrencyCode.RUB));

        UserService userService = mock(UserService.class);

        CurrencyRateService currencyRateService = mock(CurrencyRateService.class);
        when(currencyRateService.convertMoney(any(Money.class), any(CurrencyCode.class)))
                .thenReturn(Money.valueOfMicros(100_000_000, CurrencyCode.RUB));

        InventoriClient medireachClient = mock(InventoriClient.class);

        service = new CampaignForecastService(authProvider,
                clientService,
                userService,
                medireachClient,
                new InventoriService(currencyRateService, mock(CampaignInfoCollector.class),
                        mock(ShardHelper.class),
                        mock(InventoriServiceCore.class),
                        mock(PricePackageService.class), mock(CryptaSegmentRepository.class),
                        mock(CampaignRepository.class),mock(FeatureService.class)));

        when(medireachClient.getCampaignPrediction(anyString(),
                eq("operator"),
                eq("client"),
                any(CampaignPredictionRequest.class)))
                .thenReturn(response);
    }

    @Test
    public void forecastTest() throws JsonProcessingException {
        CpmForecastResult result = service.forecast(REQUEST_ID, request());

        assertThat(result.getErrors()).is(matchedBy(hasDefectWithDefinition(validationError(path(), defectId))));
    }

    private CpmForecastRequest request() {
        return new CpmForecastRequest().withNewCampaignExampleType(0)
                .withStrategy(new CampaignStrategy()
                        .withBudget(100_000.00)
                        .withStartDate(LocalDate.now())
                        .withEndDate(LocalDate.now().plusMonths(1))
                        .withImpressionLimit(new ImpressionLimit(0L, 0L))
                        .withType("MAX_REACH"));
    }

    private static CampaignPredictionAvailableResponse internalErrorResponse() {
        return new CampaignPredictionAvailableResponse(
                singletonList(new PredictionResponseInternalError(ErrorType.INTERNAL_ERROR,
                        ErrorType.INTERNAL_ERROR.name(), "")),
                0L,
                0L,
                0L,
                0L,
                0L,
                null, null, null, null, null);
    }

    private static CampaignPredictionAvailableResponse invalidBudgetResponse() {
        return new CampaignPredictionAvailableResponse(
                singletonList(new PredictionResponseInvalidBudgetError(ErrorType.INVALID_BUDGET,
                        100L)),
                0L,
                0L,
                0L,
                0L,
                0L,
                null, null, null, null, null);
    }

    private static CampaignPredictionAvailableResponse invalidCpmResponse() {
        return new CampaignPredictionAvailableResponse(
                singletonList(new PredictionResponseInvalidCpmError(ErrorType.INVALID_CPM,
                        1000L)),
                0L,
                0L,
                0L,
                0L,
                0L,
                null, null, null, null, null);
    }

    private static CampaignPredictionAvailableResponse invalidDatesResponse() {
        return new CampaignPredictionAvailableResponse(
                singletonList(new PredictionResponseInvalidDatesError(ErrorType.INVALID_DATES,
                        "invalidDates")),
                0L,
                0L,
                0L,
                0L,
                0L,
                null, null, null, null, null);
    }

    private static CampaignPredictionAvailableResponse invalidRequestResponse() {
        return new CampaignPredictionAvailableResponse(
                singletonList(new PredictionResponseInvalidRequestError(ErrorType.INVALID_REQUEST,
                        "invalidRequest", "")),
                0L,
                0L,
                0L,
                0L,
                0L,
                null, null, null, null, null);
    }

    private static CampaignPredictionAvailableResponse invalidRfResponse() {
        return new CampaignPredictionAvailableResponse(
                singletonList(new PredictionResponseInvalidRfError(ErrorType.INVALID_RF,
                        "invalidRf")),
                0L,
                0L,
                0L,
                0L,
                0L,
                null, null, null, null, null);
    }

    private static CampaignPredictionAvailableResponse unknownSegmentsResponse() {
        return new CampaignPredictionAvailableResponse(
                singletonList(new PredictionResponseUnknownSegmentsError(ErrorType.UNKNOWN_SEGMENTS,
                        singletonList("unknownSegment"))),
                0L,
                0L,
                0L,
                0L,
                0L,
                null, null, null, null, null);
    }

    private static CampaignPredictionAvailableResponse unsupportedSegmentsResponse() {
        return new CampaignPredictionAvailableResponse(
                singletonList(new PredictionResponseUnsupportedSegmentsError(ErrorType.UNSUPPORTED_SEGMENTS,
                        singletonList("unsupportedSegment"))),
                0L,
                0L,
                0L,
                0L,
                0L,
                null, null, null, null, null);
    }
}
