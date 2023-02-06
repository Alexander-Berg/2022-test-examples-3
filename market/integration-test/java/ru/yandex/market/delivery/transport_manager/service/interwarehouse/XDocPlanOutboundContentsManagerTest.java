package ru.yandex.market.delivery.transport_manager.service.interwarehouse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.dto.QuotaLimits;
import ru.yandex.market.delivery.transport_manager.domain.entity.DistributionCenterUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.TmPropertyKey;
import ru.yandex.market.delivery.transport_manager.domain.entity.XDockTransportationSuppliesOverride;
import ru.yandex.market.delivery.transport_manager.domain.entity.XDockTransportationSuppliesOverrideType;
import ru.yandex.market.delivery.transport_manager.repository.mappers.DistributionCenterUnitMapper;
import ru.yandex.market.delivery.transport_manager.service.xdoc.XDocPlanOutboundContentsManager;
import ru.yandex.market.delivery.transport_manager.service.xdoc.data.MetaSupplyGroup;
import ru.yandex.market.delivery.transport_manager.service.xdoc.data.SupplyGroupsForOutbound;
import ru.yandex.market.delivery.transport_manager.util.TimeUtil;

public class XDocPlanOutboundContentsManagerTest extends AbstractContextualTest {

    private static final Long TRANSPORTATION_ID = 1L;

    @Autowired
    private XDocPlanOutboundContentsManager manager;

    @Autowired
    private DistributionCenterUnitMapper mapper;

    @Autowired
    private TestableClock clock;

    @BeforeEach
    void setClock() {
        clock.setFixed(
            LocalDateTime.of(2021, 5, 12, 20, 0, 0).toInstant(ZoneOffset.UTC),
            ZoneId.ofOffset("UTC", ZoneOffset.UTC)
        );
    }

    @ParameterizedTest
    @MethodSource("noSupplyIdsOverrideParamsAndbreakBulkXdock")
    @DatabaseSetup("/repository/distribution_unit_center/single_supplies.xml")
    void testOnlySingleSupplies(XDockTransportationSuppliesOverride suppliesOverride, Boolean breakBulkXdock) {
        List<DistributionCenterUnit> availablePallets = mapper.getAvailablePallets(1L, 2L);
        List<String> supplies = manager
            .getMetaSupplyGroupsForWithdraw(
                availablePallets,
                new QuotaLimits(5, Long.MAX_VALUE),
                TRANSPORTATION_ID,
                Optional.ofNullable(suppliesOverride),
                breakBulkXdock
            )
            .getGroups()
            .stream()
            .map(MetaSupplyGroup::toString)
            .collect(Collectors.toList());
        softly.assertThat(supplies).containsExactly("[10]", "[12]", "[11]");
    }

    @ParameterizedTest
    @MethodSource("noSupplyIdsOverrideParamsAndbreakBulkXdock")
    @DatabaseSetup("/repository/distribution_unit_center/several_meta_supplies.xml")
    void testSeveralMetaSupplies(XDockTransportationSuppliesOverride suppliesOverride, Boolean breakBulkXdock) {
        List<DistributionCenterUnit> availablePallets = mapper.getAvailablePallets(1L, 2L);
        SupplyGroupsForOutbound supplies = manager
            .getMetaSupplyGroupsForWithdraw(
                availablePallets,
                new QuotaLimits(33, Long.MAX_VALUE),
                TRANSPORTATION_ID,
                Optional.ofNullable(suppliesOverride),
                breakBulkXdock
            );
        softly.assertThat(supplies.getGroups().stream().map(MetaSupplyGroup::toString).collect(Collectors.toList()))
            .containsExactlyInAnyOrder("[14, 15]", "[11, 12, 13]", "[17]");
        softly.assertThat(supplies.getGroups().get(0).getRequiredQuota()).isEqualTo(new QuotaLimits(2, 0));
        softly.assertThat(supplies.getGroups().get(1).getRequiredQuota()).isEqualTo(new QuotaLimits(4, 0));
        softly.assertThat(supplies.getGroups().get(2).getRequiredQuota()).isEqualTo(new QuotaLimits(2, 0));
        softly.assertThat(supplies.getUsedQuota()).isEqualTo(new QuotaLimits(8, 0));
    }

