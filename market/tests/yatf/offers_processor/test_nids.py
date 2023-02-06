#!/usr/bin/env python
# coding: utf-8

import pytest

from hamcrest import (
    assert_that,
    all_of,
    has_items
)

from market.idx.offers.yatf.matchers.offers_processor.env_matchers import HasGenlogRecord

from market.idx.yatf.resources.tovar_tree_pb import MboCategory, TovarTreePbGz
from market.idx.offers.yatf.utils.fixtures import default_blue_genlog, default_genlog

from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.yatf.resources.mbo.cataloger_navigation_xml import CatalogerNavigationXml, NavigationTree, NavigationNode, ModelLists, ModelList
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join


@pytest.fixture(scope="module")
def tovar_tree():
    return [
        MboCategory(
            hid=1, tovar_id=0,
            unique_name="Все товары", name="Все товары",
            output_type=MboCategory.GURULIGHT),
        MboCategory(
            hid=11, tovar_id=1, parent_hid=1,
            unique_name="Бытовая техника", name="Бытовая техника",
            output_type=MboCategory.GURULIGHT),
        MboCategory(
            hid=111, tovar_id=2, parent_hid=11,
            unique_name="Мелкая техника для кухни", name="Мелкая техника для кухни",
            output_type=MboCategory.GURULIGHT),
        MboCategory(
            hid=1111, tovar_id=3, parent_hid=111,
            unique_name="Чайники", name="Чайники",
            output_type=MboCategory.GURULIGHT),
        MboCategory(
            hid=121, tovar_id=4, parent_hid=1,
            unique_name="Детские товары", name="Детские товары",
            output_type=MboCategory.GURULIGHT),
        MboCategory(
            hid=13, tovar_id=5, parent_hid=1,
            unique_name="Зоотовары", name="Зоотовары",
            output_type=MboCategory.GURULIGHT),
        MboCategory(
            hid=131, tovar_id=6, parent_hid=13,
            unique_name="Корма для собак", name="Корма для собак",
            output_type=MboCategory.GURULIGHT),
        MboCategory(
            hid=14, tovar_id=7, parent_hid=1,
            unique_name="Авто", name="Авто",
            output_type=MboCategory.GURULIGHT),
        MboCategory(
            hid=500, tovar_id=8, parent_hid=1,
            unique_name="Товары по ведьмаку", name="Товары по ведьмаку",
            output_type=MboCategory.GURULIGHT),
        MboCategory(
            hid=501, tovar_id=9, parent_hid=500,
            unique_name="Ведьмаки и Чародейки", name="Ведьмаки и Чародейки",
            output_type=MboCategory.GURULIGHT),
        MboCategory(
            hid=502, tovar_id=10, parent_hid=500,
            unique_name="Нильфгаард", name="Нильфгаард",
            output_type=MboCategory.GURULIGHT)
    ]


@pytest.fixture(scope="module")
def cataloger_navigation_old_tree():
    return NavigationTree(
        1111,
        NavigationNode(
            nid=2, hid=1, is_blue=1,
            children=[
                # simple nid
                NavigationNode(
                    nid=21, hid=11, is_blue=1,
                    children=[
                        NavigationNode(
                            nid=211, hid=111, is_blue=1
                        ),
                    ]
                ),

                # virtual nid
                NavigationNode(
                    nid=22, is_blue=1,
                    children=[
                        NavigationNode(
                            nid=221, hid=121, is_blue=1
                        ),
                    ]
                ),

                # not blue nid
                NavigationNode(
                    nid=23, hid=13, is_blue=0,
                    children=[
                        NavigationNode(
                            nid=231, hid=131, is_blue=1
                        ),
                    ]
                ),

                # duplicate nid
                NavigationNode(
                    nid=241, hid=14, is_blue=1,
                ),
                NavigationNode(
                    nid=242, hid=14, is_blue=1,
                ),
                NavigationNode(
                    nid=243, hid=14, is_blue=0,
                )
            ]
        )
    )


@pytest.fixture(scope="module")
def cataloger_navigation_white_tree():
    # Пока что белое дерево не участвует в формировании синих поисковых литералов.
    # Заводу его, чтобы показать, что это не делается
    # Белое дерево является копией старого дерева
    return NavigationTree(
        1112,
        NavigationNode(
            nid=3, hid=1,
            children=[
                # simple nid
                NavigationNode(
                    nid=31, hid=11,
                    children=[
                        NavigationNode(
                            nid=311, hid=111
                        ),
                    ]
                ),

                # virtual nid
                NavigationNode(
                    nid=32,
                    children=[
                        NavigationNode(
                            nid=321, hid=121
                        ),
                    ]
                ),

                # duplicate nid
                NavigationNode(
                    nid=341, hid=14,
                ),
                NavigationNode(
                    nid=342, hid=14,
                ),
                NavigationNode(
                    nid=343, hid=14,
                ),
            ]
        ),
        code='green'
    )


