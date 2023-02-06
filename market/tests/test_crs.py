# -*- coding: utf-8 -*-

from collections import namedtuple
import json

from context import StatsCalcBaseTestCase as TestCaseBase
from market.idx.pylibrary.offer_flags.flags import OfferFlags
from utils import string_regions_to_list


SHOP_ONLINE = 1000000002
SHOP_OFFLINE = 1000000003


def make_price(price):
    """Makes a price expression from the price in rubles.
    """
    return '%.2f 1 0 RUR RUR' % price


def discount(price, old_price):
    """Calculates the discount from the current and the old prices.
    """
    price = float(price)
    old_price = float(old_price)
    return int(((old_price - price) / old_price) * 100)


CRSRecord = namedtuple(
    'CRSRecord',
    (
        'category',
        'region',
        'n_offers',
        'n_visual',
        'min_price',
        'max_discount',
        'n_discounts',
        'n_models',
        'n_discount_models'
    )
)


def parse_line(line):
    """Parses a line of CRS output and produces a CRSRecord
    with appropiate field types.
    """
    parts = line.strip().split('\t')
    return CRSRecord(
        int(parts[0]),
        int(parts[1]),
        int(parts[2]),
        int(parts[3]),
        float(parts[4]),
        int(parts[5]),
        int(parts[6]),
        int(parts[7]),
        int(parts[8]),
    )


def load_stats(path):
    """Loads the CRS output into a sorted array of records, stored
    as CRSRecord instances.
    """
    with open(path) as file_object:
        return sorted(parse_line(line) for line in file_object)


