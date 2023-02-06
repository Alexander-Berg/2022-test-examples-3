import json
import os

import yatest.common as yatest

from search.tools.fast_data_deployment.lib import utils


DATA_DIR = 'search/tools/fast_data_deployment/lib/tests/data'


def test_get_version_from_svn_info():
    svn_info_content = """author: insight
                          commit_revision: 4184778
                          date: 2018-11-08T21:23:06.432589Z
                          entry_kind: dir
                          entry_path: arcadi
                          entry_revision: 4184778
                          fast_data_version: 1261
                          repository_root: svn+ssh://zomb-sandbox-rw@arcadia.yandex.ru/arc
                          sandbox_task: 328921895
                          url: arcadia:/arc/trunk/arcadia"""
    svn_info_content_no_version = """author: insight
                                     commit_revision: 4184778
                                     date: 2018-11-08T21:23:06.432589Z
                                     entry_revision: 4184778
                                     sandbox_task: 328921895
                                     url: arcadia:/arc/trunk/arcadia"""

    assert utils.get_version_from_svn_info(svn_info_content) == 1261
    assert utils.get_version_from_svn_info(svn_info_content_no_version) is None
    assert utils.get_version_from_svn_info("fast_data_version: 1337 7331") == 1337
    assert utils.get_version_from_svn_info("fast_data_version: ") is None
    assert utils.get_version_from_svn_info("") is None


def test_get_version():
    fast_data_dir = os.path.join(yatest.source_path(DATA_DIR), 'rearrange.fast')
    assert utils.get_version(fast_data_dir) == 1261

    dir_with_no_svninfo = yatest.source_path(DATA_DIR)
    assert utils.get_version(dir_with_no_svninfo) is None

    nonexistent_dir = os.path.join(yatest.source_path(DATA_DIR), 'nonexistent')
    assert utils.get_version(nonexistent_dir) is None


def test_get_geo_from_tags():
    tags = ["SAS_WEB_NOAPACHE_HAMSTER", "a_ctype_hamster", "a_dc_sas", "a_geo_sas",
            "a_itype_noapache", "a_line_sas-1.3.1", "a_metaprj_web", "a_prj_web-main",
            "a_prj_web-main-rkub", "a_tier_none", "a_topology_stable-114-r40"]
    assert utils.get_geo_from_tags(tags) == 'sas'


def test_get_geo_from_nanny_config():
    config = json.loads(
        """{
            "engine": "",
            "network_settings": "NO_NETWORK_ISOLATION",
            "container_hostname": "sas1-7167-sas-web-noapache-hamster-12191.gencfg-c.yandex.net",
            "hostname": "sas1-7167.search.yandex.net",
            "port": 12191,
            "itags": [
                "SAS_WEB_NOAPACHE_HAMSTER",
                "a_ctype_hamster",
                "a_dc_sas",
                "a_geo_sas",
                "a_itype_noapache",
                "a_line_sas-1.3.1",
                "a_metaprj_web",
                "a_prj_web-main",
                "a_prj_web-main-rkub",
                "a_tier_none",
                "a_topology_cgset-memory.limit_in_bytes=24696061952",
                "a_topology_cgset-memory.low_limit_in_bytes=13958643712",
                "a_topology_group-SAS_WEB_NOAPACHE_HAMSTER",
                "a_topology_stable-114-r40",
                "a_topology_version-stable-114-r40",
                "cgset_memory_recharge_on_pgfault_1",
                "itag_ipv6_only",
                "enable_hq_report",
                "enable_hq_poll"
            ]
        }""")
    assert utils.get_geo_from_nanny_config(config) == 'sas'


def test_get_geo_from_bsconfig_tags():
    tags = ("VLA_NOAPACHEUPPER_PRIEMKA_1 a_ctype_test a_dc_vla a_geo_vla "
            "a_itype_noapache a_line_vla-02 a_metaprj_web a_prj_none "
            "a_tier_none a_topology_cgset-memory.limit_in_bytes=43054530560 "
            "a_topology_cgset-memory.low_limit_in_bytes=42949672960 "
            "a_topology_group-VLA_NOAPACHEUPPER_PRIEMKA_1 a_topology_stable-114-r462 "
            "a_topology_version-stable-114-r462 cgset_memory_recharge_on_pgfault_1 "
            "enable_hq_report enable_hq_poll")
    assert utils.get_geo_from_bsconfig_tags(tags)


def test_extract():
    archive_path = os.path.join(yatest.source_path(DATA_DIR), 'rearrange.fast.tar.gz')
    output_dir = yatest.output_path('rearrange.fast.extracted')
    utils.extract(archive_path, output_dir)

    assert os.path.exists(output_dir)
    return yatest.canonical_dir(output_dir)
