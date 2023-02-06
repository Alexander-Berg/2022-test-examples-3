# coding: utf-8

from hamcrest import assert_that, equal_to, is_not
import pytest
from datetime import datetime

from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import (
    OfferIdentifiers, Flag,
    PUSH_PARTNER_API, PULL_PARTNER_API, MARKET_ABO, MARKET_IDX, MARKET_PRICELABS,
    BLUE, WHITE,
)
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import Offer as DatacampOffer
from market.idx.datacamp.proto.api.SyncChangeOffer_pb2 import ChangeOfferRequest
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock

from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampBasicOffersTable,
    DataCampServiceOffersTable
)

from market.idx.yatf.matchers.yt_rows_matchers import HasOffers

from market.idx.datacamp.yatf.utils import create_meta, create_status, create_update_meta
from market.idx.pylibrary.datacamp.utils import wait_until
from market.pylibrary.proto_utils import message_from_data
time_pattern = "%Y-%m-%dT%H:%M:%SZ"


def make_api_data(
    shop_id,
    offer_id,
    warehouse_id,
    status,
    ts,
    source=PUSH_PARTNER_API,
    color=BLUE,
    available_for_businesses=None,
    prohibited_for_persons=None
):
    return DatacampOffer(
        identifiers=OfferIdentifiers(
            shop_id=shop_id,
            offer_id=offer_id,
            warehouse_id=warehouse_id,
            business_id=1,
        ),
        meta=create_meta(color=color),
        status=create_status(
            status,
            ts,
            source,
            available_for_businesses=None if available_for_businesses is None else Flag(
                flag=available_for_businesses,
                meta=create_update_meta(ts, source=source),
            ),
            prohibited_for_persons=None if prohibited_for_persons is None else Flag(
                flag=prohibited_for_persons,
                meta=create_update_meta(ts, source=source),
            )
        ),
    )


API_DATA = [
    # Обновление цены и статуса available_for_businesses
    make_api_data(1, 'T1050', 10, True, 100, prohibited_for_persons=True),
    # Обновление цены и статуса available_for_businesses
    make_api_data(1, 'T1000', 10, True, 100, available_for_businesses=True),
    # данные с pull источником должны быть полностью проигнорированы
    make_api_data(1, 'T800', 10, True, 100, PULL_PARTNER_API),
    # данные не своего цвета не должны фильтроваться
    make_api_data(1, 'T800', 0, True, 100, color=WHITE),
    # данные с MARKET_ABO и (пока) только синего цвета не должны игнорироваться
    make_api_data(1, 'T900', 10, True, 100, MARKET_ABO),
    # а данные с MARKET_ABO, но не синего цвета (пока) должны игнорироваться
    make_api_data(1, 'T800', 10, True, 100, MARKET_ABO, color=WHITE),
    # данные с MARKET_PRICELABS
    make_api_data(1, 'T950', 10, True, 100, MARKET_PRICELABS),
]


def make_expected_status_dict(status, ts):
    return {
        'disabled': [{
            'flag': status,
            'meta': {
                'timestamp': datetime.utcfromtimestamp(ts).strftime(time_pattern)
            }
        }]
    }


EXPECTED_SERVICE_OFFERS = [
    message_from_data({
        'identifiers': {
            'business_id': 1,
            'offer_id': 'T1050',
            'shop_id': 1,
        },
        'status': make_expected_status_dict(True, 100),
    }, DatacampOffer()),
    message_from_data({
        'identifiers': {
            'business_id': 1,
            'offer_id': 'T1000',
            'shop_id': 1,
        },
        'status': make_expected_status_dict(True, 100),
    }, DatacampOffer()),
    message_from_data({
        'identifiers': {
            'business_id': 1,
            'offer_id': 'T800',
            'shop_id': 1,
        },
        'status': make_expected_status_dict(True, 100),
    }, DatacampOffer()),
    message_from_data({
        'identifiers': {
            'business_id': 1,
            'offer_id': 'T900',
            'shop_id': 1,
        },
        'status': {
            'disabled': [{
                'flag': True,
                'meta': {
                    'source': MARKET_ABO,
                    'timestamp': datetime.utcfromtimestamp(100).strftime(time_pattern)
                }
            }]
        }
    }, DatacampOffer()),
    message_from_data({
        'identifiers': {
            'business_id': 1,
            'offer_id': 'T950',
            'shop_id': 1,
        },
        'status': {
            'disabled': [{
                'flag': True,
                'meta': {
                    'source': MARKET_PRICELABS,
                    'timestamp': datetime.utcfromtimestamp(100).strftime(time_pattern)
                }
            }]
        }
    }, DatacampOffer())]


