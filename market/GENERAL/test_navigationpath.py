#!/usr/bin/env python
# -*- coding: utf-8 -*-

from core.testcase import TestCase, main
from core.types.navigation_info import NavigationNode, NavigationTree
from core.types.navigation_redirects import TreeRedirects
from core.matcher import Absent


class T(TestCase):
    @classmethod
    def prepare(cls):
        '''Раньше было одно дерево и для зеленого, и для синего маркета.
        Подходит ли узел по цвету определялось значениями атрибутов is_green и is_blue
        '''
        main_nodes = [
            NavigationNode(nid=1000, hid=100, parent_nid=0, short_name='Все товары сине-зеленые'),
            NavigationNode(nid=1500, hid=0, parent_nid=1000, short_name='Бесцветный департамент', is_blue=False, is_green=False),
            NavigationNode(nid=2000, hid=200, parent_nid=1500, short_name='Мебель сине-зеленая'),
            NavigationNode(nid=2050, hid=201, parent_nid=1500, short_name='Посуда сине-зеленая'),
            NavigationNode(nid=2051, hid=202, parent_nid=2050, short_name='Керамика зеленая', is_blue=False),
            NavigationNode(nid=2052, hid=203, parent_nid=2051, short_name='Керамические тарелки сине-зеленые', is_primary=True),
            NavigationNode(nid=2100, hid=0, parent_nid=2000, short_name='Кухня зеленая', is_blue=False),
            NavigationNode(nid=2110, hid=211, parent_nid=2100, short_name='Столы в зеленой кухне', is_primary=True, is_blue=False),
            NavigationNode(nid=2200, hid=0, parent_nid=2000, short_name='Кухня синяя', is_green=False),
            NavigationNode(nid=2210, hid=211, parent_nid=2200, short_name='Столы в синей кухне', is_primary=False, is_green=False),

            NavigationNode(nid=3100, hid=311, parent_nid=2000, short_name='Комоды в зеленом дереве', is_primary=True, is_blue=False),
            NavigationNode(nid=3120, hid=312, parent_nid=2000, short_name='Стулья в сине-зеленом дереве', is_primary=True),
        ]

        cls.index.navigation_trees += [NavigationTree(code='green', nodes=main_nodes)]

        redir_map = {
            3200: 3100,  # удаленный в существующий, но скрытый
            3201: 3120   # удаленный в существующий
        }

        cls.index.navigation_redirects += [TreeRedirects('green', redir_map)]

    @staticmethod
    def wrap_up_in_2000(response):
        return {
            'result': {
                'id': 1500,
                'name': 'Бесцветный департамент',
                'navnodes': [
                    {
                        'id': 2000,
                        'name': 'Мебель сине-зеленая',
                        'navnodes': [response]
                    }
                ]
            }
        }

    def test_colorless_path_in_main_tree(self):
        # Без параметра rgb цвета узлов не проверяются
        response = self.cataloger.request_json('GetNavigationPath?nid=2110&format=json')
        self.assertFragmentIn(response, self.wrap_up_in_2000(
            {
                'id': 2100,
                'name': 'Кухня зеленая',
                'navnodes': [
                    {
                        'id': 2110,
                        'name': 'Столы в зеленой кухне'
                    }
                ]
            }
        ))

        response = self.cataloger.request_json('GetNavigationPath?nid=2210&format=json')
        self.assertFragmentIn(response, self.wrap_up_in_2000(
            {
                'id': 2200,
                'name': 'Кухня синяя',
                'navnodes': [
                    {
                        'id': 2210,
                        'name': 'Столы в синей кухне'
                    }
                ]
            }
        ))

    def test_multiple_hids(self):
        response = self.cataloger.request_json('GetNavigationPath?nid=2110&format=json&rgb=green')
        self.assertFragmentIn(response, {
            'result': {
                'id': 2000,
                'category': {
                    'id': 200
                },
                'navnodes': [
                    {
                        'id': 2100,
                        'navnodes': [
                            {
                                'category': {
                                    'id': 211
                                },
                                'id': 2110,
                            }
                        ]
                    }
                ]
            }
        })

        response = self.cataloger.request_json('GetNavigationPath?nid=2110&format=json&rgb=green&multiple-hids=true')
        self.assertFragmentIn(response, {
            'result': {
                'id': 2000,
                "categories": [
                    {"id": 200}
                ],
                'navnodes': [
                    {
                        "categories": Absent(),
                        'id': 2100,
                        'navnodes': [
                            {
                                'id': 2110,
                                'categories': [
                                    {"id": 211}
                                ]
                            }
                        ]
                    }
                ]
            }
        })

    def test_models(self):
        response = self.cataloger.request_json('GetNavigationPath?nid=2110&format=json&rgb=green')
        self.assertFragmentIn(response, {
            'result': {
                'id': 2000,
                'models': Absent(),
                'category': {
                    'id': 200
                },
                'navnodes': [
                    {
                        'id': 2100,
                        'models': Absent(),
                        'navnodes': [
                            {
                                'category': {
                                    'id': 211
                                },
                                'models': Absent(),
                                'id': 2110,
                            }
                        ]
                    }
                ]
            }
        })

        response = self.cataloger.request_json('GetNavigationPath?nid=2110&format=json&rgb=green&models=1&multiple-hids=true')
        self.assertFragmentIn(response, {
            'result': {
                'id': 2000,
                'models': [175941311, 558171067],
                'navnodes': [
                    {
                        'id': 2100,
                        'models': [175941311, 558171067],
                        'navnodes': [
                            {
                                'models': [175941311, 558171067],
                                'id': 2110,
                            }
                        ]
                    }
                ]
            }
        })

    def test_green_path_in_main_tree(self):
        # Из выдачи пропадает департамент 1500 с is_green=False
        response = self.cataloger.request_json('GetNavigationPath?nid=2110&format=json&rgb=green')
        self.assertFragmentIn(response, {
            'result': {
                'id': 2000,
                'navnodes': [
                    {
                        'id': 2100,
                        'navnodes': [
                            {
                                'id': 2110,
                            }
                        ]
                    }
                ]
            }
        })

        # У категории 2210 is_green=False, при rgb=green должно четырехсотить
        with self.assertRaises(RuntimeError):
            self.cataloger.request_json('GetNavigationPath?nid=2210&format=json&rgb=green')

    def test_blue_path_in_main_tree(self):
        response = self.cataloger.request_json('GetNavigationPath?nid=2210&format=json&rgb=blue')
        self.assertFragmentIn(response, {
            'result': {
                'id': 2000,
                'navnodes': [
                    {
                        'id': 2200,
                        'navnodes': [
                            {
                                'id': 2210,
                            }
                        ]
                    }
                ]
            }
        })

        # У категории 2110 is_blue=False, при rgb=blue должно четырехсотить
        with self.assertRaises(RuntimeError):
            self.cataloger.request_json('GetNavigationPath?nid=2110&format=json&rgb=blue')

    def test_path_with_no_blue_node_on_path(self):
        response = self.cataloger.request_json('GetNavigationPath?nid=2052&format=json&rgb=blue')
        self.assertFragmentIn(response, {
            'result': {
                'id': 2050,
                'name': 'Посуда сине-зеленая',
                'navnodes': [
                    {
                        # nid 2051 был пропущен, т.к. имеет флаг is_blue=0
                        'id': 2052,
                        'name': 'Керамические тарелки сине-зеленые',
                    }
                ]
            }
        })

    def test_primary_nids(self):
        # У хида 211 приоритетный нид 2110
        response = self.cataloger.request_json('GetNavigationPath?hid=211&format=json')
        self.assertFragmentIn(response, {
            'id': 2100,
            'navnodes': [
                {
                    'category': {
                        'id': 211
                    },
                    'id': 2110,
                }
            ]
        })

        # Но он имеет is_blue=False. В таком случае при rgb=blue
        # берется любой синий узел с таким хидом, не зависимо от is_primary
        response = self.cataloger.request_json('GetNavigationPath?hid=211&format=json&rgb=blue')
        self.assertFragmentIn(response, {
            'id': 2200,
            'navnodes': [
                {
                    'category': {
                        'id': 211
                    },
                    'id': 2210,
                }
            ]
        })

    def test_redirects(self):
        with self.assertRaisesRegexp(RuntimeError, 'bad navigation node color'):
            # Это редирект в скрытый нид
            response = self.cataloger.request_json('GetNavigationPath?nid=3200&format=json&rgb=blue')

        # Редирект из удаленного в существующий
        response = self.cataloger.request_json('GetNavigationPath?nid=3201&format=json&rgb=blue')
        self.assertFragmentIn(response, {
            'result': {
                'id': 2000,
                'navnodes': [
                    {
                        'id': 3120,
                        'oldNid': 3201,  # bad nid
                    }
                ]
            }
        })

if __name__ == '__main__':
    main()
