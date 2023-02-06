# coding: utf-8

"""
Тест проверяет, что для моделей у которых в выгрузке MBO есть гипотезы значений parameter_value_hypothesis
эти значения прокидываются в сниппетные параметры MboModel
см. https://st.yandex-team.ru/MARKETPROJECT-3257
"""


import pytest

import base64
from hamcrest import assert_that

from market.idx.yatf.matchers.env_matchers import HasDocs
from market.idx.yatf.resources.mbo.models_pb import ModelsPb

from market.idx.models.yatf.test_envs.models_indexer import ModelsIndexerTestEnv

from market.proto.content.mbo.ExportReportModel_pb2 import (
    ExportReportModel,
    ParameterValueHypothesis,
    Video,
)
from market.proto.content.mbo.MboParameters_pb2 import ValueType, Word


MODEL_WITH_HYPOTHESIS_ID = 1
MODEL_WITHOUT_HYPOTHESIS_ID = 2


@pytest.fixture(scope="module")
def category_id():
    return 90592


@pytest.fixture(scope="module")
def vendor_id():
    return 1


@pytest.fixture(scope="module")
def models(category_id, vendor_id):
    return [
        ExportReportModel(
            id=MODEL_WITH_HYPOTHESIS_ID,
            category_id=category_id,
            vendor_id=vendor_id,
            current_type='GURU',
            published_on_market=True,
            parameter_value_hypothesis=[
                ParameterValueHypothesis(
                    param_id=1000100,
                    value_type=ValueType.NUMERIC_ENUM,
                    str_value=[Word(
                        name="1000100LHypo",
                    )],
                ),
            ],
            videos=[
                Video(url='www.youtube.com/watch?v=1'),
                Video(url='www.youtube.com/watch?v=2'),
            ]
        ),
        ExportReportModel(
            id=MODEL_WITHOUT_HYPOTHESIS_ID,
            category_id=category_id,
            vendor_id=vendor_id,
            current_type='GURU',
            published_on_market=True
        )
    ]


@pytest.fixture(scope="module")
def workflow(models, category_id):
    resources = {
        'models': ModelsPb(models, category_id),
    }
    with ModelsIndexerTestEnv(**resources) as env:
        env.execute()
        env.verify()
        yield env


def test_with_property(workflow, models):
    expected_mbo_model = ExportReportModel(
        parameter_value_hypothesis=models[0].parameter_value_hypothesis,
        videos=models[0].videos
    )
    assert_that(
        workflow,
        HasDocs()
        .attributes(hyper=str(MODEL_WITH_HYPOTHESIS_ID))
        .attributes(MboModel=base64.b64encode(expected_mbo_model.SerializeToString())),
        'Model with MboModel attribute')


def test_without_property(workflow):
    assert_that(
        workflow,
        HasDocs()
        .attributes(hyper=str(MODEL_WITHOUT_HYPOTHESIS_ID))
        .attributes(sale_begin_ts=None),
        'Model without MboModel attribute')
