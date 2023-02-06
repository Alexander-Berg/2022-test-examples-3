import itertools

import pytest

from yamarec1.data import ArrayData
from yamarec1.data import DataBank
from yamarec1.data import DataBankLayout
from yamarec1.data import DataQuery
from yamarec1.data import DataRecord
from yamarec1.data import IterableData
from yamarec1.data import QueryableData
from yamarec1.data.banklayouts import ExplicitDataBankLayout
from yamarec1.data.storages import PermanentDataStorage


class DummyIterableData(IterableData):

    def __iter__(self):
        yield DataRecord(0, "0")


class DummyQueryableData(QueryableData):

    @property
    def query(self):
        return DataQuery("SELECT 0, '0'", preamble=("$x = 1",))


class DummyDataBank(DataBank):

    def merge(self, storages, indices):
        records = itertools.chain(*(storage.data for storage in storages))
        return PermanentDataStorage(ArrayData(list(records)))


class DummyDataBankLayout(DataBankLayout):

    def get(self, index):
        return PermanentDataStorage(ArrayData([DataRecord(index, len(index))]))


@pytest.fixture
def iterable_data():
    return DummyIterableData()


@pytest.fixture
def queryable_data():
    return DummyQueryableData()


@pytest.fixture
def data(queryable_data):
    return queryable_data


@pytest.fixture
def iterable_data_storage(iterable_data):
    return PermanentDataStorage(iterable_data)


@pytest.fixture
def queryable_data_storage(queryable_data):
    return PermanentDataStorage(queryable_data)


@pytest.fixture
def data_storage(data):
    return PermanentDataStorage(data)


@pytest.fixture
def data_bank():
    return DummyDataBank(DummyDataBankLayout(), "data bank")


@pytest.fixture
def deep_data_bank(data_bank):
    return DummyDataBank(ExplicitDataBankLayout({"just": data_bank}), "deep data bank")
