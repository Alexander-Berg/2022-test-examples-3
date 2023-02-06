# coding: utf-8


import mock
from market.sre.tools.balancer_regenerate.lib.resolver import Resolver


class ResolverStub(Resolver):

    def get_hosts(self, service_def):
        return [{'name': 'testservice.market.yandex.net', 'port': 80}]


def test_get_items_hash():
    host_item = {
        'name': 'vla3-1742.gencfg-c.yandex.net',
        'port': '32366'
    }

    assert ResolverStub._get_item_hash(host_item) == 'vla3-1742.gencfg-c.yandex.net:32366'


def test_get_nonlocal_backs():
    backs = [
        {
            'name': 'N%testing_market_some_service',
            'timeout': '5s'
        },
        {
            'name': 'N%testing_market_some_service@local',
            'timeout': '5s'
        },
        {
            'name': 'N%testing_market_another_service',
            'timeout': '5s'
        },
        {
            'name': 'N%testing_market_another_service@local',
            'timeout': '5s'
        }
    ]

    nonlocal_backs = [
        {
            'name': 'N%testing_market_some_service',
            'timeout': '5s'
        },
        {
            'name': 'N%testing_market_another_service',
            'timeout': '5s'
        }
    ]

    assert ResolverStub._get_nonlocal_backs(backs) == nonlocal_backs


def test_get_local_backs():
    backs = [
        {
            'name': 'N%testing_market_some_service',
            'timeout': '5s'
        },
        {
            'name': 'N%testing_market_some_service@local',
            'timeout': '5s'
        },
        {
            'name': 'N%testing_market_another_service',
            'timeout': '5s'
        },
        {
            'name': 'N%testing_market_another_service@local',
            'timeout': '5s'
        },
    ]

    local_backs = [
        {
            'name': 'N%testing_market_some_service@local',
            'timeout': '5s'
        },
        {
            'name': 'N%testing_market_another_service@local',
            'timeout': '5s'
        }
    ]

    assert ResolverStub._get_local_backs(backs) == local_backs


