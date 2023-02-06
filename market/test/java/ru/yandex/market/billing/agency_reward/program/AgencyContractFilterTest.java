package ru.yandex.market.billing.agency_reward.program;

import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.hamcrest.Matcher;
import org.hamcrest.core.CombinableMatcher;
import org.hamcrest.core.IsSame;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.billing.agency_reward.AgencyScale;

import static java.time.Month.FEBRUARY;
import static java.time.Month.JANUARY;
import static java.time.Month.MARCH;
import static java.time.Month.MAY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;


/**
 * Тесты для {@link AgencyContractFilter}.
 *
 * @author vbudnev
 */
class AgencyContractFilterTest {

    private static final YearMonth D_2019_02 = YearMonth.of(2019, FEBRUARY);
    private static final ProgramAgencyInfo INFO_01_01__01_31 = buildInfo(1, JANUARY, 1, JANUARY, 31);
    private static final ProgramAgencyInfo INFO_01_01__03_28 = buildInfo(2, JANUARY, 1, MARCH, 28);
    private static final ProgramAgencyInfo INFO_02_01__02_28 = buildInfo(3, FEBRUARY, 1, FEBRUARY, 28);
    private static final ProgramAgencyInfo INFO_02_01__02_15 = buildInfo(4, FEBRUARY, 1, FEBRUARY, 15);
    private static final ProgramAgencyInfo INFO_02_20__02_28 = buildInfo(4, FEBRUARY, 20, FEBRUARY, 28);
    private static final ProgramAgencyInfo INFO_01_01__02_15 = buildInfo(5, JANUARY, 1, FEBRUARY, 15);
    private static final ProgramAgencyInfo INFO_03_01__03_31 = buildInfo(6, MARCH, 1, MARCH, 31);
    private static final ProgramAgencyInfo INFO_05_01__05_31 = buildInfo(7, MAY, 1, MAY, 31);
    private static final ProgramAgencyInfo INFO_01_01__05_31_AG_2000 = buildInfo(2000, 8, JANUARY, 1, MAY, 31);
    private static final ProgramAgencyInfo INFO_01_01__01_31_AG_3000 = buildInfo(3000, 9, JANUARY, 1, JANUARY, 31);
    private static final ProgramAgencyInfo INFO_02_05__02_28_AG_3000 = buildInfo(3000, 10, FEBRUARY, 5, FEBRUARY, 28);

    static Stream<Arguments> args() {
        return Stream.of(
                Arguments.of(
                        "Контракт, начавшийся до и закончивший действие в середине целевого месяца",
                        List.of(
                                INFO_01_01__02_15,
                                INFO_05_01__05_31 // для фона в будущем
                        ),
                        D_2019_02,
                        CombinableMatcher.both(hasSize(1))
                                .and(contains(IsSame.sameInstance(INFO_01_01__02_15)))
                ),
                Arguments.of(
                        "Контракт, начавшийся в середине месяца и захватывающий целевой месяц",
                        List.of(
                                INFO_01_01__02_15,
                                INFO_02_20__02_28,
                                INFO_03_01__03_31 // для фона в будущем
                        ),
                        D_2019_02,
                        CombinableMatcher.both(hasSize(1))
                                .and(contains(IsSame.sameInstance(INFO_02_20__02_28)))
                ),
                Arguments.of(
                        "Контракт, действующий на момент целевой даты (несколько клиентов)",
                        List.of(
                                // одно агентство
                                INFO_01_01__01_31, // для фона в прошлом
                                INFO_02_01__02_28,
                                INFO_03_01__03_31, // для фона в будущем
                                // другое агентство
                                INFO_01_01__05_31_AG_2000,
                                // еще одно агентство
                                INFO_01_01__01_31_AG_3000,
                                INFO_02_05__02_28_AG_3000
                        ),
                        D_2019_02,
                        CombinableMatcher.both(hasSize(3))
                                .and(contains(
                                        IsSame.sameInstance(INFO_02_01__02_28),
                                        IsSame.sameInstance(INFO_01_01__05_31_AG_2000),
                                        IsSame.sameInstance(INFO_02_05__02_28_AG_3000)
                                ))
                ),
                Arguments.of(
                        "Целевая может быть в общем случае не последним днем месяца",
                        List.of(
                                INFO_01_01__02_15,
                                INFO_05_01__05_31 // для фона в будущем
                        ),
                        D_2019_02,
                        CombinableMatcher.both(hasSize(1))
                                .and(contains(IsSame.sameInstance(INFO_01_01__02_15)))
                )
        );
    }

    private static ProgramAgencyInfo buildInfo(long contractId, Month mStart, int dStart, Month mEnd, int dEnd) {
        return buildInfo(1001, contractId, mStart, dStart, mEnd, dEnd);
    }

    private static ProgramAgencyInfo buildInfo(
            long clientId,
            long contractId,
            Month mStart,
            int dStart,
            Month mEnd,
            int dEnd
    ) {
        return new ProgramAgencyInfo(
                clientId,
                contractId,
                "eid_" + contractId,
                true,
                AgencyScale.MSC_SPB,
                LocalDate.of(2019, mStart, dStart),
                LocalDate.of(2019, mEnd, dEnd)
        );
    }

    @MethodSource("args")
    @ParameterizedTest(name = "{0}")
    @DisplayName("Поиск контрактов")
    void test_filterContractsByDate(
            String description,
            List<ProgramAgencyInfo> infos,
            YearMonth targetMonth,
            Matcher<Iterable<ProgramAgencyInfo>> matcher
    ) {
        var res = AgencyContractFilter.getActiveContracts(infos, targetMonth);
        // сортировка для удобства сравнения в тестах
        res.sort(Comparator.comparing(ProgramAgencyInfo::getContractId));
        assertThat(res, matcher);
    }

    @DisplayName("Ошибка, если несколько контрактов действуют на момент заданной даты")
    @Test
    void test_filterContractsByDate_multipleActive_then_throw() {
        IllegalStateException ex = Assertions.assertThrows(
                IllegalStateException.class,
                () ->
                        AgencyContractFilter.getActiveContracts(
                                List.of(
                                        INFO_01_01__01_31,
                                        INFO_02_01__02_28,
                                        INFO_01_01__03_28
                                ),
                                D_2019_02
                        )
        );

        assertThat(
                ex.getMessage(),
                is("Found more then one active contract for agency_client_id=1001 for date=2019-02")
        );
    }

    @DisplayName("Ошибка, если есть контракт, с датой начала и конца в целеовм месяце")
    @Test
    void test_filterContractsByDate_errorOnStrangeContract() {
        IllegalStateException ex = Assertions.assertThrows(
                IllegalStateException.class,
                () ->
                        AgencyContractFilter.getActiveContracts(
                                List.of(
                                        INFO_01_01__01_31, // для фона в прошлом
                                        INFO_02_01__02_15  // подозрительный
                                ),
                                D_2019_02
                        )
        );

        assertThat(
                ex.getMessage(),
                is("contract_id=4 starts and ends in the same month 2019-02")
        );
    }
}
