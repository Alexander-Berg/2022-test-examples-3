# -*- coding: utf-8 -*-
import base64
import hashlib
import inspect
import os
import socket
import time
import urllib2
import urlparse
from datetime import timedelta, datetime

import jwt
import mock
import pytest
from hamcrest import assert_that, has_entries
from lxml import etree
from mock import patch
from nose_parameterized import parameterized

import mpfs
from mpfs.common import errors
from mpfs.common.errors import OrchestratorNotFoundError
from mpfs.common.errors.share import GroupNoPermit
from mpfs.common.static import codes, tags
from mpfs.common.static.tags import COMMIT_FILE_INFO, COMMIT_FILE_UPLOAD, COMMIT_FINAL
from mpfs.common.util import to_json
# Такой порядок важен, тк. здесь происходит настройка
# тестового окружения
from mpfs.common.util.experiments.logic import enable_experiment_for_uid
from mpfs.config import settings
from mpfs.core import cache
from mpfs.core import factory
# Импортируем модуль, а не actions из него, чтобы избежать циклического импорта
# mpfs.core.office.actions -> mpfs.core.base -> mpfs.core.office.interface -> mpfs.core.office.actions
from mpfs.core import office
from mpfs.core.address import Address, ResourceId
from mpfs.core.factory import get_resource
from mpfs.core.filesystem.base import Filesystem
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase
from mpfs.core.filesystem.helpers.lock import LockHelper
from mpfs.core.filesystem.quota import Quota
from mpfs.core.metastorage.control import fs_locks, operations
from mpfs.core.office.errors import OfficeFileIsReadOnlyError
from mpfs.core.office.events import OfficeFileConvertedEvent
from mpfs.core.office.logic import microsoft, only_office
from mpfs.core.office.logic.microsoft import MicrosoftEditor
from mpfs.core.office.logic.only_office import OnlyOfficeEditor
from mpfs.core.office.logic.only_office_utils import OnlyOfficeToken
from mpfs.core.office.models import OfficeAllowedPDDDomain
from mpfs.core.office.operations import (OfficeLockingOperationStub, OfficeConvertFile,
                                         CopyMailAttachmentToDisk, HancomAutosaveOperation)
from mpfs.core.office.static import OfficeAccessStateConst, OfficeServiceIDConst
from mpfs.core.office.util import (OFFICE_SIZE_LIMITS, OFFICE_CONVERT_SIZE_LIMITS,
                                   make_resource_id, build_office_online_url,
                                   make_session_context)
from mpfs.core.operations import manager
from mpfs.core.services.discovery_service import DiscoveryService
from mpfs.core.services.mail_service import MailStidService
from mpfs.core.services.orchestrator_service import OrchestratorService
from mpfs.core.services.passport_service import Passport
from mpfs.core.services.search_service import DiskSearch
from mpfs.core.social.share.constants import SharedFolderRights
from mpfs.core.user.anonymous import AnonymousUID
from mpfs.core.user.base import StandartUser, User
from mpfs.core.user.constants import DEFAULT_LOCALE, YATEAM_DIR_PATH
from mpfs.core.user.dao.user import UserDAO
from mpfs.core.yateam.logic import make_yateam
from mpfs.dao.session import Session
from mpfs.metastorage.mongo.collections.base import UserIndexCollection
from mpfs.platform.v1.case.handlers import DocviewerYabrowserUrlService
from test.base import DiskTestCase, get_search_item_response
from test.base_suit import SharingTestCaseMixin
from test.conftest import REAL_MONGO, INIT_USER_IN_POSTGRES
from test.fixtures.users import pdd_user, user_1
from test.helpers.operation import PendingOperationDisabler
from test.helpers.size_units import KB, GB
from test.helpers.stubs.base import BaseStub
from test.helpers.stubs.services import KladunStub, HancomServiceStub
from test.helpers.utils import construct_requests_resp

REST_API_HOST = settings.office['only_office']['rest_api_host']
ONLY_OFFICE_LOCAL_PROXY = settings.office['only_office']['local_proxy']
ONLY_OFFICE_MPFS_TOKEN = settings.office['only_office']['mpfs_token_secret']


def only_office_settings_mock(office_only_office_enabled_for_yandex_nets=True, get_only_office_enabled=True, orchestrator_service=True):
    possible_func = office_only_office_enabled_for_yandex_nets
    if callable(possible_func):
        return only_office_settings_mock(True, get_only_office_enabled)(possible_func)

    def wrapper(fn):
        def wrapped(*args, **kwargs):
            with OnlyOfficeTestCase.OnlyOfficeSettingsMock(
                    office_only_office_enabled_for_yandex_nets,
                    get_only_office_enabled, orchestrator_service):
                return fn(*args, **kwargs)
        return wrapped
    return wrapper


class OfficeTestCase(SharingTestCaseMixin, DiskTestCase):
    """Базовый класс офисных тестов"""

    FILE_PATH = '/disk/test.docx'
    FILE_PATH_2 = '/disk/test.doc'
    FILE_PATH_3 = '/disk/test.jpg'
    WOPITEST_FILE_PATH = '/disk/test.wopitest'

    OTHER_UID = user_1.uid

    @classmethod
    def setup_class(cls):
        super(OfficeTestCase, cls).setup_class()
        with open('fixtures/xml/discovery.xml') as fd:
            xml = fd.read()
        with patch.object(DiscoveryService, 'open_url', return_value=xml):
            DiscoveryService().ensure_cache()

    def setup_method(self, method):
        super(OfficeTestCase, self).setup_method(method)

        file_size = 1024  # чтобы не срабатывали ограничения на размер

        self.create_user(self.uid, noemail=1)
        self.create_user(self.OTHER_UID, noemail=1)
        self.upload_file(self.uid, self.FILE_PATH, file_data={'size': file_size})
        self.upload_file(self.uid, self.FILE_PATH_2, file_data={'size': file_size})
        self.upload_file(self.uid, self.FILE_PATH_3, file_data={'size': file_size})
        self.upload_file(self.uid, self.WOPITEST_FILE_PATH, file_data={'size': file_size})

    @staticmethod
    def _get_lock(uid, path):
        fs = Filesystem()
        resource = get_resource(uid, Address.Make(uid, path))
        return fs.get_lock(resource)

    def _get_kladun_callback_bodies(self, path, file_md5=None, file_sha256=None, size=None):
        body_1 = etree.fromstring(open('fixtures/xml/kladun_store_1.xml').read())
        body_2 = etree.fromstring(open('fixtures/xml/kladun_store_2.xml').read())
        body_3 = etree.fromstring(open('fixtures/xml/kladun_store_3.xml').read())

        rand = str('%f' % time.time()).replace('.', '')[9:]

        if file_md5 is None:
            file_md5 = hashlib.md5(rand).hexdigest()

        if file_sha256 is None:
            file_sha256 = hashlib.sha256(rand).hexdigest()

        file_id = hashlib.sha256(file_md5 + ':' + file_sha256).hexdigest()

        if size is None:
            size = int(rand)

        mid_digest = '100000.yadisk:%s.%s' % (self.uid, int(file_md5[:16], 16))
        mid_file = '100000.yadisk:%s.%s' % (self.uid, int(file_md5[:16][::-1], 16))
        drweb = 'true'
        mimetype = 'application/x-www-form-urlencoded'

        for body in (body_1, body_2, body_3):
            body.find('request').find('chemodan-file-attributes').set('uid', self.uid)
            body.find('request').find('chemodan-file-attributes').set('file-id', file_id)
            body.find('request').find('chemodan-file-attributes').set('path', path)
            for arg in ('current', 'total'):
                body.find('stages').find('incoming-http').find('progress').set(arg, str(size))
            for tag in ('incoming-http', 'incoming-file'):
                body.find('stages').find(tag).find('result').set('content-length', str(size))
                body.find('stages').find(tag).find('result').set('content-type', mimetype)
            body.find('stages').find('incoming-file').find('result').set('md5', file_md5)
            body.find('stages').find('incoming-file').find('result').set('sha256', file_sha256)
        for body in (body_2, body_3):
            body.find('stages').find('mulca-file').find('result').set('mulca-id', mid_file)
            body.find('stages').find('mulca-digest').find('result').set('mulca-id', mid_digest)
            body.find('stages').find('antivirus').find('result').set('result', drweb)

        return (body_1, body_2, body_3)

    def _office_action_data(self, path, hardlink, size_constraint_failed, file_md5=None, file_sha256=None,
                            file_size=None, opts=None):
        """Проверяем ответ ручки, если требуется конвертация"""
        new_path = path + 'x'
        body_1, body_2, body_3 = self._get_kladun_callback_bodies(new_path, file_md5, file_sha256, file_size)

        with KladunStub(status_values=(body_1,
                                       body_2,
                                       body_3)):
            if opts is None:
                opts = {'uid': self.uid, 'action': 'edit', 'path': path}

            response = self.json_ok('office_action_data', opts)

            assert 'oid' in response
            oid = response['oid']

            if hardlink:
                with self.patch_mulca_is_file_exist(func_resp=True):
                    if size_constraint_failed:
                        # Callback #1
                        response = self.service_error('kladun_callback', {
                            'uid': self.uid,
                            'oid': oid,
                            'status_xml': body_1,
                            'type': tags.COMMIT_FILE_INFO,
                        })
                        self.assertEqual(response['code'], codes.KLADUN_CONFLICT)
                    else:
                        # Callback #1
                        response = self.service_error('kladun_callback', {
                            'uid': self.uid,
                            'oid': oid,
                            'status_xml': body_1,
                            'type': tags.COMMIT_FILE_INFO,
                        })
                        self.assertEqual(response['code'], codes.KLADUN_HARDLINK_FOUND)
            elif size_constraint_failed:
                    # Callback #1
                    response = self.service_error('kladun_callback', {
                        'uid': self.uid,
                        'oid': oid,
                        'status_xml': body_1,
                        'type': tags.COMMIT_FILE_INFO,
                    })
                    self.assertEqual(response['code'], codes.KLADUN_CONFLICT)
            else:
                for callback_type, body in (
                        (tags.COMMIT_FILE_INFO, body_1),
                        (tags.COMMIT_FILE_UPLOAD, body_2),
                        (tags.COMMIT_FINAL, body_3)):
                    opts = {'uid': self.uid, 'oid': oid, 'status_xml': body, 'type': callback_type}
                    self.service_ok('kladun_callback', opts)

        # сохраняем чтоб иметь возможность потом зачистить все созданные тестом файлы
        self.uploaded_files.append({'uid': self.uid, 'path': new_path})
        return oid

    def _office_action_data_edit(self, uid, file_path, service_id='disk',
                                 locale=None, ext=None, error_status=None, error_code=None, tld=None,
                                 post_message_origin=None, with_orchestrator=True):
        """наиболее типичное обращение к ручке office_action_data с 'action': 'edit'"""
        opts = {'uid': uid,
                'action': 'edit',
                'service_id': service_id,
                'service_file_id': file_path}
        if locale:
            opts['locale'] = locale
        if ext:
            opts['ext'] = ext
        if tld:
            opts['tld'] = tld
        if post_message_origin:
            opts['post_message_origin'] = post_message_origin
        if with_orchestrator:
            with enable_experiment_for_uid('only_office_orchestrator', self.uid):
                if error_status or error_code:
                    return self.json_error('office_action_data', opts, status=error_status, code=error_code)
                return self.json_ok('office_action_data', opts)
        else:
            if error_status or error_code:
                return self.json_error('office_action_data', opts, status=error_status, code=error_code)
            return self.json_ok('office_action_data', opts)


    def _upload_shared_file(self, uid, file_folder, file_path, invite_uids=[], file_data=None):
        try:
            self.json_ok('mkdir', {'uid': uid, 'path': file_folder})
        except:
            pass
        self.upload_file(uid, file_path, file_data=file_data)
        group = self.json_ok('share_create_group', {'uid': uid, 'path': file_folder})
        gid = group['gid']
        self.json_ok('set_public', {'uid': uid, 'path': file_path})
        for invite_uid in invite_uids:
            invite_hash = self.share_invite(gid, invite_uid, rights=660)
            try:
                self.json_ok('share_activate_invite', {'uid': invite_uid, 'hash': invite_hash})
            except:
                pass


class RightOfficeAllowed(OfficeTestCase):

    def test_only_office_allowed(self):
        resp = self.json_ok('office_action_check', {'uid': self.uid})
        assert resp['office_online_editor_type'] == 'microsoft_online'

    @only_office_settings_mock(get_only_office_enabled=None)
    def test_ms_for_yandex_not_in_vpn(self):
        resp = self.json_ok('office_action_check', {'uid': self.uid})
        assert resp['office_online_editor_type'] == 'microsoft_online'

    @only_office_settings_mock(office_only_office_enabled_for_yandex_nets=False)
    def test_oo_for_yandex_not_in_vpn(self):
        resp = self.json_ok('office_action_check', {'uid': self.uid})
        assert resp['office_online_editor_type'] == 'only_office'

    @only_office_settings_mock
    def test_oo_for_yandex_in_vpn(self):
        resp = self.json_ok('office_action_check', {'uid': self.uid, 'is_yandex_nets': 1})
        assert resp['office_online_editor_type'] == 'only_office'

    def test_ms_for_pdd_in_list(self):
        with mock.patch('mpfs.core.office.logic.microsoft.FEATURE_TOGGLES_CHECK_OFFICE_BY_DOMAIN_ENABLED', new=True), \
             mock.patch('mpfs.core.services.passport_service.passport.get_all_domain_list', return_value='example.com'), \
             mock.patch('mpfs.core.user.common.was_editor_used', return_value=True), \
             mock.patch('mpfs.core.user.common.CommonUser.is_pdd'), \
             mock.patch('mpfs.core.user.common.CommonUser.get_pdd_domain', return_value='example.com'):
            resp = self.json_ok('office_action_check', {'uid': self.uid})
            assert resp['office_online_editor_type'] == 'microsoft_online'

    def test_no_office_for_b2b(self):
        with mock.patch('mpfs.core.office.logic.microsoft.FEATURE_TOGGLES_CHECK_OFFICE_BY_DOMAIN_ENABLED', new=True), \
             mock.patch('mpfs.core.office.util.FEATURE_TOGGLES_ONLYOFFICE_EDITOR_FOR_USERS_WITHOUT_EDITOR_ENABLED', False), \
             mock.patch('mpfs.core.user.common.CommonUser.is_b2b'), \
             mock.patch('mpfs.core.user.common.CommonUser.get_online_editor_enabled', return_value=False):
            self.json_error('office_action_check', {'uid': self.uid}, code=codes.OFFICE_IS_NOT_ALLOWED)


class HancomHandlersTestCase(OfficeTestCase):
    """Тестируем работоспособность ручек для ханкома"""
    def setup_method(self, method):
        super(HancomHandlersTestCase, self).setup_method(method)
        self.json_ok('office_enable_hancom', {'uid': self.uid})

    def teardown_method(self, method):
        self.json_ok('office_disable_hancom', {'uid': self.uid})
        super(HancomHandlersTestCase, self).teardown_method(method)

    def test_office_meta(self):
        file_path = '/disk/1.doc'
        self.upload_file(self.uid, file_path)
        with mock.patch('mpfs.core.office.util.FEATURE_TOGGLES_HANCOM_EDITOR_ENABLED', True):
            resp = self.json_ok('info', {'uid': self.uid, 'path': file_path, 'meta': 'office_online_url,office_online_editor_type'})
        assert 'office_online_url' in resp['meta']
        assert 'office_online_editor_type' in resp['meta']
        assert resp['meta']['office_online_editor_type'] == 'hancom'

    def test_office_action_check(self):
        file_path = '/disk/1.doc'
        self.upload_file(self.uid, file_path)
        with mock.patch('mpfs.core.office.util.FEATURE_TOGGLES_HANCOM_EDITOR_ENABLED', True):
            resp = self.json_ok('office_action_check', {'uid': self.uid, 'action': 'edit', 'service_id': 'disk', 'service_file_id': file_path})
        assert 'office_online_url' in resp
        assert 'office_online_editor_type' in resp
        assert resp['office_online_editor_type'] == 'hancom'

    def test_office_action_check_for_public_user(self):
        first_req_params = {
            'uid': 0,
            'action': 'edit',
            'service_id': 'web',
            'service_file_id': 'http://yandex.net/doc.doc',
            'file_name': 'test-file-name.docx',
            'size': 12345,
            'source': 'dv',
        }
        self.json_ok('office_action_check', first_req_params)

    def test_office_action_data(self):
        file_path = '/disk/1.doc'
        self.upload_file(self.uid, file_path)
        with mock.patch('mpfs.core.office.util.FEATURE_TOGGLES_HANCOM_EDITOR_ENABLED', True):
            resp = self._office_action_data_edit(self.uid, file_path, locale='ru', ext='docx', tld='com')
        assert 'action_url' in resp
        assert 'office_online_editor_type' in resp
        # TODO: переделать красивее
        assert '1.doc' in resp['action_url']  # Имя файла должно быть в урле
        assert '.com' in resp['action_url']  # Должны прокидывать переданный tld
        assert resp['office_online_editor_type'] == 'hancom'

    def test_unsupported_extension(self):
        file_path = '/disk/1.jpg'
        self.upload_file(self.uid, file_path)
        resp = self.json_ok('info', {'uid': self.uid, 'path': file_path, 'meta': 'office_online_url,office_online_editor_type'})
        assert 'office_online_url' not in resp['meta']
        assert 'office_online_editor_type' not in resp['meta']

    def test_unsupported_extension_office_action(self):
        file_path = '/disk/1.jpg'
        self.upload_file(self.uid, file_path)
        self.json_error('office_action_check', {'uid': self.uid, 'action': 'edit', 'service_id': 'disk', 'service_file_id': file_path}, code=169)
        self._office_action_data_edit(self.uid, file_path, error_code=169)

    @HancomServiceStub()
    def test_office_hancom_lock(self):
        file_path = '/disk/1.doc'
        self.upload_file(self.uid, file_path)
        info = self.json_ok('info', {'uid': self.uid, 'path': file_path, 'meta': 'file_id'})
        resp = self.json_ok('office_hancom_lock', {'uid': self.uid, 'owner_uid': self.uid, 'file_id': info['meta']['file_id']})
        assert 'oid' in resp
        assert resp['type'] == 'hancom'
        resp = self.json_ok('status', {'uid': self.uid, 'oid': resp['oid']})
        assert resp['status'] == 'DONE'

    @HancomServiceStub()
    def test_office_hancom_unlock(self):
        file_path = '/disk/1.doc'
        self.upload_file(self.uid, file_path)
        info = self.json_ok('info', {'uid': self.uid, 'path': file_path, 'meta': 'file_id'})
        with PendingOperationDisabler(HancomAutosaveOperation):
            oid = self.json_ok('office_hancom_lock', {'uid': self.uid, 'owner_uid': self.uid, 'file_id': info['meta']['file_id']})['oid']
            self.json_ok('office_hancom_unlock', {'uid': self.uid, 'oid': oid})
            # unlock переводит операци в COMPLETED
            resp = self.json_ok('status', {'uid': self.uid, 'oid': oid})
            assert resp['status'] == 'DONE'
        # тут мы не отменяем выполняющую таску и после этой строки операция
        # будет в статусе DONE
        oid = self.json_ok('office_hancom_lock', {'uid': self.uid, 'owner_uid': self.uid, 'file_id': info['meta']['file_id']})['oid']
        # Но на unlock это не влияет
        self.json_ok('office_hancom_unlock', {'uid': self.uid, 'oid': oid})

    @HancomServiceStub()
    def test_office_hancom_store(self):
        file_path = '/disk/1.doc'
        self.upload_file(self.uid, file_path)

        with PendingOperationDisabler(HancomAutosaveOperation):
            info = self.json_ok('info', {'uid': self.uid, 'path': file_path, 'meta': 'file_id'})
            oid = self.json_ok('office_hancom_lock', {'uid': self.uid, 'owner_uid': self.uid, 'file_id': info['meta']['file_id']})['oid']
            resp = self.json_ok('office_hancom_store', {'uid': self.uid, 'oid': oid})
            assert resp['type'] == 'hancom'
            assert 'upload_url' in resp

    @HancomServiceStub(2)
    def test_auto_download(self):
        file_path = '/disk/1.doc'
        self.upload_file(self.uid, file_path)
        from mpfs.core.filesystem.resources.disk import MPFSFile

        status_file_path = 'fixtures/xml/upload-from-service.xml'
        with open(status_file_path) as fix_file:
            status_xml = fix_file.read()
        with mock.patch('mpfs.core.filesystem.resources.disk.MPFSFile.Create', wraps=MPFSFile.Create) as file_create_mock, \
                KladunStub(status_value_paths=(status_file_path,)):
            info = self.json_ok('info', {'uid': self.uid, 'path': file_path, 'meta': 'file_id'})
            file_id = info['meta']['file_id']
            self.json_ok('office_hancom_lock', {'uid': self.uid, 'owner_uid': self.uid, 'file_id': file_id})['oid']
            external_copy_operations = [x for x in operations.find(self.uid, None, {}, None, None, None) if x['type'] == 'external_copy']
            external_copy_operations.sort(key=lambda x: x['data']['hancom']['modification_time'])

            self.service_ok('kladun_callback', {
                'uid': self.uid,
                'oid': external_copy_operations[0]['id'],
                'status_xml': status_xml,
                'type': tags.COMMIT_FILE_UPLOAD,
            })
            # Не должен создаться файл, потому что поставили задачу на скачку более свежей версии
            assert not file_create_mock.called
            assert self.json_ok('info',
                                {'uid': self.uid, 'path': file_path, 'meta': 'file_id'})['meta']['file_id'] == file_id

            self.service_ok('kladun_callback', {
                'uid': self.uid,
                'oid': external_copy_operations[1]['id'],
                'status_xml': status_xml,
                'type': tags.COMMIT_FILE_UPLOAD,
            })
            # Должны создать файл с последней версий
            assert file_create_mock.called
            assert self.json_ok('info',
                                {'uid': self.uid, 'path': file_path, 'meta': 'file_id'})['meta']['file_id'] == file_id
            # Создание последней версии должно снять лок
            assert not fs_locks.find_all(self.uid)

    def test_return_microsoft_office_for_unregistered_users(self):
        mail_service_file_id = '1231234012360143/1.1'
        mail_file_name = 'test-file-name'
        mail_file_ext = 'docx'
        mail_full_file_name = '%s.%s' % (mail_file_name, mail_file_ext)
        mail_file_size = 12345

        source = 'dv'
        service_id = 'mail'
        opts = {
            'uid': '1234',
            'action': 'edit',
            'service_id': service_id,
            'service_file_id': mail_service_file_id,
            'source': source,
            'file_name': mail_full_file_name,
            'size': mail_file_size
        }
        res = self.json_ok('office_action_check', opts)
        assert res['office_online_editor_type'] == 'microsoft_online'


