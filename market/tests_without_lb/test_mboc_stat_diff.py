# coding: utf-8

from hamcrest import assert_that, equal_to
import pytest

import yt.wrapper as yt

from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock
from market.idx.yatf.resources.yt_tables.mboc_tables import MbocStatTable

from market.idx.datacamp.routines.yatf.test_env import MbocStatOffersDiffCreatorEnv

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC


OFFERS = [
    {
        'identifiers': {
            'business_id': 1,
            'offer_id': 'offer1'
        },
        'meta': {
            'rgb': DTC.BLUE,
        },
        'status': {
            'version': {
                'actual_content_version': {
                    'counter': 2,
                }
            }
        }
    },
    {
        'identifiers': {
            'business_id': 2,
            'offer_id': 'offer2',
        },
        'meta': {
            'rgb': DTC.BLUE,
        },
        'partner_info': {
            'is_dsbs': True,
        },
        'status': {
            'version': {
                'actual_content_version': {
                    'counter': 3,
                }
            }
        }
    },
    {
        'identifiers': {
            'business_id': 4,
            'offer_id': 'offer4',
        },
        'meta': {
            'rgb': DTC.BLUE,
        },
        'status': {
            'version': {
                'actual_content_version': {
                    'counter': 4,
                }
            }
        }
    }
]

MBOC_STAT_TABLE = [
    {
        'is_base_offer': True,
        'is_datacamp_offer': True,
        'datacamp_content_version': 1,
        'business_id': 1,
        'shop_sku': "offer1",
        'supplier_id': 1
    },
    {
        'is_base_offer': True,
        'is_datacamp_offer': True,
        'datacamp_content_version': 2,
        'business_id': 2,
        'shop_sku': "offer2",
        'supplier_id': 2
    },
    {
        'is_base_offer': True,
        'is_datacamp_offer': True,
        'datacamp_content_version': 3,
        'business_id': 3,
        'shop_sku': "offer3",
        'supplier_id': 3
    },
]


@pytest.fixture(scope='module')
def config(yt_server):
    config = RoutinesConfigMock(
        yt_server,
        config={
            'yt': {
                'map_reduce_proxies': [yt_server.get_yt_client().config["proxy"]["url"]],
            },
            'general': {
                'yt_home': '//home/datacamp/united',
            },
            'mboc_stat_offers_diff': {
                'enable': True,
                'diff_offers_path': 'mboc_diff',
                'mboc_stat_table': '//mboc_offers'
            }
        })
    return config


@pytest.fixture(scope='module')
def basic_offers_table_data():
    return OFFERS


@pytest.fixture(scope='module')
def service_offers_table_data():
    return OFFERS


@pytest.fixture(scope='module')
def mboc_stat_table(yt_server, config):
    return MbocStatTable(yt_server, config.yt_mboc_stat_offers_table, data=[msg for msg in MBOC_STAT_TABLE])


@pytest.yield_fixture(scope='module')
def diff_builder(
        yt_server,
        config,
        basic_offers_table,
        service_offers_table,
        mboc_stat_table
):
    resources = {
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'mboc_stat_table': mboc_stat_table,
        'config': config,
    }
    with MbocStatOffersDiffCreatorEnv(yt_server, **resources) as routines_env:
        yield routines_env


def test_mboc_stat_diff(yt_server, config, diff_builder):
    yt_client = yt_server.get_yt_client()

    results = list(yt_client.read_table(yt.ypath_join(config.yt_home, config.yt_mboc_stat_offers_diff_path, 'recent', 'offers_diff')))
    assert_that(len(results), equal_to(2))
