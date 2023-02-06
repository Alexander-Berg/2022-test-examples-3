# coding: utf8

from __future__ import absolute_import, division, print_function, unicode_literals

import json

import pytest
from hamcrest import assert_that, contains, contains_inanyorder

from travel.rasp.library.python.sqs.queue import FIFOQueue, Queue
from travel.rasp.library.python.sqs.wrapper import SQSClientWrapper


SQS_USER = 'default_user'
SQS_QUEUE = 'rasp_queue'
TIMEOUT = 15


class TestPQApi(object):
    @classmethod
    def setup_class(cls):
        cls.sqs_port = cls.read_sqs_port()
        cls.sqs_user = SQS_USER
        cls.default_queue = SQS_QUEUE

        cls.sqs_client = SQSClientWrapper(
            aws_access_key_id=SQS_USER,
            aws_secret_access_key='unused',
            aws_session_token='',
            endpoint_url='http://localhost:{}'.format(cls.sqs_port),
            connect_timeout=1,
            read_timeout=15,
            retries=1,
            enable_writing=True
        )

    @classmethod
    def read_sqs_port(cls):
        try:
            with open('env.json.txt', 'r') as f:
                result = f.readlines()
                return json.loads(result[1])['SQS_PORT']
        except IOError:
            pytest.skip('SQS port is not found')

    def get_msg_deduplication_id(self, queue, index):
        return str(queue.__hash__()) + '.' + str(index)

    def setup_method(self, method):
        queues = self.sqs_client.list_queues()['QueueUrls']
        for queue in queues:
            self.sqs_client.purge_queue(QueueUrl=queue)

    def test_fifo_queue_order(self):
        queue = FIFOQueue(sqs_client=self.sqs_client, queue_name=SQS_QUEUE)

        for i in range(5):
            msg = 'm_{}'.format(i)
            queue.push(data=msg, message_deduplication_id=self.get_msg_deduplication_id(queue, i))

        messages = []
        for msg, token in queue.receive_messages(wait_time_seconds=TIMEOUT):
            messages.append(msg)
            queue.delete_message(token)

        assert_that(messages, contains('m_0', 'm_1', 'm_2', 'm_3', 'm_4'))

    def test_fifo_read_exact_count(self):
        queue = FIFOQueue(sqs_client=self.sqs_client, queue_name=SQS_QUEUE)

        for i in range(5):
            msg = 'm_{}'.format(i)
            queue.push(data=msg, message_deduplication_id=self.get_msg_deduplication_id(queue, i))

        messages = []
        for msg, token in queue.receive_messages(wait_time_seconds=TIMEOUT, max_messages=3):
            messages.append(msg)
            queue.delete_message(token)

        assert len(messages) == 3

        messages = []
        for msg, token in queue.receive_messages(wait_time_seconds=TIMEOUT, max_messages=4):
            messages.append(msg)
            queue.delete_message(token)

        assert len(messages) == 2

    def test_fifo_message_deduplication(self):
        queue = FIFOQueue(sqs_client=self.sqs_client, queue_name=SQS_QUEUE)

        base_msg = 'msg_{}'.format(queue.__hash__())
        push_messages = ['1', '1', '2', '2', '2', '7']

        for msg in push_messages:
            queue.push(base_msg + msg)

        messages = []
        for msg, token in queue.receive_messages(wait_time_seconds=TIMEOUT, max_messages=4):
            messages.append(msg)
            queue.delete_message(token)

        assert len(messages) == 3

    def test_read_visibility_timeout(self):
        queue = FIFOQueue(sqs_client=self.sqs_client, queue_name=SQS_QUEUE)

        for i in range(5):
            msg = 'm_{}'.format(i)
            queue.push(data=msg, message_deduplication_id=self.get_msg_deduplication_id(queue, i))

        message0 = message1 = message2 = None

        for msg, token in queue.receive_messages(wait_time_seconds=TIMEOUT, max_messages=1, visibility_timeout=0):
            message0 = msg

        for msg, token in queue.receive_messages(wait_time_seconds=TIMEOUT, max_messages=1, visibility_timeout=30):
            message1 = msg

        for msg, token in queue.receive_messages(wait_time_seconds=TIMEOUT, max_messages=1, visibility_timeout=0):
            message2 = msg

        assert message0 == message1
        assert message1 != message2

    def test_read_from_queue(self):
        queue = Queue(sqs_client=self.sqs_client, queue_name=SQS_QUEUE)

        for i in range(5):
            msg = 'm_{}'.format(i)
            queue.push(data=msg)

        messages = []
        while len(messages) < 5:
            for msg, token in queue.receive_messages(wait_time_seconds=TIMEOUT):
                messages.append(msg)
                queue.delete_message(token)

        assert len(messages) == 5

        assert_that(messages, contains_inanyorder('m_0', 'm_1', 'm_2', 'm_3', 'm_4'))
