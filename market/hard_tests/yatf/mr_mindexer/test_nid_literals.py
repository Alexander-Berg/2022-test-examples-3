# coding: utf-8

import pytest
from hamcrest import assert_that

from market.idx.generation.yatf.test_envs.mr_mindexer import MrMindexerBuildTestEnv, MrMindexerMergeTestEnv
from market.idx.generation.yatf.resources.mr_mindexer.mr_mindexer_helpers import MrMindexerMergeOptions, MrMindexerMergeIndexType

from market.idx.offers.yatf.matchers.offers_indexer.env_matchers import HasLiterals
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.utils.fixtures import default_genlog, default_blue_genlog

from market.idx.yatf.resources.tovar_tree_pb import MboCategory, TovarTreePbGz
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
def model_lists():
    return ModelLists([
        ModelList(id=100500, models=[3220, 3221]),
        ModelList(id=100501, models=[3223]),
        ModelList(id=100502, models=[3220, 3224, 555]),
        ModelList(id=100503, models=[3223]),
    ])


WARE_IDS = ['bmsD+9/S6qcBoJx09K/A9A', 'offerXdsbsXnoXmskuXXXg', 'offerXdsbsXXXXXXXXXXXg', 'offerXCPCXXXXXXXXXXXXg']
OFFER2ID = dict()


def make_offer_id(ware_id, category_id):
    return ware_id + str(category_id)


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

    result_rows = blue_offers + white_cpa_no_msku_offers + white_cpa_with_msku_offers + white_cpc_offers + offers_for_nodes_with_model_lists
    doc_id = 0
    for offer in result_rows:
        OFFER2ID[make_offer_id(offer['ware_md5'], offer['category_id'])] = doc_id
        doc_id += 1

    return result_rows


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.yield_fixture(scope="module")
def offers_processor_workflow(yt_server, genlog_table, cataloger_navigation_old_tree, tovar_tree, cataloger_navigation_white_tree, cataloger_navigation_blue_tree, model_lists):
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
            nav_trees=[cataloger_navigation_white_tree, cataloger_navigation_blue_tree],
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


@pytest.yield_fixture(scope="module")
def mr_mindexer_direct(yt_server, offers_processor_workflow):
    with MrMindexerBuildTestEnv() as build_env:
        build_env.execute_from_offers_list(yt_server, offers_processor_workflow.genlog_dicts)
        build_env.verify()

        resourses = {
            'merge_options': MrMindexerMergeOptions(
                input_portions_path=build_env.yt_index_portions_path,
                part=0,
                index_type=MrMindexerMergeIndexType.DIRECT,
            ),
        }

        with MrMindexerMergeTestEnv(**resourses) as env:
            env.execute(yt_server)
            env.verify()
            yield env


def check_nids(mr_mindexer_direct, nids, docs):
    for nid in nids:
        assert_that(mr_mindexer_direct, HasLiterals('#nid="' + nid, docs))


