package ru.yandex.market.replenishment.autoorder.service;

import java.time.LocalDate;
import java.util.Objects;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.exception.UserWarningException;
import ru.yandex.market.replenishment.autoorder.model.DemandType;
import ru.yandex.market.replenishment.autoorder.model.SupplyRouteType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DemandUpdateDeliveryTypeServiceTest extends FunctionalTest {

    @Autowired
    private DemandUpdateDeliveryTypeService demandUpdateDeliveryTypeService;

    @Test
    @DbUnitDataSet(before = "DemandUpdateServiceTest.UpdateDeliveryType.before.csv",
        after = "DemandUpdateServiceTest.UpdateDeliveryType_DirectToMonoXdoc.after.csv")
    public void updateDeliveryType_DirectToMonoXdoc() {

        var changedDeliveryData=
            demandUpdateDeliveryTypeService.changeDeliveryType(DemandType.TYPE_1P, 1, SupplyRouteType.MONO_XDOC);

        assertEquals(LocalDate.of(2019, 3, 23), changedDeliveryData.getDeliveryDate());
        assertEquals(LocalDate.of(2019, 3, 14), changedDeliveryData.getXdocDate());
        assertEquals(444222, Objects.requireNonNull(changedDeliveryData.getMinPurchase()).intValue());
        assertEquals(172L, Objects.requireNonNull(changedDeliveryData.getWarehouseIdFrom()).longValue());
        assertEquals("mono_xdoc", changedDeliveryData.getSupplyRoute());
    }

    @Test
    @DbUnitDataSet(before = "DemandUpdateServiceTest.UpdateDeliveryType.before.csv",
        after = "DemandUpdateServiceTest.UpdateDeliveryType_DirectToXdoc.after.csv")
    public void updateDeliveryType_DirectToXdoc() {
        var changedDeliveryData=
            demandUpdateDeliveryTypeService.changeDeliveryType(DemandType.TYPE_1P, 1, SupplyRouteType.XDOC);

        assertEquals(LocalDate.of(2019, 3, 23), changedDeliveryData.getDeliveryDate());
        assertEquals(LocalDate.of(2019, 3, 14), changedDeliveryData.getXdocDate());
        assertEquals(555111, Objects.requireNonNull(changedDeliveryData.getMinPurchase()).intValue());
        assertEquals(402L, Objects.requireNonNull(changedDeliveryData.getWarehouseIdFrom()).longValue());
        assertEquals("xdoc", changedDeliveryData.getSupplyRoute());
    }

    @Test
    @DbUnitDataSet(before = "DemandUpdateServiceTest.UpdateDeliveryType.before.csv",
        after = "DemandUpdateServiceTest.UpdateDeliveryType_XdocToDirect.after.csv")
    public void updateDeliveryType_XdocToDirect() {
        var changedDeliveryData=
            demandUpdateDeliveryTypeService.changeDeliveryType(DemandType.TYPE_1P, 2, SupplyRouteType.DIRECT);

        assertEquals(LocalDate.of(2019, 3, 15), changedDeliveryData.getDeliveryDate());
        assertNull(changedDeliveryData.getXdocDate());
        assertEquals(100600, Objects.requireNonNull(changedDeliveryData.getMinPurchase()).intValue());
        assertNull(changedDeliveryData.getWarehouseIdFrom());
        assertEquals("direct", changedDeliveryData.getSupplyRoute());
    }

    @Test
    @DbUnitDataSet(before = "DemandUpdateServiceTest.UpdateDeliveryType.before.csv",
        after = "DemandUpdateServiceTest.UpdateDeliveryType_XdocToMonoXdoc.after.csv")
    public void updateDeliveryType_XdocToMonoXdoc() {
        var changedDeliveryData=
            demandUpdateDeliveryTypeService.changeDeliveryType(DemandType.TYPE_1P, 2, SupplyRouteType.MONO_XDOC);

        assertEquals(LocalDate.of(2019, 3, 23), changedDeliveryData.getDeliveryDate());
        assertEquals(LocalDate.of(2019, 3, 14), changedDeliveryData.getXdocDate());
        assertEquals(444222, Objects.requireNonNull(changedDeliveryData.getMinPurchase()).intValue());
        assertEquals(172L, Objects.requireNonNull(changedDeliveryData.getWarehouseIdFrom()).longValue());
        assertEquals("mono_xdoc", changedDeliveryData.getSupplyRoute());
    }


    @Test
    @DbUnitDataSet(before = "DemandUpdateServiceTest.UpdateDeliveryType.before.csv",
        after = "DemandUpdateServiceTest.UpdateDeliveryType_MonoXdocToDirect.after.csv")
    public void updateDeliveryType_MonoXdocToDirect() {
        var changedDeliveryData=
            demandUpdateDeliveryTypeService.changeDeliveryType(DemandType.TYPE_1P, 3, SupplyRouteType.DIRECT);

        assertEquals(LocalDate.of(2019, 3, 15), changedDeliveryData.getDeliveryDate());
        assertNull(changedDeliveryData.getXdocDate());
        assertEquals(100600, Objects.requireNonNull(changedDeliveryData.getMinPurchase()).intValue());
        assertNull(changedDeliveryData.getWarehouseIdFrom());
        assertEquals("direct", changedDeliveryData.getSupplyRoute());
    }

    @Test
    @DbUnitDataSet(before = "DemandUpdateServiceTest.UpdateDeliveryType.before.csv",
        after = "DemandUpdateServiceTest.UpdateDeliveryType_MonoXdocToXdoc.after.csv")
    public void updateDeliveryType_MonoXdocToXdoc() {
        var changedDeliveryData=
            demandUpdateDeliveryTypeService.changeDeliveryType(DemandType.TYPE_1P, 3, SupplyRouteType.XDOC);

        assertEquals(LocalDate.of(2019, 3, 23), changedDeliveryData.getDeliveryDate());
        assertEquals(LocalDate.of(2019, 3, 14), changedDeliveryData.getXdocDate());
        assertEquals(555111, Objects.requireNonNull(changedDeliveryData.getMinPurchase()).intValue());
        assertEquals(402L, Objects.requireNonNull(changedDeliveryData.getWarehouseIdFrom()).longValue());
        assertEquals("xdoc", changedDeliveryData.getSupplyRoute());
    }

    @Test
    @DbUnitDataSet(before = "DemandUpdateServiceTest.UpdateDeliveryType_OneXdocToOneMonoXdoc.before.csv",
        after = "DemandUpdateServiceTest.UpdateDeliveryType_OneXdocToOneMonoXdoc.after.csv")
    public void updateDeliveryType_OneXdocToOneMonoXdoc() {
        var changedDeliveryData=
            demandUpdateDeliveryTypeService.changeDeliveryType(DemandType.TYPE_1P, 1, SupplyRouteType.MONO_XDOC);

        assertEquals(LocalDate.of(2019, 3, 23), changedDeliveryData.getDeliveryDate());
        assertEquals(LocalDate.of(2019, 3, 14), changedDeliveryData.getXdocDate());
        assertEquals(444222, Objects.requireNonNull(changedDeliveryData.getMinPurchase()).intValue());
        assertEquals(172L, Objects.requireNonNull(changedDeliveryData.getWarehouseIdFrom()).longValue());
        assertEquals("mono_xdoc", changedDeliveryData.getSupplyRoute());
    }

    @Test
    @DbUnitDataSet(before = "DemandUpdateServiceTest.UpdateDeliveryType_OneXdocToTwoMonoXdoc.before.csv",
        after = "DemandUpdateServiceTest.UpdateDeliveryType_OneXdocToTwoMonoXdoc.after.csv")
    public void updateDeliveryType_OneXdocToTwoMonoXdoc() {
        var changedDeliveryData=
            demandUpdateDeliveryTypeService.changeDeliveryType(DemandType.TYPE_1P, 1, SupplyRouteType.MONO_XDOC);

        assertEquals(LocalDate.of(2019, 3, 23), changedDeliveryData.getDeliveryDate());
        assertEquals(LocalDate.of(2019, 3, 14), changedDeliveryData.getXdocDate());
        assertEquals(444222, Objects.requireNonNull(changedDeliveryData.getMinPurchase()).intValue());
        assertEquals(172L, Objects.requireNonNull(changedDeliveryData.getWarehouseIdFrom()).longValue());
        assertEquals("mono_xdoc", changedDeliveryData.getSupplyRoute());
    }

    @Test
    @DbUnitDataSet(before = "DemandUpdateServiceTest.UpdateDeliveryType_HonestSign.before.csv")
    public void updateDeliveryType_HonestSign() {
        UserWarningException err = assertThrows(
                UserWarningException.class,
                () -> demandUpdateDeliveryTypeService
                    .changeDeliveryType(DemandType.TYPE_1P, 1, SupplyRouteType.MONO_XDOC)
        );
        Assertions.assertEquals(
            "Потребность 1 содержит ненулевую рекомендацияю с Честным знаком. Её тип нельзя поменять на Mono-Xdock",
            err.getMessage()
        );
    }
}
