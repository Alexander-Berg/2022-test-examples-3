# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import copy
import six
import uuid

if six.PY2:
    import mock
    from contextlib2 import ExitStack
else:
    import unittest.mock as mock
    from contextlib import ExitStack

from google.protobuf.json_format import MessageToDict
from randomproto import randproto
from unittest import skipUnless

from search.martylib.core.date_utils import now, mock_now
from search.martylib.db_utils import to_model, clear_db, prepare_db, session_scope
from search.martylib.http.exceptions import NotFound
from search.martylib.protobuf_utils import replace_in_repeated

from search.martylib.proto.structures.yp_lite_pb2 import AllocationRequest

from search.priemka.yappy.src.model.model_service.workers.yp_manager import YPManager
from search.priemka.yappy.src.yappy_lib.config_utils import get_test_config, DEFAULT_YAPPY_CONFIG
from search.priemka.yappy.src.yappy_lib.nanny import DuplicateError
from search.priemka.yappy.proto.structures.conf_pb2 import YappyConfiguration
from search.priemka.yappy.proto.structures.beta_component_pb2 import BetaComponent
from search.priemka.yappy.proto.structures.quota_pb2 import Quota
from search.priemka.yappy.proto.structures.resources_pb2 import InstanceSpec, Container, CoredumpPolicy
from search.priemka.yappy.proto.structures.slot_pb2 import Slot, ClusterList, BrokenSlot
from search.priemka.yappy.proto.structures.yp_pb2 import YP
from search.priemka.yappy.sqla.yappy import model

from search.priemka.yappy.tests.utils.test_cases import TestCase


class TestYPManager(TestCase):

    @classmethod
    def setUpClass(cls):
        super(TestYPManager, cls).setUpClass()
        cls.yp_manager = YPManager(get_test_config())
        cls.nanny = cls.yp_manager.clients.nanny
        cls.yp = cls.yp_manager.clients.yp

        cls._setUp_test_data()

    def setUp(self):
        self._setUp_nanny()
        self._setUp_yp()
        self._setUp_db()

    def tearDown(self):
        self._tearDown_nanny()
        self._tearDown_yp()
        self._tearDown_db()

    @classmethod
    def _setUp_test_data(cls):
        cls.test_data = {}

    def _setUp_nanny(self):
        self.nanny.load_services(copy.deepcopy(self.test_data.get('services', {})))

    def _tearDown_nanny(self):
        self.nanny.data = {}
        self.nanny.reset_events()

    def _setUp_yp(self):
        for slot_id, spec in self.test_data.get('services', {}).items():
            if '_yp' not in spec:
                continue
            spec = spec['_yp']
            for cluster in spec:
                self.yp.create_pod_set(AllocationRequest(replicas=spec[cluster]), slot_id, cluster)

    def _tearDown_yp(self):
        self.yp.reset_mock()

    def _setUp_db(self):
        self._no_db = True

    def _tearDown_db(self):
        if not getattr(self, '_no_db', False):
            clear_db()

    def _slot_by_id(self, slot_id):
        data = self.test_data.get('services', {}).get(slot_id)
        if not data:
            slot = Slot(id=slot_id)
        else:
            slot = Slot(
                id=slot_id,
                is_yp=True,
                clusters=ClusterList(objects=data.get('_yp', {}).keys())
            )
        return slot


