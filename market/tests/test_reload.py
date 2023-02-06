# -*- coding: utf-8 -*-

from collections import namedtuple
import pytest
from mock import patch, call

from .common import (  # noqa
    make_filestubs,
    file_with_data,
    backctld_app,
    make_full_generation,
    make_full_index,
    make_qbid_mds,
    SyncThread,
)


@pytest.yield_fixture(scope='class')
def marketsearch_mocks():
    class Mocks(object):
        def __init__(self):
            self._servants_list_patcher = patch('pyb.report_config.servant_list',
                                                return_value=['market-report'])
            self.servants_list = None
            self._check_call_patcher = patch('market.pylibrary.mi_util.util.watching_check_call',
                                             return_value=0)
            self.check_call = None
            self._system_call_patcher = patch('market.pylibrary.mi_util.util.watching_system_ex',
                                              return_value=0)
            self.system_call = None
            self._system_ex_patcher = patch('market.pylibrary.mi_util.util.system_ex',
                                            return_value=(0, 'stdout', 'stderr'))
            self.system_ex = None
            Responce = namedtuple('Responce', ['status_code', 'text'])
            self._requests_get_patcher = patch(
                'requests.get',
                return_value=Responce(200, '<admin-action>ok</admin-action>')
            )
            self.get = None
            DummyServantConfig = namedtuple('ServantConfig', ['market_report', 'server', 'collections'])
            self.dummy_servant_config = DummyServantConfig(
                market_report={},
                server={'Port': '17051'},
                collections={
                    'basesearch16-0': {},
                    'basesearch16-8': {},
                    'basesearch-diff16-0': {},
                }
            )
            self._servant_config_patcher = patch(
                'pyb.report_config.read_servant_config',
                return_value=self.dummy_servant_config
            )
            self._qpipe_async_thread_patcher = patch(
                'market.pylibrary.hammer.AsyncThread',
                SyncThread
            )
            self._reload_lock_path_patcher = patch(
                'pyb.plugin.marketsearch.MarketSearch.get_reload_lock_path',
                return_value='reload.lock'
            )

        def start(self):
            self.servants_list = self._servants_list_patcher.start()
            self.check_call = self._check_call_patcher.start()
            self.system_call = self._system_call_patcher.start()
            self.system_ex = self._system_ex_patcher.start()
            self.get = self._requests_get_patcher.start()
            self.servant_config = self._servant_config_patcher.start()
            self._qpipe_async_thread_patcher.start()
            self._reload_lock_path_patcher.start()

        def stop(self):
            self._servants_list_patcher.stop()
            self._check_call_patcher.stop()
            self._system_call_patcher.stop()
            self._system_ex_patcher.stop()
            self._requests_get_patcher.stop()
            self._servant_config_patcher.stop()
            self._qpipe_async_thread_patcher.stop()
            self._reload_lock_path_patcher.stop()

    mocks = Mocks()
    try:
        mocks.start()
        yield mocks
    finally:
        mocks.stop()


class TestReloadNoCurrentGeneration(object):
    @pytest.fixture(scope='class')
    def reload(self, backctld_app, tmpdir_factory, marketsearch_mocks):  # noqa
        '''
        Релоад в случае отсутствия current.generation.
        '''
        root_dir = tmpdir_factory.mktemp('TestReloadNoCurrentGeneration')
        backctld_app.set_root_dir(root_dir)
        # Не делаем make_full_index - значит, нет current.generation.
        make_full_generation(root_dir, '20170320_1100')
        backctld_app.run('marketsearch3 reload 20170320_1100')
        Fixture = namedtuple('Fixture', ['app', 'root_dir'])
        return Fixture(backctld_app, root_dir)

    def test_reload_ok(self, reload):
        # reload finished correctly
        assert reload.app.run('marketsearch3 check') == 'ok'

    @pytest.mark.parametrize('path, value', [
        ('search/index/mmap/content', '20170320_1100'),
        ('search/report-data/content', '20170320_1100'),
        ('search/report-data/backends/content', '20170320_1100'),
        ('search/marketsearch/current.generation', '20170320_1100'),
    ] + [
        ('search/index/part-{}/content'.format(n), '20170320_1100') for n in (0, 1)
    ])
    def test_updated(self, reload, path, value):
        assert reload.root_dir.join(path).read() == value


class TestReload(object):
    @pytest.fixture(scope='class')
    def reload(self, backctld_app, tmpdir_factory, marketsearch_mocks):  # noqa
        '''
        Релоад поколения(сервис marketsearch3) с заменой full поколения на новое
        '''
        root_dir = tmpdir_factory.mktemp('TestReloadBoth')
        backctld_app.set_root_dir(root_dir)
        make_full_index(root_dir, '20170320_1000')
        make_full_generation(root_dir, '20170320_1100')
        marketsearch_mocks.system_call.reset_mock()
        marketsearch_mocks.check_call.reset_mock()
        backctld_app.run('marketsearch3 reload 20170320_1100')
        update_timeout = int(backctld_app._contexts['updater'].config.get2('qpipe', 'qpipe_update_timeout'))
        Fixture = namedtuple('Fixture', ['app', 'root_dir', 'update_timeout'])
        return Fixture(backctld_app, root_dir, update_timeout)

    def test_reload_ok(self, reload):
        # reload finished correctly
        assert reload.app.run('marketsearch3 check') == 'ok'

    @pytest.mark.parametrize('path, value', [
        ('search/index/mmap/content', '20170320_1100'),
        ('search/report-data/content', '20170320_1100'),
        ('search/report-data/backends/content', '20170320_1100'),
        ('search/marketsearch/current.generation', '20170320_1100'),
        ('search/marketsearch/current.generation', '20170320_1100'),
    ] + [
        ('search/index/part-{}/content'.format(n), '20170320_1100') for n in (0, 1)
    ])
    def test_updated(self, reload, path, value):
        assert reload.root_dir.join(path).read() == value

    def test_slb_opened(self, reload, marketsearch_mocks):
        assert marketsearch_mocks.system_call.mock_calls.count(call([
            '/etc/init.d/httpsearch',
            'open-for-load',
            'market-report',
        ])) == 1
