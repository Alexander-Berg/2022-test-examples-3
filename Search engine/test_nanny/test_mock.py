# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import copy

from search.martylib.core.date_utils import now, mock_now
from search.martylib.test_utils import TestCase
from search.martylib.http.exceptions import NotFound, BadRequest, PreconditionFailed, NotAuthorized

from search.martylib.nanny import NannyClientMock


class TestNannyClientMock(TestCase):
    mock_data = {
        'empty-service': {},
        'mock-service': {
            'auth_attrs': {
                'content': {
                    'owners': {
                        'logins': ['owner-user'],
                    },
                    'ops_managers': {
                        'logins': ['ops-manager'],
                    },
                },
            },
            'target_state': {
                '_id': '',
                'content': {
                    'is_enabled': True,
                },
            },
            'runtime_attrs': {
                '_id': '',
                'content': {
                    'resources': {
                        'sandbox_files': [
                            {
                                'local_path': 'sandbox_file',
                            }
                        ],
                        'static_files': [
                            {
                                'local_path': 'static_file',
                                'content': 'mock_content',
                                'is_dynamic': False,
                            }
                        ],
                        'url_files': [],
                    }
                }
            },
            'current_state': {
                'content': {
                    'active_snapshots': [
                        {
                            'snapshot_id': 'active-snapshot-id',
                            'state': 'ACTIVE',
                        },
                        {
                            'snapshot_id': 'created-snapshot-id',
                            'state': 'CREATED',
                        }
                    ],
                    'summary': {
                        'value': 'ONLINE',
                    }
                }
            },
            'target_state': {
                'content': {
                    'snapshots': [
                        {
                            'snapshot_id': 'active-snapshot-id',
                            'state': 'ACTIVE',
                        },
                        {
                            'snapshot_id': 'created-snapshot-id',
                            'state': 'CREATED',
                        }
                    ]
                }
            }
        },
    }

    @classmethod
    def setUpClass(cls):
        super(TestNannyClientMock, cls).setUpClass()
        cls.mock = NannyClientMock()

    def setUp(self):
        self.mock.load_services(copy.deepcopy(self.mock_data))
        
    def tearDown(self):
        self.mock.data = {}
        self.mock.reset_events()
   
    def test_get_target_state(self):
        expected = self.mock_data['mock-service']['target_state']
        result = self.mock.get_target_state('mock-service')
        self.assertEqual(result, expected)

    def test_get_empty_target_state(self):
        expected = {'_id': '', 'info': {}, 'content': {}}
        result = self.mock.get_target_state('empty-service')
        self.assertEqual(result, expected)

    def test_get_resources_content(self):
        expected = self.mock_data['mock-service']['runtime_attrs']['content']['resources']
        result = self.mock.get_resources('mock-service')
        self.assertEqual(result['content'], expected)

    def test_get_empty_resources_content(self):
        expected = {'sandbox_files': [], 'static_files': [], 'url_files': []}
        result = self.mock.get_resources('empty-service')
        self.assertEqual(result['content'], expected)

    def test_put_resources(self):
        service = self.mock_data['mock-service']
        resources = copy.deepcopy(service['runtime_attrs']['content']['resources'])
        resources['url_files'] = [{'local_path': 'new_url_file', 'url': 'URL'}]
        expected = copy.deepcopy(service['runtime_attrs']['content'])
        expected['resources'] = resources

        result = self.mock.put_resources('empty-service', {'content': resources}, 'comment')
        self.assertEqual(result['runtime_attrs']['content'], expected)

    def test_put_runtime_attrs_content(self):
        runtime_attrs = {'content': {'resources': {}}, '_id': ''}
        expected_content = {'resources': {}}
        result = self.mock.put_runtime_attrs('mock-service', runtime_attrs, 'comment')
        self.assertEqual(result['content'], expected_content)

    def test_put_auth_attrs_content(self):
        auth_attrs = {'content': {'observiers': {'logins': ['fake-user']}}, '_id': ''}
        expected_content = {'observiers': {'logins': ['fake-user']}}
        result = self.mock.put_auth_attrs('mock-service', auth_attrs, 'comment')
        self.assertEqual(result['content'], expected_content)

    def test_put_info_attrs_content(self):
        info_attrs = {'content': {'category': '/mock/category'}, '_id': ''}
        expected_content = {'category': '/mock/category'}
        result = self.mock.put_info_attrs('mock-service', info_attrs, 'comment')
        self.assertEqual(result['content'], expected_content)

    def test_put_get_resources(self):
        resources = {'content': self.mock_data['mock-service']['runtime_attrs']['content']['resources']}
        expected = resources['content']
        self.mock.put_resources('empty-service', resources, 'Mock resources update')
        result = self.mock.get_resources('empty-service')
        self.assertEqual(result['content'], expected)

    def test_put_get_runtime_attrs_content(self):
        runtime_attrs = {'content': {'resources': {}}, '_id': ''}
        expected_content = {'resources': {}}
        self.mock.put_runtime_attrs('mock-service', runtime_attrs, 'comment')
        result = self.mock.get_runtime_attrs('mock-service')
        self.assertEqual(result['content'], expected_content)

    def test_put_get_auth_attrs_content(self):
        auth_attrs = {'content': {'observiers': {'logins': ['fake-user']}}, '_id': ''}
        expected_content = {'observiers': {'logins': ['fake-user']}}
        self.mock.put_auth_attrs('mock-service', auth_attrs, 'comment')
        result = self.mock.get_auth_attrs('mock-service')
        self.assertEqual(result['content'], expected_content)

    def test_put_get_info_attrs_content(self):
        info_attrs = {'content': {'category': '/mock/category'}, '_id': ''}
        expected_content = {'category': '/mock/category'}
        self.mock.put_info_attrs('mock-service', info_attrs, 'comment')
        result = self.mock.get_info_attrs('mock-service')
        self.assertEqual(result['content'], expected_content)

    def test_list_summaries(self):
        _now = now().timestamp()
        self.mock.load_services(
            {
                'service-1': {
                    'info_attrs': {
                        'content': {
                            'category': '/category/1',
                            'desc': 'Service 1 description',
                        },
                    },
                    'current_state': {
                        'content': {
                            'summary': {
                                'value': 'ONLINE',
                                'entered': int((_now - 3600) * 1000),
                            },
                        },
                    },
                },
                'service-2': {
                    'info_attrs': {
                        'content': {
                            'category': '/category/2',
                            'desc': 'Service 2 description',
                        },
                    },
                    'current_state': {
                        'content': {
                            'summary': {
                                'value': 'OFFLINE',
                                'entered': int((_now - 86400) * 1000),
                            },
                        },
                    },
                },
                'service-3': {
                    'info_attrs': {
                        'content': {
                            'category': '/category/1',
                        },
                    },
                },
            }
        )

        # List sorted by serviceID
        expected = [
            {
                'category': '',
                'since': '0',
                'labels': {},
                'summary': 'ONLINE',
                'serviceId': 'empty-service',
                'desc': ''
            },
            {
                'category': '',
                'since': '0',
                'labels': {},
                'summary': 'ONLINE',
                'serviceId': 'mock-service',
                'desc': ''
            },
            {
                'category': '/category/1',
                'since': str(int((_now - 3600) * 1000)),
                'labels': {},
                'summary': 'ONLINE',
                'serviceId': 'service-1',
                'desc': 'Service 1 description'
            },
            {
                'category': '/category/2',
                'since': str(int((_now - 86400) * 1000)),
                'labels': {},
                'summary': 'OFFLINE',
                'serviceId': 'service-2',
                'desc': 'Service 2 description'
            },
            {
                'category': '/category/1',
                'since': '0',
                'labels': {},
                'summary': 'ONLINE',
                'serviceId': 'service-3',
                'desc': ''
            },
        ]

        result = self.mock.list_summaries()
        self.assertEqual(result, expected)

    def test_not_found(self):
        self.assertRaises(
            NotFound,
            self.mock.get_runtime_attrs,
            'no-such-service',
        )

    def test_stop_service_response(self):
        with mock_now(1627970000):
            _now = int(now().timestamp() * 1000)
            result = self.mock.stop_service('mock-service', 'stop mock service comment')
            expected = {
                'status': 'IN_QUEUE',
                'mtime': _now,
                'service_id': 'mock-service',
                'set_target_state': {
                    'is_enabled': False,
                    'labels': [],
                    'tracked_tickets': {
                        'tickets': [],
                        'startrek_tickets': [],
                    }
                },
                '_id': '{}-12345678'.format(now().timestamp_mcs()),
                'type': 'SET_TARGET_STATE',
            }
            self.assertEqual(result, expected)

    def test_stop_service_event(self):
        self.mock.stop_service('mock-service', 'stop mock service comment')
        event = self.mock.last_event()
        expected = {
            'content': {
                'is_enabled': False,
                'comment': 'stop mock service comment',
            },
            'type': 'SET_TARGET_STATE',
        }
        self.assertEqual(event, expected)

    def test_stop_service_current_state_summary(self):
        with mock_now(1627970000):
            self.mock.stop_service('mock-service')
            current_state = self.mock.get_current_state('mock-service')
            summary = current_state.get('content', {}).get('summary')
            expected = {
                'value': 'OFFLINE',
                'entered': int(now().timestamp() * 1000),
            }
        self.assertEqual(summary, expected)

    def test_stop_service_current_state_snapshots(self):
        self.mock.stop_service('mock-service')
        current_state = self.mock.get_current_state('mock-service')
        snapshot_states = {
            snapshot.get('snapshot_id'): snapshot.get('state')
            for snapshot in current_state.get('content', {}).get('active_snapshots', [])
        }
        expected = {
            'active-snapshot-id': 'PREPARED',
            'created-snapshot-id': 'CREATED',
        }
        self.assertEquals(snapshot_states, expected)

    def test_stop_service_target_state_enabled(self):
        self.mock.stop_service('mock-service')
        target_state = self.mock.get_target_state('mock-service')
        enabled = target_state.get('content', {}).get('is_enabled')
        self.assertEquals(enabled, False)

    def test_stop_service_target_state_snapshots(self):
        self.mock.stop_service('mock-service')
        target_state = self.mock.get_target_state('mock-service')
        snapshot_states = {
            snapshot.get('snapshot_id'): snapshot.get('state')
            for snapshot in target_state.get('content', {}).get('snapshots', [])
        }
        expected = {
            'active-snapshot-id': 'PREPARED',
            'created-snapshot-id': 'CREATED',
        }
        self.assertEquals(snapshot_states, expected)

    def test_stop_service_not_found(self):
        self.assertRaises(
            NotFound,
            self.mock.stop_service,
            'no-such-service',
        )

    def test_stop_service_not_authorized(self):
        with self.mock.mock_auth('not-authorized'):
            self.assertRaises(
                NotAuthorized,
                self.mock.stop_service,
                'mock-service',
            )

    def test_stop_service_owner(self):
        with self.mock_auth('owner-user'):
            result = self.mock.stop_service('mock-service')
        self.assertIsNotNone(result)

    def test_stop_service_ops_manager(self):
        with self.mock_auth('ops-manager'):
            result = self.mock.stop_service('mock-service')
        self.assertIsNotNone(result)

    def test_set_snapshot_state_service_not_found(self):
        self.assertRaises(
            NotFound,
            self.mock.set_snapshot_state,
            'no-such-service',
            '',
            'ACTIVE',
        )

    def test_set_snapshot_state_snapshot_not_found(self):
        self.assertRaises(
            NotFound,
            self.mock.set_snapshot_state,
            'empty-service',
            'no-such-snapshot',
            'ACTIVE',
        )

    def test_set_snapshot_state_destroy_not_stopped(self):
        self.assertRaises(
            BadRequest,
            self.mock.set_snapshot_state,
            'mock-service',
            'active-snapshot-id',
            'DESTROYED',
        )

    def test_set_snapshot_state_response(self):
        self.mock.stop_service('mock-service')
        with mock_now(1627970000):
            result = self.mock.set_snapshot_state('mock-service', 'active-snapshot-id', 'DESTROYED')
        expected = {
            'set_snapshot_state': {
                'comment': '-',
                'prepare_recipe': 'default',
                'recipe': 'default',
                'state': 'DESTROYED',
                'snapshot_id': 'active-snapshot-id',
                'set_as_current': False,
                'labels': [],
                'recipe_parameters': [],
                'tracked_tickets': {
                    'tickets': [],
                    'startrek_tickets': [],
                }
            },
            'mtime': 1627970000000,
            'service_id': 'mock-service',
            '_id': '',
            'type': 'SET_SNAPSHOT_STATE',
        }
        self.assertEqual(result, expected)

    def test_set_snapshot_state_event(self):
        self.mock.set_snapshot_state('mock-service', 'created-snapshot-id', 'PREPARED', 'some comment')
        event = self.mock.last_event()
        expected = {
            'content': {
                'snapshot_id': 'created-snapshot-id',
                'state': 'PREPARED',
                'comment': 'some comment',
                'prepare_recipe': 'default',
                'recipe': 'default',
                'set_as_current': False,
            },
            'type': 'SET_SNAPSHOT_STATE',
        }
        self.assertEqual(event, expected)

    def test_set_snapshot_state_state(self):
        self.mock.set_snapshot_state('mock-service', 'created-snapshot-id', 'PREPARED', 'some comment')
        current_state = self.mock.get_current_state('mock-service')
        current_snapshot = {}
        for snapshot in current_state['content']['active_snapshots']:
            if snapshot['snapshot_id'] == 'created-snapshot-id':
                current_snapshot = snapshot
                break
        target_state = self.mock.get_target_state('mock-service')
        target_snapshot = {}
        for snapshot in target_state['content']['snapshots']:
            if snapshot['snapshot_id'] == 'created-snapshot-id':
                target_snapshot = snapshot
                break
        states = (current_snapshot['state'], target_snapshot['state'])
        self.assertEqual(states, ('PREPARED', 'PREPARED'))

    def test_set_snapshot_state_destroyed_state(self):
        self.mock.stop_service('mock-service')
        with mock_now(1627970000):
            result = self.mock.set_snapshot_state('mock-service', 'active-snapshot-id', 'DESTROYED')

        current_snapshots = (
            self.mock.get_current_state('mock-service').get('content', {}).get('active_snapshots', [])
        )
        target_snapshots = self.mock.get_target_state('mock-service').get('content', {}).get('snapshots', [])

        found = {'current': False, 'target': False}
        expected = {'current': False, 'target': False}
        for snapshot in current_snapshots:
            if snapshot.get('snapshot_id') == 'active-snapshot-id':
                found['current'] = True
                break
        for snapshot in target_snapshots:
            if snapshot.get('snapshot_id') == 'active-snapshot-id':
                found['target'] = True
                break

        self.assertEqual(found, expected)

    def test_set_snapshot_state_not_authorized(self):
        with self.mock.mock_auth('not-authorized'):
            self.assertRaises(
                NotAuthorized,
                self.mock.set_snapshot_state,
                'mock-service',
                'snapshot-id',
                'PREPARED',
            )

    def test_set_snapshot_state_ops_manager(self):
        with self.mock.mock_auth('ops-manager'):
            result = self.mock.set_snapshot_state('mock-service', 'created-snapshot-id', 'PREPARED')
        self.assertIsNotNone(result)

    def test_set_snapshot_state_owner(self):
        with self.mock.mock_auth('owner-user'):
            result = self.mock.set_snapshot_state('mock-service', 'created-snapshot-id', 'PREPARED')
        self.assertIsNotNone(result)

    def test_delete_service_not_exists(self):
        self.mock.delete_service('no-such-service')
        n_services_left = len(self.mock.data)
        expected = len(self.mock_data)
        self.assertEqual(n_services_left, expected)

    def test_delete_online_service(self):
        self.assertRaises(
            PreconditionFailed,
            self.mock.delete_service,
            'mock-service',
        )

    def test_delete_service(self):
        self.mock.stop_service('mock-service')
        self.mock.delete_service('mock-service')
        self.assertRaises(
            NotFound,
            self.mock.get_current_state,
            'mock-service',
        )

    def test_delete_service_owner(self):
        service_id = 'mock-service'
        owner_user = self.mock_data[service_id]['auth_attrs']['content']['owners']['logins'][0]
        self.mock.stop_service(service_id)
        self.mock.delete_service(service_id)
        with self.mock.mock_auth(owner_user):
            self.assertRaises(
                NotFound,
                self.mock.get_current_state,
                service_id,
            )

    def test_delete_service_not_authorized(self):
        service_id = 'mock-service'
        self.mock.stop_service(service_id)
        with self.mock.mock_auth('user-not-authorized'):
            self.assertRaises(
                NotAuthorized,
                self.mock.delete_service,
                service_id,
            )

