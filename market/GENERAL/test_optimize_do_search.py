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

    rid_msk = 213

    hid_a = 1
    hid_s = 5

    model_a = 10100
    model_b = 10200
    model_c = 10300
    model_s = 10500

    sku_a_1 = model_a | (1 << 50)
    sku_a_2 = model_a | (1 << 50) + 1
    ungroupped_a_0 = model_a
    ungroupped_a_1 = sku_a_1
    ungroupped_a_2 = sku_a_2

    sku_c_1 = model_c | (1 << 50)
    ungroupped_c_1 = sku_c_1

    sku_s_1 = model_s + 1
    sku_s_2 = model_s + 2
    sku_s_3 = model_s + 3
    ungroupped_s_1 = sku_s_1
    ungroupped_s_2 = sku_s_2
    ungroupped_s_3 = sku_s_3


class T(TestCase):
    """
    Тест проверки работы группировки при SKU id отличающимся от modelId в старшем бите (бит 50)
    """

    @classmethod
    def prepare(cls):
        cls.disable_check_empty_output()

    @classmethod
    def prepare_ordinary_request(cls):
        cls.index.models += [
            Model(
                hyperid=_C.model_a,
                hid=_C.hid_a,
                title="model a",
                ungrouped_blue=[
                    UngroupedModel(group_id=_C.ungroupped_a_0, title="model a.0", key='a_0'),
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
            # model A w/o SKU
            Offer(
                waremd5=Offer.generate_waremd5('A_w_o_SKU'),
                title="offer A w/o SKU",
                cpa=Offer.CPA_REAL,
                hyperid=_C.model_a,
                ungrouped_model_blue=_C.ungroupped_a_0,
            ),
            # model A with SKU-1
            Offer(
                waremd5=Offer.generate_waremd5('A_with_SKU-1'),
                title="offer A with SKU-1",
                cpa=Offer.CPA_REAL,
                hyperid=_C.model_a,
                sku=_C.sku_a_1,
                ungrouped_model_blue=_C.ungroupped_a_1,
            ),
            # model A with SKU-2
            Offer(
                waremd5=Offer.generate_waremd5('A_with_SKU-2'),
                title="offer A with SKU-2",
                cpa=Offer.CPA_REAL,
                hyperid=_C.model_a,
                sku=_C.sku_a_2,
                ungrouped_model_blue=_C.ungroupped_a_2,
            ),
            # model B w/o SKU
            Offer(
                waremd5=Offer.generate_waremd5('B_w_o_SKU'),
                title="offer B w/o SKU",
                cpa=Offer.CPA_REAL,
                hyperid=_C.model_b,
            ),
        ]

    def test_ordinary_request(self):
        """
        Проверяем базовый запрос в репорт (офферы со SKU с/без группировкой и офферы без SKU)
        """

        request = (
            'place=prime&hid={hid}'
            '&use-default-offers=1&allow-collapsing=1&allow-ungrouping=1'
            '&rearr-factors=market_optimize_default_offers_search_v2={enable_flag}'
        )

        for enable_flag in [0, 1]:
            response = self.report.request_json(request.format(hid=_C.hid_a, enable_flag=enable_flag))

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
                                        'wareId': 'A_with_SKU-1_________g',
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
                                        'wareId': 'A_with_SKU-2_________g',
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
                                        'wareId': 'B_w_o_SKU____________g',
                                        'marketSku': Absent(),
                                    }
                                ],
                            },
                        },
                    ]
                },
                allow_different_len=False,
            )

    @classmethod
    def prepare_sku_filter(cls):
        cls.index.models += [
            Model(
                hyperid=_C.model_s,
                hid=_C.hid_s,
                title="model s",
                ungrouped_blue=[
                    UngroupedModel(group_id=_C.ungroupped_s_1, title="model s.1", key='s_1'),
                    UngroupedModel(group_id=_C.ungroupped_s_2, title="model s.2", key='s_2'),
                    UngroupedModel(group_id=_C.ungroupped_s_3, title="model s.3", key='s_3'),
                ],
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                title='SKU-1 - in prime', hyperid=_C.model_s, sku=_C.sku_s_1, ungrouped_model_blue=_C.ungroupped_s_1
            ),
            MarketSku(
                title='SKU-2 - in prime', hyperid=_C.model_s, sku=_C.sku_s_2, ungrouped_model_blue=_C.ungroupped_s_2
            ),
            MarketSku(
                title='SKU-3 - absent', hyperid=_C.model_s, sku=_C.sku_s_3, ungrouped_model_blue=_C.ungroupped_s_3
            ),
        ]

        cls.index.offers += [
            Offer(
                waremd5=Offer.generate_waremd5('S_with_SKU-1'),
                title="offer S with SKU-1 - in prime",
                cpa=Offer.CPA_REAL,
                hyperid=_C.model_s,
                sku=_C.sku_s_1,
                ungrouped_model_blue=_C.ungroupped_s_1,
            ),
            Offer(
                waremd5=Offer.generate_waremd5('S_with_SKU-2'),
                title="offer S with SKU-2 - in prime",
                cpa=Offer.CPA_REAL,
                hyperid=_C.model_s,
                sku=_C.sku_s_2,
                ungrouped_model_blue=_C.ungroupped_s_2,
            ),
            Offer(
                waremd5=Offer.generate_waremd5('S_with_SKU-3'),
                title="offer S with SKU-3 - absent",
                cpa=Offer.CPA_REAL,
                hyperid=_C.model_s,
                sku=_C.sku_s_3,
                ungrouped_model_blue=_C.ungroupped_s_3,
            ),
        ]

    def test_sku_filter(self):
        """
        Проверяем что при включенной оптимизации ДО для СКЮ, которые не были найдены поиском по СКЮ,
        не передаются с базовых при получении списка ДО
        """

        request = (
            'place=prime&hid={hid}&text={text}'
            '&use-default-offers=1&allow-collapsing=1&allow-ungrouping=1'
            '&debug=1'
            '&rearr-factors=market_optimize_default_offers_search_v2=1'
        )

        # Строка для SKU которая исключается добавлением 'text=...'
        test_str = 'Got DO {}, SKU: {}, MODEL: {} [for_SKU]'.format('S_with_SKU-3_________g', _C.sku_s_3, _C.model_s)

        # Без указания текста запроса 'text' получаем все СКЮ
        response = self.report.request_json(request.format(hid=_C.hid_s, text=''))
        self.assertTrue(test_str in str(response))
        self.assertFragmentIn(
            response,
            {
                'total': 3,
            },
        )

        # Указание текста запроса 'text' оставляет только СКЮ с соответствующим текстом
        response = self.report.request_json(request.format(hid=_C.hid_s, text='prime'))
        self.assertTrue(test_str not in str(response))
        self.assertFragmentIn(
            response,
            {
                'total': 2,
                'results': [
                    {
                        'entity': 'product',
                        'id': _C.model_s,
                        'offers': {
                            'items': [
                                {
                                    'wareId': 'S_with_SKU-1_________g',
                                    'marketSku': str(_C.sku_s_1),
                                }
                            ],
                        },
                    },
                    {
                        'entity': 'product',
                        'id': _C.model_s,
                        'offers': {
                            'items': [
                                {
                                    'wareId': 'S_with_SKU-2_________g',
                                    'marketSku': str(_C.sku_s_2),
                                }
                            ],
                        },
                    },
                ],
            },
        )


if __name__ == '__main__':
    main()
