#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryCalcFeedInfo,
    DeliveryOption,
    DeliveryServiceRegionToRegionInfo,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    GpsCoord,
    HyperCategory,
    HyperCategoryType,
    Model,
    Offer,
    Outlet,
    OutletDeliveryOption,
    PickupBucket,
    PickupOption,
    Region,
    RegionalDelivery,
    Shop,
    WarehouseWithPriority,
    WarehousesPriorityInRegion,
)
from core.testcase import TestCase, main
from core.types.offer import OfferDimensions
from core.types.sku import MarketSku, BlueOffer
from core.types.taxes import Vat, Tax
from core.matcher import Absent, ElementCount


USE_DEPRECATED_DIRECT_SHIPPING_FLOW = "&rearr-factors=get_rid_of_direct_shipping=0"


def make_supplier(
    supplier_id,
    feed_id,
    warehouse_id,
    type=Shop.THIRD_PARTY,
    is_fulfillment=True,
    client_id=None,
    direct_shipping=True,
    ignore_stocks=False,
):
    return Shop(
        fesh=supplier_id,
        datafeed_id=feed_id,
        priority_region=2,
        name='shop{}_feed{}'.format(supplier_id, feed_id),
        tax_system=Tax.OSN,
        supplier_type=type,
        blue=Shop.BLUE_REAL,
        cpa=Shop.CPA_REAL if is_fulfillment else Shop.CPA_NO,
        warehouse_id=warehouse_id,
        fulfillment_program=is_fulfillment,
        client_id=client_id or supplier_id,
        direct_shipping=direct_shipping,
        ignore_stocks=ignore_stocks,
    )


def make_offer(price, shop_sku, supplier, ware_md5, stock_store_count, is_fulfillment=True):
    return BlueOffer(
        price=price,
        vat=Vat.VAT_10,
        feedid=supplier.datafeed_id,
        offerid=shop_sku,
        waremd5=ware_md5,
        supplier_id=supplier.fesh,
        stock_store_count=stock_store_count,
        is_fulfillment=is_fulfillment,
        weight=1,
        dimensions=OfferDimensions(length=10, width=10, height=10),
    )


