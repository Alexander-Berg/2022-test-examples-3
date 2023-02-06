# -*- coding: utf-8 -*-

from contextlib import contextmanager
import os
import shutil
import time

import pytest
import yatest

import market.idx.pylibrary.disk_queue.disk_queue as disk_queue


BASE_PATH = yatest.common.output_path('queue')


class ManualQueueStrategy(object):
    def create_item_id(self, item_id):
        return item_id

    def dequeue_item(self, items):
        return min(items)


@pytest.fixture(scope='module')
def names():
    """Names and contents of the test files.
    """
    return [
        ('bar', 'foo'),
        ('baz',),
        ('awol',),
        ('smth',),
    ]


@pytest.fixture(scope='module')
def files(names):
    """Paths of the test files.
    """
    def make_file(name):
        path = yatest.common.output_path(name)
        with open(path, 'w') as file_object:
            file_object.write(name)
        return path

    return [
        tuple(make_file(name) for name in names_tuple)
        for names_tuple in names
    ]


@contextmanager
def make_queue(strategy):
    """Makes a queue with a given strategy.
    """
    os.mkdir(BASE_PATH)
    try:
        yield disk_queue.DiskQueue(strategy, BASE_PATH, 3)
    finally:
        shutil.rmtree(BASE_PATH)


@pytest.yield_fixture()
def queue():
    """A queue that we would use in production.
    """
    with make_queue(disk_queue.DiskQueueStrategySimple()) as queue:
        yield queue


@pytest.yield_fixture()
def manual_queue():
    """A queue that we feed item IDs ourselves.
    """
    with make_queue(ManualQueueStrategy()) as queue:
        yield queue


def check_dequeue(queue, expected_item_id, expected_words):
    actual_item_id = queue.dequeue()
    item_path = queue.unstaged_item_path(actual_item_id)
    paths = sorted(
        os.path.join(item_path, name)
        for name in os.listdir(item_path)
    )

    actual_words = tuple(
        open(path).read()
        for path in paths
    )

    expected_data = {'item_id': expected_item_id, 'words': expected_words}
    actual_data = {'item_id': actual_item_id, 'words': actual_words}

    assert expected_data == actual_data


def test_stage(queue):
    """Tests that staging works.
    """
    item_path = None
    with queue.stage_item() as item_id:
        item_path = queue.staged_item_path(item_id)
        assert os.path.exists(item_path)

    assert not os.path.exists(item_path)


def test_stage_files(queue, files, names):
    """Tests that staging files works.
    """
    item_path = None
    with queue.stage_item_files(files[0]) as item_id:
        item_path = queue.staged_item_path(item_id)
        assert os.path.exists(item_path)
        assert list(names[0]) == sorted(os.listdir(item_path))

    assert not os.path.exists(item_path)


def test_enqueue(queue, files, names):
    """Tests that enqueuing works.
    """
    item0 = queue.enqueue_files(files[0])
    assert [item0] == queue.list_queued_items()

    item1 = queue.enqueue_files(files[1])
    assert [item0, item1] == sorted(queue.list_queued_items())
    assert [] == queue.list_staged_items()
    assert [] == queue.list_unstaged_items()


def test_enqueue_full(queue, files, names):
    """Tests that trying to enqueue onto a full queue
    fails cleanly.
    """
    queue.enqueue_files(files[0])
    queue.enqueue_files(files[1])
    queue.enqueue_files(files[2])

    assert 3 == len(queue.list_queued_items())

    with pytest.raises(disk_queue.DiskQueueFullError):
        queue.enqueue_files(files[3], timeout=0)

    assert 3 == len(queue.list_queued_items())
    assert [] == queue.list_staged_items()
    assert [] == queue.list_unstaged_items()


def test_enqueue_missing(queue):
    """Tests that adding invalid files fails cleanly.
    """
    with pytest.raises(IOError):
        queue.enqueue_files(('/missing_file',)),

    assert [] == queue.list_queued_items()
    assert [] == queue.list_staged_items()
    assert [] == queue.list_unstaged_items()


def test_dequeue_order(manual_queue, files, names):
    """Tests how IDs are adapted when staging.
    """
    manual_queue.enqueue_files(files[0], item_id='2')
    manual_queue.enqueue_files(files[1], item_id='1')
    manual_queue.enqueue_files(files[2], item_id='1')

    check_dequeue(manual_queue, '1', names[1])
    check_dequeue(manual_queue, '1-1', names[2])
    check_dequeue(manual_queue, '2', names[0])


def test_sanity(queue, files, names):
    """Tests that the default strategy works to ensure
    FIFO order.
    """
    item0 = queue.enqueue_files(files[0])
    item1 = queue.enqueue_files(files[1])
    time.sleep(1)
    item2 = queue.enqueue_files(files[2])

    check_dequeue(queue, item0, names[0])
    check_dequeue(queue, item1, names[1])
    check_dequeue(queue, item2, names[2])


def test_dequeue_empty(queue):
    """Checks that trying to dequeue from an empty queue
    raises the appropriate exception.
    """
    with pytest.raises(disk_queue.DiskQueueEmptyError):
        queue.dequeue(timeout=0)
