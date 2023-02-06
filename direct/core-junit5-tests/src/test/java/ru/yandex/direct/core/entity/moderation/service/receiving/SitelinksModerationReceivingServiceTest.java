package ru.yandex.direct.core.entity.moderation.service.receiving;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.banner.model.old.StatusSitelinksModerate;
import ru.yandex.direct.core.entity.domain.repository.DomainRepository;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonObjectType;
import ru.yandex.direct.core.entity.moderationreason.repository.ModerationReasonRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
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
import static ru.yandex.direct.core.entity.moderation.service.receiving.ModerationResponseUtil.makeSitelinkSetResponse;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;

@CoreTest
@ExtendWith(SpringExtension.class)
public class SitelinksModerationReceivingServiceTest {

    @Autowired
    private SitelinksModerationReceivingService sitelinksModerationReceivingService;
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
    private TextBannerInfo bannerInfo;
    private Integer shard;

    @BeforeEach
    public void setUp() {
        domain = "test.domain.com";
        modVersion = 1L;
        var clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
        var sitelinksId = steps.sitelinkSetSteps().createDefaultSitelinkSet(clientInfo).getSitelinkSetId();
        var banner = activeTextBanner().withDomain(domain)
                .withSitelinksSetId(sitelinksId).withStatusSitelinksModerate(StatusSitelinksModerate.YES);
        bannerInfo = steps.bannerSteps().createBanner(banner, clientInfo);
        moderationRepository.createSitelinksVersion(shard, bannerInfo.getBannerId(), modVersion);
    }

    @AfterEach
    public void tearDown() {
        testDomainRepository.deleteDomainStat(domain);
    }

    @Test
    public void processModerationResponses_updatesApiDomainStat() {
        var responses = List.of(
                makeSitelinkSetResponse(bannerInfo.getBannerId(), modVersion, Yes, List.of(), null)
        );
        checkModerationResponses(sitelinksModerationReceivingService, responses, domainRepository, domain, shard,
                1L, 0L, 0L);
    }

    @Test
    public void processModerationResponses_updatesApiDomainStat_withDetails() {
        var responses = List.of(
                makeSitelinkSetResponse(bannerInfo.getBannerId(), modVersion, No, REASONS_IDS_RESPONSE,
                        DETAILED_REASONS_RESPONSE)
        );
        checkModerationResponses(sitelinksModerationReceivingService, responses, domainRepository, domain, shard,
                0L, 1L, REASONS_IDS_RESPONSE.size());
        checkResponsesWithDetails(shard, moderationReasonRepository, ModerationReasonObjectType.SITELINKS_SET,
                bannerInfo.getBannerId(), EXPECTED_REASONS_IN_DB);
    }
}
