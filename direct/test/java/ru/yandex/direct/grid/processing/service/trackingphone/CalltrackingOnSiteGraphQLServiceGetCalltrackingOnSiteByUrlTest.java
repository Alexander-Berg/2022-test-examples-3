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
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.trackingphone.GdCalltrackingOnSiteCounterStatus;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.rbac.RbacRepType;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
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
public class CalltrackingOnSiteGraphQLServiceGetCalltrackingOnSiteByUrlTest {
    private static final Long COUNTER_ID = 123456L;
    private static final String PHONE_1 = "+79101112231";
    private static final String PHONE_2 = "+79101112232";
    private static final String PHONE_3 = "+79101112233";
    private static final String PHONE_4 = "+79101112234";
    private static final String PHONE_5 = "+79101112235";
    private static final String DOMAIN_POSTFIX = ".com";

    private static final String QUERY_TEMPLATE = "{\n" +
            "  client(searchBy: {login: \"%s\"}) {\n" +
            "    calltrackingOnSiteByUrl(url: %s) {\n" +
            "      isMetrikaAvailable\n" +
            "      item {\n" +
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

    private GridGraphQLContext chiefContext;

    private ClientInfo clientInfo;
    private DomainInfo domainInfo;

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();
        domainInfo = steps.domainSteps().createDomain(clientInfo.getShard(), DOMAIN_POSTFIX, true);

        // set up chef
        long chiefUserUid = clientInfo.getChiefUserInfo().getUser().getUid();
        User chiefUser = userService.getUser(chiefUserUid);
        chiefContext = ContextHelper.buildContext(chiefUser);

        metrikaClientStub.addUserCounters(chiefUserUid,
                List.of(MetrikaClientStub.buildCounter(COUNTER_ID.intValue())));

        // set up operator
        long operatorUserUid = steps.userSteps().createRepresentative(clientInfo, RbacRepType.MAIN).getUser().getUid();

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
    }

    @Test
    public void getCalltrackingOnSite_getByUrl() {
        var calltrackingSettingsId = steps.calltrackingSettingsSteps().add(
                clientInfo.getClientId(),
                domainInfo.getDomainId(),
                COUNTER_ID,
                List.of(PHONE_1, PHONE_2),
                true,
                LocalDateTime.now().minusDays(1L)
        );
        steps.calltrackingPhoneSteps().add(PHONE_5, LocalDateTime.now());
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

        var domain = domainInfo.getDomain().getDomain();
        var counterIdWithDomain = new CounterIdWithDomain(COUNTER_ID, domain);
        when(calltrackingNumberClicksRepository.getClicksOnPhonesByDomainWithCounterIds(Mockito.eq(List.of(counterIdWithDomain))))
                .thenReturn(
                        Map.of(counterIdWithDomain,
                                Map.of(
                                        PHONE_3, 3,
                                        PHONE_4, 4,
                                        PHONE_5, 1
                                )
                        )
                );
        when(calltrackingExternalRepository.getExternalCalltrackingDomains(anyCollection(), any()))
                .thenReturn(Set.of(domain));

        String url = "https://" + domain + "/page1";
        var query = String.format(QUERY_TEMPLATE, chiefContext.getOperator().getLogin(), graphQlSerialize(url));
        ExecutionResult result = processor.processQuery(null, query, null, chiefContext);
        assumeThat(result.getErrors(), hasSize(0));
        Map<String, Object> data = result.getData();
        Map<String, Object> expected = Collections.singletonMap(
                "client",
                ImmutableMap.of(
                        "calltrackingOnSiteByUrl", ImmutableMap.of(
                                "isMetrikaAvailable", true,
                                "item", Map.of(
                                        "calltrackingSettingsId", calltrackingSettingsId,
                                        "domain", domain,
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
                                                        .put("isCalltrackingOnSiteEnabled", true)
                                                        .put("isCalltrackingOnSiteActive", true)
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
                                                        .put("hasClicks", false)
                                                        .put("isCalltrackingOnSiteEnabled", true)
                                                        .put("isCalltrackingOnSiteActive", false)
                                                        .put("isNew", false)
                                                        .put("hasNotReplacements", false)
                                                        .build()
                                        )
                                )
                        )
                )
        );
        BeanFieldPath prefix = newPath("client/calltrackingOnSiteByUrl/item/calltrackingPhones/\\d/phoneId");
        CompareStrategy compareStrategy = DefaultCompareStrategies.allFields()
                .forFields(prefix)
                .useMatcher(nullValue());
        assertThat(data).is(matchedBy(beanDiffer(expected).useCompareStrategy(compareStrategy)));
    }
}
