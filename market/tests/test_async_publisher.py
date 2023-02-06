#!/usr/bin/env python
# -*- coding: utf-8 -*-
from collections import namedtuple, Counter

import pytest
import mock
import json
import socket
import six

from async_publishing import (
    Client as AsyncPublishingClient,
    AsyncPublishingMode,
    HostState,
    BaseSearchHostState,
    SnippetSearchHostState,
    HttpSearchStatus,
    ReportStatus,
)
from lxml import etree
from pyb.plugin.async_publisher import AsyncPublisher, FlapFilter, ReportStatusStruct
from pyb.plugin.async_publisher.backctld_runner import Backctld
from pyb.plugin.async_publisher.market_access_agent import MarketAccessAgent
from pyb.plugin.async_publisher.report_status import UpdateReportStatus
from pyb.plugin.async_publisher.state_collector import StateCollector

# what to test?
# - own state gathering
# - group state gathering
# - reload decision making
# - subscribe after start
# - get_state with/without state file

PACKAGES_STATE = {
    "package-222": {
        "status": "completed",
        "version": "222",
        "name": "package",
    },
    "package-333": {
        "status": "started",
        "version": "333",
        "name": "package",
    },
    "package-444": {
        "status": "started",
        "version": "444",
        "name": "package",
    },
    "package-555": {
        "status": "started",
        "version": "555",
        "name": "package",
    },
}


class BackctldForTests(Backctld):
    def __init__(self, *args):
        super(BackctldForTests, self).__init__(*args)
        self.app_run = None

    def _call(self, command, raise_on_error=False):
        return self.app_run(command)


class StateCollectorForTests(StateCollector):
    def __init__(self, config, async_publishing_client_getter, backctld, report_cluster_id):
        super(StateCollectorForTests, self).__init__(config, async_publishing_client_getter, backctld, report_cluster_id)
        self.call_mock = mock.Mock()

    def _call(self, args):
        return self.call_mock(args)


@pytest.fixture(scope='module')
def async_publisher_config():
    Config = namedtuple('Config', [
        'backcltd_bin',
        'httpsearch_bin',
        'httpsearch_role',
        'report_config_path',
        'zk_config_path',
        'zk_root_prefix',
        'state_file_path',
        'reload_lock_path',
        'action_state_path',
        'action_state_log_path',
        'hosts_state_dump_path',
        'clusters_state_dump_path',
        'ignore_not_for_publishing',
        'use_separate_dist_generations',
        'access_agent_host',
        'access_agent_port',
        'access_agent_use_for_index',
        'access_agent_consumer_name',
        'access_agent_index_dist_name',
        'hardware_info_file_path',
    ])
    return Config(
        backcltd_bin='backctld',
        httpsearch_bin='httpsearch',
        httpsearch_role='market',
        report_config_path='report.conf',
        zk_config_path='zookeeper.conf',
        zk_root_prefix='/publisher',
        state_file_path='state.json',
        reload_lock_path='reload.lock',
        action_state_path='action_state.json',
        action_state_log_path='action_state.log',
        hosts_state_dump_path='hosts_state_dump.json',
        clusters_state_dump_path='clusters_state_dump.json',
        ignore_not_for_publishing=False,
        use_separate_dist_generations=False,
        access_agent_host='localhost',
        access_agent_port=17069,
        access_agent_use_for_index=False,
        access_agent_consumer_name='market_report_fresh_base',
        access_agent_index_dist_name='fresh_index_dist_00',
        hardware_info_file_path=None,
    )


@pytest.fixture(scope='module')
def mock_context():
    Context = namedtuple('Context', [
        'report_cluster_id'
    ])
    return Context(
        report_cluster_id='test@market@iva@00'
    )


@pytest.fixture(scope='module')
def async_publisher_cls(mock_context):
    class AsyncPublisherForTests(AsyncPublisher):
        App = namedtuple('App', ['run'])

        @staticmethod
        def app_run(cmd_list):
            service, cmd = cmd_list[:2]
            if service == 'package_installer':
                if cmd == 'get_packages':
                    return '{}'
                if cmd == 'get_force_report_restart':
                    return 'false'
            if service == 'updater':
                if cmd == 'get_deploy_path':
                    return '.'
            raise Exception("Backctld mock does not know such cmd:{}".format(cmd_list))

        def __init__(self, config):
            backctld = BackctldForTests(None, None)
            backctld.app_run = self.app_run

            self._config = config
            self._backctld = backctld
            self.__client_mock = mock.create_autospec(AsyncPublishingClient, instance=True)
            # self.__zk_client = ZkClient(os.environ["ZK_HOST"])
            # self.__client_mock = AsyncPublishingClient(self.__zk_client, "/publisher")
            self._reload_attempts = Counter()
            self._state_cache = {}
            self.last_packages_check = 0
            self._subscribed_for_new_generations = False
            self._subscribed_for_ban = False
            self.context = mock_context
            self._state_collector = StateCollectorForTests(self._config, lambda: self.__client_mock, self._backctld, self.context.report_cluster_id)

        @AsyncPublisher._client.getter
        def _client(self):
            return self.__client_mock

    return AsyncPublisherForTests


