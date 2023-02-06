#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

import os

from core.types import (
    Model,
    Offer,
)
from core.cms_incut_storage import CmsIncutStorage

from core.testcase import TestCase, main
from core.blender_bundles import create_blender_bundles, get_supported_incuts_cgi


class BlenderBundleCatalog:
    BUNDLE = '''
{
    "incut_places": ["Top", "Search"],
    "incut_positions": [1,8],
    "incut_viewtypes": ["CatalogQuiz"],
    "incut_ids": ["catalog_quiz"],
    "result_scores": [
        {
            "incut_place": "Top",
            "row_position": 1,
            "incut_viewtype": "CatalogQuiz",
            "incut_id": "catalog_quiz",
            "score": 1.0
        },
        {
            "incut_place": "Search",
            "row_position": 8,
            "incut_viewtype": "CatalogQuiz",
            "incut_id": "catalog_quiz",
            "score": 1.0
        }
    ],
    "calculator_type": "ConstPosition"
}
'''


class BlenderBundleConstPositon:
    BUNDLE_CONST_SEARCH_POSITION = '''
{{
    "incut_places": ["Search"],
    "incut_positions": [{row_position}],
    "incut_viewtypes": ["Gallery"],
    "incut_ids": ["{incut_id}"],
    "result_scores": [
        {{
            "incut_place": "Search",
            "row_position": {row_position},
            "incut_viewtype": "Gallery",
            "incut_id": "{incut_id}",
            "score": 1.0
        }}
    ],
    "calculator_type": "ConstPosition"
}}
'''


