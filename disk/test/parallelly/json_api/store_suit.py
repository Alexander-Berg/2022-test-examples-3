# -*- coding: utf-8 -*-
import datetime
import hashlib
import random
import time
import attrdict
import mock
import pytest

from hamcrest import assert_that, has_entry, has_item, is_not, has_key, equal_to, contains_inanyorder, contains_string
from lxml import etree
from nose_parameterized import parameterized

from mpfs.common.errors import KladunNoResponse, EmptyFileUploadedForNonEmpyStoreError, KladunHardlinkFound
from mpfs.common.util.crypt import CryptAgent
from mpfs.common.util.filetypes import MediaType
from mpfs.common.util.generator import md5, sha256
from mpfs.core.bus import Bus
from mpfs.core.operations import manager
from mpfs.core.user.attach import AttachUser
from mpfs.core.user.base import User
from mpfs.core.user.constants import DEFAULT_FOLDERS, DEFAULT_FOLDERS_NAMES
from test.base import time_machine
from mpfs.common.static import codes, tags
from mpfs.common.util import from_json, to_json
from mpfs.config import settings
from mpfs.core import factory
from mpfs.core.address import Address
from mpfs.core.operations.filesystem.store import ExtractFileFromArchive
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase
from mpfs.core.filesystem.quota import Quota
from mpfs.core.filesystem.hardlinks.common import AbstractLink, construct_hid
from mpfs.core.services.passport_service import passport
from mpfs.core.filesystem.cleaner.models import DeletedStid
from mpfs.core.services.previewer_service import RegeneratePreviewResult
from test.base_suit import patch_http_client_open_url
from test.common.sharing import CommonSharingMethods
from test.conftest import INIT_USER_IN_POSTGRES
from test.fixtures import users
from test.helpers.size_units import MB
from test.helpers.stubs.manager import StubsManager
from test.helpers.stubs.services import KladunStub, PreviewerStub, PassportStub
from test.parallelly.json_api.base import CommonJsonApiTestCase


SERVICES_MULCA_EMPTY_FILE_MID = settings.services['mulca']['empty_file_mid']
SERVICES_MULCA_EMPTY_FILE_DIGEST_MID = settings.services['mulca']['empty_file_digest_mid']
SPEED_LIMITS_BY_MEDIA_TYPE = settings.speed_limits['by_media_type']
SPEED_LIMITS_BY_AUTH_METHOD = settings.speed_limits['by_auth_method']


db = CollectionRoutedDatabase()


class ExtractFileFromArchiveTestCase(CommonJsonApiTestCase):
    def test_common(self):
        self.upload_file(self.uid, '/disk/1.zip')
        opts = {
            'uid': self.uid,
            'src_archive': '/disk/1.zip',
            'src_file': '1.jpg',
            'dst': '/disk/1.jpg',
        }
        required_keys = ('uid', 'file-id', 'path', 'source-service', 'service-file-id',
                         'file-to-extract', 'max-file-size', 'oid')
        with KladunStub() as kladun_mocks:
            resp = self.json_ok('extract_file_from_archive', opts)
            assert resp['type'] == 'store'
            uploader_actual_keys = kladun_mocks.extract_file_post_request.call_args[0][0].keys()
        assert_that(uploader_actual_keys, contains_inanyorder(*required_keys))

    def test_private_hash(self):
        self.create_user(self.uid_3)
        self.upload_file(self.uid_3, '/disk/1.zip')
        p_hash = self.json_ok('set_public', {'uid': self.uid_3, 'path': '/disk/1.zip'})['hash']
        opts = {
            'uid': self.uid,
            'private_hash': p_hash,
            'src_file': '1.jpg',
            'dst': '/disk/1.jpg',
        }
        with patch_http_client_open_url() as service_requests:
            self.json_ok('extract_file_from_archive', opts)

    def test_errors(self):
        self.upload_file(self.uid, '/disk/1.zip')
        self.upload_file(self.uid, '/disk/1.jpg')
        opts = {
            'uid': self.uid,
            'src_archive': '/disk/1.zip',
            'src_file': '1.jpg',
            'dst': '/disk/1.jpg',
        }
        with patch_http_client_open_url():
            # У dst нет родительской папки
            opts['dst'] = '/disk/none_folder/1.jpg'
            self.json_error('extract_file_from_archive', opts, code=62)
            # не существует src_archive
            opts['dst'] = '/disk/1.jpg'
            opts['src_archive'] = '/disk/none_folder/1.zip'
            self.json_error('extract_file_from_archive', opts, code=71)

    def test_existing_destination(self):
        self.upload_file(self.uid, '/disk/1.zip')
        self.upload_file(self.uid, '/disk/1.jpg')
        opts = {
            'uid': self.uid,
            'src_archive': '/disk/1.zip',
            'src_file': '1.jpg',
            'dst': '/disk/1.jpg',
        }
        with patch_http_client_open_url():
            # dst уже существует
            response = self.json_ok('extract_file_from_archive', opts)
            operation_status = self.json_ok('status', {'uid': self.uid, 'oid': response['oid']})

        dst_path = str(operation_status['params']['path'])
        filename = dst_path.split(":")[1]

        assert filename == '/disk/1 (1).jpg'

    def test_mulca(self):
        self.upload_file(self.uid, '/disk/1.zip')
        opts = {
            'uid': self.uid,
            'src_archive': '/mulca/57691.338915389.1422753727131600720447904958480:1.2',
            'src_file': '1.jpg',
            'dst': '/disk/1.jpg',
        }
        with patch_http_client_open_url():
            self.json_ok('extract_file_from_archive', opts)

    def test_extract_file_from_archive_returns_lenta_block_id_if_hard_linked(self):
        """Протестировать что после отработки операции ручка `status` возвращает `lenta_block_id`
        в случае если файл у нас уже существует и он соответственно хардлинкнулся."""
        uid = self.uid

        random_size = str(random.randint(1, 100000))
        random_md5 = hashlib.md5(random_size).hexdigest()
        random_sha256 = hashlib.sha256(random_size).hexdigest()

        # загружаем файл с нужными параметрами
        self.upload_file(uid, '/disk/random.file', file_data={
            'size': random_size,
            'md5': random_md5,
            'sha256': random_sha256
        })

        self.upload_file(self.uid, '/disk/test.zip')
        result = self.json_ok('extract_file_from_archive', {
            'uid': uid,
            'src_archive': '/disk/test.zip',
            'src_file': 'test.jpg',
            'dst': '/disk/test.jpg',
        })
        oid = result['oid']

        # подставляем параметры как у файла, который у нас есть (можем схардлинкать)
        with open('fixtures/xml/kladun/extract-file-from-archive/commitFileInfo_hard_linked.xml') as f:
            commit_file_info_xml_data = f.read().replace('{{ sha256 }}', random_sha256).replace(
                '{{ md5 }}', random_md5
            ).replace('{{ size }}', random_size)

        with KladunStub(
            status_values=(
                etree.fromstring(commit_file_info_xml_data),
            )
        ):
            with mock.patch(
                'mpfs.core.filesystem.base.Filesystem.hardlink',
                return_value=attrdict.AttrDict({'file_id': '100500'})
            ):
                with mock.patch(
                    'mpfs.core.services.mulca_service.Mulca.is_file_exist',
                    return_value=True
                ):
                    lenta_block_id = '100500'
                    with mock.patch(
                        'mpfs.core.services.lenta_loader_service.LentaLoaderService'
                        '.process_log_line_and_return_created_block_id',
                        return_value=lenta_block_id
                    ) as mocked_process_log_line_and_return_created_block_id:
                        result = self.service('kladun_callback', {
                            'uid': uid,
                            'oid': oid,
                            'status_xml': commit_file_info_xml_data,
                            'type': 'commitFileInfo'
                        })
                        assert mocked_process_log_line_and_return_created_block_id.called
                        assert result['code'] == codes.KLADUN_HARDLINK_FOUND

        result = self.json_ok('status', {'uid': self.uid, 'oid': oid})
        assert tags.LENTA_BLOCK_ID in result
        assert result[tags.LENTA_BLOCK_ID] == lenta_block_id

    def test_extract_file_from_archive_returns_lenta_block_id_if_not_hard_linked(self):
        """Протестировать что после отработки операции ручка `status` возвращает `lenta_block_id`
        в случае если файла у нас такого не существует и он соответственно загрузился стандартным механизмом `store`."""
        uid = self.uid

        self.upload_file(self.uid, '/disk/test.zip')
        result = self.json_ok('extract_file_from_archive', {
            'uid': uid,
            'src_archive': '/disk/test.zip',
            'src_file': 'test.jpg',
            'dst': '/disk/test.jpg',
        })
        oid = result['oid']

        with open('fixtures/xml/kladun/extract-file-from-archive/commitFileInfo.xml') as f:
            commit_file_info_xml_data = f.read()

        with open('fixtures/xml/kladun/extract-file-from-archive/commitFileUpload.xml') as f:
            commit_file_upload_xml_data = f.read()

        with open('fixtures/xml/kladun/extract-file-from-archive/commitFinal.xml') as f:
            commit_final_xml_data = f.read()

        with KladunStub(
            status_values=(
                etree.fromstring(commit_file_info_xml_data),
                etree.fromstring(commit_file_upload_xml_data),
                etree.fromstring(commit_final_xml_data)
            )
        ):
            self.service_ok('kladun_callback', {
                'uid': uid,
                'oid': oid,
                'type': 'commitFileInfo',
                'status_xml': commit_file_info_xml_data
            })

            lenta_block_id = '100500'
            with mock.patch(
                'mpfs.core.services.lenta_loader_service.LentaLoaderService'
                '.process_log_line_and_return_created_block_id',
                return_value=lenta_block_id
            ) as mocked_process_log_line_and_return_created_block_id:
                self.service_ok('kladun_callback', {
                    'uid': uid,
                    'oid': oid,
                    'type': 'commitFileUpload',
                    'status_xml': commit_file_upload_xml_data
                })
                assert mocked_process_log_line_and_return_created_block_id.called

            self.service_ok('kladun_callback', {
                'uid': uid,
                'oid': oid,
                'type': 'commitFinal',
                'status_xml': commit_final_xml_data
            })

        result = self.json_ok('status', {'uid': uid, 'oid': oid})
        assert tags.LENTA_BLOCK_ID in result
        assert result[tags.LENTA_BLOCK_ID] == lenta_block_id

    def test_extract_file_from_archive_with_mail_mid_and_hid(self):
        """Протестировать работу ручки `extract_file_from_archive`
        с переданными туда идентификаторами письма и части в письме."""
        # https://st.yandex-team.ru/CHEMODAN-35942
        uid = self.uid
        self.upload_file(uid, '/disk/test.zip')

        with open('fixtures/xml/mail_service1.xml') as f:
            mail_service_response = f.read()
        with mock.patch(
            'mpfs.core.services.mail_service.Mail.open_url',
            return_value=mail_service_response
        ):
            with mock.patch(
                'mpfs.core.operations.filesystem.store.ExtractFileFromArchive.post_request_to_kladun',
                wraps=ExtractFileFromArchive.post_request_to_kladun
            ) as mocked_post_request_to_kladun:
                mail_mid = '2280000020845606474'
                mail_hid = '1.1'

                opts = {
                    'uid': uid,
                    'src_archive': '/mail/file:%s:%s' % (mail_mid, mail_hid),
                    'src_file': 'test.jpg',
                    'dst': '/disk/test.jpg',
                }
                self.json_ok('extract_file_from_archive', opts)
                assert mocked_post_request_to_kladun.called
                args, kwargs = mocked_post_request_to_kladun.call_args
                (post_data,) = args
                assert post_data['source-service'] == 'mail2'
                assert post_data['service-file-id'] == '%s:%s/%s' % (uid, mail_mid, mail_hid)


class StoreUtilsMixin(object):
    def prepare_kladun_callbacks(self, **filedata):
        """
        Метод организации фейкового кладуна, который позволит дергать коллбеки, как будто настоящий

        :param kwargs: куча параметров, смотри код
        :return: list из тел для всех трех коллбеков
        """
        # готовим переменные для имитации данных коллбеков
        uid = filedata.get('uid') or self.uid
        path = filedata.get('path') or '/disk/test.jpg'
        address = Address.Make(uid, path).id
        rand = str('%f' % time.time()).replace('.', '')[9:]
        file_md5 = filedata.get('md5') or hashlib.md5(rand).hexdigest()
        file_sha256 = filedata.get('sha256') or hashlib.sha256(rand).hexdigest()
        mimetype = filedata.get('mimetype') or 'image/jpeg'
        drweb = 'true'
        size = filedata.get('size', int(rand))
        etime = filedata.get('etime') or datetime.datetime.fromtimestamp(time.time()).strftime('%Y-%m-%dT%H:%M:%SZ')
        file_id = filedata.get('file_id') or hashlib.sha256(file_md5 + ':' + file_sha256).hexdigest()
        mid_digest = '100000.yadisk:%s.%s' % (self.uid, int(file_md5[:16], 16))
        mid_file = '100000.yadisk:%s.%s' % (self.uid, int(file_md5[:16][::-1], 16))
        pmid = '100000.yadisk:%s.%s' % (self.uid, int(file_md5[:32][::2], 16))

        # готовим кладунские коллбеки
        body_1_raw = open(filedata.get('body_1_file') or 'fixtures/xml/kladun_store_1.xml').read()
        body_2_raw = open(filedata.get('body_2_file') or 'fixtures/xml/kladun_store_2.xml').read()
        body_3_raw = open(filedata.get('body_3_file') or 'fixtures/xml/kladun_store_3.xml').read()
        body_1 = etree.fromstring(body_1_raw)
        body_2 = etree.fromstring(body_2_raw)
        body_3 = etree.fromstring(body_3_raw)

        for body in (body_1, body_2, body_3):
            body.find('request').find('chemodan-file-attributes').set('uid', self.uid)
            body.find('request').find('chemodan-file-attributes').set('file-id', file_id)
            body.find('request').find('chemodan-file-attributes').set('path', address)
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

        body_3.find('stages').find('exif-info').find('result').set('creation-date', etime)
        body_3.find('stages').find('antivirus').find('result').set('result', drweb)
        body_3.find('stages').find('preview-image-mulca-upload').find('result').set('mulca-id', pmid)

        return body_1, body_2, body_3


