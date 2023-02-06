# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import typing  # noqa
import six
import uuid

if six.PY2:
    import mock
else:
    import unittest.mock as mock

from itertools import chain
from sqlalchemy import func

from infra.awacs.proto.model_pb2 import Upstream, UpstreamMeta

from search.martylib.core.date_utils import now, mock_now
from search.martylib.db_utils import prepare_db, clear_db, session_scope
from search.martylib.soy import SoyClientMock
from search.martylib.protobuf_utils.patch import patch_enums

from search.priemka.yappy.src.model.model_service.workers.gc import (
    AUTO_CREATED_TTL,
    BETA_TTL,
    FAILED_TTL,
    HISTORY_TTL,
    GarbageCollector,
    SB_TTL,
)
from search.priemka.yappy.src.yappy_lib.config_utils import get_test_config, DEFAULT_YAPPY_CONFIG
from search.priemka.yappy.src.yappy_lib.worker import BaseLockedWorker

from search.priemka.yappy.sqla.yappy import model
from search.priemka.yappy.proto.structures.auth_pb2 import AuthObject
from search.priemka.yappy.proto.structures.conf_pb2 import YappyConfiguration
from search.priemka.yappy.proto.structures.history_pb2 import History
from search.priemka.yappy.proto.structures.temporary_pb2 import TemporaryObjects

from search.priemka.yappy.tests.utils.test_cases import TestCase

patch_enums()


class TestGC(TestCase):
    @classmethod
    def setUpClass(cls):
        super(TestGC, cls).setUpClass()

        config = get_test_config()
        cls.gc_worker = GarbageCollector(config)
        cls.gc_worker.soy_client = SoyClientMock()

        # Trigger clients initialization and sync GC's and YP Manager's sets
        cls.gc_worker._clients = cls.gc_worker.yp_manager.clients

        cls._setUp_test_data()

    @classmethod
    def _setUp_test_data(cls):
        cls.test_data = {}

    def setUp(self):
        self._setUp_db()
        self._setUp_soy()
        self._setUp_yp()
        self._setUp_awacs()
        self._setUp_stoker()

    def tearDown(self):
        self._tearDown_db()
        self._tearDown_soy()
        self._tearDown_yp()
        self._tearDown_awacs()
        self._tearDown_stoker()

    def _setUp_db(self):
        clear_db()
        prepare_db()

    def _tearDown_db(self):
        clear_db()

    def _setUp_soy(self):
        pass

    def _tearDown_soy(self):
        self.gc_worker.soy_client.data = {}

    def _setUp_yp(self):
        pass

    def _tearDown_yp(self):
        self.gc_worker.yp_manager.clients.yp.reset_mock()

    def _setUp_awacs(self):
        pass

    def _tearDown_awacs(self):
        self.gc_worker.clients.awacs.reset_mock()

    def _setUp_stoker(self):
        pass

    def _tearDown_stoker(self):
        self.gc_worker.clients.stoker.reset_mock()


class TestGcDeallocateOrphanComponents(TestGC):
    @classmethod
    def _setUp_test_data(cls):
        super(TestGcDeallocateOrphanComponents, cls)._setUp_test_data()
        cls.test_data.update(
            {
                'orphan_components': [str(uuid.uuid4()), str(uuid.uuid4())],
            }
        )

    def _setUp_db(self):
        super(TestGcDeallocateOrphanComponents, self)._setUp_db()
        with session_scope() as session:
            for idx, component_id in enumerate(self.test_data['orphan_components']):
                session.add(
                    model.BetaComponent(
                        id=component_id,
                        slot=model.Slot(id='orphan_component_slot-{}'.format(idx)),
                    )
                )

    def test_deallocate_components_orphan(self):
        result = {}
        with session_scope() as session:
            q = (
                session
                    .query(func.count(model.Slot.id))
                    .filter(model.Slot.yappy__BetaComponent_id.in_(self.test_data['orphan_components']))
            )
            result['before'] = (q.scalar())
            self.gc_worker.deallocate_components()
            result['after'] = (q.scalar())
            session.rollback()
        n_components = len(self.test_data['orphan_components'])
        expected = {'before': n_components, 'after': 0}
        self.assertEqual(result, expected)


