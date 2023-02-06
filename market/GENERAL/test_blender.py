#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    ClickType,
    Const,
    DateSwitchTimeAndRegionInfo,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
    EntityCtr,
    ExperimentalBoostFeeReservePrice,
    GradeDispersionItem,
    HyperCategory,
    HyperCategoryType,
    IncutBlackListFb,
    LogosInfo,
    MarketSku,
    MnPlace,
    Model,
    NavCategory,
    Offer,
    Opinion,
    QueryEntityCtr,
    RecommendedFee,
    RedirectWhiteListRecord,
    ReportState,
    ReviewDataItem,
    Shop,
    UGCItem,
    VirtualModel,
    NewShopRating,
)
from core.bigb import SkuLastOrderEvent, BeruSkuOrderLastTimeCounter
from core.dj import DjModel
from core.matcher import Round, Regex, NoKey, ElementCount, EmptyList, Greater, NotEmpty, Contains, Absent
from core.testcase import TestCase, main
from core.types.autogen import b64url_md5
from core.types.dynamic_filters import TimeInfo, TimeIntervalInfo, DynamicTimeIntervalsSet
from core.blender_bundles import create_blender_bundles, get_supported_incuts_cgi
from core.types.express_partners import EatsWarehousesEncoder
from core.types.hypercategory import (
    CLOTHES_CATEG_ID,
    EATS_CATEG_ID,
    CategoryStreamRecord,
    Stream,
)

import string
import json
import six
from unittest import skip

if six.PY3:
    import urllib.parse as urlparse
else:
    import urlparse


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

    AVATAR = (
        '{"thumbnails": [{"densities": [{"url": "//avatars.mds.yandex.net/'
        'get-marketcms/879900/img-6540725b-f86a-4ba1-96af-294ce2cdfeba.png/'
        'optimize", "id": "1", "entity": "density"}, {"url": "//avatars.mds.'
        'yandex.net/get-marketcms/879900/img-2001de53-00f4-4139-ada8-'
        'f56162d63a6d.png/optimize", "id": "2", "entity": "density"}], '
        '"containerHeight": "62", "averageColors": {"borders": {"top": '
        '"d3d5c5", "right": "e7e5e6", "left": "c7c7be", "bottom": "a5a4ad"}, '
        '"full": "bcb1a9"}, "containerWidth": "62", "height": "62", "width": '
        '"62", "entity": "thumbnail", "id": "62x62"}], "url": "//avatars.mds.'
        'yandex.net/get-marketcms/879900/img-261c9fed-c00a-459b-a6e0-'
        '6feb893b2f16.png/optimize", "height": "300", "selectedThumb": "62x62"'
        ', "width": "300", "entity": "picture", "isNewTab": false}'
    )


