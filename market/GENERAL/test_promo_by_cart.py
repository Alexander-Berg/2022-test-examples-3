#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

import re

from core.testcase import TestCase, main
from core.types import (
    BlueOffer,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DynamicBlueGenericBundlesPromos,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
    FilterByHid,
    FilterByMsku,
    GLParam,
    GLType,
    HyperCategory,
    HyperCategoryType,
    MarketSku,
    MnPlace,
    Model,
    NavCategory,
    Offer,
    OfferDimensions,
    Promo,
    PromoByCart,
    PromoByCartSku,
    PromoMSKU,
    PromoType,
    RegionalDelivery,
    Shop,
)
from core.matcher import NoKey, Contains, Regex, with_capture, Absent, NotEmpty
from core.bigb import ModelLastOrderEvent, BeruModelOrderLastTimeCounter
from core.dj import DjModel

from core.types.autogen import b64url_md5
from core.types.offer_promo import PromoBlueCashback, PromoBlueSet, PromoDirectDiscount, PromoBlueFlash

from market.pylibrary.const.offer_promo import MechanicsPaymentType
from core.types.offer_promo import make_generic_bundle_content, OffersMatchingRules

from itertools import permutations


USE_DEPRECATED_DIRECT_SHIPPING_FLOW = "&rearr-factors=get_rid_of_direct_shipping=0"
EXTREQUEST_DJ_EMPTY_RECOM = 3788

UNDEFINED_WARE_MD5 = 'UNDEFINED_WARE_MD5_XXX'
INVALID_WARE_MD5 = 'INVALID_WARE_MD5'

DEFAULT_DJ_EXP = 'default_for_pricedrop'

blue_cashback_1 = Promo(
    promo_type=PromoType.BLUE_CASHBACK,
    description='blue_cashback_1_description',
    key=b64url_md5(1),
    blue_cashback=PromoBlueCashback(share=0.2, version=10, priority=1),
)

blue_promocode = Promo(
    promo_type=PromoType.PROMO_CODE,
    promo_code='promocode_text',
    description='promocode_description',
    discount_value=15,
    feed_id=1111,
    key=b64url_md5(2),
    url='http://promocode.com/',
    mechanics_payment_type=MechanicsPaymentType.CPA,
    shop_promo_id='promocode',
    conditions='conditions to buy',
)


def create_direct_discount(offerid, key, old_price, discount_price):
    return Promo(
        promo_type=PromoType.DIRECT_DISCOUNT,
        feed_id=1111,
        key=b64url_md5(key),
        url='http://direct_discount.com/',
        shop_promo_id='direct_discount' + str(key),
        direct_discount=PromoDirectDiscount(
            items=[
                {
                    'feed_id': 1111,
                    'offer_id': offerid,
                    'discount_price': {'value': discount_price, 'currency': 'RUR'},
                    'old_price': {'value': old_price, 'currency': 'RUR'},
                }
            ],
            allow_berubonus=True,
            allow_promocode=True,
        ),
    )


direct_discount_1 = create_direct_discount("coil-with-discount-1", 3, 200, 180)
direct_discount_2 = create_direct_discount("coil-with-discount-2", 4, 200, 180)

blue_flash = Promo(
    promo_type=PromoType.BLUE_FLASH,
    key=b64url_md5(5),
    url='http://яндекс.рф/',
    blue_flash=PromoBlueFlash(
        items=[{'feed_id': 1111, 'offer_id': "coil-blue-flash", 'price': {'value': 140, 'currency': 'RUR'}}],
        allow_berubonus=False,
        allow_promocode=False,
    ),
)

MULTIPROMO_QUERY = (
    'place=prime&hyperid={hyperid}&cart={cart}&allow-collapsing=0&rids=213&rgb=green_and_blue&rearr-factors='
    'market_enable_single_promo=0;market_promo_cart_discount_enable_any_rgb=1;market_promo_quantity_limit=3'
)


