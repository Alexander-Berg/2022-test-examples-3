# coding: utf-8
from hamcrest import assert_that, has_items, has_entry, not_, has_item
import pytest
import os

from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.common.SchemaType_pb2 import PULL_TO_PUSH, PUSH_TO_PULL, PUSH, PULL
from market.idx.datacamp.proto.offer import DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.business.Business_pb2 import BusinessStatus

from market.idx.datacamp.routines.lib.tasks.sender_to_miner import yt_table_state_path
from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock
from market.idx.datacamp.routines.yatf.test_env import SenderToMinerTestEnv
from market.idx.datacamp.routines.yatf.utils import (
    make_basic_offer,
    make_service_offer,
    make_actual_service_offer,
    make_united_offer_matcher
)
from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf

from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampBusinessStatusTable,
)

from market.idx.yatf.utils.utils import create_pb_timestamp


EXPECTED_MINED_OFFERS = 13


@pytest.fixture(scope='module')
def basic_offers_table_data():
    return [
        make_basic_offer(business_id, shop_sku, merge_data={
            'identifiers': {
                'feed_id': feed_id
            }
        }) for business_id, shop_sku, feed_id in [
            (1, 'T600', None),
            (1, 'T700', None),
            (1, 'T800', None),
            (2, 'T800', None),
            (4, 'T900', None),
            (5, 'T1000', None),
            (6, 'T1100', None),
            (7, 'T1200', None),
            (8, 'T1300', 801),
            (8, 'T1400', 802),
            (8, 'T1500', None),
            (9, 'T1600', None),
            (10, 'T2000', None),
            (10, 'T2001', None),
            (10, 'T2002', None),
            (10, 'T2003', None),
            (11, 'T3000', None),
            (12, 'ff.offer.without.actual', None),
            (13, 'offer.with.zero.actual', None),
            (14, 'lavka_offer', None),
            (15, 'eda_offer', None),
            (16, 'offer.alive', None),
            (17, 'offer.clone.1', None),
            (17, 'offer.clone.2', None),
            (17, 'offer.clone.3', None),
            (18, 'offer.ARCHIVE', None),
            (18, 'offer.SORTDC_STUB', None),
        ]
    ]


@pytest.fixture(scope='module')
def service_offers_table_data():
    return [
        make_service_offer(business_id, shop_sku, shop_id, merge_data={
            'identifiers': {
                'feed_id': feed_id
            }
        }) for business_id, shop_sku, shop_id, feed_id in [
            (1, 'T600', 1, None),
            (1, 'T700', 1, None),
            (1, 'T800', 1, None),
            (2, 'T800', 2, None),
            (4, 'T900', 4, None),
            (5, 'T1000', 5, None),
            (6, 'T1100', 6, None),
            (7, 'T1200', 7, None),
            (8, 'T1300', 8, 801),
            (8, 'T1400', 8, 802),
            (8, 'T1500', 8, None),
            (9, 'T1600', 9, None),
            (10, 'T2000', 10, None),
            (10, 'T2001', 10, None),
            (10, 'T2002', 10, None),
            (10, 'T2003', 10, None),
            (11, 'T3000', 11, None),
            (12, 'ff.offer.without.actual', 12, None),
            (13, 'offer.with.zero.actual', 13, None),
            (14, 'lavka_offer', 14, None),
            (15, 'eda_offer', 15, None),
            (16, 'offer.alive', 16, None),
            (17, 'offer.clone.1', 171, None),
            (17, 'offer.clone.2', 171, None),
            (17, 'offer.clone.3', 171, None),
            (17, 'offer.clone.1', 172, None),
            (17, 'offer.clone.2', 172, None),
            (17, 'offer.clone.3', 172, None),
        ]
    ] + [
        make_service_offer(18, 'offer.ARCHIVE', 181, merge_data={
            'content': {
                'partner': {
                    'original_terms': {
                        'supply_plan': {
                            'value': DTC.SupplyPlan.ARCHIVE,
                        },
                    },
                },
            }
        }),
        make_service_offer(18, 'offer.SORTDC_STUB', 181, merge_data={
            'meta': {
                'sortdc_context': {
                    'export_items': [
                        {
                            'offer_type': 'FEED_OFFER'
                        }
                    ]
                },
                'rgb': DTC.VERTICAL_GOODS_ADS,
            }
        }),
    ]


