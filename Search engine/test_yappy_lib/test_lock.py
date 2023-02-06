# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import six

if six.PY2:
    import mock
else:
    import unittest.mock as mock

from search.martylib.core.exceptions import Locked

from search.priemka.yappy.src.yappy_lib.lock import MasterLock

from search.priemka.yappy.tests.utils.test_cases import TestCase


class MasterLockTestCase(TestCase):

    class DummyException(Exception):
        """ Dummy exception to be used in tests as "some exception" and not mask real ones """

    @classmethod
    def setUpClass(cls):
        cls.lock = MasterLock()
        cls.raft_lock_patch = mock.patch.object(cls.lock, 'lock')
        cls.local_lock_patch = mock.patch.object(cls.lock, 'local_lock')

    def setUp(self):
        self.raft_lock = self.raft_lock_patch.start()
        self.local_lock = self.local_lock_patch.start()
        self.local_lock.acquire.return_value = True
        self.addCleanup(self.raft_lock_patch.stop)
        self.addCleanup(self.local_lock_patch.stop)

    def test_raft_locked(self):
        self.raft_lock.acquire.side_effect = Locked()
        self.assertRaises(
            Locked,
            self.lock.__enter__,
        )

    def test_local_locked(self):
        self.local_lock.acquire.return_value = False
        self.assertRaises(
            Locked,
            self.lock.__enter__,
        )

    def test_success(self):
        try:
            with self.lock:
                pass
        except Locked:
            self.failureException('unexpected `Locked` exception raised')

    def test_local_released_on_success(self):
        with self.lock:
            pass
        self.local_lock.release.assert_called_once()

    def test_local_released_on_error(self):
        try:
            with self.lock:
                raise self.DummyException
        except self.DummyException:
            pass
        self.local_lock.release.assert_called_once()

    def test_local_released_on_locked_raft(self):
        self.raft_lock.acquire.side_effect = self.DummyException()
        try:
            with self.lock:
                raise self.DummyException
        except self.DummyException:
            pass
        self.local_lock.release.assert_called_once()

    def test_local_released_on_raft_relaese_error(self):
        self.raft_lock.release.side_effect = self.DummyException()
        try:
            with self.lock:
                raise self.DummyException
        except self.DummyException:
            pass
        self.local_lock.release.assert_called_once()
