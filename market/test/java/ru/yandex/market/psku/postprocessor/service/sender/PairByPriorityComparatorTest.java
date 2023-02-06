package ru.yandex.market.psku.postprocessor.service.sender;

import org.junit.Test;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PairType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.Pair;
import ru.yandex.market.psku.postprocessor.common.util.PairBuilder;

import java.util.Arrays;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Fedor Dergachev <a href="mailto:dergachevfv@yandex-team.ru"></a>
 */
public class PairByPriorityComparatorTest {

    private Comparator<Pair> comparator =new UniquePskuOrderByPriorityPairCollector.PairByPriorityComparator();

    @Test
    public void testCompareUCType() {
        Pair pairUC = new PairBuilder().pskuId(1).type(PairType.UC).build();

        assertThat(comparator.compare(pairUC, pairUC)).isEqualTo(0);

        Arrays.stream(PairType.values())
                .filter(pairType -> !pairType.equals(PairType.UC))
                .forEach(pairType -> {
                    Pair pairWithType = new PairBuilder().pskuId(2).type(pairType).build();
                    assertThat(comparator.compare(pairUC, pairWithType))
                            .isEqualTo(1);
                });
    }

    @Test
    public void testCompareReportType() {
        Pair pairReport = new PairBuilder().pskuId(1).type(PairType.REPORT).build();
        Pair pairUC = new PairBuilder().pskuId(1).type(PairType.UC).build();

        assertThat(comparator.compare(pairReport, pairReport)).isEqualTo(0);
        assertThat(comparator.compare(pairReport, pairUC)).isEqualTo(-1);

        Arrays.stream(PairType.values())
                .filter(pairType -> !pairType.equals(PairType.UC)
                        && !pairType.equals(PairType.REPORT))
                .forEach(pairType -> {
                    Pair pairWithType = new PairBuilder().pskuId(2).type(pairType).build();
                    assertThat(comparator.compare(pairReport, pairWithType))
                            .isEqualTo(1);
                });
    }

    @Test
    public void testCompareUnknownType() {
        Pair pairUnknown = new PairBuilder().pskuId(1).type(PairType.UNKNOWN).build();

        assertThat(comparator.compare(pairUnknown, pairUnknown)).isEqualTo(0);

        Arrays.stream(PairType.values())
                .filter(pairType -> !pairType.equals(PairType.UNKNOWN))
                .forEach(pairType -> {
                    Pair pairWithType = new PairBuilder().pskuId(2).type(pairType).build();
                    assertThat(comparator.compare(pairUnknown, pairWithType))
                            .isEqualTo(-1);
                });
    }

    @Test
    public void testCompareReportPosition() {
        Pair pairPosition0 = new PairBuilder().pskuId(1).type(PairType.REPORT)
                .reportPosition(0)
                .build();

        Pair pairPosition1 = new PairBuilder().pskuId(1).type(PairType.REPORT)
                .reportPosition(1)
                .build();

        assertThat(comparator.compare(pairPosition0, pairPosition1)).isEqualTo(1);
        assertThat(comparator.compare(pairPosition1, pairPosition0)).isEqualTo(-1);
    }

    @Test
    public void testCompareReportMatchRate() {
        Pair pairPosition0Rate05 = new PairBuilder().pskuId(1).type(PairType.REPORT)
                .reportPosition(0).reportMatchRate(0.5)
                .build();

        Pair pairPosition0Rate07 = new PairBuilder().pskuId(1).type(PairType.REPORT)
                .reportPosition(0).reportMatchRate(0.7)
                .build();

        assertThat(comparator.compare(pairPosition0Rate07, pairPosition0Rate05)).isEqualTo(1);
        assertThat(comparator.compare(pairPosition0Rate05, pairPosition0Rate07)).isEqualTo(-1);
    }

    @Test
    public void testCompareReportPositionThenReportMatchRate() {
        Pair pairPosition0Rate05 = new PairBuilder().pskuId(1).type(PairType.REPORT)
                .reportPosition(0).reportMatchRate(0.5)
                .build();

        Pair pairPosition1Rate07 = new PairBuilder().pskuId(1).type(PairType.REPORT)
                .reportPosition(1).reportMatchRate(0.7)
                .build();

        assertThat(comparator.compare(pairPosition0Rate05, pairPosition1Rate07)).isEqualTo(1);
        assertThat(comparator.compare(pairPosition1Rate07, pairPosition0Rate05)).isEqualTo(-1);
    }
}
