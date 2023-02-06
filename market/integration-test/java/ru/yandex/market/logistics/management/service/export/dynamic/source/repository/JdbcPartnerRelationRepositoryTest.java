package ru.yandex.market.logistics.management.service.export.dynamic.source.repository;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerType;
import ru.yandex.market.logistics.management.domain.entity.type.PointType;
import ru.yandex.market.logistics.management.domain.entity.type.ShipmentType;
import ru.yandex.market.logistics.management.service.client.PartnerService;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.CutoffDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.LogisticsPointDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerRelationDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.ProductRatingDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.ScheduleDayDto;
import ru.yandex.market.logistics.management.util.CleanDatabase;

@CleanDatabase
@Sql("/data/repository/jdbc/partner-relation.sql")
class JdbcPartnerRelationRepositoryTest extends AbstractContextualTest {
    public static final long RELATION_ID_1 = 125L;

    @Autowired
    private JdbcPartnerRelationRepository jdbcPartnerRelationRepository;

    @Test
    void testFindAll() {
        softly
            .assertThat(jdbcPartnerRelationRepository.findAllForDynamic(
                Constants.BERU,
                PartnerService.WAREHOUSE_TYPES,
                Set.of(PartnerType.DELIVERY),
                id -> new PartnerDto().setId(id)
            ))
            .containsExactlyInAnyOrder(new PartnerRelationDto(
                RELATION_ID_1,
                new PartnerDto().setId(124L), // provided by 4-th parameter function
                new PartnerDto().setId(123L), // provided by 4-th parameter function
                1,
                Set.of(
                    new ProductRatingDto(1, 1),
                    new ProductRatingDto(2, 2)
                ),
                true,
                new LogisticsPointDto(1L, 1L, PointType.WAREHOUSE, 1, Set.of(1, 3, 5)),
                Duration.ofDays(1),
                Duration.ofHours(1),
                ShipmentType.IMPORT,
                123L,
                1L,
                Set.of(
                    new CutoffDto(LocalTime.of(10, 0), Duration.ofHours(1), 1),
                    new CutoffDto(LocalTime.of(11, 0), Duration.ofHours(2), 2)
                ),
                false,
                false,
                List.of(createDay(1), createDay(3), createDay(4))
            ));
    }

    private static ScheduleDayDto createDay(int day) {
        return new ScheduleDayDto(day, LocalTime.of(12, 0), LocalTime.of(13, 0));
    }
}
