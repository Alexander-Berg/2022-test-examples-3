# -*- coding: utf-8 -*-
from hamcrest.core import assert_that, calling, raises
from nose_parameterized import parameterized

from mpfs.core.operations.manager import generate_operation_id
from test.base import DiskTestCase

import random
import mock
import time

import mpfs.core.operations
import mpfs.core.services.kladun_service
from mpfs.common.static.codes import FAILED

from mpfs.engine.queue2.celery import SilentTaskRetryException
from mpfs.core.job_handlers.operation import handle_operation
from mpfs.common import errors
from mpfs.common.static import codes
from mpfs.common.util import trace_calls
from mpfs.core import factory
from mpfs.core.address import ResourceId
from mpfs.core.filesystem.base import Filesystem
from mpfs.core.operations import manager
from mpfs.core.operations.base import set_source_resource_id, Operation
from mpfs.core.operations.dao.operation import OperationDAO
from mpfs.core.operations.filesystem.copy import CopyOnDisk
from mpfs.core.operations.filesystem.move import MoveOnDisk
from mpfs.core.operations.filesystem.remove import RemoveDisk
from mpfs.core.operations.filesystem.trash import TrashAppend
from mpfs.core.operations.util import OperationPinger
from test.base_suit import UserOperationsTestCaseMixin
from test.conftest import capture_queue_errors
from test.helpers.stubs.services import PushServicesStub
from test.helpers.utils import catch_return_values


class OperationsTestCase(DiskTestCase):

    op_type = 'store'
    op_subtype = 'disk'
    op_data = {
        "path": "128280859:/disk/foo.jpg",
        "md5": "698d51a19d8a121ce581499d7b701668",
        "size": 111
    }
    another_op_data = {
        "path": "128280859:/disk/foo.jpg",
        "md5": "bcbe3365e6ac95ea2c0343a2395834dd",
        "size": 222
    }

    def test_create_and_get(self):
        operation = manager.create_operation(self.uid, self.op_type, self.op_subtype, odata=self.op_data)
        self.assertNotEqual(operation, None)

        oid = operation.id
        operation = manager.get_operation(self.uid, oid)
        self.assertNotEqual(operation, None)

        self.assertEqual(operation.data, self.op_data)
        self.assertEqual(operation.type, self.op_type)

    def test_save(self):
        operation = manager.create_operation(self.uid, self.op_type, self.op_subtype, odata=self.op_data)
        oid = operation.id

        operation = manager.get_operation(self.uid, oid)
        operation.data['field'] = 'xxx'
        operation.save()

        del operation
        operation = manager.get_operation(self.uid, oid)
        self.assertEqual(operation.data['field'], 'xxx')

    def test_search_exists(self):
        manager.create_operation(self.uid, self.op_type, self.op_subtype, odata=self.op_data)

        search_results = manager.search_operation(
            self.uid,
            {
                'md5': self.op_data['md5'],
                'state': [1],
            },
        )

        self.assertEqual(len(search_results), 1)

    def test_get_open_store_operation(self):
        manager.create_operation(self.uid, self.op_type, self.op_subtype, odata=self.op_data)
        manager.create_operation(self.uid, self.op_type, self.op_subtype, odata=self.another_op_data)

        operation = manager.get_opened_store_operation(self.uid, self.op_data['path'], self.op_data['md5'], None)
        self.assertEqual(operation.state, codes.EXECUTING)
        self.assertEqual(operation.data['md5'], self.op_data['md5'])
        self.assertEqual(operation.data['path'], self.op_data['path'])

    @parameterized.expand([('photounlim',), ('photostream',)])
    def test_get_open_unlim_store_operation(self, store):
        op_data = {
            "path": "128280859:/%s/foo.jpg" % store,
            "md5": "698d51a19d8a121ce581499d7b701668",
            "size": 111
        }
        manager.create_operation(self.uid, self.op_type, self.op_subtype, odata=op_data)
        manager.create_operation(self.uid, self.op_type, self.op_subtype, odata=self.another_op_data)

        operation = manager.get_opened_store_operation(self.uid, "128280859:/photostream/foo.jpg", op_data['md5'], None)
        self.assertEqual(operation.state, codes.EXECUTING)
        self.assertEqual(operation.data['md5'], op_data['md5'])
        self.assertEqual(operation.data['path'], op_data['path'])

    def test_search_non_exits(self):
        manager.create_operation(self.uid, self.op_type, self.op_subtype, odata=self.op_data)

        search_results = manager.search_operation(
            self.uid,
            {'data.type': 'nonexist'},
        )
        self.assertEqual(len(search_results), 0)

    def test_remove(self):
        operation = manager.create_operation(self.uid, self.op_type, self.op_subtype, odata=self.op_data)
        oid = operation.id
        manager.delete_operation(self.uid, oid)

        del operation
        self.assertRaises(errors.OperationNotFound, lambda: manager.get_operation(self.uid, oid))

    def test_wrong_operation(self):
        self.assertRaises(errors.OperationNotFound, lambda: manager.get_operation(self.uid, '1111111'))

    def test_active_operations(self):
        manager.create_operation(self.uid, self.op_type, self.op_subtype, odata=self.op_data)
        manager.create_operation(self.uid, self.op_type, self.op_subtype, odata=self.another_op_data)

        operations = [x for x in manager.get_active_operations(self.uid)]
        self.assertEqual(len(operations), 2)

    def test_fail_operation_if_sending_to_queller_was_unsuccessful(self):
        file_path = '/disk/test.txt'
        self.upload_file(self.uid, file_path)
        with mock.patch('mpfs.core.queue.mpfs_queue.put', side_effect=Exception('fake exception')) as put_mock:
            assert_that(calling(self.json_ok).with_args('async_trash_append', {'uid': self.uid, 'path': file_path}), raises(Exception))
            oid = put_mock.call_args[0][0]['oid']
        assert manager.get_operation(self.uid, oid).state == FAILED

    def test_update_dtime(self):
        operation = manager.create_operation(self.uid, self.op_type, self.op_subtype, odata=self.op_data)
        operation.update_dtime()
        prev = operation.dtime
        time.sleep(0.001)
        operation.update_dtime()
        operation_item = OperationDAO().find_one({'_id': operation.id, 'uid': self.uid})
        assert prev < operation.dtime
        micros = operation.dtime.microsecond
        dtime = operation.dtime.replace(microsecond=micros - micros % 1000)
        db_micros = operation_item['dtime'].microsecond
        db_dtime = operation_item['dtime'].replace(microsecond=db_micros - db_micros % 1000)
        assert dtime == db_dtime

    def test_operation_pinger(self):
        operation = manager.create_operation(self.uid, self.op_type, self.op_subtype, odata=self.op_data)
        operation.update_dtime()
        prev = operation.dtime
        with OperationPinger(operation, delay=0.01, delay_first_action=False):
            time.sleep(0.1)
        assert prev < operation.dtime


