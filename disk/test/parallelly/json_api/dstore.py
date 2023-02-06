# -*- coding: utf-8 -*-
import hashlib
import time

import mock
from hamcrest import assert_that, has_entry, has_item, is_not, has_key, equal_to, has_entries, starts_with, all_of
from lxml import etree

from test.parallelly.json_api.base import CommonJsonApiTestCase

from mpfs.common.static import codes
from mpfs.common.static import codes, tags
from test.helpers.stubs.services import KladunStub, SearchIndexerStub
from test.fixtures.kladun import KladunMocker
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase


class DstoreUseHttpsTestCase(CommonJsonApiTestCase):

    def test_dstore_http(self):

        file_path = '/disk/https.file'
        self.upload_file(self.uid, '/disk/https.file')
        opts = {'uid': self.uid,
                'path': file_path,
                'meta': ''}
        folder_info = self.json_ok('info', opts)
        file_md5 = folder_info['meta']['md5']
        # http
        opts = {'uid': self.uid,
                'path': file_path,
                'md5': file_md5,
                'use_https': 0}
        with KladunStub() as kladun_mocks:
            self.json_ok('dstore', opts)
            assert_that(kladun_mocks.patch_post_request.called,
                        equal_to(True))
            kladun_args, _ = kladun_mocks.patch_post_request.call_args
        assert_that(kladun_args,
                    has_item(has_entry('use-https', 'false')))
        # =======================================================================
        # https
        opts = {'uid': self.uid,
                'path': file_path,
                'md5': file_md5,
                'use_https': 1}
        with KladunStub() as kladun_mocks:
            self.json_ok('dstore', opts)
            assert_that(kladun_mocks.patch_post_request.called,
                        equal_to(True))
            kladun_args, _ = kladun_mocks.patch_post_request.call_args
        assert_that(kladun_args,
                    has_item(has_entry('use-https', 'true')))
        # =======================================================================
        # none
        # https
        opts = {'uid': self.uid,
                'path': file_path,
                'md5': file_md5}
        with KladunStub() as kladun_mocks:
            self.json_ok('dstore', opts)
            assert_that(kladun_mocks.patch_post_request.called,
                        equal_to(True))
            kladun_args, _ = kladun_mocks.patch_post_request.call_args
        assert_that(kladun_args,
                    is_not(has_item(has_key('use-https'))))


