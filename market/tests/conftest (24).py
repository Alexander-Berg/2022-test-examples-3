#!/usr/bin/env python
# coding: utf8


import importlib
config = importlib.import_module('market.sre.conf.kraken.common.tests.config')


ENV = u'prod'


def pytest_generate_tests(metafunc):

    if 'primer_config' in metafunc.fixturenames:
        metafunc.parametrize('primer_config', config.get_primer_configs(ENV))

    if 'get_primer_configs' in metafunc.fixturenames:
        metafunc.parametrize(
            'get_primer_configs, environment, excludes',
            [(config.get_primer_configs, ENV, config.excludes)]
        )
