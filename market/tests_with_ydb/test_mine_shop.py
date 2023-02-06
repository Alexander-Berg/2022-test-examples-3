# coding: utf-8
import json
import time
import os

from hamcrest import assert_that, has_items, has_entries, not_, equal_to
import pytest

from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock
from market.idx.datacamp.routines.yatf.resources.juggler_mock import JugglerServer
from market.idx.datacamp.routines.yatf.test_env import HttpRoutinesTestEnv, SenderToMinerTestEnv
from market.idx.datacamp.routines.yatf.utils import (
    make_basic_offer,
    make_service_offer,
    make_actual_service_offer,
    make_united_offer_matcher
)
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.datacamp.yatf.utils import create_tech_info_dict, create_resolution_dict
from market.idx.yatf.matchers.env_matchers import IsSerializedJson
from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf


VERDICT_CODE = '49i'


@pytest.fixture(scope='module')
def basic_offers_table_data():
    return [
        make_basic_offer(business_id, shop_sku, merge_data={
            'tech_info': create_tech_info_dict(last_mining=last_mining_ts) if last_mining_ts else {}
        }) for business_id, shop_sku, last_mining_ts in [
            (101, 'T600', None),
            (102, 'T1000', None),
            (103, 'offer.for.disabled.partners.data', None),
            (104, 'offer.for.empty.partners.data', None),
            (201, 'offer.in.time', 2500),
            (201, 'offer.out.of.time', 2500),
            (202, 'offer.in.time', 1500),
            (202, 'offer.out.of.time', 2500),
            (300, 'offer.with.verdict.in.time', 2500),
            (300, 'offer.with.verdict.out.time', 2500),
            (300, 'offer.with.other.verdict', 1500),
        ]
    ]


@pytest.fixture(scope='module')
def service_offers_table_data():
    return [
        make_service_offer(business_id, shop_sku, shop_id, merge_data={
            'tech_info': create_tech_info_dict(last_mining=last_mining_ts) if last_mining_ts else {}
        }) for business_id, shop_sku, shop_id, last_mining_ts in [
            (101, 'T600', 1, None),
            (102, 'T1000', 2, None),
            (102, 'T1000', 22, None),
            (102, 'T1000', 222, None),
            (103, 'offer.for.disabled.partners.data', 3, None),
            (104, 'offer.for.empty.partners.data', 4, None),
            (201, 'offer.in.time', 2201, 1500),
            (201, 'offer.out.of.time', 2201, 2500),
            (202, 'offer.in.time', 2202, 2500),
            (202, 'offer.out.of.time', 2202, 2500),
            (300, 'offer.with.verdict.in.time', 3000, 1500),
            (300, 'offer.with.verdict.out.time', 3000, 2500),
            (300, 'offer.with.other.verdict', 3000, 1500),
        ]
    ]


@pytest.fixture(scope='module')
def actual_service_offers_table_data():
    return [
        make_actual_service_offer(business_id, shop_sku, shop_id, warehouse_id)
        for business_id, shop_sku, shop_id, warehouse_id in [
            (101, 'T600', 1, 10),
        ]
    ] + [
        make_actual_service_offer(business_id, shop_sku, shop_id, warehouse_id, merge_data={
            'resolution': create_resolution_dict(verdict)
        }) for business_id, shop_sku, shop_id, warehouse_id, verdict in [
            (300, 'offer.with.verdict.in.time', 3000, 3, VERDICT_CODE),  # проверяем комбо правил по границам ts и коду вердикта
            (300, 'offer.with.verdict.out.time', 3000, 3, VERDICT_CODE),
            (300, 'offer.with.other.verdict', 3000, 3, '123456'),  # код случайный, убеждаемся, что его игнорим
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
                'business_id': 101,
                'datafeed_id': 123,
                'is_push_partner': True,
                'warehouse_id': 10,
                'is_enabled': True
            }]
        },
        {
            'shop_id': 2,
            'status': 'publish',
            'mbi': [{
                'shop_id': 2,
                'business_id': 102,
                'datafeed_id': 1234,
                'is_push_partner': True,
                'is_enabled': True
            }]
        },
        # Выключенные магазины из 102 бизнеса
        {
            'shop_id': 22,
            'status': 'disable',
        },
        {
            'shop_id': 222,
            'status': 'disable',
            'mbi': [{
                'shop_id': 222,
                'business_id': 102,
                'datafeed_id': 102102,
                'is_push_partner': True,
            }]
        },
        # Выключенный магазин из 103 бизнеса
        {
            'shop_id': 3,
            'status': 'disable',
            'mbi': [{
                'shop_id': 3,
                'business_id': 103,
                'datafeed_id': 103103,
                'is_push_partner': True,
            }]
        },
        {
            'shop_id': 2201,
            'status': 'publish',
            'mbi': [{
                'shop_id': 2201,
                'business_id': 201,
                'datafeed_id': 201201,
                'is_push_partner': True,
                'is_enabled': True,
            }]
        },
        {
            'shop_id': 2202,
            'status': 'publish',
            'mbi': [{
                'shop_id': 2202,
                'business_id': 202,
                'datafeed_id': 202202,
                'is_push_partner': True,
                'is_enabled': True,
            }]
        },
        {
            'shop_id': 3000,
            'status': 'publish',
            'mbi': [{
                'shop_id': 3000,
                'business_id': 300,
                'datafeed_id': 300300,
                'is_push_partner': True,
                'is_enabled': True,
            }]
        },
    ]


