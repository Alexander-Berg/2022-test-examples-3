# coding: utf-8

import pytest

from hamcrest import assert_that, has_items, not_, empty

from market.idx.datacamp.yatf.matchers.matchers import HasSerializedProtos
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobufMap
from market.idx.datacamp.yatf.utils import dict2tskv

from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.offer.UnitedOffer_pb2 import UnitedOffersBatch, UnitedOffer
from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import OfferMeta, WHITE

from market.proto.common.common_pb2 import PriceExpression
from market.proto.common import process_log_pb2 as PL
from market.proto.common.common_pb2 import EComponent
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import (
    OfferIdentifiers,
    PriceBundle,
    OfferPrice,
    Offer as DatacampOffer,
    MARKET_IDX,
    OfferContent,
    OriginalSpecification,
    PartnerContent,
    StringValue,
    Explanation
)
from market.idx.datacamp.proto.offer.OfferBlog_pb2 import OfferBlog

from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampPartnersTable


OFFERS = [
    {
        'identifiers': {
            'shop_id': 1111,
            'business_id': 1111,
            'offer_id': 'offerWithBadUrl',
        },
        'price': 100,
        'url': 'totally_not_url',
    },
    {
        'identifiers': {
            'shop_id': 1111,
            'business_id': 1111,
            'offer_id': 'offerWithUrl',
        },
        'price': 100,
        'url': 'http://datacamp.ru/pretty_offer',
    },
    {
        'identifiers': {
            'shop_id': 2222,
            'business_id': 2222,
            'offer_id': 'offerWithBadUrl',
        },
        'price': 100,
        'url': 'totally_not_url',
    },
    {
        'identifiers': {
            'shop_id': 2222,
            'business_id': 2222,
            'offer_id': 'offerWithUrl',
        },
        'price': 100,
        'url': 'http://datacamp.ru/pretty_offer',
    },
]

EXPECTED_MESSAGES = [
    {
        'identifiers': {
            'shop_id': 1111,
            'offer_id': 'offerWithBadUrl',
        },
        'errors': {
            'error': [{
                'code': '450',
                'level': PL.ERROR,
                'namespace': PL.OFFER,
                'source': EComponent.MINER
            }]
        }
    },
    {
        'identifiers': {
            'shop_id': 2222,
            'offer_id': 'offerWithBadUrl',
        },
        'errors': {
            'error': [{
                'code': '35S',
                'level': PL.WARNING,
                'namespace': PL.OFFER,
                'source': EComponent.MINER
            }]
        }
    }
]


@pytest.fixture(scope='module')
def partner_data():
    return [
        {
            'shop_id': 1111,
            'mbi':  dict2tskv({
                'shop_id': 1111,
                'business_id': 1111,
                'datafeed_id': 1110,
                'direct_product_mapping': 'NO',
                'is_dsbs': 'false',
                'is_site_market': 'true'
            }),
            'status': 'publish'
        },
        {
            'shop_id': 2222,
            'mbi':  dict2tskv({
                'shop_id': 2222,
                'business_id': 2222,
                'datafeed_id': 2220,
                'direct_product_mapping': 'REAL',
                'is_site_market': 'true'
            }),
            'status': 'publish'
        },
    ]


@pytest.fixture(scope='module')
def miner_config(
        yt_server,
        log_broker_stuff,
        input_topic,
        output_topic,
        offers_blog_topic,
        yt_token,
        partner_info_table_path
):
    cfg = MinerConfig()
    cfg.create_datacamp_logger_initializer(log_broker_stuff, offers_blog_topic, enable_trace_log=False)

    cfg.create_miner_initializer(
        yt_server=yt_server,
        token=yt_token.path,
        partners_table_path=partner_info_table_path,
    )

    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic, True)
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic)
    unpacker = cfg.create_proto_unpacker_processor()
    adapter_converter = cfg.create_offer_adapter_converter()
    shopsdat_enricher = cfg.create_shopsdat_enricher(color='white')
    offer_content_converter = cfg.create_offer_content_converter(color='white')

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, shopsdat_enricher)
    cfg.create_link(shopsdat_enricher, offer_content_converter)
    cfg.create_link(offer_content_converter, writer)

    return cfg


