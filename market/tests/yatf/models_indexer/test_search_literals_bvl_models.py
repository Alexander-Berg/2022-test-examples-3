# coding: utf-8

"""
Тест проверяет, что для моделей и кластеров, у которых есть MBO-параметры
лицензиар, франщиза, персонаж (licensor, hero_global, pers_model)
проставляются соответствующие поисковые литералы
"""


import pytest
from hamcrest import assert_that

from market.idx.yatf.matchers.env_matchers import HasDocs
from market.idx.yatf.resources.mbo.models_pb import ModelsPb
from market.idx.yatf.resources.mbo.parameters_pb import ParametersPb
from market.idx.models.yatf.test_envs.models_indexer import ModelsIndexerTestEnv
from market.idx.models.yatf.resources.models_indexer.cluster_pictures import ClusterPicturesMmap

from market.proto.content.mbo.ExportReportModel_pb2 import (
    ExportReportModel,
    ParameterValue,
)
from market.proto.content.mbo.MboParameters_pb2 import Category, Parameter


MODEL_ID_1 = 1
MODEL_ID_3 = 3


@pytest.fixture(scope="module", params=[90592])
def category_parameters(request):
    return Category(
        hid=request.param,
        parameter=[
            Parameter(
                id=15060326,
                xsl_name='licensor',
                published=True,
            ),
            Parameter(
                id=14020987,
                xsl_name='hero_global',
                published=True,
            ),
            Parameter(
                id=15086295,
                xsl_name='pers_model',
                published=True,
            ),
        ]
    )


@pytest.fixture(scope="module", params=[90592])
def category_parameters_not_published(request):
    return Category(
        hid=request.param,
        parameter=[
            Parameter(
                id=15060326,
                xsl_name='licensor',
            ),
            Parameter(
                id=14020987,
                xsl_name='hero_global',
                published=False,
            ),
        ]
    )


@pytest.fixture(scope="module")
def models(category_parameters):
    models = []

    model1 = ExportReportModel(id=MODEL_ID_1,
                               category_id=category_parameters.hid,
                               vendor_id=152712,
                               current_type='GURU',
                               published_on_market=True,
                               parameter_values=[
                                   ParameterValue(
                                       param_id=15060326,
                                       xsl_name='licensor',
                                       option_id=111,
                                   ),
                                   ParameterValue(
                                       param_id=14020987,
                                       xsl_name='hero_global',
                                       option_id=222,
                                   ),
                                   ParameterValue(
                                       param_id=15086295,
                                       xsl_name='pers_model',
                                       option_id=333,
                                   ),
                               ])
    models.append(model1)

    model2 = ExportReportModel(id=MODEL_ID_3,
                               category_id=category_parameters.hid,
                               vendor_id=152712,
                               current_type='GURU',
                               published_on_market=True,
                               parameter_values=[
                                   ParameterValue(
                                       param_id=15060326,
                                       xsl_name='licensor',
                                       option_id=555,
                                   ),
                                   ParameterValue(
                                       param_id=14020987,
                                       xsl_name='hero_global',
                                       option_id=0,
                                   ),
                                   ParameterValue(
                                       param_id=15086295,
                                       xsl_name='pers_model',
                                   ),
                               ])
    models.append(model2)
    return models


@pytest.fixture(scope="module")
def workflow(models, category_parameters):
    resources = {
        'models': ModelsPb(models, category_parameters.hid),
        'parameters': ParametersPb(category_parameters),
        'cluster_pictures_mmap': ClusterPicturesMmap([]),
    }
    with ModelsIndexerTestEnv(**resources) as env:
        env.execute()
        env.verify()
        yield env


@pytest.fixture(scope="module")
def workflow_not_published_params(models, category_parameters_not_published):
    resources = {
        'models': ModelsPb(models, category_parameters_not_published.hid),
        'parameters': ParametersPb(category_parameters_not_published),
        'cluster_pictures_mmap': ClusterPicturesMmap([]),
    }
    with ModelsIndexerTestEnv(**resources) as env:
        env.execute()
        env.verify()
        yield env


def test_model_bvl_search_literals(workflow):
    assert_that(
        workflow,
        HasDocs()
        .attributes(hyper=str(MODEL_ID_1))
        .literals(licensor='111', hero_global='222', pers_model='333'),
        'Для модели проставились все три поисковых литерала')


def test_model_wrong_mbo_param_values(workflow):
    assert_that(
        workflow,
        HasDocs()
        .attributes(hyper=str(MODEL_ID_3))
        .literals(licensor='555', hero_global=None, pers_model=None),
        'Для модели проставился лицензиар, франшиза не проставилась, т.к. равна 0, персонажа - нету')


def test_model_not_published_bvl_params(workflow_not_published_params):
    assert_that(
        workflow_not_published_params,
        HasDocs()
        .attributes(hyper=str(MODEL_ID_1))
        .literals(licensor=None, hero_global=None, pers_model=None),
        'Не проставлены литералы: licensor - нет признака published, hero_global - pulished = False, pers_model - нет параметра в категории')
