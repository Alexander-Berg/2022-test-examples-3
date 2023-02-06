#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import BlueOffer, GLParam, GLType, MarketSku, Model, ModelGroup, Offer


class T(TestCase):
    @classmethod
    def prepare_sku_stats(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.gltypes += [
            GLType(param_id=201, hid=1, cluster_filter=True, gltype=GLType.ENUM, values=[1, 2]),
        ]

        # Модель с синими и зелёными офферами.
        # Офферы не должны находиться по запросу 'Polaris PIR 2695AK', потому что нам нужно убедиться, что мы подсчитаем
        # sku по запросу в базовые за дефолт офферми.
        cls.index.models += [
            Model(hyperid=1, hid=1, title='Утюг Polaris PIR 2695AK'),
        ]
        cls.index.mskus += [
            MarketSku(hyperid=1, sku=1, blue_offers=[BlueOffer()], glparams=[GLParam(param_id=201, value=1)]),
            MarketSku(hyperid=1, sku=2, blue_offers=[BlueOffer()], glparams=[GLParam(param_id=201, value=2)]),
        ]
        cls.index.offers += [
            Offer(hyperid=1),
        ]

        cls.index.model_groups += [
            ModelGroup(title='Утюг Philips GC2990/20 PowerLife', hid=1, hyperid=7),
        ]
        cls.index.models += [
            Model(hid=1, group_hyperid=7),
        ]

    def test_sku_stats__ask_simple_model(self):
        """
        Проверяем случай, когда запрос за ДО происходит в этой ветке
        https://a.yandex-team.ru/arc/trunk/arcadia/market/report/src/place/prime/prime_base.cpp?rev=4510852#L1248-1254
        """
        response = self.report.request_json("place=prime&use-default-offers=1&text=Polaris+PIR+2695AK")
        self.assertFragmentIn(
            response,
            {
                "entity": "product",
                "id": 1,
                "skuStats": {
                    "totalCount": 2,
                    "beforeFiltersCount": 2,
                    "afterFiltersCount": 2,
                },
            },
        )

    def test_sku_stats__ask_simple_and_group_models(self):
        """
        Проверяем случай, когда запрос за ДО происходит в этой ветке
        https://a.yandex-team.ru/arc/trunk/arcadia/market/report/src/place/prime/prime_base.cpp?rev=4510852#L1257-1271
        """
        response = self.report.request_json("place=prime&use-default-offers=1&text=утюг")
        self.assertFragmentIn(
            response,
            {
                "entity": "product",
                "id": 1,
                "skuStats": {
                    "totalCount": 2,
                    "beforeFiltersCount": 2,
                    "afterFiltersCount": 2,
                },
            },
        )

    def test_sku_stats__ask_simple_model_with_filters(self):
        response = self.report.request_json(
            "place=prime&use-default-offers=1&text=Polaris+PIR+2695AK&glfilter=201:1&hid=1"
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "product",
                "id": 1,
                "skuStats": {
                    "totalCount": 2,
                    "beforeFiltersCount": 2,
                    "afterFiltersCount": 1,
                },
            },
        )

    @classmethod
    def prepare_fixed_response(cls):
        """Некоторые hyperid захардкожены чтобы их отдавать по запросу [хочу купить подарок]"""
        cls.index.models += [
            Model(title="Чайник", hyperid=1731984537),
            Model(title="Электронная книга", hyperid=142792001),
            Model(title="Фитнес-браслет", hyperid=649939022),  # фитнесбраслет не в продаже
            Model(title="Кружка в подарок", hyperid=38383838),  # кружка не входит в список подарков
        ]

        cls.index.offers += [
            Offer(hyperid=1731984537),
            Offer(hyperid=142792001),
            Offer(hyperid=38383838),
        ]

    def test_alice_podarok(self):
        """При запросе из алисы по фразе "хочу купить подарок"
        Делается запрос в репорт с текстом [подарок]
        По которому захардкожена подборка подарков
        (без флага alice=1 ищем просто по тексту)
        """

        response = self.report.request_json('place=prime&text=подарок&alice=1&allow-collapsing=1')
        self.assertFragmentIn(
            response,
            {'search': {'results': [{'titles': {'raw': 'Чайник'}}, {'titles': {'raw': 'Электронная книга'}}]}},
            allow_different_len=False,
        )

        response = self.report.request_json('place=prime&text=подарок&allow-collapsing=1')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'titles': {'raw': 'Кружка в подарок'}},
                    ]
                }
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
