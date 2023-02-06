# coding: utf-8

import pytest
from hamcrest import assert_that

import market.idx.datacamp.proto.api.ExportMessage_pb2 as EM
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
import market.idx.datacamp.proto.external.Offer_pb2 as EO

from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.pylibrary.datacamp.utils import wait_until


from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampBasicOffersTable, DataCampServiceOffersTable, DataCampPartnersTable
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers

from market.pylibrary.proto_utils import message_from_data


OFFERS = [
    EO.Offer(
        business_id=1,
        offer_id='o1',
        shop_id=10,
        price=EO.OfferPrice(
            currency=1,
            price=100000000000
        ),
        original_content=EO.PartnerOfferContent(
            description="description",
            url="http://some.com/1",
            available=True,
            name="name"
        )
    ),
    EO.Offer(
        business_id=2,
        offer_id='o2',
        shop_id=20,
        price=EO.OfferPrice(
            currency=1,
            price=200000000000
        ),
        original_content=EO.PartnerOfferContent(
            description="description",
            url="http://some.com/2",
            available=True,
            name="name"
        )
    ),
]

SHOPS = [
    {
        'shop_id': 1
    },
    {
        'shop_id': 2
    }
]


@pytest.fixture(scope='module')
def basic_offers_table(yt_server, config):
    return DataCampBasicOffersTable(yt_server, config.yt_basic_offers_tablepath)


@pytest.fixture(scope='module')
def service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_service_offers_tablepath)


@pytest.fixture(scope='module')
def actual_service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(
        yt_server, config.yt_actual_service_offers_tablepath
    )


@pytest.fixture(scope='session')
def sortdc_fast_updates_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='session')
def config(yt_server, log_broker_stuff, sortdc_fast_updates_topic):
    cfg = {
        'general': {
            'batch_size': 10,
        },
        'logbroker': {
            'sortdc_fast_updates_topic': sortdc_fast_updates_topic.topic,
        },
    }
    return PiperConfigMock(yt_server=yt_server, log_broker_stuff=log_broker_stuff, config=cfg)


@pytest.fixture(scope='module')
def partners_table(yt_server, config):
    return DataCampPartnersTable(
        yt_server,
        config.yt_partners_tablepath,
        data=SHOPS,
    )


@pytest.yield_fixture(scope='module')
def piper(
        yt_server,
        log_broker_stuff,
        config,
        sortdc_fast_updates_topic,
        actual_service_offers_table,
        service_offers_table,
        basic_offers_table,
        partners_table
):
    resources = {
        'config': config,
        'sortdc_fast_updates_topic': sortdc_fast_updates_topic,
        'actual_service_offers_table': actual_service_offers_table,
        'service_offers_table': service_offers_table,
        'basic_offers_table': basic_offers_table,
        'partners_table': partners_table,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


@pytest.yield_fixture(scope='module')
def inserter(piper, sortdc_fast_updates_topic):
    for offer in OFFERS:
        msg = EM.ExportMessage(offer=offer)
        sortdc_fast_updates_topic.write(msg.SerializeToString())

    wait_until(lambda: piper.united_offers_processed >= len(OFFERS), timeout=60)


def test_offers_in_basic_table(inserter, piper):
    assert_that(
        piper.basic_offers_table.data,
        HasOffers([message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'o1',
            }
        }, DTC.Offer()),
        message_from_data({
            'identifiers': {
                'business_id': 2,
                'offer_id': 'o2',
                }
            }, DTC.Offer())
        ])
    )
