# -*- coding: utf-8 -*-
import calendar
import os
import random
import re
import json
import time
import urllib
from copy import deepcopy

import mock
import string
import urlparse

import pytest
from datetime import timedelta, datetime
from hamcrest import (
    empty,
    is_,
    not_,
    close_to, has_entry, contains_string, greater_than, equal_to)
from matchers import has_keys
from nose_parameterized import parameterized
from hamcrest import assert_that, has_entries
from unittest import TestCase
from httplib import OK, BAD_REQUEST, CREATED

from mpfs.common.static import codes
from mpfs.common.util.size.datasize import DataSize
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase
from mpfs.core import factory
from mpfs.core.filesystem.hardlinks.common import FileChecksums
from mpfs.core.filesystem.quota import Quota
from mpfs.core.global_gallery.dao.source_id import SourceIdDAOItem
from mpfs.core.office.static import OfficeAccessStateConst
from mpfs.platform.exceptions import FieldsAllOrNoneValidationError
from test.base import CommonDiskTestCase, time_machine
from test.base_suit import (
    UploadFileTestCaseMixin, UserTestCaseMixin,
    BillingApiTestCaseMixin, SharingTestCaseMixin,
    SupportApiTestCaseMixin,
)
from test.helpers.stubs.resources.users_info import DEFAULT_USERS_INFO
from test.common.sharing import CommonSharingMethods
from test.conftest import INIT_USER_IN_POSTGRES
from test.helpers.size_units import GB, MB
from test.parallelly.api.disk.base import DiskApiTestCase
from test.parallelly.json_api.base import CommonJsonApiTestCase
from test.parallelly.office_suit import OfficeTestCase
from test.parallelly.yateam_suit import BaseYaTeamTestCase
from test.helpers.stubs.manager import StubsManager
from test.helpers.stubs.services import SearchDBStub, PassportStub, DjfsApiMockHelper
from test.helpers.stubs.users import MailishUserStub
from test.fixtures.kladun import KladunMocker
from test.fixtures.users import user_6
from mpfs.common.static import tags
from mpfs.common.util import from_json, to_json
from mpfs.config import settings
from mpfs.core.address import ResourceId
from mpfs.core.base import content
from mpfs.core.operations.filesystem.copy import ImportMailAttach
from mpfs.core.services.mail_service import Mail as MailService
from mpfs.core.services.passport_service import passport
from mpfs.core.services.search_service import SearchDB, DiskSearch
from mpfs.core.user.constants import PHOTOUNLIM_AREA_PATH
from mpfs.core.user.standart import StandartUser
from mpfs.dao.session import Session
from mpfs.platform.v1.disk.handlers import (
    GetResourceDimensionsHandler, SearchResourcesHandler,
    PLATFORM_DISK_APPS_IDS
)
from mpfs.platform.v1.disk.serializers import ResourceSerializer, UploadResourceSerializer, LinkSerializer
from mpfs.platform.v1.disk.permissions import DiskSearchPermission, WebDavPermission, DiskAttachWritePermission
from mpfs.platform.v1.disk.exceptions import DiskStorageQuotaExhaustedError, DiskOwnerStorageQuotaExhaustedError
from test.parallelly.unlimited_autouploading import UnlimitedAreaTestCase
from test.helpers.stubs.smart_services import DiskGeoSearchSmartMockHelper



class GetRootTestCase(CommonDiskTestCase, UserTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'
    method = 'GET'
    url = 'disk'

    def setup_method(self, method):
        super(GetRootTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)
        self.login = str(self.uid)

    def test_disk_user_meta_blocked(self):
        """
        Проверяем, что /v1/disk возвращает 4хх, если юзер заблокирован
        """
        resp = self.client.request(self.method, self.url, uid=self.uid)
        assert resp.status_code == 200
        opts = {
            'uid': self.uid,
            'moderator': 'moderator',
            'comment': 'comment',
        }
        self.support_ok('block_user', opts)
        resp = self.client.request(self.method, self.url, uid=self.uid)
        assert resp.status_code == 403

    def test_api_skip_restriction_for_intapi(self):
        used = 100500 * GB
        limit = 1 * GB
        ycrid = 'rest-123321-yay'

        with self.specified_client(uid=self.uid), \
                 mock.patch('mpfs.core.services.disk_service.MPFSStorageService.used',
                            return_value=used), \
                 mock.patch('mpfs.core.services.disk_service.MPFSStorageService.limit',
                            return_value=limit), \
                 mock.patch('mpfs.engine.process.get_cloud_req_id',
                            return_value=ycrid), \
                 mock.patch('mpfs.frontend.api.FEATURE_TOGGLES_DISABLE_API_FOR_OVERDRAFT_PERCENTAGE', 100):
            resp = self.client.request(self.method, 'disk')

        assert resp.status_code == 200

    def test_field_set(self):
        """Проверить что отдается корректный набор полей."""
        response = self.client.request(self.method, self.url, uid=self.uid)
        data = from_json(response.content)
        assert 'max_file_size' in data

    @parameterized.expand([
        (
            'ru', {
                'social': u'disk:/Социальные сети/',
                'facebook': u'disk:/Социальные сети/Facebook',
                'google': u'disk:/Социальные сети/Google+',
                'vkontakte': u'disk:/Социальные сети/ВКонтакте',
                'mailru': u'disk:/Социальные сети/Мой Мир',
                'odnoklassniki': u'disk:/Социальные сети/Одноклассники',
                'instagram': u'disk:/Социальные сети/Instagram'
            }
        ),
        (
            'en', {
                'social': u'disk:/Social networks/',
                'facebook': u'disk:/Social networks/Facebook',
                'google': u'disk:/Social networks/Google+',
                'vkontakte': u'disk:/Social networks/VK',
                'mailru': u'disk:/Social networks/Мой Мир',
                'odnoklassniki': u'disk:/Social networks/Одноклассники',
                'instagram': u'disk:/Social networks/Instagram'
            }
        ),
        (
            'tr', {
                'social': u'disk:/Sosyal ağlar/',
                'facebook': u'disk:/Sosyal ağlar/Facebook',
                'google': u'disk:/Sosyal ağlar/Google+',
                'vkontakte': u'disk:/Sosyal ağlar/VK',
                'mailru': u'disk:/Sosyal ağlar/Мой Мир',
                'odnoklassniki': u'disk:/Sosyal ağlar/Одноклассники',
                'instagram': u'disk:/Sosyal ağlar/Instagram'
            }
        ),
        (
            'uk', {
                'social': u'disk:/Соціальні мережі/',
                'facebook': u'disk:/Соціальні мережі/Facebook',
                'google': u'disk:/Соціальні мережі/Google+',
                'vkontakte': u'disk:/Соціальні мережі/ВКонтакте',
                'mailru': u'disk:/Соціальні мережі/Мой Мир',
                'odnoklassniki': u'disk:/Соціальні мережі/Одноклассники',
                'instagram': u'disk:/Соціальні мережі/Instagram'
            }
        )
    ])
    def test_social_folders_included_in_system_folders(self, locale, expected_set):
        u"""Проверить что папки соц. сетей отдаются в ключе `system_folders` и с правильной локализацией."""
        uid = self.user_3.uid
        self.create_user(uid, locale=locale)

        response = self.client.request(self.method, self.url, uid=uid)
        data = from_json(response.content)
        assert 'system_folders' in data
        assert_that(data['system_folders'], has_entries(expected_set))


class GetResourcesTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    method = 'GET'
    url = 'disk/resources'
    file_path = '/disk/test1.jpg'
    file_data = {'mimetype': 'image/jpg'}

    def setup_method(self, method):
        super(GetResourcesTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)
        self.upload_file(self.uid, self.file_path, file_data=self.file_data)

    @parameterized.expand([
        ('not_overdrafted_rest', 8*GB, 10*GB, 'rest-23456-meow', 200),
        ('overdrafted_but_not_enough_rest', 10*GB + 100*MB, 10*GB, 'rest-23456-meow', 200),
        ('overdrafted_rest', 12*GB, 10*GB, 'rest-23456-meow', 403),
        ('overdrafted_webdav', 12*GB, 10*GB, 'dav-23456-meow', 403),
        ('overdrafted_but_platform_wo_restrictions', 12*GB, 10*GB, 'rest_win-23456-meow', 200),
    ])
    def test_api_restrictions_for_overdraft_users(self, case_name, used, limit, ycrid, expected_code):
        with self.specified_client(uid=self.uid,
                                   scopes=['cloud_api:disk.read', 'cloud_api:disk.write']), \
                 mock.patch('mpfs.core.services.disk_service.MPFSStorageService.used',
                            return_value=used), \
                 mock.patch('mpfs.core.services.disk_service.MPFSStorageService.limit',
                            return_value=limit), \
                 mock.patch('mpfs.engine.process.get_cloud_req_id',
                            return_value=ycrid), \
                 mock.patch('mpfs.frontend.api.FEATURE_TOGGLES_DISABLE_API_FOR_OVERDRAFT_PERCENTAGE', 100):
            resp = self.client.request(self.method,
                                       self.url, query={'path': '/test1.jpg'})

        assert resp.status_code == expected_code
        if expected_code == 403:
            result = json.loads(resp.content)
            assert result['error'] == 'DiskAPIDisabledForOverdraftUserError'

    def test_get_root_folder_with_limit(self):
        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.request(self.method, self.url, query={'path': '/', 'limit': 1})
        assert resp.status_code == 200
        result = json.loads(resp.content)
        self.assertEqual(len(result['_embedded']['items']), 1)

    def test_api_skip_restriction_on_some_handlers(self):
        used = 100500 * GB
        limit = 1 * GB
        ycrid = 'rest-123321-yay'

        with self.specified_client(uid=self.uid,
                                   scopes=['cloud_api:disk.read', 'cloud_api:disk.write'],
                                   id=random.choice(PLATFORM_DISK_APPS_IDS)), \
                 mock.patch('mpfs.core.services.disk_service.MPFSStorageService.used',
                            return_value=used), \
                 mock.patch('mpfs.core.services.disk_service.MPFSStorageService.limit',
                            return_value=limit), \
                 mock.patch('mpfs.engine.process.get_cloud_req_id',
                            return_value=ycrid), \
                 mock.patch('mpfs.frontend.api.FEATURE_TOGGLES_DISABLE_API_FOR_OVERDRAFT_PERCENTAGE', 100):
            resp = self.client.request(self.method, 'disk/clients/features')

        assert resp.status_code == 200

    @parameterized.expand([('non-official', ['cloud_api:disk.read'], BAD_REQUEST),
                           ('official', WebDavPermission.scopes, OK)])
    def test_photounlim_area(self, case_name, scopes, expected_code):
        self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})
        name = 'enot.jpg'
        self.upload_file(self.uid, '/photostream/%s' % name, headers=UnlimitedAreaTestCase.mobile_headers)
        with self.specified_client(scopes=scopes):
            resp = self.client.request(self.method, self.url, query={'path': 'photounlim:/enot.jpg'})
        assert resp.status_code == expected_code

    def test_folder_attributes(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': u'/disk/Музыка'})
        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.request(self.method, self.url, query={'path': '/Музыка'})
        assert resp.status_code == 200
        folder = json.loads(resp.content)
        attributes = ['name', 'path', 'created', 'modified', 'type']
        for field in attributes:
            self.assertTrue(field in folder)
            self.assertTrue(folder[field] is not None)

    def test_resource_not_found(self):
        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.request(self.method, self.url, query={'path': '/бред'})
        assert resp.status_code == 404

    def test_exif_field(self):
        self.create_user(self.uid, noemail=1)
        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.request(self.method, self.url, query={
                'path': '/test1.jpg',
                'fields': ''
            })
            assert resp.status_code == 200
            resource = json.loads(resp.content)
            assert 'exif' in resource
            assert 'date_time' in resource['exif']
            assert 'gps_latitude' not in resource['exif']
            assert 'gps_longitude' not in resource['exif']

    def test_exif_coordinates_field(self):
        self.create_user(self.uid, noemail=1)
        self.set_file_coordinates(self.uid, self.file_path)

        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.request(self.method, self.url, query={
                'path': '/test1.jpg',
                'fields': ''
            })
            assert resp.status_code == 200
            resource = json.loads(resp.content)
            assert 'exif' in resource
            assert 'gps_latitude' in resource['exif']
            assert 'gps_longitude' in resource['exif']
            assert resource['exif']['gps_latitude'] == self.default_latitude
            assert resource['exif']['gps_longitude'] == self.default_longitude

    def test_photoslice_time_field(self):
        self.create_user(self.uid, noemail=1)
        self.upload_file(self.uid, '/disk/2.jpg', file_data={'mimetype': 'image/jpeg'})
        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.request(self.method, self.url, query={
                'path': '/2.jpg',
                'fields': ''
            })
            assert resp.status_code == 200
            resource = json.loads(resp.content)
            assert 'photoslice_time' in resource

    def test_fields_qs_parameter(self):
        with self.specified_client(scopes=['cloud_api:disk.read']):
            self.create_user(self.uid, noemail=1)
            self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/files_test'})
            resp = self.client.request(self.method, self.url, query={
                'path': '/',
                'fields': 'name,_embedded.items.path,_embedded.items.type,_embedded.items.resource_id'
            })
            assert resp.status_code == 200
            resource = json.loads(resp.content)
            assert len(resource) == 2
            assert 'name' in resource
            assert '_embedded' in resource
            assert len(resource['_embedded']) == 1
            assert 'items' in resource['_embedded']
            assert len(resource['_embedded']['items'][0]) == 3
            assert 'path' in resource['_embedded']['items'][0]
            assert 'type' in resource['_embedded']['items'][0]
            assert 'resource_id' in resource['_embedded']['items'][0]

            resp = self.client.request(self.method, self.url, query={
                'path': '/asdfafdsgsdfg',
                'fields': 'name,_embedded.items.path,_embedded.items.type'
            })
            assert resp.status_code == 404
            error = json.loads(resp.content)
            assert 'error' in error
            assert 'message' in error
            assert 'description' in error

    def test_limit_0(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': u'/disk/Музыка'})
        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.request(self.method, self.url, query={'path': '/', 'limit': 0})
        assert resp.status_code == 200
        result = json.loads(resp.content)
        assert len(result['_embedded']['items']) == 0
        assert result['_embedded']['total'] == 2

    def test_visible_fields_dosent_cache_betwin_requests(self):
        self.create_user(self.uid, noemail=1)

        dir_number = 5
        for i in range(0, dir_number - 1):
            self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/files_test%s' % i})

        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.request(self.method, self.url, query={'path': '/'})
            assert resp.status_code == 200
            unaffected_result = json.loads(resp.content)

            resp = self.client.request(self.method, self.url, query={'path': '/', 'fields': '_embedded.items.path'})
            assert resp.status_code == 200

            resp = self.client.request(self.method, self.url, query={'path': '/'})
            assert resp.status_code == 200
            control_result = json.loads(resp.content)

            # проверяем, что результат третьего запроса не был зааффекчен вторым запросом с fields
            assert unaffected_result == control_result

    def test_revisions(self):
        """Проверить наличие поля revision у каждого из элементов дерева ресурсов.
        """
        with self.specified_client(scopes=['cloud_api:disk.read']):
            # Тестируем листинг файла
            resp = self.client.request(self.method, 'disk/resources', query={'path': '/test1.jpg'})
            assert resp.status_code == 200
            result = json.loads(resp.content)
            assert 'revision' in result and isinstance(result.get('revision'), int)

            # Тестируем листинг директории
            resp = self.client.request(self.method, 'disk/resources', query={'path': '/'})
            assert resp.status_code == 200
            result = json.loads(resp.content)
            assert all('revision' in item and isinstance(item.get('revision'), int)
                       for item in result['_embedded']['items'])

    def test_comment_ids_on_folder(self):
        u"""Проверить наличие поля comment_ids в ответах Платформы для папки
        """
        path = '/'
        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.request(self.method, self.url, query={'path': path})
        assert resp.status_code == 200
        result = json.loads(resp.content)
        assert 'comment_ids' in result
        assert_that(result['comment_ids'], is_(empty()))

    def test_comment_ids_on_file(self):
        u"""Проверить наличие поля comment_ids в ответах Платформы для файла
        """
        path = '/test1.jpg'
        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.request(self.method, self.url, query={'path': path})
        assert resp.status_code == 200
        result = json.loads(resp.content)
        assert 'comment_ids' in result
        assert_that(result['comment_ids'], has_keys(['private_resource', 'public_resource']))

    def test_autouploaded_field_doesnt_show_in_ext_api(self):
        # загрузка в фотокамеру
        self.upload_file(self.uid, '/photostream/1.jpg')

        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.request(self.method, self.url, query={'path': '/Фотокамера/1.jpg'})
        assert resp.status_code == 200

        result = json.loads(resp.content)
        assert 'autouploaded' not in result


class GetResourcesTestCaseInternal(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'
    method = 'GET'
    url = 'disk/resources'

    def setup_method(self, method):
        super(GetResourcesTestCaseInternal, self).setup_method(method)
        self.create_user(self.uid, noemail=1)

    def test_autouploaded_field_showed_in_int_api(self):
        # загрузка в фотокамеру
        self.upload_file(self.uid, '/photostream/1.jpg')

        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.request(self.method, self.url, query={'path': '/Фотокамера/1.jpg'})
        assert resp.status_code == 200

        result = json.loads(resp.content)
        assert 'autouploaded' in result


class ResourceSerializerTestCase(TestCase):
    def test_owner_group(self):
        obj = {
            'type': 'dir',
            'name': 'test',
            'path': '/disk/test',
            'ctime': 1333569600,
            'mtime': 1333569600,
            'meta': {
                'group': {'is_owned': True, 'rights': '660'}
            }
        }
        self.assertEqual(
            {'is_owned': True, 'rights': 'rw'},
            ResourceSerializer(obj=obj).data.get('share', {})
        )


class CustomPreviewsTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    method = 'GET'
    file_name = 'test1.jpg'
    dir_name = 'test'
    mpfs_dir_path = '/disk/%s' % (dir_name,)
    api_dir_path = 'disk:/%s' % (dir_name,)
    mpfs_file_path = '%s/%s' % (mpfs_dir_path, file_name,)
    api_file_path = '%s/%s' % (api_dir_path, file_name,)

    def setup_method(self, method):
        super(CustomPreviewsTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)
        file_data = {'mimetype': 'image/jpg'}
        self.json_ok('mkdir', opts={'uid': self.uid, 'path': self.mpfs_dir_path})
        self.json_ok('set_public', opts={'uid': self.uid, 'path': self.mpfs_dir_path})
        public_dir = self.json_ok('info',
                                  opts={'uid': self.uid, 'path': self.mpfs_dir_path, 'meta': ''})
        self.private_hash = public_dir['meta']['public_hash']
        self.upload_file(self.uid, self.mpfs_file_path, file_data=file_data)
        self.json_ok('set_public', opts={'uid': self.uid, 'path': self.mpfs_file_path})

    def test_default_preview_in_resource(self):
        """Превьюшка поумолчанию должна быть S'кой."""
        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.request(self.method,
                                       'disk/resources',
                                       query={'path': self.api_file_path})
            result = json.loads(resp.content)
            assert 'size=S' in result['preview']

    def test_custom_previews_in_resource(self):
        """Кастомная превьюшка должна быть такой, какой мы её зададим."""
        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.request(self.method,
                                       'disk/resources',
                                       query={'path': self.api_file_path, 'preview_size': 'L'})
            result = json.loads(resp.content)
            assert 'size=L' in result['preview']

    def test_custom_previews_in_resources_list(self):
        """Кастомные превьюшки в списке должны быть такими, какими мы их зададим."""
        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.request(self.method,
                                       'disk/resources',
                                       query={'path': self.api_dir_path, 'preview_size': 'XL'})
            result = json.loads(resp.content)
            assert 'size=XL' in result['_embedded']['items'][-1]['preview']

    def test_custom_previews_in_last_uploaded_list(self):
        """Кастомные превьюшки в списке последних загруженных должны быть такими, какими мы их зададим."""
        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.request(self.method,
                                       'disk/resources/last-uploaded',
                                       query={'path': self.api_file_path, 'preview_size': 'L'})
            result = json.loads(resp.content)
            assert 'size=L' in result['items'][0]['preview']

    def test_custom_previews_in_files_list(self):
        """Кастомные превьюшки в списке файлов должны быть такими, какими мы их зададим."""
        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.request(self.method,
                                       'disk/resources/files',
                                       query={'path': self.api_file_path, 'preview_size': 'XXL'})
            result = json.loads(resp.content)
            assert 'size=XXL' in result['items'][0]['preview']

    def test_custom_previews_in_public_resource(self):
        """Кастомная превьюшка публичного ресурса должна быть такой, какой мы её зададим."""
        resp = self.client.request(self.method,
                                   'disk/public/resources',
                                   query={'public_key': self.private_hash,
                                          'path': '/%s' % self.file_name,
                                          'preview_size': 'M'})
        result = json.loads(resp.content)
        assert 'size=M' in result['preview']

    def test_custom_previews_in_public_resources_list(self):
        """Кастомные превьюшки в списке содержимого публичного ресурса должны быть такими, какими мы их зададим."""
        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.request(self.method,
                                       'disk/public/resources',
                                       query={'public_key': self.private_hash,
                                              'preview_size': 'XXXL'})
            result = json.loads(resp.content)
            assert 'size=XXXL' in result['_embedded']['items'][-1]['preview']

    def test_custom_previews_in_resources_public(self):
        """Кастомные превьюшки в списке публичных ресурсов должны быть такими, какими мы их зададим."""
        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.request(self.method,
                                       'disk/resources/public',
                                       query={'preview_size': 'XXXL'})
            result = json.loads(resp.content)
            assert 'size=XXXL' in result['items'][0]['preview']

    def test_numeric_custom_preview(self):
        """Тест превьюшек с числовыми размерами"""
        with self.specified_client(scopes=['cloud_api:disk.read']):
            for preview_size in ('666', '666x666', '666x', 'x666'):
                resp = self.client.request(self.method,
                                           'disk/resources',
                                           query={'path': self.api_file_path,
                                                  'preview_size': preview_size})
                result = json.loads(resp.content)
                assert 'size=%s' % preview_size in result['preview']
            resp = self.client.request(self.method,
                                       'disk/resources',
                                       query={'path': self.api_file_path, 'preview_size': '4e3'})
            result = json.loads(resp.content)
            assert result['error'] == u'FieldValidationError'

    def test_preview_crop(self):
        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.request(self.method,
                                       'disk/resources',
                                       query={'path': self.api_file_path, 'preview_crop': '1'})
            result = json.loads(resp.content)
            assert 'crop=1' in result['preview']
            resp = self.client.request(self.method,
                                       'disk/resources',
                                       query={'path': self.api_file_path, 'preview_crop': ''})
            result = json.loads(resp.content)
            assert 'crop=0' in result['preview']

    def test_allow_big_size_preview(self):
        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.request(self.method, 'disk/resources',
                                       query={'path': self.api_file_path, 'preview_allow_big_size': '1'})
            result = json.loads(resp.content)
            assert 'allow_big_size=1' not in result['preview']

        with self.specified_client(scopes=['yadisk:all']):
            resp = self.client.request(self.method, 'disk/resources',
                                       query={'path': self.api_file_path, 'preview_allow_big_size': '1'})
            result = json.loads(resp.content)
            assert 'allow_big_size=1' in result['preview']

    def test_preview_quality(self):
        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.request(self.method, 'disk/resources',
                                       query={'path': self.api_file_path, 'preview_quality': 50})
            result = json.loads(resp.content)
            assert 'quality=50' not in result['preview']

        with self.specified_client(scopes=['yadisk:all']):
            resp = self.client.request(self.method, 'disk/resources',
                                       query={'path': self.api_file_path, 'preview_quality': 50})
            result = json.loads(resp.content)
            assert 'quality=50' in result['preview']


class GetResourcesFilesTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    method = 'GET'
    url = 'disk/resources/files'

    def setup_method(self, method):
        super(GetResourcesFilesTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)
        self._create_resources()

    def _create_resources(self):
        """Создать ресрусы разного типа, чтобы они образовывали вложенности.
        """
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/files_test'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/music'})

        file_data = {'mimetype': 'image/jpg'}
        self.upload_file(self.uid, '/disk/files_test/test1.jpg', file_data=file_data)
        self.upload_file(self.uid, '/disk/test2.jpg', file_data=file_data)
        self.upload_file(self.uid, '/disk/music/song.mp3')
        self.upload_file(self.uid, '/disk/note.txt')

    def test_files(self):
        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.request(self.method,
                                       self.url,
                                       query={'path': '/', 'media_type': 'image'})
            assert resp.status_code == 200
            result = json.loads(resp.content)
            assert 'limit' in result
            assert 'offset' in result
            assert [x for x in result['items'] if x.get('name') == 'test1.jpg']
            assert [x for x in result['items'] if x.get('name') == 'test2.jpg']
            assert not [x for x in result['items'] if x.get('name') == 'song.mp3']

            # тест на список медиатипов
            resp = self.client.request(self.method,
                                       self.url,
                                       query={'path': '/', 'media_type': 'image,audio'})
            result = json.loads(resp.content)
            assert [x for x in result['items'] if x.get('name') == 'test1.jpg']
            assert [x for x in result['items'] if x.get('name') == 'test2.jpg']
            assert [x for x in result['items'] if x.get('name') == 'song.mp3']
            assert not [x for x in result['items'] if x.get('name') == 'note.txt']

            # limit, offset test
            resp = self.client.request(self.method,
                                       self.url,
                                       query={'path': '/', 'limit': 1, 'offset': 1})
            result = json.loads(resp.content)
            assert result['limit'] == 1
            assert result['offset'] == 1
            assert len(result['items']) == 1

            # sort test
            resp = self.client.request(self.method,
                                       self.url,
                                       query={'path': '/', 'media_type': 'image', 'sort': 'path'})
            result = json.loads(resp.content)
            paths = [item['path'] for item in result['items']]
            sorted_paths = sorted(paths)
            assert paths == sorted_paths

    def test_revisions(self):
        """Проверить наличие поля revision у каждого из элементов плоского списка ресурсов.
        """
        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.request(self.method, self.url, query={'path': '/'})
            assert resp.status_code == 200

            result = json.loads(resp.content)
            assert all('revision' in item for item in result['items'])


class DeleteResourcesTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    method = 'DELETE'
    url = 'disk/resources'

    def __init__(self, *args, **kwargs):
        super(DeleteResourcesTestCase, self).__init__(*args, **kwargs)
        self.dir_path = '/%s' % type(self).__name__

    def setup_method(self, method):
        super(DeleteResourcesTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)

    def test_permissions(self):
        # Пытаемся удалить папку из Диска
        self.json_ok('mkdir', {'uid': self.uid, 'path': '%s:/disk%s' % (self.uid, self.dir_path)})
        query = {'path': self.dir_path}
        scopes_to_status = (
            ([], 403),
            (['cloud_api:disk.read'], 403),
            (['cloud_api:disk.app_folder'], 403),
            (['cloud_api:disk.write'], 204),
        )
        self._permissions_test(scopes_to_status, self.method, self.url, query=query)

        # Пытаемся удалить из папки приложения
        path = 'app:%s' % self.dir_path
        with self.specified_client(scopes=['cloud_api:disk.app_folder']):
            resp = self.client.put(self.url, query={'path': path})
            self.assertEqual(resp.status_code, 201)
        query = {'path': path}
        scopes_to_status = (
            ([], 403),
            (['cloud_api:disk.read'], 403),
            (['cloud_api:disk.app_folder'], 204),
        )
        self._permissions_test(scopes_to_status, self.method, self.url, query=query)

    def test_on_empty_directory(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '%s:/disk%s' % (self.uid, self.dir_path)})

        with self.specified_client(scopes=['cloud_api:disk.write']):
            resp = self.client.request(self.method, self.url, query={'path': self.dir_path})
            self.assertEqual(resp.status_code, 204)

    def test_on_file(self):
        file_path = '/test_operation_on_file.txt'
        self.upload_file(self.uid, '/disk%s' % file_path)

        with self.specified_client(scopes=['cloud_api:disk.write']):
            resp = self.client.request(self.method, self.url, query={'path': file_path})
            self.assertEqual(resp.status_code, 204)

    def test_request_for_noninit_user_with_passport_bad_response(self):
        uid = '123456789'
        user_info = deepcopy(DEFAULT_USERS_INFO[self.uid])
        user_info['uid'] = uid
        with self.specified_client(scopes=['yadisk:all'], uid=uid, id=settings.platform['disk_apps_ids'][0]):
            with PassportStub(userinfo=user_info) as stub:
                stub.subscribe.side_effect = passport.errors_map['accountwithpasswordrequired']
                response = self.client.request(self.method, self.url, uid=uid, query={'path': '/test.txt'})
                assert response.status_code == 403
                content = json.loads(response.content)
                assert content['error'] == 'DiskUnsupportedUserAccountTypeError'
                assert content['description'] == 'User account type is not supported.'

    def test_request_for_noninit_user_with_disk_apps(self):
        uid = '123456789'
        user_info = deepcopy(DEFAULT_USERS_INFO[self.uid])
        user_info['uid'] = uid
        with self.specified_client(scopes=['yadisk:all'], uid=uid, id=settings.platform['disk_apps_ids'][0]):
            with PassportStub(userinfo=user_info):
                response = self.client.request(self.method, self.url, uid=uid, query={'path': '/test.txt'})
                # init and retry with _auto_initialize_user
                assert response.status_code == 404

    def test_on_not_empty_directory(self):
        file_path = '%s/test_operation_on_not_empty_directory.txt' % self.dir_path

        self.json_ok('mkdir', {'uid': self.uid, 'path': '%s:/disk%s' % (self.uid, self.dir_path)})
        self.upload_file(self.uid, '/disk%s' % file_path)

        # Защита от рекурсивного удаления отключена.
        # resp = self.client.request(self.method,
        #                            self.url,
        #                            query={'path': self.dir_path}, uid=self.uid)
        # self.assertEqual(resp.status_code, 400)

        with self.specified_client(scopes=['cloud_api:disk.write']):
            resp = self.client.request(self.method, self.url, query={'path': self.dir_path})
            self.assertEqual(resp.status_code, 202)

        # check operation
        resp = json.loads(resp.content)
        self.assertTrue('href' in resp)
        operation_url = resp['href']
        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.get(operation_url)
            self.assertEqual(resp.status_code, 200)

    def test_on_directory_with_md5(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '%s:/disk%s' % (self.uid, self.dir_path)})
        with self.specified_client(scopes=['cloud_api:disk.write']):
            resp = self.client.request(self.method, self.url, query={'path': self.dir_path, 'md5': '123'})
            self.assertEqual(resp.status_code, 400)

    def test_on_file_with_md5(self):
        md5 = 'd41d8cd98f00b204e9800998ecf8427e'
        file_path = '/test_operation_on_file.txt'
        self.upload_file(self.uid, '/disk%s' % file_path, file_data={'md5': md5})
        with self.specified_client(scopes=['cloud_api:disk.write']):
            resp = self.client.request(self.method, self.url, query={'path': file_path, 'md5': md5})
            self.assertEqual(resp.status_code, 204)

    def test_on_file_with_wrong_nd5(self):
        file_path = '/test_operation_on_file.txt'
        self.upload_file(self.uid, '/disk%s' % file_path)
        with self.specified_client(scopes=['cloud_api:disk.write']):
            resp = self.client.request(self.method, self.url, query={'path': file_path, 'md5': '123'})
            self.assertEqual(resp.status_code, 409)


class DeleteResourcesByResourceIdTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    method = 'DELETE'
    base_url = 'disk/resources/%s/'

    def __init__(self, *args, **kwargs):
        super(DeleteResourcesByResourceIdTestCase, self).__init__(*args, **kwargs)
        self.dir_path = '/%s' % type(self).__name__

    def setup_method(self, method):
        super(DeleteResourcesByResourceIdTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)

    def test_permissions(self):
        file_path = '/test_operation_on_file.txt'
        self.upload_file(self.uid, '/disk%s' % file_path)
        resource_id = self.json_ok('info', {'uid': self.uid, 'path': '%s:/disk%s' % (self.uid, file_path), 'meta': 'resource_id'})['meta']['resource_id']
        url = self.base_url % resource_id
        scopes_to_status = (
            ([], 403),
            (['cloud_api:disk.read'], 403),
            (['cloud_api:disk.app_folder'], 403),
            (['cloud_api:disk.write'], 403),
            (['yadisk:all'], 204),
        )
        self._permissions_test(scopes_to_status, self.method, url)

    def test_directory_deletion_is_forbidden(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '%s:/disk%s' % (self.uid, self.dir_path)})
        resource_id = self.json_ok('info', {'uid': self.uid, 'path': '%s:/disk%s' % (self.uid, self.dir_path),'meta': 'resource_id'})['meta']['resource_id']
        url = self.base_url % resource_id

        with self.specified_client(scopes=['yadisk:all']):
            resp = self.client.request(self.method, url)
            self.assertEqual(resp.status_code, 400)

    def test_on_file(self):
        file_path = '/test_operation_on_file.txt'
        self.upload_file(self.uid, '/disk%s' % file_path)
        resource_id = self.json_ok('info', {'uid': self.uid, 'path': '/disk%s' % file_path, 'meta': 'resource_id'})['meta']['resource_id']
        url = self.base_url % resource_id

        with self.specified_client(scopes=['yadisk:all']):
            resp = self.client.request(self.method, url)
            self.assertEqual(resp.status_code, 204)

    def test_delete_nonexisting_file_returns_404(self):
        resource_id = ':'.join([self.uid, '1' * 64])
        url = self.base_url % resource_id

        with self.specified_client(scopes=['yadisk:all']):
            resp = self.client.request(self.method, url)
            self.assertEqual(resp.status_code, 404)

    @parameterized.expand([
        ('a' * 32, None, None, None, None),
        (None, 'a' * 64, None, None, None),
        (None, None, 1, None, None),
        (None, None, None, True, None),
        (None, None, None, False, None),
        (None, None, None, True, True),
        (None, None, None, True, False),
        (None, None, None, False, True),
        (None, None, None, False, False),
    ])
    def test_parameters(self, md5, sha256, size, delete_all, permanently):
        query = {}
        if md5:
            query['md5'] = md5
        if sha256:
            query['sha256'] = sha256
        if size:
            query['size'] = size
        if delete_all is not None:
            query['delete_all'] = delete_all
        if permanently is not None:
            query['permanently'] = permanently

        url = self.base_url % ':'.join(['111', '1' * 64])
        if query:
            url += '?' + urllib.urlencode(query)

        with self.specified_client(scopes=['yadisk:all']), \
                mock.patch('mpfs.core.base.info_by_resource_id'), \
                mock.patch('mpfs.frontend.formatter.disk.MPFSFormatter._info', return_value={'type': 'file'}), \
                mock.patch('mpfs.core.base.trash_append_by_resource_id') as mpfs_trash_append_mock, \
                mock.patch('mpfs.core.base.rm_by_resource_id') as mpfs_rm_mock:
            self.client.request(self.method, url)
            if permanently:
                mock_to_check = mpfs_rm_mock
            else:
                mock_to_check = mpfs_trash_append_mock

            assert mock_to_check.called
            assert (not md5 and not mock_to_check.call_args[0][0].md5) or md5 == mock_to_check.call_args[0][0].md5
            assert (not sha256 and not mock_to_check.call_args[0][0].sha256) or sha256 == mock_to_check.call_args[0][0].sha256
            assert (not size and not mock_to_check.call_args[0][0].size) or size == int(mock_to_check.call_args[0][0].size)
            if permanently:
                assert bool(delete_all) == bool(mpfs_rm_mock.call_args[0][0].rm_all)
            else:
                assert bool(delete_all) == bool(mpfs_trash_append_mock.call_args[0][0].append_all)


class Base(object):
    """Класс-обёртка, позволяющий спрятать от автопоиска тестов базовый класс."""
    class MoveCopyBaseTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
        api_mode = tags.platform.EXTERNAL
        method = 'POST'
        url = None

        def __init__(self, *args, **kwargs):
            super(Base.MoveCopyBaseTestCase, self).__init__(*args, **kwargs)
            self.dir_path = '/%s' % type(self).__name__

        def setup_method(self, method):
            super(Base.MoveCopyBaseTestCase, self).setup_method(method)
            self.create_user(self.uid)

        def test_on_empty_directory(self):
            self.json_ok('mkdir',
                         {'uid': self.uid, 'path': '%s:/disk%s' % (self.uid, self.dir_path)})

            with self.specified_client(scopes=['cloud_api:disk.read', 'cloud_api:disk.write']):
                resp = self.client.request(self.method,
                                           self.url,
                                           query={'from': self.dir_path,
                                                  'path': '%s-dst' % self.dir_path})
                self.assertEqual(resp.status_code, 201)

        def test_on_file(self):
            file_path = '/test_on_file.txt'
            self.upload_file(self.uid, '/disk%s' % file_path)

            with self.specified_client(scopes=['cloud_api:disk.read', 'cloud_api:disk.write']):
                resp = self.client.request(self.method,
                                           self.url,
                                           query={'from': file_path, 'path': '%s-dst' % file_path})
                self.assertEqual(resp.status_code, 201)

        def test_on_not_empty_directory(self):
            file_path = '%s/test_on_not_empty_directory.txt' % self.dir_path

            self.json_ok('mkdir',
                         {'uid': self.uid, 'path': '%s:/disk%s' % (self.uid, self.dir_path)})
            self.upload_file(self.uid, '/disk%s' % file_path)

            query = {'from': self.dir_path, 'path': '%s-dst' % self.dir_path}
            with self.specified_client(scopes=['cloud_api:disk.read', 'cloud_api:disk.write']):
                # Защита от рекурсивного удаления отключена.
                # resp = self.client.request(self.method, self.url, query=query, uid=self.uid)
                # self.assertEqual(resp.status_code, 400)
                # query['recursive'] = True
                resp = self.client.request(self.method, self.url, query=query)
                self.assertEqual(resp.status_code, 202)

                # check operation
                resp = json.loads(resp.content)
                self.assertTrue('href' in resp)
                operation_url = resp['href']
                resp = self.client.get(operation_url)
                self.assertEqual(resp.status_code, 200)

        def test_on_existent_resource(self):
            self.json_ok('mkdir',
                         {'uid': self.uid, 'path': '%s:/disk%s' % (self.uid, self.dir_path)})
            self.json_ok('mkdir',
                         {'uid': self.uid, 'path': '%s:/disk%s-dst' % (self.uid, self.dir_path)})

            with self.specified_client(scopes=['cloud_api:disk.read', 'cloud_api:disk.write']):
                resp = self.client.request(self.method,
                                           self.url,
                                           query={'from': self.dir_path,
                                                  'path': '%s-dst' % self.dir_path})
                self.assertEqual(resp.status_code, 409)

                resp = self.client.request(self.method,
                                           self.url,
                                           query={'from': self.dir_path,
                                                  'path': '%s-dst' % self.dir_path,
                                                  'overwrite': True})
                self.assertEqual(resp.status_code, 201)