@pytest.fixture(scope='module')
def access_agent_state():
    return {
        "resource": {
            "fresh_index_dist_00": {
                "in_download": [
                    {
                        "key": "1354.0.0",
                        "value": {
                            "done": True,
                            "done_time": {
                                "seconds": 1633003038,
                            },
                        },
                    },
                    {
                        "key": "1353.0.0",
                        "value": {
                            "done": True,
                            "done_time": {
                                "seconds": 1633003038,
                            },
                        },
                    },
                    {
                        "key": "373.0.0",
                        "value": {
                            "done": True,
                            "done_time": {
                                "seconds": 1633336507,
                            },
                            "error": "(yexception)",
                        },
                    },
                ],
                "in_install": [
                    {
                        "key": "1354.0.0",
                        "value": {
                            "done": True,
                        },
                    },
                ],
                "in_load": [
                    {
                        "key": "1354.0.0",
                        "value": {
                            "done": False,
                        },
                    },
                ],
            }
        }
    }


@pytest.fixture(scope='module')
def report_admin_action_versions_response():
    return etree.fromstring(six.ensure_binary(
        '<?xml version="1.0" encoding="utf-8"?>'
        '<admin-action>'
            '<report>2021.4.243.0</report>'
            '<revision>8950596</revision>'
            '<host>sas2-2012-sas-market-prod-report--7c4-17050.gencfg-c.yandex.net</host>'
            '<market-indexer-version>2021.4.2586.0</market-indexer-version>'
            '<mbo-stuff>20211215_1136</mbo-stuff>'
            '<index-generation>20211217_0818</index-generation>'
            '<regional_delivery_fb></regional_delivery_fb>'
            '<num-offers>368364113</num-offers>'
            '<num-blue-offers>21093637</num-blue-offers>'
            '<report-status>CLOSED_CONSISTENT_MANUAL_OPENING</report-status>'
            '<report-lockdown></report-lockdown>'
            '<report-safe-mode>0</report-safe-mode>'
            '<dssm>0.8645999</dssm>'
            '<formulas>0.8959790</formulas>'
            '<color>white</color>'
            '<flat-report>enabled</flat-report>'
            '<replica-count>6</replica-count>'
            '<report-cpu-usage>15.84076363</report-cpu-usage>'
            '<report-cpu-limit>33.4957883</report-cpu-limit>'
            '<report-stats>'
                '<rty-backup>'
                    '<index ts="1639738801" time="Fri, 17 Dec 2021 14:00:01 MSK"/>'
                    '<backup_in_progress>False</backup_in_progress>'
                    '<was_restore>False</was_restore>'
                '</rty-backup>'
                '<dynamic-rollback>'
                    '<qpromos_filter timestamp="2021-12-17T05:18:00.000000Z"></qpromos_filter>'
                '</dynamic-rollback>'
                '<last-rty-document-freshness>'
                    '1639738836'
                '</last-rty-document-freshness>'
                '<dynamic-data>'
                    '<cpashopfilter timestamp="2021-12-17T10:41:06.000000Z">'
                        '#33443300'
                    '</cpashopfilter>'
                    '<cpcshopfilter timestamp="2021-12-17T10:41:06.000000Z">'
                        '#33443300'
                    '</cpcshopfilter>'
                    '<offerfilter timestamp="2021-12-17T10:41:06.000000Z">'
                        '#33443300'
                    '</offerfilter>'
                '</dynamic-data>'
            '</report-stats>'
        '</admin-action>'
    ))


class FakeUpdateReportStatus(object):
    def __init__(self, report_status):
        self._report_status = report_status
        self.container_port = 23

    def get(self):
        return ReportStatusStruct(
            self._report_status.get(),
            0,
            0,
            0,
            '2020.1.1.0',
            1,
            0.1,
            0.1,
            'white',
            'disabled',
            1581944370,
        )