class StoreJsonApiTestCase(StoreUtilsMixin, CommonJsonApiTestCase):
    def test_fail_store_with_zero_file_size_for_web_at_first_kladun_callback(self):
        ycrid = 'web-passed_by_uploader'
        oid = self.json_ok('store', {'uid': self.uid,
                                     'path': '/disk/enot.jpg',
                                     'size': '77'},
                           headers={'Yandex-Cloud-Request-ID': ycrid})['oid']

        body_1, body_2, body_3 = self.prepare_kladun_callbacks(uid=self.uid,
                                                               path='/disk/enot.jpg',
                                                               md5='d41d8cd98f00b204e9800998ecf8427e',
                                                               sha256='e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
                                                               size=0)
        with KladunStub(status_values=(body_1,)):
            self.service_error('kladun_callback', {'uid': self.uid,
                                                   'oid': oid,
                                                   'type': 'commitFileInfo',
                                                   'status_xml': etree.tostring(body_1)},
                               headers={'Yandex-Cloud-Request-ID': ycrid},
                               status=418)

    @parameterized.expand([('client_specifed_zero_size', {'size': '0'}),
                           ('client_didnt_pass_size_param', {})])
    def test_no_error_on_store_with_zero_file_size_for_web_if(self, case_name, additional_params):
        ycrid = 'web-passed_by_uploader'
        params = {'uid': self.uid, 'path': '/disk/enot.jpg'}
        params.update(additional_params)
        oid = self.json_ok('store', params,
                           headers={'Yandex-Cloud-Request-ID': ycrid})['oid']

        body_1, body_2, body_3 = self.prepare_kladun_callbacks(uid=self.uid,
                                                               path='/disk/enot.jpg',
                                                               md5='d41d8cd98f00b204e9800998ecf8427e',
                                                               sha256='e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
                                                               size=0)
        with KladunStub(status_values=(body_1,)):
            self.service_error('kladun_callback', {'uid': self.uid,
                                                   'oid': oid,
                                                   'type': 'commitFileInfo',
                                                   'status_xml': etree.tostring(body_1)},
                               headers={'Yandex-Cloud-Request-ID': ycrid},
                               code=KladunHardlinkFound.code)

    @parameterized.expand([('http', 0, 'false'),
                           ('https', 1, 'true')])
    def test_store_use_https(self, case_name, use_https, expected_value):
        opts = {'uid': self.uid,
                'path': '/disk/http.file',
                'use_https': use_https}
        with KladunStub() as kladun_mocks:
            self.json_ok('store', opts)
            assert_that(kladun_mocks.upload_to_disk_post_request.called, equal_to(True))
            kladun_args, _ = kladun_mocks.upload_to_disk_post_request.call_args
        assert_that(kladun_args,
                    has_item(has_entry('use-https', expected_value)))

    def test_store_user_with_speed_limit(self):
        limit = 1*MB
        with KladunStub() as kladun_mocks,\
                mock.patch.dict('mpfs.core.base.SPEED_LIMITS_BY_UID', {self.uid: limit}):
            self.json_ok('store', {'uid': self.uid,
                                   'path': '/disk/raccoon.jpg'})
            assert_that(kladun_mocks.upload_to_disk_post_request.called, equal_to(True))
            kladun_args, _ = kladun_mocks.upload_to_disk_post_request.call_args
        assert_that(kladun_args,
                    has_item(has_entry('upload-max-speed-bps', equal_to(limit))))

    def test_store_skip_limit_by_param(self):
        limit = 1*MB
        with KladunStub() as kladun_mocks,\
                mock.patch.dict('mpfs.core.base.SPEED_LIMITS_BY_UID', {self.uid: limit}):
            self.json_ok('store', {'uid': self.uid,
                                   'path': '/disk/raccoon.jpg',
                                   'skip_speed_limit': '1'})
            assert_that(kladun_mocks.upload_to_disk_post_request.called, equal_to(True))
            kladun_args, _ = kladun_mocks.upload_to_disk_post_request.call_args
        assert_that(kladun_args,
                    is_not(has_key('upload-max-speed-bps')))

    def test_store_user_without_speed_limit(self):
        with KladunStub() as kladun_mocks:
            self.json_ok('store', {'uid': self.uid,
                                   'path': '/disk/raccoon.jpg'})
            assert_that(kladun_mocks.upload_to_disk_post_request.called, equal_to(True))
            kladun_args, _ = kladun_mocks.upload_to_disk_post_request.call_args
        assert_that(kladun_args,
                    is_not(has_key('upload-max-speed-bps')))

    def test_store_for_media_type_with_speed_limit(self):
        expected_limit = SPEED_LIMITS_BY_MEDIA_TYPE[MediaType.DATA]
        with KladunStub() as kladun_mocks:
            self.json_ok('store', {'uid': self.uid,
                                   'path': '/disk/enot.yam'},
                         headers={'Yandex-Cloud-Request-ID': 'dav-test'})
        assert_that(kladun_mocks.upload_to_disk_post_request.called, equal_to(True))
        kladun_args, _ = kladun_mocks.upload_to_disk_post_request.call_args
        assert_that(kladun_args,
                    has_item(has_entry('upload-max-speed-bps', equal_to(expected_limit))))

    def test_store_with_excluded_ua_wo_speed_limit(self):
        with KladunStub() as kladun_mocks:
            self.json_ok('store', {'uid': self.uid,
                                   'path': '/disk/enot.yam'},
                         headers={'Yandex-Cloud-Request-ID': 'dav-test', 'User-Agent': 'biglion.tech'})
        assert_that(kladun_mocks.upload_to_disk_post_request.called, equal_to(True))
        kladun_args, _ = kladun_mocks.upload_to_disk_post_request.call_args
        assert_that(kladun_args,
                    is_not(has_key('upload-max-speed-bps')))

    def test_store_with_speed_limit_by_auth_method(self):
        expected_limit = SPEED_LIMITS_BY_AUTH_METHOD['basic']
        with KladunStub() as kladun_mocks:
            self.json_ok('store', {'uid': self.uid,
                                   'path': '/disk/pushistyj_enot.jpg'},
                         headers={'Yandex-Cloud-Request-ID': 'dav-test',
                                  'X-Auth-Method': 'BASIC'})
        assert_that(kladun_mocks.upload_to_disk_post_request.called, equal_to(True))
        kladun_args, _ = kladun_mocks.upload_to_disk_post_request.call_args
        assert_that(kladun_args,
                    has_item(has_entry('upload-max-speed-bps', equal_to(expected_limit))))

    @parameterized.expand(['Safari/537.36 YandexSearch/7.77 (deviceId: enot)',
                           'ru.yandex.mail/4.37.2.64675 (samsung SM-A105F; Android 9)'])
    def test_store_for_media_type_with_speed_limit_from_native_client(self, user_agent):
        with KladunStub() as kladun_mocks:
            self.json_ok('store', {'uid': self.uid,
                                   'path': '/disk/enot.yam'},
                         headers={'Yandex-Cloud-Request-ID': 'dav-test', 'User-Agent': user_agent})
        assert_that(kladun_mocks.upload_to_disk_post_request.called, equal_to(True))
        kladun_args, _ = kladun_mocks.upload_to_disk_post_request.call_args
        assert_that(kladun_args,
                    is_not(has_item(has_key('upload-max-speed-bps'))))

    def test_store_without_use_https(self):
        """Тест ручки `store` без указания опции `use_https`."""
        opts = {'uid': self.uid,
                'path': '/disk/https.file'}
        with KladunStub() as kladun_mocks:
            self.json_ok('store', opts)
            assert_that(kladun_mocks.upload_to_disk_post_request.called, equal_to(True))
            kladun_args, _ = kladun_mocks.upload_to_disk_post_request.call_args
        assert_that(kladun_args, is_not(has_item(has_key('use-https'))))

    def test_upload_video(self):
        path = '/disk/video_file.avi'
        file_data = {'size': 100}
        self.upload_video(self.uid, path, file_data=file_data)
        opts = {'uid': self.uid,
                'path': path,
                'meta': ''}
        file_data = self.json_ok('info', opts)
        self.assertTrue('pmid' in file_data['meta'])
        self.assertTrue('video_info' in file_data['meta'])
        self.assertEqual(type(file_data['meta']['video_info']), dict)
        self.assertTrue('streams' in file_data['meta']['video_info'])
        self.assertEqual(type(file_data['meta']['video_info']['streams']), list)

    def test_async_store_external(self):
        url = 'http://disk.test/file'
        for status_file_path, ostatus in (('fixtures/xml/upload-from-service-failed.xml', 'FAILED'),
                                          ('fixtures/xml/upload-from-service.xml', 'DONE')):
            with KladunStub(status_value_paths=(status_file_path,)):
                oid = self.json_ok('async_store_external', {'uid': self.uid,
                                                            'path': '/disk/file1',
                                                            'external_url': url})['oid']

                with open(status_file_path) as fix_file:
                    status_xml = fix_file.read()

                self.service_ok('kladun_callback', {
                    'uid': self.uid,
                    'oid': oid,
                    'status_xml': status_xml,
                    'type': tags.COMMIT_FILE_INFO,
                })
                assert self.json_ok('status', {'uid': self.uid, 'oid': oid})['status'] == ostatus

    def test_async_store_external_status_returns_lenta_block_id_if_file_hard_linked(self):
        """Проверить что после полной отработки ручки `async_store_external` ручка `status` возвращает блок
        ленты `lenta_block_id`. Кейс для случая когда файл у нас есть (жесткая ссылка)."""

        # имитируем ситуацию, когда у нас есть такой файл и мы его хардлинкаем

        uid = self.uid

        random_size = str(random.randint(1, 100000))
        random_md5 = hashlib.md5(random_size).hexdigest()
        random_sha256 = hashlib.sha256(random_size).hexdigest()

        # загружаем файл с нужными параметрами
        self.upload_file(uid, '/disk/random.file', file_data={
            'size': random_size,
            'md5': random_md5,
            'sha256': random_sha256
        })

        result = self.json_ok('async_store_external', {
            'uid': uid,
            'path': '/disk/test.jpg',
            'external_url': 'http://disk.test/file'
        })
        oid = result['oid']

        # подставляем параметры как у файла, который у нас есть (можем схардлинкать)
        with open('fixtures/xml/kladun/upload-from-service/commitFileInfo_hard_linked.xml') as f:
            template = f.read()
            commit_file_info_xml_data = template.replace(
                '{{ uid }}', str(uid)
            ).replace('{{ oid }}', str(oid)).replace('{{ size }}', random_size).replace(
                '{{ md5 }}', random_md5
            ).replace('{{ sha256 }}', random_sha256)

        with KladunStub(
            status_values=(
                etree.fromstring(commit_file_info_xml_data),
            )
        ):
            with mock.patch(
                'mpfs.core.filesystem.base.Filesystem.hardlink',
                return_value=attrdict.AttrDict({'file_id': '100500'})
            ):
                with mock.patch(
                    'mpfs.core.services.mulca_service.Mulca.is_file_exist',
                    return_value=True
                ):
                    lenta_block_id = '100500'
                    with mock.patch(
                        'mpfs.core.services.lenta_loader_service.LentaLoaderService'
                        '.process_log_line_and_return_created_block_id',
                        return_value=lenta_block_id
                    ) as mocked_process_log_line_and_return_created_block_id:
                        result = self.service('kladun_callback', {
                            'uid': uid,
                            'oid': oid,
                            'status_xml': commit_file_info_xml_data,
                            'type': 'commitFileInfo'
                        })
                        assert mocked_process_log_line_and_return_created_block_id.called
                        assert result['code'] == codes.KLADUN_HARDLINK_FOUND

                        result = self.json_ok('status', {'uid': self.uid, 'oid': oid})
                        assert 'lenta_block_id' in result
                        assert result[tags.LENTA_BLOCK_ID] == lenta_block_id

    def test_async_store_external_status_returns_lenta_block_id_if_file_not_hard_linked(self):
        """Проверить что после полной отработки ручки `async_store_external` ручка `status` возвращает блок
        ленты `lenta_block_id`. Кейс для случая когда файла у нас есть (без хардлинка)."""
        uid = self.uid

        result = self.json_ok('async_store_external', {
            'uid': uid,
            'path': '/disk/test.jpg',
            'external_url': 'http://disk.test/file'
        })
        oid = result['oid']

        with open('fixtures/xml/kladun/upload-from-service/commitFileInfo.xml') as f:
            commit_file_info_xml_data = f.read()

        with open('fixtures/xml/kladun/upload-from-service/commitFileUpload.xml') as f:
            commit_file_upload_xml_data = f.read()

        with open('fixtures/xml/kladun/upload-from-service/commitFinal.xml') as f:
            commit_final_xml_data = f.read()

        with KladunStub(
            status_values=(
                etree.fromstring(commit_file_info_xml_data),
                etree.fromstring(commit_file_upload_xml_data),
                etree.fromstring(commit_final_xml_data)
            )
        ):
            self.service_ok('kladun_callback', {
                'uid': uid,
                'oid': oid,
                'type': 'commitFileInfo',
                'status_xml': commit_file_info_xml_data
            })

            lenta_block_id = '100500'
            with mock.patch(
                'mpfs.core.services.lenta_loader_service.LentaLoaderService'
                '.process_log_line_and_return_created_block_id',
                return_value=lenta_block_id
            ) as mocked_process_log_line_and_return_created_block_id:
                self.service_ok('kladun_callback', {
                    'uid': uid,
                    'oid': oid,
                    'type': 'commitFileUpload',
                    'status_xml': commit_file_upload_xml_data
                })
                assert mocked_process_log_line_and_return_created_block_id.called

            self.service_ok('kladun_callback', {
                'uid': uid,
                'oid': oid,
                'type': 'commitFinal',
                'status_xml': commit_final_xml_data
            })

        result = self.json_ok('status', {'uid': uid, 'oid': oid})
        assert tags.LENTA_BLOCK_ID in result
        assert result[tags.LENTA_BLOCK_ID] == lenta_block_id

    def test_async_store_external_with_suffix(self):
        """
        Проверяем, что ручка async_store_external добавляет суффикс к имени файла,
        если файл с таким именем уже существует.
        """
        status_file_path = 'fixtures/xml/upload-from-service.xml'

        self.upload_file(self.uid, '/disk/file1')

        url = 'http://disk.test/file'
        oid = self.json_ok('async_store_external', {'uid': self.uid,
                                                    'path': '/disk/file1',
                                                    'external_url': url})['oid']

        with open(status_file_path) as fix_file:
            status_xml = fix_file.read()

        with KladunStub(status_value_paths=(status_file_path,)):
            self.service_ok('kladun_callback', {
                'uid': self.uid,
                'oid': oid,
                'status_xml': status_xml,
                'type': tags.COMMIT_FILE_UPLOAD
            })

        self.json_ok('info', {'uid': self.uid, 'path': '/disk/file1'})
        self.json_ok('info', {'uid': self.uid, 'path': '/disk/file1 (1)'})

    def test_state_status_done_completed(self):
        """
        Тестируем отдельную выдачу поля state, в котором выдаем реальный статус операции

        Подробности: https://st.yandex-team.ru/CHEMODAN-20922
        """
        rand = str('%f' % time.time()).replace('.', '')[9:]
        file_md5 = hashlib.md5(rand).hexdigest()
        file_sha256 = hashlib.sha256(rand).hexdigest()
        size = int(rand)
        path = '/disk/test.jpg'

        body_1, body_2, body_3 = self.prepare_kladun_callbacks(uid=self.uid,
                                                               path=path,
                                                               md5=file_md5,
                                                               sha256=file_sha256,
                                                               size=int(rand))

        # инициируем загрузку
        opts = {'uid': self.uid,
                'path': path,
                'md5': file_md5,
                'size': size,
                'callback': ''}
        store_call_result = self.json_ok('store', opts)
        assert 'oid' in store_call_result
        assert store_call_result['oid'] is not None
        oid = store_call_result['oid']

        with KladunStub(status_values=(body_1,)):
            # callback #1, state EXECUTING
            opts = {
                'uid': self.uid,
                'oid': oid,
                'status_xml': etree.tostring(body_1),
                'type': tags.COMMIT_FILE_INFO,
            }
            self.service_ok('kladun_callback', opts)

            status_one = self.json_ok('status', {'uid': self.uid, 'oid': oid})
            assert status_one['status'] == 'EXECUTING'
            assert status_one['state'] == 'EXECUTING'
            # проверяем другие форматтеры на случай
            self.mail_ok('status', {'uid': self.uid, 'oid': oid})
            self.desktop('status', {'uid': self.uid, 'oid': oid})

        with KladunStub(status_values=(body_2,)):
            # callback #2, state DONE
            opts = {
                'uid': self.uid,
                'oid': oid,
                'status_xml': etree.tostring(body_2),
                'type': tags.COMMIT_FILE_UPLOAD,
            }
            self.service_ok('kladun_callback', opts)

            status_two = self.json_ok('status', {'uid': self.uid, 'oid': oid})
            assert status_two['status'] == 'DONE'
            assert status_two['state'] == 'DONE'
            # проверяем другие форматтеры на случай
            self.mail_ok('status', {'uid': self.uid, 'oid': oid})
            self.desktop('status', {'uid': self.uid, 'oid': oid})

        with KladunStub(status_values=(body_3,)):
            # callback #3, state COMPLETED
            opts = {
                'uid': self.uid,
                'oid': oid,
                'status_xml': etree.tostring(body_3),
                'type': tags.COMMIT_FINAL,
            }
            self.service_ok('kladun_callback', opts)

            status_three = self.json_ok('status', {'uid': self.uid, 'oid': oid})
            assert status_three['status'] == 'DONE'
            assert status_three['state'] == 'COMPLETED'
            # проверяем другие форматтеры на случай
            self.mail_ok('status', {'uid': self.uid, 'oid': oid})
            self.desktop('status', {'uid': self.uid, 'oid': oid})

    def test_uploading_errors_logging(self):
        existed_path = '/disk/enot.jpg'
        self.upload_file(self.uid, existed_path)
        target_path = '/disk/fluffy_enot.jpg'
        body_1, body_2, body_3 = self.prepare_kladun_callbacks(uid=self.uid,
                                                               path=target_path)
        oid = self.json_ok('store', {'uid': self.uid,
                                     'path': target_path,
                                     'callback': ''})['oid']

        with KladunStub(status_values=(body_1,body_2,body_3)), \
                 mock.patch('mpfs.core.operations.base.error_log.error') as mocked_error_log, \
                 mock.patch('mpfs.core.operations.base.FEATURE_TOGGLES_SKIP_UPLOAD_ERRORS_FOR_REMOVED_RESOURCES',
                            False):
            self.service_ok('kladun_callback', {
                'uid': self.uid,
                'oid': oid,
                'status_xml': etree.tostring(body_1),
                'type': tags.COMMIT_FILE_INFO,
            })
            self.service_ok('kladun_callback', {
                'uid': self.uid,
                'oid': oid,
                'status_xml': etree.tostring(body_2),
                'type': tags.COMMIT_FILE_UPLOAD,
            })
            self.json_ok('rm', {'uid': self.uid, 'path': target_path})
            self.service_error('kladun_callback', {
                'uid': self.uid,
               'oid': oid,
               'status_xml': etree.tostring(body_3),
               'type': tags.COMMIT_FINAL,
            })
        mocked_error_log.assert_called()
        assert 'Operation failed with: ' in mocked_error_log.call_args_list[0][0][0]

    def test_moving_file_while_uploading(self):
        rand = str('%f' % time.time()).replace('.', '')[9:]
        file_md5 = hashlib.md5(rand).hexdigest()
        file_sha256 = hashlib.sha256(rand).hexdigest()
        size = int(rand)
        path = '/disk/fluffy_enot.jpg'

        body_1, body_2, body_3 = self.prepare_kladun_callbacks(uid=self.uid,
                                                               path=path,
                                                               md5=file_md5,
                                                               sha256=file_sha256,
                                                               size=int(rand))

        # инициируем загрузку
        store_call_result = self.json_ok('store', {'uid': self.uid,
                                                   'path': path,
                                                   'md5': file_md5,
                                                   'size': size,
                                                   'callback': ''})
        oid = store_call_result['oid']

        with KladunStub(status_values=(body_1,)):
            opts = {
                'uid': self.uid,
                'oid': oid,
                'status_xml': etree.tostring(body_1),
                'type': tags.COMMIT_FILE_INFO,
            }
            self.service_ok('kladun_callback', opts)


        with KladunStub(status_values=(body_2,)):
            opts = {
                'uid': self.uid,
                'oid': oid,
                'status_xml': etree.tostring(body_2),
                'type': tags.COMMIT_FILE_UPLOAD,
            }
            self.service_ok('kladun_callback', opts)

            self.json_ok('info', {'uid': self.uid, 'path': path})

        new_path = '/disk/fluffy_foxy.jpg'
        self.json_ok('move', {'uid': self.uid, 'src': path, 'dst': new_path})

        with KladunStub(status_values=(body_3,)):
            # Проверяем что callback будет успешным (раньше падали)
            opts = {
                'uid': self.uid,
                'oid': oid,
                'status_xml': etree.tostring(body_3),
                'type': tags.COMMIT_FINAL,
            }
            self.service_ok('kladun_callback', opts)

        # Проверяем, что файл доступен по новому пути
        info = self.json_ok('info', {'uid': self.uid, 'path': new_path})
        assert info.get('etime') is not None
        # И не доступен по старому
        self.json_error('info', {'uid': self.uid, 'path': path}, code=codes.RESOURCE_NOT_FOUND)

    def test_remove_while_uploading(self):
        rand = str('%f' % time.time()).replace('.', '')[9:]
        file_md5 = hashlib.md5(rand).hexdigest()
        file_sha256 = hashlib.sha256(rand).hexdigest()
        size = int(rand)
        path = '/disk/fluffy_enot.jpg'

        body_1, body_2, body_3 = self.prepare_kladun_callbacks(uid=self.uid,
                                                               path=path,
                                                               md5=file_md5,
                                                               sha256=file_sha256,
                                                               size=int(rand))

        # инициируем загрузку
        oid = self.json_ok('store', {'uid': self.uid,
                                     'path': path,
                                     'md5': file_md5,
                                     'size': size,
                                     'callback': ''})['oid']

        with KladunStub(status_values=(body_1, body_2, body_3)):
            self.service_ok('kladun_callback', {
                'uid': self.uid,
                'oid': oid,
                'status_xml': etree.tostring(body_1),
                'type': tags.COMMIT_FILE_INFO,
            })

            self.service_ok('kladun_callback', {
                'uid': self.uid,
                'oid': oid,
                'status_xml': etree.tostring(body_2),
                'type': tags.COMMIT_FILE_UPLOAD,
            })

            self.json_ok('rm', {'uid': self.uid, 'path': path})

            # Проверяем что callback будет успешным (раньше падали)
            self.service_ok('kladun_callback', {
                'uid': self.uid,
                'oid': oid,
                'status_xml': etree.tostring(body_3),
                'type': tags.COMMIT_FINAL,
            })

        self.json_error('info', {'uid': self.uid, 'path': path}, code=codes.RESOURCE_NOT_FOUND)

    def test_store_attach_with_hardlink_on_store_as_second_upload(self):
        """
        https://st.yandex-team.ru/CHEMODAN-20947

        Загрузка в аттачи через хардлинк должна работать
        """
        # грузим файл в аттачи просто так
        self.upload_file(self.uid, '/attach/somefile.txt')

        # грузим файл
        self.upload_file(self.uid, '/disk/somefile.txt')

        # получаем его данные
        info = self.json_ok('info', {'uid': self.uid, 'path': '/disk/somefile.txt', 'meta': ''})
        size, sha256, md5 = info['meta']['size'], info['meta']['sha256'], info['meta']['md5']

        with self.patch_mulca_is_file_exist(func_resp=True):
            # делаем загрузку хардлинком в attach
            first_result = self.json_ok(
                'store', {
                    'uid': self.uid,
                    'path': '/attach/somefile.txt',
                    'md5': md5,
                    'sha256': sha256,
                    'size': size
                }
            )
            assert first_result == {u'status': u'hardlinked'}

        with self.patch_mulca_is_file_exist(func_resp=False):
            # файла в мульке нет, заливаем обычным способом
            first_result = self.json_ok('store', {'uid': self.uid,
                                                  'path': '/attach/somefile.txt',
                                                  'md5': md5,
                                                  'sha256': sha256,
                                                  'size': size})
            assert first_result != {u'status': u'hardlinked'}

        # листаем аттачи и видим два файла
        attach_contents = self.json_ok('list', {'uid': self.uid, 'path': '/attach/', 'meta': ''})
        assert len(attach_contents) == 3

        filescount = 0
        for item in attach_contents:
            if item.get('meta', {}).get('original_name') == 'somefile.txt':
                filescount += 1
        assert filescount == 2

    def test_store_attach_with_hardlink_on_store_as_first_upload(self):
        """
        https://st.yandex-team.ru/CHEMODAN-20947

        Загрузка в аттачи через хардлинк должна работать даже в случае первой загрузки хардлинком
        """
        # грузим файл
        self.upload_file(self.uid, '/disk/somefile.txt')

        # получаем его данные
        info = self.json_ok('info', {'uid': self.uid, 'path': '/disk/somefile.txt', 'meta': ''})
        size, sha256, md5 = info['meta']['size'], info['meta']['sha256'], info['meta']['md5']

        # делаем загрузку хардлинком в attach
        with self.patch_mulca_is_file_exist(func_resp=True):
            first_result = self.json_ok(
                'store', {
                    'uid': self.uid,
                    'path': '/attach/somefile.txt',
                    'md5': md5,
                    'sha256': sha256,
                    'size': size
                }
            )
            assert first_result == {u'status': u'hardlinked'}

            # делаем еще загрузку хардлинком в attach
            second_result = self.json_ok(
                'store', {
                    'uid': self.uid,
                    'path': '/attach/somefile.txt',
                    'md5': md5,
                    'sha256': sha256,
                    'size': size
                }
            )
            assert second_result == {u'status': u'hardlinked'}

        # листаем аттачи и видим два файла
        attach_contents = self.json_ok('list', {'uid': self.uid, 'path': '/attach/', 'meta': ''})
        assert len(attach_contents) == 3

        filescount = 0
        for item in attach_contents:
            if item.get('meta', {}).get('original_name') == 'somefile.txt':
                filescount += 1
        assert filescount == 2

    def test_store_attach_with_hardlink_on_first_kladun_callback(self):
        """
        https://st.yandex-team.ru/CHEMODAN-21391

        Загрузка в аттачи при срабатывании хардлинка на первый коллбек от кладуна
        """
        # грузим файл
        self.upload_file(self.uid, '/disk/somefile.txt')

        # получаем его данные
        info = self.json_ok('info', {'uid': self.uid, 'path': '/disk/somefile.txt', 'meta': ''})
        size, sha256, md5 = info['meta']['size'], info['meta']['sha256'], info['meta']['md5']

        # получаем фейковые тела для кладунских коллбеков
        path = '/attach/someattach.txt'

        body_1, body_2, body_3 = self.prepare_kladun_callbacks(uid=self.uid,
                                                               path=path,
                                                               md5=md5,
                                                               sha256=sha256,
                                                               size=size)

        # создаем простую загрузку в аттачи
        oid = self.json_ok('store', {'uid': self.uid, 'path': '/attach/someattach.txt'}).get('oid')

        with KladunStub(status_values=(body_1,)):
            # делаем первый фейк-коллбек от Кладуна
            opts = {
                'uid': self.uid,
                'oid': oid,
                'status_xml': etree.tostring(body_1),
                'type': tags.COMMIT_FILE_INFO,
            }
            with self.patch_mulca_is_file_exist(func_resp=True):
                callback_result = self.service_error('kladun_callback', opts)
            assert callback_result == {u'code': 94, u'title': u'Kladun hardlink found'}

            status = self.json_ok('status', {'uid': self.uid, 'oid': oid})
            assert status['status'] == 'DONE'
            assert status['state'] == 'COMPLETED'

        # листаем аттачи и видим файл
        attach_contents = self.json_ok('list', {'uid': self.uid, 'path': '/attach/', 'meta': ''})
        assert len(attach_contents) == 2

        for item in attach_contents:
            if item.get('meta', {}).get('original_name') == 'someattach.txt':
                public_link = item.get('meta').get('short_url')
                assert public_link
                assert 'mail' in public_link
                assert 'hash' in public_link

    def test_store_with_hardlink_and_update_mids(self):
        """
        https://st.yandex-team.ru/CHEMODAN-18963

        При заливке файла через хардлинки делаем дополнительную проверку на наличие файлов в мульке.
        """
        # грузим файл
        with self.patch_mulca_is_file_exist(func_resp=False):
            self.upload_file(self.uid, '/disk/somefile.txt')

            # получаем его данные
            info = self.json_ok('info', {'uid': self.uid, 'path': '/disk/somefile.txt', 'meta': ''})
            size, sha256, md5 = info['meta']['size'], info['meta']['sha256'], info['meta']['md5']

            # получаем фейковые тела для кладунских коллбеков
            path = '/attach/someattach.txt'

            body_1, body_2, body_3 = self.prepare_kladun_callbacks(uid=self.uid,
                                                                   path=path,
                                                                   md5=md5,
                                                                   sha256=sha256,
                                                                   size=size)

            # создаем простую загрузку в аттачи
            oid = self.json_ok('store', {'uid': self.uid, 'path': '/attach/someattach.txt'}).get('oid')

            with KladunStub(status_values=(body_1,)):
                # делаем первый фейк-коллбек от Кладуна
                opts = {
                    'uid': self.uid,
                    'oid': oid,
                    'status_xml': etree.tostring(body_1),
                    'type': tags.COMMIT_FILE_INFO,
                }
                self.service_ok('kladun_callback', opts)

                status = self.json_ok('status', {'uid': self.uid, 'oid': oid})
                assert status['status'] == 'EXECUTING'
                assert status['state'] == 'EXECUTING'

            with KladunStub(status_values=(body_2,
                                           body_3)):
                # callback #2, state DONE
                opts = {
                    'uid': self.uid,
                    'oid': oid,
                    'status_xml': etree.tostring(body_2),
                    'type': tags.COMMIT_FILE_UPLOAD,
                }
                self.service_ok('kladun_callback', opts)

                # callback #3, state COMPLETED
                opts = {
                    'uid': self.uid,
                    'oid': oid,
                    'status_xml': etree.tostring(body_3),
                    'type': tags.COMMIT_FINAL
                }
                self.service_ok('kladun_callback', opts)

    def test_skipped_failure(self):
        """
        Тест на корректную обработку статуса status="skipped-failure" от кладуна

        Подробности: https://st.yandex-team.ru/CHEMODAN-21199
        """
        rand = str('%f' % time.time()).replace('.', '')[9:]
        file_md5 = hashlib.md5(rand).hexdigest()
        file_sha256 = hashlib.sha256(rand).hexdigest()
        size = int(rand)
        path = '/disk/test.jpg'

        body_1, body_2, body_3 = self.prepare_kladun_callbacks(uid=self.uid,
                                                               path=path,
                                                               md5=file_md5,
                                                               sha256=file_sha256,
                                                               size=int(rand))
        body_3.find('stages').find('exif-info').set('status', 'skipped-failure')

        # инициируем загрузку
        opts = {'uid': self.uid,
                'path': path,
                'md5': file_md5,
                'size': size,
                'callback': ''}
        store_call_result = self.json_ok('store', opts)
        assert 'oid' in store_call_result
        assert store_call_result['oid'] is not None
        oid = store_call_result['oid']

        with KladunStub(status_values=(body_1, body_1,
                                       body_2, body_2,
                                       body_3, body_3)):
            # callback #1, state EXECUTING
            opts = {
                'uid': self.uid,
                'oid': oid,
                'status_xml': etree.tostring(body_1),
                'type': tags.COMMIT_FILE_INFO,
            }
            self.service_ok('kladun_callback', opts)
            status_one = self.json_ok('status', {'uid': self.uid, 'oid': oid})
            assert status_one['status'] == 'EXECUTING'
            assert status_one['state'] == 'EXECUTING'

            # callback #2, state DONE
            opts = {
                'uid': self.uid,
                'oid': oid,
                'status_xml': etree.tostring(body_2),
                'type': tags.COMMIT_FILE_UPLOAD,
            }
            self.service_ok('kladun_callback', opts)

            status_two = self.json_ok('status', {'uid': self.uid, 'oid': oid})
            assert status_two['status'] == 'DONE'
            assert status_two['state'] == 'DONE'

            # callback #3, state COMPLETED
            opts = {
                'uid': self.uid,
                'oid': oid,
                'status_xml': etree.tostring(body_3),
                'type': tags.COMMIT_FINAL,
            }
            self.service_ok('kladun_callback', opts)

            status_three = self.json_ok('status', {'uid': self.uid, 'oid': oid})
            assert status_three['status'] == 'DONE'
            assert status_three['state'] == 'COMPLETED'

    def test_hardlink_zero_file_by_store(self):
        FILE_PATH = '/disk/empty-file.txt'

        result = self.json_ok(
            'store', {
                'uid': self.uid,
                'path': FILE_PATH,
                'md5': 'd41d8cd98f00b204e9800998ecf8427e',
                'sha256': 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
                'size': 0,
            }
        )
        assert 'status' in result
        assert result['status'] == 'hardlinked'

        # проверим, что реальный хардлинк не сработал и мы просто подставили некоторые предопределенные значения
        result = self.json_ok(
            'info', {
                'uid': self.uid,
                'path': FILE_PATH,
                'meta': 'file_mid,digest_mid'
            }
        )

        assert result['meta']['file_mid'] == SERVICES_MULCA_EMPTY_FILE_MID
        assert result['meta']['digest_mid'] == SERVICES_MULCA_EMPTY_FILE_DIGEST_MID

    def test_hardlink_zero_file_by_kladun_callback(self):
        FILE_PATH = '/disk/empty-file.txt'

        body_1, body_2, body_3 = self.prepare_kladun_callbacks(uid=self.uid,
                                                               path=FILE_PATH,
                                                               md5='d41d8cd98f00b204e9800998ecf8427e',
                                                               sha256='e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
                                                               size=0)

        # зовем store и убеждаемся, что создалась операция для покладки файла
        response = self.json_ok('store', {'uid': self.uid, 'path': FILE_PATH})
        assert 'oid' in response

        oid = response['oid']

        # делаем первый фейк-коллбек от Кладуна
        with KladunStub(status_values=(body_1,)):
            opts = {
                'uid': self.uid,
                'oid': oid,
                'status_xml': etree.tostring(body_1),
                'type': tags.COMMIT_FILE_INFO,
            }
            callback_result = self.service_error('kladun_callback', opts)
        assert callback_result == {u'code': 94, u'title': u'Kladun hardlink found'}

        # проверим статус операции
        status = self.json_ok('status', {'uid': self.uid, 'oid': oid})
        assert status['status'] == 'DONE'
        assert status['state'] == 'COMPLETED'

        # проверим, что реальный хардлинк не сработал и мы просто подставили некоторые предопределенные значения
        result = self.json_ok('info', {'uid': self.uid,
                                       'path': FILE_PATH,
                                       'meta': 'file_mid,digest_mid'})

        assert result['meta']['file_mid'] == SERVICES_MULCA_EMPTY_FILE_MID
        assert result['meta']['digest_mid'] == SERVICES_MULCA_EMPTY_FILE_DIGEST_MID

    def test_hardlink_zero_file_with_zero_hash_by_kladun_callback(self):
        file_path = '/disk/empty-file.txt'

        body_1, body_2, body_3 = self.prepare_kladun_callbacks(
            uid=self.uid,
            path=file_path,
            md5='d41d8cd98f00b204e9800998ecf8427e',
            sha256='e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
            size=0,
        )

        # зовем store и убеждаемся, что создалась операция для покладки файла
        response = self.json_ok('store', {
            'uid': self.uid,
            'path': file_path,
            'md5': '00000000000000000000000000000000',
            'sha256': '0000000000000000000000000000000000000000000000000000000000000000',
            'size': 0,
        })
        assert 'oid' in response

        oid = response['oid']

        # делаем первый фейк-коллбек от Кладуна
        with KladunStub(status_values=(body_1,)):
            opts = {
                'uid': self.uid,
                'oid': oid,
                'status_xml': etree.tostring(body_1),
                'type': tags.COMMIT_FILE_INFO,
            }
            callback_result = self.service_error('kladun_callback', opts)
        assert callback_result == {u'code': 94, u'title': u'Kladun hardlink found'}

        # проверим статус операции
        status = self.json_ok('status', {'uid': self.uid, 'oid': oid})
        assert status['status'] == 'DONE'
        assert status['state'] == 'COMPLETED'

        # проверим, что реальный хардлинк не сработал и мы просто подставили некоторые предопределенные значения
        result = self.json_ok('info', {'uid': self.uid,
                                       'path': file_path,
                                       'meta': 'file_mid,digest_mid'})

        assert result['meta']['file_mid'] == SERVICES_MULCA_EMPTY_FILE_MID
        assert result['meta']['digest_mid'] == SERVICES_MULCA_EMPTY_FILE_DIGEST_MID

    def test_hardlink_zero_file_random_file_ids(self):
        file_ids = set()

        for i in xrange(10):
            FILE_PATH = '/disk/empty-file-%d.txt' % i

            result = self.json_ok(
                'store', {
                    'uid': self.uid,
                    'path': FILE_PATH,
                    'md5': 'd41d8cd98f00b204e9800998ecf8427e',
                    'sha256': 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
                    'size': 0,
                }
            )
            assert 'status' in result
            assert result['status'] == 'hardlinked'

            # проверим, что реальный хардлинк не сработал и мы просто подставили некоторые предопределенные значения
            result = self.json_ok(
                'info', {
                    'uid': self.uid,
                    'path': FILE_PATH,
                    'meta': 'file_mid,digest_mid,file_id'
                }
            )

            assert result['meta']['file_mid'] == SERVICES_MULCA_EMPTY_FILE_MID
            assert result['meta']['digest_mid'] == SERVICES_MULCA_EMPTY_FILE_DIGEST_MID

            file_id = result['meta']['file_id']
            assert file_id not in file_ids
            file_ids.add(file_id)

    def test_store_with_predefined_hashes(self):
        first_file_path = '/disk/file.txt'
        second_file_path = '/disk/same-file.txt'
        self.upload_file(self.uid, first_file_path)

        result = self.json_ok('info', {'uid': self.uid, 'path': first_file_path, 'meta': 'md5,sha256,size,file_id'})
        first_file_meta = result['meta']

        with self.patch_mulca_is_file_exist(func_resp=True):
            result = self.json_ok(
                'store', {
                    'uid': self.uid,
                    'path': second_file_path,
                    'md5': first_file_meta['md5'],
                    'sha256': first_file_meta['sha256'],
                    'size': first_file_meta['size'],
                }
            )
        assert 'status' in result
        assert result['status'] == 'hardlinked'

    @parameterized.expand([
        ('md5', ['md5'],),
        ('sha256', ['sha256'],),
        ('both', ['md5', 'sha256'],),
    ])
    def test_store_with_uppercase_hashes(self, _, upper_hashes):
        first_file_path = '/disk/file.txt'
        second_file_path = '/disk/same-file.txt'
        self.upload_file(self.uid, first_file_path)

        result = self.json_ok('info', {'uid': self.uid, 'path': first_file_path, 'meta': 'md5,sha256,size'})
        first_file_meta = result['meta']
        for hash_name in upper_hashes:
            first_file_meta[hash_name] = first_file_meta[hash_name].upper()

        with self.patch_mulca_is_file_exist(func_resp=True):
            result = self.json_ok(
                'store', {
                    'uid': self.uid,
                    'path': second_file_path,
                    'md5': first_file_meta['md5'],
                    'sha256': first_file_meta['sha256'],
                    'size': first_file_meta['size'],
                }
            )
        assert 'status' in result
        assert result['status'] == 'hardlinked'

    @parameterized.expand([
        ('heif', 'image'),
        ('heic', 'image'),
        ('HEIC', 'image'),
        ('rp', 'unknown'),
    ])
    def test_media_type_by_ext(self, ext, media_type):
        file_path = '/disk/1.%s' % ext
        self.upload_file(self.uid, file_path)
        info = self.json_ok('info', {'uid': self.uid, 'path': file_path, 'meta': ''})
        assert info['meta']['media_type'] == media_type

    def test_skip_space_check(self):
        free = Quota().free(address=Address('%s:/disk/random' % self.uid))
        opts = {
            'uid': self.uid,
            'path': '/disk/random',
            'md5': '1bc29b36f623ba82aaf6724fd3b16718',
            'size': free + 1,
            'callback': '',
            'skip_check_space': '1',
        }
        with self.patch_mulca_is_file_exist(func_resp=False):
            result = self.json_ok('store', opts)
            assert 'upload_url' in result

    def test_store_proxied_tld_to_kladun(self):
        """Проверить что передаваемый извне GET-параметр `tld` пробрасывается в Кладун."""
        # https://st.yandex-team.ru/CHEMODAN-36706
        uid = self.uid

        with self.patch_mulca_is_file_exist(func_resp=False):
            with KladunStub() as stub:
                self.json_ok('store', {
                    'uid': uid,
                    'path': '/disk/test.txt',
                    'md5': 'de9ef78c8819c9ab88277e1aa13c1169',  # random
                    'sha256': 'f7df23b258eab15fac46f54881a1c63a1f1f46a9c8dfd749965e22225e8d1325',  # random
                    'size': '53',  # random
                    'tld': 'ua'
                })
                assert stub.upload_to_disk_post_request.called
                (data,), _ = stub.upload_to_disk_post_request.call_args
                assert 'tld' in data
                assert data['tld'] == 'ua'

    @staticmethod
    def generate_random_stid(uid):
        random_int = random.randint(10 ** 10, 10 ** 11)
        return '100000.yadisk:%s.%s' % (uid, random_int)

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='postgres test')
    def test_storage_fail_on_hardlink_with_updating_stids(self):
        """
        Тестируем следующий случай:
            1. Пользователь залил файл
            2. Пользователь залил такой же файл
            3. При второй заливке нашли хардлинк
            4. При хардлинке пошли в сторадж и он отдал ошибку (500 или еще что)
            5. Мы залили файл заново и обновили стиды у файла в таблице storage_files
            6. Старые стиды попали в чистку (DeletedStids)
        """

        file_1_path = '/disk/test-1.txt'
        file_2_path = '/disk/test-2.txt'
        file_1_stid = self.generate_random_stid(self.uid)

        self.upload_file(self.uid, file_1_path, file_data={'file_mid': file_1_stid})
        file_1_info = self.json_ok('info', {'uid': self.uid, 'path': file_1_path, 'meta': 'md5,sha256,size,file_mid'})

        with mock.patch('mpfs.core.filesystem.cleaner.controllers.DeletedStidsController.bulk_create') as stids_stub:
            with mock.patch.object(AbstractLink, 'is_file_in_storage', return_value=False):
                self.upload_file(
                    self.uid,
                    file_2_path,
                    file_data={
                        'md5': file_1_info['meta']['md5'],
                        'sha256': file_1_info['meta']['sha256'],
                        'size': file_1_info['meta']['size'],
                        'file_mid': self.generate_random_stid(self.uid),
                    },
                )
                assert stids_stub.called_once()
                deleted_stid = stids_stub.call_args[0][0][0]
                assert file_1_stid in deleted_stid.stid

        file_2_info = self.json_ok('info', {'uid': self.uid, 'path': file_2_path, 'meta': 'md5,sha256,size,file_mid'})
        assert file_2_info['meta']['file_mid'] != file_1_info['meta']['file_mid']

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='postgres test')
    def test_simultaneous_same_file_upload(self):
        self.create_user(self.uid_3)

        file_path = '/disk/test-1.txt'
        self.upload_file(self.uid, file_path)

        file_info = self.json_ok('info', {'uid': self.uid, 'path': file_path, 'meta': 'md5,sha256,size'})

        with mock.patch('mpfs.dao.session.Session.clear_cache', return_value=None):
            from mpfs.dao.session import Session

            session_1 = Session.create_from_uid(self.uid)
            session_1.begin()

            self.upload_file(self.uid, '/disk/test-2.txt', file_data={
                'md5': file_info['meta']['md5'],
                'sha256': file_info['meta']['sha256'],
                'size': file_info['meta']['size']

            })

            session_1.detach_from_cache()

            session_2 = Session.create_from_uid(self.uid_3)
            session_2.begin()

            with mock.patch.object(AbstractLink, 'is_file_in_storage', return_value=False):
                self.upload_file(self.uid_3, '/disk/test-3.txt', file_data={
                    'md5': file_info['meta']['md5'],
                    'sha256': file_info['meta']['sha256'],
                    'size': file_info['meta']['size'],
                    'file_mid': self.generate_random_stid(self.uid_3),
                })

        session_2.commit()
        session_1.commit()

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='postgres test')
    def test_update_file_on_overwrite(self):
        file_path = '/disk/test-1.txt'

        self.upload_file(self.uid, file_path)
        self.json_ok('info', {'uid': self.uid, 'path': file_path, 'meta': 'md5,sha256,size,file_mid'})

        db = CollectionRoutedDatabase()
        db_item = db.user_data.find_one({'uid': self.uid, 'key': file_path})
        db_item['parent'] = '/disk'
        new_mtime = 123123123
        db_item['data']['mtime'] = new_mtime
        db.user_data.update({'uid': self.uid, 'path': file_path}, db_item, upsert=True)

        file_2_info = self.json_ok('info', {'uid': self.uid, 'path': file_path, 'meta': 'mtime'})
        assert file_2_info['mtime'] == new_mtime

    # https://st.yandex-team.ru/CHEMODAN-41657
    # https://st.yandex-team.ru/CHEMODAN-43109
    @parameterized.expand([
        ('client_etime_is_saved_for_image', '/disk/some.jpg', '/disk/some.jpg', None, '123456', None, None, None, 123456),
        ('client_etime_is_saved_for_video', '/disk/some.mov', '/disk/some.mov', None, '123456', None, None, None, 123456),
        ('client_etime_is_not_saved_for_document', '/disk/some.doc', '/disk/some.doc', None, '123456', None, None, None, None),
        ('client_etime_is_saved_for_image_mimetype', '/disk/some', '/disk/some', 'image/png', '123456', None, None, None, 123456),
        ('kladun_does_not_overwrite_client_etime', '/disk/some.jpg', '/disk/some.jpg', None, '123456', None, None, '2018-01-01T12:00:00Z', 123456),
        ('zero_client_etime_is_not_saved', '/disk/some.jpg', '/disk/some.jpg', None, '0', None, None, None, None),

        ('client_mtime_is_not_ignored_for_non_photostream_folders', '/disk/some.jpg', '/disk/some.jpg', None, None, '123456', None, None, 123456),
        ('client_mtime_is_saved_for_image', '/photostream/some.jpg', u'/disk/Фотокамера/some.jpg', None, None, '123456', None, None, 123456),
        ('client_mtime_is_saved_for_video', '/photostream/some.mov', u'/disk/Фотокамера/some.mov', None, None, '123456', None, None, 123456),
        ('client_mtime_is_not_saved_for_document', '/photostream/some.doc', u'/disk/Фотокамера/some.doc', None, None, '123456', None, None, None),
        ('client_mtime_is_saved_for_image_mimetype', '/photostream/some', u'/disk/Фотокамера/some', 'image/png', None, '123456', None, None, 123456),
        ('kladun_does_not_overwrite_client_mtime', '/photostream/some.jpg', u'/disk/Фотокамера/some.jpg', None, None, '123456', None, '2018-01-01T12:00:00Z', 123456),
        ('kladun_does_not_overwrite_client_mtime_and_zero_etime', '/photostream/some.jpg', u'/disk/Фотокамера/some.jpg', None, '0', '123456', None, '2018-01-01T12:00:00Z', 123456),

        ('client_ctime_is_not_ignored_for_non_photostream_folders', '/disk/some.jpg', '/disk/some.jpg', None, None, None, '123456', None, 123456),
        ('client_ctime_is_saved_for_image', '/photostream/some.jpg', u'/disk/Фотокамера/some.jpg', None, None, None, '123456', None, 123456),
        ('client_ctime_is_saved_for_video', '/photostream/some.mov', u'/disk/Фотокамера/some.mov', None, None, None, '123456', None, 123456),
        ('client_ctime_is_not_saved_for_document', '/photostream/some.doc', u'/disk/Фотокамера/some.doc', None, None, None, '123456', None, None),
        ('client_ctime_is_saved_for_image_mimetype', '/photostream/some', u'/disk/Фотокамера/some', 'image/png', None, None, '123456', None, 123456),
        ('kladun_does_not_overwrite_client_ctime', '/photostream/some.jpg', u'/disk/Фотокамера/some.jpg', None, None, None, '123456', '2018-01-01T12:00:00Z', 123456),
        ('kladun_does_not_overwrite_client_ctime_and_zero_etime', '/photostream/some.jpg', u'/disk/Фотокамера/some.jpg', None, '0', None, '123456', '2018-01-01T12:00:00Z', 123456),

        ('client_etime_overwrites_client_ctime', '/photostream/some.jpg', u'/disk/Фотокамера/some.jpg', None, '123456', None, '1234567', None, 123456),
        ('client_etime_overwrites_client_mtime', '/photostream/some.jpg', u'/disk/Фотокамера/some.jpg', None, '123456', '1234567', None, None, 123456),
        ('client_ctime_overwrites_client_mtime', '/photostream/some.jpg', u'/disk/Фотокамера/some.jpg', None, None, '1234567', '123456', None, 123456),
        ('zero_client_mtime_is_not_saved', '/photostream/some.jpg', u'/disk/Фотокамера/some.jpg', None, None, '0', None, None, None),
        ('zero_client_mtime_is_not_saved', '/photostream/some.jpg', u'/disk/Фотокамера/some.jpg', None, None, None, '0', None, None),
    ])
    def test_store_with_client_etime_and_mtime(self, _, store_path, info_path, mimetype, etime_from_client, mtime_from_client, ctime_from_client, etime_from_kladun, expected_etime):
        file_data = {'etime': etime_from_kladun}
        if mimetype:
            file_data['mimetype'] = mimetype

        opts = {}
        if etime_from_client is not None:
            opts['etime'] = etime_from_client
        if mtime_from_client is not None:
            opts['mtime'] = mtime_from_client
        if ctime_from_client is not None:
            opts['ctime'] = ctime_from_client

        self.upload_file(self.uid, store_path, opts=opts, file_data=file_data)
        result = self.json_ok('info', {'uid': self.uid, 'path': info_path, 'meta': ''})
        if expected_etime is None:
            assert 'etime' not in result['meta']
        else:
            assert 'etime' in result['meta']
            assert result['meta']['etime'] == expected_etime

    # https://st.yandex-team.ru/CHEMODAN-41657
    # https://st.yandex-team.ru/CHEMODAN-43109
    @parameterized.expand([
        ('client_etime_is_saved_for_image', '/disk/some.jpg', '/disk/some.jpg', None, '123456', None, None, None, 123456),
        ('client_etime_is_saved_for_video', '/disk/some.mov', '/disk/some.mov', None, '123456', None, None, None, 123456),
        ('client_etime_is_not_saved_for_document', '/disk/some.doc', '/disk/some.doc', None, '123456', None, None, None, None),
        ('client_etime_is_saved_for_image_mimetype', '/disk/some', '/disk/some', 'image/png', '123456', None, None, None, 123456),
        ('kladun_does_not_overwrite_client_etime', '/disk/some.jpg', '/disk/some.jpg', None, '123456', None, None, '2018-01-01T12:00:00Z', 123456),
        ('zero_client_etime_is_not_saved', '/disk/some.jpg', '/disk/some.jpg', None, '0', None, None, None, None),

        ('client_mtime_is_not_ignored_for_non_photostream_folders', '/disk/some.jpg', '/disk/some.jpg', None, None, '123456', None, None, 123456),
        ('client_mtime_is_saved_for_image', '/photostream/some.jpg', u'/disk/Фотокамера/some.jpg', None, None, '123456', None, None, 123456),
        ('client_mtime_is_saved_for_video', '/photostream/some.mov', u'/disk/Фотокамера/some.mov', None, None, '123456', None, None, 123456),
        ('client_mtime_is_not_saved_for_document', '/photostream/some.doc', u'/disk/Фотокамера/some.doc', None, None, '123456', None, None, None),
        ('client_mtime_is_saved_for_image_mimetype', '/photostream/some', u'/disk/Фотокамера/some', 'image/png', None, '123456', None, None, 123456),
        ('kladun_does_not_overwrite_client_mtime', '/photostream/some.jpg', u'/disk/Фотокамера/some.jpg', None, None, '123456', None, '2018-01-01T12:00:00Z', 123456),
        ('kladun_does_not_overwrite_client_mtime_and_zero_etime', '/photostream/some.jpg', u'/disk/Фотокамера/some.jpg', None, '0', '123456', None, '2018-01-01T12:00:00Z', 123456),

        ('client_ctime_is_not_ignored_for_non_photostream_folders', '/disk/some.jpg', '/disk/some.jpg', None, None, None, '123456', None, 123456),
        ('client_ctime_is_saved_for_image', '/photostream/some.jpg', u'/disk/Фотокамера/some.jpg', None, None, None, '123456', None, 123456),
        ('client_ctime_is_saved_for_video', '/photostream/some.mov', u'/disk/Фотокамера/some.mov', None, None, None, '123456', None, 123456),
        ('client_ctime_is_not_saved_for_document', '/photostream/some.doc', u'/disk/Фотокамера/some.doc', None, None, None, '123456', None, None),
        ('client_ctime_is_saved_for_image_mimetype', '/photostream/some', u'/disk/Фотокамера/some', 'image/png', None, None, '123456', None, 123456),
        ('kladun_does_not_overwrite_client_ctime', '/photostream/some.jpg', u'/disk/Фотокамера/some.jpg', None, None, None, '123456', '2018-01-01T12:00:00Z', 123456),
        ('kladun_does_not_overwrite_client_ctime_and_zero_etime', '/photostream/some.jpg', u'/disk/Фотокамера/some.jpg', None, '0', None, '123456', '2018-01-01T12:00:00Z', 123456),

        ('client_etime_overwrites_client_ctime', '/photostream/some.jpg', u'/disk/Фотокамера/some.jpg', None, '123456', None, '1234567', None, 123456),
        ('client_etime_overwrites_client_mtime', '/photostream/some.jpg', u'/disk/Фотокамера/some.jpg', None, '123456', '1234567', None, None, 123456),
        ('client_ctime_overwrites_client_mtime', '/photostream/some.jpg', u'/disk/Фотокамера/some.jpg', None, None, '1234567', '123456', None, 123456),
        ('zero_client_mtime_is_not_saved', '/photostream/some.jpg', u'/disk/Фотокамера/some.jpg', None, None, '0', None, None, None),
        ('zero_client_mtime_is_not_saved', '/photostream/some.jpg', u'/disk/Фотокамера/some.jpg', None, None, None, '0', None, None),
    ])
    def test_store_with_client_etime_and_mtime_hardlink_from_kladun(self, _, store_path, info_path, mimetype, etime_from_client, mtime_from_client, ctime_from_client, etime_from_kladun, expected_etime):
        md5 = hashlib.md5('1').hexdigest()
        sha256 = hashlib.sha256('1').hexdigest()
        file_data = {'md5': md5, 'sha256': sha256, 'size': 1, 'etime': etime_from_kladun}
        if mimetype:
            file_data['mimetype'] = mimetype
        self.create_user(self.uid_3)
        self.upload_file(self.uid_3, '/disk/for_hardlink', file_data=file_data)

        opts = {}
        if etime_from_client is not None:
            opts['etime'] = etime_from_client
        if mtime_from_client is not None:
            opts['mtime'] = mtime_from_client
        if ctime_from_client is not None:
            opts['ctime'] = ctime_from_client

        self.upload_file(self.uid, store_path, hardlink=True, opts=opts, file_data=file_data)
        result = self.json_ok('info', {'uid': self.uid, 'path': info_path, 'meta': ''})
        if expected_etime is None:
            assert 'etime' not in result['meta']
        else:
            assert 'etime' in result['meta']
            assert result['meta']['etime'] == expected_etime

    # https://st.yandex-team.ru/CHEMODAN-41657
    # https://st.yandex-team.ru/CHEMODAN-43109
    @parameterized.expand([
        ('client_etime_is_saved_for_image', '/disk/some.jpg', '/disk/some.jpg', None, '123456', None, None, None, 123456),
        ('client_etime_is_saved_for_video', '/disk/some.mov', '/disk/some.mov', None, '123456', None, None, None, 123456),
        ('client_etime_is_not_saved_for_document', '/disk/some.doc', '/disk/some.doc', None, '123456', None, None, None, None),
        ('client_etime_is_saved_for_image_mimetype', '/disk/some', '/disk/some', 'image/png', '123456', None, None, None, 123456),
        ('kladun_does_not_overwrite_client_etime', '/disk/some.jpg', '/disk/some.jpg', None, '123456', None, None, '2018-01-01T12:00:00Z', 123456),
        ('zero_client_etime_is_not_saved', '/disk/some.jpg', '/disk/some.jpg', None, '0', None, None, None, None),

        ('client_mtime_is_not_ignored_for_non_photostream_folders', '/disk/some.jpg', '/disk/some.jpg', None, None, '123456', None, None, 123456),
        ('client_mtime_is_saved_for_image', '/photostream/some.jpg', u'/disk/Фотокамера/some.jpg', None, None, '123456', None, None, 123456),
        ('client_mtime_is_saved_for_video', '/photostream/some.mov', u'/disk/Фотокамера/some.mov', None, None, '123456', None, None, 123456),
        ('client_mtime_is_not_saved_for_document', '/photostream/some.doc', u'/disk/Фотокамера/some.doc', None, None, '123456', None, None, None),
        ('client_mtime_is_saved_for_image_mimetype', '/photostream/some', u'/disk/Фотокамера/some', 'image/png', None, '123456', None, None, 123456),
        ('kladun_does_not_overwrite_client_mtime', '/photostream/some.jpg', u'/disk/Фотокамера/some.jpg', None, None, '123456', None, '2018-01-01T12:00:00Z', 123456),
        ('kladun_does_not_overwrite_client_mtime_and_zero_etime', '/photostream/some.jpg', u'/disk/Фотокамера/some.jpg', None, '0', '123456', None, '2018-01-01T12:00:00Z', 123456),

        ('client_ctime_is_not_ignored_for_non_photostream_folders', '/disk/some.jpg', '/disk/some.jpg', None, None, None, '123456', None, 123456),
        ('client_ctime_is_saved_for_image', '/photostream/some.jpg', u'/disk/Фотокамера/some.jpg', None, None, None, '123456', None, 123456),
        ('client_ctime_is_saved_for_video', '/photostream/some.mov', u'/disk/Фотокамера/some.mov', None, None, None, '123456', None, 123456),
        ('client_ctime_is_not_saved_for_document', '/photostream/some.doc', u'/disk/Фотокамера/some.doc', None, None, None, '123456', None, None),
        ('client_ctime_is_saved_for_image_mimetype', '/photostream/some', u'/disk/Фотокамера/some', 'image/png', None, None, '123456', None, 123456),
        ('kladun_does_not_overwrite_client_ctime', '/photostream/some.jpg', u'/disk/Фотокамера/some.jpg', None, None, None, '123456', '2018-01-01T12:00:00Z', 123456),
        ('kladun_does_not_overwrite_client_ctime_and_zero_etime', '/photostream/some.jpg', u'/disk/Фотокамера/some.jpg', None, '0', None, '123456', '2018-01-01T12:00:00Z', 123456),

        ('client_etime_overwrites_client_ctime', '/photostream/some.jpg', u'/disk/Фотокамера/some.jpg', None, '123456', None, '1234567', None, 123456),
        ('client_etime_overwrites_client_mtime', '/photostream/some.jpg', u'/disk/Фотокамера/some.jpg', None, '123456', '1234567', None, None, 123456),
        ('client_ctime_overwrites_client_mtime', '/photostream/some.jpg', u'/disk/Фотокамера/some.jpg', None, None, '1234567', '123456', None, 123456),
        ('zero_client_mtime_is_not_saved', '/photostream/some.jpg', u'/disk/Фотокамера/some.jpg', None, None, '0', None, None, None),
        ('zero_client_mtime_is_not_saved', '/photostream/some.jpg', u'/disk/Фотокамера/some.jpg', None, None, None, '0', None, None),
    ])
    def test_store_with_client_etime_and_mtime_hardlink(self, _, store_path, info_path, mimetype, etime_from_client, mtime_from_client, ctime_from_client, etime_from_kladun, expected_etime):
        md5 = hashlib.md5('1').hexdigest()
        sha256 = hashlib.sha256('1').hexdigest()
        file_data = {'md5': md5, 'sha256': sha256, 'size': 1, 'etime': etime_from_kladun}
        if mimetype:
            file_data['mimetype'] = mimetype
        self.create_user(self.uid_3)
        self.upload_file(self.uid_3, '/disk/for_hardlink', file_data=file_data)

        opts = {'uid': self.uid, 'path': store_path, 'md5': md5, 'sha256': sha256, 'size': 1}
        if etime_from_client is not None:
            opts['etime'] = etime_from_client
        if mtime_from_client is not None:
            opts['mtime'] = mtime_from_client
        if ctime_from_client is not None:
            opts['ctime'] = ctime_from_client

        with mock.patch('mpfs.core.services.mulca_service.Mulca.is_file_exist', return_value=True):
            result = self.json_ok('store', opts)
            assert result == {'status': 'hardlinked'}
        result = self.json_ok('info', {'uid': self.uid, 'path': info_path, 'meta': ''})
        if expected_etime is None:
            assert 'etime' not in result['meta']
        else:
            assert 'etime' in result['meta']
            assert result['meta']['etime'] == expected_etime

    def test_store_param_disk_client_file_id_not_saved(self):
        path = '/disk/file'
        self.upload_file(self.uid, path, opts={'disk_client_file_id': '1'})

        db = CollectionRoutedDatabase()
        record = db.user_data.find_one({'uid': self.uid, 'key': path})
        zdata = record.pop('zdata')
        if isinstance(zdata, str):
            zdata = from_json(zdata.decode('zlib'))
        assert '"disk_client_file_id"' not in to_json(record)
        assert '"disk_client_file_id"' not in to_json(zdata)

    def test_store_error_for_uninititialized_user(self):
        opts = {'uid': '1254', 'path': '/disk/file'}
        self.json_error('store', opts, code=codes.WH_USER_NEED_INIT)

    def test_store_error_for_blocked_user(self):
        self.support_ok('block_user', {
            'uid': self.uid,
            'moderator': 'moderator',
            'comment': 'comment',
        })
        opts = {'uid': self.uid, 'path': '/disk/file'}
        self.json_error('store', opts, code=codes.USER_BLOCKED)

    def test_kladun_callback_twice(self):
        rand = str('%f' % time.time()).replace('.', '')[9:]
        file_md5 = hashlib.md5(rand).hexdigest()
        file_sha256 = hashlib.sha256(rand).hexdigest()
        size = int(rand)
        path = '/disk/test.jpg'

        body_1, body_2, body_3 = self.prepare_kladun_callbacks(uid=self.uid,
                                                               path=path,
                                                               md5=file_md5,
                                                               sha256=file_sha256,
                                                               size=int(rand))

        # инициируем загрузку
        opts = {'uid': self.uid,
                'path': path,
                'md5': file_md5,
                'size': size,
                'callback': ''}
        store_call_result = self.json_ok('store', opts)
        assert 'oid' in store_call_result
        assert store_call_result['oid'] is not None
        oid = store_call_result['oid']
        with KladunStub(status_values=(body_1, body_2, body_2)), \
            mock.patch('mpfs.core.filesystem.events.FilesystemStoreEvent.send_self_or_group') as send:
            opts = {'uid': self.uid, 'oid': oid, 'status_xml': etree.tostring(body_1), 'type': tags.COMMIT_FILE_INFO}
            self.service_ok('kladun_callback', opts)
            assert send.call_count == 0
            opts = {'uid': self.uid, 'oid': oid, 'status_xml': etree.tostring(body_2), 'type': tags.COMMIT_FILE_UPLOAD}
            self.service_ok('kladun_callback', opts)
            assert send.call_count == 1
            opts = {'uid': self.uid, 'oid': oid, 'status_xml': etree.tostring(body_2), 'type': tags.COMMIT_FILE_UPLOAD}
            self.service_ok('kladun_callback', opts)
            assert send.call_count == 1


    def test_kladun_callback_error_for_blocked_user(self):
        self.support_ok('block_user', {
            'uid': self.uid,
            'moderator': 'moderator',
            'comment': 'comment',
        })
        opts = {'uid': self.uid, 'oid': 'fake_oid', 'status_xml': 'fake'}
        self.service_error('kladun_callback', opts, code=codes.USER_BLOCKED)

    def test_kladun_callback_callable_for_attached_user(self):
        self.create_user(self.uid, noemail=1)
        self.remove_created_users()
        AttachUser.Create(self.uid)
        user = User(self.uid)
        assert isinstance(user, AttachUser)

        userinfo = passport.userinfo(self.uid)
        userinfo['has_disk'] = False

        with PassportStub(userinfo=userinfo):
            opts = {'uid': self.uid, 'oid': 'fake_oid', 'status_xml': 'fake'}
            # expect bad request cause request is really bad but the error means that method was called
            self.service_error('kladun_callback', opts, code=codes.BAD_REQUEST_ERROR)

    def test_kladun_download_counter_inc_skip_for_blocked_user(self):
        self.support_ok('block_user', {
            'uid': self.uid,
            'moderator': 'moderator',
            'comment': 'comment',
        })

        opts = {
            'uid': self.uid,
            'hash': CryptAgent().encrypt('%s:%s' % (self.uid, 'file')),
            'bytes_downloaded': 100,
            }
        with mock.patch('mpfs.core.social.publicator.Publicator.download_counter_inc') as publicator_mock:
            self.service_ok('kladun_download_counter_inc', opts)
            assert not publicator_mock.called

    def test_kladun_download_counter_inc_skip_for_blocked_user_public_file(self):
        file_name = '/disk/file.txt'
        self.upload_file(self.uid, file_name)
        self.json_ok('set_public', {'uid': self.uid, 'path': file_name})
        public_address = Address.Make(self.uid, file_name).id
        resource = Bus().resource(self.uid, public_address)
        hash_ = resource.get_public_hash()

        with mock.patch('mpfs.core.social.publicator.Publicator.download_counter_inc') as publicator_mock:
            self.service_ok('kladun_download_counter_inc', {'hash': hash_, 'bytes': '42'})
            assert publicator_mock.called

        self.support_ok('block_user', {
            'uid': self.uid,
            'moderator': 'moderator',
            'comment': 'comment',
        })

        with mock.patch('mpfs.core.social.publicator.Publicator.download_counter_inc') as publicator_mock:
            self.service_ok('kladun_download_counter_inc', {'hash': hash_, 'bytes': '42'})
            assert not publicator_mock.called

    def test_mail_store_ok_for_uninitialized_user(self):
        opts = {'uid': '1254', 'path': '/attach/file'}
        self.mail_ok('store', opts)

    @parameterized.expand([('ru', users.default_user.uid, 'ru'),
                           ('tr', users.turkish_user.uid, 'tr')])
    def test_mail_store_for_user_under_experiment(self, case_name, uid, locale):
        file_name = 'pack.deb'
        mpfs_original_path = '/attach/%s' % file_name
        default_attach_dir = DEFAULT_FOLDERS[DEFAULT_FOLDERS_NAMES.ATTACH][locale]
        expected_mpfs_path = u'%s:%s/' % (uid, default_attach_dir)
        opts = {'uid': uid, 'path': mpfs_original_path}
        with mock.patch('mpfs.common.util.experiments.logic.ExperimentManager.is_feature_active', return_value=True):
            store_result = self.mail_ok('attach_store', opts)

        oid = store_result.find("oid").text
        actual_path = self.json_ok('status', {'uid': uid, 'oid': oid})['params']['path']
        assert actual_path.startswith(expected_mpfs_path)

    def test_attach_store_wo_speed_limit(self):
        file_name = 'dancing raccoon.mp4'
        mpfs_original_path = '/attach/%s' % file_name
        opts = {'uid': self.uid, 'path': mpfs_original_path}
        with mock.patch('mpfs.common.util.experiments.logic.ExperimentManager.is_feature_active', return_value=True):
            store_result = self.json_ok('attach_store', opts, headers={'Yandex-Cloud-Request-ID': 'rest-123'})

        operation = manager.get_operation(self.uid, store_result['oid'])
        assert_that(operation.dict()['data'], is_not(has_key('upload-max-speed-bps')))

    def test_mail_store_for_user_wo_free_space(self):
        file_name = 'pack.deb'
        mpfs_original_path = '/attach/%s' % file_name
        opts = {'uid': self.uid, 'path': mpfs_original_path}
        with mock.patch('mpfs.common.util.experiments.logic.ExperimentManager.is_feature_active', return_value=True), \
             mock.patch('mpfs.core.services.disk_service.MPFSStorageService.free',
                        return_value=0):
            store_result = self.mail_error('attach_store', opts, status=507)

        error_text = store_result.find('error').find('title').text
        assert error_text == "Unable to store file: no free space within limit"

    def test_mail_store_error_for_blocked_user(self):
        self.support_ok('block_user', {
            'uid': self.uid,
            'moderator': 'moderator',
            'comment': 'comment',
        })
        opts = {'uid': self.uid, 'path': '/attach/file'}
        self.mail_error('store', opts, code=codes.USER_BLOCKED)

    def _prepare_files_for_hardlink(self, same_user):
        if same_user:
            uid1 = uid2 = self.uid
        else:
            uid1 = self.uid
            uid2 = self.uid_3
            self.create_user(uid2)

        file1_info = {
            'path': '/disk/some_file.txt',
            'file_data': {
                'md5': hashlib.md5('1').hexdigest(),
                'sha256': hashlib.sha256('1').hexdigest(),
                'size': 1
            }
        }

        file2_info = {
            'path': '/disk/some_another_file.txt',
            'file_data': {
                'md5': hashlib.md5('2').hexdigest(),
                'sha256': hashlib.sha256('2').hexdigest(),
                'size': 2
            }
        }

        self.upload_file(uid1, **file1_info)
        self.upload_file(uid2, **file2_info)
        return uid1, file1_info, uid2, file2_info

    def _assert_file_is_public(self, file_meta, private_hash):
        assert file_meta.get('public')
        assert file_meta.get('public_hash') == private_hash
        self.json_ok('public_url', {'private_hash': file_meta.get('public_hash')})

    @parameterized.expand([
        ('from_kladun_keeps_public_link_intact_for_same_user', True, True),
        ('from_kladun_keeps_public_link_intact_for_different_users', True, False),
        ('keeps_public_link_intact_for_same_user', False, True),
        ('keeps_public_link_intact_for_different_users', False, False),
    ])
    def test_overwrite_public_file_with_hardlink(self, _, use_kladun, same_user):
        uid1, file1_info, uid2, file2_info = self._prepare_files_for_hardlink(same_user)

        # делаем первый файл публичным
        private_hash = self.json_ok('set_public', {'uid': uid1, 'path': file1_info['path']})['hash']

        # перезаписываем первый файл так, чтобы схардлинкалось со вторым файлом
        if use_kladun:
            self.upload_file(uid1, hardlink=True, force=1, path=file1_info['path'], file_data=file2_info['file_data'])
        else:
            with mock.patch('mpfs.core.services.mulca_service.Mulca.is_file_exist', return_value=True):
                self.json_ok('store', dict(uid=uid1, force=1, path=file1_info['path'], **file2_info['file_data']))

        file1_meta = self.json_ok('info', {'uid': uid1, 'path': file1_info['path'], 'meta': ''})['meta']
        file2_meta = self.json_ok('info', {'uid': uid2, 'path': file2_info['path'], 'meta': ''})['meta']

        # Проверяем, что файлы схардлинкались
        assert file1_meta['file_mid'] == file2_meta['file_mid']

        # Проверяем, что первый файл остался публичным
        self._assert_file_is_public(file1_meta, private_hash)

    @parameterized.expand([
        ('does_not_copy_public_link_for_different_users', False, False),
        ('from_kladun_does_not_copy_public_link_for_different_users', True, False),
        ('does_not_copy_public_link_for_same_user', False, True),
        ('from_kladun_does_not_copy_public_link_for_same_user', True, True),
    ])
    def test_overwrite_with_hardlink_to_public_file(self, _, use_kladun, same_user):
        uid1, file1_info, uid2, file2_info = self._prepare_files_for_hardlink(same_user)

        # делаем файл первого пользователя публичным
        private_hash = self.json_ok('set_public', {'uid': uid1, 'path': file1_info['path']})['hash']

        # перезаписываем второй файл так, чтобы схардлинкалось с первым файлом
        if use_kladun:
            self.upload_file(uid2, hardlink=True, force=1, path=file2_info['path'], file_data=file1_info['file_data'])
        else:
            with mock.patch('mpfs.core.services.mulca_service.Mulca.is_file_exist', return_value=True):
                self.json_ok('store', dict(uid=uid2, force=1, path=file2_info['path'], **file1_info['file_data']))

        file1_meta = self.json_ok('info', {'uid': uid1, 'path': file1_info['path'], 'meta': ''})['meta']
        file2_meta = self.json_ok('info', {'uid': uid2, 'path': file2_info['path'], 'meta': ''})['meta']

        # Проверяем, что файлы схардлинкались
        assert file1_meta['file_mid'] == file2_meta['file_mid']

        # Проверяем, что первый файл остался публичным
        self._assert_file_is_public(file1_meta, private_hash)

        # А у второй по прежнему не публичный
        assert not file2_meta.get('public')
        assert not file2_meta.get('public_hash')

    @parameterized.expand([
        ('for_different_users', False, False, False),
        ('for_same_user', False, True, False),
        ('from_kladun_for_different_users', True, False, False),
        ('from_kladun_for_same_user', True, True, False),
        ('for_different_users_with_full_regenerate_preview_response', False, False, True),
        ('for_same_user_with_full_regenerate_preview_response', False, True, True),
        ('from_kladun_for_different_users_with_full_regenerate_preview_response', True, False, True),
        ('from_kladun_for_same_user_with_full_regenerate_preview_response', True, True, True),
    ])
    def test_regenerate_broken_preview_on_hardlink(self, _, use_kladun, same_user, full_regenerate_preview_response):
        if same_user:
            uid1 = uid2 = self.uid
        else:
            uid1 = self.uid
            uid2 = self.uid_3
            self.create_user(uid2)

        path1 = '/disk/some_file_1.txt'
        path2 = '/disk/some_file_2.txt'

        file_data = {
            'md5': hashlib.md5('1').hexdigest(),
            'sha256': hashlib.sha256('1').hexdigest(),
            'size': 1
        }

        self.upload_file(uid1, path=path1, file_data=file_data)
        file_meta_1 = self.json_ok('info', {'uid': uid1, 'path': path1, 'meta': ''})['meta']

        original_preview_stid = file_meta_1['pmid']
        file_stid = file_meta_1['file_mid']

        if full_regenerate_preview_response:
            regenerate_preview_result = RegeneratePreviewResult('123456.yadisk:123456.123456', original_width=100,
                                                                original_height=100, rotate_angle=90, video_info={'x': 1})
        else:
            regenerate_preview_result = RegeneratePreviewResult('123456.yadisk:123456.123456')

        if use_kladun:
            self.upload_file(
                uid=uid2,
                hardlink=True,
                path=path2,
                file_data=file_data,
                nonexistent_stids_on_hardlink=(original_preview_stid,),
                regenerate_preview_result_on_hardlink=regenerate_preview_result,
            )
        else:
            def is_file_exist(stid):
                if stid == file_stid:
                    return True
                else:
                    return False
            with mock.patch('mpfs.core.services.mulca_service.Mulca.is_file_exist', side_effect=is_file_exist), \
                    PreviewerStub(regenerate_preview_result=regenerate_preview_result):
                self.json_ok('store', dict(uid=uid2, path=path2, **file_data))


        file_meta_1 = self.json_ok('info', {'uid': uid1, 'path': path1, 'meta': ''})['meta']
        file_meta_2 = self.json_ok('info', {'uid': uid2, 'path': path2, 'meta': ''})['meta']

        # Проверяем, что файлы схардлинкались
        assert file_meta_1['file_mid'] == file_meta_2['file_mid']
        # Проверяем, что у файлов правильные атрибуты
        assert file_meta_1['pmid'] == file_meta_2['pmid'] == regenerate_preview_result.pmid

        if regenerate_preview_result.original_width is not None and regenerate_preview_result.original_height is not None:
            assert file_meta_1['width'] == file_meta_2['width'] == regenerate_preview_result.original_width
            assert file_meta_1['height'] == file_meta_2['height'] == regenerate_preview_result.original_height
        else:
            assert 'width' not in file_meta_1 and 'width' not in file_meta_2
            assert 'height' not in file_meta_1 and 'height' not in file_meta_2

        if regenerate_preview_result.rotate_angle is not None:
            assert file_meta_1['angle'] == file_meta_2['angle'] == regenerate_preview_result.rotate_angle
        else:
            assert 'angle' not in file_meta_1 and 'angle' not in file_meta_2

        if regenerate_preview_result.video_info is not None:
            assert file_meta_1['video_info'] == file_meta_2['video_info'] == regenerate_preview_result.video_info
        else:
            assert 'video_info' not in file_meta_1 and 'video_info' not in file_meta_2

    def test_error_on_kladun_callback_with_unknown_type(self):
        path = '/disk/enot.jpg'

        body_1, body_2, body_3 = self.prepare_kladun_callbacks(uid=self.uid, path=path)
        oid = self.json_ok('store', {'uid': self.uid, 'path': path, 'callback': ''})['oid']

        with KladunStub(status_values=(body_1, body_2, body_3)):
            self.service_error('kladun_callback', {
                'uid': self.uid,
                'oid': oid,
                'status_xml': etree.tostring(body_1),
                'type': '123',
            }, code=codes.BAD_REQUEST_ERROR)

    def test_error_on_kladun_callback_without_type(self):
        path = '/disk/enot.jpg'

        body_1, body_2, body_3 = self.prepare_kladun_callbacks(uid=self.uid, path=path)
        oid = self.json_ok('store', {'uid': self.uid, 'path': path, 'callback': ''})['oid']

        with KladunStub(status_values=(body_1, body_2, body_3)):
            self.service_error('kladun_callback', {
                'uid': self.uid,
                'oid': oid,
                'status_xml': etree.tostring(body_1),
            }, code=codes.BAD_REQUEST_ERROR)

    @parameterized.expand([
        ('without_hardlink', False),
        ('with_hardlink', True),
    ])
    def test_saving_ip_to_link_data_on_store_attach(self, case_name, with_hardlink):
        file_data = None
        if with_hardlink:
            file_data = {
                'md5': md5(),
                'sha256': sha256(),
                'size': random.randint(100, 2000)
            }
            self.upload_file(self.uid, '/disk/file_to_hardlink_with.txt', file_data=file_data)

        self.upload_file(self.uid, '/attach/my_lovely_cat.mov',
                         headers={'X-Real-Ip': '127.0.0.1'},
                         file_data=file_data,
                         hardlink=with_hardlink)

        list_result = self.json_ok('list', {'uid': self.uid, 'path': '/attach', 'meta': 'file_id'})
        print list_result
        for item in list_result:
            if item['name'] == 'my_lovely_cat.mov':
                file_id = item['meta']['file_id']
                break
        else:
            self.fail('attached file not found')

        db = CollectionRoutedDatabase()
        link_data_rows = list(db.link_data.find({'uid': self.uid, 'key': {'$ne': '/'}}))

        assert len(link_data_rows) == 1
        link_data_row = link_data_rows[0]

        assert 'data' in link_data_row
        assert 'file_id' in link_data_row['data']
        assert link_data_row['data']['file_id'] == file_id

        assert 'user_ip' in link_data_row['data']
        assert link_data_row['data']['user_ip'] == '127.0.0.1'

    def test_store_destination_dir_with_matching_existing_filename(self):
        rand = str('%f' % time.time()).replace('.', '')[9:]
        file_md5 = hashlib.md5(rand).hexdigest()
        size = int(rand)
        path1 = '/disk/1'
        path2 = '/disk/1/test.jpg'

        self.upload_file(self.uid, path1)
        opts = {'uid': self.uid,
                'path': path2,
                'md5': file_md5,
                'size': size}
        self.json_error('store', opts, code=codes.NOT_FOLDER, status=409)


