package ru.yandex.market.replenishment.autoorder.service.recommendations_grouping;

import java.time.LocalDate;
import java.util.Objects;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.model.DemandType;
import ru.yandex.market.replenishment.autoorder.model.SupplyRouteType;
import ru.yandex.market.replenishment.autoorder.model.WarehouseType;
import ru.yandex.market.replenishment.autoorder.model.dto.DemandDTO;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.Supplier;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.Warehouse;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.WarehouseRegion;
import ru.yandex.market.replenishment.autoorder.service.DbDemandService;
import ru.yandex.market.replenishment.autoorder.service.WarehouseService;
import ru.yandex.market.replenishment.autoorder.service.not_grouped_recommendations_processing_service.DemandMonoXdocHandler;
import ru.yandex.market.replenishment.autoorder.utils.IdGenerator;

import static ru.yandex.market.replenishment.autoorder.model.SupplyRouteType.DIRECT;
import static ru.yandex.market.replenishment.autoorder.model.SupplyRouteType.MONO_XDOC;


public class DemandMonoXdocHandlerTest extends FunctionalTest {

    @Autowired
    private WarehouseService warehouseService;

    @Autowired
    private DbDemandService dbDemandService;

    private DemandMonoXdocHandler demandMonoXdocHandler;

    private static void setMskWarehouse(DemandDTO demand) {
        if (demand == null) {
            throw new IllegalArgumentException("Demand is null");
        }
        WarehouseRegion region = new WarehouseRegion(WarehouseRegion.Companion.getMoscowId(), "Moscow",
            WarehouseRegion.Companion.getMoscowId());
        Warehouse warehouse = new Warehouse(145L, "Москва 145", WarehouseType.FULFILLMENT, region,
            10000927726L);
        demand.setWarehouse(warehouse);
    }

    private static void setWarehouse(DemandDTO demand, long warehouseId) {
        if (demand == null) {
            throw new IllegalArgumentException("Demand is null");
        }
        WarehouseRegion region = new WarehouseRegion(WarehouseRegion.Companion.getRostovId(), "Rostov",
            WarehouseRegion.Companion.getMoscowId());
        Warehouse warehouse = new Warehouse(warehouseId, String.valueOf(warehouseId), WarehouseType.FULFILLMENT, region,
            10000985804L);
        demand.setWarehouse(warehouse);
    }

    private static DemandDTO createDemand(DemandType type,
                                          long id,
                                          long supplierId,
                                          SupplyRouteType supplyRouteType,
                                          LocalDate orderDate,
                                          String catman) {
        final Supplier supplier = new Supplier();
        supplier.setId(supplierId);
        supplier.setName("Supplier #" + supplierId);
        supplier.setRsId(Objects.toString(supplierId));

        final DemandDTO demand = new DemandDTO();
        demand.setDemandType(type);
        demand.setId(id);
        demand.setSupplier(supplier);
        demand.setOrderDate(orderDate);
        demand.setCatman(catman);
        demand.setSupplyRoute(supplyRouteType.getInternalName());
        return demand;
    }

    @Before
    public void setUp() {
        demandMonoXdocHandler = new DemandMonoXdocHandler(warehouseService);
    }

    @DbUnitDataSet(before = "DemandMonoXdocHandlerTest_testGrouping.before.csv")
    @Test
    public void testGroupingByDeliveryType() {
        final IdGenerator idGenerator = dbDemandService.newIdGenerator();
        demandMonoXdocHandler.init(false, idGenerator);
        final DemandDTO demand1 =
            createDemand(DemandType.TYPE_1P, idGenerator.generateId(), 537190L, MONO_XDOC, LocalDate.now(),
                "onishekat");
        setWarehouse(demand1, 147);
        final DemandDTO demand2 =
            createDemand(DemandType.TYPE_1P, idGenerator.generateId(), 537190L, MONO_XDOC, LocalDate.now(),
                "onishekat");
        setWarehouse(demand2, 300);

        final DemandDTO demand3 =
            createDemand(DemandType.TYPE_1P, idGenerator.generateId(), 5371901L, MONO_XDOC, LocalDate.now(),
                "onishekat");
        setWarehouse(demand3, 300);

        final DemandDTO demand4 =
            createDemand(DemandType.TYPE_1P, idGenerator.generateId(), 537190L, MONO_XDOC, LocalDate.now(),
                "onishekat");
        setMskWarehouse(demand4);

        final DemandDTO demand5 =
            createDemand(DemandType.TYPE_1P, idGenerator.generateId(), 543060L, MONO_XDOC, LocalDate.now(),
                "alexreshetilo");
        setWarehouse(demand5, 147);
        final DemandDTO demand6 =
            createDemand(DemandType.TYPE_1P, idGenerator.generateId(), 543060L, MONO_XDOC, LocalDate.now(),
                "alexreshetilo");
        setWarehouse(demand6, 300);

        final DemandDTO demand7 =
            createDemand(DemandType.TYPE_1P, idGenerator.generateId(), 543060L, DIRECT, LocalDate.now(),
                "alexreshetilo");
        setWarehouse(demand7, 147);
        final DemandDTO demand8 =
            createDemand(DemandType.TYPE_1P, idGenerator.generateId(), 543060L, DIRECT, LocalDate.now(),
                "alexreshetilo");
        setWarehouse(demand8, 300);

        demandMonoXdocHandler.storeDemand(demand1);
        demandMonoXdocHandler.storeDemand(demand2);
        demandMonoXdocHandler.storeDemand(demand3);
        demandMonoXdocHandler.storeDemand(demand4);
        demandMonoXdocHandler.storeDemand(demand5);
        demandMonoXdocHandler.storeDemand(demand6);
        demandMonoXdocHandler.storeDemand(demand6);


        final long resultGroup = demandMonoXdocHandler.setXdocParentDemandIdAndClear(1);
        Assert.assertEquals(3, resultGroup);

        Assert.assertNotNull(demand1.getXdocParentDemandId());
        Assert.assertEquals(demand1.getXdocParentDemandId(), demand2.getXdocParentDemandId());
        Assert.assertNotNull(demand1.getLinkGroup());
        Assert.assertEquals(demand1.getLinkGroup(), demand2.getLinkGroup());

        Assert.assertNotNull(demand3.getXdocParentDemandId());
        Assert.assertNull(demand3.getLinkGroup());

        Assert.assertNull(demand4.getXdocParentDemandId());
        Assert.assertNull(demand4.getLinkGroup());

        Assert.assertNotNull(demand5.getXdocParentDemandId());
        Assert.assertEquals(demand5.getXdocParentDemandId(), demand6.getXdocParentDemandId());
        Assert.assertNotNull(demand5.getLinkGroup());
        Assert.assertEquals(demand5.getLinkGroup(), demand6.getLinkGroup());

        Assert.assertNull(demand7.getXdocParentDemandId());
        Assert.assertNull(demand7.getLinkGroup());

        Assert.assertNull(demand8.getXdocParentDemandId());
        Assert.assertNull(demand8.getLinkGroup());
    }

