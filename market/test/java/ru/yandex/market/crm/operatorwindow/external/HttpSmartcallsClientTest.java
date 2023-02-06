package ru.yandex.market.crm.operatorwindow.external;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.crm.domain.Phone;
import ru.yandex.market.crm.environment.Environment;
import ru.yandex.market.crm.environment.EnvironmentResolver;
import ru.yandex.market.crm.operatorwindow.domain.tasks.smartcalls.SmartcallsConstants;
import ru.yandex.market.crm.operatorwindow.external.smartcalls.AccessTokenResult;
import ru.yandex.market.crm.operatorwindow.external.smartcalls.HttpSmartcallsClient;
import ru.yandex.market.crm.operatorwindow.external.smartcalls.SmartCallsSerializationHelper;
import ru.yandex.market.crm.operatorwindow.external.smartcalls.SmartcallsConfiguration;
import ru.yandex.market.crm.operatorwindow.external.smartcalls.SmartcallsPasswordSupplier;
import ru.yandex.market.crm.util.CrmStrings;
import ru.yandex.market.jmf.configuration.ConfigurationService;
import ru.yandex.market.jmf.http.Http;
import ru.yandex.market.jmf.http.HttpClient;
import ru.yandex.market.jmf.http.HttpClientFactory;
import ru.yandex.market.jmf.http.HttpResponse;
import ru.yandex.market.jmf.http.test.ResponseBuilder;
import ru.yandex.market.jmf.utils.serialize.CustomJsonSerializer;
import ru.yandex.market.jmf.utils.serialize.ObjectMapperFactory;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class HttpSmartcallsClientTest {

    private static final CustomJsonSerializer jsonSerializer
            = new CustomJsonSerializer(new ObjectMapperFactory(Optional.empty()));
    private final String testCampaignId = "5550";
    private final Phone testPhone = Phone.fromRaw("79920137256");
    private final String testToken = "testToken";
    @Mock
    private HttpClientFactory factory;
    @Mock
    private SmartCallsSerializationHelper serializationHelper;
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

    @BeforeEach
    public void init() {
        Mockito.when(factory.create(any())).thenReturn(httpClient);
        Mockito.when(configuration.getUsername()).thenReturn("testName");
        Mockito.when(passwordSupplier.get()).thenReturn("testPassword");

        HttpResponse executeResult = ResponseBuilder.newBuilder().build();
        Mockito.when(httpClient.execute(any())).thenReturn(executeResult);

        AccessTokenResult accessToken = new AccessTokenResult();
        accessToken.setResult(testToken);
        accessToken.setSuccess(true);
        Mockito.when(serializationHelper.readAccessTokenResult(any())).thenReturn(accessToken);

        Mockito.when(serializationHelper.serializeToString(any()))
                .thenAnswer(x -> jsonSerializer.writeObjectAsString(x.getArguments()[0]));

        Mockito.when(environmentResolver.is(eq(Environment.PRODUCTION))).thenReturn(true);

        smartcallsClient = new HttpSmartcallsClient(factory, serializationHelper, configuration, passwordSupplier,
                environmentResolver, configurationService);
    }

    @Test
    public void testAppendToCampaign() {
        boolean appendResult = smartcallsClient.appendToCampaign(testCampaignId, testPhone, buildParameters());
        Assertions.assertTrue(appendResult);

        // Получим запросы, которые были переданы на выполнение в httpClient
        final ArgumentCaptor<Http> httpCaptor = ArgumentCaptor.forClass(Http.class);
        verify(httpClient, Mockito.times(2)).execute(httpCaptor.capture());
        List<Http> httpClientArgs = httpCaptor.getAllValues();

        String body = new String(httpClientArgs.get(1).getBody(), StandardCharsets.UTF_8);
        assertAppendToCampaignBody(body);

        List<Http.NamedValue> queryParams = httpClientArgs.get(1).getQueryParameters();
        assertAppendToCampaignQueryParams(queryParams);

    }

    private void assertAppendToCampaignQueryParams(List<Http.NamedValue> queryParams) {
        Assertions.assertEquals(2, queryParams.size());
        queryParams.stream()
                .filter(x -> x.getName().equals("access_token"))
                .forEach(x -> Assertions.assertEquals(testToken, x.getValue()));
    }

    private void assertAppendToCampaignBody(String body) {
        String expectedBody = "campaign_id=5550&rows=%5B%7B%22name%22%3A%22%D0%9A%D0%BE%D0%BD%D1%81%D1%82%D0%B0%D0%BD"
                + "%D1%82%D0%B8%D0%BD%22%2C%22date%22%3A%229+%D0%B8%D1%8E%D0%BB%D1%8F%22%2C%22orderId%22%3A%222111221"
                + "%22%2C%22UTC%22%3A%22Asia%2FYekaterinburg%22%2C%22phone%22%3A%22%2B79920137256%22%2C%22order_amount"
                + "%22%3A%22123%22%7D%5D";
        Assertions.assertEquals(expectedBody, body);
    }


    private Map<String, String> buildParameters() {
        return ImmutableMap.of(
                SmartcallsConstants.ORDER_ID, String.valueOf(2111221L),
                SmartcallsConstants.UTC, "Asia/Yekaterinburg",
                SmartcallsConstants.ORDER_AMOUNT, String.valueOf(123L),
                SmartcallsConstants.NAME, CrmStrings.emptyIfNull("Константин"),
                SmartcallsConstants.DATE, "9 июля"
        );
    }

}
