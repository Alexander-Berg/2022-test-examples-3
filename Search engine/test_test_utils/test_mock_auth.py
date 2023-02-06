# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from search.martylib.test_utils import TestCase


class TestMockAuth(TestCase):

    @TestCase.mock_auth(login='foo')
    def test_mock_auth(self):
        self.assertEqual(self.storage.thread_local.auth_info.login, 'foo')

        with self.mock_auth(login='bar'):
            self.assertEqual(self.storage.thread_local.auth_info.login, 'bar')

            with self.mock_auth(login='baz'):
                self.assertEqual(self.storage.thread_local.auth_info.login, 'baz')

            self.assertEqual(self.storage.thread_local.auth_info.login, 'bar')

        self.assertEqual(self.storage.thread_local.auth_info.login, 'foo')
