# coding=utf-8

"""
Тест используется для проверки работы вызова offers-status
"""

import pytest

from hamcrest import assert_that, equal_to, has_length
from mapreduce.yt.python.table_schema import extract_column_attributes

from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.yatf.resources.shops_dat import ShopsDat
from market.idx.generation.offer_status.yatf.test_env import YtOfferStatusTestEnv
from market.idx.generation.offer_status.yatf.resources.yt_offer_status_data import YTOfferStatusData
from market.idx.offers.yatf.utils.fixtures import default_shops_dat
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf

from market.idx.yatf.matchers.yt_rows_matchers import HasDatacampYtUnitedOffersRows

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import MARKET_IDX_GENERATION
import market.idx.datacamp.proto.offer.OfferStatus_pb2 as OfferStatus
from market.proto.common.process_log_pb2 import ERROR, WARNING

genlog_dir= "//market/genlogs"
result_table_path = "//results/offer_status"

FEED_ID = 777
SHOP_ID = 77
SUPPLIER_ID = 333
BUSINESS_ID = 7
WH_ID = 1
DROPPED_OFFER = "dropped_offer"
OFFER_WITH_ERROR = "offer_with_error"
OFFER_WITH_WARNING_SKU_SIZE = "offer_with_warning_sku_size"
OFFER_WITH_WARNING_SKU_SIZE1 = "offer_with_warning_sku_size1"
OFFER_WITH_NOT_JSON_DETAILS = "offer_with_not_json_details"
NORMAL_OFFER = "normal_offer"
NORMAL_OFFER_WITH_SUPPLIER_ID = "normal_offer_with_supplier_id"

PUBLISHED = OfferStatus.PublicationStatus.PUBLISHED
NOT_PUBLISHED = OfferStatus.PublicationStatus.NOT_PUBLISHED


@pytest.fixture(scope='module')
def process_log_tables(yt_stuff):
    process_log_table_path = "//market/process_log"
    schema = [
        {'required': False, "name": "feed_id", "type": "uint32"},
        {'required': False, "name": "offer_id", "type": "string"},
        {'required': False, "name": "code", "type": "string"},
        {'required': False, "name": "level", "type": "uint32"},
        {'required': False, "name": "details", "type": "string"},
        {'required': False, "name": "namespace", "type": "int64"},
    ]

    rows = [
        {'feed_id': FEED_ID,
         'offer_id': OFFER_WITH_ERROR,
         'code': '490',
         'level': 3,
         'details': '{\"price_limit\":\"1727.4\",\"price\":\"1892\",\"market-sku\":682008601,\"offerId\":\"not_published_offer_with_verdict\",\"code\":\"490\"}',
         'namespace': 5
         },
        {'feed_id': FEED_ID,
         'offer_id': OFFER_WITH_WARNING_SKU_SIZE1,
         'code': '25m',
         'level': 2,
         'details': '{\"market-sku\":100758729767,\"offerId\":\"172038\",\"code\":\"25m\"}',
         'namespace': 5
         },
        {'feed_id': FEED_ID,
         'offer_id': OFFER_WITH_NOT_JSON_DETAILS,
         'code': '25m',
         'level': 2,
         'details': 'bad not json-like details',
         'namespace': 5
         },
    ]

    return [YtTableResource(yt_stuff, process_log_table_path, data=rows, fail_on_exists=False, attributes={'schema': schema})]


@pytest.fixture(scope='module')
def dropped_offers_tables(yt_stuff):
    dropped_offers_table_path = "//market/dropped_offers"
    schema = [
        {'required': False, "name": "feed_id", "type": "uint32"},
        {'required': False, "name": "offer_id", "type": "string"},
    ]

    rows = [
        {'feed_id': FEED_ID,
         'offer_id': DROPPED_OFFER,
         },
    ]

    return [YtTableResource(yt_stuff, dropped_offers_table_path, data=rows, fail_on_exists=False, attributes={'schema': schema})]


