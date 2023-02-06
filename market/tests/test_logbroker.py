# coding: utf-8

import threading
from hamcrest import assert_that, equal_to
from utils import make_expected


def test_write_read(log_broker_stuff, lbk_client):
    """
    Проверяем запись и чтения из логброкера
    """
    topic_name = 'test--topic-write-read'
    log_broker_stuff.create_topic(topic_name)

    data = ['testMessage1', 'test message 2', 'test_message_3']
    lbk_client.simple_write(
        data=data,
        topic=topic_name,
        source_id='LbkTestWriter',
    )

    requested_values = lbk_client.simple_read(
        topic_name,
        'LbkTestReader',
        count=len(data),
        do_commit=False
    )

    assert_that(
        requested_values,
        equal_to(make_expected(data)),
        'Requested values are different'
    )


def test_write_read_partition(log_broker_stuff, lbk_client):
    """
    Проверяем, запись в конкретную партицию и чтения из конкретной партиции
    """
    topic_name = 'test--topic-write-read-partition'
    log_broker_stuff.create_topic(topic_name, partitions=2)

    data_to_partition_1 = ['message1', 'message2', 'message3']
    data_to_partition_2 = ['message4', 'message5', 'message6', 'message7', 'message8']

    lbk_client.simple_write(
        data=data_to_partition_1,
        topic=topic_name,
        source_id='LbkTestWriterTo1',
        partition_group=1
    )

    lbk_client.simple_write(
        data=data_to_partition_2,
        topic=topic_name,
        source_id='LbkTestWriterTo2',
        partition_group=2
    )

    requested_values_from_part_1 = lbk_client.simple_read(
        topic=topic_name,
        client_id='LbkTestReaderFrom1',
        count=len(data_to_partition_1),
        partition_groups=[1],
        do_commit=False,
    )
    requested_values_from_part_2 = lbk_client.simple_read(
        topic=topic_name,
        client_id='LbkTestReaderFrom2',
        count=len(data_to_partition_2),
        partition_groups=[2],
        do_commit=False,
    )

    assert_that(
        requested_values_from_part_1,
        equal_to(make_expected(data_to_partition_1)),
        'Requested values from partition 1 are different'
    )
    assert_that(
        requested_values_from_part_2,
        equal_to(make_expected(data_to_partition_2)),
        'Requested values from partition 2 are different'
    )


def test_thread_safe_write(log_broker_stuff, lbk_client):
    """Проверяем, что запись из несколько потоков в один инстанс писателя работает корректно"""
    topic_name = 'test--topic-thread-safe_write'
    log_broker_stuff.create_topic(topic_name, partitions=2)

    writer = lbk_client.create_thread_safe_writer(
        topic=topic_name,
        source_id='LbkTestThreadSafeWriter'
    )

    threads_count = 10

    def write(index):
        for i in range(threads_count):
            writer.write(data='message' + str(index * threads_count + i))

    threads = []
    for i in range(threads_count):
        threads.append(threading.Thread(target=write, args=(i,)))

    for t in threads:
        t.start()
    for t in threads:
        t.join()

    data = lbk_client.simple_read(
        topic=topic_name,
        client_id='LbkTestThreadSafeReader',
        count=threads_count * threads_count,
        wait_timeout=60,
        do_commit=False
    )

    expected = set(make_expected([
        'message' + str(i) for i in range(threads_count * threads_count)
    ]))
    assert_that(set(data), equal_to(expected))
