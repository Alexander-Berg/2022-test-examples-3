#!/usr/bin/env python
# coding: utf8


import importlib


config = importlib.import_module('market.sre.conf.slb-haproxy.common.tests.config')
ENV = u'test'


def pytest_generate_tests(metafunc):

    if 'primer_config' in metafunc.fixturenames:
        metafunc.parametrize('primer_config, utils', config.get_primer_configs(ENV))

    if 'get_primer_configs' in metafunc.fixturenames:
        metafunc.parametrize(
            'get_primer_configs, environment, excludes',
            [(config.get_primer_configs, ENV, config.excludes)]
        )
