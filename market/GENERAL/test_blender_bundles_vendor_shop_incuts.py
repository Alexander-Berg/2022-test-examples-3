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
from core.blender_bundles import get_supported_incuts_cgi, create_blender_bundles


class BlenderConstCpaShopIncut:
    BUNDLE = '''
{
    "incut_places": ["Search"],
    "incut_positions": [1,3,4,5,6,7,8],
    "incut_viewtypes": ["Gallery"],
    "incut_ids": ["default"],
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
            "row_position": 3,
            "incut_viewtype": "Gallery",
            "incut_id": "default",
            "score": 0.75
        },
        {
            "incut_place": "Search",
            "row_position": 4,
            "incut_viewtype": "Gallery",
            "incut_id": "default",
            "score": 0.75
        }
    ],
    "calculator_type": "ConstPosition"
}
'''


class BlenderTopVendorIncut:
    BUNDLE = '''
{
    "incut_places": ["Top"],
    "incut_positions": [1],
    "incut_viewtypes": ["GalleryWithBanner", "VendorGallery"],
    "incut_ids": ["vendor_incut_with_banner", "vendor_incut"],
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

        cls.index.vendors += [Vendor(vendor_id=x, name='vendor_{}'.format(x)) for x in range(1, 4)]

        titles = list(range(1, 22))
        cls.index.models += [
            Model(hid=66, hyperid=66 + i, title="Модель {}".format(titles[i]), ts=100020 + i, vendor_id=1, vbid=20)
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

        for i in range(1, 21):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100020 + i).respond(0.04)

    def test_cpa_and_vendor_incuts_touch_list(self):
        params = {
            'place': 'blender',
            'text': 'маркс',
            'hid': 66,
            'use-default-offers': 1,
            'allow-collapsing': 1,
            'additional_entities': 'articles',
            'touch': '1',
            'client': 'frontend',
            'platform': 'touch',
            'supported-incuts': get_supported_incuts_cgi(),
            'viewtype': 'list',
        }

        req_size = 8
        rearr_flags = {
            "market_blender_cpa_shop_incut_enabled": 1,
            'market_report_blender_vendor_incut_enable': 1,  # разрешение работы вендорской врезки
            'market_vendor_incut_size': req_size,
            'market_vendor_incut_with_CPA_offers_only': 0,
            'market_vendor_incut_hide_undeliverable_models': 0,
            'market_blender_media_adv_incut_enabled': 0,
        }

        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'incutId': 'vendor_incut',
                            'entity': 'searchIncut',
                            'title': 'Предложения vendor_{}'.format(1),
                            'inClid': 3,
                            'position': 1,
                        },
                        {
                            'entity': 'searchIncut',
                            'incutId': "default",
                            'title': 'Популярные предложения',
                            'inClid': 2,
                            'position': 4,
                        },
                    ]
                }
            },
            preserve_order=True,
        )

    def test_cpa_and_vendor_incuts_touch_grid(self):
        params = {
            'place': 'blender',
            'text': 'маркс',
            'hid': 66,
            'use-default-offers': 1,
            'allow-collapsing': 1,
            'additional_entities': 'articles',
            'touch': '1',
            'client': 'frontend',
            'platform': 'touch',
            'supported-incuts': get_supported_incuts_cgi(),
            'viewtype': 'grid',
            'columns-in-grid': 2,
        }

        req_size = 8
        rearr_flags = {
            "market_blender_cpa_shop_incut_enabled": 1,
            'market_report_blender_vendor_incut_enable': 1,  # разрешение работы вендорской врезки
            'market_vendor_incut_size': req_size,
            'market_vendor_incut_with_CPA_offers_only': 0,
            'market_vendor_incut_hide_undeliverable_models': 0,
            'market_blender_media_adv_incut_enabled': 0,
        }

        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'incutId': 'vendor_incut',
                            'entity': 'searchIncut',
                            'title': 'Предложения vendor_{}'.format(1),
                            'inClid': 3,
                            'position': 1,
                        },
                        {
                            'entity': 'searchIncut',
                            'incutId': "default",
                            'title': 'Популярные предложения',
                            'inClid': 2,
                            'position': 5,
                        },
                    ]
                }
            },
            preserve_order=True,
        )

    def test_cpa_and_vendor_incuts_desktop_list(self):
        params = {
            'place': 'blender',
            'text': 'маркс',
            'hid': 66,
            'use-default-offers': 1,
            'allow-collapsing': 1,
            'additional_entities': 'articles',
            'client': 'frontend',
            'platform': 'desktop',
            'supported-incuts': get_supported_incuts_cgi(),
            'viewtype': 'list',
        }

        req_size = 8
        rearr_flags = {
            'market_blender_cpa_shop_incut_enabled': 1,
            'market_report_blender_vendor_incut_enable': 1,  # разрешение работы вендорской врезки
            'market_vendor_incut_size': req_size,
            'market_vendor_incut_with_CPA_offers_only': 0,
            'market_vendor_incut_hide_undeliverable_models': 0,
            'market_blender_media_adv_incut_enabled': 0,
        }

        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'incutId': 'vendor_incut',
                            'entity': 'searchIncut',
                            'title': 'Предложения vendor_{}'.format(1),
                            'inClid': 3,
                            'position': 1,
                        },
                        {
                            'entity': 'searchIncut',
                            'incutId': "default",
                            'title': 'Популярные предложения',
                            'inClid': 2,
                            'position': 4,
                        },
                    ]
                }
            },
            preserve_order=True,
        )

    def test_cpa_and_vendor_incuts_desktop_grid(self):
        params = {
            'place': 'blender',
            'text': 'маркс',
            'hid': 66,
            'use-default-offers': 1,
            'allow-collapsing': 1,
            'additional_entities': 'articles',
            'client': 'frontend',
            'platform': 'desktop',
            'supported-incuts': get_supported_incuts_cgi(),
            'viewtype': 'grid',
            'columns-in-grid': 3,
        }

        req_size = 8
        rearr_flags = {
            'market_blender_cpa_shop_incut_enabled': 1,
            'market_report_blender_vendor_incut_enable': 1,  # разрешение работы вендорской врезки
            'market_vendor_incut_size': req_size,
            'market_vendor_incut_with_CPA_offers_only': 0,
            'market_vendor_incut_hide_undeliverable_models': 0,
            'market_blender_media_adv_incut_enabled': 0,
        }

        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'incutId': 'vendor_incut',
                            'entity': 'searchIncut',
                            'title': 'Предложения vendor_{}'.format(1),
                            'inClid': 3,
                            'position': 1,
                        },
                        {
                            'entity': 'searchIncut',
                            'incutId': "default",
                            'title': 'Популярные предложения',
                            'inClid': 2,
                            'position': 7,
                        },
                    ]
                }
            },
            preserve_order=True,
        )


if __name__ == '__main__':
    main()
