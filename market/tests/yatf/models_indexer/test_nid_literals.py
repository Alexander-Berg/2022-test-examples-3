# coding: utf-8

"""
Проверяем работу простановки нид литералов и логику model-lists в нав дереве
"""

import pytest
from hamcrest import assert_that

from market.idx.yatf.matchers.env_matchers import HasDocs
from market.idx.yatf.resources.mbo.models_pb import ModelsPb
from market.idx.yatf.resources.mbo.parameters_pb import ParametersPb
from market.proto.content.mbo.MboParameters_pb2 import Category
from market.idx.models.yatf.test_envs.models_indexer import ModelsIndexerTestEnv
from market.proto.content.mbo.ExportReportModel_pb2 import ExportReportModel, LocalizedString, ParameterValue
from market.idx.yatf.resources.tovar_tree_pb import MboCategory, TovarTreePb
from market.idx.yatf.resources.mbo.cataloger_navigation_xml import CatalogerNavigationXml, NavigationTree, NavigationNode, ModelLists, ModelList

from pictures import Pictures


@pytest.fixture(scope="module")
def tovar_tree():
    return [
        MboCategory(
            hid=1, tovar_id=0,
            unique_name="Все товары", name="Все товары",
            output_type=MboCategory.GURULIGHT),
        MboCategory(
            hid=500, tovar_id=2, parent_hid=1,
            unique_name="Товары по ведьмаку", name="Товары по ведьмаку",
            output_type=MboCategory.GURULIGHT),
        MboCategory(
            hid=501, tovar_id=3, parent_hid=500,
            unique_name="Ведьмаки и Чародейки", name="Ведьмаки и Чародейки",
            output_type=MboCategory.GURULIGHT),
        MboCategory(
            hid=502, tovar_id=4, parent_hid=500,
            unique_name="Нильфгаард", name="Нильфгаард",
            output_type=MboCategory.GURULIGHT),
    ]


@pytest.fixture(scope="module")
def nav_tree_blue_part():
    return NavigationTree(
        123,
        NavigationNode(
            nid=1, hid=1,
            children=[
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
        ModelList(id=100502, models=[3220, 3224]),
        ModelList(id=100503, models=[3223]),
    ])


def pictures2():
    return Pictures(2, 2)


def gen_name_param(name):
    return ParameterValue(xsl_name="name", str_value=[LocalizedString(isoCode="ru", value=name)])


@pytest.fixture(scope="module")
def categ_to_models():
    return {
        502: [
            ExportReportModel(id=3220,
                              category_id=502,
                              vendor_id=502,
                              current_type='GURU',
                              published_on_market=True,
                              pictures=pictures2().proto_pictures(),
                              title_without_vendor=[LocalizedString(isoCode="ru", value="504_3220")],
                              parameter_values=[gen_name_param("504_3220")]),
            ExportReportModel(id=3221,
                              category_id=502,
                              vendor_id=502,
                              current_type='GURU',
                              published_on_market=True,
                              pictures=pictures2().proto_pictures(),
                              title_without_vendor=[LocalizedString(isoCode="ru", value="504_3221")],
                              parameter_values=[gen_name_param("504_3221")]),
            ExportReportModel(id=3224,
                              category_id=502,
                              vendor_id=502,
                              current_type='GURU',
                              published_on_market=True,
                              pictures=pictures2().proto_pictures(),
                              title_without_vendor=[LocalizedString(isoCode="ru", value="504_3224")],
                              parameter_values=[gen_name_param("504_3224")]),
            ExportReportModel(id=322,
                              category_id=502,
                              vendor_id=502,
                              current_type='GURU',
                              published_on_market=True,
                              pictures=pictures2().proto_pictures(),
                              title_without_vendor=[LocalizedString(isoCode="ru", value="504_322")],
                              parameter_values=[gen_name_param("504_322")])
        ],
        501: [
            ExportReportModel(id=3223,
                              category_id=501,
                              vendor_id=501,
                              current_type='GURU',
                              published_on_market=True,
                              pictures=pictures2().proto_pictures(),
                              title_without_vendor=[LocalizedString(isoCode="ru", value="505_3223")],
                              parameter_values=[gen_name_param("505_3223")]),
        ]
    }


@pytest.fixture(scope="module")
def category_parameters():
    return [Category(hid=hid) for hid in [1, 500, 501, 502]]


@pytest.fixture(scope="module")
def workflow_indexer(categ_to_models, category_parameters, tovar_tree, nav_tree_blue_part, model_lists):
    resources = {
        'models': [
            ModelsPb(categ_to_models[hid], hid)
            for hid in categ_to_models
        ],
        'parameters': [
            ParametersPb(category)
            for category in category_parameters
        ],
        'tovar_categories': TovarTreePb(tovar_tree),
        'navigation_categories_all': CatalogerNavigationXml(
            filename="cataloger.navigation.all.xml",
            nav_trees=[nav_tree_blue_part],
            model_lists=model_lists
        ),
    }
    with ModelsIndexerTestEnv(**resources) as env:
        env.execute()
        env.verify()
        yield env


def test_nid_literals(workflow_indexer):
    def check_model_nid_literals(model_id, hid, literals):
        assert_that(
            workflow_indexer,
            HasDocs()
            .attributes(hyper=str(model_id))
            .attributes(hidd=str(hid))
            .literals(nid=literals),
        'Модель с категорией из дерева с model-lists')

    # По-умолчанию для hid-а 502 должны быть ниды 1, 100, 101, 1102, 1103
    # Хотя нид 102 является родителем к ноде с хидом 502, он должен отсутствовать, тк является списком моделей
    # Модель не из списков:
    check_model_nid_literals(model_id=322, hid=502, literals=[1, 100, 101, 1102, 1103])

    # Для модели 3220 должны добавиться ниды 1101, 1104, 103
    # Тк эта модель есть в их списках
    check_model_nid_literals(model_id=3220, hid=502, literals=[1, 100, 101, 1102, 1103, 1101, 1104, 103])

    # Для модели 3221 должен добавиться нид 1101
    # Тк эта модель есть в его списке
    check_model_nid_literals(model_id=3221, hid=502, literals=[1, 100, 101, 1102, 1103, 1101])

    # Для модели 3224 должен добавиться нид 1104, 103
    # Тк эта модель есть в его списке
    check_model_nid_literals(model_id=3224, hid=502, literals=[1, 100, 101, 1102, 1103, 1104, 103])

    # Модель 3223 с категорией 501 должена иметь литералы 1, 100, 101 из дерева и 102, 104, 1105  из списков моделей 100503
    check_model_nid_literals(model_id=3223, hid=501, literals=[1, 100, 101, 102, 104, 1105])
