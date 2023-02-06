#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types import (
    Autostrategy,
    AutostrategyType,
    AutostrategyWithDatasourceId,
    BlueOffer,
    ClickType,
    GLParam,
    GLType,
    GLValue,
    HyperCategory,
    IncutBlackListFb,
    MarketSku,
    MnPlace,
    Model,
    ModelDescriptionTemplates,
    Offer,
    Opinion,
    RedirectWhiteListRecord,
    ReqwExtMarkupMarketCategory,
    ReqwExtMarkupToken,
    ReqwExtMarkupTokenChar,
    Shop,
    UrlType,
    Vendor,
)
from core.types.fashion_parameters import FashionCategory
from core.types.reserveprice_fee import ReservePriceFee
from core.testcase import TestCase, main
from core.matcher import Contains, ElementCount, EmptyList, NotEmpty, Absent, Regex, Capture
from core.blender_bundles import get_supported_incuts_cgi, create_blender_bundles


class BlenderConstCpaShopIncut:
    BUNDLE = '''
{
    "incut_places": ["Search"],
    "incut_positions": [1],
    "incut_viewtypes": ["Gallery", "PremiumRichSnippet"],
    "incut_ids": ["default", "cpa_shop_incut_rich_snippet"],
    "result_scores": [
        {
            "incut_place": "Search",
            "row_position": 1,
            "incut_viewtype": "Gallery",
            "incut_id": "default",
            "score": 0.75
        },
        {
            "incut_place": "Search",
            "row_position": 1,
            "incut_viewtype": "PremiumRichSnippet",
            "incut_id": "cpa_shop_incut_rich_snippet",
            "score": 0.74
        }
    ],
    "calculator_type": "ConstPosition"
}
'''
    BUNDLE_HIGH_MODEL_RATING_INCUT = '''
{
    "incut_places": ["Search"],
    "incut_positions": [1],
    "incut_viewtypes": ["Gallery"],
    "incut_ids": ["default", "cpa_shop_incut_filter_by_model_rating"],
    "result_scores": [
        {
            "incut_place": "Search",
            "row_position": 1,
            "incut_viewtype": "Gallery",
            "incut_id": "default",
            "score": 0.75
        },
        {
            "incut_place": "Search",
            "row_position": 1,
            "incut_viewtype": "Gallery",
            "incut_id": "cpa_shop_incut_filter_by_model_rating",
            "score": 0.76
        }
    ],
    "calculator_type": "ConstPosition"
}
'''
    BUNDLE_DEFAULT_THEN_HIGH_RATING = '''
{
    "incut_places": ["Search"],
    "incut_positions": [1, 4, 7],
    "incut_viewtypes": ["Gallery"],
    "incut_ids": ["default", "cpa_shop_incut_filter_by_model_rating"],
    "result_scores": [
        {
            "incut_place": "Search",
            "row_position": 1,
            "incut_viewtype": "Gallery",
            "incut_id": "default",
            "score": 0.65
        },
        {
            "incut_place": "Search",
            "row_position": 4,
            "incut_viewtype": "Gallery",
            "incut_id": "default",
            "score": 0.75
        },
        {
            "incut_place": "Search",
            "row_position": 4,
            "incut_viewtype": "Gallery",
            "incut_id": "cpa_shop_incut_filter_by_model_rating",
            "score": 0.74
        },
        {
            "incut_place": "Search",
            "row_position": 7,
            "incut_viewtype": "Gallery",
            "incut_id": "cpa_shop_incut_filter_by_model_rating",
            "score": 0.75
        }
    ],
    "calculator_type": "ConstPosition"
}
'''
    BUNDLE_HIGH_RATING_THEN_DEFAULT = '''
{
    "incut_places": ["Search"],
    "incut_positions": [1, 4, 7],
    "incut_viewtypes": ["Gallery"],
    "incut_ids": ["default", "cpa_shop_incut_filter_by_model_rating"],
    "result_scores": [
        {
            "incut_place": "Search",
            "row_position": 1,
            "incut_viewtype": "Gallery",
            "incut_id": "cpa_shop_incut_filter_by_model_rating",
            "score": 0.65
        },
        {
            "incut_place": "Search",
            "row_position": 1,
            "incut_viewtype": "Gallery",
            "incut_id": "default",
            "score": 0.64
        },
        {
            "incut_place": "Search",
            "row_position": 4,
            "incut_viewtype": "Gallery",
            "incut_id": "cpa_shop_incut_filter_by_model_rating",
            "score": 0.75
        },
        {
            "incut_place": "Search",
            "row_position": 4,
            "incut_viewtype": "Gallery",
            "incut_id": "default",
            "score": 0.74
        },
        {
            "incut_place": "Search",
            "row_position": 7,
            "incut_viewtype": "Gallery",
            "incut_id": "default",
            "score": 0.75
        }
    ],
    "calculator_type": "ConstPosition"
}
'''
    VENDOR_INCUT_BUNDLE = '''
{
    "incut_places": ["Top"],
    "incut_positions": [1],
    "incut_viewtypes": ["GalleryWithBanner", "VendorGallery"],
    "incut_ids": ["vendor_incut_with_banner", "vendor_incut"],
    "result_scores": [
        {
            "incut_place": "Top",
            "position": 1,
            "row_position": 1,
            "incut_viewtype": "GalleryWithBanner",
            "incut_id": "vendor_incut_with_banner",
            "score": 0.75
        },
        {
            "incut_place": "Top",
            "position": 1,
            "row_position": 1,
            "incut_viewtype": "VendorGallery",
            "incut_id": "vendor_incut",
            "score": 0.74
        }
    ],
    "calculator_type": "ConstPosition"
}
'''


