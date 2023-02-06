#!/usr/bin/env python
# coding: utf-8
import os
import pytest

from hamcrest import assert_that, not_

from market.idx.offers.yatf.matchers.offers_processor.env_matchers import HasFeedlogRecord
from market.idx.generation.yatf.utils.feedlog import write_feedlogs
from market.idx.yatf.resources.feedlog import FeedLog
from market.proto.common.common_pb2 import EColor
from market.proto.indexer.FeedLog_pb2 import (
    Feed,
    Status,
)

import yatest.common


class FeedlogMergerEnv(object):
    def __init__(self, feedlog_path):
        self.feedlog = FeedLog(feedlog_path).load()


@pytest.fixture()
def white_feeds():
    feed1 = Feed()
    feed1.feed_id = 1
    feed1.honest_discount_offers_count = 1
    feed1.indexation.statistics.total_offers = 2
    feed1.indexation.statistics.valid_offers = 1
    feed1.Color = EColor.Value('C_WHITE')
    feed1.indexed_status = 'ok'

    feed2 = Feed()
    feed2.feed_id = 2
    feed2.honest_discount_offers_count = 2
    # no total_offers field
    feed2.indexation.statistics.valid_offers = 2
    feed2.Color = EColor.Value('C_WHITE')
    feed2.indexed_status = 'ok'

    feed3 = Feed()
    feed3.feed_id = 3
    feed3.honest_discount_offers_count = 3
    feed3.indexation.statistics.total_offers = 3
    feed3.indexation.statistics.valid_offers = 3
    feed3.Color = EColor.Value('C_BLUE')
    feed3.indexed_status = 'ok'

    feed4 = Feed()
    feed4.feed_id = 4
    feed4.honest_discount_offers_count = 4
    feed4.indexation.statistics.total_offers = 16
    feed4.indexation.statistics.valid_offers = 4
    feed4.Color = EColor.Value('C_BLUE')
    feed4.indexed_status = 'ok'

    return [feed1, feed2, feed3, feed4]


@pytest.fixture()
def blue_feeds():
    feed1 = Feed()
    feed1.feed_id = 1
    feed1.honest_discount_offers_count = 10
    # no indexation field
    feed1.Color = EColor.Value('C_WHITE')
    feed1.indexed_status = 'ok'

    feed2 = Feed()
    feed2.feed_id = 20
    feed2.honest_discount_offers_count = 20
    feed2.indexation.statistics.total_offers = 8
    feed2.indexation.statistics.valid_offers = 6
    feed2.Color = EColor.Value('C_WHITE')
    feed2.indexed_status = 'ok'

    feed3 = Feed()
    feed3.feed_id = 30
    feed3.honest_discount_offers_count = 30
    feed3.indexation.statistics.total_offers = 9
    feed3.indexation.statistics.valid_offers = 9
    feed3.Color = EColor.Value('C_BLUE')
    feed3.indexed_status = 'ok'

    feed4 = Feed()
    feed4.feed_id = 4
    feed4.honest_discount_offers_count = 40
    feed4.indexation.statistics.total_offers = 16
    feed4.indexation.statistics.valid_offers = 8
    feed4.Color = EColor.Value('C_BLUE')
    feed4.indexed_status = 'ok'

    feed5 = Feed()
    feed5.feed_id = 5
    feed5.Color = EColor.Value('C_BLUE')
    # no indexed_status field

    return [feed1, feed2, feed3, feed4, feed5]


