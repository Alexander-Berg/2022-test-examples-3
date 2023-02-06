# coding: utf-8

import pytest
from datetime import datetime, timedelta
from hamcrest import assert_that, equal_to

from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.yatf.utils import dict2tskv
from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampPartnersTable, DataCampBasicOffersTable, DataCampServiceOffersTable
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.pylibrary.proto_utils import message_from_data
from market.idx.pylibrary.datacamp.conversion import offer_to_basic_row, offer_to_service_row

NOW_UTC = datetime.utcnow()  # JSON serializer should always use UTC
PAST_UTC = NOW_UTC - timedelta(minutes=45)
FUTURE_UTC = NOW_UTC + timedelta(minutes=45)
FUTURE_UTC_2 = NOW_UTC + timedelta(minutes=50)
time_pattern = "%Y-%m-%dT%H:%M:%SZ"

current_time = NOW_UTC.strftime(time_pattern)
past_time = PAST_UTC.strftime(time_pattern)
future_time = FUTURE_UTC.strftime(time_pattern)
future_time_2 = FUTURE_UTC_2.strftime(time_pattern)

PARTNERS = [
    {
        'shop_id': 1,
        'status': 'publish',
        'mbi': dict2tskv({
            'shop_id': 1,
            'business_id': 10,
            'datafeed_id': 1234,
            'warehouse_id': 54321,
            'is_push_partner': True,
        })
    },
]

BASIC_OFFERS = [
    offer_to_basic_row(
        message_from_data({
            'identifiers': {
                'business_id': 10,
                'offer_id': 'TestAmoreRtyUpdate'
            },
            'meta': {
                'ts_first_added': past_time
            },
        }, DTC.Offer())),
    offer_to_basic_row(
        message_from_data({
            'identifiers': {
                'business_id': 10,
                'offer_id': 'InvisibleOffer'
            },
            'meta': {
                'ts_first_added': past_time
            },
            'status': {
                'invisible': {
                    'flag': True
                }
            }
        }, DTC.Offer()))
]

SERVICE_OFFERS = [
    offer_to_service_row(message_from_data({
        'identifiers': {
            'business_id': business_id,
            'offer_id': shop_sku,
            'shop_id': shop_id,
            'warehouse_id': 0,
        },
        'meta': {
            'rgb': DTC.BLUE,
            'scope': DTC.SERVICE,
            'ts_first_added': past_time
        },
        'bids': {
            'amore_beru_vendor_data': {
                'meta': {
                    'timestamp': past_time,
                },
                'amore_pl_gen_create': 1,
            }
        },
    }, DTC.Offer()))
    for business_id, shop_id, shop_sku in [
        (10, 1, 'TestAmoreRtyUpdate'),
        (10, 1, 'InvisibleOffer'),
    ]
]

ACTUAL_SERVICE_OFFERS = [
    offer_to_service_row(message_from_data({
        'identifiers': {
            'business_id': business_id,
            'offer_id': shop_sku,
            'shop_id': shop_id,
            'warehouse_id': warehouse_id,
            'feed_id': feed_id,
        },
        'meta': {
            'rgb': DTC.BLUE,
            'scope': DTC.SERVICE,
            'ts_first_added': past_time
        },
    }, DTC.Offer()))
    for business_id, shop_id, shop_sku, warehouse_id, feed_id in [
        (10, 1, 'TestAmoreRtyUpdate', 54321, 1234),
        (10, 1, 'TestAmoreRtyUpdate', 145, 1235),
        (10, 1, 'TestAmoreRtyUpdate', 147, 1236),
        (10, 1, 'InvisibleOffer', 54321, 1234),
    ]
]


@pytest.fixture(scope='module')
def partners_table(yt_server, config):
    return DataCampPartnersTable(yt_server, config.yt_partners_tablepath, PARTNERS)


@pytest.fixture(scope='module')
def basic_offers_table(yt_server, config):
    return DataCampBasicOffersTable(yt_server, config.yt_basic_offers_tablepath, data=BASIC_OFFERS)


@pytest.fixture(scope='module')
def service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_service_offers_tablepath, data=SERVICE_OFFERS)


@pytest.fixture(scope='module')
def actual_service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_actual_service_offers_tablepath, data=ACTUAL_SERVICE_OFFERS)


@pytest.fixture(scope='module')
def amore_topic_data():
    return [
        [{
        'identifiers': {
            'feed_id': 1234,
            'offer_id': 'TestAmoreRtyUpdate',
        },
        'bids': {
            'amore_beru_vendor_data': {
                'meta': {
                    'timestamp': future_time,
                },
                'value': b'00000000',
                'amore_pl_gen_create': 2,
            }
        },
        'meta': {
            'rgb': DTC.BLUE,
        },
    }], [{
        'identifiers': {
            'feed_id': 1234,
            'offer_id': 'TestAmoreRtyUpdate',
        },
        'bids': {
            'amore_beru_vendor_data': {
                'meta': {
                    'timestamp': future_time_2,
                },
                'value': b'00000000',
                'amore_pl_gen_create': 4,  # не отсылаем сообщения в сторону репорта при обновлении только amore_pl_gen_create
            }
        },
        'meta': {
            'rgb': DTC.BLUE,
        },
    }], [{
        'identifiers': {
            'feed_id': 1234,
            'offer_id': 'InvisibleOffer',
        },
        'bids': {
            'amore_beru_vendor_data': {
                'meta': {
                    'timestamp': future_time,
                },
                'value': b'00000000',
                'amore_pl_gen_create': 2,
            }
        },
        'meta': {
            'rgb': DTC.BLUE,
        },
    }]]


@pytest.fixture(scope='module')
def amore_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff, 'amore_topic')
    return topic


@pytest.fixture(scope='module')
def rty_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff, 'rty_topic')
    return topic


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, amore_topic, rty_topic):
    cfg = {
        'logbroker': {
            'rty_topic': rty_topic.topic,
            'amore_topic': amore_topic.topic,
        },
        'general': {
            'color': 'blue',
        }
    }
    return PiperConfigMock(
        yt_server=yt_server,
        log_broker_stuff=log_broker_stuff,
        config=cfg
    )


@pytest.yield_fixture(scope='module')
def piper(
    yt_server,
    log_broker_stuff,
    config,
    basic_offers_table,
    service_offers_table,
    actual_service_offers_table,
    amore_topic,
    rty_topic,
    partners_table
):
    resources = {
        'config': config,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'partners_table': partners_table,
        'amore_topic': amore_topic,
        'rty_topic': rty_topic,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


@pytest.yield_fixture(scope='module')
def rty_topic_data(piper, amore_topic, amore_topic_data, rty_topic):
    for message in amore_topic_data:
        amore_topic.write(message_from_data({'offer': message}, DTC.OffersBatch()).SerializeToString())
    result = rty_topic.read(count=3)

    # проверяем, что в топике больше нет данных, которые мы можем вычитать
    assert_that(rty_topic, HasNoUnreadData())

    return result


def test_amore_update(rty_topic_data):
    """ При изменении bids в оригинальной сервисной части отправляются все актуальные офферы """
    assert_that(len(rty_topic_data), equal_to(3))
