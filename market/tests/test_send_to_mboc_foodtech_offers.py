# coding: utf-8

from hamcrest import assert_that, has_items
import pytest

from market.idx.datacamp.yatf.utils import dict2tskv

from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock

from market.idx.pylibrary.datacamp.utils import wait_until
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.offer.OfferMeta_pb2 import MarketColor
from market.idx.pylibrary.datacamp.conversion import offer_to_basic_row, offer_to_service_row

from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampBasicOffersTable,
    DataCampPartnersTable,
    DataCampServiceOffersTable,
)
from market.idx.yatf.resources.lbk_topic import LbkTopic

from market.pylibrary.proto_utils import message_from_data


def make_united_offer(offer_id, business_id, color):
    offer = {
        'basic': {
            'identifiers': {
                'business_id': business_id,
                'offer_id': offer_id,
            },
            'meta': {'scope': DTC.BASIC},
        },
        'service': {
            SHOP_ID_1: {
                'identifiers': {
                    'shop_id': SHOP_ID_1,
                    'business_id': business_id,
                    'offer_id': offer_id,
                    'warehouse_id': 1,
                },
                'meta': {
                    'rgb': color,
                    'scope': DTC.SERVICE,
                }
            },
            SHOP_ID_2: {
                'identifiers': {
                    'shop_id': SHOP_ID_2,
                    'business_id': business_id,
                    'offer_id': offer_id,
                    'warehouse_id': 1,
                },
                'meta': {
                    'rgb': color,
                    'scope': DTC.SERVICE,
                }
            },
        }
    }

    return offer


BUSINESS_ID_LAVKA = 1
BUSINESS_ID_EDA = 2
SHOP_ID_1 = 11
SHOP_ID_2 = 12

UPDATED_UNITED_OFFERS = [
    make_united_offer('1', BUSINESS_ID_LAVKA, DTC.LAVKA),
    make_united_offer('1', BUSINESS_ID_EDA, DTC.EDA),
]

BASIC_OFFERS = [
    DTC.Offer(
        identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID_LAVKA, offer_id='1'),
    ),
]

SERVICE_OFFERS = [
    DTC.Offer(
        identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID_LAVKA, offer_id='1', shop_id=SHOP_ID_1),
        meta=DTC.OfferMeta(rgb=MarketColor.LAVKA),
    ),
    DTC.Offer(
        identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID_LAVKA, offer_id='1', shop_id=SHOP_ID_2),
        meta=DTC.OfferMeta(rgb=MarketColor.LAVKA),
    ),
]

PARTNERS_DATA = [
    {
        'shop_id': SHOP_ID_1,
        'mbi': dict2tskv({
            'shop_id': SHOP_ID_1,
            'business_id': BUSINESS_ID_LAVKA,
            'datafeed_id': SHOP_ID_1,
        }),
        'status': 'publish'
    },
    {
        'shop_id': SHOP_ID_2,
        'mbi': dict2tskv({
            'shop_id': SHOP_ID_2,
            'business_id': BUSINESS_ID_LAVKA,
            'datafeed_id': SHOP_ID_2,
        }),
        'status': 'publish'
    },
]


@pytest.fixture(scope='module')
def basic_offers_table(yt_server, config):
    return DataCampBasicOffersTable(
        yt_server, config.yt_basic_offers_tablepath, data=[offer_to_basic_row(offer) for offer in BASIC_OFFERS]
    )


@pytest.fixture(scope='module')
def service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(
        yt_server,
        config.yt_service_offers_tablepath,
        data=[offer_to_service_row(offer) for offer in SERVICE_OFFERS],
    )


@pytest.fixture(scope='module')
def datacamp_messages_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def mboc_foodtech_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def partners_table(yt_server, config):
    return DataCampPartnersTable(
        yt_stuff=yt_server,
        path=config.yt_partners_tablepath,
        data=PARTNERS_DATA
    )


@pytest.fixture(scope='module')
def subscription_service_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff, 'subscription-service')
    return topic


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, datacamp_messages_topic, mboc_foodtech_topic, subscription_service_topic):
    cfg = {
        'general': {
            'color': 'blue',
            'batch_size': 10,
            'enable_subscription_dispatcher': True,
        },
        'logbroker': {
            'datacamp_messages_topic': datacamp_messages_topic.topic,
            'mboc_foodtech_topic': mboc_foodtech_topic.topic,
            'subscription_service_topic': subscription_service_topic.topic,
        },
        'mboc_sender': {
            'send_consistent_offers_only': False,
            'accepted_colors': 'LAVKA',
        },
    }
    return PiperConfigMock(yt_server=yt_server, log_broker_stuff=log_broker_stuff, config=cfg)


@pytest.yield_fixture(scope='module')
def piper(
    yt_server,
    log_broker_stuff,
    mboc_foodtech_topic,
    datacamp_messages_topic,
    basic_offers_table,
    service_offers_table,
    partners_table,
    config,
    subscription_service_topic
):
    resources = {
        'config': config,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'mboc_foodtech_topic': mboc_foodtech_topic,
        'datacamp_messages_topic': datacamp_messages_topic,
        'partners_table': partners_table,
        'subscription_service_topic': subscription_service_topic,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


def test_mboc_sender_foodtech_offers(piper, datacamp_messages_topic, mboc_foodtech_topic):
    """ Проверяет, что офферы фудтеха отправляются в MBOC по особому протоколу:
    """
    datacamp_messages = [
        message_from_data({'united_offers': [{'offer': [united_offer]}]}, DatacampMessage())
        for united_offer in UPDATED_UNITED_OFFERS
    ]

    for message in datacamp_messages:
        datacamp_messages_topic.write(message.SerializeToString())

    wait_until(lambda: piper.united_offers_processed == 2, timeout=60)

    data = mboc_foodtech_topic.read(count=2, wait_timeout=10)
    offers_with_empty_service = [
        {
            'united_offers': [
                {
                    'offer': [
                        {
                            'basic': {
                                'identifiers': {
                                    'business_id': 1,
                                    'offer_id': '1',
                                },
                            },
                            'service': []
                        }
                    ]
                }
            ]
        }
    ]

    expected_united_offers = [
        {
            'united_offers': [
                {
                    'offer': [
                        {
                            'basic': {
                                'identifiers': {
                                    'business_id': business_id,
                                    'offer_id': offer_id,
                                },
                            },
                        }
                    ]
                }
            ]
        } for business_id, offer_id in [[BUSINESS_ID_EDA, '1'],
                                        [BUSINESS_ID_LAVKA, '1']]
    ]

    assert_that(
        data,
        not(has_items(
            IsSerializedProtobuf(
                DatacampMessage,
                offers_with_empty_service[0]
            ),
        ))
    )

    assert_that(
        data,
        has_items(
            IsSerializedProtobuf(
                DatacampMessage,
                expected_united_offers[0]
            ),
            IsSerializedProtobuf(
                DatacampMessage,
                expected_united_offers[1]
            )
        )
    )

    # Проверяем, что топик опустел
    assert_that(mboc_foodtech_topic, HasNoUnreadData())
