package ru.yandex.market.jmf.bcp.test;

import java.util.Map;
import java.util.function.Consumer;

import javax.inject.Named;
import javax.transaction.Transactional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.crm.util.Exceptions;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.bcp.ComputeIfAbsentExtensionStrategy;
import ru.yandex.market.jmf.bcp.test.internal.ComputeIfAbsentTest;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.tx.TxService;

@Transactional
@SpringJUnitConfig(InternalBcpTestConfiguration.class)
public class ComputeIfAbsentExtensionTest {
    private final BcpService bcpService;
    private final TxService txService;
    private final DbService dbService;
    private final ComputeIfAbsentExtensionStrategy<ComputeIfAbsentTest> test1;
    private final ComputeIfAbsentExtensionStrategy<ComputeIfAbsentTest> test2;

    public ComputeIfAbsentExtensionTest(
            BcpService bcpService,
            TxService txService,
            DbService dbService,
            @Named("computeIfAbsentTestStrategy1") ComputeIfAbsentExtensionStrategy<ComputeIfAbsentTest> test1,
            @Named("computeIfAbsentTestStrategy2") ComputeIfAbsentExtensionStrategy<ComputeIfAbsentTest> test2) {
        this.bcpService = bcpService;
        this.txService = txService;
        this.dbService = dbService;
        this.test1 = test1;
        this.test2 = test2;
    }

    @BeforeEach
    public void setUp() {
        Mockito.when(test1.compute(Mockito.any(ComputeIfAbsentTest.class))).thenReturn("test1");
        Mockito.when(test2.compute(Mockito.any(ComputeIfAbsentTest.class))).thenReturn("test2");
    }

    @AfterEach
    public void tearDown() {
        Mockito.reset(test1, test2);
    }

    @Test
    public void testThatItDoesNotFallIntoRecursion() {
        ComputeIfAbsentTest entity = bcpService.create(ComputeIfAbsentTest.FQN, Map.of());

        Mockito.verifyNoInteractions(test1, test2);

        String value1 = entity.getAttribute("computeIfAbsent1");

        Mockito.verify(test1, Mockito.times(1)).compute(Mockito.eq(entity));
        Mockito.verifyNoInteractions(test2);

        Mockito.clearInvocations(test1, test2);

        String value2 = entity.getAttribute("computeIfAbsent2");

        Mockito.verifyNoInteractions(test1);
        Mockito.verify(test2, Mockito.times(1)).compute(Mockito.eq(entity));

        Mockito.verifyNoMoreInteractions(test1, test2);

        Assertions.assertEquals("test1", value1);
        Assertions.assertEquals("test2", value2);
    }

    @ParameterizedTest
    @CsvSource({
            "true",
            "false"
    })
    @Transactional(value = Transactional.TxType.NEVER)
    public void testDifferentReadWriteTxModes(boolean readOnlyTx) {
        try {
            String gid = txService.doInTx(() -> bcpService.create(ComputeIfAbsentTest.FQN, Map.of()).getGid());

            Mockito.verifyNoInteractions(test1, test2);

            Consumer<Exceptions.TrashRunnable> txRunner = readOnlyTx
                    ? txService::runInNewReadOnlyTx
                    : txService::runInNewTx;

            var verificationMode = readOnlyTx
                    ? Mockito.atLeastOnce()
                    : Mockito.never();

            txRunner.accept(() -> {
                ComputeIfAbsentTest entity = dbService.get(gid);
                Mockito.clearInvocations(txService);
                String value1 = entity.getAttribute("computeIfAbsent1");

                // Mockito.atLeastOnce() - потому что соединения берутся и в других потоках (например, обновления
                // локов), и тогда получаем большее число вызовов, чем хотели бы
                Mockito.verify(txService, Mockito.atLeastOnce()).isReadOnly();
                Mockito.verify(txService, verificationMode).runInNewTx(Mockito.any(Exceptions.TrashRunnable.class));
                Mockito.verify(test1, Mockito.times(1)).compute(Mockito.eq(entity));
                Mockito.verifyNoInteractions(test2);

                Mockito.clearInvocations(test1, test2, txService);

                String value2 = entity.getAttribute("computeIfAbsent2");

                // Mockito.atLeastOnce() - потому что соединения берутся и в других потоках (например, обновления
                // локов), и тогда получаем большее число вызовов, чем хотели бы
                Mockito.verify(txService, Mockito.atLeastOnce()).isReadOnly();
                Mockito.verify(txService, verificationMode).runInNewTx(Mockito.any(Exceptions.TrashRunnable.class));
                Mockito.verifyNoInteractions(test1);
                Mockito.verify(test2, Mockito.times(1)).compute(Mockito.eq(entity));

                Mockito.verifyNoMoreInteractions(test1, test2);

                Assertions.assertEquals("test1", value1);
                Assertions.assertEquals("test2", value2);
            });

            Mockito.clearInvocations(test1, test2);

            txService.runInNewTx(() -> {
                ComputeIfAbsentTest entity = dbService.get(gid);
                Mockito.clearInvocations(txService);
                String value1 = entity.getAttribute("computeIfAbsent1");

                Mockito.verify(txService, Mockito.never()).runInNewTx(Mockito.any(Exceptions.TrashRunnable.class));
                Mockito.verifyNoInteractions(test1, test2);

                String value2 = entity.getAttribute("computeIfAbsent2");

                Mockito.verify(txService, Mockito.never()).runInNewTx(Mockito.any(Exceptions.TrashRunnable.class));
                Mockito.verifyNoMoreInteractions(test1, test2);

                Assertions.assertEquals("test1", value1);
                Assertions.assertEquals("test2", value2);
            });
        } finally {
            txService.runInTx(() ->
                    dbService.createQuery("DELETE FROM %s".formatted(ComputeIfAbsentTest.FQN)).executeUpdate()
            );
        }
    }
}
