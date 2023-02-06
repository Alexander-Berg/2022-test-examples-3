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

BUSINESS_ID1 = 1
BUSINESS_ID2 = 2
BUSINESS_ID3 = 3
SHOP_ID1 = 34351
SHOP_ID2 = 34361
SHOP_ID3 = 34342
LAVKA_OFFER_ID = 'lavka'
EDA_OFFER_ID = 'eda'
RESTAURANTS_OFFER_ID = 'eda_restaurants'

OFFERS = [
    UnitedOffer(
        basic=DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID1,
                offer_id=LAVKA_OFFER_ID,
            )
        ),
        service={
            SHOP_ID1: DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=BUSINESS_ID1,
                    offer_id=LAVKA_OFFER_ID,
                    shop_id=SHOP_ID1,
                ),
                meta=create_meta(10, DTC.LAVKA),
            )
        }
    ),
    UnitedOffer(
        basic=DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID2,
                offer_id=EDA_OFFER_ID,
            )
        ),
        service={
            SHOP_ID2: DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=BUSINESS_ID2,
                    offer_id=EDA_OFFER_ID,
                    shop_id=SHOP_ID2,
                ),
                meta=create_meta(10, DTC.EDA),
            )
        }
    ),
    UnitedOffer(
        basic=DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID3,
                offer_id=RESTAURANTS_OFFER_ID,
            )
        ),
        service={
            SHOP_ID3: DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=BUSINESS_ID3,
                    offer_id=RESTAURANTS_OFFER_ID,
                    shop_id=SHOP_ID3,
                ),
                meta=create_meta(10, DTC.EDA_RESTAURANTS),
            )
        }
    ),
]


@pytest.fixture(scope='module')
def miner_config(log_broker_stuff, input_topic, output_topic):
    cfg = MinerConfig()

    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic, united=True)
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic)
    unpacker = cfg.create_proto_unpacker_processor()
    adapter_converter = cfg.create_offer_adapter_converter()
    foodtech_delivery_enricher = cfg.create_foodtech_delivery_enricher()

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, foodtech_delivery_enricher)
    cfg.create_link(foodtech_delivery_enricher, writer)

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


def test_eda_retail_lavka_use_yml_data_enricher(miner, input_topic, output_topic):
    """Проверяем что проставляем use_yml_delivery = true только для ритейла и лавки
    """
    request = UnitedOffersBatch()
    request.offer.extend(OFFERS)

    input_topic.write(DatacampMessage(united_offers=[request]).SerializeToString())

    data = output_topic.read(count=1)

    assert_that(data, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [
                {
                    'basic': {
                        'identifiers': {
                            'business_id': BUSINESS_ID1,
                            'offer_id': LAVKA_OFFER_ID,
                        }
                    },
                    'service': IsProtobufMap({
                        SHOP_ID1: {
                            'identifiers': {
                                'business_id': BUSINESS_ID1,
                                'offer_id': LAVKA_OFFER_ID,
                                'shop_id': SHOP_ID1,
                            },
                            'delivery': {
                                'delivery_info': {
                                    'use_yml_delivery': True
                                },
                                'market': {
                                    'use_yml_delivery': {
                                        'flag': True,
                                        'meta': {
                                            'applier': DTC.MINER
                                        }
                                    }
                                }
                            }
                        }
                    })
                },
                {
                    'basic': {
                        'identifiers': {
                            'business_id': BUSINESS_ID2,
                            'offer_id': EDA_OFFER_ID,
                        }
                    },
                    'service': IsProtobufMap({
                        SHOP_ID2: {
                            'identifiers': {
                                'business_id': BUSINESS_ID2,
                                'offer_id': EDA_OFFER_ID,
                                'shop_id': SHOP_ID2,
                            },
                            'delivery': {
                                'delivery_info': {
                                    'use_yml_delivery': True
                                },
                                'market': {
                                    'use_yml_delivery': {
                                        'flag': True,
                                        'meta': {
                                            'applier': DTC.MINER
                                        }
                                    }
                                },
                                'partner': {
                                    'actual': {
                                        'delivery_currency': {
                                            'value': 'RUR'
                                        }
                                    }
                                }
                            },
                        }
                    })
                },
                {
                    'basic': {
                        'identifiers': {
                            'business_id': BUSINESS_ID3,
                            'offer_id': RESTAURANTS_OFFER_ID,
                        }
                    },
                    'service': IsProtobufMap({
                        SHOP_ID3: {
                            'identifiers': {
                                'business_id': BUSINESS_ID3,
                                'offer_id': RESTAURANTS_OFFER_ID,
                                'shop_id': SHOP_ID3,
                            },
                            'delivery': None,
                        }
                    })
                },
            ]
        }]
    }]))
