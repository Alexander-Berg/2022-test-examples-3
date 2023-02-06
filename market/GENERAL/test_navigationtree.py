#!/usr/bin/env python
# -*- coding: utf-8 -*-

from core.testcase import TestCase, main
from core.types.navigation_info import NavigationNode, NavigationTree
from core.types.navigation_redirects import TreeRedirects
from core.types.links import NodeLink, Recipe, Filter
from core.types.categories_stats import RegionStats, CategoryStats, ShopCategories
from core.matcher import NotEmpty, Absent, ElementCount


DISCOUNT_DEPARTMENT = {
    'category': {
        'id': 100,
    },
    'id': 61522,
    'isLeaf': False,
    'name': 'Скидочный департамент',
    'navnodes': [
        {
            'category': {
                'id': 211,
            },
            'id': 60000,
            'name': 'Проверка отсеивания скидок в GetShopTree',
            'isLeaf': True,
            'navnodes': Absent(),
            'rootNavnode': {
                'id': 61522,
            },
        },
    ],
    'rootNavnode':  {
        'id': 61522,
    },
}


LINKS_DEPARTMENT = {
    "id": 7000,
    "isLeaf": False,
    "name": "Департамент узлов-ссылок",
    "navnodes": [
        {
            "category": {
                "id": 200,
            },
            "id": 7100,
            "isLeaf": False,
            "name": "Ссылка на электронику",
            "navnodes": Absent(),
            "rootNavnode": {
                "id": 7000
            },
        },
        {
            "category": {
                "id": 300,
            },
            "id": 7200,
            "isLeaf": True,
            "name": "Ссылка на бытовую технику",
            "navnodes": Absent(),
            "rootNavnode": {
                "id": 7000
            },
        },
        {
            "category": {
                "id": 311,
            },
            "id": 7300,
            "isLeaf": True,
            "name": "Ссылка на мультиварки с рецептом",
            "navnodes": Absent(),
            "rootNavnode": {
                "id": 7000
            },
        },
        {
            "category": {
                "id": 740,
            },
            "id": 7400,
            "isLeaf": True,
            "name": "Большие товары в хиде 740",
            "navnodes": Absent(),
            "rootNavnode": {
                "id": 7000
            },
        },
        {
            "category": {
                "id": 740,
            },
            "id": 7500,
            "isLeaf": True,
            "name": "Маленькие товары в хиде 740",
            "navnodes": Absent(),
            "rootNavnode": {
                "id": 7000
            },
        },
        {
            "category": {
                "id": 740,
            },
            "id": 7600,
            "isLeaf": True,
            "name": "Ссылка на узел-рецепт",
            "navnodes": Absent(),
            "rootNavnode": {
                "id": 7000
            },
        },
        {
            "category": {
                "id": 770,
            },
            "id": 7700,
            "isLeaf": True,
            "name": "Полезный узел-рецепт с is_primary=1",
            "navnodes": Absent(),
            "rootNavnode": {
                "id": 7000
            },
        },
    ],
    "rootNavnode": {
        "id": 7000
    },
}


