# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import time

from search.martylib.collections import TempStorage, ProtectedTempStorage
from search.martylib.test_utils import TestCase


class TestTempStorage(TestCase):

    def test_ttl(self):
        s = TempStorage(ttl=1)
        self.assertEqual(s._ttl, 1)
        s.set('foo', 'bar')
        time.sleep(2)
        self.assertIsNone(s.get('foo'))

    def test_base(self):
        s = TempStorage(ttl=1)
        s.set('foo', 'bar')
        s.set('hello', 'world')
        self.assertEqual(s.get('foo'), 'bar')
        self.assertEqual(s.get('hello'), 'world')

    def test_protected(self):
        s = ProtectedTempStorage(ttl=60)
        s.set('foo', 'bar')
        self.assertNotIn('foo', s._storage)
        self.assertEqual(s.get('foo'), 'bar')
