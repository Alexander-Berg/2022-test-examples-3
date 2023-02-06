#!/usr/bin/env python
# -*- coding: utf-8 -*-

import json
import random
import itertools
from copy import deepcopy
from utils import string_regions_to_list
import yt.yson as yson

from context import StatsCalcBaseTestCase
from market.idx.pylibrary.offer_flags.flags import DisabledFlags, OfferFlags


RGB_GREEN = 'green'
RGB_BLUE = 'blue'

# it is used several times throughout the test
header = (
    'C\tKZT\t0\tRUR\t1\tBYN\t2\tUAH\t3\n'
    '@\t\tNOFFERS\tNRETAILERS\tONSTOCK\tBID1\tDC_COUNT\tEQUAL\t'
    'MAX_DISCOUNT\tHAS_PRICE_FROM\tMIN_DELIVERY_INTERVAL\t'
    'MIN_FREE_DELIVERY_INTERVAL\tPROMO_COUNT\tWHITE_PROMO_COUNT\tPROMO_TYPES\t'
    'MODEL_TYPE\tFULFILLMENT_LIGHT\tHAS_CPA\tNOFFER_COLOR_GLOB\tNOFFER_COLOR_VENDOR\tVALID_DC_COUNT\tMAX_VALID_DISCOUNT\t'
    'CUTPRICE_COUNT\tN_RECOM_VENDOR_RETAILERS\t'
    'MEDIAN_PRICE:KZT\tMAX_PRICE:KZT\tMIN_PRICE:KZT\tMIN_OLDPRICE:KZT\tMIN_VALID_OLDPRICE:KZT\tMIN_CUTPRICE:KZT\t'
    'MEDIAN_PRICE:RUR\tMAX_PRICE:RUR\tMIN_PRICE:RUR\tMIN_OLDPRICE:RUR\tMIN_VALID_OLDPRICE:RUR\tMIN_CUTPRICE:RUR\t'
    'MEDIAN_PRICE:BYN\tMAX_PRICE:BYN\tMIN_PRICE:BYN\tMIN_OLDPRICE:BYN\tMIN_VALID_OLDPRICE:BYN\tMIN_CUTPRICE:BYN\t'
    'MEDIAN_PRICE:UAH\tMAX_PRICE:UAH\tMIN_PRICE:UAH\tMIN_OLDPRICE:UAH\tMIN_VALID_OLDPRICE:UAH\tMIN_CUTPRICE:UAH\n'
)

# common
record = dict(
    model_id=100,
    regions='2;166;',
    int_regions=[yson.YsonUint64(2), yson.YsonUint64(166)],
    binary_price='122 1 0 RUR RUR',
    geo_regions='213',
    int_geo_regions=[yson.YsonUint64(213)],
    category_id=7814999,
    binary_ware_md5='8133e7d175260f06c09646ba555f6479',
    contex_info={
        'experiment_id': 'green',
    },
)


# main expected numbers for a good run of ***RegionalStat for one record as below without oldprice
def result_for_regional_stats(model_type):
    return (
        '\t1\t1\t0\t0\t0\t1'
        '\t0\t0\t0'
        '\t0\t0\t0\t0\t'
        '%s\t0\t0\t0\t0\t0\t0\t0\t0\t'
        '601\t601.460022\t601.460022\t601.460022\t601.460022\t0\t'
        '122\t122\t122\t122\t122\t0\t'
        '3.416\t3.416\t3.416\t3.416\t3.416\t0\t'
        '41\t40.959061\t40.959061\t40.959061\t40.959061\t0\t'
        '|\t-1\t213\n'
    ) % model_type


classifier_id = "ad1d66153519254f804f33eda7868cbd"

# for this cluster_id there are no required picture: see cluster_pictures.pbuf.sn
bad_cluster_id = 1005899581
good_cluster_id = 1005899582


