package ru.yandex.market.partner.notification.task;

import java.io.IOException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.open.api.exception.MbiOpenApiClientResponseException;
import ru.yandex.market.partner.notification.AbstractFunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class PreparePersistentNotificationTaskSyncTest extends AbstractFunctionalTest {
    @Autowired
    private PreparePersistentNotificationTask prepareTask;

    private static Exception[] clientExceptions() {
        return new Exception[]{
                new MbiOpenApiClientResponseException("oops", 500, null),
                new RuntimeException(new IOException())};
    }

    @ParameterizedTest
    @MethodSource("clientExceptions")
    @DbUnitDataSet(before = "prepare.error.before.csv", after = "prepare.error.after.csv")
    public void markPreparationErrorIfClientThrowsException(Exception exception) throws Exception {
        when(mbiOpenApiClient.provideTelegramAddresses(any()))
                .thenThrow(exception);

        prepareTask.execute();
    }
}
