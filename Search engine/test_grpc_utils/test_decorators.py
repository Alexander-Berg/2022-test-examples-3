# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals


from search.martylib.core.exceptions import NotAuthenticated, NotAuthorized
from search.martylib.grpc_utils.decorators import require_auth
from search.martylib.test_utils import TestCase


def f():
    pass


class TestRequireAuth(TestCase):
    @classmethod
    def setUpClass(cls):
        super(TestRequireAuth, cls).setUpClass()

    def test_require_authentication(self):
        with self.assertRaises(NotAuthenticated):
            require_auth()(f)()

    def test_require_authorization_without_authentication(self):
        with self.assertRaises(NotAuthenticated):
            require_auth(roles=['foo', 'bar'])(f)()

    def test_require_roles(self):
        with self.mock_auth(login='test-user'):
            # 'baz' role not granted
            with self.assertRaises(NotAuthorized):
                require_auth(roles=['baz'])(f)()

        with self.mock_auth(login='test-user', roles=['foo']):
            # 'foo' granted, 'baz' not granted
            with self.assertRaises(NotAuthorized):
                require_auth(roles=['foo', 'baz'])(f)()

        with self.mock_auth(login='test-user', roles=['foo']):
            # 'foo' granted
            require_auth(roles=['foo'])(f)()

        with self.mock_auth(login='test-user', roles=['bar']):
            # 'bar' granted
            require_auth(roles=['bar'])(f)()

        with self.mock_auth(login='test-user', roles=['foo', 'bar']):
            # both 'foo' and 'bar' granted
            require_auth(roles=['foo', 'bar'])(f)()

    def test_require_logins(self):
        with self.mock_auth(login='test-user'):
            with self.assertRaises(NotAuthorized):
                require_auth(logins=['roboslone'])(f)()

        with self.mock_auth(login='robot-yappy'):
            require_auth(logins=['robot-yappy'])(f)()
            require_auth(logins=['robot-yappy', 'roboslone'])(f)()
            require_auth(logins=['roboslone', 'robot-yappy'])(f)()
