import pytest
import os

from crypta.lib.python import crypta_env
import crypta.lib.python.bt.conf.conf as conf
import crypta.lib.python.bt.conf.resource_conf as resource_conf

from crypta.lab.proto.config_pb2 import (
    TLabConfig,
)

pytest_plugins = [
    'crypta.lib.python.yql.test_helpers.fixtures',
]


@pytest.fixture(scope="function")
def config(local_yt, local_yt_and_yql_env):
    conf.use(resource_conf.find('/crypta/lab'))
    proto_config = TLabConfig()
    proto_config.Yt.Token = 'fake'
    proto_config.Yql.Token = 'fake'
    proto_config.Yt.Proxy = local_yt.get_server()

    conf.use_proto(proto_config)

    os.environ[crypta_env.EnvNames.crypta_environment] = "testing"
    os.environ.update(local_yt_and_yql_env)
    yield conf


@pytest.fixture
def day():
    return '2021-05-27'
