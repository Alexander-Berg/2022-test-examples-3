import collections
from datetime import datetime
import json
import mock
import os
import pytest
import tempfile
import time

from market.idx.admin.auto_subzero.lib import report_watcher, notifications
from market.idx.admin.auto_subzero.lib.signals import Signal
from market.idx.pylibrary.report_control import default_values


class NamedDict(collections.UserDict):
    def __init__(self, dict):
        super().__init__(self)
        self.update(dict)

    def __getattr__(self, name):
        return self[name]


@pytest.yield_fixture
def args(tmpdir):
    config = {
        'dcgroups': {
            'test_report_market@atlantis': {
                'async_publishing_mode': 'enabled',
                'hosts': {
                    'host1': {
                        'cluster': 0,
                        'datacenter': 'sas',
                        'name': 'host1.sas.yandex.net',
                        'key': 'rtc-ct:host1.sas.yandex.net:17050'
                    },
                    'host2': {
                        'cluster': 0,
                        'datacenter': 'vla',
                        'name': 'host2.vla.yandex.net',
                        'key': 'rtc-ct:host2.vla.yandex.net:17050'
                    },
                }
            },
            'test_report_meta_market@atlantis': {
                'async_publishing_mode': 'enabled',
                'hosts': {
                    'host1': {
                        'cluster': 0,
                        'datacenter': 'sas',
                        'name': 'host3.sas.yandex.net',
                        'key': 'rtc-ct:host3.sas.yandex.net:17050'
                    },
                    'host2': {
                        'cluster': 0,
                        'datacenter': 'vla',
                        'name': 'host4.vla.yandex.net',
                        'key': 'rtc-ct:host4.vla.yandex.net:17050'
                    },
                }
            },
        }
    }
    with tempfile.NamedTemporaryFile(mode='w+t', dir=tmpdir) as publisher_conf:
        json.dump(config, publisher_conf)
        publisher_conf.flush()

        yield NamedDict({
            'dry_run': False, 'not_only_master': False, 'force_group': [],
            'use_min_alive': True, 'group_threshold': 0.5,
            'rollback_report': True,
            'publisher_conf': publisher_conf.name,
            'dump_dir': tmpdir,
            'lock_path': os.path.join(tmpdir, 'auto_subzero.lock'),
            'dc': [],
        })


@pytest.yield_fixture
def i_am_master():
    with mock.patch('market.pylibrary.zkclient.am_i_master', return_value=True):
        yield


@pytest.yield_fixture
def its_mock():
    its_mock = mock.Mock()
    with mock.patch('market.idx.admin.auto_subzero.lib.report_watcher.ItsHelper', return_value=its_mock):
        yield its_mock


@pytest.yield_fixture
def flags_its_mock():
    flags_its_mock = mock.Mock()
    flags_its_mock.report_emergency_flags = 'etag', {}
    with mock.patch('market.idx.admin.auto_subzero.lib.flag_watcher.ItsHelper', return_value=flags_its_mock):
        yield flags_its_mock


@pytest.yield_fixture
def fake_access_helper():
    class FakeAccessHelper:
        def __init__(self, **kwargs):
            self.timestamp = int(time.time())
            self.report_groups = list()
            self.locations = list()

        def all_dynamics(self):
            return {'test_dynamic'}

        def rollback_dynamics_to_moment(
            self,
            timestamp,
            report_groups,
            locations,
            dynamics,
            dry_mode=False
        ):
            self.timestamp = timestamp
            self.report_groups = report_groups
            self.locations = locations

    fake_access_helper = FakeAccessHelper()
    with mock.patch('market.idx.admin.auto_subzero.lib.report_watcher.ReportWatcher._ReportWatcher__create_access_helper', return_value=fake_access_helper):
        yield fake_access_helper


