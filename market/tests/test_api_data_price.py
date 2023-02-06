# coding: utf-8

from hamcrest import assert_that, equal_to
import pytest
from datetime import datetime

from market.idx.datacamp.proto.api.SyncChangeOffer_pb2 import ChangeOfferRequest
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import (
    OfferIdentifiers, OfferScope,
    OfferPrice, PriceBundle, PriceExpression, UpdateMeta,
    PUSH_PARTNER_API, PULL_PARTNER_API, MARKET_IDX_PRICE_QUARANTINE,
    BLUE, WHITE,
    Offer as DatacampOffer,
)
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock

from market.idx.pylibrary.datacamp.conversion import offer_to_service_row

from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampBasicOffersTable,
    DataCampServiceOffersTable
)

from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
from market.idx.yatf.utils.utils import create_pb_timestamp
from market.pylibrary.proto_utils import message_from_data

from market.idx.datacamp.yatf.utils import create_meta, create_api_price
from market.idx.pylibrary.datacamp.utils import wait_until

time_pattern = "%Y-%m-%dT%H:%M:%SZ"


def make_api_data(shop_id, offer_id, warehouse_id, price, ts, source=PUSH_PARTNER_API, color=BLUE, scope=OfferScope.SELECTIVE):
    return DatacampOffer(
        identifiers=OfferIdentifiers(
            shop_id=shop_id,
            offer_id=offer_id,
            warehouse_id=warehouse_id,
            business_id=1,
        ),
        meta=create_meta(color=color, scope=scope),
        price=create_api_price(price, ts, source),
    )


def make_api_data_with_currency(shop_id, offer_id, warehouse_id, price, ts, currency):
    offer = make_api_data(shop_id, offer_id, warehouse_id, price, ts)
    offer.price.basic.binary_price.id = currency
    return offer


API_DATA = [
    make_api_data(1, 'T1000', 10, 20, 100),
    # данные с pull источником должны быть полностью проигнорированы
    make_api_data(2, 'T800', 10, 300, 100, PULL_PARTNER_API),
    # данные не своего цвета не должны фильтроваться
    make_api_data(3, 'T900', 0, 300, 100, color=WHITE),
    # Проверяем, что update с пустой ценой получает скрытие MARKET_IDX (by united_tx_handler.cpp)
    make_api_data(6, 'T1400', 147, price=0, ts=100, color=WHITE),
    # Проверяем, что у синих не-рублевая цена очищается (by united_tx_handler.cpp)
    make_api_data_with_currency(7, 'Blue.Unknown.Currency', 0, price=100, ts=100, currency="KZT"),
    # Проверяем, что оффер попадает в карантин при сильном изменении цены
    make_api_data(6, 'OfferForPriceQuarantine', 0, price=100, ts=100),
]


def make_expected_price_dict(price, ts, last_valid_price=None):
    result = {
        'basic': {
            'binary_price': {
                'price': price * 10**7,
            },
            'meta': {
                'timestamp': datetime.utcfromtimestamp(ts).strftime(time_pattern),
                'source': PUSH_PARTNER_API,
            }
        }
    }
    if last_valid_price:
        result['last_valid_price'] = {
            'binary_price': {
                'price': last_valid_price * 10**7
            },
        }


EXPECTED_SERVICE_OFFERS = [
    message_from_data({
        'identifiers': {
            'business_id': 1,
            'offer_id': 'T1000',
            'shop_id': 1,
        },
        'price': make_expected_price_dict(20, 100),
    }, DatacampOffer()),
    message_from_data({
        'identifiers': {
            'business_id': 1,
            'offer_id': 'T900',
            'shop_id': 3,
        },
        'price': make_expected_price_dict(300, 100),
    }, DatacampOffer()),
    message_from_data({
        'identifiers': {
            'business_id': 1,
            'offer_id': 'T1400',
            'shop_id': 6,
        },
        'price': make_expected_price_dict(0, 100),
    }, DatacampOffer()),
    message_from_data({
        'identifiers': {
            'business_id': 1,
            'offer_id': 'Blue.Unknown.Currency',
            'shop_id': 7,
        },
        'price': None,
    }, DatacampOffer()),
    message_from_data({
        'identifiers': {
            'business_id': 1,
            'offer_id': 'OfferForPriceQuarantine',
            'shop_id': 6,
        },
        'price': make_expected_price_dict(100, 100, last_valid_price=1000),
        'resolution': {
            'by_source': [{
                'verdict': [{
                    'results': [{
                        'is_banned': True,
                        'messages': [{
                            'code': '49w'
                        }]
                    }]
                }],
                'meta': {
                    'source': MARKET_IDX_PRICE_QUARANTINE
                }
            }]
        },
        'status': {
            'disabled': [{
                'meta': {
                    'source': MARKET_IDX_PRICE_QUARANTINE
                },
                'flag': True
            }]
        }
    }, DatacampOffer()),
]


