#!/usr/bin/env python
# -*- coding: utf-8 -*-
import os

from core.testcase import TestCase
from core.types import Package, File
from core import utils


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.need_execute_prepare_script = False
        cls.packages += [
            Package(name='package-damaged-archive', damaged_archive=True,
                    files=[
                        File(name='info.txt', content='Just info about package')
                    ]),
            Package(name='package-damaged-conf',
                    files=[
                        File(name='app.conf', path='./zeus_tmpl/conf', content='''

[general]
exec_path = {{ some_variable }}/app.bin
''')]
                    ),
            Package(name='package-stoping-failure',
                    files=[
                        File(name='package-stoping-failure.tar.gz.control', path='./init.d', executable=True,
                             content='''#!/bin/bash
case "$1" in
    stop)
        >&2 echo "Not enough memory for something"
        exit 23
    ;;
    check)
        exit 1
    ;;
esac
                ''')
                    ]),
            Package(name='package-starting-failure',
                    files=[
                        File(name='package-starting-failure.tar.gz.control', path='./init.d', executable=True,
                             content='''#!/bin/bash
case "$1" in
    start)
        >&2 echo "port is already in use"
        exit 19
    ;;
    check)
        exit 1
    ;;
esac
        ''')
                    ])
        ]

    def test_error_on_uncompressing(self):
        response = self.zeus.request('install_package package-damaged-archive.tar.gz')
        self.assertTrue(response.startswith('!failed to install package package-damaged-archive.tar.gz'))

    def test_error_on_templating(self):
        response = self.zeus.request('install_package package-damaged-conf.tar.gz')
        self.assertEqual("!error on generating configs for package package-damaged-conf.tar.gz: 'some_variable' is "
                         "undefined", response)

        self.assertFalse(os.path.exists(utils.abs_path(self.env, 'conf/app.conf')))

    def test_error_on_stoping(self):
        response = self.zeus.request('install_package package-stoping-failure.tar.gz')
        self.assertEqual('!error on stoping package package-stoping-failure.tar.gz: return code: 23, output: Not enough memory for something',
                         response)

    def test_error_on_starting(self):
        response = self.zeus.request('install_package package-starting-failure.tar.gz')
        self.assertEqual('!error on starting package package-starting-failure.tar.gz: return code: 19, output: port is already in use',
                         response)
