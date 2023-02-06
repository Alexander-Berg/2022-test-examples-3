# coding: utf-8

from hamcrest import assert_that, equal_to
import pytest

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.offer.UnitedOffer_pb2 import UnitedOffer, UnitedOffersBatch
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.pylibrary.datacamp.conversion import offer_to_service_row, offer_to_basic_row
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock

from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampBasicOffersTable,
    DataCampServiceOffersTable,
    DataCampPartnersTable,
)

from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf

from market.idx.datacamp.yatf.utils import create_meta, create_update_meta, dict2tskv
from market.idx.pylibrary.datacamp.utils import wait_until

BUSINESS_ID = 1
SHOP_ID = 1
SHOP_ID_DIRECTSHIPPING = 200
WAREHOUSE_ID = 145
NON_FF_WAREHOUSE_ID = 123
MSKU_ID = 1

DATACAMP_TABLE_DATA = [
    {
        "business_id": BUSINESS_ID,
        "offer_id": "ssku.empty.stock",
        "shop_id": SHOP_ID,
        "warehouse_id": WAREHOUSE_ID,
        "market_sku_id": MSKU_ID,
    }, {
        "business_id": BUSINESS_ID,
        "offer_id": "ssku.updated.stock",
        "shop_id": SHOP_ID,
        "warehouse_id": WAREHOUSE_ID,
        "market_sku_id": MSKU_ID,
        "stock_info": DTC.OfferStockInfo(
            partner_stocks=DTC.OfferStocks(
                meta=create_update_meta(10),
                count=200,
            )
        ),
    }, {
        "business_id": BUSINESS_ID,
        "offer_id": "ssku.updated.stock.but.zero.warehouse_id",
        "shop_id": SHOP_ID,
        "warehouse_id": 0,
        "market_sku_id": MSKU_ID,
        "stock_info": DTC.OfferStockInfo(
            partner_stocks=DTC.OfferStocks(
                meta=create_update_meta(10),
                count=200,
            )
        ),

    }, {
        "business_id": BUSINESS_ID,
        "offer_id": "ssku.updated.stock.nonff.whid",
        "shop_id": SHOP_ID,
        "warehouse_id": NON_FF_WAREHOUSE_ID,
        "market_sku_id": MSKU_ID,
        "stock_info": DTC.OfferStockInfo(
            partner_stocks=DTC.OfferStocks(
                meta=create_update_meta(10),
                count=200,
            )
        ),
    }, {
        "business_id": BUSINESS_ID,
        "offer_id": "ssku.updated.stock.but.no.msku",
        "shop_id": SHOP_ID,
        "warehouse_id": 0,
        "market_sku_id": 0,
        "stock_info": DTC.OfferStockInfo(
            partner_stocks=DTC.OfferStocks(
                meta=create_update_meta(10),
                count=200,
            )
        ),
    }, {
        "business_id": BUSINESS_ID,
        "offer_id": "ssku.updated.stock.set.msku",
        "shop_id": SHOP_ID,
        "warehouse_id": WAREHOUSE_ID,
        "market_sku_id": 0,
        "stock_info": DTC.OfferStockInfo(
            partner_stocks=DTC.OfferStocks(
                meta=create_update_meta(10),
                count=200,
            )
        ),
    }, {
        "business_id": BUSINESS_ID,
        "offer_id": "ssku.archived.offer",
        "shop_id": SHOP_ID,
        "warehouse_id": WAREHOUSE_ID,
        "market_sku_id": MSKU_ID,
        "stock_info": DTC.OfferStockInfo(
            partner_stocks=DTC.OfferStocks(
                meta=create_update_meta(10),
                count=200,
            )
        ),
        "supply_plan": DTC.SupplyPlan.ARCHIVE
    }, {
        "business_id": BUSINESS_ID,
        "offer_id": "stock.updated.zero.whid",
        "shop_id": SHOP_ID_DIRECTSHIPPING,
        "warehouse_id": 0,
        "market_sku_id": 0,
    }
]

SHOPS = [
    {
        'shop_id': SHOP_ID,
        # no mbi info for FF_WAREHOUSE_ID
        'mbi': '\n\n'.join([
            dict2tskv({
                'shop_id': SHOP_ID,
                'warehouse_id': WAREHOUSE_ID,
                'ff_program': 'REAL',
                'blue_status': 'REAL',
            }),
        ]),
        'status': 'publish'
    },
    {
        'shop_id': SHOP_ID_DIRECTSHIPPING,
        'mbi': '\n\n'.join([
            dict2tskv({
                'shop_id': SHOP_ID_DIRECTSHIPPING,
                'warehouse_id': NON_FF_WAREHOUSE_ID,
                'ff_program': 'NO',
                'blue_status': 'REAL',
                'direct_shipping': True
            }),
        ]),
        'status': 'publish'
    },
]


