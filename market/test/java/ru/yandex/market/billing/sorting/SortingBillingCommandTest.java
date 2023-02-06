package ru.yandex.market.billing.sorting;


import java.io.PrintWriter;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.billing.service.environment.EnvironmentAwareDateValidationService;
import ru.yandex.market.core.calendar.DatePeriod;
import ru.yandex.market.core.partner.PartnerDao;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Тесты команды запуска переобиливания сортировки.
 * {@link SortingBillingCommand}
 */
@ExtendWith(MockitoExtension.class)
public class SortingBillingCommandTest {
    private static final String COMMAND_NAME = "recalculate-sorting-billing";
    private static final String FROM_DATE_OPTION = "from-date";
    private static final long FIRST_SUPPLIER = 123;
    private static final long SECOND_SUPPLIER = 45;
    private static final String[] SUPPLIERS = {String.valueOf(FIRST_SUPPLIER), String.valueOf(SECOND_SUPPLIER)};
    private static final Instant DATE_2019_04_01 = LocalDate.of(2019, Month.APRIL, 1)
            .atStartOfDay(ZoneId.systemDefault()).toInstant();
    private static final Instant DATE_2019_04_30 = LocalDate.of(2019, Month.APRIL, 30)
            .atStartOfDay(ZoneId.systemDefault()).toInstant();

    @Mock
    private PartnerDao partnerDao;

    @Mock
    private SortingBillingService sortingBillingService;

    @Mock
    private EnvironmentAwareDateValidationService environmentAwareDateValidationService;

    @Mock
    private Terminal terminal;

    private SortingBillingCommand sortingBillingCommand;
    private CommandInvocation commandInvocation;

    private static Stream<Arguments> getValidCases() {
        return Stream.of(
                Arguments.of(
                        DATE_2019_04_30,
                        "2019-04-01",
                        DatePeriod.of(LocalDate.of(2019, 4, 1), 29),
                        "Переобиливание с начала текущего месяца"
                ),
                Arguments.of(
                        DATE_2019_04_30,
                        "2019-04-29",
                        DatePeriod.of(LocalDate.of(2019, 4, 29), 1),
                        "Переобиллить только вчерашний день"
                ),
                Arguments.of(
                        DATE_2019_04_01,
                        "2019-03-01",
                        DatePeriod.of(LocalDate.of(2019, 3, 1), 31),
                        "Если сегодня первое число месяца, то еще можно переобилить за прошлый месяц."
                )
        );
    }

    private void init(Instant fixedInstant, String fromDate) {
        sortingBillingCommand = new SortingBillingCommand(
                sortingBillingService,
                partnerDao,
                environmentAwareDateValidationService,
                Clock.fixed(fixedInstant, ZoneId.systemDefault())
        );

        commandInvocation = new CommandInvocation(
                COMMAND_NAME,
                SUPPLIERS,
                Map.of(FROM_DATE_OPTION, fromDate)
        );

        when(terminal.getWriter()).thenReturn(new PrintWriter(System.out));
        when(terminal.confirm(anyString())).thenReturn(true);

        when(partnerDao.existingPartnerId(Set.of(FIRST_SUPPLIER, SECOND_SUPPLIER)))
                .thenReturn(Set.of(FIRST_SUPPLIER, SECOND_SUPPLIER));
    }

    @ParameterizedTest(name = "{3}")
    @DisplayName("Успешные кейсы выполнения команды")
    @MethodSource("getValidCases")
    void test_executeCommand(
            Instant timeOfTest,
            String fromDate,
            DatePeriod expectedDatePeriod,
            String description
    ) {
        init(timeOfTest, fromDate);

        sortingBillingCommand.executeCommand(commandInvocation, terminal);

        expectedDatePeriod.forEach(localDate -> verify(sortingBillingService).processForPartners(
                eq(localDate),
                eq(Set.of(FIRST_SUPPLIER, SECOND_SUPPLIER))
        ));

        verifyNoMoreInteractions(sortingBillingService);
    }
}

