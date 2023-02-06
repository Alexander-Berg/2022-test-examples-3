# coding: utf-8

import pytest
from hamcrest import assert_that

from yt.wrapper import ypath_join

from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv
from market.idx.datacamp.yatf.resources.tokens import YtTokenStub
from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampMskuTable
from market.pylibrary.proto_utils import message_from_data
from market.idx.datacamp.proto.models.MarketSku_pb2 import MarketSku, MarketSkuBatch
from market.idx.datacamp.proto.models.MarketSkuMboContent_pb2 import MarketSkuMboContent
from market.idx.datacamp.proto.models.MappingToOffers_pb2 import MappingToOffers
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import OfferIdentifiers
from market.idx.datacamp.yatf.utils import create_update_meta
from market.proto.content.mbo.MboParameters_pb2 import ValueType
from market.idx.yatf.resources.yt_table_resource import YtDynTableResource
from market.idx.datacamp.yatf.utils import dict2tskv
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampPartnersTable

META = 1618763231


BUSINESS_ID = 1
SHOP_ID_WITH_FULFULLMENT_PROGRAM = 8884
SHOP_ID_WITH_CROSS_DOCK_PROGRAM = 774
SHOP_ID_WITH_CLICK_N_COLLECT = 44
PARTNER_DATA = [
    {
        'shop_id': SHOP_ID_WITH_FULFULLMENT_PROGRAM,
        'mbi':  dict2tskv({
            'shop_id': SHOP_ID_WITH_FULFULLMENT_PROGRAM,
            'blue_status': 'REAL',
            'datafeed_id': 888,
            'warehouse_id': 77,
            'ff_program': 'REAL',
            'direct_shipping': True,
        }),
        'status': 'publish'
    },
    {
        'shop_id': SHOP_ID_WITH_CROSS_DOCK_PROGRAM,
        'mbi':  dict2tskv({
            'blue_status': 'REAL',
            'shop_id': SHOP_ID_WITH_CROSS_DOCK_PROGRAM,
            'datafeed_id': 7334,
            'ff_program': 'REAL',
        }),
        'status': 'publish'
    },
    {
        'shop_id': SHOP_ID_WITH_CLICK_N_COLLECT,
        'mbi':  dict2tskv({
            'blue_status': 'REAL',
            'shop_id': SHOP_ID_WITH_CLICK_N_COLLECT,
            'datafeed_id': 2343,
            'ignore_stocks': True
        }),
        'status': 'publish'
    },
]


@pytest.fixture(scope='module')
def datacamp_msku_table_path():
    return ypath_join(get_yt_prefix(), 'msku/msku')


@pytest.fixture(scope='module')
def datacamp_msku_table_data():
    return [
        {
            'id': 1234,
            'mbo_content': message_from_data({
                'msku': {
                    'id': 1234,
                    'parameter_values': [
                        {
                            'param_id': 10,
                            'value_type': ValueType.BOOLEAN,
                            'bool_value': True
                        }
                    ],
                    'titles': [
                        {
                            'isoCode': 'ru',
                            'value': 'Msku1234'
                        }
                    ]
                }
            }, MarketSkuMboContent()).SerializeToString()
        },
        {
            'id': 1235,
            'mbo_content': message_from_data({
                'msku': {
                    'id': 1235,
                    'parameter_values': [
                        {
                            'param_id': 11,
                            'value_type': ValueType.BOOLEAN,
                            'bool_value': False
                        }
                    ],
                    'titles': [
                        {
                            'isoCode': 'ru',
                            'value': 'Msku1235'
                        }
                    ]
                }
            }, MarketSkuMboContent()).SerializeToString()
        },
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
    {'msku_id': 33, 'shop_sku': 'shop_sku0', 'business_id': BUSINESS_ID, 'supplier_id': SHOP_ID_WITH_FULFULLMENT_PROGRAM},
    {'msku_id': 33, 'shop_sku': 'shop_sku1', 'business_id': BUSINESS_ID, 'supplier_id': SHOP_ID_WITH_FULFULLMENT_PROGRAM},
    {'msku_id': 33, 'shop_sku': 'shop_sku3', 'business_id': BUSINESS_ID, 'supplier_id': SHOP_ID_WITH_CLICK_N_COLLECT},  # Должен отфильтроваться!
    {'msku_id': 34, 'shop_sku': 'shop_sku2', 'business_id': BUSINESS_ID, 'supplier_id': SHOP_ID_WITH_CROSS_DOCK_PROGRAM},
    {'msku_id': 34, 'shop_sku': 'shop_sku8', 'business_id': BUSINESS_ID, 'supplier_id': SHOP_ID_WITH_CLICK_N_COLLECT},  # Должен отфильтроваться!
    {'msku_id': 35, 'shop_sku': 'shop_sku3', 'business_id': BUSINESS_ID, 'supplier_id': SHOP_ID_WITH_FULFULLMENT_PROGRAM},
    {'msku_id': 36, 'shop_sku': 'shop_sku4', 'business_id': BUSINESS_ID, 'supplier_id': SHOP_ID_WITH_FULFULLMENT_PROGRAM},
]


@pytest.fixture(scope='module')
def mboc_offers_table(yt_server, mboc_offers_directory_path):
    client = yt_server.get_yt_client()
    client.mkdir(mboc_offers_directory_path, recursive=True)

    table_path = ypath_join(mboc_offers_directory_path, GENERATION)
    mboc_table = MbocOffersExternalTable(yt_stuff=yt_server, path=table_path,
                                         data=MBOC_OFFERS_DATA)
    return mboc_table


