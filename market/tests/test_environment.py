import mock
import unittest
import os
import json
import runtime_cloud.environment as rc_env

TESTING_MARKET_GENERAL_ITAGS = \
    "a_geo_vla a_itype_report VLA_MARKET_TEST_REPORT_GENERAL_MARKET cgset_memory_recharge_on_pgfault_1 " \
    "a_tier_MarketMiniClusterTier0 a_line_vla-01 a_topology_cgset-memory.low_limit_in_bytes=120259084288 " \
    "itag_replica_1 a_shard_2 a_topology_group-VLA_MARKET_TEST_REPORT_GENERAL_MARKET " \
    "a_topology_version-stable-102-r75 a_metaprj_market a_dc_vla a_prj_report-general-market a_ctype_testing " \
    "a_topology_stable-102-r75 a_topology_cgset-memory.limit_in_bytes=120363941888 use_hq_spec enable_hq_report " \
    "enable_hq_poll "

TESTING_BLUE_REPORT_ITAGS = """\
SAS_MARKET_TEST_REPORT_SINGLE_BLUE
a_ctype_testing
a_dc_sas
a_geo_sas
a_itype_marketreport
a_line_sas-1.1.3
a_metaprj_market
a_prj_report-general-blue-market
a_tier_none
cgset_memory_recharge_on_pgfault_1
use_hq_spec
enable_hq_report
enable_hq_poll
a_shard_0
itag_replica_0
"""

PRODUCTION_RED_REPORT_ITAGS = """\
SAS_MARKET_PROD_REPORT_GENERAL_RED_MARKET
a_ctype_production
a_dc_sas
a_geo_sas
a_itype_marketreport
a_line_sas-09
a_metaprj_market
a_prj_report-general-red-market
a_shard_1
a_tier_MarketMiniClusterTier0
cgset_memory_recharge_on_pgfault_1
itag_replica_5
use_hq_spec
enable_hq_report
enable_hq_poll
"""

PRODUCTION_YP_REPORT_ITAGS = """\
a_ctype_production
a_dc_sas
a_geo_sas
a_itype_marketreport
a_line_sas-09
a_metaprj_market
a_prj_report-general-market
a_tier_MarketMiniClusterTier0
cgset_memory_recharge_on_pgfault_1
use_hq_spec
enable_hq_report
enable_hq_poll
"""


def write_experiment_flags(root_dir, flags):
    controls_path = os.path.join(root_dir, 'controls')
    if not os.path.exists(controls_path):
        os.makedirs(controls_path)
    path = os.path.join(controls_path, 'experiment_flags.json')
    with open(path, 'w') as f:
        json.dump(flags, f)


