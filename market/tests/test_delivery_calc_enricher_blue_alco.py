# coding: utf-8

import pytest
import json

from hamcrest import assert_that

from market.idx.datacamp.yatf.utils import dict2tskv
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampPartnersTable
from yatest.common.network import PortManager

from market.idx.feeds.feedparser.yatf.resources.delivery_calc import DeliveryCalcServer

from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv

from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import (
    OfferIdentifiers,
    PartnerInfo,
    OfferPrice,
    PriceBundle,
    OfferContent,
    PartnerContent,
    ProcessedSpecification,
    ProductType,
    MarketMasterData,
    PreciseDimensions,
    PreciseWeight,
    Offer as DatacampOffer,
)
from market.idx.datacamp.proto.offer.UnitedOffer_pb2 import (
    UnitedOffer,
    UnitedOffersBatch,
)
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages
from market.idx.yatf.matchers.protobuf_matchers import IsProtobufMap
from market.proto.common.eproduct_type_pb2 import EProductType
from market.proto.common.common_pb2 import PriceExpression


DC_GENERATION = 10
DC_GENERATION_FOR_FEED = 123123
DC_EMPTY_FEED_ID = -1

BUCKET_URL_PATH = 'bucketUrlPath'
SHOP_ID = 1
FEED_ID = 3
WAREHOUSE_ID = 147
VIRTUAL_SHOP_ID = 1001
FF_FEED_ID = 4444


@pytest.fixture(scope='module')
def offer():
    return {
        'shop_id': SHOP_ID,
        'offer_id': '2',
        'feed_id': FEED_ID,
        'warehouse_id': WAREHOUSE_ID,
        'type': EProductType.ALCO,

        'price': 123.4,

        'width': 100.0,
        'height': 200.0,
        'length': 300.0,
        'weight': 1.0
    }


@pytest.fixture(scope='module')
def partner_data():
    mbi = {
        'shop_id': SHOP_ID,
        'datafeed_id': FEED_ID,
        'warehouse_id': WAREHOUSE_ID,
        'ff_program': 'REAL',
        'is_online': True,
    }
    return [
        {
            'shop_id': SHOP_ID,
            'mbi':  dict2tskv(mbi),
            'status': 'publish'
        },
        {
            'shop_id': VIRTUAL_SHOP_ID,
            'mbi':  dict2tskv({
                'shop_id': VIRTUAL_SHOP_ID,
                'datafeed_id': FF_FEED_ID,
                'market_delivery_courier': True,
                'is_online': True,
            }),
            'status': 'publish'
        },
    ]


def bucket():
    return {
        'delivery_opt_bucket_id': 1,
        'currency': 'RUR',
        'carrier_ids': [99, 105],
        'program': 'MARKET_DELIVERY_PROGRAM',
        'delivery_option_group_regs': [{
            'region': '2',
            'delivery_opt_group_id': 1,
            'option_type': 'NORMAL_OPTION'
        }]
    }


def pickup_bucket():
    return {
        'bucket_id': 4,
        'program': 'MARKET_DELIVERY_PROGRAM',
        'currency': 'RUR',
        'carrier_ids': [15],
        'delivery_option_group_outlets': [{
            'outlet_id': 1001,
            'option_group_id': 1,
        }, {
            'outlet_id': 1002,
            'option_group_id': 1,
        }, {
            'outlet_id': 1003,
            'option_group_id': 2,
        }]
    }


def post_bucket():
    return {
        'bucket_id': 5,
        'program': 'MARKET_DELIVERY_PROGRAM',
        'currency': 'RUR',
        'carrier_ids': [20],
        'delivery_option_group_post_outlets': [{
            'post_code': 620100,
            'option_group_id': 1,
        }]
    }


def options_group():
    return {
        'delivery_option_group_id': 1,
        'payment_types': ['YANDEX'],
        'delivery_options': [{
            'delivery_cost': 1000,
            'min_days_count': 1,
            'max_days_count': 2,
            'order_before': 13,
        }]
    }


