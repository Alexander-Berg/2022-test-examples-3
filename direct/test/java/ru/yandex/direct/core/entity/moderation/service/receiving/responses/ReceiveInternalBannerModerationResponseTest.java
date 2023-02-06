package ru.yandex.direct.core.entity.moderation.service.receiving.responses;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.InternalBanner;
import ru.yandex.direct.core.entity.banner.model.InternalModerationInfo;
import ru.yandex.direct.core.entity.banner.model.TemplateVariable;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationResponse;
import ru.yandex.direct.core.entity.moderation.service.ModerationObjectType;
import ru.yandex.direct.core.entity.moderation.service.receiving.InternalBannerModerationReceivingService;
import ru.yandex.direct.core.entity.moderation.service.receiving.ModerationReceivingService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.NewInternalBannerInfo;
import ru.yandex.direct.core.testing.mock.TemplatePlaceRepositoryMockUtils;
import ru.yandex.direct.core.testing.mock.TemplateResourceRepositoryMockUtils;
import ru.yandex.direct.core.testing.repository.TestBannerRepository;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.BannersStatusshow;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.core.entity.moderation.model.ModerationDecision.Yes;
import static ru.yandex.direct.core.testing.data.TestNewInternalBanners.fullInternalBanner;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class ReceiveInternalBannerModerationResponseTest extends OldAbstractBannerModerationResponseTest {

    @Autowired
    private InternalBannerModerationReceivingService internalBannerModerationReceivingService;

    @Autowired
    private TestModerationRepository testModerationRepository;

    @Autowired
    private TestBannerRepository testBannerRepository;

    @Autowired
    private BannerTypedRepository bannerTypedRepository;

    @Autowired
    private Steps steps;

    private ClientInfo clientInfo;
    private NewInternalBannerInfo bannerInfo;
    private AdGroupInfo adGroupInfo;

    private int shard;

    @Before
    public void setUp() throws Exception {
        clientInfo = steps.internalAdProductSteps().createDefaultInternalAdProduct();
        shard = clientInfo.getShard();

        CampaignInfo campaignInfo =
                steps.campaignSteps().createActiveInternalDistribCampaignWithModeratedPlace(clientInfo);
        adGroupInfo = steps.adGroupSteps().createActiveInternalAdGroup(campaignInfo);

        InternalBanner internalBanner = getInternalBanner();
        bannerInfo = steps.internalBannerSteps().createInternalBanner(new NewInternalBannerInfo()
                .withBanner(internalBanner)
                .withAdGroupInfo(adGroupInfo));
        testModerationRepository.createBannerVersion(shard, bannerInfo.getBannerId(), getDefaultVersion());
    }

    @Override
    protected int getShard() {
        return shard;
    }

    @Override
    protected ModerationReceivingService<BannerModerationResponse> getReceivingService() {
        return internalBannerModerationReceivingService;
    }

    @Override
    protected long createObjectInDb(long version) {
        InternalBanner internalBanner = getInternalBanner();
        NewInternalBannerInfo secondInternalBannerInfo =
                steps.internalBannerSteps().createInternalBanner(new NewInternalBannerInfo()
                        .withBanner(internalBanner)
                        .withAdGroupInfo(adGroupInfo));
        testModerationRepository.createBannerVersion(shard, secondInternalBannerInfo.getBannerId(), version);
        return secondInternalBannerInfo.getBannerId();
    }

    @Override
    protected ModerationObjectType getObjectType() {
        return ModerationObjectType.INTERNAL_BANNER;
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

    private InternalBanner getInternalBanner() {
        return fullInternalBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                .withStatusModerate(BannerStatusModerate.SENT)
                .withLanguage(defaultLanguage())
                .withTemplateId(TemplatePlaceRepositoryMockUtils.PLACE_3_TEMPLATE_1)
                .withTemplateVariables(List.of(
                        new TemplateVariable()
                                .withTemplateResourceId(TemplateResourceRepositoryMockUtils.TEMPLATE_7_RESOURCE)
                                .withInternalValue("bbb")))
                .withIsStoppedByUrlMonitoring(false)
                .withModerationInfo(new InternalModerationInfo()
                        .withIsSecretAd(true)
                        .withStatusShowAfterModeration(true)
                        .withTicketUrl("https://st.yandex-team.ru/LEGAL-113"));
    }

    @Test
    public void receiveResponseForInactiveBannerWithShowAfterModeration_ActivateBanner() {
        testBannerRepository.updateStatusShow(bannerInfo.getShard(), bannerInfo.getBannerId(), BannersStatusshow.No);
        BannerModerationResponse response = createResponseForDefaultObject(Yes);
        var unknownVerdictCountAndSuccess = getReceivingService()
                .processModerationResponses(getShard(), singletonList(response));
        assertEquals(0, (int) unknownVerdictCountAndSuccess.getLeft());
        assertEquals(1, unknownVerdictCountAndSuccess.getRight().size());
        List<InternalBanner> banners = bannerTypedRepository.getStrictlyFullyFilled(bannerInfo.getShard(),
                List.of(bannerInfo.getBannerId()), InternalBanner.class);
        assertEquals(true, banners.get(0).getStatusShow());
    }

    @Test
    public void receiveResponseForInactiveBannerWithoutShowAfterModeration_NotActivateBanner() {
        testBannerRepository.updateStatusShow(bannerInfo.getShard(), bannerInfo.getBannerId(), BannersStatusshow.No);
        testBannerRepository.updateInternalBannerModerationInfo(bannerInfo.getShard(),
                bannerInfo.getBannerId(),
                new InternalModerationInfo()
                        .withIsSecretAd(true)
                        .withStatusShowAfterModeration(false)
                        .withTicketUrl("https://st.yandex-team.ru/LEGAL-113"));

        BannerModerationResponse response = createResponseForDefaultObject(Yes);
        var unknownVerdictCountAndSuccess = getReceivingService()
                .processModerationResponses(getShard(), singletonList(response));
        assertEquals(0, (int) unknownVerdictCountAndSuccess.getLeft());
        assertEquals(1, unknownVerdictCountAndSuccess.getRight().size());
        List<InternalBanner> banners = bannerTypedRepository.getStrictlyFullyFilled(bannerInfo.getShard(),
                List.of(bannerInfo.getBannerId()), InternalBanner.class);

        assertEquals(false, banners.get(0).getStatusShow());
    }

    @Test
    public void receiveResponseForInactiveBannerWithShowAfterModeration_AndStoppedByUrlMonitoring_NotActivateBanner() {
        testBannerRepository.updateStatusShow(bannerInfo.getShard(), bannerInfo.getBannerId(), BannersStatusshow.No);
        testBannerRepository
                .updateInternalBannerStoppedByUrlMonitoring(bannerInfo.getShard(), bannerInfo.getBannerId(), true);

        BannerModerationResponse response = createResponseForDefaultObject(Yes);
        var unknownVerdictCountAndSuccess = getReceivingService()
                .processModerationResponses(getShard(), singletonList(response));
        assertEquals(0, (int) unknownVerdictCountAndSuccess.getLeft());
        assertEquals(1, unknownVerdictCountAndSuccess.getRight().size());
        List<InternalBanner> banners = bannerTypedRepository.getStrictlyFullyFilled(bannerInfo.getShard(),
                List.of(bannerInfo.getBannerId()), InternalBanner.class);

        assertEquals(false, banners.get(0).getStatusShow());
    }

}
