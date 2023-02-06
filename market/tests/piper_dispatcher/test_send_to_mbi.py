# coding: utf-8

import pytest
from datetime import datetime, timedelta
from hamcrest import assert_that, has_items

from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.dispatcher.yatf.test_env import DispatcherTestEnv
from market.idx.datacamp.dispatcher.yatf.resources.config import DispatcherConfig
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.datacamp.yatf.utils import dict2tskv
from market.idx.pylibrary.datacamp.conversion import offer_to_service_row, offer_to_basic_row

from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampPartnersTable,
    DataCampServiceOffersTable,
    DataCampBasicOffersTable,
)
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.utils.utils import create_timestamp_from_json

from market.pylibrary.proto_utils import message_from_data
from market.mbi.mbi.proto.bidding.MbiBids_pb2 import Parcel, Bid

NOW_UTC = datetime.utcnow()  # JSON serializer should always use UTC
FUTURE_UTC = NOW_UTC + timedelta(minutes=45)
time_pattern = "%Y-%m-%dT%H:%M:%SZ"

future_time = FUTURE_UTC.strftime(time_pattern)
future_ts = create_timestamp_from_json(future_time)

future_time_2 = (FUTURE_UTC + timedelta(minutes=45)).strftime(time_pattern)
future_ts_2 = create_timestamp_from_json(future_time_2)

PARTNERS = [
    {
        'shop_id': 1,
        'status': 'publish',
        'mbi': dict2tskv(
            {
                'shop_id': 1,
                'business_id': 1,
                'datafeed_id': 4321,
                'warehouse_id': 145,
                'is_push_partner': True,
            }
        ),
    },
    {
        'shop_id': 1,
        'status': 'publish',
        'mbi': dict2tskv(
            {
                'shop_id': 1,
                'business_id': 1,
                'datafeed_id': 1234,
                'warehouse_id': 111,
                'is_push_partner': True,
            }
        ),
    },
]

BASIC_TABLE_DATA = [
    offer_to_basic_row(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='TestQuickBidsUpdate'),
        )
    ),
]

SERVICE_TABLE_DATA = [
    offer_to_service_row(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='TestQuickBidsUpdate', shop_id=1),
        )
    )
]

ACTUAL_SERVICE_TABLE_DATA = [
    offer_to_service_row(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=1, feed_id=1234, offer_id='TestQuickBidsUpdate', shop_id=1)
        )
    )
]

EXPECTED_PARCELS = [
    {
        'bids': [
            {
                'domain_type': Bid.DomainType.FEED_OFFER_ID,
                'partner_type': Bid.PartnerType.SHOP,
                'feed_id': 1234,
                'target': Bid.BidTarget.OFFER,
                'value_for_card': {
                    'modification_time': int(future_ts_2.seconds),
                    'publication_status': Bid.PublicationStatus.APPLIED,
                    'value': 15,
                },
                'partner_id': 1,
                'domain_id': 'TestQuickBidsUpdate',
                'domain_ids': ['1234', 'TestQuickBidsUpdate'],
            }
        ]
    },
    {
        'bids': [
            {
                'target': Bid.BidTarget.OFFER,
                'domain_type': Bid.DomainType.FEED_OFFER_ID,
                'partner_type': Bid.PartnerType.SHOP,
                'feed_id': 1234,
                'value_for_card': {
                    'modification_time': int(future_ts.seconds),
                    'publication_status': Bid.PublicationStatus.APPLIED,
                    'value': 10,
                },
                'partner_id': 1,
                'domain_id': 'TestQuickBidsUpdate',
                'domain_ids': ['1234', 'TestQuickBidsUpdate'],
            }
        ]
    },
]


@pytest.fixture(scope='module', params=[True, False])
def send_to_dispatcher_via_qyt(request):
    return request.param


@pytest.fixture(scope='module')
def basic_offers_table(yt_server, piper_config):
    return DataCampBasicOffersTable(yt_server, piper_config.yt_basic_offers_tablepath, data=BASIC_TABLE_DATA)


@pytest.fixture(scope='module')
def service_offers_table(yt_server, piper_config):
    return DataCampServiceOffersTable(yt_server, piper_config.yt_service_offers_tablepath, data=SERVICE_TABLE_DATA)


@pytest.fixture(scope='module')
def actual_service_offers_table(yt_server, piper_config):
    return DataCampServiceOffersTable(
        yt_server, piper_config.yt_actual_service_offers_tablepath, data=ACTUAL_SERVICE_TABLE_DATA
    )


@pytest.fixture(scope='module')
def partners_table(yt_server, piper_config):
    return DataCampPartnersTable(yt_server, piper_config.yt_partners_tablepath, PARTNERS)


@pytest.fixture(scope='module')
def offer_bids_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff, 'offer_bids')
    return topic


@pytest.fixture(scope='module')
def mbi_offer_bids_reply_topic(log_broker_stuff):
    return LbkTopic(log_broker_stuff, 'mbi_offer_bids_reply')


