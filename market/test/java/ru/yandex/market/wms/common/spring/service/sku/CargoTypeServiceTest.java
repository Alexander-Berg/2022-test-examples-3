package ru.yandex.market.wms.common.spring.service.sku;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestConstructor;

import ru.yandex.market.logistic.api.model.fulfillment.CargoType;
import ru.yandex.market.logistic.api.model.fulfillment.CargoTypes;
import ru.yandex.market.logistic.api.model.fulfillment.Item;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.SkuCargotype;

@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
public class CargoTypeServiceTest extends IntegrationTest {

    private final CargoTypeService cargoTypeService;

    public CargoTypeServiceTest(CargoTypeService cargoTypeService) {
        this.cargoTypeService = cargoTypeService;
    }

    @Test
    public void extractCargoTypesWhenCargoTypesProvided() {
        List<CargoType> cargoTypes = Arrays.asList(CargoType.MEDICAL_SUPPLIES, CargoType.CIS_REQUIRED);
        Item item = new Item
                .ItemBuilder("Test Item", 1, BigDecimal.TEN)
                .setUnitId(new UnitId("12345", 603674L, "IPC647BOOKCCRYWHI"))
                .setCargoTypes(new CargoTypes(cargoTypes))
                .build();
        String sku = "ROV0000000001002240153";

        List<SkuCargotype> result = cargoTypeService.extractCargoTypes(item, sku, "");

        assertions.assertThat(result.size()).isEqualTo(cargoTypes.size());
        assertions.assertThat(result.stream()
                .filter(skuCargoType -> skuCargoType.getCargotype().equals(CargoType.MEDICAL_SUPPLIES.getCode()))
                .count() == 1).isTrue();
        assertions.assertThat(result.stream()
                .filter(skuCargoType -> skuCargoType.getCargotype().equals(CargoType.CIS_REQUIRED.getCode()))
                .count() == 1).isTrue();
    }

    @Test
    public void extractCargoTypesWhenOnlyOldCargoTypeProvided() {
        Item item = new Item
                .ItemBuilder("Test Item", 1, BigDecimal.TEN)
                .setUnitId(new UnitId("12345", 603674L, "IPC647BOOKCCRYWHI"))
                .setCargoType(CargoType.MEDICAL_SUPPLIES)
                .build();
        String sku = "ROV0000000001002240153";

        List<SkuCargotype> result = cargoTypeService.extractCargoTypes(item, sku, "");

        assertions.assertThat(result.size()).isEqualTo(1);
        assertions.assertThat(result.get(0).getCargotype()).isEqualTo(CargoType.MEDICAL_SUPPLIES.getCode());
    }

    @Test
    public void extractCargoTypesWhenNoCargoTypesProvided() {
        Item item = new Item
                .ItemBuilder("Test Item", 1, BigDecimal.TEN)
                .setUnitId(new UnitId("12345", 603674L, "IPC647BOOKCCRYWHI"))
                .build();
        String sku = "ROV0000000001002240153";

        List<SkuCargotype> result = cargoTypeService.extractCargoTypes(item, sku, "");

        assertions.assertThat(result.isEmpty()).isTrue();
    }

    @Test
    public void extractCargoTypesWhenNoCargoTypesProvidedAndCargoTypesListIsNull() {
        Item item = new Item
                .ItemBuilder("Test Item", 1, BigDecimal.TEN)
                .setUnitId(new UnitId("12345", 603674L, "IPC647BOOKCCRYWHI"))
                .setCargoTypes(new CargoTypes(null))
                .build();
        String sku = "ROV0000000001002240153";

        List<SkuCargotype> result = cargoTypeService.extractCargoTypes(item, sku, "");

        assertions.assertThat(result.isEmpty()).isTrue();
    }
}
