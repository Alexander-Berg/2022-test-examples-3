package ru.yandex.direct.core.entity.abt.service;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.log.container.LogUaasData;
import ru.yandex.direct.common.log.service.LogUaasDataService;
import ru.yandex.direct.core.entity.abt.container.AllowedFeatures;
import ru.yandex.direct.core.entity.abt.container.TestInfo;
import ru.yandex.direct.core.entity.abt.container.UaasInfoRequest;
import ru.yandex.direct.core.entity.abt.container.UaasInfoResponse;
import ru.yandex.direct.core.entity.abt.repository.UaasInfoRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.uaas.UaasClient;
import ru.yandex.direct.uaas.UaasRequest;
import ru.yandex.direct.uaas.UaasResponse;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UaasInfoServiceTest {
    @Autowired
    private Steps steps;

    @Autowired
    private UaasInfoRepository uaasInfoRepository;
    @Autowired
    private UaasConditionEvaluator uaasConditionEvaluator;


    private EnvironmentNameGetter environmentNameGetter;

    private UaasClient uaasClient;
    private UaasInfoService uaasInfoService;
    private LogUaasDataService logUaasDataService;
    private static final String CREATE_DATE_GT_TEMPLATE = "clientCreateDate > \'%s\'";
    private static final String INTERFACE_LANG_RU_CONDITION = "interfaceLang == 'ru'";
    private static final String ENV_CONDITION_TEMPLATE = "env == \'%s\'";
    private static final String CREATE_DATE_LT_TEMPLATE = "clientCreateDate < \'%s\'";
    private static final String HANDLER = "TEST_HANDLER";
    private static final String SERVICE = "test_service";

    @Before
    public void before() {
        logUaasDataService = mock(LogUaasDataService.class);
        uaasClient = mock(UaasClient.class);
        environmentNameGetter = mock(EnvironmentNameGetter.class);
        uaasInfoService = new UaasInfoService(uaasClient, SERVICE, HANDLER, logUaasDataService,
                uaasInfoRepository, uaasConditionEvaluator, environmentNameGetter);
    }

    /**
     * Тест проверяет, что если фича включена в uaas, то она залогируется и вернется в ответе
     */
    @Test
    public void getInfo_FeatureAllowedTest() {
        var client = steps.clientSteps().createDefaultClient();
        ClientId clientId = client.getClientId();
        var uaasRequest = new UaasInfoRequest().withClientId(clientId.asLong());
        String expFlags = String.format("[{\"HANDLER\":\"%s\",\"CONTEXT\":{\"MAIN\":{\"%s\":{\"flags" +
                "\":[\"test_feature" +
                "\"]}}},\"TESTID\":[\"20193\"]}]", HANDLER, HANDLER);
        when(uaasClient.split(eq(
                new UaasRequest(SERVICE)
                        .withCuid(String.valueOf(clientId)))))
                .thenReturn(UaasResponse.builder()
                        .withBoxes("20193,0,12")
                        .withFlagsEncoded(Base64.getEncoder().encodeToString(expFlags.getBytes()))
                        .withBoxedCrypted("boxesCrypted")
                        .withSplitParamsEncoded(Base64.getEncoder().encodeToString("splitParamsEncode".getBytes()))
                        .withConfigVersion("1")
                        .build());
        var responses = uaasInfoService.getInfo(List.of(uaasRequest));
        verify(logUaasDataService).logUaasData(eq(new LogUaasData()
                .withClientId(clientId.asLong())
                .withConfigVersion("1")
                .withExpBoxes("20193,0,12")
                .withFeatures("test_feature")));
        assertThat(responses).hasSize(1);
        var response = responses.get(0);
        assertThat(response.getBoxes()).isEqualTo("20193,0,12");
        assertThat(response.getBoxesCrypted()).isEqualTo("boxesCrypted");
        assertThat(response.getClientId()).isEqualTo(clientId);
        assertThat(response.getFeatures()).isEqualTo(List.of("test_feature"));
        assertThat(response.getConfigVersion()).isEqualTo("1");
    }

    /**
     * Тест проверяет, что если фича включена в uaas, но ее нет в списке доступных фичей, то оне не залогируется и не
     * вернется в ответе
     */
    @Test
    public void getInfo_FeatureNotAllowedTest() {
        var client = steps.clientSteps().createDefaultClient();
        ClientId clientId = client.getClientId();
        var uaasRequest = new UaasInfoRequest().withClientId(clientId.asLong());
        String expFlagsNotAllowed = String.format("[{\"HANDLER\":\"%s\",\"CONTEXT\":{\"MAIN\":{\"%s\":{\"flags" +
                "\":[\"test_feature_not_allowed" +
                "\"]}}},\"TESTID\":[\"20193\"]}]", HANDLER, HANDLER);
        String expFlagsAllowed = String.format("[{\"HANDLER\":\"%s\",\"CONTEXT\":{\"MAIN\":{\"%s\":{\"flags" +
                "\":[\"test_feature_allowed\"]}}},\"TESTID\":[\"20194\"]}]", HANDLER, HANDLER);
        var expFlagsEncoded =
                Base64.getEncoder().encodeToString(expFlagsNotAllowed.getBytes()) + "," + Base64.getEncoder().encodeToString(expFlagsAllowed.getBytes());


        when(uaasClient.split(eq(
                new UaasRequest(SERVICE)
                        .withCuid(String.valueOf(clientId)))))
                .thenReturn(UaasResponse.builder()
                        .withBoxes("20193,0,12;20194,0,45")
                        .withFlagsEncoded(expFlagsEncoded)
                        .withBoxedCrypted("boxesCrypted")
                        .withSplitParamsEncoded(Base64.getEncoder().encodeToString("splitParamsEncode".getBytes()))
                        .withConfigVersion("1")
                        .build());
        var responses = uaasInfoService.getInfo(List.of(uaasRequest), AllowedFeatures.of("test_feature_allowed"));
        verify(logUaasDataService).logUaasData(eq(new LogUaasData()
                .withClientId(clientId.asLong())
                .withConfigVersion("1")
                .withExpBoxes("20194,0,45")
                .withFeatures("test_feature_allowed")));
        assertThat(responses).hasSize(1);
        var response = responses.get(0);
        assertThat(response.getBoxes()).isEqualTo("20194,0,45");
        assertThat(response.getBoxesCrypted()).isEqualTo("boxesCrypted");
        assertThat(response.getClientId()).isEqualTo(clientId);
        assertThat(response.getFeatures()).isEqualTo(List.of("test_feature_allowed"));
        assertThat(response.getConfigVersion()).isEqualTo("1");
    }

    /**
     * Тест проверяет, что если condition выполнен для клиента, то все фичи и boxes вернутся
     */
    @Test
    public void getInfo_TrueCondition() {
        var client = steps.clientSteps().createDefaultClient();
        var dateBeforeClientCreate = client.getClient().getCreateDate().minus(1, DAYS);
        var condition = String.format(CREATE_DATE_GT_TEMPLATE, dateBeforeClientCreate);
        ClientId clientId = client.getClientId();
        var uaasRequest = new UaasInfoRequest().withClientId(clientId.asLong());
        String expFlags = String.format("[{\"HANDLER\":\"%s\",\"CONTEXT\":{\"MAIN\":{\"%s\":{\"flags" +
                "\":[\"test_feature" +
                "\"]}}},\"TESTID\":[\"20193\"],\"CONDITION\":\"%s\"}]", HANDLER, HANDLER, condition);
        when(uaasClient.split(eq(
                new UaasRequest(SERVICE)
                        .withCuid(String.valueOf(clientId)))))
                .thenReturn(UaasResponse.builder()
                        .withBoxes("20193,0,12")
                        .withFlagsEncoded(Base64.getEncoder().encodeToString(expFlags.getBytes()))
                        .withBoxedCrypted("boxesCrypted")
                        .withSplitParamsEncoded(Base64.getEncoder().encodeToString("splitParamsEncode".getBytes()))
                        .withConfigVersion("1")
                        .build());
        var responses = uaasInfoService.getInfo(List.of(uaasRequest));
        verify(logUaasDataService).logUaasData(eq(new LogUaasData()
                .withClientId(clientId.asLong())
                .withConfigVersion("1")
                .withExpBoxes("20193,0,12")
                .withFeatures("test_feature")));
        assertThat(responses.size()).isEqualTo(1);
        var response = responses.get(0);
        assertThat(response.getBoxes()).isEqualTo("20193,0,12");
        assertThat(response.getBoxesCrypted()).isEqualTo("boxesCrypted");
        assertThat(response.getClientId()).isEqualTo(clientId);
        assertThat(response.getFeatures()).isEqualTo(List.of("test_feature"));
        assertThat(response.getConfigVersion()).isEqualTo("1");
    }

    /**
     * Тест проверяет, что если condition не выполнен для клиента, то фичи для неподходящего эксперимента не
     * вернутся, а boxes не будут содержать не подощедший эксперимент
     */
    @Test
    public void getInfo_FalseCondition() {
        var client = steps.clientSteps().createDefaultClient();
        var dateAfterClientCreate = client.getClient().getCreateDate().plus(1, DAYS);
        var condition = String.format(CREATE_DATE_GT_TEMPLATE, dateAfterClientCreate);
        ClientId clientId = client.getClientId();
        var uaasRequest = new UaasInfoRequest().withClientId(clientId.asLong());
        String expFlagsWithCondition = String.format("[{\"HANDLER\":\"%s\",\"CONTEXT\":{\"MAIN\":{\"%s\":{\"flags" +
                "\":[\"test_feature1\"]}}},\"TESTID\":[\"20193\"],\"CONDITION\":\"%s\"}]", HANDLER, HANDLER, condition);
        String expFlagsWithoutCondition = String.format("[{\"HANDLER\":\"%s\",\"CONTEXT\":{\"MAIN\":{\"%s\":{\"flags" +
                "\":[\"test_feature2\"]}}},\"TESTID\":[\"20194\"]}]", HANDLER, HANDLER);

        var expFlagsEncoded =
                Base64.getEncoder().encodeToString(expFlagsWithCondition.getBytes()) + "," + Base64.getEncoder().encodeToString(expFlagsWithoutCondition.getBytes());
        when(uaasClient.split(eq(
                new UaasRequest(SERVICE)
                        .withCuid(String.valueOf(clientId)))))
                .thenReturn(UaasResponse.builder()
                        .withBoxes("20193,0,12;20194,0,45")
                        .withFlagsEncoded(expFlagsEncoded)
                        .withBoxedCrypted("boxesCrypted")
                        .withSplitParamsEncoded(Base64.getEncoder().encodeToString("splitParamsEncode".getBytes()))
                        .withConfigVersion("1")
                        .build());
        var responses = uaasInfoService.getInfo(List.of(uaasRequest));
        verify(logUaasDataService).logUaasData(eq(new LogUaasData()
                .withClientId(clientId.asLong())
                .withConfigVersion("1")
                .withExpBoxes("20194,0,45")
                .withFeatures("test_feature2")));
        assertThat(responses.size()).isEqualTo(1);
        var response = responses.get(0);
        assertThat(response.getBoxes()).isEqualTo("20194,0,45");
        assertThat(response.getBoxesCrypted()).isEqualTo("boxesCrypted");
        assertThat(response.getClientId()).isEqualTo(clientId);
        assertThat(response.getFeatures()).isEqualTo(List.of("test_feature2"));
        assertThat(response.getConfigVersion()).isEqualTo("1");
    }

    /**
     * Тест проверяет, что если клиента не существует - condition будет считаться невыполненым
     */
    @Test
    public void getInfo_NotExistingClient() {
        ClientId notExistingClientId = steps.clientSteps().generateNewClientId();
        var dateForCondition = LocalDateTime.now();

        var condition = String.format(CREATE_DATE_GT_TEMPLATE, dateForCondition);
        var uaasRequest = new UaasInfoRequest().withClientId(notExistingClientId.asLong());
        String expFlags = String.format("[{\"HANDLER\":\"%s\",\"CONTEXT\":{\"MAIN\":{\"%s\":{\"flags" +
                "\":[\"test_feature" +
                "\"]}}},\"TESTID\":[\"20193\"],\"CONDITION\":\"%s\"}]", HANDLER, HANDLER, condition);
        when(uaasClient.split(eq(
                new UaasRequest(SERVICE)
                        .withCuid(String.valueOf(notExistingClientId)))))
                .thenReturn(UaasResponse.builder()
                        .withBoxes("20193,0,12")
                        .withFlagsEncoded(Base64.getEncoder().encodeToString(expFlags.getBytes()))
                        .withBoxedCrypted("boxesCrypted")
                        .withSplitParamsEncoded(Base64.getEncoder().encodeToString("splitParamsEncode".getBytes()))
                        .withConfigVersion("1")
                        .build());
        var responses = uaasInfoService.getInfo(List.of(uaasRequest));
        assertThat(responses).hasSize(1);
        var response = responses.get(0);
        assertThat(response.getBoxes()).isEmpty();
        assertThat(response.getBoxesCrypted()).isEqualTo("boxesCrypted");
        assertThat(response.getClientId()).isEqualTo(notExistingClientId);
        assertThat(response.getFeatures()).isEmpty();
        assertThat(response.getConfigVersion()).isEqualTo("1");
    }

    /**
     * Тест проверяет, что для двух клиентов, подходящих для разных condition, каждому вернется только подходящие ему
     * фичи и exp_boxes
     */
    @Test
    public void getInfo_UsersWithOppositeConditions() {
        LocalDateTime dateForCondition = LocalDateTime.of(2020, 1, 2, 0, 0);
        var clientAfterConditionDate =
                steps.clientSteps().createClient(defaultClient().withCreateDate(dateForCondition.plus(1, DAYS)));
        var clientBeforeConditionDate =
                steps.clientSteps().createClient(defaultClient().withCreateDate(dateForCondition.minus(1, DAYS)));


        var conditionAfter = String.format(CREATE_DATE_GT_TEMPLATE, dateForCondition);
        var conditionBefore = String.format(CREATE_DATE_LT_TEMPLATE, dateForCondition);

        String expFlagsWithAfterCondition = String.format("[{\"HANDLER\":\"%s\"," +
                        "\"CONTEXT\":{\"MAIN\":{\"%s\":{\"flags" +
                        "\":[\"test_feature1\"]}}},\"TESTID\":[\"20193\"],\"CONDITION\":\"%s\"}]", HANDLER, HANDLER,
                conditionAfter);
        String expFlagsWithoutBeforeCondition = String.format("[{\"HANDLER\":\"%s\"," +
                        "\"CONTEXT\":{\"MAIN\":{\"%s\":{\"flags\":[\"test_feature2\"]}}}," +
                        "\"TESTID\":[\"20194\"],\"CONDITION\":\"%s\"}]", HANDLER, HANDLER,
                conditionBefore);
        var expFlagsEncoded =
                Base64.getEncoder().encodeToString(expFlagsWithAfterCondition.getBytes()) + "," + Base64.getEncoder().encodeToString(expFlagsWithoutBeforeCondition.getBytes());

        when(uaasClient.split(any()))
                .thenReturn(UaasResponse.builder()
                        .withBoxes("20193,0,12;20194,0,36")
                        .withFlagsEncoded(expFlagsEncoded)
                        .withBoxedCrypted("boxesCrypted")
                        .withSplitParamsEncoded(Base64.getEncoder().encodeToString("splitParamsEncode".getBytes()))
                        .withConfigVersion("1")
                        .build());
        var uaasInfoRequests = List.of(
                new UaasInfoRequest().withClientId(clientAfterConditionDate.getClientId().asLong()),
                new UaasInfoRequest().withClientId(clientBeforeConditionDate.getClientId().asLong())
        );

        var expectedTestInfoAfterConditionDate =
                new TestInfo().withCondition(conditionAfter).withTestIds(List.of("20193")).withFeatures(List.of(
                        "test_feature1"));
        var responses = uaasInfoService.getInfo(uaasInfoRequests);
        var expectedResponseAfterConditionDate = new UaasInfoResponse().withBoxes("20193,0,12").withBoxesCrypted(
                "boxesCrypted").withClientId(clientAfterConditionDate.getClientId()).withConfigVersion("1").withTests(List.of(expectedTestInfoAfterConditionDate));

        var expectedTestInfoBeforeConditionDate =
                new TestInfo().withCondition(conditionBefore).withTestIds(List.of("20194")).withFeatures(List.of(
                        "test_feature2"));
        var expectedResponseBeforeConditionDate = new UaasInfoResponse().withBoxes("20194,0,36").withBoxesCrypted(
                "boxesCrypted").withClientId(clientBeforeConditionDate.getClientId()).withConfigVersion("1").withTests(List.of(expectedTestInfoBeforeConditionDate));

        assertThat(responses).hasSize(2);
        assertThat(responses).containsExactlyInAnyOrder(expectedResponseAfterConditionDate,
                expectedResponseBeforeConditionDate);
    }

    /**
     * Тест проверяет, что поле interfaceLang будет передано для вычиления condition
     */
    @Test
    public void getInfo_InterfaceLangForConditionsTest() {
        var client =
                steps.clientSteps().createClient(defaultClient());

        String expFlagsWithLangCondition = String.format("[{\"HANDLER\":\"%s\"," +
                        "\"CONTEXT\":{\"MAIN\":{\"%s\":{\"flags" +
                        "\":[\"test_feature1\"]}}},\"TESTID\":[\"20193\"],\"CONDITION\":\"%s\"}]", HANDLER, HANDLER,
                INTERFACE_LANG_RU_CONDITION);

        var expFlagsEncoded =
                Base64.getEncoder().encodeToString(expFlagsWithLangCondition.getBytes());

        when(uaasClient.split(any()))
                .thenReturn(UaasResponse.builder()
                        .withBoxes("20193,0,12")
                        .withFlagsEncoded(expFlagsEncoded)
                        .withBoxedCrypted("boxesCrypted")
                        .withSplitParamsEncoded(Base64.getEncoder().encodeToString("splitParamsEncode".getBytes()))
                        .withConfigVersion("1")
                        .build());
        var uaasInfoRequestsWithRuLang = List.of(
                new UaasInfoRequest().withClientId(client.getClientId().asLong()).withInterfaceLang("ru"));
        var uaasInfoRequestsWithEnLang = List.of(
                new UaasInfoRequest().withClientId(client.getClientId().asLong()).withInterfaceLang("en"));

        var expectedTestInfoWithRuLang =
                new TestInfo().withCondition(INTERFACE_LANG_RU_CONDITION).withTestIds(List.of("20193")).withFeatures(List.of(
                        "test_feature1"));
        var responsesWithRuLang = uaasInfoService.getInfo(uaasInfoRequestsWithRuLang);
        var responsesWithEnLang = uaasInfoService.getInfo(uaasInfoRequestsWithEnLang);
        var expectedResponseWithRuLang = new UaasInfoResponse().withBoxes("20193,0,12").withBoxesCrypted(
                "boxesCrypted").withClientId(client.getClientId()).withConfigVersion("1").withTests(List.of(expectedTestInfoWithRuLang));

        var expectedResponseWithEnLang = new UaasInfoResponse().withBoxes("").withBoxesCrypted(
                "boxesCrypted").withClientId(client.getClientId()).withConfigVersion("1").withTests(List.of());


        assertThat(responsesWithRuLang).hasSize(1);
        assertThat(responsesWithRuLang.get(0)).isEqualTo(expectedResponseWithRuLang);
        assertThat(responsesWithEnLang).hasSize(1);
        assertThat(responsesWithEnLang.get(0)).isEqualTo(expectedResponseWithEnLang);
    }

    /**
     * Тест проверяет, что выборки, содержищие в condition поле evn, будут правильно посчитаны
     */
    @Test
    public void getInfo_EnvForConditionConditions() {
        var client =
                steps.clientSteps().createClient(defaultClient());

        when(environmentNameGetter.get()).thenReturn("testing");
        var productionCondition = String.format(ENV_CONDITION_TEMPLATE, "production");
        var tsCondition = String.format(ENV_CONDITION_TEMPLATE, "testing");
        String expFlagsWithProdCondition = String.format("[{\"HANDLER\":\"%s\"," +
                        "\"CONTEXT\":{\"MAIN\":{\"%s\":{\"flags" +
                        "\":[\"test_feature_prod\"]}}},\"TESTID\":[\"20193\"],\"CONDITION\":\"%s\"}]", HANDLER, HANDLER,
                productionCondition);

        String expFlagsWithTsCondition = String.format("[{\"HANDLER\":\"%s\"," +
                        "\"CONTEXT\":{\"MAIN\":{\"%s\":{\"flags" +
                        "\":[\"test_feature_ts\"]}}},\"TESTID\":[\"20194\"],\"CONDITION\":\"%s\"}]", HANDLER, HANDLER,
                tsCondition);

        var expFlagsEncoded =
                Base64.getEncoder().encodeToString(expFlagsWithProdCondition.getBytes())
                        + "," + Base64.getEncoder().encodeToString(expFlagsWithTsCondition.getBytes());

        when(uaasClient.split(any()))
                .thenReturn(UaasResponse.builder()
                        .withBoxes("20193,0,12;20194,0,78")
                        .withFlagsEncoded(expFlagsEncoded)
                        .withBoxedCrypted("boxesCrypted")
                        .withSplitParamsEncoded(Base64.getEncoder().encodeToString("splitParamsEncode".getBytes()))
                        .withConfigVersion("1")
                        .build());
        var uaasInfoRequests = List.of(
                new UaasInfoRequest().withClientId(client.getClientId().asLong()));

        var expectedTestInfo =
                new TestInfo().withCondition(tsCondition).withTestIds(List.of("20194")).withFeatures(List.of(
                        "test_feature_ts"));
        var gotResponses = uaasInfoService.getInfo(uaasInfoRequests);
        var expectedResponse = new UaasInfoResponse().withBoxes("20194,0,78").withBoxesCrypted(
                "boxesCrypted").withClientId(client.getClientId()).withConfigVersion("1").withTests(List.of(expectedTestInfo));

        assertThat(gotResponses).hasSize(1);
        assertThat(gotResponses.get(0)).isEqualTo(expectedResponse);
    }
}
