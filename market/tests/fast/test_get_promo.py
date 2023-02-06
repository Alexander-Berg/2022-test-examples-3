# coding: utf-8

import pytest
from datetime import datetime, timedelta
from hamcrest import assert_that, equal_to, has_length
from google.protobuf.timestamp_pb2 import Timestamp

from market.idx.datacamp.controllers.stroller.yatf.test_env import make_stroller
from market.idx.datacamp.controllers.stroller.yatf.utils import (
    gen_promo_row
)
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.datacamp.proto.api.SyncGetPromo_pb2 import GetPromoBatchRequest, GetPromoBatchResponse
from market.idx.datacamp.proto.promo.Promo_pb2 import PromoType
from market.proto.common.promo_pb2 import ESourceType


def to_timestamp(a_date):
    diff = a_date - datetime(1970, 1, 1)
    return int(diff.total_seconds())

NOW_UTC = datetime.utcnow()
FUTURE_UTC = NOW_UTC + timedelta(hours=5)
FUTURE_UTC_TS = to_timestamp(FUTURE_UTC)
PAST_UTC_TS = 1621717199

# promo_id (str), business_id (int), source (ESourceType), promo_type (PromoType), ts (int), enabled (bool), end_date (int)
DATACAMP_PROMO_DATA = [
    ('ThisIsFirstPromo',  10, ESourceType.ANAPLAN,        PromoType.BLUE_FLASH,       10,  True,  FUTURE_UTC_TS),
    ('ThisIsSecondPromo', 12, ESourceType.PARTNER_SOURCE, PromoType.BLUE_CASHBACK,    210, True,  PAST_UTC_TS),
    ('ThisIsSecondPromo', 12, ESourceType.CATEGORYIFACE,  PromoType.BLUE_FLASH,       210, True,  FUTURE_UTC_TS),
    ('ThisIsThirdPromo',  12, ESourceType.PARTNER_SOURCE, PromoType.MARKET_PROMOCODE, 210, False, FUTURE_UTC_TS),
    # Если нет business_id и source, то в таблице будут вместо них 0
    ('ThisIsPromoWithNoBusinessId', 0, 0,                 PromoType.BLUE_FLASH,       210, True,  FUTURE_UTC_TS),
]

EXPECTED_RESP = {
    'promos': {
        'promo': [
            {
                'primary_key': {
                    'promo_id': 'ThisIsFirstPromo',
                    'business_id': 10,
                    'source': ESourceType.ANAPLAN
                },
                'promo_general_info': {
                    'promo_type': PromoType.BLUE_FLASH,
                    'meta': {
                        'timestamp': Timestamp(seconds=10)
                    }
                }
            },
            {
                'primary_key': {
                    'promo_id': 'ThisIsSecondPromo',
                    'business_id': 12,
                    'source': ESourceType.PARTNER_SOURCE
                },
                'promo_general_info': {
                    'promo_type': PromoType.BLUE_CASHBACK,
                    'meta': {
                        'timestamp': Timestamp(seconds=210)
                    }
                }
            }
        ]
    }
}


EXPECTED_RESP_ENABLED = {
    'promos': {
        'promo': [
            {
                'primary_key': {
                    'promo_id': 'ThisIsFirstPromo',
                    'business_id': 10,
                    'source': ESourceType.ANAPLAN
                },
                'promo_general_info': {
                    'promo_type': PromoType.BLUE_FLASH,
                    'meta': {
                        'timestamp': Timestamp(seconds=10)
                    }
                }
            },
            {
                'primary_key': {
                    'promo_id': 'ThisIsSecondPromo',
                    'business_id': 12,
                    'source': ESourceType.PARTNER_SOURCE
                },
                'promo_general_info': {
                    'promo_type': PromoType.BLUE_CASHBACK,
                    'meta': {
                        'timestamp': Timestamp(seconds=210)
                    }
                }
            }
        ]
    }
}


