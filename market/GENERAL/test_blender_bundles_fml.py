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
    MnPlace,
)
from core.testcase import TestCase, main
from core.matcher import ElementCount
from core.blender_bundles import get_supported_incuts_cgi, create_blender_bundles


class BlenderBundles:
    BUNDLE_RANDOM_POSITION_VENDOR_INCUT = '''
{{
    "incut_places": ["Top", "Search"],
    "incut_viewtypes": ["GalleryWithBanner", "VendorGallery"],
    "incut_positions": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10],
    "incut_ids": ["vendor_incut_with_banner", "vendor_incut"],
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
    "organic_formula": "MOCK_FORMULA",
    "incut_formula": "MOCK_FORMULA",
    "max_position_for_formula": 10,
    "incut_target_coeff": 0.01,
    "noshow_intersept": -0.3,
    "calculator_type": "FMLLinearCombination"
}}
'''


class BlenderBundlesConfig:
    BUNDLES_CONFIG = """
{
    "INCLID_VENDOR_INCUT" : {
        "client == frontend && platform == desktop" : {
            "bundle_name": "fml_vendor_incut.json"
        },
        "client == frontend && platform == touch" : {
            "bundle_name": "fml_vendor_incut.json"
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
    def prepare_blender_bundles_and_formulas(cls):
        cls.settings.formulas_path = create_blender_bundles(
            cls.meta_paths.testroot,
            BlenderBundlesConfig.BUNDLES_CONFIG,
            {
                "fml_vendor_incut.json": BlenderBundles.BUNDLE_RANDOM_POSITION_VENDOR_INCUT.format(
                    incut_formula="high_incut_formula.json", organic_formula="high_organic_formula.json"
                ),
            },
        )
        cls.matrixnet.on_place(MnPlace.BLENDER_INCUT_FML, "vendor_incut_with_banner").respond(1.0)
        cls.matrixnet.on_place(MnPlace.BLENDER_ORGANIC_FML, "vendor_incut_with_banner").respond(0.05)

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

    def test_fml_bundle_one_incut(self):
        params = self.CgiParams(
            {
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
                'market_blender_bundles_row_position_format': 1,
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

    def test_random_bundle_supported_incuts_patch(self):
        """
        проверяем, что если фронт поддерживает только пересечение комбинаций, то врезка все равно покажется
        """
        params = self.CgiParams(
            {
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
                'market_blender_use_supported_incuts': 1,
                'market_blender_bundles_row_position_format': 1,
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


if __name__ == '__main__':
    main()