class OperationsApiTestCase(DiskTestCase):

    def test_create_operation(self):
        """
        Проверяем выдачу данных при создании операции
        Должен быть oid и version на момент создания операции
        """
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})

        # получаем версию на начало
        start_version = self.json_ok('user_info', {'uid': self.uid})['version']

        # создаем операцию по перемещению
        operation_info = self.json_ok('async_move', {'uid': self.uid, 'src': '/disk/folder', 'dst': '/disk/moved'})
        assert 'oid' in operation_info
        assert 'at_version' in operation_info
        assert operation_info['at_version'] == start_version

    def test_operation_status(self):
        """
        Проверяем выдачу статуса
        Должен быть state/status и version на момент создания операции
        """
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})

        # получаем версию на начало
        start_version = self.json_ok('user_info', {'uid': self.uid})['version']

        # создаем операцию по перемещению
        operation_info = self.json_ok('async_move', {'uid': self.uid, 'src': '/disk/folder', 'dst': '/disk/moved'})
        oid = operation_info['oid']

        # читаем статус в json
        json_operation_status = self.json_ok('status', {'uid': self.uid, 'oid': oid})
        assert 'state' in json_operation_status
        assert 'status' in json_operation_status
        assert 'at_version' in json_operation_status
        assert json_operation_status['at_version'] == start_version

        # читаем статус в xml
        xml_operation_status = self.mail_ok('status', {'uid': self.uid, 'oid': oid})
        assert int(xml_operation_status.find('operation').find('at_version').text) == start_version

    def test_status_after_store_with_410_from_kladun(self):
        with trace_calls(Filesystem, 'store') as tracer:
            self.upload_file(self.uid, '/disk/test.txt')
            operation = tracer['return_value']
            oid = operation.id

        def fake_status(*args, **kwargs):
            raise errors.OperationNotFoundError()

        with mock.patch.object(mpfs.core.services.kladun_service.Kladun, 'status', fake_status):
            result = self.json_ok('status', {'uid': self.uid, 'oid': oid})

        assert result['status'] == 'DONE'

    def test_status_for_unknown_operation_type(self):
        class UnknownAlbumOperation(Operation):
            type = 'albums_merge'
            subtype = 'albums_merge'

        operation = UnknownAlbumOperation.Create(self.uid, {'id': generate_operation_id(self.uid), 'a': 1})

        resp = self.json_ok('status', {'uid': self.uid, 'oid': operation.id})
        assert resp['type'] == 'albums_merge'

    def test_status_operation_after_unknown_operation(self):
        class UnknownAlbumOperation(Operation):
            type = 'albums_merge'
            subtype = 'albums_merge'

        ctime = int(time.time()) - 10000
        mtime = int(time.time()) - 5000
        file_path = '/disk/custom_time_1.txt'

        operation = UnknownAlbumOperation.Create(self.uid, {'id': generate_operation_id(self.uid), 'a': 1})

        resp = self.json_ok('status', {'uid': self.uid, 'oid': operation.id})
        assert resp['type'] == 'albums_merge'

        with mock.patch('mpfs.frontend.api.disk.STORE_CTIME_FROM_CLIENT_ENABLED', True):
            self.upload_file(self.uid, file_path, opts={'ctime': ctime, 'mtime': mtime})

        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.async_ok('async_trash_append', opts={'uid': self.uid, 'path': file_path})


