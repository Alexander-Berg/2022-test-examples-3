# -*- coding: utf-8 -*-
import multiprocessing
import os
import os.path
import time
import threading

from travel.avia.ticket_daemon.ticket_daemon.lib.ipc import lock_resource, LOCKS_DIR


TEST_FILE = 'lock-counter'
RESOURCE_KEY = 'resource-key'
MAX_PARALLEL_UNIT_COUNT = 20


def race_condition_method(index, critical_list):
    with lock_resource(RESOURCE_KEY):
        if not os.path.exists(TEST_FILE):
            time.sleep(1)
            critical_list[index] = 1
            fobj = open(TEST_FILE, 'w')
            fobj.close()
        else:
            pass


def test_lock_resource_thread():
    critical_list = [-1] * MAX_PARALLEL_UNIT_COUNT

    threads = [
        threading.Thread(target=race_condition_method, args=(i, critical_list))
        for i in range(MAX_PARALLEL_UNIT_COUNT)
    ]

    for thread in threads:
        thread.start()

    for thread in threads:
        thread.join()

    assert len(filter(lambda i: i != -1, critical_list)) == 1


def test_lock_resource_process():
    critical_list = multiprocessing.RawArray('i', [-1] * MAX_PARALLEL_UNIT_COUNT)

    processes = [
        multiprocessing.Process(target=race_condition_method, args=(i, critical_list))
        for i in range(MAX_PARALLEL_UNIT_COUNT)
    ]

    for process in processes:
        process.start()

    for process in processes:
        process.join()

    assert len(filter(lambda i: i != -1, critical_list)) == 1


def test_file_system_key():
    with lock_resource('/some/key'):
        assert os.path.exists('%s/_some_key.lock' % LOCKS_DIR)


def teardown():
    if os.path.exists(TEST_FILE):
        os.remove(TEST_FILE)
