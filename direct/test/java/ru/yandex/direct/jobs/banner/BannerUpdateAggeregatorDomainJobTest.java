package ru.yandex.direct.jobs.banner;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.bs.resync.queue.model.BsResyncPriority;
import ru.yandex.direct.core.entity.bs.resync.queue.model.BsResyncQueueInfo;
import ru.yandex.direct.core.entity.bs.resync.queue.service.BsResyncService;
import ru.yandex.direct.core.entity.domain.model.BannerUpdateAggeregatorDomainParams;
import ru.yandex.direct.core.entity.domain.repository.AggregatorDomainsRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbqueue.repository.DbQueueRepository;
import ru.yandex.direct.dbqueue.service.DbQueueService;
import ru.yandex.direct.dbqueue.steps.DbQueueSteps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.jobs.banner.service.BannerUpdateAggeregatorDomainService;
import ru.yandex.direct.jobs.configuration.JobsTest;

import static com.google.common.base.Preconditions.checkState;
import static ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes.UPDATE_AGGREGATOR_DOMAINS;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;

@JobsTest
@ExtendWith(SpringExtension.class)
class BannerUpdateAggeregatorDomainJobTest {

    @Autowired
    private Steps steps;
    @Autowired
    private DbQueueSteps dbQueueSteps;
    @Autowired
    private DbQueueService dbQueueService;
    @Autowired
    private DbQueueRepository dbQueueRepository;
    @Autowired
    private BannerUpdateAggeregatorDomainService bannerUpdateAggeregatorDomainService;
    @Autowired
    private AggregatorDomainsRepository aggregatorDomainsRepository;
    @Autowired
    private BsResyncService bsResyncService;

    private BannerUpdateAggeregatorDomainJob bannerUpdateAggeregatorDomainJob;

    private AdGroupInfo adGroupInfo;

    @BeforeEach
    void before() {
        //создаём клиента, чтобы занять clientId = 1 и баннер создался для второго клиента. Да, это жирный костыль^^
        steps.clientSteps().createDefaultClient();
        adGroupInfo = steps.keywordSteps().createDefaultKeyword().getAdGroupInfo();

        dbQueueSteps.registerJobType(UPDATE_AGGREGATOR_DOMAINS);
        dbQueueSteps.clearQueue(UPDATE_AGGREGATOR_DOMAINS);
        bannerUpdateAggeregatorDomainJob = new BannerUpdateAggeregatorDomainJob(adGroupInfo.getShard(),
                dbQueueService, bannerUpdateAggeregatorDomainService);
    }

    @Test
    void smokeTest() {
        String domain = "maps.yandex.ru";
        OldTextBanner banner1 = activeTextBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                .withHref("https://yandex.ru/maps/org/1789193057").withDomain(domain)
                .withReverseDomain(StringUtils.reverse(domain));
        OldTextBanner banner2 = activeTextBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                .withHref("https://yandex.ru/maps/org/1789193057").withDomain("")
                .withReverseDomain(StringUtils.reverse(""));

        steps.bannerSteps().createBanner(banner1, adGroupInfo);
        steps.bannerSteps().createBanner(banner2, adGroupInfo);

        var params = new BannerUpdateAggeregatorDomainParams();
        params.setDomain("maps.yandex.ru");
        Long jobId = dbQueueRepository
                .insertJob(adGroupInfo.getShard(), UPDATE_AGGREGATOR_DOMAINS, ClientId.fromLong(0L), 0L, params)
                .getId();

        executeJob();

        var job = dbQueueRepository.findJobById(adGroupInfo.getShard(), UPDATE_AGGREGATOR_DOMAINS, jobId);
        checkState(job != null);
        Map<Long, String> domains = aggregatorDomainsRepository.getAggregatorDomains(adGroupInfo.getShard(),
                List.of(banner1.getId(), banner2.getId()));
        BsResyncQueueInfo bsResyncQueue =
                bsResyncService.getBsResyncItemsByCampaignIds(List.of(adGroupInfo.getCampaignId())).stream()
                        .findFirst().orElse(null);

        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(domains).containsEntry(banner1.getId(), "1789193057.maps.yandex.ru");
        sa.assertThat(domains).doesNotContainKey(banner2.getId());
        sa.assertThat(bsResyncQueue).matches(bsResyncQueueInfo ->
                adGroupInfo.getCampaignId().equals(bsResyncQueueInfo.getCampaignId()) &&
                        banner1.getId().equals(bsResyncQueueInfo.getBannerId()) &&
                        BsResyncPriority.SYNC_AGGREGATOR_DOMAINS.value() == bsResyncQueueInfo.getPriority());

        sa.assertAll();
    }

    private void executeJob() {
        Assertions.assertThatCode(() -> bannerUpdateAggeregatorDomainJob.execute())
                .doesNotThrowAnyException();
    }
}
