package ru.yandex.market.billing.distribution.command;

import java.io.PrintWriter;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.billing.distribution.share.DistributionClidDao;
import ru.yandex.market.billing.distribution.share.DistributionShareCalculationService;
import ru.yandex.market.core.calendar.DatePeriod;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Тесты команды для перерасчета вознаграждений для партнеров дистрибуции по клидам.
 * {@link DistributionShareCalculationCommand}
 */
@ExtendWith(MockitoExtension.class)
class DistributionShareCalculationCommandTest {

    private static final String COMMAND_NAME = "recalculate-distribution-share";
    private static final String FROM_DATE_OPTION = "from-date";
    private static final Long CLID_1 = 1L;
    private static final Long CLID_2 = 2L;
    private static final String[] CLIDS = {String.valueOf(CLID_1), String.valueOf(CLID_2)};
    private static final Instant DATE_2021_05_31 = LocalDate.of(2021, Month.MAY, 31)
            .atStartOfDay(ZoneId.systemDefault()).toInstant();


    @Mock
    private DistributionShareCalculationService distributionShareCalculationService;
    @Mock
    private DistributionClidDao distributionClidDao;

    @Mock
    private Terminal terminal;

    private DistributionShareCalculationCommand distributionShareCalculationCommand;
    private CommandInvocation commandInvocation;

    private static Stream<Arguments> getValidCases() {
        return Stream.of(
                Arguments.of(
                        DATE_2021_05_31,
                        "2021-05-01",
                        DatePeriod.of(LocalDate.of(2021, 5, 1), 30),
                        "Перерасчет с начала месяца"
                ),
                Arguments.of(
                        DATE_2021_05_31,
                        "2021-05-30",
                        DatePeriod.of(LocalDate.of(2021, 5, 30), 1),
                        "Перерасчет вчерашнего дня"
                )
        );
    }

    private static Stream<Arguments> getNotValidCases() {
        return Stream.of(
                Arguments.of(
                        DATE_2021_05_31,
                        "",
                        "Require --from-date option",
                        "Перерасчет без даты начала",
                        CLIDS
                ),
                Arguments.of(
                        DATE_2021_05_31,
                        null,
                        "Option's value is empty. Try this format: --from-date=YYYY-MM-DD",
                        "Перерасчет с датой начала == null",
                        CLIDS
                ),
                Arguments.of(
                        DATE_2021_05_31,
                        "2021",
                        "java.time.format.DateTimeParseException",
                        "Перерасчет с датой начала в неверном формате",
                        CLIDS
                ),
                Arguments.of(
                        DATE_2021_05_31,
                        "2021-05-01",
                        "Some clids don't exist:",
                        "Перерасчет для клидов которых нет",
                        CLIDS
                ),
                Arguments.of(
                        DATE_2021_05_31,
                        "2021-05-01",
                        "Expected at least 1 argument with clid",
                        "Перерасчет для пустого списка клидов",
                        new String[]{}
                ),
                Arguments.of(
                        DATE_2021_05_31,
                        "2021-05-01",
                        "Error while parsing arguments",
                        "Перерасчет для кривого списка клидов",
                        new String[]{"abc"}
                )
        );
    }

    private void init(Instant fixedInstant, String fromDate) {
        init(fixedInstant, fromDate, true, CLIDS);
    }

    private void init(Instant fixedInstant, String fromDate, boolean withClids, String[] clids) {
        distributionShareCalculationCommand = new DistributionShareCalculationCommand(
                distributionShareCalculationService, distributionClidDao, Clock.fixed(fixedInstant, ZoneId.systemDefault())
        );
        Map<String, String> options = new HashMap<>();
        if (fromDate == null || !fromDate.isEmpty()) {
            options.put(FROM_DATE_OPTION, fromDate);
        }
        commandInvocation = new CommandInvocation(COMMAND_NAME, clids, options);

        when(terminal.getWriter()).thenReturn(new PrintWriter(System.out));
        if (withClids) {
            when(distributionClidDao.loadClids(any(), any())).thenReturn(List.of(CLID_1, CLID_2));
        }
    }

    @ParameterizedTest(name = "{3}")
    @DisplayName("Успешные кейсы выполнения команды")
    @MethodSource("getValidCases")
    void testSuccessCommand(
            Instant timeOfTest,
            String fromDate,
            DatePeriod expectedDatePeriod,
            String description
    ) {
        init(timeOfTest, fromDate);

        distributionShareCalculationCommand.executeCommand(commandInvocation, terminal);

        expectedDatePeriod.forEach(date -> verify(distributionShareCalculationService).recalculateShareForClids(
                eq(date),
                eq(Set.of(CLID_1, CLID_2))
        ));

        verifyNoMoreInteractions(distributionShareCalculationService);
    }

    @ParameterizedTest(name = "{3}")
    @DisplayName("Неуспешные кейсы выполнения команды")
    @MethodSource("getNotValidCases")
    void testNowSuccessCommand(
            Instant timeOfTest,
            String fromDate,
            String exceptionMessage,
            String description,
            String[] clids
    ) {
        init(timeOfTest, fromDate, false, clids);
        when(distributionClidDao.loadClids(any(), any())).thenReturn(List.of(CLID_1));

        Exception exception = Assertions.assertThrows(
                Exception.class,
                () -> distributionShareCalculationCommand.executeCommand(commandInvocation, terminal)
        );
        assertThat(exception.getMessage(), Matchers.startsWith(exceptionMessage));

        verifyZeroInteractions(distributionShareCalculationService);
    }

}
