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


@pytest.fixture(scope='module')
def mbo_msku_protobufs():
    return [
        make_sku_protobuf(100000000004, 'SKU с модельным заголовком', MODEL_TITLE_MODEL_ID, CATEG_ID),
    ]


@pytest.fixture(scope='module')
def mbo_models_protobufs():
    return [
        make_model_protobuf(MODEL_TITLE_MODEL_ID, CATEG_ID),
    ]


@pytest.fixture(scope='module')
def mbo_category_protobufs():
    return [
        Category(
            hid=CATEG_ID,
            show_model_title_for_sku=True
        )
    ]


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
        mbo_msku_protobufs,
        mbo_models_protobufs,
        mbo_category_protobufs,
        use_pokupki_domain
):
    resources = {
        'sku': MboSkuPb(mbo_msku_protobufs, CATEG_ID),
        'models': MboModelPb(mbo_models_protobufs, CATEG_ID),
        'parameters': MboCategoryPb(mbo_category_protobufs, CATEG_ID),
    }

    with MskuUploaderTestEnv(
            use_pokupki_domain=use_pokupki_domain['value'],
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
