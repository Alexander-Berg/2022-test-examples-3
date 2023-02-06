#!/usr/bin/python
# -*- coding: utf-8 -*-

import context
import os
import time
import unittest
import logging
import re
from collections import namedtuple

from reductor import reductor
reductor.CLOSE_FIREWALL_SLEEP = 0
reductor.WAITING_FOR_SOMETHING_SLEEP = 0

logging.basicConfig(filename='/dev/null')


CONFIG_DATA = '''\
{
    "dcgroups": {
        "market_corba-stable@eto": {
            "failures_threshold": "2",
            "hosts": {
                "gravicapa01c.yandex.ru": {
                    "cluster": 0,
                    "dists": {
                        "marketcorba": {}
                    },
                    "name": "gravicapa01c.yandex.ru",
                    "redundancy": 1,
                    "service": "marketcorba"
                },
                "gravicapa02c.yandex.ru": {
                    "cluster": 1,
                    "dists": {
                        "marketcorba": {}
                    },
                    "name": "gravicapa02c.yandex.ru",
                    "redundancy": 1,
                    "service": "marketcorba"
                },
                "gravicapa03c.yandex.ru": {
                    "cluster": 2,
                    "dists": {
                        "marketcorba": {}
                    },
                    "name": "gravicapa03c.yandex.ru",
                    "redundancy": 1,
                    "service": "marketcorba"
                },
                "gravicapa04c.yandex.ru": {
                    "cluster": 3,
                    "dists": {
                        "marketcorba": {}
                    },
                    "name": "gravicapa04c.yandex.ru",
                    "redundancy": 1,
                    "service": "marketcorba"
                }
            },
            "simultaneous_restart": "2"
        }
    },
    "download_timeout": "10",
    "publish_timeout": "1200",
    "reload_timeout": "0"
}
'''


CONFIG_NAME = 'test_config'


class DummyBackend(reductor.Backend):
    def __init__(self, hostname, datacenter=None, port=None, timeout=None):
        reductor.Backend.__init__(self, hostname, datacenter, port, timeout)
        self.port = port if port else 9002

    def do(self, command):
        logging.debug('DummyBackend.do %s on %s:%i', command, self.hostname, self.port)
        return 'fake ok'


class BackendWithFail(reductor.Backend):

    failed_command = ''
    failed_hostnames = set()

    def __init__(self, hostname, datacenter=None, port=None, timeout=None):
        reductor.Backend.__init__(self, hostname, datacenter, port, timeout)
        self.port = port if port else 9002

    def do(self, command):
        logging.debug('DummyBackend.do %s on %s:%i', command, self.hostname, self.port)
        if command.startswith(self.failed_command) and self.hostname in self.failed_hostnames:
            logging.debug('!!! no ok %s on %s', command, self.hostname)
            return '! not ok'
        return 'fake ok'


