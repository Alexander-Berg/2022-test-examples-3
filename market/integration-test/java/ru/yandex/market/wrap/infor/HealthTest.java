package ru.yandex.market.wrap.infor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.common.ping.CheckResult;
import ru.yandex.market.common.ping.PingChecker;
import ru.yandex.market.wrap.infor.configuration.AbstractContextualTest;
import ru.yandex.market.wrap.infor.service.health.ShutdownPingChecker;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wrap.infor.configuration.PingCheckersConfiguration.WRAP_DB_CONNECTION_CHECKER;

class HealthTest extends AbstractContextualTest {

    private static final CheckResult DB_CONNECTION_ERR =
        new CheckResult(CheckResult.Level.CRITICAL, "Database connection error");

    @MockBean(name = WRAP_DB_CONNECTION_CHECKER)
    @Qualifier(WRAP_DB_CONNECTION_CHECKER)
    private PingChecker wrapDbHealthChecker;

    @SpyBean
    private ShutdownPingChecker shutdownHealthChecker;

    @BeforeEach
    void setUp() {
        when(wrapDbHealthChecker.check()).thenReturn(CheckResult.OK);
    }

    /**
     * Сценарий #1: пинг во время остановки приложения.
     * <p>
     * ShutdownCheck: 2;Application is terminating
     * WrapDatabaseConnectionCheck: 0;OK
     * <p>
     * Результат: 2.
     */
    @Test
    void pingWhenAppIsTerminating() throws Exception {
        when(shutdownHealthChecker.check())
            .thenReturn(new CheckResult(CheckResult.Level.CRITICAL, "Application is terminating"));

        String monrun = httpOperationWithResult(
            get("/ping"),
            status().is2xxSuccessful()
        );

        softly.assertThat(monrun).contains("2;", "Application is terminating");
        verifyOnlyShutdownCheckerWasCalled();
    }

    /**
     * Сценарий #2: ошибка соединения WRAP DB connection.
     * <p>
     * ShutdownCheck: 0;OK
     * WrapDatabaseConnectionCheck: 2;DB
     * <p>
     * Результат: ошибка - 2.
     */
    @Test
    void pingWhenWrapDbConnectionIsLost() throws Exception {
        when(wrapDbHealthChecker.check()).thenReturn(DB_CONNECTION_ERR);

        String monrun = httpOperationWithResult(
            get("/ping"),
            status().is2xxSuccessful()
        );

        softly.assertThat(monrun).contains("2;");
        verifyAllCheckersWereCalledOnce();
    }

    /**
     * Сценарий #3: ошибка соединения с WRAP БД при отсанавливающимся приложении.
     * <p>
     * ShutdownCheck: 2;Application is terminating;
     * WrapDatabaseConnectionCheck: проверки не происходит
     * <p>
     * Результат: ошибка - 2.
     */
    @Test
    void pingWhenAppIsTerminatingAndDbConnectionsAreLost() throws Exception {
        when(shutdownHealthChecker.check()).thenReturn(DB_CONNECTION_ERR);
        when(wrapDbHealthChecker.check()).thenReturn(DB_CONNECTION_ERR);

        String monrun = httpOperationWithResult(
            get("/ping"),
            status().is2xxSuccessful()
        );

        softly.assertThat(monrun).contains("2;", "Database connection error");
        verifyOnlyShutdownCheckerWasCalled();
    }

    /**
     * Сценарий #6: проверяем, что ни один из чеков не срабатыает.
     * <p>
     * ShutdownCheck: 0;OK
     * WrapDatabaseConnectionCheck: 0;OK
     * <p>
     * Результат: 0;OK.
     */
    @Test
    void ping() throws Exception {
        String monrun = httpOperationWithResult(
            get("/ping"),
            status().is2xxSuccessful()
        );

        softly.assertThat(monrun).isEqualTo("0;OK");
        verifyAllCheckersWereCalledOnce();
    }

    private void verifyAllCheckersWereCalledOnce() {
        verify(wrapDbHealthChecker, times(1)).check();
        verify(shutdownHealthChecker, times(1)).check();
    }

    private void verifyOnlyShutdownCheckerWasCalled() {
        verify(wrapDbHealthChecker, times(0)).check();
        verify(shutdownHealthChecker, times(1)).check();

    }
}
