# coding: utf-8

import pytest
from hamcrest import assert_that, is_not, has_entries, has_items

import yt.wrapper as yt
from market.idx.datacamp.routines.yatf.test_env import UnitedDatacampDumperEnv
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
import market.idx.datacamp.proto.external.Offer_pb2 as EO
import robot.rthub.yql.protos.queries_pb2 as Queries
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampPartnersTable
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.datacamp.yatf.utils import dict2tskv
from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampBasicOffersTable,
    DataCampServiceOffersTable
)

BUSINESS_ID_DIRECT = 1000
SHOP_DIRECT = 2000
FEED_DIRECT = 3000
OFFER_DIRECT = 'direct_offer_1'

OFFERS = [
    EO.Offer(
        business_id=BUSINESS_ID_DIRECT,
        offer_id='no_url',
        shop_id=SHOP_DIRECT,
        feed_id=FEED_DIRECT,
        original_pictures=[
            DTC.SourcePicture(
                source="DIRECT_LINK",
                url="http://some.com/2.png"
            )
        ],
        price=EO.OfferPrice(
            currency=1,
            price=523400000000
        ),
        original_content=EO.PartnerOfferContent(
            description="no url offer",
            available=True,
            name="name"
        )
    ),
    EO.Offer(
        business_id=BUSINESS_ID_DIRECT,
        offer_id='direct_offer_1',
        shop_id=SHOP_DIRECT,
        feed_id=FEED_DIRECT,
        original_pictures=[
            DTC.SourcePicture(
                source="DIRECT_LINK",
                url="http://some.com/1.png"
            )
        ],
        price=EO.OfferPrice(
            currency=1,
            price=123400000000
        ),
        original_content=EO.PartnerOfferContent(
            description="description",
            url="http://some.com/1",
            available=True,
            name="name"
        )
    )
]


@pytest.fixture(scope='session')
def piper_config(yt_server, log_broker_stuff, external_in_message_topic):
    config = PiperConfigMock(
        yt_server=yt_server,
        log_broker_stuff=log_broker_stuff,
        config={
            'general': {
                'color': 'direct_goods_ads',
                'batch_size': 10,
                'yt_home': '//home/datacamp/united',
            },
            'logbroker': {
                'external_in_message_topic': external_in_message_topic.topic,
            },
        }
    )
    return config


@pytest.fixture(scope='session')
def routines_config(yt_server):
    config = RoutinesConfigMock(
        yt_server,
        config={
            'general': {
                'color': 'direct_goods_ads',
                'yt_home': '//home/datacamp/united'
            },
            'routines': {
                'enable_united_datacamp_dumper': True,
                'days_number_to_take_disabled_offer_in_index': 5,
                'enable_united_datacamp_export_dumper': True,
            },
            'yt': {
                'white_out': 'white_out',
                'blue_out': 'blue_out',
                'direct_out': 'direct_out',
                'turbo_out': 'turbo_out',
                'blue_turbo_out': 'blue_turbo_out',
                'eda_out': 'eda_out',
                'lavka_out': 'lavka_out',
                'map_reduce_proxies': [yt_server.get_yt_client().config["proxy"]["url"]],
                'export_dir': '//home/ecom/offers'
            }
        })
    return config


@pytest.fixture(scope='session')
def partners_table(yt_server, routines_config):
    data = [
        {
            'shop_id': SHOP_DIRECT,
            'mbi': '\n\n'.join([
                dict2tskv({'shop_id': SHOP_DIRECT, 'business_id': BUSINESS_ID_DIRECT}),
            ]),
        },
    ]
    return DataCampPartnersTable(
        yt_server,
        routines_config.yt_partners_tablepath,
        data
    )


@pytest.fixture(scope='session')
def basic_offers_table(yt_server, piper_config):
    return DataCampBasicOffersTable(
        yt_server, piper_config.yt_basic_offers_tablepath,
    )


@pytest.fixture(scope='session')
def service_offers_table(yt_server, piper_config):
    return DataCampServiceOffersTable(
        yt_server, piper_config.yt_service_offers_tablepath,
    )


@pytest.fixture(scope='session')
def actual_service_offers_table(yt_server, piper_config):
    return DataCampServiceOffersTable(
        yt_server, piper_config.yt_actual_service_offers_tablepath,
    )


@pytest.fixture(scope='session')
def external_in_message_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.yield_fixture(scope='session')
def piper(
    yt_server,
    log_broker_stuff,
    piper_config,
    partners_table,
    external_in_message_topic,
    actual_service_offers_table,
    service_offers_table,
    basic_offers_table,
):
    resources = {
        'config': piper_config,
        'external_in_message_topic' : external_in_message_topic,
        'actual_service_offers_table': actual_service_offers_table,
        'service_offers_table': service_offers_table,
        'basic_offers_table': basic_offers_table,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


def create_rthub_message(offer):
    return Queries.TOfferParserItem(SerializedOffer=offer.SerializeToString())


@pytest.yield_fixture(scope='session')
def inserter(piper, external_in_message_topic):
    for external_offer in OFFERS:
        msg = create_rthub_message(external_offer)
        external_in_message_topic.write(msg.SerializeToString())

    # minus 1, because no_url offer is discarded (MARKETINDEXER-38272)
    wait_until(lambda: piper.united_offers_processed >= len(OFFERS) - 1, timeout=60)


@pytest.yield_fixture(scope='session')
def routines(
    yt_server,
    piper_config,
    routines_config,
    partners_table,
    basic_offers_table,
    service_offers_table,
    actual_service_offers_table,
):

    resources = {
        'partners_table': partners_table,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'config': routines_config,
    }
    with UnitedDatacampDumperEnv(yt_server, **resources) as routines_env:
        yield routines_env


# TODO: piper возвращает цвет 'direct_goods_ads' (https://st.yandex-team.ru/MARKETINDEXER-41260),
# routines прнимает на вход цвет 'direct' и 'direct_search_snippet_gallery'.
# Нужно сделать так, чтобы цвет был одинаковый.
def test_piper_and_dumper(yt_server, piper, piper_config, inserter, routines):
    yt_client = yt_server.get_yt_client()
    output_dir = '//home/datacamp/united/direct_out'
    results = list(yt_client.read_table(yt.ypath_join(output_dir, 'recent')))
    # assert_that(results, has_items(
    #     has_entries({
    #         'business_id': BUSINESS_ID_DIRECT,
    #         'offer_id': 'direct_offer_1',
    #     })
    # ))
    assert_that(results, is_not(has_items(
        has_entries({
            'business_id': BUSINESS_ID_DIRECT,
            'offer_id': 'no_url',
        }))
    ))