@pytest.fixture(scope="module")
def cataloger_navigation_blue_tree():
    # Отдельное синее дерево
    # Для него не важно наличие маркера is_blue, как для старого дерева
    return NavigationTree(
        1113,
        NavigationNode(
            nid=4, hid=1,
            children=[
                # simple nid
                NavigationNode(
                    nid=41, hid=11,
                    children=[
                        NavigationNode(
                            nid=411, hid=111,
                        ),
                    ]
                ),

                # virtual nid
                NavigationNode(
                    nid=42,
                    children=[
                        NavigationNode(
                            nid=421, hid=121,
                        ),
                    ]
                ),

                # duplicate nid
                NavigationNode(
                    nid=441, hid=14,
                ),
                NavigationNode(
                    nid=442, hid=14,
                ),
                NavigationNode(
                    nid=443, hid=14,
                ),
                NavigationNode(
                    nid=100,
                    hid=500,
                    children=[
                        NavigationNode(
                            nid=101,
                            hid=501,
                            children=[
                                NavigationNode(nid=1101, model_list_id=100500),
                                NavigationNode(nid=1102, hid=502)
                            ]
                        ),
                        NavigationNode(
                            nid=102,
                            hid=502,
                            children=[
                                NavigationNode(nid=1103, hid=502)
                            ],
                            model_list_id=100501
                        ),
                        NavigationNode(
                            nid=103,
                            children=[
                                NavigationNode(nid=1104, model_list_id=100502)
                            ]
                        ),
                        NavigationNode(
                            nid=104,
                            children=[
                                NavigationNode(nid=1105, model_list_id=100503)
                            ]
                        )
                    ]
                )
            ]
        ),
        code='blue'
    )


@pytest.fixture(scope="module")
def cataloger_navigation_b2b_tree():
    # дерево для Маркета для бизнеса
    return NavigationTree(
        1114,
        NavigationNode(
            nid=5, hid=1,
            children=[
                # simple nid
                NavigationNode(
                    nid=51, hid=11,
                    children=[
                        NavigationNode(
                            nid=511, hid=111,
                        ),
                    ]
                ),

                # virtual nid
                NavigationNode(
                    nid=52,
                    children=[
                        NavigationNode(
                            nid=521, hid=121,
                        ),
                    ]
                ),

                # duplicate nid
                NavigationNode(
                    nid=541, hid=14,
                ),
                NavigationNode(
                    nid=542, hid=14,
                ),
                NavigationNode(
                    nid=543, hid=14,
                ),
                NavigationNode(
                    nid=200,
                    hid=500,
                    children=[
                        NavigationNode(
                            nid=201,
                            hid=501,
                            children=[
                                NavigationNode(nid=2101, model_list_id=100500),
                                NavigationNode(nid=2102, hid=502)
                            ]
                        ),
                        NavigationNode(
                            nid=202,
                            hid=502,
                            children=[
                                NavigationNode(nid=2103, hid=502)
                            ],
                            model_list_id=100501
                        ),
                        NavigationNode(
                            nid=203,
                            children=[
                                NavigationNode(nid=2104, model_list_id=100502)
                            ]
                        ),
                        NavigationNode(
                            nid=204,
                            children=[
                                NavigationNode(nid=2105, model_list_id=100503)
                            ]
                        )
                    ]
                )
            ]
        ),
        code='b2b'
    )


@pytest.fixture(scope="module")
def model_lists():
    return ModelLists([
        ModelList(id=100500, models=[3220, 3221]),
        ModelList(id=100501, models=[3223]),
        ModelList(id=100502, models=[3220, 3224, 555]),
        ModelList(id=100503, models=[3223]),
    ])


WARE_IDS = ['bmsD+9/S6qcBoJx09K/A9A', 'offerXdsbsXnoXmskuXXXg', 'offerXdsbsXXXXXXXXXXXg', 'offerXCPCXXXXXXXXXXXXg']


