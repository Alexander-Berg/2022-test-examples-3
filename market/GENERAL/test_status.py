#!/usr/bin/env python
# -*- coding: utf-8 -*-

import __classic_import  # noqa
import market.media_adv.incut_search.mt.env as env

from market.media_adv.incut_search.beam.region import Region
from market.media_adv.incut_search.beam.regional_model import RegionalModel


class T(env.MediaAdvIncutSearchSuite):
    @classmethod
    def setUpClass(cls):
        """
        переопределенный метод для дополнительного вызова настроек
        """
        cls.settings.access_using = True
        super(T, cls).setUpClass()

    @classmethod
    def setup_market_access_resources(cls):
        cls.access_resources.reserve_prices = {
            # hid, rp
            1: 10,
            2: 20,
        }
        cls.access_resources.cutoff_resource += [1, 2, 3]
        cls.access_resources.region_models += [
            RegionalModel(
                hyperid=1 + i,
                offers=10,
                has_cpa=True,
                rids=[200],
            )
            for i in range(0, 5)
        ]
        cls.access_resources.region_tree += [
            Region(
                rid=45,
                name="Вестерос",
                region_type=Region.FEDERAL_DISTRICT,
            )
        ]

    def test_simple_case(self):
        response = self.request(handler='status')
        self.assertFragmentIn(
            response,
            {
                "result": {
                    "AccessResourceVersions": [
                        {'name': 'geobase'},
                        {'name': 'media_adv_reserve_prices'},
                        {'name': 'madv_incut_search_data'},
                        {'name': 'vendor_model_bids_cutoff_dynamic'},
                    ]
                }
            },
        )
