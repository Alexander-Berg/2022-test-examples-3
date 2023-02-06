package ru.yandex.direct.grid.processing.service.group;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import graphql.ExecutionResult;
import jdk.jfr.Description;
import org.jooq.Select;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.grid.model.Order;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.group.GdAdGroup;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupFilter;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupOrderBy;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupOrderByField;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupsContainer;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupsContext;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.GraphQLUtils;
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.feature.FeatureName.SHOW_CPM_PRICE_CAMPAIGNS_IN_GRID;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.service.client.ClientGraphQlService.CLIENT_RESOLVER_NAME;
import static ru.yandex.direct.grid.processing.service.group.AdGroupGraphQlService.AD_GROUPS_RESOLVER_NAME;
import static ru.yandex.direct.grid.processing.service.group.AdGroupGraphQlService.CAN_BE_DELETED_AD_GROUPS_COUNT_RESOLVER_NAME;
import static ru.yandex.direct.grid.processing.service.group.AdGroupGraphQlService.CAN_DELETED_AD_GROUP_RESOLVER_NAME;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.getDefaultGdAdGroupsContainer;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.schema.yt.Tables.PHRASESTABLE_DIRECT;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowBuilder.rowBuilder;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder.rowsetBuilder;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupGraphQlServiceCpmPriceTest {

    private static final String CAN_DELETE_QUERY_TEMPLATE_WITH_CACHE_KEY = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    adGroups(input: %s) {\n"
            + "      features {\n"
            + "         " + CAN_BE_DELETED_AD_GROUPS_COUNT_RESOLVER_NAME + "\n"
            + "      }\n"
            + "      cacheKey\n"
            + "      rowset {\n"
            + "        access {\n"
            + "          " + CAN_DELETED_AD_GROUP_RESOLVER_NAME + "\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";

    private static final GdAdGroupOrderBy ORDER_BY_ID = new GdAdGroupOrderBy()
            .withField(GdAdGroupOrderByField.ID)
            .withOrder(Order.ASC);

    private GdAdGroupsContainer adGroupsContainer;
    private GridGraphQLContext context;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private GridContextProvider gridContextProvider;

    @Autowired
    private Steps steps;

    @Autowired
    private UserSteps userSteps;

    @Autowired
    private YtDynamicSupport gridYtSupport;
    private UserInfo userInfo;

    @Before
    public void initTestData() {
        userInfo = userSteps.createUser(generateNewUser());
        var clientInfo = userInfo.getClientInfo();
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), SHOW_CPM_PRICE_CAMPAIGNS_IN_GRID, true);
        var cpmPriceCampaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo);
        var defaultAdGroup = steps.adGroupSteps().createDefaultAdGroupForPriceSales(cpmPriceCampaign, clientInfo);
        var specificAdGroup = steps.adGroupSteps().createSpecificAdGroupForPriceSales(cpmPriceCampaign, clientInfo);

        doAnswer((Answer<UnversionedRowset>) invocation -> {
            RowsetBuilder rowsetBuilder = rowsetBuilder();
            addRowBuilder(rowsetBuilder, cpmPriceCampaign, defaultAdGroup);
            addRowBuilder(rowsetBuilder, cpmPriceCampaign, specificAdGroup);
            return rowsetBuilder.build();
        }).when(gridYtSupport).selectRows(eq(userInfo.getShard()), any(Select.class), anyBoolean());

        adGroupsContainer = getDefaultGdAdGroupsContainer()
                .withFilter(new GdAdGroupFilter()
                        .withCampaignIdIn(ImmutableSet.of(cpmPriceCampaign.getId()))
                        .withAdGroupIdIn(ImmutableSet.of(defaultAdGroup.getId(), specificAdGroup.getId()))
                )
                .withOrderBy(Collections.singletonList(ORDER_BY_ID));
    }

    private static void addRowBuilder(RowsetBuilder builder, CpmPriceCampaign cpmPriceCampaign,
                                      CpmYndxFrontpageAdGroup cpmPriceAdGroup) {
        builder.add(
                rowBuilder()
                        .withColValue(PHRASESTABLE_DIRECT.PID.getName(), cpmPriceAdGroup.getId())
                        .withColValue(PHRASESTABLE_DIRECT.CID.getName(), cpmPriceCampaign.getId())
                        .withColValue(PHRASESTABLE_DIRECT.ADGROUP_TYPE.getName(),
                                cpmPriceAdGroup.getType().name().toLowerCase()));
    }

    @Test
    @Description("В прайсовой кампании дефолтную группу удалить нельзя, а специфическую - можно.")
    public void testCanDeleteAdGroupsResolvers() {
        for (int i = 0; i < 2; i++) {
            // Хотим чтобы на втором прогоне данные достались из кэша, а context был свежий, т.к. тут потанциально
            // возможны ошибки.
            // Есть две ветви формирования context - когда стандартно грузим из базы и когда из кэша, нужно проверить
            // обе ветви.
            context = ContextHelper.buildContext(userInfo.getUser());
            gridContextProvider.setGridContext(context);

            ExecutionResult result = processQuery(CAN_DELETE_QUERY_TEMPLATE_WITH_CACHE_KEY);

            GraphQLUtils.logErrors(result.getErrors());

            assertThat(result.getErrors())
                    .isEmpty();
            Map<String, Object> data = result.getData();

            Map<String, Object> expectedAccessForDefaultAdGroup = singletonMap(GdAdGroup.ACCESS.name(),
                    singletonMap(CAN_DELETED_AD_GROUP_RESOLVER_NAME, false));
            Map<String, Object> expectedAccessForSpecificAdGroup = singletonMap(GdAdGroup.ACCESS.name(),
                    singletonMap(CAN_DELETED_AD_GROUP_RESOLVER_NAME, true));
            Map<String, Object> expected = singletonMap(CLIENT_RESOLVER_NAME,
                    ImmutableMap.of(AD_GROUPS_RESOLVER_NAME, ImmutableMap.builder()
                            .put(GdAdGroupsContext.FEATURES.name(),
                                    singletonMap(CAN_BE_DELETED_AD_GROUPS_COUNT_RESOLVER_NAME, 1))
                            .put(GdAdGroupsContext.ROWSET.name(),
                                    Arrays.asList(expectedAccessForDefaultAdGroup, expectedAccessForSpecificAdGroup))
                            .build()
                    )
            );

            assertThat(data)
                    .is(matchedBy(beanDiffer(expected).useCompareStrategy(allFieldsExcept(newPath(".*", "cacheKey")))));

            String cacheKey = extractCacheKey(data);
            adGroupsContainer.setCacheKey(cacheKey);
        }

    }

    @SuppressWarnings("unchecked")
    private static String extractCacheKey(Object data) {
        var clientData = ((Map<String, Object>) data).get("client");
        var adGroupsData = ((Map<String, Object>) clientData).get("adGroups");
        return (String) ((Map<String, Object>) adGroupsData).get("cacheKey");
    }

    private ExecutionResult processQuery(String queryTemplate) {
        String query = String.format(queryTemplate, context.getOperator().getLogin(),
                graphQlSerialize(adGroupsContainer));
        return processor.processQuery(null, query, null, context);
    }

}
