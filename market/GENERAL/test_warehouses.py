#!/usr/bin/env python
# -*- coding: utf-8 -*-

from core.testcase import TestCase, main
from core.types.navigation_info import NavigationNode, NavigationTree
from core.types.links import Recipe
from core.types.categories_stats import WarehouseCategories
from core.types.express_partners import ExpressWarehousesEncoder, EatsWarehousesEncoder
from core.matcher import Absent, ElementCount, NotEmptyList
from unittest import skip
from core.types.combinator import (
    CombinatorGpsCoords,
    CombinatorExpressWarehouse,
)

EXPRESS_ROOT = 23272130
EXPRESS_EATS_NID = 23277530

WAREHOUSE_ID_100 = 100
WAREHOUSE_ID_200 = 200
WAREHOUSE_ID_300 = 300

MOSCOW_REGION_ZONE_ID = 1

WAREHOUSE_100 = CombinatorExpressWarehouse(
    warehouse_id=WAREHOUSE_ID_100,
    zone_id=MOSCOW_REGION_ZONE_ID,
)
WAREHOUSE_200 = CombinatorExpressWarehouse(
    warehouse_id=WAREHOUSE_ID_200,
    zone_id=MOSCOW_REGION_ZONE_ID,
)
WAREHOUSE_300 = CombinatorExpressWarehouse(
    warehouse_id=WAREHOUSE_ID_300,
    zone_id=MOSCOW_REGION_ZONE_ID,
)


def make_gps(lat, lon):
    return 'lat:{lat};lon:{lon}'.format(lat=lat, lon=lon)


def make_same_gps(one_coord):
    return make_gps(one_coord, one_coord)


GPS_0 = make_same_gps(0.0)
GPS_1 = make_same_gps(1.0)
GPS_2 = make_same_gps(2.0)
GPS_3 = make_same_gps(3.0)

# для неизвестных координат
GPS_90 = make_same_gps(90)


