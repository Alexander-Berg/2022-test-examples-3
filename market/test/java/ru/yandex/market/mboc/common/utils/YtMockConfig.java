package ru.yandex.market.mboc.common.utils;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.impl.transactions.TransactionImpl;
import ru.yandex.inside.yt.kosher.operations.Operation;
import ru.yandex.inside.yt.kosher.operations.YtOperations;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.transactions.Transaction;
import ru.yandex.inside.yt.kosher.transactions.YtTransactions;

/**
 * @author kravchenko-aa
 * @date 18.09.2020
 */
@Configuration
public class YtMockConfig {
    @Bean
    public Yt ytMock() {
        Yt ytMock = Mockito.mock(Yt.class);
        Cypress cypress = Mockito.mock(Cypress.class);
        YtTables ytTables = Mockito.mock(YtTables.class);
        YtTransactions transactions = Mockito.mock(YtTransactions.class);
        YtOperations operations = Mockito.mock(YtOperations.class);

        Transaction transaction = new TransactionImpl(GUID.create(), null, ytMock, Instant.now(),
            Duration.ofHours(1));
        Mockito.when(transactions.startAndGet(Mockito.any(), Mockito.anyBoolean(), Mockito.any()))
            .thenReturn(transaction);

        Operation operation = Mockito.mock(Operation.class);
        Mockito.when(operation.getId()).thenReturn(GUID.create());
        Mockito.when(operations.mergeAndGetOp(Mockito.any(), Mockito.anyBoolean(), Mockito.any()))
            .thenReturn(operation);

        Mockito.when(ytMock.cypress()).thenReturn(cypress);
        Mockito.when(ytMock.tables()).thenReturn(ytTables);
        Mockito.when(ytMock.transactions()).thenReturn(transactions);
        Mockito.when(ytMock.operations()).thenReturn(operations);

        Mockito.doCallRealMethod()
            .when(ytTables)
            .write(Mockito.any(), Mockito.any(), Mockito.anyIterable());
        Mockito.doCallRealMethod()
            .when(ytTables)
            .write(Mockito.any(), Mockito.any(), Mockito.any(Iterator.class));
        return ytMock;
    }
}
