package ru.yandex.direct.core.testing.data;

import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;

import ru.yandex.direct.core.entity.banner.model.InternalBanner;
import ru.yandex.direct.core.entity.banner.model.InternalModerationInfo;
import ru.yandex.direct.core.entity.banner.model.TemplateVariable;
import ru.yandex.direct.core.testing.mock.TemplatePlaceRepositoryMockUtils;
import ru.yandex.direct.core.testing.mock.TemplateResourceRepositoryMockUtils;

import static java.util.Collections.singletonList;

public class TestNewInternalBanners {
    private TestNewInternalBanners() {
        // only for static methods
    }

    public static InternalBanner fullInternalBanner(Long campaignId, Long adGroupId) {
        var banner = clientInternalBanner(adGroupId)
                .withCampaignId(campaignId);
        fillInternalBannerSystemFields(banner);
        return banner;
    }

    public static InternalBanner clientInternalBanner(Long adGroupId) {
        return new InternalBanner()
                .withAdGroupId(adGroupId)
                .withDescription("internal banner desc")
                .withTemplateId(TemplatePlaceRepositoryMockUtils.PLACE_1_TEMPLATE_1)
                .withTemplateVariables(
                        singletonList(new TemplateVariable().withTemplateResourceId(
                                TemplateResourceRepositoryMockUtils.TEMPLATE_1_RESOURCE_1_REQUIRED)
                                .withInternalValue("bbb")))
                .withIsStoppedByUrlMonitoring(false);
    }


    public static InternalBanner moderatedInternalBanner(Long adGroupId) {
        return new InternalBanner()
                .withAdGroupId(adGroupId)
                .withDescription("internal banner desc")
                .withTemplateId(TemplatePlaceRepositoryMockUtils.PLACE_3_TEMPLATE_1)
                .withTemplateVariables(List.of(new TemplateVariable()
                        .withTemplateResourceId(TemplateResourceRepositoryMockUtils.TEMPLATE_7_RESOURCE)
                        .withInternalValue(RandomStringUtils.randomAlphanumeric(11))))
                .withModerationInfo(new InternalModerationInfo()
                        .withIsSecretAd(false)
                        .withStatusShowAfterModeration(true)
                        .withSendToModeration(true)
                        .withTicketUrl("https://st.yandex-team.ru/LEGAL-113"));
    }

    private static void fillInternalBannerSystemFields(InternalBanner banner) {
        TestNewBanners.fillSystemFieldsForActiveBanner(banner);
        banner.withGeoFlag(false);
    }

}
