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

class RegistryBoxIdValidatorTest extends SoftAssertionSupport {

    private RegistryBoxIdValidator registryBoxIdValidator;

    @BeforeEach
    void setUp() {
        registryBoxIdValidator = new RegistryBoxIdValidator();
    }

    @Test
    void skipValidationForItem() {
        RegistryUnitEntity item = RegistryUnitEntity.builder().type(RegistryUnitType.ITEM).build();
        ValidationResult actual = registryBoxIdValidator.validate(item, null);

        assertions.assertThat(actual.getErrors()).isEqualTo(ValidationResult.valid().getErrors());
    }

    @Test
    void skipValidationForPallet() {
        RegistryUnitEntity pallet = RegistryUnitEntity.builder().type(RegistryUnitType.PALLET).build();
        ValidationResult actual = registryBoxIdValidator.validate(pallet, null);

        assertions.assertThat(actual.getErrors()).isEqualTo(ValidationResult.valid().getErrors());
    }

    @Test
    void onNoId() {
        RegistryUnitEntity box = RegistryUnitEntity.builder()
                .type(RegistryUnitType.BOX)
                .identifiers(RegistryUnitId.of())
                .build();
        ValidationResult actual = registryBoxIdValidator.validate(box, null);
        ValidationResult expected = ValidationResult.of(
                RequestValidationErrorType.REGISTRY_UNIT_INVALID_ID,
                "Unit of type BOX with ID parts " +
                        "[] has unsupported or misses required idType"
        );

        assertions.assertThat(actual.getErrors()).isEqualTo(expected.getErrors());
    }

    @Test
    void onBoxIdOnly() {
        RegistryUnitEntity box = RegistryUnitEntity.builder()
                .type(RegistryUnitType.BOX)
                .identifiers(RegistryUnitId.of(RegistryUnitIdType.BOX_ID, "box"))
                .build();

        ValidationResult actual = registryBoxIdValidator.validate(box, null);

        assertions.assertThat(actual.getErrors()).isEqualTo(ValidationResult.valid().getErrors());
    }

    @Test
    void onBoxIdAndOrderId() {
        RegistryUnitEntity box = RegistryUnitEntity.builder()
                .type(RegistryUnitType.BOX)
                .identifiers(RegistryUnitId.of(RegistryUnitIdType.BOX_ID, "box", RegistryUnitIdType.ORDER_ID, "order"))
                .build();

        ValidationResult actual = registryBoxIdValidator.validate(box, null);

        assertions.assertThat(actual.getErrors()).isEqualTo(ValidationResult.valid().getErrors());
    }

    @Test
    void onPalletId() {
        RegistryUnitEntity box = RegistryUnitEntity.builder()
                .type(RegistryUnitType.BOX)
                .identifiers(RegistryUnitId.of(RegistryUnitIdType.PALLET_ID, "pallet_id"))
                .build();

        ValidationResult actual = registryBoxIdValidator.validate(box, null);
        ValidationResult expected = ValidationResult.of(
                RequestValidationErrorType.REGISTRY_UNIT_INVALID_ID,
                "Unit of type BOX with ID parts " +
                        "[UnitPartialId(value=pallet_id, type=PALLET_ID)] has unsupported or misses required idType"
        );


        assertions.assertThat(actual.getErrors()).isEqualTo(expected.getErrors());
    }
}
