# coding: utf-8

"""
Тест проверяет, что для моделей у которых в выгрузке MBO есть параметр SaleDate эта дата правильно дезжает до
сниппетного параметра sale_begin_ts.
см. https://st.yandex-team.ru/MARKETOUT-31908
"""


import pytest
import datetime
import calendar
from hamcrest import assert_that

from market.idx.yatf.matchers.env_matchers import HasDocs
from market.idx.yatf.resources.mbo.models_pb import ModelsPb
from market.idx.models.yatf.test_envs.models_indexer import ModelsIndexerTestEnv

from market.proto.content.mbo.ExportReportModel_pb2 import (
    ExportReportModel,
    ParameterValue,
    LocalizedString,
)


MODEL_WITH_SALE_DATE_ID = 1
MODEL_WITHOUT_SALE_DATE_ID = 2
MODEL_WITH_SALE_DATE_GLOB_ID = 3
MODEL_WITH_SALE_DATE_AND_SALE_DATE_GLOB_ID = 4

SALE_DATE = datetime.date.today()
SALE_DATE_YESTURDAY = datetime.date.today() - datetime.timedelta(days=1)


@pytest.fixture(scope="module")
def category_id():
    return 90592


@pytest.fixture(scope="module")
def vendor_id():
    return 1


@pytest.fixture(scope="module")
def models(category_id):
    models = []

    model1 = ExportReportModel(id=MODEL_WITH_SALE_DATE_ID,
                               category_id=category_id,
                               vendor_id=152712,
                               current_type='GURU',
                               published_on_market=True,
                               parameter_values=[
                                   ParameterValue(
                                       param_id=4925810,
                                       xsl_name='SaleDate',
                                       str_value=[
                                           LocalizedString(
                                               value=SALE_DATE.strftime('%Y-%m-%d'),
                                               isoCode='ru',
                                           ),
                                       ]
                                   ),
                               ])
    models.append(model1)

    model2 = ExportReportModel(id=MODEL_WITHOUT_SALE_DATE_ID,
                               category_id=category_id,
                               vendor_id=152712,
                               current_type='GURU',
                               published_on_market=True)
    models.append(model2)

    model3 = ExportReportModel(id=MODEL_WITH_SALE_DATE_GLOB_ID,
                               category_id=category_id,
                               vendor_id=152712,
                               current_type='GURU',
                               published_on_market=True,
                               parameter_values=[
                                   ParameterValue(
                                       param_id=4925810,
                                       xsl_name='SaleDateGlob',
                                       str_value=[
                                           LocalizedString(
                                               value=SALE_DATE_YESTURDAY.strftime('%Y-%m-%d'),
                                               isoCode='ru',
                                           ),
                                       ]
                                   ),
                               ])

    models.append(model3)

    model4 = ExportReportModel(id=MODEL_WITH_SALE_DATE_AND_SALE_DATE_GLOB_ID,
                               category_id=category_id,
                               vendor_id=152712,
                               current_type='GURU',
                               published_on_market=True,
                               parameter_values=[
                                   ParameterValue(
                                       param_id=4925811,
                                       xsl_name='SaleDateGlob',
                                       str_value=[
                                           LocalizedString(
                                               value=SALE_DATE_YESTURDAY.strftime('%Y-%m-%d'),
                                               isoCode='ru',
                                           ),
                                       ]
                                   ),
                                   ParameterValue(
                                       param_id=4925810,
                                       xsl_name='SaleDate',
                                       str_value=[
                                           LocalizedString(
                                               value=SALE_DATE.strftime('%Y-%m-%d'),
                                               isoCode='ru',
                                           ),
                                       ]
                                   ),
                               ])

    models.append(model4)

    return models


@pytest.fixture(scope="module")
def workflow(models, category_id):
    resources = {
        'models': ModelsPb(models, category_id),
    }
    with ModelsIndexerTestEnv(**resources) as env:
        env.execute()
        env.verify()
        yield env


def test_with_property(workflow):
    assert_that(
        workflow,
        HasDocs()
        .attributes(hyper=str(MODEL_WITH_SALE_DATE_ID))
        .attributes(sale_begin_ts=str(calendar.timegm(SALE_DATE.timetuple()))),
        'Model with sale_begin_ts attribute')


def test_with_property_glob(workflow):
    assert_that(
        workflow,
        HasDocs()
        .attributes(hyper=str(MODEL_WITH_SALE_DATE_GLOB_ID))
        .attributes(sale_begin_ts=str(calendar.timegm(SALE_DATE_YESTURDAY.timetuple()))),
        'Model with sale_begin_ts attribute generated from SaleDateGlob')


def test_with_property_glob_prefered(workflow):
    assert_that(
        workflow,
        HasDocs()
        .attributes(hyper=str(MODEL_WITH_SALE_DATE_AND_SALE_DATE_GLOB_ID))
        .attributes(sale_begin_ts=str(calendar.timegm(SALE_DATE_YESTURDAY.timetuple()))),
        'Model with sale_begin_ts attribute generated from SaleDateGlob and prefered ofer SaleDate')


def test_without_property(workflow):
    assert_that(
        workflow,
        HasDocs()
        .attributes(hyper=str(MODEL_WITHOUT_SALE_DATE_ID))
        .attributes(sale_begin_ts=None),
        'Model without sale_begin_ts attribute')
