# coding: utf-8

import pytest
import json
import os

from hamcrest import assert_that, equal_to

from market.idx.datacamp.yatf.utils import dict2tskv
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampPartnersTable
from yatest.common.network import PortManager

from market.idx.feeds.feedparser.yatf.resources.delivery_calc import DeliveryCalcServer

from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobuf, IsProtobufMap

from market.idx.datacamp.proto.offer.UnitedOffer_pb2 import UnitedOffersBatch, UnitedOffer
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import (
    DeliveryInfo,
    MarketContent,
    OfferContent,
    OfferDelivery,
    OfferIdentifiers,
    OriginalSpecification,
    PartnerContent,
    PreciseDimensions,
    PreciseWeight,
    OfferMeta,
    WHITE,
    ProcessedSpecification
)
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import Offer as DatacampOffer
from market.idx.datacamp.proto.category.PartnerCategory_pb2 import PartnerCategory
import market.proto.delivery.delivery_calc.delivery_calc_pb2 as DeliveryCalc
import market.proto.ir.UltraController_pb2 as UC

from market.idx.yatf.resources.categories_dimensions import CategoriesDimensions

# ----------------------------- prepare ------------------------------

DC_GENERATION = 10
SHOP_ID = 999
BUSINESS_ID = 999
FEED_ID = 888

CATEGORIES_DIMENSIONS_FILE = {
    'category_id': 100500,
    'weight': 123.23,
    'height': 111,
    'length': 222,
    'width': 333
}

DIMENSIONS_FROM_DC = {
    'weight': 444,
    'height': 555,
    'length': 666,
    'width': 777
}

