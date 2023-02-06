package ru.yandex.market.logistics.utilizer.service.events.ss;

import java.time.Instant;
import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItem;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SkuStocks;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.StockAmount;
import ru.yandex.market.fulfillment.stockstorage.client.entity.enums.SSStockType;
import ru.yandex.market.logistics.utilizer.base.AbstractContextualTest;
import ru.yandex.market.logistics.utilizer.domain.entity.Sku;
import ru.yandex.market.logistics.utilizer.repo.SkuJpaRepository;

public class SkuStocksEventHandlingServiceTest extends AbstractContextualTest {

    @Autowired
    private SkuStocksEventHandlingService skuStocksEventHandlingService;

    @Autowired
    private SkuJpaRepository skuJpaRepository;

    @Test
    @DatabaseSetup(value = "classpath:fixtures/service/sku-stocks-event-handling/1/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/sku-stocks-event-handling/1/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void handleWithInsertingAllNewEntities() {
        runInExternalTransaction(
                () -> skuStocksEventHandlingService.handle(
                        SkuStocks.builder()
                                .unitId(SSItem.of("sku2", 100501, 172))
                                .updated(Instant.parse("2020-12-20T14:00:00Z"))
                                .typeToStock(Map.of(
                                        SSStockType.DEFECT, StockAmount.of(SSStockType.DEFECT, 3L),
                                        SSStockType.EXPIRED, StockAmount.of(SSStockType.EXPIRED, 5L),
                                        SSStockType.FIT, StockAmount.of(SSStockType.FIT, 200L)
                                ))
                                .build()), false
        );
    }


