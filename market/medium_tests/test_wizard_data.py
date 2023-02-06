#!/usr/bin/python
# -*- coding: utf-8 -*-

import unittest

from market.pylibrary.yatestwrap.yatestwrap import source_path
from market.idx.marketindexer.marketindexer import wizard_data

from mapreduce.yt.python.yt_stuff import YtStuff, YtConfig


def parse_fields(item):
    lines = item.split("\n")
    fields_dict = {}
    for index in range(len(lines)):
        words = lines[index].split(" ")
        if len(words) == 3 and words[1] == '=':
            fields_dict[words[0]] = words[2]
        words = lines[index].split("\"")
        if len(words) == 3 and words[0] == '':
            fields_dict["Alias"] = words[1]
    return fields_dict


def parse_id(fields_dict):
    return int(fields_dict["Id"])


def parse_is_good_for_matching(fields_dict):
    return fields_dict["ShopIsGoodForMatching"] == "true"


def parse_good_alias(fields_dict):
    return fields_dict["GoodAlias"] == 'true'


class TestDataParser(unittest.TestCase):
    def test_get_shop_info_from_shops_dat(self):
        shops_info = wizard_data.get_shop_info_from_shops_dat(
            source_path('market/idx/marketindexer/medium_tests/data/shops.dat.test'),
            []  # test_shops
        )
        actual = [shop_info.id_ for shop_info in shops_info]
        expected = [1, 2, 3, 4, 5]
        self.assertEqual(actual, expected)

    def test_shops_for_matching(self):
        good_shops_for_matching = [1, 2, 5]
        expected_shops_for_matching = [1, 2, 3, 5]
        YT_SERVER = YtStuff(YtConfig(wait_tablet_cell_initialization=True))
        YT_SERVER.start_local_yt()
        yt_client = YT_SERVER.get_yt_client()
        schema = [
            dict(name='alias', type='string'),
            dict(name='shop_id', type='int64'),
            dict(name='verdict', type='boolean'),
        ]
        attributes = {'schema': schema}
        table_name = '//home/market/production/abo/shop_aliases'
        yt_client.create(
            'table',
            table_name,
            ignore_existing=True,
            recursive=True,
            attributes=attributes
        )
        yt_client.write_table(table_name , [
            dict(alias="golddisk.ru", shop_id=1, verdict=False),
            dict(alias="golddisk2.ru", shop_id=2, verdict=True),
            dict(alias="golddisk2", shop_id=2, verdict=True),
            dict(alias='голддиск2.ру', shop_id=2, verdict=True),
            dict(alias='голддиск2', shop_id=2, verdict=True)
        ])
        data_parser = wizard_data.DataParser('', '', '', '', table_name, yt_client)
        data_parser.parse_shops(source_path('market/idx/marketindexer/medium_tests/data/shops.dat.test'), '', good_shops_for_matching, '')

        shops_content = data_parser.get_shops_content()
        for index in range(1, len(shops_content)):
            shop = shops_content[index]
            fields_dict = parse_fields(shop)
            if parse_id(fields_dict) in expected_shops_for_matching:
                self.assertTrue(parse_is_good_for_matching(fields_dict))
            else:
                self.assertFalse(parse_is_good_for_matching(fields_dict))

        content = data_parser.get_content()
        for index in range(1, len(shops_content)):
            shop = content[index]
            fields_dict = parse_fields(shop)
            if parse_id(fields_dict) == 2:
                self.assertTrue(parse_good_alias(fields_dict))
            else:
                self.assertFalse(parse_good_alias(fields_dict))


if __name__ == '__main__':
    unittest.main()
