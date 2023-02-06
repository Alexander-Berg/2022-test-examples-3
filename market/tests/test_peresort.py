# coding: utf-8

import pytest
import time
from datetime import datetime, timedelta
from hamcrest import assert_that, greater_than

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.yatf.matchers.yt_rows_matchers import HasDatacampYtUnitedOffersRows
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampBasicOffersTable,
    DataCampServiceOffersTable,
    DataCampPartnersTable
)
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.utils.utils import create_timestamp_from_json
from market.idx.datacamp.yatf.utils import create_update_meta, dict2tskv
from market.idx.pylibrary.datacamp.conversion import offer_to_basic_row
from market.pylibrary.proto_utils import message_from_data

NOW_TIME = int(time.time())
BLOCKED_BUSINESS = 1000
MIGRATING_OLD_BLUE_SHOP = 10
NOW_UTC = datetime.utcnow()  # JSON serializer should always use UTC
time_pattern = "%Y-%m-%dT%H:%M:%SZ"
current_time = NOW_UTC.strftime(time_pattern)
current_ts = create_timestamp_from_json(current_time)

FUTURE_UTC = NOW_UTC + timedelta(minutes=45)

DATACAMP_MESSAGES = [
    {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': 1,
                        'offer_id': 'content.peresort',
                    },
                    'content': {
                        'partner': {
                            'actual': {
                                'title': {
                                    'value': 'some.title.after.update',
                                    'meta': {
                                        'timestamp': NOW_UTC.strftime(time_pattern)
                                    }
                                },
                                'category': {
                                    'id': 2,
                                    'meta': {
                                        'timestamp': NOW_UTC.strftime(time_pattern)
                                    }
                                }
                            }
                        }
                    },
                },
            }, {
                'basic': {
                    'identifiers': {
                        'business_id': 1,
                        'offer_id': 'content.no.peresort',
                    },
                    'content': {
                        'partner': {
                            'actual': {
                                'title': {
                                    'value': 'some.title.1',
                                    'meta': {
                                        'timestamp': NOW_UTC.strftime(time_pattern)
                                    }
                                },
                                'category': {
                                    'id': 2,
                                    'meta': {
                                        'timestamp': NOW_UTC.strftime(time_pattern)
                                    }
                                }
                            }
                        }
                    },
                },
            }, {
                'basic': {
                    'identifiers': {
                        'business_id': 1,
                        'offer_id': 'maybe.peresort',
                    },
                    'meta': {
                        'maybe_peresort': True,
                    },
                },
            }]
        }]
    }
]


@pytest.fixture(scope='module')
def datacamp_messages_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def partners_table(yt_server, config):
    return DataCampPartnersTable(
        yt_server,
        config.yt_partners_tablepath,
        data=[
            {
                'shop_id': 1,
                'mbi': '\n\n'.join([
                    dict2tskv({
                        'shop_id': 1,
                        'business_id': 1,
                        'cpc': 'NO',
                        'is_turbo': 'true',
                        'turbo_plus': 'REAL',
                        'united_catalog_status': 'SUCCESS',
                    }),
                ]),
                'status': 'publish'
            },
            {
                'shop_id': 2,
                'mbi': '\n\n'.join([
                    dict2tskv({
                        'shop_id': 2,
                        'business_id': 1,
                        'united_catalog_status': 'NO',
                    }),
                ]),
                'status': 'publish'
            },
        ],
    )


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, datacamp_messages_topic):
    cfg = {
        'logbroker': {
            'datacamp_messages_topic': datacamp_messages_topic.topic,
        },
        'general': {
            'color': 'white',
        },
    }
    return PiperConfigMock(yt_server=yt_server, log_broker_stuff=log_broker_stuff, config=cfg)


@pytest.fixture(scope='module')
def basic_offers_table(yt_server, config):
    return DataCampBasicOffersTable(yt_server, config.yt_basic_offers_tablepath, data=[
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='content.peresort',
            ),
            content=DTC.OfferContent(
                partner=DTC.PartnerContent(
                    actual=DTC.ProcessedSpecification(
                        title=DTC.StringValue(meta=create_update_meta(10), value='some.title'),
                        category=DTC.PartnerCategory(meta=create_update_meta(10), id=1)
                    )
                ),
            ),
        )),
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='content.no.peresort',
            ),
            content=DTC.OfferContent(
                partner=DTC.PartnerContent(
                    actual=DTC.ProcessedSpecification(
                        title=DTC.StringValue(meta=create_update_meta(10), value='some.title'),
                        category=DTC.PartnerCategory(meta=create_update_meta(10), id=1)
                    )
                ),
            ),
        )),
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='maybe.peresort',
            ),
            content=DTC.OfferContent(
                partner=DTC.PartnerContent(
                    actual=DTC.ProcessedSpecification(
                        title=DTC.StringValue(meta=create_update_meta(10), value='some.title'),
                        category=DTC.PartnerCategory(meta=create_update_meta(10), id=1)
                    )
                ),
            ),
        ))
    ])


@pytest.fixture(scope='module')
def service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_service_offers_tablepath, data=[])


@pytest.fixture(scope='module')
def actual_service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_actual_service_offers_tablepath, data=[])


@pytest.yield_fixture(scope='module')
def piper(yt_server, log_broker_stuff, config, datacamp_messages_topic, partners_table, basic_offers_table,
          service_offers_table, actual_service_offers_table):
    resources = {
        'config': config,
        'datacamp_messages_topic': datacamp_messages_topic,
        'partners_table': partners_table,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


@pytest.fixture(scope='module')
def inserter(piper, datacamp_messages_topic):
    united_offers = 0
    for message in DATACAMP_MESSAGES:
        for offers in message['united_offers']:
            united_offers += len(offers)
        datacamp_messages_topic.write(message_from_data(message, DatacampMessage()).SerializeToString())

    wait_until(lambda: piper.united_offers_processed >= united_offers)


def test_content_peresort(piper, inserter, datacamp_messages_topic):
    """ Изменение категории и названия приводит к установке признака возможного пересорта """
    assert_that(piper.basic_offers_table.data, HasDatacampYtUnitedOffersRows([{
        'business_id': 1,
        'shop_sku': 'content.peresort',
        'status': IsSerializedProtobuf(DTC.OfferStatus, {
            'peresort': {
                'content_peresort_ts': {
                    'seconds': greater_than(NOW_TIME - 10)
                },
            }
        })
    }]))


def test_content_no_peresort(piper, inserter, datacamp_messages_topic):
    """ Недостаточное изменение названия не приводит к установке признака возможного пересорта """
    assert_that(piper.basic_offers_table.data, HasDatacampYtUnitedOffersRows([{
        'business_id': 1,
        'shop_sku': 'content.no.peresort',
        'status': IsSerializedProtobuf(DTC.OfferStatus, {
            'peresort': None
        })
    }]))


def test_maybe_peresort(piper, inserter, datacamp_messages_topic):
    """ Парсер может передать прямой признак возможного пересорта """
    assert_that(piper.basic_offers_table.data, HasDatacampYtUnitedOffersRows([{
        'business_id': 1,
        'shop_sku': 'maybe.peresort',
        'status': IsSerializedProtobuf(DTC.OfferStatus, {
            'peresort': {
                'content_peresort_ts': {
                    'seconds': greater_than(NOW_TIME - 10)
                },
            }
        })
    }]))
