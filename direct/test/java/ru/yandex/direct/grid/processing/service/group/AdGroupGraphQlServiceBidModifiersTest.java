package ru.yandex.direct.grid.processing.service.group;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import graphql.ExecutionResult;
import one.util.streamex.StreamEx;
import org.jooq.Select;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile;
import ru.yandex.direct.core.entity.bidmodifier.OsType;
import ru.yandex.direct.core.testing.data.TestBidModifiers;
import ru.yandex.direct.core.testing.data.TestGroups;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.grid.model.Order;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupFilter;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupOrderBy;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupOrderByField;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupsContainer;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.bidmodifier.BidModifierGraphQlService;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.GraphQLUtils;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultMobileAdjustment;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.getDefaultGdAdGroupsContainer;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.schema.yt.Tables.PHRASESTABLE_DIRECT;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowBuilder.rowBuilder;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder.rowsetBuilder;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupGraphQlServiceBidModifiersTest {
    private static final Boolean MODIFIER_ENABLED = true;
    private static final Integer MODIFIER_PERCENT =
            RandomNumberUtils.nextPositiveInteger() % TestBidModifiers.PERCENT_MAX;
    private static final OsType MODIFIER_OS_TYPE = OsType.IOS;

    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    adGroups(input: %s) {\n"
            + "      rowset {\n"
            + "        bidModifiers{\n"
            + "          id\n"
            + "          type\n"
            + "          enabled\n"
            + "          campaignId\n"
            + "          adGroupId\n"
            + "          adjustments{\n"
            + "            id\n"
            + "            percent\n"
            + "            ... on GdBidModifierMobileAdjustment{\n"
            + "                osType\n"
            + "            }\n"
            + "          }\n"
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
    private AdGroupInfo groupInfo;
    private BidModifierMobile bidModifier;

    @Autowired
    private GridContextProvider gridContextProvider;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private Steps steps;

    @Autowired
    private YtDynamicSupport gridYtSupport;

    @Before
    public void initTestData() {
        UserInfo userInfo = steps.userSteps().createUser(generateNewUser());

        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCampaign(userInfo.getClientInfo());
        groupInfo = steps.adGroupSteps()
                .createAdGroup(TestGroups.activeTextAdGroup()
                        .withGeo(asList(Region.RUSSIA_REGION_ID, Region.CRIMEA_REGION_ID)), campaignInfo);

        doReturn(convertToGroupsRowset(Collections.singletonList(groupInfo)))
                .when(gridYtSupport).selectRows(eq(userInfo.getShard()), any(Select.class), anyBoolean());

        adGroupsContainer = getDefaultGdAdGroupsContainer()
                .withFilter(new GdAdGroupFilter()
                        .withAdGroupIdIn(ImmutableSet.of(groupInfo.getAdGroupId()))
                        .withCampaignIdIn(ImmutableSet.of(campaignInfo.getCampaignId()))
                )
                .withOrderBy(Collections.singletonList(ORDER_BY_ID));

        bidModifier = TestBidModifiers.createDefaultBidModifierMobile(campaignInfo.getCampaignId())
                .withCampaignId(campaignInfo.getCampaignId())
                .withEnabled(MODIFIER_ENABLED)
                .withMobileAdjustment(createDefaultMobileAdjustment()
                        .withPercent(MODIFIER_PERCENT)
                        .withOsType(MODIFIER_OS_TYPE));

        steps.bidModifierSteps().createAdGroupBidModifier(bidModifier, groupInfo);

        context = ContextHelper.buildContext(userInfo.getUser())
                .withFetchedFieldsReslover(null);
        gridContextProvider.setGridContext(context);
    }

    @Test
    public void testService() {
        ExecutionResult result = processQuery();

        GraphQLUtils.logErrors(result.getErrors());

        assertThat(result.getErrors())
                .isEmpty();
        Map<String, Object> data = result.getData();

        List<Map> expectedRowset = StreamEx.of(groupInfo)
                .map(AdGroupInfo::getAdGroup)
                .map(adGroup -> ImmutableMap.of(
                        BidModifierGraphQlService.AD_GROUP_BID_MODIFIERS_RESOLVER_NAME,
                        Collections.singletonList(new ImmutableMap.Builder<String, Object>()
                                .put("id", bidModifier.getId())
                                .put("type", bidModifier.getType().name())
                                .put("enabled", bidModifier.getEnabled())
                                .put("campaignId", groupInfo.getCampaignId())
                                .put("adGroupId", groupInfo.getAdGroupId())
                                .put("adjustments", Collections.singletonList(ImmutableMap.of(
                                        "osType", bidModifier.getMobileAdjustment().getOsType().name(),
                                        "id",
                                        bidModifier.getMobileAdjustment().getId(),
                                        "percent", bidModifier.getMobileAdjustment().getPercent()
                                ))).build()
                        )
                ))
                .map(Map.class::cast)
                .toList();

        Map<String, Object> expected = singletonMap(
                "client",
                ImmutableMap.of("adGroups", ImmutableMap.of(
                        "rowset", expectedRowset)
                )
        );

        assertThat(data)
                .is(matchedBy(beanDiffer(expected)));
    }

    private ExecutionResult processQuery() {
        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(),
                graphQlSerialize(adGroupsContainer));
        return processor.processQuery(null, query, null, context);
    }

    public static UnversionedRowset convertToGroupsRowset(List<AdGroupInfo> infos) {
        RowsetBuilder builder = rowsetBuilder();
        infos.forEach(info -> builder.add(
                rowBuilder()
                        .withColValue(PHRASESTABLE_DIRECT.PID.getName(), info.getAdGroupId())
                        .withColValue(PHRASESTABLE_DIRECT.CID.getName(), info.getCampaignId())
                        .withColValue(PHRASESTABLE_DIRECT.ADGROUP_TYPE.getName(),
                                info.getAdGroupType().name().toLowerCase())
        ));

        return builder.build();
    }
}
