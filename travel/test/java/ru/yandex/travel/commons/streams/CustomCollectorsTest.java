package ru.yandex.travel.commons.streams;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.javamoney.moneta.Money;
import org.junit.Test;

import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomCollectorsTest {
    @Test
    public void testBigDecimalCollector() {
        List<Tuple2<Integer, BigDecimal>> items = List.of(
                Tuple2.tuple(1, BigDecimal.valueOf(12)),
                Tuple2.tuple(2, BigDecimal.valueOf(34)),
                Tuple2.tuple(1, BigDecimal.valueOf(5)),
                Tuple2.tuple(2, BigDecimal.valueOf(1020)),
                Tuple2.tuple(3, BigDecimal.valueOf(42))
        );
        Map<Integer, BigDecimal> result = items.stream()
                .collect(Collectors.groupingBy(Tuple2::get1, CustomCollectors.summingBigDecimal(Tuple2::get2)));
        assertThat(result)
                .containsOnlyKeys(1, 2, 3)
                .containsEntry(1, BigDecimal.valueOf(17))  // 12 + 5
                .containsEntry(2, BigDecimal.valueOf(1054)) // 34 + 1020
                .containsEntry(3, BigDecimal.valueOf(42));
    }

    @Test
    public void testMoneyCollector() {
        List<Tuple2<Integer, Money>> items = List.of(
                Tuple2.tuple(1, Money.of(12, ProtoCurrencyUnit.RUB)),
                Tuple2.tuple(2, Money.of(34, ProtoCurrencyUnit.RUB)),
                Tuple2.tuple(1, Money.of(5, ProtoCurrencyUnit.RUB)),
                Tuple2.tuple(2, Money.of(1020, ProtoCurrencyUnit.RUB)),
                Tuple2.tuple(3, Money.of(42, ProtoCurrencyUnit.RUB))
        );
        Map<Integer, Money> result = items.stream()
                .collect(Collectors.groupingBy(Tuple2::get1, CustomCollectors.summingMoney(Tuple2::get2,
                        ProtoCurrencyUnit.RUB)));
        assertThat(result)
                .containsOnlyKeys(1, 2, 3)
                .containsEntry(1, Money.of(17, ProtoCurrencyUnit.RUB))  // 12 + 5
                .containsEntry(2, Money.of(1054, ProtoCurrencyUnit.RUB)) // 34 + 1020
                .containsEntry(3, Money.of(42, ProtoCurrencyUnit.RUB));
    }

}
