package ru.yandex.direct.grid.processing.service.group.type;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.model.campaign.GdCampaignType;
import ru.yandex.direct.grid.model.entity.adgroup.GdAdGroupType;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.banner.GdAdType;
import ru.yandex.direct.grid.processing.model.group.GdAdGroup;
import ru.yandex.direct.grid.processing.model.group.GdContentPromotionAdGroup;
import ru.yandex.direct.grid.processing.model.group.GdCpmGeoproductAdGroup;
import ru.yandex.direct.grid.processing.model.group.GdCpmPriceAdGroup;
import ru.yandex.direct.grid.processing.model.group.GdCpmYndxFrontpageAdGroup;
import ru.yandex.direct.grid.processing.model.group.GdDynamicAdGroup;
import ru.yandex.direct.grid.processing.model.group.GdMobileContentAdGroup;
import ru.yandex.direct.grid.processing.model.group.GdSmartAdGroup;
import ru.yandex.direct.grid.processing.model.group.GdTextAdGroup;
import ru.yandex.direct.grid.processing.model.showcondition.GdShowConditionType;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType.COLLECTION;
import static ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType.VIDEO;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmGeoproductAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmYndxFrontpageAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDefaultAdGroupForPriceSales;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDynamicTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeMobileAppAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activePerformanceAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.core.testing.data.adgroup.TestContentPromotionAdGroups.fullContentPromotionAdGroup;
import static ru.yandex.direct.grid.model.entity.adgroup.AdGroupTypeConverter.toGdAdGroupType;

@GridProcessingTest
@RunWith(Parameterized.class)
public class GroupTypeSupportsTest {
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Autowired
    public GroupTypeFacade adGroupTypeFacade;

    @Autowired
    public AdGroupSteps adGroupSteps;

    @Autowired
    public Steps steps;

    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Parameterized.Parameter()
    public Function<Steps, AdGroup> adGroupProvider;

    @Parameterized.Parameter(1)
    public GdAdGroup gdAdGroup;

    @Parameterized.Parameter(2)
    public Set<GdShowConditionType> showConditionTypes;

    @Parameterized.Parameter(3)
    public Set<GdAdType> adTypes;

    @Parameterized.Parameter(4)
    public GdCampaignType gdCampaignType;

    @Parameterized.Parameter(5)
    public Set<FeatureName> featuresToEnable;

    private AdGroupInfo adGroupInfo;
    private AdGroup adGroup;