@pytest.fixture(scope='module')
def subscription_service_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff, 'subscription-service')
    return topic


@pytest.fixture(scope='module')
def piper_config(yt_server, log_broker_stuff, offer_bids_topic, subscription_service_topic, send_to_dispatcher_via_qyt, qyt_subscription):
    cfg = {
        'logbroker': {
            'offer_bids_topic': offer_bids_topic.topic,
            'subscription_service_topic': subscription_service_topic.topic,
        },
        'general': {
            'color': 'blue',
            'send_to_dispatcher_via_qyt': send_to_dispatcher_via_qyt
        },
        'yt': {
            'dispatcher_queue_path': qyt_subscription.path if send_to_dispatcher_via_qyt else None,
        },
    }
    return PiperConfigMock(yt_server=yt_server, log_broker_stuff=log_broker_stuff, config=cfg)


@pytest.yield_fixture(scope='module')
def piper(
    yt_server,
    log_broker_stuff,
    piper_config,
    offer_bids_topic,
    partners_table,
    actual_service_offers_table,
    basic_offers_table,
    service_offers_table,
    subscription_service_topic,
):
    resources = {
        'config': piper_config,
        'offer_bids_topic': offer_bids_topic,
        'partners_table': partners_table,
        'actual_service_offers_table': actual_service_offers_table,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'subscription_service_topic': subscription_service_topic,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


@pytest.fixture(scope='module')
def dispatcher_config(
    yt_token,
    yt_server,
    log_broker_stuff,
    piper_config,
    send_to_dispatcher_via_qyt,
    mbi_offer_bids_reply_topic,
    subscription_service_topic,
    qyt_subscription,
    port_manager,
):
    cfg = DispatcherConfig()
    cfg.create_initializer(yt_server=yt_server, yt_token_path=yt_token.path)

    reader = cfg.create_dispatcher_reader(yt_server, qyt_subscription, port_manager) if send_to_dispatcher_via_qyt else cfg.create_lb_reader(log_broker_stuff, subscription_service_topic)
    unpacker = cfg.create_subscription_message_unpacker()
    dispatcher = cfg.create_subscription_dispatcher(
        piper_config.yt_basic_offers_tablepath, piper_config.yt_service_offers_tablepath, piper_config.yt_actual_service_offers_tablepath
    )
    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, dispatcher)

    filter = cfg.create_subscription_filter('MBI_OFFER_BIDS_REPLY_SUBSCRIBER', extra_params={'Mode': 'MIXED'})
    sender = cfg.create_mbi_bids_reply_sender()
    writer = cfg.create_lb_writer(log_broker_stuff, mbi_offer_bids_reply_topic)

    cfg.create_link(dispatcher, filter)
    cfg.create_link(filter, sender)
    cfg.create_link(sender, writer)

    return cfg


@pytest.yield_fixture(scope='module')
def dispatcher(
    dispatcher_config,
    basic_offers_table,
    service_offers_table,
    actual_service_offers_table,
    subscription_service_topic,
    send_to_dispatcher_via_qyt,
    mbi_offer_bids_reply_topic,
    qyt_subscription
):
    resources = {
        'dispatcher_config': dispatcher_config,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'subscription_service_topic': subscription_service_topic,
        'mbi_offer_bids_reply_topic': mbi_offer_bids_reply_topic,
    }
    if send_to_dispatcher_via_qyt:
        resources['qyt'] = qyt_subscription

    with DispatcherTestEnv(**resources) as env:
        env.verify()
        yield env


def test_send_to_mbi(piper, offer_bids_topic, dispatcher, mbi_offer_bids_reply_topic):
    shop_id = 1
    feed_id = 1234
    offer_id = 'TestQuickBidsUpdate'
    bid_data = {
        'identifiers': {
            'shop_id': shop_id,
            'feed_id': feed_id,
            'offer_id': offer_id,
        },
        'bids': {'bid': {'meta': {'timestamp': future_time}, 'value': 10}},
        'meta': {
            'rgb': DTC.BLUE,
            'modification_ts': future_ts.seconds,
        },
    }

    offer_bids_topic.write(message_from_data({'offer': [bid_data]}, DTC.OffersBatch()).SerializeToString())
    wait_until(lambda: dispatcher.subscription_dispatcher_processed >= 1, timeout=120)

    bid_data['bids']['bid']['value'] = 15
    bid_data['bids']['bid']['meta']['timestamp'] = future_time_2

    offer_bids_topic.write(message_from_data({'offer': [bid_data]}, DTC.OffersBatch()).SerializeToString())
    wait_until(lambda: dispatcher.subscription_dispatcher_processed >= 2, timeout=120)

    data = mbi_offer_bids_reply_topic.read(2)
    assert_that(data, has_items(*[IsSerializedProtobuf(Parcel, _) for _ in EXPECTED_PARCELS]))