@pytest.fixture(scope='module')
def states_table_data():
    return [
        {
            'key': key,
            'state': json.dumps({
                'last_touch_time': time.time()
            })
        } for key in [101, 102, 201, 202, 300]
    ]


@pytest.fixture(scope='module')
def juggler_server_history():
    class JugglerServerHistory():
        request_path = set()
        request_count = 0

        @classmethod
        def callback(cls, request):
            cls.request_count = cls.request_count + 1
            cls.request_path.add(request.path)
            return '{"status":"OK"}'

    return JugglerServerHistory()


@pytest.fixture(scope='module')
def juggler_server(juggler_server_history):
    return JugglerServer(juggler_server_history.callback).init()


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, united_miner_topic, juggler_server):
    cfg = {
        'general': {
            'color': 'white',
        },
        'routines': {
            'enable_mining': True,
            'enable_overload_control': True,
            'force_mining_limit': 10,
            'mining_time': 10**9,
        },
        'juggler': {
            'address': 'http://{}:{}'.format(juggler_server.host, juggler_server.port),
            'mining_host_name': 'mi-datacamp-united',
            'mining_service_name': 'mining-queue-length',
        },
        'miner': {
            'united_topic': united_miner_topic.topic
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


@pytest.yield_fixture(scope='module')
def routines_http(yt_server, config, partners_table, states_table, basic_offers_table,
                    service_offers_table, actual_service_offers_table):
    resources = {
        'partners_table': partners_table,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'states_table': states_table,
        'config': config,
    }
    with HttpRoutinesTestEnv(yt_server, **resources) as routines_http_env:
        yield routines_http_env


@pytest.fixture(scope='module')
def mine_shop(routines_http, states_table):
    response = routines_http.post('/mine?business_id={business_id}'.format(business_id=102))
    assert_that(response, HasStatus(200))

    # Майним несколько бизнесов с временными рамками контроля последнего успешного майнинга офферов
    response = routines_http.post(
        '/mine?business_id={business_id_1}&lower_ts=1000&upper_ts=2000'.format(
            business_id_1=201
        ),
        data=json.dumps({
            'businesses': [202]
        }),
        headers={'Content-Type': 'application/json; charset=utf-8'},
    )
    assert_that(response, HasStatus(200))

    # Майним только офферы, которые были помайнены между 1000 и 2000, и на которых висит ошибка VERDICT_CODE
    request = '/mine?business_id={business_id}&lower_ts=1000&upper_ts=2000&verdict_code={code}'.format(business_id=300, code=VERDICT_CODE)
    response = routines_http.post(request)
    assert_that(response, HasStatus(200))

    states_table.load()

    assert_that(states_table.data, has_items(*[
        has_entries({
            'key': 102,
            'state': IsSerializedJson({
                'force': 'true'
            })
        }),
    ]))

    assert_that(states_table.data, has_items(*[
        has_entries({
            'key': key,
            'state': IsSerializedJson({
                'force': 'true',
                'lower_ts': 1000,
                'upper_ts': 2000,
            })
        }) for key in [201, 202]
    ]))

    assert_that(states_table.data, has_items(*[
        has_entries({
            'key': 300,
            'state': IsSerializedJson({
                'force': 'true',
                'lower_ts': 1000,
                'upper_ts': 2000,
                'verdict_code': VERDICT_CODE,
            })
        }),
    ]))


@pytest.yield_fixture(scope='function')
def sender_to_miner(yt_server, config, mine_shop, united_miner_topic, partners_table,
                    basic_offers_table, service_offers_table, actual_service_offers_table):
    resources = {
        'config': config,
        'partners_table': partners_table,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'united_miner_topic': united_miner_topic,
    }
    with SenderToMinerTestEnv(yt_server, **resources) as miner_env:
        miner_env.verify()
        yield miner_env


def test_complete_mine(sender_to_miner, states_table):
    """Проверяем, что после майнинга сбросился флажок force"""
    states_table.load()
    assert_that(states_table.data, not_(has_items(*[
        has_entries({
            'key': 102,
            'state': IsSerializedJson({
                'force': 'true'
            })
        }),
    ])))


def test_mine_shop(sender_to_miner, united_miner_topic, routines_http):
    """
    Проверяем, что после вызова ручки переобогащения, соответствующий бизнес отправится на переобогащение, при этом
    остальные не отправятся.
    """
    data = united_miner_topic.read(count=4)

    assert_that(data, has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    make_united_offer_matcher(102, 'T1000', 2, 0),
                ]
            }]
        }),
    ]))
    assert_that(data, not_(has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    make_united_offer_matcher(102, 'T1000', 22, 0),
                ]
            }]
        }),
    ])))
    assert_that(data, not_(has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    make_united_offer_matcher(102, 'T1000', 222, 0),
                ]
            }]
        }),
    ])))
    assert_that(data, not_(has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    make_united_offer_matcher(101, 'T600', 1, 10),
                ]
            }]
        }),
    ])))

    assert_that(data, has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    make_united_offer_matcher(201, 'offer.in.time', 2201, 0),
                ]
            }]
        }),
    ]))
    assert_that(data, has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    make_united_offer_matcher(202, 'offer.in.time', 2202, 0),
                ]
            }]
        }),
    ]))
    assert_that(data, not_(has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    make_united_offer_matcher(201, 'offer.out.of.time', 2201, 0),
                ]
            }]
        }),
    ])))
    assert_that(data, not_(has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    make_united_offer_matcher(202, 'offer.out.of.time', 2202, 0),
                ]
            }]
        }),
    ])))

    assert_that(data, has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    make_united_offer_matcher(300, 'offer.with.verdict.in.time', 3000, 3),
                ]
            }]
        }),
    ]))
    assert_that(data, not_(has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    make_united_offer_matcher(300, 'offer.with.verdict.out.time', 3000, 3),
                ]
            }]
        }),
    ])))
    assert_that(data, not_(has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    make_united_offer_matcher(300, 'offer.with.other.verdict', 3000, 3),
                ]
            }]
        }),
    ])))


