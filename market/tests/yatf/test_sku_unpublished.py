# coding=utf-8
import pytest

from hamcrest import (
    assert_that,
    equal_to
)

from market.proto.content.mbo.MboParameters_pb2 import (
    Category,
    Parameter,
)
import market.proto.feedparser.deprecated.OffersData_pb2
import market.proto.ir.UltraController_pb2

from msku_uploader.yatf.resources.mbo_pb import (
    MboCategoryPb,
    MboModelPb,
    MboSkuPb,
)
from msku_uploader.yatf.test_env import MskuUploaderTestEnv
from msku_uploader.yatf.utils import (
    make_sku_protobuf,
    make_model_protobuf
)
from market.idx.pylibrary.offer_flags.flags import OfferFlags


CATEG_ID = 989040
MODEL_TITLE_CATEG_ID = 989041
MODEL_ID = 1713074440


@pytest.fixture(scope='module')
def mbo_msku_protobufs():
    data = {
        'good_sku': make_sku_protobuf(
            skuid=100000000001,
            title='SKU с минимально необходимыми данными',
            model_id=MODEL_ID,
            category_id=CATEG_ID
        ),
        'unpublished_sku': make_sku_protobuf(
            skuid=100000000002,
            title='Неопубликованный SKU',
            model_id=MODEL_ID,
            category_id=CATEG_ID
        ),
    }
    data['unpublished_sku'].published_on_blue_market = False

    # order does matter
    return [
        data['good_sku'],
        data['unpublished_sku'],
    ]


@pytest.fixture(scope='module')
def mbo_models_protobufs():
    return [
        make_model_protobuf(
            model_id=MODEL_ID,
            category_id=CATEG_ID
        ),
    ]


def create_params_descr():
    return [
        Parameter(
            id=1000001,
            xsl_name='Length',
            published=True,
            common_filter_index=1,
            value_type='NUMERIC'
        ),
    ]


@pytest.fixture(scope='module')
def mbo_category_protobufs():
    return [
        Category(
            hid=CATEG_ID,
            parameter=create_params_descr()
        ),
    ]


def create_cargo_types_table(yt_server):
    yt_client = yt_server.get_yt_client()

    schema = [
        dict(name='id', type='int64'),  # cargo_type_id
        dict(name='mbo_parameter_id', type='int64'),  # mbo_param_id
    ]

    attributes = {'schema': schema}

    table_name = '//home/test/mbo_id_to_cargo_type'
    yt_client.create(
        'table',
        table_name,
        ignore_existing=True,
        recursive=True,
        attributes=attributes
    )


@pytest.yield_fixture(scope='module')
def workflow(
    yt_server,
    mbo_msku_protobufs,
    mbo_models_protobufs,
    mbo_category_protobufs
):
    create_cargo_types_table(yt_server)
    resources = {
        "sku": MboSkuPb(mbo_msku_protobufs, CATEG_ID),
        "models": MboModelPb(mbo_models_protobufs, CATEG_ID),
        "parameters": MboCategoryPb(mbo_category_protobufs, CATEG_ID),
        "filter_out_unpublished_msku": False,
    }

    with MskuUploaderTestEnv(**resources) as env:
        env.execute(yt_server)
        env.verify()
        yield env


@pytest.fixture(scope='module')
def result_yt_table(workflow):
    return workflow.outputs.get('result_table')


def test_unpublished_msku(result_yt_table):
    result_record = result_yt_table.data[0]
    fact_offer = market.proto.feedparser.deprecated.OffersData_pb2.Offer()
    fact_offer.ParseFromString(result_record["offer"])
    assert_that(
        fact_offer.genlog.market_sku,
        equal_to(100000000001),
        "offer protobuf of published sku"
    )

    assert fact_offer.genlog.flags & OfferFlags.IS_MSKU_PUBLISHED.value != 0,  "offer flags of published sku"

    result_record_unpublished = result_yt_table.data[1]
    fact_offer_unpublished = market.proto.feedparser.deprecated.OffersData_pb2.Offer()
    fact_offer_unpublished.ParseFromString(result_record_unpublished["offer"])
    assert_that(
        fact_offer_unpublished.genlog.market_sku,
        equal_to(100000000002),
        "offer protobuf of unpublished sku"
    )
    assert fact_offer_unpublished.genlog.flags & OfferFlags.IS_MSKU_PUBLISHED.value == 0,  "offer flags of unpublished sku"