class TestReductorUpload(unittest.TestCase):
    UPLOAD = 'upload 20140904_1837'

    def setUp(self):
        open(CONFIG_NAME, 'w').write(CONFIG_DATA)

    def tearDown(self):
        os.unlink(CONFIG_NAME)

    def test_successful(self):
        rd = reductor.Reductor(CONFIG_NAME, backend_factory=DummyBackend)
        rv = rd.onecmd(self.UPLOAD)
        self.assertEqual(rv, 0)
        for group in rd.groups.itervalues():
            self.assertTrue(group.is_upload_successfull)
            for cluster in group.clusters.itervalues():
                self.assertTrue(cluster.is_upload_successfull)

    def test_fail_1(self):
        BackendWithFail.failed_command = 'marketcorba start_download marketcorba'
        BackendWithFail.failed_hostnames = ['gravicapa01c.yandex.ru']
        rd = reductor.Reductor(CONFIG_NAME, backend_factory=BackendWithFail)
        rv = rd.onecmd(self.UPLOAD)
        self.assertEqual(rv, 0)
        group = rd.groups.values()[0]
        self.assertTrue(group.is_upload_successfull)
        self.assertFalse(group.clusters[0].is_upload_successfull)
        self.assertTrue(group.clusters[1].is_upload_successfull)
        self.assertTrue(group.clusters[2].is_upload_successfull)
        self.assertTrue(group.clusters[3].is_upload_successfull)

    def test_fail_2(self):
        BackendWithFail.failed_command = 'marketcorba start_download marketcorba'
        BackendWithFail.failed_hostnames = ['gravicapa01c.yandex.ru', 'gravicapa02c.yandex.ru']
        rd = reductor.Reductor(CONFIG_NAME, backend_factory=BackendWithFail)
        rv = rd.onecmd(self.UPLOAD)
        self.assertEqual(rv, 11)
        group = rd.groups.values()[0]
        self.assertFalse(group.is_upload_successfull)
        self.assertFalse(group.clusters[0].is_upload_successfull)
        self.assertFalse(group.clusters[1].is_upload_successfull)
        self.assertTrue(group.clusters[2].is_upload_successfull)
        self.assertTrue(group.clusters[3].is_upload_successfull)

    def test_fail_timeout_1(self):
        BackendWithFail.failed_command = 'marketcorba is_download_finished'
        BackendWithFail.failed_hostnames = ['gravicapa01c.yandex.ru']
        rd = reductor.Reductor(CONFIG_NAME, backend_factory=BackendWithFail)
        rv = rd.onecmd(self.UPLOAD)
        self.assertEqual(rv, 0)
        group = rd.groups.values()[0]
        self.assertTrue(group.is_upload_successfull)
        self.assertFalse(group.clusters[0].is_upload_successfull)
        self.assertTrue(group.clusters[1].is_upload_successfull)
        self.assertTrue(group.clusters[2].is_upload_successfull)
        self.assertTrue(group.clusters[3].is_upload_successfull)

    def test_fail_timeout_2(self):
        BackendWithFail.failed_command = 'marketcorba is_download_finished'
        BackendWithFail.failed_hostnames = ['gravicapa01c.yandex.ru', 'gravicapa02c.yandex.ru']
        rd = reductor.Reductor(CONFIG_NAME, backend_factory=BackendWithFail)
        rv = rd.onecmd(self.UPLOAD)
        self.assertEqual(rv, 11)
        group = rd.groups.values()[0]
        self.assertFalse(group.is_upload_successfull)
        self.assertFalse(group.clusters[0].is_upload_successfull)
        self.assertFalse(group.clusters[1].is_upload_successfull)
        self.assertTrue(group.clusters[2].is_upload_successfull)
        self.assertTrue(group.clusters[3].is_upload_successfull)


class TestReductorSwitch(unittest.TestCase):
    def setUp(self):
        open(CONFIG_NAME, 'w').write(CONFIG_DATA)

    def tearDown(self):
        os.unlink(CONFIG_NAME)

    def test_successful(self):
        rd = reductor.Reductor(CONFIG_NAME, backend_factory=DummyBackend)
        rv = rd.onecmd('switch')
        self.assertEqual(rv, 0)
        for group in rd.groups.itervalues():
            self.assertTrue(group.is_reload_successfull)
            for cluster in group.clusters.itervalues():
                self.assertTrue(cluster.is_reload_successfull)

    def test_fail_first_host(self):
        BackendWithFail.failed_command = 'marketcorba check'
        BackendWithFail.failed_hostnames = ['gravicapa01c.yandex.ru']
        rd = reductor.Reductor(CONFIG_NAME, backend_factory=BackendWithFail)
        rv = rd.onecmd('switch')
        self.assertEqual(rv, 0)

    def test_fail_last_host(self):
        BackendWithFail.failed_command = 'marketcorba check'
        BackendWithFail.failed_hostnames = ['gravicapa04c.yandex.ru']
        rd = reductor.Reductor(CONFIG_NAME, backend_factory=BackendWithFail)
        rv = rd.onecmd('switch')
        self.assertEqual(rv, 0)
        group = rd.groups.values()[0]
        self.assertTrue(group.is_reload_successfull)
        self.assertTrue(group.clusters[0].is_reload_successfull)
        self.assertTrue(group.clusters[1].is_reload_successfull)
        self.assertTrue(group.clusters[2].is_reload_successfull)
        self.assertFalse(group.clusters[3].is_reload_successfull)

    def test_fail_two_first_hosts(self):
        BackendWithFail.failed_command = 'marketcorba check'
        BackendWithFail.failed_hostnames = ['gravicapa01c.yandex.ru', 'gravicapa02c.yandex.ru']
        rd = reductor.Reductor(CONFIG_NAME, backend_factory=BackendWithFail)
        rv = rd.onecmd('switch')
        self.assertEqual(rv, 22)
        group = rd.groups.values()[0]
        self.assertFalse(group.is_reload_successfull)
        self.assertFalse(group.clusters[0].is_reload_successfull)
        self.assertFalse(group.clusters[1].is_reload_successfull)
        self.assertEqual(group.clusters[2].is_reload_successfull, None)
        self.assertEqual(group.clusters[3].is_reload_successfull, None)

    def test_fail_two_last_hosts(self):
        BackendWithFail.failed_command = 'marketcorba check'
        BackendWithFail.failed_hostnames = ['gravicapa03c.yandex.ru', 'gravicapa04c.yandex.ru']
        rd = reductor.Reductor(CONFIG_NAME, backend_factory=BackendWithFail)
        rv = rd.onecmd('switch')
        self.assertEqual(rv, 22)
        group = rd.groups.values()[0]
        self.assertFalse(group.is_reload_successfull)
        self.assertTrue(group.clusters[0].is_reload_successfull)
        self.assertTrue(group.clusters[1].is_reload_successfull)
        self.assertFalse(group.clusters[2].is_reload_successfull)
        self.assertFalse(group.clusters[3].is_reload_successfull)


