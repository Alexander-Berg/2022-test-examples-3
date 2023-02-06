import asyncio
from contextvars import copy_context
from functools import partial
from urllib.parse import urlparse


def dummy_async_context_manager(value):
    class _Inner:
        async def __aenter__(self):
            return value

        async def __aexit__(self, *args):
            pass

        async def _await_mock(self):
            return value

        def __await__(self):
            return self._await_mock().__await__()

    return _Inner()


def dummy_async_function(result=None, exc=None, calls=[]):
    async def _inner(*args, **kwargs):
        nonlocal calls
        calls.append((args, kwargs))

        if exc:
            raise exc
        return result

    return _inner


def get_host(url: str) -> str:
    return urlparse(url).netloc


class Holder:
    def __init__(self, func):
        self.func = func
        self.calls = []
        self.released = False
        self.mock = None

    def __call__(self, *args, **kwargs):
        if self.released:
            return self.func(*args, **kwargs)

        future = asyncio.get_event_loop().create_future()
        context = copy_context()
        self.calls.append((future, context, args, kwargs))
        return future

    def __get__(self, obj, type=None):
        return partial(self.__call__, obj)

    async def wait_entered(self, enters=1):
        while len(self.calls) < enters:
            await asyncio.sleep(0)

    def release(self):
        assert not self.released
        self.released = True
        for call in self.calls:
            future, context, args, kwargs = call

            async def perform_call():
                try:
                    coro = self.func(*args, **kwargs)
                    future.set_result(await context.run(asyncio.create_task, coro))
                except Exception as e:
                    future.set_exception(e)

            asyncio.create_task(perform_call())
