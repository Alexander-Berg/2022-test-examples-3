import pytest
import yatest.common

from crypta.lib.python import templater


pytest_plugins = [
    "crypta.lib.python.test_utils.fixtures",
    "crypta.lib.python.yql.test_helpers.fixtures",
    "crypta.lib.python.yt.test_helpers.fixtures",
]


@pytest.fixture(scope="function")
def config_file(local_yt, mock_sandbox_server_with_identifiers_udf):
    config_file_path = yatest.common.test_output_path("config.yaml")

    templater.render_file(
        yatest.common.source_path("crypta/data_import/services/realty/bundle/config.yaml"),
        config_file_path,
        {
            "environment": "qa",
            "yt_proxy": local_yt.get_server(),
            "crypta_identifier_udf_url": mock_sandbox_server_with_identifiers_udf.get_udf_url(),
        },
    )

    return config_file_path
