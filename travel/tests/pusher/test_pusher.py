# -*- coding: utf-8 -*-
import logging
import sys
import mock
import pytest
import json

from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.avia_api.avia.lib.pusher import Pusher, PusherTag
from travel.avia.avia_api.avia.lib.pusher.exception import PusherError
from travel.avia.avia_api.avia.lib.pusher.transport import NullTransport
from travel.avia.avia_api.avia.lib.pusher.transport.xiva import Xiva
from travel.avia.avia_api.avia.v1.model.device import Device
from travel.avia.avia_api.avia.v1.model.user import User

log = logging.getLogger('')
log.addHandler(logging.StreamHandler(sys.stdout))
log.setLevel(logging.INFO)


class PusherTest(TestCase):

    @staticmethod
    def create_device(uuid=None, push_token=None, platform=None, transport=None, user_uid=None):
        device = Device()
        device.uuid = uuid
        device.push_token = push_token
        device.platform = platform
        device.transport = transport
        device.user = User()
        device.user.yandex_uid = user_uid

        return device

    def test_add_user_unknown_platform(self):
        device = self.create_device()
        pusher = Pusher(NullTransport())

        with pytest.raises(PusherError):
            pusher.add(device)

    @mock.patch.object(NullTransport, 'add_user')
    def test_add_user(self, transport_add_user_mock):
        device = self.create_device(
            uuid='uuid', push_token='push_token', platform='android', user_uid='yandex_uid'
        )

        pusher = Pusher(NullTransport())
        pusher.add(device)

        assert transport_add_user_mock.call_count == 1

        args, kwargs = transport_add_user_mock.call_args
        assert not kwargs
        assert args == (
            device.push_token,
            device.user.yandex_uid,
            device.uuid,
            device.platform,
        )

    @mock.patch.object(NullTransport, 'delete')
    @mock.patch.object(Xiva, 'delete')
    @mock.patch.object(Device, 'objects')
    @mock.patch.object(Device, 'save')
    def test_delete_by_uuid(self, device_save_mock, device_objects_mock, transport_delete_mock, null_delete_mock):
        device1 = self.create_device(uuid='uuid_transport', user_uid='uid_transport', transport='Xiva')
        device2 = self.create_device(uuid='uuid_null', user_uid='uid_null')
        device_objects_mock.return_value = [
            device1, device2
        ]
        device_save_mock.return_value = None

        pusher = Pusher(NullTransport())
        pusher.delete_by_uuid('uuid_null')

        assert device_objects_mock.call_count == 1

        assert null_delete_mock.call_count == 1
        args, kwargs = null_delete_mock.call_args
        assert not kwargs
        assert args == (
            device2.user.yandex_uid, device2.uuid
        )

        assert transport_delete_mock.call_count == 1
        args, kwargs = transport_delete_mock.call_args
        assert not kwargs
        assert args == (
            device1.user.yandex_uid, device1.uuid
        )

        assert device1.push_token is None
        assert device2.push_token is None

    @mock.patch.object(NullTransport, 'push')
    @mock.patch.object(Xiva, 'push')
    def test_push_many_devices(self, transport_push_mock, null_push_mock):
        data = {'a1': 'A1', 'a2': {'b1': 'B1'}, 'u': {'u1': 'U1', 'u2': 'U2'}}
        message = 'message'
        device_transport = self.create_device(uuid='uuid1', transport='Xiva')
        device_null = self.create_device(uuid='uuid1')

        pusher = Pusher(NullTransport())
        pusher.push_many(
            devices=[device_transport, device_null],
            data=data.copy(),
            message=message,
            ttl=60,
            push_tag=PusherTag.Undefined
        )

        data['pw_msg'] = 1
        data['u'] = json.dumps(data['u'], separators=(',', ':'))

        assert null_push_mock.call_count == 1
        args, kwargs = null_push_mock.call_args
        assert args[0] == [device_null.uuid]
        assert args[1] == data
        assert kwargs == {
            'message': message,
            'ttl': 60,
            'push_tag': PusherTag.Undefined,
        }

        assert transport_push_mock.call_count == 1
        args, kwargs = null_push_mock.call_args
        assert args[0] == [device_transport.uuid]
        assert args[1] == data
        assert kwargs == {
            'message': message,
            'ttl': 60,
            'push_tag': PusherTag.Undefined,
        }

    @mock.patch.object(NullTransport, 'push')
    @mock.patch.object(Xiva, 'push')
    def test_push_many_users_and_devices(self, transport_push_mock, null_push_mock):
        data = {'a1': 'A1', 'a2': {'b1': 'B1'}, 'u': {'u1': 'U1', 'u2': 'U2'}}
        message = 'message'
        device_transport = self.create_device(uuid='uuid1', transport='Xiva')
        user_transport = User()
        user_transport.devices = [device_transport]
        device_null = self.create_device(uuid='uuid1')
        user_null = User()
        user_null.devices = [device_null, device_transport]

        pusher = Pusher(NullTransport())
        pusher.push_many(
            users=[user_null, user_transport],
            devices=[device_transport, device_null],
            data=data.copy(),
            message=message,
            ttl=60,
            push_tag=PusherTag.Undefined
        )

        data['pw_msg'] = 1
        data['u'] = json.dumps(data['u'], separators=(',', ':'))

        assert null_push_mock.call_count == 1
        args, kwargs = null_push_mock.call_args
        assert args[0] == [device_null.uuid]
        assert args[1] == data
        assert kwargs == {
            'message': message,
            'ttl': 60,
            'push_tag': PusherTag.Undefined,
        }

        assert transport_push_mock.call_count == 1
        args, kwargs = null_push_mock.call_args
        assert args[0] == [device_transport.uuid]
        assert args[1] == data
        assert kwargs == {
            'message': message,
            'ttl': 60,
            'push_tag': PusherTag.Undefined,
        }
