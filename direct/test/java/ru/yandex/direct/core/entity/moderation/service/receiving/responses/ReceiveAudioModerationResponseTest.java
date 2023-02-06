package ru.yandex.direct.core.entity.moderation.service.receiving.responses;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationResponse;
import ru.yandex.direct.core.entity.moderation.service.ModerationObjectType;
import ru.yandex.direct.core.entity.moderation.service.receiving.CpmAudioModerationReceivingService;
import ru.yandex.direct.core.entity.moderation.service.receiving.ModerationReceivingService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CpmAudioBannerInfo;
import ru.yandex.direct.core.testing.repository.TestBannerCreativeRepository;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;

import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmAudioBanner;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpmAudioAddition;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ReceiveAudioModerationResponseTest extends OldAbstractBannerModerationResponseTest {

    private static final long DEFAULT_VERSION = 1L;

    @Autowired
    TestBannerCreativeRepository testBannerCreativeRepository;

    @Autowired
    TestModerationRepository testModerationRepository;

    @Autowired
    Steps steps;

    @Autowired
    CpmAudioModerationReceivingService cpmAudioModerationReceivingService;

    private CpmAudioBannerInfo bannerInfo;
    private AdGroupInfo adGroupInfo;
    private Creative creative;


    @Before
    public void before() {
        adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup();
        bannerInfo = steps.bannerSteps().createActiveCpmAudioBanner(
                activeCpmAudioBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId(), null)
                        .withStatusModerate(OldBannerStatusModerate.SENT)
                        .withCreativeStatusModerate(OldBannerCreativeStatusModerate.SENT)
                        .withStatusBsSynced(StatusBsSynced.YES),
                adGroupInfo);

        creative = defaultCpmAudioAddition(adGroupInfo.getClientId(), null);
        steps.creativeSteps().createCreative(creative, adGroupInfo.getClientInfo());

        testBannerCreativeRepository.linkBannerWithCreative(bannerInfo, creative.getId());

        testModerationRepository.createBannerVersion(adGroupInfo.getShard(), bannerInfo.getBannerId(), DEFAULT_VERSION);
    }


    @Override
    protected int getShard() {
        return bannerInfo.getClientInfo().getShard();
    }

    @Override
    protected ModerationReceivingService<BannerModerationResponse> getReceivingService() {
        return cpmAudioModerationReceivingService;
    }

    @Override
    protected long createObjectInDb(long version) {
        var bannerInfo = steps.bannerSteps().createActiveCpmAudioBanner(
                activeCpmAudioBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId(), null)
                        .withStatusModerate(OldBannerStatusModerate.SENT)
                        .withCreativeStatusModerate(OldBannerCreativeStatusModerate.SENT)
                        .withStatusBsSynced(StatusBsSynced.YES),
                adGroupInfo);

        testBannerCreativeRepository.linkBannerWithCreative(bannerInfo, creative.getId());
        testModerationRepository.createBannerVersion(getShard(), bannerInfo.getBannerId(), version);

        return bannerInfo.getBannerId();
    }

    @Override
    protected ModerationObjectType getObjectType() {
        return ModerationObjectType.AUDIO_CREATIVE;
    }

    @Override
    protected long getDefaultVersion() {
        return DEFAULT_VERSION;
    }

    @Override
    protected long getDefaultObjectId() {
        return bannerInfo.getBannerId();
    }

    @Override
    protected ClientInfo getDefaultObjectClientInfo() {
        return bannerInfo.getClientInfo();
    }
}
