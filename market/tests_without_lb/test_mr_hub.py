# coding: utf-8

from datetime import datetime
from hamcrest import assert_that, has_entries, equal_to, has_items, is_not
import pytest
import logging

import yt.wrapper as yt

from market.idx.yatf.resources.resource import Resource

from market.idx.yatf.resources.yt_table_resource import convert_row
from market.idx.yatf.resources.yt_tables.stock_sku_table import StockSkuTable
from market.idx.datacamp.yatf.utils import create_meta, create_status, create_update_meta, dict2tskv

from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock
from market.idx.pylibrary.datacamp.conversion import offer_to_service_row
from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampPartnersTable,
    DataCampServiceOffersTable
)
from market.idx.datacamp.routines.yatf.test_env import MRHubEnv
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC

NOW_TS = datetime.utcnow()
time_pattern = "%Y-%m-%dT%H:%M:%S.%fZ"
NOW = NOW_TS.strftime(time_pattern)

log = logging.getLogger()


class LinkResource(Resource):
    def __init__(self, yt_server, link, table):
        self.yt_server = yt_server
        self.link = link
        self.table = table

    def init(self, env):
        super(LinkResource, self).init(env=env)
        yt_client = self.yt_server.get_yt_client()
        yt_client.link(self.table, self.link)

    @property
    def path(self):
        return self.link


@pytest.fixture(scope='module')
def partners_table_data():
    return [
        {
            'shop_id': 1,
            'mbi': '\n\n'.join([
                dict2tskv({
                    'shop_id': 1,
                    'business_id': 1,
                    'warehouse_id': 145,
                    'ff_program': 'REAL',
                    'direct_shipping': True,
                    'blue_status': 'REAL',
                }),
            ]),
            'status': 'publish'
        },
        {
            'shop_id': 1,
            'mbi': '\n\n'.join([
                dict2tskv({
                    'shop_id': 1,
                    'business_id': 1,
                    'warehouse_id': 146,
                    'ff_program': 'NO',
                    'ignore_stocks': True,
                    'blue_status': 'REAL',
                }),
            ]),
            'status': 'publish'
        },
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
            (1, 1, '1'),
            (1, 1, '2'),
            (1, 1, '3'),
            (1, 1, '4'),
            (1, 1, '5'),
            (1, 1, '7'),
        ]
    ]


@pytest.fixture(scope='module')
def actual_service_offers_table_data():
    return [
        offer_to_service_row(
            DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=1,
                    shop_id=1,
                    warehouse_id=146,
                    offer_id='1',
                ),
                meta=create_meta(10, DTC.BLUE),
                status=create_status(False, 10, DTC.MARKET_STOCK),
                order_properties=DTC.OfferOrderProperties(
                    meta=create_update_meta(10),
                    order_method=DTC.OfferOrderProperties.PRE_ORDERED,
                ),
                stock_info=DTC.OfferStockInfo(
                    market_stocks=DTC.OfferStocks(
                        meta=create_update_meta(10),
                        count=1
                    )
                )
            )
        ),
        offer_to_service_row(
            DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=1,
                    shop_id=1,
                    warehouse_id=146,
                    offer_id='2',
                ),
                meta=create_meta(10, DTC.BLUE),
                status=create_status(False, 10, DTC.MARKET_STOCK),
                order_properties=DTC.OfferOrderProperties(
                    meta=create_update_meta(10),
                    order_method=DTC.OfferOrderProperties.PRE_ORDERED,
                ),
                stock_info=DTC.OfferStockInfo(
                    market_stocks=DTC.OfferStocks(
                        meta=create_update_meta(10),
                        count=222
                    )
                )
            )
        ),
        offer_to_service_row(
            DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=1,
                    shop_id=1,
                    warehouse_id=146,
                    offer_id='3',
                ),
                meta=create_meta(10, DTC.BLUE),
                status=create_status(False, 10, DTC.MARKET_STOCK),
                order_properties=DTC.OfferOrderProperties(
                    meta=create_update_meta(10),
                    order_method=DTC.OfferOrderProperties.PRE_ORDERED,
                ),
                stock_info=DTC.OfferStockInfo(
                    market_stocks=DTC.OfferStocks(
                        meta=create_update_meta(10),
                        count=333
                    )
                )
            )
        ),
        offer_to_service_row(
            DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=1,
                    shop_id=1,
                    warehouse_id=145,
                    offer_id='4',
                ),
                meta=create_meta(10, DTC.BLUE),
                status=create_status(False, 10, DTC.MARKET_STOCK),
                order_properties=DTC.OfferOrderProperties(
                    meta=create_update_meta(10),
                    order_method=DTC.OfferOrderProperties.PRE_ORDERED,
                ),
                stock_info=DTC.OfferStockInfo(
                    market_stocks=DTC.OfferStocks(
                        meta=create_update_meta(10),
                        count=444
                    )
                )
            )
        ),
        offer_to_service_row(
            DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=1,
                    shop_id=1,
                    warehouse_id=146,
                    offer_id='5',
                ),
                meta=create_meta(10, DTC.BLUE),
                status=create_status(False, 10, DTC.MARKET_STOCK),
                order_properties=DTC.OfferOrderProperties(
                    meta=create_update_meta(10),
                    order_method=DTC.OfferOrderProperties.PRE_ORDERED,
                ),
                stock_info=DTC.OfferStockInfo(
                    market_stocks=DTC.OfferStocks(
                        meta=create_update_meta(10),
                        count=555
                    )
                )
            )
        ),
    ]