def test_mine_shop_sync(sender_to_miner, united_miner_topic, routines_http):
    """
    Выключенные магазины отправляются на майнинг, только если в запросе передается параметр sync.
    """
    response = routines_http.post('/mine?business_id={business_id}&sync'.format(business_id=103))
    assert_that(response, HasStatus(200))
    data = united_miner_topic.read(count=1)
    assert_that(data, has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    make_united_offer_matcher(103, 'offer.for.disabled.partners.data', 3, 0),
                ]
            }]
        }),
    ]))

    response = routines_http.post('/mine?business_id={business_id}&sync'.format(business_id=104))
    assert_that(response, HasStatus(200))
    data = united_miner_topic.read(count=1)
    assert_that(data, has_items(*[
        IsSerializedProtobuf(DatacampMessage, {
            'united_offers': [{
                'offer': [
                    make_united_offer_matcher(104, 'offer.for.empty.partners.data', 4, 0),
                ]
            }]
        }),
    ]))


def test_overload_check(sender_to_miner, routines_http, juggler_server_history):
    """ Проверяем, что запросы к juggler выполняются """
    assert_that(juggler_server_history.request_count, equal_to(1))
    assert_that(len(juggler_server_history.request_path), equal_to(1))
    assert_that(
        juggler_server_history.request_path,
        has_items(
            '/api/dashboard/check_details?do=1&limit=1&host=mi-datacamp-united&service=mining-queue-length'
        )
    )


def test_ze_last_test(sender_to_miner, united_miner_topic, routines_http):
    """
    Проверяем, что в топике больше нет данных, которые мы можем вычитать
    """
    assert_that(united_miner_topic, HasNoUnreadData())
