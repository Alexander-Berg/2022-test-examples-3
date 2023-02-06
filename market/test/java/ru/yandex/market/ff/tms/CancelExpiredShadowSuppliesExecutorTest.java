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
import ru.yandex.market.ff.service.ConcreteEnvironmentParamService;
import ru.yandex.market.ff.service.DateTimeService;
import ru.yandex.market.ff.service.RequestCancellationService;
import ru.yandex.market.ff.service.ShopRequestFetchingService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

class CancelExpiredShadowSuppliesExecutorTest extends IntegrationTest {
    @Autowired
    private ShopRequestFetchingService shopRequestFetchingService;
    @Autowired
    private RequestCancellationService cancellationService;
    @Autowired
    ConcreteEnvironmentParamService concreteEnvironmentParamService;
    @Autowired
    DateTimeService dateTimeService;
    @Autowired
    HistoryAgent historyAgent;

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    private CancelExpiredShadowSuppliesExecutor cancelExpiredShadowSuppliesExecutor;

    @BeforeEach
    void init() {
        cancelExpiredShadowSuppliesExecutor = new CancelExpiredShadowSuppliesExecutor(shopRequestFetchingService,
                cancellationService, concreteEnvironmentParamService, dateTimeService, executorService, historyAgent);
    }

    /**
     * Проверяем:
     * Заявки 1p с датой requested_date ранее даты устаревания переходят в статус CANCELED.
     * Заявки 3p с датой created_at ранее даты устаревания переходят в статус CANCELED.
     * Заявки с соответствующими датами позже дат устаревания остаются неизменными.
     * Таймслоты (для 1p) выделенные для устаревших заявок переходят в статус INACTIVE.
     * Квоты, выделенные для 1p освобождаются.
     * <p>
     * Примечания:
     * 1. Записи в истории статусов не проверяем - они создаются с одинаковой датой, движок dbunit'a их путает и выдаёт
     * ошибку при сравнении.
     * 2. cancelExpiredShadowSuppliesExecutor фактически переводит заявки в статус CANCELLATION_REQUESTED, далее
     * в тесте мы принудительно запускаем джобу processCancellationsExecutor, который реально отменяет помеченные
     * заявки (в реальности эта джоба тоже пускается по хрону движком tms)
     */
    @Test
    @DatabaseSetup("classpath:tms/cancel-expired-shadow-supplies/before.xml")
    @ExpectedDatabase(value = "classpath:tms/cancel-expired-shadow-supplies/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shouldCancelExpiredShadowSuppliesAndSaveOthers() {
        cancelExpiredShadowSuppliesExecutor.doJob(null);
    }

    @Test
    @DatabaseSetup("classpath:tms/cancel-expired-shadow-xdoc/before.xml")
    @ExpectedDatabase(value = "classpath:tms/cancel-expired-shadow-xdoc/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shouldCancelExpiredShadowXDocSupply1p() {
        cancelExpiredShadowSuppliesExecutor.doJob(null);
    }

}
