# coding: utf-8

import time
from hamcrest import assert_that
import pytest

from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampServiceOffersTable, DataCampDcoTable
from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock
from market.idx.datacamp.routines.yatf.matchers.env_matchers import HasDatacampDcoYtRows
from market.idx.datacamp.routines.yatf.test_env import DcoUploaderTestEnv
from market.idx.datacamp.yatf.utils import create_meta
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC

BUSINESS_ID_1P = 1
SHOP_ID_1P = 1
SHOP_ID_NOT_1P = 2


def create_identifiers(offer, extra=False):
    ids = DTC.OfferIdentifiers(
        business_id=offer['business_id'],
        shop_id=offer['shop_id'],
        warehouse_id=offer['warehouse_id'],
        offer_id=offer['offer_id'],
        extra=DTC.OfferExtraIdentifiers(market_sku_id=offer['msku']) if extra else None
    )
    return ids


def create_offer_status(flags):
    status = DTC.OfferStatus()

    for i, meta in enumerate(flags):
        new_flag = DTC.Flag()

        new_flag.flag = True
        new_flag.meta.source = meta['source']
        if meta.get('timestamp'):
            new_flag.meta.timestamp.seconds = meta['timestamp']

        flag = status.disabled.add()
        flag.CopyFrom(new_flag)

    return status


def create_oldprice(basic, priority=None):
    price = DTC.OfferPrice()

    def set_price_bundle(price_bundle, oldprice, ts):
        price_bundle.binary_oldprice.id = 'RUR'
        price_bundle.binary_oldprice.price = oldprice * 10**7
        price_bundle.meta.timestamp.FromSeconds(ts)

    set_price_bundle(price.basic, basic[0], basic[1])
    if priority is not None:
        set_price_bundle(price.priority, priority[0], priority[1])

    return price