class ReturningMetaForStoringTest(StoreUtilsMixin, CommonSharingMethods, CommonJsonApiTestCase):

    def setup_method(self, method):
        super(ReturningMetaForStoringTest, self).setup_method(method)
        origin_file_path = '/disk/somefile.txt'
        self.upload_file(self.uid, origin_file_path)
        info = self.json_ok('info', {'uid': self.uid, 'path': origin_file_path, 'meta': ''})
        self.size, self.sha256, self.md5 = info['meta']['size'], info['meta']['sha256'], info['meta']['md5']

    @parameterized.expand([
        (False,),
        (True,),
    ])
    def test_hardlink_during_store_returns_locations(self, in_shared_folder):
        if in_shared_folder:
            new_file_path = self._create_shared_dir() + '/new_file.txt'
        else:
            new_file_path = '/disk/new_file.txt'
        with self.patch_mulca_is_file_exist(func_resp=True):
            self.json_ok(
                'store', {
                    'uid': self.uid,
                    'path': new_file_path,
                    'md5': self.md5,
                    'sha256': self.sha256,
                    'size': self.size,
                }
            )

        assert 'X-Disk-Path' in self.response.headers
        assert 'X-Disk-Resource-Id' in self.response.headers
        resource_id_from_store = self.response.headers['X-Disk-Resource-Id']
        path_from_store = self.response.headers['X-Disk-Path']

        info_resp = self.json_ok('info', {'uid': self.uid, 'path': new_file_path, 'meta': 'resource_id'})
        assert info_resp['meta']['resource_id'] == resource_id_from_store
        assert info_resp['path'] == path_from_store

    def test_location_headers_are_encoded(self):
        new_file_path = '/disk/новый файл.txt'
        with self.patch_mulca_is_file_exist(func_resp=True):
            self.json_ok(
                'store', {
                    'uid': self.uid,
                    'path': new_file_path,
                    'md5': self.md5,
                    'sha256': self.sha256,
                    'size': self.size,
                }
            )
        assert '/disk/%D0%BD%D0%BE%D0%B2%D1%8B%D0%B9%20%D1%84%D0%B0%D0%B9%D0%BB.txt' == self.response.headers['X-Disk-Path']
        assert isinstance(self.response.headers['X-Disk-Path'], str)

    @parameterized.expand([
        (False,),
        (True,),
    ])
    def test_store_412_conflict_returns_locations(self, in_shared_folder):
        if in_shared_folder:
            new_file_path = self._create_shared_dir() + '/new_file.txt'
        else:
            new_file_path = '/disk/new_file.txt'
        self.upload_file(self.uid, new_file_path)
        self.json_error('store', {'uid': self.uid, 'path': new_file_path}, code=codes.STORE_FILE_EXISTS)

        assert 'X-Disk-Path' in self.response.headers
        assert 'X-Disk-Resource-Id' in self.response.headers
        resource_id_from_store = self.response.headers['X-Disk-Resource-Id']
        path_from_store = self.response.headers['X-Disk-Path']

        info_resp = self.json_ok('info', {'uid': self.uid, 'path': new_file_path, 'meta': 'resource_id'})
        assert info_resp['meta']['resource_id'] == resource_id_from_store
        assert info_resp['path'] == path_from_store

    @parameterized.expand([
        ('/disk/original.jpg', '/photostream/1.jpg',),
        ('/disk/original.jpg', '/photounlim/1.jpg'),
        ('/photounlim/original.jpg', '/photostream/1.jpg'),
        ('/photounlim/original.jpg', '/photounlim/1.jpg'),
    ])
    def test_store_409_conflict_from_photostream_returns_locations(self, original_path, upload_path):
        md5 = '123456789abcdef0123456789abcdef0'
        sha256 = '123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0'
        size = 1000

        if original_path.startswith('/photounlim'):
            self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})
        self.upload_file(self.uid, original_path, file_data={'md5': md5, 'sha256': sha256, 'size': size})
        self.json_error('store', {'uid': self.uid, 'path': upload_path, 'md5': md5, 'sha256': sha256, 'size': size}, code=codes.FILE_EXISTS)

        assert 'X-Disk-Path' in self.response.headers
        assert 'X-Disk-Resource-Id' in self.response.headers
        resource_id_from_store = self.response.headers['X-Disk-Resource-Id']
        path_from_store = self.response.headers['X-Disk-Path']

        info_resp = self.json_ok('info', {'uid': self.uid, 'path': original_path, 'meta': 'resource_id'})
        assert info_resp['meta']['resource_id'] == resource_id_from_store
        assert info_resp['path'] == path_from_store

    @parameterized.expand([
        (False,),
        (True,),
    ])
    def test_return_additional_meta_on_first_kladun_callback_if_hardlinked(self, in_shared_folder):
        if in_shared_folder:
            new_file_path = self._create_shared_dir() + '/new_file.txt'
        else:
            new_file_path = '/disk/new_file.txt'

        body_1, _, _ = self.prepare_kladun_callbacks(
            uid=self.uid, path=new_file_path, md5=self.md5, sha256=self.sha256, size=self.size)

        oid = self.json_ok('store', {'uid': self.uid, 'path': new_file_path}).get('oid')

        with self.patch_mulca_is_file_exist(func_resp=True), \
                KladunStub(status_values=(body_1,)):
            opts = {'uid': self.uid, 'oid': oid, 'status_xml': etree.tostring(body_1), 'type': tags.COMMIT_FILE_INFO}
            callback_resp = self.service_error('kladun_callback', opts)
            assert callback_resp['code'] == codes.KLADUN_HARDLINK_FOUND
            assert 'X-Disk-Path' in self.response.headers
            assert 'X-Disk-Resource-Id' in self.response.headers
            resource_id_from_kladun = self.response.headers['X-Disk-Resource-Id']
            path_from_kladun = self.response.headers['X-Disk-Path']

            info_resp = self.json_ok('info', {'uid': self.uid, 'path': new_file_path, 'meta': 'resource_id'})
            assert info_resp['meta']['resource_id'] == resource_id_from_kladun
            assert info_resp['path'] == path_from_kladun

    @parameterized.expand([
        (False,),
        (True,),
    ])
    def test_return_additional_meta_on_second_kladun_callback_if_not_hardlinked(self, in_shared_folder):
        if in_shared_folder:
            new_file_path = self._create_shared_dir() + '/new_file.txt'
        else:
            new_file_path = '/disk/new_file.txt'

        body_1, body_2, body_3 = self.prepare_kladun_callbacks(
            uid=self.uid, path=new_file_path, md5=self.md5, sha256=self.sha256, size=self.size)

        oid = self.json_ok('store', {'uid': self.uid, 'path': new_file_path}).get('oid')

        with self.patch_mulca_is_file_exist(func_resp=False), \
                KladunStub(status_values=(body_1, body_2, body_3)):
            opts = {'uid': self.uid, 'oid': oid, 'status_xml': etree.tostring(body_1), 'type': tags.COMMIT_FILE_INFO}
            self.service_ok('kladun_callback', opts)
            assert 'X-Disk-Path' not in self.response.headers
            assert 'X-Disk-Resource-Id' not in self.response.headers

            opts = {'uid': self.uid, 'oid': oid, 'status_xml': etree.tostring(body_2), 'type': tags.COMMIT_FILE_UPLOAD}
            self.service_ok('kladun_callback', opts)
            assert 'X-Disk-Path' in self.response.headers
            assert 'X-Disk-Resource-Id' in self.response.headers
            resource_id_from_kladun = self.response.headers['X-Disk-Resource-Id']
            path_from_kladun = self.response.headers['X-Disk-Path']

            info_resp = self.json_ok('info', {'uid': self.uid, 'path': new_file_path, 'meta': 'resource_id'})
            assert info_resp['meta']['resource_id'] == resource_id_from_kladun
            assert info_resp['path'] == path_from_kladun

            opts = {'uid': self.uid, 'oid': oid, 'status_xml': etree.tostring(body_3), 'type': tags.COMMIT_FINAL}
            self.service_ok('kladun_callback', opts)
            assert 'X-Disk-Path' in self.response.headers
            assert 'X-Disk-Resource-Id' in self.response.headers
            assert resource_id_from_kladun == self.response.headers['X-Disk-Resource-Id']
            assert path_from_kladun == self.response.headers['X-Disk-Path']

    def _create_shared_dir(self):
        self.create_user(self.uid_3)
        self.xiva_subscribe(self.uid_3)

        self.json_ok('mkdir', {'uid': self.uid_3, 'path': '/disk/share'})
        self.json_ok('mkdir', {'uid': self.uid_3, 'path': '/disk/share/nested'})
        self.create_group(uid=self.uid_3, path='/disk/share/nested')
        hash_ = self.invite_user(uid=self.uid, email=self.email, owner=self.uid_3, path='/disk/share/nested')
        self.activate_invite(uid=self.uid, hash=hash_)
        return '/disk/nested'


