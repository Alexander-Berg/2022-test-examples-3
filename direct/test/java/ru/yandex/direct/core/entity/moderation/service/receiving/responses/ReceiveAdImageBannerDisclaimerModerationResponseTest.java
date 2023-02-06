package ru.yandex.direct.core.entity.moderation.service.receiving.responses;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.banner.model.ImageSize;
import ru.yandex.direct.core.entity.banner.model.old.OldImageHashBanner;
import ru.yandex.direct.core.entity.image.model.BannerImageFormat;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationResponse;
import ru.yandex.direct.core.entity.moderation.model.ModerationDecision;
import ru.yandex.direct.core.entity.moderation.service.receiving.AdImageBannerModerationReceivingService;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonDetailed;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestBannerCreativeRepository;
import ru.yandex.direct.core.testing.repository.TestBannerRepository;
import ru.yandex.direct.core.testing.repository.TestImageRepository;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate.SENT;
import static ru.yandex.direct.core.entity.moderation.model.ModerationDecision.Yes;
import static ru.yandex.direct.core.entity.moderation.service.ModerationObjectType.AD_IMAGE;
import static ru.yandex.direct.core.testing.data.TestBanners.activeImageHashBanner;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ReceiveAdImageBannerDisclaimerModerationResponseTest extends ReceiveModerationResponseBaseTest {
    private static final List<Long> DEFAULT_MINUS_REGION = Arrays.asList(4L, 5L, 6L);
    protected static final List<ModerationReasonDetailed> DEFAULT_REASONS = Arrays.asList(
            new ModerationReasonDetailed().withId(2L),
            new ModerationReasonDetailed().withId(3L));

    @Autowired
    private AdImageBannerModerationReceivingService adImageBannerModerationReceivingService;

    @Autowired
    private TestBannerCreativeRepository testBannerCreativeRepository;

    @Autowired
    private TestModerationRepository testModerationRepository;

    @Autowired
    private TestBannerRepository testBannerRepository;

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private TestImageRepository testImageRepository;

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
        adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(clientInfo);

        shard = clientInfo.getShard();

        banner = (OldImageHashBanner) steps.bannerSteps()
                .createBanner(activeImageHashBanner(campaignInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                                .withStatusModerate(SENT),
                        clientInfo
                )
                .getBanner();

        bannerImageFormat = testModerationRepository.addBannerImageFormat(shard, banner.getImage().getImageHash(),
                new ImageSize().withHeight(100).withWidth(100));

        adGroupId = adGroupInfo.getAdGroupId();

        testModerationRepository.createBannerVersion(shard, banner.getId(), 1L);
    }


    private BannerModerationResponse createResponse(OldImageHashBanner bannerInfo, ModerationDecision status) {
        return createResponse(bannerInfo, status, null, 1L);
    }

    private BannerModerationResponse createResponse(OldImageHashBanner bannerInfo, ModerationDecision status,
                                                    @Nullable String language, long version) {
        return createResponse(bannerInfo, status, language, version, emptyMap());
    }

    private BannerModerationResponse createResponse(OldImageHashBanner bannerInfo, ModerationDecision status,
                                                    @Nullable String language, long version,
                                                    Map<String, String> flags) {
        return createResponse(bannerInfo.getId(), AD_IMAGE, status, language, version,
                flags, DEFAULT_MINUS_REGION, clientInfo, DEFAULT_REASONS);
    }


    @Test
    public void advertizedItem_savedInDb() {
        BannerModerationResponse response = createResponse(banner, Yes, "en", 1L);
        response.getResult().setAdvertisedItem("необычный текст");

        var unknownVerdictCountAndSuccess =
                adImageBannerModerationReceivingService.processModerationResponses(shard, singletonList(response));

        assertEquals(0, (int) unknownVerdictCountAndSuccess.getLeft());
        assertEquals(1, unknownVerdictCountAndSuccess.getRight().size());

        String txt = testImageRepository.getImageText(shard, banner.getId());

        assertThat("Текст объекта рекламирования сохранен в БД", txt,
                is("необычный текст"));
    }

    @Test
    public void advertizedItem_removedInDb() {
        BannerModerationResponse response = createResponse(banner, Yes, "en", 1L);
        response.getResult().setAdvertisedItem("необычный текст");

        var unknownVerdictCountAndSuccess =
                adImageBannerModerationReceivingService.processModerationResponses(shard, singletonList(response));

        assertEquals(0, (int) unknownVerdictCountAndSuccess.getLeft());
        assertEquals(1, unknownVerdictCountAndSuccess.getRight().size());

        String txt = testImageRepository.getImageText(shard, banner.getId());

        assertThat("Текст объекта рекламирования сохранен в БД", txt,
                is("необычный текст"));

        BannerModerationResponse response2 = createResponse(banner, Yes, "en", 2L);
        response.getResult().setAdvertisedItem("");

        adImageBannerModerationReceivingService.processModerationResponses(shard, singletonList(response));

        txt = testImageRepository.getImageText(shard, banner.getId());

        assertThat("Текст объекта рекламирования сохранен в БД", txt,
                is(""));

    }


}
