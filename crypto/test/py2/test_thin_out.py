import datetime
from collections import namedtuple
import os
import pytest

from crypta.lib.python.yt import yt_helpers
from crypta.lib.python.yt.yt_helpers.proto.thin_out_config_pb2 import TThinOutConfig
from crypta.lib.python.yt.yt_helpers.thin_out import ThinOutHistoricalData


TEST_DIR = "//test"
TRANSACTION = namedtuple("TRANSACTION", ["transaction_id"])
CURRENT_TIME = datetime.datetime.now()


@pytest.fixture
def delta():
    return CURRENT_TIME - datetime.datetime(2021, 6, 13)


@pytest.fixture
def short_ttl(delta):
    return (datetime.timedelta(30) + delta).days


@pytest.fixture
def long_ttl(delta):
    return (datetime.timedelta(365) + delta).days


def get_expiration_period(table, yt_client):
    expiration_time = datetime.datetime.strptime(yt_client.get_attribute(table, "expiration_time"), "%Y-%m-%dT%H:%M:%S.%fZ")
    creation_time = datetime.datetime.strptime(yt_client.get_attribute(table, "creation_time"), "%Y-%m-%dT%H:%M:%S.%fZ")

    return expiration_time - creation_time


class MockYtClient(object):
    def __init__(self, yt_stuff):
        self.yt = yt_stuff.get_yt_client()
        self.COMMAND_PARAMS = dict()

    def list(self, *args, **kwargs):
        return self.yt.list(*args, **kwargs)

    def set_attribute(self, *args, **kwargs):
        return self.yt.set_attribute(*args, **kwargs)

    def has_attribute(self, *args, **kwargs):
        return self.yt.has_attribute(*args, **kwargs)

    def create(self, *args, **kwargs):
        return self.yt.create(*args, **kwargs)

    def get(self, path):
        if path.endswith("creation_time"):
            return "{}T08:54:47.874193Z".format(path.split("@")[0].split('/')[-2])
        return self.yt.get(path)

    def set(self, *args, **kwargs):
        return self.yt.set(*args, **kwargs)

    def get_attribute(self, table, name):
        if name == "creation_time":
            return "{}T08:54:47.874193Z".format(table.split('/')[-1])
        return self.yt.get_attribute(table, name)

    def exists(self, *args, **kwargs):
        return True


def test_first_table(yt_stuff, long_ttl, short_ttl):
    yt_client = MockYtClient(yt_stuff)
    config = TThinOutConfig(DailyFolder=TEST_DIR, ShortTtlDays=short_ttl, LongTtlDays=long_ttl, ThinOutDays=7)

    test_table = os.path.join(TEST_DIR, "2021-06-28")
    yt_client.create("table", path=test_table, recursive=True)
    t = ThinOutHistoricalData(
        config=config,
        yt=yt_client,
    )

    t.thin_out(transaction=TRANSACTION(transaction_id="0-0-0-0"),)

    assert get_expiration_period(test_table, yt_client) == datetime.timedelta(days=short_ttl)


def test_small_amount_of_tables(yt_stuff, long_ttl, short_ttl):
    yt_client = MockYtClient(yt_stuff)
    config = TThinOutConfig(DailyFolder=TEST_DIR, ShortTtlDays=short_ttl, LongTtlDays=long_ttl, ThinOutDays=7)

    yt_client.create("table", path=os.path.join(TEST_DIR, "2021-06-24"), recursive=True)
    yt_helpers.set_ttl(os.path.join(TEST_DIR, "2021-06-24"), datetime.timedelta(days=long_ttl), yt_client=yt_client)

    for d in ["2021-06-21", "2021-06-22", "2021-06-23"]:
        yt_client.create("table", path=os.path.join(TEST_DIR, d), recursive=True)
        yt_helpers.set_ttl(os.path.join(TEST_DIR, d), datetime.timedelta(days=short_ttl), yt_client=yt_client)

    test_table = os.path.join(TEST_DIR, "2021-06-20")
    yt_client.create("table", path=test_table, recursive=True)
    t = ThinOutHistoricalData(
        config=config,
        yt=yt_client,
    )

    t.thin_out(transaction=TRANSACTION(transaction_id="0-0-0-0"))
    assert get_expiration_period(test_table, yt_client) == datetime.timedelta(days=short_ttl)


