package ru.yandex.direct.grid.processing.service.trackingphone;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import graphql.ExecutionResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.calltracking.model.CalltrackingSettings;
import ru.yandex.direct.core.entity.calltrackingsettings.repository.CalltrackingSettingsRepository;
import ru.yandex.direct.core.entity.calltrackingsettings.repository.mapper.CalltrackingSettingsMapper;
import ru.yandex.direct.core.entity.domain.model.Domain;
import ru.yandex.direct.core.entity.metrika.container.CounterIdWithDomain;
import ru.yandex.direct.core.entity.metrika.repository.CalltrackingExternalRepository;
import ru.yandex.direct.core.entity.metrika.repository.CalltrackingNumberClicksRepository;
import ru.yandex.direct.core.entity.metrika.repository.CalltrackingPhonesWithoutReplacementsRepository;
import ru.yandex.direct.core.entity.metrikacounter.model.MetrikaCounterPermission;
import ru.yandex.direct.core.entity.trackingphone.model.ClientPhone;
import ru.yandex.direct.core.entity.trackingphone.model.ClientPhoneType;
import ru.yandex.direct.core.entity.trackingphone.model.PhoneNumber;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.DomainInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.trackingphone.GdCalltrackingOnSite;
import ru.yandex.direct.grid.processing.model.trackingphone.GdCalltrackingOnSiteCounterStatus;
import ru.yandex.direct.grid.processing.model.trackingphone.GdCalltrackingOnSiteInputItem;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.metrika.client.MetrikaClientException;
import ru.yandex.direct.metrika.client.model.response.UserCountersExtendedResponse;
import ru.yandex.direct.rbac.RbacRepType;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CalltrackingOnSiteGraphQLServiceGetCalltrackingOnSiteTest {
    private static final Long COUNTER_ID = 123456L;
    private static final String PHONE_1 = "+79101112231";
    private static final String PHONE_2 = "+79101112232";
    private static final String PHONE_3 = "+79101112233";
    private static final String PHONE_4 = "+79101112234";
    private static final String PHONE_5 = "+79101112235";
    private static final String DOMAIN_POSTFIX = ".com";
    private static final String PUNYCODE_DOMAIN = "xn--d1aqf.xn--p1ai";
    private static final String CYRILLIC_DOMAIN = "дом.рф";

    private static final String QUERY_TEMPLATE = "{\n" +
            "  client(searchBy: {login: \"%s\"}) {\n" +
            "    calltrackingOnSite(input: %s) {\n" +
            "      isMetrikaAvailable\n" +
            "      items {\n" +
            "        calltrackingSettingsId\n" +
            "        domain\n" +
            "        counterId\n" +
            "        counterStatus\n" +
            "        hasExternalCalltracking\n" +
            "        calltrackingPhones {\n" +
            "          redirectPhone\n" +
            "          formattedRedirectPhone {\n" +
            "            countryCode\n" +
            "            cityCode\n" +
            "            phoneNumber\n" +
            "          }\n" +
            "          hasClicks\n" +
            "          isCalltrackingOnSiteEnabled\n" +
            "          isCalltrackingOnSiteActive\n" +
            "          isNew\n" +
            "          hasNotReplacements\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }" +
            "}";

    @Autowired
    private GridContextProvider gridContextProvider;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private Steps steps;
    @Autowired
    private UserService userService;
    @Autowired
    private CalltrackingNumberClicksRepository calltrackingNumberClicksRepository;
    @Autowired
    private CalltrackingPhonesWithoutReplacementsRepository calltrackingPhonesWithoutReplacementsRepository;
    @Autowired
    private CalltrackingExternalRepository calltrackingExternalRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private MetrikaClientStub metrikaClientStub;

    @Autowired
    private CalltrackingSettingsRepository calltrackingSettingsRepository;

    private GridGraphQLContext chiefContext;
    private GridGraphQLContext operatorContext;

    private ClientInfo clientInfo;
    private DomainInfo domainInfo1;
    private DomainInfo domainInfo2;
    private DomainInfo domainInfo3;

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();
        domainInfo1 = steps.domainSteps().createDomain(clientInfo.getShard(), new Domain().withDomain(PUNYCODE_DOMAIN));
        domainInfo2 = steps.domainSteps().createDomain(clientInfo.getShard(), DOMAIN_POSTFIX, true);
        domainInfo3 = steps.domainSteps().createDomain(clientInfo.getShard(), new Domain().withDomain(CYRILLIC_DOMAIN));

        when(calltrackingExternalRepository.getExternalCalltrackingDomains(anyCollection(), any()))
                .thenReturn(Set.of(domainInfo1.getDomain().getDomain()));

        // set up chef
        long chiefUserUid = clientInfo.getChiefUserInfo().getUser().getUid();
        User chiefUser = userService.getUser(chiefUserUid);
        chiefContext = ContextHelper.buildContext(chiefUser);

        metrikaClientStub.addUserCounters(chiefUserUid,
                List.of(MetrikaClientStub.buildCounter(COUNTER_ID.intValue())));

        // set up operator
        long operatorUserUid = steps.userSteps().createRepresentative(clientInfo, RbacRepType.MAIN).getUser().getUid();
        User operatorUser = userService.getUser(operatorUserUid);
        operatorContext = ContextHelper.buildContext(operatorUser);

        metrikaClientStub.addUserCounters(operatorUserUid, List.of(
                MetrikaClientStub.buildCounter(COUNTER_ID.intValue(), null, MetrikaCounterPermission.VIEW)
        ));

        gridContextProvider.setGridContext(chiefContext);

        when(calltrackingPhonesWithoutReplacementsRepository.getPhonesWithoutReplacements(any(), any(), any()))
                .thenReturn(Set.of());
    }

    @After
    public void tearDown() {
        reset(metrikaClientStub);
        steps.calltrackingSettingsSteps().deleteAll(clientInfo.getShard());
        steps.calltrackingPhoneSteps().deleteAll();
        steps.domainSteps().delete(
                clientInfo.getShard(),
                List.of(PUNYCODE_DOMAIN, CYRILLIC_DOMAIN, domainInfo2.getDomain().getDomain())
        );
        reset(calltrackingNumberClicksRepository);
    }

    @Test
    public void getCalltrackingOnSiteChiefHappyPath() {
        makeGetCalltrackingOnSiteHappyPath(chiefContext, true, GdCalltrackingOnSiteCounterStatus.OK);
    }

    @Test
    public void getCalltrackingOnSiteOperatorHappyPath() {
        makeGetCalltrackingOnSiteHappyPath(
                operatorContext,
                true,
                GdCalltrackingOnSiteCounterStatus.NO_WRITE_PERMISSIONS
        );
    }

    @Test
    public void getCalltrackingOnSiteByUrlAndCounterIdSettingsNotExist() {
        var clientId = clientInfo.getClientId();

        ClientPhone clientPhone = new ClientPhone()
                .withClientId(clientId)
                .withPhoneNumber(new PhoneNumber().withPhone(PHONE_1))
                .withPhoneType(ClientPhoneType.MANUAL)
                .withCounterId(COUNTER_ID)
                .withIsDeleted(false);
        steps.clientPhoneSteps().addClientManualPhone(clientId, clientPhone);

        var counterIdWithDomain = new CounterIdWithDomain(COUNTER_ID, domainInfo1.getDomain().getDomain());

        when(calltrackingNumberClicksRepository.getClicksOnPhonesByDomainWithCounterIds(
                Mockito.eq(List.of(counterIdWithDomain)))
        ).thenReturn(Map.of(counterIdWithDomain, Map.of(PHONE_1, 2, PHONE_2, 1)));

        GdCalltrackingOnSiteInputItem item = new GdCalltrackingOnSiteInputItem()
                .withCounterId(COUNTER_ID)
                .withUrl("https://" + domainInfo1.getDomain().getDomain() + "/123");
        GdCalltrackingOnSite input = new GdCalltrackingOnSite()
                .withItems(List.of(item));

        gridContextProvider.setGridContext(chiefContext);
        var query = String.format(QUERY_TEMPLATE, chiefContext.getOperator().getLogin(), graphQlSerialize(input));
        ExecutionResult result = processor.processQuery(null, query, null, chiefContext);
        assumeThat(result.getErrors(), hasSize(0));

        Map<String, Object> data = result.getData();

        Map<String, Object> expected = Collections.singletonMap(
                "client",
                ImmutableMap.of(
                        "calltrackingOnSite", ImmutableMap.of(
                                "isMetrikaAvailable", true,
                                "items", ImmutableList.of(
                                        ImmutableMap.of(
                                                "domain", domainInfo1.getDomain().getDomain(),
                                                "counterId", COUNTER_ID,
                                                "counterStatus", GdCalltrackingOnSiteCounterStatus.OK.name(),
                                                "hasExternalCalltracking", true,
                                                "calltrackingPhones", ImmutableList.of(
                                                        ImmutableMap.builder()
                                                                .put("redirectPhone", "+79101112231")
                                                                .put("formattedRedirectPhone", ImmutableMap.builder()
                                                                        .put("countryCode", "+7")
                                                                        .put("cityCode", "910")
                                                                        .put("phoneNumber", "111-22-31")
                                                                        .build()
                                                                )
                                                                .put("hasClicks", true)
                                                                .put("isCalltrackingOnSiteEnabled", false)
                                                                .put("isCalltrackingOnSiteActive", false)
                                                                .put("isNew", false)
                                                                .put("hasNotReplacements", false)
                                                                .build(),
                                                        ImmutableMap.builder()
                                                                .put("redirectPhone", "+79101112232")
                                                                .put("formattedRedirectPhone", ImmutableMap.builder()
                                                                        .put("countryCode", "+7")
                                                                        .put("cityCode", "910")
                                                                        .put("phoneNumber", "111-22-32")
                                                                        .build()
                                                                )
                                                                .put("hasClicks", true)
                                                                .put("isCalltrackingOnSiteEnabled", false)
                                                                .put("isCalltrackingOnSiteActive", false)
                                                                .put("isNew", false)
                                                                .put("hasNotReplacements", false)
                                                                .build()
                                                )
                                        )
                                )
                        )
                )
        );

        BeanFieldPath settingsPrefix = newPath("client/calltrackingOnSite/items/0");
        BeanFieldPath phonePrefix = newPath("client/calltrackingOnSite/items/0/calltrackingPhones/1");
        CompareStrategy compareStrategy = DefaultCompareStrategies.allFields()
                .forFields(phonePrefix.join("phoneId"), settingsPrefix.join("calltrackingSettingsId"))
                .useMatcher(nullValue());

        assertThat(data).is(matchedBy(beanDiffer(expected).useCompareStrategy(compareStrategy)));
    }

    @Test
    public void getCalltrackingOnSiteByCyrillicUrl() {
        var calltrackingSettingsId = steps.calltrackingSettingsSteps().add(
                clientInfo.getClientId(),
                domainInfo3.getDomainId(),
                COUNTER_ID,
                List.of(PHONE_2)
        );

        var counterIdWithDomain = new CounterIdWithDomain(COUNTER_ID, domainInfo1.getDomain().getDomain());
        when(calltrackingNumberClicksRepository.getClicksOnPhonesByDomainWithCounterIds(
                Mockito.eq(List.of(counterIdWithDomain)))
        ).thenReturn(Map.of(counterIdWithDomain, Map.of(PHONE_1, 2)));

        GdCalltrackingOnSiteInputItem item = new GdCalltrackingOnSiteInputItem()
                .withCounterId(COUNTER_ID)
                .withUrl("https://" + CYRILLIC_DOMAIN + "/123");
        GdCalltrackingOnSite input = new GdCalltrackingOnSite()
                .withItems(List.of(item));

        gridContextProvider.setGridContext(chiefContext);
        var query = String.format(QUERY_TEMPLATE, chiefContext.getOperator().getLogin(), graphQlSerialize(input));
        ExecutionResult result = processor.processQuery(null, query, null, chiefContext);
        assumeThat(result.getErrors(), hasSize(0));

        Map<String, Object> data = result.getData();

        Map<String, Object> expected = Collections.singletonMap(
                "client",
                ImmutableMap.of(
                        "calltrackingOnSite", ImmutableMap.of(
                                "isMetrikaAvailable", true,
                                "items", ImmutableList.of(
                                        Map.of(
                                                "calltrackingSettingsId", calltrackingSettingsId,
                                                "domain", CYRILLIC_DOMAIN,
                                                "counterId", COUNTER_ID,
                                                "counterStatus", GdCalltrackingOnSiteCounterStatus.OK.name(),
                                                "hasExternalCalltracking", false,
                                                "calltrackingPhones", ImmutableList.of(
                                                        ImmutableMap.builder()
                                                                .put("redirectPhone", "+79101112231")
                                                                .put("formattedRedirectPhone", ImmutableMap.builder()
                                                                        .put("countryCode", "+7")
                                                                        .put("cityCode", "910")
                                                                        .put("phoneNumber", "111-22-31")
                                                                        .build()
                                                                )
                                                                .put("hasClicks", true)
                                                                .put("isCalltrackingOnSiteEnabled", false)
                                                                .put("isCalltrackingOnSiteActive", false)
                                                                .put("hasNotReplacements", false)
                                                                .put("isNew", false)
                                                                .build(),
                                                        ImmutableMap.builder()
                                                                .put("redirectPhone", "+79101112232")
                                                                .put("formattedRedirectPhone", ImmutableMap.builder()
                                                                        .put("countryCode", "+7")
                                                                        .put("cityCode", "910")
                                                                        .put("phoneNumber", "111-22-32")
                                                                        .build()
                                                                )
                                                                .put("hasClicks", false)
                                                                .put("isCalltrackingOnSiteEnabled", true)
                                                                .put("isCalltrackingOnSiteActive", false)
                                                                .put("hasNotReplacements", false)
                                                                .put("isNew", true)
                                                                .build()
                                                )
                                        )
                                )
                        )
                )
        );

        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void getCalltrackingOnSiteMetrikaIsUnavailable() {
        doThrow(new MetrikaClientException())
                .when(metrikaClientStub).getUsersCountersNumExtended2(any(), any());

        Long calltrackingSettingId = makeGetCalltrackingOnSiteHappyPath(
                operatorContext, false, GdCalltrackingOnSiteCounterStatus.NOT_AVAILABLE);

        List<CalltrackingSettings> actualSettings = calltrackingSettingsRepository.getByIds(
                clientInfo.getClientId(), List.of(calltrackingSettingId));

        // проверяем что isAvailable не изменился
        assertEquals(1, actualSettings.size());
        assertTrue(actualSettings.get(0).getIsAvailableCounter());
    }

    @Test
    public void getCalltrackingOnSiteHasNoAccessToCounter() {
        doReturn(new UserCountersExtendedResponse().withUsers(List.of()))
                .when(metrikaClientStub).getUsersCountersNumExtended2(any(), any());

        Long calltrackingSettingId = makeGetCalltrackingOnSiteHappyPath(
                operatorContext, true, GdCalltrackingOnSiteCounterStatus.NOT_AVAILABLE);

        List<CalltrackingSettings> actualSettings = calltrackingSettingsRepository.getByIds(
                clientInfo.getClientId(), List.of(calltrackingSettingId));

        // проверяем что isAvailable изменился на false
        assertEquals(1, actualSettings.size());
        assertFalse(actualSettings.get(0).getIsAvailableCounter());
    }

    public Long makeGetCalltrackingOnSiteHappyPath(
            GridGraphQLContext context,
            boolean isMetrikaAvailable,
            GdCalltrackingOnSiteCounterStatus status
    ) {
        var clientId = clientInfo.getClientId();
        Long firstCalltrackingSettingsId = steps.calltrackingSettingsSteps().add(
                clientId,
                domainInfo1.getDomainId(),
                COUNTER_ID,
                List.of(PHONE_1, PHONE_2)
        );

        ClientPhone clientPhone = new ClientPhone()
                .withClientId(clientId)
                .withPhoneNumber(new PhoneNumber().withPhone(PHONE_1))
                .withPhoneType(ClientPhoneType.MANUAL)
                .withCounterId(COUNTER_ID)
                .withIsDeleted(false);
        steps.clientPhoneSteps().addClientManualPhone(clientId, clientPhone);

        var counterIdWithDomain = new CounterIdWithDomain(COUNTER_ID, domainInfo1.getDomain().getDomain());

        when(calltrackingNumberClicksRepository.getClicksOnPhonesByDomainWithCounterIds(
                Mockito.eq(List.of(counterIdWithDomain)))
        ).thenReturn(Map.of(counterIdWithDomain, Map.of(PHONE_1, 1)));

        GdCalltrackingOnSiteInputItem item = new GdCalltrackingOnSiteInputItem()
                .withCounterId(COUNTER_ID)
                .withUrl("https://" + domainInfo1.getDomain().getDomain() + "/123");
        GdCalltrackingOnSite input = new GdCalltrackingOnSite()
                .withItems(List.of(item));

        gridContextProvider.setGridContext(context);
        var query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(), graphQlSerialize(input));
        ExecutionResult result = processor.processQuery(null, query, null, context);
        assumeThat(result.getErrors(), hasSize(0));

        Map<String, Object> data = result.getData();

        Map<String, Object> expected = Collections.singletonMap(
                "client",
                ImmutableMap.of(
                        "calltrackingOnSite", ImmutableMap.of(
                                "isMetrikaAvailable", isMetrikaAvailable,
                                "items", ImmutableList.of(
                                        Map.of(
                                                "calltrackingSettingsId", firstCalltrackingSettingsId,
                                                "domain", domainInfo1.getDomain().getDomain(),
                                                "counterId", COUNTER_ID,
                                                "counterStatus", status.name(),
                                                "hasExternalCalltracking", true,
                                                "calltrackingPhones", ImmutableList.of(
                                                        ImmutableMap.builder()
                                                                .put("redirectPhone", "+79101112231")
                                                                .put("formattedRedirectPhone", ImmutableMap.builder()
                                                                        .put("countryCode", "+7")
                                                                        .put("cityCode", "910")
                                                                        .put("phoneNumber", "111-22-31")
                                                                        .build()
                                                                )
                                                                .put("hasClicks", true)
                                                                .put("isCalltrackingOnSiteEnabled", true)
                                                                .put("isCalltrackingOnSiteActive", false)
                                                                .put("isNew", true)
                                                                .put("hasNotReplacements", false)
                                                                .build(),
                                                        ImmutableMap.builder()
                                                                .put("redirectPhone", "+79101112232")
                                                                .put("formattedRedirectPhone", ImmutableMap.builder()
                                                                        .put("countryCode", "+7")
                                                                        .put("cityCode", "910")
                                                                        .put("phoneNumber", "111-22-32")
                                                                        .build()
                                                                )
                                                                .put("hasClicks", false)
                                                                .put("isCalltrackingOnSiteEnabled", true)
                                                                .put("isCalltrackingOnSiteActive", false)
                                                                .put("isNew", true)
                                                                .put("hasNotReplacements", false)
                                                                .build()
                                                )
                                        )
                                )
                        )
                )
        );

        BeanFieldPath prefix = newPath("client/calltrackingOnSite/items/0/calltrackingPhones/1");
        CompareStrategy compareStrategy = DefaultCompareStrategies.allFields()
                .forFields(prefix.join("phoneId"))
                .useMatcher(nullValue());

        assertThat(data).is(matchedBy(beanDiffer(expected).useCompareStrategy(compareStrategy)));
        return firstCalltrackingSettingsId;
    }

    @Test
    public void getCalltrackingOnSiteNotFound() {
        GdCalltrackingOnSiteInputItem item = new GdCalltrackingOnSiteInputItem()
                .withCounterId(COUNTER_ID)
                .withUrl("https://" + domainInfo1.getDomain().getDomain() + "/123");
        GdCalltrackingOnSite input = new GdCalltrackingOnSite()
                .withItems(List.of(item));

        var query = String.format(QUERY_TEMPLATE, chiefContext.getOperator().getLogin(), graphQlSerialize(input));
        ExecutionResult result = processor.processQuery(null, query, null, chiefContext);
        assumeThat(result.getErrors(), hasSize(0));

        Map<String, Object> data = result.getData();

        Map<String, Object> expected = Collections.singletonMap(
                "client",
                ImmutableMap.of(
                        "calltrackingOnSite", ImmutableMap.of(
                                "isMetrikaAvailable", true,
                                "items", ImmutableList.of(
                                        ImmutableMap.of(
                                                "domain", domainInfo1.getDomain().getDomain(),
                                                "counterId", COUNTER_ID,
                                                "counterStatus", GdCalltrackingOnSiteCounterStatus.OK.name(),
                                                "hasExternalCalltracking", true,
                                                "calltrackingPhones", emptyList())
                                )
                        )
                )
        );

        BeanFieldPath prefix = newPath("client/calltrackingOnSite/items/0");
        CompareStrategy compareStrategy = DefaultCompareStrategies.allFields()
                .forFields(prefix.join("calltrackingSettingsId"))
                .useMatcher(nullValue());

        assertThat(data).is(matchedBy(beanDiffer(expected).useCompareStrategy(compareStrategy)));
    }

    @Test
    public void getCalltrackingOnSiteBatch() {
        ClientId clientId = clientInfo.getClientId();
        var firstCalltrackingSettingsId = steps.calltrackingSettingsSteps().add(
                clientId,
                domainInfo1.getDomainId(),
                COUNTER_ID,
                List.of(PHONE_1)
        );

        var secondCalltrackingSettingsId = steps.calltrackingSettingsSteps().add(
                clientId,
                domainInfo2.getDomainId(),
                COUNTER_ID,
                List.of(PHONE_2)
        );

        var thirdCalltrackingSettingsId = steps.calltrackingSettingsSteps().add(
                clientId,
                domainInfo3.getDomainId(),
                COUNTER_ID,
                List.of(PHONE_3)
        );

        when(calltrackingNumberClicksRepository.getClicksOnPhonesByDomainWithCounterIds(anySet()))
                .thenReturn(Map.of());

        GdCalltrackingOnSiteInputItem item1 = new GdCalltrackingOnSiteInputItem()
                .withCalltrackingSettingsId(firstCalltrackingSettingsId);

        GdCalltrackingOnSiteInputItem item2 = new GdCalltrackingOnSiteInputItem()
                .withCalltrackingSettingsId(secondCalltrackingSettingsId);

        GdCalltrackingOnSiteInputItem item3 = new GdCalltrackingOnSiteInputItem()
                .withCalltrackingSettingsId(thirdCalltrackingSettingsId);

        GdCalltrackingOnSite input = new GdCalltrackingOnSite()
                .withItems(List.of(item1, item2, item3));

        var query = String.format(QUERY_TEMPLATE, chiefContext.getOperator().getLogin(), graphQlSerialize(input));
        ExecutionResult result = processor.processQuery(null, query, null, chiefContext);
        assumeThat(result.getErrors(), hasSize(0));

        Map<String, Object> data = result.getData();

        Map<String, Object> expected = Collections.singletonMap(
                "client",
                ImmutableMap.of(
                        "calltrackingOnSite", ImmutableMap.of(
                                "isMetrikaAvailable", true,
                                "items", ImmutableList.of(
                                        Map.of(
                                                "calltrackingSettingsId", firstCalltrackingSettingsId,
                                                "domain", domainInfo1.getDomain().getDomain(),
                                                "counterId", COUNTER_ID,
                                                "counterStatus", GdCalltrackingOnSiteCounterStatus.OK.name(),
                                                "hasExternalCalltracking", true,
                                                "calltrackingPhones", ImmutableList.of(
                                                        ImmutableMap.builder()
                                                                .put("redirectPhone", "+79101112231")
                                                                .put("formattedRedirectPhone", ImmutableMap.builder()
                                                                        .put("countryCode", "+7")
                                                                        .put("cityCode", "910")
                                                                        .put("phoneNumber", "111-22-31")
                                                                        .build()
                                                                )
                                                                .put("hasClicks", false)
                                                                .put("isCalltrackingOnSiteEnabled", true)
                                                                .put("isCalltrackingOnSiteActive", false)
                                                                .put("isNew", true)
                                                                .put("hasNotReplacements", false)
                                                                .build()
                                                )
                                        ),
                                        Map.of(
                                                "calltrackingSettingsId", secondCalltrackingSettingsId,
                                                "domain", domainInfo2.getDomain().getDomain(),
                                                "counterId", COUNTER_ID,
                                                "counterStatus", GdCalltrackingOnSiteCounterStatus.OK.name(),
                                                "hasExternalCalltracking", false,
                                                "calltrackingPhones", ImmutableList.of(
                                                        ImmutableMap.builder()
                                                                .put("redirectPhone", "+79101112232")
                                                                .put("formattedRedirectPhone", ImmutableMap.builder()
                                                                        .put("countryCode", "+7")
                                                                        .put("cityCode", "910")
                                                                        .put("phoneNumber", "111-22-32")
                                                                        .build()
                                                                )
                                                                .put("hasClicks", false)
                                                                .put("isCalltrackingOnSiteEnabled", true)
                                                                .put("isCalltrackingOnSiteActive", false)
                                                                .put("isNew", true)
                                                                .put("hasNotReplacements", false)
                                                                .build()
                                                )
                                        ),
                                        Map.of(
                                                "calltrackingSettingsId", thirdCalltrackingSettingsId,
                                                "domain", domainInfo3.getDomain().getDomain(),
                                                "counterId", COUNTER_ID,
                                                "counterStatus", GdCalltrackingOnSiteCounterStatus.OK.name(),
                                                "hasExternalCalltracking", false,
                                                "calltrackingPhones", ImmutableList.of(
                                                        ImmutableMap.builder()
                                                                .put("redirectPhone", "+79101112233")
                                                                .put("formattedRedirectPhone", ImmutableMap.builder()
                                                                        .put("countryCode", "+7")
                                                                        .put("cityCode", "910")
                                                                        .put("phoneNumber", "111-22-33")
                                                                        .build()
                                                                )
                                                                .put("hasClicks", false)
                                                                .put("isCalltrackingOnSiteEnabled", true)
                                                                .put("isCalltrackingOnSiteActive", false)
                                                                .put("isNew", true)
                                                                .put("hasNotReplacements", false)
                                                                .build()
                                                )
                                        )
                                )
                        )
                )
        );

        BeanFieldPath prefix = newPath("client/calltrackingOnSite/items/\\d/calltrackingPhones/\\d/phoneId");
        CompareStrategy compareStrategy = DefaultCompareStrategies.allFields()
                .forFields(prefix)
                .useMatcher(nullValue());

        assertThat(data).is(matchedBy(beanDiffer(expected).useCompareStrategy(compareStrategy)));
    }

    @Test
    public void getCalltrackingOnSiteFilterCalltrackingPhones() {
        var calltrackingSettingsId = steps.calltrackingSettingsSteps().add(
                clientInfo.getClientId(),
                domainInfo1.getDomainId(),
                COUNTER_ID,
                List.of(PHONE_1)
        );

        steps.calltrackingPhoneSteps().add(PHONE_4, LocalDateTime.now());

        var originAndTelephonyPhone = new ClientPhone()
                .withClientId(clientInfo.getClientId())
                .withPhoneType(ClientPhoneType.TELEPHONY)
                .withTelephonyServiceId(String.valueOf(RandomNumberUtils.nextPositiveInteger()))
                .withPhoneNumber(new PhoneNumber().withPhone(PHONE_1))
                .withTelephonyPhone(new PhoneNumber().withPhone(PHONE_5))
                .withIsDeleted(false);
        Long phoneId = steps.clientPhoneSteps().addPhone(clientInfo.getClientId(), originAndTelephonyPhone).getId();

        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);
        steps.campCalltrackingSettingsSteps()
                .link(clientInfo.getShard(), campaignInfo.getCampaignId(), calltrackingSettingsId);
        steps.campCalltrackingPhonesSteps().add(clientInfo.getShard(), phoneId, campaignInfo.getCampaignId());

        var counterIdWithDomain = new CounterIdWithDomain(COUNTER_ID, domainInfo1.getDomain().getDomain());

        when(calltrackingNumberClicksRepository.getClicksOnPhonesByDomainWithCounterIds(
                Mockito.eq(List.of(counterIdWithDomain)))
        ).thenReturn(
                Map.of(counterIdWithDomain,
                        Map.of(
                                PHONE_3, 3,
                                PHONE_4, 4,
                                PHONE_5, 1
                        )
                )
        );

        GdCalltrackingOnSiteInputItem item1 = new GdCalltrackingOnSiteInputItem()
                .withCalltrackingSettingsId(calltrackingSettingsId);

        GdCalltrackingOnSite input = new GdCalltrackingOnSite()
                .withItems(List.of(item1));

        var query = String.format(QUERY_TEMPLATE, chiefContext.getOperator().getLogin(), graphQlSerialize(input));
        ExecutionResult result = processor.processQuery(null, query, null, chiefContext);
        assumeThat(result.getErrors(), hasSize(0));

        Map<String, Object> data = result.getData();

        Map<String, Object> expected = Collections.singletonMap(
                "client",
                ImmutableMap.of(
                        "calltrackingOnSite", ImmutableMap.of(
                                "isMetrikaAvailable", true,
                                "items", ImmutableList.of(
                                        Map.of(
                                                "calltrackingSettingsId", calltrackingSettingsId,
                                                "domain", domainInfo1.getDomain().getDomain(),
                                                "counterId", COUNTER_ID,
                                                "counterStatus", GdCalltrackingOnSiteCounterStatus.OK.name(),
                                                "hasExternalCalltracking", true,
                                                "calltrackingPhones", ImmutableList.of(
                                                        ImmutableMap.builder()
                                                                .put("redirectPhone", "+79101112233")
                                                                .put("formattedRedirectPhone", ImmutableMap.builder()
                                                                        .put("countryCode", "+7")
                                                                        .put("cityCode", "910")
                                                                        .put("phoneNumber", "111-22-33")
                                                                        .build()
                                                                )
                                                                .put("hasClicks", true)
                                                                .put("isCalltrackingOnSiteEnabled", false)
                                                                .put("isCalltrackingOnSiteActive", false)
                                                                .put("isNew", false)
                                                                .put("hasNotReplacements", false)
                                                                .build(),
                                                        ImmutableMap.builder()
                                                                .put("redirectPhone", "+79101112231")
                                                                .put("formattedRedirectPhone", ImmutableMap.builder()
                                                                        .put("countryCode", "+7")
                                                                        .put("cityCode", "910")
                                                                        .put("phoneNumber", "111-22-31")
                                                                        .build()
                                                                )
                                                                .put("hasClicks", true)
                                                                .put("isCalltrackingOnSiteEnabled", true)
                                                                .put("isCalltrackingOnSiteActive", true)
                                                                .put("isNew", true)
                                                                .put("hasNotReplacements", false)
                                                                .build()
                                                )
                                        )
                                )
                        )
                )
        );

        BeanFieldPath prefix = newPath("client/calltrackingOnSite/items/\\d/calltrackingPhones/\\d/phoneId");
        CompareStrategy compareStrategy = DefaultCompareStrategies.allFields()
                .forFields(prefix)
                .useMatcher(nullValue());

        assertThat(data).is(matchedBy(beanDiffer(expected).useCompareStrategy(compareStrategy)));
    }

    @Test
    public void getCalltrackingOnSiteCheckCorrectIsNew() {
        ClientId clientId = clientInfo.getClientId();
        var firstCalltrackingSettingsId = steps.calltrackingSettingsSteps().add(
                clientId,
                domainInfo1.getDomainId(),
                COUNTER_ID,
                List.of(PHONE_1),
                true,
                CalltrackingSettingsMapper.DEFAULT_LAST_UPDATE
        );

        var secondCalltrackingSettingsId = steps.calltrackingSettingsSteps().add(
                clientId,
                domainInfo2.getDomainId(),
                COUNTER_ID,
                List.of(PHONE_2),
                true,
                LocalDateTime.now()
        );

        ClientPhone clientPhone = steps.clientPhoneSteps().addCalltrackingOnSitePhone(clientId, PHONE_1, COUNTER_ID);
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);

        steps.campCalltrackingPhonesSteps().add(
                clientInfo.getShard(),
                clientPhone.getId(),
                campaignInfo.getCampaignId());
        steps.campCalltrackingSettingsSteps().link(
                clientInfo.getShard(),
                campaignInfo.getCampaignId(),
                firstCalltrackingSettingsId
        );

        when(calltrackingNumberClicksRepository.getClicksOnPhonesByDomainWithCounterIds(anyList()))
                .thenReturn(Map.of());

        GdCalltrackingOnSiteInputItem item1 = new GdCalltrackingOnSiteInputItem()
                .withCalltrackingSettingsId(firstCalltrackingSettingsId);

        GdCalltrackingOnSiteInputItem item2 = new GdCalltrackingOnSiteInputItem()
                .withCalltrackingSettingsId(secondCalltrackingSettingsId);

        GdCalltrackingOnSite input = new GdCalltrackingOnSite()
                .withItems(List.of(item1, item2));

        var query = String.format(QUERY_TEMPLATE, chiefContext.getOperator().getLogin(), graphQlSerialize(input));
        ExecutionResult result = processor.processQuery(null, query, null, chiefContext);
        assumeThat(result.getErrors(), hasSize(0));

        Map<String, Object> data = result.getData();

        Map<String, Object> expected = Collections.singletonMap(
                "client",
                ImmutableMap.of(
                        "calltrackingOnSite", ImmutableMap.of(
                                "isMetrikaAvailable", true,
                                "items", ImmutableList.of(
                                        Map.of(
                                                "calltrackingSettingsId", firstCalltrackingSettingsId,
                                                "domain", domainInfo1.getDomain().getDomain(),
                                                "counterId", COUNTER_ID,
                                                "counterStatus", GdCalltrackingOnSiteCounterStatus.OK.name(),
                                                "hasExternalCalltracking", true,
                                                "calltrackingPhones", ImmutableList.of(
                                                        ImmutableMap.builder()
                                                                .put("redirectPhone", "+79101112231")
                                                                .put("formattedRedirectPhone", ImmutableMap.builder()
                                                                        .put("countryCode", "+7")
                                                                        .put("cityCode", "910")
                                                                        .put("phoneNumber", "111-22-31")
                                                                        .build()
                                                                )
                                                                .put("hasClicks", false)
                                                                .put("isCalltrackingOnSiteEnabled", true)
                                                                .put("isCalltrackingOnSiteActive", true)
                                                                .put("isNew", false)
                                                                .put("hasNotReplacements", false)
                                                                .build()
                                                )
                                        ),
                                        Map.of(
                                                "calltrackingSettingsId", secondCalltrackingSettingsId,
                                                "domain", domainInfo2.getDomain().getDomain(),
                                                "counterId", COUNTER_ID,
                                                "counterStatus", GdCalltrackingOnSiteCounterStatus.OK.name(),
                                                "hasExternalCalltracking", false,
                                                "calltrackingPhones", ImmutableList.of(
                                                        ImmutableMap.builder()
                                                                .put("redirectPhone", "+79101112232")
                                                                .put("formattedRedirectPhone", ImmutableMap.builder()
                                                                        .put("countryCode", "+7")
                                                                        .put("cityCode", "910")
                                                                        .put("phoneNumber", "111-22-32")
                                                                        .build()
                                                                )
                                                                .put("hasClicks", false)
                                                                .put("isCalltrackingOnSiteEnabled", true)
                                                                .put("isCalltrackingOnSiteActive", false)
                                                                .put("isNew", true)
                                                                .put("hasNotReplacements", false)
                                                                .build()
                                                )
                                        )
                                )
                        )
                )
        );

        BeanFieldPath prefix = newPath("client/calltrackingOnSite/items/\\d/calltrackingPhones/\\d/phoneId");
        CompareStrategy compareStrategy = DefaultCompareStrategies.allFields()
                .forFields(prefix)
                .useMatcher(nullValue());

        assertThat(data).is(matchedBy(beanDiffer(expected).useCompareStrategy(compareStrategy)));
    }


    @Test
    public void getCalltrackingOnSiteCheckCorrectHasNotReplacements() {
        ClientId clientId = clientInfo.getClientId();
        var firstCalltrackingSettingsId = steps.calltrackingSettingsSteps().add(
                clientId,
                domainInfo1.getDomainId(),
                COUNTER_ID,
                List.of(PHONE_1),
                true,
                CalltrackingSettingsMapper.DEFAULT_LAST_UPDATE
        );

        var secondCalltrackingSettingsId = steps.calltrackingSettingsSteps().add(
                clientId,
                domainInfo2.getDomainId(),
                COUNTER_ID,
                List.of(PHONE_2),
                true,
                LocalDateTime.now()
        );

        when(calltrackingNumberClicksRepository.getClicksOnPhonesByDomainWithCounterIds(anySet()))
                .thenReturn(Map.of());

        when(calltrackingPhonesWithoutReplacementsRepository.getPhonesWithoutReplacements(any(), any(), any()))
                .thenReturn(Set.of("+79101112231", "+79101112232"));

        GdCalltrackingOnSiteInputItem item1 = new GdCalltrackingOnSiteInputItem()
                .withCalltrackingSettingsId(firstCalltrackingSettingsId);

        GdCalltrackingOnSiteInputItem item2 = new GdCalltrackingOnSiteInputItem()
                .withCalltrackingSettingsId(secondCalltrackingSettingsId);

        GdCalltrackingOnSite input = new GdCalltrackingOnSite()
                .withItems(List.of(item1, item2));

        var query = String.format(QUERY_TEMPLATE, chiefContext.getOperator().getLogin(), graphQlSerialize(input));
        ExecutionResult result = processor.processQuery(null, query, null, chiefContext);
        assumeThat(result.getErrors(), hasSize(0));

        Map<String, Object> data = result.getData();

        Map<String, Object> expected = Collections.singletonMap(
                "client",
                ImmutableMap.of(
                        "calltrackingOnSite", ImmutableMap.of(
                                "isMetrikaAvailable", true,
                                "items", ImmutableList.of(
                                        Map.of(
                                                "calltrackingSettingsId", firstCalltrackingSettingsId,
                                                "domain", domainInfo1.getDomain().getDomain(),
                                                "counterId", COUNTER_ID,
                                                "counterStatus", GdCalltrackingOnSiteCounterStatus.OK.name(),
                                                "hasExternalCalltracking", true,
                                                "calltrackingPhones", ImmutableList.of(
                                                        ImmutableMap.builder()
                                                                .put("redirectPhone", "+79101112231")
                                                                .put("formattedRedirectPhone", ImmutableMap.builder()
                                                                        .put("countryCode", "+7")
                                                                        .put("cityCode", "910")
                                                                        .put("phoneNumber", "111-22-31")
                                                                        .build()
                                                                )
                                                                .put("hasClicks", false)
                                                                .put("isCalltrackingOnSiteEnabled", true)
                                                                .put("isCalltrackingOnSiteActive", false)
                                                                .put("isNew", false)
                                                                .put("hasNotReplacements", true)
                                                                .build()
                                                )
                                        ),
                                        Map.of(
                                                "calltrackingSettingsId", secondCalltrackingSettingsId,
                                                "domain", domainInfo2.getDomain().getDomain(),
                                                "counterId", COUNTER_ID,
                                                "counterStatus", GdCalltrackingOnSiteCounterStatus.OK.name(),
                                                "hasExternalCalltracking", false,
                                                "calltrackingPhones", ImmutableList.of(
                                                        ImmutableMap.builder()
                                                                .put("redirectPhone", "+79101112232")
                                                                .put("formattedRedirectPhone", ImmutableMap.builder()
                                                                        .put("countryCode", "+7")
                                                                        .put("cityCode", "910")
                                                                        .put("phoneNumber", "111-22-32")
                                                                        .build()
                                                                )
                                                                .put("hasClicks", false)
                                                                .put("isCalltrackingOnSiteEnabled", true)
                                                                .put("isCalltrackingOnSiteActive", false)
                                                                .put("isNew", true)
                                                                .put("hasNotReplacements", true)
                                                                .build()
                                                )
                                        )
                                )
                        )
                )
        );

        BeanFieldPath prefix = newPath("client/calltrackingOnSite/items/\\d/calltrackingPhones/\\d/phoneId");
        CompareStrategy compareStrategy = DefaultCompareStrategies.allFields()
                .forFields(prefix)
                .useMatcher(nullValue());

        assertThat(data).is(matchedBy(beanDiffer(expected).useCompareStrategy(compareStrategy)));
    }

}
