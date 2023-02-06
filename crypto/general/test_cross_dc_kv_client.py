import datetime

import pytest

from crypta.lib.python.yt.dyntables import kv_client
from crypta.lib.python.yt.dyntables.cross_dc_kv_client import CrossDcKvClient


KEY = "key"
KEY2 = "key2"
VALUE = "value"
VALUE2 = "value2"


def create_client(replica):
    return kv_client.make_kv_client(replica.yt_client.config["proxy"]["url"], replica.path, token="FAKE")


@pytest.fixture
def clients(yt_kv):
    yield [create_client(replica) for replica in yt_kv.replicas]


@pytest.fixture
def master_client(yt_kv):
    yield create_client(yt_kv.master)


@pytest.fixture
def cross_dc_client(clients):
    yield CrossDcKvClient(clients[0], clients[1:], datetime.timedelta(seconds=1))


def test_simple(master_client, cross_dc_client):
    assert cross_dc_client.lookup(KEY) is None

    master_client.write(KEY, VALUE)
    assert VALUE == cross_dc_client.lookup(KEY)

    master_client.delete(KEY)
    assert cross_dc_client.lookup(KEY) is None


def test_multi_ops(master_client, cross_dc_client):
    records = {KEY: VALUE, KEY2: VALUE2}
    assert {} == cross_dc_client.lookup_many(records.keys())

    master_client.write_many(records)
    assert records == cross_dc_client.lookup_many(records.keys())

    master_client.delete_many(records.keys())
    assert {} == cross_dc_client.lookup_many(records.keys())


def test_select(master_client, cross_dc_client):
    records = {KEY: VALUE, KEY2: VALUE2}

    master_client.write_many(records)
    assert {KEY2: VALUE2} == cross_dc_client.select('WHERE is_substr("2", key)')


def test_main_replica_down(master_client, cross_dc_client, yt_kv):
    records = {KEY: VALUE, KEY2: VALUE2}

    master_client.write_many(records)
    assert VALUE == cross_dc_client.lookup(KEY)
    assert {KEY2: VALUE2} == cross_dc_client.select('WHERE is_substr("2", key)')

    cross_dc_client.main_kv_client.yt_client.unmount_table(yt_kv.replica_path)
    assert VALUE == cross_dc_client.lookup(KEY)
    assert {KEY2: VALUE2} == cross_dc_client.select('WHERE is_substr("2", key)')
