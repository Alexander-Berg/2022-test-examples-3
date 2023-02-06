# coding: utf-8

import pytest
from datetime import datetime
import uuid
import yt.wrapper as yt
from hamcrest import assert_that
from yt.wrapper import ypath_join

from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.models.MarketSku_pb2 import MarketSku, MarketSkuBatch
from market.idx.datacamp.proto.models.MarketSkuMboContent_pb2 import MarketSkuMboContent
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.pylibrary.datacamp.conversion import offer_to_basic_row
from market.idx.datacamp.yatf.resources.tokens import YtTokenStub
from market.idx.datacamp.yatf.utils import create_meta
from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampMskuTable, DataCampBasicOffersTable
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.yatf.resources.yt_table_resource import YtDynTableResource
from market.pylibrary.proto_utils import message_from_data


TS = 1618763231
MSKU_TS = datetime.utcfromtimestamp(TS).strftime('%Y-%m-%dT%H:%M:%SZ')

BUSINESS_ID = 1
BUSINESS_ID_FIRST_PARTY = 924574
SHOP_ID = 2


@pytest.fixture(scope='module')
def datacamp_msku_table_path():
    return ypath_join(get_yt_prefix(), 'msku/msku')


@pytest.fixture(scope='module')
def basic_offers_table_path():
    return yt.ypath_join('//home/test_datacamp', str(uuid.uuid4()), 'basic_offers')


def create_msku(id=0, with_ts=True, params=None):
    mboc_content = {
        'msku': {
            'id': id,
        },
    }
    if with_ts:
        mboc_content['meta'] = {'timestamp': MSKU_TS}
    if params:
        mboc_content['msku']['parameter_values'] = params
    return {
        'id': id,
        'mbo_content': message_from_data(mboc_content, MarketSkuMboContent()).SerializeToString()
    }


@pytest.fixture(scope='module')
def datacamp_msku_table_data():
    return [
        create_msku(id=2),
        create_msku(id=31, with_ts=False),
        create_msku(id=32),
        create_msku(id=4),
        create_msku(id=5, with_ts=True, params=[
            {
                'xsl_name': 'cargoType100',
                'bool_value': True
            },
            {
                'xsl_name': 'cargoType200',
                'bool_value': False  # не попадет на оффер
            },
            {
                'xsl_name': 'cargoType300',
                'bool_value': True
            }
        ]),
        create_msku(id=6, with_ts=True, params=[
            {
                'xsl_name': 'cargoType100',
                'bool_value': False
            }
        ]),
        create_msku(id=7),
        create_msku(id=8, with_ts=True, params=[
            {
                'xsl_name': 'cargoType100',
                'bool_value': True
            }
        ]),
        create_msku(id=9, with_ts=True, params=[
            {
                'xsl_name': 'cargoType100',
                'bool_value': True
            }
        ]),
        create_msku(id=10, with_ts=True, params=[
            {
                'xsl_name': 'cargoType100',
                'bool_value': True
            }
        ]),
    ]


def mboc_offers_external_attributes():
    schema = [
        dict(name='supplier_id', type='int64', sort_order='ascending'),
        dict(name='shop_sku', type='string', sort_order='ascending'),
        dict(name='msku_id', type='int64', sort_order='ascending'),
        dict(name='msku_ts', type='string'),
        dict(name='business_id', type='int64')
    ]

    attrs = {
        'schema': schema,
        'dynamic': True
    }

    return attrs


class MbocOffersExternalTable(YtDynTableResource):
    def __init__(self, yt_stuff, path, data=None):
        super(MbocOffersExternalTable, self).__init__(
            yt_stuff=yt_stuff,
            path=path,
            attributes=mboc_offers_external_attributes(),
            data=data,
        )


@pytest.fixture(scope='module')
def mboc_offers_directory_path():
    return ypath_join(get_yt_prefix(), 'datacamp/mboc_offers/states')


