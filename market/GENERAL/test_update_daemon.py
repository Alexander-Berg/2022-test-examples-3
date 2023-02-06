#!/usr/bin/env python
# -*- coding: utf-8 -*-

import psutil

from core import utils
from core.testcase import TestCase
from core.types import Package, File

CONTROL_BIN = '''#!/bin/bash
mkdir -p {root_dir}/pids/user_daemon/

case "$1" in
    start)
        /sbin/start-stop-daemon --make-pidfile --start --quiet --background --pidfile {root_dir}/pids/user_daemon/user_daemon.pid --startas {root_dir}/bin/user_daemon.bin
        ;;
    stop)
        /sbin/start-stop-daemon --oknodo --retry 5 --stop --quiet --pidfile {root_dir}/pids/user_daemon/user_daemon.pid
        ;;
    check)
        /sbin/start-stop-daemon --status --pidfile {root_dir}/pids/user_daemon/user_daemon.pid
        ;;
esac
'''


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.packages += [
            Package(name='package-daemon',
                    files=[
                        File(name='version_daemon.txt', path='./meta', content='1.0.0'),
                        File(name='user_daemon.bin', path='./bin', executable=True, content='''#!/bin/bash
sleep 360'''),
                        File(name='package-daemon.tar.gz.control', path='./init.d', executable=True,
                             content=CONTROL_BIN.format(root_dir=cls.env['HOME']))
                    ])
        ]

    def test_update_daemon(self):
        response = self.zeus.request('install_package package-daemon.tar.gz')
        self.assertEqual('package package-daemon.tar.gz installed', response)

        self.assertEqual('1.0.0', utils.read_file(self.env, 'meta/version_daemon.txt'))

        def daemon_is_running():
            pid = utils.read_pid_file(self.env, 'user_daemon/user_daemon.pid')
            return psutil.pid_exists(pid)

        self.assertTrue(
            utils.wait_for_success(daemon_is_running, 2)
        )

        old_pid = utils.read_pid_file(self.env, 'user_daemon/user_daemon.pid')

        response = self.zeus.request('install_package package-daemon.tar.gz')
        self.assertEqual('package package-daemon.tar.gz is up to date', response)

        Package(name='package-daemon',
                files=[
                    File(name='version_daemon.txt', path='./meta', content='2.0.0'),
                    File(name='user_daemon.bin', path='./bin', executable=True, content='''#!/bin/bash
                sleep 360'''),
                    File(name='package-daemon.tar.gz.control', path='./init.d', executable=True,
                         content=CONTROL_BIN.format(root_dir=self.env['HOME']))
                ]).create(self.env['HOME'])

        response = self.zeus.request('install_package package-daemon.tar.gz')
        self.assertEqual('package package-daemon.tar.gz installed', response)

        self.assertTrue(
            utils.wait_for_success(daemon_is_running, 2)
        )

        new_pid = utils.read_pid_file(self.env, 'user_daemon/user_daemon.pid')
        self.assertNotEquals(old_pid, new_pid)

        response = self.zeus.request('stop_package package-daemon.tar.gz')
        self.assertEqual('package package-daemon.tar.gz stopped', response)

        self.assertTrue(
            utils.wait_for_failure(daemon_is_running, 2)
        )