def make_qoffers_data():
    message = DatacampMessage()

    for (offer_id, warehouse_id, count) in [
        ('ssku.empty.stock', WAREHOUSE_ID, 200),
        ('ssku.updated.stock', WAREHOUSE_ID, 300),
        ('ssku.new.offer', WAREHOUSE_ID, 100),
        ('ssku.updated.stock.but.zero.warehouse_id', 0, 300),
        ('ssku.updated.stock.nonff.whid', NON_FF_WAREHOUSE_ID, 300),
        ('ssku.new.offer.but.no.msku', WAREHOUSE_ID, 300),
        ('ssku.new.offer.set.msku', WAREHOUSE_ID, 200),
        ('ssku.archived.offer', WAREHOUSE_ID, 300),
        ('stock.updated.zero.whid', 0, 300),
    ]:
        shop_id = SHOP_ID_DIRECTSHIPPING if offer_id in ('stock.updated.zero.whid') else SHOP_ID
        # пока что поставщики не заполняют актуальную часть через отдельное поле
        service_offer = DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                shop_id=shop_id,
                offer_id=offer_id,
                warehouse_id=warehouse_id,
            ),
            meta=DTC.OfferMeta(
                scope=DTC.SERVICE,
                rgb=DTC.BLUE,
            ),
            stock_info=DTC.OfferStockInfo(
                partner_stocks=DTC.OfferStocks(
                    meta=create_update_meta(100, source=DTC.PUSH_PARTNER_FEED),
                    count=count,
                )
            ),
            partner_info=DTC.PartnerInfo(
                meta=create_update_meta(200, source=DTC.PUSH_PARTNER_FEED),
                is_dsbs=True,
            ) if offer_id in ('stock.updated.zero.whid') else None
        )

        united_offer = UnitedOffer(
            basic=DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    offer_id=service_offer.identifiers.offer_id,
                    business_id=service_offer.identifiers.business_id
                ),
                content=DTC.OfferContent(
                    binding=DTC.ContentBinding(
                        approved=DTC.Mapping(
                            market_sku_id=MSKU_ID
                        )
                    )
                ) if offer_id in ('ssku.new.offer', 'ssku.new.offer.set.msku') else None
            ),
            service={shop_id: service_offer}
        )
        message.united_offers.extend([UnitedOffersBatch(offer=[united_offer])])

    return message


@pytest.fixture()
def basic_offers_table(yt_server, config):
    return DataCampBasicOffersTable(
        yt_server,
        config.yt_basic_offers_tablepath,
        data=[
            offer_to_basic_row(DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=offer['business_id'],
                    offer_id=offer['offer_id'],
                ),
                meta=create_meta(10, scope=DTC.BASIC),
                content=DTC.OfferContent(
                    binding=DTC.ContentBinding(
                        approved=DTC.Mapping(
                            market_sku_id=offer['market_sku_id']
                        )
                    )
                )
            )) for offer in DATACAMP_TABLE_DATA
        ]
    )


@pytest.fixture()
def service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(
        yt_server,
        config.yt_service_offers_tablepath,
        data=[
            offer_to_service_row(DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=offer['business_id'],
                    offer_id=offer['offer_id'],
                    shop_id=offer['shop_id'],
                ),
                meta=create_meta(10, color=DTC.BLUE, scope=DTC.SERVICE),
                stock_info=offer.get('stock_info'),
                content=DTC.OfferContent(
                    partner=DTC.PartnerContent(
                        original_terms=DTC.OriginalTerms(
                            supply_plan=DTC.SupplyPlan(
                                value=offer.get('supply_plan', DTC.SupplyPlan.WILL_SUPPLY)
                            )
                        )
                    )
                )
            )) for offer in DATACAMP_TABLE_DATA
        ]
    )


