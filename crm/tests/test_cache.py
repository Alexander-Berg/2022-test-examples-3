from asyncio import sleep
from crm.agency_cabinet.common.cache import TTLCache, cached


class Functor:

    def __init__(self):
        self.counter = 0

    @cached(cache=TTLCache(ttl=5, maxsize=1))
    async def identity(self, val):
        self.counter += 1
        return val

    async def __call__(self, x):
        return await self.identity(x)


async def test_identity_cache():
    f = Functor()
    c1 = await f(1)
    c2 = await f(1)
    c3 = await f(2)
    c4 = await f(2)
    c5 = await f(1)

    assert c1 == c2 == c5 == 1
    assert c3 == c4 == 2

    assert f.counter == 3

    await sleep(6)

    await f(1)
    await f(1)
    await f(1)
    assert f.counter == 4
