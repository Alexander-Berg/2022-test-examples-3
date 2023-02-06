# -*- coding: utf-8 -*-
import re
import threading
import pytest

from lxml import etree
from hamcrest import assert_that, all_of, has_item, has_entry, has_entries

from test.base import DiskTestCase
from test.base_suit import set_up_open_url, tear_down_open_url
from test.conftest import INIT_USER_IN_POSTGRES
from test.fixtures.users import pdd_user
import test.fixtures.filesystem as file_fixtures

import mpfs.engine.process
from mpfs.common.util.filetypes import DEFAULT_MIME_TYPE
from mpfs.core.address import Address
from mpfs.core.bus import Bus
from mpfs.metastorage.mongo.util import decompress_data
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase

db = CollectionRoutedDatabase()
changelog_db = mpfs.engine.process.dbctl().database()



class TestDesktopApi(DiskTestCase):

    pdd_uid = pdd_user.uid

    def test_user_init(self):
        self.desktop('user_init', {'uid': self.uid, 'locale': 'en'})
        result = self.desktop('user_check', {'uid': self.uid})
        self.assertEqual(result['need_init'], '0')
        url = 'http://localhost/service/echo'
        opts = {'uid' : self.uid, 'callback' : url}
        self.service('xiva_subscribe', opts)

    def test_diff(self):
        diff = self.desktop('diff', {'uid': self.uid, 'path' : '/disk'})
        first_version = diff['version']
        self.assertTrue(first_version)
        diff = self.desktop('diff', {'uid': self.uid, 'path' : '/disk', 'version' : first_version})
        self.assertEqual(diff['version'], first_version)
        self.assertEqual(diff['result'], [])
        self.desktop('mkdir', {'uid': self.uid, 'path': '/disk/test'})
        #self.assertEqual(self.response.status, 200)
        diff = self.desktop('diff', {'uid': self.uid, 'path': '/disk', 'version': first_version, 'meta': 'op,key,type'})
        self.assertNotEqual(diff['version'], first_version)
        second_version = diff['version']
        self.assertEqual(diff['amount'], 1)
        has_diff_2 = has_item(all_of(has_entry('op', 'new'),
                                     has_entry('key', '/disk/test'),
                                     has_entry('type', 'dir')))
        assert_that(diff['result'], has_diff_2)

        self.assertFalse(self.desktop('mkdir', {'uid': self.uid, 'path' : '/disk/test/spam'}))
        diff = self.desktop('diff', {'uid': self.uid, 'path' : '/disk', 'version' : first_version, 'meta': 'op,key,type'})
        self.assertEqual(diff['amount'], 2)
        third_version = diff['version']
        has_diff_3 = has_item(all_of(has_entry('op', 'new'),
                                     has_entry('key', '/disk/test/spam'),
                                     has_entry('type', 'dir')))
        assert_that(diff['result'], all_of(has_diff_2, has_diff_3))

        diff = self.desktop('diff', {'uid': self.uid, 'path' : '/disk', 'version' : second_version, 'meta': 'op,key,type'})
        assert_that(diff['result'], has_diff_3)
        diff = self.desktop('diff', {'uid': self.uid, 'path' : '/disk', 'version' : third_version})
        self.assertEqual(len(diff['result']), 0)
        self.assertEqual(diff['amount'], 0)
        diff = self.desktop('diff', {'uid': self.uid, 'path' : '/disk/test/spam'})
        self.assertEqual(diff['version'], third_version)
        faddr = Address.Make(self.uid, '/disk/superfile.txt').id
        result = Bus().mkfile(self.uid, faddr, data=file_fixtures.file_data)
        diff = self.desktop('diff', {'uid': self.uid, 'path' : '/disk'})
        files = filter(lambda x: x['type'] == 'file', diff['result'])
        self.assertEqual(len(files), 1)
        for item in files:
            for file_field in ('md5', 'sha256', 'size'):
                self.assertTrue(file_field in item)
            self.assertTrue(isinstance(item['size'], int))


    def test_info_list_meta_names(self):
        fdir  = Address.Make(self.uid, '/disk/').id
        faddr = Address.Make(self.uid, '/disk/meta_names_file.txt').id
        file_src = Bus().mkfile(self.uid, faddr, data=file_fixtures.file_data)

        info_result = self.desktop('info', {'uid': self.uid, 'path': faddr, 'meta': '', 'meta_names': 1})

        info_meta = info_result.get('meta')
        info_meta_names = info_result.get('meta_names')

        self.assertEqual(type(info_meta), type(dict()))
        self.assertEqual(type(info_meta_names), type(list()))
        self.assertEqual(info_meta.keys().sort(), info_meta_names.sort())

        list_result = self.desktop('list', {'uid': self.uid, 'path': fdir, 'meta': '', 'meta_names': 1})

        for item in list_result:
            if item.get('id') == info_result.get('id'):
                list_meta = item.get('meta')
                list_meta_names = item.get('meta_names')

                self.assertEqual(type(list_meta), type(dict()))
                self.assertEqual(type(list_meta_names), type(list()))
                self.assertEqual(list_meta.keys().sort(), list_meta_names.sort())

                break
        Bus().rm(self.uid, faddr)

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_diff_rename_file(self):
        opts = {
                'uid' : self.uid,
                'path' : '/disk',
                }
        self.service('inspect', opts)
        changelog_db.changelog.remove({'uid' : str(self.uid)})

        opts = {
                'uid' : self.uid,
                'path' : '/disk/folder_with_file'
                }
        self.desktop('mkdir', opts)

        opts = {
                'uid' : self.uid,
                'path' : '/disk',
                }
        version_before = int(self.desktop('diff', opts)['version'])
        initial_file_size = self.upload_file(self.uid, '/disk/folder_with_file/file0')
        opts = {
                'uid' : self.uid,
                'path' : '/disk',
                'version' : version_before,
                }
        diff_after = self.desktop('diff', opts)
        version_after = int(diff_after['version'])
        self.assertTrue(version_after > version_before)

        self.assertEqual(diff_after['amount'], 2, diff_after['result'])
        self.assertEqual(diff_after['result'][0]['key'], '/disk/folder_with_file/file0')
        self.assertEqual(diff_after['result'][0]['op'], 'new')
        self.assertEqual(diff_after['result'][0]['size'], initial_file_size)
        self.assertTrue('mimetype' in diff_after['result'][0])
        self.assertTrue('media_type' in diff_after['result'][0])
        self.assertTrue('etime' in diff_after['result'][0])
        self.assertTrue('mtime' in diff_after['result'][0])
        self.assertTrue('has_preview' in diff_after['result'][0])

        #=======================================================================
        # Переименовываем файл file0->File0
        version_no_file = version_before
        version_file_created = version_after
        version_before = version_after
        opts = {
                'uid' : self.uid,
                'src' : '/disk/folder_with_file/file0',
                'dst' : '/disk/folder_with_file/File0',
                }
        self.desktop('async_move', opts)
        opts = {
                'uid' : self.uid,
                'path' : '/disk',
                'version' : version_before,
                }
        diff_after = self.desktop('diff', opts)
        version_after = int(diff_after['version'])
        self.assertTrue(version_after > version_before)
        self.assertEqual(diff_after['amount'], 3, diff_after['result'])
        for each in diff_after['result']:
            if each['key'] == '/disk/folder_with_file/File0':
                self.assertEqual(each['op'], 'new')
                self.assertEqual(each['size'], initial_file_size)
            elif each['key'] == '/disk/folder_with_file/file0':
                self.assertEqual(each['op'], 'deleted')
            elif each['key'] == '/disk/folder_with_file':
                self.assertEqual(each['op'], 'changed')
            else:
                self.fail(each)
        #=======================================================================
        # Дифф между состоянием, когда не было файла и состоянием,
        # когда его переименовали
        opts = {
                'uid' : self.uid,
                'path' : '/disk',
                'version' : version_no_file,
                }
        diff_after = self.desktop('diff', opts)
        version_after = int(diff_after['version'])
        self.assertTrue(version_after > version_no_file)
        self.assertEqual(diff_after['amount'], 2, diff_after['result'])
        for each in diff_after['result']:
            if each['key'] == '/disk/folder_with_file/File0':
                self.assertEqual(each['op'], 'new')
                self.assertEqual(each['size'], initial_file_size)
            elif each['key'] == '/disk/folder_with_file':
                self.assertEqual(each['op'], 'changed')
            else:
                self.fail(each)
        #=======================================================================
        self.assertTrue(version_no_file < version_file_created)
        chlog = []
        for element in changelog_db.changelog.find({'uid' : str(self.uid)}):
            data = decompress_data(element.pop('zdata'))
            element.update(data)
            if element['version'] > version_file_created:
                chlog.append(element)