class PostResourcesCopyTestCase(Base.MoveCopyBaseTestCase, CommonSharingMethods):
    method = 'POST'
    url = 'disk/resources/copy'

    def test_permissions(self):
        # Пытаемся скопировать папку из Диска в Диск
        self.json_ok('mkdir', {'uid': self.uid, 'path': '%s:/disk%s' % (self.uid, self.dir_path)})
        path = '%s-dst' % self.dir_path
        query = {'from': self.dir_path, 'path': path}
        scopes_to_status = (
            ([], 403),
            (['cloud_api:disk.read'], 403),
            (['cloud_api:disk.write'], 403),
            (['cloud_api:disk.app_folder'], 403),
            (['cloud_api:disk.app_folder', 'cloud_api:disk.write'], 403),
            (['cloud_api:disk.app_folder', 'cloud_api:disk.read'], 403),
            (['cloud_api:disk.write', 'cloud_api:disk.read'], 201),
        )
        self._permissions_test(scopes_to_status, self.method, self.url, query=query)

        # Пытаемся скопировать папку из Диска в папку приложения
        app_path = 'app:/%s' % type(self).__name__
        query = {'from': self.dir_path, 'path': app_path}
        scopes_to_status = (
            ([], 403),
            (['cloud_api:disk.app_folder'], 403),
            (['cloud_api:disk.app_folder', 'cloud_api:disk.write'], 403),
            (['cloud_api:disk.app_folder', 'cloud_api:disk.read'], 201),
        )
        self._permissions_test(scopes_to_status, self.method, self.url, query=query)

        # Пытаемся скопировать из папки приложения в Диск
        path = '%s-dst-from-app' % self.dir_path
        query = {'from': app_path, 'path': path}
        scopes_to_status = (
            ([], 403),
            (['cloud_api:disk.app_folder'], 403),
            (['cloud_api:disk.app_folder', 'cloud_api:disk.read'], 403),
            (['cloud_api:disk.app_folder', 'cloud_api:disk.write'], 201),
        )
        self._permissions_test(scopes_to_status, self.method, self.url, query=query)

        # Пытаемся скопировать из папки приложения в папку приложения
        path = '%s-dst-from-app' % app_path
        query = {'from': app_path, 'path': path}
        scopes_to_status = (
            ([], 403),
            (['cloud_api:disk.read'], 403),
            (['cloud_api:disk.app_folder'], 201),
        )
        self._permissions_test(scopes_to_status, self.method, self.url, query=query)

    @parameterized.expand([
        ('owner', 0, 10 * GB, DiskOwnerStorageQuotaExhaustedError),
        ('invited', 10*GB, 0, DiskStorageQuotaExhaustedError),
    ])
    def test_error_when_no_free_space(self, case_name, owner_free_space, invited_free_space, expected_error):
        u"""Проверяет возможность загрузить файл в расшаренную папку"""
        invited_uid = user_6.uid
        self.json_ok('user_init', {'uid': invited_uid})
        owner_folder_path = '/disk/external/shared_folder'
        src_file_path = '/1.jpg'
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/external'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': owner_folder_path})

        hsh = self.invite_user(uid=invited_uid, owner=self.uid,
                               email=user_6.email, rights=660, path=owner_folder_path)
        self.activate_invite(uid=invited_uid, hash=hsh)
        self.upload_file(invited_uid, '/disk%s' % src_file_path)

        dst_folder_path = 'disk:/shared_folder'
        if expected_error == DiskStorageQuotaExhaustedError:
            dst_folder_path = 'disk:'

        with self.specified_client(uid=invited_uid,
                                   scopes=['cloud_api:disk.read', 'cloud_api:disk.write']), \
                 mock.patch('mpfs.core.services.disk_service.MPFSStorageService.free',
                            side_effect=lambda uid: owner_free_space if uid == self.uid else invited_free_space):
            resp = self.client.request(self.method,
                                       self.url,
                                       query={'from': src_file_path,
                                              'path': '%s/2.jpg' % dst_folder_path})
            assert resp.status_code == 507
            assert from_json(resp.content)['error'] == expected_error.__name__


class PostResourcesMoveTestCase(Base.MoveCopyBaseTestCase, CommonSharingMethods):
    method = 'POST'
    url = 'disk/resources/move'

    def test_permissions(self):
        # Пытаемся переместить папку из Диска в Диск
        self.json_ok('mkdir', {'uid': self.uid, 'path': '%s:/disk%s' % (self.uid, self.dir_path)})
        path = '%s-dst' % self.dir_path
        query = {'from': self.dir_path, 'path': path}
        scopes_to_status = (
            ([], 403),
            (['cloud_api:disk.read'], 403),
            (['cloud_api:disk.write'], 403),
            (['cloud_api:disk.app_folder'], 403),
            (['cloud_api:disk.app_folder', 'cloud_api:disk.write'], 403),
            (['cloud_api:disk.app_folder', 'cloud_api:disk.read'], 403),
            (['cloud_api:disk.write', 'cloud_api:disk.read'], 201),
        )
        self._permissions_test(scopes_to_status, self.method, self.url, query=query)

        # Пытаемся переместить папку из Диска в папку приложения
        app_path = 'app:/%s' % type(self).__name__
        query = {'from': path, 'path': app_path}
        scopes_to_status = (
            ([], 403),
            (['cloud_api:disk.app_folder'], 403),
            (['cloud_api:disk.app_folder', 'cloud_api:disk.write'], 403),
            (['cloud_api:disk.app_folder', 'cloud_api:disk.read'], 403),
            (['cloud_api:disk.write', 'cloud_api:disk.read'], 201),
        )
        self._permissions_test(scopes_to_status, self.method, self.url, query=query)

        # Пытаемся переместить из папки приложения в Диск
        path = '%s-dst-from-app' % self.dir_path
        query = {'from': app_path, 'path': path}
        scopes_to_status = (
            ([], 403),
            (['cloud_api:disk.app_folder'], 403),
            (['cloud_api:disk.write'], 403),
            (['cloud_api:disk.app_folder', 'cloud_api:disk.read'], 403),
            (['cloud_api:disk.app_folder', 'cloud_api:disk.write'], 201),
        )
        self._permissions_test(scopes_to_status, self.method, self.url, query=query)

        # Пытаемся переместить из папки приложения в папку приложения
        with self.specified_client(scopes=['cloud_api:disk.read', 'cloud_api:disk.write']):
            self.client.post(self.url, query={'from': path, 'path': app_path})
        path = '%s-dst-from-app' % app_path
        query = {'from': app_path, 'path': path}
        scopes_to_status = (
            ([], 403),
            (['cloud_api:disk.read'], 403),
            (['cloud_api:disk.write'], 403),
            (['cloud_api:disk.app_folder'], 201),
        )
        self._permissions_test(scopes_to_status, self.method, self.url, query=query)

    @parameterized.expand([
        ('owner', 0, 10 * GB, DiskOwnerStorageQuotaExhaustedError),
        ('invited', 10 * GB, 0, DiskStorageQuotaExhaustedError),
    ])
    def test_error_when_no_free_space_on_async_operation(self, case_name, owner_free_space,
                                                         invited_free_space, expected_error):
        u"""Проверяет возможность выполнить асинхронную операцию при нехватке места"""
        invited_uid = user_6.uid
        self.json_ok('user_init', {'uid': invited_uid})
        owner_folder_path = '/disk/external/shared_folder'
        src_path = '/dir'
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/external'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': owner_folder_path})
        self.json_ok('mkdir', {'uid': invited_uid, 'path': '/disk%s' % src_path})

        hsh = self.invite_user(uid=invited_uid, owner=self.uid,
                               email=user_6.email, rights=660, path=owner_folder_path)
        self.activate_invite(uid=invited_uid, hash=hsh)
        self.upload_file(invited_uid, '/disk%s/1.jpg' % src_path)

        dst_path = 'disk:/shared_folder/new_dir'

        with self.specified_client(uid=invited_uid,
                                   scopes=['cloud_api:disk.read', 'cloud_api:disk.write']), \
             mock.patch('mpfs.core.services.disk_service.MPFSStorageService.free',
                        side_effect=lambda uid: owner_free_space if uid == self.uid else invited_free_space):
            resp = self.client.request(self.method,
                                       self.url,
                                       query={'from': 'disk:%s' % src_path,
                                              'path': dst_path})
            operation_id = from_json(resp.content)['href'].rsplit('/', 1)[1]
            response = self.client.get('disk/operations/%s' % operation_id)
            data = from_json(response.content)
            if expected_error == DiskOwnerStorageQuotaExhaustedError:
                assert 'error_data' in data
                assert data['error_data']['error'] == expected_error.__name__
            else:
                assert 'error_data' not in data

    @parameterized.expand([('non-official', ['cloud_api:disk.read'], BAD_REQUEST),
                           ('official', WebDavPermission.scopes, CREATED)])
    def test_move_in_photounlim_area(self, case_name, scopes, expected_code):
        self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})
        name = 'enot.jpg'
        self.upload_file(self.uid, '/photostream/%s' % name, headers=UnlimitedAreaTestCase.mobile_headers)
        with self.specified_client(scopes=scopes):
            resp = self.client.request(self.method, self.url,
                                       query={'from': 'photounlim:/enot.jpg', 'path': 'photounlim:/enot2.jpg'})
        assert resp.status_code == expected_code

    def test_move_to_wrong_designation_error_code(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/1'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/2'})
        self.json_ok('share_create_group', {'uid': self.uid, 'path': '/disk/1/'})
        self.json_ok('share_create_group', {'uid': self.uid, 'path': '/disk/2/'})
        with self.specified_client(uid=self.uid, scopes=['cloud_api:disk.read', 'cloud_api:disk.write']):
            resp = self.client.request(self.method, 'disk/resources/move', query={'from': '/1', 'path': '/2/1'})
            result = json.loads(resp.content)
            assert resp.status_code == 403
            assert result['error'] == 'DiskMoveWrongDestinationError'

    def test_move_folder_to_itself_error_code(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/1'})
        with self.specified_client(uid=self.uid, scopes=['cloud_api:disk.read', 'cloud_api:disk.write']):
            resp = self.client.request(self.method, 'disk/resources/move', query={'from': '/1', 'path': '/1/1'})
            assert resp.status_code == 409


class GetResourceDownloadTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    method = 'GET'
    url = 'disk/resources/download'

    def setup_method(self, method):
        super(GetResourceDownloadTestCase, self).setup_method(method)
        # создаём пользователя, т.к. self.upload_file не умеет автоинициализацию пользователей
        self.create_user(self.uid)

    def test_get_link_for_file(self):
        self.upload_file(self.uid, '/disk/somefile')
        # Check zaberun redirect is returned on GET .../file
        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.request(self.method, self.url, query={'path': '/somefile'})
        assert resp.status_code == 200
        href = json.loads(resp.content)['href']
        self.assertTrue(re.match(r'^https?://downloader', href))


class GetPhotounlimResourceDownloadTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    method = 'GET'
    url = 'disk/photounlim/resources/download'

    def setup_method(self, method):
        super(GetPhotounlimResourceDownloadTestCase, self).setup_method(method)
        self.create_user(self.uid)
        self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})
        self.name = 'enot.jpg'
        self.upload_file(self.uid, '/photostream/%s' % self.name, headers=UnlimitedAreaTestCase.mobile_headers)

    def test_get_link_for_file(self):
        # Check zaberun redirect is returned on GET .../file
        with self.specified_client(scopes=WebDavPermission.scopes):
            resp = self.client.request(self.method, self.url, query={'path': 'photounlim:/%s' % self.name})
        assert resp.status_code == 200
        href = json.loads(resp.content)['href']
        self.assertTrue(re.match(r'^https?://downloader', href))


class GetPhotounlimResourceDownloadByResourceIDTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    method = 'GET'
    url = 'disk/resources/%s/download'

    def setup_method(self, method):
        super(GetPhotounlimResourceDownloadByResourceIDTestCase, self).setup_method(method)
        self.create_user(self.uid)
        self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})
        file_name = 'enot.jpg'
        file_path = '%s/%s' % (PHOTOUNLIM_AREA_PATH, file_name)
        self.upload_file(self.uid, '/photostream/%s' % file_name, headers=UnlimitedAreaTestCase.mobile_headers)
        response = self.json_ok('info', {'uid': self.uid, 'path': file_path, 'meta': ''})
        self.resource_id = response['meta']['resource_id']

    def test_get_link_for_file_by_resource_id(self):
        with self.specified_client(scopes=WebDavPermission.scopes):
            resp = self.client.request(self.method, self.url % self.resource_id)
        assert resp.status_code == 200
        href = json.loads(resp.content)['href']
        self.assertTrue(re.match(r'^https?://downloader', href))


class UploadResourceTestCase(BillingApiTestCaseMixin, UserTestCaseMixin,
                             SupportApiTestCaseMixin,
                             UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    dir_path = '/test_get_upload'
    file_path = '%s/test_get_upload.txt' % dir_path
    url = 'disk/resources/upload'

    def setup_method(self, method):
        super(UploadResourceTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)

    def test_api_restrictions_for_overdraft_users(self):
        with self.specified_client(uid=self.uid,
                                   scopes=['cloud_api:disk.read', 'cloud_api:disk.write']), \
             mock.patch('mpfs.core.services.disk_service.MPFSStorageService.used',
                        return_value=13*GB), \
             mock.patch('mpfs.core.services.disk_service.MPFSStorageService.limit',
                        return_value=10*GB), \
             mock.patch('mpfs.engine.process.get_cloud_req_id',
                        return_value='rest-123-meow'), \
             mock.patch('mpfs.frontend.api.FEATURE_TOGGLES_DISABLE_API_FOR_OVERDRAFT_PERCENTAGE', 100):
            resp = self.client.get(self.url, query={'path': '/test1.jpg'})
        assert resp.status_code == 403
        result = json.loads(resp.content)
        assert result['error'] == 'DiskAPIDisabledForOverdraftUserError'

    def test_without_path(self):
        with self.specified_client(scopes=['yadisk:all']):
            resp = self.client.put(self.url)
            self.assertEqual(resp.status_code, 400)

    def test_upload_file_to_directory_path(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '%s:/disk%s' % (self.uid, self.dir_path)})

        with self.specified_client(scopes=['yadisk:all']):
            resp = self.client.put(self.url, query={'path': self.dir_path})
            self.assertEqual(resp.status_code, 409)

    def test_upload_file_to_non_existent_directory(self):
        with self.specified_client(scopes=['yadisk:all']):
            resp = self.client.get(self.url, query={'path': self.file_path})
            self.assertEqual(resp.status_code, 409)

    def test_upload_existent_file(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '%s:/disk%s' % (self.uid, self.dir_path)})
        self.upload_file(self.uid, '/disk%s' % self.file_path)

        with self.specified_client(scopes=['yadisk:all']):
            resp = self.client.put(self.url, query={'path': self.file_path})
            self.assertEqual(resp.status_code, 409)

            resp = self.client.put(self.url, query={'path': self.file_path, 'overwrite': 1})
            self.assertEqual(resp.status_code, 200)

    def test_upload_when_storage_quota_exhausted(self):
        with self.specified_client(scopes=['yadisk:all']), \
             mock.patch('mpfs.core.services.disk_service.MPFSStorageService.free', return_value=0):
            resp = self.client.put(self.url, query={'path': '/test_storage_quota_exhausted.txt'})
            self.assertEqual(resp.status_code, 507)

    def test_upload_when_storage_quota_exhausted_with_skip_space_check(self):
        with self.specified_client(id=settings.platform['skip_check_space']['allowed_client_ids'][0],
                                   scopes=['yadisk:all']), \
             mock.patch('mpfs.core.services.disk_service.MPFSStorageService.free', return_value=0):
            resp = self.client.put(self.url, query={'path': '/test_storage_quota_exhausted.txt',
                                                    'skip_check_space': 'true'})
            self.assertEqual(resp.status_code, 200)

    def test_upload_success(self):
        with self.specified_client(scopes=['yadisk:all']):
            resp = self.client.put(self.url, query={'path': '/test.txt'})
            assert resp.status_code == 200

            resp = from_json(resp.content)
            assert not resp.viewkeys() ^ set(UploadResourceSerializer.visible_fields)

            assert not resp['operation_link'].viewkeys() ^ set(LinkSerializer.visible_fields)
            assert not resp['upload_link'].viewkeys() ^ set(LinkSerializer.visible_fields)

    def test_new_user_after_upload_from_images_got_welcome_mail(self):
        """
        Проверить, что новый пользователь после сохранения изображения из Яндекс.Картинок
        получает правильное приветственное письмо.
        """
        uid = self.uid
        self.remove_user(uid)  # новый пользователь (без диска)
        userinfo = passport.userinfo(uid)
        userinfo['has_disk'] = False
        userinfo['username'] = 'sasha'
        userinfo['login'] = 'dp'
        with self.specified_client(id='images', scopes=['cloud_api:disk.write'], uid=uid),\
                mock.patch.object(passport, 'userinfo', return_value=userinfo),\
                mock.patch.object(StandartUser, 'info', return_value={'space': 100500}), \
                mock.patch('mpfs.core.user.standart.StandartUser.send_welcome_mail', return_value=None) as mock_send:
            self.client.post(self.url, {'path': '/test.txt', 'url': 'https://example.com'})
            mock_send.assert_called_once()

    def test_operation_link_success(self):
        with self.specified_client(scopes=['yadisk:all']):
            resp = self.client.put(self.url, query={'path': '/test.txt'})
            assert resp.status_code == 200

            resp = from_json(resp.content)
            resp = self.client.get(resp['operation_link']['href'])
            assert resp.status_code == 200

    def test_hardlink_success(self):
        self.upload_file(uid=self.uid, path='/disk/text.txt', file_data={'size': 1024})
        resp = self.json_ok('info', {'uid': self.uid, 'path': '/disk/text.txt', 'meta': ''})
        md5 = resp['meta']['md5']
        sha256 = resp['meta']['sha256']
        size = resp['meta']['size']
        with self.specified_client(scopes=['yadisk:all']):
            with mock.patch('mpfs.core.bus.Filesystem.store', return_value={'status': 'hardlinked'}):
                resp = self.client.put(self.url, query={'path': '/test2.txt',
                                                        'sha256': sha256,
                                                        'md5': md5,
                                                        'size': size})
                assert resp.status_code == 201

                resp = from_json(resp.content)
                assert not resp.viewkeys() ^ set(LinkSerializer.visible_fields)

    def test_permissions(self):
        # Пытаемся загрузить файл в Диск
        query = {'path': self.dir_path}
        scopes_to_status = (
            ([], 403),
            (['cloud_api:disk.read'], 403),
            (['cloud_api:disk.app_folder'], 403),
            (['cloud_api:disk.app_folder', 'cloud_api:disk.read'], 403),
            (['cloud_api:disk.write'], 403),
            (['yadisk:all'], 200)
        )
        self._permissions_test(scopes_to_status, 'PUT', self.url, query=query)

        # Пытаемся загрузить файл в папку приложения
        query = {'path': 'app:%s' % self.dir_path}
        scopes_to_status = (
            ([], 403),
            (['cloud_api:disk.read'], 403),
            (['cloud_api:disk.app_folder'], 403),
            (['yadisk:all'], 200),
        )
        self._permissions_test(scopes_to_status, 'PUT', self.url, query=query)

    def test_last_uploaded_contains_revision(self):
        self.upload_file(uid=self.uid, path='/disk/text.txt', file_data={'size': 1024})

        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.get('disk/resources/last-uploaded', query={'path': '/test.txt'})
            result = json.loads(resp.content)
            assert all('revision' in item for item in result['items'])

    def test_device_collections_passing(self):
        device_collections = '["raccoons", "foxes"]'
        with self.specified_client(scopes=['yadisk:all']), \
             mock.patch('mpfs.core.base.store', return_value={'status': 'hardlinked'}) as mocked:
            self.client.put(self.url,
                            query={'path': '/test.txt'},
                            headers={'X-Yandex-Device-Collections': device_collections})

        assert mocked.call_args[0][0].device_collections == device_collections

    @parameterized.expand([('GET',), ('PUT',)])
    def test_request_for_noninit_user_with_passport_bad_response(self, method):
        uid = '123456789'
        user_info = deepcopy(DEFAULT_USERS_INFO[self.uid])
        user_info['uid'] = uid
        with self.specified_client(scopes=['yadisk:all'], uid=uid, id=settings.platform['disk_apps_ids'][0]):
            with PassportStub(userinfo=user_info) as stub:
                stub.subscribe.side_effect = passport.errors_map['accountwithpasswordrequired']
                response = self.client.request(method, self.url, uid=uid, query={'path': '/test.txt'})
                assert response.status_code == 403
                content = json.loads(response.content)
                assert content['error'] == 'DiskUnsupportedUserAccountTypeError'
                assert content['description'] == 'User account type is not supported.'

    @parameterized.expand([('GET',), ('PUT',)])
    def test_request_for_blocked_user(self, method):
        opts = {
            'uid': self.uid,
            'moderator': 'moderator',
            'comment': 'comment',
        }
        self.support_ok('block_user', opts)
        with self.specified_client(scopes=['yadisk:all']):
            response = self.client.request(method, self.url, uid=self.uid, query={'path': '/test.txt'})
            content = json.loads(response.content)
            assert response.status_code == 403
            assert content['error'] == 'DiskUserBlockedError'
            assert content['description'] == 'User is blocked.'

    @parameterized.expand([('GET',), ('PUT',)])
    def test_request_for_noninit_user_with_disk_apps(self, method='GET'):
        uid = '123456789'
        user_info = deepcopy(DEFAULT_USERS_INFO[self.uid])
        user_info['uid'] = uid
        with self.specified_client(scopes=['yadisk:all'], uid=uid, id=settings.platform['disk_apps_ids'][0]):
            with PassportStub(userinfo=user_info):
                response = self.client.request(method, self.url, uid=uid, query={'path': '/test.txt'})
                # init and retry with _auto_initialize_user
                assert response.status_code == 200


