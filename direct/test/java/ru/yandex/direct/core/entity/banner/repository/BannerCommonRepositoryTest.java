package ru.yandex.direct.core.entity.banner.repository;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.repository.TestBannerRepository;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.RedirectCheckQueueObjectType;
import ru.yandex.direct.dbschema.ppc.tables.records.RedirectCheckQueueRecord;

import static java.time.LocalDateTime.now;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerCommonRepositoryTest {

    @Autowired
    private Steps steps;

    @Autowired
    public AdGroupSteps adGroupSteps;

    @Autowired
    public BannerCommonRepository repoUnderTest;

    @Autowired
    private TestBannerRepository testBannerRepository;

    private int shard;

    @Before
    public void before() {
        AdGroupInfo adGroupInfo = adGroupSteps.createDefaultAdGroup();
        shard = adGroupInfo.getShard();
    }

    @Test
    public void addToRedirectCheckQueue_OneBanner() {
        TextBannerInfo bannerInfo1 = steps.bannerSteps().createActiveTextBanner();

        repoUnderTest.addToRedirectCheckQueue(shard, singletonList(bannerInfo1.getBanner().getId()));

        List<RedirectCheckQueueRecord> records = testBannerRepository
                .getRedirectQueueRecords(shard, singletonList(bannerInfo1.getBannerId()));
        assertThat(records, hasSize(1));

        RedirectCheckQueueRecord record = records.get(0);
        assertThat(record.getObjectId(), is(bannerInfo1.getBannerId()));
        assertThat(record.getObjectType(), is(RedirectCheckQueueObjectType.banner));
        assertThat(record.getTries(), is(0L));
    }

    @Test
    public void addToRedirectCheckQueue_TwoBanners() {
        TextBannerInfo bannerInfo1 = steps.bannerSteps().createActiveTextBanner();
        TextBannerInfo bannerInfo2 = steps.bannerSteps().createActiveTextBanner();
        List<Long> bannerIds = asList(bannerInfo1.getBanner().getId(), bannerInfo2.getBanner().getId());

        repoUnderTest.addToRedirectCheckQueue(shard, bannerIds);

        List<RedirectCheckQueueRecord> records = testBannerRepository
                .getRedirectQueueRecords(shard, asList(bannerInfo1.getBannerId(), bannerInfo2.getBannerId()));
        assertThat(records, hasSize(2));
    }

    @Test
    public void addToRedirectCheckQueue_OnDuplicateReplaceWithNewId_TriesReset() {
        TextBannerInfo bannerInfo = steps.bannerSteps().createActiveTextBanner();
        long bannerId = bannerInfo.getBannerId();

        RedirectCheckQueueRecord beforeQueueRecord = new RedirectCheckQueueRecord();
        beforeQueueRecord.setObjectId(bannerId);
        beforeQueueRecord.setLogtime(now());
        beforeQueueRecord.setTries(5L);

        Long recordId = testBannerRepository.addToRedirectCheckQueue(shard, singletonList(beforeQueueRecord)).get(0);

        repoUnderTest.addToRedirectCheckQueue(shard, singletonList(bannerInfo.getBanner().getId()));

        List<RedirectCheckQueueRecord> savedRecords =
                testBannerRepository.getRedirectQueueRecords(shard, singletonList(bannerId));
        assertThat(savedRecords, hasSize(1));

        RedirectCheckQueueRecord savedRecord = savedRecords.get(0);
        assertThat(savedRecord.getId(), is(not(recordId)));
        assertThat(savedRecord.getObjectId(), is(bannerId));
        assertThat(savedRecord.getTries(), is(0L));
    }

}
