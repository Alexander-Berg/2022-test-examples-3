package ru.yandex.market.logistics.management.util;

import lombok.experimental.UtilityClass;

import ru.yandex.market.logistics.management.domain.entity.PartnerSubtype;
import ru.yandex.market.logistics.management.domain.entity.PartnerSubtypeFeatures;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerType;

@UtilityClass
@SuppressWarnings("HideUtilityClassConstructor")
public class TestPartnerSubtypes {

    public static final PartnerSubtype SUB_TYPE_1 = new PartnerSubtype()
        .setId(1L)
        .setPartnerType(PartnerType.DELIVERY)
        .setName("Партнерская доставка (контрактная)")
        .setFeatures(new PartnerSubtypeFeatures());

    public static final PartnerSubtype SUB_TYPE_2 = new PartnerSubtype()
        .setId(2L)
        .setPartnerType(PartnerType.DELIVERY)
        .setName("Маркет Курьер")
        .setFeatures(new PartnerSubtypeFeatures());

    public static final PartnerSubtype SUB_TYPE_3 = new PartnerSubtype()
        .setId(3L)
        .setPartnerType(PartnerType.DELIVERY)
        .setName("Маркет свои ПВЗ")
        .setFeatures(new PartnerSubtypeFeatures());

    public static final PartnerSubtype SUB_TYPE_4 = new PartnerSubtype()
        .setId(4L)
        .setPartnerType(PartnerType.DELIVERY)
        .setName("Партнерские ПВЗ (ИПэшники)")
        .setFeatures(new PartnerSubtypeFeatures());

    public static final PartnerSubtype SUB_TYPE_5 = new PartnerSubtype()
        .setId(5L)
        .setPartnerType(PartnerType.DELIVERY)
        .setName("Маркет Локеры")
        .setFeatures(new PartnerSubtypeFeatures().setDefaultMarketBrandedLogisticsPoints(true));

    public static final PartnerSubtype SUB_TYPE_6 = new PartnerSubtype()
        .setId(6L)
        .setPartnerType(PartnerType.SORTING_CENTER)
        .setName("СЦ для МК")
        .setFeatures(new PartnerSubtypeFeatures());

    public static final PartnerSubtype SUB_TYPE_7 = new PartnerSubtype()
        .setId(7L)
        .setPartnerType(PartnerType.SORTING_CENTER)
        .setName("Партнерский СЦ")
        .setFeatures(new PartnerSubtypeFeatures());

    public static final PartnerSubtype SUB_TYPE_8 = new PartnerSubtype()
        .setId(8L)
        .setPartnerType(PartnerType.DELIVERY)
        .setName("Такси-Лавка")
        .setFeatures(new PartnerSubtypeFeatures());

    public static final PartnerSubtype SUB_TYPE_34 = new PartnerSubtype()
        .setId(34L)
        .setPartnerType(PartnerType.DELIVERY)
        .setName("Такси-Экспресс")
        .setFeatures(new PartnerSubtypeFeatures());

    public static final PartnerSubtype SUB_TYPE_67 = new PartnerSubtype()
        .setId(67L)
        .setPartnerType(PartnerType.DELIVERY)
        .setName("Такси-Авиа")
        .setFeatures(new PartnerSubtypeFeatures());

    public static final PartnerSubtype SUB_TYPE_68 = new PartnerSubtype()
        .setId(68L)
        .setPartnerType(PartnerType.SORTING_CENTER)
        .setName("Дроп-офф")
        .setFeatures(new PartnerSubtypeFeatures());

    public static final PartnerSubtype SUB_TYPE_69 = new PartnerSubtype()
        .setId(69L)
        .setPartnerType(PartnerType.DELIVERY)
        .setName("Локеры (sandbox)")
        .setFeatures(new PartnerSubtypeFeatures());

    public static final PartnerSubtype SUB_TYPE_70 = new PartnerSubtype()
        .setId(70L)
        .setPartnerType(PartnerType.FULFILLMENT)
        .setName("Даркстор")
        .setFeatures(new PartnerSubtypeFeatures());

    public static final PartnerSubtype SUB_TYPE_103 = new PartnerSubtype()
        .setId(103L)
        .setPartnerType(PartnerType.DELIVERY)
        .setName("Go Платформа")
        .setFeatures(new PartnerSubtypeFeatures());
}
