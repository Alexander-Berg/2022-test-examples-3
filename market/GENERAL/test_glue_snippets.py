#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

import json
from core.types import (
    Model,
    Offer,
    Shop,
)
from core.testcase import TestCase, main

GLUE_EXTERNAL_DATA = {'ext_data_1': 'str', 'ext_data_2': True}


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.shops += [Shop(fesh=213, priority_region=213, regions=[213])]

        cls.index.offers += [
            Offer(
                title='iphone',
                hyperid=103,
                waremd5="wtZDmSlp7DGGgA1BL6erMA",
                glue_external_data=GLUE_EXTERNAL_DATA,
                fesh=213,
            ),
        ]

        cls.index.models += [Model(hyperid=103, title="Айфон")]

    def test_print_doc_raw(self):
        response = self.report.request_json('place=print_doc&text=iphone')
        self.assertFragmentIn(response, {'glue_external_data': json.dumps(GLUE_EXTERNAL_DATA)})

    def test_prime(self):
        response = self.report.request_json('place=prime&text=iphone')
        self.assertFragmentIn(response, {'externalData': GLUE_EXTERNAL_DATA, "entity": "offer"})

    def test_offersinfo(self):
        response = self.report.request_json('place=offerinfo&offerid=wtZDmSlp7DGGgA1BL6erMA&rids=0&regset=1')
        self.assertFragmentIn(response, {'externalData': GLUE_EXTERNAL_DATA, "entity": "offer"})

    def test_productoffers(self):
        response = self.report.request_json('place=productoffers&hyperid=103')
        self.assertFragmentIn(response, {'externalData': GLUE_EXTERNAL_DATA, "entity": "offer"})

    def test_modelinfo_with_defaultoffer(self):
        response = self.report.request_json('place=modelinfo&hyperid=103&rids=213&use-default-offers=1')
        self.assertFragmentIn(response, {'externalData': GLUE_EXTERNAL_DATA, "entity": "offer"})


if __name__ == '__main__':
    main()
