#!/usr/bin/env python
# coding: utf-8

import pytest
from hamcrest import assert_that

from market.idx.generation.yatf.test_envs.mr_mindexer import MrMindexerBuildTestEnv, MrMindexerMergeTestEnv
from market.idx.generation.yatf.resources.mr_mindexer.mr_mindexer_helpers import MrMindexerMergeOptions, MrMindexerMergeIndexType

from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.matchers.offers_indexer.env_matchers import HasLiterals
from market.idx.offers.yatf.utils.fixtures import default_blue_genlog

from market.idx.yatf.resources.tovar_tree_pb import MboCategory, TovarTreePbGz
from market.idx.yatf.resources.mbo.cataloger_navigation_xml import CatalogerNavigationXml, NavigationTree, NavigationNode, Recipes, Recipe, RecipeFilter, RecipeValue
from market.proto.content.mbo.MboParameters_pb2 import Category, Parameter, BOOLEAN, ENUM, NUMERIC, MODEL_LEVEL, OFFER_LEVEL
from market.idx.offers.yatf.resources.offers_indexer.gl_mbo_pb import GlMboPb
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join
import yt.wrapper as yt


@pytest.fixture(scope="module")
def tovar_tree():
    return [
        MboCategory(
            hid=1, tovar_id=0,
            unique_name="Все товары", name="Все товары",
            output_type=MboCategory.GURULIGHT),
        MboCategory(
            hid=11, tovar_id=1, parent_hid=1,
            unique_name="Телефоны", name="Телефоны",
            output_type=MboCategory.GURULIGHT),
        MboCategory(
            hid=12, tovar_id=2, parent_hid=1,
            unique_name="Stub", name="Stub",
            output_type=MboCategory.GURULIGHT),
        MboCategory(
            hid=111, tovar_id=3, parent_hid=11,
            unique_name="Телефончики", name="Телефончики",
            output_type=MboCategory.GURULIGHT),
        MboCategory(
            hid=1111, tovar_id=4, parent_hid=111,
            unique_name="Телефончики", name="Телефончики",
            output_type=MboCategory.GURULIGHT),
        MboCategory(
            hid=112, tovar_id=5, parent_hid=11,
            unique_name="Телефончики", name="Телефонушки",
            output_type=MboCategory.GURULIGHT),
    ]


@pytest.fixture(scope="module")
def gl_mbo():
    parameters = [
        Parameter(id=11, xsl_name='bool_param1', value_type=BOOLEAN, published=True, param_type=OFFER_LEVEL),
        Parameter(id=12, xsl_name='numeric_param1', value_type=NUMERIC, published=True, param_type=OFFER_LEVEL),
        Parameter(id=13, xsl_name='enum_param1', value_type=ENUM, published=True, param_type=OFFER_LEVEL),
        Parameter(id=91, xsl_name='model_bool_param1', value_type=BOOLEAN, published=True, param_type=MODEL_LEVEL),
    ]
    return [
        Category(
            hid=11,
            parameter=parameters
        ),
        Category(
            hid=111,
            parameter=parameters
        ),
        Category(
            hid=1111,
            parameter=parameters
        ),
        Category(
            hid=112,
            parameter=parameters
        )
    ]


@pytest.fixture(scope="module")
def cataloger_navigation_white_tree():
    # Пустое несинее дерево
    return NavigationTree(
        1112,
        NavigationNode(
            nid=1, hid=1,
            children=[]
        ),
        code='green'
    )


@pytest.fixture(scope="module")
def cataloger_navigation_old_tree():
    return NavigationTree(
        1111,
        NavigationNode(
            nid=2, hid=1, is_blue=1,
            children=[]
        )
    )


