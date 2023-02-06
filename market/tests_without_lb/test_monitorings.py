# coding: utf-8

import pytest
import freezegun
from hamcrest import assert_that, all_of, matches_regexp, starts_with, equal_to
from market.idx.yatf.resources.yt_replicated_table_resource import YtReplicatedTable
from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.pylibrary.datacamp.schema import service_offers_attributes, basic_offers_attributes
from datetime import datetime, timedelta
from six.moves.urllib.parse import urlencode
from market.idx.yatf.resources.shops_dat import ShopsDat
from market.pylibrary.dyt.utils import make_ytclient


from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock
from market.idx.datacamp.routines.yatf.test_env import HttpRoutinesTestEnv
import yt.wrapper as yt
import yt.yson as yson


def change_tablet_count_(yt_client, path, pivot_keys):
    yt_client.unmount_table(path=path, sync=True)
    yt_client.reshard_table(path, pivot_keys=pivot_keys)
    yt_client.mount_table(path=path, sync=True)


def update_with_keys_(attrs):
    attrs.update({'pivot_keys': [[], [yson.YsonUint64(1), '1']]})
    return attrs


@pytest.fixture(scope='module')
def basic_offers_table(config, yt_server):
    return YtReplicatedTable(
        yt_server,
        path=config.yt_basic_offers_tablepath,
        sync_path=config.yt_basic_offers_tablepath + '_replica',
        attributes=update_with_keys_(basic_offers_attributes()),
    )


@pytest.fixture(scope='module')
def service_offers_table(config, yt_server):
    return YtReplicatedTable(
        yt_server,
        path=config.yt_service_offers_tablepath,
        sync_path=config.yt_service_offers_tablepath + '_replica',
        attributes=update_with_keys_(service_offers_attributes()),
    )


@pytest.fixture(scope='module')
def actual_service_offers_table(config, yt_server):
    return YtReplicatedTable(
        yt_server,
        path=config.yt_actual_service_offers_tablepath,
        sync_path=config.yt_actual_service_offers_tablepath + '_replica',
        attributes=update_with_keys_(service_offers_attributes()),
    )


@pytest.fixture(scope='module')
def verdict_hash_table_path(config):
    return yt.ypath_join(config.yt_verdicts_hash_dir, 'recent')


@pytest.fixture(scope='module')
def verdict_hash_table(yt_server, verdict_hash_table_path):
    return YtTableResource(
        yt_server,
        verdict_hash_table_path,
        attributes=dict(
            dynamic=False,
            external=False,
            schema=[
                dict(name='hash', type='uint32'),
                dict(name='code', type='string'),
            ]
        ),
        data=[
            {'hash': 123, 'code': 'Test.Error.1'},
            {'hash': 455, 'code': 'Test.Error.2'},
            {'hash': 123, 'code': 'Test.Error.3'},
        ])


def check_response(routines_http, params):
    request = '/monitoring?' + urlencode(params)
    response = routines_http.get(request)
    assert_that(response, HasStatus(200))
    return response.data.decode('utf-8')


@pytest.fixture(scope='module')
def shopsdat(config):
    return ShopsDat(filename=config.shopsdat)


@pytest.fixture(scope='session')
def config(yt_server):
    return RoutinesConfigMock(
        yt_server,
        config={
            'general': {'color': 'direct_goods_ads', 'yt_home': '//home/datacamp/united'},
            'yt': {
                'white_out': 'white_out',
                'blue_out': 'blue_out',
                'direct_out': 'direct_out',
                'turbo_out': 'turbo_out',
                'map_reduce_proxies': [yt_server.get_yt_client().config["proxy"]["url"]],
            },
            'monitoring': {
                'tablet_count_threshold': 2,
                'enable_united_out_check_tables_modification': True,
                'enable_check_unique_verdict_hashes': True,
            },
        },
    )


@pytest.fixture()
def make_replica_client_mock(config):
    def do_make_replica_client(_, token):
        return make_ytclient(config.yt_meta_proxy, token)

    yield do_make_replica_client


