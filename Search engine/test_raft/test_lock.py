# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import socket
import threading
import time


from pysyncobj import SyncObj

from search.martylib.core.exceptions import Locked
from search.martylib.test_utils import TestCase
from search.martylib.raft import MasterLock


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


class TestLock(TestCase):

    def setUp(self):
        self.lock1 = MasterLock(unlock_time=2.01)
        self.lock2 = MasterLock(unlock_time=2.02)
        self.lock3 = MasterLock(unlock_time=2.03)  # init singleton

        self.assertIsNot(self.lock1, self.lock2)

        addrs = ['{}:{}'.format(socket.getfqdn(), port) for port in find_free_ports(3)]

        self.so1 = SyncObj(addrs[0], [addrs[1], addrs[2]], consumers=[self.lock1])
        self.so2 = SyncObj(addrs[1], [addrs[0], addrs[2]], consumers=[self.lock2])
        self.so3 = SyncObj(addrs[2], [addrs[0], addrs[1]], consumers=[self.lock3])
        self.so1.waitBinded()
        self.so2.waitBinded()
        self.so3.waitBinded()
        time.sleep(1)

    def test_base_lock(self):
        self.assertFalse(self.lock2.locked)
        with self.lock2:
            self.assertTrue(self.lock2.locked)
            self.assertFalse(self.lock1.lock.acquire(self.lock1.name, self.lock1.id, time.time()))
        self.assertFalse(self.lock2.locked)

    def test_inner_locks(self):
        with self.lock2:
            with self.assertRaises(Locked):
                with self.lock1:
                    pass

    def test_locks(self):
        thread = threading.Thread(target=_acquire_lock, args=(self.lock2,))
        thread.start()

        time.sleep(0.5)

        with self.assertRaises(Locked):
            with self.lock3:
                pass

        thread.join()
        with self.lock2:
            self.assertTrue(self.lock2.locked)

    def test_dead_master(self):
        thread = threading.Thread(target=_acquire_lock, args=(self.lock2, 10))
        thread.start()

        time.sleep(0.5)
        self.assertTrue(self.lock2.locked)

        self.so1.destroy()
        self.so3.destroy()

        time.sleep(2)
        self.assertFalse(self.lock2.locked)
