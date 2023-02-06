import pytest

from yamarec1.data.banklayouts import BasicDataBankLayout
from yamarec1.data.banklayouts import ExplicitDataBankLayout
from yamarec1.data.banks import FactorizedDataBank
from yamarec1.data.banks import PartitionedDataBank
from yamarec1.data.banks import VersionedDataBank


@pytest.fixture
def partitioned_queryable_data_bank(queryable_data_storage):
    layout = ExplicitDataBankLayout(
        {
            "first": queryable_data_storage,
            "second": queryable_data_storage,
            "third": queryable_data_storage,
        })
    return PartitionedDataBank(layout, "partitioned queryable data bank")


@pytest.fixture
def partitioned_iterable_data_bank(iterable_data_storage):
    layout = ExplicitDataBankLayout(
        {
            "first": iterable_data_storage,
            "second": iterable_data_storage,
            "third": iterable_data_storage,
        })
    return PartitionedDataBank(layout, "partitioned iterable data bank")


@pytest.fixture
def versioned_data_bank():
    return VersionedDataBank(BasicDataBankLayout("//home/me"), "versioned data bank")


@pytest.fixture
def versioned_and_suffixed_data_bank():
    return VersionedDataBank(BasicDataBankLayout("//home/me", suffix="suffix"), "versioned and suffixed data bank")


@pytest.fixture
def factorized_data_bank(queryable_data_storage):
    return FactorizedDataBank(
        ExplicitDataBankLayout(
            {
                "left": queryable_data_storage,
                "central": queryable_data_storage,
                "right": queryable_data_storage,
            }
        ),
        "factorized data bank",
        ("user", "item")
    )
