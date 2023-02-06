# coding: utf-8

from hamcrest import assert_that
import pytest
import time

from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.offer.UnitedOffer_pb2 import UnitedOffersBatch, UnitedOffer
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages
from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.yatf.matchers.protobuf_matchers import IsProtobufMap
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampBasicOffersTable, DataCampServiceOffersTable
from market.idx.datacamp.yatf.utils import create_update_meta
from market.idx.pylibrary.datacamp.conversion import offer_to_service_row, offer_to_basic_row


BASIC_TABLE_DATA = [
    offer_to_basic_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='T2000'),
        pictures=DTC.OfferPictures(
            partner=DTC.PartnerPictures(
                original=DTC.SourcePictures(
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
]

SERVICE_TABLE_DATA = [
    offer_to_service_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(business_id=1, offer_id=offer_id, shop_id=1)
    ))
    for offer_id in ('T2000', 'T3000')
] + [
    offer_to_service_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='T4000', shop_id=1),
        price=DTC.OfferPrice(
            basic=DTC.PriceBundle(
                binary_price=DTC.PriceExpression(
                    price=10
                )
            )
        )
    ))
]

ACTUAL_SERVICE_TABLE_DATA = [
    offer_to_service_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='T3000', shop_id=1)
    ))
]


def create_datacamp_message(basic_offer):
    return DatacampMessage(united_offers=[UnitedOffersBatch(offer=[UnitedOffer(basic=basic_offer)])])


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
    DatacampMessage(united_offers=[UnitedOffersBatch(offer=[UnitedOffer(basic=DTC.Offer(
        identifiers=DTC.OfferIdentifiers(
            business_id=123,
            offer_id="l1"
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
    ), service={
        1: DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=123,
                offer_id="l1",
                shop_id=1
            ),
            meta=DTC.OfferMeta(
                rgb=DTC.LAVKA
            )
        )
    })])]),
    DatacampMessage(united_offers=[UnitedOffersBatch(offer=[UnitedOffer(basic=DTC.Offer(
        identifiers=DTC.OfferIdentifiers(
            business_id=1,
            offer_id="T4000"
        ),
        content=DTC.OfferContent(
            partner=DTC.PartnerContent(
                original=DTC.OriginalSpecification(
                    name=DTC.StringValue(meta=create_update_meta(int(time.time())), value='name')
                )
            )
        ),
    ), service={
        1: DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id="T4000",
                shop_id=1
            ),
        )
    }),
    ])]),
    DatacampMessage(united_offers=[UnitedOffersBatch(offer=[UnitedOffer(basic=DTC.Offer(
        identifiers=DTC.OfferIdentifiers(
            business_id=1,
            offer_id="sortdc_stub"
        ),
        content=DTC.OfferContent(
            partner=DTC.PartnerContent(
                original=DTC.OriginalSpecification(
                    name=DTC.StringValue(meta=create_update_meta(int(time.time())), value='sortdc_stub_should_be_ignored')
                )
            )
        ),
    ), service={
        1: DTC.Offer(
            meta=DTC.OfferMeta(
                rgb=DTC.VERTICAL_GOODS_ADS,
                sortdc_context=DTC.TSortDCExportContext(
                    meta=create_update_meta(int(time.time())),
                    export_items=[
                        DTC.TSortDCExportContext.TExportItem(
                            offer_type=DTC.TSortDCExportContext.TExportItem.FEED_OFFER,
                        )
                    ],
                )
            ),
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id="sortdc_stub",
                shop_id=1
            ),
        )
    }),
    ])]),
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


@pytest.fixture(scope='module')
def subscription_service_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff, 'subscription-service')
    return topic


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, datacamp_messages_topic, united_miner_topic, subscription_service_topic):
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
        'miner': {
            'united_topic': united_miner_topic.topic,
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
    subscription_service_topic
):
    resources = {
        'config': config,
        'datacamp_messages_topic': datacamp_messages_topic,
        'united_miner_topic': united_miner_topic,
        'actual_service_offers_table': actual_service_offers_table,
        'service_offers_table': service_offers_table,
        'basic_offers_table': basic_offers_table,
        'subscription_service_topic': subscription_service_topic,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


def test_filtering(piper, datacamp_messages_topic, united_miner_topic):
    """
    Проверяем, что united_miner_sender правильно фильтрует оферы
    """
    for message in UPDATED_OFFER_MESSAGES:
        datacamp_messages_topic.write(message.SerializeToString())

    data = united_miner_topic.read(count=3)
    assert_that(
        data,
        HasSerializedDatacampMessages(
            [
                {
                    'united_offers': [
                        {
                            'offer': [
                                {
                                    'basic': {
                                        'identifiers': {'offer_id': 'T2000', 'business_id': 1},
                                        'pictures': {'partner': {'original': {'source': [{'url': 'new_source_url'}]}}},
                                        'tech_info': {'last_mining': {'reason': 1}}
                                    }
                                }
                            ]
                        }
                    ]
                },
                {
                    'united_offers': [
                        {
                            'offer': [
                                {
                                    'service': IsProtobufMap({
                                        1: {
                                            'identifiers': {'offer_id': 'T4000', 'business_id': 1, 'shop_id': 1},
                                            'price': {'basic': {'binary_price': {'price': 10}}}
                                        }
                                    })
                                }
                            ]
                        }
                    ]
                },
                {
                    'united_offers': [
                        {
                            'offer': [
                                {
                                    'basic': {
                                        'identifiers': {'offer_id': 'l1', 'business_id': 123},
                                        'pictures': {'partner': {'original': {'source': [{'url': 'new_source_url'}]}}},
                                        'tech_info': {'last_mining': {'reason': 1}}
                                    },
                                    'service': IsProtobufMap({
                                        1: {
                                            'identifiers': {'offer_id': 'l1', 'business_id': 123, 'shop_id': 1},
                                            'tech_info': {'last_mining': {'reason': 1}}
                                        }
                                    })
                                }
                            ]
                        }
                    ]
                }
            ]
        ),
    )

    # проверяем, что в топике больше ничего не осталось
    assert_that(united_miner_topic, HasNoUnreadData())
