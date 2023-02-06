package ru.yandex.market.replenishment.autoorder.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.exception.UserWarningException;
import ru.yandex.market.replenishment.autoorder.model.DemandFilter;
import ru.yandex.market.replenishment.autoorder.model.DemandStatus;
import ru.yandex.market.replenishment.autoorder.model.DemandType;
import ru.yandex.market.replenishment.autoorder.model.RecommendationNew;
import ru.yandex.market.replenishment.autoorder.model.dto.DemandDTO;
import ru.yandex.market.replenishment.autoorder.model.dto.DemandIdentityDTO;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.ReplenishmentResult;
import ru.yandex.market.replenishment.autoorder.repository.postgres.DemandDTORepository;
import ru.yandex.market.replenishment.autoorder.repository.postgres.RecommendationRepository;
import ru.yandex.market.replenishment.autoorder.repository.postgres.ReplenishmentResultRepository;
import ru.yandex.market.replenishment.autoorder.service.environment.EnvironmentService;
import ru.yandex.market.replenishment.autoorder.utils.StreamUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.replenishment.autoorder.service.environment.EnvironmentConstants.USE_DENORMALIZED_INFOS_ENABLED;

public class DbDemandServiceTest extends FunctionalTest {
    private static final String LOGIN = "pupkin";

    @Autowired
    private DbDemandService demandService;

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    @Autowired
    private ReplenishmentResultRepository replenishmentResultRepository;

    @Autowired
    private EnvironmentService environmentService;

    @Test
    @DbUnitDataSet(before = "DbReplenishmentServiceTest.before.csv")
    public void testSaveCorrectedDemandForExport_ForProcessedDemand() {
        UserWarningException exception = assertThrows(UserWarningException.class, () ->
            demandService.saveDemandsForExport(DemandType.TYPE_1P,
                Collections.singletonList(new DemandIdentityDTO(2L, 1)),
                LOGIN));

        assertEquals("Потребность #2 была изменена. Пожалуйста, перезагрузите страницу, " +
            "чтобы увидеть актуальные данные", exception.getMessage());
    }

    @Test
    @DbUnitDataSet(before = "DbReplenishmentServiceTest.multiGroups.before.csv")
    public void testSaveCorrectedDemandForExport_MultiGroups() {
        List<Long> demandIds = demandService.saveDemandsForExport(
            DemandType.TYPE_1P,
            Arrays.asList(
                new DemandIdentityDTO(101L, 1),
                new DemandIdentityDTO(103L, 1)
            ),
            LOGIN
        ).stream().map(DemandDTO::getId).collect(Collectors.toList());

        List<DemandDTO> demands;
        try (final SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.SIMPLE)) {
            final DemandDTORepository mapper = sqlSession.getMapper(DemandDTORepository.class);
            demands = StreamUtils.streamFromIterable(mapper.getDemandsByIds(DemandType.TYPE_1P, demandIds))
                .sorted(Comparator.comparing(DemandDTO::getItems))
                .collect(Collectors.toList());
        }

        assertEquals(4, demands.size());
        demands.sort(Comparator.comparing(DemandDTO::getItems));

        LocalDate expectedDate = LocalDate.of(2019, 1, 5);

        DemandDTO demand = demands.get(0);
        assertNotNull(demand.getId());

        assertEquals(DemandStatus.PROCESSED, demand.getStatus());
        assertEquals(147L, demand.getWarehouse().getId().longValue());
        assertEquals("000123", demand.getSupplier().getRsId());
        assertEquals(expectedDate, demand.getDeliveryDate());
        assertEquals(1L, demand.getMskus());
        assertEquals(21L, demand.getItems());

        assertNotNull(demand.getSum());
        assertEquals(new BigDecimal(210), demand.getSum());

        assertNotNull(demand.getAdjustedMskus());
        assertEquals(1L, (long) demand.getAdjustedMskus());

        assertNotNull(demand.getAdjustedItems());
        assertEquals(21L, (long) demand.getAdjustedItems());

        assertNotNull(demand.getAdjustedSum());
        assertEquals(210.0, demand.getAdjustedSum(), 0.1);
        assertFalse(demand.isAutoProcessing());

        assertNotNull(demand.getSplitParentDemandId());
        assertEquals(103L, (long) demand.getSplitParentDemandId());

