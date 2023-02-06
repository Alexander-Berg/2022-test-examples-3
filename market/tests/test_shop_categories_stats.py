# -*- coding: utf-8 -*-

import json
from context import StatsCalcBaseTestCase
from market.idx.pylibrary.offer_flags.flags import OfferFlags
from utils import string_regions_to_list


def generate_record(shop_id, category_id, regions, geo_regions=None,
                    supplier_id=None, supplier_type=None, contex_info=None,
                    is_cpa=False, disabled_flags=None, flags=None):
    gl_record = dict()
    gl_record["shop_id"] = shop_id
    if supplier_id is not None:
        gl_record['supplier_id'] = supplier_id
    if supplier_type is not None:
        gl_record['supplier_type'] = supplier_type
    gl_record["category_id"] = category_id
    gl_record["regions"] = regions
    gl_record["int_regions"] = string_regions_to_list(regions)
    gl_record["geo_regions"] = geo_regions
    if geo_regions is not None:
        gl_record["int_geo_regions"] = string_regions_to_list(geo_regions)
    gl_record["delivery_flag"] = True
    if contex_info is not None:
        gl_record['contex_info'] = contex_info
    if disabled_flags is not None:
        gl_record["disabled_flags"] = disabled_flags
    gl_record["cpa"] = 4 if is_cpa else 0
    if flags:
        gl_record["flags"] = flags
    return gl_record


