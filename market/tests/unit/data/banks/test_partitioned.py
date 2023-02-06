from yamarec1.data import DataRecord
from yamarec1.data import IterableData
from yamarec1.data import QueryableData


def test_bank_unites_queryable_storages(partitioned_queryable_data_bank):
    storage = partitioned_queryable_data_bank[["first", "second"]]
    assert isinstance(storage.data, QueryableData)
    assert storage.data.query.preamble == ("$x = 1", "$x = 1")
    assert storage.data.query.body == "SELECT 0, '0'\nUNION ALL\nSELECT 0, '0'"


def test_bank_concatenates_iterable_storages(partitioned_iterable_data_bank):
    storage = partitioned_iterable_data_bank[["first", "second"]]
    assert isinstance(storage.data, IterableData)
    assert list(storage.data) == [DataRecord(0, "0"), DataRecord(0, "0")]
