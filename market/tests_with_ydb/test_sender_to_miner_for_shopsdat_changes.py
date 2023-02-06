# coding: utf-8
import json
import time
import os
from hamcrest import assert_that, has_items, anything, not_, has_entries
import pytest

from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.common.SchemaType_pb2 import PUSH

from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock
from market.idx.datacamp.routines.yatf.test_env import SenderToMinerTestEnv
from market.idx.datacamp.yatf.utils import create_tech_info_dict
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobufMap
from market.idx.yatf.matchers.env_matchers import IsSerializedJson
from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData

from market.idx.datacamp.routines.yatf.utils import (
    make_basic_offer,
    make_service_offer,
    make_actual_service_offer
)


WAIT_VALUE = 60 * 60
NEW_SHOPSDATA_MININIG_WAIT_TIME = 6 * 60
NOW = int(time.time())
OLD = NOW - WAIT_VALUE * 2
FUTURE = NOW + WAIT_VALUE


@pytest.fixture(scope='module')
def basic_offers_table_data():
    return [
        make_basic_offer(business_id, shop_sku, merge_data={
            'tech_info': create_tech_info_dict(last_mining=last_mining_ts)
        }) for business_id, shop_sku, last_mining_ts in [
            # offer that was mined recently (has ready shopsdat changes, wasn't mined long time ago)
            (1, 'T1000', NOW),
            # offer that was mined long time ago (has ready shopsdat changes, wasn't mined long time ago)
            (1, 'T2000', 0),
            # offer that was mined recently (has not ready shopsdat changes, wasn't mined long time ago)
            (2, 'T3000', NOW),
            # offer that was mined long time ago (has not ready shopsdat changes, wasn't mined long time ago)
            (2, 'T4000', 0),
            # offer that was mined recently (has ready shopsdat changes, was mined recently)
            (3, 'T5000', NOW),
            # offer that was mined long time ago (has ready shopsdat changes, was mined recently)
            (3, 'T6000', 0),
            # offer that was mined recently (has not ready shopsdat changes, was mined recently)
            (4, 'T7000', NOW),
            # offer that was mined long time ago (has not ready shopsdat changes, was mined recently)
            (4, 'T8000', 0),
            # offer with shop in force mode (has not ready shopsdat changes)
            (5, 'T9000', NOW),
            # offer with shop in force mode (has ready shopsdat changes)
            (6, 'T10000', NOW),
            # offer that was mined recently (shop with deferred mining, should be sent to mining now)
            (7, 'T11000', NOW),
            # offer that was mined long time ago (shop with deferred mining, should be sent to mining now)
            (7, 'T12000', 0),
            # offer that was mined long time ago (shop with deferred mining, should be sent in the future)
            (8, 'T13000', 0),
        ]
    ]


@pytest.fixture(scope='module')
def service_offers_table_data():
    return [
        make_service_offer(business_id, shop_sku, shop_id)
        for business_id, shop_sku, shop_id in [
            (1, 'T1000', 1),
            (1, 'T2000', 1),
            (2, 'T3000', 2),
            (2, 'T4000', 2),
            (3, 'T5000', 3),
            (3, 'T6000', 3),
            (4, 'T7000', 4),
            (4, 'T8000', 4),
            (5, 'T9000', 5),
            (6, 'T10000', 6),
            (7, 'T11000', 7),
            (7, 'T12000', 7),
            (8, 'T13000', 8),
        ]
    ]


@pytest.fixture(scope='module')
def actual_service_offers_table_data():
    return [
        make_actual_service_offer(business_id, shop_sku, shop_id, warehouse_id)
        for business_id, shop_sku, shop_id, warehouse_id in [
            (1, 'T1000', 1, 10),
            (1, 'T2000', 1, 10),
            (2, 'T3000', 2, 10),
            (2, 'T4000', 2, 10),
            (3, 'T5000', 3, 10),
            (3, 'T6000', 3, 10),
            (4, 'T7000', 4, 10),
            (4, 'T8000', 4, 10),
            (5, 'T9000', 5, 10),
            (6, 'T10000', 6, 10),
            (7, 'T11000', 7, 10),
            (7, 'T12000', 7, 10),
            (8, 'T13000', 8, 10),
        ]
    ]


def _partners_record(business_id, shop_id):
    return {
        'shop_id': shop_id,
        'status': 'publish',
        'mbi': {'shop_id': shop_id, 'business_id': business_id, 'is_push_partner': True, 'is_enabled': True, 'warehouse_id': 10},
        'partner_additional_info': {
            'partner_schema': PUSH,
        },
    }


@pytest.fixture(scope='module')
def partners_table_data():
    return [
        _partners_record(1, 1),
        _partners_record(2, 2),
        _partners_record(3, 3),
        _partners_record(4, 4),
        _partners_record(5, 5),
        _partners_record(6, 6),
        _partners_record(7, 7),
        _partners_record(8, 8),
    ]


