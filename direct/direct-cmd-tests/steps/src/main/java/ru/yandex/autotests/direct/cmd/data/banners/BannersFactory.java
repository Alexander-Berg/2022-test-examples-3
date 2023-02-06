package ru.yandex.autotests.direct.cmd.data.banners;

import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.banner.HashFlags;
import ru.yandex.autotests.direct.cmd.data.commons.banner.SiteLink;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.steps.base.DirectCmdStepsException;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BannersFactory {

    public static List<SiteLink> getEmptySiteLinks() {
        List<SiteLink> siteLinks = new ArrayList<>();
        for (int i = 0; i <= 3; i++) {
            siteLinks.add(SiteLink.getEmptySiteLink());
        }
        return siteLinks;
    }

    public static Banner getDefaultTextBanner() {
        return fillTextBannerNeededParams(new Banner()
                .withBid(0L)
                .withTitle("Заголовок объявления")
                .withBody("Текст объявления")
                .withHref("ya.ru")
                .withDomain("ya.ru")
                .withUrlProtocol("http://")
                .withBannerType("desktop")
                .withHasVcard(0)
                .withHasHref(1d));
    }

    public static Banner fillTextBannerNeededParams(Banner banner) {
        return banner.withModelId("new1-new1")
                .withHashFlags(new HashFlags())
                .withSiteLinks(getEmptySiteLinks())
                .withAutobudget("")
                .withDayBudget("");
    }

    public static Banner getDefaultMobileAppBanner() {
        return BeanLoadHelper.loadCmdBean(CmdBeans.COMMON_REQUEST_GROUP_MOBILE_DEFAULT2, Group.class).getBanners().get(0);
    }

    public static Banner getDefaultTextImageBanner() {
        return BeanLoadHelper.loadCmdBean(CmdBeans.CMD_COMMON_REQUEST_BANNER_IMAGE_DEFAULT, Banner.class);
    }

    public static Banner getDefaultMobileAppImageBanner() {
        return addNeededAttribute(BeanLoadHelper.loadCmdBean(CmdBeans.CMD_COMMON_REQUEST_BANNER_IMAGE_DEFAULT, Banner.class));
    }

    public static Banner addNeededAttribute(Banner banner) {
        return banner
                .withDomain(null)
                .withHrefModel(null)
                .withHref("")
                .withUrlProtocol("")
                .withReflectedAttrs(Collections.emptyList())
                .withHashFlags(new HashFlags().withAge(""));
    }

    public static Banner getDefaultImageBanner(CampaignTypeEnum campaignType) {
        switch (campaignType) {
            case TEXT:
                return getDefaultTextImageBanner();
            case MOBILE:
                return getDefaultMobileAppImageBanner();
            default:
                throw new DirectCmdStepsException("для типа кампании " + campaignType + " нет графического баннера");
        }
    }

    public static Banner getDefaultBanner(CampaignTypeEnum campaignType) {
        switch (campaignType) {
            case TEXT:
                return getDefaultTextBanner();
            case MOBILE:
                return getDefaultMobileAppBanner();
            default:
                throw new DirectCmdStepsException("для типа кампании" + campaignType + "нет text баннера");
        }
    }

    public static Banner getDefaultMCBanner() {
        return BeanLoadHelper.loadCmdBean(CmdBeans.CMD_COMMON_REQUEST_MC_BANNER_DEFAULT, Banner.class);
    }
}
