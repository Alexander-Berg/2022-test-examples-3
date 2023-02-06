# -*- coding: utf-8 -*-

import pytest
import mock
from collections import namedtuple
from pyb.plugin.report_starter import ReportStarter
from datetime import datetime, timedelta

from .common import (  # noqa
    backctld_app,
    no_zk_filter,
    make_full_generation,
)


@pytest.yield_fixture(scope='class')
def marketsearch_mocks():
    class Mocks(object):
        def __init__(self):
            self.install_package_mock = mock.patch.object(
                ReportStarter,
                '_ReportStarter__install_package',
                return_value=['ok', 'ok']
            )

            self._system_call_patcher = mock.patch('market.pylibrary.mi_util.util.watching_system_ex',
                                                   return_value=0)
            self.system_call = None
            self._system_ex_patcher = mock.patch('market.pylibrary.mi_util.util.system_ex',
                                                 return_value=(0, 'stdout', 'stderr'))
            self._reload_lock_path_patcher = mock.patch(
                'pyb.plugin.marketsearch.MarketSearch.get_reload_lock_path',
                return_value='reload.lock'
            )

        def start(self):
            self.install_package = self.install_package_mock.start()
            self.system_call = self._system_call_patcher.start()
            self.system_ex = self._system_ex_patcher.start()
            self._reload_lock_path_patcher.start()

        def stop(self):
            self.install_package_mock.stop()
            self._system_call_patcher.stop()
            self._system_ex_patcher.stop()
            self._reload_lock_path_patcher.stop()

    mocks = Mocks()
    try:
        mocks.start()
        yield mocks
    finally:
        mocks.stop()


def create_generation_name(delta_min):
    return (datetime.now() - timedelta(minutes=delta_min)).strftime('%Y%m%d_%H%M')


# нужны сформированными на лету "свежими" датами, иначе starter сочтет поколения старыми и мы не проверим нужное
full_generation_older = create_generation_name(90)

full_generation = create_generation_name(60)


class TestReportStarterNoGen(object):
    """Пробуем поднять репорт без разложенного поколения
    """
    @pytest.fixture(scope='class')
    def no_full_generation(self, backctld_app, no_zk_filter, tmpdir_factory, marketsearch_mocks):  # noqa
        root_dir = tmpdir_factory.mktemp('TestReportStarterNoBaseGen')
        backctld_app.set_root_dir(root_dir)

        backctld_app.run('report_starter start')
        Fixture = namedtuple('Fixture', ['app', 'root_dir'])
        marketsearch_mocks.system_call.reset_mock()

        return Fixture(backctld_app, root_dir)

    def test_start_not_ok(self, no_full_generation):
        assert no_full_generation.app.run('report_starter check') == '! failed: Cannot choose right generation. Give up'


class TestReportStarterOnlyFull(object):
    """На вход - только полное загруженное поколение
    """
    @pytest.fixture(scope='class')
    def only_full(self, backctld_app, no_zk_filter, tmpdir_factory, marketsearch_mocks):  # noqa
        root_dir = tmpdir_factory.mktemp('TestReportStarterOnlyFull')
        backctld_app.set_root_dir(root_dir)

        make_full_generation(root_dir, full_generation, completed=True)

        backctld_app.run('report_starter start')
        Fixture = namedtuple('Fixture', ['app', 'root_dir'])
        marketsearch_mocks.system_call.reset_mock()

        return Fixture(backctld_app, root_dir)

    def test_start_ok_only_full(self, only_full):
        assert only_full.app.run('report_starter check') == 'ok'

    @pytest.mark.parametrize(
        'path, value',
        [('search/index/mmap/content', full_generation),
         ('search/report-data/content', full_generation),
         ('search/report-data/backends/content', full_generation),
         ('search/marketsearch/current.generation', full_generation),
         ] + [('search/index/part-{}/content'.format(n), full_generation) for n in (0, 1)],
        ids=['search_index_mmap_content',
             'search_report-data_content',
             'search_report-data_backends_content',
             'search_marketsearch_current.generation',
             ] + [('search_index_part{}_content'.format(n)) for n in (0, 1)]
    )
    def test_updated(self, only_full, path, value):
        assert only_full.root_dir.join(path).read() == value
