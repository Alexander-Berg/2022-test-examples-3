# coding: utf-8

import pytest
from hamcrest import assert_that

from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import (
    MarketContent,
    Offer as DatacampOffer,
    OfferContent,
    OfferIdentifiers,
    OfferMeta,
    OfferPrice,
    PriceBundle,
    PriceExpression,
    PartnerContent,
    ProcessedSpecification,
    InstallmentOptions,
    InstallmentOptionsGroup,
    BLUE
)
from market.idx.yatf.matchers.protobuf_matchers import ListWithNeededLength
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
    unpacker = cfg.create_proto_unpacker_processor()
    adapter_converter = cfg.create_offer_adapter_converter()
    installment_options_enricher = cfg.create_installment_options_enricher()
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic)

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, installment_options_enricher)
    cfg.create_link(installment_options_enricher, writer)

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


def gen_offer(offer_id, category_id, vendor_id):
    return UnitedOffer(
        basic=DatacampOffer(
            identifiers=OfferIdentifiers(
                offer_id=offer_id,
            ),
            content=OfferContent(
                market=MarketContent(
                    category_id=category_id,
                    vendor_id=vendor_id,
                ),
                partner=PartnerContent(
                    actual=ProcessedSpecification(
                        installment_options=InstallmentOptions(
                            options_groups=[InstallmentOptionsGroup(
                                group_name='old options',
                            )],
                        ),
                    ),
                ),
            ),
        ),
        service={
            100500: DatacampOffer(
                identifiers=OfferIdentifiers(
                    shop_id=100500,
                    offer_id=offer_id,
                ),
                price=OfferPrice(
                    basic=PriceBundle(
                        binary_price=PriceExpression(
                            price=(5000 * 10**7)
                        )
                    )
                ),
                meta=OfferMeta(rgb=BLUE),
            )
        }
    )


def test_installment_options_enricher(miner, input_topic, output_topic):
    offer1 = gen_offer('offer_with_category_installment', 200, 3000)
    offer2 = gen_offer('offer_with_vendor_installment', 500, 1000)
    offer3 = gen_offer('offer_with_both_installments', 100, 1000)

    request = UnitedOffersBatch()
    request.offer.extend([offer1, offer2, offer3])

    input_topic.write(DatacampMessage(united_offers=[request]).SerializeToString())

    # файла bnpl_conditions.json нет, bnpl рассрочка будет у всех синих офферов
    result_bnpl_group = {
        'group_name': 'bnpl',
        'bnpl_available': True,
    }
    result_group_for_category = {
        'group_name': 'group for categories',
        'installment_time_in_days': [90, 180],
        'bnpl_available': False,  # bnpl_available больше не заполняется из файла
    }
    result_group_for_vendor= {
        'group_name': 'group for vendors',
        'installment_time_in_days': [365],
        'bnpl_available': False,
    }

    data = output_topic.read(count=1)

    expected_offer_1 = {
        'basic': {
            'identifiers': {
                'offer_id': 'offer_with_category_installment'
            },
        },
        'service': IsProtobufMap({
            100500: {
                'identifiers': {
                    'offer_id': 'offer_with_category_installment'
                },
                'content': {
                    'partner': {
                        'actual': {
                            'installment_options': {
                                'options_groups': ListWithNeededLength([
                                    result_bnpl_group,
                                    result_group_for_category
                                ])
                            }
                        }
                    }
                },
            }
        }),
    }

    expected_offer_2 = {
        'basic': {
            'identifiers': {
                'offer_id': 'offer_with_vendor_installment'
            },
        },
        'service': IsProtobufMap({
            100500: {
                'identifiers': {
                    'offer_id': 'offer_with_vendor_installment'
                },
                'content': {
                    'partner': {
                        'actual': {
                            'installment_options': {
                                'options_groups': ListWithNeededLength([
                                    result_bnpl_group,
                                    result_group_for_vendor
                                ])
                            }
                        }
                    }
                },
            }
        }),
    }

    expected_offer_3 = {
        'basic': {
            'identifiers': {
                'offer_id': 'offer_with_both_installments'
            },
        },
        'service': IsProtobufMap({
            100500: {
                'identifiers': {
                    'offer_id': 'offer_with_both_installments'
                },
                'content': {
                    'partner': {
                        'actual': {
                            'installment_options': {
                                'options_groups': ListWithNeededLength([
                                    result_bnpl_group,
                                    result_group_for_category,
                                    result_group_for_vendor
                                ])
                            }
                        }
                    }
                },
            }
        })
    }

    assert_that(data, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [expected_offer_1, expected_offer_2, expected_offer_3]
        }]
    }]))