@pytest.fixture(scope='module')
def miner_config(log_broker_stuff, input_topic, output_topic, yt_server, yt_token, datacamp_msku_table_path, mboc_offers_directory_path, partner_info_table_path):
    cfg = MinerConfig()

    cfg.create_miner_initializer(
        yt_server=yt_server,
        token=yt_token.path,
        partners_table_path=partner_info_table_path,
    )

    reader = cfg.create_lbk_reader_datacamp_message(log_broker_stuff, input_topic)
    unpacker = cfg.create_msku_miner_datacamp_message_unpacker()
    rebatcher = cfg.create_datacamp_message_msku_rebatcher(enable=True, max_msku_in_message=2)
    enricher = cfg.create_msku_miner_msku_enricher_processor(
        yt_server=yt_server,
        yt_token=yt_token.path,
        yt_msku_table_path=datacamp_msku_table_path,
        request_mbo_content=True,
        yt_mboc_offers_table_path=ypath_join(mboc_offers_directory_path, GENERATION),
        request_mappings=True,
        mappings_output_row_limit=10000000
    )
    offers_filter = cfg.create_msku_miner_offers_filter()
    message_filter_by_msku_route = cfg.create_msku_miner_message_filter_by_msku_route(True, 'to_iris')
    sender = cfg.create_msku_miner_datacamp_message_sender()
    writer = cfg.create_general_lbk_writer_processor(log_broker_stuff, output_topic)

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, rebatcher)
    cfg.create_link(rebatcher, enricher)
    cfg.create_link(enricher, message_filter_by_msku_route)
    cfg.create_link(message_filter_by_msku_route, offers_filter)
    cfg.create_link(offers_filter, sender)
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
def miner(miner_config, input_topic, output_topic, yt_server,
          datacamp_msku_table_path, datacamp_msku_table_data, mboc_offers_table, partner_info_table_path):
    resources = {
        'miner_cfg': miner_config,
        'input_topic': input_topic,
        'output_topic': output_topic,
        'datacamp_msku_table': DataCampMskuTable(
            yt_stuff=yt_server,
            path=datacamp_msku_table_path,
            data=datacamp_msku_table_data
        ),
        'mboc_offers_table': mboc_offers_table,
        'partners_table': DataCampPartnersTable(
            yt_stuff=yt_server,
            path=partner_info_table_path,
            data=PARTNER_DATA)
    }
    with MinerTestEnv(**resources) as miner:
        miner.verify()
        yield miner


@pytest.yield_fixture(scope='module')
def messages(input_topic, output_topic):
    message = DatacampMessage(market_skus=MarketSkuBatch(
        msku=[MarketSku(
            id=1234,
            mapping_to_offers=MappingToOffers(
                meta=create_update_meta(META),
                offers=[
                    OfferIdentifiers(business_id=BUSINESS_ID, shop_id=100, offer_id='offer0'),  # Про этот магазин нет записи в таблице партнёров, поэтому пропускаем оффера дальше
                    OfferIdentifiers(business_id=BUSINESS_ID, shop_id=100, offer_id='offer1')
                ]
            ),
            msku_route_flags=1,  # to_iris
        ),
            MarketSku(id=666, msku_route_flags=1),  # Такого id нет в таблице msku. Проверяем, что не упадём.
            MarketSku(id=33, msku_route_flags=1),
            MarketSku(id=34, msku_route_flags=1),
            MarketSku(id=36, msku_route_flags=2),  # msku_cargo_type_to_datacamp - не должно быть отправлено
        ]
    ))

    input_topic.write(message.SerializeToString())
    return output_topic.read(2, wait_timeout=10)  # Поставим 2, так как у нас есть ребатчер в конфиге


def test_request_mbo_content(miner, messages, output_topic):
    m = messages[0]
    assert_that(m, IsSerializedProtobuf(DatacampMessage, {
        'market_skus': {
            'msku': [
                {
                    'id': 1234,
                    'mapping_to_offers': {
                        'meta': {
                            'timestamp': {
                                'seconds': META
                            }
                        },
                        'offers': [
                            {
                                'shop_id': 100,
                                'offer_id': 'offer0',
                                'business_id': BUSINESS_ID,
                            },
                            {
                                'shop_id': 100,
                                'offer_id': 'offer1',
                                'business_id': BUSINESS_ID,
                            }
                        ]
                    },
                    'mbo_content': {
                        'msku': {
                            'id': 1234,
                            'parameter_values': [
                                {
                                    'param_id': 10,
                                    'value_type': ValueType.BOOLEAN,
                                    'bool_value': True
                                }
                            ],
                            'titles': [
                                {
                                    'isoCode': 'ru',
                                    'value': 'Msku1234'
                                }
                            ]
                        }
                    }
                },
                {
                    'id': 666,
                }
            ]
        }
    }))

    m = messages[1]
    assert_that(m, IsSerializedProtobuf(DatacampMessage, {
        'market_skus': {
            'msku': [
                {
                    'id': 33,
                    'mapping_to_offers': {
                        'offers': [
                            {'offer_id': 'shop_sku0', 'shop_id': SHOP_ID_WITH_FULFULLMENT_PROGRAM, 'business_id': BUSINESS_ID},
                            {'offer_id': 'shop_sku1', 'shop_id': SHOP_ID_WITH_FULFULLMENT_PROGRAM, 'business_id': BUSINESS_ID},
                        ]
                    }
                },
                {
                    'id': 34,
                    'mapping_to_offers': {
                        'offers': [
                            {'offer_id': 'shop_sku2', 'shop_id': SHOP_ID_WITH_CROSS_DOCK_PROGRAM, 'business_id': BUSINESS_ID},
                        ]
                    }
                }
            ]
        }
    }))

    # проверяем, что в топике больше нет данных, которые мы можем вычитать
    # => msku с флагом карготипного route не будет отправлена
    assert_that(output_topic, HasNoUnreadData())
