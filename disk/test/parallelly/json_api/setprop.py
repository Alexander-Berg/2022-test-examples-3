# -*- coding: utf-8 -*-
import random
import re
import string

import pytest

from lxml import etree

from base import CommonJsonApiTestCase
from mpfs.config import settings
from test.base_suit import set_up_open_url, tear_down_open_url


SETPROP_SYMLINK_FIELDNAME = settings.system['setprop_symlink_fieldname']


class SetPropJsonApiTestCase(CommonJsonApiTestCase):

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_setprop_file(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/mail_attach'})
        self.upload_file(self.uid, '/disk/mail_attach/file1.txt')
        opts = {
            'uid': self.uid,
        }
        diff = self.json_ok('diff', opts)
        old_version = diff['version']
        opts = {
            'uid': self.uid,
            'path': '/disk/mail_attach/file1.txt',
            'foo': 'spam',
        }
        xiva_requests = set_up_open_url()
        self.json_ok('setprop', opts)
        tear_down_open_url()
        notified = False
        for k, v in xiva_requests.iteritems():
            if k.startswith('http://localhost/service/echo?uid='):
                for each in v:
                    uid = re.search('http://localhost/service/echo\?uid=(\d+)', k).group(1)
                    self.assertEqual(uid, self.uid)
                    data = etree.fromstring(each['pure_data'])
                    if data.tag == 'diff':
                        self.assertEqual(len(list(data.iterfind('op'))), 2)
                        for each in data.iterfind('op'):
                            if each.get('key') == '/disk/mail_attach/file1.txt':
                                self.assertEqual(each.get('type'), 'changed', etree.tostring(each))
                                self.assertEqual(each.get('action'), 'setprop', etree.tostring(each))
                                self.assertEqual(each.get('external_setprop'), '1', etree.tostring(each))
                            elif each.get('key') == '/disk/mail_attach':
                                self.assertEqual(each.get('type'), 'changed', etree.tostring(each))
                                self.assertEqual(each.get('action'), None, etree.tostring(each))
                                self.assertEqual(each.get('external_setprop'), None, etree.tostring(each))
                            else:
                                self.fail(etree.tostring(each))
                        notified = True
        self.assertTrue(notified)

        # diff with version
        opts = {
            'uid': self.uid,
            'version': old_version,
            'meta': '',
        }
        diff = self.json_ok('diff', opts)
        self.assertEqual(len(diff['result']), 2, diff)
        self.assertEqual(diff['result'][0]['external_setprop'], 1)

        # diff without version
        opts = {
            'uid': self.uid,
            'meta': '',
        }
        diff = self.json_ok('diff', opts)
        found = False
        for resource in diff['result']:
            if resource['key'] == '/disk/mail_attach/file1.txt':
                self.assertTrue('external_setprop' in resource)
                self.assertEqual(resource['external_setprop'], 1)
                found = True
        self.assertTrue(found)

    def test_setprop_and_internal_fields(self):
        """
        Если в setprop передаются внутренние поля, то после таска https://st.yandex-team.ru/CHEMODAN-22019
        НЕ raise'им 400 ошибку, а тупо проставляем что передали.
        """
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/d'})
        # set

        new_file_id = ''.join([random.choice(string.hexdigits.lower()) for _ in xrange(64)])
        self.json_ok('setprop', {'uid': self.uid, 'path': '/disk/d', 'foo': 'spam', 'file_id': new_file_id})
        self.json_ok('setprop', {'uid': self.uid, 'path': '/disk/d', 'foo': 'spam', 'fa': 'fa'})

        info = self.json_ok('info', {'uid': self.uid, 'path': '/disk/d', 'meta': ''})
        assert info['meta']['file_id'] == new_file_id
        assert 'foo' in info['meta']
        assert 'fa' in info['meta']
        assert info['meta']['foo'] == 'spam'
        assert info['meta']['fa'] == 'fa'

        # delete
        self.json_ok('setprop', {'uid': self.uid, 'path': '/disk/d', 'setprop_delete': 'foo,fa'})

        info = self.json_ok('info', {'uid': self.uid, 'path': '/disk/d', 'meta': ''})
        assert info['meta']['file_id'] == new_file_id
        assert 'foo' not in info['meta']
        assert 'fa' not in info['meta']

    def test_diff_and_external_setprop(self):
        """
        https://st.yandex-team.ru/CHEMODAN-20585

        Ожидается, что флаг 'external_setprop' будет передан в diff-е при
        любых изменениях, пока свойство установленo
        """
        file_path = '/disk/mail_attach/file1.txt'
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/mail_attach'})
        self.upload_file(self.uid, file_path)

        old_version = self.json_ok('diff', {'uid': self.uid})['version']
        opts = {
            'uid': self.uid,
            'version': old_version,
        }

        self.json_ok('setprop', {'uid': self.uid, 'path': file_path, 'foo': 'spam'})
        diff = self.json_ok('diff', opts)
        assert 'external_setprop' not in diff['result'][0]

        self.json_ok('setprop', {'uid': self.uid, 'path': file_path, SETPROP_SYMLINK_FIELDNAME: 'spam'})
        diff = self.json_ok('diff', opts)
        assert 'external_setprop' in diff['result'][0]
        assert diff['result'][0]['external_setprop'] == 1

        self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/mail_attach/file1.txt'})
        diff = self.json_ok('diff', opts)
        assert 'external_setprop' in diff['result'][0]
        assert diff['result'][0]['external_setprop'] == 1

        self.json_ok('setprop', {'uid': self.uid, 'path': '/disk/mail_attach/file1.txt', 'setprop_delete': 'foo'})
        diff = self.json_ok('diff', opts)
        assert 'external_setprop' in diff['result'][0]
        assert diff['result'][0]['external_setprop'] == 1

        self.json_ok('setprop', {
            'uid': self.uid, 'path': '/disk/mail_attach/file1.txt', 'setprop_delete': SETPROP_SYMLINK_FIELDNAME})
        diff = self.json_ok('diff', opts)
        assert 'external_setprop' not in diff['result'][0]

    def test_setprop_renamed_file(self):
        self.upload_file(self.uid, '/disk/test_setprop')

        diff = self.json_ok('diff', {'uid': self.uid})
        old_version = diff['version']

        self.json_ok('setprop', {'uid': self.uid, 'path': '/disk/test_setprop', 'test_setprop': '1'})
        version_diff = self.json_ok('diff', {'uid': self.uid, 'version': old_version})
        assert 'external_setprop' not in version_diff['result'][0]

        self.json_ok('setprop', {'uid': self.uid, 'path': '/disk/test_setprop', SETPROP_SYMLINK_FIELDNAME: '1'})
        version_diff = self.json_ok('diff', {'uid': self.uid, 'version': old_version})
        assert 'external_setprop' in version_diff['result'][0]
        assert version_diff['result'][0]['external_setprop'] == 1

        self.json_ok('async_move', {'uid': self.uid, 'src': '/disk/test_setprop', 'dst': '/disk/new_test_setprop'})
        version_diff = self.json_ok('diff', {'uid': self.uid, 'version': old_version})
        assert 'external_setprop' in version_diff['result'][0]
        assert version_diff['result'][0]['external_setprop'] == 1

    def test_setprop_folder(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/mail_attach'})
        opts = {
            'uid': self.uid,
        }
        diff = self.json_ok('diff', opts)
        old_version = diff['version']
        opts = {
            'uid': self.uid,
            'path': '/disk/mail_attach',
            SETPROP_SYMLINK_FIELDNAME: 'spam',
        }
        xiva_requests = set_up_open_url()
        self.json_ok('setprop', opts)
        tear_down_open_url()
        notified = False
        for k, v in xiva_requests.iteritems():
            if k.startswith('http://localhost/service/echo?uid='):
                for each in v:
                    uid = re.search('http://localhost/service/echo\?uid=(\d+)', k).group(1)
                    self.assertEqual(uid, self.uid)
                    data = etree.fromstring(each['pure_data'])
                    if data.tag == 'diff':
                        self.assertEqual(len(list(data.iterfind('op'))), 1)
                        self.assertEqual(data.find('op').get('type'), 'changed')
                        self.assertEqual(data.find('op').get('key'), '/disk/mail_attach')
                        self.assertEqual(data.find('op').get('action'), 'setprop')
                        self.assertEqual(data.find('op').get('external_setprop'), '1')
                        notified = True
        self.assertTrue(notified)

        # diff with version
        opts = {
            'uid': self.uid,
            'version': old_version,
            'meta': '',
        }
        diff = self.json_ok('diff', opts)
        self.assertEqual(len(diff['result']), 1, diff)
        self.assertEqual(diff['result'][0]['external_setprop'], 1, diff)

        # diff without version
        opts = {
            'uid': self.uid,
            'meta': '',
        }
        diff = self.json_ok('diff', opts)
        found = False
        for resource in diff['result']:
            if resource['key'] == '/disk/mail_attach':
                self.assertTrue('external_setprop' in resource)
                self.assertEqual(resource['external_setprop'], 1)
                found = True
        self.assertTrue(found)