EXPECTED_RESP_ONLY_UNFINISHED = {
    'promos': {
        'promo': [
            {
                'primary_key': {
                    'promo_id': 'ThisIsFirstPromo',
                    'business_id': 10,
                    'source': ESourceType.ANAPLAN
                },
                'promo_general_info': {
                    'promo_type': PromoType.BLUE_FLASH,
                    'meta': {
                        'timestamp': Timestamp(seconds=10)
                    }
                }
            }
        ]
    }
}


EXPECTED_RESP_TYPE = {
    'promos': {
        'promo': [
            {
                'primary_key': {
                    'promo_id': 'ThisIsFirstPromo',
                    'business_id': 10,
                    'source': ESourceType.ANAPLAN
                },
                'promo_general_info': {
                    'promo_type': PromoType.BLUE_FLASH,
                    'meta': {
                        'timestamp': Timestamp(seconds=10)
                    }
                }
            }
        ]
    }
}


EXPECTED_RESP_PARTIAL_KEY = {
    'promos': {
        'promo': [
            {
                'primary_key': {
                    'promo_id': 'ThisIsPromoWithNoBusinessId',
                    'business_id': 0,
                    'source': ESourceType.UNKNOWN
                },
                'promo_general_info': {
                    'promo_type': PromoType.BLUE_FLASH,
                    'meta': {
                        'timestamp': Timestamp(seconds=210)
                    }
                }
            },
        ]
    }
}


@pytest.fixture(scope='module')
def promo():
    return [gen_promo_row(promo_id=promo_id,
                          business_id=business_id,
                          source=source,
                          promo_type=promo_type,
                          ts=ts,
                          enabled=enabled,
                          end_date=end_date)
            for promo_id, business_id, source, promo_type, ts, enabled, end_date in DATACAMP_PROMO_DATA]


@pytest.yield_fixture(scope='module')
def stroller(
        config,
        yt_server,
        log_broker_stuff,
        promo_table
):
    with make_stroller(
            config,
            yt_server,
            log_broker_stuff,
            promo_table=promo_table
    ) as stroller_env:
        yield stroller_env


# Запрашиваем пару акций, имеющих полный ключ
def test_get_promo_full_key(stroller):
    request = GetPromoBatchRequest()
    entry0 = request.entries.add()
    entry0.promo_id = 'ThisIsFirstPromo'
    entry0.business_id = 10
    entry0.source = ESourceType.ANAPLAN

    entry1 = request.entries.add()
    entry1.promo_id = 'ThisIsSecondPromo'
    entry1.business_id = 12
    entry1.source = ESourceType.PARTNER_SOURCE

    response = stroller.post('/v1/promo/get', data=request.SerializeToString())

    # Немного помогает в расследованиях
    # o = GetPromoBatchResponse()
    # o.ParseFromString(response.data)
    # print('ResponseProto:')
    # print(o)

    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(GetPromoBatchResponse, EXPECTED_RESP))
    assert_that(response.headers['Content-type'], equal_to('application/x-protobuf'))


# Запрашиваем пару акций, имеющих полный ключ, с фильтром enabled
def test_get_promo_full_key_enabled(stroller):
    request = GetPromoBatchRequest()
    entry0 = request.entries.add()
    entry0.promo_id = 'ThisIsFirstPromo'
    entry0.business_id = 10
    entry0.source = ESourceType.ANAPLAN

    entry1 = request.entries.add()
    entry1.promo_id = 'ThisIsSecondPromo'
    entry1.business_id = 12
    entry1.source = ESourceType.PARTNER_SOURCE

    response = stroller.post('/v1/promo/get?enabled=1', data=request.SerializeToString())

    # Немного помогает в расследованиях
    # o = GetPromoBatchResponse()
    # o.ParseFromString(response.data)
    # print('ResponseProto:')
    # print(o)

    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(GetPromoBatchResponse, EXPECTED_RESP_ENABLED))
    assert_that(response.headers['Content-type'], equal_to('application/x-protobuf'))


