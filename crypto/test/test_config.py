import os
import re

import pytest
import six.moves

from crypta.profile.utils.config import (
    production_config,
    testing_config,
)
from crypta.profile.utils.config import config  # noqa  # flake failed to understand that config is used in test_config
from crypta.profile.utils.config import environment  # noqa


class CryptaEnvironment(object):
    def __init__(self, env_value):
        self.env_value = env_value

    def __enter__(self):
        self.prev_value = os.environ.get(environment.CRYPTA_ENVIRONMENT_ENV)
        os.environ[environment.CRYPTA_ENVIRONMENT_ENV] = self.env_value

    def __exit__(self, exc_type, exc_val, traceback):
        if self.prev_value is None:
            del os.environ[environment.CRYPTA_ENVIRONMENT_ENV]
        else:
            os.environ[environment.CRYPTA_ENVIRONMENT_ENV] = self.prev_value


def is_config_key(key):
    match = re.match(r'(^[A-Z0-9]+(_[A-Z0-9]+)*$)|(^environment$)', key)
    return match is not None


def build_config_keys_dict(module):
    return {key: value for key, value in module.__dict__.items() if is_config_key(key)}


def test_production_testing_are_the_same():
    assert sorted(filter(is_config_key, production_config.__dict__)) == \
           sorted(filter(is_config_key, testing_config.__dict__))


@pytest.mark.parametrize('env_type', [
    environment.CRYPTA_ENVIRONMENT_TESTING,
    environment.CRYPTA_ENVIRONMENT_PRODUCTION,
])
def test_config(env_type):
    global config
    global environment

    with CryptaEnvironment(env_type):
        environment = six.moves.reload_module(environment)
        config = six.moves.reload_module(config)

        return build_config_keys_dict(config)


@pytest.mark.parametrize('env_type', [
    environment.CRYPTA_ENVIRONMENT_TESTING,
    environment.CRYPTA_ENVIRONMENT_PRODUCTION,
])
def test_environment(env_type):
    global environment

    with CryptaEnvironment(env_type):
        environment = six.moves.reload_module(environment)

        return build_config_keys_dict(environment)