class TestCRS(TestCaseBase):
    """Tests the new MapReduce-optimized CRS implementation.

    Uses self.out_name and self.stat_name variables that must be
    defined in child classes.

    Relevant external data subtrees:

    categories:
                      90401
                     /     \
                 198119    91512
                 /   \\     /    \\
           1558993  90607 91518 91522

    regions:
                   1
                   |
                  213
               /   |   \
           20357 20359 20361
             |           |
           117041      120562
    """
    stat_name = 'ShopRegionalCategoriesStats'
    out_name = 'category_region_stats.csv'

    def make_record(self, **kwargs):
        """Makes a genlog record with sensible defaults.
        """
        record = dict(
            shop_id=SHOP_ONLINE,
            delivery_flag=True,
            priority_regions='1',
            category_id=90401,
            model_id=100,
        )
        record.update(kwargs)
        if ('priority_regions' in record) and ('regions' not in record):
            record['regions'] = record.get('priority_regions')
        if ('regions' in record) and ('int_regions' not in record):
            record['int_regions'] = string_regions_to_list(record.get('regions'))
        if ('geo_regions' in record) and ('int_geo_regions' not in record):
            record['int_geo_regions'] = string_regions_to_list(record.get('geo_regions'))
        return record

    def test_online_offline(self):
        """Legacy test, checks that only online shops end up in the stats.
        """
        file_path = self.tmp_file_path(self.out_name)

        record = self.make_record(
            binary_price=make_price(1000),
            binary_oldprice=make_price(1800),
        )
        self.run_stats_calc(self.stat_name, json.dumps([record]))
        self.assertNotEquals(0, len(load_stats(file_path)))

        record['shop_id'] = SHOP_OFFLINE
        self.run_stats_calc(self.stat_name, json.dumps([record]))
        self.assertEquals(0, len(load_stats(file_path)))

    def test_empty(self):
        """Checks that CRS doesn't crash or produce any spurious output
        when given no input.
        """
        file_path = self.tmp_file_path(self.out_name)

        self.run_stats_calc(self.stat_name, json.dumps([]))
        self.assertEquals([], load_stats(file_path))

    def test_one(self):
        """Checks that one offer propagates correctly through all categories
        upwards but doesn't propagate through any regions downwards.
        """
        file_path = self.tmp_file_path(self.out_name)

        record = self.make_record(
            binary_price=make_price(1000),
            binary_oldprice=make_price(1800),
            category_id=1558993,
        )

        self.run_stats_calc(self.stat_name, json.dumps([record]))
        self.assertEquals(
            [
                CRSRecord(90401, -1, 1, 0, 1000.0, discount(1000, 1800), 1, 1, 1),
                CRSRecord(90401, 1, 1, 0, 1000.0, discount(1000, 1800), 1, 1, 1),
                CRSRecord(90401, 213, 1, 0, 1000.0, discount(1000, 1800), 1, 1, 1),
                CRSRecord(90401, 20357, 1, 0, 1000.0, discount(1000, 1800), 1, 1, 1),
                CRSRecord(90401, 20359, 1, 0, 1000.0, discount(1000, 1800), 1, 1, 1),
                CRSRecord(90401, 20361, 1, 0, 1000.0, discount(1000, 1800), 1, 1, 1),
                CRSRecord(90401, 117041, 1, 0, 1000.0, discount(1000, 1800), 1, 1, 1),
                CRSRecord(90401, 120562, 1, 0, 1000.0, discount(1000, 1800), 1, 1, 1),
                CRSRecord(198119, -1, 1, 0, 1000.0, discount(1000, 1800), 1, 1, 1),
                CRSRecord(198119, 1, 1, 0, 1000.0, discount(1000, 1800), 1, 1, 1),
                CRSRecord(1558993, -1, 1, 0, 1000.0, discount(1000, 1800), 1, 1, 1),
                CRSRecord(1558993, 1, 1, 0, 1000.0, discount(1000, 1800), 1, 1, 1),
            ],
            load_stats(file_path)
        )

    def test_one_downloadable(self):
        """Checks that one offer propagates correctly through all categories
        upwards but doesn't propagate through any regions downwards.

        Downloadable version (regions calc goes to the other branch).
        """
        file_path = self.tmp_file_path(self.out_name)

        record = self.make_record(
            binary_price=make_price(1000),
            binary_oldprice=make_price(1800),
            category_id=1558993,
            regions='1',
            downloadable=True,
        )

        del record['priority_regions']

        self.run_stats_calc(self.stat_name, json.dumps([record]))
        self.assertEquals(
            [
                CRSRecord(90401, -1, 1, 0, 1000.0, discount(1000, 1800), 1, 1, 1),
                CRSRecord(90401, 1, 1, 0, 1000.0, discount(1000, 1800), 1, 1, 1),
                CRSRecord(90401, 213, 1, 0, 1000.0, discount(1000, 1800), 1, 1, 1),
                CRSRecord(90401, 20357, 1, 0, 1000.0, discount(1000, 1800), 1, 1, 1),
                CRSRecord(90401, 20359, 1, 0, 1000.0, discount(1000, 1800), 1, 1, 1),
                CRSRecord(90401, 20361, 1, 0, 1000.0, discount(1000, 1800), 1, 1, 1),
                CRSRecord(90401, 117041, 1, 0, 1000.0, discount(1000, 1800), 1, 1, 1),
                CRSRecord(90401, 120562, 1, 0, 1000.0, discount(1000, 1800), 1, 1, 1),
                CRSRecord(198119, -1, 1, 0, 1000.0, discount(1000, 1800), 1, 1, 1),
                CRSRecord(198119, 1, 1, 0, 1000.0, discount(1000, 1800), 1, 1, 1),
                CRSRecord(1558993, -1, 1, 0, 1000.0, discount(1000, 1800), 1, 1, 1),
                CRSRecord(1558993, 1, 1, 0, 1000.0, discount(1000, 1800), 1, 1, 1),
            ],
            load_stats(file_path)
        )

    def test_category_same_model(self):
        """Tests model counters propagation through categories
        when two offers have the same model.
        """
        file_path = self.tmp_file_path(self.out_name)

        records = [
            self.make_record(
                binary_price=make_price(900),
                category_id=198119,
            ),
            self.make_record(
                binary_price=make_price(1000),
                binary_oldprice=make_price(2000),
                category_id=198119,
            ),
            self.make_record(
                binary_price=make_price(900),
                category_id=198119,
                contex_info={'original_msku_id': 100},
            ),
            self.make_record(
                binary_price=make_price(1900),
                category_id=198119,
                flags=OfferFlags.IS_DIRECT.value,
            ),
            self.make_record(
                binary_price=make_price(1900),
                category_id=198119,
                flags=OfferFlags.IS_LAVKA.value,
            ),
            self.make_record(
                binary_price=make_price(1900),
                category_id=198119,
                flags=OfferFlags.IS_EDA_RESTAURANTS.value,
            ),
        ]

        self.run_stats_calc(self.stat_name, json.dumps(records))
        self.assertEquals(
            [
                CRSRecord(90401, -1, 2, 0, 900.0, discount(1000, 2000), 1, 1, 1),
                CRSRecord(90401, 1, 2, 0, 900.0, discount(1000, 2000), 1, 1, 1),
                CRSRecord(90401, 213, 2, 0, 900.0, discount(1000, 2000), 1, 1, 1),
                CRSRecord(90401, 20357, 2, 0, 900.0, discount(1000, 2000), 1, 1, 1),
                CRSRecord(90401, 20359, 2, 0, 900.0, discount(1000, 2000), 1, 1, 1),
                CRSRecord(90401, 20361, 2, 0, 900.0, discount(1000, 2000), 1, 1, 1),
                CRSRecord(90401, 117041, 2, 0, 900.0, discount(1000, 2000), 1, 1, 1),
                CRSRecord(90401, 120562, 2, 0, 900.0, discount(1000, 2000), 1, 1, 1),
                CRSRecord(198119, -1, 2, 0, 900.0, discount(1000, 2000), 1, 1, 1),
                CRSRecord(198119, 1, 2, 0, 900.0, discount(1000, 2000), 1, 1, 1),
            ],
            load_stats(file_path)
        )

    def test_category_different_models(self):
        """Tests model counters propagation through categories
        when two offers have different models.
        """
        file_path = self.tmp_file_path(self.out_name)

        records = [
            self.make_record(
                binary_price=make_price(900),
                category_id=198119,
            ),
            self.make_record(
                binary_price=make_price(1000),
                binary_oldprice=make_price(2000),
                category_id=90401,
                model_id=200,
            ),
            self.make_record(
                binary_price=make_price(900),
                category_id=1,
                contex_info={'original_msku_id': 100},
            ),
            self.make_record(
                binary_price=make_price(1900),
                category_id=198119,
                flags=OfferFlags.IS_DIRECT.value,
            ),
            self.make_record(
                binary_price=make_price(1900),
                category_id=198119,
                flags=OfferFlags.IS_LAVKA.value,
            ),
            self.make_record(
                binary_price=make_price(1900),
                category_id=198119,
                flags=OfferFlags.IS_EDA_RESTAURANTS.value,
            ),
        ]

        self.run_stats_calc(self.stat_name, json.dumps(records))
        self.assertEquals(
            [
                CRSRecord(90401, -1, 2, 0, 900.0, discount(1000, 2000), 1, 2, 1),
                CRSRecord(90401, 1, 2, 0, 900.0, discount(1000, 2000), 1, 2, 1),
                CRSRecord(90401, 213, 2, 0, 900.0, discount(1000, 2000), 1, 2, 1),
                CRSRecord(90401, 20357, 2, 0, 900.0, discount(1000, 2000), 1, 2, 1),
                CRSRecord(90401, 20359, 2, 0, 900.0, discount(1000, 2000), 1, 2, 1),
                CRSRecord(90401, 20361, 2, 0, 900.0, discount(1000, 2000), 1, 2, 1),
                CRSRecord(90401, 117041, 2, 0, 900.0, discount(1000, 2000), 1, 2, 1),
                CRSRecord(90401, 120562, 2, 0, 900.0, discount(1000, 2000), 1, 2, 1),
                CRSRecord(198119, -1, 1, 0, 900.0, 0, 0, 1, 0),
                CRSRecord(198119, 1, 1, 0, 900.0, 0, 0, 1, 0),
            ],
            load_stats(file_path)
        )

    def test_region_models(self):
        """Tests model counters propagation through regions.
        """
        file_path = self.tmp_file_path(self.out_name)

        # regions (relevant subtree):
        #                213
        #             /   |   \
        #         20357 20359 20361
        #            |          |
        #         117041      120562
        #
        # propagation pattern:
        # 213 - one prop (#1), one no-prop (#2)
        # 20357 - one prop (#3)
        # 117041 - one prop (#2)
        # 20359 - one no-prop (#4)
        # 20361 - (#1 from 213, distinct from 213 because of no-prop #2)
        # 120562 - root category propagation from 20361
        records = [
            # 1
            self.make_record(
                binary_price=make_price(800),
                priority_regions='213',
            ),
            # 2
            self.make_record(
                binary_price=make_price(700),
                priority_regions='117041',
                geo_regions='213',
                model_id=200,
            ),
            # 3
            self.make_record(
                binary_price=make_price(600),
                binary_oldprice=make_price(1000),
                priority_regions='20357',
                model_id=200,
            ),
            # 4
            self.make_record(
                binary_price=make_price(500),
                priority_regions='20359',
            ),
        ]

        self.run_stats_calc(self.stat_name, json.dumps(records))
        self.assertEquals(
            [
                CRSRecord(90401, -1, 4, 0, 500.0, discount(600, 1000), 1, 2, 1),
                CRSRecord(90401, 213, 2, 0, 700.0, 0, 0, 2, 0),
                CRSRecord(90401, 20357, 2, 0, 600.0, discount(600, 1000), 1, 2, 1),
                CRSRecord(90401, 20359, 2, 0, 500.0, 0, 0, 1, 0),
                CRSRecord(90401, 20361, 1, 0, 800.0, 0, 0, 1, 0),
                CRSRecord(90401, 117041, 3, 0, 600.0, discount(600, 1000), 1, 2, 1),
                CRSRecord(90401, 120562, 1, 0, 800.0, 0, 0, 1, 0),
            ],
            load_stats(file_path)
        )

    def test_visual(self):
        """Tests that visual offers increase the visual offers counter.
        """
        file_path = self.tmp_file_path(self.out_name)

        records = [
            self.make_record(
                binary_price=make_price(800),
                model_id=0,
                cluster_id=1005899581,  # bad cluster
            ),
            self.make_record(
                binary_price=make_price(900),
                model_id=0,
                cluster_id=1005899582,  # good cluster
            ),
        ]

        self.run_stats_calc(self.stat_name, json.dumps(records))
        self.assertEquals(
            [
                CRSRecord(90401, -1, 2, 0, 800.0, 0, 0, 0, 0),
                CRSRecord(90401, 1, 2, 0, 800.0, 0, 0, 0, 0),
                CRSRecord(90401, 213, 2, 0, 800.0, 0, 0, 0, 0),
                CRSRecord(90401, 20357, 2, 0, 800.0, 0, 0, 0, 0),
                CRSRecord(90401, 20359, 2, 0, 800.0, 0, 0, 0, 0),
                CRSRecord(90401, 20361, 2, 0, 800.0, 0, 0, 0, 0),
                CRSRecord(90401, 117041, 2, 0, 800.0, 0, 0, 0, 0),
                CRSRecord(90401, 120562, 2, 0, 800.0, 0, 0, 0, 0),
            ],
            load_stats(file_path)
        )

    def test_combined_propagation(self):
        """Tests that offers are correctly propagated across
        both the regions and the categories.
        """
        file_path = self.tmp_file_path(self.out_name)

        big_region = 1
        subregion = 213

        big_category = 1005463
        leaf_category = 6480508
        leaf_category2 = 1565817

        records = [
            self.make_record(
                binary_price=make_price(400),
                model_id=0,
                category_id=leaf_category,
                geo_regions='',
                priority_regions='',
                regions=str(subregion),
            ),
            self.make_record(
                binary_price=make_price(380),
                model_id=0,
                category_id=leaf_category,
                geo_regions='',
                priority_regions='',
                regions=str(big_region),
            ),
            self.make_record(
                binary_price=make_price(500),
                model_id=0,
                category_id=leaf_category2,
                geo_regions='',
                priority_regions='',
                regions=str(big_region),
            ),
        ]

        self.run_stats_calc(self.stat_name, json.dumps(records))
        combined_record = next(
            stats_record
            for stats_record in load_stats(file_path)
            if stats_record.category == big_category and stats_record.region == subregion
        )

        self.assertEquals(
            CRSRecord(big_category, subregion, 3, 0, 380.0, 0, 0, 0, 0),
            combined_record
        )

    def test_nested_regions(self):
        """Tests that nested delivery regions don't result in
        offer duplication.
        """
        file_path = self.tmp_file_path(self.out_name)

        record = self.make_record(
            binary_price=make_price(1000),
            binary_oldprice=make_price(1800),
            category_id=1558993,
            priority_regions='1 213',
        )

        self.run_stats_calc(self.stat_name, json.dumps([record]))
        self.assertEquals(
            [
                CRSRecord(90401, -1, 1, 0, 1000.0, discount(1000, 1800), 1, 1, 1),
                CRSRecord(90401, 1, 1, 0, 1000.0, discount(1000, 1800), 1, 1, 1),
                CRSRecord(90401, 213, 1, 0, 1000.0, discount(1000, 1800), 1, 1, 1),
                CRSRecord(90401, 20357, 1, 0, 1000.0, discount(1000, 1800), 1, 1, 1),
                CRSRecord(90401, 20359, 1, 0, 1000.0, discount(1000, 1800), 1, 1, 1),
                CRSRecord(90401, 20361, 1, 0, 1000.0, discount(1000, 1800), 1, 1, 1),
                CRSRecord(90401, 117041, 1, 0, 1000.0, discount(1000, 1800), 1, 1, 1),
                CRSRecord(90401, 120562, 1, 0, 1000.0, discount(1000, 1800), 1, 1, 1),
                CRSRecord(198119, -1, 1, 0, 1000.0, discount(1000, 1800), 1, 1, 1),
                CRSRecord(198119, 1, 1, 0, 1000.0, discount(1000, 1800), 1, 1, 1),
                CRSRecord(1558993, -1, 1, 0, 1000.0, discount(1000, 1800), 1, 1, 1),
                CRSRecord(1558993, 1, 1, 0, 1000.0, discount(1000, 1800), 1, 1, 1),
            ],
            load_stats(file_path)
        )

    def test_punctured_region(self):
        """Tests that delivery region inside no-delivery region still works.
        """
        file_path = self.tmp_file_path(self.out_name)

        big_region = 1
        no_delivery_region = 213
        small_region = 20357

        category = 1558993

        records = [
            self.make_record(
                binary_price=make_price(400),
                model_id=0,
                category_id=category,
                geo_regions=str(no_delivery_region),
                priority_regions='',
                regions=str(big_region),
            ),
            self.make_record(
                binary_price=make_price(1000),
                model_id=0,
                category_id=category,
                geo_regions='',
                priority_regions='',
                regions=str(small_region),
            ),
        ]

        self.run_stats_calc(self.stat_name, json.dumps(records))
        stats_records = load_stats(file_path)

        # record should propagate despite no_delivery region in between
        prop_records = [
            stats_record
            for stats_record in stats_records
            if stats_record.category == category and stats_record.region == small_region
        ]
        self.assertEquals(1, len(prop_records))
        self.assertEquals(
            CRSRecord(category, small_region, 2, 0, 400.0, 0, 0, 0, 0),
            prop_records[0]
        )
