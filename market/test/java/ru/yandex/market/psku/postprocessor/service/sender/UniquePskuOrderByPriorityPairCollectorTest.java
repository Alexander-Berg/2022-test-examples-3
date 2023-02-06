package ru.yandex.market.psku.postprocessor.service.sender;

import org.junit.Test;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PairType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.Pair;
import ru.yandex.market.psku.postprocessor.common.util.PairBuilder;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Fedor Dergachev <a href="mailto:dergachevfv@yandex-team.ru"></a>
 */
public class UniquePskuOrderByPriorityPairCollectorTest {

    @Test
    public void whenCollectOk() {
        List<Pair> pairs = Stream.of(
                new PairBuilder().pskuId(1).type(PairType.REPORT).reportPosition(1).reportMatchRate(0.2),
                new PairBuilder().pskuId(1).type(PairType.REPORT).reportPosition(0).reportMatchRate(0.3),
                new PairBuilder().pskuId(1).type(PairType.REPORT).reportPosition(2).reportMatchRate(0.2),
                new PairBuilder().pskuId(2).type(PairType.UC),
                new PairBuilder().pskuId(2).type(PairType.REPORT).reportPosition(2).reportMatchRate(0.3),
                new PairBuilder().pskuId(2).type(PairType.REPORT).reportPosition(1).reportMatchRate(0.4),
                new PairBuilder().pskuId(2).type(PairType.REPORT).reportPosition(0).reportMatchRate(0.5),
                new PairBuilder().pskuId(3).type(PairType.UC),
                new PairBuilder().pskuId(4).type(PairType.REPORT).reportPosition(0).reportMatchRate(0.7),
                new PairBuilder().pskuId(4).type(PairType.REPORT).reportPosition(1).reportMatchRate(0.6),
                new PairBuilder().pskuId(4).type(PairType.REPORT).reportPosition(2).reportMatchRate(0.5),
                new PairBuilder().pskuId(4).type(PairType.UNKNOWN),
                new PairBuilder().pskuId(5).type(PairType.REPORT).reportPosition(1).reportMatchRate(0.1),
                new PairBuilder().pskuId(5).type(PairType.REPORT).reportPosition(2).reportMatchRate(0.1),
                new PairBuilder().pskuId(5).type(PairType.REPORT).reportPosition(0).reportMatchRate(0.2),
                new PairBuilder().pskuId(6).type(PairType.REPORT).reportPosition(0).reportMatchRate(0.9),
                new PairBuilder().pskuId(6).type(PairType.REPORT).reportPosition(1).reportMatchRate(0.8),
                new PairBuilder().pskuId(6).type(PairType.REPORT).reportPosition(2).reportMatchRate(0.7),
                new PairBuilder().pskuId(7).type(PairType.REPORT).reportPosition(0).reportMatchRate(1.0),
                new PairBuilder().pskuId(7).type(PairType.REPORT).reportPosition(1).reportMatchRate(0.9),
                new PairBuilder().pskuId(7).type(PairType.REPORT).reportPosition(2).reportMatchRate(0.8),
                new PairBuilder().pskuId(8).type(PairType.UNKNOWN))
                .map(PairBuilder::build)
                .collect(Collectors.toList());

        Collection<Pair> collectedPairs = new UniquePskuOrderByPriorityPairCollector()
                .collect(5, pairs.iterator());

        List<Pair> expecterPairs = Stream.of(
                new PairBuilder().pskuId(2).type(PairType.UC),
                new PairBuilder().pskuId(3).type(PairType.UC),
                new PairBuilder().pskuId(4).type(PairType.REPORT).reportPosition(0).reportMatchRate(0.7),
                new PairBuilder().pskuId(6).type(PairType.REPORT).reportPosition(0).reportMatchRate(0.9),
                new PairBuilder().pskuId(7).type(PairType.REPORT).reportPosition(0).reportMatchRate(1.0))
                .map(PairBuilder::build)
                .collect(Collectors.toList());

        assertThat(collectedPairs)
                .usingElementComparatorOnFields("pskuId", "type", "reportPosition", "reportMatchRate")
                .containsExactlyInAnyOrderElementsOf(expecterPairs);
    }
}
