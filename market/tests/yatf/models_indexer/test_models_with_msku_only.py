# coding: utf-8
import pytest
from hamcrest import assert_that, all_of

from market.idx.yatf.matchers.env_matchers import HasDocs
from market.idx.yatf.resources.mbo.models_pb import ModelsPb
from market.idx.yatf.resources.mbo.parameters_pb import ParametersPb
from market.idx.models.yatf.test_envs.models_indexer import ModelsIndexerTestEnv
from market.proto.content.mbo.ExportReportModel_pb2 import (
    ExportReportModel,
    Relation,
    SKU_MODEL,
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
            published_on_market=True,
        ),

        ExportReportModel(
            id=2441136,
            category_id=category_parameters.hid,
            vendor_id=152712,
            current_type='GURU',
            published_on_market=True,
            relations=[
                Relation(type=SKU_MODEL)
            ]
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


@pytest.yield_fixture(scope="module")
def blue_workflow(models, category_parameters):
    resources = {
        'models': ModelsPb(models, category_parameters.hid),
        'parameters': ParametersPb(category_parameters),
    }
    with ModelsIndexerTestEnv(only_with_msku=True, **resources) as env:
        env.execute()
        env.verify()
        yield env


def test_white_mode(workflow):
    """Test that model without msku will be accepted in white mode.
    """
    assert_that(workflow, all_of(
        HasDocs().attributes(hyper=str(2441135)).count(1),
        HasDocs().attributes(hyper=str(2441136)).count(1),
    ))


def test_blue_mode(blue_workflow):
    """Test that model without msku will be rejected in blue mode.
    """
    assert_that(blue_workflow, all_of(
        HasDocs().attributes(hyper=str(2441135)).count(0),
        HasDocs().attributes(hyper=str(2441136)).count(1),
    ))
