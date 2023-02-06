import os
import pytest

from crypta.lib.python.ydb.test_helpers.local_ydb import LocalYdb


def get_ydb_endpoint():
    return os.getenv('YDB_ENDPOINT')


def get_ydb_database():
    return os.getenv('YDB_DATABASE')


@pytest.fixture(scope="session")
def local_ydb():
    endpoint = get_ydb_endpoint()
    database = get_ydb_database()
    return LocalYdb(endpoint, database)
