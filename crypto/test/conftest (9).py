import os

import mock
import pytest

from crypta.audience.proto.config_pb2 import (
    TAudienceConfig,
)
from crypta.lib.python import (
    time_utils,
    zk,
)


pytest_plugins = [
    "crypta.lib.python.solomon.test_utils.fixtures",
    "crypta.lib.python.yt.test_helpers.fixtures",
]


@pytest.fixture(scope="session")
def conf_paths():
    import crypta.lib.python.bt.conf.conf as conf
    import crypta.lib.python.bt.conf.resource_conf as resource_conf

    conf.use(resource_conf.find('/crypta/audience/config'))


@pytest.fixture(scope="function")
def conf(yt_stuff, mock_solomon_server, conf_paths):
    import crypta.lib.python.bt.conf.conf as conf

    os.environ["YT_USE_SINGLE_TABLET"] = "1"

    proto_config = TAudienceConfig()

    proto_config.Solomon.Url = mock_solomon_server.url_prefix
    proto_config.Solomon.Token = 'fake'

    proto_config.Yt.Token = 'fake'
    proto_config.Yt.Proxy = yt_stuff.get_server()

    conf.use_proto(proto_config)

    os.environ["YT_CLUSTERS"] = yt_stuff.get_server()
    os.environ["ZK_HOSTS"] = ""
    os.environ["ZK_PORT"] = "1234"

    yield conf


@pytest.fixture
def frozen_time():
    result = "1600000000"
    os.environ[time_utils.CRYPTA_FROZEN_TIME_ENV] = result
    yield result


@pytest.fixture
def mock_zk():
    with mock.patch("crypta.audience.lib.watchman.workflow.zk_client", lambda *_, **__: zk.fake_zk_client()):
        yield
