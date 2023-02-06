package ru.yandex.market.wms.picking.modules.service;

import java.math.BigDecimal;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.picking.modules.model.PutawayzoneSettings;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PutawayzoneServiceTest extends IntegrationTest {
    @Autowired
    private PutawayzoneService putawayzoneService;

    @Test
    public void getSettingsByAssignmentEmptyDataEmptyConfigs() {
        PutawayzoneSettings settings = putawayzoneService.getSettingsByAssignment("999");
        assertEquals("DEFAULT", settings.getPutawayzone());
        assertEquals(BigDecimal.valueOf(30), settings.getMaxWeightPerPickingOrder());
        assertEquals(BigDecimal.valueOf(1000000), settings.getMaxVolumePerPickingOrder());
    }

    @Test
    @DatabaseSetup("/service/zone-settings/1/before.xml")
    public void getSettingsByAssignmentEmptyDataWithConfigs() {
        PutawayzoneSettings settings = putawayzoneService.getSettingsByAssignment("999");
        assertEquals("DEFAULT", settings.getPutawayzone());
        assertEquals(BigDecimal.valueOf(99), settings.getMaxWeightPerPickingOrder());
        assertEquals(BigDecimal.valueOf(999), settings.getMaxVolumePerPickingOrder());
    }

    @Test
    @DatabaseSetup("/service/zone-settings/2/before.xml")
    public void getSettingsByAssignmentExistDataWithConfigs() {
        PutawayzoneSettings settings = putawayzoneService.getSettingsByAssignment("999");
        assertEquals("TEST_ZONE", settings.getPutawayzone());
        assertEquals(0, BigDecimal.valueOf(11).compareTo(settings.getMaxWeightPerPickingOrder()));
        assertEquals(0, BigDecimal.valueOf(111).compareTo(settings.getMaxVolumePerPickingOrder()));
    }

    @Test
    public void getSettingsByLocEmptyData() {
        PutawayzoneSettings settings = putawayzoneService.getSettingsByLoc("TEST_LOC");
        assertEquals("DEFAULT", settings.getPutawayzone());
        assertEquals(BigDecimal.valueOf(30), settings.getMaxWeightPerPickingOrder());
        assertEquals(BigDecimal.valueOf(1000000), settings.getMaxVolumePerPickingOrder());
    }

    @Test
    @DatabaseSetup("/service/zone-settings/1/before.xml")
    public void getSettingsByLocEmptyDataWithConfigs() {
        PutawayzoneSettings settings = putawayzoneService.getSettingsByLoc("TEST_LOC");
        assertEquals("DEFAULT", settings.getPutawayzone());
        assertEquals(BigDecimal.valueOf(99), settings.getMaxWeightPerPickingOrder());
        assertEquals(BigDecimal.valueOf(999), settings.getMaxVolumePerPickingOrder());
    }

    @Test
    @DatabaseSetup("/service/zone-settings/2/before.xml")
    public void getSettingsByLocExistDataWithConfigs() {
        PutawayzoneSettings settings = putawayzoneService.getSettingsByLoc("TEST_LOC");
        assertEquals("TEST_ZONE", settings.getPutawayzone());
        assertEquals(0, BigDecimal.valueOf(11).compareTo(settings.getMaxWeightPerPickingOrder()));
        assertEquals(0, BigDecimal.valueOf(111).compareTo(settings.getMaxVolumePerPickingOrder()));
    }
}
