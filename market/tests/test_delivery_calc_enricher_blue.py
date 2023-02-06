# coding: utf-8

import pytest
import json
import tempfile
import os

from hamcrest import assert_that, equal_to

from market.idx.datacamp.yatf.utils import dict2tskv, create_meta
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampPartnersTable
from yatest.common.network import PortManager

from market.idx.feeds.feedparser.yatf.resources.delivery_calc import DeliveryCalcServer

from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import (
    OfferIdentifiers,
    PartnerInfo,
    Offer as DatacampOffer,
    OfferContent,
    MarketMasterData,
    PreciseDimensions,
    PreciseWeight,
    BLUE,
)
from market.idx.datacamp.proto.offer.UnitedOffer_pb2 import (
    UnitedOffer,
    UnitedOffersBatch,
)
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages
from market.idx.yatf.matchers.protobuf_matchers import IsProtobuf, IsProtobufMap

import market.proto.delivery.delivery_calc.delivery_calc_pb2 as DeliveryCalc

import market.proto.delivery.LMS_pb2 as LMSProto

from market.pylibrary.snappy_protostream import SnappyProtoWriter


DC_GENERATION = 10
DC_GENERATION_FOR_FEED = 123123
DC_EMPTY_FEED_ID = -1

BUCKET_URL_PATH = 'bucketUrlPath'
SHOP_ID = 1
FEED_ID = 3
WAREHOUSE_ID = 147
CROSSDOCK_WAREHOUSE_ID = 65432
VIRTUAL_SHOP_ID = 1001
FF_FEED_ID = 4444

LMS_BASE_DIR = tempfile.mkdtemp()
LMS_FILEPATH = os.path.join(LMS_BASE_DIR, 'lms.pbuf.sn')


@pytest.fixture(scope='module')
def shop_id():
    return SHOP_ID


@pytest.fixture(scope='module')
def feed_id():
    return FEED_ID


@pytest.fixture(scope='module')
def offers(shop_id, feed_id):
    return [
        {
            'shop_id': shop_id,
            'offer_id': '2',
            'feed_id': feed_id,
            'warehouse_id': CROSSDOCK_WAREHOUSE_ID,

            'type': 5,  # general offer

            'price': 123.4,
            'default_price': None,

            'width': 100.0,
            'height': 200.0,
            'length': 300.0,
            'weight': 1.0
        },
        # Оффер с дефолной базовой ценой
        # Запрос в КД должен работать корректно с отнаследованной базовой ценой
        {
            'shop_id': shop_id,
            'offer_id': 'OfferWithDefaultPrice',
            'feed_id': feed_id,
            'warehouse_id': CROSSDOCK_WAREHOUSE_ID,

            'type': 5,  # general offer

            'price': None,
            'default_price': 123.4,

            'width': 100.0,
            'height': 200.0,
            'length': 300.0,
            'weight': 1.0
        }
    ]


@pytest.fixture(scope='module')
def partner_data(shop_id, feed_id):
    mbi = {
        'shop_id': shop_id,
        'datafeed_id': feed_id,
        'warehouse_id': CROSSDOCK_WAREHOUSE_ID,
        'ff_program': 'REAL',
        'is_online': True,
        'direct_shipping': False,
    }
    return [
        {
            'shop_id': shop_id,
            'mbi':  dict2tskv(mbi),
            'status': 'publish'
        },
        {
            'shop_id': VIRTUAL_SHOP_ID,
            'mbi':  dict2tskv({
                'shop_id': VIRTUAL_SHOP_ID,
                'datafeed_id': FF_FEED_ID,
                'market_delivery_courier': True,
            }),
            'status': 'publish'
        },
    ]


def lms_file(fd):
    warehouseToWarehouseRel = LMSProto.WarehouseToWarehouseInfo()
    warehouseToWarehouseRel.warehouse_from_id = CROSSDOCK_WAREHOUSE_ID
    warehouseToWarehouseRel.warehouse_to_id = WAREHOUSE_ID
    warehouseToWarehouseRel.is_active = True

    warehouseFrom = LMSProto.WarehouseInfo()
    warehouseFrom.id = CROSSDOCK_WAREHOUSE_ID
    warehouseFrom.home_region = 213

    warehouseTo = LMSProto.WarehouseInfo()
    warehouseTo.id = WAREHOUSE_ID
    warehouseTo.home_region = 213

    LMSData = LMSProto.MetaInfo()

    LMSData.calendar_meta_info.start_date = "17.06.1985"
    LMSData.calendar_meta_info.depth = 84

    LMSData.warehouses.extend([warehouseFrom, warehouseTo])
    LMSData.warehouses_to_warehouses.extend([warehouseToWarehouseRel])

    with SnappyProtoWriter(fd, "MLMS") as writer:
        writer.write(LMSData)


