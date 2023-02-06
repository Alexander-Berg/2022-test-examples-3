# -*- coding: utf-8 -*-
import mock
import pytest
import urlparse

from attrdict import AttrDict
from hamcrest import (
    assert_that,
    has_entries,
)

from mpfs.common.util import urls
from mpfs.engine.http import client as http_client
from mpfs.common.static import tags
from test.parallelly.api.base import ApiTestCase
from test.base_suit import UserTestCaseMixin


class NotifierBaseTestCase(UserTestCaseMixin, ApiTestCase):
    pass


class MarkNotificationAsReadTestcase(NotifierBaseTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    service = 'disk'
    notification_id = '12292555439655'
    url = 'notifier/notifications/%s/%s/%s/mark-as-read' % (service, '%s', notification_id)

    def test_common(self):
        with self.specified_client(uid=self.uid, login=self.login, id='disk_verstka_ext'):
            response = self.client.put(self.url % 'comment_like')
            assert response.status_code == 200
            assert response.content == '{}'

    def test_scope_permissions(self):
        with self.specified_client(uid=self.uid, scopes=['cloud_api.notifier:notification.mark_as_read']):
            response = self.client.put(self.url % 'comment_like')
            assert response.status_code == 200

    def test_auth(self):
        with self.specified_client(uid=self.uid, login=self.login, id='other'):
            response = self.client.put(self.url % 'comment_like')
            assert response.status_code == 403

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-33860')
    def test_bad_type(self):
        with self.specified_client(uid=self.uid, login=self.login, id='disk_verstka_ext'):
            response = self.client.put(self.url % 'bad_type')
            assert response.status_code == 400
            assert 'BadNotificationEntityTypeError' in response.content

    def test_proxy_parameters(self):
        """
        Проверяем прокидываемые параметры
        """
        notification_entity_type = 'comment_like'
        with self.specified_client(uid=self.uid, login=self.login, id='disk_verstka_ext'):
            with mock.patch.object(http_client, 'open_url',
                                   return_value=(200, {}, {})) as open_url_stub:
                self.client.put(self.url % notification_entity_type)

                args, kwargs = open_url_stub.call_args
                url_parts = urlparse.urlsplit(args[0])
                param_dict = dict(urlparse.parse_qsl(url_parts.query))
                expected_params = {
                    'record_type': notification_entity_type,
                    'service': self.service,
                    'group_key': self.notification_id,
                }
                assert_that(param_dict, has_entries(**expected_params))


class NotifierProxyHandlerTestcase(NotifierBaseTestCase):
    api_version = 'v1'
    url = 'notifier/service/%s'

    def test_not_available_for_external_clients(self):
        resp = self.external_client.get(self.url % 'random/path')
        assert resp.status_code == 403

    def test_proxy_pass(self):
        response_code = 200
        response_content = '{"a":"1"}'
        path_part = 'random/path/part'
        params = {
            'service': 'test',
            'actor': 'test'
        }
        params_with_client_values = {
            'client_name': 'autompfstestservice',
            'service': 'test',
            'client_id': 'autompfstest',
            'actor': 'test'
        }
        for http_method in ('GET', 'POST', 'PUT', 'DELETE'):
            with mock.patch.object(http_client, 'open_url', return_value=(response_code, response_content, {})) as open_url_stub, \
                    self.specified_client(scopes=[''], uid=None):
                req_url = self.url % (path_part + '?' + urls.urlencode(params))
                response = self.client.request(http_method, req_url)
                assert response.status_code == response_code
                assert response.content == response_content
                args, kwargs = open_url_stub.call_args
                assert kwargs['method'] == http_method

                url_parts = urlparse.urlsplit(args[0])
                assert url_parts.path == '/notifier/%s' % path_part
                param_dict = dict(urlparse.parse_qsl(url_parts.query))
                assert params_with_client_values == param_dict
