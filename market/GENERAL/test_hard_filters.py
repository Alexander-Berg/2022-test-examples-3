#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types import GLParam, GLType, Model, MarketSku, BlueOffer, NavCategory
from core.testcase import TestCase, main

from core.types.fashion_parameters import FashionCategory

HARD_FILTER_FIRST = 26417130
HARD_FILTER_SECOND = 26419990
HARD_FILTER_VENDOR = 7893318
NOT_HARD_FILTER_FIRST = 12324
NOT_HARD_FILTER_THIRD = 34567

blueoffer301 = BlueOffer(
    offerid='blue.offer.301',
    waremd5='Sku1Price5-IiLVm1Goleg',
)

blueoffer302 = BlueOffer(
    offerid='blue.offer.302',
    waremd5='Sku2Price5-IiLVm1Goleg',
)


class T(TestCase):
    @classmethod
    def prepare(cls):

        cls.index.fashion_categories += [FashionCategory("CATEGORY_101", 101), FashionCategory("CATEGORY_102", 102)]

        cls.index.mskus += [
            MarketSku(
                title="blue.offer.301",
                hyperid=301,
                sku=101010,
                waremd5='Sku1-wdDXWsIiLVm1goleg',
                blue_offers=[blueoffer301],
                glparams=[
                    GLParam(param_id=HARD_FILTER_FIRST, value=2),
                    GLParam(param_id=HARD_FILTER_SECOND, value=1),
                    GLParam(param_id=HARD_FILTER_VENDOR, value=4),
                    GLParam(param_id=NOT_HARD_FILTER_FIRST, value=2),
                    GLParam(param_id=1000, value=2),
                ],
            ),
            MarketSku(
                title="blue.offer.302",
                hyperid=302,
                sku=101011,
                waremd5='Sku2-wdDXWsIiLVm1goleg',
                blue_offers=[blueoffer302],
                glparams=[
                    GLParam(param_id=HARD_FILTER_FIRST, value=1),
                    GLParam(param_id=HARD_FILTER_SECOND, value=0),
                    GLParam(param_id=HARD_FILTER_VENDOR, value=5),
                    GLParam(param_id=NOT_HARD_FILTER_THIRD, value=1),
                    GLParam(param_id=1000, value=2),
                ],
            ),
        ]

        cls.index.models += [
            Model(
                hyperid=301,
                hid=101,
                title='model_301',
            ),
            Model(
                hyperid=302,
                hid=102,
                title='model_302',
            ),
        ]

        for hid in [101, 102]:
            cls.index.gltypes += [
                GLType(cluster_filter=False, param_id=HARD_FILTER_FIRST, hid=hid, gltype=GLType.ENUM, values=[1, 2]),
                GLType(cluster_filter=False, param_id=HARD_FILTER_SECOND, hid=hid, gltype=GLType.BOOL),
                GLType(cluster_filter=False, param_id=HARD_FILTER_VENDOR, hid=hid, gltype=GLType.ENUM, values=[4, 5]),
                GLType(
                    cluster_filter=False, param_id=NOT_HARD_FILTER_FIRST, hid=hid, gltype=GLType.ENUM, values=[1, 2]
                ),
                GLType(cluster_filter=False, param_id=1000, hid=hid, gltype=GLType.ENUM, values=[1, 2]),
            ]

        cls.index.navtree += [
            NavCategory(nid=73281, hid=101, name="Test cat 1"),
        ]

    def test_prime_hard_filter_show(self):
        response = self.report.request_json('place=prime&text=blue.offer')
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {"id": str(HARD_FILTER_SECOND)},
                    {"id": str(HARD_FILTER_VENDOR)},
                    {"id": str(HARD_FILTER_FIRST)},
                ],
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "filters": [{"id": str(NOT_HARD_FILTER_FIRST)}],
            },
        )
        self.assertFragmentIn(
            response,
            {
                "results": [{"marketSku": str(101010)}, {"marketSku": str(101011)}],
            },
            allow_different_len=False,
        )

    def test_prime_hard_filter_list_flag_show(self):
        '''
        https://st.yandex-team.ru/MARKETOUT-44273
        Тестируем флаг market_hard_filters_for_search для добавления фильтров на серч
        '''
        response = self.report.request_json('place=prime&text=blue.offer')
        self.assertFragmentNotIn(
            response,
            {
                "filters": [{"id": str(1000)}],
            },
        )

        response = self.report.request_json(
            'place=prime&text=blue.offer&rearr-factors=market_hard_filters_for_search=1001,1000'
        )
        self.assertFragmentIn(
            response,
            {
                "filters": [{"id": str(1000)}],
            },
        )

    def test_prime_hard_filter_using(self):
        response = self.report.request_json(
            'place=prime&glfilter={}:1&debug=1&text=blue.offer'.format(HARD_FILTER_FIRST)
        )
        self.assertFragmentIn(
            response,
            {
                "results": [{"marketSku": str(101011)}],
            },
            allow_different_len=False,
        )

    def test_prime_hard_with_hid_pass_with_another_way(self):
        response = self.report.request_json('place=prime&text=model_301&hid=101')
        self.assertFragmentIn(
            response,
            {
                "filters": [{"id": str(NOT_HARD_FILTER_FIRST)}],
            },
        )

    def test_prime_hard_with_nid_pass_with_another_way(self):
        response = self.report.request_json('place=prime&text=model_301&nid=73281')
        self.assertFragmentIn(
            response,
            {
                "filters": [{"id": str(NOT_HARD_FILTER_FIRST)}],
            },
        )


if __name__ == '__main__':
    main()
