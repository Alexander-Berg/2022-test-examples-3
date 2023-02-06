#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from unittest import skip
from core.types import (
    BlueOffer,
    IncutBlackListFb,
    MarketSku,
    MnPlace,
    Model,
    NewShopRating,
    Offer,
    Opinion,
    Shop,
    PriceThreshold,
)
from core.testcase import TestCase, main
from core.matcher import Contains, ElementCount


def get_normalized_matrixnet_value(matrixnet_value):
    return int(((1 << 30) - 1) * matrixnet_value)


class T(TestCase):
    @classmethod
    def prepare(cls):

        _ = 20
        CPA_SHOP_COUNT = 5
        FIRST_PARTY_SHOP_ID = 666

        for i in range(1, CPA_SHOP_COUNT + 1):
            cls.index.shops += [
                Shop(
                    fesh=i,
                    priority_region=213,
                    cpa=Shop.CPA_REAL,
                    name='CPA Магазин в Москве #{}'.format(i),
                    new_shop_rating=NewShopRating(new_rating_total=i),
                ),
            ]

        cls.index.shops += [
            Shop(
                fesh=FIRST_PARTY_SHOP_ID,
                datafeed_id=FIRST_PARTY_SHOP_ID,
                priority_region=213,
                regions=[213],
                name='1P Магазин в Москве #6',
                supplier_type=Shop.FIRST_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
            )
        ]

        cls.index.models += [
            Model(
                vendor_id=11,
                ts=6,
                title='модель молот 6',
                hid=111,
                hyperid=6,
                opinion=Opinion(rating=3.0, rating_count=25, total_count=150),
            ),
            Model(
                vendor_id=22,
                ts=5,
                title='модель молот 5',
                hid=111,
                hyperid=5,
                opinion=Opinion(rating=3.0, rating_count=35, total_count=150),
            ),
            Model(
                vendor_id=33,
                ts=3,
                title='модель молот 3',
                hid=222,
                hyperid=3,
                opinion=Opinion(rating=3.0, rating_count=45, total_count=150),
            ),
            Model(
                vendor_id=44,
                ts=1,
                title='модель молот 1',
                hid=222,
                hyperid=1,
                opinion=Opinion(rating=7.0, rating_count=55, total_count=150),
            ),
            Model(
                vendor_id=55,
                ts=4,
                title='модель молот 4',
                hid=222,
                hyperid=4,
                opinion=Opinion(rating=6.0, rating_count=65, total_count=150),
            ),
            Model(
                vendor_id=66,
                ts=2,
                title='модель молот 2',
                hid=222,
                hyperid=2,
                opinion=Opinion(rating=7.0, rating_count=75, total_count=150),
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                title="1P офер #1-1P",
                hid=111,
                hyperid=6,
                sku=6,
                vbid=50,
                blue_offers=[
                    BlueOffer(
                        price=100,
                        vbid=50,
                        feedid=FIRST_PARTY_SHOP_ID,
                        fee=1500,
                        title="1P оффер с высокой fee",
                        waremd5="FIRST-PARTY-6-1-1-XXXg",
                        ts=100060,
                    ),
                ],
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2).respond(0.9998)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3).respond(0.9997)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4).respond(0.9996)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1).respond(0.9993)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 6).respond(0.9994)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 5).respond(0.9995)

        cls.index.offers += [
            Offer(hyperid=1, fesh=1, vbid=20, price=135, fee=30, title='оффер молот1 магазин1', cpa=Offer.CPA_REAL),
            Offer(hyperid=2, fesh=2, vbid=50, price=193, fee=60, title='оффер молот2 магазин2', cpa=Offer.CPA_REAL),
            Offer(hyperid=3, fesh=3, vbid=10, price=156, fee=20, title='оффер молот3 магазин3', cpa=Offer.CPA_REAL),
            Offer(hyperid=4, fesh=4, vbid=40, price=172, fee=50, title='оффер молот4 магазин4', cpa=Offer.CPA_REAL),
            Offer(hyperid=5, fesh=5, vbid=30, price=161, fee=40, title='оффер молот5 магазин5', cpa=Offer.CPA_REAL),
        ]

    def test_advertising_carousel_sort_by_matrixnet_text(self):
        response = self.report.request_json(
            'place=advertising_carousel&text=молот&ranking-mode=mn&debug=1&rearr-factors=market_advertising_carousel_1p_zero_fee=1'
        )

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "модель молот 2"}},
                    {"titles": {"raw": "модель молот 3"}},
                    {"titles": {"raw": "модель молот 4"}},
                    {"titles": {"raw": "модель молот 5"}},
                    {"titles": {"raw": "модель молот 6"}},
                    {"titles": {"raw": "модель молот 1"}},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'report': {
                        'logicTrace': [
                            Contains("title :модель молот 2 Price 193 MNValue {}".format(0.9998)),
                            Contains("title :модель молот 3 Price 156 MNValue {}".format(0.9997)),
                            Contains("title :модель молот 4 Price 172 MNValue {}".format(0.9996)),
                            Contains("title :модель молот 5 Price 161 MNValue {}".format(0.9995)),
                            Contains("title :модель молот 6 Price 100 MNValue {} Fee 0".format(0.9994)),
                            Contains("title :модель молот 1 Price 135 MNValue {}".format(0.9993)),
                        ],
                    },
                },
            },
        )

    def test_advertising_carousel_sort_by_matrixnet_text_with_relevance_threshold(self):
        response = self.report.request_json(
            'place=advertising_carousel&text=молот&ranking-mode=mn&debug=1&rearr-factors=market_auction_high_relevance_formula_threshold=0.9995'
        )

        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'report': {
                        'logicTrace': [
                            Contains("title :модель молот 2 Price 193 MNValue {}".format(0.9998)),
                            Contains("title :модель молот 3 Price 156 MNValue {}".format(0.9997)),
                            Contains("title :модель молот 4 Price 172 MNValue {}".format(0.9996)),
                            Contains("title :модель молот 5 Price 161 MNValue {}".format(0.9995)),
                        ],
                    },
                },
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                'debug': {
                    'report': {
                        'logicTrace': [
                            Contains("title :модель молот 6 Price 100 MNValue"),
                        ],
                    },
                },
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                'debug': {
                    'report': {
                        'logicTrace': [
                            Contains("title :модель молот 1 Price 135 MNValue"),
                        ],
                    },
                },
            },
        )

    @skip('Broken due to offers vbid')
    def test_adv_carousel_mode(self):
        response = self.report.request_json(
            'place=cpa_shop_incut&text=молот&rearr-factors=market_advertising_carousel_1p_zero_fee=1;market_premium_ads_gallery_shop_incut_enable_adv_carousel_output=1&debug=1'
        )

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "оффер молот2 магазин2"}},
                    {"titles": {"raw": "оффер молот4 магазин4"}},
                    {"titles": {"raw": "оффер молот5 магазин5"}},
                    {"titles": {"raw": "1P оффер с высокой fee"}},
                    {"titles": {"raw": "оффер молот1 магазин1"}},
                    {"titles": {"raw": "оффер молот3 магазин3"}},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_adv_carousel_mode_filter_by_model_rating(self):
        response = self.report.request_json(
            'place=cpa_shop_incut&filter-by-model-rating=true&text=молот&rearr-factors=market_premium_ads_gallery_shop_incut_enable_adv_carousel_output=1&debug=1'
        )

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "оффер молот2 магазин2"}},
                    {"titles": {"raw": "оффер молот4 магазин4"}},
                    {"titles": {"raw": "оффер молот1 магазин1"}},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_advertising_carousel_sort_by_matrixnet_hid(self):
        response = self.report.request_json(
            'place=advertising_carousel&hid=111&ranking-mode=mn&min-num-doc=1&debug=1&rearr-factors=market_advertising_carousel_1p_zero_fee=1;market_advertising_carousel_1p_zero_fee=1'
        )

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "модель молот 5"}},
                    {"titles": {"raw": "модель молот 6"}},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'report': {
                        'logicTrace': [
                            Contains("title :модель молот 5 Price 161 MNValue {}".format(0.9995)),
                        ],
                    },
                },
            },
        )

    @skip('Broken due to offers vbid')
    def test_advertisig_cnarousel_sort_by_high_rating(self):
        response = self.report.request_json(
            'place=advertising_carousel&text=молот&ranking-mode=high_rating&debug=1&rearr-factors=market_advertising_carousel_1p_zero_fee=1'
        )

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "модель молот 2"}},
                    {"titles": {"raw": "модель молот 4"}},
                    {"titles": {"raw": "модель молот 5"}},
                    {"titles": {"raw": "модель молот 6"}},
                    {"titles": {"raw": "модель молот 1"}},
                    {"titles": {"raw": "модель молот 3"}},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'report': {
                        'logicTrace': [
                            Contains(
                                "title :модель молот 2 Price 193 MNValue 0.9998 Fee 60 vendorFee 10000 ShopBid 10 VendorBid "
                                "50 brokered_Shop 10 brokered_Vendor 42 brokered_Fee 49 relevance 1941192 relevanceMultiplier 0.8283060099"
                            ),
                            Contains(
                                "title :модель молот 4 Price 172 MNValue 0.9996 Fee 50 vendorFee 9302 ShopBid 10 VendorBid "
                                "40 brokered_Shop 10 brokered_Vendor 30 brokered_Fee 37 relevance 1607901 relevanceMultiplier 0.7499031346"
                            ),
                            Contains(
                                "title :модель молот 5 Price 161 MNValue 0.9995 Fee 40 vendorFee 7453 ShopBid 10 VendorBid "
                                "30 brokered_Shop 10 brokered_Vendor 25 brokered_Fee 33 relevance 1205770 relevanceMultiplier 0.8288479561"
                            ),
                            Contains(
                                "title :модель молот 6 Price 100 MNValue 0.9994 Fee 0 vendorFee 10000 ShopBid 10 VendorBid "
                                "50 brokered_Shop 10 brokered_Vendor 41 brokered_Fee 0 relevance 999400 relevanceMultiplier 0.8038453072"
                            ),
                            Contains(
                                "title :модель молот 1 Price 135 MNValue 0.9993 Fee 30 vendorFee 5925 ShopBid 10 VendorBid "
                                "20 brokered_Shop 10 brokered_Vendor 11 brokered_Fee 15 relevance 803363 relevanceMultiplier 0.5016213094"
                            ),
                            Contains(
                                "title :модель молот 3 Price 156 MNValue 0.9997 Fee 20 vendorFee 2564 ShopBid 10 VendorBid "
                                "10 brokered_Shop 10 brokered_Vendor 10 brokered_Fee 20 relevance 402984 relevanceMultiplier 1"
                            ),
                        ],
                    },
                },
            },
        )

    def test_advertisig_cnarousel_numdoc(self):
        response = self.report.request_json('place=advertising_carousel&numdoc=2&text=молот')

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "модель молот 2"}},
                    {"titles": {"raw": "модель молот 4"}},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_advertisig_cnarousel_filter_by_model_rating(self):

        response = self.report.request_json(
            'place=advertising_carousel&min-num-doc=1&ranking-mode=high_rating&text=молот&model-rating=7&debug=1'
        )

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "модель молот 2"}},
                    {"titles": {"raw": "модель молот 1"}},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_disable_by_blacklist(cls):
        # Один запрос можно заблокировать сразу по нескольким инклидам
        cls.index.incut_black_list_fb += [
            IncutBlackListFb(texts=['iPhone 13'], inclids=['PremiumAds', 'Mimicry', 'VendorIncut']),
            IncutBlackListFb(texts=['айфон 13'], inclids=['PremiumAds']),
            IncutBlackListFb(subtreeHids=[333], inclids=['PremiumAds']),
        ]
        cls.index.models += [
            Model(
                vendor_id=77,
                ts=7,
                title='Смартфон Apple iPhone 13',
                hid=333,
                hyperid=1414986413,
                opinion=Opinion(rating=4.5, rating_count=18, total_count=55),
            ),
            Model(
                vendor_id=77,
                ts=8,
                title='Смартфон Apple iPhone 13 mini',
                hid=333,
                hyperid=1414858419,
                opinion=Opinion(rating=4.4, rating_count=13, total_count=24),
            ),
            Model(
                vendor_id=77,
                ts=9,
                title='Смартфон Apple iPhone 13 Pro',
                hid=333,
                hyperid=1414773422,
                opinion=Opinion(rating=4.2, rating_count=20, total_count=69),
            ),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 7).respond(0.9994)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 8).respond(0.9993)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 9).respond(0.9992)
        cls.index.offers += [
            Offer(
                hyperid=1414986413,
                hid=333,
                fee=110,
                fesh=3,
                vbid=70,
                ts=7,
                price=77990,
                title='Смартфон Apple iPhone 13 512 ГБ RU, розовый',
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                hyperid=1414858419,
                hid=333,
                fee=100,
                fesh=4,
                vbid=70,
                ts=8,
                price=69800,
                title='Смартфон Apple iPhone 13 mini 512 ГБ RU, сияющая звезда',
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                hyperid=1414773422,
                hid=333,
                fee=130,
                fesh=5,
                vbid=70,
                ts=9,
                price=99990,
                title='Телефон Apple iPhone 13 Pro 256 Gb (Silver)',
                cpa=Offer.CPA_REAL,
            ),
        ]

    def test_disable_by_blacklist(self):
        response = self.report.request_json(
            'place=advertising_carousel&text=iPhone 13&min-num-doc=0&ranking-mode=mn&debug=1'
            '&rearr-factors=market_output_advert_request_blacklist_fb=0'
        )
        self.assertFragmentIn(response, {"results": ElementCount(3)})
        response_adv_off = self.report.request_json(
            'place=advertising_carousel&text=iPhone 13&min-num-doc=0&ranking-mode=mn&debug=1'
            '&rearr-factors=market_output_advert_request_blacklist_fb=1'
        )
        self.assertFragmentIn(response_adv_off, {"results": ElementCount(0)})
        response_adv_off_russian = self.report.request_json(
            'place=advertising_carousel&text=айфон 13&min-num-doc=0&ranking-mode=mn&debug=1'
            '&rearr-factors=market_output_advert_request_blacklist_fb=1'
        )
        self.assertFragmentIn(response_adv_off_russian, {"results": ElementCount(0)})

    # проверим отключение врезки по hid из запроса по флагу market_output_advert_request_blacklist_fb
    def test_disable_by_blacklist_hid(self):
        response = self.report.request_json(
            'place=advertising_carousel&hid=333&min-num-doc=0&ranking-mode=mn&debug=1'
            '&rearr-factors=market_output_advert_request_blacklist_fb=0'
        )
        self.assertFragmentIn(response, {"results": ElementCount(3)})
        response_adv_off = self.report.request_json(
            'place=advertising_carousel&hid=333&min-num-doc=0&ranking-mode=mn&debug=1'
            '&rearr-factors=market_output_advert_request_blacklist_fb=1'
        )
        self.assertFragmentIn(response_adv_off, {"results": ElementCount(0)})

    @classmethod
    def prepare_high_price_threshold(cls):
        cls.index.models += [
            Model(
                vendor_id=77,
                ts=1007,
                title='Смартфон Apple iPhone 23',
                hid=2333,
                hyperid=10013,
                opinion=Opinion(rating=4.5, rating_count=18, total_count=55),
            ),
            Model(
                vendor_id=77,
                ts=1008,
                title='Смартфон Apple iPhone 23 mini',
                hid=2333,
                hyperid=10019,
                opinion=Opinion(rating=4.4, rating_count=13, total_count=24),
            ),
            Model(
                vendor_id=77,
                ts=1009,
                title='Смартфон Apple iPhone 23 Pro',
                hid=2333,
                hyperid=10022,
                opinion=Opinion(rating=4.2, rating_count=20, total_count=69),
            ),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 7).respond(0.9994)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 8).respond(0.9993)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 9).respond(0.9992)
        cls.index.offers += [
            Offer(
                hyperid=10013,
                hid=2333,
                fee=110,
                fesh=3,
                vbid=70,
                ts=1007,
                price=77990,
                title='Смартфон Apple iPhone 23 512 ГБ RU, розовый',
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                hyperid=10019,
                hid=2333,
                fee=100,
                fesh=4,
                vbid=70,
                ts=1008,
                price=69800,
                title='Смартфон Apple iPhone 23 mini 512 ГБ RU, сияющая звезда',
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                hyperid=10022,
                hid=2333,
                fee=130,
                fesh=5,
                vbid=70,
                ts=1009,
                price=99990,
                title='Телефон Apple iPhone 23 Pro 256 Gb (Silver)',
                cpa=Offer.CPA_REAL,
            ),
        ]

        cls.index.price_threshold += [
            PriceThreshold(hid=2333, price=77990.159999999949),
        ]

    def test_high_price_threshold_enabled(self):
        response = self.report.request_json(
            'place=advertising_carousel&text=iPhone 23&min-num-doc=0&ranking-mode=mn&debug=1'
            '&rearr-factors=market_ac_high_price_threshold=1'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "Смартфон Apple iPhone 23"},
                        "prices": {"min": "77990"},
                    },
                    {
                        "titles": {"raw": "Смартфон Apple iPhone 23 mini"},
                        "prices": {"min": "69800"},
                    },
                ],
            },
        )

    def test_high_price_threshold_disabled(self):
        response = self.report.request_json(
            'place=advertising_carousel&text=iPhone 23&min-num-doc=0&ranking-mode=mn&debug=1'
            '&rearr-factors=market_ac_high_price_threshold=0'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "Смартфон Apple iPhone 23 Pro"},
                        "prices": {"min": "99990"},
                    },
                    {
                        "titles": {"raw": "Смартфон Apple iPhone 23"},
                        "prices": {"min": "77990"},
                    },
                    {
                        "titles": {"raw": "Смартфон Apple iPhone 23 mini"},
                        "prices": {"min": "69800"},
                    },
                ],
            },
        )


if __name__ == '__main__':
    main()
