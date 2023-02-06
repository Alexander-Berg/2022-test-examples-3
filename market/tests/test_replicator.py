# -*- coding: utf-8 -*-

"""Tests cross-cluster YT replication.

Each test starts with a set of data split across two clusters in batches
marked by the key column. Some batches may be present in both clusters,
but a batch shall never be split.

Each test then runs replication and then checks that the table sets
are now identical.
"""

import os
import pytest

from contextlib import contextmanager
from multiprocessing.pool import ThreadPool

from mapreduce.yt.python.yt_stuff import YtConfig, YtStuff
from market.pylibrary.yt_replicator import DumbTransferManager, Replicator, SerialPool

import yatest


@pytest.fixture(scope='module')
def data():
    """Stub data sets for YT tables.
    """
    def make_data(key, values):
        return [
            dict(key=key, value=value)
            for value in values
        ]

    return {
        1: make_data(1, ['foo', 'bar']),
        2: make_data(2, ['fubar', 'awol']),
        3: make_data(3, ['lzzy']),
        4: make_data(4, ['john', 'paul', 'george', 'ringo']),
    }


@contextmanager
def local_yt(wd_root):
    os.mkdir(wd_root)

    config = YtConfig(yt_work_dir=wd_root)
    yt_stuff = YtStuff(config=config)
    yt_stuff.start_local_yt()
    try:
        yield yt_stuff
    finally:
        yt_stuff.stop_local_yt()


@pytest.yield_fixture(scope='module')
def yt_foo():
    """A local YT server.
    """
    wd_root = yatest.common.output_path('yt_foo_wd')
    with local_yt(wd_root) as yt_stuff:
        yield yt_stuff


@pytest.yield_fixture(scope='module')
def yt_bar():
    """A local YT server.
    """
    wd_root = yatest.common.output_path('yt_bar_wd')
    with local_yt(wd_root) as yt_stuff:
        yield yt_stuff


@pytest.yield_fixture()
def replicator(yt_foo, yt_bar):
    """Sets up a replicator and then does the cheap cleanup afterwards.
    """
    yield Replicator(
        'key',
        {
            yt_foo.get_server(): '//root_foo',
            yt_bar.get_server(): '//root_bar',
        },
        pool_class=SerialPool,
        transfer_manager_class=DumbTransferManager,
    )
    yt_foo.get_yt_client().remove('//root_foo', recursive=True, force=True)
    yt_bar.get_yt_client().remove('//root_bar', recursive=True, force=True)


@pytest.yield_fixture()
def replicator_for_test_attrs(yt_foo, yt_bar):
    """Sets up a replicator and then does the cheap cleanup afterwards.
    """
    yield Replicator(
        'key',
        {
            yt_foo.get_server(): '//root_foo',
            yt_bar.get_server(): '//root_bar',
        },
        pool_class=SerialPool,
        transfer_manager_class=DumbTransferManager,
        attrs_to_copy_set=set(['myattr_name']),
    )
    yt_foo.get_yt_client().remove('//root_foo', recursive=True, force=True)
    yt_bar.get_yt_client().remove('//root_bar', recursive=True, force=True)


def write_data(yt_stuff, table, data):
    """Writes data to the given table on the local YT server.
    """
    yt = yt_stuff.get_yt_client()
    yt.create(
        'table',
        table,
        ignore_existing=True,
        recursive=True,
        attributes=dict(
            schema=[
                dict(name='key', type='uint64'),
                dict(name='value', type='string'),
            ],
        )
    )
    yt.write_table(table, data)


def check_data(yt_stuff, table, expected_data):
    """Check that the table exists and has expected_data as contents,
    not necessarily in the same order.
    """
    def normalize_data(values):
        return sorted(
            (value['key'], value['value'])
            for value in values
        )

    yt = yt_stuff.get_yt_client()

    expected_data = normalize_data(expected_data)
    actual_data = normalize_data(yt.read_table(table))

    # check values disregarding the order
    assert expected_data == actual_data


def write_attribute(yt_stuff, table, attr_name, attr_value):
    """Writes attribute to the given table on the local YT server
    """
    yt = yt_stuff.get_yt_client()
    yt.set_attribute(table, attr_name, attr_value)


def check_attribute(yt_stuff, table, attr_name, expected_value):
    """Check that the table exist and has expected attribue
    """
    yt = yt_stuff.get_yt_client()
    actual_value = yt.get(table + '/@{attr_name}'.format(attr_name=attr_name))

    assert expected_value == actual_value


def check_no_data(yt_stuff, table):
    """Check that a table doesn't exist.
    """
    assert not yt_stuff.get_yt_client().exists(table)


