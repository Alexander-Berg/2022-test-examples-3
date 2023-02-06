# coding: utf-8

from hamcrest import assert_that, has_items, is_not
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
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampBasicOffersTable, DataCampServiceOffersTable
from market.idx.yatf.resources.lbk_topic import LbkTopic


def create_approved_content(market_sku_id, market_sku_name, add_meta=False):
    return DTC.OfferContent(
        binding=DTC.ContentBinding(
            approved=DTC.Mapping(
                market_sku_id=market_sku_id,
                market_sku_name=market_sku_name,
                meta=create_update_meta(int(time.time())) if add_meta else None
            )
        )
    )


def create_mdm_status(master_data_version_counter):
    return DTC.OfferStatus(
        version=DTC.VersionStatus(
            master_data_version=DTC.VersionCounter(
                counter=master_data_version_counter
            )
        )
    )

BASIC_TABLE_DATA = [
    # no update, not sent
    offer_to_basic_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='T1000'),
        status=create_mdm_status(1000),
    )),
    # approved.market_sku_id has changed, ignored
    offer_to_basic_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='T2000'),
        content=create_approved_content(2000, '2000'),
        status=create_mdm_status(2000),
    )),
    # approved.market_sku_id has disappeared, ignored
    offer_to_basic_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='T3000'),
        content=create_approved_content(3000, '3000'),
        status=create_mdm_status(3000),
    )),
    # approved.market_sku_id has been created, ignored
    offer_to_basic_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='T4000'),
        status=create_mdm_status(4000),
    )),
    # approved hasn't changed, not sent
    offer_to_basic_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='T5000'),
        content=create_approved_content(5000, '5000'),
        status=create_mdm_status(5000),
    )),
    # approved.market_sku_name has changed, the version must change
    offer_to_basic_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='T6000'),
        content=create_approved_content(6000, '6000'),
        status=create_mdm_status(6000),
    )),
]

SERVICE_TABLE_DATA = [
    offer_to_service_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(business_id=1, offer_id=offer_id, shop_id=1)
    ))
    for offer_id in ('T1000', 'T3000', 'T4000', 'T5000', 'T6000')
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
    # approved.market_sku_id has changed, ignored
    create_datacamp_message(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='T2000',
            ),
            content=create_approved_content(12, '2000', True)
        )
    ),
    # approved.market_sku_id has disappeared, ignored
    create_datacamp_message(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='T3000',
            ),
            content=create_approved_content(None, '3000', True),
        )
    ),
    # approved.market_sku_id has been created, ignored
    create_datacamp_message(
        basic_offer=DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='T4000',
            ),
            content=create_approved_content(1234, None, True)
        ),
    ),
    # approved hasn't changed, not sent
    create_datacamp_message(
        basic_offer=DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='T5000',
            ),
            content=create_approved_content(5000, '5000', True),
        )
    ),
    # approved.market_sku_name has changed, the version must change
    create_datacamp_message(
        basic_offer=DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='T6000',
            ),
            content=create_approved_content(6000, '123456', True),
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
            'overriden_version_type_fields': {
                'VDT_FOR_MDM': {
                    'content/binding/approved': 'off',
                    'content/binding/approved/market_sku_name': 'on',
                }
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


def test_override_version(piper, datacamp_messages_topic, united_miner_topic):
    """
    Проверяем, что united_miner_sender переопределяет поля подписки
    """
    for message in UPDATED_OFFER_MESSAGES:
        datacamp_messages_topic.write(message.SerializeToString())

    # проверяем что данные по подписке пришли с необновлённой версией
    data = united_miner_topic.read(count=4)
    assert_that(data, has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    {
                        'basic': {
                            'identifiers': {
                                'business_id': 1,
                                'offer_id': 'T' + str(offer_num),
                            },
                            'status': {
                                'version': {
                                    'master_data_version': {
                                        'counter': offer_num,
                                    },
                                },
                            },
                        },
                    }
                ]
            }]
        }) for offer_num in (2000, 3000, 4000)
    ]))
    assert_that(data, has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    {
                        'basic': {
                            'identifiers': {
                                'business_id': 1,
                                'offer_id': 'T6000',
                            },
                        },
                    }
                ]
            }]
        }),
    ]))
    assert_that(data, is_not(has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    {
                        'basic': {
                            'identifiers': {
                                'business_id': 1,
                                'offer_id': 'T6000',
                            },
                        },
                        'status': {
                            'version': {
                                'master_data_version': {
                                    'counter': 6000,
                                },
                            },
                        },
                    }
                ]
            }]
        }),
    ])))

    # проверяем, что в топике больше ничего не осталось
    assert_that(united_miner_topic, HasNoUnreadData())
