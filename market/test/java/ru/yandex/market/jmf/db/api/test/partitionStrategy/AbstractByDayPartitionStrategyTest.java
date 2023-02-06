package ru.yandex.market.jmf.db.api.test.partitionStrategy;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.jmf.db.api.AbstractByDayPartitionStrategy;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.Fqns;


public class AbstractByDayPartitionStrategyTest extends AbstractByDayPartitionStrategy {

    protected AbstractByDayPartitionStrategyTest() {
        super(Fqns.versionOf(Fqn.of("test")), new PartitionDeletionInfo(false, Period.ofDays(1)));
    }

    private static List<Arguments> correctData() {
        return List.of(
                Arguments.of("tbl_ticket_version__partition__2021_07_05", LocalDate.of(2021, 7, 5)),
                Arguments.of("tbl_ticket_version__partition__2021_7_28", LocalDate.of(2021, 7, 28)),
                Arguments.of("tbl_employeedistributionstatus_version__partition__2021_09_15", LocalDate.of(2021, 9, 15))
        );
    }

    private static List<Arguments> incorrectData() {
        return List.of(
                Arguments.of("tbl_ticket_version__partition__2019", NoSuchElementException.class),
                Arguments.of("tbl_ticket_version__partition__2000_0", NoSuchElementException.class),
                Arguments.of("tbl_ticket_version__partition__2000_0_0", DateTimeException.class),
                Arguments.of("tbl_ticket_version__partition__2021_07_OTHER", NumberFormatException.class),
                Arguments.of("tbl_ticket_version__partition__OTHER_2021_07", NumberFormatException.class)
        );
    }

    @ParameterizedTest(name = "{index} {0} => {1}")
    @MethodSource(value = "correctData")
    public void correctPartitionCreationDateTest(String partitionName, LocalDate expected) {
        Assertions.assertEquals(expected, super.partitionCreationDate(partitionName));
    }

    @ParameterizedTest(name = "{index} !({0} => {1})")
    @MethodSource(value = "incorrectData")
    public void incorrectPartitionCreationDateTest(String partitionName, Class<? extends Throwable> expected) {
        Assertions.assertThrows(expected, () -> super.partitionCreationDate(partitionName));
    }

}
