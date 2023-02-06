package ru.yandex.direct.core.entity.recommendation.repository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.recommendation.model.RecommendationOnlineInfo;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.recommendation.repository.RecommendationStatusRepository.ACTUALITY_DAYS_LIMIT;
import static ru.yandex.direct.utils.DateTimeUtils.fromEpochSeconds;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RecommendationOnlineRepositoryTest {
    private static final int SHARD = 1;

    @Autowired
    private RecommendationOnlineRepository recommendationOnlineRepository;

    private RecommendationOnlineInfo recommendation;
    private long timestamp;

    @Before
    public void before() {
        timestamp = LocalDateTime.now().minusDays(ACTUALITY_DAYS_LIMIT).toEpochSecond(ZoneOffset.UTC);
        recommendation = new RecommendationOnlineInfo()
                .withClientId(12L)
                .withType(34L)
                .withCampaignId(56L)
                .withAdGroupId(78L)
                .withBannerId(90L)
                .withUserKey1("")
                .withUserKey2("")
                .withUserKey3("")
                .withTimestamp(timestamp);

        recommendationOnlineRepository.add(SHARD, singleton(recommendation));
    }

    @After
    public void after() {
        recommendationOnlineRepository.delete(SHARD, singleton(recommendation));
    }

    @Test
    @Description("Получение рекомендации с пустым статусом")
    public void get() {
        assertEquals(singletonList(recommendation),
                recommendationOnlineRepository.get(SHARD, singleton(recommendation)));
    }

    @Test
    @Description("Удаление рекомендации")
    public void delete() {
        recommendationOnlineRepository.delete(SHARD, singleton(recommendation));
        assertThat(recommendationOnlineRepository.get(SHARD, singleton(recommendation)), empty());
    }

    @Test
    @Description("Удаление старой рекомендации")
    public void deleteOld() {
        recommendationOnlineRepository.deleteOlderThan(SHARD, timestamp);
        assertThat(recommendationOnlineRepository.get(SHARD, singleton(recommendation)), empty());
    }

    @Test
    @Description("Попытка удалить свежую рекомендацию")
    public void deleteOld_NewRecomendation() {
        recommendationOnlineRepository
                .deleteOlderThan(SHARD, fromEpochSeconds(timestamp).minusDays(1).toEpochSecond(ZoneOffset.UTC));
        assertEquals(singletonList(recommendation),
                recommendationOnlineRepository.get(SHARD, singleton(recommendation)));
    }
}
