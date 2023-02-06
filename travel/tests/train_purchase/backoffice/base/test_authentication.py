# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import base64
import json

import mock
import pytest
from django.http.cookie import SimpleCookie
from hamcrest import assert_that, has_entries

from common.apps.train_order.models import BackofficeUser
from common.tester.utils.replace_setting import replace_setting
from common.utils.blackbox_wrapper import SessionError
from travel.rasp.train_api.train_purchase.backoffice import base


@pytest.mark.parametrize('auth_header', [
    'asdfasdf',  # bad base64
    base64.b64encode(b'{a:')  # bad json
])
def test_bad_auth_header(async_urlconf_client, auth_header):
    response = async_urlconf_client.get('/ru/train-purchase-backoffice/user-info/',
                                        HTTP_AUTHORIZATION=auth_header)
    assert response.status_code == 401
    assert response.data['detail'] == 'Bad auth header'


@pytest.mark.parametrize('auth, detail', [
    ({'a': 3}, 'Unknown auth type'),
    ({'a': 3, 'type': 'blackbox'}, 'Bad auth header params'),
])
def test_bad_auth_header_params(async_urlconf_client, auth, detail):
    response = async_urlconf_client.get('/ru/train-purchase-backoffice/user-info/',
                                        HTTP_AUTHORIZATION=base64.b64encode(json.dumps(auth)))

    assert response.status_code == 401
    assert response.data['detail'] == detail


@mock.patch.object(base, 'get_session', autospec=True)
def test_bad_auth_session(m_get_session, async_urlconf_client):
    m_get_session.side_effect = SessionError
    response = async_urlconf_client.get('/ru/train-purchase-backoffice/user-info/',
                                        HTTP_AUTHORIZATION=base64.b64encode(json.dumps({
                                            'type': 'blackbox',
                                            'userip': '1.2.3.4',
                                            'host': 'train-purchase-backoffice.rasp.yandex.ru',
                                            'sessionid': 'sessionid_value',
                                            'ssl_sessionid': 'ssl_sessionid_value'
                                        })))

    m_get_session.assert_called_once_with('1.2.3.4', 'train-purchase-backoffice.rasp.yandex.ru',
                                          'sessionid_value', 'ssl_sessionid_value')

    assert response.status_code == 401
    assert response.data['detail'] == 'Bad auth session'


@mock.patch.object(base, 'get_session', autospec=True)
def test_blackbox_is_not_available(m_get_session, async_urlconf_client):
    m_get_session.return_value = None
    response = async_urlconf_client.get('/ru/train-purchase-backoffice/user-info/',
                                        HTTP_AUTHORIZATION=base64.b64encode(json.dumps({
                                            'type': 'blackbox',
                                            'userip': '1.2.3.4',
                                            'host': 'train-purchase-backoffice.rasp.yandex.ru',
                                            'sessionid': 'sessionid_value',
                                            'ssl_sessionid': 'ssl_sessionid_value'
                                        })))

    m_get_session.assert_called_once_with('1.2.3.4', 'train-purchase-backoffice.rasp.yandex.ru',
                                          'sessionid_value', 'ssl_sessionid_value')

    assert response.status_code == 500
    assert response.data['detail'] == 'Blackbox is not available'


@mock.patch.object(base, 'get_session', autospec=True)
@pytest.mark.dbuser
def test_bad_user_session(m_get_session, async_urlconf_client):
    BackofficeUser.objects.create(username='yndx.yaemployee', is_active=True)
    m_get_session.return_value = mock.Mock(fields={'login': 'some_user', 'staff_login': ''})
    response = async_urlconf_client.get('/ru/train-purchase-backoffice/user-info/',
                                        HTTP_AUTHORIZATION=base64.b64encode(json.dumps({
                                            'type': 'blackbox',
                                            'userip': '1.2.3.4',
                                            'host': 'train-purchase-backoffice.rasp.yandex.ru',
                                            'sessionid': 'sessionid_value',
                                            'ssl_sessionid': 'ssl_sessionid_value'
                                        })))

    m_get_session.assert_called_once_with('1.2.3.4', 'train-purchase-backoffice.rasp.yandex.ru',
                                          'sessionid_value', 'ssl_sessionid_value')
    assert response.status_code == 403
    assert response.data['detail'] == 'Has no permission'