@pytest.yield_fixture
def fake_zk():
    class MiniZk:
        def __init__(self, kv=None):
            self._kv = kv or {}

        def exists(self, key):
            return key in self._kv

        def ensure_path(self, key):
            pass

        def set(self, key, value):
            self._kv[key] = value

        def get(self, key):
            return self._kv[key], None

        def create_if_not_exists(self, key, value):
            if not self.exists(key):
                self.set(key, value)

        def create(self, key, value='', **kwargs):
            self.set(key, value)

        def stop(self):
            pass

        def close(self):
            pass

        def delete(self, key):
            del self._kv[key]

    fake_zk = MiniZk()

    with mock.patch('market.pylibrary.zkclient.create_and_start_client', return_value=fake_zk):
        yield fake_zk


@pytest.yield_fixture
def ch_mock():
    ch_mock = mock.Mock()
    ch_mock.all_generations = ['20201010_1010', '20201010_1011']
    ch_mock.all_report_versions = ['2020.4.120', '2020.4.123']
    ch_mock.all_meta_report_versions = ['2020.4.125']

    with mock.patch('market.idx.pylibrary.report_control.helpers.ClickhouseHelper', return_value=ch_mock):
        yield ch_mock


@pytest.yield_fixture
def sb_mock():
    sb_generations = {
        'items': [
            {
                'attributes': {
                    'generation': '20201010_1010'
                }
            },
            {
                'attributes': {
                    'generation': '20201010_0909'
                }
            }
        ]
    }

    sb_reports = {
        'items': [
            {
                'task': {
                    'status': 'RELEASED'
                },
                'attributes': {
                    'resource_version': '2020.4.123'
                }
            },
            {
                'task': {
                    'status': 'RELEASED'
                },
                'attributes': {
                    'resource_version': '2020.4.124'
                }
            }
        ]
    }

    sb_meta_reports = {
        'items': [
            {
                'task': {
                    'status': 'RELEASED'
                },
                'attributes': {
                    'resource_version': '2020.4.125'
                }
            },
            {
                'task': {
                    'status': 'RELEASED'
                },
                'attributes': {
                    'resource_version': '2020.4.126'
                }
            }
        ]
    }

    def sb(**kwargs):
        if kwargs['type'] == default_values.SANDBOX_GENERATION_RESOURCE_NAME:
            return sb_generations
        elif kwargs['type'] == default_values.SANDBOX_REPORT_RESOURCE_NAME:
            return sb_reports
        elif kwargs['type'] == default_values.SANDBOX_META_REPORT_RESOURCE_NAME:
            return sb_meta_reports

    sb_mock = mock.Mock()
    sb_mock.resource.read.side_effect = sb

    with mock.patch('market.idx.pylibrary.report_control.helpers.sandbox_api.Client', return_value=sb_mock):
        yield sb_mock


@pytest.fixture
def now_ts():
    return datetime.now().timestamp()


def convert_json_to_signal(json_signal):
    return [Signal.from_dict(s) for s in json_signal]


@pytest.yield_fixture
def ok_signal(now_ts):
    hosts = [{
        "status": "OK",
        "received_time": now_ts,
        "service": "market-report-heartbeat",
        "tags": [],
        "instance": "",
        "host": "host1.sas.yandex.net",
        "heartbeat": 60.0,
        "digest": ""
    }, {
        "status": "OK",
        "received_time": now_ts,
        "service": "market-report-heartbeat",
        "tags": [],
        "instance": "",
        "host": "host2.vla.yandex.net",
        "heartbeat": 60.0,
        "digest": ""
    }, {
        "status": "OK",
        "received_time": now_ts,
        "service": "market-report-heartbeat",
        "tags": [],
        "instance": "",
        "host": "host3.sas.yandex.net",
        "heartbeat": 60.0,
        "digest": ""
    }, {
        "status": "OK",
        "received_time": now_ts,
        "service": "market-report-heartbeat",
        "tags": [],
        "instance": "",
        "host": "host4.vla.yandex.net",
        "heartbeat": 60.0,
        "digest": ""
    }]
    signals = convert_json_to_signal(hosts)
    with mock.patch('market.idx.admin.auto_subzero.lib.report_watcher.signals.get_juggler_signal', return_value=signals) as juggler_api:
        yield juggler_api


