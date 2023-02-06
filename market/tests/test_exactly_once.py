# coding: utf-8
import pytest
from hamcrest import assert_that, equal_to, has_items, has_entries
from mock import patch

from market.pylibrary.logbroker.exactly_once.exactly_once_reader import LogbrokerExactlyOnceReader
from market.pylibrary.logbroker.exactly_once.reader_with_locks import LbkReaderWithLockConfig, MultiDCLogbrokerReaderWithLock, LogbrokerReaderWithLock
from market.pylibrary.thread_pool.bounded_thread_pool import BoundedThreadPoolExecutor
from market.idx.yatf.resources.lbk_topic import LbkTopic
from utils import DataKeeper, DataKeeperMultiDc, wait_until, make_expected


LOCKDIR = '//logbroker/test_exactly_once/experiment_number_74836'

WORKERS_DIR = '{}/workers'.format(LOCKDIR)
OFFSET_TABLEPATH = '{}/offsets'.format(LOCKDIR)

PARTITIONS_COUNT = 2


@pytest.yield_fixture(scope='session')
def yt_client(yt_server):
    return yt_server.get_yt_client()


@pytest.fixture(scope='module')
def topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff, partitions_count=PARTITIONS_COUNT)
    topic.create()
    return topic


@pytest.fixture(scope='module')
def data_keeper_1():
    return DataKeeper()


@pytest.fixture(scope='module')
def data_keeper_2():
    return DataKeeper()


@pytest.fixture(scope='module')
def data_keeper_with_tvm():
    return DataKeeperMultiDc()


@pytest.fixture(scope='module')
def reader_config_1(log_broker_stuff, topic, data_keeper_1):
    return LbkReaderWithLockConfig(
        name='Reader1',
        host=log_broker_stuff.host,
        port=log_broker_stuff.port,
        topic=topic.topic,
        client_id='Reader1',
        on_data_callback=data_keeper_1.store,
    )


@pytest.fixture(scope='module')
def reader_config_2(log_broker_stuff, topic, data_keeper_2):
    return LbkReaderWithLockConfig(
        name='Reader2',
        host=log_broker_stuff.host,
        port=log_broker_stuff.port,
        topic=topic.topic,
        client_id='Reader1',
        on_data_callback=data_keeper_2.store,
    )


@pytest.fixture(scope='module')
def reader_config_with_tvm(log_broker_stuff, topic, data_keeper_with_tvm):
    return LbkReaderWithLockConfig(
        name='ReaderWithTvm',
        host=log_broker_stuff.host,
        port=log_broker_stuff.port,
        topic=topic.topic,
        client_id='ReaderWithTvm',
        tvm_client_id=111111,
        tvm_secret='secret',
        on_data_callback=data_keeper_with_tvm.store,
    )


@pytest.fixture(scope='module')
def thread_pool():
    return BoundedThreadPoolExecutor(bound=10, max_workers=5)


@pytest.fixture(scope='module')
def reader_1(reader_config_1, yt_client, thread_pool):
    reader = LogbrokerExactlyOnceReader(
        yt_proxy=yt_client.config["proxy"]["url"],
        yt_token=yt_client.config["token"],
        yt_lockdir=LOCKDIR,
        partitions_count=PARTITIONS_COUNT,
        reader_config=reader_config_1,
        thread_pool=thread_pool,
        timeout=0.1,
    )

    return reader


@pytest.fixture(scope='module')
def reader_2(reader_config_2, yt_client, thread_pool):
    reader = LogbrokerExactlyOnceReader(
        yt_proxy=yt_client.config["proxy"]["url"],
        yt_token=yt_client.config["token"],
        yt_lockdir=LOCKDIR,
        partitions_count=PARTITIONS_COUNT,
        reader_config=reader_config_2,
        thread_pool=thread_pool,
        timeout=0.1,
    )

    return reader


def mocked_cred_provider():
    notlocal = {'cnt': 0}
    def impl(v1, v2):
        notlocal['cnt'] += 1
        if notlocal['cnt'] == 2:
            raise RuntimeError('First time we raise')
        else:
            return None
    return impl


def mocked_create_reader():
    notlocal = {'cnt': 0}
    def impl(self):
        notlocal['cnt'] += 1
        if notlocal['cnt'] == 1:
            raise RuntimeError('{} time we raise'.format(notlocal['cnt']))
        else:
            self.lbk_reader = self.lbk_client.create_reader(
                topic=self.config.topic,
                client_id=self.config.client_id,
                max_count=self.config.max_count,
                partition_groups=[self.partition] if self.partition else None,
                read_only_local=True,
                use_client_locks=True,
                read_infly_count=1,
                partitions_at_once=self.config.read_infly_count
            )
    return impl