def test_all_foo(yt_foo, yt_bar, replicator, data):
    """Basic replication check.
    """
    write_data(yt_foo, '//root_foo/1', data[1])
    write_data(yt_foo, '//root_foo/2', data[2])
    replicator.run(
        [
            (yt_foo.get_server(), ['1', '2']),
            (yt_bar.get_server(), [])
        ]
    )

    check_data(yt_foo, '//root_foo/1', data[1])
    check_data(yt_foo, '//root_foo/2', data[2])
    check_data(yt_bar, '//root_bar/1', data[1])
    check_data(yt_bar, '//root_bar/2', data[2])


def test_all_bar(yt_foo, yt_bar, replicator, data):
    """Basic replication check.
    """
    write_data(yt_bar, '//root_bar/1', data[1])
    write_data(yt_bar, '//root_bar/2', data[2])
    replicator.run(
        [
            (yt_foo.get_server(), []),
            (yt_bar.get_server(), ['1', '2'])
        ]
    )

    check_data(yt_foo, '//root_foo/1', data[1])
    check_data(yt_foo, '//root_foo/2', data[2])
    check_data(yt_bar, '//root_bar/1', data[1])
    check_data(yt_bar, '//root_bar/2', data[2])


def test_some_bar(yt_foo, yt_bar, replicator, data):
    """Incomplete spec should still be respected.
    """
    write_data(yt_bar, '//root_bar/1', data[1])
    write_data(yt_bar, '//root_bar/2', data[2])
    replicator.run(
        [
            (yt_foo.get_server(), []),
            (yt_bar.get_server(), ['1'])
        ]
    )

    check_data(yt_foo, '//root_foo/1', data[1])
    check_no_data(yt_foo, '//root_foo/2')
    check_data(yt_bar, '//root_bar/1', data[1])
    check_data(yt_bar, '//root_bar/2', data[2])


def test_mixed(yt_foo, yt_bar, replicator, data):
    """Tables should be handled independently.
    """
    write_data(yt_foo, '//root_foo/1', data[1])
    write_data(yt_bar, '//root_bar/2', data[2])
    replicator.run(
        [
            (yt_foo.get_server(), ['1']),
            (yt_bar.get_server(), ['2'])
        ]
    )

    check_data(yt_foo, '//root_foo/1', data[1])
    check_data(yt_foo, '//root_foo/2', data[2])
    check_data(yt_bar, '//root_bar/1', data[1])
    check_data(yt_bar, '//root_bar/2', data[2])


def test_partially_equal(yt_foo, yt_bar, replicator, data):
    """Partially intersecting data should be retained
    and not duplicated.
    """
    write_data(yt_foo, '//root_foo/1', data[1] + data[2])
    write_data(yt_bar, '//root_bar/1', data[1] + data[3])
    replicator.run(
        [
            (yt_foo.get_server(), ['1']),
            (yt_bar.get_server(), ['1']),
        ]
    )

    check_data(yt_foo, '//root_foo/1', data[1] + data[2] + data[3])
    check_data(yt_bar, '//root_bar/1', data[1] + data[2] + data[3])


def test_unmatched(yt_foo, yt_bar, replicator, data):
    """Unmatched tables should not result in data loss. But we
    don't require consistency either, we assume that we'll get
    there next time.
    """
    write_data(yt_foo, '//root_foo/1', data[1])
    write_data(yt_bar, '//root_bar/1', data[2])

    # may change with the implementation
    with pytest.raises(Exception):
        replicator.run(
            [
                (yt_foo.get_server(), ['1']),
                (yt_bar.get_server(), []),
            ]
        )

    # this is what we expect from the next replicator run
    replicator.run(
        [
            (yt_foo.get_server(), ['1']),
            (yt_bar.get_server(), ['1']),
        ]
    )

    check_data(yt_foo, '//root_foo/1', data[1] + data[2])
    check_data(yt_bar, '//root_bar/1', data[1] + data[2])


def test_duplicate_table(yt_foo, yt_bar, replicator, data):
    """Duplicate tables should be just ignored.
    """
    write_data(yt_foo, '//root_foo/1', data[1] + data[2])
    write_data(yt_bar, '//root_bar/1', data[1] + data[3])
    replicator.run(
        [
            (yt_foo.get_server(), ['1', '1']),
            (yt_bar.get_server(), ['1']),
        ]
    )

    check_data(yt_foo, '//root_foo/1', data[1] + data[2] + data[3])
    check_data(yt_bar, '//root_bar/1', data[1] + data[2] + data[3])


def test_parallel(yt_foo, yt_bar, replicator, data):
    """Parallel execution with a thread pool should work.
    This is the default behavior, but here we disable multithreading
    by default to make debugging easier. This test is here to ensure
    that everything still works in parallel.
    """
    write_data(yt_foo, '//root_foo/1', data[1] + data[2])
    write_data(yt_foo, '//root_foo/2', data[4])
    write_data(yt_bar, '//root_bar/1', data[1] + data[3])
    write_data(yt_bar, '//root_bar/3', data[4])
    replicator.pool_class = ThreadPool
    replicator.run(
        [
            (yt_foo.get_server(), ['1', '2']),
            (yt_bar.get_server(), ['1', '3']),
        ]
    )

    check_data(yt_foo, '//root_foo/1', data[1] + data[2] + data[3])
    check_data(yt_foo, '//root_foo/2', data[4])
    check_data(yt_foo, '//root_foo/3', data[4])
    check_data(yt_bar, '//root_bar/1', data[1] + data[2] + data[3])
    check_data(yt_bar, '//root_bar/2', data[4])
    check_data(yt_bar, '//root_bar/3', data[4])


