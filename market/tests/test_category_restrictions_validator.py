# coding: utf-8
import os
import tempfile

import pytest
from hamcrest import assert_that

from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import (
    OfferContent,
    MarketContent,
    OfferIdentifiers,
    EnrichedOfferSubset,
    Offer as DatacampOffer,
    MARKET_IDX,
    OfferStatus,
    Flag,
    UpdateMeta,
    OfferMeta,
    BLUE
)
from market.idx.datacamp.proto.offer.UnitedOffer_pb2 import (
    UnitedOffer,
    UnitedOffersBatch,
)
import market.proto.ir.UltraController_pb2 as UC
from market.proto.content.mbo.Restrictions_pb2 import RestrictionsData, Restriction, RegionalRestrictions, Category, Region

from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages

from market.idx.yatf.matchers.protobuf_matchers import IsProtobufMap
from market.idx.yatf.resources.category_restrictions_pb import CategoryRestrictions
from market.idx.yatf.utils.utils import create_pb_timestamp

RESTRICTIONS_BASE_DIR = tempfile.mkdtemp()


@pytest.fixture(scope='module')
def category_restrictions():
    return CategoryRestrictions(RestrictionsData(
        restriction=[
            Restriction(
                name='globally restricted category WITHOUT subcategories, only matched offers',
                category=[
                    Category(
                        id=13314855,
                        include_subtree=False
                    )
                ],
                regional_restriction=[
                    RegionalRestrictions(
                        region=[],
                        display_only_matched_offers=True
                    )
                ]
            ),
            Restriction(
                name='globally banned category WITH subcategories',
                category=[
                    Category(
                        id=90537,  # subcat is 1009488
                        include_subtree=True
                    )
                ],
                regional_restriction=[
                    RegionalRestrictions(
                        banned=True,
                    )
                ]
            ),
            Restriction(
                name='REGIONALLY restricted category WITH subcategories, only unmatched offers',
                category=[
                    Category(
                        id=8475840,  # subcat is 90521, also globally restricted 90537
                        include_subtree=True
                    )
                ],
                regional_restriction=[
                    RegionalRestrictions(
                        region=[
                            Region(
                                id=225
                            )
                        ],
                        display_only_matched_offers=True
                    )
                ]
            ),
            Restriction(
                name='globally BLUE banned category',
                category=[
                    Category(
                        id=7693914,
                    )
                ],
                regional_restriction=[
                    RegionalRestrictions(
                        on_blue=True,
                        on_white=False,
                        banned=True,
                    )
                ]
            )
        ]
    ), preset_file_path=RESTRICTIONS_BASE_DIR)


@pytest.fixture(scope='module')
def miner_config(log_broker_stuff, input_topic, output_topic, offers_blog_topic, category_restrictions):
    cfg = MinerConfig()
    cfg.create_datacamp_logger_initializer(log_broker_stuff, offers_blog_topic, enable_trace_log=False)

    category_restriction_filepath = os.path.join(RESTRICTIONS_BASE_DIR, category_restrictions.filename)

    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic, united=True)
    unpacker = cfg.create_proto_unpacker_processor()
    adapter_converter = cfg.create_offer_adapter_converter()
    cfg.create_miner_initializer()
    category_restrictions_validator = cfg.create_category_restrictions_validator(category_restriction_filepath=category_restriction_filepath)
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic)

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, category_restrictions_validator)
    cfg.create_link(category_restrictions_validator, writer)

    return cfg


@pytest.yield_fixture(scope='module')
def miner(miner_config, input_topic, output_topic, offers_blog_topic, category_restrictions):
    resources = {
        'miner_cfg': miner_config,
        'input_topic': input_topic,
        'output_topic': output_topic,
        'category_restrictions': category_restrictions,
        'offers_blog_topic': offers_blog_topic
    }
    with MinerTestEnv(**resources) as miner:
        miner.verify()
        yield miner