class TestGcFrozenComponents(TestGC):

    @classmethod
    def _setUp_test_data(cls):
        super(TestGcFrozenComponents, cls)._setUp_test_data()
        finished_states = ['ok', 'error']
        unfinished_states = cls.gc_worker.soy_client.UNFINISHED_BATCH_STATUSES
        cls.test_data.update(
            {
                'frozen_components': {
                    'batch_finished': {
                        str(uuid.uuid4()): {
                            'batch_id': 'batch-{}-1'.format(idx),
                            'state': finished_states[idx % len(finished_states)]
                        }
                        for idx in range(5)
                    },
                    'batch_unfinished': {
                        str(uuid.uuid4()): {
                            'batch_id': 'batch-{}-2'.format(idx),
                            'state': unfinished_states[idx % len(unfinished_states)]
                        }
                        for idx in range(6)
                    },
                    'no_batch': {
                        str(uuid.uuid4()): {}
                        for _ in range(10)
                    },
                },
            }
        )

    def _setUp_db(self):
        super(TestGcFrozenComponents, self)._setUp_db()
        with session_scope() as session:
            for set_name in self.test_data['frozen_components']:
                components = self.test_data['frozen_components'][set_name]
                for component_id in components:
                    session.add(
                        model.FrozenComponent(
                            component_id=component_id,
                            batch_id=components[component_id].get('batch_id', ''),
                        )
                    )

    def _setUp_soy(self):
        for set_name in self.test_data['frozen_components']:
            components = self.test_data['frozen_components'][set_name]
            for component_id in components:
                batch = components[component_id]
                if batch:
                    self.gc_worker.soy_client.load_batch(batch['batch_id'], batch['state'])

    def test_clear_frozen_components(self):
        components = self.test_data['frozen_components']
        n_finished = len(components['batch_finished'])
        n_others = len(components['batch_unfinished']) + len(components['no_batch'])
        expected = {'before': n_finished + n_others, 'after': n_others}
        result = {}
        with session_scope() as session:
            q = session.query(func.count(model.FrozenComponent.component_id))
            result['before'] = q.scalar()
            self.gc_worker.clear_frozen_components()
            result['after'] = q.scalar()
        self.assertEqual(result, expected)

    def test_clear_frozen_components_x2(self):
        with session_scope() as session:
            q = session.query(func.count(model.FrozenComponent.component_id))
            self.gc_worker.clear_frozen_components()
            result_x1 = q.scalar()
            self.gc_worker.clear_frozen_components()
            result_x2 = q.scalar()
        self.assertEqual(result_x1, result_x2)


