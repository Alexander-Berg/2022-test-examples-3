from __future__ import absolute_import
import os

import mock

from crypta.lib.python.yt import metrics


def test_process_yt_metrics():
    os.environ["YT_TOKEN"] = "FAKE"

    config = [
        metrics.YtMetricConfig("proxy", "root", "path", "env1", "project", False),
        metrics.YtMetricConfig("proxy2", "root", "path", "env2", "project", True),
    ]

    def get_metrics(yt_client, node):
        return {"metric": 1}

    class MockSolomonClient(object):
        uploaded = []

        def __init__(self, push_interval, cluster, project):
            assert push_interval == 1
            self.cluster = cluster
            self.project = project

        def set_value(self, sensor, value, labels):
            self.uploaded.append((sensor, value, labels, self.cluster, self.project))

    class MockYtClient(object):
        def __init__(self, *args):
            pass

        def exists(self, path):
            return True

    with mock.patch("crypta.lib.python.yt.metrics.yt_helpers.get_yt_client", side_effect=MockYtClient),\
            mock.patch(
                "crypta.lib.python.yt.metrics.reporter.create_throttled_solomon_reporter",
                side_effect=MockSolomonClient):
        metrics.process_yt_metrics(
            config,
            get_metrics,
            {},
        )

    return MockSolomonClient.uploaded
