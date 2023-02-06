package ru.yandex.market.wms.radiator.core.config.properties;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WarehouseMapPropertiesTest {

    private final Map<String, WarehouseProperties> enabledProperties = Map.of(
            "wh1", new WarehouseProperties("token1", "wh1", null, null, null),
            "wh3", new WarehouseProperties("token3", "wh3", null, null, null)
    );
    private final Map<String, WarehouseProperties> disabledProperties = Map.of(
            "wh2", new WarehouseProperties("token2", "wh2", null, null, null)
    );


    @Test
    public void checkExceptionForIllegalToken() {
        WarehouseMapProperties warehouseProperties = new WarehouseMapProperties(enabledProperties, disabledProperties);

        Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> warehouseProperties.getByToken("wrongToken")
        );

        assertEquals("No warehouse properties found for token : wrongToken", exception.getMessage());
    }

    @Test
    public void checkExceptionForDisabledToken() {
        WarehouseMapProperties warehouseProperties = new WarehouseMapProperties(enabledProperties, disabledProperties);

        Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> warehouseProperties.getByToken("token2")
        );

        assertEquals("Warehouse properties was disabled for token : token2", exception.getMessage());
    }

    @Test
    public void checkExceptionForNullToken() {
        WarehouseMapProperties warehouseProperties = new WarehouseMapProperties(enabledProperties, disabledProperties);

        Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> warehouseProperties.getByToken(null)
        );

        assertEquals("No token specified", exception.getMessage());
    }

    @Test
    public void checkResultForValidToken() {
        WarehouseMapProperties warehouseProperties = new WarehouseMapProperties(enabledProperties, disabledProperties);

        WarehouseProperties actualProperties = warehouseProperties.getByToken("token3");

        assertEquals(new WarehouseProperties("token3", "wh3", null, null, null), actualProperties);
    }


    @Test
    public void checkExceptionForIllegalWarehouseId() {
        WarehouseMapProperties warehouseProperties = new WarehouseMapProperties(enabledProperties, disabledProperties);

        Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> warehouseProperties.getByWarehouseId("wrongWarehouseId")
        );

        assertEquals("No warehouse properties found for warehouseId : wrongWarehouseId", exception.getMessage());
    }

    @Test
    public void checkExceptionForDisabledWarehouseId() {
        WarehouseMapProperties warehouseProperties = new WarehouseMapProperties(enabledProperties, disabledProperties);

        Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> warehouseProperties.getByWarehouseId("wh2")
        );

        assertEquals("Warehouse properties was disabled for warehouseId : wh2", exception.getMessage());
    }

    @Test
    public void checkExceptionForNullWarehouseId() {
        WarehouseMapProperties warehouseProperties = new WarehouseMapProperties(enabledProperties, disabledProperties);

        Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> warehouseProperties.getByWarehouseId(null)
        );

        assertEquals("No warehouseId specified", exception.getMessage());
    }

    @Test
    public void checkResultForValidWarehouseId() {
        WarehouseMapProperties warehouseProperties = new WarehouseMapProperties(enabledProperties, disabledProperties);

        WarehouseProperties actualProperties = warehouseProperties.getByWarehouseId("wh3");

        assertEquals(new WarehouseProperties("token3", "wh3", null, null, null), actualProperties);
    }

}
