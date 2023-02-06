package ru.yandex.market.delivery.transport_manager;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.delivery.transport_manager.domain.entity.Movement;
import ru.yandex.market.delivery.transport_manager.domain.entity.MovementStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationSource;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;
import ru.yandex.market.delivery.transport_manager.repository.mappers.MovementMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationUnitMapper;
import ru.yandex.market.delivery.transport_manager.util.Profiles;

@Transactional
@ActiveProfiles(Profiles.INTEGRATION_TEST)
public class EntityFactory {

    @Autowired
    private TransportationMapper transportationMapper;
    @Autowired
    private TransportationUnitMapper transportationUnitMapper;
    @Autowired
    private MovementMapper movementMapper;

    @Autowired
    private Clock clock;

    public List<Transportation> createDummyTransportations(
        int num,
        TransportationType type,
        TransportationStatus status,
        boolean regular
    ) {
        return IntStream.range(0, num)
            .mapToObj(i -> createDummyTransportation(type, status, regular))
            .collect(Collectors.toList());
    }

    public Transportation createDummyTransportation(
        TransportationType transportationType,
        TransportationStatus status,
        boolean regular
    ) {
        Movement dummyMovement = createDummyMovement();
        TransportationUnit dummyInbound = createDummyUnit(TransportationUnitType.INBOUND);
        TransportationUnit dummyOutbound = createDummyUnit(TransportationUnitType.OUTBOUND);

        Transportation transportation = new Transportation()
            .setMovement(dummyMovement)
            .setInboundUnit(dummyInbound)
            .setOutboundUnit(dummyOutbound)
            .setTransportationType(transportationType)
            .setRegular(regular)
            .setDeleted(false)
            .setStatus(status)
            .setHash("")
            .setTransportationSource(TransportationSource.LMS_TM_MOVEMENT);

        Long id = transportationMapper.persist(transportation);
        return transportation.setId(id);
    }

    public TransportationUnit createDummyUnit(TransportationUnitType type) {
        TransportationUnit unit =
            new TransportationUnit()
                .setStatus(TransportationUnitStatus.NEW)
                .setLogisticPointId(1L)
                .setPlannedIntervalStart(LocalDateTime.now(clock))
                .setType(type);

        Long id = transportationUnitMapper.persist(unit);
        return unit.setId(id);
    }

    public Movement createDummyMovement() {
        Movement movement = new Movement()
            .setStatus(MovementStatus.NEW)
            .setPartnerId(1L);

        Long id = movementMapper.persist(movement);
        return movement.setId(id);
    }
}
