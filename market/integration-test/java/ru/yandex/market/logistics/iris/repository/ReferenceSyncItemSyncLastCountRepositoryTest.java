package ru.yandex.market.logistics.iris.repository;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.core.domain.source.SourceType;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

public class ReferenceSyncItemSyncLastCountRepositoryTest extends AbstractContextualTest {


    @Autowired
    private ReferenceSyncItemSyncLastCountRepository referenceSyncItemSyncLastCountRepository;

    @Test
    @ExpectedDatabase(value = "classpath:fixtures/expected/reference_sync_item_sync_last_count/1.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void insertNewRowTest() {
        referenceSyncItemSyncLastCountRepository.updateCountByEmbeddableSource(SourceType.CONTENT.name(), "1", 19L);
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/reference_sync_item_sync_last_count/1.xml")
    @ExpectedDatabase(value = "classpath:fixtures/expected/reference_sync_item_sync_last_count/2.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void updateExistingRowTest() {
        referenceSyncItemSyncLastCountRepository.updateCountByEmbeddableSource(SourceType.CONTENT.name(), "1", 20L);
    }

}
