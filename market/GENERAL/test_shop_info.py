#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.cpc import Cpc
from core.testcase import TestCase, main
from core.types import (
    AboShopRating,
    BlueOffer,
    ClickType,
    Currency,
    DeliveryBucket,
    DeliveryCalcFeedInfo,
    DeliveryOption,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    ExchangeRate,
    HyperCategory,
    MarketSku,
    NewShopRating,
    Offer,
    Outlet,
    OutletDeliveryOption,
    PickupBucket,
    PickupOption,
    PrescriptionManagementSystem,
    Region,
    RegionalDelivery,
    Shop,
    ShopOperationalRating,
    LogosInfo,
    UrlType,
    Vendor,
)
from core.matcher import Absent, ElementCount, NoKey, Wildcard
from core.types.delivery import OutletWorkingTime
from core.types.express_partners import EatsWarehousesEncoder
from core.types.dynamic_filters import TimeInfo, TimeIntervalInfo, DynamicTimeIntervalsSet
from core.types.combinator import CombinatorGpsCoords, CombinatorExpressWarehouse


def make_mock_rearr(**kwds):
    suffix = 'parallel_smm=1.0;ext_snippet=1;no_snippet_arc=1;market_enable_sins_offers_wizard=1'
    rearr = make_rearr(**kwds)
    if rearr != '':
        rearr += ';'
    return rearr + suffix


def make_rearr(**kwds):
    kvlist = ['{}={}'.format(key, kwds[key]) for key in kwds]
    kvlist.sort(key=lambda x: x[0])
    return ';'.join(kvlist)


class _Gps1:
    _lat = 15.1234
    _lon = 13.4321

    location_combinator = CombinatorGpsCoords(_lat, _lon)
    location_str = 'lat:{lat};lon:{lon}'.format(lat=_lat, lon=_lon)


class _Gps2:
    _lat = 1.0
    _lon = 1.0

    location_combinator = CombinatorGpsCoords(_lat, _lon)
    location_str = 'lat:{lat};lon:{lon}'.format(lat=_lat, lon=_lon)


