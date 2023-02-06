package ru.yandex.direct.core.entity.adgroup.service;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.DynamicFeedAdGroup;
import ru.yandex.direct.core.entity.feed.container.FeedQueryFilter;
import ru.yandex.direct.core.entity.feed.service.FeedService;
import ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseValidator;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestGroups;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.testing.matchers.validation.Matchers;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@CoreTest
@RunWith(SpringRunner.class)
public class AdGroupServiceUpdateDynamicFeedTest {
    private static final String[] COMPARE_PROPERTIES = new String[]{
            "Name", "Geo", "MinusKeywords", "TrackingParams", "FeedId"
    };

    public static final String[] DYNAMIC_COMPARE_PROPERTIES = new String[]{
            "FieldToUseAsName", "FieldToUseAsBody"
    };

    private static final String OLD_NAME = "old_name";
    private static final String NEW_NAME = "new_name";

    private static final List<Long> OLD_GEO = singletonList(Region.MOSCOW_REGION_ID);
    private static final List<Long> NEW_GEO = singletonList(Region.CRIMEA_REGION_ID);

    private static final List<String> OLD_MINUS_KEYWORDS = singletonList("abc");
    private static final List<String> NEW_MINUS_KEYWORDS = singletonList("efh");

    private static final String OLD_TRACKING_PARAMS = "a";
    private static final String NEW_TRACKING_PARAMS = "b";

    public static final String OLD_FIELD_TO_USE_AS_NAME = "old_field_name";
    public static final String NEW_FIELD_TO_USE_AS_NAME = "new_field_name";

    public static final String OLD_FIELD_TO_USE_AS_BODY = "old_field_body";
    public static final String NEW_FIELD_TO_USE_AS_BODY = "new_field_body";

    @Autowired
    public CampaignSteps campaignSteps;
    @Autowired
    public Steps steps;
    @Autowired
    public AdGroupSteps adGroupSteps;
    @Autowired
    public GeoTreeFactory geoTreeFactory;
    @Autowired
    public AdGroupService adGroupService;

    @Autowired
    public FeedService feedService;

    private GeoTree geoTree;
    private ClientInfo clientInfo;

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();
        geoTree = geoTreeFactory.getGlobalGeoTree();
    }

    @Test
    public void updateCommonFields() {
        CampaignInfo campaignInfo = campaignSteps.createActiveDynamicCampaign();
        FeedInfo feedInfo = steps.feedSteps().createDefaultFeed(clientInfo);
        AdGroupInfo adGroupInfo = adGroupSteps.createAdGroup(
                TestGroups.activeDynamicFeedAdGroup(campaignInfo.getCampaignId(), feedInfo.getFeedId())
                        .withName(OLD_NAME)
                        .withGeo(OLD_GEO)
                        .withMinusKeywords(OLD_MINUS_KEYWORDS)
                        .withTrackingParams(OLD_TRACKING_PARAMS), clientInfo);

        ModelChanges<AdGroup> changes = new ModelChanges<>(adGroupInfo.getAdGroupId(), DynamicFeedAdGroup.class)
                .process(NEW_NAME, AdGroup.NAME)
                .process(NEW_GEO, AdGroup.GEO)
                .process(NEW_MINUS_KEYWORDS, AdGroup.MINUS_KEYWORDS)
                .process(NEW_TRACKING_PARAMS, AdGroup.TRACKING_PARAMS)
                .castModelUp(AdGroup.class);

        AdGroup expected = changes.applyTo(adGroupInfo.getAdGroup()).getModel();

        AdGroup actual = updateAdGroup(clientInfo, changes);

        assertThat(actual)
                .isEqualToComparingOnlyGivenFields(expected, COMPARE_PROPERTIES);
    }

    @Test
    public void updateDynSmartFields() {
        CampaignInfo campaignInfo = campaignSteps.createActiveDynamicCampaign(clientInfo);
        FeedInfo feedInfo = steps.feedSteps().createDefaultFeed(clientInfo);
        AdGroupInfo adGroupInfo = adGroupSteps.createAdGroup(
                TestGroups.activeDynamicFeedAdGroup(campaignInfo.getCampaignId(), feedInfo.getFeedId())
                        .withName(OLD_NAME)
                        .withGeo(OLD_GEO)
                        .withMinusKeywords(OLD_MINUS_KEYWORDS)
                        .withTrackingParams(OLD_TRACKING_PARAMS)
                        .withFieldToUseAsName(OLD_FIELD_TO_USE_AS_NAME)
                        .withFieldToUseAsBody(OLD_FIELD_TO_USE_AS_BODY), clientInfo);

        FeedInfo newFeedInfo = steps.feedSteps().createDefaultFeed(clientInfo);

        ModelChanges<AdGroup> changes = new ModelChanges<>(adGroupInfo.getAdGroupId(), DynamicFeedAdGroup.class)
                .process(NEW_NAME, AdGroup.NAME)
                .process(NEW_GEO, AdGroup.GEO)
                .process(NEW_MINUS_KEYWORDS, AdGroup.MINUS_KEYWORDS)
                .process(NEW_TRACKING_PARAMS, AdGroup.TRACKING_PARAMS)
                .process(NEW_FIELD_TO_USE_AS_NAME, DynamicFeedAdGroup.FIELD_TO_USE_AS_NAME)
                .process(NEW_FIELD_TO_USE_AS_BODY, DynamicFeedAdGroup.FIELD_TO_USE_AS_BODY)
                .process(newFeedInfo.getFeedId(), DynamicFeedAdGroup.FEED_ID)
                .castModelUp(AdGroup.class);

        AdGroup expected = changes.applyTo(adGroupInfo.getAdGroup()).getModel();

        feedService.getFeedsSimple(clientInfo.getClientId(),
                FeedQueryFilter.newBuilder().withFeedIds(List.of(feedInfo.getFeedId(), newFeedInfo.getFeedId())).build());
        AdGroup actual = updateAdGroup(clientInfo, changes);

        assertThat(actual)
                .isEqualToComparingOnlyGivenFields(expected, DYNAMIC_COMPARE_PROPERTIES);
    }

    private AdGroup updateAdGroup(ClientInfo clientInfo, ModelChanges<AdGroup> changes) {
        MassResult<Long> result = adGroupService.updateAdGroupsPartialWithFullValidation(
                singletonList(changes),
                geoTree,
                MinusPhraseValidator.ValidationMode.ONE_ERROR_PER_TYPE,
                clientInfo.getUid(),
                clientInfo.getClientId());
        assertThat(result.getValidationResult()).is(matchedBy(Matchers.hasNoErrorsAndWarnings()));
        return adGroupService.getAdGroups(clientInfo.getClientId(), singletonList(result.get(0).getResult()))
                .get(0);
    }
}