class TestYPManagerExpiredSlots(TestYPManager):

    @classmethod
    def _setUp_test_data(cls):
        _now = now().timestamp()
        cls.test_data = {
            'now': _now,
            'slots': {
                'slot-in-use': Slot(),
                'slot-just-deallocated': Slot(
                    last_deallocate=(_now - 0.1 * cls.yp_manager.config.model.allocator.yp_slot_expires),
                ),
                'slot-just-deallocated-but-linked': Slot(
                    last_deallocate=(_now - 0.1 * cls.yp_manager.config.model.allocator.yp_slot_expires),
                ),
                'slot-not-deallocated': Slot(
                    last_deallocate=0,
                ),
                'slot-just-created': Slot(
                    last_deallocate=cls.yp_manager._new_slot_last_deallocate_value(_now),
                ),
                'slot-just-created-and-allocated': Slot(
                    last_deallocate=cls.yp_manager._new_slot_last_deallocate_value(_now),
                ),
                'slot-long-deallocated': Slot(
                    last_deallocate=(_now - 2 * cls.yp_manager.config.model.allocator.yp_slot_expires),
                ),
            },
            'quotas': {
                'permanent-quota': Quota(),
                'tmp-quota': Quota(tmp_quota=True),
            },
            'slots-with-components': [
                'slot-in-use',
                'slot-just-deallocated-but-linked',
                'slot-just-created-and-allocated',
            ],
        }

    def _setUp_db(self):
        prepare_db()
        with session_scope() as session:
            for quota_name, quota in self.test_data.get('quotas', {}).items():
                quota.name = quota_name
                session.add(to_model(quota))
                for slot_id, slot in self.test_data.get('slots', {}).items():
                    slot.quota_name = quota_name

                    slot.id = '{}-in-{}'.format(slot_id, quota_name)
                    slot.is_yp = False
                    model_slot = to_model(slot)
                    if slot_id in self.test_data.get('slots-with-components', []):
                        model_slot.beta_component = model.BetaComponent(id=str(uuid.uuid4()))
                    session.add(model_slot)

                    slot.id = 'yp-{}'.format(slot.id)
                    slot.is_yp = True
                    model_slot = to_model(slot)
                    if slot_id in self.test_data.get('slots-with-components', []):
                        model_slot.beta_component = model.BetaComponent(id=str(uuid.uuid4()))
                    session.add(model_slot)

    def _test_get_expired_slots(self):
        with session_scope() as session:
            with mock_now(self.test_data['now']):
                response = self.yp_manager.get_expired_slots(session)
                result = [slot.id for slot in response]

        expected = [
            'yp-slot-long-deallocated-in-permanent-quota',
            'yp-slot-long-deallocated-in-tmp-quota',
            'yp-slot-just-deallocated-in-tmp-quota',
        ]
        return expected, result

    @skipUnless(six.PY2, 'py2 only')
    def test_get_expired_slots_py2(self):
        self.assertItemsEqual(*self._test_get_expired_slots())

    @skipUnless(six.PY3, 'py3 only')
    def test_get_expired_slots_py3(self):
        self.assertCountEqual(*self._test_get_expired_slots())

    def test_run_delete_slots_not_fails(self):
        with \
                session_scope() as session, \
                mock.patch.object(self.yp_manager.allocator_lock, 'acquire'), \
                mock.patch.object(self.yp_manager.allocator_lock, 'release'):
            self.yp_manager.run_delete_slots(session, self.test_data['now'])


