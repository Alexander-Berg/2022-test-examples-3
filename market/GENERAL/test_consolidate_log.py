#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa


from core.logs import ConsolidateLogFrontend, LogBrokerTopicBackend
from core.types import (
    BlueOffer,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DeliveryServiceRegionToRegionInfo,
    DynamicDaysSet,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    GpsCoord,
    MarketSku,
    OfferDimensions,
    Outlet,
    OutletDeliveryOption,
    PickupBucket,
    PickupOption,
    Region,
    RegionalDelivery,
    Shop,
    Tax,
    Vat,
    WarehouseWithPriority,
    WarehousesPriorityInRegion,
)
from core.testcase import TestCase, main
from core.logbroker import LogBrokerClient, CONSOLIDATE_TOPIC


class _Shops(object):
    blue_virtual_shop = Shop(
        fesh=1,
        priority_region=213,
        name='Beru!',
        tax_system=Tax.OSN,
        fulfillment_virtual=True,
        delivery_service_outlets=[2001],
        cpa=Shop.CPA_REAL,
        virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
    )

    red_virtual_shop = Shop(
        fesh=101,
        name='red virtual_shop',
        fulfillment_virtual=True,
        virtual_shop_color=Shop.VIRTUAL_SHOP_RED,
        cpa=Shop.CPA_REAL,
    )

    DROPSHIP_SUPPLIER_ID = 4

    def make_supplier(supplier_id, feed_id, name, warehouse, is_fulfillment=True):
        return Shop(
            fesh=supplier_id,
            datafeed_id=feed_id,
            priority_region=2,
            name=name,
            tax_system=Tax.OSN,
            supplier_type=Shop.FIRST_PARTY,
            blue=Shop.BLUE_REAL,
            cpa=Shop.CPA_REAL,
            warehouse_id=warehouse,
            fulfillment_program=is_fulfillment,
        )

    blue_supplier_145 = make_supplier(2, 21, 'shop1_priority_1', 145)
    blue_supplier_148 = make_supplier(2, 23, 'warehouse_disabled_in_lms', 148)
    blue_supplier_149 = make_supplier(2, 24, 'shop1_priority_3', 149)

    dropship_444 = make_supplier(DROPSHIP_SUPPLIER_ID, 4, 'dropship at warehouse 444', 444, is_fulfillment=False)
    dropship_555 = make_supplier(DROPSHIP_SUPPLIER_ID, 5, 'dropship at warehouse 555', 555, is_fulfillment=False)


class _Offers(object):
    def make_offer(price, shop_sku, feed_id, ware_md5, stock_store_count):
        return BlueOffer(
            price=price,
            vat=Vat.VAT_10,
            feedid=feed_id,
            offerid=shop_sku,
            waremd5=ware_md5,
            stock_store_count=stock_store_count,
            weight=75,
            dimensions=OfferDimensions(length=80, width=60, height=220),
        )

    alyonka_s1_145 = make_offer(50, 'Alyonka_145', 21, 'Alyonka_s1_145_______g', 15)
    alyonka_s1_149 = make_offer(51, 'Alyonka_149', 24, 'Alyonka_s1_149_______g', 18)

    coke_s1_145 = make_offer(34, 'Coke_145_1', 21, 'CocaCola_s1_145______g', 8)
    coke_s1_148 = make_offer(35, 'Coke_148_1', 23, 'CocaCola_s1_148______g', 25)

    def make_dropship_offer(price, shop_sku, feed_id, ware_md5, stock_store_count):
        return BlueOffer(
            price=price,
            vat=Vat.VAT_20,
            feedid=feed_id,
            offerid=shop_sku,
            waremd5=ware_md5,
            is_fulfillment=False,
            supplier_id=_Shops.DROPSHIP_SUPPLIER_ID,
            weight=75,
            dimensions=OfferDimensions(length=80, width=60, height=220),
            stock_store_count=stock_store_count,
        )

    hoover_444_offer = make_dropship_offer(14990, 'Hoover_444', 4, 'VacuumCleaner_444____g', 10)
    hoover_555_offer = make_dropship_offer(15000, 'Hoover_555', 5, 'VacuumCleaner_555____g', 24)

    fridge_444_offer = make_dropship_offer(35000, 'Fridge_444', 4, 'Refrigerator_444_____g', 7)
    fridge_555_offer = make_dropship_offer(35000, 'Fridge_555', 5, 'Refrigerator_555_____g', 8)

    microwave_555_offer = make_dropship_offer(12990, 'Microwave_555', 5, 'MicrowaveOven_555____g', 17)


