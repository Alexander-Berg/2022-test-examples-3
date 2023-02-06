#!/usr/bin/env python
# -*- coding: utf-8 -*-

import socket

from core.testcase import TestCase, main
from core.types.navigation_info import NavigationNode, NavigationTree
from core.types.navigation_redirects import TreeRedirects
from core.types.categories_stats import RegionStats, CategoryStats
from core.matcher import NotEmpty, Absent


class T(TestCase):

    @classmethod
    def prepare(cls):
        green_nodes = [
            NavigationNode(nid=1000, hid=100, parent_nid=0, children_type='mixed'),
            NavigationNode(nid=2300, hid=230, parent_nid=1000,
                           short_name='Категория', unique_name='Категория 2300'),
            NavigationNode(nid=2700, hid=270, parent_nid=1000, children_type='aaa'),
            NavigationNode(nid=2000, hid=200, parent_nid=2300),
            NavigationNode(nid=2050, hid=205, parent_nid=2300),
            NavigationNode(nid=2400, hid=240, parent_nid=2300,
                           guru_category_id=4800, has_icon=True),
            NavigationNode(nid=2500, hid=250, parent_nid=2700),
            NavigationNode(nid=2900, hid=290, parent_nid=2700),
            NavigationNode(nid=2100, hid=210, parent_nid=2000),
            NavigationNode(nid=2200, hid=215, parent_nid=2000, tags=['tag1', 'tag2']),

            NavigationNode(nid=3000, hid=0, parent_nid=2900),  # виртуальный
            NavigationNode(nid=3100, hid=310, parent_nid=3000, is_primary=False),
            # с тем же хидом, но с is_primary=True
            NavigationNode(nid=3200, hid=310, parent_nid=3000, is_primary=True),
        ]
        b2b_nodes = [
            NavigationNode(nid=10000, hid=100, parent_nid=0),
            NavigationNode(nid=23000, hid=230, parent_nid=10000,
                            short_name='B2B Категория', unique_name='B2B Категория 23000'),
            NavigationNode(nid=27000, hid=270, parent_nid=10000),
            NavigationNode(nid=20000, hid=200, parent_nid=23000)
        ]

        cls.index.navigation_trees += [NavigationTree(code='green', nodes=green_nodes),
                                        NavigationTree(code='b2b', nodes=b2b_nodes)]

        # 100500 и 100501 не существуют
        green_to_green_redir = {100500: 2100, 2100: 100501}  # редиректы с удаленных категорий

        cls.index.navigation_redirects += [TreeRedirects('green', green_to_green_redir)]

        category_200_green_offers = [
            RegionStats(region=2, offers=cls.offers_in_hid(200, 2)),
            RegionStats(region=213, offers=cls.offers_in_hid(200, 213)),
        ]
        category_240_green_offers = [
            RegionStats(region=143, offers=cls.offers_in_hid(240, 143)),
            RegionStats(region=213, offers=cls.offers_in_hid(240, 213)),
            RegionStats(region=214, offers=0),  # Добавляем регион с нулем офферов
            # region=215 - регион без настроенной конфигурации, по нему проверяется работа плейса GetAvailable
            # для региона, не указанного в конфигурационном файле.
        ]
        cls.index.categories_stats += [
            CategoryStats(200, category_200_green_offers),
            CategoryStats(240, category_240_green_offers),
        ]

        category_205_blue_offers = [RegionStats(region=213, offers=100500)]
        cls.index.blue_categories_stats += [CategoryStats(205, category_205_blue_offers)]

    @staticmethod
    def offers_in_hid(hid, region):
        return hid * 1000 + region

    def test_baseinfo(self):
        # В выдаче любого метода, кроме ping, должны быть блоки __info__ и result
        output_info = {
            'servant': 'marketcataloger',
            'hostname': socket.gethostname(),
            'version': NotEmpty(),
        }
        for method in (
            'getnavigationnode',
            'getpath',
            'getnavigationpath',
            'getnavigationtree',
            'gettree',
            'getcatalogerstat',
            'getmarketavailability',
        ):
            response = self.cataloger.request_json('{}?hid=230&nid=2300&format=json'.format(method))
            self.assertFragmentIn(response, {'__info__': output_info, 'result': NotEmpty()})

    def test_ids_output(self):
        for hid in (100, 230, 270, 200, 240, 250, 290, 210):
            nid = hid * 10
            if nid in (2300, 2700):
                target = 'department'
            elif nid == 2400:
                target = 'catalogleaf'
            else:
                target = 'catalog'
            if nid == 1000:
                root_navnode = Absent()
            elif nid <= 2400:
                root_navnode = {'id': 2300}
            else:
                root_navnode = {'id': 2700}
            response = self.cataloger.request_json('GetNavigationNode?nid={}&format=json'.format(nid))
            if nid == 2300:
                name = 'Категория'
                fullName = 'Категория 2300'
                slug = 'kategoriia-2300'
            else:
                name = 'short name of category {}'.format(nid)
                fullName = 'unique name of category {}'.format(nid)
                slug = 'unique-name-of-category-{}'.format(nid)
            if nid == 2400:
                icons = [{'entity': 'picture', 'url': 'icon_for_node_2400.png'}]
            else:
                icons = Absent()
            self.assertFragmentIn(response, {
                'result': {
                    'entity': 'navnode',
                    'category': {
                        'entity': 'category',
                        'id': hid,
                        'nid': nid,
                        'name': 'tovar category {}'.format(hid)
                    },
                    'id': nid,
                    'fullName': fullName,
                    'name': name,
                    'slug': slug,
                    'icons': icons,
                    'link': {
                        'params': {
                            'hid': [str(hid)],
                            'nid': [str(nid)],
                        },
                        'target': target
                    },
                    'rootNavnode': root_navnode,
                },
            })

    def test_nodes_types(self):
        # корень
        response = self.cataloger.request_json('GetNavigationNode?nid=1000&format=json')
        self.assertFragmentIn(response, {
            'result': {
                'category': {
                    'id': 100,
                },
                'id': 1000,
                'type': 'category',
                'childrenType': 'mixed',
                'isLeaf': False,
                'link': {
                    'target': 'catalog'
                }
            },
        })

        # департамент. childrenType просто прокидывается из выгрузки мбо
        response = self.cataloger.request_json('GetNavigationNode?nid=2700&format=json')
        self.assertFragmentIn(response, {
            'result': {
                'category': {
                    'id': 270,
                },
                'id': 2700,
                'type': 'category',
                'childrenType': 'aaa',
                'isLeaf': False,
                'link': {
                    'target': 'department'
                }
            },
        })

        # виртуальная, childrenType не известен
        response = self.cataloger.request_json('GetNavigationNode?nid=3000&format=json')
        self.assertFragmentIn(response, {
            'result': {
                'category': Absent(),
                'id': 3000,
                'type': 'virtual',
                'childrenType': Absent(),
                'isLeaf': False,
                'link': {
                    'params': {
                        'hid': Absent(),
                        'nid': ["3000"]
                    },
                    'target': 'catalog'
                }
            },
        })

        response = self.cataloger.request_json('GetNavigationNode?nid=3100&format=json')
        self.assertFragmentIn(response, {
            'result': {
                'category': {
                    'id': 310,
                },
                'id': 3100,
                'type': 'category',
                'isLeaf': True,
            },
        })

    def test_bulk_nids_many(self):
        response = self.cataloger.request_json('GetNavigationNode?nids=1000,2700&format=json')

        self.assertFragmentIn(
            response,
            {
                'result': {
                    'category': [{
                        'category': {
                            'id': 100,
                        },
                        'id': 1000,
                    }, {
                        'category': {
                            'id': 270,
                        },
                        'id': 2700,
                    }]
                }
            }
        )

    def test_bulk_nids_single(self):
        response = self.cataloger.request_json('GetNavigationNode?nids=1000&format=json')

        self.assertFragmentIn(response, {
            'result': {
                'category': [{
                    'category': {
                        'id': 100,
                    },
                    'id': 1000,
                }]
            }
        })

    def test_nid_single(self):
        response = self.cataloger.request_json('GetNavigationNode?nid=1000&format=json')

        self.assertFragmentIn(response, {
            'result': {
                'category': {
                    'id': 100,
                },
                'id': 1000,
            }
        })

    def test_primary_nids(self):
        # для хида 310 есть узлы 3100 и 3200, второй - приоритетный
        response = self.cataloger.request_json('GetNavigationNode?hid=310&format=json')
        self.assertFragmentIn(response, {
            'result': {
                'category': {
                    'id': 310,
                },
                'id': 3200,
            },
        })

        # корни деревьев в зависимости от цвета
        # Для белого дерева достаточно только значения rgb.
        for root_nid, color in [(1000, 'green'), (1000, 'blue'), (10000, 'black')]:
            response = self.cataloger.request_json('GetNavigationNode?hid=100&format=json&rgb={}'.format(color))
            self.assertFragmentIn(response, {
                'result': {
                    'category': {
                        'id': 100,
                    },
                    'id': root_nid,
                },
            })

    def test_nid_b2b(self):
        '''
        Проверяем, что получение нидов из B2B дерева работает корректно
        '''
        expected_response = {
            "category": {
                "id": 200
            },
            "id": 20000
        }
        # проверяем, что мы находим нид при явном указании цвета в запросе
        response = self.cataloger.request_json('GetNavigationNode?nid=20000&format=json&rgb=black')
        self.assertFragmentIn(response, expected_response)
        # проверяем, что если цвет в запросе не указан - мы находим нужное дерево по nid'у
        response = self.cataloger.request_json('GetNavigationNode?nid=20000&format=json')
        self.assertFragmentIn(response, expected_response)
        # проверяем, что получение узла по hid'у тоже работает
        response = self.cataloger.request_json('GetNavigationNode?hid=200&format=json&rgb=black')
        self.assertFragmentIn(response, expected_response)
        # проверяем, что в ответ на запрос с другим цветом не просачиваются ниды B2B дерева
        response = self.cataloger.request_json('GetNavigationNode?nid=20000&format=json&rgb=green')
        self.assertFragmentNotIn(response, {"id": 20000})

    def test_nid_by_hid_b2b(self):
        '''
        Проверяем, что определение нида по хиду работает корректно и для B2B дерева
        '''
        b2b_response = {
            "category": {
                "id": 200
            },
            "id": 20000
        }
        main_tree_response = {
            "category": {
                "id": 200
            },
            "id": 2000
        }
        # проверяем получение b2b нида
        response = self.cataloger.request_json('GetNavigationNode?hid=200&format=json&rgb=black')
        self.assertFragmentIn(response, b2b_response)
        # а для обычных запросов ожидаем ниды из главного дерева
        response = self.cataloger.request_json('GetNavigationNode?hid=200&format=json')
        self.assertFragmentIn(response, main_tree_response)

    def test_redirects(self):
        # Узла 100500 в зеленом дереве не существует, но задан редирект 100500->2100.
        response = self.cataloger.request_json('GetNavigationNode?nid=100500&format=json&rgb=green')
        self.assertFragmentIn(response, {
            'id': 2100,
            'oldNid': 100500
        })

        response = self.cataloger.request_json('GetNavigationNode?nid=100501&format=json&rgb=white')
        self.assertFragmentIn(response, {
            'result': {
                'id': 2100,  # редирект есть
                'oldNid': 100501,  # нид из запроса
            },
        })

        # проверяем, что без указания цвета мы получаем редиректы к нидам из главного дерева
        response = self.cataloger.request_json('GetNavigationNode?nid=100501&format=json')
        self.assertFragmentIn(response, {
            'result': {
                'id': 2100,  # редирект есть
                'oldNid': 100501,  # нид из запроса
            },
        })

        response = self.cataloger.request_json('GetNavigationNode?nid=2100&format=json&rgb=white')
        self.assertFragmentIn(response, {
            'result': {
                'id': 2100,
                'oldNid': Absent(),  # обратного редиректа нет
            },
        })

    def test_green_offers(self):
        show_offers = {200: dict(), 240: dict(), 230: dict(), 100: dict()}
        # 1) Пары категория + регион, указанные в статистике

        # - в листовом хиде 200 офферы есть в регионах 2 (Питер) и 213 (Москва).
        show_offers[200][2] = self.offers_in_hid(200, 2)
        show_offers[200][213] = self.offers_in_hid(200, 213)
        # Для родительских регионов ПРИ СОХРАНЕНИИ СТАТИСТИКИ В ТЕСТАХ
        # суммируются листовые регионы-потомки, и тоже записываются в статистику.
        # 225 - Россия, просуммировались Москва и Питер.
        show_offers[200][225] = show_offers[200][2] + show_offers[200][213]
        # 1000 - весь мир, тоже просуммировались Москва и Питер
        show_offers[200][10000] = show_offers[200][2] + show_offers[200][213]

        # - в листовом хиде 240 офферы есть в регионах 143 (Киев) и 213 (Москва)
        show_offers[240][143] = self.offers_in_hid(240, 143)
        show_offers[240][213] = self.offers_in_hid(240, 213)
        # ПРИ СОХРАНЕНИИ СТАСТИКИ для 225 (Россия) офферы взялись из Москвы,
        # а для 10000 (весь мир) просуммировались Москва и Киев
        show_offers[240][225] = show_offers[240][213]
        show_offers[240][10000] = show_offers[240][143] + show_offers[240][213]

        # - категория 230 - предок категорий 200 и 240, ПРИ СОХРАНЕНИИ СТАТИСТИКИ
        # В ТЕСТАХ для родительских категорий суммируются категории-листы
        # в регионе 2 (Питер) офферы есть только в листе 200.
        show_offers[230][2] = show_offers[200][2]
        # в регионе 143 (Киев) офферы есть только в листе 240
        show_offers[230][143] = show_offers[240][143]
        # в регионе 213 (Москва) офферы есть и в листе 200, и в листе 240
        show_offers[230][213] = show_offers[200][213] + show_offers[240][213]

        # - так же происходит расчет для родительских регионов
        # в регионе 225 (Россия) суммируются офферы из Москвы и Питера
        show_offers[230][225] = show_offers[230][213] + show_offers[230][2]
        # в регионе 10000 (весь мир) суммируются офферы из Москвы, Питера и Киева
        show_offers[230][10000] = show_offers[230][213] + show_offers[230][2] + show_offers[230][143]

        # - Категория 100 - корень, там значения как и в категории 230
        for region in (2, 213, 143, 225, 10000):
            show_offers[100][region] = show_offers[230][region]

        # 2) Пары категория + регион, НЕ указанные в статистике. В таком случае
        # в КАТАЛОГЕРЕ (а не при сохранении статистики) "поднимаемся"
        # по дереву регионов вверх, проверяя каждый регион. Как только нашелся
        # регион с офферами, останавливаемся. Это криво, но годами так.

        # - для категории 200 в регионе 143 (Киев) в статистике нет записей, зато
        # есть для регионов 213 (Москва) и 2 (Питер). Сумма Москвы и Питера
        # есть в регионе 10000 (весь мир). "Поднимаясь" от Киева, мы доходим
        # до всего мира, и берем это значение
        show_offers[200][143] = show_offers[200][10000]

        # - в категории 240 в регионе 2 (Питер) офферов нет.
        # "Поднялись" по дереву регионов до 225 (Россия), там уже есть офферы
        # из 213 (Москвы)
        show_offers[240][2] = show_offers[240][225]

        # В регионе 135312 (тьмутаракань) офферов нет вообще нигде,
        # но тоже поднялись до России, и взяли значения от туда
        for hid in (200, 230, 240, 100):
            show_offers[hid][135312] = show_offers[hid][225]

        for region in (2, 213, 143, 225, 10000):
            for hid in (200, 240, 230, 100, 270):
                offers = 0 if hid == 270 else show_offers[hid][region]
                response = self.cataloger.request_json('GetNavigationNode?hid={}&region={}&format=json'.format(hid, region))
                self.assertFragmentIn(response, {
                    'id': hid * 10,
                    'category': {
                        'id': hid,
                        'offersCount': offers
                    }
                })

    def test_blue_stats_offers_in_main_tree(self):
        # в зеленой статистике у хида 205 нет офферов
        response = self.cataloger.request_json('GetNavigationNode?hid=205&region=213&format=json')
        self.assertFragmentIn(response, {
            'id': 205,
            'offersCount': 0
        })
        # В главном дереве у хида 205 родитель 230. Офферы попадают туда и в корень.
        for hid in (205, 230, 100):
            response = self.cataloger.request_json('GetNavigationNode?hid={}&region=213&format=json&rgb=blue'.format(hid))
            self.assertFragmentIn(response, {
                'id': hid,
                'offersCount': 100500
            })

    def test_blue_stats_offers_in_blue_tree(self):
        # В синем дереве 230 не является предком 205. Но в category/offersCount
        # суммируются офферы по товарному дереву, а оно общее с зеленым. Поэтому
        # в синем дереве в категорию 230 всеравно пролезают офферы из 205
        for hid in (205, 230, 100):
            response = self.cataloger.request_json('GetNavigationNode?hid={}&region=213&format=json&rgb=blue&use-multi-navigation-tree=1'.format(hid))
            self.assertFragmentIn(response, {
                'id': hid,
                'offersCount': 100500
            })

    def test_regional_slug(self):
        """
        Во всех регионах, кроме Москвы и Московской области и Санкт-Петербурга и Ленинградсткой области должны быть
        региональные слаги
        """
        # 213 - Москва
        # 1 - Московская область
        # 2 - Санкт-Петербург
        # 10174 - Ленинградская область
        # 100500 - несуществующий
        for region in (213, 1, 2, 10174, 100500):
            response = self.cataloger.request_json('GetNavigationNode?nid=2300&format=json&region={}'.format(region))
            self.assertFragmentIn(response, {
                'result': {
                    'entity': 'navnode',
                    'id': 2300,
                    'name': 'Категория',
                    'slug': 'kategoriia-2300',
                },
            })

        # Симферополь
        response = self.cataloger.request_json('GetNavigationNode?nid=2300&format=json&region=146')
        self.assertFragmentIn(response, {
            'result': {
                'entity': 'navnode',
                'id': 2300,
                'name': 'Категория',
                'slug': 'kategoriia-2300-v-simferopole',
            },
        })

    def test_tags(self):
        response = self.cataloger.request_json('GetNavigationNode?nid=2200&format=json')
        self.assertFragmentIn(response, {
            'result': {
                'entity': 'navnode',
                'id': 2200,
                'tags': [
                    'tag1',
                    'tag2',
                ],
            },
        })

    def test_tags_xml(self):
        response = self.cataloger.request_xml('GetNavigationNode?nid=2200&format=xml')
        self.assertFragmentIn(
            response,
            '''
            <category nid="2200">
                <tags>
                    <tag name="tag1"/>
                    <tag name="tag2"/>
                </tags>
            </category>''',
        )

    def test_availability(self):
        # Проверка доступности маркета в регионе
        response = self.cataloger.request_json('GetMarketAvailability?region={}'.format(213))
        self.assertFragmentIn(
            response,
            {
                'result': {
                    'exists': True,
                    'available': True,
                },
            },
        )

        # Проверка для региона из файла, но с числом офферов == 0
        response = self.cataloger.request_json('GetMarketAvailability?region={}'.format(214))
        self.assertFragmentIn(
            response,
            {
                'result': {
                    'exists': False,
                    'available': False,
                },
            },
        )

        # Проверка для региона, не указанного в файле конфигурации
        response = self.cataloger.request_json('GetMarketAvailability?region={}'.format(215))
        self.assertFragmentIn(
            response,
            {
                'result': {
                    'exists': False,
                    'available': False,
                },
            },
        )


if __name__ == '__main__':
    main()