class TestGcAuthObjects(TestGC):

    @classmethod
    def _setUp_test_data(cls):
        super(TestGcAuthObjects, cls)._setUp_test_data()
        auth_objects = {}
        n_objects = 5
        for type_name in AuthObject.Type.keys():
            auth_objects[type_name] = [
                '{}-{}'.format(type_name.lower(), obj_id)
                for obj_id in range(AuthObject.Type[type_name] + n_objects)
            ]
        cls.test_data.update({'auth_objects': auth_objects})
        name = '{}-{{}}'.format(AuthObject.Type[AuthObject.Type.BETA_TEMPLATE].lower())
        cls.test_data['templates'] = [name.format(idx) for idx in range(1, 3)] + ['no_auth_template']
        name = '{}-{{}}'.format(AuthObject.Type[AuthObject.Type.BETA].lower())
        cls.test_data['betas'] = [name.format(idx) for idx in range(1)] + ['no_auth_beta']
        name = '{}-{{}}'.format(AuthObject.Type[AuthObject.Type.QUOTA].lower())
        cls.test_data['quotas'] = [name.format(idx) for idx in range(2, 6)] + ['no_auth_quota']
        name = '{}-{{}}'.format(AuthObject.Type[AuthObject.Type.COMPONENT_TYPE].lower())
        cls.test_data['component_types'] = [name.format(idx) for idx in range(3)] + ['no_auth_component_type']

    def _setUp_db(self):
        super(TestGcAuthObjects, self)._setUp_db()
        with session_scope() as session:
            for auth_type in self.test_data['auth_objects']:
                session.add_all(
                    [
                        model.AuthObject(type=auth_type, name=obj_name)
                        for obj_name in self.test_data['auth_objects'][auth_type]
                    ]
                )
            session.add_all([model.BetaTemplate(name=name) for name in self.test_data['templates']])
            session.add_all([model.Beta(name=name) for name in self.test_data['betas']])
            session.add_all([model.Quota(name=name) for name in self.test_data['quotas']])
            session.add_all([model.ComponentType(name=name) for name in self.test_data['component_types']])

    def test_clear_auth_objects_unbound(self):
        type = AuthObject.Type[AuthObject.Type.UNBOUND]
        n_obj = len(self.test_data['auth_objects'][type])
        expected = {'before': n_obj, 'after': n_obj}
        result = {}
        with session_scope() as session:
            q = session.query(func.count(1)).filter(model.AuthObject.type == type)
            result['before'] = q.scalar()
            self.gc_worker.clear_auth_objects()
            result['after'] = q.scalar()
        self.assertEqual(result, expected)

    def test_clear_auth_objects_betas(self):
        type = AuthObject.Type[AuthObject.Type.BETA]
        n_obj = len(self.test_data['auth_objects'][type])
        n_related = len(self.test_data['betas']) - 1
        expected = {'before': n_obj, 'after': n_related}
        result = {}
        with session_scope() as session:
            q = session.query(func.count(1)).filter(model.AuthObject.type == type)
            result['before'] = q.scalar()
            self.gc_worker.clear_auth_objects()
            result['after'] = q.scalar()
        self.assertEqual(result, expected)

    def test_clear_auth_objects_quotas(self):
        type = AuthObject.Type[AuthObject.Type.QUOTA]
        n_obj = len(self.test_data['auth_objects'][type])
        n_related = len(self.test_data['quotas']) - 1
        expected = {'before': n_obj, 'after': n_related}
        result = {}
        with session_scope() as session:
            q = session.query(func.count(1)).filter(model.AuthObject.type == type)
            result['before'] = q.scalar()
            self.gc_worker.clear_auth_objects()
            result['after'] = q.scalar()
        self.assertEqual(result, expected)

    def test_clear_auth_objects_component_types(self):
        type = AuthObject.Type[AuthObject.Type.COMPONENT_TYPE]
        n_obj = len(self.test_data['auth_objects'][type])
        n_related = len(self.test_data['component_types']) - 1
        expected = {'before': n_obj, 'after': n_related}
        result = {}
        with session_scope() as session:
            q = session.query(func.count(1)).filter(model.AuthObject.type == type)
            result['before'] = q.scalar()
            self.gc_worker.clear_auth_objects()
            result['after'] = q.scalar()
        self.assertEqual(result, expected)

    def test_clear_auth_objects_templates(self):
        type = AuthObject.Type[AuthObject.Type.BETA_TEMPLATE]
        n_obj = len(self.test_data['auth_objects'][type])
        n_related = len(self.test_data['templates']) - 1
        expected = {'before': n_obj, 'after': n_related}
        result = {}
        with session_scope() as session:
            q = session.query(func.count(1)).filter(model.AuthObject.type == type)
            result['before'] = q.scalar()
            self.gc_worker.clear_auth_objects()
            result['after'] = q.scalar()
        self.assertEqual(result, expected)


