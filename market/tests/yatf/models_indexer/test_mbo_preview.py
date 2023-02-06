# coding: utf-8
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
            parameter_values=[
                ParameterValue(
                    xsl_name='BarCode',
                    str_value=[
                        LocalizedString(
                            isoCode='ru',
                            value='asdf',
                        ),
                        LocalizedString(
                            isoCode='ru',
                            value='qwerty',
                        ),
                    ],
                )
            ],
        ),

        ExportReportModel(
            id=2441136,
            category_id=category_parameters.hid,
            vendor_id=152712,
            current_type='GURU',
            published_on_market=False,
            published_on_blue_market=False,
            parameter_values=[
                ParameterValue(
                    xsl_name='BarCode',
                    str_value=[
                        LocalizedString(
                            isoCode='ru',
                            value='asdf',
                        ),
                        LocalizedString(
                            isoCode='ru',
                            value='qwerty',
                        ),
                    ],
                )
            ],
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
def mbo_preview_workflow(models, category_parameters):
    resources = {
        'models': ModelsPb(models, category_parameters.hid),
        'parameters': ParametersPb(category_parameters),
    }
    with ModelsIndexerTestEnv(**resources) as env:
        env.mbo_preview_mode = True
        env.execute()
        env.verify()
        yield env


def test_normal_mode(workflow):
    """Tests that unpublished models don't get into the index
    in the normal mode.
    """
    assert_that(
        workflow,
        HasDocs().attributes(hyper=str(2441136)).count(0),
    )


def test_mbo_preview_mode(mbo_preview_workflow):
    """Tests that unpublished models do get into the index
    in the MBO Preview mode.
    """
    assert_that(
        mbo_preview_workflow,
        HasDocs().attributes(hyper=str(2441136)).count(1),
    )
