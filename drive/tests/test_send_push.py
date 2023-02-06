import io
import json
import uuid
from unittest import mock, TestCase

from django.urls import reverse

from cars.django.tests import CarsharingAPITestCase
from cars.users.factories.user import UserFactory

from cars.admin.core.send_push_manager import SendPushManager
from cars.admin.models.push_preset import PushPreset
from .base import AdminAPITestCase


class SendPushManagerTestCase(CarsharingAPITestCase):

    def setUp(self):
        self.users = [UserFactory() for _ in range(20)]
        self.pusher_mock = mock.MagicMock()
        self.push_manager = SendPushManager.from_settings(
            pusher=self.pusher_mock,
        )

    def test_send_push(self):
        '''Send single push - basic test.'''
        message = 'push message'
        self.push_manager.send_push(user_id=self.users[0].id, message=message)
        self.assertEqual(
            self.pusher_mock.send.mock_calls,
            [
                mock.call(self.users[0].uid, message=message),
            ]
        )

    def test_send_push_bad_user_id(self):
        '''user_id is not found in base'''
        message = 'push message'
        bad_user_id = str(uuid.UUID(int=42))
        with self.assertRaises(AssertionError):
            self.push_manager.send_push(user_id=bad_user_id, message=message)

    def test_send_push_to_batch_ok(self):
        '''Send push to multiple users - basic test.'''
        message = 'push message'
        ttl = 42
        self.pusher_mock.send_batch.return_value = {
            self.users[0].uid: [{'code': 200}, {'code': 200}],
            self.users[1].uid: {'code': 200}
        }
        result = self.push_manager.send_push_to_batch(
            user_ids=[self.users[0].id, self.users[1].id],
            message=message,
            ttl=ttl,
        )
        self.assertEqual(
            self.pusher_mock.send_batch.call_args_list,
            [
                mock.call(
                    uids=[self.users[0].uid, self.users[1].uid],
                    message=message,
                    ttl=ttl,
                    payload=None,
                )
            ]
        )
        self.assertEqual(result, {
            'bad_user_ids': [],
            'stat': {
                'any_device_delivered': 2,
                'subscribed_not_delivered': 0,
                'all_devices_delivered': 2,
                'status_codes_stat': {200: 3},
                'not_subscribed': 0
            },
            'need_resend_count': 0,
            'need_resend_url': None,
        })

    def test_send_push_to_batch_bad_user(self):
        '''user_id is not found in base'''
        message = 'push message'
        bad_user_id = str(uuid.UUID(int=42))
        self.pusher_mock.send_batch.return_value = {
            self.users[0].uid: {'code': 200}
        }
        result = self.push_manager.send_push_to_batch(
            user_ids=[self.users[0].id, bad_user_id],
            message=message,
        )
        self.assertEqual(
            self.pusher_mock.send_batch.call_args_list,
            [
                mock.call(
                    uids=[self.users[0].uid],
                    message=message,
                    ttl=None,
                    payload=None,
                )
            ]
        )
        self.assertEqual(result, {
            'bad_user_ids': [bad_user_id],
            'stat': {
                'any_device_delivered': 1,
                'subscribed_not_delivered': 0,
                'all_devices_delivered': 1,
                'status_codes_stat': {200: 1},
                'not_subscribed': 0
            },
            'need_resend_count': 0,
            'need_resend_url': None,
        })

    def test_send_push_to_batch_stat_device_delivered(self):
        '''Check any_device_delivered, all_devices_delivered params'''

        message = 'push message'
        self.pusher_mock.send_batch.return_value = {
            self.users[0].uid: [  # any device delivered
                {'code': status}
                for status in [202, 204, 205, 200, 200]
            ],
            self.users[1].uid: [  # any device delivered
                {'code': status}
                for status in [200, 202, 204, 205, 400, 403, 409, 429, 500, 502, 504]
            ],
            self.users[2].uid: [  # all devices delivered
                {'code': status}
                for status in [200]
            ],
            self.users[3].uid: [  # all devices delivered
                {'code': status}
                for status in [200, 200]
            ],
            self.users[4].uid: [  # all devices delivered
                {'code': status}
                for status in [200]
            ],
        }
        result = self.push_manager.send_push_to_batch(
            user_ids=[self.users[0].id, self.users[1].id, self.users[2].id, self.users[3].id],
            message=message,
        )
        self.assertEqual(result, {
            'bad_user_ids': [],
            'stat': {
                'any_device_delivered': 5,
                'subscribed_not_delivered': 0,
                'all_devices_delivered': 3,
                'status_codes_stat': {
                    200: 7,
                    202: 2, 204: 2, 205: 2,
                    400: 1, 403: 1, 409: 1, 429: 1, 500: 1, 502: 1, 504: 1,
                },
                'not_subscribed': 0
            },
            'need_resend_count': 0,
            'need_resend_url': None,
        })

    def test_send_push_to_batch_stat_subscribed(self):
        '''Check subscribed_not_delivered and not_subscribed params'''
        message = 'push message'
        self.pusher_mock.send_batch.return_value = {
            self.users[0].uid: [  # not subscribed
                {'code': status}
                for status in [204, 205]
            ],
            self.users[1].uid: [  # not subscribed
                {'code': status}
                for status in [204, 204]
            ],
            self.users[2].uid: [  # not subscribed
                {'code': status}
                for status in [205]
            ],
            self.users[3].uid: [  # subscribed not delivered
                {'code': status}
                for status in [202, 204, 205]
            ],
            self.users[4].uid: [  # delivered to at least one device
                {'code': status}
                for status in [200, 202, 204]
            ],
        }
        result = self.push_manager.send_push_to_batch(
            user_ids=[self.users[0].id, self.users[1].id, self.users[2].id, self.users[3].id],
            message=message,
        )
        self.assertEqual(result, {
            'bad_user_ids': [],
            'stat': {
                'any_device_delivered': 1,
                'subscribed_not_delivered': 1,
                'all_devices_delivered': 0,
                'status_codes_stat': {
                    200: 1, 202: 2, 204: 5, 205: 3,
                },
                'not_subscribed': 3
            },
            'need_resend_count': 0,
            'need_resend_url': None,
        })

    def test_send_push_to_batch_stat_need_resend(self):
        '''Check need_resend file'''
        message = 'push message'
        need_resend_codes = [400, 403, 429, 500, 502, 504]
        return_value = {
            self.users[0].uid: [  # no chance to resend
                {'code': status}
                for status in [202, 204, 205, 409]
            ],
            self.users[1].uid: [  # needs resend
                {'code': status}
                for status in [202] + need_resend_codes
            ],
            self.users[2].uid: [  # delivered to at least one device
                {'code': status}
                for status in [200] + need_resend_codes
            ],
        }
        return_value.update({  # all need resend
            self.users[3 + i].uid: [
                {'code': status}
                for status in [202, need_resend_codes[i]]
            ]
            for i in range(len(need_resend_codes))
        })
        self.pusher_mock.send_batch.return_value = return_value
        result = self.push_manager.send_push_to_batch(
            user_ids=[self.users[0].id, self.users[1].id, self.users[2].id, self.users[3].id],
            message=message,
        )
        result['stat'].pop('status_codes_stat')
        need_resend_url = result.pop('need_resend_url')
        self.assertIsNotNone(need_resend_url)
        self.assertTrue(need_resend_url.startswith('http'))
        need_resend_file_content = self.push_manager._mds_client.get_object_content(
            'carsharing-push-reports',
            need_resend_url.split('/')[-1]
        )
        self.assertEqual(
            set(need_resend_file_content.decode('ascii').split('\n')),
            set([
                str(self.users[1].id)
            ] + [
                str(self.users[3 + i].id)
                for i in range(len(need_resend_codes))
            ])
        )
        self.assertEqual(result, {
            'bad_user_ids': [],
            'stat': {
                'any_device_delivered': 1,
                'subscribed_not_delivered': len(need_resend_codes) + 2,
                'all_devices_delivered': 0,
                'not_subscribed': 0
            },
            'need_resend_count': len(need_resend_codes) + 1,
        })