################################################################################
CONFIG_DATA2 = '''\
{
    "dcgroups": {
        "market_corba-stable@eto": {
            "failures_threshold": "1",
            "hosts": {
                "gravicapa01c.yandex.ru": {
                    "cluster": 0,
                    "dists": {
                        "marketcorba": {}
                    },
                    "name": "gravicapa01c.yandex.ru",
                    "redundancy": 1,
                    "service": "marketcorba"
                },
                "gravicapa02c.yandex.ru": {
                    "cluster": 1,
                    "dists": {
                        "marketcorba": {}
                    },
                    "name": "gravicapa02c.yandex.ru",
                    "redundancy": 1,
                    "service": "marketcorba"
                }
            },
            "simultaneous_restart": "1",
            "simultaneous_dc_restart": "1"
        },
        "market_corba-stable@iva": {
            "failures_threshold": "1",
            "hosts": {
                "gravicapa01d.yandex.ru": {
                    "cluster": 0,
                    "dists": {
                        "marketcorba": {}
                    },
                    "name": "gravicapa01d.yandex.ru",
                    "redundancy": 1,
                    "service": "marketcorba"
                },
                "gravicapa02d.yandex.ru": {
                    "cluster": 1,
                    "dists": {
                        "marketcorba": {}
                    },
                    "name": "gravicapa02d.yandex.ru",
                    "redundancy": 1,
                    "service": "marketcorba"
                }
            },
            "simultaneous_restart": "1",
            "simultaneous_dc_restart": "1"
        }
    },
    "download_timeout": "1",
    "publish_timeout": "1200",
    "reload_timeout": "4"
}
'''

CONFIG_NAME2 = 'test_config2'


class BackendWithSlowReload(reductor.Backend):
    def __init__(self, hostname, datacenter=None, reload_duration=0.01, port=None, timeout=None):
        reductor.Backend.__init__(self, hostname, datacenter, port, timeout)
        self.reload_duration = reload_duration
        self.port = port if port else 9002

    def do(self, command):
        logging.debug('DummyBackend.do %s on %s:%i', command, self.hostname, self.port)
        if command == 'marketcorba reload':
            time.sleep(self.reload_duration)

        return 'fake ok'


class TestReductorSwitch2(unittest.TestCase):
    def setUp(self):
        open(CONFIG_NAME2, 'w').write(CONFIG_DATA2)

    def tearDown(self):
        os.unlink(CONFIG_NAME2)

    def test_one_dc_at_a_time(self):
        rd = reductor.Reductor(CONFIG_NAME2, backend_factory=BackendWithSlowReload)
        # start = time.time()
        rv = rd.onecmd('switch')
        # end = time.time()
        # duration = end - start

        self.assertEqual(rv, 0)
        # self.assertTrue(duration > 4 and duration < 5)
        for group in rd.groups.itervalues():
            self.assertTrue(group.is_reload_successfull)
            for cluster in group.clusters.itervalues():
                self.assertTrue(cluster.is_reload_successfull)


