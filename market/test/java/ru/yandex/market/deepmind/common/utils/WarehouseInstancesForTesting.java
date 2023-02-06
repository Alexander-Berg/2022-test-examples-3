package ru.yandex.market.deepmind.common.utils;

import java.util.List;

import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.WarehouseRegion;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.WarehouseType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.WarehouseUsingType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Warehouse;

import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.CONTENTLAB_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.CROSSDOCK_ROSTOV_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.CROSSDOCK_SOFINO_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.DARKSTORE_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.EKATERINBURG_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.MARSHRUT_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.NOVOSIBIRSK_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.ROSTOV_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SAINT_PETERSBURG_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SAMARA_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SOFINO_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SOFINO_KGT_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SOFINO_RETURN_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.TOMILINO_ID;

public final class WarehouseInstancesForTesting {
    public static final Warehouse MARSHRUT = new Warehouse()
        .setId(MARSHRUT_ID).setName("Маршрут (Котельники)")
        .setType(WarehouseType.FULFILLMENT)
        .setUsingType(WarehouseUsingType.USE_FOR_FULFILLMENT)
        .setRegion(WarehouseRegion.MOSCOW);
    public static final Warehouse ROSTOV = new Warehouse()
        .setId(ROSTOV_ID).setName("Яндекс.Маркет (Ростов-на-Дону)")
        .setType(WarehouseType.FULFILLMENT)
        .setUsingType(WarehouseUsingType.USE_FOR_FULFILLMENT);
    public static final Warehouse TOMILINO = new Warehouse()
        .setId(TOMILINO_ID).setName("Яндекс.Маркет (Томилино)")
        .setType(WarehouseType.FULFILLMENT)
        .setUsingType(WarehouseUsingType.USE_FOR_FULFILLMENT)
        .setRegion(WarehouseRegion.MOSCOW);
    public static final Warehouse SOFINO = new Warehouse()
        .setId(SOFINO_ID).setName("Яндекс.Маркет (Софьино)")
        .setType(WarehouseType.FULFILLMENT)
        .setUsingType(WarehouseUsingType.USE_FOR_FULFILLMENT)
        .setRegion(WarehouseRegion.MOSCOW);
    public static final Warehouse EKATERINBURG = new Warehouse()
        .setId(EKATERINBURG_ID).setName("Яндекс.Маркет (Екатеринбург)")
        .setType(WarehouseType.FULFILLMENT)
        .setUsingType(WarehouseUsingType.USE_FOR_FULFILLMENT)
        .setRegion(WarehouseRegion.EKB);
    public static final Warehouse SAINT_PETERSBURG = new Warehouse()
        .setId(SAINT_PETERSBURG_ID).setName("Яндекс.Маркет (Санкт-Петербург)")
        .setType(WarehouseType.FULFILLMENT)
        .setUsingType(WarehouseUsingType.USE_FOR_FULFILLMENT)
        .setRegion(WarehouseRegion.SPB);
    public static final Warehouse SAMARA = new Warehouse()
        .setId(SAMARA_ID).setName("Яндекс.Маркет (Самара)")
        .setType(WarehouseType.FULFILLMENT)
        .setUsingType(WarehouseUsingType.USE_FOR_FULFILLMENT)
        .setRegion(WarehouseRegion.SAMARA);
    public static final Warehouse NOVOSIBIRSK = new Warehouse()
        .setId(NOVOSIBIRSK_ID).setName("Яндекс.Маркет (Новосибирск)")
        .setType(WarehouseType.FULFILLMENT)
        .setUsingType(WarehouseUsingType.USE_FOR_FULFILLMENT)
        .setRegion(WarehouseRegion.NOVOSIB);
    public static final Warehouse SOFINO_KGT = new Warehouse()
        .setId(SOFINO_KGT_ID).setName("Яндекс.Маркет (Софьино КГТ)")
        .setType(WarehouseType.FULFILLMENT)
        .setUsingType(WarehouseUsingType.USE_FOR_FULFILLMENT)
        .setRegion(WarehouseRegion.MOSCOW);
    public static final Warehouse DARKSTORE = new Warehouse()
        .setId(DARKSTORE_ID).setName("Яндекс Маркет для Даркстора")
        .setType(WarehouseType.FULFILLMENT)
        .setUsingType(WarehouseUsingType.USE_FOR_FULFILLMENT);
    public static final Warehouse SOFINO_RETURN = new Warehouse()
        .setId(SOFINO_RETURN_ID).setName("Яндекс.Маркет (Софьино Возвратный склад)")
        .setType(WarehouseType.FULFILLMENT)
        .setUsingType(WarehouseUsingType.USE_FOR_FULFILLMENT)
        .setRegion(WarehouseRegion.MOSCOW);
    public static final Warehouse CONTENTLAB = new Warehouse()
        .setId(CONTENTLAB_ID).setName("Лаборатория Контента")
        .setType(WarehouseType.FULFILLMENT);
    public static final Warehouse CROSSDOCK_ROSTOV = new Warehouse()
        .setId(CROSSDOCK_ROSTOV_ID).setName("Яндекс.Маркет (Ростов-на-Дону) (кроссдок)")
        .setType(WarehouseType.FAKE)
        .setUsingType(WarehouseUsingType.USE_FOR_CROSSDOCK)
        .setCargoTypeLmsIds();
    public static final Warehouse CROSSDOCK_SOFINO = new Warehouse()
        .setId(CROSSDOCK_SOFINO_ID).setName("Яндекс.Маркет (Софьино) (кроссдок)")
        .setType(WarehouseType.FAKE)
        .setUsingType(WarehouseUsingType.USE_FOR_CROSSDOCK)
        .setCargoTypeLmsIds();
    public static final Warehouse SORTING_CENTER_1 = new Warehouse()
        .setId(1551L).setName("Сортировочный центр 1")
        .setType(WarehouseType.SORTING_CENTER)
        .setUsingType(WarehouseUsingType.USE_FOR_DROPSHIP);
    public static final Warehouse LOTTE = new Warehouse()
        .setId(1000L).setName("Лотте-Плаза")
        .setType(WarehouseType.FULFILLMENT)
        .setUsingType(WarehouseUsingType.USE_FOR_FULFILLMENT)
        .setRegion(WarehouseRegion.MOSCOW);

    public static final List<Warehouse> ALL_FULFILLMENT = List.of(
        MARSHRUT,
        ROSTOV,
        TOMILINO,
        SOFINO,
        SOFINO_RETURN
    );
    public static final List<Warehouse> ALL_CROSSDOCK = List.of(
        CROSSDOCK_ROSTOV,
        CROSSDOCK_SOFINO
    );
    public static final List<Warehouse> ALL_DROPSHIP = List.of(
        SORTING_CENTER_1
    );
    public static final List<Warehouse> ALL_FULFILLMENT_FOR_UNIT_TESTS = List.of(
        MARSHRUT,
        ROSTOV,
        TOMILINO,
        SOFINO,
        SOFINO_RETURN,
        EKATERINBURG,
        SAINT_PETERSBURG,
        SAMARA,
        NOVOSIBIRSK,
        SOFINO_KGT,
        DARKSTORE
    );
    public static final List<Warehouse> ALL_FULFILLMENT_FOR_SPECIAL_ORDERS = List.of(
        ROSTOV,
        TOMILINO,
        SOFINO,
        EKATERINBURG,
        SAINT_PETERSBURG,
        SAMARA,
        SOFINO_KGT
    );
    private WarehouseInstancesForTesting() {
        throw new IllegalStateException("Accessing private constructor of utility class");
    }
}
