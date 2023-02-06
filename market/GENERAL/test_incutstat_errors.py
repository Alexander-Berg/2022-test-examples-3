#!/usr/bin/env python
# -*- coding: utf-8 -*-

import __classic_import  # noqa
import market.media_adv.incut_search.mt.env as env
from market.media_adv.incut_search.beam.incut import IncutModelsList
from market.media_adv.incut_search.beam.model import ModelWithBid


class T(env.MediaAdvIncutSearchSuite):
    @classmethod
    def prepare_incuts_for_ok_test(cls):
        cls.content.incuts += [
            IncutModelsList(
                id=707,  # врезка для проверки правильного запроса
                hid=1200,
                vendor_id=90981,
                datasource_id=10,
                models=[ModelWithBid(model_id=1000 + i) for i in range(1, 4)],
                bid=70,
            ),
            IncutModelsList(
                id=808,  # врезка для проверки правильного запроса
                hid=2100,
                vendor_id=90981,
                datasource_id=20,
                models=[ModelWithBid(model_id=2000 + i) for i in range(1, 4)],
                bid=341,
            ),
            IncutModelsList(
                id=3002,  # врезка с истекшим сроком жизни для проверки ответа с ошибкой
                hid=8500,
                vendor_id=785698,
                datasource_id=40,
                models=[ModelWithBid(model_id=4000 + i) for i in range(1, 4)],
                bid=256,
                age=846200,
            ),
            IncutModelsList(
                id=9856,  # врезка с одной моделью для проверки ответа с ошибкой
                hid=8500,
                vendor_id=785698,
                datasource_id=50,
                models=[ModelWithBid(model_id=4005)],
                bid=2506,
            ),
        ]

    def test_all_ok(self):
        """
        Тест 1. Проверяем, для корректных врезок, есть SaaS id и время жизни врезки не истекло,
        ответ возвращается со статистикой для каждой врезки
        """
        response = self.request(
            {
                'incut_ids': '707,808',
                'rids': '1,2,3',
            },
            exp_flags={
                'market_madv_model_regional_stats_enabled': 0,
            },
            handler='incutstat',
        )
        self.assertFragmentIn(
            response,
            {
                'IncutStat': {
                    '707': {
                        'SaasId': '707',
                        'ModelIds': ['1001', '1002', '1003'],
                        'RegionStat': {'1': 'NoFilter', '2': 'NoFilter', '3': 'NoFilter'},
                    },
                    '808': {
                        'SaasId': '808',
                        'ModelIds': ['2001', '2002', '2003'],
                        'RegionStat': {'1': 'NoFilter', '2': 'NoFilter', '3': 'NoFilter'},
                    },
                }
            },
        )

    def test_cant_find_incut_by_saas_id(self):
        """
        Тест 2. По такому SaaS id не нашли врезку - ответе ошибка - NotFound
        """
        response = self.request(
            {
                'incut_ids': '709',
                'rids': '1,2,3',
            },
            exp_flags={},
            handler='incutstat',
        )
        self.assertFragmentIn(
            response,
            {
                'IncutStat': {
                    '709': {
                        'Errors': [
                            'NotFound',
                        ],
                    },
                }
            },
        )

    def test_too_old_incut(self):
        """
        Тест 3. По запрошенному id находится врезка с истекшим сроком жизни,
        в ответ - ошибка TooOld
        """
        response = self.request(
            {
                'incut_ids': '3002',
                'rids': '50,70,90',
            },
            exp_flags={
                'market_madv_saas_incut_age_to_drop': '9000',
            },
            handler='incutstat',
        )
        self.assertFragmentIn(
            response,
            {
                'IncutStat': {
                    '3002': {
                        'Errors': [
                            'TooOld',
                        ],
                    },
                }
            },
        )

    def test_not_enought_models(self):
        """
        Тест 5. По запрошенному id находится врезка, но у нее не достаочно моделей
        в ответе - ошибка NotEnoughModels
        """

        response = self.request(
            {
                'incut_ids': '9856',
                'rids': '113,68,790',
            },
            exp_flags={},
            handler='incutstat',
        )
        self.assertFragmentIn(
            response,
            {
                'IncutStat': {
                    '9856': {
                        'Errors': [
                            'NotEnoughModels',
                        ],
                    },
                }
            },
        )


if __name__ == '__main__':
    env.main()