class _MSKUs(object):
    alyonka = MarketSku(
        title="Alyonka",
        hyperid=1,
        sku=1,
        blue_offers=[_Offers.alyonka_s1_145, _Offers.alyonka_s1_149],
        delivery_buckets=[801, 802],
    )

    coke = MarketSku(
        title="Coca Cola",
        hyperid=2,
        sku=2,
        blue_offers=[_Offers.coke_s1_145, _Offers.coke_s1_148],
        delivery_buckets=[801, 802],
    )
    fridge = MarketSku(
        title="Refrigerator",
        hyperid=101,
        sku=101,
        blue_offers=[_Offers.fridge_444_offer, _Offers.fridge_555_offer],
        delivery_buckets=[803, 804],
    )

    hoover = MarketSku(
        title="Vacuum cleaner",
        hyperid=3,
        sku=3,
        blue_offers=[_Offers.hoover_444_offer, _Offers.hoover_555_offer],
        delivery_buckets=[802, 803, 804, 805],
    )

    microwave = MarketSku(
        title="Microwave oven", hyperid=4, sku=4, blue_offers=[_Offers.microwave_555_offer], delivery_buckets=[804]
    )


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.logbroker_enabled = True
        # НЕ делайте так в новых тестах!
        # Походов в КД на проде уже нет, пожалуйста, проверяйте новую функциональность, создавая доставку через комбинатор
        cls.settings.default_search_experiment_flags += ['force_disable_delivery_calculator_requests=0']
        cls.settings.direct_sending_consolidate_log_to_logbroker = True
        cls.index.regiontree += [
            Region(rid=213),
            Region(rid=111, region_type=Region.COUNTRY),  # no delivery to this region
            Region(rid=444, region_type=Region.COUNTRY),  # delivery only from warehouse 444
        ]

        cls.index.outlets += [
            Outlet(
                point_id=2001,
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
                bucket_id=5001,
                dc_bucket_id=5001,
                fesh=1,
                carriers=[103],
                options=[PickupOption(outlet_id=2001, day_from=1, day_to=1, price=100)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            )
        ]

        def delivery_service_region_to_region_info(region_from=213, region_to=225):
            return DeliveryServiceRegionToRegionInfo(region_from=region_from, region_to=region_to, days_key=1)

        def link_warehouse_delivery_service(warehouse_id, delivery_service_id, region_to=225):
            return DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=warehouse_id,
                delivery_service_id=delivery_service_id,
                operation_time=0,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=2, region_to=region_to)],
            )

        cls.settings.lms_autogenerate = False

        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=213, holidays_days_set_key=1),
            DynamicWarehouseInfo(id=148, home_region=213, holidays_days_set_key=1),
            DynamicWarehouseInfo(id=149, home_region=213, holidays_days_set_key=1),
            DynamicWarehouseInfo(id=444, home_region=213),
            DynamicWarehouseInfo(id=555, home_region=213),
            DynamicDeliveryServiceInfo(
                99, "self-delivery", region_to_region_info=[delivery_service_region_to_region_info()]
            ),
            DynamicDeliveryServiceInfo(103, "c_103", region_to_region_info=[delivery_service_region_to_region_info()]),
            DynamicDeliveryServiceInfo(
                111, "courier", region_to_region_info=[delivery_service_region_to_region_info()]
            ),
            DynamicDeliveryServiceInfo(
                165,
                "dropship_delivery",
                region_to_region_info=[
                    delivery_service_region_to_region_info(213, 225),
                    delivery_service_region_to_region_info(213, 444),
                ],
            ),
            DynamicDaysSet(key=1, days=[]),
            link_warehouse_delivery_service(warehouse_id=145, delivery_service_id=103),
            link_warehouse_delivery_service(warehouse_id=148, delivery_service_id=111),
            link_warehouse_delivery_service(warehouse_id=149, delivery_service_id=111),
            link_warehouse_delivery_service(warehouse_id=444, delivery_service_id=99),
            link_warehouse_delivery_service(warehouse_id=444, delivery_service_id=165, region_to=444),
            link_warehouse_delivery_service(warehouse_id=555, delivery_service_id=165),
        ]
        cls.index.shops += [
            _Shops.blue_virtual_shop,
            _Shops.red_virtual_shop,
            _Shops.blue_supplier_145,
            _Shops.blue_supplier_148,
            _Shops.blue_supplier_149,
            _Shops.dropship_444,
            _Shops.dropship_555,
        ]

        cls.index.mskus += [_MSKUs.alyonka, _MSKUs.coke, _MSKUs.fridge, _MSKUs.hoover, _MSKUs.microwave]

        std_options = [RegionalDelivery(rid=213, options=[DeliveryOption(price=5, day_from=1, day_to=2)])]
        cls.index.delivery_buckets += [
            DeliveryBucket(bucket_id=801, dc_bucket_id=801, fesh=1, carriers=[103], regional_options=std_options),
            DeliveryBucket(
                bucket_id=802,
                dc_bucket_id=802,
                fesh=1,
                carriers=[111],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=std_options,
            ),
            DeliveryBucket(
                bucket_id=803,
                dc_bucket_id=803,
                fesh=4,
                carriers=[99],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=std_options,
            ),
            DeliveryBucket(
                bucket_id=804,
                dc_bucket_id=804,
                fesh=4,
                carriers=[165],
                delivery_program=DeliveryBucket.BERU_CROSSDOCK,
                regional_options=std_options,
            ),
            DeliveryBucket(
                bucket_id=805,
                dc_bucket_id=805,
                fesh=4,
                carriers=[165],
                delivery_program=DeliveryBucket.BERU_CROSSDOCK,
                regional_options=[
                    RegionalDelivery(rid=444, options=[DeliveryOption(price=50, day_from=1, day_to=2)]),
                    RegionalDelivery(rid=2, options=[DeliveryOption(price=50, day_from=1, day_to=2)]),
                ],
            ),
        ]
        cls.index.warehouse_priorities += [
            WarehousesPriorityInRegion(
                regions=[225],
                warehouse_with_priority=[
                    WarehouseWithPriority(444, 1),
                    WarehouseWithPriority(555, 2),
                    WarehouseWithPriority(145, 3),
                    WarehouseWithPriority(147, 4),
                    WarehouseWithPriority(149, 5),
                ],
            ),
            WarehousesPriorityInRegion(regions=[444], warehouse_with_priority=[WarehouseWithPriority(444, 1)]),
            WarehousesPriorityInRegion(
                regions=[2],
                warehouse_with_priority=[
                    WarehouseWithPriority(555, 1),
                ],
            ),
        ]

        cls.delivery_calc.on_request_offer_buckets(
            length=80, width=60, height=220, weight=75, warehouse_id=145
        ).respond([801, 802, 803, 804, 805], [5001], [])
        cls.delivery_calc.on_request_offer_buckets(
            length=80, width=60, height=220, weight=75, warehouse_id=148
        ).respond([801, 802, 803, 804, 805], [5001], [])
        cls.delivery_calc.on_request_offer_buckets(
            length=80, width=60, height=220, weight=75, warehouse_id=149
        ).respond([801, 802, 803, 804, 805], [5001], [])
        cls.delivery_calc.on_request_offer_buckets(
            length=80, width=60, height=220, weight=75, warehouse_id=444
        ).respond([801, 802, 803, 804, 805], [5001], [])
        cls.delivery_calc.on_request_offer_buckets(
            length=80, width=60, height=220, weight=75, warehouse_id=555
        ).respond([801, 802, 803, 804, 805], [5001], [])

    def request_combine(
        self, mskus, offers, region=213, count=1, promo_type='', promo_id='3AdET44sf4SeBq2vk7dOKCz', flags=''
    ):
        request = 'place=combine&uid=123456789&uid-type=yandexuid&rgb=blue&rids={region}'.format(region=region)
        assert len(mskus) == len(offers)

        promo_type_string = ';promo_type:{};promo_id:{}'.format(promo_type, promo_id) if promo_type else ''
        request_offers = [
            '{}:{};msku:{}'.format(offer.waremd5, count, msku.sku) + promo_type_string
            for msku, offer in zip(mskus, offers)
        ]
        if request_offers:
            request += '&offers-list=' + ','.join(request_offers)
        request += '&rearr-factors=partial_replacement=1'
        request += flags
        return self.report.request_json(request)

    def check_offer_in_response(self, response, msku, offer):
        self.assertFragmentIn(
            response, {"offers": {"items": [{"entity": "offer", "marketSku": msku.sku, "wareId": offer.waremd5}]}}
        )

    def check_total_warehouses(self, response, expected):
        self.assertFragmentIn(response, {"search": {"totalWarehouses": expected}})

    def get_consolidate_log(self):
        consolidate_log = ConsolidateLogFrontend("consolidate_log")
        consolidate_log.bind(LogBrokerTopicBackend(self.logbroker, CONSOLIDATE_TOPIC, LogBrokerClient.CODEC_GZIP))
        consolidate_log.dump("*")
        return consolidate_log

    def parse_tskv(self, line):
        parsed = {}
        line_parts = line.strip().split('\t')
        format_marker = line_parts.pop(0)
        assert format_marker == 'tskv'

        for kv in line_parts:
            key, value = kv.split('=', 1)
            if value == 'true':
                value = True
            elif value == 'false':
                value = False
            parsed[key] = value
        return parsed

    def check_field(self, data, field_name, may_values):
        assert field_name in data

        assert data[field_name] in may_values, "field {} has value = {} but expected one of {}".format(
            field_name, data[field_name], may_values
        )

    def check_consolidation_flag(self, response, flag):
        self.assertFragmentIn(response, {"search": {"replaced": flag}})

    def test_simple_trace(self):
        '''
        Проверяем формат записи через LogBroker
        '''
        response = self.request_combine(
            (_MSKUs.alyonka, _MSKUs.coke),
            (_Offers.alyonka_s1_149, _Offers.coke_s1_148),
            flags='&rearr-factors=enable_cart_split_on_combinator=0',
        )
        self.check_offer_in_response(response, _MSKUs.alyonka, _Offers.alyonka_s1_145)
        self.check_offer_in_response(response, _MSKUs.coke, _Offers.coke_s1_145)

        parsed_lines = (self.parse_tskv(line) for line in self.get_consolidate_log().backend)
        parsed_tskv = next(parsed_lines)

        self.check_field(parsed_tskv, 'tskv_format', ["market-report-consolidate-log"])
        self.check_field(parsed_tskv, 'consolidated', [True])
        self.check_field(
            parsed_tskv, 'offer_list', ["{},{}".format(_Offers.coke_s1_148.waremd5, _Offers.alyonka_s1_149.waremd5)]
        )
        self.check_field(
            parsed_tskv, 'bucket_out', ["{},{}".format(_Offers.coke_s1_145.waremd5, _Offers.alyonka_s1_145.waremd5)]
        )
        self.check_field(parsed_tskv, 'warehouses_from', ["148,149"])
        self.check_field(parsed_tskv, 'warehouse', ["145"])
        self.check_field(parsed_tskv, 'uid-type', ['yandexuid'])
        self.check_field(parsed_tskv, 'uid', ['123456789'])

    def test_trace_with_warehouses(self):
        """
        Проверяем, что если мы подменили на несколько складов, то количество строк совпадает с количеством складов
        """
        response = self.request_combine(
            (_MSKUs.alyonka, _MSKUs.coke, _MSKUs.hoover, _MSKUs.fridge, _MSKUs.microwave),
            (
                _Offers.alyonka_s1_149,
                _Offers.coke_s1_148,
                _Offers.hoover_444_offer,
                _Offers.fridge_444_offer,
                _Offers.microwave_555_offer,
            ),
            flags='&rearr-factors=enable_cart_split_on_combinator=0',
        )
        self.check_offer_in_response(response, _MSKUs.alyonka, _Offers.alyonka_s1_145)
        self.check_offer_in_response(response, _MSKUs.coke, _Offers.coke_s1_145)
        self.check_offer_in_response(response, _MSKUs.hoover, _Offers.hoover_444_offer)
        self.check_offer_in_response(response, _MSKUs.fridge, _Offers.fridge_444_offer)
        self.check_offer_in_response(response, _MSKUs.microwave, _Offers.microwave_555_offer)

        for line in self.get_consolidate_log().backend:
            parsed_tskv = self.parse_tskv(line)
            self.check_field(parsed_tskv, 'consolidated', [True])
            self.check_field(
                parsed_tskv,
                'bucket_out',
                [
                    _Offers.microwave_555_offer.waremd5,
                    "{},{}".format(_Offers.fridge_444_offer.waremd5, _Offers.hoover_444_offer.waremd5),
                    "{},{}".format(_Offers.coke_s1_145.waremd5, _Offers.alyonka_s1_145.waremd5),
                ],
            )
            self.check_field(parsed_tskv, 'warehouse', ["555", "444", "145"])
            self.check_field(
                parsed_tskv,
                'offer_list',
                [
                    _Offers.microwave_555_offer.waremd5,
                    "{},{}".format(_Offers.fridge_444_offer.waremd5, _Offers.hoover_444_offer.waremd5),
                    "{},{}".format(_Offers.coke_s1_148.waremd5, _Offers.alyonka_s1_149.waremd5),
                ],
            )
            self.check_field(parsed_tskv, 'warehouses_from', ["555", "444,444", "148,149"])

    def test_exclude_offer_log(self):
        """
        Проверяем, что причина исключения оффера из подмены логируется
        """
        EXCLUDE_PROMO_FLAG = '&rearr-factors=exclude_promo_in_consolidation=1;enable_cart_split_on_combinator=0'

        _ = self.request_combine(
            (_MSKUs.alyonka, _MSKUs.coke),
            (_Offers.alyonka_s1_149, _Offers.coke_s1_148),
            promo_type='blue-cashback',
            flags=EXCLUDE_PROMO_FLAG,
        )

        parsed_lines = (self.parse_tskv(line) for line in self.get_consolidate_log().backend)
        parsed_tskv = next(parsed_lines)
        self.check_field(parsed_tskv, 'consolidated', [True])
        self.check_field(
            parsed_tskv,
            'excluded_offers_with_reason',
            [
                "{}:{}".format(_Offers.alyonka_s1_149.waremd5, 'EXCLUDE_PROMO_IN_CONSOLIDATION'),
                "{}:{}".format(_Offers.coke_s1_148.waremd5, 'EXCLUDE_PROMO_IN_CONSOLIDATION'),
            ],
        )


if __name__ == '__main__':
    main()
