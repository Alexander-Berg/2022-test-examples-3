# coding: utf-8

import Queue
import socket
import pytest
import itertools

import sandbox.common.types.task as ctt
from sandbox.common import utils


@pytest.fixture()
def free_port():
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.bind(('127.0.0.1', 0))
    addr, port = s.getsockname()
    s.close()
    return port


class TestUtils:
    def test__lock_on_port(self, free_port):
        sock = utils.lock_on_port(free_port)
        assert sock
        pytest.raises(socket.error, utils.lock_on_port, free_port)


class TestHostInfo():
    host = ("sandbox-storage1.search.yandex.net", "iva6")
    ninja = ("ninja", "unk")
    unavailable_ssd = ("kamikadze", False)
    host_with_ssd = ("sandbox216.search.yandex.net", True)

    def test__correct_dc(self):
        assert utils.HostInfo.dc(self.host[0]) == self.host[1]

    def test_ninja_host(self):
        assert utils.HostInfo.dc(self.ninja[0]) == self.ninja[1]

    def test_inaccessible_host(self):
        assert utils.HostInfo.has_ssd(self.unavailable_ssd[0]) == self.unavailable_ssd[1]


def test__task_priority_type():
    _TP = ctt.Priority
    p = _TP(_TP.Class.BACKGROUND, _TP.Subclass.LOW)
    p = p.next
    assert p == _TP(_TP.Class.BACKGROUND, _TP.Subclass.NORMAL)
    p = p.next
    assert p == _TP(_TP.Class.BACKGROUND, _TP.Subclass.HIGH)
    p = p.next
    assert p == _TP(_TP.Class.SERVICE, _TP.Subclass.LOW)
    p = p.next
    assert p == _TP(_TP.Class.SERVICE, _TP.Subclass.NORMAL)
    p = p.next
    assert p == _TP(_TP.Class.SERVICE, _TP.Subclass.HIGH)
    p = p.next
    assert p == _TP(_TP.Class.USER, _TP.Subclass.LOW)
    p = p.next
    assert p == _TP(_TP.Class.USER, _TP.Subclass.NORMAL)
    p = p.next
    assert p == _TP(_TP.Class.USER, _TP.Subclass.HIGH)
    p = p.next
    assert p == _TP(_TP.Class.USER, _TP.Subclass.HIGH)

    p = p.prev
    assert p == _TP(_TP.Class.USER, _TP.Subclass.NORMAL)
    p = p.prev
    assert p == _TP(_TP.Class.USER, _TP.Subclass.LOW)
    p = p.prev
    assert p == _TP(_TP.Class.SERVICE, _TP.Subclass.HIGH)
    p = p.prev
    assert p == _TP(_TP.Class.SERVICE, _TP.Subclass.NORMAL)
    p = p.prev
    assert p == _TP(_TP.Class.SERVICE, _TP.Subclass.LOW)
    p = p.prev
    assert p == _TP(_TP.Class.BACKGROUND, _TP.Subclass.HIGH)
    p = p.prev
    assert p == _TP(_TP.Class.BACKGROUND, _TP.Subclass.NORMAL)
    p = p.prev
    assert p == _TP(_TP.Class.BACKGROUND, _TP.Subclass.LOW)
    p = p.prev
    assert p == _TP(_TP.Class.BACKGROUND, _TP.Subclass.LOW)


@pytest.mark.parametrize("jobs_graph", [
    [],
    [[]] * 42,
    [[], [0], [1], [2], [3], [4]],
    [[], [], [], [0, 1, 2], [3], [0], [1], [2], []],
])
def test__simple_computation_graph(jobs_graph):
    queue = Queue.Queue()
    for _ in range(3):  # repeat for increasing probability of flapping test detection
        graph = utils.SimpleComputationGraph()
        for job_ind, wait_jobs in enumerate(jobs_graph):
            graph.add_job(queue.put, args=(job_ind,), wait_jobs=wait_jobs)
        graph.run()
        results = [queue.get() for _ in jobs_graph]
        for job_ind, wait_jobs in enumerate(jobs_graph):
            previous_jobs = results[:results.index(job_ind)]
            assert all(child_job in previous_jobs for child_job in wait_jobs)


def test__simple_computation_graph__invalid_graph():
    queue = Queue.Queue()

    graph = utils.SimpleComputationGraph()
    job_id = graph.add_job(queue.put, args=(1,))
    job_id = graph.add_job(queue.put, args=(2,), wait_jobs=[job_id])
    with pytest.raises(ValueError):
        graph.add_job(queue.put, args=(3,), wait_jobs=[-1])
    with pytest.raises(ValueError):
        graph.add_job(queue.put, args=(3,), wait_jobs=[job_id + 1])
    with pytest.raises(ValueError):
        graph.add_job(queue.put, args=(4,), wait_jobs=[job_id + 2])
    graph.run()

    assert 1 == queue.get()
    assert 2 == queue.get()


def test__grouper():
    for g1, g2 in itertools.izip_longest(utils.grouper('ABCDEFG', 3), ['ABC', 'DEF', 'G']):
        assert "".join(g1) == g2


def test__grouper_longest():
    for g1, g2 in itertools.izip_longest(utils.grouper_longest('ABCDEFG', 3, 'x'), ['ABC', 'DEF', 'Gxx']):
        assert "".join(g1) == g2


def test__checker_fetcher():
    remote = ("http://proxy.sandbox.yandex-team.ru/144394513", "04fee33d0ea0c68feb4779b251127435")
    assert sum(map(len, utils.checker_fetcher(*remote))) == 222623


class TestClass(object):
    def __str__(self):
        return self.__class__.__name__


class TestClassWithUnicode(TestClass):
    REPR = u"class"

    def __unicode__(self):
        return self.REPR


def test__html_escape():
    text = u""
    assert utils.escape(text) is text
    text = u"йцукен"
    assert utils.escape(text.encode("utf-8")) == text
    text = "<>&"
    assert utils.escape(text) == u"&lt;&gt;&amp;"
    assert utils.escape(TestClass()) == TestClass.__name__
    assert utils.escape(TestClassWithUnicode()) == TestClassWithUnicode.REPR


def test__str2size():
    assert utils.str2size("1024") == 1024
    assert utils.str2size(100500) == 100500
    assert utils.str2size("1K") == 1 << 10
    assert utils.str2size("2M") == 2 << 20
    assert utils.str2size("3G") == 3 << 30
    assert utils.str2size("4.5T") == int((1 << 40) * 4.5)
