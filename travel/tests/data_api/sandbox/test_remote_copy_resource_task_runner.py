# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import timedelta
from unittest import TestCase

from mock import Mock

from common.data_api.sandbox.remote_copy_resource_task_runner import RemoteCopyResourceTaskRunner
from common.data_api.sandbox.sandbox_task_runner import SandboxTaskRunner


class TestRemoteCopyResourceTaskRunner(TestCase):
    def setUp(self):
        self._fake_sandbox_runner = Mock(name='SANDBOX_RUNNER', spec=SandboxTaskRunner)
        self._task_runner = RemoteCopyResourceTaskRunner(
            sandbox_runner=self._fake_sandbox_runner
        )

    def test_run(self):
        resource_type = 'RESOURCE_TYPE'
        rbtorrent = 'rbtorrent:xxx'
        source_name = 'SOURCE_NAME'
        resource_id = Mock(name='RESOURCE_ID')

        self._fake_sandbox_runner.run = Mock(return_value=resource_id)

        assert self._task_runner.run(resource_type, rbtorrent, source_name) == resource_id

        self._fake_sandbox_runner.run.assert_called_once_with(
            task_type='REMOTE_COPY_RESOURCE',
            resource_type=resource_type,
            description='rasp resource upload',
            max_wait_time=timedelta(hours=1),
            check_delay_time=timedelta(seconds=20),
            custom_fields=[
                {'name': 'resource_type', 'value': resource_type},
                {'name': 'created_resource_name', 'value': source_name},
                {'name': 'remote_file_name', 'value': rbtorrent},
            ]
        )