        demand = demands.get(1);
        assertNotNull(demand.getId());
        assertEquals(DemandStatus.PROCESSED, demand.getStatus());
        assertEquals(147L, demand.getWarehouse().getId().longValue());
        assertEquals("000123", demand.getSupplier().getRsId());
        assertEquals(expectedDate, demand.getDeliveryDate());
        assertEquals(1L, demand.getMskus());
        assertEquals(31L, demand.getItems());

        assertNotNull(demand.getSum());
        assertEquals(new BigDecimal(310), demand.getSum());

        assertNotNull(demand.getAdjustedMskus());
        assertEquals(1L, (long) demand.getAdjustedMskus());

        assertNotNull(demand.getAdjustedItems());
        assertEquals(35L, (long) demand.getAdjustedItems());

        assertNotNull(demand.getAdjustedSum());
        assertEquals(350.0, demand.getAdjustedSum(), 0.1);
        assertFalse(demand.isAutoProcessing());

        assertNotNull(demand.getSplitParentDemandId());
        assertEquals(103L, (long) demand.getSplitParentDemandId());

        demand = demands.get(2);
        assertEquals(DemandStatus.PROCESSED, demand.getStatus());
        assertEquals(145L, demand.getWarehouse().getId().longValue());
        assertEquals("000123", demand.getSupplier().getRsId());
        assertEquals(expectedDate, demand.getDeliveryDate());
        assertEquals(1L, demand.getMskus());
        assertEquals(51L, demand.getItems());

        assertNotNull(demand.getSum());
        assertEquals(new BigDecimal(510), demand.getSum());

        assertNotNull(demand.getAdjustedMskus());
        assertEquals(1L, (long) demand.getAdjustedMskus());

        assertNotNull(demand.getAdjustedItems());
        assertEquals(51L, (long) demand.getAdjustedItems());

        assertNotNull(demand.getAdjustedSum());
        assertEquals(510.0, demand.getAdjustedSum(), 0.1);
        assertFalse(demand.isAutoProcessing());

        assertNotNull(demand.getSplitParentDemandId());
        assertEquals(101L, (long) demand.getSplitParentDemandId());

        demand = demands.get(3);
        assertEquals(DemandStatus.PROCESSED, demand.getStatus());
        assertEquals(145L, demand.getWarehouse().getId().longValue());
        assertEquals("000123", demand.getSupplier().getRsId());
        assertEquals(expectedDate, demand.getDeliveryDate());
        assertEquals(1L, demand.getMskus());
        assertEquals(52L, demand.getItems());

        assertNotNull(demand.getSum());
        assertEquals(new BigDecimal(520), demand.getSum());

        assertNotNull(demand.getAdjustedMskus());
        assertEquals(1L, (long) demand.getAdjustedMskus());

        assertNotNull(demand.getAdjustedItems());
        assertEquals(52L, (long) demand.getAdjustedItems());

        assertNotNull(demand.getAdjustedSum());
        assertEquals(520.0, demand.getAdjustedSum(), 0.1);
        assertFalse(demand.isAutoProcessing());

