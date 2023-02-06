# -*- coding: utf-8 -*-

import errno
import os

import pytest
import yatest

import market.idx.marketindexer.miconfig as miconfig
from market.idx.pylibrary.mindexer_core.qbid.bids_service import BIDS_TYPE_OFFER
from market.idx.pylibrary.mindexer_core.qbid.upload_service import (
    META_NAME,
    enqueue_auction_result,
    make_ar_queue,
    upload_auction_result,
)
from market.pylibrary.mindexerlib import util


class MockLog():
    def __init__(self):
        self.called_info = False
        self.called_error = False

    def info(self, *args, **kwargs):
        self.called_info = True
        util.get_logger(self).info(*args, **kwargs)

    def error(self, *args, **kwargs):
        self.called_error = True
        util.get_logger(self).error(*args, **kwargs)


def list_queue_items(queue):
    return {
        'stage': queue.list_staged_items(),
        'queue': queue.list_queued_items(),
        'unstage': queue.list_unstaged_items(),
    }


@pytest.fixture()
def config():
    queue_path = yatest.common.test_output_path('ar-queue')
    try:
        os.makedirs(queue_path)
    except OSError as exc:
        if exc.errno != errno.EEXIST:
            raise

    class Config(object):
        def __init__(self):
            self.mbi_queue_path = queue_path
            self.log_dir = os.path.dirname(queue_path)
            self.mbi_queue_enabled = True
            self.mbi_failure_interval = 0
            self.mbi_yt_enabled = False
            self.exchange_service_url = 'http://localhost:9999/'  # doesn't matter
            self.auction_result_upload_mode = miconfig.UPLOAD_MODE_MOCK
            self.yt_proxy = 'hahn'

    return Config()


def enqueue_test_data(queue, config):
    source_path = yatest.common.test_output_path('source.pbuf.sn')
    open(source_path, 'w').close()

    enqueue_auction_result(
        source_path=source_path,
        bids_type=BIDS_TYPE_OFFER,
        mbi_exchange_id=123,
        mbi_yt_path='//home/production/market/out/applied_bids/20170308_0000',
        pub_date=util.now(),
        generation='20170308_0000',
        delta_generation=None,
        queue=queue,
        config=config,
    )


@pytest.fixture()
def queue(config):
    return make_ar_queue(config)


@pytest.fixture()
def log():
    return MockLog()


def test_good_items(config, queue, log):
    """Tests that good items are generated and pumped correctly.
    """
    enqueue_test_data(queue, config)
    enqueue_test_data(queue, config)

    upload_auction_result(config=config, queue=queue, log=log)
    assert not log.called_error, 'Uploader must have no errors'

    expected_queue_items = {
        'stage': [],
        'queue': [],
        'unstage': [],
    }
    assert list_queue_items(queue) == expected_queue_items


def test_unstaged_item(config, queue, log):
    """Tests that already unstaged good items are also pumped.
    """
    enqueue_test_data(queue, config)
    enqueue_test_data(queue, config)

    queue.dequeue()

    upload_auction_result(config=config, queue=queue, log=log)
    assert not log.called_error, 'Uploader must have no errors'

    expected_queue_items = {
        'stage': [],
        'queue': [],
        'unstage': [],
    }
    assert list_queue_items(queue) == expected_queue_items


def test_accessible(config, queue, log):
    """Tests that enqueued items can be inspected by anyone.
    """
    enqueue_test_data(queue, config)

    item_path = queue.list_queued_items(full_paths=True)[0]
    assert \
        len(os.listdir(item_path)) > 0, \
        'Item must have files inside'

    for name in os.listdir(item_path):
        path = os.path.join(item_path, name)
        assert \
            (os.stat(path).st_mode & 0o444) == 0o444, \
            'Item files must be readable by anyone'


def test_empty(config, queue, log):
    """Tests that empty queue doesn't hang the process or raise.
    """
    upload_auction_result(config=config, queue=queue, log=log)
    assert not log.called_error, 'Uploader must have no errors'

    expected_queue_items = {
        'stage': [],
        'queue': [],
        'unstage': [],
    }
    assert list_queue_items(queue) == expected_queue_items


def test_bad_item(config, queue, log):
    """Tests that bad items stay in the unstaged area, reported to the log,
    but don't cause an exception to be raised.
    """
    enqueue_test_data(queue, config)
    enqueue_test_data(queue, config)

    bad_item_path = queue.list_queued_items(full_paths=True)[0]
    meta_path = os.path.join(bad_item_path, META_NAME)
    with open(meta_path, 'w') as meta_file:
        meta_file.write('asdf')
    bad_item_id = os.path.basename(bad_item_path)

    upload_auction_result(config=config, queue=queue, log=log)
    assert log.called_error, 'Uploader must report errors'

    expected_queue_items = {
        'stage': [],
        'queue': [],
        'unstage': [bad_item_id],
    }
    assert list_queue_items(queue) == expected_queue_items
