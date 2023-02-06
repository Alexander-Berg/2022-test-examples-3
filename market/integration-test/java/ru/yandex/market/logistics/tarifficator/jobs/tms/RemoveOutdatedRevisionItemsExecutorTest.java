package ru.yandex.market.logistics.tarifficator.jobs.tms;

import java.time.Clock;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.service.mds.MdsFileService;
import ru.yandex.market.logistics.tarifficator.service.revision.RevisionItemService;
import ru.yandex.market.logistics.test.integration.jpa.JpaQueriesCount;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@DisplayName("Интеграционный тест RemoveOutdatedRevisionItemsExecutor")
class RemoveOutdatedRevisionItemsExecutorTest extends AbstractContextualTest {

    @Autowired
    private Clock clock;
    @Autowired
    private RevisionItemService revisionItemService;
    @Autowired
    private MdsFileService mdsFileService;

    private final int itemsExpireAfterHours = 12;
    private final int expiredItemsProcessingBatchSize = 1;

    @DisplayName("Успешное удаление двух элементов отдельными батчами и пометка их файлов к удалению")
    @DatabaseSetup("/tms/remove-outdated-items/before.xml")
    @ExpectedDatabase(value = "/tms/remove-outdated-items/after_success.xml", assertionMode = NON_STRICT)
    @Test
    void successDelete() {
        new RemoveOutdatedRevisionItemsExecutor(
            clock,
            revisionItemService,
            itemsExpireAfterHours,
            expiredItemsProcessingBatchSize
        ).doJob(null);
    }

    @JpaQueriesCount(4)
    @DisplayName("Подсчет количества запросов для удаления одного батча")
    @DatabaseSetup("/tms/remove-outdated-items/before.xml")
    @ExpectedDatabase(value = "/tms/remove-outdated-items/after_success.xml", assertionMode = NON_STRICT)
    @Test
    void successWithCounting() {
        new RemoveOutdatedRevisionItemsExecutor(
            clock,
            revisionItemService,
            itemsExpireAfterHours,
            100
        ).doJob(null);
    }

    @DisplayName("Ошибка удаления элементов, setStatus бросает исключение")
    @DatabaseSetup("/tms/remove-outdated-items/before.xml")
    @ExpectedDatabase(value = "/tms/remove-outdated-items/before.xml", assertionMode = NON_STRICT)
    @Test
    void errorDeleteSetStatusThrowsException() {
        String errorMessage = "Failed";
        doThrow(new IllegalArgumentException(errorMessage))
            .when(mdsFileService).setStatus(any(), any());

        softly.assertThatThrownBy(() -> new RemoveOutdatedRevisionItemsExecutor(
            clock,
            revisionItemService,
            itemsExpireAfterHours,
            expiredItemsProcessingBatchSize
        ).doJob(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(errorMessage);
    }
}