class GetResourcesUploadTestCase(BillingApiTestCaseMixin, UserTestCaseMixin,
                                 UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    dir_path = '/test_get_upload'
    file_path = '%s/test_get_upload.txt' % dir_path
    url = 'disk/resources/upload'

    def setup_method(self, method):
        super(GetResourcesUploadTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)

    def test_wihtout_path(self):
        with self.specified_client(scopes=['cloud_api:disk.write']):
            resp = self.client.get(self.url)
            self.assertEqual(resp.status_code, 400)

    def test_upload_file_to_directory_path(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '%s:/disk%s' % (self.uid, self.dir_path)})

        with self.specified_client(scopes=['cloud_api:disk.write']):
            resp = self.client.get(self.url, query={'path': self.dir_path})
            self.assertEqual(resp.status_code, 409)

    def test_upload_file_to_non_existent_directory(self):
        with self.specified_client(scopes=['cloud_api:disk.write']):
            resp = self.client.get(self.url, query={'path': self.file_path})
            self.assertEqual(resp.status_code, 409)

    def test_upload_existent_file(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '%s:/disk%s' % (self.uid, self.dir_path)})
        self.upload_file(self.uid, '/disk%s' % self.file_path)

        with self.specified_client(scopes=['cloud_api:disk.write']):
            resp = self.client.get(self.url, query={'path': self.file_path})
            self.assertEqual(resp.status_code, 409)

            resp = self.client.get(self.url, query={'path': self.file_path, 'overwrite': 1})
            self.assertEqual(resp.status_code, 200)

    def test_upload_when_storage_quota_exhausted(self):
        with self.specified_client(scopes=['cloud_api:disk.write']), \
             mock.patch('mpfs.core.services.disk_service.MPFSStorageService.free', return_value=0):
            resp = self.client.get(self.url, query={'path': '/test_storage_quota_exhausted.txt'})
            self.assertEqual(resp.status_code, 507)

    def test_upload_when_file_size_limit_exceeded(self):
        with self.specified_client(scopes=['yadisk:all']), \
             mock.patch('mpfs.common.util.experiments.logic.ExperimentManager.is_feature_active', return_value=True):
            resp = self.client.put(self.url, query={'path': '/too_big.txt', 'size': 20 * (1 << 30)})

            self.assertEqual(resp.status_code, 413)
            body = from_json(resp.content)
            assert_that(body, has_entries({'reason': equal_to('FreeUserUploadFileSizeLimitExceeded'),
                                           'limit': greater_than(0)}))

    def test_upload_when_traffic_limit_exceeded(self):
        from mpfs.common.util.experiments.logic import experiment_manager
        experiment_manager.update_context(uid=self.uid)
        already_uploaded_traffic = DataSize.parse('21GB').to_bytes()
        Quota().update_upload_traffic(self.uid, already_uploaded_traffic)

        with self.specified_client(scopes=['cloud_api:disk.write']), \
             mock.patch('mpfs.common.util.experiments.logic.ExperimentManager.is_feature_active', return_value=True):
            resp = self.client.get(self.url, query={'path': '/test_storage_quota_exhausted.txt'})

            self.assertEqual(resp.status_code, 423)
            body = from_json(resp.content)
            assert_that(body, has_entries({'reason': contains_string('Monthly'), 'limit': greater_than(0)}))

    def test_upload_when_traffic_limit_exceeded_wo_details(self):
        from mpfs.common.util.experiments.logic import experiment_manager
        experiment_manager.update_context(uid=self.uid)
        already_uploaded_traffic = DataSize.parse('600MB').to_bytes()
        Quota().update_upload_traffic(self.uid, already_uploaded_traffic)

        with self.specified_client(scopes=['cloud_api:disk.write']), \
             mock.patch.dict(settings.limits['upload_traffic']['weekly']['by_reg_time'], {'enabled': True,
                                                                                          'error_with_details': False,
                                                                                          'reg_time': '2019-05-05 11:11:11',
                                                                                          'percentage': 100,
                                                                                          'limit': '500MB'}):
            resp = self.client.get(self.url, query={'path': '/test_storage_quota_exhausted.txt'})
            self.assertEqual(resp.status_code, 423)
            body = from_json(resp.content)
            assert_that(body, has_entries({'description': 'User is in read-only mode.'}))

    def test_upload_when_storage_quota_exhausted_with_skip_space_check(self):
        with self.specified_client(id=settings.platform['skip_check_space']['allowed_client_ids'][0],
                                   scopes=['cloud_api:disk.write']), \
             mock.patch('mpfs.core.services.disk_service.MPFSStorageService.free', return_value=0):
            resp = self.client.get(self.url, query={'path': '/test_storage_quota_exhausted.txt', 'skip_check_space': 'true'})
            self.assertEqual(resp.status_code, 200)

    def test_normal_behaviour(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '%s:/disk%s' % (self.uid, self.dir_path)})
        with self.specified_client(scopes=['cloud_api:disk.write']):
            resp = self.client.get(self.url, query={'path': self.file_path})
            self.assertEqual(resp.status_code, 200)
            self.assertTrue('Location' not in resp.headers)
            resp = from_json(resp.content)
            self.assertTrue('href' in resp)
            self.assertTrue('method' in resp)
            self.assertEqual(resp['method'], 'PUT')
            self.assertTrue('operation_id' in resp)
            resp = self.client.get('disk/operations/%s' % resp['operation_id'])
            assert resp.status_code == 200

    def test_permissions(self):
        # Пытаемся загрузить файл в Диск
        query = {'path': self.dir_path}
        scopes_to_status = (
            ([], 403),
            (['cloud_api:disk.read'], 403),
            (['cloud_api:disk.app_folder'], 403),
            (['cloud_api:disk.app_folder', 'cloud_api:disk.read'], 403),
            (['cloud_api:disk.write'], 200),
        )
        self._permissions_test(scopes_to_status, 'GET', self.url, query=query)

        # Пытаемся загрузить файл в папку приложения
        query = {'path': 'app:%s' % self.dir_path}
        scopes_to_status = (
            ([], 403),
            (['cloud_api:disk.read'], 403),
            (['cloud_api:disk.app_folder'], 200),
        )
        self._permissions_test(scopes_to_status, 'GET', self.url, query=query)


class PutResourceTestCase(CommonDiskTestCase, UserTestCaseMixin, DiskApiTestCase):
    api_version = 'v1'
    api_mode = tags.platform.EXTERNAL
    dir_path = '/InternalPostDirTestCase'
    url = 'disk/resources'

    def setup_method(self, method):
        super(PutResourceTestCase, self).setup_method(method)
        self.create_user(uid=self.uid, locale='ru')

    def test_normal_behaviour_and_standard_errors(self):
        with self.specified_client(scopes=['cloud_api:disk.write']):
            resp = self.client.put(self.url, query={'path': self.dir_path})
        self.assertEqual(resp.status_code, 201)
        self.assertTrue('Location' not in resp.headers)
        self.assertTrue('href' in from_json(resp.content))

        with self.specified_client(scopes=['cloud_api:disk.write']):
            resp = self.client.put(self.url, query={'path': self.dir_path})
            self.assertEqual(resp.status_code, 409)

        with self.specified_client(scopes=['cloud_api:disk.write']):
            resp = self.client.put(self.url, query={'path': '%s/missing/test' % self.dir_path})
            self.assertEqual(resp.status_code, 409)

        with self.specified_client(scopes=['cloud_api:disk.write']), \
             mock.patch('mpfs.frontend.api.disk.JAVA_DJFS_API_PROXY_MKDIR_ENABLED', True), \
             DjfsApiMockHelper.mock_request(error_code=codes.FOLDER_TOO_DEEP, status_code=403):
            resp = self.client.put(self.url, query={'path': 'not_relevant'})
            self.assertEqual(403, resp.status_code)
            self.assertEqual(
                {
                    'error': 'DiskFolderTooDeepError',
                    'description': 'Folder depth limit exceeded',
                    'message': u'Достигнут предел вложенности папок',
                },
                from_json(resp.content),
            )

    def test_call_without_path(self):
        with self.specified_client(scopes=['cloud_api:disk.write']):
            resp = self.client.put('disk/resources', query={})
            self.assertEqual(resp.status_code, 400)

    def test_cyr_name(self):
        path = u'/КирилическаяПапкаЪЁ'
        with self.specified_client(scopes=['cloud_api:disk.write', 'cloud_api:disk.read']):
            resp = self.client.put(self.url, query={'path': path})
            self.assertEqual(resp.status_code, 201)
            self.assertTrue('href' in from_json(resp.content))
            resp = self.client.get(self.url, query={'path': path})
            self.assertEqual(resp.status_code, 200)

    def test_permissions(self):
        # Пытаемся создать папку в Диске
        query = {'path': self.dir_path}
        scopes_to_status = (
            ([], 403),
            (['cloud_api:disk.read'], 403),
            (['cloud_api:disk.app_folder'], 403),
            (['cloud_api:disk.app_folder', 'cloud_api:disk.read'], 403),
            (['cloud_api:disk.write'], 201),
        )
        self._permissions_test(scopes_to_status, 'PUT', self.url, query=query)

        # Пытаемся создать папку в папке приложения
        query = {'path': 'app:%s' % self.dir_path}
        scopes_to_status = (
            ([], 403),
            (['cloud_api:disk.read'], 403),
            (['cloud_api:disk.app_folder'], 201),
        )
        self._permissions_test(scopes_to_status, 'PUT', self.url, query=query)

    def test_create_system_dirs(self):
        with self.specified_client(scopes=['cloud_api:disk.write']):
            resp = self.client.put(self.url, query={'path': '/test'})
        self.assertEqual(resp.status_code, 201)
        system_dirs = (u'/Загрузки',
                       u'/Фотокамера',
                       u'/Яндекс.Фотки',
                       u'/Скриншоты',
                       u'/Социальные сети',
                       u'/Приложения/',
                       u'/Яндекс.Книги/')

        for folder in system_dirs:
            with self.specified_client(scopes=['cloud_api:disk.write']):
                resp = self.client.put(self.url, query={'path': folder})
                self.assertEqual(resp.status_code, 201)
        res_list = self.json_ok('list', {'uid': self.uid, 'path': '/disk', 'meta': ''})
        names_set = set([item['name'] for item in res_list])
        system_dirs_set = set([sys_dir.replace('/', '') for sys_dir in system_dirs])
        assert names_set > system_dirs_set

        for item in res_list:
            if item['name'] in system_dirs_set:
                assert 'folder_type' in item['meta']

    def test_area_specification(self):
        # https://st.yandex-team.ru/CHEMODAN-25413
        # Пытаемся создать папку в Диске, указывая path=disk:path и disk:/path1
        scopes_to_status = (
            (['cloud_api:disk.write'], 201),
        )
        query = {'path': 'disk:/path1'}
        self._permissions_test(scopes_to_status, 'PUT', self.url, query=query)

        scopes_to_status = (
            (['cloud_api:disk.write'], 400),
        )
        query = {'path': 'disk:path'}
        self._permissions_test(scopes_to_status, 'PUT', self.url, query=query)

        # Пытаемся создать папку в Диске, указывая path=app:path и app:/path1
        scopes_to_status = (
            (['cloud_api:disk.app_folder'], 201),
        )
        query = {'path': 'app:/path1'}
        self._permissions_test(scopes_to_status, 'PUT', self.url, query=query)

        scopes_to_status = (
            (['cloud_api:disk.app_folder'], 400),
        )
        query = {'path': 'app:path'}
        self._permissions_test(scopes_to_status, 'PUT', self.url, query=query)

    def test_user_blocked(self):
        with self.specified_client(scopes=['cloud_api:disk.write']):
            resp = self.client.put(self.url, query={'path': '/test'})
        assert resp.status_code == 201
        opts = {
            'uid': self.uid,
            'moderator': 'moderator',
            'comment': 'comment',
        }
        self.support_ok('block_user', opts)
        with self.specified_client(scopes=['cloud_api:disk.write']):
            resp = self.client.put(self.url, query={'path': '/test'})
        assert resp.status_code == 403


class UpdateResourceTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, SupportApiTestCaseMixin, DiskApiTestCase):
    url = 'disk/resources'
    file_name = 'test.txt'
    api_file_path = 'disk:/%s' % file_name
    mpfs_file_path = '/disk/%s' % file_name
    api_mode = tags.platform.EXTERNAL

    def setup_method(self, method):
        super(UpdateResourceTestCase, self).setup_method(method)
        self.create_user(uid=self.uid, locale='ru')

    def test_update_file_custom_properties(self):
        self.upload_file(self.uid, self.mpfs_file_path)
        with self.specified_client(scopes=['cloud_api:disk.write', 'cloud_api:disk.read']):
            resp = self.client.get(self.url, query={'path': self.api_file_path})
            file_data = json.loads(resp.content)
            assert 'custom_properties' not in file_data

            custom_properties = {'my_prop': 'test'}
            file_data['custom_properties'] = custom_properties
            resp = self.client.patch(self.url, query={'path': self.api_file_path}, data=file_data)
            file_data = json.loads(resp.content)
            assert 'custom_properties' in file_data
            assert file_data['custom_properties'] == custom_properties

            resp = self.client.get(self.url, query={'path': self.api_file_path})
            new_file_data = json.loads(resp.content)
            assert new_file_data['custom_properties'] == file_data['custom_properties']

            custom_properties = {'bar': 'test'}
            file_data['custom_properties'] = custom_properties
            resp = self.client.patch(self.url, query={'path': self.api_file_path}, data=file_data)
            file_data = json.loads(resp.content)
            assert 'bar' in file_data['custom_properties']
            assert 'my_prop' in file_data['custom_properties']

    def test_update_only_custom_properties(self):
        self.upload_file(self.uid, self.mpfs_file_path)
        with self.specified_client(scopes=['cloud_api:disk.write', 'cloud_api:disk.read']):
            custom_properties = {'my_prop': 'test'}
            file_data = {'custom_properties': custom_properties}
            resp = self.client.patch(self.url, query={'path': self.api_file_path}, data=file_data)
            file_data = json.loads(resp.content)
            assert 'custom_properties' in file_data
            assert file_data['custom_properties'] == custom_properties

    def test_update_with_wrong_content_type(self):
        self.upload_file(self.uid, self.mpfs_file_path)
        with self.specified_client(scopes=['cloud_api:disk.write', 'cloud_api:disk.read']):
            custom_properties = {'my_prop': 'test'}
            file_data = {'custom_properties': custom_properties}
            resp = self.client.patch(self.url, query={'path': self.api_file_path}, data=file_data,
                                     headers={'Content-Type': 'application/x-www-form-urlencoded'})
            assert resp.status_code == 415

    def test_revisions(self):
        """Проверить наличие поля revision у обновленного ресурса.
        """
        self.upload_file(self.uid, self.mpfs_file_path)
        with self.specified_client(scopes=['cloud_api:disk.write', 'cloud_api:disk.read']):
            resp = self.client.get(self.url, query={'path': self.api_file_path})
            file_data = json.loads(resp.content)
            assert 'custom_properties' not in file_data

            custom_properties = {'my_prop': 'test'}
            file_data['custom_properties'] = custom_properties
            resp = self.client.patch(self.url, query={'path': self.api_file_path}, data=file_data)
            file_data = json.loads(resp.content)
            assert 'custom_properties' in file_data
            assert file_data['custom_properties'] == custom_properties

            result = json.loads(resp.content)
            assert 'revision' in result

            url_files = 'disk/resources/files'
            resp = self.client.get(url_files, query={'path': self.api_file_path})
            file_data = json.loads(resp.content)
            assert all('revision' in data for data in file_data['items'])

    def test_request_for_noninit_user_with_passport_bad_response(self):
        """Протестировать, что при запросе информации пользователем с аккаунтом без пароля он получит 403 ошибку."""
        uid = '123456789'
        user_info = deepcopy(DEFAULT_USERS_INFO[self.uid])
        user_info['uid'] = uid
        file_data = {
            'custom_properties': {'my_prop': 'test'}
        }
        with self.specified_client(scopes=['yadisk:all'], uid=uid, id=settings.platform['disk_apps_ids'][0]):
            with PassportStub(userinfo=user_info) as stub:
                stub.subscribe.side_effect = passport.errors_map['accountwithpasswordrequired']
                response = self.client.patch(self.url, query={'path': self.api_file_path}, data=file_data, uid=uid)
                assert response.status_code == 403
                content = json.loads(response.content)
                assert content['error'] == 'DiskUnsupportedUserAccountTypeError'
                assert content['description'] == 'User account type is not supported.'

    def test_request_for_blocked_user(self):
        file_data = {
            'custom_properties': {'my_prop': 'test'}
        }
        opts = {
            'uid': self.uid,
            'moderator': 'moderator',
            'comment': 'comment',
        }
        self.support_ok('block_user', opts)
        with self.specified_client(scopes=['yadisk:all'], uid=self.uid, id=settings.platform['disk_apps_ids'][0]):
            response = self.client.patch(self.url, query={'path': self.api_file_path}, data=file_data, uid=self.uid)
            content = json.loads(response.content)
            assert response.status_code == 403
            assert content['error'] == 'DiskUserBlockedError'
            assert content['description'] == 'User is blocked.'

    def test_request_for_noninit_user(self):
        uid = '123456789'
        user_info = deepcopy(DEFAULT_USERS_INFO[self.uid])
        user_info['uid'] = uid
        file_data = {
            'custom_properties': {'my_prop': 'test'}
        }
        with self.specified_client(scopes=['yadisk:all'], uid=uid, id=settings.platform['disk_apps_ids'][0]):
            with PassportStub(userinfo=user_info):
                response = self.client.patch(self.url, query={'path': self.api_file_path}, data=file_data, uid=uid)
                # init and retry with _auto_initialize_user
                # resource was created for another uid - so 404
                assert response.status_code == 404


class DecodeCustomPropertiesTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    """Тестовый класс для проверки обработки поля `meta.custom_properties` (CP).

    Поле может иметь два типа: ``str`` и ``dict`` в зависимости от параметра `decode_custom_properties`
    в запросах к МПФС. Здесь предполагается что тип поля ``dict``

    Это поле обрабатывается в единственном сериализаторе class:`ResourceSerializer`.
    поэтому идеологически должно обрабатывться одинаково во всех хендлерах.

    """
    api_mode = tags.platform.EXTERNAL

    custom_properties = {"a": 1}
    album_dict = {
        'title': 'MyAlbum',
        'description': 'My Test Album',
        'items': [
            {'type': 'resource', 'path': '/disk/0.jpg'},
            {'type': 'resource', 'path': '/disk/1.jpg'},
            {'type': 'resource', 'path': '/disk/2.jpg'},
        ],
    }

    def setup_method(self, method):
        super(DecodeCustomPropertiesTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)
        for i in range(2):
            self.upload_file(self.uid, '/disk/%i.jpg' % i, {'mimetype': 'image/jpg'})

        self.album = self.json_ok('albums_create_with_items',
                                  opts={'uid': self.uid}, json=self.album_dict)
        self.album_item = self.album['items'][0]

        self.json_ok('setprop', {
            'uid': self.uid,
            'path': self.album_item['object']['path'],
            'custom_properties': json.dumps(self.custom_properties)
        })

    def test_list_file(self):
        """Протестировать наличие перекодированного CP в мете в листинге файла (list)
        """
        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.get('disk/resources', query={'path': 'disk:/0.jpg'})
            assert self.custom_properties == json.loads(resp.content)['custom_properties']

    def test_timeline(self):
        """Протестировать наличие перекодированного CP в мете для последних файлов (timeline)
        """
        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.get('disk/resources/last-uploaded', query={'path': 'disk:/'})
            assert self.custom_properties == json.loads(resp.content)['items'][1]['custom_properties']

    def test_albums_list(self):
        """Протестировать наличие перекодированного CP в мете обложки фотоальбома (albums_list).
        """
        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.request('GET', 'disk/photoalbums', query={})
            items = json.loads(resp.content)['items']

            item = next((item for item in items if 'cover' in item and item['cover']['name'] == '0.jpg'))
            assert item
            assert self.custom_properties == item['cover']['custom_properties']

    def test_album_item_info(self):
        """Протестировать наличие перекодированного CP в мете элемента альбома (album_item_info).
        """
        url = 'disk/photoalbums/%s/items/%s' % (self.album['id'], self.album_item['id'])
        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.request('GET', url)
            resp = json.loads(resp.content)
            assert self.custom_properties == resp['resource']['custom_properties']


class PostResourceImportMailTestCase(UserTestCaseMixin, DiskApiTestCase):
    """Импорт почтового аттача в диск"""
    api_version = 'v1'
    api_mode = tags.platform.INTERNAL
    dst_api_path = 'disk:/bullshit.jpg'
    url = 'disk/resources/import/mail'

    def test_operation_created(self):
        with mock.patch.object(ImportMailAttach.kladun, 'open_url') as mock_obj:
            mock_obj.return_value = {'post-target': 'bullshit', 'poll-result': 'bullshit'}
            with mock.patch.object(MailService, 'open_url') as mail_service_open_url:
                mail_service_open_url.return_value = '{"envelopes":[{"stid":"bullshit"}]}'
                self.create_user(self.uid, noemail=bool(True))
                resp = self.client.post(
                    self.url,
                    query={
                        'path': self.dst_api_path,
                        'service_file_id': '2160000000033115472/1.1',
                        'overwrite': '1'
                    },
                    uid=self.uid
                )
                assert resp.status_code == 200
                json_ = from_json(resp.content)
                assert json_.get('type') == 'copy'
                assert 'oid' in json_
                assert 'at_version' in json_

    def test_wrong_service_file_id_format(self):
        """Тестирует что будет отдана 400 ошибка в случае,
        если передан `service_file_id` в неправильном формате."""
        self.create_user(self.uid, noemail=bool(True))
        resp = self.client.post(
            self.url,
            query={
                'path': self.dst_api_path,
                'service_file_id': '2160000000033115472__no_slash_here__1.1',
                'overwrite': '1'
            },
            uid=self.uid
        )
        assert resp.status_code == 400

    def test_overwrite_with_autosuffix_forbidden(self):
        """Тестирует что будет отдана 409 ошибка в случае,
        если передан autosuffix=1 и overwrite=1."""
        self.create_user(self.uid, noemail=bool(True))
        resp = self.client.post(
            self.url,
            query={
                'path': self.dst_api_path,
                'service_file_id': '2160000000033115472/1.1',
                'overwrite': '1',
                'autosuffix': '1'
            },
            uid=self.uid
        )
        assert resp.status_code == 409


class GetResourceDetailHandlerTestCase(UserTestCaseMixin, UploadFileTestCaseMixin,
                                       SupportApiTestCaseMixin,
                                       DiskApiTestCase, SharingTestCaseMixin):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    method = 'GET'
    url = 'disk/resources/%s'

    def test_default_behaviour(self):
        """Проверить обычный стандартный запрос без всяких премудростей для ресурса-файла."""
        self.create_user(self.user_1.uid, noemail=True)
        path = '/disk/test.jpg'
        self.upload_file(self.user_1.uid, path, file_data={'mimetype': 'image/jpg'})
        resource_info = self.json_ok('info', {'uid': self.user_1.uid, 'path': path, 'meta': ''})
        resource_id = resource_info['meta']['resource_id']
        with self.specified_client(scopes=['cloud_api:disk.read'], uid=self.user_1.uid), \
             DjfsApiMockHelper.mock_request(status_code=200, content=to_json([resource_info])):
            response = self.client.request(
                self.method,
                self.url % resource_id,
            )
            assert response.status_code == 200
            data = from_json(response.content)
            assert 'created' in data
            assert 'md5' in data
            assert 'media_type' in data and data['media_type'] == 'image'
            assert 'mime_type' in data and data['mime_type'] == 'image/jpg'
            assert 'modified' in data
            assert 'name' in data and data['name'] == 'test.jpg'
            assert 'path' in data and data['path'] == 'disk:/test.jpg'
            assert 'preview' in data
            assert 'revision' in data
            assert 'sha256' in data
            assert 'size' in data
            assert 'type' in data and data['type'] == 'file'
            assert 'antivirus_status' in data
            assert data['antivirus_status'] == 'clean'

    def test_request_with_incorrect_format_resource_id_got_400(self):
        """Протестировать, что запрос с идентификатором ресурса некорректного формата
        получит ответ с 400 кодом.
        """
        self.create_user(self.user_1.uid, noemail=True)
        path = '/disk/test.jpg'
        self.upload_file(self.user_1.uid, path, file_data={'mimetype': 'image/jpg'})
        with self.specified_client(scopes=['cloud_api:disk.read']):
            incorrect_resource_id = 'incorrect_format_resource_id'
            response = self.client.request(
                self.method,
                self.url % incorrect_resource_id,
                uid=self.user_1.uid
            )
            assert response.status_code == 400
            data = from_json(response.content)
            assert data['error'] == 'IncorrectResourceIdFormatError'
            assert data['description'] == (
                'Specified resource identifier "%s" has incorrect format.'
            ) % incorrect_resource_id

    def test_request_with_folder(self):
        """Протестировать запрос для ресурса-папки."""
        # https://st.yandex-team.ru/CHEMODAN-32427
        self.create_user(self.user_1.uid, noemail=True)
        self.json_ok('mkdir', {'uid': self.user_1.uid, 'path': u'/disk/Музыка'})
        response = self.json_ok('info', {'uid': self.user_1.uid, 'path': u'/disk/Музыка', 'meta': ''})
        resource_id = response['meta']['resource_id']
        with self.specified_client(scopes=['cloud_api:disk.read'], uid=self.user_1.uid), \
             DjfsApiMockHelper.mock_request(status_code=200, content=to_json([response])):
            response = self.client.request(
                self.method,
                self.url % resource_id,
            )
            assert response.status_code == 200
            data = from_json(response.content)
            assert data['path'] == u'disk:/Музыка'

    def test_request_for_resource_without_access_to_it(self):
        """Протестировать кейс, когда пользователь запрашивает ресурс чужой либо тот, к которому убрали уже доступ."""
        # https://st.yandex-team.ru/CHEMODAN-32755
        self.create_user(self.user_1.uid, noemail=True)
        self.create_user(self.user_2.uid, noemail=True)

        shared_folder_path = '/disk/shared_folder'
        self.json_ok('mkdir', {'uid': self.user_2.uid, 'path': shared_folder_path})
        result = self.json_ok('share_create_group', {'uid': self.user_2.uid, 'path': shared_folder_path})
        gid = result['gid']

        invite_hash = self.share_invite(gid, uid=self.user_1.uid)
        self.json_ok('share_activate_invite', {'hash': invite_hash, 'uid': self.user_1.uid})

        result = self.json_ok('info', {'uid': self.user_1.uid, 'path': shared_folder_path, 'meta': ''})
        resource_id = result['meta']['resource_id']

        with self.specified_client(scopes=['cloud_api:disk.read'], uid=self.user_1.uid), \
             DjfsApiMockHelper.mock_request(status_code=200, content=to_json([result])):
            response = self.client.request(
                self.method,
                self.url % resource_id,
            )
            assert response.status_code == 200

        self.json_ok('share_leave_group', {'uid': self.user_1.uid, 'gid': gid})
        with self.specified_client(scopes=['cloud_api:disk.read'], uid=self.user_1.uid), \
             DjfsApiMockHelper.mock_request(status_code=200, content=to_json([])):
            response = self.client.request(
                self.method,
                self.url % resource_id,
            )
            assert response.status_code == 404

    def test_get_trash_file_by_id(self):
        """Проверяем, что если достаем через ручку файл из корзины, то вернется 404."""
        self.create_user(self.user_1.uid, noemail=True)
        path = '/disk/test.jpg'
        self.upload_file(self.user_1.uid, path, file_data={'mimetype': 'image/jpg'})

        info = self.json_ok('info', {'uid': self.user_1.uid, 'path': path, 'meta': ''})
        resource_id = info['meta']['resource_id']

        self.json_ok('trash_append', {'uid': self.user_1.uid, 'path': path})

        with self.specified_client(scopes=['cloud_api:disk.read'], uid=self.user_1.uid), \
                mock.patch('mpfs.platform.v1.disk.handlers.JAVA_DJFS_API_PROXY_PLATFORM_BULK_INFO_ENABLED', False):
            response = self.client.request(
                self.method,
                self.url % resource_id,
            )
            assert response.status_code == 404

    def test_do_not_make_extra_mpfs_call(self):
        self.create_user(self.user_1.uid, noemail=True)
        path = '/disk/test.jpg'
        self.upload_file(self.user_1.uid, path, file_data={'mimetype': 'image/jpg'})
        response = self.json_ok('info', {'uid': self.user_1.uid, 'path': path, 'meta': ''})
        resource_id = response['meta']['resource_id']

        def original_list(*args, **kwargs):
            return content(*args, **kwargs)

        with self.specified_client(scopes=['cloud_api:disk.read'], uid=self.user_1.uid), \
                mock.patch('mpfs.platform.v1.disk.handlers.JAVA_DJFS_API_PROXY_PLATFORM_BULK_INFO_ENABLED', False), \
                mock.patch('mpfs.core.base.content', side_effect=original_list) as mpfs_list:
            response = self.client.request(
                self.method,
                self.url % resource_id,
            )
            assert response.status_code == 200
            mpfs_list.assert_not_called()

    def test_request_for_noninit_user_with_passport_bad_response(self):
        """Протестировать, что при запросе информации пользователем с аккаунтом без пароля он получит 403 ошибку."""
        self.create_user(self.user_1.uid, noemail=True)
        path = '/disk/test.jpg'
        self.upload_file(self.user_1.uid, path, file_data={'mimetype': 'image/jpg'})
        response = self.json_ok('info', {'uid': self.user_1.uid, 'path': path, 'meta': ''})
        resource_id = response['meta']['resource_id']
        url = self.url % resource_id
        uid = '123456789'
        user_info = deepcopy(DEFAULT_USERS_INFO[self.uid])
        user_info['uid'] = uid
        with self.specified_client(scopes=['yadisk:all'], uid=uid), \
                mock.patch('mpfs.platform.v1.disk.handlers.JAVA_DJFS_API_PROXY_PLATFORM_BULK_INFO_ENABLED', False):
            with PassportStub(userinfo=user_info) as stub:
                stub.subscribe.side_effect = passport.errors_map['accountwithpasswordrequired']
                response = self.client.request(self.method, url, uid=uid)
                assert response.status_code == 403
                content = json.loads(response.content)
                assert content['error'] == 'DiskUnsupportedUserAccountTypeError'
                assert content['description'] == 'User account type is not supported.'

    def test_request_for_blocked_user(self):
        uid = self.user_1.uid
        self.create_user(uid, noemail=True)
        path = '/disk/test.jpg'
        self.upload_file(self.user_1.uid, path, file_data={'mimetype': 'image/jpg'})
        response = self.json_ok('info', {'uid': self.user_1.uid, 'path': path, 'meta': ''})
        resource_id = response['meta']['resource_id']
        opts = {
            'uid': uid,
            'moderator': 'moderator',
            'comment': 'comment',
        }
        self.support_ok('block_user', opts)

        with self.specified_client(scopes=['cloud_api:disk.read'], uid=uid), \
             DjfsApiMockHelper.mock_request(error_code=codes.USER_BLOCKED,
                                            status_code=403,
                                            content=to_json({'code': codes.USER_BLOCKED, 'title': 'meow'})):
            response = self.client.request(
                self.method,
                self.url % resource_id,
            )
            content = json.loads(response.content)
            assert response.status_code == 403
            assert content['error'] == 'DiskUserBlockedError'
            assert content['description'] == 'User is blocked.'

    def test_request_for_noninit_user(self):
        uid = '123456789'
        user_info = deepcopy(DEFAULT_USERS_INFO[self.uid])
        user_info['uid'] = uid
        self.create_user(self.user_1.uid, noemail=True)
        path = '/disk/test.jpg'
        self.upload_file(self.user_1.uid, path, file_data={'mimetype': 'image/jpg'})
        response = self.json_ok('info', {'uid': self.user_1.uid, 'path': path, 'meta': ''})
        resource_id = response['meta']['resource_id']
        url = self.url % resource_id

        with self.specified_client(scopes=['yadisk:all'], uid=uid), \
                mock.patch('mpfs.platform.v1.disk.handlers.JAVA_DJFS_API_PROXY_PLATFORM_BULK_INFO_ENABLED', False):
            with PassportStub(userinfo=user_info):
                response = self.client.request(self.method, url, uid=uid)
                # init and retry with _auto_initialize_user
                assert response.status_code == 404


