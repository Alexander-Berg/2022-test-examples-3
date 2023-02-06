# coding: utf-8

import pytest
import time
from hamcrest import assert_that, greater_than

from market.idx.datacamp.controllers.stroller.yatf.test_env import make_stroller
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.datacamp.yatf.utils import create_ts
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
import market.idx.datacamp.proto.tables.Partner_pb2 as Partner
import market.idx.datacamp.proto.api.CopyOffers_pb2 as CopyOffers
from market.idx.yatf.matchers.yt_rows_matchers import HasDatacampPartersYtRows
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC


@pytest.fixture(scope='module')
def partners():
    return [
        {
            'shop_id': 1,
            'partner_additional_info': Partner.PartnerAdditionalInfo(
                offers_copying_tasks=[
                    CopyOffers.OffersCopyTask(
                        business_id=4,
                        dst_shop_id=1,
                        src_shop_ids=[2],
                        copy_content_from_shop=2,
                        init_ts=init_ts,
                        start_ts=start_ts,
                        finish_ts=finish_ts
                    ) for (init_ts, start_ts, finish_ts) in ((create_ts(10), None, None), (create_ts(10), create_ts(11), None), (create_ts(10), create_ts(11), create_ts(12)))
                ]
            ).SerializeToString(),
        },
        {
            'shop_id': 2,
            'partner_additional_info': Partner.PartnerAdditionalInfo().SerializeToString(),
        },
    ]


@pytest.yield_fixture(scope='module')
def stroller(
        config,
        yt_server,
        log_broker_stuff,
        partners_table,
):
    with make_stroller(
            config,
            yt_server,
            log_broker_stuff,
            partners_table=partners_table,
    ) as stroller_env:
        yield stroller_env


@pytest.mark.parametrize('shop_id', [
    2,
    3,
], ids=['existing_shop', 'new_shop'])
def test_start(stroller, partners_table, shop_id):
    init_ts = int(time.time()) - 100
    expected_resp = {
        'shop_id': shop_id,
        'id': 0,
        'status': CopyOffers.OffersCopyTaskStatus.SCHEDULED,
    }

    req_data = CopyOffers.OffersCopyTask(
        business_id=4,
        dst_shop_id=shop_id,
        src_shop_ids=[1],
        copy_content_from_shop=1
    )

    response = stroller.post('/v1/partners/{}/offers/services/{}/copy'.format(4, shop_id), req_data.SerializeToString())
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(CopyOffers.OffersCopyTaskStatus, expected_resp))

    partners_table.load()
    assert_that(
        partners_table.data,
        HasDatacampPartersYtRows(
            [{
                'shop_id': shop_id,
                'partner_additional_info': IsSerializedProtobuf(Partner.PartnerAdditionalInfo, {
                    'offers_copying_tasks': [
                        {
                            'business_id': 4,
                            'dst_shop_id': shop_id,
                            'dst_warehouse_id': None,
                            'id': 0,
                            'src_shop_ids': [1],
                            'copy_content_from_shop': 1,
                            'init_ts': {
                                'seconds': greater_than(init_ts)
                            },
                        }
                    ],
                })
            }]
        )
    )


@pytest.mark.parametrize('task_id, status', [
    (0, CopyOffers.OffersCopyTaskStatus.SCHEDULED),
    (1, CopyOffers.OffersCopyTaskStatus.STARTED),
    (2, CopyOffers.OffersCopyTaskStatus.FINISHED),
], ids=['scheduled', 'started', 'finished'])
def test_status(stroller, task_id, status):
    expected_resp = {
        'shop_id': 1,
        'id': task_id,
        'status': status,
    }

    response = stroller.get('/v1/partners/{}/offers/services/{}/copy/{}'.format(4, 1, task_id))
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(CopyOffers.OffersCopyTaskStatus, expected_resp))


@pytest.mark.parametrize('warehouse_id', [
    None,
    145,
], ids=['empty_warehouse', 'not_empty_warehouse'])
def test_set_warehouse_id(stroller, partners_table, warehouse_id):
    shop_id = 5
    expected_resp = {
        'shop_id': shop_id,
        'status': CopyOffers.OffersCopyTaskStatus.SCHEDULED,
    }

    req_data = CopyOffers.OffersCopyTask(
        business_id=4,
        dst_shop_id=shop_id,
        src_shop_ids=[1],
        copy_content_from_shop=1
    )

    warehouse_request = '?warehouse_id={}'.format(warehouse_id) if warehouse_id else ''
    response = stroller.post(
        '/v1/partners/{}/offers/services/{}/copy{}'.format(4, shop_id, warehouse_request),
        req_data.SerializeToString()
    )
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(CopyOffers.OffersCopyTaskStatus, expected_resp))

    partners_table.load()
    assert_that(
        partners_table.data,
        HasDatacampPartersYtRows(
            [{
                'shop_id': shop_id,
                'partner_additional_info': IsSerializedProtobuf(Partner.PartnerAdditionalInfo, {
                    'offers_copying_tasks': [
                        {
                            'business_id': 4,
                            'dst_shop_id': shop_id,
                            'dst_warehouse_id': warehouse_id,
                        }
                    ],
                })
            }]
        )
    )


def test_set_rgb(stroller, partners_table):
    shop_id = 5
    expected_resp = {
        'shop_id': shop_id,
        'status': CopyOffers.OffersCopyTaskStatus.SCHEDULED,
    }

    req_data = CopyOffers.OffersCopyTask(
        business_id=4,
        dst_shop_id=shop_id,
        src_shop_ids=[1],
        copy_content_from_shop=1,
        rgb=DTC.BLUE
    )

    response = stroller.post(
        '/v1/partners/{}/offers/services/{}/copy'.format(4, shop_id),
        req_data.SerializeToString()
    )
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(CopyOffers.OffersCopyTaskStatus, expected_resp))

    partners_table.load()
    assert_that(
        partners_table.data,
        HasDatacampPartersYtRows(
            [{
                'shop_id': shop_id,
                'partner_additional_info': IsSerializedProtobuf(Partner.PartnerAdditionalInfo, {
                    'offers_copying_tasks': [
                        {
                            'business_id': 4,
                            'dst_shop_id': shop_id,
                            'rgb': DTC.BLUE
                        }
                    ],
                })
            }]
        )
    )