@pytest.fixture(scope="module")
def genlog_rows():
    hids = [
        11, 111, 1111,
        121,
        13, 131,
        14,
    ]

    blue_offers = [
        default_blue_genlog(category_id=hid, ware_md5=WARE_IDS[0]) for hid in hids
    ]

    white_cpa_no_msku_offers = [
        default_genlog(category_id=hid, ware_md5=WARE_IDS[1], cpa=4) for hid in hids
    ]

    white_cpa_with_msku_offers = [
        default_genlog(category_id=hid, ware_md5=WARE_IDS[2], cpa=4, market_sku=100500) for hid in hids
    ]

    white_cpc_offers = [
        default_genlog(category_id=hid, ware_md5=WARE_IDS[3], cpa=1) for hid in hids
    ]

    offers_for_nodes_with_model_lists = [
        default_genlog(category_id=502, ware_md5='offerX502X3220XXXXXXXg', model_id=3220),
        default_genlog(category_id=502, ware_md5='offerX502X3221XXXXXXXg', model_id=3221),
        default_genlog(category_id=502, ware_md5='offerX502X3224XXXXXXXg', model_id=3224),
        default_genlog(category_id=502, ware_md5='offerX502X322XXXXXXXXg', model_id=322),
        default_genlog(category_id=501, ware_md5='offerX505X3223XXXXXXXg', model_id=3223),
        default_genlog(category_id=502, ware_md5='offerX502X0XXXXXXXXXXg'),

        default_genlog(category_id=502, ware_md5='offerX502X3220XMSKUXXg', market_sku=3220),
        default_genlog(category_id=502, ware_md5='offerX502X3221XMSKUXXg', market_sku=3221),
        default_genlog(category_id=502, ware_md5='offerX502X3224XMSKUXXg', market_sku=3224),
        default_genlog(category_id=502, ware_md5='offerX502X322XXMSKUXXg', market_sku=322),
        default_genlog(category_id=501, ware_md5='offerX505X3223XMSKUXXg', market_sku=3223),

        default_genlog(category_id=501, ware_md5='offerX505X3223X555XXXg', model_id=3223, market_sku=555),
    ]

    return blue_offers + white_cpa_no_msku_offers + white_cpa_with_msku_offers + white_cpc_offers + offers_for_nodes_with_model_lists


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.yield_fixture(scope="module")
def workflow(yt_server, genlog_table, cataloger_navigation_old_tree, tovar_tree, cataloger_navigation_white_tree, cataloger_navigation_blue_tree, cataloger_navigation_b2b_tree, model_lists):
    input_table_paths = [genlog_table.get_path()]

    resources = {
        'tovar_tree_pb': TovarTreePbGz(tovar_tree),
        'cataloger.navigation.xml': CatalogerNavigationXml(
            filename="cataloger.navigation.xml",
            nav_trees=[cataloger_navigation_old_tree],
            model_lists=model_lists
        ),
        'cataloger.navigation.all.xml': CatalogerNavigationXml(
            filename="cataloger.navigation.all.xml",
            nav_trees=[cataloger_navigation_white_tree, cataloger_navigation_blue_tree, cataloger_navigation_b2b_tree],
            model_lists=model_lists
        )
    }

    with OffersProcessorTestEnv(
            yt_server,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths,
            **resources
    ) as env:
        env.execute()
        yield env


def test_offer_model_lists(workflow):
    '''
    Проверям, что работает логика model-lists:
    У нав ноды может быть список моделек и тогда нидом этой ноды должны обогащаться только оффера с model_id из списка

    Механизм такой:
    Для моделек (офферов с model_id) из списка нида нужно добавить все литералы от корня до этой ноды-списка
    Но при этом потомки списка моделей не наследуют его нид
    '''
    assert_that(workflow, all_of(
        # По-умолчанию для hid-а 502 должны быть ниды 2, 4, 100, 101, 1102, 1103
        # Хотя нид 102 является родителем к ноде с хидом 502, он должен отсутствовать, тк является списком моделей
        # Оффера без модели:
        HasGenlogRecord({'ware_md5': 'offerX502X0XXXXXXXXXXg', 'nids_literals': has_items(*[2, 4, 5, 100, 101, 1102, 1103])}),
        HasGenlogRecord({'ware_md5': 'offerX502X322XXXXXXXXg', 'nids_literals': has_items(*[2, 4, 5, 100, 101, 1102, 1103])}),
        HasGenlogRecord({'ware_md5': 'offerX502X322XXMSKUXXg', 'nids_literals': has_items(*[2, 4, 5, 100, 101, 1102, 1103])}),
        # Для оффера с model_id == 3220 должны добавиться ниды 1101, 1104
        # Тк эта модель/мску есть в их списках
        HasGenlogRecord({'ware_md5': 'offerX502X3220XXXXXXXg', 'nids_literals': has_items(*[2, 4, 5, 100, 101, 1102, 1103, 1101, 1104, 103])}),
        HasGenlogRecord({'ware_md5': 'offerX502X3220XMSKUXXg', 'nids_literals': has_items(*[2, 4, 5, 100, 101, 1102, 1103, 1101, 1104, 103])}),
        # Для оффера с model_id == 3221 должен добавиться нид 1101
        # Тк эта модель/мску есть в его списке
        HasGenlogRecord({'ware_md5': 'offerX502X3221XXXXXXXg', 'nids_literals': has_items(*[2, 4, 5, 100, 101, 1102, 1103, 1101])}),
        HasGenlogRecord({'ware_md5': 'offerX502X3221XMSKUXXg', 'nids_literals': has_items(*[2, 4, 5, 100, 101, 1102, 1103, 1101])}),
        # Для оффера с model_id == 3224 должен добавиться нид 1104, 103
        # Тк эта модель/мску есть в его списке
        HasGenlogRecord({'ware_md5': 'offerX502X3224XXXXXXXg', 'nids_literals': has_items(*[2, 4, 5, 100, 101, 1102, 1103, 1104, 103])}),
        HasGenlogRecord({'ware_md5': 'offerX502X3224XMSKUXXg', 'nids_literals': has_items(*[2, 4, 5, 100, 101, 1102, 1103, 1104, 103])}),
        # Оффер с model_id == 3223, и категорией 501 должен иметь литералы 2, 4, 100, 101 из дерева и 102, 104, 1105  из списков моделей 100503
        HasGenlogRecord({'ware_md5': 'offerX505X3223XXXXXXXg', 'nids_literals': has_items(*[2, 4, 5, 100, 101, 102, 104, 1105])}),
        HasGenlogRecord({'ware_md5': 'offerX505X3223XMSKUXXg', 'nids_literals': has_items(*[2, 4, 5, 100, 101, 102, 104, 1105])}),
        # Оффер с model_id == 3223, и категорией 501 должен иметь литералы 2, 4, 100, 101 из дерева и 102, 104, 1105  из списков моделей 100503
        # + у оффера offerX505X3223X555XXXg есть мску == 555, которое лежит в списке 100502
        # поэтому добавятся ниды 1104, 103
        HasGenlogRecord({'ware_md5': 'offerX505X3223X555XXXg', 'nids_literals': has_items(*[2, 4, 5, 100, 101, 102, 104, 1105, 1104, 103])}),
    ))


