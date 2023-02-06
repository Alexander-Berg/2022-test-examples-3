# coding: utf-8

import pytest
from hamcrest import assert_that

from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv
from market.idx.datacamp.yatf.utils import dict2tskv
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobufMap

from market.idx.datacamp.proto.offer.UnitedOffer_pb2 import UnitedOffersBatch, UnitedOffer
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import OfferIdentifiers
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import Offer as DatacampOffer
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import OfferMeta, WHITE
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage

from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampPartnersTable


MBI_DATA_1 = '\n'.join([
    dict2tskv({
        'shop_id': 10300813,
        'business_id': 10300813,
        'datafeed_id': 200403138,
        'warehouse_id': 147,
        'united_catalog_status': 'SUCCESS',
        'is_site_market': 'true'
    }),
    dict2tskv({
        'shop_id': 10300813,
        'business_id': 10300813,
        'datafeed_id': 200403139,
        'warehouse_id': 145,
        'shopname': 'shopname',
        'supplier_type': 1,  # 1P
        'ff_program': 'REAL',
        'ff_virtual_id': 11111,
        'vat': 7,
        'autobroker_enabled': True,
        'blue_status': 'REAL',
        'united_catalog_status': 'SUCCESS',
        'is_site_market': 'true'
    })])

MBI_DATA_2 = dict2tskv({
    'shop_id': 10300814,
    'business_id': 10300814,
    'datafeed_id': 200403140,
    'warehouse_id': 145,
    'shopname': 'shopname_10300814',
    'supplier_type': 3,
    'ff_program': 'NO',
    'vat': 7,
    'autobroker_enabled': True,
    'blue_status': 'REAL',
    'united_catalog_status': 'SUCCESS',
    'is_site_market': 'true'
})

MBI_DATA_3 = dict2tskv([
    {
        'shop_id': 10300815,
        'datafeed_id': 200403141,
        'shopname': 'shopname_10300815',
        'vat': 7,
        'business_id': 10300815,
        'autobroker_enabled': True,
        'united_catalog_status': 'SUCCESS',
        'is_site_market': 'true'
    },
    {
        'shop_id': 10300815,
        'datafeed_id': 200403142,
        'business_id': 10300815,
        'shopname': 'shopname_10300815',
        'vat': 7,
        'autobroker_enabled': True,
        'is_default': True,
        'united_catalog_status': 'SUCCESS',
        'is_site_market': 'true'
    },
])

PARTNER_DATA = [
    {
        'shop_id': 10300813,
        'mbi': MBI_DATA_1,
        'status': 'publish'
    },
    {
        'shop_id': 10300814,
        'mbi': MBI_DATA_2,
        'status': 'publish'
    },
    {
        'shop_id': 11111,
        'mbi': dict2tskv({
            'shop_id': 11111,
            'business_id': 11111,
            'datafeed_id': 22222,
            'united_catalog_status': 'SUCCESS',
            'is_site_market': 'true'
        }),
        'status': 'publish'
    },
    {
        'shop_id': 10296180,
        'mbi': dict2tskv({
            'shop_id': 10296180,
            'business_id': 10296180,
            'datafeed_id': 200398708,
            'warehouse_id': 145,
            'united_catalog_status': 'SUCCESS',
            'is_site_market': 'true'
        }),
        'status': 'publish'
    },
    {
        'shop_id': 10300815,
        'mbi': MBI_DATA_3,
        'status': 'publish'
    }
]


@pytest.fixture(scope='module')
def miner_config(yt_server, log_broker_stuff, input_topic, output_topic, yt_token, partner_info_table_path):
    cfg = MinerConfig()

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
    blue_shopsdat_enricher = cfg.create_shopsdat_enricher(color='blue')

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, shopsdat_enricher)
    cfg.create_link(shopsdat_enricher, blue_shopsdat_enricher)
    cfg.create_link(blue_shopsdat_enricher, writer)

    return cfg


@pytest.yield_fixture(scope='module')
def miner(yt_server, miner_config, input_topic, output_topic, partner_info_table_path):
    resources = {
        'miner_cfg': miner_config,
        'input_topic': input_topic,
        'output_topic': output_topic,
        'partners_table': DataCampPartnersTable(
            yt_stuff=yt_server,
            path=partner_info_table_path,
            data=PARTNER_DATA
        )
    }
    with MinerTestEnv(**resources) as miner:
        miner.verify()
        yield miner


def write_read_offer_lbk(offer_data, input_topic, output_topic):
    offer = DatacampOffer(
        identifiers=OfferIdentifiers(
            business_id=offer_data['identifiers']['business_id'],
            shop_id=offer_data['identifiers']['shop_id'],
            offer_id=offer_data['identifiers']['offer_id'],
        ),
        meta=OfferMeta(rgb=WHITE),
        status=DTC.OfferStatus(
            united_catalog=DTC.Flag(
                flag=True,
            )
        )
    )

    if 'warehouse_id' in offer_data['identifiers']:
        offer.identifiers.warehouse_id = offer_data['identifiers']['warehouse_id']

    if 'feed_id' in offer_data['identifiers']:
        offer.identifiers.feed_id = offer_data['identifiers']['feed_id']

    if 'partner_info' in offer_data:
        offer.partner_info.supplier_id = offer_data['partner_info']['supplier_id']
        offer.partner_info.supplier_name = offer_data['partner_info']['supplier_name']
        offer.partner_info.supplier_type = offer_data['partner_info']['supplier_type']

    message = DatacampMessage(united_offers=[UnitedOffersBatch(
        offer=[UnitedOffer(
            basic=DatacampOffer(
                identifiers=OfferIdentifiers(
                    business_id=offer_data['identifiers']['business_id'],
                    offer_id=offer_data['identifiers']['offer_id'],
                ),
            ),
            service={offer_data['identifiers']['shop_id']: offer}
        )]
    )])
    input_topic.write(message.SerializeToString())
    return output_topic.read(count=1)