    @ParameterizedTest
    @MethodSource("noSupplyIdsOverrideParams")
    @DatabaseSetup({
        "/repository/distribution_unit_center/several_meta_supplies.xml",
        "/repository/distribution_unit_center/transportations_to_dc_and_ff.xml"
    })
    void testSeveralMetaSuppliesWithItemQuota(XDockTransportationSuppliesOverride suppliesOverride) {
        List<DistributionCenterUnit> availablePallets = mapper.getAvailablePallets(1L, 2L);
        SupplyGroupsForOutbound supplies = manager
            .getMetaSupplyGroupsForWithdraw(
                availablePallets,
                new QuotaLimits(33, Long.MAX_VALUE),
                TRANSPORTATION_ID,
                Optional.ofNullable(suppliesOverride),
                false
            );
        softly.assertThat(supplies.getGroups().stream().map(MetaSupplyGroup::toString).collect(Collectors.toList()))
            .containsExactlyInAnyOrder("[14, 15]", "[11, 12, 13]", "[17]");
        softly.assertThat(supplies.getGroups().get(0).getRequiredQuota()).isEqualTo(new QuotaLimits(2, 9));
        softly.assertThat(supplies.getGroups().get(1).getRequiredQuota()).isEqualTo(new QuotaLimits(4, 6));
        softly.assertThat(supplies.getGroups().get(2).getRequiredQuota()).isEqualTo(new QuotaLimits(2, 7));
        softly.assertThat(supplies.getUsedQuota()).isEqualTo(new QuotaLimits(8, 22));
    }

    @ParameterizedTest
    @MethodSource("noSupplyIdsOverrideParams")
    @DatabaseSetup({
        "/repository/distribution_unit_center/more_meta_supplies.xml",
        "/repository/distribution_unit_center/transportations_to_dc_and_ff.xml",
    })
    void testFirstOverdueThenSortedBySizeDesc(XDockTransportationSuppliesOverride suppliesOverride) {
        List<DistributionCenterUnit> availablePallets = mapper.getAvailablePallets(1L, 2L);
        SupplyGroupsForOutbound supplies = manager
            .getMetaSupplyGroupsForWithdraw(
                availablePallets,
                new QuotaLimits(33, Long.MAX_VALUE),
                TRANSPORTATION_ID,
                Optional.ofNullable(suppliesOverride),
                false
            );
        softly.assertThat(supplies.getGroups().stream().map(MetaSupplyGroup::toString).collect(Collectors.toList()))
            .containsExactlyInAnyOrder("[14, 15]", "[11, 12, 13]", "[18]", "[17]");
        softly.assertThat(supplies.getGroups().get(0).getRequiredQuota()).isEqualTo(new QuotaLimits(2, 9));
        softly.assertThat(supplies.getGroups().get(1).getRequiredQuota()).isEqualTo(new QuotaLimits(4, 6));
        softly.assertThat(supplies.getGroups().get(2).getRequiredQuota()).isEqualTo(new QuotaLimits(4, 0));
        softly.assertThat(supplies.getGroups().get(3).getRequiredQuota()).isEqualTo(new QuotaLimits(2, 7));
        softly.assertThat(supplies.getUsedQuota()).isEqualTo(new QuotaLimits(12, 22));
    }

    @ParameterizedTest
    @MethodSource("noSupplyIdsOverrideParams")
    @DatabaseSetup({
        "/repository/distribution_unit_center/more_meta_supplies.xml",
        "/repository/distribution_unit_center/transportations_to_dc_and_ff.xml",
    })
    void testFirstOverdueThenSortedBySizeDescCustomDeadline(XDockTransportationSuppliesOverride suppliesOverride) {
        mockProperty(TmPropertyKey.XDOC_DEADLINES_BY_LOGISTIC_POINT_PAIR, Map.of(
            "1,2", 400L
        ));
        List<DistributionCenterUnit> availablePallets = mapper.getAvailablePallets(1L, 2L);
        SupplyGroupsForOutbound supplies = manager
            .getMetaSupplyGroupsForWithdraw(
                availablePallets,
                new QuotaLimits(33, Long.MAX_VALUE),
                TRANSPORTATION_ID,
                Optional.ofNullable(suppliesOverride),
                false
            );
        softly.assertThat(supplies.getGroups().stream().map(MetaSupplyGroup::toString).collect(Collectors.toList()))
            .containsExactlyInAnyOrder("[14, 15]", "[11, 12, 13]", "[18]", "[17]");
        softly.assertThat(supplies.getGroups().get(0).getRequiredQuota()).isEqualTo(new QuotaLimits(4, 6));
        softly.assertThat(supplies.getGroups().get(1).getRequiredQuota()).isEqualTo(new QuotaLimits(4, 0));
        softly.assertThat(supplies.getGroups().get(2).getRequiredQuota()).isEqualTo(new QuotaLimits(2, 9));
        softly.assertThat(supplies.getGroups().get(3).getRequiredQuota()).isEqualTo(new QuotaLimits(2, 7));
        softly.assertThat(supplies.getUsedQuota()).isEqualTo(new QuotaLimits(12, 22));
    }

