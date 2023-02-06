# coding: utf-8

from hamcrest import assert_that, equal_to
import pytest
from datetime import datetime

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.api.SyncChangeOffer_pb2 import ChangeOfferRequest
from market.idx.datacamp.controllers.piper.yatf.test_env_new import PiperTestEnv
from market.idx.datacamp.controllers.piper.yatf.resources.config import PiperConfig
from market.idx.pylibrary.datacamp.conversion import offer_to_service_row, offer_to_basic_row

from market.idx.yatf.resources.yt_token_resource import YtTokenResource
from market.idx.yatf.resources.lbk_topic import LbkTopic

from market.idx.yatf.matchers.yt_rows_matchers import HasOffers

from market.idx.datacamp.yatf.utils import dict2tskv
from market.idx.pylibrary.datacamp.utils import wait_until
from market.pylibrary.proto_utils import message_from_data


NOW_UTC = datetime.utcnow()


def make_offer_data(
    business_id,
    offer_id,
    shop_id=None,
    warehouse_id=None,
    scope=None,
    source=None,
    stock_count=None,
):
    offer = {
        'identifiers': {
            'business_id': business_id,
            'offer_id': offer_id,
            'shop_id': shop_id,
            'warehouse_id': warehouse_id
        },
        'meta': {
            'scope': scope
        },
        'stock_info': {
            'partner_stocks': {
                'count': stock_count,
                'meta': {
                    'timestamp': NOW_UTC.strftime("%Y-%m-%dT%H:%M:%SZ"),
                    'source': source
                }
            }
        } if stock_count is not None else None
    }
    return message_from_data(offer, DTC.Offer())


API_DATA = [
    make_offer_data(
        business_id=None,  # Бизнес не передается, должен проставиться в color_preprocessor
        offer_id='offer.with.push.api.stocks',
        shop_id=12,
        warehouse_id=12345,
        source=DTC.MARKET_API_STOCK,
        stock_count=1
    ),
    make_offer_data(
        business_id=None,
        offer_id='offer.with.api.stocks',
        shop_id=12,
        warehouse_id=12345,
        source=DTC.PUSH_PARTNER_API,
        stock_count=1
    ),
    make_offer_data(
        business_id=None,
        offer_id='offer.with.office.stocks',
        shop_id=12,
        warehouse_id=12345,
        source=DTC.PUSH_PARTNER_OFFICE,
        stock_count=1
    ),
]


@pytest.fixture(scope='module')
def partners_table_data():
    return [{
        'shop_id': 12,
        'mbi': '\n\n'.join([
            dict2tskv({
                'shop_id': 12,
                'business_id': 1,
            }),
        ]),
    }]


@pytest.fixture(scope='module')
def basic_offers_table_data():
    return [
        offer_to_basic_row(make_offer_data(1, 'offer.with.push.api.stocks', scope=DTC.BASIC)),
        offer_to_basic_row(make_offer_data(1, 'offer.with.api.stocks', scope=DTC.BASIC)),
        offer_to_basic_row(make_offer_data(1, 'offer.with.office.stocks', scope=DTC.BASIC)),
    ]


@pytest.fixture(scope='module')
def service_offers_table_data():
    return [
        offer_to_service_row(make_offer_data(1, 'offer.with.push.api.stocks', 12, scope=DTC.SERVICE)),
        offer_to_service_row(make_offer_data(1, 'offer.with.api.stocks', 12, scope=DTC.SERVICE)),
        offer_to_service_row(make_offer_data(1, 'offer.with.office.stocks', 12, scope=DTC.SERVICE)),
    ]


@pytest.fixture(scope='module')
def actual_service_offers_table_data():
    return [
        offer_to_service_row(make_offer_data(1, 'offer.with.push.api.stocks', 12, warehouse_id=12345, scope=DTC.SERVICE)),
        offer_to_service_row(make_offer_data(1, 'offer.with.api.stocks', 12, warehouse_id=12345, scope=DTC.SERVICE)),
        offer_to_service_row(make_offer_data(1, 'offer.with.office.stocks', 12, warehouse_id=12345, scope=DTC.SERVICE)),
    ]


@pytest.fixture(scope='module')
def api_data_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, api_data_topic):
    cfg = PiperConfig(log_broker_stuff, YtTokenResource().path, yt_server, 'white')
    cfg.update_properties({
        'yt_server': yt_server,
        'lb_host': log_broker_stuff.host,
        'lb_port': log_broker_stuff.port,
    })

    cfg.create_initializer()
    lb_reader = cfg.create_lb_reader(api_data_topic.topic, **{
        'MessageType': 'ApiDataBatch',
        'MaxCount': '10'
    })
    api_data = cfg.create_processor('API_DATA_PROCESSOR')
    color_preprocessor = cfg.create_processor('COLOR_PREPROCESSOR', **{
        'FillBusinessId': 'true',
    })
    legacy_offers_converter = cfg.create_processor('LEGACY_OFFERS_CONVERTER')
    united_offers_subscribers_gateway = cfg.create_input_gateway()
    united_updater = cfg.create_united_updater(united_offers_subscribers_gateway)

    cfg.create_links([
        (lb_reader, api_data),
        (api_data, color_preprocessor),
        (color_preprocessor, legacy_offers_converter),
        (legacy_offers_converter, united_updater),
    ])

    return cfg


@pytest.yield_fixture(scope='module')
def piper(
    yt_server,
    log_broker_stuff,
    config,
    api_data_topic,
    partners_table,
    basic_offers_table,
    service_offers_table,
    actual_service_offers_table,
):
    resources = {
        'piper_config': config,
        'api_data_topic': api_data_topic,
        'basic_offers': basic_offers_table,
        'service_offers': service_offers_table,
        'actual_service_offers': actual_service_offers_table,
        'partners': partners_table,
    }
    options = {
    }
    with PiperTestEnv(yt_server, log_broker_stuff, options, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


@pytest.yield_fixture(scope='module')
def inserter(piper, api_data_topic):
    for api in API_DATA:
        request = ChangeOfferRequest()
        request.offer.extend([api])
        api_data_topic.write(request.SerializeToString())

    wait_until(lambda: piper.united_offers_processed >= 3, timeout=60)


def test_offers_count(
    piper,
    inserter,
    basic_offers_table,
    service_offers_table,
    actual_service_offers_table
):
    """ Не было создано ничего лишнего с неверными business_id """
    assert_that(len(piper.service_offers_table.data), equal_to(3))
    assert_that(len(piper.actual_service_offers_table.data), equal_to(3))
    assert_that(len(piper.basic_offers_table.data), equal_to(3))


@pytest.mark.parametrize("offer_id, source", [
    ('offer.with.push.api.stocks', DTC.MARKET_API_STOCK),
    ('offer.with.api.stocks', DTC.PUSH_PARTNER_API),
    ('offer.with.office.stocks', DTC.PUSH_PARTNER_OFFICE)
])
def test_api_stocks(
    piper,
    inserter,
    actual_service_offers_table,
    offer_id,
    source
):
    """ Источники MARKET_API_STOCK, PUSH_PARTNER_API, PUSH_PARTNER_OFFICE могут обновлять значение партнерского стока
    """
    assert_that(piper.actual_service_offers_table.data, HasOffers([make_offer_data(
        business_id=1,
        offer_id=offer_id,
        shop_id=12,
        warehouse_id=12345,
        source=source,
        stock_count=1
    )]))
