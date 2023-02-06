"""
    Test for etcd client class
"""

import pytest

from search.mon.wabbajack.libs.utils.etcd_client import EtcdClient
from search.mon.wabbajack.libs.utils import etcd_avail_test


host = 'localhost'
port = 2379


@pytest.mark.skipif(not etcd_avail_test.etcd_up(host, port),
                    reason='etcd server is not running on this machine.')
class TestEtcdPut:
    def setup_class(self):
        self.etcd_client = EtcdClient()

    def put(self, uuid, data):
        return self.etcd_client.put(uuid=uuid, data=data)

    def test_return_type(self):
        return_value = self.put(91206998, {'key': 'value'})
        assert isinstance(return_value, tuple)

    def test_return_value(self):
        return_value = self.put(91206998, {'key': 'value'})
        assert return_value == (False, None)

    def test_raise_errors(self):
        with pytest.raises(Exception) as e:
            self.put(123123123, {'a': 'a', 'a': 'a'})
            assert isinstance(e, Exception)

    def teardown_class(self):
        self.etcd_client.delete(91206998)
        self.etcd_client.delete(123123123)


@pytest.mark.skipif(not etcd_avail_test.etcd_up(host, port),
                    reason='etcd server is not running on this machine.')
class TestEtcdGet:
    def setup_class(self):
        self.etcd_client = EtcdClient()
        self.suspected_result = {'key': 'value'}
        self.etcd_client.put(91206998, {'key': 'value'})

    def test_return_type(self):
        assert isinstance(self.etcd_client.get(91206998), dict)

    def test_return_value(self):
        assert self.etcd_client.get(91206998)['key'] == 'value'

    def teardown_class(self):
        self.etcd_client.delete(91206998)