@pytest.yield_fixture
def crit_signal(now_ts):
    hosts = [{
        "status": "CRIT",
        "received_time": now_ts,
        "service": "market-report-heartbeat",
        "tags": [],
        "instance": "",
        "host": "host1.sas.yandex.net",
        "heartbeat": 60.0,
        "digest": ""
    }, {
        "status": "OK",
        "received_time": now_ts,
        "service": "market-report-heartbeat",
        "tags": [],
        "instance": "",
        "host": "host1.vla.yandex.net",
        "heartbeat": 60.0,
        "digest": ""
    }, {
        "status": "OK",
        "received_time": now_ts,
        "service": "market-report-heartbeat",
        "tags": [],
        "instance": "",
        "host": "host3.sas.yandex.net",
        "heartbeat": 60.0,
        "digest": ""
    }, {
        "status": "OK",
        "received_time": now_ts,
        "service": "market-report-heartbeat",
        "tags": [],
        "instance": "",
        "host": "host4.vla.yandex.net",
        "heartbeat": 60.0,
        "digest": ""
    }]
    signals = convert_json_to_signal(hosts)
    with mock.patch('market.idx.admin.auto_subzero.lib.report_watcher.signals.get_juggler_signal', return_value=signals) as juggler_api:
        yield juggler_api


@pytest.yield_fixture
def crashing_signal(now_ts):
    def effect():
        raise RuntimeError('Oops!')

    with mock.patch('market.idx.admin.auto_subzero.lib.report_watcher.signals.get_juggler_signal', side_effect=effect) as juggler_api:
        yield juggler_api


@pytest.yield_fixture
def juggler_push_mock():
    juggler_push_mock = mock.Mock()
    with mock.patch('market.idx.admin.auto_subzero.lib.notifications.JugglerEventSender', return_value=juggler_push_mock):
        yield juggler_push_mock


@pytest.fixture
def cowboy_config():
    config = {
        "simultaneous_restart": 1,
        "failures_threshold": 3,
        "hosts": {
            "1": [
            {
                "key": "rtc-ct:host1.vla.yandex.net:17050",
                "fqdn": "host2-test-report--341-17050.gencfg-c.yandex.net",
                "port": 17053,
                "datacenter": "vla"
            }
            ],
            "0": [
            {
                "key": "rtc-ct:host1.sas.yandex.net:17050",
                "fqdn": "host1-test-report--d79-17050.gencfg-c.yandex.net",
                "port": 17053,
                "datacenter": "sas"
            }
            ]
        },
        "reload_timeout": "900",
        "async_publishing": "enabled",
        "min_alive": {
            "sas": 0,
            "vla": 0
        },
        "full_generation": {
            "name": "20201026_0536",
            "torrent_server_host": "mi01ht.market.yandex.net",
            "torrent_server_port": 80,
            "available_datacenters": [],
            "not_for_publishing": False,
            "reload_phase": None,
            "override_skynet_http_url": True,
            "wait_for_production_requests_to_stop": False
        },
        "packages": {
            "testing": [
            {
                "name": "yandex-market-report",
                "torrent_server_host": "mi01ht.market.yandex.net",
                "torrent_server_port": 80,
                "available_datacenters": [],
                "not_for_publishing": False,
                "reload_phase": None,
                "override_skynet_http_url": False,
                "wait_for_production_requests_to_stop": True,
                "version": "2020.4.72.0",
                "rbtorrent": None
            }
            ]
        },
        "two_phase_reload": "disabled",
        "first_phase_nclusters": 0,
        "close_report_with_old_docs": None
    }
    return json.dumps(config)


