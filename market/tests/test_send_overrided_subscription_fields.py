# coding: utf-8

from hamcrest import assert_that, has_items
import pytest
import time

from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.yatf.utils import create_update_meta
from market.idx.pylibrary.datacamp.conversion import offer_to_basic_row, offer_to_service_row
from market.idx.datacamp.proto.offer.UnitedOffer_pb2 import UnitedOffersBatch, UnitedOffer
from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobufMap
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampBasicOffersTable, DataCampServiceOffersTable
from market.idx.yatf.resources.lbk_topic import LbkTopic

BASIC_TABLE_DATA = [
    offer_to_basic_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='T1000'),
    )),
    offer_to_basic_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='T2000'),
        pictures=DTC.OfferPictures(
            partner=DTC.PartnerPictures(
                original=DTC.SourcePictures(
                    meta=create_update_meta(0),
                    source=[
                        DTC.SourcePicture(url='source_url')
                    ]
                )
            )
        ),
    )),
    offer_to_basic_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='T3000'),
        content=DTC.OfferContent(
            partner=DTC.PartnerContent(
                partner_content_desc=DTC.PartnerContentDescription(title='description_title')
            )
        ),
    )),
    offer_to_basic_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='T4000'),
    )),
    offer_to_basic_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='T5000'),
    )),
    offer_to_basic_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='T6000'),
    )),
]

SERVICE_TABLE_DATA = [
    offer_to_service_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(business_id=1, offer_id=offer_id, shop_id=1),
        price=offerPrice
    ))
    for offer_id, offerPrice in (
        ('T1000', None),
        ('T2000', None),
        ('T3000', None),
        ('T4000', None),
        # offer with price
        ('T5000', DTC.OfferPrice(
            basic=DTC.PriceBundle(
                binary_price=DTC.PriceExpression(
                    id='RUR',
                    price=100,
                ),
                meta=create_update_meta(1234),
            )
        )),
        # offer with deleted price
        ('T6000', DTC.OfferPrice(
            basic=DTC.PriceBundle(
                meta=create_update_meta(1234),
            )
        ))
    )
]

ACTUAL_SERVICE_TABLE_DATA = [
    offer_to_service_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(business_id=1, offer_id=offer_id, shop_id=1)
    ))
    for offer_id in ('T1000', 'T3000', 'T4000', 'T5000', 'T6000')
]


def create_datacamp_message(basic_offer, service_offers=None):
    return DatacampMessage(
        united_offers=[UnitedOffersBatch(offer=[UnitedOffer(
            basic=basic_offer,
            service=service_offers
        )])])


UPDATED_OFFER_MESSAGES = [
    create_datacamp_message(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='T2000',
            ),
            pictures=DTC.OfferPictures(
                partner=DTC.PartnerPictures(
                    original=DTC.SourcePictures(
                        meta=create_update_meta(int(time.time())),
                        source=[
                            DTC.SourcePicture(url='new_source_url')
                        ]
                    )
                )
            ),
        )
    ),
    create_datacamp_message(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='T3000',
            ),
            content=DTC.OfferContent(
                partner=DTC.PartnerContent(
                    actual=DTC.ProcessedSpecification(
                        title=DTC.StringValue(meta=create_update_meta(int(time.time())), value='should_be_ignored')
                    )
                )
            ),
        )
    ),
    # create price in offer for the first time
    create_datacamp_message(
        basic_offer=DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='T4000',
            ),
        ),
        service_offers={
            1: DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=1,
                    offer_id='T4000',
                    shop_id=1
                ),
                price=DTC.OfferPrice(
                    basic=DTC.PriceBundle(
                        binary_price=DTC.PriceExpression(
                            id='RUR',
                            price=100,
                        ),
                        meta=create_update_meta(int(time.time())),
                    )
                )
            )
        }
    ),
    # just update price in proto - won't be sent to miner
    create_datacamp_message(
        basic_offer=DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='T5000',
            ),
        ),
        service_offers={
            1: DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=1,
                    offer_id='T5000',
                    shop_id=1
                ),
                price=DTC.OfferPrice(
                    basic=DTC.PriceBundle(
                        binary_price=DTC.PriceExpression(
                            id='RUR',
                            price=200,
                        ),
                        meta=create_update_meta(int(time.time())),
                    )
                )
            )
        }
    ),
    # there was empty price in offer - sent new binary price - should be sent to miner
    create_datacamp_message(
        basic_offer=DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='T6000',
            ),
        ),
        service_offers={
            1: DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=1,
                    offer_id='T6000',
                    shop_id=1
                ),
                price=DTC.OfferPrice(
                    basic=DTC.PriceBundle(
                        binary_price=DTC.PriceExpression(
                            id='RUR',
                            price=300,
                        ),
                        meta=create_update_meta(int(time.time())),
                    )
                )
            )
        }
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
def united_miner_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='session')
def config(yt_server, log_broker_stuff, datacamp_messages_topic, united_miner_topic):
    cfg = {
        'general': {
            'color': 'white',
            'batch_size': 10,
        },
        'logbroker': {
            'datacamp_messages_topic': datacamp_messages_topic.topic,
        },
        'miner': {
            'united_topic': united_miner_topic.topic,
            'overriden_subscription_type_fields': {
                'identifiers/offer_id': 'ST_NONE',
                'pictures/partner/original': 'ST_NONE',
                'price/basic/binary_price': 'ST_EXISTENCE_TRIGGER',
            }
        },
    }
    return PiperConfigMock(yt_server=yt_server, log_broker_stuff=log_broker_stuff, config=cfg)


@pytest.yield_fixture(scope='module')
def piper(
    yt_server,
    log_broker_stuff,
    config,
    datacamp_messages_topic,
    united_miner_topic,
    actual_service_offers_table,
    service_offers_table,
    basic_offers_table,
):
    resources = {
        'config': config,
        'datacamp_messages_topic': datacamp_messages_topic,
        'united_miner_topic': united_miner_topic,
        'actual_service_offers_table': actual_service_offers_table,
        'service_offers_table': service_offers_table,
        'basic_offers_table': basic_offers_table,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


def test_override_trigger_to_none(piper, datacamp_messages_topic, united_miner_topic):
    """
    Проверяем, что united_miner_sender переопределяет поля подписки
    """
    for message in UPDATED_OFFER_MESSAGES:
        datacamp_messages_topic.write(message.SerializeToString())

    # проверяем что данные по подписке пришли (по переопределенной и по не переопределенной)
    data = united_miner_topic.read(count=2)
    assert_that(data, has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    {
                        'basic': {
                            'identifiers': {
                                'business_id': 1,
                                'offer_id': 'T4000',
                            }
                        },
                        'service': IsProtobufMap({
                            1: {
                                'identifiers': {
                                    'shop_id': 1,
                                    'business_id': 1,
                                    'offer_id': 'T4000',
                                }
                            }
                        }),
                    }
                ]
            }]
        }),
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    {
                        'basic': {
                            'identifiers': {
                                'business_id': 1,
                                'offer_id': 'T6000',
                            }
                        },
                        'service': IsProtobufMap({
                            1: {
                                'identifiers': {
                                    'shop_id': 1,
                                    'business_id': 1,
                                    'offer_id': 'T6000',
                                }
                            }
                        }),
                    }
                ]
            }]
        })
    ]))

    # проверяем, что в топике больше ничего не осталось
    assert_that(united_miner_topic, HasNoUnreadData())
