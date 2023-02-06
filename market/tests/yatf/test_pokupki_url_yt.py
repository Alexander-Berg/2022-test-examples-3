# coding=utf-8
import pytest

import market.proto.feedparser.deprecated.OffersData_pb2

from market.proto.content.mbo.MboParameters_pb2 import Category
from msku_uploader.yatf.test_env import MskuUploaderTestEnv

from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from msku_uploader.yatf.resources.mbo_yt import (
    MboAllModelsTable,
    MboModelsTable,
    MboParamsTable,
    MboSkuTable,
)
from msku_uploader.yatf.utils import (
    make_sku_protobuf,
    make_model_protobuf
)


CATEG_ID = 989040
MODEL_TITLE_MODEL_ID = 1713074441


@pytest.fixture(scope='module')
def mbo_msku_protobufs():
    return [
        make_sku_protobuf(
            100000000004,
            'SKU с модельным заголовком',
            MODEL_TITLE_MODEL_ID,
            CATEG_ID
        ),
    ]


@pytest.fixture(scope='module')
def mbo_models_protobufs():
    return [
        make_model_protobuf(
            MODEL_TITLE_MODEL_ID,
            CATEG_ID
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


@pytest.fixture(params=[{'value': True,
                         'expected_url': 'https://pokupki.market.yandex.ru/product--sku-s-modelnym-zagolovkom/100000000004'},
                        {'value': False,
                         'expected_url': 'https://market.yandex.ru/product--sku-s-modelnym-zagolovkom/1713074441?sku=100000000004'},
                        ],
                ids=['True',
                     'False'
                     ],
                scope='module'
                )
def use_pokupki_domain(request):
    return request.param


@pytest.yield_fixture(scope='module')
def workflow(
        yt_server,
        yt_dir,
        mbo_all_models_table,
        mbo_models_table,
        mbo_params_table,
        mbo_sku_table,
        use_pokupki_domain
):
    resources = {
        'mbo_all_models_table': mbo_all_models_table,
        'mbo_models_table': mbo_models_table,
        'mbo_params_table': mbo_params_table,
        'mbo_sku_table': mbo_sku_table,
    }

    with MskuUploaderTestEnv(
            use_pokupki_domain=use_pokupki_domain['value'],
            yt_input_dir=yt_dir,
            **resources
    ) as env:
        env.execute(yt_server)
        env.verify()
        yield env


@pytest.fixture(scope='module')
def result_yt_table(workflow):
    return workflow.outputs.get('result_table')


def test_model_title(result_yt_table, use_pokupki_domain):
    result_record = result_yt_table.data[0]

    fact_offer = market.proto.feedparser.deprecated.OffersData_pb2.Offer()
    fact_offer.ParseFromString(result_record['offer'])

    assert fact_offer.genlog.url == use_pokupki_domain['expected_url']