class CopyOnDiskTestCase(DiskTestCase):

    def test_data_has_file_resource_id(self):
        file_path = '/disk/test.txt'
        file_path2 = '/disk/test2.txt'
        self.upload_file(self.uid, file_path)

        with catch_return_values(CopyOnDisk, 'Create') as (mocked, return_values):
            self.json_ok('async_copy', {
                'uid': self.uid, 'src': file_path, 'dst': file_path2
            })
            assert mocked.call_count == 1
            operation_object = return_values[0]
            assert operation_object.data['source_resource_id']

    def test_data_has_folder_resource_id(self):
        folder_path = '/disk/Folder1'
        folder_path2 = '/disk/Folder2'
        self.json_ok('mkdir', {'uid': self.uid, 'path': folder_path})

        with catch_return_values(CopyOnDisk, 'Create') as (mocked, return_values):
            self.json_ok('async_copy', {
                'uid': self.uid, 'src': folder_path, 'dst': folder_path2
            })
            assert mocked.call_count == 1
            operation_object = return_values[0]
            assert operation_object.data['source_resource_id']

    def test_data_has_system_folder_resource_id(self):
        folder_path2 = '/disk/Folder2'

        with catch_return_values(CopyOnDisk, 'Create') as (mocked, return_values):
            self.json_ok('async_copy', {
                'uid': self.uid, 'src': '/disk', 'dst': folder_path2
            })
            assert mocked.call_count == 1
            operation_object = return_values[0]
            assert operation_object.data['source_resource_id']


class MoveOnDiskTestCase(DiskTestCase):

    def test_data_has_file_resource_id(self):
        file_path = '/disk/test.txt'
        file_path2 = '/disk/test2.txt'
        self.upload_file(self.uid, file_path)

        with catch_return_values(MoveOnDisk, 'Create') as (mocked, return_values):
            self.json_ok('async_move', {
                'uid': self.uid, 'src': file_path, 'dst': file_path2
            })
            assert mocked.call_count == 1
            operation_object = return_values[0]
            assert operation_object.data['source_resource_id']

    def test_data_has_folder_resource_id(self):
        folder_path = '/disk/Folder1'
        folder_path2 = '/disk/Folder2'
        self.json_ok('mkdir', {'uid': self.uid, 'path': folder_path})

        with catch_return_values(MoveOnDisk, 'Create') as (mocked, return_values):
            self.json_ok('async_move', {
                'uid': self.uid, 'src': folder_path, 'dst': folder_path2
            })
            assert mocked.call_count == 1
            operation_object = return_values[0]
            assert operation_object.data['source_resource_id']


class TrashAppendTestCase(DiskTestCase):

    def test_data_has_file_resource_id(self):
        file_path = '/disk/test.txt'
        self.upload_file(self.uid, file_path)

        with catch_return_values(TrashAppend, 'Create') as (mocked, return_values):
            self.json_ok('async_trash_append', {
                'uid': self.uid, 'path': file_path
            })
            assert mocked.call_count == 1
            operation_object = return_values[0]
            assert operation_object.data['source_resource_id']

    def test_data_has_folder_resource_id(self):
        folder_path = '/disk/Folder1'
        self.json_ok('mkdir', {'uid': self.uid, 'path': folder_path})

        with catch_return_values(TrashAppend, 'Create') as (mocked, return_values):
            self.json_ok('async_trash_append', {
                'uid': self.uid, 'path': folder_path
            })
            assert mocked.call_count == 1
            operation_object = return_values[0]
            assert operation_object.data['source_resource_id']


