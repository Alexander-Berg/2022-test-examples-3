import time

from search.martylib.core.exceptions import Locked

from search.morty.src.common.lock import PostgreLock

from search.morty.tests.utils.test_case import MortyTestCase


class TestPostgresLock(MortyTestCase):
    def test_lock(self):
        lock = PostgreLock()

        # test lock acquired
        with lock.lock():
            try:
                with lock.lock():
                    assert False
            except Locked:
                pass

        # test lock absolve
        with lock.lock():
            pass

        # test lock timeout
        lock = PostgreLock(timeout=1)
        with lock.lock():
            time.sleep(2)
            with lock.lock():
                pass

        # test different locks
        lock_1 = PostgreLock('1')
        lock_2 = PostgreLock('2')

        with lock_1.lock():
            with lock_2.lock():
                pass