class StoreCtimeTestCase(CommonJsonApiTestCase):
    path = '/disk/test'
    ctime = 5678

    @staticmethod
    def _patch_feature_ctime_from_client(enabled):
        return mock.patch('mpfs.frontend.api.disk.STORE_CTIME_FROM_CLIENT_ENABLED', enabled)

    @parameterized.expand([('store_ctime_from_client_enabled', True), ('store_ctime_from_client_disabled', False)])
    def test_store_ctime_current_time_without_ctime_from_client(self, _, store_ctime_from_client):
        ts = int(time.time())
        with self._patch_feature_ctime_from_client(store_ctime_from_client), \
             time_machine(datetime.datetime.fromtimestamp(ts)):
            self.upload_file(self.uid, self.path)
        info = self.json_ok('info', {'uid': self.uid, 'path': self.path})
        assert ts == info['ctime']

    @parameterized.expand([('store_ctime_from_client_enabled', True), ('store_ctime_from_client_disabled', False)])
    def test_uses_ctime_from_client_only_when_enabled_in_settings(self, _, store_ctime_from_client):
        u'''
            Проверяем, что ctime сохраняется только при включенной настройке.
            Также проверяем, куда и в каком типе сохраняется ctime
        '''
        ts = int(time.time())

        with self._patch_feature_ctime_from_client(store_ctime_from_client), \
             time_machine(datetime.datetime.fromtimestamp(ts)):
            self.upload_file(self.uid, self.path, opts={'ctime': str(self.ctime)})

        info = self.json_ok('info', {'uid': self.uid, 'path': self.path})
        expected_ctime = self.ctime if store_ctime_from_client else ts
        assert expected_ctime == info['ctime']
        resource = factory.get_resource(self.uid, self.path)
        assert expected_ctime == resource.ctime
        db = CollectionRoutedDatabase()
        record = db.user_data.find_one({'uid': self.uid, 'key': self.path})
        zdata = record['zdata']
        if isinstance(zdata, str):
            zdata = from_json(zdata.decode('zlib'))
        assert expected_ctime == zdata['meta']['ctime']


