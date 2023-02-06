package ru.yandex.direct.grid.processing.service.client;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import graphql.ExecutionResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants;
import ru.yandex.direct.core.entity.user.utils.UserUtil;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.ClientMccSteps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.grid.model.campaign.notification.GdCampaignEmailEvent;
import ru.yandex.direct.grid.model.campaign.notification.GdCampaignSmsEvent;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.grid.model.campaign.GdCampaignType.CONTENT_PROMOTION;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientGraphQlServiceProcessingTest {
    private static final String QUERY_TEMPLATE = "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    info {\n"
            + "      id\n"
            + "      shard\n"
            + "      clientMccCommonInfo {\n"
            + "        hasManagedClients\n"
            + "        hasControlRequests\n"
            + "        canUseClientMccCommonInfo\n"
            + "      }\n"
            + "    }\n"
            + "    access {\n"
            + "      operatorIsClient\n"
            + "    }\n"
            + "    features {\n"
            + "      hasCampaignsWithStats\n"
            + "    }\n"
            + "    metrikaCounters {\n"
            + "      counters\n"
            + "      isMetrikaAvailable\n"
            + "    }\n"
            + "    defaultCampaignNotification {\n"
            + "      smsSettings {\n"
            + "        smsTime {\n"
            + "          startTime {\n"
            + "            hour\n"
            + "            minute\n"
            + "          }\n"
            + "          endTime {\n"
            + "            hour\n"
            + "            minute\n"
            + "          }\n"
            + "        }\n"
            + "        events {\n"
            + "          checked\n"
            + "          event\n"
            + "        }\n"
            + "      }\n"
            + "      emailSettings {\n"
            + "        email\n"
            + "        allowedEvents\n"
            + "        checkPositionInterval\n"
            + "        warningBalance\n"
            + "        sendAccountNews\n"
            + "        xlsReady\n"
            + "        stopByReachDailyBudget\n"
            + "      }\n"
            + "    }\n"
            + "    defaultCampaignNotifications(campaignTypes: %s) {\n"
            + "      campaignType\n"
            + "      smsSettings {\n"
            + "        smsTime {\n"
            + "          startTime {\n"
            + "            hour\n"
            + "            minute\n"
            + "          }\n"
            + "          endTime {\n"
            + "            hour\n"
            + "            minute\n"
            + "          }\n"
            + "        }\n"
            + "        events {\n"
            + "          checked\n"
            + "          event\n"
            + "        }\n"
            + "      }\n"
            + "      emailSettings {\n"
            + "        email\n"
            + "        allowedEvents\n"
            + "        checkPositionInterval\n"
            + "        warningBalance\n"
            + "        sendAccountNews\n"
            + "        xlsReady\n"
            + "        stopByReachDailyBudget\n"
            + "      }\n"
            + "    }\n"
            + "    campaignsCount\n"
            + "    chiefLogin\n"
            + "  }\n"
            + "}\n";

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private GridContextProvider gridContextProvider;

    @Autowired
    private UserSteps userSteps;

    @Autowired
    private ClientMccSteps clientMccSteps;

    @Test
    public void testService() {
        UserInfo userInfo = userSteps.createUser(generateNewUser());
        var managedUserInfo = userSteps.createDefaultUser();
        var mccRequestUserInfo = userSteps.createDefaultUser();

        clientMccSteps.createClientMccLink(userInfo.getClientId(), managedUserInfo.getClientId());
        clientMccSteps.addMccRequest(userInfo.getClientId(), mccRequestUserInfo.getClientId());

        GridGraphQLContext context = new GridGraphQLContext(userInfo.getUser());
        gridContextProvider.setGridContext(context);
        String query = String.format(QUERY_TEMPLATE, userInfo.getUser().getLogin(), asList(CONTENT_PROMOTION));

        ExecutionResult result = processor.processQuery(null, query, null, context);

        assertThat(result.getErrors())
                .isEmpty();

        Map<String, Object> data = result.getData();

        Map<Object, Object> smsSettings = Map.of(
                "smsTime", Map.of(
                        "endTime", Map.of("hour", 21, "minute", 0),
                        "startTime", Map.of("hour", 9, "minute", 0))
        );
        Map<Object, Object> emailSettings = Map.of(
                "email", userInfo.getUser().getEmail(),
                "sendAccountNews", true,
                "xlsReady", true,
                "stopByReachDailyBudget", true,
                "warningBalance", CampaignConstants.DEFAULT_CAMPAIGN_WARNING_BALANCE
        );
        Map<String, Map<Object, Object>> notification = Map.of("emailSettings", emailSettings, "smsSettings",
                smsSettings);
        List<Object> newNotifications = List.of(
                Map.of("emailSettings", emailSettings,
                        "smsSettings", smsSettings,
                        "campaignType", CONTENT_PROMOTION.name()));

        Map<String, Object> expected = Collections.singletonMap(
                "client",
                Map.of(
                        "info", ImmutableMap.of(
                                "id", userInfo.getClientInfo().getClientId().asLong(),
                                "shard", userInfo.getShard(),
                                "clientMccCommonInfo", ImmutableMap.of(
                                        "hasManagedClients", true,
                                        "hasControlRequests", true,
                                        "canUseClientMccCommonInfo", false
                                )
                        ),
                        "access", Collections.singletonMap("operatorIsClient", UserUtil.isClient(userInfo.getUser())),
                        "features", Collections.singletonMap("hasCampaignsWithStats", false),
                        "metrikaCounters", Map.of("counters", List.of(), "isMetrikaAvailable", true),
                        "campaignsCount", 0,
                        "defaultCampaignNotification", notification,
                        "defaultCampaignNotifications", newNotifications,
                        "chiefLogin", userInfo.getUser().getLogin()
                )
        );
        CompareStrategy compareStrategy = DefaultCompareStrategies.allFields()
                .forFields(newPath("client", "defaultCampaignNotification", "emailSettings", "checkPositionInterval"))
                .useMatcher(nullValue())
                .forFields(newPath("client", "defaultCampaignNotification", "smsSettings", "events"))
                .useMatcher(containsInAnyOrder(
                        Map.of("event", GdCampaignSmsEvent.STOP_BY_REACH_DAILY_BUDGET.name(), "checked", false),
                        Map.of("event", GdCampaignSmsEvent.MONEY_IN.name(), "checked", false),
                        Map.of("event", GdCampaignSmsEvent.MONEY_OUT.name(), "checked", false),
                        Map.of("event", GdCampaignSmsEvent.FINISHED.name(), "checked", false),
                        Map.of("event", GdCampaignSmsEvent.MODERATION.name(), "checked", false),
                        Map.of("event", GdCampaignSmsEvent.MONITORING.name(), "checked", false)))
                .forFields(newPath("client", "defaultCampaignNotification", "emailSettings", "allowedEvents"))
                .useMatcher(containsInAnyOrder(
                        GdCampaignEmailEvent.WARNING_BALANCE.name(),
                        GdCampaignEmailEvent.XLS_READY.name(),
                        GdCampaignEmailEvent.CHECK_POSITION.name(),
                        GdCampaignEmailEvent.STOP_BY_REACH_DAILY_BUDGET.name()))

                .forFields(newPath("client", "defaultCampaignNotifications", "0", "emailSettings",
                        "checkPositionInterval"))
                .useMatcher(nullValue())
                .forFields(newPath("client", "defaultCampaignNotifications", "0", "smsSettings", "events"))
                .useMatcher(containsInAnyOrder(
                        Map.of("event", GdCampaignSmsEvent.STOP_BY_REACH_DAILY_BUDGET.name(), "checked", false),
                        Map.of("event", GdCampaignSmsEvent.MONEY_IN.name(), "checked", false),
                        Map.of("event", GdCampaignSmsEvent.MONEY_OUT.name(), "checked", false),
                        Map.of("event", GdCampaignSmsEvent.FINISHED.name(), "checked", false),
                        Map.of("event", GdCampaignSmsEvent.MODERATION.name(), "checked", false)))
                .forFields(newPath("client", "defaultCampaignNotifications", "0", "emailSettings", "allowedEvents"))
                .useMatcher(containsInAnyOrder(
                        GdCampaignEmailEvent.WARNING_BALANCE.name(),
                        GdCampaignEmailEvent.STOP_BY_REACH_DAILY_BUDGET.name()));

        assertThat(data)
                .is(matchedBy(beanDiffer(expected).useCompareStrategy(compareStrategy)));
    }

}
