package ru.yandex.direct.core.entity.moderation.service.receiving.responses;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationResponse;
import ru.yandex.direct.core.entity.moderation.service.ModerationObjectType;
import ru.yandex.direct.core.entity.moderation.service.receiving.CpmVideoBannerModerationReceivingService;
import ru.yandex.direct.core.entity.moderation.service.receiving.ModerationReceivingService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CpmBannerInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.repository.TestBannerCreativeRepository;
import ru.yandex.direct.core.testing.repository.TestBannerRepository;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;

import static ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate.SENT;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpmVideoAddition;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ReceiveCpmVideoModerationResponseTest extends OldAbstractBannerModerationResponseTest {

    @Autowired
    private CpmVideoBannerModerationReceivingService cpmVideoBannerModerationReceivingService;

    @Autowired
    private OldBannerRepository bannerRepository;

    @Autowired
    private TestBannerCreativeRepository testBannerCreativeRepository;

    @Autowired
    private TestModerationRepository testModerationRepository;

    @Autowired
    private TestBannerRepository testBannerRepository;

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private Steps steps;

    private ClientInfo clientInfo;
    private CpmBannerInfo bannerInfo;
    private AdGroupInfo adGroupInfo;

    private int shard;
    private long adGroupId;
    private long creativeId;

    @Before
    public void setUp() throws Exception {
        clientInfo = steps.clientSteps().createDefaultClient();
        adGroupInfo = steps.adGroupSteps().createDefaultCpmVideoAdGroup(clientInfo);
        Creative creative = defaultCpmVideoAddition(null, null).withHeight(67L).withWidth(320L);
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(creative, clientInfo);

        adGroupId = adGroupInfo.getAdGroupId();
        creativeId = creativeInfo.getCreativeId();

        OldCpmBanner banner =
                activeCpmBanner(adGroupInfo.getCampaignId(), adGroupId, creativeId)
                        .withStatusModerate(SENT).withLanguage(defaultLanguage());

        bannerInfo = steps.bannerSteps().createActiveCpmBanner(banner, adGroupInfo);
        shard = bannerInfo.getShard();
        testModerationRepository.createBannerVersion(shard, bannerInfo.getBannerId(), getDefaultVersion());
    }

    @Override
    protected int getShard() {
        return shard;
    }

    @Override
    protected ModerationReceivingService<BannerModerationResponse> getReceivingService() {
        return cpmVideoBannerModerationReceivingService;
    }

    @Override
    protected long createObjectInDb(long version) {
        OldCpmBanner banner =
                activeCpmBanner(adGroupInfo.getCampaignId(), adGroupId, creativeId)
                        .withStatusModerate(SENT).withLanguage(defaultLanguage());
        steps.bannerSteps().createActiveCpmBanner(banner, adGroupInfo);
        testModerationRepository.createBannerVersion(shard, banner.getId(), version);
        return banner.getId();
    }

    @Override
    protected ModerationObjectType getObjectType() {
        return ModerationObjectType.CPM_VIDEO;
    }

    @Override
    protected long getDefaultVersion() {
        return 64L;
    }

    @Override
    protected long getDefaultObjectId() {
        return bannerInfo.getBannerId();
    }

    @Override
    protected ClientInfo getDefaultObjectClientInfo() {
        return clientInfo;
    }

}
