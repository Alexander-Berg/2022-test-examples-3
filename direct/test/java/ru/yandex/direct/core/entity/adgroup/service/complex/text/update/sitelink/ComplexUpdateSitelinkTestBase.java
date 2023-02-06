package ru.yandex.direct.core.entity.adgroup.service.complex.text.update.sitelink;

import java.util.Collection;
import java.util.List;

import one.util.streamex.StreamEx;

import ru.yandex.direct.core.entity.adgroup.service.complex.model.ComplexTextBanner;
import ru.yandex.direct.core.entity.adgroup.service.complex.text.update.banner.ComplexUpdateBannerTestBase;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.sitelink.model.Sitelink;
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.SitelinkSetInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestSitelinkSets.defaultSitelinkSet;
import static ru.yandex.direct.core.testing.data.TestSitelinkSets.defaultSitelinkSet2;

public class ComplexUpdateSitelinkTestBase extends ComplexUpdateBannerTestBase {

    protected TextBannerInfo createRandomTitleBanner(AdGroupInfo adGroupInfo, SitelinkSet sitelinkSet) {
        SitelinkSetInfo sitelinkSetInfo =
                steps.sitelinkSetSteps().createSitelinkSet(sitelinkSet, campaignInfo.getClientInfo());
        OldTextBanner randomTitleBanner = activeTextBanner()
                .withTitle(randomAlphabetic(10))
                .withSitelinksSetId(sitelinkSetInfo.getSitelinkSetId());
        return steps.bannerSteps().createBanner(randomTitleBanner, adGroupInfo);
    }

    protected SitelinkSetInfo createRandomDescriptionSitelinkSet() {
        SitelinkSet sitelinkSet = defaultSitelinkSet(clientId);
        sitelinkSet.getSitelinks().forEach(sl -> sl.setDescription(randomAlphabetic(10)));
        return steps.sitelinkSetSteps().createSitelinkSet(sitelinkSet, campaignInfo.getClientInfo());
    }

    protected SitelinkSet randomDescriptionSitelinkSet() {
        SitelinkSet sitelinkSet = defaultSitelinkSet2(clientId);
        sitelinkSet.getSitelinks().forEach(sl -> sl.setDescription(randomAlphabetic(10)));
        return sitelinkSet;
    }

    protected ComplexTextBanner bannerWithRandomDescriptionSitelinks() {
        return bannerWithRandomDescriptionSitelinks(null);
    }

    protected ComplexTextBanner bannerWithRandomDescriptionSitelinks(TextBannerInfo bannerInfo) {
        ComplexTextBanner complexBanner = randomTitleTextComplexBanner(bannerInfo)
                .withSitelinkSet(randomDescriptionSitelinkSet());
        complexBanner.getBanner().withHref("https://www.yandex.ru");
        complexBanner.getSitelinkSet().getSitelinks().get(0).withHref("https://www.yandex.ru/company");
        complexBanner.getSitelinkSet().getSitelinks().get(1).withHref("https://www.yandex.ru/research");
        complexBanner.getSitelinkSet().getSitelinks().get(2).withHref("https://www.yandex.ru/news");
        return complexBanner;
    }

    protected List<String> extractDescriptions(List<SitelinkSet> sitelinkSets) {
        return StreamEx.of(sitelinkSets)
                .flatCollection(SitelinkSet::getSitelinks)
                .map(Sitelink::getDescription)
                .toList();
    }

    protected List<String> extractDescriptions(SitelinkSet sitelinkSet) {
        return StreamEx.of(sitelinkSet.getSitelinks())
                .nonNull()
                .map(Sitelink::getDescription)
                .toList();
    }

    protected List<SitelinkSet> findClientSitelinkSets() {
        return testSitelinkSetRepository.getSitelinkSetsByClientId(shard, clientId);
    }

    protected List<String> findClientSitelinkDescriptions() {
        return extractDescriptions(findClientSitelinkSets());
    }

    protected List<SitelinkSet> findAddedSitelinkSets(Collection<Long> oldSitelinkSetIds) {
        List<SitelinkSet> allClientSitelinkSets = testSitelinkSetRepository.getSitelinkSetsByClientId(shard, clientId);
        return StreamEx.of(allClientSitelinkSets)
                .remove(sitelinkSet -> oldSitelinkSetIds.contains(sitelinkSet.getId()))
                .toList();
    }
}