@pytest.yield_fixture(scope='module')
def miner(
        yt_server,
        miner_config,
        input_topic, output_topic, offers_blog_topic,
        partner_info_table_path,
        partner_data
):
    resources = {
        'miner_cfg': miner_config,
        'input_topic': input_topic,
        'output_topic': output_topic,
        'offers_blog_topic': offers_blog_topic,
        'partners_table': DataCampPartnersTable(
            yt_stuff=yt_server,
            path=partner_info_table_path,
            data=partner_data)
    }
    with MinerTestEnv(**resources) as miner:
        miner.verify()
        yield miner


@pytest.yield_fixture(scope='module')
def workflow(miner, input_topic, output_topic):
    for o in OFFERS:
        offer = DatacampOffer(
            price=OfferPrice(
                basic=PriceBundle(
                    binary_price=PriceExpression(
                        price=int(o['price'] * 10**7)
                    ),
                )
            ),
            identifiers=OfferIdentifiers(
                offer_id=o['identifiers']['offer_id'],
                business_id=o['identifiers']['business_id'],
            ),
            content=OfferContent(
                partner=PartnerContent(
                    original=OriginalSpecification(
                        url=StringValue(
                            value=o['url']
                        ),
                        name=StringValue(
                            value='title'
                        ),
                    ),
                )
            ),
        )
        message = DatacampMessage(united_offers=[UnitedOffersBatch(
            offer=[UnitedOffer(
                basic=offer,
                service={o['identifiers']['shop_id']: DatacampOffer(
                    identifiers=OfferIdentifiers(
                        business_id=o['identifiers']['business_id'],
                        shop_id=o['identifiers']['shop_id'],
                        offer_id=o['identifiers']['offer_id'],
                    ),
                    meta=OfferMeta(rgb=WHITE),
                    content=OfferContent(
                        partner=PartnerContent(
                            original=OriginalSpecification(
                                url=StringValue(value=o['url']),
                            )
                        )
                    )
                )}
            )]
        )])
        input_topic.write(message.SerializeToString())
    yield output_topic.read(count=len(OFFERS))


@pytest.fixture(scope='module')
def offers_blog_messages(workflow, offers_blog_topic):
    return offers_blog_topic.read(len(EXPECTED_MESSAGES))


@pytest.mark.parametrize('shop_id, offer_id, disabled_flag, resolution', [
    (1111, 'offerWithBadUrl', True, {
        'by_source': [{
            'meta': {
                'source': MARKET_IDX
            },
            'verdict': [{
                'results': [{
                    'is_banned': True,
                    'applications': empty(),
                    'messages': [{
                        'code': '450',
                        'level': Explanation.ERROR,
                        'params': [
                            {'name': 'url', 'value': 'totally_not_url'},
                            {'name': 'code', 'value': '450'}
                        ],
                        'details': not_(empty()),
                    }]
                }]
            }]
        }]
    }),
    (1111, 'offerWithUrl', False, {}),
    (2222, 'offerWithBadUrl', False, {
        'by_source': [{
            'meta': {
                'source': MARKET_IDX
            },
            'verdict': [{
                'results': [{
                    'is_banned': False,
                    'applications': empty(),
                    'messages': [{
                        'code': '35S',
                        'level': Explanation.WARNING,
                        'params': [
                            {'name': 'url', 'value': 'totally_not_url'},
                            {'name': 'code', 'value': '35S'}
                        ],
                        'details': not_(empty()),
                    }],
                }]
            }]
        }]
    }),
    (2222, 'offerWithUrl', False, {}),
    ],
    ids=[
        '1offerWithBadUrl',
        '1offerWithUrl',
        '2offerWithBadUrl',
        '2offerWithUrl'
    ]
)
def test_offer_status(workflow, shop_id, offer_id, disabled_flag, resolution):
    assert_that(workflow, has_items(IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': shop_id,
                        'offer_id': offer_id,
                    },
                },
                'service': IsProtobufMap({
                    shop_id: {
                        'identifiers': {
                            'business_id': shop_id,
                            'offer_id': offer_id,
                            'shop_id': shop_id,
                        },
                        'status': {
                            'disabled': [
                                {
                                    'flag': disabled_flag,
                                    'meta': {
                                        'source': MARKET_IDX
                                    }
                                }
                            ],
                        },
                        'resolution': resolution
                    },
                })
            }]
        }]})))


def test_offer_blog_messages(offers_blog_messages):
    assert_that(offers_blog_messages, HasSerializedProtos(EXPECTED_MESSAGES, proto_cls=OfferBlog))
