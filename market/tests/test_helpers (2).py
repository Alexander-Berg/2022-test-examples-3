import json
from pathlib import Path
import pytest
import tempfile

from market.idx.pylibrary.report_control.helpers import (
    parse_report_cluster_id,
    parse_report_group_name,
    build_report_consumer_names,
    PublisherConfigHelper,
    ReportStateHelper
)


@pytest.yield_fixture
def publisher_config():
    obj = {
        "dcgroups": {
            "indexer_shadow@atlantis": {
                "async_publishing_mode": "enabled",
                "close_firewall_sleep": 15,
                "close_report_with_old_docs": 1800.0,
                "failures_threshold": 1,
                "generations_prefix": "generations",
                "hosts": {
                    "atlantis.00.rtc.vla1-6046.search.yandex.net@17050": {
                        "cluster": 1,
                        "datacenter": "vla",
                        "dists": {
                            "book-part-0": {},
                            "model-part-0": {},
                            "search-cards": {},
                            "search-part-additions-0": {},
                            "search-part-additions-8": {},
                            "search-part-base-0": {},
                            "search-part-base-8": {},
                            "search-part-blue-0": {},
                            "search-report-data": {},
                            "search-stats": {},
                            "search-wizard": {}
                        },
                        "key": "rtc-ct:vla1-6046.search.yandex.net:17050",
                        "name": "vla1-6046-efc-vla-market-prod--984-17050.gencfg-c.yandex.net",
                        "port": 17053,
                        "redundancy": 1,
                        "rtc_host": "vla1-6046.search.yandex.net",
                        "rtc_port": 17050,
                        "rtc_service": "prod_report_shadow_vla",
                        "service": "marketsearch3"
                    }
                }
            },
            "prod_report_parallel@parallel": {
                "async_publishing_mode": "enabled",
                "close_firewall_sleep": 15,
                "close_report_with_old_docs": 1800.0,
                "failures_threshold": 1,
                "generations_prefix": "generations",
                "hosts": {
                    "parallel.00.rtc.man0-0281.search.yandex.net@17050": {
                        "cluster": 0,
                        "datacenter": "man",
                        "dists": {
                            "book-part-0": {},
                            "model-part-0": {},
                            "search-cards": {},
                            "search-part-additions-0": {},
                            "search-part-additions-8": {},
                            "search-part-base-0": {},
                            "search-part-base-8": {},
                            "search-part-blue-0": {},
                            "search-report-data": {},
                            "search-stats": {},
                            "search-wizard": {}
                        },
                        "key": "rtc-ct:man0-0281.search.yandex.net:17050",
                        "name": "man0-0281-man-market-prod-repo-54a-17050.gencfg-c.yandex.net",
                        "port": 17053,
                        "redundancy": 1,
                        "rtc_host": "man0-0281.search.yandex.net",
                        "rtc_port": 17050,
                        "rtc_service": "prod_report_parallel_man",
                        "service": "marketsearch3"
                    },
                    "parallel.00.rtc.vla1-0100.search.yandex.net@17050": {
                        "cluster": 1,
                        "datacenter": "vla",
                        "dists": {
                            "book-part-0": {},
                            "model-part-0": {},
                            "search-cards": {},
                            "search-part-additions-0": {},
                            "search-part-additions-8": {},
                            "search-part-base-0": {},
                            "search-part-base-8": {},
                            "search-part-blue-0": {},
                            "search-report-data": {},
                            "search-stats": {},
                            "search-wizard": {}
                        },
                        "key": "rtc-ct:vla1-0100.search.yandex.net:17050",
                        "name": "vla1-0100-vla-market-prod-repo-54a-17050.gencfg-c.yandex.net",
                        "port": 17053,
                        "redundancy": 1,
                        "rtc_host": "vla1-0100.search.yandex.net",
                        "rtc_port": 17050,
                        "rtc_service": "prod_report_parallel_vla",
                        "service": "marketsearch3"
                    }
                },
                "min_alive": {
                    "man": 0,
                    "total": 1,
                    "vla": 3
                },
            },
            "market_buker@iva": {
                "async_publishing_mode": "disabled",
                "close_firewall_sleep": 8,
                "close_report_with_old_docs": 0.0,
                "failures_threshold": 2,
                "hosts": {
                    "rtc.iva1-0698.search.yandex.net@27423": {
                        "cluster": 0,
                        "datacenter": "iva",
                        "dists": {
                            "marketkgb": {}
                        },
                        "key": "rtc-ct:iva1-0698.search.yandex.net:27423",
                        "name": "iva1-0698-41b-iva-market-prod--4df-27423.gencfg-c.yandex.net",
                        "port": 27426,
                        "redundancy": 1,
                        "rtc_host": "iva1-0698.search.yandex.net",
                        "rtc_port": 27423,
                        "rtc_service": "production_market_buker_iva",
                        "service": "marketkgb"
                    }
                }
            }
        }
    }
    with tempfile.NamedTemporaryFile(mode='w+t') as f:
        json.dump(obj, f)
        f.flush()
        yield f.name


