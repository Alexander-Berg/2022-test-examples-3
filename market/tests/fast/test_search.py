# coding: utf-8

import pytest

from hamcrest import assert_that, equal_to

from market.idx.datacamp.controllers.stroller.yatf.test_env import make_stroller
from market.idx.datacamp.controllers.stroller.yatf.utils import (
    gen_basic_row,
    gen_service_row,
    gen_actual_service_row,
)
from market.idx.datacamp.proto.api.SyncSearch_pb2 import SearchRequest, SearchResponse
from market.idx.datacamp.proto.tables.Partner_pb2 import PartnerStat
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.datacamp.yatf.utils import dict2tskv
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, OrderedList


# TODO(qman) remaining tests to write:
# * Selecting non-existent shop
# * Selecting out-of range page
# * Handling general error cases
# * Get parameters and json format (not important because it's for manual testing only)


BUSINESS_ID = 1
WAREHOUSE_ID = 100


@pytest.fixture(scope='module', params=[0, 1])
def original_mode(request):
    return request.param


_DATACAMP_TABLE_DATA = [
    (1, "1", 0),
    (1, "2", 0),
    (1, "3", 0),
    (1, "4", 0),
    (1, "5", 0),
    (1, "6", 0),
    (1, "7", 0),
    (1, "8", 0),
    (2, "1", 0),
    (2, "2", 0),
    (2, "3", 0),
    (2, "4", 0),
    (3, "1", 0),
    (3, "2", 0),
    (3, "3", 0),
    (3, "4", 0),
    (3, "5", 0),
    (3, "6", 0),
    (3, "7", 0),
    (3, "8", 0),
]


PARTNERS_TABLE_DATA = [
    {
        'shop_id': 1,
        'mbi': '\n\n'.join([
            dict2tskv({'shop_id': 1, 'business_id': BUSINESS_ID}),
        ]),
        'partner_stat': PartnerStat(offers_count=8).SerializeToString()
    },
    {
        'shop_id': 2,
        'mbi': '\n\n'.join([
            dict2tskv({'shop_id': 2, 'business_id': BUSINESS_ID}),
        ]),
        'partner_stat': PartnerStat(offers_count=4).SerializeToString()
    },
    {
        'shop_id': 3,
        'mbi': '\n\n'.join([
            dict2tskv({'shop_id': 3, 'business_id': BUSINESS_ID}),
        ]),
        'partner_stat': PartnerStat(offers_count=8).SerializeToString()
    }
]


@pytest.fixture(scope='module')
def partners():
    return PARTNERS_TABLE_DATA


@pytest.fixture(scope='module')
def basic_offers():
    return [gen_basic_row(BUSINESS_ID, offer_id, ts)
            for shop_id, offer_id, ts in _DATACAMP_TABLE_DATA]


@pytest.fixture(scope='module')
def service_offers():
    return [gen_service_row(BUSINESS_ID, shop_id, offer_id, ts)
            for shop_id, offer_id, ts in _DATACAMP_TABLE_DATA]


@pytest.fixture(scope='module')
def actual_service_offers():
    return [gen_actual_service_row(BUSINESS_ID, shop_id, WAREHOUSE_ID, offer_id, [], ts)
            for shop_id, offer_id, ts in _DATACAMP_TABLE_DATA if shop_id != 3]


@pytest.yield_fixture(scope='module')
def stroller(
    config,
    yt_server,
    log_broker_stuff,
    basic_offers_table,
    service_offers_table,
    actual_service_offers_table,
    partners_table,
):
    with make_stroller(
        config,
        yt_server,
        log_broker_stuff,
        shopsdat_cacher=True,
        partners_table=partners_table,
        basic_offers_table=basic_offers_table,
        service_offers_table=service_offers_table,
        actual_service_offers_table=actual_service_offers_table,
    ) as stroller_env:
        yield stroller_env


def simple_search_request(client, shop_id, req, original_mode=0):
    return client.post('/shops/{}/offers/search?original={}'.format(shop_id, original_mode), data=req.SerializeToString())


def _gen_offer(shop_id, offer_id, ts):
    return {
        'identifiers': {
            'shop_id': shop_id,
            'offer_id': offer_id,
        },
        'meta': {
            'ts_created': {
                "seconds": ts,
            },
        },
    }


def test_search_single(stroller, original_mode):
    # Empty position should mean selecting from start
    req = SearchRequest(position="", page_size=1)
    shop_id = 3 if original_mode else 1
    expected_resp = {
        'meta': {
            'total_response': 1,
            'total_available': 8,
            'paging': {'start': "0_1", 'end': "0_1", 'is_first_page': True, 'is_last_page': False},
        },
        'offer': OrderedList([
            _gen_offer(shop_id, "1", 0)
        ]),
    }
    response = simple_search_request(stroller, shop_id=shop_id, req=req, original_mode=original_mode)

    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(SearchResponse, expected_resp))
    assert_that(response.headers['Content-type'], equal_to('application/x-protobuf'))


