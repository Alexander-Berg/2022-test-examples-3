# coding: utf-8

import pytest
from hamcrest import assert_that

from market.idx.datacamp.controllers.stroller.yatf.test_env import make_stroller
from market.idx.datacamp.controllers.stroller.yatf.utils import DisabledFlag, shops_request, prepare_expected_with_flags
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.datacamp.proto.api.SyncChangeOffer_pb2 import FullOfferResponse
from market.idx.datacamp.proto.offer import DataCampOffer_pb2 as DTC
from market.idx.datacamp.yatf.utils import create_meta, dict2tskv
from market.idx.pylibrary.datacamp.conversion import offer_to_basic_row, offer_to_service_row


BUSINESS_ID = 1000
SHOP_ID = 1
WAREHOUSE_ID = 6000


OFFERS = [
    ('T1001', DTC.AVAILABLE),
    ('OID-YY', None),
]


SHOPS = [
    {
        'shop_id': SHOP_ID,
        'mbi': '\n\n'.join([
            dict2tskv({
                'shop_id': SHOP_ID,
                'warehouse_id': WAREHOUSE_ID,
                'business_id': BUSINESS_ID,
            }),
        ]),
    }
]


@pytest.fixture()
def warehouse_id():
    return WAREHOUSE_ID


@pytest.fixture()
def partners():
    return SHOPS


@pytest.fixture()
def service_offers():
    return [
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id=offer_id,
                shop_id=SHOP_ID,
                warehouse_id=0,
            ),
            meta=create_meta(10, color=DTC.BLUE, scope=DTC.SERVICE),
            status=DTC.OfferStatus(
                publish_by_partner=status
            ) if status else None,
        )) for offer_id, status in OFFERS
    ]


@pytest.fixture()
def actual_service_offers(warehouse_id):
    return [
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id=offer_id,
                shop_id=SHOP_ID,
                warehouse_id=warehouse_id,
            ),
            meta=create_meta(10, color=DTC.BLUE, scope=DTC.SERVICE),
            status=DTC.OfferStatus(
                publish=status,
            ) if status else None
        )) for offer_id, status in OFFERS
    ]


@pytest.yield_fixture()
def stroller(
    config,
    yt_server,
    log_broker_stuff,
    partners_table,
    service_offers_table,
    actual_service_offers_table,
):
    with make_stroller(
        config,
        yt_server,
        log_broker_stuff,
        shopsdat_cacher=True,
        partners_table=partners_table,
        service_offers_table=service_offers_table,
        actual_service_offers_table=actual_service_offers_table,
    ) as stroller_env:
        yield stroller_env


def test_timestamps(stroller, warehouse_id):
    """
    Проверяет:
    - при вызове записи метка времени обязательна
    - изменения к офферу применяются только при наличии метки времени больше текущей
    """
    source = DTC.PUSH_PARTNER_API
    timestamp_cur = '2019-05-24T18:00:01Z'
    timestamp_new = '2019-05-24T18:00:02Z'
    shop_id = SHOP_ID
    offer_id = 'OID-YY'

    # без метки времени нельзя
    disabled_bad = DisabledFlag(flag=True, source=source, timestamp=None)
    response = shops_request(stroller, shop_id=shop_id, offer_id=offer_id, warehouse_id=warehouse_id, flag=disabled_bad)
    assert_that(response, HasStatus(403))

    disabled_good = DisabledFlag(flag=True, source=source, timestamp=timestamp_cur)
    response = shops_request(stroller, shop_id=shop_id, offer_id=offer_id, warehouse_id=warehouse_id, flag=disabled_good)
    assert_that(response, HasStatus(200))
    expected_data = prepare_expected_with_flags(shop_id, offer_id, warehouse_id, [disabled_good])
    assert_that(response.data, IsSerializedProtobuf(FullOfferResponse, expected_data))

    # а с новой - поменяется
    enabled_good = DisabledFlag(flag=False, source=source, timestamp=timestamp_new)
    response = shops_request(stroller, shop_id=shop_id, offer_id=offer_id, warehouse_id=warehouse_id, flag=enabled_good)
    assert_that(response, HasStatus(200))
    expected_data = prepare_expected_with_flags(shop_id, offer_id, warehouse_id, flags=[enabled_good])
    assert_that(response.data, IsSerializedProtobuf(FullOfferResponse, expected_data))
