# coding: utf-8

import pytest
from datetime import datetime

from hamcrest import assert_that, equal_to, is_not
from market.idx.datacamp.controllers.scanner.yatf.test_env import make_scanner
from market.idx.pylibrary.datacamp.conversion import offer_to_service_row
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.datacamp.yatf.utils import create_meta, create_status, create_update_meta, dict2tskv
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.pylibrary.proto_utils import message_from_data

NOW = '2022-02-16 01:05:31.725916'
NOW_TS = 1644962731.725916
time_pattern = "%Y-%m-%dT%H:%M:%S.%fZ"

BUSINESS_ID = 1000
SHOP_ID = 1
SHOP_ID_CLICK_AND_COLLECT = 2
SHOP_DELETED_WH_ID = 3

WAREHOUSE_ID = 145
UNKNOWN_WAREHOUSE_ID = 150
DELETED_WAREHOUSE_ID = 151
DELETED_WAREHOUSE_WITHOUT_SUPPLIER_DATA_ID = 152


@pytest.fixture(scope='module')
def partners_table_data():
    return [
        {
            'shop_id': SHOP_ID,
            'mbi': '\n\n'.join([
                dict2tskv({
                    'shop_id': SHOP_ID,
                    'business_id': BUSINESS_ID,
                    'warehouse_id': WAREHOUSE_ID,
                    'ff_program': 'REAL',
                    'direct_shipping': True,
                    'blue_status': 'REAL',
                }),
            ]),
            'status': 'publish'
        },
        {
            'shop_id': SHOP_ID_CLICK_AND_COLLECT,
            'mbi': '\n\n'.join([
                dict2tskv({
                    'shop_id': SHOP_ID_CLICK_AND_COLLECT,
                    'business_id': BUSINESS_ID,
                    'warehouse_id': WAREHOUSE_ID,
                    'ff_program': 'NO',
                    'ignore_stocks': True,
                    'blue_status': 'REAL',
                }),
            ]),
            'status': 'publish'
        },
        {
            'shop_id': SHOP_DELETED_WH_ID,
            'mbi': '\n\n'.join([
                dict2tskv({
                    'shop_id': SHOP_DELETED_WH_ID,
                    'business_id': BUSINESS_ID,
                    'warehouse_id': DELETED_WAREHOUSE_ID,
                    'ff_program': 'NO',
                    'direct_shipping': True,
                    'blue_status': 'REAL',
                }),
            ]),
            'status': 'publish'
        }
    ]


@pytest.fixture(scope='module')
def service_offers_table_data():
    return [
        offer_to_service_row(
            DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=business_id,
                    shop_id=shop_id,
                    offer_id=offer_id,
                ),
                meta=create_meta(10, DTC.BLUE)
            )
        )
        for business_id, shop_id, offer_id in [
            (BUSINESS_ID, SHOP_DELETED_WH_ID, '7')
        ]
    ]


@pytest.fixture(scope='module')
def actual_service_offers_table_data():
    return [
        offer_to_service_row(
            DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=BUSINESS_ID,
                    shop_id=SHOP_ID,
                    warehouse_id=WAREHOUSE_ID,
                    offer_id='1',
                ),
                meta=create_meta(10, DTC.BLUE),
                status=create_status(True, 10, DTC.MARKET_STOCK),
                order_properties=DTC.OfferOrderProperties(
                    meta=create_update_meta(10),
                    order_method=DTC.OfferOrderProperties.AVAILABLE_FOR_ORDER,
                ),
                stock_info=DTC.OfferStockInfo(
                    market_stocks=DTC.OfferStocks(
                        meta=create_update_meta(10),
                        count=0
                    )
                )
            )
        ),
        offer_to_service_row(
            DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=BUSINESS_ID,
                    shop_id=SHOP_ID,
                    warehouse_id=WAREHOUSE_ID,
                    offer_id='3',
                ),
                meta=create_meta(10, DTC.BLUE),
                status=create_status(True, 10, DTC.MARKET_STOCK),
                order_properties=DTC.OfferOrderProperties(
                    meta=create_update_meta(10),
                    order_method=DTC.OfferOrderProperties.AVAILABLE_FOR_ORDER,
                ),
                stock_info=DTC.OfferStockInfo(
                    market_stocks=DTC.OfferStocks(
                        meta=create_update_meta(10),
                        count=0
                    )
                )
            )
        )
    ]


