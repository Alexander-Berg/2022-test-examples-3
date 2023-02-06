from __future__ import print_function
import datetime
import time

import pytest
import yt.wrapper as yt

from crypta.lib.python.yt.dyntables import kv_client
from crypta.utils.rtmr_resource_service.lib.db_client import DbClient

STABLE = "stable"
RESOURCE = "resource"
VERSION = 12345


def create_client(yt_kv):
    return DbClient(kv_client.make_kv_client(yt_kv.master.yt_client.config["proxy"]["url"], yt_kv.master.path, token="FAKE"))


@pytest.fixture
def db_client(yt_kv):
    yield create_client(yt_kv)


def test_latest_resource_version(db_client):
    assert db_client.get_latest_resource_version(STABLE, RESOURCE) is None

    db_client.set_latest_resource_version(STABLE, RESOURCE, VERSION)
    assert VERSION == db_client.get_latest_resource_version(STABLE, RESOURCE)


def test_env(db_client):
    assert db_client.get_env("test-cluster") is None

    db_client.set_env("test-cluster", "testing")
    assert "testing" == db_client.get_env("test-cluster")


def test_instances(db_client):
    id1 = "id1"
    id2 = "id2"

    assert set() == db_client.get_instances()
    db_client.register_instance(id1)

    assert {id1} == db_client.get_instances()

    time.sleep(5)
    db_client.register_instance(id2)
    assert {id1, id2} == db_client.get_instances()

    db_client.expire_instances(datetime.timedelta(seconds=5))
    assert {id2} == db_client.get_instances()

    assert set() == db_client.get_instance_resources(id1)
    resources = {("resource1", 123), ("resource2", 456)}

    db_client.set_instance_resources(id1, resources)
    assert resources == db_client.get_instance_resources(id1)


def test_resources(db_client):
    assert set() == db_client.get_resources_to_download()
    resources = {("resource1", 123), ("resource2", 456)}

    db_client.set_resources_to_download(resources)
    assert resources == db_client.get_resources_to_download()


def test_conflicts(db_client, yt_kv):
    db_client2 = create_client(yt_kv)
    id1 = "id1"

    with pytest.raises(yt.errors.YtTabletTransactionLockConflict):
        with db_client.transaction(), db_client2.transaction():
            db_client.set_instance_resources(id1, {("resource1", 1)})
            db_client2.set_instance_resources(id1, {("resource2", 2)})

    print(db_client.get_instance_resources(id1))
