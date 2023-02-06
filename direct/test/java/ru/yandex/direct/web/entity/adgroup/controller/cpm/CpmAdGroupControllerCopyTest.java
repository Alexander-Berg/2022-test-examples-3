package ru.yandex.direct.web.entity.adgroup.controller.cpm;

import java.math.BigDecimal;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmIndoorAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmOutdoorAdGroup;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerType;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.placements.model1.IndoorPlacement;
import ru.yandex.direct.core.entity.placements.model1.OutdoorPlacement;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CpmBannerInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.entity.adgroup.model.WebCpmAdGroup;
import ru.yandex.direct.web.entity.adgroup.model.WebCpmAdGroupRetargeting;
import ru.yandex.direct.web.entity.adgroup.model.WebPageBlock;
import ru.yandex.direct.web.entity.banner.model.WebCpmBanner;
import ru.yandex.direct.web.entity.banner.model.WebPixel;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.core.testing.data.TestKeywords.keywordForCpmBanner;
import static ru.yandex.direct.core.testing.data.TestPlacements.indoorBlockWithOneSize;
import static ru.yandex.direct.core.testing.data.TestPlacements.indoorPlacementWithBlock;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorBlockWithOneSize;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorPlacementWithBlock;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.web.entity.adgroup.converter.AdGroupConverterUtils.convertPageBlocks;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebCpmBannerAdGroup;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebCpmIndoorAdGroup;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebCpmOutdoorAdGroup;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebCpmVideoAdGroup;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebCpmYndxFrontpageAdGroup;
import static ru.yandex.direct.web.testing.data.TestBanners.webCpmBanner;
import static ru.yandex.direct.web.testing.data.TestKeywords.randomPhraseKeyword;

@DirectWebTest
@RunWith(SpringRunner.class)
public class CpmAdGroupControllerCopyTest extends CpmAdGroupControllerTestBase {

    @Test
    public void retargetingConditionIdChanged() {
        AdGroupInfo adGroupForCopy = steps.adGroupSteps().createActiveCpmBannerAdGroup(campaignInfo);
        WebCpmAdGroupRetargeting retargetingForCopy = createRetargeting(adGroupForCopy);

        WebCpmAdGroup complexCpmAdGroup =
                randomNameWebCpmVideoAdGroup(adGroupForCopy.getAdGroupId(), campaignInfo.getCampaignId())
                        .withRetargetings(singletonList(retargetingForCopy));

        WebResponse webResponse = controller
                .saveCpmAdGroup(singletonList(complexCpmAdGroup), campaignInfo.getCampaignId(), false, true, true,
                        null);
        checkResponse(webResponse);

        List<AdGroup> adGroups = findAdGroups();
        assertThat("количество групп не соответствует ожидаемому", adGroups, hasSize(2));
        Long copiedAdGroupId = adGroups.get(0).getId().equals(adGroupForCopy.getAdGroupId())
                ? adGroups.get(1).getId()
                : adGroups.get(0).getId();
        List<Retargeting> copiedRetargetings = findRetargetings(copiedAdGroupId);
        assertThat("количество ретаргетингов не соответствует ожидаемому", copiedRetargetings, hasSize(1));
        List<Retargeting> oldRetargetings = findRetargetings(adGroupForCopy.getAdGroupId());
        assertThat("id условия ретаргетинга должно измениться", copiedRetargetings.get(0).getRetargetingConditionId(),
                not(oldRetargetings.get(0).getRetargetingConditionId()));
    }

    @Test
    public void copyAdGroupWithRetargetingsManualStrategy_PricesCopied() {
        AdGroupInfo adGroupForCopy = steps.adGroupSteps().createActiveCpmBannerAdGroup(campaignInfo);
        WebCpmAdGroupRetargeting retargetingForCopy = createRetargeting(adGroupForCopy);

        WebCpmAdGroup complexCpmAdGroup =
                randomNameWebCpmVideoAdGroup(adGroupForCopy.getAdGroupId(), campaignInfo.getCampaignId())
                        .withRetargetings(singletonList(retargetingForCopy));

        WebResponse webResponse = controller
                .saveCpmAdGroup(singletonList(complexCpmAdGroup), campaignInfo.getCampaignId(), false, true, true,
                        null);
        checkResponse(webResponse);

        List<AdGroup> adGroups = findAdGroups();
        assertThat("количество групп не соответствует ожидаемому", adGroups, hasSize(2));
        Long copiedAdGroupId = adGroups.get(0).getId().equals(adGroupForCopy.getAdGroupId())
                ? adGroups.get(1).getId()
                : adGroups.get(0).getId();

        Retargeting expectedRetargeting = new Retargeting()
                .withPriceContext(BigDecimal.valueOf(retargetingForCopy.getPriceContext()));
        checkRetargetings(expectedRetargeting, copiedAdGroupId);
    }

