# -*- coding: utf-8 -*-
import mock

from test.base import DiskTestCase, TestCase, parse_open_url_call
from mpfs.core.services.bazinga_service import BazingaInterface, OnetimeTask, TaskStatus


class BazingaResponses(object):
    tasks_count = '{"result":{"count":0}}'
    add_tasks = '{"result":{"jobsIds":[{"taskId":"test","uuid":"97e2d4c7-a2fe-4689-9993-b10248ada796"},{"taskId":"test","uuid":"adcddda6-72fe-42b6-ae06-667ab939a427"}]}}'


class DummyOnetimeTask(OnetimeTask):
    BAZINGA_TASK_NAME = 'test'

    def __init__(self, param):
        self.param = param

    def run(self):
        pass

    def build_command_parameters(self):
        return [self.param]


class BazingaInterfaceTestCase(TestCase):

    def test_create_task(self):
        bazinga_interface = BazingaInterface()
        with mock.patch.object(bazinga_interface, 'open_url', return_value=BazingaResponses.add_tasks) as stub:
            bazinga_interface.create_task(DummyOnetimeTask(1))
            url_info = parse_open_url_call(stub)
            assert url_info['page'] == '/tasks/add-tasks'
            assert len(url_info['json_body']['tasks']) == 1
            assert url_info['json_body']['tasks'][0]['taskId'] == DummyOnetimeTask.BAZINGA_TASK_NAME
            assert url_info['json_body']['tasks'][0]['parameters'] == '{"commandParameters": [1]}'

    def test_bulk_create_tasks(self):
        bazinga_interface = BazingaInterface()
        with mock.patch.object(bazinga_interface, 'open_url', return_value=BazingaResponses.add_tasks) as stub:
            bazinga_interface.bulk_create_tasks([DummyOnetimeTask(1), DummyOnetimeTask('a')])
            url_info = parse_open_url_call(stub)
            assert url_info['page'] == '/tasks/add-tasks'
            assert len(url_info['json_body']['tasks']) == 2
            assert url_info['json_body']['tasks'][0]['taskId'] == DummyOnetimeTask.BAZINGA_TASK_NAME
            assert url_info['json_body']['tasks'][0]['parameters'] == '{"commandParameters": [1]}'
            assert url_info['json_body']['tasks'][1]['taskId'] == DummyOnetimeTask.BAZINGA_TASK_NAME
            assert url_info['json_body']['tasks'][1]['parameters'] == '{"commandParameters": ["a"]}'

    def test_get_tasks_count(self):
        bazinga_interface = BazingaInterface()
        with mock.patch.object(bazinga_interface, 'open_url', return_value=BazingaResponses.tasks_count) as stub:
            # по дефолту все статусы
            bazinga_interface.get_tasks_count(DummyOnetimeTask)
            url_info = parse_open_url_call(stub)
            assert url_info['page'] == '/tasks/get-tasks-count'
            status_param = url_info['params'].pop('status')
            task_param = url_info['params'].pop('task')
            assert not url_info['params']
            assert len(task_param) == 1
            assert task_param[0] == DummyOnetimeTask.BAZINGA_TASK_NAME
            assert len(status_param) == 5
            for status in ['ready', 'starting', 'running', 'completed', 'failed']:
                assert status in status_param

        with mock.patch.object(bazinga_interface, 'open_url', return_value=BazingaResponses.tasks_count) as stub:
            # с указанием статуса
            bazinga_interface.get_tasks_count(DummyOnetimeTask, TaskStatus.FAILED)
            url_info = parse_open_url_call(stub)
            status_param = url_info['params'].pop('status')
            assert len(status_param) == 1
            assert status_param[0] == TaskStatus.FAILED.value

        with mock.patch.object(bazinga_interface, 'open_url', return_value=BazingaResponses.tasks_count) as stub:
            # с указанием нескольких статусов
            bazinga_interface.get_tasks_count(DummyOnetimeTask, [TaskStatus.FAILED, TaskStatus.COMPLETED])
            url_info = parse_open_url_call(stub)
            status_param = url_info['params'].pop('status')
            assert len(status_param) == 2
            assert set(status_param) == {TaskStatus.FAILED.value, TaskStatus.COMPLETED.value}