class GetResourceDimensionsHandlerTestCase(UserTestCaseMixin, UploadFileTestCaseMixin,
                                           SupportApiTestCaseMixin,
                                           DiskApiTestCase):
    """Набор тестов для тестирования ручки получения геометрических размеров (ширина, высота) изображения."""
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    method = 'GET'
    url = 'disk/resources/%s/dimensions'
    path = '/disk/test.jpg'

    def setup_method(self, method):
        super(GetResourceDimensionsHandlerTestCase, self).setup_method(method)
        self.create_user(self.user_1.uid, noemail=True)
        self.upload_file(self.user_1.uid, self.path, file_data={'mimetype': 'image/jpg'})
        response = self.json_ok('info', {'uid': self.user_1.uid, 'path': self.path, 'meta': ''})
        self.existing_resource_id = response['meta']['resource_id']

    def test_success_response_from_search_db(self):
        """Протестировать случай, когда Поиск знает размеры изображения и вернул их."""
        with self.specified_client(scopes=['cloud_api:disk.read'], uid=self.user_1.uid):
            with SearchDBStub() as stub:
                stub.resources_info_by_file_ids.return_value = {
                    'total': 1,
                    'items': [
                        {
                            'height': '720',
                            'width': '1280'
                        }
                    ]
                }
                response = self.client.request(
                    self.method,
                    self.url % self.existing_resource_id,
                )
                assert response.status_code == 200
                data = from_json(response.content)
                assert data['height'] == 720
                assert data['width'] == 1280

    def test_not_found_response_from_search_db(self):
        """Протестировать случай, когда Поиск не нашел вообще информации по ресурсу,
        но в диске он есть (вернуть null'ы)."""
        with self.specified_client(scopes=['cloud_api:disk.read'], uid=self.user_1.uid):
            with SearchDBStub() as stub:
                stub.resources_info_by_file_ids.return_value = {
                    'total': 0,
                    'items': []
                }
                response = self.client.request(
                    self.method,
                    self.url % self.existing_resource_id,
                )
                assert response.status_code == 200
                data = from_json(response.content)
                assert data['height'] is None
                assert data['width'] is None

    def test_uses_dimensions_from_mpfs(self):
        width = 1234
        height = 5678
        resource = factory.get_resource(self.user_1.uid, self.path)
        resource.meta['width'] = width
        resource.meta['height'] = height
        resource.save()
        with self.specified_client(scopes=['cloud_api:disk.read'], uid=self.user_1.uid),\
                SearchDBStub() as stub:
            stub.resources_info_by_file_ids.return_value = {
                'total': 1,
                'items': [{'height': '720', 'width': '1280'}]
            }
            response = self.client.request(
                self.method,
                self.url % self.existing_resource_id,
            )
            assert 0 == stub.resources_info_by_file_ids.call_count
            assert response.status_code == 200
            data = from_json(response.content)
            assert width == data['width']
            assert height == data['height']


    def test_width_and_height_null_response_from_search_db(self):
        """Протестировать случай, когда Поиск вернул ширину и высоту, но они null,
        то есть либо это не изображение, либо он их не знает."""
        with self.specified_client(scopes=['cloud_api:disk.read'], uid=self.user_1.uid):
            with SearchDBStub() as stub:
                stub.resources_info_by_file_ids.return_value = {
                    'total': 1,
                    'items': [
                        {
                            'height': None,
                            'width': None
                        }
                    ]
                }
                response = self.client.request(
                    self.method,
                    self.url % self.existing_resource_id,
                )
                assert response.status_code == 200
                data = from_json(response.content)
                assert data['height'] is None
                assert data['width'] is None

    def test_search_db_error_transformed_to_503(self):
        with self.specified_client(scopes=['cloud_api:disk.read'], uid=self.user_1.uid):
            with SearchDBStub() as stub:
                stub.resources_info_by_file_ids.side_effect = SearchDB.api_error()
                response = self.client.request(
                    self.method,
                    self.url % self.existing_resource_id,
                )
                assert response.status_code == 503

    def test_resource_not_found_in_disk(self):
        """Протестировать случай, что в независимости от информации в Поиске, если в Диске нет файла - 404."""
        non_existing_file_id = ''.join(
            [random.choice(string.ascii_letters + string.digits) for _ in xrange(ResourceId.FILE_ID_LEN)]
        )
        resource_id = ResourceId(uid=self.user_1.uid, file_id=non_existing_file_id)
        raw_resource_id = resource_id.serialize()
        with self.specified_client(scopes=['cloud_api:disk.read'], uid=self.user_1.uid):
            with SearchDBStub() as stub:
                stub.resources_info_by_file_ids.return_value = {  # в поиске осталась старая информация
                    'total': 1,
                    'items': [
                        {
                            'height': '720',
                            'width': '1280'
                        }
                    ]
                }
                response = self.client.request(
                    self.method,
                    self.url % raw_resource_id,
                )
                assert response.status_code == 404

    def test_handler_is_hidden_from_public_polygon(self):
        """Протестировать, что ручка скрыта из публичного Полигона."""
        assert GetResourceDimensionsHandler.hidden

    def test_request_for_noninit_user_with_passport_bad_response(self):
        """Протестировать, что при запросе информации пользователем с аккаунтом без пароля он получит 403 ошибку."""
        uid = '123456789'
        user_info = deepcopy(DEFAULT_USERS_INFO[self.uid])
        user_info['uid'] = uid
        with self.specified_client(scopes=['yadisk:all'], uid=uid):
            with PassportStub(userinfo=user_info) as stub:
                stub.subscribe.side_effect = passport.errors_map['accountwithpasswordrequired']
                with SearchDBStub() as stub:
                    stub.resources_info_by_file_ids.return_value = {
                        'total': 1,
                        'items': [
                            {
                                'height': None,
                                'width': None
                            }
                        ]
                    }
                    response = self.client.request(
                        self.method,
                        self.url % self.existing_resource_id,
                    )
                    assert response.status_code == 403
                    content = json.loads(response.content)
                    assert content['error'] == 'DiskUnsupportedUserAccountTypeError'
                    assert content['description'] == 'User account type is not supported.'

    def test_request_for_blocked_user(self):
        uid = self.user_1.uid
        opts = {
            'uid': uid,
            'moderator': 'moderator',
            'comment': 'comment',
        }
        self.support_ok('block_user', opts)
        with self.specified_client(scopes=['yadisk:all'], uid=uid):
            with SearchDBStub() as stub:
                stub.resources_info_by_file_ids.return_value = {
                    'total': 1,
                    'items': [
                        {
                            'height': None,
                            'width': None
                        }
                    ]
                }
                response = self.client.request(
                    self.method,
                    self.url % self.existing_resource_id,
                )

                content = json.loads(response.content)
                assert response.status_code == 403
                assert content['error'] == 'DiskUserBlockedError'
                assert content['description'] == 'User is blocked.'

    def test_request_for_noninit_user(self):
        uid = '123456789'
        user_info = deepcopy(DEFAULT_USERS_INFO[self.uid])
        user_info['uid'] = uid
        with self.specified_client(scopes=['yadisk:all'], uid=uid):
            with PassportStub(userinfo=user_info):
                with SearchDBStub() as stub:
                    stub.resources_info_by_file_ids.return_value = {
                        'total': 1,
                        'items': [
                            {
                                'height': None,
                                'width': None
                            }
                        ]
                    }
                    response = self.client.request(
                        self.method,
                        self.url % self.existing_resource_id,
                    )

                    # init and retry with _auto_initialize_user
                    assert response.status_code == 404


class GetResourceImageMetadataHandlerTestCase(UserTestCaseMixin, UploadFileTestCaseMixin,
                                           SupportApiTestCaseMixin,
                                           DiskApiTestCase):
    """Набор тестов для тестирования ручки получения геометрических размеров (ширина, высота) изображения."""
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    method = 'GET'
    url = 'disk/resources/%s/image_metadata'
    path = '/disk/test.jpg'

    def setup_method(self, method):
        super(GetResourceImageMetadataHandlerTestCase, self).setup_method(method)
        self.create_user(self.user_1.uid, noemail=True)
        self.upload_file(self.user_1.uid, self.path, file_data={'mimetype': 'image/jpg'})
        response = self.json_ok('info', {'uid': self.user_1.uid, 'path': self.path, 'meta': ''})
        self.existing_resource_id = response['meta']['resource_id']

    def test_request_for_noninit_user_with_passport_bad_response(self):
        """Протестировать, что при запросе информации пользователем с аккаунтом без пароля он получит 403 ошибку."""
        uid = '123456789'
        user_info = deepcopy(DEFAULT_USERS_INFO[self.uid])
        user_info['uid'] = uid
        with self.specified_client(scopes=['yadisk:all'], uid=uid):
            with PassportStub(userinfo=user_info) as stub:
                stub.subscribe.side_effect = passport.errors_map['accountwithpasswordrequired']
                response = self.client.request(
                    self.method,
                    self.url % self.existing_resource_id,
                )
                assert response.status_code == 403
                content = json.loads(response.content)
                assert content['error'] == 'DiskUnsupportedUserAccountTypeError'
                assert content['description'] == 'User account type is not supported.'

    def test_request_for_blocked_user(self):
        uid = self.user_1.uid
        opts = {
            'uid': uid,
            'moderator': 'moderator',
            'comment': 'comment',
        }
        self.support_ok('block_user', opts)
        with self.specified_client(scopes=['yadisk:all'], uid=uid):
            response = self.client.request(
                self.method,
                self.url % self.existing_resource_id,
            )
            content = json.loads(response.content)
            assert response.status_code == 403
            assert content['error'] == 'DiskUserBlockedError'
            assert content['description'] == 'User is blocked.'

    def test_request_for_noninit_user(self):
        uid = '123456789'
        user_info = deepcopy(DEFAULT_USERS_INFO[self.uid])
        user_info['uid'] = uid
        with self.specified_client(scopes=['yadisk:all'], uid=uid):
            with PassportStub(userinfo=user_info):
                response = self.client.request(
                    self.method,
                    self.url % self.existing_resource_id,
                )
                # init and retry with _auto_initialize_user
                assert response.status_code == 404


class CreateResourceHandlerTestCase(BaseYaTeamTestCase, CommonDiskTestCase, UserTestCaseMixin, DiskApiTestCase):
    api_version = 'v1'
    api_mode = tags.platform.EXTERNAL
    endpoint = 'disk/resources'

    @parameterized.expand([
        (
            'ru', {
                'social': u'/Социальные сети/',
                'facebook': u'/Социальные сети/Facebook',
                'google': u'/Социальные сети/Google+',
                'vkontakte': u'/Социальные сети/ВКонтакте',
                'mailru': u'/Социальные сети/Мой Мир',
                'odnoklassniki': u'/Социальные сети/Одноклассники',
                'instagram': u'/Социальные сети/Instagram'
            }
        ),
        (
            'en', {
                'social': u'/Social networks/',
                'facebook': u'/Social networks/Facebook',
                'google': u'/Social networks/Google+',
                'vkontakte': u'/Social networks/VK',
                'mailru': u'/Social networks/Мой Мир',
                'odnoklassniki': u'/Social networks/Одноклассники',
                'instagram': u'/Social networks/Instagram'
            }
        ),
        (
            'tr', {
                'social': u'/Sosyal ağlar/',
                'facebook': u'/Sosyal ağlar/Facebook',
                'google': u'/Sosyal ağlar/Google+',
                'vkontakte': u'/Sosyal ağlar/VK',
                'mailru': u'/Sosyal ağlar/Мой Мир',
                'odnoklassniki': u'/Sosyal ağlar/Одноклассники',
                'instagram': u'/Sosyal ağlar/Instagram'
            }
        ),
        (
            'uk', {
                'social': u'/Соціальні мережі/',
                'facebook': u'/Соціальні мережі/Facebook',
                'google': u'/Соціальні мережі/Google+',
                'vkontakte': u'/Соціальні мережі/ВКонтакте',
                'mailru': u'/Соціальні мережі/Мой Мир',
                'odnoklassniki': u'/Соціальні мережі/Одноклассники',
                'instagram': u'/Соціальні мережі/Instagram'
            }
        )
    ])
    def test_create_system_dirs_for_social_folders(self, locale, mapping):
        u"""Проверить создание папок для социальных сетей.

        Создаем все папки социальных сетей, делаем листинг по общей папке социальных сетей,
        проверяем что для каждой папки проставлен корректный folder_type и корректный путь в
        зависимости от локали.
        """
        uid = self.user_3.uid
        self.create_user(uid, locale=locale)
        with self.specified_client(scopes=['cloud_api:disk.write'], uid=uid):
            for folder_type, folder_path in mapping.items():
                resp = self.client.put(self.endpoint, query={'path': folder_path})
                self.assertEqual(resp.status_code, 201)

        result = self.json_ok('list', {'uid': uid, 'path': '/disk' + mapping['social'], 'meta': ''})
        folder_type_to_path = {o['meta']['folder_type']: o['id'] for o in result}
        for folder_type, folder_path in mapping.items():
            assert folder_type in folder_type_to_path
            assert '/disk' + folder_path.rstrip('/') == folder_type_to_path[folder_type].rstrip('/')

    def test_write_to_share_folder_with_ro_rights(self):
        """Попытка записи в ОП с правами RO"""
        self.create_user(self.user_1.uid)
        self.create_user(self.user_3.uid)

        self.json_ok('mkdir', {'uid': self.user_1.uid, 'path': '/disk/shared_folder'})
        self.share_dir(self.user_1.uid, self.user_3.uid, self.user_3.email, '/disk/shared_folder', rights=640)
        with self.specified_client(scopes=['cloud_api:disk.write'], uid=self.user_3.uid):
            resp = self.client.put(self.endpoint, query={'path': '/shared_folder/test'})
            assert resp.status_code == 403
            data = from_json(resp.content)
            assert data['error'] == 'DiskNoWritePermissionForSharedFolderError'

    def test_setprop_to_share_folder_with_ro_rights(self):
        """Попытка выставить пользовательские данные в ОП с правами RO"""
        self.create_user(self.user_1.uid)
        self.create_user(self.user_3.uid)

        self.json_ok('mkdir', {'uid': self.user_1.uid, 'path': '/disk/shared_folder'})
        self.share_dir(self.user_1.uid, self.user_3.uid, self.user_3.email, '/disk/shared_folder', rights=640)
        self.upload_file(self.user_1.uid, '/disk/shared_folder/enot.jpg')
        # Приглашенный делает операции с токеном с правами на запись + чтение
        with self.specified_client(scopes=['cloud_api:disk.write', 'cloud_api:disk.read'], uid=self.user_3.uid):
            resp = self.client.patch(self.endpoint,
                                     query={'path': '/shared_folder/enot.jpg'},
                                     data={'custom_properties': {'overwrite user key': 'trash data'}})
            assert resp.status_code == 403
            data = from_json(resp.content)
            assert data['error'] == 'DiskNoWritePermissionForSharedFolderError'

    def test_dir_with_nda_path_is_created_as_common_dir(self):
        uid = self.user_3.uid
        self.create_user(uid)
        with self.specified_client(scopes=['cloud_api:disk.write'], uid=uid):
            resp = self.client.put(self.endpoint, query={'path': 'Yandex Team (NDA)'})
            self.assertEqual(resp.status_code, 201)
            resp = self.client.put(self.endpoint, query={'path': 'Yandex Team (NDA)'})
            self.assertEqual(resp.status_code, 409)

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='postgres test')
    def test_dir_with_nda_path_is_created_as_common_dir_for_yateam_user(self):
        uid = self.user_3.uid
        self.create_user(uid)
        self._assert_no_nda_folder(uid)
        self._make_yateam(uid)
        self._assert_nda_folder(uid)
        with self.specified_client(scopes=['cloud_api:disk.write'], uid=uid):
            resp = self.client.put(self.endpoint, query={'path': 'Yandex Team (NDA)'})
            self.assertEqual(resp.status_code, 409)


class SearchResourcesHandlerTestCase(CommonJsonApiTestCase, CommonDiskTestCase, UserTestCaseMixin, DiskApiTestCase):
    api_version = 'v1'
    api_mode = tags.platform.INTERNAL
    endpoint = 'disk/resources/search'

    folder_name = u'Цветной бульвар'
    file_name = u'Цветной переулок.txt'
    search_string = u'цветной'
    folder_path = u'/disk/%s' % folder_name
    file_path = u'/disk/%s' % file_name

    search_service_response = to_json({
       'request': '%(search_string)s',
       'sortBy': {
          'how': 'mtime',
          'order': 'descending',
          'priority': 'no'
       },
       'groupings': [
          {
             'attr': '',
             'categ': '',
             'docs': '1',
             'groups-on-page': '2',
             'mode': 'flat'
          }
       ],
       'response': {
          'found': {
             'all': '2',
             'phrase': '2',
             'strict': '2'
          },
          'results': [
             {
                'attr': '',
                'docs': '1',
                'found': {
                   'all': '2',
                   'phrase': '0',
                   'strict': '0'
                },
                'groups': [
                   {
                      'doccount': '1',
                      'relevance': '0539fda541c4d1dad76c013ef94e3f5d2aa812f3cb32574749d1987a50f73514',
                      'documents': [
                         {
                            'docId': '0539fda541c4d1dad76c013ef94e3f5d2aa812f3cb32574749d1987a50f73514',
                            'url': '',
                            'relevance': '0539fda541c4d1dad76c013ef94e3f5d2aa812f3cb32574749d1987a50f73514',
                            'properties': {
                               'id': '0539fda541c4d1dad76c013ef94e3f5d2aa812f3cb32574749d1987a50f73514',
                               'key': '%(folder_path)s',
                               'scope': 'folder'
                            }
                         }
                      ]
                   },
                   {
                      'doccount': '1',
                      'relevance': '96e9f58fa7d383886c0633d95aae84b8ffa37ceca5ee01c1532248a943f8bd9d',
                      'documents': [
                         {
                            'docId': '96e9f58fa7d383886c0633d95aae84b8ffa37ceca5ee01c1532248a943f8bd9d',
                            'url': '',
                            'relevance': '96e9f58fa7d383886c0633d95aae84b8ffa37ceca5ee01c1532248a943f8bd9d',
                            'properties': {
                               'id': '96e9f58fa7d383886c0633d95aae84b8ffa37ceca5ee01c1532248a943f8bd9d',
                               'key': '%(file_path)s',
                               'scope': 'file'
                            }
                         }
                      ]
                   },
                ]
             }
          ]
       }
    }) % {'search_string': search_string, 'folder_path': folder_path, 'file_path': file_path}

    @parameterized.expand([
        (
            {'query': 'test'},
            {
                'path': '/disk', 'query': 'test', 'preview_size': 'S', 'preview_crop': '0',
                'amount': '20', 'offset': '0'
            }
        ),
        (
            {'query': '*', 'preview_size': 'M', 'preview_crop': '1'},
            {
                'path': '/disk', 'query': '*', 'preview_size': 'M', 'preview_crop': '1',
                'amount': '20', 'offset': '0'
            }
        ),
        (
            {'query': '*', 'preview_size': 'L', 'preview_crop': '1', 'limit': '3'},
            {
                'path': '/disk', 'query': '*', 'preview_size': 'L', 'preview_crop': '1',
                'amount': '3', 'offset': '0',
            }
        ),
        (
            {'query': '*', 'preview_size': 'XL', 'preview_crop': '1', 'limit': '5'},
            {
                'path': '/disk', 'query': '*', 'preview_size': 'XL', 'preview_crop': '1',
                'amount': '5', 'offset': '0',
            }
        ),
        (
            {'query': 'lol kek cheburek'},
            {
                'query': 'lol kek cheburek', 'path': '/disk', 'preview_crop': '0', 'preview_size': 'S',
                'amount': '20', 'offset': '0'
            }
        )
    ])
    def test_request_proxied_to_new_search(self, query, mpfs_params):
        u"""Протестировать что GET-параметры правильно проксируются при запросе в MPFS."""
        self.create_user(self.uid)

        original_open_url = SearchResourcesHandler.service.open_url

        def _new_open_url(*_args, **_kwargs):
            """Изменяет ответ только для mpfs-ручки `new_search`, ответ остальных оставляет как есть."""
            (_url,) = _args
            _parsed_url = urlparse.urlparse(_url)
            if _parsed_url.path == '/json/new_search':
                return (
                    200,
                    to_json({
                        'query': query, 'results': [
                            {
                                'ctime': 1333569600, 'mtime': 1333569600, 'path': '/disk', 'utime': 0,
                                'type': 'dir', 'id': '/disk/', 'name': 'disk'
                            }
                        ]
                    }), {}
                )
            else:
                return original_open_url(*_args, **_kwargs)

        with self.specified_client(scopes=WebDavPermission.scopes, uid=self.uid), \
             mock.patch.object(SearchResourcesHandler.service, 'open_url', wraps=_new_open_url) as mocked_open_url:
            response = self.client.get(self.endpoint, query=query)
            assert response.status_code == 200

            assert mocked_open_url.called
            args, kwargs = mocked_open_url.call_args
            (url,) = args

            parsed_url = urlparse.urlparse(url)
            assert parsed_url.path == '/json/new_search'
            parsed_qs = urlparse.parse_qs(parsed_url.query)

            for param in mpfs_params:
                assert param in parsed_qs
                assert parsed_qs[param] == [mpfs_params[param]], param
                parsed_qs.pop(param)

            assert 'uid' in parsed_qs
            assert parsed_qs['uid'] == [self.uid]
            parsed_qs.pop('uid')

            assert 'meta' in parsed_qs
            parsed_qs.pop('meta')
            parsed_qs.pop('count_lost_results')

            assert 'skip_overdraft_check' in parsed_qs
            parsed_qs.pop('skip_overdraft_check')

            assert not parsed_qs

    def test_response_format(self):
        """Протестировать формат ответа ручки.

        Создаем 2 ресурса, а именно одну папку и один файл. Мокаем ответ сервиса поиска будто он их и вернул при поиске,
        то есть его данные совпадают с нашими в базе, а дальше проверяем формат ответа.
        """
        self.create_user(self.uid)

        self.json_ok('mkdir', {'path': self.folder_path, 'uid': self.uid})
        self.upload_file(self.uid, self.file_path)

        with mock.patch.object(DiskSearch, 'open_url', return_value=self.search_service_response), \
             self.specified_client(scopes=WebDavPermission.scopes, uid=self.uid):
            response = self.client.get(self.endpoint, query={'query': self.search_string})
            result = from_json(response.content)
            assert_that(result, has_keys(['items', 'limit', 'offset']))
            assert_that(result, not_(has_keys(['total'])))
            assert len(result['items']) == 2
            first, second = result['items']
            # отличается одинм полем от обычного ресурса
            assert first.pop('search_scope') == 'folder'
            assert second.pop('search_scope') == 'file'

            response = self.client.get('disk/resources', uid=self.uid, query={'path': self.folder_name})
            folder_response = from_json(response.content)
            folder_response.pop('_embedded')
            response = self.client.get('disk/resources', uid=self.uid, query={'path': self.file_name})
            file_response = from_json(response.content)

            assert folder_response == first
            assert file_response == second

    def test_response_format_2(self):
        """Протестировать формат ответа ручки.

        Создаем 2 ресурса, а именно одну папку и один файл. Мокаем ответ сервиса поиска будто он их и вернул при поиске,
        то есть его данные совпадают с нашими в базе, а дальше проверяем формат ответа.
        """
        self.create_user(self.uid)

        self.json_ok('mkdir', {'path': self.folder_path, 'uid': self.uid})
        self.upload_file(self.uid, self.file_path)

        with mock.patch.object(DiskSearch, 'open_url', return_value=self.search_service_response), \
             self.specified_client(scopes=WebDavPermission.scopes, uid=self.uid):
            response = self.client.get(self.endpoint, query={'query': self.search_string, 'limit': 2})
            result = from_json(response.content)
            assert 'iteration_key' in result
            assert result['iteration_key'] == '2;2'

            response = self.client.get(self.endpoint, query={'query': self.search_string, 'limit': 3})
            result = from_json(response.content)
            assert 'iteration_key' not in result

    def test_search_permitted_app_has_rights(self):
        """Проверить возможность поиска приложением, которому разрешен доступ."""
        self.create_user(self.uid)

        self.json_ok('mkdir', {'path': self.folder_path, 'uid': self.uid})
        self.upload_file(self.uid, self.file_path)

        with mock.patch.object(DiskSearch, 'open_url', return_value=self.search_service_response), \
             self.specified_client(scopes=DiskSearchPermission.scopes, uid=self.uid):
            response = self.client.get(self.endpoint, query={'query': self.search_string})
            assert response.status_code == 200


