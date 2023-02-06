import pytest

from crypta.lib.python.yt.local_tests import local_clusters
from crypta.lib.python.yt.local_tests.yt_replicated_dyntables import YtReplicatedDyntables


@pytest.fixture(scope="session")
def local_yt_with_dyntables():
    cluster_name = "localhost"
    yt = local_clusters.start_local_clusters([cluster_name])[cluster_name]

    try:
        yield yt
    finally:
        yt.stop_local_yt()


@pytest.fixture(scope="session")
def yt_kv(local_yt_with_dyntables):
    return YtReplicatedDyntables(local_yt_with_dyntables)
