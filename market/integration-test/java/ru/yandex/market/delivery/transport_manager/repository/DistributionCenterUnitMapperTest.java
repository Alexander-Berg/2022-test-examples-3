package ru.yandex.market.delivery.transport_manager.repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.DistributionCenterUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.DistributionCenterUnitCargoType;
import ru.yandex.market.delivery.transport_manager.domain.entity.DistributionCenterUnitType;
import ru.yandex.market.delivery.transport_manager.domain.entity.DropoffPair;
import ru.yandex.market.delivery.transport_manager.domain.filter.DistributionCenterUnitSearchFilter;
import ru.yandex.market.delivery.transport_manager.dto.distribution_center.ResourceIdDto;
import ru.yandex.market.delivery.transport_manager.repository.mappers.DistributionCenterUnitMapper;
import ru.yandex.market.delivery.transport_manager.util.TimeUtil;

@DatabaseSetup("/repository/distribution_unit_center/distribution_center_units.xml")
class DistributionCenterUnitMapperTest extends AbstractContextualTest {
    @Autowired
    private DistributionCenterUnitMapper mapper;

    private static final DistributionCenterUnit MODEL =
        new DistributionCenterUnit()
            .setId(7L)
            .setDcUnitId("3")
            .setFrozen(true)
            .setLogisticPointFromId(1L)
            .setLogisticPointToId(2L)
            .setType(DistributionCenterUnitType.PALLET)
            .setParentId(null)
            .setInboundExternalId("6")
            .setInboundTime(
                ZonedDateTime.of(2021, 4, 29, 13, 0, 0, 0, ZoneId.of("Europe/Moscow"))
                    .toInstant()
            );

    @Test
    void get() {
        DistributionCenterUnit model = mapper.getById(7);
        assertThatModelEquals(MODEL, model);
    }

    @Test
    @DatabaseSetup(
        value = "/repository/distribution_unit_center/distribution_center_units.xml",
        type = DatabaseOperation.DELETE_ALL
    )
    @ExpectedDatabase(
        value = "/repository/distribution_unit_center/after/after_persist.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void persistNew() {
        Long persistedId = mapper.persist(
            new DistributionCenterUnit()
                .setDcUnitId("5")
                .setFrozen(false)
                .setLogisticPointFromId(1L)
                .setLogisticPointToId(2L)
                .setType(DistributionCenterUnitType.PALLET)
                .setParentId(null)
                .setInboundExternalId("6")
                .setInboundTime(
                    ZonedDateTime.of(2021, 4, 29, 10, 0, 0, 0, TimeUtil.DEFAULT_ZONE_OFFSET)
                        .toInstant()
                )
                .setCargoType(DistributionCenterUnitCargoType.XDOCK)
        );
        softly.assertThat(persistedId).isNotNull();
    }

