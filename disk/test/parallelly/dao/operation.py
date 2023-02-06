# -*- coding: utf-8 -*-
import datetime
import pytest

from test.base import DiskTestCase
from test.helpers.operation import PendingOperationDisabler
from test.conftest import INIT_USER_IN_POSTGRES
from mpfs.core.operations import manager
from mpfs.core.operations.dao.operation import OperationDAO
from mpfs.core.operations.filesystem.move import MoveOnDisk
from mpfs.common.static.codes import WAITING, EXECUTING
from nose_parameterized import parameterized


class OperationDAOTestCase(DiskTestCase):
    def test_fetch_by_date_period_and_state(self):
        self.json_ok('store', {'uid': self.uid, 'path': '/disk/1.txt'})
        cur_dt = datetime.datetime.utcnow()
        delta_5_min = datetime.timedelta(minutes=5)
        before_5_min = cur_dt - delta_5_min
        after_5_min = cur_dt + delta_5_min
        oper_data_items = list(OperationDAO().fetch_by_date_period_and_states(before_5_min, after_5_min, range(6)))
        assert len(oper_data_items) == 1
        assert oper_data_items[0].type == 'store'
        oper_data_items = list(OperationDAO().fetch_by_date_period_and_states(before_5_min, after_5_min, [6]))
        assert len(oper_data_items) == 0

    @parameterized.expand([(1200, 1),
                           (600, 0)])
    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='pg is the best')
    def test_fetch_by_age_types_subtypes_and_states(self, timeshift, operations_count):
        path1 = "/disk/1.jpg"
        path2 = "/disk/2.jpg"
        self.upload_file(self.uid, path1)
        data = {
            "source": self.uid + ':' + path1,
            "target": self.uid + ':' + path2,
            "force": 0,
        }
        with PendingOperationDisabler(MoveOnDisk):
            operation = manager.create_operation(self.uid, 'move', 'disk_disk', odata=data)
            operation.update_dtime()
            delta = datetime.timedelta(seconds=timeshift)
            new_dtime = datetime.datetime.utcnow() - delta
            OperationDAO().set_dtime(self.uid, operation.id, new_dtime)
            oper_items = list(OperationDAO().fetch_by_age_types_subtypes_and_states(900, (('move', 'disk_disk'),),
                                                                                    (WAITING, EXECUTING)))
            assert len(oper_items) == operations_count

    @parameterized.expand([(1200, 1),
                           (600, 0)])
    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='pg is the best')
    def test_get_count_by_age_types_subtypes_and_states(self, timeshift, operations_count):
        path1 = "/disk/1.jpg"
        path2 = "/disk/2.jpg"
        self.upload_file(self.uid, path1)
        data = {
            "source": self.uid + ':' + path1,
            "target": self.uid + ':' + path2,
            "force": 0,
        }
        with PendingOperationDisabler(MoveOnDisk):
            operation = manager.create_operation(self.uid, 'move', 'disk_disk', odata=data)
            operation.update_dtime()
            delta = datetime.timedelta(seconds=timeshift)
            new_dtime = datetime.datetime.utcnow() - delta
            OperationDAO().set_dtime(self.uid, operation.id, new_dtime)
            items_count = OperationDAO().get_count_by_age_types_subtypes_and_states(900, (('move', 'disk_disk'),),
                                                                                    (WAITING, EXECUTING))
            assert items_count == operations_count
