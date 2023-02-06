#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.testcase import TestCase, main

import errno
import fcntl
import os


class T(TestCase):
    @classmethod
    def prepare(cls):
        pass

    def test(self):
        running_flag_path = os.path.join(self.meta_paths.lock_path, 'report_running.lock')
        with open(running_flag_path, 'r') as f:
            try:
                fcntl.flock(f, fcntl.LOCK_EX | fcntl.LOCK_NB)
            except IOError as e:
                self.assertEqual(e.errno, errno.EAGAIN)
            else:
                self.fail("Flag is not set")


if __name__ == '__main__':
    main()
