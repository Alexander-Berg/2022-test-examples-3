import boto3
import logging
from os import environ
from time import time
from random import random
import base64

boto3.set_stream_logger('boto3')


class SQSMessage(object):
    def __init__(self, queue, data, max_receive_count):
        self.queue = queue
        self.raw = data
        self.max_receive_count = max_receive_count
        if not self.empty() and len(self.raw['Messages']) != 1:
            for i, item in enumerate(self.raw['Messages']):
                logging.warning('#{} multi message ID {}'.format(i, item['ReceiptHandle']))

    def _get_attribute(self, attr, default=None):
        if self.empty():
            return None
        return self.raw['Messages'][0]['Attributes'].get(attr, default)

    def receive_count_exceeded(self):
        if self.max_receive_count is None:
            return False
        recv_cnt = int(self._get_attribute('ApproximateReceiveCount', 0))
        if recv_cnt > self.max_receive_count:
            logging.warning('receive_count_exceeded: {} > {}'.format(recv_cnt, self.max_receive_count))
            return True
        else:
            return False

    def empty(self):
        return 'Messages' not in self.raw

    def done(self):
        self.queue.done(self)

    def id(self):
        return self.raw['Messages'][0]['ReceiptHandle']

    def change_visibility(self, timeout):
        return self.queue.change_visibility(self.id(), timeout)

    def payload(self):
        return base64.b64decode(self.raw['Messages'][0]['Body'])

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_val, backtrace):
        if exc_type is None:
            self.done()


class SQSQueue(object):
    def __init__(self, name, client, queue, max_receive_count):
        self.name = name
        self.client = client
        self.queue = queue
        self.max_receive_count = max_receive_count
        self.attrs = ['ApproximateReceiveCount'] if max_receive_count else []

    def pop(self, wait_timeout=20, visibility_timeout=None):
        params = {
            'MaxNumberOfMessages': 1,
            'WaitTimeSeconds': wait_timeout
        }
        if self.max_receive_count:
            params['AttributeNames'] = ['ApproximateReceiveCount']
        if visibility_timeout:
            params['VisibilityTimeout'] = visibility_timeout
        message = SQSMessage(self,
                             self.client.receive_message(QueueUrl=self.queue['QueueUrl'], **params),
                             self.max_receive_count)
        if not message.empty():
            logging.info('got [sqs:{}] message {}'.format(self.name, message.id()))
        return message

    def done(self, message):
        if not message.empty():
            logging.info('deleting [sqs:{}] message {}'.format(self.name, message.id()))
            return self.client.delete_message(QueueUrl=self.queue['QueueUrl'], ReceiptHandle=message.id())

    def change_visibility(self, handle, timeout):
        return self.client.change_message_visibility(
            QueueUrl=self.queue['QueueUrl'],
            ReceiptHandle=handle,
            VisibilityTimeout=timeout
        )

    def push(self, payload):
        result = self.client.send_message(QueueUrl=self.queue['QueueUrl'], MessageBody=base64.b64encode(payload))
        logging.info('sent [sqs:{}] message length {}'.format(self.name, len(payload)))
        return result

    def size(self):
        result = self.client.get_queue_attributes(QueueUrl=self.queue['QueueUrl'], AttributeNames=['ApproximateNumberOfMessages'])
        return int(result['Attributes'].get('ApproximateNumberOfMessages', 0))


class SQSClient(object):
    def __init__(self, config):
        self.session = boto3.session.Session()
        self.client = self.session.client('sqs',
                                          region_name='yandex',
                                          endpoint_url=config.endpoint_url,
                                          aws_access_key_id=config.account,
                                          aws_secret_access_key=environ.get('AWS_SECRET_KEY', 'unused'),
                                          aws_session_token=environ.get('AWS_SESSION_TOKEN'))
        self.visibility_timeout = config.visibility_timeout
        self.max_receive_count = config.max_receive_count

    def get_queue(self, name, visibility_timeout=180):
        queue = None
        try:
            queue = self.client.get_queue_url(QueueName=name)
            logging.info('using queue [sqs:%s]', name)
        except:
            pass
        if queue is None:
            queue = self.client.create_queue(QueueName=name, Attributes={'VisibilityTimeout': str(self.visibility_timeout)})
            logging.info('created queue [sqs:%s]', name)
        return SQSQueue(name, self.client, queue, self.max_receive_count)

    def drop_queue(self, name):
        self.client.delete_queue(QueueUrl=self.client.get_queue_url(QueueName=name)['QueueUrl'])
        logging.info('queue {} has been deleted'.format(name))


class SQSQueueBalancer(object):
    def __init__(self, client, config, refresh_time=180, visibility_timeout=180):
        self.ts = 0
        self.config = config
        self.queues = {}
        self.weights = []
        self.refresh_time = refresh_time
        for name in self.config.iterkeys():
            self.queues[name] = client.get_queue(name, visibility_timeout=visibility_timeout)

    def _weight(self):
        now = time()
        if self.ts + self.refresh_time > now:
            return
        logging.info('it is time to weight queues')
        self.weights = []
        total = 0
        for name, weight in self.config.iteritems():
            sz = self.queues[name].size()
            if sz > 0:
                logging.info('queue [sqs:{}] size {}'.format(name, sz))
                self.weights.append([float(weight), name])
                total += weight
        for w in self.weights:
            w[0] /= total
            logging.info('queue [sqs:{}] weight {:2f}'.format(w[1], w[0]))
        self.ts = now

    def get_queue(self):
        self._weight()
        rnd = random()
        total = 0.0
        for w, name in self.weights:
            total += w
            if rnd < total:
                return self.queues[name]
        return self.queues.values()[0]