def test_subscribe_for_new_generations(async_publisher_cls, async_publisher_config):
    publisher = async_publisher_cls(async_publisher_config)
    publisher._subscribe_for_new_generations()


def test_gather_self_state(async_publisher_cls, async_publisher_config):
    publisher = async_publisher_cls(async_publisher_config)
    publisher._state_collector.gather_self_state(FakeUpdateReportStatus(FlapFilter(15, ReportStatus.OPENED_CONSISTENT)))


def test_gather_self_state_on_basesearch(async_publisher_cls, async_publisher_config, tmp_path):
    publisher = async_publisher_cls(async_publisher_config)
    publisher._client.my_host_config.dists = {
        'marketsearch3': set(['part-0', 'stats']),
    }

    dynamic_data_timestamp = 1581944370
    (tmp_path / 'timestamp').write_text(six.text_type(dynamic_data_timestamp))

    def app_run(cmd_list):
        service, cmd = cmd_list[:2]
        if service == 'marketsearch3':
            if cmd == 'get_dist_generations':
                return '20180101_1000,20180101_1200'
            elif cmd == 'get_generation':
                return '20180101_1200'
            elif cmd == 'is_reloading':
                return 'false'
            elif cmd == 'get_generations_in_progress':
                return ''
        if service == 'package_installer':
            if cmd == 'get_packages':
                return json.dumps(PACKAGES_STATE)
            if cmd == 'get_force_report_restart':
                return 'false'
            elif cmd == 'get_generations_in_progress':
                return ''
        if service == 'updater':
            if cmd == 'get_deploy_path':
                return str(tmp_path)

        raise Exception("Backctld mock does not know such cmd:{}".format(cmd_list))

    backctld = BackctldForTests(None, None)
    backctld.app_run = app_run

    publisher._backctld = backctld
    publisher._state_collector._backctld = backctld
    publisher._state_collector.call_mock.return_value = 0

    state = publisher._state_collector.gather_self_state(FakeUpdateReportStatus(FlapFilter(15, ReportStatus.OPENED_CONSISTENT)))
    # check that we wrote state to file and it's the same state as function result
    with open(async_publisher_config.state_file_path) as fd:
        files_state = HostState.from_str(fd.read())
    assert state == files_state
    # on base search host we create BaseSearchHostState
    assert isinstance(state, BaseSearchHostState)
    assert state.downloaded_generations == {
        'marketsearch3': {'20180101_1000', '20180101_1200'},
    }
    assert state.active_generations == {
        'marketsearch3': '20180101_1200',
    }
    assert state.services == {'marketsearch3'}
    assert not state.is_reloading
    assert state.httpsearch_status is HttpSearchStatus.UP
    assert state.report_status is ReportStatus.OPENED_CONSISTENT
    assert state.packages == PACKAGES_STATE
    assert state.fqdn == socket.getfqdn()
    assert state.dynamic_data_timestamp == dynamic_data_timestamp
    assert state.port == 23


def test_gather_self_state_on_basesearch_with_market_access(access_agent_state, async_publisher_cls, async_publisher_config, tmp_path):
    async_publisher_config = async_publisher_config._replace(access_agent_use_for_index=True)
    publisher = async_publisher_cls(async_publisher_config)
    publisher._client.my_host_config.dists = {
        'marketsearch3': set(['part-0', 'stats']),
    }

    dynamic_data_timestamp = 1581944370
    (tmp_path / 'timestamp').write_text(six.text_type(dynamic_data_timestamp))

    def app_run(cmd_list):
        service, cmd = cmd_list[:2]
        if service == 'marketsearch3':
            if cmd == 'is_reloading':
                return 'false'
        if service == 'package_installer':
            if cmd == 'get_packages':
                return json.dumps(PACKAGES_STATE)
        if service == 'updater':
            if cmd == 'get_deploy_path':
                return str(tmp_path)
        raise Exception("Backctld mock does not know such cmd:{}".format(cmd_list))

    def call_access_agent(url):
        if 'market_report_fresh_base' in url:
            return access_agent_state
        raise Exception('No such consumer')

    agent = MarketAccessAgent('localhost', 17069)
    agent._call = call_access_agent

    backctld = BackctldForTests(None, None)
    backctld.app_run = app_run

    publisher._backctld = backctld
    publisher._state_collector._backctld = backctld
    publisher._state_collector._access_agent = agent
    publisher._state_collector.call_mock.return_value = 0

    state = publisher._state_collector.gather_self_state(FakeUpdateReportStatus(FlapFilter(15, ReportStatus.OPENED_CONSISTENT)))
    # check that we wrote state to file and it's the same state as function result
    with open(async_publisher_config.state_file_path) as fd:
        files_state = HostState.from_str(fd.read())
    assert state == files_state
    # on base search host we create BaseSearchHostState
    assert isinstance(state, BaseSearchHostState)
    assert state.downloaded_generations == {
        'marketsearch3': {'1354.0.0', '1353.0.0'},
    }
    assert state.active_generations == {'marketsearch3': '1354.0.0'}
    assert state.services == {'marketsearch3'}
    assert not state.is_reloading
    assert state.httpsearch_status is HttpSearchStatus.UP
    assert state.report_status is ReportStatus.OPENED_CONSISTENT
    assert state.packages == PACKAGES_STATE
    assert state.fqdn == socket.getfqdn()
    assert state.dynamic_data_timestamp == dynamic_data_timestamp
    assert state.port == 23