    @Test
    @DatabaseSetup(
        value = "/repository/distribution_unit_center/distribution_center_units.xml",
        type = DatabaseOperation.DELETE_ALL
    )
    @ExpectedDatabase(
        value = "/repository/distribution_unit_center/after/after_persist.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void insertNew() {
        mapper.insert(
            new DistributionCenterUnit()
                .setDcUnitId("5")
                .setFrozen(false)
                .setLogisticPointFromId(1L)
                .setLogisticPointToId(2L)
                .setType(DistributionCenterUnitType.PALLET)
                .setParentId(null)
                .setInboundExternalId("6")
                .setInboundTime(
                    ZonedDateTime.of(2021, 4, 29, 10, 0, 0, 0, TimeUtil.DEFAULT_ZONE_OFFSET)
                        .toInstant()
                )
                .setCargoType(DistributionCenterUnitCargoType.XDOCK)
        );
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/distribution_unit_center/distribution_center_units.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void persistExisting() {
        mapper.persist(MODEL);
    }

    @Test
    void getByParentId() {
        Set<DistributionCenterUnit> boxes = mapper.getByParentId(1L);
        softly.assertThat(boxes).extracting(DistributionCenterUnit::getId).containsExactlyInAnyOrder(2L, 3L, 4L);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/distribution_unit_center/after/delete_by_outbound_point_dc_unit_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteAllByLogisticsPointFromIdAndDcUnitIdIn() {
        mapper.deleteUnitsForOutboundPointId(1L, Set.of("10", "11", "12", "13", "4", "5"));
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/distribution_unit_center/after/delete_by_direction_and_dc_unit_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteAllByDirectionAndDcUnitIdIn() {
        mapper.deleteUnitsForDirection(1L, 2L, Set.of("10", "11", "12", "13", "4", "5"));
    }


    @Test
    @ExpectedDatabase(
        value = "/repository/distribution_unit_center/after/delete_by_direction_null_tagret_and_dc_unit_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteAllByDirectionNullTargetAndDcUnitIdIn() {
        mapper.deleteUnitsForDirection(1L, null, Set.of("10", "11", "12", "13", "4", "5"));
    }

    @Test
    void getDcUnitIdsByLogisticsPointFromId() {
        Set<String> unitDcIds = mapper.getAllDcUnitIdsForOutboundPoint(1L);
        softly.assertThat(unitDcIds).containsExactlyInAnyOrder("1", "10", "11", "12", "2", "13", "3", "4", "5");
    }

    @Test
    void getDcUnitIdsByDirection() {
        Set<String> unitDcIds = mapper.getAllDcUnitIdsForDirection(1L, 2L);
        softly.assertThat(unitDcIds).containsExactlyInAnyOrder("1", "10", "11", "12", "3");
    }

    @Test
    void getDcUnitIdsByDirectionNullTarget() {
        Set<String> unitDcIds = mapper.getAllDcUnitIdsForDirection(1L, null);
        softly.assertThat(unitDcIds).containsExactlyInAnyOrder("4", "5");
    }

    @Test
    void getUnitIds() {
        List<ResourceIdDto> idsMapping =
            mapper.getUnitIdsForOutboundPoint(1L, List.of("1", "3", "10", "12", "13"));
        softly.assertThat(idsMapping).containsExactlyInAnyOrder(
            new ResourceIdDto(1L, "1"),
            new ResourceIdDto(7L, "3"),
            new ResourceIdDto(2L, "10"),
            new ResourceIdDto(4L, "12"),
            new ResourceIdDto(6L, "13")
        );
    }

    @Test
    void getUnitIdsMappingForOutboundPointId() {
        Map<String, Long> idsMapping =
            mapper.getUnitIdsMappingForOutboundPoint(1L, List.of("1", "3", "10", "12", "13", "4", "5"));
        softly.assertThat(idsMapping).isEqualTo(Map.of(
            "1", 1L,
            "3", 7L,
            "10", 2L,
            "12", 4L,
            "13", 6L,
            "4", 8L,
            "5", 9L
        ));
    }

    @Test
    void getUnitIdsMappingForDirection() {
        Map<String, Long> idsMapping =
            mapper.getUnitIdsMappingForDirection(1L, 2L, List.of("1", "3", "10", "12", "13", "4", "5"));
        softly.assertThat(idsMapping).isEqualTo(Map.of(
            "1", 1L,
            "3", 7L,
            "10", 2L,
            "12", 4L
        ));
    }

    @Test
    void getUnitIdsMappingForDirectionNullTarget() {
        Map<String, Long> idsMapping =
            mapper.getUnitIdsMappingForDirection(1L, null, List.of("1", "3", "10", "12", "13", "4", "5"));
        softly.assertThat(idsMapping).isEqualTo(Map.of(
            "4", 8L,
            "5", 9L
        ));
    }

    @Test
    void getFrozenDcUnitIdsByLogisticsPointFromId() {
        Set<String> frozenUnits = mapper.getFrozenDcUnitIdsForOutboundPoint(1L);
        softly.assertThat(frozenUnits).containsExactlyInAnyOrder("2", "3", "13", "5");
    }

    @Test
    void getFrozenDcUnitIdsByDirection() {
        Set<String> frozenUnits = mapper.getFrozenDcUnitIdsForDirection(1L, 2L);
        softly.assertThat(frozenUnits).containsExactlyInAnyOrder("3");
    }

    @Test
    void getFrozenDcUnitIdsByDirectionNullTarget() {
        Set<String> frozenUnits = mapper.getFrozenDcUnitIdsForDirection(1L, null);
        softly.assertThat(frozenUnits).containsExactlyInAnyOrder("5");
    }

    @Test
    @DatabaseSetup(
        value = "/repository/distribution_unit_center/additional_units.xml",
        type = DatabaseOperation.INSERT
    )
    void getAvailablePallets() {
        List<DistributionCenterUnit> availablePallets = mapper.getAvailablePallets(1L, 2L);
        softly.assertThat(availablePallets.stream().map(DistributionCenterUnit::getId)).containsExactlyInAnyOrder(
            1L
        );
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/distribution_unit_center/after/after_freeze.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void freezeWithRelations() {
        clock.setFixed(Instant.parse("2021-06-01T21:00:00Z"), ZoneOffset.UTC);
        Set<Long> frozen = mapper.freezeWithRelations(List.of(1L, 5L, 7L), instant(LocalDateTime.now(clock)));
        softly.assertThat(frozen).containsExactlyInAnyOrder(1L, 2L, 3L, 4L);
    }

    @Test
    void freezeWithRelationsEmptyNoException() {
        softly
            .assertThat(mapper.freezeWithRelations(List.of(), instant(LocalDateTime.now(clock))))
            .isEmpty();
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/distribution_unit_center/after/after_unfreeze.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void unfreezeWithRelations() {
        clock.setFixed(Instant.parse("2021-06-01T21:00:00Z"), ZoneOffset.UTC);
        mapper.unfreezeWithRelations(List.of(5L));
    }

    @Test
    void unfreezeWithRelationsEmptyNoException() {
        mapper.unfreezeWithRelations(List.of());
    }

    @Test
    @DatabaseSetup("/repository/distribution_unit_center/with_creation_timestamp.xml")
    void findUnitsBefore() {
        List<DistributionCenterUnit> outdated =
            mapper.findPalletsBefore(
                instant(LocalDateTime.of(2021, 5, 30, 14, 0))
            );

        softly.assertThat(outdated.stream().map(DistributionCenterUnit::getId).collect(Collectors.toList()))
            .containsExactlyInAnyOrder(1L, 7L);

    }

    @Test
    @DatabaseSetup("/repository/distribution_unit_center/with_frozen_timestamp.xml")
    void findUnitsFrozenBefore() {
        List<DistributionCenterUnit> frozenBefore = mapper.findPalletsFrozenBefore(
            instant(LocalDateTime.of(2021, 5, 30, 13, 0, 0)));
        softly.assertThat(frozenBefore.size()).isEqualTo(1);
        softly.assertThat(frozenBefore.get(0).getId()).isEqualTo(1);
    }

    @Test
    @DatabaseSetup("/repository/distribution_unit_center/dropoff_returns.xml")
    void getDropoffReturns() {
        Set<DropoffPair> dropoffReturns = mapper.getDropoffReturns();
        softly.assertThat(dropoffReturns).containsExactlyInAnyOrder(
            new DropoffPair(3L, 4L),
            new DropoffPair(3L, 5L),
            new DropoffPair(7L, 8L)
        );
    }

    @Test
    @DatabaseSetup("/repository/distribution_unit_center/distribution_center_units.xml")
    void search() {
        DistributionCenterUnitSearchFilter filter = new DistributionCenterUnitSearchFilter();
        filter.setDcUnitIds(List.of("1"));
        filter.setInboundExternalId(null);
        List<DistributionCenterUnit> result = mapper.search(filter);
        softly.assertThat(result.isEmpty()).isFalse();
        softly.assertThat(result.get(0).getDcUnitId()).isEqualTo("1");

        filter.setDcUnitIds(null);
        filter.setInboundExternalId("5");
        result = mapper.search(filter);
        softly.assertThat(result.size()).isEqualTo(2);
        softly.assertThat(result.get(0).getInboundExternalId()).isEqualTo("5");
        softly.assertThat(result.get(1).getInboundExternalId()).isEqualTo("5");

        filter.setDcUnitIds(List.of("13"));
        filter.setInboundExternalId("5");
        result = mapper.search(filter);

        softly.assertThat(result.size()).isEqualTo(1);
        softly.assertThat(result.get(0).getInboundExternalId()).isEqualTo("5");
        softly.assertThat(result.get(0).getDcUnitId()).isEqualTo("13");
    }

    @Test
    void getByInboundIdsNonFrozen() {
        softly
            .assertThat(mapper.getByInboundIds(List.of("1"), false))
            .usingElementComparatorIgnoringFields("created", "updated")
            .containsExactlyInAnyOrder(
                new DistributionCenterUnit()
                    .setId(2L)
                    .setDcUnitId("10")
                    .setFrozen(false)
                    .setLogisticPointFromId(1L)
                    .setLogisticPointToId(2L)
                    .setType(DistributionCenterUnitType.BOX)
                    .setParentId(1L)
                    .setInboundExternalId("1")
                    .setInboundTime(
                        ZonedDateTime.of(2021, 4, 26, 13, 0, 0, 0, ZoneId.of("Europe/Moscow"))
                            .toInstant()
                    ),
                new DistributionCenterUnit()
                    .setId(3L)
                    .setDcUnitId("11")
                    .setFrozen(false)
                    .setLogisticPointFromId(1L)
                    .setLogisticPointToId(2L)
                    .setType(DistributionCenterUnitType.BOX)
                    .setParentId(1L)
                    .setInboundExternalId("1")
                    .setInboundTime(
                        ZonedDateTime.of(2021, 4, 26, 13, 0, 0, 0, ZoneId.of("Europe/Moscow"))
                            .toInstant()
                    )
            );
    }

    @Test
    void getByInboundIdsFrozen() {
        softly
            .assertThat(mapper.getByInboundIds(List.of("1"), true))
            .isEmpty();

    }

    private Instant instant(LocalDateTime now) {
        return now.atZone(ZoneId.ofOffset("UTC", ZoneOffset.UTC)).toInstant();
    }
}