@pytest.fixture
def meta_cowboy_config():
    config = {
        "simultaneous_restart": 1,
        "failures_threshold": 3,
        "hosts": {
            "1": [
            {
                "key": "rtc-ct:host3.vla.yandex.net:17050",
                "fqdn": "host3-test-report--f13-17050.gencfg-c.yandex.net",
                "port": 17053,
                "datacenter": "vla"
            }
            ],
            "0": [
            {
                "key": "rtc-ct:host4.sas.yandex.net:17050",
                "fqdn": "host4-test-report--1a4-17050.gencfg-c.yandex.net",
                "port": 17053,
                "datacenter": "sas"
            }
            ]
        },
        "reload_timeout": "900",
        "async_publishing": "enabled",
        "min_alive": {
            "sas": 0,
            "vla": 0
        },
        "full_generation": {
            "name": "20201026_0536",
            "torrent_server_host": "mi01ht.market.yandex.net",
            "torrent_server_port": 80,
            "available_datacenters": [],
            "not_for_publishing": False,
            "reload_phase": None,
            "override_skynet_http_url": True,
            "wait_for_production_requests_to_stop": False
        },
        "packages": {
            "testing": [
            {
                "name": "yandex-market-report",
                "torrent_server_host": "mi01ht.market.yandex.net",
                "torrent_server_port": 80,
                "available_datacenters": [],
                "not_for_publishing": False,
                "reload_phase": None,
                "override_skynet_http_url": False,
                "wait_for_production_requests_to_stop": True,
                "version": "2020.4.72.0",
                "rbtorrent": None
            },
            {
                "name": "yandex-market-report-meta",
                "torrent_server_host": "mi01ht.market.yandex.net",
                "torrent_server_port": 80,
                "available_datacenters": [],
                "not_for_publishing": False,
                "reload_phase": None,
                "override_skynet_http_url": False,
                "wait_for_production_requests_to_stop": True,
                "version": "2020.4.76.0",
                "rbtorrent": None
            }
            ]
        },
        "two_phase_reload": "disabled",
        "first_phase_nclusters": 0,
        "close_report_with_old_docs": None
    }
    return json.dumps(config)


def report_collapse_notification_sent(juggler_push_mock):
    return any(notifications.REPORT_COLLAPSE_SERVICE in call[0] for call in juggler_push_mock.send_event.call_args_list)


def self_test_signal_sent(juggler_push_mock):
    return any(notifications.SELF_TEST_SERVICE in call[0] for call in juggler_push_mock.send_event.call_args_list)


def emergency_flag_signal_sent(juggler_push_mock, flag):
    return any(flag in call[0] for call in juggler_push_mock.send_event.call_args_list)


def test_no_auto_fix_if_ok(args, fake_zk, sb_mock, its_mock, flags_its_mock, ch_mock, ok_signal, juggler_push_mock, i_am_master, fake_access_helper):
    """ No-op if signal is OK """
    # act
    report_watcher.do_run(args, raise_on_fail=True)

    # assert
    its_mock.set_report_emergency_flag.assert_not_called()

    assert not fake_zk.exists('/publisher/test_report_market@atlantis/cowboy-config')

    assert not report_collapse_notification_sent(juggler_push_mock)

    assert int(time.time()) - fake_access_helper.timestamp < 10
    assert len(fake_access_helper.report_groups) == 0
    assert len(fake_access_helper.locations) == 0


