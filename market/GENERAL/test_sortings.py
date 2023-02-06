#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.matcher import Absent, Contains, NotEmpty
from core.testcase import TestCase, main
from core.types import (
    BlueOffer,
    Currency,
    DeliveryBucket,
    DeliveryOption,
    DynamicShop,
    ExchangeRate,
    HyperCategory,
    MarketSku,
    MnPlace,
    Model,
    NewShopRating,
    Offer,
    Promo,
    PromoType,
    Region,
    Shop,
    VCluster,
    YamarecPlaceReasonsToBuy,
    Opinion,
)


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]
        cls.settings.default_search_experiment_flags += ['market_new_cpm_iterator=0']
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        # RANDX randomizing is disabled because these tests don't work with it. See MARKETOUT-21319
        cls.disable_randx_randomize()

        cls.index.hypertree += [
            HyperCategory(hid=1),
        ]

        cls.index.shops += [
            Shop(fesh=1, priority_region=213, regions=[225], new_shop_rating=NewShopRating(new_rating_total=1.0)),
            Shop(fesh=2, priority_region=213, regions=[225], new_shop_rating=NewShopRating(new_rating_total=1.0)),
            Shop(fesh=3, priority_region=213, regions=[225], new_shop_rating=NewShopRating(new_rating_total=2.0)),
            Shop(fesh=4, priority_region=213, regions=[225], new_shop_rating=NewShopRating(new_rating_total=2.0)),
            Shop(fesh=5, priority_region=213, regions=[225], new_shop_rating=NewShopRating(new_rating_total=3.0)),
            Shop(fesh=6, priority_region=213, regions=[225], new_shop_rating=NewShopRating(new_rating_total=3.0)),
            Shop(fesh=7, priority_region=213, regions=[225], new_shop_rating=NewShopRating(new_rating_total=4.0)),
            Shop(fesh=8, priority_region=213, regions=[225], new_shop_rating=NewShopRating(new_rating_total=4.0)),
            Shop(fesh=9, priority_region=213, regions=[225], new_shop_rating=NewShopRating(new_rating_total=5.0)),
            Shop(fesh=10, priority_region=213, regions=[225], new_shop_rating=NewShopRating(new_rating_total=5.0)),
            Shop(fesh=11, priority_region=2, regions=[213], new_shop_rating=NewShopRating(new_rating_total=1.0)),
            Shop(fesh=12, priority_region=2, new_shop_rating=NewShopRating(new_rating_total=1.0)),
            Shop(fesh=13, priority_region=2, new_shop_rating=NewShopRating(new_rating_total=2.0)),
            Shop(fesh=14, priority_region=2, new_shop_rating=NewShopRating(new_rating_total=2.0)),
            Shop(fesh=15, priority_region=2, new_shop_rating=NewShopRating(new_rating_total=3.0)),
            Shop(fesh=16, priority_region=2),  # undefined rating means rating is 3 for sorting
            Shop(fesh=17, priority_region=2, new_shop_rating=NewShopRating(new_rating_total=4.0)),
            Shop(fesh=18, priority_region=2, new_shop_rating=NewShopRating(new_rating_total=4.0)),
            Shop(fesh=19, priority_region=2, new_shop_rating=NewShopRating(new_rating_total=5.0)),
            Shop(fesh=20, priority_region=2, new_shop_rating=NewShopRating(new_rating_total=5.0)),
        ]

        cls.index.models += [
            Model(hyperid=201, title="model 1", hid=1, opinion=Opinion(rating=5, rating_count=10, total_count=12)),
            Model(hyperid=202, title="model 2", hid=1, opinion=Opinion(rating=5, rating_count=10, total_count=12)),
            Model(hyperid=203, title="model 3", hid=1, opinion=Opinion(rating=4.3, rating_count=10, total_count=12)),
            Model(hyperid=204, title="model 4", hid=1, opinion=Opinion(rating=4, rating_count=10, total_count=12)),
            Model(hyperid=205, title="model 5", hid=1, opinion=Opinion(rating=3, rating_count=10, total_count=12)),
            Model(hyperid=206, title="model 6", hid=1, opinion=Opinion(rating=3, rating_count=10, total_count=12)),
            Model(hyperid=207, title="model 7", hid=1, opinion=Opinion(rating=2, rating_count=10, total_count=12)),
            Model(hyperid=208, title="model 8", hid=1, opinion=Opinion(rating=2, rating_count=10, total_count=12)),
            Model(hyperid=209, title="model 9", hid=1, opinion=Opinion(rating=1, rating_count=10, total_count=12)),
            Model(hyperid=210, title="model 10", hid=1, opinion=Opinion(rating=1, rating_count=10, total_count=12)),
            Model(hyperid=301, title="model 11", hid=1, opinion=Opinion(rating=5, rating_count=10, total_count=12)),
            Model(hyperid=302, title="model 12", hid=1, opinion=Opinion(rating=5, rating_count=10, total_count=12)),
            Model(hyperid=303, title="model 13", hid=1, opinion=Opinion(rating=4, rating_count=10, total_count=12)),
            Model(hyperid=304, title="model 14", hid=1, opinion=Opinion(rating=4, rating_count=10, total_count=12)),
            Model(hyperid=305, title="model 15", hid=1, opinion=Opinion(rating=3, rating_count=10, total_count=12)),
            Model(hyperid=306, title="model 16", hid=1, opinion=Opinion(rating=3, rating_count=10, total_count=12)),
            Model(hyperid=307, title="model 17", hid=1, opinion=Opinion(rating=2, rating_count=10, total_count=12)),
            Model(hyperid=308, title="model 18", hid=1, opinion=Opinion(rating=2, rating_count=10, total_count=12)),
            Model(hyperid=309, title="model 10", hid=1, opinion=Opinion(rating=1, rating_count=10, total_count=12)),
            Model(hyperid=310, title="model 20", hid=1, opinion=Opinion(rating=1, rating_count=10, total_count=12)),
        ]

        cls.index.models += [
            Model(hyperid=3010, title='model for sort', hid=1),
            Model(hyperid=3011, title='model for sort', hid=1),
            Model(hyperid=10863012, title='model for sort', hid=1),
            Model(hyperid=42, title='meow model for abs discount', hid=1),
        ]

        cls.index.offers += [
            Offer(title='entity', hyperid=309, hid=1, fesh=1, price=104, discount=11),
            Offer(title='entity', hyperid=310, hid=1, fesh=2, price=105, discount=12),
            Offer(title='entity', hyperid=307, hid=1, fesh=3, price=101, discount=13),
            Offer(title='entity', hyperid=308, hid=1, fesh=4, price=108, discount=14),
            Offer(title='entity', hyperid=305, hid=1, fesh=5, price=100, discount=15),
            Offer(title='entity', hyperid=306, hid=1, fesh=6, price=109, discount=16),
            Offer(title='entity', hyperid=303, hid=1, fesh=7, price=102, discount=17),
            Offer(title='entity', hyperid=304, hid=1, fesh=8, price=106, discount=18),
            Offer(title='entity', hyperid=301, hid=1, fesh=9, price=103, discount=10),
            Offer(title='entity', hyperid=302, hid=1, fesh=10, price=107, discount=20),
            Offer(title='entity', hyperid=209, hid=1, fesh=11, price=116),
            Offer(title='entity', hyperid=210, hid=1, fesh=12, price=117, discount=2),
            Offer(title='entity', hyperid=207, hid=1, fesh=13, price=110, discount=3),
            Offer(title='entity', hyperid=208, hid=1, fesh=14, price=118, discount=4),
            Offer(title='entity', hyperid=205, hid=1, fesh=15, price=111, discount=5),
            Offer(title='entity', hyperid=206, hid=1, fesh=16, price=119, discount=6),
            Offer(title='entity', hyperid=203, hid=1, fesh=17, price=112, discount=7),
            Offer(title='entity', hyperid=204, hid=1, fesh=18, price=114, discount=8),
            Offer(title='entity', hyperid=201, hid=1, fesh=19, price=113, discount=9),
            Offer(title='entity', hyperid=202, hid=1, fesh=20, price=115, discount=19),
        ]

        cls.index.offers += [
            Offer(title='offer for sort', hyperid=3010, hid=1, fesh=1, price=104, discount=11),
            Offer(title='offer for sort', hyperid=3011, hid=1, fesh=2, price=105, discount=12),
            Offer(title='offer for sort', hyperid=3010, hid=1, fesh=3, price=101, discount=13),
            Offer(title='offer for sort', hyperid=3011, hid=1, fesh=4, price=108, discount=14),
            Offer(title='offer for sort', hyperid=3010, hid=1, fesh=5, price=100, discount=15),
            Offer(title='offer for sort', hyperid=3011, hid=1, fesh=6, price=109, discount=16),
            Offer(title='offer for sort', hyperid=3010, hid=1, fesh=7, price=102, discount=17),
            Offer(title='offer for sort', hyperid=3011, hid=1, fesh=8, price=106, discount=18),
            Offer(title='offer for sort', hyperid=3011, hid=1, fesh=9, price=103, discount=10),
            Offer(title='offer for sort', hyperid=3011, hid=1, fesh=10, price=107, discount=20),
            Offer(title='offer for sort', hyperid=3010, hid=1, fesh=11, price=116),
            Offer(title='offer for sort', hyperid=3011, hid=1, fesh=12, price=117, discount=2),
            Offer(title='offer for sort', hyperid=3010, hid=1, fesh=13, price=110, discount=3),
            Offer(title='offer for sort', hyperid=3011, hid=1, fesh=14, price=118, discount=4),
            Offer(title='offer for sort', hyperid=3010, hid=1, fesh=15, price=111, discount=5),
            Offer(title='offer for sort', hyperid=3011, hid=1, fesh=16, price=119, discount=6),
            Offer(title='offer for sort', hyperid=3010, hid=1, fesh=17, price=112, discount=7),
            Offer(title='offer for sort', hyperid=3011, hid=1, fesh=18, price=114, discount=8),
            Offer(title='offer for sort', hyperid=3010, hid=1, fesh=19, price=113, discount=9),
            Offer(title='offer for sort', hyperid=3010, hid=1, fesh=20, price=115, discount=19),
            Offer(title='offer for sort', hyperid=10863012, hid=1, fesh=21, price=0.3),
            Offer(title='offer for sort', hyperid=10863012, hid=1, fesh=22, price=0.9),
            Offer(title='offer for sort', hyperid=10863012, hid=1, fesh=23, price=1.3),
            Offer(title='offer for sort', hyperid=10863012, hid=1, fesh=24, price=1.5),
            Offer(title='meow abs discount', price=100, price_old=140, hyperid=42),
            Offer(title='meow abs discount', price=10, price_old=30, hyperid=42),
            Offer(title='meow abs discount', price=200, price_old=215, hyperid=42),
            Offer(title='meow abs discount', price=1000, price_old=1100, hyperid=42),
            Offer(
                title='meow abs discount promo',
                price=1050,
                promo=Promo(
                    promo_type=PromoType.PROMO_CODE,
                    key='promo1034701_03_key000',
                    discount_value=80,
                    discount_currency='RUR',
                ),
                hyperid=42,
            ),
        ]

        cls.index.currencies += [
            Currency(
                name=Currency.UAH,
                exchange_rates=[
                    ExchangeRate(fr=Currency.RUR, rate=2),
                    ExchangeRate(fr=Currency.BYN, rate=0.4),
                    ExchangeRate(fr=Currency.KZT, rate=20),
                ],
            ),
            Currency(
                name=Currency.BYN,
                exchange_rates=[
                    ExchangeRate(fr=Currency.RUR, rate=0.1),
                    ExchangeRate(fr=Currency.UAH, rate=2.5),
                    ExchangeRate(fr=Currency.KZT, rate=50),
                ],
            ),
            Currency(
                name=Currency.KZT,
                exchange_rates=[
                    ExchangeRate(fr=Currency.RUR, rate=0.1),
                    ExchangeRate(fr=Currency.UAH, rate=0.05),
                    ExchangeRate(fr=Currency.BYN, rate=0.02),
                ],
            ),
        ]

    def test_sorting_abs_discount(self):
        """
        Проверяем сортировку по абсолютному значению скидки в плейсах prime и productoffers
        """
        response = self.report.request_json('place=prime&text=meow&numdoc=20&how=adiscount&pp=18')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "prices": {"value": "200", "discount": {"oldMin": "215"}}},
                    {"entity": "product", "prices": {"min": "10", "discount": {"oldMin": "30"}}},
                    {"entity": "offer", "prices": {"value": "10", "discount": {"oldMin": "30"}}},
                    {"entity": "offer", "prices": {"value": "100", "discount": {"oldMin": "140"}}},
                    {"entity": "offer", "prices": {"value": "1050"}, "promos": NotEmpty()},
                    {"entity": "offer", "prices": {"value": "1000", "discount": {"oldMin": "1100"}}},
                ]
            },
            preserve_order=True,
        )

        response = self.report.request_json('place=prime&text=meow&numdoc=20&how=ddiscount&pp=18')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "prices": {"value": "1000", "discount": {"oldMin": "1100"}}},
                    {"entity": "offer", "prices": {"value": "1050"}, "promos": NotEmpty()},
                    {"entity": "offer", "prices": {"value": "100", "discount": {"oldMin": "140"}}},
                    {"entity": "offer", "prices": {"value": "10", "discount": {"oldMin": "30"}}},
                    {"entity": "product", "prices": {"min": "10", "discount": {"oldMin": "30"}}},
                    {"entity": "offer", "prices": {"value": "200", "discount": {"oldMin": "215"}}},
                ]
            },
            preserve_order=True,
        )

        response = self.report.request_json('place=productoffers&how=adiscount&pp=18&hyperid=42')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "prices": {"value": "200", "discount": {"oldMin": "215"}}},
                    {"entity": "offer", "prices": {"value": "10", "discount": {"oldMin": "30"}}},
                    {"entity": "offer", "prices": {"value": "100", "discount": {"oldMin": "140"}}},
                    {"entity": "offer", "prices": {"value": "1050"}, "promos": NotEmpty()},
                    {"entity": "offer", "prices": {"value": "1000", "discount": {"oldMin": "1100"}}},
                ]
            },
            preserve_order=True,
        )

        response = self.report.request_json('place=productoffers&how=ddiscount&pp=18&hyperid=42')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "prices": {"value": "1000", "discount": {"oldMin": "1100"}}},
                    {"entity": "offer", "prices": {"value": "1050"}, "promos": NotEmpty()},
                    {"entity": "offer", "prices": {"value": "100", "discount": {"oldMin": "140"}}},
                    {"entity": "offer", "prices": {"value": "10", "discount": {"oldMin": "30"}}},
                    {"entity": "offer", "prices": {"value": "200", "discount": {"oldMin": "215"}}},
                ]
            },
            preserve_order=True,
        )

    def test_prime_rorp_sort(self):
        """Проверяем что офера сортируются в начале по рейтингу модели а потом по цене
        + отсутсвие рейтинга считается за рейтинг 3.0"""
        response = self.report.request_json('place=prime&text=entity&numdoc=20&rids=2&how=rorp')
        self.assertFragmentIn(
            response, {"sorts": [{"text": "по рейтингу", "options": [{"id": "rorp", "isActive": True}]}]}
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "prices": {"value": "113"}, "model": {"rating": 5}},
                    {"entity": "offer", "prices": {"value": "115"}, "model": {"rating": 5}},
                    {"entity": "offer", "prices": {"value": "112"}, "model": {"rating": 4.3}},
                    {"entity": "offer", "prices": {"value": "114"}, "model": {"rating": 4}},
                    {"entity": "offer", "prices": {"value": "111"}, "model": {"rating": 3}},
                    {"entity": "offer", "prices": {"value": "119"}, "model": {"rating": 3}},
                    {"entity": "offer", "prices": {"value": "110"}, "model": {"rating": 2}},
                    {"entity": "offer", "prices": {"value": "118"}, "model": {"rating": 2}},
                    {"entity": "offer", "prices": {"value": "116"}, "model": {"rating": 1}},
                    {"entity": "offer", "prices": {"value": "117"}, "model": {"rating": 1}},
                    {"entity": "regionalDelimiter"},
                    {"entity": "offer", "prices": {"value": "103"}, "model": {"rating": 5}},
                    {"entity": "offer", "prices": {"value": "107"}, "model": {"rating": 5}},
                    {"entity": "offer", "prices": {"value": "102"}, "model": {"rating": 4}},
                    {"entity": "offer", "prices": {"value": "106"}, "model": {"rating": 4}},
                    {"entity": "offer", "prices": {"value": "100"}, "model": {"rating": 3}},
                    {"entity": "offer", "prices": {"value": "109"}, "model": {"rating": 3}},
                    {"entity": "offer", "prices": {"value": "101"}, "model": {"rating": 2}},
                    {"entity": "offer", "prices": {"value": "108"}, "model": {"rating": 2}},
                    {"entity": "offer", "prices": {"value": "104"}, "model": {"rating": 1}},
                    {"entity": "offer", "prices": {"value": "105"}, "model": {"rating": 1}},
                ]
            },
            preserve_order=True,
        )

    def test_prime_discount_sort(self):
        response = self.report.request_json('place=prime&text=for%20sort&numdoc=25&rids=2&how=discount_p')
        self.assertFragmentIn(
            response, {"sorts": [{"text": "по размеру скидки", "options": [{"id": "discount_p", "isActive": True}]}]}
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "prices": {"discount": {"percent": 20}}},
                    {"entity": "product", "prices": {"discount": {"percent": 19}}},
                    {"entity": "offer", "prices": {"discount": {"percent": 19}}},
                    {"entity": "offer", "prices": {"discount": {"percent": 9}}},
                    {"entity": "offer", "prices": {"discount": {"percent": 8}}},
                    {"entity": "offer", "prices": {"discount": {"percent": 7}}},
                    {"entity": "offer", "prices": {"discount": {"percent": 6}}},
                    {"entity": "offer", "prices": {"discount": {"percent": 5}}},
                    {"entity": "offer", "prices": {"discount": Absent()}},
                    {"entity": "offer", "prices": {"discount": Absent()}},
                    {"entity": "offer", "prices": {"discount": Absent()}},
                    {"entity": "offer", "prices": {"discount": Absent()}},
                    {"entity": "regionalDelimiter"},
                    {"entity": "offer", "prices": {"discount": {"percent": 20}}},
                    {"entity": "offer", "prices": {"discount": {"percent": 18}}},
                    {"entity": "offer", "prices": {"discount": {"percent": 17}}},
                    {"entity": "offer", "prices": {"discount": {"percent": 16}}},
                    {"entity": "offer", "prices": {"discount": {"percent": 15}}},
                    {"entity": "offer", "prices": {"discount": {"percent": 14}}},
                    {"entity": "offer", "prices": {"discount": {"percent": 13}}},
                    {"entity": "offer", "prices": {"discount": {"percent": 12}}},
                    {"entity": "offer", "prices": {"discount": {"percent": 11}}},
                    {"entity": "offer", "prices": {"discount": {"percent": 10}}},
                ]
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_documents_for_discount_sort(cls):
        """
        Создаем кластера и офферы для тестирования сортировки по размеру скидки
        01 dress - 2 оффера со скидкой - в скидке кластера должна быть наибольная
        02 dress - 2 оффера, 1 оффер без скидки, 2 оффер со скидкой, но его цена не минимальная - у кластера нет скидки
        03 dress - 2 оффера, 1 оффер из региона 213 и без скидки, 2 оффер из региона 2 со скидкой - у кластера есть скидка
        """
        cls.index.hypertree += [
            HyperCategory(hid=2, visual=True),
        ]

        cls.index.vclusters += [
            VCluster(hid=2, title='01 dress', vclusterid=1000000101, randx=711),
            VCluster(hid=2, title='02 dress', vclusterid=1000000102, randx=721),
            VCluster(hid=2, title='03 dress', vclusterid=1000000103, randx=731),
        ]

        cls.index.offers += [
            Offer(
                title='01 dress offer 10% discount',
                vclusterid=1000000101,
                hid=2,
                fesh=1,
                price=1000,
                discount=10,
                randx=712,
            ),
            Offer(
                title='01 dress offer 20% discount',
                vclusterid=1000000101,
                hid=2,
                fesh=2,
                price=1000,
                discount=20,
                randx=713,
            ),
            Offer(title='02 dress offer without discount', vclusterid=1000000102, hid=2, fesh=1, price=1000, randx=722),
            Offer(
                title='02 dress offer 50% discount',
                vclusterid=1000000102,
                hid=2,
                fesh=2,
                price=2000,
                discount=50,
                randx=723,
            ),
            Offer(
                title='03 dress from 2 region 50% discount',
                vclusterid=1000000103,
                hid=2,
                fesh=2,
                price=2000,
                discount=15,
                randx=738,
            ),
            Offer(
                title='03 dress from 2 region 70% discount',
                vclusterid=1000000103,
                hid=2,
                fesh=12,
                price=1500,
                discount=70,
                randx=739,
            ),
            # офферы из Питера, чтобы кластера были локальными для Питера
            Offer(vclusterid=1000000101, fesh=12, price=5000, randx=714),
            Offer(vclusterid=1000000102, fesh=12, price=5000, randx=715),
        ]

    def test_prime_discount_sort_clusters(self):
        """Сортировка по размеру скидки
        В данной сортировке нет приоритета моделей над офферами
        Документы выводятся по убыванию максимальной скидки
        (это происходит за счет переранжирования на мете после схлопывания)
        """
        response = self.report.request_json('place=prime&text=dress&rids=213&how=discount_p')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "model": {"id": 1000000102}, "prices": {"discount": {"percent": 50}}},
                    {"entity": "offer", "model": {"id": 1000000101}, "prices": {"discount": {"percent": 20}}},
                    {"type": "cluster", "id": 1000000101, "prices": {"discount": {"percent": 20}}},
                    {"type": "cluster", "id": 1000000103, "prices": {"discount": {"percent": 15}}},
                    {"entity": "offer", "model": {"id": 1000000103}, "prices": {"discount": {"percent": 15}}},
                    {"entity": "offer", "model": {"id": 1000000101}, "prices": {"discount": {"percent": 10}}},
                    {"entity": "offer", "model": {"id": 1000000102}, "prices": {"discount": Absent()}},
                    {"type": "cluster", "id": 1000000102, "prices": {"discount": Absent()}},
                ]
            },
            preserve_order=True,
        )

        # проверка сохранения корректности сортировки после региональной черты
        response = self.report.request_json('place=prime&text=dress&rids=2&how=discount_p')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "model": {"id": 1000000103}, "prices": {"discount": {"percent": 70}}},
                    {"type": "cluster", "id": 1000000103, "prices": {"discount": {"percent": 70}}},
                    {"type": "cluster", "id": 1000000101, "prices": {"discount": {"percent": 20}}},
                    {"type": "cluster", "id": 1000000102, "prices": {"discount": Absent()}},
                    {"entity": "regionalDelimiter"},
                    {"entity": "offer", "model": {"id": 1000000102}, "prices": {"discount": {"percent": 50}}},
                    {"entity": "offer", "model": {"id": 1000000101}, "prices": {"discount": {"percent": 20}}},
                    {"entity": "offer", "model": {"id": 1000000103}, "prices": {"discount": {"percent": 15}}},
                    {"entity": "offer", "model": {"id": 1000000101}, "prices": {"discount": {"percent": 10}}},
                    {"entity": "offer", "model": {"id": 1000000102}, "prices": {"discount": Absent()}},
                ]
            },
            preserve_order=True,
        )

        # отключился единственный локальный для Питера магазин
        # во всех кластерах остались только региональные офферы, поэтому кластера под чертой
        # динстатистики для кластеров считаются в соответствии с оставшимися внутри офферами
        self.dynamic.market_dynamic.disabled_cpc_shops += [DynamicShop(12)]

        response = self.report.request_json('place=prime&text=dress&rids=2&how=discount_p')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "regionalDelimiter"},
                    {"entity": "offer", "model": {"id": 1000000102}, "prices": {"discount": {"percent": 50}}},
                    {"type": "cluster", "id": 1000000101, "prices": {"discount": {"percent": 20}}},
                    {"entity": "offer", "model": {"id": 1000000101}, "prices": {"discount": {"percent": 20}}},
                    {"type": "cluster", "id": 1000000103, "prices": {"discount": {"percent": 15}}},
                    {"entity": "offer", "model": {"id": 1000000103}, "prices": {"discount": {"percent": 15}}},
                    {"entity": "offer", "model": {"id": 1000000101}, "prices": {"discount": {"percent": 10}}},
                    {"type": "cluster", "id": 1000000102, "prices": {"discount": Absent()}},
                    {"entity": "offer", "model": {"id": 1000000102}, "prices": {"discount": Absent()}},
                ]
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_documents_for_old_places_sortings(cls):
        """
        Создаем 10 офферв, у которых будет одинаково слово в описании. Офферы с
        разной ценой, разной стоимостью доставки. Так же несколько офферов
        должно быть с доставкой из других регионов. У некоторых магазинов, для
        которых мы создали офферы, должен быть разный рейтинг. Офферы должны
        быть от нескольких моделей.
        """
        cls.index.regiontree += [
            Region(
                rid=226,
                region_type=Region.COUNTRY,
                children=[
                    Region(
                        rid=227,
                        region_type=Region.FEDERATIVE_SUBJECT,
                        children=[Region(rid=229, region_type=Region.CITY)],
                    ),
                    Region(rid=228, region_type=Region.CITY),
                    Region(rid=230, region_type=Region.CITY),
                ],
            )
        ]

        cls.index.shops += [
            Shop(fesh=101, priority_region=229, regions=[226], new_shop_rating=NewShopRating(new_rating_total=1.0)),
            Shop(fesh=102, priority_region=228, regions=[227], new_shop_rating=NewShopRating(new_rating_total=3.0)),
            Shop(fesh=103, priority_region=230, regions=[229], new_shop_rating=NewShopRating(new_rating_total=5.0)),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket.default(bucket_id=101, fesh=101, rids=[225, 229]),
            DeliveryBucket.default(bucket_id=102, fesh=102, rids=[225, 229]),
            DeliveryBucket.default(bucket_id=103, fesh=103, rids=[225, 229]),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=3, visual=True),
        ]

        cls.index.models += [
            Model(hyperid=4010, title='01 oldplace model', hid=3),
            Model(hyperid=4011, title='02 oldplace model', hid=3),
        ]

        cls.index.offers += [
            Offer(
                title='01 oldplace',
                ts=3001,
                hid=3,
                fesh=101,
                price=1010,
                discount=10,
                hyperid=4011,
                delivery_options=[DeliveryOption(price=45)],
                delivery_buckets=[101],
                bid=10,
            ),
            Offer(
                title='02 oldplace',
                ts=3002,
                hid=3,
                fesh=101,
                price=1001,
                discount=1,
                hyperid=4010,
                delivery_options=[DeliveryOption(price=170)],
                delivery_buckets=[101],
                bid=13,
            ),
            Offer(
                title='03 oldplace',
                ts=3003,
                hid=3,
                fesh=101,
                price=1004,
                discount=4,
                hyperid=4010,
                delivery_options=[DeliveryOption(price=21)],
                delivery_buckets=[101],
                bid=80,
            ),
            Offer(
                title='04 oldplace',
                ts=3004,
                hid=3,
                fesh=102,
                price=1030,
                discount=7,
                hyperid=4010,
                delivery_options=[DeliveryOption(price=3)],
                delivery_buckets=[102],
                bid=90,
            ),
            Offer(
                title='05 oldplace',
                ts=3005,
                hid=3,
                fesh=102,
                price=1600,
                discount=15,
                hyperid=4010,
                delivery_options=[DeliveryOption(price=12)],
                delivery_buckets=[102],
                bid=11,
            ),
            Offer(
                title='06 oldplace',
                ts=3006,
                hid=3,
                fesh=102,
                price=1043,
                discount=0,
                hyperid=4011,
                delivery_options=[DeliveryOption(price=40)],
                delivery_buckets=[102],
                bid=20,
            ),
            Offer(
                title='07 oldplace',
                ts=3007,
                hid=3,
                fesh=103,
                price=1003,
                discount=3,
                hyperid=4010,
                delivery_options=[DeliveryOption(price=43)],
                delivery_buckets=[103],
                bid=30,
            ),
            Offer(
                title='08 oldplace',
                ts=3008,
                hid=3,
                fesh=103,
                price=1020,
                discount=80,
                hyperid=4011,
                delivery_options=[DeliveryOption(price=1)],
                delivery_buckets=[103],
                bid=50,
            ),
            Offer(
                title='09 oldplace',
                ts=3009,
                hid=3,
                fesh=103,
                price=1004,
                discount=30,
                hyperid=4011,
                delivery_options=[DeliveryOption(price=123)],
                delivery_buckets=[103],
                bid=21,
            ),
            Offer(
                title='10 oldplace',
                ts=3010,
                hid=3,
                fesh=103,
                price=1093,
                discount=12,
                hyperid=4011,
                delivery_options=[DeliveryOption(price=3)],
                delivery_buckets=[103],
                bid=80,
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3001).respond(0.353001)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3002).respond(0.344)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3003).respond(0.326)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3004).respond(0.326)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3005).respond(0.346001)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3006).respond(0.340)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3007).respond(0.336001)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3008).respond(0.333)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3009).respond(0.340)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3010).respond(0.326)

    def test_prime_aprice_productoffers(self):
        """
        Проверяем что сортировка по цене в белорусских рублях сортирует с учетом копеек
        и при этом не ломает сортировку товаров, которые стоят меньше одного рубля
        """
        response = self.report.request_json('place=productoffers&currency=BYN&how=aprice&hyperid=3010')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "prices": {"value": "10"}},
                    {"entity": "offer", "prices": {"value": "10.1"}},
                    {"entity": "offer", "prices": {"value": "10.2"}},
                    {"entity": "offer", "prices": {"value": "10.4"}},
                    {"entity": "offer", "prices": {"value": "11"}},
                    {"entity": "offer", "prices": {"value": "11.1"}},
                    {"entity": "offer", "prices": {"value": "11.2"}},
                    {"entity": "offer", "prices": {"value": "11.3"}},
                    {"entity": "offer", "prices": {"value": "11.5"}},
                    {"entity": "offer", "prices": {"value": "11.6"}},
                ]
            },
            preserve_order=True,
        )

        response = self.report.request_json('place=productoffers&how=aprice&hyperid=10863012')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "prices": {"value": "0.3"}},
                    {"entity": "offer", "prices": {"value": "0.9"}},
                    {"entity": "offer", "prices": {"value": "1"}},
                    {"entity": "offer", "prices": {"value": "2"}},
                ]
            },
            preserve_order=True,
        )

    def test_prime_aprice_text_sorting(self):
        """
        place=prime&how=aprice&text=special_text - сортировка по price
        asc, под региональной чертой офферы так же отсортированы верно
        """
        response = self.report.request_json('place=prime&how=aprice&text=oldplace&rids=229&numdoc=20')
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "prices": {"value": "1001"}},
                {"entity": "offer", "prices": {"value": "1004"}},
                {"entity": "offer", "prices": {"value": "1010"}},
                {"entity": "regionalDelimiter"},
                {"entity": "offer", "prices": {"value": "1003"}},
                {"entity": "offer", "prices": {"value": "1004"}},
                {"entity": "offer", "prices": {"value": "1020"}},
                {"entity": "offer", "prices": {"value": "1030"}},
                {"entity": "offer", "prices": {"value": "1043"}},
                {"entity": "offer", "prices": {"value": "1093"}},
                {"entity": "offer", "prices": {"value": "1600"}},
            ],
            preserve_order=True,
        )

    def test_prime_aprice_hid_sorting(self):
        """
        place=prime&how=aprice&hid=special_hid - сортировка по price asc,
        под региональной чертой офферы так же отсортированы верно
        """
        response = self.report.request_json('place=prime&how=aprice&hid=3&rids=229&numdoc=20')
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "prices": {"value": "1001"}},
                {"entity": "offer", "prices": {"value": "1004"}},
                {"entity": "offer", "prices": {"value": "1010"}},
                {"entity": "regionalDelimiter"},
                {"entity": "offer", "prices": {"value": "1003"}},
                {"entity": "offer", "prices": {"value": "1004"}},
                {"entity": "offer", "prices": {"value": "1020"}},
                {"entity": "offer", "prices": {"value": "1030"}},
                {"entity": "offer", "prices": {"value": "1043"}},
                {"entity": "offer", "prices": {"value": "1093"}},
                {"entity": "offer", "prices": {"value": "1600"}},
            ],
            preserve_order=True,
        )

    def test_prime_dprice_text_sorting(self):
        """
        place=prime&how=dprice&text=special_text - сортировка по price
        desc, под региональной чертой офферы так же отсортированы верно
        """
        response = self.report.request_json('place=prime&how=dprice&text=oldplace&rids=229&numdoc=20')

        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "prices": {"value": "1010"}},
                {"entity": "offer", "prices": {"value": "1004"}},
                {"entity": "offer", "prices": {"value": "1001"}},
                {"entity": "regionalDelimiter"},
                {"entity": "offer", "prices": {"value": "1600"}},
                {"entity": "offer", "prices": {"value": "1093"}},
                {"entity": "offer", "prices": {"value": "1043"}},
                {"entity": "offer", "prices": {"value": "1030"}},
                {"entity": "offer", "prices": {"value": "1020"}},
                {"entity": "offer", "prices": {"value": "1004"}},
                {"entity": "offer", "prices": {"value": "1003"}},
            ],
            preserve_order=True,
        )

    def test_prime_dprice_hid_sorting(self):
        """
        place=prime&how=dprice&hid=special_hid - сортировка по price
        desc, под региональной чертой офферы так же отсортированы верно
        """
        response = self.report.request_json('place=prime&how=dprice&hid=3&rids=229&numdoc=20')
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "prices": {"value": "1010"}},
                {"entity": "offer", "prices": {"value": "1004"}},
                {"entity": "offer", "prices": {"value": "1001"}},
                {"entity": "regionalDelimiter"},
                {"entity": "offer", "prices": {"value": "1600"}},
                {"entity": "offer", "prices": {"value": "1093"}},
                {"entity": "offer", "prices": {"value": "1043"}},
                {"entity": "offer", "prices": {"value": "1030"}},
                {"entity": "offer", "prices": {"value": "1020"}},
                {"entity": "offer", "prices": {"value": "1004"}},
                {"entity": "offer", "prices": {"value": "1003"}},
            ],
            preserve_order=True,
        )

    def test_prime_aprice_text_with_delivery_sorting(self):
        """
        place=prime&how=aprice&text=special_text&deliveryincluded=1 -
        сортировка по price asc, в цену включена стоимость доставки, под
        региональной чертой офферы так же отсортированы верно
        """
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        response = self.report.request_json(
            'place=prime&how=aprice&text=oldplace&rids=229&numdoc=20' '&deliveryincluded=1' + unified_off_flags
        )
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "prices": {"rawValue": "1004", "value": "1025"}},
                {"entity": "offer", "prices": {"rawValue": "1010", "value": "1055"}},
                {"entity": "offer", "prices": {"rawValue": "1001", "value": "1171"}},
                {"entity": "regionalDelimiter"},
                {"entity": "offer", "prices": {"rawValue": "1003", "value": "1303"}},
                {"entity": "offer", "prices": {"rawValue": "1004", "value": "1304"}},
                {"entity": "offer", "prices": {"rawValue": "1020", "value": "1320"}},
                {"entity": "offer", "prices": {"rawValue": "1030", "value": "1330"}},
                {"entity": "offer", "prices": {"rawValue": "1043", "value": "1343"}},
                {"entity": "offer", "prices": {"rawValue": "1093", "value": "1393"}},
                {"entity": "offer", "prices": {"rawValue": "1600", "value": "1900"}},
            ],
            preserve_order=True,
        )

    def test_prime_aprice_hid_with_delivery_sorting(self):
        """
        place=prime&how=aprice&hid=special_hid&deliveryincluded=1 -
        сортировка по price asc, в цену включена стоимость доставки, под
        региональной чертой офферы так же отсортированы верно
        """
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        response = self.report.request_json(
            'place=prime&how=aprice&hid=3&rids=229&numdoc=20' '&deliveryincluded=1' + unified_off_flags
        )
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "prices": {"rawValue": "1004", "value": "1025"}},
                {"entity": "offer", "prices": {"rawValue": "1010", "value": "1055"}},
                {"entity": "offer", "prices": {"rawValue": "1001", "value": "1171"}},
                {"entity": "regionalDelimiter"},
                {"entity": "offer", "prices": {"rawValue": "1003", "value": "1303"}},
                {"entity": "offer", "prices": {"rawValue": "1004", "value": "1304"}},
                {"entity": "offer", "prices": {"rawValue": "1020", "value": "1320"}},
                {"entity": "offer", "prices": {"rawValue": "1030", "value": "1330"}},
                {"entity": "offer", "prices": {"rawValue": "1043", "value": "1343"}},
                {"entity": "offer", "prices": {"rawValue": "1093", "value": "1393"}},
                {"entity": "offer", "prices": {"rawValue": "1600", "value": "1900"}},
            ],
            preserve_order=True,
        )

    def test_prime_default_text_or_hid_sorting(self):
        """
        Запрос с текстом или запрос по определенному hid
        сортировка по formula_value*bid*10^5 desc,
        под региональной чертой офферы отсортированы по тому же принципу
        """

        expected = [
            {
                "entity": "offer",
                "prices": {"value": "1010"},
                "debug": {"formulaValue": Contains("0.353"), "sale": {"bid": 10}},
            },
            {
                "entity": "offer",
                "prices": {"value": "1001"},
                "debug": {"formulaValue": Contains("0.344"), "sale": {"bid": 13}},
            },
            {
                "entity": "offer",
                "prices": {"value": "1004"},
                "debug": {"formulaValue": Contains("0.326"), "sale": {"bid": 80}},
            },
            {"entity": "regionalDelimiter"},
            {
                "entity": "offer",
                "prices": {"value": "1600"},
                "debug": {"formulaValue": Contains("0.346"), "sale": {"bid": 11}},
            },
            {
                "entity": "offer",
                "prices": {"value": "1004"},
                "debug": {"formulaValue": Contains("0.340"), "sale": {"bid": 21}},
            },
            {
                "entity": "offer",
                "prices": {"value": "1043"},
                "debug": {"formulaValue": Contains("0.340"), "sale": {"bid": 20}},
            },
            {
                "entity": "offer",
                "prices": {"value": "1003"},
                "debug": {"formulaValue": Contains("0.336"), "sale": {"bid": 30}},
            },
            {
                "entity": "offer",
                "prices": {"value": "1020"},
                "debug": {"formulaValue": Contains("0.333"), "sale": {"bid": 50}},
            },
            {
                "entity": "offer",
                "prices": {"value": "1093"},
                "debug": {"formulaValue": Contains("0.326"), "sale": {"bid": 80}},  # ставки отключили
            },
            {
                "entity": "offer",
                "prices": {"value": "1030"},
                "debug": {"formulaValue": Contains("0.326"), "sale": {"bid": 90}},  # ставки отключили
            },
        ]

        response = self.report.request_json('place=prime&text=oldplace&rids=229&numdoc=20&debug=da')
        self.assertFragmentIn(response, expected, preserve_order=True)

        response = self.report.request_json('place=prime&hid=3&rids=229&numdoc=20&debug=da')
        self.assertFragmentIn(response, expected, preserve_order=True)

    def _test_factors_calc_on_sorting(self, aux_req, how, has_factors):
        """Проверим, что без how= формула вычисляется"""
        response = self.report.request_json('place=prime&rids=229&debug=da{aux}'.format(aux=aux_req))
        self.assertFragmentIn(response, {"search": {"total": 12, "totalOffers": 10}})
        doc_with_factors = {
            "entity": "offer",
            "debug": {
                "factors": NotEmpty(),
                "formulaValue": NotEmpty(),
            },
        }
        self.assertFragmentIn(response, doc_with_factors)

        """ ... а с сортировкой может и не вычисляться ..."""
        response = self.report.request_json('place=prime&rids=229&{how}&debug=da{aux}'.format(how=how, aux=aux_req))
        if has_factors:
            self.assertFragmentIn(response, doc_with_factors)
        else:
            self.assertFragmentIn(response, {"search": {"total": 12, "totalOffers": 10}})
            self.assertFragmentNotIn(response, {"debug": {"factors": NotEmpty()}})
            self.assertFragmentNotIn(response, {"debug": {"formulaValue": NotEmpty()}})

    def test_has_factors_calc_on_how_random_text(self):
        """На текстовых запросах нам нужно фильтровать по релевантности поэтому факторы надо считать"""
        return self._test_factors_calc_on_sorting("&text=oldplace", "&how=random", has_factors=True)

    def test_no_factors_calc_on_how_random_hid(self):
        """На бестексте на сортировках не нуждающихся в релевантности факторы не считаются, т.к. ничего не фильтруем"""
        self._test_factors_calc_on_sorting("&hid=3", "&how=random", has_factors=False)

    def test_has_factors_calc_on_how_price_hid(self):
        """На бестексте на сортировках нуждающихся в релевантности факторы считаются, т.к. нужна релевантность"""
        self._test_factors_calc_on_sorting("&hid=3", "&how=aprice", has_factors=True)
        self._test_factors_calc_on_sorting("&hid=3", "&how=dprice", has_factors=True)
        self._test_factors_calc_on_sorting("&hid=3", "&how=bestseller", has_factors=True)
        self._test_factors_calc_on_sorting("&hid=3", "&how=quality", has_factors=True)

    @classmethod
    def prepare_documents_for_promo_quality_sorting(cls):
        """
        Создаем 3 офферв с разной ценой и у разных магазинов
        Цены в магазинах обратно пропорциональны рейтингу
        Проверяем что сортировка по цене дает сортировку по цене,
        а сортировка по качеству промо - сортировку по рейтингу магазина (внезапно)
        """
        cls.index.shops += [
            Shop(fesh=2385001, new_shop_rating=NewShopRating(new_rating=1)),
            Shop(fesh=2385002, new_shop_rating=NewShopRating(new_rating=3)),
            Shop(fesh=2385003, new_shop_rating=NewShopRating(new_rating=5)),
        ]

        cls.index.offers += [
            Offer(title='title_2385001', hid=2385001, fesh=2385001, price=3000),
            Offer(title='title_2385002', hid=2385001, fesh=2385002, price=2000),
            Offer(title='title_2385003', hid=2385001, fesh=2385003, price=1000),
        ]

    def test_promo_quality_sort(self):
        response = self.report.request_json("place=prime&hid=2385001&how=aprice")

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "title_2385003"}},
                    {"titles": {"raw": "title_2385002"}},
                    {"titles": {"raw": "title_2385001"}},
                ]
            },
        )

        response = self.report.request_json("place=prime&hid=2385001&how=promo_quality")

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "title_2385001"}},
                    {"titles": {"raw": "title_2385002"}},
                    {"titles": {"raw": "title_2385003"}},
                ]
            },
        )

    @classmethod
    def prepare_reasons_to_buy(cls):

        cls.index.models += [
            Model(hid=178349, hyperid=17834901, randx=1),
            Model(hid=178349, hyperid=17834902, randx=2),
            Model(hid=178349, hyperid=17834903, randx=3),
            Model(hid=178349, hyperid=17834904, randx=4),
            Model(hid=178349, hyperid=17834905, randx=5),
        ]

        cls.index.mskus += [
            MarketSku(hyperid=17834901, sku=17834901, blue_offers=[BlueOffer()]),
            MarketSku(hyperid=17834902, sku=17834902, blue_offers=[BlueOffer()]),
            MarketSku(hyperid=17834903, sku=17834903, blue_offers=[BlueOffer()]),
            MarketSku(hyperid=17834904, sku=17834904, blue_offers=[BlueOffer()]),
            MarketSku(hyperid=17834905, sku=17834905, blue_offers=[BlueOffer()]),
        ]

        def make_reasons(*values):
            return [
                {
                    "id": "best_by_factor",
                    "type": "consumerFactor",
                    "factor_id": str(i + 1),
                    "factor_priority": str(i + 1),
                    "factor_name": "factor_{}".format(i + 1),
                    "value": value,
                }
                for i, value in enumerate(values)
                if value is not None
            ]

        cls.index.yamarec_places += [
            YamarecPlaceReasonsToBuy()
            .new_partition()
            .add(17834901, make_reasons(0.85, 0.93, 0.71, 0.44))
            .add(17834902, make_reasons(None, 0.32, 0.94, None))
            .add(17834903, make_reasons(0.94, 0.45, 0.34, 0.35))
            .add(17834904, make_reasons(0.30, 0.89, 0.21, None)),
            YamarecPlaceReasonsToBuy(blue=1)
            .new_partition()
            .add(17834901, make_reasons(0.12, 0.23))
            .add(17834902, make_reasons(0.13, 0.22))
            .add(17834903, make_reasons(0.17, 0.21))
            .add(17834904, make_reasons(0.11, 0.43)),
        ]

    def test_sorting_by_best_factor(self):
        """Проверяем что сортировка по &how=best_by_factor:n сортирует по фактору с factor_id=n
        и при этом в сортировки добавляется активный элемент [пользователям нравится factor_n]"""

        response = self.report.request_json('place=prime&hid=178349&how=best_by_factor:1&debug=da&allow-collapsing=1')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'id': 17834903},  # 0.94
                        {'id': 17834901},  # 0.85
                        {'id': 17834904},  # 0.30
                        {'id': 17834905},  # 0.0
                        {'id': 17834902},  # 0.0
                    ]
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json('place=prime&hid=178349&how=best_by_factor:2&debug=da&allow-collapsing=1')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'id': 17834901},  # 0.93
                        {'id': 17834904},  # 0.89
                        {'id': 17834903},  # 0.45
                        {'id': 17834902},  # 0.32
                        {'id': 17834905},  # 0.0
                    ]
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'id': 17834901,
                'debug': {
                    'rank': [
                        {'name': 'DELIVERY_TYPE'},
                        {'name': 'MODEL_TYPE'},
                        {'name': 'REASON_TO_BUY_FACTOR', 'value': "93"},
                        {'name': 'RANDX'},
                    ]
                },
            },
        )

        self.assertFragmentIn(
            response,
            {
                'sorts': [
                    {
                        'text': 'пользователям нравится factor_2',
                        'options': [{'id': 'best_by_factor:2', 'isActive': True}],
                    }
                ]
            },
        )

    @classmethod
    def prepare_emtpy_models_pessimization_on_discount_sort(cls):
        cls.index.offers += [
            Offer(hid=101, title="offer with {}% discount".format(85 - i), price=100, discount=85 - i)
            for i in range(60)
        ]
        cls.index.models += [
            Model(hid=101, hyperid=171, title="empty model with randx=1", randx=1),
            Model(hid=101, hyperid=172, title="empty model with randx=2", randx=2),
            Model(hid=101, hyperid=174, title="not empty model with 10% discount"),
            Model(hid=101, hyperid=175, title="not empty model with 8% discount"),
        ]
        cls.index.offers += [
            Offer(hyperid=174, title="model 174 offer with 10% discount", price=100, discount=10),
            Offer(hyperid=175, title="model 175 offer with 8% discount", price=100, discount=8),
        ]

    def test_empty_models_pessimization_on_discount_sort(self):
        """
        В индексе:
        - 60 офферов со скидками от 26 до 85
        - 2 модели без офферов
        - 2 непустых модели со скидками 8 и 10
        Ожидается:
        - Пустые модели будут в конце списка из-за пессимизации пустых моделей
        - Между собой модели будут отсортированны по убыванию скидки

        Чтобы инф-я о скидках не перетёрлась после запроса за модельными статистиками
        перед 2м этопом переранжирования:
        - Я создаю 60 офферов, которые составят топ60 документов после 1го этапа переранжирования
          для запроса за обновлёнными статистиками;
          На первом этапе переранжирования, модели окажутся в конце списка из-за той же
          пессимизации пустых моделей
        - Устанавливаю numdoc < 60, чтобы не переопредлить кол-во доков для перезапроса статистик
        """
        # Запрашиваю 4 страницу по 19 доков(с 58 по 67), чтобы захватить модели и последние офферы из top60.
        # allow-collapsing не указан, потому что для сортировки по скидке он всё равно отключается.
        # Поэтому в выдаче есть и модели, и привязанные к ним офферы.
        for flag in [None, 0, 1]:
            request = 'place=prime&hid=101&how=discount_p&numdoc=19&page=4'
            if flag is not None:
                request += '&rearr-factors=market_disable_blue_3p_discount_profitability_check={}'.format(flag)
            response = self.report.request_json(request)
            # Если флаг=1 или отсутствует в запросе, то офферы со скидкой >75% идут в самом конце выдачи, так
            # как скидки указанного размера не проходят валидацию на честность.
            # Если флаг=0, то невалидными признаются скидки >95%, но офферов с такими скидками в этом тесте нет, так что
            # в конце выдачи идут офферы с маленькими процентами скидки
            if flag in [None, 1]:
                self.assertFragmentIn(
                    response,
                    {
                        "total": 66,
                        "results": [
                            {"entity": "offer", "titles": {"raw": "offer with 79% discount"}},
                            {"entity": "offer", "titles": {"raw": "offer with 80% discount"}},
                            {"entity": "offer", "titles": {"raw": "offer with 81% discount"}},
                            {"entity": "offer", "titles": {"raw": "offer with 82% discount"}},
                            {"entity": "offer", "titles": {"raw": "offer with 83% discount"}},
                            {"entity": "offer", "titles": {"raw": "offer with 84% discount"}},
                            {"entity": "offer", "titles": {"raw": "offer with 85% discount"}},
                            {"id": 172, "titles": {"raw": "empty model with randx=2"}},
                            {"id": 171, "titles": {"raw": "empty model with randx=1"}},
                        ],
                    },
                    allow_different_len=False,
                    preserve_order=True,
                )
            else:
                self.assertFragmentIn(
                    response,
                    {
                        "total": 66,
                        "results": [
                            {"entity": "offer", "titles": {"raw": "offer with 28% discount"}},
                            {"entity": "offer", "titles": {"raw": "offer with 27% discount"}},
                            {"entity": "offer", "titles": {"raw": "offer with 26% discount"}},
                            {"id": 174, "titles": {"raw": "not empty model with 10% discount"}},
                            {"entity": "offer", "titles": {"raw": "model 174 offer with 10% discount"}},
                            {"id": 175, "titles": {"raw": "not empty model with 8% discount"}},
                            {"entity": "offer", "titles": {"raw": "model 175 offer with 8% discount"}},
                            {"id": 172, "titles": {"raw": "empty model with randx=2"}},
                            {"id": 171, "titles": {"raw": "empty model with randx=1"}},
                        ],
                    },
                    allow_different_len=False,
                    preserve_order=True,
                )

    @classmethod
    def prepare_dynstat_top_pessimization(cls):
        cls.index.offers += [Offer(hid=201, title="offer with price=150", price=150) for _ in range(57)]
        cls.index.models += [
            Model(hid=201, hyperid=251, title="model with min_price=101 DO_price=302"),
            Model(hid=201, hyperid=252, title="model with min_price=102 DO_price=303"),
            Model(hid=201, hyperid=253, title="model with min_price=103 DO_price=301"),
            Model(hid=201, hyperid=254, title="model with min_price=201"),
        ]
        cls.index.offers += [
            Offer(hyperid=251, price=101),
            Offer(hyperid=251, price=302, ts=201),
            Offer(hyperid=252, price=102),
            Offer(hyperid=252, price=303, ts=201),
            Offer(hyperid=253, price=103),
            Offer(hyperid=253, price=301, ts=201),
            Offer(hyperid=254, price=201),
            Offer(hid=201, title="offer with price=200", price=200),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 201).respond(1)

    def test_dynstat_top_pessimization(self):
        """
        Проверяем, что модели, попавшие в top60 для запроса за дин. статистиками, останутся в top60
        Модели 251, 252, 253 на первом этапе переранжирования сортируются по min_price и попадают
        в top60, и для них запрашиваются дин. статитсики.
        После пересортировки с дин. статистиками модели съезжают вниз, но не опускаются ниже top60
        """
        response = self.report.request_json(
            'place=prime&hid=201&use-default-offers=1&allow-collapsing=1&how=aprice&numdoc=56&page=2'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 62,
                    "results": [
                        {"entity": "offer", "titles": {"raw": "offer with price=150"}},
                        {"id": 253, "titles": {"raw": "model with min_price=103 DO_price=301"}},
                        {"id": 251, "titles": {"raw": "model with min_price=101 DO_price=302"}},
                        {"id": 252, "titles": {"raw": "model with min_price=102 DO_price=303"}},
                        # ----- top60 end -----
                        {"entity": "offer", "titles": {"raw": "offer with price=200"}},
                        {"id": 254, "titles": {"raw": "model with min_price=201"}},
                    ],
                }
            },
        )

    @classmethod
    def prepare_doc_rel_sorting(cls):

        cls.index.offers += [
            Offer(title='Валенки валенки ой да не подшиты стареньки'),
            Offer(title='Нельзя валенки носить не в чем к милому ходить'),
            Offer(title='Ой ты Коля Николай сиди дома не гуляй'),
            Offer(title='Не ходи на тот конец не носи девкам колец'),
            Offer(title='Чем подарочки носить лучше б валенки подшить'),
        ]

    def test_doc_rel_sorting(self):
        """Для отладки ранжирования пантеры"""
        response = self.report.request_json(
            'place=prime&text=Валенки валенки подшитые в подарок милому Николаю&how=doc_rel&debug=da'
        )

        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'titles': {'raw': 'Нельзя валенки носить не в чем к милому ходить'},
                            'debug': {'rank': [{'name': 'DOC_REL', 'value': '30717'}]},
                        },
                        {
                            'titles': {'raw': 'Валенки валенки ой да не подшиты стареньки'},
                            'debug': {'rank': [{'name': 'DOC_REL', 'value': '21909'}]},
                        },
                        {
                            'titles': {'raw': 'Чем подарочки носить лучше б валенки подшить'},
                            'debug': {'rank': [{'name': 'DOC_REL', 'value': '19985'}]},
                        },
                        {
                            'titles': {'raw': 'Ой ты Коля Николай сиди дома не гуляй'},
                            'debug': {'rank': [{'name': 'DOC_REL', 'value': '9919'}]},
                        },
                    ]
                }
            },
            preserve_order=True,
        )

        response = self.report.request_json(
            'place=prime&text=Валенки валенки подшитые в подарок милому Николаю&how=doc_rel_desc&debug=da'
        )

        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'titles': {'raw': 'Ой ты Коля Николай сиди дома не гуляй'},
                            'debug': {'rank': [{'name': 'DOC_REL', 'value': '9919'}]},
                        },
                        {
                            'titles': {'raw': 'Чем подарочки носить лучше б валенки подшить'},
                            'debug': {'rank': [{'name': 'DOC_REL', 'value': '19985'}]},
                        },
                        {
                            'titles': {'raw': 'Валенки валенки ой да не подшиты стареньки'},
                            'debug': {'rank': [{'name': 'DOC_REL', 'value': '21909'}]},
                        },
                        {
                            'titles': {'raw': 'Нельзя валенки носить не в чем к милому ходить'},
                            'debug': {'rank': [{'name': 'DOC_REL', 'value': '30717'}]},
                        },
                    ]
                }
            },
            preserve_order=True,
        )


if __name__ == '__main__':
    main()
