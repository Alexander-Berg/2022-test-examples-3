#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types import (
    Model,
    Offer,
    Shop,
    UGCItem,
    NavCategory,
    HyperCategory,
    HyperCategoryType,
    MnPlace,
    Region,
    RegionalDelivery,
    DeliveryBucket,
    DeliveryOption,
)
from core.testcase import TestCase, main
from core.matcher import Contains, ElementCount, EmptyList
from core.blender_bundles import create_blender_bundles, get_supported_incuts_cgi

import json
from collections import OrderedDict
from copy import deepcopy


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
    BUNDLE_BLENDING_MATERIALS_LIST = '''
{
    "incut_places": ["Search"],
    "incut_positions": [1, 3, 4, 5],
    "incut_viewtypes": ["ArticlesGallery"],
    "incut_ids": ["default"],
    "result_scores": [
        {
            "incut_place": "Search",
            "row_position": 1,
            "incut_viewtype": "ArticlesGallery",
            "incut_id": "default",
            "score": 0.75
        },
        {
            "incut_place": "Search",
            "row_position": 3,
            "incut_viewtype": "ArticlesGallery",
            "incut_id": "default",
            "score": 1.0
        },
        {
            "incut_place": "Search",
            "row_position": 4,
            "incut_viewtype": "ArticlesGallery",
            "incut_id": "default",
            "score": 0.5
        },
        {
            "incut_place": "Search",
            "row_position": 5,
            "incut_viewtype": "ArticlesGallery",
            "incut_id": "default",
            "score": 0.5
        }
    ],
    "calculator_type": "ConstPosition"
}
'''
    BUNDLE_BLENDING_MATERIALS_LIST_TOP = '''
{
    "incut_places": ["Top"],
    "incut_positions": [1],
    "incut_viewtypes": ["ArticlesGallery"],
    "incut_ids": ["default"],
    "result_scores": [
        {
            "incut_place": "Top",
            "row_position": 1,
            "incut_viewtype": "ArticlesGallery",
            "incut_id": "default",
            "score": 1.0
        }
    ],
    "calculator_type": "ConstPosition"
}
'''
    BUNDLE_BLENDING_PREMIUM_ADS_LIST = '''
{
    "incut_places": ["Search"],
    "incut_positions": [1, 3, 4, 5],
    "incut_viewtypes": ["Gallery"],
    "incut_ids": ["default"],
    "result_scores": [
        {
            "incut_place": "Search",
            "row_position": 1,
            "incut_viewtype": "Gallery",
            "incut_id": "default",
            "score": 1.0
        },
        {
            "incut_place": "Search",
            "row_position": 3,
            "incut_viewtype": "Gallery",
            "incut_id": "default",
            "score": 1.0
        },
        {
            "incut_place": "Search",
            "row_position": 4,
            "incut_viewtype": "Gallery",
            "incut_id": "default",
            "score": 1.0
        },
        {
            "incut_place": "Search",
            "row_position": 5,
            "incut_viewtype": "Gallery",
            "incut_id": "default",
            "score": 1.0
        }
    ],
    "calculator_type": "ConstPosition"
}
'''
    BUNDLE_BLENDING_MATERIALS_GRID = '''
{
    "incut_places": ["Search"],
    "incut_positions": [1, 2, 3],
    "incut_viewtypes": ["ArticlesGallery"],
    "incut_ids": ["default"],
    "result_scores": [
        {
            "incut_place": "Search",
            "row_position": 1,
            "incut_viewtype": "ArticlesGallery",
            "incut_id": "default",
            "score": 0.75
        },
        {
            "incut_place": "Search",
            "row_position": 2,
            "incut_viewtype": "ArticlesGallery",
            "incut_id": "default",
            "score": 1.0
        },
        {
            "incut_place": "Search",
            "row_position": 3,
            "incut_viewtype": "ArticlesGallery",
            "incut_id": "default",
            "score": 0.5
        }
    ],
    "calculator_type": "ConstPosition"
}
'''
    BUNDLE_BLENDING_MATERIALS_GRID_TOP = '''
{
    "incut_places": ["Top"],
    "incut_positions": [1],
    "incut_viewtypes": ["ArticlesGallery"],
    "incut_ids": ["default"],
    "result_scores": [
        {
            "incut_place": "Top",
            "row_position": 1,
            "incut_viewtype": "ArticlesGallery",
            "incut_id": "default",
            "score": 1.0
        }
    ],
    "calculator_type": "ConstPosition"
}
'''
    BUNDLE_BLENDING_PREMIUM_ADS_GRID = '''
{
    "incut_places": ["Search"],
    "incut_positions": [1, 2, 3],
    "incut_viewtypes": ["Gallery"],
    "incut_ids": ["default"],
    "result_scores": [
        {
            "incut_place": "Search",
            "row_position": 1,
            "incut_viewtype": "Gallery",
            "incut_id": "default",
            "score": 1.0
        },
        {
            "incut_place": "Search",
            "row_position": 2,
            "incut_viewtype": "Gallery",
            "incut_id": "default",
            "score": 1.0
        },
        {
            "incut_place": "Search",
            "row_position": 3,
            "incut_viewtype": "Gallery",
            "incut_id": "default",
            "score": 1.0
        }
    ],
    "calculator_type": "ConstPosition"
}
'''
    BUNDLE_BLENDING_MATERIALS_AT_9_SEARCH_POSITION = '''
{
    "incut_places": ["Search"],
    "incut_positions": [9],
    "incut_viewtypes": ["Gallery"],
    "incut_ids": ["default"],
    "result_scores": [
        {
            "incut_place": "Search",
            "row_position": 9,
            "incut_viewtype": "Gallery",
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
        "client == frontend && platform == desktop && search_type == text" : {
            "bundle_name": "bundle_material_list.json"
        },
        "client == frontend && platform == desktop && search_type == textless" : {
            "bundle_name": "bundle_material_list_top.json"
        },
        "client == frontend && platform == desktop && search_type == text" : {
            "bundle_name": "bundle_material_grid.json"
        },
        "client == frontend && platform == desktop && search_type == textless" : {
            "bundle_name": "bundle_material_grid_top.json"
        },
        "client == frontend && platform == touch && search_type == text" : {
            "bundle_name": "bundle_material_list.json"
        },
        "client == frontend && platform == touch && search_type == text" : {
            "bundle_name": "bundle_material_grid.json"
        },
        "client == frontend && platform == desktop" : {
            "bundle_name": "bundle_material_at_9_search_position.json"
        }
    },
    "INCLID_PREMIUM_ADS": {
        "client == frontend && platform == desktop" : {
            "bundle_name": "bundle_premium_list.json"
        },
        "client == frontend && platform == desktop" : {
            "bundle_name": "bundle_premium_grid.json"
        },
        "client == frontend && platform == touch && search_type == text" : {
            "bundle_name": "bundle_premium_list.json"
        },
        "client == frontend && platform == touch && search_type == text" : {
            "bundle_name": "bundle_premium_grid.json"
        }
    }
}
"""


def get_found_bundle_str(bundle_name, inclid):
    return "Bundle by name " + bundle_name + " selected for inClid=" + inclid


ACCESS_LOG_GENERATED = OrderedDict(
    [
        ("2", ["default"]),
        ("1", ["default"]),
    ]
)

ACCESS_LOG_CALCULATED = OrderedDict(
    [
        (
            "1",
            OrderedDict(
                [
                    (
                        "result_scores",
                        [
                            OrderedDict(
                                [
                                    ("incut_id", "default"),
                                    ("incut_place", 1),
                                    ("incut_viewtype", 4),
                                    ("position", 1),
                                    ("score", 0.75),
                                ]
                            ),
                            OrderedDict(
                                [
                                    ("incut_id", "default"),
                                    ("incut_place", 1),
                                    ("incut_viewtype", 4),
                                    ("position", 2),
                                    ("score", 1),
                                ]
                            ),
                            OrderedDict(
                                [
                                    ("incut_id", "default"),
                                    ("incut_place", 1),
                                    ("incut_viewtype", 4),
                                    ("position", 3),
                                    ("score", 0.5),
                                ]
                            ),
                        ],
                    )
                ]
            ),
        ),
        (
            "2",
            OrderedDict(
                [
                    (
                        "result_scores",
                        [
                            OrderedDict(
                                [
                                    ("incut_id", "default"),
                                    ("incut_place", 1),
                                    ("incut_viewtype", 1),
                                    ("position", 1),
                                    ("score", 1),
                                ]
                            ),
                            OrderedDict(
                                [
                                    ("incut_id", "default"),
                                    ("incut_place", 1),
                                    ("incut_viewtype", 1),
                                    ("position", 2),
                                    ("score", 1),
                                ]
                            ),
                            OrderedDict(
                                [
                                    ("incut_id", "default"),
                                    ("incut_place", 1),
                                    ("incut_viewtype", 1),
                                    ("position", 3),
                                    ("score", 1),
                                ]
                            ),
                        ],
                    )
                ]
            ),
        ),
    ]
)

ACCESS_LOG_BLENDED = OrderedDict(
    [
        ("1", {"default": {"incut_place": 1, "position": 5}}),
        ("2", {"default": {"incut_place": 1, "position": 1}}),
    ]
)


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
        cls.index.navtree += [
            NavCategory(
                hid=66,
                nid=6666,
                is_blue=False,
                name='Марс и другие шоколадки',
            ),
        ]
        cls.index.hypertree += [
            HyperCategory(hid=66, output_type=HyperCategoryType.GURU, name='марс', uniq_name='марс')
        ]
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
                        'nid': '["6666",]',
                    },
                )
            ]
        )
        cls.index.dssm.hard2_query_embedding.on(query='марс').set(*embs_mars)

    @classmethod
    def prepare_blending(cls):
        cls.index.regiontree += [
            Region(rid=25, name='Local'),
        ]

        regional_delimeter_idx = 3
        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=211 + i,
                fesh=66 + i,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(rid=25, options=[DeliveryOption(price=400, day_from=1, day_to=3, order_before=23)])
                ],
            )
            for i in range(regional_delimeter_idx, 10)
        ]

        titles = ["Air", "Breath", "Fresh", "Oxy", "Воздух", "Дыши", "Вдох", "Выдох", "Кислород", "Небо"]
        cls.index.models += [
            Model(hid=66, hyperid=66 + i, title="Модель {}".format(titles[i]), ts=100020 + i) for i in range(1, 10)
        ]

        cls.index.shops += [
            Shop(
                fesh=66 + i,
                priority_region=25,
                shop_fee=100,
                cpa=Shop.CPA_REAL,
                name='CPA Shop {} regional'.format(titles[i]),
            )
            for i in range(1, regional_delimeter_idx)
        ]
        cls.index.shops += [
            Shop(
                fesh=66 + i, priority_region=213, shop_fee=100, cpa=Shop.CPA_REAL, name='CPA Shop {}'.format(titles[i])
            )
            for i in range(regional_delimeter_idx, 10)
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
            for i in range(1, regional_delimeter_idx)
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
                delivery_buckets=[211 + i],
            )
            for i in range(regional_delimeter_idx, 10)
        ]

        for i in range(1, 10):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100020 + i).respond(0.04)

    @classmethod
    def prepare_blender_bundles_config(cls):
        cls.settings.formulas_path = create_blender_bundles(
            cls.meta_paths.testroot,
            BlenderBundlesConfig.BUNDLES_CONFIG,
            {
                "bundle_material_list.json": BlenderBundleConstPositon.BUNDLE_BLENDING_MATERIALS_LIST,
                "bundle_material_list_top.json": BlenderBundleConstPositon.BUNDLE_BLENDING_MATERIALS_LIST_TOP,
                "bundle_premium_list.json": BlenderBundleConstPositon.BUNDLE_BLENDING_PREMIUM_ADS_LIST,
                "bundle_material_grid.json": BlenderBundleConstPositon.BUNDLE_BLENDING_MATERIALS_GRID,
                "bundle_material_grid_top.json": BlenderBundleConstPositon.BUNDLE_BLENDING_MATERIALS_GRID_TOP,
                "bundle_premium_grid.json": BlenderBundleConstPositon.BUNDLE_BLENDING_PREMIUM_ADS_GRID,
                "bundle_material_at_9_search_position.json": BlenderBundleConstPositon.BUNDLE_BLENDING_MATERIALS_AT_9_SEARCH_POSITION,
            },
        )

    def test_blending(self):
        """Статейная и премиальная врезки конкурируют за позиции 1, 3, 4, 5 для листовой выдачи
        и за позиции 1, 4, 5, 7, 8 для гридовой.
        """

        """
        - Для листовой выдачи на десктопе и таче в заданных позициях должны быть показаны следующие врезки:
        1: премиальная врезка, поскольку у нее выше score в бандле
        3: ни одна из врезок из-за продуктового ограничения (3 снипетта между врезками)
        4: статейная врезка, так как премиальная уже была установлена выше, а у статейной наибольший скор на позиции
        5: ни одна из врезок, так как обе врезки уже поставлены выше + продуктовое ограничение (3 снипетта между врезками)
        """
        request = (
            "place=blender&text=марс&additional_entities=articles&debug=da&"
            + "viewtype=list&client=frontend&platform=desktop&"
            + "rearr-factors=market_blender_use_bundles_config=1;market_blender_cpa_shop_incut_enabled=1;market_blender_write_access_log=1;"
            + "market_blender_bundles_for_inclid=1:bundle_material_list,2:bundle_premium_list;"
            + "market_ugc_saas_enabled=1;"
            + "market_blender_media_adv_incut_enabled=0;"
        )
        request += "&supported-incuts=" + get_supported_incuts_cgi()
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains(get_found_bundle_str("bundle_premium_list.json", "INCLID_PREMIUM_ADS")),
                    Contains(get_found_bundle_str("bundle_material_list.json", "INCLID_MATERIAL_ENTRYPOINTS")),
                ]
            },
        )
        """
        Реальные позиции в выдаче:
        1: для премиальной
        5: для статейной, так как к органической позиции = 4 прибавится единица из-за ранее установленной премиальной
        """
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "entity": "searchIncut",
                            "title": "Популярные предложения",
                            "position": 1,
                            "inClid": 2,
                        },
                        {
                            "entity": "searchIncut",
                            "title": "Полезные статьи",
                            "position": 5,
                            "inClid": 1,
                        },
                    ]
                },
            },
            allow_different_len=False,
            preserve_order=True,
        )

        request = (
            "place=blender&text=марс&touch=1&additional_entities=articles&debug=da&"
            + "viewtype=list&client=frontend&platform=touch&"
            + "rearr-factors=market_blender_use_bundles_config=1;market_blender_cpa_shop_incut_enabled=1;market_blender_write_access_log=1;"
            + "market_blender_bundles_for_inclid=1:bundle_material_list,2:bundle_premium_list;"
            + "market_ugc_saas_enabled=1;"
            + "market_blender_media_adv_incut_enabled=0;"
        )
        request += "&supported-incuts=" + get_supported_incuts_cgi()
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains(get_found_bundle_str("bundle_premium_list.json", "INCLID_PREMIUM_ADS")),
                    Contains(get_found_bundle_str("bundle_material_list.json", "INCLID_MATERIAL_ENTRYPOINTS")),
                ]
            },
        )
        """
        Реальные позиции в выдаче:
        1: для премиальной
        5: для статейной, так как к органической позиции = 4 прибавится единица из-за ранее установленной премиальной
        """
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "entity": "searchIncut",
                            "title": "Популярные предложения",
                            "position": 1,
                            "inClid": 2,
                        },
                        {
                            "entity": "searchIncut",
                            "title": "Полезные статьи",
                            "position": 5,
                            "inClid": 1,
                        },
                    ]
                },
            },
            allow_different_len=False,
            preserve_order=True,
        )

        blended_incuts_organic_positions = OrderedDict(
            [
                ("1", {"default": {"incut_place": 1, "position": 5}}),
                ("2", {"default": {"incut_place": 1, "position": 1}}),
            ]
        )
        self.access_log.expect(
            blender_incuts_blended=json.dumps(blended_incuts_organic_positions, separators=(',', ':'))
        ).times(2)

        """
        - Для гридовой выдачи на десктопе в заданных позициях должны быть показаны следующие врезки:
        1: премиальная врезка, поскольку у нее выше score в бандле
        4: ни одна из врезок из-за продуктового ограничения (6 снипеттов между врезками)
        5: ни одна из врезок из-за продуктового ограничения (6 снипеттов между врезками)
        7: статейная врезка, так как премиальная уже была установлена выше, а у статейной наибольший скор на позиции
        8: ни одна из врезок, так как обе врезки уже поставлены выше + продуктовое ограничение (6 снипеттов между врезками)
        """
        request = (
            "place=blender&text=марс&additional_entities=articles&debug=da&"
            + "viewtype=grid&client=frontend&platform=desktop&columns-in-grid=3&"
            + "rearr-factors=market_blender_use_bundles_config=1;market_blender_cpa_shop_incut_enabled=1;market_blender_write_access_log=1;"
            + "market_blender_bundles_for_inclid=1:bundle_material_grid,2:bundle_premium_grid;"
            + "market_ugc_saas_enabled=1;"
            + "market_blender_media_adv_incut_enabled=0;"
        )
        request += "&supported-incuts=" + get_supported_incuts_cgi()
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains(get_found_bundle_str("bundle_premium_grid.json", "INCLID_PREMIUM_ADS")),
                    Contains(get_found_bundle_str("bundle_material_grid.json", "INCLID_MATERIAL_ENTRYPOINTS")),
                ]
            },
        )
        """
        Реальные позиции в выдаче:
        1: для премиальной
        8: для статейной, так как к органической позиции = 7 прибавится единица из-за ранее установленной премиальной
        """
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "entity": "searchIncut",
                            "title": "Популярные предложения",
                            "position": 1,
                            "inClid": 2,
                        },
                        {
                            "entity": "searchIncut",
                            "title": "Полезные статьи",
                            "position": 8,
                            "inClid": 1,
                        },
                    ]
                },
            },
            allow_different_len=False,
            preserve_order=True,
        )

        blended_incuts_organic_positions = OrderedDict(
            [
                ("1", {"default": {"incut_place": 1, "position": 7}}),
                ("2", {"default": {"incut_place": 1, "position": 1}}),
            ]
        )
        self.access_log.expect(
            blender_incuts_blended=json.dumps(blended_incuts_organic_positions, separators=(',', ':'))
        ).once()

        """
        - Для гридовой выдачи на таче в заданных позициях должны быть показаны следующие врезки:
        1: премиальная врезка, поскольку у нее выше score в бандле
        4: ни одна из врезок из-за продуктового ограничения (4 снипетта между врезками)
        5: статейная врезка, так как премиальная уже была установлена выше, а у статейной наибольший скор на позиции
        7: ни одна из врезок, так как обе врезки уже поставлены выше + продуктовое ограничение (4 снипетта между врезками)
        8: ни одна из врезок, так как обе врезки уже поставлены выше + продуктовое ограничение (4 снипетта между врезками)
        """
        request = (
            "place=blender&text=марс&touch=1&additional_entities=articles&debug=da&"
            + "viewtype=grid&client=frontend&platform=touch&columns-in-grid=2&"
            + "rearr-factors=market_blender_use_bundles_config=1;market_blender_cpa_shop_incut_enabled=1;"
            + "market_blender_write_access_log=1;market_blender_write_calculators_access_log=1;"
            + "market_blender_bundles_for_inclid=1:bundle_material_grid,2:bundle_premium_grid;"
            + "market_ugc_saas_enabled=1;"
            + "market_blender_media_adv_incut_enabled=0;"
        )
        request += "&supported-incuts=" + get_supported_incuts_cgi()
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains(get_found_bundle_str("bundle_premium_grid.json", "INCLID_PREMIUM_ADS")),
                    Contains(get_found_bundle_str("bundle_material_grid.json", "INCLID_MATERIAL_ENTRYPOINTS")),
                ]
            },
        )
        """
        Реальные позиции в выдаче:
        1: для премиальной
        6: для статейной, так как к органической позиции = 5 прибавится единица из-за ранее установленной премиальной
        """
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "entity": "searchIncut",
                            "title": "Популярные предложения",
                            "position": 1,
                            "inClid": 2,
                        },
                        {
                            "entity": "searchIncut",
                            "title": "Полезные статьи",
                            "position": 6,
                            "inClid": 1,
                        },
                    ]
                },
            },
            allow_different_len=False,
            preserve_order=True,
        )

        self.access_log.expect(blender_incuts_blended=json.dumps(ACCESS_LOG_BLENDED, separators=(',', ':'))).once()

        self.access_log.expect(blender_incuts_generated=json.dumps(ACCESS_LOG_GENERATED, separators=(',', ':'))).times(
            4
        )

        self.access_log.expect(
            blender_incuts_calculated=json.dumps(ACCESS_LOG_CALCULATED, separators=(',', ':'))
        ).once()

    def test_inclids_banned_after_calculator(self):
        request = (
            "place=blender&text=марс&touch=1&additional_entities=articles&debug=da&"
            + "viewtype=grid&client=frontend&platform=touch&columns-in-grid=2&"
            + "rearr-factors=market_blender_use_bundles_config=1;market_blender_cpa_shop_incut_enabled=1;"
            + "market_blender_write_access_log=1;market_blender_write_calculators_access_log=1;"
            + "market_blender_bundles_for_inclid=1:bundle_material_grid,2:bundle_premium_grid;"
            + "market_blender_inclids_banned_after_calculator=2;"
            + "market_ugc_saas_enabled=1;"
            + "market_blender_media_adv_incut_enabled=0;"
        )
        request += "&supported-incuts=" + get_supported_incuts_cgi()
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains(get_found_bundle_str("bundle_premium_grid.json", "INCLID_PREMIUM_ADS")),
                    Contains(get_found_bundle_str("bundle_material_grid.json", "INCLID_MATERIAL_ENTRYPOINTS")),
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
                            "title": "Полезные статьи",
                            "position": 1,
                            "inClid": 1,
                        },
                    ]
                },
            },
            allow_different_len=False,
            preserve_order=True,
        )

        blended_incuts_organic_positions = OrderedDict([("1", {"default": {"incut_place": 1, "position": 1}})])
        self.access_log.expect(
            blender_incuts_blended=json.dumps(blended_incuts_organic_positions, separators=(',', ':'))
        ).once()

        self.access_log.expect(blender_incuts_generated=json.dumps(ACCESS_LOG_GENERATED, separators=(',', ':'))).once()

        self.access_log.expect(
            blender_incuts_calculated=json.dumps(ACCESS_LOG_CALCULATED, separators=(',', ':'))
        ).once()

    def test_inclids_banned_after_generator(self):
        request = (
            "place=blender&text=марс&touch=1&additional_entities=articles&debug=da&"
            + "viewtype=grid&client=frontend&platform=touch&columns-in-grid=2&"
            + "rearr-factors=market_blender_use_bundles_config=1;market_blender_cpa_shop_incut_enabled=1;"
            + "market_blender_write_access_log=1;market_blender_write_calculators_access_log=1;"
            + "market_blender_bundles_for_inclid=1:bundle_material_grid,2:bundle_premium_grid;"
            + "market_blender_inclids_banned_after_generator=2;"
            + "market_ugc_saas_enabled=1;"
            + "market_blender_media_adv_incut_enabled=0;"
        )
        request += "&supported-incuts=" + get_supported_incuts_cgi()
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains(get_found_bundle_str("bundle_premium_grid.json", "INCLID_PREMIUM_ADS")),
                    Contains(get_found_bundle_str("bundle_material_grid.json", "INCLID_MATERIAL_ENTRYPOINTS")),
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
                            "title": "Полезные статьи",
                            "position": 1,
                            "inClid": 1,
                        },
                    ]
                },
            },
            allow_different_len=False,
            preserve_order=True,
        )

        self.access_log.expect(blender_incuts_generated=json.dumps(ACCESS_LOG_GENERATED, separators=(',', ':'))).once()

        calculated = deepcopy(ACCESS_LOG_CALCULATED)
        del calculated["2"]
        self.access_log.expect(blender_incuts_calculated=json.dumps(calculated, separators=(',', ':'))).once()

        blended_incuts_organic_positions = OrderedDict([("1", {"default": {"incut_place": 1, "position": 1}})])
        self.access_log.expect(
            blender_incuts_blended=json.dumps(blended_incuts_organic_positions, separators=(',', ':'))
        ).once()

    def test_inclids_banned_after_blending(self):
        request = (
            "place=blender&text=марс&touch=1&additional_entities=articles&debug=da&"
            + "viewtype=grid&client=frontend&platform=touch&columns-in-grid=2&"
            + "rearr-factors=market_blender_use_bundles_config=1;market_blender_cpa_shop_incut_enabled=1;"
            + "market_blender_write_access_log=1;market_blender_write_calculators_access_log=1;"
            + "market_blender_bundles_for_inclid=1:bundle_material_grid,2:bundle_premium_grid;"
            + "market_blender_inclids_banned_after_blending=2;"
            + "market_ugc_saas_enabled=1;"
            + "market_blender_media_adv_incut_enabled=0;"
        )
        request += "&supported-incuts=" + get_supported_incuts_cgi()
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains(get_found_bundle_str("bundle_premium_grid.json", "INCLID_PREMIUM_ADS")),
                    Contains(get_found_bundle_str("bundle_material_grid.json", "INCLID_MATERIAL_ENTRYPOINTS")),
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
                            "title": "Полезные статьи",
                            "position": 5,
                            "inClid": 1,
                        },
                    ]
                },
            },
            allow_different_len=False,
            preserve_order=True,
        )

        self.access_log.expect(blender_incuts_generated=json.dumps(ACCESS_LOG_GENERATED, separators=(',', ':'))).once()

        self.access_log.expect(
            blender_incuts_calculated=json.dumps(ACCESS_LOG_CALCULATED, separators=(',', ':'))
        ).once()

        self.access_log.expect(blender_incuts_blended=json.dumps(ACCESS_LOG_BLENDED, separators=(',', ':'))).once()

    def test_inclids_banned_after_blending_multiple_times(self):
        request = (
            "place=blender&text=марс&touch=1&additional_entities=articles&debug=da&"
            + "market_blender_inclids_banned_after_blending=1;"
            + "viewtype=grid&client=frontend&platform=touch&columns-in-grid=2&"
            + "rearr-factors=market_blender_use_bundles_config=1;market_blender_cpa_shop_incut_enabled=1;"
            + "market_blender_write_access_log=1;market_blender_write_calculators_access_log=1;"
            + "market_blender_bundles_for_inclid=1:bundle_material_grid,2:bundle_premium_grid;"
            + "market_blender_inclids_banned_after_blending=2;"
            + "market_ugc_saas_enabled=1;"
            + "market_blender_media_adv_incut_enabled=0;"
        )
        request += "&supported-incuts=" + get_supported_incuts_cgi()
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains(get_found_bundle_str("bundle_premium_grid.json", "INCLID_PREMIUM_ADS")),
                    Contains(get_found_bundle_str("bundle_material_grid.json", "INCLID_MATERIAL_ENTRYPOINTS")),
                ]
            },
        )
        self.assertFragmentIn(
            response,
            {
                "incuts": {"results": EmptyList()},
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_blending_with_top(self):
        """Статейная врезка показывается над выдачей, премиальная пытается встроиться
        в выдачу с учетом продуктовых ограничений
        """

        """
        - Для листовой выдачи на бестексте десктопа в заданных позициях должны быть показаны следующие врезки:
        Над выдачей:
            1: статейная, так как нет других претендентов
        Внутри выдачи:
            1: ни одна из врезок из-за продуктового ограничения (3 снипетта между, бан врезок сквозной между плейсами)
            3: ни одна из врезок из-за продуктового ограничения (3 снипетта между, бан врезок сквозной между плейсами)
            4: премиальная, так как нет других претендентов
            5: ни одна из врезок, так как обе уже показаны
        """
        request = (
            "place=blender&hid=66&additional_entities=articles&debug=da&"
            + "viewtype=list&client=frontend&platform=desktop&"
            + "rearr-factors=market_blender_use_bundles_config=1;market_blender_cpa_shop_incut_enabled=1;"
            + "market_blender_bundles_for_inclid=1:bundle_material_list_top,2:bundle_premium_list;"
            + "market_ugc_saas_enabled=1;"
            + "market_blender_media_adv_incut_enabled=0;"
        )
        request += "&supported-incuts=" + get_supported_incuts_cgi()
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains(get_found_bundle_str("bundle_premium_list.json", "INCLID_PREMIUM_ADS")),
                    Contains(get_found_bundle_str("bundle_material_list_top.json", "INCLID_MATERIAL_ENTRYPOINTS")),
                ]
            },
        )
        """
        Реальные позиции в выдаче:
        1(place=Top(id=2)): для статейной
        4(place=Search(id=1)): для премиальной, сдвига реальной позиции не происходит,
            так как ранее в выдаче не было ни одной врезки
        """
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "entity": "searchIncut",
                            "title": "Полезные статьи",
                            "position": 1,
                            "placeId": 2,
                            "inClid": 1,
                        },
                        {
                            "entity": "searchIncut",
                            "title": "Популярные предложения",
                            "position": 4,
                            "placeId": 1,
                            "inClid": 2,
                        },
                    ]
                },
            },
            allow_different_len=False,
            preserve_order=True,
        )

        """
        - Для гридовой выдачи на бестексте десктопа в заданных позициях должны быть показаны следующие врезки:
        Над выдачей:
            1: статейная, так как нет других претендентов
        Внутри выдачи:
            1: ни одна из врезок из-за продуктового ограничения (6 снипеттов между врезками)
            4: ни одна из врезок из-за продуктового ограничения (6 снипеттов между врезками)
            5: ни одна из врезок из-за продуктового ограничения (6 снипеттов между врезками)
            7: премиальная врезка, так как нет других претендентов
            8: ни одна из врезок, так как обе врезки уже поставлены выше + продуктовое ограничение (6 снипеттов между врезками)
        """
        request = (
            "place=blender&hid=66&additional_entities=articles&debug=da&"
            + "viewtype=grid&client=frontend&platform=desktop&columns-in-grid=3&"
            + "rearr-factors=market_blender_use_bundles_config=1;market_blender_cpa_shop_incut_enabled=1;"
            + "market_blender_bundles_for_inclid=1:bundle_material_grid_top,2:bundle_premium_grid;"
            + "market_ugc_saas_enabled=1;"
            + "market_blender_media_adv_incut_enabled=0;"
        )
        request += "&supported-incuts=" + get_supported_incuts_cgi()
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains(get_found_bundle_str("bundle_premium_grid.json", "INCLID_PREMIUM_ADS")),
                    Contains(get_found_bundle_str("bundle_material_grid_top.json", "INCLID_MATERIAL_ENTRYPOINTS")),
                ]
            },
        )
        """
        Реальные позиции в выдаче:
        1(place=Top(id=2)): для статейной
        7(place=Search(id=1)): для премиальной, сдвига позиции не будет,
            так как внутри выдачи выше не было ни одной врезки
        """
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "entity": "searchIncut",
                            "title": "Полезные статьи",
                            "position": 1,
                            "placeId": 2,
                            "inClid": 1,
                        },
                        {
                            "entity": "searchIncut",
                            "title": "Популярные предложения",
                            "position": 7,
                            "placeId": 1,
                            "inClid": 2,
                        },
                    ]
                },
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_blending_with_delimeter(self):
        """Статейная и премиальная врезки конкурируют за позиции 1, 3, 4, 5 для листовой выдачи
        и за позиции 1, 4, 5, 7, 8 для гридовой c учетом регионального разделителя на позиции 3
        """

        """
        - Для листовой выдачи на десктопе в заданных позициях должны быть показаны следующие врезки:
        1: премиальная врезка, поскольку у нее выше score в бандле
        3: ни одна из врезок из-за продуктового ограничения (3 снипетта между врезками)
        4: статейная врезка, так как премиальная уже была установлена выше, а у статейной наибольший скор на позиции
        5: ни одна из врезок, так как обе установлены ранее
        """
        request = (
            "place=blender&rids=25&text=марс&additional_entities=articles&debug=da&local-offers-first=1&allow-collapsing=1&"
            "viewtype=list&client=frontend&platform=desktop&"
            "rearr-factors=market_blender_use_bundles_config=1;market_blender_cpa_shop_incut_enabled=1;"
            "market_blender_bundles_for_inclid=1:bundle_material_list,2:bundle_premium_list;"
            "market_ugc_saas_enabled=1;"
        )
        request += "&supported-incuts=" + get_supported_incuts_cgi()
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains(get_found_bundle_str("bundle_premium_list.json", "INCLID_PREMIUM_ADS")),
                    Contains(get_found_bundle_str("bundle_material_list.json", "INCLID_MATERIAL_ENTRYPOINTS")),
                ]
            },
        )
        """
        Реальные позиции в выдаче:
        1: для премиальной
        5: для статейной, так как к органической позиции = 4 прибавится единица из-за ранее установленной премиальной
        """
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "entity": "searchIncut",
                            "title": "Популярные предложения",
                            "position": 1,
                            "inClid": 2,
                        },
                        {
                            "entity": "searchIncut",
                            "title": "Полезные статьи",
                            "position": 5,
                            "inClid": 1,
                        },
                    ]
                },
            },
            allow_different_len=False,
            preserve_order=True,
        )

        """
        - Для гридовой выдачи на десктопе в заданных позициях должны быть показаны следующие врезки:
        1: премиальная врезка, поскольку у нее выше score в бандле
        4: ни одна из врезок из-за продуктового ограничения (6 снипеттов между врезками)
        5: ни одна из врезок из-за продуктового ограничения (6 снипеттов между врезками)
        7: статейная врезка, так как премиальная уже была установлена выше, а у статейной наибольший скор на позиции
           (поскольку в первой строке до разделителя только два сниппета, может показаться, что позиция врезки = 6,
           но региональный разделитель также занимает позицию, поэтому она сдвигается до седьмой)
        8: ни одна из врезок, так как обе врезки уже поставлены выше + продуктовое ограничение (6 снипеттов между врезками)
        """
        request = (
            "place=blender&rids=25&text=марс&additional_entities=articles&debug=da&local-offers-first=1&allow-collapsing=1&"
            "viewtype=grid&client=frontend&platform=desktop&columns-in-grid=3&"
            "rearr-factors=market_blender_use_bundles_config=1;market_blender_cpa_shop_incut_enabled=1;"
            "market_blender_bundles_for_inclid=1:bundle_material_grid,2:bundle_premium_grid;"
            "market_ugc_saas_enabled=1;"
        )
        request += "&supported-incuts=" + get_supported_incuts_cgi()
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains(get_found_bundle_str("bundle_premium_grid.json", "INCLID_PREMIUM_ADS")),
                    Contains(get_found_bundle_str("bundle_material_grid.json", "INCLID_MATERIAL_ENTRYPOINTS")),
                ]
            },
        )
        """
        Реальные позиции в выдаче:
        1: для премиальной
        8: для статейной, так как к органической позиции = 7 прибавится единица из-за ранее установленной премиальной
        """
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "entity": "searchIncut",
                            "title": "Популярные предложения",
                            "position": 1,
                            "inClid": 2,
                        },
                        {
                            "entity": "searchIncut",
                            "title": "Полезные статьи",
                            "position": 8,
                            "inClid": 1,
                        },
                    ]
                },
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_blender_incut_place_in_show_log(self):
        request = (
            "place=blender&hid=66&additional_entities=articles&debug=da&"
            + "viewtype=list&client=frontend&platform=desktop&"
            + "rearr-factors=market_blender_use_bundles_config=1;market_blender_cpa_shop_incut_enabled=1;"
            + "market_blender_bundles_for_inclid=1:bundle_material_list_top,2:bundle_premium_list;"
            + "market_ugc_saas_enabled=1;"
            + "market_blender_media_adv_incut_enabled=0;"
        )
        request += "&supported-incuts=" + get_supported_incuts_cgi()
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "entity": "searchIncut",
                            "title": "Полезные статьи",
                            "position": 1,
                            "placeId": 2,
                            "inClid": 1,
                        },
                        {
                            "entity": "searchIncut",
                            "title": "Популярные предложения",
                            "position": 4,
                            "placeId": 1,
                            "inClid": 2,
                        },
                    ]
                },
            },
            allow_different_len=False,
            preserve_order=True,
        )
        self.show_log_tskv.expect(record_type=3, inclid=1, incut_place=2, incut_viewtype=4, incut_id="default")
        self.show_log_tskv.expect(record_type=3, inclid=2, incut_place=1, incut_viewtype=1, incut_id="default")

    def test_blender_request_format(self):
        request = (
            "place=blender&text=марс&additional_entities=articles&debug=da&"
            + "viewtype=list&client=frontend&platform=desktop&"
            + "rearr-factors=market_blender_use_bundles_config=1;market_blender_cpa_shop_incut_enabled=1;"
            + "market_blender_bundles_for_inclid=1:bundle_material_list,2:bundle_premium_list;"
            + "market_ugc_saas_enabled=1;"
            + "market_blender_media_adv_incut_enabled=0;"
        )
        request += "&supported-incuts=" + get_supported_incuts_cgi()
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "entity": "searchIncut",
                            "title": "Популярные предложения",
                            "typeId": 1,
                            "placeId": 1,
                            "position": 1,
                            "inClid": 2,
                        },
                        {
                            "entity": "searchIncut",
                            "title": "Полезные статьи",
                            "typeId": 4,
                            "placeId": 1,
                            "position": 5,
                            "inClid": 1,
                        },
                    ]
                },
            },
            allow_different_len=False,
            preserve_order=True,
        )

        """
        Добавление параметра &columns-in-grid=2 изменит ширину грида до двух сниппетов
        в строке и позволит статейной врезке встать на позицию 6 (или органическую позицию 5),
        вместо 8 (органическую 7)
        (по умолчанию на десктопе ширина грида = 3)
        """
        request = (
            "place=blender&text=марс&additional_entities=articles&debug=da&"
            + "viewtype=grid&client=frontend&platform=desktop&columns-in-grid=2&"
            + "rearr-factors=market_blender_use_bundles_config=1;market_blender_cpa_shop_incut_enabled=1;"
            + "market_blender_bundles_for_inclid=1:bundle_material_grid,2:bundle_premium_grid;"
            + "market_ugc_saas_enabled=1;"
            + "market_blender_media_adv_incut_enabled=0;"
        )
        request += "&supported-incuts=" + get_supported_incuts_cgi()
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains(get_found_bundle_str("bundle_premium_grid.json", "INCLID_PREMIUM_ADS")),
                    Contains(get_found_bundle_str("bundle_material_grid.json", "INCLID_MATERIAL_ENTRYPOINTS")),
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
                            "title": "Популярные предложения",
                            "position": 1,
                            "inClid": 2,
                        },
                        {
                            "entity": "searchIncut",
                            "title": "Полезные статьи",
                            "position": 6,
                            "inClid": 1,
                        },
                    ]
                },
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_access_log_incuts_show_count(self):
        request = (
            "place=blender&text=марс&additional_entities=articles&debug=da&"
            + "viewtype=list&client=frontend&platform=desktop&"
            + "supported-incuts="
            + get_supported_incuts_cgi()
            + "&rearr-factors=market_blender_use_bundles_config=1;market_blender_cpa_shop_incut_enabled=1;"
            + "market_blender_bundles_for_inclid=1:bundle_material_list,2:bundle_premium_list;"
            + "market_ugc_saas_enabled=1;"
            + "market_blender_media_adv_incut_enabled=0;"
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"incuts": {"results": ElementCount(2)}})
        self.access_log.expect(blender_incuts_show_count=2).once()

    def test_product_rules_for_premium_ads(self):
        """Тестирование продуктовых ограничений для премиальной врезки"""

        """
        - Если на странице >= 8 элементов, премиальная врезка будет показана вместе со статейной
        """
        request = (
            "place=blender&text=марс&numdoc=8&additional_entities=articles&debug=da&"
            + "viewtype=list&client=frontend&platform=desktop&"
            + "rearr-factors=market_blender_use_bundles_config=1;market_blender_cpa_shop_incut_enabled=1;"
            + "market_blender_bundles_for_inclid=1:bundle_material_list,2:bundle_premium_list;"
            + "market_ugc_saas_enabled=1;"
            + "market_blender_media_adv_incut_enabled=0;"
        )
        request += "&supported-incuts=" + get_supported_incuts_cgi()
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains(get_found_bundle_str("bundle_premium_list.json", "INCLID_PREMIUM_ADS")),
                    Contains(get_found_bundle_str("bundle_material_list.json", "INCLID_MATERIAL_ENTRYPOINTS")),
                ]
            },
        )
        """
        Реальные позиции в выдаче:
        1: для премиальной
        5: для статейной, так как к органической позиции = 4 прибавится единица из-за ранее установленной премиальной
        """
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "entity": "searchIncut",
                            "title": "Популярные предложения",
                            "position": 1,
                            "inClid": 2,
                        },
                        {
                            "entity": "searchIncut",
                            "title": "Полезные статьи",
                            "position": 5,
                            "inClid": 1,
                        },
                    ]
                },
            },
            allow_different_len=False,
            preserve_order=True,
        )
        """
        - Если на странице < 8 элементов, премиальная врезка не будет показана, останется только статейная
        """
        request = (
            "place=blender&text=марс&numdoc=7&additional_entities=articles&debug=da&"
            + "viewtype=list&client=frontend&platform=desktop&"
            + "rearr-factors=market_blender_use_bundles_config=1;market_blender_cpa_shop_incut_enabled=1;"
            + "market_blender_bundles_for_inclid=1:bundle_material_list,2:bundle_premium_list;"
            + "market_ugc_saas_enabled=1;"
            + "market_blender_media_adv_incut_enabled=0;"
        )
        request += "&supported-incuts=" + get_supported_incuts_cgi()
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains(get_found_bundle_str("bundle_premium_list.json", "INCLID_PREMIUM_ADS")),
                    Contains(get_found_bundle_str("bundle_material_list.json", "INCLID_MATERIAL_ENTRYPOINTS")),
                ]
            },
        )
        """
        Реальные позиции в выдаче:
        1: для статейной, так как премиальная показана не будет
        """
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "entity": "searchIncut",
                            "title": "Полезные статьи",
                            "position": 1,
                            "inClid": 1,
                        },
                    ]
                },
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_incut_at_end_of_search_out_banned(self):
        """Врезка не должна быть показана в самом конце поисковой выдачи, так как
        не существует такой органической позиции
        """

        """
        - При numdoc=9 будут показаны все врезки, так как статейная встанет над девятым сниппетом
        """
        request = (
            "place=blender&text=марс&numdoc=9&additional_entities=articles&debug=da&"
            + "viewtype=list&client=frontend&platform=desktop&"
            + "rearr-factors=market_blender_use_bundles_config=1;market_blender_cpa_shop_incut_enabled=1;"
            + "market_blender_bundles_for_inclid=1:bundle_material_at_9_search_position,2:bundle_premium_list;"
            + "market_ugc_saas_enabled=1;"
            + "market_blender_media_adv_incut_enabled=0;"
        )
        request += "&supported-incuts=" + get_supported_incuts_cgi()
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains(get_found_bundle_str("bundle_premium_list.json", "INCLID_PREMIUM_ADS")),
                    Contains(
                        get_found_bundle_str("bundle_material_at_9_search_position.json", "INCLID_MATERIAL_ENTRYPOINTS")
                    ),
                ]
            },
        )
        """
        Реальные позиции в выдаче:
        1: для премиальной
        10: для статейной, так как к органической позиции = 9 прибавится единица из-за ранее установленной премиальной
        """
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "entity": "searchIncut",
                            "title": "Популярные предложения",
                            "position": 1,
                            "inClid": 2,
                        },
                        {
                            "entity": "searchIncut",
                            "title": "Полезные статьи",
                            "position": 10,
                            "inClid": 1,
                        },
                    ]
                },
            },
            allow_different_len=False,
            preserve_order=True,
        )
        """
        - При numdoc=8 будет показана только премиальная, так как нет сниппета на позиции 9, над
        которым может встать статейная
        """
        request = (
            "place=blender&text=марс&numdoc=8&additional_entities=articles&debug=da&"
            + "viewtype=list&client=frontend&platform=desktop&"
            + "rearr-factors=market_blender_use_bundles_config=1;market_blender_cpa_shop_incut_enabled=1;"
            + "market_blender_bundles_for_inclid=1:bundle_material_at_9_search_position,2:bundle_premium_list;"
            + "market_ugc_saas_enabled=1;"
            + "market_blender_media_adv_incut_enabled=0;"
        )
        request += "&supported-incuts=" + get_supported_incuts_cgi()
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains(get_found_bundle_str("bundle_premium_list.json", "INCLID_PREMIUM_ADS")),
                    Contains(
                        get_found_bundle_str("bundle_material_at_9_search_position.json", "INCLID_MATERIAL_ENTRYPOINTS")
                    ),
                ]
            },
        )
        """
        Реальные позиции в выдаче:
        1: для премиальной
        статейная показана не будет
        """
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "entity": "searchIncut",
                            "title": "Популярные предложения",
                            "position": 1,
                            "inClid": 2,
                        }
                    ]
                },
            },
            allow_different_len=False,
            preserve_order=True,
        )


if __name__ == '__main__':
    main()