    @Test
    public void copyAdGroupWithKeywordsManualStrategy_PricesCopied() {
        AdGroupInfo adGroupForCopy = steps.adGroupSteps().createActiveCpmBannerAdGroup(campaignInfo);
        BigDecimal priceContext = BigDecimal.valueOf(100);
        KeywordInfo keyword = steps.keywordSteps()
                .createKeyword(adGroupForCopy, keywordForCpmBanner().withPrice(null).withPriceContext(priceContext));

        WebCpmAdGroup complexCpmAdGroup =
                randomNameWebCpmBannerAdGroup(adGroupForCopy.getAdGroupId(), campaignInfo.getCampaignId())
                        .withKeywords(singletonList(randomPhraseKeyword(keyword.getId())))
                        .withGeneralPrice(200.0);

        WebResponse webResponse = controller
                .saveCpmAdGroup(singletonList(complexCpmAdGroup), campaignInfo.getCampaignId(), false, true, true,
                        null);
        checkResponse(webResponse);

        List<AdGroup> adGroups = findAdGroups();
        assertThat("количество групп не соответствует ожидаемому", adGroups, hasSize(2));
        Long copiedAdGroupId = adGroups.get(0).getId().equals(adGroupForCopy.getAdGroupId())
                ? adGroups.get(1).getId()
                : adGroups.get(0).getId();

        Keyword expectedKeyword = keyword.getKeyword()
                .withPriceContext(BigDecimal.valueOf(200.0));
        checkKeywords(expectedKeyword, copiedAdGroupId);
    }

    @Test
    public void cpmOutdoorAdGroupCopied() {
        steps.placementSteps().clearPlacements();
        final long pageId = 29342L;
        final long blockId = 123L;
        OutdoorPlacement placement = outdoorPlacementWithBlock(pageId, outdoorBlockWithOneSize(pageId, blockId));
        steps.placementSteps().addPlacement(placement);
        WebPageBlock newPageBlock = new WebPageBlock()
                .withPageId(placement.getId())
                .withImpId(placement.getBlocks().get(0).getBlockId());

        AdGroupInfo adGroupForCopy = steps.adGroupSteps().createActiveCpmOutdoorAdGroup(campaignInfo);
        WebCpmAdGroupRetargeting retargetingForCopy = createRetargeting(adGroupForCopy);

        WebCpmAdGroup complexCpmAdGroup = randomNameWebCpmOutdoorAdGroup(adGroupForCopy.getAdGroupId(),
                campaignInfo.getCampaignId(), newPageBlock)
                .withRetargetings(singletonList(retargetingForCopy));

        WebResponse webResponse = controller
                .saveCpmAdGroup(singletonList(complexCpmAdGroup), campaignInfo.getCampaignId(), false, true, true,
                        null);
        checkResponse(webResponse);

        List<AdGroup> adGroups = findAdGroups();
        assertThat("количество групп не соответствует ожидаемому", adGroups, hasSize(2));
        AdGroup copiedAdGroup = adGroups.get(0).getId().equals(adGroupForCopy.getAdGroupId())
                ? adGroups.get(1)
                : adGroups.get(0);

        AdGroup expectedAdGroup = new CpmOutdoorAdGroup()
                .withName(complexCpmAdGroup.getName())
                .withPageBlocks(convertPageBlocks(complexCpmAdGroup.getPageBlocks()));

        assertThat(copiedAdGroup, beanDiffer(expectedAdGroup).useCompareStrategy(onlyExpectedFields()));

        Retargeting expectedRetargeting = new Retargeting()
                .withPriceContext(BigDecimal.valueOf(retargetingForCopy.getPriceContext()));
        checkRetargetings(expectedRetargeting, copiedAdGroup.getId());
    }

