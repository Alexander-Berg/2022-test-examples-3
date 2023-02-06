#!/usr/bin/python
# -*- coding: utf-8 -*-

import os
import shutil
import unittest
import subprocess

import yatest.common

TEST_DIR = os.path.abspath('tmp')
CONF_DIR = os.path.join(TEST_DIR, 'conf-available')
ENV_TYPE_PATH = os.path.join(TEST_DIR, 'environment.type')
IDX_TYPE_PATH = os.path.join(TEST_DIR, 'marketindexer', 'type')


def config_path(name):
    return os.path.join(CONF_DIR, name)


def make_env_type(val):
    with open(ENV_TYPE_PATH, 'w') as f:
        f.write('%s\n' % val)


def make_idx_type(val):
    with open(IDX_TYPE_PATH, 'w') as f:
        f.write('%s\n' % val)


def make_config(name):
    open(config_path(name), 'w').close()


def run(name, dirname=TEST_DIR, extension=None):
    binpath = yatest.common.binary_path('market/tools/update_conf_available/update_conf_available')
    args = [
        binpath,
        '--name', name,
        '--prefix-conf-path', dirname,
        dirname,
    ]
    if extension is not None:
        args.append('--extension={}'.format(extension))
    return subprocess.call(args)


class Test(unittest.TestCase):
    def setUp(self):
        shutil.rmtree(TEST_DIR, ignore_errors=True)
        for dirname in (CONF_DIR, os.path.dirname(IDX_TYPE_PATH)):
            os.makedirs(dirname)

    def tearDown(self):
        shutil.rmtree(TEST_DIR, ignore_errors=True)

    def run_and_check(self, correct_name, name='local.ini', extension=None):
        local = os.path.join(TEST_DIR, name)
        self.assertEqual(0, run(name, extension=extension))
        self.assertTrue(os.path.lexists(local))
        self.assertTrue(os.path.exists(local))
        self.assertEqual(config_path(correct_name), os.path.realpath(local))

    def test_symlink_ok(self):
        make_config('testing.stratocaster.ini')
        make_config('testing.gibson.ini')
        make_config('testing.ini')

        make_env_type('testing')

        make_idx_type('stratocaster')
        self.run_and_check('testing.stratocaster.ini')

        make_idx_type('gibson')
        self.run_and_check('testing.gibson.ini')

        make_idx_type('planeshift.stratocaster')
        self.run_and_check('testing.ini')

        make_config('planeshift.ini')
        self.run_and_check('planeshift.ini')

        make_config('planeshift.stratocaster.ini')
        self.run_and_check('planeshift.stratocaster.ini')

        make_config('testing.planeshift.ini')
        self.run_and_check('testing.planeshift.ini')

        make_config('testing.planeshift.stratocaster.ini')
        self.run_and_check('testing.planeshift.stratocaster.ini')

    def test_symlink_red_testing_ok(self):
        make_config('testing.red.stratocaster.ini')
        make_config('testing.ini')

        make_env_type('testing')
        make_idx_type('red.stratocaster')
        self.run_and_check('testing.red.stratocaster.ini')

    def test_symlink_red_testing_planeshift_ok(self):
        make_config('testing.red.planeshift.stratocaster.ini')
        make_config('testing.planeshift.ini')

        make_env_type('testing')
        make_idx_type('red.planeshift.stratocaster')
        self.run_and_check('testing.red.planeshift.stratocaster.ini')

    def test_symlink_red_production_ok(self):
        make_config('production.red.stratocaster.ini')
        make_config('production.red.gibson.ini')
        make_config('production.ini')

        make_env_type('production')
        make_idx_type('red.stratocaster')
        self.run_and_check('production.red.stratocaster.ini')

        make_idx_type('red.gibson')
        self.run_and_check('production.red.gibson.ini')

    def test_symlink_red_production_planeshift_ok(self):
        make_config('production.red.planeshift.stratocaster.ini')
        make_config('production.red.planeshift.gibson.ini')
        make_config('production.planeshift.ini')

        make_env_type('production')
        make_idx_type('red.planeshift.stratocaster')
        self.run_and_check('production.red.planeshift.stratocaster.ini')

        make_idx_type('red.planeshift.gibson')
        self.run_and_check('production.red.planeshift.gibson.ini')

    def test_old_configs(self):
        '''Конфиги без типа индексатора.
        '''
        make_config('planeshift.ini')

        make_env_type('production')
        make_idx_type('planeshift')
        self.run_and_check('planeshift.ini')

    def test_new_configs(self):
        '''Старые конфиги с типом индексатора.
        '''
        make_config('production.ini')
        make_config('production.stratocaster.ini')
        make_config('production.gibson.ini')

        make_env_type('production')
        make_idx_type('gibson')
        self.run_and_check('production.gibson.ini')

    def test_datasources(self):
        '''Конфиги из market-datasources, без {environment.type}.
        '''
        make_config('planeshift.stratocaster.conf')
        make_config('stratocaster.conf')

        make_idx_type('stratocaster')
        self.run_and_check('stratocaster.conf', name='datasources.conf', extension='conf')

        make_idx_type('planeshift.stratocaster')
        self.run_and_check('planeshift.stratocaster.conf', name='datasources.conf', extension='conf')

    def test_directory_config(self):
        os.mkdir(config_path('testing.stratocaster'))
        os.mkdir(config_path('testing.gibson'))
        os.mkdir(config_path('production.stratocaster'))
        os.mkdir(config_path('production.gibson'))
        os.mkdir(config_path('development.stratocaster'))
        os.mkdir(config_path('development.gibson'))

        make_env_type('production')
        make_idx_type('gibson')

        self.run_and_check('production.gibson', 'local', extension='')

        make_env_type('development')
        self.run_and_check('development.gibson', 'local', extension='')


if '__main__' == __name__:
    unittest.main()