class TestYPManagerDeleteSlot(TestYPManager):

    @classmethod
    def _setUp_test_data(cls):
        cls.test_data = {
            'services': {
                'slot-with-active-snapshots': {
                    'current_state': {
                        'content': {
                            'active_snapshots': [{'snapshot_id': 'active-snapshot-id'}],
                        },
                    },
                },
                'slot-in-single-dc': {
                    'current_state': {'content': {'active_snapshots': []}},
                    '_yp': {
                        YP.ClusterType.keys()[0]: 2,
                    },
                },
                'slot-in-all-dc': {
                    'current_state': {'content': {'active_snapshots': []}},
                    '_yp': {
                        cluster: 2 for cluster in YP.ClusterType.keys()
                    },
                },
                'slot-with-auth': {
                    'current_state': {'content': {'active_snapshots': [{'snapshot_id': 'active-snapshot-id'}]}},
                    'auth_attrs': {
                        'content': {
                            'owners': {'logins': ['owner-user']},
                            'ops_managers': {'logins': ['ops-manager']},
                        },
                    },
                },
            },
        }

    def setUp(self):
        super(TestYPManagerDeleteSlot, self).setUp()
        self.session_scope_patch = mock.patch('{}.session_scope'.format(self.yp_manager.__class__.__module__))
        self.session_scope = self.session_scope_patch.start()
        self.addCleanup(self.session_scope_patch.stop)
        self.session = self.session_scope().__enter__()

    def test_delete_slot_nanny_not_found(self):
        slot = self._slot_by_id('no_such_service')
        result = self.yp_manager._delete_slot_external(slot)
        self.assertEqual(result, True)

    def test_delete_slot_nanny(self):
        slot_id = 'slot-in-single-dc'
        slot = self._slot_by_id(slot_id)
        self.yp_manager._delete_slot_external(slot)
        self.assertRaises(
            NotFound,
            self.nanny.get_current_state,
            slot_id,
        )

    def test_delete_slot_with_active_snapshots(self):
        slot_id = 'slot-with-active-snapshots'
        slot = self._slot_by_id(slot_id)
        deleted = self.yp_manager._delete_slot_external(slot)
        nanny_result = self.nanny.get_current_state(slot_id)
        result = {
            'deleted': deleted,
            'nanny_service': {
                'summary': nanny_result['content']['summary']['value'],
                'snapshots': nanny_result['content']['active_snapshots'],
            },
        }
        expected = {
            'deleted': False,
            'nanny_service': {
                'summary': 'OFFLINE',
                'snapshots': [],
            },
        }
        self.assertEqual(result, expected)

    def test_delete_slot_with_active_snapshots_x2(self):
        slot_id = 'slot-with-active-snapshots'
        slot = self._slot_by_id(slot_id)
        self.yp_manager._delete_slot_external(slot)
        deleted = self.yp_manager._delete_slot_external(slot)
        self.assertEqual(deleted, True)

    def test_delete_slot_single_dc(self):
        slot_id = 'slot-in-single-dc'
        slot = self._slot_by_id(slot_id)
        deleted = self.yp_manager._delete_slot_external(slot)
        yp_result = self.yp.mock_data[slot.clusters.objects[0]].get(slot_id)
        result = {'deleted': deleted, 'yp_pods': yp_result}
        expected = {'deleted': True, 'yp_pods': None}
        self.assertEqual(result, expected)

    def test_delete_slot_all_dc(self):
        slot_id = 'slot-in-all-dc'
        slot = self._slot_by_id(slot_id)
        deleted = self.yp_manager._delete_slot_external(slot)
        yp_data = self.yp.mock_data
        yp_result = {
            cluster: yp_data[cluster].get(slot_id) for cluster in YP.ClusterType.keys()
        }
        result = {'deleted': deleted, 'yp_pods': yp_result}
        expected = {'deleted': True, 'yp_pods': {cluster: None for cluster in YP.ClusterType.keys()}}
        self.assertEqual(result, expected)

    def test_delete_slot_wrong_dc(self):
        slot_id = 'slot-in-all-dc'
        slot = self._slot_by_id(slot_id)
        replace_in_repeated(slot.clusters.objects, ['wrong-dc'])
        deleted = self.yp_manager._delete_slot_external(slot)
        yp_data = self.yp.mock_data
        yp_result = {
            cluster: yp_data[cluster].get(slot_id) for cluster in YP.ClusterType.keys()
        }
        result = {'deleted': deleted, 'yp_pods': yp_result}
        expected = {'deleted': True, 'yp_pods': {cluster: None for cluster in YP.ClusterType.keys()}}
        self.assertEqual(result, expected)

    def test_delete_slot_not_authorized(self):
        slot_id = 'slot-with-auth'
        slot = self._slot_by_id(slot_id)
        with self.nanny.mock_auth('not-authorized'):
            deleted = self.yp_manager._delete_slot_external(slot)
        self.assertEquals(deleted, False)

    def test_slot_state_db_update(self):
        slot_id = 'slot-with-active-snapshots'
        slot = self._slot_by_id(slot_id)
        query = mock.Mock()
        filtered_query = mock.Mock()
        self.session.query.side_effect = lambda x: (
            query if x == model.Slot else self.fail('querying unexpected table: {}'.format(x))
        )
        query.filter.side_effect = lambda x: (
            filtered_query
            if x.compare(model.Slot.id == slot_id)
            else self.fail('unexpected query filter: {}'.format(x.compile(compile_kwargs={"literal_binds": True})))
        )
        self.yp_manager._delete_slot_external(slot)
        filtered_query.update.assert_called_with({model.Slot.state.name: Slot.State[Slot.State.BROKEN]})


