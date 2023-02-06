package ru.yandex.market.delivery.transport_manager.facade.transportation_task;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.Movement;
import ru.yandex.market.delivery.transport_manager.domain.entity.MovementStatus;
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
import ru.yandex.market.delivery.transport_manager.domain.enums.RegisterStatus;
import ru.yandex.market.delivery.transport_manager.domain.enums.RegisterType;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationSource;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;
import ru.yandex.market.delivery.transport_manager.domain.enums.UnitType;

@DatabaseSetup("/repository/facade/transportation_task/saving/before.xml")
class TransportationTaskSplitterIntTest extends AbstractContextualTest {

    @Autowired
    private TransportationTaskSplitter transportationTaskSplitter;

    @BeforeEach
    void setUp() {
        clock.setFixed(
            LocalDateTime.of(2021, 6, 2, 15, 0, 0).atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()
        );
    }

    @ExpectedDatabase(
        value = "/repository/facade/transportation_task/saving/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testSaved() {

        TransportationTask task = new TransportationTask().setId(1L);
        var registers = List.of(
            Pair.of(register(), transportation()), Pair.of(register(), transportation())
        );

        TransportationTaskSplitter.Result result = new TransportationTaskSplitter.Result(task, registers);

        List<Long> transportationIds = transportationTaskSplitter.saveReturningTransportationIds(result);

        softly.assertThat(transportationIds).containsExactlyInAnyOrder(1L, 2L);
    }

    private Register register() {
        return new Register()
            .setStatus(RegisterStatus.NEW)
            .setType(RegisterType.PLAN)
            .setPallets(List.of(pallet()))
            .setComment("comment 2")
            .setItems(
                List.of(
                    unit(10001, UnitType.ITEM, List.of(count()), 10002L)
                )
            );
    }

    private RegisterUnit pallet() {
        return unit(10002, UnitType.PALLET, null, null);
    }

    private RegisterUnit unit(long id, UnitType type, List<UnitCount> counts, Long parentId) {
        return new RegisterUnit()
            .setId(id)
            .setType(type)
            .setCounts(counts)
            .setParentIds(Optional.ofNullable(parentId).map(Set::of).orElse(null));
    }

    private UnitCount count() {
        return new UnitCount()
            .setCountType(CountType.FIT)
            .setQuantity(1000);
    }

    private Transportation transportation() {
        return new Transportation()
            .setStatus(TransportationStatus.DRAFT)
            .setTransportationSource(TransportationSource.MBOC_EXTERNAL)
            .setTransportationType(TransportationType.INTERWAREHOUSE)
            .setScheme(TransportationScheme.NEW)
            .setOutboundUnit(
                new TransportationUnit()
                    .setStatus(TransportationUnitStatus.NEW)
                    .setType(TransportationUnitType.OUTBOUND)
                    .setExternalId(null)
                    .setPartnerId(1L)
                    .setLogisticPointId(10L)
                    .setPlannedIntervalStart(LocalDateTime.of(2021, 6, 3, 12, 0))
                    .setPlannedIntervalEnd(LocalDateTime.of(2021, 6, 3, 13, 0))
            )
            .setInboundUnit(
                new TransportationUnit()
                    .setStatus(TransportationUnitStatus.NEW)
                    .setType(TransportationUnitType.INBOUND)
                    .setExternalId(null)
                    .setPartnerId(2L)
                    .setLogisticPointId(20L)
                    .setPlannedIntervalStart(LocalDateTime.of(2021, 6, 3, 14, 0))
                    .setPlannedIntervalEnd(LocalDateTime.of(2021, 6, 3, 15, 0))
            )
            .setMovement(
                new Movement()
                    .setStatus(MovementStatus.DRAFT)
                    .setPartnerId(null)
                    .setIsTrackable(false)
                    .setWeight(0)
                    .setPlannedIntervalStart(LocalDateTime.of(2021, 6, 3, 13, 0))
                    .setPlannedIntervalEnd(LocalDateTime.of(2021, 6, 3, 14, 0))
            )
            .setPlannedLaunchTime(LocalDateTime.now(clock))
            .setHash("interwarehouse");
    }
}
