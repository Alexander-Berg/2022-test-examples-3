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
import ru.yandex.direct.core.entity.banner.model.DynamicBanner;
import ru.yandex.direct.core.entity.banner.model.Language;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.moderation.service.ModerationObjectType;
import ru.yandex.direct.core.entity.moderation.service.receiving.ModerationReceivingService;
import ru.yandex.direct.core.entity.moderation.service.receiving.TextBannerModerationReceivingService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.NewDynamicBannerInfo;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.DynamicBannerSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.BannersBannerType;

import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.core.entity.banner.model.BannerStatusModerate.SENT;
import static ru.yandex.direct.core.testing.data.TestNewDynamicBanners.fullDynamicBanner;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ReceiveDynamicBannerModerationResponseTest extends OldAbstractBannerModerationResponseTest {

    @Autowired
    private TextBannerModerationReceivingService receivingService;

    @Autowired
    private TestModerationRepository testModerationRepository;

    @Autowired
    protected BannerTypedRepository bannerTypedRepository;

    @Autowired
    private DynamicBannerSteps bannerSteps;

    @Autowired
    private Steps steps;

    private AdGroupInfo adGroupInfo;

    private int shard;
    private ClientInfo clientInfo;
    private CampaignInfo campaignInfo;

    private DynamicBanner banner;

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();

        campaignInfo = steps.campaignSteps().createActiveDynamicCampaign(clientInfo);
        adGroupInfo = steps.adGroupSteps().createActiveDynamicTextAdGroup(campaignInfo);

        shard = clientInfo.getShard();
        banner = fullDynamicBanner(campaignInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                .withLanguage(Language.RU_)
                .withStatusModerate(SENT)
                .withBody("Test Body");

        bannerSteps.createDynamicBanner(new NewDynamicBannerInfo()
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
        var banner = fullDynamicBanner(null, null)
                .withStatusModerate(SENT);
        bannerSteps.createDynamicBanner(new NewDynamicBannerInfo()
                .withBanner(banner));

        testModerationRepository.createBannerVersion(shard, banner.getId(), version);

        return banner.getId();
    }

    // Динамические баннера модерируются, как text_sm.
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
        return PpcPropertyNames.RESTRICTED_DYNAMIC_TRANSPORT_NEW_MODERATION;
    }

    @Override
    protected BannersBannerType getDirectBannerType() {
        return BannersBannerType.dynamic;
    }

    @Override
    protected void checkStatusModerateNotChanged(long id) {
        List<DynamicBanner> banners = bannerTypedRepository.getStrictly(shard, Collections.singletonList(id), DynamicBanner.class);
        assertEquals(1, banners.size());
        assertEquals(BannerStatusModerate.SENT, banners.get(0).getStatusModerate());
    }
}