class TestGcBrokenSlots(TestGC):
    @classmethod
    def _setUp_test_data(cls):
        super(TestGcBrokenSlots, cls)._setUp_test_data()
        _now = now().timestamp()
        step = 180
        cls.test_data['broken_slots'] = {
            'slot-{}'.format(idx): {'time': _now - step * idx}
            for idx in range(10)
        }
        cls.test_data['broken_slots_step_sec'] = step
        cls.test_data['now'] = _now

    def _setUp_db(self):
        super(TestGcBrokenSlots, self)._setUp_db()
        with session_scope() as session:
            session.add_all(
                [
                    model.BrokenSlot(
                        id=slot_id,
                        time=self.test_data['broken_slots'][slot_id]['time']
                    )
                    for slot_id in self.test_data['broken_slots']
                ]
            )

    def test_clear_broken_slots(self):
        with mock_now(self.test_data['now']):
            _now = now().timestamp()
            n_slots = len(self.test_data['broken_slots'])
            n_slots_survive = len([
                slot
                for slot in self.test_data['broken_slots'].values()
                if slot['time'] >= _now - self.gc_worker.BROKEN_SLOT_TTL
            ])
            expected = {'before': n_slots, 'after': n_slots_survive}
            result = {}
            with session_scope() as session:
                q = session.query(func.count(model.BrokenSlot.id))
                result['before'] = q.scalar()
                self.gc_worker.clear_broken_slots()
                result['after'] = q.scalar()
            self.assertEqual(result, expected)

    def test_clear_broken_slots_x2(self):
        with mock_now(self.test_data['now']):
            with session_scope() as session:
                q = session.query(func.count(model.BrokenSlot.id))
                self.gc_worker.clear_broken_slots()
                result_x1 = q.scalar()
                self.gc_worker.clear_broken_slots()
                result_x2 = q.scalar()
            self.assertEqual(result_x1, result_x2)


class TestGcAwacs(TestGC):

    @classmethod
    def _setUp_test_data(cls):
        super(TestGcAwacs, cls)._setUp_test_data()
        cls.test_data['awacs_records'] = {
            'awacs-record-{}'.format(i): 'beta-{}'.format(i)
            for i in range(10)
        }
        betas = sorted(cls.test_data['awacs_records'].values())
        cls.test_data['betas'] = betas[:3] + ['no_awacs_rec_beta']
        cls.test_data['allocated_betas'] = betas[3:7] + ['no_awacs_rec_allocated_beta']

    def _setUp_db(self):
        super(TestGcAwacs, self)._setUp_db()
        with session_scope() as session:
            for record_id, related_id in self.test_data['awacs_records'].items():
                session.add(
                    model.TemporaryObjects(
                        id=record_id,
                        type=TemporaryObjects.Type[TemporaryObjects.Type.AWACS],
                        related_id=related_id,
                        namespace_id='namespace',
                    )
                )
            for beta in self.test_data['betas']:
                session.add(model.Beta(name=beta))
            for beta in self.test_data['allocated_betas']:
                session.add(model.Beta(name=beta, allocated=True))

    def _setUp_awacs(self):
        self.gc_worker.clients.awacs.upstreams._load_data(
            [
                Upstream(meta=UpstreamMeta(id=record, namespace_id='namespace'))
                for record in self.test_data['awacs_records']
            ]
        )

    def test_clean_upstreams_db_records(self):
        with session_scope() as session:
            self.gc_worker.clean_upstreams()
            records = (
                session
                .query(model.TemporaryObjects.id)
                .filter(model.TemporaryObjects.type == TemporaryObjects.Type[TemporaryObjects.Type.AWACS])
                .order_by(model.TemporaryObjects.related_id)
                .all()
            )
            result = [record.id for record in records]
        expected = ['awacs-record-{}'.format(i) for i in range(7)]
        self.assertEqual(result, expected)

    def test_clean_upstreams_client_calls(self):
        self.gc_worker.clean_upstreams()
        result = sorted([request.id for request in self.gc_worker.clients.awacs.upstreams._list_remove_attempts()])
        expected = ['awacs-record-{}'.format(i) for i in range(7, 10)]
        self.assertEqual(result, expected)


