# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals


import socket
import threading
import time

from pysyncobj import SyncObj

from search.martylib.test_utils import TestCase
from search.martylib.raft import KVStorage


def find_free_ports(count=3):
    sockets = []
    try:
        for _ in range(count):
            sockets.append(socket.socket())
            sockets[-1].bind(('', 0))
        return [sock.getsockname()[1] for sock in sockets]
    finally:
        for sock in sockets:
            sock.close()


class TestContainers(TestCase):

    def setUp(self):
        self.store1 = KVStorage()
        self.store2 = KVStorage()
        self.store3 = KVStorage()

        addrs = ['{}:{}'.format(socket.getfqdn(), port) for port in find_free_ports(3)]

        self.so1 = SyncObj(addrs[0], [addrs[1], addrs[2]], consumers=[self.store1])
        self.so2 = SyncObj(addrs[1], [addrs[0], addrs[2]], consumers=[self.store2])
        self.so3 = SyncObj(addrs[2], [addrs[0], addrs[1]], consumers=[self.store3])

        self.so1.waitBinded()
        self.so2.waitBinded()
        self.so3.waitBinded()

    def test_kv_storage(self):
        self.store1['foo'] = 'not-bar'
        self.store2['foo'] = 'bar'
        self.assertEqual(self.store1['foo'], 'bar')
        self.assertEqual(self.store2['foo'], 'bar')
        self.assertEqual(self.store3['foo'], 'bar')

    def test_kv_storage_threaded(self):

        def _test():
            self.store1['hello'] = 'there'
            time.sleep(1)

        thread = threading.Thread(target=_test)
        thread.start()
        thread.join()

        self.assertEqual(self.store1['hello'], 'there')
        self.assertEqual(self.store2['hello'], 'there')
        self.assertEqual(self.store3['hello'], 'there')
