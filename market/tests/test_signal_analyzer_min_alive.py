# coding: utf-8

import copy
import datetime
import json
import mock
import os
import pytest
import random

import yatest

from market.idx.admin.auto_subzero.lib import signal_analyzer
from market.idx.admin.auto_subzero.lib.signals import Signal
from market.idx.pylibrary.report_control.helpers import PublisherConfigHelper


TEST_DATA_PATH = yatest.common.source_path('market/idx/admin/auto_subzero/tests/data')
GROUP_THRESHOLD = 0
REPORT_GROUP_NAME = 'report_market_16@atlantis'


class SignalModifier:
    def __init__(self, publisher_config, signal):
        self.publisher_config = publisher_config
        self.signal = copy.deepcopy(signal)
        self.affected_clusters = set()
        self.total_clusters = publisher_config.get_total_clusters(REPORT_GROUP_NAME)
        self.total_min_alive = publisher_config.get_total_min_alive(REPORT_GROUP_NAME)
        self.crit = 0
        self.stale = 0
        self.missing = 0
        random.shuffle(self.signal)

    def set_crit(self, crit=None, greater_than_min_alive=False, dc_lost=False, dc=None):
        if crit is None:
            crit = self.total_clusters - self.total_min_alive
            if dc_lost:
                crit /= signal_analyzer.LOST_DC_ERROR_LEVEL_FACTOR
            if greater_than_min_alive:
                crit += 1
        self.mutate(crit=crit, dc=dc)
        return self

    def set_lost(self, stale=None, missing=None, all_clusters_in_dc=False, dc=None):
        if stale is not None or missing is not None:
            self.mutate(stale=stale, missing=missing, dc=dc)
        else:
            total_cluster_in_dc = self.publisher_config.get_total_clusters_in_dc(REPORT_GROUP_NAME, dc)
            stale = total_cluster_in_dc - 1
            missing = 1
            if not all_clusters_in_dc:
                stale -= 1
            self.mutate(stale=stale, missing=missing, dc=dc)
        return self

    def mutate(self, crit=0, stale=0, missing=0, dc=None):
        # Make dc-specific signal at first place, before making signal for all dc.
        for host_signal in self.signal:
            if not crit and not stale and not missing:
                break
            cluster = self.publisher_config.get_cluster(host_signal.host)
            if cluster in self.affected_clusters:
                continue
            if dc is not None and not cluster.startswith(f'{dc}@'):
                continue
            if crit:
                host_signal.status = 'CRIT'
                self.affected_clusters.add(cluster)
                crit -= 1
                self.crit += 1
            elif stale:
                host_signal.received_time -= signal_analyzer.STALE_SIGNAL_DELAY_SECONDS
                self.affected_clusters.add(cluster)
                stale -= 1
                self.stale += 1
            elif missing:
                self.signal.remove(host_signal)
                self.affected_clusters.add(cluster)
                missing -= 1
                self.missing += 1
        return self


@pytest.yield_fixture(scope='module')
def publisher_config():
    config_path = os.path.join(TEST_DATA_PATH, 'publisher_config.json')
    yield PublisherConfigHelper(config_path)


@pytest.fixture(scope='module')
def signal():
    with open(os.path.join(TEST_DATA_PATH, 'signal.json')) as f:
        now = datetime.datetime.now().timestamp()
        data = json.load(f)
        signal = []
        for host_signal in data:
            host_signal['received_time'] = now
            signal.append(Signal.from_dict(host_signal))
        return signal


def get_signal_modifier(publisher_config, signal):
    return SignalModifier(publisher_config, signal)


def get_analyzer(publisher_config, signal):
    return signal_analyzer.ReportHeartbeatAnalyzer(publisher_config, GROUP_THRESHOLD, signal, use_min_alive=True)


def test_signal_without_failed_clusters(publisher_config, signal):
    analyzer = get_analyzer(publisher_config, signal)
    assert analyzer.crit_hosts == set()
    assert analyzer.stale_hosts == set()
    assert analyzer.missing_hosts == set()
    assert analyzer.failed_groups == set()


def test_min_alive_is_not_reached(publisher_config, signal):
    modifier = get_signal_modifier(publisher_config, signal) \
        .set_lost(stale=1, missing=1) \
        .set_crit(greater_than_min_alive=False)
    analyzer = get_analyzer(publisher_config, modifier.signal)
    assert len(analyzer.crit_hosts) == 12
    assert len(analyzer.stale_hosts) == 1
    assert len(analyzer.missing_hosts) == 1
    assert analyzer.failed_groups == set()


def test_min_alive_is_reached(publisher_config, signal):
    modifier = get_signal_modifier(publisher_config, signal) \
        .set_lost(stale=1, missing=1) \
        .set_crit(greater_than_min_alive=True)
    analyzer = get_analyzer(publisher_config, modifier.signal)
    assert len(analyzer.crit_hosts) == 13
    assert len(analyzer.stale_hosts) == 1
    assert len(analyzer.missing_hosts) == 1
    assert analyzer.failed_groups == {REPORT_GROUP_NAME}


def test_min_alive_is_not_reached_all_dc_open(publisher_config, signal):
    modifier = get_signal_modifier(publisher_config, signal) \
        .set_lost(all_clusters_in_dc=False, dc='man') \
        .set_crit(greater_than_min_alive=False)
    analyzer = get_analyzer(publisher_config, modifier.signal)
    assert len(analyzer.crit_hosts) == 12
    assert len(analyzer.stale_hosts) == 8
    assert len(analyzer.missing_hosts) == 1
    assert analyzer.failed_groups == set()


def test_min_alive_is_not_reached_one_dc_close(publisher_config, signal):
    modifier = get_signal_modifier(publisher_config, signal) \
        .set_lost(all_clusters_in_dc=True, dc='man') \
        .set_crit(greater_than_min_alive=False, dc_lost=True)
    analyzer = get_analyzer(publisher_config, modifier.signal)
    assert len(analyzer.crit_hosts) == 8
    assert len(analyzer.stale_hosts) == 9
    assert len(analyzer.missing_hosts) == 1
    assert analyzer.failed_groups == set()


def test_min_alive_is_reached_one_dc_close(publisher_config, signal):
    modifier = get_signal_modifier(publisher_config, signal) \
        .set_lost(all_clusters_in_dc=True, dc='man') \
        .set_crit(greater_than_min_alive=True, dc_lost=True)
    analyzer = get_analyzer(publisher_config, modifier.signal)
    assert len(analyzer.crit_hosts) == 9
    assert len(analyzer.stale_hosts) == 9
    assert len(analyzer.missing_hosts) == 1
    assert analyzer.failed_groups == {REPORT_GROUP_NAME}


def test_min_alive_is_zero(publisher_config, signal):
    modifier = get_signal_modifier(publisher_config, signal).set_crit(greater_than_min_alive=True)
    with mock.patch.object(PublisherConfigHelper, 'get_total_min_alive', return_value=0):
        analyzer = get_analyzer(publisher_config, modifier.signal)
        assert len(analyzer.crit_hosts) == 13
        assert len(analyzer.stale_hosts) == 0
        assert len(analyzer.missing_hosts) == 0
        assert analyzer.failed_groups == set()