class OfficeMiscTestCase(OfficeTestCase):
    """Класс тестов функций, сопутствующих основным.
    """
    def test_office_generate_online_editor_url(self):
        file_path = '/disk/raccoon-story.docx'
        self.upload_file(self.uid, file_path)

        resp = self.json_ok('office_generate_online_editor_url', {'uid': self.uid,
                                                                  'path': file_path})
        assert 'edit_url' in resp
        assert resp['edit_url'] == 'https://disk.yandex.ru/edit/disk/disk%%2Fraccoon-story.docx?uid=%s' % self.uid

    def test_office_session_context(self):
        """Проверить что добавляется параметр `sc`
        """
        post_message_origin = 'https://disk.qa.yandex.ru'
        resp = self._office_action_data_edit(self.uid, self.FILE_PATH, post_message_origin=post_message_origin)
        session_context = make_session_context(post_message_origin, DEFAULT_LOCALE)
        assert session_context == urlparse.parse_qs(resp['action_url'])['sc'][0]

    def test_office_action_data_edit_mail_post_correct_data_to_kladun_if_converted(self):
        """Проверить что в кладун отправляются корректные данные."""
        # https://st.yandex-team.ru/CHEMODAN-35942
        uid = self.user_1.uid
        self.create_user(uid)

        with open('fixtures/xml/mail_service1.xml') as f:
            mail_service_response = f.read()

        with mock.patch(
            'mpfs.core.services.mail_service.MailStidService.get_mail_stid',
            return_value='12345.6789'
        ):
            with mock.patch(
                'mpfs.core.services.mail_service.Mail.open_url',
                return_value=mail_service_response
            ):
                with mock.patch(
                    'mpfs.core.office.operations.OfficeConvertFile.post_request_to_kladun',
                    wraps=OfficeConvertFile.post_request_to_kladun
                ) as mocked_post_request_to_kladun:
                    mail_mid = '162129586585340230'
                    mail_hid = '1.2'
                    self.json_ok('office_action_data', {
                        'uid': uid,
                        'action': 'edit',
                        'service_id': 'mail',
                        'service_file_id': '%s/%s' % (mail_mid, mail_hid),
                        'post_message_origin': 'https://disk.qa.yandex.ru',
                        'size': '69120',
                        'filename': 'test.xls',
                        'ext': 'xls'  # будет превращено в xlsx
                    })
                    assert mocked_post_request_to_kladun.called
                    args, kwargs = mocked_post_request_to_kladun.call_args
                    (post_data,) = args
                    assert post_data['source-service'] == 'mail2'
                    assert post_data['service-file-id'] == '%s:%s/%s' % (uid, mail_mid, mail_hid)

    def test_office_action_data_edit_mail_post_correct_data_to_kladun_if_not_converted(self):
        """Проверить что в кладун отправляются корректные данные."""
        # https://st.yandex-team.ru/CHEMODAN-35942
        uid = self.user_1.uid
        self.create_user(uid)

        with open('fixtures/xml/mail_service1.xml') as f:
            mail_service_response = f.read()

        with mock.patch(
            'mpfs.core.services.mail_service.MailStidService.get_mail_stid',
            return_value='12345.6789'
        ):
            with mock.patch(
                'mpfs.core.services.mail_service.Mail.open_url',
                return_value=mail_service_response
            ):
                with mock.patch(
                    'mpfs.core.office.operations.CopyMailAttachmentToDisk.post_request_to_kladun',
                    wraps=CopyMailAttachmentToDisk.post_request_to_kladun
                ) as mocked_post_request_to_kladun:
                    mail_mid = '162129586585340230'
                    mail_hid = '1.2'
                    self.json_ok('office_action_data', {
                        'uid': uid,
                        'action': 'edit',
                        'service_id': 'mail',
                        'service_file_id': '%s/%s' % (mail_mid, mail_hid),
                        'post_message_origin': 'https://disk.qa.yandex.ru',
                        'size': '69120',
                        'filename': 'test.xlsx',
                        'ext': 'xlsx'
                    })
                    assert mocked_post_request_to_kladun.called
                    args, kwargs = mocked_post_request_to_kladun.call_args
                    (post_data,) = args
                    assert post_data['source-service'] == 'mail2'
                    assert post_data['service-file-id'] == '%s:%s/%s' % (uid, mail_mid, mail_hid)

    def test_discovery_cached(self):
        """Протестировать, что между последовательными запросами
        на обновление, кеш discovery запрашивается единожды, т.е. кешируется.
        """

        with open('fixtures/xml/discovery.xml') as fd:
            xml = fd.read()

        with patch.dict(cache._caches, {}, clear=True):
            with patch.dict(DiscoveryService._discovery, {}, clear=True):
                with patch.object(DiscoveryService, 'open_url', return_value=xml) as mocked_open_url:
                    DiscoveryService().ensure_cache()
                    DiscoveryService().ensure_cache()
                    assert mocked_open_url.call_count == 1

    def test_office_download_redirect(self):
        info = self.json_ok('info', {
            'uid': self.uid,
            'path': '%s:%s' % (self.uid, self.FILE_PATH),
            'meta': 'file_id,file_url'
        })
        file_id = info['meta']['file_id']
        resource_id = make_resource_id(self.uid, file_id)
        access_token = office.auth.generate_access_token(self.uid, resource_id)
        self.json_ok('office_download_redirect', {
            'resource_id': resource_id,
            'access_token': access_token,
        })

        assert self.response.status == 302
        assert not {'Location', 'Content-Type', 'Content-Disposition'} - self.response.headers.viewkeys()
        assert self.response.headers['Location'].startswith('http')

    def test_discovery_templates_resolving(self):
        url = ('https://word-view.officeapps-df.live.com/wv/wordviewerframe.aspx?'
               'new=1&<ui=UI_LLCC&><rs=DC_LLCC&><dchat=DISABLE_CHAT&><showpagestats=PERFSTATS&>')
        url = DiscoveryService().resolve_qs_templates(url, {'ui': 'ru-RU', 'rs': 'ru-RU'})
        assert '<' not in url and '&>' not in url
        query = urlparse.urlparse(url).query
        assert not {'new', 'ui', 'rs'} ^ urlparse.parse_qs(query).viewkeys()

    def test_wopitest_view(self):
        response = self.json_ok('office_action_data', {
            'uid': self.uid, 'action': 'view', 'path': self.WOPITEST_FILE_PATH
        })
        assert not {'action_url', 'access_token',
                    'access_token_ttl', 'resource_url',
                    'resource_id', 'office_online_editor_type'} ^ response.viewkeys()

    def test_find_available_name_in_shared_folder(self):
        """Протестировать поиск подходящего имени файла в ОП,
        имя которой отличается у гостя и владельца.
        """
        guest_uid = user_1.uid
        self.create_user(guest_uid)

        shared_dir = '/disk/shared-folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': shared_dir})

        group = self.json_ok('share_create_group', {'uid': self.uid, 'path': shared_dir})
        invite_hash = self.share_invite(group['gid'], guest_uid, rights=660)
        self.json_ok('share_activate_invite', {'uid': guest_uid, 'hash': invite_hash})

        self.json_ok('move', {'uid': self.uid, 'src': shared_dir, 'dst': shared_dir + '_new'})

        p1 = office.actions._make_file_path('docx', shared_dir, guest_uid, 'ru')
        self.upload_file(guest_uid, p1)

        p2 = office.actions._make_file_path('docx', shared_dir, guest_uid, 'ru')
        assert p1 != p2

    def test_file_converted_notification(self):
        """Протестировать push уведомление после конвертации файла
        """
        old_format_file = '/disk/test.doc'
        self.upload_file(self.uid, old_format_file)
        with patch.object(OfficeFileConvertedEvent, 'send', return_value=None) as mocked:
            self._office_action_data(old_format_file, False, False, file_size=1024)
            assert mocked.call_count == 1

    def test_convert_disk_full_error(self):
        """Тестируем ситуацию, когда у пользователя не хватает места для файла.

        Кейс с попыткой конвертировать файл и таким образом положить в диск.
        Проверяем, что в статусе для верстки вернется ошибка NO_FREE_SPACE
        """
        file_size = 10

        filepath = '/disk/file-for-convert.xls'
        self.upload_file(self.uid, filepath, file_data={'size': file_size})
        resp = self.json_ok('info', {'uid': self.uid, 'path': filepath, 'meta': 'md5,sha256'})
        file_md5 = resp['meta']['md5']
        file_sha256 = resp['meta']['sha256']
        new_file_size = file_size  # размер нового файла (сконвертированного) будет такой же

        with patch('mpfs.core.filesystem.quota.Quota.free_with_shared_support', return_value=file_size - 5):
            oid = self._office_action_data(
                path=filepath,
                hardlink=False,
                size_constraint_failed=True,
                file_md5=file_md5,
                file_sha256=file_sha256,
                file_size=new_file_size
            )

        resp = self.json_ok('status', {'uid': self.uid, 'oid': oid})
        assert resp['error']['code'] == codes.NO_FREE_SPACE

    def test_size_failed(self):
        file_size = 10

        filepath = '/disk/file-for-convert.xls'
        self.upload_file(self.uid, filepath, file_data={'size': file_size})
        resp = self.json_ok('info', {'uid': self.uid, 'path': filepath, 'meta': 'md5,sha256'})
        file_md5 = resp['meta']['md5']
        file_sha256 = resp['meta']['sha256']
        new_file_size = file_size  # размер нового файла (сконвертированного) будет больше выходного ограничения

        with patch.dict(OFFICE_SIZE_LIMITS, {'Excel': {'edit': 1}}):
            oid = self._office_action_data(
                path=filepath,
                hardlink=False,
                size_constraint_failed=True,
                file_md5=file_md5,
                file_sha256=file_sha256,
                file_size=new_file_size
            )

        resp = self.json_ok('status', {'uid': self.uid, 'oid': oid})
        assert resp['error']['code'] == codes.OFFICE_FILE_TOO_LARGE

    def test_size_limits_constraints(self):
        # Тестируем такое поведение. Для старых файлов (doc, xls, ppt) мы проверяем ограничения по размеру входных на
        # конвертацию файлов. Для новых - по таблице размеров от Офиса (т.к. мы их сразу открываем офисом,
        # а не конвертируем)

        file_size = 12345
        old_file_path = '/disk/old.doc'
        new_file_path = '/disk/old.docx'
        self.upload_file(self.uid, old_file_path, file_data={'size': file_size})
        self.upload_file(self.uid, new_file_path, file_data={'size': file_size})

        with patch.dict(OFFICE_CONVERT_SIZE_LIMITS, {'doc': file_size - 1}):
            self.json_error('office_action_check', {
                    'uid': self.uid,
                    'action': 'edit',
                    'service_id': 'disk',
                    'service_file_id': old_file_path
                },
                code=codes.OFFICE_FILE_TOO_LARGE
            )

            resp = self.json_ok('info', {
                'uid': self.uid,
                'path': old_file_path,
                'meta': ''
            })
            assert 'office_online_url' not in resp['meta']

        with patch.dict(OFFICE_CONVERT_SIZE_LIMITS, {'doc': file_size + 1}):
            resp = self.json_ok('office_action_check',
                                {
                                    'uid': self.uid,
                                    'action': 'edit',
                                    'service_id': 'disk',
                                    'service_file_id': old_file_path
                                })
            assert 'office_online_url' in resp

            resp = self.json_ok('info', {
                'uid': self.uid,
                'path': old_file_path,
                'meta': ''
            })
            assert 'office_online_url' in resp['meta']

        with patch.dict(OFFICE_SIZE_LIMITS, {'Word': {'edit': file_size - 1}}):
            resp = self.json_error('office_action_check',
                                   {
                                       'uid': self.uid,
                                       'action': 'edit',
                                       'service_id': 'disk',
                                       'service_file_id': new_file_path
                                   },
                                   code=codes.OFFICE_FILE_TOO_LARGE)

            resp = self.json_ok('info', {
                'uid': self.uid,
                'path': new_file_path,
                'meta': ''
            })
            assert 'office_online_url' not in resp['meta']

        with patch.dict(OFFICE_SIZE_LIMITS, {'Word': {'edit': file_size + 1}}):
            resp = self.json_ok('office_action_check',
                                {
                                    'uid': self.uid,
                                    'action': 'edit',
                                    'service_id': 'disk',
                                    'service_file_id': new_file_path
                                })
            assert 'office_online_url' in resp

            resp = self.json_ok('info', {
                'uid': self.uid,
                'path': new_file_path,
                'meta': ''
            })
            assert 'office_online_url' in resp['meta']


class OfficeEditNegativeTestCase(OfficeTestCase):
    def setup_method(self, method):
        super(OfficeEditNegativeTestCase, self).setup_method(method)
        old_size = 2 * KB
        self.new_size = 4 * KB
        shared_dir = '/disk/docs'
        self.file_path = '%s/test.docx' % shared_dir
        self.json_ok('mkdir', {'uid': self.uid, 'path': shared_dir})
        self.upload_file(self.uid, self.file_path, file_data={'size': old_size})
        group = self.json_ok('share_create_group', {'uid': self.uid, 'path': shared_dir})
        self.share_invite(group['gid'], user_1.uid, rights=660)
        file_id = self.json_ok('info', {'uid': self.uid,
                                        'path': '%s:%s' % (self.uid, self.file_path),
                                        'meta': ','})['meta']['file_id']
        self.office_lock_id = '123'
        with patch.object(OfficeLockingOperationStub, '_process'):
            self.json_ok('office_lock', {'uid': self.uid,
                                         'owner_uid': self.uid,
                                         'file_id': file_id,
                                         'office_lock_id': self.office_lock_id})
        self.resource_id = make_resource_id(self.uid, file_id)
        self.access_token = office.auth.generate_access_token(self.uid, self.resource_id)

    @staticmethod
    def error_on_queue_put_update_last_files_cache(*args, **kwargs):
        # Пропускаем стэки с обработкой мока (2 шт.) и берем предыдущий перед попыткой положить задачу в очередь
        caller_name = inspect.stack()[3][3]
        if caller_name == 'update_for_group_async':
            raise socket.error("timed out or connection reffused")

    def test_update_file_when_network_problems_on_first_last_files_update(self):
        """Проверяем сможем ли обновить файл.

        Когда есть сетевые проблемы и не получится поставить асинхронный таск, которое выполняется между
        удалением файла и созданием нового.
        """
        response = self.json_ok('office_store',
                                {'resource_id': self.resource_id,
                                 'access_token': self.access_token},
                                json={'headers': {'X-Wopi-Lock': self.office_lock_id}})
        assert 'oid' in response['store_info']
        oid = response['store_info']['oid']
        with patch('mpfs.core.queue.mpfs_queue.put',
                   side_effect=self.error_on_queue_put_update_last_files_cache) as mocked_update, \
                 patch.dict('mpfs.config.settings.feature_toggles', {'suppress_exception_block_enabled': True}):
            body_1, body_2, body_3 = self._get_kladun_callback_bodies(self.file_path, size=self.new_size)
            for callback_type, body in (
                    (tags.COMMIT_FILE_INFO, body_1),
                    (tags.COMMIT_FILE_UPLOAD, body_2),
                    (tags.COMMIT_FINAL, body_3)):
                with KladunStub(status_values=(body,)):
                    self.service_ok('kladun_callback', {
                        'uid': self.uid,
                        'oid': oid,
                        'status_xml': etree.tostring(body),
                        'type': callback_type
                    })

            assert mocked_update.call_count >= 1, u'Обновление кэша Последних Файлов не было вызвано'
        # Файл должен быть на месте
        file_info = self.json_ok('info', {'uid': self.uid, 'path': self.file_path, 'meta': 'size,'})
        # С новым размером
        assert file_info['meta']['size'] == self.new_size


