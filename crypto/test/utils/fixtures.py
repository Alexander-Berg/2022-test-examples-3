import os

import pytest

import crypta.lib.python.yql.client as yql_helpers


@pytest.fixture
def patched_yt_yql_clients(local_yt, local_yt_and_yql_env):
    os.environ.update(local_yt_and_yql_env)
    yt_client = local_yt.get_yt_client()
    yql_client = yql_helpers.create_yql_client(
        yt_proxy=local_yt.get_server(),
        pool='fake_pool',
        token=os.getenv('YQL_TOKEN'),
    )
    yield yt_client, yql_client
