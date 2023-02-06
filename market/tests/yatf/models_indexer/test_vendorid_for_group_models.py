# coding: utf-8

"""
Тест проверяет, что для групповых моделей, так же, как и для обычных,
проставляются поисковый литерал и групповой аттрибут vendor_id
Это нужно, чтобы в кабинете вендора отображались и групповые модели тоже
"""


import pytest
from hamcrest import assert_that

from market.idx.yatf.matchers.env_matchers import HasDocs
from market.idx.yatf.resources.mbo.models_pb import ModelsPb
from market.idx.yatf.resources.mbo.parameters_pb import ParametersPb
from market.idx.models.yatf.test_envs.models_indexer import ModelsIndexerTestEnv

from market.proto.content.mbo.ExportReportModel_pb2 import (
    ExportReportModel,
)
from market.proto.content.mbo.MboParameters_pb2 import Category


SIMPLE_MODEL_ID = 1
GROUP_MODEL_ID = 2
SIMPLE_MODEL_VENDOR_ID = 10
GROUP_MODEL_VENDOR_ID = 20


@pytest.fixture(scope="module", params=[90592])
def category_parameters(request):
    return Category(
        hid=request.param,
    )


@pytest.fixture(scope="module")
def models(category_parameters):
    models = []

    model1 = ExportReportModel(id=SIMPLE_MODEL_ID,
                               category_id=category_parameters.hid,
                               vendor_id=SIMPLE_MODEL_VENDOR_ID,
                               current_type='GURU',
                               published_on_market=True,
                               )
    models.append(model1)

    model2 = ExportReportModel(id=GROUP_MODEL_ID,
                               category_id=category_parameters.hid,
                               vendor_id=GROUP_MODEL_VENDOR_ID,
                               current_type='GURU',
                               group_size=5,  # Это признак групповой модели для модельного индексатора
                               published_on_market=True,
                               )
    models.append(model2)
    return models


@pytest.fixture(scope="module")
def workflow(models, category_parameters):
    resources = {
        'models': ModelsPb(models, category_parameters.hid),
        'parameters': ParametersPb(category_parameters),
    }
    with ModelsIndexerTestEnv(**resources) as env:
        env.execute()
        env.verify()
        yield env


def test_vendor_id_for_simple_model(workflow):
    assert_that(
        workflow,
        HasDocs()
        .attributes(hyper=str(SIMPLE_MODEL_ID), vendor_id=str(SIMPLE_MODEL_VENDOR_ID))
        .literals(vendor_id=str(SIMPLE_MODEL_VENDOR_ID)),
        'Для обычной модели проставились SL и GA vendor_id, что и неудивительно')


def test_vendor_id_for_group_model(workflow):
    assert_that(
        workflow,
        HasDocs()
        .attributes(hyper=str(GROUP_MODEL_ID), vendor_id=str(GROUP_MODEL_VENDOR_ID))
        .literals(vendor_id=str(GROUP_MODEL_VENDOR_ID)),
        'Для групповой модели тоже проставились SL и GA vendor_id')