EXPECTED_ACTUAL_SERVICE_OFFERS = [
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
            'offer_id': 'T900',
            'shop_id': 3,
            'warehouse_id': 0,
        },
    }, DatacampOffer()),
    message_from_data({
        'identifiers': {
            'business_id': 1,
            'offer_id': 'T1400',
            'shop_id': 6,
            'warehouse_id': 147,
        }
    }, DatacampOffer())
]


EXPECTED_BASIC_OFFERS = [
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
            'shop_id': None,
            'warehouse_id': None,
        },
        'price': None,
    }, DatacampOffer()),
    message_from_data({
        'identifiers': {
            'business_id': 1,
            'offer_id': 'T1400',
        },
    }, DatacampOffer()),
    message_from_data({
        'identifiers': {
            'business_id': 1,
            'offer_id': 'Blue.Unknown.Currency',
        },
    }, DatacampOffer()),
    message_from_data({
        'identifiers': {
            'business_id': 1,
            'offer_id': 'OfferForPriceQuarantine',
        },
    }, DatacampOffer()),
]


@pytest.fixture(scope='module')
def basic_offers_table(yt_server, config):
    return DataCampBasicOffersTable(yt_server, config.yt_basic_offers_tablepath)


@pytest.fixture(scope='module')
def service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_service_offers_tablepath, data=[
        offer_to_service_row(DatacampOffer(
            identifiers=OfferIdentifiers(
                business_id=1,
                offer_id='T1400',
                shop_id=6,
            ),
            meta=create_meta(10, color=BLUE),
            price=OfferPrice(
                basic=PriceBundle(
                    binary_price=PriceExpression(
                        id='RUR',
                        price=0,
                    ),
                    meta=UpdateMeta(
                        timestamp=create_pb_timestamp(50),
                    )
                ),
            )
        )),
        offer_to_service_row(DatacampOffer(
            identifiers=OfferIdentifiers(
                business_id=1,
                offer_id='OfferForPriceQuarantine',
                shop_id=6,
            ),
            meta=create_meta(10, color=BLUE),
            price=OfferPrice(
                basic=PriceBundle(
                    binary_price=PriceExpression(
                        id='RUR',
                        price=1000,
                    ),
                    meta=UpdateMeta(
                        timestamp=create_pb_timestamp(50),
                    )
                ),
            )
        )),
    ])


@pytest.fixture(scope='module')
def actual_service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_actual_service_offers_tablepath, data=[
        offer_to_service_row(DatacampOffer(
            identifiers=OfferIdentifiers(
                business_id=1,
                offer_id='T1400',
                shop_id=6,
                warehouse_id=147
            ),
            meta=create_meta(10, color=BLUE),
        ))])


@pytest.fixture(scope='session')
def api_data_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='session')
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


def test_api_price(
    piper,
    api_data_topic,
    basic_offers_table,
    service_offers_table,
    actual_service_offers_table
):
    """
    piper читает апи цены из топика и корректно создает офферы в таблицах, несмотря на то, что ts_created не передается
    """
    for api in API_DATA:
        request = ChangeOfferRequest()
        request.offer.extend([api])
        api_data_topic.write(request.SerializeToString())

    wait_until(lambda: piper.api_data_processed >= len(API_DATA), timeout=60)

    service_offers_table.load()
    assert_that(len(service_offers_table.data), equal_to(len(EXPECTED_SERVICE_OFFERS)))
    assert_that(service_offers_table.data, HasOffers(EXPECTED_SERVICE_OFFERS))

    actual_service_offers_table.load()
    assert_that(len(actual_service_offers_table.data), equal_to(len(EXPECTED_ACTUAL_SERVICE_OFFERS)))
    assert_that(actual_service_offers_table.data, HasOffers(EXPECTED_ACTUAL_SERVICE_OFFERS))

    basic_offers_table.load()
    assert_that(len(basic_offers_table.data), equal_to(len(EXPECTED_BASIC_OFFERS)))
    assert_that(basic_offers_table.data, HasOffers(EXPECTED_BASIC_OFFERS))
