# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import json
import os

from collections import defaultdict
from copy import copy
from inspect import getmembers
from mock import Mock, patch

from search.martylib.raft import Raft
from search.martylib.raft.base import _disc_cache, _serialize
from search.martylib.test_utils import TestCase
from search.martylib.sd import ServiceDiscoveryClientMock


class TestFetchInstancesYP(TestCase):

    @classmethod
    def setUpClass(cls):
        cls.orig_sd_client = Raft.sd_client
        Raft.sd_client = ServiceDiscoveryClientMock()

        cls.orig_port = Raft.port
        Raft.port = 32321

    @classmethod
    def tearDownClass(cls):
        os.remove(Raft.DUMP_FILE)
        Raft.sd_client = cls.orig_sd_client

    def setUp(self):
        self.orig_endpoint_set = Raft.endpoint_set

    def tearDown(self):
        Raft.endpoint_set = self.orig_endpoint_set

    def test_empty_endpoint_set(self):
        Raft.endpoint_set = []
        result = Raft._fetch_instances_yp()
        self.assertEqual(result, [])

    def test_fetch_instances_yp(self):
        cluster = 'test-cluster'
        es_id = 'test-es-id'
        Raft.endpoint_set = ['{}/{}'.format(cluster, es_id)]
        result = Raft._fetch_instances_yp()
        expected = {
            'fqdn': Raft.sd_client.fqdn_pattern.format(cluster=cluster, es_id=es_id, idx=0),
            'port': Raft.port
        }
        self.assertEqual(result, ['{fqdn}:{port}'.format(**expected)])


class DiscCacheTestCase(TestCase):
    DUMP_FILE = 'dummy.dump'
    DUMP_FILE_ATTR = 'dummy_dummy.dump'

    class DummyException(Exception):
        """ Dummy test exception """

    def tearDown(self):
        if hasattr(self.__class__, '_wrapped'):
            delattr(self.__class__, '_wrapped')

        for f in (self.DUMP_FILE, self.DUMP_FILE_ATTR):
            if os.path.exists(f):
                os.remove(f)

    @staticmethod
    def dummy(bound_obj=None):
        return {"dummy": "json"}

    @staticmethod
    def dummy_fail(bound_obj=None):
        raise DiscCacheTestCase.DummyException()

    def wrap(self, func, *args, **kwargs):
        return _disc_cache(*args, **kwargs)(func)

    def call_as_bound_wrapped(self, func, *args, **kwargs):
        """ Wrap `func` into `_disc_cache` and bounds result to this test case instance before call """
        setattr(self.__class__, '_wrapped', self.wrap(func, *args, **kwargs))
        return self._wrapped()

    def call_as_unbound_wrapped(self, func, *args, **kwargs):
        """ Wrap `func` into `_disc_cache` before call """
        return self.wrap(func, *args, **kwargs)()

    def test_fname_write(self):
        self.call_as_bound_wrapped(self.dummy, self.DUMP_FILE)
        with open(self.DUMP_FILE) as fd:
            result = json.load(fd)
        self.assertEqual(result, self.dummy())

    def test_fname_read(self):
        self.call_as_bound_wrapped(self.dummy, self.DUMP_FILE)
        result = self.call_as_bound_wrapped(self.dummy_fail, self.DUMP_FILE)
        self.assertEqual(result, self.dummy())

    def test_fname_attr_write_bound(self):
        self.call_as_bound_wrapped(self.dummy, filename_attr='DUMP_FILE_ATTR')
        with open(self.DUMP_FILE_ATTR) as fd:
            result = json.load(fd)
        self.assertEqual(result, self.dummy())

    def test_fname_attr_read_bound(self):
        self.call_as_bound_wrapped(self.dummy, filename_attr='DUMP_FILE_ATTR')
        result = self.call_as_bound_wrapped(self.dummy_fail, filename_attr='DUMP_FILE_ATTR')
        self.assertEqual(result, self.dummy())

    def test_fname_attr_unbound(self):
        self.assertRaises(
            ValueError,
            self.call_as_unbound_wrapped,
            self.dummy,
            filename_attr='DUMP_FILE',
        )

    def test_fname_unbound(self):
        try:
            self.call_as_unbound_wrapped(self.dummy, self.DUMP_FILE)
        except (IndexError, ValueError) as err:
            self.fail('unexpected exception: {}'.format(err))


