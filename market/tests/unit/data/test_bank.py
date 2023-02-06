import pytest

from yamarec1.data import DataRecord
from yamarec1.data.exceptions import DataBankOperationError


def test_bank_supports_simple_indices(data_bank):
    assert list(data_bank["hello"].data) == [DataRecord("hello", 5)]


def test_bank_supports_complex_indices(data_bank):
    assert list(data_bank[["hello", "hi"]].data) == [DataRecord("hello", 5), DataRecord("hi", 2)]


def test_bank_supports_simple_multiindices(deep_data_bank):
    assert list(deep_data_bank["just", "hello"].data) == [DataRecord("hello", 5)]


def test_bank_supports_complex_multiindices(deep_data_bank):
    assert list(deep_data_bank["just", ["hello", "hi"]].data) == [
        DataRecord("hello", 5),
        DataRecord("hi", 2),
    ]


def test_bank_fails_on_invalid_indices(deep_data_bank):
    with pytest.raises(DataBankOperationError):
        deep_data_bank["oops", ["hello", "hi"]]