@mock.patch.object(base, 'get_session', autospec=True)
@pytest.mark.dbuser
def test_blackbox_registered_user(m_get_session, async_urlconf_client):
    BackofficeUser.objects.create(username='yndx.yaemployee', is_active=True, is_admin=True)
    m_get_session.return_value = mock.Mock(fields={'login': 'yndx.yaemployee', 'yaemployee': ''})
    response = async_urlconf_client.get('/ru/train-purchase-backoffice/user-info/',
                                        HTTP_AUTHORIZATION=base64.b64encode(json.dumps({
                                            'type': 'blackbox',
                                            'userip': '1.2.3.4',
                                            'host': 'train-purchase-backoffice.rasp.yandex.ru',
                                            'sessionid': 'sessionid_value',
                                            'ssl_sessionid': 'ssl_sessionid_value'
                                        })))

    m_get_session.assert_called_once_with('1.2.3.4', 'train-purchase-backoffice.rasp.yandex.ru',
                                          'sessionid_value', 'ssl_sessionid_value')
    assert response.status_code == 200
    assert_that(response.data, has_entries({
        'username': 'yndx.yaemployee',
        'isActive': True,
        'isAdmin': True
    }))


@pytest.mark.dbuser
def test_direct_registered_user(async_urlconf_client):
    BackofficeUser.objects.create(username='yndx.yaemployee', is_active=True, is_admin=True)
    response = async_urlconf_client.get('/ru/train-purchase-backoffice/user-info/',
                                        HTTP_AUTHORIZATION=base64.b64encode(json.dumps({
                                            'type': 'direct',
                                            'login': 'yndx.yaemployee'
                                        })))

    assert response.status_code == 200
    assert_that(response.data, has_entries({
        'username': 'yndx.yaemployee',
        'isActive': True,
        'isAdmin': True
    }))


@pytest.mark.dbuser
@pytest.mark.parametrize('auth', [
    {'type': 'direct', 'login': 'bad_login'},
    {'type': 'direct'}
])
def test_direct_bad_user(async_urlconf_client, auth):
    BackofficeUser.objects.create(username='yndx.yaemployee', is_active=True)
    response = async_urlconf_client.get('/ru/train-purchase-backoffice/user-info/',
                                        HTTP_AUTHORIZATION=base64.b64encode(json.dumps(auth)))

    assert response.status_code == 403
    assert response.data['detail'] == 'Has no permission'


@mock.patch.object(base, 'get_session', autospec=True)
@pytest.mark.dbuser
def test_cookie_registered_user(m_get_session, async_urlconf_client):
    cookies = SimpleCookie()
    cookies['Session_id'] = 'sessionid_value'
    cookies['sessionid2'] = 'ssl_sessionid_value'

    BackofficeUser.objects.create(username='yndx.yaemployee', is_active=True, is_admin=True)
    m_get_session.return_value = mock.Mock(fields={'login': 'yndx.yaemployee', 'yaemployee': ''})
    response = async_urlconf_client.get('/ru/train-purchase-backoffice/user-info/',
                                        HTTP_COOKIE=cookies.output(header='', sep='; '),
                                        HTTP_HOST='train-purchase-backoffice.rasp.yandex.ru',
                                        REMOTE_ADDR='1.2.3.4')

    m_get_session.assert_called_once_with('1.2.3.4', 'train-purchase-backoffice.rasp.yandex.ru',
                                          'sessionid_value', 'ssl_sessionid_value')
    assert response.status_code == 200
    assert_that(response.data, has_entries({
        'username': 'yndx.yaemployee',
        'isActive': True,
        'isAdmin': True
    }))


@replace_setting('BYPASS_BACKOFFICE_AUTH', True)
def test_bypass_backoffice_auth(async_urlconf_client):
    response = async_urlconf_client.get('/ru/train-purchase-backoffice/user-info/')
    assert response.status_code == 200
    assert_that(response.data, has_entries({
        'username': 'anonymous',
        'isActive': True,
        'isAdmin': True
    }))
