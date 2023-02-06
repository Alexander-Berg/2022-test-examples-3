#!/usr/bin/env python
# -*- coding: utf-8 -*-

import __classic_import  # noqa
import market.media_adv.incut_search.mt.env as env
from market.media_adv.incut_search.beam.incut import IncutModelsList
from market.media_adv.incut_search.beam.model import ModelWithBid


class T(env.MediaAdvIncutSearchSuite):
    @classmethod
    def setUpClass(cls):
        '''
        переопределенный метод для дополнительного вызова настроек
        '''
        cls.settings.access_using = True
        super(T, cls).setUpClass()

    @classmethod
    def prepare_incutstat_mrs(cls):
        cls.content.incuts += [
            IncutModelsList(
                id=1,
                hid=1234,
                vendor_id=2345,
                datasource_id=10,
                models=[ModelWithBid(model_id=1000 + i) for i in range(1, 4)],
                bid=90,
            ),
        ]

    def test_incutstat_wrong_id(self):
        response = self.request(
            {
                'incut_ids': '11111111',
                'rids': '1,2,3',
            },
            handler='incutstat',
        )
        self.assertFragmentIn(
            response,
            {
                'Errors': [
                    'NotFound',
                ],
            },
        )

    def test_incutstat_mrs_disabled(self):
        response = self.request(
            {
                'incut_ids': '1',
                'rids': '1,2,3',
            },
            exp_flags={
                'market_madv_model_regional_stats_enabled': 0,
            },
            handler='incutstat',
        )
        self.assertFragmentIn(
            response,
            {
                'ModelIds': [
                    '1001',
                    '1002',
                    '1003',
                ],
                'RegionStat': {
                    '1': 'NoFilter',
                    '2': 'NoFilter',
                    '3': 'NoFilter',
                },
            },
        )


if __name__ == '__main__':
    env.main()
