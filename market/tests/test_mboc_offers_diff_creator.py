# coding: utf-8

import pytest
import mock
import logging
from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock
from market.idx.datacamp.routines.yatf.test_env import MbocOffersDiffCreatorAndSenderEnv
from market.idx.yatf.resources.yt_table_resource import YtDynTableResource
from yt.wrapper.ypath import ypath_join
from market.idx.yatf.utils.utils import rows_as_table
from market.idx.yatf.resources.lbk_topic import LbkTopic
from hamcrest import assert_that
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages

NEW_GENERATION_0 = '20200101_0000'
NEW_GENERATION_1 = '20200102_0000'
NEW_GENERATION_2 = '20200302_0000'  # Прошло слишком много времени с момента последнего запуска
MBOC_OFFERS_DIR = '//home/mboc_offers_expanded'
# Эмулируем изменение дин-таблицы, за счёт указания разных таблиц
MBOC_OFFERS_EXTERNAL_TABLE_0 = '//home/mboc_offers_external_table0'
MBOC_OFFERS_EXTERNAL_TABLE_1 = '//home/mboc_offers_external_table1'
MBOC_OFFERS_EXTERNAL_TABLE_2 = '//home/mboc_offers_external_table2'
STATES_NUM = 2


MBOC_OFFERS_EXTERNAL_TABLE_DATA_0 = [
    {'supplier_id': 10, 'shop_sku': 'shop_sku0', 'approved_market_sku_id': 0, 'title': None, 'business_id': 1},
    {'supplier_id': 10, 'shop_sku': 'shop_sku1', 'approved_market_sku_id': 100, 'title': None, 'business_id': 1},
    {'supplier_id': 10, 'shop_sku': 'shop_sku2', 'approved_market_sku_id': 100, 'title': 'sometitle', 'approved_sku_mapping_ts': 'somets', 'business_id': 1},
    {'supplier_id': 11, 'shop_sku': 'shop_sku1', 'approved_market_sku_id': 111, 'title': 'sometitle', 'approved_sku_mapping_ts': 'somets', 'business_id': 1},
    {'supplier_id': 11, 'shop_sku': 'shop_sku2', 'approved_market_sku_id': 111, 'title': 'sometitle', 'approved_sku_mapping_ts': 'somets', 'business_id': 1},
    {'supplier_id': 12, 'shop_sku': 'shop_sku1', 'approved_market_sku_id': 121, 'title': 'sometitle', 'approved_sku_mapping_ts': 'somets', 'business_id': 1},
    {'supplier_id': 13, 'shop_sku': 'shop_sku1', 'approved_market_sku_id': 10, 'title': 'sometitle', 'approved_sku_mapping_ts': 'somets', 'business_id': 1},
    {'supplier_id': 13, 'shop_sku': 'shop_sku100', 'title': 'sometitle', 'business_id': 1},  # Нет колонки approved_market_sku_id
    {'supplier_id': 13, 'shop_sku': 'shop_sku101',  'approved_market_sku_id': None, 'title': 'sometitle', 'business_id': 1},  # В колонке None
    {'supplier_id': 13, 'shop_sku': 'shop_sku102',  'approved_market_sku_id': 0, 'title': 'sometitle', 'business_id': 1},  # В колонке 0
]

MBOC_OFFERS_EXTERNAL_TABLE_DATA_1 = [
    {'supplier_id': 10, 'shop_sku': 'shop_sku0', 'approved_market_sku_id': 0, 'title': None, 'business_id': 1},
    {'supplier_id': 10, 'shop_sku': 'shop_sku1', 'approved_market_sku_id': 100, 'title': None, 'business_id': 1},
    {'supplier_id': 10, 'shop_sku': 'shop_sku2', 'approved_market_sku_id': 100, 'title': 'sometitle',
     'approved_sku_mapping_ts': 'somets', 'business_id': 1},
    {'supplier_id': 10, 'shop_sku': 'shop_sku3', 'approved_market_sku_id': 100, 'title': 'sometitle',
     'approved_sku_mapping_ts': 'somets', 'business_id': 1},  # добавлен оффер к msku 100
    {'supplier_id': 11, 'shop_sku': 'shop_sku1', 'approved_market_sku_id': 111, 'title': 'sometitle',
     'approved_sku_mapping_ts': 'somets', 'business_id': 1},
    # {'id': 5, 'supplier_id': 11, 'shop_sku': 'shop_sku2', 'approved_market_sku_id': 111, 'title': 'sometitle', 'approved_sku_mapping_ts': 'somets'},  # удалён оффер из msku 111
    {'supplier_id': 12, 'shop_sku': 'shop_sku1', 'approved_market_sku_id': 141, 'title': 'sometitle',
     'approved_sku_mapping_ts': 'somets', 'business_id': 1},  # поменялся маппинг у оффера. Задело msku 121, 141
    {'supplier_id': 13, 'shop_sku': 'shop_sku1', 'approved_market_sku_id': 10, 'title': 'sometitle',
     'approved_sku_mapping_ts': 'somets', 'business_id': 1},  # у msku 10 ничего не поменялось
    {'supplier_id': 13, 'shop_sku': 'shop_sku100', 'title': 'sometitle', 'business_id': 1},
    {'supplier_id': 13, 'shop_sku': 'shop_sku101', 'approved_market_sku_id': None, 'title': 'sometitle', 'business_id': 1},
    {'supplier_id': 13, 'shop_sku': 'shop_sku102', 'approved_market_sku_id': 0, 'title': 'sometitle', 'business_id': 1},
]