class TestYPManagerGetBrokenSlots(TestYPManager):

    @classmethod
    def _setUp_test_data(cls):
        broken_component_id = str(uuid.uuid4())
        single_broken_slot_component_id = str(uuid.uuid4())
        _now = now().timestamp()
        cls.test_data = {
            '_now': _now,
            'broken_component_id': broken_component_id,
            'single_broken_slot_component_id': single_broken_slot_component_id,
            'broken_slots': [
                BrokenSlot(
                    component_id=broken_component_id,
                    id='broken-slot-id-{}'.format(i),
                    time=(_now - i),
                )
                for i in range(cls.yp_manager.BROKEN_SLOT_LIMIT + 1)
            ] + [
                BrokenSlot(
                    component_id=single_broken_slot_component_id,
                    id='single-broken-slot',
                    time=_now,
                )
            ]
        }
        cls.test_data['min_slot_time'] = min([slot.time for slot in cls.test_data['broken_slots']])

    def _setUp_db(self):
        with session_scope() as session:
            prepare_db()

            for broken_slot in self.test_data['broken_slots']:
                session.add(model.BrokenSlot.from_protobuf(broken_slot))

    def test_no_broken_slots(self):
        result = self.yp_manager._get_broken_slots(BetaComponent(id=str(uuid.uuid4())))
        self.assertEqual(result, [])

    def test_single_broken_slot(self):
        with mock_now(self.test_data['_now']):
            result = self.yp_manager._get_broken_slots(
                BetaComponent(id=self.test_data['single_broken_slot_component_id']),
            )
            result = len(result)

        expected = 1
        self.assertEqual(result, expected)

    def test_more_than_limit_broken_slots(self):
        with mock_now(self.test_data['min_slot_time']):
            result = self.yp_manager._get_broken_slots(
                BetaComponent(id=self.test_data['broken_component_id']),
            )
            result = len(result)

        expected = self.yp_manager.BROKEN_SLOT_LIMIT
        self.assertEqual(result, expected)

    def test_obsolete_broken_slots(self):
        _now = self.test_data['_now'] + 2 * self.yp_manager.BROKEN_SLOT_TTL
        with mock_now(_now):
            result = self.yp_manager._get_broken_slots(
                BetaComponent(id=self.test_data['broken_component_id']),
            )
            result = len(result)

        expected = 0
        self.assertEqual(result, expected)


class TestYpManagerConfig(TestCase):

    @mock.patch('search.priemka.yappy.src.yappy_lib.worker.get_config')
    def test_global_default_category(self, mocked_config):
        mocked_config.return_value = DEFAULT_YAPPY_CONFIG
        yp_manager = YPManager()
        expected = DEFAULT_YAPPY_CONFIG.model.allocator.yp_slot_nanny_category
        result = yp_manager.NANNY_CATEGORY
        self.assertEqual(result, expected)

    @mock.patch('search.priemka.yappy.src.yappy_lib.worker.get_config')
    def test_globally_configured_category(self, mocked_config):
        category = '/test/category/'
        config = YappyConfiguration()
        config.model.allocator.yp_slot_nanny_category = category
        mocked_config.return_value = config
        yp_manager = YPManager()
        expected = category
        result = yp_manager.NANNY_CATEGORY
        self.assertEqual(result, expected)

    def test_locally_configured_category(self):
        category = '/test/category/'
        config = YappyConfiguration()
        config.model.allocator.yp_slot_nanny_category = category
        yp_manager = YPManager(config)
        expected = category
        result = yp_manager.NANNY_CATEGORY
        self.assertEqual(result, expected)

    def test_not_configured(self):
        config = YappyConfiguration()
        yp_manager = YPManager(config)
        self.assertRaises(
            RuntimeError,
            lambda: yp_manager.NANNY_CATEGORY,   # assertRaises require callable here, so it's lambda
        )


