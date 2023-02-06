# -*- coding: utf-8 -*-
import mock

from nose_parameterized import parameterized

from mpfs.common.static import codes
from mpfs.core.operations import manager
from mpfs.core.operations.mail_attaches import ImportMailAttaches
from test.base import DiskTestCase
from test.fixtures.kladun import KladunMocker

class ImportAttachToDiskTestCase(DiskTestCase):
    """Тестируем перекладывание аттача из почты в диск по почтовому mid/hid"""
    def check_import_attach_to_disk(self, overwrite, autosuffix, file_exists,
                                    expect_content_changes, expect_new_filename):
        file_path = '/disk/test-file.txt'
        file_path_with_suffix = '/disk/test-file (1).txt'
        if file_exists:
            self.upload_file(self.uid, file_path)
            res = self.json_ok('info', {'uid': self.uid, 'path': self.uid + ':' + file_path, 'meta': 'md5'})
            existing_file_md5 = res['meta']['md5']

        opts = {'uid': self.uid,
                'mail_mid': '123435627147236458163',
                'mail_hid': '1.1',
                'dst': file_path,
                'overwrite': overwrite,
                'autosuffix': autosuffix}
        response = self.json_ok('import_attach_to_disk', opts)

        assert 'oid' in response
        oid = response['oid']

        KladunMocker().mock_kladun_callbacks_for_upload_from_service(self.uid, oid)

        operation_status = self.json_ok('status', {'uid': self.uid, 'oid': oid})
        assert operation_status['status'] == 'DONE'
        assert operation_status['state'] == 'COMPLETED'

        response = self.json_ok('info', {'uid': self.uid, 'path': self.uid + ':' + file_path})
        assert response['path'] == file_path
        if expect_new_filename:
            assert operation_status['resource']['path'] == file_path_with_suffix
            response = self.json_ok('info', {'uid': self.uid, 'path': self.uid + ':' + file_path_with_suffix})
            assert response['path'] == file_path_with_suffix
        else:
            assert operation_status['resource']['path'] == file_path
            self.json_error('info', {'uid': self.uid, 'path': self.uid + ':' + file_path_with_suffix},
                            code=codes.RESOURCE_NOT_FOUND)

        if file_exists:
            response = self.json_ok('info', {'uid': self.uid, 'path': self.uid + ':' + file_path, 'meta': 'md5'})
            if expect_content_changes:
                assert response['meta']['md5'] != existing_file_md5, 'File contents not changed'
            else:
                assert response['meta']['md5'] == existing_file_md5, 'File contents changed'

    def test_import_attach_to_disk(self):
        self.check_import_attach_to_disk(overwrite='0', autosuffix='0', file_exists=False,
                                         expect_content_changes=False, expect_new_filename=False)

    def test_import_attach_to_disk_with_overwrite_without_old_file(self):
        self.check_import_attach_to_disk(overwrite='1', autosuffix='0', file_exists=False,
                                         expect_content_changes=False, expect_new_filename=False)

    def test_import_attach_to_disk_with_overwrite_with_old_file(self):
        self.check_import_attach_to_disk(overwrite='1', autosuffix='0', file_exists=True,
                                         expect_content_changes=True, expect_new_filename=False)

    def test_import_attach_to_disk_with_autosuffix_without_old_file(self):
        self.check_import_attach_to_disk(overwrite='0', autosuffix='1', file_exists=False,
                                         expect_content_changes=False, expect_new_filename=False)

    def test_import_attach_to_disk_with_autosuffix_with_old_file(self):
        self.check_import_attach_to_disk(overwrite='0', autosuffix='1', file_exists=True,
                                         expect_content_changes=False, expect_new_filename=True)

    def test_import_attach_to_disk_with_overwrite_and_autosuffix_fails(self):
        file_path = '/disk/test-file.txt'
        opts = {'uid': self.uid,
                'mail_mid': '123435627147236458163',
                'mail_hid': '1.1',
                'dst': file_path,
                'overwrite': '1',
                'autosuffix': '1'}

        self.json_error('import_attach_to_disk', opts, code=codes.CODE_ERROR, title='BadArguments')

    def check_import_attach_to_disk_without_overwrite_and_autosuffix_with_old_file_fails(self, autosuffix):
        file_path = '/disk/test-file.txt'
        self.upload_file(self.uid, file_path, file_data={'etime': "111"})
        res = self.json_ok('info', {'uid': self.uid, 'path': self.uid + ':' + file_path, 'meta': 'md5'})
        existing_file_md5 = res['meta']['md5']

        opts = {'uid': self.uid,
                'mail_mid': '123435627147236458163',
                'mail_hid': '1.1',
                'dst': file_path,
                'overwrite': '0'
        }
        if autosuffix is not None:
            opts['autosuffix'] = autosuffix

        self.json_error('import_attach_to_disk', opts, code=codes.FILE_EXISTS)
        response = self.json_ok('info', {'uid': self.uid, 'path': self.uid + ':' + file_path, 'meta': 'md5'})
        assert response['meta']['md5'] == existing_file_md5, 'File contents changed'

    def test_import_attach_to_disk_without_overwrite_and_autosuffix_with_old_file_fails(self):
        self.check_import_attach_to_disk_without_overwrite_and_autosuffix_with_old_file_fails(autosuffix='0')

    def test_autosuffix_default_off(self):
        self.check_import_attach_to_disk_without_overwrite_and_autosuffix_with_old_file_fails(autosuffix=None)


