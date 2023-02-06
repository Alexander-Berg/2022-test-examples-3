# -*- coding: utf-8 -*-

import random
import tempfile
from unittest import TestCase

from search.pumpkin.yalite_service.libyalite.common.config import YaLiteConfiguration

import utils_for_tests as utils
from search.pumpkin.yalite_service.libyalite.common.exceptions import ConfigurationError
import yatest.common

wrong_config_data = """
field_1:
 - val1
 - val2
 wrong_val
"""


class TestYaLiteConfiguration(TestCase):
    @classmethod
    def setUpClass(cls):
        print "Testing YaLiteConfiguration class:"

    @classmethod
    def tearDownClass(cls):
        print ""

    def compare_floats(self, float1, float2, epsilon):
        return abs(float1 - float2) < epsilon

    def test__convert_time(self):
        # pdb.set_trace()
        result = YaLiteConfiguration._convert_time("6ms 5.5 4s 3m 2h 1d")

        check = float(93789.506)
        epsilon = 1e-5

        err_msg = "{0} is not equal {1} in epsilon {2}".format(result, check, epsilon)

        self.assertTrue(self.compare_floats(result, check, epsilon), err_msg)

    def test__clear_attributes(self):
        attr_list = {"test_attr_1": "val1",
                     "test_attr_2": "val2",
                     "loglevel": "CRITICAL"}

        config = YaLiteConfiguration(utils.test_config_path)
        for k, v in attr_list.iteritems():
            config.__setattr__(k, v)

        config.config = attr_list

        config._clear_attributes()

        self.assertRaises(AttributeError, config.__getattribute__, "test_attr_1")
        self.assertRaises(AttributeError, config.__getattribute__, "test_attr_2")
        self.assertEqual(config.loglevel, attr_list["loglevel"],
                         "_clear_attributes function removes default attribute.")

    # TODO: Test loglevel
    # def test_loglevel(self):
    #     config = YaLiteConfiguration('/dev/null')
    #
    #     config.level = "CRITICAL"
    #     lvl = config.level
    #
    #     self.assertEqual(logging.getLevelName(logging.root.getEffectiveLevel()), "CRITICAL")

    def test_read_config(self):
        # Correct configuration
        YaLiteConfiguration(utils.test_config_path)

        # Incorrect configuration
        f = tempfile.NamedTemporaryFile()

        f.write(wrong_config_data)
        f.seek(0)

        self.assertRaises(ConfigurationError, YaLiteConfiguration, f.name)

    def test_read_services(self):
        config = YaLiteConfiguration(utils.test_config_path)

        config.services_available_dir = yatest.common.source_path(config.services_available_dir)
        services_config = config.read_services(config.services_available_dir)

        self.assertTrue(isinstance(services_config, dict))
        self.assertTrue('test-separated-service' in services_config)

    def test_open_config(self):
        config = YaLiteConfiguration(utils.test_config_path)

        config_file = tempfile.NamedTemporaryFile()
        valid_config_path = config_file.name

        check_data = range(30)
        random.shuffle(check_data)
        check_data = ''.join(str(c) for c in check_data)

        config_file.write(check_data)
        config_file.flush()

        f = config._open_config(valid_config_path)

        self.assertEqual(f.read().strip(), check_data)

    def test_global_domains(self):
        config = YaLiteConfiguration(utils.test_config_path)

        self.assertEqual(config.global_domains, set(['pumpkin.yandex.ru',
                                                     'yandex.ru']))

    def test_service_domains(self):
        config = YaLiteConfiguration(utils.test_config_path)

        self.assertEqual(config.service_domains, set(['search.yandex.ru']))

    def test_all_domains(self):
        config = YaLiteConfiguration(utils.test_config_path)

        self.assertEqual(config.all_domains, set(['pumpkin.yandex.ru',
                                                  'yandex.ru',
                                                  'search.yandex.ru']))

# suite = unittest.TestLoader().loadTestsFromTestCase(TestModule_1)
