package ru.yandex.market.billing.returns;

import java.io.PrintWriter;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
 * Тесты для комманды {@link ReturnItemBillingCommand}
 */
@ExtendWith(MockitoExtension.class)
public class ReturnItemBillingCommandTest {
    private static final String COMMAND_NAME = "recalculate-return-item-billing";
    private static final String FROM_DATE_OPTION = "from-date";
    private static final Instant DATE_2021_10_22 = LocalDate.of(2021, 10, 22)
            .atStartOfDay(ZoneId.systemDefault()).toInstant();
    private static final Instant DATE_2021_11_01 = LocalDate.of(2021, 11, 1)
            .atStartOfDay(ZoneId.systemDefault()).toInstant();

    @Mock
    private PartnerService partnerService;

    @Mock
    private ReturnItemBillingService returnItemBillingService;

    @Mock
    private EnvironmentAwareDateValidationService environmentAwareDateValidationService;

    @Mock
    private Terminal terminal;

    private ReturnItemBillingCommand returnItemBillingCommand;
    private CommandInvocation commandInvocation;

    private void init(Instant fixedInstant, String fromDate, String[] partners) {
        returnItemBillingCommand = new ReturnItemBillingCommand(
                returnItemBillingService,
                partnerService,
                environmentAwareDateValidationService,
                Clock.fixed(fixedInstant, ZoneId.systemDefault())
        );

        commandInvocation = new CommandInvocation(
                COMMAND_NAME,
                partners,
                Map.of(FROM_DATE_OPTION, fromDate)
        );

        Set<Long> partnersSet = Arrays.stream(partners)
                .map(Long::valueOf)
                .collect(Collectors.toSet());

        when(terminal.getWriter()).thenReturn(new PrintWriter(System.out));
        when(terminal.confirm(anyString())).thenReturn(true);
        when(partnerService.getPartners(partnersSet))
                .thenReturn(partnersSet.stream()
                        .map(partnerId -> new PartnerInfo(partnerId, CampaignType.SUPPLIER, null))
                        .collect(Collectors.toList()));
    }

    @ParameterizedTest(name = "{3}")
    @DisplayName("Успешные кейсы выполнения команды")
    @MethodSource("getValidCases")
    void test_executeCommand(
            Instant timeOfTest,
            String fromDate,
            DatePeriod expectedDatePeriod,
            String[] partners
    ) {
        init(timeOfTest, fromDate, partners);

        returnItemBillingCommand.executeCommand(commandInvocation, terminal);

        expectedDatePeriod.forEach(localDate -> verify(returnItemBillingService).process(
                eq(localDate),
                eq(Arrays.stream(partners)
                        .map(Long::valueOf)
                        .collect(Collectors.toSet()))
        ));

        verifyNoMoreInteractions(returnItemBillingService);
    }

    private static Stream<Arguments> getValidCases() {
        return Stream.of(
                Arguments.of(
                        DATE_2021_10_22,
                        "2021-10-01",
                        DatePeriod.of(LocalDate.of(2021, 10, 1), 21),
                        new String[] {"111", "222"}
                ),
                //Переобиллить только вчерашний день
                Arguments.of(
                        DATE_2021_10_22,
                        "2021-10-21",
                        DatePeriod.of(LocalDate.of(2021, 10, 21), 1),
                        new String[] {"333", "444"}
                ),
                //Если сегодня первое число месяца, то еще можно переобилить хранение за прошлый месяц.
                Arguments.of(
                        DATE_2021_11_01,
                        "2021-10-01",
                        DatePeriod.of(LocalDate.of(2021, 10, 1), 31),
                        new String[] {"555", "3123"}
                )
        );
    }
}