def test_nid_literals(mr_mindexer_direct):
    # По-умолчанию для hid-а 502 должны быть ниды 2, 4, 100, 101, 1102, 1103
    # Хотя нид 102 является родителем к ноде с хидом 502, он должен отсутствовать, тк является списком моделей
    # Оффера без модели:
    check_nids(
        mr_mindexer_direct,
        nids=['2', '4', '100', '101', '1102', '1103'],
        docs=[
            OFFER2ID[make_offer_id('offerX502X0XXXXXXXXXXg', 502)],
            OFFER2ID[make_offer_id('offerX502X322XXXXXXXXg', 502)],
            OFFER2ID[make_offer_id('offerX502X322XXMSKUXXg', 502)],
        ]
    )
    # Для оффера с model_id == 3220 должны добавиться ниды 1101, 1104
    # Тк эта модель/мску есть в их списках
    check_nids(
        mr_mindexer_direct,
        nids=['2', '4', '100', '101', '1102', '1103', '1101', '1104', '103'],
        docs=[
            OFFER2ID[make_offer_id('offerX502X3220XXXXXXXg', 502)],
            OFFER2ID[make_offer_id('offerX502X3220XMSKUXXg', 502)],
        ]
    )
    # Для оффера с model_id == 3221 должен добавиться нид 1101
    # Тк эта модель/мску есть в его списке
    check_nids(
        mr_mindexer_direct,
        nids=['2', '4', '100', '101', '1102', '1103', '1101'],
        docs=[
            OFFER2ID[make_offer_id('offerX502X3221XXXXXXXg', 502)],
            OFFER2ID[make_offer_id('offerX502X3221XMSKUXXg', 502)],
        ]
    )
    # Для оффера с model_id == 3224 должен добавиться нид 1104, 103
    # Тк эта модель/мску есть в его списке
    check_nids(
        mr_mindexer_direct,
        nids=['2', '4', '100', '101', '1102', '1103', '1104', '103'],
        docs=[
            OFFER2ID[make_offer_id('offerX502X3224XXXXXXXg', 502)],
            OFFER2ID[make_offer_id('offerX502X3224XMSKUXXg', 502)],
        ]
    )
    # Оффер с model_id == 3223, и категорией 501 должен иметь литералы 2, 4, 100, 101 из дерева и 102, 104, 1105  из списков моделей 100503
    check_nids(
        mr_mindexer_direct,
        nids=['2', '4', '100', '101', '102', '104', '1105'],
        docs=[
            OFFER2ID[make_offer_id('offerX505X3223XXXXXXXg', 501)],
            OFFER2ID[make_offer_id('offerX505X3223XMSKUXXg', 501)],
        ]
    )
    # Оффер с model_id == 3223, и категорией 501 должен иметь литералы 2, 4, 100, 101 из дерева и 102, 104, 1105  из списков моделей 100503
    # + у оффера offerX505X3223X555XXXg есть мску == 555, которое лежит в списке 100502
    # поэтому добавятся ниды 1104, 103
    check_nids(
        mr_mindexer_direct,
        nids=['2', '4', '100', '101', '102', '104', '1105', '1104', '103'],
        docs=[
            OFFER2ID[make_offer_id('offerX505X3223X555XXXg', 501)],
        ]
    )

    # check simple navigation node literals (не листья)
    # К документу добавляются все родительские ниды
    docs = []
    for wareId in WARE_IDS:
        docs.append(OFFER2ID[make_offer_id(wareId, 11)])
    check_nids(mr_mindexer_direct, nids=['2', '21', '4', '41'], docs=docs)

    # check simple navigation node literals (листья)
    # К документу добавляются все родительские ниды
    docs = []
    for wareId in WARE_IDS:
        docs.append(OFFER2ID[make_offer_id(wareId, 111)])
    check_nids(mr_mindexer_direct, nids=['2', '21', '211', '4', '41', '411'], docs=docs)

    # Для hid не имеющего соответствия nid берутся nid его родительского hid
    docs = []
    for wareId in WARE_IDS:
        docs.append(OFFER2ID[make_offer_id(wareId, 121)])
    check_nids(mr_mindexer_direct, nids=['2', '22', '221', '4', '42', '421'], docs=docs)

    # check virtual navigation node literals
    # К документу добавляются все родительские ниды. В том числе и виртуальные
    docs = []
    for wareId in WARE_IDS:
        docs.append(OFFER2ID[make_offer_id(wareId, 121)])
    check_nids(mr_mindexer_direct, nids=['2', '22', '221', '4', '42', '421'], docs=docs)

    # check not blue navigation node literals
    # К документу добавляются все родительские ниды, кроме скрытых на синем маркете
    docs = []
    for wareId in WARE_IDS:
        docs.append(OFFER2ID[make_offer_id(wareId, 131)])
    check_nids(mr_mindexer_direct, nids=['2', '231', '4'], docs=docs)

    # check multi navigation node literals
    # К документу добавляются все ниды, которые соответствуют заданному hid
    docs = []
    for wareId in WARE_IDS:
        docs.append(OFFER2ID[make_offer_id(wareId, 14)])
    check_nids(mr_mindexer_direct, nids=['2', '241', '242', '4', '441', '442', '443'], docs=docs)
