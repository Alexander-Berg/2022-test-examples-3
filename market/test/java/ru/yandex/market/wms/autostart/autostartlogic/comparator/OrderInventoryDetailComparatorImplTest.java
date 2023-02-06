package ru.yandex.market.wms.autostart.autostartlogic.comparator;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ru.yandex.market.wms.common.spring.dao.entity.OrderInventoryDetail;
import ru.yandex.market.wms.common.spring.dao.entity.PickSkuLocation;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.common.spring.dao.entity.SkuProperties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static ru.yandex.common.util.math.MathUtils.sign;

public class OrderInventoryDetailComparatorImplTest {

    private final OrderInventoryDetailComparatorImpl orderInventoryDetailComparator =
            new OrderInventoryDetailComparatorImpl();

    @Test
    void doNotCompareByExpirationDateWhenRotateByLotForBoth() {
        OrderInventoryDetail one = makeInventoryDetail("Lot", Instant.parse("2013-12-11T10:09:08.00Z"));
        OrderInventoryDetail another = makeInventoryDetail("Lot", Instant.parse("2013-12-11T10:00:00.00Z"));

        int compareResult = orderInventoryDetailComparator.compare(one, another);

        assertThat(compareResult, equalTo(0));
    }

    @Test
    void doNotCompareByExpirationDateWhenRotateByIsNotSetForBoth() {
        OrderInventoryDetail one = makeInventoryDetail(null, Instant.parse("2013-12-11T10:09:08.00Z"));
        OrderInventoryDetail another = makeInventoryDetail(null, Instant.parse("2013-12-11T10:00:00.00Z"));

        int compareResult = orderInventoryDetailComparator.compare(one, another);

        assertThat(compareResult, equalTo(0));
    }

    @ParameterizedTest
    @CsvSource({"2013-12-11T09:09:08.00Z,-1", "2013-12-11T10:00:00.00Z,0", "2013-12-11T10:09:08.00Z,1"})
    void compareByExpirationDateWhenRotateByExpirationDateForBoth(String expirationDate, int compareResultSign) {
        OrderInventoryDetail one = makeInventoryDetail("Lottable05", Instant.parse(expirationDate));
        OrderInventoryDetail another = makeInventoryDetail("Lottable05", Instant.parse("2013-12-11T10:00:00.00Z"));

        int compareResult = orderInventoryDetailComparator.compare(one, another);

        assertThat(sign(compareResult), equalTo(compareResultSign));
    }

    @Test
    void compareByExpirationDateUsingFarFutureWhenRotateByExpirationDateForBothAndExpirationDateIsNotSet() {
        OrderInventoryDetail one = makeInventoryDetail("Lottable05", null);
        OrderInventoryDetail another = makeInventoryDetail("Lottable05", Instant.parse("2013-12-11T10:00:00.00Z"));

        int compareResult = orderInventoryDetailComparator.compare(one, another);

        assertThat(compareResult, greaterThan(0));
    }

    @Test
    void compareByExpirationDateUsingFarFutureWhenRotateByLotForOneAndRotateByExpirationDateForAnother() {
        OrderInventoryDetail one = makeInventoryDetail("Lot", Instant.parse("2013-12-11T09:00:00.00Z"));
        OrderInventoryDetail another = makeInventoryDetail("Lottable05", Instant.parse("2013-12-11T10:00:00.00Z"));

        int compareResult = orderInventoryDetailComparator.compare(one, another);

        assertThat(compareResult, greaterThan(0));
    }

    @Test
    void compareByExpirationDateUsingFarFutureWhenRotateByLotForOneAndRotateByExpDateAndExpDateIsNotSetForAnother() {
        OrderInventoryDetail one = makeInventoryDetail("Lot", Instant.parse("2013-12-11T09:00:00.00Z"));
        OrderInventoryDetail another = makeInventoryDetail("Lottable05", null);

        int compareResult = orderInventoryDetailComparator.compare(one, another);

        assertThat(compareResult, equalTo(0));
    }

