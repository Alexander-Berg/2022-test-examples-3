#!/usr/bin/env python
# -*- coding: utf-8 -*-

from core.testcase import TestCase, main
from core.types.navigation_info import NavigationNode, NavigationTree
from core.types.navigation_info import NavigationMenu, Departament, DepartamentBlock
from core.types.categories_stats import RegionStats, CategoryStats
from core.matcher import NotEmpty, Absent


class T(TestCase):

    @classmethod
    def prepare(cls):
        green_nodes = [
            NavigationNode(nid=1000, hid=100, parent_nid=0, short_name='Все товары'),

            NavigationNode(nid=2000, hid=200, parent_nid=1000, short_name='Электроника', has_icon=True),
            NavigationNode(nid=2100, hid=210, parent_nid=2000, short_name='Телефоны', has_icon=True),
            NavigationNode(nid=2110, hid=211, parent_nid=2100, short_name='Мобильные телефоны'),
            NavigationNode(nid=2200, hid=220, parent_nid=2000, short_name='Фото', has_icon=True),
            NavigationNode(nid=2210, hid=221, parent_nid=2200, short_name='Аксессуары для фото'),

            NavigationNode(nid=3000, hid=300, parent_nid=1000, short_name='Бытовая техника'),
            NavigationNode(nid=3100, hid=310, parent_nid=3000, short_name='Для кухни', has_icon=True),
            NavigationNode(nid=3110, hid=311, parent_nid=3100, short_name='Плиты', has_icon=True),
            NavigationNode(nid=3120, hid=312, parent_nid=3100, guru_category_id=100500, short_name='Холодильники', has_icon=True),

            NavigationNode(nid=20000, hid=200, parent_nid=1000, short_name='Электроника синяя'),
            NavigationNode(nid=21000, hid=210, parent_nid=20000, short_name='Телефоны синие',),
            NavigationNode(nid=21100, hid=211, parent_nid=21000, short_name='Мобильные телефоны синие'),
            NavigationNode(nid=23000, hid=0, parent_nid=20000, short_name='Подарок на 23 феврал'),
            NavigationNode(nid=23100, hid=0, parent_nid=23000, short_name='Красота и здоровье'),
            NavigationNode(nid=23110, hid=2311, parent_nid=23100, short_name='Электробритвы'),
        ]

        cls.index.navigation_trees += [NavigationTree(code='green', nodes=green_nodes)]

        links_2100 = {2110: 'ссылка на мобильные'}
        links_3100 = {3110: 'ссылка на плиты', 3120: 'ссылка на холодильники'}
        green_departaments = [
            Departament(name='департамент Электроника', nid=2000, has_picture=True,
                        blocks=[
                            DepartamentBlock(nid=2100, name='блок Телефоны', links=links_2100),
                            DepartamentBlock(nid=2200, name='блок Фото')
                        ]),
            Departament(name='департамент Бытовая техника', nid=3000,
                        blocks=[
                            DepartamentBlock(nid=3100, name='блок Для кухни', links=links_3100),
                        ]),
            Departament(name='департамент только с блоком 2100', nid=2100,
                        blocks=[DepartamentBlock(nid=2100)]
                        ),
        ]

        links_21000 = {21100: 'ссылка на синие мобильные'}
        links_23000 = {23100: 'ссылка на все подарки', 23110: 'ссылка на электробритвы'}
        blue_departaments = [
            Departament(name='синий департамент Электроника', nid=20000,
                        blocks=[
                            DepartamentBlock(nid=21000, name='блок синих телефонов', links=links_21000),
                            DepartamentBlock(nid=23000, name='блок подароков на 23 февраля', links=links_23000)
                        ]),
        ]

        cls.index.navigation_menus += [NavigationMenu(name='greenmenu', departaments=green_departaments),
                                       NavigationMenu(name='bluemenu', departaments=blue_departaments)]

        cls.index.categories_stats += [CategoryStats(211, [RegionStats(region=213, offers=1)]),
                                       CategoryStats(311, [RegionStats(region=213, offers=1)])]
        cls.index.blue_categories_stats += [CategoryStats(2311, [RegionStats(region=2, offers=1)])]

    @staticmethod
    def offers_in_hid(hid, region):
        return hid * 1000 + region

    def test_greenmenu(self):
        response = self.cataloger.request_json('GetNavigationMenu?name=greenmenu&format=json')
        self.assertFragmentIn(response, {
            'result': {
                'entity': 'navmenu',
                'domain': 'ru',
                'name': 'greenmenu',
                'navnodes': [
                    {
                        'entity': 'navnode',
                        'type': 'category',
                        'name': 'департамент Электроника',
                        'slug': 'departament-elektronika',  # надо исправлять, слаг должен быть от узла, и не меню
                        'id': 2000,
                        'isLeaf': False,
                        'icon': 'picture_for_departament_to_nid_2000',  # берется из меню
                        'icons': [
                            {
                                'url': 'icon_for_node_2000.png'  # берется из привязанного узла
                            }
                        ],
                        'link': {
                            'params': {
                                'hid': ['200'],
                                'nid': ['2000']
                            },
                            'target': 'department'
                        },
                        'navnodes': [
                            {
                                'entity': 'navnode',
                                'id': 2100,
                                'isLeaf': False,
                                'name': 'блок Телефоны',
                                'slug': 'blok-telefony',
                                'icons': NotEmpty(),
                                'navnodes': [
                                    {
                                        'entity': 'navnode',
                                        'id': 2110,
                                        'isLeaf': True,
                                        'name': 'ссылка на мобильные',
                                        'slug': 'ssylka-na-mobilnye',
                                        'navnodes': Absent(),
                                    }
                                ]
                            },
                            {
                                'entity': 'navnode',
                                'id': 2200,
                                'isLeaf': False,
                                'name': 'блок Фото',
                                'slug': 'blok-foto',
                                'navnodes': Absent(),  # в дереве дети есть, но не в меню
                                'link': {
                                    'params': {
                                        'hid': ['220'],
                                        'nid': ['2200'],
                                    },
                                    'target': 'catalog',
                                },
                            }
                        ]
                    },
                    {
                        'entity': 'navnode',
                        'type': 'category',
                        'isLeaf': False,
                        'name': 'департамент Бытовая техника',
                        'slug': 'departament-bytovaia-tekhnika',  # надо исправлять, слаг должен быть от узла, и не меню
                        'id': 3000,
                        'icon': Absent(),
                        'icons': Absent(),
                        'navnodes': [
                            {
                                'entity': 'navnode',
                                'id': 3100,
                                'isLeaf': False,
                                'name': 'блок Для кухни',
                                'slug': 'blok-dlia-kukhni',  # надо исправлять
                                'icons': [
                                    {
                                        'url': 'icon_for_node_3100.png'
                                    }
                                ],
                                'navnodes': [
                                    {
                                        'entity': 'navnode',
                                        'id': 3110,
                                        'isLeaf': True,
                                        'name': 'ссылка на плиты',
                                        'slug': 'ssylka-na-plity',  # надо исправлять
                                        'link': {
                                            'params': {
                                                'hid': ['311'],
                                                'nid': ['3110']
                                            },
                                            'target': 'catalog',
                                        },
                                    },
                                    {
                                        'entity': 'navnode',
                                        'id': 3120,
                                        'isLeaf': True,
                                        'name': 'ссылка на холодильники',
                                        'slug': 'ssylka-na-kholodilniki',  # надо исправлять
                                        'link': {
                                            'params': {
                                                'hid': ['312'],
                                                'nid': ['3120']
                                            },
                                            'target': 'catalogleaf',  # из-за catId
                                        },
                                    }
                                ]
                            }
                        ]
                    },
                ]
            },
        })

    def test_bluemenu(self):
        response = self.cataloger.request_json('GetNavigationMenu?name=bluemenu&format=json')
        self.assertFragmentIn(response, {
            'result': {
                'entity': 'navmenu',
                'domain': 'ru',
                'name': 'bluemenu',
                'navnodes': [
                    {
                        'id': 20000,
                        'name': 'синий департамент Электроника',
                        'slug': 'sinii-departament-elektronika',
                        'isLeaf': False,
                        'link': {
                            'params': {
                                'nid': ['20000'],
                                'hid': ['200'],
                            },
                            'target': 'department',
                        },
                        'navnodes': [
                            {
                                'id': 21000,
                                'name': 'блок синих телефонов',
                                'slug': 'blok-sinikh-telefonov',
                                'isLeaf': False,
                                'link': {
                                    'params': {
                                        'hid': ['210'],
                                        'nid': ['21000'],
                                    },
                                    'target': 'catalog',
                                },
                                'navnodes': [
                                    {
                                        'id': 21100,
                                        'navnodes': Absent(),
                                    }
                                ]
                            },
                            {
                                'id': 23000,
                                'name': 'блок подароков на 23 февраля',
                                'slug': 'blok-podarokov-na-23-fevralia',
                                'isLeaf': False,
                                'link': {
                                    'params': {
                                        'hid': Absent(),
                                        'nid': ['23000'],
                                    },
                                    'target': 'catalog',
                                },
                                'navnodes': [
                                    {
                                        'id': 23100,
                                        'name': 'ссылка на все подарки',
                                        'slug': 'ssylka-na-vse-podarki',
                                        'isLeaf': False,
                                        'link': {
                                            'params': {
                                                'hid': Absent(),
                                                'nid': ['23100'],
                                            },
                                            'target': 'catalog',
                                        },
                                        'navnodes': Absent(),
                                    },
                                    {
                                        'id': 23110,
                                        'name': 'ссылка на электробритвы',
                                        'slug': 'ssylka-na-elektrobritvy',
                                        'isLeaf': True,
                                        'link': {
                                            'params': {
                                                'hid': ['2311'],
                                                'nid': ['23110'],
                                            },
                                            'target': 'catalog',
                                        },
                                        'navnodes': Absent(),
                                    }
                                ]
                            }
                        ]
                    }
                ]
            }
        })

    def test_green_not_empty_regions(self):
        # Офферы есть в категориях 211 и 311 в Москве. Для меню проверяется наличие
        # офферов в регионе -1, значит при наличии в Москве меню будет и для всех
        # других регионов

        response = self.cataloger.request_json('GetNavigationMenu?name=greenmenu&format=json&show_empty=0')
        self.assertFragmentIn(response, [
            {
                'id': 2000,
                'category': {
                    'id': 200
                },
                'navnodes': [
                    {
                        'id': 2100,
                        'category': {
                            'id': 210
                        },
                        'navnodes': [
                            {
                                'id': 2110,
                                'category': {
                                    'id': 211
                                }
                            }
                        ]
                    }
                ]

            },
            {
                'id': 3000,
                'category': {
                    'id': 300,
                },
                'navnodes': [
                    {
                        'id': 3100,
                        'category': {
                            'id': 310
                        },
                        'navnodes': [
                            {
                                'id': 3110,
                                'category': {
                                    'id': 311
                                },
                                'navnodes': Absent()
                            }
                        ]
                    }
                ]
            },
            {
                'id': 2100,
                'category': {
                    'id': 210
                },
                'navnodes': [
                    {
                        'id': 2100,
                        'category': {
                            'id': 210
                        },
                        'navnodes': Absent()  # узла 2110 в департаменте нет,
                                              # но 2100 попал в выдачу благодаря
                                              # офферам в 2110
                    }
                ]
            }
        ])

        for empty_nid in (2200, 3120):
            self.assertFragmentNotIn(response, {'id': empty_nid})

    def test_blue_not_empty_regions(self):
        # Офферы есть в категории 2311 в Питере. Для меню проверяется наличие
        # офферов в регионе -1, значит при наличии в Питере меню будет и для всех
        # других регионов

        response = self.cataloger.request_json('GetNavigationMenu?name=bluemenu&format=json&show_empty=0&rgb=blue')
        self.assertFragmentIn(response, [
            {
                'id': 20000,
                'category': {
                    'id': 200
                },
                'navnodes': [
                    {
                        'id': 23000,
                        'category': Absent(),  # виртуальная
                        'navnodes': [
                            {
                                'id': 23100,
                                'category': Absent(),  # виртуальная
                                'navnodes': Absent()  # нижний уровень меню, подкатегорий нет,
                                                      # но в дереве есть подкатегория 2311 с офферами
                            },
                            {
                                'id': 23110,
                                'category': {
                                    'id': 2311,  # офферы отсюда
                                },
                                'navnodes': Absent()
                            },
                        ]
                    }
                ]
            },
        ])

        for empty_nid in (21000,):
            self.assertFragmentNotIn(response, {'id': empty_nid})


if __name__ == '__main__':
    main()