def test_offer_simple_not_leaf(workflow):
    # check simple navigation node literals
    # К документу добавляются все родительские ниды
    for wareId in WARE_IDS:
        assert_that(
            workflow,
            HasGenlogRecord({
                'category_id': 11,
                'ware_md5': wareId,
                'nids_literals': has_items(*[
                    2, 21,
                    4, 41,
                    5, 51,
                ])
            }),
            u'Простой случай не листового узла')


def test_offer_simple_leaf(workflow):
    # check simple navigation node literals
    # К документу добавляются все родительские ниды
    for wareId in WARE_IDS:
        assert_that(
            workflow,
            HasGenlogRecord({
                'category_id': 111,
                'ware_md5': wareId,
                'nids_literals': has_items(*[
                    2, 21, 211,
                    4, 41, 411,
                    5, 51, 511,
                ])
            }),
            u'Простой случай листового узла')


def test_offer_nids_of_parent_hid(workflow):
    # Для hid не имеющего соответствия nid берутся nid его родительского hid
    for wareId in WARE_IDS:
        assert_that(
            workflow,
            HasGenlogRecord({
                'category_id': 1111,
                'ware_md5': wareId,
                'nids_literals': has_items(*[
                    2, 21, 211,
                    4, 41, 411,
                    5, 51, 511,
                ])
            }),
            u'Оффер с категорией, не представленной в навигационном дереве')


def test_offer_virtual_node(workflow):
    # check virtual navigation node literals
    # К документу добавляются все родительские ниды. В том числе и виртуальные
    for wareId in WARE_IDS:
        assert_that(
            workflow,
            HasGenlogRecord({
                'category_id': 121,
                'ware_md5': wareId,
                'nids_literals': has_items(*[
                    2, 22, 221,
                    4, 42, 421,
                    5, 52, 521,
                ])
            }),
            u'Документ с виртуальным родителем')


def test_offer_not_blue(workflow):
    # check not blue navigation node literals
    # К документу добавляются все родительские ниды, кроме скрытых на синем маркете
    for wareId in WARE_IDS:
        assert_that(
            workflow,
            HasGenlogRecord({
                'category_id': 131,
                'ware_md5': wareId,
                'nids_literals': has_items(*[
                    231, 2, 4, 5,
                ])
            }),
            u'Документ с не синим родителем')


def test_offer_multi_nids(workflow):
    # check multi navigation node literals
    # К документу добавляются все ниды, которые соответствуют заданному hid
    for wareId in WARE_IDS:
        assert_that(
            workflow,
            HasGenlogRecord({
                'category_id': 14,
                'ware_md5': wareId,
                'nids_literals': has_items(*[
                    2, 241, 242,
                    4, 441, 442, 443,
                    5, 541, 542, 543
                ])
            }),
            u'Документ с несколькими нидами')