    @Parameterized.Parameters()
    public static Object[][] getParameters() {
        return new Object[][]{
                {
                        (Function<Steps, AdGroup>) (steps) -> activeTextAdGroup(null),
                        new GdTextAdGroup(),
                        ImmutableSet.of(GdShowConditionType.RELEVANCE_MATCH, GdShowConditionType.KEYWORD,
                                GdShowConditionType.RETARGETING, GdShowConditionType.MOBILE_INTERESTS),
                        ImmutableSet.of(GdAdType.IMAGE_AD, GdAdType.TEXT),
                        GdCampaignType.TEXT,
                        Collections.emptySet(),
                },
                {
                        (Function<Steps, AdGroup>) (steps) -> activeMobileAppAdGroup(null),
                        new GdMobileContentAdGroup(),
                        ImmutableSet.of(GdShowConditionType.RELEVANCE_MATCH, GdShowConditionType.KEYWORD,
                                GdShowConditionType.RETARGETING, GdShowConditionType.MOBILE_INTERESTS),
                        ImmutableSet.of(GdAdType.MOBILE_CONTENT),
                        GdCampaignType.MOBILE_CONTENT,
                        Collections.emptySet(),
                },
                {
                        (Function<Steps, AdGroup>) (steps) -> activeDynamicTextAdGroup(null),
                        new GdDynamicAdGroup(),
                        ImmutableSet.of(GdShowConditionType.FILTERS, GdShowConditionType.DYNAMIC_TARGETING),
                        ImmutableSet.of(GdAdType.DYNAMIC),
                        GdCampaignType.DYNAMIC,
                        Collections.emptySet(),
                },
                {
                        (Function<Steps, AdGroup>) (steps) -> activeCpmGeoproductAdGroup(null),
                        new GdCpmGeoproductAdGroup(),
                        ImmutableSet.of(GdShowConditionType.USER_PROFILE),
                        ImmutableSet.of(GdAdType.CPM_BANNER),
                        // TODO: а для CpmGeoproduct используем TEXT кампанию или какую-то другую?
                        GdCampaignType.TEXT,
                        Collections.emptySet(),
                },
                {
                        (Function<Steps, AdGroup>) (steps) ->
                        {
                            Long feedId = steps.feedSteps().createDefaultFeed().getFeedId();
                            return activePerformanceAdGroup(null, feedId);
                        },
                        new GdSmartAdGroup(),
                        ImmutableSet.of(GdShowConditionType.FILTERS),
                        ImmutableSet.of(GdAdType.PERFORMANCE),
                        GdCampaignType.PERFORMANCE,
                        Collections.emptySet(),
                },
                {
                        (Function<Steps, AdGroup>) (steps) ->
                                fullContentPromotionAdGroup(VIDEO),
                        new GdContentPromotionAdGroup(),
                        ImmutableSet.of(GdShowConditionType.RELEVANCE_MATCH, GdShowConditionType.KEYWORD,
                                GdShowConditionType.RETARGETING, GdShowConditionType.MOBILE_INTERESTS),
                        ImmutableSet.of(GdAdType.CONTENT_PROMOTION),
                        GdCampaignType.CONTENT_PROMOTION,
                        Collections.emptySet(),
                },
                {
                        (Function<Steps, AdGroup>) (steps) ->
                                fullContentPromotionAdGroup(COLLECTION),
                        new GdContentPromotionAdGroup(),
                        ImmutableSet.of(GdShowConditionType.RELEVANCE_MATCH, GdShowConditionType.KEYWORD,
                                GdShowConditionType.RETARGETING, GdShowConditionType.MOBILE_INTERESTS),
                        ImmutableSet.of(GdAdType.CONTENT_PROMOTION),
                        GdCampaignType.CONTENT_PROMOTION,
                        Collections.emptySet(),
                },
                {
                        (Function<Steps, AdGroup>) (steps) ->
                                activeCpmYndxFrontpageAdGroup(null),
                        new GdCpmYndxFrontpageAdGroup(),
                        ImmutableSet.of(GdShowConditionType.USER_PROFILE),
                        ImmutableSet.of(GdAdType.CPM_YNDX_FRONTPAGE),
                        GdCampaignType.CPM_YNDX_FRONTPAGE,
                        Set.of(FeatureName.CPM_YNDX_FRONTPAGE_PROFILE),
                },
                {
                        (Function<Steps, AdGroup>) (steps) ->
                                activeCpmYndxFrontpageAdGroup(null),
                        new GdCpmYndxFrontpageAdGroup(),
                        Collections.emptySet(),
                        ImmutableSet.of(GdAdType.CPM_YNDX_FRONTPAGE),
                        GdCampaignType.CPM_YNDX_FRONTPAGE,
                        Collections.emptySet(),
                },
                {
                        (Function<Steps, AdGroup>) (steps) -> activeDefaultAdGroupForPriceSales(null),
                        new GdCpmPriceAdGroup(),
                        ImmutableSet.of(GdShowConditionType.USER_PROFILE),
                        ImmutableSet.of(GdAdType.CPM_PRICE),
                        GdCampaignType.CPM_PRICE,
                        Collections.emptySet(),
                }
        };
    }

    @Before
    public void before() {
        adGroup = adGroupProvider.apply(steps);
        adGroupInfo = adGroupSteps.createAdGroup(adGroup);
        featuresToEnable.forEach(featureName -> steps.featureSteps()
                .addClientFeature(adGroupInfo.getClientId(), featureName, true));
    }

    @Test
    public void setAvailableBannerAndShowConditionTypes() {
        GdAdGroupType gdAdGroupType = toGdAdGroupType(adGroup.getType(), gdCampaignType);
        gdAdGroup.withId(adGroup.getId())
                .withType(gdAdGroupType);
        adGroupTypeFacade.setAvailableEntityTypes(adGroupInfo.getShard(), adGroupInfo.getClientId(),
                adGroupInfo.getUid(), singleton(gdAdGroup));

        assertThat(gdAdGroup.getAvailableShowConditionTypes())
                .containsOnly(showConditionTypes.toArray(new GdShowConditionType[0]));
        assertThat(gdAdGroup.getAvailableAdTypes()).containsOnly(adTypes.toArray(new GdAdType[0]));
    }
}