@pytest.fixture(scope='module')
def actual_service_offers_table_data():
    return [
        make_actual_service_offer(business_id, shop_sku, shop_id, warehouse_id, merge_data={
            'identifiers': {
                'feed_id': feed_id
            }
        }) for business_id, shop_sku, shop_id, warehouse_id, feed_id in [
            (1, 'T600', 1, 10, None),
            (1, 'T600', 1, 33, None),
            (1, 'T700', 1, 10, None),
            (1, 'T800', 1, 10, None),
            (2, 'T800', 2, 10, None),
            (4, 'T900', 4, 10, None),
            (5, 'T1000', 5, 10, None),
            (6, 'T1100', 6, 10, None),
            (7, 'T1200', 7, 10, None),
            (8, 'T1300', 8, 10, 801),
            (8, 'T1400', 8, 10, 802),
            (8, 'T1500', 8, 10, None),
            (9, 'T1600', 9, 10, None),
            (10, 'T2000', 10, 10, None),
            (10, 'T2000', 10, 11, None),
            (10, 'T2000', 10, 12, None),
            (10, 'T2000', 10, 13, None),
            (10, 'T2001', 10, 10, None),
            (10, 'T2001', 10, 11, None),
            (10, 'T2001', 10, 12, None),
            (10, 'T2001', 10, 13, None),
            (10, 'T2002', 10, 10, None),
            (10, 'T2002', 10, 11, None),
            (10, 'T2002', 10, 12, None),
            (10, 'T2002', 10, 13, None),
            (10, 'T2003', 10, 10, None),
            (10, 'T2003', 10, 11, None),
            (10, 'T2003', 10, 12, None),
            (10, 'T2003', 10, 13, None),
            (11, 'T3000', 11, 0, None),
            (13, 'offer.with.zero.actual', 13, 0, None),
            (16, 'offer.alive', 16, 14, None),
            (17, 'offer.clone.1', 171, 0, None),
            (17, 'offer.clone.2', 171, 0, None),
            (17, 'offer.clone.3', 171, 0, None),
            (17, 'offer.clone.1', 172, 0, None),
            (17, 'offer.clone.2', 172, 0, None),
            (17, 'offer.clone.3', 172, 0, None),
            (18, 'offer.ARCHIVE', 181, 10, None),
            (18, 'offer.SORTDC_STUB', 181, 10, None),
        ]
    ]


