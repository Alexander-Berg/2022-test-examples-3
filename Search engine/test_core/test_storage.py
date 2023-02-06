# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from search.martylib.auth import AuthInfo
from search.martylib.core.exceptions import NotAuthenticated
from search.martylib.grpc_utils.middleware.auth import AuthMiddlewareMock
from search.martylib.test_utils import GrpcContextMock, TestCase


class TestStorage(TestCase):
    def test_set_wrong_auth_credentials(self):
        with self.assertRaises(NotAuthenticated):
            with AuthMiddlewareMock(context=GrpcContextMock(metadata={'auth_credentials': 'lol'})) as mock:
                mock.process_request()

    def test_set_empty_auth_credentials(self):
        with AuthMiddlewareMock(context=GrpcContextMock(metadata={'auth_credentials': ''})) as mock:
            mock.process_request()
            self.assertEqual(self.storage.thread_local.auth_info, AuthInfo())

        # Check None as auth_credentials.
        with AuthMiddlewareMock(context=GrpcContextMock()) as mock:
            mock.process_request()
            self.assertEqual(self.storage.thread_local.auth_info, AuthInfo())

    def test_db_scope(self):
        with self.storage.thread_local.db_scope():
            self.assertTrue(self.storage.thread_local.read_only_db)

        with self.storage.thread_local.db_scope(False):
            self.assertFalse(self.storage.thread_local.read_only_db)
