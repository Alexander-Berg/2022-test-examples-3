# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from itertools import chain
from typing import Any, Dict, List, NamedTuple
from six import PY2, PY3
from sqlalchemy.exc import SQLAlchemyError
from unittest import skipUnless

if PY2:
    import mock
else:
    import unittest.mock as mock

from search.priemka.yappy.proto.structures.scheduler_pb2 import SchedulersTask, ProcessorTask
from search.priemka.yappy.src.scheduler.queue import ProcessorTasksSourceQueue, ProcessorTasksSinkQueue
from search.priemka.yappy.src.yappy_lib.queue import PartitionedMessage
from search.priemka.yappy.sqla.yappy import model
from search.priemka.yappy.tests.utils.test_cases import TestCase

TaskIdent = NamedTuple('TaskIdent', [('type', type(SchedulersTask.Type.UNKNOWN)), ('id', str)])
MockTasks = NamedTuple(
    'MockTasks',
    (
        ('schedulers_tasks', Dict[TaskIdent, mock.Mock]),
        ('processed_existing_tasks', List[TaskIdent]),
        ('processed_deleted_tasks', List[TaskIdent]),
        ('polled_tasks', List[mock.Mock])
    )
)


class ProcessorTasksSourceQueueTestCase(TestCase):

    TASK_SLEEP_INTERVAL = 100

    @classmethod
    def setUpClass(cls):
        cls.queue = ProcessorTasksSourceQueue()

    def setUp(self):
        self.queue.set_task_sleep_interval(self.TASK_SLEEP_INTERVAL)

    @staticmethod
    def mock_create_processor_tasks(tasks, *args, **kwargs):
        # type: (List[SchedulersTask], *Any, **Any) -> List[ProcessorTask]
        return [ProcessorTask(meta=ProcessorTask.Meta(id=t.id)) for t in tasks]

    @mock.patch('search.priemka.yappy.src.scheduler.queue.waiting_schedulers_tasks')
    def test_get_query_result(self, query_constructor):
        result = self.queue.get_query(mock.MagicMock(name='session'))
        self.assertEqual(result, query_constructor.return_value)

    @mock.patch('search.priemka.yappy.src.scheduler.queue.waiting_schedulers_tasks')
    def test_get_query_args(self, query_constructor):
        args = ('session', '_now')
        expected = ('session', self.queue.default_task_sleep_interval, '_now')
        self.queue.get_query(*args)
        query_constructor.assert_called_with(*expected)

    def test_set_task_sleep_interval(self):
        expected = 1235
        self.queue.set_task_sleep_interval(expected)
        self.assertEqual(self.queue.default_task_sleep_interval, expected)

    @mock.patch('search.priemka.yappy.src.scheduler.queue.update_tasks_meta')
    @mock.patch('search.priemka.yappy.src.scheduler.queue.create_processor_tasks')
    def test_post_session_hook_result_type(self, create_tasks, *mocks):
        task_ids = ['task-1', 'task-2']
        post_query_result = [SchedulersTask(id=task_id) for task_id in task_ids]
        create_tasks.side_effect = self.mock_create_processor_tasks
        expected = self.mock_create_processor_tasks(post_query_result)
        self.queue.post_session_hook(post_query_result)
        self.assertEqual(post_query_result, expected)

    @mock.patch('search.priemka.yappy.src.scheduler.queue.create_processor_tasks')
    @mock.patch('search.priemka.yappy.src.scheduler.queue.log_tasks')
    def test_post_session_hook_tasks_logging_params(self, log_tasks, *mocks):
        task_ids = ['task-1', 'task-2']
        post_query_result = [SchedulersTask(id=task_id) for task_id in task_ids]
        trace_kwargs = {'kw1': 'val1', 'kw2': 'val2'}
        self.queue.post_session_hook(post_query_result, trace_kwargs=trace_kwargs)
        log_tasks.assert_called_with(mock.ANY, self.queue.logger, **trace_kwargs)

    @mock.patch('search.priemka.yappy.src.scheduler.queue.create_processor_tasks')
    @mock.patch('search.priemka.yappy.src.scheduler.queue.log_tasks')
    def _test_post_session_hook_tasks_logging(self, log_tasks, create_tasks):
        task_ids = ['task-1', 'task-2']
        post_query_result = [SchedulersTask(id=task_id) for task_id in task_ids]
        create_tasks.side_effect = self.mock_create_processor_tasks
        self.queue.post_session_hook(post_query_result)
        passed_tasks = log_tasks.call_args.args[0]
        passed_ids = [task.meta.id for task in passed_tasks]
        return passed_ids, task_ids

    @skipUnless(PY2, 'py2 only')
    def test_post_session_hook_tasks_logging_py2(self):
        self.assertItemsEqual(*self._test_post_session_hook_tasks_logging())

    @skipUnless(PY3, 'py3 only')
    def test_post_session_hook_tasks_logging_py3(self):
        self.assertCountEqual(*self._test_post_session_hook_tasks_logging())

    @mock.patch('search.priemka.yappy.src.scheduler.queue.create_processor_tasks')
    @mock.patch('search.priemka.yappy.src.scheduler.queue.update_tasks_meta')
    def test_post_session_hook_tasks_meta_update(self, update_meta, create_tasks):
        task_ids = ['task-1', 'task-2']
        post_query_result = [SchedulersTask(id=task_id) for task_id in task_ids]
        create_tasks.side_effect = self.mock_create_processor_tasks
        self.queue.post_session_hook(post_query_result)
        update_meta.assert_called_with(post_query_result)

    @mock.patch('search.priemka.yappy.src.scheduler.queue.create_processor_tasks')
    @mock.patch('search.priemka.yappy.src.scheduler.queue.now')
    @mock.patch('search.priemka.yappy.src.scheduler.queue.update_schedulers_tasks')
    def test_post_query_hook_sched_tasks_update(self, update_tasks, now, *mocks):
        task_ids = ['task-1', 'task-2']
        query_result = [model.SchedulersTask(id=task_id) for task_id in task_ids]
        self.queue.post_query_hook(mock.Mock(), query_result)
        update_tasks.assert_called_with(
            query_result,
            in_fly=True,
            force_create=False,
            last_creation=now().timestamp()
        )

    @mock.patch('search.priemka.yappy.src.scheduler.queue.create_processor_tasks')
    @mock.patch('search.priemka.yappy.src.scheduler.task_utils.session_scope')
    def test_post_query_hook_not_crashes_on_failed_update(self, session_scope, *mocks):
        session_scope().__enter__().queue.side_effect = SQLAlchemyError('some error')
        try:
            self.queue.post_session_hook([])
        except SQLAlchemyError:
            self.fail('ProcessorTasksSourceQueue.post_query_hook() must not raise SQLAlchemyError')