def bucket_1():
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


def bucket_without_options():
    return {
        'delivery_opt_bucket_id': 2,
        'currency': 'RUR',
        'carrier_ids': [105],
        'program': 'REGULAR_PROGRAM',
        'delivery_option_group_regs': [{
            'region': '213',
            'option_type': 'UNSPECIFIC_OPTION'
        }]
    }


def mardo_bucket_without_connection_to_offer():
    # Бакеты МарДо всегда линкуются к фиду. Даже, если он не подключен ни к одному оферу этого фида.
    # Эти бакеты потом потребуются для place=actual_delivery
    return {
        'delivery_opt_bucket_id': 3,
        'currency': 'RUR',
        'carrier_ids': [100],
        'program': 'MARKET_DELIVERY_PROGRAM',
        'delivery_option_group_regs': [{
            'region': '213',
            'option_type': 'UNSPECIFIC_OPTION'
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


def unused_pickup_bucket():
    # Бакеты МарДо всегда линкуются к фиду. Даже, если он не подключен ни к одному оферу этого фида.
    # Эти бакеты потом потребуются для place=actual_delivery
    return {
        'bucket_id': 14,
        'program': 'MARKET_DELIVERY_PROGRAM',
        'currency': 'RUR',
        'carrier_ids': [15],
        'delivery_option_group_outlets': [{
            'outlet_id': 2001,
            'option_group_id': 1,
        }, {
            'outlet_id': 2002,
            'option_group_id': 1,
        }, {
            'outlet_id': 2003,
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


def bucket_red_program():
    return {
        'delivery_opt_bucket_id': 1,
        'currency': 'RUR',
        'carrier_ids': [99, 105],
        'program': 'MARKET_DELIVERY_RED_PROGRAM',
        'delivery_option_group_regs': [{
            'region': '2',
            'delivery_opt_group_id': 1,
            'option_type': 'NORMAL_OPTION'
        }]
    }


def unused_post_bucket():
    # Бакеты МарДо всегда линкуются к фиду. Даже, если он не подключен ни к одному оферу этого фида.
    # Эти бакеты потом потребуются для place=actual_delivery
    return {
        'bucket_id': 15,
        'program': 'MARKET_DELIVERY_PROGRAM',
        'currency': 'RUR',
        'carrier_ids': [20],
        'delivery_option_group_post_outlets': [{
            'post_code': 620200,
            'option_group_id': 1,
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


def options_group_2():
    return {
        'delivery_option_group_id': 2,
        'payment_types': ['CASH_ON_DELIVERY'],
        'delivery_options': [{
            'delivery_cost': 10,
            'min_days_count': 4,
            'max_days_count': 5,
            'order_before': 24,
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
                bucket_1(),
                bucket_red_program(),
                bucket_without_options(),
                mardo_bucket_without_connection_to_offer()
            ],
            'delivery_option_groups': [options_group_1(), options_group_2()],
            'pickup_buckets': [pickup_bucket(), unused_pickup_bucket()],
            'post_buckets': [post_bucket(), unused_post_bucket()],
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
        }] * 2  # (для обоих офферов)
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
        lms_file_path=LMS_FILEPATH
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
    with open(LMS_FILEPATH, 'wb') as fd:
        lms_file(fd)

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
def lbk_sender(miner, offers, input_topic):
    request = UnitedOffersBatch()
    for o in offers:
        offer = UnitedOffer(
            basic=DatacampOffer(
                identifiers=OfferIdentifiers(
                    offer_id=o['offer_id']
                ),
                content=OfferContent(
                    master_data=MarketMasterData(
                        dimensions=PreciseDimensions(
                            length_mkm=int(o['length'])*10000,
                            width_mkm=int(o['width'])*10000,
                            height_mkm=int(o['height'])*10000
                        ),
                        weight_gross=PreciseWeight(
                            value_mg=int(o['weight'])*1000000
                        )
                    )
                )
            ),
            service={
                o['shop_id']: DatacampOffer(
                    identifiers=OfferIdentifiers(
                        shop_id=o['shop_id'],
                        offer_id=o['offer_id'],
                        feed_id=o['feed_id'],
                        warehouse_id=o['warehouse_id'],
                    ),
                    meta=create_meta(10, BLUE),
                    partner_info=PartnerInfo(
                        fulfillment_feed_id=FF_FEED_ID,
                        fulfillment_virtual_shop_id=VIRTUAL_SHOP_ID,
                        # fulfillment_market_delivery_courier=True,
                        # ff_program='REAL',
                    ),
                )
            }
        )
        if o['price']:
            offer.service[o['shop_id']].price.basic.binary_price.price = int(o['price'] * 10**7)
        if o['default_price']:
            offer.basic.price.basic.binary_price.price = int(o['default_price'] * 10**7)

        request.offer.extend([offer])

    input_topic.write(DatacampMessage(united_offers=[request]).SerializeToString())


@pytest.fixture(scope='function')
def processed_offers(lbk_sender, miner, output_topic):
    data = output_topic.read(count=1)
    return data


@pytest.fixture(scope='module')
def expected_meta_programs():
    programs = ['market_delivery']
    return programs


@pytest.fixture(scope='module')
def expected_offer_programs():
    programs = [DeliveryCalc.REGULAR_PROGRAM, DeliveryCalc.MARKET_DELIVERY_PROGRAM]
    return programs


@pytest.fixture(scope='module')
def expected_meta_path(feed_id, expected_meta_programs):
    def make_program(program_name):
        return "program={}".format(program_name)

    programs = '&'.join([make_program(program) for program in expected_meta_programs])

    def make_path(feed_id, programs):
        return '/feedDeliveryOptionsMeta?feed-id={feed_id}&{programs}'.format(
            feed_id=feed_id,
            programs=programs
        )

    return make_path(feed_id, programs)


def test_dc_requests(
    miner,
    processed_offers,
    offers,
    expected_offer_programs,
    expected_meta_path
):
    """Проверяем, что майнер посылал правильные запросы к калькулятору доставки во время обогащения офера
    """
    dc_requests = miner.resources['delivery_calc_server'].GetRequests()

    assert_that(len(dc_requests), equal_to(3))

    assert_that(dc_requests[0], equal_to((expected_meta_path, None)))

    # вторым запросом скачиваем бакет, так как его нет в майнере
    assert_that(dc_requests[1], equal_to(('/{path}'.format(path=BUCKET_URL_PATH), None)))

    # далее идут пооферные запросы
    expected_offers = [
        {
            'offer_id': o['offer_id'],
            'price': o['price'] or o['default_price'],
            'weight': o['weight'],
            'height': o['height'],
            'width': o['width'],
            'length': o['length'],
            'min_quantity': 1,
            'price_map': [{
                'currency': 'RUR',
                'value': o['price'] or o['default_price'],
            }],
            'program_type': expected_offer_programs,
        } for o in offers
    ]
    expected_offer_request = {
        'feed_id':  offers[0]['feed_id'],
        'generation_id': DC_GENERATION,
        'warehouse_id': WAREHOUSE_ID,
        'offers': expected_offers
    }
    assert_that(dc_requests[2][0], equal_to('/feedOffers'))
    assert_that(dc_requests[2][1], IsProtobuf(expected_offer_request), '/feedOffers has unexpected body')


def test_dc_enricher(processed_offers, offers):
    """Проверяем, что майнер корректно обработал ответ калькулятора доставки
    """

    calculator = {
        'delivery_calc_generation': 1,  # дефолтное значение поколения - 1
        'delivery_bucket_ids': [1, 2],
        'pickup_bucket_ids': [4],
        'post_bucket_ids': [5],
        'fulfillment_delivery_calc': [{
            'Generation': 1,
            'BucketIds': [1],  # bucketId = 2 не участвует в маркетной доставке
            'Id': FF_FEED_ID,
        }],
        'real_deliverycalc_generation': DC_GENERATION_FOR_FEED,
    }

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
                            'calculator': calculator,
                            'market': {
                                'calculator': calculator,
                                'use_yml_delivery': {
                                    'flag': False
                                }
                            },
                            'partner': {
                                'actual': {
                                    'pickup': {
                                        'flag': True
                                    },
                                    'available': {
                                        'flag': True
                                    },
                                    'delivery': {
                                        'flag': True
                                    },
                                    'store': {
                                        'flag': True
                                    }
                                }
                            },
                            'delivery_info': {
                                'use_yml_delivery': False,
                                'has_delivery': True,
                                'pickup': True,
                                'available': True,
                                'real_deliverycalc_generation': DC_GENERATION_FOR_FEED,
                            },
                        }
                    }
                })
            } for offer in offers]
        }]
    }]))