class OfficeEditTestCase(OfficeTestCase):
    """Класс тестов различных кейсов для редактирования файла
    """

    def test_editnew(self):
        test_file = '/disk/office/Таблица.xlsx'
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/office/'})
        self.upload_file(self.uid, test_file)

        real_get_resource = factory.get_resource

        def fake_get_resource(uid, address, *args, **kwargs):
            if not isinstance(address, Address):
                try:
                    address = Address(address)
                except errors.AddressError:
                    address = Address.Make(uid, address)

            if address.is_folder:
                return real_get_resource(uid, address, *args, **kwargs)

            class FakeResource(object):
                meta = {
                    'file_id': 'd599205e3dc7f699a00de8aa664572a40ddd24283cd3115621d1cfe12be48b1e'
                }
                id = test_file
                visible_address = Address.Make(self.uid, test_file)
                address = visible_address
                size = 10
                is_shared = False
                owner_uid = self.uid
                office_access_state = None
                resource_id = ResourceId(self.uid, 'd599205e3dc7f699a00de8aa664572a40ddd24283cd3115621d1cfe12be48b1e')
                def get_short_url(self): return

            return FakeResource()

        def fake_store(*args, **kwargs):
            return {'status': 'hardlinked'}

        # тестируем нормальный сценарий
        with patch.object(Filesystem, 'store', fake_store):
            with patch.object(factory, 'get_resource', fake_get_resource):
                resp = self.json_ok('office_action_data', {'uid': self.uid, 'action': 'editnew',
                                                           'path': '/disk/office/',
                                                           'ext': 'xlsx'})

                assert 'action_url' in resp
                assert resp['office_online_editor_type'] == 'microsoft_online'

        # тестируем невалидный path - существующий файл
        incorrect_path = '/disk/123321'
        self.upload_file(self.uid, incorrect_path)

        with patch.object(Filesystem, 'store', fake_store):
            with patch.object(factory, 'get_resource', fake_get_resource):
                self.json_error('office_action_data', {
                                    'uid': self.uid, 'action': 'editnew',
                                    'path': incorrect_path,
                                    'ext': 'xlsx'
                                }, code=codes.NOT_FOLDER)

                self.json_error('office_action_data', {
                                    'uid': self.uid, 'action': 'editnew',
                                    'path': '/disk/folder-which-does-not-exist/',
                                    'ext': 'xlsx'
                                }, code=codes.FOLDER_NOT_FOUND)

        # тестируем передачу service_id и service_file_id без path
        with patch.object(Filesystem, 'store', fake_store):
            with patch.object(factory, 'get_resource', fake_get_resource):
                resp = self.json_ok('office_action_data', {'uid': self.uid, 'action': 'editnew',
                                                           'service_id': 'disk',
                                                           'service_file_id': '/disk/office/',
                                                           'ext': 'xlsx'})

                assert 'action_url' in resp
                assert resp['office_online_editor_type'] == 'microsoft_online'

        # тестируем ошибку, если не указать path и service_file_id
        with patch.object(Filesystem, 'store', fake_store):
            with patch.object(factory, 'get_resource', fake_get_resource):
                self.json_error('office_action_data', {'uid': self.uid, 'action': 'editnew',
                                                       'service_id': 'disk',
                                                       'ext': 'xlsx'},
                                code=codes.OFFICE_SERVICE_FILE_ID_NOT_SPECIFIED)

        # тестируем ошибку, если не указать service_id не disk
        with patch.object(Filesystem, 'store', fake_store):
            with patch.object(factory, 'get_resource', fake_get_resource):
                self.json_error('office_action_data', {'uid': self.uid, 'action': 'editnew',
                                                       'service_id': 'mail',
                                                       'service_file_id': '/disk/office/',
                                                       'ext': 'xlsx'},
                                code=codes.OFFICE_STORAGE_NOT_SUPPORTED)

    def test_editnew_with_custom_name(self):
        ext = 'xlsx'
        file_name = u'Отчет'
        dir_path = '/disk/docs'
        self.json_ok('mkdir', opts={'uid': self.uid, 'path': dir_path})
        resp = self.json_ok('office_action_data', {'uid': self.uid,
                                                   'action': 'editnew',
                                                   'service_id': 'disk',
                                                   'service_file_id': dir_path,
                                                   'ext': ext,
                                                   'filename': file_name})
        assert resp['resource_path'] == os.path.join(dir_path, '.'.join([file_name, ext]))

    def test_editnew_with_custom_name_bad_path(self):
        ext = 'xlsx'
        file_name = u'1' * settings.system['system']['limits']['max_name_length']
        dir_path = '/disk/docs'
        self.json_ok('mkdir', opts={'uid': self.uid, 'path': dir_path})
        self.json_error('office_action_data', {'uid': self.uid,
                                               'action': 'editnew',
                                               'service_id': 'disk',
                                               'service_file_id': dir_path,
                                               'ext': ext,
                                               'filename': file_name},
                        status=400)

    @parameterized.expand([
        ('docx', MicrosoftEditor.text_type),
        ('xlsx', MicrosoftEditor.spreadsheet_type),
        ('pptx', MicrosoftEditor.presentation_type),
    ])
    def test_edit(self, ext, expected_doc_type):
        # Проверяем ответ ручки, если конвертация не требуется
        file_path = '/disk/homyak.%s' % ext
        self.upload_file(self.uid, file_path, file_data={'size': 100})

        resp = self._office_action_data_edit(self.uid, file_path)
        assert 'action_url' in resp
        assert resp['office_online_editor_type'] == 'microsoft_online'
        assert_that(resp, has_entries({'editor_config': has_entries({'documentType': expected_doc_type})}))

    def test_edit_shared_old_format(self):
        """Протестировать редактирование файла старого формата из ОП гостем
        """
        other_uid = user_1.uid
        self.create_user(other_uid)
        shared_dir = '/disk/Shared'
        self.json_ok('mkdir', opts={'uid': self.uid, 'path': shared_dir})
        group = self.json_ok('share_create_group', {'uid': self.uid, 'path': shared_dir})
        invite_hash = self.share_invite(group['gid'], other_uid, rights=660)
        self.json_ok('share_activate_invite', {'uid': other_uid, 'hash': invite_hash})

        shared_file = '/disk/Shared/test.doc'
        self.upload_file(self.uid, shared_file)

        resp = self.json_ok('office_action_data',
                            {'uid': other_uid, 'action': 'edit', 'path': shared_file})
        assert 'oid' in resp

    @mock.patch('mpfs.core.office.logic.only_office.OFFICE_ONLY_OFFICE_ENABLED', True)
    def test_edit_shared(self):
        owner_uid = user_1.uid
        self.create_user(owner_uid)
        self.json_ok('office_set_selection_strategy', {'uid': owner_uid,
                                                       'selection_strategy': 'force_oo'})
        shared_dir = '/disk/Shared'
        self.json_ok('mkdir', opts={'uid': owner_uid, 'path': shared_dir})
        group = self.json_ok('share_create_group', {'uid': owner_uid, 'path': shared_dir})
        invite_hash = self.share_invite(group['gid'], self.uid, rights=660)
        self.json_ok('share_activate_invite', {'uid': self.uid, 'hash': invite_hash})

        shared_file = '/disk/Shared/test.docx'
        self.upload_file(owner_uid, shared_file)

        resp = self.json_ok('office_action_check', {'uid': self.uid})
        assert resp['office_online_editor_type'] == MicrosoftEditor.type_label, u'У Гостя должен быть MSO'

        resp = self.json_ok('office_action_data', {'uid': self.uid,
                                                   'action': 'edit',
                                                   'path': shared_file})
        assert resp['office_online_editor_type'] == OnlyOfficeEditor.type_label, u'У Владельца должен быть OO'

    @mock.patch('mpfs.core.office.logic.only_office.OFFICE_ONLY_OFFICE_ENABLED', True)
    def test_edit_shared_when_ro_rights(self):
        owner_uid = user_1.uid
        self.create_user(owner_uid)
        self.json_ok('office_set_selection_strategy', {'uid': owner_uid,
                                                       'selection_strategy': 'force_oo'})
        shared_dir = '/disk/Shared'
        self.json_ok('mkdir', opts={'uid': owner_uid, 'path': shared_dir})
        group = self.json_ok('share_create_group', {'uid': owner_uid, 'path': shared_dir})
        invite_hash = self.share_invite(group['gid'], self.uid, rights=SharedFolderRights.READ_ONLY_INT)
        self.json_ok('share_activate_invite', {'uid': self.uid, 'hash': invite_hash})

        shared_file = '/disk/Shared/test.docx'
        self.upload_file(owner_uid, shared_file)

        self.json_error('office_action_data', {'uid': self.uid,
                                               'action': 'edit',
                                               'path': shared_file},
                        code=OfficeFileIsReadOnlyError.code)

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Postgres test')
    def test_edit_with_convert(self):
        file_size = 5242879  # размер файла меньше ограничения
        with patch.dict(OFFICE_SIZE_LIMITS, {'Excel': {'edit': 5242880}}):
            filepath1 = '/disk/file-for-convert.xls'
            self.upload_file(self.uid, filepath1, file_data={'size': file_size})
            resp = self.json_ok('info', {'uid': self.uid, 'path': filepath1, 'meta': 'md5,sha256'})
            file_md5 = resp['meta']['md5']
            file_sha256 = resp['meta']['sha256']
            new_file_size = file_size + 100500  # размер нового файла (сконвертированного) будет больше ограничения

            # зальем файл, чтобы новый файл схардлинкался с этим
            self.upload_file(self.uid,
                             '/disk/file-to-hardlink-with.xlsx',
                             file_data={
                                'md5': file_md5,
                                'sha256': file_sha256,
                                'size': new_file_size
                             })

            # проверим, что при хардлинке и если размер больше, чем ограничение - вернется ошибка kladun conflict
            self._office_action_data(
                path=filepath1,
                hardlink=True,
                size_constraint_failed=True,
                file_md5=file_md5,
                file_sha256=file_sha256,
                file_size=new_file_size
            )

            # проверим, что при хардлинке и если размер меньше, чем ограничение - все будет ок и вернется "ошибка"
            # Kladun hardlink found
            self._office_action_data(
                path=filepath1,
                hardlink=True,
                size_constraint_failed=False,
                file_md5=file_md5,
                file_sha256=file_sha256,
                file_size=file_size
            )

            # проверим, что без хардлинка и если размер больше, чем ограничение - вернется ошибка kladun conflict
            self._office_action_data(
                path=filepath1,
                hardlink=False,
                size_constraint_failed=True,
                file_size=new_file_size
            )

            # проверим, что без хардлинка и если размер меньше, чем ограничение - все будет ок
            self._office_action_data(
                path=filepath1,
                hardlink=False,
                size_constraint_failed=False,
                file_size=file_size
            )

    def test_edit_attach(self):
        kladun_status_file = 'fixtures/xml/upload-from-service.xml'

        class FakeMailService(MailStidService):
            def open_url(self, *args, **kwargs):
                content = open('fixtures/json/meta-mail.json').read()
                return content

        # тестируем создание операции перекладывания без конвертации
        with patch.object(office.actions, 'mail_stid_service', FakeMailService()):
            opts = {
                'uid': self.uid,
                'action': 'edit',
                'service_id': 'mail',
                'service_file_id': '2280000140035024981/1.1',
                'filename': 'new_file_name',
                'ext': 'docx',
                'size': '123123',
            }

            response = self.json_ok('office_action_data', opts)

            assert 'oid' in response
            oid = response['oid']

            with open(kladun_status_file) as fix_file:
                status_xml = fix_file.read()

            with KladunStub(status_value_paths=(kladun_status_file,)):
                self.service('kladun_callback', {'oid': oid, 'uid': self.uid, 'status_xml': status_xml})

        # тестируем создание операции конвертации
        with patch.object(office.actions, 'mail_stid_service', FakeMailService()):
            opts = {
                'uid': self.uid,
                'action': 'edit',
                'service_id': 'mail',
                'service_file_id': '2280000140035024981/1.1',
                'filename': 'new_file_name',
                'ext': 'doc',
                'size': '123123',
            }

            self._office_action_data(path='',
                                     hardlink=False,
                                     size_constraint_failed=False,
                                     opts=opts)

        # тестируем ситуацию, когда письмо с таким id не было найдено
        class FakeMailService(MailStidService):
            def open_url(self, *args, **kwargs):
                return to_json({"envelopes": []})

        with patch.object(office.actions, 'mail_stid_service', FakeMailService()):
            opts = {
                'uid': self.uid,
                'action': 'edit',
                'service_id': 'mail',
                'service_file_id': '2280000140035024981/1.1',
                'filename': 'new_file_name',
                'ext': 'docx',
                'size': '123123',
            }

            self.json_error('office_action_data', opts, code=codes.OFFICE_MAIL_ATTACHMENT_NOT_FOUND)

    def test_edit_public(self):
        file_path = '/disk/old.docx'
        self.upload_file(self.uid, file_path)
        file_public_hash = self.json_ok('set_public', {'uid': self.uid, 'path': file_path})['hash']

        response = self._office_action_data_edit(self.uid, file_public_hash, service_id='public')
        oid = response['oid']
        response = self.json_ok('status', {'oid': oid, 'uid': self.uid})
        assert '%s:/disk/Загрузки/old.docx' % self.uid == response['params']['target'].encode('utf-8')

    def test_edit_public_from_attach(self):
        """Протестировать редактирование файла через DV, который был загружен в аттач."""
        self.upload_file(self.uid, '/attach/old.docx')
        attach_list = self.json_ok('list', {'uid': self.uid, 'path': '/attach', 'meta': ''})
        public_hash = None
        path = None
        for resource in attach_list:
            if resource['name'] != 'old.docx':
                continue
            public_hash = resource['meta']['public_hash']
            path = resource['path']
            break

        assert path
        assert public_hash

        response = self._office_action_data_edit(self.uid, public_hash, service_id='public')
        oid = response['oid']
        response = self.json_ok('status', {'oid': oid, 'uid': self.uid})
        assert '%s:/disk/Загрузки/old.docx' % self.uid == response['params']['target'].encode('utf-8')

    @patch.object(microsoft, 'OFFICE_DISABLE_FOR_B2B', new=True)
    @mock.patch('mpfs.core.office.util.FEATURE_TOGGLES_ONLYOFFICE_EDITOR_FOR_USERS_WITHOUT_EDITOR_ENABLED', False)
    def test_edit_public_for_b2b_not_allowed(self):
        self.json_ok('user_make_b2b', {'uid': self.uid, 'b2b_key': 'true'})

        file_path = '/disk/old.docx'
        self.upload_file(self.uid, file_path)
        file_public_hash = self.json_ok('set_public', {'uid': self.uid, 'path': file_path})['hash']

        response = self._office_action_data_edit(self.uid, file_public_hash, service_id='public',
                                                error_code=codes.OFFICE_IS_NOT_ALLOWED)

    def test_edit_public_folder_file_not_public(self):
        """Протестировать редактирование непубличного файла в публичной папке.
        """
        dir_path = '/disk/dir'
        file_path = '%s/old.docx' % dir_path

        self.json_ok('mkdir', {'uid': self.uid, 'path': dir_path})
        self.upload_file(self.uid, file_path)
        dir_public_hash = self.json_ok('set_public', {'uid': self.uid, 'path': dir_path})['hash']

        file_public_path = '%s:/%s' % (dir_public_hash, file_path.split('/')[-1])
        resp = self._office_action_data_edit(self.uid, file_public_path, service_id='public')
        oid = resp['oid']
        response = self.json_ok('status', {'oid': oid, 'uid': self.uid})
        assert '%s:/disk/Загрузки/old.docx' % self.uid == response['params']['target'].encode('utf-8')

    def test_edit_public_with_convert(self):
        """Протестировать редактирование публичного ресурса старого формата.

        Проверяются так же аргументы запроса в Кладун.
        """
        file_path = '/disk/old.doc'
        self.upload_file(self.uid, file_path)
        file_public_hash = self.json_ok('set_public', {'uid': self.uid, 'path': file_path})['hash']

        stid = self.json_ok('info', {'uid': self.uid, 'path': file_path, 'meta': 'file_mid'})['meta']['file_mid']

        with KladunStub() as kladun_mocks:
            office_action = self._office_action_data_edit(self.uid, file_public_hash, service_id='public')
            post_data = kladun_mocks.office_post_request.call_args[0][0]

            assert post_data['uid'] == self.uid
            assert post_data['source-service'] == 'mulca'
            assert post_data['service-file-id'] == '%s:%s' % (self.uid, stid)
            assert post_data['path'] == u'%s:/disk/Загрузки/old.docx' % self.uid

            status = self.json_ok('status', {'oid': office_action['oid'], 'uid': self.uid})
            assert u'%s:/disk/Загрузки/old.docx' % self.uid == status['params']['path']

    def test_edit_public_shared_file(self):
        """Протестировать редактирование публичного файла в расшаренной папке гостем.
        """
        file_name = 'Document.docx'
        file_folder = '/disk/shared'
        file_path = '%s/%s' % (file_folder, file_name)

        self.json_ok('mkdir', {'uid': self.OTHER_UID, 'path': file_folder})
        self.upload_file(self.OTHER_UID, file_path)
        group = self.json_ok('share_create_group', {'uid': self.OTHER_UID, 'path': file_folder})
        gid = group['gid']
        invite_hash = self.share_invite(gid, self.uid, rights=660)
        self.json_ok('share_activate_invite', {'uid': self.uid, 'hash': invite_hash})

        file_public_hash = self.json_ok('set_public', {'uid': self.uid, 'path': file_path})['hash']

        response = self._office_action_data_edit(self.uid, file_public_hash, service_id='public')
        oid = response['oid']
        response = self.json_ok('status', {'oid': oid, 'uid': self.uid})
        assert '%s:/disk/Загрузки/%s' % (self.uid, file_name) == response['params']['target'].encode('utf-8')

    def test_edit_web(self):
        """
        Тестируем редактирование с СЕРП через DV

        Шаги:
            1. Проверяем, что файл редактируемый office_action_check
            2. Создаем операцию на выкачку файла через office_action_data
            3. Создаем операцию на выкачку файла с одновременной конвертацией через office_action_data
        """
        # Шаг 1. Бэкенд DV идет в MPFS
        first_req_params = {
            'uid': self.uid,
            'action': 'edit',
            'service_id': 'web',
            'service_file_id': 'http://yandex.net/doc.doc',
            'file_name': 'test-file-name.docx',
            'size': 12345,
            'source': 'dv',
        }
        resp = self.json_ok('office_action_check', first_req_params)

        assert 'office_online_url' in resp
        parsed_url = urlparse.urlparse(resp['office_online_url'])
        action, service_id, service_file_id = parsed_url.path.strip('/').split('/', 3)
        assert action == first_req_params['action']
        assert service_id == first_req_params['service_id']
        qs_params = urlparse.parse_qs(parsed_url.query)
        assert qs_params['size'][0] == str(first_req_params['size'])
        assert qs_params['source'][0] == first_req_params['source']
        assert qs_params['filename'][0] == 'test-file-name'
        assert qs_params['ext'][0] == 'docx'

        # Шаг 2. Моделируем поведение верстки и ее поход в office_action_data без конвертации
        second_req_params = {
            'uid': self.uid,
            'action': action,
            'service_id': service_id,
            'service_file_id': service_file_id,
        }
        for name, values in qs_params.iteritems():
            second_req_params[name] = values[0]

        with patch.object(office.actions.EditAction, '_create_external_copy_operation', return_value={'oid': '1234'}) as mock_obj:
            self.json_ok('office_action_data', second_req_params)
            _, args, _ = mock_obj.mock_calls[0]
            assert args[0] == self.uid
            assert args[1] == first_req_params['service_file_id']
            assert args[2] == u'%s:/disk/Загрузки/%s' % (self.uid, first_req_params['file_name'])

        # Шаг 3. С конвертацией
        third_req_params = second_req_params.copy()
        third_req_params['ext'] = 'doc'

        with patch.object(OfficeConvertFile, 'post_request_to_kladun') as mock_obj:
            self.json_ok('office_action_data', third_req_params)
            _, args, _ = mock_obj.mock_calls[0]
            kladun_data = args[0]
            assert kladun_data['uid'] == self.uid
            assert kladun_data['source-service'] == 'web'
            assert kladun_data['service-file-url'] == first_req_params['service_file_id']
            assert kladun_data['path'] == u'%s:/disk/Загрузки/test-file-name.docx' % self.uid

    def test_edit_browser(self):
        """
        Тестируем редактирование файлов Я.Браузера через DV

        Шаги:
            1. Проверяем, что файл редактируемый office_action_check
            2. Создаем операцию на выкачку файла через office_action_data
            3. Создаем операцию на выкачку файла с одновременной конвертацией через office_action_data
        """
        # Шаг 1. Бэкенд DV идет в MPFS
        first_req_params = {
            'uid': self.uid,
            'action': 'edit',
            'service_id': 'browser',
            'service_file_id': 'mds/key',
            'file_name': 'test-file-name.docx',
            'size': 12345,
            'source': 'dv',
        }
        resp = self.json_ok('office_action_check', first_req_params, headers={'cookie': 'yandexuid=12345'})

        assert 'office_online_url' in resp
        assert resp['office_online_editor_type'] == 'microsoft_online'
        parsed_url = urlparse.urlparse(resp['office_online_url'])
        action, service_id, service_file_id = parsed_url.path.strip('/').split('/', 3)
        assert action == first_req_params['action']
        assert service_id == first_req_params['service_id']
        qs_params = urlparse.parse_qs(parsed_url.query)
        assert qs_params['size'][0] == str(first_req_params['size'])
        assert qs_params['source'][0] == first_req_params['source']
        assert qs_params['filename'][0] == 'test-file-name'
        assert qs_params['ext'][0] == 'docx'

        # Шаг 2. Моделируем поведение верстки и ее поход в office_action_data без конвертации
        service_file_id = urllib2.unquote(service_file_id)
        (mds_key, signature) = DocviewerYabrowserUrlService().parse_yabrowser_url(service_file_id)
        second_req_params = {
            'uid': self.uid,
            'action': action,
            'service_id': service_id,
            'service_file_id': mds_key,
        }
        for name, values in qs_params.iteritems():
            second_req_params[name] = values[0]

        with patch.object(office.actions.EditAction, '_create_browser_to_disk_operation', return_value={'oid': '1234'}) as mock_obj:
            self.json_ok('office_action_data', second_req_params)
            _, args, _ = mock_obj.mock_calls[0]
            assert args[0] == self.uid
            assert args[1] == mds_key
            assert args[2].id == u'%s:/disk/Загрузки/%s' % (self.uid, first_req_params['file_name'])

        # Шаг 3. С конвертацией
        third_req_params = second_req_params.copy()
        third_req_params['ext'] = 'doc'

        with patch.object(OfficeConvertFile, 'post_request_to_kladun') as mock_obj:
            self.json_ok('office_action_data', third_req_params)
            _, args, _ = mock_obj.mock_calls[0]
            kladun_data = args[0]
            assert kladun_data['uid'] == self.uid
            assert kladun_data['source-service'] == 'browser'
            assert kladun_data['service-file-id'] == mds_key
            assert kladun_data['path'] == u'%s:/disk/Загрузки/test-file-name.docx' % self.uid


class OfficeLockTestCase(OfficeTestCase):
    """Класс тестов ручки `office_lock`
    """
    @pytest.mark.skipif(True, reason="test hangs")
    def test_lock(self):
        resp = self.json_ok('info', {'uid': self.uid, 'path': self.FILE_PATH, 'meta': 'file_id'})
        file_id = resp['meta']['file_id']

        office_lock_id = '123'
        resp = self.json_ok('office_lock',
                            {'uid': self.uid,
                             'owner_uid': self.uid,
                             'file_id': file_id,
                             'office_lock_id': office_lock_id})

        assert resp['success']
        assert resp['office_lock_id'] == office_lock_id

    @pytest.mark.skipif(True, reason="test hangs")
    def test_already_locked(self):
        resp = self.json_ok('info', {'uid': self.uid, 'path': self.FILE_PATH, 'meta': 'file_id'})
        file_id = resp['meta']['file_id']

        office_lock_id = '123'
        resp = self.json_ok('office_lock',
                            {'uid': self.uid,
                             'owner_uid': self.uid,
                             'file_id': file_id,
                             'office_lock_id': office_lock_id})
        assert resp['success']
        assert resp['office_lock_id'] == office_lock_id

        new_office_lock_id = 'abc'
        resp = self.json_ok('office_lock',
                            {'uid': self.uid,
                             'owner_uid': self.uid,
                             'file_id': file_id,
                             'office_lock_id': new_office_lock_id})
        assert not resp['success']
        assert resp['office_lock_id'] != new_office_lock_id

    @pytest.mark.skipif(True, reason="test hangs")
    def test_parent_locked(self):
        fs = Filesystem()
        resource = get_resource(self.uid, Address.Make(self.uid, '/disk/'))
        fs.set_lock(resource)

        resp = self.json_ok('info', {'uid': self.uid, 'path': self.FILE_PATH, 'meta': 'file_id'})
        file_id = resp['meta']['file_id']

        office_lock_id = '123'
        resp = self.json_ok('office_lock',
                            {'uid': self.uid,
                             'owner_uid': self.uid,
                             'file_id': file_id,
                             'office_lock_id': office_lock_id})
        assert not resp['success']
        assert resp['office_lock_id'] == ''
        fs.unset_lock(resource)

    def test_relock(self):
        resp = self.json_ok('info', {'uid': self.uid, 'path': self.FILE_PATH, 'meta': 'file_id'})
        file_id = resp['meta']['file_id']

        db = CollectionRoutedDatabase()

        import mpfs.core.office.interface
        with mock.patch.object(mpfs.core.office.interface, 'create_operation', return_value=''):
            office_lock_id = '123'
            _ = self.json_ok('office_lock',
                             {'uid': self.uid,
                              'owner_uid': self.uid,
                              'file_id': file_id,
                              'office_lock_id': office_lock_id})

            locks = list(db.filesystem_locks.find({'uid': self.uid}))
            assert len(locks) == 1
            expected_lock_time = datetime.utcnow() - timedelta(minutes=30)
            assert abs(locks[0]['dtime'] - expected_lock_time) < timedelta(minutes=5)

            new_office_lock_id = 'abc'
            resp = self.json_ok('office_lock',
                                {'uid': self.uid,
                                 'owner_uid': self.uid,
                                 'file_id': file_id,
                                 'office_lock_id': new_office_lock_id,
                                 'old_office_lock_id': office_lock_id})
            assert resp['success']
            assert resp['office_lock_id'] == new_office_lock_id

            locks = list(db.filesystem_locks.find({'uid': self.uid}))
            assert len(locks) == 1
            expected_lock_time = datetime.utcnow() - timedelta(minutes=30)
            assert abs(locks[0]['dtime'] - expected_lock_time) < timedelta(minutes=5)

    @pytest.mark.skipif(True, reason="test hangs")
    def test_relock_lock_mismatch(self):
        resp = self.json_ok('info', {'uid': self.uid, 'path': self.FILE_PATH, 'meta': 'file_id'})
        file_id = resp['meta']['file_id']

        office_lock_id = '123'
        _ = self.json_ok('office_lock',
                        {'uid': self.uid,
                         'owner_uid': self.uid,
                         'file_id': file_id,
                         'office_lock_id': office_lock_id})

        bad_lock_id = office_lock_id + '0'
        new_office_lock_id = 'abc'
        resp = self.json_ok('office_lock',
                            {'uid': self.uid,
                             'owner_uid': self.uid,
                             'file_id': file_id,
                             'office_lock_id': new_office_lock_id,
                             'old_office_lock_id': bad_lock_id})
        assert not resp['success']
        assert resp['office_lock_id'] == office_lock_id

    @pytest.mark.skipif(True, reason="test hangs")
    def test_double_lock(self):
        resp = self.json_ok('info', {'uid': self.uid, 'path': self.FILE_PATH, 'meta': 'file_id'})
        file_id = resp['meta']['file_id']

        office_lock_id = '123'
        for _ in xrange(2):
            resp = self.json_ok('office_lock',
                                {'uid': self.uid,
                                 'owner_uid': self.uid,
                                 'file_id': file_id,
                                 'office_lock_id': office_lock_id})
            assert resp['success'] is True

    @pytest.mark.skipif(True, reason="test hangs")
    def test_unlock(self):
        resp = self.json_ok('info', {'uid': self.uid, 'path': self.FILE_PATH, 'meta': 'file_id'})
        file_id = resp['meta']['file_id']

        office_lock_id = '123'
        _ = self.json_ok('office_lock',
                         {'uid': self.uid,
                          'owner_uid': self.uid,
                          'file_id': file_id,
                          'office_lock_id': office_lock_id})

        resp = self.json_ok('office_unlock',
                            {'uid': self.uid,
                             'owner_uid': self.uid,
                             'file_id': file_id,
                             'office_lock_id': office_lock_id})
        assert resp['success'] is True
        assert resp['office_lock_id'] == ''

    @pytest.mark.skipif(True, reason="test hangs")
    def test_unlock_unlocked(self):
        resp = self.json_ok('info', {'uid': self.uid, 'path': self.FILE_PATH, 'meta': 'file_id'})
        file_id = resp['meta']['file_id']

        office_lock_id = '123'
        resp = self.json_ok('office_unlock',
                            {'uid': self.uid,
                             'owner_uid': self.uid,
                             'file_id': file_id,
                             'office_lock_id': office_lock_id})
        assert resp['success'] is False
        assert resp['office_lock_id'] == ''

    @pytest.mark.skipif(True, reason="test hangs")
    def test_unlock_lock_mismatch(self):
        resp = self.json_ok('info', {'uid': self.uid, 'path': self.FILE_PATH, 'meta': 'file_id'})
        file_id = resp['meta']['file_id']

        office_lock_id = '123'
        _ = self.json_ok('office_lock',
                         {'uid': self.uid,
                          'owner_uid': self.uid,
                          'file_id': file_id,
                          'office_lock_id': office_lock_id})

        bad_lock_id = office_lock_id + '0'
        resp = self.json_ok('office_unlock',
                            {'uid': self.uid,
                             'owner_uid': self.uid,
                             'file_id': file_id,
                             'office_lock_id': bad_lock_id})

        assert resp['success'] is False
        assert resp['office_lock_id'] == office_lock_id

    @pytest.mark.skipif(True, reason="test hangs")
    def test_locking_operation_stub(self):
        """Проверить, что операция ставится заново в очередь и
        активна в течении TTL лока.
        """
        fs = Filesystem()
        resource = get_resource(self.uid, Address.Make(self.uid, self.FILE_PATH))
        file_id = resource.meta['file_id']

        _process = OfficeLockingOperationStub._process

        times = [0]

        def new_process(*args, **kwargs):
            times[0] += 1
            if times[0] == 2:
                fs.unset_lock(resource)
            return _process(*args, **kwargs)

        with patch.object(OfficeLockingOperationStub, '_process', new=new_process):
            self.json_ok('office_lock', {
                'uid': self.uid, 'owner_uid': self.uid, 'file_id': file_id, 'office_lock_id': '123'
            })
            assert times[0] == 2

    @pytest.mark.skipif(True, reason="test hangs")
    def test_refresh_lock(self):
        resp = self.json_ok('info', {'uid': self.uid, 'path': self.FILE_PATH, 'meta': 'file_id'})
        file_id = resp['meta']['file_id']

        office_lock_id = '123'
        resp = self.json_ok('office_lock',
                            {'uid': self.uid,
                             'owner_uid': self.uid,
                             'file_id': file_id,
                             'office_lock_id': office_lock_id})
        assert resp['success'] is True
        assert resp['office_lock_id'] == office_lock_id

        old_lock = self._get_lock(self.uid, self.FILE_PATH)

        time.sleep(1)
        resp = self.json_ok('office_lock',
                            {'uid': self.uid,
                             'owner_uid': self.uid,
                             'file_id': file_id,
                             'office_lock_id': office_lock_id,
                             'old_office_lock_id': office_lock_id})
        assert resp['success'] is True
        assert resp['office_lock_id'] == office_lock_id

        lock = self._get_lock(self.uid, self.FILE_PATH)
        assert lock

        old_dtime, new_dtime = old_lock.pop('dtime'), lock.pop('dtime')

        assert old_lock == lock
        assert new_dtime - old_dtime >= timedelta(seconds=1)


