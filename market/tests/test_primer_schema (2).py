# coding: utf8

import importlib

tests = importlib.import_module('market.sre.conf.slb-haproxy.common.tests.primer_config_tests')
unique_config_tests = importlib.import_module('market.sre.conf.slb-haproxy.common.tests.unique_config_tests')


test_primer_schema = tests.test_primer_schema
test_configs_name_unique = unique_config_tests.test_configs_name_unique
