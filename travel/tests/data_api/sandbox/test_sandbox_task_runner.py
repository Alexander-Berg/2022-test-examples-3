# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import timedelta, datetime
from logging import Logger
from unittest import TestCase

from mock import Mock, call

from travel.rasp.library.python.api_clients.sandbox import SandboxClient

from common.data_api.sandbox.errors import (
    SandboxFailTaskException, SandboxNotFinishedTaskException,
    SandboxNotReadyResourceException, SandboxNotFoundResourceException
)

from common.data_api.sandbox.sandbox_task_runner import SandboxTaskRunner


class TestSandboxRunner(TestCase):
    def setUp(self):
        self._fake_public_api = Mock(name='SandboxApiClient', spec=SandboxClient)
        self._fake_logger = Mock(spec=Logger)
        self._fake_environment = Mock()
        self._task_runner = SandboxTaskRunner(
            api=self._fake_public_api,
            environment=self._fake_environment,
            logger=self._fake_logger
        )

        self._now = datetime(2018, 9, 1)

        self._task_id = 666
        self._task_type = 'TASK_TYPE'
        self._resource_id = 999
        self._resource_type = 'RESOURCE_TYPE'

    def _make_resource_info(self, type, state, pk):
        return {
            'type': type,
            'state': state,
            'id': pk
        }

    def _run(self):
        self._task_runner.run(
            task_type=self._task_type,
            resource_type=self._resource_type,
            description="DESCRIPTION",
            custom_fields=[{'name': 'name', 'value': 'value'}],
            max_wait_time=timedelta(milliseconds=10),
            check_delay_time=timedelta(milliseconds=1)
        )

    def _async_run(self):
        self._task_runner.async_run(
            task_type=self._task_type,
            description="DESCRIPTION",
            custom_fields=[{'name': 'name', 'value': 'value'}],
        )

    def _assert_draft_call(self):
        self._fake_public_api.create_task_draft.assert_called_once_with(
            self._task_type,
            owner='FAKE_OWNER',
            description='DESCRIPTION',
            custom_fields=[{'name': 'name', 'value': 'value'}]
        )

    def test_async_run(self):
        self._fake_public_api.create_task_draft.return_value = self._task_id
        self._async_run()

        self._assert_draft_call()
        self._fake_public_api.start_task.assert_called_once_with(
            self._task_id
        )

    def test_run_immediate_done_task(self):
        self._fake_public_api.create_task_draft.return_value = self._task_id
        self._fake_environment.now.return_value = self._now
        self._fake_public_api.get_task_status.side_effect = [
            'SUCCESS', 'SUCCESS'
        ]
        self._fake_public_api.get_task_resources.return_value = [
            self._make_resource_info('another_' + self._resource_type, 'READY', self._resource_id - 1),
            self._make_resource_info(self._resource_type, 'READY', self._resource_id),
            self._make_resource_info('another_' + self._resource_type, 'READY', self._resource_id + 1),
            self._make_resource_info(self._resource_type, 'FAIL', self._resource_id + 2),
        ]

        self._run()

        self._assert_draft_call()
        self._fake_public_api.start_task.assert_called_once_with(
            self._task_id
        )
        self._fake_public_api.get_task_status.assert_has_calls([
            call(self._task_id) for _ in range(2)
        ])
        self._fake_public_api.get_task_resources.assert_called_once_with(
            self._task_id
        )

    def test_run_immediate_done_task_but_resource_is_not_ready(self):
        self._fake_public_api.create_task_draft.return_value = self._task_id
        self._fake_environment.now.return_value = self._now
        self._fake_public_api.get_task_status.side_effect = [
            'SUCCESS', 'SUCCESS'
        ]
        self._fake_public_api.get_task_resources.return_value = [
            self._make_resource_info('another_' + self._resource_type, 'READY', self._resource_id - 1),
            self._make_resource_info(self._resource_type, 'NOT_READY', self._resource_id),
            self._make_resource_info('another_' + self._resource_type, 'READY', self._resource_id + 1),
            self._make_resource_info(self._resource_type, 'FAIL', self._resource_id + 2),
        ]

        with self.assertRaises(SandboxNotReadyResourceException) as error:
            self._run()
        assert error.exception.msg == 'Task #666. Resource type [RESOURCE_TYPE]. Resource is not ready.'

        self._fake_public_api.create_task_draft.assert_called_once_with(
            self._task_type,
            owner='FAKE_OWNER',
            description='DESCRIPTION',
            custom_fields=[{'name': 'name', 'value': 'value'}]
        )
        self._fake_public_api.start_task.assert_called_once_with(
            self._task_id
        )
        self._fake_public_api.get_task_status.assert_has_calls([
            call(self._task_id) for _ in range(2)
        ])
        self._fake_public_api.get_task_resources.assert_called_once_with(
            self._task_id
        )

    def test_run_immediate_done_task_but_resource_is_not_found(self):
        self._fake_public_api.create_task_draft.return_value = self._task_id
        self._fake_environment.now.return_value = self._now
        self._fake_public_api.get_task_status.side_effect = [
            'SUCCESS', 'SUCCESS'
        ]
        self._fake_public_api.get_task_resources.return_value = [
            self._make_resource_info('another_' + self._resource_type, 'READY', self._resource_id - 1),
            self._make_resource_info('another_' + self._resource_type, 'READY', self._resource_id + 1),
        ]

        with self.assertRaises(SandboxNotFoundResourceException) as error:
            self._run()
        assert error.exception.msg == 'Task #666. Resource type [RESOURCE_TYPE]. Resource is not found.'

        self._assert_draft_call()
        self._fake_public_api.start_task.assert_called_once_with(
            self._task_id
        )
        self._fake_public_api.get_task_status.assert_has_calls([
            call(self._task_id) for _ in range(2)
        ])
        self._fake_public_api.get_task_resources.assert_called_once_with(
            self._task_id
        )

    def test_run_task_with_one_retry(self):
        self._fake_public_api.create_task_draft.return_value = self._task_id
        self._fake_environment.now.return_value = self._now
        self._fake_public_api.get_task_status.side_effect = [
            'WAIT', 'SUCCESS', 'SUCCESS'
        ]
        self._fake_public_api.get_task_resources.return_value = [
            {
                'type': 'another_' + self._resource_type,
                'state': 'READY',
                'id': self._resource_id
            },
            {
                'type': self._resource_type,
                'state': 'READY',
                'id': self._resource_id
            },
            {
                'type': 'another_' + self._resource_type,
                'state': 'READY',
                'id': self._resource_id
            },
            {
                'type': self._resource_type,
                'state': 'FAIL',
                'id': self._resource_id * self._resource_id
            },
        ]

        self._run()

        self._assert_draft_call()
        self._fake_public_api.start_task.assert_called_once_with(
            self._task_id
        )
        self._fake_public_api.get_task_status.assert_has_calls([
            call(self._task_id) for _ in range(3)
        ])
        self._fake_public_api.get_task_resources.assert_called_once_with(
            self._task_id
        )

    def test_run_broken_task(self):
        self._fake_public_api.create_task_draft.return_value = self._task_id
        self._fake_environment.now.return_value = self._now
        self._fake_public_api.get_task_status.side_effect = [
            'FAILURE'
        ]

        with self.assertRaises(SandboxFailTaskException) as error:
            self._run()
        assert error.exception.msg == 'Task #666. Task has failed.'
        self._assert_draft_call()
        self._fake_public_api.start_task.assert_called_once_with(
            self._task_id
        )
        self._fake_public_api.get_task_status.assert_has_calls([
            call(self._task_id) for _ in range(1)
        ])
        assert not self._fake_public_api.get_task_resources.called

    def test_run_slow_task(self):
        self._fake_public_api.create_task_draft.return_value = self._task_id
        self._fake_environment.now.side_effect = [
            self._now,
            self._now + timedelta(milliseconds=1),
            self._now + timedelta(seconds=1)
        ]
        self._fake_public_api.get_task_status.side_effect = [
            'WAIT', 'WAIT', 'WAIT'
        ]

        with self.assertRaises(SandboxNotFinishedTaskException) as error:
            self._run()
        assert error.exception.msg == 'Task #666. Task has not finished.'
        self._assert_draft_call()

        self._fake_public_api.start_task.assert_called_once_with(
            self._task_id
        )
        self._fake_public_api.get_task_status.assert_has_calls([
            call(self._task_id) for _ in range(1)
        ])
        assert not self._fake_public_api.get_task_resources.called
