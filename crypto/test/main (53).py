import pytest
import yatest.common

from crypta.lib.python import (
    templater,
    yaml_config,
)
from crypta.lib.python.yt.test_helpers import tests
from crypta.lookalike.proto.yt_node_names_pb2 import TYtNodeNames
from crypta.lookalike.services.model_supplier.proto.model_supplier_config_pb2 import TModelSupplierConfig

YT_NODE_NAMES = TYtNodeNames()
VERSION = "1584085850"
OLD_VERSION_1 = "1584085849"
OLD_VERSION_2 = "1584085848"


@pytest.fixture(scope="function")
def config_file(yt_stuff):
    config_file_path = yatest.common.test_output_path("config.yaml")

    templater.render_file(
        yatest.common.source_path("crypta/lookalike/services/model_supplier/bundle/config.yaml"),
        config_file_path,
        {
            "environment": "qa",
            "yt_proxy": yt_stuff.get_server(),
        },
    )

    return config_file_path


def test_basic(yt_stuff, config_file, model_version):
    config = yaml_config.parse_config(TModelSupplierConfig, config_file)

    input_tables = model_version(VERSION, versions_dir=config.SrcDir, test=tests.Exists()) + \
        model_version(OLD_VERSION_1, versions_dir=config.DstDir, test=tests.Exists()) + \
        model_version(OLD_VERSION_2, versions_dir=config.DstDir, test=tests.IsAbsent())

    tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path(
            "crypta/lookalike/services/model_supplier/bin/crypta-lookalike-model-supplier",
        ),
        args=["--config", config_file],
        data_path=yatest.common.test_source_path("data"),
        input_tables=input_tables,
        output_tables=model_version(VERSION, versions_dir=config.DstDir, test=tests.Exists()),
        env={"LOCAL_YT_SERVER": yt_stuff.get_server()},
    )