@pytest.fixture
def report_state():
    obj = {
        "iva1-5776-305-iva-market-test--728-17050.gencfg-c.yandex.net": {
            "downloaded_generations": {
                "marketsearchsnippet": [
                    "20200917_0816",
                    "20200917_1007"
                ]
            },
            "gather_ctime": "2020-09-17 13:33:01",
            "services": [
                "marketsearchsnippet"
            ],
            "dc": "iva",
            "is_reloading": False,
            "packages": {
                "yandex-market-report-dssm-0.7328303": {
                    "status": "installed",
                    "version": "0.7328303",
                    "name": "yandex-market-report-dssm"
                },
                "yandex-market-report-2020.3.187.0": {
                    "status": "downloaded",
                    "version": "2020.3.187.0",
                    "name": "yandex-market-report"
                },
                "yandex-market-report-2020.3.188.0": {
                    "status": "installed",
                    "version": "2020.3.188.0",
                    "name": "yandex-market-report"
                },
                "yandex-market-report-dssm-0.7279374": {
                    "status": "downloaded",
                    "version": "0.7279374",
                    "name": "yandex-market-report-dssm"
                },
                "yandex-market-report-formulas-0.7318636": {
                    "status": "downloaded",
                    "version": "0.7318636",
                    "name": "yandex-market-report-formulas"
                },
                "yandex-market-report-formulas-0.7318936": {
                    "status": "installed",
                    "version": "0.7318936",
                    "name": "yandex-market-report-formulas"
                }
            },
            "port": "17050",
            "environment_type": "testing",
            "httpsearch_status": "UP",
            "fqdn": "iva1-5776-305-iva-market-test--728-17050.gencfg-c.yandex.net",
            "active_generations": {
                "marketsearchsnippet": "20200917_1007"
            },
            "report_cluster_id": "test@market@iva@01",
            "master_action_state": None
        },
        "vla1-4547-vla-market-test-report--341-17050.gencfg-c.yandex.net": {
            "downloaded_generations": {
                "marketsearch3": [
                    "20200917_1007"
                ]
            },
            "gather_ctime": "2020-09-17 13:33:00",
            "report_cpu_usage": 24.99232985,
            "services": [
                "marketsearch3"
            ],
            "dc": "vla",
            "is_reloading": False,
            "packages": {
                "yandex-market-report-dssm-0.7328303": {
                    "status": "installed",
                    "version": "0.7328303",
                    "name": "yandex-market-report-dssm"
                },
                "yandex-market-report-6671f50b2a47ecef311950e38c580ec4667ea9e1": {
                    "status": "downloaded",
                    "version": "6671f50b2a47ecef311950e38c580ec4667ea9e1",
                    "name": "yandex-market-report"
                },
                "yandex-market-report-0.0.1": {
                    "status": "downloaded",
                    "version": "0.0.1",
                    "name": "yandex-market-report"
                },
                "yandex-market-report-2020.3.2.0": {
                    "status": "downloaded",
                    "version": "2020.3.2.0",
                    "name": "yandex-market-report"
                },
                "yandex-market-report-2020.3.188.0": {
                    "status": "installed",
                    "version": "2020.3.188.0",
                    "name": "yandex-market-report"
                },
                "yandex-market-report-formulas-0.7311581": {
                    "status": "downloaded",
                    "version": "0.7311581",
                    "name": "yandex-market-report-formulas"
                },
                "yandex-market-report-dssm-0.7279374": {
                    "status": "downloaded",
                    "version": "0.7279374",
                    "name": "yandex-market-report-dssm"
                },
                "yandex-market-report-formulas-0.7318936": {
                    "status": "installed",
                    "version": "0.7318936",
                    "name": "yandex-market-report-formulas"
                }
            },
            "port": "17050",
            "environment_type": "testing",
            "report_cpu_limit": 34.44,
            "httpsearch_status": "UP",
            "fqdn": "vla1-4547-vla-market-test-report--341-17050.gencfg-c.yandex.net",
            "report_status": "OPENED_CONSISTENT",
            "active_generations": {
                "marketsearch3": "20200917_1007"
            },
            "report_cluster_id": "test@market@vla@00",
            "dynamic_data_timestamp": 1600338420,
            "master_action_state": None,
            "last_rty_document_freshness": 1600338764.0
        },
        "man0-0935-2da-man-market-prep--fd9-17058.gencfg-c.yandex.net": {
            "downloaded_generations": {
                "marketsearch3": [
                    "20220124_1123"
                ]
            },
            "gather_ctime": "2022-01-24 13:39:03",
            "report_cpu_usage": 0.08782824344,
            "services": [
                "marketsearch3"
            ],
            "dc": "man",
            "is_reloading": False,
            "packages": {
                "yandex-market-report-meta-2022.1.46.0": {
                    "status": "installed",
                    "version": "2022.1.46.0",
                    "name": "yandex-market-report-meta"
                },
                "yandex-market-report-formulas-0.9061789": {
                    "status": "downloaded",
                    "version": "0.9061789",
                    "name": "yandex-market-report-formulas"
                },
                "yandex-market-report-formulas-0.9062556": {
                    "status": "installed",
                    "version": "0.9062556",
                    "name": "yandex-market-report-formulas"
                },
                "yandex-market-report-dssm-0.8645999": {
                    "status": "installed",
                    "version": "0.8645999",
                    "name": "yandex-market-report-dssm"
                }
            },
            "port": "17058",
            "environment_type": "prestable",
            "report_cpu_limit": 21.23433884,
            "httpsearch_status": "UP",
            "fqdn": "man0-0935-2da-man-market-prep--fd9-17058.gencfg-c.yandex.net",
            "report_status": "OPENED_CONSISTENT",
            "active_generations": {
                "marketsearch3": "20220124_1123"
            },
            "report_cluster_id": "prep@meta-int@man@00",
            "dynamic_data_timestamp": 1643020143,
            "master_action_state": None,
            "last_rty_document_freshness": None
        }
    }
    with tempfile.NamedTemporaryFile(mode='w+t') as f:
        json.dump(obj, f)
        f.flush()
        yield f.name