class OfficeActionCheckTestCase(OfficeTestCase):
    """Класс тестов рчки `office_action_check`
    """
    def test_common_case(self):
        # Тестируем обычный сценарий
        resp = self.json_ok('office_action_check', {'uid': self.uid,
                                                    'action': 'edit',
                                                    'service_id': 'disk',
                                                    'service_file_id': self.FILE_PATH})
        assert 'office_online_url' in resp
        assert resp['office_online_editor_type'] == 'microsoft_online'

        # Тестируем view старого формата без конвертацию
        resp = self.json_ok('office_action_check', {'uid': self.uid,
                                                    'action': 'view',
                                                    'service_id': 'disk',
                                                    'service_file_id': self.FILE_PATH_2})
        assert 'office_online_url' in resp
        assert resp['office_online_editor_type'] == 'microsoft_online'

        # Тестируем ошибку конвертации
        self.json_error('office_action_check', {'uid': self.uid,
                                                'action': 'edit',
                                                'service_id': 'disk',
                                                'service_file_id': self.FILE_PATH_3
                                                }, code=codes.OFFICE_UNSUPPORTED_EXTENSION)

        # Тестируем проверку uid
        resp = self.json_ok('office_action_check', {'uid': self.uid})
        assert {'editnew': True, 'office_online_editor_type': 'microsoft_online'} == resp

        # Тестируем ошибку конвертации
        self.json_error('office_action_check', {'uid': self.uid,
                                                'action': 'edit',
                                                'service_id': 'disk',
                                                'service_file_id': self.FILE_PATH_3
                                                }, code=codes.OFFICE_UNSUPPORTED_EXTENSION)

        # Тестируем конвертацию
        resp = self.json_ok('office_action_check', {'uid': self.uid,
                                                    'action': 'edit',
                                                    'service_id': 'disk',
                                                    'service_file_id': self.FILE_PATH_2})
        assert 'office_online_url' in resp
        assert resp['office_online_editor_type'] == 'microsoft_online'

        with patch.object(office.logic.microsoft, 'OFFICE_AVAILABLE_FOR_UIDS', new=[]), \
             mock.patch('mpfs.core.office.util.FEATURE_TOGGLES_ONLYOFFICE_EDITOR_FOR_USERS_WITHOUT_EDITOR_ENABLED', False):
            with patch.object(office.logic.microsoft, 'OFFICE_DISABLE_UID_CHECK', new=False):
                # Тестируем ошибку конвертации
                self.json_error('office_action_check', {'uid': self.uid}, code=codes.OFFICE_IS_NOT_ALLOWED)

        # тестируем передачу параметра source

        source = 'dv'

        resp = self.json_ok('office_action_check', {'uid': self.uid,
                                                    'action': 'edit',
                                                    'service_id': 'disk',
                                                    'service_file_id': self.FILE_PATH,
                                                    'source': source})
        assert 'office_online_url' in resp
        url = resp['office_online_url']
        qs = urlparse.parse_qs(urlparse.urlparse(url).query)

        assert 'source' in qs
        assert ''.join(qs['source']) == source

        resp = self.json_ok('office_action_check', {'uid': self.uid,
                                                    'action': 'edit',
                                                    'service_id': 'disk',
                                                    'service_file_id': self.FILE_PATH})
        assert 'office_online_url' in resp
        url = resp['office_online_url']
        qs = urlparse.parse_qs(urlparse.urlparse(url).query)

        assert 'source' not in qs

        # тестируем ручку для service_id=mail
        mail_service_file_id = '1231234012360143/1.1'
        mail_file_name = 'test-file-name'
        mail_file_ext = 'docx'
        mail_full_file_name = '%s.%s' % (mail_file_name, mail_file_ext)
        mail_file_size = 12345

        source = 'dv'
        for service_id in ('mail', 'public'):
            resp = self.json_ok('office_action_check', {'uid': self.uid,
                                                        'action': 'edit',
                                                        'service_id': service_id,
                                                        'service_file_id': mail_service_file_id,
                                                        'source': source,
                                                        'file_name': mail_full_file_name,
                                                        'size': mail_file_size})
            assert 'office_online_url' in resp
            url = resp['office_online_url']
            qs = urlparse.parse_qs(urlparse.urlparse(url).query)

            assert 'source' in qs
            assert ''.join(qs['source']) == source

            assert 'filename' in qs
            assert ''.join(qs['filename']) == mail_file_name

            assert 'ext' in qs
            assert ''.join(qs['ext']) == mail_file_ext

            assert 'size' in qs
            assert int(''.join(qs['size'])) == mail_file_size

    def test_cyrillic_file_name(self):
        # Тестируем обычный сценарий

        name = 'Книга1'
        resp = self.json_ok('office_action_check', {'uid': self.uid,
                                                    'size': 8862,
                                                    'action': 'edit',
                                                    'service_id': 'mail',
                                                    'service_file_id': '2550000003375590816%2F1.1',
                                                    'file_name': '%s.xlsx' % name,
                                                    })
        assert 'office_online_url' in resp
        assert resp['office_online_editor_type'] == 'microsoft_online'
        parsed_url = urlparse.urlparse(resp['office_online_url'])
        qs_params = urlparse.parse_qs(str(parsed_url.query))
        filename = ''.join(qs_params['filename'])
        assert filename == name

        name = 'Workbook1'
        resp = self.json_ok('office_action_check', {'uid': self.uid,
                                                    'size': 8862,
                                                    'action': 'edit',
                                                    'service_id': 'mail',
                                                    'service_file_id': '2550000003375590816%2F1.1',
                                                    'file_name': '%s.xlsx' % name,
                                                    })
        assert 'office_online_url' in resp
        parsed_url = urlparse.urlparse(resp['office_online_url'])
        qs_params = urlparse.parse_qs(str(parsed_url.query))
        filename = ''.join(qs_params['filename'])
        assert filename == name

    def test_shared_file(self):
        other_uid = user_1.uid
        self.create_user(other_uid)

        # создаем папку
        shared_dir = '/disk/shared-folder'
        shared_file = '/disk/shared-folder/test.docx'
        self.json_ok('mkdir', opts={'uid': self.uid, 'path': shared_dir})

        # заливаем туда файл
        self.upload_file(self.uid, shared_file)

        # расшариваем папку
        group = self.json_ok('share_create_group', {'uid': self.uid, 'path': shared_dir})
        # приглашаем тестового пользователя в расшаренную папку
        invite_hash = self.share_invite(group['gid'], other_uid, rights=640)
        # подключаем расшареную папку тестовому пользователю
        self.json_ok('share_activate_invite', {'uid': other_uid, 'hash': invite_hash})


        self.json_error('office_action_check', {'uid': other_uid,
                                                'action': 'edit',
                                                'service_id': 'disk',
                                                'service_file_id': shared_file
                                                }, code=codes.OFFICE_FILE_IS_READ_ONLY, status=403)

        resp = self.json_ok('office_action_check', {'uid': other_uid,
                                                    'action': 'view',
                                                    'service_id': 'disk',
                                                    'service_file_id': shared_file})
        assert 'office_online_url' in resp

        # меняем права группе на 660 и проверяем теперь возможность редактирования:
        self.json_ok('share_change_rights', {'uid': self.uid,
                                             'gid': group['gid'],
                                             'user_uid': other_uid,
                                             'rights': 660})

        resp = self.json_ok('office_action_check', {'uid': other_uid,
                                                    'action': 'edit',
                                                    'service_id': 'disk',
                                                    'service_file_id': shared_file})
        assert 'office_online_url' in resp

        resp = self.json_ok('office_action_check', {'uid': other_uid,
                                                    'action': 'view',
                                                    'service_id': 'disk',
                                                    'service_file_id': shared_file})
        assert 'office_online_url' in resp


@pytest.mark.skipif(True, reason="hang tests")
class OfficeStoreTestCase(OfficeTestCase):
    """Класс тестов ручки `office_store`
    """

    def test_common_case(self):
        """Протестировать создание офисного файла через ручку `office_store`
        """
        info = self.json_ok('info', {
            'uid': self.uid,
            'path': '%s:%s' % (self.uid, self.FILE_PATH),
            'meta': 'file_id'
        })
        file_id = info['meta']['file_id']

        office_lock_id = '123'
        _ = self.json_ok('office_lock', {
            'uid': self.uid,
            'owner_uid': self.uid,
            'file_id': file_id,
            'office_lock_id': office_lock_id,
        })

        resource_id = make_resource_id(self.uid, file_id)
        access_token = office.auth.generate_access_token(self.uid, resource_id)
        response = self.json_ok('office_store',
                                {'resource_id': resource_id,
                                 'access_token': access_token},
                                json={'headers': {'X-Wopi-Lock': office_lock_id}})

        assert not {'store_info', 'response_code',
                    'response_headers', 'response_body'} ^ response.viewkeys()

        assert not {'file-id', 'path', 'max-file-size',
                    'callback', 'service', 'api',
                    'uid', 'oid'} ^ response['store_info'].viewkeys()

    def test_lock_mismatch_failed(self):
        info = self.json_ok('info', {
            'uid': self.uid,
            'path': '%s:%s' % (self.uid, self.FILE_PATH),
            'meta': 'file_id'
        })
        file_id = info['meta']['file_id']

        office_lock_id = '123'
        with patch.object(OfficeLockingOperationStub, '_process', lambda: None):
            response = self.json_ok('office_lock', {
                 'uid': self.uid,
                 'owner_uid': self.uid,
                 'file_id': file_id,
                 'office_lock_id': office_lock_id
            })
            assert response['success'] is True

        bad_lock_id = office_lock_id + '0'
        resource_id = make_resource_id(self.uid, file_id)
        access_token = office.auth.generate_access_token(self.uid, resource_id)
        body = self.json_ok('office_store', {
            'resource_id': resource_id,
            'access_token': access_token
        }, json={'headers': {'X-Wopi-Lock': bad_lock_id}})
        assert self.response.status == 409
        assert body['response_headers']['X-WOPI-Lock'] == office_lock_id
        assert 'store_info' not in body

    def test_zero_file_failed(self):
        zero_path = '/disk/zero.docx'
        self.upload_file(self.uid, zero_path, file_data={'size': 0})
        info = self.json_ok('info', {
            'uid': self.uid,
            'path': '%s:%s' % (self.uid, zero_path),
            'meta': 'file_id'
        })
        file_id = info['meta']['file_id']

        resource_id = make_resource_id(self.uid, file_id)
        access_token = office.auth.generate_access_token(self.uid, resource_id)
        body = self.json_ok('office_store',
                            {'resource_id': resource_id,
                             'access_token': access_token},
                            json={'headers': {}})

        assert 'store_info' in body

    def test_no_lock_failed(self):
        info = self.json_ok('info', {
            'uid': self.uid,
            'path': '%s:%s' % (self.uid, self.FILE_PATH),
            'meta': 'file_id'
        })
        file_id = info['meta']['file_id']

        resource_id = make_resource_id(self.uid, file_id)
        access_token = office.auth.generate_access_token(self.uid, resource_id)
        body = self.json_ok('office_store', {
            'resource_id': resource_id,
            'access_token': access_token
        }, json={'headers': {}})
        assert self.response.status == 409
        assert body['response_headers']['X-WOPI-Lock'] == ''
        assert 'store_info' not in body

    def test_store_shared(self):
        """Протестировать создание нового документа из интерфейса гостя в папке владельца.
        """
        other_uid = user_1.uid
        self.create_user(other_uid)

        shared_dir = '/disk/shared-folder'
        self.json_ok('mkdir', opts={'uid': self.uid, 'path': shared_dir})

        group = self.json_ok('share_create_group', {'uid': self.uid, 'path': shared_dir})
        invite_hash = self.share_invite(group['gid'], other_uid, rights=660)
        self.json_ok('share_activate_invite', {'uid': other_uid, 'hash': invite_hash})

        resp = self.json_ok('office_action_data', {
            'uid': other_uid, 'action': 'editnew', 'path': shared_dir, 'ext': 'xlsx'
        })
        resource_id = resp['resource_id']
        access_token = resp['access_token']
        body = self.json_ok('office_store',
                            {'resource_id': resource_id,
                             'access_token': access_token},
                            json={'headers': {}})

        assert 'store_info' in body

    def test_keep_public_hash(self):
        size = 1024
        path = '/disk/document.xls'
        self.upload_file(self.uid, path, file_data={'size': size})
        self.json_ok('set_public', {'uid': self.uid,
                                    'path': path})
        info = self.json_ok('info', {'uid': self.uid,
                                     'path': '%s:%s' % (self.uid, path),
                                     'meta': ','})
        file_id = info['meta']['file_id']
        assert 'public' in info['meta']
        public_hash = info['meta']['public_hash']

        office_lock_id = '123'
        with patch.object(OfficeLockingOperationStub, '_process', lambda: None):
            self.json_ok('office_lock', {'uid': self.uid,
                                         'owner_uid': self.uid,
                                         'file_id': file_id,
                                         'office_lock_id': office_lock_id})

        resource_id = make_resource_id(self.uid, file_id)
        access_token = office.auth.generate_access_token(self.uid, resource_id)
        response = self.json_ok('office_store', {'resource_id': resource_id,
                                                 'access_token': access_token},
                                json={'headers': {'X-Wopi-Lock': office_lock_id}})

        assert 'oid' in response['store_info']
        oid = response['store_info']['oid']

        for body in self._get_kladun_callback_bodies(path):
            with KladunStub(status_values=(body,)):
                self.service_ok('kladun_callback', {'uid': self.uid,
                                                    'oid': oid,
                                                    'status_xml': etree.tostring(body),
                                                    'type': None})

        info = self.json_ok('info', {'uid': self.uid,
                                     'path': '%s:%s' % (self.uid, path),
                                     'meta': ','})
        assert 'public' in info['meta']
        assert public_hash == info['meta']['public_hash']

        response = self.json_ok('public_info', {'private_hash': public_hash})
        assert response

    def test_keep_lock(self):
        """Протестировать сохранение лока при перезаписи
        """
        info = self.json_ok('info', {'uid': self.uid,
                                     'path': '%s:%s' % (self.uid, self.FILE_PATH),
                                     'meta': ','})
        file_id = info['meta']['file_id']

        office_lock_id = '123'
        with patch.object(OfficeLockingOperationStub, '_process', lambda: None):
            self.json_ok('office_lock', {'uid': self.uid,
                                         'owner_uid': self.uid,
                                         'file_id': file_id,
                                         'office_lock_id': office_lock_id})

        resource_id = make_resource_id(self.uid, file_id)
        access_token = office.auth.generate_access_token(self.uid, resource_id)
        response = self.json_ok('office_store', {'resource_id': resource_id,
                                                 'access_token': access_token},
                                json={'headers': {'X-Wopi-Lock': office_lock_id}})

        assert 'oid' in response['store_info']
        oid = response['store_info']['oid']

        for body in self._get_kladun_callback_bodies(self.FILE_PATH):
            with KladunStub(status_values=(body,)):
                self.service_ok('kladun_callback', {'uid': self.uid,
                                                    'oid': oid,
                                                    'status_xml': etree.tostring(body),
                                                    'type': None})

        lock = self._get_lock(self.uid, self.FILE_PATH)
        assert lock

    def test_no_left_space_success(self):
        """Протестировать сохранение файла если не хватает места на Диске"""
        info = self.json_ok('info', {
            'uid': self.uid,
            'path': '%s:%s' % (self.uid, self.FILE_PATH),
            'meta': ','
        })
        file_id = info['meta']['file_id']

        office_lock_id = '123'
        with patch.object(OfficeLockingOperationStub, '_process', lambda: None):
            self.json_ok('office_lock', {'uid': self.uid,
                                         'owner_uid': self.uid,
                                         'file_id': file_id,
                                         'office_lock_id': office_lock_id})

        resource_id = make_resource_id(self.uid, file_id)
        access_token = office.auth.generate_access_token(self.uid, resource_id)

        with patch.object(Quota, 'free', return_value=-1341933007):
            response = self.json_ok('office_store',
                                    {'resource_id': resource_id,
                                     'access_token': access_token},
                                    json={'headers': {'X-Wopi-Lock': office_lock_id}})

        assert 'oid' in response['store_info']
        oid = response['store_info']['oid']

        free_space = Filesystem().quota.free(uid=self.uid)
        for body in self._get_kladun_callback_bodies(self.FILE_PATH, size=free_space + 1):
            with KladunStub(status_values=(body,)):
                self.service_ok('kladun_callback', {'uid': self.uid,
                                                    'oid': oid,
                                                    'status_xml': etree.tostring(body),
                                                    'type': None})

        info = self.json_ok('info', {'uid': self.uid,
                                     'path': '%s:%s' % (self.uid, self.FILE_PATH),
                                     'meta': ','})
        assert free_space + 1 == info['meta']['size']


class OfficeOnlineUrlTestCase(OfficeTestCase):
    """Класс тестов для работы с `office_online_url`
    """

    def test_incorrect_tld(self):

        # проверим, что по-умолчанию возвращается ссылка на ru
        info = self.json_ok('info', {
            'uid': self.uid,
            'path': '%s:%s' % (self.uid, self.FILE_PATH),
            'meta': ''
        })

        assert 'office_online_url' in info['meta']

        url = info['meta']['office_online_url']
        hostname = urlparse.urlparse(url).hostname
        assert hostname.endswith('.ru')

        # теперь укажем tld=com и проверим, что возвращается ссылка на com
        info = self.json_ok('info', {
            'uid': self.uid,
            'path': '%s:%s' % (self.uid, self.FILE_PATH),
            'meta': '',
            'tld': 'sk'
        })

        assert 'office_online_url' in info['meta']

        url = info['meta']['office_online_url']
        hostname = urlparse.urlparse(url).hostname
        assert hostname.endswith('.ru')

    def test_get_url_by_info(self):

        # проверим, что по-умолчанию возвращается ссылка на ru
        info = self.json_ok('info', {
            'uid': self.uid,
            'path': '%s:%s' % (self.uid, self.FILE_PATH),
            'meta': ''
        })

        assert 'office_online_url' in info['meta']

        url = info['meta']['office_online_url']
        hostname = urlparse.urlparse(url).hostname
        assert hostname.endswith('.ru')

        # теперь укажем tld=uk и проверим, что возвращается ссылка на uk
        info = self.json_ok('info', {
            'uid': self.uid,
            'path': '%s:%s' % (self.uid, self.FILE_PATH),
            'meta': '',
            'tld': 'com'
        })

        assert 'office_online_url' in info['meta']

        url = info['meta']['office_online_url']
        hostname = urlparse.urlparse(url).hostname
        assert hostname.endswith('.com')

    def test_get_url_by_list(self):

        # проверим, что по-умолчанию возвращается ссылка на ru
        res = self.json_ok('list', {
            'uid': self.uid,
            'path': '/disk',
            'meta': ''
        })

        file_item = None
        for item in res:
            if item['id'] == self.FILE_PATH:
                file_item = item
                break

        assert file_item is not None
        assert 'office_online_url' in file_item['meta']

        url = file_item['meta']['office_online_url']
        hostname = urlparse.urlparse(url).hostname
        assert hostname.endswith('.ru')

        # теперь укажем tld=uk и проверим, что возвращается ссылка на uk
        res = self.json_ok('list', {
            'uid': self.uid,
            'path': '/disk',
            'meta': '',
            'tld': 'com'
        })

        file_item = None
        for item in res:
            if item['id'] == self.FILE_PATH:
                file_item = item
                break

        assert file_item is not None
        assert 'office_online_url' in file_item['meta']

        url = file_item['meta']['office_online_url']
        hostname = urlparse.urlparse(url).hostname
        assert hostname.endswith('.com')

    def test_get_url_by_public_list_and_public_info(self):
        dir_path = '/disk/dir'
        file_path = '%s/old.docx' % dir_path

        self.json_ok('mkdir', {'uid': self.uid, 'path': dir_path})
        self.upload_file(self.uid, file_path)
        file_public_hash = self.json_ok('set_public', {'uid': self.uid, 'path': file_path})['hash']
        dir_public_hash = self.json_ok('set_public', {'uid': self.uid, 'path': dir_path})['hash']

        office_online_url = build_office_online_url('public', file_public_hash, 'ua')

        resp = self.json_ok('public_info', {'private_hash': file_public_hash, 'meta': 'office_online_url', 'tld': 'ua'})
        assert office_online_url == resp['resource']['meta']['office_online_url']

        resp = self.json_ok('public_list', {'private_hash': file_public_hash, 'meta': 'office_online_url', 'tld': 'ua'})
        assert office_online_url == resp['meta']['office_online_url']

        resp = self.json_ok('public_list', {'private_hash': dir_public_hash, 'meta': 'office_online_url', 'tld': 'ua'})
        assert office_online_url == resp[1]['meta']['office_online_url']

    def test_get_url_by_public_list_file_not_public_success(self):
        """Проверить наличие урла для непубличного файла в публичной папке.
        """
        dir_path = '/disk/dir'
        file_path = '%s/test.docx' % dir_path

        self.json_ok('mkdir', {'uid': self.uid, 'path': dir_path})
        self.upload_file(self.uid, file_path)
        dir_public_hash = self.json_ok('set_public', {'uid': self.uid, 'path': dir_path})['hash']

        file_public_path = '%s:/%s' % (dir_public_hash, file_path.split('/')[-1])
        office_online_url = build_office_online_url('public', file_public_path, 'ua')

        resp = self.json_ok('public_list', {'private_hash': dir_public_hash, 'meta': 'office_online_url', 'tld': 'ua'})
        assert office_online_url == resp[1]['meta']['office_online_url']

    def test_get_url_by_office_action_check(self):

        # проверим, что по-умолчанию возвращается ссылка на ru
        response = self.json_ok('office_action_check', {
            'uid': self.uid,
            'action': 'edit',
            'service_id': 'disk',
            'service_file_id': self.FILE_PATH,
        })

        assert 'office_online_url' in response

        url = response['office_online_url']
        hostname = urlparse.urlparse(url).hostname
        assert hostname.endswith('.ru')


        # теперь укажем tld=uk и проверим, что возвращается ссылка на uk
        response = self.json_ok('office_action_check', {
            'uid': self.uid,
            'action': 'edit',
            'service_id': 'disk',
            'service_file_id': self.FILE_PATH,
            'tld': 'com'
        })

        assert 'office_online_url' in response

        url = response['office_online_url']
        hostname = urlparse.urlparse(url).hostname
        assert hostname.endswith('.com')

    def test_get_url_by_new_search(self):
        search_response = get_search_item_response(self.FILE_PATH, '*')
        with patch.object(DiskSearch, 'open_url') as mocked_open_url:
            mocked_open_url.return_value = search_response
            resp = self.json_ok('new_search', {'uid': self.uid,
                                               'path': '/disk',
                                               'query': 'docx',
                                               'meta': ''})

        assert 'results' in resp

        for item in resp['results']:
            if item['type'] != 'file':
                continue

            assert 'office_online_url' in item['meta']

    def test_url_is_urlencoded(self):
        """Протестировать, что office_online_url URL-safe.

        Например, символ `#` должен экранироваться.
        """

        file_path = u"/disk/Документ# 1.docx"
        self.upload_file(self.uid, file_path)

        # URL из office_action_check
        response = self.json_ok('office_action_check', {
            'uid': self.uid,
            'action': 'edit',
            'service_id': 'disk',
            'service_file_id': file_path,
        })
        assert urllib2.quote(file_path[1:].encode('utf-8'), safe='') in response['office_online_url'].encode('utf-8')

        # URL из меты
        response = self.json_ok('info', {'uid': self.uid, 'path': file_path, 'meta': ''})
        assert urllib2.quote(file_path[1:].encode('utf-8'), safe='') in response['meta']['office_online_url'].encode('utf-8')

    @pytest.mark.skipif(not REAL_MONGO,
                        reason='https://st.yandex-team.ru/CHEMODAN-34246')
    def test_get_url_by_get_last_files(self):
        """Протестировать наличие поля `office_online_url` в мете офисных файлов
        из ответа ручки `get_last_files`.
        """
        response = self.json_ok('get_last_files', {
            'uid': self.uid,
            'meta': 'office_online_url',
            'tld': 'com',
            'amount': 50
        })
        assert response

        office_files = []
        for r in response:
            if r['name'].endswith('doc') or r['name'].endswith('docx'):
                office_files.append(r)

        assert office_files
        assert office_files == filter(lambda f: 'office_online_url' in f['meta'], office_files)


