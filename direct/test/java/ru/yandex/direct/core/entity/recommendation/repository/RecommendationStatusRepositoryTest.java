package ru.yandex.direct.core.entity.recommendation.repository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.recommendation.model.RecommendationStatus;
import ru.yandex.direct.core.entity.recommendation.model.RecommendationStatusInfo;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.recommendation.repository.RecommendationStatusRepository.ACTUALITY_DAYS_LIMIT;
import static ru.yandex.direct.utils.DateTimeUtils.fromEpochSeconds;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RecommendationStatusRepositoryTest {
    private static final int SHARD = 1;

    @Autowired
    private RecommendationStatusRepository recommendationStatusRepository;

    private RecommendationStatusInfo recommendation;
    private long timestamp;

    @Before
    public void before() {
        timestamp = LocalDateTime.now().minusDays(ACTUALITY_DAYS_LIMIT).toEpochSecond(ZoneOffset.UTC);
        recommendation = new RecommendationStatusInfo()
                .withClientId(12L)
                .withType(34L)
                .withCampaignId(56L)
                .withAdGroupId(78L)
                .withBannerId(90L)
                .withUserKey1("")
                .withUserKey2("")
                .withUserKey3("")
                .withTimestamp(timestamp);

        recommendationStatusRepository.add(SHARD, singleton(recommendation));
    }

    @After
    public void after() {
        recommendationStatusRepository.delete(SHARD, singleton(recommendation));
    }

    @Test
    @Description("Получение рекомендации с пустым статусом")
    public void get() {
        assertEquals(singleton(recommendation),
                recommendationStatusRepository.get(SHARD, singleton(recommendation)));
    }

    @Test
    @Description("Обновление статуса рекомендации")
    public void update() {
        recommendation.setStatus(RecommendationStatus.CANCELLED);
        recommendationStatusRepository.update(SHARD, recommendation);
        assertEquals(singleton(recommendation),
                recommendationStatusRepository.get(SHARD, singleton(recommendation)));
    }

    @Test
    @Description("Удаление статуса рекомендации")
    public void delete() {
        recommendationStatusRepository.delete(SHARD, singleton(recommendation));
        assertThat(recommendationStatusRepository.get(SHARD, singleton(recommendation)), empty());
    }

    @Test
    @Description("Удаление статуса старой рекомендации")
    public void deleteOld() {
        recommendationStatusRepository.deleteOlderThan(SHARD, timestamp);
        assertThat(recommendationStatusRepository.get(SHARD, singleton(recommendation)), empty());
    }

    @Test
    @Description("Попытка удалить статус свежей рекомендации")
    public void deleteOld_NewRecomendation() {
        recommendationStatusRepository
                .deleteOlderThan(SHARD, fromEpochSeconds(timestamp).minusDays(1).toEpochSecond(ZoneOffset.UTC));
        assertEquals(singleton(recommendation),
                recommendationStatusRepository.get(SHARD, singleton(recommendation)));
    }
}