MBOC_OFFERS_EXTERNAL_TABLE_DATA_2 = []

CONFIG = {
    'general': {
        'color': 'white',
        'yt_home': '//home/datacamp'
    },
    'mboc_offers': {
        'enable_mboc_offers_creator_and_sender': True,
        'mboc_offers_dir': MBOC_OFFERS_DIR,
        'states_num': STATES_NUM,
        'mboc_offers_big_dates_difference_sec': 432000,
        'mboc_offers_send_diff_to_datacamp_topic': True,
        'mboc_offers_mskus_in_message': 2  # Специально поставим 2, чтобы проверить разбиение на сообщения
    },
}

log = logging.getLogger('')


def mboc_offers_external_attributes():
    schema = [
        dict(name='supplier_id', type='int64', sort_order='ascending'),
        dict(name='shop_sku', type='string', sort_order='ascending'),
        dict(name='approved_market_sku_id', type='int64'),
        dict(name='approved_sku_mapping_ts', type='string'),
        dict(name='title', type='string'),
        dict(name='business_id', type='int64'),
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
def datacamp_internal_msku_topic(log_broker_stuff):
    return LbkTopic(log_broker_stuff)


def prepare_config(external_mboc_offers_table, yt_server, log_broker_stuff, datacamp_internal_msku_topic):
    c = dict(CONFIG)
    c['mboc_offers']['mboc_offers_external_table'] = external_mboc_offers_table
    c['mboc_offers']['datacamp_msku_topic'] = datacamp_internal_msku_topic.topic
    c['yt'] = {'map_reduce_proxies': [yt_server.get_yt_client().config["proxy"]["url"]]}
    config = RoutinesConfigMock(
        yt_server,
        log_broker_stuff=log_broker_stuff,
        config=c
    )
    return config


@pytest.fixture(scope='module')
def config_0(yt_server, datacamp_internal_msku_topic, log_broker_stuff):
    return prepare_config(MBOC_OFFERS_EXTERNAL_TABLE_0, yt_server, log_broker_stuff, datacamp_internal_msku_topic)


@pytest.fixture(scope='module')
def config_1(yt_server, datacamp_internal_msku_topic, log_broker_stuff):
    return prepare_config(MBOC_OFFERS_EXTERNAL_TABLE_1, yt_server, log_broker_stuff, datacamp_internal_msku_topic)


@pytest.fixture(scope='module')
def config_2(yt_server, datacamp_internal_msku_topic, log_broker_stuff):
    return prepare_config(MBOC_OFFERS_EXTERNAL_TABLE_2, yt_server, log_broker_stuff, datacamp_internal_msku_topic)


@pytest.fixture(scope='module')
def mboc_offers_external_table_0(yt_server, config_0):
    return MbocOffersExternalTable(yt_server, config_0.mboc_offers_external_table, data=MBOC_OFFERS_EXTERNAL_TABLE_DATA_0)


@pytest.fixture(scope='module')
def mboc_offers_external_table_1(yt_server, config_1):
    return MbocOffersExternalTable(yt_server, config_1.mboc_offers_external_table, data=MBOC_OFFERS_EXTERNAL_TABLE_DATA_1)


@pytest.fixture(scope='module')
def mboc_offers_external_table_2(yt_server, config_2):
    return MbocOffersExternalTable(yt_server, config_2.mboc_offers_external_table, data=MBOC_OFFERS_EXTERNAL_TABLE_DATA_2)


@pytest.yield_fixture(scope='module')
def routines_0(
        yt_server,
        config_0,
        mboc_offers_external_table_0,
        datacamp_internal_msku_topic,
):
    resources = {
        'mboc_offers_external_table': mboc_offers_external_table_0,
        'config': config_0,
        'datacamp_internal_msku_topic': datacamp_internal_msku_topic
    }
    with mock.patch('market.idx.datacamp.routines.lib.tasks.mboc_offers_diff_creator.create_generation_name', return_value=NEW_GENERATION_0):
        with MbocOffersDiffCreatorAndSenderEnv(yt_server, **resources) as routines_env:
            yield routines_env


def print_table(table_data, path):
    log.info('path\n{}\n{}'.format(path, rows_as_table(table_data)))


@pytest.yield_fixture(scope='module')
def internal_test_0(routines_0, yt_server):
    yt_client = yt_server.get_yt_client()
    tables = yt_client.list(ypath_join(MBOC_OFFERS_DIR, 'states'))

    # I. Проверяем, что создана директория и в ней есть таблица и recent
    assert tables == [NEW_GENERATION_0, 'recent']
    assert 'recent' in tables

    # II. Проверяем содежимое таблицы и её атрибуты
    p = ypath_join(MBOC_OFFERS_DIR, 'states', 'recent')
    assert bool(yt_client.get(ypath_join(p, '@dynamic'))) is True
    assert yt_client.get_attribute(p, 'key_columns') == ['msku_id', 'supplier_id', 'shop_sku']

    last_copied_mboc_table_data = list(yt_client.read_table(p))
    print_table(last_copied_mboc_table_data, p)


@pytest.yield_fixture(scope='module')
def routines_1(
        internal_test_0,
        yt_server,
        config_1,
        mboc_offers_external_table_1,
        datacamp_internal_msku_topic
):
    resources = {
        'mboc_offers_external_table': mboc_offers_external_table_1,
        'config': config_1,
        'datacamp_internal_msku_topic': datacamp_internal_msku_topic
    }
    with mock.patch('market.idx.datacamp.routines.lib.tasks.mboc_offers_diff_creator.create_generation_name', return_value=NEW_GENERATION_1):
        with MbocOffersDiffCreatorAndSenderEnv(yt_server, **resources) as routines_env:
            yield routines_env


@pytest.yield_fixture(scope='module')
def internal_test_1(routines_1, yt_server, datacamp_internal_msku_topic):
    yt_client = yt_server.get_yt_client()

    diff_table = ypath_join(MBOC_OFFERS_DIR, 'diffs', '{}-{}_diff'.format(NEW_GENERATION_1, NEW_GENERATION_0))
    assert bool(yt_client.exists(diff_table)) is True
    diff_data = list(yt_client.read_table(diff_table))
    print_table(diff_data, diff_table)

    # 1. Проверяем содержание diff таблицы
    assert diff_data == [
        {'msku_id': 100},
        {'msku_id': 111},
        {'msku_id': 121},
        {'msku_id': 141}
    ]

    # 2. Проверяем содержимое топика. Ожидаем два сообщения (mboc_offers_mskus_in_message = 2 в конфиге)
    # 'msku_route_flags': 1 - EMskuRouteFlag::TO_IRIS - офферы из диффа отправляются только iris
    messages = datacamp_internal_msku_topic.read(2, wait_timeout=10)
    assert_that(messages, HasSerializedDatacampMessages([
        {
            'market_skus': {
                'msku': [
                    {'id': 100, 'msku_route_flags': 1},
                    {'id': 111, 'msku_route_flags': 1},
                ]
            }
        },
        {
            'market_skus': {
                'msku': [
                    {'id': 121, 'msku_route_flags': 1},
                    {'id': 141, 'msku_route_flags': 1},
                ]
            }
        }
    ]))


@pytest.yield_fixture(scope='module')
def routines_2(
        internal_test_1,
        yt_server,
        config_2,
        mboc_offers_external_table_2,
        datacamp_internal_msku_topic
):
    resources = {
        'mboc_offers_external_table': mboc_offers_external_table_2,
        'config': config_2,
        'datacamp_internal_msku_topic': datacamp_internal_msku_topic
    }
    with mock.patch('market.idx.datacamp.routines.lib.tasks.mboc_offers_diff_creator.create_generation_name', return_value=NEW_GENERATION_2):
        with MbocOffersDiffCreatorAndSenderEnv(yt_server, **resources) as routines_env:
            yield routines_env


@pytest.yield_fixture(scope='module')
def internal_test_2(routines_2, yt_server):
    yt_client = yt_server.get_yt_client()
    states_dir = ypath_join(MBOC_OFFERS_DIR, 'states')
    tables = yt_client.list(states_dir)

    assert tables == [NEW_GENERATION_2, 'recent']
    assert 'recent' in tables
    assert yt_client.get(ypath_join(states_dir, 'recent', '@path')).split('/')[-1] == NEW_GENERATION_2


def test_whole_cycle(internal_test_0, internal_test_1, internal_test_2):
    pass
