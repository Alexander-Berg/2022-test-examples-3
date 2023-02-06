#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa


from core.testcase import TestCase, main
from core.types import (
    BlueOffer,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
    HyperCategory,
    HyperCategoryType,
    MarketSku,
    Model,
    NavCategory,
    Offer,
    Promo,
    PromoByCart,
    PromoType,
    RegionalDelivery,
    Shop,
)
from core.matcher import Contains
from core.dj import DjModel

from core.types.autogen import b64url_md5
from core.types.offer_promo import PromoBlueCashback, PromoBlueFlash, PromoDirectDiscount

from market.pylibrary.const.offer_promo import MechanicsPaymentType


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
    'market_enable_single_promo=0;market_promo_cart_discount_enable_any_rgb=1'
)


class T(TestCase):
    """
    MARKETOUT-24781
    Параметр cart-hid передается фронтом и чекаутером и содержит категории товаров в корзине пользователя
    Некоторые из категорий дают дополнительные скидки на товары из других категорий (см svn-data/package-data/promo-by-cart.tsv)
    Данные скидки должны применяться к только к офферам с пометкой "Товар предоставлен магазином Беру" (т.е. предоставляемые first-party поставщиком)
    """

    @classmethod
    def prepare(cls):

        cls.settings.set_default_reqid = False
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
                            randx=hyperid * 10 + 1,
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
                            randx=hyperid * 10 + 2,
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
                            randx=hyperid * 10 + 3,
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
                            randx=hyperid * 10 + 4,
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
                            randx=hyperid * 10 + 5,
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
                            randx=hyperid * 10 + 6,
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
                            randx=hyperid * 10 + 7,
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
                            randx=hyperid * 10 + 8,
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

    def test_choose_dj_exp(self):
        response = self.report.request_json(
            'place=pricedrop_kit&rids=213&offerid=BLUEModel4FEED2222dddQ&debug=da'
            '&rearr-factors=market_dj_exp_for_pricedrop_kit=777'
        )

        self.assertFragmentIn(response, {"logicTrace": [Contains("Selected DJ Exp: 777")]})
        self.error_log.expect(code=3787)

    def test_cgi_invalid(self):
        expected_error = {"error": {"code": "INVALID_USER_CGI"}}

        response = self.report.request_json(
            'place=pricedrop_kit&rids=213&offerid=BLUEModel4FEED2222dddQ,BLUEModel1FEED3333wwww'
        )
        self.assertFragmentIn(response, expected_error)
        self.error_log.expect(code=3043)

        response = self.report.request_json('place=pricedrop_kit&rids=213')
        self.assertFragmentIn(response, expected_error)
        self.error_log.expect(code=3043)

    def test_remove_extra_cart_cgi_params(self):
        response = self.report.request_json(
            'place=pricedrop_kit&rids=213&offerid=BLUEModel4FEED2222dddQ'
            '&cart-fo=BLUEModel1FEED3333wwww&cart-sku=BLUEModel3FEED1111gggg&debug=1'
        )

        self.assertFragmentIn(response, {"logicTrace": [Contains("Found 1 documents from cart")]})
        self.error_log.ignore(code=3787)


if __name__ == '__main__':
    main()
