#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    GradeDispersionItem,
    HyperCategory,
    HyperCategoryType,
    Model,
    Offer,
    ReviewDataItem,
    Vendor,
    VendorBanner,
)
from core.testcase import TestCase, main
from core.matcher import ElementCount, EmptyList
from core.blender_bundles import get_supported_incuts_cgi, create_blender_bundles
from collections import OrderedDict
import json


class BlenderBundles:
    BUNDLE_RANDOM_POSITION_VENDOR_INCUT = '''
{{
    "incut_places": ["Top", "Search"],
    "incut_viewtypes": ["GalleryWithBanner", "VendorGallery"],
    "incut_positions": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10],
    "incut_ids": ["vendor_incut_with_banner", "vendor_incut"],
    "incut_place_viewtype_combinations": {{
        "Top": [
            "GalleryWithBanner", "VendorGallery"
        ],
        "Search": [
            "GalleryWithBanner", "VendorGallery"
        ]
    }},
    "pos_weights": {{
        "Top": {{
            "1": 1
        }},
        "Search": {{
            "1": 1,
            "2": 1,
            "3": 1,
            "4": 1,
            "5": 1,
            "6": 2,
            "7": 2,
            "8": 2,
            "9": 2,
            "10": 2
        }}
    }},

    "min_score": 0.2,
    "max_score": 0.8,
    "noshow_prob": {noshow_prob},
    "available_combinations": [
        {{
            "places": ["Top", "Search"],
            "viewtype": "GalleryWithBanner",
            "incut_id": "vendor_incut_with_banner"
        }},
        {{
            "places": ["Top", "Search"],
            "viewtype": "VendorGallery",
            "incut_id": "vendor_incut_with_banner"
        }}
    ],
    "available_position_count_after_selected": 5,
    "calculator_type": "RandomCombination"
}}
'''


class BlenderBundlesConfig:
    BUNDLES_CONFIG = """
{
    "INCLID_VENDOR_INCUT" : {
        "client == frontend && platform == desktop" : {
            "bundle_name": "random_vendor_incut.json"
        },
        "client == frontend && platform == touch" : {
            "bundle_name": "random_vendor_incut.json"
        }
    }
}
"""


