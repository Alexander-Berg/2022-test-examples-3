from contextlib import asynccontextmanager


@asynccontextmanager
async def context_lock(*args, **kwargs):
    try:
        yield None
    finally:
        return


class MockZK:
    _cache = {}

    def lock(self, *args, **kwargs):
        return context_lock()

    async def get_raw(self, key):
        return self._cache.get(key)

    async def set_raw(self, key, val):
        self._cache[key] = val
