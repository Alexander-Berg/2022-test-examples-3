# -*- coding: utf-8 -*-
import mock
from nose_parameterized import parameterized
from test.base import DiskTestCase
from mpfs.core.factory import get_resource
from mpfs.core.filesystem.cleaner.models import DeletedStid
from test.helpers.stubs.services import MulcaServiceStub


class MarkStidDeletedTestCase(DiskTestCase):

    def test_mark_stid_deleted(self):
        deleted_stid = '4770.yadisk:162819441.1644407979128345854531949721022'
        with MulcaServiceStub(), \
             mock.patch('mpfs.core.filesystem.cleaner.controllers.DeletedStidsController.bulk_create') as stub:
                self.service_ok('mark_stid_deleted', {'stid': deleted_stid})
        deleted_stids_list = stub.call_args[0][0]
        self.assertEqual(len(deleted_stids_list), 1)
        self.assertEqual(deleted_stid, deleted_stids_list[0].stid)
        deleted_stids_list[0].delete()

    def test_mark_stid_deleted_empty_value(self):
        deleted_stid = ''
        with MulcaServiceStub():
            self.service_ok('mark_stid_deleted', {'stid': deleted_stid})
        self.assertEqual(len(DeletedStid.controller.filter()), 0)


class BulkCheckStidsTestCase(DiskTestCase):
    def test_common(self):
        path = '/disk/1.txt'
        self.upload_file(self.uid, path)
        resource = get_resource(self.uid, path)
        exist_stids = [resource.meta['file_mid'], resource.meta['pmid'], resource.meta['digest_mid']]
        not_exist_stids = ['fake:1', 'fake:2']

        resp = self.service_ok('bulk_check_stids', {'service': 'test'}, json={'stids': exist_stids + not_exist_stids})
        assert 'items' in resp
        for resp_item in resp['items']:
            if resp_item['stid'] in not_exist_stids:
                assert resp_item['in_db'] == False
            elif resp_item['stid'] in exist_stids:
                assert resp_item['in_db'] == True
            else:
                raise ValueError()

    @parameterized.expand([
        (None,),
        ([],),
        ({},),
        ({'stids': {}},)
    ])
    def test_bad_body(self, body):
        resp = self.service_error('bulk_check_stids', {'service': 'test'}, json=body)
        assert resp['title'] == 'BadRequestError'


class DVDataTestCase(DiskTestCase):
    def test_dv_data_common_file(self):
        path = '/disk/1.txt'
        self.upload_file(self.uid, path)
        resource = get_resource(self.uid, path)
        resp = self.service_ok('dv_data', {'uid': self.uid, 'path': path})
        assert resp['mimetype']
        assert resp['name'] == '1.txt'
        assert resp['media_type']
        assert resp['file_stid'] == resource.file_mid()

    def test_dv_data_version(self):
        path = '/disk/1.txt'
        self.upload_file(self.uid, path)
        self.upload_file(self.uid, path)
        resource = get_resource(self.uid, path)
        resp = self.json_ok('versioning_get_checkpoints', {'uid': self.uid, 'resource_id': resource.resource_id.serialize()})
        assert len(resp['versions']) == 2
        version = resp['versions'][-1]
        resp = self.service_ok('dv_data', {'uid': self.uid, 'path': path, 'version_id': version['id']})
        assert resp['mimetype']
        assert resp['name'] == '1.txt'
        assert resp['media_type']
        assert resp['file_stid'] == version['file_stid']

    def test_public_dv_data(self):
        path = '/disk/1.txt'
        self.upload_file(self.uid, path)
        resource = get_resource(self.uid, path)
        private_hash = self.json_ok('set_public', {'uid': self.uid, 'path': path})['hash']
        resp = self.service_ok('public_dv_data', {'private_hash': private_hash})
        assert resp['mimetype']
        assert resp['name'] == '1.txt'
        assert resp['media_type']
        assert resp['file_stid'] == resource.file_mid()
