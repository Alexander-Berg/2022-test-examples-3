# -*- coding: utf-8 -*-
import mock

from mpfs.common.static import tags
from mpfs.core.queue import mpfs_queue
from test.base import DiskTestCase

from mpfs.common.util.experiments.logic import experiment_manager
from test.base_suit import UserTestCaseMixin
from test.parallelly.api.disk.base import DiskApiTestCase


class ExperimentsMPFSTestCase(DiskTestCase):

    def test_experiments_initialized_on_endpoint_request(self):
        experiment_manager.context.pop('uid')
        assert not experiment_manager.context.get('uid')
        self.json_ok('user_info', {'uid': self.uid})
        assert self.uid == experiment_manager.context.get('uid')

    def test_experiments_initialized_on_task_execution(self):
        experiment_manager.context.pop('uid')
        assert not experiment_manager.context.get('uid')
        mpfs_queue.put({'uid': self.uid}, 'restore_yateam_folder')
        assert self.uid == experiment_manager.context.get('uid')


class ExperimentsRESTAPITestCase(UserTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'

    def test_experiments_initialized_on_endpoint_request(self):
        experiment_manager.context.pop('uid', None)
        assert not experiment_manager.context.get('uid')
        with mock.patch('mpfs.platform.v1.disk.rostelecom.handlers.RostelecomCheckUserHandler.handle', return_value=None):
            self.client.request('GET', 'disk/rostelecom/cloud-platform/check', uid=self.uid)
        assert self.uid == experiment_manager.context.get('uid')
