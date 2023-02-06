#!/usr/bin/python
# -*- coding: utf-8 -*-
from six import StringIO

import pytest
import mock

from market.pylibrary.mindexerlib.recent_symlink_system import generation2unixtime
from market.idx.pylibrary.mindexer_core.search_state.search_state import (
    check_index_age,
    is_generation_uploaded,
    get_active_uploaded_generations,
    get_cluster_ids,
)

publisher_config = '''{
    "dcgroups": {
        "test_report_market_iva@iva": {
            "hosts": {
                "host1": {
                    "name": "host1.fqdn",
                    "service": "marketsearch3",
                    "cluster": 1
                },
                "host2": {
                    "name": "host2.fqdn",
                    "service": "marketsearch3",
                    "cluster": 2
                },
                "dead_host": {
                    "name": "dead_host.fqdn",
                    "service": "marketsearch3",
                    "cluster": 3
                }
            }
        },
        "test_report_daas@atlantis": {
            "async_publishing_mode": "upload",
            "hosts": {
                "host1": {
                    "name": "daas_host1.fqdn",
                    "service": "marketsearch3",
                    "cluster": 1
                }
            }
        },
        "report_shadow@vla": {
            "async_publishing_mode": "enabled",
            "close_firewall_sleep": 15,
            "failures_threshold": 1,
            "hosts": {
                "rtc.vla1@77777": {
                    "name": "rtc.vla1.fqdn",
                    "service": "marketsearch3",
                    "cluster": 4
                }
            }
        }
    }
}'''

search_state = '''{
    "host1.fqdn": {
        "is_reloading": false,
        "packages": {},
        "force_report_restart": false,
        "downloaded_generations": {
            "marketsearch3": [
                "20180101_1000",
                "20180101_1300",
                "20180101_1325",
                "20180101_2325"
            ]
        },
        "active_generations": {
            "marketsearch3": "%s"
        },
        "report_status": "opened_consistent",
        "services": [
            "marketsearch3"
        ]
    },
    "daas_host1.fqdn": {
        "is_reloading": false,
        "packages": {},
        "force_report_restart": false,
        "downloaded_generations": {
            "marketsearch3": [
                "20180101_1000",
                "20180101_1300"
            ]
        },
        "active_generations": {
            "marketsearch3": "20180101_1300"
        },
        "report_status": "opened_consistent",
        "services": [
            "marketsearch3"
        ]
    },
    "rtc.vla1.fqdn": {
        "is_reloading": false,
        "packages": {},
        "force_report_restart": false,
        "downloaded_generations": {
            "marketsearch3": [
                "20180101_1000",
                "20180101_1300"
            ]
        },
        "active_generations": {
            "marketsearch3": "20180101_1300"
        },
        "report_status": "opened_consistent",
        "services": [
            "marketsearch3"
        ]
    },
    "host2.fqdn": {
        "is_reloading": false,
        "packages": {},
        "force_report_restart": false,
        "downloaded_generations": {
            "marketsearch3": [
                "20180101_1000",
                "20180101_1300",
                "20180101_1325",
                "20180101_2325"
            ]
        },
        "active_generations": {
            "marketsearch3": "%s"
        },
        "report_status": "opened_consistent",
        "services": [
            "marketsearch3"
        ]
    },
    "dead_host.fqdn": null
}'''


def test_empty():
    empty_config = StringIO('{}')
    empty_state = StringIO('{}')
    result = check_index_age(empty_config, empty_state, '1h', '3h', '1h', False)
    assert result == '2;no alive clusters to check generation'


@pytest.mark.parametrize('host1_active_gen, host2_active_gen, now, expected', [
    ('20180101_1325', '20180101_1300', '20180101_1330',
     '0;oldest generation under load(20180101_1300) age is 30 minutes found on cluster test_report_market_iva@iva:2. '
     'newest generation under load(20180101_1325) age is 5 minutes'),
    ('20180101_1325', '20180101_1300', '20180101_1410',
     '1;oldest generation under load(20180101_1300) age is 1 hour and 10 minutes found on cluster test_report_market_iva@iva:2. '
     'newest generation under load(20180101_1325) age is 45 minutes'),
    ('20180101_1325', '20180101_1300', '20180101_1900',
     '2;oldest generation under load(20180101_1300) age is 6 hours found on cluster test_report_market_iva@iva:2. '
     'newest generation under load(20180101_1325) age is 5 hours and 35 minutes'),
    ('20180101_1325', '20180101_1325', '20180101_2325',
     '0;oldest and newest generations are the same (20180101_1325)'),
    ('20180101_1325', '20180101_2325', '20180101_2355',
     '1;oldest generation under load(20180101_1325) age is 10 hours and 30 minutes found on cluster test_report_market_iva@iva:1. '
     'newest generation under load(20180101_2325) age is 30 minutes'),
])
def test_index_age(host1_active_gen, host2_active_gen, now, expected):
    now = generation2unixtime(now)
    with mock.patch('time.time', return_value=now):
        result = check_index_age(
            StringIO(publisher_config),
            StringIO(search_state % (host1_active_gen, host2_active_gen)),
            warning_threshlod_str='1h',
            error_threshlold_str='3h',
            deployment_duration_str='1h',
            stand_alone_blue_server=False
        )
    assert result == expected


