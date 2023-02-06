package ru.yandex.market.delivery.transport_manager.facade.transportation_task;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.delivery.transport_manager.config.tpl.TplProperties;
import ru.yandex.market.delivery.transport_manager.domain.entity.Movement;
import ru.yandex.market.delivery.transport_manager.domain.entity.MovementStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TmPropertyKey;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationScheme;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationTask;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.Register;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.RegisterUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.UnitCount;
import ru.yandex.market.delivery.transport_manager.domain.enums.CountType;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationSource;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;
import ru.yandex.market.delivery.transport_manager.facade.register.RegisterSplitterBySkuService;
import ru.yandex.market.delivery.transport_manager.facade.transportation.TransportationFacade;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationTaskMapper;
import ru.yandex.market.delivery.transport_manager.service.TmPropertyService;
import ru.yandex.market.delivery.transport_manager.service.external.lms.LogisticsPointReceiver;
import ru.yandex.market.delivery.transport_manager.service.external.lms.PartnerScheduleReceiver;
import ru.yandex.market.delivery.transport_manager.service.register.RegisterService;
import ru.yandex.market.delivery.transport_manager.service.transportation.InitialStatusSelector;
import ru.yandex.market.delivery.transport_manager.util.IntRange;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointGatesScheduleResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDateTimeResponse;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class TransportationTaskSplitterTest {
    private static final Long FROM_WAREHOUSE_ID = 100L;
    private static final Long FROM_PARTNER_ID = 10L;
    private static final Long TO_WAREHOUSE_ID = 200L;
    private static final Long TO_PARTNER_ID = 20L;
    private static final long ANOTHER_WAREHOUSE_ID = 101L;
    private static final LocalDate[] DATES = IntStream.range(0, 11)
        .mapToObj(i -> LocalDate.of(2020, 10, 20).plusDays(i))
        .collect(Collectors.toList())
        .toArray(LocalDate[]::new);

    public static final long REGISTER_ID = 10001L;
    private static final LocalTime NOW_TIME = LocalTime.of(10, 0);
    public static final int MAX_DAYS_OFFSET = 10;
    public static final long VIRTUAL_LINEHAUL = 50000L;

    private TransportationTaskSplitter transportationTaskSplitter;
    private LogisticsPointReceiver logisticsPointReceiver;
    private PartnerScheduleReceiver partnerScheduleReceiver;
    private TestableClock clock;
    private UnregularInterwarehouseTransportationDateCalculator dateCalculator;
    private RegisterSplitterBySkuService registerSplitterBySkuService;
    private TmPropertyService tmPropertyService;

    @BeforeEach
    void setUp() {
        logisticsPointReceiver = mock(LogisticsPointReceiver.class);
        partnerScheduleReceiver = mock(PartnerScheduleReceiver.class);
        registerSplitterBySkuService = mock(RegisterSplitterBySkuService.class);
        RegisterService registerService = mock(RegisterService.class);
        TransportationFacade transportationFacade = mock(TransportationFacade.class);
        TransportationTaskMapper transportationTaskMapper = mock(TransportationTaskMapper.class);
        tmPropertyService = mock(TmPropertyService.class);

        clock = new TestableClock();

        clock.setFixed(
            LocalDateTime
                .of(LocalDate.of(2020, 10, 20), NOW_TIME)
                .atZone(ZoneId.systemDefault())
                .toInstant(),
            ZoneId.systemDefault()
        );

        dateCalculator = new UnregularInterwarehouseTransportationDateCalculator(clock);
        dateCalculator.setMaxDaysOffset(MAX_DAYS_OFFSET);
        dateCalculator.setMovementIntervalLengthHours(1);
        dateCalculator.setOutboundIntervalDays(new IntRange(2, 3));

        transportationTaskSplitter = new TransportationTaskSplitter(
            logisticsPointReceiver,
            partnerScheduleReceiver,
            dateCalculator,
            registerSplitterBySkuService,
            registerService,
            transportationFacade,
            transportationTaskMapper,
            clock,
            new InitialStatusSelector(tmPropertyService),
            new TplProperties().setVirtualLinehaul(VIRTUAL_LINEHAUL)
        );
    }

    @Test
    public void testCreate() {
        when(partnerScheduleReceiver.receive(
            eq(FROM_PARTNER_ID),
            eq(DATES[0]),
            eq(DATES[MAX_DAYS_OFFSET])
        )).thenReturn(List.of(
            LogisticsPointGatesScheduleResponse.newBuilder()
                .logisticsPointId(ANOTHER_WAREHOUSE_ID)
                .schedule(List.of(
                    ScheduleDateTimeResponse.newBuilder().date(DATES[2]).build(),
                    ScheduleDateTimeResponse.newBuilder().date(DATES[3]).build()
                ))
                .build(),
            LogisticsPointGatesScheduleResponse.newBuilder()
                .logisticsPointId(FROM_WAREHOUSE_ID)
                .schedule(List.of(
                    ScheduleDateTimeResponse.newBuilder().date(DATES[4]).build(),
                    ScheduleDateTimeResponse.newBuilder().date(DATES[5]).build()
                ))
                .build()
        ));
        Set<LocalDate> workingDays = transportationTaskSplitter.getWorkingDays(LogisticsPointResponse.newBuilder()
            .id(FROM_WAREHOUSE_ID)
            .partnerId(FROM_PARTNER_ID)
            .build());

        Assertions.assertEquals(
            Set.of(DATES[4], DATES[5]),
            workingDays
        );

        verify(partnerScheduleReceiver).receive(
            eq(FROM_PARTNER_ID),
            eq(DATES[0]),
            eq(DATES[MAX_DAYS_OFFSET])
        );
        verifyNoMoreInteractions(partnerScheduleReceiver);
    }

    @Test
    void getLogisticsPoint() {
        LogisticsPointResponse resp =
            LogisticsPointResponse.newBuilder().id(FROM_WAREHOUSE_ID).partnerId(FROM_PARTNER_ID).build();
        Mockito.when(logisticsPointReceiver.getLogisticPoint(FROM_WAREHOUSE_ID)).thenReturn(resp);
        LogisticsPointResponse actual = transportationTaskSplitter.getLogisticsPoint(FROM_WAREHOUSE_ID);
        Assertions.assertEquals(resp, actual);
    }

    @Test
    void getNonExistingLogisticsPoint() {
        Mockito.when(logisticsPointReceiver.getLogisticPoint(FROM_WAREHOUSE_ID)).thenReturn(null);
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> transportationTaskSplitter.getLogisticsPoint(FROM_WAREHOUSE_ID)
        );
    }

    @Test
    void createOne() {
        when(tmPropertyService.getBoolean(TmPropertyKey.ENABLE_INTERWAREHOUSE_ASSEMBLAGE_ONLY)).thenReturn(false);
        mockLms();

        Mockito.when(registerSplitterBySkuService.split(eq(REGISTER_ID), anyLong(), anyLong())).thenReturn(
            List.of(new Register())
        );

        var result = transportationTaskSplitter.split(
            new TransportationTask()
                .setLogisticPointFromId(FROM_WAREHOUSE_ID)
                .setLogisticPointToId(TO_WAREHOUSE_ID)
                .setRegisterId(REGISTER_ID)
        );

        Assertions.assertEquals(
            List.of(
                new Transportation()
                    .setStatus(TransportationStatus.NEW)
                    .setTransportationSource(TransportationSource.MBOC_EXTERNAL)
                    .setTransportationType(TransportationType.INTERWAREHOUSE)
                    .setScheme(TransportationScheme.NEW)
                    .setOutboundUnit(
                        new TransportationUnit()
                            .setStatus(TransportationUnitStatus.NEW)
                            .setType(TransportationUnitType.OUTBOUND)
                            .setExternalId(null)
                            .setPartnerId(FROM_PARTNER_ID)
                            .setLogisticPointId(FROM_WAREHOUSE_ID)
                            .setPlannedIntervalStart(DATES[4].atTime(NOW_TIME))
                            .setPlannedIntervalEnd(DATES[5].atTime(NOW_TIME))
                    )
                    .setInboundUnit(
                        new TransportationUnit()
                            .setStatus(TransportationUnitStatus.NEW)
                            .setType(TransportationUnitType.INBOUND)
                            .setExternalId(null)
                            .setPartnerId(TO_PARTNER_ID)
                            .setLogisticPointId(TO_WAREHOUSE_ID)
                            .setPlannedIntervalStart(DATES[5].atTime(NOW_TIME).plusHours(1))
                            .setPlannedIntervalEnd(DATES[6].atTime(NOW_TIME).plusHours(1))
                    )
                    .setMovement(
                        new Movement()
                            .setStatus(MovementStatus.NEVER_SEND)
                            .setPartnerId(VIRTUAL_LINEHAUL)
                            .setIsTrackable(false)
                            .setWeight(0)
                            .setPlannedIntervalStart(DATES[5].atTime(NOW_TIME))
                            .setPlannedIntervalEnd(DATES[5].atTime(NOW_TIME).plusHours(1))
                    )
                    .setPlannedLaunchTime(LocalDateTime.now(clock))
                    .setHash("INTERWAREHOUSE")
            ),
            result.getSeparatedRegisters().stream().map(Pair::getValue).collect(Collectors.toList())
        );

        verify(logisticsPointReceiver).getLogisticPoint(FROM_WAREHOUSE_ID);
        verify(logisticsPointReceiver).getLogisticPoint(TO_WAREHOUSE_ID);
        verify(partnerScheduleReceiver).receive(
            eq(FROM_PARTNER_ID),
            eq(DATES[0]),
            eq(DATES[MAX_DAYS_OFFSET])
        );

        verifyNoMoreInteractions(logisticsPointReceiver, partnerScheduleReceiver);

        org.assertj.core.api.Assertions.assertThat(result.getSeparatedRegisters())
            .extracting(Pair::getSecond)
            .extracting(Transportation::getMovement)
            .extracting(Movement::getPartnerId)
            .allMatch(p -> p.equals(VIRTUAL_LINEHAUL));
    }

    @Test
    void createSeveral() {
        when(tmPropertyService.getBoolean(TmPropertyKey.ENABLE_INTERWAREHOUSE_ASSEMBLAGE_ONLY)).thenReturn(false);
        mockLms();

        TransportationTask task = new TransportationTask()
            .setLogisticPointFromId(FROM_WAREHOUSE_ID)
            .setLogisticPointToId(TO_WAREHOUSE_ID)
            .setRegisterId(REGISTER_ID);

        List<Register> registers = List.of(
            new Register().setId(1L),
            new Register().setId(2L),
            new Register().setId(3L)
        );

        Mockito.when(registerSplitterBySkuService.split(eq(REGISTER_ID), anyLong(), anyLong())).thenReturn(registers);

        var result = transportationTaskSplitter.split(task);

        Assertions.assertEquals(result.getTask(), task);
        Assertions.assertEquals(result.getSeparatedRegisters().size(), registers.size());
        Assertions.assertEquals(
            result.getSeparatedRegisters().stream().map(Pair::getKey).collect(Collectors.toList()),
            registers
        );
        Assertions.assertEquals(
            result.getSeparatedRegisters().stream().map(Pair::getValue).count(),
            registers.size()
        );
        org.assertj.core.api.Assertions.assertThat(result.getSeparatedRegisters())
            .extracting(Pair::getSecond)
            .extracting(Transportation::getMovement)
            .extracting(Movement::getPartnerId)
            .allMatch(p -> p.equals(VIRTUAL_LINEHAUL));
    }

    @Test
    void createSeveralWithFlags() {
        when(tmPropertyService.getBoolean(TmPropertyKey.ENABLE_INTERWAREHOUSE_ASSEMBLAGE_ONLY)).thenReturn(false);
        mockLms();

        List<Long> partnersWithEnabledAssemblage = List.of(1L, 3L);

        TransportationTask task = new TransportationTask()
            .setLogisticPointFromId(FROM_WAREHOUSE_ID)
            .setLogisticPointToId(TO_WAREHOUSE_ID)
            .setRegisterId(REGISTER_ID);

        List<Register> registers = List.of(
            new Register()
                .setId(1L)
                .setPartnerId(1L)
                .setItems(List.of(
                    new RegisterUnit().setCounts(List.of(new UnitCount().setCountType(CountType.FIT).setQuantity(1)
                    )))),
            new Register()
                .setId(2L)
                .setPartnerId(2L)
                .setItems(List.of(
                    new RegisterUnit().setCounts(List.of(new UnitCount().setCountType(CountType.FIT).setQuantity(1)
                    )))),
            new Register()
                .setId(3L)
                .setPartnerId(3L)
                .setItems(List.of(
                    new RegisterUnit().setCounts(List.of(new UnitCount().setCountType(CountType.FIT).setQuantity(1)
                    ))))
        );

        Mockito.when(tmPropertyService.getBoolean(TmPropertyKey.ENABLE_INTERWAREHOUSE_ASSEMBLAGE_ONLY))
            .thenReturn(true);
        Mockito.when(tmPropertyService.getList(TmPropertyKey.INTERWAREHOUSE_ASSEMBLAGE_ONLY_WH_IDS))
            .thenReturn(List.of(1L, 3L));

        Mockito.when(registerSplitterBySkuService.split(eq(REGISTER_ID), anyLong(), anyLong())).thenReturn(registers);

        var result = transportationTaskSplitter.split(task);

        result.getSeparatedRegisters().forEach(registerTransportationPair -> {
            if (partnersWithEnabledAssemblage.contains(registerTransportationPair.getFirst().getPartnerId())) {
                Assertions.assertEquals(
                    registerTransportationPair.getSecond().getTransportationType(),
                    TransportationType.FULFILLMENT_ASSEMBLAGE
                );
            }
        });

        Assertions.assertEquals(result.getTask(), task);
        Assertions.assertEquals(result.getSeparatedRegisters().size(), registers.size());
        Assertions.assertEquals(
            result.getSeparatedRegisters().stream().map(Pair::getKey).collect(Collectors.toList()),
            registers
        );
        Assertions.assertEquals(
            result.getSeparatedRegisters().stream().map(Pair::getValue).count(),
            registers.size()
        );
        org.assertj.core.api.Assertions.assertThat(result.getSeparatedRegisters())
            .extracting(Pair::getSecond)
            .extracting(Transportation::getMovement)
            .extracting(Movement::getPartnerId)
            .allMatch(p -> p.equals(TO_PARTNER_ID));
    }

    private void mockLms() {
        Mockito.when(logisticsPointReceiver.getLogisticPoint(FROM_WAREHOUSE_ID))
            .thenReturn(LogisticsPointResponse.newBuilder().id(FROM_WAREHOUSE_ID).partnerId(FROM_PARTNER_ID).build());
        Mockito.when(logisticsPointReceiver.getLogisticPoint(TO_WAREHOUSE_ID))
            .thenReturn(LogisticsPointResponse.newBuilder().id(TO_WAREHOUSE_ID).partnerId(TO_PARTNER_ID).build());

        when(partnerScheduleReceiver.receive(
            eq(FROM_PARTNER_ID),
            eq(DATES[0]),
            eq(DATES[MAX_DAYS_OFFSET])
        )).thenReturn(List.of(
            LogisticsPointGatesScheduleResponse.newBuilder()
                .logisticsPointId(ANOTHER_WAREHOUSE_ID)
                .schedule(List.of(
                    ScheduleDateTimeResponse.newBuilder().date(DATES[2]).build(),
                    ScheduleDateTimeResponse.newBuilder().date(DATES[3]).build()
                ))
                .build(),
            LogisticsPointGatesScheduleResponse.newBuilder()
                .logisticsPointId(FROM_WAREHOUSE_ID)
                .schedule(List.of(
                    ScheduleDateTimeResponse.newBuilder().date(DATES[0]).build(),
                    ScheduleDateTimeResponse.newBuilder().date(DATES[1]).build(),
                    ScheduleDateTimeResponse.newBuilder().date(DATES[4]).build(),
                    ScheduleDateTimeResponse.newBuilder().date(DATES[5]).build()
                ))
                .build()
        ));
    }
}
