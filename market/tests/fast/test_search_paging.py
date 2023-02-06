# coding: utf-8

import pytest

from hamcrest import assert_that

from market.idx.datacamp.controllers.stroller.yatf.test_env import make_stroller
from market.idx.datacamp.controllers.stroller.yatf.utils import (
    gen_basic_row,
    gen_service_row,
    gen_actual_service_row,
)
from market.idx.datacamp.proto.api.SyncSearch_pb2 import SearchRequest, SearchResponse
from market.idx.datacamp.proto.tables.Partner_pb2 import PartnerStat
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.datacamp.yatf.utils import create_meta, dict2tskv


BUSINESS_ID = 1
WAREHOUSE_ID = 100


def dc_row(shop_id, offer_id, ts):
    return {
        'shop_id': shop_id,
        'offer_id': offer_id,
        'supplemental_id': shop_id + 1000,
        'meta': create_meta(ts).SerializeToString()
    }


DATACAMP_TABLE_DATA = [
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
    (2, "5", 0),
    (2, "6", 0),
    (2, "7", 0),
    (2, "8", 0),
    (2, "9", 0),
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
        'partner_stat': PartnerStat(offers_count=9).SerializeToString()
    }
]


@pytest.fixture(scope='module')
def partners():
    return PARTNERS_TABLE_DATA


@pytest.fixture(scope='module')
def basic_offers():
    return [gen_basic_row(BUSINESS_ID, offer_id, ts)
            for shop_id, offer_id, ts in DATACAMP_TABLE_DATA]


@pytest.fixture(scope='module')
def service_offers():
    return [gen_service_row(BUSINESS_ID, shop_id, offer_id, ts)
            for shop_id, offer_id, ts in DATACAMP_TABLE_DATA]


@pytest.fixture(scope='module')
def actual_service_offers():
    return [gen_actual_service_row(BUSINESS_ID, shop_id, WAREHOUSE_ID, offer_id, [], ts)
            for shop_id, offer_id, ts in DATACAMP_TABLE_DATA]


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


def simple_search_request(client, shop_id, req):
    return client.post('/shops/{}/offers/search'.format(shop_id), data=req.SerializeToString())


def test_forward_simple(stroller):
    """
    Простой запрос на офера где то в середине списка
    """
    req = SearchRequest(position="4_4", page_size=2)
    expected_resp = {
        'meta': {
            'total_response': 2,
            'paging': {
                'start': "0_5",
                'end': "0_6",
                'is_first_page': False,
                'is_last_page': False,
            },
        },
    }
    response = simple_search_request(stroller, shop_id=1, req=req)

    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(SearchResponse, expected_resp))


def test_forward_upper_bound(stroller):
    """
    Запрос, начиная от самого нового офера
    """
    req = SearchRequest(position="", page_size=2)
    expected_resp = {
        'meta': {
            'total_response': 2,
            'paging': {
                'start': "0_1",
                'end': "0_2",
                'is_first_page': True,
                'is_last_page': False,
            },
        },
    }
    response = simple_search_request(stroller, shop_id=1, req=req)

    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(SearchResponse, expected_resp))


def test_forward_upper_bound_with_skip(stroller):
    """
    Запрос, начиная от самого нового офера, пропустив первую страницу
    """
    req = SearchRequest(position="", page_skip=1, page_size=2)
    expected_resp = {
        'meta': {
            'total_response': 2,
            'paging': {
                'start': "0_3",
                'end': "0_4",
                'is_first_page': False,
                'is_last_page': False,
            },
        },
    }
    response = simple_search_request(stroller, shop_id=1, req=req)

    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(SearchResponse, expected_resp))


def test_forward_lower_bound(stroller):
    """
    Запрос полной, последней страницы
    """
    req = SearchRequest(position="3_6", page_size=2)
    expected_resp = {
        'meta': {
            'total_response': 2,
            'paging': {
                'start': "0_7",
                'end': "0_8",
                'is_first_page': False,
                'is_last_page': True,
            },
        },
    }
    response = simple_search_request(stroller, shop_id=1, req=req)

    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(SearchResponse, expected_resp))


