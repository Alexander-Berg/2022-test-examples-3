#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.testcase import TestCase, main

from core.matcher import Absent
from core.types import (
    GLType,
    HyperCategory,
    NavCategory,
    Offer,
    Shop,
    Currency,
    Tax,
    Picture,
    MnPlace,
    FashionCategory,
    FashionPremiumBrand,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
)
from core.types.sku import MarketSku, BlueOffer
from core.types.autogen import b64url_md5
from core.types.picture import thumbnails_config
from itertools import count


CATEGORY_ROOT = 0
CATEGORY_CLOTHES = 7812062
CATEGORY_CLOTHES_MAIN = 7877999
CATEGORY_CLOTHES_SECOND = 7811879
CATEGORY_CLOTHES_SECOND_CHILD = 15625409
CATEGORY_CLOTHES_THIRD = 9999999
CATEGORY_CLOTHES_FOURTH = 7812879

CATEGORY_NO_CLOTHES = 280280
CATEGORY_NO_CLOTHES_SECOND = 14231177
CATEGORY_NO_CLOTHES_SECOND_CHILD = 14231372

CATEGORY_NO_HID_NID = 7889
CATEGORY_NO_HID_NID_SECOND = 8889

VENDOR_UNKNOWN = 0
PREMIUM_BRAND = 8523720


counter = count()


def get_offer_id(x):
    return 'offer_id_{}'.format(x)


def __blue_offer(price=1000, price_old=1000, is_fulfillment=True):

    num = next(counter)
    offer_id = next(counter)
    fesh = next(counter)
    feedid = next(counter)
    return BlueOffer(
        waremd5=b64url_md5(num),
        price=price,
        price_old=price_old,
        fesh=fesh,
        feedid=feedid,
        offerid=get_offer_id(offer_id),
        is_fulfillment=is_fulfillment,
    )


def __msku(offers, hid, vendor_id=0):
    num = next(counter)
    return MarketSku(
        sku=num, hyperid=num, hid=hid, blue_offers=offers if isinstance(offers, list) else [offers], vendor_id=vendor_id
    )


mskus = []


def create_offer(category, vendor_id):
    result = __blue_offer()
    mskus.append(
        __msku(
            [
                result,
            ],
            category,
            vendor_id,
        )
    )

    return result


