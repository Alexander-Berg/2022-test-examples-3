package ru.yandex;

import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * @author komarovns
 * @date 08.10.18
 */
public abstract class EmptyTestWithTransactionTemplate {
    protected final TransactionTemplate transactionTemplate = Mockito.mock(TransactionTemplate.class);

    @BeforeEach
    public void initTransactionTemplate() {
        when(transactionTemplate.execute(any())).thenAnswer(invocation ->
                ((TransactionCallback) invocation.getArguments()[0]).doInTransaction(new SimpleTransactionStatus()));
        doAnswer(invocation -> {
            ((Consumer<TransactionStatus>) invocation.getArguments()[0]).accept(new SimpleTransactionStatus());
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }
}