def mocked_PQStreamingAPI_start():
    notlocal = {'cnt': 0}
    def impl(self):
        notlocal['cnt'] += 1
        if notlocal['cnt'] == 2:
            self._PQStreamingAPI__started = True
            raise RuntimeError('{} time we raise'.format(notlocal['cnt']))
        else:
            if self._PQStreamingAPI__started:
                raise RuntimeError("start() must be called exactly once!")
            self._PQStreamingAPI__api_start_future.set_running_or_notify_cancel()
            self._PQStreamingAPI__backend_reactor.start()
            self._PQStreamingAPI__started = True
            self._PQStreamingAPI__watcher.start()
            return self._PQStreamingAPI__api_start_future
    return impl


@pytest.fixture(scope='module')
def reader_with_tvm(reader_config_with_tvm, yt_client, thread_pool):
    """
    Логика:
    1) Инициализируем логброкер
    2) Роняем создание ридера
    3) Уходим на повторную инициализацию логброкера и в этот момент роняем получение tvm
    """
    with patch('market.pylibrary.logbroker.logbroker.get_credentials_provider', new_callable=mocked_cred_provider):
        with patch.object(LogbrokerReaderWithLock, '_create_reader', new_callable=mocked_create_reader):
            reader = MultiDCLogbrokerReaderWithLock(
                reader_config=reader_config_with_tvm,
                thread_pool=thread_pool,
            )

            reader.start()
            yield reader
            reader.stop()


@pytest.yield_fixture(scope='module')
def readers(reader_1, reader_2):
    # стартуем читателей вместе, после того, как они зарегестрировались,
    # чтобы клиент логброкера не посылал лишний раз лок на партицию, так как это может
    # сильно замедлить получение следующего лока другим инстансом
    with reader_1, reader_2:
        wait_until(lambda: reader_1.is_balanced())
        wait_until(lambda: reader_2.is_balanced())
        yield


@pytest.fixture(scope='module')
def writer(readers, reader_1, reader_2, topic, lbk_client):
    data_to_partition_1 = ['message1']
    data_to_partition_2 = ['message2', 'message3']

    lbk_client.simple_write(
        data=data_to_partition_1,
        topic=topic.topic,
        source_id='LbkTestWriterTo1',
        partition_group=1
    )

    lbk_client.simple_write(
        data=data_to_partition_2,
        topic=topic.topic,
        source_id='LbkTestWriterTo2',
        partition_group=2
    )

    message_count = 3
    wait_until(lambda: reader_1.message_data_processed + reader_2.message_data_processed >= message_count)


@pytest.fixture(scope='module')
def writer_with_tvm(reader_with_tvm, topic, lbk_client):
    lbk_client.simple_write(
        data=['message'],
        topic=topic.topic,
        source_id='LbkTestWriterToTvmReader'
    )

    message_count = 1
    wait_until(lambda: reader_with_tvm.message_data_processed >= message_count)


def test_register_instances(readers, yt_client):
    """Проверяем, что читатели поднялись и зарегистрировались в ыте"""
    locks = yt_client.get('{}/@locks'.format(WORKERS_DIR))
    assert_that(len(locks), equal_to(2))


def test_lock_partitions(readers, yt_client):
    """Проверяем, что читатели залочили все партиции"""
    locks = yt_client.get('{}/@locks'.format(OFFSET_TABLEPATH))
    assert_that(len(locks), equal_to(PARTITIONS_COUNT))


def test_read(writer, data_keeper_1, data_keeper_2):
    """Проверяем, что после записи читатели прочитали все данные"""
    data = data_keeper_1.data + data_keeper_2.data

    assert_that(sorted(data), equal_to(make_expected(['message1', 'message2', 'message3'])))


def test_check_offset_table(writer, data_keeper_1, data_keeper_2, yt_client):
    """Проверяем, что после чтения записанных данных, клиенты закомитили оффсеты в таблицу оффсетов"""
    rows = list(yt_client.select_rows('* from [{path}] where partition in (1, 2)'.format(path=OFFSET_TABLEPATH)))
    assert_that(rows, has_items(*[
        has_entries({
            'partition': 1,
            'offset': 0,
        }),
        has_entries({
            'partition': 2,
            'offset': 1,
        }),
    ]))


def test_reinit_after_tvm_fail(writer_with_tvm, data_keeper_with_tvm):
    """Проверяем, что после ошибки получения tvm_id мы не уходим в бесконечный цикл ретраев"""
    assert_that(data_keeper_with_tvm.data, equal_to(make_expected(['message'])))