class SetSourceResourceIdTestCase(DiskTestCase):
    """Класс с тестами для `set_source_resource_id`.
    """
    def test_file_has_file_id(self):
        odata = {}
        file_path = '/disk/test.txt'
        self.upload_file(self.uid, file_path)

        resource = factory.get_resource(self.uid, file_path)
        set_source_resource_id(resource, odata)

        assert odata['source_resource_id'] == resource.resource_id.serialize()

    def test_file_has_no_file_id_no_resave(self):
        file_path = '/disk/test.txt'
        self.upload_file(self.uid, file_path)

        resource = factory.get_resource(self.uid, file_path)
        resource.meta.pop('file_id')
        odata = {}
        set_source_resource_id(resource, odata)

        assert odata['source_resource_id'] == ''

    def test_file_has_no_file_id_resave(self):
        file_path = '/disk/test.txt'
        self.upload_file(self.uid, file_path)

        resource = factory.get_resource(self.uid, file_path)
        resource.meta.pop('file_id')
        odata = {}
        with mock.patch.object(mpfs.core.operations.base, 'RESAVE_RESOURCE_WITHOUT_FILE_ID', new=True):
            set_source_resource_id(resource, odata)

        resource = factory.get_resource(self.uid, file_path)
        assert odata['source_resource_id'] == resource.resource_id.serialize()
        assert odata['file_id'] == resource.meta['file_id']

    def test_system_folder_has_no_file_id(self):
        resource = factory.get_resource(self.uid, '/disk')
        assert 'file_id' not in resource.meta

        odata = {}
        set_source_resource_id(resource, odata)

        file_id = resource.generate_file_id(resource.uid, resource.path)
        new_resource_id = ResourceId(resource.storage_address.uid, file_id).serialize()
        assert odata['source_resource_id'] == new_resource_id


class RemoveTestCase(DiskTestCase):

    def test_data_has_file_resource_id(self):
        file_path = '/disk/test.txt'
        self.upload_file(self.uid, file_path)

        with catch_return_values(RemoveDisk, 'Create') as (mocked, return_values):
            self.json_ok('async_rm', {
                'uid': self.uid, 'path': file_path
            })
            assert mocked.call_count == 1
            operation_object = return_values[0]
            assert operation_object.data['source_resource_id']


class OperationNotificationTestCase(DiskTestCase):
    """Класс тестов нотификаций, связанных с операциями."""

    def test_created_success(self):
        """Протестировать отправку пуш-нотификации дл ясобытия создания операции."""

        path1 = "/disk/foo.jpg"
        path2 = "/disk/foo2.jpg"
        self.upload_file(self.uid, path1)
        data = {
            "source": self.uid + ':' + path1,
            "target": self.uid + ':' + path2,
        }

        with PushServicesStub() as patches:
            manager.create_operation(self.uid, 'move', 'disk_disk', odata=data)
            assert patches.send.called
            assert patches.send.call_count == 1

            parsed_push = PushServicesStub.parse_send_call(patches.send.call_args)
            assert parsed_push['event_name'] == 'operations'
            assert 'connection_id' in parsed_push
            assert parsed_push['uid'] == self.uid

            json_payload = parsed_push['json_payload']
            assert json_payload['root']['tag'] == 'operation'
            assert json_payload['root']['parameters']['type'] == 'created'
            assert json_payload['root']['parameters']['oid']