    @ParameterizedTest
    @MethodSource("noSupplyIdsOverrideParams")
    @DatabaseSetup({
        "/repository/distribution_unit_center/all_equal_meta_supplies.xml",
        "/repository/distribution_unit_center/transportations_to_dc_and_ff.xml",
    })
    void testAllOverdueAllSameSizeSortFIFO(XDockTransportationSuppliesOverride suppliesOverride) {
        mockProperty(TmPropertyKey.XDOC_DEADLINES_BY_LOGISTIC_POINT_PAIR, Map.of(
            "1,2", 10000L
        ));
        List<DistributionCenterUnit> availablePallets = mapper.getAvailablePallets(1L, 2L);
        SupplyGroupsForOutbound supplies = manager
            .getMetaSupplyGroupsForWithdraw(
                availablePallets,
                new QuotaLimits(33, Long.MAX_VALUE),
                TRANSPORTATION_ID,
                Optional.ofNullable(suppliesOverride),
                false
            );
        softly.assertThat(supplies.getGroups().stream().map(MetaSupplyGroup::toString).collect(Collectors.toList()))
            .containsExactlyInAnyOrder("[11]", "[12]", "[14]", "[17]", "[18]");
        softly.assertThat(toLocalDate(supplies.getGroups().get(0))).isEqualTo(LocalDate.of(
            2021,
            3,
            25
        ));
        softly.assertThat(toLocalDate(supplies.getGroups().get(1))).isEqualTo(LocalDate.of(
            2021,
            4,
            25
        ));
        softly.assertThat(toLocalDate(supplies.getGroups().get(2))).isEqualTo(LocalDate.of(
            2021,
            4,
            27
        ));
        softly.assertThat(toLocalDate(supplies.getGroups().get(3))).isEqualTo(LocalDate.of(
            2021,
            6,
            23
        ));
        softly.assertThat(toLocalDate(supplies.getGroups().get(4))).isEqualTo(LocalDate.of(
            2021,
            6,
            25
        ));
        softly.assertThat(supplies.getUsedQuota()).isEqualTo(new QuotaLimits(5, 14));
    }


    @ParameterizedTest
    @MethodSource("noSupplyIdsOverrideParamsAndbreakBulkXdock")
    @DatabaseSetup("/repository/distribution_unit_center/several_meta_supplies.xml")
    void nothingFits(XDockTransportationSuppliesOverride suppliesOverride, Boolean breakBulkXdock) {
        List<DistributionCenterUnit> availablePallets = mapper.getAvailablePallets(1L, 2L);
        SupplyGroupsForOutbound supplies = manager
            .getMetaSupplyGroupsForWithdraw(
                availablePallets,
                new QuotaLimits(1, Long.MAX_VALUE),
                TRANSPORTATION_ID,
                Optional.ofNullable(suppliesOverride),
                breakBulkXdock
            );
        softly.assertThat(supplies.getGroups()).isEmpty();
    }

    @Test
    @DatabaseSetup({
        "/repository/distribution_unit_center/all_equal_meta_supplies.xml",
        "/repository/distribution_unit_center/transportations_to_dc_and_ff.xml",
    })
    void testAllSupplyIdsOverrideOnly() {
        mockProperty(TmPropertyKey.XDOC_DEADLINES_BY_LOGISTIC_POINT_PAIR, Map.of(
            "1,2", 10000L
        ));
        List<DistributionCenterUnit> availablePallets = mapper.getAvailablePallets(1L, 2L);
        SupplyGroupsForOutbound supplies = manager
            .getMetaSupplyGroupsForWithdraw(
                availablePallets,
                new QuotaLimits(33, Long.MAX_VALUE),
                TRANSPORTATION_ID,
                Optional.of(new XDockTransportationSuppliesOverride()
                    .setType(XDockTransportationSuppliesOverrideType.ONLY_SELECTED)
                    .setSupplyIds(List.of(
                        "Зп-17"
                    ))),
                false
            );
        softly.assertThat(supplies.getGroups().stream().map(MetaSupplyGroup::toString).collect(Collectors.toList()))
            .containsExactlyInAnyOrder("[17]");
    }