# Запрашиваем пару акций, имеющих полный ключ, с фильтром only_unfinished
def test_get_promo_full_key_only_unfinished(stroller):
    request = GetPromoBatchRequest()
    entry0 = request.entries.add()
    entry0.promo_id = 'ThisIsFirstPromo'
    entry0.business_id = 10
    entry0.source = ESourceType.ANAPLAN

    entry1 = request.entries.add()
    entry1.promo_id = 'ThisIsSecondPromo'
    entry1.business_id = 12
    entry1.source = ESourceType.PARTNER_SOURCE

    response = stroller.post('/v1/promo/get?only_unfinished=1', data=request.SerializeToString())

    # Немного помогает в расследованиях
    # o = GetPromoBatchResponse()
    # o.ParseFromString(response.data)
    # print('ResponseProto:')
    # print(o)

    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(GetPromoBatchResponse, EXPECTED_RESP_ONLY_UNFINISHED))
    assert_that(response.headers['Content-type'], equal_to('application/x-protobuf'))


# Запрашиваем пару акций, имеющих полный ключ, с фильтром type
def test_get_promo_full_key_type(stroller):
    request = GetPromoBatchRequest()
    entry0 = request.entries.add()
    entry0.promo_id = 'ThisIsFirstPromo'
    entry0.business_id = 10
    entry0.source = ESourceType.ANAPLAN

    entry1 = request.entries.add()
    entry1.promo_id = 'ThisIsSecondPromo'
    entry1.business_id = 12
    entry1.source = ESourceType.PARTNER_SOURCE

    response = stroller.post('/v1/promo/get?type=5&type=8', data=request.SerializeToString())

    # Немного помогает в расследованиях
    # o = GetPromoBatchResponse()
    # o.ParseFromString(response.data)
    # print('ResponseProto:')
    # print(o)

    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(GetPromoBatchResponse, EXPECTED_RESP_TYPE))
    assert_that(response.headers['Content-type'], equal_to('application/x-protobuf'))


# Проверяем, что если ничего не найдено, то вернулся код 404
def test_get_promo_full_key_nothing_found(stroller):
    request = GetPromoBatchRequest()
    entry0 = request.entries.add()
    entry0.promo_id = 'ThisIsMissingPromoId'
    entry0.business_id = 10
    entry0.source = ESourceType.ANAPLAN

    response = stroller.post('/v1/promo/get', data=request.SerializeToString())
    assert_that(response, HasStatus(404))


# Проверяем, что если ничего не передали в запросе, то вернётся 400
def test_get_promo_empty_query(stroller):
    request = GetPromoBatchRequest()

    response = stroller.post('/v1/promo/get', data=request.SerializeToString())
    assert_that(response, HasStatus(400))


# Проверяем, что ищется акция без business_id и source.
def test_get_promo_partial_key(stroller):
    request = GetPromoBatchRequest()
    entry0 = request.entries.add()
    entry0.promo_id = 'ThisIsPromoWithNoBusinessId'

    response = stroller.post('/v1/promo/get', data=request.SerializeToString())

    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(GetPromoBatchResponse, EXPECTED_RESP_PARTIAL_KEY))
    assert_that(response.headers['Content-type'], equal_to('application/x-protobuf'))


def test_select_promos_batch(stroller):
    business_id = 12
    response = stroller.get('/v1/partners/{}/promos'.format(business_id))

    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(GetPromoBatchResponse, {
        'promos': {
            'promo': has_length(3)
        }
    }))
    assert_that(response.data, IsSerializedProtobuf(GetPromoBatchResponse, {
        'promos': {
            'promo': [{
                'primary_key': {
                    'promo_id': 'ThisIsSecondPromo',
                    'business_id': 12,
                    'source': ESourceType.PARTNER_SOURCE
                },
            }, {
                'primary_key': {
                    'promo_id': 'ThisIsThirdPromo',
                    'business_id': 12,
                    'source': ESourceType.PARTNER_SOURCE
                },
            }, {
                'primary_key': {
                    'promo_id': 'ThisIsSecondPromo',
                    'business_id': 12,
                    'source': ESourceType.CATEGORYIFACE
                },
            }]
        }
    }))
    assert_that(response.headers['Content-type'], equal_to('application/x-protobuf'))


