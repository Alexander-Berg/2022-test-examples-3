import pytest

from search.mon.wabbajack.libs.utils import etcd_avail_test

host = 'localhost'
port = 2379


def test_etcd_avail_test_return_types():
    # assert isinstance(etcd_avail_test.get_host_port(), tuple)
    assert isinstance(etcd_avail_test.etcd_up(host, port), bool)

