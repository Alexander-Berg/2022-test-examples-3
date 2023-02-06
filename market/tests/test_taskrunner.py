# -*- coding: utf-8 -*-
import shutil
import subprocess
import time
import unittest

import mock
import psutil

from pyb import taskrunner


def sleep1(tts):
    time.sleep(tts)
    return 0


def sleep2(tts):
    return subprocess.call(args=['sleep', str(tts)])


class Test(unittest.TestCase):
    rootdir = 'tmp/taskrunner'

    def _clean(self):
        shutil.rmtree(self.rootdir, ignore_errors=True)

    def setUp(self):
        self._clean()

    def tearDown(self):
        self._clean()

    def test(self):
        name = 'sleep1'

        trunner = taskrunner.TaskRunner(self.rootdir)

        self.assertFalse(trunner.get(name).running)

        for _ in range(2):
            trunner.start(name=name, target=sleep1, args=(0.2,))
            task = trunner.get(name)
            self.assertTrue(task.running)

        while True:
            task = trunner.get(name)
            if task.finished:
                break
            time.sleep(0.1)

        task = trunner.get(name)
        self.assertTrue(task.finished)
        self.assertEqual(task.result, 0)

    def test_kill(self):
        name = 'sleep'
        trunner = taskrunner.TaskRunner(self.rootdir)
        trunner.start(name=name, target=sleep1, args=(100,))

        # try to kill zombie process
        with mock.patch('pyb.taskrunner.psutil.Process') as MockClass:
            instance = MockClass.return_value
            instance.status.return_value = psutil.STATUS_ZOMBIE
            trunner.kill(name)
            self.assertFalse(trunner.get(name).finished)

        trunner.kill(name)  # try to kill common process
        tinfo = trunner.get(name)
        self.assertTrue(tinfo.finished)
        self.assertEqual(tinfo.result, None)

    def test_taskinfo_error(self):
        caught = False
        try:
            taskrunner.TaskInfo('qwerty', 'unknown_state')
        except taskrunner.Error:
            caught = True
        self.assertTrue(caught)

    def test_kill_start_races(self):
        task_name = 'sleep'

        trunner = taskrunner.TaskRunner(self.rootdir)
        trunner.start(name=task_name, target=sleep1, args=(100,))

        self.assertTrue(trunner.get(task_name).running)

        trunner.kill(name=task_name)
        trunner.start(name=task_name, target=sleep1, args=(100,))

        self.assertTrue(trunner.get(task_name).running)

        trunner.kill(name=task_name)
        self.assertFalse(trunner.get(task_name).running)


if __name__ == '__main__':
    unittest.main()
