# coding=utf-8
import pytest

import market.proto.feedparser.deprecated.OffersData_pb2
from market.proto.content.mbo.ExportReportModel_pb2 import ParameterValue
from market.proto.content.mbo import MboParameters_pb2

from msku_uploader.yatf.test_env import MskuUploaderTestEnv

from msku_uploader.yatf.resources.mbo_pb import (
    MboCategoryPb,
    MboModelPb,
    MboSkuPb,
)
from msku_uploader.yatf.utils import (
    make_category_option_info_pb,
    make_sku_protobuf,
    make_model_protobuf,
)


VENDOR_PARAM_ID = 7893318
VENDOR_PARAM_XSL_NAME = 'vendor'
FAKE_VENDOR_ID=16644882

CATEG_ID = 989040
MODEL_ID = 1713074441


@pytest.fixture(scope='module')
def mbo_msku_protobufs():
    msku = make_sku_protobuf(
        skuid=100000000004,
        title='SKU с заданным вендором',
        model_id=MODEL_ID,
        category_id=CATEG_ID,
        parameters=[
            ParameterValue(
                param_id=VENDOR_PARAM_ID,
                xsl_name=VENDOR_PARAM_XSL_NAME,
                option_id=1,
                value_type=MboParameters_pb2.ENUM,
            )
        ]
    )

    fake_vendor_msku = make_sku_protobuf(
        skuid=100000000005,
        title='SKU с фейковым вендором (вендор не определен)',
        model_id=MODEL_ID,
        category_id=CATEG_ID,
        parameters=[
            ParameterValue(
                param_id=VENDOR_PARAM_ID,
                xsl_name=VENDOR_PARAM_XSL_NAME,
                option_id=FAKE_VENDOR_ID,
                value_type=MboParameters_pb2.ENUM,
            )
        ]
    )

    # order does matter
    return [
        msku,
        fake_vendor_msku,
    ]


@pytest.fixture(scope='module')
def mbo_models_protobufs():
    return [
        make_model_protobuf(MODEL_ID, CATEG_ID),
    ]


@pytest.fixture(scope='module')
def mbo_category_protobufs():
    return [
        MboParameters_pb2.Category(
            hid=CATEG_ID,
            parameter=[
                MboParameters_pb2.Parameter(
                    id=VENDOR_PARAM_ID,
                    xsl_name=VENDOR_PARAM_XSL_NAME,
                    published=True,
                    value_type=MboParameters_pb2.ENUM,
                    param_type=MboParameters_pb2.MODEL_LEVEL,
                    option=[
                        make_category_option_info_pb(
                            option_id=1,
                            value='VendorName1'
                        ),
                        make_category_option_info_pb(
                            option_id=FAKE_VENDOR_ID,
                            value='FakeVendorName',
                            is_fake_vendor=True,
                        ),
                    ]
                ),
            ]
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


def test_vendor_name(result_yt_table):
    result_record = result_yt_table.data[0]

    fact_offer = market.proto.feedparser.deprecated.OffersData_pb2.Offer()
    fact_offer.ParseFromString(result_record["offer"])

    assert fact_offer.genlog.vendor_name == "VendorName1"


def test_no_fake_vendor_name(result_yt_table):
    result_record = result_yt_table.data[1]

    fact_offer = market.proto.feedparser.deprecated.OffersData_pb2.Offer()
    fact_offer.ParseFromString(result_record["offer"])

    assert fact_offer.genlog.vendor_name == ""
