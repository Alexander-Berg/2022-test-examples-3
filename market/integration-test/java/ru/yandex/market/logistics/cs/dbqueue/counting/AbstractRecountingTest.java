package ru.yandex.market.logistics.cs.dbqueue.counting;

import java.time.Clock;
import java.time.LocalDate;

import javax.annotation.Nonnull;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.cs.AbstractIntegrationTest;
import ru.yandex.market.logistics.cs.domain.entity.ServiceCapacityMapping;
import ru.yandex.market.logistics.cs.domain.enumeration.DayOffType;
import ru.yandex.market.logistics.cs.domain.enumeration.UnitType;
import ru.yandex.market.logistics.cs.domain.jdbc.RecalculationChangedData;
import ru.yandex.market.logistics.cs.domain.jdbc.VersionedServiceCapacityMapping;

public abstract class AbstractRecountingTest extends AbstractIntegrationTest {
    @Autowired
    private Clock dbUnitClock;

    protected static final int RIGHT_SHIFT = 60;
    protected static final int LEFT_SHIFT = 1;
    protected static final long SERVICE_ID20 = 20L;
    protected static final long SERVICE_ID30 = 30L;
    protected static final long SERVICE_ID40 = 40L;
    protected static final long SERVICE_ID50 = 50L;
    protected static final long CAPACITY_ID1 = 1L;
    protected static final long CAPACITY_ID2 = 2L;
    protected static final long CAPACITY_ID3 = 3L;
    protected static final long CAPACITY_ID4 = 4L;

    protected LocalDate now() {
        return LocalDate.now(dbUnitClock);
    }

    protected LocalDate startDate() {
        return now().minusDays(LEFT_SHIFT);
    }

    protected LocalDate finishDate() {
        return now().plusDays(RIGHT_SHIFT);
    }

    @Nonnull
    protected VersionedServiceCapacityMapping versionedMapping(Long id, Long serviceId, Long capacityId, Long version) {
        ServiceCapacityMapping mapping = ServiceCapacityMapping.builder()
            .id(id)
            .serviceId(serviceId)
            .capacityId(capacityId)
            .build();
        return new VersionedServiceCapacityMapping(mapping, version);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    @Nonnull
    protected RecalculationChangedData changedData(
        Long counterId,
        Long capacityId,
        Integer counter,
        Integer diff,
        LocalDate day,
        UnitType unitType,
        Long threshold,
        DayOffType dayOffType
    ) {
        return new RecalculationChangedData(
            counterId,
            capacityId,
            counter,
            diff,
            day,
            unitType,
            threshold,
            false,
            dayOffType
        );
    }
}