# Проверяем фильтр enabled
def test_select_promos_batch_with_filter_enabled(stroller):
    business_id = 12
    response = stroller.get('/v1/partners/{}/promos?enabled=1'.format(business_id))

    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(GetPromoBatchResponse, {
        'promos': {
            'promo': has_length(2)
        }
    }))
    assert_that(response.data, IsSerializedProtobuf(GetPromoBatchResponse, {
        'promos': {
            'promo': [{
                'primary_key': {
                    'promo_id': 'ThisIsSecondPromo',
                    'business_id': 12,
                    'source': ESourceType.PARTNER_SOURCE
                },
            }, {
                'primary_key': {
                    'promo_id': 'ThisIsSecondPromo',
                    'business_id': 12,
                    'source': ESourceType.CATEGORYIFACE
                },
            }]
        }
    }))
    assert_that(response.headers['Content-type'], equal_to('application/x-protobuf'))


# Проверяем фильтр only_unfinished
def test_select_promos_batch_with_filter_only_unfinished(stroller):
    business_id = 12
    response = stroller.get('/v1/partners/{}/promos?only_unfinished=1'.format(business_id))

    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(GetPromoBatchResponse, {
        'promos': {
            'promo': has_length(2)
        }
    }))
    assert_that(response.data, IsSerializedProtobuf(GetPromoBatchResponse, {
        'promos': {
            'promo': [{
                'primary_key': {
                    'promo_id': 'ThisIsThirdPromo',
                    'business_id': 12,
                    'source': ESourceType.PARTNER_SOURCE
                },
            }, {
                'primary_key': {
                    'promo_id': 'ThisIsSecondPromo',
                    'business_id': 12,
                    'source': ESourceType.CATEGORYIFACE
                },
            }]
        }
    }))
    assert_that(response.headers['Content-type'], equal_to('application/x-protobuf'))


# Проверяем фильтр по типам акций
def test_select_promos_of_a_given_types(stroller):
    business_id = 12
    response = stroller.get('/v1/partners/{}/promos?type=5&type=8'.format(business_id))

    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(GetPromoBatchResponse, {
        'promos': {
            'promo': has_length(2)
        }
    }))
    assert_that(response.data, IsSerializedProtobuf(GetPromoBatchResponse, {
        'promos': {
            'promo': [{
                'primary_key': {
                    'promo_id': 'ThisIsSecondPromo',
                    'business_id': 12,
                    'source': ESourceType.PARTNER_SOURCE
                },
            }, {
                'primary_key': {
                    'promo_id': 'ThisIsSecondPromo',
                    'business_id': 12,
                    'source': ESourceType.CATEGORYIFACE
                },
            }]
        }
    }))
    assert_that(response.headers['Content-type'], equal_to('application/x-protobuf'))


# Проверяем фильтр по источникам акций
def test_get_promos_of_given_sources(stroller):
    business_id = 12
    response = stroller.get('/v1/partners/{}/promos?source=1&source=3'.format(business_id))

    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(GetPromoBatchResponse, {
        'promos': {
            'promo': has_length(1)
        }
    }))
    assert_that(response.data, IsSerializedProtobuf(GetPromoBatchResponse, {
        'promos': {
            'promo': [{
                'primary_key': {
                    'promo_id': 'ThisIsSecondPromo',
                    'business_id': 12,
                    'source': ESourceType.CATEGORYIFACE
                },
            }]
        }
    }))
    assert_that(response.headers['Content-type'], equal_to('application/x-protobuf'))
