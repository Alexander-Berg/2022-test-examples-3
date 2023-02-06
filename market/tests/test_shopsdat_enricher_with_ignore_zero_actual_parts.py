# coding: utf-8

import pytest
from hamcrest import assert_that

from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages
from market.idx.datacamp.yatf.utils import dict2tskv

from market.idx.yatf.matchers.protobuf_matchers import IsProtobufMap
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampPartnersTable

from market.idx.datacamp.proto.offer import UnitedOffer_pb2 as DTC
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage

from market.pylibrary.proto_utils import message_from_data


BUSINESS_ID_FROM_ALLOWLIST = 1
BUSINESS_ID_FROM_BLOCKLIST = 2
OFFER_ID = "offer"
SHOP_ID = 123
FEED_ID = 200398708
WAREHOUSE_ID = 145
META={'rgb': DTC.WHITE}
PARTNER_DATA = [
    {
        'shop_id': SHOP_ID,
        'mbi': dict2tskv({
            'shop_id': SHOP_ID,
            'business_id': BUSINESS_ID_FROM_ALLOWLIST,
            'datafeed_id': FEED_ID,
            'warehouse_id': WAREHOUSE_ID,
            'united_catalog_status': 'SUCCESS',
            'is_site_market': 'true'
        }),
        'status': 'publish'
    },
]


@pytest.fixture(scope='module')
def miner_config(yt_server, log_broker_stuff, input_topic, output_topic, yt_token, partner_info_table_path):
    cfg = MinerConfig()

    cfg.create_miner_initializer(
        yt_server=yt_server,
        token=yt_token.path,
        partners_table_path=partner_info_table_path,
    )

    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic, united=True)
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic)
    unpacker = cfg.create_proto_unpacker_processor(
        ignore_zero_actual_parts=False,
        ignore_zero_actual_parts_allowlist=[BUSINESS_ID_FROM_ALLOWLIST]
    )
    adapter_converter = cfg.create_offer_adapter_converter()
    shopsdat_enricher = cfg.create_shopsdat_enricher(
        color='white',
        ignore_zero_actual_parts=False,
        ignore_zero_actual_parts_allowlist=[BUSINESS_ID_FROM_ALLOWLIST]
    )
    blue_shopsdat_enricher = cfg.create_shopsdat_enricher(
        color='blue',
        ignore_zero_actual_parts=False,
        ignore_zero_actual_parts_allowlist=[BUSINESS_ID_FROM_ALLOWLIST]
    )

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


def _make_offer(business_id=None, offer_id=None, shop_id=None, feed_id=None, warehouse_id=None, meta=None):
    identifiers = {}
    if business_id is not None:
        identifiers['business_id'] = business_id
    if offer_id is not None:
        identifiers['offer_id'] = offer_id
    if shop_id is not None:
        identifiers['shop_id'] = shop_id
    if feed_id is not None:
        identifiers['feed_id'] = feed_id
    if warehouse_id is not None:
        identifiers['warehouse_id'] = warehouse_id
    result = {
        'identifiers': identifiers
    }
    if meta is not None:
        result['meta'] = meta
    return result


def test_shopsdat_with_business_id_from_allowlist_without_warehouse(miner, input_topic, output_topic):
    """Проверяем, что если business_id принадлежит к allowlist, то офферу будет установлен
    склад на основе информации из shopsdat"""
    request = message_from_data({
        'united_offers': [{
            'offer': [{
                'basic': _make_offer(BUSINESS_ID_FROM_ALLOWLIST, OFFER_ID),
                'service': {
                    SHOP_ID: _make_offer(BUSINESS_ID_FROM_ALLOWLIST, OFFER_ID, SHOP_ID, FEED_ID, warehouse_id=0, meta=META),
                }
            }]
        }]
    }, DatacampMessage())

    input_topic.write(request.SerializeToString())
    data = output_topic.read(count=1)

    assert_that(data, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'basic': _make_offer(BUSINESS_ID_FROM_ALLOWLIST, OFFER_ID),
                'service': IsProtobufMap({
                    SHOP_ID: _make_offer(BUSINESS_ID_FROM_ALLOWLIST, OFFER_ID, SHOP_ID, FEED_ID, WAREHOUSE_ID, meta=META),
                })
            }]
        }]
    }]))


def test_shopsdat_with_business_id_from_allowlist_with_warehouse(miner, input_topic, output_topic):
    """Проверяем, что если business_id принадлежит к allowlist и у оффера уже установлен склад, то
    он не будет изменён"""
    request = message_from_data({
        'united_offers': [{
            'offer': [{
                'basic': _make_offer(BUSINESS_ID_FROM_ALLOWLIST, OFFER_ID),
                'service': {
                    SHOP_ID: _make_offer(BUSINESS_ID_FROM_ALLOWLIST, OFFER_ID, SHOP_ID, FEED_ID, warehouse_id=333, meta=META),
                }
            }]
        }]
    }, DatacampMessage())

    input_topic.write(request.SerializeToString())
    data = output_topic.read(count=1)

    assert_that(data, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'basic': _make_offer(BUSINESS_ID_FROM_ALLOWLIST, OFFER_ID),
                'service': IsProtobufMap({
                    SHOP_ID: _make_offer(BUSINESS_ID_FROM_ALLOWLIST, OFFER_ID, SHOP_ID, FEED_ID, warehouse_id=333, meta=META),
                })
            }]
        }]
    }]))


def test_shopsdat_with_business_id_from_blocklist_without_warehouse(miner, input_topic, output_topic):
    """Проверяем, что если business_id НЕ принадлежит к allowlist, то склад не будет установлен"""
    request = message_from_data({
        'united_offers': [{
            'offer': [{
                'basic': _make_offer(BUSINESS_ID_FROM_BLOCKLIST, OFFER_ID),
                'service': {
                    1: _make_offer(BUSINESS_ID_FROM_BLOCKLIST, OFFER_ID, 1, FEED_ID, warehouse_id=0, meta=META),
                }
            }]
        }]
    }, DatacampMessage())

    input_topic.write(request.SerializeToString())
    data = output_topic.read(count=1)

    assert_that(data, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'basic': _make_offer(BUSINESS_ID_FROM_BLOCKLIST, OFFER_ID),
                'service': IsProtobufMap({
                    1: _make_offer(BUSINESS_ID_FROM_BLOCKLIST, OFFER_ID, 1, FEED_ID, warehouse_id=0, meta=META),
                })
            }]
        }]
    }]))
