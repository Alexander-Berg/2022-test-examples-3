# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import os
import six

from mock import patch

from infra.yp_service_discovery.api.api_pb2 import TRspResolveEndpoints

from search.martylib.sd import ServiceDiscoveryClient

from search.martylib.test_utils import TestCase

if six.PY2:
    from thread import error as LockReleaseError
else:
    LockReleaseError = RuntimeError

class SDClientTestCase(TestCase):
    lock_patch = patch('search.martylib.sd.client.InterProcessLock')

    def setUp(self):
        self.mocked_lock = self.lock_patch.start()
        # reset Singleton
        ServiceDiscoveryClient._instances.pop(ServiceDiscoveryClient, None)

    def tearDown(self):
        sdc = ServiceDiscoveryClient()
        if os.path.exists(sdc.path):
            os.remove(sdc.path)
        try:
            sdc.ipc_lock.release()
        except LockReleaseError:
            pass
        if os.path.exists(sdc.ipc_lock_path):
            os.remove(sdc.ipc_lock_path)

        patch.stopall()

    def test_locked_start_ok(self):
        self.mocked_lock.acquire.return_value = False
        try:
            ServiceDiscoveryClient()
        except Exception as err:
            self.fail('unexpected exception: {}'.format(err))

    def test_locked_dump_not_fails(self):
        sdc = ServiceDiscoveryClient()
        sdc.ipc_lock.acquired = False
        try:
            sdc.dump()
        except Exception as err:
            self.fail('unexpected exception: {}'.format(err))

    def test_locked_dump_skipped(self):
        sdc = ServiceDiscoveryClient()
        sdc.ipc_lock.acquired = False
        with patch('search.martylib.sd.client.open') as open:
            import logging
            logging.getLogger('martylib').error('lock.acquired: %s', sdc.ipc_lock.acquired)
            sdc.dump()
            open.assert_not_called()

    def test_custom_path_lock(self):
        fname = 'custom_path.sd_cache'
        sdc = ServiceDiscoveryClient(cache_file_path='custom_path.sd_cache')
        self.assertIn(fname, sdc.ipc_lock_path)

    def test_functional(self):
        patch.stopall()
        sdc = ServiceDiscoveryClient(cache_file_path='dummy_cache_file')
        cluster = 'cluster'
        es = 'endpoint_set'

        expected = TRspResolveEndpoints(host='expected.host')
        with patch.object(sdc.resolver, 'resolve_endpoints', return_value=expected):
            sdc.resolve(cluster, es)

        sdc.pop((cluster, es))
        sdc._load_cache()
        with patch.object(sdc.resolver, 'resolve_endpoints', return_value=TRspResolveEndpoints(host='unexpected')):
            result = sdc.resolve(cluster, es)

        self.assertEqual(result, expected)
