#!/usr/bin/env python
# -*- coding: utf-8 -*-
import ConfigParser
import os

import psutil

from core.testcase import TestCase
from core.types import Package, File
from core import utils

CONTROL_BIN_CONTENT = '''#!/bin/bash
mkdir -p {root_dir}/state/{daemon_name}/help_directory
mkdir -p {root_dir}/pids/{daemon_name}/

case "$1" in
    start)
        /sbin/start-stop-daemon --make-pidfile --start --quiet --background --pidfile {root_dir}/pids/{daemon_name}/{daemon_name}.pid --startas {root_dir}/bin/{daemon_name}.bin
        ;;
    stop)
        /sbin/start-stop-daemon --oknodo --signal KILL --retry 1 --stop --quiet --pidfile {root_dir}/pids/{daemon_name}/{daemon_name}.pid
        ;;
    check)
        /sbin/start-stop-daemon --status --pidfile {root_dir}/pids/{daemon_name}/{daemon_name}.pid
        ;;
esac
'''


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.need_install_on_start = True

        package_config = '''
{%- set port = 5555 -%}

[general]
app_port = {{ port }}
'''
        cls.packages += [
            Package(name='static_package', is_dynamic=False,
                    files=[File(name='info.txt', content='static_package', path='./meta/nondynamic'),
                           File(name='static_package.bin', path='./bin', executable=True,
                                content='''#!/bin/bash
sleep 360; '''),
                           File(name='static_package.tar.gz.control', path='./init.d', executable=True,
                                content=CONTROL_BIN_CONTENT.format(root_dir=cls.env['HOME'],
                                                                   daemon_name='static_package')),
                           ]),
            Package(name='dynamic_package',
                    files=[File(name='app.conf', path='./zeus_tmpl/conf', content=package_config),
                           File(name='dynamic_package.bin', path='./bin', executable=True, content='''#!/bin/bash
sleep 360; '''),
                           File(name='dynamic_package.tar.gz.control', path='./init.d', executable=True,
                                content=CONTROL_BIN_CONTENT.format(root_dir=cls.env['HOME'],
                                                                   daemon_name='dynamic_package')),
                           ])
        ]

    def test_install_all(self):
        actual_parser = ConfigParser.ConfigParser()
        actual_parser.read(utils.abs_path(self.env, 'conf/app.conf'))
        self.assertEqual('5555', actual_parser.get('general', 'app_port'))

        dynamic_package_pid = os.path.join(self.env['HOME'], 'pids', 'dynamic_package/dynamic_package.pid')
        self.assertTrue(os.path.exists(dynamic_package_pid))
        psutil.pid_exists(utils.read_pid_file(self.env, 'dynamic_package/dynamic_package.pid'))

        self.assertFalse(os.path.exists(os.path.join(self.env['HOME'], 'pids', 'static_package.pid')))
