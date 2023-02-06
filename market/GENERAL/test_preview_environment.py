#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.testcase import TestCase, main
from core.types import Model, Offer
from core.matcher import Regex, ListMatcher, Not


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.index.models += [
            Model(title='conflicting hid model from production', hid=1, hyperid=100),
            Model(title='conflicting hid production only model', hid=1, hyperid=101),
            Model(title='production hid model', hid=2),
        ]

        cls.index.offers += [Offer(hyperid=100, title="offer for conflicting model")]

        cls.index.preview_models += [
            Model(title='conflicting hid model from preview', hid=1, hyperid=100),
            Model(title='conflicting hid preview only model', hid=1, hyperid=102),
            Model(title='preview hid model', hid=3),
        ]

    def test_fetch_production_hid_model(self):
        """
        Модель не из превью-категории, которая находится только в обычной коллекции, находится при поиске
        """

        response = self.report.request_json('place=prime&text=model')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "production hid model"},
                    }
                ]
            },
        )

    def test_fetch_preview_hid_model(self):
        """
        Модель из превью-категории, которая находится только в превью коллекции, находится при поиске
        """

        response = self.report.request_json('place=prime&text=model')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "preview hid model"},
                    }
                ]
            },
        )

    def test_fetch_conflicting_hid_model_mainrelevance(self):
        """
        Модель из превью-категории, которая находится и в превью-коллекции и в обычной коллекции.
        Оффер к этой модели находится только в обычной коллекции.
        Тестируем, что:
        1. Модель из превью коллекции находится
        2. Оффер к ней из ОБЫЧНОЙ коллекции тоже находится
        3. Модель из обычной коллекции НЕ находится
        4. Тестируем это на плейсе, где работает ERelevanceName::Main
        """

        response = self.report.request_json('place=prime&text=model&debug=da')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "conflicting hid model from preview"},
                    },
                    {"titles": {"raw": "offer for conflicting model"}},
                ]
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "conflicting hid model from production"},
                    }
                ]
            },
        )

        # Проверяем запрос в коллекцию
        regex = Regex("preview_hids:")
        self.assertFragmentIn(
            response,
            {
                'how': [
                    {
                        'collections': ListMatcher(expected=['MODEL'], unexpected=['PREVIEW_MODEL']),
                        'args': regex,
                    }
                ]
            },
        )
        self.assertFragmentIn(
            response,
            {
                'how': [
                    {
                        'collections': ListMatcher(expected=['PREVIEW_MODEL'], unexpected=['MODEL']),
                        'args': Not(regex),
                    }
                ]
            },
        )

    def test_fetch_conflicting_hid_model_trivialrelevance(self):
        """
        Модель из превью-категории, категория есть и в превью-коллекции и в обычной коллекции.
        Тестируем, что:
        1. Модель из превью коллекции находится (hyperid=102)
        2. Модель из обычной коллекции НЕ находится (hyperid=101)
        3. Тестируем это на плейсе, где работает ERelevanceName::TrivialInorder
        """

        response = self.report.request_json('place=modelinfo&hyperid=101&rids=0')

        self.assertFragmentIn(response, {"search": {"total": 0, "results": []}})

        response = self.report.request_json('place=modelinfo&hyperid=102&rids=0')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "conflicting hid preview only model"},
                    }
                ]
            },
        )


if __name__ == '__main__':
    main()
