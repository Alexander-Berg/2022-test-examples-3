import inspect
import json
import os
import sys
import unittest
import subprocess
import glob
import shutil

import yatest.common
from market.pylibrary.compress_dist import compress_dist

import clients
import services

from .types import Package, File

UC_BIN = yatest.common.binary_path('tools/uc/uc')


def create_dump(packages):
    result = {'resources': {}}
    for package in packages:
        result['resources'][package.archive_name] = {
            '@class': 'ru.yandex.iss.{}Resource'.format('Dynamic' if package.is_dynamic else '')
        }

    return result


def create_zeus_package():
    with open(UC_BIN) as f:
        uc_content = f.read()

    with open(yatest.common.binary_path('market/report/runtime_cloud/zeus/zeus')) as f:
        zeus_content = f.read()

    with open(yatest.common.binary_path('market/report/runtime_cloud/zeus/control_bin/control_bin')) as f:
        control_bin_content = f.read()

    with open(yatest.common.binary_path('market/report/runtime_cloud/zeus/zeus_notify/zeus_notify')) as f:
        zeus_notify_content = f.read()

    with open(yatest.common.binary_path('market/report/runtime_cloud/zeus/zeus_prepare/zeus_prepare')) as f:
        zeus_prepare_content = f.read()

    with open(yatest.common.binary_path('market/tools/package_installer/bin/package_installer')) as f:
        package_installer_content = f.read()

    return Package(name='zeus', is_dynamic=True, files=[
        File(name='zeus', path='./bin', executable=True, content=zeus_content),
        File(name='uc', path='./bin', executable=True, content=uc_content),
        File(name='zeus.tar.gz.control', path='./init.d', executable=True, content=control_bin_content),
        File(name='zeus_notify', path='./bin', executable=True, content=zeus_notify_content),
        File(name='zeus_prepare', path='./bin', executable=True, content=zeus_prepare_content),
        File(name='zeus_package_installer', path='./bin', executable=True, content=package_installer_content),
    ])


PROD_MARKET_GENERAL_ITAGS = \
    "a_geo_vla a_itype_report VLA_MARKET_PROD_REPORT_GENERAL_MARKET cgset_memory_recharge_on_pgfault_1 " \
    "a_tier_MarketMiniClusterTier0 a_line_vla-01 a_topology_cgset-memory.low_limit_in_bytes=120259084288 " \
    "itag_replica_1 a_shard_2 a_topology_group-VLA_MARKET_PROD_REPORT_GENERAL_MARKET " \
    "a_topology_version-stable-102-r75 a_metaprj_market a_dc_vla a_prj_report-general-market a_ctype_production " \
    "a_topology_stable-102-r75 a_topology_cgset-memory.limit_in_bytes=120363941888 use_hq_spec enable_hq_report " \
    "enable_hq_poll "

class TestCase(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        testname = os.path.splitext(os.path.basename(inspect.getfile(cls)))[0]
        cls.env = {
            'BSCONFIG_IPORT': str(2000 + os.getuid() % 30500),
            'BSCONFIG_ITAGS': PROD_MARKET_GENERAL_ITAGS,
            'HOME': os.path.join(yatest.common.output_path(), testname),
            'DISABLE_MEMORY_LOCKING_FOR_TESTING': '1'
        }

        cls.need_install_on_start = False
        cls.need_execute_prepare_script = True
        cls.packages = []

        cls.prepare()

        cls.packages += [
            create_zeus_package()
        ]
        for package in cls.packages:
            package.create(os.path.join(cls.env['HOME']))

        dump = create_dump(cls.packages)
        with open(os.path.join(cls.env['HOME'], 'dump.json'), 'w') as dump_file:
            json.dump(dump, dump_file)

        for archive_file in glob.glob(os.path.join(cls.env['HOME'], '*.tar.gz')):
            try:
                compress_dist.decompress(archive_file, cls.env['HOME'], threads=8, bin_path=UC_BIN)
            except compress_dist.CompressError:
                if 'damaged' in os.path.basename(archive_file):
                    pass

        if cls.need_execute_prepare_script:
            cls.zeus_prepare()

        cls.__zeus_daemon = services.ZeusDaemon(cls.env, cls.need_install_on_start)
        cls.__zeus_daemon.start_with_monitoring()

    @classmethod
    def zeus_prepare(cls):
        return subprocess.check_output([os.path.join(cls.env['HOME'], 'bin/zeus_prepare')], env=cls.env)

    def zeus_notify(self):
        return subprocess.check_output([os.path.join(self.env['HOME'], 'bin/zeus_notify')], env=self.env)

    @classmethod
    def tearDownClass(cls):
        cls.__zeus_daemon.stop()

    @classmethod
    def prepare(cls):
        """Hook method for setting up class fixture before running tests in the class."""

    def setUp(self):
        self.zeus = clients.ZeusClient(self.__zeus_daemon.port)
        self.zeus.check_alive()

    def tearDown(self):
        self.zeus.check_alive()


def main():
    unittest.TestProgram(argv=sys.argv[:1])
