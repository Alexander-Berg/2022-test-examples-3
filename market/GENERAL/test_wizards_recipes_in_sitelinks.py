#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    CardCategory,
    GLFilterEnum,
    GLFilterNumeric,
    GLType,
    HyperCategory,
    HyperCategoryType,
    NavCategory,
    Recipe,
    Vendor,
)
from core.testcase import TestCase, main
from core.matcher import LikeUrl, NoKey, ElementCount

# Тесты на проверку новой функциональности - добавление рецептов в колдунщики


class T(TestCase):
    @classmethod
    def prepare_guru_category_universal(cls):
        cls.index.hypertree += [
            HyperCategory(hid=3, name='spoons', output_type=HyperCategoryType.GURU),
            HyperCategory(hid=4, name='forks', output_type=HyperCategoryType.GURU),
            HyperCategory(hid=7, name='knives', output_type=HyperCategoryType.GURU),
        ]

        cls.index.navtree += [NavCategory(hid=3, nid=33), NavCategory(hid=4, nid=44)]

        cls.index.vendors += [
            Vendor(vendor_id=10),
            Vendor(vendor_id=11),
            Vendor(vendor_id=12),
            Vendor(vendor_id=13),
            Vendor(vendor_id=14),
            Vendor(vendor_id=15),
        ]

        # Без привязанных к категориям вендоров не сформируются категорийные колдунщики
        cls.index.cards += [
            CardCategory(hid=3, vendor_ids=[10, 11, 12]),
            CardCategory(hid=4, vendor_ids=[13, 14, 15]),
            CardCategory(hid=7, vendor_ids=[10, 11, 12]),
        ]

        cls.index.gltypes += [
            GLType(param_id=105, hid=3, gltype=GLType.ENUM, values=[1024, 1025], unit_name='Материал'),
            GLType(param_id=106, hid=7, gltype=GLType.NUMERIC, unit_name='Размер'),
        ]

        cls.index.recipes += [
            Recipe(
                recipe_id=1006,
                hid=3,
                name='Silver spoons',
                short_name='Silver',
                popularity=100,
                glfilters=[GLFilterEnum(param_id=105, values=[1025])],
            ),
        ]

        # Добавляем более 15 рецептов к категории knives
        RECIPES_COUNT = 16
        cls.index.recipes += [
            Recipe(
                recipe_id=1011 + i,
                hid=7,
                name='Size {0} knives'.format(i),
                short_name='Size {0}'.format(i),
                popularity=100 - i,
                glfilters=[GLFilterNumeric(param_id=106 + i, min_value=i)],
            )
            for i in range(RECIPES_COUNT)
        ]

    def test_guru_category_universal(self):
        """Рецепты в колдунщике гуру-категории.
        Пробрасываются только под конструктор.

        https://st.yandex-team.ru/MARKETOUT-12973
        """

        # Сценарий:
        # запрос в категорию с рецептами ("spoons")
        # в sitelinks будут ссылки на рецепты

        response = self.report.request_bs(
            'place=parallel&text=spoons&rearr-factors=market_recipes_in_guru_category_wizard=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_ext_category": [
                    {
                        "title": {"__hl": {"text": "Spoons на Маркете", "raw": True}},
                        "url": LikeUrl.of('//market.yandex.ru/catalog--spoons/33?hid=3&clid=500'),
                        "sitelinks": [
                            {
                                "text": "Silver",
                                "url": LikeUrl.of(
                                    '//market.yandex.ru/search?glfilter=105%3A1025&hid=3&nid=33&clid=500'
                                ),
                            }
                        ],
                    }
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # тот же запрос без флага - нет рецептов
        # пустой блок sitelinks
        response = self.report.request_bs('place=parallel&text=spoons')
        self.assertFragmentIn(
            response,
            {
                "market_ext_category": [
                    {
                        "title": {"__hl": {"text": "Spoons на Маркете", "raw": True}},
                        "url": LikeUrl.of('//market.yandex.ru/catalog--spoons/33?hid=3&clid=500'),
                        "sitelinks": NoKey("sitelinks"),
                    }
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Сценарий:
        # запрос в гуру-категорию без рецептов ("forks")
        # пустой блок sitelinks

        response = self.report.request_bs(
            'place=parallel&text=forks&rearr-factors=market_recipes_in_guru_category_wizard=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_ext_category": [
                    {
                        "title": {"__hl": {"text": "Forks на Маркете", "raw": True}},
                        "url": LikeUrl.of('//market.yandex.ru/catalog--forks/44?hid=4&clid=500'),
                        "sitelinks": NoKey("sitelinks"),
                    }
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # тот же запрос без флага - нет рецептов
        # пустой блок sitelinks
        response = self.report.request_bs('place=parallel&text=forks')
        self.assertFragmentIn(
            response,
            {
                "market_ext_category": [
                    {
                        "title": {"__hl": {"text": "Forks на Маркете", "raw": True}},
                        "url": LikeUrl.of('//market.yandex.ru/catalog--forks/44?hid=4&clid=500'),
                        "sitelinks": NoKey("sitelinks"),
                    }
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_guru_category_universal_recipes_count(self):
        """Проверяем наличие 15 рецептов в колдунщике гуру-категории под конструктором.
        https://st.yandex-team.ru/MARKETOUT-14062
        """
        response = self.report.request_bs(
            'place=parallel&text=knives&rearr-factors=market_recipes_in_guru_category_wizard=1'
        )
        self.assertFragmentIn(
            response,
            {
                "market_ext_category": [
                    {"title": {"__hl": {"text": "Knives на Маркете", "raw": True}}, "sitelinks": ElementCount(15)}
                ]
            },
        )


if __name__ == "__main__":
    main()
