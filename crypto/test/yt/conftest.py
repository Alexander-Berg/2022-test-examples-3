import crypta.lib.python.bt.conf.conf as conf
import pytest
import crypta.lib.python.bt.conf.resource_conf as resource_conf
from mapreduce.yt.python.yt_stuff import (
    YtStuff,
    YtConfig,
)

from data import dataset
from utils import (
    create_direct_table,
    create_directbytypes_table,
    create_main_task_table,
)


@pytest.fixture(scope="session")
def config():
    conf.use(resource_conf.find('/crypta/graph/configs'))
    return conf


@pytest.fixture(scope="function")
def local_yt(request, config):
    yt = YtStuff(config=YtConfig())
    yt.start_local_yt()
    url = 'localhost:{port}'.format(port=yt.yt_proxy_port)
    config.yt._I_know_what_I_do_set(
        'proxy', {'url': url, 'name': 'local_yt', }
    )
    request.addfinalizer(yt.stop_local_yt)
    return yt


@pytest.fixture(scope="function")
def yt_client(local_yt):
    return local_yt.get_yt_client()


@pytest.fixture(scope="function")
def direct_table(yt_client, config):
    return create_direct_table(yt_client, config, dataset)


@pytest.fixture(scope="function")
def directbytypes_table(yt_client, config):
    return create_directbytypes_table(yt_client, config, dataset)


@pytest.fixture(scope="function")
def main_task_table(yt_client, config):
    return create_main_task_table(yt_client, config, dataset)
