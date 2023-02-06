import edera.storage
from edera.exceptions import StorageOperationError


class _TestingCacheStorage(edera.storage.Storage):
    """
    Mock store for test results.
    """
    def __init__(self, *args, **kwargs):
        super(_TestingCacheStorage, self).__init__(*args, **kwargs)
        self._storage = {}

    def clear(self):
        raise StorageOperationError("Not implemented")

    def delete(self, key, til=None):
        raise StorageOperationError("Not implemented")

    def gather(self):
        raise StorageOperationError("Not implemented")

    def get(self, key, since=None, limit=None):
        if since is not None:
            raise StorageOperationError("Not implemented")
        self._storage.get(key)

    def put(self, key, value):
        self._storage[key] = value
