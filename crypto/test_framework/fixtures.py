import os

import pytest
import yatest.common

pytest_plugins = [
    "crypta.utils.rtmr_resource_service.bin.server.testutils.fixtures",
]


@pytest.fixture
def file_root(request):
    yield getattr(request.module, "FILE_ROOT", yatest.common.test_source_path("data/resource_service/resources"))


@pytest.fixture
def resources(file_root):
    yield {name: {"resource_type": name} for name in os.listdir(file_root)}


@pytest.fixture
def cluster_envs():
    yield {
        "rtmr-vla": "stable",
    }


@pytest.fixture
def public_resources(file_root, resources):
    yield {
        (resource, "stable", int(version))
        for resource in resources
        for version in os.listdir(os.path.join(file_root, resource))
    }
