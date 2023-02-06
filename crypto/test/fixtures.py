import pytest
from yt.wrapper import YtClient


@pytest.fixture(scope="function")
def local_yt_and_yql(tmpdir, yql_api, mongo, yt):
    import crypta.graph.fuzzy.lib.config as config

    config.Yt.PROXY = yt.get_server()
    config.Yt.TOKEN = "token"
    config.Yql.SERVER = "localhost"
    config.Yql.PORT = yql_api.port
    config.Yql.DB = "plato"
    yield yt, yql_api


@pytest.fixture(scope="function")
def yt_client(local_yt_and_yql):
    import crypta.graph.fuzzy.lib.config as config

    return YtClient(proxy=config.Yt.PROXY, token=config.Yt.TOKEN)