class GeoSearchResourcesHandlerTestCase(CommonJsonApiTestCase, CommonDiskTestCase, UserTestCaseMixin, DiskApiTestCase):
    api_version = 'v1'
    api_mode = tags.platform.INTERNAL
    endpoint = 'disk/resources/search/ugc'

    file_name = u'test.jpg'
    file_path = u'/disk/%s' % file_name

    @parameterized.expand((
        ('all_params',
         {'latitude': 55,
          'longitude': 37,
          'distance': 1000,
          'start_date': datetime.now().isoformat(),
          'end_date': datetime.now().isoformat()}),
        ('minimal_required_params',
         {'start_date': datetime.now().isoformat(),
          'end_date': datetime.now().isoformat()}),
        ('extra_param',
         {'latitude': 55,
          'start_date': datetime.now().isoformat(),
          'end_date': datetime.now().isoformat()}, FieldsAllOrNoneValidationError.code)
    ))
    def test_response_format(self, name, query, expected_error = None):

        self.create_user(self.uid)
        self.upload_file(self.uid, self.file_path)

        with DiskGeoSearchSmartMockHelper.mock(),\
             self.specified_client(scopes=DiskSearchPermission.scopes, uid=self.uid):
            DiskGeoSearchSmartMockHelper.add_to_index(self.uid, self.file_path)
            response = self.client.get(self.endpoint, query=query)
            result = from_json(response.content)
            if (expected_error):
                assert result['error'] == expected_error
            else:
                assert_that(result, has_keys(['items', 'limit', 'offset']))
                assert_that(result, not_(has_keys(['total'])))
                assert len(result['items']) == 1
                item = result['items'][0]
                assert item.pop('name') == self.file_name


class UploadAttachHandlerTestCase(CommonJsonApiTestCase, CommonDiskTestCase, UserTestCaseMixin, DiskApiTestCase):
    api_version = 'v1'
    api_mode = tags.platform.INTERNAL
    endpoint = 'disk/attach/resources/upload'

    # отключаем дефолтную заглушку паспорта, так как нам нужная более тонкая
    # настройка для проверки
    stubs_manager = StubsManager(
        class_stubs=set(StubsManager.DEFAULT_CLASS_STUBS) - {PassportStub}
    )

    test_mailish_user_uid_1 = '4008083026'

    def run_000_user_check(self, uid=None):
        pass

    def test_200_response_format(self):
        with PassportStub():
            uid = self.uid
            self.create_user(uid)
            with self.specified_client(scopes=DiskAttachWritePermission.scopes, uid=uid):
                response = self.client.put(self.endpoint, query={'path': 'test.jpg'})
                data = from_json(response.content)
                assert 'operation_link' in data
                assert 'upload_link' in data

                assert 'href' in data['operation_link']
                assert 'method' in data['operation_link']
                assert 'templated' in data['operation_link']

                assert 'href' in data['upload_link']
                assert 'method' in data['upload_link']
                assert 'templated' in data['upload_link']

                assert response.status_code == 200

    def test_get_post_delete_request(self):
        with PassportStub():
            uid = self.uid
            self.create_user(uid)
            with self.specified_client(scopes=DiskAttachWritePermission.scopes, uid=uid):
                response = self.client.get(self.endpoint, query={'path': 'test.jpg'})
                assert response.status_code == 405

                response = self.client.post(self.endpoint, query={'path': 'test.jpg'})
                assert response.status_code == 405

                response = self.client.delete(self.endpoint, query={'path': 'test.jpg'})
                assert response.status_code == 405

    def test_new_user_auto_inited(self):
        uid = self.test_mailish_user_uid_1
        result = self.json_ok('user_check', {'uid': uid})
        assert result['need_init'] == '1'

        with MailishUserStub(uid=uid), self.specified_client(scopes=DiskAttachWritePermission.scopes, uid=uid):
            response = self.client.put(self.endpoint, query={'path': 'test.jpg'})
            assert response.status_code == 200

            result = self.json_ok('user_check', {'uid': uid})
            assert result['need_init'] == '0'

    def test_store_attach_operation_has_data_key(self):
        # https://st.yandex-team.ru/CHEMODAN-37726
        with PassportStub():
            uid = self.uid
            self.create_user(uid)
            with self.specified_client(scopes=DiskAttachWritePermission.scopes, uid=uid):
                response = self.client.put(self.endpoint, query={'path': 'test.jpg'})
                data = from_json(response.content)
                operation_id = os.path.basename(urlparse.urlparse(data['operation_link']['href']).path)

            KladunMocker().mock_kladun_callbacks_for_store(uid, operation_id)

            mpfs_status = self.json_ok('status', {'uid': uid, 'oid': operation_id, 'meta': 'short_url'})
            assert 'short_url' in mpfs_status['resource']['meta']

            with self.specified_client(scopes=DiskAttachWritePermission.scopes, uid=uid):
                response = self.client.get(data['operation_link']['href'])
                data = from_json(response.content)
                assert 'status' in data
                assert 'data' in data
                assert 'public_url' in data['data']


class GetOperationStatusHandlerTestCase(CommonJsonApiTestCase, CommonDiskTestCase, UserTestCaseMixin, DiskApiTestCase):
    api_version = 'v1'
    api_mode = tags.platform.INTERNAL

    def test_store_operation_has_no_data_key(self):
        # https://st.yandex-team.ru/CHEMODAN-37726
        with PassportStub():
            uid = self.uid
            self.create_user(uid)
            data = self.json_ok('store', {'uid': uid, 'path': '/disk/test.jpg'})
            operation_id = data['oid']

            KladunMocker().mock_kladun_callbacks_for_store(uid, operation_id)

            self.json_ok('set_public', {'uid': uid, 'path': '/disk/test.jpg'})

            with self.specified_client(uid=uid):
                response = self.client.get('disk/operations/%s' % operation_id)
                data = from_json(response.content)
                assert 'status' in data
                assert 'data' not in data


class PhotounlimLastModifiedTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    method = 'GET'
    url = 'disk/photounlim/resources/last-modified'

    def setup_method(self, method):
        super(PhotounlimLastModifiedTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)
        self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})
        self.db = CollectionRoutedDatabase()

    #case for CHEMODAN-77607
    @pytest.mark.skipif(True, reason='Wait for fix in CHEMODAN-77607')
    def test_batch_edge(self):
        # Загружаем нужное количество файлов
        now = int(time.time())
        elem = [(1, now + 1), (2, now + 2), (3, now + 2), (4, now + 4)]
        i = 1
        for (name, mtime) in elem:
            i = i + 1
            self.upload_photostream_with_mtime('%s.jpg' % name, mtime)
        time.sleep(0.7)

        self.validate_count(2, len(elem))


    @parameterized.expand((('empty', 0),
                           ('less_than_limit', 9),
                           ('equal_to_limit', 10),
                           ('more_than_limit', 12)))
    def test_list_photounlim_last_modified(self, case_name, photo_count):
        # Загружаем нужное количество файлов
        now = int(time.time())
        for i in xrange(photo_count):
            self.upload_photostream_with_mtime('%s.jpg' % i, now + i)
        time.sleep(0.7)

        self.validate_count(10, photo_count)

    def validate_count(self, batch_size, exptected_count):
        # TODO(kis8ya): нужно подправить продакшен-код, чтобы было удобно проверять
        from mpfs.frontend.api import Default
        original_process = Default.process_core_method
        def _mocked_process(*args, **kwargs):
            obj, method, kw = args
            kw['args']['bounds']['amount'] = batch_size
            args = (obj, method, kw)

            original_process(*args, **kwargs)

        with self.specified_client(scopes=WebDavPermission.scopes, uid=self.uid), \
            mock.patch('mpfs.frontend.api.Default.process_core_method', _mocked_process):
            actual_items = []
            response = self.client.get(self.url)
            data = from_json(response.content)
            current_items = data['items'][1:]
            iteration_key = data['iteration_key']
            actual_items.extend(current_items)
            while current_items:
                response = self.client.get(self.url,
                                           query={'iteration_key': iteration_key})
                data = from_json(response.content)
                current_items = data['items'][1:]
                iteration_key = data['iteration_key']
                actual_items.extend(current_items)
        uniq_items = {item['path'] for item in actual_items}
        assert len(uniq_items) == exptected_count

    def upload_photostream_with_mtime(self, filename, mtime):
        self.upload_file(self.uid, ('/photostream/%s') % filename, headers=UnlimitedAreaTestCase.mobile_headers)
        path = '/photounlim/%s' % filename
        doc = self.db.photounlim_data.find_one({'uid': self.uid, 'key': path})
        doc['data']['mtime'] = mtime
        doc['parent'] = doc['key'].rsplit('/', 1)[0]
        self.db.photounlim_data.update({'uid': self.uid, 'path': path}, doc, upsert=True)

    @parameterized.expand((('all', 0, 101),
                           ('nothing', 101, 0),
                           ('part', 101, 121)))
    def test_start_date_list_photounlim_last_modified(self, case_name, before_photo_count, after_photo_count):
        date = datetime.now().date() - timedelta(days=30)
        mtime = calendar.timegm(date.timetuple())

        # Загружаем фотографии до даты, от которой будем делать выборку
        for i in xrange(before_photo_count):
            created = mtime - random.randint(1, 100)
            created = mtime - random.randint(1, 10000)
            name = 'before_%s.jpg' % i
            self.upload_file(self.uid, '/photostream/%s' % name,
                             headers=UnlimitedAreaTestCase.mobile_headers,
                             opts={'mtime': created, 'ctime': created})
            # Модифицируем mtime в базе, т.к. после заливки он обновлен до текущего времени
            doc = self.db.photounlim_data.find_one({'uid': self.uid, 'key': '/photounlim/%s' % name})
            doc['data']['mtime'] = created
            doc['parent'] = doc['key'].rsplit('/', 1)[0]
            self.db.photounlim_data.update({'uid': self.uid, 'path': '/photounlim/%s' % name},
                                           doc, upsert=True)

        # Загружаем фотографии после даты, от которой будем делать выборку
        for i in xrange(after_photo_count):
            created = mtime + random.randint(1, 100)
            created = mtime + random.randint(1, 10000)
            name = 'after_%s.jpg' % i
            self.upload_file(self.uid, '/photostream/%s' % name,
                             headers=UnlimitedAreaTestCase.mobile_headers,
                             opts={'mtime': created, 'ctime': created})
            doc = self.db.photounlim_data.find_one({'uid': self.uid, 'key': '/photounlim/%s' % name})
            doc['data']['mtime'] = created
            doc['parent'] = doc['key'].rsplit('/', 1)[0]
            self.db.photounlim_data.update({'uid': self.uid, 'path': '/photounlim/%s' % name},
                                           doc, upsert=True)

        with self.specified_client(scopes=WebDavPermission.scopes, uid=self.uid):
            actual_items = []
            response = self.client.get(self.url, query={'start_date': date.isoformat()})
            data = from_json(response.content)
            current_items = data['items'][1:]
            iteration_key = data['iteration_key']
            actual_items.extend(current_items)
            while current_items:
                response = self.client.get(self.url,
                                           query={'iteration_key': iteration_key})
                data = from_json(response.content)
                current_items = data['items'][1:]
                iteration_key = data['iteration_key']
                actual_items.extend(current_items)

            # Запрашиваем данные еще раз, чтобы проверить, что не придут данные лишний раз (или лишнии данные)
            response = self.client.get(self.url,
                                       query={'iteration_key': iteration_key})
            data = from_json(response.content)
            current_items = data['items'][1:]
            assert len(current_items) == 0
            assert iteration_key == data['iteration_key']

        uniq_items = {item['path'] for item in actual_items}
        assert len(uniq_items) == after_photo_count

    def test_endpoint_hidden_from_external_swagger(self):
        resp = self.client.get('schema/resources/v1/disk/photounlim/resources/last-modified')
        assert 'photounlim/resources/last-modified' not in resp.content

    def test_file_field_in_resource_info(self):
        self.upload_file(self.uid, '/photostream/0.jpg', headers=UnlimitedAreaTestCase.mobile_headers)

        with self.specified_client(scopes=WebDavPermission.scopes, uid=self.uid):
            response = self.client.get(self.url)
            data = from_json(response.content)
            items = data['items'][1:]

            assert 'file' in items[0]


class PhotounlimLastModifiedVisibleForInternalSwaggerTestCase(DiskApiTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'

    def test_endpoint_visible_for_internal_swagger(self):
        resp = self.client.get('schema/resources/v1/disk/photounlim/resources/last-modified')
        assert 'photounlim/resources/last-modified' in resp.content


class UploadClientHandlerTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_version = 'v1'
    api_mode = tags.platform.INTERNAL
    url = 'disk/clients/fos/resources/upload'

    def setup_method(self, method):
        super(UploadClientHandlerTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)

    def test_upload_success(self):
        with self.specified_client(scopes=WebDavPermission.scopes):
            resp = self.client.put(self.url,
                                   query={
                                       'app_version': 'test_version',
                                       'reply_email': 'test@test.test',
                                       'expire_seconds': '1211222',
                                       'os_version': 'test_os',
                                       'subject': 'test_theme',
                                       'recipient_type': 'testers',
                                   },
                                   data={'fos_support_text': 'what'},
                                   headers={'Content-Type': 'application/json'})
            assert resp.status_code == 200
            resp = from_json(resp.content)
            assert not resp.viewkeys() ^ set(UploadResourceSerializer.visible_fields)
            assert not resp['operation_link'].viewkeys() ^ set(LinkSerializer.visible_fields)
            assert not resp['upload_link'].viewkeys() ^ set(LinkSerializer.visible_fields)

    @parameterized.expand([
        ('recipient_type', 'test@test.test', 'someone', {'fos_support_text': 'what'}),
        ('email', 'test@test@t.st', 'testers', {'fos_support_text': 'what'}),
        ('no_email', None, 'testers', {'fos_support_text': 'what'}),
    ])
    def test_upload_bad(self, test_name, reply_email, recipient_type, data):
        with self.specified_client(scopes=WebDavPermission.scopes):
            resp = self.client.put(self.url,
                                   query={
                                       'app_version': 'test_version',
                                       'reply_email': reply_email,
                                       'expire_seconds': '1211222',
                                       'os_version': 'test_os',
                                       'subject': 'test_theme',
                                       'recipient_type': recipient_type,
                                   },
                                   data=data,
                                   headers={'Content-Type': 'application/json'})
            assert resp.status_code == 400
            assert 'FieldValidationError' in resp.content

    def test_upload_no_support_text(self):
        with self.specified_client(scopes=WebDavPermission.scopes):
            resp = self.client.put(self.url,
                                   query={
                                       'app_version': 'test_version',
                                       'reply_email': 'test@test.test',
                                       'expire_seconds': '1211222',
                                       'os_version': 'test_os',
                                       'subject': 'test_theme',
                                       'recipient_type': 'testers',
                                   },
                                   data={'fos_support_tsxt': 'what'},
                                   headers={'Content-Type': 'application/json'})
            assert resp.status_code == 400
            assert 'DiskClientBadRequest' in resp.content

    @parameterized.expand([
        (False, 'false'),
        (True, 'true')
    ])
    def test_email_template_fill(self, is_paid, header_paid):
        SUPPORT_EMAILS = settings.feedback['fos_email']
        with self.specified_client(scopes=WebDavPermission.scopes), \
             mock.patch('mpfs.core.user.standart.StandartUser.is_paid', return_value=is_paid),\
             mock.patch('mpfs.core.queue.mpfs_queue.put') as mock_obj:
                fos_path = '/client/Report-2019-01-25'
                app_version = 'test_version'
                reply_email = 'test@test.test'
                expire_seconds = '1211222'
                os_version = 'test_os'
                subject = 'test_theme'
                support_text = 'what a wonderful test'
                recipient_type = 'testers'
                self.upload_file(self.uid, fos_path,
                                 opts={
                                     'fos_app_version': app_version,
                                     'fos_reply_email': reply_email,
                                     'fos_expire_seconds': expire_seconds,
                                     'fos_os_version': os_version,
                                     'fos_subject': subject,
                                     'fos_recipient_type': recipient_type
                                 },
                                 json={'fos_support_text': support_text})
                resp = self.json_ok('info', {'uid': self.uid, 'path': fos_path})
                assert resp
                data = None
                for call in mock_obj.call_args_list:
                    if call[0][-1] == 'send_email':
                        data = call[0][0]
                assert data
                assert not data.viewkeys() ^ {'email_to', 'template_name', 'sender_name', 'sender_email', 'template_args', 'headers'}
                assert not data['template_args'].viewkeys() ^ {'body', 'subject'}
                assert data['template_args']['subject'] == '%s - %s - %s' % (os_version, app_version, subject)
                assert support_text in data['template_args']['body']
                assert os_version in data['template_args']['body']
                assert app_version in data['template_args']['body']
                assert data['email_to'] == SUPPORT_EMAILS[recipient_type]
                assert data['headers']['x-otrs-paid'] == header_paid


class GettingSourceIdsFromResourceHandlerBase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):

    def setup_method(self, method):
        super(GettingSourceIdsFromResourceHandlerBase, self).setup_method(method)

        self.create_user(self.uid, noemail=1)
        self.no_source_ids_path = '/disk/no_source_ids.jpg'
        self.no_source_ids_name = '/no_source_ids.jpg'
        self.upload_file(self.uid, self.no_source_ids_path)
        r = self.json_ok('info', {'uid': self.uid, 'path': self.no_source_ids_path, 'meta': ''})
        no_source_id_hid = FileChecksums(r['meta']['md5'], r['meta']['sha256'], r['meta']['size']).hid

        self.source_ids_path = '/disk/source_ids.jpg'
        self.source_ids_name = '/source_ids.jpg'
        self.upload_file(self.uid, self.source_ids_path)
        r = self.json_ok('info', {'uid': self.uid, 'path': self.source_ids_path, 'meta': ''})
        md5, sha256, size = r['meta']['md5'], r['meta']['sha256'], r['meta']['size']
        source_id_hid = FileChecksums(r['meta']['md5'], r['meta']['sha256'], r['meta']['size']).hid
        self.source_ids = {'111', '222'}
        self.json_ok('add_source_ids',
                     {'uid': self.uid, 'md5': md5, 'sha256': sha256, 'size': size},
                     json={'source_ids': list(self.source_ids)})

        self.source_ids_exist_mock = self._generate_loading_source_ids_mock(self.uid, source_id_hid, self.source_ids)
        self.no_source_ids_mock = self._generate_loading_source_ids_mock(self.uid, no_source_id_hid, [])

    @staticmethod
    def _generate_loading_source_ids_mock(uid, hid, return_source_ids):
        return_values = [SourceIdDAOItem.build_by_params(uid, hid, source_id) for source_id in return_source_ids]
        return mock.patch(
            'mpfs.core.global_gallery.logic.controller.GlobalGalleryController.get_source_ids_for_hids',
            return_value=return_values)


