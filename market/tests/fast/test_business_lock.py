# coding: utf-8

import pytest
import time
from hamcrest import assert_that, is_not, has_entries, all_of, greater_than

from market.idx.datacamp.controllers.stroller.yatf.test_env import make_stroller
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.datacamp.proto.api.Business_pb2 import BusinessStatusResponse
from market.mbi.proto.BusinessMigration_pb2 import (
    LockBusinessRequest,
    LockBusinessResponse,
    UnlockBusinessRequest,
    UnlockBusinessResponse,
    SUCCESS,
    FAIL,
)
from market.idx.yatf.matchers.yt_rows_matchers import HasDatacampBusinessStatusYtRows
from market.idx.datacamp.proto.business.Business_pb2 import BusinessStatus


@pytest.fixture(scope='module')
def business_status():
    return [
        {
            'business_id': 1000,
            'status': BusinessStatus(
                value=BusinessStatus.Status.LOCKED,
            ).SerializeToString(),
        },
        {
            'business_id': 2000,
            'status': BusinessStatus(
                value=BusinessStatus.Status.LOCKED,
            ).SerializeToString(),
        },
        {
            'business_id': 3000,
            'status': BusinessStatus(
                value=BusinessStatus.Status.UNLOCKED,
            ).SerializeToString(),
        },
        {
            'business_id': 4000,
            'status': BusinessStatus(
                value=BusinessStatus.Status.LOCKED,
            ).SerializeToString(),
        },
        {
            'business_id': 5000,
            'status': BusinessStatus(
                value=BusinessStatus.Status.UNLOCKED,
                shops={
                    502: 5002
                }
            ).SerializeToString(),
        },
        {
            'business_id': 5001,
            'status': BusinessStatus(
                value=BusinessStatus.Status.UNLOCKED,
                shops={
                    500: 5000,
                    503: 5003,
                }
            ).SerializeToString(),
        },
        {
            'business_id': 6000,
            'status': BusinessStatus(
                value=BusinessStatus.Status.LOCKED,
                shops={
                    600: 7000
                }
            ).SerializeToString(),
        },
        {
            'business_id': 7000,
            'status': BusinessStatus(
                value=BusinessStatus.Status.LOCKED,
            ).SerializeToString(),
        },
        {
            'business_id': 9000,
            'status': BusinessStatus(
                value=BusinessStatus.Status.LOCKED,
                shops={
                    900: 9000
                },
            ).SerializeToString(),
        },
    ]


@pytest.yield_fixture(scope='module')
def stroller(
        config,
        yt_server,
        log_broker_stuff,
        business_status_table,
):
    with make_stroller(
            config,
            yt_server,
            log_broker_stuff,
            business_status_table=business_status_table,
    ) as stroller_env:
        yield stroller_env


def test_business_status_get(stroller):
    """ Статус бизнеса, из таблицы """
    response = stroller.get('/v1/business/{}/status'.format(1000))
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(BusinessStatusResponse, {
        'status': {
            'value': BusinessStatus.Status.LOCKED,
        }
    }))


def test_empty_business_status_get(stroller):
    """ Статус бизнеса, которго нет в таблице """
    response = stroller.get('/v1/business/{}/status'.format(999))
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(BusinessStatusResponse, {
        'status': {
            'value': BusinessStatus.Status.UNDEFINED,
        }
    }))


def test_business_lock(stroller):
    """ Проверка ручки блокировки бизнеса """
    assert_that(
        stroller.business_status_table.data,
        is_not(HasDatacampBusinessStatusYtRows(
            [{
                'business_id': 3001,
            }]
        ))
    )

    response = stroller.post('/v1/business/lock', LockBusinessRequest(
        src_business_id=3000,
        dst_business_id=3001,
        shop_id=300,
    ).SerializeToString())
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(LockBusinessResponse, {
        'status': SUCCESS,
    }))

    assert_that(
        stroller.business_status_table.data,
        HasDatacampBusinessStatusYtRows([{
            'business_id': 3000,
            'status': IsSerializedProtobuf(BusinessStatus, {
                'value': BusinessStatus.Status.LOCKED,
                'shops': has_entries({
                    300: 3001
                }),
            })
        }, {
            'business_id': 3001,
            'status': IsSerializedProtobuf(BusinessStatus, {
                'value': BusinessStatus.Status.LOCKED,
            })
        }])
    )