@pytest.fixture(scope='module')
def stock_sku_table_data():
    return [{
        # оффер, оличающийся от оффера в актуальной сервисной таблице, должен быть записан
        'supplier_id': SHOP_ID,
        'warehouse_id': WAREHOUSE_ID,
        'shop_sku': '1',
        'is_available': True,
        'is_preorder': True,
        'available_amount': 999,
        'updated_at': NOW
    },
    {
        # новый оффер, должен быть записан в актуальную сервисную таблицу
        'supplier_id': SHOP_ID,
        'warehouse_id': WAREHOUSE_ID,
        'shop_sku': '2',
        'is_available': True,
        'is_preorder': False,
        'available_amount': 888,
        'updated_at': NOW
    },
    {
        # оффер, не оличающийся от оффера в актуальной сервисной таблице, не должен быть записан
        'supplier_id': SHOP_ID,
        'warehouse_id': WAREHOUSE_ID,
        'shop_sku': '3',
        'is_available': False,
        'is_preorder': False,
        'available_amount': 0,
        'updated_at': NOW
    },
    {
        # оффер с неизвестного склада, не должен быть записан
        'supplier_id': SHOP_ID,
        'warehouse_id': UNKNOWN_WAREHOUSE_ID,
        'shop_sku': '4',
        'is_available': True,
        'is_preorder': False,
        'available_amount': 555,
        'updated_at': NOW
    },
    {
        # новый скрытый оффер, НЕ должен быть записан в актуальную сервисную таблицу
        'supplier_id': SHOP_ID,
        'warehouse_id': WAREHOUSE_ID,
        'shop_sku': '5',
        'is_available': False,
        'is_preorder': False,
        'available_amount': 0,
        'updated_at': NOW
    },
    {
        # оффер click & collect магазина, не должен быть записан
        'supplier_id': SHOP_ID_CLICK_AND_COLLECT,
        'warehouse_id': WAREHOUSE_ID,
        'shop_sku': '1',
        'is_available': True,
        'is_preorder': False,
        'available_amount': 777,
        'updated_at': NOW
    },
    {
        # не фф оффер у которого была удалена актуальная часть, но склад есть в shopsdat, должен быть записан
        'supplier_id': SHOP_DELETED_WH_ID,
        'warehouse_id': DELETED_WAREHOUSE_ID,
        'shop_sku': '7',
        'is_available': True,
        'is_preorder': False,
        'available_amount': 777,
        'updated_at': NOW
    },
    {
        # не фф оффер у которого была удалена актуальная часть, склад отсутствует в shopsdat, не должен быть записан
        'supplier_id': SHOP_DELETED_WH_ID,
        'warehouse_id': DELETED_WAREHOUSE_WITHOUT_SUPPLIER_DATA_ID,
        'shop_sku': '8',
        'is_available': True,
        'is_preorder': False,
        'available_amount': 777,
        'updated_at': NOW
    }]


@pytest.fixture(scope='module')
def scanner(
    log_broker_stuff,
    yt_server,
    scanner_resources,
    color,
):
    with make_scanner(yt_server, log_broker_stuff, color, shopsdat_cacher=True, **scanner_resources) as scanner_env:
        yield scanner_env


def test_update_united_market_stocks(scanner):
    # проверяем, что дедупликация отработала правильно
    wait_until(lambda: scanner.united_offers_processed > 0, timeout=60)
    assert_that(scanner.united_offers_processed, equal_to(4))

    assert_that(scanner.actual_service_offers_table.data,
                HasOffers([
                    message_from_data({
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'shop_id': SHOP_ID_CLICK_AND_COLLECT,
                            'warehouse_id': WAREHOUSE_ID,
                            'offer_id': '1',
                        },
                        'status': {
                            'disabled': [{
                                'flag': False,
                                'meta': {
                                    'timestamp': datetime.utcfromtimestamp(NOW_TS).strftime(time_pattern)
                                }
                            }]
                        },
                        'order_properties': {
                            'order_method': DTC.OfferOrderProperties.AVAILABLE_FOR_ORDER
                        },
                        'stock_info': {
                            'market_stocks': {
                                'count': 777,
                            }
                        }
                    }, DTC.Offer()),
                    message_from_data({
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'shop_id': SHOP_ID,
                            'warehouse_id': WAREHOUSE_ID,
                            'offer_id': '1',
                        },
                        'status': {
                            'disabled': [{
                                'flag': False,
                                'meta': {
                                    'timestamp': datetime.utcfromtimestamp(NOW_TS).strftime(time_pattern)
                                }
                            }]
                        },
                        'order_properties': {
                            'order_method': DTC.OfferOrderProperties.PRE_ORDERED
                        },
                        'stock_info': {
                            'market_stocks': {
                                'count': 999,
                            }
                        }
                    }, DTC.Offer()),
                    message_from_data({
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'shop_id': SHOP_ID,
                            'warehouse_id': WAREHOUSE_ID,
                            'offer_id': '2',
                        },
                        'status': {
                            'disabled': [{
                                'flag': False
                            }]
                        },
                        'order_properties': {
                            'order_method': DTC.OfferOrderProperties.AVAILABLE_FOR_ORDER
                        },
                        'stock_info': {
                            'market_stocks': {
                                'count': 888,
                            }
                        }
                    }, DTC.Offer()),
                    message_from_data({
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'shop_id': SHOP_DELETED_WH_ID,
                            'warehouse_id': DELETED_WAREHOUSE_ID,
                            'offer_id': '7',
                        },
                        'status': {
                            'disabled': [{
                                'flag': False
                            }]
                        },
                        'order_properties': {
                            'order_method': DTC.OfferOrderProperties.AVAILABLE_FOR_ORDER
                        },
                        'stock_info': {
                            'market_stocks': {
                                'count': 777,
                            }
                        }
                    }, DTC.Offer())])
                )


def test_cant_create_disabled_offers(scanner):
    """ Проверяем, что нельзя создать скрытый оффер, только офферы достпные к продаже """
    wait_until(lambda: scanner.united_offers_processed > 0, timeout=60)
    assert_that(scanner.united_offers_processed, equal_to(4))

    assert_that(scanner.actual_service_offers_table.data, is_not(HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': BUSINESS_ID,
                'shop_id': SHOP_ID,
                'warehouse_id': WAREHOUSE_ID,
                'offer_id': '5',
            }
        }, DTC.Offer()),
        message_from_data({
            'identifiers': {
                'business_id': BUSINESS_ID,
                'shop_id': SHOP_DELETED_WH_ID,
                'warehouse_id': DELETED_WAREHOUSE_WITHOUT_SUPPLIER_DATA_ID,
                'offer_id': '8',
            }
        }, DTC.Offer())
    ])))
