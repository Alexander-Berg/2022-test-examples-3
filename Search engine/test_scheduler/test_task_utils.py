# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import random
import six
import unittest
import uuid

if six.PY2:
    import mock
else:
    import unittest.mock as mock

from search.martylib.db_utils import session_scope, to_model
from search.martylib.protobuf_utils.patch import patch_enums

from search.priemka.yappy.sqla.yappy import model
from search.priemka.yappy.src.scheduler import task_utils
from search.priemka.yappy.proto.structures.auth_pb2 import StaffUnion
from search.priemka.yappy.proto.structures.beta_component_pb2 import BetaComponent
from search.priemka.yappy.proto.structures.scheduler_pb2 import ProcessorTask, SchedulersTask
from search.priemka.yappy.proto.structures.slot_pb2 import Slot

from search.priemka.yappy.tests.utils.test_cases import TestCase, TestCaseWithDB, TestCaseWithStaticDB

patch_enums()

N_TYPES = len(list(SchedulersTask.Type.keys()))


class TaskUtilsWaitingTasksTestCase(TestCaseWithStaticDB):

    _NOW = 100000000
    SLEEP_INTERVAL = 600  # 10 min
    CUSTOM_SLEEP_INTERVAL = 300  # 5 min

    FORCED_WAITING = 1
    FORCED_SLEEPING = 2
    FORCED_PROCESSING = 3
    FORCED_FLYING = 4

    SLEEPING_TASKS = 10
    WAITING_TASKS = 15
    FLYING_TASKS = 20

    waiting_timestamps = []
    sleeping_timestamps = []

    @classmethod
    def setUpClass(cls):
        super(TaskUtilsWaitingTasksTestCase, cls).setUpClass()
        cls.patch_now = mock.patch('search.priemka.yappy.src.scheduler.task_utils.now')
        cls._now = cls.patch_now.start()

    def setUp(self):
        self._now().timestamp.return_value = self._NOW

    @classmethod
    def tearDownClass(cls):
        cls.patch_now.stop()
        super(TaskUtilsWaitingTasksTestCase, cls).tearDownClass()

    @classmethod
    def any_timestamp(cls):
        """ Generate timestamp for cases that must not be affected by the value """
        return cls._NOW - random.randint(1, 2 * cls.SLEEP_INTERVAL)

    @staticmethod
    def any_bool():
        """ Generate flag value for cases that must not be affected by the value """
        return bool(random.randint(0, 1))

    @staticmethod
    def any_type():
        """ Generate task type for cases that must not be affected by the value """
        return SchedulersTask.Type[random.randint(0, N_TYPES - 1)]

    @classmethod
    def generate_sleeping_timestamps(cls):
        """ Generated timestamps within `SLEEP_INTERVAL` sec before `NOW` (for waiting tasks) """
        cls.sleeping_timestamps = [
            cls._NOW - random.randint(1, cls.SLEEP_INTERVAL)
            for _ in range(cls.SLEEPING_TASKS)
        ]

    @classmethod
    def generate_waiting_timestamps(cls):
        """ Generated timestamps prior to `SLEEP_INTERVAL` sec before `NOW` (for sleeping tasks) """
        cls.waiting_timestamps = [
            cls._NOW - cls.SLEEP_INTERVAL - random.randint(1, cls.SLEEP_INTERVAL)
            for _ in range(cls.WAITING_TASKS)
        ]

    @classmethod
    def create_test_data(cls):
        cls.generate_waiting_timestamps()
        cls.generate_sleeping_timestamps()
        tasks = (
            [
                model.SchedulersTask(
                    id='sleeping-{}'.format(i),
                    last_creation=cls.sleeping_timestamps[i],
                    force_create=(i < cls.FORCED_SLEEPING),
                    in_fly=False,
                    sleep_interval=cls.CUSTOM_SLEEP_INTERVAL,
                    type=cls.any_type(),
                )
                for i in range(cls.SLEEPING_TASKS)
            ] +
            [
                model.SchedulersTask(
                    id='waiting-{}'.format(i),
                    last_creation=cls.waiting_timestamps[i],
                    force_create=(i < cls.FORCED_WAITING),
                    in_fly=False,
                    sleep_interval=cls.CUSTOM_SLEEP_INTERVAL,
                    type=cls.any_type(),
                )
                for i in range(cls.WAITING_TASKS)
            ] +
            [
                model.SchedulersTask(
                    id='flying-{}'.format(i),
                    last_creation=cls.any_timestamp(),
                    force_create=(i < cls.FORCED_FLYING),
                    in_fly=True,
                    sleep_interval=cls.CUSTOM_SLEEP_INTERVAL,
                    type=cls.any_type(),
                )
                for i in range(cls.FLYING_TASKS)
            ]
        )
        with session_scope() as session:
            session.add_all(tasks)

    @property
    def custom_sleeping_unforced_tasks(self):
        """ Calculate number of sleeping tasks without `force_create` flag within `CUSTOM_SLEEP_INTERVAL` """
        return len(
            [
                ts for ts in self.sleeping_timestamps[self.FORCED_SLEEPING:]
                if ts >= self._NOW - self.CUSTOM_SLEEP_INTERVAL
            ]
        )

    def test_with_interval(self):
        expected = self.WAITING_TASKS + self.FORCED_SLEEPING
        with session_scope() as session:
            q = task_utils.waiting_schedulers_tasks(session, self.SLEEP_INTERVAL)
            tasks = q.all()
            data = [[task.id, self._NOW - task.last_creation, task.force_create] for task in tasks]
            for task in sorted(data):
                print(task)
            result = q.count()
        self.assertEqual(result, expected)

    def test_passed_now_with_shift(self):
        expected = self.WAITING_TASKS + self.FORCED_SLEEPING
        shift = 5 * self.SLEEP_INTERVAL
        self._now().timestamp.return_value = self._NOW + shift
        with session_scope() as session:
            q = task_utils.waiting_schedulers_tasks(session, self.SLEEP_INTERVAL, self._NOW)
            result = q.count()
        self.assertEqual(result, expected)

    def test_passed_now_all_awaken(self):
        expected = self.WAITING_TASKS + self.SLEEPING_TASKS
        shifted_now = self._NOW + 2 * self.SLEEP_INTERVAL
        with session_scope() as session:
            q = task_utils.waiting_schedulers_tasks(session, self.SLEEP_INTERVAL, shifted_now)
            result = q.count()
        self.assertEqual(result, expected)

    def test_custom_sleep_interval(self):
        expected = self.WAITING_TASKS + self.SLEEPING_TASKS - self.custom_sleeping_unforced_tasks
        with session_scope() as session:
            q = task_utils.waiting_schedulers_tasks(session)
            result = q.count()
        self.assertEqual(result, expected)