@pytest.fixture(scope='module')
def genlog_tables(yt_stuff):
    genlog_table_path = genlog_dir + "/0000"
    schema = [
        {'required': False, "name": "business_id", "type": "uint32"},
        {'required': False, "name": "shop_id", "type": "uint32"},
        {'required': False, "name": "supplier_id", "type": "uint32"},
        {'required': False, "name": "offer_id", "type": "string"},
        {'required': False, "name": "warehouse_id", "type": "uint32"},
    ]

    rows = [
        {'business_id': BUSINESS_ID,
         'shop_id': SHOP_ID,
         'offer_id': NORMAL_OFFER,
         'warehouse_id': WH_ID,
         },
        {'business_id': BUSINESS_ID,
         'shop_id': SHOP_ID,
         'offer_id': OFFER_WITH_WARNING_SKU_SIZE,
         'warehouse_id': WH_ID,
         },
        {'business_id': BUSINESS_ID,
         'shop_id': SHOP_ID,
         'offer_id': NORMAL_OFFER_WITH_SUPPLIER_ID,
         'warehouse_id': WH_ID,
         'supplier_id': SUPPLIER_ID,
         },
    ]

    return [YtTableResource(yt_stuff, genlog_table_path, data=rows, fail_on_exists=False, attributes={'schema': schema})]


@pytest.fixture(scope='module')
def genlog_info():
    parts_count = 1
    return [(genlog_dir, parts_count)]


@pytest.fixture(scope='module')
def shops_dat_file():
    shop = default_shops_dat()
    shop['business_id'] = str(BUSINESS_ID)
    shop['datafeed_id'] = str(FEED_ID)
    shop['shop_id'] = str(SHOP_ID)
    shop['warehouse_id'] = str(WH_ID)

    return ShopsDat(shops=[shop])


@pytest.fixture(scope='module')
def input_data(shops_dat_file, process_log_tables, dropped_offers_tables, genlog_info, genlog_tables):
    return YTOfferStatusData(shops_dat_file, process_log_tables, dropped_offers_tables, genlog_info, genlog_tables)


@pytest.yield_fixture(scope='module')
def workflow(yt_stuff, input_data):
    out = YtTableResource(yt_stuff, result_table_path, load=False)
    with YtOfferStatusTestEnv(yt_stuff, input=input_data, output=out) as env:
        env.execute(yt_stuff)
        env.verify()
        yield env


@pytest.fixture(scope='module')
def offer_status_table(workflow):
    return workflow.result_table


def test_result_table_exists(yt_stuff, offer_status_table):
    assert_that(yt_stuff.get_yt_client().exists(offer_status_table.table_path), 'Table exist')


def test_result_table_schema(offer_status_table):
    assert_that(extract_column_attributes(list(offer_status_table.schema)), equal_to([
        {'required': False, 'sort_order': 'ascending', "name": "business_id", "type": "uint64"},
        {'required': False, 'sort_order': 'ascending', "name": "offer_id", "type": "string"},
        {'required': False, 'sort_order': 'ascending', "name": "shop_id", "type": "uint64"},
        {'required': False, 'sort_order': 'ascending', "name": "warehouse_id", "type": "uint64"},
        {'required': False, "name": "offer", "type": "string"},
    ]), "Schema is incorrect")


def test_offer_stats_data(offer_status_table):
    assert_that(offer_status_table.data, has_length(7), "Records count is incorrect")