    @ParameterizedTest
    @CsvSource({"MEZONIN_1,-1", "MEZONIN_2,0", "MEZONIN_3,1"})
    void compareByZone(String zone, int compareResultSign) {
        OrderInventoryDetail one = makeInventoryDetail(zone, "170006", "C4-17-0006");
        OrderInventoryDetail another = makeInventoryDetail("MEZONIN_2", "170006", "C4-17-0006");

        int compareResult = orderInventoryDetailComparator.compare(one, another);

        assertThat(sign(compareResult), equalTo(compareResultSign));
    }

    @ParameterizedTest
    @CsvSource({"150009,-1", "160008,0", "170006,1"})
    void compareByLogicalLocation(String logicalLocation, int compareResultSign) {
        OrderInventoryDetail one = makeInventoryDetail("MEZONIN_1", logicalLocation, "Loc-001");
        OrderInventoryDetail another = makeInventoryDetail("MEZONIN_1", "160008", "Loc-001");

        int compareResult = orderInventoryDetailComparator.compare(one, another);

        assertThat(sign(compareResult), equalTo(compareResultSign));
    }

    @ParameterizedTest
    @CsvSource({"1,1", "2,0", "3,-1"})
    void compareByQtyUsingReverseOrdering(int qty, int compareResultSign) {
        OrderInventoryDetail one = makeInventoryDetail(qty);
        OrderInventoryDetail another = makeInventoryDetail(2);

        int compareResult = orderInventoryDetailComparator.compare(one, another);

        assertThat(sign(compareResult), equalTo(compareResultSign));
    }

    @ParameterizedTest
    @CsvSource({"C3-17-0006,-1", "C4-17-0006,0", "C5-17-0006,1"})
    void compareByLoc(String loc, int compareResultSign) {
        OrderInventoryDetail one = makeInventoryDetail("MEZONIN_1", "170006", loc);
        OrderInventoryDetail another = makeInventoryDetail("MEZONIN_1", "170006", "C4-17-0006");

        int compareResult = orderInventoryDetailComparator.compare(one, another);

        assertThat(sign(compareResult), equalTo(compareResultSign));
    }

    @ParameterizedTest
    @CsvSource({"Lot-001,-1", "Lot-002,0", "Lot-003,1"})
    void compareByLot(String lot, int compareResultSign) {
        OrderInventoryDetail one = makeInventoryDetail(lot);
        OrderInventoryDetail another = makeInventoryDetail("Lot-002");

        int compareResult = orderInventoryDetailComparator.compare(one, another);

        assertThat(sign(compareResult), equalTo(compareResultSign));
    }

    @Test
    void compareByExpirationDateFirst() {
        OrderInventoryDetail one = makeInventoryDetail(
                "Lottable05",
                Instant.parse("2013-12-11T10:09:08.00Z"),
                "MEZONIN_1",
                "160008",
                1,
                "C3-17-0006",
                "Lot-001"
        );
        OrderInventoryDetail another = makeInventoryDetail("Lottable05", Instant.parse("2013-12-11T10:00:00.00Z"));

        int compareResult = orderInventoryDetailComparator.compare(one, another);

        assertThat(compareResult, greaterThan(0));
    }

    @Test
    void compareByZoneFirstIfExpirationDatesAreEqual() {
        OrderInventoryDetail one = makeInventoryDetail(
                "Lot",
                null,
                "MEZONIN_3",
                "160008",
                1,
                "C3-17-0006",
                "Lot-001"
        );
        OrderInventoryDetail another = makeInventoryDetail("MEZONIN_2", "170006", "C4-17-0006");

        int compareResult = orderInventoryDetailComparator.compare(one, another);

        assertThat(compareResult, greaterThan(0));
    }

