from datetime import datetime
import json
import pytest
import tempfile

from market.idx.pylibrary.report_control import default_values as defaults
from market.idx.pylibrary.report_control.helpers import PublisherConfigHelper
from market.idx.admin.auto_subzero.lib.signals import Signal
from market.idx.admin.auto_subzero.lib.signal_analyzer import (
    ReportHeartbeatAnalyzer,
    IGNORED_REPORT_GROUPS,
    STALE_SIGNAL_DELAY_SECONDS
)


GROUP_NAME = 'report_market@atlantis'
EXCEPTION_REPORT_GROUP_NAME = next(iter(defaults.EXCEPTION_REPORT_GROUPS))
IGNORED_GROUP_NAME = next(iter(IGNORED_REPORT_GROUPS))


@pytest.fixture
def now_ts():
    return datetime.now().timestamp()


def convert_json_to_signal(json_signal):
    return [Signal.from_dict(s) for s in json_signal]


@pytest.fixture
def signal(now_ts):
    json_signal = [
        {
            "status": "CRIT",
            "received_time": now_ts,
            "service": "market-report-heartbeat",
            "tags": [],
            "instance": "",
            "host": "vla2-0568-595-vla-market-prod--08c-17050.gencfg-c.yandex.net",
            "heartbeat": 60.0,
            "digest": ""
        },
        {
            "status": "OK",
            "received_time": now_ts,
            "service": "market-report-heartbeat",
            "tags": [],
            "instance": "",
            "host": "vla2-0600-595-vla-market-prod--08c-17050.gencfg-c.yandex.net",
            "heartbeat": 60.0,
            "digest": ""
        },
        {
            "status": "OK",
            "received_time": now_ts,
            "service": "market-report-heartbeat",
            "tags": [],
            "instance": "",
            "host": "man2-1409-1f1-man-market-prep--367-17050.gencfg-c.yandex.net",
            "heartbeat": 60.0,
            "digest": ""
        },
        {
            "status": "CRIT",
            "received_time": now_ts,
            "service": "market-report-heartbeat",
            "tags": [],
            "instance": "",
            "host": "vla3-1740-b1d-vla-market-prod--8cd-17050.gencfg-c.yandex.net",
            "heartbeat": 60.0,
            "digest": ""
        },
    ]
    return convert_json_to_signal(json_signal)


@pytest.fixture
def lost_signal(now_ts):
    json_signal = [
        {
            "status": "OK",
            "received_time": now_ts,
            "service": "market-report-heartbeat",
            "tags": [],
            "instance": "",
            "host": "man2-1409-1f1-man-market-prep--367-17050.gencfg-c.yandex.net",
            "heartbeat": 60.0,
            "digest": ""
        },
    ]
    return convert_json_to_signal(json_signal)


@pytest.fixture
def stale_signal(now_ts):
    json_signal = [
        {
            "status": "OK",
            "received_time": now_ts - STALE_SIGNAL_DELAY_SECONDS + 1,
            "service": "market-report-heartbeat",
            "tags": [],
            "instance": "",
            "host": "vla2-0568-595-vla-market-prod--08c-17050.gencfg-c.yandex.net",
            "heartbeat": 60.0,
            "digest": ""
        },
        {
            "status": "OK",
            "received_time": now_ts - STALE_SIGNAL_DELAY_SECONDS,
            "service": "market-report-heartbeat",
            "tags": [],
            "instance": "",
            "host": "vla2-0600-595-vla-market-prod--08c-17050.gencfg-c.yandex.net",
            "heartbeat": 60.0,
            "digest": ""
        },
        {
            "status": "OK",
            "received_time": now_ts,
            "service": "market-report-heartbeat",
            "tags": [],
            "instance": "",
            "host": "vla3-1740-b1d-vla-market-prod--8cd-17050.gencfg-c.yandex.net",
            "heartbeat": 60.0,
            "digest": ""
        },
    ]
    return convert_json_to_signal(json_signal)