def test_gather_self_state_on_snippetsearch(async_publisher_cls, async_publisher_config):
    publisher = async_publisher_cls(async_publisher_config)
    publisher._client.my_host_config.dists = {
        'marketsearchsnippet': set(['snippet-part-0']),
    }

    def app_run(cmd_list):
        service, cmd = cmd_list[:2]
        if service == 'marketsearchsnippet':
            if cmd == 'get_dist_generations':
                return '20180101_1000,20180101_1200'
            elif cmd == 'get_generation':
                return '20180101_1200'
            elif cmd == 'is_reloading':
                return 'false'
            elif cmd == 'get_generations_in_progress':
                return ''
        if service == 'package_installer':
            if cmd == 'get_packages':
                return json.dumps(PACKAGES_STATE)
            if cmd == 'get_force_report_restart':
                return 'false'
            elif cmd == 'get_generations_in_progress':
                return ''
        if service == 'updater':
            if cmd == 'get_deploy_path':
                return '.'

        raise Exception("Backctld mock does not know such cmd:{}".format(cmd_list))

    backctld = BackctldForTests(None, None)
    backctld.app_run = app_run

    publisher._backctld = backctld
    publisher._state_collector._backctld = backctld
    publisher._state_collector.call_mock.return_value = 0

    state = publisher._state_collector.gather_self_state(FakeUpdateReportStatus(ReportStatus.OPENED_CONSISTENT))

    # check that we wrote state to file and it's the same state as function result
    with open(async_publisher_config.state_file_path) as fd:
        files_state = HostState.from_str(fd.read())
    assert state == files_state
    # on snippet search host we create SnippetSearchHostState
    assert isinstance(state, SnippetSearchHostState)
    assert state.downloaded_generations == {
        'marketsearchsnippet': {'20180101_1000', '20180101_1200'},
    }
    assert state.active_generations == {
        'marketsearchsnippet': '20180101_1200',
    }
    assert state.services == {'marketsearchsnippet'}
    assert state.packages == PACKAGES_STATE
    assert not state.is_reloading
    assert state.httpsearch_status is HttpSearchStatus.UP
    # don't try to get admin_action=versions from snippet report
    assert state.fqdn == socket.getfqdn()
    assert state.port == 23


def test_group_master_loop(async_publisher_cls, async_publisher_config):
    publisher = async_publisher_cls(async_publisher_config)
    # master loop will work only once

    async_publishing_mock = mock.PropertyMock(side_effect=[
        AsyncPublishingMode.enabled,
        AsyncPublishingMode.enabled,
        AsyncPublishingMode.enabled,
        AsyncPublishingMode.enabled,
        AsyncPublishingMode.disabled,
    ])

    publisher._client.my_group_config.__class__.async_publishing = async_publishing_mock
    publisher._client.my_group_config.full_generation._not_for_publishing = False
    publisher._client.full_generation._not_for_publishing = False
    publisher._group_master_loop(delay_between_iterations=0, report_status_updater=mock.Mock(container_port=23))


def test_main_loop_with_upload(async_publisher_cls, async_publisher_config):
    publisher = async_publisher_cls(async_publisher_config)
    # master loop will work only once

    # Tricky
    async_publishing_mock = mock.PropertyMock(side_effect=[
        AsyncPublishingMode.upload,
        AsyncPublishingMode.upload,
        AsyncPublishingMode.upload,
        AsyncPublishingMode.upload,
        AsyncPublishingMode.disabled,
    ])

    publisher._client.my_group_config.__class__.async_publishing = async_publishing_mock
    publisher._client.my_group_config.full_generation._not_for_publishing = False
    publisher._client.full_generation._not_for_publishing = False
    publisher._group_master_loop(delay_between_iterations=0, report_status_updater=mock.Mock(container_port=23))


