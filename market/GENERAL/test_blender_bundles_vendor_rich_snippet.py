#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types import (
    Model,
    Offer,
    Vendor,
    Shop,
    MnPlace,
)
from core.testcase import TestCase, main
from core.matcher import ElementCount
from core.blender_bundles import get_supported_incuts_cgi, create_blender_bundles


class BlenderConstCpaShopIncut:
    BUNDLE = '''
{
    "incut_places": ["Top", "Search"],
    "incut_positions": [1, 7],
    "incut_viewtypes": ["Gallery", "PremiumRichSnippet"],
    "incut_ids": ["default", "cpa_shop_incut_rich_snippet"],
    "result_scores": [
        {
            "incut_place": "Top",
            "row_position": 1,
            "incut_viewtype": "Gallery",
            "incut_id": "default",
            "score": 0.73
        },
        {
            "incut_place": "Search",
            "row_position": 7,
            "incut_viewtype": "Gallery",
            "incut_id": "default",
            "score": 0.75
        },
        {
            "incut_place": "Search",
            "row_position": 7,
            "incut_viewtype": "PremiumRichSnippet",
            "incut_id": "cpa_shop_incut_rich_snippet",
            "score": 0.74
        }
    ],
    "calculator_type": "ConstPosition"
}
'''


class BlenderTopVendorIncut:
    BUNDLE = '''
{
    "incut_places": ["Top", "Search"],
    "incut_positions": [1, 4],
    "incut_viewtypes": ["GalleryWithBanner", "VendorGallery", "PremiumRichSnippet"],
    "incut_ids": ["vendor_incut_with_banner", "vendor_incut", "vendor_incut_rich_snippet"],
    "result_scores": [
        {
            "incut_place": "Top",
            "row_position": 1,
            "incut_viewtype": "GalleryWithBanner",
            "incut_id": "vendor_incut_with_banner",
            "score": 0.75
        },
        {
            "incut_place": "Top",
            "row_position": 1,
            "incut_viewtype": "VendorGallery",
            "incut_id": "vendor_incut",
            "score": 0.74
        },
        {
            "incut_place": "Search",
            "row_position": 4,
            "incut_viewtype": "PremiumRichSnippet",
            "incut_id": "vendor_incut_rich_snippet",
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
        "client == frontend && platform == desktop" : {
            "bundle_name": "const_premium_ads.json"
        },
        "client == frontend && platform == touch" : {
            "bundle_name": "const_premium_ads.json"
        }
    },
    "INCLID_VENDOR_INCUT" : {
        "client == frontend && platform == desktop" : {
            "bundle_name": "const_vendor_incut.json"
        },
        "client == frontend && platform == touch" : {
            "bundle_name": "const_vendor_incut.json"
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

        return "{}&rearr-factors={}".format(dict_to_str(params, '&'), dict_to_str(rearr, ';'))

    @classmethod
    def prepare_blender_bundles_config(cls):
        cls.settings.formulas_path = create_blender_bundles(
            cls.meta_paths.testroot,
            BlenderBundlesConfig.BUNDLES_CONFIG,
            {
                "const_vendor_incut.json": BlenderTopVendorIncut.BUNDLE,
                "const_premium_ads.json": BlenderConstCpaShopIncut.BUNDLE,
            },
        )

    @classmethod
    def prepare_cpa_and_vendor_incuts(cls):

        cls.index.vendors += [Vendor(vendor_id=x, name='vendor_{}'.format(x)) for x in range(1, 5)]

        for x in range(1, 5):
            for i in range(1, 21):
                cls.index.models += [
                    Model(
                        hid=66,
                        hyperid=66 + i + 100 * x,
                        title="Модель {}".format(i + 100 * x),
                        ts=100020 + i + 100 * x,
                        vendor_id=x,
                        vbid=20 + x * 5,
                    ),
                ]

        for x in range(1, 5):
            for i in range(1, 3):
                cls.index.models += [
                    Model(
                        hid=67,
                        hyperid=1067 + i + 100 * x,
                        title="Модель {}".format(i + 100 * x),
                        ts=200020 + i + 100 * x,
                        vendor_id=x,
                        vbid=20 + x * 5,
                    ),
                ]

        for x in range(1, 5):
            for i in range(1, 21):
                cls.index.shops += [
                    Shop(
                        fesh=66 + i + 100 * x,
                        priority_region=213,
                        shop_fee=100 + 10 * x,
                        cpa=Shop.CPA_REAL,
                        name='CPA Shop {}'.format(i + 100 * x),
                    ),
                ]

        for x in range(1, 5):
            for i in range(1, 21):
                cls.index.offers += [
                    Offer(
                        fesh=66 + i + 100 * x,
                        hyperid=66 + i + 100 * x,
                        hid=66,
                        fee=90 + i + 10 * x,
                        ts=100020 + i + 100 * x,
                        price=100,
                        cpa=Offer.CPA_REAL,
                        title="Маркс {}".format(i + 100 * x),
                    ),
                ]

        for x in range(1, 5):
            for i in range(1, 3):
                cls.index.offers += [
                    Offer(
                        fesh=66 + i + 100 * x,
                        hyperid=1067 + i + 100 * x,
                        hid=67,
                        fee=90 + i + 10 * x,
                        ts=200020 + i + 100 * x,
                        price=100,
                        cpa=Offer.CPA_REAL,
                        title="Marks {}".format(i + 100 * x),
                    ),
                ]

        for x in range(1, 5):
            for i in range(1, 21):
                cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100020 + i + 100 * x).respond(0.0001 * (x + i))
                cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 200020 + i + 100 * x).respond(0.0002 * (x + i))

    # Проверяем что показываются и vendor_incut и богатый снипет при наличии 2-х вендоров
    def test_vendor_incuts_and_rich_snippet(self):
        params = {
            "place": "blender",
            "text": "маркс",
            'hid': 66,
            'use-default-offers': 1,
            'allow-collapsing': 1,
            "additional_entities": "articles",
            "client": "frontend",
            "platform": "desktop",
            'supported-incuts': get_supported_incuts_cgi(),
            'viewtype': "list",
        }

        req_size = 8
        rearr_flags = {
            "market_blender_cpa_shop_incut_enabled": 1,
            'market_report_blender_vendor_incut_enable': 1,  # разрешение работы вендорской врезки
            'market_vendor_incut_size': req_size,
            'market_vendor_incut_with_CPA_offers_only': 0,
            'market_vendor_incut_hide_undeliverable_models': 0,
            'market_blender_use_bundles_config': 1,
            'market_premium_ads_gallery_min_num_doc_to_request_from_base': 1,
            'market_vendor_rich_snippet_enable': 1,
            'market_blender_media_adv_incut_enabled': 0,
            'market_report_madv_search_purchase_probability': 1,
        }

        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'typeId': 2,
                            'incutId': 'vendor_incut',
                            'entity': 'searchIncut',
                            'title': 'Предложения vendor_4',
                            'inClid': 3,
                            'position': 1,
                            'brand_id': 4,
                            "items": ElementCount(8),
                        },
                        {
                            'typeId': 5,
                            'incutId': 'vendor_incut_rich_snippet',
                            'entity': 'searchIncut',
                            'title': 'Предложения vendor_3',
                            'inClid': 3,
                            'position': 4,
                            'brand_id': 3,
                            "items": ElementCount(1),
                        },
                        {
                            'typeId': 1,
                            'incutId': 'default',
                            'entity': 'searchIncut',
                            'title': 'Популярные предложения',
                            'inClid': 2,
                            'position': 8,
                            "items": ElementCount(10),
                        },
                    ]
                }
            },
            preserve_order=True,
        )

    # Проверяем что показываются только богатый снипет если моделей для врезки не набирается
    def test_rich_snippet_only(self):
        params = {
            "place": "blender",
            "text": "Marks",
            'hid': 67,
            'use-default-offers': 1,
            'allow-collapsing': 1,
            "additional_entities": "articles",
            "client": "frontend",
            "platform": "desktop",
            'supported-incuts': get_supported_incuts_cgi(),
            'viewtype': "list",
        }

        req_size = 8
        rearr_flags = {
            "market_blender_cpa_shop_incut_enabled": 1,
            'market_report_blender_vendor_incut_enable': 1,  # разрешение работы вендорской врезки
            'market_vendor_incut_size': req_size,
            'market_vendor_incut_with_CPA_offers_only': 0,
            'market_vendor_incut_hide_undeliverable_models': 0,
            'market_vendor_rich_snippet_enable': 1,
            'market_blender_media_adv_incut_enabled': 0,
        }

        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'typeId': 1,
                            'incutId': 'default',
                            'entity': 'searchIncut',
                            'title': 'Популярные предложения',
                            'inClid': 2,
                            'position': 1,
                            "items": ElementCount(8),
                        },
                        {
                            'typeId': 5,
                            'incutId': 'vendor_incut_rich_snippet',
                            'entity': 'searchIncut',
                            'title': 'Предложения vendor_4',
                            'inClid': 3,
                            'position': 4,
                            'brand_id': 4,
                            "items": ElementCount(1),
                        },
                    ]
                }
            },
            preserve_order=True,
        )

    # Проверяем что в ответе возвращаются вероятности заказа в офферах вендорской врезки
    def test_offer_probabilities_in_vendor_incuts(self):
        params = {
            "place": "blender",
            "text": "маркс",
            'hid': 66,
            'use-default-offers': 1,
            'allow-collapsing': 1,
            "additional_entities": "articles",
            "client": "frontend",
            "platform": "desktop",
            'supported-incuts': get_supported_incuts_cgi(),
            'viewtype': "list",
        }

        req_size = 8
        rearr_flags = {
            "market_blender_cpa_shop_incut_enabled": 1,
            'market_report_blender_vendor_incut_enable': 1,  # разрешение работы вендорской врезки
            'market_vendor_incut_size': req_size,
            'market_vendor_incut_with_CPA_offers_only': 0,
            'market_vendor_incut_hide_undeliverable_models': 0,
            'market_blender_use_bundles_config': 1,
            'market_premium_ads_gallery_min_num_doc_to_request_from_base': 1,
            'market_vendor_rich_snippet_enable': 1,
            'market_blender_media_adv_incut_enabled': 0,
            'market_report_madv_search_purchase_probability': 1,
        }

        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                'items': [
                    {
                        'model': {'id': 486},
                        "purchaseProbability": 0.0024,
                    },
                    {
                        'model': {'id': 485},
                        "purchaseProbability": 0.0023,
                    },
                    {
                        'model': {'id': 484},
                        "purchaseProbability": 0.0022,
                    },
                    {
                        'model': {'id': 483},
                        "purchaseProbability": 0.0021,
                    },
                ]
            },
            preserve_order=False,
        )


if __name__ == '__main__':
    main()
