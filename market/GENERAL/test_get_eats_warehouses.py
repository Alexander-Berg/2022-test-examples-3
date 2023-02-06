#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.matcher import Absent, NotEmpty, Not, Equal
from core.types.combinator import CombinatorGpsCoords, CombinatorEatsWarehouse
from core.types import Shop, ExpressWarehouseWithCategories
from core.testcase import TestCase, main
from core.types import Region

EMPTY = "H4sIAAAAAAAAA2MAAI3vAtIBAAAA"

WH_EATS_1_1 = 101
WH_EATS_1_2 = 102

WH_EATS_2_1 = 201


def make_mock_rearr(**kwds):
    suffix = 'parallel_smm=1.0;ext_snippet=1;no_snippet_arc=1;market_enable_sins_offers_wizard=1'
    rearr = make_rearr(**kwds)
    if rearr != '':
        rearr += ';'
    return rearr + suffix


def make_rearr(**kwds):
    kvlist = ['{}={}'.format(key, kwds[key]) for key in kwds]
    kvlist.sort(key=lambda x: x[0])
    return ';'.join(kvlist)


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]
        cls.index.regiontree += [
            Region(rid=213, name='Москва'),
        ]

    @classmethod
    def prepare_get_eats_warehouses(cls):
        cls.combinatorExpress.on_eats_warehouses_request(
            region_id=213,
            gps_coords=CombinatorGpsCoords(55.7521, 37.6156),
            rear_factors=make_mock_rearr(a=1),
        ).respond_with_eats_warehouses(
            eats_warehouses=[
                CombinatorEatsWarehouse(
                    warehouse_id=WH_EATS_1_1,
                    priority=1,
                    shop_id=WH_EATS_1_1,
                    delivery_time_minutes=15,
                    min_order_price=555,
                ),
                CombinatorEatsWarehouse(
                    warehouse_id=WH_EATS_1_2,
                    priority=2,
                    shop_id=WH_EATS_1_2,
                    available_in_hours=40,
                    delivery_price_min=100,
                    delivery_price_max=300,
                    min_order_price=556,
                ),
                CombinatorEatsWarehouse(
                    warehouse_id=WH_EATS_2_1,
                    priority=1,
                    shop_id=WH_EATS_2_1,
                    delivery_time_minutes=25,
                    delivery_price_min=0,
                    delivery_price_max=500,
                    free_delivery_threshold=3000,
                    min_order_price=557,
                ),
            ],
            currency="RUR",
        )

        cls.index.shops += [
            Shop(fesh=WH_EATS_1_1, datafeed_id=WH_EATS_1_1, warehouse_id=WH_EATS_1_1, business_fesh=1),
            Shop(fesh=WH_EATS_1_2, datafeed_id=WH_EATS_1_2, warehouse_id=WH_EATS_1_2, business_fesh=1),
            Shop(fesh=WH_EATS_2_1, datafeed_id=WH_EATS_2_1, warehouse_id=WH_EATS_2_1, business_fesh=2),
        ]

        cls.index.warehouses_express_categories += [
            ExpressWarehouseWithCategories(warehouse_id=WH_EATS_1_1, categories=[123, 456]),
            ExpressWarehouseWithCategories(warehouse_id=WH_EATS_1_2, categories=[456, 789]),
        ]

    def test_eats_warehouses(self):
        '''
        Проверяем получение списка складов еды и отображение его в расшифрованном виде
        '''
        response = self.report.request_json(
            'place=get_eats_warehouses&rids={region}&gps=lat:{lat};lon:{lon}&rearr-factors={rearr}'.format(
                region=213, lat=55.7521, lon=37.6156, rearr=make_rearr(a=1)
            )
        )
        self.assertFragmentIn(response, {"expressWarehouses": {"compressed": NotEmpty()}})
        self.assertFragmentIn(response, {"expressWarehouses": {"compressed": Not(Equal(EMPTY))}})
        compressed_wh = response["expressWarehouses"]["compressed"]

        wh_1_1_response = {
            "warehouse_id": WH_EATS_1_1,
            "priority": 1,
            "business_id": 1,
            "delivery_time_minutes": 15,
            "delivery_price_min": Absent(),
            "delivery_price_max": Absent(),
            "free_delivery_threshold": Absent(),
            "available_in_hours": Absent(),
            "min_order_price": 555,
        }
        wh_1_2_response = {
            "warehouse_id": WH_EATS_1_2,
            "priority": 2,
            "business_id": 1,
            "delivery_time_minutes": Absent(),
            "delivery_price_min": 100,
            "delivery_price_max": 300,
            "free_delivery_threshold": Absent(),
            "available_in_hours": 40,
            "min_order_price": 556,
        }
        wh_2_1_response = {
            "warehouse_id": WH_EATS_2_1,
            "priority": 1,
            "business_id": 2,
            "delivery_time_minutes": 25,
            # "delivery_price_min": 0, # Число 0 не отображается с помощью дебажной выдачи proto3
            "delivery_price_max": 500,
            "free_delivery_threshold": 3000,
            "available_in_hours": Absent(),
            "min_order_price": 557,
        }

        # Все склады
        response = self.report.request_json(
            'place=parse_wh_compressed&rearr-factors=market_filter_express_warehouses=0&eats-warehouses-compressed={}'.format(
                compressed_wh
            )
        )
        self.assertFragmentIn(
            response,
            {
                "eats_warehouses": [
                    wh_1_1_response,
                    wh_1_2_response,
                    wh_2_1_response,
                ],
                "currency": "RUR",
            },
            allow_different_len=False,
        )

        # Склады только первого бизнеса
        response = self.report.request_json(
            'place=parse_wh_compressed&rearr-factors=market_filter_express_warehouses=0&eats-warehouses-compressed={}&business-id=1'.format(
                compressed_wh
            )
        )
        self.assertFragmentIn(
            response,
            {
                "eats_warehouses": [
                    wh_1_1_response,
                    wh_1_2_response,
                ],
                "currency": "RUR",
            },
            allow_different_len=False,
        )

        # Несколько бизнесов
        response = self.report.request_json(
            'place=parse_wh_compressed&rearr-factors=market_filter_express_warehouses=0&eats-warehouses-compressed={}&business-id=1,2'.format(
                compressed_wh
            )
        )
        self.assertFragmentIn(
            response,
            {
                "eats_warehouses": [
                    wh_1_1_response,
                    wh_1_2_response,
                    wh_2_1_response,
                ],
                "currency": "RUR",
            },
            allow_different_len=False,
        )

        # Склады не существующего бизнеса
        response = self.report.request_json(
            'place=parse_wh_compressed&rearr-factors=market_filter_express_warehouses=0&eats-warehouses-compressed={}&business-id=3'.format(
                compressed_wh
            )
        )
        self.assertFragmentIn(response, {"eats_warehouses": Absent(), "currency": "RUR"}, allow_different_len=False)

        # По одному складу от бизнеса
        response = self.report.request_json(
            'place=parse_wh_compressed&rearr-factors=market_filter_express_warehouses=0&eats-warehouses-compressed={}&warehouses-per-business=1'.format(
                compressed_wh
            )
        )
        self.assertFragmentIn(
            response,
            {
                "eats_warehouses": [
                    wh_1_1_response,
                    wh_2_1_response,
                ],
                "currency": "RUR",
            },
            allow_different_len=False,
        )

        # По два склада от бизнеса
        response = self.report.request_json(
            'place=parse_wh_compressed&rearr-factors=market_filter_express_warehouses=0&eats-warehouses-compressed={}&warehouses-per-business=2'.format(
                compressed_wh
            )
        )
        self.assertFragmentIn(
            response,
            {
                "eats_warehouses": [
                    wh_1_1_response,
                    wh_1_2_response,
                    wh_2_1_response,
                ],
                "currency": "RUR",
            },
            allow_different_len=False,
        )

        # Без ограничения количе складов от бизнеса
        response = self.report.request_json(
            'place=parse_wh_compressed&rearr-factors=market_filter_express_warehouses=0&eats-warehouses-compressed={}&warehouses-per-business=0'.format(
                compressed_wh
            )
        )
        self.assertFragmentIn(
            response,
            {
                "eats_warehouses": [
                    wh_1_1_response,
                    wh_1_2_response,
                    wh_2_1_response,
                ],
                "currency": "RUR",
            },
            allow_different_len=False,
        )

    def test_filter_express_warehouses(self):
        '''
        Проверяем фильтрацию экспресс складов по файлу статистики warehouses_express_categories.csv
        '''
        response = self.report.request_json(
            'place=get_eats_warehouses&rids={region}&gps=lat:{lat};lon:{lon}&rearr-factors={rearr}'.format(
                region=213, lat=55.7521, lon=37.6156, rearr=make_rearr(a=1)
            )
        )
        compressed_wh = response["expressWarehouses"]["compressed"]

        # с реарр флагом
        response = self.report.request_json(
            'place=parse_wh_compressed&rearr-factors=market_filter_express_warehouses=1&eats-warehouses-compressed={}'.format(
                compressed_wh
            )
        )
        self.assertFragmentIn(
            response,
            {
                "eats_warehouses": [
                    {"warehouse_id": WH_EATS_1_1},
                    {"warehouse_id": WH_EATS_1_2},
                ],
                "currency": "RUR",
            },
            allow_different_len=False,
        )
        self.assertFragmentNotIn(
            response,
            {
                "eats_warehouses": [
                    {"warehouse_id": WH_EATS_2_1},
                ],
                "currency": "RUR",
            },
        )

        # без реарр флага
        response = self.report.request_json(
            'place=parse_wh_compressed&rearr-factors=market_filter_express_warehouses=0&eats-warehouses-compressed={}'.format(
                compressed_wh
            )
        )
        self.assertFragmentIn(
            response,
            {
                "eats_warehouses": [
                    {"warehouse_id": WH_EATS_1_1},
                    {"warehouse_id": WH_EATS_1_2},
                    {"warehouse_id": WH_EATS_2_1},
                ],
                "currency": "RUR",
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_shops(cls):
        cls.index.shops += [
            Shop(11301115, warehouse_id=61017),
            Shop(11301151, warehouse_id=61055),
            Shop(11428055, warehouse_id=61166),
        ]


if __name__ == '__main__':
    main()
