from market.tools.resource_monitor.lib.abc import AbcClient
from market.tools.resource_monitor.lib.bot import BotClient
from market.tools.resource_monitor.lib.calc import run_all, run_yt_dtables
from market.tools.resource_monitor.lib.conductor import ConductorClient
from market.tools.resource_monitor.lib.gencfg import GenCfgClient
from market.tools.resource_monitor.lib.nanny import NannyClient
from market.tools.resource_monitor.lib.st import StClient

import re
import mock
from mock import Mock, patch
import json
from library.python import resource

import yt.wrapper as yt
from yt.wrapper import YtClient

import os
import time


def nanny_services_response(category, skip):
    if skip is None:
        data_str = resource.find('/data/nanny_services_response.json')
        return json.loads(data_str)
    return {'result': []}


def gencfg_response(request_url, data=None):
    if request_url.find('searcherlookup/groups') != -1:
        data_str = resource.find('/data/gencfg_group_info_response.json')
    elif request_url.find('groups') != -1:
        data_str = resource.find('/data/gencfg_groups_response.json')
    else:
        return {}
    return json.loads(data_str)


def conductor_response(request_url, data=None):
    if request_url.find('api/projects') != -1:
        data_str = resource.find('/data/conductor_projects_response.xml')
    elif request_url.find('projects2hosts') != -1:
        data_str = resource.find('/data/conductor_hosts_response.tsv')
    else:
        return None
    return data_str


def abc_response(request_url, params=None):
    service_info_test_data = {
        'meta_market': '/data/abc_service_info_response.json',
    }
    services_test_data = {
        '905': '/data/abc_services_2_response.json'
    }
    patterns = {
        r'^.*/services/\?slug=(?P<key>[a-z_]+)': service_info_test_data,
        r'^.*/services/\?parent=(?P<key>\d+)&.*': services_test_data
    }
    empty_file_name = '/data/abc_empty_response.json'
    filename = empty_file_name
    for pattern, test_data in patterns.iteritems():
        m = re.match(pattern, request_url)
        if m:
            key = m.group('key')
            if key:
                filename = test_data.get(key, empty_file_name)
            break
    return json.loads(resource.find(filename))


@patch.object(BotClient, '_get')
@patch.object(GenCfgClient, '_get')
@patch.object(NannyClient, '_request_services')
def test_run_all(nanny_fn, gencfg_fn, bot_fn):
    nanny_fn.side_effect = nanny_services_response
    gencfg_fn.side_effect = gencfg_response
    bot_fn.return_value = resource.find('/data/bot_servers_response.tsv')

    output_path = '//home/market/production/infra/resources'
    run_all(nanny_token='some_nanny_token', yt_path=output_path, yt_proxy=os.environ['YT_PROXY'] )

    yt_client = YtClient(os.environ["YT_PROXY"])
    rtc_path = os.path.join(output_path, 'rtc')
    bm_path = os.path.join(output_path, 'bm')
    assert yt_client.exists(rtc_path)
    assert yt_client.exists(bm_path)
    assert yt_client.exists(os.path.join(rtc_path, 'recent'))
    assert len(yt_client.list(rtc_path)) == 2
    assert yt_client.exists(os.path.join(bm_path, 'recent'))
    assert len(yt_client.list(bm_path)) == 2

    bot_resp = [json.loads(line) for line in yt.read_table(os.path.join(bm_path, 'recent'), raw=True, format='json').read().split('\n') if line]
    assert any(map(lambda item: item['serial'] == '900156390' and item['source'] == 'cold_reserve', bot_resp))
    assert any(map(lambda item: item['serial'] == '900156141' and item['source'] == 'bare_metal', bot_resp))

    rtc_resp = [json.loads(line) for line in yt.read_table(os.path.join(rtc_path, 'recent'), raw=True, format='json').read().split('\n') if line]
    assert any(map(lambda item: item['dc'] == 'sas', rtc_resp))


