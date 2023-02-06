# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from search.priemka.yappy.src.yappy_lib.sandbox import YappySandboxClient

from yappy_protoc.yappy import SandboxTask, SandboxFile

from search.priemka.yappy.tests.utils.test_cases import TestCase


class MySandboxClientMock(YappySandboxClient):
    YASM_PREFIX = 'my-sandbox-mock'

    def get_resource_proto(self, resource_id, ignore_cache):
        if 'my-resource-id' == resource_id:
            return SandboxFile(
                id='my-resource-id',
                task_type='my-task-type',
                task_id='my-task-id',
                resource_type='my-resource-type',
            )
        else:
            raise ValueError('invalid resource id for mock {}'.format(resource_id))

    def get_task_proto(self, task_id, ignore_cache=False):
        if 'my-task-id' == task_id:
            return SandboxTask(
                id='my-task-id',
                status=SandboxTask.Status.SUCCESS,
                type='my-task-type',
                files=[
                    SandboxFile(
                        id='my-resource-id',
                        task_type='my-task-type',
                        task_id='my-task-id',
                        resource_type='my-resource-type',
                    )
                ],
            )
        else:
            raise ValueError('invalid task id for mock {}'.format(task_id))


class TestSandboxClient(TestCase):
    def setUp(self):
        self.sandbox = MySandboxClientMock()

    def test_sandbox_resource_resolution(self):
        with self.assertRaises(ValueError):
            self.sandbox.resolve_sandbox_file(task_id='123', resource_type='some-resource-type')

        with self.assertRaises(ValueError):
            self.sandbox.resolve_sandbox_file(task_id='my-task-id', resource_type='some-resource-type')

        resource = self.sandbox.resolve_sandbox_file(task_id='my-task-id', resource_type='my-resource-type')

        self.assertEqual('my-resource-id', resource.id)

        with self.assertRaises(ValueError):
            self.sandbox.resolve_sandbox_file(resource_id='123')

        resource = self.sandbox.resolve_sandbox_file(resource_id='my-resource-id')

        self.assertEqual('my-resource-type', resource.resource_type)