class RaftTestCase(TestCase):
    N_INSTANCES = 3

    DUMMY_PREPARE_ARGS = {'endpoint_set': ['CLUS/set-name']}

    INITIAL_ATTRS = {
        key: copy(val)
        for key, val in getmembers(Raft)
        if not (key == 'metrics' or key.startswith('_') or callable(val))
    }

    SYNC_OBJ = Mock()

    @classmethod
    def setUpClass(cls):
        patch.object(Raft, '_fetch_instances_yp', side_effect=cls.mock_instances).start()
        patch.object(Raft, '_fetch_instances_nanny', side_effect=cls.mock_instances).start()
        patch('search.martylib.raft.base.SyncObj', return_value=cls.SYNC_OBJ).start()
        cls.SYNC_OBJ.getStatus.return_value = defaultdict(
            lambda: 0,
            leader=Mock(address='leader.fqdn:7000'),
        )

    @classmethod
    def tearDownClass(cls):
        cls.reset_raft_attrs()
        patch.stopall()

    def setUp(self):
        self.reset_raft_attrs()

    @classmethod
    def reset_raft_attrs(cls):
        for attr, val in cls.INITIAL_ATTRS.items():
            setattr(Raft, attr, val)
        Raft.metrics.clear()
        Raft.metrics._histogram_map.clear()

    @classmethod
    def mock_instances(cls):
        return [
            'instance-{}.fqdn:{}'.format(i, Raft.port)
            for i in range(cls.N_INSTANCES)
        ]


class PrepareTestCase(RaftTestCase):
    def test_port(self):
        port = 123123123
        Raft.prepare(port=port, **self.DUMMY_PREPARE_ARGS)
        self.assertEqual(Raft.port, port)

    def test_dump_file(self):
        port = 123123123
        expected = self.INITIAL_ATTRS['DUMP_FILE'].replace(str(self.INITIAL_ATTRS['DEFAULT_PORT']), str(port))
        Raft.prepare(port=port, **self.DUMMY_PREPARE_ARGS)
        self.assertEqual(Raft.DUMP_FILE, expected)

    def test_double_call_new_dump_file(self):
        port_1 = 123123121111
        port_2 = 7700
        expected = self.INITIAL_ATTRS['DUMP_FILE'].replace(str(self.INITIAL_ATTRS['DEFAULT_PORT']), str(port_2))
        Raft.prepare(port=port_1, **self.DUMMY_PREPARE_ARGS)
        Raft.prepare(port=port_2, **self.DUMMY_PREPARE_ARGS)
        self.assertEqual(Raft.DUMP_FILE, expected)

    def test_default_metric_name(self):
        Raft.prepare(**self.DUMMY_PREPARE_ARGS)
        self.assertEqual(Raft.metric_name, self.INITIAL_ATTRS['metric_name'])

    def test_custom_metric_name(self):
        metric_name = 'my-metric-name'
        Raft.prepare(metric_name=metric_name, **self.DUMMY_PREPARE_ARGS)
        self.assertEqual(Raft.metric_name, metric_name)

    def test_double_call_resets_metric_name(self):
        metric_name = 'my-metric-name'
        expected = self.INITIAL_ATTRS['metric_name']
        Raft.prepare(metric_name=metric_name, **self.DUMMY_PREPARE_ARGS)
        Raft.prepare(**self.DUMMY_PREPARE_ARGS)
        self.assertEqual(Raft.metric_name, expected)


class RaftMetricsTestCase(RaftTestCase):
    METRIC_NAME = 'test-metric-name'
    PREPARE_ARGS = dict(
        metric_name=METRIC_NAME,
        additional_monitorings=True,
        **RaftTestCase.DUMMY_PREPARE_ARGS
    )

    STRICT_METRICS_ORIG = Raft.metrics.STRICT_VALIDATION

    @classmethod
    def setUpClass(cls):
        super(RaftMetricsTestCase, cls).setUpClass()
        Raft.metrics.STRICT_VALIDATION = True

    @classmethod
    def tearDownClass(cls):
        Raft.metrics.STRICT_VALIDATION = cls.STRICT_METRICS_ORIG
        super(RaftMetricsTestCase, cls).tearDownClass()

    def get_raft_hisitogram_metric(self, metric_name=None):
        for hist_name, hist in Raft.metrics._histogram_map.items():
            if hist_name and self.METRIC_NAME in hist_name and (not metric_name or metric_name in hist_name):
                return hist

    def test_prepare_adds_histogram(self):
        Raft.prepare(**self.PREPARE_ARGS)
        if not self.get_raft_hisitogram_metric():
            self.fail("no histogram matching Raft metric name ('{}') found".format(self.METRIC_NAME))

    def test_status_metrics(self):
        Raft.prepare(**self.PREPARE_ARGS)
        r = Raft()
        r.get_status()
        for metric_name in Raft.metrics.keys():
            if self.METRIC_NAME  in metric_name:
                break
        else:
            self.fail("no metric matching Raft metric name ('{}') found".format(self.METRIC_NAME))

    def test_serialize_adds_request_size_value(self):
        Raft.prepare(**self.PREPARE_ARGS)
        metric = 'request-size'
        hist = self.get_raft_hisitogram_metric(metric)
        if not hist:
            self.fail(
                "expected histogram metric for '{}' matching Raft metric name ('{}') not found"
                .format(metric, self.METRIC_NAME)
            )
        _serialize()
        for val in hist.bins_weights:
            if val > 0:
                break
        else:
            self.fail("no value was added to the Raft '{}' histogram metric".format(metric))
