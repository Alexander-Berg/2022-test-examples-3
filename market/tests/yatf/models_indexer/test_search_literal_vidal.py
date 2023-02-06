# coding: utf-8

"""Проверяем что поле "ATCCode 23181290 j05ax13" из выгрузки MBO передается до
модельного индекса как поисковый литерал vidal_atc_code=j05ax13.
"""

import pytest
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
from market.proto.content.mbo.MboParameters_pb2 import Category


VIDAL_MODEL_ID=1
VIDAL_CATEGORY_ID=90592
VIDAL_ATC_CODE=23181290
VIDAL_XLS_NAME='ATCCode'
VIDAL_ATC_CODE_VALUE='j05ax13'


@pytest.fixture(scope="module")
def category_parameters():
    return Category(
        hid=VIDAL_CATEGORY_ID
    )


@pytest.fixture(scope="module")
def models(category_parameters):
    return [
        ExportReportModel(
            id=VIDAL_MODEL_ID,
            category_id=category_parameters.hid,
            current_type='GURU',
            published_on_market=True,
            parameter_values=[
                ParameterValue(
                    param_id=VIDAL_ATC_CODE,
                    xsl_name=VIDAL_XLS_NAME,
                    str_value=[
                        LocalizedString(value=VIDAL_ATC_CODE_VALUE)
                    ]
                )
            ]
        ),
        ExportReportModel(
            id=VIDAL_MODEL_ID+1,
            category_id=category_parameters.hid,
            current_type='GURU',
            published_on_market=True,
            parameter_values=[
                ParameterValue(
                    param_id=VIDAL_ATC_CODE+1,
                    xsl_name=VIDAL_XLS_NAME,
                    str_value=[
                        LocalizedString(value=VIDAL_ATC_CODE_VALUE)
                    ]
                )
            ]
        ),
        ExportReportModel(
            id=VIDAL_MODEL_ID+2,
            category_id=category_parameters.hid,
            current_type='GURU',
            published_on_market=True,
            parameter_values=[
                ParameterValue(
                    param_id=VIDAL_ATC_CODE,
                    xsl_name=VIDAL_XLS_NAME,
                    str_value=[]
                )
            ]
        ),
        ExportReportModel(
            id=VIDAL_MODEL_ID+3,
            category_id=category_parameters.hid,
            current_type='GURU',
            published_on_market=True,
            parameter_values=[
                ParameterValue(
                    param_id=VIDAL_ATC_CODE,
                    xsl_name=VIDAL_XLS_NAME,
                    str_value=[
                        LocalizedString(value="")
                    ]
                )
            ]
        )
    ]


@pytest.yield_fixture(scope="module")
def workflow(models, category_parameters):
    resources = {
        'models': ModelsPb(models, category_parameters.hid),
        'parameters': ParametersPb(category_parameters),
    }
    with ModelsIndexerTestEnv(**resources) as env:
        env.execute()
        env.verify()
        yield env


def test_model_with_vidal_params(workflow):
    assert_that(
        workflow,
        HasDocs()
        .attributes(hyper=str(VIDAL_MODEL_ID))
        .literals(vidal_atc_code=VIDAL_ATC_CODE_VALUE),
        'Model has vidal params and vidal atc code')


def test_model_without_vidal_params(workflow):
    assert_that(
        workflow,
        HasDocs()
        .attributes(hyper=str(VIDAL_MODEL_ID+1))
        .literals(vidal_atc_code=None),
        'Model has not vidal params')


def test_model_with_null_vidal_params(workflow):
    assert_that(
        workflow,
        HasDocs()
        .attributes(hyper=str(VIDAL_MODEL_ID+2))
        .literals(vidal_atc_code=None),
        'Model has vidal params but vidal atc code is absent')


def test_model_with_empty_vidal_params(workflow):
    assert_that(
        workflow,
        HasDocs()
        .attributes(hyper=str(VIDAL_MODEL_ID+3))
        .literals(vidal_atc_code=None),
        'Model has vidal params but vidal atc code is empty')