class TestGcStoker(TestGC):

    @classmethod
    def _setUp_test_data(cls):
        super(TestGcStoker, cls)._setUp_test_data()
        stoker_objects = {}
        for i in range(10):
            stoker_objects['stoker-record-{}'.format(i)] = 'beta-{}'.format(i)
        cls.test_data['stoker_records'] = stoker_objects
        betas = sorted(stoker_objects.values())
        cls.test_data['betas'] = betas[:3] + ['no_stoker_rec_beta']
        cls.test_data['allocated_betas'] = betas[3:5] + ['no_stoker_rec_beta_allocated']

    def _setUp_db(self):
        super(TestGcStoker, self)._setUp_db()
        with session_scope() as session:
            for record_id, related_id in self.test_data['stoker_records'].items():
                session.add(
                    model.TemporaryObjects(
                        id=record_id,
                        type=TemporaryObjects.Type[TemporaryObjects.Type.STOKER],
                        related_id=related_id,
                    )
                )
            for beta in self.test_data['betas']:
                session.add(model.Beta(name=beta))
            for beta in self.test_data['allocated_betas']:
                session.add(model.Beta(name=beta, allocated=True))

    def test_clean_stoker_db_records(self):
        self.gc_worker.clean_stoker()
        with session_scope() as session:
            records = (
                session
                .query(model.TemporaryObjects.id)
                .filter(model.TemporaryObjects.type == TemporaryObjects.Type[TemporaryObjects.Type.STOKER])
                .order_by(model.TemporaryObjects.related_id)
                .all()
            )
            result = [record.id for record in records]
        expected = ['stoker-record-{}'.format(i) for i in range(3, 5)]
        self.assertEqual(result, expected)

    def test_clean_stoker_client_calls(self):
        self.gc_worker.clean_stoker()
        result = sorted(self.gc_worker.clients.stoker._list_remove_attempts())
        expected = ['stoker-record-{}'.format(i) for i in range(10) if i not in range(3, 5)]
        self.assertEqual(result, expected)


