# coding: utf-8

import socket
import time
import mock

from pysyncobj import SyncObj

from search.martylib.core.exceptions import Locked
from search.martylib.test_utils import TestCase
from search.martylib.raft import LockQueue


def _acquire_lock(lock, timeout=1):
    with lock:
        time.sleep(timeout)


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


class TestQueue(TestCase):

    def setUp(self):
        self.queue1 = LockQueue(unlock_time=2.01)
        self.queue2 = LockQueue(unlock_time=2.02)
        self.queue3 = LockQueue(unlock_time=2.03)  # init singleton

        self.assertIsNot(self.queue1, self.queue2)

        addrs = ['{}:{}'.format(socket.getfqdn(), port) for port in find_free_ports(3)]

        self.so1 = SyncObj(addrs[0], [addrs[1], addrs[2]], consumers=[self.queue1.consumer])
        self.so2 = SyncObj(addrs[1], [addrs[0], addrs[2]], consumers=[self.queue2.consumer])
        self.so3 = SyncObj(addrs[2], [addrs[0], addrs[1]], consumers=[self.queue3.consumer])
        self.so1.waitBinded()
        self.so2.waitBinded()
        self.so3.waitBinded()
        time.sleep(1)

    def test_rotate(self):
        self.queue1.update(list(range(10)))

        with self.queue2.rotate(count=3) as to_process:
            self.assertEqual(to_process.objects, [0, 1, 2])

        with self.queue1.rotate(count=3) as to_process:
            self.assertEqual(to_process.objects, [3, 4, 5])

        with self.queue1.rotate(count=5) as to_process:
            self.assertEqual(to_process.objects, [6, 7, 8, 9, 0])

    def test_lock_rotate(self):
        self.queue1.update(list(range(10)))

        with self.queue1.rotate(count=10) as to_process:
            with self.assertRaises(Locked):
                with self.queue2.rotate(count=1):
                    pass

            self.assertEqual(to_process.objects, list(range(10)))

    @mock.patch('{}._LockQueueConsumer._is_expire'.format(LockQueue.__module__), return_value=True)
    def test_expired_during_rotation(self, *mocked):
        """ May happen if items unlocked by timeout """
        items = list(range(3))
        self.queue1.update(list(items))

        with self.queue1.rotate(count=1):
            try:
                with self.queue2.rotate(0):  # will expire locks taken by `queue1` due to mocked `_is_expire`
                    pass
            except Locked:
                pass

        self.assertEqual(list(self.queue1.consumer.queue), list(items))
