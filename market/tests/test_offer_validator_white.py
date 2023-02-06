# coding: utf-8

import pytest
import yatest

from hamcrest import assert_that, has_items, greater_than, has_entries
from datetime import datetime, timedelta

from market.idx.datacamp.yatf.utils import dict2tskv
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobufMap

from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv

from market.proto.common.common_pb2 import PriceExpression
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.offer.UnitedOffer_pb2 import UnitedOffersBatch, UnitedOffer

from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampPartnersTable
from market.idx.yatf.utils.utils import create_timestamp_from_json

BUSINESS_ID = 12345
SHOP_ID = 111
TITLE = 'Title'
DESCR = 'Description Description Description'


@pytest.fixture(scope="module")
def offers():
    return [
        {
            'identifiers': {
                'business_id': BUSINESS_ID,
                'shop_id': SHOP_ID,
                'offer_id': 'with_zero_price',
            },
            'classifier_good_id': '12',
            'classifier_magic_id': '23',
            'price': 0,
            'dynamic_pricing_threshold_percent': 15,
            'expected_error': '453',
        },
        {
            'identifiers': {
                'business_id': BUSINESS_ID,
                'shop_id': SHOP_ID,
                'offer_id': 'with_wrong_dynamic_pricing_percent',
            },
            'classifier_good_id': '12',
            'classifier_magic_id': '23',
            'price': 234,
            'dynamic_pricing_threshold_percent': 105.5,
            'expected_error': '49c',
        },
        {
            'identifiers': {
                'business_id': BUSINESS_ID,
                'shop_id': SHOP_ID,
                'offer_id': 'no_classififer_good_id',
            },
            'classifier_magic_id': '23',
            'price': 234,
            'dynamic_pricing_threshold_percent': 15,
            'expected_error': '49S',
        },
        {
            'identifiers': {
                'business_id': BUSINESS_ID,
                'shop_id': SHOP_ID,
                'offer_id': 'no_classififer_magic_id',
            },
            'classifier_good_id': '12',
            'price': 234,
            'dynamic_pricing_threshold_percent': 15,
            'expected_error': '49S',
        },
    ]


@pytest.fixture(scope='module')
def partner_data():
    return [
        {
            'shop_id': SHOP_ID,
            'mbi':  dict2tskv({
                'business_id': BUSINESS_ID,
                'shop_id': SHOP_ID,
                'datafeed_id': 1110,
                'united_catalog_status': 'SUCCESS',
            }),
            'status': 'publish'
        },
    ]


@pytest.fixture(scope='session')
def port_manager():
    with yatest.common.network.PortManager() as port_manager:
        yield port_manager


@pytest.fixture(scope='module')
def miner_config(
        yt_server,
        log_broker_stuff,
        input_topic,
        output_topic,
        offers_blog_topic,
        yt_token,
        partner_info_table_path,
        port_manager
):
    cfg = MinerConfig()
    cfg.create_datacamp_logger_initializer(log_broker_stuff, offers_blog_topic, enable_trace_log=False)

    cfg.create_miner_initializer(
        yt_server=yt_server,
        token=yt_token.path,
        partners_table_path=partner_info_table_path,
        monservice_port=port_manager.get_port()
    )

    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic, united=True)
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic)
    unpacker = cfg.create_proto_unpacker_processor()
    adapter_converter = cfg.create_offer_adapter_converter()
    offer_validator = cfg.create_offer_validator(
        color='white',
    )

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, offer_validator)
    cfg.create_link(offer_validator, writer)

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
def workflow(miner, input_topic, output_topic, offers):
    old_time = (datetime.utcnow() - timedelta(minutes=45)).strftime("%Y-%m-%dT%H:%M:%SZ")
    for o in offers:
        message = DatacampMessage(united_offers=[UnitedOffersBatch(
            offer=[UnitedOffer(
                basic=DTC.Offer(
                    identifiers=DTC.OfferIdentifiers(
                        business_id=o['identifiers']['business_id'],
                        offer_id=o['identifiers']['offer_id'],
                        extra=DTC.OfferExtraIdentifiers(
                            market_sku_id=10003,
                            classifier_good_id=o['classifier_good_id'] if 'classifier_good_id' in o else None,
                            classifier_magic_id2=o['classifier_magic_id'] if 'classifier_magic_id' in o else None,
                        ),
                    ),
                ),
                service={o['identifiers']['shop_id']: DTC.Offer(
                    price=DTC.OfferPrice(
                        basic=DTC.PriceBundle(
                            binary_price=PriceExpression(
                                price=int(o['price'] * 10**7)
                            ),
                        ),
                        dynamic_pricing=DTC.DynamicPricing(
                            type=DTC.DynamicPricing.RECOMMENDED_PRICE,
                            threshold_percent=int(o['dynamic_pricing_threshold_percent'] * 100)
                        )
                    ),
                    identifiers=DTC.OfferIdentifiers(
                        business_id=o['identifiers']['business_id'],
                        shop_id=o['identifiers']['shop_id'],
                        offer_id=o['identifiers']['offer_id'],
                        extra=DTC.OfferExtraIdentifiers(
                            market_sku_id=10003,
                            ware_md5='KXGI8T3GP_pqjgdd7HfoHQ',
                        ),
                    ),
                    meta=DTC.OfferMeta(rgb=DTC.WHITE),
                    status=DTC.OfferStatus(
                        disabled=[
                            DTC.Flag(
                                flag=False,
                                meta=DTC.UpdateMeta(
                                    source=DTC.MARKET_IDX,
                                    timestamp=create_timestamp_from_json(old_time)
                                )
                            )
                        ]
                    )
                )}
            )]
        )])

        input_topic.write(message.SerializeToString())

    yield output_topic.read(count=len(offers))


@pytest.mark.parametrize("shop_id, ssku", [
    (SHOP_ID, 'with_zero_price'),
    (SHOP_ID, 'with_wrong_dynamic_pricing_percent'),
    (SHOP_ID, 'no_classififer_good_id'),
    (SHOP_ID, 'no_classififer_magic_id'),
])
def test_offer_disabled(miner, workflow, offers, shop_id, ssku):
    offer = [offer for offer in offers if offer['identifiers']['offer_id'] == ssku and offer['identifiers']['shop_id'] == shop_id][0]
    assert_that(workflow, has_items(IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': offer['identifiers']['business_id'],
                        'offer_id': offer['identifiers']['offer_id'],
                    },
                },
                'service': IsProtobufMap({
                    offer['identifiers']['shop_id']: {
                        'identifiers': {
                            'shop_id': offer['identifiers']['shop_id'],
                            'business_id': offer['identifiers']['business_id'],
                            'offer_id': offer['identifiers']['offer_id'],
                        },
                        'status': {
                            'disabled': [
                                {
                                    'flag': True,
                                    'meta': {
                                        'source': DTC.MARKET_IDX
                                    }
                                }
                            ],
                        },
                        'resolution': {
                            'by_source': [{
                                'verdict': [{
                                    'results': [{
                                        'is_banned': True,
                                        'messages': [{
                                            'code': offer['expected_error']
                                        }]
                                    }]
                                }]
                            }]
                        }
                    },
                })
            }]
        }]})))

    metrics = miner.metrics
    assert_that(metrics, has_items(has_entries({'labels': {'sensor': 'error_count', 'error_code': offer['expected_error']}, 'value': greater_than(0)})))
