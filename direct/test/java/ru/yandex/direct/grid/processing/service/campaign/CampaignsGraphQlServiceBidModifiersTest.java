package ru.yandex.direct.grid.processing.service.campaign;

import java.util.Collections;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import graphql.ExecutionResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile;
import ru.yandex.direct.core.entity.bidmodifier.OsType;
import ru.yandex.direct.core.testing.data.TestBidModifiers;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignsContainer;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.bidmodifier.BidModifierGraphQlService;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.CampaignTestDataUtils;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.GraphQLUtils;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultMobileAdjustment;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignsGraphQlServiceBidModifiersTest {
    private static final Boolean MODIFIER_ENABLED = true;
    private static final Integer MODIFIER_PERCENT =
            RandomNumberUtils.nextPositiveInteger() % TestBidModifiers.PERCENT_MAX;
    private static final OsType MODIFIER_OS_TYPE = OsType.IOS;
    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    campaigns(input: %s) {\n"
            + "      rowset {\n"
            + "        bidModifiers{\n"
            + "          id\n"
            + "          type\n"
            + "          enabled\n"
            + "          campaignId\n"
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

    private GdCampaignsContainer campaignsContainer;
    private CampaignInfo campaignInfo;
    private GridGraphQLContext context;
    private BidModifierMobile bidModifier;

    @Autowired
    private GridContextProvider gridContextProvider;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private Steps steps;

    @Before
    public void initTestData() {
        UserInfo userInfo = steps.userSteps().createUser(generateNewUser());
        Campaign camp = activeTextCampaign(null, null)
                .withName("Name 2");
        campaignInfo = steps.campaignSteps().createCampaign(new CampaignInfo()
                .withClientInfo(userInfo.getClientInfo())
                .withCampaign(camp));

        bidModifier = TestBidModifiers.createEmptyMobileModifier()
                .withCampaignId(campaignInfo.getCampaignId())
                .withEnabled(MODIFIER_ENABLED)
                .withMobileAdjustment(createDefaultMobileAdjustment()
                        .withPercent(MODIFIER_PERCENT)
                        .withOsType(MODIFIER_OS_TYPE));

        steps.bidModifierSteps().createCampaignBidModifier(bidModifier, campaignInfo);

        campaignsContainer = CampaignTestDataUtils.getDefaultCampaignsContainerInput();

        context = ContextHelper.buildContext(userInfo.getUser())
                .withFetchedFieldsReslover(null);
        gridContextProvider.setGridContext(context);
    }

    @Test
    public void testService() {
        campaignsContainer.getFilter().setCampaignIdIn(ImmutableSet.of(campaignInfo.getCampaignId()));
        campaignsContainer.getLimitOffset().withLimit(1).withOffset(0);

        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(),
                graphQlSerialize(campaignsContainer));
        ExecutionResult result = processor.processQuery(null, query, null, context);

        GraphQLUtils.logErrors(result.getErrors());

        assertThat(result.getErrors())
                .isEmpty();
        Map<String, Object> data = result.getData();
        Map<String, Object> expected = ImmutableMap.of(
                "client",
                ImmutableMap.of(
                        "campaigns", ImmutableMap.of(
                                "rowset", Collections.singletonList(ImmutableMap.of(
                                        BidModifierGraphQlService.CAMPAIGN_BID_MODIFIERS_RESOLVER_NAME,
                                        Collections.singletonList(ImmutableMap.of(
                                                "id", bidModifier.getId(),
                                                "type", bidModifier.getType().name(),
                                                "enabled", bidModifier.getEnabled(),
                                                "campaignId", campaignInfo.getCampaignId(),
                                                "adjustments", Collections.singletonList(ImmutableMap.of(
                                                        "osType", bidModifier.getMobileAdjustment().getOsType().name(),
                                                        "id",
                                                        bidModifier.getMobileAdjustment().getId(),
                                                        "percent", bidModifier.getMobileAdjustment().getPercent()
                                                ))
                                        ))
                                ))
                        )
                )
        );
        assertThat(data)
                .is(matchedBy(beanDiffer(expected)));
    }
}
