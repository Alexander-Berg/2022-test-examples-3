#!/usr/bin/env python
# -*- coding: utf-8 -*-

from core.testcase import TestCase, main
from core.types.navigation_info import NavigationNode, NavigationTree

'''
Дерево регионов находится в data/geobase.xml
'''


class T(TestCase):

    @classmethod
    def prepare(cls):
        nodes = [
            NavigationNode(nid=1, hid=1, parent_nid=0, short_name='Все товары')
        ]
        cls.index.navigation_trees += [NavigationTree(code='green', nodes=nodes)]

        cls.index.geo_map_pairs = [
            {'id': 10000, 'parent': 0, 'name': "Земля", 'locative': "Земле", 'preposition': "на"},
            {'id': 11, 'parent': 10000, 'name': 'Евразия', 'locative': 'Евразии', 'preposition': 'в'},
            {'id': 111, 'parent': 11, 'name': 'СНГ', 'locative': 'СНГ', 'preposition': 'в'},
            {'id': 1111, 'parent': 111, 'name': 'Россия', 'locative': 'России', 'preposition': 'в'},
        ]

    def test_region_path_leaf(self):
        # Проверяем путь от листа до корня
        response = self.cataloger.request_json('GetRegionPathLite?format=json&region=1111')
        self.assertFragmentIn(response, {
            "result": {
                "__name__": "data",
                "pathToRoot": [1111, 111, 11, 10000],
                "pathFromRoot": [10000, 11, 111, 1111]
            }
        })

    def test_region_path_not_leaf(self):
        # Проверяем путь от нелистового узла до корня
        response = self.cataloger.request_json('GetRegionPathLite?format=json&region=11')
        self.assertFragmentIn(response, {
            "result": {
                "__name__": "data",
                "pathToRoot": [11, 10000],
                "pathFromRoot": [10000, 11]
            }
        })

    def test_region_path_missed(self):
        # Проверяем генерацию ошибки для неизвестного узла
        with self.assertRaisesRegexp(RuntimeError, 'no region id=22'):
            self.cataloger.request_json('GetRegionPathLite?format=json&region=22')

    def test_request_without_hid(self):
        # В запросе нет regionq
        with self.assertRaisesRegexp(RuntimeError, 'required parameter region is missed'):
            self.cataloger.request_json('GetRegionPathLite?format=json')

    def test_region_path_xml(self):
        # Проверяем формат выдачи в xml
        with self.assertRaisesRegexp(RuntimeError, 'json format only'):
            self.cataloger.request_json('GetRegionPathLite?format=xml&region=111')


if __name__ == '__main__':
    main()