class T(TestCase):
    @classmethod
    def prepare(cls):
        nodes = [
            NavigationNode(nid=1000, hid=100, parent_nid=0),
            NavigationNode(nid=EXPRESS_ROOT, hid=0, parent_nid=1000),

            NavigationNode(nid=EXPRESS_EATS_NID, hid=150, parent_nid=EXPRESS_ROOT),
            NavigationNode(nid=1510, hid=151, parent_nid=EXPRESS_EATS_NID),
            NavigationNode(nid=1520, hid=0, parent_nid=EXPRESS_EATS_NID),
            NavigationNode(nid=1600, hid=160, parent_nid=1520),

            NavigationNode(nid=2000, hid=200, parent_nid=EXPRESS_ROOT),
            NavigationNode(nid=2100, hid=210, parent_nid=2000),
            NavigationNode(nid=2200, hid=220, parent_nid=2000),
            NavigationNode(nid=2110, hid=211, parent_nid=2100),

            NavigationNode(nid=3000, hid=300, parent_nid=EXPRESS_ROOT),
            NavigationNode(nid=3100, hid=310, parent_nid=3000),
            NavigationNode(nid=3200, hid=320, parent_nid=3000),

            NavigationNode(nid=4000, hid=400, parent_nid=EXPRESS_ROOT),
            # проверка, что наличие xml подтэгов в листовых навигационных узлах ничего не ломает
            NavigationNode(nid=4100, hid=410, parent_nid=4000, tags=['tag1', 'tag2']),
            NavigationNode(nid=4200, hid=420, parent_nid=4000),

            # виртуальный Экспресс поддепартамент, и промежуточный узел виртуальный
            NavigationNode(nid=5000, hid=0, parent_nid=EXPRESS_ROOT),
            NavigationNode(nid=5100, hid=0, parent_nid=5000),
            NavigationNode(nid=5110, hid=511, parent_nid=5100),

            # узел-рецепт с подходящим хидом тоже должен попасть в выдачу
            NavigationNode(nid=6000, hid=410, parent_nid=EXPRESS_ROOT, recipe_id=1),

            # не Экспресс департамент
            NavigationNode(nid=1500, hid=150, parent_nid=1000),
            # в нем есть экспресс хид
            NavigationNode(nid=1550, hid=210, parent_nid=1500),
        ]

        cls.index.navigation_trees += [NavigationTree(code='green', nodes=nodes)]
        cls.index.navigation_recipes += [Recipe(recipe_id=1, hid=410, filters=list())]

        # региональность в статистике убрали, все записи относятся к региону -1
        cls.index.warehouses_express_categories = [WarehouseCategories(wid=100, region=-1, hids=[211, 220]),  # был регион 213
                                                   WarehouseCategories(wid=200, region=-1, hids=[410, 310]),  # был регион 213
                                                   WarehouseCategories(wid=300, region=-1, hids=[160, 151]),
                                                   WarehouseCategories(wid=310, region=-1, hids=[151])]

        cls.combinator_express.on_express_warehouses_request(
            region_id=213,
            gps_coords=CombinatorGpsCoords(0.0, 0.0),
            rear_factors="",
        ).respond_with_express_warehouses(
            [
                WAREHOUSE_100,
            ]
        )
        cls.combinator_express.on_express_warehouses_request(
            region_id=213,
            gps_coords=CombinatorGpsCoords(1.0, 1.0),
            rear_factors="",
        ).respond_with_express_warehouses(
            [
                WAREHOUSE_200,
            ]
        )
        cls.combinator_express.on_express_warehouses_request(
            region_id=213,
            gps_coords=CombinatorGpsCoords(2.0, 2.0),
            rear_factors="",
        ).respond_with_express_warehouses(
            [
                WAREHOUSE_100,
                WAREHOUSE_200,
            ]
        )
        cls.combinator_express.on_express_warehouses_request(
            region_id=213,
            gps_coords=CombinatorGpsCoords(3.0, 3.0),
            rear_factors="",
        ).respond_with_express_warehouses(
            [
                WAREHOUSE_300,
            ]
        )
        #  WarehouseCategories(wid=100, region=2, hids=(310, 511)),
        #  WarehouseCategories(wid=200, region=146, hids=(410,))]

    def get_compressed_common_warehouses(self, compressed_warehouses, warehouses):
        if isinstance(warehouses, int):
            compressed_warehouses.add_warehouse(warehouses)
        else:
            for wh in warehouses:
                compressed_warehouses.add_warehouse(wh)
        return compressed_warehouses.encode()

    def get_compressed_express_warehouses(self, warehouses):
        compressed_warehouses = ExpressWarehousesEncoder()
        return self.get_compressed_common_warehouses(compressed_warehouses, warehouses)

    def get_compressed_eats_warehouses(self, warehouses):
        compressed_warehouses = EatsWarehousesEncoder()
        return self.get_compressed_common_warehouses(compressed_warehouses, warehouses)

    def _gen_request(
        self,
        region=None,
        warehouses=None,
        compress_express_warehouses=None,
        gps=None,
        compress_eats_warehouses=None,
        depth=1,
        express_root=True,
        nid=None,
        new_express_logic=None,
    ):
        # show_empty=1 т.к нет офферной статистики, а категории без офферов скрываются
        req = 'GetNavigationTree?format=json&show_empty=1'
        if express_root:
            req += '&nid={}'.format(nid or EXPRESS_ROOT)

        # региональность в статистике по складам убрали, но вдруг захотят вернуть
        if region is not None:
            req += '&region={}'.format(region)

        if compress_express_warehouses:
            express_warehouses_compressed = self.get_compressed_express_warehouses(warehouses=compress_express_warehouses)
            req += '&express-warehouses-compressed={}'.format(express_warehouses_compressed)

        if gps is not None:
            req += '&gps={}'.format(gps)

        if compress_eats_warehouses:
            express_warehouses_compressed = self.get_compressed_eats_warehouses(warehouses=compress_eats_warehouses)
            req += '&eats-warehouses-compressed={}'.format(express_warehouses_compressed)

        if warehouses is not None:
            if isinstance(warehouses, int):
                req += '&express-warehouse={}'.format(warehouses)
            else:
                for w in warehouses:
                    req += '&express-warehouse={}'.format(w)

        if depth is not None:
            req += '&depth={}'.format(depth)
        if new_express_logic is not None:
            req += '&new-express-logic={}'.format(new_express_logic)
        return req

    def test_params_failure(self):
        # без express-warehouses фильтрации нет
        response = self.cataloger.request_json(self._gen_request(region=213, depth=1))
        self.assertFragmentIn(response, {'id': EXPRESS_ROOT, 'navnodes': ElementCount(6)})

        # без региона выдача пустая
        response1 = self.cataloger.request_json(self._gen_request(warehouses=100, depth=1))
        # без depth или с depth=0 только экспресс корень
        response2 = self.cataloger.request_json(self._gen_request(region=213, warehouses=100, depth=None))
        response3 = self.cataloger.request_json(self._gen_request(region=213, warehouses=100, depth=0))

        for response in (response1, response2, response3):
            self.assertFragmentIn(response, {'id': EXPRESS_ROOT, 'navnodes': Absent()})

    def test_only_express_department(self):
        # Предрасчет делается только для поддерева 23272130. Остальные департаменты
        # никогда не будут показываться, даже если в них и есть экспресс хиды

        response = self.cataloger.request_json(self._gen_request(region=213, warehouses=100, depth=2, express_root=False))
        self.assertFragmentIn(response, {
            'id': 1000,
            'navnodes': [
                {
                    'id': EXPRESS_ROOT
                },
            ]
        }, allow_different_len=False)

    def _warehouse_for_req_id(self, warehouses_lists):
        for wlist in warehouses_lists:
            if wlist is not None:
                return wlist if isinstance(wlist, int) else wlist[0]
        return 0

    def check_depth_param(
        self,
        warehouses=None,
        compress_express_warehouses=None,
        gps=None,
        compress_eats_warehouses=None,
        new_express_logic=None,
    ):
        for depth in (2, 3, 10):
            response = self.cataloger.request_json(
                self._gen_request(
                    region=213,
                    warehouses=warehouses,
                    compress_express_warehouses=compress_express_warehouses,
                    gps=gps,
                    compress_eats_warehouses=compress_eats_warehouses,
                    depth=depth,
                    new_express_logic=new_express_logic,
                ),
                headers={'market_req_id': 'check_depth_param'}
            )
            self.assertFragmentIn(response, {
                'id': EXPRESS_ROOT,
                'navnodes': [
                    {
                        'id': 2000,
                        'navnodes': [
                            {
                                'id': 2100,
                                # запоминаются только экспресс категории 1-2 уровня, поэтому нид 2110 не будет показан
                                'navnodes': Absent(),
                                # если снова будут запоминаться и более глубокие узлы
                                # 'navnodes': Absent() if depth == 2 else [{'id': 2110, 'navnodes': Absent()}]
                            },
                            {
                                'id': 2200,
                                'navnodes': Absent()
                            }
                        ],
                    },
                ]
            }, allow_different_len=False, preserve_order=True)

        # вызов для подузла в экспресс департаменте
        response = self.cataloger.request_json(
            self._gen_request(
                nid=2100,
                region=213,
                warehouses=warehouses,
                compress_express_warehouses=compress_express_warehouses,
                gps=gps,
                compress_eats_warehouses=compress_eats_warehouses,
                depth=2
            )
        )
        self.assertFragmentIn(response, {
            'result': {
                'navnodes': [
                    {
                        'id': 2100,
                        # запоминаются только экспресс категории 1-2 уровня, поэтому нид 2110 не будет показан
                        'navnodes': Absent(),
                        # если снова будут запоминаться и более глубокие узлы
                        # 'navnodes': [{
                        #    'id': 2110,
                        #    'navnodes': Absent(),
                        # }]
                    }
                ]
            }
        }, allow_different_len=False)

    def test_depth_param(self):
        for new_express_logic in (None, False, True):
            # склады в запросе приоритетнее, в случае их наличия плохой gps ничего не ломает
            self.check_depth_param(gps=GPS_90, warehouses=100, new_express_logic=new_express_logic)
            self.check_depth_param(gps=GPS_90, compress_express_warehouses=100, new_express_logic=new_express_logic)

            self.check_depth_param(warehouses=100, new_express_logic=new_express_logic)
            self.check_depth_param(compress_express_warehouses=100, new_express_logic=new_express_logic)
            self.check_depth_param(gps=GPS_0, new_express_logic=new_express_logic)
            self.check_depth_param(compress_eats_warehouses=100, new_express_logic=new_express_logic)

            # Склады экспресса игнорируются, если передан основной склад
            self.check_depth_param(warehouses=100, compress_express_warehouses=200, new_express_logic=new_express_logic)
            self.check_depth_param(warehouses=100, compress_express_warehouses=[100, 200], new_express_logic=new_express_logic)
            self.check_depth_param(warehouses=100, compress_eats_warehouses=[100, 200], new_express_logic=new_express_logic)

        # В запросах или хорошие gps, или есть склады, поэтому не встречалось ошибок в походе в комбинатор
        self.assertFalse(self.server.has_note_in_common_log('.*request_id\\ =\\ check_depth_param.*'))

    def test_bad_gps(self):
        response = self.cataloger.request_json(
            self._gen_request(region=213, gps=GPS_90, depth=1),
            headers={'market_req_id': 'test_bad_gps_no_warehouses'},
        )

        # Явно указываем внутри чего находится 'id' чтобы под ответ не подходило содержимое 'rootNavnode', которое встречается в элементах непустых 'navnodes'
        self.assertFragmentIn(response, {
            'navnodes': [
                {
                    'id': EXPRESS_ROOT,
                    'navnodes': Absent(),
                },
            ]
        })

        # поход в комбинатор будет и при наличии в запросе складов еды
        response = self.cataloger.request_json(
            self._gen_request(region=213, gps=GPS_90, compress_eats_warehouses=300, depth=1),
            headers={'market_req_id': 'test_bad_gps_eda'},
        )
        # В логах должны появиться записи о проблеме с координатой GPS_90 (заодно проверим, что передается айдишник запроса)
        for req_id in ('test_bad_gps_no_warehouses', 'test_bad_gps_eda'):
            self.assertTrue(self.server.has_note_in_common_log('.*Empty\\ express\\ warehouses\\ from\\ combinator\\ call,\\ request_id\\ =\\ {}\\ gps\\ =\\ lat:90\\ lon:90'.format(req_id)))

    def test_no_gps_no_warehouses(self):
        response = self.cataloger.request_json(
            self._gen_request(region=213, depth=2),
            headers={'market_req_id': 'test_no_gps_no_warehouses'},
        )

        self.assertFragmentIn(response, {
            'id': EXPRESS_ROOT,
            'navnodes': NotEmptyList(),
        })

    def check_multi_warehouses(
        self,
        warehouses=None,
        compress_express_warehouses=None,
        gps=None,
        compress_eats_warehouses=None
    ):
        for new_express_logic in (None, False, True):
            response = self.cataloger.request_json(
                self._gen_request(
                    region=213,
                    warehouses=warehouses,
                    compress_express_warehouses=compress_express_warehouses,
                    gps=gps,
                    compress_eats_warehouses=compress_eats_warehouses,
                    new_express_logic=new_express_logic,
                )
            )
            self.assertFragmentIn(response, {
                'id': EXPRESS_ROOT,
                'navnodes': [
                    {
                        'id': 2000  # склад 100
                    },
                    {
                        'id': 3000  # склад 200
                    },
                    {
                        'id': 4000  # склад 200
                    },
                    {
                        'id': 6000  # склад 200
                    },
                ]
            }, allow_different_len=False, preserve_order=True)

    def test_multi_warehouses(self):
        self.check_multi_warehouses(warehouses=[100, 200])
        self.check_multi_warehouses(compress_express_warehouses=[100, 200])
        self.check_multi_warehouses(compress_eats_warehouses=[100, 200])
        self.check_multi_warehouses(gps=GPS_2)

        # Склады экспресса игнорируются, если передан основной склад
        self.check_multi_warehouses(warehouses=[100, 200], compress_express_warehouses=300)
        self.check_multi_warehouses(warehouses=[100, 200], compress_eats_warehouses=300)

        # склады в запросе приоритетнее gps
        self.check_multi_warehouses(warehouses=[100, 200], gps=GPS_90)

        # Склады экспресса и еды объединяются
        self.check_multi_warehouses(compress_eats_warehouses=100, compress_express_warehouses=200)
        self.check_multi_warehouses(compress_eats_warehouses=100, gps=GPS_1)

    @skip('Региональность в статистике убрали, но вдруг захотят вернуть')
    def test_regions(self):
        # Москва
        response = self.cataloger.request_json(self._gen_request(region=213, warehouses=100))
        self.assertFragmentIn(response, {
            'id': EXPRESS_ROOT,
            'navnodes': [
                {
                    'id': 2000
                },
            ]
        }, allow_different_len=False, preserve_order=True)

        # Питер
        response = self.cataloger.request_json(self._gen_request(region=2, warehouses=100))
        self.assertFragmentIn(response, {
            'id': EXPRESS_ROOT,
            'navnodes': [
                {
                    'id': 3000
                },
                {
                    'id': 5000  # поднялись из узла 5110, хотя экспресс поддепартамент и промежуточный узел не имеют хидов
                },
            ]
        }, allow_different_len=False, preserve_order=True)

        # Симферополь, отсутствует в статистике склада 100
        response = self.cataloger.request_json(self._gen_request(region=146, warehouses=100))
        self.assertFragmentIn(response, {'id': EXPRESS_ROOT, 'navnodes': Absent()})

        # Но есть в статистике склада 200
        response = self.cataloger.request_json(self._gen_request(region=146, warehouses=200))
        self.assertFragmentIn(response, {
            'id': EXPRESS_ROOT,
            'navnodes': [
                {
                    'id': 4000
                },
                {
                    'id': 6000
                }
            ]
        }, allow_different_len=False)

        # Невалидный регион
        response = self.cataloger.request_json(self._gen_request(region=100500, warehouses=100))
        self.assertFragmentIn(response, {
            'id': EXPRESS_ROOT,
            'navnodes': Absent()
        })

    def test_sub_nid(self):
        '''
        Проверяем отображение категорий продуктов в экспрессе (костыль для ШиШ Еды)
        '''
        response = self.cataloger.request_json(self._gen_request(region=213, warehouses=300, nid=EXPRESS_EATS_NID, depth=1))
        self.assertFragmentIn(response, {
            'id': EXPRESS_EATS_NID,
            'navnodes': [{
                'id': 1510,
                'navnodes': Absent(),
            }, {
                'id': 1520,     # Поднялся из 1600
                'navnodes': Absent(),
            }]
        }, allow_different_len=False)

        # На этом складе только одна подкатегория Продуктов
        response = self.cataloger.request_json(self._gen_request(region=213, warehouses=310, nid=EXPRESS_EATS_NID, depth=1))
        self.assertFragmentIn(response, {
            'id': EXPRESS_EATS_NID,
            'navnodes': [{
                'id': 1510,
                'navnodes': Absent(),
            }]
        }, allow_different_len=False)

        response = self.cataloger.request_json(self._gen_request(region=213, warehouses=310, nid=EXPRESS_EATS_NID, depth=1))
        self.assertFragmentIn(response, {
            'id': EXPRESS_EATS_NID,
            'navnodes': [{
                'id': 1510,
                'navnodes': Absent(),
            }]
        }, allow_different_len=False)

        response = self.cataloger.request_json(self._gen_request(region=213, warehouses=100, nid=EXPRESS_EATS_NID, depth=1))
        self.assertFragmentIn(response, {
            'id': EXPRESS_EATS_NID,
            'navnodes': Absent()
        }, allow_different_len=False)


if __name__ == '__main__':
    main()