USE_DEPRECATED_DIRECT_SHIPPING_FLOW = "&rearr-factors=get_rid_of_direct_shipping=0"


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        # RANDX randomizing is disabled because these tests don't work with it. See MARKETOUT-21319
        cls.disable_randx_randomize()
        cls.index.currencies = [
            Currency(
                name=Currency.USD,
                exchange_rates=[
                    ExchangeRate(to=Currency.RUR, rate=60.0),
                    ExchangeRate(to=Currency.BYN, rate=2.2),
                ],
            ),
            Currency(
                name=Currency.BYN,
                exchange_rates=[
                    ExchangeRate(to=Currency.RUR, rate=25),
                ],
            ),
        ]

        cls.index.regiontree += [
            Region(rid=213, name='Москва', genitive='Москвы', preposition='в ', accusative='Москву', tz_offset=10800),
            Region(rid=2, name='Санкт-Петербург', tz_offset=10800),
            Region(rid=10758, name='Химки', tz_offset=10800),
            Region(rid=157, name='Минск', tz_offset=10800),
            Region(
                rid=134,
                name='Китай',
                tz_offset=28800,
                region_type=Region.COUNTRY,
                genitive="Китая",
                preposition="в",
                accusative="Китай",
                children=[
                    Region(
                        rid=10590, name='Пекин', genitive="Пекина", preposition="в", accusative="Пекин", tz_offset=28800
                    ),
                ],
            ),
        ]

        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=3, home_region=213),
            DynamicWarehouseInfo(id=4, home_region=213),
            DynamicWarehouseInfo(id=7, home_region=213),
            DynamicWarehouseToWarehouseInfo(warehouse_from=3, warehouse_to=3),
            DynamicWarehouseToWarehouseInfo(warehouse_from=4, warehouse_to=4),
            DynamicWarehouseToWarehouseInfo(warehouse_from=7, warehouse_to=7),
        ]

    @classmethod
    def prepare_shop_info(cls):
        """
        1151901 - магазин без указания валюты (в LITE => RUR)
        1151902 - магазин с валютой USD,
        1151903 - магазин с указанной валютой доставки, совпадающей с валютой магазина,
        1151904 - магазин с указанной валютой доставки, не совпадающей с валютой магазина
        """
        cls.index.shops += [
            Shop(
                fesh=1151901,
                shop_logo_url='avatars.mds.yandex.net/1151901/small',
                shop_logo_retina_url='avatars.mds.yandex.net/1151901/orig',
            ),
            Shop(fesh=1151902, shop_logo_url='avatars.mds.yandex.net/1151902/small', currency=Currency.USD),
            Shop(fesh=1151903, currency=Currency.RUR, delivery_currency=Currency.RUR),
            Shop(fesh=1151904, currency=Currency.RUR, delivery_currency=Currency.USD),
            Shop(fesh=1151907),
        ]

    def test_invalid_user_cgi(self):
        """
        Запрос без параметров ведёт к ошибке
        """
        response = self.report.request_json('place=shop_info')
        self.assertFragmentIn(response, {"error": {"code": "INVALID_USER_CGI"}})
        self.error_log.expect(code=3043)

    def test_unknown_shop(self):
        """
        Запрос неизвестного магазина
        """
        response = self.report.request_json('place=shop_info&fesh=1234567')
        self.assertEqual(0, response.count({"entity": "shop"}))

    def test_one_fesh(self):
        """
        Запрос одного магазина
        """
        response = self.report.request_json('place=shop_info&fesh=1151901')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "shop",
                        "id": 1151901,
                        "shopCurrency": "RUR",
                    }
                ]
            },
        )

    def test_two_feshes(self):
        """
        Запрос нескольких магазинов
        """
        response = self.report.request_json('place=shop_info&fesh=1151901&fesh=1151902')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "shop",
                        "id": 1151901,
                        "shopCurrency": "RUR",
                        "logo": "avatars.mds.yandex.net/1151901/orig",
                    },
                    {
                        "entity": "shop",
                        "id": 1151902,
                        "shopCurrency": "USD",
                        "logo": "avatars.mds.yandex.net/1151902/small",
                    },
                ]
            },
        )

    def test_delivery_currency_not_specified_RUR(self):
        """
        Запрос рублёвого магазина, где не указан delivery_currency
        """
        response = self.report.request_json('place=shop_info&fesh=1151901')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "shop",
                        "id": 1151901,
                        "shopCurrency": "RUR",
                        "deliveryCurrency": "RUR",
                    },
                ]
            },
        )

    def test_delivery_currency_not_specified_USD(self):
        """
        Запрос долларового магазина, где не указан delivery_currency
        """
        response = self.report.request_json('place=shop_info&fesh=1151902')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "shop", "id": 1151902, "shopCurrency": "USD", "deliveryCurrency": "USD"},
                ]
            },
        )

    def test_delivery_currency_specified_and_equal_to_shop_currency(self):
        """
        Запрос долларового магазина, где delivery_currency указан и равен валюте магазина
        """
        response = self.report.request_json('place=shop_info&fesh=1151903')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "shop",
                        "id": 1151903,
                        "shopCurrency": "RUR",
                        "deliveryCurrency": "RUR",
                        "logo": Absent(),
                    },
                ]
            },
        )

    def test_delivery_currency_specified_and_not_equal_to_shop_currency(self):
        """
        Запрос долларового магазина, где delivery_currency указан и не равен валюте магазина
        """
        response = self.report.request_json('place=shop_info&fesh=1151904')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "shop",
                        "id": 1151904,
                        "shopCurrency": "RUR",
                        "deliveryCurrency": "USD",
                        "logo": Absent(),
                    },
                ]
            },
        )

    @classmethod
    def prepare_return_delivery_address(cls):
        """Магазины с/без аттрибута return_delivery_address"""
        cls.index.hypertree += [HyperCategory(hid=1234, goods_return_policy="return policy")]

        cls.index.shops += [
            Shop(fesh=1001, priority_region=213, return_delivery_address="return address", cpa=Shop.CPA_REAL),
            Shop(fesh=1002, priority_region=213, return_delivery_address="return address", cpa=Shop.CPA_REAL),
            Shop(fesh=1003, priority_region=213, cpa=Shop.CPA_REAL),
        ]

        cls.index.offers += [
            Offer(fesh=1001, hyperid=111, waremd5="Gapsb7S6n8856JP_H9W1vQ"),
            Offer(fesh=1002, hid=1234, hyperid=222, waremd5="UqjtaoHIiFbiBqp0dZpBAw", cpa=Offer.CPA_REAL),
            Offer(fesh=1003, hid=1234, hyperid=333, waremd5="otENNVzevIeeT8bsxvY91w", cpa=Offer.CPA_REAL),
            Offer(fesh=1002, hid=1234, hyperid=444, waremd5="qaerG3FLBO_yXOXngxNZAg", cpa=Offer.CPA_NO),
        ]

    def check_json_return_delivery_address(self, response, address, policy):
        self.assertFragmentIn(
            response, {"results": [{"shop": {"returnDeliveryAddress": address}, "returnPolicy": policy}]}
        )

    def test_return_delivery_address(self):
        """Проверяется, что аттрибут return_delivery_address"""
        """    1) Для магазина опциональный"""
        """    2) Выводится только тогда, когда это CPA оффер и в категории оффера есть goods_return_policy, и как часть информации"""
        """        - об оффере для плейсов prime, offerinfo"""
        """        - о магазине для плейса shop_info"""
        """Выводится аттрибут returnPolicy и он опциональный"""

        returnDelvieryAddress = "return address"
        NO_returnDelvieryAddress = NoKey("returnDeliveryAddress")
        goodsReturnPolicy = "return policy"
        NO_goodsReturnPolicy = NoKey("returnPolicy")

        # prime, productoffers
        for place_name in ['prime', 'productoffers']:
            response = self.report.request_json(('place={0}&hyperid=111').format(place_name))
            self.check_json_return_delivery_address(response, NO_returnDelvieryAddress, NO_goodsReturnPolicy)

            response = self.report.request_json(('place={0}&hyperid=222').format(place_name))
            self.check_json_return_delivery_address(response, returnDelvieryAddress, goodsReturnPolicy)

            for hyperid in ["333", "444"]:
                response = self.report.request_json(('place={0}&hyperid={1}').format(place_name, hyperid))
                self.check_json_return_delivery_address(response, NO_returnDelvieryAddress, goodsReturnPolicy)

            # offerinfo
        response = self.report.request_json(
            'place=offerinfo&offerid=Gapsb7S6n8856JP_H9W1vQ&show-urls=encrypted&rids=213&regset=1'
        )
        self.check_json_return_delivery_address(response, NO_returnDelvieryAddress, NO_goodsReturnPolicy)

        response = self.report.request_json(
            'place=offerinfo&offerid=UqjtaoHIiFbiBqp0dZpBAw&show-urls=encrypted&rids=213&regset=1'
        )
        self.check_json_return_delivery_address(response, returnDelvieryAddress, goodsReturnPolicy)

        for offerid in ["otENNVzevIeeT8bsxvY91w", "qaerG3FLBO_yXOXngxNZAg"]:
            response = self.report.request_json(
                ('place=offerinfo&offerid={0}&show-urls=encrypted&rids=213&regset=1').format(offerid)
            )
            self.check_json_return_delivery_address(response, NO_returnDelvieryAddress, goodsReturnPolicy)

            # shop_info
        response = self.report.request_json('place=shop_info&fesh=1001')
        self.assertFragmentIn(response, {"returnDeliveryAddress": NO_returnDelvieryAddress})

        response = self.report.request_json('place=shop_info&fesh=1002')
        self.assertFragmentIn(response, {"returnDeliveryAddress": returnDelvieryAddress})

    @classmethod
    def prepare_tab_in_value(cls):
        """
        Подготовка данных о магазинах с символами табуляции в значении поля return_delivery_address
        """
        cls.index.shops += [
            Shop(fesh=10000, priority_region=213, return_delivery_address="return\taddress", cpa=Shop.CPA_REAL),
        ]

    def test_tab_in_value(self):
        """
        Проверка корректности работы в случае,
        когда значение поля return_delivery_address содержало символы табуляции
        (при парсинге должны быть заменены пробелами)

        тикет MARKETOUT-12128.
        """

        # проверяем, что по запросу с соответсвующим id магазина,
        # поле returnDeliveryAddress содержит строку из подготовленных данных,
        # но с табом, замененным на пробел
        # и неявно проверям, что репорт коррекно загрузился, когда в файле shops.dat
        # есть строки с табами

        response = self.report.request_json('place=shop_info&fesh=10000')
        self.assertFragmentIn(response, {"returnDeliveryAddress": "return address"})

    @classmethod
    def prepare_shopsdat_cpc(cls):
        """
        Подготовка магазинов с разными CPC
        """
        cls.index.shops += [
            Shop(fesh=1259901, cpc=Shop.CPC_REAL),
            Shop(fesh=1259902, cpc=Shop.CPC_SANDBOX),
            Shop(fesh=1259903, cpc=Shop.CPC_NO),
            Shop(fesh=1259904),
        ]

    def test_shopsdat_cpc_real(self):
        """
        Просто тестируем выдачу CPC
        """
        response = self.report.request_json('place=shop_info&fesh=1259901')
        self.assertFragmentIn(
            response,
            {
                'entity': 'shop',
                'id': 1259901,
                'cpc': {'shopsDat': 'real'},
            },
        )

    def test_shopsdat_cpc_sbx(self):
        """
        Просто тестируем выдачу CPC
        """
        response = self.report.request_json('place=shop_info&fesh=1259902')
        self.assertFragmentIn(
            response,
            {
                'entity': 'shop',
                'id': 1259902,
                'cpc': {'shopsDat': 'sandbox'},
            },
        )

    def test_shopsdat_cpc_no(self):
        """
        Просто тестируем выдачу CPC
        """
        response = self.report.request_json('place=shop_info&fesh=1259903')
        self.assertFragmentIn(
            response,
            {
                'entity': 'shop',
                'id': 1259903,
                'cpc': {'shopsDat': 'no'},
            },
        )

    def test_shopsdat_cpc_not_specified(self):
        """
        Просто тестируем выдачу CPC
        """
        response = self.report.request_json('place=shop_info&fesh=1259904')
        self.assertFragmentIn(
            response,
            {
                'entity': 'shop',
                'id': 1259904,
                'cpc': {'shopsDat': 'real'},
            },
        )

    # MARKETOUT-13092
    @classmethod
    def prepare_global_shop(cls):
        cls.index.shops += [
            Shop(fesh=1309201, is_global=True, cpa=Shop.CPA_REAL, priority_region=213),
            Shop(
                fesh=1309202,
                is_global=True,
                cpa=Shop.CPA_REAL,
                return_delivery_address="return address",
                priority_region=213,
            ),
        ]

        cls.index.hypertree += [HyperCategory(hid=1309202, goods_return_policy="some return policy")]

        cls.index.offers += [
            Offer(fesh=1309201, hyperid=1309201, cpa=Offer.CPA_REAL, waremd5="mysuperwaremd5_1309201"),
            Offer(fesh=1309202, hid=1309202, hyperid=1309202, cpa=Offer.CPA_REAL, waremd5="mysuperwaremd5_1309202"),
        ]

    def test_global_return_policy_if_empty(self):
        """
        Проверяем, что значение поля returnPolicy == "global", даже если для hid не установлена политика возврата
        """
        response = self.report.request_json(
            'place=offerinfo&offerid=mysuperwaremd5_1309201&show-urls=encrypted&rids=213&regset=1'
        )
        self.assertFragmentIn(response, {"entity": "offer", "model": {"id": 1309202}, "returnPolicy": "global"})

    def test_global_return_policy_if_empty_2(self):
        """
        Проверяем, что значение поля returnPolicy == "global", даже если для hid явно установлена любая другая политика возврата
        """
        response = self.report.request_json(
            'place=offerinfo&offerid=mysuperwaremd5_1309202&show-urls=encrypted&rids=213&regset=1'
        )
        self.assertFragmentIn(response, {"entity": "offer", "model": {"id": 1309202}, "returnPolicy": "global"})

    @classmethod
    def prepare_outlet_bool_props_and_work_time(cls):
        '''Создаются аутлеты с булевыми свойствами A, prop_2, C'''
        cls.index.shops += [
            Shop(
                fesh=112233, datafeed_id=112233, priority_region=213, delivery_service_outlets=[400], cpa=Shop.CPA_REAL
            ),
            Shop(fesh=112235, priority_region=213, delivery_service_outlets=[400], cpa=Shop.CPA_REAL),
            Shop(fesh=112236, priority_region=213, delivery_service_outlets=[400], cpa=Shop.CPA_REAL),
            Shop(fesh=4455, priority_region=213, delivery_service_outlets=[500], cpa=Shop.CPA_REAL),
        ]

        cls.index.outlets += [
            Outlet(
                point_id=11223301,
                fesh=112233,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                bool_props=["A", "C", "cashAllowed"],
                delivery_option=OutletDeliveryOption(
                    day_from=3,
                    day_to=5,
                    order_before=6,
                    work_in_holiday=False,
                    price=500,
                    shipper_readable_id="self shipper",
                ),
                working_days=[0, 1, 2, 3, 4, 5, 6],
                working_times=[
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.MONDAY,
                        days_till=OutletWorkingTime.MONDAY,
                        hours_from='0:10',
                        hours_till='18:00',
                    ),
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.TUESDAY,
                        days_till=OutletWorkingTime.WEDNESDAY,
                        hours_from='0:0',
                        hours_till='24:00',
                    ),
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.THURSDAY,
                        days_till=OutletWorkingTime.SUNDAY,
                        hours_from='9:0',
                        hours_till='19:00',
                    ),
                ],
            ),
            Outlet(
                point_id=11223302,
                fesh=112233,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                bool_props=["A", "prop_2", "C"],
                working_days=[0, 1, 2, 5],
                working_times=[
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.MONDAY,
                        days_till=OutletWorkingTime.SATURDAY,
                        hours_from='0:00',
                        hours_till='0:00',
                    )
                ],
            ),
            Outlet(
                point_id=11223303,
                fesh=112233,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                working_days=[3, 4, 5, 6],
                working_times=[
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.MONDAY,
                        days_till=OutletWorkingTime.FRIDAY,
                        hours_from='9:00',
                        hours_till='18:00',
                    )
                ],
            ),
            Outlet(
                point_id=11223304,
                fesh=4455,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                working_days=[0, 1, 2, 3, 4, 5, 6],
                working_times=[
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.MONDAY,
                        days_till=OutletWorkingTime.SUNDAY,
                        hours_from='9:00',
                        hours_till='18:00',
                    )
                ],
            ),
            Outlet(
                point_id=11223305,
                fesh=112235,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                bool_props=["A", "prop_2", "C"],
                working_days=[0, 1, 2, 5],
                working_times=[
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.MONDAY,
                        days_till=OutletWorkingTime.MONDAY,
                        hours_from="0:10",
                        hours_till="18:00",
                    ),
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.TUESDAY,
                        days_till=OutletWorkingTime.TUESDAY,
                        hours_from="0:0",
                        hours_till="19:00",
                    ),
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.WEDNESDAY,
                        days_till=OutletWorkingTime.WEDNESDAY,
                        hours_from="0:0",
                        hours_till="24:00",
                    ),
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.SATURDAY,
                        days_till=OutletWorkingTime.SATURDAY,
                        hours_from="0:0",
                        hours_till="0:0",
                    ),
                ],
            ),
            Outlet(
                point_id=11223306,
                fesh=112236,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                bool_props=["A", "prop_2", "C"],
                working_days=[0, 1, 2, 5],
                working_times=[
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.MONDAY,
                        days_till=OutletWorkingTime.MONDAY,
                        hours_from="0:10",
                        hours_till="18:00",
                    ),
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.MONDAY,
                        days_till=OutletWorkingTime.MONDAY,
                        hours_from="18:30",
                        hours_till="19:00",
                    ),
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.MONDAY,
                        days_till=OutletWorkingTime.MONDAY,
                        hours_from="20:00",
                        hours_till="22:00",
                    ),
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.TUESDAY,
                        days_till=OutletWorkingTime.TUESDAY,
                        hours_from="0:0",
                        hours_till="19:00",
                    ),
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.WEDNESDAY,
                        days_till=OutletWorkingTime.WEDNESDAY,
                        hours_from="0:0",
                        hours_till="24:00",
                    ),
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.SATURDAY,
                        days_till=OutletWorkingTime.SATURDAY,
                        hours_from="0:0",
                        hours_till="0:0",
                    ),
                ],
            ),
            Outlet(
                point_id=11223307,
                fesh=112233,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                bool_props=["returnAllowed"],
                delivery_option=OutletDeliveryOption(
                    day_from=3,
                    day_to=5,
                    order_before=6,
                    work_in_holiday=False,
                    price=500,
                    shipper_readable_id="self shipper",
                ),
                working_days=[0, 2, 3, 4, 5, 6],
                working_times=[
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.MONDAY,
                        days_till=OutletWorkingTime.MONDAY,
                        hours_from='0:10',
                        hours_till='18:00',
                    ),
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.THURSDAY,
                        days_till=OutletWorkingTime.SUNDAY,
                        hours_from='9:0',
                        hours_till='19:00',
                    ),
                ],
            ),
            Outlet(
                point_id=400,
                delivery_service_id=103,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                bool_props=["prop_2", "C", "prepayAllowed", "cashAllowed", "cardAllowed"],
                working_days=[0, 1, 2, 3, 4, 5, 6, 8],
                working_times=[
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.MONDAY,
                        days_till=OutletWorkingTime.TUESDAY,
                        hours_from='0:00',
                        hours_till='24:00',
                    ),
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.WEDNESDAY,
                        days_till=OutletWorkingTime.THURSDAY,
                        hours_from='0:00',
                        hours_till='23:59',
                    ),
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.FRIDAY,
                        days_till=OutletWorkingTime.FRIDAY,
                        hours_from='9:00',
                        hours_till='18:00',
                    ),
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.SATURDAY,
                        days_till=OutletWorkingTime.SUNDAY,
                        hours_from='11:00',
                        hours_till='17:00',
                    ),
                ],
            ),
            Outlet(
                point_id=500,
                delivery_service_id=103,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                working_days=[0, 2, 3, 4, 5, 6],
                working_times=[
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.MONDAY,
                        days_till=OutletWorkingTime.FRIDAY,
                        hours_from='0:00',
                        hours_till='23:59',
                    ),
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.SATURDAY,
                        days_till=OutletWorkingTime.SATURDAY,
                        hours_from='11:00',
                        hours_till='17:00',
                    ),
                ],
            ),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5001,
                fesh=112233,
                carriers=[99],
                options=[
                    PickupOption(outlet_id=11223301, day_from=3, day_to=5, price=500),
                    PickupOption(outlet_id=11223302),
                    PickupOption(outlet_id=11223303),
                    PickupOption(outlet_id=11223307, day_from=3, day_to=5, price=500),
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5002,
                carriers=[103],
                options=[PickupOption(outlet_id=400)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5003,
                carriers=[103],
                options=[PickupOption(outlet_id=500)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5004,
                fesh=4455,
                carriers=[99],
                options=[PickupOption(outlet_id=11223304)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5005,
                fesh=112235,
                carriers=[99],
                options=[PickupOption(outlet_id=11223305)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5006,
                fesh=112236,
                carriers=[99],
                options=[PickupOption(outlet_id=11223306)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.delivery_calc_feed_info += [
            DeliveryCalcFeedInfo(feed_id=112233, pickupBuckets=[5001]),
        ]

        cls.index.offers += [
            Offer(fesh=112233, hyperid=777, post_term_delivery=True, cpa=Offer.CPA_REAL, pickup_buckets=[5001, 5002]),
            Offer(fesh=112235, hyperid=999, post_term_delivery=True, cpa=Offer.CPA_REAL, pickup_buckets=[5005, 5002]),
            Offer(fesh=112236, hyperid=99901, post_term_delivery=True, cpa=Offer.CPA_REAL, pickup_buckets=[5006, 5002]),
            Offer(fesh=4455, hyperid=888, post_term_delivery=True, cpa=Offer.CPA_REAL, pickup_buckets=[5004, 5003]),
        ]

    def generate_bool_prop(self, prop_name):
        return {"name": prop_name, "description": "property " + prop_name}

    def test_outlet_bool_props(self):
        """Проверяется, что булевы свойства выводятся для аутлетов"""
        """    для place=geo"""
        response = self.report.request_json('place=geo&hyperid=777&rids=213')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "outlet": {
                            "entity": "outlet",
                            "id": "11223301",
                            "BooleanProperties": [self.generate_bool_prop("A"), self.generate_bool_prop("C")],
                        },
                    },
                    {
                        "entity": "offer",
                        "outlet": {
                            "entity": "outlet",
                            "id": "11223302",
                            "BooleanProperties": [
                                self.generate_bool_prop("A"),
                                self.generate_bool_prop("C"),
                                self.generate_bool_prop("prop_2"),
                            ],
                        },
                    },
                    {
                        "entity": "offer",
                        "outlet": {
                            "entity": "outlet",
                            "id": "400",
                            "BooleanProperties": [self.generate_bool_prop("C"), self.generate_bool_prop("prop_2")],
                        },
                    },
                ]
            },
        )

        """    для place=prime"""
        response = self.report.request_json('place=prime&hyperid=777&rids=213')
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "outlet": {
                    "entity": "outlet",
                    "id": "11223301",
                    "BooleanProperties": [self.generate_bool_prop("A"), self.generate_bool_prop("C")],
                },
            },
        )

        outlet_11223301 = (
            '<outlet>'
            '<PointId>11223301</PointId>'
            '<BooleanProperties>'
            '<A description="property A"/>'
            '<C description="property C"/>'
            '</BooleanProperties>'
            '</outlet>'
        )
        outlet_11223302 = (
            '<outlet>'
            '<PointId>11223302</PointId>'
            '<BooleanProperties>'
            '<A description="property A"/>'
            '<C description="property C"/>'
            '<prop_2 description="property prop_2"/>'
            '</BooleanProperties>'
            '</outlet>'
        )
        outlet_400 = (
            '<outlet>'
            '<PointId>400</PointId>'
            '<BooleanProperties>'
            '<C description="property C"/>'
            '<prop_2 description="property prop_2"/>'
            '</BooleanProperties>'
            '</outlet>'
        )

        """    для place=outlets"""
        response = self.report.request_xml('place=outlets&rids=213&outlets=11223301,11223302,400')
        self.assertFragmentIn(
            response, ('<outlets>' '{0}' '{1}' '{2}' '</outlets>').format(outlet_11223301, outlet_11223302, outlet_400)
        )

    @classmethod
    def prepare_outlet_payment_method__cpa_partner(cls):
        cls.index.shops += [
            Shop(fesh=3355, priority_region=213, cpa=Shop.CPA_REAL, is_cpa_partner=True),
            Shop(fesh=3365, priority_region=213, cpa=Shop.CPA_REAL, is_cpa_partner=True),
            Shop(fesh=3375, priority_region=213, cpa=Shop.CPA_REAL, is_cpa_partner=False),
        ]

        cls.index.outlets += [
            # fesh=3355
            Outlet(point_id=3355, fesh=3355, region=213, point_type=Outlet.FOR_PICKUP, working_days=[0]),
            Outlet(point_id=3356, fesh=3355, region=213, point_type=Outlet.FOR_POST_TERM, working_days=[0]),
            Outlet(point_id=33566, fesh=3355, region=213, point_type=Outlet.MIXED_TYPE, working_days=[0]),
            Outlet(point_id=33567, fesh=3355, region=213, point_type=Outlet.FOR_STORE, working_days=[0]),
            Outlet(point_id=3357, fesh=3355, region=2, point_type=Outlet.FOR_PICKUP, working_days=[0]),
            Outlet(point_id=3358, fesh=3355, region=2, point_type=Outlet.FOR_POST_TERM, working_days=[0]),
            Outlet(point_id=3359, fesh=3355, region=2, point_type=Outlet.MIXED_TYPE, working_days=[0]),
            Outlet(point_id=3360, fesh=3355, region=2, point_type=Outlet.FOR_STORE, working_days=[0]),
            # fesh=3365
            Outlet(point_id=3365, fesh=3365, region=213, point_type=Outlet.FOR_PICKUP, working_days=[0]),
            Outlet(point_id=3366, fesh=3365, region=213, point_type=Outlet.FOR_POST_TERM, working_days=[0]),
            Outlet(point_id=33666, fesh=3365, region=213, point_type=Outlet.MIXED_TYPE, working_days=[0]),
            Outlet(point_id=33667, fesh=3365, region=213, point_type=Outlet.FOR_STORE, working_days=[0]),
            Outlet(point_id=3367, fesh=3365, region=2, point_type=Outlet.FOR_PICKUP, working_days=[0]),
            Outlet(point_id=3368, fesh=3365, region=2, point_type=Outlet.FOR_POST_TERM, working_days=[0]),
            Outlet(point_id=3369, fesh=3365, region=2, point_type=Outlet.MIXED_TYPE, working_days=[0]),
            Outlet(point_id=3370, fesh=3365, region=2, point_type=Outlet.FOR_STORE, working_days=[0]),
            # fesh=3375
            Outlet(point_id=3375, fesh=3375, region=213, point_type=Outlet.FOR_PICKUP, working_days=[0]),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=6001,
                carriers=[99],
                options=[
                    PickupOption(outlet_id=3355),
                    PickupOption(outlet_id=3356),
                    PickupOption(outlet_id=33566),
                    PickupOption(outlet_id=33567),
                    PickupOption(outlet_id=3357),
                    PickupOption(outlet_id=3358),
                    PickupOption(outlet_id=3359),
                    PickupOption(outlet_id=3360),
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=6002,
                carriers=[99],
                options=[
                    PickupOption(outlet_id=3365),
                    PickupOption(outlet_id=3366),
                    PickupOption(outlet_id=33666),
                    PickupOption(outlet_id=33667),
                    PickupOption(outlet_id=3367),
                    PickupOption(outlet_id=3368),
                    PickupOption(outlet_id=3369),
                    PickupOption(outlet_id=3370),
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=6003,
                carriers=[99],
                options=[PickupOption(outlet_id=3375)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.offers += [
            Offer(fesh=3355, hyperid=8888, post_term_delivery=True, cpa=Offer.CPA_REAL, pickup_buckets=[6001]),
            Offer(fesh=3365, hyperid=8888, post_term_delivery=True, cpa=Offer.CPA_REAL, pickup_buckets=[6002]),
            Offer(fesh=3375, hyperid=8888, post_term_delivery=True, cpa=Offer.CPA_REAL, pickup_buckets=[6003]),
        ]

    def test_outlet_payment_method__cpa_partner(self):
        # see MARKETOUT-15072
        # see https://wiki.yandex-team.ru/market/projects/multiregion/Opcii-oplaty-po-sposobam-dostavki/Opcii-oplaty-po-sposobam-dostavki/

        # Свой регион
        response = self.report.request_json('place=geo&hyperid=8888&rids=213&max-outlets=100')
        # магазин с предоплатой
        self.assertFragmentIn(response, {"outlet": {"id": "3355", "paymentMethods": ["CASH_ON_DELIVERY"]}})  # PICKUP
        self.assertFragmentIn(response, {"outlet": {"id": "3356", "paymentMethods": ["CASH_ON_DELIVERY"]}})  # POST_TERM
        self.assertFragmentIn(response, {"outlet": {"id": "33566", "paymentMethods": ["CASH_ON_DELIVERY"]}})  # RETAIL

        # магазин без предоплаты
        self.assertFragmentIn(response, {"outlet": {"id": "3365", "paymentMethods": ["CASH_ON_DELIVERY"]}})  # PICKUP
        self.assertFragmentIn(response, {"outlet": {"id": "3366", "paymentMethods": ["CASH_ON_DELIVERY"]}})  # POST_TERM
        self.assertFragmentIn(response, {"outlet": {"id": "33666", "paymentMethods": ["CASH_ON_DELIVERY"]}})  # MIXED
        self.assertFragmentIn(response, {"outlet": {"id": "33667", "paymentMethods": ["CASH_ON_DELIVERY"]}})  # RETAIL

        # Чужой регион
        response = self.report.request_json('place=geo&hyperid=8888&rids=2&max-outlets=100')
        # магазин с предоплатой
        self.assertFragmentIn(response, {"outlet": {"id": "3357", "paymentMethods": Absent()}})  # PICKUP
        self.assertFragmentIn(response, {"outlet": {"id": "3358", "paymentMethods": Absent()}})  # POST_TERM
        self.assertFragmentIn(response, {"outlet": {"id": "3359", "paymentMethods": Absent()}})  # MIXED
        self.assertFragmentIn(response, {"outlet": {"id": "3360", "paymentMethods": ["CASH_ON_DELIVERY"]}})  # RETAIL

        # магазин без предоплаты
        self.assertFragmentIn(response, {"outlet": {"id": "3367", "paymentMethods": Absent()}})  # PICKUP
        self.assertFragmentIn(response, {"outlet": {"id": "3368", "paymentMethods": Absent()}})  # POST_TERM
        self.assertFragmentIn(response, {"outlet": {"id": "3369", "paymentMethods": Absent()}})  # MIXED
        self.assertFragmentIn(response, {"outlet": {"id": "3370", "paymentMethods": ["CASH_ON_DELIVERY"]}})  # RETAIL

    def test_outlet_payment_method__shop_stub(self):
        """
        Для стаба ПИ показываем способы оплаты, даже если магазин работает через API
        MARKETOUT-15399
        """
        response = self.report.request_json(
            'place=geo&hyperid=8888&rids=213&max-outlets=100&client=checkout&co-from=shopadmin-stub'
        )
        self.assertFragmentIn(response, {"outlet": {"id": "3375", "paymentMethods": ["CASH_ON_DELIVERY"]}})

    def test_outlet_return_prop(self):
        """Проверяется, что при заданом &is-return=1 для place=geo выводятся только аутлеты, предназначенные для возврата"""
        response = self.report.request_json('place=geo&hyperid=777&rids=213&is-return=1')
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "outlet": {
                        "id": "11223307",
                        "BooleanProperties": [{"name": "returnAllowed"}],
                    },
                }
            ],
            allow_different_len=False,
        )

    def generate_outlet(self, outlet):
        return {"entity": "offer", "outlet": {"id": outlet}}

    def generate_outlet_work_time_filter(self, daily, daily_num, around_the_clock, around_the_clock_num):
        return [
            {
                "id": "outlet-work-time",
                "type": "enum",
                "name": "Режим работы",
                "subType": "",
                "kind": 2,
                "values": [
                    {"checked": daily, "found": daily_num, "value": "Ежедневно", "id": "daily"},
                    {
                        "checked": around_the_clock,
                        "found": around_the_clock_num,
                        "value": "Круглосуточно",
                        "id": "around-the-clock",
                    },
                ],
            }
        ]

    def test_outlet_work_time(self):
        request = 'place=geo&hyperid=777&rids=213'
        # &outlet-daily
        """Проверяется, что выводятся только аутлеты 11223301 и 400, т.к. только они работают хотябы один раз в каждый день недели"""
        response = self.report.request_json(request + '&outlet-daily=1')
        self.assertFragmentIn(
            response,
            {
                "total": 2,
                "results": [self.generate_outlet("11223301"), self.generate_outlet("400")],
            },
        )
        self.assertEqual(2, response.count({"entity": "outlet"}))

        """Проверяется значение только фильтра daily"""
        response = self.report.request_json('place=geo&hyperid=888&rids=213&outlet-daily=1')
        self.assertFragmentIn(
            response,
            {
                "total": 1,
                "results": [self.generate_outlet("11223304")],
            },
        )
        self.assertEqual(1, response.count({"entity": "outlet"}))

        # &outlet-around-the-clock
        """Проверяется, что выводятся только аутлеты 11223302 и 400, т.к. они работают круглосуточно >=4 дней в неделю"""
        response = self.report.request_json(request + '&outlet-around-the-clock=1')
        self.assertFragmentIn(
            response,
            {
                "total": 2,
                "results": [self.generate_outlet("11223302"), self.generate_outlet("400")],
            },
        )
        self.assertEqual(2, response.count({"entity": "outlet"}))

        """Проверяется значение только фильтра around-the-clock"""
        response = self.report.request_json('place=geo&hyperid=888&rids=213&outlet-around-the-clock=1')
        self.assertFragmentIn(
            response,
            {
                "total": 1,
                "results": [self.generate_outlet("500")],
            },
        )
        self.assertEqual(1, response.count({"entity": "outlet"}))

        # &outlet-working-week-days и &outlet-around-the-clock
        """Проверяется, что выводится только аутлет 400"""
        response = self.report.request_json(request + '&outlet-daily=1&outlet-around-the-clock=1')
        self.assertFragmentIn(
            response,
            {
                "total": 1,
                "results": [self.generate_outlet("400")],
            },
        )
        self.assertEqual(1, response.count({"entity": "outlet"}))

    def test_outlet_daily_around_the_clock(self):
        """
        Проверяем, что для outlet показываются флаги daily и around-the-clock
        """
        response = self.report.request_json('place=outlets&outlets=11223301,11223302,11223304,400,500')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "outlet",
                        "id": "11223301",
                        "daily": True,
                        "around-the-clock": False,
                    },
                    {
                        "entity": "outlet",
                        "id": "11223302",
                        "daily": False,
                        "around-the-clock": True,
                    },
                    {
                        "entity": "outlet",
                        "id": "11223304",
                        "daily": True,
                        "around-the-clock": False,
                    },
                    {
                        "entity": "outlet",
                        "id": "400",
                        "daily": True,
                        "around-the-clock": True,
                    },
                    {
                        "entity": "outlet",
                        "id": "500",
                        "daily": False,
                        "around-the-clock": True,
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_outlet_non_working_time(self):
        """Проверяется вывод секции nonWorkingTime для аутлета"""
        response = self.report.request_json('place=geo&hyperid=999&rids=213')
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "outlet": {
                        "id": "11223305",
                        "workingDay": [
                            {"date": "1985-06-24", "startTime": "00:10", "endTime": "18:00"},
                            {"date": "1985-06-25", "startTime": "00:00", "endTime": "19:00"},
                            {"date": "1985-06-26", "startTime": "00:00", "endTime": "24:00"},
                            {"date": "1985-06-29", "startTime": "00:00", "endTime": "24:00"},
                        ],
                        "nonWorkingTime": [
                            {"1985-06-24": "nonAroundTheClock"},
                            {"1985-06-25": "nonAroundTheClock"},
                            # "1985-06-26" is working- and around-the-clock- day
                            {"1985-06-27": "nonWorking"},
                            {"1985-06-28": "nonWorking"}
                            # "1985-06-29" is working- and around-the-clock- day
                        ],
                    },
                }
            ],
        )

        self.assertFragmentIn(
            response, [{"entity": "offer", "outlet": {"id": "11223305", "nonWorkingTime": ElementCount(4)}}]
        )

    def test_multiple_work_times_in_day(self):
        """Проверяется, что множественные интервалы внутри одного дня отображаются корректно для json и xml плейсов"""
        response = self.report.request_json('place=geo&hyperid=99901&rids=213')
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "outlet": {
                        "id": "11223306",
                        "workingTime": [
                            {"daysFrom": "1", "daysTo": "1", "hoursFrom": "00:10", "hoursTo": "18:00"},
                            {"daysFrom": "1", "daysTo": "1", "hoursFrom": "18:30", "hoursTo": "19:00"},
                            {"daysFrom": "1", "daysTo": "1", "hoursFrom": "20:00", "hoursTo": "22:00"},
                            {"daysFrom": "2", "daysTo": "2", "hoursFrom": "00:00", "hoursTo": "19:00"},
                            {"daysFrom": "3", "daysTo": "3", "hoursFrom": "00:00", "hoursTo": "24:00"},
                            {"daysFrom": "6", "daysTo": "6", "hoursFrom": "00:00", "hoursTo": "24:00"},
                        ],
                        "workingDay": [
                            {"date": "1985-06-24", "startTime": "00:10", "endTime": "18:00"},
                            {"date": "1985-06-24", "startTime": "18:30", "endTime": "19:00"},
                            {"date": "1985-06-24", "startTime": "20:00", "endTime": "22:00"},
                            {"date": "1985-06-25", "startTime": "00:00", "endTime": "19:00"},
                            {"date": "1985-06-26", "startTime": "00:00", "endTime": "24:00"},
                            {"date": "1985-06-29", "startTime": "00:00", "endTime": "24:00"},
                        ],
                        "nonWorkingTime": [
                            {"1985-06-24": "nonAroundTheClock"},
                            {"1985-06-25": "nonAroundTheClock"},
                            # "1985-06-26" is working- and around-the-clock- day
                            {"1985-06-27": "nonWorking"},
                            {"1985-06-28": "nonWorking"}
                            # "1985-06-29" is working- and around-the-clock- day
                        ],
                    },
                }
            ],
        )

        self.assertFragmentIn(
            self.report.request_xml('place=outlets&outlets=11223306'),
            '<WorkingTime>'
            '<WorkingDaysFrom>1</WorkingDaysFrom>'
            '<WorkingDaysTill>1</WorkingDaysTill>'
            '<WorkingHoursFrom>00:10</WorkingHoursFrom>'
            '<WorkingHoursTill>18:00</WorkingHoursTill>'
            '</WorkingTime>'
            '<WorkingTime>'
            '<WorkingDaysFrom>1</WorkingDaysFrom>'
            '<WorkingDaysTill>1</WorkingDaysTill>'
            '<WorkingHoursFrom>18:30</WorkingHoursFrom>'
            '<WorkingHoursTill>19:00</WorkingHoursTill>'
            '</WorkingTime>'
            '<WorkingTime>'
            '<WorkingDaysFrom>1</WorkingDaysFrom>'
            '<WorkingDaysTill>1</WorkingDaysTill>'
            '<WorkingHoursFrom>20:00</WorkingHoursFrom>'
            '<WorkingHoursTill>22:00</WorkingHoursTill>'
            '</WorkingTime>'
            '<WorkingTime>'
            '<WorkingDaysFrom>2</WorkingDaysFrom>'
            '<WorkingDaysTill>2</WorkingDaysTill>'
            '<WorkingHoursFrom>00:00</WorkingHoursFrom>'
            '<WorkingHoursTill>19:00</WorkingHoursTill>'
            '</WorkingTime>'
            '<WorkingTime>'
            '<WorkingDaysFrom>3</WorkingDaysFrom>'
            '<WorkingDaysTill>3</WorkingDaysTill>'
            '<WorkingHoursFrom>00:00</WorkingHoursFrom>'
            '<WorkingHoursTill>24:00</WorkingHoursTill>'
            '</WorkingTime>'
            '<WorkingTime>'
            '<WorkingDaysFrom>6</WorkingDaysFrom>'
            '<WorkingDaysTill>6</WorkingDaysTill>'
            '<WorkingHoursFrom>00:00</WorkingHoursFrom>'
            '<WorkingHoursTill>24:00</WorkingHoursTill>'
            '</WorkingTime>',
        )

    @classmethod
    def prepare_global_shop_info(cls):
        """Создаем два магазина: is_global и не-is_global"""
        cls.index.shops += [
            Shop(fesh=7777, is_global=True),
            Shop(fesh=8888, is_global=False),
        ]

    def test_global_shop_info(self):
        """Проверяем, что для глобал-магазина на выдаче "isGlobal": True,
        а для не-глобал параметра "isGlobal": False
        """
        response = self.report.request_json('place=shop_info&fesh=7777&fesh=8888')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "id": 7777,
                        "isGlobal": True,
                    },
                    {
                        "id": 8888,
                        "isGlobal": False,
                    },
                ]
            },
        )

    def test_shop_pickup_info(self):
        response = self.report.request_json('place=shop_info&fesh=112233&shop-delivery=1&rids=213')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "outletCounts": {"all": 4, "store": 0, "pickup": 4, "depot": 4, "postomat": 0, "bookNow": 0},
                        "pickupOptions": [
                            {"price": {"currency": "RUR", "value": "0"}},
                            {"price": {"currency": "RUR", "value": "500"}, "dayFrom": 3, "dayTo": 5, "orderBefore": 6},
                        ],
                    }
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_shop_delivery_info(cls):
        """Создаем магазины и бакеты в несколько регионов"""
        cls.index.shops += [
            Shop(fesh=22222, currency=Currency.RUR),
            Shop(fesh=22237, currency=Currency.BYN),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=211,
                fesh=22222,
                carriers=[1, 3],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(price=500, day_from=1, day_to=3, order_before=23),
                            DeliveryOption(price=1000, day_from=0, day_to=0, order_before=23),
                        ],
                    ),
                    RegionalDelivery(rid=2, forbidden=True),
                    RegionalDelivery(rid=10758, unknown=True),
                ],
            ),
            DeliveryBucket(
                bucket_id=217,
                fesh=22237,
                carriers=[1, 3],
                regional_options=[
                    RegionalDelivery(
                        rid=157,
                        options=[
                            DeliveryOption(price=60, day_from=1, day_to=3, order_before=23),
                            DeliveryOption(price=100, day_from=0, day_to=0, order_before=23),
                        ],
                    ),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(fesh=22222, hyperid=2, delivery_buckets=[211]),
            Offer(fesh=22237, hyperid=2, delivery_buckets=[217]),
        ]

    def test_shop_delivery_options(self):
        """Проверяем, что при запросах из разных регионов выводятся
        соответствующие условия доставки
        """
        response = self.report.request_json(
            'place=shop_info&fesh=22222&shop-delivery=1&rids=213&base=default.market-exp-prestable.yandex.ru'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "hasDelivery": True,
                        "cheapestDeliveryOption": {
                            "price": {"currency": "RUR", "value": "500", "isDeliveryIncluded": False},
                            "dayFrom": 1,
                            "dayTo": 3,
                            "orderBefore": "2",
                            "isDefault": False,
                            "serviceId": "99",
                        },
                        "fastestDeliveryOption": {
                            "price": {"currency": "RUR", "value": "1000", "isDeliveryIncluded": False},
                            "dayFrom": 0,
                            "dayTo": 0,
                            "orderBefore": "2",
                            "isDefault": False,
                            "serviceId": "99",
                        },
                    }
                ]
            },
            allow_different_len=False,
        )

        # Запрос в BYN
        response = self.report.request_json(
            'place=shop_info&fesh=22222&shop-delivery=1&rids=213&currency=BYN&base=default.market-exp-prestable.yandex.ru'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "cheapestDeliveryOption": {
                            "price": {
                                "currency": "BYN",
                                "value": "20",
                            },
                        },
                        "fastestDeliveryOption": {
                            "price": {
                                "currency": "BYN",
                                "value": "40",
                            },
                        },
                    }
                ]
            },
            allow_different_len=False,
        )

        # Запрос белорусского магазина
        response = self.report.request_json(
            'place=shop_info&fesh=22237&shop-delivery=1&rids=157&currency=BYN&base=default.market-exp-prestable.yandex.ru'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "hasDelivery": True,
                        "cheapestDeliveryOption": {
                            "price": {"currency": "BYN", "value": "60", "isDeliveryIncluded": False},
                            "dayFrom": 1,
                            "dayTo": 3,
                            "orderBefore": "2",
                            "isDefault": False,
                            "serviceId": "99",
                        },
                        "fastestDeliveryOption": {
                            "price": {"currency": "BYN", "value": "100", "isDeliveryIncluded": False},
                            "dayFrom": 0,
                            "dayTo": 0,
                            "orderBefore": "2",
                            "isDefault": False,
                            "serviceId": "99",
                        },
                    }
                ]
            },
            allow_different_len=False,
        )

        # Запрос в RUR
        response = self.report.request_json(
            'place=shop_info&fesh=22237&shop-delivery=1&rids=157&currency=RUR&base=default.market-exp-prestable.yandex.ru'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "cheapestDeliveryOption": {
                            "price": {
                                "currency": "RUR",
                                "value": "1500",
                            },
                        },
                        "fastestDeliveryOption": {
                            "price": {
                                "currency": "RUR",
                                "value": "2500",
                            },
                        },
                    }
                ]
            },
            allow_different_len=False,
        )

        response = self.report.request_json(
            'place=shop_info&fesh=22222&shop-delivery=1&rids=10758&base=default.market-exp-prestable.yandex.ru'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "hasDelivery": True,
                    }
                ]
            },
            allow_different_len=False,
        )

        response = self.report.request_json(
            'place=shop_info&fesh=22222&shop-delivery=1&rids=2&base=default.market-exp-prestable.yandex.ru'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "hasDelivery": False,
                    }
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_shop_delivery_info_offstock(cls):
        """Создаем магазин и бакеты в несколько регионов"""
        cls.index.shops += [
            Shop(fesh=22223, priority_region=213),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=212,
                fesh=22223,
                carriers=[1, 3, 100],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(price=500, day_from=33, day_to=33, order_before=23),
                            DeliveryOption(price=1000, day_from=32, day_to=32, order_before=23),
                        ],
                    ),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(fesh=22223, hyperid=3, delivery_buckets=[212]),
        ]

    def test_shop_delivery_options_offstock(self):
        """Проверяем, что при запросе опций магазина, у которого
        все опции с длительными сроками ("на заказ"), выводится
        только cheapestDeliveryOption
        """
        response = self.report.request_json(
            'place=shop_info&fesh=22223&shop-delivery=1&rids=213&base=default.market-exp-prestable.yandex.ru'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "hasDelivery": True,
                        "cheapestDeliveryOption": {
                            "price": {"currency": "RUR", "value": "500", "isDeliveryIncluded": False},
                            "dayFrom": Absent(),
                            "dayTo": Absent(),
                            "isDefault": False,
                            "serviceId": "99",
                        },
                        "fastestDeliveryOption": Absent(),
                    }
                ]
            },
            allow_different_len=False,
        )

    def test_shop_delivery_options_another_currency(self):
        """Проверяем, что при запросе опций магазина в другой валюте
        цена доставки пересчитывается корректно
        """
        response = self.report.request_json(
            'place=shop_info&fesh=22223&shop-delivery=1&rids=213&currency=USD&base=default.market-exp-prestable.yandex.ru'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "hasDelivery": True,
                        "cheapestDeliveryOption": {
                            "price": {"currency": "USD", "value": "8", "isDeliveryIncluded": False},
                            "dayFrom": Absent(),
                            "dayTo": Absent(),
                            "isDefault": False,
                            "serviceId": "99",
                        },
                        "fastestDeliveryOption": Absent(),
                    }
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_shop_return_info(cls):
        cls.index.hypertree += [
            HyperCategory(hid=101, goods_return_policy="7d"),
            HyperCategory(hid=102, goods_return_policy="14d"),
            HyperCategory(hid=103, goods_return_policy="21d"),
            HyperCategory(hid=104, goods_return_policy="28d"),
            HyperCategory(hid=105, goods_return_policy="with_problems"),
            HyperCategory(hid=106),
        ]

        cls.index.shops += [
            Shop(fesh=22224, name='all options', priority_region=213),
            Shop(fesh=22225, name='some options', priority_region=213),
            Shop(fesh=22226, name='no options', priority_region=213),
            Shop(fesh=22227, name='global', is_global=True, priority_region=213),
        ]

        for seq in range(1, 6):
            cls.index.offers += [
                Offer(fesh=22224, hid=100 + seq),
                Offer(fesh=22227, hid=100 + seq),
            ]

        cls.index.offers += [
            Offer(fesh=22225, hid=102),
            Offer(fesh=22225, hid=103),
            Offer(fesh=22226, hid=106),
        ]

    def test_shop_return_info(self):
        """Проверяем, что опции возврата магазина возвращаются в соответствии
        с форматом и сроки возврата на выдаче правильные
        """
        # Магазин со всеми возможными опциями
        response = self.report.request_json(
            'place=shop_info&fesh=22224&shop-delivery=1&rids=213&base=default.market-exp-prestable.yandex.ru'
        )
        self.assertFragmentIn(
            response,
            {"results": [{"returnPolicy": {"fast": "7d", "slow": "28d", "other": ["with_problems"]}}]},
            allow_different_len=False,
        )

        # Магазин со частью возможных опций
        response = self.report.request_json(
            'place=shop_info&fesh=22225&shop-delivery=1&rids=213&base=default.market-exp-prestable.yandex.ru'
        )
        self.assertFragmentIn(
            response,
            {"results": [{"returnPolicy": {"fast": "14d", "slow": "21d", "other": Absent()}}]},
            allow_different_len=False,
        )

        # Магазин без опций возврата
        response = self.report.request_json(
            'place=shop_info&fesh=22226&shop-delivery=1&rids=213&base=default.market-exp-prestable.yandex.ru'
        )
        self.assertFragmentIn(response, {"results": [{"returnPolicy": Absent()}]}, allow_different_len=False)

        # Глобал-магазин
        response = self.report.request_json(
            'place=shop_info&fesh=22227&shop-delivery=1&rids=213&base=default.market-exp-prestable.yandex.ru'
        )
        self.assertFragmentIn(
            response, {"results": [{"returnPolicy": {"other": ["global"]}}]}, allow_different_len=False
        )

    @classmethod
    def prepare_vendor_recommended_shops(cls):
        cls.index.shops += [
            Shop(fesh=22228, name='recommended', priority_region=213),
            Shop(fesh=22229, name='not recommended', priority_region=213),
        ]

        cls.index.vendors += [
            Vendor(vendor_id=1001, recommended_shops=[22228]),
            Vendor(vendor_id=1002, recommended_shops=[22228]),
        ]

    def test_vendor_recommended_shops(self):
        """Проверяем, что рекомендации от вендоров выводятся в нужном формате"""
        # Магазин с рекомендациями
        response = self.report.request_json('place=shop_info&fesh=22228&base=default.market-exp-prestable.yandex.ru')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "vendorRecommendations": [
                            {"entity": "vendor", "id": 1001, "name": "VENDOR-1001"},
                            {"entity": "vendor", "id": 1002, "name": "VENDOR-1002"},
                        ]
                    }
                ]
            },
            allow_different_len=False,
        )

        # Магазин без рекомендаций
        response = self.report.request_json('place=shop_info&fesh=22229&base=default.market-exp-prestable.yandex.ru')
        self.assertFragmentIn(response, {"results": [{"vendorRecommendations": Absent()}]}, allow_different_len=False)

    def check_ff_light_delivery_options(self, request, service1, service2):
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "delivery": {
                    "availableServices": [{"serviceId": service1}, {"serviceId": service2}],
                    "options": [
                        {"isDefault": True, "serviceId": str(service1)},
                        {"isDefault": False, "serviceId": str(service2)},
                    ],
                }
            },
            allow_different_len=False,
        )

    def check_payment_methods(self, request, methods):
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response, {"options": [{"serviceId": "157", "paymentMethods": methods}]}, allow_different_len=False
        )

    def check_ff_light_delivery(self, rids, add_day):
        results = [
            {
                "entity": "offer",
                "fulfillment": Absent(),
                "shop": {"id": 2222},
                "delivery": {
                    "options": [
                        {"isDefault": True, "serviceId": "157"},
                        {
                            "isDefault": False,
                            "serviceId": "158",
                            "dayFrom": 5 + add_day,  # [5 + add_day; 7 + add_day] days for 'regular' offer ...
                            "dayTo": 7 + add_day,
                        },
                    ]
                },
            },
            {
                "entity": "offer",
                "fulfillment": {"type": "light"},
                "shop": {"id": 444555},
                "delivery": {
                    "options": [
                        {
                            "isDefault": True,
                            "serviceId": "158",
                            "dayFrom": 6
                            + add_day,  # ... and [5 + ff_light_day + add_day; 7 + ff_light_day + add_day] for 'ff_ligth' offer, where ff_light_day = 1
                            "dayTo": 8 + add_day + 1,
                        }
                    ]
                },
            },
        ]

        if rids == "47":
            results.append({"entity": "regionalDelimiter"})

        response = self.report.request_json(
            "place=prime&hyperid=7891011&&base=default.market-exp-prestable.yandex.ru&rids=" + rids
        )
        self.assertFragmentIn(response, {"results": results}, allow_different_len=False)

    @classmethod
    def prepare_shop_equal_delivery_options(cls):
        """Создаем магазин и оффер в нем с двумя бакетами доставки,
        в которых опции одинаковы (с учетом order-before)
        """
        cls.index.shops += [
            Shop(fesh=22230, priority_region=213),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=214,
                fesh=22230,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(price=500, day_from=0, day_to=1, order_before=1),
                        ],
                    ),
                ],
            ),
            DeliveryBucket(
                bucket_id=215,
                fesh=22230,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(price=500, day_from=1, day_to=2),
                        ],
                    ),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(fesh=22230, delivery_buckets=[214, 215]),
        ]

    def test_shop_equal_delivery_options(self):
        """Задаем запрос за магазином с одинаковыми опциями.
        Они получаются одинаковыми после применения order-before,
        т.к. тестовые запросы происходят фиксированно в 3 часа утра по Мск
        Проверяем, что в этом случае на выдаче есть только cheapestDeliveryOption,
        а fastestDeliveryOption не выводится
        """
        response = self.report.request_json(
            'place=shop_info&fesh=22230&shop-delivery=1&rids=213&base=default.market-exp-prestable.yandex.ru'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "cheapestDeliveryOption": {
                            "price": {"currency": "RUR", "value": "500", "isDeliveryIncluded": False},
                            "dayFrom": 1,
                            "dayTo": 2,
                            "orderBefore": Absent(),
                            "isDefault": False,
                            "serviceId": "99",
                        },
                        "fastestDeliveryOption": Absent(),
                    }
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_is_cpa_partner_shop_info(cls):
        """Создаем два магазина: c is_cpa_partner и без него"""
        cls.index.shops += [
            Shop(fesh=22231, is_cpa_partner=True),
            Shop(fesh=22232),
        ]

    def test_is_cpa_partner_shop_info(self):
        """Проверяем, что для is_cpa_partner-магазина на выдаче "isCpaPartner": True,
        а для не-is_cpa_partner на выдаче "isCpaPartner": False
        """
        response = self.report.request_json(
            'place=shop_info&fesh=22231&fesh=22232&base=default.market-exp-prestable.yandex.ru'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "id": 22231,
                        "isCpaPartner": True,
                    },
                    {
                        "id": 22232,
                        "isCpaPartner": False,
                    },
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_shop_country(cls):
        """Создаем магазин в Пекине"""
        cls.index.shops += [
            Shop(fesh=22233, priority_region=10590),
        ]

    def test_shop_country(self):
        """Проверяем, что для магазина выводится страна в микроформате региона"""
        response = self.report.request_json('place=shop_info&fesh=22233&base=default.market-exp-prestable.yandex.ru')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "shopPriorityCountry": {
                            "entity": "region",
                            "id": 134,
                            "name": "Китай",
                            "lingua": {
                                "name": {
                                    "genitive": "Китая",
                                    "preposition": "в",
                                    "prepositional": "Китай",
                                    "accusative": "Китай",
                                }
                            },
                        }
                    }
                ]
            },
        )

    @classmethod
    def prepare_shop_phone_link(cls):
        """Создаем обычный и CPA-only магазины и офферы в них"""
        cls.index.shops += [
            Shop(fesh=22234, priority_region=213, phone='+74951234567', cpc=Shop.CPC_REAL),
            Shop(fesh=22235, priority_region=213, cpc=Shop.CPC_NO),
        ]

        cls.index.offers += [
            Offer(fesh=22234),
            Offer(fesh=22235),
        ]

    def test_shop_phone(self):
        """Проверяем формат вывода телефона магазина"""
        response = self.report.request_json(
            'place=shop_info&fesh=22234&rids=213&base=default.market-exp-prestable.yandex.ru'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "phones": {"raw": "+74951234567", "sanitized": "+74951234567"},
                    }
                ]
            },
        )

    def test_shop_phone_link(self):
        """Проверяем, что нет записей в show- и click- логах для обычного магазина
        без контекста оффера
        """
        _ = self.report.request_json(
            'place=shop_info&fesh=22234&show-urls=showPhone&yandexuid=100500&rids=213&base=default.market-exp-prestable.yandex.ru'
        )
        self.show_log.expect(url=Wildcard('*')).never()
        self.click_log.expect(url=Wildcard('*')).never()

    def test_cpa_only_shop_phone_link(self):
        """Проверяем, что нет записей в show- и click- логах для cpa-only магазина
        без контекста оффера
        """
        _ = self.report.request_json(
            'place=shop_info&fesh=22235&show-urls=showPhone&yandexuid=100500&rids=213&base=default.market-exp-prestable.yandex.ru'
        )
        self.show_log.expect(url=Wildcard('*')).never()
        self.click_log.expect(url=Wildcard('*')).never()

    def test_shop_phone_link_with_context(self):
        """Проверяем записи в show- и click- логах для обычного магазина
        с контекстом оффера, переданном в cpc - цена клика должна быть взята из контекста
        """
        cpc = str(
            Cpc.create_for_offer(
                click_price=91,
                offer_id='RcSMzi4tf73qGvxRx8atJg',
                bid=80,
                hid=111,
                shop_id=22234,
                vendor_click_price=24,
                vendor_bid=31,
                minimal_bid=14,
                shop_fee=2,
                minimal_fee=1,
                fee=1,
            )
        )
        _ = self.report.request_json(
            'place=shop_info&fesh=22234&show-urls=showPhone&yandexuid=100500&rids=213&cpc={}&base=default.market-exp-prestable.yandex.ru'.format(
                cpc
            )
        )
        self.show_log.expect(
            click_price=91,
            shop_id=22234,
            bid=80,
            autobroker_enabled=0,
            ctr=0,
            position=0,
            vbid=31,
            vendor_click_price=24,
            vendor_ds_id=None,
            vendor_price=None,
            vc_bid=0,
            click_type_id=1,
            record_type=0,
            url_type=UrlType.SHOW_PHONE,
            phone_click_ratio=None,
            phone_click_threshold=None,
            tariff=None,
            feed_id=-1,
            category_id=111,
            ware_md5='RcSMzi4tf73qGvxRx8atJg',
        )

        self.click_log.expect(
            ClickType.SHOW_PHONE,
            cp=91,
            shop_id=22234,
            cb=80,
            ae=0,
            position=0,
            cb_vnd=31,
            cp_vnd=24,
            vendor_ds_id=None,
            vendor_price=None,
            url_type=UrlType.SHOW_PHONE,
            phone_click_ratio=None,
            feed_id=-1,
            categid=111,
            hyper_cat_id=111,
            nav_cat_id=0,
            ware_md5='RcSMzi4tf73qGvxRx8atJg',
        )

    def test_cpa_only_shop_phone_link_with_context(self):
        """Проверяем, что нет записей в show- и click- логах для cpa-only магазина
        с контекстом оффера, переданном в cpc
        """
        cpc = str(
            Cpc.create_for_offer(
                click_price=91,
                offer_id='RcSMzi4tf73qGvxRx8atJg',
                bid=80,
                shop_id=22235,
                hid=111,
                vendor_click_price=24,
                vendor_bid=31,
                minimal_bid=14,
                shop_fee=2,
                minimal_fee=1,
                fee=1,
            )
        )
        _ = self.report.request_json(
            'place=shop_info&fesh=22235&show-urls=showPhone&yandexuid=100500&rids=213&cpc={}&base=default.market-exp-prestable.yandex.ru'.format(
                cpc
            )
        )
        self.show_log.expect(url=Wildcard('*')).never()
        self.click_log.expect(url=Wildcard('*')).never()

    def test_shop_priority_region(self):
        """Проверяем, что для магазина выводится приоритетный регион в микроформате региона
        Используем данные тестов test_shop_country и test_global_shop
        """
        # Магазин из Пекина
        response = self.report.request_json('place=shop_info&fesh=22233')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "shopPriorityRegion": {
                            "entity": "region",
                            "id": 10590,
                            "name": "Пекин",
                            "lingua": {
                                "name": {
                                    "genitive": "Пекина",
                                    "preposition": "в",
                                    "prepositional": "Пекин",
                                    "accusative": "Пекин",
                                }
                            },
                        }
                    }
                ]
            },
        )

        # Магазин из Москвы
        response = self.report.request_json('place=shop_info&fesh=1309201')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "shopPriorityRegion": {
                            "entity": "region",
                            "id": 213,
                            "name": "Москва",
                            "lingua": {"name": {"accusative": "Москву"}},
                        }
                    }
                ]
            },
        )

    @classmethod
    def prepare_new_ratings(cls):
        """
        Подготовка новых рейтингов
        """
        cls.index.shops += [
            Shop(
                fesh=1667901,
                new_shop_rating=NewShopRating(
                    new_rating=4.5,
                    new_rating_total=3.9,
                    skk_disabled=False,
                    force_new=False,
                    new_grades_count_3m=123,
                    new_grades_count=456,
                    rec_and_nonrec_pub_count=789,
                    abo_old_rating=2,
                    abo_old_raw_rating=0.4,
                ),
                abo_shop_rating=AboShopRating(
                    shop_name="magazin.tld",
                    rating=3,  # теперь смотрится abo_old_rating из NewShopRating
                    raw_rating=0.5,  # аналогично с abo_old_raw_rating
                    status="oldshop",
                    cutoff="2017-01-30T15:23:51",
                    grade_base=0,
                    grade_total=123,
                ),
            ),
            Shop(
                fesh=1667905,
            ),
        ]
        """
        Могут быть магазины, которые есть в файле с рейтингами от персов,
        но отсутствуют в shops.dat. На выдаче они имеют поле historicOnly = true
        """
        cls.index.shops += [
            Shop(
                fesh=1667902,
                historical=True,
                new_shop_rating=NewShopRating(
                    new_rating=4.1,
                    new_rating_total=3.8,
                    skk_disabled=False,
                    force_new=False,
                    new_grades_count_3m=321,
                    new_grades_count=654,
                    fesh=1667902,
                ),
            ),
            Shop(fesh=1667903, historical=True, new_shop_rating=NewShopRating(new_rating_total=4.7)),
            Shop(
                fesh=1667904,
                historical=True,
                new_shop_rating=NewShopRating(
                    fesh=1667904,
                    abo_old_rating=4,
                    abo_old_raw_rating=0.65,
                ),
            ),
        ]

        """
        Магазины с невалидным айдишником не должны ломать считывание файла с новым рейтингом
        """
        cls.index.shops += [
            Shop(fesh=-1, historical=True, new_shop_rating=NewShopRating(new_rating=4.1)),
            Shop(fesh=0, historical=True, new_shop_rating=NewShopRating(new_rating=4.7)),
        ]

    def test_new_rating(self):
        """
        Проверяем, что новый рейтинг есть в выдаче place=shop_info
        """
        response = self.report.request_json('place=shop_info&fesh=1667901')
        self.assertFragmentIn(
            response,
            {
                "historicOnly": False,
                "oldRating": 2,
                "newRating": 4.5,
                "newRatingTotal": 3.9,
                "ratingToShow": 4.5,
                "ratingType": 3,
                "skkDisabled": False,
                "newGradesCount3M": 123,
                "newGradesCount": 456,
                "overallGradesCount": 789,
                "aboName": "magazin.tld",
                "aboOldRating": 2,
                "oldRawRating": 0.4,
                "oldStatus": "oldshop",
                "oldCutoff": "2017-01-30T15:23:51",
                "oldGradeBase": 0,
                "oldGradesCount": 123,
            },
        )

        # Если ratingToShow брался из newRating, то ratingType == 3
        response = self.report.request_json('place=shop_info&fesh=1667902')
        self.assertFragmentIn(
            response,
            {
                "historicOnly": True,
                "oldRating": 0,
                "newRating": 4.1,
                "newRatingTotal": 3.8,
                "ratingToShow": 4.1,
                "ratingType": 3,
            },
        )

        # Если ratingToShow брался из newRatingTotal, то ratingType == 2
        response = self.report.request_json('place=shop_info&fesh=1667903')
        self.assertFragmentIn(
            response,
            {
                "historicOnly": True,
                "oldRating": 0,
                "newRating": 0,
                "newRatingTotal": 4.7,
                "ratingToShow": 4.7,
                "ratingType": 2,
            },
        )

        # Если ratingToShow брался из aboOldRating, то ratingType == 1
        response = self.report.request_json('place=shop_info&fesh=1667904')
        self.assertFragmentIn(
            response,
            {
                "aboOldRating": 4,
                "ratingToShow": 4,
                "ratingType": 1,
            },
        )

        # Если магазина еще нет в выгрузке персов, то мы должны нарисовать ему NoRating (ratingType==6)
        response = self.report.request_json('place=shop_info&fesh=1667905')
        self.assertFragmentIn(
            response,
            {
                "ratingToShow": 0,
                "ratingType": 6,
            },
        )

    @classmethod
    def prepare_shopsdat_is_supplier(cls):
        cls.index.shops += [
            Shop(fesh=17203001, is_supplier=False),
            Shop(fesh=17203002, blue='NO'),
            Shop(fesh=17203003, blue='REAL'),
            Shop(fesh=17203004, blue='REAL', datafeed_id=3004, warehouse_id=3),
            Shop(fesh=17203004, blue='REAL', datafeed_id=4004, warehouse_id=4),
            Shop(fesh=17203008, blue='REAL', datafeed_id=5004, warehouse_id=7),
            Shop(fesh=17203005),  # False by default
            Shop(fesh=17203006, cpa=Shop.CPA_REAL),
        ]

        cls.index.shop_operational_rating += [
            ShopOperationalRating(
                calc_time=1589936458409,
                shop_id=17203003,
                late_ship_rate=5.9,
                cancellation_rate=1.93,
                return_rate=0.14,
                total=99.8,
            ),
            ShopOperationalRating(
                calc_time=1589936458409,
                shop_id=17203006,
                late_ship_rate=5.9,
                cancellation_rate=1.93,
                return_rate=0.14,
                total=99.8,
                dsbs_return_rate=0.5,
            ),
        ]

    def test_is_supplier_correct(self):
        self.assertFragmentIn(
            self.report.request_json('place=shop_info&fesh=17203001'),
            {
                'id': 17203001,
                'isSupplier': False,
                'blueStatus': 'no',
            },
        )

        self.assertFragmentIn(
            self.report.request_json('place=shop_info&fesh=17203002'),
            {
                'id': 17203002,
                'isSupplier': False,
                'blueStatus': 'no',
            },
        )

        self.assertFragmentIn(
            self.report.request_json('place=shop_info&fesh=17203003'),
            {'id': 17203003, 'isSupplier': True, 'blueStatus': 'real'},
        )

        self.assertFragmentIn(
            self.report.request_json('place=shop_info&fesh=17203004'),
            {'id': 17203004, 'isSupplier': True, 'blueStatus': 'real'},
        )

        self.assertFragmentIn(
            self.report.request_json('place=shop_info&fesh=17203005'),
            {'id': 17203005, 'isSupplier': False, 'blueStatus': 'no'},
        )

    def test_shop_operational_rating(self):
        """
        Проверяем, что при переданном флаге market_opeational_rating происходит добавление в выдачу
        операционного рейтинга, если таковой имеется для данного магазина.
        """
        OPERATIONAL_RATING_FLAGS = (
            ('', True),
            ('&rearr-factors=market_operational_rating=1;market_operational_rating_everywhere=1', True),
            ('&rearr-factors=market_operational_rating=0;market_operational_rating_everywhere=0', False),
        )

        for rearr_flag, has_rating in OPERATIONAL_RATING_FLAGS:
            response = self.report.request_json("place=shop_info&fesh=17203003&rgb=blue{}".format(rearr_flag))
            if has_rating:
                self.assertFragmentIn(
                    response,
                    {
                        "id": 17203003,
                        "operationalRating": {
                            "calcTime": 1589936458409,
                            "lateShipRate": 5.9,
                            "cancellationRate": 1.93,
                            "returnRate": 0.14,
                            "total": 99.8,
                        },
                    },
                )
            else:
                self.assertFragmentIn(response, {"id": 17203003, "operationalRating": Absent()})

        response = self.report.request_json(
            "place=shop_info&fesh=17203006&rgb=blue&rearr-factors=market_operational_rating=1"
        )
        self.assertFragmentIn(
            response,
            {
                "id": 17203006,
                "operationalRating": {
                    "calcTime": 1589936458409,
                    "lateShipRate": 5.9,
                    "cancellationRate": 1.93,
                    "returnRate": 0.14,
                    "total": 99.8,
                    "dsbsReturnRate": 0.5,
                },
            },
        )

        response = self.report.request_json(
            "place=shop_info&fesh=17203006&rgb=blue&rearr-factors=market_operational_rating=1;market_operational_rating_everywhere=0"
        )
        self.assertFragmentIn(response, {"id": 17203006, "operationalRating": Absent()})

    @classmethod
    def prepare_shop_name(cls):
        cls.index.shops += [
            Shop(fesh=18893001, name='HornsAndHooves', business_fesh=12345, business_name="hah"),
            Shop(fesh=18893004, name='HornsAndHooves_2', business_fesh=12345, business_name="hah"),
            Shop(fesh=18893002, name='Глобглогабгалаб'),
            Shop(fesh=18893003, name='Рыба, рак и щука'),
        ]

    def test_shop_name(self):
        self.assertFragmentIn(
            self.report.request_json('place=shop_info&fesh=18893001'),
            {
                'id': 18893001,
                'shopName': 'HornsAndHooves',
                'businessId': 12345,
                'businessName': 'hah',
            },
        )

        self.assertFragmentIn(
            self.report.request_json('place=shop_info&fesh=18893002'),
            {
                'id': 18893002,
                'shopName': 'Глобглогабгалаб',
            },
        )

    def test_slug(self):
        self.assertFragmentIn(
            self.report.request_json('place=shop_info&fesh=18893003'),
            {
                'id': 18893003,
                'slug': 'ryba-rak-i-shchuka',
            },
        )

    @classmethod
    def prepare_shopsdat_ignore_stocks(cls):
        cls.index.shops += [
            Shop(fesh=155751),
            Shop(fesh=155752, ignore_stocks=False),
            Shop(fesh=155753, ignore_stocks=True),
        ]

    def test_ignore_stocks(self):
        for fesh, ignore_stocks in ((155751, False), (155752, False), (155753, True)):
            self.assertFragmentIn(
                self.report.request_json('place=shop_info&fesh={}'.format(fesh)),
                {'id': fesh, 'ignoreStocks': ignore_stocks},
            )

    @classmethod
    def prepare_is_virtual(cls):
        cls.index.shops += [
            Shop(
                fesh=1111,
                priority_region=43,
                regions=[47, 43, 172, 213],
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                datafeed_id=1111234,
            ),
            Shop(
                fesh=1100,
                priority_region=213,
                regions=[47, 43, 172],
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_RED,
                cpa=Shop.CPA_REAL,
            ),
            Shop(fesh=2222, priority_region=43),
        ]

    def test_is_virtual(self):
        test_data = (
            (1100, True),  # Red virtual shop
            (1111, True),  # Blue virtual shop
            (2222, False),  # White shop
            (17203004, False),  # Blue supplier
        )

        for fesh, is_virtual in test_data:
            response = self.report.request_json('place=shop_info&fesh={}'.format(fesh))
            self.assertFragmentIn(response, {'id': fesh, 'isVirtual': is_virtual})

    def test_feeds_by_warehouse(self):
        test_data = (
            (2222, None),  # White shop
            (17203004, [(3, 3004), (4, 4004)]),  # Blue supplier, two warehouses
        )

        for fesh, feeds in test_data:
            response = self.report.request_json('place=shop_info&fesh={}'.format(fesh))
            if feeds is None:
                self.assertFragmentIn(response, {'id': fesh, 'feeds': NoKey('feeds')})
            else:
                self.assertFragmentIn(
                    response,
                    {
                        'id': fesh,
                        'feeds': [{'warehouseId': warehouse_id, 'feedId': feed_id} for warehouse_id, feed_id in feeds],
                    },
                )

    @classmethod
    def prepare_shopsdat_direct_shipping(cls):
        cls.index.shops += [
            Shop(fesh=146641),
            Shop(fesh=146642, direct_shipping=False),
            Shop(fesh=146643, direct_shipping=True),
        ]

    def test_direct_shipping(self):
        for fesh, direct_shipping in ((146641, True), (146642, False), (146643, True)):
            self.assertFragmentIn(
                self.report.request_json('place=shop_info&fesh={}'.format(fesh) + USE_DEPRECATED_DIRECT_SHIPPING_FLOW),
                {'id': fesh, 'isDirectShipping': direct_shipping},
            )

    @classmethod
    def prepare_shop_new_rating(cls):
        cls.index.shops += [
            Shop(
                fesh=424242,
                datafeed_id=424242,
                priority_region=213,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                new_shop_rating=NewShopRating(new_rating_total=4.2),
            ),
            Shop(
                # для того же магазина делаем еще один фид, чтобы проверить,
                # что это не ломает рейтинг магазина
                fesh=424242,
                datafeed_id=424243,
                priority_region=213,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                sku=42, hyperid=42, blue_offers=[BlueOffer(price=10, fesh=424242, feedid=424242, offerid='blue1')]
            ),
        ]

        cls.index.shop_operational_rating += [
            ShopOperationalRating(
                calc_time=1589936458409,
                shop_id=424242,
                late_ship_rate=5.9,
                cancellation_rate=1.93,
                return_rate=0.14,
                total=99.8,
                dsbs_return_rate=0.5,
            ),
        ]

    def test_shop_new_rating(self):
        # проверяем что рейтинг синего магазина выставляется правильно как в shop_info, так и в prime
        fesh = 424242
        msku = 42
        self.assertFragmentIn(
            self.report.request_json('place=shop_info&fesh={}'.format(fesh)),
            {
                'id': fesh,
                'ratingToShow': 4.2,
            },
        )
        self.assertFragmentIn(
            self.report.request_json('place=prime&market-sku={}&rearr-factors=market_metadoc_search=no'.format(msku)),
            {
                'realShop': {
                    'id': fesh,
                    'ratingToShow': 4.2,
                }
            },
        )

    def test_shop_operational_rating_multifeed(self):
        # Проверяем что рейтинг не перебивается другим фидом
        msku = 42
        self.assertFragmentIn(
            self.report.request_json(
                'place=prime&market-sku={}&rearr-factors=market_metadoc_search=no;market_operational_rating=1'.format(
                    msku
                )
            ),
            {
                "search": {
                    "results": [
                        {
                            "marketSku": "42",
                            "supplier": {
                                "id": 424242,
                                "operationalRating": {
                                    "calcTime": 1589936458409,
                                    "lateShipRate": 5.9,
                                    "cancellationRate": 1.93,
                                    "returnRate": 0.14,
                                    "total": 99.8,
                                    "dsbsReturnRate": 0.5,
                                },
                            },
                        },
                    ]
                }
            },
        )

    @classmethod
    def prepare_medicine_shops(cls):
        cls.index.shops += [
            Shop(fesh=1001001),
            Shop(fesh=1001002, medicine_license='license_data'),
            Shop(fesh=1001003, prescription_management_system=PrescriptionManagementSystem.PS_MEDICATA),
            Shop(fesh=1001004, medicine_courier=True),
        ]

    def test_shop_medicine_license(self):
        response = self.report.request_json('place=shop_info&fesh=1001001&fesh=1001002')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "shop",
                        "id": 1001001,
                    },
                    {
                        "entity": "shop",
                        "id": 1001002,
                        "medicineLicense": "license_data",
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_shop_prescription_management_system(self):
        response = self.report.request_json('place=shop_info&fesh=1001001&fesh=1001003')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "shop",
                        "id": 1001001,
                    },
                    {
                        "entity": "shop",
                        "id": 1001003,
                        "prescriptionManagementSystem": PrescriptionManagementSystem.PS_MEDICATA_STRING,
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_shop_medicine_courier(self):
        response = self.report.request_json('place=shop_info&fesh=1001001&fesh=1001004')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "shop",
                        "id": 1001001,
                    },
                    {
                        "entity": "shop",
                        "id": 1001004,
                        "medicineCourier": "true",
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_businessid_as_fesh(self):
        self.assertFragmentIn(
            self.report.request_json('place=shop_info&fesh=12345'),
            {
                "results": [
                    {
                        'id': 18893001,
                        'shopName': 'HornsAndHooves',
                        'businessId': 12345,
                        'businessName': 'hah',
                    },
                    {
                        'id': 18893004,
                        'shopName': 'HornsAndHooves_2',
                        'businessId': 12345,
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_shops_per_business(self):
        # https://st.yandex-team.ru/MARKETOUT-45207
        # костыль чтобы получить информацию о бизнесе
        # shops-per-business=0 показывает все магазины бизнеса
        requests = (
            'place=shop_info&fesh=12345&shops-per-business=0',
            'place=shop_info&fesh=12345&shops-per-business=2',
        )

        for request in requests:
            self.assertFragmentIn(
                self.report.request_json(request),
                {
                    "results": [
                        {
                            'id': 18893001,
                            'shopName': 'HornsAndHooves',
                            'businessId': 12345,
                            'businessName': 'hah',
                        },
                        {
                            'id': 18893004,
                            'shopName': 'HornsAndHooves_2',
                            'businessId': 12345,
                        },
                    ]
                },
                allow_different_len=False,
            )

        # shops-per-business=1 - показываем только один магазин бизнеса
        self.assertFragmentIn(
            self.report.request_json('place=shop_info&fesh=12345&shops-per-business=1'),
            {"results": [{"entity": "shop"}]},
            allow_different_len=False,
        )

    @classmethod
    def prepare_only_express(cls):
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=_Gps1.location_combinator,
            rear_factors=make_mock_rearr(),
        ).respond_with_express_warehouses(
            [
                CombinatorExpressWarehouse(
                    warehouse_id=3,
                    zone_id=1,
                    priority=2,
                ),
                CombinatorExpressWarehouse(
                    warehouse_id=4,
                    zone_id=1,
                    priority=5,
                ),
            ]
        )
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=_Gps2.location_combinator,
            rear_factors=make_mock_rearr(),
        ).respond_with_express_warehouses(
            [
                CombinatorExpressWarehouse(
                    warehouse_id=3,
                    zone_id=1,
                    priority=2,
                ),
                CombinatorExpressWarehouse(
                    warehouse_id=4,
                    zone_id=1,
                    priority=1,
                ),
            ]
        )

    def test_only_express(self):
        BusinessIds = "17203004,17203008"

        request = "place=shop_info&rids=213&fesh={}&hyperlocal-mode=only_express&gps={}"
        response = self.report.request_json(request.format(BusinessIds, _Gps1.location_str))
        self.assertFragmentIn(
            response,
            {
                'id': 17203004,
                "feeds": [
                    {"feedId": 3004, "warehouseId": 3},
                    {"feedId": 4004, "warehouseId": 4},
                ],
            },
            allow_different_len=False,
        )

        # ограничим количество магазинов на бизнес
        # Сортировка по приоритету
        response = self.report.request_json(request.format(BusinessIds, _Gps1.location_str) + "&shops-per-business=1")
        self.assertFragmentIn(
            response,
            {
                'id': 17203004,
                "feeds": [
                    {"feedId": 3004, "warehouseId": 3},
                ],
            },
            allow_different_len=False,
        )

        # Сортировка по приоритету
        response = self.report.request_json(request.format(BusinessIds, _Gps2.location_str) + "&shops-per-business=1")
        self.assertFragmentIn(
            response,
            {
                'id': 17203004,
                "feeds": [
                    {"feedId": 4004, "warehouseId": 4},
                ],
            },
            allow_different_len=False,
        )

    def test_only_express_surge(self):
        # Проверяем, что СУРЖ-данные складов попадают в выдачу если они указаны.
        EXPRESS_COMPRESSED = (
            EatsWarehousesEncoder()
            .add_warehouse(
                wh_id=4,
                delivery_time_minutes=10,
                delivery_price_min=0,
                delivery_price_max=150,
                free_delivery_threshold=1000,
                available_in_hours=9,
            )
            .encode()
        )
        BusinessId = 17203004

        response = self.report.request_json(
            "place=shop_info&fesh={}&hyperlocal-mode=only_express&eats-warehouses-compressed={}".format(
                BusinessId, EXPRESS_COMPRESSED
            )
        )

        self.assertFragmentIn(
            response,
            {
                'id': BusinessId,
                "feeds": [
                    {
                        "feedId": 4004,
                        "warehouseId": 4,
                        "availablaInHours": 9,
                        "deliveryPriceMax": 150,
                        "deliveryPriceMin": 0,
                        "deliveryTimeMinutes": 10,
                        "freeDeliveryThreshold": 1000,
                    }
                ],
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_shop_logos(cls):
        cls.index.shops += [
            Shop(
                fesh=100200,
                business_fesh=100500,
                business_logos=LogosInfo()
                .set_brand_color('#030405')
                .add_logo_url(
                    logo_type='square',
                    url='//avatars.ru/get-my-namespace/12345/first.jpg/orig',
                    img_width=120,
                    img_height=120,
                )
                .add_logo_params(
                    logo_type='with_name',
                    img_namespace='their-namespace',
                    img_group='54321',
                    img_key='2.png',
                    img_width=120,
                    img_height=180,
                )
                .set_shop_group("supermarket"),
            ),
            Shop(
                fesh=100201,
                business_fesh=100501,
            ),
            Shop(
                fesh=100202,
                business_fesh=100502,
            ),
        ]

        cls.index.business_logos.set_businnes_info(
            id=100501, logos_info=LogosInfo().set_brand_color('#123456').set_shop_group("micro")
        )

        cls.index.business_logos.set_businnes_info(
            id=100502,
            logos_info=LogosInfo().add_logo_url(
                logo_type='square',
                url='//avatars.ru/get-our-namespace/55555/hahaha123456/small',
                img_width=100,
                img_height=100,
            ),
        )

    def test_shop_logos(self):
        response = self.report.request_json("place=shop_info&fesh=100200,100201,100202")

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "shop",
                        "id": 100200,
                        "businessId": 100500,
                        "brandColor": "#030405",
                        "shopGroup": "supermarket",
                        "logos": [
                            {
                                "entity": "picture",
                                "logoType": "square",
                                "original": {
                                    "namespace": "my-namespace",
                                    "groupId": 12345,
                                    "key": "first.jpg",
                                    "width": 120,
                                    "height": 120,
                                },
                            },
                            {
                                "entity": "picture",
                                "logoType": "with_name",
                                "original": {
                                    "namespace": "their-namespace",
                                    "groupId": 54321,
                                    "key": "2.png",
                                    "width": 120,
                                    "height": 180,
                                },
                            },
                        ],
                    },
                    {
                        "entity": "shop",
                        "id": 100201,
                        "businessId": 100501,
                        "brandColor": "#123456",
                        "shopGroup": "micro",
                    },
                    {
                        "entity": "shop",
                        "id": 100202,
                        "businessId": 100502,
                        "logos": [
                            {
                                "entity": "picture",
                                "logoType": "square",
                                "original": {
                                    "namespace": "our-namespace",
                                    "groupId": 55555,
                                    "key": "hahaha123456",
                                    "width": 100,
                                    "height": 100,
                                },
                            },
                        ],
                    },
                ],
            },
            allow_different_len=False,
        )

    def test_shop_main_logo_set(self):
        logo_to_url = {
            "square": "http://avatars.mdst.yandex.net/get-my-namespace/12345/first.jpg/orig",
            "with_name": "http://avatars.mdst.yandex.net/get-their-namespace/54321/2.png/orig",
        }

        for logo, url in logo_to_url.items():
            response = self.report.request_json(
                "place=shop_info&fesh=100200&rearr-factors=market_business_logo_name={}".format(logo)
            )

            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "shop",
                            "id": 100200,
                            "logos": [
                                {
                                    "entity": "picture",
                                    "logoType": "square",
                                    "original": {
                                        "namespace": "my-namespace",
                                        "groupId": 12345,
                                        "key": "first.jpg",
                                        "width": 120,
                                        "height": 120,
                                    },
                                },
                                {
                                    "entity": "picture",
                                    "logoType": "with_name",
                                    "original": {
                                        "namespace": "their-namespace",
                                        "groupId": 54321,
                                        "key": "2.png",
                                        "width": 120,
                                        "height": 180,
                                    },
                                },
                            ],
                            "logo": url,
                        },
                    ],
                },
                allow_different_len=False,
            )

    def test_shop_main_logo_not_set(self):
        for rearr in [
            "&rearr-factors=market_business_logo_name=non_existent",
            "&rearr-factors=market_business_logo_name=",
        ]:
            response = self.report.request_json("place=shop_info&fesh=100200" + rearr)

            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {"entity": "shop", "id": 100200, "logo": Absent()},
                    ],
                },
                allow_different_len=False,
            )

    @classmethod
    def prepare_shop_is_eats(cls):
        cls.index.shops += [Shop(123456, is_eats=True)]

    def test_is_eats_shop(self):
        response = self.report.request_json("place=shop_info&fesh=123456")

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "shop",
                        "id": 123456,
                        "isEats": True,
                    }
                ]
            },
        )

    @classmethod
    def prepare_eats_shop_schedule(cls):
        schedule_data = [
            DynamicTimeIntervalsSet(key=1, intervals=[TimeIntervalInfo(TimeInfo(9, 0), TimeInfo(19, 0))]),
            DynamicTimeIntervalsSet(
                key=3,
                intervals=[
                    TimeIntervalInfo(TimeInfo(6, 0), TimeInfo(12, 0)),
                ],
            ),
            DynamicTimeIntervalsSet(
                key=6,
                intervals=[
                    TimeIntervalInfo(TimeInfo(12, 0), TimeInfo(14, 30)),
                ],
            ),
        ]

        warehouse_with_schedule_id = 10001

        cls.index.shops += [
            Shop(223456, is_eats=True, warehouse_id=warehouse_with_schedule_id, datafeed_id=warehouse_with_schedule_id)
        ]

        cls.index.express_warehouses.add(warehouse_with_schedule_id, region_id=None, work_schedule=schedule_data)

    def test_eats_shop_schedule(self):
        response = self.report.request_json("place=shop_info&fesh=223456")

        self.assertFragmentIn(
            response,
            {
                "id": 223456,
                "workScheduleList": [
                    {"day": 0, "from": {"hour": 9, "minute": 0}, "to": {"hour": 19, "minute": 0}},
                    {"day": 2, "from": {"hour": 6, "minute": 0}, "to": {"hour": 12, "minute": 0}},
                    {"day": 5, "from": {"hour": 12, "minute": 0}, "to": {"hour": 14, "minute": 30}},
                ],
            },
        )

    def test_shop_open_at(self):
        response = self.report.request_json(
            "place=shop_info&fesh=223456&rearr-factors=market_promo_datetime=20200412T000000"
        )

        self.assertFragmentIn(
            response,
            {
                "OpenAt": "2020-04-13T09:00",
            },
        )

        response = self.report.request_json(
            "place=shop_info&fesh=223456&rearr-factors=market_promo_datetime=20200413T090000"
        )

        self.assertFragmentNotIn(
            response,
            {
                "OpenAt",
            },
        )

        response = self.report.request_json(
            "place=shop_info&fesh=223456&rearr-factors=market_promo_datetime=20200413T200000"
        )

        self.assertFragmentIn(
            response,
            {
                "OpenAt": "2020-04-15T06:00",
            },
        )


if __name__ == '__main__':
    main()
