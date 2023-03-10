#!/usr/bin/env python
# -*- coding: utf-8 -*-

import __classic_import  # noqa
import copy
import market.media_adv.incut_search.mt.env as env
from market.media_adv.incut_search.beam.incut import IncutModelsList
from market.media_adv.incut_search.beam.model import ModelWithBid


class T(env.MediaAdvIncutSearchSuite):
    @classmethod
    def prepare_extra_coverage(cls):
        cls.content.incuts += [
            IncutModelsList(
                foreign_hid=1234,
                vendor_id=2345,
                datasource_id=10,
                models=[ModelWithBid(model_id=1000 + i) for i in range(1, 4)],
                bid=90,
            ),
            IncutModelsList(
                hid=1234,
                vendor_id=2346,
                datasource_id=11,
                models=[ModelWithBid(model_id=1010 + i) for i in range(1, 4)],
                bid=45,
            ),
        ]

    def test_extra_coverage(self):
        high_bid_incut_entities = {
            'incutLists': [
                [
                    {
                        'entity': 'incut',
                        'id': '1',
                    }
                ]
            ],
            'entities': {
                'model': {
                    '1': {
                        'modelId': 1001,
                    },
                    '2': {
                        'modelId': 1002,
                    },
                    '3': {
                        'modelId': 1003,
                    },
                },
                'incut': {
                    '1': {
                        'incutType': 'ModelsList',
                        'saasRequestHid': 1234,
                        'models': [
                            {
                                'entity': 'model',
                                'id': '1',
                            },
                            {
                                'entity': 'model',
                                'id': '2',
                            },
                            {
                                'entity': 'model',
                                'id': '3',
                            },
                        ],
                    },
                },
                'vendor': {
                    '1': {
                        'vendorId': 2345,
                    },
                },
            },
        }
        prim_target_incut_entities = copy.deepcopy(high_bid_incut_entities)
        prim_target_incut_entities['entities']['model']['1']['modelId'] = 1011
        prim_target_incut_entities['entities']['model']['2']['modelId'] = 1012
        prim_target_incut_entities['entities']['model']['3']['modelId'] = 1013
        prim_target_incut_entities['entities']['vendor']['1']['vendorId'] = 2346

        response = self.request(
            {
                'hid': 1234,
                'incuts': 'ml,ml3,header',
            },
            exp_flags={
                'market_madv_filter_foreign_incuts_on_header': 0,
                'market_madv_prioritize_main_incuts_on_header': 0,
            },
        )

        # ???????????????????????? ???????????? ?? ?????????????? ??????????????
        self.assertFragmentIn(response, high_bid_incut_entities)

        # ???????????????? ???????????????????? ?????????????????? ?? ????????
        response = self.request(
            {
                'hid': 1234,
                'incuts': 'ml,ml3,header',
            },
            exp_flags={'market_madv_filter_foreign_incuts_on_header': True},
        )

        # ???????????????????????? ????????????, ?????? ?????????????? ???????????? ?????????????????? ????????????????
        self.assertFragmentIn(response, prim_target_incut_entities)

        # ?????????????????????? ???????????? ?????? ???????????????????? ??????????, ???????? ???? ???????????? ????????????
        response = self.request(
            {
                'hid': 1234,
                'incuts': 'ml,ml3,search-block',
            },
            exp_flags={'market_madv_filter_foreign_incuts_on_header': True},
        )

        # ???????????????????????? ????????????, ?????? ?????????????? ???????????? ?????????????????? ????????????????
        self.assertFragmentIn(response, high_bid_incut_entities)

    def test_main_incuts_crutch_for_header_off(self):
        # ?????????????????? ????????, ???????????????????????????????? ???????????? ???? ?????????????????? ????????????,
        # ?????? ?????????? ?????????????? ?????????????????????? ???????????? ?? ?????????????? ??????????????
        response = self.request(
            {
                'hid': 1234,
                'incuts': 'ml,ml3,header',
            },
            exp_flags={
                'market_madv_filter_foreign_incuts_on_header': 0,
                'market_madv_prioritize_main_incuts_on_header': 0,
            },
        )
        self.assertFragmentIn(
            response,
            {
                'incutLists': [
                    [
                        {
                            'entity': 'incut',
                            'id': '1',
                        },
                        {
                            'entity': 'incut',
                            'id': '2',
                        },
                    ]
                ],
                'entities': {
                    'incut': {
                        '1': {
                            'vendor': {
                                'entity': 'vendor',
                                'id': '1',
                            },
                            'bidInfo': {
                                'bid': 90,
                                'clickPrice': 46,
                            },
                        },
                    },
                    'vendor': {
                        '1': {
                            'vendorId': 2345,
                        },
                    },
                },
            },
        )

    def test_main_incuts_crutch_for_header_on(self):
        # ?? ???????????? ?????????????? ???????????? ???? ???????????????? ??????????????????, ?????????????????? ????.
        # ?????????????????????? ???????????? ????????????????????, ???????????????????? ?????????? ????
        response = self.request(
            {
                'hid': 1234,
                'incuts': 'ml,ml3,header',
            },
            exp_flags={
                'market_madv_filter_foreign_incuts_on_header': 0,
                'market_madv_prioritize_main_incuts_on_header': 1,
            },
        )
        self.assertFragmentIn(
            response,
            {
                'incutLists': [
                    [
                        {
                            'entity': 'incut',
                            'id': '1',
                        },
                        {
                            'entity': 'incut',
                            'id': '2',
                        },
                    ]
                ],
                'entities': {
                    'incut': {
                        '1': {
                            'vendor': {
                                'entity': 'vendor',
                                'id': '1',
                            },
                            'bidInfo': {
                                'bid': 45,
                                'clickPrice': T.default_rp,
                            },
                        },
                        '2': {
                            'vendor': {
                                'entity': 'vendor',
                                'id': '2',
                            },
                            'bidInfo': {
                                'bid': 90,
                                'clickPrice': T.default_rp,
                            },
                        },
                    },
                    'vendor': {
                        '1': {
                            'vendorId': 2346,
                        },
                        '2': {
                            'vendorId': 2345,
                        },
                    },
                },
            },
        )

    def test_main_incuts_crutch_for_header_search(self):
        # ?????? ?????????????? ???????????? ?????? ???????????? ???????? ???? ????????????????
        response = self.request(
            {
                'hid': 1234,
                'incuts': 'ml,ml3,search-block',
            },
            exp_flags={
                'market_madv_filter_foreign_incuts_on_header': 0,
                'market_madv_prioritize_main_incuts_on_header': 1,
            },
        )
        self.assertFragmentIn(
            response,
            {
                'incutLists': [
                    [
                        {
                            'entity': 'incut',
                            'id': '1',
                        },
                        {
                            'entity': 'incut',
                            'id': '2',
                        },
                    ]
                ],
                'entities': {
                    'incut': {
                        '1': {
                            'vendor': {
                                'entity': 'vendor',
                                'id': '1',
                            },
                            'bidInfo': {
                                'bid': 90,
                                'clickPrice': 46,
                            },
                        },
                    },
                    'vendor': {
                        '1': {
                            'vendorId': 2345,
                        },
                    },
                },
            },
        )


if __name__ == '__main__':
    env.main()
