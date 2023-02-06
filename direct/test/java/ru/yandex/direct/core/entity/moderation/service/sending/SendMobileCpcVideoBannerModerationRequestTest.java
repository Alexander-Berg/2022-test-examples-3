package ru.yandex.direct.core.entity.moderation.service.sending;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroup;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.moderation.model.cpm.video.CpmVideoBannerRequestData;
import ru.yandex.direct.core.entity.moderation.model.mobile_content.MobileAppModerationData;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static ru.yandex.direct.core.testing.data.TestBanners.activeCpcVideoBanner;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SendMobileCpcVideoBannerModerationRequestTest extends AbstractSendCpcVideoBannerModerationRequestTest {

    @Autowired
    private MobileCpcVideoBannerSender mobileCpcVideoBannerSender;

    @Override
    protected BaseCpmVideoBannerSender getSender() {
        return mobileCpcVideoBannerSender;
    }

    @Override
    protected void init() {
        adGroupInfo = steps.adGroupSteps().createActiveMobileContentAdGroup(clientInfo);
        cpcBannerInfo = steps.bannerSteps().createActiveCpcVideoBanner(
                activeCpcVideoBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId(),
                        creativeInfo.getCreativeId())
                        .withStatusModerate(OldBannerStatusModerate.READY),
                adGroupInfo);
    }

    @Override
    protected void setExpectedRequestDataFields(CpmVideoBannerRequestData requestData) {
        requestData.setMobileContentModerationData(getExpectedMobileAppModerationData());
    }

    private MobileAppModerationData getExpectedMobileAppModerationData() {
        MobileContentAdGroup mobileContentAdGroup = (MobileContentAdGroup) adGroupInfo.getAdGroup();
        MobileAppModerationData data = new MobileAppModerationData();

        data.setStoreContentId(mobileContentAdGroup.getMobileContent().getStoreContentId());
        data.setAppHref(mobileContentAdGroup.getStoreUrl());
        data.setMobileContentId(mobileContentAdGroup.getMobileContentId());
        data.setBundleId(mobileContentAdGroup.getMobileContent().getBundleId());

        return data;
    }
}
