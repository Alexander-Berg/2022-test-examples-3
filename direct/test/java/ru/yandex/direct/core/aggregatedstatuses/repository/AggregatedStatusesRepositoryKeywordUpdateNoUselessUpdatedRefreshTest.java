package ru.yandex.direct.core.aggregatedstatuses.repository;

import java.time.LocalDateTime;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason;
import ru.yandex.direct.core.entity.aggregatedstatuses.keyword.AggregatedStatusKeywordData;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbschema.ppc.tables.AggrStatusesKeywords;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.dbschema.ppc.tables.AggrStatusesKeywords.AGGR_STATUSES_KEYWORDS;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AggregatedStatusesRepositoryKeywordUpdateNoUselessUpdatedRefreshTest {
    private final AggrStatusesKeywords t = AGGR_STATUSES_KEYWORDS;
    private final AggregatedStatusKeywordData initialStatusData = new AggregatedStatusKeywordData(
            GdSelfStatusEnum.DRAFT,
            GdSelfStatusReason.DRAFT
    );
    private final AggregatedStatusKeywordData nextStatusData = new AggregatedStatusKeywordData(
            GdSelfStatusEnum.DRAFT,
            GdSelfStatusReason.REJECTED_ON_MODERATION // не бывает, но нужно для теста
    );
    private int shard = 1;
    private long keywordId = 123456L;
    private LocalDateTime initialUpdatedTime;
    private LocalDateTime future;

    @Autowired
    private AggregatedStatusesRepository aggregatedStatusesRepository;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Before
    public void prepare() {
        aggregatedStatusesRepository.updateKeywords(shard, null, Map.of(keywordId, initialStatusData));
        initialUpdatedTime = LocalDateTime.now().withNano(0).minusHours(1);
        setUpdated(keywordId, initialUpdatedTime);
        future = initialUpdatedTime.plusHours(1);
    }

    @Test
    public void updateWithoutStatusChangeWontRefreshUpdated() {
        aggregatedStatusesRepository.updateKeywords(shard, null, Map.of(keywordId, initialStatusData));
        assertEquals("Updated didn't changed if status didn't", initialUpdatedTime, getUpdated(keywordId));
    }

    @Test
    public void updateWithStatusChangeDoRefreshUpdated() {
        aggregatedStatusesRepository.updateKeywords(shard, null, Map.of(keywordId, nextStatusData));
        assertThat("Updated do change when status gets updated", getUpdated(keywordId), greaterThan(initialUpdatedTime));
    }


    // Ниже проверяем, что для ситуации когда задана пороговая дата, записи старше которой мы обновляем
    // то мы сбрасываем updated независимо поменялся ли реально status или нет
    // На то, что сама пороговая дата работает есть отдельный тест
    @Test
    public void updateWithoutStatusChangeAndWithTimeThresholdDoRefreshUpdated() {
        aggregatedStatusesRepository.updateKeywords(shard, future, Map.of(keywordId, initialStatusData));
        assertThat("Updated do change when status gets updated (while time threshold)", getUpdated(keywordId),
                greaterThan(initialUpdatedTime));
    }

    @Test
    public void updateWithoutStatusChangeAndTimeThresholdWontRefreshUpdated() {
        aggregatedStatusesRepository.updateKeywords(shard, future, Map.of(keywordId, nextStatusData));
        assertThat("Updated do change when status gets updated (while time threshold)", getUpdated(keywordId),
                greaterThan(initialUpdatedTime));
    }

    private void setUpdated(Long id, LocalDateTime dateTime) {
        dslContextProvider.ppc(shard).update(t)
                .set(t.UPDATED, dateTime)
                .where(t.ID.eq(id)).execute();
    }

    private LocalDateTime getUpdated(Long id) {
        return dslContextProvider.ppc(shard).select(t.UPDATED).from(t).where(t.ID.eq(id)).fetchOne(t.UPDATED);
    }
}