fashion_and_premium_tests = [
    (True, True, create_offer(CATEGORY_CLOTHES, PREMIUM_BRAND)),
    (True, False, create_offer(CATEGORY_CLOTHES, VENDOR_UNKNOWN)),
    (True, True, create_offer(CATEGORY_CLOTHES_MAIN, PREMIUM_BRAND)),
    (True, False, create_offer(CATEGORY_CLOTHES_MAIN, VENDOR_UNKNOWN)),
    (False, False, create_offer(CATEGORY_ROOT, VENDOR_UNKNOWN)),
    (False, False, create_offer(CATEGORY_ROOT, PREMIUM_BRAND)),
    (False, False, create_offer(CATEGORY_NO_CLOTHES, VENDOR_UNKNOWN)),
    (False, False, create_offer(CATEGORY_NO_CLOTHES, PREMIUM_BRAND)),
]


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.mskus += mskus
        cls.settings.loyalty_enabled = True

        cls.index.fashion_categories += [
            FashionCategory("CATEGORY_CLOTHES", CATEGORY_CLOTHES),
            FashionCategory("CATEGORY_CLOTHES_MAIN", CATEGORY_CLOTHES_MAIN),
            FashionCategory("CATEGORY_CLOTHES_SECOND", CATEGORY_CLOTHES_SECOND),
            FashionCategory("CATEGORY_CLOTHES_SECOND_CHILD", CATEGORY_CLOTHES_SECOND_CHILD),
            FashionCategory("CATEGORY_CLOTHES_THIRD", CATEGORY_CLOTHES_THIRD),
            FashionCategory("CATEGORY_CLOTHES_FOURTH", CATEGORY_CLOTHES_FOURTH),
        ]
        cls.index.fashion_first_party_premium_brands += [FashionPremiumBrand("PREMIUM_BRAND", PREMIUM_BRAND)]
        cls.index.fashion_third_party_premium_brands += [FashionPremiumBrand("PREMIUM_BRAND", PREMIUM_BRAND)]

        cls.index.hypertree += [
            HyperCategory(
                hid=CATEGORY_CLOTHES_MAIN,
                children=[
                    HyperCategory(hid=CATEGORY_CLOTHES),
                    HyperCategory(
                        hid=CATEGORY_CLOTHES_SECOND, children=[HyperCategory(hid=CATEGORY_CLOTHES_SECOND_CHILD)]
                    ),
                ],
            ),
            HyperCategory(hid=CATEGORY_CLOTHES_THIRD),
            HyperCategory(hid=CATEGORY_NO_CLOTHES),
            HyperCategory(
                hid=CATEGORY_NO_CLOTHES_SECOND, children=[HyperCategory(hid=CATEGORY_NO_CLOTHES_SECOND_CHILD)]
            ),
            HyperCategory(hid=CATEGORY_CLOTHES_FOURTH),
        ]

        cls.index.navtree += [
            NavCategory(
                hid=0,
                nid=CATEGORY_NO_HID_NID,
                children=[NavCategory(hid=CATEGORY_CLOTHES_SECOND, nid=CATEGORY_CLOTHES_SECOND)],
            ),
            NavCategory(
                hid=0,
                nid=CATEGORY_NO_HID_NID_SECOND,
                children=[
                    NavCategory(hid=CATEGORY_NO_CLOTHES, nid=CATEGORY_NO_CLOTHES),
                    NavCategory(
                        hid=CATEGORY_CLOTHES_THIRD,
                        nid=CATEGORY_CLOTHES_THIRD,
                    ),
                ],
            ),
        ]

        cls.index.offers += [
            # The first intent is fashion, so isMostlyFashion = True when search "котик"
            Offer(hid=CATEGORY_CLOTHES_MAIN, title='просто котик'),
            Offer(hid=CATEGORY_CLOTHES, title='сверх котик'),
            Offer(hid=CATEGORY_NO_CLOTHES, title='корм для котик'),
            # The most similar to text to "попугайчик" is "C++ попугайчик",
            # so it doesn't matter that others are fashion, isMostlyFashion = False when search "попугайчик",
            # because the first intent is no fashion
            Offer(hid=CATEGORY_CLOTHES_MAIN, title='дизельный попугайчик'),
            Offer(hid=CATEGORY_CLOTHES_THIRD, title='скучный попугайчик'),
            Offer(hid=CATEGORY_NO_CLOTHES, title='с++ попугайчик'),
        ]

    def test_combo_is_fashion_and_is_premium_fashion(self):

        for is_fashion, is_premium, test_offer in fashion_and_premium_tests:
            request = 'place=prime&rids=0&regset=1&pp=18&offerid={}'.format(test_offer.waremd5)
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                [
                    {
                        'entity': 'offer',
                        'isFashion': is_fashion,
                        'isFashionPremium': is_premium,
                        'wareId': test_offer.waremd5,
                        'prices': {
                            'value': str(test_offer.price),
                            'currency': 'RUR',
                        },
                    }
                ],
                allow_different_len=False,
            )

    def test_is_mostly_fashion_false_when_first_intent_is_no_fashion(self):

        response = self.report.request_json('place=prime&text=попугайчик')
        self.assertFragmentIn(
            response,
            {
                "search": {"isMostlyFashion": False},
                "intents": [
                    {"category": {"hid": CATEGORY_NO_CLOTHES}},
                    {"category": {"hid": CATEGORY_CLOTHES_MAIN}},
                    {"category": {"hid": CATEGORY_CLOTHES_THIRD}},
                ],
            },
            allow_different_len=False,
        )

    def test_is_mostly_fashion_true_when_first_intent_is_fashion(self):

        response = self.report.request_json('place=prime&text=котик')
        self.assertFragmentIn(
            response,
            {
                "search": {"isMostlyFashion": True},
                "intents": [
                    {"category": {"hid": CATEGORY_CLOTHES_MAIN}, "intents": [{"category": {"hid": CATEGORY_CLOTHES}}]},
                    {"category": {"hid": CATEGORY_NO_CLOTHES}},
                ],
            },
            allow_different_len=False,
        )

    def test_is_mostly_fashion_true_when_first_intent_has_no_hid(self):
        """
        Проверяем, что при наличии первой категории фэшн, после отсутствия hid получим isMostlyFashion True
        """
        response = self.report.request_json('place=prime&nid={}'.format(CATEGORY_NO_HID_NID))
        self.assertFragmentIn(
            response,
            {
                "search": {"isMostlyFashion": True},
                "intents": [
                    {"category": {"hid": 0}, "intents": [{"category": {"hid": CATEGORY_CLOTHES_SECOND}}]},
                ],
            },
            allow_different_len=False,
        )

    def test_is_mostly_fashion_false_when_first_intent_has_no_hid(self):
        """
        Проверяем, что при наличии первой категории не фэшн, после отсутствия hid получим isMostlyFashion False
        """
        response = self.report.request_json('place=prime&nid={}'.format(CATEGORY_NO_HID_NID_SECOND))
        self.assertFragmentIn(
            response,
            {
                "search": {"isMostlyFashion": False},
                "intents": [
                    {
                        "category": {"hid": 0},
                        "intents": [
                            {"category": {"hid": CATEGORY_NO_CLOTHES}},
                            {"category": {"hid": CATEGORY_CLOTHES_THIRD}},
                        ],
                    },
                ],
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_fashion_groupings(cls):
        cls.index.shops += [
            Shop(
                fesh=431782,
                datafeed_id=431782,
                priority_region=213,
                name='Яндекс.Маркет',
                currency=Currency.RUR,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                tax_system=Tax.OSN,
                cpa=Shop.CPA_REAL,
                blue=Shop.BLUE_REAL,
            ),
            Shop(
                fesh=465852,
                datafeed_id=465852,
                priority_region=213,
                name='Яндекс.Маркет BAON',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                fulfillment_program=True,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=465853,
                datafeed_id=465853,
                priority_region=213,
                name='Яндекс.Маркет Finn Flare',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                fulfillment_program=True,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=465854,
                datafeed_id=465854,
                priority_region=213,
                name='Яндекс.Маркет TBOE',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                fulfillment_program=True,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=465855,
                datafeed_id=465855,
                priority_region=213,
                name='Яндекс.Маркет',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                fulfillment_program=True,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
            ),
            Shop(fesh=4322401, cpa=Shop.CPA_REAL, priority_region=213),
            Shop(fesh=4322402, cpa=Shop.CPA_NO, priority_region=213),
        ]

        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=213),
            DynamicWarehouseToWarehouseInfo(warehouse_from=145, warehouse_to=145),
        ]

        for (seq, suffix) in [(1, 'мужская'), (2, 'женская'), (3, 'детская'), (4, 'унисекс')]:
            cls.index.mskus += [
                MarketSku(
                    title='Куртка Baon {}'.format(suffix),
                    hyperid=4322400 + seq,
                    sku=43224000 + seq,
                    hid=CATEGORY_CLOTHES_SECOND,
                    vendor_id=43224100,
                    blue_offers=[
                        BlueOffer(
                            feedid=465852,
                            title='Куртка Baon {}'.format(suffix),
                            hid=CATEGORY_CLOTHES_SECOND,
                            ts=4322400 + seq,
                            vendor_id=43224100,
                            picture=Picture(
                                picture_id='Iy0{}nHslqLtqZJLygVAHe1'.format(seq),
                                width=200,
                                height=200,
                                thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
                            ),
                            waremd5='Sku0{}-dDXWsIiLVm1goleg'.format(seq),
                        )
                    ],
                ),
                MarketSku(
                    title='Куртка Finn Flare {}'.format(suffix),
                    hyperid=4322410 + seq,
                    sku=43224010 + seq,
                    hid=CATEGORY_CLOTHES_SECOND,
                    vendor_id=43224200,
                    blue_offers=[
                        BlueOffer(
                            feedid=465853,
                            title='Куртка Finn Flare {}'.format(suffix),
                            hid=CATEGORY_CLOTHES_SECOND,
                            ts=4322410 + seq,
                            vendor_id=43224200,
                            picture=Picture(
                                picture_id='Iy1{}nHslqLtqZJLygVAHe1'.format(seq),
                                width=200,
                                height=200,
                                thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
                            ),
                            waremd5='Sku1{}-dDXWsIiLVm1goleg'.format(seq),
                        )
                    ],
                ),
                MarketSku(
                    title='Куртка TBOE {}'.format(suffix),
                    hyperid=4322420 + seq,
                    sku=43224020 + seq,
                    hid=CATEGORY_CLOTHES_SECOND,
                    vendor_id=43224300,
                    blue_offers=[
                        BlueOffer(
                            feedid=465854,
                            title='Куртка TBOE {}'.format(suffix),
                            hid=CATEGORY_CLOTHES_SECOND,
                            ts=4322420 + seq,
                            vendor_id=43224300,
                            picture=Picture(
                                picture_id='Iy2{}nHslqLtqZJLygVAHe1'.format(seq),
                                width=200,
                                height=200,
                                thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
                            ),
                            waremd5='Sku2{}-dDXWsIiLVm1goleg'.format(seq),
                        ),
                    ],
                ),
            ]

        cls.index.offers += [
            Offer(
                hid=CATEGORY_CLOTHES_SECOND,
                title='просто куртка CPA',
                fesh=4322401,
                ts=4322451,
                vendor_id=43224400,
                picture=Picture(
                    picture_id='Iy31nHslqLtqZJLygVAHe1',
                    width=200,
                    height=200,
                    thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
                ),
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                hid=CATEGORY_CLOTHES_SECOND,
                title='просто куртка',
                fesh=4322402,
                ts=4322452,
                vendor_id=43224400,
                picture=Picture(
                    picture_id='Iy32nHslqLtqZJLygVAHe1',
                    width=200,
                    height=200,
                    thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
                ),
            ),
        ]

        for seq in range(0, 99):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4322400 + seq).respond(0.9 - seq * 0.001)

    def test_suppliers_grouping(self):

        # Проверяем выдачу по дефолту - все 1P офферы идут как 1 магазин
        rearr_factors = [
            'market_max_offers_per_shop_count=3',
            'market_new_cpm_iterator=0',
            'market_max_offers_per_brand=0',
        ]
        request = 'place=prime&text=куртка&rids=213&numdoc=20&rearr-factors={}&hid=7877999'.format(
            ';'.join(rearr_factors)
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'titles': {'raw': 'Куртка Baon мужская'}},
                    {'titles': {'raw': 'Куртка Baon женская'}},
                    {'titles': {'raw': 'Куртка Baon детская'}},
                    {'titles': {'raw': 'просто куртка CPA'}},
                    {'titles': {'raw': 'просто куртка'}},
                    {'titles': {'raw': 'Куртка Baon унисекс'}},
                    {'titles': {'raw': 'Куртка Finn Flare мужская'}},
                    {'titles': {'raw': 'Куртка Finn Flare женская'}},
                    {'titles': {'raw': 'Куртка Finn Flare детская'}},
                    {'titles': {'raw': 'Куртка Finn Flare унисекс'}},
                    {'titles': {'raw': 'Куртка TBOE мужская'}},
                    {'titles': {'raw': 'Куртка TBOE женская'}},
                    {'titles': {'raw': 'Куртка TBOE детская'}},
                    {'titles': {'raw': 'Куртка TBOE унисекс'}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Проверяем выдачу с группировкой по поставщику, с ограничением на товары одного бренда
        rearr_factors = ['market_max_offers_per_brand=3', 'market_new_cpm_iterator=0']
        request = 'place=prime&text=куртка&rids=213&numdoc=20&rearr-factors={}&hid=7877999'.format(
            ';'.join(rearr_factors)
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'titles': {'raw': 'Куртка Baon мужская'}},
                    {'titles': {'raw': 'Куртка Baon женская'}},
                    {'titles': {'raw': 'Куртка Baon детская'}},
                    {'titles': {'raw': 'Куртка Finn Flare мужская'}},
                    {'titles': {'raw': 'Куртка Finn Flare женская'}},
                    {'titles': {'raw': 'Куртка Finn Flare детская'}},
                    {'titles': {'raw': 'Куртка TBOE мужская'}},
                    {'titles': {'raw': 'Куртка TBOE женская'}},
                    {'titles': {'raw': 'Куртка TBOE детская'}},
                    {'titles': {'raw': 'просто куртка CPA'}},
                    {'titles': {'raw': 'просто куртка'}},
                    {'titles': {'raw': 'Куртка Baon унисекс'}},
                    {'titles': {'raw': 'Куртка Finn Flare унисекс'}},
                    {'titles': {'raw': 'Куртка TBOE унисекс'}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # с новым cpm-итераторм ограничение на максимальное количество офферов от одного бренда/магазина не работает
        rearr_factors = ['market_max_offers_per_brand=3', 'market_new_cpm_iterator=4']
        request = 'place=prime&text=куртка&rids=213&numdoc=20&rearr-factors={}&hid=7877999'.format(
            ';'.join(rearr_factors)
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'titles': {'raw': 'Куртка Baon мужская'}},
                    {'titles': {'raw': 'Куртка Baon женская'}},
                    {'titles': {'raw': 'Куртка Baon детская'}},
                    {'titles': {'raw': 'Куртка Baon унисекс'}},
                    {'titles': {'raw': 'Куртка Finn Flare мужская'}},
                    {'titles': {'raw': 'Куртка Finn Flare женская'}},
                    {'titles': {'raw': 'Куртка Finn Flare детская'}},
                    {'titles': {'raw': 'Куртка Finn Flare унисекс'}},
                    {'titles': {'raw': 'Куртка TBOE мужская'}},
                    {'titles': {'raw': 'Куртка TBOE женская'}},
                    {'titles': {'raw': 'Куртка TBOE детская'}},
                    {'titles': {'raw': 'Куртка TBOE унисекс'}},
                    {'titles': {'raw': 'просто куртка CPA'}},
                    {'titles': {'raw': 'просто куртка'}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_fashion_supplier_groupings(cls):
        for (seq, suffix) in [(1, 'мужская'), (2, 'женская'), (3, 'детская'), (4, 'унисекс')]:
            cls.index.mskus += [
                MarketSku(
                    title='Шапка Baon {}'.format(suffix),
                    hyperid=4347500 + seq,
                    sku=43475000 + seq,
                    hid=CATEGORY_CLOTHES,
                    vendor_id=43475100,
                    blue_offers=[
                        BlueOffer(
                            feedid=465852,
                            title='Шапка Baon {}'.format(suffix),
                            hid=CATEGORY_CLOTHES,
                            ts=4347500 + seq,
                            vendor_id=43475100,
                            picture=Picture(
                                picture_id='Iy0{}nHslqLtqZJLygVAHe1'.format(seq),
                                width=200,
                                height=200,
                                thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
                            ),
                            waremd5='Sk10{}-dDXWsIiLVm1goleg'.format(seq),
                        )
                    ],
                ),
                MarketSku(
                    title='Шапка Finn Flare {}'.format(suffix),
                    hyperid=4347510 + seq,
                    sku=43475010 + seq,
                    hid=CATEGORY_CLOTHES,
                    vendor_id=43475200,
                    blue_offers=[
                        BlueOffer(
                            feedid=465853,
                            title='Шапка Finn Flare {}'.format(suffix),
                            hid=CATEGORY_CLOTHES,
                            ts=4347510 + seq,
                            vendor_id=43475200,
                            picture=Picture(
                                picture_id='Iy1{}nHslqLtqZJLygVAHe1'.format(seq),
                                width=200,
                                height=200,
                                thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
                            ),
                            waremd5='Sk11{}-dDXWsIiLVm1goleg'.format(seq),
                        )
                    ],
                ),
            ]

        for (seq, suffix) in [(1, 'большая'), (2, 'средняя'), (3, 'маленькая'), (4, 'игрушечная')]:
            cls.index.mskus += [
                MarketSku(
                    title='Кружка "шапка" {}'.format(suffix),
                    hyperid=4347580 + seq,
                    sku=43475080 + seq,
                    hid=CATEGORY_NO_CLOTHES,
                    vendor_id=43475300,
                    blue_offers=[
                        BlueOffer(
                            feedid=465854,
                            title='Кружка "шапка" {}'.format(suffix),
                            hid=CATEGORY_NO_CLOTHES,
                            ts=4347520 + seq,
                            vendor_id=43475500,
                            picture=Picture(
                                picture_id='Iy2{}nHslqLtqZJLygVAHe1'.format(seq),
                                width=200,
                                height=200,
                                thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
                            ),
                            waremd5='Sk12{}-dDXWsIiLVm1goleg'.format(seq),
                        ),
                    ],
                ),
            ]

        cls.index.offers += [
            Offer(
                hid=CATEGORY_CLOTHES,
                title='просто шапка CPA',
                fesh=4322401,
                ts=4347551,
                vendor_id=43475400,
                picture=Picture(
                    picture_id='Iy31nHslqLtqZJLygVAHe1',
                    width=200,
                    height=200,
                    thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
                ),
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                hid=CATEGORY_CLOTHES,
                title='просто шапка',
                fesh=4322402,
                ts=4322452,
                vendor_id=43475400,
                picture=Picture(
                    picture_id='Iy32nHslqLtqZJLygVAHe1',
                    width=200,
                    height=200,
                    thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
                ),
            ),
            Offer(
                hid=CATEGORY_NO_CLOTHES,
                title='просто кружка "шапка" CPA',
                fesh=4322401,
                ts=4347591,
                vendor_id=43475600,
                picture=Picture(
                    picture_id='Iy41nHslqLtqZJLygVAHe1',
                    width=200,
                    height=200,
                    thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
                ),
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                hid=CATEGORY_NO_CLOTHES,
                title='просто кружка "шапка"',
                fesh=4322402,
                ts=4322492,
                vendor_id=43475600,
                picture=Picture(
                    picture_id='Iy42nHslqLtqZJLygVAHe1',
                    width=200,
                    height=200,
                    thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
                ),
            ),
        ]

        for seq in range(0, 99):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4347500 + seq).respond(0.9 - seq * 0.001)

    def test_suppliers_grouping_on_search(self):

        # Проверяем выдачу по дефолту - все 1P офферы идут как 1 магазин
        rearr_factors = [
            'market_max_offers_per_shop_count=3',
            'market_new_cpm_iterator=0',
            'market_use_supplier_grouping_in_fashion_in_search=0',
        ]
        request = 'place=prime&text=шапка&rids=213&numdoc=20&rearr-factors={}'.format(';'.join(rearr_factors))
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    # Head
                    {'titles': {'raw': 'Шапка Baon мужская'}},
                    {'titles': {'raw': 'Шапка Baon женская'}},
                    {'titles': {'raw': 'Шапка Baon детская'}},
                    {'titles': {'raw': 'просто шапка CPA'}},
                    {'titles': {'raw': 'просто шапка'}},
                    {'titles': {'raw': 'просто кружка "шапка" CPA'}},
                    {'titles': {'raw': 'просто кружка "шапка"'}},
                    # Tail
                    {'titles': {'raw': 'Шапка Baon унисекс'}},
                    {'titles': {'raw': 'Шапка Finn Flare мужская'}},
                    {'titles': {'raw': 'Шапка Finn Flare женская'}},
                    {'titles': {'raw': 'Шапка Finn Flare детская'}},
                    {'titles': {'raw': 'Шапка Finn Flare унисекс'}},
                    {'titles': {'raw': 'Кружка "шапка" большая'}},
                    {'titles': {'raw': 'Кружка "шапка" средняя'}},
                    {'titles': {'raw': 'Кружка "шапка" маленькая'}},
                    {'titles': {'raw': 'Кружка "шапка" игрушечная'}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Проверяем выдачу с группировкой по поставщику, при этом группируются по поставщику
        # только товары в категориях fashion
        rearr_factors = [
            'market_max_offers_per_shop_count=3',
            'market_new_cpm_iterator=0',
            'market_use_supplier_grouping_in_fashion_in_search=1',
        ]
        request = 'place=prime&text=шапка&rids=213&numdoc=20&rearr-factors={}'.format(';'.join(rearr_factors))
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    # Head
                    {'titles': {'raw': 'Шапка Baon мужская'}},
                    {'titles': {'raw': 'Шапка Baon женская'}},
                    {'titles': {'raw': 'Шапка Baon детская'}},
                    {'titles': {'raw': 'Шапка Finn Flare мужская'}},
                    {'titles': {'raw': 'Шапка Finn Flare женская'}},
                    {'titles': {'raw': 'Шапка Finn Flare детская'}},
                    {'titles': {'raw': 'просто шапка CPA'}},
                    {'titles': {'raw': 'просто шапка'}},
                    {'titles': {'raw': 'просто кружка "шапка" CPA'}},
                    {'titles': {'raw': 'просто кружка "шапка"'}},
                    # Tail
                    {'titles': {'raw': 'Шапка Baon унисекс'}},
                    {'titles': {'raw': 'Шапка Finn Flare унисекс'}},
                    {'titles': {'raw': 'Кружка "шапка" большая'}},
                    {'titles': {'raw': 'Кружка "шапка" средняя'}},
                    {'titles': {'raw': 'Кружка "шапка" маленькая'}},
                    {'titles': {'raw': 'Кружка "шапка" игрушечная'}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_suppliers_grouping_on_search_new_cpm_iterator(self):

        # Проверяем выдачу по дефолту - все 1P офферы идут как 1 магазин
        rearr_factors = [
            'market_max_offers_per_shop_count=3',
            'market_new_cpm_iterator=4',
            'market_use_supplier_grouping_in_fashion_in_search=0',
        ]
        request = 'place=prime&text=шапка&rids=213&numdoc=20&rearr-factors={}'.format(';'.join(rearr_factors))
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'titles': {'raw': 'Шапка Baon мужская'}},
                    {'titles': {'raw': 'Шапка Baon женская'}},
                    {'titles': {'raw': 'Шапка Baon детская'}},
                    {'titles': {'raw': 'Шапка Baon унисекс'}},
                    {'titles': {'raw': 'Шапка Finn Flare мужская'}},
                    {'titles': {'raw': 'Шапка Finn Flare женская'}},
                    {'titles': {'raw': 'Шапка Finn Flare детская'}},
                    {'titles': {'raw': 'Шапка Finn Flare унисекс'}},
                    {'titles': {'raw': 'Кружка "шапка" большая'}},
                    {'titles': {'raw': 'Кружка "шапка" средняя'}},
                    {'titles': {'raw': 'Кружка "шапка" маленькая'}},
                    {'titles': {'raw': 'Кружка "шапка" игрушечная'}},
                    {'titles': {'raw': 'просто шапка CPA'}},
                    {'titles': {'raw': 'просто шапка'}},
                    {'titles': {'raw': 'просто кружка "шапка" CPA'}},
                    {'titles': {'raw': 'просто кружка "шапка"'}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Проверяем выдачу с группировкой по поставщику, при этом группируются по поставщику
        # только товары в категориях fashion
        rearr_factors = [
            'market_max_offers_per_shop_count=3',
            'market_new_cpm_iterator=4',
            'market_use_supplier_grouping_in_fashion_in_search=1',
        ]
        request = 'place=prime&text=шапка&rids=213&numdoc=20&rearr-factors={}'.format(';'.join(rearr_factors))
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'titles': {'raw': 'Шапка Baon мужская'}},
                    {'titles': {'raw': 'Шапка Baon женская'}},
                    {'titles': {'raw': 'Шапка Baon детская'}},
                    {'titles': {'raw': 'Шапка Baon унисекс'}},
                    {'titles': {'raw': 'Шапка Finn Flare мужская'}},
                    {'titles': {'raw': 'Шапка Finn Flare женская'}},
                    {'titles': {'raw': 'Шапка Finn Flare детская'}},
                    {'titles': {'raw': 'Шапка Finn Flare унисекс'}},
                    {'titles': {'raw': 'Кружка "шапка" большая'}},
                    {'titles': {'raw': 'Кружка "шапка" средняя'}},
                    {'titles': {'raw': 'Кружка "шапка" маленькая'}},
                    {'titles': {'raw': 'Кружка "шапка" игрушечная'}},
                    {'titles': {'raw': 'просто шапка CPA'}},
                    {'titles': {'raw': 'просто шапка'}},
                    {'titles': {'raw': 'просто кружка "шапка" CPA'}},
                    {'titles': {'raw': 'просто кружка "шапка"'}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_visual_search_flags(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=54503, name="Стоительство и ремонт", children=[HyperCategory(hid=22401912, name="Смесители")]
            )
        ]
        cls.index.gltypes += [GLType(param_id=7893318, hid=CATEGORY_CLOTHES)]

    def test_visual_search_flags(self):
        rearr_factors_empty = []
        rearr_factors_disabled = ['market_visual_search_disabled=1']
        rearr_factors_visual_departments = ['market_visual_search_departments=%d' % CATEGORY_NO_CLOTHES_SECOND]
        rearr_factors_not_visual_departments = ['market_not_visual_search_departments=%d' % CATEGORY_NO_CLOTHES_SECOND]
        rearr_factors_add_after_product_card_return = ['market_visual_search_add_after_product_card_return=1']
        # визуальный поиск НЕ работает
        # 1) ВНЕ одежды
        # 2) с выставленным флагом market_visual_search_disabled
        # 3) внутри rearr_factors_not_visual_departments категорий (в них и их детях)
        for (hid, rearr) in [
            (CATEGORY_NO_CLOTHES, rearr_factors_empty),
            (CATEGORY_NO_CLOTHES, rearr_factors_disabled),
            (CATEGORY_CLOTHES_SECOND, rearr_factors_disabled),
            (CATEGORY_NO_CLOTHES_SECOND_CHILD, rearr_factors_not_visual_departments),
        ]:
            request = 'place=prime&hid={}&rids=213&numdoc=20&rearr-factors={}'.format(hid, ';'.join(rearr))
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "isVisualSearchEnabled": Absent(),
                    }
                },
            )
        # визуальный поиск РАБОТАЕТ
        # 1) с одеждой
        # 2) в перечисленных в rearr_factors_visual_departments категориях
        # 3) ВНЕ перечисленных в rearr_factors_not_visual_departments категориях
        for (hid, rearr) in [
            (CATEGORY_CLOTHES_SECOND, rearr_factors_empty),
            (CATEGORY_NO_CLOTHES_SECOND_CHILD, rearr_factors_visual_departments),
            (CATEGORY_NO_CLOTHES, rearr_factors_not_visual_departments),
        ]:
            request = 'place=prime&hid={}&rids=213&numdoc=20&rearr-factors={}'.format(hid, ';'.join(rearr))
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "isVisualSearchEnabled": True,
                    }
                },
            )
        # визуальный поиск НЕ работает если
        # 1) категорий больше 1
        # 2) выставлены хоть какие-то glfilter-ы
        # 3) выставлен признак Универмага (isUnivermag, в параметрах filter-univermag)
        for (hids, other_params) in [
            ([CATEGORY_CLOTHES, CATEGORY_CLOTHES_SECOND], ''),
            ([CATEGORY_CLOTHES], '&glfilter=7893318:8340189'),
            ([CATEGORY_CLOTHES], '&filter-univermag=1'),
        ]:
            request = 'place=prime&hid={}&rids=213&numdoc=20{}'.format(
                ','.join([str(hid) for hid in hids]), other_params
            )
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "isVisualSearchEnabled": Absent(),
                    }
                },
            )
        # https://st.yandex-team.ru/MARKETYA-754
        for (hid, rearr) in [
            (CATEGORY_NO_CLOTHES, []),
            (CATEGORY_NO_CLOTHES_SECOND_CHILD, rearr_factors_not_visual_departments),
        ]:
            rearr.extend(rearr_factors_add_after_product_card_return)
            request = 'place=prime&hid={}&rids=213&numdoc=20&rearr-factors={}'.format(hid, ';'.join(rearr))
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "isVisualSimilarAddingEnabled": Absent(),
                    }
                },
            )
        for (hid, rearr) in [
            (CATEGORY_CLOTHES_SECOND, []),
            (CATEGORY_NO_CLOTHES, rearr_factors_not_visual_departments),
        ]:
            rearr.extend(rearr_factors_add_after_product_card_return)
            request = 'place=prime&hid={}&rids=213&numdoc=20&rearr-factors={}'.format(hid, ';'.join(rearr))
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "isVisualSimilarAddingEnabled": True,
                    }
                },
            )

    @classmethod
    def prepare_fashion_groupings_for_collapsing(cls):
        # добавляем кучу товаров от маркета и от разных поставщиков,
        # чтоб они все попадали в виртуальную группировку по поставщикам и магазинам
        # а в группировку только по магазинам не поместились
        for i in range(20):
            for (seq, suffix) in [(1, 'мужская'), (2, 'женская'), (3, 'детская'), (4, 'унисекс')]:
                cls.index.mskus += [
                    MarketSku(
                        title='Юбка Baon {}'.format(suffix),
                        hyperid=532240 + i * 10 + seq,
                        sku=5322400 + i * 10 + seq,
                        hid=CATEGORY_CLOTHES_FOURTH,
                        vendor_id=53224100,
                        blue_offers=[
                            BlueOffer(
                                feedid=465852,
                                title='Юбка Baon {}'.format(suffix),
                                hid=CATEGORY_CLOTHES_FOURTH,
                                ts=532240 + i * 10 + seq,
                                vendor_id=43224100,
                                picture=Picture(
                                    picture_id='Iy0{}nHslqLtqZJLygVAHe1'.format(seq),
                                    width=200,
                                    height=200,
                                    thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
                                ),
                                waremd5='Sku0{}-dDXWsIiLV{}1gole{}'.format(seq, i % 10, i / 10),
                            )
                        ],
                    ),
                    MarketSku(
                        title='Юбка Finn Flare {}'.format(suffix),
                        hyperid=632241 + i * 10 + seq,
                        sku=6322401 + i * 10 + seq,
                        hid=CATEGORY_CLOTHES_FOURTH,
                        vendor_id=43224200,
                        blue_offers=[
                            BlueOffer(
                                feedid=465853,
                                title='Юбка Finn Flare {}'.format(suffix),
                                hid=CATEGORY_CLOTHES_FOURTH,
                                ts=632241 + i * 10 + seq,
                                vendor_id=43224200,
                                picture=Picture(
                                    picture_id='Iy1{}nHslqLtqZJLygVAHe1'.format(seq),
                                    width=200,
                                    height=200,
                                    thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
                                ),
                                waremd5='Sku1{}-dDXWsIiLV{}1gole{}'.format(seq, i % 10, i / 10),
                            )
                        ],
                    ),
                    MarketSku(
                        title='Юбка TBOE {}'.format(suffix),
                        hyperid=732242 + i * 10 + seq,
                        sku=7322402 + i * 10 + seq,
                        hid=CATEGORY_CLOTHES_FOURTH,
                        vendor_id=43224300,
                        blue_offers=[
                            BlueOffer(
                                feedid=465854,
                                title='Юбка TBOE {}'.format(suffix),
                                hid=CATEGORY_CLOTHES_FOURTH,
                                ts=732242 + i * 10 + seq,
                                vendor_id=43224300,
                                picture=Picture(
                                    picture_id='Iy2{}nHslqLtqZJLygVAHe1'.format(seq),
                                    width=200,
                                    height=200,
                                    thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
                                ),
                                waremd5='Sku2{}-dDXWsIiLV{}1gole{}'.format(seq, i % 10, i / 10),
                            ),
                        ],
                    ),
                ]
                cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 532240 + i * 10 + seq).respond(0.1)
                cls.matrixnet.on_place(MnPlace.META_REARRANGE, 532240 + i * 10 + seq).respond(0.9)
                cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 632241 + i * 10 + seq).respond(0.3)
                cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 732242 + i * 10 + seq).respond(0.6)


if __name__ == '__main__':
    main()
