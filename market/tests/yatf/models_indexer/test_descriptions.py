# coding: utf-8

import pytest
from hamcrest import assert_that

from market.idx.yatf.matchers.env_matchers import HasDocs
from market.idx.yatf.resources.mbo.models_pb import ModelsPb
from market.idx.yatf.resources.mbo.parameters_pb import ParametersPb
from market.idx.models.yatf.test_envs.models_indexer import ModelsIndexerTestEnv
from market.proto.content.mbo.ExportReportModel_pb2 import ExportReportModel, LocalizedString, ParameterValue
from market.proto.content.mbo.MboParameters_pb2 import Category


@pytest.fixture(scope='module')
def category_parameters():
    return Category(
        hid=90592
    )


@pytest.fixture(scope='module')
def models(category_parameters):
    return [
        ExportReportModel(
            id=2441135,
            category_id=category_parameters.hid,
            published_on_market=True,
            current_type='GURU',
            parameter_values=[
                ParameterValue(
                    param_id=15341921,
                    xsl_name='description',
                    str_value=[
                        LocalizedString(value='Ребята, напишите сюда описание товара', isoCode='ru')
                    ]
                )
            ],
        ),
        ExportReportModel(
            id=2441136,
            category_id=category_parameters.hid,
            published_on_market=True,
            current_type='GURU',
            parameter_values=[
                ParameterValue(
                    param_id=15341921,
                    xsl_name='description',
                    str_value=[
                        LocalizedString(value='Какое описание выбрать?', isoCode='ru'),
                        LocalizedString(value='Нам сказали, выбирать самое длинное описание!', isoCode='ru'),
                        LocalizedString(value='Use only russian descriptions, even if it is shortest', isoCode='en'),
                    ],
                ),
            ],
        ),
    ]


@pytest.fixture(scope='module')
def workflow(category_parameters, models):
    resources = {
        'models': ModelsPb(models, category_parameters.hid),
        'parameters': ParametersPb(category_parameters),
    }
    with ModelsIndexerTestEnv(**resources) as env:
        env.execute()
        env.verify()
        yield env


def test_full_description_field(workflow):
    assert_that(
        workflow,
        HasDocs().attributes(hyper='2441135')
                 .attributes(FullDescription='Ребята, напишите сюда описание товара'))


def test_pick_longest_description(workflow):
    assert_that(
        workflow,
        HasDocs().attributes(hyper='2441136')
                 .attributes(FullDescription='Нам сказали, выбирать самое длинное описание!'))
