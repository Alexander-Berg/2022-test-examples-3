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

class RegistryPalletIdValidatorTest extends SoftAssertionSupport {

    private RegistryPalletIdValidator registryPalletIdValidator;

    @BeforeEach
    void setUp() {
        registryPalletIdValidator = new RegistryPalletIdValidator();
    }

    @Test
    void skipValidationForBox() {
        RegistryUnitEntity box = RegistryUnitEntity.builder().type(RegistryUnitType.BOX).build();
        ValidationResult actual = registryPalletIdValidator.validate(box, null);

        assertions.assertThat(actual.getErrors()).isEqualTo(ValidationResult.valid().getErrors());
    }

    @Test
    void skipValidationForItem() {
        RegistryUnitEntity item = RegistryUnitEntity.builder().type(RegistryUnitType.ITEM).build();
        ValidationResult actual = registryPalletIdValidator.validate(item, null);

        assertions.assertThat(actual.getErrors()).isEqualTo(ValidationResult.valid().getErrors());
    }

    @Test
    void onNoId() {
        RegistryUnitEntity pallet = RegistryUnitEntity.builder()
                .type(RegistryUnitType.PALLET)
                .identifiers(RegistryUnitId.of())
                .build();
        ValidationResult actual = registryPalletIdValidator.validate(pallet, null);
        ValidationResult expected = ValidationResult.of(
                RequestValidationErrorType.REGISTRY_UNIT_INVALID_ID,
                "Unit of type PALLET with ID parts " +
                        "[] has unsupported or misses required idType"
        );

        assertions.assertThat(actual.getErrors()).isEqualTo(expected.getErrors());
    }

    @Test
    void onPalletId() {
        RegistryUnitEntity pallet = RegistryUnitEntity.builder()
                .type(RegistryUnitType.PALLET)
                .identifiers(RegistryUnitId.of(RegistryUnitIdType.PALLET_ID, "pallet_id"))
                .build();

        ValidationResult actual = registryPalletIdValidator.validate(pallet, null);

        assertions.assertThat(actual.getErrors()).isEqualTo(ValidationResult.valid().getErrors());
    }

    @Test
    void onVendorId() {
        RegistryUnitEntity pallet = RegistryUnitEntity.builder()
                .type(RegistryUnitType.PALLET)
                .identifiers(RegistryUnitId.of(RegistryUnitIdType.VENDOR_ID, "456123"))
                .build();

        ValidationResult actual = registryPalletIdValidator.validate(pallet, null);
        ValidationResult expected = ValidationResult.of(
                RequestValidationErrorType.REGISTRY_UNIT_INVALID_ID,
                "Unit of type PALLET with ID parts " +
                        "[UnitPartialId(value=456123, type=VENDOR_ID)] has unsupported or misses required idType"
        );

        assertions.assertThat(actual.getErrors()).isEqualTo(expected.getErrors());
    }
}
