package ru.yandex.market.billing.fulfillment.command;

import java.io.PrintWriter;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.List;
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
import ru.yandex.market.billing.fulfillment.billing.disposal.DisposalBillingCommand;
import ru.yandex.market.billing.fulfillment.billing.disposal.DisposalBillingService;
import ru.yandex.market.billing.util.EnvironmentAwareDateValidationService;
import ru.yandex.market.core.calendar.DatePeriod;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.partner.PartnerService;
import ru.yandex.market.core.partner.model.PartnerInfo;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Тест команды для запуска обилливания услуг добровольной утилизации для списка поставщиков.
 */
@ExtendWith(MockitoExtension.class)
public class DisposalBillingCommandTest {
    private static final String COMMAND_NAME = "recalculate-disposal-billing";
    private static final String FROM_DATE_OPTION = "from-date";
    private static final long FIRST_SUPPLIER_ID = 123;
    private static final long SECOND_SUPPLIER_ID = 45;
    private static final String[] SUPPLIER_IDS =
            {String.valueOf(FIRST_SUPPLIER_ID), String.valueOf(SECOND_SUPPLIER_ID)};
    private static final Instant DATE_2019_04_01 = LocalDate.of(2019, Month.APRIL, 1)
            .atStartOfDay(ZoneId.systemDefault()).toInstant();
    private static final Instant DATE_2019_04_30 = LocalDate.of(2019, Month.APRIL, 30)
            .atStartOfDay(ZoneId.systemDefault()).toInstant();

    @Mock
    private PartnerService partnerService;

    @Mock
    private DisposalBillingService disposalBillingService;

    @Mock
    private EnvironmentAwareDateValidationService environmentAwareDateValidationService;

    @Mock
    private Terminal terminal;

    private DisposalBillingCommand testing;
    private CommandInvocation commandInvocation;

    private static Stream<Arguments> getValidCases() {
        return Stream.of(
                Arguments.of(
                        DATE_2019_04_30,
                        "2019-04-01",
                        DatePeriod.of(LocalDate.of(2019, 4, 1), 29),
                        "Переобилливание с начала текущего месяца."
                ),
                Arguments.of(
                        DATE_2019_04_30,
                        "2019-04-29",
                        DatePeriod.of(LocalDate.of(2019, 4, 29), 1),
                        "Переобилливание только вчерашнего дня."
                ),
                Arguments.of(
                        DATE_2019_04_01,
                        "2019-03-01",
                        DatePeriod.of(LocalDate.of(2019, 3, 1), 31),
                        "Переобилливание прошлого месяца, если сегодня первое число месяца."
                )
        );
    }

    private void init(Instant fixedInstant, String fromDate) {
        testing = new DisposalBillingCommand(
                disposalBillingService,
                partnerService,
                environmentAwareDateValidationService,
                Clock.fixed(fixedInstant, ZoneId.systemDefault())
        );

        commandInvocation = new CommandInvocation(
                COMMAND_NAME,
                SUPPLIER_IDS,
                Map.of(FROM_DATE_OPTION, fromDate)
        );

        when(terminal.getWriter()).thenReturn(new PrintWriter(System.out));
        when(terminal.confirm(anyString())).thenReturn(true);
        when(partnerService.getPartners(Set.of(FIRST_SUPPLIER_ID, SECOND_SUPPLIER_ID)))
                .thenReturn(
                        List.of(
                                new PartnerInfo(FIRST_SUPPLIER_ID, CampaignType.SUPPLIER, null),
                                new PartnerInfo(SECOND_SUPPLIER_ID, CampaignType.SUPPLIER, null)
                        )
                );
    }

    @ParameterizedTest(name = "{3}")
    @DisplayName("Успешные кейсы выполнения команды")
    @MethodSource("getValidCases")
    void executeCommand(
            Instant timeOfTest,
            String fromDate,
            DatePeriod expectedDatePeriod,
            String description
    ) {
        init(timeOfTest, fromDate);

        testing.executeCommand(commandInvocation, terminal);

        expectedDatePeriod.forEach(localDate -> verify(disposalBillingService).billForSuppliers(
                eq(localDate),
                eq(Set.of(FIRST_SUPPLIER_ID, SECOND_SUPPLIER_ID))
        ));

        verifyNoMoreInteractions(disposalBillingService);
    }
}
