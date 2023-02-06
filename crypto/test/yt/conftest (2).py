import crypta.lib.python.bt.conf.conf as conf
import pytest
import crypta.lib.python.bt.conf.resource_conf as resource_conf

from data import (
    match_params_auto,
    match_params_auto_description
)
from mapreduce.yt.python.yt_stuff import (
    YtStuff,
    YtConfig,
)
from crypta.lab.proto.config_pb2 import (
    TLabConfig,
)


@pytest.fixture(scope="session")
def config():
    conf.use(resource_conf.find('/crypta/lab'))
    proto_config = TLabConfig()
    proto_config.Yt.Token = 'fake'
    conf.use_proto(proto_config)
    return conf


@pytest.fixture(scope="function")
def local_yt(request, config):
    yt = YtStuff(config=YtConfig())
    yt.start_local_yt()
    url = "{}:{}".format("localhost", yt.yt_proxy_port)
    config.yt._I_know_what_I_do_set(
        "proxy",
        {
            "url": url,
            "name": "local_yt",
        }
    )
    conf.proto.Yt.Proxy = url
    request.addfinalizer(yt.stop_local_yt)
    return yt


@pytest.fixture(scope="function")
def yt_client(local_yt):
    return local_yt.get_yt_client()


@pytest.fixture(scope="function", params=match_params_auto, ids=match_params_auto_description)
def view_params(request):
    return request.param