CONFIG_DATA_DELTA = '''\
{
    "dcgroups": {
        "fake_search@atlantis": {
            "failures_threshold": 1,
            "hosts": {
                "dharma.market.yandex.net": {
                    "cluster": 0,
                    "dists": {
                        "qbids.delta": {
                            "torrent_server_host": "dharma.market.yandex.net",
                            "torrent_server_port": "3131",
                            "delta_mode": true,
                            "failover_dist": "qbids.snapshot"
                        }
                    },
                    "name": "dharma.market.yandex.net",
                    "redundancy": 1,
                    "service": "market_qbids"
                }
            },
            "simultaneous_dc_restart": 10000,
            "simultaneous_restart": 1000000
        }
    },
    "download_timeout": "3500",
    "reload_timeout": "1300"
}
'''


CONFIG_NAME_DELTA = 'test_config_delta'


class HistoryBackend(reductor.Backend):

    EPOCH_BEGIN = '19700101_0000'
    CURRENT_GENERATION = EPOCH_BEGIN
    COMMANDS = []

    def __init__(self, hostname, datacenter=None, port=None, timeout=None):
        super(HistoryBackend, self).__init__(hostname, datacenter, port, timeout)
        self.port = port if port else 9002

    def do(self, command):
        logging.debug('DummyBackend.do %s on %s:%i', command, self.hostname, self.port)

        if -1 != command.find('get_generation'):
            return self.CURRENT_GENERATION

        self.COMMANDS.append(command)
        return 'fake ok'


