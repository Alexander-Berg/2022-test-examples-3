package ru.yandex.direct.core.entity.moderation.service.receiving.responses;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationResponse;
import ru.yandex.direct.core.entity.moderation.service.ModerationObjectType;
import ru.yandex.direct.core.entity.moderation.service.receiving.CpmGeoPinModerationReceivingService;
import ru.yandex.direct.core.entity.moderation.service.receiving.ModerationReceivingService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CpmGeoPinBannerInfo;
import ru.yandex.direct.core.testing.repository.TestBannerCreativeRepository;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;

import static ru.yandex.direct.core.entity.moderation.service.ModerationObjectType.GEO_PIN_CREATIVE;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmGeoPinBanner;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpmGeoPinAddition;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ReceiveCpmGeoPinModerationResponseTest extends OldAbstractBannerModerationResponseTest {

    private static final long DEFAULT_VERSION = 1L;

    @Autowired
    TestBannerCreativeRepository testBannerCreativeRepository;

    @Autowired
    TestModerationRepository testModerationRepository;

    @Autowired
    Steps steps;

    @Autowired
    OldBannerRepository bannerRepository;

    @Autowired
    CpmGeoPinModerationReceivingService cpmGeoPinModerationReceivingService;

    private CpmGeoPinBannerInfo bannerInfo;
    private AdGroupInfo adGroupInfo;
    private Creative creative;
    private int shard;
    private long defaultBannerId;

    @Before
    public void before() {
        adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup();
        bannerInfo = steps.bannerSteps().createActiveCpmGeoPinBanner(
                activeCpmGeoPinBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId(), null, null)
                        .withStatusModerate(OldBannerStatusModerate.SENT)
                        .withCreativeStatusModerate(OldBannerCreativeStatusModerate.SENT)
                        .withStatusBsSynced(StatusBsSynced.YES)
                        .withFlags(null),
                adGroupInfo);

        shard = bannerInfo.getShard();
        defaultBannerId = bannerInfo.getBannerId();

        creative = defaultCpmGeoPinAddition(adGroupInfo.getClientId(), null);
        steps.creativeSteps().createCreative(creative, adGroupInfo.getClientInfo());

        testBannerCreativeRepository.linkBannerWithCreative(bannerInfo, creative.getId());

        testModerationRepository.createBannerVersion(shard, defaultBannerId, DEFAULT_VERSION);
    }

    @Override
    protected int getShard() {
        return shard;
    }

    @Override
    protected ModerationReceivingService<BannerModerationResponse> getReceivingService() {
        return cpmGeoPinModerationReceivingService;
    }

    @Override
    protected long createObjectInDb(long version) {
        var bannerInfo = steps.bannerSteps().createActiveCpmGeoPinBanner(
                activeCpmGeoPinBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId(), null, null)
                        .withStatusModerate(OldBannerStatusModerate.SENT)
                        .withCreativeStatusModerate(OldBannerCreativeStatusModerate.SENT)
                        .withStatusBsSynced(StatusBsSynced.YES),
                adGroupInfo);

        testBannerCreativeRepository.linkBannerWithCreative(bannerInfo, creative.getId());

        testModerationRepository.createBannerVersion(shard, bannerInfo.getBannerId(), version);

        return bannerInfo.getBannerId();
    }

    @Override
    protected ModerationObjectType getObjectType() {
        return GEO_PIN_CREATIVE;
    }

    @Override
    protected long getDefaultVersion() {
        return DEFAULT_VERSION;
    }

    @Override
    protected long getDefaultObjectId() {
        return defaultBannerId;
    }

    @Override
    protected ClientInfo getDefaultObjectClientInfo() {
        return bannerInfo.getClientInfo();
    }
}
