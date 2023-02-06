package ru.yandex.direct.api.v5.entity.ads.delegate;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.ads.AdStateSelectionEnum;
import com.yandex.direct.api.v5.ads.AdStatusSelectionEnum;
import com.yandex.direct.api.v5.ads.AdTypeEnum;
import com.yandex.direct.api.v5.ads.GetRequest;
import com.yandex.direct.api.v5.general.ExtensionStatusSelectionEnum;
import com.yandex.direct.api.v5.general.YesNoEnum;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.direct.api.v5.entity.ads.converter.GetResponseConverter;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.core.entity.YesNo;
import ru.yandex.direct.core.entity.addition.callout.repository.CalloutRepository;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.banner.container.AdsSelectionCriteria;
import ru.yandex.direct.core.entity.banner.model.ExtensionStatus;
import ru.yandex.direct.core.entity.banner.model.State;
import ru.yandex.direct.core.entity.banner.model.Status;
import ru.yandex.direct.core.entity.banner.repository.BannerCommonRepository;
import ru.yandex.direct.core.entity.banner.service.BannerService;
import ru.yandex.direct.core.entity.banner.type.creative.BannerCreativeRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.creative.repository.CreativeRepository;
import ru.yandex.direct.core.entity.mobilecontent.repository.MobileContentRepository;
import ru.yandex.direct.core.entity.moderationreason.service.ModerationReasonService;
import ru.yandex.direct.dbschema.ppc.enums.BannersBannerType;
import ru.yandex.direct.dbutil.sharding.ShardHelper;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.mock;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFields;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class GetAdsDelegateExtractSelectionCriteriaTest {

    private static final List<Long> IDS = asList(1L, 2L, 3L);
    private static final List<Long> IDS_WITH_DUPS = asList(1L, 2L, 3L, 1L, 2L, 3L);
    private static final Set<Long> EXPECTED_IDS = new HashSet<>(IDS);

    private static final List<String> HASHES = asList("aaa", "bbb", "ccc");
    private static final List<String> HASHES_WITH_DUPS = asList("aaa", "bbb", "ccc", "aaa", "bbb", "ccc");
    private static final Set<String> EXPECTED_HASHES = new HashSet<>(HASHES);

    private static final List<AdTypeEnum> TYPES = asList(AdTypeEnum.values());
    private static final List<AdTypeEnum> TYPES_WITH_DUPS =
            Stream.of(TYPES, TYPES).flatMap(Collection::stream).collect(toList());
    private static final BannersBannerType[] BANNER_TYPES = {BannersBannerType.dynamic,
            BannersBannerType.image_ad,
            BannersBannerType.mobile_content,
            BannersBannerType.text,
            BannersBannerType.cpm_banner,
            BannersBannerType.cpc_video,
            BannersBannerType.content_promotion,
            BannersBannerType.performance,
            BannersBannerType.performance_main};
    private static final Set<BannersBannerType> EXPECTED_TYPES = new HashSet<>(asList(BANNER_TYPES));

    private static final List<AdStateSelectionEnum> STATES = asList(AdStateSelectionEnum.values());
    private static final List<AdStateSelectionEnum> STATES_WITH_DUPS =
            Stream.of(STATES, STATES).flatMap(Collection::stream).collect(toList());
    private static final State[] AD_STATES = State.values();
    private static final Set<State> EXPECTED_STATES = new HashSet<>(asList(AD_STATES));

    private static final List<AdStatusSelectionEnum> STATUSES = asList(AdStatusSelectionEnum.values());
    private static final List<AdStatusSelectionEnum> STATUSES_WITH_DUPS =
            Stream.of(STATUSES, STATUSES).flatMap(Collection::stream).collect(toList());
    private static final Status[] AD_STATUSES = Status.values();
    private static final Set<Status> EXPECTED_STATUSES = new HashSet<>(asList(AD_STATUSES));

    private static final List<ExtensionStatusSelectionEnum> EXTENSION_STATUSES =
            asList(ExtensionStatusSelectionEnum.values());
    private static final List<ExtensionStatusSelectionEnum> EXTENSION_STATUSES_WITH_DUPS =
            Stream.of(EXTENSION_STATUSES, EXTENSION_STATUSES).flatMap(Collection::stream).collect(toList());
    private static final ExtensionStatus[] INT_EXTENSION_STATUSES = {ExtensionStatus.ACCEPTED,
            ExtensionStatus.DRAFT,
            ExtensionStatus.MODERATION,
            ExtensionStatus.REJECTED};
    private static final Set<ExtensionStatus> EXPECTED_EXTENSION_STATUSES = new HashSet<>(
            asList(INT_EXTENSION_STATUSES));
    private static final ContentPromotionAdgroupType[] INT_CONTENT_PROMOTION_TYPES =
            new ContentPromotionAdgroupType[]{
                    ContentPromotionAdgroupType.VIDEO,
                    ContentPromotionAdgroupType.COLLECTION,
                    ContentPromotionAdgroupType.SERVICE,
                    ContentPromotionAdgroupType.EDA
            };
    private static final Set<ContentPromotionAdgroupType> EXPECTED_CONTENT_PROMOTION_TYPES = Set.of(
            ContentPromotionAdgroupType.VIDEO,
            ContentPromotionAdgroupType.COLLECTION,
            ContentPromotionAdgroupType.SERVICE,
            ContentPromotionAdgroupType.EDA
    );

    @Parameterized.Parameter
    public String description;

    @Parameterized.Parameter(1)
    public com.yandex.direct.api.v5.ads.AdsSelectionCriteria selectionCriteria;

    @Parameterized.Parameter(2)
    public AdsSelectionCriteria expectedSelectionCriteria;

    @Parameterized.Parameter(3)
    public DefaultCompareStrategy strategy;

    private GetAdsDelegate delegate;

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {"without all criterias", buildSelectionCriteria(), new AdsSelectionCriteria(), null},
                {"with ids", buildSelectionCriteria().withIds(IDS), new AdsSelectionCriteria().withAdIds(EXPECTED_IDS),
                        null},
                {"with ids with duplicate", buildSelectionCriteria().withIds(IDS_WITH_DUPS),
                        new AdsSelectionCriteria().withAdIds(EXPECTED_IDS), null},
                {"with adgroup ids", buildSelectionCriteria().withAdGroupIds(IDS),
                        new AdsSelectionCriteria().withAdGroupIds(EXPECTED_IDS), null},
                {"with adgroup ids with duplicate", buildSelectionCriteria().withAdGroupIds(IDS_WITH_DUPS),
                        new AdsSelectionCriteria().withAdGroupIds(EXPECTED_IDS), null},
                {"with campaign ids", buildSelectionCriteria().withCampaignIds(IDS),
                        new AdsSelectionCriteria().withCampaignIds(EXPECTED_IDS), null},
                {"with campaign ids with duplicate", buildSelectionCriteria().withCampaignIds(IDS_WITH_DUPS),
                        new AdsSelectionCriteria().withCampaignIds(EXPECTED_IDS), null},
                {"with vcard ids", buildSelectionCriteria().withVCardIds(IDS),
                        new AdsSelectionCriteria().withVCardIds(EXPECTED_IDS), null},
                {"with vcard ids with duplicate", buildSelectionCriteria().withVCardIds(IDS_WITH_DUPS),
                        new AdsSelectionCriteria().withVCardIds(EXPECTED_IDS), null},
                {"with sitelink set ids", buildSelectionCriteria().withSitelinkSetIds(IDS),
                        new AdsSelectionCriteria().withSitelinkSetIds(EXPECTED_IDS), null},
                {"with sitelink set ids with duplicate", buildSelectionCriteria().withSitelinkSetIds(IDS_WITH_DUPS),
                        new AdsSelectionCriteria().withSitelinkSetIds(EXPECTED_IDS), null},
                {"with ad extension ids", buildSelectionCriteria().withAdExtensionIds(IDS),
                        new AdsSelectionCriteria().withAdExtensionIds(EXPECTED_IDS), null},
                {"with ad extension ids with duplicate", buildSelectionCriteria().withAdExtensionIds(IDS_WITH_DUPS),
                        new AdsSelectionCriteria().withAdExtensionIds(EXPECTED_IDS), null},
                {"with ad image hashes", buildSelectionCriteria().withAdImageHashes(HASHES),
                        new AdsSelectionCriteria().withAdImageHashes(EXPECTED_HASHES), null},
                {"with ad image hashes with duplicate", buildSelectionCriteria().withAdImageHashes(HASHES_WITH_DUPS),
                        new AdsSelectionCriteria().withAdImageHashes(EXPECTED_HASHES), null},
                {"with types", buildSelectionCriteria().withTypes(TYPES),
                        new AdsSelectionCriteria().withContentPromotionAdgroupTypes(EXPECTED_CONTENT_PROMOTION_TYPES),
                        allFields().forFields(newPath("types")).useMatcher(containsInAnyOrder(BANNER_TYPES))
                                .forFields(newPath("contentPromotionAdgroupTypes")).useMatcher(
                                containsInAnyOrder(INT_CONTENT_PROMOTION_TYPES))},
                {"with types with duplicate", buildSelectionCriteria().withTypes(TYPES_WITH_DUPS),
                        new AdsSelectionCriteria().withContentPromotionAdgroupTypes(EXPECTED_CONTENT_PROMOTION_TYPES),
                        allFields().forFields(newPath("types")).useMatcher(containsInAnyOrder(BANNER_TYPES))
                                .forFields(newPath("contentPromotionAdgroupTypes")).useMatcher(
                                containsInAnyOrder(INT_CONTENT_PROMOTION_TYPES))},
                {"with states", buildSelectionCriteria().withStates(STATES),
                        new AdsSelectionCriteria().withStates(EXPECTED_STATES),
                        allFields().forFields(newPath("states")).useMatcher(containsInAnyOrder(AD_STATES))},
                {"with states with duplicate", buildSelectionCriteria().withStates(STATES_WITH_DUPS),
                        new AdsSelectionCriteria().withStates(EXPECTED_STATES),
                        allFields().forFields(newPath("states")).useMatcher(containsInAnyOrder(AD_STATES))},
                {"with statuses", buildSelectionCriteria().withStatuses(STATUSES),
                        new AdsSelectionCriteria().withStatuses(EXPECTED_STATUSES),
                        allFields().forFields(newPath("statuses")).useMatcher(containsInAnyOrder(AD_STATUSES))},
                {"with statuses with duplicate", buildSelectionCriteria().withStatuses(STATUSES_WITH_DUPS),
                        new AdsSelectionCriteria().withStatuses(EXPECTED_STATUSES),
                        allFields().forFields(newPath("statuses")).useMatcher(containsInAnyOrder(AD_STATUSES))},
                {"with vcard statuses",
                        buildSelectionCriteria().withVCardModerationStatuses(EXTENSION_STATUSES),
                        new AdsSelectionCriteria().withVCardStatuses(EXPECTED_EXTENSION_STATUSES),
                        allFields().forFields(newPath("VCardStatuses")).useMatcher(
                                containsInAnyOrder(INT_EXTENSION_STATUSES))},
                {"with vcard statuses with duplicate",
                        buildSelectionCriteria().withVCardModerationStatuses(EXTENSION_STATUSES_WITH_DUPS),
                        new AdsSelectionCriteria().withVCardStatuses(EXPECTED_EXTENSION_STATUSES),
                        allFields().forFields(newPath("VCardStatuses")).useMatcher(
                                containsInAnyOrder(INT_EXTENSION_STATUSES))},
                {"with sitelinks statuses",
                        buildSelectionCriteria().withSitelinksModerationStatuses(EXTENSION_STATUSES),
                        new AdsSelectionCriteria().withSitelinksStatuses(EXPECTED_EXTENSION_STATUSES),
                        allFields().forFields(newPath("sitelinksStatuses")).useMatcher(
                                containsInAnyOrder(INT_EXTENSION_STATUSES))},
                {"with sitelinks statuses with duplicate",
                        buildSelectionCriteria().withSitelinksModerationStatuses(EXTENSION_STATUSES_WITH_DUPS),
                        new AdsSelectionCriteria().withSitelinksStatuses(EXPECTED_EXTENSION_STATUSES),
                        allFields().forFields(newPath("sitelinksStatuses")).useMatcher(
                                containsInAnyOrder(INT_EXTENSION_STATUSES))},
                {"with ad image statuses",
                        buildSelectionCriteria().withAdImageModerationStatuses(EXTENSION_STATUSES),
                        new AdsSelectionCriteria().withAdImageStatuses(EXPECTED_EXTENSION_STATUSES),
                        allFields().forFields(newPath("adImageStatuses")).useMatcher(
                                containsInAnyOrder(INT_EXTENSION_STATUSES))},
                {"with ad image statuses with duplicate",
                        buildSelectionCriteria().withAdImageModerationStatuses(EXTENSION_STATUSES_WITH_DUPS),
                        new AdsSelectionCriteria().withAdImageStatuses(EXPECTED_EXTENSION_STATUSES),
                        allFields().forFields(newPath("adImageStatuses")).useMatcher(
                                containsInAnyOrder(INT_EXTENSION_STATUSES))},
                {"with mobile = yes",
                        buildSelectionCriteria().withMobile(YesNoEnum.YES),
                        new AdsSelectionCriteria().withMobile(YesNo.YES), null},
                {"with mobile = no",
                        buildSelectionCriteria().withMobile(YesNoEnum.NO),
                        new AdsSelectionCriteria().withMobile(YesNo.NO), null},
                {"with all criterias",
                        buildSelectionCriteria()
                                .withIds(IDS)
                                .withAdGroupIds(IDS)
                                .withCampaignIds(IDS)
                                .withVCardIds(IDS)
                                .withSitelinkSetIds(IDS)
                                .withAdExtensionIds(IDS)
                                .withAdImageHashes(HASHES)
                                .withTypes(TYPES)
                                .withStates(STATES)
                                .withStatuses(STATUSES)
                                .withVCardModerationStatuses(EXTENSION_STATUSES)
                                .withSitelinksModerationStatuses(EXTENSION_STATUSES)
                                .withAdImageModerationStatuses(EXTENSION_STATUSES)
                                .withMobile(YesNoEnum.YES),
                        new AdsSelectionCriteria()
                                .withAdIds(EXPECTED_IDS)
                                .withAdGroupIds(EXPECTED_IDS)
                                .withCampaignIds(EXPECTED_IDS)
                                .withVCardIds(EXPECTED_IDS)
                                .withSitelinkSetIds(EXPECTED_IDS)
                                .withAdExtensionIds(EXPECTED_IDS)
                                .withAdImageHashes(EXPECTED_HASHES)
                                .withTypes(EXPECTED_TYPES)
                                .withStates(EXPECTED_STATES)
                                .withStatuses(EXPECTED_STATUSES)
                                .withVCardStatuses(EXPECTED_EXTENSION_STATUSES)
                                .withSitelinksStatuses(EXPECTED_EXTENSION_STATUSES)
                                .withAdImageStatuses(EXPECTED_EXTENSION_STATUSES)
                                .withMobile(YesNo.YES)
                                .withContentPromotionAdgroupTypes(EXPECTED_CONTENT_PROMOTION_TYPES),
                        allFields()
                                .forFields(newPath("types")).useMatcher(containsInAnyOrder(BANNER_TYPES))
                                .forFields(newPath("states")).useMatcher(containsInAnyOrder(AD_STATES))
                                .forFields(newPath("statuses")).useMatcher(containsInAnyOrder(AD_STATUSES))
                                .forFields(newPath("VCardStatuses"))
                                .useMatcher(containsInAnyOrder(INT_EXTENSION_STATUSES))
                                .forFields(newPath("sitelinksStatuses"))
                                .useMatcher(containsInAnyOrder(INT_EXTENSION_STATUSES))
                                .forFields(newPath("adImageStatuses")).useMatcher(
                                        containsInAnyOrder(INT_EXTENSION_STATUSES))
                                .forFields(newPath("contentPromotionAdgroupTypes")).useMatcher(
                                containsInAnyOrder(INT_CONTENT_PROMOTION_TYPES))},
        };
    }

    private static com.yandex.direct.api.v5.ads.AdsSelectionCriteria buildSelectionCriteria() {
        return new com.yandex.direct.api.v5.ads.AdsSelectionCriteria();
    }

    @Before
    public void prepare() {
        delegate = new GetAdsDelegate(mock(ApiAuthenticationSource.class),
                mock(ShardHelper.class),
                mock(BannerService.class), mock(BannerCommonRepository.class), mock(CampaignService.class),
                mock(CalloutRepository.class), mock(BannerCreativeRepository.class), mock(CreativeRepository.class),
                mock(ModerationReasonService.class), mock(MobileContentRepository.class),
                mock(GetResponseConverter.class), mock(AdGroupService.class), mock(AdGroupRepository.class));
    }

    @Test
    public void test() {
        AdsSelectionCriteria selectionCriteria =
                delegate.extractSelectionCriteria(new GetRequest().withSelectionCriteria(this.selectionCriteria));

        BeanDifferMatcher<AdsSelectionCriteria> matcher = beanDiffer(expectedSelectionCriteria);

        if (strategy != null) {
            matcher.useCompareStrategy(strategy);
        }

        assertThat(selectionCriteria).is(matchedBy(matcher));
    }

}