@pytest.fixture(scope="module")
def cataloger_navigation_blue_tree():
    # Отдельное синее дерево
    # Для него не важно наличие маркера is_blue, как для старого дерева
    return NavigationTree(
        1113,
        NavigationNode(
            nid=3, hid=1,
            children=[
                # simple nid
                NavigationNode(
                    nid=31, hid=12,
                    children=[
                        NavigationNode(
                            nid=311, hid=111, recipe_id="1"
                        ),
                        NavigationNode(
                            nid=312, hid=111, recipe_id="2"
                        ),
                        NavigationNode(
                            nid=313, hid=111, recipe_id="3"
                        ),
                        NavigationNode(
                            nid=314, hid=111, recipe_id="4"
                        ),
                        NavigationNode(
                            nid=315, hid=111, recipe_id="5"
                        ),
                        NavigationNode(
                            nid=316, hid=111, recipe_id="6"
                        ),
                        NavigationNode(
                            nid=317, hid=112, recipe_id="7",
                            children=[
                                NavigationNode(
                                    nid=3171, hid=112
                                ),
                            ],
                        ),
                        NavigationNode(
                            nid=318, hid=111, recipe_id="8"
                        ),
                    ]
                ),
            ]
        ),
        code='blue'
    )


@pytest.fixture(scope="module")
def recipes():
    return Recipes(
        [
            Recipe(
                id="1",
                hid="111",
                name="Один офферный булев фильтр",
                filters=[
                    RecipeFilter(
                        param_id=11,
                        type=RecipeFilter.ParamTypes.boolean,
                        values=[RecipeValue(1)]
                    ),
                ]
            ),
            Recipe(
                id="2",
                hid="111",
                name="Один офферный численный фильтр",
                filters=[
                    RecipeFilter(
                        param_id=12,
                        type=RecipeFilter.ParamTypes.number,
                        max_value=500,
                        min_value=100,
                    ),
                ]
            ),
            Recipe(
                id="3",
                hid="111",
                name="Один офферный enum фильтр",
                filters=[
                    RecipeFilter(
                        param_id=13,
                        type=RecipeFilter.ParamTypes.enum,
                        values=[RecipeValue(301)]
                    ),
                ]
            ),
            Recipe(
                id="4",
                hid="111",
                name="Модельный булевый фильтр",
                filters=[
                    RecipeFilter(
                        param_id=91,
                        type=RecipeFilter.ParamTypes.boolean,
                        values=[RecipeValue(1)]
                    ),
                ]
            ),
            Recipe(
                id="5",
                hid="111",
                name="Модельный и офферный булевы фильтры",
                filters=[
                    RecipeFilter(
                        param_id=91,
                        type=RecipeFilter.ParamTypes.boolean,
                        values=[RecipeValue(0)]
                    ),
                    RecipeFilter(
                        param_id=11,
                        type=RecipeFilter.ParamTypes.boolean,
                        values=[RecipeValue(0)]
                    ),
                ]
            ),
            Recipe(
                id="6",
                hid="111",
                name="Комбинация офферных и модельного фильтров",
                filters=[
                    RecipeFilter(
                        param_id=91,
                        type=RecipeFilter.ParamTypes.boolean,
                        values=[RecipeValue(0)]
                    ),
                    RecipeFilter(
                        param_id=11,
                        type=RecipeFilter.ParamTypes.boolean,
                        values=[RecipeValue(0)]
                    ),
                    RecipeFilter(
                        param_id=12,
                        type=RecipeFilter.ParamTypes.number,
                        max_value=5001,
                        min_value=4999,
                    ),
                    RecipeFilter(
                        param_id=13,
                        type=RecipeFilter.ParamTypes.enum,
                        values=[RecipeValue(303)]
                    ),
                ]
            ),
            Recipe(
                id="7",
                hid="111",
                name="Комбинация офферных и модельного фильтров",
                filters=[
                    RecipeFilter(
                        param_id=91,
                        type=RecipeFilter.ParamTypes.boolean,
                        values=[RecipeValue(0)]
                    ),
                    RecipeFilter(
                        param_id=11,
                        type=RecipeFilter.ParamTypes.boolean,
                        values=[RecipeValue(0)]
                    ),
                    RecipeFilter(
                        param_id=12,
                        type=RecipeFilter.ParamTypes.number,
                        max_value=15001,
                        min_value=14999,
                    ),
                    RecipeFilter(
                        param_id=13,
                        type=RecipeFilter.ParamTypes.enum,
                        values=[RecipeValue(303)]
                    ),
                ]
            ),
            Recipe(
                id="8",
                hid="111",
                name="Enum фильтр на множество значений",
                filters=[
                    RecipeFilter(
                        param_id=13,
                        type=RecipeFilter.ParamTypes.enum,
                        values=[RecipeValue(309), RecipeValue(308), RecipeValue(307)]
                    ),
                ]
            ),
        ]
    )


