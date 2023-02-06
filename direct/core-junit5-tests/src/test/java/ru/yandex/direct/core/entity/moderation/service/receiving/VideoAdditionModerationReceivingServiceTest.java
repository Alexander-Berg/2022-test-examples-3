package ru.yandex.direct.core.entity.moderation.service.receiving;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.banner.model.old.OldBannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.domain.repository.DomainRepository;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonObjectType;
import ru.yandex.direct.core.entity.moderationreason.repository.ModerationReasonRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.repository.TestDomainRepository;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;

import static ru.yandex.direct.core.entity.moderation.model.ModerationDecision.No;
import static ru.yandex.direct.core.entity.moderation.model.ModerationDecision.Yes;
import static ru.yandex.direct.core.entity.moderation.service.receiving.ModerationReceivingServiceUtil.checkModerationResponses;
import static ru.yandex.direct.core.entity.moderation.service.receiving.ModerationReceivingServiceUtil.checkResponsesWithDetails;
import static ru.yandex.direct.core.entity.moderation.service.receiving.ModerationResponseUtil.DETAILED_REASONS_RESPONSE;
import static ru.yandex.direct.core.entity.moderation.service.receiving.ModerationResponseUtil.EXPECTED_REASONS_IN_DB;
import static ru.yandex.direct.core.entity.moderation.service.receiving.ModerationResponseUtil.REASONS_IDS_RESPONSE;
import static ru.yandex.direct.core.entity.moderation.service.receiving.ModerationResponseUtil.makeVideoAdditionResponse;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;

@CoreTest
@ExtendWith(SpringExtension.class)
public class VideoAdditionModerationReceivingServiceTest {
        @Autowired
        private VideoAdditionModerationReceivingService moderationReceivingService;
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

        private String domain;
        private long modVersion;
        private Long creativeId;
        private TextBannerInfo bannerInfo;
        private Integer shard;

        @BeforeEach
        public void setUp() {
            ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
            domain = "test.domain.com";
            modVersion = 1L;
            creativeId = steps.creativeSteps().getNextCreativeId();
            steps.creativeSteps().addDefaultVideoAdditionCreative(clientInfo, creativeId);

            bannerInfo = steps.bannerSteps().createActiveTextBanner(activeTextBanner()
                    .withCreativeId(creativeId)
                    .withCreativeStatusModerate(OldBannerCreativeStatusModerate.YES)
                    .withDomain(domain));
            shard = bannerInfo.getShard();
            moderationRepository.createBannerCreativeVersion(shard, bannerInfo.getBannerId(), creativeId, modVersion);
        }

        @AfterEach
        public void tearDown() {
            testDomainRepository.deleteDomainStat(domain);
        }

        @Test
        public void processModerationResponses_updatesApiDomainStat() {
            var responses = List.of(
                    makeVideoAdditionResponse(bannerInfo.getBannerId(), modVersion, creativeId, Yes, List.of(), null)
            );
            checkModerationResponses(moderationReceivingService, responses, domainRepository, domain, shard,
                    1L, 0L, 0L);
        }

        @Test
        public void processModerationResponses_updatesApiDomainStat_withDetails() {
            var responses = List.of(
                    makeVideoAdditionResponse(bannerInfo.getBannerId(), modVersion, creativeId, No, REASONS_IDS_RESPONSE,
                            DETAILED_REASONS_RESPONSE)
            );
            checkModerationResponses(moderationReceivingService, responses, domainRepository, domain, shard,
                    0L, 1L, REASONS_IDS_RESPONSE.size());
            checkResponsesWithDetails(shard, moderationReasonRepository, ModerationReasonObjectType.VIDEO_ADDITION,
                    bannerInfo.getBannerId(), EXPECTED_REASONS_IN_DB);
        }
}