class TestYPManagerCreateSlot(TestYPManager):

    def enter_create_slot_stack(self, stack):
        """ Mock everything we need to mock for ``create_slot`` internal logics texting """
        mocked = {
            'copy_service': stack.enter_context(mock.patch.object(self.yp_manager.clients.nanny, 'copy_service')),
            'broken_slots': stack.enter_context(mock.patch.object(self.yp_manager, '_get_broken_slots')),
            'create_pod_set': stack.enter_context(mock.patch.object(self.yp_manager.clients.yp, 'create_pod_set')),
            'get_runtime': stack.enter_context(mock.patch.object(self.yp_manager.clients.nanny, 'get_runtime_attrs')),
            'put_runtime': stack.enter_context(mock.patch.object(self.yp_manager.clients.nanny, 'put_runtime_attrs')),
            'set_snapshot': stack.enter_context(mock.patch.object(self.yp_manager.clients.nanny, 'set_snapshot_state')),
            'get_info': stack.enter_context(mock.patch.object(self.yp_manager.clients.nanny, 'get_info_attrs')),
            'put_info': stack.enter_context(mock.patch.object(self.yp_manager.clients.nanny, 'put_info_attrs')),
            'clear_cdump': stack.enter_context(mock.patch.object(self.yp_manager, 'clear_cdump_policy')),
            'reset_tickets': stack.enter_context(mock.patch.object(self.yp_manager, 'reset_tickets_integration')),
            'reset_sched_policy': stack.enter_context(mock.patch.object(self.yp_manager, 'reset_scheduling_policy')),
            'create_broken_slot': stack.enter_context(mock.patch.object(self.yp_manager, '_create_broken_slot')),
            'session_scope': stack.enter_context(
                mock.patch('{}.session_scope'.format(self.yp_manager.__class__.__module__))
            ),
            'slot_exists': stack.enter_context(mock.patch.object(self.yp_manager, '_slot_exists')),
        }
        mocked['broken_slots'].return_value = []
        mocked['get_runtime'].return_value = {}
        mocked['get_info'].return_value = {}
        mocked['create_pod_set'].return_value = ['pod_id']
        mocked['slot_exists'].return_value = False
        return mocked

    def test_nanny_service_copied(self):
        component = BetaComponent(id=str(uuid.uuid4()))
        component.type.yp.service_template_id = 'template-service'
        component.quota.abc_service_id = 123

        expected_template_service = component.type.yp.service_template_id

        with ExitStack() as stack:
            mocked = self.enter_create_slot_stack(stack)
            self.yp_manager.create_slot(to_model(component), reqid='reqid')
            copy_service = mocked['copy_service']
            copy_service.assert_called_once()
            template_service = copy_service.call_args.kwargs.get('source_service_id')

        self.assertEqual(template_service, expected_template_service)

    def test_pods_set(self):
        component = BetaComponent(id=str(uuid.uuid4()))
        component.type.yp.service_template_id = 'template-service'
        component.quota.abc_service_id = 123

        expected_pods = ['pod_id-{}'.format(i) for i in range(3)]

        with ExitStack() as stack:
            mocked = self.enter_create_slot_stack(stack)

            mocked['create_pod_set'].return_value = expected_pods

            self.yp_manager.create_slot(to_model(component), reqid='reqid')

            put_runtime = mocked['put_runtime']
            put_runtime.assert_called_once()
            sent_runtime = put_runtime.call_args.args[1]
            sent_pods = [pod['pod_id'] for pod in sent_runtime['content']['instances']['yp_pod_ids']['pods']]

        self.assertEqual(sent_pods, expected_pods)

    def test_slot_saved(self):
        component = BetaComponent(id=str(uuid.uuid4()))
        component.type.yp.service_template_id = 'template-service'
        component.quota.abc_service_id = 123

        with ExitStack() as stack:
            mocked = self.enter_create_slot_stack(stack)
            session = mock.Mock()
            mocked['session_scope'].return_value.__enter__.return_value = session
            self.yp_manager.create_slot(to_model(component), reqid='reqid')
            session.add.assert_called()
            saved = session.add.call_args.args[0]

        self.assertIsInstance(saved, model.Slot)

    def test_tag_set(self):
        component = BetaComponent(id=str(uuid.uuid4()))
        component.type.yp.service_template_id = 'template-service'
        component.quota.abc_service_id = 123
        component.tags.tier = 'tier-tag-test'

        expected_tier_tag = component.tags.tier

        with ExitStack() as stack:
            mocked = self.enter_create_slot_stack(stack)

            self.yp_manager.create_slot(to_model(component), reqid='reqid')

            put_runtime = mocked['put_runtime']
            put_runtime.assert_called_once()
            sent_runtime = put_runtime.call_args.args[1]
            sent_tags = sent_runtime['content']['instances']['yp_pod_ids']['orthogonal_tags']
            tier_tag = sent_tags['tier']

        self.assertEqual(tier_tag, expected_tier_tag)

    def test_cdump_policy_cleared(self):
        component = BetaComponent(id=str(uuid.uuid4()))
        component.type.yp.service_template_id = 'template-service'
        component.quota.abc_service_id = 123

        with ExitStack() as stack:
            mocked = self.enter_create_slot_stack(stack)
            self.yp_manager.create_slot(to_model(component), reqid='reqid')
            mocked['clear_cdump'].assert_called()

    def test_cdump_policy_preserved(self):
        component = BetaComponent(id=str(uuid.uuid4()))
        component.type.yp.service_template_id = 'template-service'
        component.quota.abc_service_id = 123
        component.patch.copy_coredump_policy = True

        with ExitStack() as stack:
            mocked = self.enter_create_slot_stack(stack)
            self.yp_manager.create_slot(to_model(component), reqid='reqid')
            mocked['clear_cdump'].assert_not_called()

    def test_tickets_integration_reset_called(self):
        component = BetaComponent(id=str(uuid.uuid4()))
        component.type.yp.service_template_id = 'template-service'
        component.quota.abc_service_id = 123

        with ExitStack() as stack:
            mocked = self.enter_create_slot_stack(stack)
            self.yp_manager.create_slot(to_model(component), reqid='reqid')
            mocked['reset_tickets'].assert_called()

    def test_tickets_integration_reset_called_with_original_values(self):
        component = BetaComponent(id=str(uuid.uuid4()))
        component.type.yp.service_template_id = 'template-service'
        component.quota.abc_service_id = 123

        with ExitStack() as stack:
            mocked = self.enter_create_slot_stack(stack)
            expected = {'info_attrs': 'original'}
            mocked['get_info'].return_value = expected
            self.yp_manager.create_slot(to_model(component), reqid='reqid')
            mocked['reset_tickets'].assert_called_with(expected)

    def test_scheduling_policy_reset_called(self):
        component = BetaComponent(id=str(uuid.uuid4()))
        component.type.yp.service_template_id = 'template-service'
        component.quota.abc_service_id = 123

        with ExitStack() as stack:
            mocked = self.enter_create_slot_stack(stack)
            self.yp_manager.create_slot(to_model(component), reqid='reqid')
            mocked['reset_sched_policy'].assert_called()

    def test_scheduling_policy_reset_called_with_original_values(self):
        component = BetaComponent(id=str(uuid.uuid4()))
        component.type.yp.service_template_id = 'template-service'
        component.quota.abc_service_id = 123

        with ExitStack() as stack:
            mocked = self.enter_create_slot_stack(stack)
            expected = {'info_attrs': 'original'}
            mocked['get_info'].return_value = expected
            self.yp_manager.create_slot(to_model(component), reqid='reqid')
            mocked['reset_sched_policy'].assert_called_with(expected)

    def test_broken_slot_created_on_duplicate_error_if_slot_not_exists(self):
        component = BetaComponent(id=str(uuid.uuid4()))
        component.type.yp.service_template_id = 'template-service'
        component.quota.abc_service_id = 123
        with ExitStack() as stack:
            mocked = self.enter_create_slot_stack(stack)
            mocked['copy_service'].side_effect = DuplicateError()
            try:
                self.yp_manager.create_slot(to_model(component), reqid='reqid')
            except DuplicateError:
                pass
            mocked['create_broken_slot'].assert_called()

    def test_broken_slot_not_created_on_duplicate_error_if_slot_exists(self):
        component = BetaComponent(id=str(uuid.uuid4()))
        component.type.yp.service_template_id = 'template-service'
        component.quota.abc_service_id = 123
        with ExitStack() as stack:
            mocked = self.enter_create_slot_stack(stack)
            mocked['copy_service'].side_effect = DuplicateError()
            mocked['slot_exists'].return_value = True
            try:
                self.yp_manager.create_slot(to_model(component), reqid='reqid')
            except DuplicateError:
                pass
            mocked['create_broken_slot'].assert_not_called()

    def test_broken_slot_created_on_failed_service_creation_not_found(self):
        component = BetaComponent(id=str(uuid.uuid4()))
        component.type.yp.service_template_id = 'template-service'
        component.quota.abc_service_id = 123
        with ExitStack() as stack:
            mocked = self.enter_create_slot_stack(stack)
            mocked['copy_service'].side_effect = NotFound()
            try:
                self.yp_manager.create_slot(to_model(component), reqid='reqid')
            except NotFound:
                pass
            mocked['create_broken_slot'].assert_called()


