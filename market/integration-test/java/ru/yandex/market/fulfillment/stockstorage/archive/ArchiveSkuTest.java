package ru.yandex.market.fulfillment.stockstorage.archive;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.fulfillment.stockstorage.AbstractContextualTest;
import ru.yandex.market.fulfillment.stockstorage.service.archive.ArchiveJobQueueSync;
import ru.yandex.market.fulfillment.stockstorage.service.helper.IdGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DatabaseSetup("classpath:database/states/system_property.xml")
public class ArchiveSkuTest extends AbstractContextualTest {
    private static final String ARCHIVE_URL = "/archive";
    private static final String UNARCHIVE_URL = "/unarchive";

    @Autowired
    private ArchiveJobQueueSync archiveJobQueueSync;
    @SpyBean
    private IdGenerator idGenerator;
    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    public void initDB() {
        doReturn("ID1")
                .doReturn("ID2")
                .doReturn("ID3")
                .when(idGenerator).get();

        jdbc.update("delete from archive.a_sku_sync_audit");
        jdbc.update("delete from archive.a_stock_lifetime");
        jdbc.update("delete from archive.a_stock_freeze");
        jdbc.update("delete from archive.a_stock");
        jdbc.update("delete from archive.a_sku");
    }

    @Test
    @ExpectedDatabase(value = "classpath:database/expected/archive/empty_queue.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testEnqueueEmpty() throws Exception {
        postArchive("requests/archive/empty.json");
    }

    @Test
    @ExpectedDatabase(value = "classpath:database/expected/archive/single.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testEnqueueSingleSku() throws Exception {
        postArchive("requests/archive/single.json");
    }

    @Test
    @DatabaseSetup("classpath:database/states/archive/batch_size_2.xml")
    @ExpectedDatabase(value = "classpath:database/expected/archive/three_batches.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testEnqueueThreeBatches() throws Exception {
        postArchive("requests/archive/three_batches.json");
    }

    @Test
    @DatabaseSetup("classpath:database/states/archive/single_sku.xml")
    @ExpectedDatabase(value = "classpath:database/expected/archive/single_sku.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            connection = "archiveDbUnitDatabaseConnection")
    @ExpectedDatabase(value = "classpath:database/expected/archive/empty.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testArchiveSingleSku() throws Exception {
        postArchive("requests/archive/single_sku.json");
        archiveJobQueueSync.consume();
    }

    @Test
    @DatabaseSetup("classpath:database/states/archive/empty_and_on_stock.xml")
    @ExpectedDatabase(value = "classpath:database/expected/archive/single_sku.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            connection = "archiveDbUnitDatabaseConnection")
    @ExpectedDatabase(value = "classpath:database/expected/archive/on_stock_sku.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testArchiveOneSuccessOneError() throws Exception {
        postArchive("requests/archive/empty_and_on_stock.json");
        archiveJobQueueSync.consume();
        assertEquals(1, jdbc.queryForObject("select count(1) from execution_queue", Integer.class));
        assertEquals(1, jdbc.queryForObject(
                "select jsonb_array_length(payload->'sku') from execution_queue", Integer.class));
    }

    @Test
    @DatabaseSetup("classpath:database/states/archive/single_sku.xml")
    @ExpectedDatabase(value = "classpath:database/expected/archive/empty_archive.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            connection = "archiveDbUnitDatabaseConnection")
    @ExpectedDatabase(value = "classpath:database/states/archive/single_sku.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testArchiveAndUnarchiveSingleSku() throws Exception {
        postArchive("requests/archive/single_sku.json");
        archiveJobQueueSync.consume();

        // check if archived
        assertEquals(0, jdbc.queryForObject("select count(1) from sku", Integer.class));
        assertEquals(1, jdbc.queryForObject("select count(1) from archive.a_sku", Integer.class));

        postUnarchive("requests/archive/single_sku.json");
    }

    @Test
    @DatabaseSetup("classpath:database/states/archive/three_sku.xml")
    public void testThreeSku() throws Exception {
        postArchive("requests/archive/three_sku.json");
        archiveJobQueueSync.consume();

        // один sku с положительным стоком вернется обратно в очередь
        assertEquals(1, jdbc.queryForObject("select count(1) from execution_queue", Integer.class));
        assertEquals(1, jdbc.queryForObject(
                "select jsonb_array_length(payload->'sku') from execution_queue", Integer.class));
    }

    private void postArchive(String resourcePath) throws Exception {
        mockMvc.perform(post(ARCHIVE_URL)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(extractFileContent(resourcePath)))
                .andExpect(status().isOk());
    }

    private void postUnarchive(String resourcePath) throws Exception {
        mockMvc.perform(post(UNARCHIVE_URL)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(extractFileContent(resourcePath)))
                .andExpect(status().isOk());
    }
}
