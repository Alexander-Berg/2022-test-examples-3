# -*- coding: utf-8 -*-
import json
import random
import time
from copy import deepcopy

import mock

from mpfs.common.util.experiments.logic import enable_experiment_for_uid
from test.parallelly.api.disk.base import DiskApiTestCase
from test.base_suit import UserTestCaseMixin, SupportApiTestCaseMixin
from test.fixtures.operations import ActiveOperationsFixture, OperationStatusFixture
from test.helpers.stubs.resources.users_info import DEFAULT_USERS_INFO
from test.helpers.stubs.services import PassportStub
from mpfs.common.static import tags
from mpfs.core.operations.filesystem.store import StoreDisk
from mpfs.core.services.passport_service import passport
from mpfs.platform.v1.disk.serializers import (OperationSerializer, OperationStatusSerializer,
                                               MoveOnDiskOperationDataSerializer, DeleteOperationDataSerializer,
                                               CopyOnDiskOperationDataSerializer, UploadOperationDataSerializer)


def get_ctime_unordered_operations():
    operations = deepcopy(ActiveOperationsFixture.COPY + ActiveOperationsFixture.MOVE)

    NOW = int(time.time())
    for i, operation in enumerate(operations):
        operation['ctime'] = NOW + random.randint(0, 1024)

    return operations


