package ru.yandex.market.ff.dbqueue.service;

import java.time.LocalDateTime;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.configuration.DateTimeTestConfig;
import ru.yandex.market.ff.exception.http.RequestNotFoundException;
import ru.yandex.market.ff.model.dbqueue.CisReturnInboundPayload;
import ru.yandex.market.ff.service.DateTimeService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

public class CisReturnInboundProcessingServiceTest extends IntegrationTest {

    @Autowired
    private CisReturnInboundProcessingService cisReturnInboundProcessingService;
    @Autowired
    private DateTimeService dateTimeService;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @AfterEach
    public void resetDateTimeServiceToDefault() {
        Mockito.when(dateTimeService.localDateTimeNow()).thenReturn(DateTimeTestConfig.FIXED_NOW);
    }

    /**
     * Проверяет, что если детали для возвратной поставки не загружены, то таска сфейлится.
     */
    @Test
    @DatabaseSetup("classpath:db-queue/service/cis-return-inbound/before.xml")
    @ExpectedDatabase(
            value = "classpath:db-queue/service/cis-return-inbound/before.xml",
            assertionMode = NON_STRICT)
    public void processForNotTransfer() {
        when(dateTimeService.localDateTimeNow()).thenReturn(DateTimeTestConfig.FIXED_NOW);
        assertThrows(IllegalStateException.class, () ->
                cisReturnInboundProcessingService.processPayload(new CisReturnInboundPayload(1)));
    }

    /**
     * Проверяет, что если ShopRequest не найден, то таска фейлится.
     */
    @Test
    public void processWithAbsentShopRequest() {
        assertThrows(RequestNotFoundException.class, () ->
                cisReturnInboundProcessingService.processPayload(new CisReturnInboundPayload(-999)));
    }

    /**
     * Проверяет, что корректно создастся запись в таблице CisReturnInboundInfo
     */
    @Test
    @DatabaseSetup("classpath:db-queue/service/cis-return-inbound/before-correct.xml")
    @ExpectedDatabase(
            value = "classpath:db-queue/service/cis-return-inbound/after-with-cis.xml",
            assertionMode = NON_STRICT)
    public void processForTransfer() {
        when(dateTimeService.localDateTimeNow()).thenReturn(LocalDateTime.of(2017, 12, 31, 23, 10, 10));
        transactionTemplate.execute(status -> {
            cisReturnInboundProcessingService.processPayload(new CisReturnInboundPayload(1));
            return null;
        });
    }


    /**
     * проверяет, что для поставки у которой в товарах нет КИЗов не будет создана запись в cis_return_inbound_info
     */
    @Test
    @DatabaseSetup("classpath:db-queue/service/cis-return-inbound/before-correct.xml")
    @ExpectedDatabase(
            value = "classpath:db-queue/service/cis-return-inbound/before-correct.xml",
            assertionMode = NON_STRICT)
    public void processRequestWithoutCis() {
        when(dateTimeService.localDateTimeNow()).thenReturn(LocalDateTime.of(2017, 12, 31, 23, 10, 10));
        transactionTemplate.execute(status -> {
            cisReturnInboundProcessingService.processPayload(new CisReturnInboundPayload(3));
            return null;
        });
    }

}
