import os

from travel.rasp.smoke_tests.smoke_tests import conf
from travel.rasp.smoke_tests.smoke_tests.checkers import Check
from travel.rasp.smoke_tests.smoke_tests.load_tests import load_tests_module


def pytest_addoption(parser):
    test_config = os.environ.get('RASP_SMOKE_TEST_CONFIG', None)
    test_env = os.environ.get('RASP_SMOKE_TEST_ENV', None)
    test_envparams = os.environ.get('RASP_SMOKE_TEST_ENVPARAMS', '')
    test_stableness = os.environ.get('RASP_SMOKE_TEST_STABLENESS', None)

    parser.addoption('--config', help=u'config module path', default=test_config)
    parser.addoption('--env', help=u'set environment', default=test_env)
    parser.addoption('--envparams', help=u'override env params', default=test_envparams)
    parser.addoption('--stableness', help=u'run only stable/unstable/all', default=test_stableness)


def _parse_envparams(envparams):
    if envparams:
        return dict(param_kv.split('=') for param_kv in envparams.split(';'))


def pytest_configure(config):
    conf.config_module = config.option.config
    conf.env_name = config.option.env or conf.env_name
    conf.envparams = _parse_envparams(config.option.envparams)
    conf.stableness = config.option.stableness
    conf.tests_data = load_tests_module(conf.config_module, conf.env_name, conf.stableness, conf.envparams or {})


def pytest_make_parametrize_id(config, val, argname):
    """
    Формирует читаемые имена из инстансов проверок, чтобы они использовались в именах тестов.
    """
    if isinstance(val, Check):
        return str(val)
