import os

import pytest

from edera.storages import MongoStorage
from edera.storages import SQLiteStorage


@pytest.fixture
def sqlite_database(tmpdir):
    return os.path.join(str(tmpdir), "edera.db")


@pytest.fixture
def sqlite_storage(sqlite_database):
    return SQLiteStorage(sqlite_database, table="test")


@pytest.yield_fixture
def mongo_storage(mongo):
    try:
        yield MongoStorage(mongo, "edera", "test")
    finally:
        mongo.instance.drop_database("edera")


@pytest.fixture(params=["sqlite_storage", "mongo_storage"])
def storage(request):
    return request.getfixturevalue(request.param)


@pytest.fixture(params=["sqlite_storage", "mongo_storage"])
def multithreaded_storage(request):
    return request.getfixturevalue(request.param)


@pytest.fixture(params=["sqlite_storage", "mongo_storage"])
def multiprocess_storage(request):
    return request.getfixturevalue(request.param)
