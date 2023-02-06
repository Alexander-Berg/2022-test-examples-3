# coding: utf-8
import json

import mock
from nose_parameterized import parameterized

from mpfs.common.util.experiments.logic import enable_experiment_for_uid
from mpfs.common.util.video_unlim import COUNTRY_KEY, COUNTRY_FIELD
from mpfs.config import settings
from mpfs.core.metastorage.control import disk_info
from mpfs.core.services.uaas_service import new_uaas
from mpfs.core.user.constants import PHOTOUNLIM_AREA
from test.parallelly.api.disk.base import DiskApiTestCase
from test.base_suit import UserTestCaseMixin, UploadFileTestCaseMixin

PLATFORM_MOBILE_APPS_IDS = settings.platform['mobile_apps_ids']


class UnlimitTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    # api_mode = tags.platform.EXTERNAL
    # api_version = 'v1'

    def test_unlimited_sets(self):
        old_get_disk_experiments = new_uaas.get_disk_experiments

        def mock_get_disk_experiments(user_agent=None, uid=None):
            assert uid
            return old_get_disk_experiments(user_agent=user_agent, uid=uid)

        new_uaas.get_disk_experiments = mock_get_disk_experiments

        self.create_user(self.uid)
        self.json_ok('user_info', {'uid': self.uid})
        # enable video unlim
        resp = self.json_ok('set_unlimited_autouploading',
                            {'unlimited_video_autoupload_enabled': 0,
                             'unlimited_video_autoupload_reason': 'by_user',
                             'uid': self.uid})
        assert resp['unlimited_video_autoupload_enabled'] == 0
        assert resp['unlimited_photo_autoupload_enabled'] is None
        assert resp['unlimited_video_autoupload_reason'] == 'by_user'

        # disable photo unlim
        resp = self.json_ok('set_unlimited_autouploading',
                            {'unlimited_photo_autoupload_enabled': 1,
                             'uid': self.uid})
        assert resp['unlimited_video_autoupload_enabled'] == 0
        assert resp['unlimited_photo_autoupload_enabled'] == 1

        # wrong reason
        self.json_error('set_unlimited_autouploading',
                               {'unlimited_video_autoupload_enabled': 1,
                                'unlimited_video_autoupload_reason': 'wrong',
                                'uid': self.uid})
        with mock.patch('mpfs.core.filesystem.resources.photounlim.PHOTOUNLIM_ALLOW_YANDEX_SEARCH_MOBILE', True):
            res = self.json_error('store', {
                'uid': self.uid,
                'photostream_destination': PHOTOUNLIM_AREA,
                'path': '/photostream/2014-11-23+10-12-44.MP4',
                'sha256': 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
                'size': 0, 'force': 1,
                'use_https': 0, 'md5': 'd41d8cd98f00b204e9800998ecf8427e'}, headers={'user_agent': '1YandexSearch1', 'Yandex-Cloud-Request-ID': 'andr-123'})
            assert res['code'] == 280
            assert res['title'] == '{"reason":"by_user"}'
        new_uaas.get_disk_experiments = old_get_disk_experiments

    def test_video_unlim_blocked(self):
        """
        тест что нельзя включить видеобезлимит бесплатным пользователям
        """
        self.create_user(self.uid)
        self.json_error('set_unlimited_autouploading', {'unlimited_video_autoupload_enabled': 1,
                                                        'unlimited_video_autoupload_reason': 'by_user',
                                                        'uid': self.uid})
        with mock.patch('mpfs.core.user.standart.StandartUser.disk_pro_enabled', return_value=True):
            self.json_ok('set_unlimited_autouploading', {'unlimited_video_autoupload_enabled': 1,
                                                         'unlimited_video_autoupload_reason': 'by_user',
                                                         'uid': self.uid})

    def test_photounlim_allowed(self):
        """
        тест - эксперимент включения фотобезлимита бесплатными пользователями
        """
        self.create_user(self.uid)

        # Пользователь не входит в эксперимент, включение фотобезлимита доступно
        self.json_ok('set_unlimited_autouploading',
                     {'unlimited_photo_autoupload_enabled': 1, 'uid': self.uid})

        # Пользователь попал в эксперимент
        with mock.patch('mpfs.core.user.common.experiment_manager.is_feature_active', return_value=True):
            # Подключен Диск про, включение фотобезлимита доступно
            with mock.patch('mpfs.core.user.standart.StandartUser.disk_pro_enabled', return_value=True):
                self.json_ok('set_unlimited_autouploading',
                             {'unlimited_photo_autoupload_enabled': 1, 'uid': self.uid})

            # Подключен Диск Про и доступен переключатель, включение фотобезлимита доступно
            with mock.patch('mpfs.core.user.standart.StandartUser.disk_pro_enabled', return_value=True):
                with mock.patch('mpfs.core.user.common.CommonUser.get_unlimited_photo_autouploading_allowed',
                                return_value=True):
                    self.json_ok('set_unlimited_autouploading',
                                 {'unlimited_photo_autoupload_enabled': 1, 'uid': self.uid})

            # Доступен переключатель, включение фотобезлимита доступно
            with mock.patch('mpfs.core.user.common.CommonUser.get_unlimited_photo_autouploading_allowed',
                            return_value=True):
                self.json_ok('set_unlimited_autouploading',
                             {'unlimited_photo_autoupload_enabled': 1, 'uid': self.uid})

            # В остальных случаях включение фотобезлимита не доступно
            self.json_error('set_unlimited_autouploading',
                            {'unlimited_photo_autoupload_enabled': 1, 'uid': self.uid})

    def test_video_unlim_allowed(self):
        """
        тест - эксперимент включения видеобезлимита бесплатными пользователями
        """
        self.create_user(self.uid)

        # Подключен Диск про, включение видеобезлимита доступно
        with mock.patch('mpfs.core.user.standart.StandartUser.disk_pro_enabled', return_value=True):
            self.json_ok('set_unlimited_autouploading',
                         {'unlimited_video_autoupload_enabled': 1,
                          'unlimited_video_autoupload_reason': 'by_user',
                          'uid': self.uid})

        # Подключен Диск Про и доступен переключатель, включение видеобезлимита доступно
        with mock.patch('mpfs.core.user.standart.StandartUser.disk_pro_enabled', return_value=True):
            with mock.patch('mpfs.core.user.common.CommonUser.get_unlimited_video_autouploading_allowed',
                            return_value=True):
                self.json_ok('set_unlimited_autouploading',
                             {'unlimited_video_autoupload_enabled': 1,
                              'unlimited_video_autoupload_reason': 'by_user',
                              'uid': self.uid})

        # Доступен переключатель, включение видеобезлимита доступно
        with mock.patch('mpfs.core.user.common.CommonUser.get_unlimited_video_autouploading_allowed',
                        return_value=True):
            self.json_ok('set_unlimited_autouploading',
                         {'unlimited_video_autoupload_enabled': 1,
                          'unlimited_video_autoupload_reason': 'by_user',
                          'uid': self.uid})

        # В остальных случаях включение видеобезлимита не доступно
        self.json_error('set_unlimited_autouploading',
                        {'unlimited_video_autoupload_enabled': 1,
                         'unlimited_video_autoupload_reason': 'by_user',
                         'uid': self.uid})

    @parameterized.expand([
        (0, 0, 'limit', None, 'photostream'),
        (0, 1, 'limit', None, 'photounlim'),
        (0, 0, 'limit', None, 'photostream'),
        (0, 1, 'limit', None, 'photounlim'),
        (1, 0, 'limit', None, 'photostream'),
        (1, 1, 'limit', None, 'photounlim'),
        (1, 0, 'limit', None, 'photostream'),
        (1, 1, 'limit', None, 'photounlim'),
        (0, 0, PHOTOUNLIM_AREA, None, 'photostream'),
        (0, 1, PHOTOUNLIM_AREA, None, 'photounlim'),
        (0, 0, PHOTOUNLIM_AREA, None, 'photostream'),
        (0, 1, PHOTOUNLIM_AREA, None, 'photounlim'),
        (1, 0, PHOTOUNLIM_AREA, None, 'photostream'),
        (1, 1, PHOTOUNLIM_AREA, None, 'photounlim'),
        (1, 0, PHOTOUNLIM_AREA, None, 'photostream'),
        (1, 1, PHOTOUNLIM_AREA, None, 'photounlim'),
        (0, 0, None, None, 'photostream'),
        (0, 1, None, None, 'photostream'),
        (0, 0, None, None, 'photostream'),
        (0, 1, None, None, 'photostream'),
        (1, 0, None, None, 'photounlim'),
        (1, 1, None, None, 'photounlim'),
        (1, 0, None, None, 'photounlim'),
        (1, 1, None, None, 'photounlim'),
        (0, None, 'limit', None, 'photostream'),
        (1, None, 'limit', None, 'photounlim'),
        (0, None, PHOTOUNLIM_AREA, None, 'photostream'),
        (1, None, PHOTOUNLIM_AREA, None, 'photounlim'),
    ])
    def test_unlimeted_photo_store(self, unlim_state, photo_state, photostream_destination, code, dest):
        old_get_disk_experiments = new_uaas.get_disk_experiments

        def mock_get_disk_experiments(user_agent=None, uid=None):
            assert uid
            return old_get_disk_experiments(user_agent=user_agent, uid=uid)

        new_uaas.get_disk_experiments = mock_get_disk_experiments

        self.create_user(self.uid)
        self.json_ok('user_info', {'uid': self.uid})
        if unlim_state:
            self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})
        params = {}
        if photo_state is not None:
            params['unlimited_photo_autoupload_enabled'] = photo_state
        if params:
            params['uid'] = self.uid
            self.json_ok('set_unlimited_autouploading', params)
        with mock.patch('mpfs.core.filesystem.resources.photounlim.PHOTOUNLIM_ALLOW_YANDEX_SEARCH_MOBILE', True):
            res = self.upload_file(
                self.uid,
                '/photostream/file.JPG', photostream_destination=photostream_destination,
                headers={'user_agent': '1YandexSearch1', 'Yandex-Cloud-Request-ID': 'andr-123'}, return_result=True,
                error_on_store=(code is not None)
            )

            if code is None:
                if dest == 'photounlim':
                    self.json_error('info', {'uid': self.uid, 'path': '/disk/Фотокамера/file.JPG'})
                    self.json_ok('info', {'uid': self.uid, 'path': '/photounlim/file.JPG'})
                else:
                    self.json_ok('info', {'uid': self.uid, 'path': '/disk/Фотокамера/file.JPG'})
                    self.json_error('info', {'uid': self.uid, 'path': '/photounlim/file.JPG'})

                assert 'code' not in res
            else:
                self.json_error('info', {'uid': self.uid, 'path': '/disk/Фотокамера/file.JPG'})
                self.json_error('info', {'uid': self.uid, 'path': '/photounlim/file.JPG'})
                assert res['code'] == code
        new_uaas.get_disk_experiments = old_get_disk_experiments

    @parameterized.expand([
        (0, 0, 0, 'limit', None, 'photostream'),
        (0, 0, 1, 'limit', None, 'photostream'),
        (1, 0, 0, 'limit', None, 'photostream'),
        (1, 0, 1, 'limit', None, 'photostream'),
        (0, 0, 0, PHOTOUNLIM_AREA, 280, ''),
        (0, 0, 1, PHOTOUNLIM_AREA, 280, ''),
        (1, 0, 0, PHOTOUNLIM_AREA, 280, ''),
        (1, 0, 1, PHOTOUNLIM_AREA, 280, ''),
        (0, 0, 0, None, None, 'photostream'),
        (0, 0, 1, None, None, 'photostream'),
        (1, 0, 0, None, None, 'photounlim'),
        (1, 0, 1, None, None, 'photounlim'),
        (0, None, None, 'limit', None, 'photostream'),
        (1, None, None, 'limit', None, 'photounlim'),
        (0, None, None, PHOTOUNLIM_AREA, 280, ''),
        (1, None, None, PHOTOUNLIM_AREA, 280, ''),
    ])
    def test_unlimeted_video_store(self, unlim_state, video_state, photo_state, photostream_destination, code, dest):
        old_get_disk_experiments = new_uaas.get_disk_experiments

        def mock_get_disk_experiments(user_agent=None, uid=None):
            assert uid
            return old_get_disk_experiments(user_agent=user_agent, uid=uid)

        new_uaas.get_disk_experiments = mock_get_disk_experiments

        self.create_user(self.uid)
        self.json_ok('user_info', {'uid': self.uid})
        if unlim_state:
            self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})
        params = {}
        if video_state is not None:
            params['unlimited_video_autoupload_enabled'] = video_state
            params['unlimited_video_autoupload_reason'] = 'by_user'
        if photo_state is not None:
            params['unlimited_photo_autoupload_enabled'] = photo_state
        if params:
            params['uid'] = self.uid
            resp = self.json_ok('set_unlimited_autouploading',
                                params)

        with mock.patch('mpfs.core.filesystem.resources.photounlim.PHOTOUNLIM_ALLOW_YANDEX_SEARCH_MOBILE', True):
            res = self.upload_file(
                self.uid,
                '/photostream/file.MP4', photostream_destination=photostream_destination,
                headers={'user_agent': '1YandexSearch1', 'Yandex-Cloud-Request-ID': 'andr-123'}, return_result=True,
                error_on_store=(code is not None)
            )

            if code is None:
                if dest == 'photounlim':
                    # файл должен попасть в безлимит и не попасть в лимит
                    self.json_error('info', {'uid': self.uid, 'path': '/disk/Фотокамера/file.MP4'})
                    self.json_ok('info', {'uid': self.uid, 'path': '/photounlim/file.MP4'})
                else:
                    # файл должен попасть в лимит и не попасть в безлимит
                    self.json_ok('info', {'uid': self.uid, 'path': '/disk/Фотокамера/file.MP4'})
                    self.json_error('info', {'uid': self.uid, 'path': '/photounlim/file.MP4'})
                # кода ошибки в ответе быть не должно
                assert 'code' not in res
            else:
                # если была ошибка, то файл не должен сохраниться
                self.json_error('info', {'uid': self.uid, 'path': '/disk/Фотокамера/file.MP4'})
                self.json_error('info', {'uid': self.uid, 'path': '/photounlim/file.MP4'})
                assert res['code'] == code
        new_uaas.get_disk_experiments = old_get_disk_experiments

    def test_change_unlim_state(self):
        self.create_user(self.uid)
        self.json_error('set_unlimited_autouploading',
                            {'unlimited_video_autoupload_enabled': 1,
                             'unlimited_video_autoupload_reason': 'by_user',
                             'uid': self.uid})
        with mock.patch('mpfs.core.user.standart.StandartUser.disk_pro_enabled', return_value=True):
            self.json_ok('set_unlimited_autouploading',
                            {'unlimited_video_autoupload_enabled': 1,
                             'unlimited_video_autoupload_reason': 'by_user',
                             'uid': self.uid})

    def test_hotfix_android(self):
        self.create_user(self.uid)
        self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})
        self.json_ok('set_unlimited_autouploading', {'unlimited_video_autoupload_enabled': 0,
                                                     'unlimited_video_autoupload_reason': 'by_user', 'uid': self.uid})
        old_get_disk_experiments = new_uaas.get_disk_experiments

        def mock_get_disk_experiments(user_agent=None, uid=None):
            assert uid
            return old_get_disk_experiments(user_agent=user_agent, uid=uid)

        new_uaas.get_disk_experiments = mock_get_disk_experiments

        with mock.patch('mpfs.core.filesystem.resources.photounlim.PHOTOUNLIM_ALLOW_YANDEX_SEARCH_MOBILE', True):
            self.upload_file(
                self.uid,
                '/photostream/file.MP4',
                headers={'user-agent': 'Yandex.Disk{"os":"android 8.0.0","device":"phone","src":"disk.mobile","vsn":"4.41.2-2652","id":"a55b27f35389730357981e52d0d7c18c","flavor":"prod","uuid":"a7864e36c54d4cc9830162fa760150c7"}', 'Yandex-Cloud-Request-ID': 'andr-123'},
                return_result=True
            )
            self.json_ok('info', {'uid': self.uid, 'path': '/disk/Фотокамера/file.MP4'})
            self.json_error('info', {'uid': self.uid, 'path': '/photounlim/file.MP4'})

            self.json_ok('disable_unlimited_autouploading', {'uid': self.uid})

        new_uaas.get_disk_experiments = old_get_disk_experiments

    def test_is_user_in_unlim_experiment(self):
        self.create_user(self.uid)
        self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})
        self.json_ok('set_unlimited_autouploading', {'unlimited_video_autoupload_enabled': 0,
                                                     'unlimited_video_autoupload_reason': 'by_user', 'uid': self.uid})
        with mock.patch('mpfs.common.util.video_unlim.get_user_country', return_value='TR'):
            resp = self.json_ok('is_user_in_unlim_experiment', {'uid': self.uid, 'is_rkub_experiment': True})
            assert resp['is_user_in_unlim_experiment']

        with mock.patch('mpfs.common.util.video_unlim.get_user_country', return_value='TR'):
            resp = self.json_ok('is_user_in_unlim_experiment', {'uid': self.uid, 'is_rkub_experiment': False})
            assert resp['is_user_in_unlim_experiment']

        with mock.patch('mpfs.common.util.video_unlim.get_user_country', return_value='RU'):
            resp = self.json_ok('is_user_in_unlim_experiment', {'uid': self.uid, 'is_rkub_experiment': True})
            assert resp['is_user_in_unlim_experiment']

        with mock.patch('mpfs.common.util.video_unlim.get_user_country', return_value='RU'):
            resp = self.json_ok('is_user_in_unlim_experiment', {'uid': self.uid, 'is_rkub_experiment': False})
            assert resp['is_user_in_unlim_experiment']
