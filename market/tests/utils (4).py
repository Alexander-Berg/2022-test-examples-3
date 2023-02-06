# coding: utf-8

import six
import threading
import time


class DataKeeper(object):
    def __init__(self):
        self.lock = threading.Lock()
        self.data = []

    def store(self, offsets, response):
        count = 0
        for batch in response.message.data.message_batch:
            for message in batch.message:
                if message.offset > offsets[batch.topic]:
                    with self.lock:
                        self.data.append(message.data)
                    count += 1
        return count


class DataKeeperMultiDc(object):
    def __init__(self):
        self.lock = threading.Lock()
        self.data = []

    def store(self, response):
        count = 0
        for batch in response.message.data.message_batch:
            for message in batch.message:
                with self.lock:
                    self.data.append(message.data)
                count += 1
        return count


def wait_until(condition, timeout=60, step=0.1):
    start = time.time()
    while time.time() - start < timeout:
        if condition():
            return
        time.sleep(step)

    raise RuntimeError('timeout while waiting condition')


def make_expected(data_list):
    return [
        six.ensure_binary(data)
        for data in data_list
    ]
