# -*- coding: utf-8 -*-

import six.moves.configparser as ConfigParser
import inspect
import mock
import os
import shutil
import unittest

import runtime_cloud.install_lib.generate_config as generate_config
import yatest
import yatest.common

BSCONFIG_ITAGS = "VLA_MARKET_PROD_REPORT_SNIPPET_PLANESHIFT a_ctype_production a_dc_vla a_geo_vla a_itype_report " \
                 "a_line_vla-02 a_metaprj_market a_prj_report-snippet-planeshift a_shard_0 " \
                 "a_tier_MarketMiniClusterTier0 a_topology_cgset-memory.limit_in_bytes=34464595968 " \
                 "a_topology_cgset-memory.low_limit_in_bytes=34359738368 " \
                 "a_topology_group-VLA_MARKET_PROD_REPORT_SNIPPET_PLANESHIFT a_topology_stable-104-r108 " \
                 "a_topology_version-stable-104-r108 cgset_memory_recharge_on_pgfault_1 itag_replica_8 use_hq_spec " \
                 "enable_hq_report enable_hq_poll "


@mock.patch.dict(os.environ, {'BSCONFIG_IHOST': 'trololo', 'BSCONFIG_IPORT': '17050', 'HOME': '/home/container',
                              'BSCONFIG_ITAGS': BSCONFIG_ITAGS, 'NANNY_SERVICE_ID': 'prod_report_parallel_man'})
class TestGenerateConfig(unittest.TestCase):
    def test_generate_all_configs(self):
        testname = os.path.splitext(os.path.basename(inspect.getfile(self.__class__)))[0]
        output_dir = os.path.join(yatest.common.output_path(), testname)
        root_dir = yatest.common.source_path('market/backctld/zeus/zeus_tmpl')
        shutil.copytree(root_dir, os.path.join(output_dir, 'zeus_tmpl'))
        generate_config.generate(output_dir)

        for root, dirs, files in os.walk(os.path.join(output_dir, 'conf')):
            for config_path in files:
                # ignore hidden temprary files like *.swp (for vim)
                if os.path.basename(config_path).startswith('.'):
                    continue
                config_abs_path = os.path.join(root, config_path)
                self.assertGreater(os.path.getsize(config_abs_path), 0)
                with open(config_abs_path) as f:
                    content = f.read()
                    self.assertFalse('{{' in content)
                    self.assertFalse('}}' in content)

                    config = ConfigParser.ConfigParser()
                    config.readfp(f)