def test_subdirs_exist(yt_foo, yt_bar, replicator, data):
    """Subdirectories should not prevent replication from working.
    """
    write_data(yt_foo, '//root_foo/a/b', data[1])
    yt_bar.get_yt_client().create(
        'map_node',
        '//root_bar/a',
        recursive=True
    )
    replicator.run(
        [
            (yt_foo.get_server(), ['a/b']),
            (yt_bar.get_server(), []),
        ]
    )

    check_data(yt_foo, '//root_foo/a/b', data[1])
    check_data(yt_bar, '//root_bar/a/b', data[1])


def test_subdirs_dont_exist(yt_foo, yt_bar, replicator, data):
    """Non-existent directories should be created.
    """
    write_data(yt_foo, '//root_foo/a/b', data[1])
    replicator.run(
        [
            (yt_foo.get_server(), ['a/b']),
            (yt_bar.get_server(), []),
        ]
    )

    check_data(yt_foo, '//root_foo/a/b', data[1])
    check_data(yt_bar, '//root_bar/a/b', data[1])


def test_preprocess_postprocess(yt_foo, yt_bar, replicator, data):
    """Preprocess should be called on existing tables before replication.
    Postprocess should be called on all tables with the appropriate
    creation flag.
    """
    write_data(yt_foo, '//root_foo/1', data[1])
    write_data(yt_foo, '//root_foo/2', data[2])
    write_data(yt_bar, '//root_bar/2', data[3])

    pres = []
    posts = []

    def preprocess(proxy, table):
        pres.append((proxy, table))

    def postprocess(proxy, table, created):
        posts.append((proxy, table, created))

    replicator.preprocess = preprocess
    replicator.postprocess = postprocess
    replicator.run(
        [
            (yt_foo.get_server(), ['1', '2']),
            (yt_bar.get_server(), ['2']),
        ]
    )

    check_data(yt_foo, '//root_foo/1', data[1])
    check_data(yt_foo, '//root_foo/2', data[2] + data[3])
    check_data(yt_bar, '//root_bar/1', data[1])
    check_data(yt_bar, '//root_bar/2', data[2] + data[3])

    expected_pres = sorted([
        (yt_foo.get_server(), '1'),
        (yt_foo.get_server(), '2'),
        (yt_bar.get_server(), '2'),
    ])

    expected_posts = sorted([
        (yt_foo.get_server(), '1', False),
        (yt_foo.get_server(), '2', False),
        (yt_bar.get_server(), '1', True),
        (yt_bar.get_server(), '2', False),
    ])

    expected_data = {'pres': expected_pres, 'posts': expected_posts}
    actual_data = {'pres': sorted(pres), 'posts': sorted(posts)}

    assert expected_data == actual_data


def test_copy_attributes(yt_foo, yt_bar, replicator_for_test_attrs, data):
    """Succesfully copying of existing attribute
    """
    write_data(yt_foo, '//root_foo/1', data[1])
    write_attribute(yt_foo, '//root_foo/1', 'myattr_name', 'myattr_value')

    replicator_for_test_attrs.run(
        [
            (yt_foo.get_server(), ['1']),
            (yt_bar.get_server(), []),
        ]
    )
    check_attribute(yt_bar, '//root_bar/1', 'myattr_name', 'myattr_value')


def test_attribute_dont_exist(yt_foo, yt_bar, replicator_for_test_attrs, data):
    """Can't copy non existing attribute, exception
    """
    write_data(yt_foo, '//root_foo/1', data[1])

    with pytest.raises(Exception):
        replicator_for_test_attrs.run(
            [
                (yt_foo.get_server(), ['1']),
                (yt_bar.get_server(), []),
            ]
        )


def test_error_table_missing(yt_foo, yt_bar, replicator):
    """Trying to replicate a missing table is an error.
    """
    with pytest.raises(Exception):
        replicator.run(
            [
                (yt_foo.get_server(), ['1']),
                (yt_bar.get_server(), []),
            ]
        )


def test_error_absolute_path(yt_foo, yt_bar, replicator, data):
    """Absolute paths are forbidden.
    """
    write_data(yt_foo, '//root_foo/1', data[1])
    with pytest.raises(ValueError):
        replicator.run(
            [
                (yt_foo.get_server(), ['//root_foo/1']),
                (yt_bar.get_server(), []),
            ]
        )