@pytest.fixture(
    scope='module',
    params=[
        {
            # case when dco table is empty, new msku should be inserted
            'offers_data': [
                {
                    'business_id': BUSINESS_ID_1P,
                    'shop_id': SHOP_ID_1P,
                    'warehouse_id': 10,
                    'offer_id': 'T1000',
                    'msku': 1,
                    'price': create_oldprice([100, 10], [200, 20]),
                    'meta': create_meta(0)
                },
                {
                    'business_id': BUSINESS_ID_1P,
                    'shop_id': SHOP_ID_1P,
                    'warehouse_id': 20,
                    'offer_id': 'T2000',
                    'msku': 2,
                    'price': create_oldprice([100, 20], [200, 10]),
                    'meta': create_meta(0)
                },
                {
                    'business_id': BUSINESS_ID_1P,
                    'shop_id': SHOP_ID_1P,
                    'warehouse_id': 30,
                    'offer_id': 'T3000',
                    'msku': 3,
                    'price': create_oldprice([100, 20]),
                    'meta': create_meta(0)
                },
                {
                    'business_id': BUSINESS_ID_1P,
                    'shop_id': SHOP_ID_NOT_1P,
                    'warehouse_id': 40,
                    'offer_id': 'T4000',
                    'msku': 3,
                    'price': create_oldprice([100, 10], [200, 20]),
                    'meta': create_meta(0)
                }
            ],
            'current_dco_table_data': [],
            'expected_dco_table_data': [
                {
                    'msku': 1,
                    'price': create_oldprice([200, 10], [200, 20]).SerializeToString()
                },
                {
                    'msku': 2,
                    'price': create_oldprice([100, 20], [200, 10]).SerializeToString()
                },
                {
                    'msku': 3,
                    'price': create_oldprice([100, 20]).SerializeToString()
                }
            ]
        },
        {
            # case when there are few record for one msku, the highest price should be written
            'offers_data': [
                {
                    'business_id': BUSINESS_ID_1P,
                    'shop_id': SHOP_ID_1P,
                    'warehouse_id': 10,
                    'offer_id': 'T1000',
                    'msku': 1,
                    'price': create_oldprice([200, 20], [200, 10]),
                    'meta': create_meta(0)
                },
                {
                    'business_id': BUSINESS_ID_1P,
                    'shop_id': SHOP_ID_1P,
                    'warehouse_id': 20,
                    'offer_id': 'T2000',
                    'msku': 1,
                    'price': create_oldprice([100, 20], [200, 10]),
                    'meta': create_meta(0)
                }
            ],
            'current_dco_table_data': [],
            'expected_dco_table_data': [
                {
                    'msku': 1,
                    'price': create_oldprice([200, 20], [200, 10]).SerializeToString()
                }
            ]
        },
        {
            # case when previous value of oldprice is less than new one, so it should be written in table
            'offers_data': [
                {
                    'business_id': BUSINESS_ID_1P,
                    'shop_id': SHOP_ID_1P,
                    'warehouse_id': 10,
                    'offer_id': 'T1000',
                    'msku': 1,
                    'price': create_oldprice([300, 20], [200, 10]),
                    'meta': create_meta(0)
                }
            ],
            'current_dco_table_data': [
                {
                    'msku': 1,
                    'price': create_oldprice([200, 10], [200, 20]).SerializeToString(),
                    'timestamp': int(time.time() - 60)
                }
            ],
            'expected_dco_table_data': [
                {
                    'msku': 1,
                    'price': create_oldprice([300, 20], [200, 10]).SerializeToString()
                }
            ]
        },
        {
            # case when previous value of oldprice is bigger than new one, it should not be written in table
            'offers_data': [
                {
                    'business_id': BUSINESS_ID_1P,
                    'shop_id': SHOP_ID_1P,
                    'warehouse_id': 10,
                    'offer_id': 'T1000',
                    'msku': 1,
                    'price': create_oldprice([100, 20], [200, 10]),
                    'meta': create_meta(0)
                }
            ],
            'current_dco_table_data': [
                {
                    'msku': 1,
                    'price': create_oldprice([200, 10], [200, 20]).SerializeToString(),
                    'timestamp': int(time.time())
                }
            ],
            'expected_dco_table_data': [
                {
                    'msku': 1,
                    'price': create_oldprice([200, 10], [200, 20]).SerializeToString()
                }
            ]
        },
        {
            # case when old price is higher than new one, but it was inserted in table 24 hours ago, new price should be inserted
            'offers_data': [
                {
                    'business_id': BUSINESS_ID_1P,
                    'shop_id': SHOP_ID_1P,
                    'warehouse_id': 10,
                    'offer_id': 'T1000',
                    'msku': 1,
                    'price': create_oldprice([100, 20], [200, 10]),
                    'meta': create_meta(0)
                }
            ],
            'current_dco_table_data': [
                {
                    'msku': 1,
                    'price': create_oldprice([200, 10], [200, 20]).SerializeToString(),
                    'timestamp': int(time.time() - 25 * 60 * 60)
                }
            ],
            'expected_dco_table_data': [
                {
                    'msku': 1,
                    'price': create_oldprice([100, 20], [200, 10]).SerializeToString()
                }
            ]
        },
        {
            # case when there are few disabled offers, some of them should be ignored
            'offers_data': [
                {
                    'business_id': BUSINESS_ID_1P,
                    'shop_id': SHOP_ID_1P,
                    'warehouse_id': 10,
                    'offer_id': 'T1000',
                    'msku': 1,
                    'price': create_oldprice([100, 0]),
                    'meta': create_meta(0)
                },
                {
                    'business_id': BUSINESS_ID_1P,
                    'shop_id': SHOP_ID_1P,
                    'warehouse_id': 20,
                    'offer_id': 'T2000',
                    'msku': 1,
                    'price': create_oldprice([300, 0]),
                    'meta': create_meta(0),
                    'status': create_offer_status([{
                        'source': DTC.PUSH_PARTNER_API,
                        'timestamp': int(time.time()) - 3 * 24 * 60 * 60
                    }])
                },
                {
                    'business_id': BUSINESS_ID_1P,
                    'warehouse_id': 30,
                    'shop_id': SHOP_ID_1P,
                    'offer_id': 'T3000',
                    'msku': 1,
                    'price': create_oldprice([200, 0]),
                    'meta': create_meta(0),
                    'status': create_offer_status([{
                        'source': DTC.PUSH_PARTNER_FEED,
                        'timestamp': int(time.time()) - 1 * 24 * 60 * 60
                    }])
                },
                {
                    'business_id': BUSINESS_ID_1P,
                    'shop_id': SHOP_ID_1P,
                    'warehouse_id': 40,
                    'offer_id': 'T4000',
                    'msku': 2,
                    'price': create_oldprice([100, 0]),
                    'meta': create_meta(0),
                    'status': create_offer_status([{
                        'source': DTC.PUSH_PARTNER_FEED,
                    }])
                },
            ],
            'current_dco_table_data': [],
            'expected_dco_table_data': [
                {
                    'msku': 1,
                    'price': create_oldprice([200, 0]).SerializeToString()
                },
                {
                    'msku': 2,
                    'price': create_oldprice([100, 0]).SerializeToString()
                }
            ]
        },
    ],
    ids=[
        'empty_dco_table',
        'for_msku_with_few_offers_only_higher_is_written',
        'replace_smaller_old_price',
        'replace_higher_old_price',
        'replace_higher_but_old_price',
        'ignore_some_disabled_offers'
    ]
)
def gen_data(request):
    return request.param


