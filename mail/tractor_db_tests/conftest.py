from fixtures import *
import pytest
import psycopg2
import time


@pytest.fixture(scope="session", autouse=True)
def service_ready():
    _wait_db_is_ready()


def _wait_db_is_ready():
    postponed_exception = None
    for _ in range(100):
        time.sleep(0.1)
        try:
            psycopg2.connect(CONNINFO)
            return
        except Exception as e:
            postponed_exception = e
    raise postponed_exception
