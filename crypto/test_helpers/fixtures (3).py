import pytest
import requests
import yatest
import yatest.common.network as network

from crypta.idm.test_helpers.crypta_idm_api import CryptaIdmApi

pytest_plugins = [
    "crypta.lib.python.postgres.test_helpers.fixtures",
]


@pytest.fixture(scope="function")
def idm_api(postgres):
    with network.PortManager() as pm:
        with CryptaIdmApi(
            working_dir=yatest.common.work_path("idm_api"),
            port=pm.get_port(),
            postgres=postgres
        ) as api:
            requests.post(f"http://localhost:{api.port}/flyway/migrate")
            yield api