class AstoreTestCase(CommonJsonApiTestCase):

    stubs_manager = StubsManager(
        method_stubs=set(StubsManager.DEFAULT_METHOD_STUBS) - {KladunStub}
    )

    def test_astore_works_after_uploading_started(self):
        rand = str('%f' % time.time()).replace('.', '')[9:]
        file_md5 = hashlib.md5(rand).hexdigest()
        file_sha256 = hashlib.sha256(rand).hexdigest()

        self.json_ok('store', {'uid': self.uid, 'path': '/photostream/1.jpg', 'size': 1, 'md5': file_md5, 'sha256': file_sha256})
        with mock.patch('mpfs.core.services.common_service.Service.open_url'):
            self.json_ok('astore', {'uid': self.uid, 'path': '/photostream/1.jpg', 'md5': file_md5})

    def test_astore_works_in_kladun_returns_404(self):
        rand = str('%f' % time.time()).replace('.', '')[9:]
        file_md5 = hashlib.md5(rand).hexdigest()
        file_sha256 = hashlib.sha256(rand).hexdigest()

        self.json_ok('store', {'uid': self.uid, 'path': '/photostream/1.jpg', 'size': 1, 'md5': file_md5, 'sha256': file_sha256})
        with mock.patch('mpfs.core.services.common_service.Service.open_url', side_effect=KladunNoResponse(data={'code': 404})):
            self.json_ok('astore', {'uid': self.uid, 'path': '/photostream/1.jpg', 'md5': file_md5})

    def test_astore_returns_404_if_kladun_is_dead(self):
        rand = str('%f' % time.time()).replace('.', '')[9:]
        file_md5 = hashlib.md5(rand).hexdigest()
        file_sha256 = hashlib.sha256(rand).hexdigest()

        self.json_ok('store', {'uid': self.uid, 'path': '/photostream/1.jpg', 'size': 1, 'md5': file_md5, 'sha256': file_sha256})
        with mock.patch('mpfs.core.services.common_service.Service.open_url', side_effect=KladunNoResponse(data={'code': 500})):
            self.json_error('astore', {'uid': self.uid, 'path': '/photostream/1.jpg', 'md5': file_md5}, code=codes.OPERATION_NOT_FOUND)
