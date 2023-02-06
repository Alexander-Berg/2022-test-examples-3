# -*- coding: utf-8 -*-
import contextlib
import datetime
import mock

from mpfs.core.overdraft.logic.overdraft_cleaner import BazingaOverdraftCleanerWorker
from mpfs.core.overdraft.tasks import create_overdraft_cleaner_tasks
from mpfs.core.user.base import User
from test.base import DiskTestCase
from mpfs.core.billing.dao.overdraft import OverdraftDAO
from test.helpers.size_units import GB
from test.helpers.stubs.services import BazingaInterfaceStub


class OverdraftCleanerManagerTestCase(DiskTestCase):
    def test_create_cleaning_tasks(self):
        uids = ['1111111', '2222222']
        for uid in uids:
            self.create_user(uid)
            user = User(uid)
            user.set_overdraft_info(datetime.date.today())

        with BazingaInterfaceStub() as stub:
            create_overdraft_cleaner_tasks()
            stub.bulk_create_tasks.assert_called_once()
            args, _ = stub.bulk_create_tasks.call_args
            tasks = args[0]
            assert len(tasks) == 2
            assert sorted(uids) == sorted(task.uid for task in tasks)


class OverdraftCleanerWorkerTestCase(DiskTestCase):
    @staticmethod
    def patch_settings():
        return contextlib.nested(
            mock.patch.object(BazingaOverdraftCleanerWorker, 'ENABLED', True),
            mock.patch.object(BazingaOverdraftCleanerWorker, 'DRY_RUN', False),
        )

    def test_user_not_in_overdraft(self):
        user = User(self.uid)
        user.set_overdraft_info(datetime.date.today())
        overdraft_before_cleaning = OverdraftDAO().get(self.uid)
        assert overdraft_before_cleaning

        with self.patch_settings():
            worker = BazingaOverdraftCleanerWorker(self.uid)
            worker.run()

        overdraft_after_cleaning = OverdraftDAO().get(self.uid)
        assert overdraft_after_cleaning is None

    def test_user_stay_in_overdraft(self):
        user = User(self.uid)
        user.set_overdraft_info(datetime.date.today())
        overdraft_before_cleaning = OverdraftDAO().get(self.uid)
        assert overdraft_before_cleaning

        with self.patch_settings(), \
            mock.patch('mpfs.core.services.disk_service.MPFSStorageService.used', return_value=12 * GB), \
            mock.patch('mpfs.core.services.disk_service.MPFSStorageService.limit', return_value=10 * GB):

            worker = BazingaOverdraftCleanerWorker(self.uid)
            worker.run()

        overdraft_after_cleaning = OverdraftDAO().get(self.uid)
        assert overdraft_after_cleaning

