# -*- coding: utf-8 -*-

import json
import mock
import contextlib

from contextlib import contextmanager
from hamcrest import (
    any_of,
    assert_that,
    equal_to,
    has_entries,
    has_item,
    instance_of,
)
from httplib import OK, NOT_FOUND, FORBIDDEN, UNAUTHORIZED
from nose_parameterized import parameterized

from test.parallelly.api.disk.base import DiskApiTestCase
from test.base_suit import UploadFileTestCaseMixin, UserTestCaseMixin, SupportApiTestCaseMixin
from test.fixtures.users import user_3, default_user
from test.helpers.stubs.services import VideoStreamingStub, PassportStub
from mpfs.common.static import tags

from mpfs.common.errors.platform import MpfsProxyBadResponse
from mpfs.common.static import codes
from mpfs.common.util import from_json
from mpfs.platform.v1.disk.handlers import ListVideoStreamsHandler, ListPublicVideoStreamsHandler
from mpfs.platform.permissions import AllowByClientIdPermission
from mpfs.core.services.mpfsproxy_service import MpfsProxy, mpfsproxy
from mpfs.core.services.passport_service import Passport
from mpfs.core.services.video_service import video
from mpfs.platform.v1.disk import handlers


class DiskVideoTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL

    mpfs_file_path = '/disk/IoTTools_high.mp4'
    api_file_path = 'disk:/IoTTools_high.mp4'

    client_id = '12345678901234567890123456789012'
    url = 'disk/video/streams'

    x_forwarded_for = '1.1.1.1'
    x_real_ip = '2.2.2.2'
    ip = '3.3.3.3'

    def setup_method(self, method):
        super(DiskVideoTestCase, self).setup_method(method)
        self.create_user(self.uid)
        self.upload_file(self.uid, self.mpfs_file_path)

        with open('fixtures/json/new_video_streaming_info_response.json') as fd:
            self.mpfs_response = json.dumps(json.loads(fd.read()))
        with open('fixtures/json/new_video_streaming_info_response.json') as fd:
            self.video_response = json.dumps(json.loads(fd.read()))

    def _test_mocks(self):
        real_open_url = MpfsProxy.open_url

        def fake_mpfsproxy_open_url(_self, url, *args, **kwargs):
            if 'video_url' in url or 'video_streams' in url:
                return 200, self.mpfs_response, {}
            else:
                return real_open_url(_self, url, *args, **kwargs)

        return contextlib.nested(
            self.specified_client(id=self.client_id),
            mock.patch.object(ListVideoStreamsHandler, 'permissions', AllowByClientIdPermission([self.client_id])),
            mock.patch.object(MpfsProxy, 'open_url', fake_mpfsproxy_open_url),
            mock.patch.object(video, 'open_url', return_value=(200, self.video_response, {}))
        )

    def _video_mocks(self):
        return contextlib.nested(
            self.specified_client(id=self.client_id),
            mock.patch.object(ListVideoStreamsHandler, 'permissions', AllowByClientIdPermission([self.client_id])),
            mock.patch.object(handlers, 'PLATFORM_VIDEO_NEW_PERCENTAGE', 99),
            VideoStreamingStub()
        )

    def test_fields(self):
        with self._test_mocks():
            resp = self.client.get(self.url, query={'path': self.mpfs_file_path})
            resp_content = from_json(resp.content)
            assert 'duration' in resp_content
            assert 'stream_id' in resp_content
            assert 'items' in resp_content

    def test_adaptive_item(self):
        adaptive_link_tmpl = '/hls-playlist/%s/master-playlist.m3u8'
        adaptive_item_fields = {'resolution', 'links', 'container', 'audio_codec', 'video_codec'}
        with self._test_mocks():
            resp = self.client.get(self.url, query={'path': self.api_file_path})
            resp_content = from_json(resp.content)
            adaptive_items = [i for i in resp_content['items'] if i['resolution'] == 'adaptive']
            assert len(adaptive_items) == 1
            adaptive_item = adaptive_items[0]
            assert 'links' in adaptive_item
            assert not adaptive_item_fields ^ adaptive_item.viewkeys()
            for link in adaptive_item['links'].itervalues():
                assert link.endswith(adaptive_link_tmpl % resp_content['stream_id'])

    def test_call_new_video_streaming(self):
        with self._video_mocks() as mocks:
            resp = self.client.get(self.url, query={'path': self.api_file_path})
            assert resp.status_code == 200
            mocks[-1].get_video_info.assert_called_once()

    @parameterized.expand([
        ('x_real_ip', {'X-Real-IP': x_real_ip}, x_real_ip),
        ('x_forwarded_for', {'X-Forwarded-For': x_forwarded_for}, x_forwarded_for),
        ('both_headers_priority', {'X-Real-IP': x_real_ip, 'X-Forwarded-For': x_forwarded_for}, x_real_ip),
        ('external_ignore', {'X-Real-IP': x_real_ip, 'X-Forwarded-For': x_forwarded_for}, ip, tags.platform.EXTERNAL),
    ])
    def test_call_video_streaming_ips(self, test_name, headers, result_ip, api_mode=tags.platform.INTERNAL):
        self.api_mode = api_mode
        with self._video_mocks() as mocks:
            self.client.get(self.url, query={'path': self.api_file_path}, headers=headers, ip=self.ip)
            video_info = mocks[-1].get_video_info
            video_info.assert_called_once()
            assert video_info.call_args[1]['user_ip'] == result_ip

    def test_client_id_forwarded_to_mpfs(self):
        with mock.patch.object(ListVideoStreamsHandler, 'permissions', AllowByClientIdPermission([self.client_id])), \
                self.specified_client(id=self.client_id), \
                mock.patch.object(handlers, 'PLATFORM_VIDEO_NEW_PERCENTAGE', 100), \
                VideoStreamingStub() as stub:
            self.client.get(self.url, query={'path': self.api_file_path})
            assert stub.get_video_info.call_args[1]['client_id'] == self.client_id

    def test_attaching_video_to_disk(self):
        self.upload_file(self.uid, '/attach/test.mp4')
        attach_path = self.json_ok('list', {'uid': self.uid, 'path': '/attach'})[1]['path']
        name = attach_path.rsplit('/', 1)[-1]
        with self._video_mocks() as mocks:
            resp = self.client.get(self.url, query={'path': 'attach:/%s' % name})
            assert resp.status_code == 200
            mocks[-1].get_video_info.assert_called_once()

    def test_mpfs_response_unprocessable_entity(self):
        real_open_url = MpfsProxy.open_url
        error = MpfsProxyBadResponse(data={'code': codes.VIDEO_STREAMING_UNPROCESSABLE_ENTITY})
        error.status_code = 422

        def fake_mpfsproxy_open_url(_self, url, *args, **kwargs):
            if 'video_url' in url or 'video_streams' in url:
                raise error
            else:
                return real_open_url(_self, url, *args, **kwargs)

        with mock.patch.object(MpfsProxy, 'open_url', fake_mpfsproxy_open_url), \
             self.specified_client(id=self.client_id), \
             mock.patch.object(ListVideoStreamsHandler, 'permissions', AllowByClientIdPermission([self.client_id])):
            resp = self.client.get(self.url, query={'path': self.mpfs_file_path})
            resp.status_code == 422


class ListPublicVideoStreamsHandlerTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, SupportApiTestCaseMixin,
                                            DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    endpoint = 'disk/public/video/streams'
    client_id = '12345678901234567890123456789012'

    def _upload_public_video_ang_get_hash(self):
        self.upload_file(self.uid, '/disk/test.mp4', media_type='video')
        result = self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/test.mp4'})
        return result['hash']

    def test_correct_request_without_errors(self):
        """Проверить запрос, в котором не возникает никаких ошибок."""
        self.create_user(self.uid)
        public_hash = self._upload_public_video_ang_get_hash()

        with open('fixtures/json/new_video_streaming_info_response.json') as f, \
             mock.patch.object(mpfsproxy, 'open_url', return_value=(200, json.dumps(json.loads(f.read())), {})), \
             self.specified_client(id=self.client_id), \
             mock.patch.object(ListPublicVideoStreamsHandler, 'permissions', AllowByClientIdPermission([self.client_id])):
            response = self.client.get(self.endpoint, {'public_key': public_hash})
            assert response.status_code == 200
            data = json.loads(response.content)
            assert 'duration' in data
            assert 'items' in data
            assert 'total' in data

    def test_request_with_social_account(self):
        self.create_user(self.uid)
        public_hash = self._upload_public_video_ang_get_hash()

        with open('fixtures/json/new_video_streaming_info_response.json') as f, \
                mock.patch.object(mpfsproxy, 'open_url', return_value=(200, json.dumps(json.loads(f.read())), {})), \
                self.specified_client(id=self.client_id, uid='415264318'), \
                mock.patch.object(ListPublicVideoStreamsHandler, 'permissions', AllowByClientIdPermission([self.client_id])), \
                PassportStub() as stub:
            stub.subscribe.side_effect = Passport.errors_map['accountwithpasswordrequired']
            response = self.client.get(self.endpoint, {'public_key': public_hash})
            assert response.status_code == 200
            data = json.loads(response.content)
            assert 'duration' in data
            assert 'items' in data
            assert 'total' in data

    def test_request_without_uid(self):
        self.create_user(self.uid)
        public_hash = self._upload_public_video_ang_get_hash()

        with open('fixtures/json/new_video_streaming_info_response.json') as f, \
                mock.patch.object(mpfsproxy, 'open_url', return_value=(200, json.dumps(json.loads(f.read())), {})), \
                self.specified_client(id=self.client_id, uid=None), \
                mock.patch.object(ListPublicVideoStreamsHandler, 'permissions', AllowByClientIdPermission([self.client_id])):
            response = self.client.get(self.endpoint, {'public_key': public_hash})
            assert response.status_code == 200
            data = json.loads(response.content)
            assert 'duration' in data
            assert 'items' in data
            assert 'total' in data

    def test_request_without_authorization(self):
        self.create_user(self.uid)
        public_hash = self._upload_public_video_ang_get_hash()

        with open('fixtures/json/new_video_streaming_info_response.json') as f, \
                mock.patch.object(video, 'open_url', return_value=(200, json.dumps(json.loads(f.read())), {})):
            response = self.client.get(self.endpoint, {'public_key': public_hash})
            assert response.status_code == 401

    @parameterized.expand([
        ('Yandex.Disk {"os":"android 7.0","device":"phone","src":"disk.mobile","vsn":"3.20-0","id":"5281b1fc6bd9b022f1b6969508ebaa57"}', True),
        ('Yandex.Disk {"os":"iOS","src":"disk.mobile","vsn":"2.14.7215","id":"E9C69BDA-0837-4867-A9B0-3AFCAAC3342A","device":"tablet"}', True),
        ('Google Chrome', False),
    ])
    def test_request_with_user_agent(self, ua, is_allowed):
        self.create_user(self.uid)
        public_hash = self._upload_public_video_ang_get_hash()

        with open('fixtures/json/new_video_streaming_info_response.json') as f, \
                mock.patch.object(mpfsproxy, 'open_url', return_value=(200, json.dumps(json.loads(f.read())), {})):
            response = self.client.get(self.endpoint, {'public_key': public_hash}, headers={'User-Agent': ua})
            if is_allowed:
                assert response.status_code == 200
                data = json.loads(response.content)
                assert 'duration' in data
                assert 'items' in data
                assert 'total' in data
            else:
                assert response.status_code == 401

    @parameterized.expand([
        ('https://disk.yandex.ru', True),
        ('https://yandex.com', True),
        ('https://yandex.com.tr', True),
        ('https://disk.yandex.com.tr', True),
        ('https://yadi.sk', True),
        ('https://sub.yadi.sk', True),
        ('https://fakeyandex.ru', False),
        ('https://fakeyadi.sk', False),
        ('https://google.com', False),
        ('https://drive.google.com', False),
    ])
    def test_request_with_origin(self, origin, is_allowed):
        self.create_user(self.uid)
        public_hash = self._upload_public_video_ang_get_hash()

        with open('fixtures/json/new_video_streaming_info_response.json') as f, \
                mock.patch.object(mpfsproxy, 'open_url', return_value=(200, json.dumps(json.loads(f.read())), {})):
            response = self.client.get(self.endpoint, {'public_key': public_hash}, headers={'Origin': origin})
            if is_allowed:
                assert response.status_code == 200
                data = json.loads(response.content)
                assert 'duration' in data
                assert 'items' in data
                assert 'total' in data
            else:
                assert response.status_code == 401

    def test_allow_access_via_scopes(self):
        self.create_user(self.uid)
        public_hash = self._upload_public_video_ang_get_hash()

        with open('fixtures/json/new_video_streaming_info_response.json') as f, \
             mock.patch.object(mpfsproxy, 'open_url', return_value=(200, json.dumps(json.loads(f.read())), {})), \
             self.specified_client(id=self.client_id, scopes=['cloud_api:disk.video.public', ]):
            response = self.client.get(self.endpoint, {'public_key': public_hash})
            assert response.status_code == 200

    def test_request_for_blocked(self):
        """Проверить запрос на заблокированный ресурс."""
        self.create_user(self.uid)
        public_hash = self._upload_public_video_ang_get_hash()

        self.support_ok('block_public_file', {
            'moderator': 'moderator',
            'comment': 'comment',
            'private_hash': public_hash,
            'type': 'block_file',
            'view': 'st',
            'link': 'https://rkn.gov.ru/',
            'notify': 0,
        })

        with self.specified_client(id=self.client_id):
            with mock.patch.object(ListPublicVideoStreamsHandler, 'permissions', AllowByClientIdPermission([self.client_id])):
                response = self.client.get(self.endpoint, {'public_key': public_hash})
                # при блокировки корня публичной ссылки убирается симлинк, поэтому в mpfs
                # будет ошибка ResourceNotFound, то есть 404
                assert response.status_code == 404

    def test_request_for_non_existing(self):
        """Проверить запрос на несуществующий ресурс."""
        self.create_user(self.uid)
        with self.specified_client(id=self.client_id):
            with mock.patch.object(ListPublicVideoStreamsHandler, 'permissions', AllowByClientIdPermission([self.client_id])):
                non_existing_resource_public_hash = 'GWbmDBKuXDgJs+WS9EmMMQBZcK1PKKxi7xfAPcEExKE='
                response = self.client.get(self.endpoint, {'public_key': non_existing_resource_public_hash})
                assert response.status_code == 404

    def test_call_new_video_streaming(self):
        self.create_user(self.uid)
        public_hash = self._upload_public_video_ang_get_hash()
        with self.specified_client(id=self.client_id):
            with mock.patch.object(ListPublicVideoStreamsHandler, 'permissions', AllowByClientIdPermission([self.client_id])):
                with mock.patch.object(handlers, 'PLATFORM_VIDEO_NEW_PERCENTAGE', 100):
                    with VideoStreamingStub() as stub:
                        resp = self.client.get(self.endpoint, query={'public_key': public_hash})
                        assert resp.status_code == 200
                        stub.get_video_info.assert_called_once()

    def test_client_id_forwarded_to_mpfs(self):
        self.create_user(self.uid)
        public_hash = self._upload_public_video_ang_get_hash()
        with mock.patch.object(ListPublicVideoStreamsHandler, 'permissions', AllowByClientIdPermission([self.client_id])), \
                self.specified_client(id=self.client_id), \
                mock.patch.object(handlers, 'PLATFORM_VIDEO_NEW_PERCENTAGE', 100), \
                VideoStreamingStub() as stub:
            self.client.get(self.endpoint, query={'public_key': public_hash})
            assert stub.get_video_info.call_args[1]['client_id'] == self.client_id

    def test_mpfs_response_unprocessable_entity(self):
        self.create_user(self.uid)
        public_hash = self._upload_public_video_ang_get_hash()
        error = MpfsProxyBadResponse(data={'code': codes.VIDEO_STREAMING_UNPROCESSABLE_ENTITY})
        error.status_code = 422

        with mock.patch.object(MpfsProxy, 'open_url', side_effect=error), \
             self.specified_client(id=self.client_id):
            resp = self.client.get(self.endpoint, query={'public_key': public_hash})
            resp.status_code == 422


