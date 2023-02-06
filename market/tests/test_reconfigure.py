# -*- coding: utf-8 -*-
from cStringIO import StringIO
import collections
import json

import py
import pytest
import mock
from yatest.common import source_path

from market.idx.pylibrary.mindexer_core.publishers.publisher import reconfigure_zk
from market.idx.pylibrary.mindexer_core.publishers.async_publisher import write_publisher_config_to_zk
from market.idx.marketindexer.marketindexer import miconfig
from market.idx.marketindexer.miconfig import TwoPhaseReloadMetaSection
from async_publishing.group_config import TwoPhaseReloadMetaConfig


REDUCTOR_CONFIG = StringIO('''{
    "reload_timeout": 600,
    "dcgroups": {
        "prod_report_int_vla@vla": {
            "async_publishing_mode": "disabled",
            "simultaneous_restart": 3,
            "failures_threshold": 4,
            "hosts": {
                "host1": {
                    "key": "key:host1.net",
                    "name": "host1.net",
                    "cluster": 0,
                    "dists": {
                        "search-stats": {},
                        "search-part-1": {}
                    },
                    "service": "marketsearch3"
                },
                "host2": {
                    "key": "key:host2.net",
                    "name": "host2.net",
                    "cluster": 0,
                    "dists": {
                        "search-stats": {},
                        "search-part-2": {}
                    },
                    "service": "marketsearch3",
                    "port": 17051
                }
            }
        },
        "prod_report_api_iva@iva": {
            "async_publishing_mode": "enabled",
            "generations_prefix": "api_generations",
            "min_alive": {
                "sas": 0,
                "iva": 1
            },
            "hosts": {
                "host3": {
                    "key": "key:host3.net",
                    "name": "host3.net",
                    "cluster": 0,
                    "dists": {
                        "search-stats": {},
                        "search-part-3": {}
                    },
                    "service": "marketsearch3",
                    "port": 17052,
                    "datacenter": "iva"
                },
                "snippet.host3": {
                    "key": "key:host3.net",
                    "name": "host3.net",
                    "cluster": 0,
                    "dists": {
                        "search-snippet-part-3": {}
                    },
                    "service": "marketsearchsnippet",
                    "port": 17052,
                    "datacenter": "iva"
                },
                "host4": {
                    "key": "key:host4.net",
                    "name": "host4.net",
                    "cluster": 1,
                    "service": "something_strange",
                    "datacenter": "sas"
                }
            }
        },
        "prod_report_main_sas@sas": {
            "async_publishing_mode": "enabled",
            "generations_prefix": "api_generations",
            "min_alive": {
                "sas": 2,
                "iva": 0
            },
            "hosts": {
                "host5": {
                    "key": "key:host5.net",
                    "name": "host5.net",
                    "cluster": 0,
                    "dists": {
                        "search-stats": {},
                        "search-part-5": {}
                    },
                    "service": "marketsearch3",
                    "port": 17052,
                    "datacenter": "sas"
                },
                "host6": {
                    "key": "key:host6.net",
                    "name": "host6.net",
                    "cluster": 1,
                    "service": "marketsearch3",
                    "port": 17052,
                    "datacenter": "sas"
                }
            }
        },
        "market_report_meta@atlantis": {
            "async_publishing_mode": "enabled",
            "min_alive": {
                "man": 2,
                "sas": 2,
                "vla": 2
            },
            "hosts": {}
        },
        "market_report_exp1@atlantis": {
            "async_publishing_mode": "enabled",
            "min_alive": {
                "man": 2,
                "sas": 2,
                "vla": 2
            },
            "hosts": {}
        }
    }
}''')


@pytest.yield_fixture()
def miconfig_mock(tmpdir):
    icpath = source_path('market/idx/miconfigs/etc/feature/common.ini')
    dspath = source_path('market/idx/marketindexer/tests/datasources.conf')
    full_config = miconfig.MiConfig(icpath, dspath, prefix_dir=str(tmpdir))
    py.path.local(full_config.log_dir).ensure(dir=True)
    py.path.local(full_config.reductor_config_path).write('{"dcgroups": {}}', ensure=True)
    with mock.patch('market.idx.marketindexer.miconfig.force_full_mode', lambda: full_config),\
            mock.patch('market.idx.pylibrary.mindexer_core.publishers.publisher.miconfig.force_full_mode', lambda: full_config):
        yield full_config


