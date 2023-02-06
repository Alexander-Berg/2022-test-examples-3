#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import CardCategoryVendor, HyperCategory, Model, Offer, RegionalModel, Shop, Vendor
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        # В этом репорте магазин с cpc=sandbox должен становиться real
        cls.settings.use_apphost = True

        cls.index.regional_models += [
            RegionalModel(hyperid=500, rids=[213]),
            RegionalModel(hyperid=501, rids=[213]),
            RegionalModel(hyperid=502, rids=[202]),
        ]
        cls.index.shops += [Shop(fesh=1, category='A&B')]
        cls.index.models += [
            Model(hyperid=500, title='iphone 0', vendor_id=22, hid=21),
            Model(hyperid=501, title='iphone 1', vendor_id=22, hid=21),
            Model(hyperid=502, title='iphone 2', vendor_id=22, hid=21),
        ]
        cls.index.offers += [
            Offer(hyperid=500, title='iphone 10'),
            Offer(hyperid=501, title='iphone 11'),
            Offer(
                title='"название" xml_quote', descr='"описание" xml_quote', comment='"комментарий" к xml_quote', fesh=1
            ),
        ]
        cls.index.cards += [CardCategoryVendor(vendor_id=22, hid=21, hyperids=[500, 501, 502])]
        cls.index.hypertree += [
            HyperCategory(hid=21, name='telephoni'),
        ]
        cls.index.vendors += [Vendor(vendor_id=22, name='apple')]

    def check_response(self, response):
        source = response.apphost_source
        self.assertEqual(source.Name, "MARKET")
        self.assertEqual(source.SearchScript, 'http://apphost')

        # Skip in https://st.yandex-team.ru/MARKETOUT-21963
        # body_64 = base64.b64encode(response.request_body)
        # self.access_log.expect(request_body=body_64)

    def test_bs(self):
        params = {"place": "parallel", "text": "iphone", "debug": "1"}

        response = self.report.request_bs_apphost(params)
        self.check_response(response)
        self.assertTrue(response.contains('pron=pruncount334')[0])
        self.assertTrue(response.contains('pron=tbs17000')[0])
        self.assertTrue(response.contains('pron=versiontbs1')[0])
        self.assertTrue(response.contains('pron=mul_tbs_to_tbh10')[0])

    def test_pb(self):
        params = {"place": "parallel", "text": "iphone", "debug": "1"}

        response = self.report.request_bs_pb_apphost(params)
        self.check_response(response)
        self.assertEqual(len(response.get_report().TotalDocCount), 3)

    def test_pb_native(self):
        params = {"place": "parallel", "text": "iphone", "debug": "1", "apphost_response_type": ["MARKET"]}

        response = self.report.request_bs_pb_apphost(params)
        self.check_response(response)
        self.assertEqual(len(response.get_report().TotalDocCount), 3)

    def test_response_error(self):
        response = self.report.request_bs_pb_apphost({'place': 'unknown', 'text': 'nokia'}, strict=False)
        report = response.get_report()
        self.assertEqual(report.ErrorInfo.GotError, 1)
        self.assertEqual(report.ErrorInfo.Code, 1010)
        self.assertIn('unknown place (unknown)', report.ErrorInfo.Text)

        self.error_log.expect(code=1010, message='unknown place (unknown)').times(1)


if __name__ == '__main__':
    main()