class OfficeRenameTestCase(OfficeTestCase):
    """Класс тестов ручки `office_rename`
    """
    def test_common_case(self):
        resp = self.json_ok('info', {'uid': self.uid, 'path': self.FILE_PATH, 'meta': 'file_id'})
        file_id = resp['meta']['file_id']

        office_lock_id = '123'
        resp = self.json_ok('office_lock',
                            {'uid': self.uid,
                             'owner_uid': self.uid,
                             'file_id': file_id,
                             'office_lock_id': office_lock_id})
        assert resp['success'] is True

        new_name = 'new'
        resp = self.json_ok('office_rename',
                            {'uid': self.uid,
                             'owner_uid': self.uid,
                             'file_id': file_id,
                             'office_lock_id': office_lock_id,
                             'new_name': new_name})

        assert resp['success'] is True

        resp = self.json_ok('info_by_file_id', {'uid': self.uid, 'owner_uid': self.uid, 'file_id': file_id})

        new_fullname = new_name + os.path.splitext(self.FILE_PATH)[1]
        assert resp['name'] == new_fullname

    def test_lock_mismatch(self):
        """ Проверить переименование, когда переданный лок не совпадает с установленным
        """
        resp = self.json_ok('info', {'uid': self.uid, 'path': self.FILE_PATH, 'meta': 'file_id'})
        file_id = resp['meta']['file_id']
        # old_fullname = resp['name']

        office_lock_id = '123'
        resp = self.json_ok('office_lock',
                            {'uid': self.uid,
                             'owner_uid': self.uid,
                             'file_id': file_id,
                             'office_lock_id': office_lock_id})
        assert resp['success'] is True

        new_name = 'new'
        resp = self.json_ok('office_rename',
                            {'uid': self.uid,
                             'owner_uid': self.uid,
                             'file_id': file_id,
                             'office_lock_id': '321',
                             'new_name': new_name})
        # TODO
        # Венуть False, когда МС научится правильно работать с локами в этой ручке.
        # Сейчас работает костыль, когда мы игнорируем локи от Офиса
        assert resp['success'] is True
        assert resp['office_lock_id'] == office_lock_id

        resp = self.json_ok('info_by_file_id', {'uid': self.uid, 'owner_uid': self.uid, 'file_id': file_id})
        # TODO
        # Вернуть old_full_name когда уберем костыль
        assert resp['name'] == new_name + '.docx'

    def test_unlocked(self):
        resp = self.json_ok('info', {'uid': self.uid, 'path': self.FILE_PATH, 'meta': 'file_id'})
        file_id = resp['meta']['file_id']

        new_name = 'new'
        resp = self.json_ok('office_rename',
                            {'uid': self.uid,
                             'owner_uid': self.uid,
                             'file_id': file_id,
                             'office_lock_id': '',
                             'new_name': new_name})

        assert resp['success'] is True

        resp = self.json_ok('info_by_file_id', {'uid': self.uid, 'owner_uid': self.uid, 'file_id': file_id})

        new_fullname = new_name + os.path.splitext(self.FILE_PATH)[1]
        assert resp['name'] == new_fullname

    def test_size_limits_constraints(self):
        # Тестируем такое поведение. Для старых файлов (doc, xls, ppt) мы проверяем ограничения по размеру входных на
        # конвертацию файлов. Для новых - по таблице размеров от Офиса (т.к. мы их сразу открываем офисом,
        # а не конвертируем)

        file_size = 12345
        old_file_path = '/disk/old.doc'
        new_file_path = '/disk/old.docx'
        self.upload_file(self.uid, old_file_path, file_data={'size': file_size})
        self.upload_file(self.uid, new_file_path, file_data={'size': file_size})

        with patch.object(microsoft, 'OFFICE_AVAILABLE_FOR_UIDS', new=[self.uid]):
            with patch.dict(OFFICE_CONVERT_SIZE_LIMITS, {'doc': file_size - 1}):
                self.json_error('office_action_check', {
                        'uid': self.uid,
                        'action': 'edit',
                        'service_id': 'disk',
                        'service_file_id': old_file_path
                    },
                    code=codes.OFFICE_FILE_TOO_LARGE
                )

                resp = self.json_ok('info', {
                    'uid': self.uid,
                    'path': old_file_path,
                    'meta': ''
                })
                assert 'office_online_url' not in resp['meta']

            with patch.dict(OFFICE_CONVERT_SIZE_LIMITS, {'doc': file_size + 1}):
                resp = self.json_ok('office_action_check',
                                    {
                                        'uid': self.uid,
                                        'action': 'edit',
                                        'service_id': 'disk',
                                        'service_file_id': old_file_path
                                    })
                assert 'office_online_url' in resp

                resp = self.json_ok('info', {
                    'uid': self.uid,
                    'path': old_file_path,
                    'meta': ''
                })
                assert 'office_online_url' in resp['meta']

            with patch.dict(OFFICE_SIZE_LIMITS, {'Word': {'edit': file_size - 1}}):
                resp = self.json_error('office_action_check',
                                       {
                                           'uid': self.uid,
                                           'action': 'edit',
                                           'service_id': 'disk',
                                           'service_file_id': new_file_path
                                       },
                                       code=codes.OFFICE_FILE_TOO_LARGE)

                resp = self.json_ok('info', {
                    'uid': self.uid,
                    'path': new_file_path,
                    'meta': ''
                })
                assert 'office_online_url' not in resp['meta']

            with patch.dict(OFFICE_SIZE_LIMITS, {'Word': {'edit': file_size + 1}}):
                resp = self.json_ok('office_action_check',
                                    {
                                        'uid': self.uid,
                                        'action': 'edit',
                                        'service_id': 'disk',
                                        'service_file_id': new_file_path
                                    })
                assert 'office_online_url' in resp

                resp = self.json_ok('info', {
                    'uid': self.uid,
                    'path': new_file_path,
                    'meta': ''
                })
                assert 'office_online_url' in resp['meta']

    def test_office_action_data_edit_public(self):
        file_path = '/disk/old.docx'
        self.upload_file(self.uid, file_path)
        file_public_hash = self.json_ok('set_public', {'uid': self.uid, 'path': file_path})['hash']

        with patch.object(microsoft, 'OFFICE_AVAILABLE_FOR_UIDS', [self.uid]):
            response = self._office_action_data_edit(self.uid, file_public_hash, service_id='public')
            oid = response['oid']
            resp = self.json_ok('status', {'oid': oid, 'uid': self.uid, 'meta': 'office_online_url', 'tld': 'ua'})
            assert '.ua/' in resp['resource']['meta']['office_online_url']

    def test_office_action_data_edit_public_folder_file_not_public(self):
        """Протестировать редактирование непубличного файла в публичной папке.
        """
        dir_path = '/disk/dir'
        file_path = '%s/old.docx' % dir_path

        self.json_ok('mkdir', {'uid': self.uid, 'path': dir_path})
        self.upload_file(self.uid, file_path)
        dir_public_hash = self.json_ok('set_public', {'uid': self.uid, 'path': dir_path})['hash']

        file_public_path = '%s:/%s' % (dir_public_hash, file_path.split('/')[-1])
        with patch.object(microsoft, 'OFFICE_AVAILABLE_FOR_UIDS', [self.uid]):
            resp = self._office_action_data_edit(self.uid, file_public_path, service_id='public')
            oid = resp['oid']
            resp = self.json_ok('status', {'oid': oid, 'uid': self.uid, 'meta': 'office_online_url', 'tld': 'ua'})

            office_online_url = build_office_online_url('disk', resp['resource']['id'][1:], 'ua')
            assert office_online_url == resp['resource']['meta']['office_online_url']

    @staticmethod
    def _get_lock(uid, path):
        fs = Filesystem()
        resource = get_resource(uid, Address.Make(uid, path))
        return fs.get_lock(resource)

    def _get_kladun_callback_bodies(self, path, file_md5=None, file_sha256=None, size=None):
        body_1 = etree.fromstring(open('fixtures/xml/kladun_store_1.xml').read())
        body_2 = etree.fromstring(open('fixtures/xml/kladun_store_2.xml').read())
        body_3 = etree.fromstring(open('fixtures/xml/kladun_store_3.xml').read())

        rand = str('%f' % time.time()).replace('.', '')[9:]

        if file_md5 is None:
            file_md5 = hashlib.md5(rand).hexdigest()

        if file_sha256 is None:
            file_sha256 = hashlib.sha256(rand).hexdigest()

        file_id = hashlib.sha256(file_md5 + ':' + file_sha256).hexdigest()

        if size is None:
            size = int(rand)

        mid_digest = '100000.yadisk:%s.%s' % (self.uid, int(file_md5[:16], 16))
        mid_file = '100000.yadisk:%s.%s' % (self.uid, int(file_md5[:16][::-1], 16))
        drweb = 'true'
        mimetype = 'application/x-www-form-urlencoded'

        for body in (body_1, body_2, body_3):
            body.find('request').find('chemodan-file-attributes').set('uid', self.uid)
            body.find('request').find('chemodan-file-attributes').set('file-id', file_id)
            body.find('request').find('chemodan-file-attributes').set('path', path)
            for arg in ('current', 'total'):
                body.find('stages').find('incoming-http').find('progress').set(arg, str(size))
            for tag in ('incoming-http', 'incoming-file'):
                body.find('stages').find(tag).find('result').set('content-length', str(size))
                body.find('stages').find(tag).find('result').set('content-type', mimetype)
            body.find('stages').find('incoming-file').find('result').set('md5', file_md5)
            body.find('stages').find('incoming-file').find('result').set('sha256', file_sha256)
        for body in (body_2, body_3):
            body.find('stages').find('mulca-file').find('result').set('mulca-id', mid_file)
            body.find('stages').find('mulca-digest').find('result').set('mulca-id', mid_digest)
            body.find('stages').find('antivirus').find('result').set('result', drweb)

        return map(etree.tostring, (body_1, body_2, body_3))

    def _office_action_data(self, path, hardlink, size_constraint_failed, file_md5=None, file_sha256=None,
                            file_size=None, opts=None):
        """Проверяем ответ ручки, если требуется конвертация"""
        new_path = path + 'x'
        body_1, body_2, body_3 = self._get_kladun_callback_bodies(new_path, file_md5, file_sha256, file_size)
        with KladunStub(status_values=(body_1,
                                       body_2,
                                       body_3)):
            if opts is None:
                opts = {'uid': self.uid, 'action': 'edit', 'path': path}

            response = self.json_ok('office_action_data', opts)

            assert 'oid' in response
            oid = response['oid']

            opts = {'uid': self.uid, 'oid': oid, 'status_xml': body_1, 'type': None}
            if hardlink:
                with self.patch_mulca_is_file_exist(func_resp=True):
                    if size_constraint_failed:
                        # Callback #1
                        response = self.service_error('kladun_callback', opts)
                        self.assertEqual(response['code'], codes.KLADUN_CONFLICT)
                    else:
                        # Callback #1
                        response = self.service_error('kladun_callback', opts)
                        self.assertEqual(response['code'], codes.KLADUN_HARDLINK_FOUND)
            elif size_constraint_failed:
                    # Callback #1
                    response = self.service_error('kladun_callback', opts)
                    self.assertEqual(response['code'], codes.KLADUN_CONFLICT)
            else:
                for body in (body_1, body_2, body_3):
                    opts = {'uid': self.uid, 'oid': oid, 'status_xml': body, 'type': None}
                    self.service_ok('kladun_callback', opts)

        # сохраняем чтоб иметь возможность потом зачистить все созданные тестом файлы
        self.uploaded_files.append({'uid': self.uid, 'path': new_path})
        return oid


class OfficeEditPossibleTestCase(OfficeTestCase):
    PDD_UID = pdd_user.uid

    def setup_method(self, method):
        super(OfficeEditPossibleTestCase, self).setup_method(method)

        self.create_user(self.PDD_UID, noemail=1)
        self.upload_file(self.PDD_UID, self.FILE_PATH, file_data={'size': 1024})
        user_info = Passport().userinfo(self.PDD_UID)
        assert Passport().is_from_pdd(user_info['login'])

    @patch.object(microsoft, 'OFFICE_DISABLE_FOR_B2B', new=False)
    @patch.object(microsoft, 'FEATURE_TOGGLES_CHECK_OFFICE_BY_DOMAIN_ENABLED', new=True)
    @patch.object(StandartUser, 'is_pdd', return_value=True)
    @patch.object(StandartUser, 'is_domain_allowed_for_office', return_value=True)
    def test_pdd_editor_allowed(self, is_pdd_mocked, is_office_allowed_mocked):
        resp = self.json_ok('office_action_check', {
            'uid': self.PDD_UID,
            'action': 'edit',
            'service_id': 'disk',
            'service_file_id': self.FILE_PATH})

        assert is_pdd_mocked.called
        assert is_office_allowed_mocked.called
        assert 'office_online_url' in resp

    @patch.object(microsoft, 'OFFICE_DISABLE_FOR_B2B', new=False)
    @patch.object(microsoft, 'FEATURE_TOGGLES_CHECK_OFFICE_BY_DOMAIN_ENABLED', new=True)
    @patch.object(StandartUser, 'is_pdd', return_value=True)
    @patch.object(StandartUser, 'is_domain_allowed_for_office', return_value=False)
    @mock.patch('mpfs.core.office.util.FEATURE_TOGGLES_ONLYOFFICE_EDITOR_FOR_USERS_WITHOUT_EDITOR_ENABLED', False)
    def test_pdd_editor_not_allowed(self, is_pdd_mocked, is_office_allowed_mocked):
        self.json_error('office_action_check', {
            'uid': self.PDD_UID,
            'action': 'edit',
            'service_id': 'disk',
            'service_file_id': self.FILE_PATH
        }, code=codes.OFFICE_IS_NOT_ALLOWED)

        assert is_pdd_mocked.called
        assert is_office_allowed_mocked.called

    @patch.object(microsoft, 'OFFICE_DISABLE_FOR_B2B', new=True)
    @patch.object(microsoft, 'FEATURE_TOGGLES_CHECK_OFFICE_BY_DOMAIN_ENABLED', new=True)
    @patch.object(StandartUser, 'is_pdd', return_value=True)
    @patch.object(StandartUser, 'is_domain_allowed_for_office', return_value=True)
    @patch.object(StandartUser, 'is_b2b', return_value=True)
    @mock.patch('mpfs.core.office.util.FEATURE_TOGGLES_ONLYOFFICE_EDITOR_FOR_USERS_WITHOUT_EDITOR_ENABLED', False)
    def test_b2b_and_pdd_editor_not_allowed(self, *mocks):
        self.json_ok('user_make_b2b', {'uid': self.PDD_UID, 'b2b_key': 'true'})
        self.json_error('office_action_check', {
            'uid': self.PDD_UID,
            'action': 'edit',
            'service_id': 'disk',
            'service_file_id': self.FILE_PATH
        }, code=codes.OFFICE_IS_NOT_ALLOWED)

    @patch.object(microsoft, 'OFFICE_DISABLE_FOR_B2B', new=True)
    @mock.patch('mpfs.core.office.util.FEATURE_TOGGLES_ONLYOFFICE_EDITOR_FOR_USERS_WITHOUT_EDITOR_ENABLED', False)
    def test_b2b_not_allowed(self, *mocks):
        self.json_ok('user_make_b2b', {'uid': self.PDD_UID, 'b2b_key': 'true'})
        self.json_error('office_action_check', {
            'uid': self.PDD_UID,
            'action': 'edit',
            'service_id': 'disk',
            'service_file_id': self.FILE_PATH
        }, code=codes.OFFICE_IS_NOT_ALLOWED)

    @patch.object(microsoft, 'OFFICE_DISABLE_FOR_B2B', new=False)
    @patch.object(microsoft, 'FEATURE_TOGGLES_CHECK_OFFICE_BY_DOMAIN_ENABLED', new=True)
    @patch.object(StandartUser, 'is_pdd', return_value=True)
    @patch.object(StandartUser, 'is_b2b', return_value=True)
    @patch.object(StandartUser, 'is_domain_allowed_for_office', return_value=True)
    def test_b2b_has_allowed_domain(self, *mocks):
        self.json_ok('user_make_b2b', {'uid': self.PDD_UID, 'b2b_key': 'true'})
        resp = self.json_ok('office_action_check', {
            'uid': self.PDD_UID,
            'action': 'edit',
            'service_id': 'disk',
            'service_file_id': self.FILE_PATH})

        for mocked_method in mocks:
            assert mocked_method.called

        assert 'office_online_url' in resp

    @patch.object(microsoft, 'OFFICE_DISABLE_FOR_B2B', new=False)
    @patch.object(microsoft, 'FEATURE_TOGGLES_CHECK_OFFICE_BY_DOMAIN_ENABLED', new=True)
    @patch.object(StandartUser, 'is_pdd', return_value=True)
    @patch.object(StandartUser, 'is_b2b', return_value=True)
    @patch.object(StandartUser, 'is_domain_allowed_for_office', return_value=False)
    @mock.patch('mpfs.core.office.util.FEATURE_TOGGLES_ONLYOFFICE_EDITOR_FOR_USERS_WITHOUT_EDITOR_ENABLED', False)
    def test_b2b_has_no_allowed_domains(self, *mocks):
        self.json_ok('user_make_b2b', {'uid': self.PDD_UID, 'b2b_key': 'true'})
        self.json_error('office_action_check', {
            'uid': self.PDD_UID,
            'action': 'edit',
            'service_id': 'disk',
            'service_file_id': self.FILE_PATH
        }, code=codes.OFFICE_IS_NOT_ALLOWED)

        for mocked_method in mocks:
            assert mocked_method.called

    @patch.object(microsoft, 'OFFICE_DISABLE_FOR_B2B', new=False)
    @patch.object(microsoft, 'FEATURE_TOGGLES_CHECK_OFFICE_BY_DOMAIN_ENABLED', new=True)
    @patch.object(StandartUser, 'get_online_editor_enabled', return_value=False)
    @mock.patch('mpfs.core.office.util.FEATURE_TOGGLES_ONLYOFFICE_EDITOR_FOR_USERS_WITHOUT_EDITOR_ENABLED', False)
    def test_common_case(self, *mocks):
        user_info = Passport().userinfo(self.PDD_UID)
        OfficeAllowedPDDDomain(domain=user_info['pdd_domain']).save(upsert=True)

        resp = self.json_ok('office_action_check', {
            'uid': self.PDD_UID,
            'action': 'edit',
            'service_id': 'disk',
            'service_file_id': self.FILE_PATH})
        assert 'office_online_url' in resp

        OfficeAllowedPDDDomain(domain=user_info['pdd_domain']).delete()
        self.json_error('office_action_check', {
            'uid': self.PDD_UID,
            'action': 'edit',
            'service_id': 'disk',
            'service_file_id': self.FILE_PATH
        }, code=codes.OFFICE_IS_NOT_ALLOWED)


class OfficeAllowedPDDDomainTestCase(OfficeTestCase):
    DOMAINS = ('first@gmail.com', 'second@gmail.com',
               'third@gmail.com')

    def setup_method(self, method):
        super(OfficeAllowedPDDDomainTestCase, self).setup_method(method)

        domain_model = OfficeAllowedPDDDomain()
        for domain in self.DOMAINS:
            domain_model.domain = domain
            domain_model.save(upsert=True)
            assert domain_model.domain == domain

    def test_save_and_get(self):
        item = OfficeAllowedPDDDomain.controller.get(domain=self.DOMAINS[0])
        assert item is not None
        assert item.domain == self.DOMAINS[0]

    def test_filter_non_empty_query(self):
        cursor = OfficeAllowedPDDDomain.controller.filter(domain={'$in': self.DOMAINS[1:]})
        assert cursor.count() == 2
        assert tuple(item.domain for item in cursor) == self.DOMAINS[1:]


class HancomUserTestCase(OfficeTestCase):

    @parameterized.expand([
        (True,),
        (False,),
    ])
    def test_enable_hancom_test_case(self, hancom_enabled_at_start):
        if hancom_enabled_at_start:
            UserDAO().enable_hancom(self.uid)
        else:
            UserDAO().disable_hancom(self.uid)

        self.json_ok('office_enable_hancom', {'uid': self.uid})
        UserIndexCollection.reset()
        assert User(self.uid).is_hancom_enabled()
        assert UserDAO().find_one({'_id': self.uid}).get('hancom_enabled')

    @parameterized.expand([
        (True,),
        (False,),
    ])
    def test_disable_hancom_test_case(self, hancom_emabled_at_start):
        if hancom_emabled_at_start:
            UserDAO().enable_hancom(self.uid)
        else:
            UserDAO().disable_hancom(self.uid)

        self.json_ok('office_disable_hancom', {'uid': self.uid})
        UserIndexCollection.reset()
        assert not User(self.uid).is_hancom_enabled()
        assert not UserDAO().find_one({'_id': self.uid}).get('hancom_enabled')

    def test_hancom_enabled_for_created_user(self):
        with mock.patch('mpfs.core.user.standart.OFFICE_HANCOM_USER_PERCENTAGE', 100), \
             mock.patch('mpfs.core.user.standart.FEATURE_TOGGLES_REGISTER_HANCOM_USERS', True):
            self.json_ok('user_init', {'uid': self.user_1.uid})
            assert User(self.user_1.uid).is_hancom_enabled()

    def test_browser_editing_disables_hancom(self):
        with mock.patch('mpfs.core.user.standart.OFFICE_HANCOM_USER_PERCENTAGE', 100), \
             mock.patch('mpfs.core.user.standart.FEATURE_TOGGLES_REGISTER_HANCOM_USERS', True):
            self.json_ok('user_init', {'uid': self.user_1.uid})
            # также убеждаемся, что ханком отключаем только после office_action_data
            parsed_url = self.__office_action_check()
            assert User(self.user_1.uid).is_hancom_enabled()
            self.__office_action_data(parsed_url)
            assert not User(self.user_1.uid).is_hancom_enabled()

    def test_web_editing_does_not_disable_hancom(self):
        with mock.patch('mpfs.core.user.standart.OFFICE_HANCOM_USER_PERCENTAGE', 100), \
             mock.patch('mpfs.core.user.standart.FEATURE_TOGGLES_REGISTER_HANCOM_USERS', True):
            self.json_ok('user_init', {'uid': self.user_1.uid})
            parsed_url = self.__office_action_check('web')
            assert User(self.user_1.uid).is_hancom_enabled()
            self.__office_action_data(parsed_url)
            assert User(self.user_1.uid).is_hancom_enabled()

    def test_editing_disk_file_does_not_disable_hancom(self):
        with mock.patch('mpfs.core.user.standart.OFFICE_HANCOM_USER_PERCENTAGE', 100), \
             mock.patch('mpfs.core.user.standart.FEATURE_TOGGLES_REGISTER_HANCOM_USERS', True):
            self.json_ok('user_init', {'uid': self.user_1.uid})
            self.upload_file(self.user_1.uid, self.FILE_PATH, file_data={'size': 1024})
            self._office_action_data_edit(self.user_1.uid, self.FILE_PATH)
            assert User(self.user_1.uid).is_hancom_enabled()

    def test_action_data_disables_hancom_for_particular_locales(self):
        file_path = '/disk/1.doc'
        self.upload_file(self.uid, file_path)
        for tld in ['il', 'am', 'ge', 'tr']:
            UserDAO().enable_hancom(self.uid)
            self.json_ok(
                'office_action_data',
                {'uid': self.uid, 'action': 'edit', 'service_id': 'disk', 'service_file_id': file_path, 'tld': tld}
            )
            UserIndexCollection.reset()
            assert not User(self.uid).is_hancom_enabled()

    def __office_action_check(self, action='browser'):
        first_req_params = {
            'uid': self.user_1.uid,
            'action': 'edit',
            'service_id': action,
            'file_name': 'test-file-name.docx',
            'size': 12345,
            'source': 'dv',
        }
        if action == 'browser':
            first_req_params['service_file_id'] = 'mds/key'
        elif action == 'web':
            first_req_params['service_file_id'] = 'http://yandex.net/doc.doc'
        else:
            raise NotImplemented()
        resp = self.json_ok('office_action_check', first_req_params, headers={'cookie': 'yandexuid=12345'})
        parsed_url = urlparse.urlparse(resp['office_online_url'])
        return parsed_url

    def __office_action_data(self, parsed_url):
        action, service_id, service_file_id = parsed_url.path.strip('/').split('/', 3)
        qs_params = urlparse.parse_qs(parsed_url.query)

        service_file_id = urllib2.unquote(service_file_id)
        if action == 'browser':
            service_file_id, signature = DocviewerYabrowserUrlService().parse_yabrowser_url(service_file_id)
        second_req_params = {
            'uid': self.user_1.uid,
            'action': action,
            'service_id': service_id,
            'service_file_id': service_file_id,
        }
        for name, values in qs_params.iteritems():
            second_req_params[name] = values[0]

        with patch.object(office.actions.EditAction, '_create_browser_to_disk_operation',
                          return_value={'oid': '1234'}):
            self.json_ok('office_action_data', second_req_params)