@pytest.yield_fixture
def publisher_config():
    config = {
        "dcgroups": {
            GROUP_NAME: {
                "async_publishing_mode": "enabled",
                "close_firewall_sleep": 15,
                "close_report_with_old_docs": 1800.0,
                "failures_threshold": 1,
                "generations_prefix": "generations",
                "hosts": {
                    "atlantis.00.rtc.vla2-0568.search.yandex.net@17050": {
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
                        "key": "rtc-ct:vla2-0568.search.yandex.net:17050",
                        "name": "vla2-0568-595-vla-market-prod--08c-17050.gencfg-c.yandex.net",
                        "port": 17053,
                        "redundancy": 1,
                        "rtc_host": "vla2-0568.search.yandex.net",
                        "rtc_port": 17050,
                        "rtc_service": "prod_report_shadow_vla",
                        "service": "marketsearch3"
                    },
                    "atlantis.00.rtc.vla2-0600.search.yandex.net@17050": {
                        "cluster": 2,
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
                        "key": "rtc-ct:vla2-0600.search.yandex.net:17050",
                        "name": "vla2-0600-595-vla-market-prod--08c-17050.gencfg-c.yandex.net",
                        "port": 17053,
                        "redundancy": 1,
                        "rtc_host": "vla2-0600.search.yandex.net",
                        "rtc_port": 17050,
                        "rtc_service": "prod_report_shadow_vla",
                        "service": "marketsearch3"
                    },
                },
                "min_alive": {
                    "total": 2,
                    "vla": 0
                }
            },
            IGNORED_GROUP_NAME: {
                "async_publishing_mode": "enabled",
                "close_firewall_sleep": 15,
                "close_report_with_old_docs": 1800.0,
                "failures_threshold": 1,
                "generations_prefix": "generations",
                "hosts": {
                    "atlantis.00.rtc.vla3-1740.search.yandex.net@17050": {
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
                        "key": "rtc-ct:vla3-1740.search.yandex.net:17050",
                        "name": "vla3-1740-b1d-vla-market-prod--8cd-17050.gencfg-c.yandex.net",
                        "port": 17053,
                        "redundancy": 1,
                        "rtc_host": "vla3-1740.search.yandex.net",
                        "rtc_port": 17050,
                        "rtc_service": "prod_report_shadow_vla",
                        "service": "marketsearch3"
                    },
                },
                "min_alive": {
                    "total": 1,
                    "vla": 0
                }
            },
            EXCEPTION_REPORT_GROUP_NAME: {
                "async_publishing_mode": "enabled",
                "close_firewall_sleep": 15,
                "close_report_with_old_docs": 1800.0,
                "failures_threshold": 1,
                "generations_prefix": "generations",
                "hosts": {
                    "atlantis.00.rtc.vla3-1740.search.yandex.net@17050": {
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
                        "key": "rtc-ct:vla3-1740.search.yandex.net:17050",
                        "name": "vla3-1740-b1d-vla-market-prod--8cd-17050.gencfg-c.yandex.net",
                        "port": 17053,
                        "redundancy": 1,
                        "rtc_host": "vla3-1740.search.yandex.net",
                        "rtc_port": 17050,
                        "rtc_service": "prod_report_shadow_vla",
                        "service": "marketsearch3"
                    },
                },
                "min_alive": {
                    "total": 1,
                    "vla": 0
                }
            }
        }
    }

    with tempfile.NamedTemporaryFile(mode='w+t') as f:
        json.dump(config, f)
        f.flush()
        yield PublisherConfigHelper(f.name)


def test_parsing_failed_hosts(signal, publisher_config):
    threshold = 0.5
    analyzer = ReportHeartbeatAnalyzer(publisher_config, threshold, signal)
    crit_signal_hosts = {
        'vla2-0568-595-vla-market-prod--08c-17050.gencfg-c.yandex.net',
        'vla3-1740-b1d-vla-market-prod--8cd-17050.gencfg-c.yandex.net',
    }
    assert analyzer.crit_hosts == crit_signal_hosts
    assert analyzer.stale_hosts == set()
    assert analyzer.missing_hosts == set()


def test_parsing_stale_signal(stale_signal, publisher_config):
    threshold = 0.5
    analyzer = ReportHeartbeatAnalyzer(publisher_config, threshold, stale_signal)
    stale_signal_hosts = {'vla2-0600-595-vla-market-prod--08c-17050.gencfg-c.yandex.net'}
    assert analyzer.crit_hosts == set()
    assert analyzer.stale_hosts == stale_signal_hosts
    assert analyzer.missing_hosts == set()


def test_parsing_lost_signal(lost_signal, publisher_config):
    threshold = 0.5
    analyzer = ReportHeartbeatAnalyzer(publisher_config, threshold, lost_signal)
    lost_signal_hosts = {
        'vla2-0568-595-vla-market-prod--08c-17050.gencfg-c.yandex.net',
        'vla2-0600-595-vla-market-prod--08c-17050.gencfg-c.yandex.net',
        'vla3-1740-b1d-vla-market-prod--8cd-17050.gencfg-c.yandex.net',
    }
    assert analyzer.crit_hosts == set()
    assert analyzer.stale_hosts == set()
    assert analyzer.missing_hosts == lost_signal_hosts


def test_failed_groups_by_threshold(signal, publisher_config):
    threshold = 0.5
    analyzer = ReportHeartbeatAnalyzer(publisher_config, threshold, signal)
    assert analyzer.failed_groups == {GROUP_NAME}


def test_failed_groups_by_high_threshold(signal, publisher_config):
    threshold = 0.8
    analyzer = ReportHeartbeatAnalyzer(publisher_config, threshold, signal)
    crit_signal_hosts = {
        'vla2-0568-595-vla-market-prod--08c-17050.gencfg-c.yandex.net',
        'vla3-1740-b1d-vla-market-prod--8cd-17050.gencfg-c.yandex.net',
    }
    assert analyzer.crit_hosts == crit_signal_hosts
    assert analyzer.stale_hosts == set()
    assert analyzer.missing_hosts == set()
    assert analyzer.failed_groups == set()