@pytest.fixture(scope='module')
def partners_table_data():
    return [
        {
            'shop_id': 1,
            'status': 'publish',
            'mbi': [{
                'shop_id': 1,
                'business_id': 1,
                'datafeed_id': 123,
                'is_push_partner': True,
                'warehouse_id': 10,
                'is_enabled': True
            }, {
                'shop_id': 1,
                'business_id': 1,
                'datafeed_id': 124,
                'is_push_partner': True,
                'warehouse_id': 33,
                'is_enabled': True
            }]
        },
        {
            'shop_id': 2,
            'status': 'disable',
        },
        # not push partner
        {
            'shop_id': 3,
            'status': 'publish',
            'mbi': {
                'shop_id': 3,
                'business_id': 3,
                'datafeed_id': 1234,
                'warehouse_id': 10,
                'is_enabled': True
            }
        },
        # not push partner
        {
            'shop_id': 4,
            'status': 'publish',
            'mbi': {
                'shop_id': 4,
                'business_id': 4,
                'datafeed_id': 1234,
                'is_push_partner': True,
                'warehouse_id': 10,
                'is_enabled': True
            },
            'partner_additional_info': {
                'partner_change_schema_process': {
                    'start_ts': create_pb_timestamp().ToJsonString(),
                    'change_schema_type': PUSH_TO_PULL,
                }
            }
        },
        # not push partner
        {
            'shop_id': 5,
            'status': 'publish',
            'mbi': {
                'shop_id': 5,
                'datafeed_id': 1234,
                'business_id': 5,
                'is_push_partner': True,
                'warehouse_id': 10,
                'is_enabled': True
            },
            'partner_additional_info': {
                'partner_schema': PULL,
            }
        },
        # push partner
        {
            'shop_id': 6,
            'status': 'publish',
            'mbi': {
                'shop_id': 6,
                'datafeed_id': 1234,
                'business_id': 6,
                'is_push_partner': False,
                'warehouse_id': 10,
                'is_enabled': True
            },
            'partner_additional_info': {
                'partner_change_schema_process': {
                    'start_ts': create_pb_timestamp().ToJsonString(),
                    'change_schema_type': PULL_TO_PUSH,
                }
            }
        },
        # push partner
        {
            'shop_id': 7,
            'status': 'publish',
            'mbi': {
                'shop_id': 7,
                'datafeed_id': 1234,
                'business_id': 7,
                'is_push_partner': False,
                'warehouse_id': 10,
                'is_enabled': True
            },
            'partner_additional_info': {
                'partner_schema': PUSH,
            }
        },
        # push partner
        {
            'shop_id': 8,
            'status': 'publish',
            'mbi': {
                'shop_id': 8,
                'business_id': 8,
                'is_push_partner': True,
                'warehouse_id': 10,
                'is_enabled': True
            },
            'partner_additional_info': {
                'partner_schema': PUSH,
            }
        },
        # push partner
        {
            'shop_id': 9,
            'status': 'publish',
            'mbi': {
                'shop_id': 9,
                'business_id': 9,
                'is_push_partner': True,
                'warehouse_id': 10,
                'is_enabled': True
            },
            'partner_additional_info': {
                'partner_schema': PUSH,
            }
        },
        # large push partner
        {
            'shop_id': 10,
            'status': 'publish',
            'mbi': [{
                'shop_id': 10,
                'business_id': 10,
                'is_push_partner': True,
                'warehouse_id': 10,
                'is_enabled': True
            }, {
                'shop_id': 10,
                'business_id': 10,
                'is_push_partner': True,
                'warehouse_id': 11,
                'is_enabled': True
            }, {
                'shop_id': 10,
                'business_id': 10,
                'is_push_partner': True,
                'warehouse_id': 12,
                'is_enabled': True
            }, {
                'shop_id': 10,
                'business_id': 10,
                'is_push_partner': True,
                'warehouse_id': 13,
                'is_enabled': True
            }],
            'partner_additional_info': {
                'partner_schema': PUSH,
            }
        },
        # blocked business
        {
            'shop_id': 11,
            'status': 'publish',
            'mbi': {
                'shop_id': 11,
                'business_id': 11,
                'datafeed_id': 11000,
                'is_push_partner': True,
                'is_enabled': True
            }
        },
        # ff push partner
        {
            'shop_id': 12,
            'status': 'publish',
            'mbi': [{
                'shop_id': 12,
                'business_id': 12,
                'is_push_partner': True,
                'warehouse_id': 145,
                'is_enabled': True
            }, {
                'shop_id': 12,
                'business_id': 12,
                'is_push_partner': True,
                'warehouse_id': 147,
                'is_enabled': True
            }],
            'partner_additional_info': {
                'partner_schema': PUSH,
            }
        },
        # push partner for test zero warehouses mining
        {
            'shop_id': 13,
            'status': 'publish',
            'mbi': [{
                'shop_id': 13,
                'business_id': 13,
                'is_push_partner': True,
                'warehouse_id': 654321,
                'is_enabled': True
            }],
            'partner_additional_info': {
                'partner_schema': PUSH,
            }
        },
        # Lavka
        {
            'shop_id': 14,
            'status': 'publish',
            'mbi': [{
                'shop_id': 14,
                'business_id': 14,
                'is_lavka': True,
                'is_enabled': True
            }],
        },
        # Eda
        {
            'shop_id': 15,
            'status': 'publish',
            'mbi': [{
                'shop_id': 15,
                'business_id': 15,
                'is_eats': True,
                'is_enabled': True
            }],
        },
        # is_alive - Магазин был активен в пределах X дней, по сути выключен, статус в partners - 'publish'
        {
            'shop_id': 16,
            'status': 'publish',
            'mbi': [{
                'shop_id': 16,
                'business_id': 16,
                'warehouse_id': 14,
                'is_push_partner': True,
                'is_alive': True,
            }],
            'partner_additional_info': {
                'partner_schema': PUSH,
            }
        },
        # clones
        {
            'shop_id': 171,
            'status': 'publish',
            'mbi': [{
                'shop_id': 171,
                'business_id': 17,
                'is_push_partner': True,
                'warehouse_id': 10,
                'is_enabled': True
            }],
            'partner_additional_info': {
                'partner_schema': PUSH,
            }
        },
        {
            'shop_id': 172,
            'status': 'publish',
            'mbi': [{
                'shop_id': 172,
                'business_id': 17,
                'is_push_partner': True,
                'warehouse_id': 10,
                'is_enabled': True
            }],
            'partner_additional_info': {
                'partner_schema': PUSH,
            }
        },
        # push partner
        {
            'shop_id': 181,
            'status': 'publish',
            'mbi': {
                'shop_id': 181,
                'business_id': 18,
                'is_push_partner': True,
                'warehouse_id': 10,
                'is_enabled': True
            },
            'partner_additional_info': {
                'partner_schema': PUSH,
            }
        },
    ]


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, united_miner_topic):
    cfg = {
        'general': {
            'color': 'blue',
            'agile_basic_select_rows_limit_enabled': True,
            'basic_select_rows_limit': 5,
            'united_select_rows_limit': 5,
            'shops_amount_to_enable_agile_select_limit': 1,
            'agile_basic_select_rows_limit_minimum': 1
        },
        'miner': {
            'united_topic': united_miner_topic.topic
        },
        'routines': {
            'enable_mining': True,
        },
        'ydb': {
            'database_end_point': os.getenv('YDB_ENDPOINT'),
            'database_path': os.getenv('YDB_DATABASE'),
            'coordination_node_path': '/coordination',
            'publishing_semaphore_name': 'mr_cluster_provider_publishing_semaphore'
        },
    }
    return RoutinesConfigMock(
        yt_server=yt_server,
        log_broker_stuff=log_broker_stuff,
        config=cfg)


