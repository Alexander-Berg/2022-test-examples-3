package ru.yandex.market.billing.tasks.cs.access.rules;

import java.net.URI;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.tasks.BadSyncUsersException;
import ru.yandex.market.billing.tasks.cs.access.rules.misc.JavaSecHelper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

/**
 * Функциональыне тесты на {@link SyncManagersInfoExecutor}.
 *
 * @author yakun
 */
class SyncManagersInfoExecutorTest extends FunctionalTest {

    @Autowired
    private JavaSecHelper javaSecHelper;

    @Autowired
    private SyncManagersInfoExecutor executor;

    @Test
    @DisplayName("Проверка что все ОК")
    void syncManagersInfoTest() throws JSONException {
        doReturn(new JSONObject("{code:0}"))
                .when(javaSecHelper)
                .requestJavaSec(any(URI.class));
        executor.doJob(null);
    }

    @Test
    @DisplayName("Проверка что упадет exception в случае ошибки в cs-access-api-rules")
    void syncManagersInfoExceptionTest() throws JSONException {
        doReturn(new JSONObject("{code:1}"))
                .when(javaSecHelper)
                .requestJavaSec(any(URI.class));
        Assertions.assertThrows(
                BadSyncUsersException.class,
                () -> executor.doJob(null));
    }

    @Test
    @DisplayName("Проверка что упадет exception в случае ошибки при парсинге json")
    void syncManagersInfoTestJsonException() throws JSONException {
        doReturn(new JSONObject("{json:exception}"))
                .when(javaSecHelper)
                .requestJavaSec(any(URI.class));
        Assertions.assertThrows(
                RuntimeException.class,
                () -> executor.doJob(null));
    }
}
