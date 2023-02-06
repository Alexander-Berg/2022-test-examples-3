# coding: utf-8

import pytest
from hamcrest import assert_that

from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv

from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobufMap

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.offer.UnitedOffer_pb2 import UnitedOffer, UnitedOffersBatch
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from google.protobuf.timestamp_pb2 import Timestamp


DATACAMP_MESSAGES = [
    DatacampMessage(
        united_offers=[UnitedOffersBatch(
            offer=[UnitedOffer(
                basic=DTC.Offer(
                    identifiers=DTC.OfferIdentifiers(
                        business_id=1,
                        offer_id='o1',
                    )
                ),
                service={
                    1: DTC.Offer(
                        identifiers=DTC.OfferIdentifiers(
                            business_id=1,
                            offer_id='o1',
                            shop_id=1,
                            warehouse_id=1,
                        ),
                        meta=DTC.OfferMeta(
                            rgb=DTC.BLUE,
                        ),
                        promos=DTC.OfferPromos(
                            anaplan_promos=DTC.MarketPromos(
                                all_promos=DTC.Promos(
                                    promos=[
                                        DTC.Promo(
                                            id='1'
                                        )
                                    ]
                                ),
                                active_promos=DTC.Promos(
                                    promos=[
                                        DTC.Promo(
                                            id='1'
                                        ),
                                        DTC.Promo(
                                            id='2'
                                        )
                                    ],
                                    meta=DTC.UpdateMeta(
                                        timestamp=Timestamp(
                                            nanos=1
                                        )
                                    )
                                )
                            )
                        ),
                    )
                }
            )]
        )]
    )
]


@pytest.fixture(scope='module')
def miner_config(log_broker_stuff, input_topic, output_topic):
    cfg = MinerConfig()

    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic, united=True)
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic)
    unpacker = cfg.create_proto_unpacker_processor()
    adapter_converter = cfg.create_offer_adapter_converter()
    promo_cleaner = cfg.create_promo_cleaner_processor()

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, promo_cleaner)
    cfg.create_link(promo_cleaner, writer)

    return cfg


@pytest.yield_fixture(scope='module')
def miner(miner_config, input_topic, output_topic):

    resources = {
        'miner_cfg': miner_config,
        'input_topic': input_topic,
        'output_topic': output_topic,
    }
    with MinerTestEnv(**resources) as miner:
        miner.verify()
        yield miner


def write_read_offer_lbk(input_topic, output_topic):
    for m in DATACAMP_MESSAGES:
        input_topic.write(m.SerializeToString())

    return output_topic.read(len(DATACAMP_MESSAGES))


def test_remove_active_promos_not_included_in_all_promos(miner, input_topic, output_topic):
    data = write_read_offer_lbk(input_topic, output_topic)

    assert_that(data[0], IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': 1,
                        'offer_id': 'o1',
                    }
                },
                'service': IsProtobufMap({
                    1: {
                        'identifiers': {
                            'business_id': 1,
                            'offer_id': 'o1',
                            'shop_id': 1,
                            'warehouse_id': 1,
                        },
                        'promos': {
                            'anaplan_promos': {
                                'all_promos': {
                                    'promos': [
                                        {
                                            'id': '1'
                                        }
                                    ]
                                },
                                'active_promos': {
                                    'promos': [
                                        {
                                            'id': '1'
                                        }
                                    ],
                                    'meta': {
                                        'timestamp': {
                                            'nanos': 2
                                        }
                                    }
                                }
                            }
                        }
                    },
                })
            }]
        }]
    }))