class TestReductorDeltaSupport(unittest.TestCase):
    UPLOAD_1 = '20140904_185407 20140904_185404 20140904_185405 20140904_185406'
    UPLOAD_2 = '20140904_185404 20140904_185405 20140904_185406 20140904_185407'
    UPLOAD = 'upload_delta'
    RELOAD = 'reload 20141017_153803'
    RELOAD_SIMULTANEOUSLY = 'reload_simultaneously 20141017_153803'
    RELOAD_TIMEOUT = '1300'

    def setUp(self):
        open(CONFIG_NAME_DELTA, 'w').write(CONFIG_DATA_DELTA)

    def tearDown(self):
        os.unlink(CONFIG_NAME_DELTA)
        deltapath = 'delta.info'
        if os.path.exists(deltapath):
            os.unlink(deltapath)

    def _assert(self, rv):
        try:
            self.assertEqual(0, rv, 'Reductor call failed')

            self.assertEqual("market_qbids stop_all_dist_versions qbids.delta",
                             HistoryBackend.COMMANDS[0])

            self.assertEqual("market_qbids start_download qbids.delta http://dharma.market.yandex.net:3131/torrent-server/torrents/qbids.delta-20140904_185404.torrent 20140904_185404",
                             HistoryBackend.COMMANDS[1])
            self.assertEqual("market_qbids start_download qbids.delta http://dharma.market.yandex.net:3131/torrent-server/torrents/qbids.delta-20140904_185405.torrent 20140904_185405",
                             HistoryBackend.COMMANDS[2])
            self.assertEqual("market_qbids start_download qbids.delta http://dharma.market.yandex.net:3131/torrent-server/torrents/qbids.delta-20140904_185406.torrent 20140904_185406",
                             HistoryBackend.COMMANDS[3])
            self.assertEqual("market_qbids start_download qbids.delta http://dharma.market.yandex.net:3131/torrent-server/torrents/qbids.delta-20140904_185407.torrent 20140904_185407",
                             HistoryBackend.COMMANDS[4])

            self.assertEqual("market_qbids is_download_finished qbids.delta 20140904_185404",
                             HistoryBackend.COMMANDS[5])
            self.assertEqual("market_qbids is_download_finished qbids.delta 20140904_185405",
                             HistoryBackend.COMMANDS[6])
            self.assertEqual("market_qbids is_download_finished qbids.delta 20140904_185406",
                             HistoryBackend.COMMANDS[7])
            self.assertEqual("market_qbids is_download_finished qbids.delta 20140904_185407",
                             HistoryBackend.COMMANDS[8])

            self.assertEqual("market_qbids stop_download qbids.delta 20140904_185404",
                             HistoryBackend.COMMANDS[9])
            self.assertEqual("market_qbids stop_download qbids.delta 20140904_185405",
                             HistoryBackend.COMMANDS[10])
            self.assertEqual("market_qbids stop_download qbids.delta 20140904_185406",
                             HistoryBackend.COMMANDS[11])
            self.assertEqual("market_qbids stop_download qbids.delta 20140904_185407",
                             HistoryBackend.COMMANDS[12])
        finally:
            HistoryBackend.COMMANDS = []

    def _delta_info_filepath(self, generations):
        filepath = 'delta.info'
        with open(filepath, 'w') as _f:
            _f.write('snapshot = {s}\n'.format(s=max(generations.split(' '))))
            _f.write('delta = {d}\n'.format(d=generations))
        return filepath

    def test_several_delta_upload(self):
        rd = reductor.Reductor(CONFIG_NAME_DELTA, HistoryBackend, delta_info_filepath=self._delta_info_filepath(self.UPLOAD_1))
        rv = rd.onecmd(self.UPLOAD)
        self._assert(rv)

    def test_several_delta_upload2(self):
        rd = reductor.Reductor(CONFIG_NAME_DELTA, HistoryBackend, delta_info_filepath=self._delta_info_filepath(self.UPLOAD_2))
        rv = rd.onecmd(self.UPLOAD)
        self._assert(rv)

    def test_several_delta_middle(self):
        HistoryBackend.CURRENT_GENERATION = '20140904_185405'
        try:
            rd = reductor.Reductor(CONFIG_NAME_DELTA, HistoryBackend, delta_info_filepath=self._delta_info_filepath(self.UPLOAD_2))

            rv = rd.onecmd(self.UPLOAD)
            self.assertEqual(0, rv, 'Reductor call failed')

            self.assertEqual("market_qbids stop_all_dist_versions qbids.delta",
                             HistoryBackend.COMMANDS[0])
            self.assertEqual("market_qbids start_download qbids.delta http://dharma.market.yandex.net:3131/torrent-server/torrents/qbids.delta-20140904_185406.torrent 20140904_185406",
                             HistoryBackend.COMMANDS[1])
            self.assertEqual("market_qbids start_download qbids.delta http://dharma.market.yandex.net:3131/torrent-server/torrents/qbids.delta-20140904_185407.torrent 20140904_185407",
                             HistoryBackend.COMMANDS[2])
            self.assertEqual("market_qbids is_download_finished qbids.delta 20140904_185406",
                             HistoryBackend.COMMANDS[3])
            self.assertEqual("market_qbids is_download_finished qbids.delta 20140904_185407",
                             HistoryBackend.COMMANDS[4])
            self.assertEqual("market_qbids stop_download qbids.delta 20140904_185406",
                             HistoryBackend.COMMANDS[5])
            self.assertEqual("market_qbids stop_download qbids.delta 20140904_185407",
                             HistoryBackend.COMMANDS[6])
        finally:
            HistoryBackend.CURRENT_GENERATION = HistoryBackend.EPOCH_BEGIN
            HistoryBackend.COMMANDS = []

    def test_several_delta_upload3(self):

        # '20141112_143100 20141112_143101 ... 20141112_143120'
        gens = ' '.join(['20141112_1431{ss:02}'.format(ss=ss) for ss in range(21)])

        try:
            rd = reductor.Reductor(CONFIG_NAME_DELTA, HistoryBackend, delta_info_filepath=self._delta_info_filepath(gens))
            rv = rd.onecmd(self.UPLOAD)

            self.assertEqual(0, rv)
            self.assertEqual("market_qbids stop_all_dist_versions qbids.delta",
                             HistoryBackend.COMMANDS[0])
            self.assertEqual("market_qbids stop_all_dist_versions qbids.snapshot",
                             HistoryBackend.COMMANDS[1])
            self.assertEqual("market_qbids start_download qbids.snapshot http://dharma.market.yandex.net:3131/torrent-server/torrents/qbids.snapshot-20141112_143120.torrent 20141112_143120",
                             HistoryBackend.COMMANDS[2])

        finally:
            HistoryBackend.COMMANDS = []

    def test_delta_reload(self):
        try:
            rd = reductor.Reductor(CONFIG_NAME_DELTA, backend_factory=HistoryBackend)
            rv = rd.onecmd(self.RELOAD_SIMULTANEOUSLY)

            self.assertEqual(0, rv)
            self.assertEqual("market_qbids {r} {t}".format(r=self.RELOAD, t=self.RELOAD_TIMEOUT), HistoryBackend.COMMANDS[0])
            self.assertEqual("market_qbids check", HistoryBackend.COMMANDS[1])
        finally:
            HistoryBackend.COMMANDS = []

    def test_delta_failed_reload_start(self):
        BackendWithFail.failed_command = 'market_qbids {cmd}'.format(cmd=self.RELOAD)
        BackendWithFail.failed_hostnames = ['dharma.market.yandex.net']
        rd = reductor.Reductor(CONFIG_NAME_DELTA, backend_factory=BackendWithFail)
        rv = rd.onecmd(self.RELOAD_SIMULTANEOUSLY)
        self.assertEqual(33, rv)


