#!/usr/bin/env python
# -*- coding: utf-8 -*-

from core.testcase import TestCase, main
from core.types.navigation_info import NavigationNode, NavigationTree
from core.types.links import Recipe, Filter


class T(TestCase):
    @classmethod
    def prepare(cls):
        nodes = [
            NavigationNode(nid=1000, hid=100, parent_nid=0),
            NavigationNode(nid=7000, hid=0, parent_nid=1000, short_name='', hide_inner_nodes=False),
            NavigationNode(nid=3110, hid=311, parent_nid=3100, short_name='Мультиварки'),
            NavigationNode(
                nid=7300,
                hid=311,
                parent_nid=7000,
                short_name='Ссылка на мультиварки с рецептом',
                is_primary=False,
                recipe_id=1,
            ),
            NavigationNode(
                nid=7400, hid=740, parent_nid=7000, short_name='Большие товары в хиде 740', is_primary=True, recipe_id=2
            ),
            NavigationNode(
                nid=7500,
                hid=740,
                parent_nid=7000,
                short_name='Маленькие товары в хиде 740',
                is_primary=False,
                recipe_id=3,
            ),
            NavigationNode(nid=60200, hid=0, parent_nid=1000, is_primary=True, short_name='Узел для больших рецептов'),
            NavigationNode(
                nid=60201,
                hid=6123,
                parent_nid=60200,
                short_name='Большие товары в хиде 6123',
                is_primary=True,
                recipe_id=2 ** 33 + 1,
            ),
            NavigationNode(
                nid=60202,
                hid=6123,
                parent_nid=60200,
                short_name='Большие товары в хиде 6123',
                is_primary=True,
                recipe_id=2 ** 34 + 1,
            ),
        ]

        cls.index.navigation_trees += [NavigationTree(code='green', nodes=nodes)]

        recipe_1_filters = [
            Filter(filter_id=100500, filter_type='enum', values=['1', '2', '3']),
            Filter(filter_id=100501, filter_type='boolean', values=['1']),
            Filter(filter_id=100502, filter_type='boolean', values=['0']),
            Filter(filter_id=100503, filter_type='number', min_value=10),
            Filter(filter_id=100504, filter_type='number', max_value=100),
            Filter(filter_id=100505, filter_type='number', min_value=20, max_value=50),
        ]

        recipe_2_filters = [Filter(filter_id=100506, filter_type='number', min_value=10)]
        recipe_3_filters = [Filter(filter_id=100506, filter_type='number', max_value=10)]

        recipe_4_filters = [Filter(filter_id=100508, filter_type='number', min_value=10)]
        recipe_5_filters = [Filter(filter_id=100509, filter_type='number', max_value=10)]

        cls.index.navigation_recipes += [
            Recipe(recipe_id=1, hid=311, filters=recipe_1_filters),
            Recipe(recipe_id=2, hid=740, filters=recipe_2_filters),
            Recipe(recipe_id=3, hid=740, filters=recipe_3_filters),
            Recipe(recipe_id=2 ** 33 + 1, hid=6123, filters=recipe_4_filters),
            Recipe(recipe_id=2 ** 34 + 1, hid=6123, filters=recipe_5_filters),
        ]

    def test_big_recipe_id(self):
        '''Проверяем что большие идентификаторы рецептов (2^33 + 1) и (2^34 + 1) имеют такой же набор фильтров как и recipe_id=1.
        Текущее поведение должно изменится при смене типа идентификаторов с int -> uint64.
        Для успешного прохождения теста надо будет изменить ожидаемые значения у glfilter в ответе на "100508:10~" и "100509:~10" соотвественно.
        '''

        response = self.cataloger.request_json('GetNavigationTree?depth=5&format=json')
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'navnode',
                    'id': 60201,
                    'link': {
                        'params': {
                            'glfilter': ['100508:10~'],
                            'hid': ['6123'],
                            'nid': ['60201'],
                        },
                    },
                    'rootNavnode': {'entity': 'navnode', 'id': 60200},
                    'type': 'gl_recipe',
                },
                {
                    'entity': 'navnode',
                    'id': 60202,
                    'link': {
                        'params': {
                            'glfilter': ['100509:~10'],
                            'hid': ['6123'],
                            'nid': ['60202'],
                        },
                    },
                    'rootNavnode': {'entity': 'navnode', 'id': 60200},
                    'type': 'gl_recipe',
                },
            ],
        )


if __name__ == '__main__':
    main()
