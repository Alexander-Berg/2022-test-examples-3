import yatest.common.network

from library.python.monlib import metric_registry
import six

from crypta.lib.python.worker_utils.stats_server import StatsServer


def test_server():
    registry = metric_registry.MetricRegistry()
    rate = registry.rate({"sensor": "rate"})
    rate.inc()

    with yatest.common.network.PortManager() as port_manager:
        port = port_manager.get_port()
        stats_server = StatsServer("localhost", port, registry)
        client = stats_server.app.test_client()
        assert "Ok" == six.ensure_str(client.get("/ping").data)

        return client.get("/metrics").json
