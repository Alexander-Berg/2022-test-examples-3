# -*- coding: utf-8 -*-

from threading import Condition, Thread
import logging
import os
import shutil
import unittest

from market.pylibrary.filelock import FileLock, LockItSafe, lock, AlreadyLocked, IsLocked


class Test(unittest.TestCase):
    rootdir = 'tmp'

    def setUp(self):
        self.tearDown()
        os.makedirs(self.rootdir)
        # logging.basicConfig(level=logging.DEBUG, format='%(asctime)s [%(levelname)s] [%(name)s] %(message)s')

    def tearDown(self):
        shutil.rmtree(self.rootdir, ignore_errors=True)

    def test(self):
        def do_write_lock(name):
            log = logging.getLogger(name)
            lock = FileLock(filepath)

            for i in range(3):
                log.info('try acquire write lock')
                result = lock.acquire(blocking=False)
                self.assertFalse(result)
                self.assertFalse(lock.locked)
                log.info('already locked')

            want_write_lock.acquire()
            log.info('i want write lock')
            want_write_lock_flag.append(0)
            want_write_lock.notify()
            want_write_lock.release()

            result = lock.acquire(blocking=True)
            self.assertTrue(result)
            self.assertTrue(lock.locked)
            log.info('write lock ok')
            lock.release()
            self.assertFalse(lock.locked)

        filepath = os.path.join(self.rootdir, 'lock')
        want_write_lock = Condition()
        want_write_lock_flag = []
        nthreads = 2

        log = logging.getLogger('main')
        lock = FileLock(filepath)

        log.info('acquire read lock')
        lock.acquire(shared=True)
        self.assertTrue(lock.locked)

        log.info('start thread')
        threads = [
            Thread(target=do_write_lock, args=('thread-%d' % (i+1),))
            for i in range(nthreads)
        ]
        for t in threads:
            t.start()

        # time.sleep(0.05)

        log.info('i am waiting for notify')
        want_write_lock.acquire()
        while len(want_write_lock_flag) != nthreads:
            want_write_lock.wait()
        log.info('i got notify')
        want_write_lock.release()

        log.info('release read lock')
        lock.release()
        self.assertFalse(lock.locked)

        for t in threads:
            t.join()

    def test_dir(self):
        lock = FileLock(self.rootdir)
        # acquire
        lock.acquire(blocking=False)
        self.assertTrue(lock.locked)
        # release
        lock.release()
        self.assertFalse(lock.locked)

    def test_two_construction_at_the_same_time(self):
        filepath = os.path.join(self.rootdir, 'lock')
        with lock(filepath, blocking=False) as outer_lock:
            self.assertTrue(outer_lock.locked)
            self.assertRaises(AlreadyLocked, lock(filepath, blocking=False).__enter__)

    def test_with_construction_one_after_another(self):
        filepath = os.path.join(self.rootdir, 'lock')
        with lock(filepath, blocking=False) as first_lock:
            self.assertTrue(first_lock.locked)
        with lock(filepath, blocking=False) as second_lock:
            self.assertTrue(second_lock.locked)

    def test_LockItSafe(self):
        filepath = os.path.join(self.rootdir, 'lock')
        with LockItSafe(filepath):
            self.assertTrue(IsLocked(filepath))
        self.assertFalse(IsLocked(filepath))


if __name__ == '__main__':
    unittest.main()