GENERATION = '20200101_0000'
MBOC_OFFERS_DATA = [
    {'msku_id': 1, 'shop_sku': 'shop_sku_1', 'business_id': BUSINESS_ID, 'supplier_id': SHOP_ID},
    {'msku_id': 31, 'shop_sku': 'shop_sku_31', 'business_id': BUSINESS_ID, 'supplier_id': SHOP_ID},
    {'msku_id': 32, 'shop_sku': 'shop_sku_32', 'business_id': None, 'supplier_id': SHOP_ID},
    {'msku_id': 4, 'shop_sku': 'shop_sku_4', 'business_id': BUSINESS_ID, 'supplier_id': SHOP_ID},
    {'msku_id': 5, 'shop_sku': 'shop_sku_5', 'business_id': BUSINESS_ID, 'supplier_id': SHOP_ID},
    {'msku_id': 5, 'shop_sku': 'shop_sku_5', 'business_id': BUSINESS_ID, 'supplier_id': SHOP_ID+1},
    {'msku_id': 6, 'shop_sku': 'shop_sku_6', 'business_id': BUSINESS_ID, 'supplier_id': SHOP_ID},
    {'msku_id': 7, 'shop_sku': 'shop_sku_7_1', 'business_id': BUSINESS_ID, 'supplier_id': SHOP_ID},
    {'msku_id': 7, 'shop_sku': 'shop_sku_7_2', 'business_id': BUSINESS_ID, 'supplier_id': SHOP_ID},
    {'msku_id': 7, 'shop_sku': 'shop_sku_7_3', 'business_id': BUSINESS_ID, 'supplier_id': SHOP_ID},
    {'msku_id': 7, 'shop_sku': 'shop_sku_7_4', 'business_id': BUSINESS_ID, 'supplier_id': SHOP_ID},
    {'msku_id': 8, 'shop_sku': 'shop_sku_8', 'business_id': BUSINESS_ID, 'supplier_id': SHOP_ID},
    {'msku_id': 9, 'shop_sku': 'shop_sku_9', 'business_id': BUSINESS_ID, 'supplier_id': SHOP_ID},
    {'msku_id': 10, 'shop_sku': 'shop_sku_10', 'business_id': BUSINESS_ID_FIRST_PARTY, 'supplier_id': SHOP_ID+2},
]


@pytest.fixture(scope='module')
def mboc_offers_table(yt_server, mboc_offers_directory_path):
    client = yt_server.get_yt_client()
    client.mkdir(mboc_offers_directory_path, recursive=True)

    table_path = ypath_join(mboc_offers_directory_path, GENERATION)
    mboc_table = MbocOffersExternalTable(yt_stuff=yt_server, path=table_path,
                                         data=MBOC_OFFERS_DATA)
    return mboc_table

BASIC_OFFERS_DATA = [
    offer_to_basic_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(
            business_id=offer['business_id'],
            offer_id=offer['shop_sku'],
        ),
        meta=create_meta(10),
        content=DTC.OfferContent(
            binding=DTC.ContentBinding(
                approved=DTC.Mapping(
                    market_sku_id=offer['msku'] if 'msku' in offer else 0,
                ),
            )
        ))) for offer in [
        {'business_id': BUSINESS_ID, 'shop_sku': 'shop_sku_5', 'msku': 5},
        {'business_id': BUSINESS_ID, 'shop_sku': 'shop_sku_6', 'msku': 6},
        {'business_id': BUSINESS_ID, 'shop_sku': 'shop_sku_7_1', 'msku': 7},
        {'business_id': BUSINESS_ID, 'shop_sku': 'shop_sku_7_2', 'msku': 7},
        {'business_id': BUSINESS_ID, 'shop_sku': 'shop_sku_7_3', 'msku': 7},
        {'business_id': BUSINESS_ID, 'shop_sku': 'shop_sku_7_4', 'msku': 7},
        {'business_id': BUSINESS_ID, 'shop_sku': 'shop_sku_8', 'msku': 80},  # msku отличается от той, что в mboc_offers
        {'business_id': BUSINESS_ID_FIRST_PARTY, 'shop_sku': 'shop_sku_10', 'msku': 10},  # для 1P мскю тоже берётся из approved
    ]
]


@pytest.fixture(scope='module')
def basic_offers_table(yt_server, basic_offers_table_path):
    return DataCampBasicOffersTable(yt_server, basic_offers_table_path, data=BASIC_OFFERS_DATA)


@pytest.fixture(scope='module')
def miner_config(log_broker_stuff,
                 input_topic,
                 output_topic,
                 yt_server,
                 yt_token,
                 datacamp_msku_table_path,
                 mboc_offers_directory_path,
                 basic_offers_table_path):
    cfg = MinerConfig()

    reader = cfg.create_lbk_reader_datacamp_message(log_broker_stuff, input_topic)
    unpacker = cfg.create_msku_miner_datacamp_message_unpacker()
    msku_rebatcher = cfg.create_datacamp_message_msku_rebatcher(enable=True, max_msku_in_message=20)
    enricher = cfg.create_msku_miner_msku_enricher_processor(
        yt_server=yt_server,
        yt_token=yt_token.path,
        yt_msku_table_path=datacamp_msku_table_path,
        request_mbo_content=True,
        yt_mboc_offers_table_path=ypath_join(mboc_offers_directory_path, GENERATION),
        request_mappings=True,
        mappings_output_row_limit=10000000
    )
    message_filter_by_msku_route = cfg.create_msku_miner_message_filter_by_msku_route(True, 'cargo_type_to_datacamp')
    msku_to_offer_converter = cfg.create_msku_to_offer_converter(yt_server=yt_server,
                                                                 yt_token=yt_token.path,
                                                                 yt_basic_offers_table_path=basic_offers_table_path,
                                                                 basic_table_lookup_limit=2)
    offer_rebatcher = cfg.create_datacamp_message_offer_rebatcher(enable=True, max_offers_in_message=2)
    sender = cfg.create_msku_miner_datacamp_message_sender()
    writer = cfg.create_general_lbk_writer_processor(log_broker_stuff, output_topic)

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, msku_rebatcher)
    cfg.create_link(msku_rebatcher, enricher)
    cfg.create_link(enricher, message_filter_by_msku_route)
    cfg.create_link(message_filter_by_msku_route, msku_to_offer_converter)
    cfg.create_link(msku_to_offer_converter, offer_rebatcher)
    cfg.create_link(offer_rebatcher, sender)
    cfg.create_link(sender, writer)

    return cfg