class OnlyOfficeTestCase(OfficeTestCase):
    class OnlyOfficeSettingsMock(BaseStub):
        def __init__(self, office_only_office_enabled_for_yandex_nets=True, get_only_office_enabled=True, orchestrator_service=True):
            return_value = {'container': 'container'}
            self.mocks = [
                mock.patch('mpfs.core.user.common.CommonUser.get_online_editor', return_value='only_office'),
                mock.patch('mpfs.core.office.logic.only_office.OFFICE_ONLY_OFFICE_ENABLED', True),
                mock.patch('mpfs.core.office.logic.only_office.OFFICE_ONLY_OFFICE_ENABLED_FOR_YANDEX_NETS',
                           office_only_office_enabled_for_yandex_nets),
                mock.patch('mpfs.core.office.logic.only_office.OFFICE_ONLY_OFFICE_INBOX_SECRET', 'inbox_secret'),
                mock.patch('mpfs.core.office.interface.OFFICE_ONLY_OFFICE_OUTBOX_SECRET', 'outbox_secret')
            ]
            if orchestrator_service:
                self.mocks.extend([
                    mock.patch('mpfs.core.services.orchestrator_service.orchestrator_service.create_session',
                               return_value=return_value),
                    mock.patch('mpfs.core.services.orchestrator_service.orchestrator_service.get_session',
                               return_value=return_value),
                    mock.patch('mpfs.core.services.orchestrator_service.orchestrator_service.delete_session'),
                    mock.patch('mpfs.core.services.orchestrator_service.orchestrator_service.refresh_session'),
                    mock.patch('mpfs.core.services.orchestrator_service.orchestrator_service.get_sessions',
                               return_value=[return_value]),
                ])
            if get_only_office_enabled is not None:
                self.mocks.append(mock.patch('mpfs.core.user.common.CommonUser.get_only_office_enabled',
                                             return_value=get_only_office_enabled))

        def start(self):
            for m in self.mocks:
                m.start()

        def stop(self):
            for m in self.mocks:
                m.stop()

    USERS_PER_FILE_LIMIT_FIELD = 'mpfs.core.office.logic.only_office.OFFICE_ONLY_USERS_PER_FILE_LIMIT'
    SHARED_FILE_LIMIT_FIELD = 'mpfs.core.office.logic.only_office.OFFICE_ONLY_SHARED_FILES_PER_USER_LIMIT'
    PRIVATE_FILE_LIMIT_FIELD = 'mpfs.core.office.logic.only_office.OFFICE_ONLY_FILES_PER_USER_LIMIT'
    OPERATION_LIMIT_TIMEOUT_FIELD = 'mpfs.core.office.logic.only_office.OFFICE_ONLY_OFFICE_OPERATION_STALE_TIMEOUT'


    @staticmethod
    def _get_oo_query(office_action_data):
        callback_url = office_action_data['editor_config']['editorConfig']['callbackUrl']
        parsed_url = urlparse.urlparse(callback_url)
        query = urlparse.parse_qs(parsed_url.query)
        for k, v in query.iteritems():
            query[k] = v[0]
        key = parsed_url.path.split('/')[-1]
        query['oo_key'] = key
        return query

    def _office_only_office_callback(self, status, query=None, office_action_data=None, url=None, users=[],
                                     error_status=None, error_code=None,):
        if not query and not office_action_data:
            raise ValueError()
        if not query:
            query = self._get_oo_query(office_action_data)
        params = {'status': status, 'key': query['oo_key'], 'users': users}
        if url:
            params['url'] = url

        body = {'token': jwt.encode(params, 'outbox_secret')}
        if error_status or error_code:
            return self.json_error('office_only_office_callback', query, json=body, status=error_status)
        return self.json_ok('office_only_office_callback', query, json=body)

    @only_office_settings_mock
    def test_only_office_for_beta(self):
        file_path = '/disk/1.docx'
        self.upload_file(self.uid, file_path)
        resp = self.json_ok('office_action_check', {'uid': self.uid,
                                                    'action': 'edit',
                                                    'service_id': 'disk',
                                                    'service_file_id': file_path})
        assert resp['office_online_editor_type'] == 'only_office'

    @only_office_settings_mock(orchestrator_service=False)
    def test_only_office_orchestrator(self):
        with enable_experiment_for_uid('only_office_orchestrator', self.uid), \
             mock.patch('mpfs.core.services.orchestrator_service.OrchestratorService.request') as request:
            container = 'host.yp-c.yandex.net:port'
            request.return_value.json = mock.Mock(return_value={'container': container})
            file_path = '/disk/1.docx'
            self.upload_file(self.uid, file_path)
            user = User(self.uid)
            with mock.patch('mpfs.core.services.directory_service.DirectoryService.get_organizations_by_uids',
                            return_value={self.uid: {'id': user.b2b_key}}):
                resp = self._office_action_data_edit(self.uid, file_path)
            assert resp['office_online_editor_type'] == 'only_office'
            assert request.called
            args = request.call_args[0]
            actual_method = args[0]
            assert actual_method == 'PUT'
            assert args[1].startswith('/v1/session/%s:' % self.uid)
            assert args[2]['group_id'] == (user.b2b_key or '')
            salted_container = container + 'orchestrator_secret'
            sign = hashlib.md5(salted_container).hexdigest()
            domain = '%s_%s' % ('host_port', sign)
            assert 'https://%s.onlyoffice.dst.yandex.net' % domain == resp['balancer_url']

    @mock.patch('mpfs.core.user.common.CommonUser.get_only_office_enabled', return_value=True)
    @mock.patch('mpfs.core.office.logic.only_office.OFFICE_ONLY_OFFICE_ENABLED', True)
    @mock.patch('mpfs.core.office.logic.only_office.OFFICE_ONLY_OFFICE_INBOX_SECRET', 'inbox_secret')
    @mock.patch('mpfs.core.office.interface.OFFICE_ONLY_OFFICE_OUTBOX_SECRET', 'outbox_secret')
    def test_only_office_drop_users(self, _):
        with enable_experiment_for_uid('only_office_orchestrator', self.uid), \
             mock.patch('mpfs.core.services.orchestrator_service.OrchestratorService.request') as request, \
             mock.patch('mpfs.core.office.interface.is_resource_shared', return_value=False), \
             mock.patch('mpfs.core.services.only_office_balancer_service.only_office_balancer_service.request') as balancer_service:
            container = 'host.yp-c.yandex.net:port'
            request.return_value.json = mock.Mock(return_value={'container': container})
            file_path = '/disk/1.docx'
            self.upload_file(self.uid, file_path)
            resp = self.json_ok('office_action_data', {'uid': self.uid,
                                                       'action': 'edit',
                                                       'service_id': 'disk',
                                                       'service_file_id': file_path})
            salted_container = container + 'orchestrator_secret'
            sign = hashlib.md5(salted_container).hexdigest()
            domain = '%s_%s' % ('host_port', sign)
            assert 'https://%s.onlyoffice.dst.yandex.net' % domain == resp['balancer_url']
            callback_url = resp['editor_config']['editorConfig']['callbackUrl']
            parsed_url = urlparse.urlparse(callback_url)
            query = urlparse.parse_qs(parsed_url.query)
            for k, v in query.iteritems():
                query[k] = v[0]
            key = parsed_url.path.split('/')[-1]
            query['oo_key'] = key
            body = {
                'token': jwt.encode({'status': 1, 'key': query['oo_key'], 'users': ['123', self.uid]}, 'outbox_secret'),

            }
            resp = self.json_ok('office_only_office_callback', query, json=body)
            assert resp['error'] == 0
            assert balancer_service.called
            assert balancer_service.call_args[0][0] == 'POST'
            assert balancer_service.call_args[0][1] == '/coauthoring/CommandService.ashx'

    @only_office_settings_mock
    def test_only_office_action_check(self):
        file_path = '/disk/1.docx'
        self.upload_file(self.uid, file_path)
        resp = self.json_ok('office_action_check', {'uid': self.uid,
                                                    'action': 'edit',
                                                    'service_id': 'disk',
                                                    'service_file_id': file_path})
        assert resp
        assert resp['office_online_editor_type'] == 'only_office'
        assert resp['office_online_url']
        from mpfs.core.office.logic import only_office
        only_office.OFFICE_ONLY_OFFICE_ENABLED = False
        resp = self.json_ok('office_action_check', {'uid': self.uid,
                                                    'action': 'edit',
                                                    'service_id': 'disk',
                                                    'service_file_id': file_path})
        assert resp
        assert resp['office_online_editor_type'] != 'only_office'
        assert resp['office_online_url']

    @only_office_settings_mock
    def test_only_office_action_data_overdraft(self):
        from mpfs.core.user.common import CommonUser
        old_is_in_hard_overdraft = getattr(CommonUser, 'is_in_hard_overdraft', None)
        try:
            CommonUser.is_in_hard_overdraft = lambda *_, **__: True
            with enable_experiment_for_uid('new_overdraft_strategy', self.uid), \
                    mock.patch('mpfs.core.services.passport_service.Passport.userinfo',
                               return_value={'country': 'ru', 'email': 'email@example.com', 'language': 'ru'}):
                file_path = '/disk/1.docx'
                self.upload_file(self.uid, file_path, file_data={'size': 50})
                USER_OVERDRAFT_RESTRICTIONS_THRESHOLD = settings.user['overdraft']['restrictions_threshold']
                resp = self.billing_ok('service_create', {'uid': self.uid,
                                                          'line': 'partner',
                                                          'pid': 'yandex_disk_for_business',
                                                          'ip': '127.0.0.1',
                                                          'product.amount': USER_OVERDRAFT_RESTRICTIONS_THRESHOLD * 2})
                self.upload_file(self.uid, '/disk/1.bin',
                                 file_data={'size': USER_OVERDRAFT_RESTRICTIONS_THRESHOLD + 1024 ** 3 * 10 + 1})
                self.billing_ok('service_delete', {'uid': self.uid, 'sid': resp['sid'], 'disable': True,
                                                   'ip': '127.0.0.1', 'send_email': 0})
                resp = self.json_error('office_action_data', {'uid': self.uid,
                                                              'action': 'edit',
                                                              'service_id': 'disk',
                                                              'service_file_id': file_path})
                assert resp['code'] == 279
                assert resp['title'] == 'Overdraft user public link not allowed'
        finally:
            if old_is_in_hard_overdraft:
                CommonUser.is_in_hard_overdraft = old_is_in_hard_overdraft
            else:
                del CommonUser.is_in_hard_overdraft



    @parameterized.expand([
        ('ru_docx', 'ru', 'docx', 'https://yastatic.net/s3/editor/_/editor_docs_icon_ru_v1.svg',
         'https://docs.yandex.ru/docs?from=editor&type=docx'),
        ('en_docx', 'en', 'docx', 'https://yastatic.net/s3/editor/_/editor_docs_icon_en_v1.svg',
         'https://docs.yandex.ru/docs?from=editor&type=docx'),
        ('ru_xlsx', 'ru', 'xlsx', 'https://yastatic.net/s3/editor/_/editor_docs_icon_ru_v1.svg',
         'https://docs.yandex.ru/docs?from=editor&type=xlsx'),
    ])
    @mock.patch('mpfs.core.office.interface.OFFICE_ONLY_OFFICE_OUTBOX_SECRET', 'outbox_secret')
    @mock.patch('mpfs.core.services.orchestrator_service.orchestrator_service.create_session',
               return_value={'container': 'container'})
    @mock.patch('mpfs.core.services.orchestrator_service.orchestrator_service.get_session',
               return_value={'container': 'container'})
    @mock.patch('mpfs.core.services.orchestrator_service.orchestrator_service.delete_session')
    @mock.patch('mpfs.core.services.orchestrator_service.orchestrator_service.refresh_session')
    @mock.patch('mpfs.core.services.orchestrator_service.orchestrator_service.get_sessions',
                return_value=[{'container': 'container'},])
    def test_only_office_action_data(self, case_name, locale, ext, expected_logo_image, expected_logo_url, *_):
        u"""Проверяет что конфиг формируется  правильный"""
        self.json_ok('office_set_selection_strategy', {'uid': self.uid,
                                                       'selection_strategy': 'force_oo'})
        from mpfs.core.office.logic import only_office
        from mpfs.core.office.logic import only_office_utils
        rest_api_host = 'rest_api_host'
        local_proxy = 'local_proxy'
        only_office.OFFICE_ONLY_OFFICE_REST_API_HOST = rest_api_host
        only_office_utils.OFFICE_ONLY_OFFICE_LOCAL_PROXY = local_proxy
        customization = {
                    "autosave": True,
                    "chat": False,  # отключен чат
                    "commentAuthorOnly": False,
                    "compactToolbar": False,
                    "feedback": {
                        "url": 'https://forms.yandex.ru/surveys/8167/',  #ссылка на нашу поддержку
                        "visible": True
                    },
                    "forcesave": True,
                    "help": True,
                    "showReviewChanges": False,
                    "zoom": 100,
                    "loaderName": u"Яндекс.Диск",
                    "logo": {
                        "image": expected_logo_image,
                        "imageEmbedded": expected_logo_image,
                        "url": expected_logo_url
                    },
                }

        docx_file_path = '/disk/1.%s' % ext
        self.upload_file(self.uid, docx_file_path, file_data={'size': 123})

        with mock.patch('mpfs.core.office.logic.only_office.OFFICE_ONLY_OFFICE_ENABLED', True):
            resp = self._office_action_data_edit( self.uid, docx_file_path, locale=locale)

        assert resp['office_online_editor_type'] == 'only_office'
        assert 'host=' + rest_api_host in resp['editor_config']['editorConfig']['callbackUrl']
        assert local_proxy in resp['editor_config']['editorConfig']['callbackUrl']
        assert local_proxy in resp['editor_config']['document']['url']
        assert resp['editor_config']['document']['fileType'] == ext
        assert resp['editor_config']['document']['permissions']['edit'] == True
        assert resp['editor_config']['document']['title'] == u'1.%s' % ext
        assert resp['editor_config']['editorConfig']['user']['id'] == self.uid
        assert resp['editor_config']['editorConfig']['lang'] == locale
        assert resp['editor_config']['editorConfig']['mode'] == 'edit'
        assert resp['editor_config']['editorConfig']['customization'] == customization

    @only_office_settings_mock
    def test_only_office_action_data_return_same_operation(self):
        """
        Проверяпт что возвращается та же самая операция. новая не создаётся
        """
        docx_file_path = '/disk/1.docx'
        self.json_ok('user_init', {'uid': self.uid})
        self.upload_file(self.uid, docx_file_path)
        mode = 'edit'
        resp = self._office_action_data_edit( self.uid, docx_file_path)
        old_key = resp['editor_config']['document']['key']
        operation_id = manager.get_operation(*base64.b64decode(old_key).split(':', 1)).id

        resp = self._office_action_data_edit( self.uid, docx_file_path)
        assert old_key == resp['editor_config']['document']['key']
        assert manager.get_operation(*base64.b64decode(resp['editor_config']['document']['key']).split(':', 1)).id == operation_id

    @only_office_settings_mock
    @mock.patch(USERS_PER_FILE_LIMIT_FIELD, 1)
    def test_only_office_action_document_editors_limit_error_response(self):
        from mpfs.core.office.logic.only_office import OnlyOfficeEditor
        file_folder = '/disk/shared'
        file_path = '%s/%s' % (file_folder, 'Document.xlsx')
        owner_uid = self.OTHER_UID
        guest_uid = self.uid

        self._upload_shared_file(owner_uid, file_folder, file_path, [guest_uid], file_data={'size': 77})
        resource = get_resource(self.uid, file_path)

        resp = self._office_action_data_edit(owner_uid, file_path)
        self._office_only_office_callback(office_action_data=resp, status=1, users=[owner_uid])
        response = self._office_action_data_edit(guest_uid, file_path,
                                                 error_status=413,
                                                 error_code=codes.OFFICE_ONLY_OFFICE_USER_LIMIT_REACHED)
        assert 'short_url' in response['data']
        assert 'public_hash' in response['data']
        assert response['title'] == 'Too many users are editing the document'
        assert response['data']['short_url'] == resource.meta['short_url']
        assert response['data']['public_hash'] == resource.meta['public_hash']
        assert response['data']['name'] == resource.name
        assert response['data']['documentType'] == OnlyOfficeEditor.spreadsheet_type

    @only_office_settings_mock
    @mock.patch(USERS_PER_FILE_LIMIT_FIELD, 2)
    def test_only_office_action_document_editors_limit_up_and_down(self):
        file_folder = '/disk/shared'
        file_path = '%s/%s' % (file_folder, 'Document.docx')
        owner_uid = self.uid
        guest1_uid = self.user_1.uid
        guest2_uid = self.user_2.uid
        self.json_ok('user_init', {'uid': guest1_uid})
        self.json_ok('user_init', {'uid': guest2_uid})

        self._upload_shared_file(owner_uid, file_folder, file_path, [guest1_uid, guest2_uid])

        resp = self._office_action_data_edit(owner_uid, file_path)
        self._office_only_office_callback(office_action_data=resp, status=1, users=[owner_uid])
        self._office_action_data_edit(guest1_uid, file_path)
        self._office_only_office_callback(office_action_data=resp, status=1, users=[owner_uid, guest1_uid])

        self._office_action_data_edit(guest2_uid, file_path,
                                      error_status=413, error_code=codes.OFFICE_ONLY_OFFICE_USER_LIMIT_REACHED)
        self._office_only_office_callback(office_action_data=resp, status=1, users=[guest1_uid])
        self._office_action_data_edit(guest2_uid, file_path)

    @only_office_settings_mock
    @mock.patch(USERS_PER_FILE_LIMIT_FIELD, 1)
    def test_only_office_action_document_editors_no_limit_for_owner(self):
        file_folder = '/disk/shared'
        file_path = '%s/%s' % (file_folder, 'Document.docx')
        owner_uid = self.OTHER_UID
        guest_uid = self.uid

        self._upload_shared_file(owner_uid, file_folder, file_path, [guest_uid])

        resp = self._office_action_data_edit(guest_uid, file_path)
        self._office_only_office_callback(office_action_data=resp, status=1, users=[guest_uid])
        self._office_action_data_edit(owner_uid, file_path)

    @parameterized.expand([
        ('rtf', 'docx'),
        ('csv', 'xlsx'),
        ('fodp', 'pptx')
    ])
    def test_only_office_action_data_office_online_url(self, wrong_type, good_type):
        u"""
        Проверяет наличие office_online_url только у поддерживаемых типов
        :return:
        """
        good_file_path = '/disk/1.%s' % good_type
        self.upload_file(self.uid, good_file_path, file_data={'size': 50})
        wrong_file_path = '/disk/1.%s' % wrong_type
        self.upload_file(self.uid, wrong_file_path, file_data={'size': 50})

        resp = self.json_ok('list', {'uid': self.uid, 'path': good_file_path, 'meta': ''})
        assert resp['meta']['office_online_url']
        resp = self.json_ok('info', {'uid': self.uid, 'path': good_file_path, 'meta': ''})
        assert resp['meta']['office_online_url']

        resp = self.json_ok('list', {'uid': self.uid, 'path': wrong_file_path, 'meta': ''})
        assert not resp['meta'].get('office_online_url')
        resp = self.json_ok('info', {'uid': self.uid, 'path': wrong_file_path, 'meta': ''})
        assert not resp['meta'].get('office_online_url')

    @parameterized.expand([
        ('log', 'docx', 'text'),
        ('tskv', 'xlsx', 'spreadsheet'),
        ('bmp', 'pptx', 'presentation')
    ])
    @only_office_settings_mock
    def test_only_office_acction_data_wrong_types(self, wrong_type, good_type, doc_type):
        u"""
        проверяет что у правильного типа правильный documentType а у неправильного типа возвращается ошибка
        :param wrong_type:
        :param good_type:
        :param doc_type:
        :return:
        """
        good_file_path = '/disk/1.%s' % good_type
        self.upload_file(self.uid, good_file_path, file_data={'size': 50})
        wrong_file_path = '/disk/1.%s' % wrong_type
        self.upload_file(self.uid, wrong_file_path, file_data={'size': 50})
        resp = self._office_action_data_edit( self.uid, good_file_path)
        assert resp['editor_config']['documentType'] == doc_type

        resp = self._office_action_data_edit( self.uid, wrong_file_path)
        assert resp['code'] == 169
        assert resp['title'] == 'unsupported file extension'

    @only_office_settings_mock(office_only_office_enabled_for_yandex_nets=False)
    def test_only_office_callback_with_orchestrator(self):
        with enable_experiment_for_uid('only_office_orchestrator', self.uid), \
             mock.patch('mpfs.core.services.orchestrator_service.OrchestratorService.request') as request:
            request.return_value.json = mock.Mock(return_value={'container': 'host:port'})
            file_path = '/disk/1.docx'
            self.upload_file(self.uid, file_path, file_data={'size': 50})

            resp = self._office_action_data_edit(self.uid, file_path, locale='ru', ext='docx')
            query = self._get_oo_query(resp)
            key = query['oo_key']
            resp = self._office_only_office_callback(query=query, status=1)
            operations = manager.get_active_operations(self.uid)
            only_office_operation = operations[0]
            assert only_office_operation['type'] == 'only_office'
            assert only_office_operation['data'].get('subdomain')

            assert resp == {'error': 0}
            self._office_only_office_callback(query=query, status=6, url='http://storage/file.docx')
            operations = manager.get_active_operations(self.uid)
            external_copy_operation = next(o for o in operations if o['id'] != only_office_operation['id'])
            assert external_copy_operation['type'] == 'external_copy'
            assert external_copy_operation['subtype'] == 'only_office'
            assert 'subdomain' in external_copy_operation['data'].get('service_file_id', '')
            self._office_only_office_callback(query=query, status=2, url='http://storage/file.docx')
            resp = self.json_ok('status', {'uid': self.uid, 'oid': base64.b64decode(key).split(':', 1)[-1]})
            assert resp['state'] == 'COMPLETED'

    @mock.patch('mpfs.core.office.logic.only_office.OFFICE_ONLY_OFFICE_ENABLED', True)
    def test_new_session_when_container_unavailable(self):
        self.json_ok('office_set_selection_strategy', {'uid': self.uid,
                                                       'selection_strategy': 'force_oo'})
        file_path = '/disk/1.docx'
        self.upload_file(self.uid, file_path)

        with enable_experiment_for_uid('only_office_orchestrator', self.uid), \
             mock.patch('mpfs.core.services.orchestrator_service.OrchestratorService.create_session',
                        return_value={'container': 'host:port'}):

            resp = self._office_action_data_edit(self.uid, file_path, locale='ru', ext='docx')
            first_session_key = resp['editor_config']['document']['key']

        with mock.patch('mpfs.core.services.only_office_balancer_service.OnlyOfficeBalancerService.info',
                        return_value=construct_requests_resp(status=502, content='nginx said 502 - bad luck')), \
             mock.patch('mpfs.core.services.orchestrator_service.OrchestratorService.get_session',
                        side_effect=OrchestratorNotFoundError()):
            resp = self.json_ok('office_action_data', {'uid': self.uid,
                                                       'action': 'edit',
                                                       'service_id': 'disk',
                                                       'service_file_id': file_path})
            second_session_key = resp['editor_config']['document']['key']

        assert first_session_key != second_session_key

    @mock.patch('mpfs.core.office.logic.only_office.OFFICE_ONLY_OFFICE_ENABLED', True)
    def test_new_session_when_container_check_faield_but_container_ok(self):
        self.json_ok('office_set_selection_strategy', {'uid': self.uid,
                                                       'selection_strategy': 'force_oo'})
        file_path = '/disk/1.docx'
        self.upload_file(self.uid, file_path)

        with enable_experiment_for_uid('only_office_orchestrator', self.uid), \
             mock.patch('mpfs.core.services.orchestrator_service.OrchestratorService.create_session',
                        return_value={'container': 'host:port'}):

            resp = self._office_action_data_edit(self.uid, file_path, locale='ru', ext='docx')
            first_session_key = resp['editor_config']['document']['key']

        with mock.patch('mpfs.core.services.only_office_balancer_service.OnlyOfficeBalancerService.info',
                        return_value=construct_requests_resp(status=502, content='nginx said 502 - bad luck')), \
             mock.patch('mpfs.core.services.orchestrator_service.OrchestratorService.get_session',
                        return_value={'status': 'norm'}):
            resp = self.json_ok('office_action_data', {'uid': self.uid,
                                                       'action': 'edit',
                                                       'service_id': 'disk',
                                                       'service_file_id': file_path})
            second_session_key = resp['editor_config']['document']['key']

        assert first_session_key == second_session_key

    @only_office_settings_mock(office_only_office_enabled_for_yandex_nets=False)
    def test_only_office_callback_without_orchestrator(self):
            file_path = '/disk/1.docx'
            self.upload_file(self.uid, file_path, file_data={'size': 50})

            resp = self._office_action_data_edit(self.uid, file_path, locale='ru', ext='docx', with_orchestrator=False)
            query = self._get_oo_query(resp)
            key=query['oo_key']
            resp = self._office_only_office_callback(query=query, status=1)
            assert resp == {'error': 0}
            self._office_only_office_callback(query=query, status=6, url='http://storage/file.docx')
            operations = manager.get_active_operations(self.uid)

            assert operations[1]['type'] == 'external_copy' or operations[0]['type'] == 'external_copy'
            assert operations[1]['subtype'] == 'only_office' or operations[0]['subtype'] == 'only_office'
            assert 'subdomain' not in operations[1]['data'].get('service_file_id', '') and \
                   'subdomain' not in operations[0]['data'].get('service_file_id', '')

            self._office_only_office_callback(query=query, status=2, url='http://storage/file.docx')
            resp = self.json_ok('status', {'uid': self.uid, 'oid': base64.b64decode(key).split(':', 1)[-1]})
            assert resp['state'] == 'COMPLETED'
            assert not (operations[1]['data'].get('subdomain') or operations[0]['data'].get('subdomain'))

    @mock.patch('mpfs.core.office.interface._only_office_save_file')
    @only_office_settings_mock(office_only_office_enabled_for_yandex_nets=False)
    def test_only_office_lock(self, _only_office_save_file):
        file_path = '/disk/1.docx'
        self.upload_file(self.uid, file_path, file_data={'size': 50})

        resp = self._office_action_data_edit(self.uid, file_path, locale='ru', ext='docx')
        self._office_only_office_callback(office_action_data=resp, status=1)
        lock = LockHelper.get_lock('%s:%s' % (self.uid, file_path))
        assert lock

    @mock.patch('mpfs.core.office.interface._only_office_save_file')
    @only_office_settings_mock(office_only_office_enabled_for_yandex_nets=False)
    def test_only_office_unlock(self, _only_office_save_file):
        file_path = '/disk/1.docx'
        self.upload_file(self.uid, file_path, file_data={'size': 50})

        resp = self._office_action_data_edit(self.uid, file_path, locale='ru', ext='docx')
        self._office_only_office_callback(office_action_data=resp, status=2)
        lock = LockHelper.get_lock('%s:%s' % (self.uid, file_path))
        assert not lock

    @only_office_settings_mock(office_only_office_enabled_for_yandex_nets=False)
    def test_only_office_unlock_session(self):
        with enable_experiment_for_uid('only_office_orchestrator', self.uid), \
                mock.patch('mpfs.core.services.orchestrator_service.OrchestratorService.request') as request:
            request.return_value.json = mock.Mock(return_value={'container': 'host:port'})
            _create_operation_results = []
            old_create_operation = manager.create_operation

            def _create_operation(*args, **kwargs):
                result = old_create_operation(*args, **kwargs)
                _create_operation_results.append(result)
                return result

            file_path = '/disk/1.docx'
            self.upload_file(self.uid, file_path, file_data={'size': 50})

            resp = self._office_action_data_edit(self.uid, file_path, locale='ru', ext='docx')
            with patch.object(manager, 'create_operation', side_effect=_create_operation):
                self._office_only_office_callback(office_action_data=resp, status=2, url='http://storage/file.docx')
            lock = LockHelper.get_lock('%s:%s' % (self.uid, file_path))
            assert not lock
            with open('fixtures/xml/kladun/extract-file-from-archive/commitFinal.xml') as f:
                commit_final_xml_data = f.read()
            body_1_xml = open('fixtures/xml/kladun/upload-from-service/commitFileInfo.xml').read()
            body_1 = etree.fromstring(body_1_xml)
            body_2_xml = open('fixtures/xml/kladun/upload-from-service/commitFileUpload.xml').read()
            body_2 = etree.fromstring(body_2_xml)
            body_3_xml = open('fixtures/xml/kladun/upload-from-service/commitFinal.xml').read()
            body_3 = etree.fromstring(body_3_xml)

            with KladunStub(status_values=(body_1, body_2, body_3)), \
                mock.patch('mpfs.core.services.orchestrator_service.orchestrator_service.delete_session') as delete_session:
                oid = _create_operation_results[-1].id
                opts = {
                    'uid': self.uid,
                    'oid': oid,
                    'status_xml': body_1_xml,
                    'type': COMMIT_FILE_INFO,
                }
                self.service_ok('kladun_callback', opts)
                opts = {
                    'uid': self.uid,
                    'oid': oid,
                    'status_xml': body_2_xml,
                    'type': COMMIT_FILE_UPLOAD,
                }
                self.service_ok('kladun_callback', opts)
                opts = {
                    'uid': self.uid,
                    'oid': oid,
                    'status_xml': body_3_xml,
                    'type': COMMIT_FINAL,
                }
                self.service_ok('kladun_callback', opts)
                assert delete_session.called

    @mock.patch('mpfs.core.office.interface._only_office_save_file')
    @only_office_settings_mock(office_only_office_enabled_for_yandex_nets=False)
    def test_only_office_wrong_lock(self, _only_office_save_file):
        from mpfs.core.bus import Bus
        from mpfs.core.office.interface import _lock
        file_path = '/disk/1.docx'
        self.upload_file(self.uid, file_path, file_data={'size': 50})
        fs = Bus()
        address = Address.Make(self.uid, file_path)
        resource = fs.get_resource(self.uid, address, unzip_file_id=True)
        try:
            fs.set_lock(resource, data={'office_online_editor_type': 'wrong_type'}, time_offset=1000)
            resp = self._office_action_data_edit(self.uid, file_path, locale='ru', ext='docx')
            assert resp['editor_config']['editorConfig']['mode'] == 'view'
        finally:
            fs.unset_lock(resource)

    @mock.patch('mpfs.core.office.interface._only_office_save_file')
    @only_office_settings_mock(office_only_office_enabled_for_yandex_nets=False)
    def test_only_office_wrong_lock_after_action_data(self, _only_office_save_file):
        from mpfs.core.bus import Bus
        from mpfs.core.office.interface import _lock
        file_path = '/disk/1.docx'
        self.upload_file(self.uid, file_path, file_data={'size': 50})
        fs = Bus()

        address = Address.Make(self.uid, file_path)
        resource = fs.get_resource(self.uid, address, unzip_file_id=True)
        try:
            resp = self._office_action_data_edit(self.uid, file_path, locale='ru', ext='docx')

            _lock(fs, resource, {'office_online_editor_type': 'wrong_type'})
            self._office_only_office_callback(office_action_data=resp, status=2, error_status=423)
        finally:
            fs.unset_lock(resource)

    @only_office_settings_mock(office_only_office_enabled_for_yandex_nets=False)
    def test_only_office_close_with_no_changes(self):

        file_path = '/disk/1.docx'
        self.upload_file(self.uid, file_path, file_data={'size': 50})
        resp = self._office_action_data_edit(self.uid, file_path, locale='ru')
        query = self._get_oo_query(resp)
        key = query['oo_key']
        resp = self._office_only_office_callback(query=query, status=1)
        assert resp == {'error': 0}

        self._office_only_office_callback(query=query, status=4, url='http://storage/file.docx')
        # на status=4 не закрываем сессию
        resp = self.json_ok('status', {'uid': self.uid, 'oid': base64.b64decode(key).split(':', 1)[-1]})
        assert resp['state'] == 'DONE'

    @parameterized.expand([
        ('rtf',),
        ('doc',),
        ('xls',),
        ('ppt',),
        ('odt',),
        ('ods',),
        ('odp',),
    ])
    @only_office_settings_mock(office_only_office_enabled_for_yandex_nets=False)
    def test_only_office_convertation(self, ext):

        file_path = '/disk/1.%s' % ext
        self.upload_file(self.uid, file_path, file_data={'size': 50})
        resp = self.json_ok('office_action_check', {'uid': self.uid,
                                                    'action': 'edit',
                                                    'service_id': 'disk',
                                                    'service_file_id': file_path,
                                                    'locale': 'ru'})
        assert resp['office_online_editor_type'] == 'only_office'
        resp = self._office_action_data_edit(self.uid, file_path, locale='ru')
        assert resp['oid']
        resp = self.json_ok('status', {'uid': self.uid, 'oid': resp['oid']})
        assert resp['type'] == 'office'

    @only_office_settings_mock(office_only_office_enabled_for_yandex_nets=False)
    def test_only_office_key_mismatch(self):
        """
        Проверяет, что нельзя прийти с левым key
        """

        file_path = '/disk/1.docx'
        self.upload_file(self.uid, file_path, file_data={'size': 50})
        resp = self._office_action_data_edit(self.uid, file_path, locale='ru')
        query=self._get_oo_query(resp)
        decoded_token = OnlyOfficeToken.decode(query['token'])
        decoded_token['key'] = 'wrong_key'
        query['token'] = OnlyOfficeToken.encode(decoded_token, ONLY_OFFICE_MPFS_TOKEN)
        self._office_only_office_callback(query=query, status=1, error_status=403)

    @only_office_settings_mock(office_only_office_enabled_for_yandex_nets=False)
    def test_jwt_codding(self):
        file_path = '/disk/1.docx'
        self.upload_file(self.uid, file_path, file_data={'size': 50})
        resp = self._office_action_data_edit(self.uid, file_path, locale='ru')
        query = self._get_oo_query(resp)
        decoded_token = OnlyOfficeToken.decode(query['token'])
        old_token = query['token']
        query['token'] = OnlyOfficeToken.encode(decoded_token, 'wrong_secret')
        self._office_only_office_callback(query=query, status=1, error_status=403)
        query['token'] = old_token
        self._office_only_office_callback(query=query, status=1)

    def test_handle_office_set_editor_type(self):
        # None - дефолтное значение
        resp = self.json_ok('user_info', {'uid': self.uid})
        assert resp['office_online_editor_type'] is None

        resp = self.json_ok('office_set_editor_type', {'uid': self.uid, 'office_online_editor_type': 'only_office'})
        assert resp['office_online_editor_type'] == 'only_office'
        resp = self.json_ok('user_info', {'uid': self.uid})
        assert resp['office_online_editor_type'] == 'only_office'

        resp = self.json_ok('office_set_editor_type', {'uid': self.uid, 'office_online_editor_type': 'microsoft_online'})
        assert resp['office_online_editor_type'] == 'microsoft_online'
        resp = self.json_ok('user_info', {'uid': self.uid})
        assert resp['office_online_editor_type'] == 'microsoft_online'

    def test_handle_office_set_editor_type_unknown_type(self):
        self.json_error('office_set_editor_type', {'uid': self.uid, 'office_online_editor_type': 'unknown_type'},
                        status=400)

    @only_office_settings_mock(office_only_office_enabled_for_yandex_nets=False)
    def test_only_office_config(self):
        from mpfs.core.office.logic import only_office

        only_office.OFFICE_ONLY_OFFICE_ENABLED_FOR_UIDS = []
        file_path = '/disk/1.docx'
        self.upload_file(self.uid, file_path, file_data={'size': 50})
        resp = self._office_action_data_edit(self.uid, file_path, locale='ru')
        editor_config = resp['editor_config']
        assert editor_config['document']['fileType'] == 'docx'
        assert editor_config['document']['key']
        assert editor_config['document']['title'] == '1.docx'
        assert editor_config['document']['info']['folder'] == '/disk'
        created = datetime.strptime(editor_config['document']['info']['created'], "%Y-%m-%d %I:%M %p")
        assert created
        assert editor_config['document']['info']['author'] == 'Vasily P.'
        assert editor_config['documentType'] == 'text'
        assert editor_config['editorConfig']['mode'] == 'edit'
        assert editor_config['editorConfig']['user']['id'] == self.uid

    @mock.patch('mpfs.core.office.interface._only_office_save_file')
    @only_office_settings_mock(office_only_office_enabled_for_yandex_nets=False)
    def test_RO(self, _):
        def raise_check_rw(*args, **kwargs):
            raise GroupNoPermit()
        file_path = '/disk/1.docx'
        self.upload_file(self.uid, file_path, file_data={'size': 50})
        with mock.patch('mpfs.core.filesystem.resources.disk.DiskFolder.check_rw', side_effect = raise_check_rw):
            resp = self._office_action_data_edit(self.uid, file_path, locale='ru', ext='docx')
            assert resp['editor_config']['editorConfig']['mode'] == 'view'
            self._office_only_office_callback(office_action_data=resp, status=2, error_status=403)

    # FILES COUNT LIMIT TESTS

    @only_office_settings_mock
    @mock.patch(PRIVATE_FILE_LIMIT_FIELD, 2)
    def test_only_office_limit_for_open_documents(self):
        """
        Проверяем, лимит на количество редактируемых своих файлов (публичных и приватных)
        """
        private_file_folder = '/disk'
        public_file_folder = '/disk/share'
        file_path_1 = '%s/%s' % (private_file_folder, 'Document1.docx')
        file_path_2 = '%s/%s' % (public_file_folder, 'Document2.docx')
        file_path_3 = '%s/%s' % (private_file_folder, 'Document3.docx')
        file_path_4 = '%s/%s' % (public_file_folder, 'Document4.docx')

        self.upload_file(self.uid, file_path_1)
        self._upload_shared_file(self.uid, public_file_folder, file_path_2)
        self.upload_file(self.uid, file_path_3)
        self._upload_shared_file(self.uid, public_file_folder, file_path_4)

        # открываем успешно один приватный файл
        self._office_action_data_edit(self.uid, file_path_1)
        # открываем успешно один публичный файл
        self._office_action_data_edit(self.uid, file_path_2)

        # еще один приватный файл открыть нельзя
        self._office_action_data_edit(self.uid, file_path_3,
                                      error_status=413,
                                      error_code=codes.OFFICE_ONLY_OFFICE_FILES_LIMIT_REACHED)
        # публичный файл тоже нельзя
        response = self._office_action_data_edit(self.uid, file_path_4,
                                                 error_status=413,
                                                 error_code=codes.OFFICE_ONLY_OFFICE_FILES_LIMIT_REACHED)

    @only_office_settings_mock
    @mock.patch(PRIVATE_FILE_LIMIT_FIELD, 0)
    def test_only_office_limit_for_open_documents_error_response(self):
        """
        Проверяем, лимит на количество редактируемых своих файлов - корректность данных ошибки
        """
        private_file_folder = '/disk'
        public_file_folder = '/disk/share'
        file_path_1 = '%s/%s' % (private_file_folder, 'Document1.docx')
        file_path_2 = '%s/%s' % (public_file_folder, 'Document2.docx')

        self.upload_file(self.uid, file_path_1)
        self._upload_shared_file(self.uid, public_file_folder, file_path_2)

        # приватный файл открыть нельзя - лимиты в 0
        response = self._office_action_data_edit(self.uid, file_path_1,
                                                 error_status=413,
                                                 error_code=codes.OFFICE_ONLY_OFFICE_FILES_LIMIT_REACHED)
        assert 'short_url' not in response['data']  # файл не публичный
        assert 'public_hash' not in response['data']
        assert response['title'] == 'Too many documents are open for editing'

        # публичный файл открыть нельзя - лимиты в 0
        response = self._office_action_data_edit(self.uid, file_path_2,
                                                 error_status=413,
                                                 error_code=codes.OFFICE_ONLY_OFFICE_FILES_LIMIT_REACHED)
        resource = get_resource(self.uid, file_path_2)
        assert 'short_url' in response['data']  # файл публичный
        assert 'public_hash' in response['data']
        assert response['title'] == 'Too many documents are open for editing'
        assert response['data']['short_url'] == resource.meta['short_url']
        assert response['data']['public_hash'] == resource.meta['public_hash']
        assert response['data']['name'] == resource.name

    @only_office_settings_mock
    @mock.patch(SHARED_FILE_LIMIT_FIELD, 0)
    def test_only_office_limit_for_open_public_documents_error_response(self):
        """
        Проверяем, лимит на количество редактируемых публичных файлов - корректность данных ошибки
        """
        public_file_folder = '/disk/share'
        file_path = '%s/%s' % (public_file_folder, 'Document2.docx')
        owner_uid = self.uid
        guest1_uid = self.user_1.uid
        self.json_ok('user_init', {'uid': guest1_uid})

        self._upload_shared_file(owner_uid, public_file_folder, file_path, [guest1_uid])

        # приватный файл открыть нельзя - лимиты в 0
        response = self._office_action_data_edit(guest1_uid, file_path,
                                                 error_status=413,
                                                 error_code=codes.OFFICE_ONLY_OFFICE_PUBLIC_FILES_LIMIT_REACHED)
        resource = get_resource(self.uid, file_path)
        assert 'short_url' in response['data']  # файл публичный
        assert 'public_hash' in response['data']
        assert response['title'] == 'Too many public documents are open for editing'
        assert response['data']['short_url'] == resource.meta['short_url']
        assert response['data']['public_hash'] == resource.meta['public_hash']
        assert response['data']['name'] == resource.name

    @only_office_settings_mock
    @mock.patch(PRIVATE_FILE_LIMIT_FIELD, 1)
    def test_only_office_limit_for_open_documents_operation_status(self):
        """
        Проверяем, что лимит количество редактируемых своих файлов работает для нужных статусов операций
        """
        file_folder = '/disk'
        file_path_1 = '%s/%s' % (file_folder, 'Document1.docx')
        file_path_2 = '%s/%s' % (file_folder, 'Document2.docx')

        self.upload_file(self.uid, file_path_1)
        self.upload_file(self.uid, file_path_2)

        # открываем успешно один приватный файл
        response = self._office_action_data_edit(self.uid, file_path_1)
        # еще один открыть нельзя
        self._office_action_data_edit(self.uid, file_path_2,
                                      error_status=413,
                                      error_code=codes.OFFICE_ONLY_OFFICE_FILES_LIMIT_REACHED)
        # выставлем операции статус executing
        self._office_only_office_callback(office_action_data=response, status=1, users=[self.uid])
        # все еще нельзя открыть
        response = self._office_action_data_edit(self.uid, file_path_2,
                                                 error_status=413,
                                                 error_code=codes.OFFICE_ONLY_OFFICE_FILES_LIMIT_REACHED)

    @only_office_settings_mock
    @mock.patch(PRIVATE_FILE_LIMIT_FIELD, 1)
    def test_only_office_limit_for_open_documents_open_same_file(self):
        """
        Проверяем, что открытие одного и того же файла не влияет на расход лимита
        :return:
        """
        file_folder = '/disk/shared'
        file_path = '%s/%s' % (file_folder, 'Document1.docx')

        self.json_ok('mkdir', {'uid': self.uid, 'path': file_folder})
        self.upload_file(self.uid, file_path)

        # открываем файл
        self._office_action_data_edit(self.uid, file_path)
        # открываем его же - должны успешно открыть, несмотря на лимит в 1
        self._office_action_data_edit(self.uid, file_path)

    @only_office_settings_mock
    @mock.patch(PRIVATE_FILE_LIMIT_FIELD, 1)
    def test_only_office_limit_for_open_documents_up_and_down(self):
        """
        Проверяем лимит на количество открытых приватных файлов.
        Проверяется ошибка при достижении лимита
        Затем проверяется, что закрытие редактируемых файлов приводит к разрешению редактирования новых
        """
        file_folder = '/disk'
        file_path_1 = '%s/%s' % (file_folder, 'Document1.docx')
        file_path_2 = '%s/%s' % (file_folder, 'Document2.docx')

        self.upload_file(self.uid, file_path_1)
        self.upload_file(self.uid, file_path_2)

        # редактируем первый файл
        response = self._office_action_data_edit(self.uid, file_path_1)
        # второй редактировать нельзя
        self._office_action_data_edit(self.uid, file_path_2,
                                      error_status=413,
                                      error_code=codes.OFFICE_ONLY_OFFICE_FILES_LIMIT_REACHED)
        # закрываем первый файл
        with mock.patch('mpfs.core.office.interface._only_office_save_file'):
            self._office_only_office_callback(office_action_data=response, status=2)
        # второй редактировать можно
        self._office_action_data_edit(self.uid, file_path_2)

    @only_office_settings_mock
    @mock.patch(SHARED_FILE_LIMIT_FIELD, 1)
    def test_only_office_limit_for_open_documents_public_file_limit_for_guest(self):
        """
        Проверяем, лимит на количество редактируемых публичных файлов гостями папки
        Гости папки не могут открыть больше, чем OFFICE_ONLY_SHARED_FILES_PER_USER_LIMIT
        """
        file_folder = '/disk/shared'
        file_path_1 = '%s/%s' % (file_folder, 'Document1.docx')
        file_path_2 = '%s/%s' % (file_folder, 'Document2.docx')
        owner_uid = self.uid
        guest1_uid = self.user_1.uid
        guest2_uid = self.user_2.uid
        self.json_ok('user_init', {'uid': guest1_uid})
        self.json_ok('user_init', {'uid': guest2_uid})

        self._upload_shared_file(owner_uid, file_folder, file_path_1, [guest1_uid, guest2_uid])
        self._upload_shared_file(owner_uid, file_folder, file_path_2, [guest2_uid])

        # первый гость редактирует первый файл
        self._office_action_data_edit(guest1_uid, file_path_1)
        # второй гость может редактировать тот же файл - лимит не срабатывает
        self._office_action_data_edit(guest2_uid, file_path_1)
        # но второй файл редакрировать нельзя - уперлись в лимит
        self._office_action_data_edit(guest2_uid, file_path_2,
                                      error_status=413,
                                      error_code=codes.OFFICE_ONLY_OFFICE_PUBLIC_FILES_LIMIT_REACHED)

    @only_office_settings_mock
    @mock.patch(PRIVATE_FILE_LIMIT_FIELD, 3)
    @mock.patch(SHARED_FILE_LIMIT_FIELD, 1)
    def test_only_office_limit_for_open_documents_public_file_limit_for_owner(self):
        """
        Проверяем, лимит на количество редактируемых публичных файлов владельцем папки
        Владелец папки не могут открыть больше, чем OFFICE_ONLY_FILES_PER_USER_LIMIT.
        Лимит OFFICE_ONLY_SHARED_FILES_PER_USER_LIMIT на него распространяется
        """
        file_folder = '/disk/shared'
        file_path_1 = '%s/%s' % (file_folder, 'Document1.docx')
        file_path_2 = '%s/%s' % (file_folder, 'Document2.docx')
        file_path_3 = '%s/%s' % (file_folder, 'Document3.docx')
        file_path_4 = '%s/%s' % (file_folder, 'Document4.docx')
        owner_uid = self.uid
        guest_uid = self.user_1.uid
        self.json_ok('user_init', {'uid': guest_uid})

        self._upload_shared_file(owner_uid, file_folder, file_path_1, [guest_uid])
        self._upload_shared_file(owner_uid, file_folder, file_path_2)
        self._upload_shared_file(owner_uid, file_folder, file_path_3)
        self._upload_shared_file(owner_uid, file_folder, file_path_4)

        # гость редактирует свой первый файл
        response = self._office_action_data_edit(guest_uid, file_path_1)
        # выставляем операцию в executing
        self._office_only_office_callback(office_action_data=response, status=1, users=[guest_uid])
        # и владелец редактирует свой первый файл
        self._office_action_data_edit(owner_uid, file_path_2)
        # но второй файл редакрировать гостю нельзя - уперлись в лимит OFFICE_ONLY_SHARED_FILES_PER_USER_LIMIT
        self._office_action_data_edit(guest_uid, file_path_2,
                                      error_status=413,
                                      error_code=codes.OFFICE_ONLY_OFFICE_PUBLIC_FILES_LIMIT_REACHED)
        # а вот владельцу можно - на него не действует лимит OFFICE_ONLY_SHARED_FILES_PER_USER_LIMIT
        self._office_action_data_edit(owner_uid, file_path_3)
        # и третий файл владелец может открыть - гость не расходует его слот в лимите OFFICE_ONLY_FILES_PER_USER_LIMIT
        self._office_action_data_edit(owner_uid, file_path_4)


    @only_office_settings_mock
    @mock.patch(PRIVATE_FILE_LIMIT_FIELD, 1)
    @mock.patch(OPERATION_LIMIT_TIMEOUT_FIELD, 0)
    def test_only_office_limit_do_not_count_stale_operations(self):
        """
        Проверяем, игнорирование устаревших сессий при расчете лимита
        """
        private_file_folder = '/disk'
        file_path_1 = '%s/%s' % (private_file_folder, 'Document1.docx')
        file_path_2 = '%s/%s' % (private_file_folder, 'Document2.docx')

        self.upload_file(self.uid, file_path_1)
        self.upload_file(self.uid, file_path_2)

        # открываем успешно один приватный файл
        self._office_action_data_edit(self.uid, file_path_1)
        # успешно открываем второй, т.к. сессия первого уже устарела
        self._office_action_data_edit(self.uid, file_path_2)

    @only_office_settings_mock
    @mock.patch(SHARED_FILE_LIMIT_FIELD, 1)
    @mock.patch(OPERATION_LIMIT_TIMEOUT_FIELD, 0)
    def test_only_office_shared_limit_do_not_count_stale_operations(self):
        """
        Проверяем, что устаревшиее сессии игнорируется при расчете лимита публичных файлов
        """
        file_folder = '/disk/shared'
        file_path_1 = '%s/%s' % (file_folder, 'Document1.docx')
        file_path_2 = '%s/%s' % (file_folder, 'Document2.docx')
        owner_uid = self.uid
        guest1_uid = self.user_1.uid
        self.json_ok('user_init', {'uid': guest1_uid})

        self._upload_shared_file(owner_uid, file_folder, file_path_1, [guest1_uid])
        self._upload_shared_file(owner_uid, file_folder, file_path_2, [guest1_uid])

        self._office_action_data_edit(guest1_uid, file_path_1)
        self._office_action_data_edit(guest1_uid, file_path_2)

    @only_office_settings_mock(orchestrator_service=False)
    def test_only_office_groupid_yandexoid(self):
        make_yateam(self.uid, yateam_uid='123')
        self._test_only_office_group_id(expected_group_id='yandexoid')

    @only_office_settings_mock(orchestrator_service=False)
    @mock.patch('mpfs.core.user.standart.StandartUser.is_paid', return_value=True)
    def test_only_office_groupid_paid_btb_user(self, _):
        b2b_key = 'b2b_key'
        self.json_ok('user_make_b2b', {'uid': self.uid, 'b2b_key': b2b_key})
        with mock.patch('mpfs.core.services.directory_service.DirectoryService.get_organizations_by_uids',
                        return_value={self.uid: {'id': b2b_key}}):
            self._test_only_office_group_id(expected_group_id=b2b_key)

    @only_office_settings_mock(orchestrator_service=False)
    @mock.patch('mpfs.core.user.standart.StandartUser.is_paid', return_value=False)
    def test_only_office_groupid_free_btb_user(self, _):
        b2b_key = 'b2b_key'
        self.json_ok('user_make_b2b', {'uid': self.uid, 'b2b_key': b2b_key})
        self._test_only_office_group_id(expected_group_id='free_b2b')

    @only_office_settings_mock(orchestrator_service=False)
    def test_only_office_groupid_yandexoid_btb_user(self):
        make_yateam(self.uid, yateam_uid='123')
        b2b_key = 'b2b_key'
        self.json_ok('user_make_b2b', {'uid': self.uid, 'b2b_key': b2b_key})
        self._test_only_office_group_id(expected_group_id='yandexoid')

    @only_office_settings_mock(orchestrator_service=False)
    def test_only_office_groupid_common_user(self):
        with mock.patch('mpfs.core.services.directory_service.DirectoryService.get_organizations_by_uids',
                        return_value={}):
            self._test_only_office_group_id(expected_group_id='')

    def _test_only_office_group_id(self, expected_group_id):
        file_folder = '/disk/shared'
        file_path = '%s/%s' % (file_folder, 'Document.docx')
        self._upload_shared_file(self.uid, file_folder, file_path)
        container = 'host.yp-c.yandex.net:port'

        with enable_experiment_for_uid('only_office_orchestrator', self.uid), \
             mock.patch('mpfs.core.services.orchestrator_service.OrchestratorService.request') as request:
            request.return_value.json = mock.Mock(return_value={'container': container})
            self._office_action_data_edit(self.uid, file_path)

        assert request.called
        args = request.call_args[0]
        actual_method = args[0]
        assert actual_method == 'PUT'
        assert args[1].startswith('/v1/session/%s:' % self.uid)
        assert args[2]['group_id'] == expected_group_id

    @only_office_settings_mock(orchestrator_service=False)
    def test_only_office_subdomain_in_operation_replaced_on_session_recreate(self):
        with enable_experiment_for_uid('only_office_orchestrator', self.uid), \
             mock.patch('mpfs.core.services.orchestrator_service.OrchestratorService.request') as request:
            host_1 = 'host_1'
            host_2 = 'host_2'

            def assert_subdomain(host):
                active_operations = manager.get_active_operations(self.uid)
                assert len(active_operations) == 1
                only_office_operation = active_operations[0]
                subdomain = only_office_operation['data'].get('subdomain')
                assert subdomain
                assert subdomain.startswith('%s_port' % host)

            file_path = '/disk/1.docx'
            self.upload_file(self.uid, file_path, file_data={'size': 50})

            request.return_value.json = mock.Mock(return_value={'container': '%s:port' % host_1})
            self._office_action_data_edit(self.uid, file_path, locale='ru', ext='docx')
            assert_subdomain(host_1)

            request.return_value.json = mock.Mock(return_value={'container': '%s:port' % host_2})
            self._office_action_data_edit(self.uid, file_path, locale='ru', ext='docx')
            assert_subdomain(host_2)

    @parameterized.expand([(1,), (2,), (3,), (4,), (5,), (6,), (7,)])
    def test_only_office_callback_with_right_container_info(self, callback_status):
        with OnlyOfficeTestCase.OnlyOfficeSettingsMock(orchestrator_service=False),\
             enable_experiment_for_uid('only_office_orchestrator', self.uid), \
             mock.patch('mpfs.core.services.orchestrator_service.OrchestratorService.request') as request:
            host_1 = 'host_1'
            host_2 = 'host_2'
            file_path = '/disk/1.docx'
            self.upload_file(self.uid, file_path, file_data={'size': 50})

            request.return_value.json = mock.Mock(return_value={'container': '%s:port' % host_1})
            response = self._office_action_data_edit(self.uid, file_path, locale='ru', ext='docx')
            url = response['balancer_url']
            self._office_only_office_callback(office_action_data=response, status=6, url='%s/file.docx' % url)

            request.return_value.json = mock.Mock(return_value={'container': '%s:port' % host_2})
            self._office_action_data_edit(self.uid, file_path, locale='ru', ext='docx')
            self._office_only_office_callback(office_action_data=response, status=callback_status, url='%s/file.docx' % url,
                                              error_status=409, error_code=codes.OFFICE_ONLY_OFFICE_OUTDATED_DATA)

    @mock.patch('mpfs.core.office.logic.only_office.OFFICE_ONLY_OFFICE_ENABLED', True)
    def test_force_OO(self):

        file_path = u'/disk/Документ.docx'
        self.upload_file(self.uid, file_path)
        resp = self.json_ok('office_action_check', {'uid': self.uid,
                                                    'action': 'edit',
                                                    'service_id': 'disk',
                                                    'service_file_id': file_path,
                                                    'locale': 'ru'})
        assert resp['office_online_editor_type'] == 'microsoft_online'
        with mock.patch('mpfs.core.user.common.CommonUser.get_office_selection_strategy', return_value='force_oo'):
            resp = self.json_ok('office_action_check', {'uid': self.uid,
                                                        'action': 'edit',
                                                        'service_id': 'disk',
                                                        'service_file_id': file_path,
                                                        'locale': 'ru'})
            assert resp['office_online_editor_type'] == 'only_office'

    @mock.patch('mpfs.core.office.logic.only_office.OFFICE_ONLY_OFFICE_ENABLED', True)
    def test_only_office_NDA(self):
        self.json_ok('office_set_selection_strategy', {'uid': self.uid,
                                                       'selection_strategy': 'force_oo'})
        file_path = YATEAM_DIR_PATH + '/1.docx'
        self.json_ok('mkdir', {'uid': self.uid, 'path': YATEAM_DIR_PATH})
        self.upload_file(self.uid, file_path, file_data={'size': 50})
        self.json_ok('set_public', {'uid': self.uid, 'path': file_path})
        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path,
                                     'meta': ','})
        resource_id = info['meta']['resource_id']
        self.json_ok('office_set_access_state', {'uid': self.uid,
                                                 'resource_id': resource_id,
                                                 'access_state': OfficeAccessStateConst.ALL})
        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path,
                                     'meta': ','})
        office_doc_id = info['meta']['office_online_sharing_url'].split('/')[-1]
        with mock.patch('mpfs.core.office.logic.only_office.user_has_nda_rights', side_effect=lambda uid: uid == self.uid):
            self.json_ok('office_action_data', {
                'uid': self.uid,
                'action': 'edit',
                'service_file_id': office_doc_id,
                'service_id': OfficeServiceIDConst.SHARING_URL
            })
            self.json_error('office_action_data', {
                'uid': user_1.uid,
                'action': 'edit',
                'service_file_id': office_doc_id,
                'service_id': OfficeServiceIDConst.SHARING_URL
            }, code=codes.RESOURCE_NOT_FOUND)

    @only_office_settings_mock
    def test_only_office_orchestrator_timeouts(self):
        with enable_experiment_for_uid('only_office_orchestrator', self.uid), \
             mock.patch('mpfs.core.services.orchestrator_service.OrchestratorService.request') as request:
            OrchestratorService().create_session(session_id=1, group_id='group')
            assert request.called
            assert request.call_args[1]['timeout'] == settings.services['OrchestratorService']['create_session_timeout']

            OrchestratorService().delete_session(session_id=1)
            assert request.called
            assert 'timeout' not in request.call_args[1]

            OrchestratorService().get_session(session_id=1)
            assert request.called
            assert 'timeout' not in request.call_args[1]

            OrchestratorService().get_sessions()
            assert request.called
            assert 'timeout' not in request.call_args[1]

            OrchestratorService().refresh_session(session_id=1)
            assert request.called
            assert 'timeout' not in request.call_args[1]

    def test_office_file_filters(self):
        self.json_ok('office_get_file_filters')

    @only_office_settings_mock
    def test_office_get_file_urls(self):
        file_folder = '/disk/files'
        file_path = '%s/%s' % (file_folder, 'Document1.docx')

        self.json_ok('mkdir', {'uid': self.uid, 'path': file_folder})
        self.upload_file(self.uid, file_path)

        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path,
                                     'meta': ','})
        resource_id = info['meta']['resource_id']

        response = self.json_ok('office_get_file_urls', {'uid': self.uid, 'resource_id': resource_id, 'tld': 'com'})
        assert 'view_url' in response
        assert 'edit_url' in response

    @parameterized.expand([
        ('no degradations', {'allow_anonymous': True, 'allow_free': True, 'allow_paid': True}, True),
        ('disallow anonymous', {'allow_anonymous': False, 'allow_free': True, 'allow_paid': True}, False),
        ('only paid', {'allow_anonymous': False, 'allow_free': False, 'allow_paid': True}, False),
        ('all disallow', {'allow_anonymous': False, 'allow_free': False, 'allow_paid': False}, False),
        ('all disallow and disable', {'allow_anonymous': False, 'allow_free': False, 'allow_paid': False}, True, False),
    ])
    @mock.patch('mpfs.core.office.logic.only_office.OFFICE_ONLY_OFFICE_ENABLED', True)
    def test_degradations_anonymous(self, case_name, degradations, allow, enabled=True):
        anonymous_yandexuid = '11223344556677'
        anonymous_uid = AnonymousUID.to_anonymous_uid(anonymous_yandexuid)
        file_path = '/disk/raccoon-story.docx'
        self.upload_file(self.uid, file_path)
        User(self.uid).set_only_office_enabled(True)
        self.json_ok('set_public', {'uid': self.uid, 'path': file_path})
        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path,
                                     'meta': ','})
        resource_id = info['meta']['resource_id']
        self.json_ok('office_set_access_state', {'uid': self.uid,
                                                 'resource_id': resource_id,
                                                 'access_state': OfficeAccessStateConst.ALL})
        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path,
                                     'meta': ','})
        office_doc_id = info['meta']['office_online_sharing_url'].split('/')[-1]

        with mock.patch('mpfs.core.degradations.settings.degradations', new={'enabled': enabled, 'only_office': degradations}):
            if allow:
                resp = self._office_action_data_edit(anonymous_uid, office_doc_id, service_id='sharing_url')
                assert resp['office_online_editor_type'] == 'only_office'
            else:
                self._office_action_data_edit(anonymous_uid, office_doc_id, service_id='sharing_url', error_code=179)

    @parameterized.expand([
        ('no degradations', {'allow_anonymous': True, 'allow_free': True, 'allow_paid': True}, True),
        # ('disallow anonymous', {'allow_anonymous': False, 'allow_free': True, 'allow_paid': True}, True),
        # ('only paid', {'allow_anonymous': False, 'allow_free': False, 'allow_paid': True}, False),
        # ('all disallow', {'allow_anonymous': False, 'allow_free': False, 'allow_paid': False}, False),
        # ('all disallow and disable', {'allow_anonymous': False, 'allow_free': False, 'allow_paid': False}, True, False),
    ])
    @mock.patch('mpfs.core.office.logic.only_office.OFFICE_ONLY_OFFICE_ENABLED', True)
    def test_degradations_free(self, case_name, degradations, allow, enabled=True):
        file_path = '/disk/raccoon-story.docx'
        self.upload_file(self.uid, file_path)
        User(self.uid).set_only_office_enabled(True)

        with mock.patch('mpfs.core.degradations.settings.degradations', new={'enabled': enabled, 'only_office': degradations}):
            resp = self._office_action_data_edit(self.uid, file_path)
            assert resp['office_online_editor_type'] == ('only_office' if allow else 'microsoft_online')


    @parameterized.expand([
        ('no degradations', {'allow_anonymous': True, 'allow_free': True, 'allow_paid': True}, True),
        ('disallow anonymous', {'allow_anonymous': False, 'allow_free': True, 'allow_paid': True}, True),
        ('only paid', {'allow_anonymous': False, 'allow_free': False, 'allow_paid': True}, True),
        ('all disallow', {'allow_anonymous': False, 'allow_free': False, 'allow_paid': False}, False),
        ('all disallow and disable', {'allow_anonymous': False, 'allow_free': False, 'allow_paid': False}, True, False),
    ])
    @mock.patch('mpfs.core.office.logic.only_office.OFFICE_ONLY_OFFICE_ENABLED', True)
    def test_degradations_paid(self, case_name, degradations, allow, enabled=True):
        file_path = '/disk/raccoon-story.docx'
        self.upload_file(self.uid, file_path)
        User(self.uid).set_only_office_enabled(True)

        with mock.patch('mpfs.core.degradations.settings.degradations', new={'enabled': enabled, 'only_office': degradations}):
            self.billing_ok(
                'service_create',
                {'uid': self.uid, 'line': 'partner', 'pid': 'yandex_b2b_mail_pro', 'product.amount': GB * 100,
                 'auto_init_user': 1, 'ip': '127.0.0.1'},
            )
            resp = self._office_action_data_edit(self.uid, file_path)
            assert resp['office_online_editor_type'] == ('only_office' if allow else 'microsoft_online')

    @only_office_settings_mock
    def test_office_get_file_urls_readonly(self):
        file_folder = '/disk/files'
        file_path = '%s/%s' % (file_folder, 'Document1.pdf')

        self.json_ok('mkdir', {'uid': self.uid, 'path': file_folder})
        self.upload_file(self.uid, file_path)

        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path,
                                     'meta': ','})
        resource_id = info['meta']['resource_id']

        response = self.json_ok('office_get_file_urls', {'uid': self.uid, 'resource_id': resource_id})
        assert 'view_url' in response
        assert 'edit_url' not in response

    @only_office_settings_mock
    def test_office_get_file_urls_wrong_types(self):
        file_folder = '/disk/files'
        file_path_1 = '%s/%s' % (file_folder, 'Document1.exe')
        file_path_2 = '%s/%s' % (file_folder, 'Document')

        self.json_ok('mkdir', {'uid': self.uid, 'path': file_folder})
        self.upload_file(self.uid, file_path_1)
        self.upload_file(self.uid, file_path_2)

        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path_1,
                                     'meta': ','})
        resource_id = info['meta']['resource_id']
        self.json_error('office_get_file_urls', {'uid': self.uid, 'resource_id': resource_id},
                        code=codes.OFFICE_UNSUPPORTED_EXTENSION)

        info = self.json_ok('info', {'uid': self.uid,
                                       'path': file_path_2,
                                       'meta': ','})
        resource_id = info['meta']['resource_id']
        self.json_error('office_get_file_urls', {'uid': self.uid, 'resource_id': resource_id},
                        code=codes.OFFICE_UNSUPPORTED_EXTENSION)

    @only_office_settings_mock
    def test_office_get_file_urls_docx_upper_case(self):
        file_folder = '/disk/files'
        file_path = '%s/%s' % (file_folder, 'Document1.DOCX')

        self.json_ok('mkdir', {'uid': self.uid, 'path': file_folder})
        self.upload_file(self.uid, file_path)

        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path,
                                     'meta': ','})
        resource_id = info['meta']['resource_id']

        response = self.json_ok('office_get_file_urls',
                                {'uid': self.uid, 'resource_id': resource_id})
        view_url = urlparse.urlparse(response['view_url'])
        view_file_path = urlparse.parse_qs(view_url.query)['url'][0]

        edit_url = urllib2.unquote(urlparse.urlparse(response['edit_url']).path)

        assert file_path in view_file_path
        assert file_path in edit_url


