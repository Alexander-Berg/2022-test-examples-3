# -*- coding: utf-8 -*-
import os
import mock

import mpfs.config.provider
from mpfs.config.base import Config

from mpfs.config.builder import patch_from_straighten_environment

from mpfs.config.constants import (ALLOWED_PACKAGES, MPFS_ENVIRONMENT_VAR, MPFS_ENVIRONMENT_PREFIX,
                                   ALLOWED_ENVIRONMENTS, MPFS_PACKAGE_VAR, PACKAGE_FILE_PATH)
from mpfs.config.errors import ProviderConfigError, NoSourceConfigError
from mpfs.config.provider import (YAMLConfigProvider, MPFSWorkingEnvironmentConfigProvider,
                                  EnvironmentConfigProvider)
from test.unit.base import NoDBTestCase


YAML_FILE = """\
---
A: true
B:
  - 1
  - test
"""
BAD_FILE = """\
---
TEST:
    -
    qwe
    qweqw : qwe: qwe
"""


class YAMLConfigProviderTestCase(NoDBTestCase):
    def setup_method(self, method):
        self.not_exists_path = '/tmp/yaml_test_not_exists.yaml'
        self.yaml_path = '/tmp/yaml_test.yaml'
        self.bad_format_path = '/tmp/yaml_test.json'
        with open(self.yaml_path, 'w') as fh:
            fh.write(YAML_FILE)
        with open(self.bad_format_path, 'w') as fh:
            fh.write(BAD_FILE)

    def test_file_not_exists(self):
        self.assertRaises(NoSourceConfigError, YAMLConfigProvider(self.not_exists_path).load)

    def test_bad_format(self):
        self.assertRaises(ProviderConfigError, YAMLConfigProvider(self.bad_format_path).load)

    def test_common(self):
        yaml_conf = YAMLConfigProvider(self.yaml_path).load()
        dict_conf = yaml_conf.as_dict()
        assert dict_conf['A'] == True
        assert dict_conf['B'][0] == 1
        assert dict_conf['B'][1] == 'test'


class EnvironmentConfigProvideTestCase(NoDBTestCase):
    def test_common(self):
        new_env = {
            'my_prefix_other': '1',
            'my_prefix_one_two': '2',
            'my_prefix_one_three': '3',
            'my_prefix_bad.delimer': '4',
        }
        result_delimeter = {
            'other': '1',
            'bad.delimer': '4',
            'one': {
                'two': '2',
                'three': '3'
            }
        }
        result_no_delimeter = {
            'other': '1',
            'bad.delimer': '4',
            'one_two': '2',
            'one_three': '3'
        }
        with mock.patch.dict(os.environ, new_env):
            cong_env = EnvironmentConfigProvider(prefix_filter='my_prefix_', delimiter='_')
            dict_conf = cong_env.load_as_dict()
            assert dict_conf == result_delimeter
            cong_env = EnvironmentConfigProvider(prefix_filter='my_prefix_')
            dict_conf = cong_env.load_as_dict()
            assert dict_conf == result_no_delimeter


class MPFSWorkingEnvironmentConfigProviderTestCase(NoDBTestCase):
    env_path = '/tmp/mpfs_test_env'

    params_with_env_file = (
        ('PACKAGE', MPFS_PACKAGE_VAR, PACKAGE_FILE_PATH, ALLOWED_PACKAGES),
        ('ENVIRONMENT', MPFS_ENVIRONMENT_VAR, env_path, ALLOWED_ENVIRONMENTS)
    )

    def change_file(self, path, text):
        with open(path, 'w') as fh:
            fh.write(text)

    @mock.patch.object(mpfs.config.provider.MPFSWorkingEnvironmentConfigProvider,
                       'load_params', params_with_env_file)
    def test_set_by_file(self):
        self.change_file(self.env_path, 'production')
        mpfs_env = MPFSWorkingEnvironmentConfigProvider()
        mpfs_env_dict = mpfs_env.load_as_dict()

        assert mpfs_env_dict['ENVIRONMENT'] == 'production'

    @mock.patch.dict(os.environ,
                     {MPFS_PACKAGE_VAR: 'browser', MPFS_ENVIRONMENT_VAR: 'production'})
    def test_set_by_env(self):
            mpfs_env = MPFSWorkingEnvironmentConfigProvider()
            mpfs_env_dict = mpfs_env.load().as_dict()

            assert mpfs_env_dict['ENVIRONMENT'] == 'production'
            assert mpfs_env_dict['PACKAGE'] == 'browser'

    @mock.patch.object(mpfs.config.provider.MPFSWorkingEnvironmentConfigProvider,
                       'load_params', params_with_env_file)
    @mock.patch.dict(os.environ,
                     {MPFS_PACKAGE_VAR: 'browser'})
    def test_set_by_both(self):
        self.change_file(self.env_path, 'production')
        mpfs_env = MPFSWorkingEnvironmentConfigProvider()
        mpfs_env_dict = mpfs_env.load_as_dict()

        assert mpfs_env_dict['ENVIRONMENT'] == 'production'
        assert mpfs_env_dict['PACKAGE'] == 'browser'

    @mock.patch.object(mpfs.config.provider.MPFSWorkingEnvironmentConfigProvider,
                       'load_params', params_with_env_file)
    @mock.patch.dict(os.environ,
                     {MPFS_ENVIRONMENT_VAR: 'incorrectenv_from_osenv'})
    def test_errors(self):
        """Проверяет возникновение ошибки, когда не подгружается значение переменной MPFS окружения.

        Значения типа окружения выставляются в некорректные значения как в файле, так и в окружении
        OS. При попытке подгрузить это значение должно возникнуть исключение.
        """
        self.change_file(self.env_path, 'incorrectenv_from_file')

        self.assertRaises(ProviderConfigError, MPFSWorkingEnvironmentConfigProvider().load)