EXPECTED_ACTUAL_SERVICE_OFFERS = [
    message_from_data({
        'identifiers': {
            'business_id': 1,
            'offer_id': 'T1050',
            'shop_id': 1,
            'warehouse_id': 10,
        },
    }, DatacampOffer()),
    message_from_data({
        'identifiers': {
            'business_id': 1,
            'offer_id': 'T1000',
            'shop_id': 1,
            'warehouse_id': 10,
        },
    }, DatacampOffer()),
    message_from_data({
        'identifiers': {
            'business_id': 1,
            'offer_id': 'T800',
            'shop_id': 1,
            'warehouse_id': 0,
        },
        'status': {
            'disabled': [{
                'flag': True,
                'meta': {
                    'source': MARKET_IDX,
                    'timestamp': datetime.utcfromtimestamp(100).strftime(time_pattern)
                }
            }]
        },
    }, DatacampOffer()),
    message_from_data({
        'identifiers': {
            'business_id': 1,
            'offer_id': 'T900',
            'shop_id': 1,
            'warehouse_id': 10,
        },
    }, DatacampOffer()),
    message_from_data({
        'identifiers': {
            'business_id': 1,
            'offer_id': 'T950',
            'shop_id': 1,
            'warehouse_id': 10,
        },
    }, DatacampOffer())
]


EXPECTED_BASIC_OFFERS = [
    message_from_data({
        'identifiers': {
            'business_id': 1,
            'offer_id': 'T1050',
        },
    }, DatacampOffer()),
    message_from_data({
        'identifiers': {
            'business_id': 1,
            'offer_id': 'T1000',
        },
    }, DatacampOffer()),
    message_from_data({
        'identifiers': {
            'business_id': 1,
            'offer_id': 'T900',
        },
    }, DatacampOffer()),
    message_from_data({
        'identifiers': {
            'business_id': 1,
            'offer_id': 'T800',
        },
    }, DatacampOffer()),
    message_from_data({
        'identifiers': {
            'business_id': 1,
            'offer_id': 'T950',
        },
    }, DatacampOffer())
]


@pytest.fixture(scope='module')
def basic_offers_table(yt_server, config):
    return DataCampBasicOffersTable(yt_server, config.yt_basic_offers_tablepath)


@pytest.fixture(scope='module')
def service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_service_offers_tablepath)


@pytest.fixture(scope='module')
def actual_service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_actual_service_offers_tablepath)


@pytest.fixture(scope='module')
def api_data_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, api_data_topic):
    cfg = {
        'general': {
            'color': 'blue',
        },
        'logbroker': {
            'api_data_topic': api_data_topic.topic,
        },
    }
    return PiperConfigMock(yt_server=yt_server,
                           log_broker_stuff=log_broker_stuff,
                           config=cfg)


@pytest.yield_fixture(scope='module')
def piper(
    yt_server,
    log_broker_stuff,
    config,
    api_data_topic,
    basic_offers_table,
    service_offers_table,
    actual_service_offers_table
):
    resources = {
        'config': config,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'api_data_topic': api_data_topic,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


@pytest.yield_fixture(scope='module')
def inserter(piper, api_data_topic):
    for api in API_DATA:
        request = ChangeOfferRequest()
        request.offer.extend([api])
        api_data_topic.write(request.SerializeToString())

    wait_until(lambda: piper.api_data_processed >= len(API_DATA), timeout=60)


def test_api_flags(
    piper,
    inserter,
    basic_offers_table,
    service_offers_table,
    actual_service_offers_table
):
    """
    piper читает апи цены из топика и корректно создает офферы в таблицах, несмотря на то, что ts_created не передается
    """
    assert_that(len(piper.service_offers_table.data), equal_to(len(EXPECTED_SERVICE_OFFERS)))
    assert_that(piper.service_offers_table.data, HasOffers(EXPECTED_SERVICE_OFFERS))

    assert_that(len(piper.actual_service_offers_table.data), equal_to(len(EXPECTED_ACTUAL_SERVICE_OFFERS)))
    assert_that(piper.actual_service_offers_table.data, HasOffers(EXPECTED_ACTUAL_SERVICE_OFFERS))

    assert_that(len(piper.basic_offers_table.data), equal_to(len(EXPECTED_BASIC_OFFERS)))
    assert_that(piper.basic_offers_table.data, HasOffers(EXPECTED_BASIC_OFFERS))


@pytest.mark.parametrize("flag_name, offer_id", [
    ('available_for_businesses', 'T1000'),
    ('prohibited_for_persons', 'T1050'),
])
def test_service_only_flags(
    piper,
    inserter,
    basic_offers_table,
    service_offers_table,
    actual_service_offers_table,
    flag_name,
    offer_id,
):
    """ Проверяем, что статус записывается только в сервисную часть """
    assert_that(piper.service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': offer_id,
                'shop_id': 1,
            },
            'status': {
                flag_name: {
                    'flag': True
                }
            },
        }, DatacampOffer())
    ]))
    assert_that(piper.basic_offers_table.data, is_not(HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': offer_id,
            },
            'status': {
                flag_name: {
                    'flag': True
                }
            },
        }, DatacampOffer())
    ])))
    assert_that(piper.actual_service_offers_table.data, is_not(HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': offer_id,
                'shop_id': 1,
                'warehouse_id': 10,
            },
            'status': {
                flag_name: {
                    'flag': True
                }
            },
        }, DatacampOffer())
    ])))