    @Test
    @DatabaseSetup({
        "/repository/distribution_unit_center/all_equal_meta_supplies.xml",
        "/repository/distribution_unit_center/transportations_to_dc_and_ff.xml",
    })
    void testAllSupplyIdsOverridePrefer() {
        mockProperty(TmPropertyKey.XDOC_DEADLINES_BY_LOGISTIC_POINT_PAIR, Map.of(
            "1,2", 10000L
        ));
        List<DistributionCenterUnit> availablePallets = mapper.getAvailablePallets(1L, 2L);
        SupplyGroupsForOutbound supplies = manager
            .getMetaSupplyGroupsForWithdraw(
                availablePallets,
                new QuotaLimits(33, Long.MAX_VALUE),
                TRANSPORTATION_ID,
                Optional.of(new XDockTransportationSuppliesOverride()
                    .setType(XDockTransportationSuppliesOverrideType.PREFER_SELECTED)
                    .setSupplyIds(List.of(
                        "Зп-17"
                    ))),
                false
            );
        softly.assertThat(supplies.getGroups().stream().map(MetaSupplyGroup::toString).collect(Collectors.toList()))
            .containsExactlyInAnyOrder("[17]", "[11]", "[12]", "[14]", "[18]");
    }

    @ParameterizedTest
    @MethodSource("noSupplyIdsOverrideParams")
    @DatabaseSetup({
        "/repository/distribution_unit_center/several_meta_supplies.xml",
        "/repository/distribution_unit_center/transportations_to_ff_break_bulk_xdock.xml"
    })
    void testSeveralMetaSuppliesWithItemQuotabreakBulkXdock(XDockTransportationSuppliesOverride suppliesOverride) {
        List<DistributionCenterUnit> availablePallets = mapper.getAvailablePallets(1L, 2L);
        SupplyGroupsForOutbound supplies = manager
            .getMetaSupplyGroupsForWithdraw(
                availablePallets,
                new QuotaLimits(33, Long.MAX_VALUE),
                TRANSPORTATION_ID,
                Optional.ofNullable(suppliesOverride),
                true
            );
        softly.assertThat(supplies.getGroups().stream().map(MetaSupplyGroup::toString).collect(Collectors.toList()))
            .containsExactlyInAnyOrder("[14, 15]", "[11, 12, 13]", "[17]");
        softly.assertThat(supplies.getGroups().get(0).getRequiredQuota()).isEqualTo(new QuotaLimits(2, 9));
        softly.assertThat(supplies.getGroups().get(1).getRequiredQuota()).isEqualTo(new QuotaLimits(4, 6));
        softly.assertThat(supplies.getGroups().get(2).getRequiredQuota()).isEqualTo(new QuotaLimits(2, 7));
        softly.assertThat(supplies.getUsedQuota()).isEqualTo(new QuotaLimits(8, 22));
    }

    @ParameterizedTest
    @MethodSource("noSupplyIdsOverrideParams")
    @DatabaseSetup({
        "/repository/distribution_unit_center/more_meta_supplies.xml",
        "/repository/distribution_unit_center/transportations_to_ff_break_bulk_xdock.xml"
    })
    void testFirstOverdueThenSortedBySizeDescbreakBulkXdock(XDockTransportationSuppliesOverride suppliesOverride) {
        List<DistributionCenterUnit> availablePallets = mapper.getAvailablePallets(1L, 2L);
        SupplyGroupsForOutbound supplies = manager
            .getMetaSupplyGroupsForWithdraw(
                availablePallets,
                new QuotaLimits(33, Long.MAX_VALUE),
                TRANSPORTATION_ID,
                Optional.ofNullable(suppliesOverride),
                true
            );
        softly.assertThat(supplies.getGroups().stream().map(MetaSupplyGroup::toString).collect(Collectors.toList()))
            .containsExactlyInAnyOrder("[14, 15]", "[11, 12, 13]", "[18]", "[17]");
        softly.assertThat(supplies.getGroups().get(0).getRequiredQuota()).isEqualTo(new QuotaLimits(2, 9));
        softly.assertThat(supplies.getGroups().get(1).getRequiredQuota()).isEqualTo(new QuotaLimits(4, 6));
        softly.assertThat(supplies.getGroups().get(2).getRequiredQuota()).isEqualTo(new QuotaLimits(4, 0));
        softly.assertThat(supplies.getGroups().get(3).getRequiredQuota()).isEqualTo(new QuotaLimits(2, 7));
        softly.assertThat(supplies.getUsedQuota()).isEqualTo(new QuotaLimits(12, 22));
    }

