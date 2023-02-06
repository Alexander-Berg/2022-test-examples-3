#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.testcase import TestCase, main
from core.matcher import NotEmptyList, Absent, EmptyList
from core.types import (
    GLType,
    HyperCategory,
    Recipe,
    GLFilterBool,
    GLFilterNumeric,
    GLFilterEnum,
    Model,
    Offer,
    GLParam,
    Promo,
    PromoType,
)


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.gltypes += [
            GLType(
                param_id=1,
                hid=1,
                gltype=GLType.ENUM,
                values=list(range(40, 51)),
                cluster_filter=False,
                model_filter_index=3,
            ),
            GLType(param_id=2, hid=1, gltype=GLType.ENUM, values=[1, 2, 3], cluster_filter=False, model_filter_index=1),
            GLType(param_id=3, hid=1, gltype=GLType.BOOL, cluster_filter=False, model_filter_index=2),
            GLType(param_id=4, hid=1, gltype=GLType.ENUM, values=list(range(10)), cluster_filter=False),
            GLType(param_id=5, hid=1, gltype=GLType.NUMERIC),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=1),
            HyperCategory(hid=2),
        ]

        cls.index.recipes += [
            Recipe(
                recipe_id=1,
                hid=1,
                name='Recipe with filters without search query',
                glfilters=[
                    GLFilterBool(param_id=3, value=1),
                ],
            ),
            Recipe(
                recipe_id=2,
                hid=1,
                name='Search recipe without filters',
                search_query='Search query 1',
                popularity=100,
            ),
            Recipe(
                recipe_id=3,
                hid=1,
                name='Search recipe with filters',
                search_query='Search query 2',
                glfilters=[
                    GLFilterNumeric(param_id=5, max_value=100),
                ],
            ),
            Recipe(
                recipe_id=4,
                hid=1,
                name='Recipe 3 without search_query',
                glfilters=[
                    GLFilterEnum(param_id=2, values=[1, 2, 3]),
                ],
            ),
            Recipe(
                recipe_id=5,
                hid=1,
                name='Recipe "iPhone" without filters',
                search_query='iPhone',
            ),
            Recipe(
                recipe_id=6,
                hid=1,
                name='Recipe "iPhone" with discount',
                search_query='iPhone',
                contains_discount=True,
            ),
            Recipe(
                recipe_id=7,
                hid=1,
                name='Recipe "iPhone" with filters',
                search_query='iPhone',
                popularity=100,
                glfilters=[
                    GLFilterBool(param_id=3, value=1),
                ],
            ),
            Recipe(
                recipe_id=8,
                hid=1,
                name='Category recipe',
                contains_discount=True,
            ),
            Recipe(
                recipe_id=9,
                hid=1,
                name='Recipe "iPhone" with filters 2',
                search_query='iPhone',
                glfilters=[
                    GLFilterBool(param_id=3, value=1),
                    GLFilterEnum(param_id=1, values=[41, 42, 43, 44]),
                ],
            ),
            Recipe(
                recipe_id=10,
                hid=1,
                name='Recipe "iPhone" with filters 3',
                search_query='iPhone',
                glfilters=[
                    GLFilterBool(param_id=3, value=1),
                    GLFilterEnum(param_id=4, values=[1, 5]),
                ],
            ),
            Recipe(
                recipe_id=11,
                hid=2,
                name='Recipe "iPhone" from category 2',
                search_query='iPhone',
            ),
            Recipe(
                recipe_id=13,
                hid=1,
                name='Recipe with cutprice',
                search_query='GoodState',
                good_state='cutprice',
            ),
        ]

    def test_get_recipe_by_recipe_id(self):
        """Если в ручку recipe_by_glfilters передан только recipe_id и hid, нужно отдать рецепт с этим ID"""
        # Не поисковый рецепт
        response = self.report.request_json('place=recipe_by_glfilters&recipe-id=1&hid=1')
        self.assertFragmentIn(
            response,
            {
                'entity': 'recipe',
                'id': 1,
                'name': 'Recipe with filters without search query',
                'filters': NotEmptyList(),
            },
        )

        # Поисковый рецепт без фильтров
        response = self.report.request_json('place=recipe_by_glfilters&recipe-id=2&hid=1')
        self.assertFragmentIn(
            response,
            {
                'entity': 'recipe',
                'id': 2,
                'name': 'Search recipe without filters',
                'nonGlFilters': [{'id': 'text', 'values': [{'value': 'Search query 1'}]}],
            },
        )

        # Поисковый рецепт с фильтрами
        response = self.report.request_json('place=recipe_by_glfilters&recipe-id=3&hid=1')
        self.assertFragmentIn(
            response,
            {
                'entity': 'recipe',
                'id': 3,
                'name': 'Search recipe with filters',
                'nonGlFilters': [{'id': 'text', 'values': [{'value': 'Search query 2'}]}],
                'filters': NotEmptyList(),
            },
        )

    def test_get_recipe_with_search_query(self):
        """
        Если задан hid и поисковый запрос (без фильтров), нужно отдать рецепт с таким же поисковым запросом без фильтров
        """
        response = self.report.request_json('place=recipe_by_glfilters&hid=1&text=Search query 1')
        self.assertFragmentIn(
            response,
            {
                'entity': 'recipe',
                'id': 2,
                'name': 'Search recipe without filters',
                'nonGlFilters': [{'id': 'text', 'values': [{'value': 'Search query 1'}]}],
            },
        )

        # Если есть несколько поисковых рецептов с одинаковым запросом, нужно отдать рецепт, совпадающий с запросом по
        # всем параметрам
        response = self.report.request_json('place=recipe_by_glfilters&hid=1&text=iPhone')
        self.assertFragmentIn(
            response,
            {
                'entity': 'recipe',
                'id': 5,
                'name': 'Recipe "iPhone" without filters',
                'filters': Absent(),
                'nonGlFilters': [{'id': 'text', 'values': [{'value': 'iPhone'}]}],
            },
        )

        response = self.report.request_json('place=recipe_by_glfilters&hid=1&text=iPhone&filter-discount-only=1')
        self.assertFragmentIn(
            response,
            {
                'entity': 'recipe',
                'id': 6,
                'name': 'Recipe "iPhone" with discount',
                'filters': Absent(),
                'nonGlFilters': [
                    {'id': 'text', 'values': [{'value': 'iPhone'}]},
                    {
                        'id': 'filter-discount-only',
                        'values': [
                            {
                                'checked': True,
                                'value': 1,
                            }
                        ],
                    },
                ],
            },
        )

        response = self.report.request_json('place=recipe_by_glfilters&hid=1&text=iPhone&glfilter=3:1')
        self.assertFragmentIn(
            response,
            {
                'entity': 'recipe',
                'id': 7,
                'name': 'Recipe "iPhone" with filters',
                'filters': NotEmptyList(),
                'nonGlFilters': [
                    {'id': 'text', 'values': [{'value': 'iPhone'}]},
                ],
            },
        )

        response = self.report.request_json('place=recipe_by_glfilters&hid=1&text=GoodState&good-state=cutprice')
        self.assertFragmentIn(
            response,
            {
                'entity': 'recipe',
                'id': 13,
                'name': 'Recipe with cutprice',
            },
        )

    def test_get_recipe_without_search_query(self):
        """
        Если поисковый запрос задан, но не подходит ни к одному рецепту, нужно искаь рецепт по старой логике среди
        рецептов без поискового запроса
        """
        # В категории нет запросов без фильтров и поисковых запросов с заданной поисковой строкой
        response = self.report.request_json(
            'place=recipe_by_glfilters&hid=1&text=invalid search query&filter-discount-only=1'
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'recipe',
                'id': 8,
                'name': 'Category recipe',
                'filters': Absent(),
                'nonGlFilters': [
                    {
                        'id': 'filter-discount-only',
                        'values': [
                            {
                                'checked': True,
                            }
                        ],
                    },
                ],
            },
        )

        # При поиске с glfilters - рецепт БЕЗ search_query с подходящими фильтрами
        response = self.report.request_json('place=recipe_by_glfilters&hid=1&text=invalid search query&glfilter=3:1')
        self.assertFragmentIn(
            response,
            {
                'entity': 'recipe',
                'id': 1,
                'name': 'Recipe with filters without search query',
                'filters': [
                    {
                        'id': '3',
                        'type': 'boolean',
                    }
                ],
                'nonGlFilters': Absent(),
            },
        )

    def test_get_recipes_without_search_query(self):
        """
        При поиске рецептов без поискового запроса и с фильтрами, нужно отдавать рецепт, полностью подходящий под все
        фильтры
        """
        response = self.report.request_json('place=recipe_by_glfilters&hid=1&glfilter=3:1')
        self.assertFragmentIn(
            response,
            {
                'entity': 'recipe',
                'id': 1,
                'name': 'Recipe with filters without search query',
                'nonGlFilters': Absent(),
                'filters': NotEmptyList(),
            },
        )

    def test_get_unexistent_recipe_id(self):
        """Если запросашивается рецепт с несуществующим id, ошибки быть не должно"""
        self.assertEqualJsonResponses(
            'place=recipe_by_glfilters&recipe-id=100500&hid=1',
            'place=recipe_by_glfilters&hid=1',
        )

    def test_get_recipes_by_search_query(self):
        """
        Если по поисковому запросу находится несколько рецептов (> 2), нужно отдать эти рецепты
        Если <= 2 - отдаем рецепты по старой логике
        """
        # По запросу находится 3 рецепта
        response = self.report.request_json('place=recipes_contain_glfilters&hid=1&text=iPhone')
        self.assertFragmentIn(
            response,
            {
                'recipes': [
                    {
                        'id': 6,
                        'name': 'Recipe "iPhone" with discount',
                        'nonGlFilters': [
                            {'id': 'text', 'values': [{'value': 'iPhone'}]},
                            {
                                'id': 'filter-discount-only',
                            },
                        ],
                    },
                    {
                        'id': 7,
                        'name': 'Recipe "iPhone" with filters',
                        'nonGlFilters': [{'id': 'text', 'values': [{'value': 'iPhone'}]}],
                    },
                    {
                        'id': 5,
                        'name': 'Recipe "iPhone" without filters',
                        'nonGlFilters': [{'id': 'text', 'values': [{'value': 'iPhone'}]}],
                    },
                    {
                        'id': 9,
                        'name': 'Recipe "iPhone" with filters 2',
                        'nonGlFilters': [{'id': 'text', 'values': [{'value': 'iPhone'}]}],
                    },
                    {
                        'id': 10,
                        'name': 'Recipe "iPhone" with filters 3',
                        'nonGlFilters': [{'id': 'text', 'values': [{'value': 'iPhone'}]}],
                    },
                ],
            },
            allow_different_len=False,
        )

        # По запросу находится только 1 рецепт. Выдача должна совпадать с запросом просто по категории
        self.assertEqualJsonResponses(
            'place=recipes_contain_glfilters&hid=1',
            'place=recipes_contain_glfilters&hid=1&text=Search query 1',
        )

    def test_get_recipes_in_category(self):
        """Если в категории есть поисковые рецепты, они тоже должны отдаваться"""
        response = self.report.request_json('place=recipes_contain_glfilters&hid=1')
        self.assertFragmentIn(
            response,
            {
                'recipes': [
                    {
                        'id': 1,
                        'name': 'Recipe with filters without search query',
                        'nonGlFilters': Absent(),
                    },
                    {
                        'id': 6,
                        'name': 'Recipe "iPhone" with discount',
                        'nonGlFilters': [{'id': 'text', 'values': [{'value': 'iPhone'}]}],
                    },
                ],
            },
        )

    def test_get_recipes_by_glfilters(self):
        """Если у поисковых рецептов есть заданный glfilter, эти рецепты должны отдаваться"""
        response = self.report.request_json('place=recipes_contain_glfilters&hid=1&glfilter=3:1')
        self.assertFragmentIn(
            response,
            {
                'recipes': [
                    {
                        'id': 1,
                        'name': 'Recipe with filters without search query',
                        'nonGlFilters': Absent(),
                    },
                    {
                        'id': 7,
                        'name': 'Recipe "iPhone" with filters',
                        'nonGlFilters': [{'id': 'text', 'values': [{'value': 'iPhone'}]}],
                    },
                ],
            },
        )

    def test_get_recipes_by_glfilters_and_search_query(self):
        """Поисковые рецепты должны находиться по совокупности поискового запроса и фильтров"""
        response = self.report.request_json('place=recipes_contain_glfilters&hid=1&text=iPhone&glfilter=3:1')
        self.assertFragmentIn(
            response,
            {
                'recipes': [
                    {
                        'id': 7,
                        'name': 'Recipe "iPhone" with filters',
                        'filters': [
                            {
                                'id': '3',
                            }
                        ],
                        'nonGlFilters': [{'id': 'text', 'values': [{'value': 'iPhone'}]}],
                    },
                    {
                        'id': 9,
                        'name': 'Recipe "iPhone" with filters 2',
                        'filters': [
                            {
                                'id': '3',
                            },
                            {
                                'id': '1',
                            },
                        ],
                        'nonGlFilters': [{'id': 'text', 'values': [{'value': 'iPhone'}]}],
                    },
                    {
                        'id': 10,
                        'name': 'Recipe "iPhone" with filters 3',
                        'filters': [
                            {
                                'id': '3',
                            },
                            {
                                'id': '4',
                            },
                        ],
                        'nonGlFilters': [{'id': 'text', 'values': [{'value': 'iPhone'}]}],
                    },
                ],
            },
            allow_different_len=False,
        )

        response = self.report.request_json('place=recipes_contain_glfilters&hid=1&text=GoodState&good-state=cutprice')
        self.assertFragmentIn(
            response,
            {
                'recipes': [
                    {
                        'entity': 'recipe',
                        'id': 13,
                        'name': 'Recipe with cutprice',
                    }
                ],
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_place_prime(cls):
        cls.index.models += [
            Model(hyperid=1, title='iPhone SE', hid=1),
            Model(hyperid=2, title='Samsung Galaxy S8', hid=1),
            Model(hyperid=3, title='iPhone X', hid=1, glparams=[GLParam(param_id=3, value=1)]),
        ]

        cls.index.offers += [
            Offer(title='iPhone SE 32Gb', hyperid=1),
            Offer(title='iPhone SE 64Gb', hyperid=1),
            Offer(title='Samsung Galaxy S8 Red', hyperid=2),
            Offer(
                title='Samsung Galaxy S8 Black',
                hyperid=2,
                discount=10,
                promo=Promo(promo_type=PromoType.N_PLUS_ONE, key='Promo'),
            ),
            Offer(title='iPhone X Red', hyperid=3),
        ]

    def test_recipes_by_doc_without_filters(self):
        """Если у документа нет фильтров, должны выдаваться поисковые рецепты без фильтров"""
        response = self.report.request_json('place=recipes_by_doc&hyperid=2')
        self.assertFragmentIn(
            response,
            {
                'recipes': [
                    {
                        'entity': 'recipe',
                        'id': 2,
                        'name': 'Search recipe without filters',
                    },
                    {
                        'entity': 'recipe',
                        'id': 5,
                        'name': 'Recipe "iPhone" without filters',
                    },
                    {
                        'entity': 'recipe',
                        'id': 6,
                        'name': 'Recipe "iPhone" with discount',
                    },
                    {
                        'entity': 'recipe',
                        'id': 8,
                        'name': 'Category recipe',
                    },
                    {
                        'entity': 'recipe',
                        'id': 13,
                        'name': 'Recipe with cutprice',
                    },
                ],
            },
            allow_different_len=False,
        )

    def test_recipes_by_doc_with_filters(self):
        """
        Если у документа есть фильтры, должны выдаваться рецепты с совпадающими фильтрами и поисковые рецепты без
        фильтров
        """
        response = self.report.request_json('place=recipes_by_doc&hyperid=3')
        self.assertFragmentIn(
            response,
            {
                'recipes': [
                    {
                        'entity': 'recipe',
                        'id': 1,
                        'name': 'Recipe with filters without search query',
                    },
                    {
                        'entity': 'recipe',
                        'id': 2,
                        'name': 'Search recipe without filters',
                    },
                    {
                        'entity': 'recipe',
                        'id': 5,
                        'name': 'Recipe "iPhone" without filters',
                    },
                    {
                        'entity': 'recipe',
                        'id': 6,
                        'name': 'Recipe "iPhone" with discount',
                    },
                    {
                        'entity': 'recipe',
                        'id': 7,
                        'name': 'Recipe "iPhone" with filters',
                    },
                    {
                        'entity': 'recipe',
                        'id': 8,
                        'name': 'Category recipe',
                    },
                    {
                        'entity': 'recipe',
                        'id': 13,
                        'name': 'Recipe with cutprice',
                    },
                ],
            },
        )

    def test_recipes_by_nonexistent_doc(self):
        """Если ищутся рецепты по несуществующему документу, выдача должна быть пустой"""
        response = self.report.request_json('place=recipes_by_doc&hyperid=100500')
        self.assertFragmentIn(response, {'recipes': EmptyList()})

    def test_place_prime(self):
        """Если place=prime запрашивают с recipe-id и без text, нужно развернуть рецепт в текст + фильтры"""
        self.assertEqualJsonResponses(
            'place=prime&hid=1&recipe-id=6',
            'place=prime&hid=1&text=iPhone&filter-discount-only=1',
        )

        self.assertEqualJsonResponses(
            'place=prime&hid=1&recipe-id=7',
            'place=prime&hid=1&text=iPhone&glfilter=3:1',
        )

        self.assertEqualJsonResponses(
            'place=prime&hid=1&recipe-id=13',
            'place=prime&hid=1&text=GoodState&good-state=cutprice',
        )

    def test_place_prime_with_recipe_without_filters(self):
        """Если передан recipe-id только с поисковым запросом, должна быть выдача под этот запрос без фильтров"""
        self.assertEqualJsonResponses(
            'place=prime&hid=1&recipe-id=5',
            'place=prime&hid=1&text=iPhone',
        )

    def test_place_prime_with_recipe_without_search_query(self):
        """Если передан recipe-id без поискового запроса, должна быть корректная выдача для параметров этого рецепта"""
        self.assertEqualJsonResponses(
            'place=prime&hid=1&recipe-id=1',
            'place=prime&hid=1&glfilter=3:1',
        )

    def test_place_prime_with_nonexistent_recipe(self):
        """Если передан recipe-id, который не существует, игнорируем его"""
        self.assertEqualJsonResponses(
            'place=prime&hid=1&recipe-id=100500',
            'place=prime&hid=1',
        )

    def test_place_prime_with_recipe_and_any_recipe_parameters(self):
        """Если задан recipe-id и любой рецепторный параметр, кроме hid, игнорируем переданный рецепт"""
        # Контрольная проверка - поиск по рецепту дает пустую выдачу
        response = self.report.request_json('place=prime&hid=1&recipe-id=3')
        self.assertFragmentIn(
            response,
            {
                'results': EmptyList(),
            },
        )

        # Передан glfilter -- ищем по категории с заданным glfilter
        self.assertEqualJsonResponses(
            'place=prime&hid=1&recipe-id=3&glfilter=3:1',
            'place=prime&hid=1&glfilter=3:1',
        )

        # Передана сортировка по цене. Ищем по категории с сортировкой по цене
        self.assertEqualJsonResponses(
            'place=prime&hid=1&recipe-id=3&how=aprice',
            'place=prime&hid=1&how=aprice',
        )

        # Передан фильтр по скидкам
        self.assertEqualJsonResponses(
            'place=prime&hid=1&recipe-id=3&filter-discount-only=1',
            'place=prime&hid=1&filter-discount-only=1',
        )

        # Передан фильтр по скидкам или промоакциям
        self.assertEqualJsonResponses(
            'place=prime&hid=1&recipe-id=3&filter-promo-or-discount=1',
            'place=prime&hid=1&filter-promo-or-discount=1',
        )

        # Передан текстовый запрос
        self.assertEqualJsonResponses(
            'place=prime&hid=1&recipe-id=3&text=samsung',
            'place=prime&hid=1&text=samsung',
        )

    def test_place_prime_with_recipe_from_another_category(self):
        """Если задан рецепт из другой категории, делаем поиск без учета рецепта"""
        self.assertEqualJsonResponses(
            'place=prime&hid=1&recipe-id=11',
            'place=prime&hid=1',
        )

    def test_letters_case(self):
        """
        Запрос должен матчиться по lowercase, не зависить от того, как рецепт сохранен в МБО и запрос передан с фронта
        """
        response = self.report.request_json('place=recipe_by_glfilters&hid=1&text=search query 1')
        self.assertFragmentIn(
            response,
            {
                'entity': 'recipe',
                'id': 2,
                'name': 'Search recipe without filters',
                'nonGlFilters': [{'id': 'text', 'values': [{'value': 'Search query 1'}]}],
            },
        )

        response = self.report.request_json('place=recipes_contain_glfilters&hid=1&text=iphone')
        self.assertFragmentIn(
            response,
            {
                'recipes': [
                    {
                        'id': 6,
                        'name': 'Recipe "iPhone" with discount',
                        'nonGlFilters': [
                            {'id': 'text', 'values': [{'value': 'iPhone'}]},
                            {
                                'id': 'filter-discount-only',
                            },
                        ],
                    },
                    {
                        'id': 7,
                        'name': 'Recipe "iPhone" with filters',
                        'nonGlFilters': [{'id': 'text', 'values': [{'value': 'iPhone'}]}],
                    },
                    {
                        'id': 5,
                        'name': 'Recipe "iPhone" without filters',
                        'nonGlFilters': [{'id': 'text', 'values': [{'value': 'iPhone'}]}],
                    },
                    {
                        'id': 9,
                        'name': 'Recipe "iPhone" with filters 2',
                        'nonGlFilters': [{'id': 'text', 'values': [{'value': 'iPhone'}]}],
                    },
                    {
                        'id': 10,
                        'name': 'Recipe "iPhone" with filters 3',
                        'nonGlFilters': [{'id': 'text', 'values': [{'value': 'iPhone'}]}],
                    },
                ],
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_cyrillic_recipe(cls):
        # Recipe(hid=103, name='Рецепт с кириллицей', contains_aprice_sort=1, glfilters=[
        #     GLFilterEnum(param_id=209, values=[93, 95, 94])]),
        cls.index.recipes += [
            Recipe(
                recipe_id=12,
                hid=2,
                name='БОЛЬШИЕ ТАРЕЛКИ',
                search_query='БОЛЬШИЕ ТАРЕЛКИ',
            ),
        ]

    def test_cyrillic_recipe(self):
        response = self.report.request_json('place=recipe_by_glfilters&hid=2&text=Большие Тарелки')
        self.assertFragmentIn(
            response,
            {
                'entity': 'recipe',
                'id': 12,
                'name': 'БОЛЬШИЕ ТАРЕЛКИ',
                'nonGlFilters': [{'id': 'text', 'values': [{'value': 'БОЛЬШИЕ ТАРЕЛКИ'}]}],
            },
        )


if __name__ == '__main__':
    main()
