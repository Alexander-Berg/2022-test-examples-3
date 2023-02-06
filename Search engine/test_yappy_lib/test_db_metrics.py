# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from typing import List, Tuple, Optional, Union, Any   # noqa
import uuid

from search.martylib.core.date_utils import now, mock_now

from search.priemka.yappy.proto.structures.auth_pb2 import AuthObject
from search.priemka.yappy.proto.structures.history_pb2 import History
from search.priemka.yappy.proto.structures.temporary_pb2 import TemporaryObjects

from search.priemka.yappy.src.yappy_lib.utils import session_scope
from search.priemka.yappy.src.yappy_lib import db_metrics
from search.priemka.yappy.sqla.yappy import model

from search.priemka.yappy.tests.utils.test_cases import TestCaseWithDB


class DbMetricsTest(TestCaseWithDB):
    MIN_OBJECTS = 1

    ALLOCATED_BETAS = 4
    UNALLOCATED_BETAS = 5
    BROKEN_SLOTS = 6
    USED_COMPONENTS = 7
    UNUSED_COMPONENTS = 8
    EVENTS_SUCCESS = 9
    EVENTS_FAIL = 10
    FROZEN_BATCH = 11
    FROZEN_NO_BATCH = 12
    HISTORY_LINEAGE2_FAIL = 13
    HISTORY_LINEAGE2_SUCCESS = 14
    HISTORY_NOT_LINEAGE2_FAIL = 15
    HISTORY_NOT_LINEAGE2_SUCCESS = 16
    SB_TASKS = 17
    SB_FILES = 18
    CTYPES_AUTO = 19
    CTYPES_REGULAR = 20
    QUOTAS_AUTO = 21
    QUOTAS_REGULAR = 22
    SCHED_TASKS_IN_FLY = 23
    SCHED_TASKS_IN_SLEEP = 24
    SCHED_TASKS_WAITING = 25

    SCHED_TASKS_MAX_WAIT_TIME = 100
    SCHED_TASKS_AVG_WAIT_TIME = 80
    SCHED_SLEEP_TIME = 300

    @classmethod
    def create_test_data(cls):
        _now = int(now().timestamp())
        with session_scope() as session:
            for obj_type in AuthObject.Type.keys():
                session.add_all([
                    model.AuthObject(name='auth-obj-{}-{}'.format(obj_type, i), type=obj_type,)
                    for i in range(AuthObject.Type[obj_type] + cls.MIN_OBJECTS)
                ])
            for obj_type in TemporaryObjects.Type.keys():
                session.add_all([
                    model.TemporaryObjects(id='tmp-obj-{}-{}'.format(obj_type, i), type=obj_type,)
                    for i in range(TemporaryObjects.Type[obj_type] + cls.MIN_OBJECTS)
                ])
            session.add_all([model.BrokenSlot(id='broken-slot-{}'.format(i)) for i in range(cls.BROKEN_SLOTS)])
            session.add_all([
                model.Event(related_id='some-object-{}', revision=_now + i, success=(i < cls.EVENTS_SUCCESS))
                for i in range(cls.EVENTS_SUCCESS + cls.EVENTS_FAIL)
            ])
            session.add_all([
                model.FrozenComponent(
                    component_id='frozen-component-{}'.format(i),
                    batch_id='batch-{}'.format(i) if i < cls.FROZEN_BATCH else ''
                )
                for i in range(cls.FROZEN_BATCH + cls.FROZEN_NO_BATCH)
            ])
            session.add_all([
                model.History(
                    type=History.Type[History.Type.LINEAGE2],
                    success=(i < cls.HISTORY_LINEAGE2_SUCCESS),
                    revision=(_now + i)
                )
                for i in range(cls.HISTORY_LINEAGE2_SUCCESS + cls.HISTORY_LINEAGE2_FAIL)
            ])
            session.add_all([
                model.History(
                    type=History.Type[History.Type.UNKNOWN],
                    success=(i < cls.HISTORY_NOT_LINEAGE2_SUCCESS),
                    revision=(_now + i)
                )
                for i in range(cls.HISTORY_NOT_LINEAGE2_SUCCESS + cls.HISTORY_NOT_LINEAGE2_FAIL)
            ])
            session.add_all([
                model.SandboxFile(id='sb-file-{}'.format(i)) for i in range(cls.SB_FILES)
            ])
            session.add_all([
                model.SandboxTask(id='sb-task-{}'.format(i)) for i in range(cls.SB_TASKS)
            ])
            session.add_all([
                model.ComponentType(
                    name='ctype-{}'.format(i),
                    auto_created=(i < cls.CTYPES_AUTO)
                )
                for i in range(cls.CTYPES_AUTO + cls.CTYPES_REGULAR)
            ])
            session.add_all([
                model.Quota(
                    name='quota-{}'.format(i),
                    auto_created=(i < cls.QUOTAS_AUTO)
                )
                for i in range(cls.QUOTAS_AUTO + cls.QUOTAS_REGULAR)
            ])

    def create_betas(self, session):
        session.add_all([
            model.Beta(name='beta-name-{}'.format(i), allocated=(i < self.ALLOCATED_BETAS))
            for i in range(self.ALLOCATED_BETAS + self.UNALLOCATED_BETAS)
        ])

    def create_components(self, session):
        session.add_all([
            model.BetaComponent(
                id=str(uuid.uuid4()),
                **(
                    {'beta': [model.Beta(name='beta-with-component-{}'.format(i))]}
                    if i < self.USED_COMPONENTS
                    else {}
                )
            )
            for i in range(self.UNUSED_COMPONENTS + self.USED_COMPONENTS)
        ])

    def create_sched_tasks_by_status(self, session, _now=None):
        if _now is None:
            _now = now().timestamp()
        session.add_all([
            model.SchedulersTask(
                id='task-running-{}'.format(i),
                in_fly=(i < self.SCHED_TASKS_IN_FLY),
            )
            for i in range(self.SCHED_TASKS_IN_FLY)
        ])
        session.add_all([
            model.SchedulersTask(
                id='task-waiting-{}'.format(i),
                last_creation=(_now - self.SCHED_SLEEP_TIME + 5 * (1 if i < self.SCHED_TASKS_IN_SLEEP else -1)),
            )
            for i in range(self.SCHED_TASKS_IN_SLEEP + self.SCHED_TASKS_WAITING)
        ])

    def create_sched_tasks_waiting(self, session, _now=None):
        if _now is None:
            _now = now().timestamp()

        step = self.SCHED_TASKS_MAX_WAIT_TIME - self.SCHED_TASKS_AVG_WAIT_TIME

        session.add_all([
            model.SchedulersTask(
                id='task-waiting-{}'.format(i),
                last_creation=(_now - self.SCHED_TASKS_MAX_WAIT_TIME + i * step),
            )
            for i in range(3)
        ])

    def _test(self, get_metrics, expected, pre_start=None, **kwargs):
        # type: (callable, List[Tuple[Union[str, unicode], int]], Optional[callable], **Any) -> None
        now_ = now().timestamp()
        with session_scope() as session, mock_now(now_):
            if pre_start:
                pre_start(session)
            metrics = sorted(get_metrics(session, **kwargs))
            session.rollback()
        expected = [
            (metric.format(suffix=db_metrics.SIGNAL_SUFFIX, avg_suffix=db_metrics.AVG_SUFFIX), value)
            for metric, value in expected
        ]
        expected.sort()

        self.assertEqual(metrics, expected)

    def test_auth_object_metrics(self):
        expected = [
            ('db-auth_objects_{}_{{suffix}}'.format(AuthObject.Type[idx]), idx + self.MIN_OBJECTS)
            for idx in AuthObject.Type.values()
        ]
        self._test(db_metrics.auth_objects_metrics, expected)

    def test_tmp_object_metrics(self):
        expected = [
            ('db-temporary_objects_{}_{{suffix}}'.format(TemporaryObjects.Type[idx]), idx + self.MIN_OBJECTS)
            for idx in TemporaryObjects.Type.values()
        ]
        self._test(db_metrics.tmp_objects_metrics, expected)

    def test_betas_metrics(self):
        expected = [
            ('db-betas_allocated_{suffix}', self.ALLOCATED_BETAS),
            ('db-betas_not_allocated_{suffix}', self.UNALLOCATED_BETAS),
        ]
        self._test(db_metrics.beta_metrics, expected, self.create_betas)

    def test_broken_slots_metrics(self):
        expected = [('db-broken_slots_{suffix}', self.BROKEN_SLOTS)]
        self._test(db_metrics.broken_slots_metrics, expected)

    def test_ctype_metrics(self):
        expected = [
            ('db-component_type_auto_created_{suffix}', self.CTYPES_AUTO),
            ('db-component_type_regular_{suffix}', self.CTYPES_REGULAR),
        ]
        self._test(db_metrics.component_type_metrics, expected)

    def test_quota_metrics(self):
        expected = [
            ('db-quota_auto_created_{suffix}', self.QUOTAS_AUTO),
            ('db-quota_regular_{suffix}', self.QUOTAS_REGULAR),
        ]
        self._test(db_metrics.quota_metrics, expected)

    def test_event_metrics(self):
        expected = [
            ('db-events_success_{suffix}', self.EVENTS_SUCCESS),
            ('db-events_fail_{suffix}', self.EVENTS_FAIL),
        ]
        self._test(db_metrics.event_metrics, expected)

    def test_components_metrics(self):
        expected = [
            ('db-beta_components_unused_{suffix}', self.UNUSED_COMPONENTS),
            ('db-beta_components_used_{suffix}', self.USED_COMPONENTS),
        ]
        self._test(db_metrics.components_metrics, expected, self.create_components)

    def test_frozen_components(self):
        expected = [
            ('db-frozen_components_with_batch_{suffix}', self.FROZEN_BATCH),
            ('db-frozen_components_no_batch_{suffix}', self.FROZEN_NO_BATCH),
        ]
        self._test(db_metrics.frozen_components_metrics, expected)

    def test_history_metrics(self):
        expected = [
            ('db-history_lineage2_success_{suffix}', self.HISTORY_LINEAGE2_SUCCESS),
            ('db-history_lineage2_fail_{suffix}', self.HISTORY_LINEAGE2_FAIL),
            ('db-history_not_lineage2_success_{suffix}', self.HISTORY_NOT_LINEAGE2_SUCCESS),
            ('db-history_not_lineage2_fail_{suffix}', self.HISTORY_NOT_LINEAGE2_FAIL),
        ]
        self._test(db_metrics.history_metrics, expected)

    def test_sb_files_metrics(self):
        expected = [('db-sandbox_files_{suffix}', self.SB_FILES)]
        self._test(db_metrics.sb_files_metrics, expected)

    def test_sb_tasks_metrics(self):
        expected = [('db-sandbox_tasks_{suffix}', self.SB_TASKS)]
        self._test(db_metrics.sb_tasks_metrics, expected)

    def test_sched_tasks_metrics(self):
        expected = [
            ('db-schedulers_tasks_in_fly_{suffix}', self.SCHED_TASKS_IN_FLY),
            ('db-schedulers_tasks_in_sleep_{suffix}', self.SCHED_TASKS_IN_SLEEP),
            ('db-schedulers_tasks_waiting_{suffix}', self.SCHED_TASKS_WAITING),
        ]
        kwargs = {'sleep_interval': self.SCHED_SLEEP_TIME}
        self._test(db_metrics.scheduler_tasks_metrics, expected, pre_start=self.create_sched_tasks_by_status, **kwargs)

    def test_sched_tasks_wait_time_metrics(self):
        expected = [
            ('db-schedulers_tasks_wait_time_{suffix}', self.SCHED_TASKS_MAX_WAIT_TIME),
            ('db-schedulers_tasks_wait_time_{avg_suffix}', self.SCHED_TASKS_AVG_WAIT_TIME),
        ]
        self._test(db_metrics.scheduler_tasks_wait_time_metrics, expected, pre_start=self.create_sched_tasks_waiting)
