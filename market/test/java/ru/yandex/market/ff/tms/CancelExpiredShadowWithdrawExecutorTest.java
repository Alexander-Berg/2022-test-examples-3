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
import ru.yandex.market.ff.service.RequestSubTypeService;
import ru.yandex.market.ff.service.ShopRequestFetchingService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

public class CancelExpiredShadowWithdrawExecutorTest extends IntegrationTest {

    @Autowired
    private ShopRequestFetchingService shopRequestFetchingService;
    @Autowired
    private RequestCancellationService cancellationService;
    @Autowired
    ConcreteEnvironmentParamService concreteEnvironmentParamService;
    @Autowired
    DateTimeService dateTimeService;
    @Autowired
    private RequestSubTypeService requestSubTypeService;
    @Autowired
    private HistoryAgent historyAgent;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private CancelExpiredShadowWithdrawExecutor cancelExpiredShadowSuppliesExecutor;
    private ProcessCancellationsExecutor processCancellationsExecutor;

    @BeforeEach
    void init() {
        cancelExpiredShadowSuppliesExecutor = new CancelExpiredShadowWithdrawExecutor(shopRequestFetchingService,
                cancellationService, concreteEnvironmentParamService, dateTimeService, executorService, historyAgent);
        processCancellationsExecutor = new ProcessCancellationsExecutor(
                shopRequestFetchingService, cancellationService, executorService, requestSubTypeService, historyAgent);
    }

    @Test
    @DatabaseSetup("classpath:tms/cancel-expired-shadow-withdraw/expired-1p-created/before.xml")
    @ExpectedDatabase(value = "classpath:tms/cancel-expired-shadow-withdraw/expired-1p-created/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shouldCancelExpiredShadowWithdraw1pInCreatedStatus() {
        cancelExpiredShadowSuppliesExecutor.doJob(null);
        processCancellationsExecutor.doJob(null);
    }

    @Test
    @DatabaseSetup("classpath:tms/cancel-expired-shadow-withdraw/expired-1p-validated/before.xml")
    @ExpectedDatabase(value = "classpath:tms/cancel-expired-shadow-withdraw/expired-1p-validated/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shouldCancelExpiredShadowWithdraw1pInValidatedStatus() {
        cancelExpiredShadowSuppliesExecutor.doJob(null);
    }

    @Test
    @DatabaseSetup("classpath:tms/cancel-expired-shadow-withdraw/not-expired-1p-created/before.xml")
    @ExpectedDatabase(value = "classpath:tms/cancel-expired-shadow-withdraw/not-expired-1p-created/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shouldNotCancelNotExpiredShadowWithdraw1pInCreatedStatus() {
        cancelExpiredShadowSuppliesExecutor.doJob(null);
        processCancellationsExecutor.doJob(null);
    }

    @Test
    @DatabaseSetup("classpath:tms/cancel-expired-shadow-withdraw/not-expired-1p-validated/before.xml")
    @ExpectedDatabase(value = "classpath:tms/cancel-expired-shadow-withdraw/not-expired-1p-validated/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shouldNotCancelNotExpiredShadowWithdraw1pInValidatedStatus() {
        cancelExpiredShadowSuppliesExecutor.doJob(null);
        processCancellationsExecutor.doJob(null);
    }

    @Test
    @DatabaseSetup("classpath:tms/cancel-expired-shadow-withdraw/expired-3p-validated/before.xml")
    @ExpectedDatabase(value = "classpath:tms/cancel-expired-shadow-withdraw/expired-3p-validated/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shouldCancelExpiredShadowWithdraw3pInValidatedStatus() {
        cancelExpiredShadowSuppliesExecutor.doJob(null);
    }

    @Test
    @DatabaseSetup("classpath:tms/cancel-expired-shadow-withdraw/not-expired-3p-validated/before.xml")
    @ExpectedDatabase(value = "classpath:tms/cancel-expired-shadow-withdraw/not-expired-3p-validated/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shouldNotCancelNotExpiredShadowWithdraw3pInValidatedStatus() {
        cancelExpiredShadowSuppliesExecutor.doJob(null);
        processCancellationsExecutor.doJob(null);
    }


}