OFFER2ID = dict()


def make_offer_id(ware_id, category_id):
    return ware_id + str(category_id)


@pytest.fixture(scope="module")
def genlog_rows():
    genlog_rows = [
        # оффер для проверки, отбрасывания родительских нидов для рецепта
        default_blue_genlog(category_id=111,
                            ware_md5='000000000000000000000w',
                            offer_params=[
                                {'enriched_param_id': yt.yson.YsonUint64(11), 'id': yt.yson.YsonUint64(0)},
                            ]),
        # пофильтровые проверки
        default_blue_genlog(category_id=111,
                            ware_md5='000000000000000000001w',
                            offer_params=[
                                {'enriched_param_id': yt.yson.YsonUint64(11), 'id': yt.yson.YsonUint64(1)},
                            ]),
        default_blue_genlog(category_id=111,
                            ware_md5='000000000000000000002w',
                            offer_params=[
                                {'enriched_param_id': yt.yson.YsonUint64(12), 'num': 121.0},
                            ]),
        default_blue_genlog(category_id=111,
                            ware_md5='000000000000000000003w',
                            offer_params=[
                                {'enriched_param_id': yt.yson.YsonUint64(13), 'id': yt.yson.YsonUint64(301)},
                            ]),
        # негативные пофильтровые проверки
        default_blue_genlog(category_id=111,
                            ware_md5='000000000000000000004w',
                            offer_params=[
                                {'enriched_param_id': yt.yson.YsonUint64(12), 'num': 1200500.0},
                            ]),
        default_blue_genlog(category_id=111,
                            ware_md5='000000000000000000005w',
                            offer_params=[
                                {'enriched_param_id': yt.yson.YsonUint64(12), 'num': 12.0},
                            ]),
        default_blue_genlog(category_id=111,
                            ware_md5='000000000000000000006w',
                            offer_params=[
                                {'enriched_param_id': yt.yson.YsonUint64(13), 'id': yt.yson.YsonUint64(302)},
                            ]),
        default_blue_genlog(category_id=111,
                            ware_md5='000000000000000000007w',
                            offer_params=[
                                {'enriched_param_id': yt.yson.YsonUint64(11), 'id': yt.yson.YsonUint64(0)},
                            ]),
        # оффер с модельным параметром
        default_blue_genlog(category_id=111,
                            ware_md5='000000000000000000008w',
                            offer_params=[
                                {'enriched_param_id': yt.yson.YsonUint64(91), 'id': yt.yson.YsonUint64(1)},
                            ]),
        # оффер с модельным параметром и не модельным параметром
        default_blue_genlog(category_id=111,
                            ware_md5='000000000000000000009w',
                            offer_params=[
                                {'enriched_param_id': yt.yson.YsonUint64(11), 'id': yt.yson.YsonUint64(0)},
                                {'enriched_param_id': yt.yson.YsonUint64(91), 'id': yt.yson.YsonUint64(0)},
                            ]),
        # оффер дочерний к товарной категории с рецептом
        default_blue_genlog(category_id=1111,
                            ware_md5='000000000000000000010w',
                            offer_params=[
                                {'enriched_param_id': yt.yson.YsonUint64(11), 'id': yt.yson.YsonUint64(0)},
                                {'enriched_param_id': yt.yson.YsonUint64(91), 'id': yt.yson.YsonUint64(0)},
                            ]),
        # оффер дочерний к товарной категории с рецептом не подходящий по модельному параметру
        default_blue_genlog(category_id=1111,
                            ware_md5='000000000000000000011w',
                            offer_params=[
                                {'enriched_param_id': yt.yson.YsonUint64(11), 'id': yt.yson.YsonUint64(0)},
                                {'enriched_param_id': yt.yson.YsonUint64(91), 'id': yt.yson.YsonUint64(1)},
                            ]),
        # оффер с большим количеством параметров
        default_blue_genlog(category_id=111,
                            ware_md5='000000000000000000012w',
                            offer_params=[
                                {'enriched_param_id': yt.yson.YsonUint64(11), 'id': yt.yson.YsonUint64(0)},
                                {'enriched_param_id': yt.yson.YsonUint64(12), 'num': 5000.0},
                                {'enriched_param_id': yt.yson.YsonUint64(13), 'id': yt.yson.YsonUint64(303)},
                                {'enriched_param_id': yt.yson.YsonUint64(91), 'id': yt.yson.YsonUint64(0)},
                            ]),
        # с чуть меньшим количеством, для проверки отбрасывания оффера с отсутствием указанного
        # в рецепте параметра
        default_blue_genlog(category_id=111,
                            ware_md5='000000000000000000013w',
                            offer_params=[
                                {'enriched_param_id': yt.yson.YsonUint64(11), 'id': yt.yson.YsonUint64(0)},
                                {'enriched_param_id': yt.yson.YsonUint64(12), 'num': 5000.0},
                                {'enriched_param_id': yt.yson.YsonUint64(13), 'id': yt.yson.YsonUint64(303)},
                            ]),
        # граничные проверки для фильтрации по числовому параметру
        default_blue_genlog(category_id=111,
                            ware_md5='000000000000000000014w',
                            offer_params=[
                                {'enriched_param_id': yt.yson.YsonUint64(12), 'num': 500.0},
                            ]),
        default_blue_genlog(category_id=111,
                            ware_md5='000000000000000000015w',
                            offer_params=[
                                {'enriched_param_id': yt.yson.YsonUint64(12), 'num': 100.0},
                            ]),
        # оффер с большим количеством параметров
        default_blue_genlog(category_id=111,
                            ware_md5='000000000000000000017w',
                            offer_params=[
                                {'enriched_param_id': yt.yson.YsonUint64(11), 'id': yt.yson.YsonUint64(0)},
                                {'enriched_param_id': yt.yson.YsonUint64(12), 'num': 5000.0},
                                {'enriched_param_id': yt.yson.YsonUint64(13), 'id': yt.yson.YsonUint64(3032)},
                                {'enriched_param_id': yt.yson.YsonUint64(91), 'id': yt.yson.YsonUint64(0)},
                            ]),
        default_blue_genlog(category_id=112,
                            ware_md5='000000000000000000017w',
                            offer_params=[
                            ]),
        default_blue_genlog(category_id=111,
                            ware_md5='000000000000000000018w',
                            offer_params=[
                                {'enriched_param_id': yt.yson.YsonUint64(13), 'id': yt.yson.YsonUint64(307)},
                                {'enriched_param_id': yt.yson.YsonUint64(13), 'id': yt.yson.YsonUint64(308)},
                                {'enriched_param_id': yt.yson.YsonUint64(13), 'id': yt.yson.YsonUint64(309)},
                            ]),
        default_blue_genlog(category_id=111,
                            ware_md5='000000000000000000019w',
                            offer_params=[
                                {'enriched_param_id': yt.yson.YsonUint64(12), 'num': 121.0},
                                {'enriched_param_id': yt.yson.YsonUint64(12), 'num': 122.0},
                                {'enriched_param_id': yt.yson.YsonUint64(12), 'num': 123.0},
                            ]),
        default_blue_genlog(category_id=111,
                            ware_md5='000000000000000000020w',
                            offer_params=[
                                {'enriched_param_id': yt.yson.YsonUint64(12), 'num': 5121.0},
                                {'enriched_param_id': yt.yson.YsonUint64(12), 'num': 122.0},
                                {'enriched_param_id': yt.yson.YsonUint64(12), 'num': 123.0},
                            ]),
        default_blue_genlog(category_id=111,
                            ware_md5='000000000000000000021w',
                            offer_params=[
                                {'enriched_param_id': yt.yson.YsonUint64(13), 'id': yt.yson.YsonUint64(307)},
                                {'enriched_param_id': yt.yson.YsonUint64(13), 'id': yt.yson.YsonUint64(308)},
                            ]),
        default_blue_genlog(category_id=111,
                            ware_md5='000000000000000000022w',
                            offer_params=[
                                {'enriched_param_id': yt.yson.YsonUint64(13), 'id': yt.yson.YsonUint64(307)},
                                {'enriched_param_id': yt.yson.YsonUint64(13), 'id': yt.yson.YsonUint64(308)},
                                {'enriched_param_id': yt.yson.YsonUint64(13), 'id': yt.yson.YsonUint64(306)},
                            ]),
    ]

    doc_id = 0
    for offer in genlog_rows:
        OFFER2ID[make_offer_id(offer['ware_md5'], offer['category_id'])] = doc_id
        doc_id += 1

    return genlog_rows


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.yield_fixture(scope="module")
def offers_processor_workflow(yt_server, genlog_table, cataloger_navigation_old_tree, tovar_tree, cataloger_navigation_white_tree, cataloger_navigation_blue_tree, recipes, gl_mbo):
    input_table_paths = [genlog_table.get_path()]
    resources = {
        'tovar_tree_pb': TovarTreePbGz(tovar_tree),
        'gl_mbo_pbuf_sn': GlMboPb(gl_mbo),  # мб это надо добавлять
        'cataloger.navigation.xml': CatalogerNavigationXml(filename="cataloger.navigation.xml", nav_trees=[cataloger_navigation_old_tree]),
        'cataloger.navigation.all.xml': CatalogerNavigationXml(
            filename="cataloger.navigation.all.xml",
            nav_trees=[cataloger_navigation_white_tree, cataloger_navigation_blue_tree],
            recipes=recipes
        ),
    }

    with OffersProcessorTestEnv(
            yt_server,
            enable_recipes_logic=True,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths,
            **resources
    ) as env:
        env.execute()
        env.verify()
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


