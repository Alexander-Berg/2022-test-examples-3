# coding: utf-8

import pytest
from hamcrest import assert_that, has_items, is_not

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
import market.idx.datacamp.proto.subscription.subscription_pb2 as SUBS

from market.idx.datacamp.dispatcher.yatf.resources.config import DispatcherConfig
from market.idx.datacamp.dispatcher.yatf.test_env import DispatcherTestEnv
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.datacamp.yatf.matchers.matchers import IsSerializedProtobuf
from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.pylibrary.proto_utils import message_from_data, proto_path_from_str_path


BUSINESS_ID = 1
SHOP_ID = 2
WAREHOUSE_ID = 3


@pytest.fixture(scope='module')
def basic_offers_table_data():
    return [message_from_data({
        'identifiers': {
            'business_id': BUSINESS_ID,
            'offer_id': offer_id,
        },
        'meta': {
            'scope': DTC.BASIC,
        },
        'content': {
            'binding': {
                'approved': {
                    'market_sku_id': 1
                }
            }
        }
    }, DTC.Offer()) for offer_id in['offer.with.push.api.stocks.update', 'offer.with.feed.stocks.update']]


@pytest.fixture(scope='module')
def service_offers_table_data():
    return [message_from_data({
        'identifiers': {
            'business_id': BUSINESS_ID,
            'offer_id': offer_id,
            'shop_id': SHOP_ID,
        },
        'meta': {
            'scope': DTC.SERVICE,
            'rgb': DTC.BLUE
        }
    }, DTC.Offer()) for offer_id in['offer.with.push.api.stocks.update', 'offer.with.feed.stocks.update']]


@pytest.fixture(scope='module')
def actual_service_offers_table_data():
    return [message_from_data({
        'identifiers': {
            'business_id': BUSINESS_ID,
            'offer_id': offer_id,
            'shop_id': SHOP_ID,
            'warehouse_id': WAREHOUSE_ID,
        },
        'meta': {
            'scope': DTC.SERVICE,
            'rgb': DTC.BLUE
        },
        'stock_info': {
            'partner_stocks': {
                'count': 1,
                'meta': {
                    'source': source
                }
            }
        }
    }, DTC.Offer()) for offer_id, source in [
        ('offer.with.push.api.stocks.update', DTC.MARKET_API_STOCK),
        ('offer.with.feed.stocks.update', DTC.PUSH_PARTNER_FEED)
    ]]


SUBSCRIPTION_MESSAGES = [
    message_from_data({
        'subscription_request': [
            {
                'business_id': BUSINESS_ID,
                'offer_id': 'offer.with.push.api.stocks.update',
                'service': {
                    SHOP_ID: {
                        'warehouse': {
                            WAREHOUSE_ID: {
                                'updated_fields': [
                                    proto_path_from_str_path(DTC.Offer, 'stock_info.partner_stocks'),
                                ],
                            }
                        }
                    }
                },
            }
        ]
    }, SUBS.UnitedOffersSubscriptionMessage()),
    message_from_data({
        'subscription_request': [
            {
                'business_id': BUSINESS_ID,
                'offer_id': 'offer.with.feed.stocks.update',
                'service': {
                    SHOP_ID: {
                        'warehouse': {
                            WAREHOUSE_ID: {
                                'updated_fields': [
                                    proto_path_from_str_path(DTC.Offer, 'stock_info.partner_stocks'),
                                ],
                            }
                        }
                    }
                },
            }
        ]
    }, SUBS.UnitedOffersSubscriptionMessage()),
]


@pytest.fixture(scope='module')
def dispatcher_config(
        yt_token,
        yt_server,
        log_broker_stuff,
        input_topic,
        output_topic,
        basic_offers_table,
        service_offers_table,
        actual_service_offers_table
):
    cfg = DispatcherConfig()
    cfg.create_initializer(
        yt_server=yt_server,
        yt_token_path=yt_token.path
    )

    lb_reader = cfg.create_lb_reader(log_broker_stuff, input_topic)
    unpacker = cfg.create_subscription_message_unpacker()
    dispatcher = cfg.create_subscription_dispatcher(
        basic_offers_table.table_path,
        service_offers_table.table_path,
        actual_service_offers_table.table_path
    )
    filter = cfg.create_subscription_filter('SUBSCRIPTION_FILTER', extra_params={
        'Threads': 1,
        'Subscriber': 'PARTNER_STOCK_SUBSCRIBER',
        'UseActualServiceFields': True,
        'PartnerStockFilterEnabled': True,
    })
    sender = cfg.create_subscription_sender(extra_params={
        'ExportFormat': 'FLAT'
    })
    lb_writer = cfg.create_lb_writer(log_broker_stuff, output_topic)

    cfg.create_link(lb_reader, unpacker)
    cfg.create_link(unpacker, dispatcher)
    cfg.create_link(dispatcher, filter)
    cfg.create_link(filter, sender)
    cfg.create_link(sender, lb_writer)

    return cfg


@pytest.yield_fixture(scope='module')
def dispatcher(
        dispatcher_config,
        basic_offers_table,
        service_offers_table,
        actual_service_offers_table,
        input_topic,
        output_topic,
):
    resources = {
        'dispatcher_config': dispatcher_config,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'input_topic': input_topic,
        'output_topic': output_topic,
    }
    with DispatcherTestEnv(**resources) as env:
        env.verify()
        yield env


@pytest.yield_fixture(scope='module')
def topic_sender(dispatcher, input_topic):
    for message in SUBSCRIPTION_MESSAGES:
        input_topic.write(message.SerializeToString())

    wait_until(lambda: dispatcher.subscription_dispatcher_processed >= len(SUBSCRIPTION_MESSAGES), timeout=10)


def test_filtering(topic_sender, output_topic):
    """ Не отправляется изменение по стокам от MARKET_API_STOCK, но отправляются от PUSH_PARTNER_FEED """
    sent_data = output_topic.read(count=1)

    assert_that(sent_data, has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'offers': [{
                'offer': [{
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': 'offer.with.feed.stocks.update',
                        'shop_id': SHOP_ID,
                        'warehouse_id': WAREHOUSE_ID,
                    },
                    'stock_info': {
                        'partner_stocks': {
                            'count': 1
                        }
                    }
                }]
            }]
        }),
    ]))

    assert_that(sent_data, is_not(has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'offers': [{
                'offer': [{
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': 'offer.with.push.api.stocks.update',
                        'shop_id': SHOP_ID,
                        'warehouse_id': WAREHOUSE_ID,
                    }
                }]
            }]
        }),
    ])))

    # Проверяем, что топик опустел, сообщение для не-маркетного оффера (по expiry) не отправлено
    assert_that(output_topic, HasNoUnreadData())
