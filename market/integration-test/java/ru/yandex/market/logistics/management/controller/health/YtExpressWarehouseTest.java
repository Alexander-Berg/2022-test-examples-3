package ru.yandex.market.logistics.management.controller.health;

import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.service.client.LogisticPointRadialLocationZoneService;
import ru.yandex.market.logistics.management.service.client.RadialLocationZoneService;

@DatabaseSetup({
    "/data/controller/health/express/before/prepare.xml",
    "/data/controller/health/express/before/prepare_warehouse.xml"
})
public class YtExpressWarehouseTest extends AbstractContextualTest {
    @Autowired
    private RadialLocationZoneService radialLocationZoneService;
    @Autowired
    private LogisticPointRadialLocationZoneService logisticPointRadialLocationZoneService;

    // Связь зоны со складом

    @Test
    @DisplayName("Связываем зону с включенным складом экспресса")
    @ExpectedDatabase(
        value = "/data/controller/health/express/after/link_active_express.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createLinkActiveExpressWarehouse() {
        logisticPointRadialLocationZoneService.addRadialZones(1L, Set.of(1L), true);
    }

    @Test
    @DisplayName("Связываем зону с включенным складом экспресса и ENABLE_EXPRESS_OUTLETS")
    @DatabaseSetup(
        value = "/data/controller/health/express/before/enable_outlets_1.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/controller/health/express/after/link_active_express_enabled_outlets.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createLinkActiveExpressWarehouseEnableOutlets() {
        logisticPointRadialLocationZoneService.addRadialZones(1L, Set.of(1L), true);
    }

    @Test
    @DisplayName("Связываем склад с несколькими зонами с еще одной")
    @DatabaseSetup(value = "/data/controller/health/express/before/link_123.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
        value = "/data/controller/health/express/after/link_active_express_several.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createSeveralLinkActiveExpressWarehouse() {
        //ничего не делаем тк данные должны появиться после настройки базы
    }

    @Test
    @DisplayName("Связываем зону с включенным складом не экспресса")
    @DatabaseSetup(value = "/data/controller/health/express/before/link_21.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
        value = "/data/controller/health/express/default_yt_express_warehouse.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createLinkActiveNotExpressWarehouse() {
        //ничего не делаем тк данные должны появиться после настройки базы
    }