@pytest.fixture(scope="module")
def cases():
    cases = [
        {
            'ware': '000000000000000000000w',
            'category': 111,
            'nids': [
                '2', '3',  # унаследовали от "все товары"
            ],
            'description':  u'Проверка на отбрасывание родителей рецепта',
        },
        {
            'ware': '000000000000000000001w',
            'category': 111,
            'nids': [
                '2', '3', '31', '311'
            ],
            'description':  u'bool фильтр для подходящего оффера',
        },
        {
            'ware': '000000000000000000002w',
            'category': 111,
            'nids': [
                '2', '3', '31', '312'
            ],
            'description':  u'number фильтр для подходящего оффера',
        },
        {
            'ware': '000000000000000000003w',
            'category': 111,
            'nids': [
                '2',  '3', '31', '313'
            ],
            'description':  u'enum фильтр для подходящего оффера',
        },
        # негативные пофильтрованные офферы
        {
            'ware': '000000000000000000004w',
            'category': 111,
            'nids': [
                '3', '2',
            ],
            'description':  u'больше верхнего ограничения на число, отбрасывание',
        },
        {
            'ware': '000000000000000000005w',
            'category': 111,
            'nids': [
                '3', '2',
            ],
            'description':  u'Меньше нижнего ограничения на число, отбрасывание',
        },
        {
            'ware': '000000000000000000006w',
            'category': 111,
            'nids': [
                '3', '2',
            ],
            'description':  u'enum фильтр, отбрасывание',
        },
        {
            'ware': '000000000000000000007w',
            'category': 111,
            'nids': [
                '3', '2',
            ],
            'description':  u'boolean ограничение, отбрасывание',
        },
        # модельный параметр
        {
            'ware': '000000000000000000008w',
            'category': 111,
            'nids': [
                '3', '2', '31', '314',
            ],
            'description':  u'boolean ограничение в модели',
        },
        # модельный + офферный параметр
        {
            'ware': '000000000000000000009w',
            'category': 111,
            'nids': [
                '3', '2', '31', '315',
            ],
            'description':  u' полностью подходит к 5му фильтру, где один из параметров - модельный',
        },
        {
            'ware': '000000000000000000010w',
            'category': 1111,
            'nids': [
                '3', '2', '31', '315',
            ],
            'description':  u'Дочерний к товарной категории полностью подходит к 5му фильтру, где один из параметров - модельный',
        },
        {
            'ware': '000000000000000000011w',
            'category': 1111,
            'nids': [
                '3', '2', '31', '314',
            ],
            'description':  u'Различие в один фильтр - не подходит к 5му фильтру, отбрасывание',
        },
        {
            'ware': '000000000000000000012w',
            'category': 111,
            'nids': [
                '3', '2', '31', '316', '315',
            ],
            'description':  u'Оффер с множеством указанных парметров, полностью, подходящий к 6му фильтру',
        },
        {
            'ware': '000000000000000000013w',
            'category': 111,
            'nids': [
                '3', '2',
            ],
            'description':  u'Офферу не хватает спецефицированных параметров, чтобы подходить под 6й фильтр, отбрасывание',
        },
        {
            'ware': '000000000000000000014w',
            'category': 111,
            'nids': [
                '3', '2', '31', '312',
            ],
            'description':  u'Включение верхней границы для числового фильтра',
        },
        {
            'ware': '000000000000000000015w',
            'category': 111,
            'nids': [
                '3', '2', '31', '312',
            ],
            'description':  u'Включение нижней границы для числового фильтра',
        },
        {
            'ware': '000000000000000000017w',
            'category': 112,
            'nids': [
                '3', '2', '31', '317', '3171',
            ],
            'description':  u'Проверка, что фильтры не наследуются',
        },
        {
            'ware': '000000000000000000018w',
            'category': 111,
            'nids': [
                '3', '2', '31', '318',
            ],
            'description':  u'Проверка, соответствия с множественными значениями enum фильтра',
        },
        {
            'ware': '000000000000000000019w',
            'category': 111,
            'nids': [
                '3', '2', '31', '312',
            ],
            'description':  u'Проверка, соответствия с множественными значениями num фильтра',
        },
        {
            'ware': '000000000000000000020w',
            'category': 111,
            'nids': [
                '3', '2',
            ],
            'description':  u'Проверка, несоответствия с множественными значениями num фильтра',
        },
        {
            'ware': '000000000000000000021w',
            'category': 111,
            'nids': [
                '318', '3', '2', '31',
            ],
            'description':  u'Проверка, соответствия с множественными значениями enum фильтра с несовпадающими мн-вами',
        },
        {
            'ware': '000000000000000000022w',
            'category': 111,
            'nids': [
                '3', '2', '31', '318',
            ],
            'description':  u'Проверка, соответствия с множественными значениями enum фильтра с несовпадающими мн-вами',
        },
    ]
    return cases


def test_accept_parents_recipe_nids(mr_mindexer_direct, cases):
    for case in cases:
        for nid in case['nids']:
            assert_that(
                mr_mindexer_direct,
                HasLiterals('#nid="' + nid, [OFFER2ID[make_offer_id(case['ware'], case['category'])]]),
                case['description']
            )