def test_paging(stroller, original_mode):
    # Default sort order is by creation date descending
    # We are selecting elements which have position **strictly** greater/less
    req = SearchRequest(position="30_1", page_size=3, page_skip=1)
    shop_id = 3 if original_mode else 1
    expected_resp = {
        'meta': {
            'total_response': 3,
            'total_available': 8,
            'paging': {'start': "0_5", 'end': "0_7", 'is_first_page': False, 'is_last_page': False}
        },
        'offer': OrderedList([
            _gen_offer(shop_id, "5", 0),
            _gen_offer(shop_id, "6", 0),
            _gen_offer(shop_id, "7", 0),
        ]),
    }
    response = simple_search_request(stroller, shop_id=shop_id, req=req, original_mode=original_mode)

    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(SearchResponse, expected_resp))


def test_direction(stroller, original_mode):
    # "Backward" here means by creation date **descending** since default order
    # is asc
    req = SearchRequest(position="5_7", page_size=2, page_skip=1, forward=False)
    shop_id = 3 if original_mode else 1
    expected_resp = {
        'meta': {
            'total_response': 2,
            'total_available': 8,
            'paging': {'start': "0_4", 'end': "0_3", 'is_first_page': False, 'is_last_page': False}
        },
        'offer': OrderedList([
            _gen_offer(shop_id, "4", 0),
            _gen_offer(shop_id, "3", 0),
        ]),
    }

    response = simple_search_request(stroller, shop_id=shop_id, req=req, original_mode=original_mode)

    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(SearchResponse, expected_resp))


# Selecting last page where number of offers equal to page size
# first/last page should be set accordingly
def test_edge(stroller, original_mode):
    shop_id = 3 if original_mode else 1
    for req, expected_resp in [(
        SearchRequest(position="8_5", page_size=2, page_skip=1),
        {
            'meta': {
                'total_response': 1,
                'total_available': 8,
                'paging': {'start': "0_8", 'end': "0_8", 'is_first_page': False, 'is_last_page': True}
            },
            'offer': OrderedList([
                _gen_offer(shop_id, "8", 0),
            ]),
        }
    ), (
        SearchRequest(position="7_5", page_size=2, page_skip=1, forward=False),
        {
            'meta': {
                'total_response': 2,
                'total_available': 8,
                'paging': {'start': "0_2", 'end': "0_1", 'is_first_page': True, 'is_last_page': False}
            },
            'offer': OrderedList([
                _gen_offer(shop_id, "2", 0),
                _gen_offer(shop_id, "1", 0),
            ]),
        }
    )]:
        response = simple_search_request(stroller, shop_id=shop_id, req=req, original_mode=original_mode)

        assert_that(response, HasStatus(200))
        assert_that(response.data, IsSerializedProtobuf(SearchResponse, expected_resp))


# Selecting more elements than available
# first/last page should be set accordingly
def test_overflow(stroller, original_mode):
    shop_id = 3 if original_mode else 1
    for req, expected_resp in [(
        SearchRequest(position="", page_size=3, page_skip=2),
        {
            'meta': {
                'total_response': 2,
                'total_available': 8,
                'paging': {'start': "0_7", 'end': "0_8", 'is_first_page': False, 'is_last_page': True}
            },
            'offer': OrderedList([
                _gen_offer(shop_id, "7", 0),
                _gen_offer(shop_id, "8", 0),
            ]),
        }
    ), (
        SearchRequest(position="5_6", page_size=2, page_skip=2, forward=False),
        {
            'meta': {
                'total_response': 1,
                'total_available': 8,
                'paging': {'start': "0_1", 'end': "0_1", 'is_first_page': True, 'is_last_page': False}
            },
            'offer': OrderedList([
                _gen_offer(shop_id, "1", 0),
            ]),
        }
    )]:
        response = simple_search_request(stroller, shop_id=shop_id, req=req, original_mode=original_mode)
        assert_that(response, HasStatus(200))
        assert_that(response.data, IsSerializedProtobuf(SearchResponse, expected_resp))


# Selecting all elements
# Both first and last should be set as True
def test_all_offers(stroller, original_mode):
    req = SearchRequest(position="", page_size=8)
    shop_id = 3 if original_mode else 1
    expected_resp = {
        'meta': {
            'total_response': 8,
            'total_available': 8,
            'paging': {'start': "0_1", 'end': "0_8", 'is_first_page': True, 'is_last_page': True}
        },
        'offer': OrderedList([
            _gen_offer(shop_id, "1", 0),
            _gen_offer(shop_id, "2", 0),
            _gen_offer(shop_id, "3", 0),
            _gen_offer(shop_id, "4", 0),
            _gen_offer(shop_id, "5", 0),
            _gen_offer(shop_id, "6", 0),
            _gen_offer(shop_id, "7", 0),
            _gen_offer(shop_id, "8", 0),
        ]),
    }

    response = simple_search_request(stroller, shop_id=shop_id, req=req, original_mode=original_mode)

    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(SearchResponse, expected_resp))


# Just to be sure we can handle more than one shop
def test_different_shop(stroller):
    req = SearchRequest(position="", page_size=2, page_skip=1)
    expected_resp = {
        'meta': {
            'total_response': 2,
            'total_available': 4,
            'paging': {'start': "0_3", 'end': "0_4", 'is_first_page': False, 'is_last_page': True}
        },
        'offer': OrderedList([
            _gen_offer(2, "3", 0),
            _gen_offer(2, "4", 0),
        ]),
    }

    response = simple_search_request(stroller, shop_id=2, req=req)

    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(SearchResponse, expected_resp))
