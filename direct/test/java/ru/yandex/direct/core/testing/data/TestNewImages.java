package ru.yandex.direct.core.testing.data;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.old.Image;
import ru.yandex.direct.core.entity.banner.model.old.StatusImageModerate;

@ParametersAreNonnullByDefault
public class TestNewImages {

    public static Image defaultImage(BannerWithSystemFields banner, String imageHash) {
        return new Image()
                .withBannerId(banner.getId())
                .withCampaignId(banner.getCampaignId())
                .withAdGroupId(banner.getAdGroupId())
                .withImageHash(imageHash)
                .withImageText("раз два три")
                .withDisclaimerText("раз два три")
                .withStatusModerate(StatusImageModerate.YES);
    }

}