publisher_config_for_upload = '''{
    "dcgroups": {
        "test_report_market_iva@iva": {
            "hosts": {
                "host1": {
                    "name": "host1.fqdn",
                    "service": "marketsearch3",
                    "cluster": 1
                },
                "host2": {
                    "name": "host2.fqdn",
                    "service": "marketsearch3",
                    "cluster": 1
                },
                "host3": {
                    "name": "host3.fqdn",
                    "service": "marketsearch3",
                    "cluster": 3
                }
            }
        }
    }
}'''


search_state_downloade_to_all_hosts = '''{
    "host1.fqdn": {
        "is_reloading": false,
        "packages": {},
        "force_report_restart": false,
        "downloaded_generations": {
            "marketsearch3": [
                "20180101_1000",
                "20180101_1300"
            ]
        },
        "active_generations": {
            "marketsearch3": "20180101_1300"
        },
        "report_status": "opened_consistent",
        "services": [
            "marketsearch3"
        ]
    },
    "host2.fqdn": {
        "is_reloading": false,
        "packages": {},
        "force_report_restart": false,
        "downloaded_generations": {
            "marketsearch3": [
                "20180101_1000",
                "20180101_1300"
            ]
        },
        "active_generations": {
            "marketsearch3": "20180101_1300"
        },
        "report_status": "opened_consistent",
        "services": [
            "marketsearch3"
        ]
    },
    "host3.fqdn": {
        "is_reloading": false,
        "packages": {},
        "force_report_restart": false,
        "downloaded_generations": {
            "marketsearch3": [
                "20180101_1000",
                "20180101_1300"
            ]
        },
        "active_generations": {
            "marketsearch3": "20180101_1300"
        },
        "report_status": "opened_consistent",
        "services": [
            "marketsearch3"
        ]
    }
}'''


search_state_downloade_not_to_all_hosts = '''{
    "host1.fqdn": {
        "is_reloading": false,
        "packages": {},
        "force_report_restart": false,
        "downloaded_generations": {
            "marketsearch3": [
                "20180101_1300"
            ]
        },
        "active_generations": {
            "marketsearch3": "20180101_1000"
        },
        "report_status": "opened_consistent",
        "services": [
            "marketsearch3"
        ]
    },
    "host2.fqdn": {
        "is_reloading": false,
        "packages": {},
        "force_report_restart": false,
        "downloaded_generations": {
            "marketsearch3": [
                "20180101_1000",
                "20180101_1300"
            ]
        },
        "active_generations": {
            "marketsearch3": "20180101_1000"
        },
        "report_status": "opened_consistent",
        "services": [
            "marketsearch3"
        ]
    },
    "host3.fqdn": {
        "is_reloading": false,
        "packages": {},
        "force_report_restart": false,
        "downloaded_generations": {
            "marketsearch3": [
                "20180101_1000",
                "20180101_1300"
            ]
        },
        "active_generations": {
            "marketsearch3": "20180101_1000"
        },
        "report_status": "opened_consistent",
        "services": [
            "marketsearch3"
        ]
    }
}'''


@pytest.mark.parametrize('search_state_upload, expected', [
    (search_state_downloade_to_all_hosts, True),
    (search_state_downloade_not_to_all_hosts, False),
])
def test_generation_upload(search_state_upload, expected):
    result = is_generation_uploaded(
        StringIO(publisher_config_for_upload),
        StringIO(search_state_upload),
        generation='20180101_1000',
    )
    assert result == expected


def test_get_active_upload_generations():
    uploaded, active = get_active_uploaded_generations(
        StringIO(publisher_config_for_upload),
        StringIO(search_state_downloade_not_to_all_hosts)
    )

    assert uploaded == set(["20180101_1300", "20180101_1000"])
    assert active == set(["20180101_1000"])


def test_get_cluster_ids():
    ids = get_cluster_ids(
        StringIO(publisher_config_for_upload),
        ["host1.fqdn", "host3.fqdn"]
    )

    assert ids == {'test_report_market_iva@iva': set(['1', '3'])}
