package ru.yandex.direct.core.entity.banner.type.language;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.MockitoRule;

import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdGroup;
import ru.yandex.direct.core.entity.banner.container.BannersAddOperationContainerImpl;
import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.entity.banner.model.Language;
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode;
import ru.yandex.direct.core.entity.banner.service.text.BannerTextExtractor;
import ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefectIds;
import ru.yandex.direct.core.entity.campaign.model.ContentLanguage;
import ru.yandex.direct.core.entity.campaign.model.ContentPromotionCampaign;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.queryrec.QueryrecService;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.regions.Region.MOSCOW_REGION_ID;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(MockitoJUnitRunner.class)
public class BannerWithLanguageValidatorProviderTest {
    private static final int SHARD = 1;
    private static final Long ADGROUP_ID = 1L;
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private QueryrecService queryrecService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private GeoTreeFactory geoTreeFactory;

    private BannerWithLanguageValidatorProvider validatorProvider;

    private ClientId clientId;
    private Long uid;
    private RbacRole operatorRole;
    private BannersAddOperationContainerImpl container;

    @Before
    public void setUp() {
        clientId = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
        uid = RandomNumberUtils.nextPositiveLong();
        operatorRole = RbacRole.CLIENT;
        container = new BannersAddOperationContainerImpl(SHARD, uid, operatorRole, clientId, null, null,
                null, emptySet(), ModerationMode.FORCE_MODERATE, false, false, false);
        validatorProvider = new BannerWithLanguageValidatorProvider(queryrecService, geoTreeFactory,
                new BannerTextExtractor());
    }

    @Test
    public void validate_Successfully() {
        long campaignId = 1L;
        var adGroup = new ContentPromotionAdGroup().withId(ADGROUP_ID).withCampaignId(campaignId);
        container.setIndexToAdGroupMap(Map.of(0, adGroup));
        container.setCampaignIdToCampaignWithContentLanguageMap(Map.of(campaignId, new ContentPromotionCampaign()));
        var validator = validatorProvider.bannerWithLanguageValidator(container);

        var vr = validator.apply(List.of(createBanner()));
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_SuccessfullyTwoBanners() {
        long campaignId = 1L;
        var adGroup = new ContentPromotionAdGroup().withId(ADGROUP_ID).withCampaignId(campaignId);
        container.setIndexToAdGroupMap(Map.of(0, adGroup));
        container.setCampaignIdToCampaignWithContentLanguageMap(Map.of(campaignId,
                new ContentPromotionCampaign().withContentLanguage(ContentLanguage.EN)));
        var validator = validatorProvider.bannerWithLanguageValidator(container);
        var vr = validator.apply(List.of(createBanner(), createBanner()));
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_wrongCampaignLanguage_InconsistentLanguageWithGeo() {
        long campaignId = 1L;
        long russiaRegionId = 225L;
        var adGroup = new ContentPromotionAdGroup().withId(ADGROUP_ID).withCampaignId(campaignId)
                .withGeo(List.of(russiaRegionId));
        container.setIndexToAdGroupMap(Map.of(0, adGroup));
        container.setCampaignIdToCampaignWithContentLanguageMap(Map.of(campaignId,
                new ContentPromotionCampaign().withContentLanguage(ContentLanguage.TR)));
        var validator = validatorProvider.bannerWithLanguageValidator(container);
        var vr = validator.apply(List.of(createBanner()));
        assertThat(vr, hasDefectWithDefinition(validationError(path(index(0)),
                BannerDefectIds.LanguageDefect.INCONSISTENT_LANGUAGE_WITH_GEO)));
    }

    @Test
    public void validate_wrongAdGroupGeo_InconsistentLanguageWithGeo() {
        var campaignId = 1L;
        var adGroup = new ContentPromotionAdGroup()
                .withId(ADGROUP_ID)
                .withGeo(List.of(MOSCOW_REGION_ID))
                .withCampaignId(campaignId);
        container.setIndexToAdGroupMap(Map.of(0, adGroup));
        container.setCampaignIdToCampaignWithContentLanguageMap(Map.of(campaignId,
                new ContentPromotionCampaign().withContentLanguage(null)));
        var validator = validatorProvider.bannerWithLanguageValidator(container);
        var vr = validator.apply(List.of(createBanner()
                .withLanguage(Language.UK)));
        assertThat(vr, hasDefectWithDefinition(validationError(path(index(0)),
                BannerDefectIds.LanguageDefect.INCONSISTENT_LANGUAGE_WITH_GEO)));
    }

    @Test
    public void validate_SuccessfullyAsPartOfComplexOperation() {
        var adGroup = new ContentPromotionAdGroup().withId(ADGROUP_ID);
        container.setIndexToAdGroupMap(Map.of(0, adGroup));
        var validator = validatorProvider.bannerWithLanguageValidator(new BannersAddOperationContainerImpl(SHARD, uid,
                operatorRole, clientId, null, null, null, emptySet(), ModerationMode.FORCE_MODERATE, false, true, false));
        var vr = validator.apply(List.of(createBanner()));
        assertThat(vr, hasNoDefectsDefinitions());
    }

    private ContentPromotionBanner createBanner() {
        return new ContentPromotionBanner()
                .withAdGroupId(ADGROUP_ID)
                .withTitle("title")
                .withBody("body")
                .withLanguage(Language.EN);
    }
}
