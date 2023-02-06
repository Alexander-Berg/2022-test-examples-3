package ru.yandex.market.manager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.Mockito.verifyNoInteractions;
import static ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest.verifySentNotificationType;

/**
 * Тесты для {@link UnbindRetiredManagersExecutor}.
 */
public class UnbindRetiredManagersExecutorTest extends FunctionalTest {
    @Autowired
    private UnbindRetiredManagersExecutor executor;

    /**
     * Проверяет корректную работу, когда не надо никого отвязывать.
     */
    @Test
    @DbUnitDataSet(
            before = "UnbindRetiredManagersExecutorTest.before.csv",
            after = "UnbindRetiredManagersExecutorTest.before.csv"
    )
    void testNoRetired() {
        executor.doJob(null);

        verifyNoInteractions(partnerNotificationClient);
    }

    /**
     * Проверяет отвязку уволенных менеджеров.
     */
    @Test
    @DbUnitDataSet(
            before = {
                    "UnbindRetiredManagersExecutorTest.before.csv",
                    "UnbindRetiredManagersExecutorTest.retired.before.csv",
            },
            after = {
                    "UnbindRetiredManagersExecutorTest.before.csv",
                    "UnbindRetiredManagersExecutorTest.retired.after.csv",
            })
    void test() {
        executor.doJob(null);

        verifySentNotificationType(partnerNotificationClient, 1, 1639240019L);
    }
}
