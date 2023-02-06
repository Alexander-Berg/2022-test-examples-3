#!/usr/bin/env python
# -*- coding: utf-8 -*-

from core import utils
from core.testcase import TestCase
from core.types import Package, File


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.packages += [
            Package(name='package_update', files=[
                File(name='version_update.txt', content='0.1.0', path='./meta')
            ]),
            Package(name='package_noupdate', files=[
                File(name='version_noupdate.txt', content='1.0.0', path='./meta')
            ])
        ]

    def test_simple_update(self):
        self.assertEqual('package package_noupdate.tar.gz installed\n'
                         'package package_update.tar.gz installed\n'
                         'package zeus.tar.gz is up to date\n',
                         self.zeus_notify())
        self.assertEqual('0.1.0', utils.read_file(self.env, 'meta/version_update.txt'))
        self.assertEqual('1.0.0', utils.read_file(self.env, 'meta/version_noupdate.txt'))
        self.assertEqual('package package_noupdate.tar.gz is up to date\n'
                         'package package_update.tar.gz is up to date\n'
                         'package zeus.tar.gz is up to date\n',
                         self.zeus_notify())

        Package(name='package_update', files=[
            File(name='version_update.txt', content='0.2.0', path='./meta')
        ]).create(self.env['HOME'])
        self.assertEqual('package package_noupdate.tar.gz is up to date\n'
                         'package package_update.tar.gz installed\n'
                         'package zeus.tar.gz is up to date\n',
                         self.zeus_notify())
        self.assertEqual('0.2.0', utils.read_file(self.env, 'meta/version_update.txt'))
        self.assertEqual('1.0.0', utils.read_file(self.env, 'meta/version_noupdate.txt'))