def test_auto_fix_if_crit(args, fake_zk, sb_mock, its_mock, flags_its_mock, ch_mock, crit_signal, juggler_push_mock, i_am_master, fake_access_helper):
    """ Auto-fix if signal is CRIT """
    # act
    report_watcher.do_run(args, raise_on_fail=True)

    # assert
    its_mock.set_report_emergency_flag.assert_any_call({'market'}, ['sas', 'vla'], default_values.NANNY_ITS_ENABLE_REPORT_SAFE_MODE_FLAG_NAME, append=True, dry_mode=False)

    cowboy, _ = fake_zk.get('/publisher/test_report_market@atlantis/cowboy-config')
    cowboy = json.loads(cowboy)
    assert cowboy['full_generation']['name'] == '20201010_1010'

    hosts = [host['fqdn'] for host in cowboy['hosts']['0']]
    assert hosts == ['host1.sas.yandex.net', 'host2.vla.yandex.net']

    packages = next(v for v in cowboy['packages'].values())
    assert packages[0]['name'] == 'yandex-market-report'
    assert packages[0]['version'] == '2020.4.123'

    # check meta group
    cowboy, _ = fake_zk.get('/publisher/test_report_meta_market@atlantis/cowboy-config')
    cowboy = json.loads(cowboy)
    assert cowboy['full_generation']['name'] == '20201010_1010'
    hosts = [host['fqdn'] for host in cowboy['hosts']['0']]
    assert hosts == ['host3.sas.yandex.net', 'host4.vla.yandex.net']
    packages = next(v for v in cowboy['packages'].values())
    assert packages[0]['name'] == 'yandex-market-report'
    assert packages[0]['version'] == '2020.4.123'
    assert packages[1]['name'] == 'yandex-market-report-meta'
    assert packages[1]['version'] == '2020.4.125'

    assert report_collapse_notification_sent(juggler_push_mock)

    # check dynamics
    assert int(time.time()) - fake_access_helper.timestamp > 10
    assert sorted(fake_access_helper.report_groups) == sorted(['test_report_market@atlantis', 'test_report_meta_market@atlantis'])
    assert sorted(fake_access_helper.locations) == sorted(['sas', 'vla'])


def test_no_auto_fix_if_not_a_master(args, fake_zk, sb_mock, its_mock, flags_its_mock, ch_mock, crit_signal, juggler_push_mock, fake_access_helper):
    """ No-op if not a master """
    # act
    report_watcher.do_run(args, raise_on_fail=True)

    # assert
    assert not report_collapse_notification_sent(juggler_push_mock)


def test_no_auto_fix_if_disabled(args, fake_zk, sb_mock, its_mock, flags_its_mock, ch_mock, crit_signal, juggler_push_mock, i_am_master, fake_access_helper):
    """ No-op if disabled """
    # arrange
    report_watcher.do_disable(args)

    # act
    report_watcher.do_run(args, raise_on_fail=True)

    # assert
    assert not report_collapse_notification_sent(juggler_push_mock)


def test_auto_fix_if_reenabled(args, fake_zk, sb_mock, its_mock, flags_its_mock, ch_mock, crit_signal, juggler_push_mock, i_am_master, fake_access_helper):
    """ Auto-fix if re-enabled """
    # arrange
    report_watcher.do_disable(args)
    report_watcher.do_enable(args)

    # act
    report_watcher.do_run(args, raise_on_fail=True)

    # assert
    assert report_collapse_notification_sent(juggler_push_mock)


def test_fix_only_given_datacenters(args, fake_zk, sb_mock, its_mock, flags_its_mock, ch_mock, crit_signal, juggler_push_mock, i_am_master, fake_access_helper):
    """ Auto-fix if signal is CRIT """
    # arrange
    args.dc = ['sas']

    # act
    report_watcher.do_run(args, raise_on_fail=True)

    # assert
    its_mock.set_report_emergency_flag.assert_any_call({'market'}, ['sas'], default_values.NANNY_ITS_ENABLE_REPORT_SAFE_MODE_FLAG_NAME, append=True, dry_mode=False)

    cowboy, _ = fake_zk.get('/publisher/test_report_market@atlantis/cowboy-config')
    cowboy = json.loads(cowboy)

    hosts = [host['fqdn'] for host in cowboy['hosts']['0']]
    assert hosts == ['host1.sas.yandex.net']
    assert fake_access_helper.locations == ['sas']


