#!/usr/bin/env python
# -*- coding: utf-8 -*-

import __classic_import  # noqa
import market.media_adv.incut_search.mt.env as env

from market.media_adv.incut_search.beam.incut import IncutModelsList
from market.media_adv.incut_search.beam.model import ModelWithBid


class T(env.MediaAdvIncutSearchSuite):
    base_bid = 90  # Базовая ставка
    incut_opponent_target_page = 0  # Врезка-кандидат для отображения на заданной позиции
    incut_opponent_vendor_ids = 0  # Врезка-кандидат для отображения в ККМ для заданного vendor_id

    @classmethod
    def prepare_incuts_target_page_tests(cls):
        """
        Подготовка врезок:
        Создаем врезку с заданным параметрами отображения target_page
        """
        start_hid = 7
        start_vendor_id = 10
        start_datasource_id = 10
        start_model_id = 10
        cls.content.incuts += [
            # врезка для поиска (search)
            IncutModelsList(
                hid=start_hid,
                vendor_id=start_vendor_id,
                datasource_id=start_datasource_id,
                bid=cls.base_bid,
                page=0,
                models=[ModelWithBid(model_id=start_model_id + z) for z in range(1, 7)],
            ),
            # врезка для поиска (modelcard)
            IncutModelsList(
                hid=start_hid + 1,
                vendor_id=start_vendor_id + 1,
                datasource_id=start_datasource_id + 1,
                bid=cls.base_bid + 10,
                page=1,
                models=[ModelWithBid(model_id=start_model_id + z) for z in range(1, 7)],
            ),
        ]
        # врезка-кандидат
        cls.incut_opponent_target_page = IncutModelsList(
            hid=start_hid + 2,
            vendor_id=start_vendor_id + 2,
            datasource_id=start_datasource_id + 2,
            bid=cls.base_bid + 20,
            models=[ModelWithBid(model_id=start_model_id + z * 20) for z in range(1, 4)],
        )

    def test_page_search_exist_incut(self):
        """
        Тест 1.
        Хотим отбражаться в категории 7.
        Запрашиваем ставки для врезки, что должна отображаться в поиске.
        Сейчас в SaaS заведена врезка для поиска.
        Поэтому ответ для мин = РП, для макс: ставка врезки из SaaS + 1 (91)
        """
        response = self.request(
            {
                'target_hids': '7',
                'target_page': 'search',
                'saas_incut': self.incut_opponent_target_page.serialize(),
            },
            exp_flags={},
            handler='recommends',
        )
        self.assertFragmentIn(
            response,
            {
                'result': {
                    '7': {
                        'maxBid': 91,
                        'minBid': T.default_rp,
                    },
                }
            },
        )

    def test_page_search_no_incut(self):
        """
        Тест 2.
        Хотим отбражаться в категории 9.
        Запрашиваем ставки для врезки, что должна отображаться в поиске.
        Сейчас в SaaS заведена врезка для поиска, но для другой категории.
        Поэтому ответ для мин и макс = РП
        """
        response = self.request(
            {
                'target_hids': '9',
                'target_page': 'search',
                'saas_incut': self.incut_opponent_target_page.serialize(),
            },
            exp_flags={},
            handler='recommends',
        )
        self.assertFragmentIn(
            response,
            {
                'result': {
                    '9': {
                        'maxBid': T.default_rp,
                        'minBid': T.default_rp,
                    },
                }
            },
        )

    def test_page_modelcard_exist_incut(self):
        """
        Тест 3.
        Хотим отбражаться в категории 8.
        Запрашиваем ставки для врезки, что должна отображаться на ККМ.
        Сейчас в SaaS заведена врезка для ККМ.
        Поэтому ответ для мин = РП, для макс: ставка врезки из SaaS + 1 (101)
        """
        response = self.request(
            {
                'target_hids': '8',
                'target_page': 'modelcard',
                'saas_incut': self.incut_opponent_target_page.serialize(),
            },
            exp_flags={},
            handler='recommends',
        )
        self.assertFragmentIn(
            response,
            {
                'result': {
                    '8': {
                        'maxBid': 101,
                        'minBid': T.default_rp,
                    },
                }
            },
        )

    def test_page_modelcard_no_incut(self):
        """
        Тест 4.
        Хотим отбражаться в категории 9.
        Запрашиваем ставки для врезки, что должна отображаться на ККМ.
        Сейчас в SaaS заведена врезка для ККМ, но для категории 8.
        Поэтому ответ для мин и макс = РП
        """
        response = self.request(
            {
                'target_hids': '9',
                'target_page': 'modelcard',
                'saas_incut': self.incut_opponent_target_page.serialize(),
            },
            exp_flags={},
            handler='recommends',
        )
        self.assertFragmentIn(
            response,
            {
                'result': {
                    '9': {
                        'maxBid': T.default_rp,
                        'minBid': T.default_rp,
                    },
                }
            },
        )

    @classmethod
    def prepare_incuts_vendor_ids_test(cls):
        start_hid = 17
        start_vendor_id = 50
        start_datasource_id = 50
        start_model_id = 50
        # врезка в SaaS для отображения на ККМ
        cls.content.incuts += [
            IncutModelsList(
                hid=start_hid,
                vendor_id=start_vendor_id,
                datasource_id=start_datasource_id,
                bid=cls.base_bid,
                page=1,
                vendor_ids=[8800, 55535, 3500, 1315],
                models=[ModelWithBid(model_id=start_model_id + z) for z in range(1, 7)],
            )
        ]
        # врезка-кандидат для отображения на ККМ
        cls.incut_opponent_vendor_ids = IncutModelsList(
            hid=start_hid + 2,
            vendor_id=start_vendor_id + 2,
            datasource_id=start_datasource_id + 2,
            bid=cls.base_bid + 10,
            page=1,
            vendor_ids=[8800, 42422, 9090, 112911],
            models=[ModelWithBid(model_id=start_model_id + z) for z in range(1, 7)],
        )

    def test_same_hid_same_vendor_ids(self):
        """
        Тест 5.
        Проверяем, что при запросе по категории, для которой уже есть врезка и указаны такие же vendor_ids
        вернется ответ: мин = РП, макс = ставка врезки из SaaS + 1.
        """
        response = self.request(
            {
                'target_hids': '17',
                'target_page': 'modelcard',
                'vendor_ids': '8800,55535,3500,1315,42422,9090,112911',
                'saas_incut': self.incut_opponent_vendor_ids.serialize(),
            },
            exp_flags={},
            handler='recommends',
        )
        self.assertFragmentIn(
            response,
            {
                'result': {
                    '17': {
                        'maxBid': 91,
                        'minBid': T.default_rp,
                    },
                }
            },
        )

    def test_same_hid_diff_vendor_ids(self):
        """
        Тест 6.
        Проверяем, что при запросе для категории, которой есть врезка в SaaS, но другие vendor_ids
        вернется ответ: для мин и макс будет РП.
        """
        response = self.request(
            {
                'target_hids': '17',
                'target_page': 'modelcard',
                'vendor_ids': '42422,9090,112911',
                'saas_incut': self.incut_opponent_vendor_ids.serialize(),
            },
            exp_flags={},
            handler='recommends',
        )
        self.assertFragmentIn(
            response,
            {
                'result': {
                    '17': {
                        'maxBid': T.default_rp,
                        'minBid': T.default_rp,
                    },
                }
            },
        )

    def test_diff_hid_same_vendor_ids(self):
        """
        Тест 7.
        Проверка, что при запросе  для категории у которой нет врезки в SaaS,
        но такие же vendor_ids как у врезки в SaaS (hid = 17)
        должно вернуться мин и макс = РП.
        """
        response = self.request(
            {
                'target_hids': '19',
                'target_page': 'modelcard',
                'vendor_ids': '8800,55535,3500,1315',
                'saas_incut': self.incut_opponent_vendor_ids.serialize(),
            },
            exp_flags={},
            handler='recommends',
        )
        self.assertFragmentIn(
            response,
            {
                'result': {
                    '19': {
                        'maxBid': T.default_rp,
                        'minBid': T.default_rp,
                    },
                }
            },
        )

    def test_diff_hid_diff_vendor_ids(self):
        """
        Тест 8.
        Проверка, что при запросе для категории, для которой нет врезки в SaaS,
        vendor_ids отличны от врезки, что уже есть в SaaS
        вернется ответ: мин и макс = РП.
        """
        response = self.request(
            {
                'target_hids': '19',
                'target_page': 'modelcard',
                'vendor_ids': '42422,9090,112911',
                'saas_incut': self.incut_opponent_vendor_ids.serialize(),
            },
            exp_flags={},
            handler='recommends',
        )
        self.assertFragmentIn(
            response,
            {
                'result': {
                    '19': {
                        'maxBid': T.default_rp,
                        'minBid': T.default_rp,
                    },
                }
            },
        )


if __name__ == '__main__':
    env.main()
