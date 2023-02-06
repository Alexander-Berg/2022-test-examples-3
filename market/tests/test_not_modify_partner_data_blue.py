# coding: utf-8

import pytest
import json
from hamcrest import assert_that

from yt.wrapper import ypath_join

from yatest.common.network import PortManager

from market.idx.pylibrary.taxes.taxes import ETaxSystem

from market.idx.feeds.feedparser.yatf.resources.delivery_calc import DeliveryCalcServer
from market.idx.feeds.feedparser.yatf.resources.ucdata_pbs import UcHTTPData

from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv
from market.idx.datacamp.yatf.matchers.matchers import IsPartnerDataNotChanged, IsEqualProtoExceptTimestamp
from market.idx.datacamp.yatf.utils import dict2tskv

from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampPartnersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.yatf.resources.msku_table import MskuExtTable
from market.idx.yatf.utils.utils import create_pb_timestamp

from market.proto.ir.UltraController_pb2 import SKUMappingResponse
from market.proto.common.common_pb2 import PriceExpression
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import (
    OfferIdentifiers,
    OfferStockInfo,
    PriceBundle,
    OfferPrice,
    OfferStatus,
    UpdateMeta,
    PartnerInfo,
    OfferStocks,
    OfferContent,
    PartnerContent,
    OfferPictures,
    PartnerPictures,
    OfferMeta,
    ProcessedSpecification,
    Flag,
    Offer as DatacampOffer,
    AVAILABLE,
    MARKET_MBO,
    BLUE,
    PreciseDimensions,
    PreciseWeight,
    StringValue,
    SourcePicture,
    SourcePictures,
)
from market.idx.datacamp.proto.offer.UnitedOffer_pb2 import (
    UnitedOffer,
    UnitedOffersBatch,
)
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage


DC_GENERATION = 10
DC_GENERATION_FOR_FEED = 123123

BUCKET_URL_PATH = 'bucketUrlPath'
SHOP_ID = 1
OFFER_ID = 'offer'
FEED_ID = 3
WAREHOUSE_ID = 147
VIRTUAL_SHOP_ID = 1001
FF_FEED_ID = 4444


@pytest.fixture(scope='module')
def offer():
    meta = UpdateMeta(
        source=MARKET_MBO,
        timestamp=create_pb_timestamp()
    )

    return UnitedOffer(
        basic=DatacampOffer(
            identifiers=OfferIdentifiers(
                offer_id=OFFER_ID,
            ),
            content=OfferContent(
                partner=PartnerContent(
                    actual=ProcessedSpecification(
                        title=StringValue(
                            value='The best offer ever',
                        ),
                        weight=PreciseWeight(
                            grams=10,
                        ),
                        dimensions=PreciseDimensions(
                            length_mkm=20,
                            width_mkm=20,
                            height_mkm=20,
                        ),
                    )
                )
            ),
            pictures=OfferPictures(
                partner=PartnerPictures(
                    original=SourcePictures(
                        source=[SourcePicture(url='http://picture1.com')]
                    )
                )
            ),
        ),
        service={
            SHOP_ID: DatacampOffer(
                identifiers=OfferIdentifiers(
                    shop_id=SHOP_ID,
                    offer_id=OFFER_ID,
                    warehouse_id=WAREHOUSE_ID,
                ),
                meta=OfferMeta(
                    rgb=BLUE,
                    ts_created=create_pb_timestamp(),
                ),
                price=OfferPrice(
                    basic=PriceBundle(
                        binary_price=PriceExpression(
                            price=int(42 * 10**7)
                        ),
                        binary_oldprice=PriceExpression(
                            price=int(50 * 10**7)
                        ),
                        vat=6,
                    ),
                    enable_auto_discounts=Flag(
                        flag=True,
                        meta=meta
                    )
                ),
                status=OfferStatus(
                    publish_by_partner=AVAILABLE
                ),
                partner_info=PartnerInfo(
                    fulfillment_feed_id=FF_FEED_ID,
                    fulfillment_virtual_shop_id=VIRTUAL_SHOP_ID,
                ),
                stock_info=OfferStockInfo(
                    market_stocks=OfferStocks(count=1),
                    partner_stocks=OfferStocks(count=100500),
                )
            )
        }
    )


@pytest.yield_fixture(scope="module")
def msku_ext_table_path():
    return ypath_join('datacamp', 'blue', 'in', 'msku_ext')


@pytest.fixture(scope='module')
def msku_ext_table_data():
    return [
        {
            'msku': 10,
            'cargo_types': [123, 456],
        }, {
            'msku': 20,
            'cargo_types': [987]
        }
    ]


