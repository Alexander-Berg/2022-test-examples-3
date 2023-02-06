#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types import (
    GLParam,
    HyperCategory,
    HyperCategoryType,
    MnPlace,
    Model,
    NewShopRating,
    Offer,
    Shop,
    UGCItem,
    Vendor,
)
from core.testcase import TestCase, main
from core.matcher import (
    Contains,
    ElementCount,
    GreaterEq,
    LessEq,
)
from core.blender_bundles import get_supported_incuts_cgi, create_blender_bundles


class Images:
    IMAGE = (
        '{"thumbnails": [{"densities": [{"url": "//avatars.mds.yandex.net/'
        'get-marketcms/1490511/img-0f5d09ca-e4a3-455f-83e3-feb1701eec24.jpeg/'
        'optimize", "id": "1", "entity": "density"}], "containerHeight": "625"'
        ', "averageColors": {"borders": {"top": "a7927f", "right": "bebbb3",'
        '"left": "b3a396", "bottom": "d0c3b7"}, "full": "b1a396"}, '
        '"containerWidth": "1000", "height": "437", "width": "700", "entity": '
        '"thumbnail", "id": "1000x625"}, {"densities": [{"url": "//avatars.mds'
        '.yandex.net/get-marketcms/1776516/img-41240ddb-9956-490c-8224-'
        '5d79e38e8815.jpeg/optimize", "id": "1", "entity": "density"}], '
        '"containerHeight": "313", "averageColors": {"borders": {"top": '
        '"a7917e", "right": "bfbbb4", "left": "b3a396", "bottom": "d0c3b7"}, '
        '"full": "b1a396"}, "containerWidth": "500", "height": "313", "width":'
        ' "500", "entity": "thumbnail", "id": "500x313"}, {"densities": [{"url'
        '": "//avatars.mds.yandex.net/get-marketcms/1668019/img-adf71cf8-70e5-'
        '4bfa-bc59-13e7fb2efdb1.jpeg/optimize", "id": "1", "entity": "density"'
        '}, {"url": "//avatars.mds.yandex.net/get-marketcms/1668019/img-'
        '00fad0ab-ed47-4781-907b-f4791069056b.jpeg/optimize", "id": "2", '
        '"entity": "density"}], "containerHeight": "188", "averageColors": '
        '{"borders": {"top": "a7917e", "right": "bebbb3", "left": "b3a396", '
        '"bottom": "d0c3b7"}, "full": "b1a396"}, "containerWidth": "300", '
        '"height": "188", "width": "300", "entity": "thumbnail", "id": '
        '"300x188"}], "url": "//avatars.mds.yandex.net/get-marketcms/1668019'
        '/img-190ed917-cb78-42e6-b2e5-c995774088d6.jpeg/optimize", "height": '
        '"500", "width": "700", "entity": "picture", "isNewTab": false}'
    )


class BlenderBundleConstPositon:
    BUNDLE_CONST_SEARCH_POSITION = '''
{{
    "incut_places": ["Search"],
    "incut_positions": [{row_position}],
    "incut_viewtypes": ["Gallery"],
    "incut_ids": ["default"],
    "result_scores": [
        {{
            "incut_place": "Search",
            "row_position": {row_position},
            "incut_viewtype": "Gallery",
            "incut_id": "default",
            "score": 1.0
        }}
    ],
    "calculator_type": "ConstPosition"
}}
'''

    BUNDLE_CONST_MANY_COMBINATIONS = '''
{
    "incut_positions": [1, 2, 3, 4, 15],
    "incut_place_viewtype_combinations" : {
        "Top": [
            "GalleryWithBanner", "VendorGallery"
        ],
        "Search": [
            "GalleryWithBanner", "Gallery"
        ]
    },
    "incut_ids": ["default"],
    "result_scores": [
        {
            "incut_place": "Search",
            "row_position": 1,
            "incut_viewtype": "Gallery",
            "incut_id": "default",
            "score": 0.99
        },
        {
            "incut_place": "Search",
            "row_position": 2,
            "incut_viewtype": "GalleryWithBanner",
            "incut_id": "default",
            "score": 0.97
        },
        {
            "incut_place": "Top",
            "row_position": 1,
            "incut_viewtype": "GalleryWithBanner",
            "incut_id": "default",
            "score": 1.0
        },
        {
            "incut_place": "Search",
            "row_position": 4,
            "incut_viewtype": "VendorGallery",
            "incut_id": "default",
            "score": 1.0
        }
    ],
    "calculator_type": "ConstPosition"
}
'''


class BlenderBundlesConfig:
    BUNDLES_CONFIG = """
{
    "INCLID_MATERIAL_ENTRYPOINTS" : {
        "client == frontend && platform == desktop" : {
            "bundle_name": "bundle_const_search_2.json"
        },
        "client == frontend && platform == touch && search_type == text && 1 == 1" : {
            "bundle_name": "bundle_const_search_8.json",
            "priority": 2
        },
        "client == frontend && platform == touch && search_type == text" : {
            "bundle_name": "bundle_const_search_2.json",
            "priority": -3
        },
        "client == android && search_type == text" : {
            "bundle_name": "bundle_const_search_8.json"
        },
        "client == android && search_type == text" : {
            "bundle_name": "bundle_const_search_8.json",
            "priority": 10
        },
        "broken_expression": {
            "bundle_name": "bundle_const_search_8.json"
        }
    },
    "INCLID_PREMIUM_ADS": {
        "client == frontend && platform == Desktop && search_type == text" : {
            "bundle_name": "bundle_const_search_2.json"
        },
        "1" : {
            "bundle_name": "bundle_const_search_2.json",
            "priority": -1
        }
    },
    "INCLID_RECOM_THEMATIC": {
        "client == frontend && platform == desktop && search_type == text" : {
            "bundle_name": "bundle_const_search_1.json",
            "bundle_name_list": "bundle_const_search_8.json",
            "bundle_name_grid": "bundle_const_search_3.json"
        },
        "client == frontend && platform == touch && search_type == text" : {
            "bundle_name_list": "bundle_const_search_8.json",
            "bundle_name": "bundle_const_search_1.json",
            "bundle_name_grid": "bundle_const_search_3.json"
        },
        "client == android && search_type == text" : {
            "bundle_name_grid": "bundle_const_search_3.json"
        },
        "client == ios && search_type == text" : {
            "bundle_name_list": "bundle_const_search_8.json"
        }
    },
    "INCLID_DISCOUNT": {
        "client == frontend && platform == desktop && search_type == text" : {
            "bundle_name": "bundle_const_search_1.json",
            "bundle_name_list": "bundle_const_search_8.json"
        },
        "client == frontend && platform == touch && search_type == text" : {
            "bundle_name": "bundle_const_search_1.json",
            "bundle_name_grid": "bundle_const_search_3.json"
        },
        "client == android && search_type == text" : {
            "bundle_name_grid": "bundle_const_search_3.json",
            "bundle_name_list": "bundle_const_search_8.json"
        }
    }
}
"""


