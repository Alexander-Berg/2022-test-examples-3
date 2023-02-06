package ru.yandex.market.billing.core.factoring;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.core.FunctionalTest;
import ru.yandex.market.billing.core.OperatingUnit;
import ru.yandex.market.billing.core.Platform;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyIterable;

@DbUnitDataSet(before = "ContractPayoutFrequencyDaoTest.schedules.before.csv")
class ContractPayoutFrequencyDaoTest extends FunctionalTest {

    @Autowired
    ContractPayoutFrequencyDao dao;

    @Test
    @DbUnitDataSet(
            before = "ContractPayoutFrequencyDaoTest.upsertContractFrequency.before.csv",
            after = "ContractPayoutFrequencyDaoTest.upsertContractFrequency.after.csv"
    )
    void upsertFrequencies() {
        dao.upsertFrequencies(getFrequenciesTestData());
    }

    @Test
    @DbUnitDataSet(
            before = "ContractPayoutFrequencyDaoTest.upsertContractFrequency.before.csv",
            after = "ContractPayoutFrequencyDaoTest.upsertContractFrequency.before.csv"
    )
    void upsertFrequenciesEmptyList() {
        dao.upsertFrequencies(Collections.emptyList());
    }

    @DisplayName("Should find payout frequencies for list of contracts in period [from, to)")
    @ParameterizedTest(name = "{0}: contracts={1} from={2} to={3}")
    @MethodSource("findFrequenciesParametersProvider")
    @DbUnitDataSet(before = "ContractPayoutFrequencyDaoTest.findFrequencies.before.csv")
    void findFrequencies(String description, List<Long> contractIds, LocalDate from, LocalDate to,
                         List<ContractPayoutFrequency> expected) {
        List<ContractPayoutFrequency> actual = dao.findFrequencies(contractIds, from, to,
                List.of(Platform.YANDEX_MARKET, Platform.GLOBAL_MARKET));
        assertThat(actual, containsInAnyOrder(expected.toArray()));
    }

    @Test
    @DbUnitDataSet(before = "ContractPayoutFrequencyDaoTest.getDefaultFrequencies.before.csv")
    void getDefaultFrequencies() {
        var records = getDefaultFrequenciesTestData().toArray();
        assertThat(dao.getDefaultFrequencies(LocalDate.parse("2021-01-01"), LocalDate.parse("2021-09-01"),
                        List.of(Platform.YANDEX_MARKET)), emptyIterable());
        assertThat(dao.getDefaultFrequencies(LocalDate.parse("2021-01-01"), LocalDate.parse("2021-10-01"),
                        List.of(Platform.YANDEX_MARKET)), containsInAnyOrder(records[0]));
        assertThat(dao.getDefaultFrequencies(LocalDate.parse("2021-01-01"), LocalDate.parse("2021-12-01"),
                        List.of(Platform.YANDEX_MARKET)), containsInAnyOrder(records[0], records[1]));
        assertThat(dao.getDefaultFrequencies(LocalDate.parse("2021-09-01"), LocalDate.parse("2021-12-01"),
                        List.of(Platform.YANDEX_MARKET)), containsInAnyOrder(records[0], records[1]));
        assertThat(dao.getDefaultFrequencies(LocalDate.parse("2021-10-01"), LocalDate.parse("2021-12-01"),
                        List.of(Platform.YANDEX_MARKET)), containsInAnyOrder(records[0], records[1]));
        assertThat(dao.getDefaultFrequencies(LocalDate.parse("2021-11-01"), LocalDate.parse("2021-12-01"),
                        List.of(Platform.YANDEX_MARKET)), containsInAnyOrder(records[1]));
        assertThat(dao.getDefaultFrequencies(LocalDate.parse("2022-01-01"), LocalDate.parse("2023-01-01"),
                        List.of(Platform.YANDEX_MARKET)), containsInAnyOrder(records[1]));
    }

    @Test
    @DbUnitDataSet(before = "ContractPayoutFrequencyDaoTest.getEndlessFrequencyRecords.before.csv")
    void getEndlessFrequencyRecords() {
        var records = getFrequenciesTestData().toArray();
        assertThat(dao.getEndlessFrequencyRecords(List.of(), List.of(Platform.YANDEX_MARKET)), emptyIterable());
        assertThat(dao.getEndlessFrequencyRecords(List.of(4897321L, 123L), List.of(Platform.YANDEX_MARKET)),
                containsInAnyOrder(records[1]));
        assertThat(dao.getEndlessFrequencyRecords(List.of(4897321L, 123L, 9235840L), List.of(Platform.YANDEX_MARKET,
                        Platform.GLOBAL_MARKET)),
                containsInAnyOrder(records[1], records[2]));
    }

    @Test
    @DisplayName("Падаем с NPE если забыли проставить org_id.")
    void shouldSaveDefaultValueWhenNoOrgIdGiven() {
        Assertions.assertThrows(
                NullPointerException.class,
                () -> dao.upsertFrequencies(getFrequencyWithoutOrgId())
        );
    }

    @Test
    @DisplayName("Падаем с NPE если забыли проставить платформу.")
    void shouldSaveDefaultPlatformWhenNotSpecified() {
        Assertions.assertThrows(
                NullPointerException.class,
                () -> dao.upsertFrequencies(getFrequencyWithoutPlatform())
        );
    }

