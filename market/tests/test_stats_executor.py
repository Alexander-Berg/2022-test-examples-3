#  -*- coding: utf-8 -*-

from market.idx.pylibrary.mindexer_core.stats_executor.stats_executor import StatsExecutor, calculate_stats

import mock
import pytest
import py
from yt.wrapper.ypath import ypath_join


@pytest.fixture()
def config():
    class Config(object):
        def __init__(self):
            self.working_dir = 'indexer/market'
            self.cron_stats_dir = 'indexer/market/cron_stats'
            self.yt_proxy = 'hahn'
            self.yt_idr_factor = 1
            self.run_dir = 'run'
            self.yt_priority_pool = None
            self.yt_pool_batch = None
            # other
            self.yt_tokenpath = 'mock_data'
            self.yt_stats_calc_dir = 'mock_data'
            self.rsync_market_indexer = 'mock_data'
            self.rsync_postfix = 'mock_data'
            self.statscalc_bin = 'mock_data'
            self.statsconvert_bin = 'mock_data'
            self.is_mir = False
            self.for_blue_shard = False
            self.use_uploaded_files = True
            self.yt_home_dir = '//home/mock'
            self.yt_stats_calc_pool = None
            self.yt_separated_stats_calc_dir_for_half = False
            self.stats_calc_thread_count = 25
            self.genlog_output_yt_dir='genlog'
            self.yt_mi3_dir='//tmp/mi3'
            self.yt_mi3_type=self.get_mi3_type()
            self.separate_cpc = False
            self.first_cpc = None
            self.generation = '20220606_1814'

        def get_mi3_type(self):
            return 'mock'

        def get_generation_yt_dir(self, generation):
            return ypath_join(self.yt_mi3_dir, self.yt_mi3_type, self.generation)

    config = Config()
    py.path.local(config.run_dir).ensure(dir=True)
    py.path.local(config.working_dir).ensure(dir=True)
    return Config()


@pytest.fixture(autouse=True)
def dummy_statscalc(monkeypatch):
    '''
    Do not run stats or copy genlogs
    '''
    mock_copy = mock.Mock()
    monkeypatch.setattr('market.idx.pylibrary.mindexer_core.stats_executor.stats_executor.StatsExecutor.copy_genlogs', mock_copy)
    mock_do = mock.Mock()
    monkeypatch.setattr('market.idx.stats.statscalc.statscalc.stats_base.StatisticsRunnerBase.do', mock_do)
    return mock_copy, mock_do


def test_calc_stats_yt(config, dummy_statscalc):
    calc_stats_yt = StatsExecutor('dummy', config, None, None)
    calc_stats_yt.calculate()

    mock_copy, mock_do = dummy_statscalc
    mock_do.assert_called_once_with()


def test_calculate_stats_yt_success(config):
    calculate_stats('dummy', config, None, None)


def test_calculate_stats_all_fail(config, monkeypatch):
    class NopeException(Exception):
        pass

    class StatsExecutorFail(StatsExecutor):
        def calculate(self):
            raise NopeException('Nope')

    mock_do = mock.Mock()
    monkeypatch.setattr('market.idx.stats.statscalc.statscalc.stats_base.StatisticsRunnerBase.do', mock_do)
    monkeypatch.setattr('market.idx.pylibrary.mindexer_core.stats_executor.stats_executor.StatsExecutor', StatsExecutorFail)

    with pytest.raises(NopeException):
        calculate_stats('dummy', config, None, None)


def test_calculate_stats_twice(config):
    calculate_stats('dummy', config, None, None)
    calculate_stats('dummy', config, None, None)
