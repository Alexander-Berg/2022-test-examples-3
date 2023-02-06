# coding: utf-8

"""
Проверяем передачу данных из файла 'model_hypes.gz' до отдельных полей в офферах
в GenerationLogger.cpp::DoCreateGenlogRecord().
"""

import pytest
import time

from market.idx.offers.yatf.resources.offers_indexer.model_hypes import ModelHypes
from market.idx.offers.yatf.resources.offers_indexer.ytfeed import YtFeed
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.utils.fixtures import default_offer
from market.proto.content.mbo.ExportReportModel_pb2 import (
    ExportReportModel,
    ParameterValue
)


SESSION_ID = int(time.time())

MODEL_ID_WITH_ALL_HYPES=1
MODEL_ID_WITH_EXCLUSIVE=2
MODEL_ID_WITH_HYPE_GOODS=3
MODEL_ID_WITH_RARE_ITEM=4

EXCLUSIVE_PARAMETER_ID=28530570
HYPE_GOODS_PARAMETER_ID=27625090
RARE_ITEM_PARAMETER_ID=33457410


def create_offer(model_id):
    offer=default_offer()
    offer['session_id']=SESSION_ID
    offer['model_id']=model_id
    offer['matched_id']=model_id
    return offer


@pytest.fixture(scope="module")
def create_offers():
    return [
        create_offer(MODEL_ID_WITH_ALL_HYPES),
        create_offer(MODEL_ID_WITH_EXCLUSIVE),
        create_offer(MODEL_ID_WITH_HYPE_GOODS),
        create_offer(MODEL_ID_WITH_RARE_ITEM),
    ]


@pytest.fixture(scope="module")
def create_models():
    with_all_hypes=ExportReportModel(
        id=MODEL_ID_WITH_ALL_HYPES,
        parameter_values=[
            ParameterValue(
                param_id=EXCLUSIVE_PARAMETER_ID,
                bool_value=True,
            ),
            ParameterValue(
                param_id=HYPE_GOODS_PARAMETER_ID,
                bool_value=True,
            ),
            ParameterValue(
                param_id=RARE_ITEM_PARAMETER_ID,
                bool_value=True,
            ),
        ]
    )

    with_exclusive=ExportReportModel(
        id=MODEL_ID_WITH_EXCLUSIVE,
        parameter_values=[
            ParameterValue(
                param_id=EXCLUSIVE_PARAMETER_ID,
                bool_value=True,
            ),
        ]
    )

    with_hype_goods=ExportReportModel(
        id=MODEL_ID_WITH_HYPE_GOODS,
        parameter_values=[
            ParameterValue(
                param_id=HYPE_GOODS_PARAMETER_ID,
                bool_value=True,
            ),
        ]
    )

    with_rare_item=ExportReportModel(
        id=MODEL_ID_WITH_RARE_ITEM,
        parameter_values=[
            ParameterValue(
                param_id=RARE_ITEM_PARAMETER_ID,
                bool_value=True,
            ),
        ]
    )

    return ModelHypes([
        with_all_hypes,
        with_exclusive,
        with_hype_goods,
        with_rare_item,
    ])


@pytest.yield_fixture(scope="module")
def workflow(create_offers, create_models, yt_server):
    resources = {
        'feed': YtFeed.from_list(yt_server.get_yt_client(), create_offers),
        'model_hypes': create_models,
    }

    with OffersProcessorTestEnv(yt_server, **resources) as env:
        env.execute()
        env.verify()
        yield env


def test_all_hypes(workflow):
    assert workflow.genlog[MODEL_ID_WITH_ALL_HYPES-1].exclusive == '1'
    assert workflow.genlog[MODEL_ID_WITH_ALL_HYPES-1].hype_goods
    assert workflow.genlog[MODEL_ID_WITH_ALL_HYPES-1].rare_item


def test_just_exclusive(workflow):
    assert workflow.genlog[MODEL_ID_WITH_EXCLUSIVE-1].exclusive == '1'


def test_just_hype_goods(workflow):
    assert workflow.genlog[MODEL_ID_WITH_HYPE_GOODS-1].hype_goods


def test_just_rare_item(workflow):
    assert workflow.genlog[MODEL_ID_WITH_RARE_ITEM-1].rare_item