@pytest.fixture(scope='module')
def business_status_table(yt_server, config):
    return DataCampBusinessStatusTable(yt_server, config.yt_business_status_tablepath, data=[{
        'business_id': 11,
        'status': BusinessStatus(
            value=BusinessStatus.Status.LOCKED,
        ).SerializeToString(),
    }])


@pytest.yield_fixture(scope='module')
def sender_to_miner(yt_server, config, partners_table, united_miner_topic, business_status_table,
                    basic_offers_table, service_offers_table, actual_service_offers_table):
    resources = {
        'config': config,
        'partners_table': partners_table,
        'business_status_table': business_status_table,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'united_miner_topic': united_miner_topic,
    }
    with SenderToMinerTestEnv(yt_server, **resources) as miner_env:
        miner_env.verify()
        yield miner_env


@pytest.fixture(scope='module')
def united_miner_topic_data(sender_to_miner, united_miner_topic):
    united_data = united_miner_topic.read(count=EXPECTED_MINED_OFFERS)
    # проверяем, что в топике больше нет данных, которые мы можем вычитать
    assert_that(united_miner_topic, HasNoUnreadData())

    return united_data


def test_sender(sender_to_miner, united_miner_topic_data):
    """Проверяем, что если таблицы со стейтом нету, то она создастся и все офера магазинов со статусом `publish`
     отправятся на переобогащение
    """
    assert_that(united_miner_topic_data, has_item(
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    make_united_offer_matcher(1, 'T600', 1, 10),
                    make_united_offer_matcher(1, 'T700', 1, 10),
                    make_united_offer_matcher(1, 'T800', 1, 10),
                    make_united_offer_matcher(1, 'T600', 1, 33),
                ]
            }]
        })
    ))


def test_mine_pull_to_push(united_miner_topic_data):
    """оффера партнера в переходном периоде pull->push должны поехать на майнинг
    """
    assert_that(united_miner_topic_data, has_item(
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    make_united_offer_matcher(6, 'T1100', 6, 10),
                ]
            }]
        })
    ))


def test_mine_push_partners(united_miner_topic_data):
    """оффера партнера с целевой схемой push должны поехать на майнинг
    """
    assert_that(united_miner_topic_data, has_item(
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    make_united_offer_matcher(7, 'T1200', 7, 10),
                ]
            }]
        })
    ))


def test_split_batches_by_feed_id(united_miner_topic_data):
    """в одном батче только один feed_id
    """
    assert_that(united_miner_topic_data, has_item(
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    make_united_offer_matcher(8, 'T1300', 8, 10, 801),
                    make_united_offer_matcher(8, 'T1400', 8, 10, 802),
                    make_united_offer_matcher(8, 'T1500', 8, 10),
                ]
            }]
        })
    ))


def test_mine_without_service_offer(united_miner_topic_data):
    """Наличие записи в таблице ServiceOffers не является обязательным
    Синие партнеры иногда привозят на FF-склад товары, но забывают добавить их в прайс-лист
    Информирование партнера о таком событии происходит в miner
    """
    assert_that(united_miner_topic_data, has_item(
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    make_united_offer_matcher(9, 'T1600', 9, 10),
                ]
            }]
        })
    ))


def test_mine_large_business(united_miner_topic_data):
    """Проверяем, что связь 1:М майнится для больших магазинов (число офферов больше united_select_rows_limit)
    """
    assert_that(united_miner_topic_data, has_item(
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    make_united_offer_matcher(10, 'T2000', 10, 10),
                    make_united_offer_matcher(10, 'T2000', 10, 11),
                    make_united_offer_matcher(10, 'T2000', 10, 12),
                    make_united_offer_matcher(10, 'T2000', 10, 13),
                    make_united_offer_matcher(10, 'T2001', 10, 10),
                    make_united_offer_matcher(10, 'T2001', 10, 11),
                    make_united_offer_matcher(10, 'T2001', 10, 12),
                    make_united_offer_matcher(10, 'T2001', 10, 13),
                    make_united_offer_matcher(10, 'T2002', 10, 10),
                    make_united_offer_matcher(10, 'T2002', 10, 11),
                    make_united_offer_matcher(10, 'T2002', 10, 12),
                    make_united_offer_matcher(10, 'T2002', 10, 13),
                    make_united_offer_matcher(10, 'T2003', 10, 10),
                    make_united_offer_matcher(10, 'T2003', 10, 11),
                    make_united_offer_matcher(10, 'T2003', 10, 12),
                    make_united_offer_matcher(10, 'T2003', 10, 13),
                ]}]
        })
    ))


