package ru.yandex.market.logistics.management.service.export.dynamic.source.repository;

import java.sql.Date;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerType;
import ru.yandex.market.logistics.management.domain.entity.type.PointType;
import ru.yandex.market.logistics.management.entity.type.CapacityService;
import ru.yandex.market.logistics.management.entity.type.CapacityType;
import ru.yandex.market.logistics.management.entity.type.DeliveryType;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;
import ru.yandex.market.logistics.management.service.client.PartnerService;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.LogisticsPointDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerCapacityDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerExternalParamTypeDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerExternalParamValueDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerHandlingTimeDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerRouteDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.WarehouseDto;
import ru.yandex.market.logistics.management.util.CleanDatabase;

@CleanDatabase
class JdbcWarehouseRepositoryTest extends AbstractContextualTest {

    @Autowired
    private JdbcWarehouseRepository jdbcWarehouseRepository;

    @DatabaseSetup(value = "/data/repository/jdbc/warehouse.xml")
    @Test
    public void testFindAll() {
        List<WarehouseDto> actual = jdbcWarehouseRepository.findAll(
            PartnerService.WAREHOUSE_TYPES,
            Constants.BERU,
            Date.valueOf(Constants.START_DATE)
        );

        List<WarehouseDto> expected = List.of(
            new WarehouseDto(
                123L,
                "My sort center",
                1,
                PartnerType.SORTING_CENTER,
                PartnerStatus.ACTIVE,
                Set.of(
                    new LogisticsPointDto(3L, 3L, PointType.WAREHOUSE, 1, Set.of(1, 2, 5))
                ),
                Set.of(
                    new PartnerExternalParamValueDto(
                        new PartnerExternalParamTypeDto(PartnerExternalParamType.IS_DROPOFF.name()),
                        "1"
                    )
                ),
                Collections.emptySet(),
                Collections.emptySet(),
                Set.of(
                    new PartnerHandlingTimeDto(225, 225, Duration.ofHours(2))
                )
            ),
            new WarehouseDto(
                124L,
                "My warehouse",
                1,
                PartnerType.FULFILLMENT,
                PartnerStatus.ACTIVE,
                Set.of(
                    new LogisticsPointDto(1L, 1L, PointType.WAREHOUSE, 1, Set.of(1, 2, 5)),
                    new LogisticsPointDto(2L, 2L, PointType.WAREHOUSE, 1, Set.of())
                ),
                Collections.emptySet(),
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
                        null
                    )
                        .addPartnerCapacityDayOff(new PartnerCapacityDto.DayOff(null, Constants.START_DATE))
                ),
                Set.of()
            )
        );

        softly.assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }
}
