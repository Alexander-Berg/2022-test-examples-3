#!/usr/bin/env python
# -*- coding: utf-8 -*-
import ConfigParser
import os

from core.testcase import TestCase
from core.types import Package, File
from core import utils


class T(TestCase):
    @classmethod
    def prepare(cls):
        package_config = '''
{%- set port = 5555 -%}

[general]
app_port = {{ port }}
'''
        cls.packages += [
            Package(name='package_with_conf',
                    files=[File(name='app.conf', path='./zeus_tmpl/conf', content=package_config)])
        ]

        # test_delete_non_finished_generations
        os.makedirs(os.path.join(cls.env['HOME'], 'pdata/generations'))
        os.mknod(os.path.join(cls.env['HOME'], 'pdata/generations/current.generation'))

        os.makedirs(os.path.join(cls.env['HOME'], 'pdata/generations/20180427_0557/dist1'))
        os.makedirs(os.path.join(cls.env['HOME'], 'pdata/generations/20180427_0557/dist2'))
        os.mknod(os.path.join(cls.env['HOME'], 'pdata/generations/20180427_0557/dist1/completed'))

        os.makedirs(os.path.join(cls.env['HOME'], 'pdata/generations/20180427_0828/dist1'))
        os.makedirs(os.path.join(cls.env['HOME'], 'pdata/generations/20180427_0828/dist2'))
        os.mknod(os.path.join(cls.env['HOME'], 'pdata/generations/20180427_0828/dist1/completed'))
        os.mknod(os.path.join(cls.env['HOME'], 'pdata/generations/20180427_0828/dist2/completed'))

    def test_install_all(self):
        actual_parser = ConfigParser.ConfigParser()
        actual_parser.read(utils.abs_path(self.env, 'conf/app.conf'))
        self.assertEqual('5555', actual_parser.get('general', 'app_port'))

        self.assertTrue(os.path.exists(utils.abs_path(self.env, 'logs/zeus_prepare.log')))

        self.assertTrue(os.path.exists(utils.abs_path(self.env, 'logs/report')))
        self.assertTrue(os.path.isdir(utils.abs_path(self.env, 'logs/report')))