class OperationsTestCase(UserTestCaseMixin, SupportApiTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'

    def setup_method(self, method):
        super(OperationsTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)

    @mock.patch('mpfs.core.operations.manager.get_active_operations',
                return_value=ActiveOperationsFixture.MOVE)
    def test_list_status_presented(self, _):
        resp = self.client.get('disk/operations', uid=self.uid)
        assert resp.status_code == 200

        active_operations = json.loads(resp.result)['items']
        assert len(active_operations) == len(ActiveOperationsFixture.MOVE)

        move_operation = active_operations[0]
        assert 'status' in move_operation
        assert move_operation['status'] == tags.IN_PROGRESS

    @mock.patch('mpfs.core.operations.manager.get_active_operations',
                return_value=ActiveOperationsFixture.COPY)
    def test_list_copy_operations_success(self, _):
        resp = self.client.get('disk/operations', uid=self.uid)
        assert resp.status_code == 200

        active_operations = json.loads(resp.result)['items']
        assert len(active_operations) == len(ActiveOperationsFixture.COPY)

        copy_operation = active_operations[0]
        assert not copy_operation.viewkeys() ^ set(OperationSerializer.visible_fields)
        assert 'data' in copy_operation
        assert not copy_operation['data'].viewkeys() ^ set(CopyOnDiskOperationDataSerializer.visible_fields)

        assert copy_operation['data']['from_resource_id']

    @mock.patch('mpfs.core.operations.manager.get_active_operations', return_value=[])
    def test_list_no_active_operations_success(self, _):
        resp = self.client.get('disk/operations', uid=self.uid)
        assert resp.status_code == 200

        assert json.loads(resp.result)['items'] == []

    @mock.patch('mpfs.core.operations.manager.get_active_operations',
                return_value=ActiveOperationsFixture.SOCIAL)
    def test_list_no_data_serializer_success(self, _):
        resp = self.client.get('disk/operations', uid=self.uid)
        assert resp.status_code == 200

        social_operation = json.loads(resp.result)['items'][0]
        assert 'data' in social_operation
        assert social_operation['data'] == {}

    @mock.patch('mpfs.core.base.status',
                return_value=OperationStatusFixture.OPERATION_STATUS)
    def test_get_operation_status_success(self, _):
        resp = self.client.get('disk/operations', uid=self.uid, query={'id': ActiveOperationsFixture.OPERATION_ID})
        assert resp.status_code == 200

        assert not json.loads(resp.result).viewkeys() ^ set(OperationStatusSerializer.visible_fields)

    def test_get_operation_status_not_found(self):
        resp = self.client.get('disk/operations', uid=self.uid, query={'id': ActiveOperationsFixture.NON_EXISTENT_OPERATION_ID})
        assert resp.status_code == 404

        non_existent_operation = json.loads(resp.result)
        assert non_existent_operation['error'] == 'DiskOperationNotFoundError'

    @mock.patch('mpfs.core.operations.manager.get_active_operations',
                return_value=ActiveOperationsFixture.TRASH_APPEND)
    def test_list_trash_append_success(self, _):
        resp = self.client.get('disk/operations', uid=self.uid)
        assert resp.status_code == 200

        active_operations = json.loads(resp.result)['items']
        assert len(active_operations) == len(ActiveOperationsFixture.TRASH_APPEND)

        delete_operation = active_operations[0]
        assert not delete_operation.viewkeys() ^ set(OperationSerializer.visible_fields)
        assert 'data' in delete_operation
        assert not delete_operation['data'].viewkeys() ^ set(DeleteOperationDataSerializer.visible_fields)

        assert delete_operation['data']['permanently'] is False
        assert delete_operation['data']['resource_id']

    @mock.patch('mpfs.core.operations.manager.get_active_operations',
                return_value=ActiveOperationsFixture.REMOVE)
    def test_list_remove_success(self, _):
        resp = self.client.get('disk/operations', uid=self.uid)
        assert resp.status_code == 200

        active_operations = json.loads(resp.result)['items']
        assert len(active_operations) == len(ActiveOperationsFixture.REMOVE)

        delete_operation = active_operations[0]
        assert not delete_operation.viewkeys() ^ set(OperationSerializer.visible_fields)
        assert 'data' in delete_operation
        assert not delete_operation['data'].viewkeys() ^ set(DeleteOperationDataSerializer.visible_fields)

        assert delete_operation['data']['permanently'] is True
        assert delete_operation['data']['resource_id']

    @mock.patch('mpfs.core.operations.manager.get_active_operations',
                return_value=ActiveOperationsFixture.MOVE)
    def test_list_move_success(self, _):
        resp = self.client.get('disk/operations', uid=self.uid)
        assert resp.status_code == 200

        active_operations = json.loads(resp.result)['items']
        assert len(active_operations) == len(ActiveOperationsFixture.MOVE)

        move_operation = active_operations[0]
        assert not move_operation.viewkeys() ^ set(OperationSerializer.visible_fields)
        assert 'data' in move_operation
        assert not move_operation['data'].viewkeys() ^ set(MoveOnDiskOperationDataSerializer.visible_fields)

        assert move_operation['data']['from_resource_id']

    @mock.patch('mpfs.core.operations.manager.get_active_operations',
                return_value=ActiveOperationsFixture.MOVE)
    def test_list_move_default_serializer_success(self, _):
        """Протестировать случай, когда для указанного типа и подтипа нет нужного сериализатора.

        Должен выполниться сериализатор по-умолчанию.
        """
        move_unsupported_subtype = deepcopy(ActiveOperationsFixture.MOVE)
        move_unsupported_subtype[0]['subtype'] = 'foo'
        with mock.patch('mpfs.core.operations.manager.get_active_operations',
                        return_value=move_unsupported_subtype):
            resp = self.client.get('disk/operations', uid=self.uid)
            assert resp.status_code == 200

            active_operations = json.loads(resp.result)['items']
            assert len(active_operations) == len(ActiveOperationsFixture.MOVE)
            assert active_operations[0]['data'] == {}

    @mock.patch('mpfs.core.operations.manager.get_active_operations',
                return_value=ActiveOperationsFixture.STORE)
    def test_list_upload_success(self, _):
        resp = self.client.get('disk/operations', uid=self.uid)
        assert resp.status_code == 200

        active_operations = json.loads(resp.result)['items']
        assert len(active_operations) == len(ActiveOperationsFixture.STORE)

        upload_operation = active_operations[0]
        assert not upload_operation.viewkeys() ^ set(OperationSerializer.visible_fields)
        assert 'data' in upload_operation
        assert not upload_operation['data'].viewkeys() ^ set(UploadOperationDataSerializer.visible_fields)

        assert upload_operation['data']['path']

    @mock.patch('mpfs.core.operations.manager.get_active_operations',
                return_value=ActiveOperationsFixture.MOVE + ActiveOperationsFixture.COPY)
    def test_paging_active_operations(self, _):
        total = len(ActiveOperationsFixture.MOVE + ActiveOperationsFixture.COPY)
        assert total >= 3

        resp = self.client.get('disk/operations', uid=self.uid, query={'offset': 0, 'limit': 1})
        assert resp.status_code == 200
        active_operations = json.loads(resp.result)['items']
        assert len(active_operations) == 1

        resp = self.client.get('disk/operations', uid=self.uid, query={'offset': 1, 'limit': 2})
        assert resp.status_code == 200
        active_operations = json.loads(resp.result)['items']
        assert len(active_operations) == 2

    def test_ctime_ordering(self):
        unordered_operations = get_ctime_unordered_operations()
        total = len(unordered_operations)

        with mock.patch('mpfs.core.operations.manager.get_active_operations',
                        return_value=unordered_operations):
            resp = self.client.get('disk/operations', uid=self.uid, query={'offset': 0, 'limit': total})
            assert resp.status_code == 200
            active_operations = json.loads(resp.result)['items']

            unordered_operations.sort(key=lambda x: x['ctime'])
            assert [op['operation_id'] for op in active_operations] == [op['id'] for op in unordered_operations]

    def test_status_not_use_kladun(self):
        operation = StoreDisk()
        with mock.patch('mpfs.core.operations.manager.get_operation', return_value=operation), \
             mock.patch('mpfs.core.services.kladun_service.UploadToDisk.status') as mock_object, \
             enable_experiment_for_uid('remove_stages', self.uid):
            self.client.get('disk/operations/operation_id=random_string', uid=self.uid)
            assert not mock_object.called

    def test_request_for_noninit_user_with_passport_bad_response(self):
        """Протестировать, что при запросе информации пользователем с аккаунтом без пароля он получит 403 ошибку."""
        uid = '123456789'
        user_info = deepcopy(DEFAULT_USERS_INFO[self.uid])
        user_info['uid'] = uid
        with PassportStub(userinfo=user_info) as stub:
            stub.subscribe.side_effect = passport.errors_map['accountwithpasswordrequired']
            response = self.client.get('disk/operations', uid=uid)
            assert response.status_code == 403
            content = json.loads(response.content)
            assert content['error'] == 'DiskUnsupportedUserAccountTypeError'
            assert content['description'] == 'User account type is not supported.'

    def test_request_for_blocked_user(self):
        opts = {
            'uid': self.uid,
            'moderator': 'moderator',
            'comment': 'comment',
        }
        self.support_ok('block_user', opts)
        response = self.client.get('disk/operations', uid=self.uid)
        content = json.loads(response.content)
        assert response.status_code == 403
        assert content['error'] == 'DiskUserBlockedError'
        assert content['description'] == 'User is blocked.'

    def test_request_for_noninit_user(self):
        uid = '123456789'
        user_info = deepcopy(DEFAULT_USERS_INFO[self.uid])
        user_info['uid'] = uid
        with PassportStub(userinfo=user_info):
            response = self.client.get('disk/operations', uid=uid)
            # init and retry with _auto_initialize_user
            assert response.status_code == 200
