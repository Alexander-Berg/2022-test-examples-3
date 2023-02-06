# coding: utf-8

import pytest
import json

from hamcrest import assert_that, equal_to, anything
from yt.wrapper import ypath_join

import market.idx.datacamp.proto.offer.UnitedOffer_pb2 as DTC

from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.yatf.utils import dict2tskv
from market.idx.feeds.feedparser.yatf.resources.delivery_calc import DeliveryCalcServer
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobufMap
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampPartnersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yatest.common.network import PortManager


DC_GENERATION = 10
SHOP_ID = 999
BUSINESS_ID = 999
FEED_ID = 888

test_data = [
    {
        'id': 'normal_delivery',
        'price': 100,
        'type': DTC.ON_DEMAND,
        'delivery_max_days': 29
    },
    {
        'id': 'long_delivery',
        'price': 100,
        'type': DTC.ON_DEMAND,
        'delivery_max_days': 61
    },
]


@pytest.fixture(scope='module', params=[False, True], ids=['no_courier', 'courier_delivery'])
def medicine_courier_status(request):
    return request.param


@pytest.fixture(scope='module')
def partner_data(medicine_courier_status):
    mbi = {
        'shop_id': SHOP_ID,
        'datafeed_id': FEED_ID,
        'business_id': BUSINESS_ID,
        'warehouse_id': 0,
        'is_online': 'true',
        'medicine_courier': str(medicine_courier_status).lower(),
        'is_site_market': 'true'
    }
    return [
        {
            'shop_id': SHOP_ID,
            'mbi': dict2tskv(mbi),
            'status': 'publish'
        }
    ]


@pytest.yield_fixture(scope='function')
def delivery_calc_server():
    shop_meta_response = json.dumps({
        'generationId': DC_GENERATION,
        'currencies': ['RUR'],
        "useYmlDelivery": True,
    })

    shop_offers_response = {
        'generation_id': DC_GENERATION,
        'offers': [{
            'courier_buckets_info': offer.get('courier_buckets_info', [])
        } for offer in test_data
        ]
    }

    with PortManager() as pm:
        port = pm.get_port()
        server = DeliveryCalcServer(feed_response=None,
                                    offer_responses=None,
                                    shop_offers_responses=[shop_offers_response],
                                    shop_meta_response=shop_meta_response,
                                    port=port)
        yield server


@pytest.yield_fixture(scope='module')
def partner_info_table_path():
    return ypath_join('datacamp', 'partners')


@pytest.fixture(scope='function')
def miner_config(
        yt_server,
        log_broker_stuff,
        input_topic,
        output_topic,
        offers_blog_topic,
        delivery_calc_server,
        yt_token,
        partner_info_table_path
):
    cfg = MinerConfig()

    cfg.create_miner_initializer(
        yt_server=yt_server,
        token=yt_token.path,
        partners_table_path=ypath_join(get_yt_prefix(), partner_info_table_path),
    )
    cfg.create_datacamp_logger_initializer(log_broker_stuff, offers_blog_topic)

    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic, True)
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic)
    unpacker = cfg.create_proto_unpacker_processor()
    adapter_converter = cfg.create_offer_adapter_converter()
    content_converter = cfg.create_offer_content_converter('white')
    dc_enricher = cfg.create_delivery_calc_enricher_processor(
        delivery_calc_server,
        color='white',
        use_average_dimensions_and_weight=True,
    )
    delivery_validator = cfg.create_delivery_validator()

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, content_converter)
    cfg.create_link(content_converter, dc_enricher)
    cfg.create_link(dc_enricher, delivery_validator)
    cfg.create_link(delivery_validator, writer)

    return cfg


@pytest.yield_fixture(scope='function')
def miner(
        yt_server,
        miner_config,
        input_topic, output_topic,
        delivery_calc_server,
        partner_info_table_path,
        partner_data,
        offers_blog_topic
):
    resources = {
        'miner_cfg': miner_config,
        'input_topic': input_topic,
        'output_topic': output_topic,
        'blog_topic': offers_blog_topic,
        'delivery_calc_server': delivery_calc_server,
        'partners_table': DataCampPartnersTable(
            yt_stuff=yt_server,
            path=ypath_join(get_yt_prefix(), partner_info_table_path),
            data=partner_data)
    }
    with MinerTestEnv(**resources) as miner:
        miner.verify()
        yield miner


