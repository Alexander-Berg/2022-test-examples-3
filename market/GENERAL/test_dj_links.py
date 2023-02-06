#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import BlueOffer, HyperCategory, MarketSku, Model, NavCategory, Offer, ReportState
from core.testcase import TestCase, main
from core.dj import DjModel
from core.matcher import Absent, NotEmpty, NotEmptyList

import six

if six.PY3:
    import urllib.parse as urlparse
else:
    import urlparse


class T(TestCase):
    @classmethod
    def generate_report_state(cls, models, thematic_id=None, topic=None, range=None):
        rs = ReportState.create()
        for m in models:
            doc = rs.search_state.nailed_docs.add()
            doc.model_id = str(m)
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

        cls.index.hypertree = [HyperCategory(hid=200 + i, name='HID_{}'.format(i)) for i in range(12)]

        cls.index.navtree = [
            NavCategory(
                nid=123,
                is_blue=True,
                children=[
                    NavCategory(hid=200 + i, nid=100 + i, is_blue=True, name='NID_{}'.format(i)) for i in range(11)
                ],
            ),
            NavCategory(hid=211, nid=321, is_blue=True),
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
            (6013, 102, 102),
        ]

        cls.index.models += [Model(hyperid=m, hid=h) for m, _, h in models]
        cls.index.offers += [Offer(hyperid=m) for m, _, _ in models]
        cls.index.mskus += [MarketSku(hyperid=m, sku=m * 10, blue_offers=[BlueOffer()]) for m, _, _ in models]

        cls.dj.on_request(exp='links_params', yandexuid='41610').respond(
            [
                DjModel(
                    id=6000, attributes={'title': 'Модель без названия', 'url': 'http://dev.null/', 'noPin': '1'}
                ),  # Название и url приходят из dj
                DjModel(
                    id=6001, attributes={'nid': '101'}
                ),  # Название и ссылка будут сформированы по переданному nid-у
                DjModel(
                    id=6002, attributes={'hid': '202'}
                ),  # Название и ссылка будут сформированы по переданному hid-у
                DjModel(id=6003),  # Название и ссылка будут сформированы по nid-у и хиду модели
                DjModel(
                    id=6004, attributes={'nid': '104', 'title': 'Всё для отдыха'}
                ),  # title и nid, переданные из dj имеют приоритет над нидами, найденными для модели
                DjModel(
                    id=6005, attributes={'glfilter': '7893318=12902528;4899804=13979876'}
                ),  # можно добавлять gl-фильтры из dj
            ],
            title='Тестовая выдача place=dj_links',
        )

        cls.dj.on_request(exp='links_unique', yandexuid='41610').respond(
            [
                # 3 модели у которых nid = 102 и hid = 202 - должна сработать уникализация
                DjModel(id=6012, attributes={'hid': '202'}),
                DjModel(id=6011, attributes={'nid': '102'}),
                DjModel(id=6002),
                DjModel(id=6006),
                DjModel(id=6007),
                DjModel(id=6008),
                DjModel(id=6009),
                DjModel(id=6010),
            ],
            nid=123,
        )  # генерация заголовка выдачи по nid-у

        # слишком мало результатов в ответе
        cls.dj.on_request(exp='links_few', yandexuid='41610').respond(
            [
                DjModel(id=6007),
                DjModel(id=6008),
                DjModel(id=6009),
                DjModel(id=6010),
                DjModel(id=6011),
            ],
            title='Пустая выдача',
        )

        # Выдача для запроса с параметром numdoc=4
        cls.dj.on_request(exp='links_numdoc_4', yandexuid='41610').respond(
            [
                DjModel(id=6007),
                DjModel(id=6008),
                DjModel(id=6009),
                DjModel(id=6010),
                DjModel(id=6011),
            ],
            title='Выдача для numdoc=4',
        )

        # Выдача для параметров с numdoc=4. Будет пустой, тк слишком мало результатов
        cls.dj.on_request(exp='links_numdoc_4_few', yandexuid='41610').respond(
            [
                DjModel(id=6007),
                DjModel(id=6008),
                DjModel(id=6009),
            ],
            title='Пустая выдача для numdoc=4',
        )

        # Выдача для дефолтного значения numdoc (=6)
        cls.dj.on_request(exp='links_numdoc_default', yandexuid='41610').respond(
            [
                DjModel(id=6006),
                DjModel(id=6007),
                DjModel(id=6008),
                DjModel(id=6009),
                DjModel(id=6010),
                DjModel(id=6011),
                DjModel(id=6012),
            ],
            title='Выдача для дефолтного значения numdoc',
        )

        some_models = [
            DjModel(id=6002),
            DjModel(id=6006),
            DjModel(id=6007),
            DjModel(id=6008),
            DjModel(id=6009),
            DjModel(id=6010),
        ]
        cls.dj.on_request(exp='links_by_thematic_id', yandexuid='41610').respond(
            some_models,
            thematic_id=11222333,
            hid=211,
            url='/catalog/321/list',
            url_endpoint='categories',
            topic='topic1',
            range='range1',
        )
        cls.dj.on_request(exp='links_only_thematic_id', yandexuid='41610').respond(
            some_models,
            thematic_id=11222333,
            title='Some thematic title',
            nid=102,
            url='/catalog/102/list',
            url_endpoint='categories',
            topic='topic2',
            range='range2',
        )

        cls.dj.on_request(exp='links_category_picture', yandexuid='41610').respond(
            [
                DjModel(
                    id=6013,
                    attributes={
                        'nid': '102',
                        'title': 'Цветы, букеты, композиции',
                        'category_picture': """{
                            'namespace': 'mpic',
                            'height': 168,
                            'key': 'tsvety__bukety__kompozitsii.jpg',
                            'width': 264,
                            'groupId': 5235448
                        }""",
                    },
                ),
            ],
            title='Выдача с категорийной картинкой',
        )

    def check_url(self, actual_url, expected_url):
        actual_url = urlparse.urlparse(actual_url)
        expected_url = urlparse.urlparse(expected_url)

        self.assertEqual(actual_url.path, expected_url.path)

        self.assertEqual(
            urlparse.parse_qs(actual_url.query),
            urlparse.parse_qs(expected_url.query),
        )

    def get_item_result(self, idx, name, models=None, url=None, endpoint=None, link_params=None):

        params = None
        if link_params is not None:
            if isinstance(link_params, dict):
                params = {}
                if models:
                    params['rs'] = self.generate_report_state(models=models)
                    params['models'] = ','.join(models)
                params.update(link_params)
            else:
                params = link_params

        result = {
            'entity': 'formula',
            'index': idx,
            'id': NotEmpty(),
            'name': name,
            'pictures': NotEmptyList(),
        }
        if params or url or endpoint:
            result['link'] = {
                'url': NotEmpty() if url is None else url,
                'urlEndpoint': 'categories' if endpoint is None else endpoint,
                'params': params,
            }
        return result

    def test_link_params(self):
        def _rs(*models):
            return self.generate_report_state(models=models)

        response = self.report.request_json(
            'place=dj_links&dj-place=links_params&yandexuid=41610&numdoc=6&allow-collapsing=true&rearr-factors=market_pin_offers_to_rs=0'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'title': 'Тестовая выдача place=dj_links',
                    'recomParams': {
                        'type-of-results': 'thematic_model_picture',
                    },
                    'results': [
                        self.get_item_result(
                            0, 'Модель без названия', models=['6000'], link_params=dict(hid=200, nid=100)
                        ),
                        self.get_item_result(1, 'NID_1', models=['6001'], link_params=dict(nid=101)),
                        self.get_item_result(2, 'NID_2', models=['6002'], link_params=dict(hid=202, nid=102)),
                        self.get_item_result(3, 'NID_3', models=['6003'], link_params=dict(hid=203, nid=103)),
                        self.get_item_result(4, 'Всё для отдыха', models=['6004'], link_params=dict(nid=104)),
                        self.get_item_result(
                            5,
                            'NID_5',
                            models=['6005'],
                            link_params=dict(hid=205, nid=105, glfilter='7893318=12902528;4899804=13979876'),
                        ),
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        expected_urls = [
            'http://dev.null/?hid=200&rs={rs}&tcl=1'.format(rs=_rs(6000)),
            '/catalog/101/list?rs={rs}&tcl=1'.format(rs=_rs(6001)),
            '/catalog/102/list?hid=202&rs={rs}&tcl=1'.format(rs=_rs(6002)),
            '/catalog/103/list?hid=203&rs={rs}&tcl=1'.format(rs=_rs(6003)),
            '/catalog/104/list?rs={rs}&tcl=1'.format(rs=_rs(6004)),
            '/catalog/105/list?hid=205&glfilter=7893318=12902528&glfilter=4899804=13979876&rs={rs}&tcl=1'.format(
                rs=_rs(6005)
            ),
        ]

        for idx, url in enumerate(expected_urls):
            self.check_url(response['search']['results'][idx]['link']['url'], url)

    def test_unique_nids(self):
        response = self.report.request_json(
            'place=dj_links&dj-place=links_unique&yandexuid=41610&numdoc=6&allow-collapsing=true'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'recomParams': {
                        'type-of-results': 'thematic_model_picture',
                    },
                    'results': [
                        self.get_item_result(0, 'NID_2'),
                        self.get_item_result(1, 'NID_6'),
                        self.get_item_result(2, 'NID_7'),
                        self.get_item_result(3, 'NID_8'),
                        self.get_item_result(4, 'NID_9'),
                        self.get_item_result(5, 'NID_10'),
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        self.assertEqual(response['search']['results'][0]['link']['params']['models'], '6012')

    def test_thematic_id(self):
        def _rs(*models):
            return self.generate_report_state(models=models, thematic_id=11222333, range='range1', topic='topic1')

        response = self.report.request_json(
            'place=dj_links&dj-place=links_by_thematic_id&yandexuid=41610&numdoc=6&allow-collapsing=true&rearr-factors=market_pin_offers_to_rs=0'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'recomParams': {
                        'type-of-results': 'thematic_model_picture',
                    },
                    'link': {
                        'urlEndpoint': 'categories',
                        'params': {'thematic_id': 11222333, 'topic': 'topic1', 'range': 'range1'},
                    },
                    'results': [
                        self.get_item_result(0, 'NID_2'),
                        self.get_item_result(1, 'NID_6'),
                        self.get_item_result(2, 'NID_7'),
                        self.get_item_result(3, 'NID_8'),
                        self.get_item_result(4, 'NID_9'),
                        self.get_item_result(5, 'NID_10'),
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        thematic_url = '/catalog/321/list?rs={rs}&hid=211&tl=1'.format(rs=_rs(6002, 6006, 6007, 6008, 6009, 6010))
        self.check_url(response['search']['link']['url'], thematic_url)

        response = self.report.request_json(
            'place=dj_links&dj-place=links_only_thematic_id&yandexuid=41610&numdoc=6&allow-collapsing=true'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'link': {
                        'urlEndpoint': "categories",
                        'params': {'thematic_id': 11222333, 'topic': 'topic2', 'range': 'range2'},
                    },
                    'title': 'Some thematic title',
                    'recomParams': {
                        'type-of-results': 'thematic_model_picture',
                    },
                    'results': [
                        self.get_item_result(0, 'NID_2'),
                        self.get_item_result(1, 'NID_6'),
                        self.get_item_result(2, 'NID_7'),
                        self.get_item_result(3, 'NID_8'),
                        self.get_item_result(4, 'NID_9'),
                        self.get_item_result(5, 'NID_10'),
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_empty_response_for_at_most_5_items(self):
        response = self.report.request_json(
            'place=dj_links&dj-place=links_few&yandexuid=41610&numdoc=6&allow-collapsing=true'
        )
        self.assertFragmentIn(
            response,
            {'search': {'title': 'Пустая выдача', 'results': []}},
            preserve_order=True,
            allow_different_len=False,
        )

    def test_numdoc_4_param(self):
        response = self.report.request_json(
            'place=dj_links&dj-place=links_numdoc_4&yandexuid=41610&numdoc=4&allow-collapsing=true'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'title': "Выдача для numdoc=4",
                    'recomParams': {'type-of-results': "thematic_model_picture"},
                    'results': [
                        self.get_item_result(0, 'NID_7'),
                        self.get_item_result(1, 'NID_8'),
                        self.get_item_result(2, 'NID_9'),
                        self.get_item_result(3, 'NID_10'),
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_numdoc_4_param_for_3_items(self):
        response = self.report.request_json(
            'place=dj_links&dj-place=links_numdoc_4_few&yandexuid=41610&numdoc=4&allow-collapsing=true&rearr-factors=market_pin_offers_to_rs=0'
        )
        self.assertFragmentIn(
            response,
            {'search': {'title': "Пустая выдача для numdoc=4", 'results': []}},
            preserve_order=True,
            allow_different_len=False,
        )

    def test_numdoc_default_value(self):
        response = self.report.request_json(
            'place=dj_links&dj-place=links_numdoc_default&yandexuid=41610&allow-collapsing=true'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'title': "Выдача для дефолтного значения numdoc",
                    'recomParams': {
                        'type-of-results': "thematic_model_picture",
                    },
                    'results': [
                        self.get_item_result(0, 'NID_6'),
                        self.get_item_result(1, 'NID_7'),
                        self.get_item_result(2, 'NID_8'),
                        self.get_item_result(3, 'NID_9'),
                        self.get_item_result(4, 'NID_10'),
                        self.get_item_result(5, 'NID_2'),
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_category_pictures(self):
        response = self.report.request_json(
            'place=dj_links&dj-place=links_category_picture&yandexuid=41610&allow-collapsing=true&numdoc=1&new-picture-format=1'
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "picture",
                "original": {
                    "groupId": 5235448,
                    "height": 168,
                    "key": "tsvety__bukety__kompozitsii.jpg",
                    "namespace": "mpic",
                    "width": 264,
                },
                "signatures": [],
            },
        )
        self.assertFragmentIn(
            response, {"picture-source": "custom", "link": {"params": {"models": "6013", "nid": 102}}}
        )
        self.assertFragmentIn(
            response,
            {"title": "Выдача с категорийной картинкой", 'recomParams': {'type-of-results': 'thematic_custom_picture'}},
        )

    @classmethod
    def prepare_category_like(cls):
        cls.dj.on_request(yandexuid='1010').respond(
            models=[DjModel(id="123")],
            title='Выдача с лайками для place=dj_links',
            djid=None,
            recommended_queries=None,
            items=None,
            liked=True,
        )
        cls.dj.on_request(yandexuid='10100').respond(
            models=[DjModel(id="123")],
            title='Выдача без лайков для place=dj_links',
            djid=None,
            recommended_queries=None,
            items=None,
            liked=False,
        )

    def test_category_like(self):
        response = self.report.request_json(
            'place=dj_links&rgb=blue&dj-place=links_unique&yandexuid=1010&allow-collapsing=true&numdoc=1'
        )
        self.assertFragmentIn(
            response,
            {
                "title": "Выдача с лайками для place=dj_links",
                "recomParams": {"liked": True, "type-of-results": "thematic_custom_picture"},
            },
        )
        response = self.report.request_json(
            'place=dj_links&rgb=blue&dj-place=links_unique&yandexuid=10100&allow-collapsing=true&numdoc=1'
        )
        self.assertFragmentIn(
            response,
            {
                "title": "Выдача без лайков для place=dj_links",
                "recomParams": {"liked": Absent(), "type-of-results": "thematic_custom_picture"},
            },
        )

    @classmethod
    def prepare_dj_links_metrics(cls):
        cls.dj.on_request(exp='metrics', yandexuid='700').respond(
            [
                DjModel(id=7000),
            ]
        )
        cls.dj.on_request(exp='metrics', yandexuid='701').respond([])

    def test_dj_links_metrics(self):
        def check_unistat_data(exp, yandexuid, error_expected):
            before = self.report.request_tass()
            self.report.request_json('place=dj_links&dj-place={}&yandexuid={}'.format(exp, yandexuid))
            after = self.report.request_tass_or_wait(wait_hole='dj_links_exp_{}_dj_request_time_hgram'.format(exp))

            self.assertEqual(
                before.get('dj_links_exp_{}_request_count_dmmm'.format(exp), 0) + 1,
                after.get('dj_links_exp_{}_request_count_dmmm'.format(exp), 0),
            )
            self.assertEqual(
                before.get('dj_links_exp_{}_error_count_dmmm'.format(exp), 0) + int(error_expected),
                after.get('dj_links_exp_{}_error_count_dmmm'.format(exp), 0),
            )
            self.assertIn('dj_links_exp_{}_request_time_hgram'.format(exp), after.keys())
            self.assertIn('dj_links_exp_{}_dj_request_time_hgram'.format(exp), after.keys())

        self.error_log.ignore(code=3787)

        check_unistat_data('metrics', 700, error_expected=False)
        check_unistat_data('metrics', 701, error_expected=False)
        check_unistat_data('null', 700, error_expected=True)

    def test_without_do(self):
        request = 'place=dj_links&djid=links_params&yandexuid=41610&debug=1'
        default_offer_subrequest = {
            'metasearch': {
                'name': '',
                'subrequests': [
                    'debug',
                    {
                        'metasearch': {'name': 'Default offer'},
                    },
                ],
            }
        }
        for cgi_param, rearr_param, is_default_offer_subrequest_expected in [
            ['&allow-collapsing=0', '&rearr-factors=recom_dj_links_without_do=0', True],
            ['&allow-collapsing=0', '&rearr-factors=recom_dj_links_without_do=1', True],
            ['&allow-collapsing=1', '&rearr-factors=recom_dj_links_without_do=0', True],
            ['&allow-collapsing=1', '&rearr-factors=recom_dj_links_without_do=1', False],
            ['', '&rearr-factors=recom_dj_links_without_do=0', True],
            ['', '&rearr-factors=recom_dj_links_without_do=1', False],
        ]:
            response = self.report.request_json(request + cgi_param + rearr_param)
            check_fn = self.assertFragmentIn if is_default_offer_subrequest_expected else self.assertFragmentNotIn
            check_fn(response, default_offer_subrequest)


if __name__ == '__main__':
    main()