    @DbUnitDataSet(before = "DemandMonoXdocHandlerTest_testGrouping.before.csv")
    @Test
    public void testGroupingMonoXdocDisabledByDeliveryType() {
        final IdGenerator idGenerator = dbDemandService.newIdGenerator();
        demandMonoXdocHandler.init(true, idGenerator);
        final DemandDTO demand1 =
            createDemand(DemandType.TYPE_1P, idGenerator.generateId(), 537190L, MONO_XDOC, LocalDate.now(),
                "onishekat");
        setWarehouse(demand1, 147);
        final DemandDTO demand2 =
            createDemand(DemandType.TYPE_1P, idGenerator.generateId(), 537190L, MONO_XDOC, LocalDate.now(),
                "onishekat");
        setWarehouse(demand2, 300);

        final DemandDTO demand3 =
            createDemand(DemandType.TYPE_1P, idGenerator.generateId(), 5371901L, MONO_XDOC, LocalDate.now(),
                "onishekat");
        setWarehouse(demand3, 300);

        final DemandDTO demand4 =
            createDemand(DemandType.TYPE_1P, idGenerator.generateId(), 537190L, MONO_XDOC, LocalDate.now(),
                "onishekat");
        setMskWarehouse(demand4);

        final DemandDTO demand5 =
            createDemand(DemandType.TYPE_1P, idGenerator.generateId(), 543060L, MONO_XDOC, LocalDate.now(),
                "alexreshetilo");
        setWarehouse(demand5, 147);
        final DemandDTO demand6 =
            createDemand(DemandType.TYPE_1P, idGenerator.generateId(), 543060L, MONO_XDOC, LocalDate.now(),
                "alexreshetilo");
        setWarehouse(demand6, 300);

        final DemandDTO demand7 =
            createDemand(DemandType.TYPE_1P, idGenerator.generateId(), 543060L, DIRECT, LocalDate.now(),
                "alexreshetilo");
        setWarehouse(demand7, 147);
        final DemandDTO demand8 =
            createDemand(DemandType.TYPE_1P, idGenerator.generateId(), 543060L, DIRECT, LocalDate.now(),
                "alexreshetilo");
        setWarehouse(demand8, 300);

        demandMonoXdocHandler.storeDemand(demand1);
        demandMonoXdocHandler.storeDemand(demand2);
        demandMonoXdocHandler.storeDemand(demand3);
        demandMonoXdocHandler.storeDemand(demand4);
        demandMonoXdocHandler.storeDemand(demand5);
        demandMonoXdocHandler.storeDemand(demand6);
        demandMonoXdocHandler.storeDemand(demand6);


        final long resultGroup = demandMonoXdocHandler.setXdocParentDemandIdAndClear(1);
        Assert.assertEquals(1, resultGroup);

        Assert.assertNull(demand1.getXdocParentDemandId());
        Assert.assertNull(demand2.getXdocParentDemandId());
        Assert.assertNull(demand3.getXdocParentDemandId());
        Assert.assertNull(demand4.getXdocParentDemandId());
        Assert.assertNull(demand5.getXdocParentDemandId());
        Assert.assertNull(demand6.getXdocParentDemandId());
        Assert.assertNull(demand7.getXdocParentDemandId());
        Assert.assertNull(demand8.getXdocParentDemandId());
    }

}
