import os

import pytest

from crypta.graph.bochka.proto import config_pb2
from crypta.lib.python import time_utils

pytest_plugins = [
    "crypta.lib.python.logbroker.test_helpers.fixtures",
    "crypta.lib.python.yt.test_helpers.fixtures",
]


@pytest.fixture(scope="function")
def conf(request, yt_stuff, logbroker_config):
    import crypta.lib.python.bt.conf.conf as conf

    proto_config = config_pb2.TYtToLBConfig()
    proto_config.Yt.Token = 'fake'
    proto_config.Yt.Proxy = yt_stuff.get_server()

    proto_config.Collector.FreshDir = "//collector/fresh"
    proto_config.Collector.TablePrefix = "prefix"

    proto_config.LbTopic.TopicName = logbroker_config.topic
    proto_config.Logbroker.Url = logbroker_config.host
    proto_config.Logbroker.Port = logbroker_config.port
    proto_config.Tvm.TvmId = "1"

    conf.use_proto(proto_config)

    yield conf


@pytest.fixture
def frozen_time():
    value = 1500000000
    os.environ[time_utils.CRYPTA_FROZEN_TIME_ENV] = str(value)
    yield value
    del os.environ[time_utils.CRYPTA_FROZEN_TIME_ENV]