class TestGcRunOnce(TestGC):

    @classmethod
    def _setUp_test_data(cls):
        super(TestGcRunOnce, cls)._setUp_test_data()
        _now = now().timestamp()
        cls.test_data.update(
            {
                '_now': _now,
                'sb_tasks_obsolete': {
                    'sb-task-obsolete-{}'.format(i): {'cached_at': int(_now - (SB_TTL + i) * 3600) - 1}
                    for i in range(5)
                },
                'sb_tasks': {
                    'sb-task-{}'.format(i): {'cached_at': int(_now - (SB_TTL - i - 1) * 3600)}
                    for i in range(5)
                },
                'sb_files_obsolete': {
                    'sb-file-obsolete-{}'.format(i): {'cached_at': int(_now - (SB_TTL + i) * 3600) - 1}
                    for i in range(5)
                },
                'sb_files': {
                    'sb-file-{}'.format(i): {'cached_at': int(_now - (SB_TTL - i - 1) * 3600)}
                    for i in range(5)
                },
                'failed_lineage_history': [
                    {
                        'related_id': 'lineage-object',
                        'revision': int(1000 * (_now - (FAILED_TTL + i - 2) * 3600)),
                        'type': History.Type[History.Type.LINEAGE2],
                    }
                    for i in range(5)
                ],
                'failed_history': [
                    {
                        'related_id': 'some-object',
                        'revision': int(1000 * (_now - (FAILED_TTL + i - 2) * 3600)),
                    }
                    for i in range(5)
                ],
                'lineage_history': [
                    {
                        'related_id': 'lineage-object',
                        'revision': int(1000 * (_now - (HISTORY_TTL + i - 2) * 3600) + 1),
                        'type': History.Type[History.Type.LINEAGE2],
                        'success': True,
                    }
                    for i in range(5)
                ],
                'history': [
                    {
                        'related_id': 'some-object',
                        'revision': int(1000 * (_now - (HISTORY_TTL + i - 2) * 3600) + 1),
                        'success': True,
                    }
                    for i in range(5)
                ],
                'failed_events': [
                    {
                        'related_id': 'some-object',
                        'revision': int(1000 * (_now - (FAILED_TTL + i - 2) * 3600)),
                    }
                    for i in range(5)
                ],
                'events': [
                    {
                        'related_id': 'some-object',
                        'revision': int(1000 * (_now - (FAILED_TTL + i - 2) * 3600) + 1),
                        'success': True,
                    }
                    for i in range(5)
                ],
                'used_components': {str(uuid.uuid4()): 'beta-{}'.format(i) for i in range(10)},
                'unused_components': {str(uuid.uuid4()): None for i in range(10)},
                'betas': {
                    'maybe-expired-beta-{}'.format(i): {'expires': _now - (BETA_TTL + i - 2) * 3600}
                    for i in range(5)
                },
                'allocated_betas': {
                    'allocated-beta-{}'.format(i): {
                        'allocated': True,
                        'expires': _now - (BETA_TTL + i - 2) * 3600
                    }
                    for i in range(2)
                },
                'preallocated_betas': {
                    'preallocated-beta-{}'.format(i): {'expires': 0}
                    for i in range(2)
                },
            }
        )

    def _setUp_db(self):
        super(TestGcRunOnce, self)._setUp_db()
        data = self.test_data
        with session_scope() as session:
            for sb_task_type in ('sb_tasks', 'sb_tasks_obsolete'):
                session.add_all([model.SandboxTask(id=_id, **params) for _id, params in data[sb_task_type].items()])
            for sb_file_type in ('sb_files', 'sb_files_obsolete'):
                session.add_all([model.SandboxFile(id=_id, **params) for _id, params in data[sb_file_type].items()])
            for history_type in ('failed_lineage_history', 'failed_history', 'lineage_history', 'history'):
                session.add_all([model.History(**params) for params in data[history_type]])
            for events_type in ('failed_events', 'events'):
                session.add_all([model.Event(**params) for params in data[events_type]])
            for c_type in ('used_components', 'unused_components'):
                session.add_all(
                    [
                        model.BetaComponent(
                            id=_id,
                            **({'beta': [model.Beta(name=beta)]} if beta is not None else {})
                        )
                        for _id, beta in data[c_type].items()
                    ]
                )
            for beta_type in ('betas', 'allocated_betas', 'preallocated_betas'):
                session.add_all([model.Beta(name=_id, **params) for _id, params in data[beta_type].items()])

    def test_run_once_sb_tasks(self):
        with session_scope() as session, mock_now(self.test_data['_now']):
            self.gc_worker.run_once(session)
            result = [r.id for r in session.query(model.SandboxTask.id).order_by(model.SandboxTask.id).all()]
        expected = ['sb-task-{}'.format(i) for i in range(5)]
        self.assertEqual(result, expected)

    def test_run_once_sb_files(self):
        with session_scope() as session, mock_now(self.test_data['_now']):
            self.gc_worker.run_once(session)
            result = [r.id for r in session.query(model.SandboxFile.id).order_by(model.SandboxFile.id).all()]
        expected = ['sb-file-{}'.format(i) for i in range(5)]
        self.assertEqual(result, expected)

    def test_run_once_any_failed_history(self):
        with session_scope() as session, mock_now(self.test_data['_now']):
            self.gc_worker.run_once(session)
            result = (
                session.query(model.History.related_id, model.History.revision)
                .filter(model.History.success.is_(False))
                .order_by(model.History.related_id, model.History.revision)
                .all()
            )
        expected = sorted(
            [
                (r['related_id'], r['revision'])
                for r in (self.test_data['failed_lineage_history'] + self.test_data['failed_history'])
                if r['revision'] >= int((self.test_data['_now'] - FAILED_TTL * 3600) * 1000)
            ]
        )
        self.assertEqual(result, expected)

    def test_run_once_any_success_history(self):
        with session_scope() as session, mock_now(self.test_data['_now']):
            self.gc_worker.run_once(session)
            result = (
                session.query(model.History.related_id, model.History.revision)
                .filter(model.History.success.is_(True))
                .order_by(model.History.related_id, model.History.revision)
                .all()
            )
        expected = sorted(
            [
                (r['related_id'], r['revision']) for r in self.test_data['lineage_history']
            ] +
            [
                (r['related_id'], r['revision'])
                for r in self.test_data['history']
                if r['revision'] >= int((self.test_data['_now'] - HISTORY_TTL * 3600) * 1000)
            ]
        )
        self.assertEqual(result, expected)

    def test_failed_events(self):
        with session_scope() as session, mock_now(self.test_data['_now']):
            self.gc_worker.run_once(session)
            result = (
                session.query(model.Event.related_id, model.Event.revision)
                .filter(model.Event.success.is_(False))
                .order_by(model.Event.related_id, model.Event.revision)
                .all()
            )
        expected = sorted(
            [
                (r['related_id'], r['revision'])
                for r in (self.test_data['failed_events'])
                if r['revision'] >= int((self.test_data['_now'] - FAILED_TTL * 3600) * 1000)
            ]
        )
        self.assertEqual(result, expected)

    def test_success_events(self):
        with session_scope() as session, mock_now(self.test_data['_now']):
            self.gc_worker.run_once(session)
            result = (
                session.query(model.Event.related_id, model.Event.revision)
                .filter(model.Event.success.is_(True))
                .order_by(model.Event.related_id, model.Event.revision)
                .all()
            )
        expected = sorted([(r['related_id'], r['revision']) for r in (self.test_data['events'])])
        self.assertEqual(result, expected)

    def test_components(self):
        with session_scope() as session:
            self.gc_worker.run_once(session)
            result = [str(r.id) for r in session.query(model.BetaComponent.id).order_by(model.BetaComponent.id).all()]
            expected = sorted(self.test_data['used_components'].keys())
        self.assertEqual(result, expected)

    def test_betas(self):
        with session_scope() as session, mock_now(self.test_data['_now']):
            self.gc_worker.run_once(session)
            result = [str(r.name) for r in session.query(model.Beta.name).order_by(model.Beta.name).all()]
            expected = sorted(
                list(self.test_data['allocated_betas'].keys()) +
                list(self.test_data['preallocated_betas']) +
                [
                    beta
                    for beta, params in self.test_data['betas'].items()
                    if params['expires'] >= self.test_data['_now'] - BETA_TTL * 3600
                ] +
                list(self.test_data['used_components'].values())
            )
        self.assertEqual(result, expected)