class TestSeveralServicesInOneCluster(unittest.TestCase):
    UPLOAD = 'upload 20140904_1837'
    CONFIG_DATA = '''\
{
    "dcgroups": {
        "group1@iva": {
            "close_firewall_sleep": 10,
            "failures_threshold": 1,
            "hosts": {
                "msh01ht.market.yandex.net": {
                    "cluster": 0,
                    "dists": {
                        "search-cards": {},
                        "search-part-0": {},
                        "search-report-data": {},
                        "search-wizard": {}
                    },
                    "name": "msh01ht.market.yandex.net",
                    "redundancy": 1,
                    "service": "marketsearch3"
                },
                "msh02ht.market.yandex.net": {
                    "cluster": 1,
                    "dists": {
                        "search-cards": {},
                        "search-part-1": {},
                        "search-report-data": {},
                        "search-wizard": {}
                    },
                    "name": "msh02ht.market.yandex.net",
                    "redundancy": 1,
                    "service": "marketsearch3"
                },
                "msh03ht.market.yandex.net": {
                    "cluster": 0,
                    "dists": {
                        "search-indexarc-0": {}
                    },
                    "name": "msh03ht.market.yandex.net",
                    "redundancy": 1,
                    "service": "marketsearchsnippet"
                },
                "msh04ht.market.yandex.net": {
                    "cluster": 1,
                    "dists": {
                        "search-indexarc-0": {}
                    },
                    "name": "msh04ht.market.yandex.net",
                    "redundancy": 1,
                    "service": "marketsearchsnippet"
                }
            },
            "simultaneous_dc_restart": 10000,
            "simultaneous_restart": 1
        }
    },
    "download_timeout": "60",
    "reload_timeout": "60"
}
'''

    def setUp(self):
        open(CONFIG_NAME, 'w').write(self.CONFIG_DATA)

    def tearDown(self):
        os.unlink(CONFIG_NAME)

    def test_1(self):
        rd = reductor.Reductor(CONFIG_NAME, backend_factory=DummyBackend)
        rd.onecmd(self.UPLOAD)
        self.assertEqual(1, len(rd.groups))

        group = rd.groups['group1@iva']
        self.assertEqual(2, len(group.clusters))
        self.assertTrue(0 in group.clusters)
        self.assertTrue(1 in group.clusters)

        def get_servants(cluster):
            cluster_servants = list(cluster._servants(reductor.Mode.full))
            cluster_servants.sort(key=lambda i: i.humanname)
            return cluster_servants

        first_cluster_servants = get_servants(group.clusters[0])
        self.assertTrue(2, len(first_cluster_servants))
        self.assertTrue(first_cluster_servants[0].name == 'marketsearch3')
        self.assertTrue(first_cluster_servants[0].humanname == 'msh01ht')
        self.assertTrue(first_cluster_servants[1].name == 'marketsearchsnippet')
        self.assertTrue(first_cluster_servants[1].humanname == 'msh03ht')

        second_cluster_servants = get_servants(group.clusters[1])
        self.assertTrue(second_cluster_servants[0].name == 'marketsearch3')
        self.assertTrue(second_cluster_servants[0].humanname == 'msh02ht')
        self.assertTrue(second_cluster_servants[1].name == 'marketsearchsnippet')
        self.assertTrue(second_cluster_servants[1].humanname == 'msh04ht')


