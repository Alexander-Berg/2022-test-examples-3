package ru.yandex.market.ff.service.registry.validation.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.ff.base.SoftAssertionSupport;
import ru.yandex.market.ff.client.enums.RegistryUnitIdType;
import ru.yandex.market.ff.client.enums.RegistryUnitType;
import ru.yandex.market.ff.enrichment.RequestValidationErrorType;
import ru.yandex.market.ff.model.dto.registry.RegistryUnitId;
import ru.yandex.market.ff.model.entity.registry.RegistryUnitEntity;
import ru.yandex.market.ff.service.registry.validation.ValidationResult;

class RegistryItemIdValidatorTest extends SoftAssertionSupport {

    private RegistryItemIdValidator registryItemIdValidator;

    @BeforeEach
    void setUp() {
        registryItemIdValidator = new RegistryItemIdValidator();
    }

    @Test
    void skipValidationForBox() {
        RegistryUnitEntity box = RegistryUnitEntity.builder().type(RegistryUnitType.BOX).build();
        ValidationResult actual = registryItemIdValidator.validate(box, null);

        assertions.assertThat(actual.getErrors()).isEqualTo(ValidationResult.valid().getErrors());
    }

    @Test
    void skipValidationForPallet() {
        RegistryUnitEntity pallet = RegistryUnitEntity.builder().type(RegistryUnitType.PALLET).build();
        ValidationResult actual = registryItemIdValidator.validate(pallet, null);

        assertions.assertThat(actual.getErrors()).isEqualTo(ValidationResult.valid().getErrors());
    }

    @Test
    void onNoId() {
        RegistryUnitEntity item = RegistryUnitEntity.builder()
                .type(RegistryUnitType.ITEM)
                .identifiers(RegistryUnitId.of())
                .build();
        ValidationResult actual = registryItemIdValidator.validate(item, null);
        ValidationResult expected = ValidationResult.of(
                RequestValidationErrorType.REGISTRY_UNIT_INVALID_ID,
                "Unit of type ITEM with ID parts " +
                        "[] has unsupported or misses required idType"
        );

        assertions.assertThat(actual.getErrors()).isEqualTo(expected.getErrors());
    }

    @Test
    void onSkuIdAndVendorId() {
        RegistryUnitEntity item = RegistryUnitEntity.builder()
                .type(RegistryUnitType.ITEM)
                .identifiers(RegistryUnitId.of(RegistryUnitIdType.SHOP_SKU, "sku123",
                        RegistryUnitIdType.VENDOR_ID, "456123"))
                .build();

        ValidationResult actual = registryItemIdValidator.validate(item, null);

        assertions.assertThat(actual.getErrors()).isEqualTo(ValidationResult.valid().getErrors());
    }

     @Test
    void onSkuIdOnly() {
        RegistryUnitEntity item = RegistryUnitEntity.builder()
                .type(RegistryUnitType.ITEM)
                .identifiers(RegistryUnitId.of(RegistryUnitIdType.SHOP_SKU, "sku123"))
                .build();

        ValidationResult actual = registryItemIdValidator.validate(item, null);
        ValidationResult expected = ValidationResult.of(
                 RequestValidationErrorType.REGISTRY_UNIT_INVALID_ID,
                 "Unit of type ITEM with ID parts " +
                         "[UnitPartialId(value=sku123, type=SHOP_SKU)] has unsupported or misses required idType"
         );

         assertions.assertThat(actual.getErrors()).isEqualTo(expected.getErrors());
    }

    @Test
    void onVirtualIdAndVendorId() {
        RegistryUnitEntity item = RegistryUnitEntity.builder()
                .type(RegistryUnitType.ITEM)
                .identifiers(RegistryUnitId.of(RegistryUnitIdType.VIRTUAL_ID, "abc123",
                        RegistryUnitIdType.VENDOR_ID, "456123"))
                .build();

        ValidationResult actual = registryItemIdValidator.validate(item, null);

        assertions.assertThat(actual.getErrors()).isEqualTo(ValidationResult.valid().getErrors());
    }

    @Test
    void onVirtualIdOnly() {
        RegistryUnitEntity item = RegistryUnitEntity.builder()
                .type(RegistryUnitType.ITEM)
                .identifiers(RegistryUnitId.of(RegistryUnitIdType.VIRTUAL_ID, "abc123"))
                .build();

        ValidationResult actual = registryItemIdValidator.validate(item, null);

         assertions.assertThat(actual.getErrors()).isEqualTo(ValidationResult.valid().getErrors());
    }

    @Test
    void onPalletId() {
        RegistryUnitEntity item = RegistryUnitEntity.builder()
                .type(RegistryUnitType.ITEM)
                .identifiers(RegistryUnitId.of(RegistryUnitIdType.PALLET_ID, "pallet_id"))
                .build();

        ValidationResult actual = registryItemIdValidator.validate(item, null);
        ValidationResult expected = ValidationResult.of(
                RequestValidationErrorType.REGISTRY_UNIT_INVALID_ID,
                "Unit of type ITEM with ID parts " +
                        "[UnitPartialId(value=pallet_id, type=PALLET_ID)] has unsupported or misses required idType"
        );


        assertions.assertThat(actual.getErrors()).isEqualTo(expected.getErrors());
    }
}