class TestGcAutoCreated(TestGC):
    @classmethod
    def _setUp_test_data(cls):
        super(TestGcAutoCreated, cls)._setUp_test_data()
        _now = now().timestamp()
        N_ITEMS = 5
        cls.test_data.update(
            {
                '_now': _now,
                'items': {
                    'auto_created_obsolete': {
                        'auto-created-old-{}'.format(i): {
                            'last_update': _now - (AUTO_CREATED_TTL + i * 3600) - 1,
                            'auto_created': True,
                        }
                        for i in range(N_ITEMS)
                    },
                    'regular_obsolete': {
                        'regular-old-{}'.format(i): {'last_update': _now - (AUTO_CREATED_TTL + i * 3600) - 1}
                        for i in range(N_ITEMS)
                    },
                    'auto_created': {
                        'auto-created-{}'.format(i): {
                            'last_update': _now - (AUTO_CREATED_TTL - i * 3600),
                            'auto_created': True,
                        }
                        for i in range(N_ITEMS)
                    },
                    'regular': {
                        'regular-{}'.format(i): {'last_update': _now - (AUTO_CREATED_TTL - i * 3600)}
                        for i in range(N_ITEMS)
                    },
                },
                'items_in_use': [
                    'auto-created-old-0',
                    'auto-created-old-{}'.format(N_ITEMS - 1),
                    'auto-created-0',
                    'auto-created-{}'.format(N_ITEMS - 1),
                    'regular-old-0',
                    'regular-old-{}'.format(N_ITEMS - 1),
                    'regular-0',
                    'regular-{}'.format(N_ITEMS - 1),
                ],
                'quotas_with_slots': ['auto-created-old-2'],
            }
        )

    def _setUp_db(self):
        super(TestGcAutoCreated, self)._setUp_db()
        with session_scope() as session:
            for items in self.test_data['items'].values():
                session.add_all([
                    model.Quota(
                        name='quota-{}'.format(key),
                        beta_component=[model.BetaComponent()] if key in self.test_data['items_in_use'] else [],
                        slots=(
                            [model.Slot(id='slot-{}'.format(key))]
                            if key in self.test_data['quotas_with_slots']
                            else []
                        ),
                        **params
                    )
                    for key, params in items.items()
                ])
                session.add_all([
                    model.ComponentType(
                        name='type-{}'.format(key),
                        beta_component=[model.BetaComponent()] if key in self.test_data['items_in_use'] else [],
                        **params
                    )
                    for key, params in items.items()
                ])

    def test_clear_component_types_auto(self):
        with session_scope() as session, mock_now(self.test_data['_now']):
            self.gc_worker.clear_component_types()
            result = (
                session
                .query(model.ComponentType.name)
                .filter(model.ComponentType.auto_created.is_(True))
                .order_by(model.ComponentType.name)
                .all()
            )
            result = [r.name for r in result]
        expected_items = (
            [
                item
                for item in self.test_data['items']['auto_created_obsolete'].keys()
                if item in self.test_data['items_in_use']
            ] +
            list(self.test_data['items']['auto_created'].keys())
        )
        expected = sorted(['type-{}'.format(item) for item in expected_items])
        self.assertEqual(result, expected)

    def test_clear_quotas_auto(self):
        with session_scope() as session, mock_now(self.test_data['_now']):
            self.gc_worker.clear_quotas()
            result = (
                session
                .query(model.Quota.name)
                .filter(model.Quota.auto_created.is_(True))
                .order_by(model.Quota.name)
                .all()
            )
            result = [r.name for r in result]
        expected_items = (
            [
                item
                for item in self.test_data['items']['auto_created_obsolete'].keys()
                if item in self.test_data['items_in_use'] or item in self.test_data['quotas_with_slots']
            ] +
            list(self.test_data['items']['auto_created'].keys())
        )
        expected = sorted(['quota-{}'.format(item) for item in expected_items])
        self.assertEqual(result, expected)

    def test_clear_component_types_regular(self):
        with session_scope() as session, mock_now(self.test_data['_now']):
            self.gc_worker.clear_component_types()
            result = (
                session
                .query(model.ComponentType.name)
                .filter(model.ComponentType.auto_created.is_(False))
                .order_by(model.ComponentType.name)
                .all()
            )
            result = [r.name for r in result]
        expected = sorted([
            'type-{}'.format(item)
            for item in chain(
                self.test_data['items']['regular_obsolete'].keys(),
                self.test_data['items']['regular'].keys(),
            )
        ])
        self.assertEqual(result, expected)

    def test_clear_quotas_regular(self):
        with session_scope() as session, mock_now(self.test_data['_now']):
            self.gc_worker.clear_quotas()
            result = (
                session
                .query(model.Quota.name)
                .filter(model.Quota.auto_created.is_(False))
                .order_by(model.Quota.name)
                .all()
            )
            result = [r.name for r in result]
        expected = sorted([
            'quota-{}'.format(item)
            for item in chain(
                self.test_data['items']['regular_obsolete'].keys(),
                self.test_data['items']['regular'].keys(),
            )
        ])
        self.assertEqual(result, expected)


