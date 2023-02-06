package ru.yandex.direct.grid.core.entity.banner.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import junitparams.converters.Nullable;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerType;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.sitelink.model.Sitelink;
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet;
import ru.yandex.direct.core.testing.data.TestSitelinkSets;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.core.entity.banner.model.GdiFindAndReplaceBannerHrefItem;
import ru.yandex.direct.grid.core.entity.banner.model.GdiFindAndReplaceBannerHrefItemSitelink;
import ru.yandex.direct.grid.model.findandreplace.ReplaceRule;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestSitelinks.defaultSitelink;
import static ru.yandex.direct.core.testing.data.TestSitelinks.defaultSitelink2;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

public class GridFindAndReplaceHrefTestBase {
    protected static final String HREF_NEED_REPLACE = "http://yandex.ru";
    protected static final String HREF_DO_NOT_NEED_REPLACE = "http://replace.net";

    @Autowired
    protected OldBannerRepository bannerRepository;
    @Autowired
    protected Steps steps;

    protected ReplaceRule replaceRule;

    protected ClientId clientId;
    protected ClientInfo clientInfo;

    @Before
    public void before() {
        replaceRule = href -> {
            if (!href.contains("yandex")) {
                return null;
            }
            return href.replace("yandex", "replace");
        };

        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
    }

    protected OldBanner createBannerWithReplace() {
        return steps.bannerSteps().createBanner(activeTextBanner().withHref(HREF_NEED_REPLACE), clientInfo).getBanner();
    }

    protected OldBanner createBannerWithoutReplace() {
        return steps.bannerSteps().createBanner(activeTextBanner().withHref(HREF_DO_NOT_NEED_REPLACE), clientInfo)
                .getBanner();
    }

    protected SitelinkSet createSitelinkSetWithoutReplace() {
        List<Sitelink> sitelinks = asList(defaultSitelink().withHref(HREF_DO_NOT_NEED_REPLACE),
                defaultSitelink2().withHref(HREF_DO_NOT_NEED_REPLACE));
        return steps.sitelinkSetSteps()
                .createSitelinkSet(TestSitelinkSets.sitelinkSet(clientInfo.getClientId(), sitelinks), clientInfo)
                .getSitelinkSet();
    }

    protected SitelinkSet createSitelinkSetWithReplaceSecondSitelink() {
        List<Sitelink> sitelinks = asList(defaultSitelink().withHref(HREF_DO_NOT_NEED_REPLACE),
                defaultSitelink2().withHref(HREF_NEED_REPLACE));
        return steps.sitelinkSetSteps()
                .createSitelinkSet(TestSitelinkSets.sitelinkSet(clientInfo.getClientId(), sitelinks), clientInfo)
                .getSitelinkSet();
    }

    protected void linkSitelinkSetToBanner(OldBanner banner, SitelinkSet sitelinkSet) {
        ModelChanges<OldBanner> bannerModelChanges = new ModelChanges<>(banner.getId(), OldTextBanner.class)
                .process(sitelinkSet.getId(), OldTextBanner.SITELINKS_SET_ID)
                .castModelUp(OldBanner.class);
        bannerRepository.updateBanners(clientInfo.getShard(), singletonList(bannerModelChanges.applyTo(banner)));
    }

    protected GdiFindAndReplaceBannerHrefItem getBannerPreviewItem(OldBanner banner, @Nullable SitelinkSet sitelinkSet,
                                                                   Map<Long, Set<Long>> sitelinksExceptions) {
        GdiFindAndReplaceBannerHrefItem gdiFindAndReplaceBannerHrefItem = new GdiFindAndReplaceBannerHrefItem()
                .withBannerId(banner.getId())
                .withBannerType(OldBannerType.toSource(banner.getBannerType()))
                .withOldHref(banner.getHref())
                .withNewHref(replaceRule.apply(banner.getHref()))
                .withAdGroupType(AdGroupType.BASE)
                .withSitelinks(sitelinkSet == null ? emptyList()
                        : mapList(sitelinkSet.getSitelinks(), sitelink -> {
                            boolean isException = sitelinksExceptions.containsKey(banner.getId())
                                    && sitelinksExceptions.get(banner.getId()).contains(sitelink.getId());
                            return getSitelinkPreviewItem(sitelink, isException);
                        }
                ));
        if (gdiFindAndReplaceBannerHrefItem.getSitelinks().stream().noneMatch(
                GdiFindAndReplaceBannerHrefItemSitelink::getChanged)) {
            gdiFindAndReplaceBannerHrefItem.withSitelinks(emptyList());
        }
        return gdiFindAndReplaceBannerHrefItem;

    }

    private GdiFindAndReplaceBannerHrefItemSitelink getSitelinkPreviewItem(Sitelink sitelink, boolean isException) {
        String newHref = isException ? null : replaceRule.apply(sitelink.getHref());
        return new GdiFindAndReplaceBannerHrefItemSitelink()
                .withSitelinkId(sitelink.getId())
                .withOldHref(sitelink.getHref())
                .withChanged(newHref != null)
                .withSitelink(newHref == null ? sitelink : sitelink
                        .withId(null)
                        .withHref(newHref));
    }
}
