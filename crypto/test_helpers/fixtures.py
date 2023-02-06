import pytest
import requests
import yatest
import yatest.common.network as network

from crypta.api.test_helpers.crypta_api import CryptaApi
from crypta.api.test_helpers.mock_audience_api import MockAudienceApi

pytest_plugins = [
    "crypta.lib.python.postgres.test_helpers.fixtures",
    "crypta.lib.python.tvm.test_utils.fixtures",
    "crypta.lib.python.yql.test_helpers.fixtures",
]


@pytest.fixture(scope="session")
def mock_audience_api():
    with MockAudienceApi() as api:
        yield api


@pytest.fixture(scope="session")
def tvm_api_dict(tvm_api):
    issue_id = tvm_api.issue_id()
    yield {'port': tvm_api.port, 'issue_id': issue_id, 'secret': tvm_api.get_secret(issue_id)}


@pytest.fixture(scope="function")
def api(local_yt, local_yql, postgres, tvm_api_dict, mock_audience_api):
    with network.PortManager() as pm:
        with CryptaApi(
            working_dir=yatest.common.work_path("api"),
            yt_proxy=local_yt.get_server(),
            yql_connection_string=f"jdbc:yql://localhost:{local_yql.port}/plato?syntaxVersion=1",
            port=pm.get_port(),
            postgres=postgres,
            tvm_api=tvm_api_dict,
            audience_api=mock_audience_api
        ) as api:
            requests.post(f"http://localhost:{api.port}/flyway/baseline")
            requests.post(f"http://localhost:{api.port}/flyway/migrate")
            yield api
