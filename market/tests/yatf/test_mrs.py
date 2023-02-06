# coding=utf-8
import pytest

from hamcrest import assert_that, equal_to

from market.proto.indexer.StatsCalc_pb2 import ModelRegionalStats

from market.idx.tools.market_yt_data_upload.yatf.test_env import YtDataUploadTestEnv
from market.idx.yatf.resources.yt_table_resource import YtTableResource

from google.protobuf.json_format import MessageToDict
import yt.wrapper as yt


@pytest.fixture(scope='module')
def mrs_records():
    return [ModelRegionalStats.Record(model_id=1000787, noffers=2, nretailers=1, onstock=2, bid1=6,
                                      prices=[ModelRegionalStats.Price(),
                                              ModelRegionalStats.Price(median_price=10, max_price=10.1,
                                                                       min_price=9.9, min_oldprice=9.8)],
                                      regions=[10295], dc_count=0),
            ModelRegionalStats.Record(model_id=1000787, noffers=2, nretailers=1, onstock=1, bid1=50,
                                      prices=[ModelRegionalStats.Price(),
                                              ModelRegionalStats.Price(median_price=10, max_price=10.1,
                                                                       min_price=9.9, min_oldprice=9.8)],
                                      regions=[157, 159, 10253, 10262, 10281, 10324, 10335], dc_count=0)
            ]


@pytest.fixture(scope='module')
def mrs_stat_table(yt_server, mrs_records):
    schema = [
        dict(name="model_id", type_v3=dict(type_name="optional", item="uint64")),
        dict(name="noffers", type_v3=dict(type_name="optional", item="int32")),
        dict(name="nretailers", type_v3=dict(type_name="optional", item="int32")),
        dict(name="onstock", type_v3=dict(type_name="optional", item="int32")),
        dict(name="bid1", type_v3=dict(type_name="optional", item="int32")),
        dict(
            name="prices",
            type_v3=dict(
                type_name="list",
                item=dict(
                    type_name="struct",
                    members=[
                        dict(
                            name="median_price",
                            type=dict(type_name="optional", item="double"),
                        ),
                        dict(
                            name="max_price",
                            type=dict(type_name="optional", item="double"),
                        ),
                        dict(
                            name="min_price",
                            type=dict(type_name="optional", item="double"),
                        ),
                        dict(
                            name="min_oldprice",
                            type=dict(type_name="optional", item="double"),
                        ),
                        dict(
                            name="min_valid_oldprice",
                            type=dict(type_name="optional", item="double"),
                        ),
                        dict(
                            name="min_cutprice",
                            type=dict(type_name="optional", item="double"),
                        ),
                    ],
                ),
            ),
        ),
        dict(name="regions", type_v3=dict(type_name="list", item="uint32")),
        dict(
            name="currencies",
            type_v3=dict(
                type_name="list",
                item=dict(
                    type_name="struct",
                    members=[
                        dict(
                            name="name",
                            type=dict(type_name="optional", item="string"),
                        ),
                        dict(
                            name="index",
                            type=dict(type_name="optional", item="uint32"),
                        ),
                    ],
                )
            ),
        ),
        dict(name="dc_count", type_v3=dict(type_name="optional", item="int32")),
        dict(name="valid_dc_count", type_v3=dict(type_name="optional", item="int32")),
        dict(name="equal", type_v3=dict(type_name="optional", item="int32")),
        dict(name="max_discount", type_v3=dict(type_name="optional", item="double")),
        dict(name="has_price_from", type_v3=dict(type_name="optional", item="bool")),
        dict(name="promo_count", type_v3=dict(type_name="optional", item="int32")),
        dict(name="promo_types", type_v3=dict(type_name="optional", item="int64")),
        dict(name="model_type", type_v3=dict(type_name="optional", item="string")),
        dict(name="has_cpa20", type_v3=dict(type_name="optional", item="bool")),
        dict(name="has_fulfillment_light", type_v3=dict(type_name="optional", item="bool")),
        dict(name="has_cpa", type_v3=dict(type_name="optional", item="bool")),
        dict(name="max_valid_discount", type_v3=dict(type_name="optional", item="double")),
        dict(name="n_has_color_glob", type_v3=dict(type_name="optional", item="int32")),
        dict(name="n_has_color_vendor", type_v3=dict(type_name="optional", item="int32")),
        dict(name="white_promo_count", type_v3=dict(type_name="optional", item="int32")),
        dict(name="max_cashback", type_v3=dict(type_name="optional", item="double")),
        dict(name="n_cutprice_offers", type_v3=dict(type_name="optional", item="int32")),
        dict(name="n_recom_vendor_retailers", type_v3=dict(type_name="optional", item="int32")),
        dict(name="good_cpa", type_v3=dict(type_name="optional", item="bool")),
    ]
    data = [MessageToDict(r, including_default_value_fields=True, preserving_proto_field_name=True) for r in mrs_records]
    for row in data:
        for field in ("model_id", "promo_types"):
            if field in row:
                row[field] = int(row[field])
        if "regions" in row:
            row["regions"] = [yt.yson.YsonUint64(region) for region in row["regions"]]
    table = YtTableResource(
        yt_server, "//home/in/mrs",
        data=data,
        attributes={"schema": schema},
    )
    table.dump()

    return table


@pytest.yield_fixture(scope='module')
def workflow(yt_server, mrs_stat_table):
    resources = {}

    with YtDataUploadTestEnv(**resources) as env:
        env.execute(yt_server, type='mrs', output_table="//home/test/mrs", input_table=mrs_stat_table.get_path())
        env.verify()
        yield env


@pytest.fixture(scope='module')
def result_yt_table(workflow):
    return workflow.outputs.get('result_table')


def test_result_table_exist(result_yt_table, yt_server):
    assert_that(yt_server.get_yt_client().exists(result_yt_table.get_path()), 'Table exist')


def test_result_table_row_count(result_yt_table, mrs_records):
    assert_that(len(result_yt_table.data), equal_to(len(mrs_records)), "Rows count equal count of mrs records in table")