    @ParameterizedTest
    @MethodSource("noSupplyIdsOverrideParams")
    @DatabaseSetup({
        "/repository/distribution_unit_center/more_meta_supplies.xml",
        "/repository/distribution_unit_center/transportations_to_ff_break_bulk_xdock.xml"
    })
    void testFirstOverdueThenSortedBySizeDescCustomDeadlinebreakBulkXdock(
        XDockTransportationSuppliesOverride suppliesOverride
    ) {
        mockProperty(TmPropertyKey.XDOC_DEADLINES_BY_LOGISTIC_POINT_PAIR, Map.of(
            "1,2", 400L
        ));
        List<DistributionCenterUnit> availablePallets = mapper.getAvailablePallets(1L, 2L);
        SupplyGroupsForOutbound supplies = manager
            .getMetaSupplyGroupsForWithdraw(
                availablePallets,
                new QuotaLimits(33, Long.MAX_VALUE),
                TRANSPORTATION_ID,
                Optional.ofNullable(suppliesOverride),
                true
            );
        softly.assertThat(supplies.getGroups().stream().map(MetaSupplyGroup::toString).collect(Collectors.toList()))
            .containsExactlyInAnyOrder("[14, 15]", "[11, 12, 13]", "[18]", "[17]");
        softly.assertThat(supplies.getGroups().get(0).getRequiredQuota()).isEqualTo(new QuotaLimits(4, 6));
        softly.assertThat(supplies.getGroups().get(1).getRequiredQuota()).isEqualTo(new QuotaLimits(4, 0));
        softly.assertThat(supplies.getGroups().get(2).getRequiredQuota()).isEqualTo(new QuotaLimits(2, 9));
        softly.assertThat(supplies.getGroups().get(3).getRequiredQuota()).isEqualTo(new QuotaLimits(2, 7));
        softly.assertThat(supplies.getUsedQuota()).isEqualTo(new QuotaLimits(12, 22));
    }

    @ParameterizedTest
    @MethodSource("noSupplyIdsOverrideParams")
    @DatabaseSetup({
        "/repository/distribution_unit_center/all_equal_meta_supplies.xml",
        "/repository/distribution_unit_center/transportations_to_ff_break_bulk_xdock.xml"
    })
    void testAllOverdueAllSameSizeSortFIFObreakBulkXdock(XDockTransportationSuppliesOverride suppliesOverride) {
        mockProperty(TmPropertyKey.XDOC_DEADLINES_BY_LOGISTIC_POINT_PAIR, Map.of(
            "1,2", 10000L
        ));
        List<DistributionCenterUnit> availablePallets = mapper.getAvailablePallets(1L, 2L);
        SupplyGroupsForOutbound supplies = manager
            .getMetaSupplyGroupsForWithdraw(
                availablePallets,
                new QuotaLimits(33, Long.MAX_VALUE),
                TRANSPORTATION_ID,
                Optional.ofNullable(suppliesOverride),
                true
            );
        softly.assertThat(supplies.getGroups().stream().map(MetaSupplyGroup::toString).collect(Collectors.toList()))
            .containsExactlyInAnyOrder("[11]", "[12]", "[14]", "[17]", "[18]");
        softly.assertThat(toLocalDate(supplies.getGroups().get(0))).isEqualTo(LocalDate.of(
            2021,
            3,
            25
        ));
        softly.assertThat(toLocalDate(supplies.getGroups().get(1))).isEqualTo(LocalDate.of(
            2021,
            4,
            25
        ));
        softly.assertThat(toLocalDate(supplies.getGroups().get(2))).isEqualTo(LocalDate.of(
            2021,
            4,
            27
        ));
        softly.assertThat(toLocalDate(supplies.getGroups().get(3))).isEqualTo(LocalDate.of(
            2021,
            6,
            23
        ));
        softly.assertThat(toLocalDate(supplies.getGroups().get(4))).isEqualTo(LocalDate.of(
            2021,
            6,
            25
        ));
        softly.assertThat(supplies.getUsedQuota()).isEqualTo(new QuotaLimits(5, 14));
    }

