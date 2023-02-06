package ru.yandex.market.delivery.transport_manager.repository;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.DistributionCenterRootUnitWithCount;
import ru.yandex.market.delivery.transport_manager.domain.entity.DistributionCenterUnitCargoType;
import ru.yandex.market.delivery.transport_manager.domain.entity.DistributionCenterUnitType;
import ru.yandex.market.delivery.transport_manager.domain.filter.DistributionCenterRootUnitsFilter;
import ru.yandex.market.delivery.transport_manager.repository.mappers.DistributionCenterUnitMapper;

@DatabaseSetup("/repository/distribution_unit_center/distribution_center_ff_units.xml")
public class DistributionCenterUnitMapperSearchTest extends AbstractContextualTest {
    @Autowired
    private DistributionCenterUnitMapper mapper;

    @Test
    void searchByInboundTimeFrom() {
        DistributionCenterRootUnitsFilter filter = DistributionCenterRootUnitsFilter.builder()
            .inboundTimeFrom(Instant.parse("2021-04-28T11:00:00Z"))
            .build();

        List<DistributionCenterRootUnitWithCount> units = mapper.findRootUnits(
            filter,
            100
        );

        softly.assertThat(units.stream().map(DistributionCenterRootUnitWithCount::getId).collect(Collectors.toList()))
            .containsExactlyInAnyOrder(8L, 11L, 14L, 16L, 7L, 15L, 17L);
    }

    @Test
    void searchByInboundTimeTo() {
        DistributionCenterRootUnitsFilter filter = DistributionCenterRootUnitsFilter.builder()
            .inboundTimeTo(Instant.parse("2021-04-28T11:00:00Z"))
            .build();

        List<DistributionCenterRootUnitWithCount> units = mapper.findRootUnits(
            filter,
            100
        );

        softly.assertThat(units.stream().map(DistributionCenterRootUnitWithCount::getId).collect(Collectors.toList()))
            .containsExactlyInAnyOrder(5L);
    }

    @Test
    void simpleSearch() {
        DistributionCenterRootUnitsFilter filter = DistributionCenterRootUnitsFilter.builder()
            .frozen(true)
            .sortDirection(Sort.Direction.ASC)
            .build();

        List<DistributionCenterRootUnitWithCount> rootUnits = mapper.findRootUnits(
            filter,
            2
        );

        softly.assertThat(rootUnits).containsExactly(
            dto(
                5L,
                1L,
                2L,
                "2",
                DistributionCenterUnitType.PALLET,
                1,
                true,
                Instant.parse("2021-04-28T10:00:00Z"),
                "5",
                null,
                null
            ),
            dto(
                7L,
                1L,
                2L,
                "3",
                DistributionCenterUnitType.PALLET,
                0,
                true,
                Instant.parse("2021-04-29T10:00:00Z"),
                "6",
                null,
                null
            )
        );
    }

    @Test
    @DatabaseSetup("/repository/distribution_unit_center/distribution_center_ff_units_limits.xml")
    void multipleUnitsPerInboundSearch() {
        DistributionCenterRootUnitsFilter filter = DistributionCenterRootUnitsFilter.builder()
            .frozen(true)
            .sortDirection(Sort.Direction.ASC)
            .build();

        List<DistributionCenterRootUnitWithCount> rootUnits = mapper.findRootUnits(
            filter,
            2
        );
        softly.assertThat(rootUnits).containsExactly(
            dto(
                204L,
                100L,
                200L,
                "WP1",
                DistributionCenterUnitType.PALLET,
                0,
                true,
                Instant.parse("2021-04-29T10:00:00Z"),
                "lol2",
                null,
                DistributionCenterUnitCargoType.ANOMALY
            ),
            dto(
                205L,
                100L,
                200L,
                "WP2",
                DistributionCenterUnitType.PALLET,
                0,
                true,
                Instant.parse("2021-04-29T10:00:00Z"),
                "lol2",
                null,
                DistributionCenterUnitCargoType.ANOMALY
            ),
            dto(
                206L,
                1000L,
                200L,
                "WP3",
                DistributionCenterUnitType.PALLET,
                0,
                true,
                Instant.parse("2021-04-29T10:00:00Z"),
                "lol4",
                null,
                DistributionCenterUnitCargoType.ANOMALY
            ),
            dto(
                207L,
                1000L,
                200L,
                "WP4",
                DistributionCenterUnitType.PALLET,
                0,
                true,
                Instant.parse("2021-04-29T10:00:00Z"),
                "lol4",
                null,
                DistributionCenterUnitCargoType.ANOMALY
            )
        );
    }

