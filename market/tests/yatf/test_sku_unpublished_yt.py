# coding=utf-8
import pytest

from hamcrest import (
    assert_that,
    equal_to
)

from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.proto.content.mbo.MboParameters_pb2 import (
    Category,
    Parameter,
)
import market.proto.feedparser.deprecated.OffersData_pb2
import market.proto.ir.UltraController_pb2

from msku_uploader.yatf.resources.mbo_yt import (
    MboAllModelsTable,
    MboModelsTable,
    MboParamsTable,
    MboSkuTable,
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


@pytest.fixture(scope='module')
def yt_dir():
    return get_yt_prefix()


@pytest.fixture(scope='module')
def mbo_sku_table(yt_server, yt_dir, mbo_msku_protobufs):
    return MboSkuTable(
        yt_server,
        yt_dir,
        data=[
            {
                'category_id': CATEG_ID,
                'data': mbo_sku.SerializeToString(),
            }
            for mbo_sku in mbo_msku_protobufs
        ]
    )


@pytest.fixture(scope='module')
def mbo_models_table(yt_server, yt_dir, mbo_models_protobufs):
    return MboModelsTable(
        yt_server,
        yt_dir,
        data=[
            {
                'category_id': CATEG_ID,
                'data': mbo_model.SerializeToString(),
            }
            for mbo_model in mbo_models_protobufs
        ]
    )


@pytest.fixture(scope='module')
def mbo_all_models_table(yt_server, yt_dir, mbo_models_protobufs, mbo_msku_protobufs):
    return MboAllModelsTable(
        yt_server,
        yt_dir,
        data=[
            {
                'data': mbo_model.SerializeToString(),
            }
            for mbo_model in
            mbo_models_protobufs + mbo_msku_protobufs
        ]
    )


@pytest.fixture(scope='module')
def mbo_params_table(yt_server, yt_dir, mbo_category_protobufs):
    return MboParamsTable(
        yt_server,
        yt_dir,
        data=[
            {
                'hid': CATEG_ID,
                'data': mbo_category.SerializeToString()
            }
            for mbo_category in mbo_category_protobufs
        ]
    )


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
    mbo_all_models_table,
    mbo_models_table,
    mbo_params_table,
    mbo_sku_table,
    yt_dir,
):
    create_cargo_types_table(yt_server)
    resources = {
        'mbo_all_models_table': mbo_all_models_table,
        'mbo_models_table': mbo_models_table,
        'mbo_params_table': mbo_params_table,
        'mbo_sku_table': mbo_sku_table,
        'filter_out_unpublished_msku': False,
    }

    with MskuUploaderTestEnv(yt_input_dir=yt_dir, **resources) as env:
        env.execute(yt_server)
        env.verify()
        yield env


@pytest.fixture(scope='module')
def result_yt_table(workflow):
    return workflow.outputs.get('result_table')


def test_unpublished_msku_yt(result_yt_table):
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