@pytest.mark.parametrize('input, expected, mbi_data', [
    (
        {
            'identifiers': {
                'shop_id': 10300813,
                'business_id': 10300813,
                'offer_id': 'pushCarp100256632255',
                'warehouse_id': 145,
            },
        },
        {
            'identifiers': {
                'feed_id': 200403139,
                'shop_id': 10300813,
                'business_id': 10300813,
                'offer_id': 'pushCarp100256632255',
            },
            'partner_info': {
                'supplier_name': 'shopname',
                'supplier_id': 10300813,
                'supplier_type': 1,
                'fulfillment_virtual_shop_id': 11111,
                'fulfillment_feed_id': 22222,
                'autobroker_enabled': True,
                'is_blue_offer': True,
                'is_fulfillment': True,
            }
        },
        MBI_DATA_1,
    ),
    (
        {
            'identifiers': {
                'shop_id': 10300814,
                'offer_id': 'sample.offer',
                'business_id': 10300814,
                'warehouse_id': 145,
            },
        },
        {
            'identifiers': {
                'feed_id': 200403140,
                'shop_id': 10300814,
                'business_id': 10300814,
                'offer_id': 'sample.offer',
            },
            'partner_info': {
                'supplier_name': 'shopname_10300814',
                'supplier_id': 10300814,
                'supplier_type': 3,
                'fulfillment_virtual_shop_id': None,
                'fulfillment_feed_id': None,
                'is_blue_offer': True,
                'is_fulfillment': False,
            }
        },
        MBI_DATA_2,
    ),
])
def test_shopsdat_from_table(miner, input_topic, output_topic, input, expected, mbi_data):
    """ Проверяем, что miner проставляется поля из шопсдата в оффер """
    data = write_read_offer_lbk(input, input_topic, output_topic)
    assert_that(data[0], IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': expected['identifiers']['business_id'],
                        'offer_id': expected['identifiers']['offer_id'],
                    },
                    'partner_info': None,
                },
                'service': IsProtobufMap({
                    expected['identifiers']['shop_id']: {
                        'identifiers': {
                            'business_id': expected['identifiers']['business_id'],
                            'offer_id': expected['identifiers']['offer_id'],
                            'shop_id': expected['identifiers']['shop_id'],
                            'feed_id': expected['identifiers']['feed_id'],
                        },
                        'partner_info': expected['partner_info'],
                    },
                })
            }]
        }]
    }))


def test_shopsdat_from_cache(miner, input_topic, output_topic):
    """Проверяем, что miner обогащает оффер, даже для тех офферов,
    где нет feed_id, но оно есть в кеше"""
    write_offer = {
        'identifiers': {
            'shop_id': 10296180,
            'business_id': 10296180,
            'offer_id': 'dropPushCardTry3',
            'warehouse_id': 145,
        }
    }
    data = write_read_offer_lbk(write_offer, input_topic, output_topic)

    assert_that(data[0], IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': 10296180,
                        'offer_id': 'dropPushCardTry3',
                    },
                },
                'service': IsProtobufMap({
                    10296180: {
                        'identifiers': {
                            'shop_id': 10296180,
                            'business_id': 10296180,
                            'offer_id': 'dropPushCardTry3',
                            'warehouse_id': 145,
                            'feed_id': 200398708
                        },
                    },
                })
            }]
        }]})
    )


@pytest.mark.parametrize(
    'input, expected, mbi_data', [
        (
            {
                'identifiers': {
                    'business_id': 10300815,
                    'shop_id': 10300815,
                    'offer_id': 'sample.offer01',
                },
                'partner_info': {
                    'supplier_name': 'shopname_10300814',
                    'supplier_id': 10300814,
                    'supplier_type': 3,
                }
            },
            {
                'identifiers': {
                    'business_id': 10300815,
                    'shop_id': 10300815,
                    'offer_id': 'sample.offer01',
                    'feed_id': 200403142,
                },
                'partner_info': {
                    'supplier_name': None,
                    'supplier_id': None,
                    'supplier_type': None,
                }
            },
            MBI_DATA_3,
        ),
        (
            {
                'identifiers': {
                    'business_id': 10300815,
                    'shop_id': 10300815,
                    'offer_id': 'sample.offer02',
                    'feed_id': 200403141
                },
            },
            {
                'identifiers': {
                    'business_id': 10300815,
                    'shop_id': 10300815,
                    'offer_id': 'sample.offer02',
                    'feed_id': 200403141,
                },
                'partner_info': {}
            },
            MBI_DATA_3,
        )
    ],
    ids=[
        'white_offer_without_feed_id',
        'white_offer_with_feed_id',
    ],
)
def test_white_shopsdat_from_table(miner, input_topic, output_topic, input, expected, mbi_data):
    """ Проверяем, что miner проставляется поля из шопсдата в оффер и делает это по дефолтному фиду"""
    data = write_read_offer_lbk(input, input_topic, output_topic)

    assert_that(data[0], IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': expected['identifiers']['business_id'],
                        'offer_id': expected['identifiers']['offer_id'],
                    },
                },
                'service': IsProtobufMap({
                    expected['identifiers']['shop_id']: {
                        'identifiers': {
                            'business_id': expected['identifiers']['business_id'],
                            'offer_id': expected['identifiers']['offer_id'],
                            'shop_id': expected['identifiers']['shop_id'],
                            'feed_id': expected['identifiers']['feed_id'],
                        },
                        'partner_info': expected['partner_info'],
                    },
                })
            }]
        }]
    }))