#        self.fail(pformat(sorted(chlog, key=lambda x: x['version'])))
        #=======================================================================

        # Обратное переименование файла
        version_before = version_after
        opts = {
                'uid' : self.uid,
                'src' : '/disk/folder_with_file/File0',
                'dst' : '/disk/folder_with_file/file0',
                }
        self.desktop('async_move', opts)
        opts = {
                'uid' : self.uid,
                'path' : '/disk',
                'version' : version_before,
                }
        diff_after = self.desktop('diff', opts)
        version_after = int(diff_after['version'])
        self.assertTrue(version_after > version_before)
        self.assertEqual(diff_after['amount'], 3, diff_after['result'])
        for each in diff_after['result']:
            if each['key'] == '/disk/folder_with_file/file0':
                self.assertEqual(each['op'], 'new')
                self.assertEqual(each['size'], initial_file_size)
            elif each['key'] == '/disk/folder_with_file/File0':
                self.assertEqual(each['op'], 'deleted')
            elif each['key'] == '/disk/folder_with_file':
                self.assertEqual(each['op'], 'changed')
            else:
                self.fail()
        #=======================================================================

        # Дифф между состоянием, когда еще не было файла и состоянием,
        # когда его переименовали 2 раза
        opts = {
                'uid' : self.uid,
                'path' : '/disk',
                'version' : version_no_file,
                }
        diff_after = self.desktop('diff', opts)
        version_after = int(diff_after['version'])
        self.assertTrue(version_after > version_no_file)
        self.assertEqual(len(diff_after['result']), 2, diff_after['result'])
        for each in diff_after['result']:
            if each['key'] == '/disk/folder_with_file/file0':
                self.assertEqual(each['op'], 'new')
                self.assertEqual(each['size'], initial_file_size)
            elif each['key'] == '/disk/folder_with_file':
                self.assertEqual(each['op'], 'changed')
            else:
                self.fail()
        #=======================================================================
        # Дифф между состоянием, когда файл уже был и состоянием,
        # когда его переименовали 2 раза
        opts = {
                'uid' : self.uid,
                'path' : '/disk',
                'version' : version_file_created,
                }
        diff_after = self.desktop('diff', opts)
        version_after = int(diff_after['version'])
        self.assertTrue(version_after > version_file_created)
        self.assertEqual(diff_after['amount'], 2, diff_after['result'])
        for each in diff_after['result']:
            if each['key'] == '/disk/folder_with_file/file0':
                self.assertEqual(each['op'], 'new')
                self.assertEqual(each['size'], initial_file_size)
            elif each['key'] == '/disk/folder_with_file':
                self.assertEqual(each['op'], 'changed')
            else:
                self.fail()

        #=======================================================================
        # Удаляем file0, заливаем File0
        opts = {
                'uid' : self.uid,
                'path' : '/disk/folder_with_file/file0',
                }
        self.mail_ok('async_trash_append', opts)
        new_file_size = self.upload_file(self.uid, '/disk/folder_with_file/File0')
        #=======================================================================

        #=======================================================================
        # Дифф между состоянием, когда файл уже был и состоянием,
        # когда его переименовали 3 раза
        # https://jira.yandex-team.ru/browse/CHEMODAN-7389
        opts = {
                'uid' : self.uid,
                'path' : '/disk',
                'version' : version_file_created,
                }
        diff_after = self.desktop('diff', opts)
        version_after = int(diff_after['version'])
        self.assertTrue(version_after > version_file_created)
        self.assertEqual(diff_after['amount'], 3, diff_after['result'])
        for each in diff_after['result']:
            if each['key'] == '/disk/folder_with_file/File0':
                self.assertEqual(each['op'], 'new')
                self.assertEqual(each['size'], new_file_size)
                self.assertNotEqual(each['size'], initial_file_size)
            elif each['key'] == '/disk/folder_with_file/file0':
                self.assertEqual(each['op'], 'deleted')
            elif each['key'] == '/disk/folder_with_file':
                self.assertEqual(each['op'], 'changed')
            else:
                self.fail()


    def test_fulldiff_with_meta(self):
        opts = {
                'uid' : self.uid,
                'path' : '/disk',
                'meta' : '',
                }
        diff = self.desktop('diff', opts)
        for item in diff['result']:
            if item['type'] == 'file':
                self.assertTrue('mimetype' in item)
                self.assertTrue('media_type' in item)
                self.assertTrue('etime' in item)
                self.assertTrue('mtime' in item)
                self.assertTrue('has_preview' in item)



    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_diff_rename_file(self):
        """
            1) upload file0
            2) rename file0->File0
            3) remove File0
            4) upload file0
            5) check version 1->5
        """
        #=======================================================================
        # Обнуляем проверку: загружаем исходный файл file0
        opts = {'uid' : self.uid, 'path' : '/disk/folder_with_file', }
        self.desktop('mkdir', opts)
        self.upload_file(self.uid, '/disk/folder_with_file/File0')

        opts = {
                'uid' : self.uid,
                'path' : '/disk/folder_with_file/File0',
                }
        self.mail_ok('async_trash_append', opts)
        opts = {
                'uid' : self.uid,
                'path' : '/disk',
                }
        version_no_file = int(self.desktop('diff', opts)['version'])
        initial_file_size = self.upload_file(self.uid, '/disk/folder_with_file/file0')
        opts = {
                'uid' : self.uid,
                'path' : '/disk',
                'version' : version_no_file,
                }
        diff_after = self.desktop('diff', opts)
        version_file_created = int(diff_after['version'])
        self.assertTrue(version_no_file < version_file_created)
        self.assertEqual(len(diff_after['result']), 2, diff_after['result'])
        self.assertEqual(diff_after['result'][0]['key'], '/disk/folder_with_file/file0')
        self.assertEqual(diff_after['result'][0]['op'], 'new')
        self.assertEqual(diff_after['result'][0]['size'], initial_file_size)
        #=======================================================================
        # Переименовываем файл file0->File0
        opts = {
                'uid' : self.uid,
                'src' : '/disk/folder_with_file/file0',
                'dst' : '/disk/folder_with_file/File0',
                }
        self.desktop('async_move', opts)
        opts = {
                'uid' : self.uid,
                'path' : '/disk',
                'version' : version_file_created,
                }
        diff_after = self.desktop('diff', opts)
        version_moved = int(diff_after['version'])
        self.assertTrue(version_file_created < version_moved)
        self.assertEqual(len(diff_after['result']), 3, diff_after['result'])
        for each in diff_after['result']:
            if each['key'] == '/disk/folder_with_file/File0':
                self.assertEqual(each['op'], 'new')
                self.assertEqual(each['size'], initial_file_size)
            elif each['key'] == '/disk/folder_with_file/file0':
                self.assertEqual(each['op'], 'deleted')
            elif each['key'] == '/disk/folder_with_file':
                self.assertEqual(each['op'], 'changed')
            else:
                self.fail()
        #=======================================================================
        # Удаляем File0, заливаем file0
        opts = {
                'uid' : self.uid,
                'path' : '/disk/folder_with_file/File0',
                }
        self.mail_ok('async_trash_append', opts)
        new_file_size = self.upload_file(self.uid, '/disk/folder_with_file/file0')
        #=======================================================================
        # Дифф между состоянием, когда файл уже был и состоянием, когда файл
        # переименовали, удалили переименованный, залили новый с первоначальным именем
        opts = {
                'uid' : self.uid,
                'path' : '/disk',
                'version' : version_file_created,
                }
        diff_after = self.desktop('diff', opts)
        version_after = int(diff_after['version'])
        self.assertTrue(version_after > version_file_created)
        self.assertEqual(len(diff_after['result']), 2, diff_after['result'])
        for each in diff_after['result']:
            if each['key'] == '/disk/folder_with_file/file0':
                self.assertEqual(each['op'], 'new')
                self.assertEqual(each['size'], new_file_size)
                self.assertNotEqual(each['size'], initial_file_size)
            elif each['key'] == '/disk/folder_with_file':
                self.assertEqual(each['op'], 'changed')
            else:
                self.fail()
        #=======================================================================

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_trash_append_file(self):
        opts = {'uid' : self.uid, 'path' : '/disk/folder_with_file', }
        self.desktop('mkdir', opts)
        self.upload_file(self.uid, '/disk/folder_with_file/file0')

        opts = {'uid' : self.uid, 'path' : '/disk', }
        diff_before = self.desktop('diff', opts)
        old_version = diff_before['version']
        opts = {'uid' : self.uid, 'path' : '/disk/folder_with_file/file0', }
        open_url_data = set_up_open_url()
        self.mail_ok('async_trash_append', opts)
        tear_down_open_url()
        url = 'http://localhost/service/echo?uid='
        # self.fail(open_url_data)

        for k, v in open_url_data.iteritems():
            if k.startswith(url):
                uid = re.search('http://localhost/service/echo\?uid=(\d+)', k).group(1)
                for each in v:
                    share_tag = etree.fromstring(each['pure_data'])
