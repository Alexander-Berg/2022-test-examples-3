# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import multiprocessing
import os
from multiprocessing import Event

import mock

from common.data_api.deploy.instance import deploy_client
from common.utils.multiproc import run_parallel, run_instance_method_parallel, get_cpu_count


def foo_test_run_parallel(args):
    a, b = args

    # Лочим выполннение процессов, пока не увидим 3й чанк данных -
    # это будет означать, что 3 воркера точно запустились.
    # Без этого функция может выполнится в одном воркере несколько раз, и мы не сможем проверить pid'ы процессов
    if (a, b) == (5, 6):
        test_run_parallel.lock.set()
    else:
        test_run_parallel.lock.wait()

    return os.getpid(), os.getppid(), a * b


def test_run_parallel():
    test_run_parallel.lock = Event()

    pids = set()
    parent_pids = set()
    results = []
    data = [(1, 2), (3, 4), (5, 6), (7, 8)]
    for proc_pid, parent_pid, result in run_parallel(foo_test_run_parallel, data, pool_size=3):
        pids.add(proc_pid)
        parent_pids.add(parent_pid)
        results.append(result)

    assert len(pids) == 3
    assert parent_pids == {os.getpid()}
    assert sorted(results) == [2, 12, 30, 56]


def test_run_instance_method_parallel():
    test_run_parallel.lock = Event()

    class A(object):
        def foo(self, a, b):
            # проверяем, что инстанс тот же, что и в мастер-процессе (т.е. не было сериализации объекта)
            assert id(self) == id(some_instance)

            return foo_test_run_parallel((a, b))

    pids = set()
    parent_pids = set()
    results = []
    data = [(1, 2), (3, 4), (5, 6), (7, 8)]

    some_instance = A()
    for proc_pid, parent_pid, result in run_instance_method_parallel(some_instance.foo, data, pool_size=3):
        pids.add(proc_pid)
        parent_pids.add(parent_pid)
        results.append(result)

    assert len(pids) == 3
    assert parent_pids == {os.getpid()}
    assert sorted(results) == [2, 12, 30, 56]


def test_get_cpu_count():
    with mock.patch.object(multiprocessing, 'cpu_count') as m_cpu_count:
        m_cpu_count.return_value = 10
        assert get_cpu_count() == 10

        m_cpu_count.return_value = 2
        assert get_cpu_count() == 2

        m_cpu_count.return_value = 1
        assert get_cpu_count() == 1

    with mock.patch.dict(os.environ, {'QLOUD_PROJECT': 'rasp'}):
        with mock.patch.dict(os.environ, {'QLOUD_CPU_GUARANTEE': '4.0'}):
            assert get_cpu_count() == 4

        with mock.patch.dict(os.environ, {'QLOUD_CPU_GUARANTEE': '0.5'}):
            cpu_count = get_cpu_count()
            assert cpu_count == 1
            assert isinstance(cpu_count, int)

        with mock.patch.dict(os.environ, {'QLOUD_CPU_GUARANTEE': '2'}):
            assert get_cpu_count() == 2

    with mock.patch.dict(os.environ, {'SANDBOX_CONFIG': 'rasp'}):
        with mock.patch.dict(os.environ, {'SANDBOX_CPU_GUARANTEE': '0.4'}):
            assert get_cpu_count() == 1

        with mock.patch.dict(os.environ, {'SANDBOX_CPU_GUARANTEE': '241'}):
            assert get_cpu_count() == 241

    with mock.patch.dict(os.environ, {'TRAVEL_DEPLOY_PROJECT': 'rasp'}):
        with mock.patch.object(deploy_client, 'get_current_box_requirements') as m_get_current_box_requirements:
            m_get_current_box_requirements.return_value = {'cpu': {'cpu_guarantee_millicores': 1005.3}}
            assert get_cpu_count() == 1

            m_get_current_box_requirements.return_value = {'cpu': {'cpu_guarantee_millicores': 5433.8}}
            assert get_cpu_count() == 5

            m_get_current_box_requirements.return_value = {'cpu': {'cpu_guarantee_millicores': 0, 'cpu_limit_millicores': 3089.9}}
            assert get_cpu_count() == 3

            m_get_current_box_requirements.return_value = {'memory': {'memory_limit_bytes': 123}}
            with mock.patch.object(multiprocessing, 'cpu_count', return_value=64):
                assert get_cpu_count() == 64
