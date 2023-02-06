#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    MarketSku,
    Model,
    Offer,
    UngroupedModel,
)
from core.testcase import TestCase, main
from core.matcher import Absent


class _C:

    hid_a = 1

    model_a = 1001
    model_b = 1002

    ungroupped_a_1 = 1
    ungroupped_a_2 = 2

    sku_a_1 = 1001 | (1 << 50)
    sku_a_2 = 1002 | (1 << 50)


class T(TestCase):
    """
    Тест проверки работы группировки при SKU id отличающимся от modelId в старшем бите (бит 50)
    """

    @classmethod
    def prepare(cls):
        cls.disable_check_empty_output()

        cls.index.models += [
            Model(
                hyperid=_C.model_a,
                hid=_C.hid_a,
                title="model a",
                ungrouped_blue=[
                    UngroupedModel(group_id=_C.ungroupped_a_1, title="model a.1", key='a_1'),
                    UngroupedModel(group_id=_C.ungroupped_a_2, title="model a.2", key='a_2'),
                ],
            ),
            Model(
                hyperid=_C.model_b,
                hid=_C.hid_a,
                title="model b",
            ),
        ]

        cls.index.mskus += [
            MarketSku(title='a_1', hyperid=_C.model_a, sku=_C.sku_a_1, ungrouped_model_blue=_C.ungroupped_a_1),
            MarketSku(title='a_2', hyperid=_C.model_a, sku=_C.sku_a_2, ungrouped_model_blue=_C.ungroupped_a_2),
        ]

        cls.index.offers += [
            # with SKU-1
            Offer(
                waremd5=Offer.generate_waremd5('_with_SKU-1'),
                title="offer with SKU-1",
                cpa=Offer.CPA_REAL,
                hyperid=_C.model_a,
                sku=_C.sku_a_1,
                ungrouped_model_blue=_C.ungroupped_a_1,
            ),
            # with SKU-2
            Offer(
                waremd5=Offer.generate_waremd5('_with_SKU-2'),
                title="offer with SKU-2",
                cpa=Offer.CPA_REAL,
                hyperid=_C.model_a,
                sku=_C.sku_a_2,
                ungrouped_model_blue=_C.ungroupped_a_2,
            ),
            # w/o SKU
            Offer(
                waremd5=Offer.generate_waremd5('_w_o_SKU'),
                title="offer w/o SKU",
                cpa=Offer.CPA_REAL,
                hyperid=_C.model_b,
            ),
        ]

    def test_sku_offers_count(self):
        """
        Проверка, что расширение группировочного атрибута до 64 бит корректно работает.
        Тест проверяет, что код корректно вычисляет ДО, когда идентификаторы моделей и SKU отличаются в старших разрядах.
        """

        query_ext = "&use-default-offers=1&allow-collapsing=1&allow-ungrouping=1"

        response = self.report.request_json('place=prime&hid={}'.format(_C.hid_a) + query_ext)

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'id': _C.model_a,
                        'offers': {
                            'items': [
                                {
                                    'wareId': '_with_SKU-1__________g',
                                    'marketSku': str(_C.sku_a_1),
                                }
                            ],
                        },
                    },
                    {
                        'entity': 'product',
                        'id': _C.model_a,
                        'offers': {
                            'items': [
                                {
                                    'wareId': '_with_SKU-2__________g',
                                    'marketSku': str(_C.sku_a_2),
                                }
                            ],
                        },
                    },
                    {
                        'entity': 'product',
                        'id': _C.model_b,
                        'offers': {
                            'items': [
                                {
                                    'wareId': '_w_o_SKU_____________g',
                                    'marketSku': Absent(),
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
