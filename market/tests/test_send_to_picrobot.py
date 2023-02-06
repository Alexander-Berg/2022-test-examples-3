# coding: utf-8

from hamcrest import assert_that, has_items
import pytest
import time

from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.offer.UnitedOffer_pb2 import UnitedOffersBatch, UnitedOffer
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.picrobot.proto.event_pb2 import TImageRequest
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.yatf.matchers.matchers import IsSerializedProtobuf
from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampBasicOffersTable, DataCampServiceOffersTable
from market.idx.datacamp.yatf.utils import create_update_meta
from market.idx.pylibrary.datacamp.conversion import offer_to_service_row, offer_to_basic_row


BASIC_TABLE_DATA = [
    offer_to_basic_row(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='T1000'),
            pictures=DTC.OfferPictures(
                partner=DTC.PartnerPictures(original=DTC.SourcePictures(meta=create_update_meta(0), source=[]))
            ),
        )
    ),
    offer_to_basic_row(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='T2000'),
            pictures=DTC.OfferPictures(
                partner=DTC.PartnerPictures(
                    original=DTC.SourcePictures(
                        meta=create_update_meta(0),
                        source=[
                            DTC.SourcePicture(url='2000url1/'),
                            DTC.SourcePicture(url='https://2000url2/'),
                            DTC.SourcePicture(url='2000url3/'),
                            DTC.SourcePicture(url='https://2000url4/'),
                        ],
                    )
                )
            ),
        )
    ),
    offer_to_basic_row(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='T3000'),
            pictures=DTC.OfferPictures(
                partner=DTC.PartnerPictures(
                    original=DTC.SourcePictures(
                        source=[
                            DTC.SourcePicture(url='https://3000url1/'),
                            DTC.SourcePicture(url='3000url3/'),
                            DTC.SourcePicture(url='3000url2/'),
                        ]
                    )
                )
            ),
        )
    ),
]

SERVICE_TABLE_DATA = [
    offer_to_service_row(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=1, offer_id=offer_id, shop_id=1),
        )
    )
    for offer_id in ('T1000', 'T2000', 'T3000')
] + [
    offer_to_service_row(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=1, offer_id=offer_id, shop_id=2),
            meta=DTC.OfferMeta(rgb=DTC.DIRECT_SEARCH_SNIPPET_GALLERY)
        ),
    )
    for offer_id in ('T1000', 'T2000', 'T3000')
]

ACTUAL_SERVICE_TABLE_DATA = [
    offer_to_service_row(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=1, offer_id=offer_id, shop_id=1),
        )
    )
    for offer_id in ('T1000', 'T2000', 'T3000')
] + [
    offer_to_service_row(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=1, offer_id=offer_id, shop_id=2),
            meta=DTC.OfferMeta(rgb=DTC.DIRECT_SEARCH_SNIPPET_GALLERY)
        )
    )
    for offer_id in ('T1000', 'T2000', 'T3000')
]


def create_datacamp_message(offer):
    return DatacampMessage(united_offers=[UnitedOffersBatch(offer=[UnitedOffer(basic=offer, service={
        1: DTC.Offer(identifiers=DTC.OfferIdentifiers(business_id=offer.identifiers.business_id, offer_id=offer.identifiers.offer_id, shop_id=1))
    })])])


UPDATED_OFFER_MESSAGES = [
    # Проверяем, что если картинки изначально отсутствуют, то принимаются все новые
    create_datacamp_message(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='T1000'),
            pictures=DTC.OfferPictures(
                partner=DTC.PartnerPictures(
                    original=DTC.SourcePictures(
                        meta=create_update_meta(int(time.time())),
                        source=[
                            DTC.SourcePicture(url='https://firstpicture/'),
                            DTC.SourcePicture(url='secondpicture/'),
                            DTC.SourcePicture(url='https://thirdpicture/'),
                            DTC.SourcePicture(url='http://httpurl/'),
                        ],
                    )
                )
            ),
        )
    ),
    # Проверяем, что если изначально были какие-то картинки, то из пришедших будут выбраны только новые
    create_datacamp_message(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='T2000'),
            pictures=DTC.OfferPictures(
                partner=DTC.PartnerPictures(
                    original=DTC.SourcePictures(
                        meta=create_update_meta(int(time.time())),
                        source=[
                            DTC.SourcePicture(url='https://2000url2/'),
                            DTC.SourcePicture(url='https://newsourceurl/'),
                            DTC.SourcePicture(url='new2url/'),
                            DTC.SourcePicture(url='2000url3/'),
                        ],
                    )
                )
            ),
        )
    ),
    # Проверяем, что получение пустого списка не порождает сообщений в пикробот
    create_datacamp_message(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='T3000'),
            pictures=DTC.OfferPictures(
                partner=DTC.PartnerPictures(
                    original=DTC.SourcePictures(meta=create_update_meta(int(time.time())), source=[])
                )
            ),
        )
    ),
]


