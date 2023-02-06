# coding: utf-8

import pytest

from hamcrest import assert_that
from yt.wrapper import ypath_join

from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv
from market.idx.datacamp.yatf.utils import create_meta
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC

from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.yatf.resources.express_table import YtExpressTable

from market.idx.datacamp.proto.offer.UnitedOffer_pb2 import (
    UnitedOffer,
    UnitedOffersBatch,
)
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages
from market.idx.yatf.matchers.protobuf_matchers import IsProtobufMap


@pytest.yield_fixture()
def yt_express_table_path():
    return ypath_join(get_yt_prefix(), 'home', 'market', 'production', 'combinator', 'graph', 'yt_express_warehouse')


@pytest.fixture()
def yt_express_table_data():
    return [
        {
            'warehouse_id': 10,
            'business_id': 10000,
        }, {
            'warehouse_id': 20,
            'business_id': 20000,
        }
    ]


@pytest.fixture(params=[None, False, True])
def is_enabled(request):
    return request.param


@pytest.fixture()
def input_topic(log_broker_stuff, is_enabled):
    return LbkTopic(log_broker_stuff)


@pytest.fixture()
def output_topic(log_broker_stuff, is_enabled):
    return LbkTopic(log_broker_stuff)


@pytest.fixture()
def miner_config(log_broker_stuff, input_topic, output_topic,
                 yt_server, yt_token, yt_express_table_path, is_enabled):
    cfg = MinerConfig()

    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic, united=True)
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic)
    unpacker = cfg.create_proto_unpacker_processor()
    adapter_converter = cfg.create_offer_adapter_converter()
    express_enricher = cfg.create_express_enricher(
        yt_server,
        yt_token,
        yt_express_table_path,
        is_enabled
    )

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, express_enricher)
    cfg.create_link(express_enricher, writer)

    return cfg


@pytest.yield_fixture()
def miner(miner_config, input_topic, output_topic,
          yt_server, yt_express_table_path, yt_express_table_data):
    resources = {
        'miner_cfg': miner_config,
        'input_topic': input_topic,
        'output_topic': output_topic,
        'yt_express_table': YtExpressTable(
            yt_stuff=yt_server,
            path=yt_express_table_path,
            data=yt_express_table_data,
        ),
    }
    with MinerTestEnv(**resources) as miner:
        miner.verify()
        yield miner


def test_expreess_enricher(miner, input_topic, output_topic, is_enabled):
    offers = [
        UnitedOffer(
            basic=DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    offer_id=offer_id,
                ),
            ),
            service={
                321: DTC.Offer(
                    identifiers=DTC.OfferIdentifiers(
                        shop_id=321,
                        offer_id=offer_id,
                        warehouse_id=warehouse_id,
                    ),
                    meta=create_meta(10, DTC.BLUE),
                )
            }
        ) for offer_id, warehouse_id in [
            ('express_offer-{}'.format(str(is_enabled)), 10),
            ('non_express_offer-{}'.format(str(is_enabled)), 30),
        ]
    ]

    request = UnitedOffersBatch()
    request.offer.extend(offers)

    input_topic.write(DatacampMessage(united_offers=[request]).SerializeToString())

    data = output_topic.read(count=1)

    assert_that(data, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [
                {
                    'service': IsProtobufMap({
                        321: {
                            'identifiers': {
                                'offer_id': 'express_offer-{}'.format(str(is_enabled)),
                                'shop_id': 321,
                                'warehouse_id': 10,
                            },
                            'partner_info': {
                                'is_express': True,
                            } if is_enabled else None,
                        }
                    })
                },
                {
                    'service': IsProtobufMap({
                        321: {
                            'identifiers': {
                                'offer_id': 'non_express_offer-{}'.format(str(is_enabled)),
                            },
                            'partner_info': {
                                'is_express': False,
                            } if is_enabled else None,
                        }
                    })
                }
            ]
        }]
    }]))


def test_foodtech_expreess_enricher(miner, input_topic, output_topic, is_enabled):
    offers = [
        UnitedOffer(
            basic=DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    offer_id=offer_id,
                ),
            ),
            service={
                3435: DTC.Offer(
                    identifiers=DTC.OfferIdentifiers(
                        shop_id=3435,
                        offer_id=offer_id,
                        warehouse_id=warehouse_id,
                    ),
                    meta=create_meta(10, color),
                )
            }
        ) for offer_id, warehouse_id, color in [
            ('eda_offer-{}'.format(str(is_enabled)), 123, DTC.EDA),
            ('eda_restaurants_offer-{}'.format(str(is_enabled)), 123, DTC.EDA_RESTAURANTS),
            ('lavka_offer-{}'.format(str(is_enabled)), 123, DTC.LAVKA),
        ]
    ]

    request = UnitedOffersBatch()
    request.offer.extend(offers)

    input_topic.write(DatacampMessage(united_offers=[request]).SerializeToString())

    data = output_topic.read(count=1)

    assert_that(data, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [
                {
                    'service': IsProtobufMap({
                        3435: {
                            'identifiers': {
                                'offer_id': 'eda_offer-{}'.format(str(is_enabled)),
                            },
                            'partner_info': {
                                'is_express': True,
                            } if is_enabled else None,
                        }
                    })
                },
                {
                    'service': IsProtobufMap({
                        3435: {
                            'identifiers': {
                                'offer_id': 'eda_restaurants_offer-{}'.format(str(is_enabled)),
                            },
                            'partner_info': {
                                'is_express': True,
                            } if is_enabled else None,
                        }
                    })
                },
                {
                    'service': IsProtobufMap({
                        3435: {
                            'identifiers': {
                                'offer_id': 'lavka_offer-{}'.format(str(is_enabled)),
                            },
                            'partner_info': {
                                'is_express': True,
                            } if is_enabled else None,
                        }
                    })
                }
            ]
        }]
    }]))
