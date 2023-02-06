package ru.yandex.market.jmf.tx;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import ru.yandex.market.crm.util.Exceptions;

@Primary
@Component
@Profile("singleTx")
public class RequireTransactionTxService implements TxService {
    private final TxService txService;

    public RequireTransactionTxService(PlatformTransactionManager platformTxManager) {
        this.txService = new TransactionTemplateTxService(platformTxManager);
    }

    @Override
    public <T> T getOrBindResource(@Nonnull Object key, @Nonnull Supplier<T> resourceProvider) {
        return txService.getOrBindResource(key, resourceProvider);
    }

    @Override
    public <T> T getOrBindResource(@Nonnull Object key, @Nonnull Supplier<T> resourceProvider,
                                   ResourceSynchronization<T> synchronization) {
        return txService.getOrBindResource(key, resourceProvider, synchronization);
    }

    @Override
    public boolean isCurrentTransactionActive() {
        return txService.isCurrentTransactionActive();
    }

    @Override
    public boolean isReadOnly() {
        return txService.isReadOnly();
    }

    @Override
    public String getCurrentTransactionName() {
        return txService.getCurrentTransactionName();
    }

    @Override
    public <T> T doInTx(Callable<T> action) {
        if (isCurrentTransactionActive()) {
            return Exceptions.sneakyRethrow(action::call);
        }
        return txService.doInTx(action);
    }

    @Override
    public void runInTx(Exceptions.TrashRunnable runnable) {
        if (isCurrentTransactionActive()) {
            runnable.run();
            return;
        }
        txService.runInTx(runnable);
    }

    @Override
    public <T> T doInReadOnlyTx(Callable<T> action) {
        if (isCurrentTransactionActive()) {
            return Exceptions.sneakyRethrow(action::call);
        }
        return txService.doInReadOnlyTx(action);
    }

    @Override
    public void runInReadOnlyTx(Exceptions.TrashRunnable runnable) {
        if (isCurrentTransactionActive()) {
            runnable.run();
            return;
        }
        txService.runInReadOnlyTx(runnable);
    }

    @Override
    public <T> T doInNewReadOnlyTx(Callable<T> action) {
        if (isCurrentTransactionActive()) {
            return Exceptions.sneakyRethrow(action::call);
        }
        return txService.doInNewReadOnlyTx(action);
    }

    @Override
    public void runInNewReadOnlyTx(Exceptions.TrashRunnable runnable) {
        if (isCurrentTransactionActive()) {
            runnable.run();
            return;
        }
        txService.runInNewReadOnlyTx(runnable);
    }

    @Override
    public <T> T doInNewTx(Callable<T> action) {
        if (isCurrentTransactionActive()) {
            return Exceptions.sneakyRethrow(action::call);
        }
        return txService.doInNewTx(action);
    }

    @Override
    public void runInNewTx(Exceptions.TrashRunnable action) {
        if (isCurrentTransactionActive()) {
            action.run();
            return;
        }
        txService.runInNewTx(action);
    }

    @Override
    public <T> T doInNewTx(Duration timeout, Callable<T> action) {
        if (isCurrentTransactionActive()) {
            return Exceptions.sneakyRethrow(action::call);
        }
        return txService.doInNewTx(timeout, action);
    }

    @Override
    public void runInNewTx(Duration timeout, Exceptions.TrashRunnable runnable) {
        if (isCurrentTransactionActive()) {
            runnable.run();
            return;
        }
        txService.runInNewTx(timeout, runnable);
    }

    @Override
    public <T> T doInNewReadonlyTx(Duration timeout, Callable<T> action) {
        if (isCurrentTransactionActive()) {
            return Exceptions.sneakyRethrow(action::call);
        }
        return txService.doInNewReadonlyTx(timeout, action);
    }

    @Override
    public void runInNewReadonlyTx(Duration timeout, Exceptions.TrashRunnable runnable) {
        if (isCurrentTransactionActive()) {
            runnable.run();
            return;
        }
        txService.runInNewReadonlyTx(timeout, runnable);
    }

    @Override
    public <T> T doInSuspend(Callable<T> action) {
        return txService.doInSuspend(action);
    }
}
