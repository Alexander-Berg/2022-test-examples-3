import os

import pytest

from crypta.lib.python.yql import yql_helpers


@pytest.fixture
def patched_environ(local_yt_and_yql_env):
    os.environ.update(local_yt_and_yql_env)


@pytest.fixture
def patched_yt_client(local_yt, patched_environ):
    yield local_yt.get_yt_client()


@pytest.fixture
def patched_yql_client(local_yt, patched_environ):
    yield yql_helpers.create_yql_client(
        yt_proxy=local_yt.get_server(),
        pool='fake_pool',
        token=os.getenv('YQL_TOKEN'),
    )