class TestGcConfig(TestCase):

    @mock.patch('concurrent.futures.ThreadPoolExecutor')
    def test_sleep_interval_default(self, *mocked):
        gc = GarbageCollector()
        sleep_interval = gc.sleep_interval
        expected = DEFAULT_YAPPY_CONFIG.model.gc.sleep_interval

        self.assertEqual(sleep_interval, expected)

    @mock.patch('concurrent.futures.ThreadPoolExecutor')
    @mock.patch('search.priemka.yappy.src.model.model_service.workers.gc.get_config')
    def test_sleep_interval_base(self, mocked_config, *mocked):
        mocked_config.return_value = YappyConfiguration()
        mocked_config.return_value.model.gc.sleep_interval = 0

        gc = GarbageCollector()
        sleep_interval = gc.sleep_interval
        expected = BaseLockedWorker.SLEEP_INTERVAL

        self.assertEqual(sleep_interval, expected)

    @mock.patch('concurrent.futures.ThreadPoolExecutor')
    @mock.patch('search.priemka.yappy.src.model.model_service.workers.gc.get_config')
    def test_sleep_interval_config(self, mocked_config, *mocked):
        mocked_config.return_value = YappyConfiguration()
        mocked_config.return_value.model.gc.sleep_interval = 1

        gc = GarbageCollector()
        sleep_interval = gc.sleep_interval
        expected = 1

        self.assertEqual(sleep_interval, expected)

    @mock.patch('concurrent.futures.ThreadPoolExecutor')
    def test_sleep_interval_param(self, *mocked):
        gc = GarbageCollector(sleep_interval=1)
        sleep_interval = gc.sleep_interval
        expected = 1

        self.assertEqual(sleep_interval, expected)
