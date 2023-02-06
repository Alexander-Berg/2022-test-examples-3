# coding: utf-8

import pytest
from hamcrest import assert_that

from market.idx.datacamp.controllers.stroller.yatf.test_env import make_stroller
from market.idx.datacamp.proto.api.ShopStats_pb2 import ShopStatsResponse
from market.idx.datacamp.proto.tables.Partner_pb2 import PartnerStat

from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.datacamp.yatf.matchers.matchers import HasStatus

BUSINESS_ID = 100
SHOP_ID_NOT_IN_PARTNERS = 100
SHOP_ID_1 = 200
SHOP_ID_2 = 201


@pytest.fixture(scope='module')
def partners():
    return [
        {
            'shop_id': SHOP_ID_1,
            'partner_stat': PartnerStat().SerializeToString(),
        },
        {
            'shop_id': SHOP_ID_2,
            'partner_stat': PartnerStat(
                offers_count=42
            ).SerializeToString(),
        }
    ]


@pytest.yield_fixture(scope='module')
def stroller(
        config,
        yt_server,
        log_broker_stuff,
        partners_table
):
    with make_stroller(
            config,
            yt_server,
            log_broker_stuff,
            partners_table=partners_table
    ) as stroller_env:
        yield stroller_env


def test_get_404_if_shop_is_not_found(stroller):
    response = stroller.get(
        '/v1/partners/{}/shops/{}/stats'.format(
            BUSINESS_ID, SHOP_ID_NOT_IN_PARTNERS
        )
    )
    assert_that(response, HasStatus(404))


def test_get_shop_stats_offers_count(stroller):
    response = stroller.get(
        '/v1/partners/{}/shops/{}/stats'.format(
            BUSINESS_ID, SHOP_ID_2
        )
    )
    assert_that(response.data, IsSerializedProtobuf(ShopStatsResponse, {
        'OffersCount': 42
    }))


def test_get_offers_count_if_no_stats(stroller):
    response = stroller.get(
        '/v1/partners/{}/shops/{}/stats'.format(
            BUSINESS_ID, SHOP_ID_1
        )
    )
    assert_that(response.data, IsSerializedProtobuf(ShopStatsResponse, {
        'OffersCount': 0
    }))