    @Test
    void compareByLogicalLocationFirstIfExpirationDatesAndZonesAreEqual() {
        OrderInventoryDetail one = makeInventoryDetail(
                "Lot",
                null,
                "MEZONIN_2",
                "180004",
                1,
                "C3-17-0006",
                "Lot-001"
        );
        OrderInventoryDetail another = makeInventoryDetail("MEZONIN_2", "170006", "C4-17-0006");

        int compareResult = orderInventoryDetailComparator.compare(one, another);

        assertThat(compareResult, greaterThan(0));
    }

    @Test
    void compareByQtyFirstIfExpirationDatesAndZonesAndLogicalLocationsAreEqual() {
        OrderInventoryDetail one = makeInventoryDetail(
                "Lot",
                null,
                "MEZONIN_2",
                "170006",
                1,
                "C3-17-0006",
                "Lot-001"
        );
        OrderInventoryDetail another = makeInventoryDetail(2);

        int compareResult = orderInventoryDetailComparator.compare(one, another);

        assertThat(compareResult, greaterThan(0));
    }

    @Test
    void compareByLocFirstIfExpirationDatesAndZonesAndLogicalLocationsAndQtysAreEqual() {
        OrderInventoryDetail one = makeInventoryDetail(
                "Lot",
                null,
                "MEZONIN_2",
                "170006",
                2,
                "C5-17-0006",
                "Lot-001"
        );
        OrderInventoryDetail another = makeInventoryDetail("MEZONIN_2", "170006", "C4-17-0006");

        int compareResult = orderInventoryDetailComparator.compare(one, another);

        assertThat(compareResult, greaterThan(0));
    }

    @Test
    void compareByLotIfExpirationDatesAndZonesAndLogicalLocationsAndQtysAndLocsAreEqual() {
        OrderInventoryDetail one = makeInventoryDetail(
                "Lot",
                null,
                "MEZONIN_2",
                "170006",
                2,
                "C4-17-0006",
                "Lot-003"
        );
        OrderInventoryDetail another = makeInventoryDetail("Lot-002");

        int compareResult = orderInventoryDetailComparator.compare(one, another);

        assertThat(compareResult, greaterThan(0));
    }

    OrderInventoryDetail makeInventoryDetail(String rotateBy, Instant expirationDate) {
        return makeInventoryDetail(rotateBy, expirationDate, "MEZONIN_2", "170006", 2, "C4-17-0006", "Lot-002");
    }

    OrderInventoryDetail makeInventoryDetail(String zone, String logicalLocation, String loc) {
        return makeInventoryDetail("Lot", null, zone, logicalLocation, 2, loc, "Lot-002");
    }

    OrderInventoryDetail makeInventoryDetail(int qty) {
        return makeInventoryDetail("Lot", null, "MEZONIN_2", "170006", qty, "C4-17-0006", "Lot-002");
    }

    OrderInventoryDetail makeInventoryDetail(String lot) {
        return makeInventoryDetail("Lot", null, "MEZONIN_2", "170006", 2, "C4-17-0006", lot);
    }

    OrderInventoryDetail makeInventoryDetail(
            String rotateBy,
            Instant expirationDate,
            String zone,
            String logicalLocation,
            int qty,
            String loc,
            String lot
    ) {
        return OrderInventoryDetail.builder()
                .skuProperties(SkuProperties.builder()
                        .packKey("PACK")
                        .cartonGroup("BC1")
                        .rotation("1")
                        .skuRotation("1")
                        .rotateby(rotateBy)
                        .lottable05(expirationDate)
                        .shelfLife(BigDecimal.ZERO)
                        .build()
                )
                .skuId(new SkuId("STORER-001", "SKU-001"))
                .location(PickSkuLocation.builder()
                        .lot(lot)
                        .lottable08("1")
                        .zone(zone)
                        .loc(loc)
                        .logicalLocation(logicalLocation)
                        .build())
                .qty(qty)
                .build();
    }
}
