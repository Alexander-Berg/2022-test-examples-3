import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.crm.lb.LBInstallation;
import ru.yandex.market.crm.lb.LogBrokerMessageConsumer;
import ru.yandex.market.crm.lb.LogBrokerReaderFactory;
import ru.yandex.market.crm.lb.LogIdentifier;
import ru.yandex.market.crm.lb.lock.MultiHostPartitionLocksManager;
import ru.yandex.market.crm.lb.lock.PartitionLocksManagerFactory;
import ru.yandex.market.crm.lb.tx.TxStrategy;
import ru.yandex.market.crm.triggers.LogBrokerConfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author vtarasoff
 * @since 12.10.2020
 */
@RunWith(MockitoJUnitRunner.class)
public class LogBrokerConsumersConfigTest {
    private static final String CLIENT_ID = "test_id";

    private static LogBrokerMessageConsumer createConsumer(LogIdentifier... logIdentifiers) {
        LogBrokerMessageConsumer mock = mock(LogBrokerMessageConsumer.class);
        when(mock.getLogIdentifiers()).thenReturn(Set.of(logIdentifiers));
        return mock;
    }

    private static LogBrokerMessageConsumer createConsumer() {
        LogBrokerMessageConsumer mock = mock(LogBrokerMessageConsumer.class);
        when(mock.getLogIdentifiers()).thenReturn(Set.of());
        return mock;
    }

    @Mock
    private LogBrokerReaderFactory logbrokerReaderFactory;

    @Mock
    private LogBrokerReaderFactory lbkxReaderFactory;

    @Mock
    private TxStrategy txStrategy;

    private LogBrokerConfig config = new LogBrokerConfig();

    @Test
    public void shouldFilterEmptyConsumers() {
        logBrokerReader(List.of());
        lbkxReader(List.of());

        verifyConsumers(logbrokerReaderFactory, List.of());
        verifyConsumers(lbkxReaderFactory, List.of());
    }

    @Test
    public void shouldFilterConsumers() {
        LogBrokerMessageConsumer lb = createConsumer(
                new LogIdentifier("lb", "lb", LBInstallation.LOGBROKER));
        LogBrokerMessageConsumer lb2 = createConsumer(
                new LogIdentifier("lb", "lb", LBInstallation.LOGBROKER),
                new LogIdentifier("lbpre", "lbpre", LBInstallation.LOGBROKER_PRESTABLE));
        LogBrokerMessageConsumer lbkx = createConsumer(
                new LogIdentifier("lbkx", "lbkx", LBInstallation.LBKX));
        LogBrokerMessageConsumer lbkx2 = createConsumer(
                new LogIdentifier("lbkx", "lbkx", LBInstallation.LBKX),
                new LogIdentifier("lbpre", "lbpre", LBInstallation.LOGBROKER_PRESTABLE));
        LogBrokerMessageConsumer common = createConsumer(
                new LogIdentifier("lb", "lb", LBInstallation.LOGBROKER),
                new LogIdentifier("lbkx", "lbkx", LBInstallation.LBKX));
        LogBrokerMessageConsumer lbpre = createConsumer(
                new LogIdentifier("lbpre", "lbpre", LBInstallation.LOGBROKER_PRESTABLE));
        LogBrokerMessageConsumer empty = createConsumer();

        List consumers = List.of(lb, lb2, lbkx, lbkx2, common, lbpre, empty);

        logBrokerReader(consumers);
        lbkxReader(consumers);

        verifyConsumers(logbrokerReaderFactory, List.of(lb, lb2, common));
        verifyConsumers(lbkxReaderFactory, List.of(lbkx, lbkx2, common));
    }


    private void logBrokerReader(Collection<LogBrokerMessageConsumer> consumers) {
        config.logBrokerReader(
                logbrokerReaderFactory,
                txStrategy,
                MultiHostPartitionLocksManager::new,
                consumers,
                CLIENT_ID
        );
    }

    private void lbkxReader(Collection<LogBrokerMessageConsumer> consumers) {
        config.lbkxReader(lbkxReaderFactory, txStrategy, consumers, CLIENT_ID);
    }

    private void verifyConsumers(LogBrokerReaderFactory logbrokerReaderFactory,
                                 Collection<LogBrokerMessageConsumer> expectedConsumers) {
        verify(logbrokerReaderFactory)
                .create(eq(expectedConsumers), eq(txStrategy), any(PartitionLocksManagerFactory.class), eq(CLIENT_ID));
    }
}
