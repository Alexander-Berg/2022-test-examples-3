package ru.yandex.market.logistic.gateway.service.util;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

public class DatabaseTablesOldEntriesCleaningServiceWithSingleBatchTest
    extends AbstractDatabaseTablesOldEntriesCleaningServiceTest {

    @Test
    @DatabaseSetup("classpath:repository/state/before_old_entries_cleaning.xml")
    @ExpectedDatabase(value = "classpath:repository/expected/after_old_entries_cleaning.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testCleanDatabaseTablesOldEntriesWithSingleBatchSuccess() {
        cleaningService.cleanDatabaseTablesOldEntries();
    }

    @Test
    @DatabaseSetup("classpath:repository/state/before_old_entries_cleaning.xml")
    @ExpectedDatabase(value = "classpath:repository/state/before_old_entries_cleaning.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testCleanDatabaseTablesOldEntriesFailed() {
        doThrow(RuntimeException.class).when(readWriteTransactionTemplate).execute(any());

        softAssert.assertThatThrownBy(() -> cleaningService.cleanDatabaseTablesOldEntries())
            .as("Asserting that the valid exception is thrown")
            .isInstanceOf(RuntimeException.class);
    }
}
