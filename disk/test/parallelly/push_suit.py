# -*- coding: utf-8 -*-
import json
from urllib2 import HTTPError

import mock
import requests

from nose_parameterized import parameterized

import mpfs.common.errors as errors
from mpfs.common.util import from_json, to_json
from mpfs.core.pushnotifier.controller import PushController
from mpfs.core.services.passport_service import passport
from mpfs.core.services.push_service import (
    XivaSendService, XivaSubscribeCommonService
)
from mpfs.core.user.constants import PUSH_TRANSLATIONS
from test.base import DiskTestCase, MpfsBaseTestCase
from test.helpers.stubs.manager import StubsManager
from test.helpers.stubs.services import PushServicesStub


class PushesOverXiva(DiskTestCase):
    """
    Тесты общих механизмов пушей.

    Сюда не надо добавлять тесты проверки конкретных пушей.
    Напр.: при перемещении ресурса должен отсылаться пуш-diff
    """
    web_callback_url = 'http://xivahub.mail.yandex.net:80/notify/disk-json-test'
    get_tinypush_callback_url_1 = 'http://push03g.disk.yandex.net:8080/rpc/yadrop_xmpppush/1'
    get_tinypush_callback_url_2 = 'http://push03g.disk.yandex.net:8080/rpc/yadrop_xmpppush/2'

    stubs_manager = StubsManager(method_stubs=set(StubsManager.DEFAULT_METHOD_STUBS) - {PushServicesStub})

    def test_desktop_subscribe(self):
        with PushServicesStub() as push_stub:
            self.service_ok('xiva_subscribe', {'uid': self.uid, 'callback': self.get_tinypush_callback_url_1})
            # положили подписку в Xiva
            args, _ = push_stub.subscribe_url.call_args_list[-1]
            assert args == (self.uid, self.get_tinypush_callback_url_1)

    def test_mobile_subs_without_filter(self):
        """
        Тестируем поведение ручки `mobile_subscribe` без фильтрации по ресурсам
        """
        events = {'share_invite_new', 'space_is_low', 'space_is_full', 'photoslice_updated', 'diff'}
        # подписка без фильтрации по ресурсам
        with PushServicesStub() as push_stub:
            self.service_ok('mobile_subscribe', {'uid': self.uid, 'token': 'test_token', 'allow': ','.join(events)})
            # подписали фотосрез в xiva
            assert push_stub.subscribe_app.called
            args, kwargs = push_stub.subscribe_app.call_args_list[-1]
            assert args[0] == self.uid
            assert args[1] == 'test_token'
            photoslice_xiva_filter = {
                'rules': [
                    {'do': 'send_bright', 'if': 'CommonEvents'},
                    {'do': 'skip'}
                ],
                'vars': {
                    'CommonEvents': {'$event': list(events)}
                }
            }
            assert kwargs['xiva_filter'] == photoslice_xiva_filter

    def test_mobile_subs_with_filter(self):
        """
        Тестируем поведение ручеки `mobile_subscribe` с фильтрацией по ресурсам
        """
        events = {'share_invite_new', 'space_is_low', 'space_is_full', 'photoslice_updated', 'diff'}
        with PushServicesStub() as push_stub:
            self.service_ok('mobile_subscribe', {'uid': self.uid, 'token': 'test_token', 'allow': ','.join(events), 'resources': '1,2,3'})
            # подписали фотосрез в xiva
            assert push_stub.subscribe_app.called
            args, kwargs = push_stub.subscribe_app.call_args_list[-1]
            assert args[0] == self.uid
            assert args[1] == 'test_token'
            events_no_diff = events.copy()
            events_no_diff.remove('diff')
            photoslice_xiva_filter = {
                'vars': {
                    "DiffEvent": {"$event": ["diff"]},
                    "RequiredFileIds": {"file_ids": {"$has": ["1", "3", "2"]}},
                    "CommonEvents": {"$event": list(events_no_diff)},
                },
                'rules': [
                    {"if": "DiffEvent & RequiredFileIds", "do": "send_bright"},
                    {"if": "CommonEvents", "do": "send_bright"},
                    {"do": "skip"},
                ],
            }
            assert kwargs['xiva_filter'] == photoslice_xiva_filter

    def test_mobile_subs_with_limit_exceeded(self):
        """
        Тестируем поведение ручки `mobile_subscribe` с превышением лимита Xiva
        """
        events = {'share_invite_new', 'space_is_low', 'space_is_full', 'photoslice_updated', 'diff'}
        token = to_json({
            'c': 'app_name',
            'p': 'w',
            't': 'push_token',
            'd': 'uuid',
        })

        def patched_request(*args, **kwargs):
            if kwargs['relative_url'] == XivaSubscribeCommonService.subscribe_app_path:
                raise HTTPError(kwargs['relative_url'], 400, 'filter size limit exceeded', None, None)
            return None

        with mock.patch('mpfs.core.services.common_service.RequestsPoweredServiceBase.request',
                        side_effect=patched_request):
            self.service_error(
                'mobile_subscribe',
                {'uid': self.uid, 'token': token, 'allow': ','.join(events), 'resources': '1,2,3'},
                code=errors.XivaLimitExceeded.code,
            )

    def test_mobile_subs_unregistered_app(self):
        """
        Тестируем поведение ручки `mobile_subscribe` с неизвестным приложением
        """
        token = to_json({
            'c': 'unregistered.app.name',
            'p': 'w',
            't': 'push_token',
            'd': 'uuid',
        })

        def patched_request(*args, **kwargs):
            if kwargs['relative_url'] == XivaSubscribeCommonService.subscribe_app_path:
                raise HTTPError(kwargs['relative_url'], 400, 'application unregistered.app.name for platform fcm is not registered', None, None)
            return None

        with mock.patch('mpfs.core.services.common_service.RequestsPoweredServiceBase.request',
                        side_effect=patched_request):
            self.service_error(
                'mobile_subscribe',
                {'uid': self.uid, 'token': token, 'allow': 'share_invite_new'},
                code=errors.XivaAppNotRegistered.code,
            )

    def test_mobile_subs_invalid_characters_in_token(self):
        """
        Тестируем поведение ручки `mobile_subscribe` с некорректными символами
        """
        token = to_json({
            'c': 'app_name',
            'p': 'w',
            't': 'push_token',
            'd': 'invalid_characters',
        })

        def patched_request(*args, **kwargs):
            if kwargs['relative_url'] == XivaSubscribeCommonService.subscribe_app_path:
                raise HTTPError(kwargs['relative_url'], 400, 'invalid characters in argument "uuid"', None, None)
            return None

        with mock.patch('mpfs.core.services.common_service.RequestsPoweredServiceBase.request',
                        side_effect=patched_request):
            self.service_error(
                'mobile_subscribe',
                {'uid': self.uid, 'token': token, 'allow': 'share_invite_new'},
                code=errors.XivaBadToken.code,
            )

    def test_mobile_unsubs(self):
        """
        Тестируем поведение ручеки `mobile_unsubscribe`
        """
        events = {'share_invite_new', 'space_is_low', 'space_is_full', 'photoslice_updated', 'diff'}
        with PushServicesStub() as push_stub:
            self.service_ok('mobile_subscribe', {'uid': self.uid, 'token': 'test_token', 'allow': ','.join(events), 'resources': '1,2,3'})
            self.service_ok('mobile_unsubscribe', {'uid': self.uid, 'token': 'test_token'})
            # Отписываемся в Xiva
            assert push_stub.unsubscribe_app.called
            args, _ = push_stub.unsubscribe_app.call_args_list[-1]
            assert args[0] == self.uid
            assert args[1] == 'test_token'

    def test_sending_events(self):
        """
        Тестируем логику отсылки сообщений: mpfs.core.pushnotifier.controller.PushController().send
        """
        with PushServicesStub() as push_stub:
            # подписываем приложения через новую ручку
            self.service_ok('mobile_subscribe', {'uid': self.uid, 'token': 'first_token', 'allow': 'share_invite_new,space_is_low,diff'})
            # подписываем ПО
            self.service_ok('async_xiva_subscribe', {'uid': self.uid, 'callback': self.get_tinypush_callback_url_1})

            diff_send_data = {
                'new_version': '1459333204914614',
                'uid': self.uid,
                'old_version': 1459333203903440,
                'action_name': 'diff',
                'xiva_data': [
                    {
                        'key': '/disk/top',
                        'resource_type': 'dir',
                        'fid': 'cf8393b4adc6a314156dce200feac1afbccd13399eec737b9117cd7b55835cad',
                        'op': 'new'
                    }
                ],
                'class': 'diff',
                'connection_id': '1234'
            }
            PushController().send(diff_send_data)

            to_xiva_pushes = push_stub.send.call_args_list
            assert len(to_xiva_pushes) == 1
            args, kwargs = to_xiva_pushes[0]
            assert args[0] == self.uid
            assert args[1] == 'diff'
            body = json.loads(args[2])
            assert body['root']['tag'] == 'diff'
            assert kwargs['connection_id'] == diff_send_data['connection_id']
            assert kwargs['keys'] == {'file_ids': [diff_send_data['xiva_data'][0]['fid']]}

    def test_share_invite_new_localized_msg(self):
        """
        Тестируем, что ивент share_invite_new содержит поле localized_msg, когда пуш отправлен приглашенному,
        и нет иначе
        """
        with PushServicesStub() as stub:
            from mpfs.common.static.tags.push import SHARE_INVITE_NEW

            # Случай, когда отправляем уведомление подписвавшемуся
            owner_name = passport.public_userinfo(self.uid)['public_name']
            dir_name = 'my_dir'
            xiva_data = {
                'root': {'tag': 'share', 'parameters': {'type': 'invite_new', 'for': 'actor'}},
                'values': [
                    {
                        'tag': 'owner',
                        'value': '',
                        'parameters': {
                            'uid': self.uid,
                            'name': owner_name
                        }
                    },
                    {
                        'tag': 'folder',
                        'value': '',
                        'parameters': {
                            'name': dir_name,
                            'rights': 'read',
                            'hash': '123'
                        }
                    }
                ]
            }
            data = {
                'class': SHARE_INVITE_NEW,
                'uid': self.uid,
                'new_version': 1,
                'xiva_data': xiva_data,
                'connection_id': '',
                'operation': 'action'
            }
            send_mock = stub.send
            PushController().send(data)
            send_args, send_kwargs = send_mock.call_args
            assert 'keys' in send_kwargs
            assert 'localized_msg' in send_kwargs['keys']
            user_locale = passport.public_userinfo(self.uid)['locale']
            correct_translation = PUSH_TRANSLATIONS['share_invite_new'][user_locale] % {
                'owner': owner_name, 'folder': dir_name}
            assert send_kwargs['keys']['localized_msg'] == correct_translation

            # Случай, когда отправляем уведомелние владельцу
            xiva_data = {
                'root': {'tag': 'share', 'parameters': {'type': 'invite_new', 'for': 'owner'}},
                'values': [
                    {
                        'tag': 'user',
                        'value': '',
                        'parameters': {
                            'universe_login': '',
                            'universe_service': '',
                            'first_invitation': 0
                        }
                    },
                    {
                        'tag': 'folder',
                        'value': '',
                        'parameters': {
                            'name': dir_name,
                            'path': '/',
                            'gid': 0,
                            'rights': ''
                        }
                    }
                ]
            }
            data = {
                'class': SHARE_INVITE_NEW,
                'uid': self.uid,
                'new_version': 1,
                'xiva_data': xiva_data,
                'connection_id': '',
                'operation': 'action'
            }

            PushController().send(data)
            send_args, send_kwargs = send_mock.call_args
            assert 'keys' in send_kwargs
            assert not send_kwargs['keys']

    def test_is_low_localized_msg(self):
        """
        Тестируем, что ивент, space_is_low содержит поле localized_msg
        """
        with PushServicesStub() as stub:
            from mpfs.common.static.tags.push import SPACE_IS_LOW
            free = 2 * 20
            xiva_data = {
                'root': {
                    'tag': 'space',
                    'parameters': {
                        'type': 'is_low',
                        'limit': free * 2,
                        'free': free,
                        'used': free
                    }
                },
                'values': []
            }
            data = {
                'class': SPACE_IS_LOW,
                'uid': self.uid,
                'new_version': 1,
                'xiva_data': xiva_data,
                'connection_id': '',
                'operation': 'action',
                'action_name': 'space'
            }
            send_mock = stub.send
            PushController().send(data)
            send_args, send_kwargs = send_mock.call_args
            assert 'keys' in send_kwargs
            assert 'localized_msg' in send_kwargs['keys']
            user_locale = passport.public_userinfo(self.uid)['locale']
            correct_translation = PUSH_TRANSLATIONS['space_is_low'][user_locale] % {'free': str(free / (2 ** 20))}
            assert send_kwargs['keys']['localized_msg'] == correct_translation

    def test_is_full_localized_msg(self):
        """
        Тестируем, что ивент space_is_full содержит поле localized_msg
        """
        with PushServicesStub() as stub:
            from mpfs.common.static.tags.push import SPACE_IS_FULL
            xiva_data = {
                'root': {
                    'tag': 'space',
                    'parameters': {
                        'type': 'is_full',
                        'limit': 1000,
                        'free': 0,
                        'used': 1000
                    }
                },
                'values': []
            }
            data = {
                'class': SPACE_IS_FULL,
                'uid': self.uid,
                'new_version': 1,
                'xiva_data': xiva_data,
                'connection_id': '',
                'operation': 'action',
                'action_name': 'space'
            }
            send_mock = stub.send
            PushController().send(data)
            send_args, send_kwargs = send_mock.call_args
            assert 'keys' in send_kwargs
            assert 'localized_msg' in send_kwargs['keys']
            user_locale = passport.public_userinfo(self.uid)['locale']
            assert send_kwargs['keys']['localized_msg'] == PUSH_TRANSLATIONS['space_is_full'][user_locale]

    def test_no_localized_msg(self):
        """
        Тестируем, что не появляются доволнительные ключи в других иветнах
        """
        with PushServicesStub() as stub:
            data = {
                'new_version': '1459333204914614',
                'uid': self.uid,
                'old_version': 1459333203903440,
                'action_name': 'diff',
                'xiva_data': [
                    {
                        'key': '/disk/top',
                        'resource_type': 'dir',
                        'fid': 'cf8393b4adc6a314156dce200feac1afbccd13399eec737b9117cd7b55835cad',
                        'op': 'new'
                    }
                ],
                'class': 'diff',
                'connection_id': '1234'
            }
            send_mock = stub.send
            PushController().send(data)
            send_args, send_kwargs = send_mock.call_args
            payload = json.loads(send_args[2])
            assert 'localized_msg' not in payload

    def test_xiva_send(self):
        """
        Тестируем XivaSendService.send()
        """

        with mock.patch('requests.sessions.Session.send') as req:
            XivaSendService().send('112', 'diff', {}, keys={'c': 4})
            headers = req.call_args[0][0].headers
            data = from_json(req.call_args[0][0].body)
            res_body = {'keys': {'c': 4}, 'payload': {}}

            assert not data.viewkeys() ^ res_body.viewkeys()
            # assert 'X-Request-Timeout' in headers
            # assert int(headers['X-Request-Timeout']) == int(XivaSendService().timeout) * 1000
            assert 'X-Request-Attempt' in headers
            assert 'X-Request-Id' in headers

    def test_repack_params(self):
        with PushServicesStub() as push_stub:
            data = {
                'uid': self.uid,
                'class': 'album_deltas_updated',
                'xiva_data': {
                    't': 'datasync_database_changed',
                    'context': 'app',
                    'database_id': 'albums',
                    'revision': 123,
                    'r': self.uid,
                },
                'repack': {
                    'test': 123
                }
            }
            PushController().send(data)
            args, kwargs = push_stub.send.call_args
            assert 'repack' in kwargs
            assert kwargs['repack'] is not None

    def test_repack_params_absence(self):
        with PushServicesStub() as push_stub:
            data = {
                'uid': self.uid,
                'class': 'album_deltas_updated',
                'xiva_data': {
                     't': 'datasync_database_changed',
                     'context': 'app',
                     'database_id': 'albums',
                     'revision': 123,
                     'r': self.uid,
                }
            }
            PushController().send(data)
            args, kwargs = push_stub.send.call_args
            assert 'repack' in kwargs
            assert kwargs['repack'] is None