class TestYPManagerCreateNannyService(TestYPManager):

    def enter_create_service_stack(self, stack):
        """ Mock everything we need to mock for ``create_slot`` internal logics texting """
        mocked = {
            'copy_service': stack.enter_context(mock.patch.object(self.yp_manager.clients.nanny, 'copy_service')),
            'get_info': stack.enter_context(mock.patch.object(self.yp_manager.clients.nanny, 'get_info_attrs')),
            'put_info': stack.enter_context(mock.patch.object(self.yp_manager.clients.nanny, 'put_info_attrs')),
            'reset_tickets': stack.enter_context(mock.patch.object(self.yp_manager, 'reset_tickets_integration')),
            'reset_sched_policy': stack.enter_context(mock.patch.object(self.yp_manager, 'reset_scheduling_policy')),
        }
        mocked['get_info'].return_value = {}
        return mocked

    def test_nanny_service_copied(self):
        expected_template_service = 'template-service'
        with ExitStack() as stack:
            mocked = self.enter_create_service_stack(stack)
            copy_service = mocked['copy_service']
            self.yp_manager.create_nanny_service('new-service', expected_template_service)
            copy_service.assert_called_once()
            template_service = copy_service.call_args.kwargs.get('source_service_id')
        self.assertEqual(template_service, expected_template_service)

    def test_tickets_integration_reset_called(self):
        template_service = 'template-service'
        new_service = 'new-service'
        with ExitStack() as stack:
            mocked = self.enter_create_service_stack(stack)
            self.yp_manager.create_nanny_service(new_service, template_service)
            mocked['reset_tickets'].assert_called()

    def test_tickets_integration_reset_called_with_original_values(self):
        template_service = 'template-service'
        new_service = 'new-service'
        with ExitStack() as stack:
            mocked = self.enter_create_service_stack(stack)
            expected = {'info_attrs': 'original'}
            mocked['get_info'].return_value = expected
            self.yp_manager.create_nanny_service(new_service, template_service)
            mocked['reset_tickets'].assert_called_with(expected)

    def test_scheduling_policy_reset_called(self):
        template_service = 'template-service'
        new_service = 'new-service'
        with ExitStack() as stack:
            mocked = self.enter_create_service_stack(stack)
            self.yp_manager.create_nanny_service(new_service, template_service)
            mocked['reset_sched_policy'].assert_called()

    def test_scheduling_policy_reset_called_with_original_values(self):
        template_service = 'template-service'
        new_service = 'new-service'
        with ExitStack() as stack:
            mocked = self.enter_create_service_stack(stack)
            expected = {'info_attrs': 'original'}
            mocked['get_info'].return_value = expected
            self.yp_manager.create_nanny_service(new_service, template_service)
            mocked['reset_sched_policy'].assert_called_with(expected)


