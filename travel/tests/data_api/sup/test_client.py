# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import json

import mock
import pytest
import requests
from hamcrest import assert_that, has_entries

from common.data_api.sup.client import SupClient, get_client


class TestSupClient(object):
    def test_call(self, httpretty):
        sup_url = 'https://sup_url'

        httpretty.register_uri(httpretty.GET, '{}/something/somepath'.format(sup_url), body='getresponse42')
        httpretty.register_uri(httpretty.POST, '{}/something/'.format(sup_url), body='postresponse42')
        httpretty.register_uri(httpretty.POST, '{}/somethingbad/'.format(sup_url), status=400)

        client = SupClient(sup_url=sup_url, oauth_token='mytoken')
        response = client.call('something', path='somepath', params={'a': 1})
        assert response == 'getresponse42'
        assert_that(httpretty.last_request.querystring, has_entries({'a': ['1']}))

        response = client.call('something', params={'a': 1}, data={'b': 2})
        assert response == 'postresponse42'
        assert httpretty.last_request.headers['Content-Type'] == 'application/json;charset=UTF-8'
        assert httpretty.last_request.headers['Authorization'] == 'OAuth mytoken'
        assert_that(httpretty.last_request.querystring, has_entries({'a': ['1']}))
        assert json.loads(httpretty.last_request.parsed_body) == {'b': 2}

        with pytest.raises(requests.HTTPError):
            client.call('somethingbad', params={'a': 1}, data={'b': 2})

    def test_pushes(self):
        client = SupClient()

        with mock.patch.object(SupClient, 'call') as m_call:
            m_call.return_value = 'response42'

            args = dict(
                project='suburban42',
                receivers=['tag:1', 'tag:2'],
                title='Яндекс',
                text='Актуальное расписание',
                image='ic_notification',
                image_url='https://wow',
                device_id_policy='device_id_policy',
                install_id_policy='install_id_policy',
                url='yandextrains://favourites',
                ttl=10,
                max_expected_receivers=20,
                data={'a': 1, 'b': 2, 'text': 'Очень актуальное расписание'},
                push_id='mypush',
                dry_run=False,
            )

            # проверяем обычный вызов со всеми параметрами
            response = client.pushes(**args)
            assert response == 'response42'
            assert len(m_call.call_args_list) == 1
            call = m_call.call_args_list[0]
            assert call[1] == {'params': {}}
            assert call[0][0] == 'pushes'

            assert call[0][1] == {
                "project": 'suburban42',
                "max_expected_receivers": 20,
                "ttl": 10,
                "schedule": "now",
                "adjust_time_zone": False,
                "receiver": ['tag:1', 'tag:2'],
                "notification": {
                    "title": 'Яндекс',
                    "body": 'Актуальное расписание',
                    "icon": 'https://wow',
                    "iconId": 'ic_notification',
                },
                "ios_features": {
                    "soundType": 0
                },
                "throttle_policies": {
                    "device_id": 'device_id_policy',
                    "install_id": 'install_id_policy'
                },
                "data": {
                    "push_id": 'mypush',
                    "push_uri": 'yandextrains://favourites',
                    "text": 'Очень актуальное расписание',
                    'a': 1,
                    'b': 2,
                },
            }

            # проверяем get-параметры запроса
            args['dry_run'] = True
            args['high_priority'] = True
            client.pushes(**args)
            assert m_call.call_args_list[1][1] == {'params': {'dry_run': True, 'priority': 'high'}}

            # проверяем push_id == project
            args.pop('push_id')
            client.pushes(**args)
            assert m_call.call_args_list[2][0][1]['data']['push_id'] == 'suburban42'

            # проверяем, что текст для приложения по умолчанию равен тексту пуша
            args.pop('data')
            client.pushes(**args)
            assert m_call.call_args_list[3][0][1]['data']['text'] == 'Актуальное расписание'

    def test_pushes_errors(self, httpretty):
        sup_url = 'http://aaaa.ru'
        httpretty.register_uri(httpretty.POST, sup_url + '/pushes/', responses=[
            httpretty.Response(
                body='''
                    {"errors": [
                        {"field": "aaa", "rejectedValue": "123", "defaultMessage": "baaad value"},
                        {"field": "bbb", "rejectedValue": "456", "defaultMessage": "very bad"}
                    ]}
                ''',
                status=400,
            ),
            httpretty.Response(
                body='''
                            {"errors": [
                                {"field": "aaa", "rejectedValue": "123", "defaultMessage": "baaad value"},
                                {"field": "bbb", "rejectedValue": "456", "defaultMessage": "very bad"}
                            ]}
                        ''',
                status=500,
            ),
            httpretty.Response(status=404, body=''),
        ])

        client = SupClient(sup_url=sup_url)

        # Для 400-го ответа парсим ошибку
        with pytest.raises(SupClient.InvalidRequest) as ex:
            client.pushes(project='123', receivers=['123'], title='123', text='123', device_id_policy='urgent')

        assert ex._excinfo[1].msg == 'SUP validation failed'
        assert ex._excinfo[1].errors == ['"aaa" = "123": baaad value', '"bbb" = "456": very bad']

        # Для других ошибок - не парсим
        with pytest.raises(requests.HTTPError):
            client.pushes(project='123', receivers=['123'], title='123', text='123', device_id_policy='urgent')

        with pytest.raises(requests.HTTPError):
            client.pushes(project='123', receivers=['123'], title='123', text='123', device_id_policy='urgent')

    def test_get_receivers_number(self):
        data = {
            "trace": {
                "resolveEvents": [
                    "[2017-08-17T15:54:54 +0300] (patch 0) resolved to targets in 2 millis",
                    "[2017-08-17T15:54:54 +0300] (patch 0) fetch takes 2 millis",
                    "[2017-08-17T15:54:54 +0300] (patch 0) resolved to 42 receiver(s) (0 with disabled notifications)",
                    "[2017-08-17T15:54:54 +0300] resolve takes 1 millis"],
            },
        }

        assert SupClient.get_receivers_number(data) == 42

        # проверяем, что не падаем на невалидных данных
        data.pop('trace')
        assert SupClient.get_receivers_number(data) is None


def test_get_client(tmpdir):
    client = get_client(sup_url='123', oauth_token='567')
    assert client.sup_url == '123'
    assert client.oauth_token == '567'
