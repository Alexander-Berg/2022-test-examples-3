package ru.yandex.market.logistics.lom.converter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.converter.lgw.fulfillment.LgwIntakeFulfillmentConverter;
import ru.yandex.market.logistics.lom.converter.lgw.fulfillment.LgwSelfExportFulfillmentConverter;
import ru.yandex.market.logistics.lom.entity.enums.ShipmentType;
import ru.yandex.market.logistics.lom.utils.ConverterTestEntitiesFactory;
import ru.yandex.market.logistics.lom.utils.lgw.CreateLgwFulfillmentEntitiesUtils;

class CreateIntakeFulfillmentConverterTest extends AbstractContextualTest {

    @Autowired
    private LgwIntakeFulfillmentConverter lgwIntakeFulfillmentConverter;

    @Autowired
    private LgwSelfExportFulfillmentConverter lgwSelfExportFulfillmentConverter;

    @Test
    @DisplayName("СЦ: Удачное создание заявки на забор")
    void createFfIntake() {
        softly.assertThat(
            lgwIntakeFulfillmentConverter.createIntakeRequest(
                ConverterTestEntitiesFactory.createFfShipmentApplication(ShipmentType.WITHDRAW),
                ConverterTestEntitiesFactory.createScLocationFrom(6)
                    .setWarehouseWorkTime(ConverterTestEntitiesFactory.createSchedule())
            )
        ).isEqualTo(CreateLgwFulfillmentEntitiesUtils.createLgwIntake());
    }

    @Test
    @DisplayName("СЦ: Удачное создание заявки на самопривоз")
    void createFfSelfExport() {
        softly.assertThat(
            lgwSelfExportFulfillmentConverter.createSelfExportRequest(
                ConverterTestEntitiesFactory.createFfShipmentApplication(ShipmentType.IMPORT),
                ConverterTestEntitiesFactory.createScLocationTo(7)
                    .setWarehouseWorkTime(ConverterTestEntitiesFactory.createSchedule())
            )
        ).isEqualTo(CreateLgwFulfillmentEntitiesUtils.createLgwSelfExport());
    }
}
