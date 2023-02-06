# -*- coding: utf-8 -*-

from test_crs import (
    TestCRS,
    SHOP_ONLINE,
)

from utils import string_regions_to_list


class TestBlueBuyboxCRS(TestCRS):
    """Just like TestCRS but for blue buybox offers"""

    stat_name = 'ShopRegionalCategoriesStats'
    out_name = 'blue_buybox_category_region_stats.csv'

    def make_record(self, **kwargs):
        """Makes a genlog record with sensible defaults.
        """
        record = dict(
            shop_id=SHOP_ONLINE,
            delivery_flag=True,
            priority_regions='1',
            category_id=90401,
            model_id=100,
            is_blue_offer=True,
            is_buyboxes=True,
        )
        record.update(kwargs)
        if ('priority_regions' in record) and ('regions' not in record):
            record['regions'] = record.get('priority_regions')
        if ('regions' in record) and ('int_regions' not in record):
            record['int_regions'] = string_regions_to_list(record.get('regions'))
        if ('geo_regions' in record) and ('int_geo_regions' not in record):
            record['int_geo_regions'] = string_regions_to_list(record.get('geo_regions'))
        if 'binary_oldprice' in record:
            oldprice_value = record.pop('binary_oldprice')
            record['binary_white_oldprice'] = oldprice_value
        return record

    def test_online_offline(self):
        pass


del TestCRS
