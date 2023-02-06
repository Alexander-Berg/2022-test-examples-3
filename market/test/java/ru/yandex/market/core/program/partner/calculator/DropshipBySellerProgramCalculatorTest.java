package ru.yandex.market.core.program.partner.calculator;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.abo.api.client.AboPublicRestClient;
import ru.yandex.market.abo.api.entity.order.limit.OrderLimitDTO;
import ru.yandex.market.abo.api.entity.order.limit.PublicOrderLimitReason;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.program.partner.model.NeedTestingState;
import ru.yandex.market.core.program.partner.model.ProgramArgs;
import ru.yandex.market.core.program.partner.model.ProgramStatus;
import ru.yandex.market.core.program.partner.model.ProgramSubStatus;
import ru.yandex.market.core.program.partner.model.ProgramType;
import ru.yandex.market.core.program.partner.model.Status;
import ru.yandex.market.core.program.partner.model.Substatus;
import ru.yandex.market.core.program.partner.status.PartnerStatusService;
import ru.yandex.market.mbi.partner.status.client.model.PartnerStatusInfo;
import ru.yandex.market.mbi.partner.status.client.model.StatusResolverResults;
import ru.yandex.market.mbi.partner.status.client.model.StatusResolverType;
import ru.yandex.market.mbi.partner.status.client.model.StatusResolversResponse;
import ru.yandex.market.mbi.partner.status.client.model.WizardStepStatus;
import ru.yandex.market.partner.notification.client.model.GetMessageHeadersResponse;
import ru.yandex.market.partner.notification.client.model.MessageHeaderDTO;
import ru.yandex.market.partner.notification.client.model.PagerDTO;
import ru.yandex.market.partner.notification.client.model.PriorityDTO;
import ru.yandex.market.partner.test.context.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class DropshipBySellerProgramCalculatorTest extends FunctionalTest {

    @Autowired
    DropshipBySellerProgramCalculator dropshipBySellerProgramCalculator;

    @Autowired
    AboPublicRestClient aboClient;

    @Autowired
    private PartnerStatusService partnerStatusService;

    @BeforeEach
    void setUp() {
        Mockito.when(partnerStatusService.getStatusResolvers(any()))
                .thenReturn(CompletableFuture.completedFuture(new StatusResolversResponse()));
    }

    private static Stream<Arguments> calculateTestData() {
        return Stream.of(
                Arguments.of("Магазин настраивается по визарду", 100L, Status.DISABLED, null, false, false, true),
                Arguments.of("Реплицированный из FBS магазин настраивается по визарду",
                        121L, Status.DISABLED, List.of(Substatus.PREMODERATION_NEEDED), false, false, false),
                Arguments.of("Магазин индексируется", 101L, Status.ENABLING, null, false, false,
                        false),
                Arguments.of("Магазин индексируется", 102L, Status.ENABLING, null, false, false,
                        false),
                Arguments.of("У магазина проблемы с качеством", 103L, Status.FAILED, List.of(Substatus.QUALITY_SERIOUS),
                        false, true, false),
                Arguments.of("Магазин выключен партнером", 104L, Status.SUSPENDED, List.of(Substatus.BY_PARTNER), false,
                        false, false),
                Arguments.of("Магазин включен", 105L, Status.FULL, null, true, false, false),
                Arguments.of("Магазин на модерации", 106L, Status.TESTING, List.of(Substatus.ON_MODERATION), false,
                        false, false),
                Arguments.of("Магазин выключен партнером, но с проблемами с качеством",
                        108L, Status.FAILED, List.of(Substatus.QUALITY_SERIOUS), false, true, false),
                Arguments.of("Магазин не может проиндексироваться из-за проблем с качеством",
                        109, Status.FAILED, List.of(Substatus.QUALITY_SERIOUS), false, true, false),
                Arguments.of("Наложен катоф на MARKETPLACE",
                        110, Status.FAILED, List.of(Substatus.QUALITY_FAILED), false, false, false),
                Arguments.of("Магазин отключен партнером, но с другими проблемами на фиче MARKETPLACE",
                        111, Status.FAILED, List.of(Substatus.QUALITY_FAILED), false, false, false),
                Arguments.of("Магазин отключен, не выбран метод работы ПИ/АПИ",
                        112, Status.FAILED, List.of(Substatus.WORK_MODE), false, false, false),
                Arguments.of("Магазин отключен, не настроены СиСы",
                        113, Status.FAILED, List.of(Substatus.DELIVERY_NOT_CONFIGURED), false, false, false),
                Arguments.of("Магазин отключен, не настроены СиСы и режим работы",
                        114, Status.FAILED, List.of(Substatus.DELIVERY_NOT_CONFIGURED, Substatus.WORK_MODE), false,
                        false, false),
                Arguments.of("Наложен катоф LIMIT_ORDERS",
                        115, Status.FAILED, List.of(Substatus.LIMIT_ORDERS), false, false, false),
                Arguments.of("Наложен катоф LOW_RATING",
                        116, Status.FAILED, List.of(Substatus.LOW_RATING), false, false, false),
                Arguments.of("Отключение за клоновость",
                        117, Status.FAILED, List.of(Substatus.CLONE), false, true, false),
                Arguments.of("Приостановка для самопроверки",
                        118, Status.SUSPENDED, List.of(Substatus.SELFCHECK_REQUIRED), false, false, false),
                Arguments.of("Приостановка для самопроверки, есть BY_PARTNER",
                        119, Status.SUSPENDED, List.of(Substatus.SELFCHECK_REQUIRED), false, false, false),
                Arguments.of("Приостановка для самопроверки, есть катофы за качество",
                        120, Status.SUSPENDED, List.of(Substatus.SELFCHECK_REQUIRED), false, true, false),
                Arguments.of("Магазину нужно нажать кнопку 'Разместиться'",
                        122, Status.SUSPENDED, List.of(Substatus.MARKETPLACE_PLACEMENT), false, false, false),
                Arguments.of("Магазин c катофом BY_PARTNER и MARKETPLACE_PLACEMENT",
                        123, Status.SUSPENDED, List.of(Substatus.MARKETPLACE_PLACEMENT), false, false, false),
                Arguments.of("Магазин c катофом MARKETPLACE_PLACEMENT и катофами за качество",
                        124, Status.FAILED, List.of(Substatus.QUALITY_SERIOUS), false, true, false)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("calculateTestData")
    @DbUnitDataSet(before = "DropshipBySellerProgramCalculatorTest.before.csv")
    public void calculateTest(String description,
                              long datasourceId,
                              Status expectedStatus,
                              List<Substatus> expectedSubstatus,
                              boolean enabled,
                              boolean requiresTesting,
                              boolean newbie) {
        ProgramStatus calculatedStatus = dropshipBySellerProgramCalculator.calculate(
                0L, datasourceId, ProgramArgs.builder().build()
        );
        ProgramStatus expected = getExpected(expectedSubstatus, expectedStatus, enabled, requiresTesting, newbie);
        assertThat(calculatedStatus).isEqualTo(expected);
    }

    @Test
    @DbUnitDataSet(before = "DropshipBySellerProgramCalculatorTest.before.csv")
    public void checkCloneMessageIdTest() {
        long datasourceId = 117;
        Status expectedStatus = Status.FAILED;
        List<Substatus> expectedSubstatus = List.of(Substatus.CLONE);
        boolean enabled = false;
        boolean requiresTesting = true;
        boolean newbie = false;

        doReturn(new GetMessageHeadersResponse().headers(
                                List.of(new MessageHeaderDTO()
                                        .messageId(1L)
                                        .sentTime(OffsetDateTime.now())
                                        .priority(PriorityDTO.NORMAL))
                        )
                        .pager(new PagerDTO().currentPage(0).pageSize(2).itemCount(2))
        )
                .when(partnerNotificationClient)
                .getMessageHeaders(any(), any(), any(), any(), any(), any(), any(),
                        ArgumentMatchers.argThat(groupIds -> groupIds.contains(67794030L)), any(), any());

        ProgramStatus calculatedStatus = dropshipBySellerProgramCalculator.calculate(
                0L, datasourceId, ProgramArgs.builder().withAddMessageId(true).build()
        );
        ProgramStatus expected = getExpected(expectedSubstatus, expectedStatus, enabled, requiresTesting, newbie, 1L);
        assertThat(calculatedStatus).isEqualTo(expected);
    }

    @Test
    @DisplayName("Магазин включен с ограничениями от ABO")
    @DbUnitDataSet(before = "DropshipBySellerProgramCalculatorTest.before.csv")
    public void calculateOrdersRestrictedTest() {
        long datasourceId = 107L;

        OrderLimitDTO orderLimitDTO = new OrderLimitDTO(
                datasourceId, 10, PublicOrderLimitReason.NEWBIE, null, null);

        when(aboClient.getOrderLimit(anyLong()))
                .thenReturn(orderLimitDTO);

        ProgramStatus calculatedStatus = dropshipBySellerProgramCalculator.calculate(
                0L, datasourceId, ProgramArgs.builder().build()
        );
        ProgramStatus expected = getExpected(
                List.of(Substatus.LIMIT_ORDERS), Status.RESTRICTED, true, false, false);
        assertThat(calculatedStatus).isEqualTo(expected);
    }

    @Test
    @DisplayName("Идем за данными в partner-status, получаем результат")
    @DbUnitDataSet(before = {"DropshipBySellerProgramCalculatorTest.before.csv"})
    void resultFromPartnerStatus() {
        Mockito.when(partnerStatusService.getStatusResolvers(any()))
                .thenReturn(CompletableFuture.completedFuture(new StatusResolversResponse()
                        .addResolversItem(new StatusResolverResults()
                                .resolver(StatusResolverType.REPLICATION_STATUS)
                                .addResultsItem(new PartnerStatusInfo()
                                        .enabled(false)
                                        .partnerId(105L)
                                        .status(WizardStepStatus.ENABLING)))));

        ProgramStatus calculatedStatus = dropshipBySellerProgramCalculator.calculate(
                0L, 105L, ProgramArgs.builder().build());
        ProgramStatus expected = getExpected(
                Collections.emptyList(), Status.ENABLING, false, false, false);
        assertThat(calculatedStatus).isEqualTo(expected);
        Mockito.verify(partnerStatusService, times(1)).getStatusResolvers(any());
    }

    private static ProgramStatus getExpected(
            @Nullable List<Substatus> substatuses,
            Status status,
            boolean enabled,
            boolean requiresTesting,
            boolean newbie
    ) {
        return getExpected(substatuses, status, enabled, requiresTesting, newbie, null);
    }

    private static ProgramStatus getExpected(
            @Nullable List<Substatus> substatuses,
            Status status,
            boolean enabled,
            boolean requiresTesting,
            boolean newbie,
            Long messageId
    ) {
        ProgramStatus.Builder builder = ProgramStatus.builder()
                .status(status)
                .program(ProgramType.DROPSHIP_BY_SELLER)
                .needTestingState(requiresTesting
                        ? NeedTestingState.REQUIRED
                        : NeedTestingState.NOT_REQUIRED)
                .enabled(enabled)
                .newbie(newbie)
                .messageId(messageId);
        if (substatuses != null) {
            substatuses.forEach(subStatus -> builder.addSubStatus(ProgramSubStatus.builder().code(subStatus).build()));
        }
        return builder.build();
    }
}