def test_forward_lower_bound_overflow(stroller):
    """
    Запрос последней страницы, но оферов меньше, чем на странице
    """
    req = SearchRequest(position="2_2", page_size=7)
    expected_resp = {
        'meta': {
            'total_response': 6,
            'paging': {
                'start': "0_3",
                'end': "0_8",
                'is_first_page': False,
                'is_last_page': True,
            },
        },
    }
    response = simple_search_request(stroller, shop_id=1, req=req)

    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(SearchResponse, expected_resp))


def test_forward_lower_bound_with_skip(stroller):
    """
    Запрос в котором после пропуска одной страницы должны вернуть последнюю, полную страницу
    """
    req = SearchRequest(position="5_4", page_skip=1, page_size=2)
    expected_resp = {
        'meta': {
            'total_response': 2,
            'paging': {
                'start': "0_7",
                'end': "0_8",
                'is_first_page': False,
                'is_last_page': True,
            },
        },
    }
    response = simple_search_request(stroller, shop_id=1, req=req)

    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(SearchResponse, expected_resp))


def test_forward_empty_response(stroller):
    """
    Запрос в котором пропускаем страниц больше, чем есть
    """
    req = SearchRequest(position="", page_skip=10, page_size=2)
    expected_resp = {
        'meta': {
            'total_response': 0,
            'paging': {
                'start': "",
                'end': "",
                'is_first_page': False,
                'is_last_page': True,
            },
        },
    }
    response = simple_search_request(stroller, shop_id=1, req=req)

    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(SearchResponse, expected_resp))


def test_backward_simple(stroller):
    """
    Простой запрос на офера где то в середине списка
    """
    req = SearchRequest(position="2_4", page_size=2, forward=False)
    expected_resp = {
        'meta': {
            'total_response': 2,
            'paging': {
                'start': "0_3",
                'end': "0_2",
                'is_first_page': False,
                'is_last_page': False,
            },
        },
    }
    response = simple_search_request(stroller, shop_id=1, req=req)

    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(SearchResponse, expected_resp))


def test_backward_lower_bound(stroller):
    """
    Запрос, начиная от самого старого офера
    """
    req = SearchRequest(position="", page_size=2, forward=False)
    expected_resp = {
        'meta': {
            'total_response': 2,
            'paging': {
                'start': "0_8",
                'end': "0_7",
                'is_first_page': False,
                'is_last_page': True,
            },
        },
    }
    response = simple_search_request(stroller, shop_id=1, req=req)

    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(SearchResponse, expected_resp))


def test_backward_lower_bound_with_skip(stroller):
    """
    Запрос, начиная от самого старого офера, пропустив первую страницу
    """
    req = SearchRequest(position="", page_skip=1, page_size=2, forward=False)
    expected_resp = {
        'meta': {
            'total_response': 2,
            'paging': {
                'start': "0_6",
                'end': "0_5",
                'is_first_page': False,
                'is_last_page': False,
            },
        },
    }
    response = simple_search_request(stroller, shop_id=1, req=req)

    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(SearchResponse, expected_resp))


def test_backward_upper_bound(stroller):
    """
    Запрос полной, первой страницы
    """
    req = SearchRequest(position="6_3", page_size=2, forward=False)
    expected_resp = {
        'meta': {
            'total_response': 2,
            'paging': {
                'start': "0_2",
                'end': "0_1",
                'is_first_page': True,
                'is_last_page': False,
            },
        },
    }
    response = simple_search_request(stroller, shop_id=1, req=req)

    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(SearchResponse, expected_resp))


def test_backward_upper_bound_overflow(stroller):
    """
    Запрос первой страницы, но оферов меньше, чем на странице
    """
    req = SearchRequest(position="6_2", page_size=2, forward=False)
    expected_resp = {
        'meta': {
            'total_response': 1,
            'paging': {
                'start': "0_1",
                'end': "0_1",
                'is_first_page': True,
                'is_last_page': False,
            },
        },
    }
    response = simple_search_request(stroller, shop_id=1, req=req)

    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(SearchResponse, expected_resp))