class ListPublicVideoStreamsFromAlbumTestCase(DiskApiTestCase,
                                              UploadFileTestCaseMixin,
                                              UserTestCaseMixin,
                                              SupportApiTestCaseMixin):
    api_mode = tags.platform.EXTERNAL
    endpoint = 'disk/public/video/streams'
    client_id = '12345678901234567890123456789012'

    VIDEO_PATH = '/disk/video_with_a_raccoon.mp4'

    ALBUM_CREATE_JSON = {'title': 'MyAlbum',
                         'description': 'My Test Album',
                         'layout': 'rows',
                         'flags': ['show_frames'],
                         'items': [{'type': 'resource', 'path': VIDEO_PATH}]}

    def setup_method(self, method):
        super(ListPublicVideoStreamsFromAlbumTestCase, self).setup_method(method)
        self.create_user(self.uid)
        self.upload_file(self.uid, self.VIDEO_PATH)
        self.album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=self.ALBUM_CREATE_JSON)

    @contextmanager
    def mock_for_successful_video_streaming_response(self):
        with open('fixtures/json/video_info_response.json') as f, \
                mock.patch.object(video, 'open_url', return_value=(200, json.dumps(json.loads(f.read())), {})), \
                open('fixtures/json/new_video_streaming_info_response.json') as new_video_streaming_response, \
                mock.patch('mpfs.core.services.video_service.VideoStreaming.open_url',
                           return_value=json.dumps(json.loads(new_video_streaming_response.read()))), \
                mock.patch.object(ListPublicVideoStreamsHandler, 'permissions',
                                  AllowByClientIdPermission([self.client_id])):

            yield

    @parameterized.expand([
        ('new_video_streaming_and_owner', 100, default_user.uid),
        ('old_video_streaming_and_owner', 0, default_user.uid),
        ('new_video_streaming_and_not_owner', 100, user_3.uid),
        ('old_video_streaming_and_not_owner', 0, user_3.uid),
        ('new_video_streaming_and_no_user', 100, None),
        ('old_video_streaming_and_no_user', 0, None),
    ])
    def test_video_from_album_and(self, case_name, new_video_users_percentage, request_uid):
        public_key = self.album['public']['public_key']
        item_id = self.album['items'][0]['id']

        with self.mock_for_successful_video_streaming_response(), \
                self.specified_client(id=self.client_id, uid=request_uid), \
                mock.patch('mpfs.platform.v1.disk.handlers.PLATFORM_VIDEO_NEW_PERCENTAGE', new_video_users_percentage):
            response = self.client.get(self.endpoint, {'public_key': '%s:/%s' % (public_key, item_id)})

        assert response.status_code == OK
        result = from_json(response.content)

        assert_that(result, has_entries(duration=instance_of(int),
                                        items=instance_of(list),
                                        total=instance_of(int),
                                        stream_id=instance_of(unicode)))

        assert_that(result['items'], has_item(has_entries(video_codec=instance_of(unicode),
                                                          resolution=any_of(equal_to('adaptive'),
                                                                            equal_to('240p'),
                                                                            equal_to('360p'),
                                                                            equal_to('480p'),
                                                                            equal_to('720p')),
                                                          container='hls',
                                                          audio_codec=instance_of(unicode),
                                                          links=has_entries(https=instance_of(unicode)))))

    @parameterized.expand([
        ('new_video_streaming_and_not_owner', 100, user_3.uid, NOT_FOUND),
        ('old_video_streaming_and_not_owner', 0, user_3.uid, NOT_FOUND),
        ('new_video_streaming_and_no_user', 100, None, NOT_FOUND),
        ('old_video_streaming_and_no_user', 0, None, NOT_FOUND),
        ('new_video_streaming_and_owner', 100, default_user.uid, OK),
        ('old_video_streaming_and_owner', 0, default_user.uid, OK),
    ])
    def test_blocked_album(self, case_name, new_video_users_percentage, request_uid, expected_code):
        u"""Проверить запрос на заблокированный альбом."""
        public_key = self.album['public']['public_key']
        self.json_ok('public_album_block', opts={'public_key': public_key, 'reason': 'raccoon is too fluffy'})

        with self.mock_for_successful_video_streaming_response(), \
                self.specified_client(id=self.client_id, uid=request_uid), \
                mock.patch('mpfs.platform.v1.disk.handlers.PLATFORM_VIDEO_NEW_PERCENTAGE', new_video_users_percentage):
            response = self.client.get(self.endpoint, {'public_key': '%s:/%s' % (public_key,
                                                                                 self.album['items'][0]['id'])})

            assert response.status_code == expected_code

    @parameterized.expand([
        ('new_video_streaming_and_not_owner', 100, user_3.uid, NOT_FOUND),
        ('old_video_streaming_and_not_owner', 0, user_3.uid, NOT_FOUND),
        ('new_video_streaming_and_no_user', 100, None, NOT_FOUND),
        ('old_video_streaming_and_no_user', 0, None, NOT_FOUND),
        ('new_video_streaming_and_owner', 100, default_user.uid, OK),
        ('old_video_streaming_and_owner', 0, default_user.uid, OK),
    ])
    def test_blocked_user(self, case_name, new_video_users_percentage, request_uid, expected_code):
        u"""Проверить запрос на альбом заблокированного пользователя."""
        opts = {'uid': self.uid,
                'moderator': 'moderator',
                'comment': 'his raccoon is too fluffy'}
        self.support_ok('block_user', opts)

        with self.mock_for_successful_video_streaming_response(), \
             self.specified_client(id=self.client_id, uid=request_uid), \
             mock.patch('mpfs.platform.v1.disk.handlers.PLATFORM_VIDEO_NEW_PERCENTAGE', new_video_users_percentage):
            response = self.client.get(self.endpoint, {'public_key': '%s:/%s' % (self.album['public']['public_key'],
                                                                                 self.album['items'][0]['id'])})

            assert response.status_code == expected_code

    @parameterized.expand([
        ('new_video_streaming_and_not_owner', 100, user_3.uid, 451),
        ('old_video_streaming_and_not_owner', 0, user_3.uid, 451),
        ('new_video_streaming_and_no_user', 100, None, 451),
        ('old_video_streaming_and_no_user', 0, None, 451),
        ('new_video_streaming_and_owner', 100, default_user.uid, OK),
        ('old_video_streaming_and_owner', 0, default_user.uid, OK),
    ])
    def test_blocked_resource(self, case_name, new_video_users_percentage, request_uid, expected_code):
        u"""Проверить запрос на заблокированный ресурс.

        Если альбом доступен для просмотра пользователю, но видео заблокировано,
        то при попытке просмотра пользователь должен получить: 451 UNAVAILABLE FOR LEGAL REASONS
        """
        private_hash = self.json_ok('set_public',
                                    {'uid': self.uid, 'path': self.VIDEO_PATH})['hash']
        self.support_ok('block_public_file', {'moderator': 'testmoder',
                                              'comment': 'testcomment',
                                              'private_hash': private_hash,
                                              'type': 'block_file',
                                              'view': 'st',
                                              'link': '',
                                              'notify': 0,
                                              'hid_block_type': 'only_view_delete'})

        with self.mock_for_successful_video_streaming_response(), \
             self.specified_client(id=self.client_id, uid=request_uid), \
             mock.patch('mpfs.platform.v1.disk.handlers.PLATFORM_VIDEO_NEW_PERCENTAGE', new_video_users_percentage):
            response = self.client.get(self.endpoint, {'public_key': '%s:/%s' % (self.album['public']['public_key'],
                                                                                 self.album['items'][0]['id'])})

            assert response.status_code == expected_code

    @parameterized.expand([
        ('new_video_streaming', 100),
        ('old_video_streaming', 0),
    ])
    def test_missed_album_item(self, case_name, new_video_users_percentage):
        u"""Проверить запрос при пропущенном элементе альбома."""
        with self.mock_for_successful_video_streaming_response(), \
                self.specified_client(id=self.client_id), \
                mock.patch('mpfs.platform.v1.disk.handlers.PLATFORM_VIDEO_NEW_PERCENTAGE', new_video_users_percentage):
            response = self.client.get(self.endpoint,
                                       {'public_key': '%s' % self.album['public']['public_key']})

        assert response.status_code == NOT_FOUND

    def test_client_id_forwarded_to_mpfs(self):
        public_key = self.album['public']['public_key']
        item_id = self.album['items'][0]['id']
        with mock.patch.object(ListPublicVideoStreamsHandler, 'permissions', AllowByClientIdPermission([self.client_id])), \
                self.specified_client(id=self.client_id), \
                mock.patch.object(handlers, 'PLATFORM_VIDEO_NEW_PERCENTAGE', 100), \
                VideoStreamingStub() as stub:
            self.client.get(self.endpoint, {'public_key': '%s:/%s' % (public_key, item_id)})
            assert stub.get_video_info.call_args[1]['client_id'] == self.client_id


class ListVideoStreamsFromPrivateAlbumTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL

    mpfs_file_path = '/disk/IoTTools_high.mp4'
    api_file_path = 'disk:/IoTTools_high.mp4'

    client_id = '12345678901234567890123456789012'
    url = 'disk/video/streams'

    x_forwarded_for = '1.1.1.1'
    x_real_ip = '2.2.2.2'
    ip = '3.3.3.3'

    ALBUM_CREATE_JSON = {'title': 'MyAlbum',
                         'description': 'My Test Album',
                         'layout': 'rows',
                         'flags': ['show_frames'],
                         'items': [{'type': 'resource', 'path': mpfs_file_path}]}

    def setup_method(self, method):
        super(ListVideoStreamsFromPrivateAlbumTestCase, self).setup_method(method)
        self.create_user(self.uid)
        self.upload_file(self.uid, self.mpfs_file_path)
        self.album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=self.ALBUM_CREATE_JSON)
        self.album_id = self.album['id']
        self.item_id = self.album['items'][0]['id']
        self.json_ok('album_unpublish', opts={'uid': self.uid, 'album_id': self.album_id})

        with open('fixtures/json/new_video_streaming_info_response.json') as fd:
            self.mpfs_response = json.dumps(json.loads(fd.read()))
        with open('fixtures/json/new_video_streaming_info_response.json') as fd:
            self.video_response = json.dumps(json.loads(fd.read()))

    def _test_mocks(self):
        real_open_url = MpfsProxy.open_url

        def fake_mpfsproxy_open_url(_self, url, *args, **kwargs):
            if ('video_url' in url) or ('album_video_streams' in url) or ('video_streams' in url):
                return 200, self.mpfs_response, {}
            else:
                return real_open_url(_self, url, *args, **kwargs)

        return contextlib.nested(
            self.specified_client(id=self.client_id),
            mock.patch.object(ListVideoStreamsHandler, 'permissions', AllowByClientIdPermission([self.client_id])),
            mock.patch.object(MpfsProxy, 'open_url', fake_mpfsproxy_open_url),
            mock.patch.object(video, 'open_url', return_value=(200, self.video_response, {}))
        )

    def _video_mocks(self):
        return contextlib.nested(
            self.specified_client(id=self.client_id),
            mock.patch.object(ListVideoStreamsHandler, 'permissions', AllowByClientIdPermission([self.client_id])),
            mock.patch.object(handlers, 'PLATFORM_VIDEO_NEW_PERCENTAGE', 99),
            VideoStreamingStub()
        )

    def test_fields(self):
        with self._test_mocks():
            resp = self.client.get(self.url, query={'album_id': self.album_id, 'item_id': self.item_id})
            resp_content = from_json(resp.content)
            assert 'duration' in resp_content
            assert 'stream_id' in resp_content
            assert 'items' in resp_content

    def test_adaptive_item(self):
        adaptive_link_tmpl = '/hls-playlist/%s/master-playlist.m3u8'
        adaptive_item_fields = {'resolution', 'links', 'container', 'audio_codec', 'video_codec'}
        with self._test_mocks():
            resp = self.client.get(self.url, query={'album_id': self.album_id, 'item_id': self.item_id})
            resp_content = from_json(resp.content)
            adaptive_items = [i for i in resp_content['items'] if i['resolution'] == 'adaptive']
            assert len(adaptive_items) == 1
            adaptive_item = adaptive_items[0]
            assert 'links' in adaptive_item
            assert not adaptive_item_fields ^ adaptive_item.viewkeys()
            for link in adaptive_item['links'].itervalues():
                assert link.endswith(adaptive_link_tmpl % resp_content['stream_id'])

    def test_call_new_video_streaming(self):
        with self._video_mocks() as mocks:
            resp = self.client.get(self.url, query={'album_id': self.album_id, 'item_id': self.item_id})
            assert resp.status_code == 200
            mocks[-1].get_video_info.assert_called_once()

    @parameterized.expand([
        ('x_real_ip', {'X-Real-IP': x_real_ip}, x_real_ip),
        ('x_forwarded_for', {'X-Forwarded-For': x_forwarded_for}, x_forwarded_for),
        ('both_headers_priority', {'X-Real-IP': x_real_ip, 'X-Forwarded-For': x_forwarded_for}, x_real_ip),
        ('external_ignore', {'X-Real-IP': x_real_ip, 'X-Forwarded-For': x_forwarded_for}, ip,
         tags.platform.EXTERNAL),
    ])
    def test_call_video_streaming_ips(self, test_name, headers, result_ip, api_mode=tags.platform.INTERNAL):
        self.api_mode = api_mode
        with self._video_mocks() as mocks:
            self.client.get(self.url, query={'album_id': self.album_id, 'item_id': self.item_id}, headers=headers, ip=self.ip)
            video_info = mocks[-1].get_video_info
            video_info.assert_called_once()
            assert video_info.call_args[1]['user_ip'] == result_ip

    def test_client_id_forwarded_to_mpfs(self):
        with mock.patch.object(ListVideoStreamsHandler, 'permissions', AllowByClientIdPermission([self.client_id])), \
                self.specified_client(id=self.client_id), \
                mock.patch.object(handlers, 'PLATFORM_VIDEO_NEW_PERCENTAGE', 100), \
                VideoStreamingStub() as stub:
            self.client.get(self.url, query={'album_id': self.album_id, 'item_id': self.item_id})
            assert stub.get_video_info.call_args[1]['client_id'] == self.client_id