    @Test
    void narrowSearch() {
        DistributionCenterRootUnitsFilter filter = DistributionCenterRootUnitsFilter.builder()
            .logisticPointFrom(1000L)
            .logisticPointTo(200L)
            .unitId("WP3")
            .cargoType(DistributionCenterUnitCargoType.ANOMALY)
            .frozen(false)
            .sortDirection(Sort.Direction.ASC)
            .inboundExternalId("lol")
            .build();

        List<DistributionCenterRootUnitWithCount> rootUnits = mapper.findRootUnits(
            filter,
            5
        );

        softly.assertThat(rootUnits.size()).isEqualTo(1);
        softly.assertThat(rootUnits.get(0)).isEqualTo(dto(
            16L,
            1000L,
            200L,
            "WP3",
            DistributionCenterUnitType.PALLET,
            0,
            false,
            Instant.parse("2021-04-29T10:00:00Z"),
            "lol14",
            null,
            DistributionCenterUnitCargoType.ANOMALY
        ));
    }

    @Test
    void fromIdSearch() {
        DistributionCenterRootUnitsFilter filter = DistributionCenterRootUnitsFilter.builder()
            .fromId(16L)
            .logisticPointTo(200L)
            .logisticPointFrom(1000L)
            .sortDirection(Sort.Direction.ASC)
            .build();
        List<DistributionCenterRootUnitWithCount> rootUnits = mapper.findRootUnits(
            filter,
            5
        );

        softly.assertThat(rootUnits.size()).isEqualTo(1);
        softly.assertThat(rootUnits.get(0)).isEqualTo(dto(
            17L,
            1000L,
            200L,
            "WP4",
            DistributionCenterUnitType.PALLET,
            0,
            true,
            Instant.parse("2021-04-29T10:00:00Z"),
            "lol15",
            null,
            DistributionCenterUnitCargoType.ANOMALY
        ));
    }

    @Test
    void toIdSearch() {
        DistributionCenterRootUnitsFilter filter = DistributionCenterRootUnitsFilter.builder()
            .toId(5L)
            .sortDirection(Sort.Direction.DESC)
            .logisticPointFrom(1L)
            .logisticPointTo(2L)
            .frozen(true)
            .type(DistributionCenterUnitType.PALLET)
            .build();

        List<DistributionCenterRootUnitWithCount> rootUnits = mapper.findRootUnits(
            filter,
            2
        );

        softly.assertThat(rootUnits.size()).isEqualTo(1);
        softly.assertThat(rootUnits.get(0)).isEqualTo(dto(
            5L,
            1L,
            2L,
            "2",
            DistributionCenterUnitType.PALLET,
            1,
            true,
            Instant.parse("2021-04-28T10:00:00Z"),
            "5",
            null,
            null
        ));
    }

    @SuppressWarnings("all")
    static DistributionCenterRootUnitWithCount dto(
        Long id,
        Long pointFrom,
        Long pointTo,
        String dcUnitId,
        DistributionCenterUnitType type,
        Integer count,
        Boolean frozen,
        Instant instant,
        String inboundId,
        Integer volume,
        DistributionCenterUnitCargoType cargoType
    ) {
        return new DistributionCenterRootUnitWithCount()
            .setId(id)
            .setDcUnitId(dcUnitId)
            .setFrozen(frozen)
            .setLogisticPointFromId(pointFrom)
            .setLogisticPointToId(pointTo)
            .setType(type)
            .setSubUnitCount(count)
            .setInboundTime(instant)
            .setInboundExternalId(inboundId)
            .setVolume(volume)
            .setCargoType(cargoType);
    }
}