    @Test
    @DisplayName("Связываем зону с включенным складом не экспресса с ENABLE_EXPRESS_OUTLETS")
    @DatabaseSetup(
        value = {
            "/data/controller/health/express/before/enable_outlets_2.xml",
            "/data/controller/health/express/before/link_21.xml"
        },
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/controller/health/express/default_yt_express_warehouse.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createLinkActiveNotExpressWarehouseWithEnableOutlets() {
        //ничего не делаем тк данные должны появиться после настройки базы
    }

    @Test
    @DisplayName("Связываем зону с выключенным складом экспресса")
    @DatabaseSetup(value = "/data/controller/health/express/before/link_31.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
        value = "/data/controller/health/express/after/link_31.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createLinkInactiveExpressWarehouse() {
        //ничего не делаем тк данные должны появиться после настройки базы
    }

    @Test
    @DisplayName("Связываем зону с выключенным складом экспресса и ENABLE_EXPRESS_OUTLETS")
    @DatabaseSetup(
        value = "/data/controller/health/express/before/enable_outlets_3.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/controller/health/express/after/link_31_enabled_outlets.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createLinkInactiveExpressWarehouseWithEnableOutlets() {
        //ничего не делаем тк данные должны появиться после настройки базы
    }

    @Test
    @DisplayName("Связываем зону с выключенным складом не экспресса")
    @DatabaseSetup(value = "/data/controller/health/express/before/link_41.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
        value = "/data/controller/health/express/default_yt_express_warehouse.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createLinkInactiveNotExpressWarehouse() {
        //ничего не делаем тк данные должны появиться после настройки базы
    }

    @Test
    @DisplayName("Связываем зону с выключенным складом не экспресса и ENABLE_EXPRESS_OUTLETS")
    @DatabaseSetup(
        value = {
            "/data/controller/health/express/before/enable_outlets_4.xml",
            "/data/controller/health/express/before/link_41.xml"
        },
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/controller/health/express/default_yt_express_warehouse.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createLinkInactiveNotExpressWarehouseEnableExpress() {
        //ничего не делаем тк данные должны появиться после настройки базы
    }

    @Test
    @DisplayName("Удаляем связь зоны с включенным складом экспресса")
    @ExpectedDatabase(
        value = "/data/controller/health/express/after/unlink_point.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void unlinkActiveExpressWarehouse() {
        logisticPointRadialLocationZoneService.removeRadialZones(7L, Set.of(1L));
    }

    @Test
    @DisplayName("Удаляем связь зоны с включенным складом экспресса и ENABLE_EXPRESS_OUTLETS")
    @DatabaseSetup(
        value = "/data/controller/health/express/before/enable_outlets_7.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/controller/health/express/after/unlink_point.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void unlinkActiveExpressWarehouseWithEnableOutlets() {
        logisticPointRadialLocationZoneService.removeRadialZones(7L, Set.of(1L));
    }

    @Test
    @DisplayName("Удалить связь зоны с включенным складом не экспресса")
    @ExpectedDatabase(
        value = "/data/controller/health/express/default_yt_express_warehouse.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void unlinkActiveNotExpressWarehouse() {
        logisticPointRadialLocationZoneService.removeRadialZones(8L, Set.of(1L));
    }

    @Test
    @DisplayName("Удалить связь зоны с включенным складом не экспресса и ENABLE_EXPRESS_OUTLETS")
    @DatabaseSetup(
        value = "/data/controller/health/express/before/enable_outlets_8.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/controller/health/express/default_yt_express_warehouse.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void unlinkActiveNotExpressWarehouseEnableOutlets() {
        logisticPointRadialLocationZoneService.removeRadialZones(8L, Set.of(1L));
    }

    @Test
    @DisplayName("Удаляем связь зоны с выключенным складом экспресса")
    @ExpectedDatabase(
        value = "/data/controller/health/express/default_yt_express_warehouse.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void unlinkInactiveExpressWarehouse() {
        logisticPointRadialLocationZoneService.removeRadialZones(9L, Set.of(1L));
    }

    @Test
    @DisplayName("Удаляем связь зоны с выключенным складом экспресса и ENABLE_EXPRESS_OUTLETS")
    @DatabaseSetup(
        value = "/data/controller/health/express/before/enable_outlets_9.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/controller/health/express/default_yt_express_warehouse.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void unlinkInactiveExpressWarehouseEnableOutlets() {
        logisticPointRadialLocationZoneService.removeRadialZones(9L, Set.of(1L));
    }

    @Test
    @DisplayName("Удаляем связь зоны с выключенным складом не экспресса")
    @ExpectedDatabase(
        value = "/data/controller/health/express/default_yt_express_warehouse.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void unlinkInactiveNotExpressWarehouse() {
        logisticPointRadialLocationZoneService.removeRadialZones(10L, Set.of(1L));
    }

    @Test
    @DisplayName("Удаляем связь зоны с выключенным складом не экспресса и ENABLE_EXPRESS_OUTLETS")
    @DatabaseSetup(
        value = "/data/controller/health/express/before/enable_outlets_10.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/controller/health/express/default_yt_express_warehouse.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void unlinkInactiveNotExpressWarehouseEnableOutlets() {
        logisticPointRadialLocationZoneService.removeRadialZones(10L, Set.of(1L));
    }

    // Параметры партнера

    /*
        Партнер 8 становится экспрессом. Для склада 8 связка есть, для склада 11 нет.
        Ожидается появление строчки для склада 8.
     */
    @Test
    @DisplayName("Делаем партнера экспрессом")
    @DatabaseSetup(
        value = "/data/controller/health/express/before/add_express_param.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/controller/health/express/after/add_param.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void addExpressParam() {
        //ничего не делаем тк данные должны появиться после настройки базы
    }

    @Test
    @DisplayName("Делаем партнера экспрессом и ENABLE_EXPRESS_OUTLETS")
    @DatabaseSetup(
        value = {
            "/data/controller/health/express/before/enable_outlets_8.xml",
            "/data/controller/health/express/before/add_express_param.xml"
        },
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/controller/health/express/after/add_param_enabled_outlets.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void addExpressParamEnableOutlets() {
        //ничего не делаем тк данные должны появиться после настройки базы
    }

    @Test
    @DisplayName("Добавляем партнеру параметр, отличный от экспресса")
    @DatabaseSetup(
        value = "/data/controller/health/express/before/add_another_param.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/controller/health/express/default_yt_express_warehouse.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void addAnotherParam() {
        //ничего не делаем тк данные должны появиться после настройки базы
    }

    @Test
    @DisplayName("Добавляем партнеру параметр, отличный от экспресса и ENABLE_EXPRESS_OUTLETS")
    @DatabaseSetup(
        value = {
            "/data/controller/health/express/before/enable_outlets_8.xml",
            "/data/controller/health/express/before/add_another_param.xml"
        },
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/controller/health/express/default_yt_express_warehouse.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void addAnotherParamEnableOutlets() {
        //ничего не делаем тк данные должны появиться после настройки базы
    }

    @Test
    @DisplayName("Добавляем партнеру параметр экспресса, но со значением 0")
    @DatabaseSetup(
        value = "/data/controller/health/express/before/add_express_param_false.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/controller/health/express/default_yt_express_warehouse.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void addExpressFalseParam() {
        //ничего не делаем тк данные должны появиться после настройки базы
    }

    @Test
    @DisplayName("Добавляем партнеру параметр экспресса, но со значением 0 и ENABLE_EXPRESS_OUTLETS")
    @DatabaseSetup(
        value = {
            "/data/controller/health/express/before/enable_outlets_8.xml",
            "/data/controller/health/express/before/add_express_param_false.xml"
        },
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/controller/health/express/default_yt_express_warehouse.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void addExpressFalseParamEnableOutlets() {
        //ничего не делаем тк данные должны появиться после настройки базы
    }

    @Test
    @DisplayName("Удаляем параметр экспресса")
    @DatabaseSetup(
        value = "/data/controller/health/express/before/prepare_partner_param.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/controller/health/express/before/drop_partner_param.xml",
        type = DatabaseOperation.DELETE
    )
    @ExpectedDatabase(
        value = "/data/controller/health/express/default_yt_express_warehouse.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void dropExpressParam() {
        //ничего не делаем тк данные должны появиться после настройки базы
    }

    @Test
    @DisplayName("Удаляем параметр экспресса и ENABLE_EXPRESS_OUTLETS")
    @DatabaseSetup(
        value = "/data/controller/health/express/before/drop_partner_param.xml",
        type = DatabaseOperation.DELETE
    )
    @ExpectedDatabase(
        value = "/data/controller/health/express/default_yt_express_warehouse.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void dropExpressParamEnableOutlets() {
        //ничего не делаем тк данные должны появиться после настройки базы
    }

    @Test
    @DisplayName("Выключаем параметр экспресса")
    @DatabaseSetup(
        value = "/data/controller/health/express/before/prepare_partner_param.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/controller/health/express/before/off_partner_param.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/data/controller/health/express/default_yt_express_warehouse.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void offExpressParam() {
        //ничего не делаем тк данные должны появиться после настройки базы
    }

    @Test
    @DisplayName("Выключаем параметр экспресса и ENABLE_EXPRESS_OUTLETS")
    @DatabaseSetup(
        value = "/data/controller/health/express/before/prepare_partner_param.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/controller/health/express/before/off_partner_param.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/data/controller/health/express/default_yt_express_warehouse.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void offExpressParamEnableOutlets() {
        //ничего не делаем тк данные должны появиться после настройки базы
    }

    @Test
    @DisplayName("Включаем параметр экспресса")
    @DatabaseSetup(
        value = "/data/controller/health/express/before/off_partner_param.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/controller/health/express/before/prepare_partner_param.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/data/controller/health/express/after/add_param.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void onExpressParam() {
        //ничего не делаем тк данные должны появиться после настройки базы
    }

    @Test
    @DisplayName("Включаем параметр экспресса и ENABLE_EXPRESS_OUTLETS")
    @DatabaseSetup(
        value = "/data/controller/health/express/before/off_partner_param.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/controller/health/express/before/prepare_partner_param.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/data/controller/health/express/after/add_param.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void onExpressParamEnableOutlets() {
        //ничего не делаем тк данные должны появиться после настройки базы
    }

    // Радиальные зоны
    @Test
    @DisplayName("Удаляем зону")
    @ExpectedDatabase(
        value = "/data/controller/health/express/after/drop_zone.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void dropRadialZone() {
        radialLocationZoneService.delete(1L);
    }

    @Test
    @DisplayName("Обновляем зону")
    @ExpectedDatabase(
        value = "/data/controller/health/express/after/update_zone.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateRadialZoneRadius() {
        radialLocationZoneService.update(2L, "name", 2000L, 200L, true);
    }

    // Склады

    @Test
    @DisplayName("Выключаем склад")
    @DatabaseSetup(value = "/data/controller/health/express/before/off_point_12.xml", type = DatabaseOperation.UPDATE)
    @ExpectedDatabase(
        value = "/data/controller/health/express/after/off_point.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void turnWarehouseOff() {
        //ничего не делаем тк данные должны появиться после настройки базы
    }

    @Test
    @DisplayName("Выключаем склад с ENABLE_EXPRESS_OUTLETS")
    @DatabaseSetup(value = "/data/controller/health/express/before/off_point_12.xml", type = DatabaseOperation.UPDATE)
    @ExpectedDatabase(
        value = "/data/controller/health/express/after/off_point.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void turnWarehouseOffEnableOutlets() {
        //ничего не делаем тк данные должны появиться после настройки базы
    }

    @Test
    @DisplayName("Включаем склад")
    @DatabaseSetup(value = "/data/controller/health/express/before/on_point_6.xml", type = DatabaseOperation.UPDATE)
    @ExpectedDatabase(
        value = "/data/controller/health/express/after/on_point.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void turnWarehouseOn() {
        //ничего не делаем тк данные должны появиться после настройки базы
    }

    @Test
    @DisplayName("Включаем склад с ENABLE_EXPRESS_OUTLETS")
    @DatabaseSetup(value = "/data/controller/health/express/before/on_point_6.xml", type = DatabaseOperation.UPDATE)
    @ExpectedDatabase(
        value = "/data/controller/health/express/after/on_point.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void turnWarehouseOnEnableOutlets() {
        //ничего не делаем тк данные должны появиться после настройки базы
    }

    @Test
    @DisplayName("Активируем партнера")
    @DatabaseSetup(
        value = "/data/controller/health/express/before/activate_partner.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/data/controller/health/express/after/activate_partner.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void makePartnerActive() {
        //ничего не делаем тк данные должны появиться после настройки базы
    }

    @Test
    @DisplayName("Активируем партнера c ENABLE_EXPRESS_OUTLETS")
    @DatabaseSetup(
        value = "/data/controller/health/express/before/enable_outlets_13.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/controller/health/express/before/activate_partner.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/data/controller/health/express/after/activate_partner_enabled_outlets.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void makePartnerActiveEnableOutlets() {
        //ничего не делаем тк данные должны появиться после настройки базы
    }

    @Test
    @DisplayName("Деактивируем партнера")
    @DatabaseSetup(
        value = "/data/controller/health/express/before/deactivate_partner.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/data/controller/health/express/after/deactivate_partner.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void makePartnerInactive() {
        //ничего не делаем тк данные должны появиться после настройки базы
    }

    @Test
    @DisplayName("Деактивируем партнера с ENABLE_EXPRESS_OUTLETS")
    @DatabaseSetup(
        value = "/data/controller/health/express/before/enable_outlets_12.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/controller/health/express/before/deactivate_partner.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/data/controller/health/express/after/deactivate_partner.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void makePartnerInactiveEnableOutlets() {
        //ничего не делаем тк данные должны появиться после настройки базы
    }

    // Параметры

    @Test
    @DisplayName("Добавляем валидный сц для невыкупов экспрессу")
    @DatabaseSetup(
        value = "/data/controller/health/express/before/add_valid_return_sc_12.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/controller/health/express/after/add_12_return_sc.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void addReturnScParam() {
        //ничего не делаем тк данные должны появиться после настройки базы
    }

    @Test
    @DisplayName("Добавляем невалидный сц для невыкупов экспрессу")
    @DatabaseSetup(
        value = "/data/controller/health/express/before/add_invalid_return_sc_12.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/controller/health/express/after/add_12_invalid_return_sc.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void addInvalidReturnScParam() {
        //ничего не делаем тк данные должны появиться после настройки базы
    }

    @Test
    @DisplayName("Обновляем валидный сц для невыкупов экспрессу на невалидный")
    @DatabaseSetup(
        value = "/data/controller/health/express/before/add_valid_return_sc_12.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/controller/health/express/before/update_invalid_return_sc_12.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/data/controller/health/express/after/add_12_invalid_return_sc.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateToInvalidReturnSc() {
        //ничего не делаем тк данные должны появиться после настройки базы
    }

    @Test
    @DisplayName("Обновляем невалидный сц для невыкупов экспрессу на валидный")
    @DatabaseSetup(
        value = "/data/controller/health/express/before/add_invalid_return_sc_12.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/controller/health/express/before/update_valid_return_sc_12.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/data/controller/health/express/after/add_12_return_sc.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateToValidReturnSc() {
        //ничего не делаем тк данные должны появиться после настройки базы
    }

    @Test
    @DisplayName("Удаляем валидный сц для невыкупов экспресса")
    @DatabaseSetup(
        value = "/data/controller/health/express/before/add_valid_return_sc_12.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/controller/health/express/before/delete_valid_return_sc_12.xml",
        type = DatabaseOperation.DELETE
    )
    @ExpectedDatabase(
        value = "/data/controller/health/express/after/add_12_invalid_return_sc.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteValidReturnSc() {
        //ничего не делаем тк данные должны появиться после настройки базы
    }

    @Test
    @DisplayName("Удаляем невалидный сц для невыкупов экспресса")
    @DatabaseSetup(
        value = "/data/controller/health/express/before/add_invalid_return_sc_12.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/controller/health/express/before/delete_invalid_return_sc_12.xml",
        type = DatabaseOperation.DELETE
    )
    @ExpectedDatabase(
        value = "/data/controller/health/express/after/add_12_invalid_return_sc.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteInvalidReturnSc() {
        //ничего не делаем тк данные должны появиться после настройки базы
    }
}