class TestUtils(StatsCalcBaseTestCase):
    def test_model_regional_stats(self):
        file_path = self.tmp_file_path('model_region_stats.csv')
        gl_record = deepcopy(record)
        gl_record["classifier_magic_id"] = classifier_id
        self.run_stats_calc('GroupRegionalStats',  json.dumps([gl_record]))
        result = header + 'a\t100' + result_for_regional_stats(0)
        self.assertEqual(result, open(file_path).read())

    def test_model_regional_stats_complex(self):
        # check new columns regarding with Sales
        file_path = self.tmp_file_path('model_region_stats.csv')

        gl_record0 = deepcopy(record)
        gl_record0["classifier_magic_id"] = classifier_id
        gl_record0['contex_info']['original_msku_id'] = 100  # this record must be filtered

        gl_record1 = deepcopy(record)
        gl_record1["classifier_magic_id"] = classifier_id
        gl_record1["binary_price"] = '122 1 0 RUR RUR'
        gl_record1["binary_oldprice"] = '140 1 0 RUR RUR'
        gl_record1["promo_type"] = 4
        gl_record1["cpa"] = 4  # REAL

        gl_record2 = deepcopy(record)
        gl_record2["classifier_magic_id"] = classifier_id
        gl_record2["binary_price"] = '130 1 0 RUR RUR'
        gl_record2["binary_oldprice"] = '150 1 0 RUR RUR'
        gl_record2["shop_id"] = 10270932
        gl_record2["is_recommended_by_vendor"] = True

        # filtered direct offer
        gl_record_direct = deepcopy(record)
        gl_record_direct["flags"] = OfferFlags.IS_DIRECT.value

        # filtered Lavka offer
        gl_record_lavka = deepcopy(record)
        gl_record_lavka["flags"] = OfferFlags.IS_LAVKA.value

        # filtered Eda offer
        gl_record_eda = deepcopy(record)
        gl_record_eda["flags"] = OfferFlags.IS_EDA_RESTAURANTS.value

        # filtered mediaservices offer
        gl_record_mediaservices_testing = deepcopy(record)
        gl_record_mediaservices_testing["supplier_id"] = 11317159

        gl_record_mediaservices_prod = deepcopy(record)
        gl_record_mediaservices_prod["supplier_id"] = 2476913

        for gl_record in (gl_record_direct, gl_record_lavka, gl_record_eda, gl_record_mediaservices_testing, gl_record_mediaservices_prod):
            gl_record["binary_price"] = '1300 1 0 RUR RUR'
            gl_record["binary_oldprice"] = '1500 1 0 RUR RUR'
            gl_record["shop_id"] = 100500

        self.run_stats_calc(
            'GroupRegionalStats',
            json.dumps([
                gl_record0, gl_record1, gl_record2, gl_record_direct, gl_record_lavka, gl_record_eda, gl_record_mediaservices_testing, gl_record_mediaservices_prod
            ])
        )

        # MAX_DISCOUNT  for RUR: 1) (122 - 140) / 140 = 12.8 %   2) (150 - 130) / 150 = 13.333 %  The second is greater
        # MIN_OLDPRICE  for RUR: 140 is less than 150 so 140 is expected

        def expected(model_type):
            return (
                '\t2\t2\t0\t0\t2\t0'
                '\t13\t0\t0'
                '\t0\t1\t0\t4\t'
                '%s\t0\t1\t0\t0\t0\t0\t0\t1\t'
                '621\t640.900024\t601.460022\t690.200012\t601.460022\t0\t'
                '126\t130\t122\t140\t122\t0\t'
                '3.528\t3.64\t3.416\t3.92\t3.416\t0\t'
                '42\t43.644901\t40.959061\t47.002201\t40.959061\t0\t'
                '|\t-1\t213\n'
            ) % model_type

        self.assertEqual(header + 'a\t100' + expected(0), open(file_path).read())

        # testing clusters
        file_path = self.tmp_file_path('group_region_stats.csv')
        gl_record1["cluster_id"] = gl_record2["cluster_id"] = gl_record0["cluster_id"] = good_cluster_id
        gl_record1["model_id"] = gl_record2["model_id"] = 0
        gl_record1["promo_type"] = 4
        self.run_stats_calc(
            'GroupRegionalStats',
            json.dumps([gl_record0, gl_record1, gl_record2])
        )
        self.assertEqual(header + 'a\t1005899582' + expected(1), open(file_path).read())

        # MARKETINDEXER-19477 models with buybox, but without MODEL_COLOR_WHITE should be filtered out

        # Should remain
        gl_record2['is_buyboxes'] = True
        gl_record2['flags'] = OfferFlags.MODEL_COLOR_WHITE.value

        # Should be filtered
        gl_record3 = deepcopy(gl_record2)
        gl_record3['flags'] = 0
        gl_record3["binary_price"] = '1 1 0 RUR RUR'  # It should 100% fail test if not filtered
        gl_record3["binary_oldprice"] = '1 1 0 RUR RUR'

        # Same check as above
        self.run_stats_calc(
            'GroupRegionalStats',
            json.dumps([gl_record0, gl_record1, gl_record2, gl_record3, gl_record_direct])
        )
        self.assertEqual(header + 'a\t1005899582' + expected(1), open(file_path).read())

        # check 'DC_COUNT' -> expected '2', check 'MAX_DISCOUNT' should be 13%, EQUAL should be '1'

        # testing clusters
        gl_record2 = deepcopy(gl_record1)
        self.run_stats_calc('GroupRegionalStats', json.dumps([gl_record1, gl_record2]))
        self.assertEqual('2', open(file_path).readlines()[2].split()[6])
        self.assertEqual('1', open(file_path).readlines()[2].split()[7])
        self.assertEqual('13', open(file_path).readlines()[2].split()[8])

        # check DC_COUNT, expected '1', there are two offers whose price is equal to MIN_PRICE, only one has onlprice but

        del gl_record2["binary_oldprice"]
        self.run_stats_calc(
            'GroupRegionalStats',
            json.dumps([gl_record1, gl_record2, gl_record_direct])
        )
        self.assertEqual('1', open(file_path).readlines()[2].split()[6])

        gl_record2['feed_id'] = 1000000003  # interacts with schedule.txt
        self.run_stats_calc(
            'GroupRegionalStats',
            json.dumps([gl_record2, gl_record_direct])
        )
        self.assertEqual(3, len(open(file_path).readlines()))

        file_path = self.tmp_file_path('model_region_stats.csv')
        self.run_stats_calc(
            'GroupRegionalStats',
            json.dumps([gl_record2, gl_record_direct])
        )
        self.assertEqual(2, len(open(file_path).readlines()))

        gl_record2.clear()

        gl_record3 = deepcopy(record)
        gl_record4 = deepcopy(record)
        gl_record3["classifier_magic_id"] = gl_record4["classifier_magic_id"] = classifier_id
        gl_record3["binary_price"] = '100 ECB 0 EUR RUR'
        gl_record3["binary_oldprice"] = '200 ECB 0 EUR RUR'
        gl_record3["binary_allowed_oldprice"] = '100 ECB 0 EUR RUR'  # invalid discount
        gl_record4["binary_price"] = '7000 CBRF 0 RUR RUR'
        gl_record4["binary_oldprice"] = '8000 CBRF 0 RUR RUR'
        gl_record4["binary_allowed_oldprice"] = '7000 CBRF 0 RUR RUR'  # invalid discount
        expected = (
            '\t2\t1\t0\t0\t2\t0'
            '\t50\t0\t0'
            '\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t'
            '34862\t35214.285156\t34510\t39440\t34510\t0\t'
            '7071\t7142.856934\t7000\t8000\t7000\t0\t'
            '198\t200\t196\t224\t196\t0\t'
            '2374\t2398.071289\t2350.110107\t2685.840088\t2350.110107\t0\t'
            '|\t-1\t213\n'
        )
        self.run_stats_calc(
            'GroupRegionalStats',
            json.dumps([gl_record3, gl_record4, gl_record_direct])
        )
        self.assertEqual(header + 'a\t100' + expected, open(file_path).read())

        # MRS with valid discounts
        gl_record3 = deepcopy(record)
        gl_record4 = deepcopy(record)
        gl_record3["classifier_magic_id"] = gl_record4["classifier_magic_id"] = classifier_id
        gl_record3["binary_price"] = '100 ECB 0 EUR RUR'
        gl_record3["binary_oldprice"] = '200 ECB 0 EUR RUR'
        gl_record3["binary_allowed_oldprice"] = '300 ECB 0 EUR RUR'  # valid discount
        gl_record4["binary_price"] = '7000 CBRF 0 RUR RUR'
        gl_record4["binary_oldprice"] = '8000 CBRF 0 RUR RUR'
        gl_record4["binary_allowed_oldprice"] = '8000 CBRF 0 RUR RUR'  # valid discount
        expected = (
            '\t2\t1\t0\t0\t2\t0\t50\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t2\t50\t0\t0\t'
            '34862\t35214.285156\t34510\t39440\t39440\t0\t'
            '7071\t7142.856934\t7000\t8000\t8000\t0\t'
            '198\t200\t196\t224\t224\t0\t'
            '2374\t2398.071289\t2350.110107\t2685.840088\t2685.840088\t0\t'
            '|\t-1\t213\n'
        )
        self.run_stats_calc(
            'GroupRegionalStats',
            json.dumps([gl_record3, gl_record4, gl_record_direct, gl_record_direct])
        )
        self.assertEqual(header + 'a\t100' + expected, open(file_path).read())

        # MRS with promos
        gl_record5 = deepcopy(record)
        gl_record6 = deepcopy(record)
        gl_record7 = deepcopy(record)
        gl_record8 = deepcopy(record)
        gl_record5["promo_type"] = 1 << 0  # NPlusM
        gl_record6["promo_type"] = 1 << 1  # GiftWithPurchase
        gl_record7["promo_type"] = 1 << 3  # SecondOfferDiscount
        gl_record8["promo_type"] = 1 << 4  # SecondOfferForFixedPrice
        expected = (
            '\t4\t1\t0\t0\t0\t1\t0\t0\t0\t0\t4\t2\t27\t0\t0\t0\t0\t0\t0\t0\t0\t0\t'
            '601\t601.460022\t601.460022\t601.460022\t601.460022\t0\t'
            '122\t122\t122\t122\t122\t0\t'
            '3.416\t3.416\t3.416\t3.416\t3.416\t0\t'
            '41\t40.959061\t40.959061\t40.959061\t40.959061\t0\t'
            '|\t-1\t213\n'
        )
        self.run_stats_calc(
            'GroupRegionalStats',
            json.dumps([gl_record5, gl_record6, gl_record7, gl_record8, gl_record_direct])
        )
        self.assertEqual(header + 'a\t100' + expected, open(file_path).read())

        expected = (
            '\t3\t1\t0\t0\t0\t1\t0\t0\t0\t0\t3\t1\t26\t0\t0\t0\t0\t0\t0\t0\t0\t0\t'
            '601\t601.460022\t601.460022\t601.460022\t601.460022\t0\t'
            '122\t122\t122\t122\t122\t0\t'
            '3.416\t3.416\t3.416\t3.416\t3.416\t0\t'
            '41\t40.959061\t40.959061\t40.959061\t40.959061\t0\t'
            '|\t-1\t213\n'
        )
        self.run_stats_calc(
            'GroupRegionalStats',
            json.dumps([gl_record6, gl_record7, gl_record8, gl_record_direct])
        )

        self.assertEqual(header + 'a\t100' + expected, open(file_path).read())

    def test_model_regional_stats_buybox(self):
        file_path = self.tmp_file_path('model_region_stats.csv')
        gl_record = deepcopy(record)
        gl_record["classifier_magic_id"] = classifier_id
        # filtered direct offer
        gl_record_direct = deepcopy(record)
        gl_record_direct['binary_price'] = '1300 1 0 RUR RUR'
        gl_record_direct['binary_oldprice'] = '1500 1 0 RUR RUR'
        gl_record_direct['shop_id'] = 100500
        gl_record_direct['flags'] = OfferFlags.IS_DIRECT.value
        self.run_stats_calc(
            'GroupRegionalStats',
            json.dumps([gl_record, gl_record_direct])
        )
        result = header + 'a\t100' + result_for_regional_stats(0)
        self.assertEqual(result, open(file_path).read())

        # check new columns regarding with Sales

        gl_record1 = deepcopy(record)
        gl_record2 = deepcopy(record)
        gl_record1["classifier_magic_id"] = gl_record2["classifier_magic_id"] = classifier_id
        gl_record1["binary_price"] = '122 1 0 RUR RUR'
        gl_record1["binary_oldprice"] = '140 1 0 RUR RUR'
        gl_record1["promo_type"] = 4
        gl_record2["binary_price"] = '130 1 0 RUR RUR'
        gl_record2["binary_white_oldprice"] = '150 1 0 RUR RUR'
        gl_record2["shop_id"] = 10
        gl_record2["is_blue_offer"] = True
        gl_record2["is_buyboxes"] = True
        # filtered direct offer
        gl_record_direct = deepcopy(record)
        gl_record_direct['binary_price'] = '1300 1 0 RUR RUR'
        gl_record_direct['binary_oldprice'] = '1500 1 0 RUR RUR'
        gl_record_direct['shop_id'] = 100500
        gl_record_direct['flags'] = OfferFlags.IS_DIRECT.value
        self.run_stats_calc(
            'GroupRegionalStats',
            json.dumps([gl_record1, gl_record2, gl_record_direct])
        )

        # MAX_DISCOUNT  for RUR: 1) (122 - 140) / 140 = 12.8 %   2) (150 - 130) / 150 = 13.333 %  The second is greater
        # MIN_OLDPRICE  for RUR: 140 is less than 150 so 140 is expected

        def expected(model_type):
            return (
                '\t2\t2\t0\t0\t2\t0\t13\t0\t0\t0\t1\t0\t4\t'
                '%s\t0\t0\t0\t0\t0\t0\t0\t0\t'
                '621\t640.900024\t601.460022\t690.200012\t601.460022\t0\t'
                '126\t130\t122\t140\t122\t0\t'
                '3.528\t3.64\t3.416\t3.92\t3.416\t0\t'
                '42\t43.644901\t40.959061\t47.002201\t40.959061\t0\t'
                '|\t-1\t213\n'
            ) % model_type

        self.assertEqual(header + 'a\t100' + expected(0), open(file_path).read())

    def test_mixed_model_cluster_regional_stats(self):
        file_path = self.tmp_file_path('group_region_stats.csv')

        for i in [bad_cluster_id, good_cluster_id]:
            gl_record = deepcopy(record)
            gl_record["classifier_magic_id"] = classifier_id
            gl_record["cluster_id"] = i
            gl_record["model_id"] = 0

            gl_record_model = deepcopy(record)

            self.run_stats_calc('GroupRegionalStats', json.dumps([gl_record, gl_record_model]))
            result = header + 'a\t100' + result_for_regional_stats(0)
            if i == good_cluster_id:
                result += 'a\t1005899582' + result_for_regional_stats(1)
            self.assertEqual(sorted(result.split('\n')), sorted(open(file_path).read().split('\n')))

    def clone_gl_record(self, record, price, oldprice, allowed_oldprice, min_price):
        glr = deepcopy(record)
        glr["binary_price"] = '{} 1 0 RUR RUR'.format(price)
        glr["binary_oldprice"] = '{} 1 0 RUR RUR'.format(oldprice)
        glr["binary_unverified_oldprice"] = '{} 1 0 RUR RUR'.format(oldprice)
        glr["binary_allowed_oldprice"] = '{} 1 0 RUR RUR'.format(allowed_oldprice)
        glr["binary_min_price"] = '{} 1 0 RUR RUR'.format(min_price)
        return glr

    def test_shop_stat_discount_stats(self):
        file_path = self.tmp_file_path('shop_discount_stats.csv')
        gl_record = deepcopy(record)
        gl_record["shop_name"] = 'my shop'
        gl_record["binary_price"] = '1000 1 0 RUR RUR'

        # test offers with cluster_id
        for i in [bad_cluster_id, good_cluster_id]:
            gl_record["classifier_magic_id"] = classifier_id
            gl_record["cluster_id"] = i
            gl_record["binary_unverified_oldprice"] = '1800 1 0 RUR RUR'
            gl_record["binary_allowed_oldprice"] = '1800 1 0 RUR RUR'
            self.run_stats_calc('ShopStats', json.dumps([gl_record]))
            self.assertEqual("100\tmy shop\t1\t1\t100.0\t0\t0.0\t0\t0.0\n", open(file_path).read())

        gl_record0 = self.clone_gl_record(gl_record, 10000, 18000, 20000, 9000)
        gl_record0['contex_info']['original_msku_id'] = 100  # this record must be filtered
        gl_record1 = self.clone_gl_record(gl_record, 1000, 1800, 2000, 900)
        gl_record2 = self.clone_gl_record(gl_record, 1000, 1800, 1000, 900)
        del gl_record2["binary_oldprice"]
        gl_record3 = self.clone_gl_record(gl_record, 1000, 1800, 1000, 1100)
        gl_record4 = self.clone_gl_record(gl_record, 1000, 1800, 1000, 1000)
        # filtered direct offer
        gl_record_direct = deepcopy(record)
        gl_record_direct['binary_price'] = '1300 1 0 RUR RUR'
        gl_record_direct['binary_oldprice'] = '1500 1 0 RUR RUR'
        gl_record_direct['shop_id'] = 100500
        gl_record_direct['flags'] = OfferFlags.IS_DIRECT.value
        self.run_stats_calc(
            'ShopStats',
            json.dumps([gl_record0, gl_record1, gl_record2, gl_record3, gl_record4, gl_record_direct])
        )
        self.assertEqual("100\tmy shop\t4\t4\t100.0\t3\t75.0\t1\t25.0\n", open(file_path).read())

    def test_shop_stat_promo_stats(self):
        file_path = self.tmp_file_path('shop_promo_stats.csv')

        gl_record0 = deepcopy(record)
        gl_record0["shop_id"] = 1000
        gl_record0["shop_name"] = 'shop1'
        gl_record0["promo_type"] = 2
        gl_record0["url"] = 'https://url.yandex.com'

        gl_record1 = deepcopy(record)
        gl_record1["shop_id"] = 1000
        gl_record1["shop_name"] = 'shop1'
        gl_record1["promo_type"] = 16
        gl_record1["url"] = 'https://url.yandex.com'

        gl_record2 = deepcopy(record)
        gl_record2["shop_id"] = 1000
        gl_record2["shop_name"] = 'shop1'
        gl_record2["url"] = 'https://url.yandex.com'

        gl_record3 = deepcopy(record)
        gl_record3["shop_id"] = 101
        gl_record3["shop_name"] = 'shop2'
        gl_record3["promo_type"] = 4
        gl_record3["url"] = 'https://url.yandex.com'

        gl_record4 = deepcopy(record)
        gl_record4["shop_id"] = 102
        gl_record4["shop_name"] = 'shop3'
        gl_record4["promo_type"] = 1
        gl_record4["url"] = 'https://url.yandex.com'

        gl_record5 = deepcopy(record)
        gl_record5["shop_id"] = 102
        gl_record5["shop_name"] = 'shop3'
        gl_record5["promo_type"] = 1
        gl_record5["url"] = 'https://url.yandex.com'
        gl_record5['contex_info']['original_msku_id'] = 100  # this record must be filtered

        # filtered direct offer
        gl_record_direct = deepcopy(record)
        gl_record_direct['binary_price'] = '1300 1 0 RUR RUR'
        gl_record_direct['binary_oldprice'] = '1500 1 0 RUR RUR'
        gl_record_direct['shop_id'] = 100500
        gl_record_direct['flags'] = OfferFlags.IS_DIRECT.value

        self.run_stats_calc(
            'ShopStats',
            json.dumps([gl_record4, gl_record0, gl_record3, gl_record2, gl_record1, gl_record5, gl_record_direct])
        )

        result_lines = open(file_path).readlines()
        expected_lines = [
            '1000\tshop1\t3\t2\t66.7\t0\t0.0\t1\t33.3\t0\t0.0\t0\t0.0\t1\t33.3\n',
            '101\tshop2\t1\t1\t100.0\t0\t0.0\t0\t0.0\t1\t100.0\t0\t0.0\t0\t0.0\n',
            '102\tshop3\t1\t1\t100.0\t1\t100.0\t0\t0.0\t0\t0.0\t0\t0.0\t0\t0.0\n'
        ]
        self.assertEqual(result_lines, expected_lines)

    def test_category_discount_stats(self):
        """Test parent propagation.
        """
        file_path = self.tmp_file_path('category_discount_stats.csv')
        gl_record = deepcopy(record)
        gl_record["classifier_magic_id"] = classifier_id
        gl_record["cluster_id"] = good_cluster_id
        gl_record["binary_allowed_oldprice"] = '1800 1 0 RUR RUR'
        gl_record["binary_unverified_oldprice"] = '1700 1 0 RUR RUR'

        gl_record0 = self.clone_gl_record(gl_record, 10000, 18000, 20000, 9000)
        gl_record0['contex_info']['original_msku_id'] = 100  # this record must be filtered
        gl_record1 = self.clone_gl_record(gl_record, 800, 1000, 2000, 800)
        gl_record2 = self.clone_gl_record(gl_record, 900, 1000, 950, 800)

        # filtered direct offer
        gl_record_direct = deepcopy(record)
        gl_record_direct['binary_price'] = '1300 1 0 RUR RUR'
        gl_record_direct['binary_oldprice'] = '1500 1 0 RUR RUR'
        gl_record_direct['shop_id'] = 100500
        gl_record_direct['flags'] = OfferFlags.IS_DIRECT.value

        self.run_stats_calc(
            'CategoryDiscountStats',
            json.dumps([gl_record, gl_record0, gl_record1, gl_record2, gl_record_direct])
        )
        # Идентификатор категории
        # Имя категории
        # Количество оферов
        # Количество оферов со скидкой
        # Процент оферов со скидкой
        # Максимальная скидка в процентах
        # Количество оферов с валидной скидкой
        # Процент оферов с валидной скидкой
        # Максимальная валидная скидка в процентах
        # Количество оферов с невалидной скидкой
        result_lines = open(file_path).readlines()
        expected_lines = [
            i.format('3\t3\t93\t100.0\t2\t66.7\t20\t1') for i in [
                '90401\tВсе товары\t{}\n',
                '7812191\tОбувь\t{}\n',
                '7812192\tЖенская обувь\t{}\n',
                '7814999\tСапоги\t{}\n',
                '7877999\tОдежда, обувь и аксессуары\t{}\n',
            ]
        ]
        self.assertEqual(result_lines, expected_lines)

    def test_category_promo_stats(self):
        file_path = self.tmp_file_path('category_promo_stats.csv')

        gl_record1 = deepcopy(record)
        gl_record1["classifier_magic_id"] = classifier_id
        gl_record1["cluster_id"] = good_cluster_id
        gl_record1["model_id"] = 42
        gl_record1["promo_type"] = 4

        gl_record2 = deepcopy(record)
        gl_record2["classifier_magic_id"] = classifier_id
        gl_record2["cluster_id"] = good_cluster_id
        gl_record2["model_id"] = 42
        gl_record2["category_id"] = 7812191
        gl_record2["promo_type"] = 16

        gl_record3 = deepcopy(record)
        gl_record3["classifier_magic_id"] = classifier_id
        gl_record3["cluster_id"] = good_cluster_id
        gl_record3["model_id"] = 4242
        gl_record3["category_id"] = 7812191

        gl_record4 = deepcopy(record)
        gl_record4["classifier_magic_id"] = classifier_id
        gl_record4["cluster_id"] = good_cluster_id
        gl_record4["model_id"] = 4242
        gl_record4["category_id"] = 7812191
        gl_record4['contex_info']['original_msku_id'] = 100  # this record must be filtered

        # filtered direct offer
        gl_record_direct = deepcopy(record)
        gl_record_direct['binary_price'] = '1300 1 0 RUR RUR'
        gl_record_direct['binary_oldprice'] = '1500 1 0 RUR RUR'
        gl_record_direct['shop_id'] = 100500
        gl_record_direct['flags'] = OfferFlags.IS_DIRECT.value

        self.run_stats_calc(
            'CategoryPromoStats',
            json.dumps([gl_record1, gl_record2, gl_record3, gl_record4, gl_record_direct])
        )

        excpected_lines = [
            '90401\tВсе товары\t3\t2\t66.666672\t0\t0.000000\t0\t0.000000\t1\t50.000000\t0\t0.000000\t1\t50.000000\n',
            '7812191\tОбувь\t3\t2\t66.666672\t0\t0.000000\t0\t0.000000\t1\t50.000000\t0\t0.000000\t1\t50.000000\n',
            '7812192\tЖенская обувь\t1\t1\t100.000000\t0\t0.000000\t0\t0.000000\t1\t100.000000\t0\t0.000000\t0\t0.000000\n',
            '7814999\tСапоги\t1\t1\t100.000000\t0\t0.000000\t0\t0.000000\t1\t100.000000\t0\t0.000000\t0\t0.000000\n',
            '7877999\tОдежда, обувь и аксессуары\t3\t2\t66.666672\t0\t0.000000\t0\t0.000000\t1\t50.000000\t0\t0.000000\t1\t50.000000\n',
        ]
        self.assertEqual(excpected_lines, open(file_path).readlines())

    def test_category_mapping_stats(self):
        def generate_record(feed_id, shop_category_id, category_id, contex_info=None, flags=None):
            gl_record = deepcopy(record)
            gl_record["feed_id"] = feed_id
            gl_record["shop_category_id"] = shop_category_id
            gl_record["category_id"] = category_id
            if contex_info:
                gl_record['contex_info'] = contex_info
            if flags:
                gl_record['flags'] = flags
            return gl_record

        def sort_result(result):
            for rec in result:
                rec['offers_count'].sort()
            result.sort()

        file_path = self.tmp_file_path('category_mapping_stats.pbuf.sn')
        records = []

        records.append(generate_record(100, "42", 38529))
        records.append(generate_record(100, "42", 38529))
        records.append(generate_record(100, "42", 38529))
        records.append(generate_record(100, "42", 2342))
        records.append(generate_record(100, "42", 2342))
        records.append(generate_record(100, "66", 2342))
        records.append(generate_record(100, "66", 2342))
        records.append(generate_record(200, "66", 2342))
        records.append(generate_record(200, "42", 38529))
        records.append(generate_record(200, "43", 12345))
        records.append(generate_record(200, "44", 77777))
        records.append(generate_record(300, "43", 1111))
        records.append(generate_record(300, "47", 90401))  # root category -> should be skipped
        records.append(generate_record(400, "0", 123))
        records.append(generate_record(400, "12345678987654321", 42))
        records.append(generate_record(400, "string", 123))
        records.append(generate_record(400, "filtered", 1233543, contex_info={'original_msku_id': 100}))
        records.append(generate_record(400, "direct", 12335438, flags=OfferFlags.IS_DIRECT.value))
        random.seed(10)
        random.shuffle(records)

        self.run_stats_calc(
            'CategoryMappingStats',
            json.dumps(records)
        )
        actual_result = self.get_stats_from_pbufsn(file_path)

        expected_result = [
            {
                u'feed_id': 100,
                u'offers_count': [
                    {u'shop_category_id': "66", u'market_category_id': 2342,  u'noffers': 2},
                    {u'shop_category_id': "42", u'market_category_id': 2342,  u'noffers': 2},
                    {u'shop_category_id': "42", u'market_category_id': 38529, u'noffers': 3},
                ]
            },
            {
                u'feed_id': 200,
                u'offers_count': [
                    {u'shop_category_id': "42", u'market_category_id': 38529, u'noffers': 1},
                    {u'shop_category_id': "43", u'market_category_id': 12345, u'noffers': 1},
                    {u'shop_category_id': "44", u'market_category_id': 77777, u'noffers': 1},
                    {u'shop_category_id': "66", u'market_category_id': 2342, u'noffers': 1},
                ]
            },
            {
                u'feed_id': 300,
                u'offers_count': [
                    {u'shop_category_id': "43", u'market_category_id': 1111, u'noffers': 1},
                ]
            },
            {
                u'feed_id': 400,
                u'offers_count': [
                    {u'shop_category_id': "0", u'market_category_id': 123, u'noffers': 1},
                    {u'shop_category_id': "string", u'market_category_id': 123, u'noffers': 1},
                    {u'shop_category_id': "12345678987654321", u'market_category_id': 42, u'noffers': 1},
                ]
            },
        ]

        sort_result(actual_result)
        sort_result(expected_result)

        self.assertEqual(actual_result, expected_result)

    def test_shop_stats(self):
        gl_record1 = deepcopy(record)
        gl_record1["classifier_magic_id"] = classifier_id
        gl_record1["cluster_id"] = good_cluster_id
        gl_record1["binary_price"] = '1000 1 0 RUR RUR'
        gl_record1["binary_oldprice"] = '1800 1 0 RUR RUR'
        gl_record1["url"] = "https://url.yandex.com"
        gl_record2 = deepcopy(gl_record1)
        gl_record2["url"] = "http://url.com"
        gl_record3 = deepcopy(gl_record1)
        gl_record3['contex_info']['original_msku_id'] = 100  # this record must be filtered

        # filtered direct offer
        gl_record_direct = deepcopy(record)
        gl_record_direct['binary_price'] = '1300 1 0 RUR RUR'
        gl_record_direct['binary_oldprice'] = '1500 1 0 RUR RUR'
        gl_record_direct['shop_id'] = 100500
        gl_record_direct['flags'] = OfferFlags.IS_DIRECT.value

        self.run_stats_calc(
            'ShopStats',
            json.dumps([gl_record1, gl_record2, gl_record3, gl_record_direct])
        )
        self.assertEqual(1, len(open(self.tmp_file_path('shop_names.csv')).readlines()))

        offers_samples_expected = ""
        offers_samples_expected += '100\thttps://url.yandex.com\t1000RUR 335.73UAH 4930KZT 28BYN\t1800RUR 604.31UAH 8874KZT 50.4BYN\n'
        offers_samples_expected += '100\thttp://url.com\t1000RUR 335.73UAH 4930KZT 28BYN\t1800RUR 604.31UAH 8874KZT 50.4BYN\n'

        self.assertEqual(offers_samples_expected, open(self.tmp_file_path('offers_samples.csv')).read())

    def test_wizard_discount_stats(self):
        open(self.tmp_file_path('clothes-zero-ctr.db'), 'w').close()
        gl_record = deepcopy(record)
        gl_record["classifier_magic_id"] = classifier_id
        gl_record["cluster_id"] = good_cluster_id
        gl_record["binary_price"] = '1000 1 0 RUR RUR'
        gl_record["binary_oldprice"] = '1800 1 0 RUR RUR'
        gl_record0 = deepcopy(gl_record)
        gl_record0['contex_info']['original_msku_id'] = 100  # this record must be filtered
        # filtered direct offer
        gl_record_direct = deepcopy(record)
        gl_record_direct['binary_price'] = '1300 1 0 RUR RUR'
        gl_record_direct['binary_oldprice'] = '1500 1 0 RUR RUR'
        gl_record_direct['shop_id'] = 100500
        gl_record_direct['flags'] = OfferFlags.IS_DIRECT.value
        self.run_stats_calc(
            'VisualClusterWizardStats',
            json.dumps([gl_record, gl_record0, gl_record_direct])
        )

    def test_model_regional_stats_empty_mapper(self):
        # TODO(a-square): replace with empty genlog mapper
        pass

    def test_vendor_category_stats(self):
        def generate_record(vendor_id, category_id, regions, geo_regions, rgb=RGB_GREEN, contex_info=None, flags=None):
            gl_record = deepcopy(record)
            gl_record["vendor_id"] = vendor_id
            gl_record["category_id"] = category_id
            gl_record["regions"] = regions
            gl_record["int_regions"] = string_regions_to_list(regions)
            gl_record["geo_regions"] = geo_regions
            gl_record["int_geo_regions"] = string_regions_to_list(geo_regions)
            gl_record["downloadable"] = True
            if rgb == RGB_BLUE:
                gl_record["flags"] = OfferFlags.BLUE_OFFER.value
            if contex_info:
                gl_record['contex_info'] = contex_info
            if flags:
                gl_record["flags"] = gl_record.get("flags", 0) | flags
            return gl_record

        file_path = self.tmp_file_path('vendor_category_stats.pbuf.sn')
        blue_file_path = self.tmp_file_path('blue_vendor_category_stats.pbuf.sn')
        records = []

        # regions:
        #     213(msk)
        #     |___________________
        #     |        |         |
        #     20357    20359     20361
        #     |                  |
        #     117041             120562
        #
        #
        #     2(spb)
        #     |
        #     20281
        #     |________
        #     |       |
        #     120610  120609
        #
        #     211 - Australia (should be skipped in csv)
        #
        #
        # important top regions:
        #
        #     0    - root
        #     2    - Петербург
        #     3    - ЦФО
        #     17   - СЗФО
        #     213  - Москва
        #     225  - Россия
        #
        # their relations:
        #
        #     2   -> 17  -> 225 -> 0
        #     213 -> 3   -> 225 -> 0
        #
        # categories (leaf): 396901 6527202 7331258
        # 90401 - root category (should be skipped)

        def generate_stats(rgb):
            records.append(generate_record(100, 396901,  "213 20357 120609",                "", rgb))
            records.append(generate_record(100, 9988777,  "225 213 2",                      "", rgb))
            records.append(generate_record(100, 396901,  "2",                               "", rgb))
            records.append(generate_record(100, 396901,  "2",                               "2", rgb))
            records.append(generate_record(100, 396901,  "",                                "2", rgb))
            records.append(generate_record(100, 6527202, "20359 20357 117041 211",          "", rgb))
            records.append(generate_record(100, 6527202, "117041 20357 20359 20361 120562", "", rgb))
            records.append(generate_record(200, 396901,  "2 120610 120609",                 "", rgb))
            records.append(generate_record(200, 7331258, "213 120562",                      "2", rgb))
            records.append(generate_record(200, 7331258, "2 2 2",                           "", rgb))
            records.append(generate_record(200, 7331258, "213 2",                           "", rgb))
            records.append(generate_record(300, 11111,   "211",                             "", rgb))
            records.append(generate_record(0,   11111,   "213",                             "", rgb))
            records.append(generate_record(111, 0,       "213",                             "", rgb))
            records.append(generate_record(100, 90401,   "213",                             "", rgb))
            records.append(generate_record(400, 396901,  "969 20281",                       "", rgb))
            records.append(generate_record(400, 396901,  "969 20281",                       "", rgb, contex_info={'original_msku_id': 100}))
            records.append(generate_record(400, 396901,  "969 20281",                       "", rgb, flags=OfferFlags.IS_DIRECT.value))

        def generate_fake_msku():
            record = generate_record(100, 396901,  "213 20357 120609",                "")
            record["flags"] = OfferFlags.MARKET_SKU.value
            records.append(record)

        def generate_has_gone():
            record = generate_record(100, 396901,  "213 20357 120609",                "", RGB_BLUE)
            record["has_gone"] = True
            record["is_blue_offer"] = True
            records.append(record)

        def generate_disabled_by_stock():
            record = generate_record(100, 396901,  "213 20357 120609", "", RGB_BLUE)
            record["disabled_flags"] = DisabledFlags.MARKET_STOCK.value
            record["is_blue_offer"] = True
            records.append(record)

        # https://st.yandex-team.ru/MARKETOUT-46551
        # хак на время обновления данных, потом надо удалить blue_vendor_category_stats.pbuf.sn
        # Данные из blue_vendor_category_stats.pbuf.sn переезжают в vendor_category_stats.pbuf.sn
        # generate_stats(RGB_GREEN)
        generate_stats(RGB_BLUE)
        generate_fake_msku()
        generate_has_gone()
        generate_disabled_by_stock()

        self.run_stats_calc(
            'VendorCategoryStats',
            json.dumps(records)
        )
        actual_result = self.get_stats_from_pbufsn(file_path)
        actual_blue_result = self.get_stats_from_pbufsn(blue_file_path)

        def normalize_results(xs):
            def normalize_result(x):
                def normalize_region_offers(region_offers):
                    return sorted(
                        (
                            region_offer[u'region_id'],
                            region_offer[u'offers_count'],
                        )
                        for region_offer in region_offers
                    )
                return (
                    x[u'vendor_id'],
                    x[u'category_id'],
                    normalize_region_offers(x.get(u'region_values') or ())
                )
            return sorted(itertools.imap(normalize_result, xs))

        self.maxDiff = None
        expected_result = [
            {
                u'vendor_id': 100,
                u'category_id': 396901,
                u'region_values': [
                    {u'region_id': 2, u'offers_count': 4},
                    {u'region_id': 17, u'offers_count': 4},
                    {u'region_id': 213, u'offers_count': 1},
                    {u'region_id': 3, u'offers_count': 1},
                    {u'region_id': 225, u'offers_count': 4},
                    {u'region_id': 0, u'offers_count': 4},
                ],
            },
            {
                u'vendor_id': 100,
                u'category_id': 6527202,
                u'region_values': [
                    {u'region_id': 213, u'offers_count': 2},
                    {u'region_id': 3, u'offers_count': 2},
                    {u'region_id': 225, u'offers_count': 2},
                    {u'region_id': 0, u'offers_count': 2},
                ],
            },
            {
                u'vendor_id': 100,
                u'category_id': 9988777,
                u'region_values': [
                    {u'region_id': 2, u'offers_count': 1},
                    {u'region_id': 17, u'offers_count': 1},
                    {u'region_id': 213, u'offers_count': 1},
                    {u'region_id': 3, u'offers_count': 1},
                    {u'region_id': 225, u'offers_count': 1},
                    {u'region_id': 0, u'offers_count': 1},
                ],
            },
            {
                u'vendor_id': 200,
                u'category_id': 396901,
                u'region_values': [
                    {u'region_id': 2, u'offers_count': 1},
                    {u'region_id': 17, u'offers_count': 1},
                    {u'region_id': 225, u'offers_count': 1},
                    {u'region_id': 0, u'offers_count': 1},
                ],
            },
            {
                u'vendor_id': 200,
                u'category_id': 7331258,
                u'region_values': [
                    {u'region_id': 2, u'offers_count': 3},
                    {u'region_id': 17, u'offers_count': 3},
                    {u'region_id': 213, u'offers_count': 2},
                    {u'region_id': 3, u'offers_count': 2},
                    {u'region_id': 225, u'offers_count': 3},
                    {u'region_id': 0, u'offers_count': 3},
                ],
            },
            {
                u'vendor_id': 300,
                u'category_id': 11111,
                u'region_values': [
                    {u'region_id': 0, u'offers_count': 1},
                ],
            },
            {
                u'vendor_id': 400,
                u'category_id': 396901,
                u'region_values': [
                    {u'region_id': 2, u'offers_count': 1},
                    {u'region_id': 17, u'offers_count': 1},
                    {u'region_id': 225, u'offers_count': 1},
                    {u'region_id': 0, u'offers_count': 1},
                ],
            },
        ]

        self.assertEqual(
            normalize_results(expected_result),
            normalize_results(actual_result))
        self.assertEqual(
            normalize_results(expected_result),
            normalize_results(actual_blue_result))

    def test_is_global_regional_stats(self):
        def generate_record(model_id, shop_id, regions, geo_regions, contex_info=None, flags=None):
            gl_record = deepcopy(record)
            gl_record["model_id"] = model_id
            gl_record["shop_id"] = shop_id
            gl_record["regions"] = regions
            gl_record["int_regions"] = string_regions_to_list(regions)
            gl_record["geo_regions"] = geo_regions
            gl_record["int_geo_regions"] = string_regions_to_list(geo_regions)
            gl_record["downloadable"] = True
            if contex_info:
                gl_record['contex_info'] = contex_info
            if flags:
                gl_record["flags"] = flags
            return gl_record

        file_path = self.tmp_file_path('is-global.pbuf.sn')
        records = []

        # shop_id with is_global
        records.append(generate_record(100, 1000000000,  "213 225", "225"))
        records.append(generate_record(200, 1000000000,  "213 225", "213"))
        records.append(generate_record(400, 1000000000,  "213 225", "213", contex_info={'original_msku_id': 100}))
        records.append(generate_record(500, 1000000000,  "213 225", "213", flags=OfferFlags.IS_DIRECT.value))

        # shop_id without is_global
        records.append(generate_record(300, 1000000001,  "213 225", "225"))

        self.run_stats_calc(
            'IsGlobalRegionalStats',
            json.dumps(records)
        )
        actual_result = self.get_stats_from_pbufsn(file_path)

        expected_result = [
            {
                u'model_id': 100,
                u'regions_id': [225],
            },
            {
                u'model_id': 200,
                u'regions_id': [225],
            },
        ]

        self.assertEqual(actual_result, expected_result)

    def test_model_regional_stats_is_fulfillment_light(self):
        file_path = self.tmp_file_path('model_region_stats.csv')

        gl_record1 = deepcopy(record)
        gl_record2 = deepcopy(record)
        gl_record1["classifier_magic_id"] = gl_record2["classifier_magic_id"] = classifier_id
        gl_record1["binary_price"] = '122 1 0 RUR RUR'
        gl_record1["binary_oldprice"] = '140 1 0 RUR RUR'
        gl_record1["promo_type"] = 4
        gl_record2["binary_price"] = '130 1 0 RUR RUR'
        gl_record2["binary_oldprice"] = '150 1 0 RUR RUR'
        gl_record3 = deepcopy(record)
        gl_record3["classifier_magic_id"] = classifier_id
        gl_record3['contex_info']['original_msku_id'] = 100  # this record must be filtered
        # filtered direct offer
        gl_record_direct = deepcopy(record)
        gl_record_direct['binary_price'] = '1300 1 0 RUR RUR'
        gl_record_direct['binary_oldprice'] = '1500 1 0 RUR RUR'
        gl_record_direct['shop_id'] = 100500
        gl_record_direct['flags'] = OfferFlags.IS_DIRECT.value

        def expected(has_fulfillment_light=0):
            return (
                '\t2\t2\t0\t0\t2\t0'
                '\t13\t0\t0'
                '\t0\t1\t0\t4'
                '\t0\t%d\t0\t0\t0\t0\t0'
                '\t0\t0\t'
                '621\t640.900024\t601.460022\t690.200012\t601.460022\t0\t'
                '126\t130\t122\t140\t122\t0\t'
                '3.528\t3.64\t3.416\t3.92\t3.416\t0\t'
                '42\t43.644901\t40.959061\t47.002201\t40.959061\t0\t'
                '|\t-1\t213\n'
            ) % has_fulfillment_light

        gl_record2["shop_id"] = 2002
        gl_record2["ff_light"] = False
        self.run_stats_calc(
            'GroupRegionalStats',
            json.dumps([gl_record1, gl_record2, gl_record3, gl_record_direct])
        )
        self.assertEqual(header + 'a\t100' + expected(has_fulfillment_light=0), open(file_path).read())

        gl_record2["ff_light"] = True
        self.run_stats_calc(
            'GroupRegionalStats',
            json.dumps([gl_record1, gl_record2, gl_record3, gl_record_direct])
        )
        self.assertEqual(header + 'a\t100' + expected(has_fulfillment_light=1), open(file_path).read())

    def test_model_regional_stats_for_blue_offer(self):
        '''
        Этот тест проверят, что для генлог-записи с выставленным флагом is_blue_offer = true при is_buyboxes = false
        не отфильтровываются и участвуют в рассчете статистик
        '''

        file_path = self.tmp_file_path('model_region_stats.csv')

        gl_record1 = deepcopy(record)
        gl_record2 = deepcopy(record)
        gl_record1["classifier_magic_id"] = gl_record2["classifier_magic_id"] = classifier_id
        gl_record1["binary_price"] = '122 1 0 RUR RUR'
        gl_record1["binary_oldprice"] = '140 1 0 RUR RUR'
        gl_record1["promo_type"] = 4
        gl_record2["binary_price"] = '130 1 0 RUR RUR'
        gl_record2["binary_blue_oldprice"] = '150 1 0 RUR RUR'
        gl_record2["shop_id"] = 2002
        gl_record2["market_sku"] = 18446744073709551615
        gl_record2["is_blue_offer"] = True
        gl_record3 = deepcopy(record)
        gl_record3["classifier_magic_id"] = classifier_id
        gl_record3["binary_price"] = '130 1 0 RUR RUR'
        gl_record3["binary_blue_oldprice"] = '150 1 0 RUR RUR'
        gl_record3['contex_info']['original_msku_id'] = 100  # this record must be filtered
        # filtered direct offer
        gl_record_direct = deepcopy(record)
        gl_record_direct['binary_price'] = '1300 1 0 RUR RUR'
        gl_record_direct['binary_oldprice'] = '1500 1 0 RUR RUR'
        gl_record_direct['shop_id'] = 100500
        gl_record_direct['flags'] = OfferFlags.IS_DIRECT.value

        def expected():
            return (
                '\t2\t2\t0\t0\t2\t0'
                '\t13\t0\t0'
                '\t0\t1\t0\t4'
                '\t0\t0\t0\t0\t0\t0\t0'
                '\t0\t0\t'
                '621\t640.900024\t601.460022\t690.200012\t601.460022\t0\t'
                '126\t130\t122\t140\t122\t0\t'
                '3.528\t3.64\t3.416\t3.92\t3.416\t0\t'
                '42\t43.644901\t40.959061\t47.002201\t40.959061\t0\t'
                '|\t-1\t213\n'
            )

        self.run_stats_calc(
            'GroupRegionalStats',
            json.dumps([gl_record1, gl_record2, gl_record3, gl_record_direct])
        )
        self.assertEqual(header + 'a\t100' + expected(), open(file_path).read())

    def test_blue_model_regional_stats(self):
        '''
        Этот тест проверяет вычисление синей mrs-статистики
        '''

        file_path = self.tmp_file_path('blue_model_region_stats.csv')

        gl_record1 = deepcopy(record)
        gl_record1["classifier_magic_id"] = classifier_id
        gl_record1["binary_price"] = '122 1 0 RUR RUR'
        gl_record1["binary_blue_oldprice"] = '140 1 0 RUR RUR'
        gl_record1["promo_type"] = 4
        gl_record1["is_blue_offer"] = True
        gl_record1["market_sku"] = 18446744073709551615

        gl_record2 = deepcopy(record)
        gl_record2["classifier_magic_id"] = classifier_id
        gl_record2["binary_price"] = '130 1 0 RUR RUR'
        gl_record2["binary_oldprice"] = '150 1 0 RUR RUR'
        gl_record2["shop_id"] = 2002
        gl_record2["cpa"] = 4  # REAL

        gl_record3 = deepcopy(record)
        gl_record3["classifier_magic_id"] = classifier_id
        gl_record3["binary_price"] = '130 1 0 RUR RUR'
        gl_record3["binary_oldprice"] = '150 1 0 RUR RUR'
        gl_record3['contex_info']['original_msku_id'] = 100  # this record must be filtered

        # filtered direct offer
        gl_record_direct = deepcopy(record)
        gl_record_direct['binary_price'] = '1300 1 0 RUR RUR'
        gl_record_direct['binary_oldprice'] = '1500 1 0 RUR RUR'
        gl_record_direct['shop_id'] = 100500
        gl_record_direct['flags'] = OfferFlags.IS_DIRECT.value

        def expected():
            return (
                # одна запись была отфильтрована
                '\t1\t1\t0\t0\t1\t1'
                '\t13\t0\t0'
                '\t0\t1\t0\t4'
                '\t0\t0\t0\t'  # в синей статистике не сработало: has_cpa=0 (возможно офферы имеют cpa=no)
                '0\t0\t0\t0\t0\t0\t'
                '601\t601.460022\t601.460022\t690.200012\t601.460022\t0\t'
                '122\t122\t122\t140\t122\t0\t'
                '3.416\t3.416\t3.416\t3.92\t3.416\t0\t'  # цены из gl_record1
                '41\t40.959061\t40.959061\t47.002201\t40.959061\t0\t'
                '|\t-1\t213\n'
            )

        self.run_stats_calc(
            'GroupRegionalStats',
            json.dumps([gl_record1, gl_record2, gl_record3, gl_record_direct])
        )
        self.assertEqual('{}a\t{}{}'.format(header, gl_record1["market_sku"], expected()), open(file_path).read())

    def test_blue_group_regional_stats(self):
        '''
        Этот тест проверяет вычисление синей grs-статистики
        '''

        file_path = self.tmp_file_path('blue_group_region_stats.csv')

        gl_record1 = deepcopy(record)
        gl_record1["classifier_magic_id"] = classifier_id
        gl_record1["binary_price"] = '122 1 0 RUR RUR'
        gl_record1["binary_blue_oldprice"] = '140 1 0 RUR RUR'
        gl_record1["promo_type"] = 4
        gl_record1["is_blue_offer"] = True
        gl_record1["market_sku"] = 18446744073709551615

        gl_record2 = deepcopy(record)
        gl_record2["classifier_magic_id"] = classifier_id
        gl_record2["binary_price"] = '130 1 0 RUR RUR'
        gl_record2["binary_oldprice"] = '150 1 0 RUR RUR'
        gl_record2["shop_id"] = 2002

        gl_record_disabled_by_dynamic = deepcopy(record)
        gl_record_disabled_by_dynamic["classifier_magic_id"] = classifier_id
        gl_record_disabled_by_dynamic["binary_price"] = '122 1 0 RUR RUR'
        gl_record_disabled_by_dynamic["binary_blue_oldprice"] = '140 1 0 RUR RUR'
        gl_record_disabled_by_dynamic["promo_type"] = 4
        gl_record_disabled_by_dynamic["is_blue_offer"] = True
        gl_record_disabled_by_dynamic["market_sku"] = 18446744073709551615
        gl_record_disabled_by_dynamic["disabled_by_dynamic"] = True

        gl_record3 = deepcopy(record)
        gl_record3["classifier_magic_id"] = classifier_id
        gl_record3["binary_price"] = '130 1 0 RUR RUR'
        gl_record3["binary_oldprice"] = '150 1 0 RUR RUR'
        gl_record3['contex_info']['original_msku_id'] = 100  # this record must be filtered

        # filtered direct offer
        gl_record_direct = deepcopy(record)
        gl_record_direct['binary_price'] = '1300 1 0 RUR RUR'
        gl_record_direct['binary_oldprice'] = '1500 1 0 RUR RUR'
        gl_record_direct['shop_id'] = 100500
        gl_record_direct['flags'] = OfferFlags.IS_DIRECT.value

        def expected():
            return (
                '\t1\t1\t0\t0\t1\t1\t13\t0\t0\t0\t1\t0\t4\t0\t0\t'  # 2 записи были отфильтрованы
                '0\t0\t0\t0\t0\t0\t0\t'
                '601\t601.460022\t601.460022\t690.200012\t601.460022\t0\t'
                '122\t122\t122\t140\t122\t0\t'
                '3.416\t3.416\t3.416\t3.92\t3.416\t0\t'  # цены из gl_record1
                '41\t40.959061\t40.959061\t47.002201\t40.959061\t0\t'
                '|\t-1\t213\n'
            )

        self.run_stats_calc(
            'GroupRegionalStats',
            json.dumps([gl_record1, gl_record2, gl_record_disabled_by_dynamic, gl_record3, gl_record_direct])
        )
        self.assertEqual('{}a\t{}{}'.format(header, gl_record1["market_sku"], expected()), open(file_path).read())

    def test_model_regional_stats_cutprice(self):
        '''
        Проверяем, что в минимальной цене не учитывается цена уцененного товара
        но учитывается в новых полях с мин ценой уцененного и количеством уцененных
        '''

        file_path = self.tmp_file_path('model_region_stats.csv')
        gl_record1 = deepcopy(record)
        gl_record1["binary_price"] = '75 1 0 RUR RUR'
        gl_record1["flags"] = OfferFlags.IS_CUTPRICE.value

        gl_record2 = deepcopy(record)
        gl_record2["binary_price"] = '122 1 0 RUR RUR'
        gl_record2["flags"] = 0

        gl_record3 = deepcopy(record)
        gl_record3["binary_price"] = '130 1 0 RUR RUR'
        gl_record3["flags"] = 0
        gl_record3['contex_info']['original_msku_id'] = 100  # this record must be filtered

        # filtered direct offer
        gl_record_direct = deepcopy(record)
        gl_record_direct['binary_price'] = '1300 1 0 RUR RUR'
        gl_record_direct['binary_oldprice'] = '1500 1 0 RUR RUR'
        gl_record_direct['shop_id'] = 100500
        gl_record_direct['flags'] = OfferFlags.IS_DIRECT.value

        def expected():
            return (
                '\t1\t1\t0\t0\t0\t1\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0'  # одна запись была отфильтрована
                '\t0\t0\t0\t0\t1\t0\t'
                '601\t601.460022\t601.460022\t601.460022\t601.460022\t369.75\t'
                '122\t122\t122\t122\t122\t75\t'
                '3.416\t3.416\t3.416\t3.416\t3.416\t2.1\t'  # цены из gl_record1
                '41\t40.959061\t40.959061\t40.959061\t40.959061\t25.17975\t'
                '|\t-1\t213\n'
            )

        self.run_stats_calc(
            'GroupRegionalStats',
            json.dumps([gl_record1, gl_record2, gl_record3, gl_record_direct])
        )
        self.assertEqual(header + 'a\t100' + expected(), open(file_path).read())

    def test_model_regional_stats_just_cutprice(self):
        '''
        Проверяем модель с только уцененным товаром
        Убеждаемся, что все данные в статистике занулены, кроме количества уцененных товаров
        и цены уцененных товаров
        '''

        file_path = self.tmp_file_path('model_region_stats.csv')
        gl_record1 = deepcopy(record)
        gl_record1["binary_price"] = '75 1 0 RUR RUR'
        gl_record1["flags"] = OfferFlags.IS_CUTPRICE.value

        gl_record3 = deepcopy(record)
        gl_record3["binary_price"] = '130 1 0 RUR RUR'
        gl_record3["flags"] = OfferFlags.IS_CUTPRICE.value
        gl_record3['contex_info']['original_msku_id'] = 100  # this record must be filtered

        # filtered direct offer
        gl_record_direct = deepcopy(record)
        gl_record_direct['binary_price'] = '1300 1 0 RUR RUR'
        gl_record_direct['binary_oldprice'] = '1500 1 0 RUR RUR'
        gl_record_direct['shop_id'] = 100500
        gl_record_direct['flags'] = OfferFlags.IS_DIRECT.value

        def expected():
            return (
                '\t0\t0\t0\t0\t0\t0'
                '\t0\t0\t0'
                '\t0\t0\t0\t0'
                '\t0\t0\t0\t0\t0\t0\t0'
                '\t1\t0\t'  # одна запись была отфильтрована
                '0\t0\t0\t0\t0\t369.75\t'
                '0\t0\t0\t0\t0\t75\t'
                '0\t0\t0\t0\t0\t2.1\t'  # цены из gl_record1
                '0\t0\t0\t0\t0\t25.17975\t'
                '|\t-1\t213\n'
            )

        self.run_stats_calc(
            'GroupRegionalStats',
            json.dumps([gl_record1, gl_record3, gl_record_direct])
        )
        self.assertEqual(header + 'a\t100' + expected(), open(file_path).read())

    def test_model_regional_stats_with_dynamic_filters(self):
        file_path = self.tmp_file_path('model_region_stats.csv')

        gl_record = deepcopy(record)
        gl_record["classifier_magic_id"] = classifier_id

        # эта запись будет отфильтрована
        gl_record_disabled_by_dynamic = deepcopy(record)
        gl_record_disabled_by_dynamic["classifier_magic_id"] = classifier_id
        gl_record_disabled_by_dynamic["disabled_by_dynamic"] = True

        gl_record3 = deepcopy(record)
        gl_record3["classifier_magic_id"] = classifier_id
        gl_record3['contex_info']['original_msku_id'] = 100  # this record must be filtered

        # filtered direct offer
        gl_record_direct = deepcopy(record)
        gl_record_direct['binary_price'] = '1300 1 0 RUR RUR'
        gl_record_direct['binary_oldprice'] = '1500 1 0 RUR RUR'
        gl_record_direct['shop_id'] = 100500
        gl_record_direct['flags'] = OfferFlags.IS_DIRECT.value

        self.run_stats_calc(
            'GroupRegionalStats',
            json.dumps([gl_record, gl_record_disabled_by_dynamic, gl_record3, gl_record_direct])
        )
        result = header + 'a\t100' + result_for_regional_stats(0)
        self.assertEqual(result, open(file_path).read())
