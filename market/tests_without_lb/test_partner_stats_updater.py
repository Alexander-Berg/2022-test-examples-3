# coding: utf-8

from hamcrest import assert_that
import pytest

from market.idx.datacamp.proto.tables.Partner_pb2 import PartnerStat
from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock
from market.idx.datacamp.routines.yatf.test_env import PartnerStatsUpdaterTestEnv
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.yatf.matchers.yt_rows_matchers import HasDatacampPartersYtRows
from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampPartnersTable,
    DataCampServiceOffersTable,
)


PARTNERS = [
    {
        'shop_id': 1,
        'mbi': 'blue_status\tREAL\ndatafeed_id\t1\nshop_id\t1\nbusiness_id\t1',
        'status': 'publish'
    },
    {
        'shop_id': 2,
        'partner_stat': PartnerStat(offers_count=0).SerializeToString(),
        'mbi': 'blue_status\tNO\ndatafeed_id\t2\nshop_id\t2\nbusiness_id\t2',
        'status': 'publish'
    },
    {
        'shop_id': 3,
        'partner_stat': PartnerStat(offers_count=1).SerializeToString(),
        'mbi': 'blue_status\tREAL\ndatafeed_id\t3\nshop_id\t3\nbusiness_id\t3',
        'status': 'publish'
    },
]

OFFERS = [
    {'shop_id': 1, 'offer_id': "1"},
    {'shop_id': 1, 'offer_id': "2"},
    {'shop_id': 1, 'offer_id': "3"},
    {'shop_id': 1, 'offer_id': "4"},

    {'shop_id': 2, 'offer_id': "1"},

    {'shop_id': 3, 'offer_id': "1"},
    {'shop_id': 3, 'offer_id': "2"},
]

ACTUAL_SERVICE_OFFERS = [
    {'business_id': 1, 'shop_id': 1, 'shop_sku': "1"},
    {'business_id': 1, 'shop_id': 1, 'shop_sku': "2"},
    {'business_id': 1, 'shop_id': 1, 'shop_sku': "3"},
    {'business_id': 1, 'shop_id': 1, 'shop_sku': "4"},

    {'business_id': 2, 'shop_id': 2, 'shop_sku': "1"},

    {'business_id': 3, 'shop_id': 3, 'shop_sku': "1"},
    {'business_id': 3, 'shop_id': 3, 'shop_sku': "2"},
]


@pytest.fixture(scope='module')
def config(yt_server):
    cfg = {
        'general': {
            'color': 'white',
        },
    }
    return RoutinesConfigMock(yt_server=yt_server, config=cfg)


@pytest.fixture(scope='module')
def actual_service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server,
                                      config.yt_actual_service_offers_tablepath,
                                      data=ACTUAL_SERVICE_OFFERS)


@pytest.fixture(scope='module')
def partners_table(yt_server, config):
    return DataCampPartnersTable(yt_server,
                                 config.yt_partners_tablepath,
                                 data=PARTNERS)


@pytest.yield_fixture(scope='module')
def partner_stats_updater(yt_server, config, partners_table, actual_service_offers_table):
    resources = {
        'config': config,
        'partners_table': partners_table,
        'actual_service_offers_table': actual_service_offers_table,
    }
    with PartnerStatsUpdaterTestEnv(yt_server, **resources) as partner_stats_updater_env:
        partner_stats_updater_env.verify()
        partners_table.load()
        yield partner_stats_updater_env


def test_partner_stats_updater_updates_stats(partner_stats_updater, partners_table):
    assert_that(partners_table.data, HasDatacampPartersYtRows(
        [
            {
                'shop_id': 1,
                'partner_stat': IsSerializedProtobuf(PartnerStat, {'offers_count': 4}),
            },
            {
                'shop_id': 2,
                'partner_stat': IsSerializedProtobuf(PartnerStat, {'offers_count': 1}),
            },
            {
                'shop_id': 3,
                'partner_stat': IsSerializedProtobuf(PartnerStat, {'offers_count': 2}),
            }
        ]
    ))
