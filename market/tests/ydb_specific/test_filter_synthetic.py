# coding: utf-8
import pytest

from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.offer.UnitedOffer_pb2 import UnitedOffersBatch, UnitedOffer
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import Offer
from market.idx.datacamp.proto.offer.OfferIdentifiers_pb2 import OfferIdentifiers
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.pylibrary.datacamp.utils import wait_until


TOPIC_NAMES = [
    'quick_pipeline_topic',
    'miner_input_topic',
    'mbock_message_topic',
]


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
        'features': {
            'use_quoter': True
        }
    }
    for name, topic in work_topics.items():
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
    options = {}
    for name, topic in work_topics.items():
        resources[name] = topic
    with PiperTestEnv(yt_server, log_broker_stuff, options, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


def test_build(piper, work_topics):
    datacamp_message = DatacampMessage(
        united_offers=[UnitedOffersBatch(
            offer=[UnitedOffer(
                basic=Offer(
                    identifiers=OfferIdentifiers(
                        business_id=1,
                        offer_id="1",
                        feed_id=1,
                    ),
                ),
                service={
                    101: Offer(
                        identifiers=OfferIdentifiers(
                            business_id=1,
                            offer_id="1",
                            shop_id=101,
                            feed_id=1,
                        ),
                    ),
                },
            )]
        )]
    )
    work_topics['quick_pipeline_topic'].write(datacamp_message.SerializeToString())
    datacamp_message.tech_info.synthetic = True
    work_topics['miner_input_topic'].write(datacamp_message.SerializeToString())
    work_topics['mbock_message_topic'].write(datacamp_message.SerializeToString())

    wait_until(lambda: piper.quoter_datacamp_message_processed == 1, 60)
