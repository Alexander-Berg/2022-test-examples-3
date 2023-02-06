#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import MarketSku, BlueOffer
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['meta_pinger_use_internal_base_consistency_checker=1']
        cls.settings.meta_pinger_use_internal_base_consistency_checker = 'True'
        cls.index.mskus += [
            MarketSku(
                title="blue offer sku1",
                hyperid=1,
                sku=1,
                waremd5='Sku1-wdDXWsIiLVm1goleg',
                blue_offers=[BlueOffer()],
            )
        ]

    def test_base_state(self):
        # репорт стартует в состоянии CLOSED_INCONSISTENT_MANUAL_OPENING
        response = self.base_search_client.request_plain('admin_action=versions')
        self.assertIn('<report-status>CLOSED_CONSISTENT_MANUAL_OPENING</report-status>', str(response))

        # place=report_status обновляет состояние консистентности
        response = self.base_search_client.request_plain('place=report_status')
        self.assertIn('2;CLOSED_CONSISTENT_MANUAL_OPENING', str(response))

        # что будет, если открыть репорт в "закрытом" состоянии? Он откроется
        response = self.base_search_client.request_plain('place=report_status&report-status-action=open')
        self.assertIn('0;OK', str(response))


if __name__ == '__main__':
    main()