class SendPushViewTestCase(AdminAPITestCase):

    '''Check logic through the view.'''

    def setUp(self):
        super().setUp()
        self.users = [UserFactory() for _ in range(20)]

    def test_send_push(self):
        message = 'push message'
        with mock.patch('cars.admin.core.send_push_manager.BasePusher')\
                as pusher_mock_class:
            pusher_mock = mock.MagicMock()
            pusher_mock_class.from_settings.return_value = pusher_mock
            url = reverse('cars-admin:user-push', kwargs={'user_id': self.users[0].id})

            resp = self.client.post(url, {'message': message}, format='json')
            self.assertEqual(resp.status_code, 200)

            self.assertEqual(
                pusher_mock.send.call_args_list,
                [
                    mock.call(
                        self.users[0].uid,
                        message=message,
                    )
                ]
            )

    def test_send_push_preset(self):
        message = 'push message'
        another_message = 'another push message'
        pp = PushPreset.objects.create(
            message=message,
        )
        PushPreset.objects.create(
            message=another_message,
        )

        with mock.patch('cars.admin.core.send_push_manager.BasePusher')\
                as pusher_mock_class:
            pusher_mock = mock.MagicMock()
            pusher_mock_class.from_settings.return_value = pusher_mock

            url = reverse('cars-admin:user-push-preset', kwargs={'user_id': self.users[0].id})

            resp = self.client.post(
                url,
                {'id': pp.id},
                format='json'
            )
            self.assertEqual(resp.status_code, 200)

            self.assertEqual(
                pusher_mock.send.call_args_list,
                [
                    mock.call(
                        self.users[0].uid,
                        message=message,
                    )
                ]
            )

    def test_send_push_list(self):
        '''Send push to multiple users - check view.'''
        message = 'push message'
        ttl = 42
        with mock.patch('cars.admin.core.send_push_manager.BasePusher')\
                as pusher_mock_class:
            pusher_mock = mock.MagicMock()
            pusher_mock_class.from_settings.return_value = pusher_mock

            pusher_mock.send_batch.return_value = {
                self.users[0].uid: [{'code': 200}, {'code': 200}],
                self.users[1].uid: {'code': 200}
            }
            url = reverse('cars-admin:user-list-push')
            resp = self.client.post(url, {
                'user_ids_file': io.StringIO('{}\n{}'.format(self.users[0].id, self.users[1].id)),
                'message': message,
                'ttl': ttl,
            })
            self.assertEqual(resp.status_code, 200)

            result = resp.json()
            self.assertEqual(
                pusher_mock.send_batch.call_args_list,
                [
                    mock.call(
                        uids=[self.users[0].uid, self.users[1].uid],
                        message=message,
                        ttl=ttl,
                        payload=None,
                    )
                ]
            )
            self.assertEqual(result, {
                'bad_user_ids': [],
                'stat': {
                    'any_device_delivered': 2,
                    'subscribed_not_delivered': 0,
                    'all_devices_delivered': 2,
                    'status_codes_stat': {'200': 3},
                    'not_subscribed': 0
                },
                'need_resend_count': 0,
                'need_resend_url': None,
            })

    def test_send_push_preset_list(self):
        message = 'push message'
        another_message = 'another push message'
        pp = PushPreset.objects.create(
            message=message,
        )
        PushPreset.objects.create(
            message=another_message,
        )

        with mock.patch('cars.admin.core.send_push_manager.BasePusher')\
                as pusher_mock_class:
            pusher_mock = mock.MagicMock()
            pusher_mock_class.from_settings.return_value = pusher_mock

        ttl = 42
        with mock.patch('cars.admin.core.send_push_manager.BasePusher')\
                as pusher_mock_class:
            pusher_mock = mock.MagicMock()
            pusher_mock_class.from_settings.return_value = pusher_mock

            pusher_mock.send_batch.return_value = {
                self.users[0].uid: [{'code': 200}, {'code': 200}],
                self.users[1].uid: {'code': 200}
            }
            url = reverse('cars-admin:user-list-push-preset')
            resp = self.client.post(url, {
                'user_ids_file': io.StringIO('{}\n{}'.format(self.users[0].id, self.users[1].id)),
                'id': pp.id,
                'ttl': ttl,
            })
            self.assertEqual(resp.status_code, 200)

            result = resp.json()
            self.assertEqual(
                pusher_mock.send_batch.call_args_list,
                [
                    mock.call(
                        uids=[self.users[0].uid, self.users[1].uid],
                        message=message,
                        ttl=ttl,
                        payload=None,
                    )
                ]
            )
            self.assertEqual(result, {
                'bad_user_ids': [],
                'stat': {
                    'any_device_delivered': 2,
                    'subscribed_not_delivered': 0,
                    'all_devices_delivered': 2,
                    'status_codes_stat': {'200': 3},
                    'not_subscribed': 0
                },
                'need_resend_count': 0,
                'need_resend_url': None,
            })
