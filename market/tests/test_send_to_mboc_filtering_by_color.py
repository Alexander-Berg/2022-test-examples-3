# coding: utf-8

from hamcrest import assert_that, has_items
import pytest
from datetime import datetime

from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock

from market.idx.pylibrary.datacamp.utils import wait_until
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.offer.OfferMeta_pb2 import (
    DIRECT_SEARCH_SNIPPET_GALLERY,
    UNKNOWN_COLOR,
    BLUE,
    WHITE,
    EDA,
    LAVKA,
)
from market.idx.datacamp.proto.offer.OfferMeta_pb2 import Flag
from market.idx.pylibrary.datacamp.conversion import offer_to_basic_row, offer_to_service_row
from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampBasicOffersTable,
    DataCampServiceOffersTable,
)
from market.idx.yatf.resources.lbk_topic import LbkTopic

from market.pylibrary.proto_utils import message_from_data

from robot.sortdc.protos.user_pb2 import EUser


def make_united_offer(offer_id):
    offer = {
        'basic': {
            'identifiers': {
                'business_id': 1,
                'offer_id': offer_id,
            },
            'meta': {'scope': DTC.BASIC},
            'pictures': {
                'partner': {
                    'original': {
                        'meta': {'timestamp': datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ")},
                        'source': [{'url': 'picurl'}],
                    }
                }
            },
        }
    }

    return offer


UPDATED_UNITED_OFFERS = [make_united_offer(offer_id) for offer_id in list(map(str, range(1, 7)))]

BASIC_OFFERS = [
    DTC.Offer(
        identifiers=DTC.OfferIdentifiers(business_id=1, offer_id=cur_offer_id),
    ) for cur_offer_id in list(map(str, range(1, 7)))
]

SERVICE_OFFERS = [
    # The WHITE offer should not be filtered
    DTC.Offer(
        identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='1', shop_id=1),
        meta=DTC.OfferMeta(rgb=WHITE),
    ),
    # The DIRECT_SEARCH_SNIPPET_GALLERY offer should be filtered
    DTC.Offer(
        identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='2', shop_id=2),
        meta=DTC.OfferMeta(rgb=DIRECT_SEARCH_SNIPPET_GALLERY),
    ),
    # The LAVKA offer should not be filtered
    DTC.Offer(
        identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='3', shop_id=3),
        meta=DTC.OfferMeta(rgb=LAVKA),
    ),
    # The EDA offer should not be filtered
    DTC.Offer(
        identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='4', shop_id=4),
        meta=DTC.OfferMeta(rgb=EDA),
    ),
    # The UNKNOWN_COLOR offer should be filtered
    DTC.Offer(
        identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='5', shop_id=5),
        meta=DTC.OfferMeta(rgb=UNKNOWN_COLOR, vertical_approved_flag=Flag(flag=True)),
    ),
    # The MARKET and vertical_approved == true offer should not be filtered!!
    DTC.Offer(
        identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='6', shop_id=6),
        meta=DTC.OfferMeta(rgb=BLUE, vertical_approved_flag=Flag(flag=True)),
    ),
    DTC.Offer(
        identifiers=DTC.OfferIdentifiers(
            business_id=1,
            offer_id='6',
            shop_id=6
        ),
        meta=DTC.OfferMeta(
            rgb=BLUE,
            vertical_approved_flag=Flag(flag=True),
            sortdc_context=DTC.TSortDCExportContext(
                export_items=[DTC.TSortDCExportContext.TExportItem(
                    user=EUser.CBIR
                )]
            )
        ),
    ),
]


def not_accepted_colors():
    return ("DIRECT;" +
            "DIRECT_SITE_PREVIEW;" +
            "DIRECT_STANDBY;" +
            "DIRECT_GOODS_ADS;" +
            "DIRECT_SEARCH_SNIPPET_GALLERY;" +
            "UNKNOWN_COLOR")


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
def actual_service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(
        yt_server,
        config.yt_actual_service_offers_tablepath,
        data=[offer_to_service_row(offer) for offer in SERVICE_OFFERS],
    )


@pytest.fixture(scope='module')
def datacamp_messages_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def mboc_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def subscription_service_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff, 'subscription-service')
    return topic


@pytest.fixture(scope='module')
def config(
    yt_server,
    log_broker_stuff,
    datacamp_messages_topic,
    mboc_topic,
    subscription_service_topic
):
    cfg = {
        'general': {
            'color': 'blue',
            'batch_size': 10,
            'enable_subscription_dispatcher': True
        },
        'logbroker': {
            'datacamp_messages_topic': datacamp_messages_topic.topic,
            'mboc_topic': mboc_topic.topic,
            'subscription_service_topic': subscription_service_topic.topic,
        },
        'mboc_sender': {
            'send_consistent_offers_only': False,
            'not_accepted_colors': not_accepted_colors(),
        },
    }
    return PiperConfigMock(yt_server=yt_server, log_broker_stuff=log_broker_stuff, config=cfg)


@pytest.yield_fixture(scope='module')
def piper(
    yt_server,
    log_broker_stuff,
    mboc_topic,
    datacamp_messages_topic,
    basic_offers_table,
    service_offers_table,
    actual_service_offers_table,
    config,
    subscription_service_topic
):
    resources = {
        'config': config,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'mboc_topic': mboc_topic,
        'datacamp_messages_topic': datacamp_messages_topic,
        'subscription_service_topic': subscription_service_topic,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


def test_mboc_sender_filtering_by_color(piper, datacamp_messages_topic, mboc_topic):
    datacamp_messages = [
        message_from_data({'united_offers': [{'offer': [united_offer]}]}, DatacampMessage())
        for united_offer in UPDATED_UNITED_OFFERS
    ]
    for message in datacamp_messages:
        datacamp_messages_topic.write(message.SerializeToString())

    wait_until(lambda: piper.united_offers_processed >= 2, timeout=60)

    data = mboc_topic.read(count=4, wait_timeout=30)
    assert_that(
        data,
        has_items(*[
            IsSerializedProtobuf(
                DatacampMessage,
                {
                    'united_offers': [
                        {
                            'offer': [
                                {
                                    'basic': {
                                        'identifiers': {
                                            'business_id': 1,
                                            'offer_id': offer_id,
                                        },
                                        'pictures': {
                                            'partner': {'original': {'source': [{'url': 'picurl'}]}},
                                        },
                                    }
                                }
                            ]
                        }
                    ]
                },
            ) for offer_id in ['1', '3', '4', '6']
        ]),
    )

    # Проверяем, что топик опустел
    assert_that(mboc_topic, HasNoUnreadData())
