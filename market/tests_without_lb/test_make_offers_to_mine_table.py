# coding: utf-8

import pytest

from hamcrest import assert_that, has_length, has_entries, contains

from google.protobuf.timestamp_pb2 import Timestamp

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.routines.yatf.test_env import DatacampTaskRunner

from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock


@pytest.fixture(scope='module')
def output_table():
    return '//tmp/output'


@pytest.fixture(scope='module')
def service_offers_table_data():
    return [
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=business_id, shop_id=shop_id, offer_id=offer_id),
            tech_info=DTC.OfferTechInfo(
                last_mining=DTC.MiningTrace(
                    revision=revision, meta=DTC.UpdateMeta(timestamp=Timestamp(seconds=ts, nanos=0))
                )
            ),
            status=DTC.OfferStatus(disabled=[DTC.Flag(flag=disable_flag)]),
            meta=DTC.OfferMeta(rgb=color),
            partner_info=DTC.PartnerInfo(is_disabled=is_disabled),
        ) for business_id, shop_id, offer_id, revision, ts, disable_flag, color, is_disabled in [
            # business_id, shop_id, offer_id, revision, ts, disable_flag, color, is_disabled
            (1, 1, '8', 20, 20, False, DTC.LAVKA, False),
            (1, 1, '1', 1, 1, False, DTC.BLUE, False),
            (1, 1, '7', 10, 10, False, DTC.LAVKA, False),
            (1, 1, '2', 10, 1, False, DTC.BLUE, False),
            (1, 1, '6', 10, 10, True, DTC.LAVKA, True),
            (1, 1, '3', 10, 10, False, DTC.BLUE, False),
            (1, 1, '5', 10, 10, True, DTC.BLUE, True),
            (1, 1, '4', 10, 10, True, DTC.BLUE, False),
        ]
    ]


@pytest.fixture(scope='module')
def config(yt_server):
    config = RoutinesConfigMock(
        yt_server,
        config={
            'general': {
                'yt_home': '//home/datacamp/united',
            },
        })
    return config


@pytest.fixture(scope='module')
def make_offers_to_mine_table(yt_server, config, service_offers_table, output_table):
    resources = {
        'config': config,
        'service_offers_table': service_offers_table
    }

    cmd_args = [
        '--proxy', yt_server.get_server(),
        '--input', service_offers_table.table_path,
        '--output', output_table,
        '--from-ts', 2,
        '--to-ts', 20,
        '--from-version', 2,
        '--to-version', 20,
        '--copy-dc', False
    ]

    with DatacampTaskRunner('make-offers-to-mine-table', cmd_args, **resources) as env:
        env.verify()

        yield env


def test_offers_ids_table(make_offers_to_mine_table, yt_server, output_table):
    output = list(yt_server.get_yt_client().read_table(output_table))

    assert_that(output, has_length(5))
    assert_that(
        output,
        contains(*[
            has_entries({
                'business_id': business_id,
                'shop_sku': shop_sku,
            }) for business_id, shop_sku in [
                (1, '3'),
                (1, '4'),
                (1, '5'),
                (1, '6'),
                (1, '7'),
            ]
        ])
    )
