#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Currency, Model, Promo, PromoType, Shop, Vendor
from core.testcase import TestCase, main
from core.matcher import ElementCount
from core.types.sku import MarketSku, BlueOffer
from core.types.autogen import b64url_md5
from market.proto.common.promo_pb2 import ESourceType
from core.types.offer_promo import PromoDirectDiscount, OffersMatchingRules

SUPPLIER_OFFERS_COUNT = 3

# Сохранить ключ промо для проверки на выдаче
DCO_3P_PROMO_MD5_KEY = b64url_md5(5)


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.shops += [
            Shop(
                fesh=4,
                datafeed_id=4,
                priority_region=213,
                regions=[225],
                warehouse_id=145,
                currency=Currency.RUR,
                is_supplier=True,
                fulfillment_program=True,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                name="3P поставщик дешевый",
            ),
            Shop(
                fesh=5,
                datafeed_id=5,
                priority_region=213,
                regions=[225],
                name="3P поставщик дорогой",
                supplier_type=Shop.THIRD_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
            ),
            Shop(
                fesh=6,
                datafeed_id=6,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                name="3P поставщик дорогой 2",
            ),
            Shop(
                fesh=500,
                datafeed_id=500,
                priority_region=555,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                supplier_type=Shop.THIRD_PARTY,
            ),
            # Как так я не знаю, но если удалить этот магазин, но rids не работает.
            # То есть отфильтруется по DELIVERY
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                name='ВиртуальныйМагазинНаБеру',
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
            ),
        ]
        cls.index.vendors += [
            Vendor(
                vendor_id=1,
                name='goodVendor',
                website='www.good.com',
                webpage_recommended_shops='http://www.good.com/ru/brandshops/',
                description='VendorDescription',
                logos=[],
            ),
            Vendor(
                vendor_id=2,
                name='fakeVendor',
                website='www.fake.com',
                webpage_recommended_shops='http://www.fake.com/ru/brandshops/',
                description='FakeVendorDescription',
                logos=[],
            ),
        ]

        cls.index.models += [
            Model(hyperid=120, hid=1, title="First Model", is_pmodel=True, vendor_id=1),
            Model(hyperid=220, hid=2, title="Second Model", is_pmodel=False, vendor_id=1),
            Model(hyperid=320, hid=3, title="No vendor Model", is_pmodel=False),
        ]

        def __get_offer_id(x):
            return 'offer_id_{}'.format(x)

        # Для удобства проверки разбрасываем счетчики offer_id
        count4 = 4000  # счетчики для поставщика 4
        count5 = 5000  # счетчики для поставщика 5
        count6 = 6000  # счетчики для поставщика 6

        # Актуальное промо поставщика 5
        promo5 = Promo(
            promo_type=PromoType.DIRECT_DISCOUNT,
            key=DCO_3P_PROMO_MD5_KEY,
            url='http://direct_discount_5.com/',
            shop_promo_id='promo5',
            source_type=ESourceType.DCO_3P_DISCOUNT,
            direct_discount=PromoDirectDiscount(
                items=[
                    {
                        'feed_id': 5,
                        'offer_id': __get_offer_id(count5),  # выдаем промо только для первого оффера
                        'discount_price': {'value': 700, 'currency': 'RUR'},
                        'old_price': {'value': 1000, 'currency': 'RUR'},
                        'max_discount': {'value': 400, 'currency': 'RUR'},
                        'max_discount_percent': 35.0,
                    }
                ],
            ),
            offers_matching_rules=[OffersMatchingRules(feed_offer_ids=[[5, __get_offer_id(count5)]])],
        )

        for i in range(1, SUPPLIER_OFFERS_COUNT + 1):
            hyperid = i * 100 + 20
            sku = 1234 + i
            # Выдаем промо только первому офферу
            promo = promo5 if count5 == 5000 else None
            cls.index.mskus += [
                MarketSku(
                    title="Sku_{}".format(i),
                    hyperid=hyperid,
                    sku=sku,
                    ref_min_price=500,
                    ref_min_price_url='test.ru',
                    hid=i,
                    blue_offers=[
                        BlueOffer(price=100, feedid=4, offerid=__get_offer_id(count4), supplier_id=4),
                        BlueOffer(price=1000, feedid=5, offerid=__get_offer_id(count5), supplier_id=5, promo=promo),
                        BlueOffer(price=505, feedid=6, offerid=__get_offer_id(count6), supplier_id=6),
                    ],
                )
            ]
            count4 += 1
            count5 += 1
            count6 += 1

    def test_offers_by_supplier_id(self):
        """
        Проверяем, что если возвращаются нужные оффера (с заданным supplier_id) в нужном количесве
        """
        response = self.report.request_json(
            'place=supplier_high_prices&supplier-id=4&high-price-problem=2&numdoc=3&debug=da&rids=213'
        )
        self.assertFragmentIn(response, {"total": 0})
        response = self.report.request_json(
            'place=supplier_high_prices&supplier-id=5&high-price-problem=2&numdoc=3&debug=da&rids=213'
        )
        self.assertFragmentIn(response, {"total": SUPPLIER_OFFERS_COUNT})
        self.assertFragmentIn(response, {"results": ElementCount(3)})
        response = self.report.request_json('place=supplier_high_prices&supplier-id=6')
        self.assertFragmentIn(response, {"total": SUPPLIER_OFFERS_COUNT})

    def test_direct_discount_dco_3p_promo(self):
        """
        Проверяем наличие флага has_dco_3p_subsidy на выдаче
        """
        response = self.report.request_json('place=supplier_high_prices&supplier-id=5')
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "shop": {"feed": {"offerId": "5.offer_id_5000"}},
                "prices": {"value": "700", "currency": "RUR"},
                "promos": [
                    {
                        "type": "direct-discount",
                        "key": DCO_3P_PROMO_MD5_KEY,
                        "url": "http://direct_discount_5.com/",
                        "shopPromoId": "promo5",
                        "itemsInfo": {
                            "price": {
                                "value": "700",
                                "currency": "RUR",
                                "discount": {"oldMin": "1000", "absolute": "300"},
                                "subsidy": {"oldMin": "1000", "absolute": "300"},
                            },
                            "has_dco_3p_subsidy": True,
                        },
                    }
                ],
            },
        )

    def test_ref_min_price_in_offer(self):
        """
        Проверяем, что есть ref_min_price и ref_min_price_url
        """
        response = self.report.request_json(
            'place=supplier_high_prices&supplier-id=5&high-price-problem=2&numdoc=1&rgb=blue'
        )
        self.assertFragmentIn(response, {"results": ElementCount(1)})
        self.assertFragmentIn(response, {"refMinPrice": {'value': '500'}})
        self.assertFragmentIn(response, {"refMinPriceUrl": "test.ru"})

    def test_paging(self):
        """
        Проверяем, что если возвращаются нужные оффера (c двумя supplier_id из трех возможных) в нужном количесве
        """
        response = self.report.request_json('place=supplier_high_prices&supplier-id=5&numdoc=1&page=2')
        self.assertFragmentIn(response, {"total": SUPPLIER_OFFERS_COUNT})
        self.assertFragmentIn(response, {"results": ElementCount(1)})

    def test_offers_by_hid(self):
        """
        Проверяем, что по hid тоже ищется
        """
        response = self.report.request_json('place=supplier_high_prices&supplier-id=5&hid=1&numdoc=10')
        self.assertFragmentIn(response, {"total": 1})
        self.assertFragmentIn(response, {"results": ElementCount(1)})
        self.assertFragmentIn(response, {"categories": [{"entity": "category", "id": 1}]})
        self.assertFragmentNotIn(response, {"categories": [{"entity": "category", "id": 2}]})
        self.assertFragmentNotIn(response, {"categories": [{"entity": "category", "id": 3}]})

    def test_offers_by_text(self):
        """
        Проверяем, текстовый поиск
        """
        response = self.report.request_json('place=supplier_high_prices&supplier-id=4,5,6&text=sku_1')
        self.assertFragmentIn(response, {"total": 2})
        response = self.report.request_json('place=supplier_high_prices&supplier-id=5&text=sku_')
        self.assertFragmentIn(response, {"total": SUPPLIER_OFFERS_COUNT})

    def test_offers_by_rids(self):
        """
        Проверяем, что поиск по региону работает
        """
        response = self.report.request_json('place=supplier_high_prices&supplier-id=5,6&rids=213')
        self.assertFragmentIn(response, {"total": 2 * SUPPLIER_OFFERS_COUNT})
        self.assertFragmentIn(response, {"results": ElementCount(2 * SUPPLIER_OFFERS_COUNT)})

    def test_offers_by_rids_empty(self):
        """
        Проверяем, что если поиск по региону не находит ничего если ничего и нет под этим rids
        """
        response = self.report.request_json(
            'place=supplier_high_prices&supplier-id=4,5&rids=555&rearr-factors=market_nordstream=0'
        )
        self.assertFragmentIn(response, {"total": 0})

    def test_psku_offers(self):
        """
        Проверяем, что если поиск включает все оффера, даже psku
        """
        response = self.report.request_json('place=supplier_high_prices&supplier-id=5')
        self.assertFragmentIn(response, {"total": SUPPLIER_OFFERS_COUNT})
        self.assertFragmentIn(response, {"results": ElementCount(SUPPLIER_OFFERS_COUNT)})

    @classmethod
    def prepare_buybox(cls):
        cls.index.mskus += [
            MarketSku(
                title="prepare_buybox",
                hyperid=9000,
                sku=10002,
                ref_min_price=1500,
                hid=90,
                blue_offers=[
                    BlueOffer(price=1900, supplier_id=31, feedid=31),
                    BlueOffer(price=1500, supplier_id=32, feedid=32),
                    BlueOffer(price=1600, supplier_id=32, feedid=32),
                ],
            )
        ]

    def test_buybox(self):
        """
        Проверяем, что попадут только те оффера которые проиграли байбокс (цена выше) но ниже рефмина
        """
        response = self.report.request_json(
            'place=supplier_high_prices&supplier-id=31&high-price-problem=1&debug=da&rgb=blue'
        )
        self.assertFragmentIn(response, {"total": 0})
        self.assertFragmentIn(response, {"filters": {"HIGH_PRICE_BUYBOX": 1}})
        response = self.report.request_json(
            'place=supplier_high_prices&supplier-id=32&high-price-problem=1&debug=da&rgb=blue'
        )
        self.assertFragmentIn(response, {"total": 1})
        self.assertFragmentIn(response, {"buyboxBestPrice": {"currency": "RUR", "value": "1500"}})
        response = self.report.request_json(
            'place=supplier_high_prices&supplier-id=32&high-price-problem=2&debug=da&rgb=blue'
        )
        self.assertFragmentIn(response, {"total": 1})
        self.assertFragmentIn(response, {"highPriceProblem": 2})

    def test_output_high_price_problem(self):
        """
        Проверяем, что какой high-price-problem мы запрашиваем, такой же в ответе
        """
        response = self.report.request_json('place=supplier_high_prices&supplier-id=5&high-price-problem=2&rgb=blue')
        self.assertFragmentIn(response, {"total": SUPPLIER_OFFERS_COUNT})
        self.assertFragmentIn(response, {"highPriceProblem": 2})
        response = self.report.request_json(
            'place=supplier_high_prices&supplier-id=32&high-price-problem=1&rgb=blue&rgb=blue'
        )
        self.assertFragmentIn(response, {"total": 1})
        self.assertFragmentIn(response, {"highPriceProblem": 1})

    @classmethod
    def prepare_buybox_same_prices(cls):
        cls.index.mskus += [
            MarketSku(
                title="prepare_buybox_same_prices",
                hyperid=777,
                sku=10007,
                ref_min_price=10000,
                hid=97,
                blue_offers=[
                    BlueOffer(price=1500, supplier_id=37, feedid=37),
                    BlueOffer(price=1500, supplier_id=37, feedid=37),
                ],
            )
        ]

    def test_buybox_same_prices(self):
        """
        Проверяем, что если у оффера цена как у байбокса то он не попадет в выдачу
        """
        response = self.report.request_json(
            'place=supplier_high_prices&supplier-id=37&high-price-problem=1&debug=da&rgb=blue'
        )
        self.assertFragmentIn(response, {"total": 0})
        self.assertFragmentIn(response, {"filters": {"HIGH_PRICE_BUYBOX": 2}})

    @classmethod
    def prepare_same_ssku_offers(cls):
        cls.index.mskus += [
            MarketSku(
                title="prepare_same_ssku_offers",
                hyperid=888,
                sku=10008,
                ref_min_price=10,
                hid=98,
                blue_offers=[
                    BlueOffer(
                        price=1500, supplier_id=38, feedid=38, offerid='10855364', waremd5='OFF1_1500_SKU1_SUP11_Q'
                    ),
                    BlueOffer(
                        price=1500, supplier_id=38, feedid=38, offerid='10855364', waremd5='OFF2_1500_SKU1_SUP11_Q'
                    ),
                    BlueOffer(
                        price=1500, supplier_id=38, feedid=39, offerid='10855364', waremd5='OFF3_1500_SKU1_SUP12_Q'
                    ),
                ],
            )
        ]

    def test_same_ssku_offers(self):
        """
        Проверяем, что если у msku два одинаковых оффера, но с разных складов, например. То они схлопнутся
        """
        response = self.report.request_json('place=supplier_high_prices&supplier-id=38&debug=da&rgb=blue')
        self.assertFragmentIn(response, {"total": 1})

    def test_msku_filter(self):
        """
        Проверяем, что фильтр по market-sku работает
        """
        response = self.report.request_json(
            'place=supplier_high_prices&supplier-id=5&market-sku=1235&debug=da&rgb=blue'  # generated marke_sku in the first prepare
        )
        self.assertFragmentIn(response, {"total": 1})

        response = self.report.request_json(
            'place=supplier_high_prices&supplier-id=5&market-sku=10007&debug=da&rgb=blue'
        )
        self.assertFragmentIn(response, {"total": 0})

    @classmethod
    def prepare_ssku_filter(cls):
        cls.index.mskus += [
            MarketSku(
                title="prepare_ssku_filter",
                hyperid=999,
                sku=10009,
                ref_min_price=10,
                hid=99,
                blue_offers=[
                    BlueOffer(price=1501, supplier_id=99, feedid=99, offerid='9991', waremd5='OFF1_1501_SKU1_SUP99_Q'),
                    BlueOffer(price=1502, supplier_id=99, feedid=99, offerid='9992', waremd5='OFF2_1502_SKU2_SUP99_Q'),
                ],
            )
        ]

    def test_ssku_filter(self):
        """
        Проверяем, что фильтр по shop-sku работает
        """
        response = self.report.request_json('place=supplier_high_prices&supplier-id=99&debug=da&rgb=blue')
        self.assertFragmentIn(response, {"total": 2})
        response = self.report.request_json('place=supplier_high_prices&supplier-id=99&shop-sku=9991&debug=da&rgb=blue')
        self.assertFragmentIn(response, {"total": 1})
        response = self.report.request_json('place=supplier_high_prices&supplier-id=99&shop-sku=9992&debug=da&rgb=blue')
        self.assertFragmentIn(response, {"total": 1})
        response = self.report.request_json(
            'place=supplier_high_prices&supplier-id=99&shop-sku=9992&shop-sku=9991&debug=da&rgb=blue'
        )
        self.assertFragmentIn(response, {"total": 2})
        response = self.report.request_json(
            'place=supplier_high_prices&supplier-id=99&shop-sku=9992,9993&debug=da&rgb=blue'
        )
        self.assertFragmentIn(response, {"total": 1})

    @classmethod
    def prepare_near_ref_min_price(cls):
        cls.index.mskus += [
            MarketSku(
                title="prepare_near_ref_min_price",
                hyperid=10011,
                sku=10011,
                ref_min_price=1500.0,
                hid=111,
                blue_offers=[
                    BlueOffer(price=1500.1, supplier_id=111, feedid=111),
                ],
            )
        ]
        cls.index.mskus += [
            MarketSku(
                title="prepare_near_ref_min_price",
                hyperid=10012,
                sku=10012,
                ref_min_price=1500.1,
                hid=112,
                blue_offers=[
                    BlueOffer(price=1500.0, supplier_id=112, feedid=112),
                ],
            )
        ]
        cls.index.mskus += [
            MarketSku(
                title="prepare_near_ref_min_price",
                hyperid=10013,
                sku=10013,
                ref_min_price=1500.0,
                hid=113,
                blue_offers=[
                    BlueOffer(price=1500.0, supplier_id=113, feedid=113),
                ],
            )
        ]
        cls.index.mskus += [
            MarketSku(
                title="prepare_near_ref_min_price",
                hyperid=10014,
                sku=10014,
                ref_min_price=1501.0,
                hid=114,
                blue_offers=[
                    BlueOffer(price=1500.0, supplier_id=114, feedid=114),
                ],
            )
        ]
        cls.index.mskus += [
            MarketSku(
                title="prepare_near_ref_min_price",
                hyperid=10015,
                sku=10015,
                ref_min_price=1500.0,
                hid=115,
                blue_offers=[
                    BlueOffer(price=1501.0, supplier_id=115, feedid=115),
                ],
            )
        ]

    def test_near_ref_min_price(self):
        """
        Проверяем, что не попадут ордера, у которых цена меньше чем на рубль отличается от рефмина
        """
        # refmin < price -> to high price, но меньше рубля, так что тоже всё окей
        response = self.report.request_json(
            'place=supplier_high_prices&supplier-id=111&high-price-problem=2&debug=da&rgb=blue'
        )
        self.assertFragmentIn(response, {"total": 0})
        self.assertFragmentIn(response, {"filters": {"HIGH_PRICE_MIN_REF": 1}})
        # refmin > price -> all fine
        response = self.report.request_json(
            'place=supplier_high_prices&supplier-id=112&high-price-problem=2&debug=da&rgb=blue'
        )
        self.assertFragmentIn(response, {"total": 0})
        self.assertFragmentIn(response, {"filters": {"HIGH_PRICE_MIN_REF": 1}})
        # refmin == price -> all fine
        response = self.report.request_json(
            'place=supplier_high_prices&supplier-id=113&high-price-problem=2&debug=da&rgb=blue'
        )
        self.assertFragmentIn(response, {"total": 0})
        self.assertFragmentIn(response, {"filters": {"HIGH_PRICE_MIN_REF": 1}})
        # refmin > price + 1 -> all fine
        response = self.report.request_json(
            'place=supplier_high_prices&supplier-id=114&high-price-problem=2&debug=da&rgb=blue'
        )
        self.assertFragmentIn(response, {"total": 0})
        self.assertFragmentIn(response, {"filters": {"HIGH_PRICE_MIN_REF": 1}})
        # refmin + 1 < price -> show problem
        response = self.report.request_json(
            'place=supplier_high_prices&supplier-id=115&high-price-problem=2&debug=da&rgb=blue'
        )
        self.assertFragmentIn(response, {"total": 1})
        self.assertFragmentIn(
            response,
            {
                "refMinPrice": {"currency": "RUR", "value": "1500"},
            },
        )

    @classmethod
    def prepare_ssku_filter_price_limit_less_ref_min(cls):
        cls.index.mskus += [
            MarketSku(
                title="prepare_ssku_filter_price_limit_less_ref_min",
                hyperid=10016,
                sku=10016,
                ref_min_price=1360.0,
                price_limit=1350.0,
                hid=116,
                blue_offers=[
                    BlueOffer(price=1500.0, supplier_id=116, feedid=116),
                    BlueOffer(price=1400.0, supplier_id=116, feedid=116),
                    BlueOffer(price=1300.0, supplier_id=116, feedid=116),
                ],
            )
        ]

    def test_ssku_filter_price_limit_less_ref_min(self):
        """
        Проверяем, что фильтр по shop-sku работает
        """
        response = self.report.request_json('place=supplier_high_prices&supplier-id=116&debug=da&rgb=blue')
        self.assertFragmentIn(response, {"total": 2})
        self.assertFragmentIn(response, {"refMinPrice": {"value": "1360"}})
        self.assertFragmentIn(response, {"commonMinPrice": {"value": "1350"}})
        self.assertFragmentIn(response, {"prices": {"value": "1500"}})
        self.assertFragmentIn(response, {"prices": {"value": "1400"}})


if __name__ == '__main__':
    main()
