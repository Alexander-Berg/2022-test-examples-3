# -*- coding: utf-8 -*-
import json
import os

import yatest.common as common

TEST_DATA_PATH = 'travel/cpa/tests/lib/data/order_processor'
TEST_DATA_FN_TEMPLATE = '{}.json'


class OrderProcessorDataProvider:
    def get_data(self, order_cls, fn_key):
        data_fn = os.path.join(TEST_DATA_PATH, TEST_DATA_FN_TEMPLATE.format(fn_key))
        with open(common.source_path(data_fn), 'r') as fdata:
            test_data = json.load(fdata)
            for order_data in test_data:
                snapshots = [order_cls.from_dict(sd, ignore_unknown=True) for sd in order_data.get('snapshots', [])]

                expected_order = order_data.get('expected_order', [])
                yield snapshots, expected_order
