package ru.yandex.direct.core.entity.moderation.service.receiving;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.banner.model.ImageType;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerImage;
import ru.yandex.direct.core.entity.domain.repository.DomainRepository;
import ru.yandex.direct.core.entity.image.model.BannerImageFromPool;
import ru.yandex.direct.core.entity.image.model.BannerImageSource;
import ru.yandex.direct.core.entity.image.repository.BannerImagePoolRepository;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonObjectType;
import ru.yandex.direct.core.entity.moderationreason.repository.ModerationReasonRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.repository.TestBannerImageFormatRepository;
import ru.yandex.direct.core.testing.repository.TestDomainRepository;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;

import static org.apache.commons.lang3.RandomStringUtils.random;
import static ru.yandex.direct.core.entity.moderation.model.ModerationDecision.No;
import static ru.yandex.direct.core.entity.moderation.model.ModerationDecision.Yes;
import static ru.yandex.direct.core.entity.moderation.service.receiving.ModerationReceivingServiceUtil.checkModerationResponses;
import static ru.yandex.direct.core.entity.moderation.service.receiving.ModerationReceivingServiceUtil.checkResponsesWithDetails;
import static ru.yandex.direct.core.entity.moderation.service.receiving.ModerationResponseUtil.DETAILED_REASONS_RESPONSE;
import static ru.yandex.direct.core.entity.moderation.service.receiving.ModerationResponseUtil.EXPECTED_REASONS_IN_DB;
import static ru.yandex.direct.core.entity.moderation.service.receiving.ModerationResponseUtil.REASONS_IDS_RESPONSE;
import static ru.yandex.direct.core.entity.moderation.service.receiving.ModerationResponseUtil.makeImageResponse;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.defaultBannerImage;

@CoreTest
@ExtendWith(SpringExtension.class)
public class ImageModerationReceivingServiceTest {
    @Autowired
    private ImageModerationReceivingService imageModerationReceivingService;
    @Autowired
    private TestBannerImageFormatRepository testBannerImageFormatRepository;
    @Autowired
    private BannerImagePoolRepository bannerImagePoolRepository;
    @Autowired
    private TestModerationRepository moderationRepository;
    @Autowired
    private TestDomainRepository testDomainRepository;
    @Autowired
    private DomainRepository domainRepository;
    @Autowired
    private Steps steps;
    @Autowired
    private ModerationReasonRepository moderationReasonRepository;

    private ClientInfo clientInfo;
    private int shard;
    private String domain;
    private long modVersion;
    private TextBannerInfo bannerInfo;

    @BeforeEach
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
        domain = "test.domain.com";
        modVersion = 1L;
        var banner = activeTextBanner().withDomain(domain)
                .withBannerImage(createImage(shard, clientInfo.getClientId()));
        bannerInfo = steps.bannerSteps().createBanner(banner, clientInfo);
        moderationRepository.createBannerImageVersion(shard, bannerInfo.getBannerId(), modVersion);
    }

    @AfterEach
    public void tearDown() {
        testDomainRepository.deleteDomainStat(domain);
    }

    private OldBannerImage createImage(int shard, ClientId clientId) {
        String imageHash = random(22, true, true);
        ImageType imageType = ImageType.WIDE;
        BannerImageFromPool bannerImageFromPool = new BannerImageFromPool()
                .withImageHash(imageHash)
                .withCreateTime(LocalDateTime.now())
                .withSource(BannerImageSource.DIRECT)
                .withClientId(clientId.asLong());

        bannerImagePoolRepository.addOrUpdateImagesToPool(shard, clientId, List.of(bannerImageFromPool));
        testBannerImageFormatRepository.create(shard, imageHash, ImageType.toSource(imageType));

        return defaultBannerImage(null, imageHash).withImageType(imageType);
    }

    @Test
    public void processModerationResponses_updatesApiDomainStat() {
        var responses = List.of(
                makeImageResponse(bannerInfo.getBannerId(), modVersion, Yes, List.of(), null)
        );
        checkModerationResponses(imageModerationReceivingService, responses, domainRepository, domain, shard,
                1L, 0L, 0L);
    }

    @Test
    public void processModerationResponses_updatesApiDomainStat_withDetails() {
        var responses = List.of(
                makeImageResponse(bannerInfo.getBannerId(), modVersion, No, REASONS_IDS_RESPONSE,
                        DETAILED_REASONS_RESPONSE)
        );
        checkModerationResponses(imageModerationReceivingService, responses, domainRepository, domain, shard,
                0L, 1L, REASONS_IDS_RESPONSE.size());
        checkResponsesWithDetails(shard, moderationReasonRepository, ModerationReasonObjectType.IMAGE,
                bannerInfo.getBannerId(), EXPECTED_REASONS_IN_DB);
    }
}
