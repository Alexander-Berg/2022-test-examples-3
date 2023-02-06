# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from six import PY2, PY3
from unittest import skipUnless

if PY2:
    import mock
else:
    import unittest.mock as mock

from search.priemka.yappy.proto.structures.conf_pb2 import YappyConfiguration, MetricsCollectorConfiguration
from search.priemka.yappy.src.model.model_service.workers.metrics_collector import BaseMetricsCollector

from search.priemka.yappy.tests.utils.test_cases import TestCase


class SchedulerCreatorCollectMetricsTest(TestCase):
    mock_active = None

    @classmethod
    def setUpClass(cls):
        super(SchedulerCreatorCollectMetricsTest, cls).setUpClass()
        cls.collector = BaseMetricsCollector()
        cls.active_patch = mock.patch.object(cls.collector.__class__, 'active', new_callable=mock.PropertyMock)
        cls.collect_patch = mock.patch.object(cls.collector, 'collect_metrics')
        cls.metrics_patch = mock.patch.object(cls.collector, 'metrics')
        cls.raft_patch = mock.patch.object(cls.collector.__class__, 'raft', new_callable=mock.PropertyMock)
        cls.save_patch = mock.patch.object(cls.collector, 'save_metrics')
        cls.should_run_patch = mock.patch.object(cls.collector, 'should_run', return_value=True)
        cls.sync_obj_patch = mock.patch.object(cls.collector.__class__, 'sync_obj', new_callable=mock.PropertyMock)

    def setUp(self):
        self.collector.metric_names.clear()
        self.mock_active = self.active_patch.start()
        self.mock_active.return_value = True
        self.collect_patch.start()
        self.metrics_patch.start()
        self.raft_patch.start()
        self.save_patch.start()
        self.should_run_patch.start()
        self.sync_obj_patch.start()
        self.session = mock.Mock()
        self.addCleanup(self.active_patch.stop)
        self.addCleanup(self.collect_patch.stop)
        self.addCleanup(self.metrics_patch.stop)
        self.addCleanup(self.raft_patch.stop)
        self.addCleanup(self.save_patch.stop)
        self.addCleanup(self.should_run_patch.stop)
        self.addCleanup(self.sync_obj_patch.stop)

    def mock_getters(self, n):
        # type: (int) -> list[mock.Mock]
        return [mock.Mock(return_value=[('metric_name_{}'.format(i), i)]) for i in range(n)]

    def test_collect_call_getters_with_args(self):
        self.collect_patch.stop()
        n = 5
        manager = mock.Mock()
        mock_getters = self.mock_getters(n)
        for i in range(n):
            manager.attach_mock(mock_getters[i], 'getter_{}'.format(i))
        getter_args = {
            mock_getters[i]: {'arg_{}'.format(i): 'val_{}'.format(i)}
            for i in range(n - 1)
        }
        expected = [
            getattr(mock.call, 'getter_{}'.format(i))(self.session, **getter_args.get(mock_getters[i], {}))
            for i in range(n)
        ]
        with mock.patch.object(self.collector.__class__, 'getter_args', new_callable=mock.PropertyMock) as args:
            args.return_value = getter_args
            self.collector.collect_metrics(mock_getters, self.session)
        result = manager.mock_calls
        self.assertEqual(result, expected)

    def test_save_set_metric_values(self):
        self.save_patch.stop()
        n = 5
        mock_getters = self.mock_getters(n)
        return_values = []
        metric_sets = []
        for getter in mock_getters:
            return_values += getter.return_value
            metric_sets.append(getter.return_value)
        self.collector.metric_getters = mock_getters
        expected = [
            mock.call(metric, val)
            for metric, val in [return_value for return_value in return_values]
        ]
        self.collector.save_metrics(metric_sets)
        result = self.collector.metrics.set_metric_value.call_args_list
        self.assertEqual(result, expected)

    def test_save_metric_name(self):
        self.save_patch.stop()
        mock_getters = self.mock_getters(3)
        return_values = []
        metric_sets = []
        for getter in mock_getters:
            return_values += getter.return_value
            metric_sets.append(getter.return_value)
        expected = set([name for name, value in return_values])
        self.collector.save_metrics(metric_sets)
        self.assertEqual(self.collector.metric_names, expected)

    def test_sleep_interval_arg(self):
        expected = 1432134
        config = YappyConfiguration()
        config.model.metrics_collectors.extend([MetricsCollectorConfiguration(sleep_interval=0)])
        collector = BaseMetricsCollector(sleep_interval=expected, config=config)
        self.assertEqual(collector.sleep_interval, expected)

    def test_sleep_interval_config_default(self):
        expected = 2345134
        config = YappyConfiguration()
        config.model.metrics_collectors.extend([MetricsCollectorConfiguration(sleep_interval=expected)])
        with mock.patch.object(BaseMetricsCollector, 'SLEEP_INTERVAL', new_callable=mock.PropertyMock) as class_var:
            class_var.return_value = 222222
            collector = BaseMetricsCollector(config=config)
        self.assertEqual(collector.sleep_interval, expected)

    def test_sleep_interval_config_typed(self):
        expected = 2345134
        ctype = MetricsCollectorConfiguration.Type.SLOW
        config = YappyConfiguration()
        config.model.metrics_collectors.extend([
            MetricsCollectorConfiguration(sleep_interval=0),
            MetricsCollectorConfiguration(sleep_interval=expected, type=ctype),
        ])
        with \
                mock.patch.object(BaseMetricsCollector, 'SLEEP_INTERVAL', new_callable=mock.PropertyMock) as class_var,\
                mock.patch.object(BaseMetricsCollector, 'TYPE', new_callable=mock.PropertyMock) as collector_type:
            class_var.return_value = 222222
            collector_type.return_value = ctype
            collector = BaseMetricsCollector(config=config)
        self.assertEqual(collector.sleep_interval, expected)

    def test_sleep_interval_class(self):
        expected = 76579
        config = YappyConfiguration()
        config.model.metrics_collectors.extend([MetricsCollectorConfiguration(sleep_interval=expected)])
        with mock.patch.object(BaseMetricsCollector, 'SLEEP_INTERVAL', new_callable=mock.PropertyMock) as class_var:
            class_var.return_value = expected
            collector = BaseMetricsCollector(config=config)
        self.assertEqual(collector.sleep_interval, expected)

    def test_run_collects_metrics_if_should(self):
        self.collector._run()
        self.collector.collect_metrics.assert_called()

    def test_run_dont_collect_metrics_if_should_not(self):
        self.collector.should_run.return_value = False
        self.mock_active.return_value = False
        self.collector._run()
        self.collector.collect_metrics.assert_not_called()

    def test_should_run_if_leader(self):
        self.should_run_patch.stop()
        self_node = leader_node = mock.Mock()
        self.collector.sync_obj.getStatus.return_value = {'leader': leader_node, 'self': self_node}
        self.assertTrue(self.collector.should_run())

    def test_should_not_run_if_not_leader(self):
        self.should_run_patch.stop()
        self_node = mock.Mock('self')
        leader_node = mock.Mock('leader')
        self.collector.sync_obj.getStatus.return_value = {'leader': leader_node, 'self': self_node}
        self.assertFalse(self.collector.should_run())

    @mock.patch('search.priemka.yappy.src.model.model_service.workers.metrics_collector.session_scope')
    def _test_clear_metrics(self, *mocks):
        self.metrics_patch.stop()
        self.collector.metric_names = {'name-1', 'name-2'}
        expected = [
            mock.call(self.collector.metrics.get_metric_name(name)[1], None)
            for name in self.collector.metric_names
        ]
        with mock.patch.object(self.collector.metrics, 'pop') as pop:
            self.collector.clear_metrics()
            return pop.call_args_list, expected

    @skipUnless(PY2, 'py2 only')
    def test_clear_metrics_py2(self):
        self.assertItemsEqual(*self._test_clear_metrics())

    @skipUnless(PY3, 'py3 only')
    def test_clear_metrics_py3(self):
        self.assertCountEqual(*self._test_clear_metrics())

    def test_double_deactivation_clears_once(self):
        self.active_patch.stop()
        self.collector.activate()
        with mock.patch.object(self.collector, 'clear_metrics') as clear_metrics:
            self.collector.deactivate()
            self.collector.deactivate()
            clear_metrics.assert_called_once()

    def test_deactivate_if_not_run(self):
        self.collector.should_run.return_value = False
        with mock.patch.object(self.collector, 'deactivate') as deactivate:
            self.collector._run()
            deactivate.assert_called()

    def test_activate_if_run(self):
        self.collector.should_run.return_value = True
        with mock.patch.object(self.collector, 'activate') as activate:
            self.collector._run()
            activate.assert_called()