def get_cms_incuts_mock():
    cms_incuts_data = {
        "growing_cashback_incut": [
            {
                "page_id": 1,
                "name": "cashback_desktop",
                "content": {
                    "minOrderTotal": 3500,
                    "maxCashbackTotal": 1550,
                    "ordersCount": 3,
                    "distributionLinkDesktop": "linkDesktop",
                },
                "device": "desktop",
                "nids": [],
                "hids": [],
            },
            {
                "page_id": 2,
                "name": "cashback_touch",
                "content": {
                    "minOrderTotal": 3500,
                    "maxCashbackTotal": 1550,
                    "ordersCount": 3,
                    "distributionLinkTouch": "linkTouch",
                },
                "device": "phone",
                "nids": [],
                "hids": [],
            },
        ],
        "catalog_quiz": [
            {
                "page_id": 3,
                "name": "catalog",
                "content": {"start": "1", "end": "2"},
                "device": "desktop",
                "nids": [10023, 10024],
                "hids": [10044, 324, 9999],
            },
        ],
    }
    return cms_incuts_data


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.market_access_settings.enabled = True
        cls.settings.market_access_settings.download_cms_incuts_from_access = True

    @classmethod
    def setup_market_access_resources(cls, access_server, shade_host_port):
        access_server.create_publisher(name='mbi')
        access_server.create_resource(name='cms_blender_incuts', publisher_name='mbi')

        dst_path = os.path.join(cls.meta_paths.access_resources, 'cms_blender_incuts/1.0.0')

        if not os.path.exists(dst_path):
            os.makedirs(dst_path)
        data_path = os.path.join(dst_path, 'cms_blender_incuts.pbsn')
        v1_url = cls._get_mds_url(shade_host_port, data_path)

        incuts_storage = CmsIncutStorage(get_cms_incuts_mock())
        incuts_storage.save(data_path)

        access_server.create_version('cms_blender_incuts', http_url=v1_url)

    @staticmethod
    def _get_mds_url(shade_host_port, path):
        path = path if path.startswith('/') else '/' + path
        return '{host_port}/mds{path}'.format(
            host_port=shade_host_port,
            path=path,
        )

    @classmethod
    def prepare_blender_bundles_config(cls):
        cls.settings.formulas_path = create_blender_bundles(
            cls.meta_paths.testroot,
            None,
            {
                "bundle_catalog_quiz.json": BlenderBundleCatalog.BUNDLE,
                "growing_cashback.json": BlenderBundleConstPositon.BUNDLE_CONST_SEARCH_POSITION.format(
                    row_position=4, incut_id="CMS_GROWING_CASHBACK"
                ),
            },
        )

    @classmethod
    def prepare_organic_documents(cls):
        cls.index.models += [
            Model(
                title="Quiz {}".format(str(i)),
                hid=9999,
                hyperid=100 + i,
            )
            for i in range(1, 10)
        ]

        cls.index.offers += [
            Offer(
                hyperid=100 + i,
                ts=1000 + i,
                price=100,
                cpa=Offer.CPA_REAL,
                title="Quiz {}".format(str(i)),
            )
            for i in range(1, 10)
        ]

    def test_blender_catalog_quiz(self):
        request = "place=blender&hid=9999&rearr-factors=market_blender_bundles_for_inclid=20:bundle_catalog_quiz.json;"
        request += "market_blender_media_adv_incut_enabled=0"
        request += "&supported-incuts=" + get_supported_incuts_cgi({1: [14]})

        request = "place=blender&hid=9999"
        request += "&rearr-factors=market_blender_bundles_for_inclid=20:bundle_catalog_quiz.json;"
        request += "market_blender_media_adv_incut_enabled=0;"
        request += "&supported-incuts=" + get_supported_incuts_cgi({1: [14]})

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "entity": "searchIncut",
                            "position": 8,
                            "inClid": 20,
                            "incutId": "catalog_quiz",
                            "items": [
                                {
                                    "name": "catalog",
                                    "pageId": 3,
                                    "content": {"start": "1", "end": "2"},
                                }
                            ],
                        }
                    ]
                },
            },
            allow_different_len=False,
        )

    def test_blender_growing_cashback_banner(self):
        request_base = "place=blender&debug=da&hid=9999&supported-incuts=" + get_supported_incuts_cgi()
        request_base += "&rearr-factors=market_blender_media_adv_incut_enabled=0;market_blender_bundles_for_inclid=18:growing_cashback.json"
        rearr_enabled = "&rearr-factors=growing_cashback_incut_enabled=1"
        rearr_disabled = "&rearr-factors=growing_cashback_incut_enabled=0"

        test_data = [
            ("&client=IOS&perks=growing_cashback" + rearr_enabled, False, False),
            ("&client=ANDROID&perks=growing_cashback" + rearr_enabled, False, False),
            ("&client=frontend&platform=desktop&perks=not_growing_cashback" + rearr_enabled, False, False),
            ("&client=frontend&platform=desktop&perks=growing_cashback" + rearr_enabled, True, False),
            ("&client=frontend&platform=touch&perks=growing_cashback" + rearr_disabled, False, True),
            ("&client=frontend&platform=touch&perks=growing_cashback" + rearr_enabled, True, True),
        ]

        for client_and_perks, show_banner, touch_client in test_data:
            response = self.report.request_json(request_base + client_and_perks)
            if show_banner:
                self.assertFragmentIn(
                    response,
                    {
                        "incuts": {
                            "results": [
                                {
                                    "entity": "searchIncut",
                                    "position": 4,
                                    "inClid": 18,
                                    "incutId": "CMS_GROWING_CASHBACK",
                                    "maxCashbackTotal": 1550,
                                    "minOrderTotal": 3500,
                                    "ordersCount": 3,
                                    "distributionLink": "linkTouch" if touch_client else "linkDesktop",
                                    "items": [],
                                },
                            ]
                        },
                    },
                    allow_different_len=False,
                )
            else:
                self.assertFragmentNotIn(
                    response,
                    {
                        "incuts": {
                            "results": [
                                {
                                    "inClid": 18,
                                }
                            ]
                        },
                    },
                )


if __name__ == '__main__':
    main()