@pytest.fixture()
def actual_service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(
        yt_server,
        config.yt_actual_service_offers_tablepath,
        data=[
            offer_to_service_row(DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=offer['business_id'],
                    offer_id=offer['offer_id'],
                    shop_id=offer['shop_id'],
                    warehouse_id=offer['warehouse_id'],
                ),
                meta=create_meta(10, color=DTC.BLUE, scope=DTC.SERVICE),
                partner_info=DTC.PartnerInfo(
                    is_blue_offer=True,
                    meta=create_update_meta(100, source=DTC.PUSH_PARTNER_FEED)
                ),
                stock_info=offer.get('stock_info')
            )) for offer in DATACAMP_TABLE_DATA if (offer['warehouse_id'] != 0 or offer['offer_id'] == 'stock.updated.zero.whid')
        ]
    )


@pytest.fixture()
def partners_table(yt_server, config):
    return DataCampPartnersTable(yt_server,
                                 config.yt_partners_tablepath,
                                 data=SHOPS)


@pytest.fixture(scope='session')
def qoffers_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='session')
def partner_stock_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture()
def config(yt_server, log_broker_stuff, qoffers_topic, partner_stock_topic):
    cfg = {
        'general': {
            'batch_size': 10,
        },
        'logbroker': {
            'qoffers_topic': qoffers_topic.topic,
            'partner_stock_topic': partner_stock_topic.topic,
        },
    }
    return PiperConfigMock(
        yt_server=yt_server,
        log_broker_stuff=log_broker_stuff,
        config=cfg
    )


@pytest.yield_fixture()
def piper(
    yt_server,
    log_broker_stuff,
    config,
    qoffers_topic,
    partner_stock_topic,
    basic_offers_table,
    service_offers_table,
    actual_service_offers_table,
    partners_table
):
    resources = {
        'config': config,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'partner_stock_topic': partner_stock_topic,
        'qoffers_topic': qoffers_topic,
        'partners_table': partners_table,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


def test_qoffers_partner_stock(piper, qoffers_topic, partner_stock_topic):
    """
    Проверяем, что piper читает топик от qparser, корректно записывает их в таблицу и отправляет изменения стоков в MBI
    """
    qoffers_topic.write(make_qoffers_data().SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= 7, timeout=60)

    data = partner_stock_topic.read(count=1)
    message = DatacampMessage()
    message.ParseFromString(data[0])
    assert_that(len(message.offers[0].offer), equal_to(5))
    assert_that(data[0], IsSerializedProtobuf(DatacampMessage, {
        'offers': [{
            'offer': [{
                'identifiers': {
                    'shop_id': SHOP_ID,
                    'offer_id': 'ssku.empty.stock',
                    'warehouse_id': WAREHOUSE_ID,
                },
                'price': None,
                'stock_info': {
                    'partner_stocks': {
                        'count': 200,
                        'meta': {
                            'timestamp': {
                                'seconds': 100,
                            }
                        },
                    },
                },
            }, {
                'identifiers': {
                    'shop_id': SHOP_ID,
                    'offer_id': 'ssku.updated.stock',
                    'warehouse_id': WAREHOUSE_ID,
                },
                'price': None,
                'stock_info': {
                    'partner_stocks': {
                        'count': 300,
                        'meta': {
                            'timestamp': {
                                'seconds': 100,
                            }
                        },
                    },
                },
            }, {
                'identifiers': {
                    'shop_id': SHOP_ID,
                    'offer_id': 'ssku.new.offer',
                    'warehouse_id': WAREHOUSE_ID,
                },
                'price': None,
                'stock_info': {
                    'partner_stocks': {
                        'count': 100,
                        'meta': {
                            'timestamp': {
                                'seconds': 100,
                            }
                        },
                    },
                },
            }, {
                'identifiers': {
                    'shop_id': SHOP_ID,
                    'offer_id': 'ssku.updated.stock.nonff.whid',
                    'warehouse_id': NON_FF_WAREHOUSE_ID,
                },
                'price': None,
                'stock_info': {
                    'partner_stocks': {
                        'count': 300,
                        'meta': {
                            'timestamp': {
                                'seconds': 100,
                            }
                        },
                    },
                },
            }, {
                'identifiers': {
                    'shop_id': SHOP_ID,
                    'offer_id': 'ssku.new.offer.set.msku',
                    'warehouse_id': WAREHOUSE_ID,
                },
                'price': None,
                'stock_info': {
                    'partner_stocks': {
                        'count': 200,
                        'meta': {
                            'timestamp': {
                                'seconds': 100,
                            }
                        },
                    },
                },
            }]
        }]
    }))

    # Проверяем, что в топике больше нет данных
    assert_that(partner_stock_topic, HasNoUnreadData())
