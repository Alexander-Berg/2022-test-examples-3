#!/usr/bin/env python
# -*- coding: utf-8 -*-
import ConfigParser
import StringIO
import os

import psutil as psutil

from core import utils
from core.testcase import TestCase
from core.types import Package, File


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.packages += [
            Package(name='package_with_content',
                    files=[
                        File(name='info.txt', content='Just info about package'),
                        File(name='version.txt', content='0.2.0', path='./meta')]
                    ),
            Package(name='package_with_conf',
                    files=[
                        File(name='app.conf', path='./zeus_tmpl/conf', content='''
{%- set port = env.BSCONFIG_IPORT|int + 8 -%}

[general]
exec_path = {{ rc_env.paths.bin }}/app.bin
log_path = {{ rc_env.paths.logs }}/app/app.log
app_port = {{ port }}
''')]
                    ),
            Package(name='package_with_control_bin',
                    files=[
                        File(name='user_daemon.bin', path='./bin', executable=True, content='''#!/bin/bash
sleep 360; '''),
                        File(name='package_with_control_bin.tar.gz.control', path='./init.d', executable=True, content='''#!/bin/bash
mkdir -p {root_dir}/state/user_daemon/help_directory
mkdir -p {root_dir}/pids/user_daemon/

case "$1" in
    start)
        /sbin/start-stop-daemon --make-pidfile --start --quiet --background --pidfile {root_dir}/pids/user_daemon/user_daemon.pid --startas {root_dir}/bin/user_daemon.bin
        ;;
    stop)
        /sbin/start-stop-daemon --oknodo --signal KILL --retry 1 --stop --quiet --pidfile {root_dir}/pids/user_daemon/user_daemon.pid
        ;;
    check)
        /sbin/start-stop-daemon --status --pidfile {root_dir}/pids/user_daemon/user_daemon.pid
        ;;
esac
'''.format(root_dir=cls.env['HOME']))
                    ])
        ]

    def test_missing_package_name(self):
        response = self.zeus.request('install_package')
        self.assertEqual('!missing package name', response)

    def test_unknown_package_name(self):
        response = self.zeus.request('install_package yandex-market-trololo.tar.gz')
        self.assertEqual('!unknown package yandex-market-trololo.tar.gz',
                         response)

    def test_simple_extract(self):
        response = self.zeus.request('install_package package_with_content.tar.gz')
        self.assertEqual('package package_with_content.tar.gz installed', response)

        self.assertEqual('Just info about package', utils.read_file(self.env, 'info.txt'))
        self.assertEqual('0.2.0', utils.read_file(self.env, 'meta/version.txt'))

    def test_extract_with_conf(self):
        response = self.zeus.request('install_package package_with_conf.tar.gz')
        self.assertEqual('package package_with_conf.tar.gz installed', response)

        expected_conf = StringIO.StringIO('''
[general]
exec_path = {bin_path}/app.bin
log_path = {log_path}/app/app.log
app_port = {app_port}
'''.format(bin_path=utils.abs_path(self.env, 'bin'),
           log_path=utils.abs_path(self.env, 'logs'),
           app_port=int(self.env['BSCONFIG_IPORT']) + 8))

        expected_parser = ConfigParser.ConfigParser()
        expected_parser.readfp(expected_conf)

        actual_parser = ConfigParser.ConfigParser()
        actual_parser.read(utils.abs_path(self.env, 'conf/app.conf'))

        for key in ('exec_path', 'log_path', 'app_port'):
            self.assertEqual(expected_parser.get('general', key), actual_parser.get('general', key))

    def test_install_script(self):
        response = self.zeus.request('install_package package_with_control_bin.tar.gz')
        self.assertEqual('package package_with_control_bin.tar.gz installed', response)

        self.assertTrue(
            os.path.exists(utils.abs_path(self.env, 'state/user_daemon/help_directory'))
        )

        def daemon_is_running():
            pid = utils.read_pid_file(self.env, 'user_daemon/user_daemon.pid')
            return psutil.pid_exists(pid)

        pid_exists = utils.wait_for_success(daemon_is_running, 2)
        self.assertTrue(pid_exists)

        response = self.zeus.request('stop_package package_with_control_bin.tar.gz')
        self.assertEqual('package package_with_control_bin.tar.gz stopped', response)

        self.assertTrue(
            utils.wait_for_failure(daemon_is_running, 2)
        )
