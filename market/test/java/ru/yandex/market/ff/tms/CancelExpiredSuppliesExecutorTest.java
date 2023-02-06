package ru.yandex.market.ff.tms;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.framework.history.wrapper.HistoryAgent;
import ru.yandex.market.ff.service.RequestExpirationService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

class CancelExpiredSuppliesExecutorTest extends IntegrationTest {

    @Autowired
    private RequestExpirationService requestExpirationService;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    @Autowired
    private HistoryAgent historyAgent;

    private CancelExpiredSuppliesExecutor cancelExpiredSuppliesExecutor;

    @BeforeEach
    void init() {
        cancelExpiredSuppliesExecutor = new CancelExpiredSuppliesExecutor(
                executorService,
                requestExpirationService,
                historyAgent
        );
    }

    /**
     * В БД:
     * Now = 2018-01-01 10:10:10
     * Интервал протухания заявок - 100 часов (чуть больше 4-х суток)
     * <p>
     * Проверяем, что:
     * 1. Для поставки с requestId=1 в статусе 3 c requestedDate='2017-12-21 10:10:10'
     * будет запрошена отмена т.к. дифф больше 4-х дней
     * <p>
     * 2. Для поставки с requestId=2 в статусе 210 c requestedDate='2017-12-21 10:10:10'
     * будет запрошена отмена т.к. дифф больше 4-х дней
     * <p>
     * 3 Для поставки с requestId=3 в статусе 1 c requestedDate='2017-12-21 10:10:10'
     * будет запрошена отмена т.к. дифф больше 4-х дней
     * <p>
     * 4. Для поставки с requestId=4 в статусе 3 c requestedDate='2018-01-01 10:10:10'
     * НЕ будет запрошена отмена т.к. дифф меньше 4-х дней
     * <p>
     * 5. Для поставки с requestId=5 в статусе 210 c requestedDate='2018-01-01 10:10:10'
     * НЕ будет запрошена отмена т.к. дифф меньше 4-х дней
     * <p>
     * 6. Для поставки с requestId=6 в статусе 1 c requestedDate='2018-01-01 10:10:10'
     * НЕ будет запрошена отмена т.к. дифф меньше 4-х дней
     * <p>
     * 7. Для изъятия с requestId=7 в статусе 1 c requestedDate='2017-12-21 10:10:10'
     * НЕ будет запрошена отмена т.к. хоть и дифф больше 4-х дней, но заявка не является поставкой
     */
    @Test
    @DatabaseSetup("classpath:tms/cancel-expired-supplies/before.xml")
    @ExpectedDatabase(value = "classpath:tms/cancel-expired-supplies/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shouldCancelExpiredSuppliesAndSaveOthers() {
        cancelExpiredSuppliesExecutor.doJob(null);
    }
}
