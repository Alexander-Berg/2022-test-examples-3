package ru.yandex.market.logistic.gateway.service.util;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "cleaning.batchSize=2")
public class DatabaseTablesOldEntriesCleaningServiceWithSeveralBatchesTest
    extends AbstractDatabaseTablesOldEntriesCleaningServiceTest {

    @Test
    @DatabaseSetup("classpath:repository/state/before_old_entries_cleaning.xml")
    @ExpectedDatabase(value = "classpath:repository/expected/after_old_entries_cleaning.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testCleanDatabaseTablesOldEntriesWithSeveralBatchesSuccess() {
        cleaningService.cleanDatabaseTablesOldEntries();
    }
}