def test_reconfigure_zk(miconfig_mock):
    reconfigure_zk(miconfig_mock, zk_master_cls=mock.MagicMock)


@pytest.yield_fixture(scope='module')
def read_state_file_fixture():
    HostState = collections.namedtuple('HostState', ['dc', 'is_meta', 'is_alive', 'report_status'])
    states = {
        'host1': HostState('iva', True, True, ''),
        'host2': HostState('sas', True, True, ''),
        'host3': HostState('sas', False, True, ''),
        'host4': HostState('vla', True, False, ''),
    }
    with mock.patch('market.idx.pylibrary.mindexer_core.publishers.async_publisher.read_state_file') as m:
        m.return_value = states
        yield


@pytest.yield_fixture(scope='module')
def write_publisher_config_to_zk_fixture(read_state_file_fixture):
    with mock.patch('time.time', mock.MagicMock(return_value=1514757600.0)):
        two_phase_reload_meta = {
            'market_report_meta@atlantis': TwoPhaseReloadMetaSection(
                'market_report_meta@atlantis',
                enabled=True,
                base_group='market_report_exp1@atlantis',
                dc_allowed_for_reload=['man', 'sas', 'vla'],
                first_phase_cluster_num=2,
            ),
        }

        class two_phase_miconfig():
            def __init__(self):
                self.async_copybases = True
                self.search_state_path = ''
                self.two_phase_reload_group = ['prod_report_main_sas@sas']
                self.first_phase_cluster_num = 1
                self.first_phase_num_candidates = 0
                self.two_phase_reload_meta = two_phase_reload_meta

        zk = mock.Mock()
        zk.get.return_value = None, None
        write_publisher_config_to_zk(
            zk,
            REDUCTOR_CONFIG,
            root_prefix='/publisher',
            config=two_phase_miconfig(),
            two_phase_reload_meta_groups=two_phase_reload_meta,
            client='test',
        )
        yield zk


class Json(object):
    def __init__(self, json_dict):
        self._dict = json_dict

    def __eq__(self, other):
        return json.loads(other) == self._dict

    def __repr__(self):
        return json.dumps(self._dict)


def _meta_and_base_config():
    return Json({
        'simultaneous_restart': 1,
        'failures_threshold': 1,
        'hosts': {},
        'min_alive': {
            'sas': 2,
            'man': 2,
            'vla': 2,
        },
        'reload_timeout': 600,
        'async_publishing': 'enabled',
        'full_generation': None,
        'packages': None,
        'two_phase_reload': 'disabled',
        'first_phase_nclusters': 0,
        'close_report_with_old_docs': None,
        'first_phase_num_candidates': 0,
        'disable_cpu_usage_limit': False,
        'two_phase_reload_meta': TwoPhaseReloadMetaConfig(
            enabled=True,
            first_phase_dc='sas',
            first_phase_cluster_num=2,
        ).as_dict(),
        'timestamp': 1514757600,
        'client': 'test',
    })


