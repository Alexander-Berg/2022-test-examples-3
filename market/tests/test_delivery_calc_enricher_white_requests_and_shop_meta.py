# coding: utf-8

import pytest
import json

from hamcrest import assert_that, equal_to

from market.idx.datacamp.yatf.utils import dict2tskv
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampPartnersTable
from yatest.common.network import PortManager

from market.idx.feeds.feedparser.yatf.resources.delivery_calc import DeliveryCalcServer

from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv
from market.idx.yatf.matchers.protobuf_matchers import IsProtobuf, IsSerializedProtobuf, IsProtobufMap

from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.offer.UnitedOffer_pb2 import UnitedOffersBatch, UnitedOffer
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import (
    OfferIdentifiers,
    OfferPrice,
    PriceBundle,
    OfferMeta,
    WHITE,
)
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import Offer as DatacampOffer
from market.proto.common.common_pb2 import PriceExpression
import market.proto.delivery.delivery_calc.delivery_calc_pb2 as DeliveryCalc

# ----------------------------- prepare ------------------------------

DC_GENERATION = 10
SHOP_ID = 999
BUSINESS_ID = 999
FEED_ID = 888

test_data = [
    {  # валюта == реф.валюте == валюте доставки => отправляем как есть
        'id': '0',
        'price': 100,
        'currency': 'KZT',
        'ref_currency': 'KZT',
        'expected': {
            'price': 100.0,
            'currency': 'KZT'
        }
    },
    {  # валюта(default=RUR) == реф.валюте(default=RUR) != валюте доставки, rate не заполнен - пересчитаем по CBRF
        # CBRF - временное решение, в будущем будем(?) смотреть на банк по региону магазина
        'id': '1',
        'price': 100,
        'expected': {
            'price': 582.4756379564426,  # посчитано по стабу файла currency_rates.xml
            'currency': 'KZT'
        }
    },
    {  # валюта != реф.валюте == валюте доставки - пересчитываем в валюту доставки по rate и отправляем
        # для такого случая не может быть пустого rate
        'id': '2',
        'price': 100,
        'currency': 'RUR',
        'ref_currency': 'KZT',
        'rate': 'NBK',
        'expected': {
            'price': 579.0,  # посчитано по стабу файла currency_rates.xml
            'currency': 'KZT'
        }
    },
    {  # невалидная валюта. этот оффер не отправляем в DC, остальные - отправляем.
        'id': 'BAD_CURR',
        'price': 100,
        'currency': 'BAD_CURR',
    },
    {  # валюта != реф.валюте != валюте доставки - пересчитываем в валюту доставки по rate и отправляем
        # для такого случая не может быть пустого rate
        'id': '3',
        'price': 100,
        'currency': 'RUR',
        'ref_currency': 'BYN',
        'rate': 'NBK',
        'expected': {
            'price': 579.0,  # посчитано по стабу файла currency_rates.xml
            'currency': 'KZT'
        },
        'courier_buckets_info': [
            {'bucket_id': 126, 'cost_modifiers_ids': [], 'time_modifiers_ids': [], 'services_modifiers_ids': [], 'region_availability_modifiers_ids': []},
        ]
    },
]


@pytest.fixture(scope='module')
def shop_meta():
    return {
        'generationId': DC_GENERATION,
        'currencies': ['KZT'],
        'useYmlDelivery': True
    }