@pytest.fixture
def report_bin_archive():
    archive = {
        '2020.3.182.0': ['yandex-market-report.2020.3.182.0.tar.gz'],
        '2020.3.184.0': ['yandex-market-report.2020.3.184.0.tar.gz', 'junk'],
        'rubbish': ['yandex-market-report.2020.3.185.0.tar.gz', 'junk']
    }
    with tempfile.TemporaryDirectory() as d:
        for dirname, files in archive.items():
            (Path(d) / dirname).mkdir()
            for filename in files:
                (Path(d) / dirname / filename).touch()
        yield d


@pytest.mark.parametrize('cluster_id, role, subrole', [
    ('prep@int@sas@02', 'market-report', 'int'),
    ('exp@meta-api@sas@05', 'meta', 'api'),
    ('prod@meta-market-kraken@vla@00', 'meta', 'market-kraken'),
    ('prod@goods-warehouse@man@00', 'market-report', 'goods-warehouse'),
    ('prod@main@man', None, None),
    ('prod@main@man@01@', None, None),
])
def test_parse_report_cluster_id(cluster_id, role, subrole):
    r, s = parse_report_cluster_id(cluster_id)
    assert r == role
    assert s == subrole


@pytest.mark.parametrize('report_group_name', [
    'indexer_shadow@atlantis',
    'prod_report_fresh_base@atlantis',
    'prod_report_goods_parallel@atlantis',
    'prod_report_goods_warehouse@atlantis',
    'prod_report_planeshift@atlantis',
    'report_api@atlantis',
    'report_api_exp@atlantis',
    'report_int@atlantis',
    'report_market@atlantis',
    'report_market_exp@atlantis',
    'report_market_kraken@atlantis',
    'report_meta_api@atlantis',
    'report_meta_api_exp@atlantis',
    'report_meta_int@atlantis',
    'report_meta_market@atlantis',
    'report_meta_market_exp@atlantis',
    'report_meta_market_kraken@atlantis',
    'report_meta_parallel@atlantis',
    'report_meta_parallel_exp@atlantis',
    'report_parallel@atlantis',
    'report_parallel_exp@atlantis',
    'report_shadow@atlantis',
    'test_report_fresh_base@atlantis',
    'test_report_goods_warehouse@atlantis',
    'test_report_market@atlantis',
    'test_report_meta_market@atlantis',
    'test_report_planeshift@sas',
])
def test_parsing_report_group_name(report_group_name):
    return parse_report_group_name(report_group_name)


