# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from search.martylib.test_utils import TestCase
from search.martylib.yp_lite import YPLiteClientMock
from search.martylib.proto.structures.yp_lite_pb2 import AllocationRequest
from search.martylib.http.exceptions import NotFound


class TestYPLiteClientMock(TestCase):

    @classmethod
    def setUpClass(cls):
        cls.mock = YPLiteClientMock()

    def tearDown(self):
        self.mock.reset_mock()

    def test_create_pod(self):
        ids = self.mock.create_pod(AllocationRequest(replicas=2), 'service', 'cluster')
        expected = ['service-0', 'service-1']
        self.assertEqual(ids, expected)

    def test_create_pod_same_service(self):
        self.mock.create_pod(AllocationRequest(replicas=2), 'service', 'cluster')
        ids = self.mock.create_pod(AllocationRequest(replicas=3), 'service', 'cluster')
        expected = ['service-2', 'service-3', 'service-4']
        self.assertEqual(ids, expected)

    def test_create_pod_different_services(self):
        self.mock.create_pod(AllocationRequest(replicas=2), 'service-1', 'cluster')
        ids = self.mock.create_pod(AllocationRequest(replicas=3), 'service-2', 'cluster')
        expected = ['service-2-0', 'service-2-1', 'service-2-2']
        self.assertEqual(ids, expected)

    def test_create_pod_different_clusters(self):
        self.mock.create_pod(AllocationRequest(replicas=2), 'service', 'cluster-1')
        ids = self.mock.create_pod(AllocationRequest(replicas=3), 'service', 'cluster-2')
        expected = ['service-0', 'service-1', 'service-2']
        self.assertEqual(ids, expected)

    def test_create_pod_set(self):
        ids = self.mock.create_pod_set(AllocationRequest(replicas=2), 'service', 'cluster')
        expected = ['service-0', 'service-1']
        self.assertEqual(ids, expected)

    def test_create_pod_set_same_service(self):
        self.mock.create_pod_set(AllocationRequest(replicas=2), 'service', 'cluster')
        ids = self.mock.create_pod_set(AllocationRequest(replicas=3), 'service', 'cluster')
        expected = ['service-2', 'service-3', 'service-4']
        self.assertEqual(ids, expected)

    def test_create_pod_set_different_services(self):
        self.mock.create_pod_set(AllocationRequest(replicas=2), 'service-1', 'cluster')
        ids = self.mock.create_pod_set(AllocationRequest(replicas=3), 'service-2', 'cluster')
        expected = ['service-2-0', 'service-2-1', 'service-2-2']
        self.assertEqual(ids, expected)

    def test_create_pod_set_different_clusters(self):
        self.mock.create_pod_set(AllocationRequest(replicas=2), 'service', 'cluster-1')
        ids = self.mock.create_pod(AllocationRequest(replicas=3), 'service', 'cluster-2')
        expected = ['service-0', 'service-1', 'service-2']
        self.assertEqual(ids, expected)

    def test_remove_pod(self):
        self.mock.create_pod(AllocationRequest(replicas=3), 'service', 'cluster')
        self.mock.remove_pod('service-1', 'cluster')
        expected = {'service': ['service-0', 'service-2']}
        self.assertEqual(self.mock.mock_data['cluster'], expected)

    def test_remove_pod_different_cluster(self):
        self.mock.create_pod(AllocationRequest(replicas=3), 'service', 'cluster-1')
        self.mock.create_pod(AllocationRequest(replicas=3), 'service', 'cluster-2')
        self.mock.remove_pod('service-1', 'cluster-1')
        pods_1 = self.mock.mock_data['cluster-1']['service']
        pods_2 = self.mock.mock_data['cluster-2']['service']
        self.assertNotEqual(pods_1, pods_2)

    def test_create_pod_after_deletion(self):
        self.mock.create_pod(AllocationRequest(replicas=3), 'service', 'cluster')
        self.mock.remove_pod('service-1', 'cluster')
        ids = self.mock.create_pod(AllocationRequest(replicas=2), 'service', 'cluster')
        expected = ['service-3', 'service-4']
        self.assertEqual(ids, expected)

    def test_create_pod_after_deletion_of_last(self):
        self.mock.create_pod(AllocationRequest(replicas=3), 'service', 'cluster')
        self.mock.remove_pod('service-2', 'cluster')
        ids = self.mock.create_pod(AllocationRequest(replicas=2), 'service', 'cluster')
        expected = ['service-2', 'service-3']
        self.assertEqual(ids, expected)

    def test_remove_pod_not_found(self):
        self.mock.create_pod(AllocationRequest(replicas=1), 'service', 'cluster')
        self.assertRaises(
            NotFound,
            self.mock.remove_pod,
            'no-such-pod',
            'cluster',
        )

    def test_remove_pod_cluster_not_found(self):
        self.mock.create_pod(AllocationRequest(replicas=1), 'service', 'cluster')
        self.assertRaises(
            NotFound,
            self.mock.remove_pod,
            'service-0',
            'no-such-cluster',
        )

    def test_remove_pod_set(self):
        self.mock.create_pod_set(AllocationRequest(replicas=3), 'service-1', 'cluster')
        self.mock.create_pod_set(AllocationRequest(replicas=3), 'service-2', 'cluster')
        self.mock.remove_pod_set('service-1', 'cluster')
        expected = ['service-2']
        pod_sets = list(self.mock.mock_data['cluster'].keys())
        self.assertEqual(pod_sets, expected)

    def test_remove_pod_set_different_cluster(self):
        self.mock.create_pod_set(AllocationRequest(replicas=3), 'service', 'cluster-1')
        self.mock.create_pod_set(AllocationRequest(replicas=3), 'service', 'cluster-2')
        self.mock.remove_pod_set('service', 'cluster-1')
        pod_sets_1 = list(self.mock.mock_data['cluster-1'].keys())
        pod_sets_2 = list(self.mock.mock_data['cluster-2'].keys())
        self.assertNotEqual(pod_sets_1, pod_sets_2)

    def test_remove_pod_set_not_found(self):
        self.mock.create_pod(AllocationRequest(replicas=1), 'service', 'cluster')
        self.assertRaises(
            NotFound,
            self.mock.remove_pod_set,
            'no-such-service',
            'cluster',
        )

    def test_remove_pod_set_cluster_not_found(self):
        self.mock.create_pod(AllocationRequest(replicas=1), 'service', 'cluster')
        self.assertRaises(
            NotFound,
            self.mock.remove_pod_set,
            'service',
            'no-such-cluster',
        )
