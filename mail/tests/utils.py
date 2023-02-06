from contextlib import contextmanager
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


def dummy_coro(result=None, label=None, exc=None):
    if label:
        print(f'{label}_coro')

    async def coro():
        if exc:
            raise exc
        else:
            return result

    return coro()


@contextmanager
def dummy_coro_ctx(*args, **kwargs):
    coro = dummy_coro(*args, **kwargs)
    yield coro
    coro.close()


def dummy_coro_generator(*args, **kwargs):
    while True:
        coro = dummy_coro(*args, **kwargs)
        yield coro
        coro.close()


def get_url_part(url: str, part: str) -> str:
    assert part in ('scheme', 'netloc', 'path', 'params', 'query', 'fragment')
    return getattr(urlparse(url), part)
