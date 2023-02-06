# -*- coding: utf-8 -*-

import mock
import pytest
from blackbox import odict
from django.http import HttpResponseRedirect

from common.middleware.blackbox_session import BlackboxSessionMiddleware
from common.tester.utils.replace_setting import replace_setting
from common.tests.middleware.request_factory import MiddlewareRequest


middleware = BlackboxSessionMiddleware()


def create_session(**kwargs):
    session = dict(status='VALID', valid=True, secure=True, uid='UID', redirect=False)
    session.update(kwargs)
    return odict(session)


def create_good_request_without_need_to_redirect(**kwargs):
    return MiddlewareRequest(cookies={'Session_id': 123, 'sessionid2': 456}, is_secure=True, **kwargs)


def create_good_request_with_need_to_redirect(**kwargs):
    return MiddlewareRequest(tld='ua', cookies={'Session_id': 123, 'sessionid2': 456}, is_secure=True, **kwargs)


@pytest.yield_fixture
def m_blackbox_sessionid():
    with mock.patch('common.utils.blackbox_wrapper.XmlBlackbox.sessionid', autospec=True) as m_blackbox_sessionid:
        yield m_blackbox_sessionid


@pytest.mark.parametrize('request_kwargs', (
    # при запросе по http сессия не секьюрная
    dict(is_secure=False, cookies={'Session_id': 123}),
    # без куки Session_id сессия не валидная
    dict(is_secure=True),
    # без куки sessionid2 сессия не секьюрная
    dict(is_secure=True, cookies={'Session_id': 123})
))
def test_blackbox_shortcuts(m_blackbox_sessionid, request_kwargs):
    """
    Не обращается к blackbox когда невозможно получить валидную и секьюрную сессию.
    """
    request = MiddlewareRequest(**request_kwargs)
    result = middleware.process_request(request)

    assert result is None
    assert request.blackbox_session is None
    assert not m_blackbox_sessionid.called


def test_no_session_from_blackbox(m_blackbox_sessionid):
    """
    Не редиректит, если не смог получить сессию из blackbox.
    """
    m_blackbox_sessionid.return_value = None
    request = create_good_request_without_need_to_redirect()
    with replace_setting('TVM_SERVICE_ID', 666):
        result = middleware.process_request(request)

    assert result is None
    assert request.blackbox_session is None
    assert m_blackbox_sessionid.call_count == 1


def test_noauth_session(m_blackbox_sessionid):
    """
    Если статус NOAUTH, то не редиректит даже если MDA включен и домен не корневой.
    """
    m_blackbox_sessionid.return_value = create_session(valid=False, status='NOAUTH')
    request = create_good_request_with_need_to_redirect()
    with replace_setting('TVM_SERVICE_ID', 666):
        result = middleware.process_request(request)

    assert request.blackbox_session is None
    assert result is None


def test_robot(m_blackbox_sessionid):
    """
    Если пришел робот, то в черный ящик не обращаемся и никаких ридеректов не производим.
    """
    request = create_good_request_with_need_to_redirect(is_robot=True)

    result = middleware.process_request(request)

    assert request.blackbox_session is None
    assert result is None
    assert not m_blackbox_sessionid.called


@pytest.mark.parametrize('session', (
    create_session(secure=False),
    create_session(uid=None),
    create_session(valid=False, secure=False, uid=None),
))
def test_not_valid_or_not_secure_session_from_blackbox(m_blackbox_sessionid, session):
    """
    Не сохраняет не секьюрную, лайт или не валидную сессию.
    """
    m_blackbox_sessionid.return_value = session
    request = create_good_request_without_need_to_redirect()
    with replace_setting('TVM_SERVICE_ID', 666):
        result = middleware.process_request(request)

    assert result is None
    assert request.blackbox_session is None
    assert m_blackbox_sessionid.call_count == 1


@pytest.mark.parametrize('tld, is_redirect_expected', (
    ('ru', False),
    ('ua', False)
))
def test_invalid_session(m_blackbox_sessionid, tld, is_redirect_expected):
    """
    При отсутствии Session_id не редиректит
    """
    request = MiddlewareRequest(tld=tld, is_secure=True)
    result = middleware.process_request(request)

    assert not m_blackbox_sessionid.called
    assert request.blackbox_session is None
    if is_redirect_expected:
        assert isinstance(result, HttpResponseRedirect)
    else:
        assert result is None


@pytest.mark.parametrize('session, is_redirect_expected', (
    (create_session(), False),
    (create_session(status='NEED_RESET', redirect=True), True),
))
def test_renew(m_blackbox_sessionid, session, is_redirect_expected):
    """
    При валидной сессии редиректит только если ее статус NEED_RESET.
    """
    m_blackbox_sessionid.return_value = session
    request = create_good_request_without_need_to_redirect()
    with replace_setting('TVM_SERVICE_ID', 666):
        result = middleware.process_request(request)

    if is_redirect_expected:
        assert isinstance(result, HttpResponseRedirect)
    else:
        assert result is None
        assert request.blackbox_session == session


@pytest.mark.parametrize('request_kwargs', (
    dict(nocookiesupport=True),
))
@pytest.mark.parametrize('invalid_session', (True, False))
def test_request_with_no_redirect(m_blackbox_sessionid, invalid_session, request_kwargs):
    if invalid_session:
        request = create_good_request_with_need_to_redirect(**request_kwargs)
    else:
        request = create_good_request_without_need_to_redirect(**request_kwargs)
        m_blackbox_sessionid.return_value = create_session(status='NEED_RESET', redirect=True)

    with replace_setting('TVM_SERVICE_ID', 666):
        assert middleware.process_request(request) is None


def test_blackbox_userip(m_blackbox_sessionid):
    with replace_setting('TVM_SERVICE_ID', 666):
        middleware.process_request(create_good_request_without_need_to_redirect(client_ip='1.2.3.4'))

    m_blackbox_sessionid.assert_called_once_with(mock.ANY, mock.ANY, '1.2.3.4', mock.ANY, sslsessionid=mock.ANY,
                                                 headers={u'X-Ya-Service-Ticket': None})
