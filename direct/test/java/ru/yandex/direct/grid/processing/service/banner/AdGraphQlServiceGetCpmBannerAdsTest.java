package ru.yandex.direct.grid.processing.service.banner;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableSet;
import graphql.ExecutionResult;
import org.jooq.Select;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerAdditionalHref;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.grid.model.Order;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.banner.GdAdFilter;
import ru.yandex.direct.grid.processing.model.banner.GdAdOrderBy;
import ru.yandex.direct.grid.processing.model.banner.GdAdOrderByField;
import ru.yandex.direct.grid.processing.model.banner.GdAdsContainer;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.feature.FeatureName.SHOW_CPM_PRICE_CAMPAIGNS_IN_GRID;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.data.TestGdAds.getDefaultGdAdsContainer;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.schema.yt.Tables.BANNERSTABLE_DIRECT;
import static ru.yandex.direct.grid.schema.yt.Tables.PHRASESTABLE_DIRECT;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowBuilder.rowBuilder;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder.rowsetBuilder;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGraphQlServiceGetCpmBannerAdsTest {

    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    ads(input: %s) {\n"
            + "      rowset {\n"
            + "        id\n"
            + "        ... on GdCPMBannerAd {\n"
            + "          additionalHrefs {\n"
            + "            href\n"
            + "          }\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";

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
    private CpmPriceCampaign campaign;
    private CpmYndxFrontpageAdGroup adGroup;
    private CreativeInfo creative;
    private ClientInfo client;

    @Before
    public void initTestData() {
        client = steps.clientSteps().createDefaultClient();
        campaign = steps.campaignSteps().createActiveCpmPriceCampaign(client);
        adGroup = steps.adGroupSteps().createDefaultAdGroupForPriceSales(campaign, client);
        creative = steps.creativeSteps().addDefaultHtml5CreativeForPriceSales(client, campaign);

        steps.featureSteps().addClientFeature(client.getClientId(), SHOW_CPM_PRICE_CAMPAIGNS_IN_GRID, true);
        context = ContextHelper.buildContext(client.getChiefUserInfo().getUser());
        gridContextProvider.setGridContext(context);
    }

    @Test
    public void testService() {
        OldCpmBanner banner = activeCpmBanner(campaign.getId(), adGroup.getId(), creative.getCreativeId())
                .withAdditionalHrefs(List.of(
                        new OldBannerAdditionalHref().withHref("http://google.com"),
                        new OldBannerAdditionalHref().withHref("http://bing.com")
                ));
        steps.bannerSteps().createActiveCpmBannerRaw(client.getShard(), banner, adGroup);
        mockYtResult(adGroup, banner);

        ExecutionResult result = processQueryGetBanner(banner.getId(), banner.getCampaignId());
        checkErrors(result.getErrors());

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = Map.of(
                "client",
                Map.of(
                        "ads", Map.of(
                                "rowset", Collections.singletonList(Map.of(
                                        "id", banner.getId(),
                                        "additionalHrefs", List.of(
                                                Map.of("href", "http://google.com"),
                                                Map.of("href", "http://bing.com")
                                        )
                                )))
                )
        );
        assertThat(data)
                .is(matchedBy(beanDiffer(expected)));
    }

    private ExecutionResult processQueryGetBanner(Long bannerId, Long campaignId) {
        GdAdsContainer adsContainer = getDefaultGdAdsContainer()
                .withFilter(new GdAdFilter()
                        .withAdIdIn(singleton(bannerId))
                        .withCampaignIdIn(ImmutableSet.of(campaignId)))
                .withOrderBy(Collections.singletonList(new GdAdOrderBy()
                        .withField(GdAdOrderByField.ID)
                        .withOrder(Order.ASC)));
        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(), graphQlSerialize(adsContainer));
        return processor.processQuery(null, query, null, context);
    }

    private void mockYtResult(CpmYndxFrontpageAdGroup adGroup, OldCpmBanner banner) {
        doAnswer(invocation -> {
            Select query = invocation.getArgument(1);
            if (query.toString().contains(BANNERSTABLE_DIRECT.getName())) {
                return ytBannerRowset(banner);
            }
            return ytAdGroupRowset(adGroup);
        })
                .when(gridYtSupport).selectRows(anyInt(), any(Select.class), anyBoolean());
    }

    private static UnversionedRowset ytBannerRowset(OldCpmBanner banner) {
        RowsetBuilder builder = rowsetBuilder();
        builder.add(rowBuilder()
                .withColValue(BANNERSTABLE_DIRECT.BID.getName(), banner.getId())
                .withColValue(BANNERSTABLE_DIRECT.PID.getName(), banner.getAdGroupId())
                .withColValue(BANNERSTABLE_DIRECT.CID.getName(), banner.getCampaignId())
                .withColValue(BANNERSTABLE_DIRECT.BANNER_TYPE.getName(), banner.getBannerType().name())
                .withColValue(BANNERSTABLE_DIRECT.STATUS_SHOW.getName(), "Yes")
                .withColValue(BANNERSTABLE_DIRECT.STATUS_ACTIVE.getName(), "Yes")
                .withColValue(BANNERSTABLE_DIRECT.STATUS_ARCH.getName(), "No")
                .withColValue(BANNERSTABLE_DIRECT.STATUS_BS_SYNCED.getName(), "Yes")
        );
        return builder.build();
    }

    private static UnversionedRowset ytAdGroupRowset(CpmYndxFrontpageAdGroup adGroup) {
        RowsetBuilder builder = rowsetBuilder();
        builder.add(rowBuilder()
                .withColValue(PHRASESTABLE_DIRECT.PID.getName(), adGroup.getId())
                .withColValue(PHRASESTABLE_DIRECT.CID.getName(), adGroup.getCampaignId())
                .withColValue(PHRASESTABLE_DIRECT.ADGROUP_TYPE.getName(), adGroup.getType().name())
        );
        return builder.build();
    }

}
