package ru.yandex.direct.core.entity.moderation.service.receiving.responses;

import java.util.Collections;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.db.PpcPropertyName;
import ru.yandex.direct.common.db.PpcPropertyNames;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.Language;
import ru.yandex.direct.core.entity.banner.model.MobileAppBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.moderation.service.ModerationObjectType;
import ru.yandex.direct.core.entity.moderation.service.receiving.ModerationReceivingService;
import ru.yandex.direct.core.entity.moderation.service.receiving.TextBannerModerationReceivingService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.NewMobileAppBannerInfo;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.MobileAppBannerSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.BannersBannerType;

import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.core.entity.banner.model.BannerStatusModerate.SENT;
import static ru.yandex.direct.core.testing.data.TestNewMobileAppBanners.fullMobileAppBanner;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ReceiveMobileContentBannerModerationResponseTest extends OldAbstractBannerModerationResponseTest {

    @Autowired
    private TextBannerModerationReceivingService receivingService;

    @Autowired
    private TestModerationRepository testModerationRepository;

    @Autowired
    protected BannerTypedRepository bannerTypedRepository;

    @Autowired
    private MobileAppBannerSteps bannerSteps;

    @Autowired
    private Steps steps;

    private AdGroupInfo adGroupInfo;

    private int shard;
    private ClientInfo clientInfo;
    private CampaignInfo campaignInfo;

    private MobileAppBanner banner;

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();

        campaignInfo = steps.campaignSteps().createActiveMobileAppCampaign(clientInfo);
        adGroupInfo = steps.adGroupSteps().createActiveMobileContentAdGroup(campaignInfo);

        shard = clientInfo.getShard();
        banner = fullMobileAppBanner(campaignInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                .withLanguage(Language.UNKNOWN)
                .withStatusModerate(SENT)
                .withBody("Test Body")
                .withTitle("TestTitle")
                .withLanguage(Language.RU_);

        bannerSteps.createMobileAppBanner(new NewMobileAppBannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(adGroupInfo));

        testModerationRepository.createBannerVersion(shard, banner.getId(), getDefaultVersion());
    }

    @Override
    protected int getShard() {
        return shard;
    }

    @Override
    protected ModerationReceivingService getReceivingService() {
        return receivingService;
    }

    @Override
    protected long createObjectInDb(long version) {
        var banner = fullMobileAppBanner(null, null)
                .withStatusModerate(SENT);
        bannerSteps.createMobileAppBanner(new NewMobileAppBannerInfo()
                .withBanner(banner));

        testModerationRepository.createBannerVersion(shard, banner.getId(), version);

        return banner.getId();
    }

    // Мобильные баннера модерируются, как text_sm.
    @Override
    protected ModerationObjectType getObjectType() {
        return ModerationObjectType.TEXT_AD;
    }

    @Override
    protected long getDefaultVersion() {
        return 75000L;
    }

    @Override
    protected long getDefaultObjectId() {
        return banner.getId();
    }

    @Override
    protected ClientInfo getDefaultObjectClientInfo() {
        return clientInfo;
    }

    @Override
    protected PpcPropertyName<Boolean> getRestrictedModePropertyName() {
        return PpcPropertyNames.RESTRICTED_MOBILE_CONTENT_TRANSPORT_NEW_MODERATION;
    }

    @Override
    protected BannersBannerType getDirectBannerType() {
        return BannersBannerType.mobile_content;
    }

    @Override
    protected void checkStatusModerateNotChanged(long id) {
        List<MobileAppBanner> banners = bannerTypedRepository.getStrictly(shard, Collections.singletonList(id), MobileAppBanner.class);
        assertEquals(1, banners.size());
        assertEquals(BannerStatusModerate.SENT, banners.get(0).getStatusModerate());
    }
}