class OfficeGetFileUrlsTestCase(OfficeTestCase):
    base_folder = '/disk/folder'
    shared_folder = '%s/%s' % (base_folder, 'shared_folder')
    file_path = '%s/%s' % (shared_folder, 'Document1.docx')
    guest_file_path = '/disk/shared_folder/Document1.docx'
    owner_uid = OfficeTestCase.uid
    guest_uid = OfficeTestCase.user_1.uid

    def setup_method(self, method):
        super(OfficeGetFileUrlsTestCase, self).setup_method(method)
        self.json_ok('user_init', {'uid': self.guest_uid})

    @parameterized.expand([
        ('oo_all_disabled', False, False, True),
        ('oo_all_disabled_with_feature', False, False, False, {'feature_enabled': True}),
        ('oo_all_enabled', True, True, True),
        ('oo_only_owner', True, False, True),
        ('oo_only_guest', False, True, True),
        ('oo_only_guest_with_feature', False, True, False, {'feature_enabled': True}),
        ('oo_all_enabled_ro_file', True, True, True, {'rights': 640}),
        ('oo_all_enabled_ro_file_with_feature', True, True, False, {'rights': 640, 'feature_enabled': True}),
    ])
    def test_exists_edit_link(self, case_name, owner_oo_enabled, guest_oo_enabled, edit_link_exist, additional_params={}):
        with OnlyOfficeTestCase.OnlyOfficeSettingsMock(get_only_office_enabled=None), \
             mock.patch('mpfs.core.office.interface.FEATURE_TOGGLES_EDITOR_BY_OWNER_ENABLED', additional_params.get('feature_enabled', False)):

            UserDAO().set_only_office_enabled(self.uid, owner_oo_enabled)
            UserDAO().set_only_office_enabled(self.guest_uid, guest_oo_enabled)

            resource_id = self._create_shared_file(**additional_params)

            response = self.json_ok('office_get_file_urls', {'uid': self.guest_uid, 'resource_id': resource_id})
            view_url = urlparse.urlparse(response['view_url'])
            view_file_path = urlparse.parse_qs(view_url.query)['url'][0]
            assert self.guest_file_path in view_file_path

            if edit_link_exist:
                edit_url = urllib2.unquote(urlparse.urlparse(response['edit_url']).path)
                assert self.guest_file_path in edit_url
            else:
                assert 'edit_url' not in response

    def _create_shared_file(self, rights=660, **kwargs):
        self.json_ok('mkdir', {'uid': self.owner_uid, 'path': self.base_folder})
        self.json_ok('mkdir', {'uid': self.owner_uid, 'path': self.shared_folder})

        group = self.json_ok('share_create_group', {'uid': self.owner_uid, 'path': self.shared_folder})
        invite_hash = self.share_invite(group['gid'], self.guest_uid, rights=rights)
        self.json_ok('share_activate_invite', {'uid': self.guest_uid, 'hash': invite_hash})
        self.upload_file(self.owner_uid, self.file_path)

        info = self.json_ok('info', {'uid': self.guest_uid, 'path': self.guest_file_path, 'meta': ','})
        return info['meta']['resource_id']
