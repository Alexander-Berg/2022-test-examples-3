# -*- coding: utf-8 -*-

import datetime
import pytest

from test.base import DiskTestCase
from test.helpers.operation import PendingOperationDisabler
from test.conftest import INIT_USER_IN_POSTGRES
from nose_parameterized import parameterized
from mpfs.core.operations.logic import frozen
from mpfs.core.operations import manager
from mpfs.core.operations.dao.operation import OperationDAO
from mpfs.core.operations.filesystem.move import MoveOnDisk
from mpfs.core.office.operations import OfficeLockingOperationStub


class OperationLogicFrozenTestCase(DiskTestCase):

    def setup_method(self, method):
        super(OperationLogicFrozenTestCase, self).setup_method(method)
        path1 = "/disk/1.doc"
        path2 = "/disk/2.doc"
        self.upload_file(self.uid, path1)
        self.move_op_data = {
            "source": self.uid + ':' + path1,
            "target": self.uid + ':' + path2,
            "force": 0,
        }
        self.office_op_data = {
            "path": path1
        }

    @parameterized.expand([(1200, 1),
                           (600, 0)])
    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='pg is the best')
    def test_get_frozen_important_operations_count(self, timeshift, operations_count):
        with PendingOperationDisabler(MoveOnDisk):
            operation = manager.create_operation(self.uid, 'move', 'disk_disk', odata=self.move_op_data)
            new_dtime = datetime.datetime.utcnow() - datetime.timedelta(seconds=timeshift)
            OperationDAO().set_dtime(self.uid, operation.id, new_dtime)
            assert frozen.get_frozen_important_operations_count() == operations_count

    @parameterized.expand([(90000, 1),
                           (600, 0)])
    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='pg is the best')
    def test_get_frozen_common_operations_count(self, timeshift, operations_count):
        with PendingOperationDisabler(OfficeLockingOperationStub):
            operation = manager.create_operation(self.uid, 'office', 'locking_operation_stub', odata=self.office_op_data)
            new_dtime = datetime.datetime.utcnow() - datetime.timedelta(seconds=timeshift)
            OperationDAO().set_dtime(self.uid, operation.id, new_dtime)
            assert frozen.get_frozen_common_operations_count() == operations_count

    @parameterized.expand([(1200, 1),
                           (600, 0)])
    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='pg is the best')
    def test_fetch_frozen_important_operations(self, timeshift, operations_count):
        with PendingOperationDisabler(MoveOnDisk):
            operation = manager.create_operation(self.uid, 'move', 'disk_disk', odata=self.move_op_data)
            new_dtime = datetime.datetime.utcnow() - datetime.timedelta(seconds=timeshift)
            OperationDAO().set_dtime(self.uid, operation.id, new_dtime)
            fetched_operations = list(frozen.fetch_frozen_important_operations())
            assert len(fetched_operations) == operations_count

    @parameterized.expand([(90000, 1),
                           (600, 0)])
    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='pg is the best')
    def test_fetch_frozen_common_operations(self, timeshift, operations_count):
        with PendingOperationDisabler(OfficeLockingOperationStub):
            operation = manager.create_operation(self.uid, 'office', 'locking_operation_stub', odata=self.office_op_data)
            new_dtime = datetime.datetime.utcnow() - datetime.timedelta(seconds=timeshift)
            OperationDAO().set_dtime(self.uid, operation.id, new_dtime)
            fetched_operations = list(frozen.fetch_frozen_common_operations())
            assert len(fetched_operations) == operations_count