@pytest.fixture(scope='module')
def config(yt_server, gen_data):
    cfg = {
        'general': {
            'color': 'white',
        },
        'routines': {
            'enable_dco_uploader': True,
            'business_id_1p': BUSINESS_ID_1P,
            'shop_id_1p': SHOP_ID_1P,
            'united_select_rows_limit': 2,
        }
    }
    return RoutinesConfigMock(yt_server=yt_server, config=cfg)


@pytest.fixture(scope='module')
def partners_table_data():
    return [
        {
            'shop_id': SHOP_ID_1P,
            'mbi': {'business_id': BUSINESS_ID_1P, 'shop_id': SHOP_ID_1P, 'supplier_type': 1, 'datafeed_id': 1}
        },
        {
            'shop_id': SHOP_ID_NOT_1P,
            'mbi': {'business_id': BUSINESS_ID_1P, 'shop_id': SHOP_ID_NOT_1P, 'supplier_type': 3, 'datafeed_id': 2}
        }
    ]


@pytest.fixture()
def service_offers_table(yt_server, config, gen_data):
    return DataCampServiceOffersTable(
        yt_server,
        config.yt_service_offers_tablepath,
        data=[{
            'business_id': offer['business_id'],
            'shop_id': offer['shop_id'],
            'warehouse_id': 0,
            'outlet_id': 0,
            'shop_sku': offer['offer_id'],
            'offer': DTC.Offer(
                identifiers=create_identifiers(offer),
                meta=offer['meta'],
                status=offer['status'] if 'status' in offer else None,
                price=offer['price']
            ).SerializeToString()
        } for offer in gen_data['offers_data']])


@pytest.fixture()
def actual_service_offers_table(yt_server, config, gen_data):
    return DataCampServiceOffersTable(
        yt_server,
        config.yt_actual_service_offers_tablepath,
        data=[{
            'business_id': offer['business_id'],
            'shop_id': offer['shop_id'],
            'warehouse_id': offer['warehouse_id'],
            'shop_sku': offer['offer_id'],
            'offer': DTC.Offer(
                identifiers=create_identifiers(offer, extra=True),
                meta=offer['meta']
            ).SerializeToString()
        } for offer in gen_data['offers_data']])


@pytest.fixture()
def dco_table(yt_server, config, gen_data):
    return DataCampDcoTable(yt_server,
                            config.dco_tablepath,
                            data=gen_data['current_dco_table_data'])


@pytest.yield_fixture()
def dco(yt_server, config, partners_table, service_offers_table, actual_service_offers_table, dco_table):
    resources = {
        'config': config,
        'partners_table': partners_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'dco_table': dco_table
    }
    with DcoUploaderTestEnv(yt_server, **resources) as dco_env:
        dco_env.verify()
        yield dco_env


def test_united_dco_uploader(dco, dco_table, gen_data):
    dco_table.load()
    assert_that(dco_table.data, HasDatacampDcoYtRows(gen_data['expected_dco_table_data']))
