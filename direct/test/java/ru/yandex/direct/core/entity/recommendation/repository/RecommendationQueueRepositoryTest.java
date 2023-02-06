package ru.yandex.direct.core.entity.recommendation.repository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.RandomUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.recommendation.model.RecommendationQueueInfo;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static org.junit.Assert.assertEquals;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RecommendationQueueRepositoryTest {
    private static final int SHARD = 1;
    private static final int PAR_ID = 11;
    private static final int ANOTHER_PAR_ID = 22;

    @Autowired
    private RecommendationQueueRepository recommendationQueueRepository;

    private Set<RecommendationQueueInfo> recommendations;
    private RecommendationQueueInfo recOneHour;
    private RecommendationQueueInfo recTwoHours;
    private RecommendationQueueInfo recLocked;
    private RecommendationQueueInfo recLockedByAnother;
    private List<Long> ids;

    @Before
    public void before() {

        recOneHour = getEmptyRecommendation()
                .withClientId(12L)
                .withSecTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).minusHours(1));

        recTwoHours = getEmptyRecommendation()
                .withCampaignId(34L)
                .withSecTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).minusHours(2));

        recLocked = getEmptyRecommendation()
                .withAdGroupId(56L)
                .withParId(PAR_ID);

        recLockedByAnother = getEmptyRecommendation()
                .withBannerId(78L)
                .withParId(ANOTHER_PAR_ID);

        recommendations = asSet(recOneHour, recTwoHours, recLocked, recLockedByAnother);
        ids = recommendations.stream().map(RecommendationQueueInfo::getId).collect(toList());

        recommendationQueueRepository.add(SHARD, recommendations);
    }

    @After
    public void after() {
        recommendationQueueRepository.delete(SHARD, ids);
    }

    private RecommendationQueueInfo getEmptyRecommendation() {
        return new RecommendationQueueInfo()
                .withId(RandomUtils.nextLong(1, Integer.MAX_VALUE))
                .withClientId(0L)
                .withType(0L)
                .withCampaignId(0L)
                .withAdGroupId(0L)
                .withBannerId(0L)
                .withUserKey1("")
                .withUserKey2("")
                .withUserKey3("")
                .withTimestamp(0L)
                .withSecTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
                .withQueueTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
                .withUid(0L)
                .withParId(null);
    }

    @Test
    @Description("Получение всех рекомендаций")
    public void get() {
        Set<RecommendationQueueInfo> set = new HashSet<>(recommendationQueueRepository.get(SHARD, ids));
        assertEquals(recommendations, set);
    }

    @Test
    @Description("Получение порции рекомендаций с правильной сортировкой")
    public void getPortion() {
        List<RecommendationQueueInfo> list = recommendationQueueRepository.getPortion(SHARD, PAR_ID);
        assertEquals(asList(recLocked, recTwoHours, recOneHour), list);
    }

    @Test
    @Description("Получение заблокированных воркером")
    public void getLocked() {
        Set<RecommendationQueueInfo> set = new HashSet<>(recommendationQueueRepository.getLocked(SHARD, PAR_ID));
        assertEquals(singleton(recLocked), set);
    }

    @Test
    @Description("Блокировка под текущего воркера")
    public void lock() {
        recommendationQueueRepository.lock(SHARD, PAR_ID, singleton(recOneHour.getId()));
        Set<RecommendationQueueInfo> set = new HashSet<>(recommendationQueueRepository.get(SHARD, ids));
        assertEquals(asSet(recOneHour.withParId(PAR_ID), recTwoHours, recLocked, recLockedByAnother), set);
    }

    @Test
    @Description("Удаление рекомендаций")
    public void delete() {
        recommendationQueueRepository.delete(SHARD, asList(recOneHour.getId(), recTwoHours.getId()));
        Set<RecommendationQueueInfo> set = new HashSet<>(recommendationQueueRepository.get(SHARD, ids));
        assertEquals(asSet(recLocked, recLockedByAnother), set);
    }
}
