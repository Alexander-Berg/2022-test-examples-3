#!/usr/bin/env python
# coding: utf8
import importlib
config = importlib.import_module('market.sre.conf.slb-nginx.common.tests.config')

ENV = u'prod'


def pytest_generate_tests(metafunc):
    metafunc.parametrize('primer_config', config.get_primer_configs(ENV))
