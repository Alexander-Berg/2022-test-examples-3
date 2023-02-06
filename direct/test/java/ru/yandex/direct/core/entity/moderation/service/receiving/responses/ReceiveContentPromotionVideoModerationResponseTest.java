package ru.yandex.direct.core.entity.moderation.service.receiving.responses;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.moderation.model.BannerModerationResponse;
import ru.yandex.direct.core.entity.moderation.service.ModerationObjectType;
import ru.yandex.direct.core.entity.moderation.service.receiving.ContentPromotionVideoModerationReceivingService;
import ru.yandex.direct.core.entity.moderation.service.receiving.ModerationReceivingService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.ContentPromotionVideoBannerInfo;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;

import static ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate.SENT;
import static ru.yandex.direct.core.testing.data.TestBanners.activeContentPromotionVideoBanner;

/**
 * Видимо, надо перевести на обработку content_promotion_banner
 */
@Ignore
@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ReceiveContentPromotionVideoModerationResponseTest extends OldAbstractBannerModerationResponseTest {

    @Autowired
    private ContentPromotionVideoModerationReceivingService contentPromotionVideoModerationReceivingService;

    @Autowired
    private TestModerationRepository testModerationRepository;

    @Autowired
    private Steps steps;

    private ClientInfo clientInfo;
    private ContentPromotionVideoBannerInfo bannerInfo;
    private AdGroupInfo adGroupInfo;

    private int shard;
    private long adGroupId;

    @Before
    public void setUp() throws Exception {
        bannerInfo = steps.bannerSteps().createActiveContentPromotionVideoBanner(
                activeContentPromotionVideoBanner().withStatusModerate(SENT));

        clientInfo = bannerInfo.getClientInfo();
        adGroupInfo = bannerInfo.getAdGroupInfo();
        shard = bannerInfo.getShard();
        adGroupId = adGroupInfo.getAdGroupId();

        testModerationRepository.createBannerVersion(shard, bannerInfo.getBannerId(), getDefaultVersion());
    }


    @Override
    protected int getShard() {
        return shard;
    }

    @Override
    protected ModerationReceivingService<BannerModerationResponse> getReceivingService() {
        return contentPromotionVideoModerationReceivingService;
    }

    @Override
    protected long createObjectInDb(long version) {
        var bannerInfo = steps.bannerSteps().createActiveContentPromotionVideoBanner(
                activeContentPromotionVideoBanner().withStatusModerate(SENT), adGroupInfo);

        testModerationRepository.createBannerVersion(shard, bannerInfo.getBannerId(), 1L);

        return bannerInfo.getBannerId();
    }

    @Override
    protected ModerationObjectType getObjectType() {
        return ModerationObjectType.CONTENT_PROMOTION_VIDEO;
    }

    @Override
    protected long getDefaultVersion() {
        return 1L;
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