    private static Stream<Arguments> findFrequenciesParametersProvider() {
        var records = getFrequenciesTestData().toArray();
        return Stream.of(
                Arguments.of(
                        "Period before frequencies were set",
                        List.of(4897321L, 123L, 9235840L),
                        LocalDate.parse("2021-08-01"),
                        LocalDate.parse("2021-09-01"),
                        List.of()
                ),
                Arguments.of(
                        "Empty contract ids list",
                        List.of(),
                        LocalDate.parse("2021-08-01"),
                        LocalDate.parse("2021-12-01"),
                        List.of()
                ),
                Arguments.of(
                        "Frequency record with end date, start date before start date of period",
                        List.of(4897321L, 123L, 9235840L),
                        LocalDate.parse("2021-08-01"),
                        LocalDate.parse("2021-10-01"),
                        List.of(records[0])
                ),
                Arguments.of(
                        "Frequency record with end date, start date equals start date of period",
                        List.of(4897321L, 123L, 9235840L),
                        LocalDate.parse("2021-09-01"),
                        LocalDate.parse("2021-10-01"),
                        List.of(records[0])
                ),
                Arguments.of(
                        "Two frequency records for one contract",
                        List.of(4897321L, 123L, 9235840L),
                        LocalDate.parse("2021-09-01"),
                        LocalDate.parse("2021-11-01"),
                        List.of(records[0], records[1])
                ),
                Arguments.of(
                        "All frequency records",
                        List.of(4897321L, 123L, 9235840L),
                        LocalDate.parse("2021-09-01"),
                        LocalDate.parse("2021-12-01"),
                        List.of(records[0], records[1], records[2])
                ),
                Arguments.of(
                        "Filter by contracts",
                        List.of(4897321L, 123L),
                        LocalDate.parse("2021-09-01"),
                        LocalDate.parse("2021-12-01"),
                        List.of(records[0], records[1])
                ),
                Arguments.of(
                        "Frequency records without end date",
                        List.of(4897321L, 123L, 9235840L),
                        LocalDate.parse("2021-10-01"),
                        LocalDate.parse("2021-12-01"),
                        List.of(records[1], records[2])
                ),
                Arguments.of(
                        "Frequency records without end date, period in the distant future",
                        List.of(4897321L, 123L, 9235840L),
                        LocalDate.parse("2022-01-01"),
                        LocalDate.parse("2022-05-01"),
                        List.of(records[1], records[2])
                )
        );
    }

    private static List<ContractPayoutFrequency> getFrequenciesTestData() {
        return List.of(
                ContractPayoutFrequency.builder()
                        .setContractId(4897321)
                        .setStartDate(LocalDate.parse("2021-09-01"))
                        .setFrequency(PayoutFrequency.WEEKLY)
                        .setEndDate(LocalDate.parse("2021-10-01"))
                        .setUpdatedAt(Instant.parse("2021-09-10T10:10:10Z"))
                        .setOrgId(OperatingUnit.YANDEX_MARKET)
                        .setPlatform(Platform.YANDEX_MARKET)
                        .build(),
                ContractPayoutFrequency.builder()
                        .setContractId(4897321)
                        .setStartDate(LocalDate.parse("2021-10-01"))
                        .setFrequency(PayoutFrequency.DAILY)
                        .setUpdatedAt(Instant.parse("2021-09-10T10:10:10Z"))
                        .setOrgId(OperatingUnit.YANDEX_MARKET)
                        .setPlatform(Platform.YANDEX_MARKET)
                        .build(),
                ContractPayoutFrequency.builder()
                        .setContractId(9235840)
                        .setStartDate(LocalDate.parse("2021-11-01"))
                        .setFrequency(PayoutFrequency.BI_WEEKLY)
                        .setUpdatedAt(Instant.parse("2021-10-10T10:10:10Z"))
                        .setOrgId(OperatingUnit.YANDEX_MARKET_ISRAEL)
                        .setPlatform(Platform.GLOBAL_MARKET)
                        .build()
        );
    }


    private static List<ContractPayoutFrequency> getDefaultFrequenciesTestData() {
        return List.of(
                ContractPayoutFrequency.builder()
                        .setContractId(0)
                        .setStartDate(LocalDate.parse("2021-09-01"))
                        .setFrequency(PayoutFrequency.DAILY)
                        .setEndDate(LocalDate.parse("2021-11-01"))
                        .setUpdatedAt(Instant.parse("2021-09-01T10:10:10Z"))
                        .setOrgId(OperatingUnit.YANDEX_MARKET)
                        .setPlatform(Platform.YANDEX_MARKET)
                        .build(),
                ContractPayoutFrequency.builder()
                        .setContractId(0)
                        .setStartDate(LocalDate.parse("2021-11-01"))
                        .setFrequency(PayoutFrequency.BI_WEEKLY)
                        .setUpdatedAt(Instant.parse("2021-09-01T10:10:10Z"))
                        .setOrgId(OperatingUnit.YANDEX_MARKET)
                        .setPlatform(Platform.YANDEX_MARKET)
                        .build()
        );
    }

    private static List<ContractPayoutFrequency> getFrequencyWithoutOrgId() {
        return List.of(
                ContractPayoutFrequency.builder()
                        .setContractId(0)
                        .setStartDate(LocalDate.parse("2021-09-01"))
                        .setFrequency(PayoutFrequency.DAILY)
                        .setEndDate(LocalDate.parse("2021-11-01"))
                        .setUpdatedAt(Instant.parse("2021-09-01T10:10:10Z"))
                        .build()
        );
    }

    private static List<ContractPayoutFrequency> getFrequencyWithoutPlatform() {
        return List.of(
                ContractPayoutFrequency.builder()
                        .setContractId(0)
                        .setStartDate(LocalDate.parse("2021-09-01"))
                        .setFrequency(PayoutFrequency.DAILY)
                        .setEndDate(LocalDate.parse("2021-11-01"))
                        .setUpdatedAt(Instant.parse("2021-09-01T10:10:10Z"))
                        .setOrgId(OperatingUnit.YANDEX_MARKET)
                        .build()
        );
    }
}
