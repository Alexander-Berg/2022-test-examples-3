import pytest

from yamarec1.data.banklayouts import ExplicitDataBankLayout
from yamarec1.data.exceptions import DataBankOperationError


def test_layout_works_with_dictionaries(data_storage):
    layout = ExplicitDataBankLayout({"first": data_storage, "second": data_storage})
    assert layout.get("first") is data_storage
    assert layout.get("second") is data_storage
    with pytest.raises(DataBankOperationError):
        layout.get("third")


def test_layout_can_list_all_indices(data_storage):
    layout = ExplicitDataBankLayout({"first": data_storage, "second": data_storage})
    assert layout.indices == {"first", "second"}
