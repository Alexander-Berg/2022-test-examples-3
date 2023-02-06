# -*- coding: utf-8 -*-

import json

from context import StatsCalcBaseTestCase


class MRSRaces(StatsCalcBaseTestCase):
    def test_thousand_models(self):
        gl_record = {
            'feed_id': 1086,
            'offer_id': "996",
            'shop_id': 721,
            'shop_name': "WineСellar",
            'regions': "213",
            'int_regions': [213],
            'priority_regions': "213",
            'shop_category_id': "77",
            'downloadable': False,
            'model_id': 5,  # fake
            'category_id': 12473293,
            'binary_price': "6500 1 0 RUR RUR",  # price expression (price rate plus id ref_id), make_genlog converts its to binary presentation
            'bid': 10,
            'cbid': 0,
            'barcode': "",
            'geo_regions': "213",
            'int_geo_regions': [213],
            'delivery_flag': True,
            'randx': 821139,
            'flags': 0x81,  # STORE && PICKUP
            'binary_ware_md5': "c39a074ce0cb486a0a7d54ec31c4ac1d",  # automatically converted to binary data in make_genlog
            'hasrequiredparams': True,
            'haspictures': False,
            'hasofferparams': True,
            'vendor_param_id': 7893318,
            'vendor_id': 10714082,
            'hasfortitleortype': True,
            'mbid': 10,
            'fee': 100,
            'has_any_required_param': False,
            'shop_country': 225,
        }

        gljson_filepath = self.tmp_file_path('genlog.json')
        total_offers = 1000  # 1000 offers of model = 1 .. 100 (10 offers per 1 model)

        def write_offer(gl_record, offer_id):
            uniq = (offer_id % 100) + 1

            gl_record['offer_id'] = str(offer_id)
            gl_record['model_id'] = uniq

            _f.write(json.dumps(gl_record))

        with open(gljson_filepath, 'w') as _f:
            _f.write('[')

            for offer_id in xrange(1, total_offers):
                write_offer(gl_record, offer_id)
                _f.write(',')

            write_offer(gl_record, total_offers)
            _f.write(',')
            gl_record['contex_info'] = {'original_msku_id': 100}  # contex, skipped
            write_offer(gl_record, total_offers)
            _f.write(']')

        self.run_stats_calc_from_file('GroupRegionalStats', gljson_filepath)

        model_ids = range(1, 101)

        file_path = self.tmp_file_path('model_region_stats.csv')
        with open(file_path, 'r') as _f:
            lines = _f.readlines()
            for line in lines[2:]:  # skip header
                _, model_id, model_count, _ = line.split('\t', 3)
                model_id, model_count = int(model_id), int(model_count)

                self.assertEqual(model_count, 10)
                self.assertTrue(model_id in model_ids)
