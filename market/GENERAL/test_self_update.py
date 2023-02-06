#!/usr/bin/env python
# -*- coding: utf-8 -*-
import os

from core import utils
from core.testcase import TestCase


class T(TestCase):
    def test_self_update(self):
        response = self.zeus.request('install_package zeus.tar.gz')
        self.assertEqual('package zeus.tar.gz is up to date', response)

        old_pid = utils.read_pid_file(self.env, 'zeus')

        os.utime(os.path.join(self.env['HOME'], 'zeus.tar.gz'), None)
        self.zeus.request('install_package zeus.tar.gz')

        def check_zeus_restarted():
            try:
                new_pid = utils.read_pid_file(self.env, 'zeus')
                return new_pid != old_pid
            except Exception:
                return False

        self.assertTrue(utils.wait_for_success(check_zeus_restarted))
        self.assertTrue(utils.wait_for_success(self.zeus.check_alive))
