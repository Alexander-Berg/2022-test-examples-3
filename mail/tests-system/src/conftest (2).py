from .common.fixtures import *  # noqa
from .common.utils import wait_db_is_ready, wait_webservice_is_ready
import pytest


@pytest.fixture(scope="session", autouse=True)
def service_ready(db_connstring, api_url):
    wait_db_is_ready(db_connstring)
    wait_webservice_is_ready(f"{api_url}/ping")
