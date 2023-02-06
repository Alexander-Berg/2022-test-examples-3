# coding: utf-8

import pytest
from hamcrest import assert_that, has_entries, has_items

from market.idx.datacamp.controllers.piper.yatf.resources.config import PiperConfig
from market.idx.datacamp.controllers.piper.yatf.test_env_new import PiperTestEnv
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.offer.UnitedOffer_pb2 import (
    ActualOffers,
    UnitedOffer,
    UnitedOffersBatch,
)
from market.idx.pylibrary.datacamp.conversion import offer_to_service_row, offer_to_basic_row
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.datacamp.yatf.matchers.matchers import IsSerializedProtobuf
from market.idx.datacamp.yatf.utils import create_meta, create_update_meta
from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampBasicOffersTable,
    DataCampServiceOffersTable,
)
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.resources.yt_token_resource import YtTokenResource
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
import market.proto.ir.UltraController_pb2 as UC


BASIC_TABLE_DATA = [
    offer_to_basic_row(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='T1000', warehouse_id=145),
            meta=create_meta(10, scope=DTC.BASIC),
            content=DTC.OfferContent(
                partner=DTC.PartnerContent(
                    actual=DTC.ProcessedSpecification(
                        title=DTC.StringValue(value='offer')
                    )
                )
            ),
        )
    ),
]

SERVICE_TABLE_DATA = [
    offer_to_service_row(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=1, offer_id=offer_id, shop_id=1, feed_id=1, warehouse_id=145),
            meta=create_meta(10, scope=DTC.SERVICE, color=DTC.WHITE),
            price=DTC.OfferPrice(
                basic=DTC.PriceBundle(
                    binary_price=DTC.PriceExpression(
                        id='RUR',
                        price=1000,
                    ),
                )
            ),
        )
    )
    for offer_id in ('T1000', 'T2000', 'T3000')
]

ACTUAL_SERVICE_TABLE_DATA = [
    offer_to_service_row(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=1, offer_id=offer_id, shop_id=1, feed_id=1, warehouse_id=145),
            meta=create_meta(10, scope=DTC.SERVICE, color=DTC.WHITE),
            price=DTC.OfferPrice(
                basic=DTC.PriceBundle(
                    binary_price=DTC.PriceExpression(
                        id='RUR',
                        price=1000,
                    ),
                )
            ),
        )
    )
    for offer_id in ('T1000', 'T2000', 'T3000')
]


def create_datacamp_message(basic, service, actual):
    return DatacampMessage(united_offers=[UnitedOffersBatch(offer=[
        UnitedOffer(
            basic=basic,
            service={1: service},
            actual={1: ActualOffers(warehouse={145: actual})}
        )
    ])])


CONTENT = DTC.OfferContent(
    market=DTC.MarketContent(
        meta=create_update_meta(10),
        enriched_offer=UC.EnrichedOffer(
            category_id=8475840,
            model_id=123456789,
            matched_id=111,
        ),
        ir_data=DTC.EnrichedOfferSubset(
            matched_id=111,
        ),
    ),
    partner=DTC.PartnerContent(
        actual=DTC.ProcessedSpecification(
            title=DTC.StringValue(value='offer')
        )
    ),
)

UPDATED_OFFER_MESSAGES = [
    create_datacamp_message(
        basic=DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='T1000'),
            content=CONTENT,
        ),
        service=DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='T1000', shop_id=1, feed_id=1, warehouse_id=145),
            meta=create_meta(10, scope=DTC.SERVICE, color=DTC.WHITE),
        ),
        actual=DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='T1000', shop_id=1, feed_id=1, warehouse_id=145),
            meta=create_meta(10, scope=DTC.SERVICE, color=DTC.WHITE),
            status=DTC.OfferStatus(
                ready_for_publication=DTC.ReadinessForPublicationStatus(
                    value=DTC.ReadinessForPublicationStatus.READY
                ),
                # чтобы не проставился статус готовности к публикации NOT_READY
                disabled=[
                    DTC.Flag(
                        flag=False,
                        meta=create_update_meta(10, source=DTC.MARKET_IDX)
                    ),
                ]
            ),
        )
    ),
]


@pytest.fixture(scope='module')
def basic_offers_table(yt_server, config):
    return DataCampBasicOffersTable(
        yt_server, config.yt_table_path('basic_offers'), data=BASIC_TABLE_DATA
    )


@pytest.fixture(scope='module')
def service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(
        yt_server, config.yt_table_path('service_offers'), data=SERVICE_TABLE_DATA
    )


@pytest.fixture(scope='module')
def actual_service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(
        yt_server, config.yt_table_path('actual_service_offers'), data=ACTUAL_SERVICE_TABLE_DATA
    )


@pytest.fixture(scope='session')
def datacamp_messages_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='session')
def config(yt_server, log_broker_stuff, datacamp_messages_topic):
    cfg = PiperConfig(log_broker_stuff, YtTokenResource().path, yt_server, 'white')
    cfg.update_properties({
        'yt_server': yt_server,
        'lb_host': log_broker_stuff.host,
        'lb_port': log_broker_stuff.port,
    })

    cfg.create_initializer()
    lb_reader = cfg.create_lb_reader(datacamp_messages_topic.topic, **{
        'MaxCount': '10'
    })
    datacamp_unpacker = cfg.create_processor('DATACAMP_MESSAGE_UNPACKER')
    offer_format_validator = cfg.create_offer_format_validator()
    united_offers_subscribers_gateway = cfg.create_input_gateway()
    united_updater = cfg.create_united_updater(united_offers_subscribers_gateway)
    fresh_offers_sender = cfg.create_fresh_offers_sender()

    cfg.create_links([
        (lb_reader, datacamp_unpacker),
        (datacamp_unpacker, offer_format_validator),
        (offer_format_validator, united_updater),
        (united_offers_subscribers_gateway, fresh_offers_sender),
    ])

    return cfg


@pytest.yield_fixture(scope='module')
def piper(
        yt_server,
        log_broker_stuff,
        config,
        datacamp_messages_topic,
        actual_service_offers_table,
        service_offers_table,
        basic_offers_table,
):
    resources = {
        'piper_config': config,
        'basic_offers_table': basic_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'service_offers_table': service_offers_table,
        'datacamp_messages_topic': datacamp_messages_topic,
    }
    options = {
        'tables': ['partners', 'fresh/white', 'fresh/blue'],
    }
    with PiperTestEnv(yt_server, log_broker_stuff, options, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


def test_fresh(piper, datacamp_messages_topic):
    for message in UPDATED_OFFER_MESSAGES:
        datacamp_messages_topic.write(message.SerializeToString())

    wait_until(lambda: piper.fresh_offers_processed > 0, timeout=60)

    assert_that(
        piper.fresh_white_offers_table.data + piper.fresh_blue_offers_table.data,
        has_items(
            has_entries(
                {
                    'business_id': 1,
                    'offer_id': 'T1000',
                    'shop_id': 1,
                    'warehouse_id': 145,
                    'outlet_id': 0,
                    'offer': IsSerializedProtobuf(DTC.Offer, {
                        'content': {
                            'market': {
                                'enriched_offer': {
                                    'category_id': 8475840,
                                    'matched_id': 111,
                                    'model_id': 123456789,
                                },
                            },
                        },
                    }),
                },
            )
        ),
        "fresh offer has been stored"
    )
