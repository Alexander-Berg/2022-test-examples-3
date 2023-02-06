package ru.yandex.direct.grid.processing.service.banner;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.addition.callout.model.Callout;
import ru.yandex.direct.core.entity.banner.model.DynamicBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldDynamicBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.service.BannerService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.DynamicBannerInfo;
import ru.yandex.direct.core.testing.repository.TestBannerRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.BannersStatusarch;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceCallouts;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceCalloutsAction;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceCalloutsInstruction;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceCalloutsPayloadItem;
import ru.yandex.direct.grid.processing.model.common.GdCachedResult;
import ru.yandex.direct.grid.processing.model.common.GdResult;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.cannotUpdateArchivedAd;
import static ru.yandex.direct.core.testing.data.TestBanners.activeDynamicBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.emptyValidationResult;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdValidationResult;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FindAndReplaceGraphQLServiceFindAndReplaceBannerCalloutsTest {

    private static final String FIND_AND_REPLACE_CALLOUTS_PREVIEW_QUERY = "findAndReplaceCalloutsPreview";
    private static final String FIND_AND_REPLACE_CALLOUTS_MUTATION = "findAndReplaceCallouts";

    private static final String PREVIEW_QUERY_TEMPLATE = ""
            + "query {\n"
            + "  %s (input: %s) {\n"
            + "    totalCount,\n"
            + "    rowset {\n"
            + "      adId\n"
            + "      oldCalloutIds\n"
            + "      newCalloutIds\n"
            + "    }\n"
            + "  }\n"
            + "}";
    private static final String FIND_AND_REPLACE_MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "    totalCount,\n"
            + "    successCount,\n"
            + "    rowset {\n"
            + "      adId\n"
            + "      oldCalloutIds\n"
            + "      newCalloutIds\n"
            + "    }\n"
            + "    validationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        path\n"
            + "        params\n"
            + "      },\n"
            + "      warnings {\n"
            + "        code\n"
            + "        path\n"
            + "        params\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}";

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private Steps steps;
    @Autowired
    private BannerService bannerService;

    @Autowired
    private TestBannerRepository testBannerRepository;

    private ClientInfo clientInfo;
    private GdFindAndReplaceCallouts request;
    private User operator;

    private OldTextBanner banner;
    private Callout firstCallout;
    private Callout secondCallout;

    @Before
    public void initTestData() {
        clientInfo = steps.clientSteps().createDefaultClient();

        firstCallout = steps.calloutSteps().createDefaultCallout(clientInfo);
        secondCallout = steps.calloutSteps().createDefaultCallout(clientInfo);

        var campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);
        banner = steps.bannerSteps()
                .createBanner(activeTextBanner(null, null)
                                .withCalloutIds(List.of(firstCallout.getId())),
                        campaignInfo).getBanner();
        operator = UserHelper.getUser(clientInfo.getClient());

        request = new GdFindAndReplaceCallouts()
                .withAdIds(List.of(banner.getId()))
                .withAction(GdFindAndReplaceCalloutsAction.ADD)
                .withReplaceInstruction(new GdFindAndReplaceCalloutsInstruction()
                        .withSearch(List.of())
                        .withReplace(List.of(secondCallout.getId()))
                );
    }

    @Test
    public void testPreview() {
        var query = String.format(PREVIEW_QUERY_TEMPLATE,
                FIND_AND_REPLACE_CALLOUTS_PREVIEW_QUERY,
                graphQlSerialize(request));

        var result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();

        var expectedPayload = new GdCachedResult<GdFindAndReplaceCalloutsPayloadItem>()
                .withTotalCount(request.getAdIds().size())
                .withRowset(List.of(expectedItem()));

        Map<String, Object> data = result.getData();
        assertThat(data).containsOnlyKeys(FIND_AND_REPLACE_CALLOUTS_PREVIEW_QUERY);

        var typeReference = new TypeReference<GdCachedResult<GdFindAndReplaceCalloutsPayloadItem>>() {
        };
        var payload = GraphQlJsonUtils.convertValue(data.get(FIND_AND_REPLACE_CALLOUTS_PREVIEW_QUERY), typeReference);

        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void testReplace_success() {
        TestAuthHelper.setDirectAuthentication(operator);

        var query = String.format(FIND_AND_REPLACE_MUTATION_TEMPLATE,
                FIND_AND_REPLACE_CALLOUTS_MUTATION,
                graphQlSerialize(request));

        var result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();

        var expectedPayload = new GdResult<GdFindAndReplaceCalloutsPayloadItem>()
                .withTotalCount(request.getAdIds().size())
                .withSuccessCount(request.getAdIds().size())
                .withRowset(List.of(expectedItem()))
                .withValidationResult(emptyValidationResult());

        Map<String, Object> data = result.getData();
        assertThat(data).containsOnlyKeys(FIND_AND_REPLACE_CALLOUTS_MUTATION);

        var typeReference = new TypeReference<GdCachedResult<GdFindAndReplaceCalloutsPayloadItem>>() {
        };
        var payload = GraphQlJsonUtils.convertValue(data.get(FIND_AND_REPLACE_CALLOUTS_MUTATION), typeReference);

        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void testReplace_whenBannerArchived_failure() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveDynamicCampaign(clientInfo);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveDynamicTextAdGroup(campaignInfo);
        OldDynamicBanner banner = activeDynamicBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                .withCalloutIds(List.of(firstCallout.getId()));
        DynamicBannerInfo bannerInfo = steps.bannerSteps().createActiveDynamicBanner(banner, adGroupInfo);
        testBannerRepository.updateStatusArchive(bannerInfo.getShard(), bannerInfo.getBannerId(), BannersStatusarch.Yes);
        bannerInfo.getBanner().withStatusArchived(true);
        DynamicBanner startBanner =
                (DynamicBanner) bannerService.getBannersByIds(singleton(bannerInfo.getBannerId())).get(0);

        request = new GdFindAndReplaceCallouts()
                .withAdIds(List.of(bannerInfo.getBannerId()))
                .withAction(GdFindAndReplaceCalloutsAction.ADD)
                .withReplaceInstruction(new GdFindAndReplaceCalloutsInstruction()
                        .withSearch(List.of())
                        .withReplace(List.of(secondCallout.getId()))
                );
        TestAuthHelper.setDirectAuthentication(operator);
        var query = String.format(FIND_AND_REPLACE_MUTATION_TEMPLATE,
                FIND_AND_REPLACE_CALLOUTS_MUTATION,
                graphQlSerialize(request));
        var result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();
        Map<String, Object> data = result.getData();
        assertThat(data).containsOnlyKeys(FIND_AND_REPLACE_CALLOUTS_MUTATION);

        var typeReference = new TypeReference<GdCachedResult<GdFindAndReplaceCalloutsPayloadItem>>() { };
        var payload = GraphQlJsonUtils.convertValue(data.get(FIND_AND_REPLACE_CALLOUTS_MUTATION), typeReference);
        var expectedPayload = new GdResult<GdFindAndReplaceCalloutsPayloadItem>()
                .withTotalCount(1)
                .withSuccessCount(0)
                .withRowset(List.of(new GdFindAndReplaceCalloutsPayloadItem()
                        .withAdId(bannerInfo.getBannerId())
                        .withOldCalloutIds(singletonList(firstCallout.getId()))
                        // Т.е. в rowset возвращается "как было бы", если бы банер поменялся. На это уже заложился
                        // фронт, например здась: https://st.yandex-team.ru/DIRECT-122434
                        .withNewCalloutIds(List.of(firstCallout.getId(), secondCallout.getId()))))
                .withValidationResult(toGdValidationResult(path(index(0)), cannotUpdateArchivedAd()));
        DynamicBanner actualBanner =
                (DynamicBanner) bannerService.getBannersByIds(singleton(bannerInfo.getBannerId())).get(0);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(payload).as("resultPayload").is(matchedBy(beanDiffer(expectedPayload)));
            soft.assertThat(actualBanner.getCalloutIds())
                    .as("CalloutIds").isEqualTo(startBanner.getCalloutIds());
        });
    }

    private GdFindAndReplaceCalloutsPayloadItem expectedItem() {
        var replace = request.getReplaceInstruction().getReplace();
        return new GdFindAndReplaceCalloutsPayloadItem()
                .withAdId(banner.getId())
                .withOldCalloutIds(banner.getCalloutIds())
                .withNewCalloutIds(List.of(banner.getCalloutIds().get(0), replace.get(0)));
    }
}