def test_category_restrictions_validator(miner, input_topic, output_topic):
    """Проверяем, что майнер скрывает офера из "плохих" категорий"""
    unmatched_offer_regions = UnitedOffer(
        basic=DatacampOffer(
            identifiers=OfferIdentifiers(
                offer_id='unmatched_offer_regions',
            ),
            content=OfferContent(
                market=MarketContent(
                    enriched_offer=UC.EnrichedOffer(
                        category_id=8475840,
                        model_id=123456789,
                    ),
                ),
            ),
        ),
        service={
            1234: DatacampOffer(
                identifiers=OfferIdentifiers(
                    shop_id=1234,
                    offer_id='unmatched_offer_regions',
                    warehouse_id=123,
                ),
                status=OfferStatus(
                    disabled=[
                        Flag(
                            flag=False,
                            meta=UpdateMeta(
                                source=MARKET_IDX,
                                timestamp=create_pb_timestamp(1)
                            )
                        )
                    ]
                ),
            )
        }
    )
    unmatched_offer = UnitedOffer(
        basic=DatacampOffer(
            identifiers=OfferIdentifiers(
                offer_id='unmatched_offer',
            ),
            content=OfferContent(
                market=MarketContent(
                    enriched_offer=UC.EnrichedOffer(
                        category_id=13314855,
                        # matched_id=0
                    ),
                ),
            ),
        ),
        service={
            1234: DatacampOffer(
                identifiers=OfferIdentifiers(
                    shop_id=1234,
                    offer_id='unmatched_offer',
                    warehouse_id=123,
                )
            )
        }
    )
    matched_offer_banned_category = UnitedOffer(
        basic=DatacampOffer(
            identifiers=OfferIdentifiers(
                offer_id='matched_offer_banned_category',
            ),
            content=OfferContent(
                market=MarketContent(
                    enriched_offer=UC.EnrichedOffer(
                        category_id=90537,
                        model_id=123456789,
                        matched_id=123456,
                    ),
                    ir_data=EnrichedOfferSubset(
                        matched_id=111,
                    ),
                ),
            ),
        ),
        service={
            1234: DatacampOffer(
                identifiers=OfferIdentifiers(
                    shop_id=1234,
                    offer_id='matched_offer_banned_category',
                    warehouse_id=123,
                ),
            )
        }
    )
    offer_nonexist_category = UnitedOffer(
        basic=DatacampOffer(
            identifiers=OfferIdentifiers(
                offer_id='offer_nonexist_category',
            ),
            content=OfferContent(
                market=MarketContent(
                    enriched_offer=UC.EnrichedOffer(
                        category_id=100,
                        model_id=123456789,
                        matched_id=1234,
                    ),
                    ir_data=EnrichedOfferSubset(
                        matched_id=111,
                    ),
                ),
            ),
        ),
        service={
            1234: DatacampOffer(
                identifiers=OfferIdentifiers(
                    shop_id=1234,
                    offer_id='offer_nonexist_category',
                    warehouse_id=123,
                ),
            )
        }
    )
    blue_offer_banned_category = UnitedOffer(
        basic=DatacampOffer(
            identifiers=OfferIdentifiers(
                offer_id='blue_offer_banned_category',
            ),
            content=OfferContent(
                market=MarketContent(
                    enriched_offer=UC.EnrichedOffer(
                        category_id=7693914,
                        model_id=123456,
                        matched_id=123444,
                    ),
                    ir_data=EnrichedOfferSubset(
                        matched_id=123444,
                    ),
                ),
            ),
        ),
        service={
            1234: DatacampOffer(
                identifiers=OfferIdentifiers(
                    shop_id=1234,
                    offer_id='blue_offer_banned_category',
                    warehouse_id=123,
                ),
                meta=OfferMeta(rgb=BLUE),
            )
        }
    )

    request = UnitedOffersBatch()
    request.offer.extend([unmatched_offer_regions, unmatched_offer, matched_offer_banned_category, offer_nonexist_category, blue_offer_banned_category])

    input_topic.write(DatacampMessage(united_offers=[request]).SerializeToString())

    data = output_topic.read(count=1)

    assert_that(data, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [
                {
                    'basic': {
                        'identifiers': {
                            'offer_id': 'unmatched_offer_regions',
                        },
                        'offers_processor_fields': {
                            'forbidden_regions': [225],
                        },
                    },
                    'service': IsProtobufMap({
                        1234: {
                            'identifiers': {
                                'offer_id': 'unmatched_offer_regions',
                                'shop_id': 1234,
                                'warehouse_id': 123,
                            },
                        }
                    })
                },
                {
                    'basic': {
                        'identifiers': {
                            'offer_id': 'unmatched_offer',
                        },
                        'resolution': {
                            'by_source': [{
                                'meta': {
                                    'source': MARKET_IDX,
                                },
                                'verdict': [{
                                    'results': [{
                                        'is_banned': True,
                                        'messages': [{
                                            'code': '45Z'
                                        }]
                                    }]
                                }]
                            }]
                        }
                    },
                    'service': IsProtobufMap({
                        1234: {
                            'identifiers': {
                                'offer_id': 'unmatched_offer',
                                'shop_id': 1234,
                                'warehouse_id': 123,
                            },
                            'status': {
                                'disabled': [{
                                    'flag': True,
                                    'meta': {
                                        'source': MARKET_IDX
                                    }
                                }]
                            },
                        }
                    })
                },
                {
                    'basic': {
                        'identifiers': {
                            'offer_id': 'matched_offer_banned_category',
                        },
                        'resolution': {
                            'by_source': [{
                                'meta': {
                                    'source': MARKET_IDX,
                                },
                                'verdict': [{
                                    'results': [{
                                        'is_banned': True,
                                        'messages': [{
                                            'code': '45Z'
                                        }]
                                    }]
                                }]
                            }]
                        }
                    },
                    'service': IsProtobufMap({
                        1234: {
                            'identifiers': {
                                'offer_id': 'matched_offer_banned_category',
                                'shop_id': 1234,
                                'warehouse_id': 123,
                            },
                            'status': {
                                'disabled': [{
                                    'flag': True,
                                    'meta': {
                                        'source': MARKET_IDX
                                    }
                                }]
                            },
                        }
                    })
                },
                {
                    'basic': {
                        'identifiers': {
                            'offer_id': 'offer_nonexist_category',
                        },
                    },
                    'service': IsProtobufMap({
                        1234: {
                            'identifiers': {
                                'offer_id': 'offer_nonexist_category',
                                'shop_id': 1234,
                                'warehouse_id': 123,
                            },
                            'status': {
                                'disabled': [{
                                    'flag': False,
                                    'meta': {
                                        'source': MARKET_IDX
                                    }
                                }]
                            },
                        }
                    })
                },
                {
                    'basic': {
                        'identifiers': {
                            'offer_id': 'blue_offer_banned_category',
                        },
                        'resolution': {
                            'by_source': [{
                                'meta': {
                                    'source': MARKET_IDX,
                                },
                                'verdict': [{
                                    'results': [{
                                        'is_banned': True,
                                        'messages': [{
                                            'code': '45Z'
                                        }]
                                    }]
                                }]
                            }]
                        }
                    },
                    'service': IsProtobufMap({
                        1234: {
                            'identifiers': {
                                'offer_id': 'blue_offer_banned_category',
                                'shop_id': 1234,
                                'warehouse_id': 123,
                            },
                            'status': {
                                'disabled': [{
                                    'flag': True,
                                    'meta': {
                                        'source': MARKET_IDX
                                    }
                                }]
                            },
                        }
                    })
                },
            ]
        }]
    }]))
