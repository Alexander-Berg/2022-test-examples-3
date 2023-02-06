# -*- coding: utf-8 -*-
import mock
import requests
import pytest

from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.avia_api.avia.lib.pusher.transport.xiva import Xiva, XivaError
from travel.avia.avia_api.avia.lib.pusher.tag import PusherTag


class XivaTest(TestCase):

    def mock_response(self, data):
        # type: (dict) -> requests.Response
        response_json_mock = mock.Mock()
        response_json_mock.return_value = data

        response = requests.Response()
        response.status_code = 200
        response.request = requests.PreparedRequest()
        response.json = response_json_mock

        return response

    def get_repack(self, message):
        # type: (str) -> dict
        return {
            'apns': {
                'aps': {
                    'alert': message,
                    'content-available': 1,
                },
                'repack_payload': ['a1', 'a2'],
            },
            'gcm': {
                'title': message,
                'sound': 'default',
            },
            'wns': {
                'toast': message,
            },
            'mpns': {
                'toast': message,
            },
        }

    @mock.patch.object(requests, 'post')
    def test_unknown_platform(self, post_mock):
        transport = Xiva()
        with pytest.raises(XivaError):
            transport.add_user('push_token', 'uid', 'uuid', 'platform')

        assert post_mock.call_count == 0

    @mock.patch.object(requests, 'post')
    def test_platform(self, post_mock):
        transport = Xiva()
        transport.add_user('push_token', 'uid', 'uuid', 'android')

        assert post_mock.call_count == 1

    @mock.patch.object(requests, 'post')
    def test_add_user(self, post_mock):
        transport = Xiva()
        transport.add_user('push_token', 'uid', 'uuid', 'android')

        assert post_mock.call_count == 1

        args, kwargs = post_mock.call_args
        assert kwargs['params']['user'] == 'uid'
        assert kwargs['params']['platform'] == 'gcm'
        assert kwargs['data']['push_token'] == 'push_token'

    @mock.patch.object(requests, 'post')
    def test_delete_by_uid(self, post_mock):
        transport = Xiva()
        transport.delete('uid', 'uuid')

        assert post_mock.call_count == 1

        args, kwargs = post_mock.call_args
        assert kwargs['params']['uuid'] == 'uuid'
        assert kwargs['params']['user'] == 'uid'

    @mock.patch.object(requests, 'post')
    def test_push_one(self, post_mock):
        message = 'message'
        data = {'a1': 'A1', 'a2': {'b1': 'B1'}}
        post_mock.return_value = self.mock_response({'results': [
            {'code': 200},
        ]})

        transport = Xiva()
        sent = transport.push('uuid', data.copy(), message=message, ttl=60, push_tag=PusherTag.Undefined)

        assert sent == 1
        assert post_mock.call_count == 1

        args, kwargs = post_mock.call_args
        assert kwargs['params']['ttl'] == 60
        assert kwargs['params']['event'] == PusherTag.Undefined
        assert kwargs['json']['recipients'] == ['uuid']
        assert kwargs['json']['payload'] == data
        assert kwargs['json']['repack'] == self.get_repack(message)

    @mock.patch.object(requests, 'post')
    def test_push_many(self, post_mock):
        message = 'message'
        data = {'a1': 'A1', 'a2': {'b1': 'B1'}}
        uuids = ['uuid1', 'uuid2', 'uuid3']
        post_mock.return_value = self.mock_response({'results': [
            {'code': 200},
            {'code': 200},
            {'code': 400},
        ]})

        transport = Xiva()
        sent = transport.push(uuids, data.copy(), message=message, ttl=60, push_tag=PusherTag.Undefined)

        assert sent == 2
        assert post_mock.call_count == 1

        args, kwargs = post_mock.call_args
        assert kwargs['params']['ttl'] == 60
        assert kwargs['params']['event'] == PusherTag.Undefined
        assert kwargs['json']['recipients'] == uuids
        assert kwargs['json']['payload'] == data
        assert kwargs['json']['repack'] == self.get_repack(message)