class OperationLimitationTestCase(DiskTestCase, UserOperationsTestCaseMixin):
    OPERATION_COUNT = 5

    def setup_method(self, method):
        super(OperationLimitationTestCase, self).setup_method(method)
        self.upload_file(self.uid, '/disk/file.txt')
        self.errors = []

    def run(self, result=None):
        with mock.patch('mpfs.core.job_handlers.operation.QUEUE2_EXECUTING_OPERATIONS_LIMITATION_ENABLED', True), \
                mock.patch('mpfs.core.job_handlers.operation.QUEUE2_EXECUTING_OPERATIONS_LIMITATION_DRY_RUN', False), \
                mock.patch('mpfs.core.job_handlers.operation.QUEUE2_EXECUTING_OPERATIONS_LIMITATION_MAX_EXECUTING_OPERATIONS',
                           self.OPERATION_COUNT), \
                capture_queue_errors() as self.errors:
                    return super(OperationLimitationTestCase, self).run(result)

    def test_limit_ok(self):
        self.create_executing_move_operation()
        self.json_ok('async_move', {'uid': self.uid, 'src': '/disk/file.txt', 'dst': '/disk/file2.txt'})
        assert not any([isinstance(err.error, SilentTaskRetryException) for err in self.errors])

    def test_move_operation_delay(self):
        for i in xrange(self.OPERATION_COUNT + 1):
            self.create_executing_move_operation()

        self.json_ok('async_move', {'uid': self.uid, 'src': '/disk/file.txt', 'dst': '/disk/file2.txt'})
        assert any([isinstance(err.error, SilentTaskRetryException) for err in self.errors])

    def test_move_operation_ok_with_lots_of_active_store(self):
        for i in xrange(self.OPERATION_COUNT + 1):
            self.create_executing_store_operation()

        self.json_ok('async_move', {'uid': self.uid, 'src': '/disk/file.txt', 'dst': '/disk/file2.txt'})
        assert not any([isinstance(err.error, SilentTaskRetryException) for err in self.errors])

    def test_allow_execution_after_executing_operation_failed(self):
        """
        Тестируем такой сценарий - запустили операции, они поставились в статус Executing, а потом все упали (ну,
        например, случилась выкладка и они все посрубались), так вот надо, чтобы после рестарта таска они могли
        продолжиться, а не обламывались об то, что лимит на количество бегущих операций превышен
        """
        operation_ids = []
        for i in xrange(self.OPERATION_COUNT + 1):
            operation_ids.append(self.create_executing_move_operation())

        any_oid = random.choice(operation_ids)
        try:
            handle_operation(self.uid, any_oid)
        except SilentTaskRetryException:
            assert False  # не должно падать, если началась операция, которая уже в статусе Executing

    def test_limitation_on_create_operation_for_uid(self):
        for i in xrange(self.OPERATION_COUNT + 1):
            self.create_executing_move_operation()

        QUEUE2_CREATING_OPERATIONS_LIMITATION_LIMITED_UIDS = \
            'mpfs.core.operations.manager.QUEUE2_CREATING_OPERATIONS_LIMITATION_LIMITED_UIDS'
        QUEUE2_CREATING_OPERATIONS_LIMITATION_ENABLED = \
            'mpfs.core.operations.manager.QUEUE2_CREATING_OPERATIONS_LIMITATION_ENABLED'
        QUEUE2_CREATING_OPERATIONS_LIMITATION_MAX_ACTIVE_OPERATIONS = \
            'mpfs.core.operations.manager.QUEUE2_CREATING_OPERATIONS_LIMITATION_MAX_ACTIVE_OPERATIONS'

        with mock.patch(QUEUE2_CREATING_OPERATIONS_LIMITATION_ENABLED, True), \
                mock.patch(QUEUE2_CREATING_OPERATIONS_LIMITATION_MAX_ACTIVE_OPERATIONS, self.OPERATION_COUNT), \
                mock.patch(QUEUE2_CREATING_OPERATIONS_LIMITATION_LIMITED_UIDS, [self.uid]):
            self.json_error(
                'async_move',
                {'uid': self.uid, 'src': '/disk/file.txt', 'dst': '/disk/file2.txt'},
                code=codes.TOO_MANY_EXECUTING_OPERATIONS
            )
            assert len(self.json_ok('active_operations', {'uid': self.uid})) == self.OPERATION_COUNT + 1


class GetActiveOperationsTestCase(DiskTestCase, UserOperationsTestCaseMixin):
    def setup_method(self, method):
        super(GetActiveOperationsTestCase, self).setup_method(method)
        self.create_executing_store_operation()
        self.create_executing_support_operation()

    def test_get_active_operations(self):
        response = self.json_ok('active_operations', {'uid': self.uid})
        self._check_response(response, [('store', 'disk')])

    def test_get_active_not_hidden_operations(self):
        response = self.json_ok('active_operations', {'uid': self.uid, 'show_hidden': '0'})
        self._check_response(response, [('store', 'disk')])

    def test_get_active_hidden_operations(self):
        response = self.json_ok('active_operations', {'uid': self.uid, 'show_hidden': '1'})
        self._check_response(response, [('store', 'disk'), ('support', 'get_file_checksums')])

    def _check_response(self, response, expected_operations):
        assert len(response) == len(expected_operations)

        current_operations = set((x['type'], x['subtype']) for x in response)
        assert set(current_operations) == set(expected_operations)