class GettingSourceIdsFromResourceHandlersTestCase(GettingSourceIdsFromResourceHandlerBase):
    api_version = 'v1'
    api_mode = tags.platform.INTERNAL

    def test_no_source_ids_in_fields_file_doesnt_have_source_ids(self):
        with self.specified_client(scopes=['cloud_api:disk.read'], uid=self.uid), \
                self.no_source_ids_mock:
            r = self.client.request('GET', 'disk/resources', query={'path': self.no_source_ids_name})
        assert 'source_ids' not in from_json(r.content)

    def test_no_source_ids_in_fields_file_have_source_ids(self):
        with self.specified_client(scopes=['cloud_api:disk.read'], uid=self.uid), \
                self.no_source_ids_mock:
            r = self.client.request('GET', 'disk/resources', query={'path': self.source_ids_name,})
        assert 'source_ids' not in from_json(r.content)

    def test_source_ids_in_fields_file_doesnt_have_source_ids(self):
        with self.specified_client(scopes=['cloud_api:disk.read'], uid=self.uid), \
                self.no_source_ids_mock:
            r = self.client.request('GET', 'disk/resources', query={'path': self.no_source_ids_name, 'fields': 'source_ids'})
        content = from_json(r.content)
        assert 'source_ids' in content
        assert 'md5' not in content
        assert set() == set(content['source_ids'])

    def test_plus_source_ids_in_fields_file_doesnt_have_source_ids(self):
        with self.specified_client(scopes=['cloud_api:disk.read'], uid=self.uid), \
                self.no_source_ids_mock:
            r = self.client.request('GET', 'disk/resources', query={'path': self.no_source_ids_name, 'fields': '+source_ids'})
        content = from_json(r.content)
        assert 'source_ids' in content
        assert 'md5' in content
        assert set() == set(content['source_ids'])

    def test_source_ids_in_fields_file_have_source_ids(self):
        with self.specified_client(scopes=['cloud_api:disk.read'], uid=self.uid), \
                self.source_ids_exist_mock:
            r = self.client.request('GET', 'disk/resources', query={'path': self.source_ids_name, 'fields': 'source_ids'})
        content = from_json(r.content)
        assert 'source_ids' in content
        assert 'md5' not in content
        assert self.source_ids == set(content['source_ids'])

    def test_plus_source_ids_in_fields_file_have_source_ids(self):
        with self.specified_client(scopes=['cloud_api:disk.read'], uid=self.uid), \
                self.source_ids_exist_mock:
            r = self.client.request('GET', 'disk/resources', query={'path': self.source_ids_name, 'fields': '+source_ids'})
        content = from_json(r.content)
        assert 'source_ids' in content
        assert 'md5' in content
        assert self.source_ids == set(content['source_ids'])

    def test_regular_field_and_with_plus(self):
        with self.specified_client(scopes=['cloud_api:disk.read'], uid=self.uid), \
                self.source_ids_exist_mock:
            r = self.client.request('GET', 'disk/resources', query={'path': self.source_ids_name, 'fields': '+md5'})
        content = from_json(r.content)
        assert 'source_ids' not in content
        assert 'md5' in content


class GettingSourceIdsFromResourceHandlersExternalTestCase(GettingSourceIdsFromResourceHandlerBase):
    api_version = 'v1'
    api_mode = tags.platform.EXTERNAL

    @parameterized.expand([
        (None,),
        ('source_ids',),
        ('+source_ids',),
    ])
    def test_no_source_id_in_external_api(self, fields):
        self.api_mode = tags.platform.EXTERNAL
        opts = {
            'path': self.source_ids_name,
        }
        if fields is not None:
            opts['fields'] = fields
        with self.specified_client(scopes=['cloud_api:disk.read'], uid=self.uid), \
                self.source_ids_exist_mock:
            r = self.client.request('GET', 'disk/resources', query={'path': self.source_ids_name, 'fields': fields})
        content = from_json(r.content)
        assert 'source_ids' not in content


@pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='postgres tests')
class NativeClientsFieldsBase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_version = 'v1'
    api_mode = tags.platform.INTERNAL

    def setup_method(self, method):
        super(NativeClientsFieldsBase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)

        # file with image_metadata
        self.file_with_image_metadata_path = '/EHOT.jpg'
        mpfs_file_with_metadata_path = '/disk%s' % self.file_with_image_metadata_path
        self.aesthetics = 77.77777
        self.width = 360
        self.height = 480
        self.angle = 90
        self.upload_file(self.uid, mpfs_file_with_metadata_path, width=self.width, height=self.height, angle=self.angle)
        self.save_aesthetics(self.uid, mpfs_file_with_metadata_path, self.aesthetics)

        # file with video_info
        self.file_with_video_metadata_path = '/dancing_raccoon.avi'
        path = '/disk%s' % self.file_with_video_metadata_path
        self.upload_video(self.uid, path)

    def save_aesthetics(self, uid, path, aesthetics):
        s = Session.create_from_uid(uid)
        s.execute(
            'UPDATE disk.files SET ext_aesthetics=:aesthetics WHERE uid=:uid AND fid=(SELECT fid FROM code.path_to_fid(:path,:uid))',
            {'uid': uid, 'path': path, 'aesthetics': aesthetics}
        )


class ImageMetadataFieldsTestCase(NativeClientsFieldsBase):
    def test_image_metadata(self):
        with self.specified_client(scopes=['cloud_api:disk.read'], uid=self.uid):
            r = self.client.request('GET', 'disk/resources', query={'path': 'disk:%s' % self.file_with_image_metadata_path,
                                                                    'fields': 'image_metadata'})
        content = from_json(r.content)

        assert 'image_metadata' in content
        assert content['image_metadata']['width'] == self.width
        assert content['image_metadata']['height'] == self.height
        assert content['image_metadata']['angle'] == self.angle
        assert_that(content['image_metadata']['beauty'],
                    close_to(self.aesthetics, delta=0.001))

    @pytest.mark.skipif(True, reason='not implemented')
    def test_no_image_metadata_if_not_requested(self):
        with self.specified_client(scopes=['cloud_api:disk.read'], uid=self.uid):
            r = self.client.request('GET', 'disk/resources',
                                    query={'path': 'disk:%s' % self.file_with_image_metadata_path})
        content = from_json(r.content)

        assert 'image_metadata' not in content


class ImageMetadataFieldsExternalTestCase(NativeClientsFieldsBase):
    api_mode = tags.platform.EXTERNAL

    @parameterized.expand([
        (None,),
        ('image_metadata',),
        ('+image_metadata',),
    ])
    def test_no_fields_in_external_api(self, fields):
        opts = {
            'path': self.file_with_image_metadata_path,
        }
        if fields is not None:
            opts['fields'] = fields
        with self.specified_client(scopes=['cloud_api:disk.read'], uid=self.uid):
            r = self.client.request('GET', 'disk/resources', query=opts)
        content = from_json(r.content)

        assert 'image_metadata' not in content


class VideoMetadataFieldsTestCase(NativeClientsFieldsBase):
    def test_video_metadata(self):
        with self.specified_client(scopes=['cloud_api:disk.read'], uid=self.uid):
            r = self.client.request('GET', 'disk/resources', query={'path': 'disk:%s' % self.file_with_video_metadata_path,
                                                                    'fields': 'video_metadata'})
        content = from_json(r.content)

        assert 'video_metadata' in content
        assert 'duration' in content['video_metadata']
        assert isinstance(content['video_metadata']['duration'], int)

    @pytest.mark.skipif(True, reason='not implemented')
    def test_no_video_metadata_if_not_requested(self):
        with self.specified_client(scopes=['cloud_api:disk.read'], uid=self.uid):
            r = self.client.request('GET', 'disk/resources',
                                    query={'path': 'disk:%s' % self.file_with_video_metadata_path})
        content = from_json(r.content)

        assert 'video_metadata' not in content


class PutSnapshotHandlerTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, SupportApiTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'
    method = 'PUT'
    url = 'disk/resources/snapshot'
    file_path = '/disk/test1.jpg'
    file_data = {'mimetype': 'image/jpg'}

    def setup_method(self, method):
        super(PutSnapshotHandlerTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)
        self.upload_file(self.uid, self.file_path, file_data=self.file_data)

    def test_request_for_noninit_user_with_passport_bad_response(self):
        """Протестировать, что при запросе информации пользователем с аккаунтом без пароля он получит 403 ошибку."""
        uid = '123456789'
        user_info = deepcopy(DEFAULT_USERS_INFO[self.uid])
        user_info['uid'] = uid
        with PassportStub(userinfo=user_info) as stub:
            stub.subscribe.side_effect = passport.errors_map['accountwithpasswordrequired']
            response = self.client.request(self.method, self.url, uid=uid)
            assert response.status_code == 403
            content = json.loads(response.content)
            assert content['error'] == 'DiskUnsupportedUserAccountTypeError'
            assert content['description'] == 'User account type is not supported.'

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-69288')
    def test_request_for_blocked_user(self):
        opts = {
            'uid': self.uid,
            'moderator': 'moderator',
            'comment': 'comment',
        }
        self.support_ok('block_user', opts)
        response = self.client.request(self.method, self.url, uid=self.uid)
        content = json.loads(response.content)
        assert response.status_code == 403
        assert content['error'] == 'DiskUserBlockedError'
        assert content['description'] == 'User is blocked.'

    def test_request_for_noninit_user(self):
        uid = '123456789'
        user_info = deepcopy(DEFAULT_USERS_INFO[self.uid])
        user_info['uid'] = uid
        with PassportStub(userinfo=user_info):
            response = self.client.request(self.method, self.url, uid=uid)
            # init and retry with _auto_initialize_user
            assert response.status_code == 200


class ResourcesV2TestCase(CommonDiskTestCase, UserTestCaseMixin, DiskApiTestCase, UploadFileTestCaseMixin):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v2'
    method = 'GET'
    url = 'disk/resources'

    def setup_method(self, method):
        super(ResourcesV2TestCase, self).setup_method(method)
        self.path1 = '/disk/1.jpg'
        self.path2 = '/disk/2.jpg'
        self.upload_file(self.uid, self.path1)
        self.upload_file(self.uid, self.path2)
        self.res1_info = self.json_ok('info', {'uid': self.uid, 'path': self.path2, 'meta': 'resource_id'})
        self.res2_info = self.json_ok('info', {'uid': self.uid, 'path': self.path2, 'meta': 'resource_id'})
        self.img1_res_id = self.res1_info['meta']['resource_id']
        self.img2_res_id = self.res2_info['meta']['resource_id']

    def test_common_case_bulk_resources(self):
        with self.specified_client(scopes=['yadisk:all'], uid=self.uid), \
             DjfsApiMockHelper.mock_request(status_code=200, content=to_json([self.res1_info, self.res2_info])):
            resp = self.client.request(self.method,
                                       self.url,
                                       query={'resource_ids': '%s,%s' % (self.img1_res_id, self.img2_res_id)})
            result = json.loads(resp.content)
            assert len(result['items']) == 2

    def test_permissions(self):
        with DjfsApiMockHelper.mock_request(status_code=200, content=to_json([self.res1_info, self.res2_info])):
            scopes_to_status = (
                ([], 403),
                (['cloud_api:disk.read'], 403),
                (['cloud_api:disk.v2.resources.read'], 200),
            )
            query = {'resource_ids': '%s,%s' % (self.img1_res_id, self.img2_res_id)}
            self._permissions_test(scopes_to_status, self.method, self.url, query=query)


class ResourceOfficeFieldsTest(OfficeTestCase, DiskApiTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'
    method = 'GET'
    url = 'disk/resources'

    @mock.patch('mpfs.core.office.logic.only_office.OFFICE_ONLY_OFFICE_ENABLED', True)
    def test_get_public_office_fields(self):
        self.json_ok('office_set_selection_strategy', {'uid': self.uid,
                                                       'selection_strategy': 'force_oo'})
        file_path = '/disk/old.docx'
        self.upload_file(self.uid, file_path)
        self.json_ok('set_public', {'uid': self.uid, 'path': file_path})
        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path,
                                     'meta': ','})
        resource_id = info['meta']['resource_id']
        self.json_ok('office_set_access_state', {'uid': self.uid,
                                                 'resource_id': resource_id,
                                                 'access_state': OfficeAccessStateConst.ALL})
        info = self.json_ok('info', {'uid': self.uid, 'path': file_path, 'meta': ','})

        assert 'office_access_state' in info['meta']
        assert 'office_online_sharing_url' in info['meta']
        assert 'office_online_url' in info['meta']
        assert 'resource_id' in info['meta']

        with self.specified_client(scopes=['cloud_api:disk.read'], uid=self.uid):
            resp = self.client.request(self.method,
                                       'disk/resources',
                                       query={'path': 'disk:/old.docx', 'fields': 'office_access_state,office_online_sharing_url,office_online_url,resource_id'})
            result = json.loads(resp.content)
            assert 'office_access_state' in result
            assert 'office_online_sharing_url' in result
            assert 'office_online_url' in result
            assert 'resource_id' in result

            # проверяем, что не возвращаем лишние поля
            resp = self.client.request(self.method, 'disk/resources', query={'path': 'disk:/old.docx', 'fields': 'office_access_state'})
            result = json.loads(resp.content)
            assert 'office_access_state' in result
            assert 'office_online_sharing_url' not in result
            assert 'office_online_url' not in result
            assert 'resource_id' not in result

    @mock.patch('mpfs.core.office.logic.only_office.OFFICE_ONLY_OFFICE_ENABLED', True)
    def test_set_office_access_state(self):
        file_path = '/disk/old.docx'
        self.upload_file(self.uid, file_path)
        self.json_ok('set_public', {'uid': self.uid, 'path': file_path})
        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path,
                                     'meta': ','})
        file_id = info['meta']['file_id']

        resource_id = ResourceId(self.uid, file_id).serialize()

        info = self.json_ok('info', {'uid': self.uid, 'path': file_path, 'meta': ','})

        assert info['meta']['office_access_state'] == OfficeAccessStateConst.DISABLED

        with self.specified_client(scopes=['cloud_api:disk.write', 'cloud_api:disk.read'], uid=self.uid):
            resp = self.client.request(self.method,
                                       'disk/resources',
                                       query={'path': 'disk:/old.docx', 'fields': 'office_access_state'})
            result = json.loads(resp.content)
            assert result['office_access_state'] == OfficeAccessStateConst.DISABLED

            resp = self.client.request('POST',
                                       'disk/docs/resources/%s/office-access-state' % resource_id,
                                       query={'resource_id': resource_id, 'access_state': OfficeAccessStateConst.ALL})
            assert resp.status_code == 200
            resp = self.client.request(self.method,
                                       'disk/resources',
                                       query={'path': 'disk:/old.docx', 'fields': 'office_access_state'})
            result = json.loads(resp.content)
            assert result['office_access_state'] == OfficeAccessStateConst.ALL

        info = self.json_ok('info', {'uid': self.uid, 'path': file_path, 'meta': ','})

        assert info['meta']['office_access_state'] == OfficeAccessStateConst.ALL


class ResourceOfficeFieldsExternalTest(OfficeTestCase, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    method = 'GET'
    url = 'disk/resources'

    @mock.patch('mpfs.core.office.logic.only_office.OFFICE_ONLY_OFFICE_ENABLED', True)
    def test_get_public_office_fields(self):
        self.json_ok('office_set_selection_strategy', {'uid': self.uid,
                                                       'selection_strategy': 'force_oo'})
        file_path = '/disk/old.docx'
        self.upload_file(self.uid, file_path)
        self.json_ok('set_public', {'uid': self.uid, 'path': file_path})
        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path,
                                     'meta': ','})
        resource_id = info['meta']['resource_id']
        self.json_ok('office_set_access_state', {'uid': self.uid,
                                                 'resource_id': resource_id,
                                                 'access_state': OfficeAccessStateConst.ALL})
        info = self.json_ok('info', {'uid': self.uid, 'path': file_path, 'meta': ','})

        assert 'office_access_state' in info['meta']
        assert 'office_online_sharing_url' in info['meta']
        assert 'office_online_url' in info['meta']
        assert 'resource_id' in info['meta']

        with self.specified_client(scopes=['cloud_api:disk.read'], uid=self.uid):
            resp = self.client.request(self.method,
                                       'disk/resources',
                                       query={'path': 'disk:/old.docx', 'fields': 'office_access_state,office_online_sharing_url,office_online_url,resource_id'})
            result = json.loads(resp.content)
            # проверяем, что не возвращаем лишние поля для внешних клиентов, даже если запросили
            assert 'office_access_state' not in result
            assert 'office_online_sharing_url' not in result
            assert 'office_online_url' not in result
            assert 'resource_id' in result