def test_business_reverse_migrate(stroller):
    """ Проверка обратной миграции магазина """
    response = stroller.post('/v1/business/lock', LockBusinessRequest(
        src_business_id=5000,
        dst_business_id=5001,
        shop_id=500,
    ).SerializeToString())
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(LockBusinessResponse, {
        'status': SUCCESS,
    }))

    assert_that(
        stroller.business_status_table.data,
        HasDatacampBusinessStatusYtRows([{
            'business_id': 5000,
            'status': IsSerializedProtobuf(BusinessStatus, {
                'value': BusinessStatus.Status.LOCKED,
                'shops': has_entries({
                    500: 5001,
                    502: 5002
                }),
            })
        }, {
            'business_id': 5001,
            'status': IsSerializedProtobuf(BusinessStatus, {
                'value': BusinessStatus.Status.LOCKED,
                'shops': all_of(
                    has_entries({
                        503: 5003,
                    }),
                    is_not(has_entries({
                        500: 5000,
                    }))
                )
            })
        }])
    )


def test_business_lock_conflict(stroller):
    """ Миграция невозможна, если один из бизнесов залочен """
    assert_that(
        stroller.business_status_table.data,
        HasDatacampBusinessStatusYtRows(
            [{
                'business_id': 4000,
                'status': IsSerializedProtobuf(BusinessStatus, {
                    'value': BusinessStatus.Status.LOCKED,
                })
            }]
        )
    )

    response = stroller.post('/v1/business/lock', LockBusinessRequest(
        src_business_id=4000,
        dst_business_id=4001,
        shop_id=400,
    ).SerializeToString())
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(LockBusinessResponse, {
        'status': FAIL,
    }))

    assert_that(
        stroller.business_status_table.data,
        HasDatacampBusinessStatusYtRows(
            [{
                'business_id': 4000,
                'status': IsSerializedProtobuf(BusinessStatus, {
                    'value': BusinessStatus.Status.LOCKED,
                })
            }]
        )
    )

    assert_that(
        stroller.business_status_table.data,
        is_not(HasDatacampBusinessStatusYtRows(
            [{
                'business_id': 4001,
            }]
        ))
    )


def test_business_lock_blue(stroller):
    """ Проверка ручки блокировки бизнеса для миграции синих в ЕКАТ """
    assert_that(
        stroller.business_status_table.data,
        is_not(HasDatacampBusinessStatusYtRows(
            [{
                'business_id': 8000,
            }]
        ))
    )

    response = stroller.post('/v1/business/lock', LockBusinessRequest(
        src_business_id=8000,
        dst_business_id=8000,
        shop_id=800,
    ).SerializeToString())
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(LockBusinessResponse, {
        'status': SUCCESS,
    }))

    assert_that(
        stroller.business_status_table.data,
        HasDatacampBusinessStatusYtRows([{
            'business_id': 8000,
            'status': IsSerializedProtobuf(BusinessStatus, {
                'value': BusinessStatus.Status.LOCKED,
                'shops': has_entries({
                    800: 8000
                }),
            })
        }])
    )


def test_business_unlock(stroller):
    """ Проверка ручки разблокировки бизнеса """
    now_ts = int(time.time())

    response = stroller.post('/v1/business/unlock', UnlockBusinessRequest(
        src_business_id=6000,
        dst_business_id=7000,
    ).SerializeToString())
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(UnlockBusinessResponse, {
        'status': SUCCESS,
    }))

    assert_that(
        stroller.business_status_table.data,
        HasDatacampBusinessStatusYtRows([{
            'business_id': 6000,
            'status': IsSerializedProtobuf(BusinessStatus, {
                'value': BusinessStatus.Status.UNLOCKED,
                'shops': has_entries({
                    600: 7000,
                }),
                'finish_ts': {
                    'seconds': greater_than(now_ts - 1)
                }
            })
        }, {
            'business_id': 7000,
            'status': IsSerializedProtobuf(BusinessStatus, {
                'value': BusinessStatus.Status.UNLOCKED,
                'finish_ts': {
                    'seconds': greater_than(now_ts - 1)
                }
            })
        }])
    )


def test_business_unlock_blue(stroller):
    """ Проверка ручки разблокировки бизнеса после миграции синих в ЕКАТ """
    now_ts = int(time.time())

    response = stroller.post('/v1/business/unlock', UnlockBusinessRequest(
        src_business_id=9000,
        dst_business_id=9000,
    ).SerializeToString())
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(UnlockBusinessResponse, {
        'status': SUCCESS,
    }))

    assert_that(
        stroller.business_status_table.data,
        HasDatacampBusinessStatusYtRows([{
            'business_id': 9000,
            'status': IsSerializedProtobuf(BusinessStatus, {
                'value': BusinessStatus.Status.UNLOCKED,
                'shops': has_entries({
                    900: 9000,
                }),
                'finish_ts': {
                    'seconds': greater_than(now_ts - 1)
                },
            })
        }])
    )
