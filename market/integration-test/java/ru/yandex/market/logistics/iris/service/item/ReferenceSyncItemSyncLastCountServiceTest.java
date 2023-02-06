package ru.yandex.market.logistics.iris.service.item;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.core.domain.source.Source;
import ru.yandex.market.logistics.iris.core.domain.source.SourceType;
import ru.yandex.market.logistics.iris.jobs.to.CountBySource;

public class ReferenceSyncItemSyncLastCountServiceTest extends AbstractContextualTest {

    @Autowired
    private ReferenceSyncItemSyncLastCountService referenceSyncItemSyncLastCountService;


    @Test
    public void findCountBySourcesWhenNotFound() {

        List<Source> sources = List.of(new Source("1", SourceType.CONTENT));

        List<CountBySource> countBySources = referenceSyncItemSyncLastCountService.findCountBySources(sources);
        assertions().assertThat(countBySources).hasSize(0);

    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/reference_sync_item_sync_last_count/1.xml")
    public void findCountBySourcesWhenFound() {

        List<Source> sources = List.of(new Source("1", SourceType.CONTENT),
                new Source("2", SourceType.MDM));

        List<CountBySource> countBySources = referenceSyncItemSyncLastCountService.findCountBySources(sources);
        assertions().assertThat(countBySources).hasSize(2);
        assertions().assertThat(countBySources.get(0).getSource().getSourceId()).isEqualTo("1");
        assertions().assertThat(countBySources.get(0).getSource().getSourceType()).isEqualTo(SourceType.CONTENT);
        assertions().assertThat(countBySources.get(0).getCount()).isEqualTo(19L);
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/reference_sync_item_sync_last_count/2.xml")
    public void findCountBySourcesWhenFoundButPartOnly() {

        List<Source> sources = List.of(new Source("1", SourceType.CONTENT),
                new Source("2", SourceType.MDM));

        List<CountBySource> countBySources = referenceSyncItemSyncLastCountService.findCountBySources(sources);
        assertions().assertThat(countBySources).hasSize(2);
        assertions().assertThat(countBySources.get(0).getSource().getSourceId()).isEqualTo("1");
        assertions().assertThat(countBySources.get(0).getSource().getSourceType()).isEqualTo(SourceType.CONTENT);
        assertions().assertThat(countBySources.get(0).getCount()).isEqualTo(19L);
    }

}