@pytest.yield_fixture(scope='module')
def routines_http(config, yt_server, shopsdat, basic_offers_table, service_offers_table, actual_service_offers_table, verdict_hash_table):
    resources = {
        'config': config,
        'shopsdat': shopsdat,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'verdict_hash_table': verdict_hash_table,
    }

    yt.create('table', yt.ypath_join(config.yt_white_output_dir, 'recent'), recursive=True)
    yt.create('table', yt.ypath_join(config.yt_turbo_output_dir, 'recent'), recursive=True)
    yt.create('table', yt.ypath_join(config.yt_direct_output_dir, 'recent'), recursive=True)
    yt.create('table', yt.ypath_join(config.yt_blue_output_dir, 'recent'), recursive=True)

    with HttpRoutinesTestEnv(yt_server=yt_server, **resources) as routines_http_env:
        yield routines_http_env


def test_check_united_out_tables_modification_time_backward(
    routines_http,
):  # backward compatibility check, TODO(elnikovss): remove with malek MARKETINDEXER-46109
    params = {'check': 'check_united_out_tables_modification_time'}
    assert_that(check_response(routines_http, params), equal_to('0;OK'))


def test_check_united_out_tables_modification_time_passed(routines_http, yt_server):
    params = {
        'check': 'check_united_out_tables_modification_time',
        'yt_proxy': yt_server.get_yt_client().config["proxy"]["url"],
    }
    assert_that(check_response(routines_http, params), equal_to('0;OK'))


def test_check_united_out_tables_modification_time_failed(routines_http, yt_server):
    with freezegun.freeze_time(time_to_freeze=datetime.now() + timedelta(hours=24)):
        params = {
            'check': 'check_united_out_tables_modification_time',
            'yt_proxy': yt_server.get_yt_client().config["proxy"]["url"],
        }
        assert_that(
            check_response(routines_http, params), starts_with('2;some tables were modified last time long time ago')
        )


def test_check_getter_data_freshness_passed(routines_http):
    params = {'check': 'check_getter_data_freshness'}
    assert_that(check_response(routines_http, params), starts_with('0;OK'))


def test_check_getter_data_freshness_failed(routines_http):
    with freezegun.freeze_time(time_to_freeze=datetime.now() + timedelta(hours=24)):
        params = {'check': 'check_getter_data_freshness'}
        assert_that(check_response(routines_http, params), matches_regexp('2;File.*shops.dat is too old'))


def test_check_tablet_count_passed(routines_http, monkeypatch, make_replica_client_mock, yt_server):
    with monkeypatch.context() as m:
        m.setattr(
            'market.idx.datacamp.routines.lib.monitorings.check_tablet_count.make_replica_client',
            make_replica_client_mock,
        )
        m.setattr(
            'market.idx.datacamp.routines.lib.monitorings.check_tablet_count.check_replica',
            lambda _, __: True,
        )
        params = {'check': 'check_tablet_count', 'replica_proxies': yt_server.get_yt_client().config["proxy"]["url"]}
        assert_that(check_response(routines_http, params), equal_to('0;'))


def test_check_tablet_count_failed(routines_http, monkeypatch, make_replica_client_mock, yt_server, config):
    with monkeypatch.context() as m:
        m.setattr(
            'market.idx.datacamp.routines.lib.monitorings.check_tablet_count.make_replica_client',
            make_replica_client_mock,
        )
        m.setattr(
            'market.idx.datacamp.routines.lib.monitorings.check_tablet_count.check_replica',
            lambda _, __: True,
        )
        params = {'check': 'check_tablet_count', 'replica_proxies': yt_server.get_yt_client().config["proxy"]["url"]}

        yt_client = yt_server.get_yt_client()
        change_tablet_count_(yt_client, config.yt_basic_offers_tablepath, pivot_keys=[[]])

        assert_that(
            check_response(routines_http, params),
            all_of(
                starts_with('2;'),
                matches_regexp('.*low tablet count on meta cluster for table.*'),
            ),
        )


def test_check_unique_verdict_hashes(routines_http, yt_server, verdict_hash_table):
    params = {
        'check': 'check_unique_verdict_hashes',
        'yt_proxy': yt_server.get_yt_client().config["proxy"]["url"],
    }
    assert_that(check_response(routines_http, params), starts_with('2;'))
