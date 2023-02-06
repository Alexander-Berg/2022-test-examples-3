package ru.yandex.direct.core.testing.steps;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.banner.model.ModerateBannerPage;
import ru.yandex.direct.core.testing.info.AbstractBannerInfo;
import ru.yandex.direct.core.testing.info.ModerateBannerPageInfo;
import ru.yandex.direct.core.testing.repository.TestModerateBannerPagesRepository;

import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.testing.data.TestModerateBannerPages.defaultModerateBannerPage;

public class ModerateBannerPageSteps {

    @Autowired
    private BannerSteps bannerSteps;

    @Autowired
    private BannerModerationVersionSteps bannerModerationVersionSteps;

    @Autowired
    private TestModerateBannerPagesRepository testModerateBannerPagesRepository;

    public ModerateBannerPageInfo createModerateBannerPage(AbstractBannerInfo bannerInfo,
                                                           ModerateBannerPage moderateBannerPage) {
        return createModerateBannerPage(new ModerateBannerPageInfo()
                .withBannerInfo(bannerInfo)
                .withBannerVersion(moderateBannerPage.getVersion())
                .withModerateBannerPage(moderateBannerPage));
    }

    public ModerateBannerPageInfo createModerateBannerPage(ModerateBannerPageInfo moderateBannerPageInfo) {
        if (moderateBannerPageInfo.getModerateBannerPage() == null) {
            moderateBannerPageInfo.withModerateBannerPage(defaultModerateBannerPage());
        }
        if (moderateBannerPageInfo.getBannerInfo() == null) {
            moderateBannerPageInfo.withBannerInfo(bannerSteps.createActiveTextBanner());
        }
        if (moderateBannerPageInfo.getBannerVersion() == null) {
            moderateBannerPageInfo.withBannerVersion(10L);
            var bannerInfo = moderateBannerPageInfo.getBannerInfo();
            bannerModerationVersionSteps.addBannerModerationVersion(bannerInfo.getShard(), bannerInfo.getBannerId(),
                    moderateBannerPageInfo.getBannerVersion());
        }
        if (moderateBannerPageInfo.getModerateBannerPageId() == null) {
            ModerateBannerPage moderateBannerPage = moderateBannerPageInfo.getModerateBannerPage();
            moderateBannerPage
                    .withBannerId(moderateBannerPageInfo.getBannerInfo().getBannerId())
                    .withVersion(moderateBannerPageInfo.getBannerVersion());
            testModerateBannerPagesRepository
                    .addModerateBannerPages(moderateBannerPageInfo.getShard(), singletonList(moderateBannerPage));
        }
        return moderateBannerPageInfo;
    }
}
