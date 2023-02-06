#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    GLFilterBool,
    GLFilterEnum,
    GLFilterNumeric,
    GLParam,
    GLType,
    HyperCategory,
    Model,
    ModelGroup,
    NavCategory,
    NavRecipe,
    NavRecipeFilter,
    Offer,
    Recipe,
    Region,
    YamarecPlaceReasonsToBuy,
)
from core.matcher import NoKey, Absent, Contains, NotEmptyList, NotEmpty


class T(TestCase):
    @classmethod
    def prepare(cls):

        cls.index.gltypes += [
            GLType(
                param_id=102,
                hid=1,
                gltype=GLType.ENUM,
                values=list(range(40, 51)),
                cluster_filter=False,
                model_filter_index=3,
            ),
            GLType(
                param_id=103, hid=1, gltype=GLType.ENUM, values=[1, 2, 3], cluster_filter=False, model_filter_index=1
            ),
            GLType(param_id=104, hid=1, gltype=GLType.BOOL, cluster_filter=False, model_filter_index=2),
            GLType(param_id=105, hid=1, gltype=GLType.ENUM, values=list(range(10)), cluster_filter=False),
            GLType(param_id=106, hid=1, gltype=GLType.NUMERIC),
            GLType(param_id=200, hid=300, gltype=GLType.ENUM, values=list(range(1, 5))),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=234, exist=False),
            HyperCategory(hid=100),
            HyperCategory(hid=101),
            HyperCategory(hid=102),
            HyperCategory(hid=300),
        ]

        cls.index.recipes += [
            Recipe(recipe_id=1001, hid=1, name='One Enum Value', glfilters=[GLFilterEnum(param_id=105, values=[8])]),
            Recipe(recipe_id=1002, hid=1, name='One Bool Value', glfilters=[GLFilterBool(param_id=104, value=1)]),
            Recipe(
                recipe_id=1003,
                hid=1,
                name='Up To Max Numeric Value',
                popularity=5,
                glfilters=[GLFilterNumeric(param_id=106, max_value=78)],
            ),
            Recipe(
                recipe_id=1004,
                hid=1,
                name='Down To Min Numeric Value',
                popularity=3,
                glfilters=[GLFilterNumeric(param_id=106, min_value=50)],
            ),
            Recipe(
                recipe_id=1005,
                hid=1,
                name='Between Numeric Value',
                glfilters=[GLFilterNumeric(param_id=106, min_value=35, max_value=36)],
            ),
            Recipe(
                recipe_id=1006,
                hid=1,
                name='Many Different Filters',
                popularity=10,
                sponsored=True,
                glfilters=[
                    GLFilterEnum(param_id=103, values=[1, 2]),
                    GLFilterBool(param_id=104, value=1),
                    GLFilterNumeric(param_id=106, min_value=35, max_value=36),
                ],
            ),
            Recipe(
                recipe_id=1007,
                hid=1,
                name='Two Different Filters',
                glfilters=[GLFilterEnum(param_id=102, values=[45, 46, 47]), GLFilterBool(param_id=104, value=0)],
            ),
            # check that cgi-parameter &good-state affect to chosen recipe
            Recipe(
                recipe_id=10011,
                hid=11,
                name='Good State New',
                good_state='new',
                popularity=3,
                glfilters=[GLFilterEnum(param_id=105, values=[8])],
            ),
            Recipe(
                recipe_id=10012,
                hid=11,
                name='Good State CutPrice',
                good_state='cutprice',
                glfilters=[GLFilterEnum(param_id=105, values=[8])],
            ),
            Recipe(
                recipe_id=10013,
                hid=11,
                name='Good State Other New',
                good_state='new',
                glfilters=[GLFilterEnum(param_id=103, values=[1])],
            ),
            Recipe(recipe_id=10014, hid=11, name='Good State None', glfilters=[GLFilterEnum(param_id=105, values=[8])]),
            # check that recipe will be linked (category with hid = 2 and gltype with
            # paramid = 207 shoud be generated)
            Recipe(hid=2, name='Some Other Recipe', glfilters=[GLFilterEnum(param_id=207, values=[93, 95, 94])]),
            # check that recipes with the same filters will not dropped
            Recipe(
                hid=2,
                name='Yet Another Recipe',
                popularity=10,
                contains_reviews=0,
                glfilters=[GLFilterEnum(param_id=207, values=[92, 94]), GLFilterBool(param_id=208, value=1)],
            ),
            Recipe(
                hid=2,
                name='Popular Recipe Like Yet Another Recipe',
                popularity=12,
                contains_reviews=0,
                glfilters=[GLFilterEnum(param_id=207, values=[92, 94]), GLFilterBool(param_id=208, value=1)],
            ),
            Recipe(
                hid=2,
                name='Yet Another Recipe Like Yet Another Recipe',
                popularity=8,
                glfilters=[GLFilterEnum(param_id=207, values=[92, 94]), GLFilterBool(param_id=208, value=1)],
            ),
            Recipe(
                hid=100,
                name='Recipe with discount',
                contains_discount=1,
                glfilters=[GLFilterEnum(param_id=207, values=[93, 95, 94])],
            ),
            Recipe(hid=100, name='Recipe with no discount', glfilters=[GLFilterEnum(param_id=207, values=[93])]),
            Recipe(
                hid=101,
                name='Recipe with discount or promo',
                contains_discount_or_promo=1,
                glfilters=[GLFilterEnum(param_id=207, values=[93, 95, 94])],
            ),
            Recipe(
                hid=101,
                name='Recipe with neither discount nor promo',
                glfilters=[GLFilterEnum(param_id=207, values=[93])],
            ),
            Recipe(
                hid=102,
                name='Recipe with aprice',
                contains_aprice_sort=1,
                glfilters=[GLFilterEnum(param_id=207, values=[93, 95, 94])],
            ),
            Recipe(
                hid=102, name='Other with aprice', how='aprice', glfilters=[GLFilterEnum(param_id=207, values=[93, 95])]
            ),
            Recipe(hid=102, name='Recipe with no aprice', glfilters=[GLFilterEnum(param_id=207, values=[93])]),
            # такая сортировка невозможна без наличия причин купить
            # но мы не будем сейчас это валидировать и оставим это на совести контентов
            Recipe(hid=102, name='Recipe with best_by_factor:1 sorting', how='best_by_factor:1'),
            Recipe(hid=102, name='Recipe with best_by_factor:2 sorting', how='best_by_factor:2'),
            # сортировки кроме aprice и best_by_factor не поддерживаются
            Recipe(hid=102, name='Recipe with unsupported sorting', how='noffers'),
            Recipe(
                hid=103,
                name='Рецепт с кириллицей',
                contains_aprice_sort=1,
                glfilters=[GLFilterEnum(param_id=209, values=[93, 95, 94])],
            ),
            Recipe(recipe_id=1101, hid=300, name='Поисковый рецепт', search_query='Фумигаторы от блох'),
            Recipe(
                recipe_id=1100,
                hid=300,
                name='Поисковый рецепт без отзывов',
                contains_reviews=False,
                search_query='Фумигаторы от блох без отзывов',
            ),
        ]

    def test_recipe_by_search_query(self):
        """
        ручки recipe_by_glfilters, recipes_contain_glfilters возвращают рецепты найденные
        по тексту поискового запроса в самом рецепте если до этого поиск по фильтрам ничего не нашел
        """
        test_response = {
            "category": {
                "id": 300,
            },
            "entity": "recipe",
            "header": "Поисковый рецепт",
            "id": 1101,
            "name": "Поисковый рецепт",
            "nonGlFilters": [{"id": "text", "values": [{"value": "Фумигаторы от блох"}]}],
            "shortName": "Поисковый рецепт",
            "slug": "poiskovyi-retsept",
        }
        test_response_no_reviews = {
            "category": {
                "id": 300,
            },
            "entity": "recipe",
            "header": "Поисковый рецепт без отзывов",
            "id": 1100,
            "name": "Поисковый рецепт без отзывов",
            "nonGlFilters": [{"id": "text", "values": [{"value": "Фумигаторы от блох без отзывов"}]}],
            "shortName": "Поисковый рецепт без отзывов",
            "slug": "poiskovyi-retsept-bez-otzyvov",
        }

        # Проверяем поисковые рецепты в плейсе recipe_by_glfilters,
        # ожидаем один результат при запросе как c фильтром так и без него
        response = self.report.request_json('place=recipe_by_glfilters&hid=300&text=Фумигаторы%20от%20блох')
        self.assertFragmentIn(response, test_response, allow_different_len=False)
        response = self.report.request_json(
            'place=recipe_by_glfilters&hid=300&glfilter=200:1&text=Фумигаторы%20от%20блох'
        )
        self.assertFragmentIn(response, test_response, allow_different_len=False)

        # Проверяем поисковые рецепты в плейсе recipes_contain_glfilters, возможно несколько рецептов в ответе
        # запрос без фильтров, должны быть оба рецепта, порядок сортировки важен
        response = self.report.request_json('place=recipes_contain_glfilters&hid=300&text=Фумигаторы%20от%20блох')
        self.assertFragmentIn(response, test_response, allow_different_len=False)
        self.assertFragmentIn(response, test_response_no_reviews, allow_different_len=False)
        self.assertFragmentIn(response, {"recipes": [{"id": 1100}, {"id": 1101}]}, allow_different_len=False)

        # запрос с фильтром и текстом только про второй рецепт, должен быть один результат
        response = self.report.request_json(
            'place=recipes_contain_glfilters&hid=300&glfilter=200:1&text=Фумигаторы%20от%20блох%20без%20отзывов'
        )
        self.assertFragmentNotIn(response, test_response)
        self.assertFragmentIn(response, test_response_no_reviews, allow_different_len=False)

        # запрос с дополнительным параметром show-reviews=1, только один рецепт с таким hid, добавляется часть про "Отзывы.."
        response = self.report.request_json(
            'place=recipes_contain_glfilters&hid=300&show-reviews=1&glfilter=200:1&text=Фумигаторы%20от%20блох'
        )
        self.assertFragmentIn(
            response, {"recipes": [{"header": "Отзывы на Поисковый рецепт"}]}, allow_different_len=False
        )

        # запрос с дополнительным параметром show-reviews=0, только один рецепт с таким hid
        response = self.report.request_json(
            'place=recipes_contain_glfilters&hid=300&show-reviews=0&glfilter=200:1&text=Фумигаторы%20от%20блох'
        )
        self.assertFragmentIn(response, test_response, allow_different_len=False)
        self.assertFragmentNotIn(response, test_response_no_reviews)

        # Проверяем поисковые рецепты в плейсе recipes_by_doc,
        # запрос с параметром recipe-text должен дополнительно возвращать поисковые рецепты
        response = self.report.request_json('place=recipes_by_doc&hyperid=700&recipe-text=Фумигаторы%20от%20блох')
        self.assertFragmentIn(
            response,
            {
                "entity": "recipe",
                "header": "Поисковый рецепт",
                "id": 1101,
                "name": "Поисковый рецепт",
                "nonGlFilters": [{"id": "text", "values": [{"value": "Фумигаторы от блох"}]}],
            },
            allow_different_len=False,
        )

    def test_recipe_by_discount(self):
        '''
        Проверяем фильтр по скидкам в рецептах в плейсе recipe_by_glfilters
        '''
        response = self.report.request_json(
            'place=recipe_by_glfilters&hid=100&glfilter=207:93,94,95&filter-discount-only=1'
        )
        self.assertFragmentIn(
            response,
            {"entity": "recipe", "name": "Recipe with discount", "nonGlFilters": [{"id": "filter-discount-only"}]},
            allow_different_len=False,
        )

        response = self.report.request_json('place=recipe_by_glfilters&hid=100&filter-discount-only=1')
        self.assertFragmentNotIn(response, {"entity": "recipe"})

        response = self.report.request_json('place=recipe_by_glfilters&hid=100&glfilter=207:93,94,95')
        self.assertFragmentNotIn(response, {"entity": "recipe"})

    def test_recipe_by_promo_or_discount(self):
        '''
        Проверяем фильтр по скидкам и акциям в рецептах в плейсе recipe_by_glfilters
        '''
        response = self.report.request_json(
            'place=recipe_by_glfilters&hid=101&glfilter=207:93,94,95&filter-promo-or-discount=1'
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "recipe",
                "name": "Recipe with discount or promo",
                "nonGlFilters": [{"id": "filter-promo-or-discount"}],
            },
            allow_different_len=False,
        )

        response = self.report.request_json('place=recipe_by_glfilters&hid=101&filter-promo-or-discount=1')
        self.assertFragmentNotIn(response, {"entity": "recipe"})

        response = self.report.request_json('place=recipe_by_glfilters&hid=101&glfilter=207:93,94,95')
        self.assertFragmentNotIn(response, {"entity": "recipe"})

    def test_recipe_by_aprice(self):
        '''
        Проверяем фильтрацию по наличию сортировки по цене в рецептах в плейсе recipe_by_glfilters
        '''
        response = self.report.request_json('place=recipe_by_glfilters&hid=102&glfilter=207:93,94,95&how=aprice')
        self.assertFragmentIn(
            response,
            {
                "entity": "recipe",
                "name": "Recipe with aprice",
                "sorts": [{"text": "по цене", "options": [{"id": "aprice", "type": "asc"}]}],
            },
            allow_different_len=False,
        )

        response = self.report.request_json('place=recipe_by_glfilters&hid=102&how=aprice')
        self.assertFragmentNotIn(response, {"entity": "recipe"})

        response = self.report.request_json('place=recipe_by_glfilters&hid=102&glfilter=207:93,94,95')
        self.assertFragmentNotIn(response, {"entity": "recipe"})

    def test_good_state(self):
        '''
        Проверяем фильтр по уцененным товарам в рецептах в плейсе recipe_by_glfilters
        '''
        response = self.report.request_json('place=recipe_by_glfilters&hid=11&glfilter=105:8&good-state=new')
        self.assertFragmentIn(
            response,
            {
                "entity": "recipe",
                "id": 10011,
                "name": "Good State New",
                "nonGlFilters": [{"id": "good-state", "values": [{"value": "new"}]}],
            },
            allow_different_len=False,
        )

        response = self.report.request_json('place=recipe_by_glfilters&hid=11&glfilter=105:8&good-state=cutprice')
        self.assertFragmentIn(
            response,
            {
                "entity": "recipe",
                "id": 10012,
                "name": "Good State CutPrice",
                "nonGlFilters": [{"id": "good-state", "values": [{"value": "cutprice"}]}],
            },
            allow_different_len=False,
        )

        # Проверяем, что без указания good-state в выдаче рецепт без указания good-state
        response = self.report.request_json('place=recipe_by_glfilters&hid=11&glfilter=105:8')
        self.assertFragmentIn(
            response,
            {"entity": "recipe", "id": 10014, "name": "Good State None", "nonGlFilters": NoKey("nonGlFilters")},
            allow_different_len=False,
        )

    def test_recipes_contains_discount(self):
        request = "place=recipes_contain_glfilters&hid=100&filter-discount-only=1"
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "recipes": [
                    {
                        "entity": "recipe",
                        "name": "Recipe with discount",
                        "nonGlFilters": [{"id": "filter-discount-only"}],
                    }
                ]
            },
            allow_different_len=False,
        )

        request = "place=recipes_contain_glfilters&hid=100"
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "recipes": [
                    {
                        "entity": "recipe",
                        "name": "Recipe with discount",
                        "nonGlFilters": [{"id": "filter-discount-only"}],
                    },
                    {"entity": "recipe", "name": "Recipe with no discount", "nonGlFilters": Absent()},
                ]
            },
            allow_different_len=False,
        )

    def test_recipes_contains_discount_or_promo(self):
        request = "place=recipes_contain_glfilters&hid=101&filter-promo-or-discount=1"
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "recipes": [
                    {
                        "entity": "recipe",
                        "name": "Recipe with discount or promo",
                        "nonGlFilters": [{"id": "filter-promo-or-discount"}],
                    }
                ]
            },
            allow_different_len=False,
        )

        request = "place=recipes_contain_glfilters&hid=101"
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "recipes": [
                    {
                        "entity": "recipe",
                        "name": "Recipe with discount or promo",
                        "nonGlFilters": [{"id": "filter-promo-or-discount"}],
                    },
                    {"entity": "recipe", "name": "Recipe with neither discount nor promo", "nonGlFilters": Absent()},
                ]
            },
            allow_different_len=False,
        )

    def test_sortin_in_recipe(self):
        """В рецептах поддержана сортировка aprice и best_by_factor:n"""
        request = "place=recipes_contain_glfilters&hid=102&how=aprice"
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "recipes": [
                    {
                        "entity": "recipe",
                        "name": "Recipe with aprice",
                        "sorts": [{"text": "по цене", "options": [{"id": "aprice", "type": "asc", "isActive": True}]}],
                    },
                    {
                        "entity": "recipe",
                        "name": "Other with aprice",
                        "sorts": [{"text": "по цене", "options": [{"id": "aprice", "type": "asc", "isActive": True}]}],
                    },
                ]
            },
            allow_different_len=False,
        )

        response = self.report.request_json(
            "place=recipes_contain_glfilters&hid=102&how=best_by_factor:1"
            "&rearr-factors=market_best_by_factor_recipes=1"
        )
        self.assertFragmentIn(
            response,
            {
                "recipes": [
                    {
                        "entity": "recipe",
                        "name": "Recipe with best_by_factor:1 sorting",
                        "sorts": [{"text": "пользователям нравится", "options": [{"id": "best_by_factor:1"}]}],
                    }
                ]
            },
            allow_different_len=False,
        )

        response = self.report.request_json(
            "place=recipes_contain_glfilters&hid=102" "&rearr-factors=market_best_by_factor_recipes=1"
        )
        self.assertFragmentIn(
            response,
            {
                "recipes": [
                    {
                        "entity": "recipe",
                        "name": "Recipe with aprice",
                        "sorts": [{"text": "по цене", "options": [{"id": "aprice", "type": "asc", "isActive": True}]}],
                    },
                    {"entity": "recipe", "name": "Recipe with no aprice", "sorts": NoKey("sorts")},
                    {"entity": "recipe", "name": "Other with aprice", "sorts": [{"text": "по цене"}]},
                    {
                        "entity": "recipe",
                        "name": "Recipe with best_by_factor:1 sorting",
                        "sorts": [{"text": "пользователям нравится", "options": [{"id": "best_by_factor:1"}]}],
                    },
                    {
                        "entity": "recipe",
                        "name": "Recipe with best_by_factor:2 sorting",
                        "sorts": [{"text": "пользователям нравится", "options": [{"id": "best_by_factor:2"}]}],
                    },
                    {"entity": "recipe", "name": "Recipe with unsupported sorting", "sorts": NoKey("sorts")},
                ]
            },
            allow_different_len=False,
        )

    def test_recipes_contains_good_state(self):
        '''
        Проверяем фильтр по уцененным товарам в рецептах в плейсе recipes_contain_glfilters
        '''
        response = self.report.request_json('place=recipes_contain_glfilters&hid=11&good-state=new')
        self.assertFragmentIn(
            response,
            {
                "recipes": [
                    {
                        "id": 10011,
                        "popularity": 3,
                        "category": {"id": 11},
                        "nonGlFilters": [{"id": "good-state", "values": [{"value": "new"}]}],
                    },
                    {
                        "id": 10013,
                        "popularity": 0,
                        "category": {"id": 11},
                        "nonGlFilters": [{"id": "good-state", "values": [{"value": "new"}]}],
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json('place=recipes_contain_glfilters&hid=11&good-state=cutprice')
        self.assertFragmentIn(
            response,
            {
                "recipes": [
                    {
                        "id": 10012,
                        "popularity": 0,
                        "category": {"id": 11},
                        "nonGlFilters": [{"id": "good-state", "values": [{"value": "cutprice"}]}],
                    }
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_no_recipe(self):
        '''ручка recipe_by_glfilters возвращает рецепт в точности совпадающий с фильтрами
        по данным запросам нет подходящих рецептов
        ожидаем результат {"recipe": null}
        '''
        expected = {"recipe": None}  # None=null

        requests = [
            'place=recipe_by_glfilters&hid=1',
            'place=recipe_by_glfilters&hid=1&glfilter=102:42',
            'place=recipe_by_glfilters&hid=1&glfilter=104:0',
            'place=recipe_by_glfilters&hid=1&glfilter=102:42&glfilter=104:1',
            'place=recipe_by_glfilters&hid=1&glfilter=102:42,43,44&glfilter=104:1',
            'place=recipe_by_glfilters&hid=1&glfilter=106:78,78',
            'place=recipe_by_glfilters&hid=1&glfilter=106:78,',
        ]

        for request in requests:
            response = self.report.request_json(request)
            self.assertFragmentIn(response, expected)

    def test_error_invalid_glfilters(self):
        '''при невалидном задании фильтров или задании несуществующих фильтров ручка возвращает ошибку'''
        expected = {"error": {"code": "INVALID_USER_CGI", "message": "invalid glfilters"}}

        requests = [
            'place=recipe_by_glfilters&hid=1&glfilter=102',
            'place=recipe_by_glfilters&hid=1&glfilter=104:0,1',
            'place=recipe_by_glfilters&hid=1&glfilter=999:1,2,3',
            'place=recipes_contain_glfilters&hid=1&glfilter=104:1,2,other',
            'place=recipes_contain_glfilters&hid=1&glfilter=106:20',
            'place=recipes_contain_glfilters&hid=1&glfilter=234:23,24',
        ]

        for request in requests:
            response = self.report.request_json(request)
            self.error_log.expect(code=3019, message=Contains("Error in glfilters syntax"))
            self.error_log.expect(
                code=3019, message=Contains("Exit with code 1 (INVALID_USER_CGI)", "because invalid glfilters")
            )
            self.assertFragmentIn(response, expected, preserve_order=True)

    def test_error_required_hid(self):
        '''параметр hid - обязательный параметр для ручек рецептов'''
        expected = {"error": {"code": "INVALID_USER_CGI", "message": "cgi param hid is required"}}

        requests = [
            'place=recipe_by_glfilters',
            'place=recipe_by_glfilters&glfilter=106:25,50',
            'place=recipes_contain_glfilters',
            'place=recipes_contain_glfilters&hid=234',  # несуществующая категория
        ]

        for request in requests:
            response = self.report.request_json(request)
            self.error_log.expect(
                code=4501, message=Contains("Exit with code 1 (INVALID_USER_CGI)", "because cgi param hid is required")
            )
            self.assertFragmentIn(response, expected)

        self.error_log.ignore("Unknown category ID 234 passed as 'hid' CGI-parameter.")

    def test_recipe_by_glfilters(self):
        '''проверяем что recipe_by_glfilters при правильном задании параметров находит нужный рецепт'''

        # рецепты с одним фильтром

        response = self.report.request_json('place=recipe_by_glfilters&hid=1&glfilter=105:8')
        self.assertFragmentIn(response, {"recipe": {"header": "One Enum Value"}}, preserve_order=True)

        response = self.report.request_json('place=recipe_by_glfilters&hid=1&glfilter=104:1')
        self.assertFragmentIn(response, {"recipe": {"header": "One Bool Value"}}, preserve_order=True)

        response = self.report.request_json('place=recipe_by_glfilters&hid=1&glfilter=106:,78')
        self.assertFragmentIn(response, {"recipe": {"header": "Up To Max Numeric Value"}}, preserve_order=True)

        response = self.report.request_json('place=recipe_by_glfilters&hid=1&glfilter=106:50,')
        self.assertFragmentIn(response, {"recipe": {"header": "Down To Min Numeric Value"}}, preserve_order=True)

        response = self.report.request_json('place=recipe_by_glfilters&hid=1&glfilter=106:35,36')
        self.assertFragmentIn(response, {"recipe": {"header": "Between Numeric Value"}}, preserve_order=True)

        response = self.report.request_json('place=recipe_by_glfilters&hid=2&glfilter=207:93,94,95')
        self.assertFragmentIn(response, {"recipe": {"header": "Some Other Recipe"}}, preserve_order=True)

        # рецепты с несколькими фильтрами

        response = self.report.request_json(
            'place=recipe_by_glfilters&hid=1&glfilter=103:1,2&glfilter=104:1&glfilter=106:35,36'
        )
        self.assertFragmentIn(response, {"recipe": {"header": "Many Different Filters"}}, preserve_order=True)

        response = self.report.request_json('place=recipe_by_glfilters&hid=1&glfilter=104:0;102:45,46,47')
        self.assertFragmentIn(response, {"recipe": {"header": "Two Different Filters"}}, preserve_order=True)

    def test_recipes_contain_glfilters(self):
        '''ручка recipes_contain_glfilters по набору gl-фильтров находит рецепты содержащие
        в точности все заданные в запросе фильтры с ровно теми же параметрами и возможно еще какие-то другие фильтры
        '''

        response = self.report.request_json('place=recipes_contain_glfilters&hid=1&glfilter=106:35,36')
        self.assertFragmentIn(
            response,
            {
                "recipes": [
                    {
                        "header": "Between Numeric Value",
                        "category": {"id": 1, "navigationId": 2},
                        "filters": [{"id": "106"}],
                        "selected": True,
                    },  # рецепт в точности совпадает с набором переданных фильтров
                    {
                        "header": "Many Different Filters",
                        "filters": [{"id": "106"}, {"id": "104"}, {"id": "103"}],
                        "selected": NoKey("selected"),
                    },
                ]
            },
            allow_different_len=False,
        )

        # проверяем, что этот и только этот рецепт находится при любом порядке задания фильтров в запросе
        # для enum-фильтров в выдаче указываются только те значения, которые используются в рецепте
        # например фильтр id=103 не содержит значения 3
        # однако это не касается булевых фильтров (например id=104)
        for request in [
            'place=recipes_contain_glfilters&hid=1&glfilter=104:1&glfilter=103:1,2',
            'place=recipes_contain_glfilters&hid=1&glfilter=103:1,2&glfilter=104:1',
        ]:

            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "recipes": [
                        {
                            "entity": "recipe",
                            "header": "Many Different Filters",
                            "name": "Many Different Filters",
                            "popularity": 10,
                            "isSponsored": True,
                            "category": {"id": 1, "navigationId": 2},
                            "filters": [
                                {"id": "106", "values": [{"min": "35", "max": "36", "id": "chosen"}]},
                                {
                                    "id": "104",
                                    "values": [
                                        {"value": "0", "checked": NoKey("checked")},
                                        {"value": "1", "checked": True},
                                    ],
                                },
                                {"id": "103", "values": [{"id": "1", "checked": True}, {"id": "2", "checked": True}]},
                            ],
                            "selected": NoKey("selected"),
                        }
                    ]
                },
                allow_different_len=False,
            )

        response = self.report.request_json('place=recipes_contain_glfilters&hid=1&glfilter=105:2,5,8')
        self.assertFragmentIn(response, {"recipes": []}, preserve_order=True, allow_different_len=False)

    def test_get_recipes_by_hid(self):
        '''по категории без фильтров возвращаем рецепты отсортированные по популярности
        параметр prun-count ограничивает количество возвращаемых фильтров
        '''
        response = self.report.request_json('place=recipes_contain_glfilters&hid=1')
        self.assertFragmentIn(
            response,
            {
                "recipes": [
                    {"id": 1006, "popularity": 10, "category": {"id": 1}},
                    {"id": 1003, "popularity": 5, "category": {"id": 1}},
                    {"id": 1004, "popularity": 3, "category": {"id": 1}},
                    {"id": 1001, "popularity": 0, "category": {"id": 1}},
                    {"id": 1002, "popularity": 0, "category": {"id": 1}},
                    {"id": 1005, "popularity": 0, "category": {"id": 1}},
                    {"id": 1007, "popularity": 0, "category": {"id": 1}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json('place=recipes_contain_glfilters&hid=1&prun-count=4')
        self.assertFragmentIn(
            response,
            {
                "recipes": [
                    {"id": 1006, "popularity": 10, "category": {"id": 1}},
                    {"id": 1003, "popularity": 5, "category": {"id": 1}},
                    {"id": 1004, "popularity": 3, "category": {"id": 1}},
                    {"id": 1001, "popularity": 0, "category": {"id": 1}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_missing_pp(self):
        self.report.request_json(
            'place=recipes_contain_glfilters&hid=1&glfilter=106:35,36&ip=127.0.0.1', add_defaults=False
        )
        self.report.request_json('place=recipe_by_glfilters&hid=1&glfilter=105:8&ip=127.0.0.1', add_defaults=False)

    def test_recipes_with_the_same_filters(self):
        '''Рецепты с одинаковым набором фильтров не отбрасываются'''

        # Для ручки recipe_by_glfilters выбирается наиболее популярный из одинаковых
        response = self.report.request_json('place=recipe_by_glfilters&hid=2&glfilter=207:92,94&glfilter=208:1')
        self.assertFragmentIn(
            response, {"recipe": {"header": "Popular Recipe Like Yet Another Recipe"}}, preserve_order=True
        )

        # В ручке recipes_by_glfilters присутствуют все
        response = self.report.request_json('place=recipes_contain_glfilters&hid=2&glfilter=208:1')
        self.assertFragmentIn(
            response,
            {
                "recipes": [
                    {"header": "Yet Another Recipe"},
                    {"header": "Popular Recipe Like Yet Another Recipe"},
                    {"header": "Yet Another Recipe Like Yet Another Recipe"},
                ]
            },
            preserve_order=False,
        )

    def test_recipes_dropped_if_no_reviews(self):
        """
        Добавляем в запрос show-reviews=1, проверяем что вылетели рецепты с contains_reviews=0
        А оставшийся содержит заголовок в формате  "Отзывы на %категория с маленькой буквы%".
        """
        response = self.report.request_json('place=recipes_contain_glfilters&hid=2&glfilter=208:1&show-reviews=1')
        self.assertFragmentIn(
            response,
            {
                "recipes": [
                    {
                        "name": "Отзывы на yet Another Recipe Like Yet Another Recipe",
                        "shortName": "Отзывы на yet Another Recipe Like Yet Another Recipe",
                    }
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_hide_recipe_filters(cls):
        cls.index.navtree += [
            NavCategory(
                hid=51,
                nid=61,
                name="NavCategory 61",
                is_blue=False,
                recipe=NavRecipe(
                    filters=[
                        NavRecipeFilter(filter_type=NavRecipeFilter.BOOLEAN, param_id=501, bool_value=True),
                        NavRecipeFilter(filter_type=NavRecipeFilter.ENUM, param_id=502, enum_values=[3, 6]),
                        NavRecipeFilter(filter_type=NavRecipeFilter.ENUM, param_id=503, enum_values=[12]),
                        NavRecipeFilter(filter_type=NavRecipeFilter.NUMBER, param_id=504, max_value=100),
                        NavRecipeFilter(filter_type=NavRecipeFilter.NUMBER, param_id=505, min_value=55, max_value=55),
                    ]
                ),
            )
        ]

        cls.index.gltypes += [
            GLType(param_id=501, hid=51, cluster_filter=False, gltype=GLType.BOOL),
            GLType(param_id=502, hid=51, gltype=GLType.ENUM, values=list(range(10))),
            GLType(param_id=503, hid=51, gltype=GLType.ENUM, values=list(range(11, 15))),
            GLType(param_id=504, hid=51, gltype=GLType.NUMERIC),
            GLType(param_id=505, hid=51, gltype=GLType.NUMERIC),
        ]

        cls.index.models += [
            Model(
                title="dress",
                hid=51,
                hyperid=751,
                glparams=[
                    GLParam(param_id=501, value=1),
                    GLParam(param_id=502, value=6),
                    GLParam(param_id=503, value=12),
                    GLParam(param_id=504, value=35),
                    GLParam(param_id=505, value=55),
                ],
            ),
        ]

    def test_hide_recipe_filters(self):
        """
        Должны скрываться рецептные фильтры типов ENUM и NUMERIC, имеющие одно значение
        """
        response = self.report.request_json('place=prime&nid=61')
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {"id": "glprice"},
                    {"id": "501"},
                    {"id": "502"},
                    {"id": "504"},
                    {"id": "onstock"},
                    {"id": "offer-shipping"},
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_recipes_by_doc(cls):
        """Cоздадим модели (модификации и групповые) с gl-фильтрами,
        которые бы подходили под значения gl-фильтров в рецептах
        """
        cls.index.navtree += [NavCategory(hid=1, nid=2, name="NavCategory")]

        cls.index.model_groups += [
            # hid=1&glfilter=106:35,36
            ModelGroup(
                title="dress",
                hid=1,
                hyperid=700,
                glparams=[GLParam(param_id=106, value=35), GLParam(param_id=106, value=36)],
            ),
            # hid=1&glfilter=106:35,36
            ModelGroup(
                title="dress",
                hid=1,
                hyperid=701,
                glparams=[GLParam(param_id=106, value=35), GLParam(param_id=106, value=36)],
            ),
        ]

        cls.index.models += [
            # hid=1&glfilter=106:35,36
            Model(
                title="dress",
                hid=1,
                hyperid=600,
                group_hyperid=701,
                glparams=[GLParam(param_id=106, value=35), GLParam(param_id=106, value=36)],
            ),
            # hid=1&glfilter=104:1&glfilter=103:1
            Model(
                title="dress",
                hid=1,
                hyperid=601,
                glparams=[GLParam(param_id=104, value=1), GLParam(param_id=103, value=1)],
            ),
            # hid=1&glfilter=103:1,2&glfilter=104:1
            Model(
                title="dress",
                hid=1,
                hyperid=602,
                glparams=[
                    GLParam(param_id=104, value=1),
                    GLParam(param_id=103, value=1),
                    GLParam(param_id=103, value=2),
                ],
            ),
            # hid=1&glfilter=105:2,5,9
            Model(
                title="dress",
                hid=1,
                hyperid=603,
                glparams=[
                    GLParam(param_id=105, value=2),
                    GLParam(param_id=105, value=5),
                    GLParam(param_id=105, value=9),
                ],
            ),
            # hid=1
            Model(title="dress", hid=1, hyperid=604),
        ]

    def test_recipes_by_doc(self):
        """Делаем разные запросы с разными hyperid и ожидаем получить рецепты, которые подходят под фильтры моделек"""

        # групповая без модификаций, групповая с модификацией и сама модификация
        for request in [
            'place=recipes_by_doc&hyperid=700',  # glfilter=106:35,36
            'place=recipes_by_doc&hyperid=701',
            'place=recipes_by_doc&hyperid=600',
        ]:
            response = self.report.request_json(request)

            # 1001 - у модели нет ни одного фильтра рецепта
            # 1002 - у модели нет ни одного фильтра рецепта
            # 1003 - подходит под фильтры, показался
            # 1004 - значение фильтра 106 большие минимального (50)
            # 1005 - подходит под фильтры, показался
            # 1006 - у модели нет фильтра 103 и 104, а они должны быть, т.к. они типа enum
            # 1007 - у модели нет ни одного фильтра рецепта

            self.assertFragmentIn(
                response,
                {
                    "recipes": [
                        {
                            "entity": "recipe",
                            "id": 1003,
                            "header": "Up To Max Numeric Value",
                            "category": {"id": 1, "navigationId": 2},
                            "filters": [{"id": "106"}],
                        },
                        {
                            "entity": "recipe",
                            "id": 1005,
                            "header": "Between Numeric Value",
                            "category": {"id": 1, "navigationId": 2},
                            "filters": [{"id": "106"}],
                        },
                    ]
                },
                allow_different_len=False,
            )

        for request in [
            'place=recipes_by_doc&hyperid=601',  # glfilter=104:1&glfilter=103:1
            'place=recipes_by_doc&hyperid=602',
        ]:  # glfilter=103:1,2&glfilter=104:1

            # 1001 - у модели нет ни одного фильтра рецепта
            # 1002 - подходит под фильтры, показался
            # 1003 - у модели нет ни одного фильтра рецепта
            # 1004 - у модели нет ни одного фильтра рецепта
            # 1005 - у модели нет ни одного фильтра рецепта
            # 1006 - у модели нет фильтра 106, а он должен быть
            # 1007 - у модели нет фильтра 102, а он должен быть

            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "recipes": [
                        {
                            "entity": "recipe",
                            "id": 1002,
                            "header": "One Bool Value",
                            "name": "One Bool Value",
                            "category": {"id": 1, "navigationId": 2},
                            "popularity": 0,
                            "isSponsored": NoKey("isSponsored"),
                            "filters": [
                                {
                                    "id": "104",
                                    "values": [
                                        {"value": "0", "id": "0", "checked": NoKey("checked")},
                                        {"value": "1", "id": "1", "checked": True},
                                    ],
                                }
                            ],
                        }
                    ]
                },
                allow_different_len=False,
            )

        response = self.report.request_json('place=recipes_by_doc&hyperid=603')  # glfilter=105:2,5,9
        self.assertFragmentIn(response, {"recipes": []})

        # 1001 - у рецепта фильтр 105 может быть только значения 8
        # 1002 - у модели нет ни одного фильтра рецепта
        # 1003 - у модели нет ни одного фильтра рецепта
        # 1004 - у модели нет ни одного фильтра рецепта
        # 1005 - у модели нет ни одного фильтра рецепта
        # 1006 - у модели нет ни одного фильтра рецепта
        # 1007 - у модели нет ни одного фильтра рецепта

        response = self.report.request_json('place=recipes_by_doc&hyperid=604')  # no glfilters
        self.assertFragmentIn(response, {"recipes": []})

    @classmethod
    def prepare_recipes_by_doc_second_kind_params(cls):
        '''Создаем GL-параметры второго рода,
        привязываем 2 офера к модели: с одним параметром и с двумя параметрами,
        создаем 4 рецепта:
         - подходящий под один из двух параметров второго офера
         - подходящий под параметр первого офера, но с еще одним параметром
         - подходящий под оба параметра второго офера
         - не подходящий под параметры оферов
        '''
        cls.index.gltypes += [
            GLType(
                param_id=107,
                hid=3,
                gltype=GLType.ENUM,
                values=list(range(10)),
                cluster_filter=True,
                position=1,
                model_filter_index=2,
            ),
            GLType(
                param_id=108,
                hid=3,
                gltype=GLType.ENUM,
                values=list(range(35, 46)),
                cluster_filter=True,
                position=2,
                model_filter_index=1,
            ),
        ]

        cls.index.recipes += [
            Recipe(
                recipe_id=1008, hid=3, name='All white trainers', glfilters=[GLFilterEnum(param_id=107, values=[2])]
            ),
            Recipe(
                recipe_id=1009,
                hid=3,
                name='White trainers size 39',
                glfilters=[GLFilterEnum(param_id=107, values=[2]), GLFilterEnum(param_id=108, values=[39])],
            ),
            Recipe(
                recipe_id=1010,
                hid=3,
                name='White trainers size 40',
                glfilters=[GLFilterEnum(param_id=107, values=[2]), GLFilterEnum(param_id=108, values=[40])],
            ),
            Recipe(recipe_id=1011, hid=3, name='All blue trainers', glfilters=[GLFilterEnum(param_id=107, values=[8])]),
        ]

        cls.index.models += [
            # hid=3&glfilter=107:2,108:39,40
            Model(title='trainers', hid=3, hyperid=605),
        ]

        cls.index.offers += [
            Offer(title='random color trainers size 39', hyperid=605, glparams=[GLParam(param_id=108, value=39)]),
            Offer(
                title='white trainers size 40',
                hyperid=605,
                glparams=[GLParam(param_id=107, value=2), GLParam(param_id=108, value=40)],
            ),
        ]

    def test_recipes_by_doc_second_kind_params(self):
        '''Проверяем, что параметры второго рода модели
        корректно применяются к рецептам:
          среди приматченных к модели оферов есть параметры: 107:2,108:39,40,
          поэтому подходят рецепты 1008, 1009, 1010, но не 1011.
        '''
        response = self.report.request_json('place=recipes_by_doc&hyperid=605')
        self.assertFragmentIn(
            response,
            {
                "recipes": [
                    {
                        "entity": "recipe",
                        "id": 1008,
                        "header": "All white trainers",
                        "filters": [{"id": "107", "values": [{"id": "2", "checked": True}]}],
                    },
                    {
                        "entity": "recipe",
                        "id": 1009,
                        "header": "White trainers size 39",
                        "filters": [
                            {"id": "107", "values": [{"id": "2", "checked": True}]},
                            {"id": "108", "values": [{"id": "39", "checked": True}]},
                        ],
                    },
                    {
                        "entity": "recipe",
                        "id": 1010,
                        "header": "White trainers size 40",
                        "filters": [
                            {"id": "107", "values": [{"id": "2", "checked": True}]},
                            {"id": "108", "values": [{"id": "40", "checked": True}]},
                        ],
                    },
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

    @classmethod
    def prepare_separated_seo_and_buttons(cls):
        """Создаем рецепты из описания тикета https://st.yandex-team.ru/MARKETOUT-15865
        и один рецепт is_seo и два рецепта is_button (с button_name и без)
        """
        cls.index.recipes += [
            Recipe(
                recipe_id=1586511,
                hid=1586501,
                popularity=305000,
                button_index=11,
                is_seo=True,
                is_button=True,
                glfilters=[GLFilterEnum(param_id=105, values=[8])],
            ),
            Recipe(
                recipe_id=1586513,
                hid=1586501,
                popularity=1400,
                button_index=9,
                is_seo=True,
                is_button=True,
                glfilters=[GLFilterNumeric(param_id=106, max_value=78), GLFilterEnum(param_id=105, values=[8])],
            ),
            Recipe(
                recipe_id=1586512,
                hid=1586501,
                popularity=2000,
                button_index=None,
                is_seo=True,
                is_button=True,
                glfilters=[GLFilterBool(param_id=104, value=1), GLFilterEnum(param_id=105, values=[8])],
            ),
            Recipe(
                recipe_id=1586514,
                hid=1586501,
                popularity=1200,
                button_index=12,
                is_seo=True,
                is_button=True,
                glfilters=[GLFilterNumeric(param_id=106, min_value=50), GLFilterEnum(param_id=105, values=[8])],
            ),
            Recipe(
                recipe_id=1586515,
                hid=1586501,
                popularity=1000,
                button_index=None,
                is_seo=True,
                is_button=True,
                glfilters=[
                    GLFilterNumeric(param_id=106, min_value=35, max_value=36),
                    GLFilterEnum(param_id=105, values=[8]),
                ],
            ),
            Recipe(
                recipe_id=1586516,
                hid=1586501,
                popularity=500,
                button_index=None,
                is_seo=True,
                is_button=True,
                glfilters=[GLFilterNumeric(param_id=106, max_value=78), GLFilterEnum(param_id=105, values=[8])],
            ),
            Recipe(
                recipe_id=1586517,
                hid=1586501,
                popularity=1,
                button_index=None,
                is_seo=True,
                is_button=True,
                glfilters=[GLFilterNumeric(param_id=106, max_value=78), GLFilterEnum(param_id=105, values=[8])],
            ),
            Recipe(
                recipe_id=1586518,
                hid=1586501,
                popularity=0,
                button_index=10,
                is_seo=True,
                is_button=True,
                glfilters=[GLFilterBool(param_id=104, value=1), GLFilterEnum(param_id=105, values=[8])],
            ),
            Recipe(
                recipe_id=1586519,
                hid=1586502,
                popularity=2,
                button_index=0,
                name='seo name',
                short_name='seo short_name',
                button_name='seo button_name',
                is_seo=True,
                is_button=False,
                glfilters=[GLFilterBool(param_id=104, value=1), GLFilterEnum(param_id=105, values=[8])],
            ),
            Recipe(
                recipe_id=1586520,
                hid=1586502,
                popularity=1,
                button_index=10,
                name='button name 1',
                short_name='button short_name 1',
                button_name='button button_name 1',
                is_seo=False,
                is_button=True,
                glfilters=[GLFilterNumeric(param_id=106, max_value=78), GLFilterEnum(param_id=105, values=[8])],
            ),
            Recipe(
                recipe_id=1586521,
                hid=1586502,
                popularity=0,
                button_index=1,
                name='button name 2',
                short_name='button short_name 2',
                is_seo=False,
                is_button=True,
                glfilters=[GLFilterNumeric(param_id=106, max_value=78), GLFilterEnum(param_id=105, values=[8])],
            ),
        ]

    def test_recipe_seo_sorting(self):
        """Что тестируем: сортировка в режиме показа seo-лендингов работает по
        популярности, как в эксперименте, так и без него
        """
        for params in [
            '',
            '&rearr-factors=market_separate_recipes_and_buttons=1',
            '&recipe-resource=seo&rearr-factors=market_separate_recipes_and_buttons=1',
        ]:
            response = self.report.request_json('place=recipes_contain_glfilters&hid=1586501&glfilter=105:8' + params)
            self.assertFragmentIn(
                response,
                {
                    "recipes": [
                        {"id": 1586511, "popularity": 305000, "buttonIndex": 11},
                        {"id": 1586512, "popularity": 2000, "buttonIndex": 0},
                        {"id": 1586513, "popularity": 1400, "buttonIndex": 9},
                        {"id": 1586514, "popularity": 1200, "buttonIndex": 12},
                        {"id": 1586515, "popularity": 1000, "buttonIndex": 0},
                        {"id": 1586516, "popularity": 500, "buttonIndex": 0},
                        {"id": 1586517, "popularity": 1, "buttonIndex": 0},
                        {"id": 1586518, "popularity": 0, "buttonIndex": 10},
                    ]
                },
                preserve_order=True,
                allow_different_len=False,
            )

    def test_recipe_button_sorting(self):
        """Что тестируем: сортировка в режиме показа кнопок-рецептов работает
        в эксперименте по button_index (с фоллбеком в популярность),
        а без эксперимента - только по популярности
        """
        response = self.report.request_json(
            'place=recipes_contain_glfilters&hid=1586501&glfilter=105:8&recipe-resource=handmade&rearr-factors=market_separate_recipes_and_buttons=1'
        )
        self.assertFragmentIn(
            response,
            {
                "recipes": [
                    {"id": 1586514, "popularity": 1200, "buttonIndex": 12},
                    {"id": 1586511, "popularity": 305000, "buttonIndex": 11},
                    {"id": 1586518, "popularity": 0, "buttonIndex": 10},
                    {"id": 1586513, "popularity": 1400, "buttonIndex": 9},
                    {"id": 1586512, "popularity": 2000, "buttonIndex": 0},
                    {"id": 1586515, "popularity": 1000, "buttonIndex": 0},
                    {"id": 1586516, "popularity": 500, "buttonIndex": 0},
                    {"id": 1586517, "popularity": 1, "buttonIndex": 0},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(
            'place=recipes_contain_glfilters&hid=1586501&glfilter=105:8&recipe-resource=handmade'
        )
        self.assertFragmentIn(
            response,
            {
                "recipes": [
                    {"id": 1586511, "popularity": 305000, "buttonIndex": 11},
                    {"id": 1586512, "popularity": 2000, "buttonIndex": 0},
                    {"id": 1586513, "popularity": 1400, "buttonIndex": 9},
                    {"id": 1586514, "popularity": 1200, "buttonIndex": 12},
                    {"id": 1586515, "popularity": 1000, "buttonIndex": 0},
                    {"id": 1586516, "popularity": 500, "buttonIndex": 0},
                    {"id": 1586517, "popularity": 1, "buttonIndex": 0},
                    {"id": 1586518, "popularity": 0, "buttonIndex": 10},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def check_recipes_without_experiment(self, response):
        """Проверяем, что вне эксперимента рецепты не фильтруются
        и выводятся в прежнем формате
        """
        self.assertFragmentIn(
            response,
            {
                "recipes": [
                    {"id": 1586519, "header": "seo name", "name": "seo name", "shortName": "seo short_name"},
                    {
                        "id": 1586520,
                        "header": "button name 1",
                        "name": "button name 1",
                        "shortName": "button short_name 1",
                    },
                    {
                        "id": 1586521,
                        "header": "button name 2",
                        "name": "button name 2",
                        "shortName": "button short_name 2",
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_recipe_seo_filtering_and_names(self):
        """Что тестируем: в режиме показа seo-лендингов в эксперименте на выдаче
        только рецепты с is_seo=True, button_name не выводится
        Проверяем, что без эксперимента все по-старому
        """
        response = self.report.request_json(
            'place=recipes_contain_glfilters&hid=1586502&glfilter=105:8&recipe-resource=seo&rearr-factors=market_separate_recipes_and_buttons=1'
        )
        self.assertFragmentIn(
            response,
            {"recipes": [{"id": 1586519, "header": "seo name", "name": "seo name", "shortName": "seo short_name"}]},
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(
            'place=recipes_contain_glfilters&hid=1586502&glfilter=105:8&recipe-resource=seo'
        )
        self.check_recipes_without_experiment(response)

    def test_recipe_button_filtering_and_names(self):
        """Что тестируем: в режиме показа кнопок-рецептов в эксперименте на выдаче
        только рецепты с is_button=True, button_name выводится в shortName,
        если определен
        Проверяем, что без эксперимента все по-старому
        """
        response = self.report.request_json(
            'place=recipes_contain_glfilters&hid=1586502&glfilter=105:8&recipe-resource=handmade&rearr-factors=market_separate_recipes_and_buttons=1'
        )
        self.assertFragmentIn(
            response,
            {
                "recipes": [
                    {
                        "id": 1586520,
                        "header": "button name 1",
                        "name": "button name 1",
                        "shortName": "button button_name 1",
                    },
                    {
                        "id": 1586521,
                        "header": "button name 2",
                        "name": "button name 2",
                        "shortName": "button short_name 2",
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(
            'place=recipes_contain_glfilters&hid=1586502&glfilter=105:8&recipe-resource=handmade'
        )
        self.check_recipes_without_experiment(response)

    @classmethod
    def prepare_recipes_slug(cls):
        cls.index.regiontree += [
            Region(
                rid=1,
                name='Москва и Московская область',
                children=[
                    Region(rid=213, name='Москва'),
                ],
            ),
            Region(rid=193, name='Воронеж', preposition='в', locative='Воронеже'),
            Region(rid=56, name='Челябинск', preposition='в', locative='Челябинске'),
            Region(rid=35, name='Краснодар', preposition='в', locative='Краснодаре'),
        ]

    def test_recipes_slug(self):
        """
        Во всех регионах slug должен быть без региональных приставок
        """
        expected_response = {
            "recipe": {
                "name": "Рецепт с кириллицей",
                "slug": "retsept-s-kirillitsei",
            }
        }

        # Регион не указан
        response = self.report.request_json('place=recipe_by_glfilters&hid=103&glfilter=209:93,94,95&how=aprice')
        self.assertFragmentIn(response, expected_response, preserve_order=True, allow_different_len=False)

        # Московский регион
        response = self.report.request_json(
            'place=recipe_by_glfilters&hid=103&glfilter=209:93,94,95&how=aprice&rids=213'
        )
        self.assertFragmentIn(response, expected_response, preserve_order=True, allow_different_len=False)

        # Указан регион -- должен быть обычный slug
        response = self.report.request_json(
            'place=recipe_by_glfilters&hid=103&glfilter=209:93,94,95&how=aprice&rids=56'
        )
        self.assertFragmentIn(response, expected_response, preserve_order=True, allow_different_len=False)

    @classmethod
    def prepare_best_by_factor_recipes(cls):
        """Заводим категории и факторы которые есть в market/recipes_best_by_factor.tsv"""

        cls.index.hypertree += [
            HyperCategory(
                hid=198119,
                name="Электроника",
                uniq_name="Электроника",
                children=[HyperCategory(hid=91491, name="Мобильники", uniq_name="Мобильные телефоны")],
            ),
            HyperCategory(hid=7070735, name="Самокаты", uniq_name="Самокаты"),
            HyperCategory(hid=7811915, name="Трусы", uniq_name="Женские трусы"),
        ]

        cls.index.models += [
            Model(hid=91491, hyperid=91491001, title='Xiaomi Redmi Note 8A'),
            Model(hid=91491, hyperid=91491002, title='Iphone XR'),
            Model(hid=91491, hyperid=91491003, title='Яндекс.Телефон'),
            Model(hid=91491, hyperid=91491004, title='Nokia'),
            Model(hid=7070735, hyperid=707073501, title='Самокат Oxelo'),
        ]

        cls.index.offers += [
            Offer(hid=91491, hyperid=91491001),
            Offer(hid=91491, hyperid=91491002),
            Offer(hid=91491, hyperid=91491003),
            Offer(hid=91491, hyperid=91491004),
            Offer(hid=7070735, hyperid=707073501),
        ]

        # (hid, id для тестов) -> (id продоовое, название продовое)
        factors = {
            (91491, 'экран'): (742, 'Экран'),
            (91491, 'аккумулятор'): (744, 'долгое время работы'),
            (91491, 'фото'): (743, 'качество фотографий'),
            (91491, 'память'): (745, 'Объем памяти'),
            # электроника
            (198119, 'надежность'): (803, 'Надежность'),
            (198119, 'эргономика'): (802, 'эргономика'),
            (7070735, 'дизайн'): (2998, 'Дизайн'),
            (7070735, 'комфорт'): (421, 'Комфорт катания'),
            (7070735, 'сборка'): (424, 'Качество сборки'),
        }

        def make_reasons(hid, values):
            reasons = []
            for i, (id, value) in enumerate(values.items()):
                factor_id, factor_name = factors[(hid, id)]
                reasons.append(
                    {
                        "id": "best_by_factor",
                        "type": "consumerFactor",
                        "factor_id": str(factor_id),
                        "factor_priority": str(i + 1),
                        "factor_name": "{}".format(factor_name),
                        "value": value,
                    }
                )
            return reasons

        cls.index.yamarec_places += [
            YamarecPlaceReasonsToBuy()
            .new_partition()
            .add(91491001, make_reasons(91491, {'экран': 0.85, 'аккумулятор': 0.75, 'фото': 0.74, 'память': 0.87}))
            .add(91491002, make_reasons(91491, {'экран': 0.89, 'аккумулятор': 0.44, 'фото': 0.99, 'память': 0.76}))
            .add(91491003, make_reasons(91491, {'экран': 0.56, 'аккумулятор': 0.67, 'фото': 0.85}))
            .add(91491004, make_reasons(91491, {'экран': 0.45, 'аккумулятор': 0.93}))
            .
            # раньше нокиа была в электронике и теперь у нее такие оценки есть
            add(91491004, make_reasons(198119, {'надежность': 0.99, 'эргономика': 0.82}))
            .add(707073501, make_reasons(7070735, {'дизайн': 0.85, 'комфорт': 0.91, 'сборка': 0.94}))
        ]

    def test_no_best_by_factor_recipes_without_flag(self):
        """Проверяем что без флага market_best_by_factor_recipes=1
        рецепты не появляются в
        place=recipe_by_glfilters
        place=recipes_contain_glfilters
        """
        response = self.report.request_json('place=recipes_contain_glfilters&hid=91491')
        self.assertFragmentIn(response, {'recipes': []}, allow_different_len=False)

        response = self.report.request_json('place=recipe_by_glfilters&how=best_by_factor:742&hid=91491')
        self.assertFragmentIn(response, {'recipe': None}, allow_different_len=False)

        response = self.report.request_json('place=recipes_by_doc&hyperid=91491001')
        self.assertFragmentIn(response, {'recipes': []}, allow_different_len=False)

        # под флагом рецепты появляются
        response = self.report.request_json(
            'place=recipes_contain_glfilters&hid=91491' '&rearr-factors=market_best_by_factor_recipes=1'
        )

        self.assertFragmentIn(
            response,
            {
                'recipes': [
                    {
                        'header': 'Лучшие мобильные телефоны: пользователям нравится экран',
                        'shortName': 'Пользователям нравится экран',
                        'sorts': [{'options': [{'id': 'best_by_factor:742'}]}],
                    }
                ]
            },
        )

        response = self.report.request_json(
            'place=recipe_by_glfilters&how=best_by_factor:742&hid=91491'
            '&rearr-factors=market_best_by_factor_recipes=1'
        )
        self.assertFragmentIn(
            response,
            {
                'recipe': {
                    'header': 'Лучшие мобильные телефоны: пользователям нравится экран',
                    'shortName': 'Пользователям нравится экран',
                    'sorts': [{'options': [{'id': 'best_by_factor:742'}]}],
                }
            },
        )

        response = self.report.request_json(
            'place=recipes_by_doc&hyperid=91491001' '&rearr-factors=market_best_by_factor_recipes=1'
        )
        self.assertFragmentIn(
            response,
            {
                'recipes': [
                    {
                        'header': 'Лучшие мобильные телефоны: пользователям нравится экран',
                        'shortName': 'Пользователям нравится экран',
                        'sorts': [{'options': [{'id': 'best_by_factor:742'}]}],
                    }
                ]
            },
        )

    def test_recipe_docs_incut(self):
        """Под флагом market_best_by_factor_docs_incut=N при наличии additional_entities=best_by_factor_incut
        отдаем блок recipeDocs с моделями с лучшей оценкой по данному фактору"""

        response = self.report.request_json(
            'place=prime&hid=91491&use-default-offers=1&allow-collapsing=1&debug=da'
            '&rearr-factors=market_best_by_factor_docs_incut=2&additional_entities=best_by_factor_incut'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {'results': NotEmptyList()},
                'recipeDocs': {
                    'recipe': {
                        'header': 'Лучшие мобильные телефоны: пользователям нравится экран',
                        'shortName': 'Пользователям нравится экран',
                        'sorts': [{'options': [{'id': 'best_by_factor:742'}]}],
                    },
                    'items': [
                        # отсортированы по значению фактора 742 (экран)
                        {'id': 91491002},
                        {'id': 91491001},
                    ],
                },
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # фильтр onstock=1 не влияет на наличие врезки
        response = self.report.request_json(
            'place=prime&hid=91491&use-default-offers=1&allow-collapsing=1&debug=da&onstock=1'
            '&rearr-factors=market_best_by_factor_docs_incut=2&additional_entities=best_by_factor_incut'
        )
        self.assertFragmentIn(response, {'recipeDocs': NotEmpty()})

        # если документов для врезки мало - врезка не формируется
        response = self.report.request_json(
            'place=prime&hid=91491&use-default-offers=1&allow-collapsing=1&debug=da'
            '&rearr-factors=market_best_by_factor_docs_incut=10&additional_entities=best_by_factor_incut'
        )
        self.assertFragmentNotIn(response, {'recipeDocs': NotEmpty()})

        # при значении флага market_best_by_factor_docs_incut=0 (или без флага)- врезка не формируется
        response = self.report.request_json(
            'place=prime&hid=91491&use-default-offers=1&allow-collapsing=1&debug=da'
            '&rearr-factors=market_best_by_factor_docs_incut=0&additional_entities=best_by_factor_incut'
        )
        self.assertFragmentNotIn(response, {'recipeDocs': NotEmpty()})

        # без additional_entities=best_by_factor_incut - врезка не формируется
        response = self.report.request_json(
            'place=prime&hid=91491&use-default-offers=1&allow-collapsing=1&debug=da'
            '&rearr-factors=market_best_by_factor_docs_incut=2'
        )
        self.assertFragmentNotIn(response, {'recipeDocs': NotEmpty()})

        # при наличии любой пользовательской сортировки - врезка не формируется
        response = self.report.request_json(
            'place=prime&hid=91491&use-default-offers=1&allow-collapsing=1&debug=da&how=aprice'
            '&rearr-factors=market_best_by_factor_docs_incut=2&additional_entities=best_by_factor_incut'
        )
        self.assertFragmentNotIn(response, {'recipeDocs': NotEmpty()})

        # при наличии фильтров - по цене, скидке, и т.п. - врезка не формируется
        response = self.report.request_json(
            'place=prime&hid=91491&use-default-offers=1&allow-collapsing=1&debug=da&mcpriceto=10000'
            '&rearr-factors=market_best_by_factor_docs_incut=2&additional_entities=best_by_factor_incut'
        )
        self.assertFragmentNotIn(response, {'recipeDocs': NotEmpty()})

        # если категория нелистовая - врезка не формируется
        response = self.report.request_json(
            'place=prime&hid=198119&use-default-offers=1&allow-collapsing=1&debug=da'
            '&rearr-factors=market_best_by_factor_docs_incut=10&additional_entities=best_by_factor_incut'
        )
        self.assertFragmentNotIn(response, {'recipeDocs': NotEmpty()})


if __name__ == '__main__':
    main()
