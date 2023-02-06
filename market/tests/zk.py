from __future__ import absolute_import

import pytest

import time
import random
import itertools as it
import functools as ft
import threading as th

import zake.fake_client

from sandbox import common


@pytest.fixture()
def fake_elector():
    class FakeElector(common.zk.Elector):
        __storage = zake.fake_client.FakeClient().storage

        def _create_zk_client(self):
            zk_client = zake.fake_client.FakeClient(storage=self.__storage)
            zk_client.start()
            return zk_client

        def wait(self, blocking=True):
            return

    return ft.partial(FakeElector, "", "/test/node", do_fork=False)


# noinspection PyShadowingNames
class TestElector(object):
    TIMEOUT = 1
    CONCURRENCY_FACTOR = 10
    ITERATIONS = 20

    @pytest.mark.xfail(run=False)  # FIXME: SANDBOX-4304
    def test__simple_election(self, fake_elector):
        contender1 = "contender1"
        contender2 = "contender2"
        contender3 = "contender3"

        elector1 = fake_elector(contender1, 1, timeout=self.TIMEOUT).start()
        elector2 = fake_elector(contender2, 2, timeout=self.TIMEOUT).start()
        elector3 = fake_elector(contender3, 3, timeout=self.TIMEOUT).start()
        for elector in (elector1, elector2, elector3):
            assert elector.is_primary is None

        result1 = []
        result2 = []
        thread2 = th.Thread(target=lambda: result2.append(elector2.elect()))
        thread1 = th.Thread(target=lambda: result1.append(elector1.elect()))
        map(th.Thread.start, (thread1, thread2))
        assert elector3.elect() == contender3
        map(th.Thread.join, (thread1, thread2))
        for result in (result1, result2):
            assert result and result[0] == contender3
        assert elector1.elect() == contender3
        for elector in (elector1, elector2, elector3):
            assert elector.elect() == contender3
        assert elector1.is_primary is False
        assert elector2.is_primary is False
        assert elector3.is_primary

        elector1.stop()
        elector1.start()
        assert elector1.is_primary is None
        assert elector2.is_primary is False
        assert elector3.is_primary
        assert elector1.elect() == contender3
        assert elector2.is_primary is False
        assert elector3.is_primary

        elector3.stop()
        assert common.utils.progressive_waiter(
            0, 1, self.TIMEOUT,
            lambda: all(_.is_primary is None for _ in (elector1, elector2, elector3)),
            sleep_first=False
        )[0]
        assert elector1.elect() == contender2
        assert elector2.elect() == contender2

        elector3.start()
        assert elector3.elect() == contender2

        elector2.stop()
        elector2.start()
        assert elector1.elect() == contender3
        assert elector3.elect() == contender3

        elector3.stop()
        elector3.start()
        result2 = []
        result3 = []
        thread2 = th.Thread(target=lambda: result2.append(elector2.elect()))
        thread3 = th.Thread(target=lambda: result3.append(elector3.elect()))
        elector1.priority = 4
        map(th.Thread.start, (thread2, thread3))
        assert elector1.elect() == contender1
        map(th.Thread.join, (thread2, thread3))
        assert result2 and result2[0] == contender1
        assert result3 and result3[0] == contender1
        elector1.stop()

        elector2.stop()
        elector1.priority = 0
        elector3.priority = 0
        result3 = []
        thread2 = th.Thread(target=lambda: result3.append(elector3.elect()))
        thread2.start()
        assert elector1.elect() == contender3
        thread2.join()
        assert result3 and result3[0] == contender3
        assert elector1.is_primary is False
        assert elector2.is_primary is None
        assert elector3.is_primary is True
        elector1.stop()
        elector3.stop()

    @pytest.mark.xfail(run=False)  # FIXME: SANDBOX-4313
    def test__democratic_elections(self, fake_elector):
        contenders = {
            name: fake_elector(name, timeout=self.TIMEOUT).start()
            for name in ("contenter{}".format(i) for i in xrange(self.CONCURRENCY_FACTOR))
        }
        for i in xrange(self.ITERATIONS):
            results = {}
            threads = [
                th.Thread(
                    target=lambda n, e: (time.sleep(random.uniform(0, self.TIMEOUT)), results.setdefault(n, e.elect())),
                    args=(name, elector)
                )
                for name, elector in contenders.iteritems()
            ]
            random.shuffle(threads)
            map(th.Thread.start, threads)
            map(th.Thread.join, threads)
            assert len(results) == self.CONCURRENCY_FACTOR
            names = set(results.itervalues())
            assert len(names) == 1
            primary = names.pop()
            for name, elector in contenders.iteritems():
                assert name != primary and not elector.is_primary or name == primary and elector.is_primary
            contenders[primary].resign()

    @pytest.mark.xfail(run=False)  # FIXME: SANDBOX-4313
    def test__corrupt_elections(self, fake_elector):
        priorities = range(self.CONCURRENCY_FACTOR)
        contenders = {
            name: fake_elector(name, timeout=self.TIMEOUT).start()
            for name in ("contenter{}".format(i) for i in xrange(self.CONCURRENCY_FACTOR))
        }
        for i in xrange(self.ITERATIONS):
            random.shuffle(priorities)
            for elector, priority in it.izip(contenders.itervalues(), priorities):
                elector.priority = priority
            results = {}
            threads = [
                th.Thread(
                    target=lambda n, e: (time.sleep(random.uniform(0, self.TIMEOUT)), results.setdefault(n, e.elect())),
                    args=(name, elector)
                )
                for name, elector in contenders.iteritems()
            ]
            random.shuffle(threads)
            map(th.Thread.start, threads)
            map(th.Thread.join, threads)
            assert len(results) == self.CONCURRENCY_FACTOR
            names = set(results.itervalues())
            assert len(names) == 1, (i, {_: (contenders[_].priority, contenders[_].is_primary) for _ in names if _})
            primary = names.pop()
            for name, elector in contenders.iteritems():
                assert name != primary and not elector.is_primary or name == primary and elector.is_primary
            contenders[primary].resign()
