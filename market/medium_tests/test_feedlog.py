# -*- coding: utf-8 -*-

import os
import pytest

from market.pylibrary.mindexerlib import util
from market.idx.generation.yatf.utils.feedlog import write_feedlogs
from market.proto.common.common_pb2 import EColor
from market.proto.indexer.FeedLog_pb2 import Feed


@pytest.fixture()
def mindexer_clt(mindexer_clt, reusable_mysql):
    """Добавляем в уже созданные для mindexer_clt пару поколений
    белое full 20180101_0101
    белое diff 20180101_0131 собранный к 20180101_0101
    синее 20180101_0100
    """
    mindexer_clt.add_generation_to_super('20180101_0101')
    mindexer_clt.add_generation_to_super('20180101_0131', base_name='20180101_0101', diff=True)
    mindexer_clt.add_generation_to_super('20180101_0100', blue=True)
    return mindexer_clt


@pytest.fixture()
def white_feeds():
    feed1 = Feed()
    feed1.feed_id = 1
    feed1.honest_discount_offers_count = 1
    feed1.Color = EColor.Value('C_WHITE')

    feed2 = Feed()
    feed2.feed_id = 2
    feed2.honest_discount_offers_count = 2
    feed2.Color = EColor.Value('C_WHITE')

    feed3 = Feed()
    feed3.feed_id = 3
    feed3.honest_discount_offers_count = 3
    feed3.Color = EColor.Value('C_BLUE')

    feed4 = Feed()
    feed4.feed_id = 4
    feed4.honest_discount_offers_count = 4
    feed4.Color = EColor.Value('C_BLUE')

    return [feed1, feed2, feed3, feed4]


@pytest.fixture()
def blue_feeds():
    feed1 = Feed()
    feed1.feed_id = 1
    feed1.honest_discount_offers_count = 10
    feed1.Color = EColor.Value('C_WHITE')

    feed2 = Feed()
    feed2.feed_id = 20
    feed2.honest_discount_offers_count = 20
    feed2.Color = EColor.Value('C_WHITE')

    feed3 = Feed()
    feed3.feed_id = 30
    feed3.honest_discount_offers_count = 30
    feed3.Color = EColor.Value('C_BLUE')

    feed4 = Feed()
    feed4.feed_id = 4
    feed4.honest_discount_offers_count = 40
    feed4.Color = EColor.Value('C_BLUE')

    return [feed1, feed2, feed3, feed4]


@pytest.fixture()
def mixed_feeds():
    feed1 = Feed()
    feed1.feed_id = 1
    feed1.honest_discount_offers_count = 10
    feed1.indexed_status = 'ok'
    feed1.Color = EColor.Value('C_RED')

    feed2 = Feed()
    feed2.feed_id = 20
    feed2.honest_discount_offers_count = 20
    feed2.indexed_status = 'ok'
    feed2.Color = EColor.Value('C_WHITE')

    feed3 = Feed()
    feed3.feed_id = 30
    feed3.honest_discount_offers_count = 30
    feed3.indexed_status = 'ok'
    feed3.Color = EColor.Value('C_BLUE')

    feed4 = Feed()
    feed4.feed_id = 4
    feed4.honest_discount_offers_count = 40
    feed4.indexed_status = 'ok'
    feed4.Color = EColor.Value('C_RED')

    return [feed1, feed2, feed3, feed4]


@pytest.fixture()
def white_feed_log(mindexer_clt, white_feeds):
    pbsn_filepath = mindexer_clt.path('indexer/market/20180101_0101/input/feedlog.main.pbuf.sn')
    util.makedirs(os.path.dirname(pbsn_filepath))
    write_feedlogs(pbsn_filepath, white_feeds)
    return pbsn_filepath


@pytest.fixture()
def diff_feed_log(mindexer_clt, white_feeds):
    pbsn_filepath = mindexer_clt.path('indexer/market/diff/20180101_0130/input/feedlog.main.pbuf.sn')
    util.makedirs(os.path.dirname(pbsn_filepath))
    write_feedlogs(pbsn_filepath, white_feeds)
    return pbsn_filepath


@pytest.fixture()
def blue_feed_log(mindexer_clt, blue_feeds):
    pbsn_filepath = mindexer_clt.path('indexer/market/blue/20180101_0100/input/feedlog.main.pbuf.sn')
    util.makedirs(os.path.dirname(pbsn_filepath))
    write_feedlogs(pbsn_filepath, blue_feeds)
    return pbsn_filepath


@pytest.fixture()
def mixed_feed_log(mindexer_clt, mixed_feeds):
    pbsn_filepath = mindexer_clt.path('indexer/market/20180101_0101/input/feedlog.main.pbuf.sn')
    util.makedirs(os.path.dirname(pbsn_filepath))
    write_feedlogs(pbsn_filepath, mixed_feeds)
    return pbsn_filepath


def test_feedlog_merge_and_calc_stats_called_from_white(mindexer_clt, white_feed_log, blue_feed_log):
    """Проверяем, что мердж фидлогов отрабатывает без ошибок и создает нужный файл.
    При запуске из белого демона.
    """
    mindexer_clt.execute('feedlog_merge_and_calc_stats', '--generation', '20180101_0101')
    assert os.path.exists(mindexer_clt.path('indexer/market/20180101_0101/input/feedlog.mbi.result.pbuf.sn'))


def test_feedlog_merge_and_calc_stats_called_from_blue(mindexer_clt, white_feed_log, blue_feed_log):
    """Проверяем, что мердж фидлогов отрабатывает без ошибок и создает нужный файл.
    При запуске из синего демона.
    """
    mindexer_clt.execute('feedlog_merge_and_calc_stats', '--generation', '20180101_0100', blue=True)
    assert os.path.exists(mindexer_clt.path('indexer/market/blue/20180101_0100/input/feedlog.mbi.result.pbuf.sn'))


def test_feedlog_merge_and_calc_stats_called_from_diff(mindexer_clt, diff_feed_log, blue_feed_log):
    """Проверяем, что мердж фидлогов отрабатывает без ошибок и создает нужный файл.
    При запуске из белого diff демона.
    """
    mindexer_clt.execute('feedlog_merge_and_calc_stats', '--generation', '20180101_0130', diff=True)
    assert os.path.exists(mindexer_clt.path('indexer/market/diff/20180101_0130/input/feedlog.mbi.result.pbuf.sn'))


def test_feedlog_merge_and_calc_stats_called_mixed_and_isolated(mindexer_clt, mixed_feed_log):
    """Проверяем, что при наличие флага о необходимости создания изолированного фидлога, он создается (ровно как и миксованый)
    MARKETINDEXER-28584
    """
    mindexer_clt.make_local_config({
        ('feedlog', 'red_isolated'): 'true',
    })
    mindexer_clt.execute('feedlog_merge_and_calc_stats', '--generation', '20180101_0101')
    assert os.path.exists(mindexer_clt.path('indexer/market/20180101_0101/input/feedlog.mbi.result.pbuf.sn'))
    assert os.path.exists(mindexer_clt.path('indexer/market/20180101_0101/input/red.feedlog.mbi.result.pbuf.sn'))