def test_backward_upper_bound_with_skip(stroller):
    """
    Запрос в котором после пропуска одной страницы должны вернуть первую, полную страницу
    """
    req = SearchRequest(position="4_5", page_skip=1, page_size=2, forward=False)
    expected_resp = {
        'meta': {
            'total_response': 2,
            'paging': {
                'start': "0_2",
                'end': "0_1",
                'is_first_page': True,
                'is_last_page': False,
            },
        },
    }
    response = simple_search_request(stroller, shop_id=1, req=req)

    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(SearchResponse, expected_resp))


def test_backward_empty_response(stroller):
    """
    Запрос в котором пропускаем страниц больше, чем есть
    """
    req = SearchRequest(position="", page_skip=10, page_size=2, forward=False)
    expected_resp = {
        'meta': {
            'total_response': 0,
            'paging': {
                'start': "",
                'end': "",
                'is_first_page': False,
                'is_last_page': True,
            },
        },
    }
    response = simple_search_request(stroller, shop_id=1, req=req)

    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(SearchResponse, expected_resp))


def test_get_last_page_forward(stroller):
    """
    Проверяем, что правильно возвращается последняя страница при forward=True
    """
    req = SearchRequest(page_size=3, last_page=True)
    expected_resp = {
        'meta': {
            'total_response': 2,
            'paging': {
                'start': "0_7",
                'end': "0_8",
                'is_first_page': False,
                'is_last_page': True,
            },
        },
    }
    response = simple_search_request(stroller, shop_id=1, req=req)

    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(SearchResponse, expected_resp))


def test_get_last_page_ignore_backward(stroller):
    """
    Проверяем, что при запросе последней страницы, игнорируется атрибут forward=False
    """
    req = SearchRequest(page_size=3, forward=False, last_page=True)
    expected_resp = {
        'meta': {
            'total_response': 2,
            'paging': {
                'start': "0_7",
                'end': "0_8",
                'is_first_page': False,
                'is_last_page': True,
            },
        },
    }
    response = simple_search_request(stroller, shop_id=1, req=req)

    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(SearchResponse, expected_resp))


def test_get_last_page_ignore_attributes(stroller):
    """
    Проверяем, что при запросе последней страницы, игнорируются атрибуты position и page_skip
    """
    req = SearchRequest(position="2_2", page_skip=1, page_size=2, forward=False, last_page=True)
    expected_resp = {
        'meta': {
            'total_response': 2,
            'paging': {
                'start': "0_7",
                'end': "0_8",
                'is_first_page': False,
                'is_last_page': True,
            },
        },
    }
    response = simple_search_request(stroller, shop_id=1, req=req)

    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(SearchResponse, expected_resp))


def test_get_last_page_when_page_size_equal_or_more_than_total(stroller):
    """
    Проверяем, что запрос последней страницы работает корректно при размере страницы >=
    количеству офферов
    """
    req = SearchRequest(page_size=9, forward=False, last_page=True)
    expected_resp = {
        'meta': {
            'total_response': 8,
            'paging': {
                'start': "0_1",
                'end': "0_8",
                'is_first_page': True,
                'is_last_page': True,
            },
        },
    }
    response = simple_search_request(stroller, shop_id=1, req=req)

    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(SearchResponse, expected_resp))


def test_not_reading_skipped_pages(stroller):
    """
    Проверяем, что если page_skip != 0, то пропускамые страницы не вычитываются из таблицы
    (17 = 1 * 8 + 8 + 1 > select_rows_limit = 15))
    """
    req = SearchRequest(page_size=8, page_skip=1, forward=True)
    expected_resp = {
        'meta': {
            'total_response': 1,
            'paging': {
                'start': "0_9",
                'end': "0_9",
                'is_first_page': False,
                'is_last_page': True,
            },
        },
    }
    response = simple_search_request(stroller, shop_id=2, req=req)

    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(SearchResponse, expected_resp))