@pytest.fixture(scope='module')
def partner_data(shop_id, feed_id, business_id):
    mbi = {
        'shop_id': shop_id,
        'datafeed_id': feed_id,
        'business_id': business_id,
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
def delivery_calc_server(shop_meta):
    shop_meta_response = json.dumps(shop_meta)

    with PortManager() as pm:
        port = pm.get_port()

        shop_offers_response = {
            'generation_id': DC_GENERATION,
            'offers': [{
                'courier_buckets_info': offer.get('courier_buckets_info', [])
            } for offer in test_data if offer['id'] != 'BAD_CURR']
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
        partner_info_table_path
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
def offers_to_send(shop_id, feed_id):
    return [
        DatacampOffer(
            identifiers=OfferIdentifiers(
                shop_id=shop_id,
                offer_id=offer['id'],
                feed_id=feed_id,
                warehouse_id=0,
            ),
            price=OfferPrice(
                basic=PriceBundle(
                    binary_price=PriceExpression(
                        price=offer['price'] * 10**7,
                        id=offer.get('currency', None),
                        ref_id=offer.get('ref_currency', None),
                        rate=offer.get('rate', None)
                    )
                )
            ),
        )
        for offer in test_data
    ]


@pytest.fixture(scope='function')
def united_offers_to_send(shop_id, feed_id, business_id):
    return [
        DatacampMessage(
            united_offers=[UnitedOffersBatch(
                offer=[UnitedOffer(
                    basic=DatacampOffer(
                        identifiers=OfferIdentifiers(
                            offer_id=offer['id'],
                            business_id=business_id,
                        )
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
                            price=OfferPrice(
                                basic=PriceBundle(
                                    binary_price=PriceExpression(
                                        price=offer['price'] * 10**7,
                                        id=offer.get('currency', None),
                                        ref_id=offer.get('ref_currency', None),
                                        rate=offer.get('rate', None)
                                    )
                                )
                            )
                        )
                    }
                ) for offer in test_data])])
    ]


@pytest.fixture(scope='function')
def lbk_sender(miner, shop_id, feed_id, input_topic, offers_to_send, united_offers_to_send):
    for message in united_offers_to_send:
        input_topic.write(message.SerializeToString())


@pytest.fixture(scope='module')
def expected_offer_programs():
    programs = [DeliveryCalc.REGULAR_PROGRAM]
    return programs


@pytest.fixture(scope='function')
def processed_offers(lbk_sender, miner, output_topic):
    data = output_topic.read(count=1)
    return data


# ----------------------------- tests ------------------------------


def test_dc_requests_and_enricher(
        miner,
        expected_offer_programs,
        processed_offers
):
    """1. Проверяем, что майнер отправил 2 запроса, в shopOffers передана правильная цена
    2. Проверяем, что майнер корректно обработал ответ калькулятора доставки, и в хранилище сложены нормальные данные
    """

    dc_requests = miner.resources['delivery_calc_server'].GetRequests()
    assert_that(len(dc_requests), equal_to(2))

    expected_shop_offers_request = {
        'shop_id': SHOP_ID,
        'feed_id':  FEED_ID,
        'generation_id': DC_GENERATION,
        'offers': [
            {
                'offer_id': offer['id'],
                'price_map': [{
                    'currency': 'KZT',
                    'value': offer['expected']['price'],
                }],
                'program_type': expected_offer_programs,
                'store': True,
                'min_quantity': 1,
            }
            for offer in test_data if offer['id'] != 'BAD_CURR'
        ]
    }

    assert_that(dc_requests[1][1], IsProtobuf(expected_shop_offers_request), '/shopOffers has unexpected body')

    # ---------------- test part 2
    # проверим оффер, что в оффере сохраняется валюта доставки от КД
    # (поэтому на примере оффера с id = '3', у которого ни одна валюта оффера не совпадает с валютой доставки)

    assert_that(processed_offers[0], IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [
                {
                    'basic': {
                        'identifiers': {
                            'offer_id': test_data[4]['id'],
                            'business_id': BUSINESS_ID,
                        }
                    },
                    'service': IsProtobufMap({
                        SHOP_ID: {
                            'identifiers': {
                                'shop_id': SHOP_ID,
                                'offer_id': test_data[4]['id'],
                                'feed_id': FEED_ID,
                            },
                            'price': None,
                            'delivery': {
                                'calculator': {
                                    'delivery_calc_generation': 1,  # дефолтное значение поколения - 1
                                    'delivery_bucket_ids': [126],
                                    'courier_buckets_info': [
                                        {
                                            'bucket_id': 126,
                                        }
                                    ],
                                },
                                'partner': {
                                    'actual': {
                                        'delivery_currency': {
                                            'value': 'KZT'
                                        }
                                    }
                                },
                                'delivery_info': {
                                    'delivery_currency': 'KZT',
                                    'use_yml_delivery': True,  # взято из shopDeliveryMeta
                                    'real_deliverycalc_generation': DC_GENERATION,
                                }
                            },
                        }
                    })
                }
            ]
        }]
    }))