def test_big_amount_of_tables(yt_stuff, long_ttl, short_ttl):
    yt_client = MockYtClient(yt_stuff)
    config = TThinOutConfig(DailyFolder=TEST_DIR, ShortTtlDays=short_ttl, LongTtlDays=long_ttl, ThinOutDays=7)

    yt_client.create("table", path=os.path.join(TEST_DIR, "2021-06-24"), recursive=True)
    yt_helpers.set_ttl(os.path.join(TEST_DIR, "2021-06-24"), datetime.timedelta(days=long_ttl), yt_client=yt_client)

    for d in ["2021-06-25", "2021-06-26", "2021-06-27", "2021-06-28", "2021-06-29", "2021-06-30"]:
        yt_client.create("table", path=os.path.join(TEST_DIR, d), recursive=True)
        yt_helpers.set_ttl(os.path.join(TEST_DIR, d), datetime.timedelta(days=short_ttl), yt_client=yt_client)

    t = ThinOutHistoricalData(
        config=config,
        yt=yt_client,
    )

    test_table = os.path.join(TEST_DIR,  "2021-07-01")
    yt_client.create("table", path=test_table, recursive=True)
    t.thin_out(transaction=TRANSACTION(transaction_id="0-0-0-0"))

    assert get_expiration_period(test_table, yt_client) == datetime.timedelta(days=long_ttl)


def test_not_processed_folder(yt_stuff, long_ttl, short_ttl):
    yt_client = MockYtClient(yt_stuff)
    config = TThinOutConfig(DailyFolder=TEST_DIR, ShortTtlDays=short_ttl, LongTtlDays=long_ttl, ThinOutDays=3)

    for d in ["2021-06-20", "2021-06-21", "2021-06-22", "2021-06-23", "2021-06-24", "2021-06-25", "2021-06-26"]:
        yt_client.create("table", path=os.path.join(TEST_DIR, d), recursive=True)

    t = ThinOutHistoricalData(
        config=config,
        yt=yt_client,
    )

    t.thin_out(transaction=TRANSACTION(transaction_id="0-0-0-0"))

    for d in ["2021-06-20", "2021-06-23", "2021-06-26"]:
        assert get_expiration_period(os.path.join(TEST_DIR, d), yt_client) == datetime.timedelta(days=long_ttl)

    for d in ["2021-06-21", "2021-06-22", "2021-06-24", "2021-06-25"]:
        assert get_expiration_period(os.path.join(TEST_DIR, d), yt_client) == datetime.timedelta(days=short_ttl)


def test_missing_tables(yt_stuff, long_ttl, short_ttl):
    yt_client = MockYtClient(yt_stuff)
    config = TThinOutConfig(DailyFolder=TEST_DIR, ShortTtlDays=short_ttl, LongTtlDays=long_ttl, ThinOutDays=5)

    for d in ["2021-06-13", "2021-06-14", "2021-06-15", "2021-06-19", "2021-06-20", "2021-06-21", "2021-06-22", "2021-06-23", "2021-06-24"]:
        yt_client.create("table", path=os.path.join(TEST_DIR, d), recursive=True)

    for d in ["2021-06-19", "2021-06-20", "2021-06-21", "2021-06-23", "2021-06-24"]:
        yt_helpers.set_ttl(os.path.join(TEST_DIR, d), datetime.timedelta(days=short_ttl), yt_client=yt_client)

    yt_helpers.set_ttl(os.path.join(TEST_DIR, "2021-06-22"), datetime.timedelta(days=long_ttl), yt_client=yt_client)

    t = ThinOutHistoricalData(
        config=config,
        yt=yt_client,
    )

    t.thin_out(transaction=TRANSACTION(transaction_id="0-0-0-0"))

    assert get_expiration_period(os.path.join(TEST_DIR, "2021-06-15"), yt_client) == datetime.timedelta(days=long_ttl)

    for d in ["2021-06-13", "2021-06-14"]:
        assert get_expiration_period(os.path.join(TEST_DIR, d), yt_client) == datetime.timedelta(days=short_ttl)
