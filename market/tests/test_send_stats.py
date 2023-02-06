# coding: utf-8

import mock
import pytest
import tempfile
import time
import threading

from google.protobuf.text_format import Parse
from lib.state import master_location, send_stats, State
from library.python import resource
from market.tools.report_stats.lib.stats import parse_metrics
from market.tools.report_stats.service.proto.config_pb2 import TConfig
from mocks_for_tests import prepare_test, finalize_test, request_get_mock, send_metric_mock, solomon_mock


class TestState:
    def setup_class(cls):
        cls.patchers = [mock.patch('requests.get', side_effect=request_get_mock),
                        mock.patch('market.pylibrary.graphite.graphite.Graphite.send_metric', side_effect=send_metric_mock),
                        mock.patch('solomon.ThrottledPushApiReporter.set_value', side_effect=solomon_mock)]
        _, cls.graphite_mock, cls.solomon_mock = map(lambda p: p.start(), cls.patchers)

    def teardown_class(cls):
        map(lambda p: p.stop(), cls.patchers)

    def test_master_location(self):
        assert master_location() == 'sas' or master_location() == 'vla'

    def check_solomon_calls(self, env, metrics, solomon_mock):
        report = 'blue-report-tst' if env == 'testing' else 'blue-report'
        expected_calls = []
        for name, metric in metrics.iteritems():
            labels = {p.label: p.value for p in metric}
            labels['report'] = report
            labels['label'] = name
            expected_calls.append(mock.call('msku_count', 5, labels))
            for filter, count in (('DELIVERY', 1), ('ABO_MARKET_SKU', 2)):
                labels['filter_reason'] = filter
                expected_calls.append(mock.call('filtered', count, labels))
        solomon_mock.assert_has_calls(expected_calls, any_order=True)

    @pytest.mark.parametrize("location", ['sas', 'vla'])
    @pytest.mark.parametrize("environment", ['testing', 'production'])
    def test_send_stats(self, monkeypatch, location, environment):
        monkeypatch.setenv("SOLOMON_TOKEN", "ABCDEF098765")
        prepare_test(location, environment)

        if environment == 'testing':
            metrics_config = resource.find('test_cfg')
        else:
            metrics_config = resource.find('prod_cfg')
        metrics_config_file = tempfile.NamedTemporaryFile()
        metrics_config_file.write(metrics_config)
        metrics_config_file.flush()
        metrics = parse_metrics(metrics_config_file.name)
        metrics_config_file.close()
        with open('./cfg.pb.txt') as config_file:
            config = Parse(config_file.read(), TConfig()).BlueOffersCountConfig[0]

        stop_event = threading.Event()
        sender_thread = threading.Thread(target=send_stats, args=(stop_event, metrics, config))
        sender_thread.start()
        time.sleep(3)
        stop_event.set()
        sender_thread.join()

        TestState.graphite_mock.assert_has_calls(
            [
                mock.call('msku_count', '5'),
                mock.call('DELIVERY', '1'),
                mock.call('ABO_MARKET_SKU', '2'),
            ],
            any_order=True
        )
        self.check_solomon_calls(environment, metrics, TestState.solomon_mock)

        finalize_test()

    @pytest.mark.parametrize("location", ['sas', 'vla'])
    @pytest.mark.parametrize("environment", ['testing', 'production'])
    def test_state(self, location, environment):
        prepare_test(location, environment)

        s = State(config_filepath='./cfg.pb.txt')
        assert s.opened
        assert s.config_set

        s.close_balancer()
        assert not s.opened
        assert s.ping() == 'closed'

        s.open_balancer()
        assert s.opened
        assert s.ping() == '0;ok'

        finalize_test()

    @pytest.mark.parametrize("location", ['sas', 'vla'])
    @pytest.mark.parametrize("environment", ['testing', 'production'])
    def test_state_object_change(self, location, environment):
        prepare_test(location, environment)

        s = State(filepath='./state.json', config_filepath='./cfg.pb.txt')
        assert s.opened
        assert s.config_set
        s.dump()

        s = State(filepath='./state.json', config_filepath='./cfg.pb.txt')
        assert s.opened
        assert s.config_set
        s.close_balancer()
        assert not s.opened
        assert s.ping() == 'closed'
        s.dump()

        s = State(filepath='./state.json', config_filepath='./cfg.pb.txt')
        assert not s.opened
        assert s.config_set
        s.open_balancer()
        assert s.opened
        assert s.ping() == '0;ok'

        finalize_test()
