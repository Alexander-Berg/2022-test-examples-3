# coding=utf-8

import pytest
from hamcrest import assert_that, equal_to
from market.idx.tools.market_yt_data_upload.yatf.test_env import YtDataUploadTestEnv
from market.idx.tools.market_yt_data_upload.yatf.resources.qpipe_pb import QPipePb
from market.proto.common.common_pb2 import PriceExpression
import copy

from mapreduce.yt.python.table_schema import extract_column_attributes


# helpers ==============================================================================================
def optional_set(d, fld, value):
    if value is not None:
        d[fld] = value


def mkdata(ts=None, price=None, oldprice=None, price_deleted=None, offer_deleted=None, vat=None):
    res = {'fields': dict()}

    optional_set(res, 'timestamp', ts)

    optional_set(res['fields'], 'price_deleted', price_deleted)
    optional_set(res['fields'], 'offer_deleted', offer_deleted)
    optional_set(res['fields'], 'vat', vat)

    if price is not None:
        res['fields']['binary_price'] = {'price': price} if isinstance(price, int) else price

    if oldprice is not None:
        res['fields']['binary_oldprice'] = {'price': oldprice} if isinstance(price, int) else oldprice

    return res


def mkoffer(feed_id=None, offer_id=None, market_sku=None, data=None):
    res = dict()
    optional_set(res, 'feed_id', feed_id)
    optional_set(res, 'offer_id', offer_id)
    optional_set(res, 'market_sku', market_sku)
    optional_set(res, 'data', data)
    return res


def normalize_fact_row(row):
    res = copy.deepcopy(row)

    def convert_price_to_proto_object(fld):
        if fld not in res or res[fld] is None:
            return

        price_pb = PriceExpression()
        price_pb.ParseFromString(res[fld])
        res[fld] = price_pb

    def drop_nones(src):
        return {k: src[k] for k in src if src[k] is not None}

    convert_price_to_proto_object('price')
    convert_price_to_proto_object('oldprice')
    return drop_nones(res)


# fixtures ==============================================================================================
@pytest.fixture(scope='module')
def input_data():
    return [
        mkoffer(feed_id=1069, offer_id='offer0', data=[
            mkdata(ts=100, price=1000)
        ]),

        mkoffer(feed_id=1069, offer_id='offer1', data=[
            mkdata(ts=101, price=1001),
            mkdata(ts=102, price=1002)
        ]),

        mkoffer(feed_id=200012, market_sku=123456789123456, data=[
            mkdata(ts=103, price=1003),
        ]),

        mkoffer(feed_id=1069, offer_id='offer4', data=[
            mkdata(ts=104, price=1004, oldprice=2004),
        ]),

        mkoffer(feed_id=1069, offer_id='offer5', data=[
            mkdata(ts=105, price={'price': 1005, 'rate': '0.5', 'plus': 0.6, 'id': 'USD', 'ref_id': 'BYN'}),
        ]),

        mkoffer(feed_id=1069, offer_id='offer6', data=[
            mkdata(ts=106, price_deleted=True),
        ]),

        mkoffer(feed_id=1069, offer_id='offer7', data=[
            mkdata(ts=107, offer_deleted=True),
        ]),

        mkoffer(feed_id=1069, offer_id='offer8', data=[
            mkdata(ts=108, vat=8),
        ]),

        # invalid offers
        mkoffer(offer_id='offer9', data=[
            mkdata(ts=109, vat=9),
        ]),

        mkoffer(feed_id=1070, data=[
            mkdata(ts=110, price=1010),
        ]),

        mkoffer(data=[
            mkdata(ts=111, price=1011),
        ]),

        mkoffer(feed_id=1069, offer_id='offer12', data=[
            mkdata(price=1012),
        ]),

        # empty offer
        mkoffer(feed_id=1069, offer_id='offer13', data=[
            mkdata(ts=113),
        ]),
    ]


@pytest.yield_fixture(scope='module')
def workflow(yt_server, input_data):
    resources = {
        'prices_api_snapshot': QPipePb(input_data)
    }

    with YtDataUploadTestEnv(**resources) as env:
        env.execute(yt_server, type='prices_api_snapshot', output_table="//home/test/prices_api_snapshot")
        env.verify()
        yield env


@pytest.fixture(scope='module')
def result_yt_table(workflow):
    return workflow.outputs.get('result_table')


# tests ==============================================================================================
def test_result_table_exists(result_yt_table, yt_server):
    assert_that(yt_server.get_yt_client().exists(result_yt_table.get_path()), 'Table exists')


def test_data_count(result_yt_table):
    assert_that(len(result_yt_table.data), equal_to(10))


def test_result_schema(result_yt_table):
    assert_that(extract_column_attributes(list(result_yt_table.schema)),
                equal_to([
                    {'required': False, 'name': 'feed_id',       'type': 'uint32'},
                    {'required': False, 'name': 'offer_id',      'type': 'string'},
                    {'required': False, 'name': 'msku',          'type': 'uint64'},
                    {'required': False, 'name': 'ts',            'type': 'uint32'},
                    {'required': False, 'name': 'price',         'type': 'string'},
                    {'required': False, 'name': 'oldprice',      'type': 'string'},
                    {'required': False, 'name': 'vat',           'type': 'uint32'},
                    {'required': False, 'name': 'price_deleted', 'type': 'boolean'},
                    {'required': False, 'name': 'offer_deleted', 'type': 'boolean'},
                ]))


def test_main(result_yt_table):
    data = result_yt_table.data
    assert_that(normalize_fact_row(data[0]), equal_to({
        'feed_id': 1069,
        'offer_id': 'offer0',
        'ts': 100,
        'price': PriceExpression(price=1000)
    }))

    assert_that(normalize_fact_row(data[1]), equal_to({
        'feed_id': 1069,
        'offer_id': 'offer1',
        'ts': 101,
        'price': PriceExpression(price=1001)
    }))

    assert_that(normalize_fact_row(data[2]), equal_to({
        'feed_id': 1069,
        'offer_id': 'offer1',
        'ts': 102,
        'price': PriceExpression(price=1002)
    }))

    assert_that(normalize_fact_row(data[3]), equal_to({
        'feed_id': 200012,
        'msku': 123456789123456,
        'ts': 103,
        'price': PriceExpression(price=1003)
    }))

    assert_that(normalize_fact_row(data[4]), equal_to({
        'feed_id': 1069,
        'offer_id': 'offer4',
        'ts': 104,
        'price': PriceExpression(price=1004),
        'oldprice': PriceExpression(price=2004),
    }))

    assert_that(normalize_fact_row(data[5]), equal_to({
        'feed_id': 1069,
        'offer_id': 'offer5',
        'ts': 105,
        'price': PriceExpression(price=1005, rate='0.5', plus=0.6, id='USD', ref_id='BYN'),
    }))

    assert_that(normalize_fact_row(data[6]), equal_to({
        'feed_id': 1069,
        'offer_id': 'offer6',
        'ts': 106,
        'price_deleted': True,
    }))

    assert_that(normalize_fact_row(data[7]), equal_to({
        'feed_id': 1069,
        'offer_id': 'offer7',
        'ts': 107,
        'offer_deleted': True,
    }))

    assert_that(normalize_fact_row(data[8]), equal_to({
        'feed_id': 1069,
        'offer_id': 'offer8',
        'ts': 108,
        'vat': 8,
    }))

    assert_that(normalize_fact_row(data[9]), equal_to({
        'feed_id': 1069,
        'offer_id': 'offer13',
        'ts': 113,
    }))
