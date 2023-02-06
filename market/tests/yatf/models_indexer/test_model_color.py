# coding: utf-8

"""Проверяет что флажок published_on_market=True из выгрузки MBO
доезжает до модельного индекса как серчилтерал model_color=white.
"""

import pytest
from hamcrest import assert_that

from market.idx.yatf.matchers.env_matchers import (
    HasDocs,
)
from market.idx.yatf.resources.mbo.models_pb import ModelsPb
from market.idx.yatf.resources.mbo.parameters_pb import ParametersPb
from market.idx.models.yatf.test_envs.models_indexer import ModelsIndexerTestEnv
from market.proto.content.mbo.ExportReportModel_pb2 import (
    ExportReportModel,
)
from market.proto.content.mbo.MboParameters_pb2 import Category


@pytest.fixture(scope="module")
def category_parameters():
    return Category(
        hid=90592
    )


@pytest.fixture(scope="module")
def models(category_parameters):
    return [
        ExportReportModel(
            id=2441135,
            category_id=category_parameters.hid,
            vendor_id=152712,
            current_type='GURU',
            published_on_market=False,
            published_on_blue_market=True,
        ),
        ExportReportModel(
            id=2441136,
            category_id=category_parameters.hid,
            vendor_id=152712,
            current_type='GURU',
            published_on_market=True,
        ),
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


def test_blue_model(workflow):
    assert_that(
        workflow,
        HasDocs().attributes(hyper='2441135')
                 .attributes(hyper_ts='2441135')
                 .attributes(hyper_ts_blue='2441135')
                 .literals(model_color=None)
    )


def test_white_model(workflow):
    assert_that(
        workflow,
        HasDocs().attributes(hyper='2441136')
                 .attributes(hyper_ts='2441136')
                 .attributes(hyper_ts_blue='2441136')
                 .literals(model_color='white')
    )