def get_found_bundle_str(bundle_name, inclid):
    return "Bundle by name " + bundle_name + " selected for inClid=" + inclid


def get_found_bundle_by_place_and_viewtype_str(bundle_name, inclid, viewtype):
    return "bundle {name} for inClid={inclid} and search_viewtype={viewtype} matched with request expression".format(
        name=bundle_name, inclid=inclid, viewtype=viewtype
    )


def get_no_bundle_by_place_and_viewtype_str(inclid, viewtype):
    return "No bundle found for inClid=" + inclid + " and search_viewtype=" + viewtype


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.hypertree += [HyperCategory(hid=100, output_type=HyperCategoryType.GURU)]

        cls.index.models += [
            Model(hid=100, hyperid=1, title="Первая модель", ts=1),
        ]

    @classmethod
    def prepare_entrypoints_from_saas(cls):
        embs_mars = [0.3] * 50
        cls.saas_ugc.on_request(embs=embs_mars, search_size=100, top_size=50,).respond(
            items=[
                UGCItem(
                    relevance=988795808,
                    attributes={
                        'page_id': '56126',
                        'compiled_generation': '20200604_0600',
                        'pages_generation': '20200608_0800',
                        'title': 'Марс - еда или планета?',
                        'type': 'knowledge',
                        'image': Images.IMAGE,
                        'semantic_id': 'mars-the-best',
                    },
                )
            ]
        )
        cls.index.dssm.hard2_query_embedding.on(query='марс').set(*embs_mars)

    @classmethod
    def prepare_blender_cpa_shop_incut(cls):
        titles = ["Air", "Breath", "Fresh", "Oxy", "Воздух", "Дыши", "Вдох", "Выдох", "Кислород", "Небо"]
        cls.index.models += [
            Model(hid=66, hyperid=66 + i, title="Модель {}".format(titles[i]), ts=100020 + i) for i in range(1, 10)
        ]

        cls.index.shops += [
            Shop(
                fesh=66 + i, priority_region=213, shop_fee=100, cpa=Shop.CPA_REAL, name='CPA Shop {}'.format(titles[i])
            )
            for i in range(1, 10)
        ]

        cls.index.offers += [
            Offer(
                fesh=66 + i,
                hyperid=66 + i,
                hid=66,
                fee=90 + i,
                ts=100020 + i,
                price=100,
                cpa=Offer.CPA_REAL,
                title="Марс {}".format(titles[i]),
            )
            for i in range(1, 10)
        ]

        for i in range(1, 10):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100020 + i).respond(0.04)

    @staticmethod
    def get_request(params, rearr):
        def dict_to_str(data, separator):
            return str(separator).join("{}={}".format(str(k), str(v)) for (k, v) in data.iteritems())

        return "{}&rearr-factors={}".format(dict_to_str(params, '&'), dict_to_str(rearr, ';'))

    @classmethod
    def prepare_blender_cpa_shop_incut_filtering_gl(cls):
        cls.index.models += [  #
            Model(
                hid=170,
                hyperid=170 + i,
                vendor_id=2170,
                vbid=10,
            )
            for i in range(0, 10)
        ]
        cls.index.models += [  # второй вендор с большей ставкой
            Model(
                hid=170,
                hyperid=180 + i,
                vendor_id=2171,
                vbid=20,
            )
            for i in range(0, 10)
        ]

        cls.index.shops += [
            Shop(
                fesh=1070 + i,
                category=170,
                cpa=Shop.CPA_REAL,
                # половина с рейтингом 2 (1080-1089) , половина - 3 (1070-1079)
                new_shop_rating=NewShopRating(new_rating_total=(3.0 if (i < 10) else 2.0)),
            )
            for i in range(0, 20)
        ]

        titles = ['one', 'two', 'three', 'four', 'five', 'six', 'seven', 'eight', 'nine', 'ten']
        cls.index.offers += [
            Offer(
                fesh=1070 + i,
                hyperid=170 + i,
                hid=170,
                fee=90,
                price=100,
                cpa=Offer.CPA_REAL,
                title="Kia first {}".format(titles[i]),
                vendor_id=2170,
                glparams=[GLParam(param_id=7893318, value=2170)],
            )
            for i in range(0, 10)
        ]
        cls.index.offers += [
            Offer(
                fesh=1080 + i,
                hyperid=180 + i,
                hid=170,
                fee=100,
                price=100 + (10 * i),
                cpa=Offer.CPA_REAL,
                title="Kia second {}".format(titles[i]),
                vendor_id=2171,
                glparams=[GLParam(param_id=7893318, value=2171)],
            )
            for i in range(0, 10)
        ]

        cls.index.vendors += [
            Vendor(
                vendor_id=2170,
            ),
            Vendor(
                vendor_id=2171,
            ),
        ]

    def test_blender_cpa_shop_incut_filtering_gl(self):
        """
        https://st.yandex-team.ru/MARKETOUT-43676
        модификация GL фильтров для премиальной врезки
        """
        gl_brand = 7893318

        params = {
            "place": "blender",
            "text": "kia",
            'additional_entities': 'articles',
            'debug': 'da',
            'supported-incuts': get_supported_incuts_cgi(),
        }
        rearr = {
            'market_blender_cpa_shop_incut_enabled': 1,
            'market_premium_ads_gallery_min_num_doc_to_request_from_base': 1,
        }

        # простой запрос, где выигрывает вендор с большей ставкой
        response = self.report.request_json(T.get_request(params, rearr))
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'inClid': 2,
                            'items': [
                                {
                                    'slug': Contains("kia-second"),
                                }
                            ],
                        }
                    ]
                }
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'inClid': 2,
                            'items': [
                                {
                                    'slug': Contains("kia-first"),
                                }
                            ],
                        }
                    ]
                }
            },
        )

        # добавление gl фильтра (остается вендор с меньшей ставкой)
        params['glfilter'] = '{}:{}'.format(gl_brand, 2170)
        response = self.report.request_json(T.get_request(params, rearr))
        self.assertFragmentNotIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'inClid': 2,
                            'items': [
                                {
                                    'slug': Contains("kia-second"),
                                }
                            ],
                        }
                    ]
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'inClid': 2,
                            'items': [
                                {
                                    'slug': Contains("kia-first"),
                                }
                            ],
                        }
                    ]
                }
            },
        )

        # не учитывать gl фильтры по вендору (тогда опять выиграет второй вендор 2171)
        rearr['market_report_premium_incut_filter_gl_exclude'] = ":".join(
            str(i)
            for i in [
                gl_brand,
            ]
        )
        response = self.report.request_json(T.get_request(params, rearr))
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'inClid': 2,
                            'items': [
                                {
                                    'slug': Contains("kia-second"),
                                }
                            ],
                        }
                    ]
                }
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'inClid': 2,
                            'items': [
                                {
                                    'slug': Contains("kia-first"),
                                }
                            ],
                        }
                    ]
                }
            },
        )

        # не учитывать все gl фильтры (тогда опять выиграет второй вендор 2171)
        rearr['market_report_premium_incut_filter_gl_exclude'] = "*"
        response = self.report.request_json(T.get_request(params, rearr))
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'inClid': 2,
                            'items': [
                                {
                                    'slug': Contains("kia-second"),
                                }
                            ],
                        }
                    ]
                }
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'inClid': 2,
                            'items': [
                                {
                                    'slug': Contains("kia-first"),
                                }
                            ],
                        }
                    ]
                }
            },
        )

    def test_blender_cpa_shop_incut_filtering_price(self):
        """
        https://st.yandex-team.ru/MARKETOUT-43676
        модификация порогов цен для премиальной врезки
        """
        params = {
            "place": "blender",
            "text": "kia",
            'additional_entities': 'articles',
            'debug': 'da',
            'mcpricefrom': 120,
            'glfilter': '7893318:2171',  # только со вторым вендором работа
            'supported-incuts': get_supported_incuts_cgi(),
        }
        rearr = {
            'market_blender_cpa_shop_incut_enabled': 1,
        }

        # простой запрос с учетом порога по нижнему уровням
        # у вендора все офферы от 100 до 190 (с шагом 10)
        response = self.report.request_json(T.get_request(params, rearr))
        self.assertFragmentIn(
            response,
            {'incuts': {'results': [{'entity': 'searchIncut', 'inClid': 2, 'items': ElementCount(8)}]}},  # 120..190
        )
        self.assertFragmentIn(response, {'incuts': {'results': [{'items': [{'prices': {'value': GreaterEq(120)}}]}]}})

        # изменение нижнего порога цены
        rearr['market_report_premium_incut_filter_price_from'] = -10  # -10%
        response = self.report.request_json(T.get_request(params, rearr))
        self.assertFragmentIn(
            response,
            {'incuts': {'results': [{'entity': 'searchIncut', 'inClid': 2, 'items': ElementCount(9)}]}},  # 110..190
        )
        self.assertFragmentIn(response, {'incuts': {'results': [{'items': [{'prices': {'value': GreaterEq(110)}}]}]}})

        # установка верхнего порога
        params.pop('mcpricefrom')
        params['mcpriceto'] = 170

        response = self.report.request_json(T.get_request(params, rearr))
        self.assertFragmentIn(
            response,
            {'incuts': {'results': [{'entity': 'searchIncut', 'inClid': 2, 'items': ElementCount(8)}]}},  # 100..170
        )
        self.assertFragmentIn(response, {'incuts': {'results': [{'items': [{'prices': {'value': LessEq(170)}}]}]}})

        # изменение верхнего порога цены
        rearr['market_report_premium_incut_filter_price_to'] = 20  # +20%
        response = self.report.request_json(T.get_request(params, rearr))
        self.assertFragmentIn(
            response,
            {'incuts': {'results': [{'entity': 'searchIncut', 'inClid': 2, 'items': ElementCount(10)}]}},  # 100..190
        )
        incut_parent_show_uid = "04884192001117778888838002"
        self.show_log_tskv.expect(
            show_uid=incut_parent_show_uid, super_uid=incut_parent_show_uid, inclid=2, position=2, pp=18
        ).times(4)
        for pos in range(1, 9):
            self.show_log_tskv.expect(
                show_uid="04884192001117778888800{:03d}".format(pos),
                inclid=2,
                position=pos,
                super_uid=incut_parent_show_uid,
            ).times(4)
        self.show_log_tskv.expect(
            show_uid="04884192001117778888800{:03d}".format(9), inclid=2, position=9, super_uid=incut_parent_show_uid
        ).times(2)
        self.show_log_tskv.expect(
            show_uid="04884192001117778888800{:03d}".format(10), inclid=2, position=10, super_uid=incut_parent_show_uid
        ).times(1)

    def test_blender_cpa_shop_incut_filtering_shop(self):
        """
        https://st.yandex-team.ru/MARKETOUT-43676
        модификация фильтров по магазинам для премиальной врезки
        """
        params = {
            "place": "blender",
            "text": "kia",
            'additional_entities': 'articles',
            'debug': 'da',
            'glfilter': '7893318:2170',  # только со первым вендором работа
            'fesh': ','.join(str(1070 + i) for i in range(0, 8)),  # первые 8 магазинов
            'supported-incuts': get_supported_incuts_cgi(),
        }
        rearr = {
            'market_blender_cpa_shop_incut_enabled': 1,
        }

        # простой запрос с учетом фильтров по магазинам
        response = self.report.request_json(T.get_request(params, rearr))
        self.assertFragmentIn(
            response, {'incuts': {'results': [{'entity': 'searchIncut', 'inClid': 2, 'items': ElementCount(8)}]}}
        )
        self.assertFragmentIn(  # все магазины c 1070 - 1077
            response, {'incuts': {'results': [{'items': [{'shop': {'id': LessEq(1077)}}]}]}}
        )

        # отключение фильтрации по магазинам
        rearr['market_report_premium_incut_filter_shop_off'] = 1
        response = self.report.request_json(T.get_request(params, rearr))
        self.assertFragmentIn(
            response, {'incuts': {'results': [{'entity': 'searchIncut', 'inClid': 2, 'items': ElementCount(10)}]}}
        )
        self.assertFragmentIn(  # все магазины, начиная с 1070
            response, {'incuts': {'results': [{'items': [{'shop': {'id': GreaterEq(1070)}}]}]}}
        )

    def test_blender_cpa_shop_incut_filtering_shop_rating(self):
        """
        https://st.yandex-team.ru/MARKETOUT-43676
        модификация фильтра рейтинга магазина для премиальной врезки
        """
        params = {
            "place": "blender",
            "text": "kia",
            'additional_entities': 'articles',
            'debug': 'da',
            'qrfrom': 3,  # только магазины с рейтингом 3 и выше
            'supported-incuts': get_supported_incuts_cgi(),
        }
        rearr = {
            'market_blender_cpa_shop_incut_enabled': 1,
        }

        # простой запрос с учетом фильтров по магазинам
        response = self.report.request_json(T.get_request(params, rearr))
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'inClid': 2,
                            'items': [
                                {
                                    'slug': Contains("kia-first"),  # ставка меньше, но учитывается рейтинг магазина
                                }
                            ],
                        }
                    ]
                }
            },
        )
        self.assertFragmentIn(
            response, {'incuts': {'results': [{'items': [{'shop': {'qualityRating': GreaterEq(3)}}]}]}}
        )

        # отключение фильтров по рейтингу магазина
        rearr['market_report_premium_incut_filter_shop_rating_off'] = 1
        response = self.report.request_json(T.get_request(params, rearr))
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'inClid': 2,
                            'items': [
                                {
                                    'slug': Contains("kia-second"),
                                }
                            ],
                        }
                    ]
                }
            },
        )
        self.assertFragmentIn(
            response, {'incuts': {'results': [{'items': [{'shop': {'qualityRating': GreaterEq(2)}}]}]}}
        )

    @classmethod
    def prepare_blender_bundles_config(cls):
        cls.settings.formulas_path = create_blender_bundles(
            cls.meta_paths.testroot,
            BlenderBundlesConfig.BUNDLES_CONFIG,
            {
                "bundle_const_search_1.json": BlenderBundleConstPositon.BUNDLE_CONST_SEARCH_POSITION.format(
                    row_position=1
                ),
                "bundle_const_search_3.json": BlenderBundleConstPositon.BUNDLE_CONST_SEARCH_POSITION.format(
                    row_position=3
                ),
                "bundle_const_search_2.json": BlenderBundleConstPositon.BUNDLE_CONST_SEARCH_POSITION.format(
                    row_position=2
                ),
                "bundle_const_search_8.json": BlenderBundleConstPositon.BUNDLE_CONST_SEARCH_POSITION.format(
                    row_position=8
                ),
                "bundle_const_search_10.json": BlenderBundleConstPositon.BUNDLE_CONST_SEARCH_POSITION.format(
                    row_position=10
                ),  # нет в конфиге бандлов
                "bundle_const_search_11.json": BlenderBundleConstPositon.BUNDLE_CONST_SEARCH_POSITION.format(
                    row_position=11
                ),  # нет в конфиге бандлов
                "bundle_const_search_12.json": BlenderBundleConstPositon.BUNDLE_CONST_SEARCH_POSITION.format(
                    row_position=12
                ),  # нет в конфиге бандлов
                "bundle_many.json": BlenderBundleConstPositon.BUNDLE_CONST_MANY_COMBINATIONS,
            },
        )

    VIEW_TYPE_REQUEST_TEMPLATE = (
        "place=blender&text=марс&additional_entities=articles&debug=da&"
        + "supported-incuts="
        + get_supported_incuts_cgi()
        + "&"
        + "client={}&platform={}&{}"
        + "rearr-factors=market_blender_cpa_shop_incut_enabled=1;"
        + "market_ugc_saas_enabled=1;"
    )

    def test_bundles_config_with_view_type_all_names(self):
        """Проверяем применение выражения по типу вьюшки"""
        # Проверим что выбранные калькуляторы не зависят от текущего вью, найдены должны быть оба
        request_desktop_none = T.VIEW_TYPE_REQUEST_TEMPLATE.format("frontend", "desktop", "")
        request_desktop_list = T.VIEW_TYPE_REQUEST_TEMPLATE.format("frontend", "desktop", "viewtype=list&")
        request_desktop_grid = T.VIEW_TYPE_REQUEST_TEMPLATE.format(
            "frontend", "desktop", "viewtype=grid&columns-in-grid=3&"
        )
        # в проде должен быть либо "bundle_name" либо один из "bundle_name_list/grid"
        # но на всякий случай протестируем когда есть все три поля, нам везет и они сортируются в дереве как надо

        # для INCLID_PREMIUM_ADS результат всегда тот же независимо от view_type
        # если вью не указана должен выбраться лист по умолчанию, 8 - для листа, 7 - для грида

        expected = {
            "logicTrace": [
                Contains(
                    get_found_bundle_by_place_and_viewtype_str(
                        "bundle_const_search_2.json", "INCLID_PREMIUM_ADS", "list"
                    )
                ),
                Contains(
                    get_found_bundle_by_place_and_viewtype_str(
                        "bundle_const_search_8.json", "INCLID_RECOM_THEMATIC", "list"
                    )
                ),
                Contains(
                    get_found_bundle_by_place_and_viewtype_str(
                        "bundle_const_search_2.json", "INCLID_PREMIUM_ADS", "grid"
                    )
                ),
                Contains(
                    get_found_bundle_by_place_and_viewtype_str(
                        "bundle_const_search_3.json", "INCLID_RECOM_THEMATIC", "grid"
                    )
                ),
            ]
        }
        self.assertFragmentIn(self.report.request_json(request_desktop_none), expected)
        self.assertFragmentIn(self.report.request_json(request_desktop_list), expected)
        self.assertFragmentIn(self.report.request_json(request_desktop_grid), expected)

    def test_bundles_config_with_view_type_three_names_weired_order(self):
        """Проверяем применение выражения по типу вьюшки не зависит от порядка полей"""
        # от порядка полей не зависит, они отсортируются в дереве и результат будет тот же
        request_touch = T.VIEW_TYPE_REQUEST_TEMPLATE.format("frontend", "touch", "")
        self.assertFragmentIn(
            self.report.request_json(request_touch),
            {
                "logicTrace": [
                    Contains(
                        get_found_bundle_by_place_and_viewtype_str(
                            "bundle_const_search_8.json", "INCLID_RECOM_THEMATIC", "list"
                        )
                    ),
                    Contains(
                        get_found_bundle_by_place_and_viewtype_str(
                            "bundle_const_search_3.json", "INCLID_RECOM_THEMATIC", "grid"
                        )
                    ),
                ]
            },
        )

    def test_bundles_config_with_view_type_override(self):
        """Проверяем что специализированные имена перекрывают стандартные"""
        request_desktop = T.VIEW_TYPE_REQUEST_TEMPLATE.format("frontend", "desktop", "")
        self.assertFragmentIn(
            self.report.request_json(request_desktop),
            {
                "logicTrace": [
                    Contains(
                        get_found_bundle_by_place_and_viewtype_str(
                            "bundle_const_search_8.json", "INCLID_DISCOUNT", "list"
                        )
                    ),
                    Contains(
                        get_found_bundle_by_place_and_viewtype_str(
                            "bundle_const_search_1.json", "INCLID_DISCOUNT", "grid"
                        )
                    ),
                ]
            },
        )

        request_touch = T.VIEW_TYPE_REQUEST_TEMPLATE.format("frontend", "touch", "")
        self.assertFragmentIn(
            self.report.request_json(request_touch),
            {
                "logicTrace": [
                    Contains(
                        get_found_bundle_by_place_and_viewtype_str(
                            "bundle_const_search_3.json", "INCLID_DISCOUNT", "grid"
                        )
                    ),
                    Contains(
                        get_found_bundle_by_place_and_viewtype_str(
                            "bundle_const_search_1.json", "INCLID_DISCOUNT", "list"
                        )
                    ),
                ]
            },
        )

        request_android = T.VIEW_TYPE_REQUEST_TEMPLATE.format("ANDROID", "empty", "")
        self.assertFragmentIn(
            self.report.request_json(request_android),
            {
                "logicTrace": [
                    Contains(
                        get_found_bundle_by_place_and_viewtype_str(
                            "bundle_const_search_3.json", "INCLID_DISCOUNT", "grid"
                        )
                    ),
                    Contains(
                        get_found_bundle_by_place_and_viewtype_str(
                            "bundle_const_search_8.json", "INCLID_DISCOUNT", "list"
                        )
                    ),
                ]
            },
        )

    def test_bundles_config_with_view_type_with_single(self):
        """Проверяем если указано одно поле, второе не установится"""
        # проверим случае когда указано только одно поле для листа или грида
        request_ios_list = T.VIEW_TYPE_REQUEST_TEMPLATE.format("IOS", "empty", "")
        self.assertFragmentIn(
            self.report.request_json(request_ios_list),
            {
                "logicTrace": [
                    Contains(
                        get_found_bundle_by_place_and_viewtype_str(
                            "bundle_const_search_8.json", "INCLID_RECOM_THEMATIC", "list"
                        )
                    ),
                    Contains(get_no_bundle_by_place_and_viewtype_str("INCLID_RECOM_THEMATIC", "grid")),
                ]
            },
        )

        request_android_list = T.VIEW_TYPE_REQUEST_TEMPLATE.format("ANDROID", "empty", "")
        self.assertFragmentIn(
            self.report.request_json(request_android_list),
            {
                "logicTrace": [
                    Contains(
                        get_found_bundle_by_place_and_viewtype_str(
                            "bundle_const_search_3.json", "INCLID_RECOM_THEMATIC", "grid"
                        )
                    ),
                    Contains(get_no_bundle_by_place_and_viewtype_str("INCLID_RECOM_THEMATIC", "list")),
                ]
            },
        )

    def test_bundles_config(self):
        """Проверяем запрос, в котором 2 врезки, а бандл выбирается в зависимости от настроек в конфиге бандлов"""

        request = (
            "place=blender&text=марс&additional_entities=articles&debug=da&"
            + "supported-incuts="
            + get_supported_incuts_cgi()
            + "&"
            + "client={client}&platform={platform}&"
            + "rearr-factors=market_blender_cpa_shop_incut_enabled=1;"
            + "market_ugc_saas_enabled=1;"
        )
        request_desktop = request.format(client="frontend", platform="desktop")
        request_touch = request.format(client="frontend", platform="touch")
        request_android = request.format(client="ANDROID", platform="empty")
        request_ios = request.format(client="IOS", platform="empty")

        """ включаем конфиг бандлов
        условие в запросе client == frontend && platform == desktop
        из конфига бандлов:
        для статейной врезки подойдет условие client == frontend && platform == desktop, значит выберется bundle_const_search_2.json
        для премиальной выберется подходящий bundle_const_search_2.json
        """
        response = self.report.request_json(request_desktop)
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains(get_found_bundle_str("bundle_const_search_2.json", "INCLID_PREMIUM_ADS")),
                    Contains(get_found_bundle_str("bundle_const_search_2.json", "INCLID_MATERIAL_ENTRYPOINTS")),
                ]
            },
        )

        """ Будет показана только статейная врезка на 2-ой позиции,
        так как продуктовые ограничения блендинга запрещают размещение
        врезок чаще, чем раз в 3 сниппета
        """
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "entity": "searchIncut",
                            "title": "Полезные статьи",
                            "position": 2,
                            "inClid": 1,
                            "isNewFormat": True,
                        },
                    ]
                },
            },
            allow_different_len=False,
            preserve_order=True,
        )

        """ включаем конфиг бандлов
        условие в запросе platform == ios
        для статейной врезки бандла нет
        для премиальной выберется дефолтный
        """
        response = self.report.request_json(request_ios)
        self.assertFragmentNotIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "entity": "searchIncut",
                            "title": "Полезные статьи",
                            "inClid": 1,
                            "isNewFormat": True,
                        },
                    ]
                },
            },
        )
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "entity": "searchIncut",
                            "title": "Популярные предложения",
                            "inClid": 2,
                            "isNewFormat": True,
                        },
                    ]
                },
            },
            allow_different_len=False,
            preserve_order=True,
        )

        """
        Для статейной врезки выберется bundle_const_search_8.json
        Для премиальной выберется дефолтный бандл
        """
        response = self.report.request_json(request_touch)
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains(get_found_bundle_str("bundle_const_search_8.json", "INCLID_MATERIAL_ENTRYPOINTS")),
                    Contains(get_found_bundle_str("bundle_const_search_2.json", "INCLID_PREMIUM_ADS")),
                ]
            },
        )

        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "entity": "searchIncut",
                            "isNewFormat": True,
                            "title": "Полезные статьи",
                            "position": 9,
                            "inClid": 1,
                        },
                        {
                            "entity": "searchIncut",
                            "isNewFormat": True,
                            "position": 2,
                            "inClid": 2,
                        },
                    ]
                },
            },
            allow_different_len=False,
        )

        """
        дефолтный для премиальной
        bundle_const_search_8 для статейной
        """
        response = self.report.request_json(request_android)
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains(get_found_bundle_str("bundle_const_search_8.json", "INCLID_MATERIAL_ENTRYPOINTS")),
                    Contains(get_found_bundle_str("bundle_const_search_2.json", "INCLID_PREMIUM_ADS")),
                ]
            },
        )

    def test_incuts_organic_position_show_log(self):
        """
        для врезок логируются органические позиции, а фронту передаются шоу позиции
        """
        request = (
            "place=blender&text=марс&additional_entities=articles&debug=da&"
            + "supported-incuts="
            + get_supported_incuts_cgi()
            + "&"
            + "client={client}&platform={platform}&"
            + "rearr-factors=market_blender_cpa_shop_incut_enabled=1;"
            + "market_ugc_saas_enabled=1"
        )
        request_touch = request.format(client="frontend", platform="touch")

        response = self.report.request_json(request_touch)
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "entity": "searchIncut",
                            "isNewFormat": True,
                            "title": "Полезные статьи",
                            "position": 9,
                            "inClid": 1,
                        },
                        {
                            "entity": "searchIncut",
                            "isNewFormat": True,
                            "position": 2,
                            "inClid": 2,
                        },
                    ]
                },
            },
            allow_different_len=False,
        )
        self.show_log_tskv.expect(
            record_type=3, inclid=1, position=8
        )  # шоу позиция (на фронте) 9, органическая позиция 8
        self.show_log_tskv.expect(record_type=3, inclid=2, position=2)

    def test_market_blender_bundles_for_inclid(self):
        """
        проверяем, что если во флаге market_blender_bundles_for_inclid указать определенный инклид для бандла, до будет использоваться он
        формат market_blender_bundles_for_inclid=1:bundles1,2:bundles2
        INCLID_MATERIAL_ENTRYPOINTS=1
        INCLID_PREMIUM_ADS=2
        """
        request = (
            "place=blender&text=марс&additional_entities=articles&debug=da&client=frontend&platform=desktop&"
            + "supported-incuts="
            + get_supported_incuts_cgi()
            + "&rearr-factors=market_blender_cpa_shop_incut_enabled=1;"
        )

        """ указываем несуществующие инклиды, врезак будет только премиальная, потому что во флаге ее нет"""
        response = self.report.request_json(request + "market_blender_bundles_for_inclid=1:bundles1,20000:bundles2")
        self.assertFragmentIn(
            response,
            {
                "incuts": {"results": [{"inClid": 2}]},
            },
            allow_different_len=False,
        )

        """ для статейной врезки указываем конкретный бандл bundle_const_search_11.json, для премиальной будет bundle_const_search_2.json из конфига"""
        response = self.report.request_json(
            request + "market_blender_bundles_for_inclid=1:bundle_const_search_11,2:bundle_strange"
        )
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains(get_found_bundle_str("bundle_const_search_11.json", "INCLID_MATERIAL_ENTRYPOINTS")),
                ]
            },
        )

        """ для статейной врезки указываем конкретный бандл bundle_const_search_11.json, для премиальной его же """
        response = self.report.request_json(
            request + "market_blender_bundles_for_inclid=1:bundle_const_search_11.json,2:bundle_const_search_11.json"
        )
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains(get_found_bundle_str("bundle_const_search_11.json", "INCLID_MATERIAL_ENTRYPOINTS")),
                    Contains(get_found_bundle_str("bundle_const_search_11.json", "INCLID_PREMIUM_ADS")),
                ]
            },
        )

        """ а теперь разнесем их в два параметра и проверим, что работает так же """
        response = self.report.request_json(
            request
            + "market_blender_bundles_for_inclid=1:bundle_const_search_11.json&rearr-factors=market_blender_bundles_for_inclid=2:bundle_const_search_11.json"
        )
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains(get_found_bundle_str("bundle_const_search_11.json", "INCLID_MATERIAL_ENTRYPOINTS")),
                    Contains(get_found_bundle_str("bundle_const_search_11.json", "INCLID_PREMIUM_ADS")),
                ]
            },
        )

    def test_supported_incuts(self):
        """
        Проверяем, что бандлы пересекаются с supported_incuts
        у бандла константной позиции плейс = 1, вьютайп = 1
        """
        request = (
            "place=blender&text=марс&additional_entities=articles&debug=da&client=frontend&platform=desktop&"
            + "rearr-factors=market_blender_cpa_shop_incut_enabled=1;market_ugc_saas_enabled=1;"
        )

        """ указываем, что фронт не умеет рисовать вьютйап 1 для плейса 1
            будет пустое пересечение с бандлами, врезок не будет
        """
        response = self.report.request_json(
            request + "&supported-incuts=" + get_supported_incuts_cgi({"1": ["2", "3"]})
        )
        self.assertFragmentIn(
            response,
            {'incuts': {'results': []}},
            allow_different_len=False,
        )

        """ указываем, что фронт умеет все что угодно"""
        response = self.report.request_json(
            request + "&supported-incuts=" + get_supported_incuts_cgi({"1": ["1", "2"], "2": ["1", "2"]})
        )
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains(
                        get_found_bundle_by_place_and_viewtype_str(
                            "bundle_const_search_2.json", "INCLID_PREMIUM_ADS", "list"
                        )
                    ),
                    Contains(
                        get_found_bundle_by_place_and_viewtype_str(
                            "bundle_const_search_2.json", "INCLID_MATERIAL_ENTRYPOINTS", "list"
                        )
                    ),
                ]
            },
        )

    def test_supported_incuts_patching(self):
        """
        проверяем, что бандл патчится по параметрам с фронта
        в бандле bundle_many в incut_place_viewtype_combinations указаны комбинации
        {"1": ["1", "3"], "2": ["2", "3"]}
        на позиции 1 плейс 1 и вьютайп 1
        на позиции 1 плейс 2 и вьютайп 3
        на позиции 2 плейс 1 и вьютайп 3
        на позиции 4 плейс 1 и вьютайп 2 (никогда не выберется, отсутствует в incut_place_viewtype_combinations)
        """
        request = (
            "place=blender&text=марс&additional_entities=articles&debug=da&client=frontend&platform=desktop&"
            + "rearr-factors=market_blender_bundles_for_inclid=1:bundle_many,2:empty;"
            + "market_ugc_saas_enabled=1"
        )

        """
        для форматов {"1": ["2", "3"]} от фронта при пересечении incut_place_viewtype_combinations остаются {"1": ["3"]}
        подойдет только позиция 2
        """
        response = self.report.request_json(
            request + "&supported-incuts=" + get_supported_incuts_cgi({"1": ["2", "3"]})
        )
        self.assertFragmentIn(
            response,
            {"incuts": {"results": [{'entity': 'searchIncut', 'inClid': 1, 'position': 2}]}},
            allow_different_len=False,
            preserve_order=True,
        )

        """
        пустые поддерживаемые комбинации - врезок не будет
        """
        response = self.report.request_json(request + "&supported-incuts=" + get_supported_incuts_cgi({}))
        self.assertFragmentIn(
            response,
            {"incuts": {"results": []}},
            allow_different_len=False,
            preserve_order=True,
        )

        """
        для форматов {"1": ["1", "3"], "2": ["2"]} от фронта при пересечении incut_place_viewtype_combinations остаются {"1": ["1", "3"], "2": ["2"]}
        подойдут позиции 1 и 2, но у 1 скор больше
        """
        response = self.report.request_json(
            request + "&supported-incuts=" + get_supported_incuts_cgi({"1": ["1", "3"], "2": ["2"]})
        )
        self.assertFragmentIn(
            response,
            {"incuts": {"results": [{'entity': 'searchIncut', 'inClid': 1, 'position': 1}]}},
            allow_different_len=False,
            preserve_order=True,
        )

        """
        если указать плейсы и вьютайпы, о которых репорт не знает, то ничего не сломается
        для форматов {"2": ["3", "10012"], "54": ["1"]} от фронта при пересечении incut_place_viewtype_combinations остаются {"2": ["3"]}
        подойдет только позиция 2
        """
        # response = self.report.request_json(request + "&supported-incuts=" + get_supported_incuts_cgi({"2": ["3", "10012"], "54": ["1"]}))
        response = self.report.request_json(
            request + "&supported-incuts=" + get_supported_incuts_cgi({"2": ["3", "10012"], "54": ["1"]})
        )
        self.assertFragmentIn(
            response,
            {"incuts": {"results": [{'entity': 'searchIncut', 'inClid': 1, 'position': 1, 'placeId': 2}]}},
            allow_different_len=False,
            preserve_order=True,
        )

        """
        все работает, если указывать в качестве вьютайпов не строки, а инты
        """
        response = self.report.request_json(
            request + "&supported-incuts=" + get_supported_incuts_cgi({2: [3, "10012"], "54": ["1"]})
        )
        self.assertFragmentIn(
            response,
            {"incuts": {"results": [{'entity': 'searchIncut', 'inClid': 1, 'position': 1, 'placeId': 2}]}},
            allow_different_len=False,
            preserve_order=True,
        )

    def test_blender_cpa_shop_incut_touch(self):

        response = self.report.request_json(
            "place=blender&text=марс&rearr-factors=market_blender_cpa_shop_incut_enabled=1;"
            + "market_ugc_saas_enabled=1;"
            + "&supported-incuts="
            + get_supported_incuts_cgi()
            + "&additional_entities=articles&touch=1&client=frontend&platform=touch"
        )
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'inClid': 2,
                            'items': [
                                {'urls': {'encrypted': Contains('/pp=620/')}},
                            ],
                        },
                    ]
                }
            },
        )

        response = self.report.request_json(
            "place=blender&text=марс&rearr-factors=market_blender_cpa_shop_incut_enabled=1;market_blender_bundles_for_inclid=1:bundle_const_search_8;"
            + "market_ugc_saas_enabled=1;"
            + "&supported-incuts="
            + get_supported_incuts_cgi()
            + "&additional_entities=articles&touch=0&client=frontend&platform=desktop&debug=da"
        )
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'inClid': 2,
                            'items': [{'urls': {'encrypted': Contains('/pp=230/')}}],
                        },
                        {
                            'entity': 'searchIncut',
                            'inClid': 1,
                        },
                    ]
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'inClid': 2,
                            'items': [
                                {'titles': {'raw': 'Марс Небо'}},
                                {'titles': {'raw': 'Марс Кислород'}},
                                {'titles': {'raw': 'Марс Выдох'}},
                                {'titles': {'raw': 'Марс Вдох'}},
                                {'titles': {'raw': 'Марс Дыши'}},
                            ],
                        },
                    ]
                }
            },
        )

    def test_catch_place_exception_v1(self):
        """
        запрос, на котором падает place=cpa_shop_incut
        блендер ловит исключение, ругается ошибкой в лог, но результат отдает
        """
        self.error_log.expect(code=3710)
        self.report.request_json(
            "place=blender&rearr-factors=market_blender_cpa_shop_incut_enabled=1;market_premium_ads_incut_get_docs_through_prime=0;"
            + "market_blender_bundles_for_inclid=1:bundle_const_search_8;"
            + "&supported-incuts="
            + get_supported_incuts_cgi()
            + "&additional_entities=articles&touch=0&client=frontend&platform=desktop&debug=da",
            strict=False,
        )

    def test_catch_place_exception_v2(self):
        """
        запрос, на котором падал place=cpa_shop_incut в блендере
        с флагом market_premium_ads_incut_get_docs_through_prime падение не воспроизводится
        потому что cpa_shop_incut вызывается через prime
        """
        self.report.request_json(
            "place=blender&rearr-factors=market_blender_cpa_shop_incut_enabled=1;market_premium_ads_incut_get_docs_through_prime=1;"
            + "market_blender_bundles_for_inclid=1:bundle_const_search_8;"
            + "&supported-incuts="
            + get_supported_incuts_cgi()
            + "&additional_entities=articles&touch=0&client=frontend&platform=desktop&debug=da",
            strict=False,
        )

    def test_second_page(self):
        """
        проверяем, что позиции органики на 2й странице правильно считаются
        2я страница нужна только для бандловой версии блендера, без него в поисковом блендере только первая страница
        """
        self.report.request_json("place=blender&text=марс&numdoc=4&supported-incuts=" + get_supported_incuts_cgi())
        self.report.request_json(
            "place=blender&text=марс&numdoc=4&page=2&supported-incuts=" + get_supported_incuts_cgi()
        )
        for i in range(8):
            self.show_log_tskv.expect(pp=18, show_uid='0488419200111777888880000' + str(i + 1), position=i + 1)

    def test_incuts_disabling(self):
        request = (
            "place=blender&text=марс&additional_entities=articles&debug=da&"
            + "supported-incuts="
            + get_supported_incuts_cgi()
            + "&"
            + "client={client}&platform={platform}&"
            + "rearr-factors=market_blender_cpa_shop_incut_enabled=1;"
            + "market_ugc_saas_enabled=1;"
        )
        request_touch = request.format(client="frontend", platform="touch")

        # Проверим, что без флага все врезки на месте
        response = self.report.request_json(request_touch)
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "entity": "searchIncut",
                            "inClid": 1,
                            "incutId": "default",
                        },
                        {
                            "entity": "searchIncut",
                            "inClid": 2,
                            "incutId": "default",
                        },
                    ]
                },
            },
            allow_different_len=False,
        )

        # Проверим способы отключения врезки с использованием флага
        # market_blender_disabled_incuts

        # Проверим, что с флагом отключится одна указанная врезка (статейная)
        response = self.report.request_json(request_touch + "market_blender_disabled_incuts=frontend:touch:2:default")
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "entity": "searchIncut",
                            "inClid": 1,
                            "incutId": "default",
                        },
                    ]
                },
            },
            allow_different_len=False,
        )

        # Проверим, что с флагом, где одна из отключаемых врезок - на другой платформе, отключится только врезка
        # с правильно указанной во флаге платформой (статейная), а с неправильно указанной - останется (премиальная)
        response = self.report.request_json(
            request_touch + "market_blender_disabled_incuts=ios:1:default,frontend:touch:2:default"
        )
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "entity": "searchIncut",
                            "inClid": 1,
                            "incutId": "default",
                        },
                    ]
                },
            },
            allow_different_len=False,
        )

        # Проверим, что с флагом, где отключаются обе врезки с правильным указанием платформы, врезок не будет
        response = self.report.request_json(
            request_touch + "market_blender_disabled_incuts=frontend:touch:1:default,frontend:touch:2:default"
        )
        self.assertFragmentIn(
            response,
            {
                "incuts": {"results": []},
            },
            allow_different_len=False,
        )

        # Проверим способы отключения врезки с использованием флага
        # market_blender_incut_generators_disabled

        response = self.report.request_json(request_touch + "market_blender_incut_generators_disabled=1,2")
        self.assertFragmentIn(
            response,
            {
                "incuts": {"results": []},
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