def test_no_auto_fix_if_already_reloading(args, fake_zk, sb_mock, its_mock, flags_its_mock, ch_mock, crit_signal, juggler_push_mock, i_am_master,
                                            cowboy_config, meta_cowboy_config, fake_access_helper):
    """ No-op if group is already in reload """
    # arrange
    fake_zk.set('/publisher/test_report_market@atlantis/cowboy-config', cowboy_config)
    fake_zk.set('/publisher/test_report_meta_market@atlantis/cowboy-config', meta_cowboy_config)

    # act
    report_watcher.do_run(args, raise_on_fail=True)

    # assert
    its_mock.set_report_emergency_flag.assert_not_called()

    assert not report_collapse_notification_sent(juggler_push_mock)


def test_send_self_test_signal_if_auto_fix(args, fake_zk, sb_mock, its_mock, flags_its_mock, ch_mock, crit_signal, juggler_push_mock, i_am_master, fake_access_helper):
    """ Self-test signal is sent if auto-fix has happened """
    # act
    report_watcher.do_run(args, raise_on_fail=True)

    # assert
    assert self_test_signal_sent(juggler_push_mock)


def test_self_test_signal_sent_if_no_auto_fix(args, fake_zk, sb_mock, its_mock, flags_its_mock, ch_mock, ok_signal, juggler_push_mock, i_am_master, fake_access_helper):
    """ Self-test signal is sent if auto-fix has not happened """
    # act
    report_watcher.do_run(args, raise_on_fail=True)

    # assert
    assert self_test_signal_sent(juggler_push_mock)


def test_self_test_signal_sent_if_not_a_master(args, fake_zk, sb_mock, its_mock, flags_its_mock, ch_mock, crit_signal, juggler_push_mock, fake_access_helper):
    """ Self-test signal is sent even if not a master """
    # act
    report_watcher.do_run(args, raise_on_fail=True)

    # assert
    assert self_test_signal_sent(juggler_push_mock)


def test_self_test_signal_not_sent_if_disabled(args, fake_zk, sb_mock, its_mock, flags_its_mock, ch_mock, crit_signal, juggler_push_mock, i_am_master, fake_access_helper):
    """ Self-test signal is not sent if disabled """
    # arrange
    report_watcher.do_disable(args)

    # act
    report_watcher.do_run(args, raise_on_fail=True)

    # assert
    assert not self_test_signal_sent(juggler_push_mock)


def test_self_test_signal_not_sent_if_crashed(args, fake_zk, sb_mock, its_mock, flags_its_mock, ch_mock, crashing_signal, juggler_push_mock, i_am_master, fake_access_helper):
    """ Self-test signal is not sent if crashed """
    # act
    report_watcher.do_run(args)

    # assert
    assert not self_test_signal_sent(juggler_push_mock)


def test_self_test_signal_not_sent_if_not_a_master_and_disabled(args, fake_zk, sb_mock, its_mock, flags_its_mock, ch_mock, crit_signal, juggler_push_mock, fake_access_helper):
    """ Self-test signal is not sent if not a master and disabled """
    # arrange
    report_watcher.do_disable(args)

    # act
    report_watcher.do_run(args, raise_on_fail=True)

    # assert
    assert not self_test_signal_sent(juggler_push_mock)


