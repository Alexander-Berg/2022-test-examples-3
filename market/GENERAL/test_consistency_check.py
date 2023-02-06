#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import MarketSku, BlueOffer
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.mskus += [
            MarketSku(
                title="blue offer sku1",
                hyperid=1,
                sku=1,
                waremd5='Sku1-wdDXWsIiLVm1goleg',
                blue_offers=[BlueOffer()],
            )
        ]

    def test_response_ok(self):
        response = self.report.request_plain('place=consistency_check')
        self.assertIn('0;OK', str(response))
        response = self.report.request_plain('place=consistency_check&light-consistency-check=1')
        self.assertIn('0;OK', str(response))

    def test_can_determine_versions(self):
        # MARKETOUT-22262
        response = self.report.request_plain('place=consistency_check&debug=da')
        response_str = str(response)
        self.assertIn('DssmVersion: test.dssm', response_str)

        # Check the old behavior:
        response = self.report.request_plain('place=consistency_check&debug=net')
        self.assertNotIn('DssmVersion', str(response))

    def test_report_configuration_hint(self):
        """
        Проверяем, что можно включать/выключать хедер x-report-config-hint.
        """

        def collect_headers(response):
            return [key.lower() for key in response.headers.keys()]

        expected_header = 'x-market-req-ctx-report-config-hint'

        response = self.report.request_plain('place=consistency_check')
        self.assertIn('0;OK', str(response))

        # Не запрашиваем хедер: он должен появиться на выдаче – таково поведение по умолчанию
        response = self.report.request_json('place=prime&text=offer')
        self.assertIn(expected_header, collect_headers(response))

        # Запрашиваем хедер: он должен появиться на выдаче
        response = self.report.request_json('place=prime&text=offer&rearr-factors=use_report_configuration_hint=1')
        self.assertIn(expected_header, collect_headers(response))

        # Не запрашиваем хедер явным образом: на выдаче его быть не должно
        response = self.report.request_json('place=prime&text=offer&rearr-factors=use_report_configuration_hint=0')
        self.assertNotIn(expected_header, collect_headers(response))


if __name__ == '__main__':
    main()