class DstoreJsonApiTestCase(CommonJsonApiTestCase):
    def prepare_dstore_kladun_callbacks(self, uid, path):
        # Загружаем файл, для коготорого приготовим dstore callback'и
        self.upload_file(self.uid, path)
        file_info = self.json_ok('info', {'uid': self.uid,
                                          'path': path,
                                          'meta': ','})

        # готовим переменные для имитации данных коллбеков
        address = '%s:%s' % (uid, path)

        rand_dstore = str('%f' % time.time()).replace('.', '')[9:]

        file_md5_dstore_2 = hashlib.md5(rand_dstore).hexdigest()
        file_sha256_dstore_2 = hashlib.sha256(rand_dstore).hexdigest()

        size_dstore_2 = int(rand_dstore)

        mid_digest_dstore_2 = '100000.yadisk:%s.%s' % (uid, int(file_md5_dstore_2[:16], 16))
        mid_file_dstore_2 = '100000.yadisk:%s.%s' % (uid, int(file_md5_dstore_2[:16][::-1], 16))

        body_1 = etree.fromstring(open('fixtures/xml/kladun_dstore_1.xml').read())
        body_2 = etree.fromstring(open('fixtures/xml/kladun_dstore_2.xml').read())
        body_3 = etree.fromstring(open('fixtures/xml/kladun_dstore_3.xml').read())

        # dstore callback
        for body in (body_1, body_2, body_3):
            body.find('request').set('original-md5', file_info['meta']['md5'])
            body.find('request').set('original-mulca-id', file_info['meta']['file_mid'])
            body.find('request').find('chemodan-file-attributes').set('uid', str(uid))
            body.find('request').find('chemodan-file-attributes').set('file-id', file_info['meta']['file_id'])
            body.find('request').find('chemodan-file-attributes').set('path', address)
            for arg in ('current', 'total'):
                body.find('stages').find('incoming-http').find('progress').set(arg, str(size_dstore_2))
            body.find('stages').find('expected-patched-md5').find('result').set('md5', file_md5_dstore_2)
            body.find('stages').find('patched-file').find('result').set('md5', file_md5_dstore_2)
            body.find('stages').find('patched-file').find('result').set('sha256', file_sha256_dstore_2)
            body.find('stages').find('patched-file').find('result').set('content-length', str(size_dstore_2))
        for body in (body_2, body_3):
            body.find('stages').find('mulca-file').find('result').set('mulca-id', mid_file_dstore_2)
            body.find('stages').find('mulca-digest').find('result').set('mulca-id', mid_digest_dstore_2)

        return body_1, body_2, body_3

    def test_dstore_in_the_middle_of_store(self):
        """
        CHEMODAN-16358
        Проверка 409 на третий коллбек store после трех коллбеков dstore

        """
        uid = self.uid
        path = '/disk/editable_picture.jpg'
        try:
            path = path.decode('utf-8')
        except UnicodeEncodeError:
            pass

        address = '%s:%s' % (uid, path)
        rand = str('%f' % time.time()).replace('.', '')[9:]
        rand_dstore = str('%f' % time.time()).replace('.', '')[9:]
        self.assertNotEqual(rand, rand_dstore)

        file_md5 = hashlib.md5(rand).hexdigest()
        file_md5_dstore = hashlib.md5(rand_dstore).hexdigest()
        file_sha256 = hashlib.sha256(rand).hexdigest()
        file_sha256_dstore = hashlib.sha256(rand_dstore).hexdigest()

        mimetype = 'application/x-www-form-urlencoded'

        size = int(rand)
        size_dstore = int(rand_dstore)

        file_id = hashlib.sha256(file_md5 + ':' + file_sha256).hexdigest()
        mid_digest = '100000.yadisk:%s.%s' % (uid, int(file_md5[:16], 16))
        mid_file = '100000.yadisk:%s.%s' % (uid, int(file_md5[:16][::-1], 16))
        pmid = '100000.yadisk:%s.%s' % (uid, int(file_md5[:32][::2], 16))
        mid_digest_dstore = '100000.yadisk:%s.%s' % (uid, int(file_md5_dstore[:16], 16))
        mid_file_dstore = '100000.yadisk:%s.%s' % (uid, int(file_md5_dstore[:16][::-1], 16))

        body_1 = etree.fromstring(open('fixtures/xml/kladun_store_1.xml').read())
        body_2 = etree.fromstring(open('fixtures/xml/kladun_store_2.xml').read())
        body_2.find('stages').find('preview-image-mulca-upload').find('result').set('mulca-id', pmid)
        body_3 = etree.fromstring(open('fixtures/xml/kladun_store_3.xml').read())
        body_3.find('stages').find('preview-image-mulca-upload').find('result').set('mulca-id', pmid)

        body_1_dstore = etree.fromstring(open('fixtures/xml/kladun_dstore_1.xml').read())
        body_2_dstore = etree.fromstring(open('fixtures/xml/kladun_dstore_2.xml').read())
        body_3_dstore = etree.fromstring(open('fixtures/xml/kladun_dstore_3.xml').read())
        # store callback
        for body in (body_1, body_2, body_3):
            body.find('request').find('chemodan-file-attributes').set('uid', str(uid))
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
        # dstore callback
        for body in (body_1_dstore, body_2_dstore, body_3_dstore):
            body.find('request').set('original-md5', file_md5)
            body.find('request').set('original-mulca-id', mid_file)
            body.find('request').find('chemodan-file-attributes').set('uid', str(uid))
            body.find('request').find('chemodan-file-attributes').set('file-id', file_id)
            body.find('request').find('chemodan-file-attributes').set('path', address)
            for arg in ('current', 'total'):
                body.find('stages').find('incoming-http').find('progress').set(arg, str(size_dstore))
            body.find('stages').find('expected-patched-md5').find('result').set('md5', file_md5_dstore)
            body.find('stages').find('patched-file').find('result').set('md5', file_md5_dstore)
            body.find('stages').find('patched-file').find('result').set('sha256', file_sha256_dstore)
            body.find('stages').find('patched-file').find('result').set('content-length', str(size))
        for body in (body_2, body_3):
            body.find('stages').find('mulca-file').find('result').set('mulca-id', mid_file_dstore)
            body.find('stages').find('mulca-digest').find('result').set('mulca-id', mid_digest_dstore)

        opts = {'uid': uid,
                'path': address,
                'force': 1,
                'md5': file_md5,
                'size': size,
                'callback': ''}
        with KladunStub():
            store_result = self.json_ok('store', opts)

        self.assertTrue(isinstance(store_result, dict))
        self.assertTrue('oid' in store_result)
        store_oid = store_result['oid']
        self.assertTrue(store_oid)

        with KladunStub(status_values=(body_1, body_2, body_3)):
            # Callback #1 store
            opts = {
                'uid': uid,
                'oid': store_oid,
                'status_xml': etree.tostring(body_1),
                'type': tags.COMMIT_FILE_INFO,
            }
            self.service_ok('kladun_callback', opts)
            # Callback #2 store
            opts = {
                'uid': uid,
                'oid': store_oid,
                'status_xml': etree.tostring(body_2),
                'type': tags.COMMIT_FILE_UPLOAD,
            }
            self.service_ok('kladun_callback', opts)

        opts = {'uid': uid,
                'path': address,
                'force': 1,
                'md5': file_md5,
                'size': size,
                'callback': ''}
        with KladunStub(status_value_paths=('fixtures/xml/kladun_dstore_1.xml',
                                            'fixtures/xml/kladun_dstore_2.xml',
                                            'fixtures/xml/kladun_dstore_3.xml')):
            dstore_result = self.json_ok('dstore', opts)

            self.assertTrue(isinstance(dstore_result, dict))
            self.assertTrue('oid' in dstore_result)
            dstore_oid = dstore_result['oid']
            self.assertTrue(dstore_oid)

            # Callback #1 dstore
            opts = {
                'uid': uid,
                'oid': dstore_oid,
                'status_xml': etree.tostring(body_1_dstore),
                'type': tags.COMMIT_FILE_INFO,
            }
            self.service_ok('kladun_callback', opts)
            # Callback #2 dstore
            opts = {
                'uid': uid,
                'oid': dstore_oid,
                'status_xml': etree.tostring(body_2_dstore),
                'type': tags.COMMIT_FILE_UPLOAD,
            }
            self.service_ok('kladun_callback', opts)
            # Callback #3 dstore
            opts = {
                'uid': uid,
                'oid': dstore_oid,
                'status_xml': etree.tostring(body_3_dstore),
                'type': tags.COMMIT_FINAL,
            }
            self.service_ok('kladun_callback', opts)
            # Callback #3 store
            opts = {
                'uid': uid,
                'oid': store_oid,
                'status_xml': etree.tostring(body_3),
                'type': tags.COMMIT_FINAL,
            }
            self.service_error('kladun_callback', opts)

    def test_dstore_after_three_callbacks_of_dstore(self):
        """
        CHEMODAN-16358
        Проверка 409 на третий коллбек dstore после трех коллбеков dstore

        """
        uid = self.uid
        path = '/disk/editable_picture.jpg'
        self.upload_file(uid, path)
        opts = {'uid': uid,
                'path': path,
                'meta': ''}
        result = self.json_ok('info', opts)
        file_md5 = result['meta']['md5']
        size = result['meta']['size']
        mid_file = result['meta']['file_mid']
        file_id = result['meta']['file_id']
        try:
            path = path.decode('utf-8')
        except UnicodeEncodeError:
            pass

        address = '%s:%s' % (uid, path)
        rand = str('%f' % time.time()).replace('.', '')[9:]
        rand_dstore = str('%f' % time.time()).replace('.', '')[9:]
        self.assertNotEqual(rand, rand_dstore)

        file_md5_dstore_1 = hashlib.md5(rand).hexdigest()
        file_md5_dstore_2 = hashlib.md5(rand_dstore).hexdigest()
        file_sha256_dstore_1 = hashlib.sha256(rand).hexdigest()
        file_sha256_dstore_2 = hashlib.sha256(rand_dstore).hexdigest()

        size_dstore_1 = int(rand)
        size_dstore_2 = int(rand_dstore)

        mid_digest_dstore_1 = '100000.yadisk:%s.%s' % (uid, int(file_md5_dstore_1[:16], 16))
        mid_file_dstore_1 = '100000.yadisk:%s.%s' % (uid, int(file_md5_dstore_1[:16][::-1], 16))
        mid_digest_dstore_2 = '100000.yadisk:%s.%s' % (uid, int(file_md5_dstore_2[:16], 16))
        mid_file_dstore_2 = '100000.yadisk:%s.%s' % (uid, int(file_md5_dstore_2[:16][::-1], 16))

        body_1_dstore_1 = etree.fromstring(open('fixtures/xml/kladun_dstore_1.xml').read())
        body_2_dstore_1 = etree.fromstring(open('fixtures/xml/kladun_dstore_2.xml').read())
        body_3_dstore_1 = etree.fromstring(open('fixtures/xml/kladun_dstore_3.xml').read())

        body_1_dstore_2 = etree.fromstring(open('fixtures/xml/kladun_dstore_1.xml').read())
        body_2_dstore_2 = etree.fromstring(open('fixtures/xml/kladun_dstore_2.xml').read())
        body_3_dstore_2 = etree.fromstring(open('fixtures/xml/kladun_dstore_3.xml').read())
        # store callback
        for body in (body_1_dstore_1, body_2_dstore_1, body_3_dstore_1):
            body.find('request').set('original-md5', file_md5)
            body.find('request').set('original-mulca-id', mid_file)
            body.find('request').find('chemodan-file-attributes').set('uid', str(uid))
            body.find('request').find('chemodan-file-attributes').set('file-id', file_id)
            body.find('request').find('chemodan-file-attributes').set('path', address)
            for arg in ('current', 'total'):
                body.find('stages').find('incoming-http').find('progress').set(arg, str(size_dstore_1))
            body.find('stages').find('expected-patched-md5').find('result').set('md5', file_md5_dstore_1)
            body.find('stages').find('patched-file').find('result').set('md5', file_md5_dstore_1)
            body.find('stages').find('patched-file').find('result').set('sha256', file_sha256_dstore_1)
            body.find('stages').find('patched-file').find('result').set('content-length', str(size_dstore_1))
        for body in (body_2_dstore_1, body_3_dstore_1):
            body.find('stages').find('mulca-file').find('result').set('mulca-id', mid_file_dstore_1)
            body.find('stages').find('mulca-digest').find('result').set('mulca-id', mid_digest_dstore_1)
        # dstore callback
        for body in (body_1_dstore_2, body_2_dstore_2, body_3_dstore_2):
            body.find('request').set('original-md5', file_md5_dstore_1)
            body.find('request').set('original-mulca-id', mid_file_dstore_1)
            body.find('request').find('chemodan-file-attributes').set('uid', str(uid))
            body.find('request').find('chemodan-file-attributes').set('file-id', file_id)
            body.find('request').find('chemodan-file-attributes').set('path', address)
            for arg in ('current', 'total'):
                body.find('stages').find('incoming-http').find('progress').set(arg, str(size_dstore_2))
            body.find('stages').find('expected-patched-md5').find('result').set('md5', file_md5_dstore_2)
            body.find('stages').find('patched-file').find('result').set('md5', file_md5_dstore_2)
            body.find('stages').find('patched-file').find('result').set('sha256', file_sha256_dstore_2)
            body.find('stages').find('patched-file').find('result').set('content-length', str(size_dstore_2))
        for body in (body_2_dstore_2, body_3_dstore_2):
            body.find('stages').find('mulca-file').find('result').set('mulca-id', mid_file_dstore_2)
            body.find('stages').find('mulca-digest').find('result').set('mulca-id', mid_digest_dstore_2)

        opts = {'uid': uid,
                'path': address,
                'force': 1,
                'md5': file_md5,
                'size': size,
                'callback': ''}
        result = self.json_ok('dstore', opts)
        self.assertTrue(isinstance(result, dict))
        self.assertTrue('oid' in result)
        first_dstore_oid = result['oid']
        self.assertTrue(first_dstore_oid)

        with KladunStub(status_values=(body_1_dstore_1,
                                       body_2_dstore_1)):
            # Callback #1 dstore 1
            opts = {
                'uid': uid,
                'oid': first_dstore_oid,
                'status_xml': etree.tostring(body_1_dstore_1),
                'type': tags.COMMIT_FILE_INFO,
            }
            self.service_ok('kladun_callback', opts)

            # Callback #2 dstore 1
            opts = {
                'uid': uid,
                'oid': first_dstore_oid,
                'status_xml': etree.tostring(body_2_dstore_1),
                'type': tags.COMMIT_FILE_UPLOAD,
            }
            self.service_ok('kladun_callback', opts)

        opts = {'uid': uid,
                'path': address,
                'force': 1,
                'md5': file_md5_dstore_1,
                'size': size_dstore_1,
                'callback': ''}
        result = self.json_ok('dstore', opts)
        self.assertTrue(isinstance(result, dict))
        self.assertTrue('oid' in result)
        second_dstore_oid = result['oid']
        self.assertTrue(second_dstore_oid)

        with KladunStub(status_values=(body_1_dstore_2,
                                       body_2_dstore_2,
                                       body_3_dstore_2)):
            # Callback #1 dstore 2
            opts = {
                'uid': uid,
                'oid': second_dstore_oid,
                'status_xml': etree.tostring(body_1_dstore_2),
                'type': tags.COMMIT_FILE_INFO,
            }
            self.service_ok('kladun_callback', opts)

            # Callback #2 dstore 2
            opts = {
                'uid': uid,
                'oid': second_dstore_oid,
                'status_xml': etree.tostring(body_2_dstore_2),
                'type': tags.COMMIT_FILE_UPLOAD,
            }
            self.service_ok('kladun_callback', opts)

            # Callback #3 dstore 2
            opts = {
                'uid': uid,
                'oid': second_dstore_oid,
                'status_xml': etree.tostring(body_3_dstore_2),
                'type': tags.COMMIT_FINAL,
            }
            self.service_ok('kladun_callback', opts)

        with KladunStub(status_values=(body_3_dstore_1,)):
            # Callback #3 dstore 1
            opts = {
                'uid': uid,
                'oid': first_dstore_oid,
                'status_xml': etree.tostring(body_3_dstore_1),
                'type': tags.COMMIT_FINAL,
            }
            self.service_error('kladun_callback', opts)

    def test_dstore_preview_saved(self):
        """
        Проверить что при патче файла сохраняется `stid` превьюшки.
        """
        uid = self.uid
        path = '/disk/editable_picture.jpg'
        self.upload_file(uid, path)
        result = self.json_ok('info', {'uid': uid, 'path': path, 'meta': ''})
        file_md5 = result['meta']['md5']
        size = result['meta']['size']
        mid_file = result['meta']['file_mid']
        file_id = result['meta']['file_id']
        address = '%s:%s' % (uid, path)
        result = self.json_ok('dstore', {
            'uid': uid,
            'path': address,
            'force': 1,
            'md5': file_md5,
            'size': size,
            'callback': ''
        })
        self.assertTrue(isinstance(result, dict))
        self.assertTrue('oid' in result)
        oid = result['oid']
        KladunMocker().mock_kladun_callbacks_for_dstore(uid, oid)
        result = self.json_ok('info', {
            'uid': uid,
            'path': path,
            'meta': ''
        })
        meta = result['meta']
        assert 'pmid' in meta  # stid превью
        assert meta['pmid'] == '320.yadisk:122625849.E67536:18812285367101674250542874121'  # из фикстуры

    def test_dstore_after_two_callbacks_of_dstore(self):
        """
        https://jira.yandex-team.ru/browse/CHEMODAN-18554
        Проверка 503 на второй коллбек dstore после второго коллбека dstore

        """
        uid = self.uid
        path = '/disk/editable_picture.jpg'
        self.upload_file(uid, path)

        opts = {'uid': uid,
                'path': path,
                'meta': ''}
        result = self.json_ok('info', opts)
        file_md5 = result['meta']['md5']
        size = result['meta']['size']
        mid_file = result['meta']['file_mid']
        file_id = result['meta']['file_id']
        try:
            path = path.decode('utf-8')
        except UnicodeEncodeError:
            pass

        address = '%s:%s' % (uid, path)
        rand = str('%f' % time.time()).replace('.', '')[9:]
        rand_dstore = str('%f' % time.time()).replace('.', '')[9:]
        self.assertNotEqual(rand, rand_dstore)

        file_md5_dstore_1 = hashlib.md5(rand).hexdigest()
        file_md5_dstore_2 = hashlib.md5(rand_dstore).hexdigest()
        file_sha256_dstore_1 = hashlib.sha256(rand).hexdigest()
        file_sha256_dstore_2 = hashlib.sha256(rand_dstore).hexdigest()

        size_dstore_1 = int(rand)
        size_dstore_2 = int(rand_dstore)

        mid_digest_dstore_1 = '100000.yadisk:%s.%s' % (uid, int(file_md5_dstore_1[:16], 16))
        mid_file_dstore_1 = '100000.yadisk:%s.%s' % (uid, int(file_md5_dstore_1[:16][::-1], 16))
        mid_digest_dstore_2 = '100000.yadisk:%s.%s' % (uid, int(file_md5_dstore_2[:16], 16))
        mid_file_dstore_2 = '100000.yadisk:%s.%s' % (uid, int(file_md5_dstore_2[:16][::-1], 16))

        body_1_dstore_1 = etree.fromstring(open('fixtures/xml/kladun_dstore_1.xml').read())
        body_2_dstore_1 = etree.fromstring(open('fixtures/xml/kladun_dstore_2.xml').read())
        body_3_dstore_1 = etree.fromstring(open('fixtures/xml/kladun_dstore_3.xml').read())

        body_1_dstore_2 = etree.fromstring(open('fixtures/xml/kladun_dstore_1.xml').read())
        body_2_dstore_2 = etree.fromstring(open('fixtures/xml/kladun_dstore_2.xml').read())
        body_3_dstore_2 = etree.fromstring(open('fixtures/xml/kladun_dstore_3.xml').read())
        # store callback
        for body in (body_1_dstore_1, body_2_dstore_1, body_3_dstore_1):
            body.find('request').set('original-md5', file_md5)
            body.find('request').set('original-mulca-id', mid_file)
            body.find('request').find('chemodan-file-attributes').set('uid', str(uid))
            body.find('request').find('chemodan-file-attributes').set('file-id', file_id)
            body.find('request').find('chemodan-file-attributes').set('path', address)
            for arg in ('current', 'total'):
                body.find('stages').find('incoming-http').find('progress').set(arg, str(size_dstore_1))
            body.find('stages').find('expected-patched-md5').find('result').set('md5', file_md5_dstore_1)
            body.find('stages').find('patched-file').find('result').set('md5', file_md5_dstore_1)
            body.find('stages').find('patched-file').find('result').set('sha256', file_sha256_dstore_1)
            body.find('stages').find('patched-file').find('result').set('content-length', str(size_dstore_1))
        for body in (body_2_dstore_1, body_3_dstore_1):
            body.find('stages').find('mulca-file').find('result').set('mulca-id', mid_file_dstore_1)
            body.find('stages').find('mulca-digest').find('result').set('mulca-id', mid_digest_dstore_1)
        # dstore callback
        for body in (body_1_dstore_2, body_2_dstore_2, body_3_dstore_2):
            body.find('request').set('original-md5', file_md5_dstore_1)
            body.find('request').set('original-mulca-id', mid_file_dstore_1)
            body.find('request').find('chemodan-file-attributes').set('uid', str(uid))
            body.find('request').find('chemodan-file-attributes').set('file-id', file_id)
            body.find('request').find('chemodan-file-attributes').set('path', address)
            for arg in ('current', 'total'):
                body.find('stages').find('incoming-http').find('progress').set(arg, str(size_dstore_2))
            body.find('stages').find('expected-patched-md5').find('result').set('md5', file_md5_dstore_2)
            body.find('stages').find('patched-file').find('result').set('md5', file_md5_dstore_2)
            body.find('stages').find('patched-file').find('result').set('sha256', file_sha256_dstore_2)
            body.find('stages').find('patched-file').find('result').set('content-length', str(size_dstore_2))
        for body in (body_2_dstore_2, body_3_dstore_2):
            body.find('stages').find('mulca-file').find('result').set('mulca-id', mid_file_dstore_2)
            body.find('stages').find('mulca-digest').find('result').set('mulca-id', mid_digest_dstore_2)

        opts = {'uid': uid,
                'path': address,
                'force': 1,
                'md5': file_md5,
                'size': size,
                'callback': ''}
        result = self.json_ok('dstore', opts)
        self.assertTrue(isinstance(result, dict))
        self.assertTrue('oid' in result)
        dstore_1_oid = result['oid']
        self.assertTrue(dstore_1_oid)

        with KladunStub(status_values=(body_1_dstore_1,)):
            # Callback #1 dstore 1
            opts = {
                'uid': uid,
                'oid': dstore_1_oid,
                'status_xml': etree.tostring(body_1_dstore_1),
                'type': tags.COMMIT_FILE_INFO,
            }
            self.service_ok('kladun_callback', opts)

        opts = {'uid': uid,
                'path': address,
                'force': 1,
                'md5': file_md5,
                'size': size,
                'callback': ''}
        result = self.json_ok('dstore', opts)
        self.assertTrue(isinstance(result, dict))
        self.assertTrue('oid' in result)
        dstore_2_oid = result['oid']
        self.assertTrue(dstore_2_oid)

        with KladunStub(status_values=(body_2_dstore_1,)):
            # Callback #2 dstore 1
            opts = {
                'uid': uid,
                'oid': dstore_1_oid,
                'status_xml': etree.tostring(body_2_dstore_1),
                'type': tags.COMMIT_FILE_UPLOAD,
            }
            self.service_ok('kladun_callback', opts)

        with KladunStub(status_values=(body_1_dstore_2,
                                       body_2_dstore_2)):
            # Callback #1 dstore 2
            opts = {
                'uid': uid,
                'oid': dstore_2_oid,
                'status_xml': etree.tostring(body_1_dstore_2),
                'type': tags.COMMIT_FILE_INFO,
            }
            self.service_ok('kladun_callback', opts)

            # Callback #2 dstore 2
            opts = {
                'uid': uid,
                'oid': dstore_2_oid,
                'status_xml': etree.tostring(body_2_dstore_2),
                'type': tags.COMMIT_FILE_UPLOAD,
            }
            self.service_error('kladun_callback', opts)

    def test_moving_file_while_dstore(self):
        file_path = '/disk/original_kitten.jpg'
        new_file_path = '/disk/fluffy_cat.jpg'
        body_1, body_2, body_3 = self.prepare_dstore_kladun_callbacks(uid=self.uid,
                                                                      path=file_path)
        file_info = self.json_ok('info', {'uid': self.uid,
                                          'path': file_path,
                                          'meta': ','})
        oid = self.json_ok('dstore', {'uid': self.uid,
                                      'path': file_path,
                                      'md5': file_info['meta']['md5']})['oid']
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
            # Перемещаем файл на новое место
            self.json_ok('move', {'uid': self.uid, 'src': file_path, 'dst': new_file_path})
            # Проверяем, что callback будет успешным (раньше падали)
            self.service_ok('kladun_callback', {
                'uid': self.uid,
                'oid': oid,
                'status_xml': etree.tostring(body_3),
                'type': tags.COMMIT_FINAL,
            })

        # Проверяем, что файл доступен по новому пути
        info = self.json_ok('info', {'uid': self.uid, 'path': new_file_path})
        assert info.get('etime') is not None
        # И не доступен по старому
        self.json_error('info', {'uid': self.uid, 'path': file_path}, code=codes.RESOURCE_NOT_FOUND)

    def test_hidden_file_id_after_dstore(self):
        db = CollectionRoutedDatabase()
        file_path = '/disk/fluffy foxy.jpg'
        body_1, body_2, body_3 = self.prepare_dstore_kladun_callbacks(uid=self.uid,
                                                                      path=file_path)

        original_file_id = list(db.user_data.find({'uid': self.uid, 'key': file_path}))[0]['data']['file_id']

        file_info = self.json_ok('info', {'uid': self.uid,
                                          'path': file_path,
                                          'meta': ','})
        oid = self.json_ok('dstore', {'uid': self.uid,
                                      'path': file_path,
                                      'md5': file_info['meta']['md5']})['oid']
        with KladunStub(status_values=(body_1, body_2, body_3)):
            self.service_ok('kladun_callback', {'uid': self.uid,
                                                'oid': oid,
                                                'status_xml': etree.tostring(body_1),
                                                'type': tags.COMMIT_FILE_INFO,})
            self.service_ok('kladun_callback', {'uid': self.uid,
                                                'oid': oid,
                                                'status_xml': etree.tostring(body_2),
                                                'type': tags.COMMIT_FILE_UPLOAD,})
            self.service_ok('kladun_callback', {'uid': self.uid,
                                                'oid': oid,
                                                'status_xml': etree.tostring(body_3),
                                                'type': tags.COMMIT_FINAL,})

        file_id_after_dstore = list(db.user_data.find({'uid': self.uid, 'key': file_path}))[0]['data']['file_id']

        hidden_path = '/hidden/fluffy foxy.jpg'
        hidden_data_content = list(db.hidden_data.find({'uid': self.uid}))
        assert_that(hidden_data_content,
                    # К пути файла добавляется суффикс, поэтому путь файла проверяем по началу пути
                    has_item(has_entries({'key': starts_with(hidden_path),
                                          # Проверяем, что file_id у файла теперь другой
                                          # При dstore file_id ресурса в /disk не меняется,
                                          # но на случай изменений в этой логике
                                          # сравниваем с file_id до и после dstore'а
                                          'data': has_entry('file_id', all_of(is_not(equal_to(original_file_id)),
                                                                              is_not(equal_to(file_id_after_dstore))))})))

    def test_djfs_callbacks_to_indexer_on_dstore(self):
        file_path = '/disk/test.jpg'
        body_1, body_2, body_3 = self.prepare_dstore_kladun_callbacks(uid=self.uid, path=file_path)
        file_info = self.json_ok('info', {'uid': self.uid, 'path': file_path, 'meta': ''})

        with mock.patch('mpfs.core.filesystem.indexer.SERVICES_SEARCH_INDEXER_SEND_DJFS_CALLBACKS_ON_STORE', True), \
                SearchIndexerStub() as stub:
            oid = self.json_ok('dstore', {'uid': self.uid, 'path': file_path, 'md5': file_info['meta']['md5']})['oid']

            with KladunStub(status_values=(body_1, body_2, body_3)):
                self.service_ok('kladun_callback', {'uid': self.uid, 'oid': oid, 'status_xml': etree.tostring(body_1),
                                                    'type': tags.COMMIT_FILE_INFO})
                self.service_ok('kladun_callback', {'uid': self.uid, 'oid': oid, 'status_xml': etree.tostring(body_2),
                                                    'type': tags.COMMIT_FILE_UPLOAD})
                self.service_ok('kladun_callback', {'uid': self.uid, 'oid': oid, 'status_xml': etree.tostring(body_3),
                                                    'type': tags.COMMIT_FINAL})

            for args, kwargs in stub.push_change.call_args_list:
                data = args[0]
                if not isinstance(data, list):
                    data = [data]
                for item in data:
                    if item.get('action') == 'delete':
                        continue
                    assert item.get('append_djfs_callbacks')