class TestShopCategoriesStats(StatsCalcBaseTestCase):
    def test_shop_regional_categories_stats(self):
        file_path = self.tmp_file_path('shop_regional_categories.csv')
        file_path_cpa = self.tmp_file_path('shop_regional_cpa_categories.csv')

        gl_records = [
            generate_record(1, 10, '213', is_cpa=True),
            generate_record(1, 10, '2', is_cpa=True),
            generate_record(1, 20, '1', is_cpa=True),
            generate_record(1, 20, '225', is_cpa=True),
            generate_record(1, 30, '1', is_cpa=True),
            generate_record(1, 30, '', geo_regions='17 3', is_cpa=True),
            generate_record(1, 30, '138', is_cpa=True),
            generate_record(2, 90801, '213', is_cpa=True),
            generate_record(2, 10, '2', is_cpa=True),
            generate_record(2, 20, '2', is_cpa=True),
            generate_record(2, 40, '', geo_regions='225', is_cpa=True),
            generate_record(2, 50, '', geo_regions='111', is_cpa=True),
            generate_record(2, 70, '', geo_regions='2', is_cpa=True, disabled_flags=1),
            generate_record(2, 80, '', geo_regions='213', is_cpa=True),
            generate_record(2, 80, '213', is_cpa=True),
            generate_record(3, 10, '', geo_regions=''),
            generate_record(4, 10, '', geo_regions='1'),
            generate_record(4, 20, '', geo_regions='1'),
            generate_record(4, 10, '', geo_regions='3'),
            generate_record(1, 10, '213', contex_info={'original_msku_id': 100}),
            generate_record(1, 10, '213', flags=OfferFlags.IS_DIRECT.value),
        ]

        self.run_stats_calc(
            'ShopRegionalCategoriesStats',
            json.dumps(gl_records)
        )
        # cpa оффера только у магазинов 1 и 2
        result_common = [
            '',
            '1\t10\t20\t30\t|\t-1',
            '1\t10\t20\t|\t2',
            '1\t20\t|\t10174',
            '1\t10\t20\t30\t|\t213',
            '1\t20\t30\t|\t3',
            '1\t20\t30\t|\t17',
            '1\t20\t|\t225',
            '1\t30\t|\t138',
            '2\t80\t90801\t|\t213',
            '2\t|\t3',
            '2\t|\t17',
            '2\t40\t|\t225',
        ]
        result = result_common + [
            '2\t10\t20\t|\t20281',
            '2\t10\t20\t70\t|\t2',
            '2\t10\t20\t40\t50\t70\t80\t90801\t|\t-1',
            '4\t10\t20\t|\t-1',
            '4\t|\t213',
            '4\t10\t20\t|\t1',
            '4\t10\t|\t3',
        ]
        # В cpa статистике не учитываются отключенные оффера, а оффер магазина 2 в категории 70 отключен.
        # Региона 20281 нет, т.к он является подрегионом Питера, и список категорий совпадает
        result_cpa = result_common + [
            '2\t10\t20\t|\t2',
            '2\t10\t20\t40\t50\t80\t90801\t|\t-1',
        ]

        self.assertEqual(sorted(result), sorted(open(file_path).read().split('\n')))
        self.assertEqual(sorted(result_cpa), sorted(open(file_path_cpa).read().split('\n')))

    def test_supplier_regional_categories_stats(self):
        gl_records = [
            generate_record(5, 10, '213', is_cpa=True, supplier_id=100501),  # supplier_type не известен, не попадет в статистику
            generate_record(6, 10, '213', is_cpa=True, supplier_id=100502, supplier_type=1),  # supplier_type не равен 3, не попадет в статистику
            generate_record(7, 10, '213', is_cpa=True, supplier_id=100503, supplier_type=3, disabled_flags=0),
            generate_record(7, 20, '2', is_cpa=True, supplier_id=100503, supplier_type=3),
            generate_record(8, 20, '2', is_cpa=True, supplier_id=100504, supplier_type=3),
            generate_record(8, 20, '138', is_cpa=True, supplier_id=100504, supplier_type=3),
            generate_record(9, 30, '213', is_cpa=True, supplier_id=100505, supplier_type=3, disabled_flags=1),  # отключенный оффер
            generate_record(9, 30, '213', is_cpa=True, supplier_id=100505, supplier_type=3, flags=OfferFlags.IS_DIRECT.value),  # оффер директа
            generate_record(10, 40, '213', is_cpa=False, supplier_id=100506),
        ]

        self.run_stats_calc('ShopRegionalCategoriesStats', json.dumps(gl_records))
        # У магазина 9/поставщика 100505 единственный оффер отключен. В статистику поставщиков он не попадает, а встатистику магазинов должен
        # У магазина 10/поставщика 100506 единственный оффер не СРА. В статистику поставщиков он не попадает, а встатистику магазинов должен
        supplier_result = [  # только в статистике поставщиков есть группировка регионов
            '',
            '100503\t10\t|\t213',
            '100503\t20\t|\t2',
            '100503\t10\t20\t|\t-1',
            '100504\t20\t|\t2\t138\t-1',
        ]
        shop_cpa_result = [
            '',
            '5\t10\t|\t-1',
            '5\t10\t|\t213',
            '6\t10\t|\t-1',
            '6\t10\t|\t213',
            '7\t10\t20\t|\t-1',
            '7\t10\t|\t213',
            '7\t20\t|\t2',
            '8\t20\t|\t-1',
            '8\t20\t|\t138',
            '8\t20\t|\t2',
        ]
        shop_result = shop_cpa_result + [
            '9\t30\t|\t-1',
            '9\t30\t|\t213',
            '10\t40\t|\t-1',
            '10\t40\t|\t213',
        ]
        supplier_path = self.tmp_file_path('supplier_regional_categories.csv')
        shop_path = self.tmp_file_path('shop_regional_categories.csv')
        shop_cpa_path = self.tmp_file_path('shop_regional_cpa_categories.csv')

        self.assertEqual(sorted(supplier_result), sorted(open(supplier_path).read().split('\n')))
        self.assertEqual(sorted(shop_result), sorted(open(shop_path).read().split('\n')))
        self.assertEqual(sorted(shop_cpa_result), sorted(open(shop_cpa_path).read().split('\n')))

    def atest_shop_popular_categories_stats(self):
        gl_records = [
            # 1 оффер в категории 10
            generate_record(1, 10, '213', is_cpa=True),
            # 2 оффера в категории 20 (регион не важен)
            generate_record(1, 20, '213', is_cpa=True),
            generate_record(1, 20, '2', is_cpa=True),
            # 3 оффера в категории 30
            generate_record(1, 30, '213', is_cpa=True),
            generate_record(1, 30, '2', is_cpa=True),
            generate_record(1, 30, '3', is_cpa=True),

            # у второго магазина 2 оффера в категории 10 и 3 в категории 20,
            # но из них только 1 сра
            generate_record(2, 10, '213', is_cpa=True),
            generate_record(2, 10, '213', is_cpa=True),
            generate_record(2, 20, '213', is_cpa=True),
            generate_record(2, 20, '213', is_cpa=False),
            generate_record(2, 20, '213', is_cpa=False),

            # у магазина 3 нет сра офферов, его не должно быть в статистике
            generate_record(3, 10, '213', is_cpa=False),

            # магазин 4 должен добавиться и как магазин, и как поставщик
            generate_record(4, 10, '213', is_cpa=True, supplier_id=40),
        ]

        self.run_stats_calc('ShopRegionalCategoriesStats', json.dumps(gl_records))
        result = [
            '',
            '1\t30\t20\t10\t|\t-1',
            '2\t10\t20\t|\t-1',
            '4\t10\t|\t-1',
            '40\t10\t|\t-1',
        ]
        filepath = self.tmp_file_path('shop_popular_cpa_categories.csv')
        self.assertEqual(sorted(result), sorted(open(filepath).read().split('\n')))
