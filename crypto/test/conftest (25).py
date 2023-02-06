import pytest

from crypta.tx.services.common.test_utils import helpers


pytest_plugins = [
    "crypta.lib.python.test_utils.fixtures",
    "crypta.lib.python.yql.test_helpers.fixtures",
    "crypta.lib.python.yt.test_helpers.fixtures",
]


@pytest.fixture(scope="function")
def config_file(local_yt, mock_sandbox_server_with_identifiers_udf):
    return helpers.render_config_file(
        "crypta/data_import/services/cars/bundle/config.yaml",
        local_yt,
        mock_sandbox_server_with_identifiers_udf,
    )
