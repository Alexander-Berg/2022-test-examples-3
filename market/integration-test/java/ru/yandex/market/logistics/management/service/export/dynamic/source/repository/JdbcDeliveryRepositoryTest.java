package ru.yandex.market.logistics.management.service.export.dynamic.source.repository;

import java.sql.Date;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerType;
import ru.yandex.market.logistics.management.domain.entity.type.PointType;
import ru.yandex.market.logistics.management.entity.type.CapacityService;
import ru.yandex.market.logistics.management.entity.type.CapacityType;
import ru.yandex.market.logistics.management.entity.type.DeliveryType;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.DeliveryDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.DeliveryIntervalDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.LogisticsPointDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerCapacityDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerExternalParamTypeDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerExternalParamValueDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerHandlingTimeDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerRouteDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.ScheduleDayDto;
import ru.yandex.market.logistics.management.util.CleanDatabase;

@CleanDatabase
@Sql("/data/repository/jdbc/delivery.sql")
class JdbcDeliveryRepositoryTest extends AbstractContextualTest {
    public static final LocalDate START_DATE = LocalDate.of(2020, 9, 7);
    @Autowired
    private JdbcDeliveryRepository jdbcDeliveryRepository;

    @Test
    public void testFindAll() {
        softly
            .assertThat(jdbcDeliveryRepository.findAll(Date.valueOf(START_DATE), Constants.BERU))
            .isEqualTo(List.of(
                new DeliveryDto(
                    124L,
                    "My delivery",
                    1,
                    PartnerType.DELIVERY,
                    PartnerStatus.ACTIVE,
                    Set.of(
                        new LogisticsPointDto(1L, 1L, PointType.WAREHOUSE, 1, Set.of(1, 2, 5)),
                        new LogisticsPointDto(2L, 2L, PointType.WAREHOUSE, 1, Set.of())
                    ),
                    Set.of(
                        new PartnerExternalParamValueDto(
                            new PartnerExternalParamTypeDto(PartnerExternalParamType.IS_DROPOFF.name()),
                            "1"
                        )
                    ),
                    Set.of(
                        new PartnerRouteDto(225, 1, Set.of(1))
                    ),
                    Set.of(
                        new PartnerCapacityDto(
                            1L,
                            CapacityType.REGULAR,
                            CapacityService.DELIVERY,
                            DeliveryType.COURIER,
                            225,
                            2,
                            Constants.START_DATE,
                            null)
                            .addPartnerCapacityDayOff(new PartnerCapacityDto.DayOff(null, START_DATE)),
                        new PartnerCapacityDto(
                            5L,
                            CapacityType.REGULAR,
                            CapacityService.DELIVERY,
                            DeliveryType.COURIER,
                            225,
                            2,
                            null,
                            Collections.emptySet())
                    ),
                    Set.of(
                        new PartnerHandlingTimeDto(2, 3, Duration.ofHours(1))
                    ),
                    "My delivery name",
                    1,
                    "tt1",
                    Set.of(
                        new DeliveryIntervalDto(1L, 2, Set.of(
                            new ScheduleDayDto(2, LocalTime.of(10, 0), LocalTime.of(20, 0))
                        ))
                    )
                )
            ));
    }
}