class ProcessorTasksSinkQueueTestCase(TestCase):
    queue = None  # type: ProcessorTasksSinkQueue

    @classmethod
    def setUpClass(cls):
        cls.queue = ProcessorTasksSinkQueue()
        cls.modules_patch = mock.patch.object(cls.queue, 'modules')
        cls.config_patch = mock.patch.object(cls.queue, 'config')
        cls.verify_revision_patch = mock.patch.object(cls.queue, 'verify_message_revision', return_value=True)

    def setUp(self):
        self.modules_patch.start()
        self.config_patch.start()
        self.verify_revision_patch.start()
        self.addCleanup(self.modules_patch.stop)
        self.addCleanup(self.config_patch.stop)
        self.addCleanup(self.verify_revision_patch.stop)

    def mock_tasks(self, n_sched_tasks, n_deleted, task_type=SchedulersTask.Type.UNKNOWN):
        # type: (int, int, int) -> MockTasks
        sched_task_ids = ['task-{}'.format(i) for i in range(n_sched_tasks)]
        deleted_task_ids = ['deleted-task-{}'.format(i) for i in range(n_deleted)]
        sched_task_mock_kwargs = {
            'sleep_interval': 0,
        }
        processed_tasks = [TaskIdent(task_type, task_id) for task_id in sched_task_ids]
        processed_deleted = [TaskIdent(task_type, task_id) for task_id in deleted_task_ids]
        return MockTasks(
            schedulers_tasks={
                TaskIdent(task_type, task_id): mock.Mock(**sched_task_mock_kwargs)
                for task_id in sched_task_ids
            },
            processed_existing_tasks=processed_tasks,
            processed_deleted_tasks=processed_deleted,
            polled_tasks=[
                mock.Mock(**{'meta.type': ident.type, 'meta.id': ident.id})
                for ident in processed_tasks + processed_deleted
            ]
        )

    @mock.patch('search.priemka.yappy.src.scheduler.queue.update_schedulers_tasks')
    @mock.patch('search.priemka.yappy.src.scheduler.queue.index_existing_schedulers_tasks')
    def test_save_data_update_tasks(self, index_tasks, update_tasks):
        n_tasks = 5
        n_deleted = 2
        tasks = self.mock_tasks(n_tasks, n_deleted)
        index_tasks.return_value = tasks.schedulers_tasks
        self.queue.modules.process.return_value = (
            tasks.processed_existing_tasks[:-1],
            tuple(tasks.processed_existing_tasks[-1]),    # all but last task has changed
        )
        expected = [ident.id for ident in tasks.processed_existing_tasks]
        messages = [PartitionedMessage(t) for t in tasks.polled_tasks]
        self.queue._save_data(messages)
        generated_updates = list(chain(*[c.args[0] for c in update_tasks.call_args_list]))
        updated_ids = [task.meta.id for task in generated_updates]
        self.assertEquals(updated_ids, expected)

    @mock.patch('search.priemka.yappy.src.scheduler.queue.update_schedulers_tasks')
    @mock.patch('search.priemka.yappy.src.scheduler.queue.index_existing_schedulers_tasks')
    def test_save_data_dont_update_removed_tasks(self, index_tasks, update_tasks):
        n_tasks = 5
        n_deleted = 2
        tasks = self.mock_tasks(n_tasks, n_deleted)
        index_tasks.return_value = tasks.schedulers_tasks
        self.queue.modules.process.return_value = (
            (tasks.processed_existing_tasks + tasks.processed_deleted_tasks),
            tuple(),
        )
        messages = [PartitionedMessage(t) for t in tasks.polled_tasks]
        self.queue._save_data(messages)
        generated_updates = list(chain(*[c.args[0] for c in update_tasks.call_args_list]))
        updated_ids = [task.meta.id for task in generated_updates]
        deleted_ids = [ident.id for ident in tasks.processed_deleted_tasks]
        message = 'some of {} unexpectedly found in {}: {}'
        unexpected = []
        for task_id in deleted_ids:
            # noinspection PyBroadException
            try:
                self.assertNotIn(task_id, updated_ids)
            except self.failureException:
                unexpected.append(task_id)
        if unexpected:
            self.fail(message.format(deleted_ids, updated_ids, unexpected))

    @mock.patch('search.priemka.yappy.src.scheduler.queue.index_existing_schedulers_tasks')
    def test_save_message_success(self, index_tasks):
        ident = TaskIdent(SchedulersTask.Type.UNKNOWN, 'task-0')
        task = mock.Mock(ProcessorTask, **{'meta.type': ident.type, 'meta.id': ident.id})
        session = mock.Mock(name='session')
        index_tasks.return_value = {}
        self.queue.modules.process.return_value = (tuple(), tuple())
        result = self.queue.save_message(PartitionedMessage(task), session)
        self.assertTrue(result)
