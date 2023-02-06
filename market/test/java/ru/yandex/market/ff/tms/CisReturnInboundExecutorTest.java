package ru.yandex.market.ff.tms;

import java.time.LocalDateTime;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.configuration.DateTimeTestConfig;
import ru.yandex.market.ff.service.DateTimeService;
import ru.yandex.market.ff.service.implementation.cisTransfer.CisTransferCreationService;
import ru.yandex.market.ff.util.BasicColumnsFilter;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.Mockito.when;

public class CisReturnInboundExecutorTest extends IntegrationTest {

    @Autowired
    private CisTransferCreationService service;
    @Autowired
    private DateTimeService dateTimeService;

    private CisReturnInboundExecutor executor;

    public CisReturnInboundExecutorTest() {
    }

    @AfterEach
    public void resetDateTimeServiceToDefault() {
        Mockito.when(dateTimeService.localDateTimeNow()).thenReturn(DateTimeTestConfig.FIXED_NOW);
    }

    @BeforeEach
    public void init() {
        executor = new CisReturnInboundExecutor(service);
    }

    @Test
    @DatabaseSetup("classpath:tms/cis-return-inbound/before-clean.xml")
    @ExpectedDatabase(value = "classpath:tms/cis-return-inbound/after-clean.xml", assertionMode = NON_STRICT_UNORDERED)
    public void onlyCorrectDraftsWereDeleted() {
        when(dateTimeService.localDateTimeNow()).thenReturn(LocalDateTime.of(2019, 1, 7,
                10, 10, 10));
        executor.doJob(null);
    }

    /**
     * Проверяет, что дата корректно сдвигается через выходные.
     */
    @Test
    @DatabaseSetup("classpath:tms/cis-return-inbound/before-clean.xml")
    @ExpectedDatabase(value = "classpath:tms/cis-return-inbound/after-clean-sunday.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void processForTransferWithLastWorkingDayDate() {
        when(dateTimeService.localDateTimeNow()).thenReturn(LocalDateTime.of(2018, 1, 7,
                10, 10, 10));
        executor.doJob(null);
    }

    /**
     * Проверяет, что дата корректно сдвигается через выходные.
     */
    @Test
    @DatabaseSetup("classpath:tms/cis-return-inbound/before-clean.xml")
    @ExpectedDatabase(value = "classpath:tms/cis-return-inbound/after-clean-monday.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void processForTransferWithLastWorkingDayDate2() {
        when(dateTimeService.localDateTimeNow()).thenReturn(LocalDateTime.of(2018, 1, 8,
                10, 10, 10));
        executor.doJob(null);
    }

    /**
     * Проверяет, что для одинаковых скю в разных заказах будет создан трансфер со всеми КИЗами
     */
    @Test
    @DatabaseSetup("classpath:tms/cis-return-inbound/before-sku-with-cis-in-different-orders.xml")
    @ExpectedDatabase(value = "classpath:tms/cis-return-inbound/after-sku-with-cis-in-different-orders.xml",
            assertionMode = NON_STRICT_UNORDERED, columnFilters = {BasicColumnsFilter.class})
    public void processForTransferWithSkuInDifferentOrders() {
        when(dateTimeService.localDateTimeNow()).thenReturn(LocalDateTime.of(2018, 1, 8,
                10, 10, 10));
        executor.doJob(null);
    }
}
