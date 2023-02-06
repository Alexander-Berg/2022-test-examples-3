# coding: utf-8

import pytest
from hamcrest import assert_that, is_not

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
import market.idx.datacamp.proto.external.Offer_pb2 as EO
import market.idx.datacamp.proto.offer.OfferMeta_pb2 as OM
import market.idx.datacamp.proto.tables.Partner_pb2 as Partner
import robot.rthub.yql.protos.queries_pb2 as Queries

from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.pylibrary.datacamp.utils import wait_until


from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampBasicOffersTable, DataCampServiceOffersTable, DataCampPartnersTable
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.yatf.matchers.yt_rows_matchers import HasDatacampPartersYtRows, HasOffers

from market.pylibrary.proto_utils import message_from_data


OFFERS = [
    EO.Offer(
        business_id=1,
        offer_id='no_url',
        shop_id=1,
        feed_id=1,
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
        business_id=1,
        business_ids=[1, 2],
        offer_id='o1',
        shop_id=10,
        shop_ids=[10, 20],
        feed_id=1,
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
    ),
    EO.Offer(
        business_id=2,
        offer_id='redirect_resolve1',
        shop_id=2,
        feed_id=2,
        original_content=EO.PartnerOfferContent(
            url="http://targethost.com/targetpath1",
            name="name"
        ),
        original_url="http://originalhost.com/originalpath1",
        service=EO.Service(
            data_source=OM.REDIRECT_RESOLVER
        ),
    )
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
def external_in_message_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='session')
def config(yt_server, log_broker_stuff, external_in_message_topic):
    cfg = {
        'general': {
            'color': 'white',
            'batch_size': 10,
        },
        'logbroker': {
            'external_in_message_topic': external_in_message_topic.topic,
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
        external_in_message_topic,
        actual_service_offers_table,
        service_offers_table,
        basic_offers_table,
        partners_table
):
    resources = {
        'config': config,
        'external_in_message_topic': external_in_message_topic,
        'actual_service_offers_table': actual_service_offers_table,
        'service_offers_table': service_offers_table,
        'basic_offers_table': basic_offers_table,
        'partners_table': partners_table,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


def create_rthub_message(offer):
    return Queries.TOfferParserItem(SerializedOffer=offer.SerializeToString())


@pytest.yield_fixture(scope='module')
def inserter(piper, external_in_message_topic):
    for external_offer in OFFERS:
        msg = create_rthub_message(external_offer)
        external_in_message_topic.write(msg.SerializeToString())

    # minus 1, becouse no_url offer is discarded (MARKETINDEXER-38272)
    wait_until(lambda: piper.united_offers_processed >= len(OFFERS) - 1, timeout=60)


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
                'offer_id': 'o1',
                }
            }, DTC.Offer())
        ])
    )


def test_direct_offers_in_service_table(inserter, piper):
    assert_that(
        piper.service_offers_table.data,
        HasOffers([message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'o1',
                'shop_id': 10
            },
            'meta': {
                'rgb': 'DIRECT_GOODS_ADS',
                'platforms': {
                    DTC.DIRECT_GOODS_ADS: True
                },
                'data_source': 'PUSH_PARTNER_SITE',
            }
        }, DTC.Offer()),
        message_from_data({
            'identifiers': {
                'business_id': 2,
                'offer_id': 'o1',
                'shop_id': 20
            },
            'meta': {
                'rgb': 'DIRECT_GOODS_ADS',
                'platforms': {
                    DTC.DIRECT_GOODS_ADS: True
                },
                'data_source': 'PUSH_PARTNER_SITE',
            }
        }, DTC.Offer())])
    )


def test_no_url_offer_in_basic_table(inserter, piper):
    assert_that(
        piper.basic_offers_table.data,
        is_not(HasOffers([message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'no_url',
            },
        }, DTC.Offer())]))
    )


def test_redirect_resolver(inserter, piper):
    assert_that(
        piper.partners_table.data,
        HasDatacampPartersYtRows([
            {
                'shop_id': 2,
                'resolved_redirect_info': IsSerializedProtobuf(Partner.ResolvedRedirectInfo, {
                    'items': [
                        {
                            'original_host': 'http://originalhost.com',
                            'target_host': 'http://targethost.com'
                        }
                    ]
                })
            }
        ])
    )
