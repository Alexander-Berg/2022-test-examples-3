package ru.yandex.market.logistics.iris.jobs.sync.content;

import java.util.Collections;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.configuration.MboClientMockConfiguration;
import ru.yandex.market.logistics.iris.configuration.queue.DbQueueConfiguration;
import ru.yandex.market.logistics.iris.core.domain.source.Source;
import ru.yandex.market.logistics.iris.jobs.consumers.sync.ContentSyncService;
import ru.yandex.market.logistics.iris.jobs.model.ItemKeysExecutionQueuePayload;
import ru.yandex.market.logistics.iris.model.ItemIdentifierDTO;

@Import({DbQueueConfiguration.class, MboClientMockConfiguration.class})
public class ContentSyncServiceTest extends AbstractContextualTest {

    @Autowired
    private ContentSyncService syncService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DatabaseSetup("classpath:fixtures/setup/sync/content/3.xml")
    @ExpectedDatabase(value = "classpath:fixtures/expected/sync/content/3.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void shouldCreateNewRecordIfItDoesNotExistAfterSyncWithContent() {
        transactionTemplate.execute(tx -> {
            syncService.processPayload(new ItemKeysExecutionQueuePayload(
                    "",
                    Collections.singletonList(new ItemIdentifierDTO("1", "sku")),
                    Source.FAKE_SOURCE
            ));
            return null;
        });
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/sync/content/4.xml")
    @ExpectedDatabase(value = "classpath:fixtures/expected/sync/content/4.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void shouldUpdateExistingContentRecordAfterSyncWithContent() {
        transactionTemplate.execute(tx -> {
            syncService.processPayload(new ItemKeysExecutionQueuePayload(
                    "",
                    Collections.singletonList(new ItemIdentifierDTO("1", "sku")),
                    Source.FAKE_SOURCE
            ));
            return null;
        });
    }
}
