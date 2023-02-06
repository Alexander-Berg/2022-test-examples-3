package ru.yandex.market.wms.common.spring.service.integration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.exception.OptimisticLockException;
import ru.yandex.market.wms.common.model.enums.LotStatus;
import ru.yandex.market.wms.common.service.OptimisticLockService;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.Lot;
import ru.yandex.market.wms.common.spring.dao.implementation.LotDao;
import ru.yandex.market.wms.common.spring.dao.implementation.ReceiptDao;
import ru.yandex.market.wms.common.spring.pojo.LotAggregatedFields;
import ru.yandex.market.wms.common.spring.pojo.SkuDimensions;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;

public class OptimisticLockServiceIntegrationTest extends IntegrationTest {

    private static final LocalDateTime FIRST_DATE = LocalDateTime.parse("2020-03-29T11:47:00");
    private static final LocalDateTime SECOND_DATE = LocalDateTime.parse("2020-03-29T11:47:25");
    private static final String RECEIPT_KEY = "0000012345";
    private static final Lot FIRST_LOT = createLot("465855", "ROV0000000000000001455");
    private static final Lot SECOND_LOT = createLot("465852", "ROV0000000000000001456");

    @Autowired
    private OptimisticLockService optimisticLockService;

    @Autowired
    private ReceiptDao receiptDao;

    @Autowired
    private LotDao lotDao;

    @Test
    @DatabaseSetup("/db/service/optimistic-lock/before.xml")
    @ExpectedDatabase(value = "/db/service/optimistic-lock/after.xml", assertionMode = NON_STRICT)
    public void oneSuccessAndOneFailBecauseOfOptimisticLock() throws OptimisticLockException, InterruptedException {
        // first read -> second read -> second save -> first try to save but should not
        CountDownLatch secondGetLatch = new CountDownLatch(1);
        CountDownLatch firstSave = new CountDownLatch(1);
        Thread thread = new Thread(() -> {
            try {
                optimisticLockService.tryExecuteWithOptimisticLock(
                        () -> {
                            String editDate = receiptDao.getEditDateAsString(RECEIPT_KEY);
                            secondGetLatch.countDown();
                            return editDate;
                        },
                        () -> {
                            lotDao.createLot(FIRST_LOT);
                            return 10;
                        },
                        (oldValue) -> FIRST_DATE,
                        (oldDate, newDate) -> {
                            try {
                                firstSave.await();
                            } catch (InterruptedException ignored) {
                            }
                            return receiptDao.tryUpdateEditDate(newDate, "TEST2", RECEIPT_KEY, oldDate);
                        }
                );
                assertions.fail("Should fail with OptimisticLockException, but not fails");
            } catch (OptimisticLockException ignored) {
            } catch (Exception e) {
                assertions.fail("Should fail with OptimisticLockException but fails with " + e);
            }
        });
        thread.start();
        String executionResult = optimisticLockService.tryExecuteWithOptimisticLock(
                () -> {
                    try {
                        secondGetLatch.await();
                    } catch (InterruptedException ignored) {
                    }
                    return receiptDao.getEditDateAsString(RECEIPT_KEY);
                },
                () -> {
                    lotDao.createLot(SECOND_LOT);
                    return "Success";
                },
                (oldValue) -> SECOND_DATE,
                (oldDate, newDate) -> {
                    int updateResult = receiptDao.tryUpdateEditDate(newDate, "TEST", RECEIPT_KEY, oldDate);
                    assertions.assertThat(updateResult).isEqualTo(1);
                    firstSave.countDown();
                    return updateResult;
                }
        );
        assertions.assertThat(executionResult).isEqualTo("Success");
        thread.join();
    }

    private static Lot createLot(String storerKey, String sku) {
        return Lot.builder()
                .storerKey(storerKey)
                .sku(sku)
                .lotStatus(LotStatus.OK)
                .lot("0000012346")
                .addWho("TEST")
                .editWho("TEST")
                .lotAggregatedFields(LotAggregatedFields.builder()
                        .quantity(10)
                        .dimensions(SkuDimensions.builder()
                                .cube(BigDecimal.valueOf(5))
                                .grossWeight(BigDecimal.valueOf(2))
                                .netWeight(BigDecimal.valueOf(1))
                                .build())
                        .build())
                .build();
    }
}
