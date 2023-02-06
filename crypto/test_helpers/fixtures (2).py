import copy
import os

import pytest

from crypta.audience.proto.config_pb2 import (
    TAudienceConfig,
)


pytest_plugins = [
    "crypta.lib.python.yql.test_helpers.fixtures",
]


@pytest.fixture(scope="session")
def path_conf():
    import crypta.lib.python.bt.conf.conf as conf
    import crypta.lib.python.bt.conf.resource_conf as resource_conf

    os.environ["CRYPTA_ENVIRONMENT"] = "testing"
    conf.use(resource_conf.find('/crypta/audience/config'))


@pytest.fixture
def default_conf(local_yt, local_yt_and_yql_env, path_conf):
    os.environ["YT_USE_SINGLE_TABLET"] = "1"

    old_env = copy.deepcopy(os.environ)

    os.environ.update(local_yt_and_yql_env)

    import crypta.lib.python.bt.conf.conf as conf

    proto_config = TAudienceConfig()
    proto_config.Yt.Token = 'fake'
    proto_config.Yt.Pool = 'fake-pool'
    proto_config.Yt.Proxy = local_yt.get_server()

    proto_config.Yql.Token = 'fake'

    conf.use_proto(proto_config)
    yield conf

    os.environ.update(old_env)