    @Test
    public void cpmIndoorAdGroupCopied() {
        steps.placementSteps().clearPlacements();
        final long pageId = 29342L;
        final long blockId = 123L;
        IndoorPlacement placement = indoorPlacementWithBlock(pageId, indoorBlockWithOneSize(pageId, blockId));
        steps.placementSteps().addPlacement(placement);
        WebPageBlock newPageBlock = new WebPageBlock()
                .withPageId(placement.getId())
                .withImpId(placement.getBlocks().get(0).getBlockId());

        AdGroupInfo adGroupForCopy = steps.adGroupSteps().createActiveCpmIndoorAdGroup(campaignInfo);
        WebCpmAdGroupRetargeting retargetingForCopy = createIndoorRetargeting(adGroupForCopy);

        WebCpmAdGroup complexCpmAdGroup = randomNameWebCpmIndoorAdGroup(adGroupForCopy.getAdGroupId(),
                campaignInfo.getCampaignId(), newPageBlock)
                .withRetargetings(singletonList(retargetingForCopy));

        WebResponse webResponse = controller
                .saveCpmAdGroup(singletonList(complexCpmAdGroup), campaignInfo.getCampaignId(), false, true, true,
                        null);
        checkResponse(webResponse);

        List<AdGroup> adGroups = findAdGroups();
        assertThat("количество групп не соответствует ожидаемому", adGroups, hasSize(2));
        AdGroup copiedAdGroup = adGroups.get(0).getId().equals(adGroupForCopy.getAdGroupId())
                ? adGroups.get(1)
                : adGroups.get(0);

        AdGroup expectedAdGroup = new CpmIndoorAdGroup()
                .withName(complexCpmAdGroup.getName())
                .withPageBlocks(convertPageBlocks(complexCpmAdGroup.getPageBlocks()));

        assertThat(copiedAdGroup, beanDiffer(expectedAdGroup).useCompareStrategy(onlyExpectedFields()));

        Retargeting expectedRetargeting = new Retargeting()
                .withPriceContext(BigDecimal.valueOf(retargetingForCopy.getPriceContext()));
        checkRetargetings(expectedRetargeting, copiedAdGroup.getId());
    }

    // cpm_yndx_frontpage
    @Test
    public void cpmYndxFrontpageAdGroupCopied() {
        AdGroupInfo adGroupForCopy = steps.adGroupSteps().createDefaultCpmYndxFrontpageAdGroup(frontpageCampaignInfo);

        CreativeInfo creativeInfo = steps.creativeSteps().addDefaultHtml5CreativeForFrontpage(clientInfo);
        CpmBannerInfo cpmBanner = steps.bannerSteps().createActiveCpmBanner(
                activeCpmBanner(adGroupForCopy.getCampaignId(), adGroupForCopy.getAdGroupId(),
                        creativeInfo.getCreativeId()),
                adGroupForCopy);
        WebCpmBanner webBanner = webCpmBanner(cpmBanner.getBannerId(), creativeInfo.getCreativeId());
        WebCpmAdGroup complexCpmAdGroup = randomNameWebCpmYndxFrontpageAdGroup(adGroupForCopy.getAdGroupId(),
                frontpageCampaignInfo.getCampaignId())
                .withBanners(singletonList(webBanner));

        WebResponse webResponse = controller
                .saveCpmAdGroup(singletonList(complexCpmAdGroup), frontpageCampaignInfo.getCampaignId(), false, true,
                        true, null);
        checkResponse(webResponse);

        List<AdGroup> adGroups = findAdGroups(frontpageCampaignInfo.getCampaignId());
        assertThat("количество групп не соответствует ожидаемому", adGroups, hasSize(2));
        Long copiedAdGroupId = adGroups.get(0).getId().equals(adGroupForCopy.getAdGroupId())
                ? adGroups.get(1).getId()
                : adGroups.get(0).getId();

        OldCpmBanner expectedBanner = new OldCpmBanner()
                .withBannerType(OldBannerType.CPM_BANNER)
                .withPixels(mapList(webBanner.getPixels(), WebPixel::getUrl))
                .withCreativeId(Long.valueOf(webBanner.getCreative().getCreativeId()))
                .withHref(webBanner.getUrlProtocol() + webBanner.getHref());

        List<OldBanner> banners = findOldBanners(copiedAdGroupId);
        assertThat("в группе должен быть один баннер", banners, Matchers.hasSize(1));
        assertThat(banners.get(0),
                beanDiffer((OldBanner) expectedBanner).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }

    private void checkRetargetings(Retargeting expected, Long adGroupId) {
        List<Retargeting> retargetings = findRetargetings(adGroupId);

        DefaultCompareStrategy strategy = DefaultCompareStrategies.onlyFields(newPath("priceContext"))
                .forFields(newPath("priceContext")).useDiffer(new BigDecimalDiffer());
        assertThat("количество ретаргетингов не соответствует ожидаемому", retargetings, hasSize(1));
        assertThat("цена в ретаргетингах скопирована верно", retargetings.get(0),
                beanDiffer(expected).useCompareStrategy(strategy));
    }

    private void checkKeywords(Keyword expected, Long adGroupId) {
        List<Keyword> keywords = findKeywordsInAdGroup(adGroupId);

        DefaultCompareStrategy strategy = DefaultCompareStrategies.onlyFields(newPath("priceContext"))
                .forFields(newPath("priceContext")).useDiffer(new BigDecimalDiffer());
        assertThat("количество фраз не соответствует ожидаемому", keywords, hasSize(1));
        assertThat("цена во фразах скопирована верно", keywords.get(0),
                beanDiffer(expected).useCompareStrategy(strategy));
    }
}
