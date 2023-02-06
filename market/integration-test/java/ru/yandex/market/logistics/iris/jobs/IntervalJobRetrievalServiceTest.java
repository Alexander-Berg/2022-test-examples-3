package ru.yandex.market.logistics.iris.jobs;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.core.domain.source.SourceType;
import ru.yandex.market.logistics.iris.entity.EmbeddableSource;
import ru.yandex.market.logistics.iris.entity.Interval;
import ru.yandex.market.logistics.iris.jobs.model.QueueType;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.management.entity.type.PartnerStatus.ACTIVE;

public class IntervalJobRetrievalServiceTest extends AbstractContextualTest {
    @Autowired
    private IntervalJobRetrievalService intervalJobRetrievalService;

    @Test
    @DatabaseSetup("/fixtures/expected/interval_job_retrieval_service/1.xml")
    public void getActiveIntervalJobs() {
        when(lmsClient.searchPartners(createFilter())).thenReturn(ImmutableList.of(
            PartnerResponse.newBuilder().id(100L).korobyteSyncEnabled(true).status(ACTIVE).build(),
            PartnerResponse.newBuilder().id(200L).korobyteSyncEnabled(false).status(ACTIVE).build(),
            PartnerResponse.newBuilder().id(300L).korobyteSyncEnabled(true).status(ACTIVE).build(),
            PartnerResponse.newBuilder().id(400L).korobyteSyncEnabled(true).status(ACTIVE).build(),
            PartnerResponse.newBuilder().id(500L).korobyteSyncEnabled(true).status(ACTIVE).build()
        ));

        assertions().assertThat(intervalJobRetrievalService.getActiveIntervalJobs(2))
            .usingElementComparatorIgnoringFields("id")
            .contains(
                intervalOf("1", SourceType.CONTENT, 2),
                intervalOf("100", SourceType.WAREHOUSE, 2),
                intervalOf("500", SourceType.WAREHOUSE, 2)
            );

        assertions().assertThat(intervalJobRetrievalService.getActiveIntervalJobs(3)).isEmpty();

        assertions().assertThat(intervalJobRetrievalService.getActiveIntervalJobs(5))
            .usingElementComparatorIgnoringFields("id")
            .contains(intervalOf("300", SourceType.WAREHOUSE, 5));

        assertions().assertThat(intervalJobRetrievalService.getActiveIntervalJobs(7)).isEmpty();
    }

    @Nonnull
    private SearchPartnerFilter createFilter() {
        return SearchPartnerFilter
            .builder()
            .setTypes(ImmutableSet.of(PartnerType.FULFILLMENT, PartnerType.DROPSHIP, PartnerType.SUPPLIER))
            .build();
    }

    private Interval intervalOf(String sourceId, SourceType sourceType, int intervalMin) {
        Interval interval = new Interval();
        interval.setActive(true);
        interval.setSource(new EmbeddableSource(sourceId, sourceType));
        interval.setSyncJobName(QueueType.REFERENCE_SYNC);
        interval.setInterval(intervalMin);
        return interval;
    }
}