    @Test
    @DatabaseSetup(value = "classpath:fixtures/service/sku-stocks-event-handling/12/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/sku-stocks-event-handling/12/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    public void handleSkipSkuNotForUtil() {
        runInExternalTransaction(
                () -> skuStocksEventHandlingService.handle(
                        SkuStocks.builder()
                                .unitId(SSItem.of("sku1", 100500, 172))
                                .updated(Instant.parse("2020-12-20T14:00:00Z"))
                                .typeToStock(Map.of(
                                        SSStockType.DEFECT, StockAmount.of(SSStockType.DEFECT, 36L),
                                        SSStockType.EXPIRED, StockAmount.of(SSStockType.EXPIRED, 6L),
                                        SSStockType.FIT, StockAmount.of(SSStockType.FIT, 200L)
                                ))
                                .build()), false
        );
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/service/sku-stocks-event-handling/2/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/sku-stocks-event-handling/2/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void handleWithSubtractFromTwoStocksAndAddToAnotherStock() {
        runInExternalTransaction(
                () -> skuStocksEventHandlingService.handle(
                        SkuStocks.builder()
                                .unitId(SSItem.of("sku1", 100500, 172))
                                .updated(Instant.parse("2020-12-20T14:00:00Z"))
                                .typeToStock(Map.of(
                                        SSStockType.DEFECT, StockAmount.of(SSStockType.DEFECT, 36L),
                                        SSStockType.EXPIRED, StockAmount.of(SSStockType.EXPIRED, 6L),
                                        SSStockType.FIT, StockAmount.of(SSStockType.FIT, 200L)
                                ))
                                .build()), false
        );
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/service/sku-stocks-event-handling/3/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/sku-stocks-event-handling/3/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void handleWithIgnoreEventForOneStockAndInsertAnotherStock() {
        runInExternalTransaction(
                () -> skuStocksEventHandlingService.handle(
                        SkuStocks.builder()
                                .unitId(SSItem.of("sku1", 100500, 172))
                                .updated(Instant.parse("2020-12-20T14:00:00Z"))
                                .typeToStock(Map.of(
                                        SSStockType.DEFECT, StockAmount.of(SSStockType.DEFECT, 1000_000L),
                                        SSStockType.EXPIRED, StockAmount.of(SSStockType.EXPIRED, 33L),
                                        SSStockType.FIT, StockAmount.of(SSStockType.FIT, 200L)
                                ))
                                .build()), false
        );
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/service/sku-stocks-event-handling/4/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/sku-stocks-event-handling/4/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void handleWithIgnoreEventForOneStockAndUpdateAnotherStock() {
        runInExternalTransaction(
                () -> skuStocksEventHandlingService.handle(
                        SkuStocks.builder()
                                .unitId(SSItem.of("sku1", 100500, 172))
                                .updated(Instant.parse("2020-12-20T14:00:00Z"))
                                .typeToStock(Map.of(
                                        SSStockType.DEFECT, StockAmount.of(SSStockType.DEFECT, 1000_000L),
                                        SSStockType.EXPIRED, StockAmount.of(SSStockType.EXPIRED, 33L),
                                        SSStockType.FIT, StockAmount.of(SSStockType.FIT, 200L)
                                ))
                                .build()), false
        );
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/service/sku-stocks-event-handling/5/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/sku-stocks-event-handling/5/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void onlyNewTransactionRolledBackInCaseOfException() {
        runInExternalTransaction(
                () -> {
                    skuJpaRepository.save(Sku.builder().sku("sku2").warehouseId(171).vendorId(100501L).build());
                    try {
                        skuStocksEventHandlingService.handle(
                                SkuStocks.builder()
                                        .unitId(SSItem.of("sku2", 100501, 172))
                                        .updated(Instant.parse("2020-12-20T14:00:00Z"))
                                        .typeToStock(null)
                                        .build());
                        softly.fail("Should throw exception");
                    } catch (NullPointerException ignored) {
                    } catch (Exception e) {
                        softly.fail("Should catch NPE");
                    }
                }, false
        );
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/empty.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/sku-stocks-event-handling/6/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void handleForUnknownStocksWithZeroAmount() {
        runInExternalTransaction(
                () -> skuStocksEventHandlingService.handle(
                        SkuStocks.builder()
                                .unitId(SSItem.of("sku1", 100500, 172))
                                .updated(Instant.parse("2020-12-20T14:00:00Z"))
                                .typeToStock(Map.of(
                                        SSStockType.DEFECT, StockAmount.of(SSStockType.DEFECT, 0L),
                                        SSStockType.EXPIRED, StockAmount.of(SSStockType.EXPIRED, 0L),
                                        SSStockType.FIT, StockAmount.of(SSStockType.FIT, 200L)
                                ))
                                .build()), false
        );
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/service/sku-stocks-event-handling/7/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/sku-stocks-event-handling/7/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void handleStocksWithUpdateOnlyDateAndWithoutUpdatesAmount() {
        runInExternalTransaction(
                () -> skuStocksEventHandlingService.handle(
                        SkuStocks.builder()
                                .unitId(SSItem.of("sku1", 100500, 172))
                                .updated(Instant.parse("2020-12-20T14:00:00Z"))
                                .typeToStock(Map.of(
                                        SSStockType.DEFECT, StockAmount.of(SSStockType.DEFECT, 20L),
                                        SSStockType.EXPIRED, StockAmount.of(SSStockType.EXPIRED, 33L),
                                        SSStockType.FIT, StockAmount.of(SSStockType.FIT, 200L)
                                ))
                                .build()), false
        );
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/service/sku-stocks-event-handling/8/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/sku-stocks-event-handling/8/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void handleWithInsertingNewSkuForAlreadyExistingCycleWithSameStockForOtherSku() {
        runInExternalTransaction(
                () -> skuStocksEventHandlingService.handle(
                        SkuStocks.builder()
                                .unitId(SSItem.of("sku2", 100500, 172))
                                .updated(Instant.parse("2020-12-20T14:00:00Z"))
                                .typeToStock(Map.of(
                                        SSStockType.DEFECT, StockAmount.of(SSStockType.DEFECT, 3L),
                                        SSStockType.EXPIRED, StockAmount.of(SSStockType.EXPIRED, 0L),
                                        SSStockType.FIT, StockAmount.of(SSStockType.FIT, 200L)
                                ))
                                .build()), false
        );
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/service/sku-stocks-event-handling/9/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/sku-stocks-event-handling/9/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void handleWithoutStockTypesToHandle() {
        runInExternalTransaction(
                () -> skuStocksEventHandlingService.handle(
                        SkuStocks.builder()
                                .unitId(SSItem.of("sku2", 100500, 172))
                                .updated(Instant.parse("2020-12-20T14:00:00Z"))
                                .typeToStock(Map.of(
                                        SSStockType.FIT, StockAmount.of(SSStockType.FIT, 200L)
                                ))
                                .build()), false
        );
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/service/sku-stocks-event-handling/10/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/sku-stocks-event-handling/10/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void handleWithNegativeAmountForExistingStock() {
        runInExternalTransaction(
                () -> skuStocksEventHandlingService.handle(
                        SkuStocks.builder()
                                .unitId(SSItem.of("sku1", 100500, 172))
                                .updated(Instant.parse("2020-12-20T14:00:00Z"))
                                .typeToStock(Map.of(
                                        SSStockType.DEFECT, StockAmount.of(SSStockType.DEFECT, -1L)
                                ))
                                .build()), false
        );
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/service/sku-stocks-event-handling/11/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/sku-stocks-event-handling/11/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void handleWithNegativeAmountForNotExistingStock() {
        runInExternalTransaction(
                () -> skuStocksEventHandlingService.handle(
                        SkuStocks.builder()
                                .unitId(SSItem.of("sku1", 100500, 172))
                                .updated(Instant.parse("2020-12-20T14:00:00Z"))
                                .typeToStock(Map.of(
                                        SSStockType.DEFECT, StockAmount.of(SSStockType.DEFECT, -1L)
                                ))
                                .build()), false
        );
    }
}
