# -*- coding: utf-8 -*-

from time import time
from mock import Mock, create_autospec, call, patch
import pytest

from market.pylibrary.s3.s3.s3_api import Client as S3Client
from market.idx.marketindexer.marketindexer.feed_for_freshness import (
    make_feed,
    make_blue_feed,
    make_offers,
    upload_feed,
)
from market.idx.marketindexer.marketindexer.freshness_stats import ResultProcessor, make_url


def test_make_feed():
    '''
    По большей части проверяем, что функция не падает и генерит что то.
    '''
    timestamp = int(time())
    feed_str = make_feed(timestamp)
    assert str(timestamp) in feed_str


def test_make_blue_feed():
    '''
    По большей части проверяем, что функция не падает и генерит что то.
    '''
    timestamp = int(time())
    feed_str = make_blue_feed(timestamp)
    assert str(timestamp) in feed_str


@patch('time.sleep')
def test_upload_feed_retry(sleep_mock):
    mock_config = Mock()
    mock_config.market_idx_public_bucket.return_value = 'market-idx-pub'
    mock_client = create_autospec(S3Client)
    mock_client.write.side_effect = [RuntimeError, None]
    feed_str = 'some cool feed'
    feed_filename = 'some_feed_file_name.xml'
    upload_feed(feed_str, feed_filename, mock_config, client=mock_client)
    write_call = call(mock_config.market_idx_public_bucket, feed_filename, feed_str)
    assert mock_client.write.mock_calls.count(write_call) == 2


def test_make_url():
    base_url = 'http://url.net'
    feed_id = 100
    url = make_url(base_url, feed_id, [1, 2])
    assert url.startswith(base_url) and \
           url.endswith('&feed_shoffer_id={}-1&feed_shoffer_id={}-2'.format(feed_id, feed_id))


class ResultProcessorForTests(ResultProcessor):
    def __init__(self, now):
        super(ResultProcessorForTests, self).__init__()
        self._diff_graph = Mock()
        self._price_graph = Mock()
        self._now = now


FEED_TIMESTAMP = 80000
NOW = 90000


@pytest.fixture(scope='module')
def process_results():
    results = [{
        'shop': {
            'feed': {
                'id': '200303249',
                'offerId': offer['id'],
            }
        },
        'prices': {
            'value': str(offer['price'])
        },
        'description': offer['description']
    } for offer in make_offers(FEED_TIMESTAMP)]
    p = ResultProcessorForTests(NOW)
    for result in results:
        p.process(result)
    p.send_stats()
    return p


def test_descr_freshness(process_results):
    expected = (NOW - FEED_TIMESTAMP) / 60
    process_results._diff_graph.send_metric.assert_any_call(
        'descr', expected)


def test_price_freshness(process_results):
    expected = (NOW - FEED_TIMESTAMP) / 60
    process_results._price_graph.send_metric.assert_any_call(
        'Delta', expected)


def test_added_offers_freshness(process_results):
    expected = NOW - max(int(FEED_TIMESTAMP / m) * m
                         for m in range(60, 61 * 60, 60))
    process_results._diff_graph.send_metric.assert_any_call(
        'added_offers_freshness', expected)


def test_removed_offers_freshness(process_results):
    expected = NOW - min(int(FEED_TIMESTAMP / m) * m + m
                         for m in range(60, 61 * 60, 60))
    process_results._price_graph.send_metric.assert_any_call(
        'removed_offers_freshness', expected)
