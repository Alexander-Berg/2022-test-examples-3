package ru.yandex.market.delivery.transport_manager.facade.transportation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.domain.dto.MovementDto;
import ru.yandex.market.delivery.transport_manager.domain.dto.TransportationUnitDto;
import ru.yandex.market.delivery.transport_manager.domain.entity.Movement;
import ru.yandex.market.delivery.transport_manager.domain.entity.TmPropertyKey;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationAdditionalData;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationRoutingConfig;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationScheduleRoutingConfig;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.domain.enums.DimensionsClass;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;
import ru.yandex.market.delivery.transport_manager.service.TmPropertyService;
import ru.yandex.market.delivery.transport_manager.service.TransportationService;
import ru.yandex.market.delivery.transport_manager.service.movement.MovementSaverService;
import ru.yandex.market.delivery.transport_manager.service.transportation.InitialStatusSelector;
import ru.yandex.market.delivery.transport_manager.service.transportation_unit.TransportationUnitService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TransportationFacadeTest {

    private TransportationFacade transportationFacade;

    private TransportationUnitService transportationUnitService;
    private MovementSaverService movementSaverService;
    private TransportationService transportationService;
    private TmPropertyService propertyService;

    @BeforeEach
    void before() {
        transportationUnitService = mock(TransportationUnitService.class);
        movementSaverService = mock(MovementSaverService.class);
        transportationService = mock(TransportationService.class);
        propertyService = mock(TmPropertyService.class);


        when(propertyService.getBoolean(TmPropertyKey.CAN_UPDATE_PLANNED_LAUNCH_TIME)).thenReturn(true);

        transportationFacade =
            new TransportationFacade(
                transportationUnitService,
                movementSaverService,
                transportationService,
                propertyService,
                new InitialStatusSelector(propertyService)
            );
    }

    @Test
    void createTransportationTest() {
        MovementDto movementDto = new MovementDto();
        TransportationUnitDto inboundUnitDto = new TransportationUnitDto().setType(TransportationUnitType.INBOUND);
        TransportationUnitDto outboundUnitDto = new TransportationUnitDto().setType(TransportationUnitType.OUTBOUND);
        TransportationUnit inboundTransportationUnit = new TransportationUnit()
            .setType(TransportationUnitType.INBOUND).setId(3L);
        TransportationUnit outboundTransportationUnit = new TransportationUnit()
            .setType(TransportationUnitType.OUTBOUND).setId(4L);
        Movement movement = new Movement().setId(5L);

        Transportation expectedInitializedTransportation = new Transportation()
            .setMovement(movement)
            .setInboundUnit(inboundTransportationUnit)
            .setOutboundUnit(outboundTransportationUnit)
            .setHash("hash")
            .setMovementSegmentId(100L)
            .setTransportationType(TransportationType.ORDERS_OPERATION)
            .setAdditionalData(new TransportationAdditionalData(
                new TransportationRoutingConfig(
                    true,
                    DimensionsClass.MEDIUM_SIZE_CARGO,
                    1.1D,
                    false,
                    "DEFAULT"
                )));

        when(transportationUnitService.persist(any(TransportationUnit.class)))
            .thenReturn(inboundTransportationUnit)
            .thenReturn(outboundTransportationUnit);

        when(movementSaverService.persist(any(Movement.class)))
            .thenReturn(movement);

        when(transportationService.persist(any(Transportation.class)))
            .thenReturn(expectedInitializedTransportation
            );

        Transportation actualInitializedTransportation = transportationFacade.createRegularTransportation(
            movementDto,
            inboundUnitDto,
            outboundUnitDto,
            "hash",
            null,
            TransportationType.ORDERS_OPERATION,
            100L,
            new TransportationScheduleRoutingConfig(
                null,
                true,
                DimensionsClass.MEDIUM_SIZE_CARGO,
                1.1D,
                false,
                "DEFAULT"
            )
        );

        verify(transportationUnitService, times(2)).persist(any(TransportationUnit.class));
        verify(movementSaverService, times(1)).persist(any(Movement.class));
        verify(transportationService, times(1)).persist(any(Transportation.class));
        assert expectedInitializedTransportation.equals(actualInitializedTransportation);
    }
}
