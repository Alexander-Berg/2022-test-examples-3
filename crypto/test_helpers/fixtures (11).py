import pytest


@pytest.fixture
def local_yt(tmpdir, yql_api, mongo, yt):
    yield yt


@pytest.fixture
def local_yql(tmpdir, yql_api, mongo, yt):
    yield yql_api


@pytest.fixture
def local_yt_and_yql_env(local_yt, local_yql):
    return dict(
        YT_TOKEN="NO_TOKEN",
        YT_PROXY=local_yt.get_server(),
        YT_POOL="pool",
        YQL_TOKEN="NO_TOKEN",
        CRYPTA_TEST_YQL_SERVER="localhost",
        CRYPTA_TEST_YQL_PORT=str(local_yql.port),
        CRYPTA_TEST_YQL_DB="plato"
    )


@pytest.fixture
def clean_local_yt(local_yt):
    for each in local_yt.yt_client.list('/', absolute=True):
        if each in ('//sys',):
            continue
        local_yt.yt_client.remove(each, recursive=True)

    local_yt.yt_client.create('map_node', '//tmp/yt_wrapper', recursive=True, ignore_existing=True)

    yield local_yt
