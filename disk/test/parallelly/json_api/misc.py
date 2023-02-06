# -*- coding: utf-8 -*-
import json
import urlparse

import pytest
from mock import patch

from mpfs.common.static import codes
from mpfs.core.services.rate_limiter_service import rate_limiter
from test.parallelly.json_api.base import CommonJsonApiTestCase
from test.base import get_search_item_response
from test.fixtures import users

import mpfs.engine.process

from mpfs.core.services.search_service import DiskSearch
from mpfs.config import settings


share_user = mpfs.engine.process.share_user()


class MiscJsonApiTestCase(CommonJsonApiTestCase):
    def setup_method(self, method):
        super(MiscJsonApiTestCase, self).setup_method(method)
        import mpfs.core.services.stock_service
        mpfs.core.services.stock_service._setup()
        self.create_user(share_user, add_services='initial_10gb')

    def _upload_autologin_installer(self):
        # заливаем фейковый инсталлятор
        self.upload_file(share_user, '/share/dist/file.msi')

        # устанавливаем особый! параметр для файла - что нужно проставлять в file_url флажок для автологина
        opts = {
            'uid': share_user,
            'path': '/share/dist/file.msi',
            'auto_login': 1
        }
        self.json_ok('setprop', opts)

    def test_store_share_dev(self):
        import mpfs.core.services.stock_service

        mpfs.engine.process.usrctl().create(share_user)
        mpfs.core.services.stock_service._setup()

        opts = {
            'uid': share_user,
            'path': '/share/test',
        }
        self.json_ok('mkdir', opts)
        self.upload_file(share_user, '/share/test/file.txt')
        opts = {
            'uid': share_user,
            'path': '/share/test/file.txt',
        }
        self.json_ok('info', opts)

    def test_autologin_setprop_and_url(self):
        import mpfs.core.services.stock_service

        mpfs.engine.process.usrctl().create(share_user)
        mpfs.core.services.stock_service._setup()

        # заливаем фейковый инсталлятор
        self.upload_file(share_user, '/share/dist/file.msi')

        # дергаем ручку list_installers и проверяем, что file_url обычный - нету параметра al в query string
        opts = {
            'al': '1'
        }
        result = self.json_ok('list_installers', opts)
        assert len(result) == 1
        assert 'file_url' in result[0]['meta']
        file_url = result[0]['meta']['file_url']
        assert 'al' not in urlparse.parse_qs(urlparse.urlparse(file_url).query)

        # устанавливаем особый! параметр для файла - что нужно проставлять в file_url флажок для автологина
        opts = {
            'uid': share_user,
            'path': '/share/dist/file.msi',
            'auto_login': 1
        }
        self.json_ok('setprop', opts)

        # дергаем ручку list_installers и проверяем, что file_url необычный! - есть параметр al=1 в query string
        opts = {
            'uid': share_user,
            'al': '1'
        }
        result = self.json_ok('list_installers', opts)
        assert len(result) == 1
        assert 'file_url' in result[0]['meta']
        file_url = result[0]['meta']['file_url']
        assert 'al' in urlparse.parse_qs(urlparse.urlparse(file_url).query)
        assert urlparse.parse_qs(urlparse.urlparse(file_url).query)['al'][0] == '1'

        # а вот если не передать в ручку al, а параметр al в файле уже есть, то все равно вгенерим обычный url
        opts = {}
        result = self.json_ok('list_installers', opts)
        assert len(result) == 1
        assert 'file_url' in result[0]['meta']
        file_url = result[0]['meta']['file_url']
        assert 'al' not in urlparse.parse_qs(urlparse.urlparse(file_url).query)

    def test_return_url_with_autologin_for_initialized_user(self):
        import mpfs.core.services.stock_service
        mpfs.engine.process.usrctl().create(share_user)
        mpfs.core.services.stock_service._setup()
        self._upload_autologin_installer()

        # дергаем ручку list_installers и проверяем, что file_url необычный! - есть параметр al=1 в query string
        # если пользователь проинициализирован
        with patch('mpfs.core.user.standart.StandartUser.can_init_user') as can_init_user_mock:
            result = self.json_ok('list_installers', {'uid': share_user, 'al': '1'})
            can_init_user_mock.assert_not_called()
        assert len(result) == 1
        assert 'file_url' in result[0]['meta']
        file_url = result[0]['meta']['file_url']
        assert 'al' in urlparse.parse_qs(urlparse.urlparse(file_url).query)
        assert urlparse.parse_qs(urlparse.urlparse(file_url).query)['al'][0] == '1'

    def test_return_url_with_autologin_for_initializable_user(self):
        import mpfs.core.services.stock_service
        mpfs.engine.process.usrctl().create(share_user)
        mpfs.core.services.stock_service._setup()
        self._upload_autologin_installer()

        # дергаем ручку list_installers и проверяем, что file_url необычный! - есть параметр al=1 в query string
        # если пользователь непроинициализирован, но его можно проинициализировать
        with patch('mpfs.core.user.standart.StandartUser.can_init_user',  return_value=True) as can_init_user_mock:
            result = self.json_ok('list_installers', {'uid': '1234', 'al': '1'})
            can_init_user_mock.assert_called_once()
        assert len(result) == 1
        assert 'file_url' in result[0]['meta']
        file_url = result[0]['meta']['file_url']
        assert 'al' in urlparse.parse_qs(urlparse.urlparse(file_url).query)
        assert urlparse.parse_qs(urlparse.urlparse(file_url).query)['al'][0] == '1'

    def test_force_return_url_without_autologin_for_uninitializable_user(self):
        import mpfs.core.services.stock_service
        mpfs.engine.process.usrctl().create(share_user)
        mpfs.core.services.stock_service._setup()
        self._upload_autologin_installer()

        # отдаем file_url без al=1 если пользователя нельзя проинициализировать
        with patch('mpfs.core.user.standart.StandartUser.can_init_user', return_value=False) as can_init_user_mock:
            result = self.json_ok('list_installers', {'uid': '1234', 'al': '1'})
            can_init_user_mock.assert_called_once()
        assert len(result) == 1
        assert 'file_url' in result[0]['meta']
        file_url = result[0]['meta']['file_url']
        assert 'al' not in urlparse.parse_qs(urlparse.urlparse(file_url).query)

    def test_open_url_after_install(self):
        import mpfs.core.services.stock_service
        mpfs.engine.process.usrctl().create(share_user)
        mpfs.core.services.stock_service._setup()
        self.upload_file(share_user, '/share/dist/file.msi')

        result = self.json_ok('list_installers', {'uid': share_user, 'open_url_after_install': 'qqq'})
        assert len(result) == 1
        assert 'file_url' in result[0]['meta']
        file_url = result[0]['meta']['file_url']
        assert 'ouai' not in urlparse.parse_qs(urlparse.urlparse(file_url).query)

        self.json_ok('setprop', {'uid': share_user, 'path': '/share/dist/file.msi', 'auto_login': 1})

        result = self.json_ok('list_installers', {'uid': share_user, 'open_url_after_install': 'qqq'})
        assert len(result) == 1
        assert 'file_url' in result[0]['meta']
        file_url = result[0]['meta']['file_url']
        assert 'ouai' in urlparse.parse_qs(urlparse.urlparse(file_url).query)

        result = self.json_ok('list_installers', {'uid': share_user})
        assert len(result) == 1
        assert 'file_url' in result[0]['meta']
        file_url = result[0]['meta']['file_url']
        assert 'ouai' not in urlparse.parse_qs(urlparse.urlparse(file_url).query)

    def test_patch_installer(self):
        """
        Проверяем ручку list_installers
        """
        import mpfs.core.services.stock_service

        mpfs.engine.process.usrctl().create(share_user)
        mpfs.core.services.stock_service._setup()

        # заливаем фейковый инсталлятор
        self.upload_file(share_user, '/share/dist/file.msi')

        # проверяем, что у обычного файла нет параметра src в query string
        opts = {
            'src': 'test_source'
        }
        result = self.json_ok('list_installers', opts)
        assert len(result) == 1
        assert 'file_url' in result[0]['meta']
        file_url = result[0]['meta']['file_url']
        assert 'src' not in urlparse.parse_qs(urlparse.urlparse(file_url).query)

        # устанавливаем особый параметр для файла, необходимый для installer_source
        opts = {
            'uid': share_user,
            'path': '/share/dist/file.msi',
            'patch_installer': 1
        }
        self.json_ok('setprop', opts)

        # проверяем, что у измененного файла есть параметр src в query string
        opts = {
            'src': 'test_source'
        }
        result = self.json_ok('list_installers', opts)
        assert len(result) == 1
        assert 'file_url' in result[0]['meta']
        file_url = result[0]['meta']['file_url']
        assert 'src' in urlparse.parse_qs(urlparse.urlparse(file_url).query)
        assert urlparse.parse_qs(urlparse.urlparse(file_url).query)['src'][0] == 'test_source'

        # проверяем, что у измененного файла есть параметр src в query string и равен дефолтному, если src = ''
        opts = {
            'src': ''
        }
        result = self.json_ok('list_installers', opts)
        assert len(result) == 1
        assert 'file_url' in result[0]['meta']
        file_url = result[0]['meta']['file_url']
        assert 'src' in urlparse.parse_qs(urlparse.urlparse(file_url).query)
        assert urlparse.parse_qs(urlparse.urlparse(file_url).query)['src'][0] == 'Yandex.Unknown'

        # проверяем, что если src нет, то параметр тоже не получаем
        opts = {
            'uid': share_user,
        }
        result = self.json_ok('list_installers', opts)
        assert len(result) == 1
        assert 'file_url' in result[0]['meta']
        file_url = result[0]['meta']['file_url']
        assert 'src' not in urlparse.parse_qs(urlparse.urlparse(file_url).query)

    def test_diff_ratelimit(self):
        """
        Проверяем, что если рейтлимитер отвечает 429, то и mpfs отдает 429 (для фулл диффов и для версионных)
        """
        diff = self.json_ok('diff', {'uid': self.uid})
        version = diff['version']

        with patch.object(rate_limiter, 'is_limit_exceeded', return_value=True) as stub:
            self.json_error('diff', {'uid': self.uid}, code=codes.REQUESTS_LIMIT_EXCEEDED_429)
            assert stub.call_args[0] == ('mpfs_full_diff_by_uid', self.uid)

        self.json_ok('diff', {'uid': self.uid, 'version': version})

        with patch.object(rate_limiter, 'is_limit_exceeded', return_value=True) as stub:
            self.json_error('diff', {'uid': self.uid, 'version': version}, code=codes.REQUESTS_LIMIT_EXCEEDED_429)
            assert stub.call_args[0] == ('mpfs_version_diff_by_uid', self.uid)

    def test_diff_shared(self):
        """
        Хозяин перемещает общую папку, приглашенный запрашивает дифф
        """
        uid1 = users.default_user.uid
        uid2 = users.user_1.uid
        self.json_ok('user_init', {'uid': uid1})
        self.json_ok('user_init', {'uid': uid2})
        email1 = 'mpfs-test@yandex.ru'  # password=hash(mpfs-test)[:16]
        # email2 = 'mpfs-test-1@yandex.ru' #password=hash(mpfs-test-1)[:16]

        uid1_shared_dir = '/disk/sh_d'
        self.json_ok('mkdir', {'uid': uid1, 'path': uid1_shared_dir})
        self.share_dir(uid1, uid2, email1, uid1_shared_dir)
        self.json_ok('move', {'uid': uid2, 'src': uid1_shared_dir, 'dst': '/disk/my'})

        self.upload_file(uid1, uid1_shared_dir + '/test')
        uid2_ver = self.json_ok('diff', {'uid': uid2})['version']
        self.upload_file(uid1, uid1_shared_dir + '/test2')
        self.json_ok('move', {'uid': uid1, 'src': uid1_shared_dir, 'dst': '/disk/his'})
        key = self.json_ok('diff', {'uid': uid2, 'version': uid2_ver})['result'][0]['key']
        self.assertEqual('/disk/my/test2', key)

    def test_folder_with_colon(self):
        """
        https://jira.yandex-team.ru/browse/CHEMODAN-10336
        https://jira.yandex-team.ru/browse/CHEMODAN-10422
        """
        opts = {'uid': self.uid, 'path': '/disk/folder:colon'}
        self.json_ok('mkdir', opts)

        opts = {'uid': self.uid, 'path': '/disk'}
        listing = self.json_ok('list', opts)

        for item in listing:
            if item['type'] == 'dir':
                self.assertTrue(item['id'].endswith('/'))

    def test_decode_custom_properties(self):
        """Протестировать, что `custom_properties` декодируется всегда
        """
        test_file = '/disk/test.txt'
        self.upload_file(self.uid, test_file)

        custom_properties = '{"a": 1}'
        self.json_ok('setprop', {
            'uid': self.uid,
            'path': test_file,
            'custom_properties': custom_properties
        })

        response = self.json_ok('info',  {
            'uid': self.uid,
            'path': test_file,
            'meta': 'custom_properties',
        })
        assert response['meta']['custom_properties'] == json.loads(custom_properties)

        response = self.json_ok('list',  {
            'uid': self.uid,
            'path': '/disk',
            'meta': 'custom_properties',
        })
        assert json.loads(custom_properties) == next((
            i['meta']['custom_properties'] for i in response if i['path'] == test_file
        ))

        search_response = get_search_item_response(test_file, '*')
        with patch.object(DiskSearch, 'open_url', return_value=search_response):
            response = self.json_ok('new_search',  {
                'uid': self.uid,
                'meta': 'custom_properties',
            })
        assert json.loads(custom_properties) == next((
            i['meta']['custom_properties'] for i in response['results'] if i['path'] == test_file
        ))
