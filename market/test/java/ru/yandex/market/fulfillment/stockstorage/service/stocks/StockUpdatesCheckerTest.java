package ru.yandex.market.fulfillment.stockstorage.service.stocks;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.fulfillment.stockstorage.domain.entity.Stock;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.StockAmount;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.StockType;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StockUpdatesCheckerTest {

    private final StockUpdatesChecker checker = new StockUpdatesChecker();

    @Nonnull
    private static Stream<Arguments> sourceAvailability() {
        return Stream.of(
                //1
                Arguments.of(
                        ImmutableMap.of(
                                StockType.FIT, getStock(100, 50)
                        ),
                        ImmutableMap.of(
                                StockType.FIT, getStockAmount(51)
                        ),
                        false
                ),
                //2
                Arguments.of(
                        ImmutableMap.of(
                                StockType.FIT, getStock(100, 100)
                        ),
                        ImmutableMap.of(
                                StockType.FIT, getStockAmount(101)
                        ),
                        true
                ),
                //3
                Arguments.of(
                        ImmutableMap.of(
                                StockType.FIT, getStock(100, 100),
                                StockType.PREORDER, getStock(3, 2)
                        ),
                        ImmutableMap.of(StockType.FIT, getStockAmount(101)),
                        false
                ),
                //4
                Arguments.of(
                        ImmutableMap.of(
                                StockType.FIT, getStock(100, 100),
                                StockType.PREORDER, getStock(3, 3)
                        ),
                        ImmutableMap.of(
                                StockType.PREORDER, getStockAmount(4)
                        ),
                        true
                ),
                //5
                Arguments.of(
                        ImmutableMap.of(
                                StockType.FIT, getStock(100, 50),
                                StockType.PREORDER, getStock(3, 3)
                        ),
                        ImmutableMap.of(
                                StockType.FIT, getStockAmount(51),
                                StockType.PREORDER, getStockAmount(4)
                        ),
                        true
                ),
                //6
                Arguments.of(
                        ImmutableMap.of(
                                StockType.FIT, getStock(100, 50),
                                StockType.PREORDER, getStock(3, 1)
                        ),
                        ImmutableMap.of(
                                StockType.FIT, getStockAmount(50),
                                StockType.PREORDER, getStockAmount(4)
                        ),
                        false
                ),
                //7
                Arguments.of(
                        ImmutableMap.of(
                                StockType.FIT, getStock(100, 100),
                                StockType.PREORDER, getStock(3, 1)
                        ),
                        ImmutableMap.of(
                                StockType.DEFECT, getStockAmount(50)
                        ),
                        false
                )
        );
    }

    @Nonnull
    private static Stream<Arguments> sourceAvailableAmount() {
        return Stream.of(
                //1
                Arguments.of(
                        ImmutableMap.of(
                                StockType.FIT, getStock(100, 50)
                        ),
                        ImmutableMap.of(
                                StockType.FIT, getStockAmount(51)
                        ),
                        true
                ),
                //2
                Arguments.of(
                        ImmutableMap.of(
                                StockType.FIT, getStock(100, 100)
                        ),
                        ImmutableMap.of(
                                StockType.FIT, getStockAmount(101)
                        ),
                        true
                ),
                //3
                Arguments.of(
                        ImmutableMap.of(
                                StockType.FIT, getStock(100, 100),
                                StockType.PREORDER, getStock(3, 2)
                        ),
                        ImmutableMap.of(StockType.FIT, getStockAmount(101)),
                        false
                ),
                //4
                Arguments.of(
                        ImmutableMap.of(
                                StockType.FIT, getStock(100, 100),
                                StockType.PREORDER, getStock(3, 3)
                        ),
                        ImmutableMap.of(
                                StockType.PREORDER, getStockAmount(4)
                        ),
                        true
                ),
                //5
                Arguments.of(
                        ImmutableMap.of(
                                StockType.FIT, getStock(100, 50),
                                StockType.PREORDER, getStock(3, 3)
                        ),
                        ImmutableMap.of(
                                StockType.FIT, getStockAmount(51),
                                StockType.PREORDER, getStockAmount(4)
                        ),
                        true
                ),
                //6
                Arguments.of(
                        ImmutableMap.of(
                                StockType.FIT, getStock(100, 50),
                                StockType.PREORDER, getStock(3, 1)
                        ),
                        ImmutableMap.of(
                                StockType.FIT, getStockAmount(50),
                                StockType.PREORDER, getStockAmount(4)
                        ),
                        true
                ),
                //7
                Arguments.of(
                        ImmutableMap.of(
                                StockType.FIT, getStock(100, 50),
                                StockType.PREORDER, getStock(3, 1)
                        ),
                        ImmutableMap.of(
                                StockType.FIT, getStockAmount(50),
                                StockType.PREORDER, getStockAmount(3)
                        ),
                        false
                ),
                //8
                Arguments.of(
                        ImmutableMap.of(
                                StockType.FIT, getStock(100, 100),
                                StockType.PREORDER, getStock(3, 1)
                        ),
                        ImmutableMap.of(
                                StockType.DEFECT, getStockAmount(50)
                        ),
                        false
                )
        );
    }

    private static StockAmount getStockAmount(int amount) {
        return new StockAmount(amount, LocalDateTime.now());
    }

    private static Stock getStock(int amount, int freezeAmount) {
        Stock stock = new Stock();
        stock.setAmount(amount);
        stock.setFreezeAmount(freezeAmount);
        return stock;
    }

    @ParameterizedTest(name = "[{index}]")
    @MethodSource("sourceAvailability")
    public void isAvailabilityChanged(Map<StockType, Stock> existingStocks,
                                      Map<StockType, StockAmount> stocksToUpdate,
                                      boolean expected) {

        boolean actual = checker.isAvailabilityChanged(existingStocks, stocksToUpdate);
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "[{index}]")
    @MethodSource("sourceAvailableAmount")
    public void isAvailableAmountChanged(Map<StockType, Stock> existingStocks,
                                         Map<StockType, StockAmount> stocksToUpdate,
                                         boolean expected) {

        boolean actual = checker.isAvailableAmountChanged(existingStocks, stocksToUpdate);
        assertEquals(expected, actual);
    }
}
