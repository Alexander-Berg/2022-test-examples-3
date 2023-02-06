package ru.yandex.direct.grid.core.entity.banner.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import one.util.streamex.StreamEx;
import org.apache.commons.beanutils.BeanUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerType;
import ru.yandex.direct.core.entity.banner.model.old.OldImageHashBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.sitelink.model.Sitelink;
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet;
import ru.yandex.direct.core.entity.sitelink.repository.SitelinkSetRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.BannerImageFormat;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.core.configuration.GridCoreTest;
import ru.yandex.direct.grid.core.entity.banner.model.GdiFindAndReplaceBannerHrefItem;
import ru.yandex.direct.grid.core.entity.banner.model.GdiFindAndReplaceBannerHrefItemSitelink;
import ru.yandex.direct.grid.core.entity.banner.service.internal.container.GridBannerUpdateInfo;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.invalidHref;
import static ru.yandex.direct.core.entity.sitelink.service.validation.SitelinkDefects.invalidSitelinkHref;
import static ru.yandex.direct.core.testing.data.TestBanners.activeImageCreativeBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeImageHashBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridCoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class GridFindAndReplaceBannerHrefServiceTest {
    private static final String NEW_HREF = "http://video.yandex.ru/?first=true";
    private static final String SECOND_NEW_HREF = "http://video.yandex.ru/?second=true";
    private static final String INVALID_HREF = "invalid href";

    @Autowired
    private OldBannerRepository bannerRepository;

    @Autowired
    private SitelinkSetRepository sitelinkSetRepository;

    @Autowired
    private Steps steps;

    @Autowired
    private GridFindAndReplaceBannerHrefService serviceUnderTest;

    private ClientInfo clientInfo;
    private CampaignInfo campaignInfo;
    private int shard;

    private OldTextBanner banner;
    private SitelinkSet sitelinkSet;

    @Before
    public void before() {
        campaignInfo = steps.campaignSteps().createDefaultCampaign();
        clientInfo = campaignInfo.getClientInfo();
        shard = clientInfo.getShard();
        sitelinkSet = steps.sitelinkSetSteps().createDefaultSitelinkSet(clientInfo).getSitelinkSet();
        banner = steps.bannerSteps()
                .createBanner(activeTextBanner(null, null).withSitelinksSetId(sitelinkSet.getId()),
                        campaignInfo).getBanner();
    }

    @Test
    public void updateBannersHrefAndSitelinksPreview_OneBannerWithSitelinks_NoErrors() {
        GridBannerUpdateInfo updateInfo = serviceUnderTest
                .updateBannersHrefAndSitelinksPreview(
                        singletonList(toFindAndReplaceBannerHrefItem(banner, sitelinkSet, NEW_HREF, NEW_HREF)),
                        clientInfo.getUid(),
                        clientInfo.getClientId());
        assertThat(updateInfo.getValidationResult(), hasNoDefectsDefinitions());
    }

    @Test
    public void updateBannersHrefAndSitelinksPreview_TwoBannerWithSitelinks_NoErrors() {
        SitelinkSet secondSitelinkSet = steps.sitelinkSetSteps().createDefaultSitelinkSet(clientInfo).getSitelinkSet();
        OldBanner secondBanner = steps.bannerSteps()
                .createBanner(activeTextBanner(null, null).withSitelinksSetId(secondSitelinkSet.getId()),
                        campaignInfo).getBanner();

        GridBannerUpdateInfo updateInfo = serviceUnderTest
                .updateBannersHrefAndSitelinksPreview(asList(
                        toFindAndReplaceBannerHrefItem(banner, sitelinkSet, NEW_HREF, NEW_HREF),
                        toFindAndReplaceBannerHrefItem(secondBanner, secondSitelinkSet, SECOND_NEW_HREF,
                                SECOND_NEW_HREF)),
                        clientInfo.getUid(),
                        clientInfo.getClientId());
        assertThat(updateInfo.getValidationResult(), hasNoDefectsDefinitions());
    }

    @Test
    public void updateBannersHrefAndSitelinksPreview_TwoBanners_OneError() {
        OldBanner secondBanner = steps.bannerSteps().createBanner(activeTextBanner(null, null), campaignInfo).getBanner();

        GridBannerUpdateInfo updateInfo = serviceUnderTest
                .updateBannersHrefAndSitelinksPreview(asList(
                        toFindAndReplaceBannerHrefItem(banner, null, NEW_HREF, null),
                        toFindAndReplaceBannerHrefItem(secondBanner, null, INVALID_HREF,
                                null)),
                        clientInfo.getUid(),
                        clientInfo.getClientId());
        assertThat(updateInfo.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(1), field(OldBanner.HREF)), invalidHref())));
    }

    @Test
    public void updateBannersHrefAndSitelinks_OneBannerWithoutSitelinks_ChangeHref() {
        OldBanner banner = steps.bannerSteps().createBanner(activeTextBanner(null, null), campaignInfo).getBanner();
        List<GdiFindAndReplaceBannerHrefItem> updateBanners =
                singletonList(toFindAndReplaceBannerHrefItem(banner, null, NEW_HREF, null));
        GridBannerUpdateInfo updateInfo = serviceUnderTest
                .updateBannersHrefAndSitelinks(updateBanners, clientInfo.getUid(), clientInfo.getClientId());
        assumeThat(updateInfo.getValidationResult(), hasNoDefectsDefinitions());

        assertThat("изменения не соответствуют ожиданию", getActualItems(banner.getId()), beanDiffer(updateBanners));
    }

    @Test
    public void updateBannersHrefAndSitelinks_OneImageHashBanner_ChangeHref() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(campaignInfo);
        BannerImageFormat bannerImageFormat = steps.bannerSteps().createImageAdImageFormat(clientInfo);
        OldImageHashBanner bannerData = activeImageHashBanner(campaignInfo.getCampaignId(), adGroupInfo.getAdGroupId());
        bannerData.getImage().setImageHash(bannerImageFormat.getImageHash());
        OldImageHashBanner banner = steps.bannerSteps().createActiveImageHashBanner(bannerData, adGroupInfo).getBanner();
        List<GdiFindAndReplaceBannerHrefItem> updateBanners =
                singletonList(
                        toFindAndReplaceBannerHrefItem(banner, null, NEW_HREF, null).withAdGroupType(AdGroupType.BASE)
                                .withImageHash(banner.getImage().getImageHash()));
        GridBannerUpdateInfo updateInfo = serviceUnderTest
                .updateBannersHrefAndSitelinks(updateBanners, clientInfo.getUid(), clientInfo.getClientId());
        assumeThat(updateInfo.getValidationResult(), hasNoDefectsDefinitions());

        assertThat("изменения не соответствуют ожиданию", getActualItems(banner.getId()),
                beanDiffer(updateBanners).useCompareStrategy(allFieldsExcept(
                        BeanFieldPath.newPath(".*", GdiFindAndReplaceBannerHrefItem.AD_GROUP_TYPE.name()),
                        BeanFieldPath.newPath(".*", GdiFindAndReplaceBannerHrefItem.IMAGE_HASH.name()))));
    }

    @Test
    public void updateBannersHrefAndSitelinks_OneImageCreativeBanner_ChangeHref() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(campaignInfo);
        CreativeInfo creativeInfo = steps.creativeSteps().addDefaultCanvasCreative(clientInfo);
        OldBanner banner = steps.bannerSteps().createActiveImageCreativeBanner(
                activeImageCreativeBanner(campaignInfo.getCampaignId(), adGroupInfo.getAdGroupId(),
                        creativeInfo.getCreativeId()), adGroupInfo).getBanner();
        List<GdiFindAndReplaceBannerHrefItem> updateBanners =
                singletonList(toFindAndReplaceBannerHrefItem(banner, null, NEW_HREF, null)
                        .withAdGroupType(AdGroupType.BASE));
        GridBannerUpdateInfo updateInfo = serviceUnderTest
                .updateBannersHrefAndSitelinks(updateBanners, clientInfo.getUid(), clientInfo.getClientId());
        assumeThat(updateInfo.getValidationResult(), hasNoDefectsDefinitions());

        assertThat("изменения не соответствуют ожиданию", getActualItems(banner.getId()),
                beanDiffer(updateBanners).useCompareStrategy(allFieldsExcept(
                        BeanFieldPath.newPath(".*", GdiFindAndReplaceBannerHrefItem.AD_GROUP_TYPE.name()))));
    }

    @Test
    public void updateBannersHrefAndSitelinks_OneBanner_ChangeHrefAndSitelinks_SitelinkHrefInvalid() {
        List<GdiFindAndReplaceBannerHrefItem> updateBanners =
                singletonList(toFindAndReplaceBannerHrefItem(banner, sitelinkSet, NEW_HREF, INVALID_HREF));
        GridBannerUpdateInfo updateInfo = serviceUnderTest
                .updateBannersHrefAndSitelinks(updateBanners, clientInfo.getUid(), clientInfo.getClientId());
        assumeThat(updateInfo.getValidationResult(),
                hasDefectDefinitionWith(validationError(
                        path(index(0), field("sitelinksSet"), field(SitelinkSet.SITELINKS), index(0),
                                field(Sitelink.HREF)),
                        invalidSitelinkHref())));

        assertThat("изменения не соответствуют ожиданию", getActualItems(banner.getId()),
                beanDiffer(singletonList(toFindAndReplaceBannerHrefItem(banner, sitelinkSet, NEW_HREF, null))));
    }

    @Test
    public void updateBannersHrefAndSitelinks_TwoBannersWithSitelinks_ChangeHrefAndSitelinks() {
        SitelinkSet secondSitelinkSet = steps.sitelinkSetSteps().createDefaultSitelinkSet(clientInfo).getSitelinkSet();
        OldBanner secondBanner = steps.bannerSteps()
                .createBanner(activeTextBanner(null, null).withSitelinksSetId(secondSitelinkSet.getId()),
                        campaignInfo).getBanner();
        List<GdiFindAndReplaceBannerHrefItem> updateBanners =
                asList(toFindAndReplaceBannerHrefItem(banner, sitelinkSet, NEW_HREF, NEW_HREF),
                        toFindAndReplaceBannerHrefItem(secondBanner, secondSitelinkSet, SECOND_NEW_HREF,
                                SECOND_NEW_HREF));

        GridBannerUpdateInfo updateInfo = serviceUnderTest
                .updateBannersHrefAndSitelinks(updateBanners, clientInfo.getUid(), clientInfo.getClientId());
        assumeThat(updateInfo.getValidationResult(), hasNoDefectsDefinitions());

        assertThat("изменения не соответствуют ожиданию", getActualItems(banner.getId(), secondBanner.getId()),
                beanDiffer(updateBanners));
    }

    @Test
    public void updateBannersHrefAndSitelinks_TwoBannersWithSitelinks_OneChangeInvalidHrefAndSitelinks() {
        SitelinkSet secondSitelinkSet = steps.sitelinkSetSteps().createDefaultSitelinkSet(clientInfo).getSitelinkSet();
        OldBanner secondBanner = steps.bannerSteps()
                .createBanner(activeTextBanner(null, null).withSitelinksSetId(secondSitelinkSet.getId()),
                        campaignInfo).getBanner();
        List<GdiFindAndReplaceBannerHrefItem> updateBanners =
                asList(toFindAndReplaceBannerHrefItem(banner, sitelinkSet, NEW_HREF, NEW_HREF),
                        toFindAndReplaceBannerHrefItem(secondBanner, secondSitelinkSet, INVALID_HREF, SECOND_NEW_HREF));

        GridBannerUpdateInfo updateInfo = serviceUnderTest
                .updateBannersHrefAndSitelinks(updateBanners, clientInfo.getUid(), clientInfo.getClientId());
        assumeThat(updateInfo.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(1), field(OldBanner.HREF)), invalidHref())));

        List<GdiFindAndReplaceBannerHrefItem> expectedUpdateBanners = asList(updateBanners.get(0),
                toFindAndReplaceBannerHrefItem(secondBanner, secondSitelinkSet, null, null));
        assertThat("изменения не соответствуют ожиданию", getActualItems(banner.getId(), secondBanner.getId()),
                beanDiffer(expectedUpdateBanners));
    }

    private List<GdiFindAndReplaceBannerHrefItem> getActualItems(Long... bannerIds) {
        List<OldBanner> banners = bannerRepository.getBanners(shard, asList(bannerIds));
        Set<Long> sitelinkSetIds = StreamEx.of(banners)
                .select(OldTextBanner.class)
                .map(OldTextBanner::getSitelinksSetId)
                .nonNull()
                .toSet();
        Map<Long, SitelinkSet> sitelinkSetsById = listToMap(sitelinkSetRepository.get(shard, sitelinkSetIds),
                SitelinkSet::getId);
        return StreamEx.of(banners)
                .map(b -> toFindAndReplaceBannerHrefItem(b,
                        b instanceof OldTextBanner ? sitelinkSetsById.get(((OldTextBanner) b).getSitelinksSetId()) : null))
                .toList();
    }

    private GdiFindAndReplaceBannerHrefItem toFindAndReplaceBannerHrefItem(OldBanner banner,
                                                                           @Nullable SitelinkSet sitelinkSet, @Nullable String newBannerHref, @Nullable String newSitelinkHref) {
        GdiFindAndReplaceBannerHrefItem item = toFindAndReplaceBannerHrefItem(banner, sitelinkSet);
        ifNotNull(newBannerHref, item::withNewHref);
        item.getSitelinks().forEach(sl -> ifNotNull(newSitelinkHref, slh -> sl.getSitelink().withHref(slh)));
        return item;
    }

    private GdiFindAndReplaceBannerHrefItem toFindAndReplaceBannerHrefItem(OldBanner banner, SitelinkSet sitelinkSet) {
        return new GdiFindAndReplaceBannerHrefItem()
                .withBannerId(banner.getId())
                .withBannerType(OldBannerType.toSource(banner.getBannerType()))
                .withNewHref(banner.getHref())
                .withSitelinks(fromSitelinkSet(sitelinkSet));
    }

    private List<GdiFindAndReplaceBannerHrefItemSitelink> fromSitelinkSet(SitelinkSet sitelinkSet) {
        if (sitelinkSet == null) {
            return emptyList();
        }
        return mapList(sitelinkSet.getSitelinks(),
                sl -> new GdiFindAndReplaceBannerHrefItemSitelink().withSitelink(copySitelink(sl)));
    }


    private static Sitelink copySitelink(Sitelink sitelink) {
        try {
            return (Sitelink) BeanUtils.cloneBean(sitelink);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
