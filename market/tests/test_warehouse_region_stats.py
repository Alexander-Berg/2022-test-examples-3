# -*- coding: utf-8 -*-

import json

from context import StatsCalcBaseTestCase
from market.idx.pylibrary.offer_flags.flags import DisabledFlags, OfferFlags


class TestWarehouseRegionStats(StatsCalcBaseTestCase):
    def setUp(self):
        super(TestWarehouseRegionStats, self).setUp()
        warehouse1 = 141
        warehouse2 = 142
        warehouse3 = 143
        retail_warehouse = 241
        restaurants_warehouse = 242
        lavka_warehouse = 243
        category1 = 91491
        category2 = 91492
        category3 = 91493

        zeroValue = 0

        self.gls = [
            {
                'warehouse_id': warehouse1,
                'category_id': category2,
                'is_express': True,
                'has_gone': False,
            },

            {
                'warehouse_id': warehouse3,
                'category_id': category3,
                'is_express': True,
                'has_gone': False,
            },

            {
                'warehouse_id': warehouse1,
                'category_id': category3,
                'is_express': True,
                'has_gone': False,
            },
            {
                'warehouse_id': warehouse1,
                'category_id': category3,
                'is_express': False,
                'has_gone': False,
            },
            {
                'warehouse_id': warehouse2,
                'category_id': category2,
                'is_express': True,
                'has_gone': False,
            },
            {
                'warehouse_id': warehouse1,
                'category_id': category1,
                'is_express': True,
                'has_gone': False,
            },
            {
                'warehouse_id': warehouse1,
                'category_id': category3,
                'is_express': True,
                'has_gone': False,
            },
            {
                'warehouse_id': warehouse1,
                'category_id': category2,
                'is_express': True,
                'has_gone': False,
            },
            {
                'warehouse_id': warehouse2,
                'category_id': category1,
                'is_express': False,
                'has_gone': False,
            },

            {
                'warehouse_id': zeroValue,
                'category_id': category1,
                'is_express': True,
                'has_gone': False,
            },

            {
                'warehouse_id': warehouse1,
                'category_id': zeroValue,
                'is_express': True,
                'has_gone': False,
            },

            {
                'warehouse_id': warehouse3,
                'category_id': category2,
                'is_express': True,
                'disabled_by_dynamic': True,
                'disabled_by_price_limit': False,
                'has_gone': False,
            },

            {
                'warehouse_id': warehouse3,
                'category_id': category1,
                'is_express': True,
                'disabled_by_dynamic': False,
                'disabled_by_price_limit': True,
                'has_gone': False,
            },

            {
                'warehouse_id': warehouse3,
                'category_id': category1,
                'is_express': True,
                'disabled_by_dynamic': False,
                'disabled_by_price_limit': False,
                'has_gone': True,
            },

            {
                'warehouse_id': warehouse3,
                'category_id': category1,
                'is_express': True,
                'disabled_by_dynamic': False,
                'disabled_by_price_limit': False,
                'has_gone': False,
                'disabled_flags' : DisabledFlags.MARKET_STOCK.value,
            },

            {
                'warehouse_id': retail_warehouse,
                'category_id': category1,
                'is_express': True,
                'has_gone': False,
                'flags': OfferFlags.IS_EDA_RETAIL.value,                    # Этот тип оферов разрешен для статистики по складам
            },

            {
                'warehouse_id': restaurants_warehouse,
                'category_id': category1,
                'is_express': True,
                'has_gone': False,
                'flags': OfferFlags.IS_EDA_RESTAURANTS.value,               # Оферы ресторанов запрещены
            },

            {
                'warehouse_id': lavka_warehouse,
                'category_id': category1,
                'is_express': True,
                'has_gone': False,
                'flags': OfferFlags.IS_LAVKA.value,                         # Оферы Лавки запрещены
            },
        ]

    def test_warehouse_region_stats(self):
        self.run_stats_calc(
            'ExpressWarehouseCategoryStats',
            json.dumps(self.gls)
        )

        file_path = self.tmp_file_path('warehouses_express_categories.csv')

        expected = {
            '141\t91491\t91492\t91493\t|\t0\n',
            '142\t91492\t|\t0\n',
            '143\t91493\t|\t0\n',
            '241\t91491\t|\t0\n'
        }

        with open(file_path, 'r') as input:
            got = set(input.readlines())
            self.assertEqual(expected, got)
