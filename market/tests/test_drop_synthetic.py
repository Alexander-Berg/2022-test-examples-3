# coding: utf-8

import pytest
from hamcrest import assert_that

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
import market.idx.datacamp.proto.subscription.subscription_pb2 as SUBS

from market.idx.datacamp.dispatcher.yatf.resources.config import DispatcherConfig
from market.idx.datacamp.dispatcher.yatf.test_env import DispatcherTestEnv
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.datacamp.yatf.utils import create_meta
from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.pylibrary.proto_utils import message_from_data, proto_path_from_str_path


BUSINESS_ID = 1
SHOP_ID = 2
NORMAL_OFFER_ID = 'NormalOffer'
SYNTHETIC_OFFER_ID = 'SyntheticOffer'


@pytest.fixture(scope='module')
def basic_offers_table_data():
    return [
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id=NORMAL_OFFER_ID,
            ),
            content=DTC.OfferContent(
                market=DTC.MarketContent(vendor_id=11),
            ),
        ),
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id=SYNTHETIC_OFFER_ID,
            ),
            content=DTC.OfferContent(
                market=DTC.MarketContent(vendor_id=11),
            ),
            meta=DTC.OfferMeta(
                synthetic=True
            ),
        ),
    ]


@pytest.fixture(scope='module')
def service_offers_table_data():
    return [
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id=NORMAL_OFFER_ID,
                shop_id=SHOP_ID,
            ),
            meta=create_meta(10),
        ),
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id=SYNTHETIC_OFFER_ID,
                shop_id=SHOP_ID,
            ),
            meta=create_meta(10, synthetic=True),
        ),
    ]


# проверяем срабатывание подписки - по полю content.market.vendor_id
SUBSCRIPTION_MESSAGES = [
    message_from_data(
        {
            'subscription_request': [
                {
                    'business_id': BUSINESS_ID,
                    'offer_id': NORMAL_OFFER_ID,
                    'basic': {
                        'updated_fields': [proto_path_from_str_path(DTC.Offer, 'content.market.vendor_id')],
                    },
                }
            ]
        },
        SUBS.UnitedOffersSubscriptionMessage(),
    ),
    message_from_data(
        {
            'subscription_request': [
                {
                    'business_id': BUSINESS_ID,
                    'offer_id': SYNTHETIC_OFFER_ID,
                    'basic': {
                        'updated_fields': [proto_path_from_str_path(DTC.Offer, 'content.market.vendor_id')],
                    },
                }
            ]
        },
        SUBS.UnitedOffersSubscriptionMessage(),
    ),
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
    actual_service_offers_table,
):
    cfg = DispatcherConfig()
    cfg.create_initializer(yt_server=yt_server, yt_token_path=yt_token.path)

    lb_reader = cfg.create_lb_reader(log_broker_stuff, input_topic)
    unpacker = cfg.create_subscription_message_unpacker()
    dispatcher = cfg.create_subscription_dispatcher(
        basic_offers_table.table_path, service_offers_table.table_path, actual_service_offers_table.table_path,
        drop_synthetic_offers=True
    )
    mboc_sender = cfg.create_mboc_sender('MBOC_LB_SENDER')
    lb_writer = cfg.create_lb_writer(log_broker_stuff, output_topic)

    cfg.create_link(lb_reader, unpacker)
    cfg.create_link(unpacker, dispatcher)
    cfg.create_link(dispatcher, mboc_sender)
    cfg.create_link(mboc_sender, lb_writer)

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


def test_drop_synthetic_offers(dispatcher, input_topic, output_topic):
    for message in SUBSCRIPTION_MESSAGES:
        input_topic.write(message.SerializeToString())

    wait_until(lambda: dispatcher.subscription_dispatcher_processed >= len(SUBSCRIPTION_MESSAGES), timeout=10)

    sent_data = output_topic.read(count=1)
    assert_that(
        sent_data,
        HasSerializedDatacampMessages(
            [
                {
                    'united_offers': [
                        {
                            'offer': [
                                {
                                    'basic': {
                                        'identifiers': {
                                            'business_id': BUSINESS_ID,
                                            'offer_id': NORMAL_OFFER_ID,
                                        },
                                    },
                                },
                            ]
                        }
                    ]
                },
            ]
        ),
    )

    assert_that(output_topic, HasNoUnreadData(wait_timeout=10))