        assertNotNull(demand.getSplitParentDemandId());
        assertEquals(101L, (long) demand.getSplitParentDemandId());
        final boolean useDenormalized = environmentService.getBooleanWithDefault(USE_DENORMALIZED_INFOS_ENABLED,
            false);
        List<RecommendationNew> recommendations;
        try (final SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.SIMPLE)) {
            final RecommendationRepository mapper = sqlSession.getMapper(RecommendationRepository.class);
            recommendations = StreamUtils.streamFromIterable(mapper.getAll(DemandType.TYPE_1P, useDenormalized))
                .filter(r -> r.getMsku() < 300 || r.getMsku() > 500)
                .sorted(Comparator.comparing(RecommendationNew::getPurchQty))
                .collect(Collectors.toList());
        }

        assertNotNull(recommendations);
        assertEquals(4, recommendations.size());

        assertThat(recommendations.get(0).getDemandId(), equalTo(demands.get(0).getId()));
        assertThat(recommendations.get(0).getMsku(), equalTo(600L));
        assertTrue(recommendations.get(0).isExported());
        assertThat(recommendations.get(0).getImportPrice(), equalTo(123.6));
        assertThat(recommendations.get(1).getDemandId(), equalTo(demands.get(1).getId()));
        assertThat(recommendations.get(1).getMsku(), equalTo(700L));
        assertTrue(recommendations.get(1).isExported());
        assertThat(recommendations.get(2).getDemandId(), equalTo(demands.get(2).getId()));
        assertThat(recommendations.get(2).getMsku(), equalTo(100L));
        assertTrue(recommendations.get(2).isExported());
        assertThat(recommendations.get(3).getDemandId(), equalTo(demands.get(3).getId()));
        assertThat(recommendations.get(3).getMsku(), equalTo(200L));
        assertTrue(recommendations.get(3).isExported());

        List<ReplenishmentResult> replenishmentResults = replenishmentResultRepository.findAll();
        replenishmentResults.sort(Comparator.comparing(ReplenishmentResult::getPurchQty));
        assertThat(replenishmentResults, hasSize(4));
        assertThat(replenishmentResults.get(0).getDemandId(), equalTo(demands.get(0).getId()));
        assertThat(replenishmentResults.get(0).getMsku(), equalTo(600L));
        assertThat(replenishmentResults.get(0).getExportTimestamp(), nullValue());
        assertThat(replenishmentResults.get(1).getDemandId(), equalTo(demands.get(1).getId()));
        assertThat(replenishmentResults.get(1).getMsku(), equalTo(700L));
        assertThat(replenishmentResults.get(1).getExportTimestamp(), nullValue());
        assertThat(replenishmentResults.get(2).getDemandId(), equalTo(demands.get(2).getId()));
        assertThat(replenishmentResults.get(2).getMsku(), equalTo(100L));
        assertThat(replenishmentResults.get(2).getExportTimestamp(), nullValue());
        assertThat(replenishmentResults.get(3).getDemandId(), equalTo(demands.get(3).getId()));
        assertThat(replenishmentResults.get(3).getMsku(), equalTo(200L));
        assertThat(replenishmentResults.get(3).getExportTimestamp(), nullValue());

        DemandDTO maybeDemand = getSingleDemandById(101L);
        assertNull(maybeDemand);

        maybeDemand = getSingleDemandById(103L);
        assertNull(maybeDemand);
    }

    @Test
    @DbUnitDataSet(before = "DbDemandServiceTest.testSaveDemandForExport_IgnoredZeroDemand.before.csv")
    public void testSaveDemandForExport_IgnoredZeroDemand() {
        Set<Long> demandIds = demandService.saveDemandsForExport(
            DemandType.TYPE_1P,
            Arrays.asList(
                new DemandIdentityDTO(101L, 1),
                new DemandIdentityDTO(222L, 1)
            ),
            LOGIN
        ).stream().map(DemandDTO::getId).collect(Collectors.toSet());
        assertEquals(Set.of(1L, 2L, 222L), demandIds);
    }

    private DemandDTO getSingleDemandById(long id) {
        DemandDTO demand;
        try (final SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.SIMPLE)) {
            final DemandDTORepository mapper = sqlSession.getMapper(DemandDTORepository.class);
            demand = mapper.getDemandById(DemandType.TYPE_1P, id);
        }

        return demand;
    }

    @Test
    @DbUnitDataSet(before = "DbReplenishmentServiceTest.multiGroups2.before.csv",
        after = "DbReplenishmentServiceTest.multiGroups2.after.csv")
    public void testSaveCorrectedDemandForExport2_MultiGroups() {
        LocalDateTime now = LocalDateTime.of(2021, 8, 9, 0, 0);
        setTestTime(now);
        demandService.saveDemandsForExport(DemandType.TYPE_1P,
            Collections.singletonList(new DemandIdentityDTO(101L, 1)), LOGIN);
    }

    @Test
    @DbUnitDataSet(before = "DbDemandServiceTest_replenishmentResultTransit.before.csv")
    public void testSaveDemand_replenishmentResultTransit() {
        demandService.saveDemandsForExport(DemandType.TYPE_1P, List.of(new DemandIdentityDTO(11L, 1),
            new DemandIdentityDTO(12L, 1)), "boris");
        final boolean useDenormalized = environmentService.getBooleanWithDefault(USE_DENORMALIZED_INFOS_ENABLED,
            false);
        List<RecommendationNew> recommendations;
        try (SqlSession session = sqlSessionFactory.openSession()) {
            final RecommendationRepository recommendationRepository = session.getMapper(RecommendationRepository.class);

            recommendationRepository.recreateReplenishmentResultTransitViewForGrouping();
            recommendationRepository.recreateReplenishmentResultTransitView();

            recommendations = StreamUtils.streamFromIterable(
                    recommendationRepository.getAll(DemandType.TYPE_1P, useDenormalized))
                .sorted(Comparator.comparingLong(RecommendationNew::getDemandId)
                    .thenComparingLong(RecommendationNew::getMsku)
                    .thenComparing(RecommendationNew::getGroupId))
                .collect(Collectors.toList());
        }

        assertThat(recommendations, hasSize(6));
        assertThat(recommendations.get(3).getWarehouseInfo().getReplenishmentResultTransit1p(), equalTo(10));
        assertThat(recommendations.get(4).getWarehouseInfo().getReplenishmentResultTransit1p(), equalTo(20));
        assertThat(recommendations.get(5).getWarehouseInfo().getReplenishmentResultTransit1p(), equalTo(30));
    }

    @Test
    @DbUnitDataSet(before = "DbDemandServiceTest.UpdateOrderInfo.before.csv")
    public void testGetOrdersByDemandId() {
        List<String> orders = demandService.getOrderIdsByDemandId(DemandType.TYPE_1P, 42L);
        Assert.assertThat(orders, empty());

        orders = demandService.getOrderIdsByDemandId(DemandType.TYPE_1P, 6L);
        orders.sort(String::compareTo);
        Assert.assertThat(orders, hasSize(2));
        Assert.assertThat(orders.get(0), equalTo("Зп-70"));
        Assert.assertThat(orders.get(1), equalTo("Зп-71"));
    }

    @Test
    @DbUnitDataSet(before = "DbDemandServiceTest.testSaveDemandForExport_MultiGroupsSpecialOrder.before.csv",
        after = "DbDemandServiceTest.testSaveDemandForExport_MultiGroupsSpecialOrder.after.csv")
    public void testSaveDemandsForExport_MultiGroupWithSpecialOrder() {
        demandService.saveDemandsForExport(DemandType.TYPE_1P,
            Collections.singletonList(new DemandIdentityDTO(101L, 1)), LOGIN);
    }

    @Test
    @DbUnitDataSet(before = "DbDemandServiceTest.testGetDemandsByOrderId_isOk.before.csv")
    public void testGetDemandsByOrderId_isOk() {
        DemandFilter filter = new DemandFilter();
        filter.setDemandType(DemandType.TYPE_1P);
        filter.setOrderIdFilter("Зп-1");
        List<DemandDTO> demands = demandService.getDemands(filter);
        assertNotNull(demands);
        assertEquals(2, demands.size());
        assertEquals(2L, demands.get(0).getId().longValue());
        assertEquals(3L, demands.get(1).getId().longValue());
    }

    @Test
    @DbUnitDataSet(before = "DbDemandServiceTest.testGetDemandsByOrderId_isOk.before.csv")
    public void testGetDemandsByOrderIdLength3_notFiltered() {
        DemandFilter filter = new DemandFilter();
        filter.setDemandType(DemandType.TYPE_1P);
        filter.setOrderIdFilter("Зп-");
        List<DemandDTO> demands = demandService.getDemands(filter);
        assertNotNull(demands);
        assertEquals(4, demands.size());
    }

    @Test
    @DbUnitDataSet(
            before = "DbDemandServiceTest.saveDemandsForExportBadMonoXdoc.before.csv",
            after = "DbDemandServiceTest.saveDemandsForExportBadMonoXdoc.after.csv"
    )
    public void testSaveDemandsForExportBadMonoXdoc() {
        final UserWarningException exception = assertThrows(UserWarningException.class, () ->
            demandService.validateDemandsBeforeExport(DemandType.TYPE_1P,
                List.of(new DemandIdentityDTO(1L, 1), new DemandIdentityDTO(4L, 1))));

        assertEquals("Нужно указать все Mono Xdoc потребности из группы", exception.getMessage());

        assertDoesNotThrow(() ->
            demandService.validateDemandsBeforeExport(DemandType.TYPE_1P,
                List.of(new DemandIdentityDTO(1L, 1), new DemandIdentityDTO(4L, 1), new DemandIdentityDTO(7L, 1))));
    }

    @Test
    @DbUnitDataSet(before = "DbDemandServiceTest.saveDemandsForExportDeletedMonoXdoc.before.csv")
    public void testSaveDemandsForExportDeletedMonoXdoc() {
        assertDoesNotThrow(() ->
            demandService.saveDemandsForExport(DemandType.TYPE_1P,
                List.of(new DemandIdentityDTO(1L, 1), new DemandIdentityDTO(4L, 1)),
                LOGIN));
    }

    @Test
    @DbUnitDataSet(
        before = "DbReplenishmentServiceTest.multiGroups.monoxdoc.before.csv",
        after = "DbReplenishmentServiceTest.multiGroups.monoxdoc.after.csv")
    public void testSaveCorrectedDemandForExport_MultiGroupsMono() {
        List<Long> demandIds = demandService.saveDemandsForExport(
            DemandType.TYPE_1P,
            Arrays.asList(
                new DemandIdentityDTO(101L, 1),
                new DemandIdentityDTO(102L, 1),
                new DemandIdentityDTO(103L, 1)
            ),
            LOGIN
        ).stream().map(DemandDTO::getId).collect(Collectors.toList());

        List<DemandDTO> demands;
        try (final SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.SIMPLE)) {
            final DemandDTORepository mapper = sqlSession.getMapper(DemandDTORepository.class);
            demands = StreamUtils.streamFromIterable(mapper.getDemandsByIds(DemandType.TYPE_1P, demandIds))
                .sorted(Comparator.comparing(DemandDTO::getItems))
                .collect(Collectors.toList());
        }
    }

    @Test
    @DbUnitDataSet(
        before = "DbReplenishmentServiceTest.consolidatedSupply_weight_direct.before.csv",
        after = "DbReplenishmentServiceTest.consolidatedSupply_weight_direct.after.csv")
    public void testConsolidatedSupplyForExportedDemands_direct_weight() {
        demandService.saveDemandsForExport(
            DemandType.TYPE_1P,
            Arrays.asList(
                new DemandIdentityDTO(103L, 1),
                new DemandIdentityDTO(104L, 1)
            ),
            LOGIN
        );
    }

    @Test
    @DbUnitDataSet(
        before = "DbReplenishmentServiceTest.consolidatedSupply_weight_direct_withoutParams.before.csv",
        after = "DbReplenishmentServiceTest.consolidatedSupply_weight_direct_withoutParams.after.csv")
    public void testConsolidatedSupplyForExportedDemands_direct_weight_withoutParams() {
        demandService.saveDemandsForExport(
            DemandType.TYPE_1P,
            Arrays.asList(
                new DemandIdentityDTO(103L, 1),
                new DemandIdentityDTO(104L, 1)
            ),
            LOGIN
        );
    }

    @Test
    @DbUnitDataSet(
        before = "DbReplenishmentServiceTest.consolidatedSupply_volume_xdock.before.csv",
        after = "DbReplenishmentServiceTest.consolidatedSupply_volume_xdock.after.csv")
    public void testConsolidatedSupplyForExportedDemands_xdock_volume() {
        demandService.saveDemandsForExport(
            DemandType.TYPE_1P,
            Arrays.asList(
                new DemandIdentityDTO(103L, 1),
                new DemandIdentityDTO(104L, 1)
            ),
            LOGIN
        );
    }

    @Test
    @DbUnitDataSet(
        before = "DbReplenishmentServiceTest.consolidatedSupply_mono_xdock.before.csv",
        after = "DbReplenishmentServiceTest.consolidatedSupply_mono_xdock.after.csv")
    public void testConsolidatedSupplyForExportedDemands_mono_xdock() {
        demandService.saveDemandsForExport(
            DemandType.TYPE_1P,
            Arrays.asList(
                new DemandIdentityDTO(103L, 1),
                new DemandIdentityDTO(104L, 1)
            ),
            LOGIN
        );
    }

    @Test
    @DbUnitDataSet(
        before = "DbReplenishmentServiceTest.quotas.before.csv",
        after = "DbReplenishmentServiceTest.quotas.after.csv"
    )
    public void testQuotasByDepartments() {
        final UserWarningException exception = assertThrows(UserWarningException.class, () ->
            demandService.validateDemandsBeforeExport(
                DemandType.TYPE_1P,
                Arrays.asList(
                    new DemandIdentityDTO(103L, 1),
                    new DemandIdentityDTO(104L, 1)
                )
            ));

        assertEquals("Квота в 20 шт на склад Ростов на дату 2019-01-06," +
                " на департамент НЕ_ЭиБТ превышена на 5, доступно 5 ",
            exception.getMessage());
    }

    @Test
    @DbUnitDataSet(
        before = "DbDemandServiceTest_ZeroUnlimitedValueByDepartments.quotas.before.csv")
    public void testQuotasWithZeroUnlimitedValueByDepartments() {
        demandService.saveDemandsForExport(
            DemandType.TYPE_1P,
            Arrays.asList(
                new DemandIdentityDTO(103L, 1),
                new DemandIdentityDTO(104L, 1)
            ),
            LOGIN);
    }
}