class TestEnvironment(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        os.makedirs("empty_home")
        os.makedirs("dump_json_0")
        os.makedirs("dump_json_1")

        with open("dump_json_0/dump.json", "w") as fn:
            data = {'container': {'constraints': {'cpu_guarantee': '0.5c'}}}
            json.dump(data, fn)

        with open("dump_json_1/dump.json", "w") as fn:
            data = {'container': {'constraints': {'cpu_guarantee': '15.5c'}}}
            json.dump(data, fn)

    @mock.patch.dict(os.environ, {'BSCONFIG_IPORT': '10050', 'HOME': '/home/container'})
    def test_directories(self):
        self.assertEqual('/home/container', rc_env.paths.root)
        self.assertEqual('/home/container/logs', rc_env.paths.logs)
        self.assertEqual('/home/container/pdata', rc_env.paths.pdata)
        self.assertEqual('/home/container/bin', rc_env.paths.bin)
        self.assertEqual('/home/container/data', rc_env.paths.data)
        self.assertEqual('/home/container/secrets/tvmtool_token', rc_env.paths.tvmtool_token)

    @mock.patch.dict(os.environ, {'BSCONFIG_IPORT': '10050', 'HOME': '/home/container'})
    def test_service_ports(self):
        self.assertEqual(10050, rc_env.ports.root)
        self.assertEqual(10050, rc_env.ports.nginx)
        self.assertEqual(10051, rc_env.ports.report)
        self.assertEqual(10052, rc_env.ports.zeus)
        self.assertEqual(10053, rc_env.ports.backctld)
        self.assertEqual(10054, rc_env.ports.aria2)
        self.assertEqual(10055, rc_env.ports.rfsd)
        self.assertEqual(10064, rc_env.ports.tvmtool)
        self.assertEqual(10066, rc_env.ports.nginx_http2)
        self.assertEqual(10067, rc_env.ports.shapi)

    @mock.patch.dict(os.environ, {'BSCONFIG_IPORT': '10050', 'HOME': '/home/container',
                                  'BSCONFIG_ITAGS': TESTING_MARKET_GENERAL_ITAGS, 'BSCONFIG_IHOST': 'ps_host',
                                  'NANNY_SERVICE_ID': "nanny_test_id"})
    def test_testing_market_general_environment(self):
        self.assertEqual('market-report', rc_env.report.role)
        self.assertEqual('market', rc_env.report.subrole)
        self.assertEqual('report', rc_env.report.log_prefix)
        self.assertEqual('/home/container/logs/report', rc_env.report.log_dir)
        self.assertEqual('testing', rc_env.host.environment)
        self.assertEqual('vla', rc_env.host.location)
        self.assertEqual('ps_host', rc_env.host.host)
        self.assertEqual('nanny_test_id', rc_env.host.nanny_service_id)
        self.assertEqual(rc_env.torrent.DEFAULT_FSYNC_INTERVAL, rc_env.torrent.fsync_interval)
        self.assertEqual(rc_env.torrent.DEFAULT_DOWN_LIMIT, rc_env.torrent.down_limit)
        self.assertEqual(rc_env.torrent.DEFAULT_UP_LIMIT, rc_env.torrent.up_limit)
        self.assertEqual(rc_env.torrent.DEFAULT_DOWN_LIMIT, rc_env.torrent.skynet_down_limit)
        self.assertEqual(rc_env.torrent.DEFAULT_UP_LIMIT, rc_env.torrent.skynet_up_limit)
        self.assertEqual('2', rc_env.report.cluster_index)
        self.assertEqual('1', rc_env.report.host_index)
        self.assertTrue(rc_env.report.is_white_market)
        self.assertFalse(rc_env.report.is_red_market)
        self.assertFalse(rc_env.report.is_blue_market)
        self.assertEqual('/place/db/bsconfig/webcache/shm/', rc_env.paths.shm_root)

    @mock.patch.dict(os.environ, {'BSCONFIG_IPORT': '17050', 'HOME': '/home/container',
                                  'BSCONFIG_ITAGS': TESTING_BLUE_REPORT_ITAGS, 'BSCONFIG_IHOST': 'sas2-5319'})
    def test_blue_report(self):
        self.assertEqual('market-report', rc_env.report.role)
        self.assertEqual('blue-market', rc_env.report.subrole)
        self.assertEqual('blue-market_report', rc_env.report.log_prefix)
        self.assertEqual('/home/container/logs/blue-market_report', rc_env.report.log_dir)
        self.assertEqual('testing', rc_env.host.environment)
        self.assertEqual('sas', rc_env.host.location)
        self.assertEqual('sas2-5319', rc_env.host.host)
        self.assertEqual(rc_env.torrent.DEFAULT_FSYNC_INTERVAL, rc_env.torrent.fsync_interval)
        self.assertEqual(rc_env.torrent.DEFAULT_DOWN_LIMIT, rc_env.torrent.down_limit)
        self.assertEqual(rc_env.torrent.DEFAULT_UP_LIMIT, rc_env.torrent.up_limit)
        self.assertEqual(rc_env.torrent.DEFAULT_DOWN_LIMIT, rc_env.torrent.skynet_down_limit)
        self.assertEqual(rc_env.torrent.DEFAULT_UP_LIMIT, rc_env.torrent.skynet_up_limit)
        self.assertEqual('0', rc_env.report.cluster_index)
        self.assertEqual('0', rc_env.report.host_index)
        self.assertTrue(rc_env.report.is_blue_market)
        self.assertFalse(rc_env.report.is_white_market)
        self.assertFalse(rc_env.report.is_red_market)
        self.assertEqual('/place/db/bsconfig/webcache/shm/', rc_env.paths.shm_root)

    @mock.patch.dict(os.environ, {'BSCONFIG_IPORT': '17050', 'HOME': '/home/container',
                                  'BSCONFIG_ITAGS': PRODUCTION_RED_REPORT_ITAGS, 'BSCONFIG_IHOST': 'sas2-5319'})
    def test_red_report(self):
        self.assertEqual('market-report', rc_env.report.role)
        self.assertEqual('red-market', rc_env.report.subrole)
        self.assertEqual('red-market_report', rc_env.report.log_prefix)
        self.assertEqual('/home/container/logs/red-market_report', rc_env.report.log_dir)
        self.assertEqual('production', rc_env.host.environment)
        self.assertEqual('sas', rc_env.host.location)
        self.assertEqual('sas2-5319', rc_env.host.host)
        self.assertEqual(rc_env.torrent.DEFAULT_FSYNC_INTERVAL, rc_env.torrent.fsync_interval)
        self.assertEqual(rc_env.torrent.DEFAULT_DOWN_LIMIT, rc_env.torrent.down_limit)
        self.assertEqual(rc_env.torrent.DEFAULT_UP_LIMIT, rc_env.torrent.up_limit)
        self.assertEqual(rc_env.torrent.DEFAULT_DOWN_LIMIT, rc_env.torrent.skynet_down_limit)
        self.assertEqual(rc_env.torrent.DEFAULT_UP_LIMIT, rc_env.torrent.skynet_up_limit)
        self.assertEqual('1', rc_env.report.cluster_index)
        self.assertEqual('5', rc_env.report.host_index)
        self.assertFalse(rc_env.report.is_blue_market)
        self.assertFalse(rc_env.report.is_white_market)
        self.assertTrue(rc_env.report.is_red_market)
        self.assertEqual('/place/db/bsconfig/webcache/shm/', rc_env.paths.shm_root)

    @mock.patch.dict(os.environ, {'HOME': 'dump_json_0'})
    def test_cpu_limit_less_0(self):
        self.assertEqual(rc_env.host.cpu_limit, 0.5)

    @mock.patch.dict(os.environ, {'HOME': 'dump_json_1'})
    def test_cpu_limit_15(self):
        self.assertEqual(rc_env.host.cpu_limit, 15.5)

    @mock.patch.dict(os.environ, {'BSCONFIG_IPORT': '80', 'HOME': '/home/container',
                                  'BSCONFIG_IHOST': 'sas2-5319',
                                  'BSCONFIG_ITAGS': PRODUCTION_YP_REPORT_ITAGS,
                                  'NANNY_SERVICE_ID': 'nanny_test_id',
                                  'YP_POD_ID': 'market-report-yp-1',
                                  'LABELS_market_report_shard': '0',
                                  'LABELS_market_report_replica': '1',
                                  'LABELS_market_report_prj': 'report-general-market',
                                  })
    def test_yp_report(self):
        self.assertEqual('market-report', rc_env.report.role)
        self.assertEqual('market', rc_env.report.subrole)
        self.assertEqual('report', rc_env.report.log_prefix)
        self.assertEqual('/home/container/logs/report', rc_env.report.log_dir)
        self.assertEqual('production', rc_env.host.environment)
        self.assertEqual('sas', rc_env.host.location)
        self.assertEqual('sas2-5319', rc_env.host.host)
        self.assertEqual('nanny_test_id', rc_env.host.nanny_service_id)
        self.assertEqual(rc_env.torrent.DEFAULT_FSYNC_INTERVAL, rc_env.torrent.fsync_interval)
        self.assertEqual(rc_env.torrent.DEFAULT_DOWN_LIMIT, rc_env.torrent.down_limit)
        self.assertEqual(rc_env.torrent.DEFAULT_UP_LIMIT, rc_env.torrent.up_limit)
        self.assertEqual(rc_env.torrent.DEFAULT_DOWN_LIMIT, rc_env.torrent.skynet_down_limit)
        self.assertEqual(rc_env.torrent.DEFAULT_UP_LIMIT, rc_env.torrent.skynet_up_limit)
        self.assertEqual('1', rc_env.report.cluster_index)
        self.assertEqual('0', rc_env.report.host_index)
        self.assertTrue(rc_env.report.is_white_market)
        self.assertFalse(rc_env.report.is_red_market)
        self.assertFalse(rc_env.report.is_blue_market)
        self.assertEqual('/place/db/bsconfig/webcache/shm/', rc_env.paths.shm_root)
        self.assertEqual(2048, rc_env.ports.aria2)