def test_result_table_data_dropped(offer_status_table):
    assert_that(offer_status_table.data, HasDatacampYtUnitedOffersRows([
        {
            'business_id': BUSINESS_ID,
            'offer_id': DROPPED_OFFER,
            'shop_id': SHOP_ID,
            'warehouse_id': WH_ID,
            'offer': IsSerializedProtobuf(DTC.Offer, {
                'identifiers': {
                    'business_id': BUSINESS_ID,
                    'offer_id': DROPPED_OFFER,
                    'shop_id': SHOP_ID,
                    'warehouse_id': WH_ID,
                },
                'status': {
                    'publication': {
                        'meta': {
                            'source': MARKET_IDX_GENERATION,
                        },
                        'value': DTC.PublicationStatus.NOT_PUBLISHED,
                    },
                },
                'resolution': {
                    'by_source': [{
                        'meta': {
                            'source': MARKET_IDX_GENERATION,
                        },
                        'verdict': [{
                            'results': [
                                {
                                    'messages': [{
                                        'code': "492",
                                        "details": "{\"code\":\"492\"}",
                                        "level": ERROR,
                                        "namespace": "5",
                                        "params": [
                                            {
                                                "name": "code",
                                                "value": "492"
                                            }
                                        ]
                                    }],
                                }]
                        }]
                    }]
                },
            }),
        },
    ]))


def test_result_table_data_published_offers(offer_status_table):
    assert_that(offer_status_table.data, HasDatacampYtUnitedOffersRows([
        {
            'business_id': BUSINESS_ID,
            'offer_id': NORMAL_OFFER,
            'shop_id': SHOP_ID,
            'warehouse_id': WH_ID,
            'offer': IsSerializedProtobuf(DTC.Offer, {
                'identifiers': {
                    'business_id': BUSINESS_ID,
                    'offer_id': NORMAL_OFFER,
                    'shop_id': SHOP_ID,
                    'warehouse_id': WH_ID,
                },
                'status': {
                    'publication': {
                        'meta': {
                            'source': MARKET_IDX_GENERATION,
                        },
                        'value': DTC.PublicationStatus.PUBLISHED,
                    },
                },
            }),
        },
    ]))

    assert_that(offer_status_table.data, HasDatacampYtUnitedOffersRows([
        {
            'business_id': BUSINESS_ID,
            'offer_id': OFFER_WITH_WARNING_SKU_SIZE,
            'shop_id': SHOP_ID,
            'warehouse_id': WH_ID,
            'offer': IsSerializedProtobuf(DTC.Offer, {
                'identifiers': {
                    'business_id': BUSINESS_ID,
                    'offer_id': OFFER_WITH_WARNING_SKU_SIZE,
                    'shop_id': SHOP_ID,
                    'warehouse_id': WH_ID,
                },
                'status': {
                    'publication': {
                        'meta': {
                            'source': MARKET_IDX_GENERATION,
                        },
                        'value': DTC.PublicationStatus.PUBLISHED,
                    },
                },
            }),
        },
    ]))

    assert_that(offer_status_table.data, HasDatacampYtUnitedOffersRows([
        {
            'business_id': BUSINESS_ID,
            'offer_id': NORMAL_OFFER_WITH_SUPPLIER_ID,
            'shop_id': SUPPLIER_ID,
            'warehouse_id': WH_ID,
            'offer': IsSerializedProtobuf(DTC.Offer, {
                'identifiers': {
                    'business_id': BUSINESS_ID,
                    'offer_id': NORMAL_OFFER_WITH_SUPPLIER_ID,
                    'shop_id': SUPPLIER_ID,
                    'warehouse_id': WH_ID,
                },
                'status': {
                    'publication': {
                        'meta': {
                            'source': MARKET_IDX_GENERATION,
                        },
                        'value': DTC.PublicationStatus.PUBLISHED,
                    },
                },
            }),
        },
    ]))


