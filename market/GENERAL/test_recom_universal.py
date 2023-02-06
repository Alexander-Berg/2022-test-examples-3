#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import BlueOffer, HyperCategory, HyperCategoryType, MarketSku, NavCategory, Offer, Shop
from core.matcher import NotEmpty

import six

if six.PY3:
    import urllib.parse as urlparse
else:
    import urlparse


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.index.hypertree += [
            HyperCategory(hid=1580501, output_type=HyperCategoryType.GURU, name="Категория 1"),
            HyperCategory(hid=1580502, output_type=HyperCategoryType.GURU, name="Категория 2"),
        ]

        cls.index.navtree = [
            NavCategory(nid=111, is_blue=True, name='Навигационный узел 1', hid=1580501),
            NavCategory(nid=222, is_blue=True, name='Навигационный узел 2', hid=1580502),
        ]

        cls.index.shops += [
            Shop(fesh=100, cpa=Shop.CPA_REAL, business_fesh=1000, business_name="Первый магазин"),
            Shop(fesh=200, cpa=Shop.CPA_REAL, business_fesh=2000, business_name="Второй магазин"),
            Shop(fesh=300, cpa=Shop.CPA_REAL, business_fesh=3000, business_name="Третий магазин"),
            Shop(fesh=400, cpa=Shop.CPA_REAL, business_fesh=4000, business_name="Четвёртый магазин"),
        ]

        # remember, waremd5 string is 22 chars long
        cls.index.offers += [
            Offer(
                hid=1580501,
                hyperid=15805010,
                fesh=100,
                cpa=Offer.CPA_REAL,
                price=100,
                title="shop1-offer1",
                business_id=1000,
                waremd5="wareid-of-shop1-ofer1w",
            ),
            Offer(
                hid=1580501,
                hyperid=15805010,
                fesh=200,
                cpa=Offer.CPA_REAL,
                price=100,
                title="shop2-offer2",
                business_id=2000,
                waremd5="wareid-of-shop2-ofer2w",
            ),
            Offer(
                hid=1580502,
                hyperid=15805020,
                fesh=100,
                cpa=Offer.CPA_REAL,
                price=100,
                title="shop1-offer3",
                business_id=1000,
                waremd5="wareid-of-shop1-ofer3w",
            ),
            Offer(
                hid=1580502,
                hyperid=15805020,
                fesh=200,
                cpa=Offer.CPA_REAL,
                price=100,
                title="shop2-offer4",
                business_id=2000,
                waremd5="wareid-of-shop2-ofer4w",
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                hid=1580501,
                hyperid=15805010,
                sku=158050100,
                blue_offers=[
                    BlueOffer(
                        hid=1580501,
                        hyperid=15805010,
                        fesh=300,
                        cpa=Offer.CPA_REAL,
                        price=100,
                        title="shop3-blue-offer1",
                        business_id=3000,
                        waremd5="wareidblueshop3-ofer1w",
                    )
                ],
            ),
            MarketSku(
                hid=1580501,
                hyperid=15805010,
                sku=158050101,
                blue_offers=[
                    BlueOffer(
                        hid=1580501,
                        hyperid=15805010,
                        fesh=400,
                        cpa=Offer.CPA_REAL,
                        price=100,
                        title="shop4-blue-offer2",
                        business_id=4000,
                        waremd5="wareidblueshop4-ofer2w",
                    )
                ],
            ),
            MarketSku(
                hid=1580502,
                hyperid=15805020,
                sku=158050200,
                blue_offers=[
                    BlueOffer(
                        hid=1580502,
                        hyperid=15805020,
                        fesh=300,
                        cpa=Offer.CPA_REAL,
                        price=100,
                        title="shop3-blue-offer3",
                        business_id=3000,
                        waremd5="wareidblueshop3-ofer3w",
                    )
                ],
            ),
            MarketSku(
                hid=1580502,
                hyperid=15805020,
                sku=158050201,
                blue_offers=[
                    BlueOffer(
                        hid=1580502,
                        hyperid=15805020,
                        fesh=400,
                        cpa=Offer.CPA_REAL,
                        price=100,
                        title="shop4-blue-offer4",
                        business_id=4000,
                        waremd5="wareidblueshop4-ofer4w",
                    )
                ],
            ),
        ]

    def _check_url(self, actual_url, expected_url):
        actual_url = urlparse.urlparse(actual_url)
        expected_url = urlparse.urlparse(expected_url)

        self.assertEqual(actual_url.path, expected_url.path)

        self.assertEqual(
            urlparse.parse_qs(actual_url.query),
            urlparse.parse_qs(expected_url.query),
        )

    def test_shop_goods_on_white(self):
        response = self.report.request_json('place=recom_universal&recom-place=shop_goods&fesh=2000&rgb=green')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'title': 'Товары от магазина «Второй магазин»',
                    'link': {'url': NotEmpty(), 'params': {'fesh': 2000}},
                    'results': [
                        {'entity': 'offer', 'wareId': 'wareid-of-shop2-ofer2w'},
                        {'entity': 'offer', 'wareId': 'wareid-of-shop2-ofer4w'},
                    ],
                },
                'sorts': NotEmpty(),
                'intents': NotEmpty(),
                'filters': NotEmpty(),
            },
            preserve_order=False,
            allow_different_len=False,
        )
        self._check_url(response['search']['link']['url'], '/search?fesh=2000')

    def test_shop_goods_on_blue(self):
        response = self.report.request_json('place=recom_universal&recom-place=shop_goods&fesh=4000&rgb=blue')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'title': 'Товары от магазина «Четвёртый магазин»',
                    'link': {'url': NotEmpty(), 'params': {'fesh': 4000}},
                    'results': [
                        {
                            'entity': 'product',
                            'id': 15805010,
                            'offers': {'items': [{'wareId': 'wareidblueshop4-ofer2w'}]},
                        },
                        {
                            'entity': 'product',
                            'id': 15805020,
                            'offers': {'items': [{'wareId': 'wareidblueshop4-ofer4w'}]},
                        },
                    ],
                },
                'sorts': NotEmpty(),
                'intents': NotEmpty(),
                'filters': NotEmpty(),
            },
            preserve_order=False,
            allow_different_len=False,
        )
        self._check_url(response['search']['link']['url'], '/search?fesh=4000')

    def test_shop_goods_in_category_on_white(self):
        response = self.report.request_json(
            'place=recom_universal&recom-place=shop_goods_in_category&hid=1580501&fesh=1000&rgb=green'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'title': 'Навигационный узел 1 от магазина «Первый магазин»',
                    'link': {
                        'url': NotEmpty(),
                        'params': {
                            'fesh': 1000,
                            'hid': 1580501,
                            'nid': 111,
                        },
                    },
                    'results': [
                        {'entity': 'offer', 'wareId': 'wareid-of-shop1-ofer1w'},
                    ],
                },
                'sorts': NotEmpty(),
                'intents': NotEmpty(),
                'filters': NotEmpty(),
            },
            preserve_order=False,
            allow_different_len=False,
        )
        self._check_url(response['search']['link']['url'], '/catalog/111/list?fesh=1000&hid=1580501&nid=111')

    def test_shop_goods_in_category_on_blue(self):
        response = self.report.request_json(
            'place=recom_universal&recom-place=shop_goods_in_category&hid=1580501&fesh=3000&rgb=blue'
            '&rearr-factors=use_meta_dssm_factors_from_model_service=0'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'title': 'Навигационный узел 1 от магазина «Третий магазин»',
                    'link': {'url': NotEmpty(), 'params': {'fesh': 3000, 'hid': 1580501, 'nid': 111}},
                    'results': [
                        {
                            'entity': 'product',
                            'id': 15805010,
                            'offers': {'items': [{'wareId': 'wareidblueshop3-ofer1w'}]},
                        },
                    ],
                },
                'sorts': NotEmpty(),
                'intents': NotEmpty(),
                'filters': NotEmpty(),
            },
            preserve_order=False,
            allow_different_len=False,
        )
        self._check_url(response['search']['link']['url'], '/catalog/111/list?fesh=3000&hid=1580501&nid=111')


if __name__ == '__main__':
    main()
