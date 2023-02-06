import mock
import pytest

from crypta.lib.python.graphite import sender
from crypta.lib.python.graphite.sender import schemas
import conftest

TOKEN = "token"


def mock_swagger(requests):
    class MockSwagger(object):
        def __init__(self, url, token):
            self.metric = MockAPIMetric(requests)
            assert TOKEN == token

    class MockAPIMetric(object):
        def __init__(self, requests):
            self.requests = requests

        def report(self, frequency, hostname, group, name, value, timestamp):
            request = self.requests.pop(0)
            assert request == (frequency, hostname, group, name, value, timestamp)
            return MockReport()

    class MockReport(object):
        def __init__(self):
            pass

        def result(self, **kwargs):
            pass

    return mock.patch("crypta.lib.python.graphite.sender.swagger", side_effect=MockSwagger)


@pytest.mark.parametrize("kwargs,metrics,requests", [
    (
        {"root_path": "root.path"},
        [("m1", 123123), ("long.metric", 12937, None)],
        [
            ("ONE_MIN", conftest.NORMALIZED_FQDN, "root.path", "m1", 123123, conftest.TIMESTAMP_INT),
            ("ONE_MIN", conftest.NORMALIZED_FQDN, "root.path", "long.metric", 12937, conftest.TIMESTAMP_INT)
        ]
    ),
    (
        {"root_path": "root.path"},
        [],
        []
    ),
    (
        {"fqdn": "f.q.d.n", "root_path": "root.path"},
        [("metric1", 1, 1500000000), ("metric.2", 2, 1400000000)],
        [
            ("ONE_MIN", "f_q_d_n", "root.path", "metric1", 1, 1500000000),
            ("ONE_MIN", "f_q_d_n", "root.path", "metric.2", 2, 1400000000),
        ]
    ),
    (
        {"fqdn": "f.q.d.n", "schema": schemas.ONE_SEC, "root_path": "root.path"},
        [("metric1", 1, 1500000000), ("metric.2", 2, 1400000000)],
        [
            ("ONE_SEC", "f_q_d_n", "root.path", "metric1", 1, 1500000000),
            ("ONE_SEC", "f_q_d_n", "root.path", "metric.2", 2, 1400000000),
        ]
    ),
    (
        {"root_path": "root.path", "timestamp": 1500000000},
        [("metric1", 1), ("metric.2", 2, 1400000000)],
        [
            ("ONE_MIN", conftest.NORMALIZED_FQDN, "root.path", "metric1", 1, 1500000000),
            ("ONE_MIN", conftest.NORMALIZED_FQDN, "root.path", "metric.2", 2, 1400000000)
        ]
    )
])
def test_send_metrics(kwargs, metrics, requests, mock_time, mock_getfqdn):
    with mock_swagger(requests), mock_time, mock_getfqdn:
        graphite_sender = sender.CryptaAPIGraphiteSender(TOKEN)
        graphite_sender.send_metrics(metrics, **kwargs)
        assert requests == []


def test_send_metrics_fail_with_no_root_path(mock_time, mock_getfqdn):
    with mock_swagger([]), mock_time, mock_getfqdn:
        graphite_sender = sender.CryptaAPIGraphiteSender(TOKEN)

        with pytest.raises(Exception):
            graphite_sender.send_metrics(("name", 1))
