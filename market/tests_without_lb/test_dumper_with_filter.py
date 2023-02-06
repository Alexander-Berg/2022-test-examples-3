# encoding=utf-8

from datetime import datetime
from hamcrest import assert_that, has_length, has_items, has_entries
import pytest
import yt.wrapper as yt


import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock
from market.idx.datacamp.routines.yatf.test_env import UnitedDatacampDumperEnv
from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.yatf.utils.utils import create_timestamp_from_json, create_pb_timestamp

NOW_TIME_UTC = datetime.utcnow()
time_pattern = "%Y-%m-%dT%H:%M:%SZ"
NOW_TIME = NOW_TIME_UTC.strftime(time_pattern)
NOW_TS = create_timestamp_from_json(NOW_TIME)


OFFERS_DATA = [
    # business_id, shop_id, offer_id, warehouse_id, revision, ts, disable_flag, color, is_disabled
    (1, 1, "1", 1, 1, NOW_TS.seconds-10, False, DTC.WHITE, False),
    (1, 1, "2", 1, 10, NOW_TS.seconds-10, False, DTC.WHITE, False),
    (1, 1, "3", 1, 10, NOW_TS.seconds, False, DTC.WHITE, False),
    (1, 1, "4", 1, 10, NOW_TS.seconds-5, False, DTC.WHITE, False),
    (1, 1, "5", 1, 10, NOW_TS.seconds-4, False, DTC.WHITE, False),
]


class IdsTable(YtTableResource):
    def __init__(self, yt_server, path, data=None):
        super(IdsTable, self).__init__(
            yt_stuff=yt_server,
            path=path,
            attributes={'schema': [
                dict(name='business_id', type='uint32', sort_order='ascending'),
                dict(name='shop_sku', type='string', sort_order='ascending'),
                dict(name='shop_id', type='uint32', sort_order='ascending'),
                dict(name='warehouse_id', type='uint32', sort_order='ascending'),
                dict(name='last_mine', type='uint64'),
            ]},
            data=data
        )


@pytest.fixture(scope='module')
def basic_offers_table_data():
    return [
        {
            'identifiers': {
                'business_id': business_id,
                'offer_id': offer_id,
            },
            'tech_info': {
                'last_mining': {
                    'revision': revision,
                    'meta': {
                        'timestamp': create_pb_timestamp(ts).ToJsonString(),
                    }
                }
            },
            'status': {
                'disabled': [{
                    'flag': disable_flag,
                }],
            },
            'meta': {
                'rgb': color,
            },
            'content': {
                'partner': {
                    'actual': {
                        'title': {
                            'value': 'title'
                        }
                    }
                },
            },
            'partner_info': {
                'is_disabled': is_disabled,
            }
        } for business_id, _, offer_id, _, revision, ts, disable_flag, color, is_disabled in OFFERS_DATA
    ]


@pytest.fixture(scope='module')
def service_offers_table_data():
    return [
        {
            'identifiers': {
                'business_id': business_id,
                'offer_id': offer_id,
                'shop_id': shop_id,
            },
            'tech_info': {
                'last_mining': {
                    'revision': revision,
                    'meta': {
                        'timestamp': create_pb_timestamp(ts).ToJsonString(),
                    }
                }
            },
            'status': {
                'disabled': [{
                    'flag': disable_flag,
                }]
            },
            'meta': {
                'rgb': color,
            },
            'partner_info': {
                'is_disabled': is_disabled,
            }
        } for business_id, shop_id, offer_id, _, revision, ts, disable_flag, color, is_disabled in OFFERS_DATA
    ]


@pytest.fixture(scope='module')
def actual_service_offers_table_data():
    return [
        {
            'identifiers': {
                'business_id': business_id,
                'offer_id': offer_id,
                'shop_id': shop_id,
                'warehouse_id': warehouse_id,
            },
            'tech_info': {
                'last_mining': {
                    'revision': revision,
                    'meta': {
                        'timestamp': create_pb_timestamp(ts).ToJsonString(),
                    }
                }
            },
            'status': {
                'disabled': [{
                    'flag': disable_flag,
                }]
            },
            'meta': {
                'rgb': color,
            },
            'partner_info': {
                'is_disabled': is_disabled,
            }

        } for business_id, shop_id, offer_id, warehouse_id, revision, ts, disable_flag, color, is_disabled in OFFERS_DATA
    ]


@pytest.fixture(scope='module')
def ids_table_path(config):
    return yt.ypath_join(config.yt_home, 'ids_table')


@pytest.fixture(scope='module')
def ids_table(yt_server, ids_table_path):
    return IdsTable(yt_server, ids_table_path, data=[{
        'business_id': 1,
        'shop_sku': '1',
        'shop_id': 1,
        'warehouse_id': 1,
        'last_mine': NOW_TS.seconds-5
    },
    {
        'business_id': 1,
        'shop_sku': '2',
        'shop_id': 1,
        'warehouse_id': 1,
        'last_mine': NOW_TS.seconds-5
    },
    {
        'business_id': 1,
        'shop_sku': '3',
        'shop_id': 1,
        'warehouse_id': 1,
        'last_mine': NOW_TS.seconds-5
    },
    {
        'business_id': 1,
        'shop_sku': '5',
        'shop_id': 1,
        'warehouse_id': 1,
        'last_mine': NOW_TS.seconds-5
    }])


@pytest.fixture(scope='module')
def config(yt_server):
    cfg = {
        'general': {
            'color': 'white',
            'yt_home': '//home/datacamp/united'
        },
        'routines': {
            'enable_united_datacamp_dumper': True,
            'days_number_to_take_disabled_offer_in_index': 5,
            'force_mine_ids_table': yt.ypath_join('//home/datacamp/united', 'ids_table'),
            # 'force_mine_ids_table': '//home/datacamp/united/ids_table',
            'skip_offers_in_mine_ids_table': True
        },
        'yt': {
            'white_out': 'white_out',
            'blue_out': 'blue_out',
            'turbo_out': 'turbo_out',
            'blue_turbo_out': 'blue_turbo_out',
            'lavka_out': 'lavka_out',
            'eda_out': 'eda_out',
            'dumper_log': 'dumper_log',
            'map_reduce_proxies': [yt_server.get_yt_client().config["proxy"]["url"]],
        }
    }
    return RoutinesConfigMock(
        yt_server=yt_server,
        config=cfg)


@pytest.fixture(scope='module')
def partners_table_data():
    return [
        {
            'shop_id': 1,
            'mbi': [
                {'shop_id': 1, 'business_id': 1},
            ],
        }
    ]


@pytest.yield_fixture(scope='module')
def routines(
    yt_server,
    config,
    partners_table,
    basic_offers_table,
    service_offers_table,
    actual_service_offers_table,
    ids_table
):
    resources = {
        'partners_table': partners_table,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'ids_table': ids_table,
        'config': config,
    }
    with UnitedDatacampDumperEnv(yt_server, **resources) as routines_env:
        yield routines_env


def test_filtered_out_table(yt_server, routines):
    """
    Offer from ids table to mine not in out table
    """
    yt_client = yt_server.get_yt_client()

    offers = list(yt_client.read_table(yt.ypath_join('//home/datacamp/united/white_out/recent')))
    assert_that(offers, has_length(3))

    assert_that(offers, has_items(
        has_entries({
            'business_id': 1,
            'offer_id': '3'
        }),
    ))
    assert_that(offers, has_items(
        has_entries({
            'business_id': 1,
            'offer_id': '4'
        }),
    ))
    assert_that(offers, has_items(
        has_entries({
            'business_id': 1,
            'offer_id': '5'
        }),
    ))