class ImportAttachesToDiskTestCase(DiskTestCase):
    """Тестируем пакетное перекладывание аттачей из почты в диск по почтовому mid/hid"""

    def check_statuses(self, oid, expected_statuses):
        operation_status = self.json_ok('status', {'uid': self.uid, 'oid': oid})
        statuses = [operation_status['status']] + [subop['status'] for subop in operation_status['protocol']]
        assert list(expected_statuses) == list(statuses)

    @parameterized.expand([(1,), (2,)])
    def test_import_attaches_to_disk(self, count):
        dest_path = '/disk'
        attaches = [
                       {'mid': '123435627147236458163', 'hid': '1.1', 'file_name': 'test-file-1.txt'},
                       {'mid': '123435627147236458164', 'hid': '1.1', 'file_name': 'test-file-2.txt'},
                   ][:count]
        params = {'uid': self.uid,
                  'path': dest_path,
                  'overwrite': '0',
                  'autosuffix': '0'}
        data = {'items': attaches}
        response = self.json_ok('import_attaches_to_disk', opts=params, json=data)

        oid = response['oid']
        operation_status = self.json_ok('status', {'uid': self.uid, 'oid': oid})

        for subop in operation_status['protocol']:
            subop_oid = subop['oid']
            KladunMocker().mock_kladun_callbacks_for_upload_from_service(self.uid, subop_oid)

        assert len(attaches) == len(operation_status['protocol'])
        for subop in operation_status['protocol']:
            assert 'WAITING' == subop['status']

        manager.get_operation(self.uid, oid).process()

        operation_status = self.json_ok('status', {'uid': self.uid, 'oid': oid})
        assert 'DONE' == operation_status['status']

        for attach in attaches:
            file_name = attach['file_name']
            file_path = dest_path + '/' + file_name
            self.json_ok('info', {'uid': self.uid, 'path': self.uid + ':' + file_path})

    @parameterized.expand([
        ('0', '0', False, False, False),
        ('1', '0', False, False, False),
        ('0', '1', False, False, False),
        ('1', '0', True, True, False),
        ('0', '1', True, False, True)
    ])
    def test_import_attaches_to_disk_overwrite_autosuffix(self, overwrite, autosuffix,
                                                          file_exists, expect_content_changes, expect_new_filename):
        dest_path = '/disk'
        file_path = '/disk/test-file-1.txt'
        file_path_with_suffix = '/disk/test-file-1 (1).txt'
        attaches = [{'mid': '123435627147236458163', 'hid': '1.1', 'file_name': 'test-file-1.txt'}]
        params = {'uid': self.uid,
                  'path': dest_path,
                  'overwrite': overwrite,
                  'autosuffix': autosuffix}
        data = {'items': attaches}

        if file_exists:
            self.upload_file(self.uid, file_path)
            res = self.json_ok('info', {'uid': self.uid, 'path': self.uid + ':' + file_path, 'meta': 'md5'})
            existing_file_md5 = res['meta']['md5']

        response = self.json_ok('import_attaches_to_disk', opts=params, json=data)
        oid = response['oid']
        operation_status = self.json_ok('status', {'uid': self.uid, 'oid': oid})
        assert 'EXECUTING' == operation_status['status'], operation_status
        KladunMocker().mock_kladun_callbacks_for_upload_from_service(self.uid, operation_status['protocol'][0]['oid'])
        manager.get_operation(self.uid, oid).process()

        operation_status = self.json_ok('status', {'uid': self.uid, 'oid': oid})
        assert operation_status['status'] == 'DONE'

        self.json_ok('info', {'uid': self.uid, 'path': self.uid + ':' + file_path})
        if expect_new_filename:
            self.json_ok('info', {'uid': self.uid, 'path': self.uid + ':' + file_path_with_suffix})
        else:
            self.json_error('info', {'uid': self.uid, 'path': self.uid + ':' + file_path_with_suffix},
                            code=codes.RESOURCE_NOT_FOUND)
        if file_exists:
            response = self.json_ok('info', {'uid': self.uid, 'path': self.uid + ':' + file_path, 'meta': 'md5'})
            if expect_content_changes:
                assert response['meta']['md5'] != existing_file_md5, 'File contents not changed'
            else:
                assert response['meta']['md5'] == existing_file_md5, 'File contents changed'

    def test_import_attaches_to_disk_autosuffix_multiple_files(self):
        dest_path = '/disk'
        attaches = [
            {'mid': '123435627147236458161', 'hid': '1.1', 'file_name': 'test-file-1.txt'},
            {'mid': '123435627147236458162', 'hid': '1.1', 'file_name': 'test-file-2.txt'},
            {'mid': '123435627147236458163', 'hid': '1.1', 'file_name': 'test-file-2.txt'},
            {'mid': '123435627147236458164', 'hid': '1.1', 'file_name': 'test-file-3.txt'},
            {'mid': '123435627147236458165', 'hid': '1.1', 'file_name': 'test-file-3.txt'},
        ]
        expected_new_paths = [
            '/disk/test-file-1 (1).txt',
            '/disk/test-file-2 (1).txt',
            '/disk/test-file-2 (2).txt',
            '/disk/test-file-3.txt',
            '/disk/test-file-3 (1).txt',
        ]
        params = {'uid': self.uid,
                  'path': dest_path,
                  'overwrite': '0',
                  'autosuffix': '1'}
        data = {'items': attaches}

        self.upload_file(self.uid, '/disk/test-file-1.txt')
        self.upload_file(self.uid, '/disk/test-file-2.txt')

        response = self.json_ok('import_attaches_to_disk', opts=params, json=data)
        oid = response['oid']
        operation_status = self.json_ok('status', {'uid': self.uid, 'oid': oid})
        assert 'EXECUTING' == operation_status['status'], operation_status
        suboperations = operation_status['protocol']
        for subop in suboperations:
            KladunMocker().mock_kladun_callbacks_for_upload_from_service(self.uid, subop['oid'])
        manager.get_operation(self.uid, oid).process()
        operation_status = self.json_ok('status', {'uid': self.uid, 'oid': oid})
        assert 'DONE' == operation_status['status'], operation_status

        for path in expected_new_paths:
            self.json_ok('info', {'uid': self.uid, 'path': self.uid + ':' + path})

    def test_import_attaches_to_disk_without_overwrite_and_autosuffix_with_old_file_fails(self):
        dest_path = '/disk'
        file_path = '/disk/test-file-1.txt'
        attaches = [{'mid': '123435627147236458163', 'hid': '1.1', 'file_name': 'test-file-1.txt'}]
        params = {'uid': self.uid,
                  'path': dest_path,
                  'overwrite': '0',
                  'autosuffix': '0'}
        data = {'items': attaches}

        self.upload_file(self.uid, file_path, file_data={'etime': "111"})
        res = self.json_ok('info', {'uid': self.uid, 'path': self.uid + ':' + file_path, 'meta': 'md5'})
        existing_file_md5 = res['meta']['md5']

        response = self.json_ok('import_attaches_to_disk', opts=params, json=data)
        operation_status = self.json_ok('status', {'uid': self.uid, 'oid': response['oid']})
        assert 'FAILED' == operation_status['status']
        assert 'FileAlreadyExist' == operation_status['error']['message']

        response = self.json_ok('info', {'uid': self.uid, 'path': self.uid + ':' + file_path, 'meta': 'md5'})
        assert response['meta']['md5'] == existing_file_md5, 'File contents changed'

    def test_import_attaches_to_disk_stops_if_first_suboperation_fails(self):
        dest_path = '/disk'
        attaches = [
                       {'mid': '123435627147236458163', 'hid': '1.1', 'file_name': 'test-file-1.txt'},
                       {'mid': '123435627147236458164', 'hid': '1.1', 'file_name': 'test-file-2.txt'},
                   ]
        params = {'uid': self.uid,
                  'path': dest_path,
                  'overwrite': '0',
                  'autosuffix': '0'}
        data = {'items': attaches}
        response = self.json_ok('import_attaches_to_disk', opts=params, json=data)
        oid = response['oid']
        response = self.json_ok('status', opts={'uid': self.uid, 'oid': oid})
        suboperations = response['protocol']
        self.check_statuses(oid, ('EXECUTING', 'WAITING', 'WAITING'))

        KladunMocker().mock_kladun_faulty_callback_for_upload_from_service(self.uid, suboperations[0]['oid'])
        manager.get_operation(self.uid, oid).process()
        self.check_statuses(oid, ('FAILED', 'FAILED', 'WAITING'))

        KladunMocker().mock_kladun_callbacks_for_upload_from_service(self.uid, suboperations[1]['oid'])
        manager.get_operation(self.uid, oid).process()
        self.check_statuses(oid, ('FAILED', 'FAILED', 'WAITING'))

    def test_import_attaches_to_disk_does_not_start_if_creating_suboperation_fails(self):
        dest_path = '/disk'
        attaches = [
                       {'mid': '123435627147236458163', 'hid': '1.1', 'file_name': 'test-file-1.txt'},
                       {'mid': '123435627147236458164', 'hid': '1.1', 'file_name': 'test-file-2.txt'},
                   ]
        params = {'uid': self.uid,
                  'path': dest_path,
                  'overwrite': '0',
                  'autosuffix': '0'}
        data = {'items': attaches}

        original_create_operation = manager.create_operation

        def create_operation_wrapper(uid, type, subtype, odata, **kw):
            if odata.get('target') == '128280859:/disk/test-file-2.txt':
                raise Exception()
            return original_create_operation(uid, type, subtype, odata, **kw)

        original_process = ImportMailAttaches._process

        with mock.patch('mpfs.core.operations.manager.create_operation', side_effect=create_operation_wrapper), \
             mock.patch.object(ImportMailAttaches, '_process', autospec=True, side_effect=original_process) \
                as process_mock:
            response = self.json_ok('import_attaches_to_disk', opts=params, json=data)
        assert 1 == process_mock.call_count
        oid = response['oid']
        self.check_statuses(oid, ('FAILED', 'WAITING', 'FAILED'))


    def test_import_attaches_to_disk_stops_if_last_suboperation_fails(self):
        dest_path = '/disk'
        attaches = [
                       {'mid': '123435627147236458163', 'hid': '1.1', 'file_name': 'test-file-1.txt'},
                       {'mid': '123435627147236458164', 'hid': '1.1', 'file_name': 'test-file-2.txt'},
                   ]
        params = {'uid': self.uid,
                  'path': dest_path,
                  'overwrite': '0',
                  'autosuffix': '0'}
        data = {'items': attaches}
        response = self.json_ok('import_attaches_to_disk', opts=params, json=data)
        oid = response['oid']
        response = self.json_ok('status', opts={'uid': self.uid, 'oid': oid})
        suboperations = response['protocol']
        self.check_statuses(oid, ('EXECUTING', 'WAITING', 'WAITING'))

        KladunMocker().mock_kladun_callbacks_for_upload_from_service(self.uid, suboperations[0]['oid'])
        manager.get_operation(self.uid, oid).process()
        self.check_statuses(oid, ('EXECUTING', 'DONE', 'WAITING'))

        KladunMocker().mock_kladun_faulty_callback_for_upload_from_service(self.uid, suboperations[1]['oid'])
        manager.get_operation(self.uid, oid).process()
        self.check_statuses(oid, ('FAILED', 'DONE', 'FAILED'))

    def test_import_attaches_to_disk_completes_when_all_suboperations_complete(self):
        dest_path = '/disk'
        attaches = [
                       {'mid': '123435627147236458163', 'hid': '1.1', 'file_name': 'test-file-1.txt'},
                       {'mid': '123435627147236458164', 'hid': '1.1', 'file_name': 'test-file-2.txt'},
                   ]
        params = {'uid': self.uid,
                  'path': dest_path,
                  'overwrite': '0',
                  'autosuffix': '0'}
        data = {'items': attaches}
        response = self.json_ok('import_attaches_to_disk', opts=params, json=data)

        oid = response['oid']

        self.check_statuses(oid, ('EXECUTING', 'WAITING', 'WAITING'))

        operation_status = self.json_ok('status', {'uid': self.uid, 'oid': oid})
        suboperations = operation_status['protocol']

        KladunMocker().mock_kladun_callbacks_for_upload_from_service(self.uid, suboperations[0]['oid'])
        manager.get_operation(self.uid, oid).process()
        self.check_statuses(oid, ('EXECUTING', 'DONE', 'WAITING'))

        KladunMocker().mock_kladun_callbacks_for_upload_from_service(self.uid, suboperations[1]['oid'])
        manager.get_operation(self.uid, oid).process()
        self.check_statuses(oid, ('DONE', 'DONE', 'DONE'))

    def test_import_attaches_to_disk_with_overwrite_and_autosuffix_fails(self):
        dest_path = '/disk'
        attaches = [{'mid': '123435627147236458163', 'hid': '1.1', 'file_name': 'test-file-1.txt'}]
        params = {'uid': self.uid,
                  'path': dest_path,
                  'overwrite': '1',
                  'autosuffix': '1'}
        data = {'items': attaches}

        self.json_error('import_attaches_to_disk', opts=params, json=data, code=codes.CODE_ERROR, title='BadArguments')

    def test_import_attaches_to_disk_fails_with_file_duplicates_without_overwrite_and_autosuffix(self):
        dest_path = '/disk'
        attaches = [
            {'mid': '123435627147236458163', 'hid': '1.1', 'file_name': 'test-file-1.txt'},
            {'mid': '123435627147236458164', 'hid': '1.1', 'file_name': 'test-file-1.txt'}
        ]
        params = {'uid': self.uid,
                  'path': dest_path,
                  'overwrite': '0',
                  'autosuffix': '0'}
        data = {'items': attaches}

        self.json_error('import_attaches_to_disk', opts=params, json=data, code=codes.BAD_REQUEST_ERROR,
                        title='BadRequestError')

    def test_import_attaches_fails_if_no_files_in_request(self):
        dest_path = '/disk'
        attaches = []
        params = {'uid': self.uid,
                  'path': dest_path,
                  'overwrite': '0',
                  'autosuffix': '0'}
        data = {'items': attaches}
        self.json_error('import_attaches_to_disk', opts=params, json=data, code=codes.BAD_REQUEST_ERROR,
                        title='BadRequestError')

    def test_import_attaches_to_disk_fails_if_target_dir_does_not_exist(self):
        dest_path = '/disk/notexists'
        attaches = [{'mid': '123435627147236458163', 'hid': '1.1', 'file_name': 'test-file-1.txt'}]
        params = {'uid': self.uid,
                  'path': dest_path,
                  'overwrite': '0',
                  'autosuffix': '0'}
        data = {'items': attaches}
        self.json_error('import_attaches_to_disk', opts=params, json=data,
                        code=71, title='resource not found')

    def test_import_attaches_to_disk_params_defaults(self):
        """
        Проверяем, что overwrite и autosuffix по умолчанию False
        """
        dest_path = '/disk'
        attaches = [{'mid': '123435627147236458163', 'hid': '1.1', 'file_name': 'test-file-1.txt'}]
        params = {'uid': self.uid,
                  'path': dest_path}
        data = {'items': attaches}
        response = self.json_ok('import_attaches_to_disk', opts=params, json=data)
        operation_data = manager.get_operation(self.uid, response['oid']).data
        assert False == operation_data['force']
        assert False == operation_data['autosuffix']
