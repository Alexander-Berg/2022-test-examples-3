package ru.yandex.market.delivery;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.delivery.model.DaasServiceTransaction;
import ru.yandex.market.core.delivery.repository.DaasServiceTransactionDao;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.BillingTransactionDto;
import ru.yandex.market.tms.quartz2.model.Executor;

import static org.mockito.Mockito.when;

class LoadDaasServiceTransactionsExecutorTest extends FunctionalTest {

    @Autowired
    private LomClient lomClient;

    @Autowired
    private DaasServiceTransactionDao daasServiceTransactionDao;

    @Autowired
    @Qualifier("loadLomTransactionsExecutor")
    private Executor executor;

    @Autowired
    private TestableClock clock;

    private static List<BillingTransactionDto> lomTransactions() {
        return List.of(
                BillingTransactionDto.builder()
                        .id(1L)
                        .entityId(100L)
                        .contractId(1000L)
                        .time(LocalDateTime.of(2000, 1, 1, 10, 0, 0))
                        .amount(new BigDecimal("55000.50"))
                        .correction(false)
                        .productId(11L)
                        .build(),
                BillingTransactionDto.builder()
                        .id(2L)
                        .entityId(100L)
                        .contractId(1000L)
                        .time(LocalDateTime.of(2000, 1, 1, 12, 0, 0))
                        .amount(new BigDecimal("-5000.50"))
                        .correction(true)
                        .productId(11L)
                        .build(),
                BillingTransactionDto.builder()
                        .id(3L)
                        .entityId(300L)
                        .contractId(3000L)
                        .time(LocalDateTime.of(2000, 1, 1, 11, 20, 33))
                        .amount(new BigDecimal("99"))
                        .correction(false)
                        .productId(22L)
                        .build()
        );
    }

    @BeforeEach
    void onBefore() {
        clock.setFixed(Instant.parse("2019-12-26T00:00:00.00Z"), ZoneOffset.UTC);
    }

    @AfterEach
    void onAfter() {
        clock.clearFixed();
    }

    /**
     * Тест проверяет валидность синхронизации при отсутствии сохраненной последней даты синхронизации.
     */
    @Test
    @DbUnitDataSet(after = "data/testSyncLomTransactionsStartDate.after.csv")
    void syncLomTransactionsEmptyStartDateTest() {
        when(lomClient.findTransactions(
                LocalDateTime.of(2019, 12, 24, 9, 0, 0),
                LocalDateTime.of(2019, 12, 26, 0, 0, 0)
        )).thenReturn(lomTransactions());

        executor.doJob(null);
    }

    /**
     * Тест проверяет валидность синхронизации при наличии сохраненной последней даты синхронизации.
     */
    @Test
    @DbUnitDataSet(
            before = "data/testSyncLomTransactionsStartDate.before.csv",
            after = "data/testSyncLomTransactionsStartDate.after.csv"
    )
    void syncLomTransactionsFilledStartDateTest() {
        when(lomClient.findTransactions(
                LocalDateTime.of(1999, 12, 31, 17, 0, 0),
                LocalDateTime.of(2019, 12, 26, 0, 0, 0)
        )).thenReturn(lomTransactions());

        executor.doJob(null);
    }

    /**
     * Тест проверяет валидность обновления последней даты синхронизации при получении пустого списка транзакций.
     */
    @Test
    @DbUnitDataSet(
            before = "data/testSyncLomTransactionsStartDate.before.csv",
            after = "data/testSyncLomTransactionsStartDateEmptyResult.after.csv"
    )
    void syncLomTransactionsEmptyResponse() {
        executor.doJob(null);
    }

    @Test
    void smokeTest() {
        daasServiceTransactionDao.saveAll(List.of(DaasServiceTransaction.builder()
                .setCorrection(true)
                .setContractId(1L)
                .setAmount(BigDecimal.ONE)
                .setTime(Instant.now())
                .setProductId(2L)
                .setEntityId(1L)
                .setId(1L)
                .build()
        ));
    }
}
