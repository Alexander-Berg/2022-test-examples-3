package ru.yandex.market.mbi.partnersearch.quartz;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.partnersearch.AbstractFunctionalTest;
import ru.yandex.market.mbi.partnersearch.data.elastic.ElasticService;
import ru.yandex.market.mbi.partnersearch.data.elastic.SearchEntity;
import ru.yandex.market.mbi.partnersearch.data.yt.PartnerSummaryService;
import ru.yandex.market.mbi.partnersearch.quartz.task.RecalculateWeightsExecutor;

/**
 * Тесты для {@link RecalculateWeightsExecutor}
 */
@DbUnitDataSet(before = "RecalculateWeightsExecutorTest.csv")
public class RecalculateWeightsExecutorTest extends AbstractFunctionalTest {

    @Autowired
    private RecalculateWeightsExecutor recalculateWeightsExecutor;

    @Autowired
    private ElasticService elasticService;

    @Autowired
    private PartnerSummaryService partnerSummaryService;

    @Test
    void testRecalculate() throws IOException {
        Mockito.when(partnerSummaryService.getMonthlyGmv(Mockito.anyCollection()))
                .then((Answer<Map<Long, Double>>) invocation -> Map.of(100L, 123.5d, 200L, 333d));
        Mockito.when(elasticService.getByPartnerIds(Mockito.anyCollection()))
                .then((Answer<List<SearchEntity>>) invocation -> {
                    SearchEntity searchEntity1 = new SearchEntity(100L, 999L, "test");
                    SearchEntity searchEntity2 = new SearchEntity(200L, 999L, "test");
                    return List.of(searchEntity1, searchEntity2);
                });

        recalculateWeightsExecutor.doJob(null);
        ArgumentCaptor<SearchEntity> captor = ArgumentCaptor.forClass(SearchEntity.class);
        Mockito.verify(elasticService, Mockito.times(2)).updateSearchEntity(captor.capture());
        List<SearchEntity> res = captor.getAllValues();
        Assertions.assertThat(res).hasSize(2);
        Assertions.assertThat(res.stream().filter(p -> p.getPartnerId() == 100L)
                .map(SearchEntity::getWeight).findFirst().get()).isEqualTo(123.5);

        Assertions.assertThat(res.stream().filter(p -> p.getPartnerId() == 200L)
                .map(SearchEntity::getWeight).findFirst().get()).isEqualTo(333);
    }
}