@pytest.fixture(scope='module')
def input_topic(log_broker_stuff):
    return LbkTopic(log_broker_stuff)


@pytest.fixture(scope='module')
def output_topic(log_broker_stuff):
    return LbkTopic(log_broker_stuff)


@pytest.fixture(scope='module')
def yt_token():
    return YtTokenStub()


@pytest.yield_fixture(scope='module')
def miner(miner_config,
          input_topic,
          output_topic,
          yt_server,
          datacamp_msku_table_path,
          datacamp_msku_table_data,
          mboc_offers_table,
          basic_offers_table):
    resources = {
        'miner_cfg': miner_config,
        'input_topic': input_topic,
        'output_topic': output_topic,
        'datacamp_msku_table': DataCampMskuTable(
            yt_stuff=yt_server,
            path=datacamp_msku_table_path,
            data=datacamp_msku_table_data
        ),
        'basic_offers_table': basic_offers_table,
        'mboc_offers_table': mboc_offers_table,
    }
    with MinerTestEnv(**resources) as miner:
        miner.verify()
        yield miner


@pytest.yield_fixture(scope='module')
def messages(input_topic, output_topic):
    message = DatacampMessage(market_skus=MarketSkuBatch(
        msku=[
            MarketSku(id=1, msku_route_flags=2),   # нет в таблице msku
            MarketSku(id=2, msku_route_flags=2),   # нет маппингов на офферы (нет записи в mboc_offers)
            MarketSku(id=31, msku_route_flags=2),  # в данных от mboc нет таймстемпа
            MarketSku(id=32, msku_route_flags=2),  # косячная запись про оффер в mboc_offers
            MarketSku(id=4, msku_route_flags=1),   # route=to_iris - в хранилище не поедет

            MarketSku(id=5, msku_route_flags=3),   # route=to_iris + cargo_types - есть карготипы с bool_value=True
            MarketSku(id=6, msku_route_flags=2),   # route=cargo_type - есть карготипы, но с bool_value=False
            MarketSku(id=7, msku_route_flags=2),   # route=cargo_type - нет карготипов на msku, много офферов заматчено, будет ребатчинг

            MarketSku(id=8, msku_route_flags=2),   # route=cargo_type - не будет отправки - в таблице маппинга и в базовой разные msku на оффере
            MarketSku(id=9, msku_route_flags=2),   # route=cargo_type - не будет отправки - в базовой таблице нет такого оффера

            MarketSku(id=10, msku_route_flags=2),   # route=cargo_type - мскю 1P оффера
        ]
    ))

    input_topic.write(message.SerializeToString())
    return output_topic.read(4, wait_timeout=10)


def create_expected_offer(id='0', cargo_types=[], business_id=BUSINESS_ID):
    return {
        'basic': {
            'identifiers': {
                'offer_id': id,
                'business_id': business_id
            },
            'content': {
                'master_data': {
                    'cargo_type': {
                        'meta': {
                            'timestamp': {
                                'seconds': TS
                            }
                        },
                        'value': cargo_types
                    }
                }
            }
        }
    }


def test_msku_flow_and_convertion_into_offer(miner, messages, output_topic):
    """
    Проверяем, что msku-шки, полученные msku_miner-ом
     - обогатятся данными из двух таблиц (msku + mboc_offers)
     - пофильтруются по их назначению/маршруту - msku_route_flags
     - msku с флагом карготипов для хранилища конвертнутся в офферы из маппинга
     - итоговый батч разобьется батчи поменьше (тк на одну msku может быть замаплено слишком много офферов)
    """
    assert(len(messages) == 4)

    assert_that(messages[0], IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [
                create_expected_offer('shop_sku_5', [100, 300]),
                create_expected_offer('shop_sku_6', []),
            ]
        }]
    }))

    assert_that(messages[1], IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [
                create_expected_offer('shop_sku_7_1', []),
                create_expected_offer('shop_sku_7_2', []),
            ]
        }]
    }))

    assert_that(messages[2], IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [
                create_expected_offer('shop_sku_7_3', []),
                create_expected_offer('shop_sku_7_4', []),
            ]
        }]
    }))

    assert_that(messages[3], IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [
                create_expected_offer('shop_sku_10', [100], business_id=BUSINESS_ID_FIRST_PARTY),
            ]
        }]
    }))

    # проверяем, что в топике больше нет данных, которые мы можем вычитать
    assert_that(output_topic, HasNoUnreadData())
