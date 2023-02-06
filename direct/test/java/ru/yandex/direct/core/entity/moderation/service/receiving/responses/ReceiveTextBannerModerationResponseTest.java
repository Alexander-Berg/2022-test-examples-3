package ru.yandex.direct.core.entity.moderation.service.receiving.responses;

import java.util.Collections;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.db.PpcPropertyName;
import ru.yandex.direct.common.db.PpcPropertyNames;
import ru.yandex.direct.core.entity.banner.model.Language;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationResponse;
import ru.yandex.direct.core.entity.moderation.service.ModerationObjectType;
import ru.yandex.direct.core.entity.moderation.service.receiving.ModerationReceivingService;
import ru.yandex.direct.core.entity.moderation.service.receiving.TextBannerModerationReceivingService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.NewTextBannerInfo;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.TextBannerSteps;

import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.core.entity.banner.model.BannerStatusModerate.SENT;
import static ru.yandex.direct.core.entity.moderation.service.ModerationObjectType.TEXT_AD;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ReceiveTextBannerModerationResponseTest extends OldAbstractBannerModerationResponseTest {

    @Autowired
    private TextBannerModerationReceivingService receivingService;

    @Autowired
    private TestModerationRepository testModerationRepository;

    @Autowired
    private TextBannerSteps textBannerSteps;

    @Autowired
    private Steps steps;

    private AdGroupInfo adGroupInfo;

    private int shard;
    private ClientInfo clientInfo;
    private CampaignInfo campaignInfo;
    private TextBanner banner;

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();

        campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);
        adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(campaignInfo);

        shard = clientInfo.getShard();

        banner = fullTextBanner(campaignInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                .withLanguage(Language.UNKNOWN)
                .withStatusModerate(SENT)
                .withBody("Test Body")
                .withTitle("TestTitle")
                .withLanguage(Language.RU_)
                .withTitleExtension("TestTitleExt");

        textBannerSteps.createBanner(new NewTextBannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(adGroupInfo));

        testModerationRepository.createBannerVersion(shard, banner.getId(), getDefaultVersion());
    }

    @Override
    protected int getShard() {
        return shard;
    }

    @Override
    protected ModerationReceivingService<BannerModerationResponse> getReceivingService() {
        return receivingService;
    }

    @Override
    protected long createObjectInDb(long version) {
        TextBanner newBanner = fullTextBanner(campaignInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                .withStatusModerate(SENT);

        TextBanner anotherBanner =
                textBannerSteps.createBanner(new NewTextBannerInfo()
                        .withAdGroupInfo(adGroupInfo)
                        .withBanner(newBanner))
                        .getBanner();

        testModerationRepository.createBannerVersion(shard, anotherBanner.getId(), version);
        return newBanner.getId();
    }

    @Override
    protected ModerationObjectType getObjectType() {
        return TEXT_AD;
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
        return PpcPropertyNames.RESTRICTED_TEXT_TRANSPORT_NEW_MODERATION;
    }

    @Override
    protected void checkStatusModerateNotChanged(long id) {
        var banners = bannerRepository.getBanners(shard, Collections.singletonList(id));
        assertEquals(1, banners.size());
        assertEquals(OldBannerStatusModerate.SENT, banners.get(0).getStatusModerate());
    }
}
