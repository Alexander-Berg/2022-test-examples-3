#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Offer
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.offers += [Offer(title='iphone')]

    def test_tass(self):
        # Предварительно прогреваем репорт запросом, чтобы метрика появилась.
        self.report.request_bs('place=parallel&text=iphone')
        tass_data = self.base_search_client.request_tass()
        self.assertIn("loadlog_dhhh", tass_data)
        self.assertIn("loadlog-supermindmult_ahhh", tass_data)

    def test_update_metrics(self):
        self.report.update_dynamic()  # filters
        self.report.update_fulfillment_dynamic()  # fulfillment_filters
        self.report.update_lms_dynamic()  # lms
        self.report.update_loyalty_dynamic()  # loyalty
        self.report.update_fast_data_outlets_dynamic()  # fast_data_outlets
        self.report.update_model_bids_cutoff_dynamic()  # model_bids_cutoff
        self.report.update_qpromos()  # qpromos

        tass_data = self.report.request_tass()
        for signal in [
            "update_filters_dmmm",
            "update_fulfillment_filters_dmmm",
            "update_lms_dmmm",
            "update_model_bids_cutoff_dmmm",
            "update_qpromos_dmmm",
        ]:
            self.assertGreaterEqual(tass_data.get(signal), 1)

        for signal, values in tass_data.items():
            if 'hgram' in signal:
                message = "Histogram {} is only allowed to have 50 buckets, but has {}".format(signal, len(values))
                self.assertLessEqual(len(values), 50, msg=message)

    @classmethod
    def prepare_speller_shiny_cache(cls):
        cls.speller.on_default_request().respond()

    def test_speller_shiny_cache(self):
        _ = self.report.request_json('place=prime&text=ifone')
        tass_data = self.report.request_tass()
        self.assertEqual(tass_data.get('Speller_local_cache_access_count_dmmm'), 1)


if __name__ == '__main__':
    main()
