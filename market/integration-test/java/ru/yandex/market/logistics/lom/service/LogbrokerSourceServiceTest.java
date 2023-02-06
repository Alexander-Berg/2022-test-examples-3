package ru.yandex.market.logistics.lom.service;

import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.entity.LogbrokerSourceLock;
import ru.yandex.market.logistics.lom.entity.embedded.LogbrokerSourceLockId;
import ru.yandex.market.logistics.lom.entity.enums.LogbrokerSourceLockType;
import ru.yandex.market.logistics.lom.service.order.history.LogbrokerSourceService;

import static org.assertj.core.api.Assertions.assertThat;

public class LogbrokerSourceServiceTest extends AbstractContextualTest {
    @Autowired
    private LogbrokerSourceService logbrokerSourceService;
    @Autowired
    private TransactionTemplate transactionTemplate;

    private int propagation;

    @BeforeEach
    void setup() {
        propagation = transactionTemplate.getPropagationBehavior();
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @AfterEach
    void tearDown() {
        transactionTemplate.setPropagationBehavior(propagation);
    }

    @ParameterizedTest
    @EnumSource(value = LogbrokerSourceLockType.class, names = "CREATE_ORDER", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Проверка эксклюзивности лока")
    @DatabaseSetup("/service/logbroker_source_lock/before/setup.xml")
    void lockExclusive(LogbrokerSourceLockType lockType) {
        transactionTemplate.execute(ts1 -> {
            Optional<LogbrokerSourceLock> lock1 = logbrokerSourceService.getRowLock(1, lockType);
            assertThat(lock1).isPresent();
            softly.assertThat(lock1)
                .map(LogbrokerSourceLock::getLogbrokerSourceLockId)
                .map(LogbrokerSourceLockId::getId)
                .contains(1);
            softly.assertThat(lock1)
                .map(LogbrokerSourceLock::getLogbrokerSourceLockId)
                .map(LogbrokerSourceLockId::getType)
                .contains(lockType);
            transactionTemplate.execute(ts2 -> {
                Optional<LogbrokerSourceLock> lock2 = logbrokerSourceService.getRowLock(1, lockType);
                assertThat(lock2).isEmpty();
                return null;
            });
            return null;
        });
    }

    @ParameterizedTest
    @EnumSource(value = LogbrokerSourceLockType.class, names = "CREATE_ORDER", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Лок sourceId не запрещает изменять заказ")
    @DatabaseSetup("/service/logbroker_source_lock/before/setup.xml")
    @ExpectedDatabase(
        value = "/service/logbroker_source_lock/after/order_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testOrdersInsert(LogbrokerSourceLockType lockType) {
        transactionTemplate.execute(ts1 -> {
            Optional<LogbrokerSourceLock> lock1 = logbrokerSourceService.getRowLock(1, lockType);
            assertThat(lock1).isPresent();
            softly.assertThat(lock1)
                .map(LogbrokerSourceLock::getLogbrokerSourceLockId)
                .map(LogbrokerSourceLockId::getId)
                .contains(1);
            softly.assertThat(lock1)
                .map(LogbrokerSourceLock::getLogbrokerSourceLockId)
                .map(LogbrokerSourceLockId::getType)
                .contains(lockType);
            transactionTemplate.execute(ts2 -> {
                jdbcTemplate.update(
                    "SET LOCAL lock_timeout = 1;"
                    + " INSERT INTO orders(platform_client_id, sender_id, status, source_id) VALUES(3, 1, 'DRAFT', 1)"
                );
                return null;
            });
            return null;
        });
    }
}