class TestStatisticInfo(unittest.TestCase):
    def make_upload_stats(self, dists):
        class Dist(object):
            def __init__(self, servant_name, backend, name, download_time):
                self.servant_name = servant_name
                self.backend = backend
                self.name = name
                self._download_time = download_time

            def download_time(self):
                return self._download_time

        Backend = namedtuple('Backend', ['hostname'])
        ClusterBase = namedtuple('Cluster', ['dists'])

        class Cluster(ClusterBase):
            def _dists(self, mode):
                return self.dists
        Group = namedtuple('Group', ['clusters'])

        backend = Backend('host1')

        dists = [Dist('servant', backend, dist[0], dist[1]) for dist in dists]

        group = Group({'cluster': Cluster(dists)})
        return reductor.make_upload_stats({'group1': group})

    def _test_choose_max_time(self):
        dists = [('search-snippet-0', 100),
                 ('search-snippet-1', 300),
                 ('search-snippet-2', 200)]

        table = self.make_upload_stats(dists)
        line = table.split('\n')[-2]
        result = re.search(r'host1\s*\|\s*300', line)
        self.assertTrue(result)

    def _test_choose_fail(self):
        dists = [('search-snippet-0', 100),
                 ('search-snippet-1', None),
                 ('search-snippet-2', 200)]

        table = self.make_upload_stats(dists)
        line = table.split('\n')[-2]
        result = re.search(r'host1\s*\|\s*FAILED', line)
        self.assertTrue(result)


class TestAlwaysSuccess(unittest.TestCase):

    def setUp(self):
        self.config = {
            "dcgroups": {
                "market_search-bk@atlantis": {
                    "failures_threshold": 1,
                    "is_always_successful": True,
                    "simultaneous_restart": 1,
                    "hosts": {
                        "atlantis@0": {
                            "cluster": 0,
                            "dists": {"dist": {}},
                            "name": "host1.yandex.ru",
                            "service": "marketcorba",
                        },
                        "atlantis@1": {
                            "cluster": 1,
                            "dists": {"dist": {}},
                            "name": "host2.yandex.ru",
                            "service": "marketcorba",
                        },
                    }
                }
            }
        }

    def test_ok(self):
        rd = reductor.Reductor(self.config, backend_factory=DummyBackend)
        rv = rd.onecmd('upload generation')
        group = rd.groups.values()[0]

        self.assertTrue(group.is_always_successful)
        self.assertEqual(group.failures_threshold, 1)
        self.assertEqual(group.simultaneous_restart, 1)

        self.assertEqual(rv, 0)
        self.assertTrue(group.is_upload_successfull)
        self.assertTrue(group._is_upload_successfull)
        self.assertTrue(group.clusters[0].is_upload_successfull)
        self.assertTrue(group.clusters[1].is_upload_successfull)

    def test_one_error(self):
        """
        2 миникластера, первый фейлится, второй нет.
        Проверяем что в случаем `is_always_successful=True`
        1) upload делается на оба миникластера, успешно только на 2-ой
        2) reload делается только на 1-ый (неуспешно), на 2-ой НЕ делается
        """
        def backend():
            backend = BackendWithFail
            backend.failed_hostnames = 'host1.yandex.ru'
            backend.failed_command = ''
            return backend

        # upload
        rd = reductor.Reductor(self.config, backend_factory=backend())
        rv = rd.onecmd('upload generation')
        group = rd.groups.values()[0]
        self.assertEqual(rv, 0)
        self.assertTrue(group.is_upload_successfull)
        self.assertTrue(group._is_upload_successfull is False)
        self.assertTrue(group.clusters[0].is_upload_successfull is False)
        self.assertTrue(group.clusters[1].is_upload_successfull is True)

        # reload
        rd = reductor.Reductor(self.config, backend_factory=backend())
        rv = rd.onecmd('reload generation')
        group = rd.groups.values()[0]
        self.assertEqual(rv, 0)
        self.assertTrue(group.is_reload_successfull)
        self.assertTrue(group._is_reload_successfull is False)
        self.assertTrue(group.clusters[0].is_reload_successfull is False)  # Failed
        self.assertTrue(group.clusters[1].is_reload_successfull is None)  # No reload


class RuntimeBackendStub(reductor.Backend):
    def __init__(self, hostname, datacenter=None, port=None, timeout=None):
        super(RuntimeBackendStub, self).__init__(hostname, datacenter, port, timeout)
        self.port = port

    def do(self, command):
        logging.debug('RuntimeBackendStub.do %s on %s:%i', command, self.hostname, self.port)
        return 'HELLO FROM YURA'


