#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    MarketSku,
    Model,
    Offer,
    Region,
    Shop,
    UngroupedModel,
)
from core.testcase import TestCase, main
from core.matcher import NoKey


class _C:

    rid_msk = 213

    hid_a = 1

    model_a = 10

    ungroupped_a_1 = 1
    ungroupped_a_2 = 2
    ungroupped_a_3 = 3

    sku_a_1 = 1001
    sku_a_2 = 1002
    sku_a_3 = 1003

    fesh_msk_1 = 501
    fesh_msk_2 = 502
    fesh_msk_3 = 503
    fesh_msk_4 = 504
    fesh_msk_5 = 505


class T(TestCase):
    """
    Тест вычисления количества офферов для SKU
    """

    @classmethod
    def prepare(cls):
        cls.disable_check_empty_output()

        cls.index.regiontree = [
            Region(rid=_C.rid_msk, name='Мск'),
        ]

        cls.index.models += [
            Model(
                hyperid=_C.model_a,
                hid=_C.hid_a,
                title="model a",
                ungrouped_blue=[
                    UngroupedModel(group_id=_C.ungroupped_a_1, title="model a.1", key='a_1'),
                    UngroupedModel(group_id=_C.ungroupped_a_2, title="model a.2", key='a_2'),
                    UngroupedModel(group_id=_C.ungroupped_a_3, title="model a.3", key='a_3'),
                ],
            ),
        ]

        cls.index.shops += [
            Shop(
                fesh=_C.fesh_msk_1,
                priority_region=_C.rid_msk,
                regions=[_C.rid_msk],
                cpa=Shop.CPA_REAL,
                name='MSK-1 shop',
            ),
            Shop(
                fesh=_C.fesh_msk_2,
                priority_region=_C.rid_msk,
                regions=[_C.rid_msk],
                cpa=Shop.CPA_REAL,
                name='MSK-2 shop',
            ),
            Shop(
                fesh=_C.fesh_msk_3,
                priority_region=_C.rid_msk,
                regions=[_C.rid_msk],
                cpa=Shop.CPA_REAL,
                name='MSK-3 shop',
            ),
            Shop(
                fesh=_C.fesh_msk_4,
                priority_region=_C.rid_msk,
                regions=[_C.rid_msk],
                cpa=Shop.CPA_REAL,
                name='MSK-4 shop',
            ),
            Shop(
                fesh=_C.fesh_msk_5,
                priority_region=_C.rid_msk,
                regions=[_C.rid_msk],
                cpa=Shop.CPA_REAL,
                name='MSK-5 shop',
            ),
        ]

        cls.index.mskus += [
            MarketSku(title='a_1', hyperid=_C.model_a, sku=_C.sku_a_1, ungrouped_model_blue=_C.ungroupped_a_1),
            MarketSku(title='a_2', hyperid=_C.model_a, sku=_C.sku_a_2, ungrouped_model_blue=_C.ungroupped_a_2),
            MarketSku(title='a_3', hyperid=_C.model_a, sku=_C.sku_a_3, ungrouped_model_blue=_C.ungroupped_a_3),
        ]

        cls.index.offers += [
            # SKU a_1
            Offer(
                fesh=_C.fesh_msk_1,
                title="offer a_1 MSK-1",
                cpa=Offer.CPA_REAL,
                price=1100,
                hyperid=_C.model_a,
                sku=_C.sku_a_1,
                ungrouped_model_blue=_C.ungroupped_a_1,
            ),
            Offer(
                fesh=_C.fesh_msk_2,
                title="offer a_1 MSK-2",
                cpa=Offer.CPA_REAL,
                price=1020,
                hyperid=_C.model_a,
                sku=_C.sku_a_1,
                ungrouped_model_blue=_C.ungroupped_a_1,
            ),
            Offer(
                fesh=_C.fesh_msk_3,
                title="offer a_1 MSK-3",
                cpa=Offer.CPA_REAL,
                price=1030,
                hyperid=_C.model_a,
                sku=_C.sku_a_1,
                ungrouped_model_blue=_C.ungroupped_a_1,
            ),
            Offer(
                fesh=_C.fesh_msk_4,
                title="offer a_1 MSK-4",
                cpa=Offer.CPA_REAL,
                price=1040,
                hyperid=_C.model_a,
                sku=_C.sku_a_1,
                ungrouped_model_blue=_C.ungroupped_a_1,
            ),
            Offer(
                fesh=_C.fesh_msk_5,
                title="offer a_1 MSK-5",
                cpa=Offer.CPA_NO,
                price=1005,
                hyperid=_C.model_a,
                sku=_C.sku_a_1,
                ungrouped_model_blue=_C.ungroupped_a_1,
            ),
            # SKU a_2
            Offer(
                fesh=_C.fesh_msk_1,
                title="offer a_2 MSK-1",
                cpa=Offer.CPA_REAL,
                price=2200,
                hyperid=_C.model_a,
                sku=_C.sku_a_2,
                ungrouped_model_blue=_C.ungroupped_a_2,
            ),
            Offer(
                fesh=_C.fesh_msk_2,
                title="offer a_2 MSK-2",
                cpa=Offer.CPA_REAL,
                price=2300,
                hyperid=_C.model_a,
                sku=_C.sku_a_2,
                ungrouped_model_blue=_C.ungroupped_a_2,
            ),
            # SKU a_3
            Offer(
                fesh=_C.fesh_msk_2,
                title="offer a_3 MSK-2",
                cpa=Offer.CPA_NO,
                price=3200,
                hyperid=_C.model_a,
                sku=_C.sku_a_3,
                ungrouped_model_blue=_C.ungroupped_a_3,
            ),
            Offer(
                fesh=_C.fesh_msk_3,
                title="offer a_3 MSK-3",
                cpa=Offer.CPA_NO,
                price=3300,
                hyperid=_C.model_a,
                sku=_C.sku_a_3,
                ungrouped_model_blue=_C.ungroupped_a_3,
            ),
            Offer(
                fesh=_C.fesh_msk_4,
                title="offer a_3 MSK-4",
                cpa=Offer.CPA_NO,
                price=3400,
                hyperid=_C.model_a,
                sku=_C.sku_a_3,
                ungrouped_model_blue=_C.ungroupped_a_3,
            ),
        ]

    def test_sku_offers_count(self):
        """
        Проверяем вычисление количества офферов для SKU
        """

        query_ext = "&use-default-offers=1&allow-collapsing=1&allow-ungrouping=1"
        query_debug_ext = "&debug=1"

        not_zero_for_args = [
            (True, ''),
            (True, '&rearr-factors=market_report_add_sku_stats_touch_only=0'),
            (True, '&platform=touch&rearr-factors=market_report_add_sku_stats_touch_only=1'),
            (False, '&rearr-factors=market_report_add_sku_stats_touch_only=1'),
        ]

        for has_count, args in not_zero_for_args:
            # Запрос без cpa=real -- в расчете количества учитываются все офферы,
            # в результате -- все офферы
            response = self.report.request_json(
                'place=prime&rids={}&hid={}'.format(_C.rid_msk, _C.hid_a) + query_ext + query_debug_ext + args
            )

            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'product',
                            'id': _C.model_a,
                            'skuOffersCount': 5 if has_count else 0,
                            'skuPrices': {'min': "1005", 'max': "1100"} if has_count else NoKey('skuPrices'),
                            'offers': {
                                'items': [
                                    {
                                        'marketSku': str(_C.sku_a_1),
                                    }
                                ],
                            },
                        },
                        {
                            'entity': 'product',
                            'id': _C.model_a,
                            'skuOffersCount': 2 if has_count else 0,
                            'skuPrices': {'min': "2200", 'max': "2300"} if has_count else NoKey('skuPrices'),
                            'offers': {
                                'items': [
                                    {
                                        'marketSku': str(_C.sku_a_2),
                                    }
                                ],
                            },
                        },
                        {
                            'entity': 'product',
                            'id': _C.model_a,
                            'skuOffersCount': 3 if has_count else 0,
                            'skuPrices': {'min': "3200", 'max': "3400"} if has_count else NoKey('skuPrices'),
                            'offers': {
                                'items': [
                                    {
                                        'marketSku': str(_C.sku_a_3),
                                    }
                                ],
                            },
                        },
                    ]
                },
                allow_different_len=False,
            )

        # Запрос с cpa=real и market_no_cpc_mode_if_cpa_real=0 -- в расчете количества учитываются все офферы,
        # в результате -- только CPA
        rearr = "&rearr-factors=market_no_cpc_mode_if_cpa_real=0"
        response = self.report.request_json(
            'place=prime&rids={}&cpa=real&hid={}'.format(_C.rid_msk, _C.hid_a) + query_ext + query_debug_ext + rearr
        )

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'id': _C.model_a,
                        'skuOffersCount': 5,
                        'skuPrices': {'min': "1005", 'max': "1100"},
                        'offers': {
                            'items': [
                                {
                                    'marketSku': str(_C.sku_a_1),
                                }
                            ],
                        },
                    },
                    {
                        'entity': 'product',
                        'id': _C.model_a,
                        'skuOffersCount': 2,
                        'skuPrices': {'min': "2200", 'max': "2300"},
                        'offers': {
                            'items': [
                                {
                                    'marketSku': str(_C.sku_a_2),
                                }
                            ],
                        },
                    },
                ]
            },
            allow_different_len=False,
        )

        # Запрос с cpa=real и без указания market_no_cpc_mode_if_cpa_real -- в расчете количества учитываются только CPA-офферы,
        # в результате -- только CPA
        response = self.report.request_json(
            'place=prime&rids={}&cpa=real&hid={}'.format(_C.rid_msk, _C.hid_a) + query_ext + query_debug_ext
        )

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'id': _C.model_a,
                        'skuOffersCount': 4,
                        'skuPrices': {'min': "1020", 'max': "1100"},
                        'offers': {
                            'items': [
                                {
                                    'marketSku': str(_C.sku_a_1),
                                }
                            ],
                        },
                    },
                    {
                        'entity': 'product',
                        'id': _C.model_a,
                        'skuOffersCount': 2,
                        'skuPrices': {'min': "2200", 'max': "2300"},
                        'offers': {
                            'items': [
                                {
                                    'marketSku': str(_C.sku_a_2),
                                }
                            ],
                        },
                    },
                ]
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