class T(TestCase):
    """
    Проверяем что стратегия минимизации посылок с подменой оферов между поставщиками собственно меняет их даже если
    это менят цену офера или поставщика
    """

    supplier2_w10_1p = make_supplier(2, 21, 10, type=Shop.FIRST_PARTY)
    supplier2_w11_1p = make_supplier(2, 22, 11, type=Shop.FIRST_PARTY)
    supplier3_w10_1p = make_supplier(3, 31, 10, type=Shop.FIRST_PARTY)
    supplier4_w11_1p = make_supplier(4, 41, 11, type=Shop.FIRST_PARTY)
    supplier5_w10_3p = make_supplier(5, 51, 10)
    supplier12_w12_3p = make_supplier(12, 121, 12)

    # dropship suppliers that sends parcels over Sorting Center
    supplier9_w300_dropship = make_supplier(
        9, 91, 300, is_fulfillment=False, direct_shipping=False
    )  # parcels goes over sc 10
    supplier10_w301_dropship = make_supplier(
        10, 101, 301, is_fulfillment=False, direct_shipping=False
    )  # parcels goes over sc 11
    supplier10_w400_dropship = make_supplier(
        11, 111, 400, is_fulfillment=False, direct_shipping=False
    )  # parcels goes over sc 10
    supplier15_w600_dropship = make_supplier(
        15, 151, 600, is_fulfillment=False, direct_shipping=False, type=None
    )  # parcels goes over sc 11

    # click-n-collect suppliers
    supplier13_w500_cnc = make_supplier(13, 131, 500, is_fulfillment=False, ignore_stocks=True)
    supplier14_w500_cnc = make_supplier(14, 141, 500, is_fulfillment=False, ignore_stocks=True)

    cheburek_s2_w10_1p = make_offer(90, 'Cheburek_w10', supplier2_w10_1p, 'Cheburek_s2_w10_1p___g', 1)
    cheburek_s2_w11_1p = make_offer(100, 'Cheburek_w11', supplier2_w11_1p, 'Cheburek_s2_w11_1p___g', 1)
    msku_cheburek = MarketSku(
        title="Cheburek",
        hyperid=100,
        sku=100,
        blue_offers=[cheburek_s2_w10_1p, cheburek_s2_w11_1p],
        delivery_buckets=[1],
    )

    nakovalnya_s3_w10_1p = make_offer(100, 'Nakovalnya_w10', supplier3_w10_1p, 'Nakovalnya_s3_w10_1p_g', 1)
    nakovalnya_s4_w11_1p = make_offer(90, 'Nakovalnya_w11', supplier4_w11_1p, 'Nakovalnya_s4_w11_1p_g', 1)
    msku_nakovalnya = MarketSku(
        title="Nokovalnya",
        hyperid=200,
        sku=200,
        blue_offers=[nakovalnya_s3_w10_1p, nakovalnya_s4_w11_1p],
        delivery_buckets=[1],
    )

    resistor_s4_w11_1p = make_offer(100, 'Resistor_w11', supplier4_w11_1p, 'Resistor_s4_w11_1p___g', 1)
    resistor_s5_w10_3p = make_offer(90, 'Resistor_w10', supplier5_w10_3p, 'Resistor_s5_w10_3p___g', 1)
    msku_resistor = MarketSku(
        title="Resistor",
        hyperid=201,
        sku=201,
        blue_offers=[resistor_s4_w11_1p, resistor_s5_w10_3p],
        delivery_buckets=[1],
    )

    gravicapa_s9_w300_ds = make_offer(
        100, 'Gravicapa_w300', supplier9_w300_dropship, 'Gravicap_s9_w300_ds__g', 1, is_fulfillment=False
    )
    gravicapa_s10_w301_ds = make_offer(
        100, 'Gravicapa_w301', supplier10_w301_dropship, 'Gravicap_s10_w301_ds_g', 1, is_fulfillment=False
    )
    gravicapa2_s10_w400_ds = make_offer(
        100, 'Gravicapa2_w400', supplier10_w400_dropship, 'Gravica2_s10_w400_ds_g', 1, is_fulfillment=False
    )
    msku_gravicapa = MarketSku(
        title="Gravicapa",
        hyperid=203,
        sku=203,
        blue_offers=[gravicapa_s9_w300_ds, gravicapa_s10_w301_ds],
        delivery_buckets=[1],
    )

    msku_gravicapa2 = MarketSku(
        title="Gravicapa", hyperid=203, sku=2030, blue_offers=[gravicapa2_s10_w400_ds], delivery_buckets=[1]
    )

    seno_s2_w10_1p = make_offer(100, 'Seno_w10', supplier2_w10_1p, 'Seno_s2_w10_1p_______g', 1)
    seno_s3_w10_1p = make_offer(99, 'Seno_w10_cheap', supplier3_w10_1p, 'Seno_s3_w10_1p_______g', 1)
    msku_seno = MarketSku(
        title="Seno", hyperid=204, sku=204, blue_offers=[seno_s2_w10_1p, seno_s3_w10_1p], delivery_buckets=[1]
    )

    oves_s2_w10_1p = make_offer(100, 'Oves_w10', supplier2_w10_1p, 'Oves_s2_w10_1p_______g', 1)
    oves_s12_w12_3p = make_offer(90, 'Oves_w12', supplier12_w12_3p, 'Oves_s12_w12_3p______g', 1)
    msku_oves = MarketSku(
        title="Овес", hyperid=205, sku=205, blue_offers=[oves_s2_w10_1p, oves_s12_w12_3p], delivery_buckets=[1]
    )

    fork_s2_w10_1p = make_offer(100, 'Fork_w10', supplier2_w10_1p, 'Fork_s2_w10_1p_______g', 1)
    fork_s13_w500_cnc = make_offer(90, 'Fork_w500', supplier13_w500_cnc, 'Fork_s13_w500_cnc____g', 1)
    msku_forks = MarketSku(
        title="Вилки", hyperid=206, sku=206, blue_offers=[fork_s2_w10_1p, fork_s13_w500_cnc], delivery_buckets=[1]
    )

    spoon_s2_w11_1p = make_offer(100, 'Spoon_w11', supplier2_w11_1p, 'Spoon_s2_w11_1p______g', 1)
    spoon_s14_w500_cnc = make_offer(90, 'Spoon_w500', supplier14_w500_cnc, 'Spoon_s14_w500_cnc___g', 1)
    msku_spoons = MarketSku(
        title="Ложки", hyperid=207, sku=207, blue_offers=[spoon_s2_w11_1p, spoon_s14_w500_cnc], delivery_buckets=[1]
    )

    axe_s5_w10_3p = make_offer(300, 'Axe_w10', supplier5_w10_3p, 'Axe_s5_w10_3p________g', 1)
    axe_s12_w12_3p = make_offer(100, "Axe_w12", supplier12_w12_3p, 'Axe_s12_w12_3p_______g', 5)
    axe_s15_w600_ds = make_offer(
        90, "Axe_w600", supplier15_w600_dropship, 'Axe___s15_w600_ds____g', 5, is_fulfillment=False
    )
    msku_axes = MarketSku(
        title="Топоры",
        hyperid=208,
        sku=208,
        blue_offers=[
            axe_s5_w10_3p,
            axe_s12_w12_3p,
            axe_s15_w600_ds,
        ],
        delivery_buckets=[1],
    )

    shop_dsbs = Shop(
        fesh=42,
        datafeed_id=4240,
        priority_region=213,
        regions=[213],
        name='Все мечи',
        client_id=11,
        cpa=Shop.CPA_REAL,
    )

    dsbs_offer = Offer(
        title="Меч dsbs",
        descr='Меч w_cpa с доставкой',
        fesh=shop_dsbs.fesh,
        hyperid=1580,
        waremd5='WhiteCpa____________0g',
        price=1000,
        cpa=Offer.CPA_REAL,
        delivery_buckets=[4240],
    )

    CONSOLIDATE_WITH_SUPPLIER_REPLACE = "consolidate-with-supplier-replace"
    CONSOLIDATE_WITHOUT_CROSSDOCK = "consolidate-without-crossdock"
    FAIL_STRATEGY = "failed-strategy"
    ENABLE_FLAG = 'market_combine_enable_supplier_replace=1;'
    DISABLE_FLAG = 'market_combine_enable_supplier_replace=0;'
    PRICE_DIFF_FLAG = 'macroconsolidate_offers_price_diff={};'
    flags_without_price_diff = '&split-strategy=' + CONSOLIDATE_WITH_SUPPLIER_REPLACE + '&rearr-factors=' + ENABLE_FLAG
    flags_without_default_price_diff = (
        '&split-strategy=' + CONSOLIDATE_WITH_SUPPLIER_REPLACE + '&rearr-factors=' + ENABLE_FLAG + PRICE_DIFF_FLAG
    )
    flags = flags_without_default_price_diff.format(0.2)

    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += [
            'market_nordstream=0',
            'enable_cart_split_on_combinator=0',
        ]
        # НЕ делайте так в новых тестах!
        # Походов в КД на проде уже нет, пожалуйста, проверяйте новую функциональность, создавая доставку через комбинатор
        cls.settings.default_search_experiment_flags += ['force_disable_delivery_calculator_requests=0']

        cls.index.hypertree += [
            HyperCategory(hid=1, name="Чебуреки", output_type=HyperCategoryType.GURU),
            HyperCategory(hid=2, name="Не чебуреки", output_type=HyperCategoryType.GURU),
        ]

        cls.index.models += [
            Model(hid=1, title="Чебурек", hyperid=100),
            Model(hid=2, title="Наковальня", hyperid=200),
            Model(hid=2, title="Резистор", hyperid=201),
            Model(hid=2, title="Plumbus", hyperid=202),
            Model(hid=2, title="Гравицапа", hyperid=203),
            Model(hid=2, title="Сено", hyperid=204),
            Model(hid=2, title="Овес", hyperid=205),
            Model(hid=2, title="Вилки", hyperid=206),
            Model(hid=2, title="Ложка", hyperid=207),
            Model(hid=3, title="Меч", hyperid=1580),
        ]

        cls.index.mskus += [
            T.msku_cheburek,
            T.msku_nakovalnya,
            T.msku_resistor,
            T.msku_gravicapa,
            T.msku_gravicapa2,
            T.msku_seno,
            T.msku_oves,
            T.msku_forks,
            T.msku_spoons,
            T.msku_axes,
        ]

        blue_virtual_shop = Shop(
            fesh=1,
            priority_region=213,
            name='Beru!',
            tax_system=Tax.OSN,
            fulfillment_virtual=True,
            delivery_service_outlets=[1],
            cpa=Shop.CPA_REAL,
            virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
        )

        cls.index.shops += [
            blue_virtual_shop,
            cls.supplier2_w10_1p,
            cls.supplier2_w11_1p,
            cls.supplier3_w10_1p,
            cls.supplier4_w11_1p,
            cls.supplier5_w10_3p,
            cls.supplier9_w300_dropship,
            cls.supplier10_w301_dropship,
            cls.supplier10_w400_dropship,
            cls.supplier15_w600_dropship,
            cls.supplier12_w12_3p,
            cls.supplier13_w500_cnc,
            cls.supplier14_w500_cnc,
            cls.shop_dsbs,
        ]

        cls.index.offers += [
            cls.dsbs_offer,
        ]

        cls.index.regiontree += [
            Region(rid=213),
        ]

        cls.index.outlets += [
            Outlet(
                point_id=1,
                delivery_service_id=103,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(shipper_id=103, day_from=1, day_to=1, order_before=2, price=100),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.12, 55.32),
            ),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=1,
                dc_bucket_id=1,
                fesh=1,
                carriers=[103],
                options=[PickupOption(outlet_id=1, day_from=1, day_to=1, price=100)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            )
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=1,
                dc_bucket_id=1,
                fesh=1,
                carriers=[103],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=5, day_from=1, day_to=2)])],
            ),
            DeliveryBucket(
                bucket_id=4240,
                fesh=cls.shop_dsbs.fesh,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=1)])],
            ),
        ]

        warehouses = [10, 11, 12, 100, 101, 200, 300, 301, 400, 500, 600, 700]

        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [DynamicWarehouseInfo(id=warehouse_id, home_region=213) for warehouse_id in warehouses]
        cls.dynamic.lms += [
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=warehouse_id,
                delivery_service_id=103,
                operation_time=0,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=2, region_to=225)],
            )
            for warehouse_id in warehouses
        ]

        cls.dynamic.lms += [
            DynamicDeliveryServiceInfo(
                103,
                "103",
                region_to_region_info=[DeliveryServiceRegionToRegionInfo(region_from=213, region_to=225, days_key=1)],
            ),
            DynamicWarehouseToWarehouseInfo(warehouse_from=10, warehouse_to=10),
            DynamicWarehouseToWarehouseInfo(warehouse_from=11, warehouse_to=11),
            DynamicWarehouseToWarehouseInfo(warehouse_from=12, warehouse_to=12),
            DynamicWarehouseToWarehouseInfo(warehouse_from=500, warehouse_to=500),
            DynamicWarehouseToWarehouseInfo(warehouse_from=100, warehouse_to=10),
            DynamicWarehouseToWarehouseInfo(warehouse_from=101, warehouse_to=11),
            DynamicWarehouseToWarehouseInfo(warehouse_from=200, warehouse_to=10),
            DynamicWarehouseToWarehouseInfo(warehouse_from=700, warehouse_to=10),
            DynamicWarehouseToWarehouseInfo(warehouse_from=300, warehouse_to=10),
            DynamicWarehouseToWarehouseInfo(warehouse_from=301, warehouse_to=11),
            DynamicWarehouseToWarehouseInfo(warehouse_from=400, warehouse_to=10),
            DynamicWarehouseToWarehouseInfo(warehouse_from=600, warehouse_to=11),
        ]
        cls.index.warehouse_priorities += [
            WarehousesPriorityInRegion(
                regions=[225],
                warehouse_with_priority=[
                    WarehouseWithPriority(10, 1),
                    WarehouseWithPriority(11, 2),
                    WarehouseWithPriority(12, 1),
                ],
            ),
        ]

        for warehouse_id in warehouses:
            cls.delivery_calc.on_request_offer_buckets(
                weight=1, height=10, length=10, width=10, warehouse_id=warehouse_id
            ).respond([1], [1], [])
            cls.delivery_calc.on_request_offer_buckets(
                weight=2, height=22, length=11, width=11, warehouse_id=warehouse_id
            ).respond([1], [1], [])
            cls.delivery_calc.on_request_offer_buckets(
                weight=3, height=22, length=22, width=11, warehouse_id=warehouse_id
            ).respond([1], [1], [])

        cls.index.delivery_calc_feed_info += [
            DeliveryCalcFeedInfo(feed_id=feed_id, warehouse_id=warehouse_id)
            for feed_id, warehouse_id in zip(
                [21, 22, 31, 41, 51, 61, 71, 81, 91, 101, 111, 121, 131, 141, 151, 161],
                [10, 11, 10, 11, 10, 100, 101, 200, 300, 301, 400, 12, 500, 500, 600, 700],
            )
        ]

    def request_combine(self, mskus, offers, rgb, region=213, count=1, flags='', wcpa_offers=[], promos_count=0):
        request = 'place=combine&rgb={}&rids={}&offers-list='.format(rgb, region)
        assert len(mskus) == len(offers), 'len(mskus) == {} is not equal to len(offers) == {}'.format(
            len(mskus), len(offers)
        )

        request_offers = [
            '{}:{};msku:{};cart_item_id:{}'.format(offer.waremd5, count, msku.sku, cart_id)
            + (cart_id < promos_count) * ';promo_type:price;promo_id:hehe'
            for msku, offer, cart_id in zip(mskus, offers, list(range(len(mskus))))
        ]
        if wcpa_offers:
            request_offers += [
                '{}:{};cart_item_id:{}'.format(wcpa_offer.waremd5, count, cart_id)
                for wcpa_offer, cart_id in zip(wcpa_offers, list(range(len(mskus))))
            ]

        if request_offers:
            request += ','.join(request_offers)

        request += flags
        return self.report.request_json(request)

    class BucketCombine(object):
        def __init__(self, shop_id, offers, warehouse_id=Absent()):
            self.shop_id = shop_id
            self.offers = offers
            self.warehouse_id = warehouse_id

    def check_buckets(self, response, strategy_name, buckets, changed_price=False, allow_different_len=False):
        self.assertFragmentIn(
            response,
            {
                "name": strategy_name,
                "priceHasChanged": changed_price,
                "buckets": [
                    {
                        "shopId": bucket.shop_id,
                        "warehouseId": bucket.warehouse_id,
                        "offers": [
                            {
                                "wareId": offer[0].waremd5,
                                "replacedId": offer[1].waremd5 if len(offer) > 1 else offer[0].waremd5,
                                "count": offer[2] if len(offer) > 2 else 1,
                                "cartItemIds": offer[3]
                                if len(offer) > 3
                                else ElementCount(offer[2] if len(offer) > 2 else 1),
                            }
                            for offer in bucket.offers
                        ],
                    }
                    for bucket in buckets
                ],
            },
            allow_different_len=allow_different_len,
        )

    def check_strategy_not_in(self, response, strategy_name):
        self.assertFragmentNotIn(
            response,
            {
                "results": [
                    {
                        "name": strategy_name,
                    }
                ]
            },
        )

    def check_buckets_size(self, response, strategy_name, count, allow_different_len=False):
        self.assertFragmentIn(
            response,
            {"results": [{"name": strategy_name, "buckets": ElementCount(count)}]},
            allow_different_len=allow_different_len,
        )

    def test_1p_1p_consolidate(self):
        """
        Проверяем простую подмену 1p -> 1p
        Мы должны заменить nakovalnya_s4_w11_1p -> nakovalnya_s3_w10_1p c заменой поставщика и увеличением цены на складе 10
        """
        for rgb in ['green_with_blue', 'blue']:
            response = self.request_combine(
                (T.msku_nakovalnya, T.msku_cheburek),
                (T.nakovalnya_s4_w11_1p, T.cheburek_s2_w10_1p),
                rgb,
                region=213,
                count=1,
                flags=T.flags,
            )
            self.check_buckets(
                response,
                self.CONSOLIDATE_WITH_SUPPLIER_REPLACE,
                [
                    T.BucketCombine(
                        shop_id=1,
                        offers=[(T.cheburek_s2_w10_1p,), (T.nakovalnya_s3_w10_1p, T.nakovalnya_s4_w11_1p)],
                        warehouse_id=10,
                    )
                ],
                changed_price=True,
            )

    def test_1p_3p_consolidate(self):
        """
        Проверяем подмену 1p -> 3p
        Мы должны заменить resistor_s4_w11_1p -> resistor_s5_w10_3p c заменой поставщика и уменьшением цены на складе 10
        """
        for rgb in ['green_with_blue', 'blue']:
            response = self.request_combine(
                (T.msku_resistor, T.msku_cheburek),
                (T.resistor_s4_w11_1p, T.cheburek_s2_w10_1p),
                rgb,
                region=213,
                count=1,
                flags=T.flags,
            )
            self.check_buckets(
                response,
                self.CONSOLIDATE_WITH_SUPPLIER_REPLACE,
                [
                    T.BucketCombine(
                        shop_id=1,
                        offers=[(T.cheburek_s2_w10_1p,), (T.resistor_s5_w10_3p, T.resistor_s4_w11_1p)],
                        warehouse_id=10,
                    )
                ],
                changed_price=True,
            )

    def test_flag_disable(self):
        """
        Проверяем что без флага мы не пытаемся подменять офера между поставщиками
        """
        for rgb in ['green_with_blue', 'blue']:
            response = self.request_combine(
                (T.msku_nakovalnya, T.msku_cheburek),
                (T.nakovalnya_s4_w11_1p, T.cheburek_s2_w10_1p),
                rgb,
                region=213,
                count=1,
                flags='&split-strategy=' + T.CONSOLIDATE_WITH_SUPPLIER_REPLACE + '&rearr-factors=' + T.DISABLE_FLAG,
            )
            self.check_buckets_size(response, self.FAIL_STRATEGY, 2)
            self.check_buckets(
                response,
                self.FAIL_STRATEGY,
                [
                    T.BucketCombine(shop_id=1, offers=[(T.nakovalnya_s4_w11_1p,)], warehouse_id=11),
                    T.BucketCombine(shop_id=1, offers=[(T.cheburek_s2_w10_1p,)], warehouse_id=10),
                ],
                changed_price=False,
            )

    def test_no_dropship_consolidate(self):
        """
        Проверяем что мы случайно не считаем консолидацией, собирание оферов дропшипа на складах сортировочного центра
        потому что на самом деле они там не смогут консолидироваться, они там проездом
        """

        # тут может появится соблазн заменить gravicapa_s10_w301_ds на gravicapa_s9_w300_ds чтобы якобы они вместе c
        # gravicapa2_s10_w400_ds поехали вместе через 10-й склад, не поддаемся на него! это все равно 2 разные посылки
        for rgb in ['green_with_blue', 'blue']:
            gravicapa_s10_w301_ds_shop_id = 1
            gravicapa2_s10_w400_ds_shop_id = 1
            response = self.request_combine(
                (T.msku_gravicapa, T.msku_gravicapa2),
                (T.gravicapa_s10_w301_ds, T.gravicapa2_s10_w400_ds),
                rgb,
                region=213,
                count=1,
                flags=T.flags,
            )
            self.check_buckets_size(response, self.CONSOLIDATE_WITH_SUPPLIER_REPLACE, 2)
            self.check_buckets(
                response,
                self.CONSOLIDATE_WITH_SUPPLIER_REPLACE,
                [
                    T.BucketCombine(
                        shop_id=gravicapa_s10_w301_ds_shop_id, offers=[(T.gravicapa_s10_w301_ds,)], warehouse_id=301
                    ),
                    T.BucketCombine(
                        shop_id=gravicapa2_s10_w400_ds_shop_id, offers=[(T.gravicapa2_s10_w400_ds,)], warehouse_id=400
                    ),
                ],
            )

    def test_min_price_wins(self):
        """
        Раз мы меняем поставщика у нас появляется кейс - несоклько оптимальных вариантов разбиения по складам с разной
        итоговой ценой. В этом случае мы берем самый дешевый вариант набора
        """

        # тут у нас 2 офера T.msku_seno на складе w10 и мы должны выбрать либо seno_s3_w10_1p либо
        # seno_s2_w10_1p, но seno_s3_w10_1p таки дешевле на 1 рубль, поэтому выбираем его
        for rgb in ['green_with_blue', 'blue']:
            response = self.request_combine(
                (T.msku_seno,), (T.seno_s2_w10_1p,), rgb, region=213, count=1, flags=T.flags
            )
            self.check_buckets(
                response,
                self.CONSOLIDATE_WITH_SUPPLIER_REPLACE,
                [T.BucketCombine(shop_id=1, offers=[(T.seno_s3_w10_1p, T.seno_s2_w10_1p)], warehouse_id=10)],
                changed_price=True,
            )

        # тут мы запрашиваем T.msku_oves изначально из 10 склада но в наличии есть тот же msku на складе 12, у обоих
        # складов один и тот же приоритет так что выбираем тот склад, где таки дешевле
        for rgb in ['green_with_blue', 'blue']:
            response = self.request_combine(
                (T.msku_oves,), (T.oves_s2_w10_1p,), rgb, region=213, count=1, flags=T.flags
            )
            self.check_buckets(
                response,
                self.CONSOLIDATE_WITH_SUPPLIER_REPLACE,
                [T.BucketCombine(shop_id=1, offers=[(T.oves_s12_w12_3p, T.oves_s2_w10_1p)], warehouse_id=12)],
                changed_price=True,
            )

    def test_click_n_collect(self):
        """
        Проверяем что мы исключаем click-n-collect оффера из подмен
        """

        # тут у нас 2 офера T.msku_forks на складе w10 и w500, пользователь изначально выбрал fork_s13_w500_cnc,
        # НЕ НАДО менять этот офер, т.к. он участвует в программе click-n-collect
        for rgb in ['green_with_blue', 'blue']:
            response = self.request_combine(
                (T.msku_forks,), (T.fork_s13_w500_cnc,), rgb, region=213, count=1, flags=T.flags
            )
            self.check_buckets(
                response,
                self.CONSOLIDATE_WITH_SUPPLIER_REPLACE,
                [T.BucketCombine(shop_id=1, offers=[(T.fork_s13_w500_cnc,)], warehouse_id=500)],
            )

        # проверяем что в обратную сторону (замена НЕ click-n-collect на click-n-collect) тоже не прокатывает
        # у нас изначально 2 офера на складе w10 и складе w11, НЕ консолидируем их на 500 складе!
        for rgb in ['green_with_blue', 'blue']:
            response = self.request_combine(
                (T.msku_forks, T.msku_spoons),
                (T.fork_s2_w10_1p, T.spoon_s2_w11_1p),
                rgb,
                region=213,
                count=1,
                flags=T.flags,
            )
            self.check_buckets_size(response, self.CONSOLIDATE_WITH_SUPPLIER_REPLACE, 2)
            self.check_buckets(
                response,
                self.CONSOLIDATE_WITH_SUPPLIER_REPLACE,
                [
                    T.BucketCombine(shop_id=1, offers=[(T.fork_s2_w10_1p,)], warehouse_id=10),
                    T.BucketCombine(shop_id=1, offers=[(T.spoon_s2_w11_1p,)], warehouse_id=11),
                ],
            )

    def test_required_strategies_list(self):
        split_strategy_flag = "&split-strategy={}"
        allow_strategy_list_flag = "&allow-strategies-list={}"
        enable_supplier_replace_flag = '&rearr-factors=market_combine_enable_supplier_replace={}'
        msku_list = (T.msku_nakovalnya, T.msku_cheburek)
        offers_list = (T.nakovalnya_s4_w11_1p, T.cheburek_s2_w10_1p)

        for rgb in ['green_with_blue', 'blue']:
            for flag in [0, 1]:
                # проверяем, что без указания параметров split-strategy и allow-strategies-list
                # не будет показана стратегия с заменой поставщика независимо от флага market_combine_enable_supplier_replace,
                # так как по умолчанию будут показаны только стратегии без кроссдока и с кроссдоком
                response = self.request_combine(
                    msku_list, offers_list, rgb, region=213, count=1, flags=enable_supplier_replace_flag.format(flag)
                )
                self.assertFragmentNotIn(response, {"results": [{"name": self.CONSOLIDATE_WITH_SUPPLIER_REPLACE}]})
                # проверяем, с указанием split-strategy будет показана стратегия из этого параметра
                response = self.request_combine(
                    msku_list,
                    offers_list,
                    rgb,
                    region=213,
                    count=1,
                    flags=enable_supplier_replace_flag.format(flag)
                    + split_strategy_flag.format(self.CONSOLIDATE_WITH_SUPPLIER_REPLACE),
                )
                if flag:
                    self.assertFragmentIn(response, {"results": [{"name": self.CONSOLIDATE_WITH_SUPPLIER_REPLACE}]})
                else:
                    self.assertFragmentNotIn(response, {"results": [{"name": self.CONSOLIDATE_WITH_SUPPLIER_REPLACE}]})
                    self.assertFragmentIn(response, {"results": [{"name": self.FAIL_STRATEGY}]})
                # проверяем, с указанием в параметре allow-strategies-list стратегии с заменой поставщика
                # она появится в ответе
                response = self.request_combine(
                    msku_list,
                    offers_list,
                    rgb,
                    region=213,
                    count=1,
                    flags=allow_strategy_list_flag.format(self.CONSOLIDATE_WITH_SUPPLIER_REPLACE)
                    + enable_supplier_replace_flag.format(flag),
                )
                if flag:
                    self.assertFragmentIn(response, {"results": [{"name": self.CONSOLIDATE_WITH_SUPPLIER_REPLACE}]})
                else:
                    self.assertFragmentNotIn(response, {"results": [{"name": self.CONSOLIDATE_WITH_SUPPLIER_REPLACE}]})
                    self.assertFragmentIn(response, {"results": [{"name": self.FAIL_STRATEGY}]})

    def test_price_diff(self):
        """
        Проверяем подмены с изменением цены товара в корзине
        """
        # дано: цена nakovalnya_s3_w10_1p - 100 рублей, цена nakovalnya_s4_w11_1p - 90
        # цена nakovalnya_s4_w11_1p - 111,11..% от цены nakovalnya_s3_w10_1p
        offers_list = (T.msku_nakovalnya, T.msku_cheburek)
        msku_list = (T.nakovalnya_s4_w11_1p, T.cheburek_s2_w10_1p)

        # проверяем, что, если флаг не установлен, то дефолтное значение максимального процента изменения
        # цены - 0%, то есть запрещена подмена на товары дороже
        # так как nakovalnya_s4_w11_1p стоит дороже, замены и объединения с cheburek_s2_w10_1p на 10 складе не произойдет
        for rgb in ['green_with_blue', 'blue']:
            response = self.request_combine(
                offers_list, msku_list, rgb, region=213, count=1, flags=T.flags_without_price_diff
            )
            self.check_buckets_size(response, self.CONSOLIDATE_WITH_SUPPLIER_REPLACE, 2)
            self.check_buckets(
                response,
                self.CONSOLIDATE_WITH_SUPPLIER_REPLACE,
                [
                    T.BucketCombine(shop_id=1, offers=[(T.nakovalnya_s4_w11_1p,)], warehouse_id=11),
                    T.BucketCombine(shop_id=1, offers=[(T.cheburek_s2_w10_1p,)], warehouse_id=10),
                ],
                changed_price=False,
            )
        # передаем флагом возможное изменение в цене в 12%, товары объединяются
        for rgb in ['green_with_blue', 'blue']:
            response = self.request_combine(
                offers_list, msku_list, rgb, region=213, count=1, flags=T.flags_without_default_price_diff.format(0.12)
            )
            self.check_buckets(
                response,
                self.CONSOLIDATE_WITH_SUPPLIER_REPLACE,
                [
                    T.BucketCombine(
                        shop_id=1,
                        offers=[(T.cheburek_s2_w10_1p,), (T.nakovalnya_s3_w10_1p, T.nakovalnya_s4_w11_1p)],
                        warehouse_id=10,
                    )
                ],
                changed_price=True,
            )

        # передаем флагом возможное изменение в цене в 11%, товары не объединяются
        for rgb in ['green_with_blue', 'blue']:
            response = self.request_combine(
                offers_list, msku_list, rgb, region=213, count=1, flags=T.flags_without_default_price_diff.format(0.11)
            )
            self.check_buckets_size(response, self.CONSOLIDATE_WITH_SUPPLIER_REPLACE, 2)
            self.check_buckets(
                response,
                self.CONSOLIDATE_WITH_SUPPLIER_REPLACE,
                [
                    T.BucketCombine(shop_id=1, offers=[(T.nakovalnya_s4_w11_1p,)], warehouse_id=11),
                    T.BucketCombine(shop_id=1, offers=[(T.cheburek_s2_w10_1p,)], warehouse_id=10),
                ],
                changed_price=False,
            )

    def test_combine_with_w_cpa(self):
        """
        Проверям, что при наличии белого cpa оффера в корзине ничего не ломается
        """

        _ = 'place=combine&rgb=green_with_blue&rids=213'

        flags = '&allow-strategies-list=' + ','.join(
            [T.CONSOLIDATE_WITH_SUPPLIER_REPLACE, T.CONSOLIDATE_WITHOUT_CROSSDOCK]
        )
        flags += '&rearr-factors=market_combine_enable_supplier_replace=1;macroconsolidate_offers_price_diff=0.05;'
        msku_list = [T.msku_cheburek]
        offers_list = [T.cheburek_s2_w10_1p]
        wcpa_list = [T.dsbs_offer]
        response = self.request_combine(msku_list, offers_list, 'green_with_blue', flags=flags, wcpa_offers=wcpa_list)

        self.check_strategy_not_in(response, T.CONSOLIDATE_WITH_SUPPLIER_REPLACE)

        self.check_buckets_size(response, T.CONSOLIDATE_WITHOUT_CROSSDOCK, 2)
        self.check_buckets(
            response,
            T.CONSOLIDATE_WITHOUT_CROSSDOCK,
            [
                T.BucketCombine(shop_id=1, offers=[(T.cheburek_s2_w10_1p,)], warehouse_id=10),
                T.BucketCombine(shop_id=T.shop_dsbs.fesh, offers=[(T.dsbs_offer,)]),
            ],
            changed_price=False,
        )

    def test_equal_parcels_collapse(self):
        """
        Проверяем, что при подмене товара на тот, что уже есть в корзине, количество второго увеличивается
        А cartItemIds расширяется
        """

        def check_offers_collapsing(
            response,
            offers,
            warehouse_ids,
            pos_collapse_to,
            pos_replaced,
            without_crossdock_count=2,
            changed_price=False,
        ):
            self.check_buckets_size(
                response, T.CONSOLIDATE_WITHOUT_CROSSDOCK, without_crossdock_count, allow_different_len=True
            )
            self.check_buckets_size(response, T.CONSOLIDATE_WITH_SUPPLIER_REPLACE, 1, allow_different_len=True)

            self.check_buckets(
                response,
                T.CONSOLIDATE_WITHOUT_CROSSDOCK,
                [
                    T.BucketCombine(shop_id=1, offers=[(offers[i], offers[i], 1, [i])], warehouse_id=warehouse_ids[i])
                    for i in range(len(offers))
                ],
                changed_price=False,  # в обычной стратегии цена не может измениться
            )

            self.check_buckets(
                response,
                T.CONSOLIDATE_WITH_SUPPLIER_REPLACE,
                [
                    T.BucketCombine(
                        shop_id=1,
                        offers=[(offers[pos_collapse_to], offers[pos_replaced], len(offers), list(range(len(offers))))],
                        warehouse_id=warehouse_ids[pos_collapse_to],
                    )
                ],
                changed_price=changed_price,
            )

        flags = '&allow-strategies-list=' + ','.join(
            [T.CONSOLIDATE_WITHOUT_CROSSDOCK, T.CONSOLIDATE_WITH_SUPPLIER_REPLACE]
        )
        flags += '&rearr-factors=market_combine_enable_supplier_replace=1;macroconsolidate_offers_price_diff=0.05;'
        flags += USE_DEPRECATED_DIRECT_SHIPPING_FLOW

        # Кейс: 3p + 3p - схлопнем
        msku_list = [T.msku_axes, T.msku_axes]
        offers_list = [T.axe_s5_w10_3p, T.axe_s12_w12_3p]
        response = self.request_combine(msku_list, offers_list, 'green_with_blue', flags=flags)
        check_offers_collapsing(
            response, offers_list, warehouse_ids=[10, 12], pos_collapse_to=1, pos_replaced=0, changed_price=True
        )

        # Кейс: Дропшип + 3p - схлопнем
        msku_list = [T.msku_axes, T.msku_axes]
        offers_list = [T.axe_s5_w10_3p, T.axe_s15_w600_ds]

        response = self.request_combine(msku_list, offers_list, 'green_with_blue', flags=flags)
        check_offers_collapsing(
            response, offers_list, warehouse_ids=[10, 600], pos_collapse_to=1, pos_replaced=0, changed_price=True
        )


if __name__ == '__main__':
    main()
