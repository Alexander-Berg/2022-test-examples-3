# -*- coding: utf-8 -*-

import os
from shutil import rmtree
from unittest import TestCase

from search.pumpkin.yalite_service.libyalite.actions import generators
from search.pumpkin.yalite_service.libyalite.common.config import YaLiteConfiguration
from search.pumpkin.yalite_service.libyalite.core import YaLiteCore

import utils_for_tests as utils
from search.pumpkin.yalite_service.libyalite.common import exceptions

import yatest.common

TMP_DIR = "./testdir/tmp"


class TestGeneratorBaseClass(TestCase):
    def setUp(self):
        configs_dir = self.config.resinfod_configs
        if not os.path.isdir(configs_dir):
            os.makedirs(configs_dir)

        if not os.path.isdir(TMP_DIR):
            os.makedirs(TMP_DIR)

    def tearDown(self):
        configs_dir = self.config.resinfod_configs
        if os.path.isdir(configs_dir):
            rmtree(configs_dir)

        if os.path.isdir(TMP_DIR):
            rmtree(TMP_DIR)


class TestBaseConfigurationGenerator(TestGeneratorBaseClass):
    @classmethod
    def setUpClass(cls):
        print "Testing 'actions.generators.BaseConfigurationGenerator' class:"

        cls.config = YaLiteConfiguration(utils.test_config_path)

    @classmethod
    def tearDownClass(cls):
        print ""

    def test__read_template_raise_GeneratorError(self):
        core = YaLiteCore(utils.test_config_path)
        core.config.templates_path = yatest.common.source_path(core.config.templates_path)

        template_name = "test_template_1.j2"

        generator = generators.BaseConfigurationGenerator(core, command='base-generator', description='')

        self.assertRaises(exceptions.GeneratorError,
                          generator._read_template, template_name)

    def test__read_template_correct_read(self):
        core = YaLiteCore(utils.test_config_path)
        core.config.templates_path = yatest.common.source_path(core.config.templates_path)

        template_name = "test_template.j2"
        generator = generators.BaseConfigurationGenerator(core, command='base-generator', description='')
        test_data = "test template contents. _read_template should read this unmodified."

        result = generator._read_template(template_name)

        self.assertEqual(result.render(), test_data)

    def test__write_config(self):
        config_path = os.path.join(self.config.resinfod_configs, "test-config")

        config_data = "test config data.\n" \
                      "write_config should write this data to file unmodified.\n"

        generators.BaseConfigurationGenerator.write_config(config_data=config_data, config_path=config_path)

        f = open(config_path, "r")
        result = f.read()
        f.close()

        self.assertEqual(result, config_data)

        self.assertRaises(exceptions.GeneratorError,
                          generators.BaseConfigurationGenerator.write_config, config_data, "/nonexistent/config/path")


class TestGenerateResinfodConfig(TestGeneratorBaseClass):
    @classmethod
    def setUpClass(cls):
        print "Testing 'actions.generators.GenerateResinfodConfig' class:"

        cls.config = YaLiteConfiguration(utils.test_config_path)

    @classmethod
    def tearDownClass(cls):
        print ""

    def test_generate_config(self):
        service1 = 'search-service-example'
        service2 = 'test-resinfod-service'

        config_path_1 = os.path.join(self.config.resinfod_configs, service1)
        config_path_2 = os.path.join(self.config.resinfod_configs, service2)

        core = YaLiteCore(utils.test_config_path)
        core.config.templates_path = yatest.common.source_path(core.config.templates_path)

        generator = generators.GenerateResinfodConfig(core)

        generator.run_action(service1)

        self.assertTrue(os.path.isfile(config_path_1))
        self.assertFalse(os.path.isfile(config_path_2))

        os.remove(config_path_1)

        generator.run_action()
        self.assertTrue(os.path.isfile(config_path_1))
        self.assertTrue(os.path.isfile(config_path_2))


class TestGenerateNginxConfig(TestGeneratorBaseClass):
    @classmethod
    def setUpClass(cls):
        print "Testing 'actions.generators.GenerateNginxConfig' class:"

        cls.config = YaLiteConfiguration(utils.test_config_path)

    @classmethod
    def tearDownClass(cls):
        print ""

    def test_generate_config(self):
        nginx_config = os.path.join(TMP_DIR, 'generated-nginx-config')

        core = YaLiteCore(utils.test_config_path)

        core.config.templates_path = yatest.common.source_path(core.config.templates_path)
        generator = generators.GenerateNginxConfig(core)

        generator.run_action(nginx_config)

        self.assertTrue(os.path.isfile(nginx_config))