def test_not_for_publishing(async_publisher_cls, async_publisher_config):
    '''
    Проверяем, что поколения с флагом not_for_publishing=True, не скачиваются и не релоадятся
    '''
    class _AsyncPublisher(async_publisher_cls):
        _download_full_generation = mock.Mock()

    publisher = _AsyncPublisher(async_publisher_config)

    # master loop will work only once

    async_publishing_mock = mock.PropertyMock(side_effect=[
        AsyncPublishingMode.enabled,
        AsyncPublishingMode.enabled,
        AsyncPublishingMode.enabled,
        AsyncPublishingMode.enabled,
        AsyncPublishingMode.disabled,
    ])

    publisher._client.my_group_config.__class__.async_publishing = async_publishing_mock
    publisher._client.my_group_config.full_generation._not_for_publishing = True
    publisher._client.full_generation._not_for_publishing = True

    with mock.patch('async_publishing.group_reload_actions') as mock_group_reload_actions:
        publisher._group_master_loop(delay_between_iterations=0, report_status_updater=mock.Mock(container_port=23))
        for args in mock_group_reload_actions.call_args_list:
            assert args[0][1].full_generation is None
            assert args[0][2] is None
        assert not _AsyncPublisher._download_full_generation.called


def test_ignore_not_for_publishing(async_publisher_cls, async_publisher_config):
    '''
    Проверяем, что с параметром конфига ignore_not_for_publishing=True, флаг not_for_publishing игнорируется
    '''
    cfg = async_publisher_config._replace(ignore_not_for_publishing=True)

    class _AsyncPublisher(async_publisher_cls):
        _download_full_generation = mock.Mock()

    publisher = _AsyncPublisher(cfg)

    # master loop will work only once

    async_publishing_mock = mock.PropertyMock(side_effect=[
        AsyncPublishingMode.enabled,
        AsyncPublishingMode.enabled,
        AsyncPublishingMode.enabled,
        AsyncPublishingMode.enabled,
        AsyncPublishingMode.disabled,
    ])

    publisher._client.my_group_config.__class__.async_publishing = async_publishing_mock
    publisher._client.my_group_config.full_generation._not_for_publishing = True
    publisher._client.full_generation._not_for_publishing = True

    with mock.patch('async_publishing.group_reload_actions') as mock_group_reload_actions:
        publisher._group_master_loop(delay_between_iterations=0, report_status_updater=mock.Mock(container_port=23))
        for args in mock_group_reload_actions.call_args_list:
            assert args[0][1].full_generation is not None
            assert args[0][2] is not None
        assert _AsyncPublisher._download_full_generation.called


def test_market_access_agent_get_consumer_state(access_agent_state):
    """
    Проверяем, что вызов MarketAccessAgent.get_consumer_state корректно форматирует результаты, полученные из аксесс-агента.
    """

    agent = MarketAccessAgent('localhost', 17069)
    agent._call = lambda _: access_agent_state
    consumer_state = agent.get_consumer_state('market_report_fresh_base')
    assert len(consumer_state) == 1
    assert 'fresh_index_dist_00' in consumer_state

    resource_state = consumer_state['fresh_index_dist_00']
    assert len(resource_state.in_download) == 3
    assert len(resource_state.in_install) == 1
    assert len(resource_state.in_load) == 1

    assert '1354.0.0' in resource_state.in_download
    assert '1353.0.0' in resource_state.in_download
    assert '373.0.0' in resource_state.in_download
    assert '1354.0.0' in resource_state.in_install
    assert '1354.0.0' in resource_state.in_load

    assert resource_state.in_download['1354.0.0'].done
    assert resource_state.in_download['1354.0.0'].error is None
    assert resource_state.in_download['1353.0.0'].done
    assert resource_state.in_download['1353.0.0'].error is None
    assert resource_state.in_download['373.0.0'].done
    assert resource_state.in_download['373.0.0'].error is not None

    assert resource_state.in_install['1354.0.0'].done
    assert resource_state.in_install['1354.0.0'].error is None

    assert not resource_state.in_load['1354.0.0'].done
    assert resource_state.in_load['1354.0.0'].error is None


def test_parse_market_dynamic_timestamp(report_admin_action_versions_response):
    ts = UpdateReportStatus._parse_market_dynamic_timestamp(report_admin_action_versions_response)
    assert ts == 1639737666
