# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import os

from search.martylib.auth.conditions import *
from search.martylib.core.exceptions import NotAuthenticated, NotAuthorized
from search.martylib.core.idm import STATIC_USER, STATIC_USER_ROLES
from search.martylib.core.storage import Storage
from search.martylib.grpc_utils.decorators import require_auth
from search.martylib.proto.structures import auth_pb2
from search.martylib.test_utils import TestCase


class TestConditions(TestCase):
    AUTH_INFO = auth_pb2.AuthInfo(
        login='test-user',
        groups=(1, 2),
        roles=('test.user', 'test.admin'),
        admin_roles=('test.admin', ),
        departments=('Test users', 'Robots'),
    )

    def test_unauthenticated(self):
        for condition in (
            Username('test-user'),
            StaffGroup(1234),
            Role('martylib.writer'),
            AdminRole('martylib.admin'),
            Department('search'),
        ):
            with self.assertRaises(NotAuthenticated):
                bool(condition)

    @TestCase.mock_auth(auth_info=AUTH_INFO)
    def test_username_match(self):
        self.assertTrue(Username('test-user'))
        self.assertFalse(Username('another-user'))

    @TestCase.mock_auth(auth_info=AUTH_INFO)
    def test_negated_username_match(self):
        self.assertFalse(-Username('test-user'))
        self.assertTrue(-Username('another-user'))

    @TestCase.mock_auth(auth_info=AUTH_INFO)
    def test_staff_group_match(self):
        self.assertTrue(StaffGroup(1))
        self.assertTrue(StaffGroup(2))
        self.assertFalse(StaffGroup(3))

    @TestCase.mock_auth(auth_info=AUTH_INFO)
    def test_negated_staff_group_match(self):
        self.assertFalse(-StaffGroup(1))
        self.assertFalse(-StaffGroup(2))
        self.assertTrue(-StaffGroup(3))

    @TestCase.mock_auth(auth_info=AUTH_INFO)
    def test_role_match(self):
        self.assertTrue(Role('test.user'))
        self.assertFalse(Role('test.missing'))

    @TestCase.mock_auth(auth_info=AUTH_INFO)
    def test_negated_role_match(self):
        self.assertFalse(-Role('test.user'))
        self.assertTrue(-Role('test.missing'))

    @TestCase.mock_auth(auth_info=AUTH_INFO)
    def test_admin_role_match(self):
        self.assertTrue(AdminRole('test.admin'))
        self.assertFalse(AdminRole('test.missing'))

    @TestCase.mock_auth(auth_info=AUTH_INFO)
    def test_negated_admin_role_match(self):
        self.assertFalse(-AdminRole('test.admin'))
        self.assertTrue(-AdminRole('test.missing'))

    @TestCase.mock_auth(auth_info=AUTH_INFO)
    def test_department_match(self):
        self.assertTrue(Department('Robots'))
        self.assertFalse(Department('Missing'))

    @TestCase.mock_auth(auth_info=AUTH_INFO)
    def test_negated_department_match(self):
        self.assertFalse(-Department('Robots'))
        self.assertTrue(-Department('Missing'))

    @TestCase.mock_auth(auth_info=AUTH_INFO)
    def test_any(self):
        self.assertTrue(
            any((
                Username('nope'),
                StaffGroup(0),
                Role('missing'),
                AdminRole('missing'),
                Department('Test users'),
            ))
        )

    @TestCase.mock_auth(auth_info=AUTH_INFO)
    def test_all(self):
        self.assertTrue(
            all((
                Username('test-user'),
                StaffGroup(1),
                Role('test.user'),
                AdminRole('test.admin'),
                Department('Robots'),
            ))
        )

    @TestCase.mock_auth(auth_info=AUTH_INFO)
    def test_any_role(self):
        self.assertTrue(AnyRole('missing.alpha', 'missing.bravo', 'test.user'))

    @TestCase.mock_auth(auth_info=AUTH_INFO)
    def test_require_auth(self):
        @require_auth(any_conditions=(StaffGroup(123), StaffGroup(234)))
        def _failing_any_conditions():
            pass

        @require_auth(any_conditions=(Username('nope'), Username('still nope'), Username('test-user')))
        def _any_conditions():
            pass

        @require_auth(all_conditions=(Username('test-user'), StaffGroup(1)))
        def _all_conditions():
            pass

        @require_auth(all_conditions=(Username('nope'), Username('test-user')))
        def _failing_all_conditions():
            pass

        _any_conditions()

        _all_conditions()

        with self.assertRaises(NotAuthorized):
            _failing_any_conditions()

        with self.assertRaises(NotAuthorized):
            _failing_all_conditions()


class IdmRoleTestCase(TestCase):
    """
    Use this test case if you need to use code with IDM roles check
    to mock users and their roles with init_static_user().
    See example in test_role().
    """

    storage = Storage()

    def __init__(self, *args, **kwargs):
        super(IdmRoleTestCase, self).__init__(*args, **kwargs)

    def init_static_user(self, login='', roles=None):
        os.environ[STATIC_USER] = login
        os.environ[STATIC_USER_ROLES] = roles
        self.storage.thread_local._auth_info = auth_pb2.AuthInfo(login=login)

    def test_role(self):
        self.init_static_user('login', 'system/admin:system/moderator')
        self.assertTrue(IdmRole('system/admin'))
        self.assertTrue(IdmRole('system/moderator'))
        self.assertFalse(IdmRole('system/user'))

        self.init_static_user('login', '')
        self.assertFalse(IdmRole('system/admin'))
        self.assertFalse(IdmRole('system/moderator'))
        self.assertFalse(IdmRole('system/user'))
