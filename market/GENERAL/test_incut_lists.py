#!/usr/bin/env python
# -*- coding: utf-8 -*-

import __classic_import     # noqa
import market.media_adv.incut_search.mt.env as env

from market.media_adv.incut_search.beam.incut import IncutModelsList
from market.media_adv.incut_search.beam.model import ModelWithBid


class T(env.MediaAdvIncutSearchSuite):

    @classmethod
    def prepare_incut_lists(cls):
        cls.content.incuts += [
            IncutModelsList(
                id=2001 + i,
                hid=1001,
                vendor_id=2001 + i,
                datasource_id=2001 + i,
                models=[
                    ModelWithBid(model_id=1000 + m + i * 10) for m in range(4 + i)
                ],
                bid=90 - i * 10,
            ) for i in range(10)
        ]

    # проверим, что incutLists приходит без дополнительных врезок-кандидатов
    def test_incut_list_no_extra(self):
        response = self.request({
            'hid': 1001,
            'frontend': 'touch',
            'incuts': 'ml',
        }, exp_flags={
            'market_madv_extra_incuts_in_lists': 0,
        })
        self.assertFragmentIn(response, {
            'incutLists': [[{
                'entity': 'incut',
                'id': '1',
            }]],
            'entities': {
                'incut': {
                    '1': {
                        'saasId': 2001,
                    },
                },
            },
        }, allow_different_len=False)

    # проверим, что для десктопа обычная врезка вернется без двух первых вендоров,
    # а узкая (из 3 моделей) - с ними, каждый incutList будет из трех врезок
    def test_incut_list_2_places_2_extra(self):
        response = self.request({
            'hid': 1001,
            'frontend': 'desktop',
            'incuts': 'ml;ml3',
        }, exp_flags={
            'market_madv_extra_incuts_in_lists': 2,
        })
        self.assertFragmentIn(response, {
            'incutLists': [[{
                'entity': 'incut',
                'id': '1',
            }, {
                'entity': 'incut',
                'id': '2',
            }, {
                'entity': 'incut',
                'id': '3',
            }], [{
                'entity': 'incut',
                'id': '4',
            }, {
                'entity': 'incut',
                'id': '5',
            }, {
                'entity': 'incut',
                'id': '6',
            }]],
            'entities': {
                'incut': {
                    '1': {
                        'saasId': 2003,
                    },
                    '2': {
                        'saasId': 2004,
                    },
                    '3': {
                        'saasId': 2005,
                    },
                    '4': {
                        'saasId': 2001,
                    },
                    '5': {
                        'saasId': 2002,
                    },
                    '6': {
                        'saasId': 2003,
                    },
                },
            },
        }, allow_different_len=False)

    # проверим, что при передаче количества врезок в плейсменте
    # вернется больше кандидатов
    def test_incut_list_2_places_2_extra_2_count(self):
        response = self.request({
            'hid': 1001,
            'frontend': 'desktop',
            'incuts': 'ml;ml3,search-block,2',
        }, exp_flags={
            'market_madv_extra_incuts_in_lists': 2,
        })
        self.assertFragmentIn(response, {
            'incutLists': [[{
                'entity': 'incut',
                'id': '1',
            }, {
                'entity': 'incut',
                'id': '2',
            }, {
                'entity': 'incut',
                'id': '3',
            }], [{
                'entity': 'incut',
                'id': '4',
            }, {
                'entity': 'incut',
                'id': '5',
            }, {
                'entity': 'incut',
                'id': '6',
            }, {
                'entity': 'incut',
                'id': '7',
            }, {
                'entity': 'incut',
                'id': '8',
            }, {
                'entity': 'incut',
                'id': '9',
            }]],
            'entities': {
                'incut': {
                    '1': {
                        'saasId': 2003,
                    },
                    '2': {
                        'saasId': 2004,
                    },
                    '3': {
                        'saasId': 2005,
                    },
                    '4': {
                        'saasId': 2001,
                    },
                    '5': {
                        'saasId': 2002,
                    },
                    '6': {
                        'saasId': 2003,
                    },
                    '7': {
                        'saasId': 2004,
                    },
                    '8': {
                        'saasId': 2005,
                    },
                    '9': {
                        'saasId': 2006,
                    },
                },
            },
        }, allow_different_len=False)


if __name__ == '__main__':
    env.main()