@pytest.fixture(scope='module')
def states_table_data():
    return [
        {
            'key': 1,
            'state': json.dumps({'mbi_params_dramatically_changed': OLD}),
        },
        {
            'key': 2,
            'state': json.dumps({'mbi_params_dramatically_changed': NOW}),
        },
        {
            'key': 3,
            'state': json.dumps({'mbi_params_dramatically_changed': OLD, 'last_touch_time': NOW}),
        },
        {
            'key': 4,
            'state': json.dumps({'mbi_params_dramatically_changed': NOW, 'last_touch_time': NOW}),
        },
        {
            'key': 5,
            'state': json.dumps({'force': True, 'mbi_params_dramatically_changed': OLD, 'deferred_ts': OLD}),
        },
        {
            'key': 6,
            'state': json.dumps({'force': True, 'mbi_params_dramatically_changed': NOW, 'deferred_ts': FUTURE}),
        },
        {
            'key': 7,
            'state': json.dumps({'deferred_ts': OLD, 'last_touch_time': NOW}),
        },
        {
            'key': 8,
            'state': json.dumps({'deferred_ts': FUTURE, 'last_touch_time': NOW}),
        },
    ]


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, united_miner_topic):
    cfg = {
        'miner': {'united_topic': united_miner_topic.topic},
        'routines': {
            'enable_mining': True,
            'mbi_params_dramatically_changed_mining_time': WAIT_VALUE,
        },
        'ydb': {
            'database_end_point': os.getenv('YDB_ENDPOINT'),
            'database_path': os.getenv('YDB_DATABASE'),
            'coordination_node_path': '/coordination',
            'publishing_semaphore_name': 'mr_cluster_provider_publishing_semaphore'
        },
    }
    return RoutinesConfigMock(yt_server=yt_server, log_broker_stuff=log_broker_stuff, config=cfg)


@pytest.yield_fixture(scope='module')
def sender_to_miner(
        yt_server,
        config,
        partners_table,
        united_miner_topic,
        basic_offers_table,
        service_offers_table,
        actual_service_offers_table,
        states_table,
):
    resources = {
        'config': config,
        'partners_table': partners_table,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'states_table': states_table,
        'united_miner_topic': united_miner_topic,
    }
    with SenderToMinerTestEnv(yt_server, **resources) as miner_env:
        miner_env.verify()
        yield miner_env


def make_united_offer_matcher(business_id, offer_id, shop_id, warehouse_id):
    identifiers = {
        'business_id': business_id,
        'shop_id': shop_id,
        'offer_id': offer_id,
        'warehouse_id': warehouse_id,
    }

    return {
        'basic': {'identifiers': {'business_id': business_id, 'offer_id': offer_id}, 'tech_info': anything()},
        'service': IsProtobufMap({shop_id: {'identifiers': identifiers}}),
    }


@pytest.fixture(scope='module')
def topic_data(sender_to_miner, united_miner_topic):
    data = united_miner_topic.read(count=5)

    assert_that(united_miner_topic, HasNoUnreadData())

    return data


def test_offers_for_shop_with_old_shopsdat_changes_was_sent(topic_data, states_table):
    """
    Проверяем, что оффера магазина, по которому были изменения в шопс дате
    и шопсдат которого по нашему предположению уже раскатился на машинки майнера, отправится на обогащение
    business_id = 1 - магазин, который давно не майнили
    business_id = 3 - магазин, который майнили недавно, но теперь еще и свежая шопс дата подоспела (достаточно искуственный случай, такого не должно встречаться на практике)
    """
    assert_that(
        topic_data,
        has_items(
            IsSerializedProtobuf(
                DatacampMessage, {
                    'united_offers': [
                        {
                            'offer': [
                                make_united_offer_matcher(1, 'T1000', 1, 10),
                                make_united_offer_matcher(1, 'T2000', 1, 10),
                            ]
                        }
                    ]
                }
            ),
            IsSerializedProtobuf(
                DatacampMessage, {
                    'united_offers': [
                        {
                            'offer': [
                                make_united_offer_matcher(3, 'T5000', 3, 10),
                                make_united_offer_matcher(3, 'T6000', 3, 10),
                            ]
                        }
                    ]
                }
            ),
        ),
    )

    # проверим, что в таблице стейтов удалился ts
    states_table.load()
    assert_that(states_table.data, has_items(*[
        has_entries({
            'key': 1,
        }),
        has_entries({
            'key': 3,
        }),
    ]))
    assert_that(states_table.data, not_(has_items(*[
        has_entries({
            'key': 1,
            'state': IsSerializedJson({
                'mbi_params_dramatically_changed': OLD
            })
        }),
    ])))
    assert_that(states_table.data, not_(has_items(*[
        has_entries({
            'key': 3,
            'state': IsSerializedJson({
                'mbi_params_dramatically_changed': OLD
            })
        }),
    ])))