class TaskUtilsTestCase(TestCase):
    """ Test case for small technical utils testing """

    TASK_ID = 'test-task-id'
    TASK_TYPE = SchedulersTask.Type.COMPONENT

    def test_get_processor_task_id(self):
        task = ProcessorTask()
        task.meta.id = self.TASK_ID
        result = task_utils.get_task_id(task)
        self.assertEqual(result, self.TASK_ID)

    def test_get_schedulers_task_id(self):
        task = SchedulersTask(id=self.TASK_ID)
        result = task_utils.get_task_id(task)
        self.assertEqual(result, self.TASK_ID)

    def test_get_processor_task_type(self):
        task = ProcessorTask()
        task.meta.type = self.TASK_TYPE
        result = task_utils.get_task_type(task)
        self.assertEqual(result, self.TASK_TYPE)

    def test_get_schedulers_task_type(self):
        task = SchedulersTask(type=self.TASK_TYPE)
        result = task_utils.get_task_type(task)
        self.assertEqual(result, self.TASK_TYPE)

    def test_task_update_invalid_field(self):
        self.assertRaises(
            AttributeError,
            task_utils.update_schedulers_tasks,
            [SchedulersTask()],
            no_such_field='some_value',
        )

    @mock.patch('search.priemka.yappy.src.scheduler.task_utils.session_scope')
    def test_task_update_task(self, _session_scope):
        fields = {'last_creation': 123}
        session = _session_scope().__enter__.return_value
        task_utils.update_schedulers_tasks([SchedulersTask(id='id', type=SchedulersTask.Type.BETA)], **fields)
        session.query.assert_called_with(model.SchedulersTask)

    @mock.patch('search.priemka.yappy.src.scheduler.task_utils.session_scope')
    def test_task_update_passed_task(self, _session_scope):
        """
        NOTE: test result depends on the filters order. Not cool, but...
        """
        fields = {'last_creation': 123}
        session = _session_scope().__enter__.return_value
        task = SchedulersTask(id='id', type=SchedulersTask.Type.BETA)
        task_utils.update_schedulers_tasks([task], **fields)
        applied_filter = session.query().filter.call_args.args
        expected_filter = [
            model.SchedulersTask.id == task.id,
            model.SchedulersTask.type == SchedulersTask.Type[task.type],
        ]
        self.assertEqual(len(applied_filter), len(expected_filter))
        for applied, expected in zip(applied_filter, expected_filter):
            self.assertTrue(applied.compare(expected))

    @mock.patch('search.priemka.yappy.src.scheduler.task_utils.session_scope')
    def test_task_update_passed_values(self, _session_scope):
        fields = {'last_creation': 123}
        session = _session_scope().__enter__.return_value
        task_utils.update_schedulers_tasks([SchedulersTask(id='id', type=SchedulersTask.Type.BETA)], **fields)
        session.query().filter().update.assert_called_with(fields)

    def test_log_tasks_custom_logger(self):
        task_ids = ['task-1', 'task-2']
        tasks = [ProcessorTask(meta=ProcessorTask.Meta(id=task_id), log_task=True) for task_id in task_ids]
        logger = mock.MagicMock(name='logger')
        task_utils.log_tasks(tasks, logger)
        expected = [mock.call(mock.ANY, task) for task in tasks]
        result = logger.info.call_args_list
        self.assertEqual(result, expected)

    @mock.patch('search.priemka.yappy.src.scheduler.task_utils.LOGGER')
    def test_log_tasks_default_logger(self, default_logger):
        task_ids = ['task-1', 'task-2']
        tasks = [ProcessorTask(meta=ProcessorTask.Meta(id=task_id), log_task=True) for task_id in task_ids]
        task_utils.log_tasks(tasks)
        expected = [mock.call(mock.ANY, task) for task in tasks]
        result = default_logger.info.call_args_list
        self.assertEqual(result, expected)

    @mock.patch('search.priemka.yappy.src.scheduler.task_utils.trace')
    def test_log_tasks_trace(self, trace):
        task_ids = ['task-1', 'task-2']
        trace_kwargs = {'kw1': 'val1', 'kw2': 'val2'}
        tasks = [ProcessorTask(meta=ProcessorTask.Meta(id=task_id), log_task=True) for task_id in task_ids]
        task_utils.log_tasks(tasks, **trace_kwargs)
        expected = [mock.call('log_task', log_task_id=task_id, **trace_kwargs) for task_id in task_ids]
        result = trace.call_args_list
        self.assertEqual(result, expected)

    def test_log_tasks_logger_log_only_tagged(self):
        task_ids = ['task-1', 'task-2']
        logged_task = ProcessorTask(meta=ProcessorTask.Meta(id='logged-task'), log_task=True)
        tasks = [ProcessorTask(meta=ProcessorTask.Meta(id=task_id)) for task_id in task_ids]
        tasks.append(logged_task)
        logger = mock.MagicMock(name='logger')
        task_utils.log_tasks(tasks, logger)
        logger.info.assert_called_once_with(mock.ANY, logged_task)

    @mock.patch('search.priemka.yappy.src.scheduler.task_utils.Storage')
    @mock.patch('search.priemka.yappy.src.scheduler.task_utils.now')
    def test_update_tasks_meta_defaults(self, _now, storage):
        _now().timestamp.return_value = 123
        reqid = 'dummy reqid'
        storage().thread_local.request_id = reqid
        task_ids = ['task-1', 'task-3']
        tasks = [ProcessorTask(meta=ProcessorTask.Meta(id=task_id)) for task_id in task_ids]
        expected = [
            ProcessorTask(
                meta=ProcessorTask.Meta(
                    id=task_id,
                    build_at=_now().timestamp(),
                    build_via=reqid,
                )
            )
            for task_id in task_ids
        ]
        task_utils.update_tasks_meta(tasks)
        self.assertEqual(tasks, expected)

    def test_update_tasks_meta_args(self):
        _now = 123
        reqid = 'dummy reqid'
        task_ids = ['task-1', 'task-3']
        tasks = [ProcessorTask(meta=ProcessorTask.Meta(id=task_id)) for task_id in task_ids]
        expected = [
            ProcessorTask(
                meta=ProcessorTask.Meta(
                    id=task_id,
                    build_at=_now,
                    build_via=reqid,
                )
            )
            for task_id in task_ids
        ]
        task_utils.update_tasks_meta(tasks, _now, reqid)
        self.assertEqual(tasks, expected)

    @mock.patch('search.priemka.yappy.src.scheduler.task_utils.index_by')
    def test_index_schedulers_tasks(self, index_by):
        fake_tasks = [mock.Mock(), mock.Mock(), mock.Mock()]
        fake_query = mock.Mock()
        def query_side_effect(arg):
            if arg != model.SchedulersTask:
                self.failureException("unexpected querying of: {}".format(arg))
            return fake_query

        session = mock.MagicMock()
        session.query.side_effect = query_side_effect
        session.query(model.SchedulersTask).all.return_value = fake_tasks

        task_utils.index_schedulers_tasks(session)
        index_by.assert_called_with([task.to_protobuf() for task in fake_tasks], ('type', 'id'), composite_key=True)