@patch.object(AbcClient, '_get')
@patch.object(BotClient, '_get')
@patch.object(ConductorClient, '_get')
@patch.object(GenCfgClient, '_get')
@patch.object(NannyClient, '_request_services')
def test_conductor_and_abc(nanny_fn, gencfg_fn, conductor_fn, bot_fn, abc_fn):
    nanny_fn.side_effect = nanny_services_response
    gencfg_fn.side_effect = gencfg_response
    conductor_fn.side_effect = conductor_response
    bot_fn.return_value = resource.find('/data/bot_servers_response.tsv')
    abc_fn.side_effect = abc_response

    output_path = '//home/market/production/infra/resources'
    run_all(nanny_token='some_nanny_token', abc_root='meta_market', abc_token='a token', yt_path=output_path, yt_proxy=os.environ['YT_PROXY'] )

    yt_client = YtClient(os.environ["YT_PROXY"])
    bm_path = os.path.join(output_path, 'bm')
    assert yt_client.exists(bm_path)
    bm_data = [json.loads(line) for line in yt.read_table(os.path.join(bm_path, 'recent'), raw=True, format='json').read().split('\n') if line]
    assert any(map(lambda item: item['abc_service'] == 'pricalabs', bm_data))


def make_group_mock(responsibles):
    return Mock(attributes={'upravlyator_responsibles': responsibles})


def make_groups_mock_response(groups_dict):
    return dict((group_name, make_group_mock(responsibles)) for group_name, responsibles in groups_dict.iteritems())


def make_tablet_cells_response(cells_statistics):
    m = {}
    for cell_id, statistics in cells_statistics.iteritems():
        m[cell_id] = Mock(attributes={
            'total_statistics': statistics
        })
    return m


def make_tablet_cell_bundles_response(data):
    m = {}
    for bundle_name, bundle in data.iteritems():
        m[bundle_name] = Mock(attributes={
            'name': bundle_name,
            'tablet_cell_ids': bundle.get('tablet_cell_ids'),
            'options': {
                'changelog_account': bundle.get('changelog_account')
            }
        })
    return m


def make_accounts_response(data):
    return {account_name: Mock(attributes=attrs) for account_name, attrs in data.iteritems()}

def make_yt_mock_response(data):
    def mock_func(path, **kwargs):
        return data.get(path)
    return mock_func


@patch.object(StClient, '_get')
@patch.object(YtClient, 'get')
def test_yt_dtables(yt_client_fn, st_fn):

    # test data and mock objects
    st_fn.return_value = json.loads(resource.find('/data/st_dep_info_response.json'))
    tablet_cell_ids1 = [
        "1c6a7-3f81b5-3fe02bc-72d527c3",
        "1c6a7-3f81b1-3fe02bc-adb96a79"
    ]
    tablet_cell_ids2 = [
        "1c6a7-3f81c0-3fe02bc-36e9c988",
        "1c6a7-3f81c1-3fe02bc-1895319e"
    ]
    yt_client_fn.side_effect = make_yt_mock_response({
        '//sys/groups': make_groups_mock_response({
            'test_group1': ['person1']
        }),
        '//sys/tablet_cells': make_tablet_cells_response({
            tablet_cell_ids1[0]: {
                'disk_space': 100
            },
            tablet_cell_ids1[1]: {
                'disk_space': 400
            },
            tablet_cell_ids2[0]: {
                'disk_space': 300
            }
        }),
        '//sys/accounts': make_accounts_response({
            'test_account': {
                'responsibles': [
                    'test_user'
                ]
            }
        }),
        '//sys/tablet_cell_bundles': make_tablet_cell_bundles_response({
            'bundle1': {
                'tablet_cell_ids': tablet_cell_ids1,
                'changelog_account': 'test_account',
            },
            'bundle2': {
                'tablet_cell_ids': tablet_cell_ids2,
                'changelog_account': 'test_account'
            }
        }),
    })

    # test config
    output_path = '//home/market/production/infra/resources'
    run_yt_dtables(user_login='grishakov', yt_proxy=os.environ['YT_PROXY'], yt_path=output_path, st_token='the st token', target_clusters=['test_cluster'])

    # test checks
    yt_client = YtClient(os.environ["YT_PROXY"])
    output_table_path = os.path.join(os.path.join(output_path, 'yt_dtables'), 'recent')
    assert yt_client.exists(output_table_path)
    data = [json.loads(line) for line in yt.read_table(output_table_path, raw=True, format='json').read().split('\n') if line]
    assert len(data) == 2
    data_dict = dict((r['tablet_cell_bundle'], r) for r in data)
    assert data_dict['bundle1']['disk_space'] == 500
    assert data_dict['bundle2']['disk_space'] == 300

