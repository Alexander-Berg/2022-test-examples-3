package ru.yandex.market.delivery.transport_manager.facade;

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

import ru.yandex.market.delivery.transport_manager.converter.RegisterConverter;
import ru.yandex.market.delivery.transport_manager.converter.TimeSlotConverter;
import ru.yandex.market.delivery.transport_manager.converter.TransportationPartnerInfoConverter;
import ru.yandex.market.delivery.transport_manager.converter.TransportationUnitConverter;
import ru.yandex.market.delivery.transport_manager.domain.entity.EntityType;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationPartnerInfo;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.Register;
import ru.yandex.market.delivery.transport_manager.domain.enums.RegisterStatus;
import ru.yandex.market.delivery.transport_manager.domain.enums.RegisterType;
import ru.yandex.market.delivery.transport_manager.exception.ResourceNotFoundException;
import ru.yandex.market.delivery.transport_manager.model.dto.RegisterDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationPartnerExtendedInfoDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationUnitDto;
import ru.yandex.market.delivery.transport_manager.model.enums.PartnerType;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationLegalInfoMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationPartnerInfoMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationUnitMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.register.RegisterMapper;
import ru.yandex.market.delivery.transport_manager.service.StatusHistoryService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class TransportationUnitFacadeTest {
    private TransportationUnitFacade transportationUnitFacade;
    private TransportationUnitMapper transportationUnitMapper;
    private TransportationMapper transportationMapper;
    private RegisterMapper registerMapper;
    private TransportationPartnerInfoMapper transportationPartnerInfoMapper;
    private TransportationLegalInfoMapper transportationLegalInfoMapper;
    private StatusHistoryService statusHistoryService;

    @BeforeEach
    void setUp() {
        transportationUnitMapper = mock(TransportationUnitMapper.class);
        transportationMapper = mock(TransportationMapper.class);
        registerMapper = mock(RegisterMapper.class);
        transportationPartnerInfoMapper = Mockito.mock(TransportationPartnerInfoMapper.class);
        transportationLegalInfoMapper = Mockito.mock(TransportationLegalInfoMapper.class);
        statusHistoryService = Mockito.mock(StatusHistoryService.class);
        transportationUnitFacade = new TransportationUnitFacade(
            transportationUnitMapper,
            transportationMapper,
            registerMapper,
            transportationPartnerInfoMapper,
            transportationLegalInfoMapper,
            statusHistoryService,
            new TransportationUnitConverter(new RegisterConverter(), new TimeSlotConverter()),
            new TransportationPartnerInfoConverter()
        );
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(
            transportationUnitMapper,
            registerMapper,
            transportationMapper,
            transportationPartnerInfoMapper,
            statusHistoryService
        );
    }

    @Test
    void getByIdWithRegistersNotFound() {
        when(transportationUnitMapper.getById(1L)).thenReturn(null);
        Assertions.assertThrows(
            ResourceNotFoundException.class,
            () -> transportationUnitFacade.getByIdWithRegisters(1L)
        );
        verify(transportationUnitMapper).getById(1L);
    }

    @Test
    void getByIdWithRegisters() {
        when(transportationUnitMapper.getById(1L))
            .thenReturn(new TransportationUnit()
                .setId(1L)
                .setType(TransportationUnitType.INBOUND)
                .setStatus(TransportationUnitStatus.SENT)
                .setExternalId("123")
                .setPartnerId(100L)
                .setLogisticPointId(1000L)
                .setPlannedIntervalStart(LocalDateTime.of(2021, 4, 7, 12, 0))
                .setPlannedIntervalEnd(LocalDateTime.of(2021, 4, 7, 13, 0))
            );
        when(registerMapper.getByTransportationUnitId(1L)).thenReturn(List.of(
            new Register()
                .setId(1L)
                .setPartnerId(100L)
                .setType(RegisterType.PLAN)
                .setStatus(RegisterStatus.NEW)
        ));
        when(transportationMapper.getIdByUnitId(1L)).thenReturn(12345L);
        final Instant changedAt = ZonedDateTime.of(
            2020, 12, 20, 10, 0, 0, 0, ZoneOffset.UTC
        ).toInstant();
        when(statusHistoryService.getLastChangedAt(EntityType.TRANSPORTATION_UNIT, 1L)).thenReturn(changedAt);
        when(transportationPartnerInfoMapper.get(12345L, 100L))
            .thenReturn(new TransportationPartnerInfo()
                .setPartnerId(100L)
                .setPartnerName("Roga&Kopyta")
                .setPartnerType(ru.yandex.market.logistics.management.entity.type.PartnerType.DROPSHIP)
            );

        Assertions.assertEquals(
            TransportationUnitDto.builder()
                .id(1L)
                .yandexId("TMU1")
                .transportationId(12345L)
                .type(ru.yandex.market.delivery.transport_manager.model.enums.TransportationUnitType.INBOUND)
                .status(ru.yandex.market.delivery.transport_manager.model.enums.TransportationUnitStatus.SENT)
                .externalId("123")
                .partner(
                    TransportationPartnerExtendedInfoDto.builder()
                        .id(100L)
                        .name("Roga&Kopyta")
                        .type(PartnerType.DROPSHIP)
                        .build()
                )
                .logisticPointId(1000L)
                .plannedIntervalStart(LocalDateTime.of(2021, 4, 7, 12, 0))
                .plannedIntervalEnd(LocalDateTime.of(2021, 4, 7, 13, 0))
                .registers(List.of(
                    RegisterDto.builder()
                        .id(1L)
                        .type(ru.yandex.market.delivery.transport_manager.model.enums.RegisterType.PLAN)
                        .status(ru.yandex.market.delivery.transport_manager.model.enums.RegisterStatus.NEW)
                        .partnerId(100L)
                        .build()
                ))
                .changedAt(changedAt)
                .build(),

            transportationUnitFacade.getByIdWithRegisters(1L)
        );
        verify(statusHistoryService).getLastChangedAt(EntityType.TRANSPORTATION_UNIT, 1L);
        verify(transportationPartnerInfoMapper).get(Mockito.anyLong(), Mockito.anyLong());
        verify(transportationUnitMapper).getById(1L);
        verify(transportationMapper).getIdByUnitId(1L);
        verify(registerMapper).getByTransportationUnitId(1L);
    }
}
