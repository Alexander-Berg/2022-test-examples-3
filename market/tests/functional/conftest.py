import os
import time

import kazoo.client
import kazoo.exceptions
import pymongo
import pytest

from edera.helpers import Lazy


@pytest.fixture(scope="session")
def zookeeper_server():
    client = kazoo.client.KazooClient(hosts=os.environ["EDERA_ZOOKEEPER_HOSTS"])
    if not client.start_async().wait(timeout=3):
        pytest.skip("ZooKeeper is unavailable")
    client.stop()
    client.close()


@pytest.yield_fixture
def zookeeper(zookeeper_server):
    client = kazoo.client.KazooClient(hosts=os.environ["EDERA_ZOOKEEPER_HOSTS"])
    client.start()
    try:
        yield client
    finally:
        client.stop()
        client.close()


@pytest.fixture(scope="session")
def mongodb_server():
    client = pymongo.MongoClient(os.environ["EDERA_MONGODB_URI"], serverselectiontimeoutms=3000)
    try:
        client.admin.command('ismaster')
    except pymongo.errors.ConnectionFailure:
        pytest.skip("MongoDB is unavailable")
    client.close()
    del client
    time.sleep(1)


@pytest.yield_fixture
def mongo(mongodb_server):
    client = Lazy[pymongo.MongoClient](os.environ["EDERA_MONGODB_URI"])
    try:
        yield client
    finally:
        client.instance.close()
        client.destroy()
