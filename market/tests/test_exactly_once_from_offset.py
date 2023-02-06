# coding: utf-8
import pytest
from hamcrest import assert_that, equal_to, has_items, has_entries

from market.pylibrary.logbroker.exactly_once.exactly_once_reader import LogbrokerExactlyOnceReader
from market.pylibrary.logbroker.exactly_once.reader_with_locks import LbkReaderWithLockConfig
from market.pylibrary.logbroker.exactly_once.offset_table import PartitionOffsetTable
from market.pylibrary.thread_pool.bounded_thread_pool import BoundedThreadPoolExecutor
from market.idx.yatf.resources.lbk_topic import LbkTopic
from utils import DataKeeper, wait_until, make_expected

from market.idx.yatf.resources.yt_table_resource import YtDynTableResource


LOCKDIR = '//logbroker/test_exactly_once/experiment_number_96729'

WORKERS_DIR = '{}/workers'.format(LOCKDIR)
OFFSET_TABLEPATH = '{}/offsets'.format(LOCKDIR)

COMMITED_OFFSET = 5
TOTAL_MESSAGES = 11

PARTITIONS_COUNT = 1


@pytest.yield_fixture(scope='session')
def yt_client(yt_server):
    return yt_server.get_yt_client()


@pytest.fixture(scope='module')
def topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff, partitions_count=PARTITIONS_COUNT)
    topic.create()
    return topic


@pytest.fixture(scope='module')
def offsets_tale(yt_server, topic):
    OFFSETS_TABLE = [{
        'partition': 1,
        'topic': 'rt3.dc1--{}'.format(topic.topic),  # реальное имя топика в "локальном" ДЦ
        'offset': COMMITED_OFFSET,  # предпологаем, что все сообщения, до 5 оффсета включительно мы уже обработали
    }]

    table = YtDynTableResource(
        yt_stuff=yt_server,
        path=OFFSET_TABLEPATH,
        data=OFFSETS_TABLE,
        attributes=PartitionOffsetTable.ATTRIBUTES
    )

    table.create()
    return table


@pytest.fixture(scope='module')
def data_keeper():
    return DataKeeper()


@pytest.fixture(scope='module')
def reader_config(log_broker_stuff, topic, data_keeper):
    return LbkReaderWithLockConfig(
        name='Reader1',
        host=log_broker_stuff.host,
        port=log_broker_stuff.port,
        topic=topic.topic,
        client_id='Reader1',
        on_data_callback=data_keeper.store,
    )


@pytest.fixture(scope='module')
def thread_pool():
    return BoundedThreadPoolExecutor(bound=10, max_workers=5)


@pytest.yield_fixture(scope='module')
def reader(reader_config, offsets_tale, yt_client, thread_pool):
    reader = LogbrokerExactlyOnceReader(
        yt_proxy=yt_client.config["proxy"]["url"],
        yt_token=yt_client.config["token"],
        yt_lockdir=LOCKDIR,
        partitions_count=PARTITIONS_COUNT,
        reader_config=reader_config,
        thread_pool=thread_pool,
        timeout=0.1,
    )

    with reader:
        wait_until(lambda: reader.is_balanced())
        yield reader


@pytest.fixture(scope='module')
def writer(reader, topic, lbk_client):
    data_to_partition = ['message' + str(i) for i in range(TOTAL_MESSAGES)]

    lbk_client.simple_write(
        data=data_to_partition,
        topic=topic.topic,
        source_id='LbkTestWriter',
    )

    message_count = TOTAL_MESSAGES - (COMMITED_OFFSET + 1)  # 6 сообщений мы уже обработали, согласно таблице оффсетов, осталось обработать еще 5
    wait_until(lambda: reader.message_data_processed >= message_count)


def test_register_instances(reader, yt_client):
    """Проверяем, что читатель поднялся и зарегистрировался в ыте"""
    locks = yt_client.get('{}/@locks'.format(WORKERS_DIR))
    assert_that(len(locks), equal_to(1))


def test_lock_partitions(reader, yt_client):
    """Проверяем, что читатель залочил все партиции"""
    locks = yt_client.get('{}/@locks'.format(OFFSET_TABLEPATH))
    assert_that(len(locks), equal_to(PARTITIONS_COUNT))


def test_read(writer, data_keeper):
    """Проверяем, что после записи читатель прочитал все данные"""
    data = data_keeper.data
    expected = sorted(make_expected([
        'message' + str(i) for i in range(COMMITED_OFFSET + 1, TOTAL_MESSAGES)
    ]))
    assert_that(sorted(data), equal_to(expected))


def test_check_offset_table(writer, data_keeper, yt_client):
    """Проверяем, что после чтения записанных данных, клиенты закомитили оффсеты в таблицу оффсетов"""
    rows = list(yt_client.select_rows('* from [{path}] where partition in (1)'.format(path=OFFSET_TABLEPATH)))
    assert_that(rows, has_items(*[
        has_entries({
            'partition': 1,
            'offset': TOTAL_MESSAGES - 1,
        }),
    ]))