@pytest.fixture(scope='module')
def stock_sku_table_data():
    return [
        {
            # отличаются стоки
            'supplier_id': 1,
            'warehouse_id': 146,
            'shop_sku': '1',
            'is_available': True,
            'is_preorder': True,
            'available_amount': 111,
            'updated_at': NOW
        },
        {
            # отличается статус
            'supplier_id': 1,
            'warehouse_id': 146,
            'shop_sku': '2',
            'is_available': False,
            'is_preorder': True,
            'available_amount': 222,
            'updated_at': NOW
        },
        {
            # отличаются условия заказа
            'supplier_id': 1,
            'warehouse_id': 146,
            'shop_sku': '3',
            'is_available': True,
            'is_preorder': False,
            'available_amount': 333,
            'updated_at': NOW
        },
        {
            # ничего не отличается, должен быть отфильтрован
            'supplier_id': 1,
            'warehouse_id': 145,
            'shop_sku': '4',
            'is_available': True,
            'is_preorder': True,
            'available_amount': 444,
            'updated_at': NOW
        },
        {
            # отличаются стоки, но таймстемп старый, должен быть отфильтрован
            'supplier_id': 1,
            'warehouse_id': 146,
            'shop_sku': '5',
            'is_available': True,
            'is_preorder': True,
            'available_amount': 10,
            'updated_at': '1970-01-01T00:00:10Z'
        },
        {
            # нет актуальной и сервисной частей, склад фф
            'supplier_id': 1,
            'warehouse_id': 145,
            'shop_sku': '6',
            'is_available': True,
            'is_preorder': False,
            'available_amount': 666,
            'updated_at': NOW
        },
        {
            # нет актуальной части, но есть сервисная
            'supplier_id': 1,
            'warehouse_id': 146,
            'shop_sku': '7',
            'is_available': True,
            'is_preorder': False,
            'available_amount': 777,
            'updated_at': NOW
        },
        {
            # нет актуальной и сервисной частей, не фф склад, должен быть отфильтрован
            'supplier_id': 1,
            'warehouse_id': 146,
            'shop_sku': '8',
            'is_available': True,
            'is_preorder': False,
            'available_amount': 888,
            'updated_at': NOW
        }
    ]


@pytest.fixture(scope='module')
def table_name():
    now = datetime.utcnow()
    time_pattern = "%Y-%m-%d"
    return now.strftime(time_pattern)


@pytest.fixture(scope='module')
def stock_sku_table(yt_server, table_name, stock_sku_table_data):
    return StockSkuTable(yt_server, yt.ypath_join('//home/stock_sku/', table_name), stock_sku_table_data)


@pytest.fixture(scope='module')
def stock_sku_table_recent_link(yt_server, stock_sku_table):
    return LinkResource(yt_server, '//home/stock_sku/recent', stock_sku_table.table_path)


@pytest.fixture(scope='module')
def config(yt_server, stock_sku_table_recent_link):
    config = RoutinesConfigMock(
        yt_server,
        config={
            'general': {
                'color': 'white',
                'yt_home': '//home/datacamp/united'
            },
            'yt': {
                'map_reduce_proxies': [yt_server.get_yt_client().config["proxy"]["url"]]
            },
            'mr_hub': {
                'enable': True,
                'stock_diff_dir': 'stock_diff',
                'stock_dump_from_stock_storage': stock_sku_table_recent_link.path
            }
        })
    return config


