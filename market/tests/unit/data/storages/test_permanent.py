import pytest

from yamarec1.data.exceptions import DataStorageOperationError
from yamarec1.data.storages import PermanentDataStorage


def test_storage_provides_readonly_access(data):
    storage = PermanentDataStorage(data)
    assert storage.data is data
    with pytest.raises(DataStorageOperationError):
        storage.store(data)
