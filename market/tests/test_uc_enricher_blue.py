# coding: utf-8

import pytest
from hamcrest import assert_that

from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.offer import DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.offer.UnitedOffer_pb2 import UnitedOffer, UnitedOffersBatch
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages

from market.idx.yatf.matchers.protobuf_matchers import IsProtobufMap
from market.idx.yatf.resources.lbk_topic import LbkTopic


def make_offer_data(
    offer_id,
    shop_id,
    business_id,
    approved_mapping=None,
    blue_uc_mapping=None
):
    result = {
        'business_id': business_id,
        'shop_id': shop_id,
        'offer_id': offer_id,
    }

    if approved_mapping:
        result['approved_mapping'] = approved_mapping
    if blue_uc_mapping:
        result['blue_uc_mapping'] = blue_uc_mapping

    return result


def make_datacamp_message(offers_data):
    batch = UnitedOffersBatch()

    for offer_data in offers_data:
        basic_offer = DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=offer_data['business_id'],
                offer_id=offer_data['offer_id'],
            ),
            content=DTC.OfferContent(
                binding=DTC.ContentBinding(
                    approved=DTC.Mapping(
                        market_sku_id=offer_data['approved_mapping']
                    ) if 'approved_mapping' in offer_data else None,
                    blue_uc_mapping=DTC.Mapping(
                        market_sku_id=offer_data['blue_uc_mapping']
                    ) if 'blue_uc_mapping' in offer_data else None
                )
            ) if 'approved_mapping' in offer_data or 'blue_uc_mapping' in offer_data else None
        )
        service_offer = DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=offer_data['business_id'],
                shop_id=offer_data['shop_id'],
                offer_id=offer_data['offer_id'],
                warehouse_id=145,
            ),
            meta=DTC.OfferMeta(
                rgb=DTC.BLUE,
            ),
            status=DTC.OfferStatus(
                united_catalog=DTC.Flag(
                    flag=True,
                )
            )
        )

        batch.offer.extend([
            UnitedOffer(
                basic=basic_offer,
                service={
                    offer_data['shop_id']: service_offer
                }
            )
        ])

    msg = DatacampMessage()
    msg.united_offers.extend([batch])

    return msg


@pytest.fixture(scope='module')
def offers():
    return [
        make_offer_data('ssku.valid.msku.mapping.1', 111, 1111,
                        approved_mapping=111001, blue_uc_mapping=111002),
        make_offer_data('ssku.zero.msku.mapping', 111, 1111,
                        approved_mapping=111001),
        make_offer_data('ssku.empty.msku.mapping', 111, 1111),
    ]


@pytest.fixture(scope='module')
def offers_batches():
    return [
        [
            make_offer_data('ssku.single.shop.batch.valid.msku.mapping.1', 222, 2222,
                            approved_mapping=222001),
            make_offer_data('ssku.single.shop.batch.valid.msku.mapping.2', 222, 2222,
                            approved_mapping=222002),
            make_offer_data('ssku.single.shop.batch.empty.msku.mapping', 222, 2222),
        ],
        [
            make_offer_data('ssku.few.shops.batch.valid.msku.mapping.1', 333, 3333,
                            approved_mapping=333001),
            make_offer_data('ssku.few.shops.batch.valid.msku.mapping.2', 333, 3333,
                            approved_mapping=333002),
            make_offer_data('ssku.few.shops.batch.empty.msku.mapping.3', 333, 3333),
            make_offer_data('ssku.few.shops.batch.valid.msku.mapping.3', 333, 3333,
                            approved_mapping=333002),
            make_offer_data('ssku.few.shops.batch.valid.msku.mapping.2', 444, 4444,
                            approved_mapping=444001),
            make_offer_data('ssku.few.shops.batch.valid.msku.mapping.3', 444, 4444,
                            approved_mapping=444001),
            make_offer_data('ssku.few.shops.batch.empty.msku.mapping.4', 444, 4444),
        ],
    ]


@pytest.fixture(scope='module')
def input_topic(log_broker_stuff):
    return LbkTopic(log_broker_stuff)


