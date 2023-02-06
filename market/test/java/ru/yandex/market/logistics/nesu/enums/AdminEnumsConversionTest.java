package ru.yandex.market.logistics.nesu.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.logistic.api.model.delivery.CargoType;
import ru.yandex.market.logistics.nesu.AbstractTest;
import ru.yandex.market.logistics.nesu.admin.converter.AdminShopValidationSettingConverter;
import ru.yandex.market.logistics.nesu.admin.model.enums.AdminCargoType;
import ru.yandex.market.logistics.nesu.admin.model.enums.AdminDropoffDisablingReason;
import ru.yandex.market.logistics.nesu.admin.model.enums.AdminFileProcessingTaskStatus;
import ru.yandex.market.logistics.nesu.admin.model.enums.AdminFileProcessingTaskType;
import ru.yandex.market.logistics.nesu.admin.model.enums.AdminShopRole;
import ru.yandex.market.logistics.nesu.admin.model.enums.AdminShopShipmentType;
import ru.yandex.market.logistics.nesu.admin.model.enums.AdminTaxSystem;
import ru.yandex.market.logistics.nesu.api.converter.EnumConverter;
import ru.yandex.market.logistics.nesu.client.enums.ShopRole;

@DisplayName("Тесты на конвертацию в админские перечисления")
class AdminEnumsConversionTest extends AbstractTest {
    private final EnumConverter enumConverter = new EnumConverter();
    private final AdminShopValidationSettingConverter adminShopValidationSettingsConverterStub =
        new AdminShopValidationSettingConverter(enumConverter);

    @ParameterizedTest
    @EnumSource(ShopRole.class)
    @DisplayName("Конвертация ShopRole")
    void shopRole(ShopRole innerValue) {
        softly.assertThat(enumConverter.toEnum(innerValue, AdminShopRole.class)).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(FileProcessingTaskType.class)
    @DisplayName("Конвертация FileProcessingTaskType")
    void processingFileType(FileProcessingTaskType fileProcessingTaskType) {
        softly.assertThat(enumConverter.toEnum(fileProcessingTaskType, AdminFileProcessingTaskType.class)).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(FileProcessingTaskStatus.class)
    @DisplayName("Конвертация FileProcessingTaskStatus")
    void processingTaskStatus(FileProcessingTaskStatus fileProcessingTaskStatus) {
        softly.assertThat(enumConverter.toEnum(fileProcessingTaskStatus, AdminFileProcessingTaskStatus.class))
            .isNotNull();
    }

    @ParameterizedTest
    @EnumSource(AdminShopRole.class)
    @DisplayName("Конвертация AdminShopRole из идентификаторов роли")
    void adminShopRole(AdminShopRole shopRole) {
        softly.assertThat(enumConverter.toEnum(shopRole, ShopRole.class)).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(ShopRole.class)
    @DisplayName("Наличие идентификаторов у ролей в конвертере")
    void convertRoleToId(ShopRole shopRole) {
        softly.assertThat(adminShopValidationSettingsConverterStub.getIdByRole(shopRole)).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(TaxSystem.class)
    @DisplayName("Конвертация TaxSystem")
    void taxSystem(TaxSystem taxSystem) {
        softly.assertThat(enumConverter.toEnum(taxSystem, AdminTaxSystem.class)).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(DropoffDisablingReason.class)
    @DisplayName("Конвертация DropoffDisablingReason")
    void dropoffDisablingReason(DropoffDisablingReason reason) {
        softly.assertThat(enumConverter.toEnum(reason, AdminDropoffDisablingReason.class)).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(AdminShopShipmentType.class)
    @DisplayName("Конвертация AdminShipmentType")
    void adminShipmentType(AdminShopShipmentType adminShipmentType) {
        softly.assertThat(enumConverter.toEnum(adminShipmentType, ShopShipmentType.class)).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(ShopShipmentType.class)
    @DisplayName("Конвертация ShopShipmentType")
    void adminShipmentType(ShopShipmentType shopShipmentType) {
        softly.assertThat(enumConverter.toEnum(shopShipmentType, AdminShopShipmentType.class)).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(value = CargoType.class, names = "UNKNOWN", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Конвертация delivery.CargoType")
    void adminCargoType(CargoType cargoType) {
        softly.assertThat(enumConverter.toEnum(cargoType, AdminCargoType.class))
            .isNotNull()
            .extracting(AdminCargoType::getCode)
            .isEqualTo(cargoType.getCode());
    }

    @ParameterizedTest
    @EnumSource(
        value = ru.yandex.market.logistic.api.model.fulfillment.CargoType.class,
        names = "UNKNOWN",
        mode = EnumSource.Mode.EXCLUDE
    )
    @DisplayName("Конвертация fulfillment.CargoType")
    void adminCargoType(ru.yandex.market.logistic.api.model.fulfillment.CargoType cargoType) {
        softly.assertThat(enumConverter.toEnum(cargoType, AdminCargoType.class))
            .isNotNull()
            .extracting(AdminCargoType::getCode)
            .isEqualTo(cargoType.getCode());
    }

}