def test_backends2hosts():
    list_reals = {
        'N%testing_market_service_vla': [
            {'name': 'vla-0001.gencfg.yandex.net', 'port': '8041'},
            {'name': 'vla-0002.gencfg.yandex.net', 'port': '8041'},
            {'name': 'vla-0003.gencfg.yandex.net', 'port': '8041'},
        ],
        'N%testing_market_service_sas': [
            {'name': 'sas-0001.gencfg.yandex.net', 'port': '8041'},
            {'name': 'sas-0002.gencfg.yandex.net', 'port': '8041'},
            {'name': 'sas-0003.gencfg.yandex.net', 'port': '8041'},
        ],
        'N%testing_market_service_vla@local': [
            {'name': 'vla-0001.gencfg.yandex.net', 'port': '8041'},
            {'name': 'vla-0002.gencfg.yandex.net', 'port': '8041'},
            {'name': 'vla-0003.gencfg.yandex.net', 'port': '8041'},
        ],
        'N%testing_market_service_sas@local': [
            {'name': 'sas-0001.gencfg.yandex.net', 'port': '8041'},
            {'name': 'sas-0002.gencfg.yandex.net', 'port': '8041'},
            {'name': 'sas-0003.gencfg.yandex.net', 'port': '8041'},
        ],
        'N%testing_market_other_port_vla': [
            {'name': 'vla-0001.gencfg.yandex.net', 'port': '8041'},
            {'name': 'vla-0002.gencfg.yandex.net', 'port': '8041'},
            {'name': 'vla-0003.gencfg.yandex.net', 'port': '8041'},
        ],
        'N%testing_market_other_port_sas': [
            {'name': 'sas-0001.gencfg.yandex.net', 'port': '8041'},
            {'name': 'sas-0002.gencfg.yandex.net', 'port': '8041'},
            {'name': 'sas-0003.gencfg.yandex.net', 'port': '8041'},
        ],
        '%market_conductor_group': [
            {'name': 'service01v.market.yandex.net'},
            {'name': 'service01h.market.yandex.net'},
            {'name': 'service01w.market.yandex.net'},
        ],
        'N%testing_market_service_dbl_vla': [
            {'name': 'vla-0001.gencfg.yandex.net', 'port': '8041'},
            {'name': 'vla-0002.gencfg.yandex.net', 'port': '8041'},
            {'name': 'vla-0001.gencfg.yandex.net', 'port': '10000'},
            {'name': 'vla-0002.gencfg.yandex.net', 'port': '10000'},
        ],
        'N%testing_market_service_dbl_sas': [
            {'name': 'sas-0001.gencfg.yandex.net', 'port': '8041'},
            {'name': 'sas-0002.gencfg.yandex.net', 'port': '8041'},
            {'name': 'sas-0001.gencfg.yandex.net', 'port': '10000'},
            {'name': 'sas-0002.gencfg.yandex.net', 'port': '10000'},
        ],
    }

    case_nonlocal_nanny = [
        {
            'name': 'N%testing_market_service_vla',
            'timeout': '5s',
            'weight': 100,
            'port': 8041,
            'plain_http_backend': True
        },
        {
            'name': 'N%testing_market_service_sas',
            'timeout': '5s',
            'weight': 100,
            'port': 8041,
            'plain_http_backend': True
        },
    ]

    case_with_local_nanny = [
        {
            'name': 'N%testing_market_service_vla',
            'timeout': '5s',
            'weight': 100,
            'port': 8041,
            'plain_http_backend': True
        },
        {
            'name': 'N%testing_market_service_sas',
            'timeout': '5s',
            'weight': 100,
            'port': 8041,
            'plain_http_backend': True
        },
        {
            'name': 'N%testing_market_service_vla@local',
            'timeout': '5s',
            'weight': 50,
            'port': 8041,
            'plain_http_backend': True
        },
        {
            'name': 'N%testing_market_service_sas@local',
            'timeout': '5s',
            'weight': 50,
            'port': 8041,
            'plain_http_backend': True
        },
    ]

    case_other_port = [
        {
            'name': 'N%testing_market_other_port_vla',
            'timeout': '5s',
            'weight': 100,
            'port': 10000,
            'plain_http_backend': True
        },
        {
            'name': 'N%testing_market_other_port_sas',
            'timeout': '5s',
            'weight': 100,
            'port': 10000,
            'plain_http_backend': True
        },
        {
            'name': '%market_conductor_group',
            'timeout': '5s',
            'weight': 100,
            'port': 10000,
            'plain_http_backend': True
        },
    ]

    case_double_ports = [
        {
            'name': 'N%testing_market_service_dbl_vla',
            'timeout': '5s',
            'weight': 100,
            'plain_http_backend': True
        },
        {
            'name': 'N%testing_market_service_dbl_sas',
            'timeout': '5s',
            'weight': 100,
            'plain_http_backend': True
        }
    ]

    def new_get_hosts_nonlocal_nanny(cls, backend):

        if 'local' not in backend:
            return list_reals[backend]

        if cls.dc in backend:
            return list(filter(lambda x: cls.dc in x['name'], list_reals[backend]))

        return []

    with mock.patch.object(Resolver, 'get_hosts', new=new_get_hosts_nonlocal_nanny):
        resolver = Resolver()
        assert sorted(resolver._backends2hosts(case_nonlocal_nanny), key=lambda d: str(d)) == [
            {
                'name': 'sas-0001.gencfg.yandex.net',
                'plain_http_backend': True,
                'port': '8041',
                'timeout': '5s',
                'weight': 100
            },
            {
                'name': 'sas-0002.gencfg.yandex.net',
                'plain_http_backend': True,
                'port': '8041',
                'timeout': '5s',
                'weight': 100
            },
            {
                'name': 'sas-0003.gencfg.yandex.net',
                'plain_http_backend': True,
                'port': '8041',
                'timeout': '5s',
                'weight': 100
            },
            {
                'name': 'vla-0001.gencfg.yandex.net',
                'plain_http_backend': True,
                'port': '8041',
                'timeout': '5s',
                'weight': 100
            },
            {
                'name': 'vla-0002.gencfg.yandex.net',
                'plain_http_backend': True,
                'port': '8041',
                'timeout': '5s',
                'weight': 100
            },
            {
                'name': 'vla-0003.gencfg.yandex.net',
                'plain_http_backend': True,
                'port': '8041',
                'timeout': '5s',
                'weight': 100
            }
        ]

    with mock.patch.object(Resolver, 'get_hosts', new=new_get_hosts_nonlocal_nanny):
        resolver = Resolver(dc='sas')
        assert sorted(resolver._backends2hosts(case_with_local_nanny), key=lambda d: str(d)) == [
            {
                'name': 'sas-0001.gencfg.yandex.net',
                'plain_http_backend': True,
                'port': '8041',
                'timeout': '5s',
                'weight': 50
            },
            {
                'name': 'sas-0002.gencfg.yandex.net',
                'plain_http_backend': True,
                'port': '8041',
                'timeout': '5s',
                'weight': 50
            },
            {
                'name': 'sas-0003.gencfg.yandex.net',
                'plain_http_backend': True,
                'port': '8041',
                'timeout': '5s',
                'weight': 50
            },
            {
                'name': 'vla-0001.gencfg.yandex.net',
                'plain_http_backend': True,
                'port': '8041',
                'timeout': '5s',
                'weight': 100
            },
            {
                'name': 'vla-0002.gencfg.yandex.net',
                'plain_http_backend': True,
                'port': '8041',
                'timeout': '5s',
                'weight': 100
            },
            {
                'name': 'vla-0003.gencfg.yandex.net',
                'plain_http_backend': True,
                'port': '8041',
                'timeout': '5s',
                'weight': 100
            }
        ]

    with mock.patch.object(Resolver, 'get_hosts', new=new_get_hosts_nonlocal_nanny):
        resolver = Resolver()
        assert sorted(resolver._backends2hosts(case_other_port), key=lambda d: str(d)) == [
            {
                'name': 'sas-0001.gencfg.yandex.net',
                'plain_http_backend': True,
                'port': '8041',
                'timeout': '5s',
                'weight': 100
            },
            {
                'name': 'sas-0002.gencfg.yandex.net',
                'plain_http_backend': True,
                'port': '8041',
                'timeout': '5s',
                'weight': 100
            },
            {
                'name': 'sas-0003.gencfg.yandex.net',
                'plain_http_backend': True,
                'port': '8041',
                'timeout': '5s',
                'weight': 100
            },
            {
                'name': 'service01h.market.yandex.net',
                'plain_http_backend': True,
                'port': 10000,
                'timeout': '5s',
                'weight': 100
            },
            {
                'name': 'service01v.market.yandex.net',
                'plain_http_backend': True,
                'port': 10000,
                'timeout': '5s',
                'weight': 100
            },
            {
                'name': 'service01w.market.yandex.net',
                'plain_http_backend': True,
                'port': 10000,
                'timeout': '5s',
                'weight': 100
            },
            {
                'name': 'vla-0001.gencfg.yandex.net',
                'plain_http_backend': True,
                'port': '8041',
                'timeout': '5s',
                'weight': 100
            },
            {
                'name': 'vla-0002.gencfg.yandex.net',
                'plain_http_backend': True,
                'port': '8041',
                'timeout': '5s',
                'weight': 100
            },
            {
                'name': 'vla-0003.gencfg.yandex.net',
                'plain_http_backend': True,
                'port': '8041',
                'timeout': '5s',
                'weight': 100
            }
        ]

    with mock.patch.object(Resolver, 'get_hosts', new=new_get_hosts_nonlocal_nanny):
        resolver = Resolver(dc='sas')
        assert sorted(resolver._backends2hosts(case_double_ports), key=lambda d: str(d)) == [
            {
                'name': 'sas-0001.gencfg.yandex.net',
                'plain_http_backend': True,
                'port': '10000',
                'timeout': '5s',
                'weight': 100
            },
            {
                'name': 'sas-0001.gencfg.yandex.net',
                'plain_http_backend': True,
                'port': '8041',
                'timeout': '5s',
                'weight': 100
            },
            {
                'name': 'sas-0002.gencfg.yandex.net',
                'plain_http_backend': True,
                'port': '10000',
                'timeout': '5s',
                'weight': 100
            },
            {
                'name': 'sas-0002.gencfg.yandex.net',
                'plain_http_backend': True,
                'port': '8041',
                'timeout': '5s',
                'weight': 100
            },
            {
                'name': 'vla-0001.gencfg.yandex.net',
                'plain_http_backend': True,
                'port': '10000',
                'timeout': '5s',
                'weight': 100
            },
            {
                'name': 'vla-0001.gencfg.yandex.net',
                'plain_http_backend': True,
                'port': '8041',
                'timeout': '5s',
                'weight': 100
            },
            {
                'name': 'vla-0002.gencfg.yandex.net',
                'plain_http_backend': True,
                'port': '10000',
                'timeout': '5s',
                'weight': 100
            },
            {
                'name': 'vla-0002.gencfg.yandex.net',
                'plain_http_backend': True,
                'port': '8041',
                'timeout': '5s',
                'weight': 100
            }
        ]
