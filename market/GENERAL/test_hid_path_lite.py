#!/usr/bin/env python
# -*- coding: utf-8 -*-

from core.testcase import TestCase, main
from core.types.navigation_info import NavigationNode, NavigationTree


class T(TestCase):

    @classmethod
    def prepare(cls):
        nodes = [
            NavigationNode(nid=1, hid=1, parent_nid=0, short_name='Все товары'),
            NavigationNode(nid=12, hid=0, parent_nid=1, short_name='Продукты'),
            NavigationNode(nid=122, hid=122, parent_nid=12, short_name='Колбасы'),
            NavigationNode(nid=1221, hid=1221, parent_nid=122, short_name='Копченые'),
            NavigationNode(nid=12211, hid=12211, parent_nid=1221, short_name='Краковские'),
        ]
        cls.index.navigation_trees += [NavigationTree(code='green', nodes=nodes)]

    def test_hid_path_leaf(self):
        # Проверяем путь от листа до корня
        response = self.cataloger.request_json('GetHidPathLite?format=json&hid=12211')
        self.assertFragmentIn(response, {
            "result": {
                "__name__": "data",
                "pathToRoot": [12211, 1221, 122, 1],
                "pathFromRoot": [1, 122, 1221, 12211]
            }
        })

    def test_hid_path_not_leaf(self):
        # Проверяем путь от нелистового узла до корня
        response = self.cataloger.request_json('GetHidPathLite?format=json&hid=1221')
        self.assertFragmentIn(response, {
            "result": {
                "__name__": "data",
                "pathToRoot": [1221, 122, 1],
                "pathFromRoot": [1, 122, 1221]
            }
        })

    def test_hid_path_missed(self):
        # Проверяем генерацию ошибки для неизвестного узла
        with self.assertRaisesRegexp(RuntimeError, 'no category id=2'):
            self.cataloger.request_json('GetHidPathLite?format=json&hid=2')

    def test_request_without_hid(self):
        # В запросе нет hid
        with self.assertRaisesRegexp(RuntimeError, 'required parameter hid is missed'):
            self.cataloger.request_json('GetHidPathLite?format=json')

    def test_hid_path_xml(self):
        # Проверяем формат выдачи в xml
        with self.assertRaisesRegexp(RuntimeError, 'json format only'):
            self.cataloger.request_json('GetHidPathLite?format=xml&hid=122')


if __name__ == '__main__':
    main()
