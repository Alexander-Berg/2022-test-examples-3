import pytest
import yatest

from crypta.lib.python import yaml2proto
from crypta.lib.python.logbroker.test_helpers.logbroker_config import LogbrokerConfig
from crypta.lib.python.logbroker.test_helpers.simple_logbroker_client import SimpleLogbrokerClient
from crypta.lookalike.services.lal_refresher.lib.config_pb2 import TConfig


pytest_plugins = [
    "crypta.lib.python.logbroker.test_helpers.fixtures",
    "crypta.lib.python.yt.test_helpers.fixtures",
]


@pytest.fixture
def config(yt_stuff, logbroker_config):
    config = TConfig()
    config.Yt.Proxy = yt_stuff.get_server()
    config.Yt.Pool = "crypta_lookalike"
    config.Tvm.SelfClientId = 0

    config.Mapper.Logbroker.Url = logbroker_config.host
    config.Mapper.Logbroker.Port = logbroker_config.port

    config.Mapper.SlowTopic = "slow"
    config.Mapper.FastTopic = "fast"
    config.Mapper.UpdateIntervalSec = 10

    config.LalsPath = "//lals"
    config.ErrorsDir = "//errors"
    config.ErrorsTtlDays = 3

    return config


@pytest.fixture
def config_path(config):
    path = yatest.common.test_output_path("config.yaml")

    with open(path, "w") as f:
        f.write(yaml2proto.proto2yaml(config))

    return path


@pytest.fixture
def slow_logbroker_client(config, logbroker_port):
    with SimpleLogbrokerClient(LogbrokerConfig("localhost", logbroker_port, config.Mapper.SlowTopic)) as client:
        yield client


@pytest.fixture
def fast_logbroker_client(config, logbroker_port):
    with SimpleLogbrokerClient(LogbrokerConfig("localhost", logbroker_port, config.Mapper.FastTopic)) as client:
        yield client
