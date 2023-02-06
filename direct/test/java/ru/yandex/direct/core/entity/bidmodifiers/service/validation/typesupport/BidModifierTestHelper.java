package ru.yandex.direct.core.entity.bidmodifiers.service.validation.typesupport;

import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktop;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktopAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktopOnly;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktopOnlyAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobileAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierSmartTV;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierSmartTVAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierTablet;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierTabletAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;

public class BidModifierTestHelper {
    public static BidModifierDesktop getDesktop() {
        return new BidModifierDesktop()
                .withCampaignId(1L)
                .withAdGroupId(1L)
                .withType(BidModifierType.DESKTOP_MULTIPLIER)
                .withDesktopAdjustment(new BidModifierDesktopAdjustment().withPercent(100));
    }

    public static BidModifierDesktop getZeroDesktop() {
        return getDesktop().withDesktopAdjustment(new BidModifierDesktopAdjustment().withPercent(0));
    }

    public static BidModifierMobile getMobile() {
        return new BidModifierMobile()
                .withCampaignId(1L)
                .withAdGroupId(1L)
                .withType(BidModifierType.MOBILE_MULTIPLIER)
                .withMobileAdjustment(new BidModifierMobileAdjustment().withOsType(null).withPercent(100));
    }

    public static BidModifierMobile getZeroMobile() {
        return getMobile()
                .withMobileAdjustment(new BidModifierMobileAdjustment().withOsType(null).withPercent(0));
    }

    public static BidModifierSmartTV getSmartTv() {
        return new BidModifierSmartTV()
                .withCampaignId(1L)
                .withAdGroupId(1L)
                .withType(BidModifierType.SMARTTV_MULTIPLIER)
                .withSmartTVAdjustment(new BidModifierSmartTVAdjustment().withPercent(100));
    }

    public static BidModifierSmartTV getZeroSmartTv() {
        return getSmartTv()
                .withSmartTVAdjustment(new BidModifierSmartTVAdjustment().withPercent(0));
    }

    public static BidModifierDesktopOnly getDesktopOnly() {
        return new BidModifierDesktopOnly()
                .withCampaignId(1L)
                .withAdGroupId(1L)
                .withType(BidModifierType.DESKTOP_ONLY_MULTIPLIER)
                .withDesktopOnlyAdjustment(new BidModifierDesktopOnlyAdjustment().withPercent(100));
    }

    public static BidModifierDesktopOnly getZeroDesktopOnly() {
        return getDesktopOnly().withDesktopOnlyAdjustment(new BidModifierDesktopOnlyAdjustment().withPercent(0));
    }

    public static BidModifierTablet getTablet() {
        return new BidModifierTablet()
                .withCampaignId(1L)
                .withAdGroupId(1L)
                .withType(BidModifierType.TABLET_MULTIPLIER)
                .withTabletAdjustment(new BidModifierTabletAdjustment().withOsType(null).withPercent(100));
    }

    public static BidModifierTablet getZeroTablet() {
        return getTablet()
                .withTabletAdjustment(new BidModifierTabletAdjustment().withOsType(null).withPercent(0));
    }
}
