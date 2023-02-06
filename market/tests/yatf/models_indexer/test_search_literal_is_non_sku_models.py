# coding: utf-8

"""
Тест проверяет проставление поискового литерала is_model_without_sku_offers
Этот литерал ставится моделям, у которых нет поскутченных офферов (в файле model2msku_white.csv)
"""


import pytest
from hamcrest import assert_that, all_of

from market.idx.yatf.matchers.env_matchers import HasDocs
from market.idx.yatf.resources.mbo.models_pb import ModelsPb
from market.idx.yatf.resources.mbo.parameters_pb import ParametersPb
from market.idx.models.yatf.test_envs.models_indexer import ModelsIndexerTestEnv
from market.idx.models.yatf.resources.models_indexer.model_to_msku import Model2Msku

from market.proto.content.mbo.ExportReportModel_pb2 import (
    ExportReportModel,
)
from market.proto.content.mbo.MboParameters_pb2 import Category


MODEL_WITH_SKU_OFFERS_ID = 1
MODEL_WITHOUT_SKU_OFFERS_ID = 2


@pytest.fixture(scope="module")
def category_parameters():
    return Category(
        hid=90592
    )


@pytest.fixture(scope="module")
def models(category_parameters):
    models = []

    model1 = ExportReportModel(id=MODEL_WITH_SKU_OFFERS_ID,
                               category_id=category_parameters.hid,
                               vendor_id=152712,
                               current_type='GURU',
                               published_on_market=True)
    models.append(model1)

    model1 = ExportReportModel(id=MODEL_WITHOUT_SKU_OFFERS_ID,
                               category_id=category_parameters.hid,
                               vendor_id=152712,
                               current_type='GURU',
                               published_on_market=True)
    models.append(model1)
    return models


@pytest.fixture(scope="module")
def model2msku():
    return {
        MODEL_WITH_SKU_OFFERS_ID: [10, 11],
        3: [30],
    }


@pytest.fixture(scope="module")
def workflow(models, category_parameters, model2msku):
    resources = {
        'models': ModelsPb(models, category_parameters.hid),
        'parameters': ParametersPb(category_parameters),
        'model2msku': Model2Msku(model2msku),
    }
    with ModelsIndexerTestEnv(**resources) as env:
        env.execute()
        env.verify()
        yield env


def test_model_with_sku_offers(workflow):
    assert_that(
        workflow,
        HasDocs()
        .attributes(hyper=str(MODEL_WITH_SKU_OFFERS_ID))
        .literals(is_model_without_sku_offers=None),
        'Model with sku offers')


def test_model_without_sku_offers(workflow):
    assert_that(
        workflow,
        HasDocs()
        .attributes(hyper=str(MODEL_WITHOUT_SKU_OFFERS_ID))
        .literals(is_model_without_sku_offers='1'),
        'Model without sku offers')


@pytest.fixture(scope="module")
def workflow_feature_disabled(models, category_parameters):
    resources = {
        'models': ModelsPb(models, category_parameters.hid),
        'parameters': ParametersPb(category_parameters),
    }
    with ModelsIndexerTestEnv(**resources) as env:
        env.execute()
        env.verify()
        yield env


def test_models_without_SL(workflow_feature_disabled):
    assert_that(
        workflow_feature_disabled,
        all_of(
            HasDocs()
                .attributes(hyper=str(MODEL_WITH_SKU_OFFERS_ID))
                .literals(is_model_without_sku_offers=None),
            HasDocs()
                .attributes(hyper=str(MODEL_WITHOUT_SKU_OFFERS_ID))
                .literals(is_model_without_sku_offers=None)
        ),
        'Models without is_model_without_sku_offers SL')
