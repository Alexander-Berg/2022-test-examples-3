# coding: utf-8

import pytest
from hamcrest import assert_that

from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import (
    BLUE,
    ContentBinding,
    EnrichedOfferSubset,
    Mapping,
    MarketContent,
    Offer as DatacampOffer,
    OfferContent,
    OfferIdentifiers,
    OfferMeta,
)
from market.idx.datacamp.proto.offer.UnitedOffer_pb2 import (
    UnitedOffer,
    UnitedOffersBatch,
)
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages
from market.idx.yatf.matchers.protobuf_matchers import IsProtobufMap


@pytest.fixture(scope='module')
def miner_config(log_broker_stuff, input_topic, output_topic):
    cfg = MinerConfig()

    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic, united=True)
    unpacker = cfg.create_proto_unpacker_processor()
    adapter_converter = cfg.create_offer_adapter_converter()
    cms_promo_enricher = cfg.create_cms_promo_enricher()
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic)

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, cms_promo_enricher)
    cfg.create_link(cms_promo_enricher, writer)

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


def test_cms_promo_enricher(miner, input_topic, output_topic):
    """Проверяем, что обогащение cms promo работает"""
    offer = UnitedOffer(
        basic=DatacampOffer(
            identifiers=OfferIdentifiers(business_id=1, offer_id='offer'),
            content=OfferContent(
                binding=ContentBinding(
                    uc_mapping=Mapping(
                        market_sku_id=100906962092,
                    ),
                ),
                market=MarketContent(
                    ir_data=EnrichedOfferSubset(
                        matched_id=657843127,
                    ),
                ),
            ),
        ),
        service={
            123: DatacampOffer(
                identifiers=OfferIdentifiers(
                    business_id=1,
                    offer_id='offer',
                    shop_id=123,
                    warehouse_id=1234
                ),
                meta=OfferMeta(rgb=BLUE),
            )
        }
    )

    request = UnitedOffersBatch()
    request.offer.extend([offer])

    input_topic.write(DatacampMessage(united_offers=[request]).SerializeToString())

    data = output_topic.read(count=1)

    assert_that(data, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [
                {
                    'basic': {
                        'identifiers': {
                            'business_id': 1,
                            'offer_id': 'offer',
                        },
                    },
                    'service': IsProtobufMap({
                        123: {
                            'identifiers': {
                                'business_id': 1,
                                'offer_id': 'offer',
                                'shop_id': 123,
                                'warehouse_id': 1234,
                            },
                            'offers_processor_fields': {
                                'msku_promos': ['nb-koltsa', 'nebo10'],
                                'model_promos': ['huawei-p40']
                            },
                        }
                    })
                },
            ]
        }]
    }]))
