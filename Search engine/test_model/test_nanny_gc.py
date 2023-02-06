# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import copy
import six
import typing  # noqa

if six.PY2:
    import mock
else:
    import unittest.mock as mock

from search.martylib.core.date_utils import now, mock_now
from search.martylib.proto.structures.yp_lite_pb2 import AllocationRequest

from search.priemka.yappy.src.model.model_service.workers.nanny_gc import NannyGC
from search.priemka.yappy.src.yappy_lib.config_utils import get_test_config
from search.priemka.yappy.proto.structures.yp_pb2 import YP

from search.priemka.yappy.tests.utils.test_cases import TestCase


class TestNannyGC(TestCase):

    @classmethod
    def setUpClass(cls):
        super(TestNannyGC, cls).setUpClass()
        config = get_test_config()
        cls.gc_worker = NannyGC(config)
        # Trigger `clients` initialization and sync them for the GC worker and its YP manager
        # (...since `MockClients` are not singleton as the normal `Clients` are)
        cls.gc_worker._clients = cls.gc_worker.yp_manager.clients

        cls._setUp_mock_data()

    @classmethod
    def _setUp_mock_data(cls):
        _now = now().timestamp()
        HOUR_AGO = int((_now - 3600) * 1000)
        YEAR_AGO = int((_now - 365 * 86400) * 1000)

        cls.SERVICES = {
            'online_service': {
                'info_attrs': {
                    'content': {
                        'category': cls.gc_worker.yp_manager.NANNY_CATEGORY,
                    },
                },
                'current_state': {
                    'content': {
                        'summary': {
                            'value': 'ONLINE',
                            'entered': HOUR_AGO,
                        },
                        'active_snapshots': [],
                    },
                },
            },
            'old_service': {
                'info_attrs': {
                    'content': {
                        'category': cls.gc_worker.yp_manager.NANNY_CATEGORY,
                    },
                },
                'current_state': {
                    'content': {
                        'summary': {
                            'value': 'ONLINE',
                            'entered': YEAR_AGO
                        },
                        'active_snapshots': [],
                    },
                },
            },
            'offline_service': {
                'info_attrs': {
                    'content': {
                        'category': cls.gc_worker.yp_manager.NANNY_CATEGORY,
                    },
                },
                'current_state': {
                    'content': {
                        'summary': {
                            'value': 'OFFLINE',
                            'entered': HOUR_AGO
                        },
                        'active_snapshots': [],
                    },
                },
            },
            'offline_old_service-1': {
                'auth_attrs': {
                    'content': {
                        'owners': {
                            'logins': ['owner-user'],
                        },
                    },
                },
                'info_attrs': {
                    'content': {
                        'category': cls.gc_worker.yp_manager.NANNY_CATEGORY,
                    },
                },
                'current_state': {
                    'content': {
                        'summary': {
                            'value': 'OFFLINE',
                            'entered': YEAR_AGO,
                        },
                        'active_snapshots': [],
                    },
                },
            },
            'offline_old_service-2': {
                'info_attrs': {
                    'content': {
                        'category': cls.gc_worker.yp_manager.NANNY_CATEGORY,
                    },
                },
                'current_state': {
                    'content': {
                        'summary': {
                            'value': 'OFFLINE',
                            'entered': YEAR_AGO
                        },
                        'active_snapshots': [],
                    },
                },
            },
            'offline_old_service_with_active_snapshots': {
                'auth_attrs': {
                    'content': {'owners': {'logins': ['owner-user']}}
                },
                'info_attrs': {
                    'content': {
                        'category': cls.gc_worker.yp_manager.NANNY_CATEGORY,
                    },
                },
                'current_state': {
                    'content': {
                        'summary': {
                            'value': 'OFFLINE',
                            'entered': YEAR_AGO,
                        },
                        'active_snapshots': [{'snapshot_id': 'active-snapshot-id'}],
                    },
                },
            },
            'not_managed_offline_old_service': {
                'info_attrs': {
                    'content': {
                        'category': '/some/random/category',
                    },
                },
                'current_state': {
                    'content': {
                        'summary': {
                            'value': 'OFFLINE',
                            'entered': YEAR_AGO,
                        },
                        'active_snapshots': [],
                    },
                },
            },
        }

    def setUp(self):
        self._setUp_nanny()
        self._setUp_yp()
        self.session_scope_patch = mock.patch(
            '{}.session_scope'.format(self.gc_worker.yp_manager.__class__.__module__)
        )
        self.session_scope = self.session_scope_patch.start()

        self.addCleanup(self.session_scope_patch.stop)

    def tearDown(self):
        self._tearDown_nanny()
        self._tearDown_yp()

    def _setUp_nanny(self):
        self.gc_worker.clients.nanny.load_services(copy.deepcopy(self.SERVICES))

    def _tearDown_nanny(self):
        self.gc_worker.clients.nanny.data = {}

    def _setUp_yp(self):
        for service in self.SERVICES:
            for cluster in YP.ClusterType.keys()[1:]:
                replicas = YP.ClusterType[cluster]
                self.gc_worker.clients.yp.create_pod_set(AllocationRequest(replicas=replicas), service, cluster)

    def _tearDown_yp(self):
        self.gc_worker.clients.yp.reset_mock()

    def test_inactive_services(self):
        result = sorted(self.gc_worker.get_inactive_services())
        expected = ['offline_old_service-1', 'offline_old_service-2', 'offline_old_service_with_active_snapshots']
        self.assertEqual(result, expected)

    def test_run_once_yp_pods(self):
        self.gc_worker.run_once()
        yp_mock_data = self.gc_worker.clients.yp.mock_data
        cluster_pods_n = {}
        for cluster in yp_mock_data:
            cluster_pods_n[cluster] = 0
            for service in yp_mock_data[cluster]:
                cluster_pods_n[cluster] += len(yp_mock_data[cluster][service])
        n_services = len(self.SERVICES)
        services_to_survive = n_services - 2   # old service with active snapshots will survive single run
        expected = {
            cluster: YP.ClusterType[cluster] * services_to_survive
            for cluster in YP.ClusterType.keys()
        }
        self.assertEqual(cluster_pods_n, expected)

    def test_run_once_yp_pod_sets(self):
        self.gc_worker.run_once()
        yp_mock_data = self.gc_worker.clients.yp.mock_data
        cluster_pod_sets_n = {
            cluster: len(yp_mock_data[cluster])
            for cluster in yp_mock_data
        }
        n_services = len(self.SERVICES)
        services_to_survive = n_services - 2   # old service with active snapshots will survive single run
        expected = {
            cluster: services_to_survive
            for cluster in YP.ClusterType.keys()[1:]
        }
        expected[YP.ClusterType[0]] = 0
        self.assertEqual(cluster_pod_sets_n, expected)

    def test_run_once_x2_yp(self):
        # nothing will change if run worker two times in a row
        # for Nanny service state has just changed by `nanny.stop_service()`
        # and it is no longer "old" enough to be GC'ed
        self.gc_worker.run_once()
        yp_data = copy.deepcopy(self.gc_worker.clients.yp.mock_data)
        self.gc_worker.run_once()
        new_yp_data = self.gc_worker.clients.yp.mock_data
        self.assertEqual(yp_data, new_yp_data)

    def test_run_once_x2_paused_yp(self):
        self.gc_worker.run_once()
        # 40 days later
        with mock_now(now().timestamp() + 40 * 86400):
            self.gc_worker.run_once()
        cluster_pod_sets_n = {
            cluster: len(self.gc_worker.clients.yp.mock_data[cluster])
            for cluster in self.gc_worker.clients.yp.mock_data
        }
        n_services = len(self.SERVICES)
        services_to_survive = n_services - 4   # all offline services: w/snapshots and "recent" will also be removed
        expected = {
            cluster: services_to_survive
            for cluster in YP.ClusterType.keys()[1:]
        }
        expected[YP.ClusterType[0]] = 0
        self.assertEqual(cluster_pod_sets_n, expected)

    def test_run_once_x3_paused_yp(self):
        self.gc_worker.run_once()
        # 40 days later
        with mock_now(now().timestamp() + 40 * 86400):
            self.gc_worker.run_once()
        yp_data = copy.deepcopy(self.gc_worker.clients.yp.mock_data)
        # 40 more days later
        with mock_now(now().timestamp() + 40 * 86400):
            self.gc_worker.run_once()
        new_yp_data = self.gc_worker.clients.yp.mock_data
        self.assertEqual(yp_data, new_yp_data)

    def test_run_once_nanny(self):
        self.gc_worker.run_once()
        nanny = self.gc_worker.clients.nanny
        services_left = sorted(nanny.data.keys())
        expected = sorted(self.SERVICES.keys())
        expected.pop(expected.index('offline_old_service-1'))
        expected.pop(expected.index('offline_old_service-2'))
        self.assertEqual(services_left, expected)

    def test_run_once_x2_nanny(self):
        # nothing will change if run worker two times in a row
        # for Nanny service state has just changed by `nanny.stop_service()`
        # and it is no longer "old" enough to be GC'ed
        self.gc_worker.run_once()
        nanny_data = copy.deepcopy(self.gc_worker.clients.nanny.data)
        self.gc_worker.run_once()
        new_nanny_data = self.gc_worker.clients.nanny.data
        self.assertEqual(nanny_data, new_nanny_data)

    def test_run_once_x2_paused_nanny(self):
        self.gc_worker.run_once()
        # 40 days later
        with mock_now(now().timestamp() + 40 * 86400):
            self.gc_worker.run_once()
        nanny = self.gc_worker.clients.nanny
        services_left = sorted(nanny.data.keys())
        expected = sorted(self.SERVICES.keys())
        expected.pop(expected.index('offline_service'))    # became "old" in 40 days
        expected.pop(expected.index('offline_old_service-1'))
        expected.pop(expected.index('offline_old_service-2'))
        expected.pop(expected.index('offline_old_service_with_active_snapshots'))
        self.assertEqual(services_left, expected)

    def test_run_once_x3_paused_nanny(self):
        self.gc_worker.run_once()
        # 40 days later
        with mock_now(now().timestamp() + 40 * 86400):
            self.gc_worker.run_once()
        nanny_data = copy.deepcopy(self.gc_worker.clients.nanny.data)
        # 40 more days later
        with mock_now(now().timestamp() + 80 * 86400):
            self.gc_worker.run_once()
        new_nanny_data = self.gc_worker.clients.nanny.data
        self.assertEqual(nanny_data, new_nanny_data)

    def test_run_once_not_authorized(self):
        with self.gc_worker.clients.nanny.mock_auth('not-authorized'):
            self.gc_worker.run_once()
            self.gc_worker.run_once()   # would remove service with snapshots if not for auth
        nanny = self.gc_worker.clients.nanny
        services_left = sorted(nanny.data.keys())
        expected = sorted(self.SERVICES.keys())
        expected.pop(expected.index('offline_old_service-2'))
        self.assertEqual(services_left, expected)