def test_offers_for_shop_with_new_shopsdat_changes_was_not_sent(topic_data, states_table):
    """
    Проверяем, что оффера магазина, по которому были изменения в шопс дате
    и шопсдат которого по нашему предположению НЕ раскатился на машинки майнера, НЕ отправится на обогащение
    business_id = 2 - магазин, который давно не майнили (но не пошлем его обогащаться потому что шопс дата НЕ готова)
    business_id = 4 - магазин, который майнили недавно, но теперь еще и свежая шопс дата подоспела
    """
    assert_that(
        topic_data,
        not_(has_items(
            IsSerializedProtobuf(
                DatacampMessage, {
                    'united_offers': [
                        {
                            'offer': [
                                make_united_offer_matcher(2, 'T3000', 2, 10),
                                make_united_offer_matcher(2, 'T4000', 2, 10),
                            ]
                        }
                    ]
                }
            ),
        )),
    )

    assert_that(
        topic_data,
        not_(has_items(
            IsSerializedProtobuf(
                DatacampMessage, {
                    'united_offers': [
                        {
                            'offer': [
                                make_united_offer_matcher(4, 'T7000', 4, 10),
                                make_united_offer_matcher(4, 'T8000', 4, 10),
                            ]
                        }
                    ]
                }
            ),
        )),
    )

    # проверим, что в таблице стейтов не удалился ts
    states_table.load()
    assert_that(states_table.data, has_items(*[
        has_entries({
            'key': 2,
            'state': IsSerializedJson({
                'mbi_params_dramatically_changed': NOW
            })
        }),
        has_entries({
            'key': 4,
            'state': IsSerializedJson({
                'mbi_params_dramatically_changed': NOW,
                'last_touch_time': NOW
            })
        }),
    ]))


def test_offers_for_shop_with_force_mine(topic_data, states_table):
    """
    Проверяем, что оффера магазина c force режимом отправятся на обогащение
    business_id = 5 - шопс дата готова в обновлению (по нему таблица стейтов должна очиститься - не надо обогащать еще раз)
    business_id = 6 - шопс дата не готова к обновлению (по нему таблица стейтов НЕ должна очиститься - нужно будет обогатить, когда шопс дата додет)
    """
    assert_that(
        topic_data,
        has_items(
            IsSerializedProtobuf(
                DatacampMessage, {
                    'united_offers': [
                        {
                            'offer': [
                                make_united_offer_matcher(5, 'T9000', 5, 10),
                            ]
                        }
                    ]
                }
            ),
            IsSerializedProtobuf(
                DatacampMessage, {
                    'united_offers': [
                        {
                            'offer': [
                                make_united_offer_matcher(6, 'T10000', 6, 10),
                            ]
                        }
                    ]
                }
            ),
        ),
    )

    # проверим таблицу стейтов
    states_table.load()
    assert_that(states_table.data, has_items(*[
        has_entries({
            'key': 5,
        }),
        has_entries({
            'key': 6,
            'state': IsSerializedJson({
                'mbi_params_dramatically_changed': NOW,
                'deferred_ts': FUTURE
            })
        }),
    ]))
    assert_that(states_table.data, not_(has_items(*[
        has_entries({
            'key': 5,
            'state': IsSerializedJson({
                'mbi_params_dramatically_changed': OLD
            })
        }),
    ])))
    assert_that(states_table.data, not_(has_items(*[
        has_entries({
            'key': 5,
            'state': IsSerializedJson({
                'deferred_ts': OLD
            })
        }),
    ])))


def test_offers_for_shop_with_new_shopsdat_were_sent(topic_data, states_table):
    """
    Проверяем, что оффера магазина, по которому только недавно пришла шопсдата (подключение/переключение)
    и шопсдат которого по нашему предположению уже можно взять в динамическом кеше майнера,
    будут отправлены на майнинг
    """
    assert_that(
        topic_data,
        has_items(
            IsSerializedProtobuf(
                DatacampMessage, {
                    'united_offers': [
                        {
                            'offer': [
                                make_united_offer_matcher(7, 'T11000', 7, 10),
                                make_united_offer_matcher(7, 'T12000', 7, 10),
                            ]
                        }
                    ]
                }
            ),
        ),
    )

    # проверим, что в таблице стейтов удалился deferred_ts
    states_table.load()
    assert_that(states_table.data, has_items(*[
        has_entries({
            'key': 7,
        }),
    ]))
    assert_that(states_table.data, not_(has_items(*[
        has_entries({
            'key': 7,
            'state': IsSerializedJson({
                'deferred_ts': OLD
            })
        }),
    ])))


def test_offers_for_shop_with_new_shopsdat_were_deferred(topic_data, states_table):
    """
    Проверяем, что оффера магазина, по которому только недавно пришла шопсдата (подключение/переключение)
    и шопсдат которого по нашему предположению еще нельзя взять в динамическом кеше майнера,
    будут отправлены на майнинг только в будущем, но не сейчас
    """
    assert_that(
        topic_data,
        not_(has_items(
            IsSerializedProtobuf(
                DatacampMessage, {
                    'united_offers': [
                        {
                            'offer': [
                                make_united_offer_matcher(8, 'T13000', 8, 10),
                            ]
                        }
                    ]
                }
            ),
        )),
    )

    # проверим, что в таблице стейтов не удалился deferred_ts
    states_table.load()
    assert_that(states_table.data, has_items(*[
        has_entries({
            'key': 8,
            'state': IsSerializedJson({
                'deferred_ts': FUTURE
            })
        }),
    ]))
