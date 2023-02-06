package ru.yandex.market.fulfillment.stockstorage;

import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.fulfillment.stockstorage.repository.WarehousePropertyRepository;
import ru.yandex.market.fulfillment.stockstorage.util.WarehouseProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WarehousePropertyRepositoryTest extends AbstractContextualTest {
    private static final int WAREHOUSE_ID = 1;
    private static final String PROPERTY = "property";
    private static final String NONEXISTENT_PROPERTY = "nonexistent property";

    @Autowired
    WarehousePropertyRepository warehousePropertyRepository;

    @Test
    @DatabaseSetup("classpath:database/states/warehouse_property/1.xml")
    public void getWarehouseProperty() {
        Optional<WarehouseProperty> warehouseProperty =
                warehousePropertyRepository.findOptionalByWarehouseIdAndProperty(WAREHOUSE_ID, PROPERTY);

        assertTrue(warehouseProperty.isPresent());
        assertEquals("property value", warehouseProperty.get().getValue());
    }

    @Test
    @DatabaseSetup("classpath:database/states/warehouse_property/1.xml")
    public void warehousePropertyNotFound() {
        Optional<WarehouseProperty> warehouseProperty =
                warehousePropertyRepository.findOptionalByWarehouseIdAndProperty(WAREHOUSE_ID, NONEXISTENT_PROPERTY);

        assertFalse(warehouseProperty.isPresent());
    }
}