class T(TestCase):
    class CgiParams(dict):
        def raw(self, separator='&'):
            if len(self):
                return separator.join("{}={}".format(str(k), str(v)) for (k, v) in self.iteritems())
            return ""

    class RearrFlags(CgiParams):
        def __init__(self, *args, **kwargs):
            super(T.RearrFlags, self).__init__(*args, **kwargs)

        def raw(self):
            if len(self):
                return 'rearr-factors={}'.format(super(T.RearrFlags, self).raw(';'))
            return str()

    @staticmethod
    def create_request(parameters, rearr):
        return '{}{}'.format(parameters.raw(), '&{}'.format(rearr.raw()) if len(rearr) else '')

    @classmethod
    def prepare(cls):

        cls.index.hypertree += [HyperCategory(hid=100, output_type=HyperCategoryType.GURU)]

        cls.index.models += [
            Model(hid=100, hyperid=1, title="Первая модель", ts=1),
        ]

        cls.index.model_reviews_data += [
            ReviewDataItem(
                review_id=101,
                model_id=1,
                short_text="Nice",
                most_useful=1,
            )
        ]
        cls.index.model_grade_dispersion_data += [
            GradeDispersionItem(model_id=1, five=1),
        ]

    @classmethod
    def prepare_blender_bundles_config(cls):
        cls.settings.formulas_path = create_blender_bundles(
            cls.meta_paths.testroot,
            BlenderBundlesConfig.BUNDLES_CONFIG,
            {
                "random_vendor_incut.json": BlenderBundles.BUNDLE_RANDOM_POSITION_VENDOR_INCUT.format(noshow_prob=0.1),
                "random_vendor_incut_hi_no_show_prob.json": BlenderBundles.BUNDLE_RANDOM_POSITION_VENDOR_INCUT.format(
                    noshow_prob=0.9
                ),
            },
        )

    @classmethod
    def prepare_vendor_incut(cls):
        cls.index.vendors += [Vendor(vendor_id=x, name='vendor_{}'.format(x)) for x in range(1, 4)]

        start_hyperid_1 = 1300
        cls.index.models += [
            Model(
                hid=551,
                hyperid=start_hyperid_1 + x,
                vendor_id=1,
                vbid=10,
                title="toy {}".format(start_hyperid_1 + x),
            )
            for x in range(0, 10)
        ]
        cls.index.offers += [
            Offer(
                hyperid=start_hyperid_1 + x,
                price=1000 * x,
                cpa=Offer.CPA_REAL,
                hid=551,
                title="offer for {}".format(start_hyperid_1 + x),
            )
            for x in range(0, 10)
        ]

        start_hyperid_2 = start_hyperid_1 + 10
        cls.index.models += [
            Model(
                hid=551,
                hyperid=start_hyperid_2 + x,
                vendor_id=2,
                vbid=20,  # more then vendor 1
                title="toy {}".format(start_hyperid_2 + x),
            )
            for x in range(0, 10)
        ]
        cls.index.offers += [
            Offer(
                hyperid=start_hyperid_2 + x,
                price=1000 * x,
                cpa=Offer.CPA_NO,  # should not be a CPA
                hid=551,
                title="offer for {}".format(start_hyperid_2 + x),
            )
            for x in range(0, 10)
        ]

        # вендор с банером
        start_hyperid_3 = start_hyperid_2 + 10
        cls.index.models += [
            Model(
                hid=552,  # другой, относительно предыдущих
                hyperid=start_hyperid_3 + x,
                vendor_id=3,
                vbid=20,  # more then vendor 1
                title="toy {}".format(start_hyperid_3 + x),
                datasource_id=1,
            )
            for x in range(0, 10)
        ]
        cls.index.offers += [
            Offer(
                hyperid=start_hyperid_3 + x,
                price=1000 * x,
                cpa=Offer.CPA_REAL,
                hid=552,
                title="offer for {}".format(start_hyperid_3 + x),
            )
            for x in range(0, 10)
        ]
        cls.index.vendors_banners += [
            VendorBanner(
                datasource_id=1,
                vendor_id=3,
                hyper_id=552,
                vendor_banner_id=71,
                bbid=800,
            )
        ]

    def test_random_bundle_one_incut(self):
        params = self.CgiParams(
            {
                'yandexuid': '4389100',
                'place': 'blender',
                'text': 'toy',
                'use-default-offers': 1,
                'debug': 'da',
                'allow-collapsing': 1,
                'pp': 18,
                'show-urls': 'productVendorBid',
                'hid': 552,  # вендор с баннером
                'client': 'frontend',
                'platform': 'desktop',
                'supported-incuts': get_supported_incuts_cgi(),
            }
        )
        req_size = 8
        rearr_factors = self.RearrFlags(
            {
                'market_report_blender_vendor_incut_enable': 1,  # разрешение работы вендорской врезки
                'market_vendor_incut_size': req_size,
                'market_vendor_incut_with_CPA_offers_only': 1,
                'market_vendor_incut_hide_undeliverable_models': 0,
                'market_vendor_incut_enable_banners': 1,  # enable to show vendor
                'market_blender_use_bundles_config': 1,
                'market_blender_media_adv_incut_enabled': 0,
            }
        )

        # проверим, что по умолчанию происходит показ врезки
        rearr_factors['market_report_blender_vendor_incut_banner_render_enable'] = 0
        response = self.report.request_json(
            self.create_request(params, rearr_factors)
        )  # запрос категории где только один вендор с банером
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'incutId': 'vendor_incut_with_banner',
                            "entity": "searchIncut",
                            "items": ElementCount(req_size + 1),  # на одну позицию больше, т.к. есть баннер
                            "position": 1,
                        },
                    ],
                },
            },
            preserve_order=True,
        )
        # проверим, что при изменении yandexuid позиция изменится
        params['yandexuid'] = '4389101'
        response = self.report.request_json(
            self.create_request(params, rearr_factors)
        )  # запрос категории где только один вендор с банером
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'incutId': 'vendor_incut_with_banner',
                            "entity": "searchIncut",
                            "items": ElementCount(req_size + 1),  # на одну позицию больше, т.к. есть баннер
                            "position": 10,
                        },
                    ],
                },
            },
            preserve_order=True,
        )

    def test_no_show_prob(self):
        params = self.CgiParams(
            {
                'yandexuid': '4389103',
                'place': 'blender',
                'text': 'toy',
                'use-default-offers': 1,
                'debug': 'da',
                'allow-collapsing': 1,
                'pp': 18,
                'show-urls': 'productVendorBid',
                'hid': 552,  # вендор с баннером
                'client': 'frontend',
                'platform': 'desktop',
                'supported-incuts': get_supported_incuts_cgi(),
            }
        )
        req_size = 8
        rearr_factors = self.RearrFlags(
            {
                'market_report_blender_vendor_incut_enable': 1,  # разрешение работы вендорской врезки
                'market_vendor_incut_size': req_size,
                'market_vendor_incut_with_CPA_offers_only': 1,
                'market_vendor_incut_hide_undeliverable_models': 0,
                'market_vendor_incut_enable_banners': 1,  # enable to show vendor
                'market_blender_use_bundles_config': 1,
                'market_blender_media_adv_incut_enabled': 0,
            }
        )
        rearr_factors['market_report_blender_vendor_incut_banner_render_enable'] = 0

        # проверим, что при использовании бандла с высоким noshow_prob врезка не покажется
        rearr_factors['market_blender_bundles_for_inclid'] = '3:random_vendor_incut_hi_no_show_prob.json'
        response = self.report.request_json(
            self.create_request(params, rearr_factors)
        )  # запрос категории где только один вендор с банером
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": EmptyList(),
                },
            },
            preserve_order=True,
        )

        # с низким noshow_prob врезка покажется
        rearr_factors['market_blender_bundles_for_inclid'] = '3:random_vendor_incut.json'
        response = self.report.request_json(
            self.create_request(params, rearr_factors)
        )  # запрос категории где только один вендор с банером
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": ElementCount(1),
                },
            },
            preserve_order=True,
        )

    def test_available_combinations(self):
        params = self.CgiParams(
            {
                'yandexuid': '4389100',
                'place': 'blender',
                'text': 'toy',
                'use-default-offers': 1,
                'debug': 'da',
                'allow-collapsing': 1,
                'pp': 18,
                'show-urls': 'productVendorBid',
                'hid': 552,  # вендор с баннером
                'client': 'frontend',
                'platform': 'desktop',
                'supported-incuts': get_supported_incuts_cgi(),
            }
        )
        req_size = 8
        rearr_factors = self.RearrFlags(
            {
                'market_report_blender_vendor_incut_enable': 1,  # разрешение работы вендорской врезки
                'market_vendor_incut_size': req_size,
                'market_vendor_incut_with_CPA_offers_only': 1,
                'market_vendor_incut_hide_undeliverable_models': 0,
                'market_vendor_incut_enable_banners': 1,  # enable to show vendor
                'market_blender_use_bundles_config': 1,
                'market_blender_media_adv_incut_enabled': 0,
            }
        )
        rearr_factors['market_report_blender_vendor_incut_banner_render_enable'] = 0

        # сравним viewtype у запросов с разными yandexuid, чтобы понять, какая комбинация выбрана
        response = self.report.request_json(
            self.create_request(params, rearr_factors)
        )  # запрос категории где только один вендор с банером
        self.assertFragmentIn(
            response,
            {
                "incuts": {"results": [{"typeId": 2}]},
            },
            preserve_order=True,
        )

        # проверим, что при смене yandexuid меняется комбинация
        params['yandexuid'] = '4389103'
        response = self.report.request_json(
            self.create_request(params, rearr_factors)
        )  # запрос категории где только один вендор с банером
        self.assertFragmentIn(
            response,
            {
                "incuts": {"results": [{"typeId": 3}]},
            },
            preserve_order=True,
        )

    def test_random_bundle_supported_incuts_patch(self):
        """
        проверяем, что если фронт поддерживает только пересечение комбинаций, то врезка все равно покажется
        """
        params = self.CgiParams(
            {
                'yandexuid': '4389100',
                'place': 'blender',
                'text': 'toy',
                'use-default-offers': 1,
                'debug': 'da',
                'allow-collapsing': 1,
                'pp': 18,
                'show-urls': 'productVendorBid',
                'hid': 552,  # вендор с баннером
                'client': 'frontend',
                'platform': 'desktop',
                'supported-incuts': get_supported_incuts_cgi({"1": [2, 3]}),
            }
        )
        req_size = 8
        rearr_factors = self.RearrFlags(
            {
                'market_report_blender_vendor_incut_enable': 1,  # разрешение работы вендорской врезки
                'market_vendor_incut_size': req_size,
                'market_vendor_incut_with_CPA_offers_only': 1,
                'market_vendor_incut_hide_undeliverable_models': 0,
                'market_vendor_incut_enable_banners': 1,  # enable to show vendor
                'market_blender_use_bundles_config': 1,
                'market_blender_use_supported_incuts': 1,
                'market_blender_media_adv_incut_enabled': 0,
            }
        )

        response = self.report.request_json(
            self.create_request(params, rearr_factors)
        )  # запрос категории где только один вендор с банером
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'incutId': 'vendor_incut_with_banner',
                            "entity": "searchIncut",
                        },
                    ],
                },
            },
            preserve_order=True,
        )

    def test_random_bundle_calculator_access_log(self):
        params = self.CgiParams(
            {
                'yandexuid': '4389100',
                'place': 'blender',
                'text': 'toy',
                'use-default-offers': 1,
                'debug': 'da',
                'allow-collapsing': 1,
                'pp': 18,
                'show-urls': 'productVendorBid',
                'hid': 552,  # вендор с баннером
                'client': 'frontend',
                'platform': 'desktop',
                'supported-incuts': get_supported_incuts_cgi(),
            }
        )
        req_size = 8
        rearr_factors = self.RearrFlags(
            {
                'market_report_blender_vendor_incut_enable': 1,  # разрешение работы вендорской врезки
                'market_vendor_incut_size': req_size,
                'market_vendor_incut_with_CPA_offers_only': 1,
                'market_vendor_incut_hide_undeliverable_models': 0,
                'market_vendor_incut_enable_banners': 1,  # enable to show vendor
                'market_blender_use_bundles_config': 1,
                'market_blender_write_access_log': 1,
                'market_blender_write_calculators_access_log': 1,
                "market_blender_media_adv_incut_enabled": 0,
            }
        )

        # проверим, что по умолчанию происходит показ врезки
        rearr_factors['market_report_blender_vendor_incut_banner_render_enable'] = 0
        response = self.report.request_json(
            self.create_request(params, rearr_factors)
        )  # запрос категории где только один вендор с банером
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'incutId': 'vendor_incut_with_banner',
                            "entity": "searchIncut",
                            "items": ElementCount(req_size + 1),  # на одну позицию больше, т.к. есть баннер
                            "position": 1,
                        },
                    ],
                },
            },
            preserve_order=True,
        )

        vendor_incuts_calculated = OrderedDict(
            [
                (
                    "3",
                    OrderedDict(
                        [
                            (
                                "available_combinations",
                                [
                                    OrderedDict(
                                        [
                                            ("places", [1, 2]),
                                            ("incut_viewtype", 3),
                                            ("incut_id", "vendor_incut_with_banner"),
                                        ]
                                    ),
                                    OrderedDict(
                                        [
                                            ("places", [1, 2]),
                                            ("incut_viewtype", 2),
                                            ("incut_id", "vendor_incut_with_banner"),
                                        ]
                                    ),
                                ],
                            ),
                            (
                                "pos_weights",
                                OrderedDict(
                                    [
                                        (
                                            1,
                                            OrderedDict(
                                                [
                                                    ("1", 1),
                                                    ("2", 1),
                                                    ("3", 1),
                                                    ("4", 1),
                                                    ("5", 1),
                                                    ("6", 2),
                                                    ("7", 2),
                                                    ("8", 2),
                                                    ("9", 2),
                                                    ("10", 2),
                                                ]
                                            ),
                                        ),
                                        (2, OrderedDict([("1", 1)])),
                                    ]
                                ),
                            ),
                            ("noshow_prob", 0.1),
                            (
                                "result_scores",
                                [
                                    OrderedDict(
                                        [
                                            ("incut_id", "vendor_incut_with_banner"),
                                            ("incut_place", 2),
                                            ("incut_viewtype", 2),
                                            ("position", 1),
                                            ("score", 0.4049474973),
                                        ]
                                    ),
                                    OrderedDict(
                                        [
                                            ("incut_id", "vendor_incut_with_banner"),
                                            ("incut_place", 1),
                                            ("incut_viewtype", 2),
                                            ("position", 1),
                                            ("score", 0.4358802171),
                                        ]
                                    ),
                                    OrderedDict(
                                        [
                                            ("incut_id", "vendor_incut_with_banner"),
                                            ("incut_place", 1),
                                            ("incut_viewtype", 2),
                                            ("position", 2),
                                            ("score", 0.7969471447),
                                        ]
                                    ),
                                    OrderedDict(
                                        [
                                            ("incut_id", "vendor_incut_with_banner"),
                                            ("incut_place", 1),
                                            ("incut_viewtype", 2),
                                            ("position", 3),
                                            ("score", 0.3366073745),
                                        ]
                                    ),
                                    OrderedDict(
                                        [
                                            ("incut_id", "vendor_incut_with_banner"),
                                            ("incut_place", 1),
                                            ("incut_viewtype", 2),
                                            ("position", 4),
                                            ("score", 0.5019267047),
                                        ]
                                    ),
                                    OrderedDict(
                                        [
                                            ("incut_id", "vendor_incut_with_banner"),
                                            ("incut_place", 1),
                                            ("incut_viewtype", 2),
                                            ("position", 5),
                                            ("score", 0.7668816773),
                                        ]
                                    ),
                                ],
                            ),
                        ]
                    ),
                ),
            ]
        )

        self.access_log.expect(
            blender_incuts_calculated=json.dumps(vendor_incuts_calculated, separators=(',', ':'))
        ).times(1)


if __name__ == '__main__':
    main()