@pytest.fixture(scope='module')
def basic_offers_table(yt_server, config):
    return DataCampBasicOffersTable(yt_server, config.yt_basic_offers_tablepath, data=BASIC_TABLE_DATA)


@pytest.fixture(scope='module')
def service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_service_offers_tablepath, data=SERVICE_TABLE_DATA)


@pytest.fixture(scope='module')
def actual_service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(
        yt_server, config.yt_actual_service_offers_tablepath, data=ACTUAL_SERVICE_TABLE_DATA
    )


@pytest.fixture(scope='session')
def datacamp_messages_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='session')
def picrobot_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def subscription_service_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff, 'subscription-service')
    return topic


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, datacamp_messages_topic, subscription_service_topic, picrobot_topic):
    cfg = {
        'general': {
            'color': 'white',
            'batch_size': 10,
            'enable_subscription_dispatcher': True
        },
        'logbroker': {
            'datacamp_messages_topic': datacamp_messages_topic.topic,
            'subscription_service_topic': subscription_service_topic.topic,
        },
        'picrobot': {
            'topic': picrobot_topic.topic,
        },
    }
    return PiperConfigMock(yt_server=yt_server, log_broker_stuff=log_broker_stuff, config=cfg)


@pytest.yield_fixture(scope='module')
def piper(
    yt_server,
    log_broker_stuff,
    config,
    datacamp_messages_topic,
    picrobot_topic,
    actual_service_offers_table,
    service_offers_table,
    basic_offers_table,
    subscription_service_topic
):
    resources = {
        'config': config,
        'datacamp_messages_topic': datacamp_messages_topic,
        'picrobot_topic': picrobot_topic,
        'actual_service_offers_table': actual_service_offers_table,
        'service_offers_table': service_offers_table,
        'basic_offers_table': basic_offers_table,
        'subscription_service_topic': subscription_service_topic,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


def test_filtering(piper, datacamp_messages_topic, picrobot_topic):
    for message in UPDATED_OFFER_MESSAGES:
        datacamp_messages_topic.write(message.SerializeToString())

    wait_until(lambda: piper.united_offers_processed >= len(UPDATED_OFFER_MESSAGES), timeout=60)

    sent_data = picrobot_topic.read(count=12)
    assert_that(
        sent_data,
        has_items(
            IsSerializedProtobuf(TImageRequest, {'Url': 'https://firstpicture/', 'MdsNamespace': 'marketpictesting'}),
            IsSerializedProtobuf(TImageRequest, {'Url': 'http://secondpicture/', 'MdsNamespace': 'marketpictesting'}),
            IsSerializedProtobuf(TImageRequest, {'Url': 'https://thirdpicture/', 'MdsNamespace': 'marketpictesting'}),
            IsSerializedProtobuf(TImageRequest, {'Url': 'https://newsourceurl/', 'MdsNamespace': 'marketpictesting'}),
            IsSerializedProtobuf(TImageRequest, {'Url': 'http://new2url/', 'MdsNamespace': 'marketpictesting'}),
            IsSerializedProtobuf(TImageRequest, {'Url': 'http://httpurl/', 'MdsNamespace': 'marketpictesting'}),

            IsSerializedProtobuf(TImageRequest, {'Url': 'https://firstpicture/', 'MdsNamespace': 'marketpictestingdirect'}),
            IsSerializedProtobuf(TImageRequest, {'Url': 'http://secondpicture/', 'MdsNamespace': 'marketpictestingdirect'}),
            IsSerializedProtobuf(TImageRequest, {'Url': 'https://thirdpicture/', 'MdsNamespace': 'marketpictestingdirect'}),
            IsSerializedProtobuf(TImageRequest, {'Url': 'https://newsourceurl/', 'MdsNamespace': 'marketpictestingdirect'}),
            IsSerializedProtobuf(TImageRequest, {'Url': 'http://new2url/', 'MdsNamespace': 'marketpictestingdirect'}),
            IsSerializedProtobuf(TImageRequest, {'Url': 'http://httpurl/', 'MdsNamespace': 'marketpictestingdirect'}),
        ),
    )

    # Проеряем, что топик опустел
    assert_that(picrobot_topic, HasNoUnreadData())