def uc_data(port):
    return UcHTTPData({
        1: [
            SKUMappingResponse.SKUMapping(
                market_sku_id=111001,
                shop_sku=['ssku.valid.msku.mapping.1']
            ),
        ],
    }, port=port)


@pytest.yield_fixture(scope='module')
def uc_server():
    with PortManager() as pm:
        server = uc_data(pm.get_port())
        yield server


@pytest.fixture(scope='module')
def partner_data():
    return [
        {
            'shop_id': SHOP_ID,
            'mbi':  dict2tskv({
                'shop_id': SHOP_ID,
                'datafeed_id': FEED_ID,
                'warehouse_id': WAREHOUSE_ID,
                'tax_system': ETaxSystem.OSN.value,
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


def options_group_1():
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


@pytest.yield_fixture(scope='module')
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
            'delivery_option_groups': [options_group_1()],
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


@pytest.fixture(scope='module')
def miner_config(
        yt_server,
        log_broker_stuff,
        input_topic,
        output_topic,
        msku_ext_table_path,
        uc_server,
        yt_token,
        partner_info_table_path,
        delivery_calc_server
):
    cfg = MinerConfig()

    cfg.create_miner_initializer(
        yt_server=yt_server,
        token=yt_token.path,
        partners_table_path=partner_info_table_path,
    )

    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic, united=True)
    unpacker = cfg.create_proto_unpacker_processor(filter_duplicated_fields=False)
    adapter_converter = cfg.create_offer_adapter_converter()
    shopsdat_enricher = cfg.create_shopsdat_enricher()
    uc_enricher = cfg.create_blue_uc_enricher_processor()
    ware_md5_creator = cfg.create_ware_md5_creator()
    dc_enricher = cfg.create_delivery_calc_enricher_processor(
        delivery_calc_server,
        color='blue',
    )
    cargo_types_enricher = cfg.create_cargo_types_enricher(yt_server, yt_token.path, get_yt_prefix(), msku_ext_table_path)
    offer_validator = cfg.create_offer_validator()
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic)

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, shopsdat_enricher)
    cfg.create_link(shopsdat_enricher, uc_enricher)
    cfg.create_link(uc_enricher, ware_md5_creator)
    cfg.create_link(ware_md5_creator, dc_enricher)
    cfg.create_link(dc_enricher, cargo_types_enricher)
    cfg.create_link(cargo_types_enricher, offer_validator)
    cfg.create_link(offer_validator, writer)

    return cfg


@pytest.yield_fixture(scope='module')
def miner(
        yt_server,
        miner_config,
        input_topic,
        output_topic,
        partner_info_table_path,
        partner_data,
        delivery_calc_server,
        msku_ext_table_data,
        msku_ext_table_path,
):
    resources = {
        'miner_cfg': miner_config,
        'input_topic': input_topic,
        'output_topic': output_topic,
        'delivery_calc_server': delivery_calc_server,
        'partners_table': DataCampPartnersTable(
            yt_stuff=yt_server,
            path=partner_info_table_path,
            data=partner_data
        ),
        'msku_ext_table': MskuExtTable(
            yt_stuff=yt_server,
            path=ypath_join(get_yt_prefix(), msku_ext_table_path),
            data=msku_ext_table_data,
        ),
    }
    with MinerTestEnv(**resources) as miner:
        miner.verify()
        yield miner


def mine(input_topic, output_topic, offer):
    request = UnitedOffersBatch()
    request.offer.extend([offer])

    input_topic.write(DatacampMessage(united_offers=[request]).SerializeToString())

    return output_topic.read(count=1)


def test_multiple_mining(miner, input_topic, output_topic, offer):
    """Проверяем, что при повторном обогащении данные не изменились"""
    result = mine(input_topic, output_topic, offer)

    once_mined_offer = DatacampMessage()
    once_mined_offer.ParseFromString(result[0])
    once_mined_offer = once_mined_offer.united_offers[0].offer[0]

    result = mine(input_topic, output_topic, once_mined_offer)

    twice_mined_offer = DatacampMessage()
    twice_mined_offer.ParseFromString(result[0])
    twice_mined_offer = twice_mined_offer.united_offers[0].offer[0]

    assert_that(twice_mined_offer, IsPartnerDataNotChanged(offer), 'Partner data must not be changed in miner')
    assert_that(twice_mined_offer, IsEqualProtoExceptTimestamp(once_mined_offer))
