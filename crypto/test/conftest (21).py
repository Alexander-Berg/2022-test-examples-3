import pytest
import yatest.common

from crypta.cm.services.toucher.proto.config_pb2 import TConfig
from crypta.lib.python import (
    templater,
    yaml_config,
)

pytest_plugins = [
    "crypta.cm.services.common.test_utils.fixtures",
]


@pytest.fixture
def config_path(cm_client, local_yt_with_dyntables, tvm_api, tvm_ids, tvm_src_id):
    context = {
        "environment": "qa",
        "yt_proxy": local_yt_with_dyntables.get_server(),
        "yt_working_dir": "//home/crypta",
        "yt_pool": "pool",
        "dst_hosts": [cm_client.host.replace("http://", "")],
        "tvm_src_id": tvm_src_id,
        "tvm_dst_id": tvm_ids.api,
        "backup_ttl_days": 1,
        "max_rps": 10,
    }
    output_path = yatest.common.test_output_path("config.yaml")
    templater.render_file(
        yatest.common.source_path("crypta/cm/services/toucher/bundle/config.yaml"),
        output_path,
        context,
        strict=True
    )
    return output_path


@pytest.fixture(scope="function")
def config(config_path):
    return yaml_config.parse_config(TConfig, config_path)
