package ru.yandex.direct.core.aggregatedstatuses.repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason;
import ru.yandex.direct.core.entity.aggregatedstatuses.campaign.AggregatedStatusCampaignData;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbschema.ppc.tables.AggrStatusesCampaigns;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.dbschema.ppc.tables.AggrStatusesCampaigns.AGGR_STATUSES_CAMPAIGNS;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AggregatedStatusesRepositoryUpdateNoUselessUpdatedRefreshTest {
    private final AggrStatusesCampaigns t = AGGR_STATUSES_CAMPAIGNS;
    private final AggregatedStatusCampaignData initialStatusData = new AggregatedStatusCampaignData(
            Collections.emptyList(), null,
            GdSelfStatusEnum.DRAFT,
            GdSelfStatusReason.DRAFT
    );
    private final AggregatedStatusCampaignData nextStatusData = new AggregatedStatusCampaignData(
            Collections.emptyList(), null,
            GdSelfStatusEnum.DRAFT,
            GdSelfStatusReason.REJECTED_ON_MODERATION // не бывает, но нужно для теста
    );
    private int shard = 1;
    private long campaignId = 123456L;
    private LocalDateTime initialUpdatedTime;
    private LocalDateTime future;

    @Autowired
    private AggregatedStatusesRepository aggregatedStatusesRepository;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Before
    public void prepare() {
        aggregatedStatusesRepository.updateCampaigns(shard, null, Map.of(campaignId, initialStatusData));
        initialUpdatedTime = LocalDateTime.now().withNano(0).minusHours(1);
        setUpdated(campaignId, initialUpdatedTime);
        future = initialUpdatedTime.plusHours(1);
    }

    @Test
    public void updateWithoutStatusChangeWontRefreshUpdated() {
        aggregatedStatusesRepository.updateCampaigns(shard, null, Map.of(campaignId, initialStatusData));
        assertEquals("Updated didn't changed if status didn't", initialUpdatedTime, getUpdated(campaignId));
    }

    @Test
    public void updateWithStatusChangeDoRefreshUpdated() {
        aggregatedStatusesRepository.updateCampaigns(shard, null, Map.of(campaignId, nextStatusData));
        assertThat("Updated do change when status gets updated", getUpdated(campaignId), greaterThan(initialUpdatedTime));
    }


    // Ниже проверяем, что для ситуации когда задана пороговая дата, записи старше которой мы обновляем
    // то мы сбрасываем updated независимо поменялся ли реально status или нет
    // На то, что сама пороговая дата работает есть отдельный тест
    @Test
    public void updateWithoutStatusChangeAndWithTimeThresholdDoRefreshUpdated() {
        aggregatedStatusesRepository.updateCampaigns(shard, future, Map.of(campaignId, initialStatusData));
        assertThat("Updated do change when status gets updated (while time threshold)", getUpdated(campaignId),
                greaterThan(initialUpdatedTime));
    }

    @Test
    public void updateWithoutStatusChangeAndTimeThresholdWontRefreshUpdated() {
        aggregatedStatusesRepository.updateCampaigns(shard, future, Map.of(campaignId, nextStatusData));
        assertThat("Updated do change when status gets updated (while time threshold)", getUpdated(campaignId),
                greaterThan(initialUpdatedTime));
    }

    private void setUpdated(Long id, LocalDateTime dateTime) {
        dslContextProvider.ppc(shard).update(t)
                .set(t.UPDATED, dateTime)
                .where(t.CID.eq(id)).execute();
    }

    private LocalDateTime getUpdated(Long id) {
        return dslContextProvider.ppc(shard).select(t.UPDATED).from(t).where(t.CID.eq(id)).fetchOne(t.UPDATED);
    }
}