@pytest.fixture()
def smart_index_feeds():
    feed1 = Feed()
    feed1.feed_id = 101
    feed1.indexation.statistics.total_offers = 2
    feed1.indexation.statistics.valid_offers = 0
    feed1.indexation.statistics.error_offers = 2
    feed1.indexation.statistics.smart_index_offers = 2
    feed1.Color = EColor.Value('C_WHITE')
    feed1.indexed_status = 'crit'

    feed2 = Feed()
    feed2.feed_id = 102
    feed2.indexation.statistics.total_offers = 2
    feed2.indexation.statistics.valid_offers = 0
    feed2.indexation.statistics.error_offers = 2
    feed2.indexation.statistics.smart_index_offers = 1
    feed2.Color = EColor.Value('C_WHITE')
    feed2.indexed_status = 'crit'

    feed3 = Feed()
    feed3.feed_id = 103
    feed3.indexation.statistics.total_offers = 3
    feed3.indexation.statistics.valid_offers = 1
    feed3.indexation.statistics.error_offers = 2
    feed3.indexation.statistics.smart_index_offers = 1
    feed3.Color = EColor.Value('C_WHITE')
    feed3.indexed_status = 'error'

    return [feed1, feed2, feed3]


@pytest.fixture()
def white_feed_log(tmpdir, white_feeds):
    pbsn_filepath = os.path.join(str(tmpdir), 'white.feedlog.pbuf.sn')
    write_feedlogs(pbsn_filepath, white_feeds)
    yield pbsn_filepath


@pytest.fixture()
def blue_feed_log(tmpdir, blue_feeds):
    pbsn_filepath = os.path.join(str(tmpdir), 'blue.feedlog.pbuf.sn')
    write_feedlogs(pbsn_filepath, blue_feeds)
    yield pbsn_filepath


@pytest.fixture()
def smart_index_feed_log(tmpdir, smart_index_feeds):
    pbsn_filepath = os.path.join(str(tmpdir), 'smart_index.feedlog.pbuf.sn')
    write_feedlogs(pbsn_filepath, smart_index_feeds)
    yield pbsn_filepath


@pytest.fixture()
def output_feed_log(tmpdir):
    pbsn_filepath = os.path.join(str(tmpdir), 'output.feedlog.pbuf.sn')
    if os.path.exists(pbsn_filepath):
        os.unlink(pbsn_filepath)
    yield pbsn_filepath


@pytest.fixture()
def workflow(white_feed_log, blue_feed_log, output_feed_log):
    yatest.common.canonical_execute([
        yatest.common.binary_path("market/idx/generation/feedlog-merger/feedlog-merger"),
        '--output', output_feed_log,
        white_feed_log,
        blue_feed_log,
    ])
    yield FeedlogMergerEnv(output_feed_log)


@pytest.mark.parametrize('feed_id, color, counter, statistics, status', [
    (1,  'C_WHITE', 11, {'total_offers': 2,  'valid_offers': 1,  'error_offers': 1}, 'ERROR'),
    (2,  'C_WHITE', 2,  {'valid_offers': 2}, 'UNKNOWN'),
    (3,  'C_BLUE',  3,  {'total_offers': 3,  'valid_offers': 3,  'error_offers': 0}, 'OK'),
    (4,  'C_BLUE',  44, {'total_offers': 16, 'valid_offers': 12, 'error_offers': 4}, 'ERROR'),
    (20, 'C_WHITE', 20, {'total_offers': 8,  'valid_offers': 6,  'error_offers': 2}, 'ERROR'),
    (30, 'C_BLUE',  30, {'total_offers': 9,  'valid_offers': 9,  'error_offers': 0}, 'OK'),
])
def test_merge(workflow, feed_id, color, counter, statistics, status):
    '''
    тест проверяет, что мы сохранили обратную совместимость
    признак: суммирование произошло
    '''
    assert_that(
        workflow,
        HasFeedlogRecord({
            'feed_id': feed_id,
            'Color': EColor.Value(color),
            'honest_discount_offers_count': counter,
            'indexation': {
                'statistics': statistics,
                'status': Status.Value(status),
            },
        }),
        u'feed_id {} смержился корректно'.format(feed_id)
    )


