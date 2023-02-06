# coding: utf-8

import pytest
from market.idx.yatf.matchers.env_matchers import HasDocs
from hamcrest import assert_that, all_of
from market.proto.content.mbo.ExportReportModel_pb2 import (
    ExportReportModel,
    ParameterValue
)
from market.idx.models.yatf.test_envs.models_indexer import ModelsIndexerTestEnv
import market.proto.content.mbo.MboParameters_pb2 as MboParameters_pb2
from market.idx.yatf.resources.mbo.models_pb import ModelsPb
from market.idx.yatf.resources.mbo.parameters_pb import ParametersPb

CATEGORY_ID = 90592
PARAM_ID_EXCLUSIVE = 28530570
PARAM_ID_HYPE_GOODS = 27625090
PARAM_ID_RARE_ITEM = 33457410


@pytest.fixture(scope="function")
def category_parameters():
    return MboParameters_pb2.Category(
        hid=CATEGORY_ID,
        parameter=[
            MboParameters_pb2.Parameter(
                id=PARAM_ID_EXCLUSIVE,
                xsl_name='exclusive',
                common_filter_index=1,
                value_type=MboParameters_pb2.BOOLEAN
            ),
            MboParameters_pb2.Parameter(
                id=PARAM_ID_HYPE_GOODS,
                xsl_name='hype_goods',
                published=True,
                common_filter_index=1,
                value_type=MboParameters_pb2.BOOLEAN
            ),
            MboParameters_pb2.Parameter(
                id=PARAM_ID_RARE_ITEM,
                xsl_name='rare_item',
                published=True,
                common_filter_index=1,
                value_type=MboParameters_pb2.BOOLEAN
            )
        ]
    )


@pytest.fixture(scope="function")
def models():
    models = []

    def parameters(exclusive, hype_goods, rare_item):
        return [
            ParameterValue(
                param_id=PARAM_ID_EXCLUSIVE,
                xsl_name='exclusive',
                bool_value=exclusive
            ),
            ParameterValue(
                param_id=PARAM_ID_HYPE_GOODS,
                xsl_name='hype_goods',
                bool_value=hype_goods
            ),
            ParameterValue(
                param_id=PARAM_ID_RARE_ITEM,
                xsl_name='rare_item',
                bool_value=rare_item
                )
        ]

    num_of_params = 3
    for i in range(2 ** num_of_params):
        exclusive = bool(i & 0x1)
        hype_goods = bool(i & 0x2)
        rare_item = bool(i & 0x4)

        model = ExportReportModel(id=i,
                                  category_id=CATEGORY_ID,
                                  vendor_id=CATEGORY_ID,
                                  current_type='GURU',
                                  published_on_market=True,
                                  parameter_values=parameters(exclusive, hype_goods, rare_item))
        models.append(model)
    return models


@pytest.fixture(scope="function")
def resources(models, category_parameters):
    return {
        'models': ModelsPb(models, category_parameters.hid),
        'parameters': ParametersPb(category_parameters)
    }


@pytest.fixture(scope="function")
def workflow_indexer(resources):
    with ModelsIndexerTestEnv(**resources) as env:
        env.execute()
        env.verify()
        yield env


def test_hyper_flags(workflow_indexer):
    assert_that(workflow_indexer, all_of(
        HasDocs().attributes(hyper=str(0)),
        HasDocs().attributes(hyper=str(1)).attributes(exclusive='1'),
        HasDocs().attributes(hyper=str(2)).attributes(hype_goods='1'),
        HasDocs().attributes(hyper=str(3)).attributes(exclusive='1').attributes(hype_goods='1'),
        HasDocs().attributes(hyper=str(4)).attributes(rare_item='1'),
        HasDocs().attributes(hyper=str(5)).attributes(exclusive='1').attributes(rare_item='1'),
        HasDocs().attributes(hyper=str(6)).attributes(hype_goods='1').attributes(rare_item='1'),
        HasDocs().attributes(hyper=str(7)).attributes(exclusive='1').attributes(hype_goods='1').attributes(rare_item='1')
    ))