@pytest.mark.parametrize('path,data', [
    # hosts
    ('/publisher/hosts/key:host1.net', Json({
        'group': 'prod_report_int_vla@vla',
        'cluster': 0,
        'fqdn': 'host1.net',
        'dists': {
            'marketsearch3': ['search-part-1', 'search-stats'],
        },
        'generations_prefix': '/publisher/generations',
        'timestamp': 1514757600,
    })),
    ('/publisher/hosts/key:host2.net', Json({
        'group': 'prod_report_int_vla@vla',
        'cluster': 0,
        'fqdn': 'host2.net',
        'dists': {
            'marketsearch3': ['search-part-2', 'search-stats'],
        },
        'generations_prefix': '/publisher/generations',
        'timestamp': 1514757600,
    })),
    ('/publisher/hosts/key:host3.net', Json({
        'group': 'prod_report_api_iva@iva',
        'cluster': 0,
        'fqdn': 'host3.net',
        'dists': {
            'marketsearch3': ['search-part-3', 'search-stats'],
            'marketsearchsnippet': ['search-snippet-part-3']
        },
        'generations_prefix': '/publisher/api_generations',
        'timestamp': 1514757600,
    })),
    ('/publisher/hosts/key:host4.net', Json({
        'group': 'prod_report_api_iva@iva',
        'cluster': 1,
        'fqdn': 'host4.net',
        'dists': {},
        'generations_prefix': '/publisher/api_generations',
        'timestamp': 1514757600,
    })),
    # group configs
    ('/publisher/prod_report_int_vla@vla/config', Json({
        'simultaneous_restart': 3,
        'failures_threshold': 4,
        'hosts': {
            '0': [
                {
                    'key': 'key:host1.net',
                    'fqdn': 'host1.net',
                    'port': 9002,
                    'datacenter': None
                }, {
                    'key': 'key:host2.net',
                    'fqdn': 'host2.net',
                    'port': 17051,
                    'datacenter': None
                }
            ]
        },
        'min_alive': {},
        'reload_timeout': 600,
        'async_publishing': 'disabled',
        'full_generation': None,
        'packages': None,
        'two_phase_reload': 'disabled',
        'first_phase_nclusters': 0,
        'close_report_with_old_docs': None,
        'first_phase_num_candidates': 0,
        'disable_cpu_usage_limit': False,
        'two_phase_reload_meta': None,
        'timestamp': 1514757600,
        'client': 'test',
    })),
    ('/publisher/prod_report_api_iva@iva/config', Json({
        'simultaneous_restart': 1,
        'failures_threshold': 1,
        'hosts': {
            '0': [
                {
                    'key': 'key:host3.net',
                    'fqdn': 'host3.net',
                    'port': 17052,
                    'datacenter': 'iva',
                }
            ],
            '1': [
                {
                    'key': 'key:host4.net',
                    'fqdn': 'host4.net',
                    'port': 9002,
                    'datacenter': 'sas'
                }
            ]
        },
        'min_alive': {
            'sas': 0,
            'iva': 1,
        },
        'reload_timeout': 600,
        'async_publishing': 'enabled',
        'full_generation': None,
        'packages': None,
        'two_phase_reload': 'disabled',
        'first_phase_nclusters': 0,
        'close_report_with_old_docs': None,
        'first_phase_num_candidates': 0,
        'disable_cpu_usage_limit': False,
        'two_phase_reload_meta': None,
        'timestamp': 1514757600,
        'client': 'test',
    })),
    ('/publisher/prod_report_main_sas@sas/config', Json({
        'simultaneous_restart': 1,
        'failures_threshold': 1,
        'hosts': {
            '0': [
                {
                    'key': 'key:host5.net',
                    'fqdn': 'host5.net',
                    'port': 17052,
                    'datacenter': 'sas',
                }
            ],
            '1': [
                {
                    'key': 'key:host6.net',
                    'fqdn': 'host6.net',
                    'port': 17052,
                    'datacenter': 'sas'
                }
            ]
        },
        'min_alive': {
            'sas': 2,
            'iva': 0,
        },
        'reload_timeout': 600,
        'async_publishing': 'enabled',
        'full_generation': None,
        'packages': None,
        'two_phase_reload': 'enabled',
        'first_phase_nclusters': 1,
        'close_report_with_old_docs': None,
        'first_phase_num_candidates': 0,
        'disable_cpu_usage_limit': False,
        'two_phase_reload_meta': None,
        'timestamp': 1514757600,
        'client': 'test',
    })),
    ('/publisher/market_report_meta@atlantis/config', _meta_and_base_config()),
    ('/publisher/market_report_exp1@atlantis/config', _meta_and_base_config()),
])
def test_write_publisher_config_to_zk_set_calls(write_publisher_config_to_zk_fixture, path, data):
    write_publisher_config_to_zk_fixture.set.assert_any_call(path, data)