class TestRuntimeCloudSupport(unittest.TestCase):

    def setUp(self):
        self.config = {
            "dcgroups": {
                "IVA_MARKET_PROD_REPORT_GENERAL_MARKET": {
                    "failures_threshold": 1,
                    "is_always_successful": True,
                    "simultaneous_restart": 1,
                    "hosts": {
                        "iva1-1853.search.yandex.net": {
                            "cluster": 0,
                            "dists": {"dist": {}},
                            "name": "iva1-1853.search.yandex.net",
                            "service": "marketsearch3",
                            "port": 1234
                        },
                        "iva1-1854.search.yandex.net": {
                            "cluster": 1,
                            "dists": {"dist": {}},
                            "name": "iva1-1854.search.yandex.net",
                            "service": "marketsearch3",
                            "port": 5678
                        },
                    }
                }
            }
        }

    def test_ok(self):
        rd = reductor.Reductor(self.config, backend_factory=RuntimeBackendStub)
        self.assertEqual(rd.onecmd('upload generation'), 0)
        self.assertEqual(len(rd.backends), 2)
        self.assertEqual(rd.backends['iva1-1853.search.yandex.net'].port, 1234)
        self.assertEqual(rd.backends['iva1-1854.search.yandex.net'].port, 5678)


class PackageInstallerBackendStub(reductor.Backend):
    MAX_CHECKS = 2

    def __init__(self, hostname, datacenter=None, port=None, timeout=None):
        super(PackageInstallerBackendStub, self).__init__(hostname,
                                                          datacenter,
                                                          port,
                                                          timeout)
        self.failures = []
        self.started_install = False
        self.checks = 0
        self.reloaded = False
        self.port = port

    def fail(self, message):
        self.failures.append(message)
        return '! {}'.format(message)

    def do(self, command):
        parts = command.split(' ')
        service = parts[0]
        if service != 'package_installer':
            return self.fail('Wrong service: {}'.format(service))

        action = parts[1]
        if action == 'start_install':
            package, version = parts[2], parts[3]
            if package != 'report':
                return self.fail('{} is not report'.format(package))
            if version != '1.0':
                return self.fail('Bad version {}'.format(version))
            if self.started_install:
                return self.fail('Double start_install')
            self.started_install = True
            return 'ok'
        elif action == 'check':
            if not self.started_install:
                return self.fail('Check called before start_install')
            self.checks += 1
            if self.checks < self.MAX_CHECKS:
                return '! in progress'
            elif self.checks == self.MAX_CHECKS:
                self.reloaded = True
                return 'ok'
            else:
                return self.fail('Too many calls to check')
        return self.fail('Wrong command: {}'.format(action))


class TestInstallPackage(unittest.TestCase):
    HOSTS = {
        "iva1-1853.search.yandex.net": {
            "cluster": 0,
            "dists": {"report": {}},
            "name": "iva1-1853.search.yandex.net",
            "service": "package_installer",
            "port": 1234
        },
        "iva1-1854.search.yandex.net": {
            "cluster": 1,
            "dists": {"report": {}},
            "name": "iva1-1854.search.yandex.net",
            "service": "package_installer",
            "port": 5678
        },
    }

    CONFIG = {
        "dcgroups": {
            "IVA_MARKET_PROD_REPORT_GENERAL_MARKET": {
                "failures_threshold": 1,
                "is_always_successful": False,
                "simultaneous_restart": 100,
                "hosts": HOSTS
            }
        }
    }

    def setUp(self):
        self.backends = []

    def create_backend(self, hostname, datacenter=None, port=None, timeout=None):
        backend = PackageInstallerBackendStub(hostname, datacenter, port, timeout)
        self.backends.append(backend)
        return backend

    def test(self):
        _reductor = reductor.Reductor(
            self.CONFIG,
            backend_factory=self.create_backend)
        _reductor.onecmd('install_package report 1.0')
        self.assertEqual(len(self.backends), 2)
        self.assertNotEqual(self.backends[0].hostname,
                            self.backends[1].hostname)

        for backend in self.backends:
            self.assertIn(backend.hostname, self.HOSTS)
            config = self.HOSTS[backend.hostname]
            self.assertEqual(backend.port, config['port'])
            self.assertTrue(backend.reloaded)
            self.assertEquals(len(backend.failures), 0,
                              ' '.join(backend.failures))

if __name__ == '__main__':
    context.main()
