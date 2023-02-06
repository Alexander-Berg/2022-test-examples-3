#!/usr/bin/env python
# -*- coding: utf-8 -*-

from core.testcase import TestCase, main
from core.types.navigation_info import NavigationNode, NavigationTree
from core.types.links import NodeLink, Recipe, Filter
from core.types.categories_stats import RegionStats, CategoryStats, ShopCategories
from core.matcher import Absent, ElementCount


class T(TestCase):
    @classmethod
    def prepare(cls):
        '''Сейчас одно дерево и для зеленого, и для синего маркета.
        Подходит ли узел по цвету определяется значениями атрибутов is_green и is_blue
        '''
        main_nodes = [
            NavigationNode(nid=1000, hid=100, parent_nid=0, short_name='Все товары сине-зеленые'),

            NavigationNode(nid=2000, hid=200, parent_nid=1000, short_name='Электроника сине-зеленая'),
            NavigationNode(nid=2100, hid=210, parent_nid=2000, short_name='Телефоны сине-зеленые'),
            NavigationNode(nid=2110, hid=211, parent_nid=2100, short_name='Мобильные телефоны сине-зеленые'),
            NavigationNode(nid=2200, hid=220, parent_nid=2000, short_name='Фото и видео зеленое', is_blue=False),
            NavigationNode(nid=2210, hid=221, parent_nid=2200, short_name='Фотики синие', is_green=False),
            NavigationNode(nid=2220, hid=222, parent_nid=2200, short_name='Видеокамеры зеленые', is_blue=False),

            NavigationNode(nid=3000, hid=300, parent_nid=1000, short_name='Бытовая техника сине-зеленая'),
            NavigationNode(nid=3100, hid=310, parent_nid=3000, short_name='Для кухни зеленое', is_blue=False),
            NavigationNode(nid=3200, hid=320, parent_nid=3000, short_name='Для дома сине-зеленое', is_blue=False),
            NavigationNode(nid=3110, hid=311, parent_nid=3100, short_name='Мультиварки сине-зеленые'),
            NavigationNode(nid=3120, hid=312, parent_nid=3100, short_name='Холодильники зеленые', is_blue=False),

            NavigationNode(nid=4000, hid=0, parent_nid=1000, short_name='Подарки на 8 марта сине-зеленые'),
            NavigationNode(nid=4100, hid=311, parent_nid=4000, short_name='Мультиварки на 8 марта', is_primary=False),

            NavigationNode(nid=5000, hid=0, parent_nid=1000, short_name='Скрытые категории смешанного типа'),
            NavigationNode(nid=5100, hid=510, parent_nid=5000, short_name='Скрытая в десктопе', desktop_hide=True),
            NavigationNode(nid=5110, hid=511, parent_nid=5100, short_name='Подкатегория скрытой категории'),
            NavigationNode(nid=5200, hid=520, parent_nid=5000, short_name='Нигде не скрытая'),
            NavigationNode(nid=5210, hid=521, parent_nid=5200, short_name='Скрытая в мобильной версии', touch_hide=True),
            NavigationNode(nid=5300, hid=530, parent_nid=5000, short_name='Скрытая в приложении', application_hide=True),

            # Категории 18+ проверяются по товарному дереву, а не навигационному.
            # Они должны быть в поддереве хида 6091783. Товарное дерево в тестах
            # генерится как выжимка зеленого навигационного дерева.

            NavigationNode(nid=6000, hid=0, parent_nid=1000, short_name='Аптека'),
            # презервативы в настоящем товарном дереве не являются подкатегорией 6091783,
            # надо что бы и в ТД из зеленого было также.
            NavigationNode(nid=6100, hid=610, parent_nid=6000, short_name='Презервативы'),
            NavigationNode(nid=6200, hid=6091783, parent_nid=6000, short_name='Товары для взрослых'),
            NavigationNode(nid=6210, hid=621, parent_nid=6200, short_name='Эротическая одежда'),
            NavigationNode(nid=6211, hid=1621, parent_nid=6210, short_name='Эротические костюмы'),
            NavigationNode(nid=6212, hid=1622, parent_nid=6210, short_name='Корсеты'),

            NavigationNode(nid=7000, hid=0, parent_nid=1000, short_name='Департамент узлов-ссылок'),
            NavigationNode(nid=7100, hid=200, parent_nid=7000, short_name='Ссылка на электронику', is_primary=False, link_id=1),
            NavigationNode(nid=7110, hid=211, parent_nid=7100, short_name='Ссылка на телефонный рецепт', is_primary=False, link_id=3),
            NavigationNode(nid=7200, hid=0, parent_nid=7000, short_name='Ссылка на бытовую технику', link_id=2),
            NavigationNode(nid=7300, hid=311, parent_nid=7000, short_name='Ссылка на мультиварки с рецептом', is_primary=False, recipe_id=1),

            NavigationNode(nid=8001, hid=800, parent_nid=1000, short_name='Циклический редирект. Белый нид', recipe_id=2),
        ]

        cls.index.navigation_trees += [NavigationTree(code='green', nodes=main_nodes)]

        cls.index.navigation_links += [NodeLink(link_id=1, target='catalog',
                                                params={'hid': '200', 'nid': '2000'}
                                                ),
                                       NodeLink(link_id=2, target='catalog',
                                                params={'hid': '300', 'nid': '3000'}
                                                ),
                                       NodeLink(link_id=3, target='catalogleaf',
                                                params={'hid': '211', 'with-discount': '', 'home_region': '213'}
                                                ),
                                       ]
        recipe_1_filters = [Filter(filter_id=100500, filter_type='enum', values=['1', '2', '3']),
                            Filter(filter_id=100501, filter_type='boolean', values=['1']),
                            Filter(filter_id=100502, filter_type='boolean', values=['0']),
                            Filter(filter_id=100503, filter_type='number', min_value=10),
                            Filter(filter_id=100504, filter_type='number', max_value=100),
                            Filter(filter_id=100505, filter_type='number', min_value=20, max_value=50)
                            ]

        cls.index.navigation_recipes += [Recipe(recipe_id=1, hid=311, filters=recipe_1_filters)]

        for hid in [211, 221, 311]:
            region_list = [RegionStats(region=2, offers=cls.offers_in_hid(hid, 2)),
                           RegionStats(region=213, offers=cls.offers_in_hid(hid, 213))]
            cls.index.categories_stats += [CategoryStats(hid, region_list)]
        for hid in [211, 311, 312]:
            region_list = [RegionStats(region=135312, offers=cls.offers_in_hid(hid, 135312)),
                           RegionStats(region=146, offers=cls.offers_in_hid(hid, 146))]
            cls.index.blue_categories_stats += [CategoryStats(hid, region_list)]

        cls.index.shops_categories_stats = [ShopCategories(shop_id=500, region=213, categories=[211, 311]),
                                            ShopCategories(shop_id=500, region=2, categories=[211, 222])
                                            ]

    @staticmethod
    def offers_in_hid(hid, region):
        return hid * 1000 + region

    @staticmethod
    def wrap_up_in_result(response):
        return {'result': {'navnodes': response}}

    def test_only_departments(self):
        # По-умолчанию строится от корня. Департаменты главного дерева являются и зелеными и синими
        # Линки прицеплены ко всем деревьям
        # Проверяется основное дерево
        for rgb in ('green', 'blue', 'blue&use-multi-navigation-trees=1'):
            response = self.cataloger.request_json('GetNavigationTree?depth=1&rgb={}&format=json'.format(rgb))
            self.assertFragmentIn(response, self.wrap_up_in_result(
                [
                    {
                        'category': {
                            'id': 100,
                        },
                        'id': 1000,
                        'name': 'Все товары сине-зеленые',
                        'isLeaf': False,
                        'navnodes': [
                            {
                                'category': {
                                    'id': 200
                                },
                                'id': 2000,
                                'name': 'Электроника сине-зеленая',
                                'isLeaf': False,
                                'navnodes': Absent(),  # из-за depth=1
                                'rootNavnode': {
                                    'id': 2000  # департамент, в данном случае сам узел
                                },
                                'link': {
                                    'params': {
                                        'hid': ["200"],
                                        'nid': ["2000"]
                                    }
                                },
                            },
                            {
                                'category': {
                                    'id': 300
                                },
                                'id': 3000,
                                'fullName': 'unique name of category 3000',
                                'name': 'Бытовая техника сине-зеленая',
                                'isLeaf': False,
                                'navnodes': Absent(),
                                'slug': 'unique-name-of-category-3000',
                                'link': {
                                    'params': {
                                        'hid': ["300"],
                                        'nid': ["3000"]
                                    }
                                },
                            },
                            {
                                'category': Absent(),  # виртуальная категория
                                'id': 4000,
                                'name': 'Подарки на 8 марта сине-зеленые',
                                'isLeaf': False,
                                'navnodes': Absent(),
                            },
                            {
                                'category': Absent(),  # виртуальная категория
                                'id': 5000,
                                'name': 'Скрытые категории смешанного типа',
                                'isLeaf': False,
                                'navnodes': Absent(),
                            },
                            {
                                'category': Absent(),
                                'id': 6000,
                                'name': 'Аптека',
                                'isLeaf': False,
                                'navnodes': Absent(),
                            }
                        ]
                    }
                ]
            )),

    # У департаментов потомки имеют разные значения is_green и is_blue.
    # Узлы с неподходящим значением должны пропускаться
    def test_main_tree_with_green_filter(self):
        response = self.cataloger.request_json('GetNavigationTree?nid=2000&depth=2&rgb=green&format=json')
        self.assertFragmentIn(response, self.wrap_up_in_result(
            [
                {
                    'id': 2000,
                    'name': 'Электроника сине-зеленая',
                    'isLeaf': False,
                    'navnodes': [
                        {
                            'id': 2100,
                            'name': 'Телефоны сине-зеленые',
                            'isLeaf': False,
                            'navnodes': [
                                {
                                    'id': 2110,
                                    'name': 'Мобильные телефоны сине-зеленые',
                                    'isLeaf': True,
                                    'rootNavnode': {
                                        'id': 2000  # департамент
                                    },
                                }
                            ]
                        },
                        {
                            'id': 2200,
                            'name': 'Фото и видео зеленое',
                            'isLeaf': False,
                            'navnodes': [
                                {
                                    'id': 2220,
                                    'name': 'Видеокамеры зеленые',
                                    'isLeaf': True,
                                }
                            ]
                        },
                    ],
                },
            ]
        )),
        # проверка, что других категорий нет
        self.assertFragmentIn(response, self.wrap_up_in_result(
            [
                {
                    'id': 2000,
                    'navnodes': [
                        {
                            'id': 2100,
                            'navnodes': ElementCount(1),
                        },
                        {
                            'id': 2200,
                            'navnodes': ElementCount(1)
                        },
                    ],
                },
            ]
        ))
        self.assertFragmentIn(response, self.wrap_up_in_result(
            [
                {
                    'navnodes': ElementCount(2)
                },
            ]
        ))

    def test_main_tree_with_blue_filter(self):
        # Для синего куска дерева is_blue проверяется так же как для зеленого
        for flag in ['', '&use-multi-navigation-trees=1']:
            response = self.cataloger.request_json('GetNavigationTree?nid=2000&depth=2&rgb=blue&format=json' + flag)
            self.assertFragmentIn(response, self.wrap_up_in_result(
                [
                    {
                        'id': 2000,
                        'name': 'Электроника сине-зеленая',
                        'isLeaf': False,
                        'navnodes': [
                            {
                                'id': 2100,
                                'name': 'Телефоны сине-зеленые',
                                'isLeaf': False,
                                'navnodes': [
                                    {
                                        'id': 2110,
                                        'name': 'Мобильные телефоны сине-зеленые',
                                        'isLeaf': True,
                                        'rootNavnode': {
                                            'id': 2000
                                        },
                                    }
                                ]
                            },
                            {
                                'id': 2210,  # родительская категория пропущена, т.к она не синяя
                                'name': 'Фотики синие',
                                'isLeaf': True,
                            },
                        ],
                    },
                ]
            ))

            # проверка, что других категорий нет
            self.assertFragmentIn(response, self.wrap_up_in_result(
                [
                    {
                        'id': 2000,
                        'navnodes': [
                            {
                                'id': 2100,
                                'navnodes': ElementCount(1),
                            },
                            {
                                'id': 2210,
                                'navnodes': Absent(),
                            },
                        ],
                    },
                ]
            ))

            self.assertFragmentIn(response, self.wrap_up_in_result(
                [
                    {
                        'id': 2000,
                        'navnodes': ElementCount(2),
                    },
                ]
            ))

    def test_full_tree_without_rgb(self):
        response = self.cataloger.request_json('GetFullNavigationTree?format=json')
        # полное дерево, на цвета не смотрим
        sample = self.wrap_up_in_result(
            [
                {
                    'id': 1000,
                    'navnodes': [
                        {
                            'id': 2000,
                            'isLeaf': False,
                            'navnodes': [
                                {
                                    'id': 2100,
                                    'isLeaf': False,
                                    'navnodes': [
                                        {
                                            'id': 2110,
                                            'isLeaf': True,
                                            'navnodes': Absent(),
                                        }
                                    ]
                                },
                                {
                                    'id': 2200,
                                    'isLeaf': False,
                                    'navnodes': [
                                        {
                                            'id': 2210,
                                            'isLeaf': True,
                                            'navnodes': Absent(),
                                        },
                                        {
                                            'id': 2220,
                                            'isLeaf': True,
                                            'navnodes': Absent(),
                                        },
                                    ]
                                }
                            ]
                        },
                        {
                            'id': 3000,
                            'isLeaf': False,
                            'navnodes': [
                                {
                                    'id': 3100,
                                    'isLeaf': False,
                                    'navnodes': [
                                        {
                                            'id': 3110,
                                            'isLeaf': True,
                                            'navnodes': Absent(),
                                        },
                                        {
                                            'id': 3120,
                                            'isLeaf': True,
                                            'navnodes': Absent(),
                                        },
                                    ]
                                },
                                {
                                    'id': 3200,
                                    'isLeaf': True,
                                    'navnodes': Absent(),
                                },
                            ]
                        },
                        {
                            'id': 4000,
                            'isLeaf': False,
                            'navnodes': [
                                {
                                    'id': 4100,
                                    'isLeaf': True,
                                    'navnodes': Absent(),
                                }
                            ]
                        },
                        {
                            # скрытые категории в этом методе выдаются и без show_hidden=1
                            'id': 5000,
                            'isLeaf': False,
                            'navnodes': [
                                {
                                    'id': 5100,
                                    'isLeaf': False,
                                    'navnodes': [
                                        {
                                            'id': 5110,
                                            'isLeaf': True,
                                            'navnodes': Absent(),
                                        },
                                    ]
                                },
                                {
                                    'id': 5200,
                                    'isLeaf': False,
                                    'navnodes': [
                                        {
                                            'id': 5210,
                                            'isLeaf': True,
                                            'navnodes': Absent(),
                                        }
                                    ]
                                },
                                {
                                    'id': 5300,
                                    'isLeaf': True,
                                    'navnodes': Absent(),
                                },
                            ]
                        },
                        {
                            'id': 6000,
                            'isLeaf': False,
                            'navnodes': [
                                {
                                    'id': 6100,
                                    'isLeaf': True,
                                    'navnodes': Absent(),
                                },
                                {
                                    'id': 6200,
                                    'isLeaf': False,
                                    'navnodes': [
                                        {
                                            'id': 6210,
                                            'isLeaf': False,
                                            'navnodes': [
                                                {
                                                    'id': 6211,
                                                    'isLeaf': True,
                                                    'navnodes': Absent(),
                                                },
                                                {
                                                    'id': 6212,
                                                    'isLeaf': True,
                                                    'navnodes': Absent(),
                                                }
                                            ]
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                }
            ]
        )
        self.assertFragmentIn(response, sample)

    def test_full_tree_green(self):
        # зеленая выборка основного дерева, пропускаем узлы с is_green=False
        response = self.cataloger.request_json('GetFullNavigationTree?rgb=green&format=json')
        self.assertFragmentIn(response, self.wrap_up_in_result(
            [
                {
                    'id': 1000,
                    'isLeaf': False,
                    'navnodes': [
                        {
                            'id': 2000,
                            'isLeaf': False,
                            'navnodes': [
                                {
                                    'id': 2100,
                                    'isLeaf': False,
                                    'navnodes': ElementCount(1),
                                },
                                {
                                    'id': 2200,
                                    'isLeaf': False,
                                    'navnodes': ElementCount(1),  # узел 2210 пропускается
                                }
                            ]
                        },
                        {
                            'id': 3000,
                            'isLeaf': False,
                            'navnodes': [
                                {
                                    'id': 3100,
                                    'isLeaf': False,
                                    'navnodes': ElementCount(2),
                                },
                                {
                                    'id': 3200,
                                    'isLeaf': True,
                                    'navnodes': Absent(),
                                },
                            ]
                        },
                        {
                            'id': 4000,
                            'isLeaf': False,
                            'navnodes': [
                                {
                                    'id': 4100,
                                    'isLeaf': True,
                                    'navnodes': Absent(),
                                }
                            ]
                        },
                        {
                            'id': 6000,
                            'isLeaf': False,
                            'navnodes': [
                                {
                                    'id': 6100,
                                    'isLeaf': True,
                                    'navnodes': Absent(),
                                },
                                {
                                    'id': 6200,
                                    'isLeaf': False,
                                    'navnodes': ElementCount(1)
                                }
                            ]
                        }
                    ]
                }
            ]
        ))


if __name__ == '__main__':
    main()
