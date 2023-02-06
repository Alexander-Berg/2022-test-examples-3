#!/usr/bin/env python
# -*- coding: utf-8 -*-

import json
import unittest
import subprocess
import os

import yatest.common


class TestJSON(unittest.TestCase):
    PBSNCAT_BIN = yatest.common.binary_path(
        'market/idx/tools/pbsncat/bin/pbsncat'
    )

    DATA_FOLDER = yatest.common.source_path(
        'market/idx/tools/pbsncat/tests/data'
    )

    def convert(self, magic, input_filename, input_format, output_format):
        """
        Convert input_filename using pbsncat.

        Output file name is <MAGIC>_<input_format>.<output_format>
        """
        output_filename = yatest.common.output_path(magic + '_' + input_format + '.' + output_format)

        with open(input_filename) as input_file:
            with open(output_filename, 'w') as output_file:
                subprocess.check_call(
                    [
                        self.PBSNCAT_BIN,
                        '--input-format', input_format,
                        '--format', output_format,
                        '--magic', magic
                    ],
                    stdin=input_file,
                    stdout=output_file
                )

        return output_filename

    def run_convert(self, magic, data):
        """
        Run pbsncat to convert given data with specified magic into
        multiple formats.

        1. Save data as JSON file.
        2. Convert the data:
            * json <-> pbsn
            * json <-> lenval
            * lenval <-> pbsn
            * json -> debug, pbsn -> debug, lenval -> debug
        3. Expect that:
            * (pbsn -> json) == (lenval -> json) == (original json)
            * (lenval -> pbsn) == (json -> pbsn)
            * (pbsn -> lenval) == (json -> lenval)
            * (json -> debug) == (pbsn -> debug) == (lenval -> debug)
        """

        def prepare_json_data(magic, data):
            """
            Save data as JSON file.
            """
            filename = yatest.common.output_path(magic + '.json')
            with open(filename, 'w') as out_file:
                json.dump(data, out_file)
            return filename

        def check_bin_files_equal(file_list):
            """
            Check the content of binary files, expect complete match
            """
            import filecmp
            for f in file_list[1:]:
                self.assertTrue(filecmp.cmp(file_list[0], f, shallow=False))

        def check_json_files_equal(file_list):
            """
            Check the content of files, regardless of order and spaces
            """
            first_str = ''
            current_str = ''
            with open(file_list[0]) as first_file:
                first_str = json.dumps(json.load(first_file), sort_keys=True)
            for filename in file_list[1:]:
                with open(filename) as f:
                    current_str = json.dumps(json.load(f), sort_keys=True)
                self.assertEqual(first_str, current_str)

        # Generate MAGIC.json
        json_orig = prepare_json_data(magic, data)
        # JSON <-> PBSN
        json_pbsn = self.convert(magic, json_orig, 'json', 'pbsn')
        pbsn_json = self.convert(magic, json_pbsn, 'pbsn', 'json')
        # JSON <-> LENVAL
        json_lenval = self.convert(magic, json_orig, 'json', 'lenval')
        lenval_json = self.convert(magic, json_lenval, 'lenval', 'json')
        # LENVAL <-> PBSN
        lenval_pbsn = self.convert(magic, json_lenval, 'lenval', 'pbsn')
        pbsn_lenval = self.convert(magic, lenval_pbsn, 'pbsn', 'lenval')
        # all -> DEBUG
        json_debug = self.convert(magic, json_orig, 'json', 'debug')
        pbsn_debug = self.convert(magic, json_pbsn, 'pbsn', 'debug')
        lenval_debug = self.convert(magic, json_lenval, 'lenval', 'debug')

        # Check files for equality.
        check_json_files_equal([json_orig, pbsn_json, lenval_json])
        check_bin_files_equal([json_pbsn, lenval_pbsn])
        check_bin_files_equal([json_lenval, pbsn_lenval])
        check_bin_files_equal([json_debug, pbsn_debug, lenval_debug])

    def test_read_pbsn(self):
        """
        Check values after conversion from pbsn to json
        """
        in_name = os.path.join(self.DATA_FOLDER, 'FPCT.pbuf.sn')
        out_name = self.convert('FPCT', in_name, 'pbsn', 'json')
        with open(out_name) as f:
            json_out = json.load(f)

            self.assertEqual(1101, json_out['feed_id'])
            self.assertEqual(1, len(json_out['category']))
            category = json_out['category'][0]
            self.assertEqual('1', category['id'])
            self.assertEqual(u'Часы', category['name'])

    def test_read_mbo_pb(self):
        """
        Check that we're able to read MBO-style protobuf.
        """
        in_name = os.path.join(self.DATA_FOLDER, 'models_91306.pb')
        out_name = self.convert('MBEM', in_name, 'mbo-pb', 'json')
        with open(out_name) as f:
            json_out = json.load(f)

            self.assertEqual(1380820037, json_out['id'])
            self.assertEqual(1486628567687, json_out['created_date'])

    def test_FPCT(self):
        # OffersData::Categories
        data = {
            'feed_id': 1101,
            'category': [
                {
                    'id': '1',
                    'name': 'watches'
                },
                {
                    'id': '100',
                    'name': 'goods'
                }
            ]
        }
        self.run_convert('FPCT', data)

    def test_GLOP(self):
        # GuruLightOfferParams::Record
        data = {
            'cmagic_id0': 10,
            'cmagic_id1': 20,
            'category_id': 123,
            'model_id': 456,
            'params': [
                {
                    'param_id': 1,
                    'value_id': 1,
                    'numeric_value': 2.5
                },
                {
                    'param_id': 15,
                    'value_id': 27,
                    'numeric_value': 0.05
                }
            ]
        }
        self.run_convert('GLOP', data)

    def test_AWSH(self):
        # Awaps::Shop
        data = {
            'client_id': 10,
            'shop_id': 123,
            'shop_name': 'test_shop',
            'datasource_name': 'data_name',
            'rating': 2,
            'is_online': True,
            'is_offline': False,
            'feeds': [
                {
                    'id': 34,
                    'url': 'ya.ru',
                    'urlforlog': 'ya_for_log.ru'
                }
            ]
        }
        self.run_convert('AWSH', data)

    def test_to_sku_filter_pbuf(self):
        json_in = {
            'offers': [
                {
                    'sku': [
                        'gr6kd74oqpsrvctn1ns5',
                        '3l8nmn3avgaqbverot3i',
                        '9nk9sbqoan05jnl469oe',
                    ],
                    'shop_id': 1,
                },
                {
                    'sku': [
                        '2nfllhr7fbv72h6ke4ek',
                        '53ejqaiieq1emnmojo7a',
                        'mtndos2f8ugakit56hr5',
                        '16rhr94kf3qoeqhkjefh',
                    ],
                    'shop_id': 100,
                }
            ]
        }
        in_name = yatest.common.output_path('sku-filter.json')
        with open(in_name, 'w') as f:
            json.dump(json_in, f)

        middle_name = self.convert('FFOF', in_name, 'json', 'sku-pb')
        out_name = self.convert('FFOF', middle_name, 'sku-pb', 'json')

        with open(out_name) as f:
            json_out = json.load(f)
            assert json_out == json_in

    def test_sku_filter_pbuf_gz(self):
        json_in = {
            'offers': [
                {
                    'sku': [
                        'gr6kd74oqpsrvctn1ns5',
                        '3l8nmn3avgaqbverot3i',
                        '9nk9sbqoan05jnl469oe',
                    ],
                    'shop_id': 1,
                },
                {
                    'sku': [
                        '2nfllhr7fbv72h6ke4ek',
                        '53ejqaiieq1emnmojo7a',
                        'mtndos2f8ugakit56hr5',
                        '16rhr94kf3qoeqhkjefh',
                    ],
                    'shop_id': 100,
                }
            ]
        }
        in_name = yatest.common.output_path('sku-filter.json')
        with open(in_name, 'w') as f:
            json.dump(json_in, f)

        middle_name = self.convert('FFOF', in_name, 'json', 'sku-pbgz')
        out_name = self.convert('FFOF', middle_name, 'sku-pbgz', 'json')

        with open(out_name) as f:
            json_out = json.load(f)
            assert json_out == json_in

    def test_FFSA(self):
        """ файл наличия фулфилмента
        https://st.yandex-team.ru/DELIVERY-3765#1503068511000
        https://github.yandex-team.ru/market-java/market-proto/blob/master/proto/Fulfillment.proto#L26-L32
        """
        data = {
            'stocks': [
                {
                    'stocks': [
                        {
                            'sku': 'vgvjc49coiv1mc5vb21d',
                            'shops': [
                                {'shop_id': 111},
                                {'shop_id': 2},
                                {'shop_id': 3}
                            ]
                        },
                        {
                            'sku': '5e8q9ags39f44d9jc4ro',
                            'shops': [
                                {
                                    'shop_id': 111,
                                    'korobyte': {
                                        'width': 123.01,
                                        'height': 2,
                                        'length': 0,
                                        'weight': 100000000000.0
                                    }
                                },
                                {'shop_id': 2}
                            ],
                            'korobyte': {
                                'width': 423.01,
                                'height': 2,
                                'length': 0,
                                'weight': 100000000000.0
                            }
                        },
                    ],
                    'warehouse_id': 111
                },
            ],
        }
        self.run_convert('FFSA', data)


if __name__ == '__main__':
    unittest.main()
