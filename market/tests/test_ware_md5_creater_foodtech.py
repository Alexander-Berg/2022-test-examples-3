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


OFFERS = [
    UnitedOffer(
        basic=DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='lavka',
            ),
        ),
        service={
            10264169: DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=1,
                    shop_id=10264169,
                    offer_id='lavka',
                    warehouse_id=172,
                    feed_id=200396944,
                ),
                meta=create_meta(10, DTC.LAVKA),
            ),
        }
    ),
    UnitedOffer(
        basic=DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='eda',
            ),
        ),
        service={
            10264169: DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=1,
                    shop_id=10264169,
                    offer_id='eda',
                    warehouse_id=172,
                    feed_id=200396944,
                ),
                meta=create_meta(10, DTC.EDA),
            ),
        }
    ),
    UnitedOffer(
        basic=DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='eda with ware md5',
            ),
        ),
        service={
            10264169: DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=1,
                    shop_id=10264169,
                    offer_id='eda with ware md5',
                    warehouse_id=172,
                    feed_id=200396944,
                    extra={
                        'ware_md5': 'custom-ware_md5'
                    }
                ),
                meta=create_meta(10, DTC.EDA),
            ),
        }
    ),
]

EXPECTED_WARE_MD5 = ['euSy4IGIGp_kSUd6Mhe_bA', 'iqJhVlpzqO_04OrK3ets6g', 'custom-ware_md5']


@pytest.fixture(scope='module')
def miner_config(log_broker_stuff, input_topic, output_topic):
    cfg = MinerConfig()

    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic, united=True)
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic)
    unpacker = cfg.create_proto_unpacker_processor()
    adapter_converter = cfg.create_offer_adapter_converter()
    ware_md5_creator = cfg.create_ware_md5_foodtech_creator()

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


def write_read_offers_lbk(input_topic, output_topic):
    request = UnitedOffersBatch()
    request.offer.extend(OFFERS)

    input_topic.write(DatacampMessage(united_offers=[request]).SerializeToString())

    return output_topic.read(count=1)


def test_ware_md5_miner(miner, input_topic, output_topic):
    """Проверяем, что miner проставляет ware_md5 в офферы фудтеха
    """
    data = write_read_offers_lbk(input_topic, output_topic)

    assert_that(data, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'service': IsProtobufMap({
                    10264169: {
                        'identifiers': {
                            'offer_id': offer_id,
                            'extra': {
                                'ware_md5': md5,
                            }
                        }
                    }
                })
            } for offer_id, md5 in zip(['lavka', 'eda', 'eda with ware md5'], EXPECTED_WARE_MD5)]
        }]
    }]))
