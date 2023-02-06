package ru.yandex.market.delivery.transport_manager.facade;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.delivery.transport_manager.converter.MovementConverter;
import ru.yandex.market.delivery.transport_manager.converter.RegisterConverter;
import ru.yandex.market.delivery.transport_manager.converter.TransportConverter;
import ru.yandex.market.delivery.transport_manager.converter.TransportationPartnerInfoConverter;
import ru.yandex.market.delivery.transport_manager.converter.courier.MovementCourierConverter;
import ru.yandex.market.delivery.transport_manager.domain.entity.EntityType;
import ru.yandex.market.delivery.transport_manager.domain.entity.Movement;
import ru.yandex.market.delivery.transport_manager.domain.entity.MovementStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationPartnerInfo;
import ru.yandex.market.delivery.transport_manager.domain.entity.courier.MovementCourier;
import ru.yandex.market.delivery.transport_manager.domain.entity.courier.MovementCourierStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.Register;
import ru.yandex.market.delivery.transport_manager.domain.enums.RegisterStatus;
import ru.yandex.market.delivery.transport_manager.domain.enums.RegisterType;
import ru.yandex.market.delivery.transport_manager.exception.ResourceNotFoundException;
import ru.yandex.market.delivery.transport_manager.model.dto.MovementCourierDto;
import ru.yandex.market.delivery.transport_manager.model.dto.MovementDto;
import ru.yandex.market.delivery.transport_manager.model.dto.RegisterDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationPartnerExtendedInfoDto;
import ru.yandex.market.delivery.transport_manager.model.enums.PartnerType;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportMetadataMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationLegalInfoMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationPartnerInfoMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.register.RegisterMapper;
import ru.yandex.market.delivery.transport_manager.service.MovementService;
import ru.yandex.market.delivery.transport_manager.service.StatusHistoryService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class MovementFacadeTest {
    private MovementFacade movementFacade;
    private MovementService movementService;
    private TransportationMapper transportationMapper;
    private RegisterMapper registerMapper;
    private TransportationPartnerInfoMapper transportationPartnerInfoMapper;
    private TransportationLegalInfoMapper transportationLegalInfoMapper;
    private StatusHistoryService statusHistoryService;
    private TransportMetadataMapper transportMetadataMapper;

    @BeforeEach
    void setUp() {
        movementService = mock(MovementService.class);
        transportationMapper = mock(TransportationMapper.class);
        transportMetadataMapper = mock(TransportMetadataMapper.class);
        registerMapper = mock(RegisterMapper.class);
        transportationPartnerInfoMapper = Mockito.mock(TransportationPartnerInfoMapper.class);
        transportationLegalInfoMapper = Mockito.mock(TransportationLegalInfoMapper.class);
        statusHistoryService = Mockito.mock(StatusHistoryService.class);
        movementFacade = new MovementFacade(
            movementService,
            transportationMapper,
            registerMapper,
            transportationPartnerInfoMapper,
            transportationLegalInfoMapper,
            transportMetadataMapper,
            statusHistoryService,
            new MovementConverter(new RegisterConverter()),
            new MovementCourierConverter(Mockito.mock(Clock.class)),
            new TransportationPartnerInfoConverter(),
            new TransportConverter(new TestableClock())
        );
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(
            movementService,
            registerMapper,
            transportationMapper,
            transportationPartnerInfoMapper,
            statusHistoryService
        );
    }

    @Test
    void getByIdWithRegistersNotFound() {
        when(movementService.getById(1L)).thenReturn(null);
        Assertions.assertThrows(
            ResourceNotFoundException.class,
            () -> movementFacade.getByIdWithRegisters(1L)
        );
        verify(movementService).getById(1L);
    }

    @Test
    void getByIdWithRegisters() {
        final LocalDateTime plannedIntervalStart = LocalDateTime.of(2021, 4, 7, 12, 0);
        final LocalDateTime plannedIntervalEnd = LocalDateTime.of(2021, 4, 7, 13, 0);
        Movement movement = new Movement()
            .setId(1L)
            .setStatus(MovementStatus.IN_PROGRESS)
            .setExternalId("123")
            .setPartnerId(100L)
            .setPlannedIntervalStart(plannedIntervalStart)
            .setPlannedIntervalEnd(plannedIntervalEnd);
        when(movementService.getById(1L)).thenReturn(movement);
        when(registerMapper.getByMovementId(1L)).thenReturn(List.of(
            new Register()
                .setId(1L)
                .setPartnerId(100L)
                .setType(RegisterType.PLAN)
                .setStatus(RegisterStatus.NEW)
        ));
        when(transportationMapper.getIdsByMovementId(1L)).thenReturn(List.of(1L, 2L, 3L));
        final Instant changedAt = ZonedDateTime.of(
            2020, 12, 20, 10, 0, 0, 0, ZoneOffset.UTC
        ).toInstant();
        when(statusHistoryService.getLastChangedAt(EntityType.MOVEMENT, 1L)).thenReturn(changedAt);
        when(transportationPartnerInfoMapper.get(1L, 100L))
            .thenReturn(new TransportationPartnerInfo()
                .setPartnerId(100L)
                .setPartnerName("Roga&Kopyta")
                .setPartnerType(ru.yandex.market.logistics.management.entity.type.PartnerType.DROPSHIP)
            );
        MovementCourier courier = new MovementCourier(
            1L,
            movement.getId(),
            "ext-1",
            "Иван",
            "Иванов",
            "Иванович",
            null,
            null,
            "a123aa777",
            null,
            null,
            "+7(901) 111-11-11",
            "222",
            0L,
            "0",
            MovementCourierStatus.SENT,
            MovementCourier.Unit.ALL,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        when(movementService.getCourier(Mockito.any())).thenReturn(courier);

        Assertions.assertEquals(
            movementFacade.getByIdWithRegisters(1L),
            MovementDto.builder()
                .id(1L)
                .transportationIds(List.of(1L, 2L, 3L))
                .status(ru.yandex.market.delivery.transport_manager.model.enums.MovementStatus.IN_PROGRESS)
                .externalId("123")
                .partner(
                    TransportationPartnerExtendedInfoDto.builder()
                        .id(100L)
                        .name("Roga&Kopyta")
                        .type(PartnerType.DROPSHIP)
                        .build()
                )
                .registers(List.of(
                    RegisterDto.builder()
                        .id(1L)
                        .type(ru.yandex.market.delivery.transport_manager.model.enums.RegisterType.PLAN)
                        .status(ru.yandex.market.delivery.transport_manager.model.enums.RegisterStatus.NEW)
                        .partnerId(100L)
                        .build()
                ))
                .courier(MovementCourierDto.builder()
                    .name(courier.getName())
                    .surname(courier.getSurname())
                    .patronymic(courier.getPatronymic())
                    .carModel(courier.getCourierUid())
                    .carNumber(courier.getCarNumber())
                    .phone(courier.getPhone())
                    .phoneAdditional(courier.getPhoneAdditional())
                    .yandexUid(courier.getYandexUid())
                    .externalId("ext-1")
                    .courierUid(courier.getCourierUid())
                    .unit(MovementCourierDto.Unit.ALL)
                    .build())
                .plannedIntervalStart(plannedIntervalStart)
                .plannedIntervalEnd(plannedIntervalEnd)
                .changedAt(changedAt)
                .build()
        );
        verify(statusHistoryService).getLastChangedAt(EntityType.MOVEMENT, 1L);
        verify(transportationPartnerInfoMapper).get(Mockito.any(), Mockito.anyLong());
        verify(movementService).getById(1L);
        verify(movementService).getCourier(Mockito.eq(movement));
        verify(transportationMapper).getIdsByMovementId(1L);
        verify(registerMapper).getByMovementId(1L);
    }
}
