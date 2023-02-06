package ru.yandex.market.tpl.billing.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.JUnitJupiterSoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.pvz.client.billing.PvzClient;
import ru.yandex.market.pvz.client.billing.dto.BillingLegalPartnerTerminationDto;
import ru.yandex.market.pvz.client.model.partner.LegalPartnerTerminationType;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;
import ru.yandex.market.tpl.billing.model.PartnerType;
import ru.yandex.market.tpl.billing.model.PaymentMethod;
import ru.yandex.market.tpl.billing.model.PaymentStatus;
import ru.yandex.market.tpl.billing.model.entity.IncomingPayment;
import ru.yandex.market.tpl.billing.model.entity.Partner;
import ru.yandex.market.tpl.billing.model.entity.PartnerDebt;
import ru.yandex.market.tpl.billing.model.entity.PvzOrder;
import ru.yandex.market.tpl.billing.repository.EnvironmentRepository;
import ru.yandex.market.tpl.billing.repository.IncomingPaymentRepository;
import ru.yandex.market.tpl.billing.repository.PartnerDebtRepository;
import ru.yandex.market.tpl.billing.repository.PartnerRepository;
import ru.yandex.market.tpl.billing.repository.PvzOrderRepository;
import ru.yandex.market.tpl.common.startrek.StartrekService;
import ru.yandex.market.tpl.common.startrek.ticket.StartrekTicket;
import ru.yandex.market.tpl.common.startrek.ticket.TicketQueueType;
import ru.yandex.startrek.client.model.Issue;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class CalcPvzPaymentDebtsServiceTest extends AbstractFunctionalTest {

    private static final int DAYS_FOR_PAYMENT = 5;

    private static final int MAX_DAYS_SINCE_LAST_ORDER = 30;

    private static final LocalDate TODAY = LocalDate.parse("2021-02-09");

    private static final LocalDate FIVE_DAYS_AGO = TODAY.minusDays(DAYS_FOR_PAYMENT);

    private static final ZoneOffset ZONE_OFFSET = OffsetDateTime.now().getOffset();

    private static final String STARTREK_URL = "https://st.yandex-team.ru/";

    private static final String NO_DEBTS_MESSAGE = "No pvz partners with payment debts found";

    private static final String TICKET_TITLE = "Просрочки партнёров ПВЗ по задолженностям " + TODAY;

    private static final String TICKET_DESCRIPTION = "Имеются партнёры ПВЗ с просрочкой задолженностей по заказам " +
            "перед Яндекс.Маркетом, они представлены в таблице ниже.\n";

    private static final String TABLE_HEADER = "\n%%(csv delimiter=; head=1)\n" +
            "Наименование партнёра;id партнёра;Номер виртуального счёта партнёра в OEBS;" +
            "Сумма просроченной задолженности;Маркетный id первого неоплаченного заказа;Дней просрочки\n";

    private static final String TABLE_FOOTER = "\n%%\n";

    private static final String DISABLED_PARTNERS_TABLE_NAME = "Отключенные партнеры";

    private static final String PARTNER_ENABLEMENT_HEADER = "\n%%(csv delimiter=; head=1)\n" +
            "id партнёра;Наименование партнера\n";

    private static final String ENABLED_PARTNERS_TABLE_NAME = "Подключенные партнеры";

    private static final String PVZ_DAYS_FOR_PAYMENT_PROPERTY_KEY = "pvzDaysForPayment";

    private static final String PVZ_MAX_DAYS_SINCE_LAST_ORDER_PROPERTY_KEY = "pvzMaxDaysSinceLastOrder";

    private static final String PARTNERS_BATCH_SIZE_PROPERTY_KEY = "pvzDebtCalculationPartnerBatchSize";

    private static final String ORDERS_BATCH_SIZE_PROPERTY_KEY = "pvzDebtCalculationOrderBatchSize";

    private static final String PVZ_MAX_DAYS_OF_DEBT = "pvzMaxDaysOfDebt";

    private static final String PVZ_MAX_SUM_OF_DEBT = "pvzMaxSumOfDebt";

    private static final String PARTNER_ENABLEMENT_KEY = "pvzDebtCalculationPartnerEnablement";

    private static final String PARTNER_ENABLEMENT_BATCH_SIZE_KEY = "pvzDebtCalculation.partnerEnablement.BatchSize";

    @Mock
    private PartnerRepository partnerRepository;

    @Mock
    private PvzOrderRepository pvzOrderRepository;

    @Mock
    private IncomingPaymentRepository incomingPaymentRepository;

    @Mock
    private PartnerDebtRepository partnerDebtRepositoryMocked;

    @Autowired
    private PartnerDebtRepository partnerDebtRepository;

    @Autowired
    private EnvironmentRepository environmentRepository;

    @Mock
    private EnvironmentRepository environmentRepositoryMock;

    @Mock
    private StartrekService startrekService;

    @Mock
    private PvzClient pvzClient;

    private CalcPvzPaymentDebtsService calcPvzPaymentDebtsService;

    private Clock clock;

    @RegisterExtension
    final JUnitJupiterSoftAssertions assertions = new JUnitJupiterSoftAssertions();

    @BeforeEach
    void setUp() {
        clock = TestableClock.fixed(
                TODAY.atStartOfDay().toInstant(OffsetDateTime.now().getOffset()),
                ZoneId.systemDefault()
        );

        calcPvzPaymentDebtsService = new CalcPvzPaymentDebtsService(
                partnerRepository,
                pvzOrderRepository,
                incomingPaymentRepository,
                partnerDebtRepositoryMocked,
                environmentRepositoryMock,
                startrekService,
                pvzClient,
                clock
        );

        when(partnerRepository.findAllByPartnerType(eq(PartnerType.PVZ))).thenReturn(getPartners());

        when(pvzOrderRepository.findOrdersForPartners(
                eq(Set.of(1L, 2L, 3L, 4L)),
                eq(FIVE_DAYS_AGO.atStartOfDay().atOffset(ZONE_OFFSET)),
                eq(Set.of(PaymentMethod.CASH.name(), PaymentMethod.CARD.name()))
                )
        ).thenReturn(Stream.of(
                getFirstPartnerPvzOrders(),
                getSecondPartnerPvzOrders(),
                getThirdPartnerPvzOrders(),
                getFourthPartnerPvzOrders()
                )
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList())
        );

        when(incomingPaymentRepository.findAllByPartnerIdOrderByStatementDateAsc(eq(1L)))
                .thenReturn(getFirstPartnerIncomingPayments());

        when(incomingPaymentRepository.findAllByPartnerIdOrderByStatementDateAsc(eq(2L)))
                .thenReturn(getSecondPartnerIncomingPayments());

        when(incomingPaymentRepository.findAllByPartnerIdOrderByStatementDateAsc(eq(4L)))
                .thenReturn(getFourthPartnerIncomingPayments());

        when(environmentRepositoryMock.getIntValueOrThrow(PARTNERS_BATCH_SIZE_PROPERTY_KEY)).thenReturn(500);

        when(environmentRepositoryMock.getIntValueOrThrow(ORDERS_BATCH_SIZE_PROPERTY_KEY)).thenReturn(250);

        var disabledPartner = BillingLegalPartnerTerminationDto.builder()
                .legalPartnerId(3)
                .fromTime(LocalDateTime.parse("2021-02-09T00:00").atOffset(ZONE_OFFSET))
                .type(LegalPartnerTerminationType.DEBT)
                .build();

        var partnersToEnable = List.of(
                BillingLegalPartnerTerminationDto.builder()
                        .legalPartnerId(1)
                        .active(false)
                        .type(LegalPartnerTerminationType.DEBT)
                        .build(),
                BillingLegalPartnerTerminationDto.builder()
                        .legalPartnerId(2)
                        .active(false)
                        .type(LegalPartnerTerminationType.DEBT)
                        .build()
        );

        var enabledPartners = List.of(
                BillingLegalPartnerTerminationDto.builder()
                        .legalPartnerId(1)
                        .active(false)
                        .type(LegalPartnerTerminationType.DEBT)
                        .build()
        );

        when(environmentRepositoryMock.getIntValueOrThrow(PVZ_MAX_DAYS_OF_DEBT)).thenReturn(6);
        when(environmentRepositoryMock.getIntValueOrThrow(PVZ_MAX_SUM_OF_DEBT)).thenReturn(100000);
        when(environmentRepositoryMock.getBooleanValueOrDefault(
                PARTNER_ENABLEMENT_KEY, Boolean.TRUE
        )).thenReturn(Boolean.TRUE);

        when(environmentRepositoryMock.getIntValueOrThrow(PARTNER_ENABLEMENT_BATCH_SIZE_KEY)).thenReturn(500);

        when(pvzClient.createLegalPartnerTermination(List.of(disabledPartner))).thenReturn(List.of(disabledPartner));

        when(pvzClient.enableLegalPartners(partnersToEnable)).thenReturn(enabledPartners);

        when(partnerDebtRepositoryMocked.findDisabledPartners(
                LocalDate.now(clock).minusDays(1L)
        )).thenReturn(List.of(1L));
    }

    @Test
    void calculatePvzPaymentDebts() {
        when(environmentRepositoryMock.getIntValueOrThrow(PVZ_DAYS_FOR_PAYMENT_PROPERTY_KEY))
                .thenReturn(DAYS_FOR_PAYMENT);

        when(environmentRepositoryMock.getIntValueOrThrow(PVZ_MAX_DAYS_SINCE_LAST_ORDER_PROPERTY_KEY))
                .thenReturn(MAX_DAYS_SINCE_LAST_ORDER);

        when(incomingPaymentRepository.findAllByPartnerIdIn(List.of(1L, 2L, 3L, 4L)))
                .thenReturn(Stream.of(
                        getFirstPartnerIncomingPayments(),
                        getSecondPartnerIncomingPayments(),
                        getThirdPartnerIncomingPayments(),
                        getFourthPartnerIncomingPayments()
                        )
                                .flatMap(Collection::stream)
                                .collect(Collectors.toList())
                );

        StartrekTicket startrekTicket = getStartrekTicket();

        when(startrekService.createTicket(
                eq(TicketQueueType.PVZDELAYED),
                eq(TICKET_TITLE),
                eq(getTicketBody()),
                anyString()
        )).thenReturn(startrekTicket);

        String response = calcPvzPaymentDebtsService.calculatePvzPaymentDebts();

        assertions.assertThat(response)
                .as("Asserting that response is valid")
                .isEqualTo(STARTREK_URL + startrekTicket.getKey());

        verifyCommon();

        verify(partnerDebtRepositoryMocked).saveAll(eq(getPartnerDebts()));

        verify(startrekService).createTicket(
                eq(TicketQueueType.PVZDELAYED),
                eq(TICKET_TITLE),
                eq(getTicketBody()),
                anyString()
        );

        verifyNoMoreInteractions(
                pvzOrderRepository,
                incomingPaymentRepository,
                partnerRepository,
                environmentRepositoryMock,
                startrekService
        );
    }

    @Test
    void calculatePvzPaymentDebtsNoPartnersWithDebts() {
        when(environmentRepositoryMock.getIntValueOrThrow(PVZ_DAYS_FOR_PAYMENT_PROPERTY_KEY))
                .thenReturn(DAYS_FOR_PAYMENT);

        when(environmentRepositoryMock.getIntValueOrThrow(PVZ_MAX_DAYS_SINCE_LAST_ORDER_PROPERTY_KEY))
                .thenReturn(MAX_DAYS_SINCE_LAST_ORDER);

        when(environmentRepositoryMock.getIntValueOrThrow(PARTNERS_BATCH_SIZE_PROPERTY_KEY)).thenReturn(500);

        when(environmentRepositoryMock.getIntValueOrThrow(ORDERS_BATCH_SIZE_PROPERTY_KEY)).thenReturn(250);

        when(incomingPaymentRepository.findAllByPartnerIdIn(List.of(1L, 2L, 3L, 4L)))
                .thenReturn(Stream.of(
                        getFirstPartnerIncomingPayments(),
                        getSecondPartnerIncomingPayments(),
                        getThirdPartnerIncomingPaymentsWithoutDebt(),
                        getFourthPartnerIncomingPayments()
                        )
                                .flatMap(Collection::stream)
                                .collect(Collectors.toList())
                );

        String response = calcPvzPaymentDebtsService.calculatePvzPaymentDebts();

        assertions.assertThat(response)
                .as("Asserting that response is valid")
                .isEqualTo(NO_DEBTS_MESSAGE);

        verifyCommon();

        verifyNoMoreInteractions(
                pvzOrderRepository,
                incomingPaymentRepository,
                partnerRepository,
                environmentRepositoryMock
        );

        verifyNoInteractions(startrekService);
    }

    @Test
    void calculatePvzPaymentDebtsInvalidDaysForPaymentProperty() {
        when(environmentRepositoryMock.getIntValueOrThrow(eq(PVZ_DAYS_FOR_PAYMENT_PROPERTY_KEY)))
                .thenThrow(new IllegalArgumentException(
                        PVZ_DAYS_FOR_PAYMENT_PROPERTY_KEY + " property value was null or unparseable for int"
                ));

        assertions.assertThatThrownBy(() -> calcPvzPaymentDebtsService.calculatePvzPaymentDebts())
                .as("Asserting that the exception was thrown to caller")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(PVZ_DAYS_FOR_PAYMENT_PROPERTY_KEY + " property value was null or unparseable for int");

        verify(partnerRepository).findAllByPartnerType(eq(PartnerType.PVZ));

        verify(environmentRepositoryMock).getIntValueOrThrow(eq(PVZ_DAYS_FOR_PAYMENT_PROPERTY_KEY));

        verifyNoMoreInteractions(partnerRepository, environmentRepositoryMock);

        verifyNoInteractions(
                pvzOrderRepository,
                incomingPaymentRepository,
                startrekService
        );
    }

    @Test
    void calculatePvzPaymentDebtsInvalidMaxDaysSinceLastOrderProperty() {
        when(environmentRepositoryMock.getIntValueOrThrow(eq(PVZ_DAYS_FOR_PAYMENT_PROPERTY_KEY)))
                .thenReturn(DAYS_FOR_PAYMENT);

        when(environmentRepositoryMock.getIntValueOrThrow(eq(PVZ_MAX_DAYS_SINCE_LAST_ORDER_PROPERTY_KEY)))
                .thenThrow(new IllegalArgumentException(
                        PVZ_MAX_DAYS_SINCE_LAST_ORDER_PROPERTY_KEY + " property value was null or unparseable"
                ));

        assertions.assertThatThrownBy(() -> calcPvzPaymentDebtsService.calculatePvzPaymentDebts())
                .as("Asserting that the exception was thrown to caller")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(PVZ_MAX_DAYS_SINCE_LAST_ORDER_PROPERTY_KEY + " property value was null or unparseable");

        verify(partnerRepository).findAllByPartnerType(eq(PartnerType.PVZ));

        verify(environmentRepositoryMock).getIntValueOrThrow(eq(PVZ_DAYS_FOR_PAYMENT_PROPERTY_KEY));

        verify(environmentRepositoryMock).getIntValueOrThrow(eq(PVZ_MAX_DAYS_SINCE_LAST_ORDER_PROPERTY_KEY));

        verifyNoMoreInteractions(partnerRepository, environmentRepositoryMock);

        verifyNoInteractions(
                pvzOrderRepository,
                incomingPaymentRepository,
                startrekService
        );
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/calcpvzdebt/before/partner_debt.csv",
            after = "/database/service/calcpvzdebt/after/partner_debt.csv")
    void calculateWithPartlyPaidThenPaidDebt() {

        var calcPvzPaymentDebtsServiceWithoutDebtMock = new CalcPvzPaymentDebtsService(
                partnerRepository,
                pvzOrderRepository,
                incomingPaymentRepository,
                partnerDebtRepository,
                environmentRepository,
                startrekService,
                pvzClient,
                clock
        );

        when(incomingPaymentRepository.findAllByPartnerIdIn(List.of(1L, 2L, 3L, 4L)))
                .thenReturn(Stream.of(
                        getFirstPartnerIncomingPayments(),
                        getSecondPartnerIncomingPayments(),
                        getThirdPartnerIncomingPayments(),
                        getFourthPartnerIncomingPayments()
                        )
                                .flatMap(Collection::stream)
                                .collect(Collectors.toList())
                );

        StartrekTicket startrekTicket = getStartrekTicket();

        when(startrekService.createTicket(
                eq(TicketQueueType.PVZDELAYED),
                eq(TICKET_TITLE),
                eq(getTicketBody()),
                anyString()
        )).thenReturn(startrekTicket);

        String response = calcPvzPaymentDebtsServiceWithoutDebtMock.calculatePvzPaymentDebts();

        assertions.assertThat(response)
                .as("Asserting that response is valid")
                .isEqualTo(STARTREK_URL + startrekTicket.getKey());
    }

    private void verifyCommon() {
        verify(partnerRepository).findAllByPartnerType(eq(PartnerType.PVZ));

        verify(environmentRepositoryMock).getIntValueOrThrow(eq(PVZ_DAYS_FOR_PAYMENT_PROPERTY_KEY));

        verify(environmentRepositoryMock).getIntValueOrThrow(eq(PVZ_MAX_DAYS_SINCE_LAST_ORDER_PROPERTY_KEY));

        verify(environmentRepositoryMock).getIntValueOrThrow(eq(PARTNERS_BATCH_SIZE_PROPERTY_KEY));

        verify(environmentRepositoryMock).getIntValueOrThrow(eq(ORDERS_BATCH_SIZE_PROPERTY_KEY));

        verify(environmentRepositoryMock).getIntValueOrThrow(eq(PARTNER_ENABLEMENT_BATCH_SIZE_KEY));

        verify(environmentRepositoryMock).getBooleanValueOrDefault(eq(PARTNER_ENABLEMENT_KEY), eq(Boolean.TRUE));

        verify(pvzOrderRepository)
                .findOrdersForPartners(
                        eq(Set.of(1L, 2L, 3L, 4L)),
                        eq(FIVE_DAYS_AGO.atStartOfDay().atOffset(ZONE_OFFSET)),
                        eq(Set.of(PaymentMethod.CASH.name(), PaymentMethod.CARD.name()))
                );

        verify(incomingPaymentRepository).findAllByPartnerIdIn(eq(List.of(1L, 2L, 3L, 4L)));

        verify(environmentRepositoryMock).getIntValueOrThrow(PVZ_MAX_SUM_OF_DEBT);
        verify(environmentRepositoryMock).getIntValueOrThrow(PVZ_MAX_DAYS_OF_DEBT);
    }

    private List<Partner> getPartners() {
        return List.of(
                new Partner()
                        .setId(1L)
                        .setVirtualAccountNumber("VAN1")
                        .setName("Partner 1")
                        .setExternalId(1L)
                        .setPartnerType(PartnerType.PVZ),
                new Partner()
                        .setId(2L)
                        .setVirtualAccountNumber("VAN2")
                        .setName("Partner 2")
                        .setExternalId(2L)
                        .setPartnerType(PartnerType.PVZ),
                new Partner()
                        .setId(3L)
                        .setVirtualAccountNumber("VAN3")
                        .setName("Partner 3")
                        .setExternalId(3L)
                        .setPartnerType(PartnerType.PVZ),
                new Partner()
                        .setId(4L)
                        .setVirtualAccountNumber("VAN4")
                        .setName("Partner 4")
                        .setExternalId(4L)
                        .setPartnerType(PartnerType.PVZ)
        );
    }

    private List<PvzOrder> getFirstPartnerPvzOrders() {
        return List.of(
                new PvzOrder()
                        .setPvzOrderId(1L)
                        .setMarketOrderId("ABC123")
                        .setDeliveryServiceId(1L)
                        .setPickupPointId(1L)
                        .setCreatedAt(LocalDateTime.parse("2021-02-01T06:00:00").atOffset(ZONE_OFFSET))
                        .setDeliveredAt(LocalDateTime.parse("2021-02-02T12:00:00").atOffset(ZONE_OFFSET))
                        .setPaymentSum(BigDecimal.valueOf(1000.0))
                        .setPaymentMethod(PaymentMethod.CARD)
                        .setPaymentStatus(PaymentStatus.PAID),
                new PvzOrder()
                        .setPvzOrderId(2L)
                        .setMarketOrderId("ABC124")
                        .setDeliveryServiceId(1L)
                        .setPickupPointId(1L)
                        .setCreatedAt(LocalDateTime.parse("2021-02-01T06:00:00").atOffset(ZONE_OFFSET))
                        .setDeliveredAt(LocalDateTime.parse("2021-02-02T12:00:00").atOffset(ZONE_OFFSET))
                        .setPaymentSum(BigDecimal.valueOf(500.0))
                        .setPaymentMethod(PaymentMethod.CARD)
                        .setPaymentStatus(PaymentStatus.PAID),
                new PvzOrder()
                        .setPvzOrderId(3L)
                        .setMarketOrderId("ABC125")
                        .setDeliveryServiceId(1L)
                        .setPickupPointId(1L)
                        .setCreatedAt(LocalDateTime.parse("2021-02-01T06:00:00").atOffset(ZONE_OFFSET))
                        .setDeliveredAt(LocalDateTime.parse("2021-02-02T18:00:00").atOffset(ZONE_OFFSET))
                        .setPaymentSum(BigDecimal.valueOf(750.0))
                        .setPaymentMethod(PaymentMethod.CASH)
                        .setPaymentStatus(PaymentStatus.PAID)
        );
    }

    private List<PvzOrder> getSecondPartnerPvzOrders() {
        return List.of(
                new PvzOrder()
                        .setPvzOrderId(4L)
                        .setMarketOrderId("DEF123")
                        .setDeliveryServiceId(2L)
                        .setPickupPointId(2L)
                        .setCreatedAt(LocalDateTime.parse("2021-02-01T06:00:00").atOffset(ZONE_OFFSET))
                        .setDeliveredAt(LocalDateTime.parse("2021-02-02T12:00:00").atOffset(ZONE_OFFSET))
                        .setPaymentSum(BigDecimal.valueOf(1000.0))
                        .setPaymentMethod(PaymentMethod.CARD)
                        .setPaymentStatus(PaymentStatus.PAID),
                new PvzOrder()
                        .setPvzOrderId(5L)
                        .setMarketOrderId("DEF124")
                        .setDeliveryServiceId(2L)
                        .setPickupPointId(2L)
                        .setCreatedAt(LocalDateTime.parse("2021-02-01T06:00:00").atOffset(ZONE_OFFSET))
                        .setDeliveredAt(LocalDateTime.parse("2021-02-02T12:00:00").atOffset(ZONE_OFFSET))
                        .setPaymentSum(BigDecimal.valueOf(500.0))
                        .setPaymentMethod(PaymentMethod.CARD)
                        .setPaymentStatus(PaymentStatus.PAID),
                new PvzOrder()
                        .setPvzOrderId(6L)
                        .setMarketOrderId("DEF125")
                        .setDeliveryServiceId(2L)
                        .setPickupPointId(2L)
                        .setCreatedAt(LocalDateTime.parse("2021-02-01T06:00:00").atOffset(ZONE_OFFSET))
                        .setDeliveredAt(LocalDateTime.parse("2021-02-02T18:00:00").atOffset(ZONE_OFFSET))
                        .setPaymentSum(BigDecimal.valueOf(750.0))
                        .setPaymentMethod(PaymentMethod.CASH)
                        .setPaymentStatus(PaymentStatus.PAID)
        );
    }

    private List<PvzOrder> getThirdPartnerPvzOrders() {
        return List.of(
                new PvzOrder()
                        .setPvzOrderId(7L)
                        .setMarketOrderId("GHI123")
                        .setDeliveryServiceId(3L)
                        .setPickupPointId(3L)
                        .setCreatedAt(LocalDateTime.parse("2021-02-01T06:00:00").atOffset(ZONE_OFFSET))
                        .setDeliveredAt(LocalDateTime.parse("2021-02-02T12:00:00").atOffset(ZONE_OFFSET))
                        .setPaymentSum(BigDecimal.valueOf(1000.0))
                        .setPaymentMethod(PaymentMethod.CARD)
                        .setPaymentStatus(PaymentStatus.PAID),
                new PvzOrder()
                        .setPvzOrderId(8L)
                        .setMarketOrderId("GHI124")
                        .setDeliveryServiceId(3L)
                        .setPickupPointId(3L)
                        .setCreatedAt(LocalDateTime.parse("2021-02-01T06:00:00").atOffset(ZONE_OFFSET))
                        .setDeliveredAt(LocalDateTime.parse("2021-02-02T12:00:00").atOffset(ZONE_OFFSET))
                        .setPaymentSum(BigDecimal.valueOf(500.0))
                        .setPaymentMethod(PaymentMethod.CARD)
                        .setPaymentStatus(PaymentStatus.PAID),
                new PvzOrder()
                        .setPvzOrderId(9L)
                        .setMarketOrderId("GHI125")
                        .setDeliveryServiceId(3L)
                        .setPickupPointId(3L)
                        .setCreatedAt(LocalDateTime.parse("2021-02-01T06:00:00").atOffset(ZONE_OFFSET))
                        .setDeliveredAt(LocalDateTime.parse("2021-02-02T18:00:00").atOffset(ZONE_OFFSET))
                        .setPaymentSum(BigDecimal.valueOf(100750.0))
                        .setPaymentMethod(PaymentMethod.CASH)
                        .setPaymentStatus(PaymentStatus.PAID)
        );
    }

    private List<PvzOrder> getFourthPartnerPvzOrders() {
        return List.of(
                new PvzOrder()
                        .setPvzOrderId(10L)
                        .setMarketOrderId("GHI123")
                        .setDeliveryServiceId(4L)
                        .setPickupPointId(4L)
                        .setCreatedAt(LocalDateTime.parse("2021-01-01T06:00:00").atOffset(ZONE_OFFSET))
                        .setDeliveredAt(LocalDateTime.parse("2021-01-02T12:00:00").atOffset(ZONE_OFFSET))
                        .setPaymentSum(BigDecimal.valueOf(1000.0))
                        .setPaymentMethod(PaymentMethod.CARD)
                        .setPaymentStatus(PaymentStatus.PAID),
                new PvzOrder()
                        .setPvzOrderId(11L)
                        .setMarketOrderId("GHI124")
                        .setDeliveryServiceId(4L)
                        .setPickupPointId(4L)
                        .setCreatedAt(LocalDateTime.parse("2021-01-01T06:00:00").atOffset(ZONE_OFFSET))
                        .setDeliveredAt(LocalDateTime.parse("2021-01-02T12:00:00").atOffset(ZONE_OFFSET))
                        .setPaymentSum(BigDecimal.valueOf(500.0))
                        .setPaymentMethod(PaymentMethod.CARD)
                        .setPaymentStatus(PaymentStatus.PAID),
                new PvzOrder()
                        .setPvzOrderId(12L)
                        .setMarketOrderId("GHI125")
                        .setDeliveryServiceId(4L)
                        .setPickupPointId(4L)
                        .setCreatedAt(LocalDateTime.parse("2021-01-01T06:00:00").atOffset(ZONE_OFFSET))
                        .setDeliveredAt(LocalDateTime.parse("2021-01-02T18:00:00").atOffset(ZONE_OFFSET))
                        .setPaymentSum(BigDecimal.valueOf(750.0))
                        .setPaymentMethod(PaymentMethod.CASH)
                        .setPaymentStatus(PaymentStatus.PAID)
        );
    }

    private List<IncomingPayment> getFirstPartnerIncomingPayments() {
        return List.of(
                new IncomingPayment()
                        .setKey(
                                new IncomingPayment.IncomingPaymentKey().setStatementNumber("ST-A-1")
                                        .setStatementLineNumber(1L)
                        )
                        .setStatementDate(LocalDate.parse("2021-02-02"))
                        .setStatementTrxText("For order")
                        .setCheckAppliedAmount(BigDecimal.valueOf(1000.0))
                        .setPartnerId(1L),
                new IncomingPayment()
                        .setKey(
                                new IncomingPayment.IncomingPaymentKey()
                                        .setStatementNumber("ST-A-1")
                                        .setStatementLineNumber(2L)
                        )
                        .setStatementDate(LocalDate.parse("2021-02-05"))
                        .setStatementTrxText("For order")
                        .setCheckAppliedAmount(BigDecimal.valueOf(500.0))
                        .setPartnerId(1L),
                new IncomingPayment()
                        .setKey(
                                new IncomingPayment.IncomingPaymentKey()
                                        .setStatementNumber("ST-A-2")
                                        .setStatementLineNumber(1L)
                        )
                        .setStatementDate(LocalDate.parse("2021-02-09"))
                        .setStatementTrxText("For order")
                        .setCheckAppliedAmount(BigDecimal.valueOf(1000.0))
                        .setPartnerId(1L)
        );
    }

    private List<IncomingPayment> getSecondPartnerIncomingPayments() {
        return List.of(
                new IncomingPayment()
                        .setKey(
                                new IncomingPayment.IncomingPaymentKey()
                                        .setStatementNumber("ST-A-1")
                                        .setStatementLineNumber(3L)
                        )
                        .setStatementDate(LocalDate.parse("2021-02-02"))
                        .setStatementTrxText("For order")
                        .setCheckAppliedAmount(BigDecimal.valueOf(1000.0))
                        .setPartnerId(2L),
                new IncomingPayment()
                        .setKey(
                                new IncomingPayment.IncomingPaymentKey()
                                        .setStatementNumber("ST-A-1")
                                        .setStatementLineNumber(4L)
                        )
                        .setStatementDate(LocalDate.parse("2021-02-05"))
                        .setStatementTrxText("For order")
                        .setCheckAppliedAmount(BigDecimal.valueOf(500.0))
                        .setPartnerId(2L),
                new IncomingPayment()
                        .setKey(
                                new IncomingPayment.IncomingPaymentKey()
                                        .setStatementNumber("ST-A-2")
                                        .setStatementLineNumber(2L)
                        )
                        .setStatementDate(LocalDate.parse("2021-02-09"))
                        .setStatementTrxText("For order")
                        .setCheckAppliedAmount(BigDecimal.valueOf(750.0))
                        .setPartnerId(2L)
        );
    }

    private List<IncomingPayment> getThirdPartnerIncomingPayments() {
        return List.of(
                new IncomingPayment()
                        .setKey(
                                new IncomingPayment.IncomingPaymentKey()
                                        .setStatementNumber("ST-A-1")
                                        .setStatementLineNumber(5L)
                        )
                        .setStatementDate(LocalDate.parse("2021-02-02"))
                        .setStatementTrxText("For order")
                        .setCheckAppliedAmount(BigDecimal.valueOf(1000.0))
                        .setPartnerId(3L),
                new IncomingPayment()
                        .setKey(
                                new IncomingPayment.IncomingPaymentKey()
                                        .setStatementNumber("ST-A-1")
                                        .setStatementLineNumber(6L)
                        )
                        .setStatementDate(LocalDate.parse("2021-02-05"))
                        .setStatementTrxText("For order")
                        .setCheckAppliedAmount(BigDecimal.valueOf(500.0))
                        .setPartnerId(3L),
                new IncomingPayment()
                        .setKey(
                                new IncomingPayment.IncomingPaymentKey()
                                        .setStatementNumber("ST-A-2")
                                        .setStatementLineNumber(3L)
                        )
                        .setStatementDate(LocalDate.parse("2021-02-09"))
                        .setStatementTrxText("For order")
                        .setCheckAppliedAmount(BigDecimal.valueOf(250.0))
                        .setPartnerId(3L)
        );
    }

    private List<IncomingPayment> getFourthPartnerIncomingPayments() {
        return List.of(
                new IncomingPayment()
                        .setKey(
                                new IncomingPayment.IncomingPaymentKey()
                                        .setStatementNumber("ST-A-1")
                                        .setStatementLineNumber(7L)
                        )
                        .setStatementDate(LocalDate.parse("2021-02-02"))
                        .setStatementTrxText("For order")
                        .setCheckAppliedAmount(BigDecimal.valueOf(1000.0))
                        .setPartnerId(4L),
                new IncomingPayment()
                        .setKey(
                                new IncomingPayment.IncomingPaymentKey()
                                        .setStatementNumber("ST-A-1")
                                        .setStatementLineNumber(8L)
                        )
                        .setStatementDate(LocalDate.parse("2021-02-05"))
                        .setStatementTrxText("For order")
                        .setCheckAppliedAmount(BigDecimal.valueOf(500.0))
                        .setPartnerId(4L),
                new IncomingPayment()
                        .setKey(
                                new IncomingPayment.IncomingPaymentKey()
                                        .setStatementNumber("ST-A-2")
                                        .setStatementLineNumber(4L)
                        )
                        .setStatementDate(LocalDate.parse("2021-02-09"))
                        .setStatementTrxText("For order")
                        .setCheckAppliedAmount(BigDecimal.valueOf(250.0))
                        .setPartnerId(4L)
        );
    }

    private List<IncomingPayment> getThirdPartnerIncomingPaymentsWithoutDebt() {
        return List.of(
                new IncomingPayment()
                        .setKey(
                                new IncomingPayment.IncomingPaymentKey()
                                        .setStatementNumber("ST-A-1")
                                        .setStatementLineNumber(5L)
                        )
                        .setStatementDate(LocalDate.parse("2021-02-02"))
                        .setStatementTrxText("For order")
                        .setCheckAppliedAmount(BigDecimal.valueOf(1000.0))
                        .setPartnerId(3L),
                new IncomingPayment()
                        .setKey(
                                new IncomingPayment.IncomingPaymentKey()
                                        .setStatementNumber("ST-A-1")
                                        .setStatementLineNumber(6L)
                        )
                        .setStatementDate(LocalDate.parse("2021-02-05"))
                        .setStatementTrxText("For order")
                        .setCheckAppliedAmount(BigDecimal.valueOf(100500.0))
                        .setPartnerId(3L),
                new IncomingPayment()
                        .setKey(
                                new IncomingPayment.IncomingPaymentKey()
                                        .setStatementNumber("ST-A-2")
                                        .setStatementLineNumber(3L)
                        )
                        .setStatementDate(LocalDate.parse("2021-02-09"))
                        .setStatementTrxText("For order")
                        .setCheckAppliedAmount(BigDecimal.valueOf(750.0))
                        .setPartnerId(3L)
        );
    }

    private List<PartnerDebt> getPartnerDebts() {
        return List.of(
                new PartnerDebt(
                        new PartnerDebt.PartnerDebtKey(3L, TODAY),
                        BigDecimal.valueOf(100500.0),
                        "GHI125",
                        7,
                        "Partner 3",
                        "VAN3",
                        7
                ),
                new PartnerDebt(
                        new PartnerDebt.PartnerDebtKey(4L, TODAY),
                        BigDecimal.valueOf(500.0),
                        "GHI125",
                        38,
                        "Partner 4",
                        "VAN4",
                        38
                )
        );
    }

    private String getTicketBody() {
        return TICKET_DESCRIPTION + TODAY + TABLE_HEADER + "Partner 3;3;VAN3;100500.0;GHI125;7" + TABLE_FOOTER +
                DISABLED_PARTNERS_TABLE_NAME + PARTNER_ENABLEMENT_HEADER + "3;Partner 3" + TABLE_FOOTER +
                ENABLED_PARTNERS_TABLE_NAME + PARTNER_ENABLEMENT_HEADER + "1;Partner 1" + TABLE_FOOTER;
    }

    private StartrekTicket getStartrekTicket() {
        return new StartrekTicket(
                new Issue(null, null, "TESTPVZDELAYED-1", null, 0, Cf.map(), null),
                null,
                null
        );
    }
}