    private LocalDate toLocalDate(MetaSupplyGroup group) {
        return group.getMinSupplyTime().atZone(TimeUtil.DEFAULT_ZONE_OFFSET).toLocalDate();
    }

    @Test
    @DatabaseSetup({
        "/repository/distribution_unit_center/all_equal_meta_supplies.xml",
        "/repository/distribution_unit_center/transportations_to_ff_break_bulk_xdock.xml",
    })
    void testAllSupplyIdsOverrideOnlyBreakBulkXdock() {
        mockProperty(TmPropertyKey.XDOC_DEADLINES_BY_LOGISTIC_POINT_PAIR, Map.of(
            "1,2", 10000L
        ));
        List<DistributionCenterUnit> availablePallets = mapper.getAvailablePallets(1L, 2L);
        SupplyGroupsForOutbound supplies = manager
            .getMetaSupplyGroupsForWithdraw(
                availablePallets,
                new QuotaLimits(33, Long.MAX_VALUE),
                TRANSPORTATION_ID,
                Optional.of(new XDockTransportationSuppliesOverride()
                    .setType(XDockTransportationSuppliesOverrideType.ONLY_SELECTED)
                    .setSupplyIds(List.of(
                        "Зп-17"
                    ))),
                true
            );
        softly.assertThat(supplies.getGroups().stream().map(MetaSupplyGroup::toString).collect(Collectors.toList()))
            .containsExactlyInAnyOrder("[17]");
    }


    @Test
    @DatabaseSetup({
        "/repository/distribution_unit_center/all_equal_meta_supplies.xml",
        "/repository/distribution_unit_center/transportations_to_ff_break_bulk_xdock.xml",
    })
    void testAllSupplyIdsOverridePreferBreakBulkXdock() {
        mockProperty(TmPropertyKey.XDOC_DEADLINES_BY_LOGISTIC_POINT_PAIR, Map.of(
            "1,2", 10000L
        ));
        List<DistributionCenterUnit> availablePallets = mapper.getAvailablePallets(1L, 2L);
        SupplyGroupsForOutbound supplies = manager
            .getMetaSupplyGroupsForWithdraw(
                availablePallets,
                new QuotaLimits(33, Long.MAX_VALUE),
                TRANSPORTATION_ID,
                Optional.of(new XDockTransportationSuppliesOverride()
                    .setType(XDockTransportationSuppliesOverrideType.PREFER_SELECTED)
                    .setSupplyIds(List.of(
                        "Зп-17"
                    ))),
                true
            );
        softly.assertThat(supplies.getGroups().stream().map(MetaSupplyGroup::toString).collect(Collectors.toList()))
            .containsExactlyInAnyOrder("[17]", "[11]", "[12]", "[14]", "[18]");
    }

    /**
     * Тут проверяем, что null сработает так же, как и XDockTransportationSuppliesOverrideType.AUTO для всех тестов
     */
    static Stream<Arguments> noSupplyIdsOverrideParamsAndBreakBulkXdock() {
        return Stream.of(
            Arguments.of(
                new XDockTransportationSuppliesOverride().setType(XDockTransportationSuppliesOverrideType.AUTO),
                true
            ),
            Arguments.of(null, true),
            Arguments.of(
                new XDockTransportationSuppliesOverride().setType(XDockTransportationSuppliesOverrideType.AUTO),
                false
            ),
            Arguments.of(null, false)
        );
    }
    /**
     * Тут проверяем, что null сработает так же, как и XDockTransportationSuppliesOverrideType.AUTO для всех тестов
     */
    static Stream<Arguments> noSupplyIdsOverrideParams() {
        return Stream.of(
            Arguments.of(
                new XDockTransportationSuppliesOverride().setType(XDockTransportationSuppliesOverrideType.AUTO)
            ),
            Arguments.of(new Object[]{null})
        );
    }
}
