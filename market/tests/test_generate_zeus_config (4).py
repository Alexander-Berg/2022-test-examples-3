import six

if six.PY2:
    import ConfigParser as ConfigParser
else:
    import configparser as ConfigParser
import inspect
import os
import shutil
import unittest

import market.report.runtime_cloud.unified_agent.lib as config_generator

import mock
import runtime_cloud.environment as rc_env
import runtime_cloud.install_lib.generate_config as generate_config
import yatest
import yatest.common

BSCONFIG_ITAGS = (
    "VLA_MARKET_PROD_REPORT_SNIPPET_PLANESHIFT a_ctype_production a_dc_vla a_geo_vla a_itype_report "
    "a_line_vla-02 a_metaprj_market a_prj_report-snippet-planeshift a_shard_0 "
    "a_tier_MarketMiniClusterTier0 a_topology_cgset-memory.limit_in_bytes=34464595968 "
    "a_topology_cgset-memory.low_limit_in_bytes=34359738368 "
    "a_topology_group-VLA_MARKET_PROD_REPORT_SNIPPET_PLANESHIFT a_topology_stable-104-r108 "
    "a_topology_version-stable-104-r108 cgset_memory_recharge_on_pgfault_1 itag_replica_8 use_hq_spec "
    "enable_hq_report enable_hq_poll "
)


@mock.patch.dict(
    os.environ,
    {
        'BSCONFIG_IHOST': 'trololo',
        'BSCONFIG_IPORT': '17050',
        'HOME': '/home/container',
        'BSCONFIG_ITAGS': BSCONFIG_ITAGS,
    },
)
class TestGenerateConfig(unittest.TestCase):
    def test_generate_all_configs(self):
        testname = os.path.splitext(os.path.basename(inspect.getfile(self.__class__)))[0]
        output_dir = os.path.join(yatest.common.output_path(), testname)
        root_dir = yatest.common.source_path('market/report/runtime_cloud/unified_agent/zeus_tmpl')
        shutil.copytree(root_dir, os.path.join(output_dir, 'zeus_tmpl'))
        generate_config.generate(output_dir)
        test_secrets = os.path.join(output_dir, "secrets")
        with open(test_secrets, "w") as f:
            f.write("""{"clients": {"market-report": {"self_tvm_id": 1234567}}}""")
        os.makedirs(os.path.join(output_dir, "conf/unified_agent"), exist_ok=True)
        cfg = {
            "unified_agent_grpc_port": rc_env.ports.unified_agent_grpc,
            "unified_agent_status_port": rc_env.ports.unified_agent_status,
            "unified_agent_metrics_read_port": rc_env.ports.unified_agent_metrics_read,
            "unified_agent_metrics_write_port": rc_env.ports.unified_agent_metrics_write,
            "host_metric_logger_port": rc_env.ports.host_metric_logger,
            "nginx_port": rc_env.ports.nginx,
            "report_port": rc_env.ports.report,
            "additional_metric_labels": {
                "role": rc_env.report.role,
                "subrole": rc_env.report.subrole,
                "cluster_index": rc_env.report.cluster_index
            },
            "conf_path": rc_env.paths.conf,
            "logs_path": rc_env.paths.logs,
            "report_log_dir": rc_env.report.log_dir,
            "environment": rc_env.host.environment,
            "data": rc_env.paths.data
        }
        config_generator.generate_config(os.path.join(output_dir, "conf/unified_agent/unified_agent.yml"), cfg, test_secrets)

        for root, _, files in os.walk(os.path.join(output_dir, 'conf')):
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
