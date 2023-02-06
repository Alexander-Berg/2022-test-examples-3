# -*- coding: utf-8 -*-

import os
import time

from market.idx.yatf.test_envs.pqtool_env import PqToolWriterEnv, PqToolReaderEnv


# ================== helpers ==================
def make_topic_name(tag):
    return 'test_pqtool_{}_{}_{}'.format(tag, time.time(), os.getuid())


def do_simple_write(log_broker_stuff, topic, data, partition=1):
    with PqToolWriterEnv(host=log_broker_stuff.host,
                         port=log_broker_stuff.port,
                         topic=topic,
                         partition=partition,
                         data=data):
        pass


def do_simple_read(log_broker_stuff, topic, read_count, partition=1, no_commit=False):
    with PqToolReaderEnv(host=log_broker_stuff.host,
                         port=log_broker_stuff.port,
                         topic=topic,
                         partition=partition,
                         read_count=read_count,
                         no_commit=no_commit) as reader:
        reader.wait()
        return reader.results


def simple_write_read_test(log_broker_stuff, topic, data, partition=1):
    do_simple_write(log_broker_stuff, topic, data=data, partition=partition)
    results = do_simple_read(log_broker_stuff, topic, read_count=len(data), partition=partition)
    assert results == data


# ================== tests ==================
def test_pqcpp_trivial(log_broker_stuff):
    # Что проверяем: обычную запись и чтение пары сообщений из топика
    topic = make_topic_name('trivial')
    log_broker_stuff.create_topic(topic)
    data = ['hello', 'world']
    simple_write_read_test(log_broker_stuff, topic, data)


def test_pqcpp_many_messages(log_broker_stuff):
    # Что проверяем: обычную запись и чтение многих сообщений из топика
    # Это важно, потому что в этом случае writer будет упираться в
    # лимит одновременно активных future и ждать
    topic = make_topic_name('many')
    log_broker_stuff.create_topic(topic)
    data = ['data{}'.format(n) for n in xrange(50)]
    simple_write_read_test(log_broker_stuff, topic, data)


def test_pqcpp_by_turns_simple(log_broker_stuff):
    # Что проверяем: запись и чтение из топика по очереди. Должно сохраняться seqno
    # А также убеждаемся что работает commit
    topic = make_topic_name('by_turns_simple')
    log_broker_stuff.create_topic(topic)

    batch_size = 10
    batches_count = 3

    for i in xrange(batches_count):
        data = ['data{}'.format(n) for n in range(batch_size * i, batch_size * (i + 1))]
        simple_write_read_test(log_broker_stuff, topic, data)


def test_pqcpp_by_turns_nested(log_broker_stuff):
    # Что проверяем: запись в топик несколькими батчами с паузами при запущенном постоянно reader
    topic = make_topic_name('by_turns_nested')
    log_broker_stuff.create_topic(topic)

    batch_size = 10
    batches_count = 3
    data = ['data{}'.format(n) for n in xrange(batch_size * batches_count)]

    with PqToolReaderEnv(host=log_broker_stuff.host,
                         port=log_broker_stuff.port,
                         topic=topic,
                         partition=1,
                         read_count=len(data)) as reader:
        for i in xrange(batches_count):
            local_data = data[batch_size * i:batch_size * (i + 1)]
            do_simple_write(log_broker_stuff, topic, data=local_data)
            time.sleep(0.5)

        reader.wait()
        assert reader.results == data


def test_no_commit(log_broker_stuff):
    # Что проверяем: повторное чтение одних и тех же данных, если не было коммита.
    topic = make_topic_name('no_commit')
    log_broker_stuff.create_topic(topic)

    data = ['hello', 'world']
    do_simple_write(log_broker_stuff, topic, data=data)

    res_data = []
    for x in xrange(2):
        results = do_simple_read(log_broker_stuff, topic, read_count=len(data), no_commit=True)
        res_data.append(results)

    assert res_data == [data, data]


def test_many_partitions(log_broker_stuff):
    # Что проверяем: независимую работу с разными партициями
    # Сначала записть в разные партиции, потом прочитать
    topic = make_topic_name('many_partitions')
    partitions = 3
    batch_size = 10

    log_broker_stuff.create_topic(topic, partitions=partitions)

    src_data = [
        ['part{}_data{}'.format(part+1, n) for n in xrange(batch_size)]
        for part in xrange(partitions)
    ]

    for p, part_data in enumerate(src_data):
        do_simple_write(log_broker_stuff, topic, data=part_data, partition=p + 1)

    res_data = []
    for p in xrange(partitions):
        results = do_simple_read(log_broker_stuff, topic, read_count=batch_size, partition=p + 1)
        res_data.append(results)

    assert res_data == src_data