def test_result_table_data_verdicts(offer_status_table):
    assert_that(offer_status_table.data, HasDatacampYtUnitedOffersRows([
        {
            'business_id': BUSINESS_ID,
            'offer_id': OFFER_WITH_ERROR,
            'shop_id': SHOP_ID,
            'warehouse_id': WH_ID,
            'offer': IsSerializedProtobuf(DTC.Offer, {
                'identifiers': {
                    'business_id': BUSINESS_ID,
                    'offer_id': OFFER_WITH_ERROR,
                    'shop_id': SHOP_ID,
                    'warehouse_id': WH_ID,
                },
                'status': {
                    'publication': {
                        'meta': {
                            'source': MARKET_IDX_GENERATION,
                        },
                        'value': DTC.PublicationStatus.NOT_PUBLISHED,
                    },
                },
                'resolution': {
                    'by_source': [{
                        'meta': {
                            'source': MARKET_IDX_GENERATION,
                        },
                        'verdict': [{
                            'results': [
                                {
                                    'messages': [{
                                        'code': "490",
                                        'details': '{\"price_limit\":\"1727.4\",\"price\":\"1892\",\"market-sku\":682008601,\"offerId\":\"not_published_offer_with_verdict\",\"code\":\"490\"}',
                                        "level": ERROR,
                                        "namespace": "5",
                                        "params": [
                                            {
                                                "name": "price_limit",
                                                "value": "1727.4"
                                            },
                                            {
                                                "name": "price",
                                                "value": "1892"
                                            },
                                            {
                                                "name": "offerId",
                                                "value": "not_published_offer_with_verdict"
                                            },
                                            {
                                                "name": "market-sku",
                                                "value": "682008601"
                                            },
                                            {
                                                "name": "code",
                                                "value": "490"
                                            }
                                        ]
                                    }],
                                }]
                        }]
                    }]
                },
            }),
        },
    ]))

    assert_that(offer_status_table.data, HasDatacampYtUnitedOffersRows([
        {
            'business_id': BUSINESS_ID,
            'offer_id': OFFER_WITH_WARNING_SKU_SIZE1,
            'shop_id': SHOP_ID,
            'warehouse_id': WH_ID,
            'offer': IsSerializedProtobuf(DTC.Offer, {
                'identifiers': {
                    'business_id': BUSINESS_ID,
                    'offer_id': OFFER_WITH_WARNING_SKU_SIZE1,
                    'shop_id': SHOP_ID,
                    'warehouse_id': WH_ID,
                },
                'resolution': {
                    'by_source': [{
                        'meta': {
                            'source': MARKET_IDX_GENERATION,
                        },
                        'verdict': [{
                            'results': [
                                {
                                    'messages': [{
                                        'code': "25m",
                                        'details': '{\"market-sku\":100758729767,\"offerId\":\"172038\",\"code\":\"25m\"}',
                                        "level": WARNING,
                                        "namespace": "5",
                                        "params": [
                                            {
                                                "name": "offerId",
                                                "value": "172038"
                                            },
                                            {
                                                "name": "market-sku",
                                                "value": "100758729767"
                                            },
                                            {
                                                "name": "code",
                                                "value": "25m"
                                            }
                                        ]
                                    }],
                                }]
                        }]
                    }]
                },
            }),
        },
    ]))

    assert_that(offer_status_table.data, HasDatacampYtUnitedOffersRows([
        {
            'business_id': BUSINESS_ID,
            'offer_id': OFFER_WITH_NOT_JSON_DETAILS,
            'shop_id': SHOP_ID,
            'warehouse_id': WH_ID,
            'offer': IsSerializedProtobuf(DTC.Offer, {
                'identifiers': {
                    'business_id': BUSINESS_ID,
                    'offer_id': OFFER_WITH_NOT_JSON_DETAILS,
                    'shop_id': SHOP_ID,
                    'warehouse_id': WH_ID,
                },
                'resolution': {
                    'by_source': [{
                        'meta': {
                            'source': MARKET_IDX_GENERATION,
                        },
                        'verdict': [{
                            'results': [
                                {
                                    'messages': [{
                                        'code': "25m",
                                        'details': 'bad not json-like details',
                                        "level": WARNING,
                                        "namespace": "5",
                                    }],
                                }]
                        }]
                    }]
                },
            }),
        },
    ]))