class CreateProcessorTasksTest(TestCaseWithDB):

    @classmethod
    def setUpClass(cls):
        super(CreateProcessorTasksTest, cls).setUpClass()
        hring_slot = 'hring_slot'
        cls.config = {
            'hring_slot': hring_slot,
            'components': [
                BetaComponent(
                    id=str(uuid.uuid4()),
                    slot=Slot(id='hring-slot-{}'.format(i), type=Slot.Type.HASHRING, hashring_id=hring_slot),
                )
                for i in range(5)
            ]
        }

    def create_test_data(self):
        with session_scope() as session:
            session.add_all([to_model(bc) for bc in self.config['components']])

    @mock.patch('search.priemka.yappy.src.scheduler.task_utils.get_beta_components_auth')
    def test_create_component_task_with_auth(self, mocked):
        cid = self.config['components'][0].id
        tasks = [SchedulersTask(type=SchedulersTask.Type.COMPONENT, id=c.id) for c in self.config['components']]
        expected = StaffUnion(logins=['a-login', 'b-login'], groups=[1, 5])
        mocked.return_value = {cid: expected}

        with session_scope() as session:
            processor_tasks = task_utils.create_processor_tasks(tasks, session)

        result = processor_tasks[0].auth

        self.assertEqual(result, expected)

    @mock.patch('search.priemka.yappy.src.scheduler.task_utils.get_beta_components_auth')
    def test_create_component_task_no_auth(self, mocked):
        cid = self.config['components'][0].id
        task = SchedulersTask(type=SchedulersTask.Type.COMPONENT, id=cid)
        mocked.return_value = {}
        expected = StaffUnion()

        with session_scope() as session:
            processor_tasks = task_utils.create_processor_tasks([task], session)

        result = processor_tasks[0].auth

        self.assertEqual(result, expected)

    @mock.patch('search.priemka.yappy.src.scheduler.task_utils.get_beta_components_auth')
    def test_create_component_task_get_auth_args(self, mocked):
        tasks = [
            SchedulersTask(type=SchedulersTask.Type.COMPONENT, id=bc.id)
            for bc in self.config['components']
        ]
        mocked.return_value = {}
        expected = sorted([bc.id for bc in self.config['components']])

        with session_scope() as session:
            task_utils.create_processor_tasks(tasks, session)
            result = sorted([str(bc.id) for bc in mocked.call_args.args[0]])

        self.assertEqual(result, expected)

    @mock.patch('search.priemka.yappy.src.scheduler.task_utils.get_beta_components_auth')
    def test_create_hashring_task_with_auth_all_components(self, mocked):
        task = SchedulersTask(type=SchedulersTask.Type.HASHRING_COMPONENT, id=self.config['hring_slot'])
        expected = StaffUnion(logins=['a-login', 'b-login'], groups=[1, 5])
        mocked.return_value = {bc.id: expected for bc in self.config['components']}

        with session_scope() as session:
            processor_tasks = task_utils.create_processor_tasks([task], session)
        result = processor_tasks[0].auth

        self.assertEqual(result, expected)

    @mock.patch('search.priemka.yappy.src.scheduler.task_utils.get_beta_components_auth')
    def test_create_hashring_task_with_auth_one_component(self, mocked):
        task = SchedulersTask(type=SchedulersTask.Type.HASHRING_COMPONENT, id=self.config['hring_slot'])
        expected = StaffUnion(logins=['a-login', 'b-login'], groups=[1, 5])
        cid = self.config['components'][0].id
        mocked.return_value = {cid: expected}

        with session_scope() as session:
            processor_tasks = task_utils.create_processor_tasks([task], session)
        result = processor_tasks[0].auth

        self.assertEqual(result, expected)

    @mock.patch('search.priemka.yappy.src.scheduler.task_utils.merge_auth')
    @mock.patch('search.priemka.yappy.src.scheduler.task_utils.get_beta_components_auth')
    def _test_create_hashring_task_with_different_auth(self, mocked_get, mocked_merge):
        task = SchedulersTask(type=SchedulersTask.Type.HASHRING_COMPONENT, id=self.config['hring_slot'])
        auth_1 = StaffUnion(logins=['a-login', 'b-login'], groups=[1, 5])
        auth_2 = StaffUnion(logins=['a-login', 'c-login'])
        cid_1 = self.config['components'][0].id
        cid_2 = self.config['components'][1].id
        mocked_get.return_value = {cid_1: auth_1, cid_2: auth_2}
        mocked_merge.return_value = StaffUnion()

        with session_scope() as session:
            task_utils.create_processor_tasks([task], session)

        result = mocked_merge.call_args.args[0]
        expected = [auth_1, auth_2]

        return result, expected

    @unittest.skipUnless(six.PY2, 'py2 only')
    def test_create_hashring_task_with_different_auth_py2(self):
        self.assertItemsEqual(*self._test_create_hashring_task_with_different_auth())

    @unittest.skipUnless(six.PY3, 'py3 only')
    def test_create_hashring_task_with_different_auth_py3(self):
        self.assertCountEqual(*self._test_create_hashring_task_with_different_auth())

    @mock.patch('search.priemka.yappy.src.scheduler.task_utils.get_beta_components_auth')
    def test_create_hashring_task_no_auth(self, mocked):
        task = SchedulersTask(type=SchedulersTask.Type.HASHRING_COMPONENT, id=self.config['hring_slot'])
        mocked.return_value = {}
        expected = StaffUnion()

        with session_scope() as session:
            processor_tasks = task_utils.create_processor_tasks([task], session)
        result = processor_tasks[0].auth

        self.assertEqual(result, expected)

    @mock.patch('search.priemka.yappy.src.scheduler.task_utils.create_beta_processor_tasks')
    @mock.patch('search.priemka.yappy.src.scheduler.task_utils.create_component_processor_tasks')
    @mock.patch('search.priemka.yappy.src.scheduler.task_utils.create_hashring_processor_tasks')
    def test_create_processor_tasks_subcalls(self, *mocks):
        expected = []
        for i, m in enumerate(mocks):
            val = 'return_value_{}'.format(i)
            m.return_value = [val]
            expected.append(val)

        with session_scope() as session:
            processor_tasks = task_utils.create_processor_tasks([], session)

        self.assertEqual(sorted(processor_tasks), expected)

    @mock.patch('search.priemka.yappy.src.scheduler.task_utils.create_component_processor_tasks')
    @mock.patch('search.priemka.yappy.src.scheduler.task_utils.create_hashring_processor_tasks')
    @mock.patch('search.priemka.yappy.src.scheduler.task_utils.create_beta_processor_tasks')
    def test_create_processor_beta_tasks(self, create_beta_tasks, *mocks):
        tasks = []
        for i in range(3):
            tasks += [
                SchedulersTask(id='b-task-{}'.format(i), type=SchedulersTask.Type.BETA),
                SchedulersTask(id='c-task-{}'.format(i), type=SchedulersTask.Type.COMPONENT),
                SchedulersTask(id='h-task-{}'.format(i), type=SchedulersTask.Type.HASHRING_COMPONENT),
            ]

        expected = {task.id: task for task in tasks if task.type == SchedulersTask.Type.BETA}

        with session_scope() as session:
            task_utils.create_processor_tasks(tasks, session)
            create_beta_tasks.assert_called_with(expected, session)

    @mock.patch('search.priemka.yappy.src.scheduler.task_utils.create_hashring_processor_tasks')
    @mock.patch('search.priemka.yappy.src.scheduler.task_utils.create_beta_processor_tasks')
    @mock.patch('search.priemka.yappy.src.scheduler.task_utils.create_component_processor_tasks')
    def test_create_processor_component_tasks(self, create_components_tasks, *mocks):
        tasks = []
        for i in range(3):
            tasks += [
                SchedulersTask(id='b-task-{}'.format(i), type=SchedulersTask.Type.BETA),
                SchedulersTask(id='c-task-{}'.format(i), type=SchedulersTask.Type.COMPONENT),
                SchedulersTask(id='h-task-{}'.format(i), type=SchedulersTask.Type.HASHRING_COMPONENT),
            ]

        expected = {task.id: task for task in tasks if task.type == SchedulersTask.Type.COMPONENT}

        with session_scope() as session:
            task_utils.create_processor_tasks(tasks, session)
            create_components_tasks.assert_called_with(expected, session)

    @mock.patch('search.priemka.yappy.src.scheduler.task_utils.create_component_processor_tasks')
    @mock.patch('search.priemka.yappy.src.scheduler.task_utils.create_beta_processor_tasks')
    @mock.patch('search.priemka.yappy.src.scheduler.task_utils.create_hashring_processor_tasks')
    def test_create_processor_hring_tasks(self, create_hring_tasks, *mocks):
        tasks = []
        for i in range(3):
            tasks += [
                SchedulersTask(id='b-task-{}'.format(i), type=SchedulersTask.Type.BETA),
                SchedulersTask(id='c-task-{}'.format(i), type=SchedulersTask.Type.COMPONENT),
                SchedulersTask(id='h-task-{}'.format(i), type=SchedulersTask.Type.HASHRING_COMPONENT),
            ]

        expected = {task.id: task for task in tasks if task.type == SchedulersTask.Type.HASHRING_COMPONENT}

        with session_scope() as session:
            task_utils.create_processor_tasks(tasks, session)
            create_hring_tasks.assert_called_with(expected, session)
