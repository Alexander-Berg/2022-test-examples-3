import pytest
import yatest.common

from crypta.utils.rtmr_resource_service.bin.server.testutils import setup
from crypta.utils.rtmr_resource_service.bin.server.testutils.crypta_resource_service import CryptaResourceService


pytest_plugins = [
    "crypta.lib.python.yt.local_tests.fixtures",
]


@pytest.fixture
def resource_service_kv(yt_kv, cluster_envs, public_resources):
    setup.setup(yt_kv, cluster_envs, public_resources)
    yield yt_kv


@pytest.fixture
def resource_service(resource_service_kv, resources, file_root):
    with yatest.common.network.PortManager() as port_manager:
        with CryptaResourceService(
                yt_kv=resource_service_kv,
                working_dir=yatest.common.test_output_path("resource_service"),
                port=port_manager.get_port(),
                resources=resources,
                file_root=file_root,
                solomon_port=port_manager.get_port(),
        ) as service:
            yield service