class T(TestCase):
    @classmethod
    def prepare(cls):
        '''Теперь одно дерево и для зеленого, и для синего маркета.
        Подходит ли узел по цвету определяется значениями атрибутов is_green и is_blue
        '''
        main_nodes = [
            NavigationNode(nid=1000, hid=100, parent_nid=0, short_name='Все товары сине-зеленые'),

            NavigationNode(nid=2000, hid=200, parent_nid=1000, short_name='Электроника сине-зеленая'),
            NavigationNode(nid=2100, hid=210, parent_nid=2000, short_name='Телефоны сине-зеленые', tags=['tag1', 'tag2']),
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

            NavigationNode(nid=7000, hid=0, parent_nid=1000, short_name='Департамент узлов-ссылок', hide_inner_nodes=False),
            NavigationNode(nid=7100, hid=200, parent_nid=7000, short_name='Ссылка на электронику', is_primary=False, link_id=1),
            NavigationNode(nid=7110, hid=211, parent_nid=7100, short_name='Ссылка на телефонный рецепт', is_primary=False, link_id=3),
            NavigationNode(nid=7200, hid=0, parent_nid=7000, short_name='Ссылка на бытовую технику', link_id=2),
            NavigationNode(nid=7300, hid=311, parent_nid=7000, short_name='Ссылка на мультиварки с рецептом', is_primary=False, recipe_id=1),
            NavigationNode(nid=7400, hid=740, parent_nid=7000, short_name='Большие товары в хиде 740', is_primary=True, recipe_id=2),
            NavigationNode(nid=7500, hid=740, parent_nid=7000, short_name='Маленькие товары в хиде 740', is_primary=False, recipe_id=3),
            NavigationNode(nid=7600, hid=740, parent_nid=7000, short_name='Ссылка на узел-рецепт', is_primary=False, link_id=4),
            NavigationNode(nid=7700, hid=770, parent_nid=7000, short_name='Полезный узел-рецепт с is_primary=1', is_primary=True, recipe_id=5),

            NavigationNode(nid=8002, hid=902, parent_nid=1000, short_name='Узел с редиректом в это же дерево', hide_inner_nodes=True),
            NavigationNode(nid=8012, hid=912, parent_nid=1000, short_name='Узел на который идет редирект'),
            # Скрытый на синем
            NavigationNode(nid=8111, hid=922, parent_nid=1000, short_name='Комоды в зеленом дереве', is_primary=True, is_blue=False),

            # Узел со скрытыми дочерними узлами
            NavigationNode(nid=9000, hid=950, parent_nid=1000, short_name='Узел, скрывающий дочерние узлы', hide_inner_nodes=True),
            NavigationNode(nid=9100, hid=960, parent_nid=9000, short_name='Узел, которого не должно быть видно 1'),
            NavigationNode(nid=9100, hid=960, parent_nid=9000, short_name='Узел, которого не должно быть видно 2', hide_inner_nodes=False),
            NavigationNode(nid=9200, hid=970, parent_nid=9000, short_name='Узел, которого не должно быть видно 3'),
            NavigationNode(nid=9110, hid=961, parent_nid=9100, short_name='Узел, которого всё равно не должно быть видно 1'),
            NavigationNode(nid=9120, hid=962, parent_nid=9100, short_name='Узел, которого всё равно не должно быть видно 2'),

            # Узлы виртуальные, т.к. используются только для проверки навигационного дерева
            NavigationNode(nid=70000, hid=0, parent_nid=1000, short_name='Детские товары синие', is_green=False, hide_inner_nodes=False),
            # Взят реальный nid, потому что он захардкожен
            NavigationNode(nid=77681, hid=0, parent_nid=70000, short_name='Детские одежда и обувь синие', is_green=False),
            NavigationNode(nid=78001, hid=0, parent_nid=77681, short_name='Верхняя одежда синяя', is_green=False),
            NavigationNode(nid=78011, hid=0, parent_nid=78001, short_name='Шапки синие', is_green=False),
            NavigationNode(nid=78012, hid=0, parent_nid=78001, short_name='Куртки синие', is_green=False),
            NavigationNode(nid=78002, hid=0, parent_nid=77681, short_name='Для девочек синяя', is_green=False),
            NavigationNode(nid=78003, hid=0, parent_nid=77681, short_name='Для мальчиков синяя', is_green=False),
            NavigationNode(nid=77000, hid=0, parent_nid=70000, short_name='Детские игрушки и игры синие', is_green=False),
            NavigationNode(nid=79001, hid=0, parent_nid=77000, short_name='Конструкторы синие', is_green=False),
            NavigationNode(nid=79002, hid=0, parent_nid=77000, short_name='Игры синие', is_green=False, hide_inner_nodes=True),

            NavigationNode(nid=50000, hid=0, parent_nid=1000, short_name='Аптека синяя', is_green=False),
            NavigationNode(nid=51000, hid=610, parent_nid=50000, short_name='Презервативы синие аптечные', is_green=False),
            NavigationNode(nid=52000, hid=6091783, parent_nid=50000, short_name='Товары для взрослых синие', is_green=False),
            NavigationNode(nid=52100, hid=621, parent_nid=52000, short_name='Эротические одежда синяя', is_green=False),
            NavigationNode(nid=52110, hid=1621, parent_nid=52100, short_name='Эротические костюмы синие', is_green=False),
            NavigationNode(nid=52120, hid=1622, parent_nid=52100, short_name='Корсеты синие', is_green=False),
            NavigationNode(nid=52200, hid=610, parent_nid=52000, is_primary=False, short_name='Презервативы синие взрослые', is_green=False),

            NavigationNode(nid=61522, hid=100, parent_nid=1000, is_primary=False, short_name='Скидочный департамент'),
            NavigationNode(nid=60000, hid=211, parent_nid=61522, is_primary=False, short_name='Проверка отсеивания скидок в GetShopTree'),
        ]

        b2b_nodes = [
            NavigationNode(nid=10000, hid=100, parent_nid=0, short_name='Все товары B2B'),

            NavigationNode(nid=20000, hid=200, parent_nid=10000, short_name='Электроника B2B'),
            NavigationNode(nid=21000, hid=210, parent_nid=20000, short_name='Телефоны B2B', tags=['tag1', 'tag2']),
        ]

        # На дереве fmcg проверяем, что считывается дерево с кодом green, а не первое попавшееся
        # На дереве blue проверяем, что оно не прорастет в основное  дерево
        cls.index.navigation_trees += [NavigationTree(code='fmcg', nodes=list()),
                                       NavigationTree(code='green', nodes=main_nodes),
                                       NavigationTree(code='blue', nodes=main_nodes),
                                       NavigationTree(code='b2b', nodes=b2b_nodes)]

        green_redir = {
            8002 : 8012,    # Редирект в это же дерево, не должен срабатывать ни прямой ни обратный редирект
            100600 : 8012,  # Несуществующий в существующий
            100601 : 8111,  # Несуществующий в существующий, но скрытый на синем
        }

        cls.index.navigation_redirects += [TreeRedirects('green', green_redir)]

        cls.index.navigation_links += [NodeLink(link_id=1, target='catalog',
                                                params={'hid': '200', 'nid': '2000'}
                                                ),
                                       NodeLink(link_id=2, target='catalog',
                                                params={'hid': '300', 'nid': '3000'}
                                                ),
                                       NodeLink(link_id=3, target='catalogleaf',
                                                params={'hid': '211', 'with-discount': '', 'home_region': '213'}
                                                ),
                                       NodeLink(link_id=4, target='catalogleaf',
                                                params={'hid': '740', 'nid': '7400'}
                                                ),
                                       NodeLink(link_id=5, target='catalogleaf',
                                                params={'hid': '770', 'with-discount': '', 'home_region': '213'}
                                                ),
                                       ]
        recipe_1_filters = [Filter(filter_id=100500, filter_type='enum', values=['1', '2', '3']),
                            Filter(filter_id=100501, filter_type='boolean', values=['1']),
                            Filter(filter_id=100502, filter_type='boolean', values=['0']),
                            Filter(filter_id=100503, filter_type='number', min_value=10),
                            Filter(filter_id=100504, filter_type='number', max_value=100),
                            Filter(filter_id=100505, filter_type='number', min_value=20, max_value=50)
                            ]
        recipe_2_filters = [Filter(filter_id=100506, filter_type='number', min_value=10)]
        recipe_3_filters = [Filter(filter_id=100506, filter_type='number', max_value=10)]

        cls.index.navigation_recipes += [Recipe(recipe_id=1, hid=311, filters=recipe_1_filters),
                                         Recipe(recipe_id=2, hid=740, filters=recipe_2_filters),
                                         Recipe(recipe_id=3, hid=740, filters=recipe_3_filters)]

        for hid in [211, 221, 311, 770]:
            region_list = [RegionStats(region=2, offers=cls.offers_in_hid(hid, 2)),
                           RegionStats(region=213, offers=cls.offers_in_hid(hid, 213))]
            cls.index.categories_stats += [CategoryStats(hid, region_list)]
        for hid in [211, 311, 312]:
            region_list = [RegionStats(region=135312, offers=cls.offers_in_hid(hid, 135312)),
                           RegionStats(region=146, offers=cls.offers_in_hid(hid, 146))]
            cls.index.blue_categories_stats += [CategoryStats(hid, region_list)]

        cls.index.shops_cpa_categories_stats = [ShopCategories(shop_id=500, region=213, categories=[211, 311, 740]),
                                                ShopCategories(shop_id=500, region=2, categories=[211, 222]),
                                                ShopCategories(shop_id=600, region=213, categories=[221, 312]),
                                                ShopCategories(shop_id=800, region=213, categories=[221]),
                                                ShopCategories(shop_id=900, region=213, categories=[222]),
                                                ]
        cls.index.shops_categories_stats = cls.index.shops_cpa_categories_stats + [ShopCategories(shop_id=650, region=213, categories=[222])]
        cls.index.suppliers_categories_stats = [ShopCategories(shop_id=900, region=213, categories=[221]),
                                                ShopCategories(shop_id=1000, region=213, categories=[211]),
                                                ShopCategories(shop_id=1100, region=213, categories=[221]),
                                                ShopCategories(shop_id=1200, region=213, categories=[6091783]),
                                                ShopCategories(shop_id=9012345678, region=213, categories=[221])
                                                ]
        # магазины 500, 600 и 650 принадлежат одному бизнес партнеру, и магазины 1000 и 1100 тоже
        cls.index.shops = [{'shop_id': 500, 'business_id': 100500},
                           {'shop_id': 600, 'business_id': 100500},
                           {'shop_id': 650, 'business_id': 100500},
                           {'shop_id': 700, 'business_id': 100501},
                           {'shop_id': 800, 'business_id': 100503, 'is_enabled': False},
                           {'shop_id': 900, 'business_id': 100504},
                           {'shop_id': 1000, 'business_id': 100505},
                           {'shop_id': 1100, 'business_id': 100505},
                           {'shop_id': 1200, 'business_id': 12000},
                           {'shop_id': 9012345678, 'business_id': 1234567890},
                           ]

    @staticmethod
    def offers_in_hid(hid, region):
        return hid * 1000 + region

    @staticmethod
    def wrap_up_in_result(response):
        return {'result': {'navnodes': response}}

    def test_only_departments(self):
        # По-умолчанию строится от корня. Проверяются не все департаменты, т.к забывают их добавлять в тест
        for rgb in ('green', 'blue'):
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
                            },
                            {
                                'category': Absent(),
                                'id': 7000,
                                'name': 'Департамент узлов-ссылок',
                                'isLeaf': False,
                                'navnodes': Absent(),
                            },
                            {
                                'category': {
                                    'id': 100
                                },
                                'id': 61522,
                                'name': 'Скидочный департамент',
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
        response = self.cataloger.request_json('GetNavigationTree?nid=2000&depth=2&rgb=blue&format=json')
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

    def test_wrong_color(self):
        # У категории 2200 is_blue=False, при rgb=blue должна быть пустая выдача
        response = self.cataloger.request_json('GetNavigationTree?nid=2200&format=json&rgb=blue')
        self.assertFragmentNotIn(response, {'navnodes': NotEmpty()})

    def test_main_tree(self):
        # Больше нет отдельного синего дерева, поэтому всегда вывод главного
        # Сперва запросим синию часть главного дерева
        response = self.cataloger.request_json('GetNavigationTree?depth=2&rgb=blue&format=json')
        self.assertFragmentIn(response, self.wrap_up_in_result(
            [
                {
                    "category": {
                        "id": 100,
                    },
                    "id": 1000,
                    "isLeaf": False,
                    "name": "Все товары сине-зеленые",
                    "navnodes": [
                        {
                            "category": {
                                "id": 200,
                            },
                            "id": 2000,
                            "isLeaf": False,
                            "name": "Электроника сине-зеленая",
                            "navnodes": [
                                {
                                    "category": {
                                        "id": 210,
                                    },
                                    "id": 2100,
                                    "isLeaf": False,
                                    "name": "Телефоны сине-зеленые",
                                    "navnodes": Absent(),  # тк depth == 2
                                    "rootNavnode": {
                                        "id": 2000
                                    },
                                },
                                {
                                    "category": {
                                        "id": 221,
                                    },
                                    "id": 2210,
                                    "isLeaf": True,
                                    "name": "Фотики синие",
                                    "navnodes": Absent(),
                                    "rootNavnode": {
                                        "id": 2000
                                    },
                                }
                            ],
                            "rootNavnode": {
                                "id": 2000
                            },
                        },
                        {
                            "category": {
                                "id": 300,
                            },
                            "id": 3000,
                            "isLeaf": False,
                            "name": "Бытовая техника сине-зеленая",
                            "navnodes": [
                                {
                                    "category": {
                                        "id": 311,
                                    },
                                    "id": 3110,
                                    "isLeaf": True,
                                    "name": "Мультиварки сине-зеленые",
                                    "navnodes": Absent(),
                                    "rootNavnode": {
                                        "id": 3000
                                    },
                                }
                            ],
                            "rootNavnode": {
                                "id": 3000
                            },
                        },
                        {
                            "id": 4000,
                            "isLeaf": False,
                            "name": "Подарки на 8 марта сине-зеленые",
                            "navnodes": [
                                {
                                    "category": {
                                        "id": 311,
                                    },
                                    "id": 4100,
                                    "isLeaf": True,
                                    "name": "Мультиварки на 8 марта",
                                    "navnodes": Absent(),
                                    "rootNavnode": {
                                        "id": 4000
                                    },
                                }
                            ],
                            "rootNavnode": {
                                "id": 4000
                            },
                            "type": "virtual"
                        },
                        {
                            "id": 5000,
                            "isLeaf": False,
                            "name": "Скрытые категории смешанного типа",
                            "navnodes": [
                                {
                                    "category": {
                                        "id": 520,
                                    },
                                    "id": 5200,
                                    "isLeaf": False,
                                    "name": "Нигде не скрытая",
                                    "navnodes": Absent(),
                                    "rootNavnode": {
                                        "id": 5000
                                    },
                                },
                                {
                                    "category": {
                                        "id": 530,
                                    },
                                    "id": 5300,
                                    "isLeaf": True,
                                    "name": "Скрытая в приложении",
                                    "navnodes": Absent(),
                                    "rootNavnode": {
                                        "id": 5000
                                    },
                                }
                            ],
                            "rootNavnode": {
                                "id": 5000
                            },
                            "type": "virtual"
                        },
                        {
                            "id": 6000,
                            "isLeaf": False,
                            "name": "Аптека",
                            "navnodes": [
                                {
                                    "category": {
                                        "id": 610,
                                    },
                                    "id": 6100,
                                    "isLeaf": True,
                                    "name": "Презервативы",
                                    "navnodes": Absent(),
                                    "rootNavnode": {
                                        "id": 6000
                                    },
                                },
                                {
                                    "adult": True,
                                    "category": {
                                        "id": 6091783,
                                    },
                                    "id": 6200,
                                    "isLeaf": False,
                                    "name": "Товары для взрослых",
                                    "navnodes": Absent(),
                                    "rootNavnode": {
                                        "id": 6000
                                    },
                                }
                            ],
                            "rootNavnode": {
                                "id": 6000
                            },
                            "type": "virtual"
                        },
                        LINKS_DEPARTMENT,
                        {
                            "category": {
                                "id": 902,
                            },
                            "id": 8002,
                            # "isLeaf": True,
                            "name": "Узел с редиректом в это же дерево",
                            # "navnodes": Absent(),
                            "rootNavnode": {
                                "id": 8002
                            },
                        },
                        {
                            "category": {
                                "id": 912,
                            },
                            "id": 8012,
                            "isLeaf": True,
                            "name": "Узел на который идет редирект",
                            "navnodes": Absent(),
                            "rootNavnode": {
                                "id": 8012
                            },
                        },
                        {
                            "id": 9000,
                            # "navnodes": Absent(),
                        },
                        {
                            "id": 70000,
                            "isLeaf": False,
                            "name": "Детские товары синие",
                            "navnodes": [
                                {
                                    "id": 77681,
                                    "isLeaf": False,
                                    "name": "Детские одежда и обувь синие",
                                    "navnodes": Absent(),
                                    "rootNavnode": {
                                        "id": 70000
                                    },
                                },
                                {
                                    "id": 77000,
                                    "isLeaf": False,
                                    "name": "Детские игрушки и игры синие",
                                    "navnodes": Absent(),
                                    "rootNavnode": {
                                        "id": 70000
                                    },
                                }
                            ],
                            "rootNavnode": {
                                "id": 70000
                            },
                        },
                        {
                            "id": 50000,
                            "isLeaf": False,
                            "name": "Аптека синяя",
                            "navnodes": [
                                {
                                    "category": {
                                        "id": 610,
                                    },
                                    "id": 51000,
                                    "isLeaf": True,
                                    "name": "Презервативы синие аптечные",
                                    "navnodes": Absent(),
                                    "rootNavnode": {
                                        "id": 50000
                                    },
                                },
                                {
                                    "adult": True,
                                    "category": {
                                        "id": 6091783,
                                    },
                                    "id": 52000,
                                    "isLeaf": False,
                                    "name": "Товары для взрослых синие",
                                    "navnodes": Absent(),
                                    "rootNavnode": {
                                        "id": 50000
                                    },
                                }
                            ],
                            "rootNavnode": {
                                "id": 50000
                            },
                        },
                        DISCOUNT_DEPARTMENT,
                    ],
                }
            ]
        ),
        allow_different_len=False),

        # Теперь зеленую часть
        response = self.cataloger.request_json('GetNavigationTree?depth=2&rgb=green&format=json')
        self.assertFragmentIn(response, self.wrap_up_in_result(
            [
                {
                    "category": {
                        "id": 100,
                    },
                    "id": 1000,
                    "isLeaf": False,
                    "name": "Все товары сине-зеленые",
                    "navnodes": [
                        {
                            "category": {
                                "id": 200,
                            },
                            "id": 2000,
                            "isLeaf": False,
                            "name": "Электроника сине-зеленая",
                            "navnodes": [
                                {
                                    "category": {
                                        "id": 210,
                                    },
                                    "id": 2100,
                                    "isLeaf": False,
                                    "name": "Телефоны сине-зеленые",
                                    "rootNavnode": {
                                        "id": 2000
                                    },
                                },
                                {
                                    "category": {
                                        "id": 220,
                                    },
                                    "id": 2200,
                                    "isLeaf": False,
                                    "name": "Фото и видео зеленое",
                                    "rootNavnode": {
                                        "id": 2000
                                    },
                                }
                            ],
                            "rootNavnode": {
                                "id": 2000
                            },
                        },
                        {
                            "category": {
                                "id": 300,
                            },
                            "id": 3000,
                            "isLeaf": False,
                            "name": "Бытовая техника сине-зеленая",
                            "navnodes": [
                                {
                                    "category": {
                                        "id": 310,
                                    },
                                    "id": 3100,
                                    "isLeaf": False,
                                    "name": "Для кухни зеленое",
                                    "rootNavnode": {
                                        "id": 3000
                                    },
                                },
                                {
                                    "category": {
                                        "id": 320,
                                    },
                                    "id": 3200,
                                    "isLeaf": True,
                                    "name": "Для дома сине-зеленое",
                                    "rootNavnode": {
                                        "id": 3000
                                    },
                                }
                            ],
                            "rootNavnode": {
                                "id": 3000
                            },
                        },
                        {
                            "id": 4000,
                            "isLeaf": False,
                            "name": "Подарки на 8 марта сине-зеленые",
                            "navnodes": [
                                {
                                    "category": {
                                        "id": 311,
                                    },
                                    "id": 4100,
                                    "isLeaf": True,
                                    "name": "Мультиварки на 8 марта",
                                    "rootNavnode": {
                                        "id": 4000
                                    },
                                }
                            ],
                            "rootNavnode": {
                                "id": 4000
                            },
                        },
                        {
                            "id": 5000,
                            "isLeaf": False,
                            "name": "Скрытые категории смешанного типа",
                            "navnodes": [
                                {
                                    "category": {
                                        "id": 520,
                                    },
                                    "id": 5200,
                                    "isLeaf": False,
                                    "name": "Нигде не скрытая",
                                    "rootNavnode": {
                                        "id": 5000
                                    },
                                },
                                {
                                    "category": {
                                        "id": 530,
                                    },
                                    "id": 5300,
                                    "isLeaf": True,
                                    "name": "Скрытая в приложении",
                                    "rootNavnode": {
                                        "id": 5000
                                    },
                                }
                            ],
                            "rootNavnode": {
                                "id": 5000
                            },
                        },
                        {
                            "id": 6000,
                            "isLeaf": False,
                            "name": "Аптека",
                            "navnodes": [
                                {
                                    "category": {
                                        "id": 610,
                                    },
                                    "id": 6100,
                                    "isLeaf": True,
                                    "name": "Презервативы",
                                    "rootNavnode": {
                                        "id": 6000
                                    },
                                },
                                {
                                    "adult": True,
                                    "category": {
                                        "id": 6091783,
                                    },
                                    "id": 6200,
                                    "isLeaf": False,
                                    "name": "Товары для взрослых",
                                    "rootNavnode": {
                                        "id": 6000
                                    },
                                }
                            ],
                            "rootNavnode": {
                                "id": 6000
                            },
                        },
                        LINKS_DEPARTMENT,
                        {
                            "category": {
                                "id": 902,
                            },
                            "id": 8002,
                            "isLeaf": True,
                            "name": "Узел с редиректом в это же дерево",
                            "rootNavnode": {
                                "id": 8002
                            },
                        },
                        {
                            "category": {
                                "id": 912,
                            },
                            "id": 8012,
                            "isLeaf": True,
                            "name": "Узел на который идет редирект",
                            "rootNavnode": {
                                "id": 8012
                            },
                        },
                        {
                            "category": {
                                "id": 922,
                            },
                            "id": 8111,
                            "isLeaf": True,
                            "name": "Комоды в зеленом дереве",
                            "rootNavnode": {
                                "id": 8111
                            },
                        },
                        {
                            "id": 9000,
                            # "navnodes": Absent(),
                        },
                        DISCOUNT_DEPARTMENT,
                    ],
                }
            ]
        ),
        allow_different_len=False),

    def test_b2b_tree(self):
        response = self.cataloger.request_json('GetNavigationTree?format=json&rgb=black&show_empty=1&depth=2')
        self.assertFragmentIn(response, self.wrap_up_in_result(
            [{
                "category":
                {
                    "id": 100,
                    "name": "tovar category 100",
                    "nid": 10000,
                },
                "id": 10000,
                "link":
                {
                    "params":
                    {
                        "hid": ["100"],
                        "nid": ["10000"]
                    },
                    "target": "catalog"
                },
                "name": "Все товары B2B",
                "navnodes":
                [{
                    "category":
                    {
                        "id": 200,
                        "name": "tovar category 200",
                        "nid": 20000,
                    },
                    "id": 20000,
                    "link":
                    {
                        "params":
                        {
                            "hid": ["200"],
                            "nid": ["20000"]
                        },
                        "target": "department"
                    },
                    "name": "Электроника B2B",
                    "navnodes":
                    [{
                        "category":
                        {
                            "id": 210,
                            "name": "tovar category 210",
                            "nid": 21000,
                        },
                        "id": 21000,
                        "link":
                        {
                            "params":
                            {
                                "hid": ["210"],
                                "nid": ["21000"]
                            },
                            "target": "catalog"
                        },
                        "name": "Телефоны B2B",
                        "rootNavnode":
                        {
                            "id": 20000
                        },
                       "tags":
                        ["tag1", "tag2"],
                    }],
                    "rootNavnode":
                    {
                        "id": 20000
                    },
                }]
        }]
    ))

    def test_full_tree_without_rgb(self):
        response = self.cataloger.request_json('GetFullNavigationTree?format=json')
        # полное дерево, на цвета не смотрим
        self.assertFragmentIn(response, self.wrap_up_in_result(
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
                        },
                        {
                            "id": 7000,
                            "isLeaf": False,
                            "navnodes": [
                                {
                                    "id": 7100,
                                    "isLeaf": False,
                                    "navnodes": [
                                        {
                                            "id": 7110,
                                            "isLeaf": True,
                                            "navnodes": Absent(),
                                        }
                                    ],
                                },
                                {
                                    "id": 7200,
                                    "isLeaf": True,
                                    "navnodes": Absent(),
                                },
                                {
                                    "id": 7300,
                                    "isLeaf": True,
                                    "navnodes": Absent(),
                                },
                                {
                                    "id": 7400,
                                    "isLeaf": True,
                                    "navnodes": Absent(),
                                },
                                {
                                    "id": 7500,
                                    "isLeaf": True,
                                    "navnodes": Absent(),
                                },
                                {
                                    "id": 7600,
                                    "isLeaf": True,
                                    "navnodes": Absent(),
                                },
                                {
                                    "id": 7700,
                                    "isLeaf": True,
                                    "navnodes": Absent(),
                                },
                            ],
                        },
                        {
                            "id": 8002,
                            # "isLeaf": True,
                            # "navnodes": Absent(),
                        },
                        {
                            "id": 8012,
                            "isLeaf": True,
                            "navnodes": Absent(),
                        },
                        {
                            "id": 8111,
                            "isLeaf": True,
                            "navnodes": Absent(),
                        },
                        {
                            "id": 9000,
                            # "navnodes": Absent(),
                        },
                        {
                            "id": 70000,
                            "isLeaf": False,
                            "navnodes": [
                                {
                                    "id": 77681,
                                    "isLeaf": False,
                                    "navnodes": [
                                        {
                                            "id": 78001,
                                            "isLeaf": False,
                                            "navnodes": [
                                                {
                                                    "id": 78011,
                                                    "isLeaf": True,
                                                    "navnodes": Absent(),
                                                },
                                                {
                                                    "id": 78012,
                                                    "isLeaf": True,
                                                    "navnodes": Absent(),
                                                }
                                            ],
                                        },
                                        {
                                            "id": 78002,
                                            "isLeaf": True,
                                            "navnodes": Absent(),
                                        },
                                        {
                                            "id": 78003,
                                            "isLeaf": True,
                                            "navnodes": Absent(),
                                        }
                                    ],
                                },
                                {
                                    "id": 77000,
                                    "isLeaf": False,
                                    "navnodes": [
                                        {
                                            "id": 79001,
                                            "isLeaf": True,
                                            "navnodes": Absent(),
                                        },
                                        {
                                            "id": 79002,
                                            # "isLeaf": True,
                                            # "navnodes": Absent(),
                                        }
                                    ],
                                }
                            ],
                        },
                        {
                            "id": 50000,
                            "isLeaf": False,
                            "navnodes": [
                                {
                                    "id": 51000,
                                    "isLeaf": True,
                                    "navnodes": Absent(),
                                },
                                {
                                    "id": 52000,
                                    "isLeaf": False,
                                    "navnodes": [
                                        {
                                            "id": 52100,
                                            "isLeaf": False,
                                            "navnodes": [
                                                {
                                                    "id": 52110,
                                                    "isLeaf": True,
                                                    "navnodes": Absent(),
                                                },
                                                {
                                                    "id": 52120,
                                                    "isLeaf": True,
                                                    "navnodes": Absent(),
                                                }
                                            ],
                                        },
                                        {
                                            "id": 52200,
                                            "isLeaf": True,
                                            "navnodes": Absent(),
                                        }
                                    ],
                                }
                            ],
                        },
                        DISCOUNT_DEPARTMENT,
                    ]
                }
            ]
        ),
        allow_different_len=False)

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
                            "id": 5000,
                            "isLeaf": False,
                            "navnodes": [
                                {
                                    "id": 5100,
                                    "isLeaf": False,
                                    "navnodes": [
                                        {
                                            "id": 5110,
                                            "isLeaf": True,
                                            "navnodes": Absent(),
                                        }
                                    ],
                                },
                                {
                                    "id": 5200,
                                    "isLeaf": False,
                                    "navnodes": [
                                        {
                                            "id": 5210,
                                            "isLeaf": True,
                                            "navnodes": Absent(),
                                        }
                                    ],
                                },
                                {
                                    "id": 5300,
                                    "isLeaf": True,
                                    "navnodes": Absent(),
                                }
                            ],
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
                        },
                        {
                            "id": 7000,
                            "isLeaf": False,
                            "navnodes": [
                                {
                                    "id": 7100,
                                    "isLeaf": False,
                                    "navnodes": [
                                        {
                                            "id": 7110,
                                            "isLeaf": True,
                                            "navnodes": Absent(),
                                        }
                                    ],
                                },
                                {
                                    "id": 7200,
                                    "isLeaf": True,
                                    "navnodes": Absent(),
                                },
                                {
                                    "id": 7300,
                                    "isLeaf": True,
                                    "navnodes": Absent(),
                                },
                                {
                                    "id": 7400,
                                    "isLeaf": True,
                                    "navnodes": Absent(),
                                },
                                {
                                    "id": 7500,
                                    "isLeaf": True,
                                    "navnodes": Absent(),
                                },
                                {
                                    "id": 7600,
                                    "isLeaf": True,
                                    "navnodes": Absent(),
                                },
                                {
                                    "id": 7700,
                                    "isLeaf": True,
                                    "navnodes": Absent(),
                                }
                            ],
                        },
                        {
                            "id": 8002,
                            # "isLeaf": True,
                            # "navnodes": Absent(),
                        },
                        {
                            "id": 8012,
                            "isLeaf": True,
                            "navnodes": Absent(),
                        },
                        {
                            "id": 8111,
                            "isLeaf": True,
                            "navnodes": Absent(),
                        },
                        {
                            "id": 9000,
                            # "navnodes": Absent(),
                        },
                        DISCOUNT_DEPARTMENT,
                    ]
                }
            ]
        ),
        allow_different_len=False)

    def test_full_tree_blue(self):
        # синяя выборка основного дерева, пропускаем узлы с is_blue=False
        response = self.cataloger.request_json('GetFullNavigationTree?rgb=blue&format=json')
        self.assertFragmentIn(response, self.wrap_up_in_result(
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
                                    'id': 2210,
                                    'isLeaf': True,
                                    'navnodes': Absent(),
                                }
                            ]
                        },
                        {
                            'id': 3000,
                            'isLeaf': False,
                            'navnodes': [
                                {
                                    'id': 3110,
                                    'isLeaf': True,
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
                            "id": 5000,
                            "isLeaf": False,
                            "navnodes": [
                                {
                                    "id": 5100,
                                    "isLeaf": False,
                                    "navnodes": [
                                        {
                                            "id": 5110,
                                            "isLeaf": True,
                                            'navnodes': Absent(),
                                        }
                                    ],
                                },
                                {
                                    "id": 5200,
                                    "isLeaf": False,
                                    "navnodes": [
                                        {
                                            "id": 5210,
                                            "isLeaf": True,
                                            'navnodes': Absent(),
                                        }
                                    ],
                                },
                                {
                                    "id": 5300,
                                    "isLeaf": True,
                                    'navnodes': Absent(),
                                }
                            ],
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
                        },
                        {
                            "id": 7000,
                            "isLeaf": False,
                            "navnodes": [
                                {
                                    "id": 7100,
                                    "isLeaf": False,
                                    "navnodes": [
                                        {
                                            "id": 7110,
                                            "isLeaf": True,
                                            'navnodes': Absent(),
                                        }
                                    ],
                                },
                                {
                                    "id": 7200,
                                    "isLeaf": True,
                                    'navnodes': Absent(),
                                },
                                {
                                    "id": 7300,
                                    "isLeaf": True,
                                    'navnodes': Absent(),
                                },
                                {
                                    "id": 7400,
                                    "isLeaf": True,
                                    "navnodes": Absent(),
                                },
                                {
                                    "id": 7500,
                                    "isLeaf": True,
                                    "navnodes": Absent(),
                                },
                                {
                                    "id": 7600,
                                    "isLeaf": True,
                                    "navnodes": Absent(),
                                },
                                {
                                    "id": 7700,
                                    "isLeaf": True,
                                    "navnodes": Absent(),
                                }
                            ],
                        },
                        {
                            "id": 8002,
                            # "isLeaf": True,
                            # 'navnodes': Absent(),
                        },
                        {
                            "id": 8012,
                            "isLeaf": True,
                            'navnodes': Absent(),
                        },
                        {
                            "id": 9000,
                            # "navnodes": Absent(),
                        },
                        {
                            "id": 70000,
                            "isLeaf": False,
                            "navnodes": [
                                {
                                    "id": 77681,
                                    "isLeaf": False,
                                    "navnodes": [
                                        {
                                            "id": 78001,
                                            "isLeaf": False,
                                            "navnodes": [
                                                {
                                                    "id": 78011,
                                                    "isLeaf": True,
                                                    'navnodes': Absent(),
                                                },
                                                {
                                                    "id": 78012,
                                                    "isLeaf": True,
                                                    'navnodes': Absent(),
                                                }
                                            ],
                                        },
                                        {
                                            "id": 78002,
                                            "isLeaf": True,
                                            'navnodes': Absent(),
                                        },
                                        {
                                            "id": 78003,
                                            "isLeaf": True,
                                            'navnodes': Absent(),
                                        }
                                    ],
                                },
                                {
                                    "id": 77000,
                                    "isLeaf": False,
                                    "navnodes": [
                                        {
                                            "id": 79001,
                                            "isLeaf": True,
                                            'navnodes': Absent(),
                                        },
                                        {
                                            "id": 79002,
                                            # "isLeaf": True,
                                            # 'navnodes': Absent(),
                                        }
                                    ],
                                }
                            ],
                        },
                        {
                            "id": 50000,
                            "isLeaf": False,
                            "navnodes": [
                                {
                                    "id": 51000,
                                    "isLeaf": True,
                                    'navnodes': Absent(),
                                },
                                {
                                    "id": 52000,
                                    "isLeaf": False,
                                    "navnodes": [
                                        {
                                            "id": 52100,
                                            "isLeaf": False,
                                            "navnodes": [
                                                {
                                                    "id": 52110,
                                                    "isLeaf": True,
                                                    'navnodes': Absent(),
                                                },
                                                {
                                                    "id": 52120,
                                                    "isLeaf": True,
                                                    'navnodes': Absent(),
                                                }
                                            ],
                                        },
                                        {
                                            "id": 52200,
                                            "isLeaf": True,
                                            'navnodes': Absent(),
                                        }
                                    ],
                                }
                            ],
                        },
                        DISCOUNT_DEPARTMENT,
                    ]
                }
            ]
        ),
        allow_different_len=False)

        for nid in (2200, 2220, 3100, 3200, 3120,):
            self.assertFragmentNotIn(response, {
                'id': nid,
            })

    def test_hidden_categories(self):
        # Узел 5100 скрыт на десктопе, 5210 в мобильной версии, 5300 в приложении.
        # По-умолчанию проверятся скрытость в десктопе
        desktop_response = self.cataloger.request_json('GetNavigationTree?depth=5&format=json')
        self.assertFragmentNotIn(desktop_response, {'id': 5100})
        # 5110 не скрыт, но он потомок скрытого 5100
        self.assertFragmentNotIn(desktop_response, {'id': 5110})
        # 5210 и 5300 в десктопе не скрыты
        self.assertFragmentIn(desktop_response, {'id': 5210})
        self.assertFragmentIn(desktop_response, {'id': 5300})

        touch_response = self.cataloger.request_json('GetNavigationTree?depth=5&format=json&device=mobile')
        self.assertFragmentNotIn(touch_response, {'id': 5210})
        self.assertFragmentIn(touch_response, [
            {
                'id': 5100,  # скрытая в десктопе тут нормально показывается вместе с потомком
                'isLeaf': False,
                'navnodes': [
                    {
                        'id': 5110,
                        'isLeaf': True,
                    }
                ]
            },
            {
                'id': 5200,
                'isLeaf': True,  # есть 5210, но из-за сокрытия 5200 становится листом
                'navnodes': Absent(),
            },
        ])
        self.assertFragmentIn(touch_response, {'id': 5300})

        app_response = self.cataloger.request_json('GetNavigationTree?depth=5&format=json&device=app')
        self.assertFragmentNotIn(app_response, {'id': 5300})
        self.assertFragmentIn(app_response, {'id': 5100})
        self.assertFragmentIn(app_response, {'id': 5200})

        # c show_hidden показываются все на любом устройстве
        for device in ('', 'touch', 'app',):
            request = 'GetNavigationTree?depth=5&format=json&device={}&show_hidden=1'.format(device)
            response = self.cataloger.request_json(request)
            self.assertFragmentIn(
                response,
                [
                    {
                        'id': 5100,
                        'isLeaf': False,
                        'navnodes': [
                            {
                                'id': 5110,
                                'isLeaf': True,
                            }
                        ]
                    },
                    {
                        'id': 5200,
                        'isLeaf': False,
                        'navnodes': [
                            {
                                'id': 5210,
                                'isLeaf': True,
                            }
                        ]
                    },
                    {
                        'id': 5300,
                        'isLeaf': True
                    }
                ]
            )

    def test_all_trees(self):
        response = self.cataloger.request_xml('GetAllNavigationTrees')
        self.assertFragmentIn(response, '''
<trees>
  <navigation-tree code="b2b" tree_id="89000">
    <navnode hid="100" id="10000"/>
  </navigation-tree>
  <navigation-tree code="blue" tree_id="80369">
    <navnode hid="100" id="1000">
      <navnode hid="200" id="2000">
        <navnode hid="210" id="2100">
          <navnode hid="211" id="2110"/>
        </navnode>
        <navnode hid="220" id="2200">
          <navnode hid="221" id="2210"/>
          <navnode hid="222" id="2220"/>
        </navnode>
      </navnode>
    </navnode>
  </navigation-tree>
  <navigation-tree code="green" tree_id="57964">
    <navnode hid="100" id="1000">
      <navnode hid="200" id="2000">
        <navnode hid="210" id="2100">
          <navnode hid="211" id="2110"/>
        </navnode>
        <navnode hid="220" id="2200">
          <navnode hid="221" id="2210"/>
          <navnode hid="222" id="2220"/>
        </navnode>
      </navnode>
      <navnode hid="300" id="3000">
        <navnode hid="310" id="3100">
          <navnode hid="311" id="3110"/>
          <navnode hid="312" id="3120"/>
        </navnode>
        <navnode hid="320" id="3200"/>
      </navnode>
      <navnode hid="0" id="4000">
        <navnode hid="311" id="4100"/>
      </navnode>
      <navnode hid="0" id="5000">
        <navnode hid="510" id="5100">
          <navnode hid="511" id="5110"/>
        </navnode>
        <navnode hid="520" id="5200">
          <navnode hid="521" id="5210"/>
        </navnode>
        <navnode hid="530" id="5300"/>
      </navnode>
      <navnode hid="0" id="6000">
        <navnode hid="610" id="6100"/>
        <navnode hid="6091783" id="6200">
          <navnode hid="621" id="6210">
            <navnode hid="1621" id="6211"/>
            <navnode hid="1622" id="6212"/>
          </navnode>
        </navnode>
      </navnode>
      <navnode hid="0" id="7000">
        <navnode hid="200" id="7100">
          <navnode hid="211" id="7110"/>
        </navnode>
        <navnode hid="300" id="7200"/>
        <navnode hid="311" id="7300"/>
        <navnode hid="740" id="7400"/>
        <navnode hid="740" id="7500"/>
        <navnode hid="740" id="7600"/>
        <navnode hid="770" id="7700"/>
      </navnode>
      <navnode hid="902" id="8002"/>
      <navnode hid="912" id="8012"/>
      <navnode hid="922" id="8111"/>
      <navnode hid="950" id="9000">
        <navnode hid="960" id="9100">
          <navnode hid="961" id="9110"/>
          <navnode hid="962" id="9120"/>
          <navnode hid="961" id="9110"/>
          <navnode hid="962" id="9120"/>
        </navnode>
        <navnode hid="960" id="9100">
          <navnode hid="961" id="9110"/>
          <navnode hid="962" id="9120"/>
          <navnode hid="961" id="9110"/>
          <navnode hid="962" id="9120"/>
        </navnode>
        <navnode hid="970" id="9200"/>
      </navnode>
      <navnode hid="0" id="70000">
        <navnode hid="0" id="77681">
          <navnode hid="0" id="78001">
            <navnode hid="0" id="78011"/>
            <navnode hid="0" id="78012"/>
          </navnode>
          <navnode hid="0" id="78002"/>
          <navnode hid="0" id="78003"/>
        </navnode>
        <navnode hid="0" id="77000">
          <navnode hid="0" id="79001"/>
          <navnode hid="0" id="79002"/>
        </navnode>
      </navnode>
      <navnode hid="0" id="50000">
        <navnode hid="610" id="51000"/>
        <navnode hid="6091783" id="52000">
          <navnode hid="621" id="52100">
            <navnode hid="1621" id="52110"/>
            <navnode hid="1622" id="52120"/>
          </navnode>
          <navnode hid="610" id="52200"/>
        </navnode>
      </navnode>
      <navnode hid="100" id="61522">
        <navnode hid="211" id="60000"/>
      </navnode>
    </navnode>
  </navigation-tree>
</trees>
''')

    def test_green_empty_regions(self):
        # Офферы есть только в Москве и Питере
        for region in (146, 143, 187,):  # Симферополь, Киев, вся Украина
            response = self.cataloger.request_json('GetNavigationTree?depth=1&show_empty=0&format=json&region={}'.format(region))
            self.assertFragmentIn(response, self.wrap_up_in_result([]))

    def test_b2b_empty_regions(self):
        # Офферы есть только в Москве и Питере
        for region in (146, 143, 187,):  # Симферополь, Киев, вся Украина
            response = self.cataloger.request_json('GetNavigationTree?depth=1&show_empty=0&format=json&region={}&rgb=black'.format(region))
            self.assertFragmentIn(response, self.wrap_up_in_result([]))

    def test_green_not_empty_regions(self):
        # Офферы есть только в Москве и Питере в категориях 211, 221, 311, 770.
        # В родительских регионах их офферы суммируются.
        offers_count = dict()
        for hid in (211, 221, 311, 770):
            offers_count[hid] = dict()
            for region in (2, 213):
                offers_count[hid][region] = self.offers_in_hid(hid, region)

            offers_count[hid][10174] = offers_count[hid][2]  # Питер + Ленинградская область
            offers_count[hid][1] = offers_count[hid][213]  # Москва + МО
            offers_count[hid][225] = offers_count[hid][2] + offers_count[hid][213]  # Россия
            offers_count[hid][-1] = offers_count[hid][2] + offers_count[hid][213]

        for region in (2, 213, -1, 10174, 1, 225):
            region_param = '&region={}'.format(region) if region != -1 else ''
            response = self.cataloger.request_json('GetNavigationTree?depth=3&show_empty=0&format=json{}'.format(region_param))
            self.assertFragmentIn(response, {
                'id': 1000,
                'navnodes': [
                    {
                        'id': 2000,
                        'category': {
                            'id': 200,
                            'offersCount': offers_count[211][region] + offers_count[221][region]
                        },
                        'navnodes': [
                            {
                                'id': 2100,
                                'category': {
                                    'id': 210,
                                    'offersCount': offers_count[211][region],
                                },
                                'navnodes': [
                                    {
                                        'category': {
                                            'id': 211,
                                            'offersCount': offers_count[211][region],
                                        },
                                        'id': 2110,
                                    }
                                ]
                            },
                            {
                                'id': 2200,
                                'category': {
                                    'id': 220,
                                    'offersCount': offers_count[221][region],
                                },
                                'navnodes': [
                                    {
                                        'category': {
                                            'id': 221,
                                            'offersCount': offers_count[221][region],
                                        },
                                        'id': 2210,
                                    }
                                ]
                            }
                        ],
                    },
                    {
                        'id': 3000,
                        'category': {
                            'id': 300,
                            'offersCount': offers_count[311][region]
                        },
                        'navnodes': [
                            {
                                'id': 3100,
                                'category': {
                                    'id': 310,
                                    'offersCount': offers_count[311][region]
                                },
                                'navnodes': [
                                    {
                                        'id': 3110,
                                        'category': {
                                            'id': 311,
                                            'offersCount': offers_count[311][region]
                                        }
                                    }
                                ]
                            },
                        ]
                    },
                    {
                        'id': 4000,
                        'category': Absent(),
                        'navnodes': [
                            {
                                'id': 4100,
                                'category': {
                                    'id': 311,
                                    'offersCount': offers_count[311][region]
                                }
                            },
                        ]
                    },
                    {
                        'id': 7000,
                        'category': Absent(),
                        'navnodes': [
                            {
                                'id': 7100,
                                'category': {
                                    'id': 200,
                                    # в навигационном дереве нет подузла с хидом 221, но суммируются по товарному дереву
                                    'offersCount': offers_count[211][region] + offers_count[221][region],
                                },
                                'navnodes': [
                                    {
                                        'id': 7110,
                                        'category': {
                                            'id': 211,
                                            'offersCount': offers_count[211][region],
                                        }
                                    },
                                ]
                            },
                            {
                                'id': 7200,  # ведет на категорию, у которой есть подузел с хидом 311
                                'category': {
                                    'id': 300,
                                    'offersCount': offers_count[311][region],
                                },
                                'navnodes': Absent(),
                            },
                            {
                                'id': 7300,
                                'category': {
                                    'id': 311,
                                    'offersCount': offers_count[311][region],
                                }
                            },
                            {
                                'id': 7700,
                                'category': {
                                    'id': 770,
                                    'offersCount': offers_count[770][region],
                                }
                            },
                        ]
                    },
                    {
                        'id': 61522,
                        'name': 'Скидочный департамент',  # TODO заполнить другие поля
                    }
                ]
            }, allow_different_len=False)

            for empty_nid in (2220, 3200, 3120, 5000, 5100, 5200, 5300, 5110, 5210):
                self.assertFragmentNotIn(response, {
                    'id': empty_nid,
                })

    def test_blue_not_empty_regions(self):
        # В синей статистике офферы есть только в Симферополе и одном селе
        # в категориях 211, 311, 312. В России их офферы просуммируются
        offers_count = dict()
        for hid in (211, 311, 312):
            offers_count[hid] = dict()
            for region in (146, 135312):
                offers_count[hid][region] = self.offers_in_hid(hid, region)

            offers_count[hid][225] = offers_count[hid][146] + offers_count[hid][135312]  # Россия
            offers_count[hid][-1] = offers_count[hid][225]

        for region in (146, 135312, -1, 225):
            offers_211 = offers_count[211].get(region, 0)
            offers_311 = offers_count[311].get(region, 0)
            offers_312 = offers_count[312].get(region, 0)
            region_param = '&region={}'.format(region) if region != -1 else ''

            # В дереве нет синего узла для хида 312, поэтому его нет и на выдаче.
            # Но офферы суммируются по товарному дереву, которое общее для всех. Поэтому
            # офферы из узла 312 пролезают в 310, 300, 100
            response_main_tree = self.cataloger.request_json('GetNavigationTree?depth=3&show_empty=0&format=json&rgb=blue{}'.format(region_param))
            self.assertFragmentNotIn(response_main_tree, {'category': {'id': 312}})
            self.assertFragmentIn(response_main_tree, {
                'id': 1000,
                'category': {
                    'id': 100,
                    'offersCount': offers_211 + offers_311 + offers_312
                },
                'navnodes': [
                    {
                        'id': 2000,
                        'category': {
                            'id': 200,
                            'offersCount': offers_211
                        },
                        'navnodes': [
                            {
                                'id': 2100,
                                'category': {
                                    'id': 210,
                                    'offersCount': offers_211
                                },
                                'navnodes': [
                                    {
                                        'id': 2110,
                                        'category': {
                                            'id': 211,
                                            'offersCount': offers_211
                                        },
                                        'navnodes': Absent()
                                    }
                                ]
                            }
                        ]
                    },
                    {
                        'id': 3000,
                        'category': {
                            'id': 300,
                            'offersCount': offers_311 + offers_312
                        },
                        'navnodes': [
                            {
                                'id': 3110,  # 3100 пропущен из-за is_blue=False
                                'category': {
                                    'id': 311,
                                    'offersCount': offers_311
                                },
                                'navnodes': Absent()
                            }
                        ]
                    }
                ]
            })

    def test_adult_categories_green(self):
        # без adult=0
        response = self.cataloger.request_json('GetNavigationTree?nid=6000&depth=3&format=json')
        self.assertFragmentIn(response, {
            'id': 6000,
            'name': 'Аптека',
            'navnodes': [
                {
                    'id': 6100,
                    'category': {
                        'id': 610
                    },
                    'name': 'Презервативы',
                    'isLeaf': True,
                    'adult': Absent()
                },
                {
                    'id': 6200,
                    'category': {
                        'id': 6091783
                    },
                    'name': 'Товары для взрослых',
                    'navnodes': [
                        {
                            'id': 6210,
                            'category': {
                                'id': 621
                            },
                            'name': 'Эротическая одежда',
                            'adult': True,
                            'navnodes': [
                                {
                                    'id': 6211,
                                    'category': {
                                        'id': 1621
                                    },
                                    'name': 'Эротические костюмы',
                                    'adult': True
                                },
                                {
                                    'id': 6212,
                                    'category': {
                                        'id': 1622
                                    },
                                    'name': 'Корсеты',
                                    'adult': True
                                },
                            ]
                        }
                    ]
                }
            ]
        })

        # с adult=0
        response = self.cataloger.request_json('GetNavigationTree?nid=6000&depth=3&adult=0&format=json')
        self.assertFragmentIn(response, {
            'id': 6000,
            'navnodes': [
                {
                    'id': 6100,
                },
            ]
        })

        for nid in (6200, 6210, 6211, 6212):
            self.assertFragmentNotIn(response, {'id': nid})
        for hid in (6091783,):
            self.assertFragmentNotIn(response, {'id': hid})

    def test_categories_blue_with_adult(self):
        # Со взрослыми категориями (без adult=0)
        response = self.cataloger.request_json('GetNavigationTree?nid=50000&depth=3&rgb=blue&format=json')
        self.assertFragmentIn(response, {
            'id': 50000,
            'navnodes': [
                {
                    'id': 51000,
                    'category': {
                        'id': 610
                    },
                    'isLeaf': True,
                    'adult': Absent(),
                    'name': 'Презервативы синие аптечные'
                },
                {
                    'id': 52000,
                    'category': {
                        'id': 6091783
                    },
                    'isLeaf': False,
                    'name': 'Товары для взрослых синие',
                    'navnodes': [
                        {
                            'id': 52100,
                            'category': {
                                'id': 621
                            },
                            'name': 'Эротические одежда синяя',
                            'isLeaf': False,
                            'adult': True,
                            'navnodes': [
                                {
                                    'id': 52110,
                                    'category': {
                                        'id': 1621
                                    },
                                    'isLeaf': True,
                                    'adult': True,
                                    'name': 'Эротические костюмы синие'
                                },
                                {
                                    'id': 52120,
                                    'category': {
                                        'id': 1622
                                    },
                                    'isLeaf': True,
                                    'adult': True,
                                    'name': 'Корсеты синие'
                                }
                            ]
                        },
                        {
                            'id': 52200,
                            'category': {
                                'id': 610
                            },
                            'name': 'Презервативы синие взрослые',
                            'isLeaf': True,
                            'adult': Absent(),
                        }
                    ]
                }
            ]
        })

    def test_categories_blue_wo_adult(self):
        # Без взрослых категорий (с adult=0)
        response = self.cataloger.request_json('GetNavigationTree?nid=50000&depth=3&rgb=blue&adult=0&format=json')
        self.assertFragmentIn(response, {
            'id': 50000,
            'navnodes': [
                {
                    'id': 51000,
                },
            ]
        })

        # Презервативы с хид==610  в синей апетеке встречались два раза.
        # Как ребенок "аптеки" они показываются, а как подкатегория "взрослых товаров" нет
        for nid in (52000, 52100, 52110, 52120, 52200,):
            self.assertFragmentNotIn(response, {'id': nid})
        for hid in (6091783,):
            self.assertFragmentNotIn(response, {'id': hid})

        # Если нид в самом запросе будет взрослый, то он покажется,
        # но его потомки уже будут фильтроваться
        response = self.cataloger.request_json('GetNavigationTree?nid=52000&depth=2&rgb=blue&adult=0&format=json')
        self.assertFragmentIn(response, {
            'id': 52000,
            'category': {
                'id': 6091783
            },
            'navnodes': [
                {
                    'id': 52200  # презервативы в ТД не взрослая категория
                }
            ]
        })

        for nid in (52100, 52110, 52120):
            self.assertFragmentNotIn(response, {'id': nid})

    def test_links(self):
        response = self.cataloger.request_json('GetNavigationTree?nid=7000&depth=3&format=json')
        self.assertFragmentIn(response, {
            'id': 7000,
            'navnodes': [
                {
                    'category': {
                        'id': 200,
                        'nid': 7100,
                    },
                    'id': 7100,
                    # имена берутся из самого узла, а не узла, на который ведет ссылка
                    'name': 'Ссылка на электронику',
                    'fullName': 'unique name of category 7100',
                    'slug': 'unique-name-of-category-7100',
                    'type': 'link',
                    'link': {
                        'params': {
                            'hid': ['200'],
                            'nid': ['2000']
                        },
                        'target': 'catalog'
                    },
                    'isLeaf': False,
                    'navnodes': [
                        {
                            'category': {
                                'id': 211,
                                'nid': 7110,
                            },
                            'id': 7110,
                            'link': {
                                'params': {
                                    'hid': ['211'],
                                    'with-discount': [''],
                                    'home_region': ['213']
                                },
                                'target': 'catalogleaf'
                            },
                            'type': 'link',
                            'isLeaf': True
                        }
                    ]
                },
                {
                    # если у узла хид == 0, то он берется из параметров ссылки
                    'category': {
                        'id': 300,
                        'nid': 7200,
                    },
                    'id': 7200,
                    'link': {
                        'params': {
                            'hid': ['300'],
                            'nid': ['3000']
                        },
                        'target': 'catalog'
                    },
                    'type': 'link',
                    'isLeaf': True,  # берется из самого узла, а не ссылки
                },
                # узел-рецепт
                {
                    'category':
                    {
                        'id': 311,
                        'nid': 7300,
                    },
                    'type': 'gl_recipe',
                    'id': 7300,
                    'isLeaf': True,
                    'link':
                    {
                        'params':
                        {
                            'glfilter': [
                                '100500:1,2,3',
                                '100501:1',
                                '100502:0',
                                '100503:10~',
                                '100504:~100',
                                '100505:20~50'
                            ],
                            'hid': ['311'],
                            'nid': ['7300']
                        },
                        'target': 'search'
                    },
                }
            ]
        })

    def _gen_path_json_rec(self, ids, index):
        if index == len(ids):
            return Absent()
        if isinstance(ids[index], int):
            return [{'id': ids[index], 'navnodes': self._gen_path_json_rec(ids, index + 1)}]
        nid, hid = ids[index]
        return [{
            'id': nid,
            'category': {'id': hid},
            'navnodes': self._gen_path_json_rec(ids, index + 1)
        }]

    def _gen_path_json(self, ids):
        return self._gen_path_json_rec([1000] + ids, 0)[0]

    def _path_2110(self):
        return self._gen_path_json([2000, 2100, (2110, 211)])

    def _path_2210(self):
        return self._gen_path_json([2000, 2200, (2210, 221)])

    def _path_2220(self):
        return self._gen_path_json([2000, 2200, (2220, 222)])

    def _path_3110(self):
        return self._gen_path_json([3000, 3100, (3110, 311)])

    def _path_3120(self):
        return self._gen_path_json([3000, 3100, (3120, 312)])

    def _path_4100(self):
        return self._gen_path_json([4000, (4100, 311)])

    def _path_7110(self):
        return self._gen_path_json([7000, 7100, (7110, 211)])

    def _path_740(self, nid):
        return self._gen_path_json([7000, (nid, 740)])

    def test_shop_trees(self):
        # в Москве у магазина 500 есть хиды 211 и 311, в Питере 211 и 222
        for region in (213, 2, 10000):  # Москва, Питер, весь мир
            response = self.cataloger.request_json('GetShopTree?fesh=500&format=json&depth=10&region={}'.format(region))
            self.assertFragmentIn(response, self._path_2110())
            if region == 213:
                self.assertFragmentNotIn(response, {'category': {'id': 222}})
            else:
                self.assertFragmentIn(response, self._path_2220())
            if region == 2:
                self.assertFragmentNotIn(response, {'category': {'id': 311}})
            else:
                self.assertFragmentIn(response, self._path_3110())
                self.assertFragmentIn(response, self._path_4100())
                self.assertFragmentIn(response, self._path_7110())
                # 7300 не показывается, т.к это категория-рецепт, а они показываются только
                # если нет полноценной категории с таким хидом
                self.assertFragmentNotIn(response, {'id': 7300})
                # а вот если у хида нет ни одной полноценной категории, то показываются все рецепты
                self.assertFragmentIn(response, self._path_740(7400))
                self.assertFragmentIn(response, self._path_740(7500))
            # Скидочный департамент никогда не попадает в выдачу, хотя в нем есть хид 211
            self.assertFragmentNotIn(response, {'id': 61522})

    def test_business_trees_for_shops(self):
        # У бизнес партнера 100500 три мгазина: 500, 600, 650.
        # Магазин 650 отсутствует в СРА статистике, при вызове с business_id его категории не должны показываться
        # В Москве у магазина 500 есть хиды 211 и 311, а у магазина 600 хиды 221 и 312
        # Параметр business_id приоритетнее fesh. Последний просто игнорится, связанность параметров не проверяется
        # 1) магазин 500 принадлежит партнеру 100500, но магазин 600 всеравно останется в выдаче
        # 2) магазин 900 принадлежит другому партнеру, и он есть в статистике, но запрос нормально отработает для партнера 100500
        for fesh in ('', '&fesh=500', '&fesh=900'):
            response = self.cataloger.request_json('GetShopTree?business_id=100500&format=json&depth=10&region=213{}'.format(fesh))
            self.assertFragmentIn(response, self._path_2110())
            self.assertFragmentIn(response, self._path_3110())
            self.assertFragmentIn(response, self._path_2210())
            self.assertFragmentIn(response, self._path_3120())
            # У магазина 500 категория 222 есть в Питере, из-за региона она не показывается.
            # У магазина 900 категория 222 есть в Москве, но этот параметр игнорится, категория не показывается
            # У магазина 650 есть категория 222 в Москве, но магазин не СРА, в статистику она не попадает
            self.assertFragmentNotIn(response, {'category': {'id': 222}})

        # При параметре fesh, а не business_id, смотрит в общую статистику, а не СРА
        response = self.cataloger.request_json('GetShopTree?fesh=650&format=json&depth=10&region=213')
        self.assertFragmentIn(response, {'category': {'id': 222}})

        # У партнера 100501 есть магазин 700, но его (магазина) нет в статистике. На выдаче только корень
        empty_tree = [{
            'id': 1000,
            'name': 'Все товары сине-зеленые',
            'navnodes': Absent(),
        }]
        response = self.cataloger.request_json('GetShopTree?business_id=100501&format=json&depth=10&region=213')
        self.assertFragmentIn(response, empty_tree)

        # Для несуществуюего партнера тоже просто пустое дерево
        response = self.cataloger.request_json('GetShopTree?business_id=100502&format=json&depth=10&region=213')
        self.assertFragmentIn(response, empty_tree)

        # У партнера 1005003 есть магазин 800, но он отключен. Дерево будет пустым, даже если магазин попал в статистику
        response = self.cataloger.request_json('GetShopTree?business_id=100503&format=json&depth=10&region=213')
        self.assertFragmentIn(response, empty_tree)

    def test_business_trees_for_suppliers(self):
        # У магазина 900 (партнер 100504) есть категория 222, где он магазин, и категория 221, где он поставщик.
        # В реальных данных такого не встречается, поэтому в коде нет мержа категорий из двух разных статистик.
        # Если есть в статистике магазинов, то категории берем только оттуда
        response = self.cataloger.request_json('GetShopTree?business_id=100504&format=json&depth=10&region=213')
        self.assertFragmentIn(response, self._path_2220())
        self.assertFragmentNotIn(response, {'category': {'id': 221}})

        # У партнера 100505 есть магазины 1000 и 1100. Оба - поставщики, их категории мержатся
        response = self.cataloger.request_json('GetShopTree?business_id=100505&format=json&depth=10&region=213')
        self.assertFragmentIn(response, self._path_2110())
        self.assertFragmentIn(response, self._path_2210())

        response = self.cataloger.request_json('GetShopTree?business_id=1234567890&format=json&depth=10&region=213')
        self.assertFragmentIn(response, self._path_2210())

    def test_common_filters(self):
        # Проверяется наличие поля commonFiltersParentNids в нодах, для которых есть предок со сквозными фильтрами
        response = self.cataloger.request_json('GetFullNavigationTree?nid=70000&format=json')
        nids_with_common_filters_parent = [78001, 78011, 78012, 78002, 78003]
        nids_without_common_filters_parent = [77000, 79001, 79002]
        for nid in nids_with_common_filters_parent:
            self.assertFragmentIn(response, {
                'entity': 'navnode',
                'id': nid,
                'commonFiltersParentNids': ["77681"],
            })
        for nid in nids_without_common_filters_parent:
            self.assertFragmentNotIn(response, {
                'entity': 'navnode',
                'id': nid,
                'commonFiltersParentNids': ["77681"],
            })

    def test_self_redirect(self):
        '''
        Проверяем, что редирект из узлов одного дерева не срабатывает, если оба узла в наличии
        '''
        for nid in [8002, 8012]:
            response = self.cataloger.request_json('GetFullNavigationTree?nid={}&format=json&rgb=blue'.format(nid))
            self.assertFragmentIn(response, {
                'entity': 'navnode',
                'id': nid,
            })

    def test_root_redirect(self):
        '''
        Проверяем правильность редиректа во все товары
        '''
        response = self.cataloger.request_json('getNavigationTree?nid=1000&depth=21&rgb=blue&format=json')
        self.assertFragmentIn(response, {'navnodes': [{
            'entity': 'navnode',
            'id': 1000,
            'name': 'Все товары сине-зеленые'
        }]})

    def test_redirects(self):
        # 100600 - несуществует, но есть редирект на реальную сине-зеленый 8012
        response = self.cataloger.request_json('GetFullNavigationTree?nid=100600&rgb=blue&format=json')
        self.assertFragmentIn(response, {
            'entity': 'navnode',
            'id': 8012,
        })

        # 100601 - несуществует, но есть редирект на зеленый 8111
        response = self.cataloger.request_json('GetFullNavigationTree?nid=100601&rgb=green&format=json')
        self.assertFragmentIn(response, {
            'entity': 'navnode',
            'id': 8111,
        })

        # тк 8111 - зеленый, то при rgb=blue редиректа не будет
        response = self.cataloger.request_json('GetFullNavigationTree?nid=100601&rgb=blue&format=json')
        self.assertFragmentIn(response, {
            'result': {}
        })

    def test_tags(self):
        response = self.cataloger.request_json('GetFullNavigationTree?nid=2000&rgb=blue&format=json')
        self.assertFragmentIn(response, {
            'entity': 'navnode',
            'id': 2100,
            'tags': [
                'tag1',
                'tag2',
            ],
        })

    def test_shop_does_not_contain_adult_categories(self):
        response = self.cataloger.request_json('CheckShopAdultCategories?business_id=100505format=json')
        self.assertFragmentIn(response, {
            '__info__': NotEmpty(),
            'result': {
                '__name__': Absent(),
                'hasAdultCategories': False,
            }
        })

    def test_shop_contains_adult_categories(self):
        response = self.cataloger.request_json('CheckShopAdultCategories?business_id=12000format=json')
        self.assertFragmentIn(response, {
            '__info__': NotEmpty(),
            'result': {
                '__name__': Absent(),
                'hasAdultCategories': True,
            }
        })

    def test_no_update_cs_data(self):
        # Есть ручка UpdateCsData, которая не должна отрабатывать без флага USE_CONTENT_STORAGE_DATA
        # В этом тесте он False
        response = self.cataloger.request_json('UpdateCsData?type=navigation')
        self.assertFragmentIn(response, {
            'result': {
                'log': 'Content-storage data is not used in cataloger (USE_CONTENT_STORAGE_DATA == False)'
            }
        })

if __name__ == '__main__':
    main()
