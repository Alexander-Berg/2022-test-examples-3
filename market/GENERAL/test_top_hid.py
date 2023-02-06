#!/usr/bin/env python
# -*- coding: utf-8 -*-

import __classic_import  # noqa
import market.media_adv.incut_search.mt.env as env
from market.media_adv.incut_search.beam.incut import IncutModelsList
from market.media_adv.incut_search.beam.model import ModelWithBid


class T(env.MediaAdvIncutSearchSuite):
    @classmethod
    def setUpClass(cls):
        """
        переопределенный метод для дополнительного вызова настроек
        """
        cls.settings.access_using = True
        super(T, cls).setUpClass()

    @classmethod
    def prepare_top_hid(cls):
        cls.content.incuts += [
            IncutModelsList(
                hid=1234,
                vendor_id=2345,
                datasource_id=10,
                models=[ModelWithBid(model_id=1000 + i) for i in range(1, 4)],
                bid=90,
            ),
            IncutModelsList(
                hid=1235,
                vendor_id=2346,
                datasource_id=11,
                models=[ModelWithBid(model_id=1010 + i) for i in range(1, 4)],
                bid=45,
            ),
        ]

    def test_top_hid(self):
        response = self.request(
            {
                'hid': 1234,
                'top_hid': 1235,
            },
            exp_flags={
                'exp_flag_one': True,
                'exp_flag_two': 'asd',
            },
        )
        # возвращается врезка с hid = top_hid
        self.assertFragmentIn(
            response,
            {
                'incutLists': [[
                    {
                        'entity': 'incut',
                        'id': '1',
                    }
                ]],
                'entities': {
                    'model': {
                        '1': {
                            'modelId': 1011,
                        },
                        '2': {
                            'modelId': 1012,
                        },
                        '3': {
                            'modelId': 1013,
                        },
                    },
                    'incut': {
                        '1': {
                            'incutType': 'ModelsList',
                            'saasRequestHid': 1235,
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
                            'vendorId': 2346,
                        },
                    },
                },
            },
        )


if __name__ == '__main__':
    env.main()
