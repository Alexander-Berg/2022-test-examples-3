#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import BlueOffer, HyperCategory, HyperCategoryType, MarketSku, Model, ReportState
from core.testcase import TestCase, main
from core.dj import DjModel
from core.types.autogen import b64url_md5

import six

if six.PY3:
    import urllib.parse as urlparse
else:
    import urlparse


class T(TestCase):
    @classmethod
    def generate_report_state(cls, docs, thematic_id=None, topic=None, range=None):
        rs = ReportState.create()
        for d in docs:
            doc = rs.search_state.nailed_docs.add()
            doc.model_id = str(d[0])
            doc.ware_id = str(d[1])
        rs.search_state.nailed_docs_from_recom_morda = True
        if thematic_id is not None:
            rs.search_state.thematic_id = thematic_id
        if topic is not None:
            rs.search_state.topic = topic
        if range is not None:
            rs.search_state.range = range
        return ReportState.serialize(rs).replace('=', ',')

    @classmethod
    def prepare(cls):
        cls.settings.set_default_reqid = False

        HID = 54321
        cls.index.hypertree += [HyperCategory(hid=HID, output_type=HyperCategoryType.GURU)]

        blue_offers = [
            BlueOffer(offerid='shop_sku_' + str(id), feedid=id, waremd5=b64url_md5('blue-{}'.format(id)))
            for id in range(1, 9)
        ]

        # models
        cls.index.models += [
            Model(hyperid=1234, hid=HID),
            Model(hyperid=1235, hid=HID),
            Model(hyperid=1236, hid=HID),
            Model(hyperid=1237, hid=HID),
            Model(hyperid=1238, hid=HID),
            Model(hyperid=1239, hid=HID),  # эту модель не рекомендует dj. Не должно быть в выдаче
            Model(hyperid=1241, hid=HID),
        ]

        # market skus
        cls.index.mskus += [
            MarketSku(title='dj_blue_market_sku_1', hyperid=1234, sku=11200001, blue_offers=[blue_offers[0]]),
            MarketSku(title='dj_blue_market_sku_2', hyperid=1235, sku=11200002, blue_offers=[blue_offers[1]]),
            MarketSku(title='dj_blue_market_sku_3', hyperid=1236, sku=11200003, blue_offers=[blue_offers[3]]),
            MarketSku(title='dj_blue_market_sku_4', hyperid=1237, sku=11200004, blue_offers=[blue_offers[4]]),
            MarketSku(title='dj_blue_market_sku_5', hyperid=1239, sku=11200005, blue_offers=[blue_offers[6]]),
            MarketSku(title='dj_blue_market_sku6', hyperid=1241, sku=11200006, blue_offers=[blue_offers[7]]),
        ]

        recommended_models = [
            DjModel(id="1241", title='model#1241'),
            DjModel(id="1240", title='model#1240'),  # модель не нашлась, не попадёт в выдачу
            DjModel(id="1238", title='model#1238'),  # нет офферов для модели, не попадёт в выдачу
            DjModel(id="1237", title='model#1237'),
            DjModel(id="1236", title='model#1236'),
            DjModel(id="1235", title='model#1235'),
            DjModel(id="1234", title='model#1234'),
        ]

        recommended_models1 = [
            DjModel(id="1241", title='model#1241'),
            DjModel(id="1237", title='model#1237'),
        ]

        recommended_models2 = [
            DjModel(id="1236", title='model#1236'),
            DjModel(id="1235", title='model#1235'),
        ]

        cls.dj.on_request(exp='dj_experiment', yandexuid='41610').respond(
            recommended_models, title='Тестовая выдача place=dj', nid='33333333', url='/catalog/33333333/list'
        )

        cls.dj.on_request(exp='dj_experiment_pinned', yandexuid='41610').respond(
            recommended_models1, title='Тестовая выдача place=dj', nid='33333333'
        )

        cls.dj.on_request(exp='dj_experiment_pinned_filter', yandexuid='41610').respond(
            recommended_models, title='Тестовая выдача place=dj', nid='33333333'
        )

        cls.dj.on_request(exp='dj_links_experiment', yandexuid='41610').respond(
            recommended_models, title='Тестовая выдача place=dj_links', nid='33333333', url='/catalog/33333333/list'
        )

        cls.dj.on_request(exp='dj_tovarborki_experiment', yandexuid='41610').respond(
            recommended_models1,
            title='Тестовая выдача товаборок',
            thematic_id='1000001',
            url='/feedlist/',
            topic='topic2',
            range='range1',
        )

        cls.dj.on_request(exp='dj_not_send_topic_and_range', yandexuid='41610').respond(
            recommended_models1,
            title='Тестовая выдача без топика и рэнджа',
            url='/feedlist/',
        )

        cls.dj.on_request(exp='dj_tovarborki_experiment_landing', yandexuid='41611').respond(
            recommended_models2, title='Тестовая выдача товаборок лендинга', thematic_id='1000001', url='/feedlist/'
        )

    def check_url(self, actual_url, expected_url):
        actual_url = urlparse.urlparse(actual_url)
        expected_url = urlparse.urlparse(expected_url)

        self.assertEqual(actual_url.path, expected_url.path)

        self.assertEqual(
            urlparse.parse_qs(actual_url.query),
            urlparse.parse_qs(expected_url.query),
        )

    # this test checks if offer pinning works in place=dj uner the flag market_pin_offers_to_rs
    def test_dj_pin_link(self):
        def _rs(*docs):
            return self.generate_report_state(docs=docs[0])

        response = self.report.request_json(
            'place=dj&dj-place=dj_experiment&yandexuid=41610&numdoc=6&allow-collapsing=true'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    "results": [
                        {'entity': 'product', 'id': 1241},
                        {'entity': 'product', 'id': 1237},
                        {'entity': 'product', 'id': 1236},
                        {'entity': 'product', 'id': 1235},
                        {'entity': 'product', 'id': 1234},
                    ]
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        docs = (
            (1241, b64url_md5('blue-{}'.format(8))),
            (1237, b64url_md5('blue-{}'.format(5))),
            (1236, b64url_md5('blue-{}'.format(4))),
            (1235, b64url_md5('blue-{}'.format(2))),
            (1234, b64url_md5('blue-{}'.format(1))),
        )
        print(docs)

        # print ([b64url_md5('blue-{}'.format(id))] for id in range(1,9))
        print(list(b64url_md5('blue-{}'.format(id)) for id in range(1, 9)))

        thematic_url = '/catalog/33333333/list?rs={rs}&tl=1'.format(rs=_rs(docs))
        self.check_url(response['search']['link']['url'], thematic_url)

    # this test checks if offer pinning works in place=dj_links uner the flag market_pin_offers_to_rs
    def test_dj_links_pin_link(self):
        def _rs(*docs):
            return self.generate_report_state(docs=docs[0])

        response = self.report.request_json(
            'place=dj&dj-place=dj_links_experiment&yandexuid=41610&numdoc=6&allow-collapsing=true'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    "results": [
                        {'entity': 'product', 'id': 1241},
                        {'entity': 'product', 'id': 1237},
                        {'entity': 'product', 'id': 1236},
                        {'entity': 'product', 'id': 1235},
                        {'entity': 'product', 'id': 1234},
                    ]
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        docs = (
            (1241, b64url_md5('blue-{}'.format(8))),
            (1237, b64url_md5('blue-{}'.format(5))),
            (1236, b64url_md5('blue-{}'.format(4))),
            (1235, b64url_md5('blue-{}'.format(2))),
            (1234, b64url_md5('blue-{}'.format(1))),
        )

        thematic_url = '/catalog/33333333/list?rs={rs}&tl=1'.format(rs=_rs(docs))
        self.check_url(response['search']['link']['url'], thematic_url)

    # in this test we unpin the models (1234, 1235)(decoded in rs) beforehand to recommended models
    def test_dj_pin_unpack(self):
        def _rs(*docs):
            return self.generate_report_state(docs=docs[0])

        response = self.report.request_json(
            'place=dj&dj-place=dj_experiment_pinned&yandexuid=41610&numdoc=6&allow-collapsing=true&rs=eJwz0vMS4eIx1CvOyC-IL84ujTcUYjE0MjYBiRojRI3BoqYJjAAqJwwl&rearr-factors=market_pin_offers_to_rs=0'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    "results": [
                        {'entity': 'product', 'id': 1234},
                        {'entity': 'product', 'id': 1235},
                        {'entity': 'product', 'id': 1241},
                        {'entity': 'product', 'id': 1237},
                    ]
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    # in this test we check if unpinned models are filtered from recommended models(in report)
    def test_dj_pin_unpack_filtered(self):
        def _rs(*docs):
            return self.generate_report_state(docs=docs[0])

        response = self.report.request_json(
            'place=dj&dj-place=dj_experiment_pinned_filter&yandexuid=41610&numdoc=6&'
            'allow-collapsing=true&rs=eJwz0vMS4eIx1CvOyC-IL84ujTcUYjE0MjYBiRojRI3BoqYJjAAqJwwl&rearr-factors=market_pin_offers_to_rs=0'  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    "results": [
                        {'entity': 'product', 'id': 1234},
                        {'entity': 'product', 'id': 1235},
                        {'entity': 'product', 'id': 1241},
                        {'entity': 'product', 'id': 1237},
                        {'entity': 'product', 'id': 1236},
                    ]
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    # test
    def test_dj_thematic_id_link(self):
        def _rs(*docs):
            return self.generate_report_state(docs=docs[0], thematic_id=1000001, range='range1', topic='topic2')

        response = self.report.request_json(
            'place=dj&dj-place=dj_tovarborki_experiment&yandexuid=41610&numdoc=6&allow-collapsing=true'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    "results": [
                        {'entity': 'product', 'id': 1241},
                        {'entity': 'product', 'id': 1237},
                    ],
                    'link': {
                        'params': {'thematic_id': 1000001, 'topic': 'topic2', 'range': 'range1'},
                    },
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        docs = (
            (1241, b64url_md5('blue-{}'.format(8))),
            (1237, b64url_md5('blue-{}'.format(5))),
        )
        print(docs)
        print(list(b64url_md5('blue-{}'.format(id)) for id in range(1, 9)))

        thematic_url = '/feedlist/?rs={rs}&tl=1'.format(rs=_rs(docs))
        self.check_url(response['search']['link']['url'], thematic_url)

    # we use rs from prevous case, that is supposed to store thematic_id, and two models - 1241, 1237. The result then should contiin 4 models(2 pinned, 2 from experiment)
    def test_dj_tovarborki_pin_unpack(self):

        response = self.report.request_json(
            'place=dj&dj-place=dj_tovarborki_experiment_landing&yandexuid=41611&numdoc=6&allow-collapsing=true'
            '&rs=eJwzcveS4xIzMjHxCggrDHTMdg2PSM1xNEpNiUgqF2IxNDIxBMmHZOQ6GnhHVeX4VeRU6ZqkVwSEpgY5guSNzRMYGxgPttgCAN1FFIc,&rearr-factors=market_pin_offers_to_rs=0'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    "results": [
                        {'entity': 'product', 'id': 1241},
                        {'entity': 'product', 'id': 1237},
                        {'entity': 'product', 'id': 1236},
                        {'entity': 'product', 'id': 1235},
                    ]
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_cgi_params_returned_when_dj_not(self):
        def _rs(*docs):
            return self.generate_report_state(docs=docs[0], range='range9', topic='topic9')

        response = self.report.request_json(
            'place=dj&dj-place=dj_not_send_topic_and_range&yandexuid=41610&numdoc=6&allow-collapsing=true&topic=topic9&range=range9'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    "results": [
                        {'entity': 'product', 'id': 1241},
                        {'entity': 'product', 'id': 1237},
                    ],
                    'link': {
                        'params': {'topic': 'topic9', 'range': 'range9'},
                    },
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        docs = (
            (1241, b64url_md5('blue-{}'.format(8))),
            (1237, b64url_md5('blue-{}'.format(5))),
        )

        thematic_url = '/feedlist/?rs={rs}&tl=1'.format(rs=_rs(docs))
        self.check_url(response['search']['link']['url'], thematic_url)


if __name__ == '__main__':
    main()
