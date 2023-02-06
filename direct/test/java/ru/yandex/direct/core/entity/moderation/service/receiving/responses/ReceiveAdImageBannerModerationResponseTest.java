package ru.yandex.direct.core.entity.moderation.service.receiving.responses;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.ImageSize;
import ru.yandex.direct.core.entity.banner.model.old.OldImageHashBanner;
import ru.yandex.direct.core.entity.image.model.BannerImageFormat;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationResponse;
import ru.yandex.direct.core.entity.moderation.service.ModerationObjectType;
import ru.yandex.direct.core.entity.moderation.service.receiving.AdImageBannerModerationReceivingService;
import ru.yandex.direct.core.entity.moderation.service.receiving.ModerationReceivingService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;

import static ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate.SENT;
import static ru.yandex.direct.core.testing.data.TestBanners.activeImageHashBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.defaultClientImageHashBanner;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ReceiveAdImageBannerModerationResponseTest extends OldAbstractBannerModerationResponseTest {

    @Autowired
    private AdImageBannerModerationReceivingService adImageBannerModerationReceivingService;

    @Autowired
    private TestModerationRepository testModerationRepository;

    @Autowired
    private Steps steps;

    private AdGroupInfo adGroupInfo;

    private long adGroupId;

    private int shard;
    private ClientInfo clientInfo;
    private CampaignInfo campaignInfo;
    private OldImageHashBanner banner;
    private BannerImageFormat bannerImageFormat;

    @Before
    public void setUp() throws Exception {
        clientInfo = steps.clientSteps().createDefaultClient();

        campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);
        adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(campaignInfo);

        shard = clientInfo.getShard();

        banner = (OldImageHashBanner) steps.bannerSteps()
                .createBanner(activeImageHashBanner(campaignInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                        .withStatusModerate(SENT), adGroupInfo
                )
                .getBanner();

        bannerImageFormat = testModerationRepository.addBannerImageFormat(shard, banner.getImage().getImageHash(),
                new ImageSize().withHeight(100).withWidth(100));

        adGroupId = adGroupInfo.getAdGroupId();

        testModerationRepository.createBannerVersion(shard, banner.getId(), getDefaultVersion());
    }


    @Override
    protected int getShard() {
        return shard;
    }

    @Override
    protected ModerationReceivingService<BannerModerationResponse> getReceivingService() {
        return adImageBannerModerationReceivingService;
    }

    @Override
    protected long createObjectInDb(long version) {
        OldImageHashBanner newBanner = defaultClientImageHashBanner(adGroupInfo.getCampaignId(), adGroupId,
                banner.getImage().getImageHash())
                .withStatusModerate(SENT);

        OldImageHashBanner anotherBanner =
                steps.bannerSteps().createActiveImageHashBanner(newBanner, adGroupInfo).getBanner();

        testModerationRepository.createBannerVersion(shard, anotherBanner.getId(), version);
        return newBanner.getId();
    }

    @Override
    protected ModerationObjectType getObjectType() {
        return ModerationObjectType.AD_IMAGE;
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

}