test_data = [
    {  # проверяем сохранение сложного ответа КД + отправку в запросе дефолтного pickup=true
        'id': '1',
        'price': 123,
        'default_price': None,
        'dimensions': '10/12/15',
        'category': '1\\2\\3',
        'store': False,
        'length_mkm': 100000,  # в микрометрах
        'width_mkm': 120000,
        'height_mkm': 150000,

        'courier_buckets_info': [
            {'bucket_id': 125, 'cost_modifiers_ids': [7, 8], 'time_modifiers_ids': [9, 10], 'services_modifiers_ids': [], 'region_availability_modifiers_ids': [11]},
            {'bucket_id': 126, 'cost_modifiers_ids': [], 'time_modifiers_ids': [], 'services_modifiers_ids': [], 'region_availability_modifiers_ids': [8]},
        ],
        'pickup_buckets_info': [
            {'bucket_id': 127, 'cost_modifiers_ids': [8, 15], 'time_modifiers_ids': [], 'services_modifiers_ids': [2, 3], 'region_availability_modifiers_ids': [11, 16]},
            {'bucket_id': 128, 'cost_modifiers_ids': [], 'time_modifiers_ids': [], 'services_modifiers_ids': [], 'region_availability_modifiers_ids': [8]},
        ],
        'post_buckets_info': [
            {'bucket_id': 127, 'cost_modifiers_ids': [], 'time_modifiers_ids': [], 'services_modifiers_ids': [], 'region_availability_modifiers_ids': []},
        ],

        'expected_with_dc_dimensions': {
            'weight': DIMENSIONS_FROM_DC['weight'],
            'length': 10.0,  # в сантиметрах
            'width': 12.0,
            'height': 15.0,
            'delivery_bucket_ids': [1, 2],
            'pickup_bucket_ids': [4],
            'post_bucket_ids': [5],
            'generation_id': DC_GENERATION,
            'categories': [1, 2, 3],
        },
        'expected_without_dc_dimensions': {
            'weight': CATEGORIES_DIMENSIONS_FILE['weight'],
            'length': 10.0,
            'width': 12.0,
            'height': 15.0,
            'categories': [1, 2, 3],
        },
        'expected_pickup': True,
    },
    {  # оффер для проверки использования ВГ из файла категорий для авторасчета + отправка дефолтного store=true
        'id': '2',
        'price': 222,
        'default_price': None,
        'category': '1',
        'enriched_category': CATEGORIES_DIMENSIONS_FILE['category_id'],
        'pickup': True,
        'available': False,

        'expected_with_dc_dimensions': {
            'weight': DIMENSIONS_FROM_DC['weight'],
            'length': DIMENSIONS_FROM_DC['length'],
            'width': DIMENSIONS_FROM_DC['width'],
            'height': DIMENSIONS_FROM_DC['height'],
            'categories': [1],
        },

        'expected_without_dc_dimensions': {
            'weight': CATEGORIES_DIMENSIONS_FILE['weight'],
            'length': CATEGORIES_DIMENSIONS_FILE['length'],
            'width': CATEGORIES_DIMENSIONS_FILE['width'],
            'height': CATEGORIES_DIMENSIONS_FILE['height'],
            'categories': [1],
        },
        'expected_store': True,
    },
    {  # оффер для проверки случая, когда все габариты берутся из данных партнера
        'id': '3',
        'price': 333,
        'default_price': None,
        'category': '50',
        'pickup': False,
        'store': True,
        'available': False,
        'length_mkm': 100000,  # в микрометрах
        'width_mkm': 120000,
        'height_mkm': 150000,
        'weight': 1500,  # в граммах

        'expected_with_dc_dimensions': {
            'weight': 1.5,  # в килограммах
            'length': 10,  # в сантиметрах
            'width': 12,
            'height': 15,
            'categories': [50],
        },

        'expected_without_dc_dimensions': {
            'weight': 1.5,  # в килограммах
            'length': 10,  # в сантиметрах
            'width': 12,
            'height': 15,
            'categories': [50],
        }
    },
    {  # оффер c дефолтной базовой ценой (параметры доставки должны получить такие же, как для оффера "2")
        'id': '4',
        'price': None,
        'default_price': 222,
        'category': '1',
        'enriched_category': CATEGORIES_DIMENSIONS_FILE['category_id'],
        'pickup': True,
        'available': False,

        'expected_with_dc_dimensions': {
            'weight': DIMENSIONS_FROM_DC['weight'],
            'length': DIMENSIONS_FROM_DC['length'],
            'width': DIMENSIONS_FROM_DC['width'],
            'height': DIMENSIONS_FROM_DC['height'],
            'categories': [1],
        },

        'expected_without_dc_dimensions': {
            'weight': CATEGORIES_DIMENSIONS_FILE['weight'],
            'length': CATEGORIES_DIMENSIONS_FILE['length'],
            'width': CATEGORIES_DIMENSIONS_FILE['width'],
            'height': CATEGORIES_DIMENSIONS_FILE['height'],
            'categories': [1],
        },
        'expected_store': True,
    }
]


@pytest.fixture(params=[
    {
        'meta': {
            'generationId': DC_GENERATION,
            'shopOffersAverageWeightDimensions': {
                'length': DIMENSIONS_FROM_DC['length'],
                'width': DIMENSIONS_FROM_DC['width'],
                'height': DIMENSIONS_FROM_DC['height'],
                'weight': DIMENSIONS_FROM_DC['weight']
            },
            'currencies': ['RUR'],
        },
        'expected': 'expected_with_dc_dimensions',
    },
    {
        'meta': {
            'generationId': DC_GENERATION,
            'currencies': ['RUR'],
        },
        'expected': 'expected_without_dc_dimensions',
    }],
    ids=[
        'meta_with_dimensions',
        'meta_without_dimensions',
    ])
def test_data_params(request):
    return request.param


@pytest.fixture(scope='function')
def categories_dimensions(tmpdir):
    catdim = CategoriesDimensions()
    catdim.add_record(CATEGORIES_DIMENSIONS_FILE['category_id'],
                      weight=CATEGORIES_DIMENSIONS_FILE['weight'],
                      length=CATEGORIES_DIMENSIONS_FILE['length'],
                      width=CATEGORIES_DIMENSIONS_FILE['width'],
                      height=CATEGORIES_DIMENSIONS_FILE['height'],
                      courier_exp=True,
                      pickup_exp=True)
    catdim_path = os.path.join(str(tmpdir), 'categories_dimensions.csv')
    catdim.dump(catdim_path)
    return catdim_path, catdim