@pytest.fixture(scope='function')
def united_offers_to_send():
    return [
        DatacampMessage(
            united_offers=[DTC.UnitedOffersBatch(
                offer=[DTC.UnitedOffer(
                    basic=DTC.Offer(
                        identifiers=DTC.OfferIdentifiers(
                            offer_id=offer['id'],
                            business_id=BUSINESS_ID,
                        ),
                        content=DTC.OfferContent(
                            partner=DTC.PartnerContent(
                                original=DTC.OriginalSpecification(
                                    type=DTC.ProductType(
                                        value=offer['type']
                                    ),
                                    name=DTC.StringValue(
                                        value=offer['id'],
                                    ),
                                )
                            )
                        ),
                    ),
                    service={
                        SHOP_ID: DTC.Offer(
                            identifiers=DTC.OfferIdentifiers(
                                shop_id=SHOP_ID,
                                offer_id=offer['id'],
                                feed_id=FEED_ID,
                                warehouse_id=0,
                                business_id=BUSINESS_ID,
                            ),
                            meta=DTC.OfferMeta(rgb=DTC.WHITE),
                            partner_info=DTC.PartnerInfo(
                                is_dsbs=True,
                            ),
                            price=DTC.OfferPrice(
                                basic=DTC.PriceBundle(
                                    binary_price=DTC.PriceExpression(
                                        price=offer['price'] * 10 ** 7,
                                        id='RUR',
                                    )
                                )
                            ),
                            delivery=DTC.OfferDelivery(
                                partner=DTC.PartnerDelivery(
                                    original=DTC.OriginalPartnerDelivery(
                                        delivery=DTC.Flag(
                                            flag=True,
                                        ),
                                        delivery_options=DTC.DeliveryOptions(
                                            options=[
                                                DTC.TDeliveryOption(
                                                    Cost=300,
                                                    DaysMax=offer['delivery_max_days'],
                                                    DaysMin=offer['delivery_max_days'],
                                                    DeliverySrc='YML',
                                                    OrderBeforeHour=18
                                                ),
                                            ]
                                        ),
                                    ),
                                ),
                            ),
                        )
                    }
                ) for offer in test_data])])
    ]


@pytest.fixture(scope='function')
def lbk_sender(miner, input_topic, united_offers_to_send):
    for message in united_offers_to_send:
        input_topic.write(message.SerializeToString())


@pytest.fixture(scope='function')
def processed_offers(lbk_sender, miner, output_topic):
    data = output_topic.read(count=1)
    return data


def test_dc_requests_and_enricher(miner, processed_offers, medicine_courier_status):
    # Проверяем корректность запросов в КД
    dc_requests = miner.resources['delivery_calc_server'].GetRequests()
    assert_that(len(dc_requests), equal_to(2))

    def gen_status(offer):
        if offer['id'] == 'long_delivery':
            return {
                'disabled': [
                    {'flag': True, 'meta': {'source': DTC.MARKET_IDX}}
                ]
            }
        return {
            'disabled': [
                {'flag': False, 'meta': {'source': DTC.MARKET_IDX}}
            ]
        }

    def gen_resolution(offer):
        if offer['id'] == 'long_delivery':
            return {
                "by_source": [{
                    "verdict": [{
                        "results": [{
                            "messages": [{
                                "code": "49Y",
                            }]
                        }]
                    }]
                }]
            }

        return anything()

    assert_that(processed_offers[0], IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [
                {
                    'service': IsProtobufMap({
                        SHOP_ID: {
                            'identifiers': {
                                'business_id': BUSINESS_ID,
                                'shop_id': SHOP_ID,
                                'offer_id': offer['id'],
                                'feed_id': FEED_ID,
                            },
                            'delivery': {
                                'market': {
                                },
                                'delivery_info': {
                                    'delivery_currency': 'RUR',
                                    'use_yml_delivery': True,
                                    'real_deliverycalc_generation': DC_GENERATION,
                                },
                            },
                            'status': gen_status(offer),
                            'resolution': gen_resolution(offer)
                        }
                    })
                } for offer in test_data
            ]
        }]
    }))