def test_mine_ff_without_actual(united_miner_topic_data):
    """Проверяем, что на майнинг отправляется FF-оффер без актуальной части
    """
    assert_that(united_miner_topic_data, has_item(
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    make_united_offer_matcher(12, 'ff.offer.without.actual', 12, 0),
                ]
            }]
        })
    ))


def test_mine_zero_warehouse_id(united_miner_topic_data):
    """Проверяем, что на майнинг отправляется оффер с нулевым складом в актуальной части
    """
    assert_that(united_miner_topic_data, has_item(
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    make_united_offer_matcher(13, 'offer.with.zero.actual', 13, 0),
                ]
            }]
        })
    ))


def test_mine_lavka(united_miner_topic_data):
    """Проверяем, что офферы Лавки отправляются на майнинг
    """
    assert_that(united_miner_topic_data, has_item(
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    make_united_offer_matcher(14, 'lavka_offer', 14, None),
                ]
            }]
        })
    ))


def test_mine_eda(united_miner_topic_data):
    """Проверяем, что офферы Еды отправляются на майнинг
    """
    assert_that(united_miner_topic_data, has_item(
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    make_united_offer_matcher(15, 'eda_offer', 15, None),
                ]
            }]
        })
    ))


def test_mine_is_alive(united_miner_topic_data):
    """Проверяем, что на майнинг отправляется оффер is_alive магазина (недавно выключен - продолжаем майнить)
    """
    assert_that(united_miner_topic_data, has_item(
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    make_united_offer_matcher(16, 'offer.alive', 16, 14),
                ]
            }]
        })
    ))


def test_agile_mining(united_miner_topic_data):
    """Проверяем работу гибкого лимита на select базовых частей: под бизнесом 2 магазина,
    basic_select_rows_limit/2=2 базовых оффера в первом запросе
    """
    assert_that(united_miner_topic_data, has_item(
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    make_united_offer_matcher(17, 'offer.clone.1', 171, 0),
                    make_united_offer_matcher(17, 'offer.clone.1', 172, 0),
                    make_united_offer_matcher(17, 'offer.clone.2', 171, 0),
                    make_united_offer_matcher(17, 'offer.clone.2', 172, 0),
                ]}]
        })
    ))

    assert_that(united_miner_topic_data, has_item(
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    make_united_offer_matcher(17, 'offer.clone.3', 171, 0),
                    make_united_offer_matcher(17, 'offer.clone.3', 172, 0),
                ]}]
        })
    ))


def test_do_not_mine_archived(united_miner_topic_data):
    """Проверяем, что оффера с признаком ARCHIVE не отправляются на майнинг
    """
    assert_that(united_miner_topic_data, not_(has_item(IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [
                {
                    'basic': {
                        'identifiers': {
                            'business_id': 18,
                            'offer_id': 'offer.ARCHIVE',
                        },
                    },
                },
            ]
        }]
    }))))


def test_do_not_mine_sortdc_stubs(united_miner_topic_data):
    """Проверяем, что офферы, являющиеся стабами SortDC, не отправляются на майнинг
    """
    assert_that(united_miner_topic_data, not_(has_item(IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [
                {
                    'basic': {
                        'identifiers': {
                            'business_id': 18,
                            'offer_id': 'offer.SORTDC_STUB',
                        },
                    },
                },
            ]
        }]
    }))))


def test_table_state(sender_to_miner, config, yt_server):
    """ Проверяем, что после отправки на переобогащение, в стейт таблицу добавились перемайневшиеся магазины"""
    tablepath = yt_table_state_path(config)
    yt_client = yt_server.get_yt_client()

    rows = list(yt_client.select_rows('* from [{table}]'.format(table=tablepath)))

    assert_that(rows, has_items(*[
        has_entry('key', 1),
        has_entry('key', 6),
        has_entry('key', 7),
        has_entry('key', 8),
        has_entry('key', 9),
        has_entry('key', 10),
    ]))
