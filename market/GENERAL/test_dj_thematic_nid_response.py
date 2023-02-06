#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import BlueOffer, HyperCategory, MarketSku, Model, NavCategory, Offer, ReportState
from core.testcase import TestCase, main
from core.dj import DjModel

import six

if six.PY3:
    import urllib.parse as urlparse
else:
    import urlparse


class T(TestCase):
    @classmethod
    def generate_report_state(cls, models):
        rs = ReportState.create()
        for m in models:
            doc = rs.search_state.nailed_docs.add()
            doc.model_id = str(m)
        rs.search_state.nailed_docs_from_recom_morda = True
        return ReportState.serialize(rs).replace('=', ',')

    @classmethod
    def prepare(cls):
        cls.settings.set_default_reqid = False

        cls.index.hypertree = [HyperCategory(hid=200 + i, name='HID_{}'.format(i)) for i in range(12)]

        cls.index.navtree = [
            NavCategory(
                nid=123,
                is_blue=True,
                name='Nid в заголовке',
                children=[
                    NavCategory(hid=200 + i, nid=100 + i, is_blue=True, name='NID_{}'.format(i)) for i in range(11)
                ],
            ),
            NavCategory(hid=211, nid=321, is_blue=True, name='Nid по hid-у в заголовке'),
        ]

        # hyperid, nid, hid
        models = [
            (6000, 100, 200),
            (6001, 101, 201),
            (6002, 102, 202),
            (6003, 103, 203),
            (6004, 104, 204),
            (6005, 105, 205),
            (6006, 106, 206),
            (6007, 107, 207),
            (6008, 108, 208),
            (6009, 109, 209),
            (6010, 110, 210),
            (6011, 102, 202),
            (6012, 102, 202),
        ]

        cls.index.models += [Model(hyperid=m, hid=h) for m, _, h in models]
        cls.index.offers += [Offer(hyperid=m) for m, _, _ in models]
        cls.index.mskus += [MarketSku(hyperid=m, sku=m * 10, blue_offers=[BlueOffer()]) for m, _, _ in models]

        cls.dj.on_request(exp='dj_links_experiment', yandexuid='41610').respond(
            [DjModel(id=6000 + m) for m in range(10)],
            title='Тестовая выдача place=dj_links',
            nid='22222222',
            url='/catalog/22222222/list',
        )

        cls.dj.on_request(exp='dj_links_experiment_short', yandexuid='41610').respond(
            [DjModel(id=6000 + m) for m in range(3)],
            title='Тестовая выдача place=dj_links',
            nid='22222223',
            url='/catalog/22222223/list',
        )

        cls.dj.on_request(exp='dj_experiment', yandexuid='41610').respond(
            [DjModel(id=6000 + m) for m in range(10)],
            title='Тестовая выдача place=dj',
            nid='33333333',
            url='/catalog/33333333/list',
        )

    def check_url(self, actual_url, expected_url):
        actual_url = urlparse.urlparse(actual_url)
        expected_url = urlparse.urlparse(expected_url)

        self.assertEqual(actual_url.path, expected_url.path)

        self.assertEqual(
            urlparse.parse_qs(actual_url.query),
            urlparse.parse_qs(expected_url.query),
        )

    def test_dj_links_thematic_nid_link(self):
        def _rs(*models):
            return self.generate_report_state(models=models)

        response = self.report.request_json(
            'place=dj_links&dj-place=dj_links_experiment&yandexuid=41610&numdoc=6&allow-collapsing=true&rearr-factors=market_pin_offers_to_rs=0'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'title': 'Тестовая выдача place=dj_links',
                    'link': {'params': {'nid': 22222222}},
                }
            },
        )

        thematic_url = '/catalog/22222222/list?rs={rs}&tl=1'.format(
            rs=_rs(6000, 6001, 6002, 6003, 6004, 6005, 6006, 6007, 6008, 6009)
        )
        self.check_url(response['search']['link']['url'], thematic_url)

    def test_dj_links_short_thematic_nid_link(self):
        def _rs(*models):
            return self.generate_report_state(models=models)

        response = self.report.request_json(
            'place=dj_links&dj-place=dj_links_experiment_short&yandexuid=41610&numdoc=6&allow-collapsing=true&rearr-factors=market_pin_offers_to_rs=0'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'title': 'Тестовая выдача place=dj_links',
                    'link': {'params': {'nid': 22222223}},
                }
            },
        )

        thematic_url = '/catalog/22222223/list?rs={rs};tl=1'.format(rs=_rs(6000, 6001, 6002))
        self.check_url(response['search']['link']['url'], thematic_url)

    def test_dj_thematic_nid_link(self):
        def _rs(*models):
            return self.generate_report_state(models=models)

        response = self.report.request_json(
            'place=dj&dj-place=dj_experiment&yandexuid=41610&numdoc=6&allow-collapsing=true&rearr-factors=market_pin_offers_to_rs=0'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'title': 'Тестовая выдача place=dj',
                    'link': {'params': {'nid': 33333333}},
                }
            },
        )

        thematic_url = '/catalog/33333333/list?rs={rs};tl=1'.format(rs=_rs(6000, 6001, 6002, 6003, 6004, 6005))
        self.check_url(response['search']['link']['url'], thematic_url)


if __name__ == '__main__':
    main()