@pytest.fixture(scope='module')
def partners_table(yt_server, config, partners_table_data):
    return DataCampPartnersTable(yt_server, config.yt_partners_tablepath, data=partners_table_data)


@pytest.fixture(scope='module')
def service_offers_table(yt_server, config, service_offers_table_data):
    return DataCampServiceOffersTable(yt_server, config.yt_service_offers_tablepath, data=service_offers_table_data)


@pytest.fixture(scope='module')
def actual_service_offers_table(yt_server, config, actual_service_offers_table_data):
    return DataCampServiceOffersTable(yt_server, config.yt_actual_service_offers_tablepath, data=actual_service_offers_table_data)


@pytest.yield_fixture(scope='module')
def routines(
    yt_server,
    config,
    partners_table,
    service_offers_table,
    actual_service_offers_table,
    stock_sku_table_recent_link,
    stock_sku_table
):
    resources = {
        'partners_table': partners_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'config': config,
        'stock_sku_table': stock_sku_table,
        'stock_sku_table_recent_link': stock_sku_table_recent_link
    }

    with MRHubEnv(yt_server, **resources) as routines_env:
        yield routines_env


def test_mr_hub(yt_server, routines, config, table_name):
    yt_client = yt_server.get_yt_client()
    rows = []
    for r in yt_client.read_table(yt.ypath_join(config.yt_home, config.stock_diff_dir, table_name)):
        row = convert_row(r)
        rows.append(row)

    assert_that(len(rows), equal_to(5))
    assert_that(rows, has_items(
        has_entries({
            'business_id': 1,
            'shop_id': 1,
            'warehouse_id': 146,
            'shop_sku': '1',
            'status': None,
            'order_properties': None,
            'stock_info': IsSerializedProtobuf(DTC.OfferStockInfo, {
                'market_stocks': {
                    'count': 111
                }
            })
        }),
        has_entries({
            'business_id': 1,
            'shop_id': 1,
            'warehouse_id': 146,
            'shop_sku': '2',
            'status': IsSerializedProtobuf(DTC.OfferStatus, {
                'disabled': [{
                    'flag': True
                }]
            }),
            'order_properties': None,
            'stock_info': None
        }),
        has_entries({
            'business_id': 1,
            'shop_id': 1,
            'warehouse_id': 146,
            'shop_sku': '3',
            'status': None,
            'order_properties': IsSerializedProtobuf(DTC.OfferOrderProperties, {
                'order_method': DTC.OfferOrderProperties.AVAILABLE_FOR_ORDER
            }),
            'stock_info': None
        }),
        has_entries({
            'business_id': 1,
            'shop_id': 1,
            'warehouse_id': 145,
            'shop_sku': '6',
            'status': IsSerializedProtobuf(DTC.OfferStatus, {
                'disabled': [{
                    'flag': False
                }]
            }),
            'order_properties': IsSerializedProtobuf(DTC.OfferOrderProperties, {
                'order_method': DTC.OfferOrderProperties.AVAILABLE_FOR_ORDER
            }),
            'stock_info': IsSerializedProtobuf(DTC.OfferStockInfo, {
                'market_stocks': {
                    'count': 666
                }
            })
        }),
        has_entries({
            'business_id': 1,
            'shop_id': 1,
            'warehouse_id': 146,
            'shop_sku': '7',
            'status': IsSerializedProtobuf(DTC.OfferStatus, {
                'disabled': [{
                    'flag': False
                }]
            }),
            'order_properties': IsSerializedProtobuf(DTC.OfferOrderProperties, {
                'order_method': DTC.OfferOrderProperties.AVAILABLE_FOR_ORDER
            }),
            'stock_info': IsSerializedProtobuf(DTC.OfferStockInfo, {
                'market_stocks': {
                    'count': 777
                }
            })
        }),
    ))

    assert_that(rows, is_not(has_items(
        has_entries({
            'business_id': 1,
            'shop_id': 1,
            'warehouse_id': 146,
            'shop_sku': '8',
        }),
        has_entries({
            'business_id': 1,
            'shop_id': 1,
            'warehouse_id': 146,
            'shop_sku': '5',
        }),
        has_entries({
            'business_id': 1,
            'shop_id': 1,
            'warehouse_id': 145,
            'shop_sku': '4',
        }),
    )))