class TestYPManagerRuntimeUpdates(TestYPManager):

    def test_clear_cdump_single(self):
        spec = InstanceSpec()
        spec.containers.append(Container(name='c-1'))
        spec.containers[0].coredump_policy.type = CoredumpPolicy.Type.COREDUMP
        runtime_attrs = {'content': {'instance_spec': MessageToDict(spec)}}

        self.yp_manager.clear_cdump_policy(runtime_attrs)
        policy = runtime_attrs['content']['instance_spec']['containers'][0].get('coredumpPolicy', None)

        self.assertIsNone(policy)

    def test_clear_cdump_many(self):
        n_containers = 5
        spec = InstanceSpec()
        spec.containers.extend([Container(name='c-{}'.format(i)) for i in range(n_containers)])
        for c in spec.containers:
            c.coredump_policy.CopyFrom(randproto(CoredumpPolicy))
        runtime_attrs = {'content': {'instance_spec': MessageToDict(spec)}}

        self.yp_manager.clear_cdump_policy(runtime_attrs)

        policies = [
            c.get('coredumpPolicy', None)
            for c in runtime_attrs['content']['instance_spec']['containers']
        ]

        expected = [None] * n_containers
        self.assertEqual(policies, expected)


class TestYPManagerInfoUpdates(TestYPManager):

    @property
    def default_sched_policy(self):
        return {
            'type': self.yp_manager.DEFAULT_SCHEDULING_POLICY,
            'force_active_trunk': {
                'prepare_recipe': 'default',
                'activate_recipe': 'default',
            },
        }

    def test_reset_tickets_integration(self):
        info_attrs = {'content': {'tickets_integration': {'key': 'value'}}}

        self.yp_manager.reset_tickets_integration(info_attrs)
        result = info_attrs['content']['tickets_integration']['service_release_tickets_enabled']

        self.assertFalse(result)

    def test_reset_tickets_integration_empty_src(self):
        info_attrs = {}

        self.yp_manager.reset_tickets_integration(info_attrs)
        result = info_attrs['content']['tickets_integration']['service_release_tickets_enabled']

        self.assertFalse(result)

    def test_reset_scheduling_policy(self):
        info_attrs = {'content': {'scheduling_policy': {'type': 'TYPE'}}}

        self.yp_manager.reset_scheduling_policy(info_attrs)
        result = info_attrs['content']['scheduling_policy']

        expected = self.default_sched_policy

        self.assertEqual(result, expected)

    def test_reset_scheduling_policy_empty_src(self):
        info_attrs = {}

        self.yp_manager.reset_scheduling_policy(info_attrs)
        result = info_attrs['content']['scheduling_policy']

        expected = self.default_sched_policy

        self.assertEqual(result, expected)

    def test_reset_scheduling_policy_force_recipes_specified(self):
        recipes = {
            'activate_recipe': 'ACTIVATE_RECIPE',
            'prepare_recipe': 'PREPARE_RECIPE',
        }
        info_attrs = {'content': {'scheduling_policy': {'force_active_trunk': recipes.copy()}}}

        self.yp_manager.reset_scheduling_policy(info_attrs)
        result = info_attrs['content']['scheduling_policy']['force_active_trunk']

        expected = recipes
        self.assertEqual(result, expected)

    def test_reset_scheduling_policy_keep_known_recipes(self):
        recipes = {
            'activate_recipe': 'ACTIVATE_RECIPE',
            'prepare_recipe': 'PREPARE_RECIPE',
        }
        policy = {
            'policy_1': {'activate_recipe': recipes['activate_recipe']},
            'policy_2': {'prepare_recipe': recipes['prepare_recipe']},
        }
        info_attrs = {'content': {'scheduling_policy': policy}}

        self.yp_manager.reset_scheduling_policy(info_attrs)
        result = info_attrs['content']['scheduling_policy']['force_active_trunk']

        expected = recipes
        self.assertEqual(result, expected)

    def test_reset_scheduling_policy_dont_override_force_recipes(self):
        force_recipes = {
            'activate_recipe': 'ACTIVATE_RECIPE',
        }
        info_attrs = {
            'content': {
                'scheduling_policy': {
                    'policy_1': {'prepare_recipe': 'RECIPE', 'activate_recipe': ''},
                    'force_active_trunk': force_recipes.copy(),
                }
            }
        }

        self.yp_manager.reset_scheduling_policy(info_attrs)
        result = info_attrs['content']['scheduling_policy']['force_active_trunk']

        expected = force_recipes
        self.assertEqual(result, expected)

    def test_reset_scheduling_policy_drop_extra_polices(self):
        force_recipes = {
            'activate_recipe': 'ACTIVATE_RECIPE',
        }
        info_attrs = {
            'content': {
                'scheduling_policy': {
                    'policy_1': {'prepare_recipe': 'RECIPE', 'activate_recipe': ''},
                    'policy_2': {},
                    'force_active_trunk': force_recipes.copy(),
                }
            }
        }

        self.yp_manager.reset_scheduling_policy(info_attrs)
        result = sorted(info_attrs['content']['scheduling_policy'].keys())

        expected = ['force_active_trunk', 'type']
        self.assertEqual(result, expected)