@pytest.yield_fixture(scope='function')
def delivery_calc_server():
    feed_response = {
        'response_code': 200,
        'generation_id': DC_GENERATION,
        'update_time_ts': 100,
        'currency': ['RUR'],
        'use_yml_delivery': False,
        'delivery_options_by_feed': {
            'delivery_option_buckets': [
                bucket(),
            ],
            'delivery_option_groups': [options_group()],
            'pickup_buckets': [pickup_bucket()],
            'post_buckets': [post_bucket()],
        }
    }

    offer_response = {
        'response_code': 200,
        'generation_id': DC_GENERATION,
        'generation_ts': 100,
        'offers': [{
            'delivery_opt_bucket_ids': [1, 2],
            'pickup_bucket_ids': [4],
            'post_bucket_ids': [5],
        }]
    }

    with PortManager() as pm:
        port = pm.get_port()

        feed_meta_response = json.dumps({
            'generationId': 1231230,  # общее поколение доставки в DC
            'realGenerationId': DC_GENERATION_FOR_FEED,  # поколение доставки для конкретного фида
            'bucketUrls': ['http://localhost:{port}/{path}'.format(port=port, path=BUCKET_URL_PATH)]
        })

        server = DeliveryCalcServer(feed_response=feed_response,
                                    offer_responses=[offer_response],
                                    feed_meta_response=feed_meta_response,
                                    feed_response_url='/{}'.format(BUCKET_URL_PATH),
                                    port=port)
        yield server


@pytest.fixture(scope='function')
def miner_config(
        yt_server,
        log_broker_stuff,
        input_topic,
        output_topic,
        delivery_calc_server,
        yt_token,
        partner_info_table_path,
):
    cfg = MinerConfig()

    cfg.create_miner_initializer(
        yt_server=yt_server,
        token=yt_token.path,
        partners_table_path=partner_info_table_path,
    )

    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic, united=True)
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic)
    unpacker = cfg.create_proto_unpacker_processor()
    adapter_converter = cfg.create_offer_adapter_converter()
    dc_enricher = cfg.create_delivery_calc_enricher_processor(
        delivery_calc_server,
        color='blue',
    )

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, dc_enricher)
    cfg.create_link(dc_enricher, writer)

    return cfg


@pytest.yield_fixture(scope='function')
def miner(
        yt_server,
        miner_config,
        input_topic, output_topic,
        delivery_calc_server,
        partner_info_table_path,
        partner_data
):
    resources = {
        'miner_cfg': miner_config,
        'input_topic': input_topic,
        'output_topic': output_topic,
        'delivery_calc_server': delivery_calc_server,
        'partners_table': DataCampPartnersTable(
            yt_stuff=yt_server,
            path=partner_info_table_path,
            data=partner_data)
    }
    with MinerTestEnv(**resources) as miner:
        miner.verify()
        yield miner


@pytest.fixture(scope='function')
def lbk_sender(miner, offer, input_topic):
    offer_to_send = UnitedOffer(
        basic=DatacampOffer(
            identifiers=OfferIdentifiers(
                offer_id=offer['offer_id'],
            ),
            content=OfferContent(
                partner=PartnerContent(
                    actual=ProcessedSpecification(
                        type=ProductType(
                            value=offer['type'],
                        )
                    )
                ),
                master_data=MarketMasterData(
                    dimensions=PreciseDimensions(
                        length_mkm=int(offer['length'])*10000,
                        width_mkm=int(offer['width'])*10000,
                        height_mkm=int(offer['height'])*10000
                    ),
                    weight_gross=PreciseWeight(
                        value_mg=int(offer['weight'])*1000000
                    )
                )
            ),
        ),
        service={
            offer['shop_id']: DatacampOffer(
                identifiers=OfferIdentifiers(
                    shop_id=offer['shop_id'],
                    offer_id=offer['offer_id'],
                    feed_id=offer['feed_id'],
                    warehouse_id=offer['warehouse_id'],
                ),
                price=OfferPrice(
                    basic=PriceBundle(
                        binary_price=PriceExpression(
                            price=int(offer['price'] * 10**7)
                        )
                    )
                ),
                partner_info=PartnerInfo(
                    fulfillment_feed_id=FF_FEED_ID,
                    fulfillment_virtual_shop_id=VIRTUAL_SHOP_ID,
                )
            )
        }
    )

    request = UnitedOffersBatch()
    request.offer.extend([offer_to_send])

    input_topic.write(DatacampMessage(united_offers=[request]).SerializeToString())


@pytest.fixture(scope='function')
def processed_offers(lbk_sender, miner, output_topic):
    data = output_topic.read(count=1)
    return data


def test_has_delivery_info(processed_offers, offer):
    """
    Проверяем, что у алкооффера нет доставки
    """

    assert_that(processed_offers, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'service': IsProtobufMap({
                    offer['shop_id']: {
                        'identifiers': {
                            'shop_id': offer['shop_id'],
                            'offer_id': offer['offer_id'],
                            'feed_id': offer['feed_id'],
                            'warehouse_id': offer['warehouse_id'],
                        },
                        'delivery': {
                            'delivery_info': {
                                'has_delivery': False
                            },
                            'partner': {
                                'actual': {
                                    'delivery': {
                                        'flag': False
                                    }
                                }
                            }
                        }
                    }
                })
            }]
        }]
    }]))
