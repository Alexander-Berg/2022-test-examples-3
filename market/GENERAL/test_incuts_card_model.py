#!/usr/bin/env python
# -*- coding: utf-8 -*-

import __classic_import  # noqa
import market.media_adv.incut_search.mt.env as env

from market.media_adv.incut_search.beam.incut import IncutModelsList
from market.media_adv.incut_search.beam.model import ModelWithBid
from market.pylibrary.lite.matcher import ElementCount
from unittest import skip


class T(env.MediaAdvIncutSearchSuite):
    @classmethod
    def setUpClass(cls):
        """
        переопределенный метод для дополнительного вызова настроек
        """
        cls.settings.access_using = True
        super(T, cls).setUpClass()

    @classmethod
    def prepare_invalid_incuts_in_request(cls):
        cls.content.incuts += [
            IncutModelsList(
                hid=101,
                vendor_id=1,
                datasource_id=11,
                models=[ModelWithBid(model_id=1000 + i) for i in range(1, 10)],
                bid=100,
            ),
        ]

    @skip
    def test_invalid_incuts_in_request(self):
        params = {
            'hid': 101,
            'incuts': 'model-card',  # неправильный запрос
            'target_page': 'modelcard',
        }
        flags = {
            'market_madv_fixed_incuts_for_model_card': 0,
        }

        # пустой ответ при неправильном запросе врезок
        response = self.request(params, exp_flags=flags, debug=1)
        self.assertFragmentIn(
            response,
            {
                "entities": {
                    "incut": {
                        "1": {
                            "incutType": "Empty",
                        },
                    },
                },
                "incutLists": ElementCount(1),
            },
        )

        # включение флага для получения врезок при неккоректном запросе
        flags['market_madv_fixed_incuts_for_model_card'] = 1
        response = self.request(params, exp_flags=flags)
        self.assertFragmentIn(
            response,
            {
                'incutLists': [
                    [
                        {
                            'entity': 'incut',
                            'id': '1',
                        }
                    ]
                ],
                'entities': {
                    'incut': {
                        '1': {
                            'incutType': 'ModelsList',
                        },
                    },
                },
            },
        )


if __name__ == '__main__':
    env.main()
