import pytest
import yatest.common

pytest_plugins = [
    "crypta.lib.python.yt.local_tests.fixtures",
    "crypta.utils.rtmr_resource_service.bin.server.testutils.fixtures",
]


@pytest.fixture
def resources():
    yield {name: {"resource_type": name} for name in ["present", "not_found"]}


@pytest.fixture
def cluster_envs():
    yield {
        "dc1": "stable",
        "dc2": "prestable",
    }


@pytest.fixture
def public_resources():
    yield {
        ("present", "stable", 0),
        ("present", "prestable", 1),
    }


@pytest.fixture
def file_root():
    yield yatest.common.test_source_path("data")