def test_emergency_flag_safe_mode_signal_is_sent(args, fake_zk, sb_mock, its_mock, flags_its_mock, ch_mock, ok_signal, juggler_push_mock, i_am_master, fake_access_helper):
    """ Test safe mode emergency flag signal """

    # act
    report_watcher.do_alert_flags(args)

    # assert
    assert not emergency_flag_signal_sent(juggler_push_mock, notifications.EMERGENCY_FLAG_SAFE_MODE_SERVICE)

    # enable emergency flag
    flags_its_mock.report_emergency_flags = 'etag', {
        default_values.NANNY_ITS_ENABLE_REPORT_SAFE_MODE_FLAG_NAME: {
            'conditions': [{
                'value': '1',
                'condition': 'IS_TEST'
            }],
            'default_value': '0'
        }
    }

    # act
    report_watcher.do_alert_flags(args)

    # disable emergency flag
    flags_its_mock.report_emergency_flags = 'etag', {}

    # assert
    assert emergency_flag_signal_sent(juggler_push_mock, notifications.EMERGENCY_FLAG_SAFE_MODE_SERVICE)
    assert not emergency_flag_signal_sent(juggler_push_mock, notifications.EMERGENCY_FLAG_REARR_DISABLED_SERVICE)
    assert not emergency_flag_signal_sent(juggler_push_mock, notifications.EMERGENCY_FLAG_RTY_DISABLED_SERVICE)


def test_emergency_flag_rearr_disabled_signal_is_sent(args, fake_zk, sb_mock, its_mock, flags_its_mock, ch_mock, ok_signal, juggler_push_mock, i_am_master, fake_access_helper):
    """ Test rearr disabled emergency flag signal """

    # act
    report_watcher.do_alert_flags(args)

    # assert
    assert not emergency_flag_signal_sent(juggler_push_mock, notifications.EMERGENCY_FLAG_REARR_DISABLED_SERVICE)

    # enable emergency flag
    flags_its_mock.report_emergency_flags = 'etag', {
        default_values.NANNY_ITS_IGNORE_REQUEST_REARR_FLAGS_FLAG_NAME: {
            'conditions': [{
                'value': '1',
                'condition': 'IS_TEST'
            }, {
                'value': '1',
                'condition': 'IS_PREP'
            }],
            'default_value': '0'
        }
    }

    # act
    report_watcher.do_alert_flags(args)

    # disable emergency flag
    flags_its_mock.report_emergency_flags = 'etag', {}

    # assert
    assert not emergency_flag_signal_sent(juggler_push_mock, notifications.EMERGENCY_FLAG_SAFE_MODE_SERVICE)
    assert emergency_flag_signal_sent(juggler_push_mock, notifications.EMERGENCY_FLAG_REARR_DISABLED_SERVICE)
    assert not emergency_flag_signal_sent(juggler_push_mock, notifications.EMERGENCY_FLAG_RTY_DISABLED_SERVICE)


def test_emergency_flag_rty_disabled_signal_is_sent(args, fake_zk, sb_mock, its_mock, flags_its_mock, ch_mock, ok_signal, juggler_push_mock, i_am_master, fake_access_helper):
    """ Test rty disabled emergency flag signal """

    # arrange
    report_watcher.do_disable(args)

    # act
    report_watcher.do_alert_flags(args)

    # assert
    assert not emergency_flag_signal_sent(juggler_push_mock, notifications.EMERGENCY_FLAG_RTY_DISABLED_SERVICE)

    # enable emergency flag
    flags_its_mock.report_emergency_flags = 'etag', {
        default_values.NANNY_ITS_DISABLE_RTY_SERVER_FLAG_NAME: {
            'conditions': [{
                'value': '1',
                'condition': 'IS_TEST'
            }],
            'default_value': '0'
        }
    }

    # act
    report_watcher.do_alert_flags(args)

    # disable emergency flag
    flags_its_mock.report_emergency_flags = 'etag', {}

    # assert
    assert not emergency_flag_signal_sent(juggler_push_mock, notifications.EMERGENCY_FLAG_SAFE_MODE_SERVICE)
    assert not emergency_flag_signal_sent(juggler_push_mock, notifications.EMERGENCY_FLAG_REARR_DISABLED_SERVICE)
    assert emergency_flag_signal_sent(juggler_push_mock, notifications.EMERGENCY_FLAG_RTY_DISABLED_SERVICE)
