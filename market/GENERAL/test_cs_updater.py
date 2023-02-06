#!/usr/bin/env python
# -*- coding: utf-8 -*-

from core.testcase import TestCase, main
from core.types.navigation_info import NavigationNode, NavigationTree, NavigationInfo


'''
Теперь cataloger при старте может ходить в сервис content-storage, чтобы получать самую свежую инфу о навигации.
В тесте проверяем, что при наличии за основу берется информация из content-storage

Также проверяем работу воркеров для обновления данныз их content-storage

TODO: Нужно составить план лайт тестов, основанный на реальных кейсах
Как минимум добавить в тесты взаимодействия с хидами
'''


class T(TestCase):
    @classmethod
    def prepare(cls):
        old_nodes = [
            NavigationNode(nid=1000, hid=100, parent_nid=0, short_name='Все товары сине-зеленые'),
            NavigationNode(nid=1500, hid=0, parent_nid=1000, short_name='Бесцветный департамент', is_blue=False, is_green=False),
            NavigationNode(nid=2000, hid=200, parent_nid=1500, short_name='Мебель сине-зеленая'),
            NavigationNode(nid=2100, hid=0, parent_nid=2000, short_name='Кухня зеленая', is_blue=False),
            NavigationNode(nid=2110, hid=211, parent_nid=2100, short_name='Столы в зеленой кухне', is_primary=True, is_blue=False),
        ]

        content_storage_nodes = [
            NavigationNode(nid=1000, hid=100, parent_nid=0, short_name='Все товары сине-зеленые (from cs)'),
            NavigationNode(nid=1500, hid=0, parent_nid=1000, short_name='Бесцветный департамент (from cs)', is_blue=False, is_green=False),
            NavigationNode(nid=2000, hid=200, parent_nid=1500, short_name='Мебель сине-зеленая (from cs)'),
            NavigationNode(nid=2100, hid=0, parent_nid=2000, short_name='Кухня зеленая (from cs)', is_blue=False),
            NavigationNode(nid=2110, hid=211, parent_nid=2100, short_name='Столы в зеленой кухне (from cs)', is_primary=True, is_blue=False),
        ]

        cls.index.navigation_trees += [NavigationTree(code='green', nodes=old_nodes)]
        cls.content_storage.navigation_trees += [NavigationTree(code='green', nodes=content_storage_nodes)]

        cls.index.use_content_storage_data = True

    def test_cs_navigation_updater(self):
        '''
        Проверка воркера обновления навигационных данных
        Будем так имитировать работу воркера:
        1) Сделаем запрос за старой навигационной инфой
        2) Оновим навигационную инфу у мока content-storage
        3) Заюзаем ручку каталогера UpdateCsData, которая нужна для принудительного обновления данных из content-storage. Она по сути вызывает код воркера
        4) проверим, что данные обновились
        '''
        # Запрос со старым деревом из content-storage
        response = self.cataloger.request_json('GetNavigationPath?nid=2110&format=json&rgb=green')
        self.assertFragmentIn(response, {
            'result': {
                'id': 2000,
                'name': 'Мебель сине-зеленая (from cs)',
                'navnodes': [
                    {
                        'id': 2100,
                        'name': 'Кухня зеленая (from cs)',
                        'navnodes': [
                            {
                                'id': 2110,
                                'name': 'Столы в зеленой кухне (from cs)',
                            }
                        ]
                    }
                ]
            }
        })

        # Обновляем дерево под content-storage
        new_content_storage_nodes = [
            NavigationNode(nid=1000, hid=100, parent_nid=0, short_name='Все товары сине-зеленые (new from cs)'),
            NavigationNode(nid=1500, hid=0, parent_nid=1000, short_name='Бесцветный департамент (new from cs)', is_blue=False, is_green=False),
            NavigationNode(nid=2000, hid=200, parent_nid=1500, short_name='Мебель сине-зеленая (new from cs)'),
            NavigationNode(nid=2100, hid=0, parent_nid=2000, short_name='Кухня зеленая (new from cs)', is_blue=False),
            NavigationNode(nid=2110, hid=211, parent_nid=2100, short_name='Столы в зеленой кухне (new from cs)', is_primary=True, is_blue=False),
        ]
        new_tree = NavigationTree(code='green', nodes=new_content_storage_nodes)
        new_navigation = NavigationInfo(trees=[new_tree], menus=[], links=[], recipes=[])
        self.content_storage.update_navigation_data(new_navigation.convert_to_string())

        # Принудительно вызываем код воркера
        response = self.cataloger.request_json('UpdateCsData?type=navigation')

        # Запрос c новым деревом
        response = self.cataloger.request_json('GetNavigationPath?nid=2110&format=json&rgb=green')
        self.assertFragmentIn(response, {
            'result': {
                'id': 2000,
                'name': 'Мебель сине-зеленая (new from cs)',
                'navnodes': [
                    {
                        'id': 2100,
                        'name': 'Кухня зеленая (new from cs)',
                        'navnodes': [
                            {
                                'id': 2110,
                                'name': 'Столы в зеленой кухне (new from cs)',
                            }
                        ]
                    }
                ]
            }
        })

if __name__ == '__main__':
    main()
