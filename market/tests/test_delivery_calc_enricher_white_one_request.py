# coding: utf-8

import pytest
import json

from hamcrest import assert_that, equal_to, is_not

from yatest.common.network import PortManager

from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import Offer as DatacampOffer
from market.idx.datacamp.yatf.utils import dict2tskv

from market.idx.feeds.feedparser.yatf.resources.delivery_calc import DeliveryCalcServer

from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobufMap

from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampPartnersTable
from market.idx.yatf.utils.utils import create_pb_timestamp

from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.offer.UnitedOffer_pb2 import UnitedOffersBatch, UnitedOffer
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import (
    DeliveryCalculatorOptions,
    DeliveryInfo,
    OfferDelivery,
    OfferIdentifiers,
    OfferPrice,
    PriceBundle,
    UpdateMeta,
    OfferMeta,
    WHITE,
)
from market.proto.common.common_pb2 import PriceExpression
from market.proto.delivery.delivery_calc.delivery_calc_pb2 import BucketInfo


"""Тест про случаи, когда во время переобогащения доставкой необходимо выполнить только один запрос - за помагазинной
метой, а второй поофферный запрос не делать, например:

(выполнить один запрос и не трогать оффер в хранилище)
    - в мете не вернулся id поколения
    - мета пустая
    - мета не содержит элемент currencies или он пустой, или содержит невалидную валюту

Временно выключено (MARKETINDEXER-33635)
(выполнить один запрос и обновить таймстемпы данных о доставке оффера в хранилище)
    - в мете от КД вернулся тот же id поколения, что уже указано для оффера в хранилище
"""

data = {
    'bucket_id': 1234567,
    'dc_generation': 10,
    'feed_id': 888,
    'offer_id': '1',
    'offer_price': 123 * 10 ** 7,
    'shop_id': 999,
    'business_id': 999,
    'timestamp_old': create_pb_timestamp(1000),
}


@pytest.fixture(params=[
    {
        'meta': {},
        'timestamp_comparing_func': equal_to
    },
    {
        'meta': {},
        'timestamp_comparing_func': equal_to,
        'meta_response_code': 500
    },
    {
        'meta': {'message': 'No one shop-related generation was found for shopId=10291790'},
        'timestamp_comparing_func': equal_to,
        'meta_response_code': 404
    },
    {
        'meta': {'generationId': 0, 'currencies': ['RUR']},
        'timestamp_comparing_func': equal_to
    },
    {
        'meta': {'generationId': 100},
        'timestamp_comparing_func': equal_to
    },
    {
        'meta': {'generationId': 100, 'currencies': []},
        'timestamp_comparing_func': equal_to
    },
    {
        'meta': {'generationId': 100, 'currencies': ['BAD']},
        'timestamp_comparing_func': equal_to
    },
    # Возвращая этот кейс, вернуть import greater_than из hamcrest
    # {
    #     'meta': {
    #         'generationId': data['dc_generation'],
    #         'shopOffersAverageWeightDimensions': None
    #     },
    #     'timestamp_comparing_func': greater_than
    # }
    ],
    ids=[
        'empty_meta',
        'DC_500',
        'DC_404_meta_with_message_without_generationId',
        'meta_with_zero_generationId',
        # 'same_generation_meta',
        'meta_without_currencies',
        'meta_with_empty_currencies',
        'meta_with_invalid_currency',
    ])
def test_data_params(request):
    return request.param


@pytest.fixture(scope='module')
def partner_data():
    mbi = {
        'shop_id': data['shop_id'],
        'datafeed_id': data['feed_id'],
        'warehouse_id': 0,
        'is_site_market': 'true'
    }
    return [
        {
            'shop_id': data['shop_id'],
            'mbi':  dict2tskv(mbi),
            'status': 'publish'
        }
    ]


@pytest.yield_fixture(scope='function')
def delivery_calc_server(test_data_params):
    shop_meta_response = json.dumps(test_data_params['meta'])

    with PortManager() as pm:
        port = pm.get_port()
        server = DeliveryCalcServer(shop_meta_response=shop_meta_response,
                                    port=port,
                                    meta_response_code=test_data_params.get('meta_response_code', 200))
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
        use_average_dimensions_and_weight=True
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
def lbk_sender(miner, input_topic):
    message = DatacampMessage(
        united_offers=[UnitedOffersBatch(
            offer=[UnitedOffer(
                basic=DatacampOffer(
                    identifiers=OfferIdentifiers(
                        offer_id=data['offer_id'],
                        business_id=data['business_id'],
                    ),
                    price=OfferPrice(
                        basic=PriceBundle(
                            binary_price=PriceExpression(
                                price=data['offer_price']
                            )
                        )
                    ),
                    delivery=OfferDelivery(
                        delivery_info=DeliveryInfo(
                            real_deliverycalc_generation=data['dc_generation'],
                            meta=UpdateMeta(
                                timestamp=data['timestamp_old']
                            )
                        ),
                        calculator=DeliveryCalculatorOptions(
                            courier_buckets_info=[
                                BucketInfo(
                                    bucket_id=data['bucket_id']
                                )
                            ],
                            delivery_bucket_ids=[data['bucket_id']],
                            meta=UpdateMeta(
                                timestamp=data['timestamp_old']
                            )
                        ),
                    )
                ),
                service={
                    data['shop_id']: DatacampOffer(
                        identifiers=OfferIdentifiers(
                            shop_id=data['shop_id'],
                            offer_id=data['offer_id'],
                            feed_id=data['feed_id'],
                            warehouse_id=0,
                            business_id=data['business_id'],
                        ),
                        meta=OfferMeta(rgb=WHITE)
                    )
                }
            )])])
    input_topic.write(message.SerializeToString())


@pytest.fixture(scope='function')
def processed_offer(lbk_sender, miner, output_topic):
    data = output_topic.read(count=1)
    return data[0]


@pytest.fixture(scope='module')
def expected_meta_path():
    # упрощаю для этого теста, без программ пока
    path = '/shopDeliveryMeta?shopId={shop_id}'.format(shop_id=data['shop_id'])
    return path


@pytest.mark.skip(reason='super flaky')
def test_dc_requests(
        miner,
        processed_offer,
        expected_meta_path
):
    """Проверяем, что майнер посылал правильные запросы к калькулятору доставки
    во время обогащения офера
    + Что не отправлялись запросы shopOffers, когда они не нужны
    """
    dc_requests = miner.resources['delivery_calc_server'].GetRequests()

    assert_that(processed_offer, is_not(equal_to(None)))
    assert_that(len(dc_requests), equal_to(1))

    # только запрос за помагазинными поколением и флагом про средние вес и dimensions
    assert_that(dc_requests[0], equal_to((expected_meta_path, None)))


def test_dc_enricher(processed_offer, test_data_params):
    """Проверяем, что майнер корректно обработал ответ калькулятора доставки:
    либо ничего не изменилось, либо изменился только таймстемп
    """

    assert_that(processed_offer, IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [
                {
                    'basic': {
                        'identifiers': {
                            'offer_id': data['offer_id'],
                            'business_id': data['business_id']
                        },
                        'delivery': None,
                        'price': None
                    },
                    'service': IsProtobufMap({
                        data['shop_id']: {
                            'identifiers': {
                                'shop_id': data['shop_id'],
                                'offer_id': data['offer_id'],
                                'feed_id': data['feed_id'],
                                'business_id': data['business_id'],
                                'warehouse_id': 0,
                            },
                        }
                    })
                }
            ]
        }
        ]}))