class T(TestCase):
    """
    MARKETOUT-24781
    Параметр cart-hid передается фронтом и чекаутером и содержит категории товаров в корзине пользователя
    Некоторые из категорий дают дополнительные скидки на товары из других категорий (см svn-data/package-data/promo-by-cart.tsv)
    Данные скидки должны применяться к только к офферам с пометкой "Товар предоставлен магазином Беру" (т.е. предоставляемые first-party поставщиком)
    """

    def setUp(self):
        # TODO: удалить setUp после возвращения дефолтных флагов в репорте
        TestCase.setUp(self)
        self.report.__request_json_original__ = self.report.request_json

        def request_json_decorator(query):
            rearr_factors = "&rearr-factors="
            rearrs_map = {
                'market_promo_by_user_cart_hids': '1',
                'market_promo_cart_force_pricedrop_return_nothing': '0',
            }
            for k, v in rearrs_map.items():
                if k not in query:
                    rearr_factors += ';{}={}'.format(k, v)
            return self.report.__request_json_original__(query + rearr_factors)

        self.report.request_json = request_json_decorator

    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.settings.set_default_reqid = False
        cls.settings.nordstream_autogenerate = False
        cls.settings.default_search_experiment_flags += ['market_nordstream=0']
        cls.settings.default_search_experiment_flags += ['enable_fast_promo_matcher=0']
        cls.settings.default_search_experiment_flags += ['enable_fast_promo_matcher_test=0']
        # НЕ делайте так в новых тестах!
        # Походов в КД на проде уже нет, пожалуйста, проверяйте новую функциональность, создавая доставку через комбинатор
        cls.settings.default_search_experiment_flags += ['force_disable_delivery_calculator_requests=0']

        # хиды которые дают скидку и хиды на которые дается скидка сохраняются в promo-by-cart.tsv
        # дочерние категории просто случайные дочерние категории
        # проверяем что дерево категорий правильно применяется при чтении промоакций
        cls.index.hypertree += [
            # велосипеды дают скидку 25% на замки для велосипедов (16044621 25728039    25)
            HyperCategory(hid=16044621, name="Велосипеды", output_type=HyperCategoryType.GURU),
            HyperCategory(hid=25728039, name="Замки для велосипедов", output_type=HyperCategoryType.GURU),
            # мелкая техника для кухни дает скидку 5% на моющие средства для кухни (13277088    4748072 5)
            HyperCategory(
                hid=13277088,
                name="Мелкая техника для кухни",
                output_type=HyperCategoryType.GURU,
                children=[
                    HyperCategory(hid=90586, name="Электрочайники и термопоты", output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=4954975, name="Мультиварки", output_type=HyperCategoryType.GURU),
                ],
            ),
            HyperCategory(
                hid=4748072,
                name="Моющие средства для кухни",
                output_type=HyperCategoryType.GURU,
                children=[
                    HyperCategory(hid=388881, name="Средства для мытья посуды", output_type=HyperCategoryType.GURU),
                    HyperCategory(
                        hid=399991, name="Средства для чистки кухонных поверхностей", output_type=HyperCategoryType.GURU
                    ),
                ],
            ),
            # категория на которую нет скидок
            HyperCategory(hid=12345, name="Электронные манки", output_type=HyperCategoryType.GURU),
            # категория на которую есть скидка т.к. она частотная (90401    15720388    5)
            HyperCategory(hid=15720388, name="Туалетная бумага", output_type=HyperCategoryType.GURU),
            # родительская категория дает скидку на дочернюю категорию
            HyperCategory(
                hid=934580,
                name='Товары для строительства и ремонта',
                output_type=HyperCategoryType.GURU,
                children=[
                    HyperCategory(hid=934581, name='Доски', output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=934582, name='Гвозди', output_type=HyperCategoryType.GURU),
                ],
            ),
        ]

        cls.index.promos_by_cart += [
            PromoByCart(cart_hid=16044621, promo_hid=25728039, percent=25),
            PromoByCart(cart_hid=13277088, promo_hid=4748072, percent=5, supplier=1),
            PromoByCart(cart_hid=PromoByCart.ANY_CATEGORY, promo_hid=15720388, percent=5),
            PromoByCart(cart_hid=934580, promo_hid=934581, percent=5),
            # в сплите s1 действует только одна скидка: велосипеды дают скидку на велозамки 5%
            PromoByCart(cart_hid=16044621, promo_hid=25728039, percent=5, split='s1'),
            # в сплите s2 действует скидка 25 на туалетку для 3p магазинов и 5% для магазинов беру
            PromoByCart(cart_hid=PromoByCart.ANY_CATEGORY, promo_hid=15720388, percent=5, split='s2', supplier=1),
            PromoByCart(cart_hid=PromoByCart.ANY_CATEGORY, promo_hid=15720388, percent=25, split='s2', supplier=3),
            # в сплите s3 и s4  модели promo_hid - номер вертикали, к которо товар относится
            # в этих тестах значение promo_hid не имеют значение, важно только то, разные они или одинаковы для двух товаров
            PromoByCart(cart_hid=16044621, promo_hid=15720388, percent=5, split='s3'),  # велосипед
            PromoByCart(cart_hid=4954975, promo_hid=15720388, percent=5, split='s3'),  # мультиварка
            PromoByCart(cart_hid=16044621, promo_hid=15720388, percent=5, split='s4'),
            PromoByCart(cart_hid=4954975, promo_hid=934581, percent=5, split='s4'),
        ]

        cls.index.navtree += [NavCategory(hid=12345, nid=12346, name="Электронные манки", is_blue=True)]

        cls.index.shops += [
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                name='ВиртуальныйМагазинНаБеру',
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
            ),
            Shop(
                fesh=1111,
                datafeed_id=1111,
                priority_region=213,
                regions=[225],
                name="Беру!",
                supplier_type=Shop.FIRST_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
            ),
            Shop(
                fesh=2222,
                datafeed_id=2222,
                priority_region=213,
                regions=[225],
                name="Беру!ЗаМКАДье",
                supplier_type=Shop.FIRST_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=147,
                fulfillment_program=True,
            ),
            Shop(
                fesh=3333,
                datafeed_id=3333,
                priority_region=213,
                regions=[225],
                name="РазмещаюсьНаБеру!",
                supplier_type=Shop.THIRD_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
            ),
            Shop(fesh=2, name="БелыйМагазин", priority_region=213, regions=[225]),
            Shop(
                fesh=4444,
                datafeed_id=4444,
                priority_region=213,
                regions=[225],
                name="КроссдокМагазин",
                supplier_type=Shop.THIRD_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=999,
                fulfillment_program=True,
                direct_shipping=False,
            ),
            Shop(
                fesh=5555,
                datafeed_id=5555,
                priority_region=213,
                regions=[225],
                name="ДропшипМагазин",
                supplier_type=Shop.THIRD_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=7,
                fulfillment_program=False,
            ),
            Shop(
                fesh=6666,
                datafeed_id=6666,
                priority_region=213,
                regions=[225],
                name="Click&Collect Магазин",
                supplier_type=Shop.THIRD_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=103,
                ignore_stocks=True,
                fulfillment_program=False,
            ),
            Shop(
                fesh=7777,
                datafeed_id=7777,
                priority_region=213,
                regions=[225],
                name="ДропшипМагазинСЦ",
                supplier_type=Shop.THIRD_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=777,
                fulfillment_program=False,
                direct_shipping=False,
            ),
        ]

        description = "Клиент всегда У! Подавай скидки ему..."

        def generate_randx(hyperid, shift):
            return (hyperid * 10 + shift) * 2

        def create_offers(hid, title, hyperid):
            cls.index.models += [Model(hid=hid, hyperid=hyperid, title=title)]
            cls.index.mskus += [
                MarketSku(
                    title=title + " от Беру и со скидкой",
                    descr=description,
                    hyperid=hyperid,
                    sku=hyperid * 10 + 1,
                    hid=hid,
                    delivery_buckets=[1234],
                    blue_offers=[
                        BlueOffer(
                            price=1300,
                            price_old=1500,
                            feedid=1111,
                            offerid="beru_model{}_Q".format(hyperid),
                            waremd5="BLUEModel{}FEED1111QQQQ".format(hyperid)[0:22],
                            randx=generate_randx(hyperid, 1),
                        )
                    ],
                ),
                MarketSku(
                    title=title + " от Беру",
                    descr=description,
                    hyperid=hyperid,
                    sku=hyperid * 10 + 2,
                    hid=hid,
                    delivery_buckets=[1234],
                    blue_offers=[
                        BlueOffer(
                            price=1400,
                            feedid=1111,
                            offerid="beru_model{}_g".format(hyperid),
                            waremd5="BLUEModel{}FEED1111gggg".format(hyperid)[0:22],
                            randx=generate_randx(hyperid, 2),
                        )
                    ],
                ),
                MarketSku(
                    title=title + " от Беру за МКАДом",
                    descr=description,
                    hyperid=hyperid,
                    sku=hyperid * 10 + 3,
                    hid=hid,
                    delivery_buckets=[1234],
                    blue_offers=[
                        BlueOffer(
                            price=1450,
                            feedid=2222,
                            offerid="beru_mkad_model{}_f".format(hyperid),
                            waremd5="BLUEModel{}FEED2222dddQ".format(hyperid)[0:22],
                            randx=generate_randx(hyperid, 3),
                        )
                    ],
                ),
                MarketSku(
                    title=title + " от РазмещаюсьНаБеру",
                    descr=description,
                    hyperid=hyperid,
                    sku=hyperid * 10 + 4,
                    hid=hid,
                    delivery_buckets=[1234],
                    blue_offers=[
                        BlueOffer(
                            price=1500,
                            feedid=3333,
                            offerid="blueshop_model{}_w".format(hyperid),
                            waremd5="BLUEModel{}FEED3333wwww".format(hyperid)[0:22],
                            randx=generate_randx(hyperid, 4),
                        )
                    ],
                ),
                MarketSku(
                    title=title + " от КроссдокМагазин",
                    descr=description,
                    hyperid=hyperid,
                    sku=hyperid * 10 + 5,
                    hid=hid,
                    delivery_buckets=[1234],
                    blue_offers=[
                        BlueOffer(
                            price=1200,
                            feedid=4444,
                            offerid="crossdock_model{}_w".format(hyperid),
                            waremd5="BLUEModel{}FEED4444xxxw".format(hyperid)[0:22],
                            randx=generate_randx(hyperid, 5),
                        )
                    ],
                ),
                MarketSku(
                    title=title + " от ДропшипМагазин",
                    descr=description,
                    hyperid=hyperid,
                    sku=hyperid * 10 + 6,
                    hid=hid,
                    delivery_buckets=[1234],
                    blue_offers=[
                        BlueOffer(
                            price=1200,
                            feedid=5555,
                            offerid="dropship_model{}_w".format(hyperid),
                            waremd5="BLUEModel{}FEED5555dddQ".format(hyperid)[0:22],
                            randx=generate_randx(hyperid, 6),
                        )
                    ],
                ),
                MarketSku(
                    title=title + " от Click&Collect",
                    descr=description,
                    hyperid=hyperid,
                    sku=hyperid * 10 + 7,
                    hid=hid,
                    delivery_buckets=[1234],
                    blue_offers=[
                        BlueOffer(
                            price=1200,
                            feedid=6666,
                            offerid="cnc_model{}_w".format(hyperid),
                            waremd5="BLUEModel{}FEED6666cccQ".format(hyperid)[0:22],
                            randx=generate_randx(hyperid, 7),
                        )
                    ],
                ),
            ]
            cls.index.offers += [
                Offer(
                    title=title + " от Белого Магазина",
                    price=1000,
                    descr=description,
                    hyperid=hyperid,
                    hid=hid,
                    fesh=2,
                    waremd5="WHITE-Model{}-FESH-2---".format(hyperid)[0:22],
                )
            ]

        def create_extra_offers(hid, title, hyperid):
            cls.index.mskus += [
                MarketSku(
                    title=title + " от ДропшипМагазинСЦ",
                    descr="Описание",
                    hyperid=hyperid,
                    sku=hyperid * 10 + 8,
                    hid=hid,
                    delivery_buckets=[1234],
                    blue_offers=[
                        BlueOffer(
                            price=1200,
                            feedid=7777,
                            offerid="dropship_sc_model{}_w".format(hyperid),
                            waremd5="BLUEModel{}FEED7777eeeQ".format(hyperid)[0:22],
                            randx=generate_randx(hyperid, 8),
                        )
                    ],
                ),
            ]

        create_offers(hid=16044621, title="Велосипед", hyperid=1)
        create_extra_offers(hid=16044621, title="Велосипед", hyperid=1)
        create_offers(hid=25728039, title="Замок для велосипеда", hyperid=2)
        create_extra_offers(hid=25728039, title="Замок для велосипеда", hyperid=2)
        create_offers(hid=90586, title="Электрочайник", hyperid=3)
        create_offers(hid=4954975, title="Мультиварка", hyperid=4)
        create_offers(hid=388881, title="Фейри", hyperid=5)
        create_offers(hid=399991, title="Мистер Мускул", hyperid=6)
        create_offers(hid=12345, title="Манок на уток", hyperid=7)
        create_offers(hid=15720388, title="Туалетка", hyperid=8)
        create_extra_offers(hid=15720388, title="Туалетка", hyperid=8)
        create_offers(hid=934581, title="Доска половая", hyperid=9)
        create_offers(hid=934582, title="Гвозди 100 шт", hyperid=10)

        cls.settings.lms_autogenerate = False

        def create_warehouse_and_delivery_info(warehouse_id, delivery_service_id, date_switch_hour=23, region_to=225):
            return DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=warehouse_id,
                delivery_service_id=delivery_service_id,
                date_switch_time_infos=[
                    DateSwitchTimeAndRegionInfo(date_switch_hour=date_switch_hour, region_to=region_to)
                ],
            )

        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=2),
            DynamicWarehouseInfo(id=147, home_region=213),
            DynamicWarehouseInfo(id=999, home_region=213),
            DynamicWarehouseInfo(id=777, home_region=213),
            DynamicWarehouseInfo(id=7, home_region=213),
            DynamicWarehouseInfo(id=103, home_region=213),
            DynamicWarehouseToWarehouseInfo(warehouse_from=999, warehouse_to=147),
            DynamicWarehouseToWarehouseInfo(warehouse_from=777, warehouse_to=147),
            DynamicWarehouseToWarehouseInfo(warehouse_from=7, warehouse_to=147),
            DynamicWarehouseToWarehouseInfo(warehouse_from=145, warehouse_to=145),
            DynamicWarehouseToWarehouseInfo(warehouse_from=147, warehouse_to=147),
            DynamicWarehouseToWarehouseInfo(warehouse_from=103, warehouse_to=103),
            create_warehouse_and_delivery_info(145, 157),
            create_warehouse_and_delivery_info(147, 157),
            create_warehouse_and_delivery_info(999, 157),
            create_warehouse_and_delivery_info(7, 157),
            create_warehouse_and_delivery_info(103, 157),
            DynamicDeliveryServiceInfo(id=157, rating=2),
            DynamicWarehousesPriorityInRegion(
                region=225,
                warehouses=[
                    145,
                    147,
                    999,
                    777,
                    7,
                    103,
                ],
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=1234,
                dc_bucket_id=1234,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(rid=225, options=[DeliveryOption(price=15, day_from=1, day_to=2)]),
                ],
            )
        ]

        cls.dj.on_request(exp=DEFAULT_DJ_EXP, yandexuid='').respond([])

        cls.dj.on_request(exp=DEFAULT_DJ_EXP, yandexuid='', uuid='da65f28c666d4dc3b7a8f47b62d3cfa').respond([])

        cls.dj.on_request(exp=DEFAULT_DJ_EXP, yandexuid='24846006', max_count=2).respond(
            [
                DjModel(id='8'),
                DjModel(id='5'),
            ]
        )

        cls.dj.on_request(exp=DEFAULT_DJ_EXP, yandexuid='1348', max_count=2).respond(
            [
                DjModel(id='8', attributes={"accessory": "1", "jointPurchase": "1"}),
                DjModel(id='5'),
            ]
        )

        cls.dj.on_request(exp=DEFAULT_DJ_EXP, yandexuid='24846006', max_count=1).respond(
            [
                DjModel(id='5'),
            ]
        )

    @classmethod
    def prepare_tests_for_parameters_passing(cls):
        cls.index.hypertree += [
            HyperCategory(hid=14333188, name="Мыши", output_type=HyperCategoryType.GURU),
            HyperCategory(hid=91075, name="Коврики для мышей", output_type=HyperCategoryType.GURU),
            HyperCategory(hid=14334315, name="Клавиатуры", output_type=HyperCategoryType.GURU),
        ]

        cls.index.promos_by_cart += [
            PromoByCart(cart_hid=14333188, promo_hid=91075, percent=5),
            PromoByCart(cart_hid=14333188, promo_hid=14334315, percent=10),
        ]

        cls.index.shops += [
            Shop(
                fesh=9999,
                datafeed_id=9999,
                priority_region=213,
                regions=[225],
                name="Покупки",
                supplier_type=Shop.FIRST_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
            ),
        ]

        description = "Покупки на Маркете"

        def create_offers(hid, title, hyperid):
            cls.index.models += [Model(hid=hid, hyperid=hyperid, title=title)]
            cls.index.mskus += [
                MarketSku(
                    title=title + " на Маркете",
                    descr=description,
                    hyperid=hyperid,
                    sku=hyperid * 10 + 2,
                    hid=hid,
                    delivery_buckets=[1234],
                    blue_offers=[
                        BlueOffer(
                            price=1400,
                            feedid=8888,
                            offerid="mrkt_model{}_e".format(hyperid),
                            waremd5="BLUEModel{}FEED9999eeee".format(hyperid)[0:22],
                            randx=(hyperid * 10 + 2) * 2,
                        )
                    ],
                ),
            ]

        create_offers(hid=14333188, title="Мышь Logitech", hyperid=41)
        create_offers(hid=91075, title="Клавиатура Logitech", hyperid=42)
        create_offers(hid=14334315, title="Коврик для мыши Logitech", hyperid=43)

        hids = [91075, 14334315, 15720388]
        percents = [5, 10, 5]
        for perm in permutations(list(zip(hids, percents))):
            current_hids = ",".join((str(hid) for hid, _ in perm))
            current_percents = ",".join((str(percent) for _, percent in perm))
            cls.dj.on_request(
                exp=DEFAULT_DJ_EXP,
                yandexuid='251539001',
                hid=current_hids,
                promo_cart_discount_percent=current_percents,
            ).respond([DjModel(id='42')])
        cls.dj.on_request(exp=DEFAULT_DJ_EXP, yandexuid='251539001').respond([DjModel(id='43')])

    class Expected:
        @staticmethod
        def offer_with_twice_discount(title, price, merch_price, old_price, discount, old_discount, promo):
            return {
                "entity": "offer",
                "titles": {"raw": title},
                "prices": {
                    "value": str(price),
                    "isDeliveryIncluded": False,
                    "discount": {
                        "oldMin": str(old_price),
                        "percent": discount,
                    },
                    "oldDiscount": {
                        "oldMin": str(merch_price),
                        "percent": old_discount,
                    },
                },
                "promos": [promo] if promo else NoKey("promos"),
            }

        @staticmethod
        def offer_with_simple_discount(title, price, old_price, discount, promo, sku=None):
            data = {
                "entity": "offer",
                "titles": {"raw": title},
                "prices": {
                    "value": str(price),
                    "isDeliveryIncluded": False,
                    "discount": {
                        "oldMin": str(old_price),
                        "percent": discount,
                    },
                },
                "promos": [promo] if promo else NoKey("promos"),
            }
            if sku:
                data["sku"] = str(sku)

            return data

        @staticmethod
        def offer_without_discount(title, price, promo, sku=None):
            data = {
                "entity": "offer",
                "titles": {"raw": title},
                "prices": {
                    "value": str(price),
                    "isDeliveryIncluded": False,
                    "discount": NoKey("discount"),
                    "oldDiscount": NoKey("oldDiscount"),
                },
                "promos": [promo] if promo else NoKey("promos"),
            }
            if sku:
                data["sku"] = str(sku)
            return data

        @staticmethod
        def offer_without_pricedrop_promo():
            data = {
                "entity": "offer",
                "promos": NoKey("promos"),
            }
            return data

    class ExpectedNoPromo(Expected):
        '''Офферы от Беру, от Беру!ЗаМКАДом, от РазмещаюсьНаБеру и от Белого Магазина без промо-скидок'''

        @staticmethod
        def offer_from_beru_with_discount(title):
            # оффер который продавался со скидкой
            return T.Expected.offer_with_simple_discount(title + " от Беру и со скидкой", 1300, 1500, 13, None)

        @staticmethod
        def offer_from_beru(title):
            # оффер от беру без скидки
            return T.Expected.offer_without_discount(title + " от Беру", 1400, None)

        @staticmethod
        def offer_from_beru_147(title):
            # оффер от беру со 147 склада без скидки
            return T.Expected.offer_without_discount(title + " от Беру за МКАДом", 1450, None)

        @staticmethod
        def offer_from_other_blue_shop(title):
            # оффер от стороннего поставщика без скидки - ничего не получает
            return T.Expected.offer_without_discount(title + " от РазмещаюсьНаБеру", 1500, None)

        @staticmethod
        def offer_from_white_shop(title):
            # оффер от белого магазина без скидки - ничего не получает
            return T.Expected.offer_without_discount(title + " от Белого Магазина", 1000, None)

        @staticmethod
        def offer_from_crossdoc(title):
            # оффер от стороннего поставщики (3p) следующий через склад Беру
            return T.Expected.offer_without_discount(title + " от КроссдокМагазин", 1200, None)

        @staticmethod
        def offer_from_dropship(title):
            # оффер от стороннего поставщики (3p) следующий напрямую со склада поставщика
            return T.Expected.offer_without_discount(title + " от ДропшипМагазин", 1200, None)

        @staticmethod
        def offer_from_cnc(title):
            # оффер от стороннего поставщики (3p) который можно забронировать и забрать в магазине
            return T.Expected.offer_without_discount(title + " от Click&Collect", 1200, None)

    class Expected25percentDiscount(Expected):
        @staticmethod
        def offer_from_beru_with_discount(title, promo):
            # оффер который продавался со скидкой - получает oldDiscount
            return T.Expected.offer_with_twice_discount(title + " от Беру и со скидкой", 975, 1300, 1500, 35, 13, promo)

        @staticmethod
        def offer_from_beru(title, promo):
            # оффер от беру без скидки получает просто discount
            return T.Expected.offer_with_simple_discount(title + " от Беру", 1050, 1400, 25, promo)

        @staticmethod
        def offer_from_beru_147(title, promo):
            # оффер от беру со 147 склада получает discount (при покупке товара с того же склада)
            return T.Expected.offer_with_simple_discount(title + " от Беру за МКАДом", 1087, 1450, 25, promo)

        @staticmethod
        def offer_from_other_blue_shop(title, promo):
            # оффер от стороннего поставщика (3p) тоже может получить скидку
            return T.Expected.offer_with_simple_discount(title + " от РазмещаюсьНаБеру", 1125, 1500, 25, promo)

        @staticmethod
        def offer_from_crossdoc(title, promo):
            # оффер от стороннего поставщики (3p) следующий через склад Беру
            return T.Expected.offer_with_simple_discount(title + " от КроссдокМагазин", 900, 1200, 25, promo)

        @staticmethod
        def offer_from_dropship(title, promo):
            # оффер от стороннего поставщики (3p) следующий напрямую со склада поставщика
            return T.Expected.offer_with_simple_discount(title + " от ДропшипМагазин", 900, 1200, 25, promo)

        @staticmethod
        def offer_from_dropship_sc(title, promo):
            return T.Expected.offer_with_simple_discount(title + " от ДропшипМагазинСЦ", 900, 1200, 25, promo)

        @staticmethod
        def offer_from_cnc(title, promo):
            # оффер от стороннего поставщики (3p) который можно забронировать и забрать в магазине
            return T.Expected.offer_with_simple_discount(title + " от Click&Collect", 900, 1200, 25, promo)

    class ExpectedVelo25:
        '''Товары с ожидаемой скидкой 25%'''

        promo = {
            "type": "cart-discount",
            "key": "promo_25percent_in_hid25728039_for_1p_if_hid16044621_in_cart",
            "title": "Специальное предложение",
            "description": "Скидка 25% на товары из категории \"Замки для велосипедов\" при покупке товара из категории \"Велосипеды\"",
            "cartDiscount": {"discount": {"percent": 25}},
            "onlyPromoCategory": {"hid": 25728039},
        }

        @staticmethod
        def offer_from_beru_with_discount(title):
            return T.Expected25percentDiscount.offer_from_beru_with_discount(title, T.ExpectedVelo25.promo)

        @staticmethod
        def offer_from_beru(title):
            return T.Expected25percentDiscount.offer_from_beru(title, T.ExpectedVelo25.promo)

        @staticmethod
        def offer_from_beru_147(title):
            return T.Expected25percentDiscount.offer_from_beru_147(title, T.ExpectedVelo25.promo)

        @staticmethod
        def offer_from_crossdoc(title):
            return T.Expected25percentDiscount.offer_from_crossdoc(title, T.ExpectedVelo25.promo)

        @staticmethod
        def offer_from_dropship(title):
            return T.Expected25percentDiscount.offer_from_dropship(title, T.ExpectedVelo25.promo)

        @staticmethod
        def offer_from_cnc(title):
            return T.Expected25percentDiscount.offer_from_cnc(title, T.ExpectedVelo25.promo)

    class Expected5percentDiscount:
        @staticmethod
        def offer_from_beru_with_discount(
            title, promo, price=1235, merch_price=1300, old_price=1500, discount=18, old_discount=13
        ):
            # оффер который продавался со скидкой - получает oldDiscount
            return T.Expected.offer_with_twice_discount(
                title + " от Беру и со скидкой", price, merch_price, old_price, discount, old_discount, promo
            )

        @staticmethod
        def offer_from_beru(title, promo, price=1330, old_price=1400, discount=5):
            # оффер от беру без скидки получает просто discount
            return T.Expected.offer_with_simple_discount(title + " от Беру", price, old_price, discount, promo)

        @staticmethod
        def offer_from_beru_147(title, promo, price=1377, old_price=1450, discount=5):
            # оффер от беру со 147 склада получает discount (при покупке товара с того же склада)
            return T.Expected.offer_with_simple_discount(
                title + " от Беру за МКАДом", price, old_price, discount, promo
            )

        @staticmethod
        def offer_from_other_blue_shop(title, promo, price=1425, old_price=1500, discount=5):
            # оффер от стороннего поставщика (3p) тоже может получить скидку
            return T.Expected.offer_with_simple_discount(
                title + " от РазмещаюсьНаБеру", price, old_price, discount, promo
            )

        @staticmethod
        def offer_from_crossdoc(title, promo, price=1140, old_price=1200, discount=5):
            # оффер от стороннего поставщики (3p) следующий через склад Беру
            return T.Expected.offer_with_simple_discount(
                title + " от КроссдокМагазин", price, old_price, discount, promo
            )

        @staticmethod
        def offer_from_dropship(title, promo, price=1140, old_price=1200, discount=5):
            # оффер от стороннего поставщики (3p) следующий напрямую со склада поставщика
            return T.Expected.offer_with_simple_discount(
                title + " от ДропшипМагазин", price, old_price, discount, promo
            )

        @staticmethod
        def offer_from_dropship_sc(title, promo, price=1140, old_price=1200, discount=5):
            return T.Expected.offer_with_simple_discount(
                title + " от ДропшипМагазинСЦ", price, old_price, discount, promo
            )

        @staticmethod
        def offer_from_cnc(title, promo, price=1140, old_price=1200, discount=5):
            # оффер от стороннего поставщики (3p) который можно забронировать и забрать в магазине
            return T.Expected.offer_with_simple_discount(title + " от Click&Collect", price, old_price, discount, promo)

    class ExpectedKitchen5:
        promo = {
            "type": "cart-discount",
            "key": "promo_5percent_in_hid4748072_for_1p_if_hid13277088_in_cart",
            "title": "Специальное предложение",
            "description": "Скидка 5% на товары из категории \"Моющие средства для кухни\" при покупке товара из категории \"Мелкая техника для кухни\"",
            "cartDiscount": {"discount": {"percent": 5}},
            "onlyPromoCategory": {"hid": 4748072},
        }

        @staticmethod
        def offer_from_beru_with_discount(title):
            return T.Expected5percentDiscount.offer_from_beru_with_discount(title, T.ExpectedKitchen5.promo)

        @staticmethod
        def offer_from_beru(title):
            return T.Expected5percentDiscount.offer_from_beru(title, T.ExpectedKitchen5.promo)

        @staticmethod
        def offer_from_beru_147(title):
            return T.Expected5percentDiscount.offer_from_beru_147(title, T.ExpectedKitchen5.promo)

    class ExpectedToilet5:
        promo = {
            "type": "cart-discount",
            "key": "promo_5percent_in_hid15720388_for_1p_if_hid90401_in_cart",
            "title": "Специальное предложение",
            "description": "Скидка 5% на товары из категории \"Туалетная бумага\" при покупке товара из категории \"All goods\"",
            "cartDiscount": {"discount": {"percent": 5}},
            "onlyPromoCategory": {"hid": 15720388},
        }

        @staticmethod
        def offer_from_beru_with_discount(title):
            return T.Expected5percentDiscount.offer_from_beru_with_discount(title, T.ExpectedToilet5.promo)

        @staticmethod
        def offer_from_beru(title):
            return T.Expected5percentDiscount.offer_from_beru(title, T.ExpectedToilet5.promo)

        @staticmethod
        def offer_from_beru_147(title):
            return T.Expected5percentDiscount.offer_from_beru_147(title, T.ExpectedToilet5.promo)

    class ExpectedDoski5:
        promo = {
            "type": "cart-discount",
            "key": "promo_5percent_in_hid934581_for_1p_if_hid934580_in_cart",
            "title": "Специальное предложение",
            "description": "Скидка 5% на товары из категории \"Доски\" при покупке товара из категории \"Товары для строительства и ремонта\"",
            "cartDiscount": {"discount": {"percent": 5}},
            "onlyPromoCategory": {"hid": 934581},
        }

        @staticmethod
        def offer_from_beru_with_discount(title):
            return T.Expected5percentDiscount.offer_from_beru_with_discount(title, T.ExpectedDoski5.promo)

        @staticmethod
        def offer_from_beru(title):
            return T.Expected5percentDiscount.offer_from_beru(title, T.ExpectedDoski5.promo)

        @staticmethod
        def offer_from_beru_147(title):
            return T.Expected5percentDiscount.offer_from_beru_147(title, T.ExpectedDoski5.promo)

    class ExpectedVelo5s1:
        # товары с ожидаемой скидкой 5% в сплите s1
        promo = {
            "type": "cart-discount",
            "key": "promo_5percent_in_hid25728039_for_1p_if_hid16044621_in_cart_split_s1",
            "title": "Специальное предложение",
            "description": "Скидка 5% на товары из категории \"Замки для велосипедов\" при покупке товара из категории \"Велосипеды\"",
            "cartDiscount": {"discount": {"percent": 5}},
            "onlyPromoCategory": {"hid": 25728039},
        }

        @staticmethod
        def offer_from_beru_with_discount(title):
            return T.Expected5percentDiscount.offer_from_beru_with_discount(title, T.ExpectedVelo5s1.promo)

        @staticmethod
        def offer_from_beru(title):
            return T.Expected5percentDiscount.offer_from_beru(title, T.ExpectedVelo5s1.promo)

        @staticmethod
        def offer_from_beru_147(title):
            return T.Expected5percentDiscount.offer_from_beru_147(title, T.ExpectedVelo5s1.promo)

    class ExpectedVeloUniversal5s1:
        # товары с ожидаемой скидкой 5% в сплите s1
        promo = {
            "type": "cart-discount",
            "key": "promo_5_percent_in_any_hid_in_cart",
            "title": "Специальное предложение",
            "description": "Скидка 5% на товары из любой категории, которую вернул DJ при покупке товара из любой категории",
            "cartDiscount": {"discount": {"percent": 5}},
        }

        @staticmethod
        def offer_from_beru_with_discount(title):
            return T.Expected5percentDiscount.offer_from_beru_with_discount(title, T.ExpectedVeloUniversal5s1.promo)

        @staticmethod
        def offer_from_beru(title):
            return T.Expected5percentDiscount.offer_from_beru(title, T.ExpectedVeloUniversal5s1.promo)

        @staticmethod
        def offer_from_beru_147(title):
            return T.Expected5percentDiscount.offer_from_beru_147(title, T.ExpectedVeloUniversal5s1.promo)

        @staticmethod
        def offer_from_other_blue_shop(title):
            return T.Expected5percentDiscount.offer_from_other_blue_shop(title, T.ExpectedVeloUniversal5s1.promo)

        @staticmethod
        def offer_from_crossdoc(title):
            return T.Expected5percentDiscount.offer_from_crossdoc(title, T.ExpectedVeloUniversal5s1.promo)

        @staticmethod
        def offer_from_dropship(title):
            return T.Expected5percentDiscount.offer_from_dropship(title, T.ExpectedVeloUniversal5s1.promo)

        @staticmethod
        def offer_from_dropship_sc(title):
            return T.Expected5percentDiscount.offer_from_dropship_sc(title, T.ExpectedVeloUniversal5s1.promo)

        @staticmethod
        def offer_from_cnc(title):
            return T.Expected5percentDiscount.offer_from_cnc(title, T.ExpectedVeloUniversal5s1.promo)

    class ExpectedToilets25for3p:
        # в сплите s2 задана скидка для 3p товаров 25% а для 1p - 5%
        promo = {
            "type": "cart-discount",
            "key": "promo_25percent_in_hid15720388_for_3p_if_hid90401_in_cart_split_s2",
            "title": "Специальное предложение",
            "description": "Скидка 25% на товары из категории \"Туалетная бумага\" при покупке товара из категории \"All goods\"",
            "cartDiscount": {"discount": {"percent": 25}},
            "onlyPromoCategory": {"hid": 15720388},
        }

        @staticmethod
        def offer_from_other_blue_shop(title):
            return T.Expected25percentDiscount.offer_from_other_blue_shop(title, T.ExpectedToilets25for3p.promo)

        @staticmethod
        def offer_from_crossdoc(title):
            return T.Expected25percentDiscount.offer_from_crossdoc(title, T.ExpectedToilets25for3p.promo)

        @staticmethod
        def offer_from_dropship(title):
            return T.Expected25percentDiscount.offer_from_dropship(title, T.ExpectedToilets25for3p.promo)

        @staticmethod
        def offer_from_dropship_sc(title):
            return T.Expected25percentDiscount.offer_from_dropship_sc(title, T.ExpectedToilets25for3p.promo)

    class ExpectedToilets5for1p:
        # в сплите s2 задана скидка для 3p товаров 25% а для 1p - 5%
        promo = {
            "type": "cart-discount",
            "key": "promo_5percent_in_hid15720388_for_1p_if_hid90401_in_cart_split_s2",
            "title": "Специальное предложение",
            "description": "Скидка 5% на товары из категории \"Туалетная бумага\" при покупке товара из категории \"All goods\"",
            "cartDiscount": {"discount": {"percent": 5}},
            "onlyPromoCategory": {"hid": 15720388},
        }

        @staticmethod
        def offer_from_beru_with_discount(title):
            return T.Expected5percentDiscount.offer_from_beru_with_discount(title, T.ExpectedToilets5for1p.promo)

        @staticmethod
        def offer_from_beru(title):
            return T.Expected5percentDiscount.offer_from_beru(title, T.ExpectedToilets5for1p.promo)

        @staticmethod
        def offer_from_beru_147(title):
            return T.Expected5percentDiscount.offer_from_beru_147(title, T.ExpectedToilets5for1p.promo)

    def _check_yasm_hgram_signals(self, tass_data_before, metrics_increment_dict):
        """
        Проверям инкремент в соответсвущем бакете
        """
        tass_data_after = self.report.request_tass()

        for metric_name in metrics_increment_dict:
            self.assertIn(metric_name, tass_data_after.keys())

            bucket_key, expected_increment = metrics_increment_dict[metric_name]

            bucket_dict_before = dict(tass_data_before.get(metric_name, [[bucket_key, 0]]))
            expected_bucket = [bucket_key, bucket_dict_before[bucket_key] + expected_increment]
            self.assertIn(
                expected_bucket,
                tass_data_after.get(metric_name, []),
            )

    class RegexUnicodeFriendly(Regex):
        @with_capture
        def value_match_reason(self, value):
            return re.search(self.pattern, value) is not None, [
                'Assertion failed: value does not match regex "{}"'.format(self.pattern)
            ]

    def test_no_discount(self):
        """Состояние вне эксперимента - оффер имеющий скидку имеет блок discount, остальные офферы без скидки, промоакций ни у кого нет"""

        zamok = "Замок для велосипеда"

        # оффер в корзине BLUEModel1FEED1111QQQQ - велосипед из категории 16044621

        # на синем без параметра &cart скидка не применяется
        response = self.report.request_json(
            'place=prime&text=замок+для+велосипеда&rids=213&hid=25728039&rgb=blue&allow-collapsing=0'
            '&rearr-factors=market_metadoc_search=no'
        )

        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru_with_discount(zamok))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru(zamok))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_other_blue_shop(zamok))

        # на белом без параметра &cart= скидка не применяется
        response = self.report.request_json(
            'place=prime&text=замок+для+велосипеда&rids=213&hid=25728039&allow-collapsing=0'
            '&rearr-factors=market_metadoc_search=no'
        )

        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru_with_discount(zamok))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru(zamok))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_other_blue_shop(zamok))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_white_shop(zamok))

    def test_discounts_by_cart(self):
        """Проверяем как отображаются скидки на товарах со скидкой и без при включенных промоакциях"""

        # оффер в корзине BLUEModel1FEED1111QQQQ - велосипед из категории 16044621 со склада 145
        response = self.report.request_json(
            'place=prime&text=замок+для+велосипеда&rids=213&hid=25728039&cart=BLUEModel1FEED1111QQQQ&rgb=blue&&allow-collapsing=0'
        )

        zamok = "Замок для велосипеда"

        # промо дает дополнительные скидки для офферов с Беру с того же склада (145)
        self.assertFragmentIn(response, T.ExpectedVelo25.offer_from_beru_with_discount(zamok))
        self.assertFragmentIn(response, T.ExpectedVelo25.offer_from_beru(zamok))
        # оффер от Беру со склада 147 не получает скидки
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru_147(zamok))
        # оффер от 3p магазина не получает скидки
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_other_blue_shop(zamok))

    def test_discounts_by_cart_white(self):
        """Скидка есть на белом под флагом market_promo_cart_discount_enable_any_rgb"""

        # оффер в корзине BLUEModel1FEED1111QQQQ - велосипед из категории 16044621 со склада 145
        response = self.report.request_json(
            'place=prime&text=замок+для+велосипеда&rids=213&hid=25728039&cart=BLUEModel1FEED1111QQQQ&rgb=green_with_blue&allow-collapsing=0'
            '&rearr-factors=market_metadoc_search=no'
        )

        zamok = "Замок для велосипеда"

        self.assertFragmentIn(response, T.ExpectedVelo25.offer_from_beru_with_discount(zamok))
        self.assertFragmentIn(response, T.ExpectedVelo25.offer_from_beru(zamok))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru_147(zamok))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_other_blue_shop(zamok))

    def test_discounts_by_cart_disabled(self):
        """Параметр promo-by-user-cart-hids=0 выключает pricedrop"""

        # оффер в корзине BLUEModel1FEED1111QQQQ - велосипед из категории 16044621 со склада 145
        response = self.report.request_json(
            'place=prime&text=замок+для+велосипеда&rids=213&hid=25728039&cart=BLUEModel1FEED1111QQQQ&rgb=blue&&allow-collapsing=0'
            '&promo-by-user-cart-hids=0'
        )

        zamok = "Замок для велосипеда"

        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru_with_discount(zamok))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru(zamok))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru_147(zamok))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_other_blue_shop(zamok))

        # Проверяем, что флаг market_promo_by_user_cart_hids отключает pricedrop всюду
        response = self.report.request_json(
            'place=prime&text=замок+для+велосипеда&rids=213&hid=25728039&cart=BLUEModel1FEED1111QQQQ&rgb=blue&&allow-collapsing=0'
            '&promo-by-user-cart-hids=1&rearr-factors=market_promo_by_user_cart_hids=0'
        )

        zamok = "Замок для велосипеда"

        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru_with_discount(zamok))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru(zamok))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru_147(zamok))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_other_blue_shop(zamok))

        response = self.report.request_json(
            'place=prime&text=замок+для+велосипеда&rids=213&hid=25728039&cart=BLUEModel1FEED1111QQQQ&rgb=blue&&allow-collapsing=0'
            '&promo-by-user-cart-hids=0&rearr-factors=market_promo_by_user_cart_hids=1'
        )

        zamok = "Замок для велосипеда"

        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru_with_discount(zamok))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru(zamok))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru_147(zamok))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_other_blue_shop(zamok))

    def test_discounts_by_cart_selected(self):
        """
        Проверяем что при включенном rear market_promo_cart_special_places_exp
        pricedrop работает только на страницах, в которые проброшен promo-by-cart-enabled
        """
        request_tpl = (
            'place=prime&text=замок+для+велосипеда&rids=213&hid=25728039&cart=BLUEModel1FEED1111QQQQ&'
            'rgb=blue&rearr-factors=market_promo_by_user_cart_hids=1;market_promo_cart_special_places_exp=1'
            '&promo-by-cart-in-special-places-only=1'
            '&allow-collapsing=0'
            '{params}'
        )
        zamok = "Замок для велосипеда"

        response = self.report.request_json(request_tpl.format(params=''))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru_with_discount(zamok))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru(zamok))

        response = self.report.request_json(request_tpl.format(params='&promo-by-cart-enabled=1'))
        self.assertFragmentIn(response, T.ExpectedVelo25.offer_from_beru_with_discount(zamok))
        self.assertFragmentIn(response, T.ExpectedVelo25.offer_from_beru(zamok))

        response = self.report.request_json(request_tpl.format(params='&promo-by-cart-mskus=22'))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru_with_discount(zamok))
        self.assertFragmentIn(response, T.ExpectedVelo25.offer_from_beru(zamok))

        response = self.report.request_json(request_tpl.format(params='&promo-by-cart-mskus=21'))
        self.assertFragmentIn(response, T.ExpectedVelo25.offer_from_beru_with_discount(zamok))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru(zamok))

        response = self.report.request_json(request_tpl.format(params='&promo-by-cart-mskus=22,21'))
        self.assertFragmentIn(response, T.ExpectedVelo25.offer_from_beru_with_discount(zamok))
        self.assertFragmentIn(response, T.ExpectedVelo25.offer_from_beru(zamok))

    def test_discounts_by_cart_selected_flags(self):
        """
        Проверяем работу флагов включения/отключениы для priedrop в специальных местах
        """

        def format_request(exp_on=None, mode_on=None, params=''):
            request_tpl = (
                'place=prime&text=замок+для+велосипеда&rids=213&hid=25728039&cart=BLUEModel1FEED1111QQQQ&'
                'rgb=blue&rearr-factors=market_promo_by_user_cart_hids=1{rearr}'
                '&allow-collapsing=0'
                '{cgi}' + params
            )
            rearr = '' if exp_on is None else ';market_promo_cart_special_places_exp={}'.format(exp_on)
            cgi = '' if mode_on is None else '&promo-by-cart-in-special-places-only={}'.format(mode_on)

            return request_tpl.format(rearr=rearr, cgi=cgi)

        zamok = "Замок для велосипеда"

        # флаг эксперимента выключен, промо работает по-старому
        response = self.report.request_json(format_request(exp_on=None, mode_on=None, params=''))
        self.assertFragmentIn(response, T.ExpectedVelo25.offer_from_beru_with_discount(zamok))
        self.assertFragmentIn(response, T.ExpectedVelo25.offer_from_beru(zamok))

        response = self.report.request_json(format_request(exp_on=0, mode_on=1, params=''))
        self.assertFragmentIn(response, T.ExpectedVelo25.offer_from_beru_with_discount(zamok))
        self.assertFragmentIn(response, T.ExpectedVelo25.offer_from_beru(zamok))

        # mode on выключен, промо работает по-старому
        response = self.report.request_json(format_request(exp_on=1, mode_on=0, params=''))
        self.assertFragmentIn(response, T.ExpectedVelo25.offer_from_beru_with_discount(zamok))
        self.assertFragmentIn(response, T.ExpectedVelo25.offer_from_beru(zamok))

        response = self.report.request_json(
            format_request(exp_on='true', mode_on='false', params='&promo-by-cart-mskus=21')
        )
        self.assertFragmentIn(response, T.ExpectedVelo25.offer_from_beru_with_discount(zamok))
        self.assertFragmentIn(response, T.ExpectedVelo25.offer_from_beru(zamok))

        # флаг эксперимента и mode on включены, скидка показана на спец странице (promo-by-cart-enabled)
        response = self.report.request_json(format_request(exp_on=1, mode_on=1, params='&promo-by-cart-enabled=1'))
        self.assertFragmentIn(response, T.ExpectedVelo25.offer_from_beru_with_discount(zamok))
        self.assertFragmentIn(response, T.ExpectedVelo25.offer_from_beru(zamok))

        # флаг эксперимента и mode on включены, скидка не показана
        response = self.report.request_json(format_request(exp_on=1, mode_on=1, params=''))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru_with_discount(zamok))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru(zamok))

        # флаг promo-by-cart-in-special-places-only=1 по-умолчанию
        response = self.report.request_json(format_request(exp_on=1, mode_on=None, params=''))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru_with_discount(zamok))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru(zamok))

    def test_discounts_by_cart_147(self):
        """Проверяем что оффер со 147 склада дает скидку на оффер с того же склада но наоборот не дает скидок на офферы со 145 склада"""

        # оффер в корзине BLUEModel1FEED2222dddQ - велосипед из категории 16044621 со склада 147
        response = self.report.request_json(
            'place=prime&text=замок+для+велосипеда&rids=213&hid=25728039&cart=BLUEModel1FEED2222dddQ&rgb=blue&allow-collapsing=0'
        )

        title = "Замок для велосипеда"

        # нет дополнительных скидок для офферов с Беру с другого склада (145)
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru_with_discount(title))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru(title))
        # оффер от Беру со склада 147 получает скидку по промоакции
        self.assertFragmentIn(response, T.ExpectedVelo25.offer_from_beru_147(title))
        # оффер от 3p магазина не получает скидки
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_other_blue_shop(title))

    def test_promo_type_filter(self):
        """Проверяем что promo-type=cart-discount оставляет только офферы со скидкой по данной промоакции"""

        # оффер в корзине BLUEModel1FEED2222dddQ - велосипед из категории 16044621 со склада 147
        response = self.report.request_json(
            'place=prime&text=замок+для+велосипеда&rids=213&hid=25728039&cart=BLUEModel1FEED2222dddQ'
            '&rgb=blue&allow-collapsing=0&promo-type=cart-discount'
        )

        # в выдаче останется всего один документ
        # офферы от Беру со 145 склада и оффер от 3p магазина отсеятся так как у них нет данной промоакции

        title = "Замок для велосипеда"

        # оффер от Беру со склада 147 получает скидку по промоакции и только он один есть в выдаче
        self.assertFragmentIn(
            response,
            {"search": {"total": 1, "results": [T.ExpectedVelo25.offer_from_beru_147(title)]}},
            allow_different_len=False,
        )

    def test_promo_on_child_categories(self):
        """Проверяем что промоакция применяетя ко всем дочерним категориям
        На примере скидкок на Моющие средства для кухни при покупке Мелкой техники для кухни
        И мультиварка и электрочайник дают скидку на все моющие средства
        """

        feiry = "Фейри"
        muskul = "Мистер Мускул"
        manok = "Манок на уток"
        zamok = "Замок для велосипеда"

        # BLUEModel3FEED1111gggg - электрочайник из hid=90586 - оффер от Беру со склада 145
        # BLUEModel4FEED3333wwww - мультиварка из hid=4954975 - оффер от p3 магазина со склада 145
        for cart in [
            'BLUEModel3FEED1111gggg',
            'BLUEModel4FEED3333wwww',
            'BLUEModel3FEED1111gggg,BLUEModel4FEED3333wwww',
        ]:
            response = self.report.request_json(
                'place=prime&text=клиент+всегда+у&rids=213&cart={}&rgb=blue&allow-collapsing=0&numdoc=100'.format(cart)
            )

            # промо дает дополнительные скидки для офферов с Беру из категории Моющие средства для кухни
            self.assertFragmentIn(response, T.ExpectedKitchen5.offer_from_beru_with_discount(feiry))
            self.assertFragmentIn(response, T.ExpectedKitchen5.offer_from_beru(feiry))
            self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_other_blue_shop(feiry))

            self.assertFragmentIn(response, T.ExpectedKitchen5.offer_from_beru_with_discount(muskul))
            self.assertFragmentIn(response, T.ExpectedKitchen5.offer_from_beru(muskul))
            self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_other_blue_shop(muskul))

            # при этом скидки на офферы из других категорий не появляются
            self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru_with_discount(manok))
            self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru(manok))

            self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru_with_discount(zamok))
            self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru(zamok))

    def test_non_gl_filters(self):
        """Фикс бага MARKETOUT-25344
        Ограничения на выдачу не должны влиять на запрос офферов из корзины пользователя
        """
        # в корзине 2 оффера за 1300 и за 1500
        query = 'place=prime&text=клиент+всегда+у&rids=213&cart=BLUEModel3FEED1111gggg,BLUEModel4FEED3333wwww&rgb=blue&debug=da'
        queries = [
            query,
            query
            + '&mcpricefrom=99&mcpriceto=100',  # устанавливаем серьезные ограничения по цене - в корзине все равно 2 оффера
            query + '&hid=12345&nid=12346',  # hid и nid не влияют на получение офферов в корзине
        ]

        for q in queries:
            response = self.report.request_json(q)
            self.assertFragmentIn(
                response,
                'Document in user cart: wareId=BLUEModel3FEED1111gggg warehouseId=145 ffWarehouseId=145 promoByCartWarehouseId=145 hid=90586',
            )
            self.assertFragmentIn(
                response,
                'Document in user cart: wareId=BLUEModel4FEED3333wwww warehouseId=145 ffWarehouseId=145 promoByCartWarehouseId=145 hid=4954975',
            )

    def test_offer_info(self):
        """Проверяем что скидки и промоакции работают в place=offerinfo (используется чекаутером)
        с различным вариантом задания корзины (через cart, cart-fo, cart-sku)
        """

        zamok = "Замок для велосипеда"

        # оффер в корзине feed-offerid =  wareMd5 = BLUEModel1FEED1111QQQQ - велосипед из категории 16044621 - склад 145
        feed_offer_id = '1-1111.beru_model1_Q'
        ware_md5 = 'BLUEModel1FEED1111QQQQ'
        msku = 11
        # корзина может задаваться как &cart=ware_md5  &cart-fo=feed_offer_id &cart-sku=msku

        for cart_format in ['cart={ware_md5}', 'cart-fo={feed_offer_id}', 'cart-sku={msku}']:

            cart = cart_format.format(feed_offer_id=feed_offer_id, msku=msku, ware_md5=ware_md5)
            query = 'place=offerinfo&offerid={offerid}&rids=213&' + cart + '&rgb=blue&show-urls=external&regset=2'

            # оффер Замок для велосипеда от беру со скидкой - получает oldDiscount
            response = self.report.request_json(query.format(offerid='BLUEModel2FEED1111QQQQ'))
            self.assertFragmentIn(response, T.ExpectedVelo25.offer_from_beru_with_discount(zamok))

            # оффер Замок для велосипеда от беру без скидки - получает просто discount
            response = self.report.request_json(query.format(offerid='BLUEModel2FEED1111gggg'))
            self.assertFragmentIn(response, T.ExpectedVelo25.offer_from_beru(zamok))

            # оффер Замок для велосипеда от беру со 147 склада не получает скидки (т.к. оффер в корзине со 145 склада)
            response = self.report.request_json(query.format(offerid='BLUEModel2FEED2222dddQ'))
            self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru_147(zamok))

            # оффер Замок для велосипеда от магазина размещающегося на беру без скидки - ничего не получает
            response = self.report.request_json(query.format(offerid='BLUEModel2FEED3333wwww'))
            self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_other_blue_shop(zamok))

    def test_undefined_ware_md5(self):
        '''Передан несуществующий wareMd5 в cart= параметре. Выдача не должна сломаться, так как оффер мог протухнуть'''

        zamok = "Замок для велосипеда"

        response = self.report.request_json(
            'place=offerinfo&offerid=BLUEModel2FEED2222dddQ&rids=213&cart={}&rgb=blue&show-urls=external&regset=2'.format(
                UNDEFINED_WARE_MD5
            )
        )
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru_147(zamok))

        response = self.report.request_json(
            'place=sku_offers&market-sku=21&cart={}&rids=213&rgb=blue&show-urls=external'.format(UNDEFINED_WARE_MD5)
        )
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru_with_discount(zamok))

    def test_invalid_ware_md5(self):
        '''Передана вообще левая строка в cart= параметре. Выдача не должна сломаться, пишем ошибку в лог'''
        zamok = "Замок для велосипеда"

        response = self.report.request_json(
            'place=offerinfo&offerid=BLUEModel2FEED2222dddQ&rids=213&cart={}&rgb=blue&show-urls=external&regset=2'.format(
                INVALID_WARE_MD5
            )
        )
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru_147(zamok))
        self.error_log.expect(message=Contains('Place has thrown an exception', 'Request is empty')).once()

    def test_sku_offers(self):
        """Проверяем что скидки и промоакции работают в place=sku_offers"""

        # оффер в корзине feed-offerid =  wareMd5 = BLUEModel1FEED1111QQQQ - велосипед из категории 16044621 - склад 145
        # дает скидки на офферы Беру с того же склада
        zamok = "Замок для велосипеда"
        query = (
            'place=sku_offers&market-sku={market_sku}&cart=BLUEModel1FEED1111QQQQ&rids=213&rgb=blue&show-urls=external'
        )

        response = self.report.request_json(query.format(market_sku=21))
        self.assertFragmentIn(response, T.ExpectedVelo25.offer_from_beru_with_discount(zamok))

        response = self.report.request_json(query.format(market_sku=22))
        self.assertFragmentIn(response, T.ExpectedVelo25.offer_from_beru(zamok))

        response = self.report.request_json(query.format(market_sku=23))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru_147(zamok))

        response = self.report.request_json(query.format(market_sku=24))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_other_blue_shop(zamok))

    def test_promo_cart_discount(self):
        """Проверяем работу place=promo_cart_discount
        При передаче параметра cart возвращает предложения из разных категорий
        упорядочивая категории: сначала сопутка потом частотка, и далее по уменьшению скидки
        """

        # BLUEModel1FEED3333wwww - велосипед из hid=16044621 - оффер от p3 магазина со склада 145
        # BLUEModel4FEED2222dddQ - мультиварка из hid=4954975 - оффер от Беру со склада 147
        # Велосипед дает скидку 25% на замки для велосипедов со склада 145 (2 оффера от Беру)
        # Мультиварка дает скидку 5% на Моющие средства для кухни со склада 147 (т.е. мускул и фейри от Беру!ЗаМКАДом)
        # В результате категория Замки для велосипедов перемежается с категорией Моющие средства для кухни и Туалетная бумага
        # Моющие средства для кухни - не листовая категория, но встречается только по одному офферу в каждом цикле
        # Туалетная бумага - частотка (скидка дается по любому товару в корзине) - идет ниже чем товары из сопутствующих категорий
        # Присутствуют только те офферы которые имеют скидку типа cart-discount

        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&rgb=blue&allow-collapsing=0'
            '&cart=BLUEModel1FEED3333wwww,BLUEModel4FEED2222dddQ'
        )

        zamok = "Замок для велосипеда"
        feiry = "Фейри"
        muskul = "Мистер Мускул"
        tualetka = "Туалетка"

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 7,
                    "results": [
                        T.ExpectedVelo25.offer_from_beru(zamok),
                        T.ExpectedKitchen5.offer_from_beru_147(muskul),
                        T.ExpectedToilet5.offer_from_beru_147(tualetka),
                        T.ExpectedVelo25.offer_from_beru_with_discount(zamok),
                        T.ExpectedKitchen5.offer_from_beru_147(feiry),
                        T.ExpectedToilet5.offer_from_beru(tualetka),
                        T.ExpectedToilet5.offer_from_beru_with_discount(tualetka),
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )
        self.error_log.ignore(code=EXTREQUEST_DJ_EMPTY_RECOM)

    def test_promo_cart_discount_thumbs(self):
        """Проверяем работу place=promo_cart_discount с флагом new-picture-format=1"""

        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&'
            '&rgb=blue&cart=BLUEModel4FEED2222dddQ'
            '&yandexuid=12311322&allow-collapsing=0'
            '&rearr-factors=market_dj_exp_for_promo_cart_force=1'
            '&new-picture-format=1'
        )

        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "pictures": [
                    {
                        "entity": "picture",
                        "original": {
                            "width": 100,
                            "height": 100,
                            "namespace": "marketpic",
                            "groupId": 1,
                            "key": "market_iyC3nHslqLtqZJLygVAHeA",
                        },
                        "thumbnails": Absent(),
                    }
                ],
            },
        )
        self.assertFragmentIn(response, {"knownThumbnails": []})
        self.error_log.ignore(code=EXTREQUEST_DJ_EMPTY_RECOM)

    def test_promo_cart_discount_pricedrop(self):
        """Проверяем что в place=promo_cart_discount c promo-by-user-cart-hids=0 pricedrop выключен"""

        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&rgb=blue&allow-collapsing=0'
            '&cart=BLUEModel1FEED3333wwww,BLUEModel4FEED2222dddQ'
            '&promo-by-user-cart-hids=0'
        )

        self.assertFragmentIn(
            response, {"search": {"total": 0, "results": []}}, allow_different_len=False, preserve_order=True
        )

        # Проверяем, что и флаг market_promo_by_user_cart_hids отключает pricedrop в place=promo_cart_discount
        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&rgb=blue&allow-collapsing=0'
            '&cart=BLUEModel1FEED3333wwww,BLUEModel4FEED2222dddQ'
            '&promo-by-user-cart-hids=1&rearr-factors=market_promo_by_user_cart_hids=0'
        )

        self.assertFragmentIn(
            response, {"search": {"total": 0, "results": []}}, allow_different_len=False, preserve_order=True
        )

        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&rgb=blue&allow-collapsing=0'
            '&cart=BLUEModel1FEED3333wwww,BLUEModel4FEED2222dddQ'
            '&promo-by-user-cart-hids=0&rearr-factors=market_promo_by_user_cart_hids=1'
        )

        self.assertFragmentIn(
            response, {"search": {"total": 0, "results": []}}, allow_different_len=False, preserve_order=True
        )

    def test_promo_cart_discount_randomize(self):
        """Категории в запросе ранжируются не совсем по размеру скидки
        а по размер_скидки*случайное число - в этом случае категории с низкой скидкой все равно
        будут иногда попадать в выдачу, но реже чем категории с высокой скидкой
        При этом сопутка все равно идет выше частотки
        Случайное число выбирается в зависимости от содержимого корзины и от идентификатора пользователя (puid/uuid/yandexuid)
        """

        # uuid подобран таким образом чтобы категория Замки для велосипедов оказалась ниже категории Средства для кухни
        # если у вас упал этот тест - почините остальные а потом подберите подходящий uuid чтобы фейри шел первым
        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&rgb=blue&allow-collapsing=0&uuid=da65f28c666d4dc3b7a8f47b62d3cfa'
            '&cart=BLUEModel1FEED3333wwww,BLUEModel4FEED2222dddQ'
        )

        zamok = "Замок для велосипеда"
        feiry = "Фейри"
        muskul = "Мистер Мускул"
        tualetka = "Туалетка"

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 7,
                    "results": [
                        T.ExpectedKitchen5.offer_from_beru_147(muskul),
                        T.ExpectedVelo25.offer_from_beru(zamok),
                        T.ExpectedToilet5.offer_from_beru_147(tualetka),
                        T.ExpectedKitchen5.offer_from_beru_147(feiry),
                        T.ExpectedVelo25.offer_from_beru_with_discount(zamok),
                        T.ExpectedToilet5.offer_from_beru(tualetka),
                        T.ExpectedToilet5.offer_from_beru_with_discount(tualetka),
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )
        self.error_log.ignore(code=EXTREQUEST_DJ_EMPTY_RECOM)

    def test_market_promo_by_user_cart_split(self):
        """При передаче флага market_promo_by_user_cart_split=s1 применяются только правила помеченные сплитом s1"""

        zamok = "Замок для велосипеда"
        feiry = "Фейри"
        muskul = "Мистер Мускул"
        tualetka = "Туалетка"

        # BLUEModel1FEED3333wwww - велосипед из hid=16044621 - оффер от p3 магазина со склада 145
        # BLUEModel4FEED2222dddQ - мультиварка из hid=4954975 - оффер от Беру со склада 147
        # Велосипед дает скидку 5% на замки для велосипедов со склада 145 (2 оффера от Беру)
        # Мультиварка ни на что скидки в этом сплите не дает
        # Скидок на частотку в данном сплите нет
        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&rgb=blue&allow-collapsing=0'
            '&cart=BLUEModel1FEED3333wwww,BLUEModel4FEED2222dddQ'
            '&rearr-factors=market_promo_by_user_cart_split=s1'
        )

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "results": [
                        T.ExpectedVelo5s1.offer_from_beru(zamok),
                        T.ExpectedVelo5s1.offer_from_beru_with_discount(zamok),
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )
        self.error_log.ignore(code=EXTREQUEST_DJ_EMPTY_RECOM)

        response = self.report.request_json(
            'place=prime&text=клиент+всегда+у&rids=213&rgb=blue&allow-collapsing=0&numdoc=100'
            '&cart=BLUEModel1FEED3333wwww,BLUEModel4FEED2222dddQ'
            '&rearr-factors=market_promo_by_user_cart_split=s1'
        )

        self.assertFragmentIn(response, T.ExpectedVelo5s1.offer_from_beru_with_discount(zamok))
        self.assertFragmentIn(response, T.ExpectedVelo5s1.offer_from_beru(zamok))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru_147(zamok))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_other_blue_shop(zamok))

        # В сплите s1 нет скидок на другие категории товаров
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru_with_discount(feiry))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru(feiry))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_other_blue_shop(feiry))

        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru_with_discount(muskul))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru_with_discount(tualetka))
        self.error_log.ignore(code=EXTREQUEST_DJ_EMPTY_RECOM)

    def test_market_promo_by_user_cart_split_ignore_cat_ties_and_warehouses_dept_logic(self):
        # При выставлении флага market_promo_cart_ignore_filters
        # значение правой категории - это на самом деле "вертикаль" или "департамент"(некая группа категорий, заданная на dj)
        # По новым продуктовым требованиям если в корзине меньше 2 департаментов, скидка не дается.
        # в сплите s3 BLUEModel1FEED3333wwww и BLUEModel4FEED2222dddQ из корзины в одном департаменте, потому скидка не выдается
        # в s4 оффера в разных депт, скидка дается на все, что возвращает плейс

        # сплит с двумя департаментами в корзине, должен возвращать все мскю, лежащие в promo-by-cart-mskus которые подразумеваются в ответе
        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&rgb=blue&allow-collapsing=0'
            '&cart=BLUEModel1FEED3333wwww,BLUEModel4FEED2222dddQ&promo-by-cart-enabled=0&promo-by-cart-in-special-places-only=1'
            '&promo-by-cart-mskus=81'
            '&rearr-factors=market_promo_by_user_cart_split=s4;market_promo_cart_ignore_filters=1;market_promo_cart_special_places_exp=1'
        )

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )
        self.error_log.ignore(code=EXTREQUEST_DJ_EMPTY_RECOM)

        # сплит с одним департаментом в корзине, должен возвращать пустоту
        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&rgb=blue&allow-collapsing=0'
            '&cart=BLUEModel1FEED3333wwww,BLUEModel4FEED2222dddQ&promo-by-cart-enabled=0&promo-by-cart-in-special-places-only=1'
            '&promo-by-cart-mskus=81'
            '&rearr-factors=market_promo_by_user_cart_split=s3;market_promo_cart_ignore_filters=1;market_promo_cart_special_places_exp=1'
        )

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 0,
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )
        self.error_log.ignore(code=EXTREQUEST_DJ_EMPTY_RECOM)

        # сплит с одним департаментом в корзине, но market_promo_cart_multi_dept_in_cart_logic=0, что отменяет логику мультидепартаментности
        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&rgb=blue&allow-collapsing=0'
            '&cart=BLUEModel1FEED3333wwww,BLUEModel4FEED2222dddQ&promo-by-cart-enabled=0&promo-by-cart-in-special-places-only=1'
            '&promo-by-cart-mskus=81'
            '&rearr-factors=market_promo_by_user_cart_split=s3;market_promo_cart_ignore_filters=1;market_promo_cart_special_places_exp=1;market_promo_cart_multi_dept_in_cart_logic=0'
        )

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )
        self.error_log.ignore(code=EXTREQUEST_DJ_EMPTY_RECOM)

        # promo-by-cart-enabled=1, возвращаем все что есть
        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&rgb=blue&allow-collapsing=0'
            '&cart=BLUEModel1FEED3333wwww,BLUEModel4FEED2222dddQ&promo-by-cart-enabled=1&promo-by-cart-in-special-places-only=1'
            '&rearr-factors=market_promo_by_user_cart_split=s3;market_promo_cart_ignore_filters=1;market_promo_cart_special_places_exp=1'
        )

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 8,
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )
        self.error_log.ignore(code=EXTREQUEST_DJ_EMPTY_RECOM)

        # promo-by-cart-enabled=1, возвращаем все что есть
        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&rgb=blue&allow-collapsing=0'
            '&cart=BLUEModel1FEED3333wwww,BLUEModel4FEED2222dddQ&promo-by-cart-enabled=1&promo-by-cart-in-special-places-only=1'
            '&rearr-factors=market_promo_by_user_cart_split=s4;market_promo_cart_ignore_filters=1;market_promo_cart_special_places_exp=1'
        )

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 15,
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )
        self.error_log.ignore(code=EXTREQUEST_DJ_EMPTY_RECOM)

    def test_promo_for_1p_3p_suppliers(self):
        """Проверяем что можно задать разную скидку для 1P (Беру) и 3P (другие магазины) поставщиков"""

        # BLUEModel7FEED3333wwww - манок из hid=12345 - оффер от p3 магазина со склада 145
        # дает скидку в категории Туалетная бумага на офферы Беру в 5% а на офферы другого магазина размещающегося на беру в 25%

        tualetka = "Туалетка"
        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&rgb=blue&allow-collapsing=0'
            '&cart=BLUEModel7FEED3333wwww'
            '&rearr-factors=market_promo_by_user_cart_split=s2'
        )

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 3,
                    "results": [
                        T.ExpectedToilets5for1p.offer_from_beru_with_discount(tualetka),
                        T.ExpectedToilets5for1p.offer_from_beru(tualetka),
                        T.ExpectedToilets25for3p.offer_from_other_blue_shop(tualetka),
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=False,
        )
        self.error_log.ignore(code=EXTREQUEST_DJ_EMPTY_RECOM)

    def test_promo_by_cart_crossdoc(self):
        """
        Кроссдок - товар находится на складе поставщика, но следует через склад Беру
        Скидки на кроссдок товары выдаются как скидки на 3P товары
        По-умолчанию warehouseId для таких товаров учитывается как склад Беру, через который следует товар
        """
        self.error_log.ignore(code=EXTREQUEST_DJ_EMPTY_RECOM)

        tualetka = "Туалетка"

        # BLUEModel1FEED4444xxxw - Велосипед, продаваемый кросс-док магазином со склада 999,
        # товар по модели кросс-док следует со склада поставщика 999 через склад Беру 147, и в корзине учитывается склад Беру
        # скидки выдаются на все товары, которые продаются со склада 147 или следуют через склад 147
        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&rgb=blue&allow-collapsing=0&debug=da'
            '&cart=BLUEModel1FEED4444xxxw'
            '&rearr-factors=market_promo_by_user_cart_use_ff_warehouse=1;market_promo_by_user_cart_split=s2'
        )
        self.assertFragmentIn(
            response,
            'Document in user cart: wareId=BLUEModel1FEED4444xxxw warehouseId=999 ffWarehouseId=147 promoByCartWarehouseId=147 hid=16044621',
        )
        self.assertFragmentIn(
            response,
            {
                'report': {
                    'how': [
                        {
                            'args': Contains(
                                '\nuser_cart {\n  orders {\n    warehouse_id: 147\n    hids: 16044621\n  }\n'
                            )
                        }
                    ]
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        T.ExpectedToilets25for3p.offer_from_crossdoc(tualetka),
                        T.ExpectedToilets5for1p.offer_from_beru_147(tualetka),
                    ]
                }
            },
            allow_different_len=False,
        )

    def test_promo_by_cart_dropship(self):
        """Дропшип - товар находится на складе поставщика и доставляется покупателю непосредственно со склада поставщика
        Скидки на дропшип товары выдаются как скидки на 3P товары
        warehouseId для таких товаров учитывается как id склада поставщика
        """

        self.error_log.ignore(code=EXTREQUEST_DJ_EMPTY_RECOM)

        tualetka = "Туалетка"

        # BLUEModel1FEED5555dddQ - Велосипед, продаваемый дропшип магазином со склада 7,
        # товар по модели дропшип следует напрямую со склада поставщика, и в корзине учитывается склад поставщика
        # скидка cart-discount выдается только на Туалетку (потому что 3P-поставщик) продаваемую с того же склада
        # флаг market_promo_by_user_cart_use_ff_warehouse=1 не влияет на warehouseId
        for add_rearr in ['', 'market_promo_by_user_cart_use_ff_warehouse=1']:
            response = self.report.request_json(
                'place=promo_cart_discount&rids=213&rgb=blue&allow-collapsing=0&debug=da'
                '&cart=BLUEModel1FEED5555dddQ'
                '&rearr-factors=market_promo_by_user_cart_split=s2;' + add_rearr + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
            )
            self.assertFragmentIn(
                response,
                'Document in user cart: wareId=BLUEModel1FEED5555dddQ warehouseId=7 ffWarehouseId=7 promoByCartWarehouseId=7 hid=16044621',
            )
            self.assertFragmentIn(
                response,
                {
                    'report': {
                        'how': [
                            {
                                'args': Contains(
                                    '\nuser_cart {\n  orders {\n    warehouse_id: 7\n    hids: 16044621\n  }\n'
                                )
                            }
                        ]
                    }
                },
            )
            self.assertFragmentIn(
                response,
                {'search': {'results': [T.ExpectedToilets25for3p.offer_from_dropship(tualetka)]}},
                allow_different_len=False,
            )

    def test_promo_by_cart_dropship_sc(self):
        """
        Для Дропшип через СЦ используется склад хранения
        """
        self.error_log.ignore(code=EXTREQUEST_DJ_EMPTY_RECOM)

        tualetka = "Туалетка"

        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&rgb=blue&allow-collapsing=0&debug=da'
            '&cart=BLUEModel1FEED7777eeeQ'
            '&rearr-factors=market_promo_by_user_cart_use_ff_warehouse=1;market_promo_by_user_cart_split=s2'
            '&debug=1'
        )
        self.assertFragmentIn(
            response,
            'Document in user cart: wareId=BLUEModel1FEED7777eeeQ warehouseId=777 ffWarehouseId=147 promoByCartWarehouseId=777 hid=16044621',
        )
        self.assertFragmentIn(
            response,
            {
                'report': {
                    'how': [
                        {
                            'args': Contains(
                                '\nuser_cart {\n  orders {\n    warehouse_id: 777\n    hids: 16044621\n  }\n'
                            )
                        }
                    ]
                }
            },
        )

        self.assertFragmentIn(response, {'logicTrace': [T.RegexUnicodeFriendly(r'Dj search in warehouses 777.')]})

        self.assertFragmentIn(
            response,
            {'search': {'results': [T.ExpectedToilets25for3p.offer_from_dropship_sc(tualetka)]}},
            allow_different_len=False,
        )

    def test_promo_by_cart_click_and_collect(self):
        """Исключаем click&collect из price drop"""

        self.error_log.ignore(code=EXTREQUEST_DJ_EMPTY_RECOM)
        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&rgb=blue&allow-collapsing=0&debug=da'
            '&cart=BLUEModel1FEED6666cccQ'
            '&rearr-factors=market_promo_by_user_cart_split=s2'
        )
        self.assertFragmentIn(
            response,
            'Document in user cart: wareId=BLUEModel1FEED6666cccQ warehouseId=103 ffWarehouseId=103 promoByCartWarehouseId=103 hid=16044621',
        )
        self.assertFragmentIn(
            response,
            {
                'report': {
                    'how': [
                        {
                            'args': Contains(
                                '\nuser_cart {\n  orders {\n    warehouse_id: 103\n    hids: 16044621\n  }\n'
                            )
                        }
                    ]
                }
            },
        )
        self.assertFragmentIn(response, {"search": {"results": []}}, allow_different_len=False)

    def test_promo_by_cart_white_offer_ignore_filters(self):
        """Тест, который проверяет что работает посчет департаментов по белым офферам для тикета MARKETRECOM-4696
        Если бы оно не работало, количество возвращенных результатов - 0, т.к. в корзине было бы меньше 2 департаментов"""

        self.error_log.ignore(code=EXTREQUEST_DJ_EMPTY_RECOM)
        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&rgb=blue&allow-collapsing=0&debug=1'
            '&cart=BLUEModel1FEED3333wwww,WHITE-Model2-FESH-2---&promo-by-cart-enabled=0&promo-by-cart-in-special-places-only=1'
            '&promo-by-cart-mskus=81'
            '&rearr-factors=market_promo_by_user_cart_split=s4;market_promo_cart_ignore_filters=1;market_promo_cart_special_places_exp=1'
        )

        self.assertFragmentIn(
            response,
            'Document in user cart: wareId=WHITE-Model2-FESH-2--w warehouseId=4294967295 No ffwarehouse promoByCartWarehouseId=1343184 hid=25728039',
        )

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_no_discount_on_the_same_category_in_case_90401(self):
        """Частотка (категории с частотными товарами, на которые дается скидка при наличии какого-либо товара в корзине)
        не дает скидку сама на себя, т.е. если в корзине только товары из одной этой категории - то на них скидка не дается"""

        tualetka = 'Туалетка'
        query = 'place=prime&hid=15720388&rids=213&cart={cart}&rgb=blue&allow-collapsing=0'

        # туалетная бумага не дает скидку на категорию Туалетная бумага
        one_tualetka = 'BLUEModel8FEED1111QQQQ'
        response = self.report.request_json(query.format(cart=one_tualetka))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru(tualetka))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru_with_discount(tualetka))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru_147(tualetka))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_other_blue_shop(tualetka))

        # туалетная бумага не дает скидку на категорию Туалетная бумага
        many_tualetok = 'BLUEModel8FEED1111QQQQ,BLUEModel8FEED1111gggg'
        response = self.report.request_json(query.format(cart=many_tualetok))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru(tualetka))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru_with_discount(tualetka))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru_147(tualetka))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_other_blue_shop(tualetka))

        # любой товар из другой категории дает скидку на частотную категорию Туалетная бумага (с учетом склада)
        other_tovar = 'BLUEModel7FEED1111QQQQ'
        response = self.report.request_json(query.format(cart=other_tovar))
        self.assertFragmentIn(response, T.ExpectedToilet5.offer_from_beru(tualetka))
        self.assertFragmentIn(response, T.ExpectedToilet5.offer_from_beru_with_discount(tualetka))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru_147(tualetka))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_other_blue_shop(tualetka))

        # если в корзине есть туалетная бумага и любой другой товар то другой товар дает скидку на Туалетную бумагу
        response = self.report.request_json(query.format(cart=one_tualetka + ',' + other_tovar))
        self.assertFragmentIn(response, T.ExpectedToilet5.offer_from_beru(tualetka))
        self.assertFragmentIn(response, T.ExpectedToilet5.offer_from_beru_with_discount(tualetka))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru_147(tualetka))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_other_blue_shop(tualetka))

    def test_no_discount_on_the_same_category(self):
        """Категория не дает скидку сама на себя, например если родительская категория "Товары для строительства"
        дает ссылку на дочернюю категорию "Доски" то товар из этой категории не дает скидку сам на себя
        или на другие товары из этой же категории"""

        doski = 'Доска половая'
        query = 'place=prime&hid=934581&rids=213&cart={cart}&rgb=blue&allow-collapsing=0'

        # Доски из категории Товары для строительства и ремонта не дает скидку на доски
        doska = 'BLUEModel9FEED1111QQQQ'
        response = self.report.request_json(query.format(cart=doska))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru(doski))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru_with_discount(doski))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru_147(doski))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_other_blue_shop(doski))

        # Но гвозди из категории Товары для строительства и ремонта дают скидку на доски
        gvozdi = 'BLUEModel10FEED1111QQQ'
        response = self.report.request_json(query.format(cart=gvozdi))
        self.assertFragmentIn(response, T.ExpectedDoski5.offer_from_beru(doski))
        self.assertFragmentIn(response, T.ExpectedDoski5.offer_from_beru_with_discount(doski))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_beru_147(doski))
        self.assertFragmentIn(response, T.ExpectedNoPromo.offer_from_other_blue_shop(doski))

    @classmethod
    def prepare_promo_collision(cls):
        cls.index.hypertree += [
            HyperCategory(hid=91529, name="Чай", output_type=HyperCategoryType.GURU),
            HyperCategory(hid=91519, name="Варенье", output_type=HyperCategoryType.GURU),
            HyperCategory(hid=91509, name="Нечто", output_type=HyperCategoryType.GURU),
        ]

        cls.index.promos_by_cart += [
            PromoByCart(cart_hid=91529, promo_hid=91519, percent=25),
            PromoByCart(cart_hid=91529, promo_hid=91509, percent=20),
        ]

        cls.index.models += [
            Model(hid=91529, hyperid=9152901, title='AzerCay'),
            Model(hid=91519, hyperid=9151901, title='Apple Jam'),
            Model(hid=91509, hyperid=9150901, title='Something'),
        ]

        cls.index.mskus += [
            MarketSku(
                title='AzerCay',
                hyperid=9152901,
                sku=91529001,
                hid=91529,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=100,
                        feedid=1111,
                        offerid="azercay-1",
                        waremd5=b64url_md5("azercay-1"),
                        weight=1,
                        dimensions=OfferDimensions(length=10, width=10, height=10),
                    )
                ],
            ),
            MarketSku(
                title='Apple Jam',
                hyperid=9151901,
                sku=91519001,
                hid=91519,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=100,
                        feedid=1111,
                        offerid="apple-jam-1",
                        waremd5=b64url_md5("apple-jam-1"),
                        weight=1,
                        dimensions=OfferDimensions(length=10, width=10, height=10),
                        promo=blue_cashback_1,
                    )
                ],
            ),
            # у оффера есть OldPrice от магазина, но для него есть активная акция, не проходит по ее условиям
            MarketSku(
                title='OfferWithOldPrice',
                hyperid=9150901,
                sku=91519333,
                hid=91509,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(price=100, price_old=110, feedid=1111, offerid="owop", waremd5=b64url_md5("owop"))
                ],
            ),
        ]
        cls.index.promos += [
            Promo(
                promo_type=PromoType.BLUE_3P_FLASH_DISCOUNT,
                key='JVvklxUgdnawSJPG4UhZGA',
                mskus=[
                    PromoMSKU(msku='91519333', market_promo_price=90, market_old_price=105),
                ],
            ),
        ]

        cls.delivery_calc.on_request_offer_buckets(weight=1, height=10, length=10, width=10, warehouse_id=145).respond(
            [1234], [], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=2, height=22, length=11, width=11, warehouse_id=145).respond(
            [1234], [], []
        )

    def test_promo_collision(self):
        """
        Проверям что при наличии другой акции (blue-cashback), pricedrop работает
        """
        query = 'place=prime&hyperid=9151901&allow-collapsing=0&rids=213&rgb=blue&perks=yandex_cashback&rearr-factors=enable_cart_split_on_combinator=0'

        # если нет cart-discount, то работает только кэшбек
        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "Apple Jam"},
                "promos": [
                    {
                        "type": blue_cashback_1.type_name,
                        "key": blue_cashback_1.key,
                    }
                ],
                "prices": {
                    "value": "100",
                    "discount": Absent(),
                },
            },
        )

        # если есть - то впереди cart-discount, кэшбек тоже присутствует в promos
        response = self.report.request_json(query + '&cart=' + b64url_md5("azercay-1"))
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "Apple Jam"},
                "promos": [
                    {
                        "type": PromoType.CART_DISCOUNT,
                        'itemsInfo': {
                            'promoPrice': {
                                'value': str(75),
                            },
                            'discount': {
                                'oldMin': str(100),
                                'absolute': str(25),
                            },
                        },
                    },
                    {
                        "type": blue_cashback_1.type_name,
                        "key": blue_cashback_1.key,
                    },
                ],
                "prices": {
                    "value": "75",
                    "discount": {
                        "oldMin": "100",
                        "percent": 25,
                    },
                },
            },
        )

        # если нет cart-discount, то работает только кэшбек
        response = self.report.request_json(
            'place=combine&rids=213&rgb=blue&perks=yandex_cashback'
            + '&cart='
            + b64url_md5("apple-jam-1")
            + '&offers-list='
            + ','.join(['{}:1;msku:{}'.format(b64url_md5("apple-jam-1"), 91519001)])
            + '&rearr-factors=enable_cart_split_on_combinator=0'
        )
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "Apple Jam"},
                "promos": [
                    {
                        "type": blue_cashback_1.type_name,
                        "key": blue_cashback_1.key,
                    }
                ],
                "prices": {
                    "value": "100",
                    "discount": Absent(),
                },
            },
        )

        # если есть - то впереди cart-discount, кэшбек тоже присутствует в promos
        response = self.report.request_json(
            'place=combine&rids=213&rgb=blue&perks=yandex_cashback'
            + '&cart='
            + b64url_md5("apple-jam-1")
            + ','
            + b64url_md5("azercay-1")
            + '&offers-list='
            + ','.join(
                [
                    '{}:1;msku:{}'.format(b64url_md5("apple-jam-1"), 91519001),
                    '{}:1;msku:{}'.format(b64url_md5("azercay-1"), 91529001),
                ]
            )
            + '&rearr-factors=enable_cart_split_on_combinator=0'
        )
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "Apple Jam"},
                "promos": [
                    {"type": "cart-discount"},
                    {
                        "type": blue_cashback_1.type_name,
                        "key": blue_cashback_1.key,
                    },
                ],
                "prices": {
                    "value": "75",
                    "discount": {
                        "oldMin": "100",
                        "percent": 25,
                    },
                },
            },
        )

        # здесь есть акция типа BLUE_3P_FLASH_DISCOUNT, но оффер не подходит под её условия
        # отбрасывания OldPrice происходить не должно, т.к. есть активная акция типа cart-discount
        params = 'place=prime&rids=213&show-urls=&regset=1&pp=42&offerid={ware}&rgb=blue&cart={cart}&debug=1&rearr-factors=market_documents_search_trace={ware};enable_cart_split_on_combinator=0'
        response = self.report.request_json(params.format(ware=b64url_md5("owop"), cart=b64url_md5("azercay-1")))

        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': b64url_md5("owop"),
                    'promos': [{"type": PromoType.CART_DISCOUNT}],
                    'prices': {
                        'value': '80',
                        'discount': {
                            "percent": 27,
                            "oldMin": "110",
                        },
                    },
                }
            ],
        )

        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'docs_search_trace': {
                        'traces': [
                            {
                                'promos': [
                                    {
                                        'promoType': PromoType.CART_DISCOUNT,
                                        'promoState': 'Active',
                                    },
                                    {
                                        'promoType': PromoType.BLUE_3P_FLASH_DISCOUNT,
                                        'promoState': 'DeclinedByInvalidBlue3PFlashDiscount',
                                    },
                                ],
                            }
                        ],
                    },
                },
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_promo_collision_promocode(cls):
        cls.index.hypertree += [
            HyperCategory(hid=91517, name="Удилища", output_type=HyperCategoryType.GURU),
            HyperCategory(hid=469554, name="Катушки", output_type=HyperCategoryType.GURU),
        ]

        cls.index.promos_by_cart += [
            PromoByCart(cart_hid=91517, promo_hid=469554, percent=25),
        ]

        cls.index.models += [
            Model(hid=91517, hyperid=9151701, title='Удочка'),
            Model(hid=469554, hyperid=46955401, title='Катушка'),
            Model(hid=469554, hyperid=46955402, title='Улучшенная катушка'),
            Model(hid=469554, hyperid=46955403, title='Катушка со скидкой'),
            Model(hid=469554, hyperid=46955404, title='Другая катушка со скидкой'),
            Model(hid=469554, hyperid=46955405, title='Катушка по флеш-акции'),
        ]

        cls.index.mskus += [
            MarketSku(
                title='Удочка',
                hyperid=9151701,
                sku=91517001,
                hid=91517,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=200,
                        feedid=1111,
                        offerid="fishing-rod-1",
                        waremd5=b64url_md5("fishing-rod-1"),
                        weight=3,
                        dimensions=OfferDimensions(length=10, width=10, height=10),
                    )
                ],
            ),
            MarketSku(
                title='Катушка',
                hyperid=46955401,
                sku=469554001,
                hid=469554,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=200,
                        feedid=1111,
                        offerid="coil-1",
                        waremd5=b64url_md5("coil-1"),
                        weight=4,
                        dimensions=OfferDimensions(length=10, width=10, height=10),
                        promo=blue_promocode,
                    )
                ],
            ),
            MarketSku(
                title='Улучшенная катушка',
                hyperid=46955402,
                sku=469554002,
                hid=469554,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=200,
                        feedid=1111,
                        offerid="improved-coil-1",
                        waremd5=b64url_md5("improved-coil-1"),
                        weight=5,
                        dimensions=OfferDimensions(length=10, width=10, height=10),
                        promo=[blue_promocode, blue_cashback_1],
                    )
                ],
            ),
            MarketSku(
                title='Катушка со скидкой',
                hyperid=46955403,
                sku=469554003,
                hid=469554,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=200,
                        feedid=1111,
                        offerid="coil-with-discount-1",
                        waremd5=b64url_md5("coil-with-discount-1"),
                        weight=6,
                        dimensions=OfferDimensions(length=10, width=10, height=10),
                        promo=[direct_discount_1, blue_cashback_1],
                    )
                ],
            ),
            MarketSku(
                title='Другая катушка со скидкой',
                hyperid=46955404,
                sku=469554004,
                hid=469554,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=200,
                        feedid=1111,
                        offerid="coil-with-discount-2",
                        waremd5=b64url_md5("coil-with-discount-2"),
                        weight=7,
                        dimensions=OfferDimensions(length=10, width=10, height=10),
                        promo=[blue_promocode, direct_discount_2, blue_cashback_1],
                    )
                ],
            ),
            MarketSku(
                title='Катушка по флеш-акции',
                hyperid=46955405,
                sku=469554005,
                hid=469554,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=200,
                        feedid=1111,
                        offerid="coil-blue-flash",
                        waremd5=b64url_md5("coil-blue-flash"),
                        weight=8,
                        dimensions=OfferDimensions(length=10, width=10, height=10),
                        promo=[blue_flash],
                    )
                ],
            ),
        ]

        cls.delivery_calc.on_request_offer_buckets(weight=3, height=10, length=10, width=10, warehouse_id=145).respond(
            [1234], [], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=4, height=10, length=10, width=10, warehouse_id=145).respond(
            [1234], [], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=5, height=10, length=10, width=10, warehouse_id=145).respond(
            [1234], [], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=6, height=10, length=10, width=10, warehouse_id=145).respond(
            [1234], [], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=7, height=10, length=10, width=10, warehouse_id=145).respond(
            [1234], [], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=8, height=10, length=10, width=10, warehouse_id=145).respond(
            [1234], [], []
        )

    def _test_promo_collision(
        self,
        query,
        expected_title,
        expected_promos,
        promoPrice,
        prePromoPrice,
        specialDiscount=None,
        baseOfferPrice=None,
    ):
        response = self.report.request_json(query + '&rearr-factors=market_metadoc_search=no')
        if baseOfferPrice is None:
            baseOfferPrice = prePromoPrice
        promos = []

        discount = Absent()
        if prePromoPrice:
            discount = int((baseOfferPrice - promoPrice) * 100 / baseOfferPrice)
        if specialDiscount:
            discount = specialDiscount

        for promo in expected_promos:
            if promo == PromoType.CART_DISCOUNT:
                promos.append(
                    {
                        'type': PromoType.CART_DISCOUNT,
                        'itemsInfo': {
                            'promoPrice': {
                                'value': str(promoPrice),
                            },
                            'promoPriceWithTotalDiscount': {
                                'value': str(promoPrice),
                                "discount": discount,
                            },
                            'discount': {
                                'oldMin': str(prePromoPrice),
                                'absolute': str(prePromoPrice - promoPrice),
                            },
                        },
                    }
                )
            else:
                promos.append(
                    {
                        'type': promo.type_name,
                        'key': promo.key,
                    }
                )

        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": expected_title},
                "promos": promos,
                "prices": {
                    "value": str(promoPrice),
                    "discount": discount,
                },
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_promo_collision_promocode_only(self):
        """
        Проверям что при наличии другой акции (promocode), pricedrop работает
        """

        # cart-discount нет, работает только промокод
        self._test_promo_collision(
            MULTIPROMO_QUERY.format(hyperid="46955401", cart=""), "Катушка", [blue_promocode], 200, None
        )

    def test_promo_collision_promocode_and_pricedrop(self):
        # Прайсдроп совместим с промокодом, цена снижается на 25%. Промокод не влияет на цену
        self._test_promo_collision(
            MULTIPROMO_QUERY.format(hyperid="46955401", cart=b64url_md5("fishing-rod-1")),
            "Катушка",
            [blue_promocode, PromoType.CART_DISCOUNT],
            150,
            200,
            {"oldMin": "200", "percent": 25},
        )

    def test_promo_collision_promocode_pricedrop_and_cashback(self):
        # Кэшбек доступен пользователю, становится первым в списке промо, не влияет на цены/скидки
        self._test_promo_collision(
            MULTIPROMO_QUERY.format(hyperid="46955402", cart=b64url_md5("fishing-rod-1")) + '&perks=yandex_cashback',
            "Улучшенная катушка",
            [blue_cashback_1, blue_promocode, PromoType.CART_DISCOUNT],
            150,
            200,
            {"oldMin": "200", "percent": 25},
        )

        # Кэшбек недоступен пользователю, применяется та же логика, что и в предыдущем тесте
        self._test_promo_collision(
            MULTIPROMO_QUERY.format(hyperid="46955402", cart=b64url_md5("fishing-rod-1")),
            "Улучшенная катушка",
            [blue_promocode, PromoType.CART_DISCOUNT],
            150,
            200,
            {"oldMin": "200", "percent": 25},
        )

    def test_promo_collision_direct_discount_pricedrop_and_cashback(self):
        # Кэшбек доступен пользователю, становится первым в списке промо, не влияет на цены/скидки
        # Сперва применяется прямая скидка, цена становится 180, затем прайсдроп, 25% от 180 = 135
        self._test_promo_collision(
            MULTIPROMO_QUERY.format(hyperid="46955403", cart=b64url_md5("fishing-rod-1")) + '&perks=yandex_cashback',
            "Катушка со скидкой",
            [blue_cashback_1, direct_discount_1, PromoType.CART_DISCOUNT],
            135,
            180,
            {"oldMin": "200", "percent": 32},
            200,
        )

        # Кэшбек недоступен пользователю, поэтому его нет в списке промо. В остальном логика та же, что и выше
        self._test_promo_collision(
            MULTIPROMO_QUERY.format(hyperid="46955403", cart=b64url_md5("fishing-rod-1")),
            "Катушка со скидкой",
            [direct_discount_1, PromoType.CART_DISCOUNT],
            135,
            180,
            {"oldMin": "200", "percent": 32},
            200,
        )

    def test_promo_collision_promocode_direct_discount_pricedrop_and_cashback(self):
        # Кэшбек доступен пользователю, всего на оффер 4 промо, а максимум можно только 3.
        # По приоритету прайсдроп отбрасывается
        self._test_promo_collision(
            MULTIPROMO_QUERY.format(hyperid="46955404", cart=b64url_md5("fishing-rod-1")) + '&perks=yandex_cashback',
            "Другая катушка со скидкой",
            [blue_cashback_1, direct_discount_2, blue_promocode],
            180,
            200,
            {"oldMin": "200", "percent": 10},
        )

        # Кэшбек недоступен пользователю. Всего 3 промо и они укладываются в лимит на их количество
        # базовая цена оффера 200
        # сначала direct-discount выставляет её в 180
        # затем cart-discount 180 уменьшает на 25% (т.е. 45 руб), цена становится 135
        self._test_promo_collision(
            MULTIPROMO_QUERY.format(hyperid="46955404", cart=b64url_md5("fishing-rod-1")),
            "Другая катушка со скидкой",
            [direct_discount_2, blue_promocode, PromoType.CART_DISCOUNT],
            135,
            180,
            {"oldMin": "200", "percent": 32},
            200,
        )

    def test_promo_collision_blue_flash_and_pricedrop(self):
        # Флеш-акции не совместимы с прайсдропом и выше него по приоритету
        self._test_promo_collision(
            MULTIPROMO_QUERY.format(hyperid="46955405", cart=b64url_md5("fishing-rod-1")),
            "Катушка по флеш-акции",
            [blue_flash],
            140,
            200,
            {"oldMin": "200", "percent": 30},
        )

    @classmethod
    def prepare_promo_collision_blue_set_secondary(cls):
        # Комплект из шоколада и конфет. Конфеты должны быть вторичными,
        # т.е. в выдаче у них должен быть промо blue-set-secondary
        promo_blue_set_secondary = Promo(
            promo_type=PromoType.BLUE_SET,
            feed_id=1111,
            key=b64url_md5("blue_set_secondary_test_promo"),
            url='http://яндекс.рф/',
            blue_set=PromoBlueSet(
                sets_content=[
                    {
                        'items': [
                            {'offer_id': "chocolates-1", 'discount': 10},
                            {'offer_id': "sweets-1"},
                        ],
                    }
                ],
            ),
        )

        cls.index.promos += [promo_blue_set_secondary, blue_flash, direct_discount_1, direct_discount_2]
        cls.settings.loyalty_enabled = True
        cls.dynamic.loyalty += [
            DynamicBlueGenericBundlesPromos(
                whitelist=[
                    promo_blue_set_secondary.key,
                    blue_flash.key,
                    direct_discount_1.key,
                    direct_discount_2.key,
                ]
            )
        ]

        cls.index.hypertree += [
            HyperCategory(hid=91539, name="Печеньки", output_type=HyperCategoryType.GURU),
            HyperCategory(hid=91549, name="Конфетки", output_type=HyperCategoryType.GURU),
            HyperCategory(hid=91559, name="Шоколадки", output_type=HyperCategoryType.GURU),
        ]

        # За категорию печенек в корзине решено выдавать пд-скидку на категорию конфет
        cls.index.promos_by_cart += [PromoByCart(cart_hid=91539, promo_hid=91549, percent=25)]

        cls.index.models += [
            Model(hid=91539, hyperid=9153901, title='Cookies'),
            Model(hid=91549, hyperid=9154901, title='Sweets'),
            Model(hid=91559, hyperid=9155901, title='Chocolates'),
        ]

        cls.index.mskus += [
            MarketSku(
                title='Cookies',
                hyperid=9153901,
                sku=91539001,
                hid=91539,
                delivery_buckets=[1234],
                blue_offers=[BlueOffer(price=100, feedid=1111, offerid="cookies-1", waremd5=b64url_md5("cookies-1"))],
            ),
            MarketSku(
                title='Sweets',
                hyperid=9154901,
                sku=91549001,
                hid=91549,
                delivery_buckets=[1234],
                blue_offers=[BlueOffer(price=100, feedid=1111, offerid="sweets-1", waremd5=b64url_md5("sweets-1"))],
            ),
            MarketSku(
                title='Sweets',
                hyperid=9155901,
                sku=91559001,
                hid=91559,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(price=100, feedid=1111, offerid="chocolates-1", waremd5=b64url_md5("chocolates-1"))
                ],
            ),
        ]

        cls.dj.on_request(yandexuid='10110231').respond(
            [  # этот пользователь придёт с печеньками в корзине
                DjModel(id='9154901'),  # и dj порекомендует ему конфеты
            ]
        )

    def test_promo_collision_blue_set_secondary(self):
        query = (
            'place={place}&rids=213&rgb=blue'
            + '&rearr-factors=market_dj_exp_for_promo_cart_force=1'
            + '&yandexuid={yandexuid}&cart={cart}'
        )

        # Ожидаем отсутствие акций в отвте, т.к. промо с типом cart-discount несовместимо с промо blue-set-secondary
        for place, extra in (
            ('prime', '&hyperid=9154901&text=Sweets&promo-type=cart-discount'),
            ('promo_cart_discount', ''),
        ):
            response = self.report.request_json(
                query.format(
                    place=place,
                    yandexuid='10110231',
                    cart=b64url_md5("cookies-1"),
                )
                + extra
            )
            self.assertFragmentNotIn(
                response,
                {
                    "titles": {"raw": "Sweets"},
                },
            )

    @classmethod
    def prepare_user_orders_history(cls):

        # Будет много сопутки и частотка не влезет
        # от того что у пользователя есть старые заказы - выдача будет не пустой
        cls.index.hypertree += [
            HyperCategory(hid=8476098),  # 8476098 категория которая дает скидку на кучу других категорий
            HyperCategory(hid=13276667),
            HyperCategory(hid=4748064),
            HyperCategory(hid=4854062),
            HyperCategory(hid=8476539),
            HyperCategory(hid=4748057),
            HyperCategory(hid=13239358),
            HyperCategory(hid=13240862),
            HyperCategory(hid=14996659),
            HyperCategory(hid=4748066),
            HyperCategory(hid=7693914),
            HyperCategory(hid=15011042),
            HyperCategory(hid=14996686),
            HyperCategory(hid=14996541),
            HyperCategory(hid=8476110),
            HyperCategory(hid=91184),
            HyperCategory(hid=8480752),
            HyperCategory(hid=8476099),
            HyperCategory(hid=13239527),
            HyperCategory(hid=91183),
            HyperCategory(hid=4748062),
            HyperCategory(hid=13239550),
            HyperCategory(hid=13239503),
            HyperCategory(hid=13277108),
            HyperCategory(hid=13314855),
            # HyperCategory(hid=4748072), # эта категория уже объявлена ранее - но на нее тоже есть скидка
        ]

        cls.index.promos_by_cart += [
            PromoByCart(cart_hid=8476098, promo_hid=13276667, percent=15),
            PromoByCart(cart_hid=8476098, promo_hid=4748064, percent=15),
            PromoByCart(cart_hid=8476098, promo_hid=4854062, percent=15),
            PromoByCart(cart_hid=8476098, promo_hid=8476539, percent=12),
            PromoByCart(cart_hid=8476098, promo_hid=4748057, percent=12),
            PromoByCart(cart_hid=8476098, promo_hid=13239358, percent=12),
            PromoByCart(cart_hid=8476098, promo_hid=13240862, percent=12),
            PromoByCart(cart_hid=8476098, promo_hid=14996659, percent=12),
            PromoByCart(cart_hid=8476098, promo_hid=4748066, percent=12),
            PromoByCart(cart_hid=8476098, promo_hid=7693914, percent=12),
            PromoByCart(cart_hid=8476098, promo_hid=15011042, percent=10),
            PromoByCart(cart_hid=8476098, promo_hid=14996686, percent=10),
            PromoByCart(cart_hid=8476098, promo_hid=14996541, percent=10),
            PromoByCart(cart_hid=8476098, promo_hid=8476110, percent=10),
            PromoByCart(cart_hid=8476098, promo_hid=91184, percent=10),
            PromoByCart(cart_hid=8476098, promo_hid=8480752, percent=8),
            PromoByCart(cart_hid=8476098, promo_hid=8476099, percent=8),
            PromoByCart(cart_hid=8476098, promo_hid=13239527, percent=8),
            PromoByCart(cart_hid=8476098, promo_hid=91183, percent=8),
            PromoByCart(cart_hid=8476098, promo_hid=4748062, percent=8),
            PromoByCart(cart_hid=8476098, promo_hid=13239550, percent=8),
            PromoByCart(cart_hid=8476098, promo_hid=13239503, percent=6),
            PromoByCart(cart_hid=8476098, promo_hid=13277108, percent=6),
            PromoByCart(cart_hid=8476098, promo_hid=13314855, percent=6),
            PromoByCart(cart_hid=8476098, promo_hid=4748072, percent=6),
        ]

        # оффер из корзины дает скидку на много сопутки - но большинство категорий пусты ее нет в продаже
        cls.index.models += [Model(hid=8476098, hyperid=847609801, title="Гнилой апельсин")]
        cls.index.mskus += [
            MarketSku(
                title='Гнилой апельсин',
                hyperid=847609801,
                sku=8476098001,
                hid=8476098,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(price=1000, feedid=1111, offerid="current-order", waremd5="wRdDS_jtPI9ej0y0wSxcCg")
                ],
            )
        ]

        cls.index.models += [Model(hid=13239358, hyperid=1323935801, title="Ночной горшок")]
        cls.index.mskus += [
            MarketSku(
                title='Ночной горшок с розочками',
                hyperid=1323935801,
                sku=13239358001,
                hid=13239358,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=1000, feedid=1111, offerid="current-order-with-promo", waremd5="9_ZLEoEbn8HNIx9K7B4qcw"
                    )
                ],
            )
        ]

        # пользователь заказывал ранее - Туалетную бумагу (8) Фейри (5) и Замок для велосипеда (2)
        # туалетка покажется все равно даже если она не вошла в список bestHids (туда войдет только частотка)
        # замок для велосипеда не покажется, т.к. у него нет скидки при текущей корзине
        counters = [
            BeruModelOrderLastTimeCounter(
                model_order_events=[
                    ModelLastOrderEvent(model_id=8, timestamp=478419200),  # туалетка
                    ModelLastOrderEvent(model_id=2, timestamp=478410200),  # замок - на него не будет скидона
                    ModelLastOrderEvent(model_id=5, timestamp=478310200),  # фейри
                ]
            ),
        ]

        cls.bigb.on_request(yandexuid=24846004, client='merch-machine').respond(counters=counters)
        cls.bigb.on_default_request().respond()

        cls.dj.on_request(exp=DEFAULT_DJ_EXP, yandexuid='24846004').respond(
            [
                DjModel(id='2'),  # замок - на него не будет скидки
                DjModel(id='8'),  # туалетка
                DjModel(id='5'),  # фейри
            ]
        )

        cls.dj.on_request(exp='dj_exp2', yandexuid='24846004').respond(
            [
                DjModel(id='5'),  # фейри
            ]
        )

    def test_user_orders_history_after_accessorizes(self):
        """При отключенном dj
        Пользователь для которого нет профиля видит один оффер из большого числа категорий сопутки
        Пользователь у которого в профиле есть заказы - видит еще и свои заказанные товары
        На них есть скидка, но их категории не не попали в bestHids
        Заказанные ранее товары идут после сопутки
        """

        # оффер в корзине wRdDS_jtPI9ej0y0wSxcCg имеет много сопутствующих категорий
        # но реально находится всего однин сопутствующий документ

        response = self.report.request_json(
            'place=promo_cart_discount&rgb=blue&cart=wRdDS_jtPI9ej0y0wSxcCg&rids=213'
            '&rearr-factors=market_dj_exp_for_promo_cart_disable=1'
            '&allow-collapsing=0'
        )

        self.assertFragmentIn(
            response,
            {"search": {"total": 1, "results": [{"titles": {"raw": "Ночной горшок с розочками"}}]}},
            preserve_order=True,
            allow_different_len=False,
        )
        self.error_log.ignore(code=EXTREQUEST_DJ_EMPTY_RECOM)

        # пользователь с yandexuid=24846004 ранее заказывал Фейри и Туалетную бумагу: эти товары добавляются после сопутки и перед частоткой (которой нет)
        response = self.report.request_json(
            'place=promo_cart_discount&rgb=blue&cart=wRdDS_jtPI9ej0y0wSxcCg&rids=213&yandexuid=24846004'
            '&rearr-factors=market_dj_exp_for_promo_cart_disable=1'
            '&allow-collapsing=0'
        )

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,  # total не меняем чтобы не сбивать пагинацию
                    "results": [
                        {"titles": {"raw": "Ночной горшок с розочками"}},
                        {"titles": {"raw": "Туалетка от Беру"}},
                        {"titles": {"raw": "Туалетка от Беру и со скидкой"}},
                        {"titles": {"raw": "Фейри от Беру"}},
                        {"titles": {"raw": "Фейри от Беру и со скидкой"}},
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )
        self.error_log.ignore(code=EXTREQUEST_DJ_EMPTY_RECOM)

        # товары из прошлых заказов появляются только на первой странице
        response = self.report.request_json(
            'place=promo_cart_discount&rgb=blue&cart=wRdDS_jtPI9ej0y0wSxcCg&rids=213&yandexuid=24846004'
            '&rearr-factors=market_dj_exp_for_promo_cart_disable=1'
            '&allow-collapsing=0&page=2'
        )

        self.assertFragmentIn(
            response,
            {"search": {"total": 1, "results": [{"titles": {"raw": "Ночной горшок с розочками"}}]}},
            preserve_order=True,
            allow_different_len=False,
        )
        self.error_log.ignore(code=EXTREQUEST_DJ_EMPTY_RECOM)

    def test_user_orders_history_before_frequent_categories(self):
        """При отключенном dj
        Пользователь для которого нет профиля видит сопутку и частотку
        Пользователь у которого в профиле есть заказы - видит еще и свои заказанные товары
        Заказанные ранее товары идут после сопутки но до частотки и могут дублироваться
        """

        # BLUEModel4FEED2222dddQ - мультиварка из hid=4954975 - оффер от Беру со склада 147
        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&rgb=blue&cart=BLUEModel4FEED2222dddQ'
            '&rearr-factors=market_dj_exp_for_promo_cart_disable=1'
            '&allow-collapsing=0'
        )

        feiry = "Фейри"
        muskul = "Мистер Мускул"
        tualetka = "Туалетка"

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 3,
                    "results": [
                        # сопутка
                        T.ExpectedKitchen5.offer_from_beru_147(muskul),
                        # вот сюда встроятся заказанные ранее товары
                        # частотка
                        T.ExpectedToilet5.offer_from_beru_147(tualetka),
                        # сопутка
                        T.ExpectedKitchen5.offer_from_beru_147(feiry),
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&rgb=blue&cart=BLUEModel4FEED2222dddQ&yandexuid=24846004'
            '&rearr-factors=market_dj_exp_for_promo_cart_disable=1'
            '&allow-collapsing=0'
        )

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 3,
                    "results": [
                        # сопутка
                        T.ExpectedKitchen5.offer_from_beru_147(muskul),
                        # заказанные ранее товары
                        T.ExpectedToilet5.offer_from_beru_147(tualetka),
                        T.ExpectedKitchen5.offer_from_beru_147(feiry),
                        # частотка
                        T.ExpectedToilet5.offer_from_beru_147(tualetka),
                        # сопутка
                        T.ExpectedKitchen5.offer_from_beru_147(feiry),
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_dj_default_exp(self):
        """
        Проверяем что сначала идут модели из ответа dj с дефолтным экспом,
        на которые есть скидки, а потом доки из категорий
        """

        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&rgb=blue&cart=BLUEModel4FEED2222dddQ&yandexuid=24846004&allow-collapsing=0'
        )

        feiry = "Фейри"
        muskul = "Мистер Мускул"
        tualetka = "Туалетка"

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 3,
                    "results": [
                        # dj
                        T.ExpectedToilet5.offer_from_beru_147(tualetka),
                        T.ExpectedKitchen5.offer_from_beru_147(feiry),
                        # сопутка
                        T.ExpectedKitchen5.offer_from_beru_147(muskul),
                        # частотка
                        T.ExpectedToilet5.offer_from_beru_147(tualetka),
                        # сопутка (по второму кругу)
                        T.ExpectedKitchen5.offer_from_beru_147(feiry),
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_dj_model_hashing(self):
        """
        Проверяем что работает хэширование в promo_cart_discount и возвращается в моделях.
        Проверяем, что при прокидывании флага pdc промка присваивается только при совпадении переданного хэша
        """

        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&rgb=blue&cart=BLUEModel4FEED2222dddQ&yandexuid=24846004&allow-collapsing=1'
        )

        self.assertFragmentIn(response, {"search": {"total": 3, "results": [{'promoCartDiscountHash': NotEmpty()}]}})

        id1 = response.root['search']['results'][0]['id']
        hash1 = response.root['search']['results'][0]['promoCartDiscountHash']

        # check that if right hash is passed, promo cart discount is applied
        response = self.report.request_json(
            'place=prime&rearr-factors=market_promo_cart_ignore_filters=1&hyperid={id}&text=клиент+всегда+у&rids=213&rgb=blue&cart=BLUEModel4FEED2222dddQ&pdc={hash}'.format(
                id=id1, hash=hash1
            )
        )

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [{"offers": {"items": [{"promos": [{"type": "cart-discount"}]}]}}],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        wrong_hash = "123"
        # check that if wrong hash is passed, promo cart discount is not applied
        response = self.report.request_json(
            'place=prime&rearr-factors=market_promo_cart_ignore_filters=1&hyperid={id}&text=клиент+всегда+у&rids=213&rgb=blue&cart=BLUEModel4FEED2222dddQ&pdc={hash}'.format(
                id=id1, hash=wrong_hash
            )
        )

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [{"offers": {"items": [{"promos": NoKey("promos")}]}}],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_dj_fixed_models(self):
        """
        Флаг market_promo_cart_dj_fixed_models не меняет выдачу
        """

        def compare_dicts(a, b, layout):
            for k, v in layout.items():
                if k not in a and k not in b:
                    continue
                if v == '*':
                    if a[k] != b[k]:
                        self.assertFalse("values '{}' doesn't match, {} != {}".format(k, a[k], b[k]))
                else:
                    compare_dicts(a[k], b[k], v)

        def compare_results(response_a, response_b, layout):
            self.assertEqual(response_a.root['search']['total'], response_b.root['search']['total'])
            results_a = response_a.root['search']['results']
            results_b = response_b.root['search']['results']
            for a, b in zip(results_a, results_b):
                compare_dicts(a, b, layout)

        layout = {
            'entity': '*',
            'titles': '*',
            'promos': '*',
            'prices': {
                'discount': {
                    'oldMin': '*',
                    'percent': '*',
                    # TODO(vdimir@) doesn't match:
                    # 'isBestDeal': '*',
                },
                'value': '*',
                'oldDiscount': {'oldMin': '*', 'percent': '*'},
            },
        }

        response_a = self.report.request_json(
            'place=promo_cart_discount&rids=213&rgb=blue&cart=BLUEModel4FEED2222dddQ&yandexuid=24846004'
            '&rearr-factors=market_promo_cart_dj_fixed_models=0'
        )

        response_b = self.report.request_json(
            'place=promo_cart_discount&rids=213&rgb=blue&cart=BLUEModel4FEED2222dddQ&yandexuid=24846004'
            '&rearr-factors=market_promo_cart_dj_fixed_models=1'
        )

        compare_results(response_a, response_b, layout)

        # c allow-collapsing=0
        response_a = self.report.request_json(
            'place=promo_cart_discount&rids=213&rgb=blue&cart=BLUEModel4FEED2222dddQ&yandexuid=24846004'
            '&allow-collapsing=0'
            '&rearr-factors=market_promo_cart_dj_fixed_models=0'
        )

        response_b = self.report.request_json(
            'place=promo_cart_discount&rids=213&rgb=blue&cart=BLUEModel4FEED2222dddQ&yandexuid=24846004'
            '&allow-collapsing=0'
            '&rearr-factors=market_promo_cart_dj_fixed_models=1'
        )

        compare_results(response_a, response_b, layout)

    def test_dj_fixed_models_with_min_category_restriction(self):
        """
        Если флаг market_promo_cart_dj_minimal_categories_count задан, а количество разных категорий
        в выдаче меньше его значения, то выдача должна стать пустой
        """

        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&rgb=blue&cart=BLUEModel4FEED2222dddQ&yandexuid=24846004&debug=da'
            '&rearr-factors=market_promo_cart_dj_fixed_models=1;market_promo_cart_dj_minimal_categories_count=3'
        )

        self.assertFragmentIn(response, {"logicTrace": [Contains("Result contains: DjResults: 0")]})

        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&rgb=blue&cart=BLUEModel4FEED2222dddQ&yandexuid=24846004&debug=da'
            '&rearr-factors=market_promo_cart_dj_fixed_models=1;market_promo_cart_dj_minimal_categories_count=2'
        )

        self.assertFragmentIn(response, {"logicTrace": [Contains("Result contains: DjResults: 2")]})

    def test_max_models_report_cut(self):
        """
        Проверяем работу флага market_promo_cart_discount_max_from_dj
        """
        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&rgb=blue&cart=BLUEModel4FEED2222dddQ&yandexuid=24846004'
            '&rearr-factors=market_promo_cart_discount_max_from_dj=2&allow-collapsing=0'
        )

        feiry = "Фейри"
        muskul = "Мистер Мускул"
        tualetka = "Туалетка"

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 3,
                    "results": [
                        # dj
                        T.ExpectedToilet5.offer_from_beru_147(tualetka),
                        # сопутка
                        T.ExpectedKitchen5.offer_from_beru_147(muskul),
                        # частотка
                        T.ExpectedToilet5.offer_from_beru_147(tualetka),
                        # сопутка (по второму кругу)
                        T.ExpectedKitchen5.offer_from_beru_147(feiry),
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_max_models_dj_cut(self):
        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&rgb=blue&cart=BLUEModel4FEED2222dddQ&yandexuid=24846006'
            '&rearr-factors=market_promo_cart_discount_max_from_dj=2;market_dj_exp_for_promo_cart_force=1&allow-collapsing=0'
        )
        feiry = "Фейри"
        tualetka = "Туалетка"
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "results": [
                        T.ExpectedToilet5.offer_from_beru_147(tualetka),
                        T.ExpectedKitchen5.offer_from_beru_147(feiry),
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&rgb=blue&cart=BLUEModel4FEED2222dddQ&yandexuid=24846006'
            '&rearr-factors=market_dj_exp_for_promo_cart_force=1;market_promo_cart_discount_max_from_dj=1&allow-collapsing=0'
        )
        feiry = "Фейри"
        tualetka = "Туалетка"
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        T.ExpectedKitchen5.offer_from_beru_147(feiry),
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_dj_request_contains_pd(self):
        """
        Скидка по price drop пробрасывается в DJ
        """
        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&rgb=blue&cart=BLUEModel41FEED9999eee'
            '&yandexuid=251539001&debug=1&rearr-factors=market_dj_exp_for_promo_cart_force=1'
        )

        self.assertFragmentIn(
            response,
            {
                'recomOutputType': 'pricedrop',
                'results': [
                    {'entity': 'product', 'id': 42},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_dj_request_contains_warehouse(self):
        """
        Склад пробрасывается в DJ
        """
        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&rgb=blue'
            '&cart=BLUEModel4FEED2222dddQ,BLUEModel1FEED3333wwww&yandexuid=24846004'
            '&rearr-factors=market_promo_by_user_cart_hids=1&allow-collapsing=0'
            '&debug=1'
        )

        self.assertFragmentIn(
            response, {'logicTrace': [T.RegexUnicodeFriendly(r'Dj search in warehouses (147, 145|147, 145).')]}
        )

        # используется ffWarehouseId (147), а не warehouseId (999)
        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&rgb=blue'
            '&allow-collapsing=0'
            '&cart=BLUEModel1FEED4444xxxw'
            '&rearr-factors=market_promo_by_user_cart_split=s2'
            '&debug=1'
        )

        self.assertFragmentIn(response, {'logicTrace': [T.RegexUnicodeFriendly(r'Dj search in warehouses 147\.')]})

        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&rgb=blue'
            '&cart=BLUEModel4FEED2222dddQ,BLUEModel1FEED3333wwww&yandexuid=24846004'
            '&rearr-factors=market_promo_by_user_cart_hids=1;market_dj_promo_cart_pass_warehouse=0'
            '&allow-collapsing=0'
            '&debug=1'
        )

        self.assertFragmentNotIn(
            response, {'logicTrace': [T.RegexUnicodeFriendly(r'Dj search in warehouses (147, 145|147, 145)\.')]}
        )

    def test_dj_other_exp(self):
        """
        Проверяем что сначала идут модели из ответа dj с указанным экспом,
        на которые есть скидки, а потом доки из категорий
        """

        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&rgb=blue&cart=BLUEModel4FEED2222dddQ&yandexuid=24846004'
            '&rearr-factors=market_dj_exp_for_promo_cart_discount=dj_exp2&allow-collapsing=0'
        )

        feiry = "Фейри"
        muskul = "Мистер Мускул"
        tualetka = "Туалетка"

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 3,
                    "results": [
                        # dj
                        T.ExpectedKitchen5.offer_from_beru_147(feiry),
                        # сопутка
                        T.ExpectedKitchen5.offer_from_beru_147(muskul),
                        # частотка
                        T.ExpectedToilet5.offer_from_beru_147(tualetka),
                        # сопутка (по второму кругу)
                        T.ExpectedKitchen5.offer_from_beru_147(feiry),
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_dj_candidates_yasm_signals(self):
        tass_data_before = self.report.request_tass()
        self.report.request_json(
            'place=promo_cart_discount&rids=213&rgb=blue&cart=BLUEModel4FEED2222dddQ'
            '&yandexuid=24846004&allow-collapsing=0'
        )

        self._check_yasm_hgram_signals(
            tass_data_before,
            {
                'pricedrop_exp_default_for_pricedrop_dj_candidates_hgram': [20, 0],
                'pricedrop_exp_default_for_pricedrop_besthids_candidates_hgram': [1, 1],
                'pricedrop_exp_default_for_pricedrop_orderhistory_candidates_hgram': [0, 1],
                'pricedrop_exp_default_for_pricedrop_total_candidates_hgram': [5, 1],
            },
        )

        tass_data_before = self.report.request_tass()
        self.report.request_json(
            'place=promo_cart_discount&rids=213&rgb=blue&cart=BLUEModel4FEED2222dddQ&yandexuid=24846004'
            '&rearr-factors=market_dj_exp_for_promo_cart_discount=dj_exp2&allow-collapsing=0'
        )

        self._check_yasm_hgram_signals(
            tass_data_before,
            {
                'pricedrop_exp_dj_exp2_dj_candidates_hgram': [1, 1],
                'pricedrop_exp_dj_exp2_besthids_candidates_hgram': [1, 1],
                'pricedrop_exp_dj_exp2_orderhistory_candidates_hgram': [0, 1],
                'pricedrop_exp_dj_exp2_total_candidates_hgram': [1, 1],
            },
        )

        self.report.request_json(
            'place=promo_cart_discount&rids=213&rgb=blue&cart=BLUEModel4FEED2222dddQ&yandexuid=24846004'
            '&rearr-factors=market_dj_exp_for_promo_cart_discount=dj_exp2&allow-collapsing=0'
        )

        self._check_yasm_hgram_signals(
            tass_data_before,
            {
                'pricedrop_exp_dj_exp2_dj_candidates_hgram': [1, 2],
                'pricedrop_exp_dj_exp2_besthids_candidates_hgram': [1, 2],
                'pricedrop_exp_dj_exp2_orderhistory_candidates_hgram': [0, 2],
                'pricedrop_exp_dj_exp2_total_candidates_hgram': [1, 2],
            },
        )

    def test_dj_page2(self):
        """Проверяем что dj не появляется на второй странице"""

        feiry = "Фейри"
        muskul = "Мистер Мускул"
        tualetka = "Туалетка"

        # на первой странице 2 товара и товары от dj
        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&rgb=blue&cart=BLUEModel4FEED2222dddQ'
            '&yandexuid=24846004&page=1&numdoc=2&allow-collapsing=0'
        )

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 3,
                    "results": [
                        # dj (just inserted before without paging)
                        T.ExpectedToilet5.offer_from_beru_147(tualetka),
                        T.ExpectedKitchen5.offer_from_beru_147(feiry),
                        # default
                        T.ExpectedKitchen5.offer_from_beru_147(muskul),  # №1
                        T.ExpectedToilet5.offer_from_beru_147(tualetka),  # №2
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # на второй странице продолжаются товары зацикленные по категориям
        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&rgb=blue&cart=BLUEModel4FEED2222dddQ'
            '&yandexuid=24846004&page=2&numdoc=2&allow-collapsing=0'
        )

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 3,
                    "results": [
                        # default
                        T.ExpectedKitchen5.offer_from_beru_147(feiry),  # №3
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_dj_force(self):
        """
        Проверяем что c флагом market_dj_exp_for_promo_cart_force работает только DJ
        """

        response = self.report.request_json(
            'place=promo_cart_discount&rids=213'
            '&rgb=blue&cart=BLUEModel4FEED2222dddQ'
            '&yandexuid=24846004&allow-collapsing=0'
            '&rearr-factors=market_dj_exp_for_promo_cart_force=1'
        )

        feiry = "Фейри"
        tualetka = "Туалетка"

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "results": [
                        T.ExpectedToilet5.offer_from_beru_147(tualetka),
                        T.ExpectedKitchen5.offer_from_beru_147(feiry),
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

    @classmethod
    def prepare_dj_paging_with_memcached(cls):
        cls.settings.memcache_enabled = True
        cls.dj.on_request(exp=DEFAULT_DJ_EXP, yandexuid='12311322').respond(
            [
                DjModel(id='8'),  # туалетка
            ]
        )
        cls.dj.on_request(exp=DEFAULT_DJ_EXP, yandexuid='12311322', shown_model_ids='8').respond(
            [
                DjModel(id='5'),  # фейри
            ]
        )
        cls.dj.on_request(exp=DEFAULT_DJ_EXP, yandexuid='12311322', shown_model_ids='8,5').respond(
            [
                # Ничего
            ]
        )

    def test_dj_paging_with_memcached(self):
        """Проверяем что одновременно под флагами market_dj_exp_for_promo_cart_force и dj_paging_promo_cart_discount
        работает пейджинг на уровне dj"""

        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&page=1'
            '&rgb=blue&cart=BLUEModel4FEED2222dddQ'
            '&yandexuid=12311322&allow-collapsing=0'
            '&rearr-factors=market_dj_exp_for_promo_cart_force=1'
            '&rearr-factors=dj_paging_promo_cart_discount=1&view-unique-id=1'
        )

        feiry = "Фейри"
        tualetka = "Туалетка"

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        T.ExpectedToilet5.offer_from_beru_147(tualetka),
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&page=2'
            '&rgb=blue&cart=BLUEModel4FEED2222dddQ'
            '&yandexuid=12311322&allow-collapsing=0'
            '&rearr-factors=market_dj_exp_for_promo_cart_force=1'
            '&rearr-factors=dj_paging_promo_cart_discount=1&view-unique-id=1'
        )

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        T.ExpectedKitchen5.offer_from_beru_147(feiry),
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&page=3'
            '&rgb=blue&cart=BLUEModel4FEED2222dddQ'
            '&yandexuid=12311322&allow-collapsing=0'
            '&rearr-factors=market_dj_exp_for_promo_cart_force=1'
            '&rearr-factors=dj_paging_promo_cart_discount=1&view-unique-id=1'
        )

        self.assertFragmentIn(
            response, {"search": {"total": 0, "results": []}}, allow_different_len=False, preserve_order=True
        )

    @classmethod
    def prepare_dj_paging_with_memcached_for_validation(cls):
        cls.settings.memcache_enabled = True
        cls.dj.on_request(exp=DEFAULT_DJ_EXP, yandexuid='12385322').respond(
            [
                DjModel(id='8'),  # туалетка
            ]
        )

        #  Assume, that dj didn't have filtration in it. Report is supposed to handle it.
        cls.dj.on_request(exp=DEFAULT_DJ_EXP, yandexuid='12385322', shown_model_ids='8').respond(
            [
                DjModel(id='8'),  # туалетка
                DjModel(id='5'),  # фейри
            ]
        )

        #  Assume, that dj didn't have filtration in it. Report is supposed to handle it.
        cls.dj.on_request(exp=DEFAULT_DJ_EXP, yandexuid='12385322', shown_model_ids='8,5').respond(
            [
                DjModel(id='8'),  # туалетка
                DjModel(id='5'),  # фейри
            ]
        )

    def test_dj_paging_with_broken_memcached_validation(self):
        """Под флагом verify_dj_paging_for_pricedrop (по дефолту включен) проверяем,
        что report пытается исправлять кеш-миссы"""

        self.error_log.expect(code=4026)

        # Запрос сразу в page=2.
        # Кеш должен оказаться пустым.
        # report должен вернуть пустой ответ, потому что кеш пуст при page > 1.
        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&page=2'
            '&rgb=blue&cart=BLUEModel4FEED2222dddQ'
            '&yandexuid=12311322&allow-collapsing=0'
            '&rearr-factors=market_dj_exp_for_promo_cart_force=1'
            '&rearr-factors=dj_paging_promo_cart_discount=1&view-unique-id=1271638'
        )
        self.assertFragmentIn(response, {"search": {"total": 0}})

    def test_dj_paging_with_broken_dj_validation(self):
        """Под флагом verify_dj_paging_for_pricedrop (по дефолту включен) проверяем,
        что report пытается исправлять ситуации, когда dj не фильтрует уже показанные товары"""

        # self.error_log.expect(code=4027)

        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&page=1'
            '&rgb=blue&cart=BLUEModel4FEED2222dddQ'
            '&yandexuid=12385322&allow-collapsing=0'
            '&rearr-factors=market_dj_exp_for_promo_cart_force=1'
            '&rearr-factors=dj_paging_promo_cart_discount=1&view-unique-id=2'
        )

        feiry = "Фейри"
        tualetka = "Туалетка"

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        T.ExpectedToilet5.offer_from_beru_147(tualetka),
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&page=2'
            '&rgb=blue&cart=BLUEModel4FEED2222dddQ'
            '&yandexuid=12385322&allow-collapsing=0'
            '&rearr-factors=market_dj_exp_for_promo_cart_force=1'
            '&rearr-factors=dj_paging_promo_cart_discount=1&view-unique-id=2'
        )

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        T.ExpectedKitchen5.offer_from_beru_147(feiry),
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&page=3'
            '&rgb=blue&cart=BLUEModel4FEED2222dddQ'
            '&yandexuid=12385322&allow-collapsing=0'
            '&rearr-factors=market_dj_exp_for_promo_cart_force=1'
            '&rearr-factors=dj_paging_promo_cart_discount=1&view-unique-id=2'
        )

        self.assertFragmentIn(
            response, {"search": {"total": 0, "results": []}}, allow_different_len=False, preserve_order=True
        )

    @classmethod
    def prepare_dj_pd_split(cls):
        cls.dj.on_request(exp=DEFAULT_DJ_EXP, yandexuid='12345567', pd_split='DEFAULT').respond(
            [
                DjModel(id='5'),
            ]
        )
        cls.dj.on_request(exp=DEFAULT_DJ_EXP, yandexuid='12345567', pd_split='s1').respond(
            [
                DjModel(id='2'),
            ]
        )

    def test_dj_pd_split(self):
        """
        Cплит пробрасывается в DJ
        """

        feiry = "Фейри"
        zamok = "Замок для велосипеда"

        base_req = (
            'place=promo_cart_discount&rids=213'
            '&rgb=blue&cart=BLUEModel1FEED3333wwww,BLUEModel4FEED2222dddQ'
            '&yandexuid=12345567&allow-collapsing=0'
            '&rearr-factors=market_dj_exp_for_promo_cart_force=1{rearr}'
        )

        response = self.report.request_json(base_req.format(rearr=''))
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        T.ExpectedKitchen5.offer_from_beru_147(feiry),
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # TROUBLE
        response = self.report.request_json(base_req.format(rearr=';market_promo_by_user_cart_split=s1'))
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        T.ExpectedVelo5s1.offer_from_beru(zamok),
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_availability_checking(self):
        """
        Проверяем, что при наличии параметра check-availability=1 параметр page игнорируется, а выдача dj не кэшируется
        """

        feiry = "Фейри"
        tualetka = "Туалетка"

        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&page=2'
            '&rgb=blue&cart=BLUEModel4FEED2222dddQ'
            '&yandexuid=12311322&allow-collapsing=0'
            '&check-availability=1'
            '&rearr-factors=market_dj_exp_for_promo_cart_force=1'
            '&rearr-factors=dj_paging_promo_cart_discount=1&view-unique-id=2'
        )
        # Первая страница выдачи
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        T.ExpectedToilet5.offer_from_beru_147(tualetka),
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&page=1'
            '&rgb=blue&cart=BLUEModel4FEED2222dddQ'
            '&yandexuid=12311322&allow-collapsing=0'
            '&check-availability=1'
            '&rearr-factors=market_dj_exp_for_promo_cart_force=1'
            '&rearr-factors=dj_paging_promo_cart_discount=1&view-unique-id=2'
        )
        # Вновь первая страница выдачи
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        T.ExpectedToilet5.offer_from_beru_147(tualetka),
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&page=1'
            '&rgb=blue&cart=BLUEModel4FEED2222dddQ'
            '&yandexuid=12311322&allow-collapsing=0'
            '&rearr-factors=market_dj_exp_for_promo_cart_force=1'
            '&rearr-factors=dj_paging_promo_cart_discount=1&view-unique-id=2'
        )
        # И ещё раз первая страница выдачи
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        T.ExpectedToilet5.offer_from_beru_147(tualetka),
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&page=2'
            '&rgb=blue&cart=BLUEModel4FEED2222dddQ'
            '&yandexuid=12311322&allow-collapsing=0'
            '&rearr-factors=market_dj_exp_for_promo_cart_force=1'
            '&rearr-factors=dj_paging_promo_cart_discount=1&view-unique-id=2'
        )
        # Вторая страница выдачи
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        T.ExpectedKitchen5.offer_from_beru_147(feiry),
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

    @classmethod
    def prepare_filters(cls):
        """Некоторые категории товаров могут быть исключены из акций cart-discount"""

        cls.index.hypertree += [
            HyperCategory(hid=9478508, name="Фильмы и сериалы", output_type=HyperCategoryType.GURU),
            HyperCategory(hid=3734930, name="Попкорн", output_type=HyperCategoryType.GURU),
        ]

        cls.index.promos_by_cart += [PromoByCart(cart_hid=9478508, promo_hid=3734930, percent=25)]

        cls.index.gltypes = [
            GLType(param_id=101, name='Вкус', hid=3734930, gltype=GLType.ENUM, values=[1, 2, 3]),
            GLType(param_id=102, name='Обьем', hid=3734930, gltype=GLType.NUMERIC),
            GLType(param_id=103, name='Сладкий', hid=3734930, gltype=GLType.BOOL),
        ]

        cls.index.mskus += [
            MarketSku(
                title='Хурал',
                hyperid=947850801,
                sku=9478508001,
                hid=9478508,
                delivery_buckets=[1234],
                blue_offers=[BlueOffer(price=1000, feedid=1111, waremd5="JKH6C4occ62gxyJ4u7wXsg")],
            ),
        ]

        cls.index.models += [
            Model(hyperid=37349301, title='Попкорн сладкий с карамелью мал', hid=3734930),
            Model(hyperid=37349302, title='Попкорн сладкий с ванилью средн', hid=3734930),
            Model(hyperid=37349303, title='Попкорн со сванской солью бол', hid=3734930),
            Model(hyperid=37349304, title='Попкорн соленая карамель средн', hid=3734930),
        ]

        cls.index.mskus += [
            MarketSku(
                title='Попкорн сладкий с карамелью мал',
                hyperid=37349301,
                sku=373493001,
                hid=3734930,
                delivery_buckets=[1234],
                glparams=[
                    GLParam(param_id=101, value=1),
                    GLParam(param_id=102, value=350),
                    GLParam(param_id=103, value=1),
                ],
                blue_offers=[BlueOffer(price=1000, feedid=1111)],
            ),
            MarketSku(
                title='Попкорн сладкий с ванилью средн',
                hyperid=37349302,
                sku=373493002,
                hid=3734930,
                delivery_buckets=[1234],
                glparams=[
                    GLParam(param_id=101, value=2),
                    GLParam(param_id=102, value=500),
                    GLParam(param_id=103, value=1),
                ],
                blue_offers=[BlueOffer(price=1000, feedid=1111)],
            ),
            MarketSku(
                title='Попкорн со сванской солью бол',
                hyperid=37349303,
                sku=373493003,
                hid=3734930,
                delivery_buckets=[1234],
                glparams=[
                    GLParam(param_id=101, value=3),
                    GLParam(param_id=102, value=800),
                    GLParam(param_id=103, value=0),
                ],
                blue_offers=[BlueOffer(price=1000, feedid=1111)],
            ),
            MarketSku(
                title='Попкорн соленая карамель средн',
                hyperid=37349304,
                sku=373493004,
                hid=3734930,
                delivery_buckets=[1234],
                glparams=[
                    GLParam(param_id=101, value=1),
                    GLParam(param_id=102, value=500),
                    GLParam(param_id=103, value=0),
                ],
                blue_offers=[BlueOffer(price=1000, feedid=1111)],
            ),
        ]

        cls.index.promos_by_cart_filters += [
            FilterByHid(hid=3734930, glfilter='101:1', split='no_caramel_popcorn'),  # запрещен карамельный попкорн
            FilterByHid(
                hid=3734930, glfilter='102:~500', split='only_big_popcorn'
            ),  # запрещены маленький и средний попкорн
            FilterByHid(hid=3734930, glfilter='103:1', split='only_salt_popcorn'),  # запрещен сладкий попкорн
            FilterByHid(
                hid=3734930, glfilter='101:1;103:0', split='no_weird_popcorn'
            ),  # запрещен попкорн с соленой карамелью
            FilterByMsku(msku=373493002, split='no_vanil'),  # запрещена определенная msku
            FilterByMsku(
                msku=373493003, price_to=1200, split='no_rich_svan_salt'
            ),  # запрещена определенная msku с ценой ниже 1200 (наша не проходит)
            FilterByMsku(
                msku=373493003, price_to=900, split='no_cheap_svan_salt'
            ),  # запрещена определенная msku с ценой ниже 900 (со скидкой 25% наша не проходит)
            FilterByMsku(
                msku=373493003, price_to=300, split='no_very_cheap_svan_salt'
            ),  # запрещена определенная msku с ценой ниже 900 (но наша проходит)
        ]

    def test_promo_by_cart_filters(self):
        """Проверяем как действуют разные ограничения на выдаваемые скидки"""

        # запрашиваем офферы из категории Попкорн с промоакцией cart-discount
        # в дефолтном сплите все 4 попкорна ничего не фильтруется
        response = self.report.request_json(
            'place=prime&hid=3734930&text=попкорн&rids=213&rgb=blue&cart=JKH6C4occ62gxyJ4u7wXsg&promo-type=cart-discount'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'titles': {'raw': 'Попкорн сладкий с карамелью мал'}},
                    {'titles': {'raw': 'Попкорн сладкий с ванилью средн'}},
                    {'titles': {'raw': 'Попкорн со сванской солью бол'}},
                    {'titles': {'raw': 'Попкорн соленая карамель средн'}},
                ]
            },
            allow_different_len=False,
            preserve_order=False,
        )

        # проверяем фильтрацию по gl-фильтрам
        response = self.report.request_json(
            'place=prime&hid=3734930&text=попкорн&rids=213&rgb=blue&cart=JKH6C4occ62gxyJ4u7wXsg&promo-type=cart-discount'
            '&rearr-factors=market_promo_by_user_cart_filter_split=no_caramel_popcorn'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    # под фильтр попали попкорны с карамелью (101:1)
                    # {'titles': {'raw': 'Попкорн сладкий с карамелью мал'}},
                    {'titles': {'raw': 'Попкорн сладкий с ванилью средн'}},
                    {'titles': {'raw': 'Попкорн со сванской солью бол'}},
                    # {'titles': {'raw': 'Попкорн соленая карамель средн'}},
                ]
            },
            allow_different_len=False,
            preserve_order=False,
        )

        response = self.report.request_json(
            'place=prime&hid=3734930&text=попкорн&rids=213&rgb=blue&cart=JKH6C4occ62gxyJ4u7wXsg&promo-type=cart-discount'
            '&rearr-factors=market_promo_by_user_cart_filter_split=only_big_popcorn'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    # под фильтр попали попкорны объемом до 500 мл (102:~500)
                    # {'titles': {'raw': 'Попкорн сладкий с карамелью мал'}},
                    # {'titles': {'raw': 'Попкорн сладкий с ванилью средн'}},
                    {'titles': {'raw': 'Попкорн со сванской солью бол'}},
                    # {'titles': {'raw': 'Попкорн соленая карамель средн'}},
                ]
            },
            allow_different_len=False,
            preserve_order=False,
        )

        response = self.report.request_json(
            'place=prime&hid=3734930&text=попкорн&rids=213&rgb=blue&cart=JKH6C4occ62gxyJ4u7wXsg&promo-type=cart-discount'
            '&rearr-factors=market_promo_by_user_cart_filter_split=only_salt_popcorn'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    # под фильтр попали все сладкие попкорны (103:1)
                    # {'titles': {'raw': 'Попкорн сладкий с карамелью мал'}},
                    # {'titles': {'raw': 'Попкорн сладкий с ванилью средн'}},
                    {'titles': {'raw': 'Попкорн со сванской солью бол'}},
                    {'titles': {'raw': 'Попкорн соленая карамель средн'}},
                ]
            },
            allow_different_len=False,
            preserve_order=False,
        )

        response = self.report.request_json(
            'place=prime&hid=3734930&text=попкорн&rids=213&rgb=blue&cart=JKH6C4occ62gxyJ4u7wXsg&promo-type=cart-discount'
            '&rearr-factors=market_promo_by_user_cart_filter_split=no_weird_popcorn'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'titles': {'raw': 'Попкорн сладкий с карамелью мал'}},
                    {'titles': {'raw': 'Попкорн сладкий с ванилью средн'}},
                    {'titles': {'raw': 'Попкорн со сванской солью бол'}},
                    # под фильтр попали соленые попкорны со вкусом карамели (101:1;103:0)
                    # {'titles': {'raw': 'Попкорн соленая карамель средн'}},
                ]
            },
            allow_different_len=False,
            preserve_order=False,
        )

        # проверяем фильтрацию по msku
        response = self.report.request_json(
            'place=prime&hid=3734930&text=попкорн&rids=213&rgb=blue&cart=JKH6C4occ62gxyJ4u7wXsg&promo-type=cart-discount'
            '&rearr-factors=market_promo_by_user_cart_filter_split=no_vanil'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'titles': {'raw': 'Попкорн сладкий с карамелью мал'}},
                    # конкретный попкорн с ванилью запрещен для акции cart-discount
                    # {'titles': {'raw': 'Попкорн сладкий с ванилью средн'}},
                    {'titles': {'raw': 'Попкорн соленая карамель средн'}},
                    {'titles': {'raw': 'Попкорн со сванской солью бол'}},
                ]
            },
            allow_different_len=False,
            preserve_order=False,
        )

        response = self.report.request_json(
            'place=prime&hid=3734930&text=попкорн&rids=213&rgb=blue&cart=JKH6C4occ62gxyJ4u7wXsg&promo-type=cart-discount'
            '&rearr-factors=market_promo_by_user_cart_filter_split=no_rich_svan_salt'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'titles': {'raw': 'Попкорн сладкий с карамелью мал'}},
                    {'titles': {'raw': 'Попкорн сладкий с ванилью средн'}},
                    # сванская соль стоит дешевле цены=1200 указанной в фильтре no_rich_svan_salt
                    # {'titles': {'raw': 'Попкорн со сванской солью бол'}},
                    {'titles': {'raw': 'Попкорн соленая карамель средн'}},
                ]
            },
            allow_different_len=False,
            preserve_order=False,
        )

        response = self.report.request_json(
            'place=prime&hid=3734930&text=попкорн&rids=213&rgb=blue&cart=JKH6C4occ62gxyJ4u7wXsg&promo-type=cart-discount'
            '&rearr-factors=market_promo_by_user_cart_filter_split=no_cheap_svan_salt'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'titles': {'raw': 'Попкорн сладкий с карамелью мал'}},
                    {'titles': {'raw': 'Попкорн сладкий с ванилью средн'}},
                    # сванская соль сама по себе дороже 900 но при применении скидки
                    # она стоит дешевле цены=900 указанной в фильтре no_cheap_svan_salt
                    # {'titles': {'raw': 'Попкорн со сванской солью бол'}},
                    {'titles': {'raw': 'Попкорн соленая карамель средн'}},
                ]
            },
            allow_different_len=False,
            preserve_order=False,
        )

        response = self.report.request_json(
            'place=prime&hid=3734930&text=попкорн&rids=213&rgb=blue&cart=JKH6C4occ62gxyJ4u7wXsg&promo-type=cart-discount'
            '&rearr-factors=market_promo_by_user_cart_filter_split=no_very_cheap_svan_salt'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'titles': {'raw': 'Попкорн сладкий с карамелью мал'}},
                    {'titles': {'raw': 'Попкорн сладкий с ванилью средн'}},
                    {
                        'titles': {'raw': 'Попкорн со сванской солью бол'}
                    },  # сванская соль при применении скидки стоит дороже цены=300
                    {'titles': {'raw': 'Попкорн соленая карамель средн'}},
                ]
            },
            allow_different_len=False,
            preserve_order=False,
        )

    def test_market_hide_discount(self):
        """Флаг market_hide_discount_on_basesearch скрывает скидки заложенные в индексе по oldprice"""

        # оффер в корзине BLUEModel1FEED1111QQQQ - велосипед из категории 16044621 со склада 145
        query = (
            'place=prime&text=замок+для+велосипеда&rids=213&hid=25728039&cart=BLUEModel1FEED1111QQQQ&'
            'rgb=blue&allow-collapsing=0'
        )

        zamok = "Замок для велосипеда"

        for rearr in [
            '&rearr-factors=market_hide_discount_on_basesearch=1.0',
            '&rearr-factors=market_hide_discount_on_output=1.0',
        ]:

            response = self.report.request_json(query)
            response_exp = self.report.request_json(query + rearr)

            # оффер имел скидку по oldPrice и еще дополнительно скидку по cart-discount
            # остается только скидка по cart-discount
            self.assertFragmentIn(response, T.ExpectedVelo25.offer_from_beru_with_discount(zamok))
            self.assertFragmentIn(
                response,
                {
                    "prices": {
                        "discount": {"oldMin": "1500", "percent": 35},
                        "value": "975",
                        "oldDiscount": {"oldMin": "1300", "percent": 13},
                    },
                    "promos": [{"type": "cart-discount"}],
                    "titles": {"raw": "Замок для велосипеда от Беру и со скидкой"},
                    "entity": "offer",
                },
            )
            self.assertFragmentIn(
                response_exp,
                {
                    "prices": {
                        "discount": {"oldMin": "1300", "percent": 25},
                        "value": "975",
                        "oldDiscount": NoKey("oldDiscount"),  # скидка от изначальной oldPrice=1500 исчезла
                    },
                    "promos": [{"type": "cart-discount"}],
                    "titles": {"raw": "Замок для велосипеда от Беру и со скидкой"},
                    "entity": "offer",
                },
            )

            # оффер содержал скидку только по cart-discount - и она остается на месте
            self.assertFragmentIn(response, T.ExpectedVelo25.offer_from_beru(zamok))
            self.assertFragmentIn(response_exp, T.ExpectedVelo25.offer_from_beru(zamok))

    @classmethod
    def prepare_special_skus(cls):

        cls.index.hypertree += [
            HyperCategory(hid=91444, name="Корм для кошек", output_type=HyperCategoryType.GURU),
            HyperCategory(hid=91445, name="Корм для собак", output_type=HyperCategoryType.GURU),
            HyperCategory(hid=91446, name="Корм для птиц", output_type=HyperCategoryType.GURU),
        ]

        # Корм для кошек дает скидку 10% на Корм для собак в пустом сплите и 15% в s1
        # На Корм для птиц скидки нет, но должна быть указана,
        #   чтобы сработала скидка на вендорский Корм для птиц
        cls.index.promos_by_cart += [
            PromoByCart(cart_hid=91444, promo_hid=91445, percent=10),
            PromoByCart(cart_hid=91444, promo_hid=91445, percent=12, split='vendor_s1'),
            PromoByCart(cart_hid=91444, promo_hid=91446, percent=0),
        ]

        cls.index.models += [
            Model(hid=91444, hyperid=1914441, title='Whiskas1'),
            Model(hid=91445, hyperid=1914451, title='Pedigree1'),
            Model(hid=91445, hyperid=1914452, title='Pedigree2'),
            Model(hid=91446, hyperid=1914461, title='Semechki1'),
            Model(hid=91446, hyperid=1914462, title='Semechki2'),
        ]

        cls.index.mskus += [
            MarketSku(
                title='Whiskas1 24шт. x 85 г',
                hyperid=1914441,
                sku=19144412485,
                hid=91444,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(price=400, feedid=1111, offerid="whiskas1_24855", waremd5="BLUEModel1xxxxwsks2485")
                ],
            ),
            MarketSku(
                title='Pedigree1 5кг',
                hyperid=1914451,
                sku=19144515,
                hid=91445,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        ts=111, price=1000, feedid=1111, offerid="pedigree1_5kg", waremd5="BLUEModel1xxxxpdgr5000"
                    )
                ],
            ),
            MarketSku(
                title='Pedigree1 7кг',
                hyperid=1914451,
                sku=19144517,
                hid=91445,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        ts=222, price=1500, feedid=1111, offerid="pedigree1_7kg", waremd5="BLUEModel1xxxxpdgr7000"
                    )
                ],
            ),
            MarketSku(
                title='Pedigree2 5кг',
                hyperid=1914452,
                sku=19144525,
                hid=91445,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        ts=333, price=1000, feedid=1111, offerid="pedigree2_5kg", waremd5="BLUEModel2xxxxpdgr5000"
                    )
                ],
            ),
            MarketSku(
                title='Pedigree2 7кг',
                hyperid=1914452,
                sku=19144527,
                hid=91445,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        ts=444, price=1500, feedid=1111, offerid="pedigree2_7kg", waremd5="BLUEModel2xxxxpdgr7000"
                    )
                ],
            ),
            MarketSku(
                title='Semechki1 1кг',
                hyperid=1914461,
                sku=19144611,
                hid=91446,
                delivery_buckets=[1234],
                blue_offers=[BlueOffer(price=700, feedid=1111, offerid="semechki1_1kg")],
            ),
            MarketSku(
                title='Semechki2 1кг',
                hyperid=1914462,
                sku=19144621,
                hid=91446,
                delivery_buckets=[1234],
                blue_offers=[BlueOffer(price=600, feedid=1111, offerid="semechki2_1kg")],
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 111).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 222).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 333).respond(0.7)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 444).respond(0.6)

        # скидки от вендоров
        cls.index.promos_by_cart += [
            PromoByCartSku(msku=19144517, percent=15),
            PromoByCartSku(msku=19144527, percent=22, split='vendor_s1'),
            PromoByCartSku(msku=19144611, percent=25),
        ]

        cls.dj.on_request(exp=DEFAULT_DJ_EXP, yandexuid='24846005').respond(
            [
                DjModel(id='1914451'),
                DjModel(id='1914452'),
                DjModel(id='1914461'),
                DjModel(id='1914462'),
            ]
        )

    def test_special_skus_promo_cart(self):
        """
        Тестируем спциальные скидки от вендоров на конкретные msku в place=promo_cart_discount
        """

        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&rgb=blue' '&cart=BLUEModel1xxxxwsks2485&yandexuid=24846005'
        )

        promo = {
            "type": "cart-discount",
            "key": "vendor_19144517_15perc_promo_10percent_in_hid91445_for_1p_if_hid91444_in_cart",
            "description": Contains("Скидка 15%"),
            "cartDiscount": {"discount": {"percent": 15}},
            "onlyPromoCategory": {"hid": 91445},
        }
        self.assertFragmentIn(
            response, T.Expected.offer_with_simple_discount("Pedigree1 7кг", 1275, 1500, 15, promo, sku=19144517)
        )

        promo = {
            "type": "cart-discount",
            "key": "promo_10percent_in_hid91445_for_1p_if_hid91444_in_cart",
            "description": Contains("Скидка 10%"),
            "cartDiscount": {"discount": {"percent": 10}},
            "onlyPromoCategory": {"hid": 91445},
        }
        self.assertFragmentIn(
            response, T.Expected.offer_with_simple_discount("Pedigree2 5кг", 900, 1000, 10, promo, sku=19144525)
        )

        promo = {
            "type": "cart-discount",
            "key": "vendor_19144611_25perc_promo_0percent_in_hid91446_for_1p_if_hid91444_in_cart",
            "description": "Скидка 25%",
            "cartDiscount": {"discount": {"percent": 25}},
            "onlyPromoCategory": {"hid": 91446},
        }
        self.assertFragmentIn(
            response, T.Expected.offer_with_simple_discount("Semechki1 1кг", 525, 700, 25, promo, sku=19144611)
        )

        # на этот товар скидка 0%
        self.assertFragmentNotIn(response, {"titles": {"raw": "Semechki2 1кг"}})

    def test_special_skus_promo_cart_split(self):
        """
        Тестируем спциальные скидки от вендоров на конкретные msku в place=promo_cart_discount под сплитом
        """
        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&rgb=blue'
            '&cart=BLUEModel1xxxxwsks2485&yandexuid=24846005'
            '&rearr-factors=market_promo_by_user_cart_split=vendor_s1'
        )
        promo = {
            "type": "cart-discount",
            "key": "promo_12percent_in_hid91445_for_1p_if_hid91444_in_cart_split_vendor_s1",
            "description": Contains("Скидка 12%"),
            "cartDiscount": {"discount": {"percent": 12}},
            "onlyPromoCategory": {"hid": 91445},
        }
        self.assertFragmentIn(
            response, T.Expected.offer_with_simple_discount("Pedigree1 5кг", 880, 1000, 12, promo, sku=19144515)
        )

        promo = {
            "type": "cart-discount",
            "key": "vendor_19144527_22perc_promo_12percent_in_hid91445_for_1p_if_hid91444_in_cart_split_vendor_s1",
            "description": "Скидка 22%",
            "cartDiscount": {"discount": {"percent": 22}},
            "onlyPromoCategory": {"hid": 91445},
        }
        self.assertFragmentIn(
            response, T.Expected.offer_with_simple_discount("Pedigree2 7кг", 1170, 1500, 22, promo, sku=19144527)
        )

    # TODO(vdimir@) test_special_skus_forced

    def test_special_skus_promo_cart_zero_discount(self):
        """
        Скидка 0% не помечается как скидка, но работает для вендорских товаров
        """
        response = self.report.request_json(
            'place=prime&rids=213&rgb=blue&cvredirect=0&how=random'
            '&cart=BLUEModel1xxxxwsks2485&hyperid=1914461&hyperid=1914462'
        )
        promo = {
            "type": "cart-discount",
            "key": "vendor_19144611_25perc_promo_0percent_in_hid91446_for_1p_if_hid91444_in_cart",
            "onlyPromoCategory": {"hid": 91446},
        }
        self.assertFragmentIn(
            response, T.Expected.offer_with_simple_discount("Semechki1 1кг", 525, 700, 25, promo, sku=19144611)
        )

        self.assertFragmentIn(response, T.Expected.offer_without_discount("Semechki2 1кг", 600, None, sku=19144621))

    @classmethod
    def prepare_replacing_pricedrop_by_accessories(cls):
        # утверждения X - сопутка (или акс) к Y могут не соответствовать реальности для указанных ниже категорий,
        # что не мешает считать эти утверждения верными для целей тестирования
        cls.index.hypertree += [
            HyperCategory(
                hid=14296140, name="Коврики для занятий йогой и фитнесом", output_type=HyperCategoryType.GURU
            ),
            # совместно покупаемые категории
            HyperCategory(hid=14288720, name="Гантели для занятий спортом", output_type=HyperCategoryType.GURU),
            HyperCategory(hid=90669, name="Полотенца", output_type=HyperCategoryType.GURU),
            HyperCategory(hid=91491, name="Мобильные телефоны", output_type=HyperCategoryType.GURU),
            # аксессуары к мобилкам
            HyperCategory(hid=91498, name="Чехлы для мобильных телефонов", output_type=HyperCategoryType.GURU),
            # сопутка для мобилок
            HyperCategory(
                hid=91503,
                name="Зарядные устройства и адаптеры для мобильных телефонов",
                output_type=HyperCategoryType.GURU,
            ),
            HyperCategory(hid=10498025, name="Умные часы и браслеты", output_type=HyperCategoryType.GURU),
            # аксессуары к браслетам
            HyperCategory(hid=13939151, name="Ремешки для умных часов", output_type=HyperCategoryType.GURU),
            HyperCategory(hid=90560, name="Портативные цифровые плееры", output_type=HyperCategoryType.GURU),
            # сопутка для мобилок и браслетов, аксы к плеерам
            HyperCategory(hid=90555, name="Наушники и Bluetooth-гарнитуры", output_type=HyperCategoryType.GURU),
        ]

        cls.index.promos_by_cart += [
            PromoByCart(cart_hid=14296140, promo_hid=90669, percent=3),
        ]

        cls.index.shops += [
            Shop(
                fesh=8888,
                datafeed_id=8888,
                priority_region=213,
                regions=[225],
                name="НеБеру!",
                supplier_type=Shop.FIRST_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
            ),
        ]

        description = "Нынче уже не Беру..."

        def create_offers(hid, title, hyperid):
            cls.index.models += [Model(hid=hid, hyperid=hyperid, title=title)]
            cls.index.mskus += [
                MarketSku(
                    title=title + " от Беру",
                    descr=description,
                    hyperid=hyperid,
                    sku=hyperid * 10 + 2,
                    hid=hid,
                    delivery_buckets=[1234],
                    blue_offers=[
                        BlueOffer(
                            price=1400,
                            feedid=8888,
                            offerid="beru_model{}_e".format(hyperid),
                            waremd5="BLUEModel{}FEED8888eeee".format(hyperid)[0:22],
                            randx=(hyperid * 10 + 2) * 2,
                        )
                    ],
                ),
            ]

        create_offers(hid=14296140, title="Коврик", hyperid=21)
        create_offers(hid=14288720, title="Гантели", hyperid=22)
        create_offers(hid=90669, title="Полотенце", hyperid=23)
        create_offers(hid=91491, title="Смартфон Meizu", hyperid=24)
        create_offers(hid=91491, title="Смартфон Xiaomi", hyperid=25)
        # create_offers(hid=91498, title="Чехол для Meizu", hyperid=26)  # эту модель поиск должен не найти
        create_offers(hid=91498, title="Чехол для Xiaomi", hyperid=27)
        create_offers(hid=91503, title="Зарядник", hyperid=28)
        create_offers(hid=10498025, title="Браслет", hyperid=29)
        create_offers(hid=13939151, title="Ремешок для браслета", hyperid=30)
        create_offers(hid=90560, title="Плеер", hyperid=31)
        create_offers(hid=90555, title="Наушники", hyperid=32)

        cls.dj.on_request(exp=DEFAULT_DJ_EXP, yandexuid='200429040').respond(
            [
                DjModel(id='22', attributes={'RecomOutputType': 'accessoriesBlock'}),
                DjModel(id='23', attributes={'RecomOutputType': 'accessoriesBlock'}),
            ]
        )

        cls.dj.on_request(exp=DEFAULT_DJ_EXP, yandexuid='200429041').respond(
            [
                DjModel(id='26', attributes={'RecomOutputType': 'accessoriesBlock', 'accessoryGroups': '0'}),
                DjModel(id='32', attributes={'RecomOutputType': 'accessoriesBlock', 'jointPurchaseGroups': '0'}),
            ]
        )

        cls.dj.on_request(exp=DEFAULT_DJ_EXP, yandexuid='200429042').respond(
            [
                DjModel(id='27', attributes={'RecomOutputType': 'accessoriesBlock', 'accessoryGroups': '0'}),
                DjModel(id='28', attributes={'RecomOutputType': 'accessoriesBlock', 'jointPurchaseGroups': '0'}),
            ]
        )

        cls.dj.on_request(exp=DEFAULT_DJ_EXP, yandexuid='200429043').respond(
            [
                DjModel(id='30', attributes={'RecomOutputType': 'accessoriesBlock', 'accessoryGroups': '0'}),
                DjModel(id='32', attributes={'RecomOutputType': 'accessoriesBlock', 'jointPurchaseGroups': '0'}),
            ]
        )

        cls.dj.on_request(exp=DEFAULT_DJ_EXP, yandexuid='200429044').respond(
            [DjModel(id='32', attributes={'RecomOutputType': 'accessoriesBlock'})]
        )

        cls.dj.on_request(exp=DEFAULT_DJ_EXP, yandexuid='200429045').respond(
            [
                DjModel(id='26', attributes={'RecomOutputType': 'accessoriesBlock', 'accessoryGroups': '0'}),
                DjModel(id='30', attributes={'RecomOutputType': 'accessoriesBlock', 'accessoryGroups': '1'}),
                DjModel(id='32', attributes={'RecomOutputType': 'accessoriesBlock', 'jointPurchaseGroups': '0,1'}),
            ]
        )

        cls.dj.on_request(exp=DEFAULT_DJ_EXP, yandexuid='200429046').respond(
            [
                DjModel(id='26', attributes={'RecomOutputType': 'accessoriesBlock', 'accessoryGroups': '0'}),
                DjModel(id='27', attributes={'RecomOutputType': 'accessoriesBlock', 'accessoryGroups': '1'}),
                DjModel(id='28', attributes={'RecomOutputType': 'accessoriesBlock', 'jointPurchaseGroups': '1'}),
                DjModel(id='32', attributes={'RecomOutputType': 'accessoriesBlock', 'jointPurchaseGroups': '0'}),
            ]
        )

        cls.dj.on_request(exp=DEFAULT_DJ_EXP, yandexuid='200429047').respond(
            [
                DjModel(id='26', attributes={'RecomOutputType': 'accessoriesBlock'}),
                # наушники - аксессуар, поэтому их показ не зависит от показа других моделей
                DjModel(id='32', attributes={'RecomOutputType': 'accessoriesBlock'}),
            ]
        )

        cls.dj.on_request(exp=DEFAULT_DJ_EXP, yandexuid='200429048').respond(
            [
                DjModel(id='26', attributes={'RecomOutputType': 'accessoriesBlock', 'accessoryGroups': '0'}),
                DjModel(id='22', attributes={'RecomOutputType': 'accessoriesBlock'}),
                DjModel(id='23', attributes={'RecomOutputType': 'accessoriesBlock'}),
                DjModel(id='32', attributes={'RecomOutputType': 'accessoriesBlock', 'jointPurchaseGroups': '0'}),
            ]
        )

        cls.dj.on_request(exp=DEFAULT_DJ_EXP, yandexuid='200429049').respond(
            [
                DjModel(id='26', attributes={'RecomOutputType': 'accessoriesBlock'}),
                DjModel(id='27', attributes={'RecomOutputType': 'accessoriesBlock', 'accessoryGroups': '0'}),
                DjModel(id='30', attributes={'RecomOutputType': 'accessoriesBlock'}),
                DjModel(id='32', attributes={'RecomOutputType': 'accessoriesBlock'}),
                DjModel(id='28', attributes={'RecomOutputType': 'accessoriesBlock', 'jointPurchaseGroups': '0'}),
                DjModel(id='22', attributes={'RecomOutputType': 'accessoriesBlock'}),
                DjModel(id='23', attributes={'RecomOutputType': 'accessoriesBlock'}),
            ]
        )

    def test_replacing_pricedrop_by_accessories(self):
        common_query = (
            'place=promo_cart_discount&rids=213&rgb=blue&client=ANDROID&debug=da'
            + '&rearr-factors=market_promo_cart_clients_permitted_accessories_replacement=ANDROID;'
            + 'market_dj_exp_for_promo_cart_force=1&yandexuid={yandexuid}&cart={cart}'
        )

        response = self.report.request_json(
            common_query.format(yandexuid='200429040', cart='BLUEModel21FEED8888eee')  # коврик
        )

        # Прайсдропных моделей мало, вернулся аксессуарный блок, в котором 2 сопутки
        # к модели из не имеющей аксов категории
        self.assertFragmentIn(
            response,
            {
                'recomOutputType': 'accessories',
                'results': [
                    {'entity': 'product', 'id': 22},
                    {'entity': 'product', 'id': 23},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Имеющуюся скидку по прайсдропу не скрываем
        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'id': 23,
                'offers': {'items': [{'entity': 'offer', 'promos': [{'type': 'cart-discount'}]}]},
            },
        )

        # Но и не даём пд-скидку на не прайсдропные модели
        self.assertFragmentNotIn(
            response,
            {
                'entity': 'product',
                'id': 22,
                'offers': {'items': [{'entity': 'offer', 'promos': [{'type': 'cart-discount'}]}]},
            },
        )

        response = self.report.request_json(
            common_query.format(yandexuid='200429041', cart='BLUEModel24FEED8888eee')  # смартфон Meizu
        )

        # Аксессуара-чехла (26) нет, поэтому наушники-сопутка (32) не показываются
        self.assertFragmentIn(
            response, {'recomOutputType': Absent(), 'results': []}, preserve_order=True, allow_different_len=False
        )

        response = self.report.request_json(
            common_query.format(yandexuid='200429042', cart='BLUEModel25FEED8888eee')  # смартфон Xiaomi
        )

        # Аксессуар-чехол (27) есть, поэтому зарядник-сопутка (28) показывается
        self.assertFragmentIn(
            response,
            {
                'recomOutputType': 'accessories',
                'results': [
                    {'entity': 'product', 'id': 27},
                    {'entity': 'product', 'id': 28},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(
            common_query.format(yandexuid='200429043', cart='BLUEModel29FEED8888eee')  # Браслет
        )

        # Аксессуар-ремешок (30) есть, поэтому наушники-сопутка (32) показываются
        self.assertFragmentIn(
            response,
            {
                'recomOutputType': 'accessories',
                'results': [
                    {'entity': 'product', 'id': 30},
                    {'entity': 'product', 'id': 32},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(
            common_query.format(yandexuid='200429044', cart='BLUEModel29FEED8888eee')  # Плеер
        )

        # Аксессуар-наушники (32) в наличии
        self.assertFragmentIn(
            response,
            {
                'recomOutputType': 'accessories',
                'results': [
                    {'entity': 'product', 'id': 32},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(
            common_query.format(
                yandexuid='200429045', cart='BLUEModel24FEED8888eee,BLUEModel29FEED8888eee'  # смартфон Meizu и браслет
            )
        )

        # Аксессуара-чехла (26) нет, но есть ремешок (30), поэтому наушники-сопутка (32) показываются
        self.assertFragmentIn(
            response,
            {
                'recomOutputType': 'accessories',
                'results': [
                    {'entity': 'product', 'id': 30},
                    {'entity': 'product', 'id': 32},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(
            common_query.format(
                yandexuid='200429046', cart='BLUEModel24FEED8888eee,BLUEModel25FEED8888eee'  # смартфоны Meizu и Xiaomi
            )
        )

        # К одному телефону нет чехла, к другому - есть, поэтому наушники-сопутка (32) не покажутся,
        # а зарядник (28) - покажется
        self.assertFragmentIn(
            response,
            {
                'recomOutputType': 'accessories',
                'results': [
                    {'entity': 'product', 'id': 27},
                    {'entity': 'product', 'id': 28},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(
            common_query.format(
                yandexuid='200429047', cart='BLUEModel24FEED8888eee,BLUEModel31FEED8888eee'  # смартфон Meizu и плеер
            )
        )

        # К телефону чехла нет, но наушники - акс для плеера, поэтому они покажутся
        self.assertFragmentIn(
            response,
            {
                'recomOutputType': 'accessories',
                'results': [
                    {'entity': 'product', 'id': 32},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(
            common_query.format(
                yandexuid='200429048', cart='BLUEModel21FEED8888eee,BLUEModel24FEED8888eee'  # коврик и смартфон Meizu
            )
        )

        # К телефону чехла нет, поэтому наушники не покажем. К коврику есть сопутка - она и покажется
        self.assertFragmentIn(
            response,
            {
                'recomOutputType': 'accessories',
                'results': [
                    {'entity': 'product', 'id': 22},
                    {'entity': 'product', 'id': 23},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(
            common_query.format(
                yandexuid='200429049',
                # коврик, смартфоны, браслет и плеер
                cart='BLUEModel21FEED8888eee,BLUEModel24FEED8888eee,BLUEModel25FEED8888eee,BLUEModel29FEED8888eee,BLUEModel31FEED8888eee',
            )
        )

        # К телефону чехла нет, но у нас есть куча других причин показать наушники
        self.assertFragmentIn(
            response,
            {
                'recomOutputType': 'accessories',
                'results': [
                    {'entity': 'product', 'id': 27},
                    {'entity': 'product', 'id': 30},
                    {'entity': 'product', 'id': 32},
                    {'entity': 'product', 'id': 28},
                    {'entity': 'product', 'id': 22},
                    {'entity': 'product', 'id': 23},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_replacing_pricedrop_by_accessories_generic_bundle(cls):
        cls.settings.loyalty_enabled = True
        cls.index.hypertree += [
            HyperCategory(
                hid=142961409, name="Коврики для занятий йогой и фитнесом", output_type=HyperCategoryType.GURU
            ),
            # совместно покупаемые категории
            HyperCategory(hid=142887209, name="Гантели для занятий спортом", output_type=HyperCategoryType.GURU),
            HyperCategory(hid=906699, name="Полотенца", output_type=HyperCategoryType.GURU),
        ]

        cls.index.promos_by_cart += [
            PromoByCart(cart_hid=142961409, promo_hid=906699, percent=3),
        ]

        cls.index.shops += [
            Shop(
                fesh=88888,
                datafeed_id=88888,
                priority_region=213,
                regions=[225],
                name="НеБеру!",
                supplier_type=Shop.FIRST_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
            ),
        ]

        description = "Нынче уже не Беру..."

        def create_offers(hid, title, hyperid):
            cls.index.models += [Model(hid=hid, hyperid=hyperid, title=title)]
            blue_offer = BlueOffer(
                price=1400,
                feedid=88888,
                offerid="beru_model{}_e".format(hyperid),
                waremd5="BLUEModel{}FEED88888aaa".format(hyperid)[0:22],
                randx=(hyperid * 10 + 2) * 2,
            )
            cls.index.mskus += [
                MarketSku(
                    title=title + " от Беру",
                    descr=description,
                    hyperid=hyperid,
                    sku=hyperid * 10 + 2,
                    hid=hid,
                    delivery_buckets=[1234],
                    blue_offers=[blue_offer],
                ),
            ]
            return blue_offer

        create_offers(hid=142961409, title="Коврик", hyperid=219)
        offer2 = create_offers(hid=142887209, title="Гантели", hyperid=229)
        offer3 = create_offers(hid=906699, title="Полотенце", hyperid=239)

        # действующая акция
        promo1 = Promo(
            promo_type=PromoType.GENERIC_BUNDLE,
            feed_id=88888,
            key='JVvklxUgdnawSJPG4UhZ-1',
            url='http://localhost.ru/',
            generic_bundles_content=[
                make_generic_bundle_content(offer2.offerid, offer3.offerid, 1),
            ],
            offers_matching_rules=[
                OffersMatchingRules(
                    feed_offer_ids=[
                        [88888, offer2.offerid],
                    ]
                ),
            ],
        )

        offer2.promo = [promo1]

        cls.index.promos += [
            promo1,
        ]
        cls.dynamic.loyalty += [
            DynamicBlueGenericBundlesPromos(
                whitelist=[
                    promo1.key,
                ]
            )
        ]

        cls.dj.on_request(exp=DEFAULT_DJ_EXP, yandexuid='300429040').respond(
            [
                DjModel(id='229', attributes={'RecomOutputType': 'accessoriesBlock'}),
                DjModel(id='239', attributes={'RecomOutputType': 'accessoriesBlock'}),
            ]
        )

    def test_promo_generic_bundle_for_promo_cart_discount(self):
        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&client=ANDROID&debug=da&cpa=real&rgb=blue&'
            '&rearr-factors=market_promo_cart_clients_permitted_accessories_replacement=ANDROID;'
            'market_dj_exp_for_promo_cart_force=1&yandexuid=300429040&cart=BLUEModel219FEED88888a'
        )

        self.assertFragmentIn(
            response,
            {
                'search': {
                    'recomOutputType': 'accessories',
                    'results': [
                        {
                            'entity': 'product',
                            'id': 229,
                            'offers': {
                                'items': [
                                    {
                                        'entity': 'offer',
                                        'wareId': 'BLUEModel229FEED88888Q',
                                        'promos': [{'type': 'generic-bundle', 'key': 'JVvklxUgdnawSJPG4UhZ-1'}],
                                    }
                                ]
                            },
                        },
                        {
                            'entity': 'product',
                            'id': 239,
                            'offers': {
                                'items': [
                                    {
                                        'entity': 'offer',
                                        'wareId': 'BLUEModel239FEED88888Q',
                                        'promos': [
                                            {'type': 'generic-bundle-secondary', 'key': 'JVvklxUgdnawSJPG4UhZ-1'}
                                        ],
                                    }
                                ]
                            },
                        },
                    ],
                },
                'offers': [
                    {
                        'entity': 'offer',
                        'wareId': 'BLUEModel239FEED88888Q',
                    }
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_paging_with_accessories_replacement(cls):
        cls.settings.memcache_enabled = True

        cls.dj.on_request(exp=DEFAULT_DJ_EXP, yandexuid='290420040').respond(
            [
                DjModel(id='8'),  # туалетка
                DjModel(id='22', attributes={'RecomOutputType': 'accessoriesBlock'}),  # гантели
            ]
        )
        cls.dj.on_request(exp=DEFAULT_DJ_EXP, yandexuid='290420040', shown_model_ids='8').respond(
            [
                DjModel(id='5'),  # фейри
                DjModel(id='22', attributes={'RecomOutputType': 'accessoriesBlock'}),  # гантели
            ]
        )

        cls.dj.on_request(exp=DEFAULT_DJ_EXP, yandexuid='290420041').respond([])

        cls.dj.on_request(exp=DEFAULT_DJ_EXP, yandexuid='290420042').respond(
            [
                DjModel(id='27', attributes={'RecomOutputType': 'accessoriesBlock'}),  # чехол для Xiaomi
                DjModel(id='22', attributes={'RecomOutputType': 'accessoriesBlock'}),  # гантели
            ]
        )
        cls.dj.on_request(exp=DEFAULT_DJ_EXP, yandexuid='290420042', shown_model_ids='27,22').respond(
            [
                DjModel(id='23', attributes={'RecomOutputType': 'accessoriesBlock'}),  # полотенце
                # зарядник (28) не будет предложен dj, т.к. его показ зависит от показа на предыдущей странице
                # чехла для телефона (27)
            ]
        )

        cls.dj.on_request(exp=DEFAULT_DJ_EXP, yandexuid='290420043').respond(
            [
                DjModel(id='27', attributes={'RecomOutputType': 'accessoriesBlock'}),  # чехол для Xiaomi
                DjModel(id='22', attributes={'RecomOutputType': 'accessoriesBlock'}),  # гантели
            ]
        )
        cls.dj.on_request(exp=DEFAULT_DJ_EXP, yandexuid='290420043', shown_model_ids='27,22').respond(
            [
                DjModel(id='23', attributes={'RecomOutputType': 'accessoriesBlock'}),  # полотенце
                DjModel(id='8'),  # вдруг на 2й странице появился пд
                # зарядник (28) не будет предложен dj, т.к. его показ зависит от показа на предыдущей странице
                # чехла для телефона (27)
            ]
        )

        cls.dj.on_request(exp=DEFAULT_DJ_EXP, yandexuid='290420044').respond(
            [
                DjModel(id='27', attributes={'RecomOutputType': 'accessoriesBlock'}),  # чехол для Xiaomi
                DjModel(id='22', attributes={'RecomOutputType': 'accessoriesBlock'}),  # гантели
            ]
        )
        cls.dj.on_request(exp=DEFAULT_DJ_EXP, yandexuid='290420044', shown_model_ids='27,22').respond(
            [
                DjModel(id='23'),  # dj считает эту модель подходящей для прайсдропа, но не подходящей для блока аксов
                # зарядник (28) не будет предложен dj, т.к. его показ зависит от показа на предыдущей странице
                # чехла для телефона (27)
            ]
        )

        cls.dj.on_request(exp=DEFAULT_DJ_EXP, yandexuid='290420046').respond(
            [
                DjModel(id='8'),  # туалетка
                DjModel(id='22', attributes={'RecomOutputType': 'accessoriesBlock'}),  # гантели
            ]
        )
        cls.dj.on_request(exp=DEFAULT_DJ_EXP, yandexuid='290420046', shown_model_ids='8').respond(
            [
                DjModel(id='22', attributes={'RecomOutputType': 'accessoriesBlock'}),  # гантели
            ]
        )

    def test_paging_with_accessories_replacement(self):
        common_query = (
            'place=promo_cart_discount&rids=213&rgb=blue&client=ANDROID&debug=da'
            + '&rearr-factors=market_promo_cart_clients_permitted_accessories_replacement=ANDROID;'
            + 'market_dj_exp_for_promo_cart_force=1;dj_paging_promo_cart_discount=1'
            + '&yandexuid={yandexuid}&page={page}&cart={cart}&view-unique-id={view_unique_id}'
        )

        # 1 страница - прайсдроп, 2 страница - прайсдроп. В обоих случаях аксы безуспешно пытаются подмешаться
        response = self.report.request_json(
            common_query.format(
                yandexuid='290420040',
                cart='BLUEModel4FEED2222dddQ,BLUEModel21FEED8888eee',
                page='1',
                view_unique_id=1418,
            )
        )

        self.assertFragmentIn(
            response,
            {
                'recomOutputType': 'pricedrop',
                'results': [
                    {'entity': 'product', 'id': 8},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(
            common_query.format(
                yandexuid='290420040',
                cart='BLUEModel4FEED2222dddQ,BLUEModel21FEED8888eee',
                page='2',
                view_unique_id=1418,
            )
        )

        self.assertFragmentIn(
            response,
            {
                'recomOutputType': 'pricedrop',
                'results': [
                    {'entity': 'product', 'id': 5},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # 2 страница - нет ни пд ни аксов
        response = self.report.request_json(
            common_query.format(
                yandexuid='290420041',
                cart='BLUEModel4FEED2222dddQ,BLUEModel21FEED8888eee',
                page='1',
                view_unique_id=1419,
            )
        )

        self.assertFragmentIn(
            response, {'recomOutputType': Absent(), 'results': []}, preserve_order=True, allow_different_len=False
        )

        # 1 страница - аксы, 2 страница - аксы. Dj не прислал пд-моделей, но на 2й странице у модели есть такая скидка
        response = self.report.request_json(
            common_query.format(
                yandexuid='290420042',
                cart='BLUEModel21FEED8888eee,BLUEModel25FEED8888eee',
                page='1',
                view_unique_id=1420,
            )
        )

        self.assertFragmentIn(
            response,
            {
                'recomOutputType': 'accessories',
                'results': [
                    {'entity': 'product', 'id': 27},
                    {'entity': 'product', 'id': 22},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(
            common_query.format(
                yandexuid='290420042',
                cart='BLUEModel21FEED8888eee,BLUEModel25FEED8888eee',
                page='2',
                view_unique_id=1420,
            )
        )

        self.assertFragmentIn(
            response,
            {
                'recomOutputType': 'accessories',
                'results': [
                    {'entity': 'product', 'id': 23},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )
        # Имеющуюся скидку по прайсдропу не скрываем
        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'id': 23,
                'offers': {'items': [{'entity': 'offer', 'promos': [{'type': 'cart-discount'}]}]},
            },
        )

        # 1 страница - аксы, 2 страница - аксы. Dj прислал пд на 2й странице, но тип выдачи не меняется,
        # даже если у модели есть пд-скидка
        response = self.report.request_json(
            common_query.format(
                yandexuid='290420043',
                cart='BLUEModel21FEED8888eee,BLUEModel25FEED8888eee',
                page='1',
                view_unique_id=1421,
            )
        )

        self.assertFragmentIn(
            response,
            {
                'recomOutputType': 'accessories',
                'results': [
                    {'entity': 'product', 'id': 27},
                    {'entity': 'product', 'id': 22},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(
            common_query.format(
                yandexuid='290420043',
                cart='BLUEModel21FEED8888eee,BLUEModel25FEED8888eee',
                page='2',
                view_unique_id=1421,
            )
        )

        self.assertFragmentIn(
            response,
            {
                'recomOutputType': 'accessories',
                'results': [
                    {'entity': 'product', 'id': 23},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )
        # Имеющуюся скидку по прайсдропу не скрываем
        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'id': 23,
                'offers': {'items': [{'entity': 'offer', 'promos': [{'type': 'cart-discount'}]}]},
            },
        )
        self.assertFragmentNotIn(response, {'results': [{'entity': 'product', 'id': 8}]})

        # 1 страница - аксы, 2 страница - только прайсдроп. 2ю страницу скрываем, т.к. нельзя менять тип выдачи
        response = self.report.request_json(
            common_query.format(
                yandexuid='290420044',
                cart='BLUEModel21FEED8888eee,BLUEModel25FEED8888eee',
                page='1',
                view_unique_id=1422,
            )
        )

        self.assertFragmentIn(
            response,
            {
                'recomOutputType': 'accessories',
                'results': [
                    {'entity': 'product', 'id': 27},
                    {'entity': 'product', 'id': 22},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(
            common_query.format(
                yandexuid='290420044',
                cart='BLUEModel21FEED8888eee,BLUEModel25FEED8888eee',
                page='2',
                view_unique_id=1422,
            )
        )

        self.assertFragmentIn(
            response, {'recomOutputType': Absent(), 'results': []}, preserve_order=True, allow_different_len=False
        )

        # 1 страница - прайсдроп, 2 страница - аксы. В обоих случаях аксы показаться не должны
        response = self.report.request_json(
            common_query.format(
                yandexuid='290420046',
                cart='BLUEModel4FEED2222dddQ,BLUEModel21FEED8888eee',
                page='1',
                view_unique_id=1426,
            )
        )

        self.assertFragmentIn(
            response,
            {
                'recomOutputType': 'pricedrop',
                'results': [
                    {'entity': 'product', 'id': 8},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(
            common_query.format(
                yandexuid='290420046',
                cart='BLUEModel4FEED2222dddQ,BLUEModel21FEED8888eee',
                page='2',
                view_unique_id=1426,
            )
        )

        self.assertFragmentIn(
            response, {'recomOutputType': Absent(), 'results': []}, preserve_order=True, allow_different_len=False
        )

    @classmethod
    def prepare_more_accessories_than_requested(cls):
        cls.dj.on_request(exp=DEFAULT_DJ_EXP, yandexuid='290420045', max_count=1).respond(
            [
                DjModel(id='27', attributes={'RecomOutputType': 'accessoriesBlock'}),
                DjModel(id='22', attributes={'RecomOutputType': 'accessoriesBlock'}),
            ]
        )

    def test_more_accessories_than_requested(self):
        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&rgb=blue&client=ANDROID&debug=da'
            + '&yandexuid=290420045&cart=BLUEModel21FEED8888eee,BLUEModel25FEED8888eee&page=1&view-unique-id=1423'
            + '&rearr-factors=market_promo_cart_clients_permitted_accessories_replacement=ANDROID;'
            + 'market_dj_exp_for_promo_cart_force=1;dj_paging_promo_cart_discount=1;market_promo_cart_discount_max_from_dj=1'
        )

        # Флаг market_promo_cart_discount_max_from_dj пока не влияет на размер аксессуарной выдачи репорта
        # Однако dj учитывает этот параметр при формировании выдачи
        self.assertFragmentIn(
            response,
            {
                'recomOutputType': 'accessories',
                'results': [
                    {'entity': 'product', 'id': 27},
                    {'entity': 'product', 'id': 22},  # выдача не обрезалась
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_applying_min_count_to_show(self):
        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&rgb=blue&client=ANDROID&debug=da'
            + '&yandexuid=290420045&cart=BLUEModel21FEED8888eee,BLUEModel25FEED8888eee&page=1&view-unique-id=1424'
            + '&min-count-to-show=3&rearr-factors=market_promo_cart_clients_permitted_accessories_replacement=ANDROID;'
            + 'market_dj_exp_for_promo_cart_force=1;dj_paging_promo_cart_discount=1;market_promo_cart_discount_max_from_dj=1'
        )

        # пустая выдача, т.к. нашлось всего 2 аксессуара/сопутки
        self.assertFragmentIn(
            response, {'recomOutputType': Absent(), 'results': []}, preserve_order=True, allow_different_len=False
        )

        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&rgb=blue&client=ANDROID&debug=da&min-count-to-show=2'
            + '&rearr-factors=market_promo_cart_clients_permitted_accessories_replacement=ANDROID;'
            + 'market_dj_exp_for_promo_cart_force=1;dj_paging_promo_cart_discount=1'
            + '&yandexuid=290420040&cart=BLUEModel4FEED2222dddQ,BLUEModel21FEED8888eee&page=1&view-unique-id=1425'
        )

        # пустая выдача, т.к. нашлась всего 1 пд-модель
        self.assertFragmentIn(
            response, {'recomOutputType': Absent(), 'results': []}, preserve_order=True, allow_different_len=False
        )

    @classmethod
    def prepare_paging_with_min_count_to_show(cls):
        cls.dj.on_request(yandexuid='11221252').respond(
            [DjModel(id='23', attributes={'jointPurchase': '1'}), DjModel(id='8', attributes={'jointPurchase': '1'})]
        )

        cls.dj.on_request(yandexuid='11221252', shown_model_ids='23,8').respond([DjModel(id='1914451')])

        cls.dj.on_request(yandexuid='11221253').respond(
            [
                DjModel(id='22', attributes={'RecomOutputType': 'accessoriesBlock'}),
                DjModel(id='27', attributes={'RecomOutputType': 'accessoriesBlock'}),
            ]
        )

        cls.dj.on_request(yandexuid='11221253', shown_model_ids='22,27').respond(
            [
                DjModel(id='23', attributes={'RecomOutputType': 'accessoriesBlock'}),
            ]
        )

    def test_paging_with_min_count_to_show(self):
        """
        Проверяем, что на страницах после первой количество моделей в выдаче может быть меньше min-count-to-show
        """
        query = (
            'place=promo_cart_discount&rids=213&rgb=blue&client=ANDROID&debug=da'
            + '&rearr-factors=market_promo_cart_clients_permitted_accessories_replacement=ANDROID;'
            + 'market_dj_exp_for_promo_cart_force=1;dj_paging_promo_cart_discount=1'
            + '&yandexuid={yandexuid}&page={page}&cart={cart}&view-unique-id={view_unique_id}&min-count-to-show={min_count_to_show}'
        )

        # Прайсдропная выдача
        response = self.report.request_json(
            query.format(
                yandexuid='11221252',
                page='1',
                cart='BLUEModel4FEED2222dddQ,BLUEModel21FEED8888eee,BLUEModel1xxxxwsks2485',
                view_unique_id='1309',
                min_count_to_show='2',
            )
        )

        self.assertFragmentIn(
            response,
            {
                'recomOutputType': 'pricedrop',
                'results': [
                    {'entity': 'product', 'id': 23},
                    {'entity': 'product', 'id': 8},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(
            query.format(
                yandexuid='11221252',
                page='2',
                cart='BLUEModel4FEED2222dddQ,BLUEModel21FEED8888eee,BLUEModel1xxxxwsks2485',
                view_unique_id='1309',
                min_count_to_show='2',
            )
        )

        # На второй странице минимальное ограничение не применяется
        # даже если на второй странице прайсдропа нет аксов/сопутки
        self.assertFragmentIn(
            response,
            {
                'recomOutputType': 'pricedrop',
                'results': [
                    {'entity': 'product', 'id': 1914451},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Аксессуарная выдача
        response = self.report.request_json(
            query.format(
                yandexuid='11221253',
                page='1',
                cart='BLUEModel21FEED8888eee,BLUEModel25FEED8888eee',
                view_unique_id='1310',
                min_count_to_show='2',
            )
        )

        self.assertFragmentIn(
            response,
            {
                'recomOutputType': 'accessories',
                'results': [
                    {'entity': 'product', 'id': 22},
                    {'entity': 'product', 'id': 27},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(
            query.format(
                yandexuid='11221253',
                page='2',
                cart='BLUEModel21FEED8888eee,BLUEModel25FEED8888eee',
                view_unique_id='1310',
                min_count_to_show='2',
            )
        )

        # На второй странице минимальное ограничение не применяется
        self.assertFragmentIn(
            response,
            {
                'recomOutputType': 'accessories',
                'results': [
                    {'entity': 'product', 'id': 23},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_pass_non_trash_to_second_page(cls):
        cls.dj.on_request(yandexuid='11240125').respond(
            [DjModel(id='23', attributes={'jointPurchase': '1'}), DjModel(id='8')]
        )

        cls.dj.on_request(yandexuid='11240125', shown_model_ids='23,8', non_trash_models='23').respond(
            [DjModel(id='1914451')]
        )

    @classmethod
    def prepare_pass_models_with_found_accessories_to_second_page(cls):
        cls.dj.on_request(yandexuid='11240143').respond(
            [
                DjModel(id='27', attributes={'RecomOutputType': 'accessoriesBlock', 'accessoryGroups': '25'}),
            ]
        )
        cls.dj.on_request(yandexuid='11240143', shown_model_ids='27', models_with_accs='25').respond(
            [DjModel(id='28', attributes={'RecomOutputType': 'accessoriesBlock', 'jointPurchaseGroups': '25'})]
        )

    def test_pass_models_with_found_accessories_to_second_page(self):
        query = (
            'place=promo_cart_discount&rids=213&rgb=blue&client=ANDROID&debug=da'
            + '&rearr-factors=market_promo_cart_clients_permitted_accessories_replacement=ANDROID;'
            + 'market_dj_exp_for_promo_cart_force=1;dj_paging_promo_cart_discount=1'
            + '&yandexuid={yandexuid}&page={page}&cart={cart}&view-unique-id={view_unique_id}&min-count-to-show={min_count_to_show}'
        )

        # Прайсдропная выдача
        response = self.report.request_json(
            query.format(
                yandexuid='11240143',
                page='1',
                cart='BLUEModel25FEED8888eee',
                view_unique_id='0145',
                min_count_to_show='1',
            )
        )

        self.assertFragmentIn(
            response,
            {
                'recomOutputType': 'accessories',
                'results': [
                    {'entity': 'product', 'id': 27},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(
            query.format(
                yandexuid='11240143',
                page='2',
                cart='BLUEModel25FEED8888eee',
                view_unique_id='0145',
                min_count_to_show='1',
            )
        )

        self.assertFragmentIn(
            response,
            {
                'recomOutputType': 'accessories',
                'results': [
                    {'entity': 'product', 'id': 28},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_min_and_max_count_tests(cls):
        cls.index.hypertree += [
            HyperCategory(hid=90639, name="Телевизоры", output_type=HyperCategoryType.GURU),
            HyperCategory(
                hid=90629, name="Кронштейны и стойки для телевизоров и аудиотехники", output_type=HyperCategoryType.GURU
            ),
            HyperCategory(hid=15727468, name="Консервы из мяса и субпродуктов", output_type=HyperCategoryType.GURU),
            HyperCategory(hid=542020, name="Комплекты акустики", output_type=HyperCategoryType.GURU),
        ]

        cls.index.promos_by_cart += [
            PromoByCart(cart_hid=90639, promo_hid=90629, percent=3),
            PromoByCart(cart_hid=90639, promo_hid=15727468, percent=5),
        ]

        cls.index.shops += [
            Shop(
                fesh=1525,
                datafeed_id=1525,
                priority_region=213,
                regions=[225],
                name="НеМаркет!",
                supplier_type=Shop.FIRST_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
            ),
        ]

        description = "Ещё не совсем Маркет..."

        def create_offers(hid, title, hyperid):
            cls.index.models += [Model(hid=hid, hyperid=hyperid, title=title)]
            cls.index.mskus += [
                MarketSku(
                    title=title + " от Маркета",
                    descr=description,
                    hyperid=hyperid,
                    sku=hyperid * 10 + 2,
                    hid=hid,
                    delivery_buckets=[1234],
                    blue_offers=[
                        BlueOffer(
                            price=1400,
                            feedid=1525,
                            offerid="non_market_model{}_e".format(hyperid),
                            waremd5="BLUEModel{}FEED1525eeee".format(hyperid)[0:22],
                            randx=(hyperid * 10 + 2) * 2,
                        )
                    ],
                ),
            ]

        create_offers(hid=90639, title="Телевизор Xiaomi", hyperid=100)

        create_offers(hid=90629, title="Кронштейн iTECHmount", hyperid=101)
        create_offers(hid=90629, title="Кронштейн Arm media", hyperid=102)
        create_offers(hid=90629, title="Кронштейн Патрон AK", hyperid=103)
        create_offers(hid=90629, title="Кронштейн Орбита", hyperid=104)
        create_offers(hid=90629, title="Кронштейн Kromax", hyperid=105)
        create_offers(hid=90629, title="Кронштейн ABC Mount", hyperid=106)

        create_offers(hid=15727468, title="Говядина тушёная", hyperid=107)
        create_offers(hid=15727468, title="Свинина тушёная", hyperid=108)
        create_offers(hid=15727468, title="Конина тушёная", hyperid=109)
        create_offers(hid=15727468, title="Оленина тушёная", hyperid=110)
        create_offers(hid=15727468, title="Лосятина тушёная", hyperid=111)
        create_offers(hid=15727468, title="Индейка тушёная", hyperid=112)

        create_offers(hid=542020, title="Саундбар Xiaomi Mi TV", hyperid=113)
        create_offers(hid=542020, title="Саундбар Xiaomi Redmi TV", hyperid=114)
        create_offers(hid=542020, title="Саундбар JBL Cinema", hyperid=115)
        create_offers(hid=542020, title="Комплект акустики YAMAHA", hyperid=116)
        create_offers(hid=542020, title="Саундбар LG", hyperid=117)
        create_offers(hid=542020, title="Саундбар Samsung", hyperid=118)

        cls.dj.on_request(yandexuid='123451600').respond(
            [
                DjModel(id='101', attributes={'accessory': '1'}),
                DjModel(id='102', attributes={'accessory': '1'}),
                DjModel(id='107'),
                DjModel(id='108'),
                DjModel(id='109'),
                DjModel(id='113', attributes={'RecomOutputType': 'accessoriesBlock'}),
                DjModel(id='114', attributes={'RecomOutputType': 'accessoriesBlock'}),
                DjModel(id='115', attributes={'RecomOutputType': 'accessoriesBlock'}),
                DjModel(id='116', attributes={'RecomOutputType': 'accessoriesBlock'}),
            ]
        )

        cls.dj.on_request(yandexuid='123451627').respond(
            [
                DjModel(id='107'),
                DjModel(id='108'),
                DjModel(id='109'),
                DjModel(id='110'),
                DjModel(id='111'),
                DjModel(id='112'),
                DjModel(id='113', attributes={'RecomOutputType': 'accessoriesBlock'}),
                DjModel(id='114', attributes={'RecomOutputType': 'accessoriesBlock'}),
                DjModel(id='115', attributes={'RecomOutputType': 'accessoriesBlock'}),
                DjModel(id='116', attributes={'RecomOutputType': 'accessoriesBlock'}),
            ]
        )

        cls.dj.on_request(yandexuid='123451630').respond(
            [
                DjModel(id='101', attributes={'accessory': '1'}),
                DjModel(id='102', attributes={'accessory': '1'}),
                DjModel(id='103', attributes={'accessory': '1'}),
                DjModel(id='104', attributes={'accessory': '1'}),
                DjModel(id='105', attributes={'accessory': '1'}),
                DjModel(id='106', attributes={'accessory': '1'}),
                DjModel(id='107'),
                DjModel(id='108'),
                DjModel(id='109'),
                DjModel(id='110'),
                DjModel(id='111'),
                DjModel(id='112'),
                DjModel(id='113', attributes={'RecomOutputType': 'accessoriesBlock'}),
                DjModel(id='114', attributes={'RecomOutputType': 'accessoriesBlock'}),
                DjModel(id='115', attributes={'RecomOutputType': 'accessoriesBlock'}),
                DjModel(id='116', attributes={'RecomOutputType': 'accessoriesBlock'}),
                DjModel(id='117', attributes={'RecomOutputType': 'accessoriesBlock'}),
                DjModel(id='118', attributes={'RecomOutputType': 'accessoriesBlock'}),
            ]
        )

    def test_output_without_min_count_flag(self):
        query = (
            'place=promo_cart_discount&rids=213&rgb=blue&client=ANDROID&debug=da'
            + '&rearr-factors=market_promo_cart_clients_permitted_accessories_replacement=ANDROID;market_dj_exp_for_promo_cart_force=1&cart=BLUEModel100FEED1525ee'
        )

        response = self.report.request_json(query + '&yandexuid=123451600')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    'recomOutputType': 'pricedrop',
                    'results': [
                        {'entity': 'product', 'id': 101},
                        {'entity': 'product', 'id': 102},
                        {'entity': 'product', 'id': 107},
                        {'entity': 'product', 'id': 108},
                        {'entity': 'product', 'id': 109},
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_pricedrop_promo_trace(self):
        """
        MARKETOUT-37497 Проверяем наличие дебажной выдачи для прайсдропной промо
        под флагом market_promo_cart_print_debug
        """
        # Прайсдропа нет, т.к. корзина пустая
        request = (
            MULTIPROMO_QUERY.format(hyperid="46955401", cart="")
            + "&debug=1&rearr-factors=market_promo_cart_print_debug=1;market_documents_search_trace="
            + b64url_md5("coil-1")
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"debugStr": "PD|469554001|0|1|1|vdhmZgzU7XsHp57zAXrUMw|145||cart_hids_empty"})

        # Прайсдроп есть
        request = (
            MULTIPROMO_QUERY.format(hyperid="46955401", cart=b64url_md5("fishing-rod-1"))
            + "&debug=1&rearr-factors=market_promo_cart_print_debug=1;market_documents_search_trace="
            + b64url_md5("coil-1")
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "debugStr": "PD|469554001|0|1|1|vdhmZgzU7XsHp57zAXrUMw|145|145|91517|469554|1||passed_validation|has_props|promo_25percent_in_hid469554_for_1p_if_hid91517_in_cart:Active,|and_what?"
            },
        )

    def test_disble_for_placements(self):
        response = self.report.request_json(
            'place=promo_cart_discount&pp=777&rids=213&offerid=BLUEModel4FEED2222dddQ&debug=da'
            '&rearr-factors=market_disabled_placements_for_promo_cart_discount=20,21,777'
        )

        self.assertFragmentIn(response, {"logicTrace": [Contains("Disabled for this placement")]})

        response = self.report.request_json(
            'place=promo_cart_discount&pp=888&rids=213&offerid=BLUEModel4FEED2222dddQ&debug=da'
            '&rearr-factors=market_disabled_placements_for_promo_cart_discount=20,21,777'
        )

        self.assertFragmentNotIn(response, {"logicTrace": [Contains("Disabled for this placement")]})

    def test_choose_dj_exp(self):
        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&offerid=BLUEModel4FEED2222dddQ&debug=da'
            '&rearr-factors=market_dj_exp_for_promo_cart_discount=dj_exp2'
        )
        self.assertFragmentIn(response, {"logicTrace": [Contains("Selected DJ Exp: dj_exp2")]})

        response = self.report.request_json(
            'place=promo_cart_discount&rids=213&offerid=BLUEModel4FEED2222dddQ&debug=da'
        )
        self.assertFragmentIn(response, {"logicTrace": [Contains("Selected DJ Exp: default_for_pricedrop")]})


if __name__ == '__main__':
    main()
