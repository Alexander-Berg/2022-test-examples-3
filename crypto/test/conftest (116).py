import pytest

from crypta.lib.python import test_utils


pytest_plugins = [
    'crypta.profile.lib.test_helpers.fixtures',
]


@pytest.fixture
def mock_sandbox_server():
    with test_utils.mock_sandbox_server_with_udf("CRYPTA_URL_UTILS_UDF", "yql/udfs/crypta/url_utils/libcrypta_url_utils_udf.so") as mock:
        yield mock


@pytest.fixture
def udf_patched_config(patched_config, mock_sandbox_server):
    patched_config.CRYPTA_URL_UTILS_UDF_URL = mock_sandbox_server.get_udf_url()
    patched_config.LOG_PARSING_SAMPLING = ''
    yield patched_config