class BlenderBundlesConfig:
    BUNDLES_CONFIG = '''
{
    "INCLID_MATERIAL_ENTRYPOINTS" : {
        "search_type == text": {
            "bundle_name": "bundle_material.json"
        },
        "search_type == textless": {
            "bundle_name": "bundle_material_8.json"
        }
    },
    "INCLID_PREMIUM_ADS": {
        "1" : {
            "bundle_name": "bundle_premium.json"
        }
    },
    "INCLID_RECOM_THEMATIC": {
        "1" : {
            "bundle_name": "bundle_recom_thematic.json"
        }
    },
    "INCLID_ADVERTISING_CAROUSEL": {
        "1" : {
            "bundle_name": "bundle_advertising_carousel.json"
        }
    },
    "INCLID_GROWING_CASHBACK_BANNER": {
        "1" : {
            "bundle_name": "growing_cashback.json"
        }
    },
    "INCLID_EATS_RETAIL_SHOPS_INCUT": {
        "1" : {
            "bundle_name": "eats_retail_shops.json"
        }
    },
    "INCLID_FIRST_PARTY_FASHION": {
        "1" : {
            "bundle_name": "first_party_fashion.json"
        }
    }
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


class BlenderBundleRecomThematics:
    BUNDLE_RECOM_THEMATICS = '''
{
    "incut_places": ["Search"],
    "incut_positions": [2],
    "incut_viewtypes": ["RecomThematicProduct", "RecomThematicCategory"],
    "incut_ids": ["recom_thematic_product_incut_id_1", "recom_thematic_category_incut_id_1"],
    "result_scores": [
        {
            "incut_place": "Search",
            "row_position": 2,
            "incut_viewtype": "RecomThematicProduct",
            "incut_id": "recom_thematic_product_incut_id_1",
            "score": 1.0
        },
        {
            "incut_place": "Search",
            "row_position": 2,
            "incut_viewtype": "RecomThematicCategory",
            "incut_id": "recom_thematic_category_incut_id_1",
            "score": 1.0
        }
    ],
    "calculator_type": "ConstPosition"
}
'''


##
# Множество тестов из этого файла было выпилено в рамках тикета
# https://st.yandex-team.ru/MARKETOUT-44895
# по причине критической неактуальности тестов непродовых врезок
# относительно новой версии блендера.
##
# При необходимости восстановить работу одной из следующих врезок лучшим решением
# будет восстановить тесты из связанного коммита и актуализировать их:
#   * bestsellers_incut
#   * new_products_incut
#   * discount_incut
#   * same_shop_incut
#   * bought_incut
#   * most_often_chosen_incut
#   * popular_brand_product_incut
##


class T(TestCase):
    class CgiParams(dict):
        def raw(self, separator='&'):
            def iter_keys_values():
                for k, v in self.items():
                    if isinstance(v, list):
                        for item in v:
                            yield k, item
                    else:
                        yield k, v

            if len(self):
                return separator.join("{}={}".format(str(k), str(v)) for (k, v) in iter_keys_values())
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

    @staticmethod
    def create_blender_request(parameters, rearr={}, supported_incuts={}):
        request_params = {
            "place": "prime",
            "blender": 1,
            "columns-in-grid": 3,
            "supported-incuts": supported_incuts if supported_incuts else get_supported_incuts_cgi(),
        }
        if not rearr:
            rearr = T.RearrFlags({})
        rearr["market_blender_use_bundles_config"] = 1
        if "market_ugc_saas_enabled" not in rearr:
            rearr["market_ugc_saas_enabled"] = 1
        parameters.update(request_params)
        return T.create_request(parameters, rearr)

    @classmethod
    def prepare(cls):
        cls.settings.market_access_settings.enabled = True
        cls.settings.market_access_settings.download_catstreams = True
        cls.index.category_streams += [
            CategoryStreamRecord(EATS_CATEG_ID, Stream.FMCG.value),
        ]

        cls.settings.default_search_experiment_flags += ['market_new_cpm_iterator=0']
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

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
                "bundle_material.json": BlenderBundleConstPositon.BUNDLE_CONST_SEARCH_POSITION.format(
                    row_position=3, incut_id="default"
                ),
                "bundle_material_8.json": BlenderBundleConstPositon.BUNDLE_CONST_SEARCH_POSITION.format(
                    row_position=8, incut_id="default"
                ),
                "bundle_material_4.json": BlenderBundleConstPositon.BUNDLE_CONST_SEARCH_POSITION.format(
                    row_position=4, incut_id="default"
                ),
                "bundle_premium.json": BlenderBundleConstPositon.BUNDLE_CONST_SEARCH_POSITION.format(
                    row_position=1, incut_id="default"
                ),
                "bundle_recom_thematic.json": BlenderBundleRecomThematics.BUNDLE_RECOM_THEMATICS,
                "bundle_rich_filters.json": BlenderBundleConstPositon.BUNDLE_CONST_SEARCH_POSITION.format(
                    row_position=1, incut_id="rich_filters_incut"
                ),
                "bundle_recom_thematic_9.json": BlenderBundleConstPositon.BUNDLE_CONST_SEARCH_POSITION.format(
                    row_position=9, incut_id="recom_thematic_product_incut_id_1"
                ),
                "bundle_advertising_carousel.json": BlenderBundleConstPositon.BUNDLE_CONST_SEARCH_POSITION.format(
                    row_position=1, incut_id="default"
                ),
                "growing_cashback.json": BlenderBundleConstPositon.BUNDLE_CONST_SEARCH_POSITION.format(
                    row_position=4, incut_id="CMS_GROWING_CASHBACK"
                ),
                "eats_retail_shops.json": BlenderBundleConstPositon.BUNDLE_CONST_SEARCH_POSITION.format(
                    row_position=1, incut_id="default"
                ),
                "first_party_fashion.json": BlenderBundleConstPositon.BUNDLE_CONST_SEARCH_POSITION.format(
                    row_position=1, incut_id="default"
                ),
            },
        )

    @classmethod
    def prepare_entrypoints_from_saas(cls):
        embs = [0.4] * 50

        cls.saas_ugc.on_request(embs=embs, search_size=100, top_size=50,).respond(
            items=[
                UGCItem(
                    relevance=988795807,
                    attributes={
                        'page_id': '56122',
                        'compiled_generation': '20200604_0600',
                        'pages_generation': '20200608_0800',
                        'title': 'Как сделать эко-люстру',
                        'type': 'knowledge',
                        'semantic_id': 'kak-sdelat-ehko-lyustru',
                        'image': Images.IMAGE,
                        'nid': '["1001", "2001", "2123",]',
                    },
                ),
                UGCItem(
                    relevance=892847537,
                    attributes={
                        'page_id': '56124',
                        'title': 'Как выбрать люстру',
                        'subtitle': 'Как же её выбрать',
                        'type': 'expertise',
                        'semantic_id': 'kak-vybrat-lustru',
                        'image': Images.IMAGE,
                        'author_name': 'Федюков Иван Станиславович',
                        'author_description': 'С люстрами на ты',
                        'author_avatar': Images.AVATAR,
                        'nid': '["1001",]',
                    },
                    plain_attributes={
                        'RequestIdInMultiRequest': '0',
                    },
                ),
                UGCItem(
                    relevance=885100722,
                    attributes={
                        'page_id': '56125',
                        'title': '7 правил хорошего освещения',
                        'description': 'Семь, то есть три плюс четыре',
                        'type': 'blog',
                        'semantic_id': '7-pravil-horoshego-osveshhenija',
                        'image': Images.IMAGE,
                        'author_uid': '1234567',
                        'author_url': 'instagram.com/rules',
                        'nid': '["1001", "1234",]',
                    },
                ),
            ]
        )

        cls.index.dssm.hard2_query_embedding.on(query='люстры 1').set(*embs)
        cls.index.dssm.hard2_query_embedding.on(query='канделябры').set(*embs)

        cls.index.hypertree += [
            HyperCategory(hid=6091783, output_type=HyperCategoryType.GURU, name='вибраторы', uniq_name='вибраторы')
        ]

        embs_adult = [0.2] * 50
        cls.saas_ugc.on_request(embs=embs_adult, search_size=100, top_size=50,).respond(
            items=[
                UGCItem(
                    relevance=885100722,
                    attributes={
                        'page_id': '12340001',
                        'title': '7 правил хорошего времяпрепровождения',
                        'description': 'Семь, то есть три плюс четыре',
                        'type': 'blog',
                        'semantic_id': '7-pravil-horoshego-vremyapreprovozhdeniya',
                        'image': Images.IMAGE,
                        'author_uid': '1234567',
                        'author_url': 'instagram.com/rules',
                        'nid': '["3000",]',
                    },
                ),
            ]
        )
        cls.index.dssm.hard2_query_embedding.on(query='вибраторы').set(*embs_adult)

        cls.index.hypertree += [
            HyperCategory(hid=1, output_type=HyperCategoryType.GURU, name='люстры 1', uniq_name='люстры 1'),
        ]

        cls.index.navtree += [
            NavCategory(
                nid=1000,
                is_blue=False,
                name='A',
                children=[
                    NavCategory(hid=1, nid=1001, is_blue=False, name='AA'),
                    NavCategory(hid=100, nid=1100, is_blue=False, name='AB'),
                ],
            ),
            NavCategory(
                nid=2000,
                is_blue=False,
                name='B',
                children=[
                    NavCategory(hid=1, nid=2001, is_blue=False, name='BA'),
                ],
            ),
            NavCategory(hid=6091783, nid=3000, is_blue=False, name='C'),
        ]

        # статьи для проверки фильтрации по nid'ам ------------------
        embs_trimmers = [0.5] * 50
        cls.index.dssm.hard2_query_embedding.on(query='триммеры').set(*embs_trimmers)

        cls.index.navtree += [
            NavCategory(
                hid=40,
                nid=4000,
                is_blue=False,
                name='Газонокосилки и триммеры',
                children=[
                    NavCategory(hid=41, nid=4001, is_blue=False, name='Триммеры'),
                ],
            ),
            NavCategory(hid=50, nid=5000, is_blue=False, name='Мужские электробритвы'),
        ]

        cls.index.hypertree += [
            HyperCategory(
                hid=40,
                output_type=HyperCategoryType.GURU,
                name='газонокосилки и триммеры',
                uniq_name='газонокосилки и триммеры',
            ),
            HyperCategory(hid=41, output_type=HyperCategoryType.GURU, name='триммеры', uniq_name='триммеры'),
            HyperCategory(
                hid=50,
                output_type=HyperCategoryType.GURU,
                name='мужские электробритвы',
                uniq_name='мужские электробритвы',
            ),
        ]

        cls.saas_ugc.on_request(embs=embs_trimmers, search_size=100, top_size=50,).respond(
            items=[
                UGCItem(
                    relevance=887100722,
                    attributes={
                        'page_id': '57000',
                        'title': 'Обзор триммеров для дачного участка',
                        'description': 'Чем лучше косить траву?',
                        'type': 'overview',
                        'semantic_id': 'obzor-trimmerov-dlya-dachnogo-uchastka',
                        'image': Images.IMAGE,
                        'author_uid': '1234567',
                        'author_url': 'instagram.com/rules',
                        'nid': '["9999", "4000",]',  # first one doesn't exist
                    },
                ),
                UGCItem(
                    relevance=886100722,
                    attributes={
                        'page_id': '57001',
                        'title': 'Выбор триммера для разных типов бород',
                        'description': 'Как же выбрать триммер для бороды?',
                        'type': 'choose',
                        'semantic_id': 'vybor-trimmera-dlya-raznih-tipov-borod',
                        'image': Images.IMAGE,
                        'author_uid': '1234567',
                        'author_url': 'instagram.com/rules',
                        'nid': '["5000",]',
                    },
                ),
                UGCItem(
                    relevance=885100722,
                    attributes={
                        'page_id': '57002',
                        'title': 'История триммеров',
                        'description': 'Как люди жили без триммеров?',
                        'type': 'research',
                        'semantic_id': 'istoriya-trimmerov',
                        'image': Images.IMAGE,
                        'author_uid': '1234567',
                        'author_url': 'instagram.com/rules',
                        # no nid
                    },
                ),
            ]
        )
        # ------------------------------------------------------

        # статьи для проверки того, что статьи с типом journal/blog попадают в конец врезки
        # https://st.yandex-team.ru/MARKETOUT-40156
        embs_headphones = [0.6] * 50
        cls.index.dssm.hard2_query_embedding.on(query='наушники').set(*embs_headphones)
        cls.saas_ugc.on_request(embs=embs_headphones, search_size=100, top_size=50,).respond(
            items=[
                UGCItem(
                    relevance=996000000,
                    attributes={
                        'page_id': '57003',
                        'title': 'Топ наушников этого года',
                        'description': 'Протестировали сами все модели',
                        'type': 'overview',
                        'semantic_id': 'top-naushnikov-etogo-goda',
                        'image': Images.IMAGE,
                        'author_uid': '1234567',
                        'author_url': 'instagram.com/rules',
                    },
                ),
                UGCItem(
                    relevance=995100721,
                    attributes={
                        'page_id': '57004',
                        'title': 'Обзор моих любимых наушников',
                        'description': 'Это самые лучшие наушники в мире!',
                        'type': 'blog',
                        'semantic_id': 'obzor-moih-lubimih-naushnikov',
                        'image': Images.IMAGE,
                        'author_uid': '1234567',
                        'author_url': 'instagram.com/rules',
                    },
                ),
                UGCItem(
                    relevance=885100721,
                    attributes={
                        'page_id': '57005',
                        'title': 'История наушников',
                        'description': 'Как люди жили без наушников?',
                        'type': 'research',
                        'semantic_id': 'istoriya-naushnikov',
                        'image': Images.IMAGE,
                        'author_uid': '1234567',
                        'author_url': 'instagram.com/rules',
                    },
                ),
                UGCItem(
                    relevance=884100721,
                    attributes={
                        'page_id': '57006',
                        'title': 'Развёрнутый отзыв на наушники',
                        'description': 'Изучил со всех сторон',
                        'type': 'blog',
                        'semantic_id': 'razvernutiy-otzyv-na-naushniki',
                        'image': Images.IMAGE,
                        'author_uid': '1234567',
                        'author_url': 'instagram.com/rules',
                    },
                ),
            ]
        )
        # ------------------------------------------------------

        embs_all_incuts = [0.3] * 50
        cls.saas_ugc.on_request(embs=embs_all_incuts, search_size=100, top_size=50,).respond(
            items=[
                UGCItem(
                    relevance=988795808,
                    attributes={
                        'page_id': '56126',
                        'compiled_generation': '20200604_0600',
                        'pages_generation': '20200608_0800',
                        'title': 'Как попасть во все врезки',
                        'type': 'knowledge',
                        'semantic_id': 'kak-popast-vezde',
                        'image': Images.IMAGE,
                        'nid': '["1100",]',
                    },
                )
            ]
        )

        cls.index.shops += [
            Shop(fesh=41, domain="best_lyustry.com"),
            Shop(fesh=3476000, cpa=Shop.CPA_NO),
        ]

        cls.index.virtual_models += [
            VirtualModel(virtual_model_id=100001),
            VirtualModel(virtual_model_id=100002),
            VirtualModel(virtual_model_id=100003),
            VirtualModel(virtual_model_id=100004),
            VirtualModel(virtual_model_id=100005),
            VirtualModel(virtual_model_id=100006),
            VirtualModel(virtual_model_id=100007),
            VirtualModel(virtual_model_id=100008),
        ]

        cls.index.offers += [
            Offer(title="люстры 1", ts=101, price=800, hid=1, fesh=41, sku=1),
            Offer(title="люстры 2", ts=102, price=700, hid=1, fesh=41, sku=1),
            Offer(title="люстры 3", ts=103, price=600, hid=1, fesh=41, sku=2),
            Offer(title="люстры 4", ts=104, price=500, hid=1, fesh=41, sku=3),
            Offer(title="люстры 5", ts=105, price=400, hid=1, fesh=41),
            Offer(title="люстры 6", ts=106, price=300, hid=1, fesh=41),
            Offer(title="люстры 7", ts=107, price=200, hid=1, fesh=41),
            Offer(title="люстры 8", ts=108, price=100, hid=1, fesh=41),
            Offer(title="люстры 9", ts=109, price=150, hid=1, fesh=41),
            Offer(title="люстры 10", ts=110, price=150, hid=1, fesh=41),
            Offer(title="люстры 11", ts=111, price=150, hid=1, fesh=41),
            Offer(title="люстры 12", ts=112, price=150, hid=1, fesh=41),
            Offer(title="люстры 13", ts=113, price=150, hid=1, fesh=41),
            Offer(title="люстры 14", ts=114, price=150, hid=1, fesh=41),
            Offer(title="люстры 15", ts=115, price=150, hid=1, fesh=41),
            Offer(title="люстры 16", ts=116, price=150, hid=1, fesh=41),
            Offer(title="люстры 17", ts=117, price=150, hid=1, fesh=41),
            Offer(title="люстры 18", ts=118, price=150, hid=1, fesh=41),
            Offer(title="люстры 19", ts=119, price=150, hid=1, fesh=41),
            Offer(title="люстры 20", ts=120, price=150, hid=1, fesh=41),
            Offer(title="люстры 21", ts=121, price=150, hid=1, fesh=41),
            Offer(title="люстры 22", ts=122, price=150, hid=1, fesh=41),
            Offer(title="люстры 23", ts=123, price=150, hid=1, fesh=41),
            Offer(title="швабры 1001", price=800, hid=42, fesh=347600, virtual_model_id=100001),
            Offer(title="швабры 1002", price=700, hid=42, fesh=347600, virtual_model_id=100002),
            Offer(title="швабры 1003", price=600, hid=42, fesh=347600, virtual_model_id=100003),
            Offer(title="швабры 1004", price=500, hid=42, fesh=347600, virtual_model_id=100004),
            Offer(title="швабры 1005", price=400, hid=42, fesh=347600, virtual_model_id=100005),
            Offer(title="швабры 1006", price=300, hid=42, fesh=347600, virtual_model_id=100006),
            Offer(title="швабры 1007", price=200, hid=42, fesh=347600, virtual_model_id=100007),
            Offer(title="швабры 1008", price=100, hid=42, fesh=347600, virtual_model_id=100008),
            Offer(title="вибраторы", ts=132, price=850, hid=6091783, fesh=41),
            Offer(title="вибраторы", ts=133, price=750, hid=6091783, fesh=41),
            Offer(title="вибраторы", ts=134, price=650, hid=6091783, fesh=41),
            Offer(title="вибраторы", ts=135, price=550, hid=6091783, fesh=41),
            Offer(title="вибраторы", ts=136, price=450, hid=6091783, fesh=41),
            Offer(title="вибраторы", ts=137, price=350, hid=6091783, fesh=41),
            Offer(title="вибраторы", ts=138, price=250, hid=6091783, fesh=41),
            Offer(title="вибраторы", ts=139, price=150, hid=6091783, fesh=41),
        ]

        # 140 - 163
        for i in range(1, 24):
            cls.index.offers.append(Offer(title="триммеры", ts=139 + i, price=i * 50, hid=41, fesh=41))
        # 164 - 171
        for i in range(1, 9):
            cls.index.offers.append(Offer(title="наушники", ts=163 + i, price=i * 100, hid=60, fesh=41))

        cls.index.mskus += [
            MarketSku(hyperid=2, sku=1, title='канделябры и люстры 1', blue_offers=[BlueOffer(ts=1025)], hid=1),
            MarketSku(hyperid=3, sku=2, title='канделябры и люстры 2', blue_offers=[BlueOffer()], hid=1),
            MarketSku(hyperid=4, sku=3, title='канделябры и люстры 3', blue_offers=[BlueOffer()], hid=1),
            MarketSku(hyperid=5, sku=4, title='канделябры и люстры 4', blue_offers=[BlueOffer()], hid=1),
            MarketSku(hyperid=6, sku=5, title='канделябры', blue_offers=[BlueOffer()]),
            MarketSku(hyperid=7, sku=6, title='канделябры', blue_offers=[BlueOffer()]),
            MarketSku(hyperid=8, sku=7, title='канделябры', blue_offers=[BlueOffer()]),
            MarketSku(hyperid=9, sku=8, title='канделябры', blue_offers=[BlueOffer()]),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1025).respond(0.28)
        cls.matrixnet.on_place(MnPlace.META_REARRANGE, 1025).respond(0.28)

        counters = [
            BeruSkuOrderLastTimeCounter(
                sku_order_events=[
                    SkuLastOrderEvent(sku_id=1, timestamp=418419200),
                    SkuLastOrderEvent(sku_id=2, timestamp=488418200),
                    SkuLastOrderEvent(sku_id=3, timestamp=488419100),
                ]
            ),
        ]

        cls.bigb.on_request(yandexuid=26471001, client='merch-machine').respond(counters=counters)
        cls.bigb.on_request(yandexuid=4144501, client='merch-machine').respond()

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 101).respond(1.0)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 102).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 103).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 104).respond(0.7)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 105).respond(0.6)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 106).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 107).respond(0.4)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 108).respond(0.3)

        respval = 0.25
        for ts in range(109, 116):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, ts).respond(respval)
            respval -= 0.01
        for ts in range(116, 124):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, ts).respond(0.1)

    def test_rearr_bundles_for_inclid_merging(self):
        """Проверяем, что учитываются два вхождения market_blender_bundles_for_inclid для одного inclid с разными viewtype"""
        request_params = self.CgiParams(
            {
                "hid": "2",
                "numdoc": "12",
                "show-reviews": "1",
                "additional_entities": "articles",
            }
        )
        rearr_factors = self.RearrFlags(
            {
                "market_blender_media_adv_incut_enabled": 0,
            }
        )

        def get_exp_fragment(pos):
            return {
                "search": {
                    "results": NotEmpty(),
                },
                "incuts": {
                    "results": [
                        {"entity": "searchIncut", "inClid": 1, "position": pos},
                    ]
                },
            }

        rearr_factors["market_blender_bundles_for_inclid"] = ["1:list:bundle_material_4", "1:grid:bundle_material_4"]
        request_params["viewtype"] = "list"
        self.assertFragmentIn(
            self.report.request_json(self.create_blender_request(request_params, rearr_factors)),
            get_exp_fragment(4),
        )
        request_params["viewtype"] = "grid"
        self.assertFragmentIn(
            self.report.request_json(self.create_blender_request(request_params, rearr_factors)),
            get_exp_fragment(10),
        )

    def test_use_blender_place_instead_prime_with_blender_param(self):
        request = "text=люстры 1"

        # Проверяем, что в блендерном ответе есть не пустой incuts
        blender_response = self.report.request_json(request + "&place=blender")
        self.assertFragmentIn(
            blender_response,
            {
                "search": {"results": NotEmpty()},
                "incuts": {"results": NotEmpty()},
            },
        )

        # Проверяем, что place=blender == place=prime&blender=1
        prime_blender_response = self.report.request_json(request + "&place=prime&blender=1")
        self.assertFragmentIn(prime_blender_response, blender_response.root)

        # Проверяем, что в праймовом ответе incuts не добавился
        prime_response = self.report.request_json(request + "&place=prime")
        self.assertFragmentIn(
            prime_response,
            {
                "search": {"results": NotEmpty()},
                "incuts": NoKey("incuts"),
            },
        )

    @skip('MARKETOUT-47278: удаление CPC коллекции')
    def test_log_organic_in_prime(self):
        request_params = self.CgiParams(
            {
                "text": "люстры 1",
                "numdoc": 8,
                "additional_entities": "articles",
                "entities": "offer",
                "viewtype": "grid",
                "columns-in-grid": 3,
            }
        )
        rearr_factors = self.RearrFlags(
            {
                "market_show_log_organic_in_prime": 1,
                "market_blender_media_adv_incut_enabled": 0,
            }
        )
        response = self.report.request_json(self.create_blender_request(request_params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"entity": "offer", "titles": {"raw": "люстры 7"}, "showUid": "04884192001117778888800007"},
                    ]
                },
                "incuts": {
                    "results": [
                        {
                            "inClid": 1,
                        }
                    ]
                },
            },
            allow_different_len=True,
        )

        rearr_factors["market_show_log_organic_in_prime"] = "0"
        response = self.report.request_json(self.create_blender_request(request_params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"entity": "offer", "titles": {"raw": "люстры 7"}, "showUid": "04884192001117778888800007"},
                    ]
                },
                "incuts": {
                    "results": [
                        {
                            "inClid": 1,
                        }
                    ]
                },
            },
            allow_different_len=True,
        )
        self.show_log_tskv.expect(record_type=0, show_uid="04884192001117778888800007", position=7).times(1)
        self.show_log_tskv.expect(record_type=0, show_uid="04884192001117778888800007", position=8).times(1)

        # Проверим, что на второй и последующих страницах на приложениях органика логируется в прайме без флага
        request_params = self.CgiParams(
            {
                "page": 2,
                "hid": "12521",
                "nid": "125210",
                "numdoc": 8,
                "yandexuid": "4144501",
                "client": "ANDROID",
            }
        )
        rearr_factors = self.RearrFlags(
            {
                "market_pin_offers_to_rs": 0,
                "market_blender_bundles_for_inclid": "15:bundle_recom_thematic_9",
                "recom_thematic_incut_enabled": 1,
                "market_blender_media_adv_incut_enabled": 0,
            }
        )
        response = self.report.request_json(self.create_blender_request(request_params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "search": {"results": [{"entity": "product", "showUid": "04884192001117778888816009"}]},
                "incuts": {
                    "results": [
                        {
                            "inClid": 15,
                            "position": 1,
                        }
                    ]
                },
            },
            allow_different_len=True,
        )
        # проверяем, что в логе позиция органики не стала = 2 из-за врезки так, как произошло бы при логировании блендером
        self.show_log_tskv.expect(record_type=1, show_uid="04884192001117778888816009", position=9).once()
        self.show_log_tskv.expect(record_type=1, show_uid="04884192001117778888816009", position=10).times(0)

    @skip('MARKETOUT-47278: удаление CPC коллекции')
    def test_material_entrypoints(self):
        request_params = self.CgiParams(
            {
                "text": "люстры 1",
                "numdoc": 8,
                "additional_entities": "articles",
                "entities": "offer",
                "viewtype": "grid",
                "columns-in-grid": 3,
            }
        )
        rearr_factors = self.RearrFlags(
            {
                "market_blender_write_access_log": 1,
                "market_blender_media_adv_incut_enabled": 0,
            }
        )
        response = self.report.request_json(self.create_blender_request(request_params, rearr_factors))

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"entity": "offer", "showUid": "04884192001117778888800001"},
                        {"entity": "offer", "showUid": "04884192001117778888800002"},
                        {"entity": "offer", "showUid": "04884192001117778888800003"},
                        {"entity": "offer", "showUid": "04884192001117778888800004"},
                        {"entity": "offer", "showUid": "04884192001117778888800005"},
                        {"entity": "offer", "showUid": "04884192001117778888800006"},
                        {"entity": "offer", "showUid": "04884192001117778888800007"},
                        {"entity": "offer", "showUid": "04884192001117778888800008"},
                    ]
                },
                "incuts": {
                    "results": [
                        {
                            "entity": "searchIncut",
                            "position": 7,
                            "showUid": Regex("[0-9]{21}34007"),
                            "inClid": 1,
                            "items": [
                                {
                                    "entity": "materialEntrypoint",
                                    "title": "Как сделать эко-люстру",
                                    "showUid": Regex("[0-9]{21}35001"),
                                },
                                {
                                    "entity": "materialEntrypoint",
                                    "title": "Как выбрать люстру",
                                    "showUid": Regex("[0-9]{21}35002"),
                                },
                                {
                                    "entity": "materialEntrypoint",
                                    "title": "7 правил хорошего освещения",
                                    "showUid": Regex("[0-9]{21}35003"),
                                },
                            ],
                        }
                    ]
                },
            },
            allow_different_len=False,
            preserve_order=True,
        )

        self.assertFragmentNotIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "entity": "materialEntrypoints",
                            "isNewFormat": True,
                        }
                    ]
                }
            },
        )

        incut_parent_show_uid = "04884192001117778888834007"
        self.show_log_tskv.expect(
            show_uid=incut_parent_show_uid, super_uid=incut_parent_show_uid, inclid=1, position=7
        ).times(1)
        for pos in range(1, 4):
            self.show_log_tskv.expect(
                show_uid="04884192001117778888835{:03d}".format(pos),
                inclid=1,
                position=pos,
                super_uid=incut_parent_show_uid,
                incut_position=7,
            ).times(1)

        # с обратным флагом список врезок пустой
        rearr_factors["market_ugc_saas_enabled"] = "0"
        response = self.report.request_json(self.create_blender_request(request_params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [{"entity": "offer"}] * 8,
                },
                "incuts": {"results": []},
            },
            allow_different_len=False,
            preserve_order=True,
        )

        request_params = self.CgiParams(
            {
                "hid": 1,
                "numdoc": 23,
                "additional_entities": "articles",
                "entities": "offer",
                "viewtype": "grid",
                "columns-in-grid": 3,
            }
        )
        rearr_factors["market_ugc_saas_enabled"] = "1"
        response = self.report.request_json(self.create_blender_request(request_params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "entity": "searchIncut",
                            "inClid": 1,
                            "position": 22,
                        }
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )
        self.show_log_tskv.expect(record_type=4, inclid=1, incut_position=7).times(3)
        self.show_log_tskv.expect(record_type=4, inclid=1, incut_position=22).times(3)
        # было 3 запроса, в 2х случаях позиция останется 7 (когда нет врезок и когда врезка на 22 позиции, а в 3 случае на 7 месте будет врезка)
        self.show_log_tskv.expect(record_type=0, show_uid="04884192001117778888800007", position=7).times(2)
        # showUid старый, позиция сдвинулась
        self.show_log_tskv.expect(record_type=0, show_uid="04884192001117778888800007", inclid=0, position=8)
        # у врезки позиция 7, но другой urlId
        self.show_log_tskv.expect(record_type=3, show_uid="04884192001117778888834007", inclid=1, position=7).times(1)
        self.show_log_tskv.expect(record_type=3, show_uid="04884192001117778888834022", inclid=1, position=22).times(1)

        # всего было 8 офферов, но у последнего позиция в клик логе будет 9-я из-за врезки
        self.click_log.expect(clicktype=ClickType.EXTERNAL, position=9)

        self.feature_log.expect(
            inclid=1,
            show_uid="04884192001117778888834007",
            position=7,
            count_incut_items=3,
            other={
                'request_words_count': 2,
                'has_all_words_ex_full_aggr_max': 1,
                'has_all_words_ex_full_aggr_min': 0,
                'has_all_words_ex_full_aggr_mean': Round(0.5),
                'has_all_words_ex_full_aggr_std': Round(0.7071),
                # у это врезки нет органических элементов, поэтому не будет этих факторов
                'total_number_offers_aggr_incut_max': NoKey('total_number_offers_aggr_incut_max'),
                'total_number_offers_aggr_incut_min': NoKey('total_number_offers_aggr_incut_min'),
                'total_number_offers_aggr_incut_mean': NoKey('total_number_offers_aggr_incut_mean'),
                'total_number_offers_aggr_incut_std': NoKey('total_number_offers_aggr_incut_std'),
            },
            document_type=13,
        )

        del request_params["hid"]
        request_params["text"] = "люстры 1"
        request_params["numdoc"] = 6
        response = self.report.request_json(self.create_blender_request(request_params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"entity": "offer", "showUid": "04884192001117778888800001"},
                        {"entity": "offer", "showUid": "04884192001117778888800002"},
                        {"entity": "offer", "showUid": "04884192001117778888800003"},
                        {"entity": "offer", "showUid": "04884192001117778888800004"},
                        {"entity": "offer", "showUid": "04884192001117778888800005"},
                        {"entity": "offer", "showUid": "04884192001117778888800006"},
                    ]
                },
                "incuts": {"results": []},
            },
            allow_different_len=False,
            preserve_order=True,
        )

        default_material_entrypoints_combination = {"1": ["default"]}

        self.access_log.expect(
            blender_incuts_generated=json.dumps(default_material_entrypoints_combination, separators=(',', ':'))
        ).times(3)

    @skip('MARKETOUT-47278: удаление CPC коллекции')
    def test_independance_organic_factors_from_numdoc(self):
        request_params = self.CgiParams(
            {
                "text": "люстры 1",
                "numdoc": 20,
                "additional_entities": "articles",
                "entities": "offer",
                "viewtype": "grid",
                "columns-in-grid": 3,
            }
        )
        rearr_factors = self.RearrFlags(
            {
                "market_blender_media_adv_incut_enabled": 0,
            }
        )

        self.report.request_json(self.create_blender_request(request_params, rearr_factors))
        request_params["numdoc"] = 10
        self.report.request_json(self.create_blender_request(request_params, rearr_factors))

        self.feature_log.expect(
            other={
                'request_words_count': 2,
                'has_all_words_ex_full_aggr_max': 1,
                'has_all_words_ex_full_aggr_min': 0,
                'has_all_words_ex_full_aggr_mean': Round(0.5),
                'has_all_words_ex_full_aggr_std': Round(0.7071),
            }
        ).times(2)

    def test_hid_material_filter(self):
        request_params = self.CgiParams(
            {
                "hid": "41",
                "numdoc": "23",
                "additional_entities": "articles",
                "viewtype": "grid",
                "columns-in-grid": 3,
            }
        )
        rearr_factors = self.RearrFlags(
            {
                "market_filter_material_entrypoints_by_hid": 0,
                "market_blender_media_adv_incut_enabled": 0,
            }
        )
        response = self.report.request_json(self.create_blender_request(request_params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "entity": "searchIncut",
                            "position": 22,
                            "inClid": 1,
                            "items": [
                                {
                                    "entity": "materialEntrypoint",
                                    "title": "Обзор триммеров для дачного участка",
                                },
                                {
                                    "entity": "materialEntrypoint",
                                    "title": "Выбор триммера для разных типов бород",
                                },
                                {
                                    "entity": "materialEntrypoint",
                                    "title": "История триммеров",
                                },
                            ],
                        }
                    ]
                },
            },
            allow_different_len=False,
            preserve_order=True,
        )
        rearr_factors["market_filter_material_entrypoints_by_hid"] = 1
        response = self.report.request_json(self.create_blender_request(request_params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "entity": "searchIncut",
                            "position": 22,
                            "inClid": 1,
                            "items": [
                                {
                                    "entity": "materialEntrypoint",
                                    "title": "Обзор триммеров для дачного участка",
                                },
                            ],
                        }
                    ]
                },
            },
            allow_different_len=False,
            preserve_order=True,
        )
        self.show_log_tskv.expect(record_type=4, inclid=1, incut_position=22).times(4)

    def test_blogs_last_in_incut(self):
        # проверяем, что статьи типа journal/blog попадают в конец врезки, сохраняя при этом относительный порядок
        request_params = self.CgiParams(
            {
                "text": "наушники",
                "numdoc": "8",
                "additional_entities": "articles",
                "viewtype": "grid",
                "columns-in-grid": 3,
            }
        )
        rearr_factors = self.RearrFlags(
            {
                "market_blender_media_adv_incut_enabled": 0,
            }
        )
        response = self.report.request_json(self.create_blender_request(request_params, rearr_factors))

        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "entity": "searchIncut",
                            "position": 7,
                            "inClid": 1,
                            "items": [
                                {
                                    "entity": "materialEntrypoint",
                                    "title": "Топ наушников этого года",
                                },
                                {
                                    "entity": "materialEntrypoint",
                                    "title": "История наушников",
                                },
                                {
                                    "entity": "materialEntrypoint",
                                    "title": "Обзор моих любимых наушников",
                                },
                                {
                                    "entity": "materialEntrypoint",
                                    "title": "Развёрнутый отзыв на наушники",
                                },
                            ],
                        }
                    ]
                },
            },
            allow_different_len=False,
            preserve_order=True,
        )

        self.show_log_tskv.expect(record_type=4, inclid=1, incut_position=7).times(4)

    def test_icookie_for_incuts(self):
        # проверяем что icookie пишется в шоу лог для врезок и элементов
        request_params = self.CgiParams(
            {
                "text": "наушники",
                "numdoc": "8",
                "additional_entities": "articles",
                "x-yandex-icookie": "6774478491508471626",
                "viewtype": "grid",
                "columns-in-grid": 3,
            }
        )
        rearr_factors = self.RearrFlags(
            {
                "market_blender_media_adv_incut_enabled": 0,
            }
        )
        self.report.request_json(self.create_blender_request(request_params, rearr_factors))
        self.show_log_tskv.expect(record_type=3, inclid=1, icookie='6774478491508471626')
        self.show_log_tskv.expect(record_type=4, inclid=1, icookie='6774478491508471626')

    def test_ugc_relevance_threshold_with_hid(self):
        # проверяем market_ugc_saas_relevance_threshold_with_hid
        request_params = self.CgiParams(
            {
                "text": "люстры 1",
                "numdoc": 8,
                "additional_entities": "articles",
                "viewtype": "grid",
                "columns-in-grid": 3,
            }
        )
        rearr_factors = self.RearrFlags(
            {
                "market_ugc_saas_relevance_threshold": "0.90",
                "market_ugc_saas_relevance_threshold_with_hid": "0.80",
                "market_blender_media_adv_incut_enabled": 0,
            }
        )
        response = self.report.request_json(self.create_blender_request(request_params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "entity": "searchIncut",
                            "position": 7,
                            "inClid": 1,
                            "items": [
                                {
                                    "entity": "materialEntrypoint",
                                    "title": "Как сделать эко-люстру",
                                },
                            ],
                        }
                    ]
                },
            },
            allow_different_len=False,
            preserve_order=True,
        )

        request_params["hid"] = "1"
        response = self.report.request_json(self.create_blender_request(request_params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "entity": "searchIncut",
                            "position": 7,
                            "inClid": 1,
                            "items": [
                                {
                                    "entity": "materialEntrypoint",
                                    "title": "Как сделать эко-люстру",
                                },
                                {
                                    "entity": "materialEntrypoint",
                                    "title": "Как выбрать люстру",
                                },
                                {
                                    "entity": "materialEntrypoint",
                                    "title": "7 правил хорошего освещения",
                                },
                            ],
                        }
                    ]
                },
            },
            allow_different_len=False,
            preserve_order=True,
        )

        self.show_log_tskv.expect(record_type=4, inclid=1, incut_position=7).times(4)

    def test_material_entrypoints_max_count(self):
        # проверяем market_material_entrypoints_max_count
        request_params = self.CgiParams(
            {
                "text": "люстры 1",
                "numdoc": 8,
                "additional_entities": "articles",
                "viewtype": "grid",
                "columns-in-grid": 3,
            }
        )
        rearr_factors = self.RearrFlags(
            {
                "market_blender_media_adv_incut_enabled": 0,
            }
        )
        response = self.report.request_json(self.create_blender_request(request_params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "entity": "searchIncut",
                            "position": 7,
                            "inClid": 1,
                            "items": [
                                {
                                    "entity": "materialEntrypoint",
                                    "title": "Как сделать эко-люстру",
                                },
                                {
                                    "entity": "materialEntrypoint",
                                    "title": "Как выбрать люстру",
                                },
                                {
                                    "entity": "materialEntrypoint",
                                    "title": "7 правил хорошего освещения",
                                },
                            ],
                        }
                    ]
                },
            },
            allow_different_len=False,
            preserve_order=True,
        )

        rearr_factors["market_material_entrypoints_max_count"] = 1
        response = self.report.request_json(self.create_blender_request(request_params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "entity": "searchIncut",
                            "position": 7,
                            "inClid": 1,
                            "items": [
                                {
                                    "entity": "materialEntrypoint",
                                    "title": "Как сделать эко-люстру",
                                },
                            ],
                        }
                    ]
                },
            },
            allow_different_len=False,
            preserve_order=True,
        )

        self.show_log_tskv.expect(record_type=4, inclid=1, incut_position=7).times(4)

    def test_adult_material_filter(self):
        request_params = self.CgiParams(
            {
                "text": "вибраторы",
                "numdoc": 8,
                "additional_entities": "articles",
                "viewtype": "grid",
                "columns-in-grid": 3,
            }
        )
        rearr_factors = self.RearrFlags(
            {
                "market_filter_material_entrypoints_adult": 0,
                "market_blender_media_adv_incut_enabled": 0,
            }
        )
        response = self.report.request_json(self.create_blender_request(request_params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "entity": "searchIncut",
                            "position": 7,
                            "inClid": 1,
                            "items": [
                                {
                                    "entity": "materialEntrypoint",
                                    "title": "7 правил хорошего времяпрепровождения",
                                },
                            ],
                        }
                    ]
                },
            },
            allow_different_len=False,
            preserve_order=True,
        )

        rearr_factors["market_filter_material_entrypoints_adult"] = 1
        response = self.report.request_json(self.create_blender_request(request_params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {"results": []},
            },
            allow_different_len=False,
            preserve_order=True,
        )

        self.show_log_tskv.expect(record_type=4, inclid=1, incut_position=7).times(1)

    @classmethod
    def prepare_incut_black_list(cls):
        cls.index.incut_black_list_fb += [IncutBlackListFb(texts=['люстры 6'], inclids=['Materials', 'VendorIncut'])]

    def test_incut_black_list(self):
        request_params = self.CgiParams(
            {
                "text": "люстры 6",
                "numdoc": 8,
                "additional_entities": "articles",
                "viewtype": "grid",
                "columns-in-grid": 3,
            }
        )
        rearr_factors = self.RearrFlags(
            {
                "market_output_advert_request_blacklist_fb": 1,
            }
        )
        response = self.report.request_json(self.create_blender_request(request_params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {"results": EmptyList()},
            },
            allow_different_len=False,
            preserve_order=True,
        )

    @classmethod
    def prepare_incut_adv_black_list(cls):
        # Один запрос можно заблокировать сразу по нескольким инклидам
        cls.index.incut_black_list_fb += [IncutBlackListFb(texts=['iphone 13'], inclids=['PremiumAds', 'VendorIncut'])]
        cls.index.models += [
            Model(hid=333, hyperid=76 + i, title='Смартфон Apple iPhone 13 Модель {}'.format(i), ts=100100 + i)
            for i in range(2, 9)
        ]
        cls.index.shops += [
            Shop(fesh=76 + i, priority_region=213, shop_fee=100, cpa=Shop.CPA_REAL, name='Магазин iPhone {}'.format(i))
            for i in range(2, 9)
        ]
        cls.index.offers += [
            Offer(
                fesh=76 + i,
                hyperid=76 + i,
                hid=333,
                fee=90 + i,
                ts=100100 + i,
                price=100,
                cpa=Offer.CPA_REAL,
                title='Смартфон Apple iPhone 13 {} ГБ RU'.format(i * 100),
            )
            for i in range(2, 9)
        ]
        for i in range(2, 9):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100100 + i).respond(0.04)

    def test_incut_adv_black_list_fb(self):
        """
        работа через обновленный формат блеклистов с flatbuffer
        """
        request_params = self.CgiParams(
            {
                "text": "iPhone 13",
                "additional_entities": "articles",
            }
        )
        rearr_factors = self.RearrFlags(
            {
                "market_output_advert_request_blacklist": 0,
                "market_output_advert_request_blacklist_fb": 1,
                "market_blender_cpa_shop_incut_enabled": 1,
            }
        )
        response = self.report.request_json(self.create_blender_request(request_params, rearr_factors))
        self.assertFragmentNotIn(response, {"incuts": {"results": [{"inClid": 2}]}})

    def test_virtual_model_logs(self):
        request_params = self.CgiParams(
            {
                "text": "швабры",
                "allow-collapsing": "1",
                "additional_entities": "articles",
            }
        )
        rearr_factors = self.RearrFlags(
            {
                "market_cards_everywhere_range": "10000:1000010",
                "market_cards_everywhere_cpa_only": 0,
            }
        )
        response = self.report.request_json(self.create_blender_request(request_params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"entity": "product", "titles": {"raw": "швабры 1001"}},
                        {"entity": "product", "titles": {"raw": "швабры 1002"}},
                        {"entity": "product", "titles": {"raw": "швабры 1003"}},
                        {"entity": "product", "titles": {"raw": "швабры 1004"}},
                        {"entity": "product", "titles": {"raw": "швабры 1005"}},
                        {"entity": "product", "titles": {"raw": "швабры 1006"}},
                        {"entity": "product", "titles": {"raw": "швабры 1007"}},
                        {"entity": "product", "titles": {"raw": "швабры 1008"}},
                    ]
                },
                "incuts": {
                    "results": [
                        {
                            "entity": "searchIncut",
                            "inClid": 1,
                            "position": 3,
                        }
                    ]
                },
            },
            allow_different_len=False,
            preserve_order=False,
        )

        self.show_log_tskv.expect(record_type=1, show_uid="04884192001117778888816003", inclid=0, position=4)
        self.show_log_tskv.expect(record_type=1, show_uid="04884192001117778888816003").times(1)
        self.show_log_tskv.expect(record_type=1, show_uid="04884192001117778888816004", inclid=0, position=5)

    @classmethod
    def prepare_material_entrypoints_position_with_show_reviews(cls):
        embs = [0.35] * 50
        cls.saas_ugc.on_request(embs=embs, search_size=100, top_size=50,).respond(
            items=[
                UGCItem(
                    relevance=900100500,
                    attributes={
                        'page_id': '56222',
                        'compiled_generation': '20200604_0600',
                        'pages_generation': '20200608_0800',
                        'title': 'Статья о ножах',
                        'type': 'knowledge',
                        'semantic_id': 'kak-tochit-nozhi',
                        'image': Images.IMAGE,
                        'nid': '["10001",]',
                    },
                ),
            ]
        )
        cls.index.dssm.hard2_query_embedding.on(query='knife').set(*embs)

        cls.index.navtree += [NavCategory(nid=10001, hid=2)]

        cls.index.hypertree += [
            HyperCategory(hid=2, output_type=HyperCategoryType.GURU, name='knife', uniq_name='knife'),
            HyperCategory(hid=42, output_type=HyperCategoryType.GURU, name='shvabry', uniq_name='shvabry'),
        ]

        for id_ in range(101, 112):
            cls.index.models += [Model(hyperid=id_, title='knife ' + str(id_), hid=2)]
            cls.index.model_reviews_data += [ReviewDataItem(review_id=id_, model_id=id_, most_useful=1)]
            cls.index.model_grade_dispersion_data += [GradeDispersionItem(model_id=id_)]

    def test_material_entrypoints_position_with_show_reviews(self):
        request_params = self.CgiParams(
            {
                "text": "knife",
                "numdoc": 12,
                "show-reviews": 1,
                "additional_entities": "articles",
            }
        )
        response = self.report.request_json(self.create_blender_request(request_params))
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": NotEmpty(),
                },
                "incuts": {
                    "results": [
                        {"entity": "searchIncut", "inClid": 1, "position": 3},
                    ]
                },
            },
        )

        rearr_factors = self.RearrFlags(
            {
                "market_blender_bundles_for_inclid": "1:bundle_material_4",
            }
        )
        response = self.report.request_json(self.create_blender_request(request_params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": NotEmpty(),
                },
                "incuts": {
                    "results": [
                        {"entity": "searchIncut", "inClid": 1, "position": 4},
                    ]
                },
            },
        )

    def test_material_entrypoints_position_with_show_reviews_different_view_types(self):
        # 7 - viewtype missmatch and used from config
        # 10 - viewtype match and used from experiment
        request_params = self.CgiParams(
            {
                "hid": "2",
                "numdoc": "12",
                "show-reviews": "1",
                "additional_entities": "articles",
            }
        )
        rearr_factors = self.RearrFlags(
            {
                "market_blender_media_adv_incut_enabled": 0,
            }
        )

        def get_exp_fragment(pos):
            return {
                "search": {
                    "results": NotEmpty(),
                },
                "incuts": {
                    "results": [
                        {"entity": "searchIncut", "inClid": 1, "position": pos},
                    ]
                },
            }

        empty_result = {
            "search": {
                "results": NotEmpty(),
            },
            "incuts": {
                "results": EmptyList(),
            },
        }
        rearr_factors["market_blender_bundles_for_inclid"] = "1:list:bundle_material_4"
        request_params["viewtype"] = "list"
        self.assertFragmentIn(
            self.report.request_json(self.create_blender_request(request_params, rearr_factors)),
            get_exp_fragment(4),
        )
        rearr_factors["market_blender_bundles_for_inclid"] = "1:grid:bundle_material_4"
        request_params["viewtype"] = "list"
        self.assertFragmentIn(
            self.report.request_json(self.create_blender_request(request_params, rearr_factors)),
            empty_result,
        )
        rearr_factors["market_blender_bundles_for_inclid"] = "1:list:bundle_material_4"
        request_params["viewtype"] = "grid"
        request_params["columns-in-grid"] = 3
        self.assertFragmentIn(
            self.report.request_json(self.create_blender_request(request_params, rearr_factors)),
            empty_result,
        )
        rearr_factors["market_blender_bundles_for_inclid"] = "1:grid:bundle_material_4"
        request_params["viewtype"] = "grid"
        self.assertFragmentIn(
            self.report.request_json(self.create_blender_request(request_params, rearr_factors)),
            get_exp_fragment(10),
        )
        rearr_factors["market_blender_bundles_for_inclid"] = "1:list:bundle_material_4,1:grid:bundle_material_4"
        request_params["viewtype"] = "list"
        self.assertFragmentIn(
            self.report.request_json(self.create_blender_request(request_params, rearr_factors)),
            get_exp_fragment(4),
        )
        rearr_factors["market_blender_bundles_for_inclid"] = "1:list:bundle_material_4,1:grid:bundle_material_4"
        request_params["viewtype"] = "grid"
        self.assertFragmentIn(
            self.report.request_json(self.create_blender_request(request_params, rearr_factors)),
            get_exp_fragment(10),
        )

    @classmethod
    def prepare_redirect(cls):
        cls.index.redirect_whitelist_records += [
            RedirectWhiteListRecord(query='how to choose a mobile phone', url='/articles/kak-vybrat-mobilnyj-telefon'),
        ]
        cls.settings.ignore_qtree_decoding_failed_in_error_log = True

    def test_redirect(self):
        """в случае редиректа блендер должен вести себя так же как прайм
        (просто вернет то, что вернул прайм)
        """
        response = self.report.request_json("place=blender&cvredirect=1&text=how to choose a mobile phone")
        self.assertFragmentIn(response, {"redirect": {"url": Contains("/articles/kak-vybrat-mobilnyj-telefon")}})

    def test_equal_urls(self):
        """
        без врезок прайм и блендер должны выдавать одинаковые урлы
        """
        request = "text=люстры&numdoc=8&viewtype=grid&rearr-factors=market_ugc_saas_enabled=0&columns-in-grid=1"

        self.assertEqualJsonResponses(request + "&place=prime", request + "&place=blender", ignore_incuts=True)

        rearr_factors = 'market_premium_offer_threshold_mult=5000;market_ugc_saas_enabled=0;'
        request = (
            'text=премиальный&use-default-offers=1&allow-collapsing=1&premium-ads=0&rearr-factors=%s' % rearr_factors
        )
        self.assertEqualJsonResponses(request + "&place=prime", request + "&place=blender", ignore_incuts=True)

    def test_equal_err_too_short(self):
        """
        без врезок прайм и блендер должны выдавать одинаковые ошибки на короткие запросы
        """

        """
        однобуквенные запросы (не считая пунктуации) должни выдавать ошибку 400 с кодом TOO_SHORT_REQUEST.
        """
        for q in ['x', '6', '%D0%B8', '%D0%B2', 'F', '%D0%81', '0', '-1', 'v,', '%D0%B2%3A', '%F0%9F%98%82']:
            request = "text={}&numdoc=8&viewtype=grid&rearr-factors=market_ugc_saas_enabled=0"
            prime_response = self.report.request_json(request.format(q) + "&place=prime", strict=False)
            self.error_log.expect(code=3043)
            blender_response = self.report.request_json(request.format(q) + "&place=blender", strict=False)
            self.error_log.expect(code=3043)
            self.assertFragmentIn(blender_response, {"error": {"code": "TOO_SHORT_REQUEST"}})
            self.assertEqual(prime_response.code, blender_response.code)

        """
        двухбуквенные запросы должны завершаться успешно
        """
        for q in [
            'xz',
            '%D0%B0%D0%B1',
            'D%C3%A4',
            '%D0%99%D0%AE',
            '09',
            '%E3%81%BE%E3%81%A0',
            '%F0%9F%98%82%F0%9F%98%82',
        ]:
            request = "text={}&numdoc=8&viewtype=grid&rearr-factors=market_ugc_saas_enabled=0"
            blender_response = self.report.request_json(request.format(q) + "&place=blender", strict=False)
            self.assertEqualJsonResponses(
                request.format(q) + "&place=prime", request.format(q) + "&place=blender", ignore_incuts=True
            )
            self.assertFragmentNotIn(blender_response, {"error": {"code": NotEmpty()}})

    @classmethod
    def prepare_empty_response(cls):
        """
        на запрос есть статьи, но нет органики
        выдача должна быть пустой, ничего не залогируется в шоу лог
        """
        embs_anything = [0.1] * 50

        cls.saas_ugc.on_request(embs=embs_anything, search_size=100, top_size=50,).respond(
            items=[
                UGCItem(
                    relevance=988795808,
                    attributes={
                        'page_id': '56120',
                        'compiled_generation': '20200604_0600',
                        'pages_generation': '20200608_0800',
                        'title': 'все что душе угодно',
                        'type': 'knowledge',
                        'semantic_id': 'anything',
                        'image': Images.IMAGE,
                    },
                )
            ]
        )
        cls.index.dssm.hard2_query_embedding.on(query='что угодно').set(*embs_anything)

    def test_empty_response(self):
        """
        на запросе с пустым ответом все хорошо:
        """
        request = "place=blender&text=что угодно"
        headers = {'X-Market-Req-ID': "x_market_req_id"}

        response = self.report.request_json(request, headers=headers)
        self.assertFragmentIn(
            response,
            {"search": {"results": []}, "incuts": {"results": []}},
            allow_different_len=False,
            preserve_order=False,
        )

        """
        никто не залогируется в шоу лог:
        """
        self.show_log.expect(x_market_req_id="x_market_req_id").times(0)

    @classmethod
    def prepare_nailed_docs(cls):
        cls.index.offers += [
            Offer(title='Empty serp rerequest one', ts=1001),
            Offer(title='Empty serp rerequest two', ts=1002),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1001).respond(0.55)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1002).respond(0.2)

    def test_nailed_docs(self):
        """
        блендер должен так же как и прайм отрабатывать
        """
        request_params = self.CgiParams(
            {
                "text": "empty+serp+rerequest",
            }
        )
        rearr_factors = self.RearrFlags({"market_relevance_formula_threshold": "0.6"})
        response = self.report.request_json(self.create_blender_request(request_params, rearr_factors))
        empty_response = {'search': {'total': 0, 'results': EmptyList()}}

        # При запросе с порогом 0.6 офферы не проходят по порогу
        # Получаем пустой SERP
        self.assertFragmentIn(response, empty_response)

        # С флагом market_rerequest_soft_relevance_threshold=0.2, и clid происходит перезапрос
        # Находятся 2 оффера
        rearr_factors["market_rerequest_soft_relevance_threshold"] = "0.2"
        request_params["clid"] = "545"
        response = self.report.request_json(self.create_blender_request(request_params, rearr_factors))
        self.assertFragmentIn(response, {'search': {'results': ElementCount(2)}})

    @classmethod
    def prepare_ctr_data(cls):
        cls.index.ctr.incuts.zero += [EntityCtr(1, 10, 20)]
        cls.index.ctr.incuts.zero += [EntityCtr(4, 11, 21)]

        cls.index.ctr.incuts.by_incut += [EntityCtr(1001, 10, 20)]
        cls.index.ctr.incuts.by_incut += [EntityCtr(2001, 20, 20)]
        cls.index.ctr.incuts.by_incut += [EntityCtr(2002, 19, 21)]
        cls.index.ctr.incuts.by_incut += [EntityCtr(2005, 18, 22)]

        cls.index.ctr.incuts.by_qlow += [QueryEntityCtr('люстры 1', 1001, 2, 20)]
        cls.index.ctr.incuts.by_qlow += [QueryEntityCtr('люстры 1', 2005, 3, 13)]
        cls.index.ctr.incuts.by_qlow += [QueryEntityCtr('люстры 1', 2006, 4, 10)]
        cls.index.ctr.incuts.by_qlow += [QueryEntityCtr('люстры 1', 10009, 1, 1)]

    def test_ctr(self):
        request_params = self.CgiParams(
            {
                "text": "люстры 1",
                "numdoc": "8",
                "additional_entities": "articles",
                "viewtype": "grid",
                "columns-in-grid": 3,
            }
        )
        rearr_factors = self.RearrFlags(
            {
                "use_offer_type_priority_as_main_factor_in_top": 0,
                "market_premium_ads_gallery_shop_fee_threshold": 0,
                "market_blender_media_adv_incut_enabled": 0,
            }
        )
        self.report.request_json(self.create_blender_request(request_params, rearr_factors))

        self.feature_log.expect(
            inclid=1,
            show_uid="04884192001117778888834007",
            other={
                "incuts_zero_pos_1_1_shows": 20,
                "incuts_zero_pos_1_1_clicks": 10,
                "incuts_zero_pos_1_1_ctr": Round(0.5),
                "incuts_zero_pos_2_2_shows": NoKey('incuts_zero_pos_2_2_shows'),
                "incuts_zero_pos_4_6_shows": 21,
                "incuts_zero_pos_4_6_clicks": 11,
                "incuts_zero_pos_4_6_ctr": Round(0.5238),
                "incuts_by_incut_and_pos_1_1_shows": 20,
                "incuts_by_incut_and_pos_1_1_clicks": 10,
                "incuts_by_incut_and_pos_1_1_ctr": Round(0.5),
                "incuts_qlow_by_incut_and_pos_1_1_shows": 20,
                "incuts_qlow_by_incut_and_pos_1_1_clicks": 2,
                "incuts_qlow_by_incut_and_pos_1_1_ctr": Round(0.1),
            },
            document_type=13,
        )

    @classmethod
    def prepare_speller(cls):
        embs = [0.7] * 50
        cls.index.dssm.hard2_query_embedding.on(query='лбстры 1').set(*embs)

        cls.speller.on_request(text='лбстры 1').respond(
            originalText='л<fix>б</fix>стры 1',
            fixedText='л<fix>ю</fix>стры 1',
            reliability=10000,
        )
        cls.speller.on_default_request().respond()

    def test_speller(self):
        """
        Проверяем, что блендер использует результат опечаточника для
        формирования запросов к статейной врезке
        """
        request_params = self.CgiParams(
            {
                "text": "лбстры 1",
                "numdoc": 8,
                "additional_entities": "articles",
            }
        )
        rearr_factors = self.RearrFlags(
            {
                "use_offer_type_priority_as_main_factor_in_top": 0,
                "market_blender_media_adv_incut_enabled": 0,
            }
        )
        response = self.report.request_json(self.create_blender_request(request_params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'inClid': 1,
                        }
                    ]
                }
            },
        )
        self.show_log_tskv.expect(query_context='люстры 1')

    @classmethod
    def prepare_collapsed_offer(cls):
        cls.index.shops += [
            Shop(fesh=2, priority_region=213, regions=[225], name="Белый магазин"),
        ]

        cls.index.models += [
            Model(hyperid=205),
        ]

        cls.index.offers += [
            Offer(hyperid=205, fesh=2, waremd5='BH8EPLtKmdLQhLUasgaOnA', ts=208, sku=15),
            Offer(
                title='подмышник хозяйственный', hyperid=205, fesh=2, waremd5='KXGI8T3GP_pqjgdd7HfoHQ', ts=209, sku=16
            ),
            # дополнительные документы чтобы забить топ
            Offer(title='подмышник хозяйственный дополнительный', ts=211),
            Offer(title='подмышник хозяйственный запасной', ts=212),
        ]

        cls.index.mskus += [
            MarketSku(
                title='подмышник хозяйственный',
                sku=16,
            ),
            MarketSku(
                hyperid=205,
                sku=17,
                blue_offers=[BlueOffer(price=2100, feedid=11, waremd5='yRgmzyBD4j8r4rkCby6Iuw', ts=210)],
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 211).respond(0.95)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 212).respond(0.95)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 205).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 208).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 209).respond(0.8)

    def test_collapsed_offer(self):
        """
        Проверяем, что если в качестве ДО подставился оффер, из которого модель схлопнулась
        То он будет залогирован в шоу лог (до блендера)
        """
        request_params = self.CgiParams(
            {
                "text": "подмышник+хозяйственный",
                "numdoc": "1",
                "page": "3",
                "use-default-offers": "1",
                "allow-collapsing": "1",
                "show-urls": "productVendorBid%2Cexternal%2Cgeo%2CgeoShipping%2Ccpa%2CshowPhone%2Coffercard%2Ccpa",
                "debug": "da",
            }
        )
        rearr_factors = self.RearrFlags({"market_dynstat_count": "1"})
        response = self.report.request_json(self.create_blender_request(request_params, rearr_factors))

        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        # модель схлопнулась из KXGI8T3GP_pqjgdd7HfoHQ, берётся он,
                        # т.к. у него есть ску, и передан соответствующий флаг
                        {
                            'entity': 'product',
                            'id': 205,
                            'offers': {
                                'items': [
                                    {
                                        'wareId': 'KXGI8T3GP_pqjgdd7HfoHQ',
                                        'marketSku': '16',
                                        'debug': {'docRel': Greater(0)},  # значит оффер из поиска и нагребался пантерой
                                    }
                                ]
                            },
                        }
                    ]
                }
            },
            allow_different_len=False,
        )
        # есть запись для оффера, из которого модель схлопнулась
        self.show_log_tskv.expect(ware_md5='KXGI8T3GP_pqjgdd7HfoHQ', url_type=16)
        self.show_log_tskv.expect(ware_md5='KXGI8T3GP_pqjgdd7HfoHQ', url_type=7)
        self.show_log_tskv.expect(ware_md5='KXGI8T3GP_pqjgdd7HfoHQ', url_type=8)

    @classmethod
    def prepare_enriched_do_in_show_log(cls):
        cls.index.hypertree += [
            HyperCategory(hid=9991, output_type=HyperCategoryType.GURULIGHT),
        ]

        cls.index.experimental_boost_fee_reserve_prices += [ExperimentalBoostFeeReservePrice(9991, 500)]

        cls.index.recommended_fee += [
            RecommendedFee(hyper_id=9991, recommended_bid=0.05),
        ]

        cls.index.shops += [
            Shop(
                fesh=1351,
                datafeed_id=7771,
                priority_region=213,
                regions=[225],
                name="Один 1P поставщик",
                supplier_type=Shop.FIRST_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=135145,
                fulfillment_program=True,
            ),
            Shop(
                fesh=1352,
                datafeed_id=7772,
                priority_region=213,
                regions=[225],
                fulfillment_program=False,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                name="3P поставщик Анатолий",
                warehouse_id=135145,
            ),
        ]

        cls.index.models += [
            Model(hid=9991, ts=501, hyperid=8881, title='IPhone X'),
            Model(hid=9991, ts=502, hyperid=8882, title='Samsung Galaxy S10'),
        ]

        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=135145, home_region=213),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=135145,
                delivery_service_id=135157,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=225)],
            ),
            DynamicDeliveryServiceInfo(id=135157, rating=2),
            DynamicWarehousesPriorityInRegion(region=225, warehouses=[145]),
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=8881,
                title='mobile phone IPhone X',
                hid=9991,
                sku=1351,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(ts=701, price=49000, feedid=7772, fee=200, waremd5='bjbuwNSDSdBXHSBndmDUCA'),
                ],
            ),
            MarketSku(
                hyperid=8882,
                title='mobile phone Samsung Galaxy S10',
                hid=9991,
                sku=1352,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(ts=702, price=50000, feedid=7771, waremd5='ozmCtRBXgUJgvxo4kHPBzg'),
                ],
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 501).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 502).respond(0.5)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 701).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 702).respond(0.5)

    def test_enriched_do_in_show_log(self):
        """
        Проверяем, что в случае обогащения ДО в TryCreateNewDOWithFee() при создании модели
        тут -> https://a.yandex-team.ru/arc/trunk/arcadia/market/report/src/output/result.cpp?blame=true&rev=r8710567#L187
        ДО логируется в show_log
        За основу взят тест https://a.yandex-team.ru/arc/trunk/arcadia/market/report/lite/test_recommended_fee_for_1p_supplier.py
        """
        request_params = self.CgiParams(
            {
                "hid": 9991,
                "debug": "da",
                "numdoc": 20,
                "rids": 213,
                "pp": 7,
                "text": "mobile+phone",
                "use-default-offers": 1,
                "allow-collapsing": 1,
            }
        )
        rearr_factors = self.RearrFlags(
            {
                "market_white_search_auction_cpa_fee": 1,
                "market_white_search_auction_cpa_fee_transfer_fee_do": 1,
                "market_tweak_search_auction_white_cpa_fee_params_desktop": "1.8,0.0015,1",
                "market_report_mimicry_in_serp_pattern": 0,
                "market_blender_media_adv_incut_enabled": 0,
            }
        )
        response = self.report.request_json(self.create_blender_request(request_params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                'search': {
                    "results": [
                        {
                            'entity': 'product',
                            'slug': 'samsung-galaxy-s10',
                            'type': 'model',
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'wareId': "ozmCtRBXgUJgvxo4kHPBzg",
                                        'slug': "mobile-phone-samsung-galaxy-s10",
                                        'debug': {
                                            'sale': {
                                                'shopFee': 500,
                                                'clickPrice': Greater(0),
                                            },
                                        },
                                    }
                                ],
                            },
                        },
                    ]
                }
            },
        )

        self.show_log_tskv.expect(ware_md5='ozmCtRBXgUJgvxo4kHPBzg', url_type=16)
        self.show_log_tskv.expect(ware_md5='ozmCtRBXgUJgvxo4kHPBzg', url_type=6)

    @classmethod
    def prepare_recom_thematic_incut(cls):
        cls.settings.set_default_reqid = False

        cls.index.hypertree += [
            HyperCategory(hid=12521, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=12522, output_type=HyperCategoryType.GURU),
        ]

        cls.index.navtree += [
            NavCategory(hid=12521, nid=125210),
            NavCategory(hid=12522, nid=125220),
        ]

        cls.index.models += [
            Model(
                title='plugin_vue_js',
                hyperid=212474657,
                hid=12521,
            ),
            Model(
                title='plugin_vue_js_next',
                hyperid=212474658,
                hid=12522,
            ),
            Model(
                title='python_plugin',
                hyperid=212474659,
                hid=12522,
            ),
            Model(
                title='plus_plus_plugin',
                hyperid=212474660,
                hid=12522,
            ),
            Model(
                title='java_plugin',
                hyperid=212474661,
                hid=12522,
            ),
            Model(
                title='rust_plugin',
                hyperid=212474662,
                hid=12521,
            ),
            Model(
                title='lisp_plugin',
                hyperid=212474663,
                hid=12521,
            ),
            Model(
                title='scheme_plugin',
                hyperid=212474664,
                hid=12521,
            ),
            Model(
                title='sharp_plugin',
                hyperid=212474665,
                hid=12521,
            ),
            Model(
                title='basic_plugin',
                hyperid=212474666,
                hid=12521,
            ),
            Model(
                title='r_plugin',
                hyperid=212474667,
                hid=12521,
            ),
            Model(
                title='matlab_plugin',
                hyperid=212474668,
                hid=12521,
            ),
            Model(
                title='haskell_plugin',
                hyperid=212474669,
                hid=12521,
            ),
            Model(
                title='node_plugin',
                hyperid=212474670,
                hid=12521,
            ),
        ]

        # market skus
        cls.index.mskus += [
            MarketSku(
                title='plugin_vue_js',
                hyperid=212474657,
                sku=2124746570,
                blue_offers=[
                    BlueOffer(
                        offerid='plugin_vue_js_offer',
                        feedid=90,
                        waremd5=b64url_md5('blue-{}'.format('plugin_vue_js_offer')),
                        price=300,
                    )
                ],
            ),
            MarketSku(
                title='plugin_vue_js_next',
                hyperid=212474658,
                sku=2124746580,
                blue_offers=[
                    BlueOffer(
                        offerid='plugin_vue_js_next_offer',
                        feedid=90,
                        waremd5=b64url_md5('blue-{}'.format('plugin_vue_js_next_offer')),
                        price=1648,
                    )
                ],
            ),
            MarketSku(
                title='python_plugin',
                hyperid=212474659,
                sku=2124746590,
                blue_offers=[
                    BlueOffer(
                        offerid='python_plugin_offer',
                        feedid=90,
                        waremd5=b64url_md5('blue-{}'.format('python_plugin_offer')),
                        price=1648,
                    )
                ],
            ),
            MarketSku(
                title='plus_plus_plugin',
                hyperid=212474660,
                sku=2124746600,
                blue_offers=[
                    BlueOffer(
                        offerid='plus_plus_plugin_offer',
                        feedid=90,
                        waremd5=b64url_md5('blue-{}'.format('plus_plus_plugin_offer')),
                        price=1648,
                    )
                ],
            ),
            MarketSku(
                title='java_plugin',
                hyperid=212474661,
                sku=2124746610,
                blue_offers=[
                    BlueOffer(
                        offerid='java_plugin_offer',
                        feedid=90,
                        waremd5=b64url_md5('blue-{}'.format('java_plugin_offer')),
                        price=1648,
                    )
                ],
            ),
        ]

        cls.bigb.on_request(yandexuid=4144502, client='merch-machine').respond()
        cls.dj.on_request(
            yandexuid='4144502',
            exp='recom_thematic_product_incut',
            djid='recom_thematic_product_incut',
            query_nid=125210,
        ).respond(
            title='Тематическая рекомендательная врезка',
            nid='33333333',
            url='/catalog/33333333/list',
            models=[
                DjModel(id='212474657', title='plugin_vue_js'),
                DjModel(id='212474658', title='plugin_vue_js_next'),
            ],
        )

        cls.dj.on_request(
            yandexuid='4144501',
            exp='recom_thematic_product_incut',
            djid='recom_thematic_product_incut',
            query_nid=125210,
        ).respond(
            title='Тематическая рекомендательная врезка',
            nid='33333333',
            url='/catalog/33333333/list',
            models=[
                DjModel(id='212474657', title='plugin_vue_js'),
                DjModel(id='212474658', title='plugin_vue_js_next'),
                DjModel(id='212474659', title='python_plugin'),
                DjModel(id='212474660', title='plus_plus_plugin'),
                DjModel(id='212474661', title='java_plugin'),
            ],
        )

    def _check_recom_landing_url(self, actual_url, expected_url):
        actual_url = urlparse.urlparse(actual_url)
        expected_url = urlparse.urlparse(expected_url)

        self.assertEqual(actual_url.path, expected_url.path)

        self.assertEqual(
            urlparse.parse_qs(actual_url.query),
            urlparse.parse_qs(expected_url.query),
        )

    def _generate_report_state(self, models):
        rs = ReportState.create()
        for m in models:
            doc = rs.search_state.nailed_docs.add()
            doc.model_id = str(m)
        rs.search_state.nailed_docs_from_recom_morda = True
        return ReportState.serialize(rs).replace('=', ',')

    def test_recom_thematic_incut(self):
        """
        Тестируем ответ рекомендательной тематической товарной врезки
        """

        request_params = self.CgiParams(
            {"hid": 12521, "nid": 125210, "numdoc": 15, "yandexuid": "4144501", "client": "ANDROID"}
        )
        rearr_factors = self.RearrFlags(
            {
                "recom_thematic_incut_enabled": 1,
                "market_pin_offers_to_rs": 0,
                "market_blender_media_adv_incut_enabled": 0,
            }
        )
        response = self.report.request_json(
            self.create_blender_request(
                request_params, rearr_factors, get_supported_incuts_cgi({1: range(1, 8), 2: range(1, 8)})
            )
        )
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "entity": "searchIncut",
                            "showUid": Regex("[0-9]{26}"),
                            "inClid": 15,
                            "position": 2,
                            "title": "Тематическая рекомендательная врезка",
                            # TODO: metaplace->djid
                            "dj-meta-place": "recom_thematic_product_incut",
                            "dj-place": "recom_thematic_product_incut",
                            "incutId": "recom_thematic_product_incut_id_1",
                            "link": {
                                "url": NotEmpty(),
                            },
                            "items": [
                                {
                                    "entity": "product",
                                    "id": 212474657,
                                    "titles": {"raw": "plugin_vue_js"},
                                    "showUid": Regex("[0-9]{26}"),
                                },
                                {
                                    "entity": "product",
                                    "id": 212474658,
                                    "titles": {"raw": "plugin_vue_js_next"},
                                    "showUid": Regex("[0-9]{26}"),
                                },
                                {
                                    "entity": "product",
                                    "id": 212474659,
                                    "titles": {"raw": "python_plugin"},
                                    "showUid": Regex("[0-9]{26}"),
                                },
                                {
                                    "entity": "product",
                                    "id": 212474660,
                                    "titles": {"raw": "plus_plus_plugin"},
                                    "showUid": Regex("[0-9]{26}"),
                                },
                                {
                                    "entity": "product",
                                    "id": 212474661,
                                    "titles": {"raw": "java_plugin"},
                                    "showUid": Regex("[0-9]{26}"),
                                },
                            ],
                        }
                    ]
                },
            },
            allow_different_len=False,
            preserve_order=True,
        )
        self._check_recom_landing_url(
            response["incuts"]["results"][0]["link"]["url"],
            '{url}&rs={rs}'.format(
                url="/catalog/33333333/list?tl=1",
                rs=self._generate_report_state([212474657, 212474658, 212474659, 212474660, 212474661]),
            ),
        )

        self.show_log_tskv.expect(
            inclid=15,
            pp=1792,
            hyper_id=212474657,
            hyper_cat_id=12521,
            position=1,
            show_uid='04884192001117778888816001',
            super_uid='04884192001117778888857002',
        ).times(1)
        self.show_log_tskv.expect(
            inclid=15,
            pp=1792,
            hyper_id=212474658,
            hyper_cat_id=12522,
            position=2,
            show_uid='04884192001117778888816002',
            super_uid='04884192001117778888857002',
        ).times(1)
        self.show_log_tskv.expect(
            inclid=15,
            pp=1792,
            hyper_id=212474659,
            hyper_cat_id=12522,
            position=3,
            show_uid='04884192001117778888816003',
            super_uid='04884192001117778888857002',
        ).times(1)
        self.show_log_tskv.expect(
            inclid=15,
            pp=1792,
            hyper_id=212474660,
            hyper_cat_id=12522,
            position=4,
            show_uid='04884192001117778888816004',
            super_uid='04884192001117778888857002',
        ).times(1)
        self.show_log_tskv.expect(
            inclid=15,
            pp=1792,
            hyper_id=212474661,
            hyper_cat_id=12522,
            position=5,
            show_uid='04884192001117778888816005',
            super_uid='04884192001117778888857002',
        ).times(1)
        self.show_log_tskv.expect(inclid=15, pp=18, url_type=57, show_uid='04884192001117778888857002').once()

    def test_recom_thematic_incut_not_enough_models(self):
        """
        Если товаров мало для тематической врезки - не показываем её
        """
        request_params = self.CgiParams(
            {"hid": 12521, "nid": 125210, "numdoc": 15, "yandexuid": "4144502", "client": "ANDROID"}
        )
        rearr_factors = self.RearrFlags(
            {
                "recom_thematic_incut_enabled": 1,
                "market_pin_offers_to_rs": 0,
                "market_blender_media_adv_incut_enabled": 0,
            }
        )
        response = self.report.request_json(
            self.create_blender_request(
                request_params, rearr_factors, get_supported_incuts_cgi({1: range(1, 8), 2: range(1, 8)})
            )
        )
        self.assertFragmentIn(response, {"incuts": Absent()})
        self.show_log_tskv.expect(inclid=15, pp=18, url_type=57).times(0)

    @classmethod
    def prepare_categorical_incut(cls):
        cls.settings.set_default_reqid = False

        cls.index.hypertree += [
            HyperCategory(hid=7000, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=7001, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=7002, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=7003, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=7004, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=7005, output_type=HyperCategoryType.GURU),
        ]

        cls.index.navtree += [
            NavCategory(hid=7000, nid=70000),
            NavCategory(hid=7001, nid=70010),
            NavCategory(hid=7002, nid=70020),
            NavCategory(hid=7003, nid=70030),
            NavCategory(hid=7004, nid=70040),
            NavCategory(hid=7005, nid=70050),
        ]

        cls.index.models += [
            Model(
                title='врезка за 100',
                hyperid=2620030,
                hid=7000,
            ),
            Model(
                title='врезка за 200',
                hyperid=2620031,
                hid=7001,
            ),
            Model(
                title='врезка за 300',
                hyperid=2620032,
                hid=7002,
            ),
            Model(
                title='врезка за 400',
                hyperid=2620033,
                hid=7003,
            ),
            Model(
                title='врезка за 500',
                hyperid=2620034,
                hid=7004,
            ),
            Model(
                title='врезка за 600',
                hyperid=2620035,
                hid=7005,
            ),
        ]

        # market skus
        cls.index.mskus += [
            MarketSku(
                title='врезка за 100',
                hyperid=2620030,
                sku=26200300,
                blue_offers=[
                    BlueOffer(
                        offerid='incut_for_100_offer',
                        feedid=90,
                        waremd5=b64url_md5('blue-{}'.format('incut_for_100_offer')),
                        price=100,
                    )
                ],
            ),
            MarketSku(
                title='врезка за 200',
                hyperid=2620031,
                sku=26200301,
                blue_offers=[
                    BlueOffer(
                        offerid='incut_for_200_offer',
                        feedid=90,
                        waremd5=b64url_md5('blue-{}'.format('incut_for_200_offer')),
                        price=200,
                    )
                ],
            ),
            MarketSku(
                title='врезка за 300',
                hyperid=2620032,
                sku=26200302,
                blue_offers=[
                    BlueOffer(
                        offerid='incut_for_300_offer',
                        feedid=90,
                        waremd5=b64url_md5('blue-{}'.format('incut_for_300_offer')),
                        price=300,
                    )
                ],
            ),
            MarketSku(
                title='врезка за 400',
                hyperid=2620033,
                sku=26200303,
                blue_offers=[
                    BlueOffer(
                        offerid='incut_for_400_offer',
                        feedid=90,
                        waremd5=b64url_md5('blue-{}'.format('incut_for_400_offer')),
                        price=400,
                    )
                ],
            ),
            MarketSku(
                title='врезка за 500',
                hyperid=2620034,
                sku=26200304,
                blue_offers=[
                    BlueOffer(
                        offerid='incut_for_500_offer',
                        feedid=90,
                        waremd5=b64url_md5('blue-{}'.format('incut_for_500_offer')),
                        price=500,
                    )
                ],
            ),
            MarketSku(
                title='врезка за 600',
                hyperid=2620035,
                sku=26200305,
                blue_offers=[
                    BlueOffer(
                        offerid='incut_for_600_offer',
                        feedid=90,
                        waremd5=b64url_md5('blue-{}'.format('incut_for_600_offer')),
                        price=600,
                    )
                ],
            ),
        ]

        cls.bigb.on_request(yandexuid=4144503, client='merch-machine').respond()
        cls.dj.on_request(
            yandexuid='4144503',
            exp='recom_thematic_category_incut',
            djid='recom_thematic_category_incut',
            query_nid=125220,
        ).respond(
            title='Категорийная врезка',
            nid='33333333',
            url='/catalog/33333333/list',
            models=[
                DjModel(id='2620030', title='врезка за 100', attributes={'title': 'X за 100', 'nid': '70000'}),
                DjModel(id='2620031', title='врезка за 200', attributes={'title': 'X за 200', 'nid': '70010'}),
                DjModel(id='2620032', title='врезка за 300', attributes={'title': 'X за 300', 'nid': '70020'}),
                DjModel(id='2620033', title='врезка за 400', attributes={'title': 'X за 400', 'nid': '70030'}),
                DjModel(id='2620034', title='врезка за 500', attributes={'title': 'X за 500', 'nid': '70040'}),
                DjModel(id='2620035', title='врезка за 600', attributes={'title': 'X за 600', 'nid': '70050'}),
            ],
        )

    def test_recom_thematic_category_incut(self):
        """
        Проверяем отображение категорийной врезки вместо товарной при наличии флага recom_thematic_incut_category_view
        """
        request_params = self.CgiParams(
            {"hid": "12522", "nid": "125220", "numdoc": 6, "yandexuid": "4144503", "client": "ANDROID"}
        )
        rearr_factors = self.RearrFlags(
            {
                "market_pin_offers_to_rs": 0,
                "recom_thematic_incut_enabled": 1,
                "recom_thematic_incut_category_view": 1,
                "market_blender_media_adv_incut_enabled": 0,
            }
        )
        response = self.report.request_json(
            self.create_blender_request(
                request_params, rearr_factors, get_supported_incuts_cgi({1: range(1, 8), 2: range(1, 8)})
            )
        )

        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "entity": "searchIncut",
                            "showUid": Regex("[0-9]{26}"),
                            "inClid": 15,
                            "position": 2,
                            "title": "Категорийная врезка",
                            # TODO: metaplace->djid
                            "dj-meta-place": "recom_thematic_category_incut",
                            "dj-place": "recom_thematic_category_incut",
                            "incutId": "recom_thematic_category_incut_id_1",
                            "link": {
                                "url": NotEmpty(),
                            },
                            "items": [
                                {
                                    "entity": "formula",
                                    "id": NotEmpty(),
                                    "name": "X за 100",
                                    "pictures": NotEmpty(),
                                    "picture-source": "model",
                                    "link": {
                                        "url": NotEmpty(),
                                        "urlEndpoint": "categories",
                                        "params": {
                                            "rs": self._generate_report_state([2620030]),
                                            "nid": 70000,
                                            "models": "2620030",
                                            "tcl": 1,
                                        },
                                    },
                                },
                                {
                                    "entity": "formula",
                                    "id": NotEmpty(),
                                    "name": "X за 200",
                                    "pictures": NotEmpty(),
                                    "picture-source": "model",
                                    "link": {
                                        "url": NotEmpty(),
                                        "urlEndpoint": "categories",
                                        "params": {
                                            "rs": self._generate_report_state([2620031]),
                                            "nid": 70010,
                                            "models": "2620031",
                                            "tcl": 1,
                                        },
                                    },
                                },
                                {
                                    "entity": "formula",
                                    "id": NotEmpty(),
                                    "name": "X за 300",
                                    "pictures": NotEmpty(),
                                    "picture-source": "model",
                                    "link": {
                                        "url": NotEmpty(),
                                        "urlEndpoint": "categories",
                                        "params": {
                                            "rs": self._generate_report_state([2620032]),
                                            "nid": 70020,
                                            "models": "2620032",
                                            "tcl": 1,
                                        },
                                    },
                                },
                                {
                                    "entity": "formula",
                                    "id": NotEmpty(),
                                    "name": "X за 400",
                                    "pictures": NotEmpty(),
                                    "picture-source": "model",
                                    "link": {
                                        "url": NotEmpty(),
                                        "urlEndpoint": "categories",
                                        "params": {
                                            "rs": self._generate_report_state([2620033]),
                                            "nid": 70030,
                                            "models": "2620033",
                                            "tcl": 1,
                                        },
                                    },
                                },
                                {
                                    "entity": "formula",
                                    "id": NotEmpty(),
                                    "name": "X за 500",
                                    "pictures": NotEmpty(),
                                    "picture-source": "model",
                                    "link": {
                                        "url": NotEmpty(),
                                        "urlEndpoint": "categories",
                                        "params": {
                                            "rs": self._generate_report_state([2620034]),
                                            "nid": 70040,
                                            "models": "2620034",
                                            "tcl": 1,
                                        },
                                    },
                                },
                                {
                                    "entity": "formula",
                                    "id": NotEmpty(),
                                    "name": "X за 600",
                                    "pictures": NotEmpty(),
                                    "picture-source": "model",
                                    "link": {
                                        "url": NotEmpty(),
                                        "urlEndpoint": "categories",
                                        "params": {
                                            "rs": self._generate_report_state([2620035]),
                                            "nid": 70050,
                                            "models": "2620035",
                                            "tcl": 1,
                                        },
                                    },
                                },
                            ],
                        }
                    ]
                },
            },
            allow_different_len=False,
            preserve_order=True,
        )

        self._check_recom_landing_url(
            response["incuts"]["results"][0]["link"]["url"],
            '{url}&rs={rs}'.format(
                url="/catalog/33333333/list?tl=1",
                rs=self._generate_report_state(
                    [
                        2620030,
                        2620031,
                        2620032,
                        2620033,
                        2620034,
                        2620035,
                    ]
                ),
            ),
        )

        self.show_log_tskv.expect(
            inclid=15,
            pp=1795,
            hyper_id=2620030,
            position=1,
            url_type=59,
            show_uid='04884192001117778888859001',
            super_uid='04884192001117778888857002',
        ).times(1)
        self.show_log_tskv.expect(
            inclid=15,
            pp=1795,
            hyper_id=2620031,
            position=2,
            url_type=59,
            show_uid='04884192001117778888859002',
            super_uid='04884192001117778888857002',
        ).times(1)
        self.show_log_tskv.expect(
            inclid=15,
            pp=1795,
            hyper_id=2620032,
            position=3,
            url_type=59,
            show_uid='04884192001117778888859003',
            super_uid='04884192001117778888857002',
        ).times(1)
        self.show_log_tskv.expect(
            inclid=15,
            pp=1795,
            hyper_id=2620033,
            position=4,
            url_type=59,
            show_uid='04884192001117778888859004',
            super_uid='04884192001117778888857002',
        ).times(1)
        self.show_log_tskv.expect(
            inclid=15,
            pp=1795,
            hyper_id=2620034,
            position=5,
            url_type=59,
            show_uid='04884192001117778888859005',
            super_uid='04884192001117778888857002',
        ).times(1)
        self.show_log_tskv.expect(
            inclid=15,
            pp=1795,
            hyper_id=2620035,
            position=6,
            url_type=59,
            show_uid='04884192001117778888859006',
            super_uid='04884192001117778888857002',
        ).times(1)
        self.show_log_tskv.expect(inclid=15, pp=18, url_type=57, show_uid='04884192001117778888857002').once()

    @classmethod
    def prepare_advertising_carousel(cls):
        COUNT = 5

        for i in range(1, COUNT + 1):
            cls.index.shops += [
                Shop(
                    fesh=i + 9999,
                    priority_region=213,
                    cpa=Shop.CPA_REAL,
                    name='Advertising{}'.format(string.ascii_uppercase[i]),
                    new_shop_rating=NewShopRating(new_rating_total=i),
                ),
            ]
            cls.index.models += [
                Model(
                    ts=9999 + i,
                    title='AdvertisingModelKnife{}'.format(string.ascii_uppercase[i]),
                    hid=9999,
                    hyperid=9999 + i,
                    opinion=Opinion(rating=3.8 + i * 0.3, rating_count=25, total_count=150),
                ),
            ]
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 9999 + i).respond(0.3 + (i % 20) / 20.0)
            cls.index.offers += [
                Offer(
                    hyperid=9999 + i,
                    fesh=i + 9999,
                    vbid=20 + i,
                    price=135 + i * 5,
                    fee=30 + i * 2,
                    title='AdvertisingOffer{}'.format(string.ascii_uppercase[i]),
                    cpa=Offer.CPA_REAL,
                ),
            ]

    def test_blender_advertising_carousel(self):
        request_params = self.CgiParams(
            {
                "hid": "9999",
            }
        )
        rearr_factors = self.RearrFlags(
            {
                "market_blender_advertising_carousel_enabled": 1,
                "market_blender_media_adv_incut_enabled": 0,
            }
        )
        response = self.report.request_json(self.create_blender_request(request_params, rearr_factors))

        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "entity": "searchIncut",
                            "position": 1,
                            "inClid": 14,
                            "items": [
                                {
                                    "entity": "product",
                                    "offers": {
                                        "count": 1,
                                    },
                                },
                            ],
                        },
                    ]
                },
            },
            allow_different_len=True,
        )

    def test_access_log_viewtype(self):
        request_params = self.CgiParams(
            {
                "hid": "9999",
            }
        )
        rearr_factors = self.RearrFlags(
            {
                "market_blender_media_adv_incut_enabled": 0,
            }
        )
        self.report.request_json(self.create_blender_request(request_params, rearr_factors))
        self.access_log.expect(viewtype="list")

    def test_show_log_row_position(self):
        request_params = self.CgiParams(
            {
                "place": "blender",
                "text": "люстры 1",
                "numdoc": 8,
                "viewtype": "grid",
                "columns-in-grid": 3,
                "additional_entities": "articles",
                "supported-incuts": "trash",
                "entities": "offer",
            }
        )
        rearr_factors = self.RearrFlags(
            {
                "market_blender_media_adv_incut_enabled": 0,
            }
        )
        response = self.report.request_json(self.create_blender_request(request_params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "entity": "searchIncut",
                            "position": 7,
                            "inClid": 1,
                        }
                    ]
                },
            },
            allow_different_len=False,
            preserve_order=True,
        )
        self.show_log_tskv.expect(
            inclid=1,
            position=7,
            organic_row=3,
        ).once()

    @classmethod
    def prepare_eats_retail_shops(cls):
        eda_fesh = [10011, 10012, 10013]
        eda_feed_id = [10011, 10012, 10013]
        eda_business_id = [100, 101, 102]

        schedule_data = [
            DynamicTimeIntervalsSet(key=1, intervals=[TimeIntervalInfo(TimeInfo(9, 0), TimeInfo(19, 0))]),
            DynamicTimeIntervalsSet(
                key=3,
                intervals=[
                    TimeIntervalInfo(TimeInfo(6, 0), TimeInfo(12, 0)),
                ],
            ),
            DynamicTimeIntervalsSet(
                key=6,
                intervals=[
                    TimeIntervalInfo(TimeInfo(12, 0), TimeInfo(14, 30)),
                ],
            ),
        ]

        warehouse_with_schedule_id = eda_feed_id[0]
        cls.index.express_warehouses.add(warehouse_with_schedule_id, region_id=213, work_schedule=schedule_data)

        for i in range(len(eda_fesh)):
            cls.index.shops += [
                Shop(
                    fesh=eda_fesh[i],
                    datafeed_id=eda_feed_id[i],
                    business_fesh=eda_business_id[i],
                    business_name="Business EDA {}".format(i),
                    warehouse_id=eda_feed_id[i],
                    cpa=Shop.CPA_REAL,
                    is_eats=True,
                ),
            ]

            cls.index.offers += [
                Offer(
                    offerid='retail_offer_1_shop_{}'.format(i),
                    waremd5='RetailOfferWaremd5___{}'.format(i),
                    title='Retail offer',
                    fesh=eda_fesh[i],
                    feedid=eda_feed_id[i],
                    is_eda_retail=True,
                    hid=91307,
                ),
            ]

        cls.index.business_logos.set_businnes_info(
            id=eda_business_id[0],
            logos_info=LogosInfo(brand_color="#FACADE", shop_group="micro").add_logo_url(
                logo_type='SQUARE',
                url='//avatars.ru/get-our-namespace/55555/hahaha123456/small',
                img_width=100,
                img_height=100,
            ),
        )

    def test_blender_eats_retail_shops(self):
        warehousesEncoder = (
            EatsWarehousesEncoder()
            .add_warehouse(wh_id=10011, wh_priority=1, business_id=100, delivery_time_minutes=42, available_in_hours=10)
            .add_warehouse(wh_id=10012, wh_priority=3, business_id=101, delivery_time_minutes=42, available_in_hours=10)
            .add_warehouse(wh_id=10013, wh_priority=2, business_id=102)
        )
        warehouses = warehousesEncoder.encode()

        response = self.report.request_json(
            'place=blender&text=retail&nid={}'
            '&enable-foodtech-offers=1&'
            'eats-warehouses-compressed={}&supported-incuts=+{}'.format(
                Const.ROOT_NID, warehouses, get_supported_incuts_cgi()
            )
        )

        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "entity": "searchIncut",
                            "inClid": 17,
                            "incutId": "default",
                            "isNewFormat": True,
                            "items": [
                                {
                                    "availableInHours": 10,
                                    "brandColor": "#FACADE",
                                    "businessId": 100,
                                    "businessName": "Business EDA 0",
                                    "deliveryTimeMinutes": 42,
                                    "entity": "shopInShopEntrypoint",
                                    "logos": [
                                        {
                                            "entity": "picture",
                                            "logoType": "SQUARE",
                                            "original": {
                                                "groupId": 55555,
                                                "height": 100,
                                                "key": "hahaha123456",
                                                "namespace": "our-namespace",
                                                "width": 100,
                                            },
                                        }
                                    ],
                                    "offersCount": 1,
                                    "slug": "business-eda-0",
                                    "showUid": "04884192001117778888865001",
                                    "shopGroup": "micro",
                                    "workScheduleList": [
                                        {"day": 0, "from": {"hour": 9, "minute": 0}, "to": {"hour": 19, "minute": 0}},
                                        {"day": 2, "from": {"hour": 6, "minute": 0}, "to": {"hour": 12, "minute": 0}},
                                        {"day": 5, "from": {"hour": 12, "minute": 0}, "to": {"hour": 14, "minute": 30}},
                                    ],
                                },
                                {
                                    "businessId": 102,
                                    "showUid": "04884192001117778888865002",
                                },
                                {
                                    "businessId": 101,
                                    "showUid": "04884192001117778888865003",
                                },
                            ],
                            "placeId": 1,
                            "position": 1,
                            "showUid": "04884192001117778888860001",
                            "title": "Доставим из магазинов",
                            "typeId": 1,
                        },
                    ],
                },
            },
        )

        self.show_log_tskv.expect(record_type=3, show_uid="04884192001117778888860001", position=1).once()
        self.show_log_tskv.expect(record_type=8, show_uid="04884192001117778888865001").once()
        self.show_log_tskv.expect(record_type=8, show_uid="04884192001117778888865002").once()
        self.show_log_tskv.expect(record_type=8, show_uid="04884192001117778888865003").once()

        # если магазинов меньше трех - не показываем врезку
        response = self.report.request_json(
            'place=blender&text=retail&nid={}'
            '&enable-foodtech-offers=1&'
            'shop-in-shop-top-count=2&'
            'eats-warehouses-compressed={}&supported-incuts=+{}'.format(
                Const.ROOT_NID, warehouses, get_supported_incuts_cgi()
            )
        )

        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": EmptyList(),
                },
            },
            allow_different_len=False,
        )

    def test_text_search(self):
        """
        Проверяем отсутствие врезки shopInShop при текстовом поиске.
        Текстовым поиском считается поиск без навигационной категории.
        """
        warehousesEncoder = (
            EatsWarehousesEncoder()
            .add_warehouse(wh_id=10011, wh_priority=4, business_id=100, delivery_time_minutes=42, available_in_hours=10)
            .add_warehouse(wh_id=10012, wh_priority=2, business_id=101, delivery_time_minutes=42, available_in_hours=10)
            .add_warehouse(wh_id=10013, wh_priority=3, business_id=102)
        )
        warehouses = warehousesEncoder.encode()

        response = self.report.request_json(
            'place=blender&text=retail'
            '&enable-foodtech-offers=1&'
            'eats-warehouses-compressed={}&supported-incuts=+{}'.format(warehouses, get_supported_incuts_cgi())
        )

        self.assertFragmentIn(
            response,
            {
                "search": NotEmpty(),
                "incuts": {
                    "results": EmptyList(),
                },
            },
        )

    @classmethod
    def prepare_first_party_fashion_incut(cls):
        count_shops = 10
        cls.index.shops += [
            Shop(
                fesh=47193000,
                supplier_type=Shop.THIRD_PARTY,
                is_supplier=True,
                priority_region=213,
                regions=[213],
                warehouse_id=1,
                blue=Shop.BLUE_REAL,
                datafeed_id=47193000,
            ),
        ]
        cls.index.shops += [
            Shop(
                fesh=47193000 + i * 100,
                name="first_party_shop_{}".format(i),
                supplier_type=Shop.FIRST_PARTY,
                is_supplier=True,
                priority_region=213,
                regions=[213],
                warehouse_id=1,
                blue=Shop.BLUE_REAL,
                datafeed_id=47193000 + i * 100,
            )
            for i in range(1, count_shops)
        ]
        cls.index.hypertree += [HyperCategory(hid=CLOTHES_CATEG_ID)]
        cls.index.models += [Model(hyperid=4719300, ts=4719300, title='third_party_model', hid=CLOTHES_CATEG_ID)]
        cls.index.models += [
            Model(hyperid=4719300 + i, ts=4719300 + i, title='first_party_model_{}'.format(i), hid=CLOTHES_CATEG_ID)
            for i in range(1, count_shops)
        ]
        cls.index.mskus += [
            MarketSku(
                sku=471930,
                title="third_party_sku",
                hid=CLOTHES_CATEG_ID,
                blue_offers=[
                    BlueOffer(
                        fesh=47193000,
                        price=1,
                        title="third_party_offer",
                        feedid=47193000,
                        waremd5='sku-00-blue-1-poq57hRg',
                        hyperid=4719300,
                    ),
                ],
                delivery_buckets=[1234],
            ),
        ]
        cls.index.mskus += [
            MarketSku(
                sku=471930 + i,
                title="first_party_sku_{}".format(i),
                hid=CLOTHES_CATEG_ID,
                blue_offers=[
                    BlueOffer(
                        fesh=47193000 + i * 100,
                        price=1,
                        title="first_party_offer_{}".format(i),
                        feedid=47193000 + i * 100,
                        waremd5='sku-{:02d}-blue-1-poq57hRg'.format(i),
                        hyperid=4719300 + i,
                    ),
                ],
                delivery_buckets=[1234],
            )
            for i in range(1, count_shops / 2)
        ]
        cls.index.mskus += [
            MarketSku(
                sku=471930 + i,
                title="mixed_party_sku_{}".format(i),
                hid=CLOTHES_CATEG_ID,
                blue_offers=[
                    BlueOffer(
                        fesh=47193000,
                        price=1,
                        title="third_party_offer_{}".format(i - count_shops / 2 + 1),
                        feedid=47193000,
                        waremd5='sku-{:02d}-blue-3-poq57hRg'.format(i),
                        hyperid=4719300 + i,
                    ),
                    BlueOffer(
                        fesh=47193000 + i * 100,
                        price=1,
                        title="first_party_offer_{}".format(i),
                        feedid=47193000 + i * 100,
                        waremd5='sku-{:02d}-blue-1-poq57hRg'.format(i),
                        hyperid=4719300 + i,
                    ),
                ],
                delivery_buckets=[1234],
            )
            for i in range(count_shops / 2, count_shops)
        ]
        for i in range(count_shops):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4719300 + i).respond(0.6 - 0.01 * i)

    def test_first_party_fashion_incut(self):
        request_params = self.CgiParams(
            {
                "place": "prime",
                "blender": 1,
                "hid": CLOTHES_CATEG_ID,
                "numdoc": 8,
                "viewtype": "grid",
                "columns-in-grid": 3,
            }
        )
        rearr_factors = self.RearrFlags(
            {
                "market_blender_first_party_fashion_min_count": 3,
                "market_blender_first_party_fashion_max_count": 10,
            }
        )

        response = self.report.request_json(self.create_blender_request(request_params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "entity": "searchIncut",
                            "position": 1,
                            "inClid": 26,
                            "title": "Закажите с примеркой",
                            "items": [
                                {
                                    "entity": "product",
                                    "titles": {"raw": "first_party_model_{}".format(i)},
                                    "offers": {
                                        "items": [
                                            {
                                                "modelAwareTitles": {"raw": "first_party_offer_{}".format(i)},
                                            }
                                        ]
                                    },
                                }
                                for i in range(1, 10)
                            ],
                        }
                    ]
                },
            },
            allow_different_len=False,
            preserve_order=False,
        )


if __name__ == '__main__':
    main()
