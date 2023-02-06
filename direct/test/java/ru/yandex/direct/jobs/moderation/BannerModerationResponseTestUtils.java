package ru.yandex.direct.jobs.moderation;

import ru.yandex.direct.core.entity.moderation.model.AbstractModerationResponse;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationResponse;
import ru.yandex.direct.core.entity.moderation.model.ModerationDecision;
import ru.yandex.direct.core.entity.moderation.model.Verdict;
import ru.yandex.direct.core.entity.moderation.service.ModerationObjectType;
import ru.yandex.direct.core.entity.moderation.service.ModerationServiceNames;
import ru.yandex.direct.core.testing.info.AbstractBannerInfo;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.steps.Steps;

import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;

public class BannerModerationResponseTestUtils {
    private BannerModerationResponseTestUtils() {
    }

    public static AbstractBannerInfo getAppropriateBanner(ModerationObjectType moderationType, Steps steps) {
        switch (moderationType) {
            case AUDIO_CREATIVE:
                return steps.bannerCreativeSteps().createDefaultCpmAudioBannerCreative().getBannerInfo();
            case GEO_PIN_CREATIVE:
                return steps.bannerCreativeSteps().createDefaultCpmGeoPinBannerCreative().getBannerInfo();
            case FIXCPM_YNDX_FRONTPAGE_CREATIVE:
                AdGroupInfo fixCpmYndxFrontpageAdGroup = steps.adGroupSteps().createDefaultFixCpmYndxFrontpageAdGroup();
                return steps.bannerSteps().createActiveCpmBanner(activeCpmBanner(null, null, null),
                        fixCpmYndxFrontpageAdGroup);
            case YNDX_FRONTPAGE_CREATIVE:
                AdGroupInfo cpmYndxFrontpageAdGroup = steps.adGroupSteps().createDefaultCpmYndxFrontpageAdGroup();
                return steps.bannerSteps().createActiveCpmBanner(activeCpmBanner(null, null, null),
                        cpmYndxFrontpageAdGroup);
            case CONTENT_PROMOTION_COLLECTION:
                return steps.bannerSteps().createActiveContentPromotionBannerCollectionType();
            case CONTENT_PROMOTION_VIDEO:
                return steps.bannerSteps().createActiveContentPromotionBannerVideoType();
            case HTML5:
                return steps.bannerSteps().createActiveImageCreativeBanner();
            case CANVAS:
                return steps.bannerSteps().createActiveCpmBanner();
            default:
                throw new UnsupportedOperationException("Moderation type unsupported");
        }
    }

    public static AbstractModerationResponse getBannerModerationResponse(ModerationObjectType type,
                                                                         ModerationDecision verdictStr,
                                                                         AbstractBannerInfo bannerInfo) {
        BannerModerationResponse response = new BannerModerationResponse();
        response.setService(ModerationServiceNames.DIRECT_SERVICE);
        response.setType(type);

        if (bannerInfo == null) {
            return response;
        }

        BannerModerationMeta meta = new BannerModerationMeta();
        meta.setVersionId(1);
        meta.setUid(bannerInfo.getClientInfo().getUid());
        meta.setClientId(bannerInfo.getClientInfo().getClientId().asLong());
        meta.setCampaignId(bannerInfo.getBanner().getCampaignId());
        meta.setAdGroupId(bannerInfo.getBanner().getAdGroupId());
        meta.setBannerId(bannerInfo.getBannerId());

        response.setMeta(meta);

        Verdict verdict = new Verdict();
        verdict.setVerdict(verdictStr);
        response.setResult(verdict);
        return response;
    }
}