def test_publisher_config_helper(publisher_config):
    helper = PublisherConfigHelper(publisher_config)

    assert list(sorted(helper.all_dc)) == ['man', 'vla']
    assert list(sorted(helper.all_group_names)) == ['indexer_shadow@atlantis', 'market_buker@iva', 'prod_report_parallel@parallel']
    assert list(sorted(helper.all_report_group_names)) == ['prod_report_parallel@parallel']
    assert list(sorted(helper.all_host_names)) == [
        'iva1-0698-41b-iva-market-prod--4df-27423.gencfg-c.yandex.net',
        'man0-0281-man-market-prod-repo-54a-17050.gencfg-c.yandex.net',
        'vla1-0100-vla-market-prod-repo-54a-17050.gencfg-c.yandex.net',
        'vla1-6046-efc-vla-market-prod--984-17050.gencfg-c.yandex.net',
    ]

    assert helper.get_group_name('iva1-0698-41b-iva-market-prod--4df-27423.gencfg-c.yandex.net') == 'market_buker@iva'
    assert helper.get_hosts('indexer_shadow@atlantis') == {'vla1-6046-efc-vla-market-prod--984-17050.gencfg-c.yandex.net'}
    assert helper.get_hosts('prod_report_parallel@parallel') == {
        'man0-0281-man-market-prod-repo-54a-17050.gencfg-c.yandex.net',
        'vla1-0100-vla-market-prod-repo-54a-17050.gencfg-c.yandex.net',
    }

    assert helper.get_min_alive('prod_report_parallel@parallel').get('man') == 0
    assert helper.get_min_alive('prod_report_parallel@parallel').get('vla') == 3
    assert helper.get_min_alive('prod_report_parallel@parallel').get('sas') is None
    assert helper.get_total_min_alive('prod_report_parallel@parallel') == 1
    assert helper.get_total_min_alive('market_buker@iva') is None

    assert helper.get_cluster('parallel.00.rtc.man0-0281.search.yandex.net@17050') is None
    assert helper.get_cluster('man0-0281-man-market-prod-repo-54a-17050.gencfg-c.yandex.net') == 'man@00'
    assert helper.get_cluster('vla1-6046-efc-vla-market-prod--984-17050.gencfg-c.yandex.net') == 'vla@01'

    assert helper.get_clusters_by_hosts([
        'man0-0281-man-market-prod-repo-54a-17050.gencfg-c.yandex.net',
        'vla1-0100-vla-market-prod-repo-54a-17050.gencfg-c.yandex.net',
        ]) == {'man@00', 'vla@01'}
    assert helper.get_clusters_by_hosts_in_dc([
        'man0-0281-man-market-prod-repo-54a-17050.gencfg-c.yandex.net',
        'vla1-0100-vla-market-prod-repo-54a-17050.gencfg-c.yandex.net',
        ], 'vla') == {'vla@01'}
    assert helper.get_clusters_by_hosts_in_dc(
        ['vla1-6046-efc-vla-market-prod--984-17050.gencfg-c.yandex.net'], 'sas') == set()

    assert helper.get_total_clusters('prod_report_parallel@parallel') == 2
    assert helper.get_total_clusters('market_buker@iva') == 1
    assert helper.get_total_clusters_in_dc('prod_report_parallel@parallel', 'man') == 1
    assert helper.get_total_clusters_in_dc('prod_report_parallel@parallel', 'iva') == 0


def test_report_state_helper(report_state):
    helper = ReportStateHelper(report_state)

    assert helper.active_generations == ['20220124_1123', '20200917_1007', '20200917_1007']
    assert helper.active_report_versions == ['2020.3.188.0', '2020.3.187.0', '2020.3.2.0']
    assert helper.active_meta_report_versions == ['2022.1.46.0']

    assert helper.downloaded_generations == {'20200917_1007', '20220124_1123'}
    assert helper.get_downloaded_generations(dcs={'zzz'}) == set()
    assert helper.get_downloaded_generations(dcs={'man'}) == {'20220124_1123'}
    assert helper.get_downloaded_generations(report_groups={'uknown_group'}) == set()
    assert helper.get_downloaded_generations(report_groups={'test_report_market@atlantis'}) == {'20200917_1007'}


@pytest.mark.parametrize('report_group_name, consumer_names', [
    ('prod_report_planeshift@atlantis', {'market_report_planeshift', 'market_report_base_planeshift'}),
    ('report_api@atlantis', {'market_report_api', 'market_report_base_api'}),
    ('report_api_exp@atlantis', {'market_report_api', 'market_report_base_api'}),
    ('report_int@atlantis', {'market_report_int', 'market_report_base_int'}),
    ('report_market@atlantis', {'market_report_market', 'market_report_base_market'}),
    ('report_market_exp@atlantis', {'market_report_market', 'market_report_base_market'}),
    ('report_meta_api@atlantis', {'market_report_api', 'market_report_meta_api'}),
    ('report_meta_api_exp@atlantis', {'market_report_api', 'market_report_meta_api'}),
    ('report_meta_int@atlantis', {'market_report_int', 'market_report_meta_int'}),
    ('report_meta_market@atlantis', {'market_report_market', 'market_report_meta_market'}),
    ('report_meta_market_exp@atlantis', {'market_report_market', 'market_report_meta_market'}),
    ('test_report_market@atlantis', {'market_report_market', 'market_report_base_market'}),
    ('test_report_meta_market@atlantis', {'market_report_market', 'market_report_meta_market'}),
])
def test_build_report_consumer_names(report_group_name, consumer_names):
    assert build_report_consumer_names(report_group_name) == consumer_names
