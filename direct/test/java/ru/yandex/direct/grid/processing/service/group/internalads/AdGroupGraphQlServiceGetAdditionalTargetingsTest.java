package ru.yandex.direct.grid.processing.service.group.internalads;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import graphql.ExecutionResult;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;
import org.jooq.Select;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargeting;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.grid.model.Order;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.group.GdAdGroup;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupFilter;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupOrderBy;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupOrderByField;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.GraphQLUtils;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.allValidInternalAdAdditionalTargetings;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.service.group.AdGroupGraphQlServiceTest.convertToGroupsRowset;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.getDefaultGdAdGroupsContainer;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupGraphQlServiceGetAdditionalTargetingsTest {
    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    adGroups(input: %s) {\n"
            + "      adGroupIds\n"
            + "      rowset {\n"
            + "        id\n"
            + "        ... on GdInternalAdGroup {\n"
            + "             targetings {\n"
            + "                 __typename\n"
            + "                 joinType\n"
            + "                 targetingMode\n"
            + "                 ... on GdAdditionalTargetingYandexUids {\n"
            + "                     yandexUids: value\n"
            + "                 }\n"
            + "                 ... on GdAdditionalTargetingQueryReferers {\n"
            + "                     referers: value\n"
            + "                 }\n"
            + "                 ... on GdAdditionalTargetingInterfaceLangs {\n"
            + "                     langs: value\n"
            + "                 }\n"
            + "                 ... on GdAdditionalTargetingUserAgents {\n"
            + "                     userAgents: value\n"
            + "                 }\n"
            + "                 ... on GdAdditionalTargetingBrowserEngines {\n"
            + "                     browserEngines: value {\n"
            + "                       targetingValueEntryId\n"
            + "                       maxVersion\n"
            + "                       maxVersion\n"
            + "                     }\n"
            + "                 }\n"
            + "                 ... on GdAdditionalTargetingBrowserNames {\n"
            + "                     browserNames: value {\n"
            + "                       targetingValueEntryId\n"
            + "                       maxVersion\n"
            + "                       maxVersion\n"
            + "                     }\n"
            + "                 }\n"
            + "                 ... on GdAdditionalTargetingOsFamilies {\n"
            + "                     osFamilies: value {\n"
            + "                       targetingValueEntryId\n"
            + "                       maxVersion\n"
            + "                       maxVersion\n"
            + "                     }\n"
            + "                 }\n"
            + "                 ... on GdAdditionalTargetingOsNames {\n"
            + "                     osNames: value {\n"
            + "                       targetingValueEntryId\n"
            + "                     }\n"
            + "                 }\n"
            + "                 ... on GdAdditionalTargetingDeviceVendors {\n"
            + "                     deviceVendors: value {\n"
            + "                       targetingValueEntryId\n"
            + "                     }\n"
            + "                 }\n"
            + "                 ... on GdAdditionalTargetingDeviceNames {\n"
            + "                     deviceNames: value\n"
            + "                 }\n"
            + "                 ... on GdAdditionalTargetingShowDates {\n"
            + "                     showDates: value\n"
            + "                 }\n"
            + "                 ... on GdAdditionalTargetingDesktopInstalledApps {\n"
            + "                     desktopApps: value\n"
            + "                 }\n"
            + "                 ... on GdAdditionalTargetingClidTypes {\n"
            + "                     clidTypes: value\n"
            + "                 }\n"
            + "                 ... on GdAdditionalTargetingClids {\n"
            + "                     clids: value\n"
            + "                 }\n"
            + "                 ... on GdAdditionalTargetingQueryOptions {\n"
            + "                     queryOptions: value\n"
            + "                 }\n"
            + "                 ... on GdAdditionalTargetingTestIds {\n"
            + "                     testIds: value\n"
            + "                 }\n"
            + "                 ... on GdAdditionalTargetingYsCookies {\n"
            + "                     ysCookies: value\n"
            + "                 }\n"
            + "                 ... on GdAdditionalTargetingMobileInstalledApps {\n"
            + "                     mobileApps: value\n"
            + "                 }\n"
            + "                 ... on GdAdditionalTargetingFeaturesInPP {\n"
            + "                     ppFeatures: value\n"
            + "                 }\n"
            + "                 ... on GdAdditionalTargetingYpCookies {\n"
            + "                     ypCookies: value\n"
            + "                 }\n"
            + "                 ... on GdAdditionalTargetingSids {\n"
            + "                     sids: value\n"
            + "                 }\n"
            + "                 ... on GdAdditionalTargetingUuids {\n"
            + "                     uuids: value\n"
            + "                 }\n"
            + "                 ... on GdAdditionalTargetingDeviceIds {\n"
            + "                     deviceIds: value\n"
            + "                 }\n"
            + "                 ... on GdAdditionalTargetingPlusUserSegments {\n"
            + "                     plusUserSegments: value\n"
            + "                 }\n"
            + "                 ... on GdAdditionalTargetingSearchText {\n"
            + "                     searchText: value\n"
            + "                 }\n"
            + "                 ... on GdAdditionalTargetingTime {\n"
            + "                     time: value {\n"
            + "                         timeBoard\n"
            + "                         useWorkingWeekends\n"
            + "                         idTimeZone\n"
            + "                     }\n"
            + "                 }\n"
            + "             }\n"
            + "         }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";

    private static final GdAdGroupOrderBy ORDER_BY_ID = new GdAdGroupOrderBy()
            .withField(GdAdGroupOrderByField.ID)
            .withOrder(Order.ASC);

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private GridContextProvider gridContextProvider;

    @Autowired
    private Steps steps;

    @Autowired
    private YtDynamicSupport gridYtSupport;

    private GridGraphQLContext context;
    private AdGroupInfo adGroupInfo;

    @Before
    public void before() {
        UserInfo productUserInfo = steps.internalAdProductSteps().createDefaultInternalAdProduct().getChiefUserInfo();
        adGroupInfo = steps.adGroupSteps().createActiveInternalAdGroup(productUserInfo.getClientInfo());

        UserInfo operatorInfo =
                steps.clientSteps().createDefaultClientWithRole(RbacRole.INTERNAL_AD_ADMIN).getChiefUserInfo();
        context = configureTestGridContext(operatorInfo, productUserInfo);
        gridYtDynamicSupportReturnsAdGroup(adGroupInfo);
    }

    @Test
    public void test() {
        List<AdGroupAdditionalTargeting> validTargetings = allValidInternalAdAdditionalTargetings();
        steps.adGroupAdditionalTargetingSteps().addValidTargetingsToAdGroup(adGroupInfo, validTargetings);

        ExecutionResult result = queryAdGroupWithTargetings(adGroupInfo);

        // здесь мы хотим удостовериться, что все таргетинги на группе смогли дойти до результата запроса
        // и не вызвали ошибок; если дошли, то подразумевается, что для них есть код поддержки со своими тестами,
        // поэтому проверяем только кол-во таргетингов, без структуры
        checkAdGroupAndReturnedTargetingsNumber(result, adGroupInfo, validTargetings);
    }

    private void checkAdGroupAndReturnedTargetingsNumber(ExecutionResult result, AdGroupInfo adGroupInfo,
                                                         List<AdGroupAdditionalTargeting> validTargetings) {
        Map<String, Object> data = result.getData();

        Map<String, Object> expected = singletonMap(
                "client",
                Map.of("adGroups", Map.of(
                        "rowset", getExpectedRowset(adGroupInfo))
                )
        );
        CompareStrategy compareStrategy = DefaultCompareStrategies.allFields()
                .forFields(newPath("client", "adGroups", "adGroupIds")).useMatcher(contains(this.adGroupInfo.getAdGroupId()))
                .forFields(newPath("client", "adGroups", "rowset", "0", "targetings"))
                .useMatcher(hasSize(validTargetings.size()));
        assertThat(data)
                .is(matchedBy(beanDiffer(expected).useCompareStrategy(compareStrategy)));
    }

    @SuppressWarnings("rawtypes")
    private List<Map> getExpectedRowset(AdGroupInfo... adGroups) {
        return StreamEx.of(adGroups)
                .map(AdGroupInfo::getAdGroup)
                .map(adGroup -> Map.of(GdAdGroup.ID.name(), adGroup.getId()))
                .map(Map.class::cast)
                .toList();
    }

    @NotNull
    private ExecutionResult queryAdGroupWithTargetings(AdGroupInfo adGroupInfo) {
        var adGroupsContainer = getDefaultGdAdGroupsContainer()
                .withFilter(new GdAdGroupFilter()
                        .withAdGroupIdIn(Set.of(adGroupInfo.getAdGroupId()))
                        .withCampaignIdIn(Set.of(adGroupInfo.getCampaignId()))
                )
                .withOrderBy(Collections.singletonList(ORDER_BY_ID));
        String query = String.format(QUERY_TEMPLATE, adGroupInfo.getClientInfo().getLogin(),
                graphQlSerialize(adGroupsContainer));
        ExecutionResult result = processor.processQuery(null, query, null, context);
        GraphQLUtils.logErrors(result.getErrors());
        assertThat(result.getErrors()).isEmpty();
        return result;
    }

    private GridGraphQLContext configureTestGridContext(UserInfo operatorInfo, UserInfo subjectUserInfo) {
        var context = ContextHelper.buildContext(operatorInfo.getUser(), subjectUserInfo.getUser());
        gridContextProvider.setGridContext(context);
        return context;
    }

    private void gridYtDynamicSupportReturnsAdGroup(AdGroupInfo adGroupInfo) {
        doAnswer((Answer<UnversionedRowset>) invocation -> {
            Select select = invocation.getArgument(1);

            //jooq заменяет "pid IN (пустое_множество)" на "1 = 0"
            // если sql запрос должен вернуть пустой результат, то возвращаем emptyList()
            if (select.getSQL().contains("AND 1 = 0")) {
                return convertToGroupsRowset(emptyList());
            }

            return convertToGroupsRowset(List.of(adGroupInfo));
        }).when(gridYtSupport).selectRows(eq(adGroupInfo.getShard()), any(Select.class), anyBoolean());
    }
}