@pytest.fixture(scope='module')
def partner_data(shop_id, business_id, feed_id):
    mbi = {
        'shop_id': shop_id,
        'business_id': business_id,
        'datafeed_id': feed_id,
        'warehouse_id': 0,
        'is_site_market': 'true'
    }
    return [
        {
            'shop_id': shop_id,
            'mbi':  dict2tskv(mbi),
            'status': 'publish'
        }
    ]


@pytest.fixture(scope='module')
def shop_id():
    return SHOP_ID


@pytest.fixture(scope='module')
def feed_id():
    return FEED_ID


@pytest.fixture(scope='module')
def business_id():
    return BUSINESS_ID


@pytest.yield_fixture(scope='function')
def delivery_calc_server(test_data_params):
    shop_meta_response = json.dumps(test_data_params['meta'])

    with PortManager() as pm:
        port = pm.get_port()

        shop_offers_response = {
            'generation_id': DC_GENERATION,
            'offers': [{
                'courier_buckets_info': offer.get('courier_buckets_info'),
                'pickup_buckets_info': offer.get('pickup_buckets_info'),
                'post_buckets_info': offer.get('post_buckets_info')
            } for offer in test_data]
        }

        server = DeliveryCalcServer(feed_response=None,
                                    offer_responses=None,
                                    shop_offers_responses=[shop_offers_response],
                                    shop_meta_response=shop_meta_response,
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
        categories_dimensions
):
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
    dc_enricher = cfg.create_delivery_calc_enricher_processor(
        delivery_calc_server,
        color='white',
        use_average_dimensions_and_weight=True,
        categories_dimensions_path=categories_dimensions[0]
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
def united_offers_to_send(shop_id, feed_id, business_id, test_data_params):
    united_offers = []
    for offer in test_data:
        united_offer = UnitedOffer(
            basic=DatacampOffer(
                identifiers=OfferIdentifiers(
                    offer_id=offer['id'],
                    business_id=business_id,
                ),
                content=OfferContent(
                    partner=PartnerContent(
                        original=OriginalSpecification(
                            dimensions=PreciseDimensions(
                                length_mkm=int(offer.get('length_mkm', 0)),
                                height_mkm=int(offer.get('height_mkm', 0)),
                                width_mkm=int(offer.get('width_mkm', 0))
                            ),
                            weight=PreciseWeight(
                                grams=int(offer.get('weight', 0))
                            ),
                        ),
                        actual=ProcessedSpecification(
                            category=PartnerCategory(
                                path_category_ids=offer['category'],
                            ),
                        ),
                    ),
                    market=MarketContent(
                        enriched_offer=UC.EnrichedOffer(
                            category_id=CATEGORIES_DIMENSIONS_FILE['category_id']
                        )
                    ),
                ),
            ),
            service={
                shop_id: DatacampOffer(
                    identifiers=OfferIdentifiers(
                        shop_id=shop_id,
                        offer_id=offer['id'],
                        feed_id=feed_id,
                        warehouse_id=0,
                        business_id=business_id,
                    ),
                    meta=OfferMeta(rgb=WHITE),
                    delivery=OfferDelivery(
                        delivery_info=DeliveryInfo(
                            pickup=offer.get('pickup'),
                            store=offer.get('store'),
                            available=offer.get('available')
                        )
                    ),
                )
            }
        )
        if offer['price']:
            united_offer.service[shop_id].price.basic.binary_price.price = int(offer['price'] * 10**7)
        if offer['default_price']:
            united_offer.basic.price.basic.binary_price.price = int(offer['default_price'] * 10**7)
        united_offers.append(united_offer)

    return [
        DatacampMessage(
            united_offers=[
                UnitedOffersBatch(
                    offer=united_offers
                )
            ]
        )
    ]


@pytest.fixture(scope='function')
def lbk_sender(miner, shop_id, feed_id, input_topic, united_offers_to_send):
    for message in united_offers_to_send:
        input_topic.write(message.SerializeToString())


@pytest.fixture(scope='function')
def processed_offers(lbk_sender, miner, output_topic):
    data = output_topic.read(count=1)
    return data


@pytest.fixture(scope='module')
def expected_meta_programs():
    programs = []
    return programs


@pytest.fixture(scope='module')
def expected_offer_programs():
    programs = [DeliveryCalc.REGULAR_PROGRAM]
    return programs


@pytest.fixture(scope='module')
def expected_meta_path(shop_id, expected_meta_programs):
    def make_program(program_name):
        return "program={}".format(program_name)

    programs = '&'.join([make_program(program) for program in expected_meta_programs])

    def make_path(shop_id, programs):
        path = '/shopDeliveryMeta?shopId={shop_id}'.format(shop_id=shop_id)
        if expected_meta_programs:
            path += programs
        return path

    return make_path(shop_id, programs)

# ----------------------------- tests ------------------------------


def test_dc_requests_and_enricher(
        miner,
        processed_offers,
        expected_offer_programs,
        expected_meta_path,
        test_data_params
):
    """1. Проверяем, что майнер посылал правильные запросы к калькулятору доставки во время обогащения офера
    2. Проверяем, что майнер корректно обработал ответ калькулятора доставки и в хралище сложены нормальные данные
    """

# ---------------- test part 1

    dc_requests = miner.resources['delivery_calc_server'].GetRequests()
    assert_that(len(dc_requests), equal_to(2))

    # сначала запрос за помагазинными поколением и флагом про средние вес и dimensions
    assert_that(dc_requests[0], equal_to((expected_meta_path, None)))

    # далее идут пооферные запросы
    assert_that(dc_requests[1][0], equal_to('/shopOffers'))

    # возьмем имя словаря с ожидаемыми данными для этого запуска теста
    expected = test_data_params['expected']

    expected_shop_offers_request = {
        'shop_id': SHOP_ID,
        'feed_id':  FEED_ID,
        'generation_id': DC_GENERATION,
        'offers': [
            {
                'offer_id': offer['id'],
                'weight': offer[expected]['weight'],
                'height': offer[expected]['height'],
                'width': offer[expected]['width'],
                'length': offer[expected]['length'],
                'min_quantity': 1,
                'price_map': [{
                    'currency': 'RUR',
                    'value': float(offer['price']) if offer['price'] else float(offer['default_price']),
                }],
                'program_type': expected_offer_programs,
                'categories': offer[expected]['categories'],
                'pickup': offer.get('pickup'),
                'store': offer.get('store'),
            }
            for offer in test_data
        ]
    }

    for offer in expected_shop_offers_request['offers']:
        if offer['pickup'] is None:
            offer.pop('pickup')
        if offer['store'] is None:
            offer.pop('store')

    assert_that(dc_requests[1][1], IsProtobuf(expected_shop_offers_request), '/shopOffers has unexpected body')

# ---------------- test part 2

    delivery0 = {
        'calculator': {
            'delivery_calc_generation': 1,  # дефолтное значение поколения - 1
            'delivery_bucket_ids': [125, 126],
            'pickup_bucket_ids': [127, 128],
            'post_bucket_ids': [127],
            'courier_buckets_info': [
                {
                    'bucket_id': 125,
                    'cost_modifiers_ids': [7, 8],
                    'time_modifiers_ids': [9, 10],
                    'region_availability_modifiers_ids': [11]
                },
                {
                    'bucket_id': 126,
                    'region_availability_modifiers_ids': [8]
                }
            ],
            'pickup_buckets_info': [
                {
                    'bucket_id': 127,
                    'cost_modifiers_ids': [8, 15],
                    'services_modifiers_ids': [2, 3],
                    'region_availability_modifiers_ids': [11, 16]
                },
                {
                    'bucket_id': 128,
                    'region_availability_modifiers_ids': [8]
                }
            ],
            'post_buckets_info': [
                {
                    'bucket_id': 127,
                }
            ],
        },
        'delivery_info': {
            'delivery_currency': 'RUR',
            'use_yml_delivery': False,
            'real_deliverycalc_generation': DC_GENERATION,
            'pickup': test_data[0]['expected_pickup'],
            'store': test_data[0]['store'],
            'available': True,  # значение из фида/OffersData, либо True (тк у smb нет фида)
        },
        'partner': {
            'actual': {
                'delivery_currency': {
                    'value': 'RUR'
                },
                'available': {
                    'flag': True,
                }
            }
        },
        'market': {
            'use_yml_delivery': {
                'flag': False
            },
            'calculator': {
                'delivery_calc_generation': 1,  # дефолтное значение поколения - 1
                'real_deliverycalc_generation': DC_GENERATION,
                'delivery_bucket_ids': [125, 126],
                'pickup_bucket_ids': [127, 128],
                'post_bucket_ids': [127],
                'courier_buckets_info': [
                    {
                        'bucket_id': 125,
                        'cost_modifiers_ids': [7, 8],
                        'time_modifiers_ids': [9, 10],
                        'region_availability_modifiers_ids': [11]
                    },
                    {
                        'bucket_id': 126,
                        'region_availability_modifiers_ids': [8]
                    }
                ],
                'pickup_buckets_info': [
                    {
                        'bucket_id': 127,
                        'cost_modifiers_ids': [8, 15],
                        'services_modifiers_ids': [2, 3],
                        'region_availability_modifiers_ids': [11, 16]
                    },
                    {
                        'bucket_id': 128,
                        'region_availability_modifiers_ids': [8]
                    }
                ],
                'post_buckets_info': [
                    {
                        'bucket_id': 127,
                    }
                ],
            }
        }
    }
    delivery1 = {
        'calculator': {
            'delivery_calc_generation': 1,  # дефолтное значение поколения - 1,
            'real_deliverycalc_generation': DC_GENERATION,
        },
        'delivery_info': {
            'delivery_currency': 'RUR',
            'use_yml_delivery': False,
            'real_deliverycalc_generation': DC_GENERATION,
            'pickup': test_data[1]['pickup'],
            'store': test_data[1]['expected_store'],
            'available': test_data[1]['available']
        },
        'partner': {
            'actual': {
                'delivery_currency': {
                    'value': 'RUR'
                }
            }
        },
        'market': {
            'calculator': {
                'delivery_calc_generation': 1,
                'real_deliverycalc_generation': DC_GENERATION,
            },
            'use_yml_delivery': {
                'flag': False
            }
        }
    }
    assert_that(processed_offers[0], IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [
                {
                    'basic': {
                        'identifiers': {
                            'offer_id': test_data[0]['id'],
                            'business_id': BUSINESS_ID,
                        },
                        'delivery': {'partner': None},
                        'price': None,
                    },
                    'service': IsProtobufMap({
                        SHOP_ID: {
                            'identifiers': {
                                'shop_id': SHOP_ID,
                                'offer_id': test_data[0]['id'],
                                'feed_id': FEED_ID,
                                'business_id': BUSINESS_ID,
                                'warehouse_id': 0,
                            },
                            'delivery': delivery0,
                            'price': None
                        }
                    })
                },
                {
                    'basic': {
                        'identifiers': {
                            'offer_id': test_data[1]['id'],
                            'business_id': BUSINESS_ID,
                        },
                        'delivery': {'partner': None},
                        'price': None,
                    },
                    'service': IsProtobufMap({
                        SHOP_ID: {
                            'identifiers': {
                                'shop_id': SHOP_ID,
                                'offer_id': test_data[1]['id'],
                                'feed_id': FEED_ID,
                                'business_id': BUSINESS_ID,
                                'warehouse_id': 0,
                            },
                            'delivery': delivery1,
                            'price': None
                        }
                    })
                },
                {
                    'basic': {
                        'identifiers': {
                            'offer_id': test_data[3]['id'],
                            'business_id': BUSINESS_ID,
                        },
                        'delivery': {'partner': None},
                        'price': None,
                    },
                    'service': IsProtobufMap({
                        SHOP_ID: {
                            'identifiers': {
                                'shop_id': SHOP_ID,
                                'offer_id': test_data[3]['id'],
                                'feed_id': FEED_ID,
                                'business_id': BUSINESS_ID,
                                'warehouse_id': 0,
                            },
                            'delivery': delivery1,
                            'price': None
                        }
                    })
                },
            ]
        }]
    }))