@pytest.fixture(scope='module')
def output_topic(log_broker_stuff):
    return LbkTopic(log_broker_stuff)


@pytest.fixture(scope='module')
def miner_config(log_broker_stuff, input_topic, output_topic):
    cfg = MinerConfig()

    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic, united=True)
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic)
    unpacker = cfg.create_proto_unpacker_processor()
    adapter_converter = cfg.create_offer_adapter_converter()
    uc_enricher = cfg.create_blue_uc_enricher_processor()

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, uc_enricher)
    cfg.create_link(uc_enricher, writer)

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


@pytest.yield_fixture(scope='module')
def workflow(miner, input_topic, output_topic, offers, offers_batches):
    count = len(offers)
    for offer in offers:
        input_topic.write(make_datacamp_message([offer]).SerializeToString())

    count += len(offers_batches)
    for batch in offers_batches:
        input_topic.write(make_datacamp_message(batch).SerializeToString())

    yield output_topic.read(count)


@pytest.mark.parametrize('business_id, shop_id, offer_id, msku', [
    (1111, 111, 'ssku.valid.msku.mapping.1', 111001),
    (2222, 222, 'ssku.single.shop.batch.valid.msku.mapping.1', 222001),
    (2222, 222, 'ssku.single.shop.batch.valid.msku.mapping.2', 222002),
    (3333, 333, 'ssku.few.shops.batch.valid.msku.mapping.1', 333001),
    (3333, 333, 'ssku.few.shops.batch.valid.msku.mapping.2', 333002),
    (3333, 333, 'ssku.few.shops.batch.valid.msku.mapping.3', 333002),
    (4444, 444, 'ssku.few.shops.batch.valid.msku.mapping.2', 444001),
    (4444, 444, 'ssku.few.shops.batch.valid.msku.mapping.3', 444001),
])
def test_valid_msku_mapping(workflow, business_id, shop_id, offer_id, msku):
    """Проверяем, что установка blue_uc_mapping и extra.market_sku_id работает корректно
    """
    assert_that(workflow, HasSerializedDatacampMessages([{
        'united_offers': [
            {
                'offer': [
                    {
                        'basic': {
                            'identifiers': {
                                'business_id': business_id,
                                'offer_id': offer_id
                            },
                            'content': {
                                'binding': {
                                    'blue_uc_mapping': {
                                        'market_sku_id': msku,
                                    }
                                }
                            },
                        },
                        'service': IsProtobufMap({
                            shop_id: {
                                'identifiers': {
                                    'business_id': business_id,
                                    'offer_id': offer_id,
                                    'shop_id': shop_id,
                                    'extra': {
                                        'market_sku_id': msku
                                    }
                                }
                            }
                        })
                    }
                ]
            }
        ]
    }]))


@pytest.mark.parametrize('business_id, shop_id, offer_id', [
    (1111, 111, 'ssku.empty.msku.mapping'),
    (1111, 111, 'ssku.zero.msku.mapping'),
    (2222, 222, 'ssku.single.shop.batch.empty.msku.mapping'),
    (3333, 333, 'ssku.few.shops.batch.empty.msku.mapping.3'),
    (4444, 444, 'ssku.few.shops.batch.empty.msku.mapping.4'),
])
def test_invalid_msku_mapping(workflow, offers, offers_batches, business_id, shop_id, offer_id):
    """Проверяем, что если approved пустой, blue_uc_mapping остается пустым
    """
    assert_that(workflow, HasSerializedDatacampMessages([{
        'united_offers': [
            {
                'offer': [
                    {
                        'basic': {
                            'identifiers': {
                                'business_id': business_id,
                                'offer_id': offer_id,
                            },
                            'content': {
                                'binding': {
                                    'blue_uc_mapping': {}
                                }
                            },
                        },
                        'service': IsProtobufMap({
                            shop_id: {
                                'identifiers': {
                                    'business_id': business_id,
                                    'offer_id': offer_id,
                                    'shop_id': shop_id,
                                    'extra': {}
                                },
                            }
                        })
                    }
                ]
            }
        ]
    }]))
