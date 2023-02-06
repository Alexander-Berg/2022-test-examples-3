import os

import pytest

from crypta.lib.python import time_utils
from crypta.tx.services.common.test_utils import helpers


@pytest.fixture(scope="function")
def config_file(local_yt, mock_sandbox_server_with_identifiers_udf):
    return helpers.render_config_file(
        "crypta/tx/services/data_import/bundle/config.yaml",
        local_yt,
        mock_sandbox_server_with_identifiers_udf,
    )


@pytest.fixture
def frozen_time():
    result = "1600000000"
    os.environ[time_utils.CRYPTA_FROZEN_TIME_ENV] = result
    yield result
