# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import collections

from search.martylib.core.exceptions import NotAuthenticated
from search.martylib.test_utils import TestCase
from search.martylib.webauth.client import WebAuthClientMock


logins = {
    ('token1', None): 'login1',
    (None, 'session_id2'): 'login2',
    ('token3', 'session_id3'): 'login3',
}

roles = collections.defaultdict(list)
roles[('token1', None)] = ['admin', 'moderator']
roles[(None, 'session_id2')] = ['admin']


class TestMockWebAuth(TestCase):

    client = WebAuthClientMock(
        logins=logins,
        roles=roles,
    )

    def test_get_login(self):
        with self.assertRaises(NotAuthenticated):
            self.client.get_login(token='token_unknown')

        for credentials, login in logins.items():
            token, session_id = credentials
            self.assertEqual(
                self.client.get_login(token=token, session_id=session_id),
                login,
                msg='unexpected result in case #{}::{}'.format(credentials, login),
            )

    def test_check_role(self):
        with self.assertRaises(NotAuthenticated):
            self.client.check_role(role='admin', token='token_unknown')

        for credentials, user_roles in roles.items():
            token, session_id = credentials
            for role in user_roles:
                self.assertTrue(self.client.check_role(role, token=token, session_id=session_id), msg='unexpected result in case #{}::{}'.format(credentials, role),)

        no_roles_token, no_roles_session_id = 'token3', 'session_id3'
        self.assertFalse(self.client.check_role('admin', token=no_roles_token, session_id=no_roles_session_id))
        self.assertFalse(self.client.check_role('moderator', token=no_roles_token, session_id=no_roles_session_id))
