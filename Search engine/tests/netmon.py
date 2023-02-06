# coding: utf-8

import itertools

from components_app.api.netmon import NetmonApi
from components_app.configs.base import netmon as netmon_config
from components_app.api.netmon.constants import Network, Protocol
from components_app.tests.base import BaseApiTestCase


def get_attr_values(obj):
    return set((v for k, v in obj.__dict__.items() if not k.startswith('__') and not k.endswith('__')))


class TestNetmonApi(BaseApiTestCase):
    def __init__(self, methodName='runTest'):
        super(TestNetmonApi, self).__init__(methodName)
        self.api = NetmonApi()
        self.api.load_config(config=netmon_config)

    def test_tags(self):
        network = set()
        protocol = set()
        tags = set()
        _tags = self.api.tags()
        for tag in _tags:
            network.add(tag['network'])
            protocol.add(tag['protocol'])
            tags.add(tag['tag'])

        self.assertEqual(network, get_attr_values(Network))
        self.assertEqual(protocol, get_attr_values(Protocol))

    def test_alive_hosts(self):
        for network, protocol in itertools.product(get_attr_values(Network), get_attr_values(Protocol)):
            hosts = self.api.alive_hosts(tag='I@ALL_SEARCH', network=network, protocol=protocol)
            self.assertIsInstance(hosts, list)

    def test_hostinfo(self):
        info = self.api.hostinfo(fqdn='sas1-6201.search.yandex.net')
        self.assertNotEmptyDict(info)

    def test_dc_latest(self):
        info = self.api.dc.latest(tag="G@ALL_SEARCH", network=Network.BB6, protocol=Protocol.ICMP)
        self.assertNotEmptyDict(info)

    # TODO(got686): add other functional tests
