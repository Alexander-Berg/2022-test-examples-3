package ru.yandex.market.crm.operatorwindow.services.fraud;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.crm.environment.EnvironmentResolver;
import ru.yandex.market.crm.operatorwindow.external.smartcalls.ConfirmFraudAttemptResult;
import ru.yandex.market.crm.operatorwindow.external.smartcalls.HttpSmartcallsClient;
import ru.yandex.market.crm.operatorwindow.external.smartcalls.SmartCallsSerializationHelper;
import ru.yandex.market.crm.operatorwindow.external.smartcalls.SmartcallsConfiguration;
import ru.yandex.market.crm.operatorwindow.external.smartcalls.SmartcallsPasswordSupplier;
import ru.yandex.market.crm.operatorwindow.serialization.JsonTreeParser;
import ru.yandex.market.crm.operatorwindow.serialization.RawJsonParser;
import ru.yandex.market.crm.serialization.JsonDeserializer;
import ru.yandex.market.crm.serialization.JsonSerializer;
import ru.yandex.market.crm.util.ResourceHelpers;
import ru.yandex.market.jmf.configuration.ConfigurationService;
import ru.yandex.market.jmf.http.HttpClient;
import ru.yandex.market.jmf.http.HttpClientFactory;
import ru.yandex.market.jmf.http.test.ResponseBuilder;
import ru.yandex.market.jmf.http.test.matcher.PathMatcher;
import ru.yandex.market.jmf.utils.serialize.CustomJsonDeserializer;
import ru.yandex.market.jmf.utils.serialize.CustomJsonSerializer;
import ru.yandex.market.jmf.utils.serialize.ObjectMapperFactory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;

@ExtendWith(MockitoExtension.class)
public class SmartcallsResultsIntegrationTest {

    private static final ObjectMapperFactory customObjectMapperFactory
            = new ObjectMapperFactory(Optional.empty());

    private static final JsonSerializer jsonSerializer
            = new CustomJsonSerializer(customObjectMapperFactory);

    private static final JsonDeserializer jsonDeserializer
            = new CustomJsonDeserializer(customObjectMapperFactory);

    private static final JsonTreeParser jsonTreeParser
            = new RawJsonParser(customObjectMapperFactory);
    private static final String CAMPAIGN_ID = "123";
    private static final String TEST_TOKEN = "{\"success\":true,\"result\":\"916911021ff60e0ca16d838f14f92e8e\"}";
    private final SmartCallsSerializationHelper serializationHelper
            = new SmartCallsSerializationHelper(
            jsonSerializer,
            jsonDeserializer,
            jsonTreeParser);
    @Mock
    private HttpClientFactory factory;
    @Mock
    private SmartcallsConfiguration configuration;
    @Mock
    private SmartcallsPasswordSupplier passwordSupplier;
    @Mock
    private HttpClient httpClient;
    @Mock
    private EnvironmentResolver environmentResolver;
    @Mock
    private ConfigurationService configurationService;
    private HttpSmartcallsClient smartcallsClient;
    private SmartcallsResults smartcallsResults;

    @BeforeEach
    public void setup() {
        setupHttpClient();
        setupCredentials();
        setupCampaign(CAMPAIGN_ID);

        // инициализация через поле невозможна, т.к. нужнается в настройках выше
        smartcallsClient = new HttpSmartcallsClient(
                factory,
                serializationHelper,
                configuration,
                passwordSupplier,
                environmentResolver,
                configurationService);
        smartcallsResults
                = new SmartcallsResults(
                configuration,
                smartcallsClient,
                jsonDeserializer);
    }

    @Test
    public void callRecordsIsNull__expectParseSuchRecordsAndConsiderNullCallRecordsAsEmptyList() {
        Mockito.when(httpClient.execute(argThat(new PathMatcher("auth/getAccessToken"))))
                .thenReturn(ResponseBuilder.newBuilder().body(TEST_TOKEN).build());

        byte[] rawResponse = ResourceHelpers.getResource("smartcalls_null_call_records.json");
        Mockito.when(httpClient.execute(argThat(new PathMatcher("attempt/searchAttempts"))))
                .thenReturn(ResponseBuilder.newBuilder().body(rawResponse).build());

        OffsetDateTime now =
                OffsetDateTime.of(
                        2020, 3, 13,
                        13, 22, 0, 0,
                        ZoneOffset.UTC);
        final Collection<ConfirmFraudAttemptResult> confirmFraudAttemptResults = smartcallsResults.get(now);
        Assertions.assertFalse(confirmFraudAttemptResults.isEmpty());
        confirmFraudAttemptResults
                .forEach(attempt -> Assertions.assertNotNull(attempt.getCallRecords()));
    }

    private void setupCredentials() {
        Mockito.when(configuration.getUsername()).thenReturn("testName");
        Mockito.when(passwordSupplier.get()).thenReturn("testPassword");
    }

    private void setupHttpClient() {
        Mockito.when(factory.create(any())).thenReturn(httpClient);
    }

    private void setupCampaign(String campaignId) {
        Mockito.when(configuration.getFraudCampaign()).thenReturn(campaignId);
    }
}
