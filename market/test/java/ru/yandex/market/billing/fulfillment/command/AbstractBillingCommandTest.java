package ru.yandex.market.billing.fulfillment.command;

import java.io.PrintWriter;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.Collections;
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
import ru.yandex.market.billing.util.EnvironmentAwareDateValidationService;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.partner.PartnerService;
import ru.yandex.market.core.partner.model.PartnerInfo;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Тесты абстрактной команды запуска переобиливания.
 * {@link AbstractBillingCommand}
 */
@ExtendWith(MockitoExtension.class)
public class AbstractBillingCommandTest {
    private static final String COMMAND_NAME = "recalculate-billing";
    private static final String FROM_DATE_OPTION = "from-date";
    private static final long FIRST_SUPPLIER = 123;
    private static final long SECOND_SUPPLIER = 45;
    private static final String[] SUPPLIERS = {String.valueOf(FIRST_SUPPLIER), String.valueOf(SECOND_SUPPLIER)};
    private static final Instant DATE_2019_04_02 = LocalDate.of(2019, Month.APRIL, 2)
            .atStartOfDay(ZoneId.systemDefault()).toInstant();
    private static final Instant DATE_2019_04_30 = LocalDate.of(2019, Month.APRIL, 30)
            .atStartOfDay(ZoneId.systemDefault()).toInstant();

    @Mock
    private PartnerService partnerService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private Terminal terminal;

    private AbstractBillingCommand billingCommand;
    private CommandInvocation commandInvocation;

    private static Stream<Arguments> getFailCases() {
        return Stream.of(
                Arguments.of(
                        DATE_2019_04_30,
                        null,
                        "Указан null в дате начала переобилливания " + FROM_DATE_OPTION
                ),
                Arguments.of(
                        DATE_2019_04_30,
                        "2019-03-31",
                        "Дата указана за прошлый месяц, исторические данные нельзя менять " +
                                "(только за прошлый месяц, если сегодня 1 число)"
                ),
                Arguments.of(
                        DATE_2019_04_30,
                        "2019-04-30",
                        "Указан тот же день, что и сегодня, в " + FROM_DATE_OPTION + ". " +
                                "Биллинг можно запустить не позже вчерашнего дня"
                ),
                Arguments.of(
                        DATE_2019_04_30,
                        "2019-05-01",
                        "Дата указана в будущем месяце"
                ),
                Arguments.of(
                        DATE_2019_04_02,
                        "2019-03-01",
                        "Если текущая дата не 1 число, то переобиливать за прошлый месяц нельзя"
                )
        );
    }

    private static Stream<Arguments> getBadSupplierIdArgs() {
        return Stream.of(
                Arguments.of(List.of("q")),
                Arguments.of(List.of("q", "1")),
                Arguments.of(List.of("1", "q")),
                Arguments.of(List.of("1", "2", "3", "-", "4"))
        );
    }

    private void init(Instant fixedInstant, String fromDate) {
        EnvironmentAwareDateValidationService environmentAwareDateValidationService =
                new EnvironmentAwareDateValidationService(
                    Clock.fixed(fixedInstant, ZoneId.systemDefault()),
                    environmentService
                );

        billingCommand = new AbstractBillingCommand(
                partnerService,
                environmentAwareDateValidationService,
                Clock.fixed(fixedInstant, ZoneId.systemDefault()))
        {
            @Override
            protected void calculate(
                    Terminal terminal,
                    Set<Long> supplierIds,
                    LocalDate date,
                    Map<String, String> options
            ) {}

            @Override
            public String[] getNames() {
                return new String[]{COMMAND_NAME};
            }

            @Override
            public String getDescription() {
                return null;
            }
        };

        commandInvocation = new CommandInvocation(
                COMMAND_NAME,
                SUPPLIERS,
                Collections.singletonMap(FROM_DATE_OPTION, fromDate)
        );

        when(terminal.getWriter()).thenReturn(new PrintWriter(System.out));
        when(partnerService.getPartners(Set.of(FIRST_SUPPLIER, SECOND_SUPPLIER)))
                .thenReturn(List.of(
                        new PartnerInfo(FIRST_SUPPLIER, CampaignType.SUPPLIER, null),
                        new PartnerInfo(SECOND_SUPPLIER, CampaignType.SUPPLIER, null)));
    }


    @ParameterizedTest(name = "{2}")
    @DisplayName("Плохие кейсы, когда команда не должна запустится")
    @MethodSource("getFailCases")
    void test_checkFailCases(Instant timeOfTest, String fromDate, String description) {
        init(timeOfTest, fromDate);

        assertThrows(
                IllegalStateException.class,
                () -> billingCommand.executeCommand(commandInvocation, terminal)
        );
    }

    @ParameterizedTest(name = "[{index}]")
    @DisplayName("Проверка получения supplierId из входящих аргументов")
    @MethodSource("getBadSupplierIdArgs")
    void test_checkBadSupplierIdsArgs(List<String> supplierIds) {
        assertThrows(
                IllegalArgumentException.class,
                () -> AbstractBillingCommand.convertToSupplierIds(supplierIds.toArray(String[]::new))
        );
    }
}