@pytest.fixture()
def colored_workflow(white_feed_log, blue_feed_log, output_feed_log):
    yatest.common.canonical_execute([
        yatest.common.binary_path("market/idx/generation/feedlog-merger/feedlog-merger"),
        '--colored',
        '--output', output_feed_log,
        '--white', white_feed_log,
        '--blue', blue_feed_log,
    ])
    yield FeedlogMergerEnv(output_feed_log)


@pytest.mark.parametrize('feed_id, color, counter, statistics, status', [
    (1,  'C_WHITE', 1,  {'total_offers': 2,  'valid_offers': 1, 'error_offers': 1}, 'ERROR'),
    (2,  'C_WHITE', 2,  {'valid_offers': 2}, 'UNKNOWN'),
    (4,  'C_BLUE',  40, {'total_offers': 16, 'valid_offers': 8, 'error_offers': 8}, 'ERROR'),
    (30, 'C_BLUE',  30, {'total_offers': 9,  'valid_offers': 9, 'error_offers': 0}, 'OK'),
])
def test_colored_merge(colored_workflow, feed_id, color, counter, statistics, status):
    '''
    тест проверяет что в итоговый фидлог попали записи,
    цвет которых соответсвует опции переданной в мержер
    признак: суммирование не произошло
    '''
    assert_that(
        colored_workflow,
        HasFeedlogRecord({
            'feed_id': feed_id,
            'Color': EColor.Value(color),
            'honest_discount_offers_count': counter,
            'indexation': {
                'statistics': statistics,
                'status': Status.Value(status),
            },
        }),
        u'feed_id {} смержился корректно'.format(feed_id)
    )


@pytest.mark.parametrize('feed_id', [
    3,  # синий только в белом
    20,  # белый только в синем
])
def test_colored_merge_negative(colored_workflow, feed_id):
    '''
    тест проверяет что в итоговый фидлог НЕ попали записи,
    цвет которых НЕ соответсвует, опции переданной в мержер
    '''
    assert_that(
        colored_workflow,
        not_(HasFeedlogRecord({
            'feed_id': feed_id,
        })),
        u'feed_id {} смержился корректно'.format(feed_id)
    )


def test_no_indexed_status_negative(colored_workflow):
    '''
    Проверяем, что запись без indexed_status не берется в фидлог
    '''
    assert_that(
        colored_workflow,
        not_(HasFeedlogRecord({
            'feed_id': 5,
        })),
        u'feed_id {} смержился корректно'.format(5)
    )


@pytest.fixture()
def smart_index_workflow(smart_index_feed_log, output_feed_log):
    yatest.common.canonical_execute([
        yatest.common.binary_path("market/idx/generation/feedlog-merger/feedlog-merger"),
        '--output', output_feed_log,
        smart_index_feed_log,
    ])
    yield FeedlogMergerEnv(output_feed_log)


def test_smart_index_book_keeping(smart_index_workflow):
    """ Проверяет, что счетчики офферов учитывают офферы, скрытые умным индексом """
    assert_that(
        smart_index_workflow,
        HasFeedlogRecord({
            'feed_id': 101,
            'indexation': {
                'statistics': {
                    'error_offers': 0,
                    'valid_offers': 2,
                    'total_offers': 2,
                    'smart_index_offers': 2
                },
                'status': Status.Value('OK'),  # promotion from crit
            },
        })
    )

    assert_that(
        smart_index_workflow,
        HasFeedlogRecord({
            'feed_id': 102,
            'indexation': {
                'statistics': {
                    'error_offers': 1,
                    'valid_offers': 1,
                    'total_offers': 2,
                    'smart_index_offers': 1
                },
                'status': Status.Value('ERROR'),  # promotion from crit
            },
        })
    )

    assert_that(
        smart_index_workflow,
        HasFeedlogRecord({
            'feed_id': 103,
            'indexation': {
                'statistics': {
                    'error_offers': 1,
                    'valid_offers': 2,
                    'total_offers': 3,
                    'smart_index_offers': 1
                },
                'status': Status.Value('ERROR'),  # keeping error
            },
        })
    )
