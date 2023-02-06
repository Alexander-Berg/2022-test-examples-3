import pytest

from travel.avia.subscriptions.app.lib.helper import deduplication_async


@pytest.mark.asyncio
async def test_deduplication():
    actual = deduplication_async(
        seq=async_iter([4, 2, 3, 1, 1, 2, 3, 4]),
        constraint_func=lambda x: x
    )
    assert await to_list(actual) == [4, 2, 3, 1]

    dict_list = async_iter([
        {'a': 1, 'b': 2, 'c': 3},
        {'a': 8, 'b': 2, 'c': 3},
        {'a': 5, 'b': 2, 'c': 3},
        {'a': 7, 'b': 2, 'c': 26},
        {'a': 73, 'b': 4, 'c': 60},
        {'a': 67, 'b': 11, 'c': 86},
    ])
    actual = deduplication_async(dict_list, lambda x: (x['c'], x['b']))
    assert await to_list(actual) == [
        {'a': 1, 'b': 2, 'c': 3},
        {'a': 7, 'b': 2, 'c': 26},
        {'a': 73, 'b': 4, 'c': 60},
        {'a': 67, 'b': 11, 'c': 86},
    ]


async def async_iter(iter_):
    for e in iter_:
        yield e


async def to_list(async_iter_):
    return [e async for e in async_iter_]
