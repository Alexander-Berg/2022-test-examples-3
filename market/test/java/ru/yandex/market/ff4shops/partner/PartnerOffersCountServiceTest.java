package ru.yandex.market.ff4shops.partner;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.config.FunctionalTest;
import ru.yandex.market.ff4shops.repository.PartnerOffersCountRepository;

class PartnerOffersCountServiceTest extends FunctionalTest {

    @Autowired
    private PartnerOffersCountService partnerOffersCountService;

    @Autowired
    private PartnerOffersCountRepository partnerOffersCountRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DbUnitDataSet(before = "partnerOffersCountServiceTest.before.csv")
    void testUpdateWithLock() throws InterruptedException, ExecutionException {
        var partnerId = 1L;
        var expected = 20;
        var priorityTransactionCountDown = new CountDownLatch(1);
        var awaitTransactionCountDown = new CountDownLatch(1);
        var feature1 = CompletableFuture.runAsync(() -> {
            transactionTemplate.execute((d) -> {
                var currentRowNum = partnerOffersCountService.getLastRowNum(partnerId);
                priorityTransactionCountDown.countDown();
                currentRowNum.setCount(currentRowNum.getCount() + 3);
                partnerOffersCountService.updateOfferCount(currentRowNum);
                try {
                    awaitTransactionCountDown.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return null;
            });
        });
      var feature2 = CompletableFuture.runAsync(() -> {
          //Ждем пока первая транзакция не возьмет лок на базу
          try {
              priorityTransactionCountDown.await();
          } catch (InterruptedException e) {
              throw new RuntimeException(e);
          }
          transactionTemplate.execute((d) -> {
            if (!checkLock(partnerId)) {
                Assertions.fail("Lock is not work");
            }
            awaitTransactionCountDown.countDown();
            var currentRowNum = partnerOffersCountService.getLastRowNum(partnerId);
            currentRowNum.setCount(currentRowNum.getCount() * 5);
            partnerOffersCountService.updateOfferCount(currentRowNum);
            return null;
        });
      });
      CompletableFuture.allOf(feature1, feature2).thenAccept(f -> {
          var v = partnerOffersCountRepository.findById(1L).get();
          //Если лок работает, то значение в базе будет 20 (1 транзакция: 1 + 3, вторая транзакция: 4 * 5)
          Assertions.assertEquals(expected, v.getCount());
      }).get();
    }

    private boolean checkLock(long partnerId) {
        return jdbcTemplate.queryForObject(
                "select count from ff4shops.partner_offers_count where partner_id = ? FOR UPDATE SKIP LOCKED;",
                Integer.class, partnerId) == null;
    }
}