#                    self.fail(etree.tostring(share_tag, pretty_print=True))
                    if share_tag.tag == 'diff':
                        self.assertEqual(share_tag.get('old'), old_version)
                        new_version = share_tag.get('new')
                        self.assertEqual(old_version, share_tag.get('old'))
                        self.assertNotEqual(old_version, new_version)
                        self.assertTrue(int(old_version) < int(new_version))
                        self.assertEqual(len(list(share_tag.iterfind('op'))), 3)
                        for op in share_tag.iterfind('op'):
                            if op.get('key') == '/disk/folder_with_file/file0':
                                self.assertEqual(uid, str(self.uid))
                                self.assertEqual(op.get('type'), 'deleted')
                            elif op.get('key').startswith('/trash/file0'):
                                self.assertEqual(uid, str(self.uid))
                                self.assertEqual(op.get('type'), 'new')
                            elif op.get('key') == '/disk/folder_with_file':
                                self.assertEqual(uid, str(self.uid))
                                self.assertEqual(op.get('type'), 'changed')
                            else:
                                self.fail(op.get('key'))
        opts = {
                'uid' : self.uid,
                'path' : '/disk',
                'version' : old_version,
                }
        diff_after = self.desktop('diff', opts)
        self.assertNotEqual(old_version, diff_after['version'])

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_put_old_file(self):
        """
            Создается файл, затирается новым, новый удаляется в корзину,
            на его место заливают первоначальный.
            Берется дифф до первой загрузки и в конце.
        """
        opts = {
                'uid' : self.uid,
                'path' : '/disk',
                }
        diff = self.desktop('diff', opts)

        version_no_file = int(diff['version'])
        self.upload_file(self.uid, '/disk/first_file.txt')
        opts = {
                'uid' : self.uid,
                'path' : '/disk/first_file.txt',
                'meta' : '',
                }
        file_info_1 = self.desktop('info', opts)
        file_1_md5 = file_info_1['meta']['md5']
        file_1_sha256 = file_info_1['meta']['sha256']
        opts = {
                'uid' : self.uid,
                'path' : '/disk',
                }
        diff = self.desktop('diff', opts)
        version_file_created = int(diff['version'])
        xiva_requests = {}
        self.upload_file(self.uid, '/disk/first_file.txt', open_url_data=xiva_requests)
        pushes = []
        for k, v in xiva_requests.iteritems():
            if k.startswith('http://localhost/service/echo'):
                pushes.append(v)
        self.assertEqual(len(pushes), 2)
        self.assertEqual(len(pushes[0]), 1)
        body = etree.fromstring(pushes[0][0]['pure_data'])
        self.assertEqual(len(list(body.iterchildren())), 1)
        self.assertEqual(body.find('op').get('type'), 'changed')
        opts = {
                'uid' : self.uid,
                'path' : '/disk/first_file.txt',
                'meta' : '',
                }
        file_info_2 = self.desktop('info', opts)
        file_2_md5 = file_info_2['meta']['md5']
        file_2_sha256 = file_info_2['meta']['sha256']
        opts = {
                'uid' : self.uid,
                'path' : '/disk',
                }
        diff = self.desktop('diff', opts)
        version_file_updated = int(diff['version'])
        opts = {
                'uid' : self.uid,
                'path' : '/disk/first_file.txt',
                'meta' : '',
                }
        file_info_2 = self.desktop('trash_append', opts)
        file_data = {
                     'md5' : file_1_md5,
                     'sha256' : file_1_sha256,
                     }
        opts = {
                'uid' : self.uid,
                'path' : '/disk',
                }
        diff = self.desktop('diff', opts)
        version_file_removed = int(diff['version'])
        self.upload_file(self.uid, '/disk/first_file.txt', file_data=file_data)
        opts = {
                'uid' : self.uid,
                'path' : '/disk',
                'version' : version_no_file,
                }
        diff_second_client = self.desktop('diff', opts)
        self.assertNotEqual(diff_second_client['result'][0]['md5'], file_2_md5)
        self.assertNotEqual(diff_second_client['result'][0]['sha256'], file_2_sha256)
        self.assertEqual(diff_second_client['result'][0]['md5'], file_1_md5)
        self.assertEqual(diff_second_client['result'][0]['sha256'], file_1_sha256)

    def test_CHEMODAN_7899(self):
        opts = {'uid': self.uid,
                'path': '/disk'}
        diff = self.desktop('diff', opts)
        version_no_file = int(diff['version'])
        self.upload_file(self.uid, '/disk/first_file.txt')
        opts = {'uid': self.uid,
                'path': '/disk/first_file.txt',
                'meta': ''}
        file_info_1 = self.desktop('info', opts)
        file_1_md5 = file_info_1['meta']['md5']
        file_1_sha256 = file_info_1['meta']['sha256']
        opts = {'uid': self.uid,
                'path': '/disk'}
        diff = self.desktop('diff', opts)
        version_file_created = int(diff['version'])
        self.assertTrue(version_no_file < version_file_created)
        self.dstore_file(self.uid, '/disk/first_file.txt')
        opts = {'uid': self.uid,
                'path': '/disk/first_file.txt',
                'meta': ''}
        file_info_2 = self.desktop('info', opts)
        file_2_md5 = file_info_2['meta']['md5']
        file_2_sha256 = file_info_2['meta']['sha256']
        self.assertNotEqual(file_1_md5, file_2_md5)
        self.assertNotEqual(file_1_sha256, file_2_sha256)
        opts = {'uid': self.uid,
                'path': '/disk/first_file.txt',
                'visible': 0}
        self.desktop('setprop', opts)
        opts = {'uid': self.uid,
                'path': '/disk'}
        diff = self.desktop('diff', opts)
        version_file_updated = int(diff['version'])
        self.assertTrue(version_file_created < version_file_updated)
        opts = {'uid': self.uid,
                'path': '/disk/first_file.txt',
                'meta': ''}
        self.desktop('trash_append', opts)
        file_data = {'md5': file_1_md5,
                     'sha256': file_1_sha256}
        opts = {'uid': self.uid,
                'path': '/disk'}
        diff = self.desktop('diff', opts)
        version_file_removed = int(diff['version'])
        self.assertTrue(version_file_updated < version_file_removed)
        self.upload_file(self.uid, '/disk/first_file.txt', file_data=file_data)
        opts = {'uid': self.uid,
                'path': '/disk'}
        diff = self.desktop('diff', opts)
        version_file_recreated = int(diff['version'])
        self.assertTrue(version_file_removed < version_file_recreated)
        chlog = []
        for element in changelog_db.changelog.find({'uid' : str(self.uid)}):
            data = decompress_data(element.pop('zdata'))
            element.update(data)
            if element['version'] > version_no_file:
                chlog.append(element)

        opts = {'uid': self.uid,
                'path': '/disk',
                'version': version_no_file}
        diff_second_client = self.desktop('diff', opts)
        self.assertEqual(len(diff_second_client['result']), 1)
        self.assertNotEqual(diff_second_client['result'][0]['md5'], file_2_md5)
        self.assertNotEqual(diff_second_client['result'][0]['sha256'], file_2_sha256)
        self.assertEqual(diff_second_client['result'][0]['md5'], file_1_md5)
        self.assertEqual(diff_second_client['result'][0]['sha256'], file_1_sha256)
        opts = {'uid': self.uid,
                'path': '/disk',
                'version': version_file_created}
        diff_second_client = self.desktop('diff', opts)
        self.assertEqual(diff_second_client['result'][0]['md5'], file_1_md5)
        self.assertEqual(diff_second_client['result'][0]['sha256'], file_1_sha256)
        opts = {'uid': self.uid,
                'path': '/disk',
                'version': version_file_updated}
        diff_second_client = self.desktop('diff', opts)
        self.assertEqual(diff_second_client['result'][0]['md5'], file_1_md5)
        self.assertEqual(diff_second_client['result'][0]['sha256'], file_1_sha256)

    def test_mksysdir(self):
        opts = {
            'uid': self.uid,
            'type': 'downloads',
        }
        result = self.desktop('mksysdir', opts)
        self.assertEqual(result['id'], u'/disk/Загрузки/')

    def test_long_diff(self):
        """
            https://jira.yandex-team.ru/browse/CHEMODAN-11472
        """
        return
        path = '/disk/long_diff_folder'
        opts = {
                'uid' : self.uid,
                'path' : path,
                }
        self.json_ok('mkdir', opts)
        for i in xrange(1, 4):
            opts = {
                    'uid' : self.uid,
                    'path' : path + '/folder_1.%s' % i,
                    }
            self.json_ok('mkdir', opts)
            for j in xrange(1, 4):
                opts = {
                        'uid' : self.uid,
                        'path' : path + '/folder_1.%s/level_2.%s' % (i, j),
                        }
                self.json_ok('mkdir', opts)
                for k in xrange(1, 5):
                    opts = {
                            'uid' : self.uid,
                            'path' : path + '/folder_1.%s/level_2.%s/level_3.%s' % (i, j, k),
                            }
                    self.json_ok('mkdir', opts)
        opts = {
                'uid' : self.uid,
                'path' : '/disk/long_diff_parent',
                }
        self.json_ok('mkdir', opts)
        initial_diff_opts = {
                     'uid' : self.uid,
                     'path' : '/disk',
                     }
        diff_result = self.json_ok('diff', initial_diff_opts)
        initial_version = diff_result['version']
        opts = {
                'uid' : self.uid,
                'src' : '/disk/long_diff_folder',
                'dst' : '/disk/long_diff_parent/long_diff_folder'
                }
        move_thread = threading.Thread(target=self.json_ok, args=('async_move', opts))
        middle_diff_opts = {
                            'uid' : self.uid,
                            'path' : '/disk',
                            }
        final_diff_opts = {
                            'uid' : self.uid,
                            'path' : '/disk',
                            }
        move_thread.start()
        middle_diff_result = self.json_ok('diff', middle_diff_opts)
        move_thread.join()
        middle_version = middle_diff_result['version']
        final_diff_result = self.json_ok('diff', final_diff_opts)
        final_version = final_diff_result['version']
        self.assertNotEqual(len(filter(lambda x: x['key'].startswith('/disk/long_diff_folder'), middle_diff_result['result'])), 0)
        self.assertEqual(len(filter(lambda x: x['key'].startswith('/disk/long_diff_folder'), final_diff_result['result'])), 0)
        for each in middle_diff_result['result']:
            if each['key'].startswith('/disk/long_diff_parent'):
                for each in middle_diff_result['result']:
                    self.assertFalse(each['key'].startswith('/disk/long_diff_folder'), each['key'])
                break
        self.assertEqual(len(filter(lambda x: x['key'].startswith('/disk/long_diff_folder'), middle_diff_result['result'])),
                            len(filter(lambda x: x['key'].startswith('/disk/long_diff_folder'), final_diff_result['result'])))
        self.assertEqual(len(filter(lambda x: x['key'].startswith('/disk/long_diff_parent'), middle_diff_result['result'])),
                            len(filter(lambda x: x['key'].startswith('/disk/long_diff_parent'), final_diff_result['result'])))

    def test_trash_is_empty(self):
        opts = {
                'uid' : self.uid,
                'path' : '/trash',
                'meta' : 'empty,file_id',
                }
        result = self.json_ok('info', opts)
        self.assertTrue('empty' in result['meta'])
        self.assertNotEqual(result['meta']['empty'], None)
        opts = {
                'uid' : self.uid,
                'path' : '/trash',
                'meta' : 'file_id',
                }
        result = self.json_ok('info', opts)
        self.assertFalse('empty' in result['meta'])
        opts = {
                'uid' : self.uid,
                'path' : '/trash',
                'meta' : '',
                }
        result = self.json_ok('info', opts)
        self.assertTrue('empty' in result['meta'])
        self.assertNotEqual(result['meta']['empty'], None)
        opts = {
                'uid' : self.uid,
                'path' : '/trash',
                'meta' : '',
                }
        result = self.json_ok('list', opts)
        self.assertTrue('empty' in result[0]['meta'])
        self.assertEqual(result[0]['meta']['empty'], None)

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_disallow_pdd(self):
        from mpfs.config import settings
        settings.feature_toggles['allow_pdd'] = False
        self.json_error('user_init', {'uid': self.pdd_uid}, code=79)
