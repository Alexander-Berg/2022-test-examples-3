import mock
import pytest
import socket

from crypta.lib.python.graphite import sender
from crypta.lib.python.graphite.sender import schemas


def mock_socket(addresses=None, data=None, bad_addresses=None):
    class MockSocket(object):
        def __init__(self):
            self.bad_addresses = bad_addresses or []
            self.data = data

        def close(self):
            pass

        def connect(self, address):
            if address in self.bad_addresses:
                raise socket.error("Bad address: {}".format(address))
            assert addresses.pop(0) == address

        def sendall(self, data):
            assert self.data == data

    def create_connection(address, *args, **kwargs):
        socket = MockSocket()
        socket.connect(address)
        return socket

    addresses = list(addresses) if addresses else []

    return mock.patch("crypta.lib.python.graphite.sender.socket.create_connection", side_effect=create_connection)


@pytest.mark.parametrize("graphite_addresses,kwargs,metrics,sent_data", [
    (
        [("first_address", 15000)],
        {},
        [("m1", 123123, None), ("long.metric", 12937, None)],
        "one_min.socket_fqdn.m1 123123 1514374718\n"
        "one_min.socket_fqdn.long.metric 12937 1514374718"
    ),
    (
        ["second_address"],
        {},
        [],
        ""
    ),
    (
        ["third_address", "fourth_address"],
        {"schema": schemas.ONE_DAY, "fqdn": "f.q.d.n", "root_path": "root.path"},
        [("metric1", 1, 1500000000), ("metric.2", 2, 1400000000)],
        "one_day.f_q_d_n.root.path.metric1 1 1500000000\n"
        "one_day.f_q_d_n.root.path.metric.2 2 1400000000"
    ),
    (
        ["address"],
        {"timestamp": 1500000000},
        [("metric1", 1), ("metric.2", 2, 1400000000)],
        "one_min.socket_fqdn.metric1 1 1500000000\n"
        "one_min.socket_fqdn.metric.2 2 1400000000"
    )
])
def test_send_metrics(graphite_addresses, kwargs, metrics, sent_data, mock_time, mock_getfqdn):
    with mock_socket(graphite_addresses, sent_data), mock_time, mock_getfqdn:
        graphite_sender = sender.GraphiteSender(graphite_addresses)
        graphite_sender.send_metrics(metrics, **kwargs)


@pytest.mark.parametrize("metric,value,timestamp,kwargs,sent_data", [
    (
        "m1", 1, 1234567890, {},
        "one_min.socket_fqdn.m1 1 1234567890"
    ),
    (
        "even.longer.metric", 3, None,
        {"schema": schemas.FIVE_MIN, "fqdn": "f.q.d.n", "root_path": "root.path"},
        "five_min.f_q_d_n.root.path.even.longer.metric 3 1514374718"
    )
])
def test_send_metric(metric, value, timestamp, kwargs, sent_data, mock_time, mock_getfqdn):
    with mock_socket([sender.DEFAULT_ADDRESS], sent_data), mock_time, mock_getfqdn:
        graphite_sender = sender.GraphiteSender()
        graphite_sender.send_metric(metric, value, timestamp, **kwargs)


@pytest.mark.parametrize("graphite_addresses,bad_address_index", [
    ([("bad.address", 80), ("worse.address", 80)], [0, 1]),
    ([("good.address", 80), ("bad.address", 80)], [1]),
])
def test_fail_sending_to_hosts(graphite_addresses, bad_address_index, mock_time, mock_getfqdn):
    data = ("metric", 10)
    sent_data = "one_min.socket_fqdn.metric 10 1514374718"
    bad_addresses = [graphite_addresses[i] for i in bad_address_index]

    with mock_socket(graphite_addresses, sent_data, bad_addresses), mock_time, mock_getfqdn:
        graphite_sender = sender.GraphiteSender(graphite_addresses)

        with pytest.raises(socket.error):
            graphite_sender.send_metric(*data)
