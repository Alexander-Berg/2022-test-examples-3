# coding=utf-8
import pytest

import market.proto.feedparser.deprecated.OffersData_pb2

from market.proto.content.mbo.MboParameters_pb2 import Category
from msku_uploader.yatf.test_env import MskuUploaderTestEnv

from msku_uploader.yatf.resources.mbo_pb import (
    MboCategoryPb,
    MboModelPb,
    MboSkuPb,
)
from msku_uploader.yatf.utils import (
    make_sku_protobuf,
    make_model_protobuf
)


CATEG_ID = 989040
MODEL_TITLE_MODEL_ID = 1713074441
PARTNER_MODEL_ID = 1713074442


@pytest.fixture(scope='module')
def mbo_msku_protobufs():
    mtitle_sku = make_sku_protobuf(
        100000000004,
        'SKU с модельным заголовком',
        MODEL_TITLE_MODEL_ID,
        CATEG_ID
    )
    mtitle_psku = make_sku_protobuf(
        100000000005,
        'PSKU с модельным заголовком',
        PARTNER_MODEL_ID,
        CATEG_ID,
        is_partner_sku=True
    )

    # order does matter
    return [
        mtitle_sku,
        mtitle_psku,
    ]


@pytest.fixture(scope='module')
def mbo_models_protobufs():
    return [
        make_model_protobuf(
            MODEL_TITLE_MODEL_ID,
            CATEG_ID
        ),
        make_model_protobuf(
            PARTNER_MODEL_ID,
            CATEG_ID,
            title="Партнёрский модельный заголовок",
            is_partner_model=True
        ),
    ]


@pytest.fixture(scope='module')
def mbo_category_protobufs():
    return [
        Category(
            hid=CATEG_ID,
            show_model_title_for_sku=True
        )
    ]


@pytest.yield_fixture(scope='module')
def workflow(
    yt_server,
    mbo_msku_protobufs,
    mbo_models_protobufs,
    mbo_category_protobufs
):
    resources = {
        "sku": MboSkuPb(mbo_msku_protobufs, CATEG_ID),
        "models": MboModelPb(mbo_models_protobufs, CATEG_ID),
        "parameters": MboCategoryPb(mbo_category_protobufs, CATEG_ID),
    }

    with MskuUploaderTestEnv(**resources) as env:
        env.execute(yt_server)
        env.verify()
        yield env


@pytest.fixture(scope='module')
def result_yt_table(workflow):
    return workflow.outputs.get('result_table')


def test_model_title(result_yt_table):
    result_record = result_yt_table.data[0]

    fact_offer = market.proto.feedparser.deprecated.OffersData_pb2.Offer()
    fact_offer.ParseFromString(result_record["offer"])

    assert fact_offer.genlog.model_title_ext == "Модельный заголовок"


def test_pmodel_title(result_yt_table):
    result_record = result_yt_table.data[1]

    fact_offer = market.proto.feedparser.deprecated.OffersData_pb2.Offer()
    fact_offer.ParseFromString(result_record["offer"])

    assert result_record["msku"] == 100000000005
    assert fact_offer.genlog.is_psku is True
    assert fact_offer.genlog.model_title == ""
    assert fact_offer.genlog.model_title_ext == "Партнёрский модельный заголовок"
