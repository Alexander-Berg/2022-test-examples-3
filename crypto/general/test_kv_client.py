import pytest
import yt.wrapper as yt

from crypta.lib.python.yt.dyntables import kv_client


KEY = "key"
KEY2 = "key2"
VALUE = "value"
VALUE2 = "value2"


def create_client(master):
    return kv_client.make_kv_client(master.yt_client.config["proxy"]["url"], master.path, token="FAKE")


@pytest.fixture
def client(yt_kv):
    yield create_client(yt_kv.master)


def test_simple(client):
    assert client.lookup(KEY) is None

    client.write(KEY, VALUE)
    assert VALUE == client.lookup(KEY)

    client.delete(KEY)
    assert client.lookup(KEY) is None


def test_multi_ops(client):
    records = {KEY: VALUE, KEY2: VALUE2}
    assert {} == client.lookup_many(records.keys())

    client.write_many(records)
    assert records == client.lookup_many(records.keys())

    client.delete_many(records.keys())
    assert {} == client.lookup_many(records.keys())


def test_transaction(client, yt_kv):
    with client.transaction():
        client.write(KEY, VALUE)

    assert VALUE == client.lookup(KEY)

    client2 = create_client(yt_kv.master)

    with pytest.raises(yt.errors.YtTabletTransactionLockConflict):
        with client.transaction(), client2.transaction():
            client.write(KEY, VALUE2)
            client2.write(KEY, VALUE2)


def test_select(client):
    records = {KEY: VALUE, KEY2: VALUE2}

    client.write_many(records)
    assert {KEY2: VALUE2} == client.select('WHERE is_substr("2", key)')
