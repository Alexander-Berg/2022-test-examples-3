# coding: utf-8

import pytest
import uuid
import datetime

from hamcrest import assert_that, equal_to
from market.proto.common.common_pb2 import SCANNER
from market.idx.datacamp.controllers.scanner.yatf.test_env import make_scanner
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.pylibrary.datacamp.conversion import offer_to_service_row
from market.idx.datacamp.yatf.utils import create_update_meta, create_meta, dict2tskv
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
from market.proto.common.common_pb2 import PriceExpression
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.pylibrary.proto_utils import message_from_data

NOW = datetime.datetime.now()
UPDATE_TS = NOW + datetime.timedelta(seconds=3)
BUSINESS_ID = 1000
SHOP_ID = 1
WAREHOUSE_ID = 2
time_pattern = "%Y-%m-%dT%H:%M:%SZ"


@pytest.fixture(scope='module')
def partners_table_data():
    return [
        {
            'shop_id': SHOP_ID,
            'mbi': '\n\n'.join([
                dict2tskv({
                    'shop_id': SHOP_ID,
                    'business_id': BUSINESS_ID,
                    'blue_status': 'REAL',
                }),
            ]),
            'status': 'publish'
        }
    ]


@pytest.fixture(scope='module')
def service_offers_table_data(color):
    return [
        offer_to_service_row(
            DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=BUSINESS_ID,
                    shop_id=SHOP_ID,
                    offer_id='1',
                ),
                meta=create_meta(10, DTC.BLUE if color == 'blue' else DTC.WHITE),
                promos=DTC.OfferPromos(
                    anaplan_promos=DTC.MarketPromos(
                        all_promos=DTC.Promos(
                            promos=[
                                DTC.Promo(
                                    id='promo1',
                                    discount_oldprice=PriceExpression(
                                        id='RUR',
                                        price=120,
                                    ),
                                )
                            ]
                        )
                    )
                ),
            )
        ),
        offer_to_service_row(
            DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=BUSINESS_ID,
                    shop_id=SHOP_ID,
                    offer_id='2',
                ),
                meta=create_meta(10, DTC.BLUE if color == 'blue' else DTC.WHITE),
            )
        )
    ]


@pytest.fixture(scope='module')
def promo_table_path():
    return '//home/promo' + str(uuid.uuid4()) + '/{}'.format(NOW.strftime("%Y%m%d_%H%M"))


@pytest.fixture(scope='module')
def promo_table_data():
    return [{
        'shop_id': SHOP_ID,
        'offer_id': '1',
        'warehouse_id': WAREHOUSE_ID,
        'promos': DTC.OfferPromos(
            anaplan_promos=DTC.MarketPromos(
                all_promos=DTC.Promos(
                    meta=create_update_meta(get_ts_seconds(UPDATE_TS), DTC.DataSource.MARKET_MBI),
                    promos=[
                        DTC.Promo(
                            id='promo1',
                            discount_oldprice=PriceExpression(
                                id='RUR',
                                price=120,
                            ),
                        ), DTC.Promo(
                            id='promo2',
                            discount_oldprice=PriceExpression(
                                id='RUR',
                                price=250,
                            ),
                        )
                    ]
                )
            )
        ).SerializeToString()
    }, {
        'shop_id': SHOP_ID,
        'offer_id': '2',
        'warehouse_id': WAREHOUSE_ID,
        'promos': DTC.OfferPromos(
            anaplan_promos=DTC.MarketPromos(
                all_promos=DTC.Promos(
                    meta=create_update_meta(get_ts_seconds(UPDATE_TS), DTC.DataSource.MARKET_MBI),
                    promos=[
                        DTC.Promo(
                            id='promo1',
                            discount_oldprice=PriceExpression(
                                id='RUR',
                                price=100,
                            ),
                        ), DTC.Promo(
                            id='promo2',
                            discount_oldprice=PriceExpression(
                                id='RUR',
                                price=200,
                            ),
                        )
                    ]
                )
            )
        ).SerializeToString()
    }, {
        'shop_id': SHOP_ID,
        'offer_id': '3',
        'warehouse_id': WAREHOUSE_ID,
        'promos': DTC.OfferPromos().SerializeToString()
    }]


@pytest.fixture(scope='module')
def scanner(
    log_broker_stuff,
    yt_server,
    scanner_resources,
    color,
):
    with make_scanner(yt_server, log_broker_stuff, color, shopsdat_cacher=True, **scanner_resources) as scanner_env:
        wait_until(lambda: scanner_env.united_offers_processed > 0, timeout=60)
        yield scanner_env


def get_ts_seconds(ts):
    return int((ts - datetime.datetime(1970, 1, 1)).total_seconds())


@pytest.mark.parametrize("expected", [
    (
        '1',
        {
            'identifiers': {
                'business_id': BUSINESS_ID,
                'shop_id': SHOP_ID,
                'offer_id': '1',
            },
            'promos': {
                'anaplan_promos': {
                    'all_promos': {
                        'meta': {
                            'timestamp': UPDATE_TS.strftime(time_pattern),
                            'source': DTC.DataSource.MARKET_MBI,
                            'applier': SCANNER,
                        },
                        'promos': [
                            {
                                'id': 'promo1',
                                'discount_oldprice': {
                                    'price': 120,
                                },
                            },
                            {
                                'id': 'promo2',
                                'discount_oldprice': {
                                    'price': 250,
                                },
                            },
                        ]
                    }
                }
            }
        }
    ),
    (
        '2',
        {
            'identifiers': {
                'business_id': BUSINESS_ID,
                'shop_id': SHOP_ID,
                'offer_id': '2',
            },
            'promos': {
                'anaplan_promos': {
                    'all_promos': {
                        'meta': {
                            'timestamp': UPDATE_TS.strftime(time_pattern),
                            'source': DTC.DataSource.MARKET_MBI,
                            'applier': SCANNER,
                        },
                        'promos': [
                            {
                                'id': 'promo1',
                                'discount_oldprice': {
                                    'price': 100,
                                },
                            },
                            {
                                'id': 'promo2',
                                'discount_oldprice': {
                                    'price': 200,
                                },
                            },
                        ]
                    }
                }
            }
        }
    ),
])
def test_promo_modify_anaplan_promos(scanner, expected):
    """ Промоакции только модифицируют таблицы, но не добавляют новые офферы """
    shop_sku, data = expected
    assert_that(scanner.service_offers_table.data, HasOffers([message_from_data(data, DTC.Offer())]))
    assert_that(len(scanner.actual_service_offers_table.data), equal_to(0))
