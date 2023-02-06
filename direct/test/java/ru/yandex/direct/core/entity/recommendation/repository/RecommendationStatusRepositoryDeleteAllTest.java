package ru.yandex.direct.core.entity.recommendation.repository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.recommendation.model.RecommendationStatusInfo;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.repository.TestRecommendationStatusRepository;
import ru.yandex.qatools.allure.annotations.Description;

import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.recommendation.repository.RecommendationStatusRepository.ACTUALITY_DAYS_LIMIT;
import static ru.yandex.direct.core.entity.recommendation.repository.RecommendationStatusRepository.NUMBER_OF_ROWS;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RecommendationStatusRepositoryDeleteAllTest {
    private static final int SHARD = 1;

    @Autowired
    private RecommendationStatusRepository recommendationStatusRepository;
    @Autowired
    private TestRecommendationStatusRepository testRecommendationStatusRepository;

    private long timestamp;

    @Before
    public void before() {
        timestamp = LocalDateTime.now().minusDays(ACTUALITY_DAYS_LIMIT).toEpochSecond(ZoneOffset.UTC);
    }

    @Test
    @Description("Удаление всех старых статусов рекомендаций")
    public void deleteAllOld() {
        Set<RecommendationStatusInfo> recommendations
                = new HashSet<>();
        int rows = NUMBER_OF_ROWS + 10;
        for (int i = 0; i < rows; i++) {
            recommendations.add(new RecommendationStatusInfo()
                    .withClientId(12L)
                    .withType(34L)
                    .withCampaignId(56L)
                    .withAdGroupId(78L)
                    .withBannerId(90L + i)
                    .withUserKey1("")
                    .withUserKey2("")
                    .withUserKey3("")
                    .withTimestamp(timestamp));
        }
        recommendationStatusRepository.add(SHARD, recommendations);
        recommendationStatusRepository.deleteOlderThan(SHARD, timestamp);
        assertThat(testRecommendationStatusRepository.getOld(SHARD, timestamp), empty());
    }
}
