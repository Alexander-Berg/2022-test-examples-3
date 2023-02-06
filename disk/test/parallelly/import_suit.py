# -*- coding: utf-8 -*-
from mpfs.common.static import codes
from test.base import DiskTestCase
from test.fixtures.kladun import KladunMocker


class ImportFileFromServiceTestCase(DiskTestCase):
    DOWNLOAD_FOLDER_PATH = u'/disk/Загрузки'
    FILE_NAME = 'pagefile.sys'
    FILE_PATH = DOWNLOAD_FOLDER_PATH + '/' + FILE_NAME

    def test_import_from_mail_to_disk_with_new_filename(self):
        opts = {
            'uid': self.uid,
            'service_id': 'only-for-kladun',
            'service_file_id': 'only-for-kladun',
            'file_name': self.FILE_NAME,
        }
        response = self.json_ok('import_file_from_service', opts)

        assert 'oid' in response
        oid = response['oid']

        KladunMocker().mock_kladun_callbacks_for_upload_from_service(self.uid, oid)

        operation_status = self.json_ok('status', {'uid': self.uid, 'oid': oid})
        assert operation_status['status'] == 'DONE'
        assert operation_status['state'] == 'COMPLETED'
        assert operation_status['resource']['path'] == self.FILE_PATH

        response = self.json_ok('info', {'uid': self.uid, 'path': self.uid + ':' + self.FILE_PATH})
        assert response['path'] == self.FILE_PATH

    def test_import_from_mail_to_disk_with_existing_filename_with_autosuffix(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': self.DOWNLOAD_FOLDER_PATH})
        self.upload_file(self.uid, self.FILE_PATH)

        opts = {
            'uid': self.uid,
            'service_id': 'only-for-kladun',
            'service_file_id': 'only-for-kladun',
            'file_name': self.FILE_NAME,
            'overwrite': 0,
            'autosuffix': 1,
        }
        response = self.json_ok('import_file_from_service', opts)

        assert 'oid' in response
        oid = response['oid']

        KladunMocker().mock_kladun_callbacks_for_upload_from_service(self.uid, oid)

        operation_status = self.json_ok('status', {'uid': self.uid, 'oid': oid})
        assert operation_status['status'] == 'DONE'
        assert operation_status['state'] == 'COMPLETED'
        assert operation_status['resource']['path'] != self.FILE_PATH
        new_path = operation_status['resource']['path']

        response = self.json_ok('info', {'uid': self.uid, 'path': self.uid + ':' + self.FILE_PATH})
        assert response['path'] == self.FILE_PATH
        response = self.json_ok('info', {'uid': self.uid, 'path': self.uid + ':' + new_path})
        assert response['path'] == new_path

    def test_import_from_mail_to_disk_with_existing_filename_with_overwrite(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': self.DOWNLOAD_FOLDER_PATH})
        self.upload_file(self.uid, self.FILE_PATH)

        opts = {
            'uid': self.uid,
            'service_id': 'only-for-kladun',
            'service_file_id': 'only-for-kladun',
            'file_name': self.FILE_NAME,
            'overwrite': 1,
            'autosuffix': 0,
        }
        response = self.json_ok('import_file_from_service', opts)

        assert 'oid' in response
        oid = response['oid']

        KladunMocker().mock_kladun_callbacks_for_upload_from_service(self.uid, oid)

        operation_status = self.json_ok('status', {'uid': self.uid, 'oid': oid})
        assert operation_status['status'] == 'DONE'
        assert operation_status['state'] == 'COMPLETED'
        assert operation_status['resource']['path'] == self.FILE_PATH

        response = self.json_ok('info', {'uid': self.uid, 'path': self.uid + ':' + self.FILE_PATH})
        assert response['path'] == self.FILE_PATH

    def test_import_from_mail_to_disk_with_existing_filename_without_overwrite_and_autosuffix_fails(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': self.DOWNLOAD_FOLDER_PATH})
        self.upload_file(self.uid, self.FILE_PATH)
        opts = {
            'uid': self.uid,
            'service_id': 'only-for-kladun',
            'service_file_id': 'only-for-kladun',
            'file_name': self.FILE_NAME,
            'overwrite': 0,
            'autosuffix': 0,
        }

        self.json_error('import_file_from_service', opts, code=codes.FILE_EXISTS)
