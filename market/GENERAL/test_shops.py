#!/usr/bin/env python
# -*- coding: utf-8 -*-

from core.testcase import TestCase, main
from core.types.navigation_info import NavigationNode, NavigationTree
from core.types.categories_stats import ShopCategories


class T(TestCase):
    @classmethod
    def prepare(cls):
        nodes = [
            NavigationNode(nid=1000, hid=100, parent_nid=0),

            NavigationNode(nid=2000, hid=200, parent_nid=1000),
            NavigationNode(nid=2100, hid=210, parent_nid=2000, is_primary=False),
            NavigationNode(nid=2200, hid=220, parent_nid=2000),

            NavigationNode(nid=3000, hid=300, parent_nid=1000),
            NavigationNode(nid=3100, hid=310, parent_nid=3000),
            NavigationNode(nid=3200, hid=320, parent_nid=3000),

            NavigationNode(nid=4000, hid=400, parent_nid=1000),
            NavigationNode(nid=4100, hid=410, parent_nid=4000),
            NavigationNode(nid=4200, hid=420, parent_nid=4000),

            NavigationNode(nid=5000, hid=0, parent_nid=1000),
            NavigationNode(nid=5100, hid=210, parent_nid=5000),  # primary узел для хида 210
            NavigationNode(nid=5200, hid=310, parent_nid=5000, is_primary=False),
        ]

        cls.index.navigation_trees += [NavigationTree(code='green', nodes=nodes)]

        cls.index.shops_popular_categories_stats = [
            ShopCategories(shop_id=101, region=-1, categories=[210, 320, 220, 310]),
            ShopCategories(shop_id=102, region=-1, categories=[420, 410, 320, 310, 220, 210]),
            ShopCategories(shop_id=9012345678, region=-1, categories=[210, 320, 220, 310]),
        ]

    def test_shop_top_categories_root(self):
        ''' Проверяем, что порядок выдачи хидов совпадает с порядком в статистике
        '''

        response = self.cataloger.request_json('GetShopTopCategories?fesh=101&format=json&depth=10')
        self.assertFragmentIn(response, [
            {
                'id': 5100,  # избегаем дублирования хидов, выбрали primary нид
                'category': {'id': 210},
            },
            {
                'id': 3200,
                'category': {'id': 320},
            },
            {
                'id': 2200,
                'category': {'id': 220},
            },
            {
                'id': 3100,
                'category': {'id': 310},
            },
        ], preserve_order=True, allow_different_len=False)

        # Выводится всего 5 хидов, хотя в статистике их больше
        response = self.cataloger.request_json('GetShopTopCategories?fesh=102&format=json&depth=10')
        self.assertFragmentIn(response, [
            {
                'id': 4200,
                'category': {'id': 420},
            },
            {
                'id': 4100,
                'category': {'id': 410},
            },
            {
                'id': 3200,
                'category': {'id': 320},
            },
            {
                'id': 3100,
                'category': {'id': 310},
            },
            {
                'id': 2200,
                'category': {'id': 220},
            },
        ], preserve_order=True, allow_different_len=False)

    def test_shop_top_categories_not_root(self):
        # департамент с реальным хидом
        response = self.cataloger.request_json('GetShopTopCategories?fesh=101&nid=3000&format=json&depth=10')
        self.assertFragmentIn(response, [
            {
                'id': 3200,
                'category': {'id': 320},
            },
            {
                'id': 3100,
                'category': {'id': 310},
            },
        ], preserve_order=True, allow_different_len=False)

        # лист (они всегда с хидами)
        response = self.cataloger.request_json('GetShopTopCategories?fesh=101&nid=3200&format=json&depth=10')
        self.assertFragmentIn(response, [
            {
                'id': 3200,
                'category': {'id': 320},
            },
        ], preserve_order=True, allow_different_len=False)

        # виртуальный департамент, uint64 fesh 9012345678
        for fesh in (101, 9012345678):
            request = 'GetShopTopCategories?fesh={}&nid=5000&format=json&depth=10'.format(fesh)
            response = self.cataloger.request_json(request)
            self.assertFragmentIn(response, [
                {
                    'id': 5100,
                    'category': {'id': 210},
                },
                {
                    'id': 5200,  # primary узел для 310 - 3100, но нам нужна подкатегории 5000
                    'category': {'id': 310},
                },
            ], preserve_order=True, allow_different_len=False)

if __name__ == '__main__':
    main()
