import logging
import pytest

from crypta.lib.python.yql.client import create_yql_client


@pytest.fixture(scope="module")
def logger():
    log = logging.getLogger(__name__)
    log.setLevel(logging.INFO)
    handler = logging.StreamHandler()
    handler.setLevel(logging.DEBUG)
    log.addHandler(handler)
    return log


@pytest.fixture(scope="function")
def local_yql(tmpdir, yql_api, mongo, yt):
    yql = create_yql_client(
        yt_proxy=yt.get_server(),
        token=None,
        db="plato",
        yql_server="localhost",
        yql_port=yql_api.port
    )
    return yql


@pytest.fixture(scope="function")
def local_yt(yt):
    client = yt.get_yt_client()
    client.proxy = client.config['proxy']['url']
    client.transaction_id = None
    client.pool = 'maozedong'
    client.token = 'maozedong'
    return client
