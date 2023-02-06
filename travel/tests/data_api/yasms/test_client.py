# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pytest
from requests import HTTPError

from common.data_api.yasms.client import YaSMSClient, YaSMSClientError
from common.tester.utils.replace_setting import replace_setting
from common.utils.lxmlutils import get_sub_tag_text

DUMMY_URL = 'https://yasms.host.ru'

RESPONSE_BASE = '<?xml version="1.0" encoding="utf-8"?> <doc>{}</doc>'
OK_RESPONSE = RESPONSE_BASE.format('<message-sent id="{}" />')
ERR_RESPONSE = RESPONSE_BASE.format('<errorcode>{}</errorcode><error>{}</error>')


class TestYaSMSClient(object):
    def test_call_api(self, httpretty):
        httpretty.register_uri(
            httpretty.GET, '{}/{}'.format(DUMMY_URL, 'somemethod'),
            responses=[
                httpretty.Response(body=RESPONSE_BASE.format('<wow>suchsms</wow>')),
                httpretty.Response(body=ERR_RESPONSE.format('BADPHONE', 'Bad phone number format')),
                httpretty.Response(body=OK_RESPONSE.format(1), status=500),
                httpretty.Response(body=RESPONSE_BASE.format('<wow>sotext</wow>')),
            ],
        )

        client = YaSMSClient(DUMMY_URL, 'rasp')

        response_xml = client.call_api('somemethod', {'a': 1, 'b': 2})
        assert httpretty.last_request.querystring == {
            'a': ['1'],
            'b': ['2'],
            'sender': ['rasp'],
        }
        assert get_sub_tag_text(response_xml, 'wow') == 'suchsms'

        with pytest.raises(YaSMSClientError) as excinfo:
            client.call_api('somemethod', {'a': 1, 'b': 2})

        assert excinfo.value.err_code == 'BADPHONE'
        assert excinfo.value.err_msg == 'Bad phone number format'

        with pytest.raises(HTTPError):
            client.call_api('somemethod', {'a': 1, 'b': 2})

        # проверяем дефолтную инициализацию клиента из настроек
        with replace_setting('YASMS_URL', DUMMY_URL), \
                replace_setting('YASMS_SENDER', 'rasp123'):
            client = YaSMSClient()
            response_xml = client.call_api('somemethod', {'a': 1, 'b': 2})
            assert httpretty.last_request.querystring == {
                'a': ['1'],
                'b': ['2'],
                'sender': ['rasp123'],
            }
            assert get_sub_tag_text(response_xml, 'wow') == 'sotext'

    def test_sendsms(self, httpretty):
        httpretty.register_uri(
            httpretty.GET, '{}/{}'.format(DUMMY_URL, 'sendsms'),
            responses=[
                httpretty.Response(body=OK_RESPONSE.format(42)),
                httpretty.Response(body=OK_RESPONSE.format(43)),
            ],
        )

        client = YaSMSClient(DUMMY_URL, 'rasp')

        with pytest.raises(ValueError):
            client.sendsms('test', phone='+742424242', uid='1111')

        with pytest.raises(ValueError):
            client.sendsms('test')

        with mock.patch.object(client, 'call_api', wraps=client.call_api) as m_call_api:
            msg_id = client.sendsms('test', '+742424242')
            assert msg_id == '42'
            m_call_api.assert_called_once_with('sendsms', {
                'text': 'test',
                'phone': '+742424242',
                'utf8': 1,
            })

            m_call_api.reset_mock()
            msg_id = client.sendsms('test', uid='123')
            assert msg_id == '43'
            m_call_api.assert_called_once_with('sendsms', {
                'text': 'test',
                'utf8': 1,
                'uid': '123'
            })

    @replace_setting('YASMS_DONT_SEND_ANYTHING', True)
    def test_log_send_sms(self):
        client = YaSMSClient()

        assert client.sendsms('test', phone='+742424242') == 'log_only'
        assert client.sendsms('test', uid='user_uid') == 'log_only'

        with mock.patch.object(client, 'call_api') as m_call_api:
            m_call_api.assert_not_called()
