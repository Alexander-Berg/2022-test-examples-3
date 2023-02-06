# -*- coding: utf-8 -*-

import mock
import random
from nose_parameterized import parameterized
from mpfs.core.wake_up.operations import WakeUpOperation
from test.parallelly.json_api.base import CommonJsonApiTestCase
from test.helpers.operation import PendingOperationDisabler
from test.helpers.stubs.services import PushServicesStub
from test.parallelly.api.disk.base import DiskApiTestCase
from mpfs.common.util import to_json, from_json
from mpfs.common.static import codes


class WakeUpTestCase(CommonJsonApiTestCase, DiskApiTestCase):
    device_id = 'DFBDE026-32ED-4S5A-9H9F-D1R497E64006'

    def test_wake_up_push_start(self):
        self.json_ok('wake_up_push_start', {'uid': self.uid, 'device_id': self.device_id})

    def test_wake_up_push_job_type(self):
        with mock.patch('mpfs.core.queue.mpfs_queue.put') as mocked_queue_put:
            self.json_ok('wake_up_push_start', {'uid': self.uid, 'device_id': self.device_id})
            args, kwargs = mocked_queue_put.call_args_list[0]
            assert args[1] == 'operation_service'

    def test_wake_up_push_start_with_missing_subscription(self):
        wrong_device_id = 'DFBDE026-32ED-4S5A-9H9F-D1R497E640061234'
        self.json_error('wake_up_push_start', {'uid': self.uid, 'device_id': wrong_device_id},
                        code=codes.DEVICE_SUBSCRIPTION_NOT_FOUND)

    def test_wake_up_push_start_with_uninitialized_uid(self):
        self.json_error('wake_up_push_start', {'uid': 1234, 'device_id': self.device_id}, code=codes.WH_USER_NEED_INIT)

    def test_second_wake_up_push_start_request(self):
        with PendingOperationDisabler(WakeUpOperation):
            oid1 = self.json_ok('wake_up_push_start', {'uid': self.uid, 'device_id': self.device_id})['oid']
            oid2 = self.json_ok('wake_up_push_start', {'uid': self.uid, 'device_id': self.device_id})['oid']
        self.assertEqual(oid1, oid2)

    def test_wake_up_push_start_interval(self):
        self.json_ok('wake_up_push_start', {'uid': self.uid, 'device_id': self.device_id, 'interval': 20})

    @parameterized.expand([(20, 20),
                           (7, 15),
                           (-14, 15),
                           (120, 120)])
    def test_wake_up_interval(self, interval, result_interval):
        with mock.patch('mpfs.core.operations.base.Operation.reenque') as mock_obj:
            resp = self.json_ok('wake_up_push_start', {'uid': self.uid,
                                                       'device_id': self.device_id,
                                                       'interval': interval})
            assert resp
            for call in mock_obj.call_args_list:
                assert call[0][0] == result_interval

    def test_wake_up_push_stop(self):
        session_id = self.json_ok('wake_up_push_start', {'uid': self.uid, 'device_id': self.device_id})['session_id']
        resp = self.client.get('disk/operations', uid=self.uid)
        self.assertEqual(len(from_json(resp.result)['items']), 1)
        self.json_ok('wake_up_push_stop', {'session_id': session_id})
        resp = self.client.get('disk/operations', uid=self.uid)
        self.assertEqual(len(from_json(resp.result)['items']), 0)

    @parameterized.expand([('wakeUpPushSessionId', codes.BAD_REQUEST_ERROR),
                           ('wakeUpPush:SessionId', codes.BAD_REQUEST_ERROR),
                           ('123:1234', codes.BAD_REQUEST_ERROR),
                           ('123:1234:1234', codes.BAD_REQUEST_ERROR),
                           (':89519942f316497b2e1453f5237644b1f1c5731fb2fd669d1b08c61e8f97c041', codes.PATH_ERROR),
                           ('128280859:', codes.PATH_ERROR),
                           ])
    def test_wake_up_push_stop_with_invalid_session_id(self, session_id, code):
        self.json_error('wake_up_push_stop', {'session_id': session_id}, code)

    def test_wake_up_push_stop_with_missing_operation(self):
        oid = self.json_ok('wake_up_push_start', {'uid': self.uid, 'device_id': self.device_id})['oid']
        random_oid = ''.join(random.sample(oid, len(oid)))
        session_id = '%s:%s' % (self.uid, random_oid)
        self.json_error('wake_up_push_stop', {'session_id': session_id}, code=codes.OPERATION_NOT_FOUND)

    def test_wake_up_push_batch_send(self):
        with PushServicesStub() as push_stub:
            with PendingOperationDisabler(WakeUpOperation):
                result = self.json_ok('wake_up_push_start', {'uid': self.uid, 'device_id': self.device_id})
                oid = result['oid']
                uid = self.uid
                PendingOperationDisabler.process(uid, oid)
                assert push_stub.batch_send.called
                args, _ = push_stub.batch_send.call_args_list[-1]
                batch_send_opts_recipients = [{self.uid: u'mob:f20e1c0cd36ea31432e66d880b20b16c'}]
                batch_send_opts_event = 'wake_up'
                batch_send_opts_data = to_json({'root': {'session_id': '{}:{}'.format(uid, oid), 'tag': 'wake_up'}})
                self.assertEqual(args[0], batch_send_opts_recipients)
                self.assertEqual(args[1], batch_send_opts_event)
                self.assertEqual(args[2], batch_send_opts_data)

    def test_wake_up_push_subscriptions_list(self):
        with PushServicesStub() as push_stub:
            with PendingOperationDisabler(WakeUpOperation):
                result = self.json_ok('wake_up_push_start', {'uid': self.uid, 'device_id': self.device_id})
                oid = result['oid']
                uid = self.uid
                PendingOperationDisabler.process(uid, oid)
                assert push_stub.subscriptions_list.called
                args, _ = push_stub.subscriptions_list.call_args_list[-1]
                self.assertEqual(args[0], self.uid)
