# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from search.martylib.test_utils import TestCase

from search.martylib.mds import MdsClientMock


class TestMdsClient(TestCase):
    def test_client_singletons(self):
        c1 = MdsClientMock('key', 'secret', 'host')
        c2 = MdsClientMock('key', 'secret', 'host')
        c3 = MdsClientMock('key2', 'secret', 'host')
        c4 = MdsClientMock('key', 'secret2', 'host')
        c5 = MdsClientMock('key', 'secret', 'host2')

        self.assertIs(c1, c2)
        self.assertIsNot(c1, c3)
        self.assertIsNot(c1, c4)
        self.assertIsNot(c1, c5)

    def test_bucket_singletons(self):
        c1 = MdsClientMock('key', 'secret', 'stable-host')
        c2 = MdsClientMock('key', 'secret', 'testing-host')

        b1 = c1['1']
        b2 = c1['1']
        b3 = c2['1']

        self.assertIs(b1, b2)
        self.assertIsNot(b1, b3)
