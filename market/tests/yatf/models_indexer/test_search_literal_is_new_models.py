# coding: utf-8

"""
Тест проверяет, что для моделей-новинок проставляется поисковый литерал is_new
Логику проставления признака is_new на основе MBO-параметров OldestDate, ModelYear, SaleDate и текущего времени
- см. MARKETOUT-427, CalcIsNew (indexer/models/common/model_info.cpp)
В данном тесте проверяем не всю логику, а только факт проставления поискового литерала
 - берем простой случай, когда заполнен только MBO-параметр OldestDate
"""


import pytest
import datetime
from hamcrest import assert_that

from market.idx.yatf.matchers.env_matchers import HasDocs
from market.idx.yatf.resources.mbo.models_pb import ModelsPb
from market.idx.yatf.resources.mbo.parameters_pb import ParametersPb
from market.idx.models.yatf.test_envs.models_indexer import ModelsIndexerTestEnv

from market.proto.content.mbo.ExportReportModel_pb2 import (
    ExportReportModel,
    ParameterValue,
    LocalizedString,
)
from market.proto.content.mbo.MboParameters_pb2 import Category, Parameter


NEW_MODEL_ID = 1
OLD_MODEL_ID = 2

IS_NEW_LIMIT_DAYS = 60
NEW_MODEL_DATE = datetime.datetime.now() - datetime.timedelta(IS_NEW_LIMIT_DAYS - 1)
OLD_MODEL_DATE = datetime.datetime.now() - datetime.timedelta(IS_NEW_LIMIT_DAYS + 1)


@pytest.fixture(scope="module", params=[90592])
def category_parameters(request):
    return Category(
        hid=request.param,
        parameter=[
            Parameter(
                id=7351723,
                xsl_name='OldestDate',
                published=True,
            ),
        ]
    )


@pytest.fixture(scope="module")
def models(category_parameters):
    models = []

    model1 = ExportReportModel(id=NEW_MODEL_ID,
                               category_id=category_parameters.hid,
                               vendor_id=152712,
                               current_type='GURU',
                               published_on_market=True,
                               parameter_values=[
                                   ParameterValue(
                                       param_id=7351723,
                                       xsl_name='OldestDate',
                                       str_value=[
                                           LocalizedString(
                                               value=NEW_MODEL_DATE.strftime('%d.%m.%Y %H:%M:%S'),
                                               isoCode='ru',
                                           ),
                                       ]
                                   ),
                               ])
    models.append(model1)

    model1 = ExportReportModel(id=OLD_MODEL_ID,
                               category_id=category_parameters.hid,
                               vendor_id=152712,
                               current_type='GURU',
                               published_on_market=True,
                               parameter_values=[
                                   ParameterValue(
                                       param_id=7351723,
                                       xsl_name='OldestDate',
                                       str_value=[
                                           LocalizedString(
                                               value=OLD_MODEL_DATE.strftime('%d.%m.%Y %H:%M:%S'),
                                               isoCode='ru',
                                           ),
                                       ]
                                   ),
                               ])
    models.append(model1)
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


def test_new_model(workflow):
    assert_that(
        workflow,
        HasDocs()
        .attributes(hyper=str(NEW_MODEL_ID))
        .literals(is_new='1'),
        'New model with SL is_new')


def test_old_model(workflow):
    assert_that(
        workflow,
        HasDocs()
        .attributes(hyper=str(OLD_MODEL_ID))
        .literals(is_new=None),
        'Old model without SL is_new')
