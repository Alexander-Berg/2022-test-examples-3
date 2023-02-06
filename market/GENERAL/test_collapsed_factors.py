#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import HyperCategory, HyperCategoryType, Model, Offer


# В данном классе должен быть только один тест,
# чтобы исключить появление записей в фича логе
# от других тестов
class T(TestCase):
    @classmethod
    def prepare(cls):
        '''
        Подготовка данных для проверки записи факторов схлопнытых документов
        '''
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        # Создаем гуру категорию, одну модель в ней оффер, привязанный к этой модели
        # заголовок оффера и модели должны различаться
        # для оффера явно прописываем waremd5
        cls.index.hypertree += [
            HyperCategory(hid=10, output_type=HyperCategoryType.GURU),
        ]

        cls.index.offers += [Offer(title='keyword', waremd5='07DMtqGjKyWUCE8NpmhhXQ', hid=10, hyperid=20, price=100)]

        cls.index.models += [Model(title='model title', hid=10, hyperid=20)]

    def test_collapsed_factors(self):
        '''
        Проверка записи факторов схлопнутых документов
        '''

        # задаем запрос с текстом из заголовка оффера в place prime
        # с флагом включения схлопывания
        response = self.report.request_json('place=prime&text=keyword&allow-collapsing=1')

        # проверем, что в выдаче одна модель (на которую был заменен оффер)
        self.assertFragmentIn(
            response,
            {
                "total": 1,
                "totalOffers": 0,
                "totalModels": 1,
                "results": [
                    {"entity": "product", "titles": {"raw": "model title"}},
                ],
            },
        )

        # проверяем, что в фича лог записаны базовые факторы
        # и что эти факторы от оффера, а не от модели
        # на примере фактора offer_price
        # также проверяем, что был записан фактор схлопывания
        # и заданный ранее ware md5
        self.feature_log.expect(offer_collapsed=1, offer_price=100, ware_md5='07DMtqGjKyWUCE8NpmhhXQ')

    def test_collapsed_in_special_cases(self):
        """проверяем что документ схлопывается и данные наследуются при разных специпальных флагах"""
        response = self.report.request_json('place=prime&text=keyword&allow-collapsing=1&show-shops=all&debug=da')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "titles": {"raw": "model title"}, "debug": {"isCollapsed": True}},
                ]
            },
        )


if __name__ == '__main__':
    main()
