# -*- coding: utf-8 -*-

from zake.fake_client import FakeClient
import threading
import pytest

import market.pylibrary.zkclient as zkclient

from market.pylibrary.zkclient.am_i_master_manager import (
    get_am_i_master,
    renew_am_i_master,
)


MASTER_DIR = '/mimaster'


def Client():
    client = zkclient._Upgrade(FakeClient)()
    # client = zkclient.Client('127.0.0.1:2451')
    client.start()
    client.safe_delete('/mimaster', recursive=True)
    return client


def test():
    with Client() as client:
        minfo = zkclient.IndexerClient(client, mitype='telecaster')
        assert minfo.get_current_master() is not None
        assert minfo.get_current_master_type() == 'telecaster'
        assert minfo.am_i_master() is True

        minfo.set_master('mi01h', 'stratocaster')
        assert minfo.get_current_master() == 'mi01h'
        assert minfo.get_current_master_type() == 'stratocaster'
        assert minfo.am_i_master() is False
        assert zkclient.am_i_master(client=client, mitype='telecaster') is False

        minfo.make_me_master()
        assert minfo.get_current_master_type() == 'telecaster'
        assert minfo.am_i_master() is True
        assert zkclient.am_i_master(client=client, mitype='telecaster') is True


def test_lock():
    lock_dir = '/mimaster/some_lock'
    with Client() as client:
        minfo = zkclient.IndexerClient(client, mitype='telecaster')

        minfo.make_me_master()
        lock = minfo.BigLock(lock_dir)
        assert not lock.is_acquired
        with lock:
            print(lock)
            assert lock.is_acquired

        minfo.set_master('mi0h', 'stratocaster')
        lock = minfo.BigLock(lock_dir)
        assert not lock.is_acquired
        with lock:
            print(lock)
            assert not lock.is_acquired


def test_lock_timeout():
    lock_dir = '/mimaster/some_lock'
    with Client() as client:
        minfo = zkclient.IndexerClient(client, mitype='telecaster')
        minfo.make_me_master()

        def failed_lock():
            lock = minfo.BigLock(lock_dir)
            lock.acquire(blocking=False)
            assert not lock.is_acquired

        def failed_timeout_lock():
            with pytest.raises(zkclient.LockTimeout):
                lock = minfo.BigLock(lock_dir)
                lock.acquire(timeout=0.001)

        lock = minfo.BigLock(lock_dir)
        with lock:
            assert lock.is_acquired
            failed_lock()
            failed_timeout_lock()


def test_lock_thread():
    with Client() as client:
        minfo = zkclient.IndexerClient(client, mitype='telecaster')
        minfo.make_me_master()

        def first():
            with minfo.do_lock('/mimaster/some_lock', blocking=False) as lock:
                assert lock.is_acquired
                event1.set()
                event2.wait()

        def second():
            event1.wait()
            with minfo.do_lock('/mimaster/some_lock', blocking=False) as lock:
                assert not lock.is_acquired
            event2.set()

        event1 = threading.Event()
        event2 = threading.Event()
        threads = [threading.Thread(target=first),
                   threading.Thread(target=second)]
        for t in threads:
            t.start()
        for t in threads:
            t.join()


def test_am_i_master():
    with Client() as client:
        zkclient.IndexerClient(client, mitype='master')

        assert zkclient.am_i_master(client, mitype='master')
        assert zkclient.am_i_master(client, mitype='slave') is False

    assert zkclient.am_i_master(mitype='master', client_type=FakeClient) is True


def test_am_i_master_manager():
    dirname = '.'
    for value1, value2 in [(True, True), (False, False), ('1', True), ('0', False)]:
        renew_am_i_master(dirname, zk_am_the_master=value1)
        assert get_am_i_master(dirname) == value2


def test_switch():
    with Client() as client:
        idx_client = zkclient.IndexerClient(client)
        assert idx_client.get_switch('foo') is False
        idx_client.set_switch('foo', True)
        assert idx_client.get_switch('foo') is True
        idx_client.set_switch('foo', False)
        assert idx_client.get_switch('foo') is False
