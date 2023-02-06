package ru.yandex.market.billing.order.returns;

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
import ru.yandex.market.billing.imports.orderservicereturn.model.OrderServiceReturnType;
import ru.yandex.market.billing.order.returns.billing.ReturnOrderBillingService;
import ru.yandex.market.billing.service.environment.EnvironmentAwareDateValidationService;
import ru.yandex.market.core.calendar.DatePeriod;
import ru.yandex.market.core.partner.PartnerDao;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Тесты для команды запуска переобиливания биллинга возврата/невыкупов.
 * {@link ReturnOrderBillingCommand}
 */
@ExtendWith(MockitoExtension.class)
class ReturnOrderBillingCommandTest {
    private static final String COMMAND_NAME = "recalculate-return-order-billing";
    private static final String ORDER_RETURN_TYPE_OPTION = "order-return-type";
    private static final String FROM_DATE_OPTION = "from-date";
    private static final long FIRST_SUPPLIER = 123;
    private static final long SECOND_SUPPLIER = 45;
    private static final String[] SUPPLIERS = {String.valueOf(FIRST_SUPPLIER), String.valueOf(SECOND_SUPPLIER)};
    private static final Instant DATE_2022_04_01 = LocalDate.of(2022, Month.APRIL, 1)
            .atStartOfDay(ZoneId.systemDefault()).toInstant();
    private static final Instant DATE_2022_04_30 = LocalDate.of(2022, Month.APRIL, 30)
            .atStartOfDay(ZoneId.systemDefault()).toInstant();

    @Mock
    private PartnerDao partnerDao;

    @Mock
    private ReturnOrderBillingService returnOrderBillingService;

    @Mock
    private EnvironmentAwareDateValidationService environmentAwareDateValidationService;

    @Mock
    private Terminal terminal;

    private ReturnOrderBillingCommand returnOrderBillingCommand;

    private CommandInvocation commandInvocation;

    private static Stream<Arguments> getValidCases() {
        return Stream.of(
                Arguments.of(
                        DATE_2022_04_30,
                        "2022-04-01",
                        DatePeriod.of(LocalDate.of(2022, 4, 1), 29),
                        OrderServiceReturnType.RETURN,
                        "Переобиливание возвратов с начала текущего месяца"
                ),
                Arguments.of(
                        DATE_2022_04_30,
                        "2022-04-29",
                        DatePeriod.of(LocalDate.of(2022, 4, 29), 1),
                        OrderServiceReturnType.RETURN,
                        "Переобиливание возвратов только за вчерашний день"
                ),
                Arguments.of(
                        DATE_2022_04_01,
                        "2022-03-01",
                        DatePeriod.of(LocalDate.of(2022, 3, 1), 31),
                        OrderServiceReturnType.RETURN,
                        "Если сегодня первое число месяца, то еще можно переобиллить возвраты за прошлый месяц."
                ),
                Arguments.of(
                        DATE_2022_04_30,
                        "2022-04-01",
                        DatePeriod.of(LocalDate.of(2022, 4, 1), 29),
                        OrderServiceReturnType.UNREDEEMED,
                        "Переобиливание возвратов с начала текущего месяца"
                ),
                Arguments.of(
                        DATE_2022_04_30,
                        "2022-04-29",
                        DatePeriod.of(LocalDate.of(2022, 4, 29), 1),
                        OrderServiceReturnType.UNREDEEMED,
                        "Переобиливание невыкупов только за вчерашний день"
                ),
                Arguments.of(
                        DATE_2022_04_01,
                        "2022-03-01",
                        DatePeriod.of(LocalDate.of(2022, 3, 1), 31),
                        OrderServiceReturnType.UNREDEEMED,
                        "Если сегодня первое число месяца, то еще можно переобиллить невыкупы за прошлый месяц."
                )
        );
    }

    private void init(Instant fixedInstant, String fromDate, OrderServiceReturnType returnType, boolean allSuppliers) {
        returnOrderBillingCommand = new ReturnOrderBillingCommand(
                returnOrderBillingService,
                partnerDao,
                environmentAwareDateValidationService,
                Clock.fixed(fixedInstant, ZoneId.systemDefault())
        );

        commandInvocation = new CommandInvocation(
                COMMAND_NAME,
                allSuppliers ? new String[]{} : SUPPLIERS,
                Map.of(FROM_DATE_OPTION, fromDate, ORDER_RETURN_TYPE_OPTION, returnType.name())
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
            OrderServiceReturnType returnType,
            String description
    ) {
        init(timeOfTest, fromDate, returnType, false);

        returnOrderBillingCommand.executeCommand(commandInvocation, terminal);

        expectedDatePeriod.forEach(localDate -> verify(returnOrderBillingService).billForPartners(
                eq(localDate),
                eq(Set.of(FIRST_SUPPLIER, SECOND_SUPPLIER)),
                eq(returnType),
                any()
        ));

        verifyNoMoreInteractions(returnOrderBillingService);
    }

    @ParameterizedTest(name = "{3}")
    @DisplayName("Успешные кейсы выполнения команды для всех партнёров")
    @MethodSource("getValidCases")
    void test_executeCommandForAllSuppliers(
            Instant timeOfTest,
            String fromDate,
            DatePeriod expectedDatePeriod,
            OrderServiceReturnType returnType,
            String description
    ) {
        init(timeOfTest, fromDate, returnType, true);

        returnOrderBillingCommand.executeCommand(commandInvocation, terminal);

        expectedDatePeriod.forEach(localDate -> verify(returnOrderBillingService).billForPartners(
                eq(localDate),
                eq(null),
                eq(returnType),
                any()
        ));

        verifyNoMoreInteractions(returnOrderBillingService);
    }
}
