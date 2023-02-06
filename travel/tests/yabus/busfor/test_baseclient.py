# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import time
from datetime import datetime, timedelta

import freezegun
import mock

from yabus.busfor import baseclient
from yabus.busfor.baseclient import AccessToken, BaseClient, TokenProvider


class TestAccessToken(object):
    @freezegun.freeze_time("2000-01-01T12:00:00")
    def test_is_expired(self):
        assert AccessToken("foo", datetime(2000, 1, 1, 10)).is_expired()
        assert not AccessToken("bar", datetime(2000, 1, 1, 13)).is_expired()


class TestTokenProvider(object):
    @mock.patch.object(
        baseclient, "get_secret", autospec=True, return_value={"busfor-login": "login", "busfor-token": "password"}
    )
    def test_get_token(self, m_get_secret):
        with freezegun.freeze_time("2000-01-01T12:00:00") as frozen_time:
            session = mock.MagicMock()
            m_post = session.__enter__.return_value.post
            m_post.return_value = {
                "access_token": "access_token",
                "expires_on": time.time() + timedelta(hours=1).total_seconds(),
            }
            provider = TokenProvider(lambda: session)

            assert provider.get_token() == "access_token"
            m_get_secret.assert_called_once()
            m_post.assert_called_once_with("v2/login", data={"username": "login", "password": "password"})

            m_get_secret.reset_mock()
            m_post.reset_mock()
            assert provider.get_token() == "access_token"
            assert not m_get_secret.called
            assert not m_post.called

            frozen_time.tick(timedelta(hours=2))
            assert provider.get_token() == "access_token"
            assert not m_get_secret.called
            m_post.assert_called_once_with("v2/login", data={"username": "login", "password": "password"})


class TestBaseClient(object):
    def test_init(self):
        m_session_cls = mock.MagicMock()
        m_token_provider_cls = mock.MagicMock()
        m_get_token = m_token_provider_cls.return_value.get_token
        m_get_token.return_value = "token"

        class Client(BaseClient):
            session_cls = m_session_cls
            token_provider_cls = m_token_provider_cls

        client = Client()

        m_token_provider_cls.assert_called_once_with(mock.ANY, None, None)
        assert not m_session_cls.called

        client.session()

        m_get_token.assert_called_once_with()
        m_session_cls.assert_called_once_with(token="token")
