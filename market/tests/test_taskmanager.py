# -*- coding: utf-8 -*-

import logging
import os
import shutil
import signal
import time
import unittest

from market.idx.mif.mif.rpc.oi import AsyncProcess  # from market.pylibrary.hammer import AsyncProcess
from market.idx.mif.mif.rpc.taskmanager import TaskManager


logging.basicConfig(format='%(asctime)s [%(process)-5d] [%(levelname)-7s] [%(module)s,%(lineno)d] %(message)s', level=logging.DEBUG)


class Test(unittest.TestCase):

    tasks_dir = 'tests.tmp'

    def setUp(self):
        # Делаем для порожденных процессов родителем init.
        # get_task_info не умеет работать с zombie (их не может быть).
        self._old_sigchld = signal.signal(signal.SIGCHLD, signal.SIG_IGN)

        shutil.rmtree(self.tasks_dir, ignore_errors=True)
        self.tm = TaskManager(self.tasks_dir)

    def tearDown(self):
        signal.signal(signal.SIGCHLD, self._old_sigchld)

        shutil.rmtree(self.tasks_dir, ignore_errors=True)

    def wait(self, tid):
        while True:
            tinfo = self.tm.get_task_info(tid)
            if tinfo.is_finished():
                return tinfo
            time.sleep(0.01)

    def test(self):
        ntasks = 3
        tids = [self.tm.start_task(time.sleep, (0.01,)) for unused in xrange(ntasks)]
        self.assertEqual(len(set(tids)), ntasks)
        while True:
            tasks = self.tm.get_tasks()
            self.assertEqual(len(tasks), ntasks)

            running_tasks = self.tm.get_running_tasks()
            if not running_tasks:
                break

        self.assertEqual(len(self.tm.get_tasks()), ntasks)
        self.assertEqual(len(self.tm.get_finished_tasks()), ntasks)
        self.assertEqual(len(self.tm.get_running_tasks()), 0)

    def test_simple(self):
        tid = self.tm.start_task(time.sleep, (0.01,))
        tinfo = self.wait(tid)
        self.assertTrue(tinfo.is_finished())

    def test_kill(self):
        tid = self.tm.start_task(time.sleep, (100,))
        self.tm.kill_task(tid)
        self.wait(tid)
        self.assertEqual(len(self.tm.get_running_tasks()), 0)
        self.assertEqual(len(self.tm.get_finished_tasks()), 1)

    def test_sub_jobs(self):
        class TaskManagerHooked(TaskManager):

            CHECK_DIR = os.path.join(self.tasks_dir, 'test_sub_jobs')

            def __init__(self, *args, **kwargs):
                os.makedirs(self.CHECK_DIR)
                return super(TaskManagerHooked, self).__init__(*args, **kwargs)

            def _do_task_finish(self, *args, **kwargs):
                # touch file 'pid' in check directory
                open(os.path.join(self.CHECK_DIR, str(os.getpid())), 'w').close()

                return super(TaskManagerHooked, self)._do_task_finish(*args, **kwargs)

        def sub_job():
            time.sleep(10)

        def job():
            a1 = AsyncProcess(sub_job, [])
            a2 = AsyncProcess(sub_job, [])
            time.sleep(10)
            a1.wait_result()
            a2.wait_result()

        tm_hooked = TaskManagerHooked(self.tasks_dir)

        tid = tm_hooked.start_task(job, ())
        tm_hooked.kill_task(tid)
        self.wait(tid)

        self.assertEqual(len(tm_hooked.get_running_tasks()), 0)
        self.assertEqual(len(tm_hooked.get_finished_tasks()), 1)

        # check only one process leave it's footstep in check directory
        self.assertEqual(len(os.listdir(TaskManagerHooked.CHECK_DIR)), 1)

    def test_task_raise(self):

        def raise_exception():
            raise Exception(error)

        error = 'BIG BARA BOOM!'

        tid = self.tm.start_task(raise_exception, ())
        self.wait(tid)

        self.assertEqual(len(self.tm.get_running_tasks()), 0)
        self.assertEqual(len(self.tm.get_finished_tasks()), 1)

        tinfo = self.tm.get_task_info(tid)
        self.assertEqual(tinfo.result, error)
        self.assertEqual(tinfo.tid, tid)

    def test_task_abort(self):
        tid = self.tm.start_task(os.abort)
        tinfo = self.wait(tid)
        self.assertTrue(tinfo.is_finished())

    def test_remove_old_finished_tasks(self):

        def nothing():
            pass

        tid = self.tm.start_task(nothing)
        self.wait(tid)
        filepaths = [
            self.tm._make_task_path(tid),
            self.tm._make_lock_path(tid),
        ]
        for filepath in filepaths:
            self.assertTrue(os.path.exists(filepath))

        self.tm.remove_old_finished_tasks(delta_in_seconds=-1)
        for filepath in filepaths:
            self.assertFalse(os.path.exists(filepath))


if __name__ == '__main__':
    unittest.main()
