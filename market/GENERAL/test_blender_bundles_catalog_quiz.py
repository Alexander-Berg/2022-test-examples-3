#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

import os
from core.types import Model, Offer
from core.testcase import TestCase, main
from core.blender_bundles import create_blender_bundles, get_supported_incuts_cgi
from core.cms_incut_storage import CmsIncutStorage


class BlenderBundleConstPositon:
    BUNDLE = '''
{
    "incut_places": ["Top", "Search"],
    "incut_positions": [1,9],
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
            "row_position": 9,
            "incut_viewtype": "CatalogQuiz",
            "incut_id": "catalog_quiz",
            "score": 1.0
        }
    ],
    "calculator_type": "ConstPosition"
}
'''


class BlenderBundlesConfig:
    BUNDLES_CONFIG = '''
{
    "INCLID_CATALOG_QUIZ_INCUT": {
        "client == ANDROID || client == IOS" : {
            "bundle_name": "bundle_catalog_quiz.json"
        }
    }
}
'''


def get_cms_incuts_mock():
    cms_incuts_data = {
        "catalog_quiz": [
            {
                "page_id": 123,
                "name": "Подборщик. ДетиСпорт. Настольные игры",
                "content": {"start": {}, "final": {}, "scenario": {}},
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
            BlenderBundlesConfig.BUNDLES_CONFIG,
            {
                "bundle_catalog_quiz.json": BlenderBundleConstPositon.BUNDLE,
            },
        )

    @classmethod
    def prepare_catalog_quiz(cls):
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

    def test_blender_catalog_quiz_client(self):
        request_base = "place=blender&hid=9999&rearr-factors=market_blender_media_adv_incut_enabled=0"
        request_base += "&supported-incuts=" + get_supported_incuts_cgi({1: [14]})

        test_data = [
            ("&client=IOS", True),
            ("&client=ANDROID", True),
            ("&client=desktop", False),
            ("&client=touch", False),
        ]

        for client, to_show in test_data:
            response = self.report.request_json(request_base + client)
            if to_show:
                self.assertFragmentIn(
                    response,
                    {
                        "incuts": {
                            "results": [
                                {
                                    "entity": "searchIncut",
                                    "position": 9,
                                    "inClid": 20,
                                    "incutId": "catalog_quiz",
                                    "items": [
                                        {
                                            "entity": "catalog_quiz",
                                            "name": "Подборщик. ДетиСпорт. Настольные игры",
                                            "pageId": 123,
                                            "content": {"start": {}, "final": {}, "scenario": {}},
                                        }
                                    ],
                                }
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
                                    "inClid": 20,
                                }
                            ]
                        },
                    },
                )

    def test_blender_catalog_quiz_position(self):
        top_position_result = 1
        search_position_result = 9
        test_data = [
            ({1: [14]}, search_position_result),
            ({2: [14]}, top_position_result),
            ({1: [14], 2: [14]}, top_position_result),  # в выводе лишь одна врезка
        ]

        for supported_incuts, position in test_data:
            request = (
                "place=blender&hid=9999&rearr-factors=market_blender_media_adv_incut_enabled=0&client=ANDROID&supported-incuts="
                + get_supported_incuts_cgi(supported_incuts)
            )
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "incuts": {
                        "results": [
                            {
                                "entity": "searchIncut",
                                "position": position,
                                "inClid": 20,
                                "incutId": "catalog_quiz",
                                "items": [
                                    {
                                        "entity": "catalog_quiz",
                                        "name": "Подборщик. ДетиСпорт. Настольные игры",
                                        "pageId": 123,
                                        "content": {"start": {}, "final": {}, "scenario": {}},
                                    }
                                ],
                            }
                        ]
                    },
                },
                allow_different_len=False,
            )


if __name__ == '__main__':
    main()
