# coding: utf8

from __future__ import absolute_import, division, print_function, unicode_literals

import pytest
from hamcrest import assert_that, has_entries

from travel.rasp.library.python.sqs.queue import FIFOQueue, Queue


class ClientMock(object):
    def __init__(self):
        self.messages = []

    def send_message(self,  **kwargs):
        self.messages.append(kwargs)

    def create_queue(self, **kwargs):
        return {'QueueUrl': 'test_queue_url'}


def test_push_message_to_queue():
    client = ClientMock()

    queue = Queue(client, 'test_queue')
    queue.push('123')

    assert len(client.messages) == 1

    message = client.messages[0]
    assert_that(message, has_entries({
        'MessageBody': '123',
        'QueueUrl': 'test_queue_url'
    }))


def test_push_message_to_fifo_queue():
    client = ClientMock()

    queue = FIFOQueue(client, 'test_queue')
    queue.push('123')
    queue.push('234', message_deduplication_id='d_id', message_group_id='g_id')

    assert len(client.messages) == 2

    message = client.messages[0]
    assert_that(message, has_entries({
        'MessageBody': '123',
        'MessageDeduplicationId':  str('123'.__hash__()),
        'MessageGroupId': 'travel.rasp.library.python.sqs.queue',
        'QueueUrl': 'test_queue_url'
    }))

    message = client.messages[1]
    assert_that(message, has_entries({
        'MessageBody': '234',
        'MessageDeduplicationId': 'd_id',
        'MessageGroupId': 'g_id',
        'QueueUrl': 'test_queue_url'
    }))


@pytest.mark.parametrize('queue_name, expected_name', [
    ('test_queue', 'test_queue.fifo'),
    ('test_queue.fifo', 'test_queue.fifo')
])
def test_fifo_queue(queue_name, expected_name):
    queue = FIFOQueue(ClientMock(), queue_name=queue_name)

    assert queue.queue_name == expected_name
    assert_that(queue.attributes, has_entries({
        'FifoQueue': 'true'
    }))


@pytest.mark.parametrize('queue_name', [
    '', None, {'test': 42}
])
def test_invalid_queue_name(queue_name):
    with pytest.raises(ValueError):
        Queue(ClientMock(), queue_name)
