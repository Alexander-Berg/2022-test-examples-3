# coding: utf-8

import pytest
from hamcrest import assert_that

from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv
from market.idx.datacamp.yatf.utils import create_meta

from market.idx.datacamp.proto.offer import DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.offer.UnitedOffer_pb2 import (
    UnitedOffer,
    UnitedOffersBatch,
)
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages
from market.idx.yatf.matchers.protobuf_matchers import IsProtobufMap
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage


@pytest.fixture(scope='module')
def miner_config(log_broker_stuff, input_topic, output_topic):
    cfg = MinerConfig()

    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic, united=True)
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic)
    unpacker = cfg.create_proto_unpacker_processor()
    adapter_converter = cfg.create_offer_adapter_converter()
    ware_md5_creator = cfg.create_ware_md5_creator()

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, ware_md5_creator)
    cfg.create_link(ware_md5_creator, writer)

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


def test_ware_md5_miner(miner, input_topic, output_topic):
    """Проверяем, что miner проставляется ware_md5 в оффер
    """
    offer = UnitedOffer(
        basic=DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                offer_id='00065.00008.2563456245626',
            ),
            content=DTC.OfferContent(
                binding=DTC.ContentBinding(
                    partner=DTC.Mapping(
                        market_sku_id=100126173307,
                    )
                )
            )
        ),
        service={
            10264169: DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    offer_id='00065.00008.2563456245626',
                    shop_id=10264169,
                    feed_id=200396944,
                    warehouse_id=172,
                ),
                meta=create_meta(10, DTC.BLUE),
            )
        }
    )

    request = UnitedOffersBatch()
    request.offer.extend([offer])

    input_topic.write(DatacampMessage(united_offers=[request]).SerializeToString())

    data = output_topic.read(count=1)

    assert_that(data, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'offer_id': '00065.00008.2563456245626',
                    },
                },
                'service': IsProtobufMap({
                    10264169: {
                        'identifiers': {
                            'shop_id': 10264169,
                            'offer_id': '00065.00008.2563456245626',
                            'warehouse_id': 172,
                            'feed_id': 200396944,
                            'extra': {
                                'ware_md5': 'LTb0b0kAB9sXXYgQJd6_SQ',
                            }
                        },
                    }
                })
            }]
        }]
    }]))
