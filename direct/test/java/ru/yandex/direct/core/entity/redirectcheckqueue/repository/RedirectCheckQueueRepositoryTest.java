package ru.yandex.direct.core.entity.redirectcheckqueue.repository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.redirectcheckqueue.model.RedirectCheckQueueDomainStat;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.repository.TestBannerRepository;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.BannerSteps;
import ru.yandex.direct.dbschema.ppc.tables.records.RedirectCheckQueueRecord;

import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static ru.yandex.direct.core.testing.data.TestBanners.defaultTextBanner;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RedirectCheckQueueRepositoryTest {
    @Autowired
    private AdGroupSteps adGroupSteps;
    @Autowired
    private BannerSteps bannerSteps;
    @Autowired
    private RedirectCheckQueueRepository repository;
    @Autowired
    private TestBannerRepository testBannerRepository;

    @Test
    public void testCheckQueue() {
        String randomDomain = UUID.randomUUID().toString();
        AdGroupInfo activeTextAdGroup = adGroupSteps.createActiveTextAdGroup();
        TextBannerInfo bannerOne = bannerSteps
                .createBanner(defaultTextBanner(activeTextAdGroup.getCampaignId(), activeTextAdGroup.getAdGroupId())
                                .withDomain(randomDomain),
                        activeTextAdGroup);
        TextBannerInfo bannerTwo = bannerSteps
                .createBanner(defaultTextBanner(activeTextAdGroup.getCampaignId(), activeTextAdGroup.getAdGroupId())
                                .withDomain(randomDomain),
                        activeTextAdGroup);

        repository.pushBannersIntoQueue(activeTextAdGroup.getShard(),
                Arrays.asList(bannerOne.getBannerId(), bannerTwo.getBannerId()));

        List<RedirectCheckQueueDomainStat> domainCheckStat = repository.getDomainCheckStat(bannerOne.getShard());
        assertThat(domainCheckStat)
                .size().isGreaterThanOrEqualTo(1);

        RedirectCheckQueueDomainStat ourStat = domainCheckStat.stream()
                .filter(r -> bannerOne.getBanner().getDomain().equals(r.getDomain()))
                .findFirst().orElse(null);
        assertThat(ourStat)
                .isNotNull();

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(ourStat.getBannersNum())
                .isEqualTo(2);
        soft.assertThat(ourStat.getCampaignsNum())
                .isEqualTo(1);
        soft.assertThat(ourStat.getOldestEntryAge())
                .isBeforeOrEqualTo(LocalDateTime.now());
        soft.assertAll();
    }

    @Test
    public void testMarkTasksFailed() {
        AdGroupInfo adGroupInfo = adGroupSteps.createActiveTextAdGroup();
        int shard = adGroupInfo.getShard();

        List<RedirectCheckQueueRecord> recordsBeforeUpdate = new ArrayList<>();
        for (long tries = 0; tries <= 2; tries++) {
            Long bannerId = bannerSteps.createActiveTextBanner(adGroupInfo).getBannerId();

            RedirectCheckQueueRecord record = new RedirectCheckQueueRecord();
            record.setObjectId(bannerId);
            record.setTries(tries);
            record.setLogtime(now());

            recordsBeforeUpdate.add(record);
        }
        List<Long> bannerIds = mapList(recordsBeforeUpdate, RedirectCheckQueueRecord::getObjectId);
        List<Long> recordIds = testBannerRepository.addToRedirectCheckQueue(shard, recordsBeforeUpdate);

        repository.markTasksFailed(shard, recordIds);

        List<RedirectCheckQueueRecord> recordsAfterUpdate = testBannerRepository.getRedirectQueueRecords(shard,
                bannerIds);
        Map<Long, RedirectCheckQueueRecord> recordByBannerId = listToMap(recordsAfterUpdate,
                RedirectCheckQueueRecord::getObjectId);
        assertThat(recordByBannerId).containsOnlyKeys(bannerIds.get(0), bannerIds.get(1));

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(recordByBannerId.get(bannerIds.get(0)).getTries())
                .isEqualTo(1L);
        soft.assertThat(recordByBannerId.get(bannerIds.get(0)).getLogtime())
                .isCloseTo(LocalDateTime.now().plusHours(2), within(2, ChronoUnit.MINUTES));
        soft.assertThat(recordByBannerId.get(bannerIds.get(1)).getTries())
                .isEqualTo(2L);
        soft.assertThat(recordByBannerId.get(bannerIds.get(1)).getLogtime())
                .isCloseTo(LocalDateTime.now().plusHours(24), within(2, ChronoUnit.MINUTES));
        soft.assertAll();
    }
}