class BlenderBundlesConfig:
    BUNDLES_CONFIG = """
{
    "INCLID_PREMIUM_ADS" : {
        "search_type == text && client == frontend && platform == desktop" : {
            "bundle_name": "const_premium_ads.json"
        },
        "client == frontend && platform == touch" : {
            "bundle_name": "const_premium_ads.json"
        },
        "search_type == textless && client == frontend && platform == desktop" : {
            "bundle_name": "const_premium_ads.json"
        },
        "client == ANDROID" : {
            "bundle_name": "const_premium_ads.json"
        },
        "client == IOS" : {
            "bundle_name": "const_premium_ads.json"
        }
    }
}
"""


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

    @staticmethod
    def get_request(params, rearr):
        def dict_to_str(data, separator):
            return str(separator).join("{}={}".format(str(k), str(v)) for (k, v) in data.iteritems())

        return "{}".format(dict_to_str(params, '&')) + str(
            '&rearr-factors={}'.format(dict_to_str(rearr, ';')) if rearr else ''
        )

    @classmethod
    def prepare_blender_bundles_config(cls):
        cls.settings.formulas_path = create_blender_bundles(
            cls.meta_paths.testroot,
            BlenderBundlesConfig.BUNDLES_CONFIG,
            {
                "const_premium_ads.json": BlenderConstCpaShopIncut.BUNDLE,
                "const_premium_ads_high_model_rating_incut.json": BlenderConstCpaShopIncut.BUNDLE_HIGH_MODEL_RATING_INCUT,
                "const_premium_ads_default_then_high_rating_incut.json": BlenderConstCpaShopIncut.BUNDLE_DEFAULT_THEN_HIGH_RATING,
                "const_premium_ads_high_rating_then_default_incut.json": BlenderConstCpaShopIncut.BUNDLE_HIGH_RATING_THEN_DEFAULT,
                "const_vendor_incut_1.json": BlenderConstCpaShopIncut.VENDOR_INCUT_BUNDLE,
            },
        )

    @classmethod
    def prepare_cpa_incut_default(cls):
        titles = list(range(1, 22))
        cls.index.models += [
            Model(
                hid=66,
                hyperid=66 + i,
                title="Модель {}".format(titles[i]),
                ts=100020 + i,
                opinion=Opinion(
                    total_count=100, positive_count=95, rating=3.5, precise_rating=3.58, rating_count=200, reviews=5
                ),
            )
            for i in range(1, 21)
        ]

        cls.index.shops += [
            Shop(
                fesh=66 + i, priority_region=213, shop_fee=100, cpa=Shop.CPA_REAL, name='CPA Shop {}'.format(titles[i])
            )
            for i in range(1, 21)
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
                title="Маркс {}".format(titles[i]),
            )
            for i in range(1, 10)
        ]

        cls.index.offers += [
            Offer(
                fesh=76,
                hyperid=76,
                hid=66,
                fee=100,
                ts=100030,
                price=100,
                cpa=Offer.CPA_REAL,
                title="Engels 1",
            )
        ]

        cls.index.offers += [
            Offer(
                fesh=76 + i,
                hyperid=76 + i,
                hid=66,
                fee=0,
                ts=100030 + i,
                price=100,
                cpa=Offer.CPA_REAL,
                title="Engels {}".format(titles[i]),
            )
            for i in range(2, 10)
        ]

        for i in range(1, 21):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100020 + i).respond(0.04)

    def test_cpa_incut_default(self):
        params = {
            "place": "blender",
            "text": "маркс",
            "additional_entities": "articles",
            "touch": "1",
            "client": "frontend",
            "platform": "touch",
            'supported-incuts': get_supported_incuts_cgi(),
            "debug": "da",
        }

        rearr_flags = {
            "market_blender_cpa_shop_incut_enabled": 1,
            "market_blender_use_bundles_config": 1,
            "market_premium_ads_incut_get_docs_through_prime": 0,
        }

        def check_pp(response, expected_pp, times):
            self.assertFragmentIn(
                response,
                {
                    'incuts': {
                        'results': [
                            {
                                'entity': 'searchIncut',
                                'incutId': 'default',
                                'inClid': 2,
                                'items': [
                                    {
                                        'slug': "marks-10",
                                        'urls': {'encrypted': Contains('/pp={}/'.format(expected_pp))},
                                        'feeShowPlain': Contains("pp: {}".format(expected_pp)),
                                    },
                                    {
                                        'slug': "marks-9",
                                        'urls': {'encrypted': Contains('/pp={}/'.format(expected_pp))},
                                        'feeShowPlain': Contains("pp: {}".format(expected_pp)),
                                    },
                                    {
                                        'slug': "marks-8",
                                        'urls': {'encrypted': Contains('/pp={}/'.format(expected_pp))},
                                        'feeShowPlain': Contains("pp: {}".format(expected_pp)),
                                    },
                                    {
                                        'slug': "marks-7",
                                        'urls': {'encrypted': Contains('/pp={}/'.format(expected_pp))},
                                        'feeShowPlain': Contains("pp: {}".format(expected_pp)),
                                    },
                                    {
                                        'slug': "marks-6",
                                        'urls': {'encrypted': Contains('/pp={}/'.format(expected_pp))},
                                        'feeShowPlain': Contains("pp: {}".format(expected_pp)),
                                    },
                                ],
                            },
                        ]
                    }
                },
                preserve_order=True,
            )
            # Проверим логи
            self.show_log_tskv.expect(title="Маркс 10", url_type=6, pp=expected_pp).times(times)
            self.show_log_tskv.expect(title="Маркс 9", url_type=6, pp=expected_pp).times(times)
            self.show_log_tskv.expect(title="Маркс 8", url_type=6, pp=expected_pp).times(times)
            self.show_log_tskv.expect(title="Маркс 7", url_type=6, pp=expected_pp).times(times)
            self.show_log_tskv.expect(title="Маркс 6", url_type=6, pp=expected_pp).times(times)

        # Проверяем тач
        response = self.report.request_json(T.get_request(params, rearr_flags))
        check_pp(response, 620, 1)

        # Проверяем десктоп
        del params["touch"]
        params["platform"] = "desktop"
        response = self.report.request_json(T.get_request(params, rearr_flags))
        check_pp(response, 230, 1)

        # Проверяем ANRDOID
        params["client"] = "ANDROID"
        del params["platform"]
        response = self.report.request_json(T.get_request(params, rearr_flags))
        check_pp(response, 1709, 1)

        # Проверяем IOS
        params["client"] = "IOS"
        response = self.report.request_json(T.get_request(params, rearr_flags))
        check_pp(response, 1809, 1)

        # проверяем тоже для запроса через prime
        rearr_flags["market_premium_ads_incut_get_docs_through_prime"] = 1

        params = {
            "place": "blender",
            "text": "маркс",
            "additional_entities": "articles",
            "touch": "1",
            "client": "frontend",
            "platform": "touch",
            'supported-incuts': get_supported_incuts_cgi(),
            "debug": "da",
        }

        # Проверяем тач
        response = self.report.request_json(T.get_request(params, rearr_flags))
        check_pp(response, 620, 2)

        # Проверяем десктоп
        del params["touch"]
        params["platform"] = "desktop"
        response = self.report.request_json(T.get_request(params, rearr_flags))
        check_pp(response, 230, 2)

        # Проверяем ANRDOID
        params["client"] = "ANDROID"
        del params["platform"]
        response = self.report.request_json(T.get_request(params, rearr_flags))
        check_pp(response, 1709, 2)

        # Проверяем IOS
        params["client"] = "IOS"
        response = self.report.request_json(T.get_request(params, rearr_flags))
        check_pp(response, 1809, 2)

    def test_cpa_shop_incut_flag_for_apps(self):
        params = {
            "place": "blender",
            "text": "маркс",
            "client": "ANDROID",
            'supported-incuts': get_supported_incuts_cgi(),
            "debug": "da",
        }

        rearr_flags = {
            "market_blender_use_bundles_config": 1,
            "market_blender_bundles_for_inclid": "2:const_premium_ads.json",
        }
        # без флага врезка есть
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'inClid': 2,
                        },
                    ]
                }
            },
            preserve_order=True,
        )
        # но отключить флагом все еще можно
        rearr_flags["market_blender_cpa_shop_incut_enabled"] = 0
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentNotIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'inClid': 2,
                        },
                    ]
                }
            },
            preserve_order=True,
        )

    def test_cpa_incut_sort(self):
        # на таче, десктопе и андроиде врезка только на дефолтных сортировках
        params = {
            "place": "blender",
            "text": "маркс",
            "additional_entities": "articles",
            "touch": "1",
            "client": "frontend",
            "platform": "touch",
            "how": "aprice",
            'supported-incuts': get_supported_incuts_cgi(),
        }

        rearr_flags = {
            "market_blender_cpa_shop_incut_enabled": 1,
            "market_blender_use_bundles_config": 1,
        }

        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentNotIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'inClid': 2,
                        },
                    ]
                }
            },
            preserve_order=True,
        )
        params["client"] = "ANDROID"
        rearr_flags["market_blender_bundles_for_inclid"] = "2:const_premium_ads.json"
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentNotIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'inClid': 2,
                        },
                    ]
                }
            },
            preserve_order=True,
        )
        params["client"] = "frontend"
        params["platform"] = "desktop"
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentNotIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'inClid': 2,
                        },
                    ]
                }
            },
            preserve_order=True,
        )
        # удаляем сортировку, врезка появляется
        del params["how"]
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'inClid': 2,
                        },
                    ]
                }
            },
            preserve_order=True,
        )

    def test_cpa_incut_rich_snippet(self):
        # проверяем что богатый сниппет собирается
        def check_response(response):
            self.assertFragmentIn(
                response,
                {
                    'incuts': {
                        'results': [
                            {
                                'entity': 'searchIncut',
                                'incutId': 'cpa_shop_incut_rich_snippet',
                                'inClid': 2,
                                'items': [
                                    {
                                        'slug': "engels-1",
                                        'urls': {'encrypted': Contains('/pp=690/')},
                                        'feeShowPlain': Contains("pp: 690"),
                                    },
                                ],
                            },
                        ]
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

        params = {
            "place": "blender",
            "text": "Engels",
            "additional_entities": "articles",
            "touch": "1",
            "client": "frontend",
            "platform": "touch",
            'supported-incuts': get_supported_incuts_cgi(),
            "debug": "da",
        }

        rearr_flags = {
            "market_blender_cpa_shop_incut_enabled": 1,
            "market_blender_use_bundles_config": 1,
            "market_premium_ads_incut_get_docs_through_prime": 0,
        }

        response = self.report.request_json(T.get_request(params, rearr_flags))
        check_response(response)

        rearr_flags["market_premium_ads_incut_get_docs_through_prime"] = 1
        response = self.report.request_json(T.get_request(params, rearr_flags))
        check_response(response)

    def test_cpa_incut_rich_snippet_pp(self):
        """
        Проверяем, что у богатого сниппета правильно проставляется собственное pp
        Проверяем для десктопа и аппов (для тача проверяется выше)
        """
        params = {
            "place": "blender",
            "text": "Engels",
            "additional_entities": "articles",
            "client": "frontend",
            "platform": "desktop",
            'supported-incuts': get_supported_incuts_cgi(),
            "debug": "da",
        }

        rearr_flags = {
            "market_blender_cpa_shop_incut_enabled": 1,
            "market_blender_use_bundles_config": 1,
        }

        def check_pp(response, expected_pp):
            # Проверим ответ репорта
            self.assertFragmentIn(
                response,
                {
                    'incuts': {
                        'results': [
                            {
                                'entity': 'searchIncut',
                                'incutId': 'cpa_shop_incut_rich_snippet',
                                'inClid': 2,
                                'items': [
                                    {
                                        'slug': "engels-1",
                                        'urls': {'encrypted': Contains('/pp={}/'.format(expected_pp))},
                                        'feeShowPlain': Contains('pp: {}'.format(expected_pp)),
                                    },
                                ],
                            },
                        ],
                    },
                },
                preserve_order=True,
                allow_different_len=False,
            )
            # Проверим логи
            self.show_log_tskv.expect(url_type=6, pp=expected_pp).once()

        # Проверяем десктоп
        response = self.report.request_json(T.get_request(params, rearr_flags))
        check_pp(response, 90)

        # Проверяем ANDROID
        params["client"] = "ANDROID"
        del params["platform"]
        response = self.report.request_json(T.get_request(params, rearr_flags))
        check_pp(response, 1790)

        # Проверяем IOS
        params["client"] = "IOS"
        response = self.report.request_json(T.get_request(params, rearr_flags))
        check_pp(response, 1890)

        # Проверяем, что флагом market_cpa_shop_incut_premium_ads_use_different_pp
        # можно вернуть pp премиальной врезки (не использовать отдельный pp в богатом сниппете)
        # TODO: убрать эту часть теста, когда будем убирать флаг
        rearr_flags["market_cpa_shop_incut_premium_ads_use_different_pp"] = 0
        response = self.report.request_json(T.get_request(params, rearr_flags))
        check_pp(response, 1809)  # pp = 1809 => IOS, премиальная врезка

    def test_cpa_shop_incut_logging(self):
        """
        Проверяем, что в shows-log попадают нужные поля. Тест нужен, так как логирование в самом плейсе может отличаться от логирования через блендер
        """
        params = {
            "place": "blender",
            "text": "маркс",
            "client": "frontend",
            "platform": "desktop",
            'supported-incuts': get_supported_incuts_cgi(),
        }

        rearr_flags = {
            "market_blender_cpa_shop_incut_enabled": 1,
            "market_blender_use_bundles_config": 1,
            "market_cpa_shop_incut_model_rating_to_analogs_fields": 1,
        }

        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'inClid': 2,
                            'incutId': 'default',
                            'items': ElementCount(9),
                        },
                    ],
                }
            },
            preserve_order=True,
        )
        self.show_log_tskv.expect(analog_reason_score=3.58, url_type=6).times(9)

    @classmethod
    def prepare_incut_high_model_rating(cls):
        cls.index.vendors += [Vendor(vendor_id=1, name='vendor_10')]
        cls.index.models += [  # Модели с низким рейтингом (ниже 4)
            Model(
                hid=266,
                hyperid=266 + i,
                title="Торт с низким рейтингом {}".format(i),
                ts=200020 + i,
                vendor_id=1,
                opinion=Opinion(
                    total_count=100, positive_count=95, rating=3.5, precise_rating=3.58, rating_count=200, reviews=5
                ),
                vbid=10,
            )
            for i in range(1, 10)
        ]
        cls.index.models += [  # Модели с хорошим рейтингом (4.2)
            Model(
                hid=266,
                hyperid=266 + i,
                title="Торт с хорошим рейтингом {}".format(i),
                ts=200020 + i,
                vendor_id=1,
                opinion=Opinion(
                    total_count=100, positive_count=95, rating=4.2, precise_rating=4.23, rating_count=200, reviews=5
                ),
                vbid=10,
            )
            for i in range(10, 13)
        ]
        cls.index.models += [  # Модели с отличным рейтингом (4.8)
            Model(
                hid=266,
                hyperid=266 + i,
                title="Торт с отличным рейтингом {}".format(i),
                ts=200020 + i,
                vendor_id=1,
                opinion=Opinion(
                    total_count=100, positive_count=95, rating=4.8, precise_rating=4.81, rating_count=200, reviews=5
                ),
                vbid=10,
            )
            for i in range(13, 19)
        ]

        cls.index.shops += [
            Shop(
                fesh=266,
                priority_region=213,
                cpa=Shop.CPA_REAL,
                name='CPA Shop 266',
            ),
            Shop(
                fesh=267,
                priority_region=213,
                cpa=Shop.CPA_REAL,
                name='CPA Shop 267',
            ),
        ]

        cls.index.offers += [  # офферы с высокими ставками для моделей низкого рейтинга
            Offer(
                fesh=266,
                hyperid=266 + i,
                hid=266,
                fee=500 + i,
                ts=200120 + i * 10,
                price=500,
                cpa=Offer.CPA_REAL,
                title="Торт {}".format(i),
                waremd5='off-lowrate-0{}-TT-111Q'.format(i),
            )
            for i in range(1, 4)
        ]
        cls.index.offers += [
            # Офферы с хорошим рейтингом
            Offer(
                # Оффер-победитель этой модели (у него ставка большая)
                fesh=266,
                hyperid=276,
                hid=266,
                fee=200,
                ts=200232,
                price=500,
                cpa=Offer.CPA_REAL,
                title="Торт 10",
                waremd5="off-finerate-10-Hbid1Q",
            ),
            Offer(
                # Оффер-проигравший этой модели (но он всё же подпирает собой off-finerate-10-Lbid1Q)
                fesh=267,
                hyperid=276,
                hid=266,
                fee=180,
                ts=200232,
                price=500,
                cpa=Offer.CPA_REAL,
                title="Торт 10",
                waremd5="off-finerate-10-Lbid1Q",
            ),
            Offer(
                # Оффер-победитель этой модели
                fesh=266,
                hyperid=277,
                hid=266,
                fee=160,
                ts=200233,
                price=500,
                cpa=Offer.CPA_REAL,
                title="Торт 11",
                waremd5="off-finerate-11-Hbid1Q",
            ),
            Offer(
                # Оффер-проигравший этой модели (но он не годится в "подпорку", т.к. есть оффер другой модели со ставкой выше)
                fesh=267,
                hyperid=277,
                hid=266,
                fee=100,
                ts=200234,
                price=500,
                cpa=Offer.CPA_REAL,
                title="Торт 11",
                waremd5="off-finerate-11-Lbid1Q",
            ),
            Offer(
                # Оффер, подпирающий off-finerate-11-Hbid1Q
                fesh=266,
                hyperid=278,
                hid=266,
                fee=150,
                ts=200235,
                price=500,
                cpa=Offer.CPA_REAL,
                title="Торт 12",
                waremd5="off-finerate-12-Hbid1Q",
            ),
        ]
        cls.index.offers += [
            # Остальные офферы с хорошим рейтингом
            Offer(
                fesh=266,
                hyperid=266 + i,
                hid=266,
                fee=150 - (i - 10) * 2,
                ts=200220 + i + 13,
                price=500,
                cpa=Offer.CPA_REAL,
                title="Торт {}".format(i),
                waremd5='off-finerate-{}-Hbid1Q'.format(i),
            )
            for i in range(13, 19)
        ]

        for i in range(1, 10):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 200020 + i).respond(0.06)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 200030).respond(0.055)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 200031).respond(0.052)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 200032).respond(0.05)
        for i in range(13, 19):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 200033 + i).respond(0.04)

    def test_incut_high_model_rating(self):
        """
        Проверяем, как собирается врезка с фильтрацией по рейтингу моделей
        """

        def check_response(response):
            # Проверяем, что не показалась обычная премиальная врезка, так как собралась врезка с высоким рейтингом
            self.assertFragmentNotIn(
                response,
                {
                    'incuts': {
                        'results': [
                            {
                                'entity': 'searchIncut',
                                'incutId': 'default',
                            },
                        ],
                    }
                },
            )
            # Офферы с низким рейтингом, которые были бы в топе (из-за ставок), не должны логироваться
            self.show_log_tskv.expect(analog_reason_score=3.58, url_type=6).times(0)
            # Проверяем, что для всех отфильтрованных офферов есть запись о фильтрации в TRACE_ME
            for offer_num in range(1, 4):
                self.assertFragmentIn(
                    response,
                    "[Premium_ads] [highModelRatingIncut] offer off-lowrate-0{}-TT-111Q has been filtered in TCPAShopIncut::FilterResponse".format(
                        offer_num
                    ),
                )

            # Дальше проверяем логику автоброкера. Есть релевантности StableRelevance (это значения формул cpa_shop_incut).
            # Множитель амнистированной ставки выбирается как отношение StableRelevance подпорки текущего оффера к StableRelevance текущего оффера.
            # Подпорка выбирается как максимум из следующего оффера этой же группы (этой же модели) и первого оффера следующей группы.
            # Проверяем, что эта логика сохраняется во врезке с фильтрацией по рейтингу

            # У офферов амнистированная ставка brokeredFee получается так - shopFee умножается на релевантность следующего оффера и делится на релевантность текущего
            self.assertFragmentIn(
                response,
                {
                    'incuts': {
                        'results': [
                            {
                                'entity': 'searchIncut',
                                'incutId': 'cpa_shop_incut_filter_by_model_rating',
                                'title': "Товары с рейтингом 4 и выше",  # Проверяем, что правильно выставлен заголовок врезки, который отрисуется фронтом
                                'items': [
                                    {
                                        'wareId': 'off-finerate-10-Hbid1Q',
                                        'debug': {
                                            'sale': {
                                                'shopFee': 200,
                                                'brokeredFee': 180,  # Этот оффер подпирается оффером с этой же модели, релевантность которого = 27000
                                            },
                                            'properties': {
                                                'CPA_SHOP_INCUT': '30000',
                                            },
                                        },
                                    },
                                    {
                                        'wareId': 'off-finerate-11-Hbid1Q',
                                        'debug': {
                                            'sale': {
                                                'shopFee': 160,
                                                'brokeredFee': 150,  # Этот оффер подпирается следующим оффером модели, 160 * 22500 / 24000 = 150
                                            },
                                            'properties': {
                                                'CPA_SHOP_INCUT': '24000',
                                            },
                                        },
                                    },
                                    {
                                        'wareId': 'off-finerate-12-Hbid1Q',
                                        'debug': {
                                            'sale': {
                                                'shopFee': 150,
                                                'brokeredFee': 144,
                                            },
                                            'properties': {
                                                'CPA_SHOP_INCUT': '22500',
                                            },
                                        },
                                    },
                                    {
                                        'wareId': 'off-finerate-13-Hbid1Q',
                                        'debug': {
                                            'sale': {
                                                'shopFee': 144,
                                                'brokeredFee': 142,
                                            },
                                            'properties': {
                                                'CPA_SHOP_INCUT': '21600',
                                            },
                                        },
                                    },
                                    {
                                        'wareId': 'off-finerate-14-Hbid1Q',
                                        'debug': {
                                            'sale': {
                                                'shopFee': 142,
                                                'brokeredFee': 140,
                                            },
                                            'properties': {
                                                'CPA_SHOP_INCUT': '21300',
                                            },
                                        },
                                    },
                                    # Дальше есть и другие офферы, но у них со списаниями всё аналогично
                                ],
                            },
                        ],
                    }
                },
            )
            # Проверяем, что по трейсам можно понять, из какого оффера выбрана подпорка
            # Оффер off-finerate-10-Hbid1Q подпирается следующим оффером этой же модели
            self.assertFragmentIn(
                response,
                "Calc multiplier: 0.9 for offer [Торт 10] by next offer in group [Торт 10], shopId: 267 relevance: 27000",  # правильная подпорка в ответе cpa_shop_incut
            )
            self.assertFragmentIn(
                response,
                "[Premium_ads] [replay_autobroker] propping relevance for off-finerate-10-Hbid1Q got from the next offer of the same model=27000",  # логика сохранилась
            )
            # Оффер off-finerate-11-Hbid1Q подпирается следующей моделью (как и последующие)
            self.assertFragmentIn(
                response,
                "[Premium_ads] [replay_autobroker] propping relevance for off-finerate-11-Hbid1Q got from the next model=22500",
            )
            self.assertFragmentIn(
                response,
                "[Premium_ads] [replay_autobroker] propping relevance for off-finerate-13-Hbid1Q got from the next model=21300",
            )

            # Проверим, что офферов столько, сколько ожидаем
            elements_in_response = 8  # вообще нужных офферов в индексе 9, но из них лишь 7 попадают в "обычную" выдачу премиальной, из которой собирается эта врезка
            self.assertFragmentIn(
                response,
                {
                    'incuts': {
                        'results': [
                            {
                                'entity': 'searchIncut',
                                'incutId': 'cpa_shop_incut_filter_by_model_rating',
                                'title': "Товары с рейтингом 4 и выше",  # Проверяем, что правильно выставлен заголовок врезки, который отрисуется фронтом
                                'items': ElementCount(elements_in_response),
                            }
                        ]
                    }
                },
            )

        params = {
            "place": "blender",
            "text": "торт",
            "client": "frontend",
            "platform": "desktop",
            'supported-incuts': get_supported_incuts_cgi(),
            "debug": "da",
        }

        rearr_flags = {
            "market_blender_cpa_shop_incut_enabled": 1,
            "market_blender_use_bundles_config": 1,
            "market_cpa_shop_incut_model_rating_to_analogs_fields": 1,
            "market_premium_ads_gallery_never_create_incut_with_high_model_rating": 0,
            # Выбираем бандл, в котором в приоритете врезка cpa_shop_incut_filter_by_model_rating с фильтрацией по рейтингу
            "market_blender_bundles_for_inclid": "2:const_premium_ads_high_model_rating_incut.json",
            "market_premium_ads_incut_get_docs_through_prime": 0,
        }

        response = self.report.request_json(T.get_request(params, rearr_flags))
        check_response(response)

        rearr_flags["market_premium_ads_incut_get_docs_through_prime"] = 1
        response = self.report.request_json(T.get_request(params, rearr_flags))
        check_response(response)

        # Проверим, что для офферов с высоким рейтингом залогируется их рейтинг
        self.show_log_tskv.expect(analog_reason_score=4.23, url_type=6).times(6)
        self.show_log_tskv.expect(analog_reason_score=4.81, url_type=6).times(10)

    def test_cpa_shop_incut_autobroker(self):
        """
        Проверяем, что автоброкер премиальной врезки не ломается для блендерного запроса
        """
        params = {
            "place": "blender",
            "text": "торт",
            "client": "frontend",
            "platform": "desktop",
            'supported-incuts': get_supported_incuts_cgi(),
            "debug": "da",
        }

        rearr_flags = {
            "market_blender_cpa_shop_incut_enabled": 1,
            "market_blender_use_bundles_config": 1,
            # Используем дефолтный бандл - нужна только премиальная врезка, никакого демукса
        }

        response = self.report.request_json(T.get_request(params, rearr_flags))
        # Есть релевантности StableRelevance (это значения формул cpa_shop_incut).
        # Множитель амнистированной ставки выбирается как отношение StableRelevance подпорки текущего оффера к StableRelevance текущего оффера.
        # Подпорка выбирается как максимум из следующего оффера этой же группы (этой же модели) и первого оффера следующей группы.
        # Проверяем, что эта логика сохраняется в премиальной врезке

        # У офферов амнистированная ставка brokeredFee получается так - shopFee умножается на релевантность следующего оффера и делится на релевантность текущего
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'incutId': 'default',
                            'items': [
                                {
                                    'wareId': 'off-lowrate-03-TT-111Q',
                                    'debug': {
                                        'sale': {
                                            'shopFee': 503,
                                            # Этот оффер подпирается оффером следующей модели, релевантность которого = 75300
                                            # brokeredFee = 503 * 75300 / 75450 = 502
                                            'brokeredFee': 502,
                                        },
                                        'properties': {
                                            # Релевантность текущего оффера
                                            'CPA_SHOP_INCUT': '75450',
                                        },
                                    },
                                },
                                {
                                    'wareId': 'off-lowrate-02-TT-111Q',
                                    'debug': {
                                        'sale': {
                                            'shopFee': 502,
                                            'brokeredFee': 501,
                                        },
                                        'properties': {
                                            'CPA_SHOP_INCUT': '75300',
                                        },
                                    },
                                },
                                {
                                    'wareId': 'off-lowrate-01-TT-111Q',
                                    'debug': {
                                        'sale': {
                                            'shopFee': 501,
                                            'brokeredFee': 200,
                                        },
                                        'properties': {
                                            'CPA_SHOP_INCUT': '75150',
                                        },
                                    },
                                },
                                {
                                    'wareId': 'off-finerate-10-Hbid1Q',
                                    'debug': {
                                        'sale': {
                                            'shopFee': 200,
                                            # Релевантность следующего оффера этой же модели - 27000
                                            # Релевантность оффера следующей модели - 24000
                                            # Поэтому в качестве подпорки берём оффер этой же модели с релевантностью 27000
                                            # Тогда brokeredFee = 200 * 27000 / 30000 = 180
                                            'brokeredFee': 180,
                                        },
                                        'properties': {
                                            'CPA_SHOP_INCUT': '30000',
                                        },
                                    },
                                },
                                {
                                    'wareId': 'off-finerate-11-Hbid1Q',
                                    'debug': {
                                        'sale': {
                                            'shopFee': 160,
                                            'brokeredFee': 150,
                                        },
                                        'properties': {
                                            'CPA_SHOP_INCUT': '24000',
                                        },
                                    },
                                },
                                # Дальше есть и другие офферы, но у них со списаниями всё аналогично
                            ],
                        },
                    ],
                },
            },
        )

    def test_incut_high_model_rating_switching_off(self):
        """
        Проверяем, что по умолчанию (со включенным флагом market_premium_ads_gallery_never_create_incut_with_high_model_rating) врезка с высоким рейтингом моделей не собирается
        """
        params = {
            "place": "blender",
            "text": "торт",
            "client": "frontend",
            "platform": "desktop",
            'supported-incuts': get_supported_incuts_cgi(),
            "debug": "da",
        }

        rearr_flags = {
            "market_blender_cpa_shop_incut_enabled": 1,
            "market_blender_use_bundles_config": 1,
            "market_cpa_shop_incut_model_rating_to_analogs_fields": 1,
        }

        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'incutId': 'default',
                            'title': "Популярные предложения",  # Проверяем, что правильно выставлен заголовок врезки, который отрисуется фронтом
                        },
                    ],
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
                            'incutId': 'cpa_shop_incut_filter_by_model_rating',
                        }
                    ]
                }
            },
        )
        # Проверяем также, что врезка не пыталась набираться
        # Важный момент - в используемом бандле врезки cpa_shop_incut_filter_by_model_rating нет, но репорт мог бы потратить время на её сборку, если бы не флаг
        self.assertFragmentIn(
            response,
            "[Premium_ads] [highModelRatingIncut] do not filter cpa shop incut by model rating because of flag",
        )

    @classmethod
    def prepare_use_text_instead_of_suggest_text_for_redirects(cls):
        cls.index.models += [
            Model(
                hid=366,
                hyperid=366 + i,
                title="Чемодан {}".format(i),
                ts=210020 + i,
            )
            for i in range(9)
        ]
        cls.index.offers += [
            Offer(
                fesh=266,
                hyperid=366 + i,
                hid=366,
                fee=500,
                ts=210120 + i,
                price=500,
                cpa=Offer.CPA_REAL,
                title="Чемодан оффер {}".format(i),
                waremd5='off-chemodan-{}-TT-111Q'.format(i),
            )
            for i in range(9)
        ]
        for i in range(9):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 210120 + i).respond(0.3 + i * 0.1)
        # TODO: правильно было бы задать разные скоры формул для текста и бестекста, но так пока нельзя.
        # Как я понял, скор в тестах задаётся общий для всех базовых формул ранжирования.

    def test_use_text_instead_of_suggest_text_for_redirects(self):
        """
        Проверяем, что с флагом market_premium_ads_cpa_shop_incut_suggest_text_to_text при редиректе в категорию
        за счёт подмены параметра suggest_text на text будет использован текстовый поиск (в cpa_shop_incut)
        """
        params = {
            "place": "blender",
            "suggest_text": "чемодан",
            "hid": "366",
            "was_redir": "1",
            "client": "frontend",
            "platform": "desktop",
            'supported-incuts': get_supported_incuts_cgi(),
            "debug": "da",
        }

        rearr_flags = {
            "market_blender_cpa_shop_incut_enabled": 1,
            "market_blender_use_bundles_config": 1,
            "market_blender_media_adv_incut_enabled": 0,
        }

        response = self.report.request_json(T.get_request(params, rearr_flags))
        # В обычной ситуации (когда флаг выключен) - категорийный поиск, используем MNA_fml_formula для ранжирования
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'incutId': 'default',
                            'items': [
                                {
                                    'debug': {
                                        'rankedWith': "MNA_fml_formula_860934",
                                    },
                                },
                            ],
                        },
                    ],
                },
            },
        )
        rearr_flags["market_premium_ads_cpa_shop_incut_suggest_text_to_text"] = 1
        response = self.report.request_json(T.get_request(params, rearr_flags))
        # С включённым флагом за счёт изменения CGI в запросе в cpa_shop_incut используем текстовую формулу
        # При этом прайм это не затрагивает - там используем категорийный поиск
        self.assertFragmentIn(
            response,
            {
                'search': {  # Выдача prime
                    'results': [
                        {
                            'debug': {
                                'rankedWith': Regex(r'base___common_.+'),  # Формула категорийного поиска
                            },
                        },
                    ],
                },
                'incuts': {
                    'results': [  # Выдача cpa_shop_incut
                        {
                            'entity': 'searchIncut',
                            'incutId': 'default',
                            'items': [
                                {
                                    'debug': {
                                        # Формула текстового поиска
                                        'rankedWith': "MNA_fml_formula_860977",
                                    },
                                },
                            ],
                        },
                    ],
                },
            },
        )

    def test_numdoc_flags(self):
        """
        Проверяем, что флагами можем контролировать numdoc и min-num-doc в запросе в cpa_shop_incut из блендера
        """

        def __assert_incut_size_equal(size_expected):
            # Когда size_expected = 1, подразумеваем, что врезка не собралась, и показывается богатый сниппет
            self.assertFragmentIn(
                response,
                {
                    'incuts': {
                        'results': [
                            {
                                'incutId': 'default' if size_expected > 1 else 'cpa_shop_incut_rich_snippet',
                                'entity': 'searchIncut',
                                'items': ElementCount(size_expected),
                            },
                        ],
                    },
                },
            )

        params = {
            "place": "blender",
            "text": "чемодан",
            "client": "frontend",
            "platform": "desktop",
            'supported-incuts': get_supported_incuts_cgi(),
            "debug": "da",
        }

        rearr_flags = {
            "market_blender_cpa_shop_incut_enabled": 1,
            "market_blender_use_bundles_config": 1,
        }

        # В обычной ситуации врезка соберётся, и покажутся все 9 офферов из 9
        response = self.report.request_json(T.get_request(params, rearr_flags))
        __assert_incut_size_equal(9)
        # Если задать min-num-doc больше, чем кол-во офферов, врезка не соберётся. Но покажется богатый сниппет
        rearr_flags['market_cpa_shop_incut_blender_min_numdoc_desktop'] = 10
        response = self.report.request_json(T.get_request(params, rearr_flags))
        __assert_incut_size_equal(1)  # Покажется богатый сниппет
        # Задаём меньший numdoc, проверяем, что ограничение сверху на кол-во возвращаемых офферов работает
        rearr_flags['market_cpa_shop_incut_blender_min_numdoc_desktop'] = 2
        rearr_flags['market_cpa_shop_incut_blender_numdoc'] = 3
        response = self.report.request_json(T.get_request(params, rearr_flags))
        __assert_incut_size_equal(4)  # TODO: сейчас cpa_shop_incut возвращает на 1 оффер больше, чем нужно
        # Проверяем, что флаг тача работает. Если зададим в таче min-num-doc = 10, то врезка не соберётся, и покажется богатый сниппет
        params['platform'] = 'touch'
        params['touch'] = '1'
        rearr_flags['market_cpa_shop_incut_blender_min_numdoc_touch'] = 10
        response = self.report.request_json(T.get_request(params, rearr_flags))
        __assert_incut_size_equal(1)

    @classmethod
    def prepare_rich_snippet_minimal_docs_in_reponse(cls):
        cls.index.models += [
            Model(
                hid=466,
                hyperid=466 + i,
                title="Телевизор {}".format(i),
                ts=220020 + i,
            )
            for i in range(9)
        ]
        cls.index.offers += [  # Офферы с низкой ставкой (ниже rp_fee)
            Offer(
                fesh=266,
                hyperid=466 + i,
                hid=466,
                fee=0,
                ts=220120 + i,
                price=500,
                cpa=Offer.CPA_REAL,
                title="Телевизор оффер {}".format(i),
                waremd5='offer-tv-----{}-TT-111Q'.format(i),
            )
            for i in range(7)
        ]
        cls.index.offers += [  # Офферы со ставкой выше rp_fee (попадут в cpa_shop_incut)
            Offer(
                fesh=266,
                hyperid=466 + i,
                hid=466,
                fee=500,
                ts=220120 + i,
                price=500,
                cpa=Offer.CPA_REAL,
                title="Телевизор оффер {}".format(i),
                waremd5='offer-tv-----{}-TT-111Q'.format(i),
            )
            for i in range(7, 9)
        ]
        cls.index.reserveprice_fee += [ReservePriceFee(hyper_id=466 + i, reserveprice_fee=0.01) for i in range(9)]
        for i in range(9):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 220120 + i).respond(0.3 + i * 0.1)

    def test_rich_snippet_minimal_docs_in_reponse(self):
        """
        Проверяем, что богатый сниппет показывается, когда офферов не хватает на врезку.
        """
        params = {
            "place": "blender",
            "text": "телевизор",
            "client": "frontend",
            "platform": "desktop",
            'supported-incuts': get_supported_incuts_cgi(),
            "debug": "da",
            "use-default-offers": "1",
            "allow-collapsing": "1",
        }
        rearr_flags = {
            "market_blender_cpa_shop_incut_enabled": 1,
            "market_blender_use_bundles_config": 1,
        }
        response = self.report.request_json(T.get_request(params, rearr_flags))
        # В индексе этого теста всего 2 оффера смогут пройти в премиальную (остальные фильтруются по rp_fee)
        # Поэтому покажется богатый сниппет (при этом 2 меньше, чем min-num-doc)
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "incutId": "cpa_shop_incut_rich_snippet",
                            "items": ElementCount(1),
                        },
                    ],
                },
            },
        )
        # Явно передадим min-num-doc больше, чем кол-во офферов, которые проходят в cpa shop incut - ответ не изменится
        rearr_flags["market_cpa_shop_incut_blender_min_numdoc_desktop"] = 3
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "incutId": "cpa_shop_incut_rich_snippet",
                            "items": ElementCount(1),
                        },
                    ],
                },
            },
        )

    @classmethod
    def prepare_fashion_blacklist(cls):
        cls.index.shops += [
            Shop(
                fesh=803,
                priority_region=213,
                fulfillment_program=False,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                name='3P поставщик',
            ),
        ]
        cls.index.hypertree += [
            HyperCategory(
                hid=7877999,
                name='Одежда, обувь и аксессуары',
            ),
        ]
        cls.index.incut_black_list_fb += [
            IncutBlackListFb(texts=['fashion'], inclids=['PremiumAds']),
            IncutBlackListFb(subtreeHids=[7877999], inclids=['PremiumAds']),
        ]
        cls.index.fashion_categories += [
            FashionCategory("CATEGORY_COMMON", 7877999),
        ]
        cls.index.models += [
            Model(
                hid=7877999,
                hyperid=4010 + i,
                ts=610 + i,
                title='model fashion {}'.format(4010 + i),
                vbid=11,
            )
            for i in range(9)
        ]
        cls.index.mskus += [
            MarketSku(
                title="model_{}_msku_1_fashion".format(4010 + i),
                hid=7877999,
                hyperid=4010 + i,
                sku=320010 + i,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=1600,
                        feedid=803,
                        fee=500,
                        waremd5="BLUE-{}-FEED-0001Q".format(320010 + i),
                        title="model_{} 3P buybox offer fashion".format(4010 + i),
                        ts=8017 + i,
                    ),
                ],
            )
            for i in range(8)
        ]
        for i in range(8):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 8017 + i).respond(0.3 + i * 0.1)

    def test_fashion_blacklist(self):
        """
        Проверяем работу флага market_premium_ads_gallery_no_blacklist_for_fashion, который выключает blacklist для fashion
        Отдельный тест именно для блендера нужен, так как сам блендер проверяет, находится ли запрос в blacklist, прежде чем запрашивать врезки
        """
        params = {
            "place": "blender",
            "suggest_text": "fashion",
            "hid": "7877999",
            "client": "frontend",
            "platform": "desktop",
            'supported-incuts': get_supported_incuts_cgi(),
            "debug": "da",
        }
        rearr_flags = {
            "market_blender_cpa_shop_incut_enabled": 1,
            "market_blender_use_bundles_config": 1,
            "market_blender_media_adv_incut_enabled": 0,
        }
        # По умолчанию флаг не задан, запрос попадёт в blacklist, и врезка не соберётся
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": EmptyList(),
                }
            },
        )
        # Выставляем флаг, который отключит blacklist на категорию fashion, и врезка соберётся
        rearr_flags["market_premium_ads_gallery_no_blacklist_for_fashion"] = 1
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "incutId": "default",
                            "items": ElementCount(8),
                        },
                    ],
                },
            },
        )

    @classmethod
    def prepare_sku_aware_fields_in_cpa_shop_incut_result(cls):
        cls.index.gltypes += [  # Фильтр для проверки skuAwareSpecs-полей
            GLType(
                hid=3010,
                param_id=10123,
                cluster_filter=True,
                gltype=GLType.ENUM,
                values=[
                    GLValue(value_id=1, text='gl_filter_value1'),
                ],
                model_filter_index=1,
                xslname='sku_filter',
            ),
        ]
        cls.index.model_description_templates += [
            ModelDescriptionTemplates(
                hid=3010,
                friendlymodel=['model friendly {sku_filter}'],
                model=[("Основное", {'model full': '{sku_filter}'})],
            ),
        ]
        cls.index.models += [
            Model(
                hid=3010,
                hyperid=3010,
                ts=710,
                title='model skuaware 3010',
                vbid=11,
            )
        ]
        cls.index.models += [
            Model(
                hid=3010,
                hyperid=3010 + i,
                ts=710 + i,
                title='model skuaware {}'.format(3010 + i),
                vbid=11,
            )
            for i in range(1, 8)
        ]
        cls.index.mskus += [
            MarketSku(
                title="model_3010_msku_1_skuaware",
                hid=3010,
                hyperid=3010,
                sku=323010,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=1600,
                        feedid=803,
                        fee=500,
                        waremd5="BLUE-323010-FEED-0001Q",
                        title="model_3010 3P buybox offer skuaware",
                        ts=9017,
                    ),
                ],
                glparams=[GLParam(param_id=10123, value=1)],
            ),
        ]
        cls.index.mskus += [
            MarketSku(
                title="model_{}_msku_1_skuaware".format(3010 + i),
                hid=3010,
                hyperid=3010 + i,
                sku=323010 + i,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=1603,
                        feedid=803,
                        fee=450,
                        waremd5="BLUE-{}-FEED-0001Q".format(323010 + i),
                        title="model_{} 3P buybox offer skuaware".format(3010 + i),
                        ts=9017 + i,
                    ),
                ],
            )
            for i in range(1, 8)
        ]
        for i in range(8):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 9017 + i).respond(0.4)

    def test_sku_aware_fields_in_cpa_shop_incut_result(self):
        """
        Проверяем, что в cpa_shop_incut в ответе репорта есть skuAware поля, когда включён флаг market_cpa_shop_incut_request_sku_aware_fields,
        и что эти поля не теряются при формировании рекламы блендером
        """
        params = {
            "place": "blender",
            "text": "skuaware",
            "client": "frontend",
            "platform": "desktop",
            "supported-incuts": get_supported_incuts_cgi(),
            "debug": "da",
            "show-models-specs": "msku-friendly,msku-full",
        }
        rearr_flags = {
            "market_blender_cpa_shop_incut_enabled": 1,
            "market_blender_use_bundles_config": 1,
            "market_blender_bundles_row_position_format": 1,
            "market_blender_media_adv_incut_enabled": 0,
            "market_cpa_shop_incut_request_sku_aware_fields": 1,
        }
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "incutId": "default",
                            "items": [
                                {
                                    "slug": "model-3010-3p-buybox-offer-skuaware",
                                    "wareId": "BLUE-323010-FEED-0001Q",
                                    "skuAwareTitles": {
                                        "raw": "model_3010_msku_1_skuaware",  # title, пришедший не от оффера, а от msku
                                        "highlighted": [
                                            {
                                                "value": "model_3010_msku_1_",
                                            },
                                            {
                                                "value": "skuaware",
                                            },
                                        ],
                                    },
                                    "skuAwarePictures": [
                                        {
                                            "entity": "picture",
                                            "original": NotEmpty(),
                                        },
                                    ],
                                    "skuAwareSpecs": {  # ModelDescriptionTemplates для hid=3010
                                        "full": [
                                            {
                                                "groupName": "Основное",
                                                "groupSpecs": [
                                                    {
                                                        "name": "model full",
                                                        "value": "gl_filter_value1",
                                                    },
                                                ],
                                            },
                                        ],
                                        "friendly": [
                                            "model friendly gl_filter_value1",
                                        ],
                                        "friendlyext": [
                                            {
                                                "value": "model friendly gl_filter_value1",
                                                "usedParams": [10123],  # id GL-фильтра
                                            },
                                        ],
                                    },
                                },
                            ],
                        },
                    ],
                },
            },
        )
        # Выключаем флаг, проверяем, что из выдачи пропадут skuAware-поля
        rearr_flags["market_cpa_shop_incut_request_sku_aware_fields"] = 0
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "incutId": "default",
                            "items": [
                                {
                                    "slug": "model-3010-3p-buybox-offer-skuaware",
                                    "wareId": "BLUE-323010-FEED-0001Q",
                                    "skuAwareTitles": Absent(),
                                    "skuAwarePictures": Absent(),
                                    "skuAwareSpecs": Absent(),
                                },
                            ],
                        },
                    ],
                },
            },
        )

    def test_switching_off_cpa_shop_incut(self):
        """
        Проверяем работу флага market_premium_ads_gallery_disable, который выключает премиальную
        врезку даже в случае, когда она запрашивается блендером
        """
        params = {
            "place": "blender",
            "text": "skuaware",
            "client": "frontend",
            "platform": "desktop",
            "supported-incuts": get_supported_incuts_cgi(),
            "debug": "da",
        }
        rearr_flags = {
            "market_blender_cpa_shop_incut_enabled": 1,  # Считаем, что этот флаг всегда шлёт фронт
            "market_blender_use_bundles_config": 1,
            "market_blender_bundles_row_position_format": 1,
            "market_blender_media_adv_incut_enabled": 0,
            "market_premium_ads_gallery_disable": 1,  # Включаем флаг - врезка не должна собраться
        }
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(response, {"incuts": {"results": EmptyList()}})

    def test_two_incuts_on_same_page(self):
        """
        Проверяем бандл, где 2 врезки на одной выдаче - high_model_rating_incut и cpa_shop_incut
        """
        params = {
            "place": "blender",
            "text": "торт",
            "client": "frontend",
            "platform": "desktop",
            'supported-incuts': get_supported_incuts_cgi(),
            "debug": "da",
        }

        rearr_flags = {
            "market_blender_cpa_shop_incut_enabled": 1,
            "market_blender_use_bundles_config": 1,
            "market_cpa_shop_incut_model_rating_to_analogs_fields": 1,
            "market_premium_ads_gallery_never_create_incut_with_high_model_rating": 0,
            "market_blender_media_adv_incut_enabled": 0,
            # Сначала выбираем бандл, где на первом месте премиальная врезка
            "market_blender_bundles_for_inclid": "2:const_premium_ads_default_then_high_rating_incut.json",
            # Проверяем со старой логикой
            "market_cpa_shop_incut_premium_ads_use_demux": 0,
        }
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'incutId': 'default',
                            'position': 1,
                        },
                        {
                            'entity': 'searchIncut',
                            'incutId': 'cpa_shop_incut_filter_by_model_rating',
                            'position': 5,  # 1-я позиция - cpa_shop_incut, дальше 3 поисковых сниппета, потом эта врезка
                        },
                    ],
                },
            },
        )
        # Выбираем бандл, где на первом месте врезка "товары с высоким рейтингом"
        rearr_flags["market_blender_bundles_for_inclid"] = "2:const_premium_ads_high_rating_then_default_incut.json"
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'incutId': 'cpa_shop_incut_filter_by_model_rating',
                            'position': 1,
                        },
                        {
                            'entity': 'searchIncut',
                            'incutId': 'default',
                            'position': 5,  # 1-я позиция - товары с высоким рейтингом, дальше 3 поисковых сниппета, потом эта врезка
                        },
                    ],
                },
            },
        )
        # Проверяем случай, когда врезка "товары с высоким рейтингом" не набирается - должна быть только одна премиальная врезка на первой позиции
        params["text"] = "маркс"
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(  # Смотрим, что премиальная врезка попала на первую позицию
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'incutId': 'default',
                            'position': 1,
                        },
                    ],
                },
            },
            allow_different_len=False,
        )
        self.assertFragmentNotIn(  # Смотрим, что премиальная врезка не продублировалась
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'incutId': 'default',
                            'position': 5,
                        },
                    ],
                },
            },
        )

    def test_three_incuts_on_same_page(self):
        """
        Проверяем, что на первой позиции показывается вендорская врезка, если она собирается
        """
        params = {
            "place": "blender",
            "text": "торт",
            "client": "frontend",
            "platform": "desktop",
            "hid": 266,
            'supported-incuts': get_supported_incuts_cgi(),
            'use-default-offers': 1,
            'allow-collapsing': 1,
            'pp': 7,
            'show-urls': 'productVendorBid',
            'market_vendor_incut_size': 6,
            "debug": "da",
        }

        rearr_flags = {
            "market_blender_cpa_shop_incut_enabled": 1,
            "market_blender_use_bundles_config": 1,
            "market_cpa_shop_incut_model_rating_to_analogs_fields": 1,
            "market_premium_ads_gallery_never_create_incut_with_high_model_rating": 0,
            "market_blender_media_adv_incut_enabled": 0,
            # Сначала выбираем бандл, где сначала премиальная врезка, потом "товары с высоким рейтингом"
            # Бандл для вендорской врезки по умолчанию не задан (чтобы не задевал другие тесты), передаём его флагом тут, чтобы вендорская врезка собиралась
            "market_blender_bundles_for_inclid": "2:const_premium_ads_default_then_high_rating_incut.json,3:const_vendor_incut_1.json",
            "market_report_blender_vendor_incut_enable": 1,
            "market_vendor_incut_hide_undeliverable_models": 0,
            # Проверяем старую логику
            "market_cpa_shop_incut_premium_ads_use_demux": 0,
        }
        # Вендорская врезка собралась, она показывается над выдачей, далее показывается премиальная, затем "товары с высоким рейтингом"
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'incutId': 'vendor_incut',  # вендорская врезка
                            'position': 1,  # первая позиция, над выдачей
                        },
                        {
                            'entity': 'searchIncut',
                            'incutId': 'default',
                            'position': 4,  # первые 3 позиции занимают поисковые сниппеты, 4-ую позицию - премиальная
                        },
                        {
                            'entity': 'searchIncut',
                            'incutId': 'cpa_shop_incut_filter_by_model_rating',  # "товары с высоким рейтингом"
                            'position': 8,  # поисковые сниппеты занимают позиции 1-3 и 5-7, врезка под шестым сниппетом поиска
                        },
                    ],
                },
            },
        )
        # Выбираем бандл, где на первом месте врезка "товары с высоким рейтингом"
        rearr_flags[
            "market_blender_bundles_for_inclid"
        ] = "2:const_premium_ads_high_rating_then_default_incut.json,3:const_vendor_incut_1.json"
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'incutId': 'vendor_incut',  # вендорская врезка
                            'position': 1,  # первая позиция, над выдачей
                        },
                        {
                            'entity': 'searchIncut',
                            'incutId': 'cpa_shop_incut_filter_by_model_rating',  # "товары с высоким рейтингом"
                            'position': 4,  # первые 3 позиции занимают поисковые сниппеты, 4-ую позицию - врезка "товары с высоким рейтингом"
                        },
                        {
                            'entity': 'searchIncut',
                            'incutId': 'default',
                            'position': 8,  # поисковые сниппеты занимают позиции 1-3 и 5-7, премиальная врезка под шестым сниппетом поиска
                        },
                    ],
                },
            },
        )
        # Проверяем, что если врезка "товары с высоким рейтингом" не набирается, вместо неё премиальная поднимается выше
        rearr_flags[
            "market_premium_ads_gallery_never_create_incut_with_high_model_rating"
        ] = 1  # Ставим флаг, чтобы "товары с высоким рейтингом" не набирались
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'incutId': 'vendor_incut',  # вендорская врезка
                            'position': 1,  # первая позиция, над выдачей
                        },
                        {
                            'entity': 'searchIncut',
                            'incutId': 'default',
                            'position': 4,  # "товары с высоким рейтингом" не набрались, премиальная поднялась на 4 позицию
                        },
                    ],
                },
            },
        )

    def test_request_appears_in_debug(self):
        """
        Проверяем, что по дебаг-трейсам легко понять, какой запрос в какой плейс отправлялся
        """
        params = {
            "place": "blender",
            "text": "торт",
            "client": "frontend",
            "platform": "desktop",
            "hid": 266,
            'supported-incuts': get_supported_incuts_cgi(),
            'use-default-offers': 1,
            'allow-collapsing': 1,
            'pp': 7,
            'show-urls': 'productVendorBid',
            'market_vendor_incut_size': 6,
            "debug": "da",
        }

        rearr_flags = {
            "market_blender_cpa_shop_incut_enabled": 1,
            "market_blender_use_bundles_config": 1,
            "market_cpa_shop_incut_model_rating_to_analogs_fields": 1,
            "market_premium_ads_gallery_never_create_incut_with_high_model_rating": 0,
            "market_blender_media_adv_incut_enabled": 0,
            "market_blender_bundles_for_inclid": "2:const_premium_ads_default_then_high_rating_incut.json,3:const_vendor_incut_1.json",
            "market_report_blender_vendor_incut_enable": 1,
            "market_vendor_incut_hide_undeliverable_models": 0,
        }

        response = self.report.request_json(T.get_request(params, rearr_flags))
        # В запросе потенциально могут быть премиальная и вендорская врезки - ищем соответствующие подзапросы
        for place in ["TCPAShopIncut", "TVendorIncutAssistant", "TVendorIncut"]:
            self.assertFragmentIn(
                response,
                "Perform additional request with TConcurrentPlaceRunner (custom cgi) to place=NMarketReport::" + place,
            )

    def test_vendor_incut_then_cpa_shop_incut_by_default(self):
        """
        Проверяем, что по умолчанию, когда вендорская врезка набирается,
        премиальная врезка тоже набирается, но не показывается
        """
        params = {
            "place": "blender",
            "text": "торт",
            "client": "frontend",
            "platform": "desktop",
            "hid": 266,
            'supported-incuts': get_supported_incuts_cgi(),
            'use-default-offers': 1,
            'allow-collapsing': 1,
            'pp': 7,
            'show-urls': 'productVendorBid',
            'market_vendor_incut_size': 6,
            "debug": "da",
        }

        rearr_flags = {
            # Передаём бандлы, которые прописаны в конфиге бандлов по умолчанию (не в тесте, а для продового репорта)
            "market_blender_bundles_for_inclid": "3:const_vendor_incut_1.json",
            "market_blender_cpa_shop_incut_enabled": 1,
            "market_blender_use_bundles_config": 1,
            "market_cpa_shop_incut_model_rating_to_analogs_fields": 1,
            "market_blender_media_adv_incut_enabled": 0,
            "market_report_blender_vendor_incut_enable": 1,
            "market_vendor_incut_hide_undeliverable_models": 0,
        }
        # Вендорская врезка собралась, она показывается над выдачей, далее показывается премиальная, затем "товары с высоким рейтингом"
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'incutId': 'vendor_incut',  # вендорская врезка
                            'position': 1,  # первая позиция, над выдачей
                        },
                    ],
                },
            },
            allow_different_len=False,
        )
        self.assertFragmentNotIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'incutId': 'default',
                        },
                    ],
                },
            },
        )
        # Имитируем ситуацию, когда вендорская врезка не собралась - премиальная будет на первой позиции
        rearr_flags["market_report_blender_vendor_incut_enable"] = 0
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'incutId': 'default',  # премиальная врезка
                            'position': 1,  # первая позиция "внутри" поисковой выдачи
                        },
                    ],
                },
            },
        )
        # Устанавливаем min-num-doc для премиальной врезки таким, чтобы он не был удовлетворён
        # Тогда премиальная врезка не наберётся и покажется богатый сниппет
        rearr_flags['market_cpa_shop_incut_blender_min_numdoc_desktop'] = 100
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'incutId': 'cpa_shop_incut_rich_snippet',  # богатый сниппет
                            'position': 1,  # первая позиция "внутри" поисковой выдачи
                            'items': ElementCount(1),
                        },
                    ],
                },
            },
        )

    def test_high_rating_incut_pp(self):
        """
        Проверяем, что правильно проставляются pp врезки "товары с рейтингом 4 и выше"
        """
        params = {
            "place": "blender",
            "text": "торт",
            "client": "frontend",
            "platform": "desktop",
            'supported-incuts': get_supported_incuts_cgi(),
            "debug": "da",
        }

        rearr_flags = {
            "market_blender_cpa_shop_incut_enabled": 1,
            "market_blender_use_bundles_config": 1,
            "market_cpa_shop_incut_model_rating_to_analogs_fields": 1,
            "market_premium_ads_gallery_never_create_incut_with_high_model_rating": 0,
            # Выбираем бандл, в котором в приоритете врезка cpa_shop_incut_filter_by_model_rating с фильтрацией по рейтингу
            "market_blender_bundles_for_inclid": "2:const_premium_ads_high_model_rating_incut.json",
        }

        def check_pp(response, expected_pp):
            # Проверим ответ репорта
            self.assertFragmentIn(
                response,
                {
                    'incuts': {
                        'results': [
                            {
                                'entity': 'searchIncut',
                                'incutId': 'cpa_shop_incut_filter_by_model_rating',
                                'items': [
                                    {
                                        'wareId': "off-finerate-10-Hbid1Q",
                                        'urls': {'encrypted': Contains('/pp={}/'.format(expected_pp))},
                                        'feeShowPlain': Contains("pp: {}".format(expected_pp)),
                                    },
                                    {
                                        'wareId': "off-finerate-11-Hbid1Q",
                                        'urls': {'encrypted': Contains('/pp={}/'.format(expected_pp))},
                                        'feeShowPlain': Contains("pp: {}".format(expected_pp)),
                                    },
                                    {
                                        'wareId': "off-finerate-12-Hbid1Q",
                                        'urls': {'encrypted': Contains('/pp={}/'.format(expected_pp))},
                                        'feeShowPlain': Contains("pp: {}".format(expected_pp)),
                                    },
                                    {
                                        'wareId': "off-finerate-13-Hbid1Q",
                                        'urls': {'encrypted': Contains('/pp={}/'.format(expected_pp))},
                                        'feeShowPlain': Contains("pp: {}".format(expected_pp)),
                                    },
                                    # Могут быть и другие офферы, но их разное кол-во на разных платформах (разные numdoc'и)
                                ],
                            },
                        ],
                    },
                },
            )
            # Проверим логи
            self.show_log_tskv.expect(ware_md5="off-finerate-10-Hbid1Q", url_type=6, pp=expected_pp).once()
            self.show_log_tskv.expect(ware_md5="off-finerate-11-Hbid1Q", url_type=6, pp=expected_pp).once()
            self.show_log_tskv.expect(ware_md5="off-finerate-12-Hbid1Q", url_type=6, pp=expected_pp).once()
            self.show_log_tskv.expect(ware_md5="off-finerate-13-Hbid1Q", url_type=6, pp=expected_pp).once()

        # Проверяем desktop
        response = self.report.request_json(T.get_request(params, rearr_flags))
        check_pp(response, 54)

        # Проверяем touch
        params["platform"] = "touch"
        params["touch"] = "1"
        response = self.report.request_json(T.get_request(params, rearr_flags))
        check_pp(response, 654)

        # Проверяем ANDROID
        del params["platform"]
        del params["touch"]
        params["client"] = "ANDROID"
        response = self.report.request_json(T.get_request(params, rearr_flags))
        check_pp(response, 1754)

        # Проверяем IOS
        params["client"] = "IOS"
        response = self.report.request_json(T.get_request(params, rearr_flags))
        check_pp(response, 1854)

        # Проверяем, что флагом market_cpa_shop_incut_premium_ads_use_different_pp
        # можно вернуть pp премиальной врезки (не использовать отдельный pp в рейтинговой врезке)
        # TODO: убрать эту часть теста, когда будем убирать флаг
        rearr_flags["market_cpa_shop_incut_premium_ads_use_different_pp"] = 0
        response = self.report.request_json(T.get_request(params, rearr_flags))
        check_pp(response, 1809)  # pp = 1809 => IOS, премиальная врезка

    def test_rating_incut_title_overriding(self):
        """
        Проверяем, что у рейтинговой врезки можно изменить заголовок, передав его флагом market_cpa_shop_incut_premium_ads_new_title_for_rating_incut
        """
        params = {
            "place": "blender",
            "text": "торт",
            "client": "frontend",
            "platform": "desktop",
            'supported-incuts': get_supported_incuts_cgi(),
            "debug": "da",
        }

        rearr_flags = {
            "market_blender_cpa_shop_incut_enabled": 1,
            "market_blender_use_bundles_config": 1,
            "market_cpa_shop_incut_model_rating_to_analogs_fields": 1,
            "market_premium_ads_gallery_never_create_incut_with_high_model_rating": 0,
            "market_blender_media_adv_incut_enabled": 0,
            # Сначала выбираем бандл, где на первом месте премиальная врезка
            "market_blender_bundles_for_inclid": "2:const_premium_ads_default_then_high_rating_incut.json",
            # Проверяем со старой логикой
            "market_cpa_shop_incut_premium_ads_use_demux": 0,
            "market_cpa_shop_incut_premium_ads_new_title_for_rating_incut": 1,
        }
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'incutId': 'default',
                            # Проверяем, что заголовок премиальной врезки не испортился
                            'title': "Популярные предложения",
                        },
                        {
                            'entity': 'searchIncut',
                            'incutId': 'cpa_shop_incut_filter_by_model_rating',
                            # Проверяем, что правильно выставлен заголовок врезки, который отрисуется фронтом
                            'title': "Товары с высоким рейтингом",
                        },
                    ],
                },
            },
            preserve_order=True,
        )

    def test_shop_fields_in_vendor_urls(self):
        """
        Проверяем, что в promotion-ссылки (вендорские) попадает информация о мерче;
        проверяем, что ссылки правильно перегенериваются блендером
        https://st.yandex-team.ru/MARKETOUT-46914
        Выключается флагом market_money_add_shop_params_to_vendor_clicks_log
        """
        params = {
            "place": "blender",
            "text": "торт",
            "client": "frontend",
            "platform": "desktop",
            'supported-incuts': get_supported_incuts_cgi(),
            "debug": "da",
            "show-urls": "cpa,promotion",
        }

        rearr_flags = {
            "market_blender_cpa_shop_incut_enabled": 1,
            "market_blender_use_bundles_config": 1,
            # Проверяем на дефолтном бандле
        }
        response = self.report.request_json(T.get_request(params, rearr_flags))
        # Парсим характеристики оффера из выдачи
        feed_id_captured, offer_id_captured = Capture(), Capture()
        shop_id_captured = Capture()
        shop_fee_captured, brokered_fee_captured = Capture(), Capture()
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "incutId": "default",
                            "items": [
                                {
                                    "shop": {
                                        "id": NotEmpty(capture=shop_id_captured),
                                    },
                                    "debug": {
                                        "feed": {
                                            "id": NotEmpty(capture=feed_id_captured),
                                            "offerId": NotEmpty(capture=offer_id_captured),
                                        },
                                        "sale": {
                                            "shopFee": NotEmpty(capture=shop_fee_captured),
                                            "brokeredFee": NotEmpty(capture=brokered_fee_captured),
                                        },
                                    },
                                },
                            ],
                        },
                    ],
                },
            },
        )
        # Проверяем, что требуемые характеристики оффера попали в promotion-ссылку
        self.click_log.expect(
            ClickType.PROMOTION,
            shop_id=shop_id_captured.value,
            supplier_id=None,  # У белых офферов supplier_id пропущено
            feed_id=feed_id_captured.value,
            offer_id=offer_id_captured.value,
            shop_fee=shop_fee_captured.value,
            shop_fee_ab=brokered_fee_captured.value,
            url_type=UrlType.PROMOTION,
        )

        # Проверим, что флагом market_money_add_shop_params_to_vendor_clicks_log можно выключить
        rearr_flags["market_money_add_shop_params_to_vendor_clicks_log"] = 0
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.click_log.expect(
            ClickType.PROMOTION,
            shop_id=None,
            supplier_id=None,
            feed_id=None,
            offer_id=None,
            shop_fee=None,
            shop_fee_ab=None,
            url_type=UrlType.PROMOTION,
        )

    @classmethod
    def prepare_redirect_with_text_param(cls):
        cls.index.models += [
            Model(
                hid=793,
                hyperid=793 + i,
                title="candy mars {}".format(i),
                ts=210020 + i,
            )
            for i in range(10)
        ]

        cls.index.models += [
            Model(
                hid=793,
                hyperid=793 + 10 + i,
                title="candy snickers {}".format(i),
                ts=210020 + 10 + i,
            )
            for i in range(10)
        ]

        cls.index.offers += [
            Offer(
                fesh=5566,
                hyperid=793 + i,
                hid=793,
                fee=500,
                ts=210120 + i,
                price=500,
                cpa=Offer.CPA_REAL,
                title="candy mars offer {}".format(i),
            )
            for i in range(10)
        ]

        cls.index.offers += [
            Offer(
                fesh=5566,
                hyperid=793 + 10 + i,
                hid=793,
                fee=500,
                ts=210120 + 10 + i,
                price=1000,  # дороже
                cpa=Offer.CPA_REAL,
                title="candy snickers offer {}".format(i),
            )
            for i in range(10)
        ]
        for i in range(20):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 210120 + i).respond(0.3 + i * 0.1)

        cls.reqwizard.on_default_request().respond()

        # данные для категорийного редиректа
        cls.reqwizard.on_request('mars').respond(
            token_chars=[
                ReqwExtMarkupTokenChar(begin_char=0, end_char=4),
            ],
            found_main_categories=[793],
            found_categories_positions=[ReqwExtMarkupToken(begin=1, end=2, data=ReqwExtMarkupMarketCategory(hid=793))],
        )

    def test_redirect_with_text_param(self):
        """
        сохранения параметра text при редиректах
        @see https://st.yandex-team.ru/MARKETOUT-47396
        """
        params = {
            "place": "blender",
            "text": "mars",
            "client": "frontend",
            "platform": "desktop",
            'supported-incuts': get_supported_incuts_cgi(),
            "show-urls": "cpa,promotion",
            'cvredirect': 1,
        }

        rearr_flags = {
            "market_blender_cpa_shop_incut_enabled": 1,
            "market_blender_use_bundles_config": 1,
            # Проверяем на дефолтном бандле
            "market_report_blender_premium_ios_text_redirect": 0,  # как было раньше
            'market_filter_offers_with_model_without_sku': 0,
        }
        response = self.report.request_json(T.get_request(params, rearr_flags))
        rs = response.root['redirect']['params']['rs'][0].encode(
            'utf-8'
        )  # добавляем report-state для следующего запроса
        params['rs'] = rs
        params['hid'] = response.root['redirect']['params']['hid'][0]
        params.pop('text')  # следующий запрос без текса, только данные из редиректа
        params.pop('cvredirect')

        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'titles': {
                                'raw': Contains('mars'),
                            },
                        },
                    ]
                },
                "incuts": {
                    "results": [
                        {
                            "items": [
                                {
                                    'titles': {
                                        'raw': Contains(
                                            'snickers'
                                        ),  # отображается та, что дороже (не учитывая текст, которого нет)
                                    },
                                },
                            ],
                        },
                    ],
                },
            },
        )

        # включение фичи, когда берется текстовый параметр из ReportState
        rearr_flags['market_report_blender_premium_ios_text_redirect'] = 1
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'titles': {
                                'raw': Contains('mars'),
                            },
                        },
                    ]
                },
                "incuts": {
                    "results": [
                        {
                            "items": [
                                {
                                    'titles': {
                                        'raw': Contains('mars'),  # врезка собралась с учетом текста
                                    },
                                },
                            ],
                        },
                    ],
                },
            },
        )

    @classmethod
    def prepare_redirect_with_text_param_by_whitelist(cls):
        cls.index.models += [
            Model(
                hid=1793,
                hyperid=1793 + i,
                title="животное бегемот {}".format(i),
                ts=210020 + i,
            )
            for i in range(10)
        ]

        cls.index.models += [
            Model(
                hid=1793,
                hyperid=1793 + 10 + i,
                title="животное суслик {}".format(i),
                ts=210020 + 10 + i,
            )
            for i in range(10)
        ]

        cls.index.offers += [
            Offer(
                fesh=5566,
                hyperid=1793 + i,
                hid=1793,
                fee=500,
                ts=210120 + i,
                price=500,
                cpa=Offer.CPA_REAL,
                title="оффер животное бегемот {}".format(i),
            )
            for i in range(10)
        ]

        cls.index.offers += [
            Offer(
                fesh=5566,
                hyperid=1793 + 10 + i,
                hid=1793,
                fee=500,
                ts=210120 + 10 + i,
                price=1000,  # дороже
                cpa=Offer.CPA_REAL,
                title="оффер животное суслик {}".format(i),
            )
            for i in range(10)
        ]
        for i in range(20):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 210120 + i).respond(0.3 + i * 0.1)

        # редирект по вайтлисту
        params = {
            'hid': 1793,
        }

        cls.index.redirect_whitelist_records += [
            RedirectWhiteListRecord(query='бегемот', url=T.get_request(params, {})),
        ]
        cls.reqwizard.on_default_request().respond()

    def test_redirect_with_text_param_by_whitelist(self):
        """
        прокидывание текста запроса при редиректе по вайтлистам
        @see https://st.yandex-team.ru/MARKETOUT-47588
        """
        params = {
            "place": "blender",
            "text": "бегемот",
            "client": "IOS",
            'supported-incuts': get_supported_incuts_cgi(),
            "show-urls": "cpa,promotion",
            'pp': 18,
            'cvredirect': 1,
        }
        rearr_flags = {
            "market_blender_cpa_shop_incut_enabled": 1,
            "market_blender_use_bundles_config": 1,
            'market_filter_offers_with_model_without_sku': 0,
        }
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                'redirect': {
                    'url': NotEmpty(),
                }
            },
        )

        url = response.root['redirect']['url'].encode('utf-8').replace('?', '&')
        params.pop('text')
        params.pop('cvredirect')
        rearr_flags['market_report_blender_premium_ios_text_redirect'] = 0
        response = self.report.request_json('{}&{}'.format(url, T.get_request(params, rearr_flags)))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "items": [
                                {
                                    'titles': {
                                        'raw': Contains('суслик'),  # врезка собралась без учета текста
                                    },
                                },
                            ],
                        },
                    ],
                },
            },
        )

        # включение фичи, когда берется текстовый параметр из ReportState
        rearr_flags['market_report_blender_premium_ios_text_redirect'] = 1
        response = self.report.request_json('{}&{}'.format(url, T.get_request(params, rearr_flags)))
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': NotEmpty(),
                },
                "incuts": {
                    "results": [
                        {
                            "items": [
                                {
                                    'titles': {
                                        'raw': Contains('бегемот'),  # врезка собралась с учетом текста
                                    },
                                },
                            ],
                        },
                    ],
                },
            },
        )

    @classmethod
    def prepare_vendor_cpa_auction(cls):
        cls.index.shops += [
            Shop(fesh=1, priority_region=213, cpa=Shop.CPA_REAL, name='CPA Магазин в Москве #1'),
            Shop(fesh=2, priority_region=213, cpa=Shop.CPA_REAL, name='CPA Магазин в Москве #2'),
        ]
        cls.index.offers += [
            Offer(
                hyperid=101,
                hid=1011,
                fesh=1,
                ts=101001,
                price=10000,
                fee=100,
                cpa=Offer.CPA_REAL,
                autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                    id=444,
                    datasource_id=1,
                    strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=100),
                    # fee = 600
                ),
            ),
            Offer(
                hyperid=101,
                hid=1011,
                fesh=2,
                ts=101002,
                price=10000,
                fee=500,
                cpa=Offer.CPA_REAL,
                autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                    id=444,
                    datasource_id=1,
                    strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=10),
                    # fee = 60
                ),
            ),
            Offer(hyperid=102, hid=1011, fesh=1, ts=102001, price=10000, fee=400, cpa=Offer.CPA_REAL),
            Offer(
                hyperid=103,
                hid=1011,
                fesh=1,
                ts=103001,
                price=10000,
                fee=0,
                cpa=Offer.CPA_REAL,
                autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                    id=444,
                    datasource_id=1,
                    strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=50),
                    # fee = 300
                ),
            ),
            Offer(hyperid=104, hid=1011, fesh=1, ts=104001, price=10000, fee=100, cpa=Offer.CPA_REAL),
            Offer(hyperid=105, hid=1011, fesh=1, ts=105001, price=10000, fee=90, cpa=Offer.CPA_REAL),
            Offer(hyperid=106, hid=1011, fesh=1, ts=106001, price=10000, fee=80, cpa=Offer.CPA_REAL),
            Offer(hyperid=107, hid=1011, fesh=1, ts=107001, price=10000, fee=70, cpa=Offer.CPA_REAL),
            Offer(hyperid=108, hid=1011, fesh=1, ts=108001, price=10000, fee=60, cpa=Offer.CPA_REAL),
        ]

    def test_vendor_cpa_auction(self):
        # проверяем что поофферная вендорская ставка корректно работает с флагом market_premium_ads_incut_get_docs_through_prime
        params = {
            "place": "prime",
            "blender": 1,
            "hid": 1011,
            'supported-incuts': get_supported_incuts_cgi(),
            "show-urls": "cpa,promotion",
            'pp': 7,
            "rids": 213,
            "client": "frontend",
            "platform": "desktop",
            "debug": 1,
        }
        rearr_flags = {
            "market_blender_cpa_shop_incut_enabled": 1,
            "market_blender_use_bundles_config": 1,
            'market_filter_offers_with_model_without_sku': 0,
            "market_blender_media_adv_incut_enabled": 0,
            "market_premium_ads_incut_get_docs_through_prime": 1,
            "market_premium_ads_gallery_shop_incut_logarithm_price": 0,
            "market_premium_ads_gallery_shop_incut_vendor_bid_to_fee_coef": 0.05,
        }
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "items": [
                                {
                                    'shop': {'id': 1},
                                    'model': {'id': 101},
                                },
                                {
                                    'shop': {'id': 1},
                                    'model': {'id': 102},
                                },
                                {
                                    'shop': {'id': 1},
                                    'model': {'id': 103},
                                },
                            ],
                        },
                    ],
                },
            },
            preserve_order=True,
        )

        self.click_log.expect(clicktype=ClickType.CPA, shop_id=1, hyper_id=101, shop_fee=100, shop_fee_ab=80)
        self.click_log.expect(clicktype=ClickType.PROMOTION, shop_id=1, hyper_id=101, cb_vnd=100, cp_vnd=80, cb=0, cp=0)

        self.click_log.expect(clicktype=ClickType.CPA, shop_id=1, hyper_id=102, shop_fee=400, shop_fee_ab=300)
        self.click_log.expect(clicktype=ClickType.PROMOTION, shop_id=1, hyper_id=102, cb_vnd=0, cp_vnd=0, cb=0, cp=0)

        self.click_log.expect(clicktype=ClickType.CPA, shop_id=1, hyper_id=103, shop_fee=0, shop_fee_ab=0)
        self.click_log.expect(clicktype=ClickType.PROMOTION, shop_id=1, hyper_id=103, cb_vnd=50, cp_vnd=17, cb=0, cp=0)

    def test_vendor_cpa_auction_vendor_bid_to_fee_coef(self):
        # проверяем что коэффициент конвертации вендорской ставки в fee корректно работает с демюксом и перепроведением автоброкера
        params = {
            "place": "prime",
            "blender": 1,
            "hid": 1011,
            'supported-incuts': get_supported_incuts_cgi(),
            "show-urls": "cpa,promotion",
            'pp': 7,
            "rids": 213,
            "client": "frontend",
            "platform": "desktop",
            "debug": 1,
        }
        rearr_flags = {
            "market_blender_cpa_shop_incut_enabled": 1,
            "market_blender_use_bundles_config": 1,
            'market_filter_offers_with_model_without_sku': 0,
            "market_blender_media_adv_incut_enabled": 0,
            "market_premium_ads_incut_get_docs_through_prime": 1,
            "market_premium_ads_gallery_shop_incut_logarithm_price": 0,
            "market_premium_ads_gallery_shop_incut_vendor_bid_to_fee_coef": 0.025,
            "market_cpa_shop_incut_premium_ads_use_demux": 1,
        }
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "items": [
                                {
                                    'shop': {'id': 1},
                                    'model': {'id': 101},
                                    'debug': {
                                        'properties': {'CPA_SHOP_INCUT': "3900000"},
                                        'sale': {'offerVendorFee': 1200},
                                    },
                                },
                                {
                                    'shop': {'id': 1},
                                    'model': {'id': 103},
                                    'debug': {
                                        'properties': {'CPA_SHOP_INCUT': "1800000"},
                                        'sale': {'offerVendorFee': 600},
                                    },
                                },
                                {
                                    'shop': {'id': 1},
                                    'model': {'id': 102},
                                    'debug': {
                                        'properties': {'CPA_SHOP_INCUT': "1200000"},
                                        'sale': {'offerVendorFee': 0},
                                    },
                                },
                            ],
                        },
                    ],
                },
            },
            preserve_order=True,
        )

        rearr_flags["market_premium_ads_gallery_shop_incut_vendor_bid_to_fee_coef"] = 0.05
        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "items": [
                                {
                                    'shop': {'id': 1},
                                    'model': {'id': 101},
                                    'debug': {
                                        'properties': {'CPA_SHOP_INCUT': "2100000"},
                                        'sale': {'offerVendorFee': 600},
                                    },
                                },
                                {
                                    'shop': {'id': 1},
                                    'model': {'id': 102},
                                    'debug': {
                                        'properties': {'CPA_SHOP_INCUT': "1200000"},
                                        'sale': {'offerVendorFee': 0},
                                    },
                                },
                                {
                                    'shop': {'id': 1},
                                    'model': {'id': 103},
                                    'debug': {
                                        'properties': {'CPA_SHOP_INCUT': "900000"},
                                        'sale': {'offerVendorFee': 300},
                                    },
                                },
                            ],
                        },
                    ],
                },
            },
            preserve_order=True,
        )


if __name__ == '__main__':
    main()
