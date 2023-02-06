# coding: utf-8

import json
import pytest

from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.picrobot.proto.event_pb2 import TImageResponse, TOffer
from market.idx.datacamp.picrobot.proto.mds_info_pb2 import TMdsInfo, TMdsId
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.api.ExportMessage_pb2 import ExportMessage
from market.idx.datacamp.proto.api.SyncChangeOffer_pb2 import ChangeOfferRequest
from market.idx.datacamp.proto.common.Types_pb2 import Currency
from market.idx.datacamp.proto.external.Offer_pb2 import Offer as ExternalOffer, IdentifiedPrice, OfferPrice
from market.idx.datacamp.proto.offer.TechCommands_pb2 import TechCommand
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import (
    OfferIdentifiers, OfferScope,
    PUSH_PARTNER_API,
    BLUE,
    Offer as DatacampOffer,
)
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.datacamp.yatf.utils import create_meta, create_api_price
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampBasicOffersTable,
    DataCampServiceOffersTable
)
from market.pylibrary.proto_utils import message_from_data
import robot.rthub.yql.protos.queries_pb2 as Queries

TOPIC_NAMES = [
    'api_data_topic',
    'datacamp_message_topic',
    'quick_pipeline_topic',
    'miner_input_topic',
    'mbock_message_topic',
    'picrobot_response_topic',
    'rthub_message_topic',
    'vertical_rthub_topic',
    'direct_moderation_topic'
]


DATACAMP_MESSAGE = {
    'united_offers': [{
        'offer': [{
            'basic': {
                'identifiers': {
                    'business_id': 1,
                    'offer_id': "1",
                    'feed_id': 1
                },
            },
            'service': {
                101: {
                    'identifiers': {
                        'business_id': 1,
                        'offer_id': "1",
                        'shop_id': 101,
                        'feed_id': 1
                    },
                }
            }
        }]
    }]
}


EXPORT_MESSAGE = ExportMessage(
    offer=ExternalOffer(
        business_id=1,
        offer_id="1",
        shop_prices=[
            IdentifiedPrice(
                shop_id=1,
                price=OfferPrice(price=1, currency=Currency.RUR),
            )
        ],
    )
)

DIRECT_MESSAGE = {
    'meta': {
        'business_id': 2,
        'shop_id': 1,
        'version': '2',
        'client_id': 11,
        'offer_yabs_id': '123445',
        'original_b2b_offer_id': 'T3000'
    },
    'result': {
        'verdict': 0,
        'reasons': [1, 2, 3, 4, 5],
        'flags': [6, 501, 502],
        'minus_regions': [8, 9, 10],
        'timestamp': 90000
    }
}


def get_image_response():
    offer_identifiers = OfferIdentifiers(business_id=1, offer_id='1')
    offer = DatacampOffer(identifiers=offer_identifiers)

    return TImageResponse(
        Url='https://original.url/',
        MdsInfo=TMdsInfo(
            MdsId=TMdsId(
                Namespace='namespace',
            )
        ),
        Offer=TOffer(OfferId=offer_identifiers.SerializeToString(), Context=offer.SerializeToString()),)


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


@pytest.fixture(scope='module')
def basic_offers_table(yt_server, config):
    return DataCampBasicOffersTable(yt_server, config.yt_basic_offers_tablepath)


@pytest.fixture(scope='module')
def service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_service_offers_tablepath)


@pytest.fixture(scope='module')
def actual_service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_actual_service_offers_tablepath)


@pytest.fixture(scope='session')
def work_topics(log_broker_stuff):
    topics = {}
    for topic in TOPIC_NAMES:
        topics[topic] = LbkTopic(log_broker_stuff)
    return topics


@pytest.fixture(scope='session')
def config(yt_server, log_broker_stuff, work_topics):
    cfg = {
        'general': {
            'color': 'blue',
        },
        'logbroker': {
        },
        'picrobot': {
            'response_topic': work_topics['picrobot_response_topic'].topic
        },
        'features': {
            'use_quoter': True
        }
    }
    for name, topic in work_topics.items():
        print('lbk_test {} {}'.format(name, topic.topic))
        cfg['logbroker'][name] = topic.topic

    return PiperConfigMock(yt_server=yt_server,
                           log_broker_stuff=log_broker_stuff,
                           config=cfg)


@pytest.yield_fixture(scope='module')
def piper(
    yt_server,
    log_broker_stuff,
    config,
    basic_offers_table,
    service_offers_table,
    actual_service_offers_table,
    work_topics
):
    resources = {
        'config': config,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
    }
    for name, topic in work_topics.items():
        resources[name] = topic
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


def test_build(piper, work_topics):
    request = ChangeOfferRequest()
    request.offer.extend([make_api_data(1, 'T1000', 10, 20, 100)])
    work_topics['api_data_topic'].write(request.SerializeToString())
    work_topics['datacamp_message_topic'].write(DatacampMessage(tech_command=[TechCommand()]).SerializeToString())

    message = message_from_data(DATACAMP_MESSAGE, DatacampMessage()).SerializeToString()
    work_topics['quick_pipeline_topic'].write(message)
    work_topics['miner_input_topic'].write(message)
    work_topics['mbock_message_topic'].write(message)
    work_topics['picrobot_response_topic'].write(get_image_response().SerializeToString())
    work_topics['rthub_message_topic'].write(EXPORT_MESSAGE.SerializeToString())
    rthub_message = Queries.TOfferParserItem(SerializedOffer=EXPORT_MESSAGE.SerializeToString()).SerializeToString()
    work_topics['rthub_message_topic'].write(rthub_message)
    work_topics['direct_moderation_topic'].write(json.dumps(DIRECT_MESSAGE))

    wait_until(lambda: piper.quoter_api_processed == 1, 60)
    wait_until(lambda: piper.quoter_datacamp_message_processed == 5, 60)
    wait_until(lambda: piper.quoter_external_message_processed == 2, 60)
    wait_until(lambda: piper.quoter_united_offer_processed == 1, 60)
